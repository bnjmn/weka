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
 *    UselessAttributeFilter.java
 *    Copyright (C) 2002 Richard Kirkby
 *
 */

package weka.filters;

import weka.core.*;
import java.util.Enumeration;
import java.util.Vector;

/** 
 * This filter removes attributes that do not vary at all or that vary too much.
 * All constant attributes are deleted automatically, along with any that exceed
 * the maximum percentage of variance parameter.<p>
 *
 * Valid filter-specific options are: <p>
 *
 * -T type <br>
 * Attribute type to delete.
 * Options are "nominal", "numeric", "string" and "date". (default "string")<p>
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
public class UselessAttributeFilter extends Filter implements OptionHandler {

  /** The type of attribute to delete */
  protected double m_maxVariancePercentage = 100;

  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input instance
   * structure (any instances contained in the object are ignored - only the
   * structure is required).
   * @return true if the outputFormat may be collected immediately
   * @exception Exception if the inputFormat can't be set successfully 
   */ 
  public boolean setInputFormat(Instances instanceInfo) throws Exception {

    super.setInputFormat(instanceInfo);

    return false;
  }

  /**
   * Input an instance for filtering.
   *
   * @param instance the input instance
   * @return true if the filtered instance may now be
   * collected with output().
   */
  public boolean input(Instance instance) {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }

    bufferInput(instance);
    return false;
  }

  /**
   * Signify that this batch of input to the filter is finished.
   *
   * @return true if there are instances pending output
   */  
  public boolean batchFinished() throws Exception {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }

    // do filtering here
    Instances toFilter = getInputFormat();
    int[] attsToDelete = new int[toFilter.numAttributes()];
    int numToDelete = 0;
    for(int i = 0; i < toFilter.numAttributes(); i++) {
      AttributeStats stats = toFilter.attributeStats(i);
      if (stats.distinctCount < 2) {
	// remove constant attributes
	attsToDelete[numToDelete++] = i;
      } else {
	// remove attributes that vary too much
	double variancePercent = (double) stats.distinctCount
	  / (double) stats.totalCount * 100.0;
	if (variancePercent > m_maxVariancePercentage) attsToDelete[numToDelete++] = i;
      }
    }

    int[] finalAttsToDelete = new int[numToDelete];
    System.arraycopy(attsToDelete, 0, finalAttsToDelete, 0, numToDelete);
    
    AttributeFilter attributeFilter = new AttributeFilter();
    attributeFilter.setAttributeIndicesArray(finalAttsToDelete);
    attributeFilter.setInvertSelection(false);
    attributeFilter.setInputFormat(toFilter);
    
    for (int i = 0; i < toFilter.numInstances(); i++) {
      attributeFilter.input(toFilter.instance(i));
    }
    attributeFilter.batchFinished();

    Instance processed;
    Instances outputDataset = attributeFilter.getOutputFormat();
    
    // restore old relation name to hide attribute filter stamp
    outputDataset.setRelationName(toFilter.relationName());
    
    setOutputFormat(outputDataset);
    while ((processed = attributeFilter.output()) != null) {
      processed.setDataset(outputDataset);
      push(processed);
    }

    flushInput();

    m_NewBatch = true;
    return (numPendingOutput() != 0);

  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(1);

    newVector.addElement(new Option(
				    "\tMaximum variance percentage allowed (default 100)",
				    "M", 1, "-M <max variance %>"));


    return newVector.elements();
  }

  /**
   * Parses the options for this object. Valid options are: <p>
   *
   * -T type <br>
   * Attribute type to delete.
   * Options are "nominal", "numeric", "string" and "date". (default "string")<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    String mString = Utils.getOption('M', options);
    if (mString.length() != 0) {
      setMaximumVariancePercentageAllowed((int) Double.valueOf(mString).doubleValue());
    } else {
      setMaximumVariancePercentageAllowed(100.0);
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

    String [] options = new String [2];
    int current = 0;

    options[current++] = "-M";
    options[current++] = "" + getMaximumVariancePercentageAllowed();
    
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "Removes constant attributes, along with attributes to vary too much.";
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String maximumVariancePercentageAllowedTipText() {

    return "Set the threshold for the highest variance allowed before an attribute will be deleted."
      + "Specifically, if (number_of_distinct_values / total_number_of_values * 100)"
      + " is greater than this value then the attribute will be removed.";
  }

  /**
   * Sets the maximum variance attributes are allowed to have before they are
   * deleted by the filter.
   *
   * @param maxVariance the maximum variance allowed, specified as a percentage
   */
  public void setMaximumVariancePercentageAllowed(double maxVariance) {
    
    m_maxVariancePercentage = maxVariance;
  }

  /**
   * Gets the maximum variance attributes are allowed to have before they are
   * deleted by the filter.
   *
   * @return the maximum variance allowed, specified as a percentage
   */
  public double getMaximumVariancePercentageAllowed() {

    return m_maxVariancePercentage;
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String [] argv) {

    try {
      if (Utils.getFlag('b', argv)) {
 	Filter.batchFilterFile(new UselessAttributeFilter(), argv); 
      } else {
	Filter.filterFile(new UselessAttributeFilter(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}
