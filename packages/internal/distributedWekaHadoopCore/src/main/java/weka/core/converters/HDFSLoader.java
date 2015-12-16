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
 *    HDFSLoader.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.converters;

import distributed.core.DistributedJobConfig;
import distributed.hadoop.HDFSConfig;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import weka.core.CommandlineRunnable;
import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Loader for data stored in HDFS. Delegates to a base loader to do the actual
 * data parsing/extraction to instances.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class HDFSLoader extends AbstractLoader implements BatchConverter,
  IncrementalConverter, EnvironmentHandler, OptionHandler, CommandlineRunnable {

  /** For serialization */
  private static final long serialVersionUID = -7174163429141110673L;

  /** The loader to delegate saving to */
  protected AbstractFileLoader m_delegate = new CSVLoader();

  /** The path in HDFS to load from */
  protected String m_hdfsPath = "/";

  /** Configuration of HDFS hostname and port */
  protected HDFSConfig m_config = new HDFSConfig();

  /** Environment variables */
  protected transient Environment m_env;

  /** Structure of the data being read */
  protected Instances m_structure;

  /**
   * Constructor
   * 
   * @throws Exception
   */
  public HDFSLoader() throws Exception {
    reset();
  }

  /**
   * Help information for the HDFSLoader
   * 
   * @return textual help information
   */
  public String globalInfo() {
    return "Read files from HDFS using a base loader.";
  }

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.addElement(new Option("\tThe path to source file in HDFS.", "source",
      1, "-source <path>"));

    result.addElement(new Option(
      "\tThe fully qualified name of the underlying loader to use, followed by its options\n\t"
        + ". E.g. \"weka.core.converters.CSVLoader -N first\".\n\t"
        + "(default: weka.core.converters.CSVLoader",
      "loader", 1, "-loader <loader>"));

    Enumeration<Option> hdfsOpts = new HDFSConfig().listOptions();
    while (hdfsOpts.hasMoreElements()) {
      result.addElement(hdfsOpts.nextElement());
    }

    return result.elements();
  }

  @Override
  public void setOptions(String[] options) throws Exception {
    m_config = new HDFSConfig();
    String path = Utils.getOption("source", options);

    if (!DistributedJobConfig.isEmpty(path)) {
      setHDFSPath(path);
    }

    String loaderSpec = Utils.getOption("loader", options);

    if (!DistributedJobConfig.isEmpty(loaderSpec)) {
      String[] split = Utils.splitOptions(loaderSpec);
      String loaderClass = split[0];
      split[0] = "";

      setLoader((AbstractFileLoader) Utils.forName(AbstractFileLoader.class,
        loaderClass, split));
    }

    m_config.setOptions(options);
  }

  @Override
  public String[] getOptions() {
    Vector<String> result = new Vector<String>();

    result.add("-source");
    result.add(getHDFSPath());

    result.add("-loader");
    String loaderSpec = m_delegate.getClass().getName();
    if (m_delegate instanceof OptionHandler) {
      loaderSpec +=
        " " + Utils.joinOptions(((OptionHandler) m_delegate).getOptions());
    }
    result.add(loaderSpec);

    for (String s : m_config.getOptions()) {
      result.add(s);
    }

    return result.toArray(new String[result.size()]);
  }

  /**
   * Get the HDFSConfig
   * 
   * @return the HDFSConfig
   */
  public HDFSConfig getConfig() {
    return m_config;
  }

  /**
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String HDFSPathTipText() {
    return "Set the path to load from in HDFS";
  }

  /**
   * Set the path to load from in HDFS
   * 
   * @param path the path to load from
   */
  public void setHDFSPath(String path) {
    m_hdfsPath = path;
  }

  /**
   * Get the path to save to in HDFS
   * 
   * @return the path to save to
   */
  public String getHDFSPath() {
    return m_hdfsPath;
  }

  /**
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String loaderTipText() {
    return "The base loader (file type) to use";
  }

  /**
   * Set the base loader to use
   * 
   * @param loader the base laoder to use
   */
  public void setLoader(AbstractFileLoader loader) {
    m_delegate = loader;
  }

  /**
   * Get the base loader to use
   * 
   * @return the base loader to use
   */
  public AbstractFileLoader getLoader() {
    return m_delegate;
  }

  /**
   * Construct a HDFS URL given the configuration of the loader
   * 
   * @return a HDFS URL as a string
   */
  protected String constructURL() {
    String url = m_hdfsPath;

    try {
      url = m_env.substitute(url);
    } catch (Exception ex) {
    }

    if (!url.toLowerCase().startsWith("hdfs://")) {
      url = "hdfs://" + m_config.getHDFSHost() + ":" + m_config.getHDFSPort()
        + (m_hdfsPath.startsWith("/") ? m_hdfsPath : "/" + m_hdfsPath);
    }

    try {
      url = m_env.substitute(url);
    } catch (Exception ex) {
    }

    return url;
  }

  @Override
  public String getRevision() {
    return "$Revision$";
  }

  @Override
  public void reset() throws Exception {
    super.reset();
    m_delegate.reset();
    m_structure = null;
  }

  @Override
  public Instances getStructure() throws IOException {

    if (m_structure == null) {
      String url = constructURL();

      ClassLoader orig = Thread.currentThread().getContextClassLoader();
      try {
        Thread.currentThread()
          .setContextClassLoader(this.getClass().getClassLoader());
        Path pt = new Path(url);
        Configuration conf = new Configuration();
        conf.set(HDFSConfig.HADOOP_FS_DEFAULT_NAME,
          HDFSConfig.constructHostURL(m_config, m_env));

        // the default of 10 is nutty
        conf.set(HDFSConfig.IPC_CLIENT_CONNECT_MAX_RETRIES, "" + 2);
        FileSystem fs = FileSystem.get(conf);

        if (!fs.exists(pt)) {
          throw new IOException(
            "The source file - \"" + url + "\" does not seem to exist in HDFS");
        }

        m_delegate.reset();
        FSDataInputStream fin = fs.open(pt);
        m_delegate.setSource(fin);
        m_structure = m_delegate.getStructure();
      } catch (java.net.ConnectException ce) {
        throw new StructureNotReadyException("Unable to connect to host "
          + m_config.getHDFSHost() + ":" + m_config.getHDFSPort());
      } finally {
        Thread.currentThread().setContextClassLoader(orig);
      }
    }

    return new Instances(m_structure, 0);
  }

  @Override
  public Instances getDataSet() throws IOException {

    if (getRetrieval() == INCREMENTAL) {
      throw new IOException(
        "Cannot mix getting instances in both incremental and batch modes");
    }
    setRetrieval(BATCH);

    Instances result = null;

    ClassLoader orig = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread()
        .setContextClassLoader(this.getClass().getClassLoader());
      String url = constructURL();
      Path pt = new Path(url);

      Configuration conf = new Configuration();
      conf.set(HDFSConfig.HADOOP_FS_DEFAULT_NAME,
        HDFSConfig.constructHostURL(m_config, m_env));

      // the default of 10 is nutty
      conf.set(HDFSConfig.IPC_CLIENT_CONNECT_MAX_RETRIES, "" + 2);
      FileSystem fs = FileSystem.get(conf);

      if (!fs.exists(pt)) {
        throw new Exception(
          "The source file - \"" + url + "\" does not seem to exist in HDFS");
      }

      m_delegate.reset();
      FSDataInputStream fin = fs.open(pt);
      m_delegate.setSource(fin);
      result = m_delegate.getDataSet();

    } catch (Exception ex) {
      throw new IOException(ex);
    } finally {
      Thread.currentThread().setContextClassLoader(orig);
    }

    m_structure = new Instances(result, 0);

    return m_delegate.getDataSet();
  }

  @Override
  public Instance getNextInstance(Instances structure) throws IOException {

    if (m_structure == null) {
      // start of new read
      getStructure();
    }

    Instance nextI = m_delegate.getNextInstance(structure);
    if (nextI == null) {
      // finished
      try {
        reset();
      } catch (Exception ex) {
        throw new IOException(ex);
      }
    }

    return nextI;
  }

  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
  }

  /**
   * Main method
   * 
   * @param args should contain the options of a Loader
   */
  public static void main(String[] args) {
    try {
      HDFSLoader loader = new HDFSLoader();
      loader.run(loader, args);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  @Override
  public void preExecution() {
  }

  @Override
  public void postExecution() {
  }

  @Override
  public void run(Object toRun, String[] options) {

    if (!(toRun instanceof HDFSLoader)) {
      throw new IllegalArgumentException(
        "Object to excecute is not an HDFSLoader!");
    }
    DFSConverterUtils.runLoader((HDFSLoader) toRun, options);
  }
}
