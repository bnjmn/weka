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
 *    NominalToBinaryFilter.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */


package weka.filters;

import java.io.*;
import java.util.*;
import weka.core.*;

/** 
 * Converts all nominal attributes into binary numeric attributes. An
 * attribute with k values is transformed into k binary attributes if
 * the class is nominal (using the one-attribute-per-value approach).
 * Binary attributes are left binary.
 *
 * If the class is numeric, k - 1 new binary attributes are generated
 * (in the manner described in "Classification and Regression
 * Trees"). Currently requires that a class attribute be set (but this
 * should be changed).<p>
 *
 * Valid filter-specific options are: <p>
 *
 * -N <br>
 * If binary attributes are to be coded as nominal ones.<p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz) 
 * @version $Revision: 1.19 $ 
 */
public class NominalToBinaryFilter extends Filter implements OptionHandler {

  /** The sorted indices of the attribute values. */
  private int[][] m_Indices = null;

  /** Are the new attributes going to be nominal or numeric ones? */
  private boolean m_Numeric = true;

  /**
   * Are we going to do the simple thing or the more complicated M5 thing
   */
  private boolean m_SimpleMode = true;

  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input 
   * instance structure (any instances contained in the object are 
   * ignored - only the structure is required).
   * @return true if the outputFormat may be collected immediately
   * @exception Exception if the input format can't be set 
   * successfully
   */
  public boolean setInputFormat(Instances instanceInfo) 
       throws Exception {

    super.setInputFormat(instanceInfo);
    if (instanceInfo.classIndex() < 0 
	|| instanceInfo.classAttribute().isNominal()) {
      //      throw new UnassignedClassException("No class has been assigned to the instances");
      m_SimpleMode = true;
    } else if (instanceInfo.classAttribute().isNumeric()) {
      m_SimpleMode = false;
    }
	  
    setOutputFormat();
    m_Indices = null;
    if (m_SimpleMode /*|| instanceInfo.classAttribute().isNominal()*/) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Input an instance for filtering. Filter requires all
   * training instances be read before producing output.
   *
   * @param instance the input instance
   * @return true if the filtered instance may now be
   * collected with output().
   * @exception IllegalStateException if no input format has been set
   */
  public boolean input(Instance instance) {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }
    if ((m_Indices != null) || 
	(m_SimpleMode == true 
	 /* getInputFormat().classAttribute().isNominal())*/)) {
      convertInstance(instance);
      return true;
    }
    bufferInput(instance);
    return false;
  }

  /**
   * Signify that this batch of input to the filter is finished. 
   * If the filter requires all instances prior to filtering,
   * output() may now be called to retrieve the filtered instances.
   *
   * @return true if there are instances pending output
   * @exception IllegalStateException if no input structure has been defined
   */
  public boolean batchFinished() {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if ((m_Indices == null) && 
	(getInputFormat().classIndex() >= 0 && 
	 getInputFormat().classAttribute().isNumeric())) {
      computeAverageClassValues();
      setOutputFormat();

      // Convert pending input instances

      for(int i = 0; i < getInputFormat().numInstances(); i++) {
	convertInstance(getInputFormat().instance(i));
      }
    } 
    flushInput();

    m_NewBatch = true;
    return (numPendingOutput() != 0);
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(1);

    newVector.addElement(new Option(
	      "\tSets if binary attributes are to be coded as nominal ones.",
	      "N", 0, "-N"));

    return newVector.elements();
  }


  /**
   * Parses the options for this object. Valid options are: <p>
   *
   * -N <br>
   * If binary attributes are to be coded as nominal ones.<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    setBinaryAttributesNominal(Utils.getFlag('N', options));

    if (getInputFormat() != null)
      setInputFormat(getInputFormat());
  }

  /**
   * Gets the current settings of the filter.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [1];
    int current = 0;

    if (getBinaryAttributesNominal()) {
      options[current++] = "-N";
    }

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Gets if binary attributes are to be treated as nominal ones.
   *
   * @return true if binary attributes are to be treated as nominal ones
   */
  public boolean getBinaryAttributesNominal() {

    return !m_Numeric;
  }

  /**
   * Sets if binary attributes are to be treates as nominal ones.
   *
   * @param bool true if binary attributes are to be treated as nominal ones
   */
  public void setBinaryAttributesNominal(boolean bool) {

    m_Numeric = !bool;
  }

  /** Computes average class values for each attribute and value */
  private void computeAverageClassValues() {
    
    double totalCounts, sum;
    Instance instance;
    double [] counts;

    double [][] avgClassValues = new double[getInputFormat().numAttributes()][0];
    m_Indices = new int[getInputFormat().numAttributes()][0];
    for (int j = 0; j < getInputFormat().numAttributes(); j++) {
      Attribute att = getInputFormat().attribute(j);
      if (att.isNominal()) {
	avgClassValues[j] = new double [att.numValues()];
	counts = new double [att.numValues()];
	for (int i = 0; i < getInputFormat().numInstances(); i++) {
	  instance = getInputFormat().instance(i);
	  if (!instance.classIsMissing() && 
	      (!instance.isMissing(j))) {
	    counts[(int)instance.value(j)] += instance.weight();
	    avgClassValues[j][(int)instance.value(j)] += 
	      instance.weight() * instance.classValue();
	  }
	}
	sum = Utils.sum(avgClassValues[j]);
	totalCounts = Utils.sum(counts);
	if (Utils.gr(totalCounts, 0)) {
	  for (int k = 0; k < att.numValues(); k++) {
	    if (Utils.gr(counts[k], 0)) {
	      avgClassValues[j][k] /= (double)counts[k];
	    } else {
	      avgClassValues[j][k] = sum / (double)totalCounts;
	    }
	  }
	}
	m_Indices[j] = Utils.sort(avgClassValues[j]);
      }
    }
  }

  /** Set the output format. */
  private void setOutputFormat() {

    if (getInputFormat().classIndex() < 0 || 
	getInputFormat().classAttribute().isNominal()) {
      setOutputFormatNominal();
    } else {
      setOutputFormatNumeric();
    }
  }

  /**
   * Convert a single instance over. The converted instance is 
   * added to the end of the output queue.
   *
   * @param instance the instance to convert
   */
  private void convertInstance(Instance inst) {

    if (getInputFormat().classIndex() < 0 || 
	getInputFormat().classAttribute().isNominal()) {
      convertInstanceNominal(inst);
    } else {
      convertInstanceNumeric(inst);
    }
  }

  /**
   * Set the output format if the class is nominal or there is no
   * class set
   */
  private void setOutputFormatNominal() {

    FastVector newAtts;
    int newClassIndex;
    StringBuffer attributeName;
    Instances outputFormat;
    FastVector vals;

    // Compute new attributes

    newClassIndex = getInputFormat().classIndex();
    newAtts = new FastVector();
    for (int j = 0; j < getInputFormat().numAttributes(); j++) {
      Attribute att = getInputFormat().attribute(j);
      if ((!att.isNominal()) || 
	  (j == getInputFormat().classIndex())) {
	newAtts.addElement(att.copy());
      } else {
	if (att.numValues() <= 2) {
	  if (m_Numeric) {
	    newAtts.addElement(new Attribute(att.name()));
	  } else {
	    newAtts.addElement(att.copy());
	  }
	} else {

	  if (j < getInputFormat().classIndex()) {
	    newClassIndex += att.numValues() - 1;
	  }

	  // Compute values for new attributes
	  for (int k = 0; k < att.numValues(); k++) {
	    attributeName = 
	      new StringBuffer(att.name() + "=");
	    attributeName.append(att.value(k));
	    if (m_Numeric) {
	      newAtts.
		addElement(new Attribute(attributeName.toString()));
	    } else {
	      vals = new FastVector(2);
	      vals.addElement("f"); vals.addElement("t");
	      newAtts.
		addElement(new Attribute(attributeName.toString(), vals));
	    }
	  }
	}
      }
    }
    outputFormat = new Instances(getInputFormat().relationName(),
				 newAtts, 0);
    if (newClassIndex >= 0) {
      outputFormat.setClassIndex(newClassIndex);
    }
    setOutputFormat(outputFormat);
  }

  /**
   * Set the output format if the class is numeric.
   */
  private void setOutputFormatNumeric() {

    if (m_Indices == null) {
      setOutputFormat(null);
      return;
    }
    FastVector newAtts;
    int newClassIndex;
    StringBuffer attributeName;
    Instances outputFormat;
    FastVector vals;

    // Compute new attributes

    newClassIndex = getInputFormat().classIndex();
    newAtts = new FastVector();
    for (int j = 0; j < getInputFormat().numAttributes(); j++) {
      Attribute att = getInputFormat().attribute(j);
      if ((!att.isNominal()) || 
	  (j == getInputFormat().classIndex())) {
	newAtts.addElement(att.copy());
      } else {
	if (j < getInputFormat().classIndex())
	  newClassIndex += att.numValues() - 2;
	  
	// Compute values for new attributes
	  
	for (int k = 1; k < att.numValues(); k++) {
	  attributeName = 
	    new StringBuffer(att.name() + "=");
	  for (int l = k; l < att.numValues(); l++) {
	    if (l > k) {
	      attributeName.append(',');
	    }
	    attributeName.append(att.value(m_Indices[j][l]));
	  }
	  if (m_Numeric) {
	    newAtts.
	      addElement(new Attribute(attributeName.toString()));
	  } else {
	    vals = new FastVector(2);
	    vals.addElement("f"); vals.addElement("t");
	    newAtts.
	      addElement(new Attribute(attributeName.toString(), vals));
	  }
	}
      }
    }
    outputFormat = new Instances(getInputFormat().relationName(),
				 newAtts, 0);
    outputFormat.setClassIndex(newClassIndex);
    setOutputFormat(outputFormat);
  }

  /**
   * Convert a single instance over if the class is nominal
   * or no class is set. The converted 
   * instance is added to the end of the output queue.
   *
   * @param instance the instance to convert
   */
  private void convertInstanceNominal(Instance instance) {
  
    double [] vals = new double [outputFormatPeek().numAttributes()];
    int attSoFar = 0;

    for(int j = 0; j < getInputFormat().numAttributes(); j++) {
      Attribute att = getInputFormat().attribute(j);
      if ((!att.isNominal()) || (j == getInputFormat().classIndex())) {
	vals[attSoFar] = instance.value(j);
	attSoFar++;
      } else {
	if (att.numValues() <= 2) {
	  vals[attSoFar] = instance.value(j);
	  attSoFar++;
	} else {
	  if (instance.isMissing(j)) {
	    for (int k = 0; k < att.numValues(); k++) {
              vals[attSoFar + k] = instance.value(j);
	    }
	  } else {
	    for (int k = 0; k < att.numValues(); k++) {
	      if (k == (int)instance.value(j)) {
                vals[attSoFar + k] = 1;
	      } else {
                vals[attSoFar + k] = 0;
	      }
	    }
	  }
	  attSoFar += att.numValues();
	}
      }
    }
    Instance inst = null;
    if (instance instanceof SparseInstance) {
      inst = new SparseInstance(instance.weight(), vals);
    } else {
      inst = new Instance(instance.weight(), vals);
    }
    copyStringValues(inst, false, instance.dataset(), getInputStringIndex(),
                     getOutputFormat(), getOutputStringIndex());
    inst.setDataset(getOutputFormat());
    push(inst);
  }

  /**
   * Convert a single instance over if the class is numeric. The converted 
   * instance is added to the end of the output queue.
   *
   * @param instance the instance to convert
   */
  private void convertInstanceNumeric(Instance instance) {
  
    double [] vals = new double [outputFormatPeek().numAttributes()];
    int attSoFar = 0;

    for(int j = 0; j < getInputFormat().numAttributes(); j++) {
      Attribute att = getInputFormat().attribute(j);
      if ((!att.isNominal()) || (j == getInputFormat().classIndex())) {
	vals[attSoFar] = instance.value(j);
	attSoFar++;
      } else {
	if (instance.isMissing(j)) {
	  for (int k = 0; k < att.numValues() - 1; k++) {
            vals[attSoFar + k] = instance.value(j);
	  }
	} else {
	  int k = 0;
	  while ((int)instance.value(j) != m_Indices[j][k]) {
            vals[attSoFar + k] = 1;
	    k++;
	  }
	  while (k < att.numValues() - 1) {
            vals[attSoFar + k] = 0;
	    k++;
	  }
	}
	attSoFar += att.numValues() - 1;
      }
    }
    Instance inst = null;
    if (instance instanceof SparseInstance) {
      inst = new SparseInstance(instance.weight(), vals);
    } else {
      inst = new Instance(instance.weight(), vals);
    }
    copyStringValues(inst, false, instance.dataset(), getInputStringIndex(),
                     getOutputFormat(), getOutputStringIndex());
    inst.setDataset(getOutputFormat());
    push(inst);
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: 
   * use -h for help
   */
  public static void main(String [] argv) {

    try {
      if (Utils.getFlag('b', argv)) {
 	Filter.batchFilterFile(new NominalToBinaryFilter(), argv);
      } else {
	Filter.filterFile(new NominalToBinaryFilter(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
      ex.printStackTrace();
    }
  }
}








