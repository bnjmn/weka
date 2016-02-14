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
 *    TestSetMaker.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Instances;
import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A step that makes an incoming dataSet or trainingSet into a testSet.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 */
@KFStep(name = "TestSetMaker", category = "Evaluation",
  toolTipText = "Make an incoming dataSet or trainingSet into a testSet",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "TestSetMaker.gif")
public class TestSetMaker extends BaseStep {

  private static final long serialVersionUID = 6384920860783839811L;

  /**
   * Initialize the step
   */
  @Override
  public void stepInit() {
    // nothing to do
  }

  /**
   * Process an incoming data payload (if the step accepts incoming connections)
   *
   * @param data the data to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public void processIncoming(Data data) throws WekaException {
    getStepManager().processing();
    String incomingConnName = data.getConnectionName();
    Instances insts = (Instances) data.getPayloadElement(incomingConnName);
    if (insts == null) {
      throw new WekaException("Incoming instances should not be null!");
    }

    getStepManager().logBasic(
      "Creating a test set for relation " + insts.relationName());
    Data newData = new Data();
    newData.setPayloadElement(StepManager.CON_TESTSET, insts);
    newData.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, 1);
    newData.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM, 1);
    if (!isStopRequested()) {
      getStepManager().outputData(StepManager.CON_TESTSET, newData);
    }
    getStepManager().finished();
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
      return Arrays
        .asList(StepManager.CON_DATASET, StepManager.CON_TRAININGSET);
    }

    return new ArrayList<String>();
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
      return Arrays.asList(StepManager.CON_TESTSET);
    }
    return new ArrayList<String>();
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
    if (!connectionName.equals(StepManager.CON_TESTSET)
      || getStepManager().numIncomingConnections() == 0) {
      return null;
    }

    Instances strucForDatasetCon =
      getStepManager().getIncomingStructureForConnectionType(
        StepManager.CON_DATASET);
    if (strucForDatasetCon != null) {
      return strucForDatasetCon;
    }

    Instances strucForTrainingSetCon =
      getStepManager().getIncomingStructureForConnectionType(
        StepManager.CON_TRAININGSET);
    if (strucForTrainingSetCon != null) {
      return strucForTrainingSetCon;
    }

    return null;
  }
}
