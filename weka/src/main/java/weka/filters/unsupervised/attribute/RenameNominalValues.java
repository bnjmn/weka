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
 *    RenameNominalValues.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.unsupervised.attribute;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import weka.core.*;
import weka.core.Capabilities.Capability;
import weka.filters.Filter;
import weka.filters.StreamableFilter;
import weka.filters.UnsupervisedFilter;

/**
 <!-- globalinfo-start --> 
 * Renames the values of nominal attributes.
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- options-start --> 
 * Valid options are:
 * <p/>
 * 
 * <pre>
 * -R
 *  Attributes to act on. Can be either a range
 *  string (e.g. 1,2,6-10) OR a comma-separated list of named attributes
 *  (default none)
 * </pre>
 * 
 * <pre>
 * -V
 *  Invert matching sense (i.e. act on all attributes other than those specified)
 * </pre>
 * 
 * <pre>
 * -N
 *  Nominal labels and their replacement values.
 *  E.g. red:blue, black:white, fred:bob
 * </pre>
 * 
 * <pre>
 * -I
 *  Ignore case when matching nominal values
 * </pre>
 * 
 <!-- options-end -->
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class RenameNominalValues extends Filter implements UnsupervisedFilter,
  StreamableFilter, OptionHandler, WeightedInstancesHandler, WeightedAttributesHandler {

  /** For serialization */
  private static final long serialVersionUID = -2121767582746512209L;

  /** Range specification or comma-separated list of attribute names */
  protected String m_selectedColsString = "";

  /** The range object to use if a range has been supplied */
  protected Range m_selectedCols = new Range();

  /** The comma-separated list of nominal values and their replacements */
  protected String m_renameVals = "";

  /** True if case is to be ignored when matching nominal values */
  protected boolean m_ignoreCase = false;

  /** True if the matching sense (for attributes) is to be inverted */
  protected boolean m_invert = false;

  /**
   * Stores the indexes of the selected attributes in order, once the dataset is
   * seen
   */
  protected int[] m_selectedAttributes;

  /** The map of nominal values and their replacements */
  protected Map<String, String> m_renameMap = new HashMap<String, String>();

  /**
   * Global help info
   * 
   * @return the help info for this filter
   */
  public String globalInfo() {
    return "Renames the values of nominal attributes.";
  }

  /**
   * Sets the format of the input instances.
   * 
   * @param instanceInfo an Instances object containing the input instance
   *          structure (any instances contained in the object are ignored -
   *          only the structure is required).
   * @return true if the outputFormat may be collected immediately
   * @throws Exception if the format couldn't be set successfully
   */
  @Override
  public boolean setInputFormat(Instances instanceInfo) throws Exception {

    super.setInputFormat(instanceInfo);

    int classIndex = instanceInfo.classIndex();

    // setup the map
    if (m_renameVals != null && m_renameVals.length() > 0) {
      String[] vals = m_renameVals.split(",");

      for (String val : vals) {
        String[] parts = val.split(":");
        if (parts.length != 2) {
          throw new WekaException("Invalid replacement string: " + val);
        }

        if (parts[0].length() == 0 || parts[1].length() == 0) {
          throw new WekaException("Invalid replacement string: " + val);
        }

        m_renameMap.put(
          m_ignoreCase ? parts[0].toLowerCase().trim() : parts[0].trim(),
          parts[1].trim());
      }
    }

    // try selected atts as a numeric range first
    Range tempRange = new Range();
    tempRange.setInvert(m_invert);
    if (m_selectedColsString == null) {
      m_selectedColsString = "";
    }

    try {
      tempRange.setRanges(m_selectedColsString);
      tempRange.setUpper(instanceInfo.numAttributes() - 1);
      m_selectedAttributes = tempRange.getSelection();
      m_selectedCols = tempRange;
    } catch (Exception r) {
      // OK, now try as named attributes
      StringBuffer indexes = new StringBuffer();
      String[] attNames = m_selectedColsString.split(",");
      boolean first = true;
      for (String n : attNames) {
        n = n.trim();
        Attribute found = instanceInfo.attribute(n);
        if (found == null) {
          throw new WekaException("Unable to find attribute '" + n
            + "' in the incoming instances'");
        }
        if (first) {
          indexes.append("" + (found.index() + 1));
          first = false;
        } else {
          indexes.append("," + (found.index() + 1));
        }
      }

      tempRange = new Range();
      tempRange.setRanges(indexes.toString());
      tempRange.setUpper(instanceInfo.numAttributes() - 1);
      m_selectedAttributes = tempRange.getSelection();
      m_selectedCols = tempRange;
    }

    ArrayList<Attribute> attributes = new ArrayList<Attribute>();
    for (int i = 0; i < instanceInfo.numAttributes(); i++) {
      if (m_selectedCols.isInRange(i)) {
        if (instanceInfo.attribute(i).isNominal()) {
          List<String> valsForAtt = new ArrayList<String>();
          for (int j = 0; j < instanceInfo.attribute(i).numValues(); j++) {
            String origV = instanceInfo.attribute(i).value(j);

            String replace = m_ignoreCase ? m_renameMap
              .get(origV.toLowerCase()) : m_renameMap.get(origV);
            if (replace != null) {
              if (!valsForAtt.contains(replace)) {
                valsForAtt.add(replace);
              }
            } else {
              valsForAtt.add(origV);
            }
          }
          Attribute newAtt = new Attribute(instanceInfo.attribute(i).name(), valsForAtt);
          newAtt.setWeight(instanceInfo.attribute(i).weight());
          attributes.add(newAtt);
        } else {
          // ignore any selected attributes that are not nominal
          Attribute att = (Attribute) instanceInfo.attribute(i).copy();
          attributes.add(att);
        }
      } else {
        Attribute att = (Attribute) instanceInfo.attribute(i).copy();
        attributes.add(att);
      }
    }

    Instances outputFormat = new Instances(instanceInfo.relationName(),
      attributes, 0);
    outputFormat.setClassIndex(classIndex);
    setOutputFormat(outputFormat);

    return true;
  }

  /**
   * Input an instance for filtering. Ordinarily the instance is processed and
   * made available for output immediately. Some filters require all instances
   * be read before producing output.
   * 
   * @param instance the input instance
   * @return true if the filtered instance may now be collected with output().
   * @throws IllegalStateException if no input structure has been defined.
   */
  @Override
  public boolean input(Instance instance) {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }

    if (getOutputFormat().numAttributes() == 0) {
      return false;
    }

    if (m_selectedAttributes.length == 0) {
      push(instance);
    } else {
      double vals[] = new double[getOutputFormat().numAttributes()];
      for (int i = 0; i < instance.numAttributes(); i++) {
        double currentV = instance.value(i);

        if (!m_selectedCols.isInRange(i)) {
          vals[i] = currentV;
        } else {
          if (currentV == Utils.missingValue()) {
            vals[i] = currentV;
          } else {
            String currentS = instance.attribute(i).value((int) currentV);
            String replace = m_ignoreCase ? m_renameMap.get(currentS
              .toLowerCase()) : m_renameMap.get(currentS);
            if (replace == null) {
              vals[i] = currentV;
            } else {
              vals[i] = getOutputFormat().attribute(i).indexOfValue(replace);
            }
          }
        }
      }

      Instance inst = null;
      if (instance instanceof SparseInstance) {
        inst = new SparseInstance(instance.weight(), vals);
      } else {
        inst = new DenseInstance(instance.weight(), vals);
      }

      copyValues(inst, false, instance.dataset(), outputFormatPeek());

      push(inst); // No need to copy
    }

    return true;
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
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enableAllClasses();
    result.enable(Capability.MISSING_CLASS_VALUES);
    result.enable(Capability.NO_CLASS);

    return result;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String selectedAttributesTipText() {
    return "The attributes (index range string or explicit "
      + "comma-separated attribute names) to work on";
  }

  public void setSelectedAttributes(String atts) {
    m_selectedColsString = atts;
  }

  public String getSelectedAttributes() {
    return m_selectedColsString;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String valueReplacementsTipText() {
    return "A comma separated list of values to replace and their "
      + "replacements. E.g. red:green, blue:purple, fred:bob";
  }

  public void setValueReplacements(String v) {
    m_renameVals = v;
  }

  public String getValueReplacements() {
    return m_renameVals;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String invertSelectionTipText() {

    return "Determines whether to apply the operation to the specified."
      + " attributes, or all attributes but the specified ones."
      + " If set to true, all attributes but the speficied ones will be affected.";
  }

  /**
   * Get whether the supplied columns are to be removed or kept
   * 
   * @return true if the supplied columns will be kept
   */
  public boolean getInvertSelection() {

    return m_invert;
  }

  /**
   * Set whether selected columns should be removed or kept. If true the
   * selected columns are kept and unselected columns are deleted. If false
   * selected columns are deleted and unselected columns are kept.
   * 
   * @param invert the new invert setting
   */
  public void setInvertSelection(boolean invert) {

    m_invert = invert;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String ignoreCaseTipText() {
    return "Whether to ignore case when matching nominal values";
  }

  public void setIgnoreCase(boolean ignore) {
    m_ignoreCase = ignore;
  }

  public boolean getIgnoreCase() {
    return m_ignoreCase;
  }

  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>(4);
    newVector
      .addElement(new Option(
        "\tAttributes to act on. Can be either a range\n"
          + "\tstring (e.g. 1,2,6-10) OR a comma-separated list of named attributes\n\t"
          + "(default none)", "R", 1, "-R <1,2-4 | attName1,attName2,...>"));
    newVector
      .addElement(new Option(
        "\tInvert matching sense (i.e. act on all attributes other than those specified)",
        "V", 0, "-V"));
    newVector.addElement(new Option(
      "\tNominal labels and their replacement values.\n\t"
        + "E.g. red:blue, black:white, fred:bob", "N", 1,
      "-N <label:label,label:label,...>"));
    newVector.addElement(new Option(
      "\tIgnore case when matching nominal values", "I", 0, "-I"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options.
   * <p>
   * 
   * <!-- options-start --> Valid options are:
   * <p/>
   * 
   * <pre>
   * -R
   *  Attributes to act on. Can be either a range
   *  string (e.g. 1,2,6-10) OR a comma-separated list of named attributes
   *  (default none)
   * </pre>
   * 
   * <pre>
   * -V
   *  Invert matching sense (i.e. act on all attributes other than those specified)
   * </pre>
   * 
   * <pre>
   * -N
   *  Nominal labels and their replacement values.
   *  E.g. red:blue, black:white, fred:bob
   * </pre>
   * 
   * <pre>
   * -I
   *  Ignore case when matching nominal values
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    String atts = Utils.getOption('R', options);
    if (atts.length() > 0) {
      setSelectedAttributes(atts);
    }

    String replacements = Utils.getOption('N', options);
    if (replacements.length() > 0) {
      setValueReplacements(replacements);
    }

    setInvertSelection(Utils.getFlag('V', options));
    setIgnoreCase(Utils.getFlag('I', options));

    Utils.checkForRemainingOptions(options);
  }

  @Override
  public String[] getOptions() {

    List<String> opts = new ArrayList<String>();

    if (getSelectedAttributes() != null && getSelectedAttributes().length() > 0) {
      opts.add("-R");
      opts.add(getSelectedAttributes());
    }

    if (getInvertSelection()) {
      opts.add("-V");
    }

    if (getValueReplacements() != null && getValueReplacements().length() > 0) {
      opts.add("-N");
      opts.add(getValueReplacements());
    }

    if (getIgnoreCase()) {
      opts.add("-I");
    }

    return opts.toArray(new String[opts.size()]);
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  /**
   * Main method for testing this class.
   * 
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String[] argv) {
    runFilter(new RenameNominalValues(), argv);
  }
}
