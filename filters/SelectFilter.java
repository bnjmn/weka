/*
 *    SelectFilter.java
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
 * Filters instances according to the value of an attribute.<p>
 *
 * Valid scheme-specific options are:<p>
 *
 * -C <num><br>
 * Choose attribute to be used for selection.<p>
 *
 * -S <num><br>
 * Numeric value to be used for selection on numeric attribute.
 * (Instances with values smaller than given value.) <p>
 *
 * -L <index1,index2-index4,...><br>
 * Range of label indices to be used for selection on nominal attribute.
 * (First and last are valid indexes.)<p>
 *
 * -V<br>
 * Invert matching sense.<p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */

public class SelectFilter extends Filter implements OptionHandler {

  // =================
  // Protected members
  // =================

  /** Stores which attribute to be used for filtering */
  protected int m_Attribute = -1;

  /** Stores which values of nominal attribute are to be used for filtering.*/
  protected Range m_Values = new Range();

  /** Stores which value of a numeric attribute is to be used for filtering.*/
  protected double m_Value = 0;

  /** Inverse of test to be used? */
  protected boolean m_Inverse = false;

  // ===============
  // Public methods.
  // ===============

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(4);

    newVector.addElement(new Option(
              "\tChoose attribute to be used for selection.",
              "C", 1, "-C <num>"));
    newVector.addElement(new Option(
              "\tNumeric value to be used for selection on numeric\n"+
	      "\tattribute.\n"+
	      "\t(Instances with values smaller than given value.)",
              "S", 1, "-S <num>"));
    newVector.addElement(new Option(
              "\tRange of label indices to be used for selection on\n"+
	      "\tnominal attribute.\n"+
	      "\t(First and last are valid indexes.)",
              "L", 1, "-L <index1,index2-index4,...>"));
    newVector.addElement(new Option(
	      "\tInvert matching sense.",
              "V", 0, "-V"));

    return newVector.elements();
  }


  /**
   * Parses a given list of options.
   * Valid options are:<p>
   *
   * -C <num><br>
   * Choose attribute to be used for selection.<p>
   *
   * -S <num><br>
   * Numeric value to be used for selection on numeric attribute.
   * (Instances with values smaller than given value.) <p>
   *
   * -L <index1,index2-index4,...><br>
   * Range of label indices to be used for selection on nominal attribute.
   * (First and last are valid indexes.)<p>
   *
   * -V<br>
   * Invert matching sense.<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String attIndex = Utils.getOption('C', options);
    if (attIndex.length() != 0) {
      if (attIndex.toLowerCase().equals("last")) {
	setAttributeIndex(0);
      } else if (attIndex.toLowerCase().equals("first")) {
        setAttributeIndex(1);
      } else {
	setAttributeIndex(Integer.parseInt(attIndex));
      }
    } else {
      setAttributeIndex(-1);
    }

    String splitPoint = Utils.getOption('S', options);
    if (splitPoint.length() != 0) {
      setSplitPoint((new Double(splitPoint)).doubleValue());
    } else {
      setSplitPoint(0);
    }

    String convertList = Utils.getOption('L', options);
    if (convertList.length() != 0) {
      setNominalIndices(convertList);
    }
    setInvertSelection(Utils.getFlag('V', options));

    // Re-initialize output format according to new options
    
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

    options[current++] = "-S"; options[current++] = "" + getSplitPoint();
    options[current++] = "-C"; options[current++] = "" + getAttributeIndex();
    if (!getNominalIndices().equals("")) {
      options[current++] = "-L"; options[current++] = getNominalIndices();
    }
    if (getInvertSelection()) {
      options[current++] = "-V";
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
   * @exception Exception if selection is not possible
   */
  public boolean inputFormat(Instances instanceInfo) throws Exception {

    m_InputFormat = new Instances(instanceInfo, 0);
    if (m_Attribute == -1) {
      setAttributeIndex(m_InputFormat.numAttributes());
    }
    if (!isNumeric() && !isNominal()) {
      throw new Exception("Can only handle numeric or nominal "+ 
			  "attributes.");
    }
    m_Values.setUpper(m_InputFormat.attribute(m_Attribute).numValues() - 1);
    setOutputFormat(m_InputFormat);
    b_NewBatch = true;
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
    if (instance.isMissing(m_Attribute)) {
      return false;
    }
    if (isNumeric()) {
      if (!m_Inverse) {
	if (Utils.sm(instance.value(m_Attribute), m_Value)) {
	  push((Instance)instance.copy());
	  return true;
	} 
      } else {
	if (Utils.grOrEq(instance.value(m_Attribute), m_Value)) {
	  push((Instance)instance.copy());
	  return true;
	} 
      }
    }
    if (isNominal()) {
      if (m_Values.isInRange((int)instance.value(m_Attribute))) {
	  push((Instance)instance.copy());
	return true;
      }
    }
    return false;
  }

  /** 
   * Returns true if selection attribute is nominal.
   *
   * @return true if selection attribute is nominal
   */
  public boolean isNominal() {
    
    if (m_InputFormat == null) {
      return false;
    } else {
      return m_InputFormat.attribute(m_Attribute).isNominal();
    }
  }

  /** 
   * Returns true if selection attribute is numeric.
   *
   * @return true if selection attribute is numeric
   */
  public boolean isNumeric() {
    
    if (m_InputFormat == null) {
      return false;
    } else {
      return m_InputFormat.attribute(m_Attribute).isNumeric();
    }
  }
  
  /**
   * Get the attribute to be used for selection
   *
   * @return the attribute index
   */
  public int getAttributeIndex() {

    return m_Attribute + 1;
  }

  /**
   * Sets attribute to be used for selection
   *
   * @param attribute the attribute's index (starting from 1, or 0 for last);
   */
  public void setAttributeIndex(int attribute) {

    m_Attribute = attribute - 1;
  }

  /**
   * Get the split point used for numeric selection
   *
   * @return the numeric split point
   */
  public double getSplitPoint() {

    return m_Value;
  }

  /**
   * Split point to be used for selection on numeric attribute.
   *
   * @param value the split point
   */
  public void setSplitPoint(double value) {

    m_Value = value;
  }

  /**
   * Get whether the supplied columns are to be removed or kept
   *
   * @return true if the supplied columns will be kept
   */
  public boolean getInvertSelection() {

    return m_Values.getInvert();
  }

  /**
   * Set whether selected values should be removed or kept. If true the 
   * selected values are kept and unselected values are deleted. 
   *
   * @param invert the new invert setting
   */
  public void setInvertSelection(boolean invert) {

    m_Inverse = invert;
    m_Values.setInvert(invert);
  }

  /**
   * Get the set of nominal value indices that will be used for selection
   *
   * @return rangeList a string representing the list of nominal indices.
   */
  public String getNominalIndices() {

    return m_Values.getRanges();
  }

  /**
   * Set which nominal labels are to be included in the selection.
   *
   * @param rangeList a string representing the list of nominal indices.
   * eg: first-3,5,6-last
   * @exception Exception if an invalid range list is supplied
   */
  public void setNominalIndices(String rangeList) throws Exception {
    
    m_Values.setRanges(rangeList);
  }

  /**
   * Set which values of a nominal attribute are to be used for
   * selection.
   *
   * @param values an array containing indexes of values to be
   * used for selection
   * @exception Exception if an invalid set of ranges is supplied
   */
  public void setNominalIndicesArr(int [] values) throws Exception {

    String rangeList = "";
    for(int i = 0; i < values.length; i++) {
      if (i == 0) {
	rangeList = ""+(values[i]+1);
      } else {
	rangeList += ","+(values[i]+1);
      }
    }
    setNominalIndices(rangeList);
  }

  // ============
  // Test method.
  // ============

  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: 
   * use -h for help
   */
  public static void main(String [] argv) {

    try {
      if (Utils.getFlag('b', argv)) {
 	Filter.batchFilterFile(new SelectFilter(),argv);
      } else {
	Filter.filterFile(new SelectFilter(),argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}








