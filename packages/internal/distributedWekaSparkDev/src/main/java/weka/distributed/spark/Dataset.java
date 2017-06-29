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
 *    Dataset
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.spark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;

import distributed.core.DistributedJobConfig;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.DistributedWekaException;

/**
 * Class that encapsulates dataset information - i.e. either an
 * {@code RDD<Instance>} along with ARFF header + summary attribute information
 * and/or, a {@code DataFrame}.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 12253 $
 */
public class Dataset extends AbstractDataset {

  private static final long serialVersionUID = -5152522334987920816L;

  /** The {@code RDD} backing this dataset */
  protected JavaRDD<Instance> m_backingRDD;

  /** The {@code DataFrame} backing this dataset */
  protected DataFrame m_backingDataFrame;

  /**
   * Constructor that takes an {@code RDD}
   * 
   * @param dataset the {@code RDD} dataset
   * @param headerWithSummary the header for the data with summary attributes
   */
  public Dataset(JavaRDD<Instance> dataset, Instances headerWithSummary) {
    this(dataset, null, headerWithSummary);
  }

  /**
   * Constructor that takes a DataFrame
   *
   * @param baseDataFrame the dataframe that backs this dataset
   */
  public Dataset(DataFrame baseDataFrame) {
    this(null, baseDataFrame, null);
  }

  /**
   * Constructor that takes an {@code RDD}, a {@code DataFrame} and a Instances
   * header. Header can be null (if it has not yet been computed); one, but not
   * both, of the RDD or DataFrame can be null.
   *
   * @param backingRDD the backing RDD
   * @param backingDataFrame the backing DataFrame
   * @param headerWithSummary the header information for the data
   */
  public Dataset(JavaRDD<Instance> backingRDD, DataFrame backingDataFrame,
    Instances headerWithSummary) {
    super(headerWithSummary);
    m_backingRDD = backingRDD;
    m_backingDataFrame = backingDataFrame;
  }

  /**
   * Get the {@code RDD} dataset
   *
   * @return the RDD, or null if this dataset is backed only by a dataframe
   */
  public JavaRDD<Instance> getRDD() {
    return m_backingRDD;
  }

  /**
   * Set the {@code RDD} dataset
   *
   * @param rdd the backing RDD
   */
  public void setRDD(JavaRDD<Instance> rdd) {
    m_backingRDD = rdd;
  }

  /**
   * Get the header (with summary attributes) for the dataset
   *
   * @return the header (with summary attributes), or null if no header if
   *         available
   */
  public Instances getHeaderWithSummary() {
    return m_headerWithSummary;
  }

  /**
   * Set an Instances object that contains ARFF header info + summary attributes
   * for the data (RDD or DataFrame) that is backing this dataset
   *
   * @param header the ARFF header
   */
  public void setHeaderWithSummary(Instances header) {
    m_headerWithSummary = header;
  }

  /**
   * Get the {@code DataFrame} dataset
   *
   * @return the dataframe, or null if this dataset is only backed by an
   *         {@code RDD}
   */
  public DataFrame getDataFrame() {
    return m_backingDataFrame;
  }

  /**
   * Set the {@code DataFrame} dataset
   *
   * @param frame the dataframe to set
   */
  public void setDataFrame(DataFrame frame) {
    m_backingDataFrame = frame;
  }

  /**
   * Convert the backing {@code DataFrame} into an {@code RDD<Instance>}. Does
   * not unpersist the DataFrame.
   *
   * @param strategy caching strategy to use for persisting the RDD
   * @param rowHelper optional {@code CSVToARFFHeaderMapTask} to use when
   *          converting DataFrame rows to Instance objects. Default values for
   *          nominal attributes can be specified by supplying a configured
   *          {@code CSVToARFFHeaderMapTaks}. If not supplied, then a default
   *          one is created
   * @param makeSparseInstances true if the RDD should contain
   *          {@code SparseInstance} objects rather than {@code DenseInstance}
   *          ones.
   * @throws DistributedWekaException if the ARFF header is not set, there is no
   *           backing DataFrame to operate on, or the number of columns in the
   *           DataFrame does not equal the number of attributes in the ARFF
   *           header
   */
  public void materializeRDD(CachingStrategy strategy,
    CSVToARFFHeaderMapTask rowHelper, boolean makeSparseInstances)
    throws DistributedWekaException {
    if (getDataFrame() == null) {
      throw new DistributedWekaException("No backing DataFrame to convert to "
        + "RDD<Instance>");
    }

    if (getHeaderWithSummary() == null) {
      throw new DistributedWekaException(
        "Can't convert to RDD<Instance> as there "
          + "is no ARFF header metadata to use");
    }

    Instances headerNoSummary =
      CSVToARFFHeaderReduceTask.stripSummaryAtts(m_headerWithSummary);
    if (m_backingDataFrame.columns().length != headerNoSummary.numAttributes()) {
      throw new DistributedWekaException("Backing DataFrame does not contain "
        + "the same number of columns (" + m_backingDataFrame.columns().length
        + ") as there are attributes in the ARFF header ("
        + headerNoSummary.numAttributes());
    }

    if (rowHelper == null) {
      rowHelper = new CSVToARFFHeaderMapTask();
    }

    m_backingRDD =
      dataFrameToInstanceRDD(strategy, m_backingDataFrame, headerNoSummary,
        rowHelper, makeSparseInstances, false);
  }

  public static DataFrame getAnyDataFrame(SparkJob job) {
    DataFrame result = null;
    Iterator<Map.Entry<String, Dataset>> e = job.getDatasetIterator();
    while (e.hasNext()) {
      Dataset next = e.next().getValue();
      result = next.getDataFrame();
      if (result != null) {
        break;
      }
    }
    return result;
  }

  public static List<DataFrame> getAllDataFrames(SparkJob job) {
    List<DataFrame> dfs = new ArrayList<DataFrame>();
    for (Dataset d : job.getDatasets()) {
      if (d.getDataFrame() != null) {
        dfs.add(d.getDataFrame());
      }
    }

    return dfs;
  }

  public static DataFrame getDataFrame(SparkJob job, String datasetType) {
    Dataset r = job.getDataset(datasetType);
    DataFrame result = null;
    if (r != null) {
      result = r.getDataFrame();
    }

    return result;
  }

  /**
   * Process an {@code RDD<String>} into an {@code RDD<Instance>}
   *
   * @param job the {@code SparkJob} that has produced the input RDD
   * @param input the {@code RDD<String>} input
   * @param headerNoSummary the header of the data without summary attributes
   * @param csvParseOptions the options for the CSV parser
   * @param enforceMaxPartitions if true then any max partitions specified by
   *          the user will be enforced (this might trigger a shuffle operation)
   * @return a {@code JavaRDD<Instance>} dataset
   */
  public static JavaRDD<Instance> stringRDDToInstanceRDD(SparkJob job,
    JavaRDD<String> input, Instances headerNoSummary, String csvParseOptions,
    boolean enforceMaxPartitions) {
    CSVToInstanceFlatMapFunction instanceFunction =
      new CSVToInstanceFlatMapFunction(headerNoSummary, csvParseOptions);

    // , true here because we preserve partitions at this point
    JavaRDD<Instance> dataset = input.mapPartitions(instanceFunction, true);

    input.unpersist();
    input = null;

    if (enforceMaxPartitions
      && !DistributedJobConfig.isEmpty(job.getSparkJobConfig()
        .getMaxInputSlices())) {
      try {
        int maxSlices =
          Integer.parseInt(job.environmentSubstitute(job.getSparkJobConfig()
            .getMaxInputSlices()));
        if (dataset.partitions().size() > maxSlices) {
          job.logMessage("[SparkJob] Coalescing to " + maxSlices
            + " input splits.");
          dataset = dataset.coalesce(maxSlices, true);
        }
      } catch (NumberFormatException e) {
      }
    }

    if (job.getCachingStrategy() != null) {
      dataset.persist(job.getCachingStrategy().getStorageLevel());
    }

    return dataset;
  }

  /**
   * Process a {@code DataFrame} into a {@code RDD<Instance>}
   *
   * @param strategy the {@code CachingStrategy} to use for the RDD
   * @param frame the {@code DataFrame} input
   * @param header the ARFF header to use
   * @param rowHelper a configured {@code CSVToARFFHeaderMapTask} - used for
   *          looking up any default nominal values the user may have specified
   * @param makeSparseInstances true if the {@code Instance} objects created
   *          should be {@code SparseInstance}s rather than
   *          {@code DenseInstance}s
   * @param unpersistFrame true if the input DataFrame should be unpersisted
   * @return a {@code JavaRDD<Instance>} dataset
   * @throws DistributedWekaException if a problem occurs
   */
  public static JavaRDD<Instance> dataFrameToInstanceRDD(
    CachingStrategy strategy, DataFrame frame, Instances header,
    CSVToARFFHeaderMapTask rowHelper, boolean makeSparseInstances,
    boolean unpersistFrame) throws DistributedWekaException {
    JavaRDD<Row> rowRDD = frame.javaRDD();

    RowToInstanceFlatMapFunction instanceFunction =
      new RowToInstanceFlatMapFunction(header, Utils.joinOptions(rowHelper
        .getOptions()), makeSparseInstances);

    JavaRDD<Instance> dataset = rowRDD.mapPartitions(instanceFunction, true);
    if (unpersistFrame) {
      frame.unpersist();
    }

    if (strategy != null) {
      dataset = dataset.persist(strategy.getStorageLevel());
    }

    return dataset;
  }

  /**
   * Convert a Weka {@code Instances} object to a {@code Dataset}.
   *
   * @param toConvert the {@code Instances} to convert
   * @param context the {@code JavaSparkContext} to use
   * @param numPartitions the number of partitions to create in the
   *          {@code Instances} RDD encapsulated in the {@code Dataset}
   * @return a Dataset
   * @throws DistributedWekaException if a problem occurs during the conversion
   */
  public static Dataset instancesToDataset(Instances toConvert,
    JavaSparkContext context, int numPartitions)
    throws DistributedWekaException {

    List<Instance> instanceList = new ArrayList<>();
    CSVToARFFHeaderMapTask stats = new CSVToARFFHeaderMapTask();
    StringBuilder nominalAtts = new StringBuilder();
    List<String> nominalAttSpecs = new ArrayList<>();
    StringBuilder dateAtts = new StringBuilder();
    StringBuilder stringAtts = new StringBuilder();
    List<String> attNames = new ArrayList<>();
    for (int i = 0; i < toConvert.numAttributes(); i++) {
      Attribute a = toConvert.attribute(i);
      attNames.add(a.name());
      switch (a.type()) {
      case Attribute.NOMINAL:
        nominalAtts.append(",").append(i + 1);
        StringBuilder nomVals = new StringBuilder();
        for (int j = 0; j < a.numValues(); j++) {
          if (a.value(j).contains(",")) {
            throw new DistributedWekaException("A nominal value of attribute "
              + "'" + a.name() + "' contains comma characters.");
          }
          nomVals.append(a.value(j)).append(",");
        }
        if (nomVals.length() > 0) {
          nomVals.setLength(nomVals.length() - 1);
        }
        nominalAttSpecs.add((i + 1) + ":" + nomVals);
        break;
      case Attribute.STRING:
        stringAtts.append(",").append(i + 1);
        break;
      case Attribute.DATE:
        dateAtts.append(",").append(i + 1);
        break;
      case Attribute.RELATIONAL:
        throw new DistributedWekaException(
          "Unable to handle relational attributes");
      }
    }
    if (nominalAtts.length() > 0) {
      stats.setNominalAttributes(nominalAtts.substring(1));
      if (nominalAttSpecs.size() > 0) {
        stats.setNominalLabelSpecs(nominalAttSpecs.toArray());
      }
    }
    if (stringAtts.length() > 0) {
      stats.setStringAttributes(stringAtts.substring(1));
    }
    if (dateAtts.length() > 0) {
      stats.setDateAttributes(dateAtts.substring(1));
    }

    Object[] reusableRow = new Object[toConvert.numAttributes()];
    for (int i = 0; i < toConvert.numInstances(); i++) {
      Instance current = toConvert.instance(i);
      instanceList.add(current);
      for (int j = 0; j < toConvert.numAttributes(); j++) {
        if (current.isMissing(j)) {
          reusableRow[j] = null;
        } else if (toConvert.attribute(j).isDate()) {
          reusableRow[j] = new Date((long) current.value(j));
        } else if (toConvert.attribute(j).isNominal()
          || toConvert.attribute(j).isString()) {
          reusableRow[j] = current.stringValue(j);
        } else {
          reusableRow[j] = current.value(j);
        }
      }
      try {
        stats.processRowValues(reusableRow, attNames);
      } catch (IOException e) {
        throw new DistributedWekaException(e);
      }
    }

    Instances headerWithSummary = stats.getHeader();
    JavaRDD<Instance> instanceRDD = context.parallelize(instanceList, numPartitions);
    return new Dataset(instanceRDD, headerWithSummary);
  }
}
