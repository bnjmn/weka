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
 *    SparkUtils.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.spark;

import distributed.core.DistributedJob;
import distributed.spark.SparkJobConfig;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.WriterAppender;
import org.apache.spark.api.java.JavaSparkContext;
import scala.Tuple2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Map;

/**
 * Some utility routines pertaining to files
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class SparkUtils {

  /**
   * Adds a subdirectory to a parent path. Handles local and hdfs:// files
   *
   * @param parent the parent (may include the {@code hdfs://host:port} part
   * @param subdirName the name of the subdirectory to add
   * @return the parent path with the sudirectory appended.
   */
  public static String addSubdirToPath(String parent, String subdirName) {

    String result =
      parent.toLowerCase().contains("://") ? (parent + "/" + subdirName)
        : parent + File.separator + subdirName;

    return result;
  }

  /**
   * Returns a Configuration object configured with the name node and port
   * present in the supplied path ({@code hdfs://host:port/path}). Also returns
   * the path-only part of the URI. Note that absolute paths will require an
   * extra /. E.g. {@code hdfs://host:port//users/fred/input}. Also handles
   * local files system paths if no protocol is supplied - e.g. bob/george for a
   * relative path (relative to the current working directory) or /bob/george
   * for an absolute path.
   *
   * @param path the URI or local path from which to configure
   * @param pathOnly will hold the path-only part of the URI
   * @return a Configuration object
   */
  public static Configuration getFSConfigurationForPath(String path,
    String[] pathOnly) {
    if (!path.toLowerCase().contains("://")) {
      // assume local file system
      Configuration conf = new Configuration();
      conf.set(SparkJobConfig.HADOOP_FS_DEFAULT_NAME, "file:/");

      // convert path to absolute if necessary
      File f = new File(path);
      if (!f.isAbsolute()) {
        path = f.getAbsolutePath();
      }

      pathOnly[0] = path;

      return conf;
    }

    Configuration conf = new Configuration();
    // extract protocol,host and port part (hdfs://host:port//)
    String portAndHost = path.substring(0, path.lastIndexOf(":"));
    String portPlusRemainder =
      path.substring(path.lastIndexOf(":") + 1, path.length());
    String port =
      portPlusRemainder.substring(0, portPlusRemainder.indexOf("/"));
    String filePath =
      portPlusRemainder.substring(portPlusRemainder.indexOf("/") + 1,
        portPlusRemainder.length());

    conf.set(SparkJobConfig.HADOOP_FS_DEFAULT_NAME, portAndHost + ":" + port);
    if (pathOnly != null && pathOnly.length > 0) {
      pathOnly[0] = filePath;
    }

    return conf;
  }

  /**
   * Takes an input path and returns a fully qualified absolute one. Handles
   * local and non-local file systems. Original path can be a relative one. In
   * the case of a local file system, the relative path (relative to the working
   * directory) is made into an absolute one. In the case of a file system such
   * as HDFS, an absolute path will require an additional '/' - E.g.
   * {@code hdfs://host:port//users/fred/input} - otherwise it will be treated
   * as relative (to the user's home directory in HDFS). In either case, the
   * returned path will be an absolute one.
   *
   * @param original original path (either relative or absolute) on a file
   *          system
   * @return absolute path
   * @throws IOException if a problem occurs
   */
  public static String resolveLocalOrOtherFileSystemPath(String original)
    throws IOException {

    String[] pathOnly = new String[1];
    Configuration conf = getFSConfigurationForPath(original, pathOnly);
    String result;

    FileSystem fs = FileSystem.get(conf);
    Path p = new Path(pathOnly[0]);
    p = p.makeQualified(fs);

    if (!p.toString().toLowerCase().startsWith("file:/")) {
      result = p.toString();
    } else {
      result = pathOnly[0];
    }

    return result;
  }

  /**
   * Delete a directory (and all contents).
   *
   * @param path the path to the directory to delete
   * @throws IOException if the path is not a directory or a problem occurs
   */
  public static void deleteDirectory(String path) throws IOException {
    String[] pathOnly = new String[1];
    Configuration conf = getFSConfigurationForPath(path, pathOnly);

    FileSystem fs = FileSystem.get(conf);
    Path p = new Path(pathOnly[0]);

    if (fs.isFile(p)) {
      throw new IOException("The path '" + pathOnly[0]
        + "' is not a directory!");
    }

    fs.delete(p, true);
  }

  /**
   * Opens the named file for reading on either the local file system or HDFS.
   * HDFS files should use the form "{@code hdfs://host:port/<path>}"
   *
   * @param file the file to open for reading on either the local or HDFS file
   *          system
   * @return an InputStream for the file
   * @throws IOException if a problem occurs
   */
  public static InputStream openFileForRead(String file) throws IOException {

    InputStream result;
    if (file.toLowerCase().indexOf("://") > 0) {
      String[] pathOnly = new String[1];
      Configuration conf = getFSConfigurationForPath(file, pathOnly);

      FileSystem fs = FileSystem.get(conf);
      Path path = new Path(pathOnly[0]);
      result = fs.open(path);
    } else {
      // local file
      result = new FileInputStream(file);
    }

    return result;
  }

  /**
   * Open the named file for writing to on either the local file system or HDFS.
   * HDFS files should use the form "{@code hdfs://host:port/<path>}". Note
   * that, on the local file system, the directory path must exist. Under HDFS,
   * the path is created automatically.
   *
   * @param file the file to write to
   * @return an OutputStream for writing to the file
   * @throws IOException if a problem occurs
   */
  public static OutputStream openFileForWrite(String file) throws IOException {
    OutputStream result;

    String[] pathOnly = new String[1];
    Configuration conf = getFSConfigurationForPath(file, pathOnly);

    FileSystem fs = FileSystem.get(conf);
    Path p = new Path(pathOnly[0]);
    result = fs.create(p);

    return result;
  }

  /**
   * Open the named file as a text file for writing to on either the local file
   * system or any other protocol specific file system supported by Hadoop.
   * Protocol files should use the form "{@code protocol://host:port/<path>}."
   * Note that, on the local file system, the directory path must exist.
   *
   * @param file the file to write to
   * @return an PrintWriter for writing to the file
   * @throws IOException if a problem occurs
   */
  public static PrintWriter openTextFileForWrite(String file)
    throws IOException {
    PrintWriter result;
    String[] pathOnly = new String[1];
    Configuration conf = getFSConfigurationForPath(file, pathOnly);

    FileSystem fs = FileSystem.get(conf);
    Path p = new Path(pathOnly[0]);

    OutputStream fout = fs.create(p);

    result = new PrintWriter(new BufferedWriter(new OutputStreamWriter(fout)));

    return result;
  }

  private static boolean checkExists(String fileOrDir, boolean isDir)
    throws IOException {
    if (fileOrDir.toLowerCase().indexOf("://") > 0) {
      String[] pathOnly = new String[1];
      Configuration conf = getFSConfigurationForPath(fileOrDir, pathOnly);

      FileSystem fs = FileSystem.get(conf);
      Path path = new Path(pathOnly[0]);

      return fs.exists(path)
        && (isDir ? fs.isDirectory(path) : fs.isFile(path));
    } else {
      File f = new File(fileOrDir);
      return f.exists() && (isDir ? f.isDirectory() : f.isFile());
    }
  }

  /**
   * Check that the named file exists on either the local file system or HDFS.
   *
   * @param file the file to check
   * @return true if the file exists on the local file system or in HDFS
   * @throws IOException if a problem occurs
   */
  public static boolean checkFileExists(String file) throws IOException {
    return checkExists(file, false);
  }

  /**
   * Check that the named directory exists on either the local file system or
   * HDFS
   * 
   * @param dir the directory to check
   * @return true if the directory exists on the local file system or in HDFS
   * @throws IOException if a problem occurs
   */
  public static boolean checkDirExists(String dir) throws IOException {
    return checkExists(dir, true);
  }

  /**
   * Get the size in bytes of a file/directory
   *
   * @param path the path to the file/directory
   * @return the size in bytes
   * @throws IOException if a problem occurs
   */
  public static long getSizeInBytesOfPath(String path) throws IOException {
    String[] pathHolder = new String[1];
    Configuration conf = getFSConfigurationForPath(path, pathHolder);
    FileSystem fs = FileSystem.get(conf);
    Path p = new Path(pathHolder[0]);
    return fs.getContentSummary(p).getLength();
  }

  /**
   * Initialize and return an appender for hooking into Spark's log4j logging
   * and directing it to Weka's log
   *
   * @param job the job to initialize the appender for
   * @return an appender
   */
  public static WriterAppender initSparkLogAppender(DistributedJob job) {
    WekaLoggingPrintWriter sparkToWeka = new WekaLoggingPrintWriter(job);

    WriterAppender appender =
      new WriterAppender(new SimpleLayout(), sparkToWeka);

    Logger sparkLog = Logger.getLogger("org.apache.spark");
    if (sparkLog != null) {
      sparkLog.addAppender(appender);

      return appender;
    }

    return null;
  }

  /**
   * Remove the supplied appender from Spark's logging. This should be called at
   * the end of a job
   *
   * @param appender the appender to remove
   */
  public static void removeSparkLogAppender(WriterAppender appender) {
    if (appender != null) {
      Logger sparkLog = Logger.getLogger("org.apache.spark");
      if (sparkLog != null) {
        sparkLog.removeAppender(appender);
      }
    }
  }

  /**
   * Attempts to determine the available cluster memory (in bytes).
   *
   * TODO not sure if this actually works reliably
   *
   * @param sparkContext the spark context to use
   * @return the total available cluster memory.
   */
  protected static long determineClusterMemory(JavaSparkContext sparkContext) {
    scala.collection.Map<String, Tuple2<Object, Object>> memoryMap =
      JavaSparkContext.toSparkContext(sparkContext).getExecutorMemoryStatus();

    long totalMem = 0L;
    Map<String, Tuple2<Object, Object>> jMap =
      scala.collection.JavaConversions.asJavaMap(memoryMap);
    for (Tuple2<Object, Object> v : jMap.values()) {
      totalMem += ((Number) v._2()).longValue();
    }

    return totalMem;
  }

  /**
   * Used for hooking into Spark's log4j logging via a WriterAppender
   *
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  public static class WekaLoggingPrintWriter extends PrintWriter {

    /** Holds the log */
    protected DistributedJob m_logHolder;

    public WekaLoggingPrintWriter() {
      super(System.out);
    }

    public WekaLoggingPrintWriter(DistributedJob job) {
      this();
      m_logHolder = job;
    }

    @Override
    public void println(String string) {
      if (m_logHolder.getLog() != null) {
        if (string.endsWith("\n")) {
          string = string.substring(0, string.length() - 1);
        }
        m_logHolder.getLog().logMessage(string);
      }
    }

    @Override
    public void println(Object obj) {
      println(obj.toString());
    }

    @Override
    public void write(String string) {
      println(string);
    }

    @Override
    public void print(String string) {
      println(string);
    }

    @Override
    public void print(Object obj) {
      print(obj.toString());
    }
  }
}
