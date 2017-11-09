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
 *    OrdinalToNumeric.java
 *    Copyright (C) 1999-2017 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.unsupervised.attribute;

import weka.core.*;
import weka.filters.StreamableFilter;
import weka.filters.UnsupervisedFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * An attribute filter that converts ordinal nominal attributes into numeric ones
 * <br><br>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p>
 * 
 * <pre> -unset-class-temporarily
 *  Unsets the class index temporarily before the filter is
 *  applied to the data.
 *  (default: no)</pre>
 * 
 * <pre> -R &lt;range or list of names&gt;
 *  Attributes to operate on. Can be a 1-based index range of indices, or a comma-separated list of names.
 *  (default: first-last)</pre>
 * 
 <!-- options-end -->
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class OrdinalToNumeric extends PotentialClassIgnorer implements
  StreamableFilter, UnsupervisedFilter, EnvironmentHandler, WeightedAttributesHandler, WeightedInstancesHandler  {

  /** For serialization */
  private static final long serialVersionUID = -5199516576940135696L;

  /** For resolving environment variables */
  protected transient Environment m_env = Environment.getSystemWide();

  /** Range of columns to consider */
  protected Range m_selectedRange;

  /** Textual range string */
  protected String m_range = "first-last";

  /** Holds the resolved range */
  protected String m_resolvedRange = "";

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {

    return "An attribute filter that converts ordinal nominal attributes into "
      + "numeric ones";
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
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<>();

    Enumeration<Option> e = super.listOptions();
    while (e.hasMoreElements()) {
      result.addElement(e.nextElement());
    }

    result.addElement(new Option("\tAttributes to operate on. Can "
      + "be a 1-based index range of indices, or a comma-separated list of "
      + "names.\n\t(default: first-last)", "R", 1, "-R <range or list of "
      + "names>"));

    return result.elements();
  }

  /**
   * Parses a list of options for this object.
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    String atts = Utils.getOption('R', options);
    if (atts.length() > 0) {
      setAttributesToOperateOn(atts);
    }

    super.setOptions(options);
  }

  /**
   * Gets the current settings of the filter.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {
    Vector<String> result = new Vector<>();

    result.addAll(Arrays.asList(super.getOptions()));
    result.add("-R");
    result.add(getAttributesToOperateOn());

    return result.toArray(new String[result.size()]);
  }

  /**
   * Set the attributes to operate on
   *
   * @param atts a range of 1-based indexes or a comma-separated list of
   *          attribute names
   */
  @OptionMetadata(displayName = "Attributes to operate on",
    description = "Attributes to operate on. Can be a 1-based index range of "
      + "indices or a comma-separated list of names",
    commandLineParamName = "R",
    commandLineParamSynopsis = "-R <range or list of names>", displayOrder = 1)
  public void setAttributesToOperateOn(String atts) {
    m_range = atts;
  }

  /**
   * Get the attributes to operate on
   *
   * @return a range of 1-based indexes or a comma-separated list of attribute
   *         names
   */
  public String getAttributesToOperateOn() {
    return m_range;
  }

  /**
   * Sets the format of the input instances.
   *
   * @param instancesInfo an Instances object containing the input instance
   *          structure (any instances contained in the object are ignored -
   *          only the structure is required).
   * @return true if the outputFormat may be collected immediately
   * @throws Exception if the input format can't be set successfully
   */
  @Override
  public boolean setInputFormat(Instances instancesInfo) throws Exception {
    super.setInputFormat(instancesInfo);

    m_resolvedRange = m_range;

    if (m_env == null) {
      m_env = Environment.getSystemWide();
    }

    if (m_resolvedRange != null && m_resolvedRange.length() > 0) {
      m_resolvedRange = m_env.substitute(m_resolvedRange);

    } else {
      throw new Exception("No attributes to operate on defined");
    }

    m_selectedRange =
      Utils.configureRangeFromRangeStringOrAttributeNameList(instancesInfo,
        m_resolvedRange);

    int[] selectedIndexes = m_selectedRange.getSelection();

    Instances outputFormat = new Instances(instancesInfo, 0);
    if (selectedIndexes.length > 0) {
      ArrayList<Attribute> atts = new ArrayList<>();
      for (int i = 0; i < instancesInfo.numAttributes(); i++) {
        if (m_selectedRange.isInRange(i)
          && instancesInfo.attribute(i).isNominal()
          && i != instancesInfo.classIndex()) {
          Attribute att = new Attribute(instancesInfo.attribute(i).name());
          att.setWeight(instancesInfo.attribute(i).weight());
          atts.add(att);
        } else {
          atts.add(instancesInfo.attribute(i)); // Copy not necessary
        }
      }

      outputFormat = new Instances(instancesInfo.relationName(), atts, 0);
      outputFormat.setClassIndex(instancesInfo.classIndex());
    }

    setOutputFormat(outputFormat);

    return true;
  }

  /**
   * Input an instance for filtering. Ordinarily the instance is processed and
   * made available for output immediately. Some filters require all instances
   * be read before producing output.
   *
   * @param inst the input instance
   * @return true if the filtered instance may now be collected with output().
   * @throws IllegalStateException if no input format has been defined.
   */
  @Override
  public boolean input(Instance inst) throws Exception {
    if (getInputFormat() == null) {
      throw new IllegalStateException("No input format defined");
    }

    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }

    push(inst);

    return true;
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
   * Set environment to use
   *
   * @param env the environment variables to
   */
  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
  }

  /**
   * Main method for testing this class.
   *
   * @param args should contain arguments to the filter: use -h for help
   */
  public static void main(String[] args) {
    runFilter(new OrdinalToNumeric(), args);
  }
}

