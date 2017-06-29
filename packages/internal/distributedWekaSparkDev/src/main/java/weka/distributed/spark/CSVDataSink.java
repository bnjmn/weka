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
 *    CSVDataSink
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.spark;

import com.databricks.spark.csv.util.TextFile;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.DataFrame;
import weka.core.OptionMetadata;
import weka.distributed.DistributedWekaException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * DataSink for writing to CSV files from DataFrames
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class CSVDataSink extends FileDataSink {

  private static final long serialVersionUID = 7540614665086928409L;

  protected static final String OUTPUT_SUBDIR = "csv";

  /** True if the CSV file is to be output with a header row */
  protected boolean m_writeHeaderRow;

  protected String m_delimiter = ",";
  protected String m_quoteChar = "'";
  protected String m_escapeChar = "\\";
  // protected String m_commentChar = "#";
  protected String m_missingValue = "";
  protected String m_charset = TextFile.DEFAULT_CHARSET().name();

  public CSVDataSink() {
    setJobName("CSVDataSink");
    setJobDescription("Writes a data frame to a csv file");
  }

  @OptionMetadata(displayName = "Output a header row",
    description = "Write the csv file with a header", displayOrder = 1,
    commandLineParamName = "header", commandLineParamSynopsis = "-header",
    commandLineParamIsFlag = true)
  public void setWriteHeaderRow(boolean hasHeaderRow) {
    m_writeHeaderRow = hasHeaderRow;
  }

  public boolean getWriteHeaderRow() {
    return m_writeHeaderRow;
  }

  @OptionMetadata(displayName = "Field separator", description = "The field "
    + "separator to use (default = ',')", commandLineParamName = "F",
    commandLineParamSynopsis = "-F <separator>", displayOrder = 4)
  public void setFieldSeparator(String delimiter) {
    m_delimiter = delimiter;
  }

  public String getFieldSeparator() {
    return m_delimiter;
  }

  @OptionMetadata(displayName = "Quote character",
    description = "Quote character for strings (default = \"'\"",
    commandLineParamName = "Q", commandLineParamSynopsis = "-Q <character>",
    displayOrder = 5)
  public void setQuoteCharacter(String quoteCharacter) {
    m_quoteChar = quoteCharacter;
  }

  public String getQuoteCharacter() {
    return m_quoteChar;
  }

  @OptionMetadata(displayName = "Escape character",
    description = "Escape character to use (default = \\)",
    commandLineParamName = "escape",
    commandLineParamSynopsis = "-escape <character>", displayOrder = 6)
  public void setEscapeCharacter(String escapeCharacter) {
    m_escapeChar = escapeCharacter;
  }

  public String getEscapeCharacter() {
    return m_escapeChar;
  }

  @OptionMetadata(displayName = "Null/missing value",
    description = "The value "
      + "to consider as \"null\" or \"missing\"\n(default = empty string)",
    commandLineParamName = "missing",
    commandLineParamSynopsis = "-missing <missing value string>",
    displayOrder = 8)
  public void setMissingValue(String missingValue) {
    m_missingValue = missingValue;
  }

  public String getMissingValue() {
    return m_missingValue;
  }

  @OptionMetadata(displayName = "Charset name",
    description = "Charset name (default = UTF-8)",
    commandLineParamName = "charset",
    commandLineParamSynopsis = "-charset <name>", displayOrder = 9)
  public void setCharsetName(String charsetName) {
    m_charset = charsetName;
  }

  public String getCharsetName() {
    return m_charset;
  }

  @Override
  public boolean runJobWithContext(JavaSparkContext sparkContext)
    throws IOException, DistributedWekaException {

    super.runJobWithContext(sparkContext);

    String outputPath = environmentSubstitute(m_sjConfig.getOutputDir());
    outputPath = addSubdirToPath(outputPath, OUTPUT_SUBDIR);
    String outputFilename = environmentSubstitute(getOutputSubDir());
    if (!outputFilename.toLowerCase().endsWith(".csv")) {
      outputFilename = outputFilename + ".csv";
    }
    outputPath +=
      (outputPath.toLowerCase().contains("://") ? "/" : File.separator);
    outputPath = SparkUtils.resolveLocalOrOtherFileSystemPath(outputPath);
    cleanOutputSubdirectory(outputPath);

    List<String> datasetTypes = new ArrayList<String>();
    Iterator<Map.Entry<String, Dataset>> datasetIterator =
      m_datasetManager.getDatasetIterator();
    List<DataFrame> dataFrames = new ArrayList<DataFrame>();
    while (datasetIterator.hasNext()) {
      Map.Entry<String, Dataset> ne = datasetIterator.next();

      if (ne.getValue().getDataFrame() != null) {
        dataFrames.add(ne.getValue().getDataFrame());
        datasetTypes.add(ne.getKey());
      } else if (ne.getValue().getRDD() != null) {
        // TODO convert the RDD back into a data frame
      }
    }

    // List<DataFrame> dataFrames = Dataset.getAllDataFrames(this);
    dataFrames = applyColumnNamesOrNamesFile(dataFrames);

    // TODO NOTE - 1.3.0 of the spark-csv library has a bug where the
    // user-specified nullValue is ignored when parsing (so only the default
    // empty string works as a null value). Assume this affects writing too...
    for (int i = 0; i < dataFrames.size(); i++) {
      DataFrame df = dataFrames.get(i);
      String datasetType = datasetTypes.get(i);
      df.write().format("com.databricks.spark.csv")
        .option("header", m_writeHeaderRow ? "true" : "false")
        .option("delimiter", m_delimiter).option("quote", m_quoteChar)
        .option("escape", m_escapeChar).option("nullValue", m_missingValue)
        .option("charset", m_charset)
        .save(outputPath + datasetType + "_" + outputFilename);
    }

    return true;
  }
}
