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
 * RandomSubset.java
 * Copyright (C) 2007-2012 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.attribute;

import java.util.*;

import weka.core.*;
import weka.core.Capabilities.Capability;
import weka.filters.SimpleBatchFilter;


/**
 <!-- globalinfo-start -->
 * Chooses a random subset of non-class attributes, either an absolute number or a percentage. Attributes are included
 * in the order in which they occur in the input data. The class attribute (if present) is always included in the output.
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -N &lt;double&gt;
 *  The number of attributes to randomly select.
 *  If &lt; 1 then percentage, &gt;= 1 absolute number.
 *  (default: 0.5)</pre>
 * 
 * <pre> -V
 *  Invert selection - i.e. randomly remove rather than select.</pre>
 * 
 * <pre> -S &lt;int&gt;
 *  The seed value.
 *  (default: 1)</pre>
 * 
 * <pre> -output-debug-info
 *  If set, filter is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -do-not-check-capabilities
 *  If set, filter capabilities are not checked before filter is built
 *  (use with caution).</pre>
 * 
 <!-- options-end -->
 * 
 * @author fracpete (fracpete at waikato dot ac dot nz)
 * @author eibe@cs.waikato.ac.nz
 * @version $Revision$
 */
public class RandomSubset extends SimpleBatchFilter
        implements Randomizable, WeightedInstancesHandler, WeightedAttributesHandler {

  /** for serialization. */
  private static final long serialVersionUID = 2911221724251628050L;

  /**
   * The number of attributes to randomly choose (&gt;= 1 absolute number of
   * attributes, &lt; 1 percentage).
   */
  protected double m_NumAttributes = 0.5;

  /** The seed value. */
  protected int m_Seed = 1;

  /** The indices of the attributes that got selected. */
  protected int[] m_Indices = null;

  /** Whether to randomly remove rather than select */
  protected boolean m_invertSelection;

  /**
   * Returns a string describing this filter.
   * 
   * @return a description of the filter suitable for displaying in the
   *         explorer/experimenter gui
   */
  @Override
  public String globalInfo() {
    return "Chooses a random subset of non-class attributes, either an absolute number "
      + "or a percentage. Attributes are included in the order in which they occur in the input data. The class "
      + "attribute (if present) is always included in the output.";
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> result = new Vector<Option>();

    result.addElement(new Option(
      "\tThe number of attributes to randomly select.\n"
        + "\tIf < 1 then percentage, >= 1 absolute number.\n"
        + "\t(default: 0.5)", "N", 1, "-N <double>"));

    result.addElement(new Option(
      "\tInvert selection - i.e. randomly remove rather than select.", "V", 0,
      "-V"));

    result.addElement(new Option("\tThe seed value.\n" + "\t(default: 1)", "S",
      1, "-S <int>"));

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

    result.add("-N");
    result.add("" + m_NumAttributes);

    if (getInvertSelection()) {
      result.add("-V");
    }

    result.add("-S");
    result.add("" + m_Seed);

    Collections.addAll(result, super.getOptions());

    return result.toArray(new String[result.size()]);
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -N &lt;double&gt;
   *  The number of attributes to randomly select.
   *  If &lt; 1 then percentage, &gt;= 1 absolute number.
   *  (default: 0.5)</pre>
   * 
   * <pre> -V
   *  Invert selection - i.e. randomly remove rather than select.</pre>
   * 
   * <pre> -S &lt;int&gt;
   *  The seed value.
   *  (default: 1)</pre>
   * 
   * <pre> -output-debug-info
   *  If set, filter is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   * <pre> -do-not-check-capabilities
   *  If set, filter capabilities are not checked before filter is built
   *  (use with caution).</pre>
   * 
   <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    String tmpStr;

    tmpStr = Utils.getOption("N", options);
    if (tmpStr.length() != 0) {
      setNumAttributes(Double.parseDouble(tmpStr));
    } else {
      setNumAttributes(0.5);
    }

    setInvertSelection(Utils.getFlag('V', options));

    tmpStr = Utils.getOption("S", options);
    if (tmpStr.length() != 0) {
      setSeed(Integer.parseInt(tmpStr));
    } else {
      setSeed(1);
    }

    super.setOptions(options);
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String numAttributesTipText() {
    return "The number of attributes to choose: < 1 percentage, >= 1 absolute number.";
  }

  /**
   * Get the number of attributes (&lt; 1 percentage, &gt;= 1 absolute number).
   * 
   * @return the number of attributes.
   */
  public double getNumAttributes() {
    return m_NumAttributes;
  }

  /**
   * Set the number of attributes.
   * 
   * @param value the number of attributes to use.
   */
  public void setNumAttributes(double value) {
    m_NumAttributes = value;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String invertSelectionTipText() {
    return "Randomly remove rather than select attributes.";
  }

  /**
   * Set whether to invert the selection - i.e. randomly remove rather than
   * select attributes.
   * 
   * @param inv true if the selection should be inverted
   */
  public void setInvertSelection(boolean inv) {
    m_invertSelection = inv;
  }

  /**
   * Get whether to invert the selection - i.e. randomly remove rather than
   * select attributes.
   * 
   * @return true if the selection should be inverted
   */
  public boolean getInvertSelection() {
    return m_invertSelection;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String seedTipText() {
    return "The seed value for the random number generator.";
  }

  /**
   * Get the seed value for the random number generator.
   * 
   * @return the seed value.
   */
  public int getSeed() {
    return m_Seed;
  }

  /**
   * Set the seed value for the random number generator.
   * 
   * @param value the seed value.
   */
  public void setSeed(int value) {
    m_Seed = value;
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
   * Returns whether to allow the determineOutputFormat(Instances) method access
   * to the full dataset rather than just the header.
   * <p/>
   *
   * @return true for this filter so that input data can affect subset of attributes that is selected
   */
  public boolean allowAccessToFullInputFormat() {
    return true;
  }

  /**
   * Determines the output format based on the input format and returns this. In
   * case the output format cannot be returned immediately, i.e.,
   * hasImmediateOutputFormat() returns false, then this method will called from
   * batchFinished() after the call of preprocess(Instances), in which, e.g.,
   * statistics for the actual processing step can be gathered.
   * 
   * @param inputFormat the input format to base the output format on
   * @return the output format
   * @throws Exception in case the determination goes wrong
   */
  @Override
  protected Instances determineOutputFormat(Instances inputFormat) throws Exception {

    // determine the number of attributes
    int numAttsWithoutClass = inputFormat.numAttributes();
    if (inputFormat.classIndex() > -1) {
      numAttsWithoutClass--;
    }

    int sizeOfSample = 0;
    if (m_NumAttributes < 1) {
      sizeOfSample = (int) Math.round(numAttsWithoutClass * m_NumAttributes);
    } else {
      if (m_NumAttributes < numAttsWithoutClass) {
        sizeOfSample = (int) m_NumAttributes;
      }
    }
    if (getDebug()) {
      System.out.println("# of atts: " + sizeOfSample);
    }

    // Get a random number generator that depends on the particular dataset passed in
    Random rand = inputFormat.getRandomNumberGenerator(getSeed());

    // The random indices (we will need to take care of the class attribute)
    int[] indices = RandomSample.drawSortedSample(sizeOfSample, numAttsWithoutClass, rand);

    // Do we need to take the inverse?
    if (m_invertSelection) {
      int[] newIndices = new int[numAttsWithoutClass - indices.length];
      int index = 0;
      int indexNew = 0;
      int i = 0;
      while ((i < numAttsWithoutClass)) {
        while ((indexNew < newIndices.length) && ((indices.length <= index) || (i < indices[index]))) {
          newIndices[indexNew++] = i++;
        }
        index++;
        i++;
      }
      indices = newIndices;
    }

    // Make a new list of indices, taking care of the class
    List<Integer> selected  = new ArrayList<>();
    int newClassIndex = -1;
    if (inputFormat.classIndex() > -1) {
      for (int i = 0; i < indices.length; i++) {
        int index = indices[i];
        if (index < inputFormat.classIndex()) {
          selected.add(index);
        } else {
          selected.add(index + 1);
        }
      }
      newClassIndex = -Collections.binarySearch(selected, inputFormat.classIndex()) - 1;
      selected.add(newClassIndex, inputFormat.classIndex());
    } else {
      for (int i = 0; i < indices.length; i++) {
        selected.add(indices[i]);
      }
    }

    if (getDebug()) {
      System.out.println("Selected indices: " + selected);
    }

    // generate output format
    ArrayList<Attribute> atts = new ArrayList<>();
    m_Indices = new int[selected.size()];
    for (int i = 0; i < selected.size(); i++) {
      atts.add((Attribute)inputFormat.attribute(selected.get(i)).copy());
      m_Indices[i] = selected.get(i);
    }
    Instances result = new Instances(inputFormat.relationName(), atts, 0).stringFreeStructure();
    if (inputFormat.classIndex() > -1) {
      result.setClassIndex(newClassIndex);
    }

    initInputLocators(inputFormatPeek(), m_Indices);

    return result;
  }

  /**
   * processes the given instance (may change the provided instance) and returns
   * the modified version.
   * 
   * @param instances the instance to process
   * @return the modified data
   * @throws Exception in case the processing goes wrong
   */
  @Override
  protected Instances process(Instances instances) throws Exception {

    Instances result = new Instances(outputFormatPeek(), 0);
    for (Instance instance : instances) {
      Instance newInstance;
      if (instance instanceof SparseInstance) {
        int n1 = instance.numValues();
        int n2 = m_Indices.length;
        int[] indices = new int[instance.numValues()];
        double[] values = new double[instance.numValues()];
        int vals = 0;
        for (int p1 = 0, p2 = 0; p1 < n1 && p2 < n2; ) {
          int ind1 = instance.index(p1);
          int ind2 = m_Indices[p2];
          if (ind1 == ind2) {
            indices[vals] = p2;
            values[vals] = instance.valueSparse(p1);
            vals++;
            p1++;
            p2++;
          } else if (ind1 > ind2) {
            p2++;
          } else {
            p1++;
          }
        }
        newInstance = new SparseInstance(instance.weight(), values, indices, m_Indices.length);
      } else {
        double[] values = new double[m_Indices.length];
        for (int i = 0; i < m_Indices.length; i++) {
          values[i] = instance.value(m_Indices[i]);
        }
        newInstance = new DenseInstance(instance.weight(), values);
      }
      copyValues(newInstance, false, instance.dataset(), result);
      result.add(newInstance);
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
   * Runs the filter with the given parameters. Use -h to list options.
   * 
   * @param args the commandline options
   */
  public static void main(String[] args) {
    runFilter(new RandomSubset(), args);
  }
}

