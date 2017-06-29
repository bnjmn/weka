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
 *    AvroDataSource
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.spark;

import distributed.core.DistributedJob;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import weka.core.Utils;
import weka.distributed.DistributedWekaException;

import java.io.IOException;
import java.util.List;

/**
 * DataSource that uses Databricks spark-avro library to read an Avro file into
 * a DataFrame
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class AvroDataSource extends FileDataSource {

  private static final long serialVersionUID = 7407690932232117753L;

  public AvroDataSource() {
    setJobName("Avro data source");
    setJobDescription("Source data from an Avro file via DataFrames");
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

    DataFrame df =
      sqlContext.read().format("com.databricks.spark.avro").load(resolvedPath);

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
    if (!(toRun instanceof AvroDataSource)) {
      throw new IllegalArgumentException("Object to run is not a "
        + "AvroDataSource");
    }

    try {
      AvroDataSource ds = (AvroDataSource) toRun;

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
    AvroDataSource ds = new AvroDataSource();
    ds.run(ds, args);
  }
}
