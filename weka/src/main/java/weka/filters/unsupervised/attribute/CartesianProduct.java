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
 * CartesianProduct.java
 * Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.attribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.*;
import weka.core.Capabilities.Capability;
import weka.filters.SimpleBatchFilter;

/**
 * <!-- globalinfo-start -->
 * A filter for performing the Cartesian product of a set of nominal attributes. The weight of the new Cartesian
 * product attribute is the sum of the weights of the combined attributes.
 * <br><br>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start -->
 * Valid options are: <p>
 * 
 * <pre> -R &lt;col1,col2-col4,...&gt;
 *  Specifies list of nominal attributes to use to form the product.
 *  (default none)</pre>
 * 
 * <pre> -output-debug-info
 *  If set, filter is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -do-not-check-capabilities
 *  If set, filter capabilities are not checked before filter is built
 *  (use with caution).</pre>
 * 
 * <!-- options-end -->
 *
 * @author Eibe Frank
  * @version $Revision: 12037 $
 */
public class CartesianProduct extends SimpleBatchFilter {

  /** for serialization */
  private static final long serialVersionUID = -227979753639722020L;

  /** the attribute range to work on */
  protected Range m_Attributes = new Range("");

  /**
   * Returns a string describing this filter
   * 
   * @return a description of the filter suitable for displaying in the
   *         explorer/experimenter gui
   */
  @Override
  public String globalInfo() {
    return "A filter for performing the Cartesian product of a set of nominal attributes. " +
            "The weight of the new Cartesian product attribute is the sum of the weights of the combined attributes.";
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
      "\tSpecifies list of nominal attributes to use to form the product.\n" + "\t(default none)", "R",
      1, "-R <col1,col2-col4,...>"));

    result.addAll(Collections.list(super.listOptions()));

    return result.elements();
  }

  /**
   * Parses a list of options for this object.
   * <p/>
   * 
   * <!-- options-start -->
   * Valid options are: <p>
   * 
   * <pre> -R &lt;col1,col2-col4,...&gt;
   *  Specifies list of nominal attributes to use to form the product.
   *  (default none)</pre>
   * 
   * <pre> -output-debug-info
   *  If set, filter is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   * <pre> -do-not-check-capabilities
   *  If set, filter capabilities are not checked before filter is built
   *  (use with caution).</pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    String tmpStr = Utils.getOption("R", options);
    if (tmpStr.length() != 0) {
      setAttributeIndices(tmpStr);
    } else {
      setAttributeIndices("");
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

    Collections.addAll(result, super.getOptions());

    return result.toArray(new String[result.size()]);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String attributeIndicesTipText() {
    return "Specify range of attributes to act on; "
      + " this is a comma separated list of attribute indices, with"
      + " \"first\" and \"last\" valid values; specify an inclusive"
      + " range with \"-\", eg: \"first-3,5,6-10,last\".";
  }

  /**
   * Gets the current range selection
   * 
   * @return a string containing a comma separated list of ranges
   */
  public String getAttributeIndices() {
    return m_Attributes.getRanges();
  }

  /**
   * Sets which attributes are to be used for interquartile calculations and
   * outlier/extreme value detection (only numeric attributes among the
   * selection will be used).
   * 
   * @param value a string representing the list of attributes. Since the string
   *          will typically come from a user, attributes are indexed from 1. <br>
   *          eg: first-3,5,6-last
   * @throws IllegalArgumentException if an invalid range list is supplied
   */
  public void setAttributeIndices(String value) {
    m_Attributes.setRanges(value);
  }

  /**
   * Sets which attributes are to be used.
   * 
   * @param value an array containing indexes of attributes to work on. Since
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
    result.enable(Capability.MISSING_VALUES);
    result.enableAllAttributes();

    // class
    result.enableAllClasses();
    result.enable(Capability.MISSING_CLASS_VALUES);
    result.enable(Capability.NO_CLASS);

    return result;
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
   * @see #hasImmediateOutputFormat()
   * @see #batchFinished()
   */
  @Override
  protected Instances determineOutputFormat(Instances inputFormat) throws Exception {

    // attributes must be numeric
    m_Attributes.setUpper(inputFormat.numAttributes() - 1);

    ArrayList<Attribute> atts = new ArrayList<Attribute>(inputFormat.numAttributes() + 1);
    ArrayList<String> values = new ArrayList<String>();

    String name = "";
    double sumOfWeights = 0;
    for (int i = 0; i < inputFormat.numAttributes(); i++) {
      atts.add(inputFormat.attribute(i)); // Can just copy reference because index remains unchanged.
      if (inputFormat.attribute(i).isNominal() && m_Attributes.isInRange(i) && i != inputFormat.classIndex()) {
        sumOfWeights += inputFormat.attribute(i).weight();
        if (values.size() == 0) {
          values = new ArrayList<String>(inputFormat.attribute(i).numValues());
          for (int j = 0; j < inputFormat.attribute(i).numValues(); j++) {
            values.add(inputFormat.attribute(i).value(j));
          }
          name = inputFormat.attribute(i).name();
        } else {
          ArrayList<String> newValues = new ArrayList<String>(values.size() * inputFormat.attribute(i).numValues());
          for (String value : values) {
            for (int j = 0; j < inputFormat.attribute(i).numValues(); j++) {
              newValues.add(value + "_x_" + inputFormat.attribute(i).value(j));
            }
          }
          name += "_x_" + inputFormat.attribute(i).name();
          values = newValues;
        }
      }
    }
    if (values.size() > 0) {
      Attribute a = new Attribute(name, values);
      a.setWeight(sumOfWeights);
      atts.add(a);
    }

    // generate header
    Instances result = new Instances(inputFormat.relationName(), atts, 0);
    result.setClassIndex(inputFormat.classIndex());

    return result;
  }

  /**
   * Processes the given data (may change the provided dataset) and returns the
   * modified version. This method is called in batchFinished(). This
   * implementation only calls process(Instance) for each instance in the given
   * dataset.
   * 
   * @param instances the data to process
   * @return the modified data
   * @throws Exception in case the processing goes wrong
   * @see #batchFinished()
   */
  @Override
  protected Instances process(Instances instances) throws Exception {

    Instances result = getOutputFormat();

    for (Instance inst : instances) {
      if (instances.numAttributes() < result.numAttributes()) { // Do we actually need to add an attribute?
        double[] newVals = new double[result.numAttributes()];
        for (int i = 0; i < inst.numValues(); i++) {
          newVals[inst.index(i)] = inst.valueSparse(i);
        }
        String value = "";
        for (int i = 0; i < inst.numAttributes(); i++) {
          if (instances.attribute(i).isNominal() && m_Attributes.isInRange(i) && i != instances.classIndex()) {
            if (Utils.isMissingValue(newVals[i])) {
              value = null;
              break;
            } else {
              value += (value.length() > 0) ? "_x_" + instances.attribute(i).value((int) newVals[i]) :
                      instances.attribute(i).value((int) newVals[i]);
            }
          }
        }
        if (value == null) {
          newVals[newVals.length - 1] = Double.NaN;
        } else {
          newVals[newVals.length - 1] = result.attribute(result.numAttributes() - 1).indexOfValue(value);;
        }
        Instance newInst = inst.copy(newVals);
        copyValues(newInst, false, inst.dataset(), result);
        result.add(newInst);
      } else {
        copyValues(inst, false, inst.dataset(), result);
        result.add(inst);
      }
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
    return RevisionUtils.extract("$Revision: 12037 $");
  }

  /**
   * Main method for testing this class.
   * 
   * @param args should contain arguments to the filter: use -h for help
   */
  public static void main(String[] args) {
    runFilter(new CartesianProduct(), args);
  }
}

