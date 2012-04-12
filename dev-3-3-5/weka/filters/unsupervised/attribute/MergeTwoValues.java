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
 *    MergeTwoValues.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */


package weka.filters.unsupervised.attribute;

import weka.filters.*;
import java.io.*;
import java.util.*;
import weka.core.*;

/** 
 * Merges two values of a nominal attribute.<p>
 * 
 * Valid filter-specific options are: <p>
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
 * @author Eibe Frank (eibe@cs.waikato.ac.nz) 
 * @version $Revision: 1.1 $
 */
public class MergeTwoValues extends Filter
  implements UnsupervisedFilter, StreamableFilter, OptionHandler {

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
  public boolean setInputFormat(Instances instanceInfo) 
       throws Exception {

    super.setInputFormat(instanceInfo);
    m_AttIndex = m_AttIndexSet;
    if (m_AttIndex < 0) {
      m_AttIndex = instanceInfo.numAttributes() - 1;
    }
    m_FirstIndex = m_FirstIndexSet;
    if (m_FirstIndex < 0) {
      m_FirstIndex = instanceInfo.attribute(m_AttIndex).numValues() - 1;
    }
    m_SecondIndex = m_SecondIndexSet;
    if (m_SecondIndex < 0) {
      m_SecondIndex = instanceInfo.attribute(m_AttIndex).numValues() - 1;
    }
    if (!instanceInfo.attribute(m_AttIndex).isNominal()) {
      throw new UnsupportedAttributeTypeException("Chosen attribute not nominal.");
    }
    if (instanceInfo.attribute(m_AttIndex).numValues() < 2) {
      throw new UnsupportedAttributeTypeException("Chosen attribute has less than two values.");
    }
    if (m_SecondIndex <= m_FirstIndex) {
      // XXX Maybe we should just swap the values??
      throw new Exception("The second index has to be greater "+
			  "than the first.");
    }
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
   * @exception IllegalStateException if no input format has been set.
   */
  public boolean input(Instance instance) {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }
    Instance newInstance = (Instance)instance.copy();
    if ((int)newInstance.value(m_AttIndex) == m_SecondIndex) {
      newInstance.setValue(m_AttIndex, (double)m_FirstIndex);
    }
    else if ((int)newInstance.value(m_AttIndex) > m_SecondIndex) {
      newInstance.setValue(m_AttIndex,
			   newInstance.value(m_AttIndex) - 1);
    }
    push(newInstance);
    return true;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
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
      setFirstValueIndex(0);
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
   
    if (getInputFormat() != null) {
      setInputFormat(getInputFormat());
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
   */
  private void setOutputFormat() {
    
    Instances newData;
    FastVector newAtts, newVals;
    boolean firstEndsWithPrime = false, 
      secondEndsWithPrime = false;
    StringBuffer text = new StringBuffer();
      
    // Compute new attributes
      
    newAtts = new FastVector(getInputFormat().numAttributes());
    for (int j = 0; j < getInputFormat().numAttributes(); j++) {
      Attribute att = getInputFormat().attribute(j);
      if (j != m_AttIndex) {
	newAtts.addElement(att.copy());
      } else {
	  
	// Compute new value
	  
	if (att.value(m_FirstIndex).endsWith("'")) {
	  firstEndsWithPrime = true;
	}
	if (att.value(m_SecondIndex).endsWith("'")) {
	  secondEndsWithPrime = true;
	}
	if (firstEndsWithPrime || secondEndsWithPrime) {
	  text.append("'");
	}
	if (firstEndsWithPrime) {
	  text.append(((String)att.value(m_FirstIndex)).
		      substring(1, ((String)att.value(m_FirstIndex)).
				length() - 1));
	} else {
	  text.append((String)att.value(m_FirstIndex));
	}
	text.append('_');
	if (secondEndsWithPrime) {
	  text.append(((String)att.value(m_SecondIndex)).
		      substring(1, ((String)att.value(m_SecondIndex)).
				length() - 1));
	} else {
	  text.append((String)att.value(m_SecondIndex));
	}
	if (firstEndsWithPrime || secondEndsWithPrime) {
	  text.append("'");
	}
	  
	// Compute list of attribute values
	  
	newVals = new FastVector(att.numValues() - 1);
	for (int i = 0; i < att.numValues(); i++) {
	  if (i == m_FirstIndex) {
	    newVals.addElement(text.toString());
	  } else if (i != m_SecondIndex) {
	    newVals.addElement(att.value(i));
	  }
	}
	newAtts.addElement(new Attribute(att.name(), newVals));
      }
    }
      
    // Construct new header
      
    newData = new Instances(getInputFormat().relationName(), newAtts,
			    0);
    newData.setClassIndex(getInputFormat().classIndex());
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
 	Filter.batchFilterFile(new MergeTwoValues(), argv);
      } else {
	Filter.filterFile(new MergeTwoValues(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}








