/*
 *    NominalToBinaryFilter.java
 *    Copyright (C) 1999 Eibe Frank
 *
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

package weka.filters;

import java.io.*;
import java.util.*;
import weka.core.*;

/** 
 * Converts all nominal attributes into binary numeric 
 * attributes. An attribute with k values is transformed into
 * k-1 new binary attributes (in a similar manner to CART if a
 * numeric class is assigned). Currently requires that a class attribute
 * be set (but this should be changed).<p>
 *
 * Valid filter-specific options are: <p>
 *
 * -N <br>
 * If binary attributes are to be coded as nominal ones.<p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz) 
 * @version $Revision: 1.6 $
 */
public class NominalToBinaryFilter extends Filter implements OptionHandler {

  /** The average class values for all nominal values in the training data. */
  private double[][] m_AvgClassValues = null;

  /** The sorted indices of the attribute values. */
  private int[][] m_Indices = null;

  /** Are the new attributes going to be nominal or numeric ones? */
  private boolean m_Numeric = true;

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
  public boolean inputFormat(Instances instanceInfo) 
       throws Exception {

    m_InputFormat = new Instances(instanceInfo, 0);
    if (m_InputFormat.classIndex() < 0) {
      throw new Exception("No class has been assigned to the instances");
    }
    m_NewBatch = true;
    setOutputFormat();
    m_AvgClassValues = null;
    m_Indices = null;
    if (m_InputFormat.classAttribute().isNominal()) {
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
   * @exception Exception if the input instance was not of the 
   * correct format or if there was a problem with the filtering.
   */
  public boolean input(Instance instance) throws Exception {

    if (m_InputFormat == null) {
      throw new Exception("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }
    if ((m_AvgClassValues != null) || 
	(m_InputFormat.classAttribute().isNominal())) {
      convertInstance(instance);
      return true;
    }
    m_InputFormat.add(instance);
    return false;
  }

  /**
   * Signify that this batch of input to the filter is finished. 
   * If the filter requires all instances prior to filtering,
   * output() may now be called to retrieve the filtered instances.
   *
   * @return true if there are instances pending output
   * @exception Exception if no input structure has been defined
   */
  public boolean batchFinished() throws Exception {

    Instance current;

    if (m_InputFormat == null) {
      throw new Exception("No input instance format defined");
    }
    if ((m_AvgClassValues == null) && 
	(m_InputFormat.classAttribute().isNumeric())) {
      computeAverageClassValues();
      setOutputFormat();

      // Convert pending input instances

      for(int i = 0; i < m_InputFormat.numInstances(); i++) {
	current = m_InputFormat.instance(i);
	convertInstance(current);
      }
      m_InputFormat = new Instances(m_InputFormat, 0);
    } 

    m_NewBatch = true;
    return (numPendingOutput() != 0);
  }

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
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

    if (m_InputFormat != null)
      inputFormat(m_InputFormat);
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
  private void computeAverageClassValues() throws Exception {
    
    double totalCounts, sum;
    Instance instance;
    double[] counts;

    m_AvgClassValues = new double[m_InputFormat.numAttributes()][0];
    m_Indices = new int[m_InputFormat.numAttributes()][0];
    for (int j = 0; j < m_InputFormat.numAttributes(); j++) {
      Attribute att = m_InputFormat.attribute(j);
      if (att.isNominal()) {
	m_AvgClassValues[j] = new double [att.numValues()];
	counts = new double [att.numValues()];
	for (int i = 0; i < m_InputFormat.numInstances(); i++) {
	  instance = m_InputFormat.instance(i);
	  if (!instance.classIsMissing() && 
	      (!instance.isMissing(j))) {
	    counts[(int)instance.value(j)] += instance.weight();
	    m_AvgClassValues[j][(int)instance.value(j)] += 
	      instance.weight() * instance.classValue();
	  }
	}
	sum = Utils.sum(m_AvgClassValues[j]);
	totalCounts = Utils.sum(counts);
	if (Utils.gr(totalCounts, 0)) {
	  for (int k = 0; k < att.numValues(); k++) {
	    if (Utils.gr(counts[k], 0)) {
	      m_AvgClassValues[j][k] /= (double)counts[k];
	    } else {
	      m_AvgClassValues[j][k] = sum / (double)totalCounts;
	    }
	  }
	}
	m_Indices[j] = Utils.sort(m_AvgClassValues[j]);
      }
    }
  }

  /** Set the output format. */
  private void setOutputFormat() throws Exception {

    if (m_InputFormat.classAttribute().isNominal()) {
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
  private void convertInstance(Instance inst) throws Exception {

    if (m_InputFormat.classAttribute().isNominal()) {
      convertInstanceNominal(inst);
    } else {
      convertInstanceNumeric(inst);
    }
  }

  /**
   * Set the output format if the class is nominal.
   *
   * @exception Exception if a problem occurs when setting the output format
   */
  private void setOutputFormatNominal() throws Exception {

    FastVector newAtts;
    int newClassIndex;
    StringBuffer attributeName;
    Instances outputFormat;
    FastVector vals;

    // Compute new attributes

    newClassIndex = m_InputFormat.classIndex();
    newAtts = new FastVector();
    for (int j = 0; j < m_InputFormat.numAttributes(); j++) {
      Attribute att = m_InputFormat.attribute(j);
      if ((!att.isNominal()) || 
	  (j == m_InputFormat.classIndex())) {
	newAtts.addElement(att.copy());
      } else {
	if (j < m_InputFormat.classIndex()) {
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
    outputFormat = new Instances(m_InputFormat.relationName(),
				 newAtts, 0);
    outputFormat.setClassIndex(newClassIndex);
    setOutputFormat(outputFormat);
  }

  /**
   * Set the output format if the class is numeric.
   *
   * @exception Exception if a problem occurs when setting the output format
   */
  private void setOutputFormatNumeric() throws Exception {

    if (m_AvgClassValues == null) {
      setOutputFormat(null);
      return;
    }
    FastVector newAtts;
    int newClassIndex;
    StringBuffer attributeName;
    Instances outputFormat;
    FastVector vals;

    // Compute new attributes

    newClassIndex = m_InputFormat.classIndex();
    newAtts = new FastVector();
    for (int j = 0; j < m_InputFormat.numAttributes(); j++) {
      Attribute att = m_InputFormat.attribute(j);
      if ((!att.isNominal()) || 
	  (j == m_InputFormat.classIndex())) {
	newAtts.addElement(att.copy());
      } else {
	if (j < m_InputFormat.classIndex())
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
    outputFormat = new Instances(m_InputFormat.relationName(),
				 newAtts, 0);
    outputFormat.setClassIndex(newClassIndex);
    setOutputFormat(outputFormat);
  }

  /**
   * Convert a single instance over if the class is nominal. The converted 
   * instance is added to the end of the output queue.
   *
   * @param instance the instance to convert
   */
  private void convertInstanceNominal(Instance instance) throws Exception {
  
    Instance newInstance = 
      new Instance(outputFormatPeek().numAttributes());
    int attSoFar = 0;

    for(int j = 0; j < m_InputFormat.numAttributes(); j++) {
      Attribute att = m_InputFormat.attribute(j);
      if ((!att.isNominal()) || (j == m_InputFormat.classIndex())) {
	newInstance.setValue(attSoFar, instance.value(j));
	attSoFar++;
      } else {
	if (instance.isMissing(j)) {
	  for (int k = 0; k < att.numValues(); k++) {
	    newInstance.setValue(attSoFar + k, instance.value(j));
	  }
	} else {
	  for (int k = 0; k < att.numValues(); k++) {
	    if (k == (int)instance.value(j)) {
	      newInstance.setValue(attSoFar + k, 1);
	    } else {
	      newInstance.setValue(attSoFar + k, 0);
	    }
	  }
	}
	attSoFar += att.numValues();
      }
    }
    newInstance.setWeight(instance.weight());
    push(newInstance);
  }

  /**
   * Convert a single instance over if the class is numeric. The converted 
   * instance is added to the end of the output queue.
   *
   * @param instance the instance to convert
   */
  private void convertInstanceNumeric(Instance instance) throws Exception {
  
    Instance newInstance = 
      new Instance(outputFormatPeek().numAttributes());
    int attSoFar = 0;

    for(int j = 0; j < m_InputFormat.numAttributes(); j++) {
      Attribute att = m_InputFormat.attribute(j);
      if ((!att.isNominal()) || (j == m_InputFormat.classIndex())) {
	newInstance.setValue(attSoFar, instance.value(j));
	attSoFar++;
      } else {
	if (instance.isMissing(j)) {
	  for (int k = 0; k < att.numValues() - 1; k++) {
	    newInstance.setValue(attSoFar + k, instance.value(j));
	  }
	} else {
	  int k = 0;
	  while ((int)instance.value(j) != m_Indices[j][k]) {
	    newInstance.setValue(attSoFar + k, 1);
	    k++;
	  }
	  while (k < att.numValues() - 1) {
	    newInstance.setValue(attSoFar + k, 0);
	    k++;
	  }
	}
	attSoFar += att.numValues() - 1;
      }
    }
    newInstance.setWeight(instance.weight());
    push(newInstance);
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
    }
  }
}








