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
 *    FileDataSource
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.spark;

import distributed.core.DistributedJobConfig;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.DataFrame;
import weka.core.OptionMetadata;
import weka.core.Utils;
import weka.distributed.DistributedWekaException;
import weka.gui.ProgrammaticProperty;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Base class for file datasources
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public abstract class FileDataSource extends DataSource {

  private static final long serialVersionUID = -6174427389899582821L;

  /** Input file to read */
  protected String m_inputFile = "";

  /** User-supplied column names as a list */
  protected String m_columnNames = "";

  /** Names file to read column names from (one per line) */
  protected String m_namesFile = "";

  protected String m_saver = "";

  /**
   * Get the path to the file to process by this job. This can be on the local
   * file system or in HDFS. Use hdfs:// to specify a file in HDFS.
   *
   * @return the path to the file to process by the job
   */
  public String getInputFile() {
    return m_inputFile;
  }

  /**
   * Set the path to the file to process by this job. This can be on the local
   * file system or in HDFS. Use hdfs:// to specify a file in HDFS.
   *
   * @param filePath the path to the file to process by the job
   */
  @OptionMetadata(displayName = "Input file",
    description = "The path to the file "
      + "to read.\nThis can be on the local file system or in HDFS\n"
      + "(use hdfs:// to specify a file in HDFS).",
    commandLineParamName = "input-file",
    commandLineParamSynopsis = "-input-file <file path>", displayOrder = 1)
  public void setInputFile(String filePath) {
    m_inputFile = filePath;
  }

  @OptionMetadata(displayName = "Column names to use",
    description = "Column names to use", commandLineParamName = "A",
    commandLineParamSynopsis = "-A <column names>", displayOrder = 2)
  public void setColumnNames(String columnNames) {
    m_columnNames = columnNames;
  }

  public String getColumnNames() {
    return m_columnNames;
  }

  @OptionMetadata(displayName = "Names file to use for column names",
    description = "Names file to use for column names. Can exist locally or "
      + "in HDFS. Use either this option or specify a list of column names "
      + "via -A. Neither have to be specified if your CSV file has a header "
      + "row", commandLineParamName = "names-file",
    commandLineParamSynopsis = "-names-file <path to file>", displayOrder = 3)
  public void setNamesFile(String path) {
    m_namesFile = path;
  }

  @ProgrammaticProperty
  @OptionMetadata(displayName = "Saver (testing only)",
    description = "Saver to save with + options (testing use only)",
    commandLineParamName = "saver", commandLineParamSynopsis = "-saver <spec>")
  public void setSaver(String saver) {
    m_saver = saver;
  }

  public String getSaver() {
    return m_saver;
  }

  public String getNamesFile() {
    return m_namesFile;
  }

  protected DataFrame applyColumnNamesOrNamesFile(DataFrame df)
    throws IOException, DistributedWekaException {
    // allow user specified names to override...
    if (!DistributedJobConfig.isEmpty(m_namesFile)) {
      List<String> names =
        DataSource.readAttributeNames(environmentSubstitute(m_namesFile));
      if (names.size() != df.columns().length) {
        throw new DistributedWekaException("Number of user-specified column "
          + "names (" + names.size()
          + ") does not match the number of columns " + "("
          + df.columns().length + ") data frame!");
      }
      df = df.toDF(names.toArray(new String[names.size()]));
    } else if (!DistributedJobConfig.isEmpty(m_columnNames)) {
      String colNames = environmentSubstitute(m_columnNames);
      String[] names = colNames.split(",");
      if (names.length != df.columns().length) {
        throw new DistributedWekaException("Number of user-specified column "
          + "names (" + names.length
          + ") does not match the number of columns " + "("
          + df.columns().length + ") data frame!");
      }

      df = df.toDF(names);
    }

    return df;
  }

  protected void runDataSinkIfNecessary(JavaSparkContext sparkContext)
    throws DistributedWekaException {
    if (!DistributedJobConfig.isEmpty(getSaver())) {
      try {
        String[] sOpts = Utils.splitOptions(environmentSubstitute(getSaver()));
        String saverName = sOpts[0];
        sOpts[0] = "";
        DataSink saver =
          (DataSink) Utils.forName(DataSink.class, saverName, sOpts);
        logMessage("Executing data sink " + saver.getClass().getCanonicalName());
        for (Map.Entry<String, Dataset> de : m_datasetManager.m_datasets
          .entrySet()) {
          saver.setDataset(de.getKey(), de.getValue());
        }
        saver.getSparkJobConfig().setOutputDir(m_sjConfig.getOutputDir());
        saver.runJobWithContext(sparkContext);
      } catch (Exception e) {
        throw new DistributedWekaException(e);
      }
    }

  }

  @Override
  protected void applyConfigProperties(JavaSparkContext sparkContext)
    throws DistributedWekaException {
    setCachingStrategy(new CachingStrategy(getInputFile(),
      m_sjConfig.getAvailableClusterMemory()
        * (m_sjConfig.getMemoryFraction() > 0
          && m_sjConfig.getMemoryFraction() <= 1 ? m_sjConfig
          .getMemoryFraction() : 0.6),
      m_sjConfig.getInMemoryDataOverheadFactor(), sparkContext));

    logMessage("Using caching strategy: "
      + getCachingStrategy().getStorageLevel().description());

    if (m_sjConfig.getMemoryFraction() > 0
      && m_sjConfig.getMemoryFraction() <= 1) {
      sparkContext.getConf().set("spark.storage.memoryFraction",
        "" + m_sjConfig.getMemoryFraction());
    }
  }

  @Override
  public boolean runJobWithContext(JavaSparkContext sparkContext)
    throws IOException, DistributedWekaException {

    m_currentContext = sparkContext;

    if (DistributedJobConfig.isEmpty(getInputFile())) {
      throw new DistributedWekaException("No file path specified!");
    }

    return true;
  }
}
