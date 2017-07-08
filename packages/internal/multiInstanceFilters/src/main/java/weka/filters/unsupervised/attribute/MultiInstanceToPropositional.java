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
 * MultiInstanceToPropositional.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.unsupervised.attribute;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.MultiInstanceCapabilitiesHandler;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RelationalLocator;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.StringLocator;
import weka.core.Tag;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.UnsupervisedFilter;

/**
 * <!-- globalinfo-start --> Converts the multi-instance dataset into single
 * instance dataset so that the Nominalize, Standardize and other type of
 * filters or transformation can be applied to these data for the further
 * preprocessing.<br>
 * Note: the first attribute of the converted dataset is a nominal attribute and
 * refers to the bagId.
 * <p>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start --> Valid options are:
 * </p>
 * 
 * <pre>
 * -A &lt;num&gt;
 *  The type of weight setting for each prop. instance:
 *  0.weight = original single bag weight /Total number of
 *  prop. instance in the corresponding bag;
 *  1.weight = 1.0;
 *  2.weight = 1.0/Total number of prop. instance in the 
 *   corresponding bag; 
 *  3. weight = Total number of prop. instance / (Total number 
 *   of bags * Total number of prop. instance in the 
 *   corresponding bag). 
 *  (default:0)
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Lin Dong (ld21@cs.waikato.ac.nz)
 * @version $Revision$
 * @see PropositionalToMultiInstance
 */
public class MultiInstanceToPropositional extends Filter implements
  OptionHandler, UnsupervisedFilter, MultiInstanceCapabilitiesHandler {

  /** for serialization */
  private static final long serialVersionUID = -4102847628883002530L;

  /** the total number of bags */
  protected int m_NumBags = -1;

  /** Indices of string attributes in the bag */
  protected StringLocator m_BagStringAtts = null;

  /** Indices of relational attributes in the bag */
  protected RelationalLocator m_BagRelAtts = null;

  /** the total number of the propositional instance in the dataset */
  protected int m_NumInstances;

  /** weight method: keep the weight to be the same as the original value */
  public static final int WEIGHTMETHOD_ORIGINAL = 0;
  /** weight method: 1.0 */
  public static final int WEIGHTMETHOD_1 = 1;
  /** weight method: 1.0 / Total # of prop. instance in the corresp. bag */
  public static final int WEIGHTMETHOD_INVERSE1 = 2;
  /**
   * weight method: Total # of prop. instance / (Total # of bags * Total # of
   * prop. instance in the corresp. bag)
   */
  public static final int WEIGHTMETHOD_INVERSE2 = 3;
  /** weight methods */
  public static final Tag[] TAGS_WEIGHTMETHOD = {
    new Tag(WEIGHTMETHOD_ORIGINAL,
      "keep the weight to be the same as the original value"),
    new Tag(WEIGHTMETHOD_1, "1.0"),
    new Tag(WEIGHTMETHOD_INVERSE1,
      "1.0 / Total # of prop. instance in the corresp. bag"),
    new Tag(
      WEIGHTMETHOD_INVERSE2,
      "Total # of prop. instance / (Total # of bags * Total # of prop. instance in the corresp. bag)") };

  /** the propositional instance weight setting method */
  protected int m_WeightMethod = WEIGHTMETHOD_INVERSE2;

  /**
   * Returns an enumeration describing the available options
   * 
   * @return an enumeration of all the available options
   */
  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>(1);

    result.addElement(new Option(
      "\tThe type of weight setting for each prop. instance:\n"
        + "\t0.weight = original single bag weight /Total number of\n"
        + "\tprop. instance in the corresponding bag;\n"
        + "\t1.weight = 1.0;\n"
        + "\t2.weight = 1.0/Total number of prop. instance in the \n"
        + "\t\tcorresponding bag; \n"
        + "\t3. weight = Total number of prop. instance / (Total number \n"
        + "\t\tof bags * Total number of prop. instance in the \n"
        + "\t\tcorresponding bag). \n" + "\t(default:0)", "A", 1, "-A <num>"));

    return result.elements();
  }

  /**
   * <p>Parses a given list of options.
   * </p>
   * 
   * <!-- options-start --> Valid options are:
   * 
   * <pre>
   * -A &lt;num&gt;
   *  The type of weight setting for each prop. instance:
   *  0.weight = original single bag weight /Total number of
   *  prop. instance in the corresponding bag;
   *  1.weight = 1.0;
   *  2.weight = 1.0/Total number of prop. instance in the 
   *   corresponding bag; 
   *  3. weight = Total number of prop. instance / (Total number 
   *   of bags * Total number of prop. instance in the 
   *   corresponding bag). 
   *  (default:0)
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    String weightString = Utils.getOption('A', options);
    if (weightString.length() != 0) {
      setWeightMethod(new SelectedTag(Integer.parseInt(weightString),
        TAGS_WEIGHTMETHOD));
    } else {
      setWeightMethod(new SelectedTag(WEIGHTMETHOD_INVERSE2, TAGS_WEIGHTMETHOD));
    }

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the classifier.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> result = new Vector<String>();

    result.add("-A");
    result.add("" + m_WeightMethod);

    return result.toArray(new String[result.size()]);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String weightMethodTipText() {
    return "The method used for weighting the instances.";
  }

  /**
   * The new method for weighting the instances.
   * 
   * @param method the new method
   */
  public void setWeightMethod(SelectedTag method) {
    if (method.getTags() == TAGS_WEIGHTMETHOD) {
      m_WeightMethod = method.getSelectedTag().getID();
    }
  }

  /**
   * Returns the current weighting method for instances.
   * 
   * @return the current weight method
   */
  public SelectedTag getWeightMethod() {
    return new SelectedTag(m_WeightMethod, TAGS_WEIGHTMETHOD);
  }

  /**
   * Returns a string describing this filter
   * 
   * @return a description of the filter suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {

    return "Converts the multi-instance dataset into single instance dataset "
      + "so that the Nominalize, Standardize and other type of filters or transformation "
      + " can be applied to these data for the further preprocessing.\n"
      + "Note: the first attribute of the converted dataset is a nominal "
      + "attribute and refers to the bagId.";
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
    result.disableAllAttributes();
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.RELATIONAL_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enableAllClasses();
    result.enable(Capability.MISSING_CLASS_VALUES);

    // other
    result.enable(Capability.ONLY_MULTIINSTANCE);

    return result;
  }

  /**
   * Returns the capabilities of this multi-instance filter for the relational
   * data (i.e., the bags).
   * 
   * @return the capabilities of this object
   * @see Capabilities
   */
  @Override
  public Capabilities getMultiInstanceCapabilities() {
    Capabilities result = new Capabilities(this);

    // attributes
    result.enableAllAttributes();
    result.disable(Capability.RELATIONAL_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enableAllClasses();
    result.enable(Capability.MISSING_CLASS_VALUES);
    result.enable(Capability.NO_CLASS);

    // other
    result.setMinimumNumberInstances(0);

    return result;
  }

  /**
   * Sets the format of the input instances.
   * 
   * @param instanceInfo an Instances object containing the input instance
   *          structure (any instances contained in the object are ignored -
   *          only the structure is required).
   * @return true if the outputFormat may be collected immediately
   * @throws Exception if the input format can't be set successfully
   */
  @Override
  public boolean setInputFormat(Instances instanceInfo) throws Exception {

    if (instanceInfo.attribute(1).type() != Attribute.RELATIONAL) {
      throw new Exception("Can only handle relational-valued attribute!");
    }
    super.setInputFormat(instanceInfo);

    Attribute classAttribute = (Attribute) instanceInfo.classAttribute().copy();
    Attribute bagIndex = (Attribute) instanceInfo.attribute(0).copy();

    // Indicator that batchFinished has not been called.
    m_NumBags = -1;

    /* create a new output format (propositional instance format) */
    Instances tempData = instanceInfo.attribute(1).relation()
      .stringFreeStructure();
    ArrayList<Attribute> atts = new ArrayList<Attribute>(tempData.numAttributes());
    for (int i = 0; i < tempData.numAttributes(); i++) {
      atts.add(tempData.attribute(i).copy(instanceInfo.attribute(1).name() + "_" + tempData.attribute(i).name()));
    }
    Instances newData = new Instances(tempData.relationName(), atts, 0);
    newData.insertAttributeAt(bagIndex, 0);
    newData.insertAttributeAt(classAttribute, newData.numAttributes());
    newData.setClassIndex(newData.numAttributes() - 1);

    super.setOutputFormat(newData.stringFreeStructure());

    m_BagStringAtts = new StringLocator(instanceInfo.attribute(1).relation()
      .stringFreeStructure());
    m_BagRelAtts = new RelationalLocator(instanceInfo.attribute(1).relation()
      .stringFreeStructure());

    return true;
  }

  /**
   * Input an instance for filtering. Filter requires all training instances be
   * read before producing output.
   * 
   * @param instance the input instance
   * @return true if the filtered instance may now be collected with output().
   * @throws IllegalStateException if no input format has been set.
   */
  @Override
  public boolean input(Instance instance) {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }

    // Has batchFinished() been called?
    if (m_NumBags != -1) {
      convertInstance(instance);
      return true;
    } else {
      bufferInput(instance);
      return false;
    }
  }

  /**
   * Signify that this batch of input to the filter is finished. If the filter
   * requires all instances prior to filtering, output() may now be called to
   * retrieve the filtered instances.
   * 
   * @return true if there are instances pending output
   * @throws IllegalStateException if no input structure has been defined
   */
  @Override
  public boolean batchFinished() {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }

    // Has batchFinished been called before?
    if (m_NumBags == -1) {
      Instances input = getInputFormat();
      m_NumBags = input.numInstances();
      m_NumInstances = 0;
      for (int i = 0; i < m_NumBags; i++) {
        if (input.instance(i).relationalValue(1) == null) {
          m_NumInstances++;
        } else {
          int nn = input.instance(i).relationalValue(1).numInstances();
          m_NumInstances += nn;
        }
      }

      // Convert pending input instances
      for (int i = 0; i < input.numInstances(); i++) {
        convertInstance(input.instance(i));
      }

      // Free memory
      flushInput();
    }

    m_NewBatch = true;
    return (numPendingOutput() != 0);
  }

  /**
   * Convert a single bag over. The converted instances is added to the end of
   * the output queue.
   * 
   * @param bag the bag to convert
   */
  private void convertInstance(Instance bag) {

    Instances data = bag.relationalValue(1);
    int bagSize = 1;
    if (data != null) {
      bagSize = data.numInstances();
    }
    double bagIndex = bag.value(0);
    double classValue = bag.classValue();
    double weight = 0.0;
    // the proper weight for each instance in a bag
    if (m_WeightMethod == WEIGHTMETHOD_1) {
      weight = 1.0;
    } else if (m_WeightMethod == WEIGHTMETHOD_INVERSE1) {
      weight = 1.0 / bagSize;
    } else if (m_WeightMethod == WEIGHTMETHOD_INVERSE2) {
      weight = (double) m_NumInstances / (m_NumBags * bagSize);
    } else {
      weight = bag.weight() / bagSize;
    }

    Instance newInst;
    Instances outputFormat = getOutputFormat().stringFreeStructure();

    for (int i = 0; i < bagSize; i++) {
      newInst = new DenseInstance(outputFormat.numAttributes());
      newInst.setDataset(outputFormat);
      newInst.setValue(0, bagIndex);
      if (!bag.classIsMissing()) {
        newInst.setClassValue(classValue);
      }
      // copy the attribute values to new instance
      for (int j = 1; j < outputFormat.numAttributes() - 1; j++) {
        if (data == null) {
          newInst.setMissing(j);
        } else {
          newInst.setValue(j, data.instance(i).value(j - 1));
        }
      }

      newInst.setWeight(weight);

      // copy strings/relational values
      StringLocator.copyStringValues(newInst, false, data, m_BagStringAtts,
        outputFormat, m_OutputStringAtts);

      RelationalLocator.copyRelationalValues(newInst, false, data,
        m_BagRelAtts, outputFormat, m_OutputRelAtts);

      push(newInst);
    }
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
   * Main method for running this filter.
   * 
   * @param args should contain arguments to the filter: use -h for help
   */
  public static void main(String[] args) {
    runFilter(new MultiInstanceToPropositional(), args);
  }
}
