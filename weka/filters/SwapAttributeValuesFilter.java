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
 * Valid filter-specific options are: <p>
 *
 * -C col <br>
 * Index of the attribute to be changed. (default last)<p>
 *
 * -F index <br>
 * Index of the first value (default first).<p>
 *
 * -S index <br>
 * Index of the second value (default last).<p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz) 
 * @version $Revision: 1.5 $
 */
public class SwapAttributeValuesFilter extends Filter 
  implements OptionHandler {

  /** The attribute's index setting. */
  private int m_AttIndexSet = -1; 

  /** The first value's index setting. */
  private int m_FirstIndexSet = 0;

  /** The second value's index setting. */
  private int m_SecondIndexSet = -1;

  /** The attribute's index. */
  private int m_AttIndex; 

  /** The first value's index. */
  private int m_FirstIndex;

  /** The second value's index. */
  private int m_SecondIndex;

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
    m_FirstIndex = m_FirstIndexSet;
    if (m_FirstIndex < 0) {
      m_FirstIndex = m_InputFormat.attribute(m_AttIndex).numValues() - 1;
    }
    m_SecondIndex = m_SecondIndexSet;
    if (m_SecondIndex < 0) {
      m_SecondIndex = m_InputFormat.attribute(m_AttIndex).numValues() - 1;
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
    if ((int)newInstance.value(m_AttIndex) == m_SecondIndex) {
      newInstance.setValue(m_AttIndex, (double)m_FirstIndex);
    } else if ((int)newInstance.value(m_AttIndex) == m_FirstIndex) {
      newInstance.setValue(m_AttIndex, (double)m_SecondIndex);
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
              "\tSets the attribute index (default last).",
              "C", 1, "-C <col>"));

    newVector.addElement(new Option(
              "\tSets the first value's index (default first).",
              "F", 1, "-F <value index>"));

    newVector.addElement(new Option(
              "\tSets the second value's index (default last).",
              "S", 1, "-S <value index>"));

    return newVector.elements();
  }


  /**
   * Parses the options for this object. Valid options are: <p>
   *
   * -C col <br>
   * The column containing the values to be merged. (default last)<p>
   *
   * -F index <br>
   * Index of the first value (default first).<p>
   *
   * -S index <br>
   * Index of the second value (default last).<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    String attributeIndex = Utils.getOption('C', options);
    if (attributeIndex.length() != 0) {
      if (attributeIndex.toLowerCase().equals("last")) {
	setAttributeIndex(-1);
      } else if (attributeIndex.toLowerCase().equals("first")) {
	setAttributeIndex(0);
      } else {
	setAttributeIndex(Integer.parseInt(attributeIndex) - 1);
      }
    } else {
      setAttributeIndex(-1);
    }
    
    String firstIndex = Utils.getOption('F', options);
    if (firstIndex.length() != 0) { 
      if (firstIndex.toLowerCase().equals("last")) {
	setFirstValueIndex(-1);
      } else if (firstIndex.toLowerCase().equals("first")) {
	setFirstValueIndex(0);
      } else {
	setFirstValueIndex(Integer.parseInt(firstIndex) - 1);
      }
    } else {
      setFirstValueIndex(-1);
    }
     
    String secondIndex = Utils.getOption('S', options);
    if (secondIndex.length() != 0) {
      if (secondIndex.toLowerCase().equals("last")) {
	setSecondValueIndex(-1);
      } else if (secondIndex.toLowerCase().equals("first")) {
	setSecondValueIndex(0);
      } else {
	setSecondValueIndex(Integer.parseInt(secondIndex) - 1); 
      }
    } else {
      setSecondValueIndex(-1);
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

    options[current++] = "-C";
    options[current++] = "" + (getAttributeIndex() + 1);
    options[current++] = "-F"; 
    options[current++] = "" + (getFirstValueIndex() + 1);
    options[current++] = "-S"; 
    options[current++] = "" + (getSecondValueIndex() + 1);
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
   * Sets index of the attribute used.
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
  public int getFirstValueIndex() {

    return m_FirstIndexSet;
  }

  /**
   * Sets index of the first value used.
   *
   * @param index the index of the first value
   */
  public void setFirstValueIndex(int firstIndex) {
    
    m_FirstIndexSet = firstIndex;
  }

  /**
   * Get the index of the second value used.
   *
   * @return the index of the second value
   */
  public int getSecondValueIndex() {

    return m_SecondIndexSet;
  }

  /**
   * Sets index of the second value used.
   *
   * @param index the index of the second value
   */
  public void setSecondValueIndex(int secondIndex) {
    
    m_SecondIndexSet = secondIndex;
  }

  /**
   * Set the output format. Takes the current average class values
   * and m_InputFormat and calls setOutputFormat(Instances) 
   * appropriately.
   *
   * @exception Exception if a problem occurs when setting the output format
   */
  private void setOutputFormat() throws Exception {
    
    Instances newData;
    FastVector newAtts, newVals;
      
    // Compute new attributes
      
    newAtts = new FastVector(m_InputFormat.numAttributes());
    for (int j = 0; j < m_InputFormat.numAttributes(); j++) {
      Attribute att = m_InputFormat.attribute(j);
      if (j != m_AttIndex) {
	newAtts.addElement(att.copy()); 
      } else {
	  
	// Compute list of attribute values
	  
	newVals = new FastVector(att.numValues());
	for (int i = 0; i < att.numValues(); i++) {
	  if (i == m_FirstIndex) {
	    newVals.addElement(att.value(m_SecondIndex));
	  } else if (i == m_SecondIndex) {
	    newVals.addElement(att.value(m_FirstIndex));
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
 	Filter.batchFilterFile(new SwapAttributeValuesFilter(), argv);
      } else {
	Filter.filterFile(new SwapAttributeValuesFilter(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}








