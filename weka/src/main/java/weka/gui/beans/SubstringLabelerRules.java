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
 *    Matches.java
 *    Copyright (C) 2011-2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Range;
import weka.core.SerializedObject;
import weka.core.Utils;
import weka.gui.Logger;

/**
 * Manages a list of match rules for labeling strings. Also has methods for
 * determining the output structure with respect to a set of rules and for
 * constructing output instances that have been labeled according to the rules.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class SubstringLabelerRules implements EnvironmentHandler, Serializable {

  /** Separator for match rules in the internal representation */
  public static final String MATCH_RULE_SEPARATOR = "@@match-rule@@";

  private static final long serialVersionUID = 1392983905562573599L;

  /** The list of rules */
  protected List<SubstringLabelerMatchRule> m_matchRules;

  /** True if the rules have user supplied labels */
  protected boolean m_hasLabels;

  /**
   * True if non matching instances should be "consumed" - i.e no output
   * instance created
   */
  protected boolean m_consumeNonMatching;

  /** The input structure */
  protected Instances m_inputStructure;

  /** The output structure */
  protected Instances m_outputStructure;

  /** The name of the new "label" attribute */
  protected String m_attName = "newAtt";

  /** An optional prefix for status log messages */
  protected String m_statusMessagePrefix = "";

  /**
   * If not multiple labels then should new att be a nominal rather than numeric
   * binary one?
   */
  protected boolean m_nominalBinary;

  protected boolean m_voteLabels = true;

  /** Environment variables */
  protected transient Environment m_env = Environment.getSystemWide();

  /**
   * Constructor
   * 
   * @param matchDetails the internally encoded match details string
   * @param newAttName the name of the new attribute that will be the label
   * @param consumeNonMatching true if non-matching instances should be consumed
   * @param nominalBinary true if, in the case where no user labels have been
   *          supplied, the new attribute should be a nominal binary one rather
   *          than numeric
   * @param inputStructure the incoming instances structure
   * @param statusMessagePrefix an optional status message prefix string for
   *          logging
   * @param log the log to use (may be null)
   * @param env environment variables
   */
  public SubstringLabelerRules(String matchDetails, String newAttName,
    boolean consumeNonMatching, boolean nominalBinary,
    Instances inputStructure, String statusMessagePrefix, Logger log,
    Environment env) throws Exception {
    m_matchRules =
      matchRulesFromInternal(matchDetails, inputStructure, statusMessagePrefix,
        log, env);

    m_inputStructure = new Instances(inputStructure, 0);
    m_attName = newAttName;
    m_statusMessagePrefix = statusMessagePrefix;
    m_consumeNonMatching = consumeNonMatching;
    m_nominalBinary = nominalBinary;
    m_env = env;

    makeOutputStructure();
  }

  /**
   * Constructor. Sets consume non matching to false and nominal binary to
   * false. Initializes with system-wide environment variables. Initializes with
   * no status message prefix and no log.
   * 
   * @param matchDetails the internally encoded match details string.
   * @param newAttName the name of the new attribute that will be the label
   * @param inputStructure the incoming instances structure
   */
  public SubstringLabelerRules(String matchDetails, String newAttName,
    Instances inputStructure) throws Exception {
    this(matchDetails, newAttName, false, false, inputStructure, "", null,
      Environment.getSystemWide());
  }

  /**
   * Set whether to consume non matching instances. If false, then they will be
   * passed through unaltered.
   * 
   * @param n true then non-matching instances will be consumed (and only
   *          matching, and thus labelled, instances will be output)
   */
  public void setConsumeNonMatching(boolean n) {
    m_consumeNonMatching = n;
  }

  /**
   * Get whether to consume non matching instances. If false, then they will be
   * passed through unaltered.
   * 
   * @return true then non-matching instances will be consumed (and only
   *         matching, and thus labelled, instances will be output)
   */
  public boolean getConsumeNonMatching() {
    return m_consumeNonMatching;
  }

  /**
   * Set whether to create a nominal binary attribute in the case when the user
   * has not supplied an explicit label to use for each rule. If no labels are
   * provided, then the output attribute is a binary indicator one (i.e. a rule
   * matched or it didn't). This option allows that binary indicator to be coded
   * as nominal rather than numeric
   * 
   * @param n true if a binary indicator attribute should be nominal rather than
   *          numeric
   */
  public void setNominalBinary(boolean n) {
    m_nominalBinary = n;
  }

  /**
   * Get whether to create a nominal binary attribute in the case when the user
   * has not supplied an explicit label to use for each rule. If no labels are
   * provided, then the output attribute is a binary indicator one (i.e. a rule
   * matched or it didn't). This option allows that binary indicator to be coded
   * as nominal rather than numeric
   * 
   * @return true if a binary indicator attribute should be nominal rather than
   *         numeric
   */
  public boolean getNominalBinary() {
    return m_nominalBinary;
  }

  /**
   * Get the output structure
   * 
   * @return the structure of the output instances
   */
  public Instances getOutputStructure() {
    return m_outputStructure;
  }

  /**
   * Get the input structure
   * 
   * @return the structure of the input instances
   */
  public Instances getInputStructure() {
    return m_inputStructure;
  }

  /**
   * Set the name to use for the new attribute that is added
   * 
   * @param newName the name to use
   */
  public void setNewAttributeName(String newName) {
    m_attName = newName;
  }

  /**
   * Get the name to use for the new attribute that is added
   * 
   * @return the name to use
   */
  public String getNewAttributeName() {
    return m_attName;
  }

  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
  }

  /**
   * Get a list of match rules from an internally encoded match specification
   * 
   * @param matchDetails the internally encoded specification of the match rules
   * @param inputStructure the input instances structure
   * @param statusMessagePrefix an optional status message prefix for logging
   * @param log the log to use
   * @param env environment variables
   * @return a list of match rules
   */
  public static List<SubstringLabelerMatchRule> matchRulesFromInternal(
    String matchDetails, Instances inputStructure, String statusMessagePrefix,
    Logger log, Environment env) {

    List<SubstringLabelerMatchRule> matchRules =
      new ArrayList<SubstringLabelerMatchRule>();

    String[] matchParts = matchDetails.split(MATCH_RULE_SEPARATOR);
    for (String p : matchParts) {
      SubstringLabelerMatchRule m = new SubstringLabelerMatchRule(p.trim());
      m.m_statusMessagePrefix =
        statusMessagePrefix == null ? "" : statusMessagePrefix;
      m.m_logger = log;
      m.init(env, inputStructure);
      matchRules.add(m);
    }

    return matchRules;
  }

  /**
   * Make the output instances structure
   * 
   * @throws Exception if a problem occurs
   */
  protected void makeOutputStructure() throws Exception {

    // m_matchRules = new ArrayList<Match>();
    if (m_matchRules.size() > 0) {

      int labelCount = 0;
      // StringBuffer labelList = new StringBuffer();
      HashSet<String> uniqueLabels = new HashSet<String>();
      Vector<String> labelVec = new Vector<String>();
      for (SubstringLabelerMatchRule m : m_matchRules) {
        if (m.getLabel() != null && m.getLabel().length() > 0) {
          if (!uniqueLabels.contains(m.getLabel())) {
            /*
             * if (labelCount > 0) { labelList.append(","); }
             */
            // labelList.append(m.getLabel());
            uniqueLabels.add(m.getLabel());
            labelVec.addElement(m.getLabel());
          }
          labelCount++;
        }
      }

      if (labelCount > 0) {
        if (labelCount == m_matchRules.size()) {
          m_hasLabels = true;
        } else {
          throw new Exception("Can't have only some rules with a label!");
        }
      }

      m_outputStructure =
        (Instances) (new SerializedObject(m_inputStructure).getObject());
      Attribute newAtt = null;
      if (m_hasLabels) {
        newAtt = new Attribute(m_attName, labelVec);
      } else if (m_nominalBinary) {
        labelVec.addElement("0");
        labelVec.addElement("1");
        newAtt = new Attribute(m_attName, labelVec);
      } else {
        newAtt = new Attribute(m_attName);
      }

      m_outputStructure.insertAttributeAt(newAtt,
        m_outputStructure.numAttributes());

      /*
       * // make the output structure m_addFilter = new Add();
       * m_addFilter.setAttributeName(m_attName); if (m_hasLabels) {
       * m_addFilter.setNominalLabels(labelList.toString()); } else if
       * (getNominalBinary()) { m_addFilter.setNominalLabels("0,1"); }
       * m_addFilter.setInputFormat(inputStructure); m_outputStructure =
       * Filter.useFilter(inputStructure, m_addFilter);
       */

      return;
    }

    m_outputStructure = new Instances(m_inputStructure);
  }

  /**
   * Process and input instance and return an output instance
   * 
   * @param inputI the incoming instance
   * @param batch whether this is being processed as part of a batch of
   *          instances
   * 
   * @throws Exception if the output structure has not yet been determined
   * @return the output instance
   */
  public Instance makeOutputInstance(Instance inputI, boolean batch)
    throws Exception {

    if (m_outputStructure == null) {
      throw new Exception("OutputStructure has not been determined!");
    }

    int newAttIndex = m_outputStructure.numAttributes() - 1;

    Instance result = inputI;
    if (m_matchRules.size() > 0) {
      String label = null;
      int[] labelVotes = new int[m_matchRules.size()];
      int index = 0;
      for (SubstringLabelerMatchRule m : m_matchRules) {
        label = m.apply(inputI);

        if (label != null) {
          if (m_voteLabels) {
            labelVotes[index]++;
          } else {
            break;
          }
        }
        index++;
      }

      if (m_voteLabels && Utils.sum(labelVotes) > 0) {
        int maxIndex = Utils.maxIndex(labelVotes);
        label = m_matchRules.get(maxIndex).getLabel();
      }

      double[] vals = new double[m_outputStructure.numAttributes()];
      for (int i = 0; i < inputI.numAttributes(); i++) {
        if (!inputI.attribute(i).isString()) {
          vals[i] = inputI.value(i);
        } else {
          if (!batch) {
            vals[i] = 0;
            String v = inputI.stringValue(i);
            m_outputStructure.attribute(i).setStringValue(v);
          } else {
            String v = inputI.stringValue(i);
            vals[i] = m_outputStructure.attribute(i).addStringValue(v);
          }
        }
      }

      if (label != null) {
        if (m_hasLabels) {
          vals[newAttIndex] =
            m_outputStructure.attribute(m_attName).indexOfValue(label);
        } else {
          vals[newAttIndex] = 1;
        }
      } else { // non match
        if (m_hasLabels) {
          if (!getConsumeNonMatching()) {
            vals[newAttIndex] = Utils.missingValue();
          } else {
            return null;
          }
        } else {
          vals[newAttIndex] = 0;
        }
      }

      result = new DenseInstance(1.0, vals);
      result.setDataset(m_outputStructure);
    }

    return result;
  }

  /**
   * Inner class encapsulating the logic for matching
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  public static class SubstringLabelerMatchRule implements Serializable {

    /** Separator for parts of the match specification */
    public static final String MATCH_PART_SEPARATOR = "@@MR@@";

    private static final long serialVersionUID = 6518104085439241523L;

    /** The substring literal/regex to use for matching */
    protected String m_match = "";

    /** The label (if any) for this rule */
    protected String m_label = "";

    /** True if a regular expression match is to be used */
    protected boolean m_regex;

    /** True if case should be ignored when matching */
    protected boolean m_ignoreCase;

    /** Precompiled regex pattern */
    protected Pattern m_regexPattern;

    /** The attributes to apply the match-replace rule to */
    protected String m_attsToApplyTo = "";

    /** Resolved match string */
    protected String m_matchS;

    /** Resolved label string */
    protected String m_labelS;

    /** Attributes to apply to */
    protected int[] m_selectedAtts;

    /** Status message prefix */
    protected String m_statusMessagePrefix;

    /** Logger to use */
    protected Logger m_logger;

    /**
     * Constructor
     */
    public SubstringLabelerMatchRule() {
    }

    /**
     * Constructor
     * 
     * @param setup an internally encoded representation of all the match
     *          information for this rule
     */
    public SubstringLabelerMatchRule(String setup) {
      parseFromInternal(setup);
    }

    /**
     * Constructor
     * 
     * @param match the match string
     * @param regex true if this is a regular expression match
     * @param ignoreCase true if case is to be ignored
     * @param selectedAtts the attributes to apply the rule to
     */
    public SubstringLabelerMatchRule(String match, boolean regex,
      boolean ignoreCase, String selectedAtts) {
      m_match = match;
      m_regex = regex;
      m_ignoreCase = ignoreCase;
      m_attsToApplyTo = selectedAtts;
    }

    /**
     * Parses from the internal representation
     * 
     * @param setup
     */
    protected void parseFromInternal(String setup) {
      String[] parts = setup.split(MATCH_PART_SEPARATOR);
      if (parts.length < 4 || parts.length > 5) {
        throw new IllegalArgumentException("Malformed match definition: "
          + setup);
      }

      m_attsToApplyTo = parts[0].trim();
      m_regex = parts[1].trim().toLowerCase().equals("t");
      m_ignoreCase = parts[2].trim().toLowerCase().equals("t");
      m_match = parts[3].trim();

      if (m_match == null || m_match.length() == 0) {
        throw new IllegalArgumentException("Must provide something to match!");
      }

      if (parts.length == 5) {
        m_label = parts[4].trim();
      }
    }

    /**
     * Set the string/regex to use for matching
     * 
     * @param match the match string
     */
    public void setMatch(String match) {
      m_match = match;
    }

    /**
     * Get the string/regex to use for matching
     * 
     * @return the match string
     */
    public String getMatch() {
      return m_match;
    }

    /**
     * Set the label to assign if this rule matches, or empty string if binary
     * flag attribute is being created.
     * 
     * @param label the label string or empty string
     */
    public void setLabel(String label) {
      m_label = label;
    }

    /**
     * Get the label to assign if this rule matches, or empty string if binary
     * flag attribute is being created.
     * 
     * @return the label string or empty string
     */
    public String getLabel() {
      return m_label;
    }

    /**
     * Set whether this is a regular expression match or not
     * 
     * @param regex true if this is a regular expression match
     */
    public void setRegex(boolean regex) {
      m_regex = regex;
    }

    /**
     * Get whether this is a regular expression match or not
     * 
     * @return true if this is a regular expression match
     */
    public boolean getRegex() {
      return m_regex;
    }

    /**
     * Set whether to ignore case when matching
     * 
     * @param ignore true if case is to be ignored
     */
    public void setIgnoreCase(boolean ignore) {
      m_ignoreCase = ignore;
    }

    /**
     * Get whether to ignore case when matching
     * 
     * @return true if case is to be ignored
     */
    public boolean getIgnoreCase() {
      return m_ignoreCase;
    }

    /**
     * Set the attributes to apply the rule to
     * 
     * @param a the attributes to apply the rule to.
     */
    public void setAttsToApplyTo(String a) {
      m_attsToApplyTo = a;
    }

    /**
     * Get the attributes to apply the rule to
     * 
     * @return the attributes to apply the rule to.
     */
    public String getAttsToApplyTo() {
      return m_attsToApplyTo;
    }

    /**
     * Initialize this match rule by substituting any environment variables in
     * the attributes, match and label strings. Sets up the attribute indices to
     * apply to and validates that the selected attributes are all String
     * attributes
     * 
     * @param env the environment variables
     * @param structure the structure of the incoming instances
     */
    public void init(Environment env, Instances structure) {
      m_matchS = m_match;
      m_labelS = m_label;
      String attsToApplyToS = m_attsToApplyTo;

      try {
        m_matchS = env.substitute(m_matchS);
        m_labelS = env.substitute(m_labelS);
        attsToApplyToS = env.substitute(attsToApplyToS);
      } catch (Exception ex) {
      }

      if (m_regex) {
        String match = m_matchS;
        if (m_ignoreCase) {
          match = match.toLowerCase();
        }

        // precompile regular expression for speed
        m_regexPattern = Pattern.compile(match);
      }

      // Try a range first for the attributes
      String tempRangeS = attsToApplyToS;
      tempRangeS =
        tempRangeS.replace("/first", "first").replace("/last", "last");
      Range tempR = new Range();
      tempR.setRanges(attsToApplyToS);
      try {
        tempR.setUpper(structure.numAttributes() - 1);
        m_selectedAtts = tempR.getSelection();
      } catch (IllegalArgumentException ex) {
        // probably contains attribute names then
        m_selectedAtts = null;
      }

      if (m_selectedAtts == null) {
        // parse the comma separated list of attribute names
        Set<Integer> indexes = new HashSet<Integer>();
        String[] attParts = m_attsToApplyTo.split(",");
        for (String att : attParts) {
          att = att.trim();
          if (att.toLowerCase().equals("/first")) {
            indexes.add(0);
          } else if (att.toLowerCase().equals("/last")) {
            indexes.add((structure.numAttributes() - 1));
          } else {
            // try and find attribute
            if (structure.attribute(att) != null) {
              indexes.add(new Integer(structure.attribute(att).index()));
            } else {
              if (m_logger != null) {
                String msg =
                  m_statusMessagePrefix + "Can't find attribute '" + att
                    + "in the incoming instances - ignoring";
                m_logger.logMessage(msg);
              }
            }
          }
        }

        m_selectedAtts = new int[indexes.size()];
        int c = 0;
        for (Integer i : indexes) {
          m_selectedAtts[c++] = i.intValue();
        }
      }

      // validate the types of the selected atts
      Set<Integer> indexes = new HashSet<Integer>();
      for (int m_selectedAtt : m_selectedAtts) {
        if (structure.attribute(m_selectedAtt).isString()) {
          indexes.add(m_selectedAtt);
        } else {
          if (m_logger != null) {
            String msg =
              m_statusMessagePrefix + "Attribute '"
                + structure.attribute(m_selectedAtt).name()
                + "is not a string attribute - " + "ignoring";
            m_logger.logMessage(msg);
          }
        }
      }

      // final array
      m_selectedAtts = new int[indexes.size()];
      int c = 0;
      for (Integer i : indexes) {
        m_selectedAtts[c++] = i.intValue();
      }
    }

    /**
     * Apply this rule to the supplied instance
     * 
     * @param inst the instance to apply to
     * 
     * @return the label (or empty string) if this rule matches (empty string is
     *         used to indicate a match in the case that a binary flag attribute
     *         is being created), or null if the rule doesn't match.
     */
    public String apply(Instance inst) {
      for (int i = 0; i < m_selectedAtts.length; i++) {
        if (!inst.isMissing(m_selectedAtts[i])) {
          String value = inst.stringValue(m_selectedAtts[i]);

          String result = apply(value);
          if (result != null) {
            // first match is good enough
            return result;
          }
        }
      }

      return null;
    }

    /**
     * Apply this rule to the supplied string
     * 
     * @param source the string to apply to
     * @return the label (or empty string) if this rule matches (empty string is
     *         used to indicate a match in the case that a binary flag attribute
     *         is being created), or null if the rule doesn't match.
     */
    protected String apply(String source) {
      String result = source;
      String match = m_matchS;
      boolean ruleMatches = false;
      if (m_ignoreCase) {
        result = result.toLowerCase();
        match = match.toLowerCase();
      }
      if (result != null && result.length() > 0) {
        if (m_regex) {
          if (m_regexPattern.matcher(result).matches()) {
            // if (result.matches(match)) {
            ruleMatches = true;
          }
        } else {
          ruleMatches = (result.indexOf(match) >= 0);
        }
      }

      return (ruleMatches) ? m_label : null;
    }

    /**
     * Return a textual description of this match rule
     * 
     * @return a textual description of this match rule
     */
    @Override
    public String toString() {
      // return a nicely formatted string for display
      // that shows all the details

      StringBuffer buff = new StringBuffer();
      buff.append((m_regex) ? "Regex: " : "Substring: ");
      buff.append(m_match).append("  ");
      buff.append((m_ignoreCase) ? "[ignore case]" : "").append("  ");
      if (m_label != null && m_label.length() > 0) {
        buff.append("Label: ").append(m_label).append("  ");
      }
      buff.append("[Atts: " + m_attsToApplyTo + "]");

      return buff.toString();
    }

    /**
     * Get the internal representation of this rule
     *
     * @return a string formatted in the internal representation
     */
    public String toStringInternal() {

      // return a string in internal format that is
      // easy to parse all the data out of
      StringBuffer buff = new StringBuffer();
      buff.append(m_attsToApplyTo).append(MATCH_PART_SEPARATOR);
      buff.append((m_regex) ? "t" : "f").append(MATCH_PART_SEPARATOR);
      buff.append((m_ignoreCase) ? "t" : "f").append(MATCH_PART_SEPARATOR);
      buff.append(m_match).append(MATCH_PART_SEPARATOR);
      buff.append(m_label);

      return buff.toString();
    }
  }
}
