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
 *    CorrelationMatrixHadoopMapper
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.hadoop;

import distributed.core.DistributedJobConfig;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.CorrelationMatrixMapTask;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.zip.GZIPOutputStream;

/**
 * Hadoop Mapper implementation for the correlation matrix job. Computes a full
 * matrix of partial computed covariance sums using the data entering this map.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class CorrelationMatrixHadoopMapper extends
  Mapper<LongWritable, Text, Text, BytesWritable> {

  /**
   * The key in the Configuration that the options for this task are associated
   * with
   */
  public static String CORRELATION_MATRIX_MAP_TASK_OPTIONS = "*weka.distributed.correlation_matrix_map_task_opts";

  /** The underlying general Weka correlation map task */
  protected CorrelationMatrixMapTask m_task = null;

  /**
   * The underlying general Weka CSV map task - simply for parsing CSV entering
   * the map
   */
  protected CSVToARFFHeaderMapTask m_rowHelper = null;

  /** Header for the incoming data (sans summary attributes) */
  protected Instances m_trainingHeader;

  /** Header for the incoming data, with summary attributes */
  protected Instances m_trainingHeaderWithSummary;

  @Override
  public void setup(Context context) throws IOException {
    m_task = new CorrelationMatrixMapTask();
    m_rowHelper = new CSVToARFFHeaderMapTask();

    Configuration conf = context.getConfiguration();
    String sTaskOpts = conf.get(CORRELATION_MATRIX_MAP_TASK_OPTIONS);
    String sCsvOpts = conf
      .get(CSVToArffHeaderHadoopMapper.CSV_TO_ARFF_HEADER_MAP_TASK_OPTIONS);

    try {
      if (!DistributedJobConfig.isEmpty(sCsvOpts)) {
        String[] csvOpts = Utils.splitOptions(sCsvOpts);
        m_rowHelper.setOptions(csvOpts);
      }
      // m_rowHelper.initParserOnly();

      if (!DistributedJobConfig.isEmpty(sTaskOpts)) {
        String[] taskOpts = Utils.splitOptions(sTaskOpts);

        // name of the training ARFF header file
        String arffHeaderFileName = Utils.getOption("arff-header", taskOpts);
        if (DistributedJobConfig.isEmpty(arffHeaderFileName)) {
          throw new IOException(
            "Can't continue without the name of the ARFF header file!");
        }
        m_trainingHeaderWithSummary = WekaClassifierHadoopMapper
          .loadTrainingHeader(arffHeaderFileName);

        m_trainingHeader = CSVToARFFHeaderReduceTask
          .stripSummaryAtts(new Instances(m_trainingHeaderWithSummary, 0));
        WekaClassifierHadoopMapper.setClassIndex(taskOpts, m_trainingHeader,
          false);

        m_rowHelper.initParserOnly(CSVToARFFHeaderMapTask
          .instanceHeaderToAttributeNameList(m_trainingHeader));

        // Use this method to set the class index in the data with summary
        // attributes
        // otherwise a class index of "last" will not work properly!!
        m_trainingHeaderWithSummary
          .setClassIndex(m_trainingHeader.classIndex());

        // WekaClassifierHadoopMapper.setClassIndex(taskOpts,
        // m_trainingHeaderWithSummary, false);

        // set any remaining options on the task itself
        m_task.setOptions(taskOpts);

        // initialize
        m_task.setup(m_trainingHeaderWithSummary);
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  @Override
  public void map(LongWritable key, Text value, Context context)
    throws IOException {

    if (value != null) {
      processRow(value.toString());
    }
  }

  /**
   * Process a row of data
   * 
   * @param row the row of data (CSV format) to process
   * @throws IOException if a problem occurs
   */
  protected void processRow(String row) throws IOException {
    if (row != null) {
      String[] parsed = m_rowHelper.parseRowOnly(row);

      if (parsed.length != m_trainingHeader.numAttributes()) {
        throw new IOException(
          "Parsed a row that contains a different number of values than "
            + "there are attributes in the training ARFF header: " + row);
      }
      try {
        Instance toProcess = WekaClassifierHadoopMapper.makeInstance(
          m_rowHelper, m_trainingHeader, false, false, parsed);

        m_task.processInstance(toProcess);
      } catch (Exception ex) {
        throw new IOException(ex);
      }
    }
  }

  /**
   * A container class for holding a partially computed row of the
   * correlation/covariance matrix along with the row number.
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   * 
   */
  protected static class MatrixRowHolder implements Serializable {

    /** For serialization */
    private static final long serialVersionUID = -3722225020174913388L;

    /** The row number of this row in the matrix */
    protected int m_rowNumber;

    /** The row itself */
    protected double[] m_row;

    /**
     * If missings are not being replaced, then this holds the count of
     * co-occurrences
     */
    protected int[] m_coCoccurrences;

    /**
     * Construct a new MatrixRowHolder
     * 
     * @param rowNum the number of this row of the matrix
     * @param row the row itself
     * @param coOccurrences the co-occurrence counts (if missings are not
     *          replaced with means)
     */
    public MatrixRowHolder(int rowNum, double[] row, int[] coOccurrences) {
      m_rowNumber = rowNum;
      m_row = row;
      m_coCoccurrences = coOccurrences;
    }

    /**
     * Construct a new MatrixRowHolder
     * 
     * @param rowNum the number of this row of the matrix
     * @param row the row itself
     */
    public MatrixRowHolder(int rowNum, double[] row) {
      this(rowNum, row, null);
    }

    /**
     * Get the row number
     * 
     * @return the row number
     */
    public int getRowNumber() {
      return m_rowNumber;
    }

    /**
     * Get the row
     * 
     * @return the row
     */
    public double[] getRow() {
      return m_row;
    }

    /**
     * Get the co-occurrences counts, or null if missings were replaced with
     * means
     * 
     * @return the co-occurrences counts for this row, or null if missings were
     *         replaced with means
     */
    public int[] getCoOccurrencesCounts() {
      return m_coCoccurrences;
    }
  }

  /**
   * Helper method to serialize a MatrixRowHolder object to a compressed array
   * of bytes
   * 
   * @param rh the MatrixRowHolder to serialize
   * @return an array of bytes holding the serialized (compressed)
   *         MatrixRowHolder object
   * @throws IOException if a problem occurs
   */
  protected static byte[] rowHolderToBytes(MatrixRowHolder rh)
    throws IOException {
    ObjectOutputStream p = null;
    byte[] bytes = null;

    try {
      ByteArrayOutputStream ostream = new ByteArrayOutputStream();
      OutputStream os = ostream;

      p = new ObjectOutputStream(new BufferedOutputStream(new GZIPOutputStream(
        os)));

      p.writeObject(rh);
      p.flush();
      p.close();

      bytes = ostream.toByteArray();

      p = null;
    } finally {
      if (p != null) {
        p.close();
      }
    }

    return bytes;
  }

  @Override
  public void cleanup(Context context) throws IOException, InterruptedException {

    // output all the rows in this partial matrix
    double[][] partialMatrix = m_task.getMatrix();
    int[][] coOcc = m_task.getCoOccurrenceCounts();

    for (int i = 0; i < partialMatrix.length; i++) {
      double[] row = partialMatrix[i];
      int[] co = null;
      if (coOcc != null) {
        co = coOcc[i];
      }
      MatrixRowHolder rh = new MatrixRowHolder(i, row, co);
      byte[] bytes = rowHolderToBytes(rh);

      String sKey = ("" + i);
      Text key = new Text();
      key.set(sKey);

      BytesWritable value = new BytesWritable();
      value.set(bytes, 0, bytes.length);

      context.write(key, value);
    }
  }
}
