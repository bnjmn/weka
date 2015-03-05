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
 *    CanopyAssigner.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.clusterers;

import java.io.IOException;
import java.io.Serializable;

import weka.clusterers.Canopy;
import weka.clusterers.InstanceWithCanopyAssignments;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.DistributedWekaException;
import weka.filters.Filter;
import distributed.core.DistributedJob;

/**
 * Assigns canopies to a given instance.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class CanopyAssigner implements Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -174426905053912537L;

  /** the structure of the data without any summary attributes */
  protected Instances m_header;

  /** Only needed if the objects in the dataset are strings */
  protected CSVToARFFHeaderMapTask m_rowHelper;

  /** CSV parsing options */
  protected String m_csvParsingOpts = "";

  /** The canopy clusterer to use for assigning canopies to instances */
  protected Canopy m_canopy;

  /**
   * Any preprocessing filters (missing values replacement etc.) used when the
   * clusterer was constructed
   */
  protected Filter m_preprocess;

  /**
   * Construct a new CanopyAssigner
   * 
   * @param headerNoSummary the structure of the data without summary attributes
   * @param csvParsingOpts the options for CSV parsing
   * @param canopy trained canopy clusterer to use for assigning canopies to
   *          instances
   * @param preprocess an optional preprocessing filter (missing values replacer
   *          + other filters that might have been used when the canopy
   *          clusterer was constructed).
   */
  public CanopyAssigner(final Instances headerNoSummary,
    final String csvParsingOpts, final Canopy canopy, final Filter preprocess) {
    m_header = new Instances(headerNoSummary, 0);
    m_csvParsingOpts = csvParsingOpts;
    m_canopy = canopy;
    m_preprocess = preprocess;
  }

  /**
   * Process an instance
   * 
   * @param current either an instance object or a string (CSV)
   * @return an InstanceWithCanopyAssignments holder object
   * @throws DistributedWekaException if a problem occurs
   * @throws IOException if a problem occurs
   */
  public InstanceWithCanopyAssignments process(Object current)
    throws DistributedWekaException, IOException {
    if (m_rowHelper == null) {
      m_rowHelper = new CSVToARFFHeaderMapTask();
      try {
        m_rowHelper.setOptions(Utils.splitOptions(m_csvParsingOpts));
        m_rowHelper.initParserOnly(CSVToARFFHeaderMapTask
          .instanceHeaderToAttributeNameList(m_header));
      } catch (Exception e) {
        throw new DistributedWekaException(e);
      }
    }

    Instance currentI =
      current instanceof String ? DistributedJob.parseInstance(
        current.toString(), m_rowHelper, m_header, true) : (Instance) current;

    // filter?
    Instance toProcess = currentI;
    if (m_preprocess != null) {
      try {
        m_preprocess.input(toProcess);
        toProcess = m_preprocess.output();
      } catch (Exception ex) {
        throw new DistributedWekaException(ex);
      }
    }

    long[] assignments = null;
    try {
      assignments = m_canopy.assignCanopies(toProcess);
    } catch (Exception e) {
      throw new DistributedWekaException(e);
    }
    InstanceWithCanopyAssignments holder =
      new InstanceWithCanopyAssignments(currentI, assignments);

    return holder;
  }
}
