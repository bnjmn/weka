/*
 *    NumericTransformFilter.java
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
import java.lang.reflect.*;
import weka.core.*;

/** 
 * Transforms numeric attributes using a
 * given transformation method.<p>
 *
 * Valid filter-specific options are: <p>
 *
 * -R index1,index2-index4,...<br>
 * Specify list of columns to transform. First and last are valid indexes.
 * (default none). Non-numeric columns are skipped.<p>
 *
 * -V<br>
 * Invert matching sense.<p>
 *
 * -C string <br>
 * Name of the class containing the method used for transformation.<p>
 *
 * -M string <br>
 * Name of the method used for the transformation.<p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz) 
 * @version $Revision: 1.4 $
 */
public class NumericTransformFilter extends Filter implements OptionHandler {

  /** Stores which columns to transform. */
  private Range m_Cols = new Range();

  /** Class containing transformation method. */
  private Class m_Class = null;

  /** Transformation method. */
  private Method m_Method = null;

  /** Parameter types. */
  private static Class[] PARAM = new Class[] {Double.TYPE};

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

    if (m_Class == null) {
      throw new Exception("No class has been set.");
    }
    if (m_Method == null) {
      throw new Exception("No method has been set.");
    }
    m_InputFormat = new Instances(instanceInfo, 0);
    m_Cols.setUpper(m_InputFormat.numAttributes() - 1);
    setOutputFormat(m_InputFormat);
    m_NewBatch = true;
    return true;
  }

  /**
   * Input an instance for filtering. The instance is processed
   * and made available for output immediately.
   *
   * @param instance the input instance
   * @return true if the filtered instance may now be
   * collected with output().
   * @exception Exception if the input instance was not of the 
   * correct format or if there was a problem with the filtering.
   */
  public boolean input(Instance instance) throws Exception {

    double oldVal;
    Double[] params = new Double[1];
    Double newVal;

    if (m_InputFormat == null) {
      throw new Exception("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }

    double[] vals = new double[instance.numAttributes()];
    for(int i = 0; i < instance.numAttributes(); i++) {
      if (instance.isMissing(i)) {
	vals[i] = Instance.missingValue();
      } else {
	oldVal = instance.value(i);
	if (m_Cols.isInRange(i) &&
	    instance.attribute(i).isNumeric()) {
	  params[0] = new Double(oldVal);
	  newVal = (Double) m_Method.invoke(null, params);
	  if (newVal.isNaN() || newVal.isInfinite()) {
	    vals[i] = Instance.missingValue();
	  } else {
	    vals[i] = newVal.doubleValue(); 
	  }
	} else {
	  vals[i] = oldVal;
	}
      }
    }
    push(new Instance(instance.weight(), vals));
   
    return true;
  }

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(4);

    newVector.addElement(new Option(
              "\tSpecify list of columns to transform. First and last are\n"
	      + "\tvalid indexes (default none). Non-numeric columns are \n"
	      + "\tskipped.",
              "R", 1, "-R <index1,index2-index4,...>"));

    newVector.addElement(new Option(
	      "\tInvert matching sense.",
              "V", 0, "-V"));

    newVector.addElement(new Option(
              "\tSets the class containing transformation method.",
              "C", 1, "-C <string>"));

    newVector.addElement(new Option(
              "\tSets the method.",
              "M", 1, "-M <string>"));

    return newVector.elements();
  }


  /**
   * Parses the options for this object. Valid options are: <p>
   *
   * -R index1,index2-index4,...<br>
   * Specify list of columns to transform. First and last are valid indexes.
   * (default none). Non-numeric columns are skipped.<p>
   *
   * -V<br>
   * Invert matching sense.<p>
   *
   * -C string <br>
   * Name of the class containing the method used for transformation.<p>
   *
   * -M string <br>
   * Name of the method used for the transformation.<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    setAttributeIndices(Utils.getOption('R', options));
    setInvertSelection(Utils.getFlag('V', options));
    setClassName(Utils.getOption('C', options));
    setMethodName(Utils.getOption('M', options));

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

    String [] options = new String [7];
    int current = 0;

    if (getInvertSelection()) {
      options[current++] = "-V";
    }
    if (!getAttributeIndices().equals("")) {
      options[current++] = "-R"; options[current++] = getAttributeIndices();
    }
    if (m_Class != null) {
      options[current++] = "-C"; options[current++] = getClassName();
    }
    if (m_Method != null) {
      options[current++] = "-M"; options[current++] = getMethodName();
    }

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Get the class containing the transformation method.
   *
   * @return string describing the class
   */
  public String getClassName() {

    return m_Class.getName();
  }
 
  /**
   * Sets the class containing the transformation method.
   *
   * @param name the name of the class
   * @exception Exception if class can't be found
   */
  public void setClassName(String name) throws Exception {
  
    m_Class = Class.forName(name);
  }
 
  /**
   * Get the transformation method.
   *
   * @return string describing the transformation method.
   */
  public String getMethodName() {

    return m_Method.getName();
  }

  /**
   * Set the transformation method.
   *
   * @param name the name of the method
   * @exception Exception if method can't be found in class
   */
  public void setMethodName(String name) throws Exception {

    m_Method = m_Class.getMethod(name, PARAM);
  }

  /**
   * Get whether the supplied columns are to be transformed or not
   *
   * @return true if the supplied columns will be kept
   */
  public boolean getInvertSelection() {

    return m_Cols.getInvert();
  }

  /**
   * Set whether selected columns should be transformed or not. 
   *
   * @param invert the new invert setting
   */
  public void setInvertSelection(boolean invert) {

    m_Cols.setInvert(invert);
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String attributeIndicesTipText() {
    return "Specify range of attributes to act on."
      + " This is a comma separated list of attribute indices, with"
      + " \"first\" and \"last\" valid values. Specify an inclusive"
      + " range with \"-\". E.g: \"first-3,5,6-10,last\".";
  }

  /**
   * Get the current range selection
   *
   * @return a string containing a comma separated list of ranges
   */
  public String getAttributeIndices() {

    return m_Cols.getRanges();
  }

  /**
   * Set which attributes are to be transformed (or kept if invert is true). 
   *
   * @param rangeList a string representing the list of attributes. Since
   * the string will typically come from a user, attributes are indexed from
   * 1. <br> eg: 
   * first-3,5,6-last
   * @exception Exception if an invalid range list is supplied
   */

  public void setAttributeIndices(String rangeList) throws Exception {

    m_Cols.setRanges(rangeList);
  }

  /**
   * Set which attributes are to be transformed (or kept if invert is true)
   *
   * @param attributes an array containing indexes of attributes to select.
   * Since the array will typically come from a program, attributes are indexed
   * from 0.
   * @exception Exception if an invalid set of ranges is supplied
   */
  public void setAttributeIndicesArray(int [] attributes) throws Exception {

    String rangeList = "";
    for(int i = 0; i < attributes.length; i++)
      if (i == 0)
	rangeList = "" + (attributes[i] + 1);
      else
	rangeList += "," + (attributes[i] + 1);
    setAttributeIndices(rangeList);
  }
  

  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String [] argv) {

    try {
      if (Utils.getFlag('b', argv)) {
	Filter.batchFilterFile(new NumericTransformFilter(), argv);
      } else {
	Filter.filterFile(new NumericTransformFilter(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}








