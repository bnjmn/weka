/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 * PropositionalToMultiInstance.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.unsupervised.attribute;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.UnsupervisedFilter;

import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

/** 
 <!-- globalinfo-start -->
 * Converts the propositional instance dataset into multi-instance dataset (with relational attribute). When normalize or standardize a multi-instance dataset, a MIToSingleInstance filter can be applied first to convert the multi-instance dataset into propositional instance dataset. After normalization or standardization, may use this PropositionalToMultiInstance filter to convert the data back to multi-instance format.<br/>
 * <br/>
 * Note: the first attribute of the original propositional instance dataset must be a nominal attribute which is expected to be bagId attribute.
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -S &lt;num&gt;
 *  The seed for the randomization of the order of bags. (default 1)</pre>
 * 
 * <pre> -R
 *  Randomizes the order of the produced bags after the generation. (default off)</pre>
 * 
 <!-- options-end -->
 *
 * @author Lin Dong (ld21@cs.waikato.ac.nz) 
 * @version $Revision: 1.2 $
 * @see MultiInstanceToPropositional
 */
public class PropositionalToMultiInstance 
  extends Filter
  implements OptionHandler, UnsupervisedFilter {

  /** for serialization */
  private static final long serialVersionUID = 5825873573912102482L;

  /** String attribute indices in the input data */
  protected int[] m_Indices = null;

  /** String attribute indices in the output data (in the relational attribute) */
  protected int[] m_IndicesNew = null;

  /** the seed for randomizing, default is 1 */
  protected int m_Seed = 1;
  
  /** whether to randomize the output data */
  protected boolean m_Randomize = false;
  
  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return  
        "Converts the propositional instance dataset into multi-instance "
      + "dataset (with relational attribute). When normalize or standardize a "
      + "multi-instance dataset, a MIToSingleInstance filter can be applied "
      + "first to convert the multi-instance dataset into propositional "
      + "instance dataset. After normalization or standardization, may use "
      + "this PropositionalToMultiInstance filter to convert the data back to "
      + "multi-instance format.\n\n"
      + "Note: the first attribute of the original propositional instance "
      + "dataset must be a nominal attribute which is expected to be bagId "
      + "attribute.";

  }

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {
    Vector result = new Vector();
  
    result.addElement(new Option(
        "\tThe seed for the randomization of the order of bags."
        + "\t(default 1)",
        "S", 1, "-S <num>"));
  
    result.addElement(new Option(
        "\tRandomizes the order of the produced bags after the generation."
        + "\t(default off)",
        "R", 0, "-R"));
  
    return result.elements();
  }


  /**
   * Parses a given list of options. <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -S &lt;num&gt;
   *  The seed for the randomization of the order of bags. (default 1)</pre>
   * 
   * <pre> -R
   *  Randomizes the order of the produced bags after the generation. (default off)</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    String        tmpStr;
    
    setRandomize(Utils.getFlag('R', options));
    
    tmpStr = Utils.getOption('S', options);
    if (tmpStr.length() != 0)
      setSeed(Integer.parseInt(tmpStr));
    else
      setSeed(1);
  }

  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {
    Vector        result;
    
    result = new Vector();
    
    result.add("-S");
    result.add("" + getSeed());
    
    if (m_Randomize)
      result.add("-R");

    return (String[]) result.toArray(new String[result.size()]);
  }

  /**
   * Sets the new seed for randomizing the order of the generated data
   * 
   * @param value     the new seed value
   */
  public void setSeed(int value) {
    m_Seed = value;
  }
  
  /**
   * Returns the current seed value for randomizing the order of the generated
   * data
   * 
   * @return          the current seed value
   */
  public int getSeed() {
    return m_Seed;
  }
  
  /**
   * Sets whether the order of the generated data is randomized
   * 
   * @param value     whether to randomize or not
   */
  public void setRandomize(boolean value) {
    m_Randomize = value;
  }
  
  /**
   * Gets whether the order of the generated is randomized
   * 
   * @return      true if the order is randomized
   */
  public boolean getRandomize() {
    return m_Randomize;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String randomizeTipText() {
    return "Whether the order of the generated data is randomized.";
  }
  
  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input 
   * instance structure (any instances contained in the object are 
   * ignored - only the structure is required).
   * @return true if the outputFormat may be collected immediately
   * @throws Exception if the input format can't be set 
   * successfully
   */
  public boolean setInputFormat(Instances instanceInfo) 
    throws Exception {

    if (instanceInfo.attribute(1).isRelationValued()) {
      throw new Exception("Can only convert propositional instance dataset!");
    }
    if (instanceInfo.attribute(0).type()!= Attribute.NOMINAL) {
      throw new Exception("The first attribute type of the original propositional instance dataset must be Nominal!");
    }
    super.setInputFormat(instanceInfo);

    /* create a new output format (multi-instance format) */
    Instances newData = instanceInfo.stringFreeStructure();
    Attribute attBagIndex = (Attribute) newData.attribute(0).copy();
    Attribute attClass = (Attribute) newData.classAttribute().copy();
    // remove the bagIndex attribute
    newData.deleteAttributeAt(0);
    // remove the class attribute
    newData.setClassIndex(-1);
    newData.deleteAttributeAt(newData.numAttributes() - 1);

    FastVector attInfo = new FastVector(3); 
    attInfo.addElement(attBagIndex);
    attInfo.addElement(new Attribute("bag", newData)); // relation-valued attribute
    attInfo.addElement(attClass);
    Instances data = new Instances("Multi-Instance-Dataset", attInfo, 0); 
    data.setClassIndex(data.numAttributes() - 1);

    super.setOutputFormat(data.stringFreeStructure());

    return true;
  }

  /**
   * adds a new bag out of the given data and adds it to the output
   * 
   * @param input       the intput dataset
   * @param output      the dataset this bag is added to
   * @param bagInsts    the instances in this bag
   * @param bagIndex    the bagIndex of this bag
   * @param classValue  the associated class value
   * @param bagWeight   the weight of the bag
   */
  protected void addBag(
      Instances input,
      Instances output,
      Instances bagInsts, 
      int bagIndex, 
      double classValue, 
      double bagWeight) {
    
    // copy strings
    for (int i = 0; i < bagInsts.numInstances(); i++) {
      copyStringValues(bagInsts.instance(i), false, input, m_Indices, bagInsts, m_IndicesNew);
    }
    
    int value = output.attribute(1).addRelation(bagInsts);
    Instance newBag = new Instance(output.numAttributes());        
    newBag.setValue(0, bagIndex);
    newBag.setValue(2, classValue);
    newBag.setValue(1, value);
    newBag.setWeight(bagWeight);
    newBag.setDataset(output);
    output.add(newBag);
  }

  /**
   * Adds an output instance to the queue. The derived class should use this
   * method for each output instance it makes available. 
   *
   * @param instance the instance to be added to the queue.
   */
  protected void push(Instance instance) {
    if (instance != null) {
      super.push(instance);
      // set correct references
    }
  }
  
  /**
   * Signify that this batch of input to the filter is finished. 
   * If the filter requires all instances prior to filtering,
   * output() may now be called to retrieve the filtered instances.
   *
   * @return true if there are instances pending output
   * @throws IllegalStateException if no input structure has been defined
   */
  public boolean batchFinished() {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }

    Instances input = getInputFormat();
    input.sort(0);   // make sure that bagID is sorted
    Instances output = getOutputFormat();
    Instances bagInsts = output.attribute(1).relation();
    Instance inst = new Instance(bagInsts.numAttributes());
    inst.setDataset(bagInsts);

    // determine String att. indices
    int count = 0;
    for (int i = 0; i < input.numAttributes(); i++) {
      if (input.attribute(i).isString())
        count++;
    }
    m_Indices = new int[count];
    m_IndicesNew = new int[count];
    count = 0;
    for (int i = 0; i < input.numAttributes(); i++) {
      if (input.attribute(i).isString()) {
        m_Indices[count] = i;
        m_IndicesNew[count] = i - 1;
        count++;
      }
    }
    
    double bagIndex   = input.instance(0).value(0);
    double classValue = input.instance(0).classValue(); 
    double bagWeight  = 0.0;

    // Convert pending input instances
    for(int i = 0; i < input.numInstances(); i++) {
      double currentBagIndex = input.instance(i).value(0);

      // copy the propositional instance value, except the bagIndex and the class value
      for (int j = 0; j < input.numAttributes() - 2; j++) 
        inst.setValue(j, input.instance(i).value(j + 1));
      inst.setWeight(input.instance(i).weight());

      if (currentBagIndex == bagIndex){
        bagInsts.add(inst);
        bagWeight += inst.weight();
      }
      else{
        addBag(input, output, bagInsts, (int) bagIndex, classValue, bagWeight);

        bagInsts   = bagInsts.stringFreeStructure();  
        bagInsts.add(inst);
        bagIndex   = currentBagIndex;
        classValue = input.instance(i).classValue();
        bagWeight  = inst.weight();
      }
    }

    // reach the last instance, create and add the last bag
    addBag(input, output, bagInsts, (int) bagIndex, classValue, bagWeight);

    if (getRandomize())
      output.randomize(new Random(getSeed()));
    
    for (int i = 0; i < output.numInstances(); i++)
      push(output.instance(i));
    
    // Free memory
    flushInput();

    m_NewBatch = true;
    return (numPendingOutput() != 0);
  }

  /**
   * Main method for running this filter.
   *
   * @param args should contain arguments to the filter: 
   * use -h for help
   */
  public static void main(String[] args) {
    try {
      if (Utils.getFlag('b', args)) {
        Filter.batchFilterFile(new PropositionalToMultiInstance(), args);
      } 
      else {
        Filter.filterFile(new PropositionalToMultiInstance(), args);
      }
    } 
    catch (Exception ex) {
      ex.printStackTrace();
      System.out.println(ex.getMessage());
    }
  }
}
