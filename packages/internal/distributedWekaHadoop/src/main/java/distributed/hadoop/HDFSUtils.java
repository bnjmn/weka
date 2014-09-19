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
 *    HDFSUtils.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package distributed.hadoop;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import weka.core.Environment;
import distributed.core.DistributedJobConfig;

/**
 * Bunch of utility routines for HDFS. Includes stuff for moving/copying files
 * in and out of HDFS, installing Weka libraries in HDFS, adding stuff to the
 * distributed cache for a job and populating the classpath for map/reduce
 * tasks.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class HDFSUtils {

  /**
   * The default location in HDFS to place weka.jar and other libraries for
   * inclusion in the classpath of the hadoop nodes
   */
  public static final String WEKA_LIBRARIES_LOCATION = "opt/nz/ac/waikato/cms/";

  /**
   * Users need to set HADOOP_ON_LINUX environment variable to "true" if
   * accessing a *nix Hadoop cluster from Windows so that we can post-process
   * the job classpath in the Configuration file to use ':' rather than ';' as
   * separators.
   */
  public static final String WINDOWS_ACCESSING_HADOOP_ON_LINUX_SYS_PROP =
    "HADOOP_ON_LINUX";

  /**
   * Staging location for non library files to be distributed to the nodes by
   * the distributed cache
   */
  public static final String WEKA_TEMP_DISTRIBUTED_CACHE_FILES =
    WEKA_LIBRARIES_LOCATION
      + "tmpDistributedCache/";

  /**
   * Utility method to resolve all environment variables in a given path
   * 
   * @param path the path in HDFS
   * @param env environment variables to use
   * @return the path with all environment variables resolved
   */
  public static String resolvePath(String path, Environment env) {
    if (env != null) {
      try {
        path = env.substitute(path);
      } catch (Exception ex) {
      }
    }

    return path;

    // if (path.startsWith("hdfs://")) {
    // return path;
    // }
    //
    // String uri = "hdfs://" + config.getHDFSHost() + ":" +
    // config.getHDFSPort()
    // // + (path.startsWith("/") ? path : "/" + path);
    // + path;
    //
    // if (env != null) {
    // try {
    // uri = env.substitute(uri);
    // } catch (Exception ex) {
    // }
    // }

    // return uri;
  }

  /**
   * Create our staging directory in HDFS (if necessary)
   * 
   * @param config the HDFSConfig containing connection details
   * @throws IOException if a problem occurs
   */
  protected static void createTmpDistributedCacheDirIfNecessary(
    HDFSConfig config) throws IOException {
    Configuration conf = new Configuration();
    config.configureForHadoop(conf, null);

    FileSystem fs = FileSystem.get(conf);

    Path p = new Path(resolvePath(WEKA_TEMP_DISTRIBUTED_CACHE_FILES, null));

    if (!fs.exists(p)) {
      fs.mkdirs(p);
    }
  }

  /**
   * Move a file from one location to another in HDFS
   * 
   * @param source the source path in HDFS
   * @param target the target path in HDFS
   * @param config the HDFSConfig with connection details
   * @param env environment variables
   * @throws IOException if a problem occurs
   */
  public static void moveInHDFS(String source, String target,
    HDFSConfig config, Environment env) throws IOException {

    createTmpDistributedCacheDirIfNecessary(config);

    Path sourceP = new Path(resolvePath(source, env));
    Path targetP = new Path(resolvePath(target, env));

    Configuration conf = new Configuration();
    config.configureForHadoop(conf, env);

    FileSystem fs = FileSystem.get(conf);

    if (fs.exists(targetP)) {
      fs.delete(targetP, true);
    }

    fs.rename(sourceP, targetP);
  }

  /**
   * Copy a local file into HDFS
   * 
   * @param localFile the path to the local file
   * @param hdfsPath the destination path in HDFS
   * @param config the HDFSConfig containing connection details
   * @param env environment variables
   * @param overwrite true if the destination should be overwritten (if it
   *          already exists)
   * @throws IOException if a problem occurs
   */
  public static void copyToHDFS(String localFile, String hdfsPath,
    HDFSConfig config, Environment env, boolean overwrite) throws IOException {
    File local = new File(localFile);
    URI localURI = local.toURI();

    Path localPath = new Path(localURI);

    Path destPath = new Path(resolvePath(hdfsPath, env));

    Configuration conf = new Configuration();
    // conf.set(HDFSConfig.FS_DEFAULT_NAME,
    // HDFSConfig.constructHostURL(config, env));
    config.configureForHadoop(conf, env);

    FileSystem fs = FileSystem.get(conf);

    // only copy if the file doesn't exist or overwrite is specified
    if (!fs.exists(destPath) || overwrite) {
      if (fs.exists(destPath)) {
        fs.delete(destPath, true);
      }
      fs.copyFromLocalFile(localPath, destPath);
    }
  }

  /**
   * Copy a set of local files into the Weka installation directory in HDFS
   * 
   * @param localFiles a list of local files to copy
   * @param config the HDFSConfig containing connection details
   * @param env environment variables
   * @param overwrite true if the destination file should be overwritten (if it
   *          exists already)
   * @throws IOException if a problem occurs
   */
  public static void copyFilesToWekaHDFSInstallationDirectory(
    List<String> localFiles, HDFSConfig config, Environment env,
    boolean overwrite) throws IOException {

    for (String local : localFiles) {
      String hdfsDest = local.substring(local.lastIndexOf(File.separator) + 1,
        local.length());
      hdfsDest = WEKA_LIBRARIES_LOCATION + hdfsDest;

      copyToHDFS(local, hdfsDest, config, env, overwrite);
    }
  }

  /**
   * If accessing Hadoop running on a *nix system from Windows then we have to
   * post-process the classpath setup for the job because it will contain ';'
   * rather than ':' as the separator.
   * 
   * @param conf the Configuration to fix up.
   */
  public static void checkForWindowsAccessingHadoopOnLinux(Configuration conf) {
    String v = System.getenv(WINDOWS_ACCESSING_HADOOP_ON_LINUX_SYS_PROP);
    if (v == null || v.toLowerCase().equals("false")) {
      return;
    }

    // replace ; path separators with : in the mapred.job.classpath.files or
    // we will get class not found errors
    String classpath = conf.get("mapred.job.classpath.files");
    if (!DistributedJobConfig.isEmpty(classpath)) {
      classpath = classpath.replaceAll(";", ":");
      conf.set("mapred.job.classpath.files", classpath);
    }
  }

  /**
   * Adds a file in HDFS to the classpath for hadoop nodes (via the
   * DistributedCache)
   * 
   * @param hdfsConfig the HDFSConfig object with host and port set
   * @param conf the Configuration object that will be changed by this operation
   * @param path the path to the file (in HDFS) to be added to the classpath for
   *          hadopp nodes
   * @param env any environment variables
   * @throws IOException if a problem occurs
   */
  public static void addFileToClasspath(HDFSConfig hdfsConfig,
    Configuration conf, String path, Environment env) throws IOException {

    // conf.set(HDFSConfig.FS_DEFAULT_NAME,
    // HDFSConfig.constructHostURL(hdfsConfig, env));
    hdfsConfig.configureForHadoop(conf, env);

    FileSystem fs = FileSystem.get(conf);

    if (path.startsWith("hdfs://")) {
      throw new IOException("Path should not include 'hdfs://host:port'");
    }
    if (env != null) {
      try {
        path = env.substitute(path);
      } catch (Exception ex) {
      }
    }
    // if (!path.startsWith("/")) {
    // path = "/" + path;
    // }

    // We know that all job-specific jars are installed in the user's home
    // directory
    Path destPath = new Path(path);
    String userHome = fs.getHomeDirectory().toString();
    String absolutePath = userHome + "/" + destPath.toString();
    if (absolutePath.startsWith("hdfs://")) {
      // strip this off - for some reason under CDH4
      // DistributedCache.addFileToClassPath() keeps the hdfs:// part
      // of the URL in the classpath spec! Apache does not do this.
      absolutePath = absolutePath.replace("hdfs://", "");
      absolutePath = absolutePath.substring(absolutePath.indexOf("/"),
        absolutePath.length());
    }
    destPath = new Path(absolutePath);

    DistributedCache.addFileToClassPath(destPath, conf, fs);

    checkForWindowsAccessingHadoopOnLinux(conf);
  }

  /**
   * Adds a set of files in HDFS to the classpath for hadoop nodes (via the
   * DistributedCache)
   * 
   * @param hdfsConfig the HDFSConfig object with host and port set
   * @param conf the Configuration object that will be changed by this operation
   * @param paths a list of paths (in HDFS) to be added to the classpath for
   *          hadopp nodes
   * @param env any environment variables
   * @throws IOException if a problem occurs
   */
  public static void addFilesClasspath(HDFSConfig hdfsConfig,
    Configuration conf, List<String> paths, Environment env) throws IOException {
    for (String path : paths) {
      addFileToClasspath(hdfsConfig, conf, path, env);
    }
  }

  /**
   * Add a list of files, relative to the root of the Weka installation
   * directory in HDFS (i.e. WEKA_LIBRARIES_LOCATION), to the classpath for the
   * mappers and reducers.
   * 
   * @param hdfsConfig
   * @param conf
   * @param paths a list of paths (relative to the Weka installation root in
   *          HDFS) to add to the classpath for mappers and reducers
   * @param env
   * @throws IOException
   */
  public static void addWekaInstalledFilesToClasspath(HDFSConfig hdfsConfig,
    Configuration conf, List<String> paths, Environment env) throws IOException {

    List<String> fullyQualifiedPaths = new ArrayList<String>();

    for (String p : paths) {
      fullyQualifiedPaths.add(WEKA_LIBRARIES_LOCATION + p);
    }

    addFilesClasspath(hdfsConfig, conf, fullyQualifiedPaths, env);
  }

  /**
   * Delete a directory in HDFS
   * 
   * @param hdfsConfig the HDFSConfig to use with connection details set
   * @param conf the Configuration object
   * @param path the path to delete
   * @param env environment variables
   * @throws IOException if a problem occurs
   */
  public static void deleteDirectory(HDFSConfig hdfsConfig, Configuration conf,
    String path, Environment env) throws IOException {
    hdfsConfig.configureForHadoop(conf, env);

    FileSystem fs = FileSystem.get(conf);

    fs.delete(new Path(path), true);
  }

  /**
   * Delete a file in HDFS
   * 
   * @param hdfsConfig the HDFSConfig to use with connection details set
   * @param conf the Configuration object
   * @param path the path to delete
   * @param env environment variables
   * @throws IOException if a problem occurs
   */
  public static void deleteFile(HDFSConfig hdfsConfig, Configuration conf,
    String path, Environment env) throws IOException {
    deleteDirectory(hdfsConfig, conf, path, env);
  }

  /**
   * Serializes the given object into a file in the staging area in HDFS and
   * then adds that file to the distributed cache for the configuration
   * 
   * @param toSerialize the object to serialize
   * @param hdfsConfig the hdfs configuration to use
   * @param conf the job configuration to configure
   * @param fileNameInCache the file name only for the serialized object in the
   *          cache
   * @param env environment variables
   * @throws IOException if a problem occurs
   */
  public static void serializeObjectToDistributedCache(Object toSerialize,
    HDFSConfig hdfsConfig, Configuration conf, String fileNameInCache,
    Environment env) throws IOException {

    createTmpDistributedCacheDirIfNecessary(hdfsConfig);
    hdfsConfig.configureForHadoop(conf, env);

    String path = WEKA_TEMP_DISTRIBUTED_CACHE_FILES + fileNameInCache;
    path = resolvePath(path, env);
    Path stage = new Path(path);

    FileSystem fs = FileSystem.get(conf);

    // delete any existing file
    fs.delete(stage, true);
    FSDataOutputStream os = fs.create(stage);
    BufferedOutputStream br = new BufferedOutputStream(os);
    ObjectOutputStream oos = new ObjectOutputStream(br);
    try {
      oos.writeObject(toSerialize);
    } finally {
      oos.flush();
      oos.close();
    }

    addFileToDistributedCache(hdfsConfig, conf, path, env);
  }

  /**
   * Adds a file to the distributed cache for the supplied Configuration
   * 
   * @param hdfsConfig the hdfs configuration to use
   * @param conf the job configuration to configure
   * @param path the path to the file to add. This can be a local file, in which
   *          case it is first staged in HDFS, or a file in HDFS.
   * @param env environment variables
   * @return the filename only part of the path (this is what will be accessible
   *         from the distributed cache to a client via standard Java file IO)
   * @throws IOException if a problem occurs
   */
  public static String addFileToDistributedCache(HDFSConfig hdfsConfig,
    Configuration conf, String path, Environment env) throws IOException {

    createTmpDistributedCacheDirIfNecessary(hdfsConfig);

    // conf.set(HDFSConfig.FS_DEFAULT_NAME,
    // HDFSConfig.constructHostURL(hdfsConfig, env));
    hdfsConfig.configureForHadoop(conf, env);

    // FileSystem fs = FileSystem.get(conf);

    if (path.startsWith("hdfs://")) {
      throw new IOException("Path should not include 'hdfs://host:port'");
    }
    if (env != null) {
      try {
        path = env.substitute(path);
      } catch (Exception ex) {
      }
    }

    File local = new File(path);
    if (local.exists()) {
      String fileNameOnly = "";
      if (path.lastIndexOf(File.separator) >= 0) {
        fileNameOnly = path.substring(path.lastIndexOf(File.separator)
          + File.separator.length(), path.length());
      } else {
        fileNameOnly = path;
      }

      // copy the local file into HDFS staging
      path = WEKA_TEMP_DISTRIBUTED_CACHE_FILES + fileNameOnly;
      copyToHDFS(local.toString(), path, hdfsConfig, env, true);
    }

    // if (!path.startsWith("/")) {
    // path = "/" + path;
    // }

    String fileNameOnly = path.substring(path.lastIndexOf('/') + 1,
      path.length());

    try {
      DistributedCache.addCacheFile(
        new URI(resolvePath(path + "#" + fileNameOnly, env)), conf);
    } catch (URISyntaxException e) {
      throw new IOException(e);
    }
    DistributedCache.createSymlink(conf);

    return fileNameOnly;
  }

  /**
   * Adds a set of files to the distributed cache for the supplied Configuration
   * 
   * @param hdfsConfig the hdfs configuration to use
   * @param conf the job configuration to configure
   * @param paths a list of paths to to add to the distributed cache. These can
   *          be a local files, in which case they are first staged in HDFS, or
   *          a files in HDFS, or a mixture of both.
   * @param env environment variables from the distributed cache to a client via
   *          standard Java file IO)
   * @throws IOException if a problem occurs
   */
  public static void addFilesToDistributedCache(HDFSConfig hdfsConfig,
    Configuration conf, List<String> paths, Environment env) throws Exception {

    for (String s : paths) {
      addFileToDistributedCache(hdfsConfig, conf, s, env);
    }
  }

  public static void main(String[] args) {
    try {

      // test copying a file to hdfs
      String host = args[0];
      String port = args[1];
      String local = args[2];
      String dest = args[3];

      HDFSConfig config = new HDFSConfig();
      config.setHDFSHost(host);
      config.setHDFSPort(port);

      copyToHDFS(local, dest, config, null, true);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

}
