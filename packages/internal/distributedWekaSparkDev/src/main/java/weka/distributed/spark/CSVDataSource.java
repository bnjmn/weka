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
 *    CSVDataSource
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.spark;

import com.databricks.spark.csv.util.TextFile;
import distributed.core.DistributedJob;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import weka.core.CommandlineRunnable;
import weka.core.OptionMetadata;
import weka.core.Utils;
import weka.distributed.DistributedWekaException;

import java.io.IOException;
import java.util.List;

/**
 * DataSource that uses the Databricks spark-csv library to read a CSV file into
 * a DataFrame
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class CSVDataSource extends FileDataSource implements
  CommandlineRunnable {

  private static final long serialVersionUID = -7039539062415120527L;

  /** True if the source CSV file has a header row */
  protected boolean m_hasHeaderRow;

  protected String m_delimiter = ",";
  protected String m_quoteChar = "'";
  protected String m_escapeChar = "\\";
  protected String m_commentChar = "#";
  protected String m_missingValue = "";
  protected String m_charset = TextFile.DEFAULT_CHARSET().name();
  protected boolean m_inferSchema;

  // TODO parserLib, parseMode, nullValue...?

  /**
   * Constructor
   */
  public CSVDataSource() {
    setJobName("CSV data source");
    setJobDescription("Source data from a CSV file via DataFrames");
  }

  @OptionMetadata(displayName = "Has header row",
    description = "Indicates that the CSV file has a header row",
    displayOrder = 1, commandLineParamName = "csv-header",
    commandLineParamSynopsis = "-csv-header", commandLineParamIsFlag = true)
  public void setHasHeaderRow(boolean hasHeaderRow) {
    m_hasHeaderRow = hasHeaderRow;
  }

  public boolean getHasHeaderRow() {
    return m_hasHeaderRow;
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

  @OptionMetadata(displayName = "Comment character",
    description = "Comment character to use (default = #)",
    commandLineParamName = "comment",
    commandLineParamSynopsis = "-comment <character>", displayOrder = 7)
  public void setCommentCharacter(String commentCharacter) {
    m_commentChar = commentCharacter;
  }

  public String getCommentCharacter() {
    return m_commentChar;
  }

  @OptionMetadata(displayName = "Null/missing value",
    description = "The value "
      + "to consider as \"null\" or \"missing\"\n(default = empty string)",
    commandLineParamName = "M",
    commandLineParamSynopsis = "-M <missing value string>", displayOrder = 8)
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

  @OptionMetadata(displayName = "Infer schema for CSV", description = "Infer "
    + "the scehma (requires a second pass over the data)",
    commandLineParamName = "infer-schema",
    commandLineParamSynopsis = "-infer-schema", commandLineParamIsFlag = true,
    displayOrder = 10)
  public void setInferSchema(boolean infer) {
    m_inferSchema = infer;
  }

  public boolean getInferSchema() {
    return m_inferSchema;
  }

  @Override
  public boolean runJobWithContext(JavaSparkContext sparkContext)
    throws IOException, DistributedWekaException {

    super.runJobWithContext(sparkContext);

    SQLContext sqlContext = new SQLContext(sparkContext);

    // TODO could allow multiple paths...
    String resolvedPath =
      SparkUtils
        .resolveLocalOrOtherFileSystemPath(environmentSubstitute(getInputFile()));

    // TODO NOTE - 1.3.0 of the spark-csv library has a bug where the
    // user-specified nullValue is ignored when parsing (so only the default
    // empty string works as a null value)
    DataFrame df =
      sqlContext.read().format("com.databricks.spark.csv")
        .option("inferSchema", m_inferSchema ? "true" : "false")
        .option("header", m_hasHeaderRow ? "true" : "false")
        .option("delimiter", m_delimiter).option("quote", m_quoteChar)
        .option("escape", m_escapeChar).option("comment", m_commentChar)
        .option("nullValue", m_missingValue).option("charset", m_charset)
        .load(resolvedPath);

    df = applyColumnNamesOrNamesFile(df);

    df = executeSQL(df, sqlContext);
    df = unionWithExisting(df);

    df = applyPartitioning(df);
    persist(df);

    m_datasetManager.setDataset(m_datasetType.toString(), new Dataset(df));

    if (getDebug()) {
      System.err.println("Schema:\n");
      df.printSchema();
      logMessage("Number of rows in DataFrame: " + df.count());
      logMessage("First 5 rows:\n");
      List<Row> rowList = df.takeAsList(5);
      for (Row r : rowList) {
        logMessage(r.toString());
      }
    }

    runDataSinkIfNecessary(sparkContext);
    return true;
  }

  @Override
  public void run(Object toRun, String[] options) {
    if (!(toRun instanceof CSVDataSource)) {
      throw new IllegalArgumentException("Object to run is not an "
        + "CSVDataSource");
    }

    try {
      CSVDataSource ds = (CSVDataSource) toRun;
      if (Utils.getFlag('h', options)) {
        String help = DistributedJob.makeOptionsStr(ds);
        System.err.println(help);
        System.exit(1);
      }

      ds.setOptions(options);
      ds.runJob();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public static void main(String[] args) {
    CSVDataSource ds = new CSVDataSource();
    ds.run(ds, args);
  }
}
