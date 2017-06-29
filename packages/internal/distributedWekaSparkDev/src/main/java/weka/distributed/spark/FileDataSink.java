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
 *    FileDataSink
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.spark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.DataFrame;

import weka.core.OptionMetadata;
import weka.distributed.DistributedWekaException;
import distributed.core.DistributedJobConfig;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class FileDataSink extends DataSink {

  private static final long serialVersionUID = -7401989458268010884L;

  /** File to save to */
  protected String m_fileName = "";

  /** User-supplied column names as a list */
  protected String m_columnNames = "";

  /** Names file to read column names from (one per line) */
  protected String m_namesFile = "";

  /** True if data frames should be coalesced to one partition before saving */
  protected boolean m_coalesceToOnePartition;

  @OptionMetadata(
    displayName = "Output subdirectory",
    description = "The name of the subdirectory to output "
      + "to.\nNote this is just the name of the subdirectory, not a full path - the\n"
      + "job output directory is where this subdirectory is created.",
    commandLineParamName = "subdir",
    commandLineParamSynopsis = "-subdir <dir name>", displayOrder = 0)
  public
    void setOutputSubDir(String file) {
    m_fileName = file;
  }

  public String getOutputSubDir() {
    return m_fileName;
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

  public String getNamesFile() {
    return m_namesFile;
  }

  @OptionMetadata(displayName = "Coalesce to one partition before writing",
    description = "Coalesce to one partition before writing",
    commandLineParamName = "coalesce", commandLineParamSynopsis = "-coalesce",
    commandLineParamIsFlag = true, displayOrder = 4)
  public void setCoalesceToOnePartition(boolean coalesceToOnePartition) {
    m_coalesceToOnePartition = coalesceToOnePartition;
  }

  public boolean getCoalesceToOnePartition() {
    return m_coalesceToOnePartition;
  }

  protected List<DataFrame> applyColumnNamesOrNamesFile(List<DataFrame> dfs)
    throws IOException, DistributedWekaException {
    List<DataFrame> result = new ArrayList<DataFrame>();
    // allow user specified names to override...
    if (!DistributedJobConfig.isEmpty(m_namesFile)) {
      List<String> names =
        DataSource.readAttributeNames(environmentSubstitute(m_namesFile));
      for (DataFrame df : dfs) {
        if (df != null) {
          if (names.size() != df.columns().length) {
            throw new DistributedWekaException(
              "Number of user-specified column " + "names (" + names.size()
                + ") does not match the number of columns " + "("
                + df.columns().length + ") data frame!");
          }
          df = df.toDF(names.toArray(new String[names.size()]));
          result.add(df);
        } else {
          result.add(null);
        }
      }
    } else if (!DistributedJobConfig.isEmpty(m_columnNames)) {
      String colNames = environmentSubstitute(m_columnNames);
      String[] names = colNames.split(",");
      for (DataFrame df : dfs) {
        if (df != null) {
          if (names.length != df.columns().length) {
            throw new DistributedWekaException(
              "Number of user-specified column " + "names (" + names.length
                + ") does not match the number of columns " + "("
                + df.columns().length + ") data frame!");
          }

          df = df.toDF(names);
          result.add(df);
        } else {
          result.add(null);
        }
      }
    } else {
      result = dfs;
    }

    return result;
  }

  @Override
  public boolean runJobWithContext(JavaSparkContext sparkContext)
    throws IOException, DistributedWekaException {

    m_currentContext = sparkContext;

    if (DistributedJobConfig.isEmpty(m_sjConfig.getOutputDir())) {
      throw new DistributedWekaException("No output directory specified!");
    }

    if (DistributedJobConfig.isEmpty(getOutputSubDir())) {
      throw new DistributedWekaException("No filename to save to specified!");
    }

    return true;
  }

  protected DataFrame coalesceToOnePartitionIfNecessary(DataFrame df) {
    if (df.javaRDD().partitions().size() > 1 && getCoalesceToOnePartition()) {
      df = df.repartition(1);
    }

    return df;
  }

  protected static void cleanOutputSubdirectory(String outputPath) throws IOException {
    if (SparkUtils.checkDirExists(outputPath)) {
      SparkUtils.deleteDirectory(outputPath);
    }
  }

  protected static DataFrame cleanseColumnNames(DataFrame df) {
    List<String> colNames = Arrays.asList(df.columns());
    List<String> cleansed = new ArrayList<String>();
    boolean ok = true;
    for (String cName : colNames) {
      if (cName.contains(" ") || cName.contains("}") || cName.contains("{")
        || cName.contains(",") || cName.contains(";") || cName.contains(")")
        || cName.contains(")") || cName.contains("=") || cName.contains("\n")
        || cName.contains("\t")) {
        ok = false;
      }
      cName =
        cName.replace(" ", "_").replace("}", "_").replace("{", "_")
          .replace(",", "_").replace(";", "_").replace(")", "_")
          .replace("(", "_").replace("\n", "_").replace("\t", "_");
      cleansed.add(cName);
    }

    if (!ok) {
      df = df.toDF(cleansed.toArray(new String[cleansed.size()]));
    }
    return df;
  }
}
