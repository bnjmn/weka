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
 *    SubstringReplacer.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.WekaException;
import weka.gui.ProgrammaticProperty;
import weka.gui.beans.SubstringReplacerRules;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.StepManagerImpl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A step that can replace sub-strings in the values of string attributes. Only
 * operates in streaming mode. Multiple match and replace "rules" can be
 * specified - these get applied in the order that they are defined. Each rule
 * can be applied to one or more user-specified input String attributes.
 * Attributes can be specified using either a range list (e.g 1,2-10,last) or by
 * a comma separated list of attribute names (where "/first" and "/last" are
 * special strings indicating the first and last attribute respectively).
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "SubstringReplacer", category = "Tools",
  toolTipText = "Replace substrings in String attribute values using "
    + "either literal match-and-replace or regular expression "
    + "matching. The attributes to apply the match and replace "
    + "rules to can be selected via a range string (e.g. "
    + "1-5,6-last) or by a comma-separated list of attribute "
    + "names (/first and /last can be used to indicate the first "
    + "and last attribute respectively)", iconPath = KFGUIConsts.BASE_ICON_PATH
    + "DefaultFilter.gif")
public class SubstringReplacer extends BaseStep {

  private static final long serialVersionUID = -8786642000811852824L;

  /** Internally encoded list of match-replace rules */
  protected String m_matchReplaceDetails = "";

  /** Handles the rules for replacement */
  protected transient SubstringReplacerRules m_mr;

  /** Reusable data object for output */
  protected Data m_streamingData;

  /** Step has been reset - i.e. start of processing */
  protected boolean m_isReset;

  /**
   * Set internally encoded list of match-replace rules
   *
   * @param details the list of match-replace rules
   */
  @ProgrammaticProperty
  public void setMatchReplaceDetails(String details) {
    m_matchReplaceDetails = details;
  }

  /**
   * Get the internally encoded list of match-replace rules
   *
   * @return the match-replace rules
   */
  public String getMatchReplaceDetails() {
    return m_matchReplaceDetails;
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
      return Arrays.asList(StepManager.CON_INSTANCE);
    }
    return null;
  }

  /**
   * Process an incoming data payload (if the step accepts incoming connections)
   *
   * @param data the data to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public void processIncoming(Data data) throws WekaException {
    Instance inst = data.getPrimaryPayload();
    if (m_isReset) {
      m_isReset = false;
      Instances structure = inst.dataset();
      m_mr =
        new SubstringReplacerRules(m_matchReplaceDetails, structure,
          ((StepManagerImpl) getStepManager()).stepStatusMessagePrefix(),
          getStepManager().getLog(), getStepManager().getExecutionEnvironment()
            .getEnvironmentVariables());
    }

    if (getStepManager().isStreamFinished(data)) {
      m_streamingData.clearPayload();
      getStepManager().throughputFinished(m_streamingData);
    } else {
      if (!isStopRequested()) {
        getStepManager().throughputUpdateStart();
        Instance outInst = m_mr.makeOutputInstance(inst);
        getStepManager().throughputUpdateEnd();
        m_streamingData.setPayloadElement(StepManager.CON_INSTANCE, outInst);
        getStepManager().outputData(m_streamingData);
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
    // we output the same structure as we receive
    if (getStepManager().numIncomingConnections() > 0) {
      for (Map.Entry<String, List<StepManager>> e : getStepManager()
        .getIncomingConnections().entrySet()) {
        if (e.getValue().size() > 0) {
          StepManager incoming = e.getValue().get(0);
          String incomingConnType = e.getKey();
          return getStepManager().getIncomingStructureFromStep(incoming,
            incomingConnType);
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
    return "weka.gui.knowledgeflow.steps.SubstringReplacerStepEditorDialog";
  }
}
