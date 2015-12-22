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
 *    Associator.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.associations.AssociationRules;
import weka.associations.AssociationRulesProducer;
import weka.core.Attribute;
import weka.core.Drawable;
import weka.core.Instances;
import weka.core.WekaException;
import weka.gui.ProgrammaticProperty;
import weka.gui.knowledgeflow.StepVisual;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Step that wraps a Weka associator. Handles dataSet, trainingSet and testSet
 * incoming connections. All connections are treated the same - i.e. are used as
 * training data.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "Associator", category = "Associations",
  toolTipText = "Weka associator wrapper", iconPath = "")
public class Associator extends WekaAlgorithmWrapper {

  private static final long serialVersionUID = -589410455393151511L;

  /** Template for the associator in use */
  protected weka.associations.Associator m_associatorTemplate;

  /**
   * Get the class of the algorithm being wrapped
   * 
   * @return the class of the wrapped algorithm
   */
  @Override
  public Class getWrappedAlgorithmClass() {
    return weka.associations.Associator.class;
  }

  /**
   * Set the wrapped algorithm
   * 
   * @param algo the wrapped algorithm
   */
  @Override
  public void setWrappedAlgorithm(Object algo) {
    super.setWrappedAlgorithm(algo);
    m_defaultIconPath = StepVisual.BASE_ICON_PATH + "DefaultAssociator.gif";
  }

  /**
   * Set the associator to use. Is a convenience method - just calls
   * setWrappedAlgorithm()
   * 
   * @param associator the associator to use
   */
  @ProgrammaticProperty
  public void setAssociator(weka.associations.Associator associator) {
    setWrappedAlgorithm(associator);
  }

  /**
   * Get the associator to use. Is a convenience method - just calls
   * getWrappedAlgorithm()
   *
   * @return the associator in use
   */
  public weka.associations.Associator getAssociator() {
    return (weka.associations.Associator) getWrappedAlgorithm();
  }

  /**
   * Initializes the step
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void stepInit() throws WekaException {
    if (!(getWrappedAlgorithm() instanceof weka.associations.Associator)) {
      throw new WekaException("Wrapped algorithm is not an instance of "
        + "a weka.associations.Associator!");
    }

    try {
      m_associatorTemplate =
        weka.associations.AbstractAssociator.makeCopy(getAssociator());
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Processes incoming data
   *
   * @param data the data to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public void processIncoming(Data data) throws WekaException {

    Instances insts = data.getPrimaryPayload();
    Integer setNum = data.getPayloadElement(StepManager.CON_AUX_DATA_SET_NUM);
    Integer maxSetNum =
      data.getPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM);

    try {
      if (!isStopRequested()) {
        getStepManager().processing();
        weka.associations.Associator associator =
          weka.associations.AbstractAssociator.makeCopy(m_associatorTemplate);

        associator.buildAssociations(insts);
        outputAssociatorData(associator, setNum, maxSetNum);
        outputTextData(associator, insts, setNum);
        outputGraphData(associator, insts, setNum);

        if (!isStopRequested()) {
          getStepManager().finished();
        } else {
          getStepManager().interrupted();
        }
      }
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Outputs the trained associator to downstream steps that are interested
   *
   * @param associator the associator to output
   * @param setNum the set number of the data used to train the associator
   * @param maxSetNum the maximum set number
   * @throws WekaException if a problem occurs
   */
  protected void outputAssociatorData(weka.associations.Associator associator,
    Integer setNum, Integer maxSetNum) throws WekaException {
    if (getStepManager()
      .numOutgoingConnectionsOfType(StepManager.CON_BATCH_ASSOCIATOR) == 0) {
      return;
    }

    Data out = new Data(StepManager.CON_BATCH_ASSOCIATOR, associator);
    if (setNum != null && maxSetNum != null) {
      out.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, setNum);
      out.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM, maxSetNum);
    }

    if (associator instanceof AssociationRulesProducer) {
      AssociationRules rules =
        ((AssociationRulesProducer) associator).getAssociationRules();
      out.setPayloadElement(StepManager.CON_AUX_DATA_BATCH_ASSOCIATION_RULES,
        rules);
    }

    getStepManager().outputData(out);
  }

  /**
   * Outputs textual representation of associator to downstream steps
   * 
   * @param associator the associator to output the textual form for
   * @param train the training data used to train the associator
   * @param setNum the set number of the data
   * @throws WekaException if a problem occurs
   */
  protected void outputTextData(weka.associations.Associator associator,
    Instances train, Integer setNum) throws WekaException {
    if (getStepManager()
      .numOutgoingConnectionsOfType(StepManager.CON_TEXT) == 0) {
      return;
    }

    String modelString = associator.toString();
    String titleString = associator.getClass().getName();

    titleString = titleString.substring(titleString.lastIndexOf('.') + 1,
      titleString.length());
    modelString = "=== Associator model ===\n\n" + "Scheme:   " + titleString
      + "\n" + "Relation: " + train.relationName() + "\n\n" + modelString;
    titleString = "Model: " + titleString;

    Data textData = new Data(StepManager.CON_TEXT, modelString);
    textData.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE,
      titleString);

    if (setNum != null) {
      textData.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, setNum);
    }

    getStepManager().outputData(textData);
  }

  protected void outputGraphData(weka.associations.Associator associator,
    Instances insts, Integer setNum) throws WekaException {

    if (!(associator instanceof Drawable) || getStepManager()
      .numOutgoingConnectionsOfType(StepManager.CON_GRAPH) == 0) {
      return;
    }

    try {
      String graphString = ((Drawable) associator).graph();
      int graphType = ((Drawable) associator).graphType();
      String grphTitle = associator.getClass().getCanonicalName();
      grphTitle =
        grphTitle.substring(grphTitle.lastIndexOf('.') + 1, grphTitle.length());
      String set = setNum != null ? "Set " + setNum : "";
      grphTitle = set + " (" + insts.relationName() + ") " + grphTitle;
      Data graphData = new Data(StepManager.CON_GRAPH);
      graphData.setPayloadElement(StepManager.CON_GRAPH, graphString);
      graphData.setPayloadElement(StepManager.CON_AUX_DATA_GRAPH_TITLE,
        grphTitle);
      graphData.setPayloadElement(StepManager.CON_AUX_DATA_GRAPH_TYPE,
        graphType);
      getStepManager().outputData(graphData);
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Get a list of incoming connection types that this step can accept at this
   * time
   * 
   * @return a list of incoming connections that this step can accept
   */
  @Override
  public List<String> getIncomingConnectionTypes() {
    List<String> result = new ArrayList<String>();

    if (getStepManager().numIncomingConnections() == 0) {
      result.addAll(Arrays.asList(StepManager.CON_DATASET,
        StepManager.CON_TRAININGSET, StepManager.CON_TESTSET));
    }
    return result;
  }

  /**
   * Get a list of outgoing connections that this step can produce at this time
   * 
   * @return a list of outgoing connection types
   */
  @Override
  public List<String> getOutgoingConnectionTypes() {
    List<String> result = new ArrayList<String>();

    if (getStepManager().numIncomingConnections() > 0) {
      result.add(StepManager.CON_BATCH_ASSOCIATOR);
      result.add(StepManager.CON_TEXT);
    }

    result.add(StepManager.CON_INFO);

    return result;
  }

  /**
   * If possible, get the output structure for the named connection type as a
   * header-only set of instances. Can return null if the specified connection
   * type is not representable as Instances or cannot be determined at present.
   * 
   * @param connectionName the connection type to generate output structure for
   * @return the output structure this step generates, or null if it can't be
   * determined at this point in time
   * @throws WekaException if a problem occurs
   */
  @Override
  public Instances outputStructureForConnectionType(String connectionName)
    throws WekaException {

    if (connectionName.equals(StepManager.CON_TEXT)) {
      ArrayList<Attribute> attInfo = new ArrayList<Attribute>();
      attInfo.add(new Attribute("Title", (ArrayList<String>) null));
      attInfo.add(new Attribute("Text", (ArrayList<String>) null));
      return new Instances("TextEvent", attInfo, 0);
    } else if (connectionName.equals(StepManager.CON_BATCH_ASSOCIATOR)) {
      if (m_associatorTemplate instanceof AssociationRulesProducer) {
        // we make the assumption here that consumers of
        // batchAssociationRules events will utilize a structure
        // consisting of the RHS of the rule (String), LHS of the
        // rule (String) and one numeric attribute for each metric
        // associated with the rules.

        String[] metricNames = ((AssociationRulesProducer) m_associatorTemplate)
          .getRuleMetricNames();
        ArrayList<Attribute> attInfo = new ArrayList<Attribute>();
        attInfo.add(new Attribute("LHS", (ArrayList<String>) null));
        attInfo.add(new Attribute("RHS", (ArrayList<String>) null));
        attInfo.add(new Attribute("Support"));
        for (String metricName : metricNames) {
          attInfo.add(new Attribute(metricName));
        }
        return new Instances(StepManager.CON_AUX_DATA_BATCH_ASSOCIATION_RULES,
          attInfo, 0);
      }
    }

    return null;
  }
}
