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
 *    CanopyBuilder.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.clusterers;

import java.io.IOException;
import java.io.Serializable;

import weka.clusterers.Clusterer;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CanopyMapTask;
import weka.distributed.DistributedWekaException;
import distributed.core.DistributedJob;

/**
 * Helper class for building a distributed canopy clusterer
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class CanopyBuilder implements Serializable {

  /** For serializaton */
  private static final long serialVersionUID = -4671655252421074060L;

  /** The map task to use */
  protected CanopyMapTask m_canopyMapTask;

  /** The header of the data without summary attributes */
  protected Instances m_headerNoSummary;

  /** The header of the data with summary attributes */
  protected Instances m_headerWithSummary;

  /** Only needed if the objects in the dataset are strings */
  protected CSVToARFFHeaderMapTask m_rowHelper;

  /** The options for the CSV parser */
  protected String m_csvMapTaskOpts = "";

  /**
   * Construct a new Canopy builder
   * 
   * @param headerWithSummary the header of the data with summary attributes
   * @param headerNoSummary the header of the data without summary attributes
   * @param canopyMapTask the configured map task to use
   * @param csvMapTaskOptions options for csv parsing
   */
  public CanopyBuilder(final Instances headerWithSummary,
    Instances headerNoSummary, final CanopyMapTask canopyMapTask,
    String csvMapTaskOptions) {
    m_headerWithSummary = headerWithSummary;
    m_headerNoSummary = headerNoSummary;
    m_canopyMapTask = canopyMapTask;
    m_csvMapTaskOpts = csvMapTaskOptions;
  }

  /**
   * Process the current object (instance or string).
   * 
   * @param current either an Instance or a string (assumed CSV)
   * @throws DistributedWekaException if a problem occurs
   * @throws IOException if a problem occurs
   */
  public void process(Object current) throws DistributedWekaException,
    IOException {

    if (m_rowHelper == null) {
      m_rowHelper = new CSVToARFFHeaderMapTask();
      try {
        m_rowHelper.setOptions(Utils.splitOptions(m_csvMapTaskOpts));
        m_rowHelper.initParserOnly(CSVToARFFHeaderMapTask
          .instanceHeaderToAttributeNameList(m_headerNoSummary));

        m_canopyMapTask.init(m_headerWithSummary);
      } catch (Exception e) {
        throw new DistributedWekaException(e);
      }
    }

    Instance currentI =
      current instanceof String ? DistributedJob.parseInstance(
        current.toString(), m_rowHelper, m_headerNoSummary, true)
        : (Instance) current;

    m_canopyMapTask.update(currentI);
  }

  /**
   * Notify that there is no more input
   */
  public void finishedInput() {
    m_canopyMapTask.updateFinished();
  }

  /**
   * Return the finalized clusterer. This method should only be called after
   * finishedInput().
   * 
   * @return the finalized clusterer
   * @throws DistributedWekaException if a problem occurs
   */
  public Clusterer getFinalizedClusterer() throws DistributedWekaException {
    return m_canopyMapTask.getFinalizedClusterer();
  }
}
