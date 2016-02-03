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
 *    AbstractHadoopJobConfig.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package distributed.hadoop;

import distributed.core.DistributedJobConfig;
import weka.core.Utils;

import java.util.Properties;

/**
 * Base job config for Hadoop-related jobs to extend
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public abstract class AbstractHadoopJobConfig extends DistributedJobConfig {

  /**
   * Location of properties - mainly to specify whether we are using Hadoop 1 or
   * 2.
   */
  protected static final String HADOOP_PROPS =
    "distributed/hadoop/Hadoop.props";

  /** Key for checking hadoop version in properties */
  protected static final String VERSION_KEY = "weka.distributed.hadoop.hadoop2";

  /** Key for checking which weka distributed hadoop package is installed */
  protected static final String PACKAGE_KEY =
    "weka.distributed.hadoop.packageName";

  protected static Properties WEKA_HADOOP_PROPS;

  /** For serialization */
  private static final long serialVersionUID = -4170580935543278227L;

  /** Key for job tracker host name */
  public static final String JOBTRACKER_HOST = "jobtrackerHost";

  /** Key for job tracker port */
  public static final String JOBTRACKER_PORT = "jobtrackerPort";

  /** Default jobtracker host */
  public static final String DEFAULT_HOST = "localhost";

  /** Default jobtracker port */
  public static final String DEFAULT_PORT = "8021";

  /** Default resource manager port under YARN */
  public static final String DEFAULT_PORT_YARN = "8032";

  /**
   * Load the properties file
   */
  protected static void loadProps() {
    if (WEKA_HADOOP_PROPS == null) {
      try {
        WEKA_HADOOP_PROPS = Utils.readProperties(HADOOP_PROPS);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  /**
   * Returns true if we are running against Hadoop2/Yarn
   *
   * @return true if we are running against Hadoop2/Yarn
   */
  public static boolean isHadoop2() {
    loadProps();
    return WEKA_HADOOP_PROPS.getProperty(VERSION_KEY, "false").equals("true");
  }

  /**
   * Get the tool tip text for this property
   * 
   * @return the tool tip text for this property
   */
  public String jobTrackerHostTipText() {
    return "The host that the job tracker/resource manager is running on";
  }

  /**
   * Set the job tracker host name to use
   * 
   * @param host the name of the host the job tracker is running on
   */
  public void setJobTrackerHost(String host) {
    setProperty(JOBTRACKER_HOST, host);
    //DistributedJobConfig.isEmpty(host) ? DEFAULT_HOST : host);
  }

  /**
   * Get the job tracker host name to use
   * 
   * @return the name of the host the job tracker is running on
   */
  public String getJobTrackerHost() {
    return getProperty(JOBTRACKER_HOST);
  }

  /**
   * Get the tool tip text for this property
   * 
   * @return the tool tip text for this property
   */
  public String jobTrackerPortTipText() {
    return "The port that the job tracker/resource manager is listening on";
  }

  /**
   * Set the port that the job tracker is running on
   * 
   * @param port the port that the job tracker is running on
   */
  public void setJobTrackerPort(String port) {
    setProperty(JOBTRACKER_PORT, port);
      //DistributedJobConfig.isEmpty(port) ? (isHadoop2() ? DEFAULT_PORT_YARN
//        : DEFAULT_PORT) : port);
  }

  /**
   * Get the port that the job tracker is running on
   * 
   * @return the port that the job tracker is running on
   */
  public String getJobTrackerPort() {
    return getProperty(JOBTRACKER_PORT);
  }
}
