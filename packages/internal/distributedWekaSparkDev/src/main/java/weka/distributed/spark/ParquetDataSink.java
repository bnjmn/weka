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
 *    ParquetDataSink
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.spark;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.DataFrame;

import weka.distributed.DistributedWekaException;

/**
 * DataSink for writing to Parquet files from DataFrames
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class ParquetDataSink extends FileDataSink {

  protected static final String OUTPUT_SUBDIR = "parquet";

  private static final long serialVersionUID = -6072669888962657860L;

  /**
   * Constructor
   */
  public ParquetDataSink() {
    setJobName("Parquet data sink");
    setJobDescription("Save data frames to a parquet files");
  }

  /**
   * Run the job
   *
   * @param sparkContext the spark context to use
   * @return true if successful
   * @throws IOException if a problem occurs
   * @throws DistributedWekaException if a problem occurs
   */
  @Override
  public boolean runJobWithContext(JavaSparkContext sparkContext)
    throws IOException, DistributedWekaException {

    super.runJobWithContext(sparkContext);

    String outputPath = environmentSubstitute(m_sjConfig.getOutputDir());
    outputPath = addSubdirToPath(outputPath, OUTPUT_SUBDIR);
    String outputFilename = environmentSubstitute(getOutputSubDir());
    if (!outputFilename.toLowerCase().endsWith(".parquet")) {
      outputFilename = outputFilename + ".parquet";
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

    dataFrames = applyColumnNamesOrNamesFile(dataFrames);
    for (int i = 0; i < dataFrames.size(); i++) {
      DataFrame df = dataFrames.get(i);
      df = coalesceToOnePartitionIfNecessary(df);
      df = cleanseColumnNames(df);
      String datasetType = datasetTypes.get(i);
      df.write().format("parquet")
        .save(outputPath + datasetType + "_" + outputFilename);
    }

    return true;
  }
}
