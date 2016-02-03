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
 *    HDFSConfig.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package distributed.hadoop;

import distributed.core.DistributedJobConfig;
import org.apache.hadoop.conf.Configuration;
import weka.core.Environment;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;

import java.util.Enumeration;
import java.util.Map;
import java.util.Vector;

/**
 * Config for HDFS filesystem related properties
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class HDFSConfig extends AbstractHadoopJobConfig implements
  OptionHandler {

  /** For serialization */
  private static final long serialVersionUID = -5422180390380034244L;

  /** Key for the HDFS host (name node) */
  public static final String HDFS_HOST = "hdfsHost";

  /** Key for the HDFS port */
  public static final String HDFS_PORT = "hdfsPort";

  /** property in core-site.xml that specifies the host running the namenode */
  public static final String HADOOP_FS_DEFAULT_NAME =
    isHadoop2() ? "fs.defaultFS" : "fs.default.name";

  /**
   * property in the core-site.xml that specifies how many times to try to
   * contact the name node before giving up
   */
  public static final String IPC_CLIENT_CONNECT_MAX_RETRIES =
    "ipc.client.connect.max.retries";

  /** Default port for the name node */
  public static final String DEFAULT_PORT = "8020";

  /** Default host for the name node */
  public static final String DEFAULT_HOST = "localhost";

  /**
   * Constructor - sets defaults
   */
  public HDFSConfig() {
    setHDFSHost(DEFAULT_HOST);
    setHDFSPort(DEFAULT_PORT);
  }

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.addElement(new Option("\tThe HDFS hostname. (default: localhost)",
      "hdfs-host", 1, "-hdfs-host <host name>"));

    result.addElement(new Option("\tThe HDFS port. (default: 8020)",
      "hdfs-port", 1, "-hdfs-port <port number>"));

    Enumeration<Option> superOpts = super.listOptions();
    while (superOpts.hasMoreElements()) {
      result.add(superOpts.nextElement());
    }

    return result.elements();
  }

  @Override
  public void setOptions(String[] options) throws Exception {
    super.setOptions(options);

    String host = Utils.getOption("hdfs-host", options);

    if (!isEmpty(host)) {
      setHDFSHost(host);
    } else {
      setHDFSHost(DEFAULT_HOST);
    }

    String port = Utils.getOption("hdfs-port", options);
    if (!isEmpty(port)) {
      setHDFSPort(port);
    } else {
      setHDFSPort(DEFAULT_PORT);
    }
  }

  @Override
  public String[] getOptions() {
    Vector<String> result = new Vector<String>();

    if (!DistributedJobConfig.isEmpty(getHDFSHost())) {
      result.add("-hdfs-host");
      result.add(getHDFSHost());
    }
    if (!DistributedJobConfig.isEmpty(getHDFSPort())) {
      result.add("-hdfs-port");
      result.add(getHDFSPort());
    }

    String[] superOpts = super.getOptions();
    for (String s : superOpts) {
      result.add(s);
    }

    return result.toArray(new String[result.size()]);
  }

  /**
   * Get the tool tip text for this property
   * 
   * @return the tool tip text for this property
   */
  public String HDFSHostTipText() {
    return "The HDFS (name node) host to use";
  }

  /**
   * Set the HDFS (name node) host to use
   * 
   * @param host the name node's host
   */
  public void setHDFSHost(String host) {
    setProperty(HDFS_HOST, host);
  }

  /**
   * Get the HDFS (name node) host to use
   * 
   * @return the name node's host
   */
  public String getHDFSHost() {
    return getProperty(HDFS_HOST);
  }

  /**
   * Get the tool tip text for this property
   * 
   * @return the tool tip text for this property
   */
  public String HDFSPortTipText() {
    return "The HDFS (name node) port";
  }

  /**
   * Set the HDFS (name node) port
   * 
   * @param port the port that the name node is running on
   */
  public void setHDFSPort(String port) {
    setProperty(HDFS_PORT, port);
  }

  /**
   * Get the HDFS (name node) port
   * 
   * @return the port that the name node is running on
   */
  public String getHDFSPort() {
    return getProperty(HDFS_PORT);
  }

  /**
   * Build an HDFS URL from the settings in the supplied HDFSConfig
   * 
   * @param config the HDFS config to use for building the URL
   * @param env environment variables
   * @return a HDFS URL as a String
   */
  public static String constructHostURL(HDFSConfig config, Environment env) {
    String url = "hdfs://" + config.getHDFSHost() + ":" + config.getHDFSPort();

    try {
      if (env != null) {
        url = env.substitute(url);
      }
    } catch (Exception ex) {
    }

    return url;
  }

  /**
   * Populate the supplied Configuration object with HDFS-related settings
   * 
   * @param conf the Configuration object to populate
   * @param env environment variables
   */
  public void configureForHadoop(Configuration conf, Environment env) {
    // transfer over the properties to Hadoop ones
    conf.set(HDFSConfig.HADOOP_FS_DEFAULT_NAME,
      HDFSConfig.constructHostURL(this, env));

    for (Map.Entry<String, String> e : getUserSuppliedProperties().entrySet()) {

      // '*' indicates special job-related properties for use by Weka map/reduce
      // tasks. No need to copy these over here as each job in question will
      // make sure that their specific props are copied over.
      if (!e.getKey().startsWith("*")) {
        if (!DistributedJobConfig.isEmpty(e.getValue())) {
          conf.set(e.getKey(), e.getValue());
        }
      }
    }
  }
}
