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
 *    RenameRelation.java
 *    Copyright (C) 2006-2017 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters;

import weka.core.*;

import java.util.regex.Pattern;

/**
 * A simple filter that allows the relation name of a set of instances to be
 * altered in various ways.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class RenameRelation extends Filter
        implements StreamableFilter, WeightedAttributesHandler, WeightedInstancesHandler {
  private static final long serialVersionUID = 8082179220141937043L;

  /** Text to modify the relation name with */
  protected String m_relationNameModText = "change me";

  /** The type of modification to make */
  protected ModType m_modType = ModType.REPLACE;

  /** Pattern for regex replacement */
  protected Pattern m_regexPattern;

  /** Regex string to match */
  protected String m_regexMatch = "([\\s\\S]+)";

  /** Whether to replace all rexex matches, or just the first */
  protected boolean m_replaceAll;

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "A filter that allows the relation name of a set of instances to be "
      + "altered.";
  }

  /**
   * Returns the Capabilities of this filter.
   *
   * @return the capabilities of this object
   * @see Capabilities
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enableAllAttributes();
    result.enable(Capabilities.Capability.MISSING_VALUES);

    // class
    result.enableAllClasses();
    result.enable(Capabilities.Capability.MISSING_CLASS_VALUES);
    result.enable(Capabilities.Capability.NO_CLASS);

    return result;
  }

  /**
   * Set the modification text to apply
   *
   * @param text the text to apply
   */
  @OptionMetadata(displayName = "Text to use",
    description = "The text to modify the relation name with",
    commandLineParamName = "modify",
    commandLineParamSynopsis = "-modify <string>", displayOrder = 0)
  public void setModificationText(String text) {
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
  @OptionMetadata(
    displayName = "Relation name modification type",
    description = "The type of modification to apply (default = REPLACE)",
    commandLineParamName = "mod-type",
    commandLineParamSynopsis = "-mod-type <REPLACE | PREPEND | APPEND | REGEX>",
    displayOrder = 1)
  public
    void setModType(ModType mod) {
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
  @OptionMetadata(displayName = "Regular expression",
    description = "Regular expression to use for matching when performing a "
      + "REGEX modification (default = ([\\s\\S]+))",
    commandLineParamName = "find",
    commandLineParamSynopsis = "-find <pattern>", displayOrder = 2)
  public void setRegexMatch(String match) {
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
      + "the first match if set to false",
    commandLineParamName = "replace-all",
    commandLineParamSynopsis = "-replace-all", commandLineParamIsFlag = true,
    displayOrder = 3)
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
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input instance
   *          structure (any instances contained in the object are ignored -
   *          only the structure is required).
   * @return true if the outputFormat may be collected immediately
   * @throws Exception if something goes wrong
   */
  @Override
  public boolean setInputFormat(Instances instanceInfo) throws Exception {

    super.setInputFormat(instanceInfo);
    setOutputFormat(instanceInfo);

    // alter the relation name on output format
    if (m_modType == ModType.REGEX && m_relationNameModText != null
      && m_relationNameModText.length() > 0 && m_regexMatch != null
      && m_regexMatch.length() > 0) {
      m_regexPattern = Pattern.compile(m_regexMatch);
    }
    applyRelationNameChange(outputFormatPeek());

    return true;
  }

  @Override
  public boolean input(Instance instance) {
    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }

    if (instance.dataset() == null) {
      push((Instance) instance.copy());
    } else {
      push(instance); // push() will make a copy anyway.
    }
    return true;
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
      insts.setRelationName(getInputFormat().relationName() + m_relationNameModText);
      break;
    case PREPEND:
      insts.setRelationName(m_relationNameModText + getInputFormat().relationName());
      break;
    case REGEX:
      String rel = getInputFormat().relationName();
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
   * Returns the revision string.
   *
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: $");
  }

  /**
   * Enum of modification types
   */
  protected enum ModType {
    REPLACE, PREPEND, APPEND, REGEX;
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String[] argv) {
    runFilter(new RenameRelation(), argv);
  }
}
