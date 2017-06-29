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
 *    SubstringLabeler
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.unsupervised.attribute;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import weka.core.Capabilities;
import weka.core.Environment;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.Utils;
import weka.filters.PreconstructedFilter;
import weka.filters.SimpleStreamFilter;
import weka.gui.beans.KFIgnore;
import weka.gui.beans.SubstringLabelerRules;

/**
 * A filter that finds matches in string attribute values (using either
 * substring or regular expression matches) and labels the instance (sets the
 * value of a new attribute) according to the supplied label for the matching
 * rule. The new label attribute can be either multivalued nominal (if each
 * match rule specified has an explicit label associated with it) or, binary
 * numeric/nominal to indicate that one of the match rules has matched or not
 * matched. This filter is intended to be used in a purely streaming fashion -
 * i.e where each instance is transformed, processed and then discarded. This is
 * because it limits the number of string values kept in memory (in the header)
 * for a particular attribute to exactly 1, i.e. the value of the current
 * instance being processed.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFIgnore
public class SubstringLabeler extends SimpleStreamFilter implements
  PreconstructedFilter {

  private static final long serialVersionUID = -5067951664657470029L;
  protected SubstringLabelerRules m_mr;

  // Can't have this turned on when a classifier using
  // this filter is processing test instances! Luckily,
  // the classifiers that are likely to use this filter
  // ignore instances with missing class values, so
  // the consume option can be safely left turned off.
  protected boolean m_consumeNonMatching;

  protected boolean m_matchUsingRegex;
  protected boolean m_ignoreCase;
  protected boolean m_makeBinaryLabelAttNominal;
  protected String m_newAttName = "class";
  protected boolean m_dontSetNewAttAsClass;

  protected List<String> m_matchSpecs = new ArrayList<String>();

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
   * Options for this filter.
   * 
   * @return an enumeration of command line options for this filter
   */
  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> options = new Vector<Option>();

    options.add(new Option(
      "\tName of the new label attribute (default = 'class').", "att-name", 1,
      "-att-name <name>"));
    options.add(new Option("\tIf the label attribute is binary (and not\n\t"
      + "explicit labels have been defined) then make it\n\t"
      + "a nominal rather than numeric attribute.", "nominal", 0, "-nominal"));
    options.add(new Option("\tConsume non-matching instances.",
      "consume-non-matching", 0, "-consume"));

    options.add(new Option("\tMatch rule. May be supplied multiple times.\n\t"
      + " (format <use regex>@@MR@@"
      + "<ignore case>@@MR@@<match string>@@MR@@<replace string>",
      "match-rule", 1, "-match-rule <rule>"));

    return options.elements();
  }

  /**
   * Get the current option settings of this filter
   *
   * @return the current option settings as an array of strings
   */
  @Override
  public String[] getOptions() {
    List<String> options = new ArrayList<String>();

    options.add("-att-name");
    options.add(getLabelAttributeName());

    if (getMakeBinaryLabelAttributeNominal()) {
      options.add("-nominal");
    }

    if (getConsumeNonMatching()) {
      options.add("-consume");
    }

    for (String s : m_matchSpecs) {
      options.add("-match-rule");
      options.add(s);
    }

    return options.toArray(new String[options.size()]);
  }

  /**
   * Parses a given list of options
   *
   * @param options the list of options as an array of strings
   * @throws Exception if a problem occurs during parsing.
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    m_matchSpecs.clear();
    String name = Utils.getOption("att-name", options);

    if (name.length() > 0) {
      setLabelAttributeName(name);
    } else {
      setLabelAttributeName("class");
    }

    setMakeBinaryLabelAttributeNominal(Utils.getFlag("nominal", options));
    setConsumeNonMatching(Utils.getFlag("consume", options));

    while (true) {
      String rule = Utils.getOption("match-rule", options);
      if (rule.length() == 0) {
        break;
      }

      m_matchSpecs.add(rule.trim());
    }
  }

  /**
   * Set whether to consume non-matching instances, rather than outputting them
   * with a missing value for the new attribute
   *
   * @param consume true if the filter should consume non-matching input
   *          instances
   */
  public void setConsumeNonMatching(boolean consume) {
    m_consumeNonMatching = consume;
  }

  /**
   * Get whether to consume non-matching instances, rather than outputing them
   * with a missing value for the new attribute
   *
   * @return true if the filter should consume non-matching input instances
   */
  public boolean getConsumeNonMatching() {
    return m_consumeNonMatching;
  }

  /**
   * Set the name of the new attribute that will take on the user-specified
   * label values for matching instances
   *
   * @param name the name of the new attribute
   */
  public void setLabelAttributeName(String name) {
    m_newAttName = name;
  }

  /**
   * Get the name of the new attribute that will take on the user-specified
   * label values for matching instances
   *
   * @return the name of the new attribute
   */
  public String getLabelAttributeName() {
    return m_newAttName;
  }

  /**
   * Set whether to make the new attribute a nominal rather than numeric one in
   * the case that it is a binary attribute.
   *
   * @param b true if the new attribute will be a nominal one rather than
   *          numeric in the case it is binary.
   */
  public void setMakeBinaryLabelAttributeNominal(boolean b) {
    m_makeBinaryLabelAttNominal = b;
  }

  /**
   * Get whether to make the new attribute a nominal rather than numeric one in
   * the case that it is a binary attribute.
   *
   * @return true if the new attribute will be a nominal one rather than numeric
   *         in the case it is binary.
   */
  public boolean getMakeBinaryLabelAttributeNominal() {
    return m_makeBinaryLabelAttNominal;
  }

  /**
   * Set whether the new attribute is not to be set as the class in the output
   * data
   *
   * @param dontSetAsClass true to not set the new attribute to be the class
   */
  public void setDontSetLabelAttributeAsClass(boolean dontSetAsClass) {
    m_dontSetNewAttAsClass = dontSetAsClass;
  }

  /**
   * Get whether the new attribute is not to be set as the class in the output
   * data
   *
   * @return true if the new attribute is not to be set as the class
   */
  public boolean getDontSetLabelAttributeAsClass() {
    return m_dontSetNewAttAsClass;
  }

  /**
   * Adds a rule to the list
   *
   * @param rule the rule to add
   */
  protected void addMatchRule(String rule) {
    m_matchSpecs.add(rule);
  }

  /**
   * Get the current matching specs as a single string in internal format
   *
   * @return the current matching specs as a single string
   */
  public String convertToSingleInternalString() {
    StringBuilder b = new StringBuilder();

    for (int i = 0; i < m_matchSpecs.size(); i++) {
      if (i != 0) {
        b.append(SubstringLabelerRules.MATCH_RULE_SEPARATOR);
      }
      b.append(m_matchSpecs.get(i));
    }

    return b.toString();
  }

  /**
   * Initialize matching specs from a single string in internal format
   *
   * @param specs the matching specs string
   */
  public void convertFromSingleInternalString(String specs) {
    String[] rules = specs.split(SubstringLabelerRules.MATCH_RULE_SEPARATOR);
    m_matchSpecs.clear();

    for (String r : rules) {
      m_matchSpecs.add(r);
    }
  }

  /**
   * Global help info for this filter
   *
   * @return help info
   */
  @Override
  public String globalInfo() {
    return "Matches substrings in String attributes using "
      + "either literal or regular expression matches. "
      + "The value of a new attribute is set to reflect"
      + " the status of the match. The new attribute can "
      + "be either binary (in which case values indicate "
      + "match or no match) or multi-valued nominal, "
      + "in which case a label must be associated with each "
      + "distinct matching rule. In the case of labeled matches, "
      + "the user can opt to have non matching instances output "
      + "with missing value set for the new attribute or not"
      + " output at all (i.e. consumed by the step).";
  }

  /**
   * Determines the output format of the data given the input format
   *
   * @param inputFormat the input format to base the output format on
   * @return the output format
   * @throws Exception if a problem occurs
   */
  @Override
  protected Instances determineOutputFormat(Instances inputFormat)
    throws Exception {

    m_mr =
      new SubstringLabelerRules(m_matchSpecs.size() == 0 ? ""
        : convertToSingleInternalString(), getLabelAttributeName(),
        getConsumeNonMatching(), getMakeBinaryLabelAttributeNominal(),
        new Instances(getInputFormat()), "", null, Environment.getSystemWide());

    Instances outStructure = m_mr.getOutputStructure();
    if (!getDontSetLabelAttributeAsClass()) {
      outStructure.setClassIndex(outStructure.numAttributes() - 1);
    }
    return outStructure;
  }

  /**
   * Input an instance for filtering. Filter requires all training instances be
   * read before producing output.
   *
   * @param instance the input instance
   * @return true if the filtered instance may now be collected with output().
   * @throws IllegalStateException if no input structure has been defined
   * @throws Exception if something goes wrong
   */
  @Override
  public boolean input(Instance instance) throws Exception {
    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }

    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }

    try {
      if (hasImmediateOutputFormat() || isFirstBatchDone()) {
        Instance processed = process(instance);
        if (processed != null) {
          // push(process((Instance) instance.copy()));

          // prevent string locator thingys from copying over string attributes
          // values to the header of the output format (so that they don't
          // accumulate in memory
          Instances tempStructure = processed.dataset();
          processed.setDataset(null);
          push(processed);
          processed.setDataset(tempStructure);
          return true;
        }
        return false;
      } else {
        bufferInput(instance);
        return false;
      }
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Process an instance
   *
   * @param instance the instance to process
   * @return the transformed instance
   * @throws Exception if a problem occurs
   */
  @Override
  protected Instance process(Instance instance) throws Exception {
    return m_mr.makeOutputInstance(instance, false);
  }

  /**
   * Returns true if this filter is considered "constructed", i.e. it
   * has an input format and a defined set of rules to apply, and is
   * ready to process data.
   *
   * @return true if the filter is "constructed"
   */
  @Override
  public boolean isConstructed() {
    return getInputFormat() != null && m_mr != null;
  }

  /**
   * Resets the filter to an non-constructed state
   */
  @Override
  public void resetPreconstructed() {
    m_mr = null;
  }
}
