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
 *    StatsCalculator.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Attribute;
import weka.core.AttributeStats;
import weka.core.Instances;
import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Simple example batch processing step. Computes stats for a user-specified
 * attribute
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "StatsCalculator", category = "Examples",
  toolTipText = "Compute summary stats for an attribute",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "DiamondPlain.gif")
public class StatsCalculator extends BaseStep {

  private static final long serialVersionUID = -1666405202068749388L;

  /** The name of the attribute in the incoming data to compute stats for */
  protected String m_attName = "petallength";

  /**
   * Initialize the step.
   *
   * @throws WekaException if no attribute name is specified
   */
  @Override
  public void stepInit() throws WekaException {
    if (m_attName == null || m_attName.length() == 0) {
      throw new WekaException("You must specify an attribute to compute "
        + "stats for!");
    }
  }

  /**
   * Get a list of acceptable incoming connection types
   *
   * @return a list of acceptable incoming connection types
   */
  @Override
  public List<String> getIncomingConnectionTypes() {
    return Arrays.asList(StepManager.CON_DATASET, StepManager.CON_TRAININGSET,
      StepManager.CON_TESTSET);
  }

  /**
   * Get a list of outgoing connection types
   *
   * @return a list of outgoing connection types
   */
  @Override
  public List<String> getOutgoingConnectionTypes() {
    List<String> outgoing = new ArrayList<String>();
    if (getStepManager().numIncomingConnections() > 0) {
      outgoing.add(StepManager.CON_TEXT);
    }
    if (getStepManager().numIncomingConnectionsOfType(StepManager.CON_DATASET) > 0) {
      outgoing.add(StepManager.CON_DATASET);
    }
    if (getStepManager().numIncomingConnectionsOfType(
      StepManager.CON_TRAININGSET) > 0) {
      outgoing.add(StepManager.CON_TRAININGSET);
    }
    if (getStepManager().numIncomingConnectionsOfType(StepManager.CON_TESTSET) > 0) {
      outgoing.add(StepManager.CON_TESTSET);
    }
    return outgoing;
  }

  /**
   * Set the name of the attribute to compute stats for
   *
   * @param attName the name of the attribute to compute stats for
   */
  public void setAttName(String attName) {
    m_attName = attName;
  }

  /**
   * Get the name of the attribute to compute stats for
   *
   * @return the name of the attribute to compute stats for
   */
  public String getAttName() {
    return m_attName;
  }

  /**
   * Process incoming data
   *
   * @param data the payload to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public void processIncoming(Data data) throws WekaException {
    getStepManager().processing();
    Instances insts = data.getPrimaryPayload();
    Attribute att = insts.attribute(getAttName());
    if (att == null) {
      throw new WekaException("Incoming data does not " + "contain attribute '"
        + getAttName() + "'");
    }
    AttributeStats stats = insts.attributeStats(att.index());
    Data textOut = new Data(StepManager.CON_TEXT, stats.toString());
    textOut.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE,
      "Stats for: " + getAttName());
    getStepManager().outputData(textOut); // output the textual stats
    getStepManager().outputData(data); // pass the original dataset on
    getStepManager().finished();
  }
}
