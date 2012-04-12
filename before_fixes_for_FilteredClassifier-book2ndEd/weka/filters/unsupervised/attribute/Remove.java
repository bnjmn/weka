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
 *    Remove.java
 *    Copyright (C) 1999 Len Trigg
 *
 */


package weka.filters.unsupervised.attribute;

import weka.filters.*;
import java.io.*;
import java.util.*;
import weka.core.*;

/** 
 * An instance filter that deletes a range of attributes from the dataset.<p>
 *
 * Valid filter-specific options are:<p>
 *
 * -R index1,index2-index4,...<br>
 * Specify list of columns to delete. First and last are valid indexes.
 * (default none)<p>
 *
 * -V<br>
 * Invert matching sense (i.e. only keep specified columns)<p>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.4 $
 */
public class Remove extends Filter
  implements UnsupervisedFilter, StreamableFilter, OptionHandler {

  /** Stores which columns to select as a funky range */
  protected Range m_SelectCols = new Range();

  /**
   * Stores the indexes of the selected attributes in order, once the
   * dataset is seen
   */
  protected int [] m_SelectedAttributes;

  /** 
   * Contains an index of string attributes in the input format
   * that will survive the filtering process 
   */
  protected int [] m_InputStringIndex;

  /**
   * Constructor so that we can initialize the Range variable properly.
   */
  public Remove() {
	
    m_SelectCols.setInvert(true);
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
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
   * @exception Exception if the format couldn't be set successfully
   */
  public boolean setInputFormat(Instances instanceInfo) throws Exception {

    super.setInputFormat(instanceInfo);
    
    m_SelectCols.setUpper(instanceInfo.numAttributes() - 1);

    // Create the output buffer
    FastVector attributes = new FastVector();
    int outputClass = -1;
    m_SelectedAttributes = m_SelectCols.getSelection();
    int inStrKeepLen = 0;
    int [] inStrKeep = new int[m_SelectedAttributes.length];
    for (int i = 0; i < m_SelectedAttributes.length; i++) {
      int current = m_SelectedAttributes[i];
      if (instanceInfo.classIndex() == current) {
	outputClass = attributes.size();
      }
      Attribute keep = (Attribute)instanceInfo.attribute(current).copy();
      if (keep.type() == Attribute.STRING) {
        inStrKeep[inStrKeepLen++] = current;
      }
      attributes.addElement(keep);
    }
    m_InputStringIndex = new int [inStrKeepLen];
    System.arraycopy(inStrKeep, 0, m_InputStringIndex, 0, inStrKeepLen);
    Instances outputFormat = new Instances(instanceInfo.relationName(),
					   attributes, 0); 
    outputFormat.setClassIndex(outputClass);
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
   * @exception IllegalStateException if no input structure has been defined.
   */
  public boolean input(Instance instance) {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }

    if (getOutputFormat().numAttributes() == 0) {
      return false;
    }
    double [] vals = new double[getOutputFormat().numAttributes()];
    for (int i = 0; i < m_SelectedAttributes.length; i++) {
      int current = m_SelectedAttributes[i];
      vals[i] = instance.value(current);
    }
    Instance inst = null;
    if (instance instanceof SparseInstance) {
      inst = new SparseInstance(instance.weight(), vals);
    } else {
      inst = new Instance(instance.weight(), vals);
    }
    copyStringValues(inst, false, instance.dataset(), m_InputStringIndex,
                     getOutputFormat(), getOutputStringIndex());
    inst.setDataset(getOutputFormat());
    push(inst);
    return true;
  }

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "An instance filter that removes a range of"
      + " attributes from the dataset.";
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String invertSelectionTipText() {

    return "Determines whether action is to select or delete."
      + " If set to true, only the specified attributes will be kept;"
      + " If set to false, specified attributes will be deleted.";
  }

  /**
   * Get whether the supplied columns are to be removed or kept
   *
   * @return true if the supplied columns will be kept
   */
  public boolean getInvertSelection() {

    return !m_SelectCols.getInvert();
  }

  /**
   * Set whether selected columns should be removed or kept. If true the 
   * selected columns are kept and unselected columns are deleted. If false
   * selected columns are deleted and unselected columns are kept.
   *
   * @param invert the new invert setting
   */
  public void setInvertSelection(boolean invert) {

    m_SelectCols.setInvert(!invert);
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
   * Get the current range selection.
   *
   * @return a string containing a comma separated list of ranges
   */
  public String getAttributeIndices() {

    return m_SelectCols.getRanges();
  }

  /**
   * Set which attributes are to be deleted (or kept if invert is true)
   *
   * @param rangeList a string representing the list of attributes.  Since
   * the string will typically come from a user, attributes are indexed from
   * 1. <br>
   * eg: first-3,5,6-last
   */
  public void setAttributeIndices(String rangeList) {

    m_SelectCols.setRanges(rangeList);
  }

  /**
   * Set which attributes are to be deleted (or kept if invert is true)
   *
   * @param attributes an array containing indexes of attributes to select.
   * Since the array will typically come from a program, attributes are indexed
   * from 0.
   */
  public void setAttributeIndicesArray(int [] attributes) {
    
    setAttributeIndices(Range.indicesToRangeList(attributes));
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String [] argv) {

    try {
      if (Utils.getFlag('b', argv)) {
 	Filter.batchFilterFile(new Remove(), argv); 
      } else {
	Filter.filterFile(new Remove(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}
