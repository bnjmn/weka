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
 * FlatToRelational.java
 * Copyright (C) 2018 University of Waikato, Hamilton, New Zealand
 */
package weka.filters.unsupervised.timeseries;

import java.util.ArrayList;
import java.util.Arrays;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionMetadata;
import weka.core.Range;
import weka.core.WekaException;
import weka.filters.SimpleStreamFilter;

/**
 * A filter that converts a flat representation of multivariate time series to a relational representation.
 * This relational representation can then be used for time series classification with other filters
 * and classifiers in WEKA. The flat representation is popular in applications in which each time series is
 * of the same length. However, time series filters and classifiers in WEKA use relational attributes
 * to store time series instead because they support time series of varying length.
 *
 * The data in each time series is assumed to be given as the values of the selected attributes (the class, if set, is ignored).
 * By default, each time series is expected to be univariate (the number of variables is set to 1).
 * In general, assuming each time series is multivariate with M attributes, the first multivariate
 * observation in the time series is assumed to be given by the first M selected attributes, the
 * second observation is assumed to be given by attributes M + 1 to 2M, and so on. Hence, the number
 * of selected attributes (minus the class) must be a multiple of M.
 *
 * @author Steven Lang
 */
public class FlatToRelational extends SimpleStreamFilter {

  private static final long serialVersionUID = 271653370775136230L;
  /** Number of variables in the timeseries */
  protected int numVariables = 1;
  /** Attribute selection range */
  protected Range range = new Range("first-last");
  /** Attribute indices to collect for the bag */
  protected int[] attsCollectForBagIdxs;
  /** Attribute indices to keep */
  protected int[] attsKeepIdxs;
  /** Keep non-selected attributes */
  protected boolean keepOtherAttributes = true;

  @Override
  public String globalInfo() {
    return "A filter that converts a flat representation of multivariate time series to a relational representation. " +
            "This relational representation can then be used for time series classification with other filters " +
            "and classifiers in WEKA. The flat representation is popular in applications in which each time series is" +
            "of the same length. However, time series filters and classifiers in WEKA use relational attributes " +
            "to store time series instead because they support time series of varying length.\n\n The data in each time " +
            "series is assumed to be given as the values of the selected attributes (the class, if set, is ignored). " +
            "By default, each time series is expected to be univariate (the number of variables is set to 1). " +
            "In general, assuming each time series is multivariate with M attributes, the first multivariate " +
            "observation in the time series is assumed to be given by the first M selected attributes, the " +
            "second observation is assumed to be given by attributes M + 1 to 2M, and so on. Hence, the number " +
            "of selected attributes (minus the class) must be a multiple of M.";
  }

  /**
   * This method needs to be called before the filter is used to transform instances. Will return true
   * for this filter because the output format can be collected immediately.
   *
   * This specialised version of setInputFormat() calls the setInputFormat() method in SimpleFilter and then sets the
   * output locators for relational and string attributes correctly. This is necessary because the setInputFormat()
   * method in SimpleFilter calls setOutputFormat(), which initialises the output locators incorrectly in the case of
   * this filter because the filter adds a relational attribute to the output.
   *
   * We also set the input locators correctly here, making sure that only attributes that are kept but
   * not placed into the relation-valued attribute are covered by the locators.
   */
  @Override
  public boolean setInputFormat(Instances instanceInfo) throws Exception {
    super.setInputFormat(instanceInfo);

    // The new relational attribute must be skipped when relational and string attribute values are copied
    // over from the input format.
    int[] indicesForOutputLocators = new int[outputFormatPeek().numAttributes() - 1];
    for (int i = 1; i < outputFormatPeek().numAttributes();i++) {
      indicesForOutputLocators[i - 1] = i;
    }
    initOutputLocators(outputFormatPeek(), indicesForOutputLocators);

    // Make sure only string and relational attributes that are not in the selected range for the time 
    // series are covered by the input locators.
    initInputLocators(inputFormatPeek(), attsKeepIdxs);

    return hasImmediateOutputFormat();
  }

  /**
   * Determines the output format of the data generated by this filter.
   *
   * @param inputFormat the input format to base the output format on
   * @return
   * @throws Exception
   */
  @Override
  protected Instances determineOutputFormat(Instances inputFormat) throws Exception {
    if (numVariables == 0) {
      return new Instances(inputFormat, 0);
    } else if (numVariables < 0) {
      throw new WekaException(
          String.format("Invalid input: number of variables was %s. Must be positive.", numVariables));
    }
    range.setUpper(inputFormat.numAttributes() - 1);
    attsCollectForBagIdxs = range.getSelection();
    // Remove class index from selected range if necessary
    if (inputFormat.classIndex() >= 0) {
      attsCollectForBagIdxs = Arrays.stream(attsCollectForBagIdxs).filter(i -> i != inputFormat.classIndex()).toArray();
    }
    if (attsCollectForBagIdxs.length == 0) {
      throw new WekaException("No time series attributes have been selected!");
    }

    // Check if number of input attributes is a multiple of given number of variables
    if (attsCollectForBagIdxs.length % numVariables != 0) {
      throw new WekaException(
          String.format("The total number of attributes (%d) is not a multiple of the given number of variables (%d)",
              attsCollectForBagIdxs.length, numVariables));
    }

    // Get the first #numVariables attribute since they are going to be repeated
    ArrayList<Attribute> types = new ArrayList<>();
    for (int i = 0; i < numVariables; i++) {
      types.add((Attribute)inputFormat.attribute(attsCollectForBagIdxs[i]).copy());
    }

    // Check if all attributes are of the same after each #numVariables attributes
    for (int i = 0; i < attsCollectForBagIdxs.length; i++) {
      Attribute actual = inputFormat.attribute(attsCollectForBagIdxs[i]);
      Attribute expected = types.get(i % types.size());

      if (actual.type() != expected.type()) {
        throw new WekaException(String.format("Attribute at position %d was of type <%s>, " + "expected type <%s>",
                i + 1, Attribute.typeToString(actual), Attribute.typeToString(expected)));
      }

      if (actual.type() == Attribute.NOMINAL) {
        if (actual.numValues() != expected.numValues()) {
          throw new WekaException(String.format("Attributes do not match after each %d attributes", numVariables));
        }

        for (int k = 0; k < actual.numValues(); k++) {
          if (!actual.value(k).equals(expected.value(k))) {
            throw new WekaException(String.format("Attributes do not match after each %d attributes", numVariables));
          }
        }
      }
    }

    if (keepOtherAttributes){
      boolean[] s = new boolean[inputFormat.numAttributes()];
      for (int i = 0; i < attsCollectForBagIdxs.length; i++) {
        s[attsCollectForBagIdxs[i]] = true;
      }
      attsKeepIdxs = new int[inputFormat.numAttributes() - attsCollectForBagIdxs.length];
      int index = 0;
      for (int i = 0; i < s.length; i++) {
        if (!s[i]) {
          attsKeepIdxs[index++] = i;
        }
      }
    } else if (inputFormat.classIndex() >= 0) {
      // Keep only class
      attsKeepIdxs = new int[]{inputFormat.classIndex()};
    }

    ArrayList<Attribute> attsOut = new ArrayList<>();
 
    // Relation-valued attribute becomes the first attribute
    attsOut.add(new Attribute("series", new Instances("series", types, 0).stringFreeStructure()));

    // Create list of attributes to keep and store new class index
    int outClassIndex = -1;
    for (int i = 0; i < attsKeepIdxs.length; i++) {
      if (inputFormat.classIndex() == attsKeepIdxs[i]) {
        outClassIndex = i + 1; // Add one because relation-valued attribute will be added at the start
      }
      attsOut.add((Attribute) inputFormat.attribute(attsKeepIdxs[i]).copy());
    }

    final Instances outputFormat = new Instances(inputFormat.relationName(), attsOut, 0);
    outputFormat.setClassIndex(outClassIndex);
    return outputFormat;
  }

  /**
   * Processes a single instance.
   *
   * @param instance the instance to process
   * @return
   * @throws Exception
   */
  @Override
  protected Instance process(Instance instance) throws Exception {
    if (numVariables == 0) {
      return instance;
    }

    Instances outData = outputFormatPeek();
    double[] instVals = new double[attsKeepIdxs.length + 1];
    // Collect attribute values to keep
    for (int i = 0; i < attsKeepIdxs.length; i++) {
      instVals[i + 1] = instance.value(attsKeepIdxs[i]); // Bag attribute is the first attribute
    }

    // Create bag data based on relational instances header
    Instances bagData = outData.attribute(0).relation().stringFreeStructure();
    double[] data = new double[numVariables];
    int idx = 0;
    for (int attIndex : attsCollectForBagIdxs) {
      double val = instance.value(attIndex);
      if (instance.attribute(attIndex).isString()) {
        data[idx] = bagData.attribute(idx).addStringValue(instance.attribute(attIndex), (int) val);
      } else if (instance.attribute(attIndex).isRelationValued()) {
        data[idx] = bagData.attribute(idx).addRelation(instance.relationalValue(attIndex));
      } else {
        data[idx] = val;
      }
      idx++;
      if (idx == numVariables) {
        bagData.add(new DenseInstance(1.0, data));
        data = new double[numVariables];
        idx = 0;
      }
    }
    // Add relation to the attribute
    instVals[0] = outData.attribute(0).addRelation(bagData);

    // Create new instance and make sure string and relational values are copied from input to output
    Instance inst = instance.copy(instVals); // Note that this will copy the reference to the input dataset!
    inst.setDataset(null); // Make sure we do not class the copyValues() method in push()
    copyValues(inst, false, instance.dataset(), outData); // Copy string and relational values

    return inst;
  }

  /** Returns the Capabilities of this filter. */
  @Override
  public Capabilities getCapabilities() {

    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enableAllAttributes();

    // attributes
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.NOMINAL_ATTRIBUTES);

    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.NUMERIC_CLASS);
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.NO_CLASS);

    result.enable(Capability.MISSING_CLASS_VALUES);

    return result;
  }

  @OptionMetadata(
    displayName = "number of variables",
    description = "The number of variables in the timeseries (default = 1, univariate time series)",
    commandLineParamName = "N",
    commandLineParamSynopsis = "-N <int>",
    displayOrder = 0
  )
  public int getNumVariables() {
    return numVariables;
  }

  public void setNumVariables(int numVariables) {
    this.numVariables = numVariables;
  }

  @OptionMetadata(
    displayName = "attribute range",
    description =
        "The attributes to transform into a multivariate timeseries, skipping the class if set (default = \"first-last\")",
    commandLineParamName = "R",
    commandLineParamSynopsis = "-R <string>",
    displayOrder = 1
  )
  public String getRange() {
    return range.getRanges();
  }

  public void setRange(String range) {
    this.range = new Range(range);
  }

  @OptionMetadata(
    displayName = "do not keep other selected attributes",
    description =
        "Whether not to keep (non-class) attributes that are outside the range.",
    commandLineParamName = "K",
    commandLineParamSynopsis = "-K",
    commandLineParamIsFlag = true,
    displayOrder = 2
  )
  public boolean getDoNotKeepOtherAttributes() {
    return !keepOtherAttributes;
  }

  public void setDoNotKeepOtherAttributes(boolean keepOtherAttributes) {
    this.keepOtherAttributes = !keepOtherAttributes;
  }
}
