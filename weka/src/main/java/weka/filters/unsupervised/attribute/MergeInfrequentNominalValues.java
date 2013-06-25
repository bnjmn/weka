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
 *    MergeInfrequentNominalValues.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.unsupervised.attribute;

import weka.core.Utils;
import weka.core.Instances;
import weka.core.Instance;
import weka.core.DenseInstance;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Option;
import weka.core.Range;
import weka.core.RevisionUtils;

import weka.filters.SimpleBatchFilter;
import weka.filters.UnsupervisedFilter;

import java.util.ArrayList;
import java.util.Vector;
import java.util.Enumeration;

/**
 <!-- globalinfo-start -->
 * Merges all values of the specified nominal attribute that are sufficiently infrequent.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -D
 *  Turns on output of debugging information.</pre>
 * 
 * <pre> -N &lt;int&gt;
 *  The minimum frequency for a value to remain (default: 2).
 * </pre>
 * 
 * <pre> -R &lt;range&gt;
 *  Sets list of attributes to act on (or its inverse). 'first and 'last' are accepted as well.'
 *  E.g.: first-5,7,9,20-last
 *  (default: 1,2)</pre>
 * 
 * <pre> -V
 *  Invert matching sense (i.e. act on all attributes not specified in list)</pre>
 * 
 <!-- options-end -->
 *
 * @author Eibe Frank 
 * @version $Revision: ???? $
 */
public class MergeInfrequentNominalValues extends SimpleBatchFilter implements UnsupervisedFilter {

  /** for serialization */
  static final long serialVersionUID = 4444337331921333847L;

  /** Set the minimum frequency for a value not to be merged. */
  protected int m_MinimumFrequency = 2;

  /** Stores which atributes to operate on (or nto) */
  protected Range m_SelectCols = new Range();

  /** Stores the indexes of the selected attributes in order. */
  protected int[] m_SelectedAttributes;

  /** Indicators for which attributes need to be changed. */
  protected boolean[] m_AttToBeModified;

  /** The new values. */
  protected int[][] m_NewValues;

  /**
   * Returns a string describing this filter.
   *
   * @return      a description of the filter suitable for
   *              displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return 
        "Merges all values of the specified nominal attribute that are sufficiently infrequent.";
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector<Option>        result;
    Enumeration   enm;

    result = new Vector<Option>();

    enm = super.listOptions();
    while (enm.hasMoreElements())
      result.addElement((Option)enm.nextElement());
    
    result.addElement(new Option("\tThe minimum frequency for a value to remain (default: 2).\n",
                                 "-N", 1, "-N <int>"));

    result.addElement(new Option(
	"\tSets list of attributes to act on (or its inverse). 'first and 'last' are accepted as well.'\n"
	+ "\tE.g.: first-5,7,9,20-last\n"
	+ "\t(default: 1,2)",
	"R", 1, "-R <range>"));
    result.addElement(new Option(
        "\tInvert matching sense (i.e. act on all attributes not specified in list)",
        "V", 0, "-V"));

    return result.elements();
  }	  

  /**
   * Gets the current settings of the filter.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {

    Vector<String> result = new Vector<String>();
    String[] options = super.getOptions();
    for (int i = 0; i < options.length; i++) {
      result.add(options[i]);
    }
    
    result.add("-N");
    result.add("" + getMinimumFrequency());

    result.add("-R"); 
    result.add(getAttributeIndices());

    if (getInvertSelection()) {
      result.add("-V");
    }

    return (String[]) result.toArray(new String[result.size()]);	  
  }	  

  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -D
   *  Turns on output of debugging information.</pre>
   * 
   * <pre> -N &lt;int&gt;
   *  The minimum frequency for a value to remain (default: 2).
   * </pre>
   * 
   * <pre> -R &lt;range&gt;
   *  Sets list of attributes to act on (or its inverse). 'first and 'last' are accepted as well.'
   *  E.g.: first-5,7,9,20-last
   *  (default: 1,2)</pre>
   * 
   * <pre> -V
   *  Invert matching sense (i.e. act on all attributes not specified in list)</pre>
   * 
   <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported 
   */
  public void setOptions(String[] options) throws Exception {
    
    String minFrequencyString = Utils.getOption('N', options);
    if (minFrequencyString.length() != 0) {
      setMinimumFrequency(Integer.parseInt(minFrequencyString));
    } else {
      setMinimumFrequency(2);
    }

    String tmpStr = Utils.getOption('R', options);
    if (tmpStr.length() != 0) {
      setAttributeIndices(tmpStr);
    } else {
      setAttributeIndices("");
    }

    setInvertSelection(Utils.getFlag('V', options));

    super.setOptions(options);
  }	  
  
  /**
   * Returns the tip text for this property
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String minimumFrequencyTipText() {

    return "The minimum frequency for a value to remain.";
  }

  /**
   * Gets the minimum frequency.
   *
   * @return int the minimum frequency.
   */
  public int getMinimumFrequency() {

    return m_MinimumFrequency;
  }
    
  /**
   * Sets the minimum frequency.
   *
   * @param the minimum frequency as an integer.
   */
  public void setMinimumFrequency(int minF) {

    m_MinimumFrequency = minF;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
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
   * @param rangeList a string representing the list of attributes.  Since
   * the string will typically come from a user, attributes are indexed from
   * 1. <br>
   * eg: first-3,5,6-last
   */
  public void setAttributeIndices(String rangeList) {

    m_SelectCols.setRanges(rangeList);
  }

  /**
   * Set which attributes are to be acted on (or not, if invert is true)
   *
   * @param attributes an array containing indexes of attributes to select.
   * Since the array will typically come from a program, attributes are indexed
   * from 0.
   */
  public void setAttributeIndicesArray(int[] attributes) {
    
    setAttributeIndices(Range.indicesToRangeList(attributes));
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String invertSelectionTipText() {

    return "Determines whether selected attributes are to be acted " + 
      "on or all other attributes are used instead.";
  }

  /**
   * Get whether the supplied attributes are to be acted on or all other attributes.
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
   * We need access to the full input data in determineOutputFormat.
   */
  public boolean allowAccessToFullInputFormat() {
    return true;
  }

  /**
   * Determines the output format based on the input format and returns 
   * this.
   *
   * @param inputFormat     the input format to base the output format on
   * @return                the output format
   */
  protected Instances determineOutputFormat(Instances inputFormat) {

    // Set the upper limit of the range
    m_SelectCols.setUpper(inputFormat.numAttributes() - 1);

    // Get the selected attributes
    m_SelectedAttributes = m_SelectCols.getSelection();

    // Allocate arrays to store frequencies
    int[][] freqs = new int[inputFormat.numAttributes()][];
    for (int i = 0; i < m_SelectedAttributes.length; i++) {
      int current = m_SelectedAttributes[i];
      Attribute att = inputFormat.attribute(current);
      if ((current != inputFormat.classIndex()) && (att.isNominal())) {
        freqs[current] = new int[att.numValues()];
      }
    }
    
    // Go through all the instances and compute frequencies
    for (Instance inst : inputFormat) {
      for (int i = 0; i < m_SelectedAttributes.length; i++) {
        int current = m_SelectedAttributes[i];
        if ((current != inputFormat.classIndex()) &&
            (inputFormat.attribute(current).isNominal())) {
          if (!inst.isMissing(current)) {
            freqs[current][(int)inst.value(current)]++;
          }
        }
      }
    }

    // Get the number of infrequent values for the corresponding attributes
    int[] numInfrequentValues = new int[inputFormat.numAttributes()];
    for (int i = 0; i < m_SelectedAttributes.length; i++) {
      int current = m_SelectedAttributes[i];
      Attribute att = inputFormat.attribute(current);
      if ((current != inputFormat.classIndex()) && (att.isNominal())) {
        for (int k = 0; k < att.numValues(); k++) {
          if (m_Debug) {
            System.err.println("Attribute: " + att.name() + " Value: " + 
                               att.value(k) + " Freq.: " + freqs[current][k]);
          }
          if (freqs[current][k] < m_MinimumFrequency) {
            numInfrequentValues[current]++;
          }
        }
      }
    }

    // Establish which attributes need to be modified.
    // Also, compute mapping of indices.
    m_AttToBeModified = new boolean[inputFormat.numAttributes()];
    m_NewValues = new int[inputFormat.numAttributes()][];
    for (int i = 0; i < m_SelectedAttributes.length; i++) {
      int current = m_SelectedAttributes[i];
      Attribute att = inputFormat.attribute(current);
      if ((numInfrequentValues[current] > 1)) {

        // Attribute needs to be modified
        m_AttToBeModified[current] = true;

        // Start with index one because 0 refers to merged values
        int j = 1;
        m_NewValues[current] = new int[att.numValues()];
        for (int k = 0; k < att.numValues(); k++) {
          if (freqs[current][k] < m_MinimumFrequency) {
            m_NewValues[current][k] = 0;
          } else {
            m_NewValues[current][k] = j++;
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
        ArrayList<String> vals = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        vals.add(""); // Placeholder
        int[] map = new int[att.numValues()];
        for (int j = 0; j < att.numValues(); j++) {
          if (m_NewValues[current][j] == 0) {
            if (sb.length() != 0) {
              sb.append("_or_");
            }
            sb.append(att.value(j));
          } else {
            vals.add(att.value(j));
          }
        }
        vals.set(0, sb.toString()); // Replace empty string
        atts.add(new Attribute(att.name() + "_merged_infrequent_values", vals));
      } else {
        atts.add((Attribute)att.copy());
      }
    }

    // Return modified header
    Instances data = new Instances(inputFormat.relationName(), atts, 0);
    data.setClassIndex(inputFormat.classIndex());
    return data;
  }

  /** 
   * Returns the Capabilities of this filter.
   *
   * @return            the capabilities of this object
   * @see               Capabilities
   */
  public Capabilities getCapabilities() {
    Capabilities 	result;
    
    result = super.getCapabilities();
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
   * Processes the given data.
   *
   * @param instances   the data to process
   * @return            the modified data
   * @throws Exception  in case the processing goes wrong
   */
  protected Instances process(Instances instances) throws Exception {

    // Generate the output and return it
    Instances result = new Instances(getOutputFormat(), instances.numInstances());
    for (int i = 0; i < instances.numInstances(); i++) {
      Instance inst = instances.instance(i);
      double[] newData = new double[instances.numAttributes()];
      for (int j = 0; j < instances.numAttributes(); j++) {
        if (m_AttToBeModified[j] && !inst.isMissing(j)) {
          newData[j] = m_NewValues[j][(int)inst.value(j)];
        } else {
          newData[j] = inst.value(j);
        }
      }
      DenseInstance instNew = new DenseInstance(1.0, newData);
      instNew.setDataset(result);
      
      // copy possible strings, relational values...
      copyValues(instNew, false, inst.dataset(), getOutputFormat());

      // Add instance to output
      result.add(instNew);
    }
    return result;
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 8034 $");
  }

  /**
   * runs the filter with the given arguments
   *
   * @param args      the commandline arguments
   */
  public static void main(String[] args) {
    runFilter(new MergeInfrequentNominalValues(), args);
  }
}

