/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    CorrelationMatrixRowHadoopReducer
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.hadoop;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import weka.core.Instances;
import weka.core.Utils;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.CorrelationMatrixRowReduceTask;
import weka.distributed.DistributedWekaException;
import weka.distributed.hadoop.CorrelationMatrixHadoopMapper.MatrixRowHolder;
import distributed.core.DistributedJobConfig;

/**
 * Hadoop Reducer implementation for the correlation matrix job. Aggregates a
 * given row (row number is key) of the matrix.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class CorrelationMatrixRowHadoopReducer extends
  Reducer<Text, BytesWritable, Text, Text> {

  /** The underlying Weka correlation reduce tasks */
  protected CorrelationMatrixRowReduceTask m_task;

  /** Header of the data with summary attributes */
  protected Instances m_headerWithSummaryAtts;

  /** True if missing values are to be replaced with means (rather than ignored) */
  protected boolean m_missingsWereReplacedWithMeans;

  /**
   * True if the final matrix is to be a covariance rather than correlation
   * matrix
   */
  protected boolean m_covariance;

  /**
   * Whether the class is to be deleted (if set) or be part of the correlation
   * analysis
   */
  protected boolean m_deleteClassIfSet = true;

  @Override
  public void setup(Context context) throws IOException {
    Configuration conf = context.getConfiguration();

    m_task = new CorrelationMatrixRowReduceTask();

    String sMapTaskOpts = conf
      .get(CorrelationMatrixHadoopMapper.CORRELATION_MATRIX_MAP_TASK_OPTIONS);
    if (!DistributedJobConfig.isEmpty(sMapTaskOpts)) {
      try {
        String[] opts = Utils.splitOptions(sMapTaskOpts);

        m_missingsWereReplacedWithMeans = !Utils
          .getFlag("ignore-missing", opts);
        m_covariance = Utils.getFlag("covariance", opts);

        m_deleteClassIfSet = !Utils.getFlag("keep-class", opts);

        // name of the training ARFF header file
        String arffHeaderFileName = Utils.getOption("arff-header", opts);
        if (DistributedJobConfig.isEmpty(arffHeaderFileName)) {
          throw new IOException(
            "Can't continue without the name of the ARFF header file!");
        }

        m_headerWithSummaryAtts = WekaClassifierHadoopMapper
          .loadTrainingHeader(arffHeaderFileName);

        Instances trainingHeader = CSVToARFFHeaderReduceTask
          .stripSummaryAtts(m_headerWithSummaryAtts);
        WekaClassifierHadoopMapper.setClassIndex(opts, trainingHeader, false);

        // set any class index in the header with summary attributes. Summary
        // atts always come after regular atts, so the class index in the
        // stripped will be the same for the version with summary atts
        if (trainingHeader.classIndex() >= 0) {
          m_headerWithSummaryAtts.setClassIndex(trainingHeader.classIndex());
        }

      } catch (Exception e) {
        throw new IOException(e);
      }
    }
  }

  @Override
  public void reduce(Text key, Iterable<BytesWritable> values, Context context)
    throws IOException {
    List<MatrixRowHolder> rowsToAgg = new ArrayList<MatrixRowHolder>();

    try {
      for (BytesWritable b : values) {
        byte[] bytes = b.getBytes();

        rowsToAgg.add(deserialize(bytes));
      }
    } catch (ClassNotFoundException ex) {
      throw new IOException(ex);
    }

    if (rowsToAgg.size() > 0) {

      int rowNum = rowsToAgg.get(0).getRowNumber();

      List<double[]> rows = new ArrayList<double[]>();
      List<int[]> coOcc = null;
      if (!m_missingsWereReplacedWithMeans) {
        coOcc = new ArrayList<int[]>();
      }

      for (MatrixRowHolder r : rowsToAgg) {
        if (r.getRowNumber() != rowNum) {
          throw new IOException(
            "Matrix row numbers for this key appear to differ!");
        }
        rows.add(r.getRow());
        if (!m_missingsWereReplacedWithMeans) {
          coOcc.add(r.getCoOccurrencesCounts());
        }
      }
      try {
        double[] aggregated = m_task.aggregate(rowsToAgg.get(0).getRowNumber(),
          rows, coOcc, m_headerWithSummaryAtts,
          m_missingsWereReplacedWithMeans, m_covariance, m_deleteClassIfSet);

        // assemble Text key (row num) and Text row (space separated
        // values)

        Text outKey = new Text();
        outKey.set("" + rowNum);

        StringBuilder b = new StringBuilder();
        for (int i = 0; i < aggregated.length; i++) {
          if (i < aggregated.length - 1) {
            b.append("" + aggregated[i]).append(" ");
          } else {
            b.append("" + aggregated[i]);
          }
        }

        Text outVal = new Text();
        outVal.set(b.toString());
        context.write(outKey, outVal);
      } catch (DistributedWekaException e) {
        throw new IOException(e);
      } catch (InterruptedException e) {
        throw new IOException(e);
      }
    }
  }

  /**
   * Helper method for decompressing and deserializing a MatrixRoHolder object
   * 
   * @param bytes an array of bytes holding the serialized compressed
   *          MatrixRowHolder object
   * @return a MatrixRowHolder object holding a partially computed row of the
   *         final matrix
   * @throws IOException if a problem occurs
   * @throws ClassNotFoundException if a particular class can't be found in the
   *           classpath
   */
  protected MatrixRowHolder deserialize(byte[] bytes) throws IOException,
    ClassNotFoundException {
    ByteArrayInputStream istream = new ByteArrayInputStream(bytes);
    ObjectInputStream p = null;
    MatrixRowHolder toReturn = null;

    try {
      p = new ObjectInputStream(new BufferedInputStream(new GZIPInputStream(
        istream)));

      toReturn = (MatrixRowHolder) p.readObject();
    } finally {
      if (p != null) {
        p.close();
      }
    }

    return toReturn;
  }
}
