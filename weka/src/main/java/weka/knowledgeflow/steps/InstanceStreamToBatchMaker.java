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
 *    InstanceStreamToBatchMaker.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Step that converts an incoming instance stream to a batch dataset
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "InstanceStreamToBatchMaker", category = "Flow",
  toolTipText = "Converts an incoming instance stream into a batch dataset",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "InstanceStreamToBatchMaker.gif")
public class InstanceStreamToBatchMaker extends BaseStep {

  /** For serialization */
  private static final long serialVersionUID = 5461324282251111320L;

  /** True if we've been reset */
  protected boolean m_isReset;

  /** The structure of the incoming instances */
  protected Instances m_structure;

  /** True if the incoming data contains string attributes */
  protected boolean m_hasStringAtts;
  
  /**
   * Initialize the step.
   *
   * @throws WekaException if a problem occurs during initialization
   */
  @Override
  public void stepInit() throws WekaException {
    m_isReset = true;
  }

  /**
   * Get a list of incoming connection types that this step can accept. Ideally
   * (and if appropriate), this should take into account the state of the step
   * and any existing incoming connections. E.g. a step might be able to accept
   * one (and only one) incoming batch data connection.
   *
   * @return a list of incoming connections that this step can accept given its
   *         current state
   */
  @Override
  public List<String> getIncomingConnectionTypes() {
    if (getStepManager().numIncomingConnections() == 0) {
      return Arrays.asList(StepManager.CON_INSTANCE);
    }
    return null;
  }

  /**
   * Get a list of outgoing connection types that this step can produce. Ideally
   * (and if appropriate), this should take into account the state of the step
   * and the incoming connections. E.g. depending on what incoming connection is
   * present, a step might be able to produce a trainingSet output, a testSet
   * output or neither, but not both.
   *
   * @return a list of outgoing connections that this step can produce
   */
  @Override
  public List<String> getOutgoingConnectionTypes() {
    if (getStepManager().numIncomingConnections() > 0) {
      return Arrays.asList(StepManager.CON_DATASET,
        StepManager.CON_TRAININGSET, StepManager.CON_TESTSET);
    }

    return null;
  }

  /**
   * Process incoming data
   *
   * @param data the payload to process
   * @throws WekaException
   */
  @Override
  public void processIncoming(Data data) throws WekaException {
    if (m_isReset) {
      m_isReset = false;
      if (data.getPrimaryPayload() == null) {
        throw new WekaException("We didn't receive any instances!");
      }
      getStepManager().logDetailed("Collecting instances...");
      Instance temp = data.getPrimaryPayload();
      m_structure = new Instances(temp.dataset(), 0).stringFreeStructure();
      m_hasStringAtts = temp.dataset().checkForStringAttributes();
    }

    if (isStopRequested()) {
      getStepManager().interrupted();
      return;
    }

    if (!getStepManager().isStreamFinished(data)) {
      getStepManager().throughputUpdateStart();
      Instance inst = data.getPrimaryPayload();
      if (m_hasStringAtts) {
        for (int i = 0; i < m_structure.numAttributes(); i++) {
          if (m_structure.attribute(i).isString() && !inst.isMissing(i)) {
            int index =
              m_structure.attribute(i).addStringValue(inst.stringValue(i));
            inst.setValue(i, index);
          }
        }
      }
      m_structure.add(inst);
      getStepManager().throughputUpdateEnd();
    } else {
      // output batch
      m_structure.compactify();
      getStepManager().logBasic(
        "Emitting a batch of " + m_structure.numInstances() + " instances.");
      List<String> outCons =
        new ArrayList<String>(getStepManager().getOutgoingConnections()
          .keySet());
      Data out = new Data(outCons.get(0), m_structure);
      out.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, 1);
      out.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM, 1);
      if (!isStopRequested()) {
        getStepManager().outputData(out);
        getStepManager().finished();
      } else {
        getStepManager().interrupted();
      }
    }
  }

  /**
   * If possible, get the output structure for the named connection type as a
   * header-only set of instances. Can return null if the specified connection
   * type is not representable as Instances or cannot be determined at present.
   *
   * @param connectionName the name of the connection type to get the output
   *          structure for
   * @return the output structure as a header-only Instances object
   * @throws WekaException if a problem occurs
   */
  @Override
  public Instances outputStructureForConnectionType(String connectionName)
    throws WekaException {

    if (getStepManager().numIncomingConnections() > 0) {
      // we don't alter the structure of the incoming data
      return getStepManager().getIncomingStructureForConnectionType(
        StepManager.CON_INSTANCE);
    }

    return null;
  }
}
