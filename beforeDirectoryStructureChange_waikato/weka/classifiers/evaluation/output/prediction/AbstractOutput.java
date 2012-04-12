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
 * AbstractOutput.java
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.evaluation.output.prediction;

import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Range;
import weka.core.Utils;
import weka.core.WekaException;
import weka.core.converters.ConverterUtils.DataSource;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

/**
 * A superclass for outputting the classifications of a classifier.
 * <p/>
 * Basic use with a classifier and a test set:
 * <pre>
 * Classifier classifier = ... // trained classifier
 * Instances testset = ... // the test set to output the predictions for
 * StringBuffer buffer = ... // the string buffer to add the output to
 * AbstractOutput output = new FunkyOutput();
 * output.setHeader(...);
 * output.printClassifications(classifier, testset);
 * </pre>
 * 
 * Basic use with a classifier and a data source:
 * <pre>
 * Classifier classifier = ... // trained classifier
 * DataSource testset = ... // the data source to obtain the test set from to output the predictions for
 * StringBuffer buffer = ... // the string buffer to add the output to
 * AbstractOutput output = new FunkyOutput();
 * output.setHeader(...);
 * output.printClassifications(classifier, testset);
 * </pre>
 * 
 * In order to make the output generation easily integrate into GUI components,
 * one can output the header, classifications and footer separately:
 * <pre>
 * Classifier classifier = ... // trained classifier
 * Instances testset = ... // the test set to output the predictions for
 * StringBuffer buffer = ... // the string buffer to add the output to
 * AbstractOutput output = new FunkyOutput();
 * output.setHeader(...);
 * // print the header
 * output.printHeader();
 * // print the classifications one-by-one
 * for (int i = 0; i &lt; testset.numInstances(); i++) {
 *   output.printClassification(classifier, testset.instance(i), i);
 *   // output progress information
 *   if ((i+1) % 100 == 0)
 *     System.out.println((i+1) + "/" + testset.numInstances());
 * }
 * // print the footer
 * output.printFooter();
 * </pre>
 * 
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public abstract class AbstractOutput
  implements Serializable, OptionHandler {

  /** for serialization. */
  private static final long serialVersionUID = 752696986017306241L;

  /** the header of the dataset. */
  protected Instances m_Header;

  /** the buffer to write to. */
  protected StringBuffer m_Buffer;
  
  /** whether to output the class distribution. */
  protected boolean m_OutputDistribution;
  
  /** the range of attributes to output. */
  protected Range m_Attributes;
  
  /**
   * Initializes the output class. 
   */
  public AbstractOutput() {
    m_Header             = null;
    m_OutputDistribution = false;
    m_Attributes         = null;
    m_Buffer             = null;
  }
  
  /**
   * Returns a string describing the output generator.
   * 
   * @return 		a description suitable for
   * 			displaying in the GUI
   */
  public abstract String globalInfo();
  
  /**
   * Returns a short display text, to be used in comboboxes.
   * 
   * @return 		a short display text
   */
  public abstract String getDisplay();

  /**
   * Returns an enumeration of all the available options..
   *
   * @return 		an enumeration of all available options.
   */
  public Enumeration listOptions() {
    Vector	result;
    
    result = new Vector();
    
    result.addElement(new Option(
        "\tThe range of attributes to print in addition to the classification.\n"
	+ "\t(default: none)",
        "p", 1, "-p <range>"));
    
    result.addElement(new Option(
        "\tWhether to turn on the output of the class distribution.\n"
	+ "\tOnly for nominal class attributes.\n"
	+ "\t(default: off)",
        "distribution", 0, "-distribution"));
    
    return result.elements();
  }

  /**
   * Sets the OptionHandler's options using the given list. All options
   * will be set (or reset) during this call (i.e. incremental setting
   * of options is not possible).
   *
   * @param options 	the list of options as an array of strings
   * @throws Exception 	if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    setAttributes(Utils.getOption("p", options));
    setOutputDistribution(Utils.getFlag("distribution", options));
  }

  /**
   * Gets the current option settings for the OptionHandler.
   *
   * @return the list of current option settings as an array of strings
   */
  public String[] getOptions() {
    Vector<String>	result;
    
    result = new Vector<String>();
    
    if (getAttributes().length() > 0) {
      result.add("-p");
      result.add(getAttributes());
    }
    
    if (getOutputDistribution())
      result.add("-distribution");
    
    return result.toArray(new String[result.size()]);
  }
  
  /**
   * Sets the header of the dataset.
   * 
   * @param value	the header
   */
  public void setHeader(Instances value) {
    m_Header = new Instances(value, 0);
  }
  
  /**
   * Returns the header of the dataset.
   * 
   * @return		the header
   */
  public Instances getHeader() {
    return m_Header;
  }
  
  /**
   * Sets the buffer to use.
   * 
   * @param value	the buffer
   */
  public void setBuffer(StringBuffer value) {
    m_Buffer = value;
  }
  
  /**
   * Returns the current buffer.
   * 
   * @return		the buffer, can be null
   */
  public StringBuffer getBuffer() {
    return m_Buffer;
  }
  
  /**
   * Sets the range of attributes to output.
   * 
   * @param value	the range
   */
  public void setAttributes(String value) {
    if (value.length() == 0)
      m_Attributes = null;
    else
      m_Attributes = new Range(value);
  }
  
  /**
   * Returns the range of attributes to output.
   * 
   * @return		the range
   */
  public String getAttributes() {
    if (m_Attributes == null)
      return "";
    else
      return m_Attributes.getRanges();
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the GUI
   */
  public String attributesTipText() {
    return "The indices of the attributes to print in addition.";
  }
  
  /**
   * Sets whether to output the class distribution or not.
   * 
   * @param value	true if the class distribution is to be output as well
   */
  public void setOutputDistribution(boolean value) {
    m_OutputDistribution = value;
  }
  
  /**
   * Returns whether to output the class distribution as well.
   * 
   * @return		true if the class distribution is output as well
   */
  public boolean getOutputDistribution() {
    return m_OutputDistribution;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the GUI
   */
  public String outputDistributionTipText() {
    return "Whether to ouput the class distribution as well (only nominal class attributes).";
  }
  
  /**
   * Performs basic checks.
   * 
   * @return		null if everything is in order, otherwise the error message
   */
  protected String checkBasic() {
    if (m_Buffer == null)
      return "Buffer is null!";
    
    if (m_Header == null)
      return "No dataset structure provided!";
    
    if (m_Attributes != null)
      m_Attributes.setUpper(m_Header.numAttributes() - 1);
    
    return null;
  }
  
  /**
   * Performs checks whether everything is correctly setup for the header.
   * 
   * @return		null if everything is in order, otherwise the error message
   */
  protected String checkHeader() {
    return checkBasic();
  }
  
  /**
   * Performs the actual printing of the header.
   */
  protected abstract void doPrintHeader();
  
  /**
   * Prints the header to the buffer.
   */
  public void printHeader() {
    String	error;
    
    if ((error = checkHeader()) != null)
      throw new IllegalStateException(error);
    
    doPrintHeader();
  }
  
  /**
   * Performs the actual printing of the classification.
   * 
   * @param classifier	the classifier to use for printing the classification
   * @param inst	the instance to print
   * @param index	the index of the instance
   * @throws Exception	if printing of classification fails
   */
  protected abstract void doPrintClassification(Classifier classifier, Instance inst, int index) throws Exception;
  
  /**
   * Prints the classification to the buffer.
   * 
   * @param classifier	the classifier to use for printing the classification
   * @param inst	the instance to print
   * @param index	the index of the instance
   * @throws Exception	if check fails or error occurs during printing of classification
   */
  public void printClassification(Classifier classifier, Instance inst, int index) throws Exception {
    String	error;
    
    if ((error = checkBasic()) != null)
      throw new WekaException(error);
    
    doPrintClassification(classifier, inst, index);
  }
  
  /**
   * Prints the classifications to the buffer.
   * 
   * @param classifier	the classifier to use for printing the classifications
   * @param testset	the data source to obtain the test instances from
   * @throws Exception	if check fails or error occurs during printing of classifications
   */
  public void printClassifications(Classifier classifier, DataSource testset) throws Exception {
    int 	i;
    Instances 	test;
    Instance 	inst;
    
    i = 0;
    testset.reset();
    test = testset.getStructure(m_Header.classIndex());
    while (testset.hasMoreElements(test)) {
      inst = testset.nextElement(test);
      doPrintClassification(classifier, inst, i);
      i++;
    }
  }
  
  /**
   * Prints the classifications to the buffer.
   * 
   * @param classifier	the classifier to use for printing the classifications
   * @param testset	the test instances
   * @throws Exception	if check fails or error occurs during printing of classifications
   */
  public void printClassifications(Classifier classifier, Instances testset) throws Exception {
    int 	i;

    for (i = 0; i < testset.numInstances(); i++)
      doPrintClassification(classifier, testset.instance(i), i);
  }
  
  /**
   * Performs the actual printing of the footer.
   */
  protected abstract void doPrintFooter();
  
  /**
   * Prints the footer to the buffer.
   * 
   * @throws Exception	if check fails
   */
  public void printFooter() throws Exception {
    String	error;
    
    if ((error = checkBasic()) != null)
      throw new WekaException(error);
    
    doPrintFooter();
  }
  
  /**
   * Prints the header, classifications and footer to the buffer.
   * 
   * @param classifier	the classifier to use for printing the classifications
   * @param testset	the data source to obtain the test instances from
   * @throws Exception	if check fails or error occurs during printing of classifications
   */
  public void print(Classifier classifier, DataSource testset) throws Exception {
    printHeader();
    printClassifications(classifier, testset);
    printFooter();
  }
  
  /**
   * Prints the header, classifications and footer to the buffer.
   * 
   * @param classifier	the classifier to use for printing the classifications
   * @param testset	the test instances
   * @throws Exception	if check fails or error occurs during printing of classifications
   */
  public void print(Classifier classifier, Instances testset) throws Exception {
    printHeader();
    printClassifications(classifier, testset);
    printFooter();
  }
  
  /**
   * Returns a fully configured object from the given commandline.
   * 
   * @param cmdline	the commandline to turn into an object
   * @return		the object or null in case of an error
   */
  public static AbstractOutput fromCommandline(String cmdline) {
    AbstractOutput	result;
    String[]				options;
    String				classname;
    
    try {
      options    = Utils.splitOptions(cmdline);
      classname  = options[0];
      options[0] = "";
      result     = (AbstractOutput) Utils.forName(AbstractOutput.class, classname, options);
    }
    catch (Exception e) {
      result = null;
    }
    
    return result;
  }
}
