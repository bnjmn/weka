/*
 *    MakeIndicatorFilter.java
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
 * Creates a new dataset with a boolean attribute replacing a nominal
 * attribute.  In the new dataset, a value of 1 is assigned to an
 * instance that exhibits a particular attribute value, a 0 to an
 * instance that doesn't. The boolean attribute is coded as numeric by
 * default.<p>
 * 
 * Valid scheme-specific options are: <p>
 *
 * -C col <br>
 * Index of the attribute to be changed.<p>
 *
 * -V index <br>
 * The value's index.<p>
 *
 * -N <br>
 * Set if new boolean attribute nominal.<p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz) 
 * @version 1.0
 *
 */
public class MakeIndicatorFilter extends Filter implements OptionHandler {

  // =================
  // Private variables
  // =================

  /** The attribute's index. */
  private int theAttIndex = -1;

  /** The value's index. */
  private int theValIndex = -1;

  /** Make boolean attribute numeric. */
  private boolean b_Numeric = true;

  // ==============
  // Public methods
  // ==============

  /**
   * Sets the format of the input instances.
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
    if (theAttIndex < 0) {
      throw new Exception("No attribute chosen.");
    }
    if (!m_InputFormat.attribute(theAttIndex).isNominal()) {
      throw new Exception("Chosen attribute not nominal.");
    }
    if (m_InputFormat.attribute(theAttIndex).numValues() < 2) {
      throw new Exception("Chosen attribute has less than two values.");
    }
    if (theValIndex < 0) {
      throw new Exception("Index of value undefined.");
    }
    b_NewBatch = true;
    setOutputFormat();
    return true;
  }

  /**
   * Input an instance for filtering. The instance is processed
   * and made available for output immediately.
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
    if (b_NewBatch) {
      resetQueue();
      b_NewBatch = false;
    }
    Instance newInstance = (Instance)instance.copy();
    if (!newInstance.isMissing(theAttIndex)) {
      if ((int)newInstance.value(theAttIndex) == theValIndex) {
	newInstance.setValue(theAttIndex, 1);
      } else {
	newInstance.setValue(theAttIndex, 0);
      }
    }
    push(newInstance);
    return true;
  }

  /**
   * Returns an enumeration describing the available options
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(3);

    newVector.addElement(new Option(
              "\tSets the attribute index.",
              "C", 1, "-C <col>"));

    newVector.addElement(new Option(
              "\tSets the value's index.",
              "V", 1, "-V <index>"));

    newVector.addElement(new Option(
              "\tSet if new boolean attribute nominal.",
              "N", 0, "-V <index>"));

    return newVector.elements();
  }


  /**
   * Parses the options for this object. Valid options are: <p>
   *
   * -C col <br>
   * Index of the attribute to be changed.<p>
   *
   * -V index <br>
   * The value's index.<p>
   *
   * -N <br>
   * Set if new boolean attribute nominal.<p>
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
      setAttributeIndex(0);
    }
    
    String valIndex = Utils.getOption('V', options);
    if (valIndex.length() != 0) {
      if (valIndex.toLowerCase().equals("last")) {
	setValueIndex(0);
      } else if (valIndex.toLowerCase().equals("first")) {
	setValueIndex(1);
      } else {
	setValueIndex(Integer.parseInt(valIndex));
      }
    } else {
      setValueIndex(0);
    }

    setNumeric(!Utils.getFlag('N', options));

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

    String [] options = new String [5];
    int current = 0;

    options[current++] = "-C"; options[current++] = "" + getAttributeIndex();
    options[current++] = "-V"; 
    options[current++] = "" + getValueIndex();
    if (!getNumeric()) {
      options[current++] = "-N"; 
    }
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Get the index (starting from 1) of the attribute used.
   * @return the index of the attribute
   */
  public int getAttributeIndex() {

    return theAttIndex + 1;
  }

  /**
   * Sets index of (starting from 1) of the attribute used.
   * @param index the index of the attribute
   */
  public void setAttributeIndex(int attIndex) {
    
    theAttIndex = attIndex - 1;
  }

  /**
   * Get the index (starting from 1) of the first value used.
   * @return the index of the first value
   */
  public int getValueIndex() {

    return theValIndex + 1;
  }

  /**
   * Sets index of (starting from 1) of the first value used.
   * @param index the index of the first value
   */
  public void setValueIndex(int valIndex) {
    
    theValIndex = valIndex - 1;
  }

  /**
   * Sets if the new Attribute is to be numeric.
   * @param bool true if new Attribute is to be numeric
   */
  public void setNumeric(boolean bool) {

    b_Numeric = bool;
  }

  /**
   * Check if new attribute is to be numeric.
   * @return true if new attribute is to be numeric
   */
  public boolean getNumeric() {

    return b_Numeric;
  }

  // ===============
  // Private methods
  // ===============

  /**
   * Set the output format. 
   */
  private void setOutputFormat() {
    
    try {
      
      Instances newData;
      FastVector newAtts, newVals;
      
      // Compute new attributes
      
      newAtts = new FastVector(m_InputFormat.numAttributes());
      for (int j = 0; j < m_InputFormat.numAttributes(); j++) {
	Attribute att = m_InputFormat.attribute(j);
	if (j != theAttIndex) {
	  newAtts.addElement(att.copy());
	} else {
	  if (b_Numeric) {
	    newAtts.addElement(new Attribute(att.name()));
	  } else {
	    newVals = new FastVector(2);
	    newVals.addElement("'not_"+removeQuotes(att.value(theValIndex))+
			       "'");
	    newVals.addElement("'"+removeQuotes(att.value(theValIndex))+"'");
	    newAtts.addElement(new Attribute(att.name(), newVals));
	  }
	}
      }
      
      // Construct new header
      
      newData = new Instances(m_InputFormat.relationName(), newAtts, 0);
      newData.setClassIndex(m_InputFormat.classIndex());
      setOutputFormat(newData);
    } catch (Exception ex) {
      System.err.println("Problem setting new output format");
      System.exit(0);
    }
  }
 
  /**
   * Removes quotes from a string.
   */
  private static final String removeQuotes(String string) {

    if (string.endsWith("'") || string.endsWith("\"")) {
      return string.substring(1, string.length()-1);
    }
    return string;
  }
 
  // ============
  // Test method.
  // ============

  /**
   * Main method for testing this class.
   * @param argv should contain arguments to the filter: 
   * use -h for help
   */

  public static void main(String [] argv) {

    try {
      if (Utils.getFlag('b', argv)) {
 	Filter.batchFilterFile(new MakeIndicatorFilter(),argv);
      } else {
	Filter.filterFile(new MakeIndicatorFilter(),argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}








