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
 *    SubstringReplacer
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
import weka.gui.beans.SubstringReplacerRules;

/**
 * A filter that can replace substrings in the values of string attributes.
 * Multiple match and replace "rules" can be specified - these get applied in
 * the order that they are defined. Each rule can be applied to one or more
 * user-specified input String attributes. Attributes can be specified using
 * either a range list (e.g 1,2-10,last) or by a comma separated list of
 * attribute names (where "/first" and "/last" are special strings indicating
 * the first and last attribute respectively). This filter is intended to be
 * used in a purely streaming fashion - i.e where each instance is transformed,
 * processed and then discarded. This is because it limits the number of string
 * values kept in memory (in the header) for a particular attribute to exactly
 * 1, i.e. the value of the current instance being processed.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFIgnore
public class SubstringReplacer extends SimpleStreamFilter implements
  PreconstructedFilter {

  private static final long serialVersionUID = -8387671051707068992L;
  protected SubstringReplacerRules m_mr;

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

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> options = new Vector<Option>();

    options.add(new Option("\tMatch rule. May be supplied multiple times.\n\t"
      + " (format <att name/index>@@MR@@<use regex>@@MR@@"
      + "<ignore case>@@MR@@<match string>@@MR@@<replace string>",
      "match-rule", 1, "-match-rule <rule>"));

    return options.elements();
  }

  @Override
  public String[] getOptions() {
    List<String> options = new ArrayList<String>();

    for (String s : super.getOptions()) {
      options.add(s);
    }

    for (String s : m_matchSpecs) {
      options.add("-match-rule");
      options.add(s);
    }

    return options.toArray(new String[options.size()]);
  }

  @Override
  public void setOptions(String[] options) throws Exception {
    m_matchSpecs.clear();

    super.setOptions(options);

    while (true) {
      String rule = Utils.getOption("match-rule", options);
      if (rule.length() == 0) {
        break;
      }

      m_matchSpecs.add(rule.trim());
    }
  }

  public String convertToSingleInternalString() {
    StringBuilder b = new StringBuilder();

    for (int i = 0; i < m_matchSpecs.size(); i++) {
      if (i != 0) {
        b.append("@@match-replace@@");
      }
      b.append(m_matchSpecs.get(i));
    }

    return b.toString();
  }

  public void convertFromSingleInternalString(String specs) {
    String[] rules = specs.split("@@match-replace@@");
    m_matchSpecs.clear();

    for (String r : rules) {
      m_matchSpecs.add(r);
    }
  }

  protected void addMatchRule(String rule) {
    m_matchSpecs.add(rule);
  }

  @Override
  public String globalInfo() {
    return "Replaces substrings in String attribute values "
      + "using either literal match and replace or "
      + "regular expression matching. The attributes"
      + "to apply the match and replace rules to "
      + "can be selected via a range string (e.g "
      + "1-5,6,last) or by a comma separated list "
      + "of attribute names (/first and /last can be"
      + " used to indicate the first and last attribute " + "respectively)";
  }

  @Override
  protected Instances determineOutputFormat(Instances inputFormat)
    throws Exception {

    m_mr =
      new SubstringReplacerRules(convertToSingleInternalString(),
        new Instances(getInputFormat()), "", null, Environment.getSystemWide());

    return getInputFormat();
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

  @Override
  protected Instance process(Instance instance) throws Exception {
    // m_mr.applyRules(instance);
    return m_mr.makeOutputInstance(instance);
    // return instance;
  }

  @Override
  public boolean isConstructed() {
    return getInputFormat() != null && m_mr != null;
  }

  @Override
  public void resetPreconstructed() {
    m_mr = null;
  }

  public static void main(String[] argv) {
    runFilter(new SubstringReplacer(), argv);
  }
}
