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
 *    HDFSSaver.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.converters;

import distributed.core.DistributedJobConfig;
import distributed.hadoop.HDFSConfig;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.CapabilitiesHandler;
import weka.core.CommandlineRunnable;
import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.Instance;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Saver for writing to HDFS. Delegates to a base saver for writing the actual
 * file format.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class HDFSSaver extends AbstractSaver implements IncrementalConverter,
  BatchConverter, OptionHandler, CapabilitiesHandler, EnvironmentHandler,
  CommandlineRunnable {

  /** For serialization */
  private static final long serialVersionUID = -7764527880435055342L;

  /** The base saver */
  protected AbstractFileSaver m_delegate = new CSVSaver();

  /**
   * The path to (primarily) the directory to save to in HDFS - if a filename is
   * given at the end of the path, then this forms the file prefix for the saved
   * file. This is because multiple data sets (i.e. train/test x-val folds) may
   * enter this saver
   */
  protected String m_hdfsPath = "/"; //$NON-NLS-1$

  /** Configuration of HDFS hostname and port */
  protected HDFSConfig m_config = new HDFSConfig();

  /** Set to true once the first instance has been seen in incremental mode */
  protected boolean m_incrementalInit;

  /**
   * Holds the relation name of the incoming data if user has opted to have the
   * relation name as part of the filename
   */
  protected String m_relationNamePartOfFilename = ""; //$NON-NLS-1$

  /**
   * File prefix to use if user is not using the relation name as part of the
   * filename and the path supplied is a directory rather than a filename
   */
  protected String m_filePrefix = ""; //$NON-NLS-1$

  /** Environment variables */
  protected transient Environment m_env;

  /** The replication factor in HDFS */
  protected String m_replicationFactor = ""; //$NON-NLS-1$

  /**
   * Constructor
   */
  public HDFSSaver() {
    resetOptions();
  }

  /**
   * Help information for the HDFSSaver
   * 
   * @return textual help information
   */
  public String globalInfo() {
    return "Write files to HDFS using a base saver."; //$NON-NLS-1$
  }

  @Override
  public void resetOptions() {
    super.resetOptions();

    m_delegate.resetOptions();

    if (m_delegate instanceof CSVSaver) {
      // make sure that no header no header row is used
      ((CSVSaver) m_delegate).setNoHeaderRow(true);
    }
  }

  /**
   * Returns the Capabilities of this saver.
   * 
   * @return the capabilities of this object
   * @see Capabilities
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = null;

    if (getSaver() != null) {
      result = getSaver().getCapabilities();
    } else {
      result = super.getCapabilities();
      result.disableAll();
    }

    // set dependencies
    for (Capability cap : Capability.values()) {
      result.enableDependency(cap);
    }

    result.setOwner(this);

    return result;
  }

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.addElement(new Option(
      "\tThe path to the destination file/directory in HDFS.", "dest", 1, //$NON-NLS-1$ //$NON-NLS-2$
      "-dest <path>")); //$NON-NLS-1$

    result.addElement(new Option(
      "\tThe HDFS replication factor (default = default for cluster).", //$NON-NLS-1$
      "dfs-replication", 1, "-dfs-replication <integer>")); //$NON-NLS-1$ //$NON-NLS-2$

    result
      .addElement(new Option(
        "\tThe fully qualified name of the underlying saver to use, followed by its options\n\t" //$NON-NLS-1$
          + ". E.g. \"weka.core.converters.CSVSaver -decimal 8\".\n\t" //$NON-NLS-1$
          + "(default: weka.core.converters.CSVSaver", "saver", 1, //$NON-NLS-1$ //$NON-NLS-2$
        "-saver <saver>")); //$NON-NLS-1$

    Enumeration<Option> hdfsOpts = new HDFSConfig().listOptions();
    while (hdfsOpts.hasMoreElements()) {
      result.addElement(hdfsOpts.nextElement());
    }

    return result.elements();
  }

  @Override
  public void setOptions(String[] options) throws Exception {
    m_config = new HDFSConfig();
    String path = Utils.getOption("dest", options); //$NON-NLS-1$

    if (!DistributedJobConfig.isEmpty(path)) {
      setHDFSPath(path);
    }

    String factor = Utils.getOption("dfs-replication", options); //$NON-NLS-1$
    if (!DistributedJobConfig.isEmpty(factor)) {
      setDFSReplicationFactor(factor);
    }

    String saverSpec = Utils.getOption("saver", options); //$NON-NLS-1$

    if (!DistributedJobConfig.isEmpty(saverSpec)) {
      String[] split = Utils.splitOptions(saverSpec);
      String saverClass = split[0];
      split[0] = ""; //$NON-NLS-1$

      AbstractFileSaver s = (AbstractFileSaver) Utils.forName(
        AbstractFileSaver.class, saverClass, split);

      setSaver(s);
    }

    m_config.setOptions(options);
  }

  @Override
  public String[] getOptions() {
    Vector<String> result = new Vector<String>();

    result.add("-dest"); //$NON-NLS-1$
    result.add(getHDFSPath());

    if (!DistributedJobConfig.isEmpty(getDFSReplicationFactor())) {
      result.add("-dfs-replication"); //$NON-NLS-1$
      result.add(getDFSReplicationFactor());
    }

    result.add("-saver"); //$NON-NLS-1$
    String saverSpec = m_delegate.getClass().getName();
    if (m_delegate != null) {
      saverSpec += " " //$NON-NLS-1$
        + Utils.joinOptions(((OptionHandler) m_delegate).getOptions());
    }
    result.add(saverSpec);

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
   * The tip text for this property
   * 
   * @return the tool tip text for this property
   */
  public String DFSReplicationFactorTipText() {
    return "The replication factor to set for the file in HDFS"; //$NON-NLS-1$
  }

  /**
   * Set the dfs replication factor to use
   * 
   * @param factor the factor to use
   */
  public void setDFSReplicationFactor(String factor) {
    m_replicationFactor = factor;
  }

  /**
   * Get the dfs replication factor to use
   * 
   * @return the factor to use
   */
  public String getDFSReplicationFactor() {
    return m_replicationFactor;
  }

  /**
   * The tip text for this property
   * 
   * @return the tool tip text for this property
   */
  public String HDFSPathTipText() {
    return "Set the path to save to in HDFS"; //$NON-NLS-1$
  }

  /**
   * Set the path to save to in HDFS
   * 
   * @param path the path to save to
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
   * The tip text for this property
   * 
   * @return the tool tip text for this property
   */
  public String saverTipText() {
    return "The base saver (file type) to use for saving"; //$NON-NLS-1$
  }

  /**
   * Set the base saver to use
   * 
   * @param saver the base saver to use
   */
  public void setSaver(AbstractFileSaver saver) {
    m_delegate = saver;
  }

  /**
   * Get the base saver to use
   * 
   * @return the base saver to use
   */
  public AbstractFileSaver getSaver() {
    return m_delegate;
  }

  @Override
  public String getRevision() {
    return "$Revision: ???"; //$NON-NLS-1$
  }

  /**
   * Constructs a final path save the data to
   * 
   * @return A final path
   */
  protected String constructFinalPath() {
    String url = m_hdfsPath;

    try {
      url = m_env.substitute(url);
    } catch (Exception ex) {
      // ignored
    }

    url = url.replace("hdfs://", "");
    if (url.endsWith("/")) { //$NON-NLS-1$
      url += DistributedJobConfig.isEmpty(m_filePrefix) ? "" : m_filePrefix; //$NON-NLS-1$
      url += DistributedJobConfig.isEmpty(m_relationNamePartOfFilename) ? "" //$NON-NLS-1$
        : m_relationNamePartOfFilename;
    } else {
      String namePart = url.substring(url.lastIndexOf('/') + 1, url.length());
      url = url.substring(0, url.lastIndexOf('/') + 1);
      if (!DistributedJobConfig.isEmpty(m_filePrefix)) {
        url += m_filePrefix + namePart;
      } else {
        url += namePart;
      }
    }

    try {
      url = m_env.substitute(url);
    } catch (Exception ex) {
      // ignored
    }

    if (!url.toLowerCase().endsWith(m_delegate.getFileExtension())) {
      url += m_delegate.getFileExtension();
    }

    return url;
  }

  @Override
  public void setFilePrefix(String prefix) {
    // being used as part of the filename and the user's hdfs path does not end
    // in filename (i.e just a directory)
    if (!prefix.equals("no-name")) { //$NON-NLS-1$
      m_filePrefix = prefix;
    }
  }

  @Override
  public String filePrefix() {
    return m_filePrefix;
  }

  @Override
  public void setDirAndPrefix(String relationName, String add) {
    if (relationName != null) {
      m_relationNamePartOfFilename = relationName;
    }
  }

  protected transient ClassLoader m_orig;

  @Override
  public void writeIncremental(Instance inst) throws IOException {
    // int writeMode = m_delegate.getWriteMode();

    if (getRetrieval() == BATCH || getRetrieval() == NONE) {
      throw new IOException("Batch and incremental saving cannot be mixed."); //$NON-NLS-1$
    }

    if (!m_incrementalInit) {
      m_orig = Thread.currentThread().getContextClassLoader();

      try {
        Thread.currentThread().setContextClassLoader(
          this.getClass().getClassLoader());
        String url = constructFinalPath();
        Path pt = new Path(url);
        Configuration conf = new Configuration();

        if (!DistributedJobConfig.isEmpty(getDFSReplicationFactor())) {
          m_config.setUserSuppliedProperty("dfs.replication", //$NON-NLS-1$
            getDFSReplicationFactor());
        }
        // this makes sure that any user-supplied properties (such as
        // dfs.replication) get set on the Configuration
        m_config.configureForHadoop(conf, m_env);

        // conf.set(HDFSConfig.HADOOP_FS_DEFAULT_NAME,
        //        HDFSConfig.constructHostURL(m_config, m_env)); //$NON-NLS-1$

        FileSystem fs = FileSystem.get(conf);
        // if the file already exists delete it.
        if (fs.exists(pt)) {
          // remove the file
          fs.delete(pt, true);
        }

        FSDataOutputStream fout = fs.create(pt);
        m_delegate.setRetrieval(INCREMENTAL);
        m_delegate.setInstances(getInstances());
        m_delegate.setDestination(fout);
        m_incrementalInit = true;
      } catch (IOException ex) {
        if (m_orig != null) {
          Thread.currentThread().setContextClassLoader(m_orig);
        }
        throw ex;
      }
    }

    m_delegate.writeIncremental(inst);

    if (inst == null) {
      // done - delegate will close the stream
      m_incrementalInit = false;

      if (m_orig != null) {
        Thread.currentThread().setContextClassLoader(m_orig);
      }
    }
  }

  @Override
  public void writeBatch() throws IOException {

    if (getInstances() == null) {
      throw new IOException("No instances to save"); //$NON-NLS-1$
    }

    if (getRetrieval() == INCREMENTAL) {
      throw new IOException("Batch and incremental saving cannot be mixed."); //$NON-NLS-1$
    }
    setRetrieval(BATCH);
    setWriteMode(WRITE);

    String url = constructFinalPath();

    ClassLoader orig = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(
        this.getClass().getClassLoader());
      Path pt = new Path(url);
      Configuration conf = new Configuration();

      if (!DistributedJobConfig.isEmpty(getDFSReplicationFactor())) {
        m_config.setUserSuppliedProperty("dfs.replication", //$NON-NLS-1$
          getDFSReplicationFactor());
      }
      // this makes sure that any user-supplied properties (such as
      // dfs.replication) get set on the Configuration
      m_config.configureForHadoop(conf, m_env);

      // conf.set(HDFSConfig.HADOOP_FS_DEFAULT_NAME,
      //        HDFSConfig.constructHostURL(m_config, m_env)); //$NON-NLS-1$

      FileSystem fs = FileSystem.get(conf);
      // if the file already exists delete it.
      if (fs.exists(pt)) {
        // remove the file
        fs.delete(pt, true);
      }

      FSDataOutputStream fout = fs.create(pt);
      m_delegate.setInstances(getInstances());
      m_delegate.setDestination(fout);
      m_delegate.writeBatch();
      setWriteMode(CANCEL);
    } catch (Exception ex) {
      throw new IOException(ex);
    } finally {
      Thread.currentThread().setContextClassLoader(orig);
    }
  }

  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
  }

  /**
   * Main method.
   * 
   * @param args should contain the options of a Saver.
   */
  public static void main(String[] args) {
    HDFSSaver saver = new HDFSSaver();

    saver.run(saver, args);
  }

  @Override
  public void preExecution() {
  }

  @Override
  public void postExecution() {
  }

  @Override
  public void run(Object toRun, String[] options) {

    if (!(toRun instanceof HDFSSaver)) {
      throw new IllegalArgumentException("Object to run is not an HDFSaver!"); //$NON-NLS-1$
    }
    DFSConverterUtils.runSaver((HDFSSaver) toRun, options);
  }
}
