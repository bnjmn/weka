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
 * DateToNumeric.java
 * Copyright (C) 2006-2017 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.attribute;

import java.util.*;

import weka.core.*;
import weka.core.Capabilities.Capability;
import weka.filters.SimpleBatchFilter;

/**
 * <!-- globalinfo-start -->
 * A filter for turning date attributes into numeric ones.
 * The numeric value will be the number of milliseconds since January 1, 1970, 00:00:00 GMT,
 * corresponding to the given date."
 * <p/>
 * <!-- globalinfo-end -->
 *
 * <!-- options-start -->
 * Valid options are:
 * <p/>
 *
 * <pre>
 * -R &lt;col1,col2-col4,...&gt;
 *  Specifies list of attributes to turn into numeric ones. Only date attributes will be converted.
 *  First and last are valid indexes.
 *  (default: first-last)
 * </pre>
 *
 * <pre>
 * -V
 *  Invert matching sense of column indexes.
 * </pre>
 *
 * <!-- options-end -->
 *
 * @author eibe (eibe at waikato dot ac dot nz)
 * @version $Revision: 14274 $
 */
public class DateToNumeric extends SimpleBatchFilter implements WeightedInstancesHandler, WeightedAttributesHandler {

  /** for serialization */
  private static final long serialVersionUID = -6614650822291796239L;

  /** Stores which columns to turn into numeric attributes */
  protected Range m_Cols = new Range("first-last");

  /** The default columns to turn into numeric attributes */
  protected String m_DefaultCols = "first-last";

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for displaying in the
   *         explorer/experimenter gui
   */
  @Override
  public String globalInfo() {
    return "A filter for turning date attributes into numeric ones. The numeric value will be the number " +
            "of milliseconds since January 1, 1970, 00:00:00 GMT, corresponding to the given date.";
  }

  /**
   * Gets an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> result = new Vector<Option>(2);

    result.addElement(new Option(
      "\tSpecifies list of columns to convert. First"
        + " and last are valid indexes.\n" + "\t(default: first-last)", "R", 1,
      "-R <col1,col2-col4,...>"));

    result.addElement(new Option("\tInvert matching sense of column indexes.",
      "V", 0, "-V"));

    return result.elements();
  }

  /**
   * Parses a given list of options.
   * <p/>
   *
   * <!-- options-start --> Valid options are:
   * <p/>
   *
   * <pre>
   * -R &lt;col1,col2-col4,...&gt;
   *  Specifies list of attributes to turn into numeric ones. Only date attributes will be converted.
   *  First and last are valid indexes.
   *  (default: first-last)
   * </pre>
   *
   * <pre>
   * -V
   *  Invert matching sense of column indexes.
   * </pre>
   *
   * <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    setInvertSelection(Utils.getFlag('V', options));

    String tmpStr = Utils.getOption('R', options);
    if (tmpStr.length() != 0) {
      setAttributeIndices(tmpStr);
    } else {
      setAttributeIndices(m_DefaultCols);
    }

    if (getInputFormat() != null) {
      setInputFormat(getInputFormat());
    }

    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the filter.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> result = new Vector<String>();

    if (!getAttributeIndices().equals("")) {
      result.add("-R");
      result.add(getAttributeIndices());
    }

    if (getInvertSelection()) {
      result.add("-V");
    }

    Collections.addAll(result, super.getOptions());

    return result.toArray(new String[result.size()]);
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String invertSelectionTipText() {
    return "Set attribute selection mode. If false, only selected"
      + " (date) attributes in the range will be turned into numeric attributes; if"
      + " true, only non-selected attributes will be turned into numeric attributes.";
  }

  /**
   * Gets whether the supplied columns are to be worked on or the others.
   *
   * @return true if the supplied columns will be worked on
   */
  public boolean getInvertSelection() {
    return m_Cols.getInvert();
  }

  /**
   * Sets whether selected columns should be worked on or all the others apart
   * from these. If true all the other columns are considered for conversion.
   *
   * @param value the new invert setting
   */
  public void setInvertSelection(boolean value) {
    m_Cols.setInvert(value);
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String attributeIndicesTipText() {
    return "Specify range of attributes to act on."
      + " This is a comma separated list of attribute indices, with"
      + " \"first\" and \"last\" valid values. Specify an inclusive"
      + " range with \"-\". E.g: \"first-3,5,6-10,last\".";
  }

  /**
   * Gets the current range selection
   *
   * @return a string containing a comma separated list of ranges
   */
  public String getAttributeIndices() {
    return m_Cols.getRanges();
  }

  /**
   * Sets which attributes are to be turned into numeric attributes (only date attributes
   * among the selection will be transformed).
   *
   * @param value a string representing the list of attributes. Since the string
   *          will typically come from a user, attributes are indexed from 1. <br>
   *          eg: first-3,5,6-last
   * @throws IllegalArgumentException if an invalid range list is supplied
   */
  public void setAttributeIndices(String value) {
    m_Cols.setRanges(value);
  }

  /**
   * Sets which attributes are to be transformed to numeric attributes (only date
   * attributes among the selection will be transformed).
   *
   * @param value an array containing indexes of attributes to turn into numeric ones. Since
   *          the array will typically come from a program, attributes are
   *          indexed from 0.
   * @throws IllegalArgumentException if an invalid set of ranges is supplied
   */
  public void setAttributeIndicesArray(int[] value) {
    setAttributeIndices(Range.indicesToRangeList(value));
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
   * Determines the output format based on the input format and returns this. In
   * case the output format cannot be returned immediately, i.e.,
   * immediateOutputFormat() returns false, then this method will be called from
   * batchFinished().
   *
   * @param data the input format to base the output format on
   * @return the output format
   * @throws Exception in case the determination goes wrong
   * @see #hasImmediateOutputFormat()
   * @see #batchFinished()
   */
  @Override
  protected Instances determineOutputFormat(Instances data) throws Exception {

    m_Cols.setUpper(data.numAttributes() - 1);
    ArrayList<Attribute> atts = new ArrayList<Attribute>();
    for (int i = 0; i < data.numAttributes(); i++) {
      if (!m_Cols.isInRange(i) || !data.attribute(i).isDate()) {
        atts.add(data.attribute(i));
      } else {
        Attribute newAtt = new Attribute(data.attribute(i).name());
        newAtt.setWeight(data.attribute(i).weight());
        atts.add(newAtt);
      }
    }

    Instances result = new Instances(data.relationName(), atts, 0);
    result.setClassIndex(data.classIndex());

    return result;
  }

  /**
   * This filter's output format is immediately available
   */
  @Override
  protected boolean hasImmediateOutputFormat() {
    return true;
  }

  /**
   * Processes the given data (may change the provided dataset) and returns the
   * modified version. This method is called in batchFinished().
   * 
   * @param instances the data to process
   * @return the modified data
   * @throws Exception in case the processing goes wrong
   * @see #batchFinished()
   */
  @Override
  protected Instances process(Instances instances) throws Exception {

    Instances result = getOutputFormat();

    for (int i = 0; i < instances.numInstances(); i++) {

      Instance newInst = (Instance)instances.instance(i).copy();

      // copy possible string, relational values
      copyValues(newInst, false, instances, outputFormatPeek());

      result.add(newInst);
    }

    return result;
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 14274 $");
  }

  /**
   * Runs the filter with the given parameters. Use -h to list options.
   * 
   * @param args the commandline options
   */
  public static void main(String[] args) {
    runFilter(new DateToNumeric(), args);
  }
}
