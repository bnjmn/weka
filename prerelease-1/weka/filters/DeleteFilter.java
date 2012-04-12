/*
 *    DeleteFilter.java
 *    Copyright (C) 1999 Len Trigg
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
 * An instance filter that deletes a range of attributes from the dataset.<p>
 *
 * Valid scheme-specific options are:<p>
 *
 * -R index1,index2-index4,...<br>
 * Specify list of columns to delete. First and last are valid indexes.
 * (default none)<p>
 *
 * -V<br>
 * Invert matching sense (i.e. only keep specified columns)<p>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version 1.0
 */
public class DeleteFilter extends Filter implements OptionHandler {

  // =================
  // Protected members
  // =================

  /** Stores which columns to delete */
  protected Range m_DeleteCols = new Range();

  // ===============
  // Public methods.
  // ===============

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(2);

    newVector.addElement(new Option(
              "\tSpecify list of columns to delete. First and last are valid\n"
	      +"\tindexes. (default none)",
              "R", 1, "-R <index1,index2-index4,...>"));
    newVector.addElement(new Option(
	      "\tInvert matching sense (i.e. only keep specified columns)",
              "V", 0, "-V"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options controlling the behaviour of this object.
   * Valid options are:<p>
   *
   * -R index1,index2-index4,...<br>
   * Specify list of columns to delete. First and last are valid indexes.
   * (default none)<p>
   *
   * -V<br>
   * Invert matching sense (i.e. only keep specified columns)<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String deleteList = Utils.getOption('R', options);
    if (deleteList.length() != 0) {
      setAttributeIndices(deleteList);
    }
    setInvertSelection(Utils.getFlag('V', options));
    
    if (m_InputFormat != null) {
      inputFormat(m_InputFormat);
    }
  }

  /**
   * Gets the current settings of the filter.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [3];
    int current = 0;

    if (getInvertSelection()) {
      options[current++] = "-V";
    }
    if (!getAttributeIndices().equals("")) {
      options[current++] = "-R"; options[current++] = getAttributeIndices();
    }

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input instance
   * structure (any instances contained in the object are ignored - only the
   * structure is required).
   * @return true if the outputFormat may be collected immediately
   */
  public boolean inputFormat(Instances instanceInfo) {

    m_InputFormat = new Instances(instanceInfo, 0);
    b_NewBatch = true;
    
    m_DeleteCols.setUpper(m_InputFormat.numAttributes()-1);
    // Create the output buffer
    Instances outputFormat = new Instances(instanceInfo, 0); 

    try {
      for(int i = m_InputFormat.numAttributes()-1; i >= 0; i--) {
	if (m_DeleteCols.isInRange(i)) {
	  // If they want the class column deleted, re-assign it
	  if (i == outputFormat.classIndex()) {
	    outputFormat.setClassIndex(0);
	  }
	  outputFormat.deleteAttributeAt(i);
	}
      }
      setOutputFormat(outputFormat);
    } catch (Exception ex) {
      System.err.println("Exception: " + ex.getMessage());
      System.err.println("Weka currently doesn't allow deleting all " +
			 "columns of Instances");
      System.exit(0);
    }
    return true;
  }
  

  /**
   * Input an instance for filtering. Ordinarily the instance is processed
   * and made available for output immediately. Some filters require all
   * instances be read before producing output.
   *
   * @param instance the input instance
   * @return true if the filtered instance may now be
   * collected with output().
   * @exception Exception if the input instance was not of the correct 
   * format or if there was a problem with the filtering.
   */
  public boolean input(Instance instance) throws Exception {

    if (m_InputFormat == null) {
      throw new Exception("No input instance format defined");
    }
    if (b_NewBatch) {
      resetQueue();
      b_NewBatch = false;
    }

    Instances outputFormat = outputFormatPeek();
    Instance newInstance = new Instance(outputFormat.
					numAttributes());
    int i, j;
    for(i = 0, j = 0; j < outputFormat.numAttributes(); i++, j++) {
      while (m_DeleteCols.isInRange(i)) {
	i++;
      }
      newInstance.setValue(j, instance.value(i));
    }
    newInstance.setWeight(instance.weight());
    push(newInstance);
    return true;
  }

  /**
   * Get whether the supplied columns are to be removed or kept
   *
   * @return true if the supplied columns will be kept
   */
  public boolean getInvertSelection() {

    return m_DeleteCols.getInvert();
  }

  /**
   * Set whether selected columns should be removed or kept. If true the 
   * selected columns are kept and unselected columns are deleted. If false
   * selected columns are deleted and unselected columns are kept.
   *
   * @param invert the new invert setting
   */
  public void setInvertSelection(boolean invert) {

    m_DeleteCols.setInvert(invert);
  }

  /**
   * Get the current range selection
   *
   * @return a string containing a comma separated list of ranges
   */
  public String getAttributeIndices() {

    return m_DeleteCols.getRanges();
  }

  /**
   * Set which attributes are to be deleted (or kept if invert is true)
   *
   * @param rangeList a string representing the list of attributes.  Since
   * the string will typically come from a user, attributes are indexed from
   * 1. <br>
   * eg: first-3,5,6-last
   * @exception Exception if an invalid range list is supplied
   */
  public void setAttributeIndices(String rangeList) throws Exception {

    m_DeleteCols.setRanges(rangeList);
  }

  /**
   * Set which attributes are to be deleted (or kept if invert is true)
   *
   * @param attributes an array containing indexes of attributes to select.
   * Since the array will typically come from a program, attributes are indexed
   * from 0.
   * @exception Exception if an invalid set of ranges is supplied
   */
  public void setAttributeIndicesArray(int [] attributes) throws Exception {

    String rangeList = "";
    for(int i = 0; i < attributes.length; i++) {
      if (i == 0) {
	rangeList = ""+(attributes[i]+1);
      } else {
	rangeList += ","+(attributes[i]+1);
      }
    }
    setAttributeIndices(rangeList);
  }

  // ============
  // Test method.
  // ============

  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String [] argv) {

    try {
      if (Utils.getFlag('b', argv)) {
 	Filter.batchFilterFile(new DeleteFilter(),argv); 
      } else {
	Filter.filterFile(new DeleteFilter(),argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}








