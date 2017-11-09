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
 *    ReplaceWithMissingValue.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.unsupervised.attribute;

import weka.core.*;
import weka.filters.SimpleBatchFilter;
import weka.filters.UnsupervisedFilter;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

/**
 * <!-- globalinfo-start -->
 * A filter that can be used to introduce missing values in a dataset.
 * The specified probability is used to flip a biased coin to decide whether to replace a particular
 * attribute value in an instance with a missing value (i.e., a probability of 0.9 means 90% of values
 * will be replaced with missing values). This filter only modifies the first batch of data that is processed.
 * The class attribute is skipped by default.
 * <br><br>
 * <!-- globalinfo-end -->
 *
 * <!-- options-start -->
 * Valid options are: <p>
 * 
 * <pre> -R &lt;col1,col2-col4,...&gt;
 *  Specifies list of columns to modify. First and last are valid indexes.
 *  (default: first-last)</pre>
 * 
 * <pre> -V
 *  Invert matching sense of column indexes.</pre>
 * 
 * <pre> -S &lt;num&gt;
 *  Specify the random number seed (default 1)</pre>
 * 
 * <pre> -P &lt;double&gt;
 *  Specify the probability  (default 0.1)</pre>
 * 
 * <pre> -unset-class-temporarily
 *  Unsets the class index temporarily before the filter is
 *  applied to the data.
 *  (default: no)</pre>
 * 
 * <!-- options-end -->
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 10215 $
 */
public class ReplaceWithMissingValue extends SimpleBatchFilter
        implements UnsupervisedFilter, Randomizable, WeightedAttributesHandler, WeightedInstancesHandler {

  /** for serialization */
  private static final long serialVersionUID = -2356630932899796239L;

  /** Stores which columns to turn into nominals */
  protected Range m_Cols = new Range("first-last");

  /** The default columns to turn into nominals */
  protected String m_DefaultCols = "first-last";

  /** The seed for the random number generator */
  protected int m_Seed = 1;

  /** The probability */
  protected double m_Probability = 0.1;

  /** True if the class is to be unset */
  protected boolean m_IgnoreClass = false;

  /**
   * Gets an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> result = new Vector<Option>(4);

    result.addElement(new Option(
            "\tSpecifies list of columns to modify. First"
                    + " and last are valid indexes.\n" + "\t(default: first-last)", "R", 1,
            "-R <col1,col2-col4,...>"));

    result.addElement(new Option("\tInvert matching sense of column indexes.",
            "V", 0, "-V"));

    result.addElement(new Option(
            "\tSpecify the random number seed (default 1)", "S", 1, "-S <num>"));

    result.addElement(new Option(
            "\tSpecify the probability  (default 0.1)", "P", 1, "-P <double>"));

    result.addElement(new Option(
            "\tUnsets the class index temporarily before the filter is\n"
                    + "\tapplied to the data.\n" + "\t(default: no)",
            "unset-class-temporarily", 1, "-unset-class-temporarily"));

    return result.elements();
  }

  /**
   * Parses a given list of options.
   * <p/>
   *
   * <!-- options-start -->
   * Valid options are: <p>
   * 
   * <pre> -R &lt;col1,col2-col4,...&gt;
   *  Specifies list of columns to modify. First and last are valid indexes.
   *  (default: first-last)</pre>
   * 
   * <pre> -V
   *  Invert matching sense of column indexes.</pre>
   * 
   * <pre> -S &lt;num&gt;
   *  Specify the random number seed (default 1)</pre>
   * 
   * <pre> -P &lt;double&gt;
   *  Specify the probability  (default 0.1)</pre>
   * 
   * <pre> -unset-class-temporarily
   *  Unsets the class index temporarily before the filter is
   *  applied to the data.
   *  (default: no)</pre>
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

    String seedString = Utils.getOption('S', options);
    if (seedString.length() != 0) {
      setSeed(Integer.parseInt(seedString));
    } else {
      setSeed(1);
    }

    String probString = Utils.getOption('P', options);
    if (probString.length() != 0) {
      setProbability(Double.parseDouble(probString));
    } else {
      setProbability(0.1);
    }

    setIgnoreClass(Utils.getFlag("unset-class-temporarily", options));

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

    result.add("-S");
    result.add("" + getSeed());

    result.add("-P");
    result.add("" + getProbability());

    if (getIgnoreClass()) {
      result.add("-unset-class-temporarily");
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
  public String ignoreClassTipText() {
    return "The class index will be unset temporarily before the filter is applied.";
  }

  /**
   * Set the IgnoreClass value. Set this to true if the class index is to be
   * unset before the filter is applied.
   *
   * @param newIgnoreClass The new IgnoreClass value.
   */
  public void setIgnoreClass(boolean newIgnoreClass) {
    m_IgnoreClass = newIgnoreClass;
  }

  /**
   * Gets the IgnoreClass value. If this to true then the class index is to
   * unset before the filter is applied.
   *
   * @return the current IgnoreClass value.
   */
  public boolean getIgnoreClass() {
    return m_IgnoreClass;
  }
  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String probabilityTipText() {
    return "Probability to use for replacement.";
  }

  /**
   * Get the probability.
   *
   * @return the probability.
   */
  public double getProbability() {

    return m_Probability;
  }

  /**
   * Set the probability to use.
   *
   * @param newProbability the probability to use.
   */
  public void setProbability(double newProbability) {

    m_Probability = newProbability;
  }
  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String seedTipText() {
    return "Seed for the random number generator.";
  }

  /**
   * Get the random number generator seed value.
   *
   * @return random number generator seed value.
   */
  public int getSeed() {

    return m_Seed;
  }

  /**
   * Set the random number generator seed value.
   *
   * @param newSeed value to use as the random number generator seed.
   */
  public void setSeed(int newSeed) {

    m_Seed = newSeed;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String invertSelectionTipText() {
    return "Set attribute selection mode. If false, only selected"
            + " attributes will be modified'; if"
            + " true, only non-selected attributes will be modified.";
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
   * from these. If true all the other columns are considered for
   * "nominalization".
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
   * Sets which attributes are to be "nominalized" (only numeric attributes
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
   * Sets which attributes are to be transoformed to nominal. (only numeric
   * attributes among the selection will be transformed).
   *
   * @param value an array containing indexes of attributes to nominalize. Since
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
    result.enable(Capabilities.Capability.MISSING_VALUES);

    // class
    result.enableAllClasses();
    result.enable(Capabilities.Capability.MISSING_CLASS_VALUES);
    result.enable(Capabilities.Capability.NO_CLASS);

    return result;
  }

  /**
   * returns true if the output format is immediately available after the input
   * format has been set and not only after all the data has been seen (see
   * batchFinished())
   *
   * @return true
   */
   @Override
  protected boolean hasImmediateOutputFormat() {
    return true;
  }

  /**
   * Determines the output format based on the input format and returns this.
   *
   * @param inputFormat the input format to base the output format on
   * @return the output format
   * @throws Exception in case the determination goes wrong
   */
  @Override
  protected Instances determineOutputFormat(Instances inputFormat) throws Exception {
    return inputFormat;
  }

  /**
   * Returns a string describing this filter.
   *
   * @return a description of the filter suitable for displaying in the
   * explorer/experimenter gui
   */
  @Override
  public String globalInfo() {
    return "A filter that can be used to introduce missing values in a dataset. The specified probability is used to" +
            " flip a biased coin to decide whether to replace a particular attribute value in an instance with a" +
            " missing value (i.e., a probability of 0.9 means 90% of values will be replaced with missing values). " +
            "This filter only modifies the first batch of data that is processed. The class attribute is skipped by default.";
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

    if (isFirstBatchDone()) {
      return instances;
    }

    Instances newData = new Instances(instances, instances.numInstances());
    Random random = new Random(getSeed());

    m_Cols.setUpper(newData.numAttributes() - 1);
    for (Instance inst : instances) {
      double[] values = inst.toDoubleArray();
      for (int i = 0; i < values.length; i++) {
        if (m_Cols.isInRange(i) && (i != instances.classIndex() || getIgnoreClass())) {
          if (random.nextDouble() < getProbability()) {
            values[i] = Utils.missingValue();
          }
        }
      }
      if (inst instanceof SparseInstance) {
        newData.add(new SparseInstance(inst.weight(), values));
      } else {
        newData.add(new DenseInstance(inst.weight(), values));
      }
    }

    return newData;
  }

  /**
   * Returns the revision string.
   *
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 10215 $");
  }


  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String[] argv) {
    runFilter(new ReplaceWithMissingValue(), argv);
  }
}

