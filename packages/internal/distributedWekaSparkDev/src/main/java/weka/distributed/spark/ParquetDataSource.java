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
 *    ParquetDataSource
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
 * DataSource for reading a Parquet file into a DataFrame
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class ParquetDataSource extends FileDataSource {

  private static final long serialVersionUID = 3258224777953440399L;

  public ParquetDataSource() {
    setJobName("Parquet data source");
    setJobDescription("Source data from parquet file via DataFrames");
  }

  @Override
  public boolean runJobWithContext(JavaSparkContext sparkContext)
    throws IOException, DistributedWekaException {

    super.runJobWithContext(sparkContext);

    SQLContext sqlContext = new SQLContext(sparkContext);
    // TODO could allow multiple paths...
    DataFrame df =
      sqlContext
        .read()
        .parquet(
          SparkUtils
            .resolveLocalOrOtherFileSystemPath(environmentSubstitute(getInputFile())));

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
    if (!(toRun instanceof ParquetDataSource)) {
      throw new IllegalArgumentException("Object to run is not an "
        + "ParquetDataSource");
    }

    try {
      ParquetDataSource ds = (ParquetDataSource) toRun;
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
    ParquetDataSource ds = new ParquetDataSource();
    ds.run(ds, args);
  }
}
