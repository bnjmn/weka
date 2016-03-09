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
 *    SubstringLabeler.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Environment;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionMetadata;
import weka.core.WekaException;
import weka.filters.unsupervised.attribute.Add;
import weka.gui.ProgrammaticProperty;
import weka.gui.beans.SubstringLabelerRules;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.StepManagerImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Step that appends a label to incoming instances according to substring
 * matches in string attributes. Multiple match "rules" can be
 * specified - these get applied in the order that they are defined. Each rule
 * can be applied to one or more user-specified input String attributes.
 * Attributes can be specified using either a range list (e.g 1,2-10,last) or by
 * a comma separated list of attribute names (where "/first" and "/last" are
 * special strings indicating the first and last attribute respectively).
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "SubstringLabeler", category = "Tools",
  toolTipText = "Label instances according to substring matches in String "
    + "attributes "
    + "The user can specify the attributes to match "
    + "against and associated label to create by defining 'match' rules. A new attribute is appended "
    + "to the data to contain the label. Rules are applied in order when processing instances, and the "
    + "label associated with the first matching rule is applied. Non-matching instances can either receive "
    + "a missing value for the label attribute or be 'consumed' (i.e. they are not output).",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "DefaultFilter.gif")
public class SubstringLabeler extends BaseStep {

  private static final long serialVersionUID = 1409175779108600014L;

  /** Internally encoded list of match rules */
  protected String m_matchDetails = "";

  /** Encapsulates our match rules */
  protected transient SubstringLabelerRules m_matches;

  /**
   * Whether to make the binary match/non-match attribute a nominal (rather than
   * numeric) binary attribute.
   */
  protected boolean m_nominalBinary;

  /**
   * For multi-valued labeled rules, whether or not to consume non-matching
   * instances or output them with missing value for the match attribute.
   */
  protected boolean m_consumeNonMatchingInstances;

  /** Add filter for adding the new attribute */
  protected Add m_addFilter;

  /** Name of the new attribute */
  protected String m_attName = "Match";

  /** Step has been reset - i.e. start of processing? */
  protected boolean m_isReset;

  /** Reusable data object for output */
  protected Data m_streamingData;

  /** Streaming instances? */
  protected boolean m_streaming;

  /**
   * Set internally encoded list of match rules
   *
   * @param details the list of match rules
   */
  @ProgrammaticProperty
  public void setMatchDetails(String details) {
    m_matchDetails = details;
  }

  /**
   * Get the internally encoded list of match rules
   *
   * @return the match rules
   */
  public String getMatchDetails() {
    return m_matchDetails;
  }

  /**
   * Set whether the new attribute created should be a nominal binary attribute
   * rather than a numeric binary attribute.
   *
   * @param nom true if the attribute should be a nominal binary one
   */
  @OptionMetadata(displayName = "Make a nominal binary attribute",
    description = "Whether to encode the new attribute as nominal "
      + "when it is binary (as opposed to numeric)", displayOrder = 1)
  public void setNominalBinary(boolean nom) {
    m_nominalBinary = nom;
  }

  /**
   * Get whether the new attribute created should be a nominal binary attribute
   * rather than a numeric binary attribute.
   *
   * @return true if the attribute should be a nominal binary one
   */
  public boolean getNominalBinary() {
    return m_nominalBinary;
  }

  /**
   * Set whether instances that do not match any of the rules should be
   * "consumed" rather than output with a missing value set for the new
   * attribute.
   *
   * @param consume true if non matching instances should be consumed by the
   *          component.
   */
  @OptionMetadata(displayName = "Consume non matching instances",
    description = "Instances that do not match any rules will be consumed, "
      + "rather than being output with a missing value for the new attribute",
    displayOrder = 2)
  public void setConsumeNonMatching(boolean consume) {
    m_consumeNonMatchingInstances = consume;
  }

  /**
   * Get whether instances that do not match any of the rules should be
   * "consumed" rather than output with a missing value set for the new
   * attribute.
   *
   * @return true if non matching instances should be consumed by the component.
   */
  public boolean getConsumeNonMatching() {
    return m_consumeNonMatchingInstances;
  }

  /**
   * Set the name of the new attribute that is created to indicate the match
   *
   * @param name the name of the new attribute
   */
  @OptionMetadata(displayName = "Name of the new attribute",
    description = "Name to give the new attribute", displayOrder = 0)
  public void setMatchAttributeName(String name) {
    m_attName = name;
  }

  /**
   * Get the name of the new attribute that is created to indicate the match
   *
   * @return the name of the new attribute
   */
  public String getMatchAttributeName() {
    return m_attName;
  }

  /**
   * Initialize the step
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void stepInit() throws WekaException {
    m_isReset = true;
    m_streamingData = new Data(StepManager.CON_INSTANCE);
    m_streaming = false;
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
    return getStepManager().numIncomingConnections() == 0 ? Arrays.asList(
      StepManager.CON_INSTANCE, StepManager.CON_DATASET,
      StepManager.CON_TRAININGSET, StepManager.CON_TESTSET) : null;
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
    List<String> result = new ArrayList<String>();

    for (Map.Entry<String, List<StepManager>> e : getStepManager()
      .getIncomingConnections().entrySet()) {
      if (e.getValue().size() > 0) {
        result.add(e.getKey());
      }
    }

    return result;
  }

  /**
   * Process an incoming data payload (if the step accepts incoming connections)
   *
   * @param data the data to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public void processIncoming(Data data) throws WekaException {
    Instances structure;
    Instance inst;
    if (m_isReset) {
      if (getStepManager().numIncomingConnectionsOfType(
        StepManager.CON_INSTANCE) > 0) {
        inst = data.getPrimaryPayload();
        structure = inst.dataset();
        m_streaming = true;
      } else {
        structure = data.getPrimaryPayload();
        structure = new Instances(structure, 0);
      }
      try {
        m_matches =
          new SubstringLabelerRules(m_matchDetails, m_attName,
            getConsumeNonMatching(), getNominalBinary(), structure,
            ((StepManagerImpl) getStepManager()).stepStatusMessagePrefix(),
            getStepManager().getLog(), getStepManager()
              .getExecutionEnvironment().getEnvironmentVariables());
      } catch (Exception ex) {
        throw new WekaException(ex);
      }

      m_isReset = false;
    }

    if (m_streaming) {
      if (getStepManager().isStreamFinished(data)) {
        m_streamingData.clearPayload();
        getStepManager().throughputFinished(m_streamingData);
        return;
      } else {
        processStreaming(data);
      }
    } else {
      processBatch(data);
    }

    if (isStopRequested()) {
      getStepManager().interrupted();
    } else if (!m_streaming) {
      getStepManager().finished();
    }
  }

  /**
   * Processes a streaming data object
   *
   * @param data the data to process
   * @throws WekaException if a problem occurs
   */
  protected void processStreaming(Data data) throws WekaException {
    getStepManager().throughputUpdateStart();
    Instance toProcess = data.getPrimaryPayload();
    try {
      Instance result = m_matches.makeOutputInstance(toProcess, false);
      if (result != null) {
        m_streamingData.setPayloadElement(StepManager.CON_INSTANCE, result);
        getStepManager().outputData(m_streamingData);
        getStepManager().throughputUpdateEnd();
      }
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Process a batch data object
   *
   * @param data the data to process
   * @throws WekaException if a problem occurs
   */
  protected void processBatch(Data data) throws WekaException {
    if (isStopRequested()) {
      return;
    }

    Instances batch = data.getPrimaryPayload();
    for (int i = 0; i < batch.numInstances(); i++) {
      Instance current = batch.instance(i);
      Instance result = null;
      try {
        result = m_matches.makeOutputInstance(current, true);
      } catch (Exception ex) {
        ex.printStackTrace();
      }

      if (isStopRequested()) {
        return;
      }

      if (result != null) {
        m_matches.getOutputStructure().add(result);
      }
    }

    Data outputD =
      new Data(data.getConnectionName(), m_matches.getOutputStructure());
    outputD.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM,
      data.getPayloadElement(StepManager.CON_AUX_DATA_SET_NUM));
    outputD.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM,
      data.getPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM));
    getStepManager().outputData(outputD);
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
    // we output the same structure as we receive + one additional attribute
    if (getStepManager().numIncomingConnections() > 0) {
      for (Map.Entry<String, List<StepManager>> e : getStepManager()
        .getIncomingConnections().entrySet()) {
        if (e.getValue().size() > 0) {
          StepManager incoming = e.getValue().get(0);
          String incomingConnType = e.getKey();
          Instances incomingStruc =
            getStepManager().getIncomingStructureFromStep(incoming,
              incomingConnType);
          if (incomingStruc == null) {
            return null;
          }

          try {
            SubstringLabelerRules rules =
              new SubstringLabelerRules(m_matchDetails, m_attName,
                getConsumeNonMatching(), getNominalBinary(), incomingStruc,
                ((StepManagerImpl) getStepManager()).stepStatusMessagePrefix(),
                null, Environment.getSystemWide());

            return rules.getOutputStructure();
          } catch (Exception ex) {
            throw new WekaException(ex);
          }
        }
      }
    }

    return null;
  }

  /**
   * Return the fully qualified name of a custom editor component (JComponent)
   * to use for editing the properties of the step. This method can return null,
   * in which case the system will dynamically generate an editor using the
   * GenericObjectEditor
   *
   * @return the fully qualified name of a step editor component
   */
  @Override
  public String getCustomEditorForStep() {
    return "weka.gui.knowledgeflow.steps.SubstringLabelerStepEditorDialog";
  }
}
