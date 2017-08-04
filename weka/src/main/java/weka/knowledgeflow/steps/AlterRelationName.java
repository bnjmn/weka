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
 *    AlterRelationName.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionMetadata;
import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Step that alters the relation name for data received via instance, dataSet,
 * trainingSet and testSet connections
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "AlterRelationName", category = "Flow",
  toolTipText = "Alter the relation name in data sets",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "DiamondPlain.gif")
public class AlterRelationName extends BaseStep {

  private static final long serialVersionUID = 5894383194664583303L;

  /**
   * The set of source step identifiers that have had their data modified so far
   */
  protected Set<String> m_hasAltered;

  /** Text to modify the relation name with */
  protected String m_relationNameModText = "";

  /** The type of modification to make */
  protected ModType m_modType = ModType.REPLACE;

  /** For regex replacement */
  protected Pattern m_regexPattern;

  /** Regex string to match */
  protected String m_regexMatch = "";

  /** Whether to replace all rexex matches, or just the first */
  protected boolean m_replaceAll;

  /**
   * Initialize the step
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void stepInit() throws WekaException {
    m_hasAltered = new HashSet<>();
    if (m_modType == ModType.REGEX && m_relationNameModText != null
      && m_relationNameModText.length() > 0 && m_regexMatch != null
      && m_regexMatch.length() > 0) {
      m_regexPattern = Pattern.compile(m_regexMatch);
    }
  }

  /**
   * Set the modification text to apply
   *
   * @param text the text to apply
   */
  @OptionMetadata(displayName = "Text to use",
    description = "The text to modify the relation name with", displayOrder = 0)
  public
    void setModificationText(String text) {
    m_relationNameModText = text;
  }

  /**
   * Get the modification text to apply
   *
   * @return the modification text
   */
  public String getModificationText() {
    return m_relationNameModText;
  }

  /**
   * Set the modification type to apply
   *
   * @param mod the modification type to apply
   */
  @OptionMetadata(displayName = "Relation name modification type",
    description = "The type of modification to apply", displayOrder = 1)
  public void setModType(ModType mod) {
    m_modType = mod;
  }

  /**
   * Get the modification type to apply
   *
   * @return the modification type to apply
   */
  public ModType getModType() {
    return m_modType;
  }

  /**
   * Set the match string for regex modifications
   *
   * @param match the regular expression to apply for matching
   */
  @OptionMetadata(
    displayName = "Regular expression",
    description = "Regular expression to match when performing a REGEX modification",
    displayOrder = 2)
  public
    void setRegexMatch(String match) {
    m_regexMatch = match;
  }

  /**
   * Get the match string for regex modifications
   *
   * @return the regular expression to apply for matching
   */
  public String getRegexMatch() {
    return m_regexMatch;
  }

  /**
   * Set whether to replace all regular expression matches, or just the first.
   *
   * @param replaceAll true to replace all regex matches
   */
  @OptionMetadata(displayName = "Replace all regex matches",
    description = "Replace all matching occurrences if set to true, or just "
      + "the first match if set to false", displayOrder = 3)
  public void setReplaceAll(boolean replaceAll) {
    m_replaceAll = replaceAll;
  }

  /**
   * Get whether to replace all regular expression matches, or just the first.
   *
   * @return true to replace all regex matches
   */
  public boolean getReplaceAll() {
    return m_replaceAll;
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
    if (m_relationNameModText.length() > 0) {
      String toCheckKey = data.getSourceStep().getName();
      String connName = data.getConnectionName();
      if (!data.getConnectionName().equals(StepManager.CON_INSTANCE)) {
        connName +=
          "_" + data.getPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, 1)
            + "_"
            + data.getPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM, 1);
      }
      toCheckKey += connName;
      if (!m_hasAltered.contains(toCheckKey)) {
        getStepManager().logBasic(
          "Altering relation name for data from step " + "'"
            + data.getSourceStep().getName() + "' (" + connName + ")");

        // Do the relation name mod
        Instances insts = null;
        if (data.getConnectionName().equals(StepManager.CON_INSTANCE)) {
          insts = ((Instance) data.getPrimaryPayload()).dataset();
        } else {
          insts = data.getPrimaryPayload();
        }
        applyRelationNameChange(insts);
        m_hasAltered.add(data.getSourceStep().getName());
      }
    }

    // pass data through
    getStepManager().outputData(data);
    getStepManager().finished();
  }

  /**
   * Apply the change to the relation name in the given Instances object
   *
   * @param insts the Instances object to operate on
   */
  protected void applyRelationNameChange(Instances insts) {
    switch (m_modType) {
    case REPLACE:
      insts.setRelationName(m_relationNameModText);
      break;
    case APPEND:
      insts.setRelationName(insts.relationName() + m_relationNameModText);
      break;
    case PREPEND:
      insts.setRelationName(m_relationNameModText + insts.relationName());
      break;
    case REGEX:
      String rel = insts.relationName();
      if (m_replaceAll) {
        rel = m_regexPattern.matcher(rel).replaceAll(m_relationNameModText);
      } else {
        rel = m_regexPattern.matcher(rel).replaceFirst(m_relationNameModText);
      }
      insts.setRelationName(rel);
      break;
    }
  }

  /**
   * Get the list of acceptable incoming connection types
   *
   * @return the list of acceptable incoming connection types
   */
  @Override
  public List<String> getIncomingConnectionTypes() {
    return Arrays.asList(StepManager.CON_INSTANCE, StepManager.CON_DATASET,
      StepManager.CON_TRAININGSET, StepManager.CON_TESTSET);
  }

  /**
   * Get the list of outgoing connection types that can be made given the
   * current state of incoming connections
   *
   * @return a list of outgoing connection types that can be made
   */
  @Override
  public List<String> getOutgoingConnectionTypes() {
    Map<String, List<StepManager>> incomingConnected =
      getStepManager().getIncomingConnections();
    return new ArrayList<String>(incomingConnected.keySet());
  }

  /**
   * Enum of modification types
   */
  protected static enum ModType {
    REPLACE, PREPEND, APPEND, REGEX;
  }
}
