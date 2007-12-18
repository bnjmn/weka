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
 *    NumericToBinary.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package weka.filters.unsupervised.attribute;

import weka.filters.*;
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
 * @version $Revision: 1.3 $ 
 */
public class NumericToBinary extends PotentialClassIgnorer
  implements UnsupervisedFilter, StreamableFilter {

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "Converts all numeric attributes into binary attributes (apart from "
      + "the class attribute, if set): if the value of the numeric attribute is "
      + "exactly zero, the value of the new attribute will be zero. If the "
      + "value of the numeric attribute is missing, the value of the new "
      + "attribute will be missing. Otherwise, the value of the new "
      + "attribute will be one. The new attributes will nominal.";
  }

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
  public boolean setInputFormat(Instances instanceInfo) throws Exception {

    super.setInputFormat(instanceInfo);
    setOutputFormat();
    return true;
  }

  /**
   * Input an instance for filtering.
   *
   * @param instance the input instance
   * @return true if the filtered instance may now be
   * collected with output().
   * @exception IllegalStateException if no input format has been defined.
   */
  public boolean input(Instance instance) {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }
    convertInstance(instance);
    return true;
  }

  /** 
   * Set the output format. 
   */
  private void setOutputFormat() {

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
      if ((j == newClassIndex) || (!att.isNumeric())) {
	newAtts.addElement(att.copy());
      } else {
	attributeName = new StringBuffer(att.name() + "_binarized");
	vals = new FastVector(2);
	vals.addElement("0"); vals.addElement("1");
	newAtts.addElement(new Attribute(attributeName.toString(), vals));
      }
    }
    outputFormat = new Instances(getInputFormat().relationName(), newAtts, 0);
    outputFormat.setClassIndex(newClassIndex);
    setOutputFormat(outputFormat);
  }

  /**
   * Convert a single instance over. The converted instance is 
   * added to the end of the output queue.
   *
   * @param instance the instance to convert
   */
  private void convertInstance(Instance instance) {
  
    Instance inst = null;
    if (instance instanceof SparseInstance) {
      double[] vals = new double[instance.numValues()];
      int[] newIndices = new int[instance.numValues()];
      for (int j = 0; j < instance.numValues(); j++) {
	Attribute att = getInputFormat().attribute(instance.index(j));
	if ((!att.isNumeric()) || (instance.index(j) == getInputFormat().classIndex())) {
	  vals[j] = instance.valueSparse(j);
	} else {
	  if (instance.isMissingSparse(j)) {
	    vals[j] = instance.valueSparse(j);
	  } else {
	    vals[j] = 1;
	  }
	} 
	newIndices[j] = instance.index(j);
      }
      inst = new SparseInstance(instance.weight(), vals, newIndices, 
                                outputFormatPeek().numAttributes());
    } else {
      double[] vals = new double[outputFormatPeek().numAttributes()];
      for (int j = 0; j < getInputFormat().numAttributes(); j++) {
	Attribute att = getInputFormat().attribute(j);
	if ((!att.isNumeric()) || (j == getInputFormat().classIndex())) {
	  vals[j] = instance.value(j);
	} else {
	  if (instance.isMissing(j) || (instance.value(j) == 0)) {
	    vals[j] = instance.value(j);
	  } else {
	    vals[j] = 1;
	  }
	} 
      }
      inst = new Instance(instance.weight(), vals);
    }
    inst.setDataset(instance.dataset());
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
 	Filter.batchFilterFile(new NumericToBinary(), argv);
      } else {
	Filter.filterFile(new NumericToBinary(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}








