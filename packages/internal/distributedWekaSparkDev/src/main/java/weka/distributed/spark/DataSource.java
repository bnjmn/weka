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
 *    DataSource
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.spark;

import distributed.core.DistributedJobConfig;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import weka.core.Option;
import weka.core.OptionMetadata;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.distributed.DistributedWekaException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

/**
 * Base class for datasources
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public abstract class DataSource extends SparkJob {

  private static final long serialVersionUID = 5765806558639120668L;

  /** Optional select to execute after loading data */
  protected String m_sqlExpression = "";

  protected DATASET_TYPE m_datasetType = DATASET_TYPE.TRAIN;

  /** Whether to union this data frame with any existing one of the same type */
  protected boolean m_unionWithExisting;

  public static enum DATASET_TYPE {

    TRAIN(AbstractDataset.TRAINING_DATA), TEST(AbstractDataset.TEST_DATA),
    DATA(AbstractDataset.DATASET);

    private final String m_stringVal;

    DATASET_TYPE(String name) {
      m_stringVal = name;
    }

    public String toString() {
      return m_stringVal;
    }
  }

  public static final Tag[] TAGS_SELECTION = {
    new Tag(DATASET_TYPE.TRAIN.ordinal(), AbstractDataset.TRAINING_DATA),
    new Tag(DATASET_TYPE.TEST.ordinal(), AbstractDataset.TEST_DATA),
    new Tag(DATASET_TYPE.DATA.ordinal(), AbstractDataset.DATASET) };

  public DataSource() {
    super("Data source", "Source data via data frames");
  }

  @OptionMetadata(displayName = "Dataset type",
    description = "The type of dataset to create (default = trainingData)",
    displayOrder = 3, commandLineParamName = "dataset-type",
    commandLineParamSynopsis = "-dataset-type [trainingData|testData|dataset]")
  public void setDatasetType(SelectedTag d) {
    int ordinal = d.getSelectedTag().getID();

    for (DATASET_TYPE t : DATASET_TYPE.values()) {
      if (t.ordinal() == ordinal) {
        m_datasetType = t;
        break;
      }
    }
  }

  public SelectedTag getDatasetType() {
    return new SelectedTag(m_datasetType.ordinal(), TAGS_SELECTION);
  }

  @OptionMetadata(displayName = "Union with existing",
    description = "Union this dataset with any existing one "
      + "of the same type", commandLineParamName = "union",
    commandLineParamSynopsis = "-union", commandLineParamIsFlag = true,
    displayOrder = 4)
  public void setUnionWithExisting(boolean unionWithExisting) {
    m_unionWithExisting = unionWithExisting;
  }

  public boolean getUnionWithExisting() {
    return m_unionWithExisting;
  }

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> opts = new Vector<Option>();
    Enumeration<Option> e = super.listOptions();
    while (e.hasMoreElements()) {
      opts.add(e.nextElement());
    }

    opts.addAll(Option.listOptionsForClassHierarchy(this.getClass(),
      SparkJob.class));

    return opts.elements();
  }

  @Override
  public void setOptions(String[] opts) throws Exception {
    super.setOptions(opts);

    Option.setOptionsForHierarchy(opts, this, SparkJob.class);
  }

  @Override
  public String[] getOptions() {
    List<String> opts = new ArrayList<String>();
    opts.addAll(Arrays.asList(super.getOptions()));

    opts.addAll(Arrays.asList(Option.getOptionsForHierarchy(this,
      SparkJob.class)));

    return opts.toArray(new String[opts.size()]);
  }

  public String[] getBaseDSOptionsOnly() {
    List<String> opts = new ArrayList<String>();
    opts.addAll(Arrays.asList(super.getBaseOptionsOnly()));
    opts.addAll(Arrays.asList(Option.getOptions(this, this.getClass())));

    return opts.toArray(new String[opts.size()]);
  }

  @OptionMetadata(displayName = "SQL expression",
    description = "SQL expression to execute after loading data.\n"
      + "Data is registered as a table called 'tmp_table', so an expression "
      + "needs to have a 'FROM tmp_table' clause.\nNote that column names "
      + "that contain spaces, hyphens etc. need to be surrounded with\n"
      + "backticks (i.e. `)", commandLineParamName = "sql",
    commandLineParamSynopsis = "-sql <expression>", displayOrder = 100)
  public void setSQLExpression(String expr) {
    m_sqlExpression = expr;
  }

  public String getSQLExpression() {
    return m_sqlExpression;
  }

  public DataFrame executeSQL(DataFrame df, SQLContext context)
    throws DistributedWekaException {
    DataFrame result = df;
    if (!DistributedJobConfig.isEmpty(getSQLExpression())) {
      String SQL = environmentSubstitute(getSQLExpression());
      if (!SQL.contains("tmp_table")) {
        throw new DistributedWekaException(
          "It appears that the SQL expression "
            + "does not reference the required temporary table name: tmp_table");
      }
      df.registerTempTable("tmp_table");
      result = context.sql(SQL);
    }

    return result;
  }

  public DataFrame unionWithExisting(DataFrame toUnion) {
    DataFrame result = toUnion;
    if (getUnionWithExisting()) {
      DataFrame existing = Dataset.getDataFrame(this, m_datasetType.toString());
      if (existing != null) {
        result = existing.unionAll(toUnion);
      }
    }
    return result;
  }

  /**
   * Apply partitioning to the data frame
   * 
   * @param df the data frame to apply partitioning to
   * @param minPartitions the minimum number of partitions. If the data frame
   *          has fewer partitions than this then it will be repartitioned
   * @param maxPartitions the maximum number of partitions. If the data frame
   *          has more partitions than this it will be coalesced
   * @return the repartitioned/coalesced data frame
   */
  public static DataFrame applyPartitioning(DataFrame df, int minPartitions,
    int maxPartitions) {
    DataFrame result = df;

    if (minPartitions > 0 || maxPartitions > 0) {
      // do we repartition or coalesce?
      JavaRDD<Row> rdd = df.javaRDD();
      if (rdd.partitions().size() < minPartitions) {
        result = df.repartition(minPartitions);
      } else if (rdd.partitions().size() > maxPartitions && maxPartitions > 0) {
        result = df.coalesce(maxPartitions);
      }
    }

    return result;
  }

  /**
   * Read attribute/column names from a file
   *
   * @param path the path to the file to read (can be local or HDFS)
   * @return a list of column names
   * @throws IOException if a problem occurs
   */
  public static List<String> readAttributeNames(String path) throws IOException {
    if (DistributedJobConfig.isEmpty(path)) {
      throw new IOException("No names file path specified!");
    }

    List<String> result = new ArrayList<String>();
    InputStream in = SparkUtils.openFileForRead(path);
    InputStreamReader isr = new InputStreamReader(in);
    BufferedReader br = new BufferedReader(isr);

    try {
      String line = "";
      while ((line = br.readLine()) != null) {
        result.add(line.trim());
      }
    } finally {
      br.close();
    }

    if (result.size() == 0) {
      throw new IOException("No names specified in the file: " + path);
    }

    return result;
  }

  /**
   * Set the number of partitions to use in the DataFrame. Does nothing if not
   * specified, or the number of requested partitions is equal to the number in
   * the DataFrame already.
   *
   * @param df the {@code DataFrame} to repartition/coalesce
   * @return the repartitioned {@code DataFrame}
   */
  protected DataFrame applyPartitioning(DataFrame df) {
    int minPartitions = -1;
    int maxPartitions = -1;
    if (!DistributedJobConfig.isEmpty(m_sjConfig.getMinInputSlices())) {
      try {
        minPartitions =
          Integer
            .parseInt(environmentSubstitute(m_sjConfig.getMinInputSlices()));
      } catch (NumberFormatException ex) {
        // quietly ignore
      }
    }
    if (!DistributedJobConfig.isEmpty(m_sjConfig.getMaxInputSlices())) {
      try {
        maxPartitions =
          Integer
            .parseInt(environmentSubstitute(m_sjConfig.getMaxInputSlices()));
      } catch (NumberFormatException ex) {
        // quietly ignore
      }
    }

    return applyPartitioning(df, minPartitions, maxPartitions);
  }

  /**
   * Persist the {@code DataFrame} with the configured caching strategy
   *
   * @param df the {@code DataFrame} to persist
   */
  protected void persist(DataFrame df) {
    df.persist(getCachingStrategy().getStorageLevel());
  }
}
