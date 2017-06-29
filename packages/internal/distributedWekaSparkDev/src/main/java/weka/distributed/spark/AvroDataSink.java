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
 *    AvroDataSink
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.spark;

import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.DataFrame;
import weka.distributed.DistributedWekaException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * DataSink for writing to Avro files from DataFrames
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class AvroDataSink extends FileDataSink {

  private static final long serialVersionUID = 605486885671750880L;

  protected final String OUTPUT_SUBDIR = "avro";

  public AvroDataSink() {
    setJobName("Avro data sink");
    setJobDescription("Save data frames to avro files");
  }

  @Override
  public boolean runJobWithContext(JavaSparkContext sparkContext)
    throws IOException, DistributedWekaException {

    super.runJobWithContext(sparkContext);

    String outputPath = environmentSubstitute(m_sjConfig.getOutputDir());
    outputPath = addSubdirToPath(outputPath, OUTPUT_SUBDIR);
    String outputFilename = environmentSubstitute(getOutputSubDir());
    if (!outputFilename.toLowerCase().endsWith(".avro")) {
      outputFilename = outputFilename + ".avro";
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

    for (int i = 0; i < dataFrames.size(); i++) {
      DataFrame df = dataFrames.get(i);
      df = cleanseColumnNames(df);
      df = coalesceToOnePartitionIfNecessary(df);
      String datasetType = datasetTypes.get(i);

      df.write().format("com.databricks.spark.avro")
        .save(outputPath + datasetType + "_" + outputFilename);
    }

    return true;
  }
}
