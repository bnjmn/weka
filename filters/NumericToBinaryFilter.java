/*
 *    NumericToBinaryFilter.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */


package weka.filters;

import java.io.*;
import java.util.*;
import weka.core.*;

/** 
 * Converts all numeric attributes into binary attributes (apart from
 * the class attribute): if the value of the numeric attribute is
 * exactly zero, the value of the new attribute will be zero. If the
 * value of the numeric attribute is missing, the value of the new
 * attribute will be missing. Otherwise, the value of the new
 * attribute will be one. The new attributes will nominal.<p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz) 
 * @version $Revision: 1.2 $ 
 */
public class NumericToBinaryFilter extends Filter {

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
  public boolean inputFormat(Instances instanceInfo) throws Exception {

    m_InputFormat = new Instances(instanceInfo, 0);
    m_NewBatch = true;
    setOutputFormat();
    return true;
  }

  /**
   * Input an instance for filtering.
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
    convertInstance(instance);
    return true;
  }

  /**
   * Signify that this batch of input to the filter is finished. 
   *
   * @return true if there are instances pending output
   * @exception Exception if no input structure has been defined
   */
  public boolean batchFinished() throws Exception {

    Instance current;

    if (m_InputFormat == null) {
      throw new Exception("No input instance format defined");
    }
    m_NewBatch = true;
    return (numPendingOutput() != 0);
  }

  /** 
   * Set the output format. 
   */
  private void setOutputFormat() throws Exception {

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
      if ((j == newClassIndex) || (!att.isNumeric())) {
	newAtts.addElement(att.copy());
      } else {
	attributeName = new StringBuffer(att.name() + "_binarized");
	vals = new FastVector(2);
	vals.addElement("0"); vals.addElement("1");
	newAtts.addElement(new Attribute(attributeName.toString(), vals));
      }
    }
    outputFormat = new Instances(m_InputFormat.relationName(), newAtts, 0);
    outputFormat.setClassIndex(newClassIndex);
    setOutputFormat(outputFormat);
  }

  /**
   * Convert a single instance over. The converted instance is 
   * added to the end of the output queue.
   *
   * @param instance the instance to convert
   */
  private void convertInstance(Instance inst) throws Exception {
  
    if (!(inst instanceof SparseInstance)) {
      double[] newVals = new double[outputFormatPeek().numAttributes()];
      for (int j = 0; j < m_InputFormat.numAttributes(); j++) {
	Attribute att = m_InputFormat.attribute(j);
	if ((!att.isNumeric()) || (j == m_InputFormat.classIndex())) {
	  newVals[j] = inst.value(j);
	} else {
	  if (inst.isMissing(j) || (inst.value(j) == 0)) {
	    newVals[j] = inst.value(j);
	  } else {
	    newVals[j] = 1;
	  }
	} 
      }
      push(new Instance(inst.weight(), newVals));
    } else {
      double[] newVals = new double[inst.numValues()];
      int[] newIndices = new int[inst.numValues()];
      for (int j = 0; j < inst.numValues(); j++) {
	Attribute att = m_InputFormat.attribute(inst.index(j));
	if ((!att.isNumeric()) || (inst.index(j) == m_InputFormat.classIndex())) {
	  newVals[j] = inst.valueSparse(j);
	} else {
	  if (inst.isMissingSparse(j)) {
	    newVals[j] = inst.valueSparse(j);
	  } else {
	    newVals[j] = 1;
	  }
	} 
	newIndices[j] = inst.index(j);
      }
      push(new SparseInstance(inst.weight(), newVals, newIndices, 
			      outputFormatPeek().numAttributes()));
    }
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
 	Filter.batchFilterFile(new NumericToBinaryFilter(), argv);
      } else {
	Filter.filterFile(new NumericToBinaryFilter(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}








