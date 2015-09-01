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
 *    SubstringReplacerRules.java
 *    Copyright (C) 2011-2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.io.Serializable;

import weka.core.DenseInstance;
import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Range;
import weka.gui.Logger;

/**
 * Manages a list of match and replace rules for replacing values in string
 * attributes
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class SubstringReplacerRules implements EnvironmentHandler, Serializable {

  private static final long serialVersionUID = -7151320452496749698L;
  
  /** Environment variables */
  protected transient Environment m_env = Environment.getSystemWide();

  protected List<SubstringReplacerMatchRule> m_matchRules;

  /** The input structure */
  protected Instances m_inputStructure;

  /** The output structure */
  protected Instances m_outputStructure;

  /** An optional prefix for status log messages */
  protected String m_statusMessagePrefix = "";

  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
  }

  /**
   * Constructor
   * 
   * @param matchDetails the internally encoded match details string
   * @param inputStructure the incoming instances structure
   * @param statusMessagePrefix an optional status message prefix string for
   *          logging
   * @param log the log to use (may be null)
   * @param env environment variables
   */
  public SubstringReplacerRules(String matchDetails, Instances inputStructure,
    String statusMessagePrefix, Logger log, Environment env) {

    m_matchRules =
      matchRulesFromInternal(matchDetails, inputStructure, statusMessagePrefix,
        log, env);

    m_inputStructure = new Instances(inputStructure);
    m_outputStructure = new Instances(inputStructure).stringFreeStructure();
    m_env = env;
    m_statusMessagePrefix = statusMessagePrefix;
  }

  /**
   * Constructor. Initializes with system-wide environment variables and uses no
   * status message and no log.
   * 
   * @param matchDetails the internally encoded match details string
   * @param inputStructure the incoming instances structure
   */
  public SubstringReplacerRules(String matchDetails, Instances inputStructure) {

    this(matchDetails, inputStructure, "", null, Environment.getSystemWide());
  }

  /**
   * Get a list of match rules from an internally encoded match specification
   * 
   * @param matchReplaceDetails the internally encoded specification of the
   *          match rules
   * @param inputStructure the input instances structure
   * @param statusMessagePrefix an optional status message prefix for logging
   * @param log the log to use
   * @param env environment variables
   * @return a list of match rules
   */
  public static List<SubstringReplacerMatchRule> matchRulesFromInternal(
    String matchReplaceDetails, Instances inputStructure,
    String statusMessagePrefix, Logger log, Environment env) {

    List<SubstringReplacerMatchRule> matchRules =
      new ArrayList<SubstringReplacerMatchRule>();

    String[] mrParts = matchReplaceDetails.split("@@match-replace@@");
    for (String p : mrParts) {
      SubstringReplacerMatchRule mr = new SubstringReplacerMatchRule(p.trim());
      mr.m_statusMessagePrefix = statusMessagePrefix;
      mr.m_logger = log;
      mr.init(env, inputStructure);
      matchRules.add(mr);
    }

    return matchRules;
  }

  public void applyRules(Instance inst) {
    for (SubstringReplacerMatchRule mr : m_matchRules) {
      mr.apply(inst);
    }
  }

  /**
   * Make an output instance given an input one
   *
   * @param inputI the input instance to process
   * @return the output instance with substrings replaced
   */
  public Instance makeOutputInstance(Instance inputI) {
    double[] vals = new double[m_outputStructure.numAttributes()];
    String[] stringVals = new String[m_outputStructure.numAttributes()];
    for (int i = 0; i < inputI.numAttributes(); i++) {
      if (inputI.attribute(i).isString() && !inputI.isMissing(i)) {
        stringVals[i] = inputI.stringValue(i);
      } else {
        vals[i] = inputI.value(i);
      }
    }

    for (SubstringReplacerMatchRule mr : m_matchRules) {
      mr.apply(stringVals);
    }

    for (int i = 0; i < m_outputStructure.numAttributes(); i++) {
      if (m_outputStructure.attribute(i).isString() && stringVals[i] != null) {
        m_outputStructure.attribute(i).setStringValue(stringVals[i]);
      }
    }

    Instance result = new DenseInstance(inputI.weight(), vals);
    result.setDataset(m_outputStructure);
    return result;
  }

  /**
   * Inner class encapsulating the logic for matching and replacing.
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  public static class SubstringReplacerMatchRule implements Serializable {

    private static final long serialVersionUID = 5792838913737819728L;

    /** The substring literal/regex to use for matching */
    protected String m_match = "";

    /** The string to replace with */
    protected String m_replace = "";

    /** True if a regular expression match is to be used */
    protected boolean m_regex;

    /** Precompiled regex */
    protected Pattern m_regexPattern;

    /** True if case should be ignored when matching */
    protected boolean m_ignoreCase;

    /** The attributes to apply the match-replace rule to */
    protected String m_attsToApplyTo = "";

    protected String m_matchS;
    protected String m_replaceS;

    protected int[] m_selectedAtts;

    protected String m_statusMessagePrefix;
    protected Logger m_logger;

    /**
     * Constructor
     */
    public SubstringReplacerMatchRule() {
    }

    /**
     * Constructor
     * 
     * @param setup an internally encoded representation of all the match and
     *          replace information for this rule
     */
    public SubstringReplacerMatchRule(String setup) {
      parseFromInternal(setup);
    }

    /**
     * Constructor
     * 
     * @param match the match string
     * @param replace the replace string
     * @param regex true if this is a regular expression match
     * @param ignoreCase true if case is to be ignored
     * @param selectedAtts the attributes to apply the rule to
     */
    public SubstringReplacerMatchRule(String match, String replace,
      boolean regex, boolean ignoreCase, String selectedAtts) {
      m_match = match;
      m_replace = replace;
      m_regex = regex;
      m_ignoreCase = ignoreCase;
      m_attsToApplyTo = selectedAtts;
    }

    protected void parseFromInternal(String setup) {
      String[] parts = setup.split("@@MR@@");
      if (parts.length < 4 || parts.length > 5) {
        throw new IllegalArgumentException(
          "Malformed match-replace definition: " + setup);
      }

      m_attsToApplyTo = parts[0].trim();
      m_regex = parts[1].trim().toLowerCase().equals("t");
      m_ignoreCase = parts[2].trim().toLowerCase().equals("t");
      m_match = parts[3].trim();

      if (m_match == null || m_match.length() == 0) {
        throw new IllegalArgumentException("Must provide something to match!");
      }

      if (parts.length == 5) {
        m_replace = parts[4];
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
     * Set the replace string
     * 
     * @param replace the replace string
     */
    public void setReplace(String replace) {
      m_replace = replace;
    }

    /**
     * Get the replace string
     * 
     * @return the replace string
     */
    public String getReplace() {
      return m_replace;
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
     * Initialize this match replace rule by substituting any environment
     * variables in the attributes, match and replace strings. Sets up the
     * attribute indices to apply to and validates that the selected attributes
     * are all String attributes
     * 
     * @param env the environment variables
     * @param structure the structure of the incoming instances
     */
    public void init(Environment env, Instances structure) {
      m_matchS = m_match;
      m_replaceS = m_replace;
      String attsToApplyToS = m_attsToApplyTo;

      try {
        m_matchS = env.substitute(m_matchS);
        m_replaceS = env.substitute(m_replace);
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
     */
    public void apply(Instance inst) {

      for (int i = 0; i < m_selectedAtts.length; i++) {
        int numStringVals = inst.attribute(m_selectedAtts[i]).numValues();
        if (!inst.isMissing(m_selectedAtts[i])) {
          String value = inst.stringValue(m_selectedAtts[i]);
          value = apply(value);
          inst.dataset().attribute(m_selectedAtts[i]).setStringValue(value);

          // only set the index to zero if there were more than 1 string values
          // for this string attribute (meaning that although the data is
          // streaming
          // in, the user has opted to retain all string values in the header.
          // We
          // only operate in pure streaming - one string value in memory at any
          // one time - mode).

          // this check saves time (no new attribute vector created) if there is
          // only one value (i.e. index is already zero).
          if (numStringVals > 1) {
            inst.setValue(m_selectedAtts[i], 0);
          }
        }
      }
    }

    /**
     * Apply this rule to the supplied array of strings. This array is expected
     * to contain string values from an instance at the same index that they
     * occur in the original instance. Null elements indicate non-string or
     * missing values from the original instance
     * 
     * @param stringVals an array of strings containing string values from an
     *          input instance
     */
    public void apply(String[] stringVals) {
      for (int i = 0; i < m_selectedAtts.length; i++) {
        if (stringVals[m_selectedAtts[i]] != null) {
          stringVals[m_selectedAtts[i]] = apply(stringVals[m_selectedAtts[i]]);
        }
      }
    }

    /**
     * Apply this rule to the supplied string
     * 
     * @param source the string to apply to
     * @return the source string with any matching substrings replaced.
     */
    protected String apply(String source) {
      String result = source;
      String match = m_matchS;
      if (m_ignoreCase) {
        result = result.toLowerCase();
        match = match.toLowerCase();
      }
      if (result != null && result.length() > 0) {
        if (m_regex) {
          // result = result.replaceAll(match, m_replaceS);
          result = m_regexPattern.matcher(result).replaceAll(m_replaceS);
        } else {
          result = result.replace(match, m_replaceS);
        }
      }

      return result;
    }

    /**
     * Return a textual description of this rule
     * 
     * @return textual description of this rule
     */
    @Override
    public String toString() {
      // return a nicely formatted string for display
      // that shows all the details

      StringBuffer buff = new StringBuffer();
      buff.append((m_regex) ? "Regex: " : "Substring: ");
      buff.append(m_match).append(" --> ").append(m_replace).append("  ");
      buff.append((m_ignoreCase) ? "[ignore case]" : "").append("  ");
      buff.append("[Atts: " + m_attsToApplyTo + "]");

      return buff.toString();
    }

    /**
     * Return the internally encoded representation of this rule
     *
     * @return the internally (parseable) representation of this rule
     */
    public String toStringInternal() {

      // return a string in internal format that is
      // easy to parse all the data out of
      StringBuffer buff = new StringBuffer();
      buff.append(m_attsToApplyTo).append("@@MR@@");
      buff.append((m_regex) ? "t" : "f").append("@@MR@@");
      buff.append((m_ignoreCase) ? "t" : "f").append("@@MR@@");
      buff.append(m_match).append("@@MR@@");
      buff.append(m_replace);

      return buff.toString();
    }
  }
}
