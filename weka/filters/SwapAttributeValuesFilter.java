/*
 *    SwapAttributeValuesFilter.java
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
 * Swaps two values of a nominal attribute.<p>
 *
 * Valid scheme-specific options are: <p>
 *
 * -C col <br>
 * Index of the attribute to be changed.<p>
 *
 * -F index <br>
 * Index of the first value.<p>
 *
 * -S index <br>
 * Index of the second value.<p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz) 
 * @version 1.0
 */
public class SwapAttributeValuesFilter extends Filter 
  implements OptionHandler {

  // =================
  // Private variables
  // =================

  /** The attribute's index. */
  private int theAttIndex = -1;

  /** The first value's index. */
  private int theFirstIndex = -1;

  /** The second value's index. */
  private int theSecondIndex = -1;

  // ==============
  // Public methods
  // ==============

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
    if (theAttIndex < 0) {
      throw new Exception("No attribute chosen.");
    }
    if (!m_InputFormat.attribute(theAttIndex).isNominal()) {
      throw new Exception("Chosen attribute not nominal.");
    }
    if (m_InputFormat.attribute(theAttIndex).numValues() < 2) {
      throw new Exception("Chosen attribute has less than two values.");
    }
    if (theFirstIndex < 0) {
      throw new Exception("First value undefined.");
    }
    if (theSecondIndex < 0) {
      throw new Exception("Second value undefined.");
    }
    b_NewBatch = true;
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
    if (b_NewBatch) {
      resetQueue();
      b_NewBatch = false;
    }
    Instance newInstance = (Instance)instance.copy();
    if ((int)newInstance.value(theAttIndex) == theSecondIndex) {
      newInstance.setValue(theAttIndex, (double)theFirstIndex);
    } else if ((int)newInstance.value(theAttIndex) == theFirstIndex) {
      newInstance.setValue(theAttIndex, (double)theSecondIndex);
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
              "\tSets the first value's index.",
              "F", 1, "-F <index>"));

    newVector.addElement(new Option(
              "\tSets the second value's index.",
              "S", 1, "-S <index>"));

    return newVector.elements();
  }


  /**
   * Parses the options for this object. Valid options are: <p>
   *
   * -C col <br>
   * Index of the attribute to be changed.<p>
   *
   * -F index <br>
   * Index of the first value.<p>
   *
   * -S index <br>
   * Index of the second value.<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    String attributeIndex = Utils.getOption('C', options);
    if (attributeIndex.length() != 0) {
      if (attributeIndex.toLowerCase().equals("last")) {
	setAttributeIndex(0);
      } else if (attributeIndex.toLowerCase().equals("first")) {
	setAttributeIndex(1);
      } else {
	setAttributeIndex(Integer.parseInt(attributeIndex));
      }
    } else {
      setAttributeIndex(0);
    }
    
    String firstIndex = Utils.getOption('F', options);
    if (firstIndex.length() != 0) { 
      if (firstIndex.toLowerCase().equals("last")) {
	setFirstValueIndex(0);
      } else if (firstIndex.toLowerCase().equals("first")) {
	setFirstValueIndex(1);
      } else {
	setFirstValueIndex(Integer.parseInt(firstIndex));
      }
    } else {
      setFirstValueIndex(0);
    }
     
    String secondIndex = Utils.getOption('S', options);
    if (secondIndex.length() != 0) {
      if (secondIndex.toLowerCase().equals("last")) {
	setSecondValueIndex(0);
      } else if (secondIndex.toLowerCase().equals("first")) {
	setSecondValueIndex(1);
      } else {
	setSecondValueIndex(Integer.parseInt(secondIndex)); 
      }
    } else {
      setSecondValueIndex(0);
    }
   
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

    String [] options = new String [6];
    int current = 0;

    options[current++] = "-C"; options[current++] = "" + getAttributeIndex();
    options[current++] = "-F"; 
    options[current++] = "" + getFirstValueIndex();
    options[current++] = "-S"; 
    options[current++] = "" + getSecondValueIndex();
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Get the index (starting from 1) of the attribute used.
   *
   * @return the index of the attribute
   */
  public int getAttributeIndex() {

    return theAttIndex + 1;
  }

  /**
   * Sets index of (starting from 1) of the attribute used.
   *
   * @param index the index of the attribute
   */
  public void setAttributeIndex(int attIndex) {
    
    theAttIndex = attIndex - 1;
  }

  /**
   * Get the index (starting from 1) of the first value used.
   *
   * @return the index of the first value
   */
  public int getFirstValueIndex() {

    return theFirstIndex + 1;
  }

  /**
   * Sets index of (starting from 1) of the first value used.
   *
   * @param index the index of the first value
   */
  public void setFirstValueIndex(int firstIndex) {
    
    theFirstIndex = firstIndex - 1;
  }

  /**
   * Get the index (starting from 1) of the second value used.
   *
   * @return the index of the second value
   */
  public int getSecondValueIndex() {

    return theSecondIndex + 1;
  }

  /**
   * Sets index of (starting from 1) of the second value used.
   *
   * @param index the index of the second value
   */
  public void setSecondValueIndex(int secondIndex) {
    
    theSecondIndex = secondIndex - 1;
  }

  // ===============
  // Private methods
  // ===============

  /**
   * Set the output format. Takes the current average class values
   * and m_InputFormat and calls setOutputFormat(Instances) 
   * appropriately.
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
	  
	  // Compute list of attribute values
	  
	  newVals = new FastVector(att.numValues());
	  for (int i = 0; i < att.numValues(); i++) {
	    if (i == theFirstIndex) {
	      newVals.addElement(att.value(theSecondIndex));
	    } else if (i == theSecondIndex) {
	      newVals.addElement(att.value(theFirstIndex));
	    } else {
	      newVals.addElement(att.value(i)); 
	    }
	  }
	  newAtts.addElement(new Attribute(att.name(), newVals));
	}
      }
      
      // Construct new header
      
      newData = new Instances(m_InputFormat.relationName(), newAtts,
			      0);
      newData.setClassIndex(m_InputFormat.classIndex());
      setOutputFormat(newData);
    } catch (Exception ex) {
      System.err.println("Problem setting new output format");
      System.exit(0);
    }
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
 	Filter.batchFilterFile(new SwapAttributeValuesFilter(),argv);
      } else {
	Filter.filterFile(new SwapAttributeValuesFilter(),argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}








