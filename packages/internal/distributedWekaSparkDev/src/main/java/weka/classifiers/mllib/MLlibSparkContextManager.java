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
 *    MLlibSparkContextManager
 *    Copyright (C) 2017 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.mllib;

import org.apache.log4j.WriterAppender;
import org.apache.spark.api.java.JavaSparkContext;
import weka.distributed.spark.SparkUtils;

/**
 * Helper class for maintaining a singleton Spark context. Stopping a Spark
 * context does not release all resources, so creating, starting and stopping
 * many contexts eventually results in OOM exceptions due to no more native
 * threads being available. Despite the ability to start and stop contexts, it
 * appears that the Spark developers do not expect folks to create more than one
 * per JVM.
 *
 * https://issues.apache.org/jira/browse/SPARK-13198
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class MLlibSparkContextManager {

  /** The Spark context in use */
  protected static JavaSparkContext s_contextInPlay;

  /** The writer appender in use */
  protected static WriterAppender s_writerAppender;

  /**
   * Set the current spark context.
   * 
   * @param context the context to set
   * @param writerAppender the writer appender to se
   * @throws IllegalStateException if there is already a context set
   */
  public static synchronized void setSparkContext(JavaSparkContext context,
    WriterAppender writerAppender) {
    if (s_contextInPlay != null) {
      throw new IllegalStateException(
        "There is already an active SparkContext!");
    }
    s_contextInPlay = context;
    s_writerAppender = writerAppender;
  }

  /**
   * Get the current Spark context, or null if there is none set
   *
   * @return the current Spark context, or null if there is none set
   */
  public static synchronized JavaSparkContext getSparkContext() {
    return s_contextInPlay;
  }

  /**
   * Get the current writer appender, or null if there is none set
   *
   * @return the current writer appender
   */
  public static synchronized WriterAppender getWriterAppender() {
    return s_writerAppender;
  }

  /**
   * Routine to stop/shutdown the current Spark context (if one is set)
   */
  public static synchronized void stopContext() {
    if (s_contextInPlay != null) {
      s_contextInPlay.stop();
      s_contextInPlay = null;

      SparkUtils.removeSparkLogAppender(s_writerAppender);
    }
  }
}
