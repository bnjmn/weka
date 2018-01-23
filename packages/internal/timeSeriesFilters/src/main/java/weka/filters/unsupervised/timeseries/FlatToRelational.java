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
import java.util.Enumeration;
import java.util.List;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionMetadata;
import weka.core.Range;
import weka.core.SparseInstance;
import weka.core.WekaException;
import weka.filters.SimpleBatchFilter;

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
public class FlatToRelational extends SimpleBatchFilter {

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

  @Override
  protected Instances determineOutputFormat(Instances inputFormat) throws Exception {
    if (numVariables == 0) {
      return new Instances(inputFormat, 0);
    } else if (numVariables < 0) {
      throw new WekaException(
          String.format(
              "Invalid input: number of variables was %s. Must be positive.", numVariables));
    }
    range.setUpper(inputFormat.numAttributes() - 1);
    ArrayList<Attribute> attsOut = new ArrayList<>();
    attsCollectForBagIdxs = range.getSelection();
    // Remove class index from selected range if necessary
    if (inputFormat.classIndex() >= 0) {
      ArrayList<Integer> al = new ArrayList<>(attsCollectForBagIdxs.length);
      for (int index : attsCollectForBagIdxs) {
        if (index != inputFormat.classIndex()) {
          al.add(index);
        }
      }
      attsCollectForBagIdxs = al.stream().mapToInt(i->i).toArray();
    }
    if (keepOtherAttributes){
      range.setInvert(!range.getInvert());
      attsKeepIdxs = range.getSelection();
      if (inputFormat.classIndex() >= 0) { // If there is a class, make sure it's included in the attributes to keep
        ArrayList<Integer> al = new ArrayList<>(attsKeepIdxs.length + 1);
        boolean classFound = false;
        for (int index : attsKeepIdxs) {
          if (index == inputFormat.classIndex()) {
            classFound = true;
          }
          if ((index > inputFormat.classIndex()) && !classFound) { // Indices returned by Range are sorted
            al.add(inputFormat.classIndex());
            classFound = true;
          }
          al.add(index);
        }
        if (!classFound) {
          al.add(inputFormat.classIndex());
        }
        attsKeepIdxs = al.stream().mapToInt(i->i).toArray();
      }
    } else if (inputFormat.classIndex() >= 0) {
      // Keep only class
      attsKeepIdxs = new int[]{inputFormat.classIndex()};
    }

    // Create list of attributes to keep and store new class index
    int outClassIndex = -1;
    for (int i = 0; i < attsKeepIdxs.length; i++) {
      if (inputFormat.classIndex() == attsKeepIdxs[i]) {
        outClassIndex = i + 1; // Add one because relation-valued attribute will be added at the start
      }
      attsOut.add((Attribute) inputFormat.attribute(attsKeepIdxs[i]).copy());
    }

    // Collect attributes which will be bagged
    ArrayList<Attribute> attsCollectForBag = new ArrayList<>();
    for (int idx : attsCollectForBagIdxs) {
      attsCollectForBag.add((Attribute) inputFormat.attribute(idx).copy());
    }

    // Check if number of input attributes is a multiple of given number of variables
    if (attsCollectForBag.size() % numVariables != 0) {
      throw new WekaException(
          String.format(
              "The total number of attributes (%d) is not a multiple of the given number of "
                  + "variables (%d)",
              attsCollectForBag.size(), numVariables));
    }

    /*
     Check if all attributes are of the same after each #numVariables attributes
    */
    List<Attribute> types = new ArrayList<>();
    // Get the first #numVariables attribute since they are going to be repeated
    for (int i = 0; i < numVariables; i++) {
      types.add(attsCollectForBag.get(i).copy("x" + (i + 1)));
    }

    // Check for each attribute
    for (int i = 0; i < attsCollectForBag.size(); i++) {
      Attribute actual = attsCollectForBag.get(i);
      Attribute expected = types.get(i % types.size());

      if (actual.type() != expected.type()) {
        throw new WekaException(
            String.format(
                "Attribute at position %d was of type <%s>, " + "expected type <%s>",
                i + 1, Attribute.typeToString(actual), Attribute.typeToString(expected)));
      }

      if (actual.type() == Attribute.NOMINAL) {
        if (actual.numValues() != expected.numValues()) {
          throw new WekaException(
              String.format("Attributes do not match after each %d attributes", numVariables));
        }

        for (int k = 0; k < actual.numValues(); k++) {
          if (!actual.value(k).equals(expected.value(k))) {
            throw new WekaException(
                String.format("Attributes do not match after each %d attributes", numVariables));
          }
        }
      }
    }

    ArrayList<Attribute> bagAtts = new ArrayList<>();
    for (int i = 0; i < numVariables; i++) {
      Attribute att = (Attribute) types.get(i).copy();
      bagAtts.add(att);
    }
    Instances bagInsts = new Instances("bagInsts", bagAtts, 0);
    Attribute relAtt = new Attribute("bag", bagInsts);
    attsOut.add(0, relAtt); // Relation-valued attribute becomes the first attribute

    final Instances outputFormat = new Instances("filtered", attsOut, 0);
    outputFormat.setClassIndex(outClassIndex);
    return outputFormat;
  }

  @Override
  protected Instances process(Instances instances) throws Exception {
    if (numVariables == 0) {
      return instances;
    }
    Instances outData = getOutputFormat();
    for (Instance instOld : instances) {

      double[] instVals = new double[attsKeepIdxs.length + 1];
      // Collect attribute values to keep
      for (int i = 0; i < attsKeepIdxs.length; i++) {
        instVals[i + 1] = instOld.value(attsKeepIdxs[i]); // Bag attribute is the first attribute
      }

      // Create bag data based on relational instances header
      Instances bagData = new Instances(outData.attribute(0).relation());
      double[] data = new double[numVariables];
      int idx = 0;
      for (int i = 0; i < attsCollectForBagIdxs.length; i++) {
        data[idx++] = instOld.value(attsCollectForBagIdxs[i]);
        if (idx == numVariables) {
          bagData.add(new DenseInstance(1.0, data));
          data = new double[numVariables];
          idx = 0;
        }
      }
      // Add relation to the attribute
      instVals[0] = outData.attribute(0).addRelation(bagData);

      Instance inst;
      if (instOld instanceof DenseInstance) {
        inst = new DenseInstance(instOld.weight(), instVals);
      } else if (instOld instanceof SparseInstance) {
        inst = new SparseInstance(instOld.weight(), instVals);
      } else {
        throw new WekaException("Input instance is neither sparse nor dense!");
      }
      outData.add(inst);
    }
    return outData;
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
    commandLineParamName = "numVariables",
    commandLineParamSynopsis = "-numVariables <int>",
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
    commandLineParamName = "range",
    commandLineParamSynopsis = "-range <string>",
    displayOrder = 1
  )
  public String getRange() {
    return range.getRanges();
  }

  public void setRange(String range) {
    this.range = new Range(range);
  }

  @OptionMetadata(
    displayName = "keep other not selected attributes",
    description =
        "Whether to keep (non-class) attributes that are not selected in range after filtering (default = true)",
    commandLineParamName = "keepOtherAttributes",
    commandLineParamSynopsis = "-keepOtherAttributes <boolean>",
    displayOrder = 2
  )
  public boolean getKeepOtherAttributes() {
    return keepOtherAttributes;
  }

  public void setKeepOtherAttributes(boolean keepOtherAttributes) {
    this.keepOtherAttributes = keepOtherAttributes;
  }
}
