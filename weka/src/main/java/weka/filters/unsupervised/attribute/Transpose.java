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
 * Transpose.java
 * Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.unsupervised.attribute;

import java.util.ArrayList;

import weka.core.*;
import weka.core.Capabilities.Capability;
import weka.filters.SimpleBatchFilter;
import weka.filters.UnsupervisedFilter;

/**
 <!-- globalinfo-start -->
 * Transposes the data: instances become attributes and attributes become instances. If the first attribute in the
 * original data is a nominal or string identifier attribute, this identifier attribute will be used to create attribute
 * names in the transposed data. All attributes other than the identifier attribute must be numeric. The attribute names
 * in the original data are used to create an identifier attribute of type string in the transposed data.<br/>
 * <br/>
 * This filter can only process one batch of data, e.g., it cannot be used in the the FilteredClassifier.<br/>
 * <br/>
 * This filter can only be applied when no class attribute has been set.<br/>
 * <br/>
 * Date values will be turned into simple numeric values.<br/>
 * <br/>
 * <p/>
 <!-- globalinfo-end -->
 * 
 * @author Eibe Frank
 * @version $Revision: 10215 $
 */
public class Transpose extends SimpleBatchFilter
        implements UnsupervisedFilter, WeightedAttributesHandler, WeightedInstancesHandler {

  /** for serialization */
  static final long serialVersionUID = 213999899640387499L;

  /**
   * Returns a string describing this filter.
   * 
   * @return a description of the filter suitable for displaying in the
   *         explorer/experimenter gui
   */
  @Override
  public String globalInfo() {
    return "Transposes the data: instances become attributes and attributes"
      + " become instances. If the first attribute in the original data"
      + " is a nominal or string identifier attribute, this identifier attribute"
      + " will be used to create attribute names in the transposed data. All"
      + " attributes other than the identifier attribute must be numeric. The"
      + " attribute names in the original data are used to create an identifier"
      + " attribute of type string in the transposed data.\n\n"
      + "This filter can only process one batch of data, e.g., it cannot be used"
      + " in the the FilteredClassifier.\n\n"
      + "This filter can only be applied when no class attribute has been set.\n\n"
      + "Date values will be turned into simple numeric values.\n\n";
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

    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.STRING_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    result.enable(Capability.NO_CLASS);

    return result;
  }

  /**
   * Determines the output format based on the input format and returns this. In
   * case the output format cannot be returned immediately, i.e.,
   * immediateOutputFormat() returns false, then this method will be called from
   * batchFinished().
   * 
   * @param inputFormat the input format to base the output format on
   * @return the output format
   * @throws Exception in case the determination goes wrong
   * @see #hasImmediateOutputFormat()
   * @see #batchFinished()
   */
  @Override
  protected Instances determineOutputFormat(Instances inputFormat)
    throws Exception {

    ArrayList<Attribute> newAtts = new ArrayList<Attribute>(
      inputFormat.numInstances());

    newAtts.add(new Attribute("Identifier", (ArrayList<String>) null));
    for (int i = 0; i < inputFormat.numInstances(); i++) {
      if (inputFormat.attribute(0).isNominal()
        || inputFormat.attribute(0).isString()) {

        // We have a proper identifier
        newAtts.add(new Attribute(inputFormat.instance(i).stringValue(0)));
      } else {

        // Just use a simple identifier
        newAtts.add(new Attribute("" + (i + 1)));
      }
      newAtts.get(i).setWeight(inputFormat.instance(i).weight());
    }

    return new Instances(inputFormat.relationName(), newAtts,
      inputFormat.numAttributes());
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
      throw new Exception(
        "The Transpose filter can only process one batch of instances.");
    }

    setOutputFormat(determineOutputFormat(instances));

    // Do we have an identifier in the original data?
    int offset = (instances.attribute(0).isNominal() || instances.attribute(0)
      .isString()) ? 1 : 0;

    // Transpose data
    double[][] newData = new double[instances.numAttributes() - offset][instances
      .numInstances() + 1];
    for (int i = 0; i < instances.numInstances(); i++) {
      for (int j = offset; j < instances.numAttributes(); j++) {
        newData[j - offset][0] = getOutputFormat().attribute(0).addStringValue(
          instances.attribute(j).name());
        if (!instances.attribute(j).isNumeric()) {
          throw new Exception("Only numeric attributes can be transposed: "
            + instances.attribute(j).name() + " is not numeric.");
        }
        newData[j - offset][i + 1] = instances.instance(i).value(j);
      }
    }

    // Create instances
    Instances result = getOutputFormat();
    for (int i = 0; i < newData.length; i++) {
      result.add(new DenseInstance(instances.attribute(i + offset).weight(),
        newData[i]));
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
    return RevisionUtils.extract("$Revision: 10215 $");
  }

  /**
   * runs the filter with the given arguments
   * 
   * @param args the commandline arguments
   */
  public static void main(String[] args) {
    runFilter(new Transpose(), args);
  }
}

