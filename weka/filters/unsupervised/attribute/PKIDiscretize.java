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
 *    PKIDiscretize.java
 *
 *    http://www.cm.deakin.edu.au/webb
 *
 *    Copyright (C) 2001 Deakin University
 *    School of Computing and Mathematics
 *    Deakin University
 *    Geelong, Vic, 3217, Australia
 *
 */

package weka.filters.unsupervised.attribute;

import weka.filters.*;
import java.io.*;
import java.util.*;
import weka.core.*;

/** 
 *
 * A filter that discretizes a range of numeric attributes in the dataset into
 * nominal attributes, by means of two methods:
 *
 * Calculate Cut Points By Data Bin : Set cutpoints for a single attribute
 * determined by data bin. If a given ideal cutpoint cannot be set (neighbouring
 * data is the same),  the algorithm attempts to "stepback" ten percent. If this
 * smaller bin can't be set, the loop cycles through until a cut can be made.<p>
 *  
 * Calculate Cut Points By PKID : Set cutpoints for a single attribute determined by
 * PKID.  PKID adjusts the number of cutpoints to the number of data objects. This
 * is more appropriate for Naive Bayes than traditional discretization  techniques
 * which seek to develop few cutpoints in order to avoid  the fragmentation problem.<p>
 *
 * For more information see: <p>
 *
 * Y. Yang and G. Webb, (2001). Proportional k-Interval Discretization for Naive-Bayes Classifiers. 12th European Conference on Machine Learning (ECML01). <p>
 *
 * Valid filter-specific options are: <p>
 *
 * -B num <br>
 * Specify the number of bins to divide numeric attributes into.
 * (for bin-based discretisation, default = PKID).<p>
 *
 * -R col1,col2-col4,... <br>
 * Specify list of columns to Discretize. First
 * and last are valid indexes. (default none) <p>
 *
 * -V <br>
 * Invert matching sense.<p>
 *
 * -D <br>
 * Make binary nominal attributes. <p>
 *
 * @author Deakin University http://www.cm.deakin.edu.au/webb (webb@deakin.edu.au)
 * @author Richard Kirkby
 * @version $Revision: 1.3 $
 */
public class PKIDiscretize extends Filter 
  implements UnsupervisedFilter, OptionHandler, WeightedInstancesHandler {

  /** Stores which columns to Discretize */
  protected Range m_DiscretizeCols = new Range();

  /** The number of bins to divide the attribute into */
  protected int m_NumBins = 10;

  /** Store the current cutpoints */
  protected double [][] m_CutPoints = null;

  /** Output binary attributes for discretized attributes. */
  protected boolean m_MakeBinary = false;
  
  /** Use data bin method */
  protected boolean m_UseDataBin = false;

  /** Constructor - initialises the filter */
  public PKIDiscretize() {

    setAttributeIndices("first-last");
  }

  /**
   * Gets an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(9);

    newVector.addElement(new Option(
              "\tSpecify the number of bins to divide numeric attributes into "
	      + "(for bin-based discretization).\n"
	      + "\t(default PKID)",
              "B", 1, "-B <num>"));

    newVector.addElement(new Option(
              "\tSpecify list of columns to Discretize. First"
	      + " and last are valid indexes.\n"
	      + "\t(default none)",
              "R", 1, "-R <col1,col2-col4,...>"));

    newVector.addElement(new Option(
              "\tInvert matching sense of column indexes.",
              "V", 0, "-V"));

    newVector.addElement(new Option(
              "\tOutput binary attributes for discretized attributes.",
              "D", 0, "-D"));
    
    return newVector.elements();
  }


  /**
   * Parses the options for this object. Valid options are: <p>
   *
   * -B num <br>
   * Specify the number of bins to divide numeric attributes into.
   * (for bin-based discretisation, default = PKID).<p>
   *
   * -R col1,col2-col4,... <br>
   * Specify list of columns to discretize. First
   * and last are valid indexes. (default none) <p>
   *
   * -V <br>
   * Invert matching sense.<p>
   *
   * -D <br>
   * Make binary nominal attributes. <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    setMakeBinary(Utils.getFlag('D', options));
    setInvertSelection(Utils.getFlag('V', options));
    setUseDataBin(Utils.getFlag('M', options));
    String numBins = Utils.getOption('B', options);
    if (numBins.length() != 0) {
      setBins(Integer.parseInt(numBins));
      setUseDataBin(true);
    } else {
      setUseDataBin(false);
    }
    
    String convertList = Utils.getOption('R', options);
    if (convertList.length() != 0) {
      setAttributeIndices(convertList);
    } else {
      setAttributeIndices("first-last");
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

    String [] options = new String [11];
    int current = 0;

    if (getMakeBinary()) {
      options[current++] = "-D";
    }
    if (getInvertSelection()) {
      options[current++] = "-V";
    }
    if (!getAttributeIndices().equals("")) {
      options[current++] = "-R"; options[current++] = getAttributeIndices();
    }
    if (getUseDataBin()) {
      options[current++] = "-B"; options[current++] = "" + getBins();
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
   * @exception Exception if the input format can't be set successfully
   */
  public boolean setInputFormat(Instances instanceInfo) throws Exception {

    super.setInputFormat(instanceInfo);

    m_DiscretizeCols.setUpper(instanceInfo.numAttributes() - 1);
    m_CutPoints = null;

    // If we implement loading cutfiles, then load 
    //them here and set the output format
    return false;
  }

  /**
   * Input an instance for filtering. Ordinarily the instance is processed
   * and made available for output immediately. Some filters require all
   * instances be read before producing output.
   *
   * @param instance the input instance
   * @return true if the filtered instance may now be
   * collected with output().
   * @exception IllegalStateException if no input format has been defined.
   */
  public boolean input(Instance instance) {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }
    
    if (m_CutPoints != null) {
      convertInstance(instance);
      return true;
    }

    bufferInput(instance);
    return false;
  }


  /**
   * Signifies that this batch of input to the filter is finished. If the 
   * filter requires all instances prior to filtering, output() may now 
   * be called to retrieve the filtered instances.
   *
   * @return true if there are instances pending output
   * @exception IllegalStateException if no input structure has been defined
   */
  public boolean batchFinished() {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_CutPoints == null) {
      calculateCutPoints();

      setOutputFormat();

      // If we implement saving cutfiles, save the cuts here

      // Convert pending input instances
      for(int i = 0; i < getInputFormat().numInstances(); i++) {
	convertInstance(getInputFormat().instance(i));
      }
    } 
    flushInput();

    m_NewBatch = true;
    return (numPendingOutput() != 0);
  }

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "A filter that discretizes a range of numeric attributes"
      + " in the dataset into nominal attributes, by means of"
      + " data bin or PKID.";
  }
    
  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String makeBinaryTipText() {

    return "Make resulting attributes binary.";
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String useDataBinTipText() {

    return "Use data bin discretization. If false, will use PKID.";
  }
  
  /**
   * Gets whether binary attributes should be made for discretized ones.
   *
   * @return true if attributes will be binarized
   */
  public boolean getMakeBinary() {

    return m_MakeBinary;
  }

  /** 
   * Sets whether binary attributes should be made for discretized ones.
   *
   * @param makeBinary if binary attributes are to be made
   */
  public void setMakeBinary(boolean makeBinary) {

    m_MakeBinary = makeBinary;
  }

  /** 
   * Sets whether Data Bin will be used as the discretisation method.
   *
   * @param useDataBin true if Data Bin should be used.
   */
  public void setUseDataBin(boolean useDataBin) {

    m_UseDataBin = useDataBin;
  }
  
  /**
   * Gets whether using Data Bin.
   *
   * @return true if DataBin will be used
   */
  public boolean getUseDataBin() {

    return m_UseDataBin;
  } 

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String binsTipText() {

    return "Number of bins for data-binning method.";
  }

  /**
   * Gets the number of bins numeric attributes will be divided into
   *
   * @return the number of bins.
   */
  public int getBins() {

    return m_NumBins;
  }

  /**
   * Sets the number of bins to divide each selected numeric attribute into
   *
   * @param numBins the number of bins
   */
  public void setBins(int numBins) {

    m_NumBins = numBins;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String invertSelectionTipText() {

    return "Set attribute selection mode. If false, only selected"
      + " (numeric) attributes in the range will be discretized; if"
      + " true, only non-selected attributes will be discretized.";
  }

  /**
   * Gets whether the supplied columns are to be removed or kept
   *
   * @return true if the supplied columns will be kept
   */
  public boolean getInvertSelection() {

    return m_DiscretizeCols.getInvert();
  }

  /**
   * Sets whether selected columns should be removed or kept. If true the 
   * selected columns are kept and unselected columns are deleted. If false
   * selected columns are deleted and unselected columns are kept.
   *
   * @param invert the new invert setting
   */
  public void setInvertSelection(boolean invert) {

    m_DiscretizeCols.setInvert(invert);
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
   * Gets the current range selection
   *
   * @return a string containing a comma separated list of ranges
   */
  public String getAttributeIndices() {

    return m_DiscretizeCols.getRanges();
  }

  /**
   * Sets which attributes are to be Discretized (only numeric
   * attributes among the selection will be Discretized).
   *
   * @param rangeList a string representing the list of attributes. Since
   * the string will typically come from a user, attributes are indexed from
   * 1. <br>
   * eg: first-3,5,6-last
   * @exception IllegalArgumentException if an invalid range list is supplied 
   */
  public void setAttributeIndices(String rangeList) {

    m_DiscretizeCols.setRanges(rangeList);
  }

  /**
   * Sets which attributes are to be Discretized (only numeric
   * attributes among the selection will be Discretized).
   *
   * @param attributes an array containing indexes of attributes to Discretize.
   * Since the array will typically come from a program, attributes are indexed
   * from 0.
   * @exception IllegalArgumentException if an invalid set of ranges
   * is supplied 
   */
  public void setAttributeIndicesArray(int [] attributes) {

    setAttributeIndices(Range.indicesToRangeList(attributes));
  }

  /**
   * Gets the cut points for an attribute
   *
   * @param the index (from 0) of the attribute to get the cut points of
   * @return an array containing the cutpoints (or null if the
   * attribute requested isn't being Discretized
   */
  public double [] getCutPoints(int attributeIndex) {

    if (m_CutPoints == null) {
      return null;
    }
    return m_CutPoints[attributeIndex];
  }

  /** Generate the cutpoints for each attribute */
  protected void calculateCutPoints() {

    Instances copy = null;

    m_CutPoints = new double [getInputFormat().numAttributes()] [];
    for(int i = getInputFormat().numAttributes() - 1; i >= 0; i--) {
      if ((m_DiscretizeCols.isInRange(i)) && 
	  (getInputFormat().attribute(i).isNumeric())) {
	if(m_UseDataBin)  {
	  // Use copy to preserve order
	  if (copy == null) {
	    copy = new Instances(getInputFormat());
	  }
	  calculateCutPointsByDataBin(i, copy, m_NumBins);
        } 
        else { 
	  // Use copy to preserve order
	  if (copy == null) {
	    copy = new Instances(getInputFormat());
	  }
	  calculateCutPointsByPKID(i, copy);
	}
      }
    }
  }
 
  /**
   * Set cutpoints for a single attribute determined by data bin.
   * If a given ideal cutpoint cannot be set (neighbouring data is the same),  
   * the algorithm attempts to "stepback" ten percent.  If this smaller bin 
   * can't be set, the loop cycles through until a cut can be made.
   * @param index the index of the attribute to set cutpoints for and copy of data
   */
  protected void calculateCutPointsByDataBin(int index, Instances data, int num_bins) {
    
    // Sort instances
    data.sort(index);
    
    long numberOfInstances = data.numInstances();
    // Find number of instances for attribute where not missing
    for (int i = 0; i < data.numInstances(); i++) {
      if (data.instance(i).isMissing(index))
        numberOfInstances--;
    }
    
    int binIndex = 1;
    double [] cutPoints = null;
    int instanceCutRange;  // ideal number of instances in each bin
    int stepBackCount;  // smallest possible number of instances in one bin
    int maxBins;  // maximum possible number of bins
    
    if(numberOfInstances != 0) {
       // checks to see if the requested bin size is larger than the number of instances,
      // to avoid a division by zero.
      if(num_bins >= numberOfInstances){
	instanceCutRange = 1;
	stepBackCount = 1;
      }
      else {
	instanceCutRange = (int)(numberOfInstances / num_bins);  
	stepBackCount = 0;  
	maxBins = 0;
      }       
      
      if(instanceCutRange <= 1) {
	stepBackCount = 1;
	maxBins = (int)numberOfInstances;
      }
      else {
	stepBackCount = (int)(instanceCutRange * 0.9);
	maxBins = (int)(numberOfInstances / stepBackCount);
      } 
      
      cutPoints = new double [maxBins - 1];       
      Instance currentInstance;
      double currentVal, lastVal, lastDiffVal;
      long lastDiffCount = 0;
      long cntr = 1;
      currentVal = lastVal = lastDiffVal = 0;
      
      // loop terminates when not enough instances left to form another bin
      for (int i = 0; i <= numberOfInstances - stepBackCount; i++)  {
	currentInstance = data.instance(i);
	currentVal = currentInstance.value(index);
	// If a value has not been seen and the number of values allowed for each bin has not been reached set the new value seen and add one to the bin count.
	if(currentVal != lastVal)  {       
	  if(cntr <= instanceCutRange)  {
	    lastDiffVal = lastVal;
	    lastVal = currentVal;
	    lastDiffCount = cntr - 1;
	    cntr++;
	  }
	  else  {
	    // If the bin count has been reached, set the cutPoint to equal the last seen value and setup the search for the next cutPoint.
	    cutPoints[binIndex - 1] = lastVal;
	    lastVal = currentVal;
	    binIndex++;
	    lastDiffCount = 0;
	    cntr = 2;
	  }
	}
	else  {
	  if(cntr <= instanceCutRange)  {
	    cntr++;
	  }
	  else  {
	    // attempt step back
	    if(lastDiffCount >= stepBackCount)  {
	      // create cutpoint
	      cutPoints[binIndex - 1] = lastDiffVal;
	      lastDiffVal = currentVal;
	      binIndex++;
	      cntr = cntr - lastDiffCount + 1;
                        lastDiffCount = 0;
	    }
	    else  {
	      cntr++;
	    }
	  }
	}
      }
    }
    
    if(binIndex == 1){
      double [] tempArray = new double [1];
      tempArray[0] = Double.MAX_VALUE;
      m_CutPoints[index] = tempArray;
    }
    else {
      double [] tempArray = new double [binIndex - 1];
      System.arraycopy(cutPoints, 0, tempArray, 0, binIndex - 1);
      m_CutPoints[index] = tempArray;
    }
  }
  
  /**
   * Set cutpoints for a single attribute determined by PKID.<BR>
   * PKID adjusts the number of cutpoints to the number of data objects.
   * This is more appropriate for Naive Bayes than traditional discretization
   * techniques which seek to develop few cutpoints in order to avoid
   * the fragmentation problem.
   * Calculates the number of bins and then calls calculateCutPointsByDataBin<BR>
   * For more Info : refer to Y. Yang and G. Webb, (2001). <i>Proportional k-Interval Discretization for Naive-Bayes Classifiers.</i> 12th European Conference on Machine Learning (ECML01). <BR>
   * @param index the index of the attribute to set cutpoints for and copy of data
   */
  protected void calculateCutPointsByPKID(int index, Instances data) {
    
    long numberOfInstances = data.numInstances();
    // Find number of instances for attribute where not missing
    for (int i = 0; i < data.numInstances(); i++) {
      if (data.instance(i).isMissing(index))
        numberOfInstances--;
    }
    
    int bins = (int)(Math.sqrt(numberOfInstances));
    calculateCutPointsByDataBin(index, data, bins);
  }

  /**
   * Set the output format. Takes the currently defined cutpoints and 
   * m_InputFormat and calls setOutputFormat(Instances) appropriately.
   */
  protected void setOutputFormat() {

    if (m_CutPoints == null) {
      setOutputFormat(null);
      return;
    }
    FastVector attributes = new FastVector(getInputFormat().numAttributes());
    int classIndex = getInputFormat().classIndex();
    for(int i = 0; i < getInputFormat().numAttributes(); i++) {
      if ((m_DiscretizeCols.isInRange(i)) 
	  && (getInputFormat().attribute(i).isNumeric())) {
	if (!m_MakeBinary) {
	  FastVector attribValues = new FastVector(1);
	  if (m_CutPoints[i] == null) {
	    attribValues.addElement("'All'");
	  } else {
	    for(int j = 0; j <= m_CutPoints[i].length; j++) {
	      if (j == 0) {
		attribValues.addElement("'(-inf-"
			+ Utils.doubleToString(m_CutPoints[i][j], 6) + "]'");
	      } else if (j == m_CutPoints[i].length) {
		attribValues.addElement("'("
			+ Utils.doubleToString(m_CutPoints[i][j - 1], 6) 
					+ "-inf)'");
	      } else {
		attribValues.addElement("'("
			+ Utils.doubleToString(m_CutPoints[i][j - 1], 6) + "-"
			+ Utils.doubleToString(m_CutPoints[i][j], 6) + "]'");
	      }
	    }
	  }
	  attributes.addElement(new Attribute(getInputFormat().
					      attribute(i).name(),
					      attribValues));
	} else {
	  if (m_CutPoints[i] == null) {
	    FastVector attribValues = new FastVector(1);
	    attribValues.addElement("'All'");
	    attributes.addElement(new Attribute(getInputFormat().
						attribute(i).name(),
						attribValues));
	  } else {
	    if (i < getInputFormat().classIndex()) {
	      classIndex += m_CutPoints[i].length - 1;
	    }
	    for(int j = 0; j < m_CutPoints[i].length; j++) {
	      FastVector attribValues = new FastVector(2);
	      attribValues.addElement("'(-inf-"
		      + Utils.doubleToString(m_CutPoints[i][j], 6) + "]'");
	      attribValues.addElement("'("
		      + Utils.doubleToString(m_CutPoints[i][j], 6) + "-inf)'");
	      attributes.addElement(new Attribute(getInputFormat().
						  attribute(i).name(),
						  attribValues));
	    }
	  }
	}
      } else {
	attributes.addElement(getInputFormat().attribute(i).copy());
      }
    }
    Instances outputFormat = 
      new Instances(getInputFormat().relationName(), attributes, 0);
    outputFormat.setClassIndex(classIndex);
    setOutputFormat(outputFormat);
  }

  /**
   * Convert a single instance over. The converted instance is added to 
   * the end of the output queue.
   *
   * @param instance the instance to convert
   */
  protected void convertInstance(Instance instance) {

    int index = 0;
    double [] vals = new double [outputFormatPeek().numAttributes()];
    // Copy and convert the values
    for(int i = 0; i < getInputFormat().numAttributes(); i++) {
      if (m_DiscretizeCols.isInRange(i) && 
	  getInputFormat().attribute(i).isNumeric()) {
	int j;
	double currentVal = instance.value(i);
	if (m_CutPoints[i] == null) {
	  if (instance.isMissing(i)) {
	    vals[index] = Instance.missingValue();
	  } else {
	    vals[index] = 0;
	  }
	  index++;
	} else {
	  if (!m_MakeBinary) {
	    if (instance.isMissing(i)) {
	      vals[index] = Instance.missingValue();
	    } else {
	      for (j = 0; j < m_CutPoints[i].length; j++) {
		if (currentVal <= m_CutPoints[i][j]) {
		  break;
		}
	      }
              vals[index] = j;
	    }
	    index++;
	  } else {
	    for (j = 0; j < m_CutPoints[i].length; j++) {
	      if (instance.isMissing(i)) {
                vals[index] = Instance.missingValue();
	      } else if (currentVal <= m_CutPoints[i][j]) {
                vals[index] = 0;
	      } else {
                vals[index] = 1;
	      }
	      index++;
	    }
	  }   
	}
      } else {
        vals[index] = instance.value(i);
	index++;
      }
    }
    
    Instance inst = null;
    if (instance instanceof SparseInstance) {
      inst = new SparseInstance(instance.weight(), vals);
    } else {
      inst = new Instance(instance.weight(), vals);
    }
    copyStringValues(inst, false, instance.dataset(), getInputStringIndex(),
                     getOutputFormat(), getOutputStringIndex());
    inst.setDataset(getOutputFormat());
    push(inst);
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String [] argv) {

    try {
      if (Utils.getFlag('b', argv)) {
 	Filter.batchFilterFile(new PKIDiscretize(), argv);
      } else {
	Filter.filterFile(new PKIDiscretize(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}
