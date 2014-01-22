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

/**
 * Base job config for Hadoop-related jobs to extend
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public abstract class AbstractHadoopJobConfig extends DistributedJobConfig {

  /** For serialization */
  private static final long serialVersionUID = -4170580935543278227L;

  /** Key for job tracker host name */
  public static final String JOBTRACKER_HOST = "jobtrackerHost";

  /** Key for job tracker port */
  public static final String JOBTRACKER_PORT = "jobtrackerPort";

  /**
   * Get the tool tip text for this property
   * 
   * @return the tool tip text for this property
   */
  public String jobTrackerHostTipText() {
    return "The host that the job tracker is running on";
  }

  /**
   * Set the job tracker host name to use
   * 
   * @param host the name of the host the job tracker is running on
   */
  public void setJobTrackerHost(String host) {
    setProperty(JOBTRACKER_HOST, host);
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
    return "The port that the job tracker is listening on";
  }

  /**
   * Set the port that the job tracker is running on
   * 
   * @param port the port that the job tracker is running on
   */
  public void setJobTrackerPort(String port) {
    setProperty(JOBTRACKER_PORT, port);
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
