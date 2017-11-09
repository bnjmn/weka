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
 *    MergeNominalValues.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.supervised.attribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.*;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.filters.SimpleBatchFilter;
import weka.filters.SupervisedFilter;

/**
 * <!-- globalinfo-start --> Merges values of all nominal attributes among the
 * specified attributes, excluding the class attribute, using the CHAID method,
 * but without considering re-splitting of merged subsets. It implements Steps 1 and
 * 2 described by Kass (1980), see<br/>
 * <br/>
 * Gordon V. Kass (1980). An Exploratory Technique for Investigating Large
 * Quantities of Categorical Data. Applied Statistics. 29(2):119-127.<br/>
 * <br/>
 * Once attribute values have been merged, a chi-squared test using the
 * Bonferroni correction is applied to check if the resulting attribute is a
 * valid predictor, based on the Bonferroni multiplier in Equation 3.2 in Kass
 * (1980). If an attribute does not pass this test, all remaining values (if
 * any) are merged. Nevertheless, useless predictors can slip through without
 * being fully merged, e.g. identifier attributes.<br/>
 * <br/>
 * The code applies the Yates correction when the chi-squared statistic is
 * computed.<br/>
 * <br/>
 * Note that the algorithm is quadratic in the number of attribute values for an
 * attribute.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -D
 *  Turns on output of debugging information.
 * </pre>
 * 
 * <pre>
 * -L &lt;double&gt;
 *  The significance level (default: 0.05).
 * </pre>
 * 
 * <pre>
 * -R &lt;range&gt;
 *  Sets list of attributes to act on (or its inverse). 'first and 'last' are accepted as well.'
 *  E.g.: first-5,7,9,20-last
 *  (default: first-last)
 * </pre>
 * 
 * <pre>
 * -V
 *  Invert matching sense (i.e. act on all attributes not specified in list)
 * </pre>
 * 
 * <pre>
 * -O
 *  Use short identifiers for merged subsets.
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Eibe Frank
 * @version $Revision$
 */
public class MergeNominalValues extends SimpleBatchFilter implements
  SupervisedFilter, WeightedInstancesHandler, WeightedAttributesHandler, TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = 7447337831221353842L;

  /** Set the significance level */
  protected double m_SigLevel = 0.05;

  /** Stores which atributes to operate on (or nto) */
  protected Range m_SelectCols = new Range("first-last");

  /** Stores the indexes of the selected attributes in order. */
  protected int[] m_SelectedAttributes;

  /** Indicators for which attributes need to be changed. */
  protected boolean[] m_AttToBeModified;

  /** The indicators used to map the old values. */
  protected int[][] m_Indicators;

  /** Use short values */
  protected boolean m_UseShortIdentifiers = false;

  /**
   * Returns a string describing this filter.
   * 
   * @return a description of the filter suitable for displaying in the
   *         explorer/experimenter gui
   */
  @Override
  public String globalInfo() {
    return "Merges values of all nominal attributes among the specified attributes, excluding "
      + "the class attribute, using the CHAID method, but without considering re-splitting of "
      + "merged subsets. It implements Steps 1 and 2 described by Kass (1980), see\n\n"
      + getTechnicalInformation().toString()
      + "\n\n"
      + "Once attribute values have been merged, a chi-squared test using the Bonferroni "
      + "correction is applied to check if the resulting attribute is a valid predictor, "
      + "based on the Bonferroni multiplier in Equation 3.2 in Kass (1980). If an attribute does "
      + "not pass this test, all remaining values (if any) are merged. Nevertheless, useless "
      + "predictors can slip through without being fully merged, e.g. identifier attributes.\n\n"
      + "The code applies the Yates correction when the chi-squared statistic is computed.\n\n"
      + "Note that the algorithm is quadratic in the number of attribute values for an attribute.";
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing detailed
   * information about the technical background of this class, e.g., paper
   * reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  @Override
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation result;

    result = new TechnicalInformation(Type.ARTICLE);
    result.setValue(Field.AUTHOR, "Gordon V. Kass");
    result
      .setValue(
        Field.TITLE,
        "An Exploratory Technique for Investigating Large Quantities of Categorical Data");
    result.setValue(Field.JOURNAL, "Applied Statistics");
    result.setValue(Field.YEAR, "1980");
    result.setValue(Field.VOLUME, "29");
    result.setValue(Field.NUMBER, "2");
    result.setValue(Field.PAGES, "119-127");

    return result;
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> result = new Vector<Option>();

    result.addElement(new Option("\tThe significance level (default: 0.05).\n",
      "-L", 1, "-L <double>"));

    result
      .addElement(new Option(
        "\tSets list of attributes to act on (or its inverse). 'first and 'last' are accepted as well.'\n"
          + "\tE.g.: first-5,7,9,20-last\n" + "\t(default: first-last)", "R",
        1, "-R <range>"));

    result
      .addElement(new Option(
        "\tInvert matching sense (i.e. act on all attributes not specified in list)",
        "V", 0, "-V"));

    result.addElement(new Option("\tUse short identifiers for merged subsets.",
      "O", 0, "-O"));

    result.addAll(Collections.list(super.listOptions()));

    return result.elements();
  }

  /**
   * Gets the current settings of the filter.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> result = new Vector<String>();

    result.add("-L");
    result.add("" + getSignificanceLevel());

    if (!getAttributeIndices().equals("")) {
      ;
    }
    {
      result.add("-R");
      result.add(getAttributeIndices());
    }

    if (getInvertSelection()) {
      result.add("-V");
    }

    if (getUseShortIdentifiers()) {
      result.add("-O");
    }

    Collections.addAll(result, super.getOptions());

    return result.toArray(new String[result.size()]);
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   * <!-- options-start --> Valid options are:
   * <p/>
   * 
   * <pre>
   * -D
   *  Turns on output of debugging information.
   * </pre>
   * 
   * <pre>
   * -L &lt;double&gt;
   *  The significance level (default: 0.05).
   * </pre>
   * 
   * <pre>
   * -R &lt;range&gt;
   *  Sets list of attributes to act on (or its inverse). 'first and 'last' are accepted as well.'
   *  E.g.: first-5,7,9,20-last
   *  (default: first-last)
   * </pre>
   * 
   * <pre>
   * -V
   *  Invert matching sense (i.e. act on all attributes not specified in list)
   * </pre>
   * 
   * <pre>
   * -O
   *  Use short identifiers for merged subsets.
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    String significanceLevelString = Utils.getOption('L', options);
    if (significanceLevelString.length() != 0) {
      setSignificanceLevel(Double.parseDouble(significanceLevelString));
    } else {
      setSignificanceLevel(0.05);
    }

    String tmpStr = Utils.getOption('R', options);
    if (tmpStr.length() != 0) {
      setAttributeIndices(tmpStr);
    } else {
      setAttributeIndices("first-last");
    }

    setInvertSelection(Utils.getFlag('V', options));

    setUseShortIdentifiers(Utils.getFlag('O', options));

    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String significanceLevelTipText() {

    return "The significance level for the chi-squared test used to decide when to stop merging.";
  }

  /**
   * Gets the significance level.
   * 
   * @return int the significance level.
   */
  public double getSignificanceLevel() {

    return m_SigLevel;
  }

  /**
   * Sets the significance level.
   * 
   * @param sF the significance level as a double.
   */
  public void setSignificanceLevel(double sF) {

    m_SigLevel = sF;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String attributeIndicesTipText() {

    return "Specify range of attributes to act on (or its inverse)."
      + " This is a comma separated list of attribute indices, with"
      + " \"first\" and \"last\" valid values. Specify an inclusive"
      + " range with \"-\". E.g: \"first-3,5,6-10,last\".";
  }

  /**
   * Get the current range selection.
   * 
   * @return a string containing a comma separated list of ranges
   */
  public String getAttributeIndices() {

    return m_SelectCols.getRanges();
  }

  /**
   * Set which attributes are to be acted on (or not, if invert is true)
   * 
   * @param rangeList a string representing the list of attributes. Since the
   *          string will typically come from a user, attributes are indexed
   *          from 1. <br>
   *          eg: first-3,5,6-last
   */
  public void setAttributeIndices(String rangeList) {

    m_SelectCols.setRanges(rangeList);
  }

  /**
   * Set which attributes are to be acted on (or not, if invert is true)
   * 
   * @param attributes an array containing indexes of attributes to select.
   *          Since the array will typically come from a program, attributes are
   *          indexed from 0.
   */
  public void setAttributeIndicesArray(int[] attributes) {

    setAttributeIndices(Range.indicesToRangeList(attributes));
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String invertSelectionTipText() {

    return "Determines whether selected attributes are to be acted "
      + "on or all other attributes are used instead.";
  }

  /**
   * Get whether the supplied attributes are to be acted on or all other
   * attributes.
   * 
   * @return true if the supplied attributes will be kept
   */
  public boolean getInvertSelection() {

    return m_SelectCols.getInvert();
  }

  /**
   * Set whether selected attributes should be acted on or all other attributes.
   * 
   * @param invert the new invert setting
   */
  public void setInvertSelection(boolean invert) {

    m_SelectCols.setInvert(invert);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String useShortIdentifiersTipText() {

    return "Whether to use short identifiers for the merged values.";
  }

  /**
   * Get whether short identifiers are to be output.
   * 
   * @return true if short IDs are output
   */
  public boolean getUseShortIdentifiers() {

    return m_UseShortIdentifiers;
  }

  /**
   * Set whether to output short identifiers for merged values.
   * 
   * @param b if true, short IDs are output
   */
  public void setUseShortIdentifiers(boolean b) {

    m_UseShortIdentifiers = b;
  }

  /**
   * We need access to the full input data in determineOutputFormat.
   */
  @Override
  public boolean allowAccessToFullInputFormat() {
    return true;
  }

  /**
   * Determines the output format based on the input format and returns this.
   * 
   * @param inputFormat the input format to base the output format on
   * @return the output format
   */
  @Override
  protected Instances determineOutputFormat(Instances inputFormat) {

    // Set the upper limit of the range
    m_SelectCols.setUpper(inputFormat.numAttributes() - 1);

    // Get the selected attributes
    m_SelectedAttributes = m_SelectCols.getSelection();

    // Allocate arrays to store frequencies
    double[][][] freqs = new double[inputFormat.numAttributes()][][];
    for (int m_SelectedAttribute : m_SelectedAttributes) {
      int current = m_SelectedAttribute;
      Attribute att = inputFormat.attribute(current);
      if ((current != inputFormat.classIndex()) && (att.isNominal())) {
        freqs[current] = new double[att.numValues()][inputFormat.numClasses()];
      }
    }

    // Go through all the instances and compute frequencies
    for (Instance inst : inputFormat) {
      for (int m_SelectedAttribute : m_SelectedAttributes) {
        int current = m_SelectedAttribute;
        if ((current != inputFormat.classIndex())
          && (inputFormat.attribute(current).isNominal())) {
          if (!inst.isMissing(current) && !inst.classIsMissing()) {
            freqs[current][(int) inst.value(current)][(int) inst.classValue()] += inst
              .weight();
          }
        }
      }
    }

    // For each attribute in turn merge values
    m_AttToBeModified = new boolean[inputFormat.numAttributes()];
    m_Indicators = new int[inputFormat.numAttributes()][];
    for (int m_SelectedAttribute : m_SelectedAttributes) {
      int current = m_SelectedAttribute;
      if ((current != inputFormat.classIndex())
        && (inputFormat.attribute(current).isNominal())) {

        if (m_Debug) {
          System.err.println(inputFormat.attribute(current));
        }

        // Compute subset indicators
        m_Indicators[current] = mergeValues(freqs[current]);

        if (m_Debug) {
          for (int j = 0; j < m_Indicators[current].length; j++) {
            System.err.print(" - " + m_Indicators[current][j] + " - ");
          }
          System.err.println();
        }

        // Does attribute need to modified?
        for (int k = 0; k < m_Indicators[current].length; k++) {
          if (m_Indicators[current][k] != k) {
            m_AttToBeModified[current] = true;
          }
        }
      }
    }

    // Create new header
    ArrayList<Attribute> atts = new ArrayList<Attribute>();
    for (int i = 0; i < inputFormat.numAttributes(); i++) {
      int current = i;
      Attribute att = inputFormat.attribute(current);
      if (m_AttToBeModified[i]) {

        // Compute number of new values
        int numValues = 0;
        for (int j = 0; j < m_Indicators[current].length; j++) {
          if (m_Indicators[current][j] + 1 > numValues) {
            numValues = m_Indicators[current][j] + 1;
          }
        }

        // Establish new values
        ArrayList<StringBuilder> vals = new ArrayList<StringBuilder>(numValues);
        for (int j = 0; j < numValues; j++) {
          vals.add(null);
        }
        for (int j = 0; j < m_Indicators[current].length; j++) {
          int index = m_Indicators[current][j];

          // Do we already have a value at the given index?
          StringBuilder val = vals.get(index);
          if (val == null) {
            if (m_UseShortIdentifiers) {
              vals.set(index, new StringBuilder("" + (index + 1)));
            } else {
              vals.set(index, new StringBuilder(att.value(j)));
            }
          } else {
            if (!m_UseShortIdentifiers) {
              vals.get(index).append("_or_").append(att.value(j));
            }
          }
        }
        ArrayList<String> valsAsStrings = new ArrayList<String>(vals.size());
        for (StringBuilder val : vals) {
          valsAsStrings.add(val.toString());
        }
        Attribute a = new Attribute(att.name() + "_merged_values", valsAsStrings);
        a.setWeight(att.weight());
        atts.add(a);
      } else {
        atts.add((Attribute) att.copy());
      }
    }

    // Return modified header
    Instances data = new Instances(inputFormat.relationName(), atts, 0);
    data.setClassIndex(inputFormat.classIndex());
    return data;
  }

  /**
   * Compute factor for Bonferroni correction. This is based on Equation 3.2 in
   * Kass (1980).
   */
  protected double BFfactor(int c, int r) {

    double sum = 0;
    double multiplier = 1.0;
    for (int i = 0; i < r; i++) {
      sum += multiplier
        * Math
          .exp((c * Math.log(r - i) - (SpecialFunctions.lnFactorial(i) + SpecialFunctions
            .lnFactorial(r - i))));
      multiplier *= -1.0;
    }
    return sum;
  }

  /**
   * Merges values and returns list of subset indicators for the values.
   */
  protected int[] mergeValues(double[][] counts) {

    int[] indicators = new int[counts.length];

    // Initially, each value is in its own subset
    for (int i = 0; i < indicators.length; i++) {
      indicators[i] = i;
    }

    // Can't merge further if only one subset remains
    while (counts.length > 1) {

      // Find two rows that differ the least according to chi-squared statistic
      double[][] reducedCounts = new double[2][];
      double minVal = Double.MAX_VALUE;
      int toMergeOne = -1;
      int toMergeTwo = -1;
      for (int i = 0; i < counts.length; i++) {
        reducedCounts[0] = counts[i];
        for (int j = i + 1; j < counts.length; j++) {
          reducedCounts[1] = counts[j];
          double val = ContingencyTables.chiVal(reducedCounts, true);
          if (val < minVal) {
            minVal = val;
            toMergeOne = i;
            toMergeTwo = j;
          }
        }
      }

      // Is least significant difference still significant?
      if (Statistics.chiSquaredProbability(minVal, reducedCounts[0].length - 1) <= m_SigLevel) {

        // Check whether overall split is insignificant using Bonferroni
        // correction
        double val = ContingencyTables.chiVal(counts, true);
        int df = (counts[0].length - 1) * (counts.length - 1);
        double originalSig = Statistics.chiSquaredProbability(val, df);
        double adjustedSig = originalSig
          * BFfactor(indicators.length, counts.length);
        if (m_Debug) {
          System.err.println("Original p-value: " + originalSig
            + "\tAdjusted p-value: " + adjustedSig);
        }
        if (!(adjustedSig <= m_SigLevel)) {

          // Not significant: merge all values
          for (int i = 0; i < indicators.length; i++) {
            indicators[i] = 0;
          }
        }
        break;
      }

      // Reduce table by merging
      double[][] newCounts = new double[counts.length - 1][];
      for (int i = 0; i < counts.length; i++) {
        if (i < toMergeTwo) {

          // Can simply copy reference
          newCounts[i] = counts[i];

        } else if (i == toMergeTwo) {

          // Need to add counts
          for (int k = 0; k < counts[i].length; k++) {
            newCounts[toMergeOne][k] += counts[i][k];
          }

        } else {

          // Need to shift row
          newCounts[i - 1] = counts[i];
        }
      }

      // Update membership indicators
      for (int i = 0; i < indicators.length; i++) {

        // All row indices < toMergeTwo remain unmodified
        if (indicators[i] >= toMergeTwo) {
          if (indicators[i] == toMergeTwo) {

            // Need to change index for *all* indicator fields corresponding to
            // merged row
            indicators[i] = toMergeOne;
          } else {

            // We have one row less because toMergeTwo is gone
            indicators[i]--;
          }
        }
      }

      // Replace matrix
      counts = newCounts;
    }
    return indicators;
  }

  /**
   * Returns the Capabilities of this filter.
   * 
   * @return the capabilities of this object
   * @see Capabilities
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result;

    result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enableAllAttributes();
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enableAllClasses();
    result.enable(Capability.MISSING_CLASS_VALUES);

    return result;
  }

  /**
   * Processes the given data.
   * 
   * @param instances the data to process
   * @return the modified data
   * @throws Exception in case the processing goes wrong
   */
  @Override
  protected Instances process(Instances instances) throws Exception {

    // Generate the output and return it
    Instances result = new Instances(getOutputFormat(),
      instances.numInstances());
    for (int i = 0; i < instances.numInstances(); i++) {
      Instance inst = instances.instance(i);
      double[] newData = new double[instances.numAttributes()];
      for (int j = 0; j < instances.numAttributes(); j++) {
        if (m_AttToBeModified[j] && !inst.isMissing(j)) {
          newData[j] = m_Indicators[j][(int) inst.value(j)];
        } else {
          newData[j] = inst.value(j);
        }
      }
      DenseInstance instNew = new DenseInstance(1.0, newData);
      instNew.setDataset(result);

      // copy possible strings, relational values...
      copyValues(instNew, false, inst.dataset(), outputFormatPeek());

      // Add instance to output
      result.add(instNew);
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
    return RevisionUtils.extract("$Revision$");
  }

  /**
   * runs the filter with the given arguments
   * 
   * @param args the commandline arguments
   */
  public static void main(String[] args) {
    runFilter(new MergeNominalValues(), args);
  }
}
