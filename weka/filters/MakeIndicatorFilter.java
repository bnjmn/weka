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
 * Index of the attribute to be changed. (default last)<p>
 *
 * -V index <br>
 * The value's index. (default last)<p>
 *
 * -N <br>
 * Set if new boolean attribute nominal.<p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz) 
 * @version $Revision: 1.2 $
 *
 */
public class MakeIndicatorFilter extends Filter implements OptionHandler {

  /** The attribute's index option setting. */
  private int m_AttIndexSet = -1;

  /** The value's index option setting. */
  private int m_ValIndexSet = -1;

  /** The attribute's index */
  private int m_AttIndex;

  /** The value's index */
  private int m_ValIndex;
  
  /** Make boolean attribute numeric. */
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
    m_AttIndex = m_AttIndexSet;
    if (m_AttIndex < 0) {
      m_AttIndex = m_InputFormat.numAttributes() - 1;
    }
    m_ValIndex = m_ValIndexSet;
    if (m_ValIndex < 0) {
      m_ValIndex = m_InputFormat.attribute(m_AttIndex).numValues() - 1;
    }
    if (!m_InputFormat.attribute(m_AttIndex).isNominal()) {
      throw new Exception("Chosen attribute not nominal.");
    }
    if (m_InputFormat.attribute(m_AttIndex).numValues() < 2) {
      throw new Exception("Chosen attribute has less than two values.");
    }
    m_NewBatch = true;
    setOutputFormat();
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

    if (m_InputFormat == null) {
      throw new Exception("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }
    Instance newInstance = (Instance)instance.copy();
    if (!newInstance.isMissing(m_AttIndex)) {
      if ((int)newInstance.value(m_AttIndex) == m_ValIndex) {
	newInstance.setValue(m_AttIndex, 1);
      } else {
	newInstance.setValue(m_AttIndex, 0);
      }
    }
    push(newInstance);
    return true;
  }

  /**
   * Returns an enumeration describing the available options
   *
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
              "N", 0, "-N <index>"));

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
	setAttributeIndex(-1);
      } else if (attIndex.toLowerCase().equals("first")) {
	setAttributeIndex(0);
      } else {
	setAttributeIndex(Integer.parseInt(attIndex) - 1);
      }
    } else {
      setAttributeIndex(-1);
    }
    
    String valIndex = Utils.getOption('V', options);
    if (valIndex.length() != 0) {
      if (valIndex.toLowerCase().equals("last")) {
	setValueIndex(-1);
      } else if (valIndex.toLowerCase().equals("first")) {
	setValueIndex(0);
      } else {
	setValueIndex(Integer.parseInt(valIndex) - 1);
      }
    } else {
      setValueIndex(-1);
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

    options[current++] = "-C";
    options[current++] = "" + (getAttributeIndex() + 1);
    options[current++] = "-V"; 
    options[current++] = "" + (getValueIndex() + 1);
    if (!getNumeric()) {
      options[current++] = "-N"; 
    }
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Get the index of the attribute used.
   *
   * @return the index of the attribute
   */
  public int getAttributeIndex() {

    return m_AttIndexSet;
  }

  /**
   * Sets index of of the attribute used.
   *
   * @param index the index of the attribute
   */
  public void setAttributeIndex(int attIndex) {
    
    m_AttIndexSet = attIndex;
  }

  /**
   * Get the index of the first value used.
   *
   * @return the index of the first value
   */
  public int getValueIndex() {

    return m_ValIndexSet;
  }

  /**
   * Sets index of of the first value used.
   *
   * @param index the index of the first value
   */
  public void setValueIndex(int valIndex) {
    
    m_ValIndexSet = valIndex;
  }

  /**
   * Sets if the new Attribute is to be numeric.
   *
   * @param bool true if new Attribute is to be numeric
   */
  public void setNumeric(boolean bool) {

    m_Numeric = bool;
  }

  /**
   * Check if new attribute is to be numeric.
   *
   * @return true if new attribute is to be numeric
   */
  public boolean getNumeric() {

    return m_Numeric;
  }

  /** Set the output format.  */
  private void setOutputFormat() {
    
    try {
      
      Instances newData;
      FastVector newAtts, newVals;
      
      // Compute new attributes
      
      newAtts = new FastVector(m_InputFormat.numAttributes());
      for (int j = 0; j < m_InputFormat.numAttributes(); j++) {
	Attribute att = m_InputFormat.attribute(j);
	if (j != m_AttIndex) {
	  newAtts.addElement(att.copy());
	} else {
	  if (m_Numeric) {
	    newAtts.addElement(new Attribute(att.name()));
	  } else {
	    newVals = new FastVector(2);
	    newVals.addElement("'not_"+removeQuotes(att.value(m_ValIndex))+
			       "'");
	    newVals.addElement("'"+removeQuotes(att.value(m_ValIndex))+"'");
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
 
  /** Removes quotes from a string. */
  private static final String removeQuotes(String string) {

    if (string.endsWith("'") || string.endsWith("\"")) {
      return string.substring(1, string.length() - 1);
    }
    return string;
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
 	Filter.batchFilterFile(new MakeIndicatorFilter(), argv);
      } else {
	Filter.filterFile(new MakeIndicatorFilter(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}








