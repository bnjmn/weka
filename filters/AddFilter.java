/*
 *    AddFilter.java
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
 * An instance filter that adds a new attribute to the dataset. 
 * The new attribute contains all missing values.<p>
 *
 * Valid filter-specific options are:<p>
 *
 * -C index <br>
 * Specify where to insert the column. First and last are valid indexes.
 * (default last)<p>
 *
 * -L label1,label2,...<br>
 * Create nominal attribute with the given labels
 * (default numeric attribute)<p>
 *
 * -N name<br>
 * Name of the new attribute. (default = 'Unnamed')<p>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.6 $
 */
public class AddFilter extends Filter implements OptionHandler {

  /** Record the type of attribute to insert */
  protected int m_AttributeType = Attribute.NUMERIC;

  /** The name for the new attribute */
  protected String m_Name = "unnamed";

  /** The location to insert the new attribute */
  protected int m_Insert = -1;

  /** The list of labels for nominal attribute */
  protected FastVector m_Labels = new FastVector(5);

  /**
   * Returns a string describing this filter
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "AddFilter: \n\nAn instance filter that adds a new attribute to "
      +"the dataset.\n"
      +"The new attribute contains all missing values.\n";
  }

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(3);

    newVector.addElement(new Option(
              "\tSpecify where to insert the column. First and last\n"
	      +"\tare valid indexes.(default last)",
              "C", 1, "-C <index>"));
    newVector.addElement(new Option(
	      "\tCreate nominal attribute with given labels\n"
	      +"\t(default numeric attribute)",
              "L", 1, "-L <label1,label2,...>"));
    newVector.addElement(new Option(
              "\tName of the new attribute.\n"
              +"\t(default = 'Unnamed')",
              "N", 1,"-N <name>"));

    return newVector.elements();
  }


  /**
   * Parses a list of options for this object. Valid options are:<p>
   *
   * -C index <br>
   * Specify where to insert the column. First and last are valid indexes.
   * (default last)<p>
   *
   * -L label1,label2,...<br>
   * Create nominal attribute with the given labels
   * (default numeric attribute)<p>
   *
   * -N name<br>
   * Name of the new attribute. (default = 'Unnamed')<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    String insertString = Utils.getOption('C', options);
    if (insertString.length() != 0) {
      if (insertString.toLowerCase().equals("last")) {
	setAttributeIndex(-1);
      } else if (insertString.toLowerCase().equals("first")) {
	setAttributeIndex(0);
      } else {
	setAttributeIndex(Integer.parseInt(insertString) - 1); 
      }
    }

    setNominalLabels(Utils.getOption('L', options));
    setAttributeName(Utils.getOption('N', options));

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

    options[current++] = "-N"; options[current++] = getAttributeName();
    if (m_AttributeType == Attribute.NOMINAL) {
      options[current++] = "-L"; options[current++] = getNominalLabels();
    }
    options[current++] = "-C";
    options[current++] = "" + (getAttributeIndex() + 1);

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
   * @exception Exception if the format couldn't be set successfully
   */
  public boolean inputFormat(Instances instanceInfo) throws Exception {

    m_InputFormat = new Instances(instanceInfo, 0);
    m_NewBatch = true;

    Instances outputFormat = new Instances(instanceInfo, 0);
    Attribute newAttribute = null;
    switch (m_AttributeType) {
    case Attribute.NUMERIC:
      newAttribute = new Attribute(m_Name);
      break;
    case Attribute.NOMINAL:
      newAttribute = new Attribute(m_Name, m_Labels);
      break;
    default:
      throw new Error("Unknown attribute type in AddFilter");
    }

    if ((m_Insert < 0) || (m_Insert > m_InputFormat.numAttributes())) {
      m_Insert = m_InputFormat.numAttributes();
    }
    outputFormat.insertAttributeAt(newAttribute, m_Insert);
    setOutputFormat(outputFormat);
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
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }

    double [] newVals = new double[outputFormatPeek().numAttributes()];
    // Copy the existing values over
    int i, j;
    for(i = 0, j = 0; i < m_InputFormat.numAttributes(); i++, j++) {
      if (i == m_Insert) {
	j++;
      }
      newVals[j] = instance.value(i);
    }
    push(new Instance(instance.weight(), newVals));
    return true;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String attributeNameTipText() {
    return "Set the new attribute's name";
  }

  /**
   * Get the name of the attribute to be created
   *
   * @return the new attribute name
   */
  public String getAttributeName() {

    return m_Name;
  }

  /** 
   * Set the new attribute's name
   *
   * @param name the new name
   */
  public void setAttributeName(String name) {

    String newName = name.trim();
    if (newName.indexOf(' ') >= 0) {
      if (newName.indexOf('\'') != 0) {
	newName = newName.replace('\'',' ');
      }
      newName = '\'' + newName + '\'';
    }
    if (newName.equals("")) {
      newName = "unnamed";
    }
    m_Name = newName;
    
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String attributeIndexTipText() {
    return "The index where the attribute will be inserted";
  }

  /**
   * Get the index where the attribute will be inserted
   *
   * @return the attribute insertion index
   */
  public int getAttributeIndex() {

    return m_Insert;
  }

  /**
   * Set the index where the attribute will be inserted
   *
   * @param attributeIndex the insertion index (-1 means last)
   */
  public void setAttributeIndex(int attributeIndex) {
    m_Insert = attributeIndex;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String nominalLabelsTipText() {
    return "The list of labels for nominal attribute creation";
  }

  /**
   * Get the list of labels for nominal attribute creation
   *
   * @return the list of labels for nominal attribute creation
   */
  public String getNominalLabels() {

    String labelList = "";
    for(int i = 0; i < m_Labels.size(); i++) {
      if (i == 0) {
	labelList = (String)m_Labels.elementAt(i);
      } else {
	labelList += "," + (String)m_Labels.elementAt(i); 
      }
    }
    return labelList;
  }

  /**
   * Set the labels for nominal attribute creation.
   *
   * @param labelList a comma separated list of labels
   * @exception Exception if the labelList was invalid
   */
  public void setNominalLabels(String labelList) throws Exception {

    FastVector labels = new FastVector (10);

    // Split the labelList up into the vector
    int commaLoc;
    while ((commaLoc = labelList.indexOf(',')) >= 0) {
      String label = labelList.substring(0, commaLoc).trim();
      if (!label.equals("")) {
	labels.addElement(label);
      } else {
	throw new Exception("Invalid label list at "+
			    labelList.substring(commaLoc));
      }
      labelList = labelList.substring(commaLoc + 1);
    }
    String label = labelList.trim();
    if (!label.equals("")) {
      labels.addElement(label);
    }

    // If everything is OK, make the type change
    m_Labels = labels;
    if (labels.size() == 0) {
      m_AttributeType = Attribute.NUMERIC;
    } else {
      m_AttributeType = Attribute.NOMINAL; 
    }
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String [] argv) {

    try {
      if (Utils.getFlag('b', argv)) {
 	Filter.batchFilterFile(new AddFilter(), argv);
      } else {
	Filter.filterFile(new AddFilter(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}








