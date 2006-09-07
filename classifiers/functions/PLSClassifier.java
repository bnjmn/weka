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
 * PLSClassifier.java
 * Copyright (C) 2006 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.functions;

import weka.classifiers.Classifier;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.filters.Filter;
import weka.filters.supervised.attribute.PLSFilter;

import java.util.Enumeration;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * A wrapper classifier for the PLSFilter, utilizing the PLSFilter's ability to perform predictions.
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -filter &lt;filter specification&gt;
 *  The PLS filter to use. Full classname of filter to include,  followed by scheme options.
 *  (default: weka.filters.supervised.attribute.PLSFilter)</pre>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> 
 * Options specific to filter weka.filters.supervised.attribute.PLSFilter ('-filter'):
 * </pre>
 * 
 * <pre> -D
 *  Turns on output of debugging information.</pre>
 * 
 * <pre> -C &lt;num&gt;
 *  The number of components to compute.
 *  (default: 20)</pre>
 * 
 * <pre> -U
 *  Updates the class attribute as well.
 *  (default: off)</pre>
 * 
 * <pre> -M
 *  Turns replacing of missing values on.
 *  (default: off)</pre>
 * 
 * <pre> -A &lt;SIMPLS|PLS1&gt;
 *  The algorithm to use.
 *  (default: PLS1)</pre>
 * 
 * <pre> -P &lt;none|center|standardize&gt;
 *  The type of preprocessing that is applied to the data.
 *  (default: center)</pre>
 * 
 <!-- options-end -->
 *
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.2 $
 */
public class PLSClassifier
  extends Classifier {
  
  /** for serialization */
  private static final long serialVersionUID = 4819775160590973256L;

  /** the PLS filter */
  protected PLSFilter m_Filter = new PLSFilter();

  /** the actual filter to use */
  protected PLSFilter m_ActualFilter = null;
  
  /**
   * Returns a string describing classifier
   * 
   * @return a description suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return 
        "A wrapper classifier for the PLSFilter, utilizing the PLSFilter's "
      + "ability to perform predictions.";
  }

  /**
   * Gets an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions(){
    Vector        	result;
    Enumeration   	en;

    result = new Vector();

    result.addElement(new Option(
	"\tThe PLS filter to use. Full classname of filter to include, "
	+ "\tfollowed by scheme options.\n"
	+ "\t(default: weka.filters.supervised.attribute.PLSFilter)",
	"filter", 1, "-filter <filter specification>"));

    en = super.listOptions();
    while (en.hasMoreElements())
      result.addElement(en.nextElement());

    if (getFilter() instanceof OptionHandler) {
      result.addElement(new Option(
	  "",
	  "", 0, "\nOptions specific to filter "
	  + getFilter().getClass().getName() + " ('-filter'):"));
      
      en = ((OptionHandler) getFilter()).listOptions();
      while (en.hasMoreElements())
	result.addElement(en.nextElement());
    }

    return result.elements();
  }
  
  /**
   * returns the options of the current setup
   *
   * @return		the current options
   */
  public String[] getOptions(){
    int       	i;
    Vector    	result;
    String[]  	options;

    result = new Vector();

    result.add("-filter");
    if (getFilter() instanceof OptionHandler)
      result.add(
  	    getFilter().getClass().getName() 
	  + " " 
	  + Utils.joinOptions(((OptionHandler) getFilter()).getOptions()));
    else
      result.add(
	  getFilter().getClass().getName());

    options = super.getOptions();
    for (i = 0; i < options.length; i++)
      result.add(options[i]);

    return (String[]) result.toArray(new String[result.size()]);	  
  }

  /**
   * Parses the options for this object. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -filter &lt;filter specification&gt;
   *  The PLS filter to use. Full classname of filter to include,  followed by scheme options.
   *  (default: weka.filters.supervised.attribute.PLSFilter)</pre>
   * 
   * <pre> -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   * <pre> 
   * Options specific to filter weka.filters.supervised.attribute.PLSFilter ('-filter'):
   * </pre>
   * 
   * <pre> -D
   *  Turns on output of debugging information.</pre>
   * 
   * <pre> -C &lt;num&gt;
   *  The number of components to compute.
   *  (default: 20)</pre>
   * 
   * <pre> -U
   *  Updates the class attribute as well.
   *  (default: off)</pre>
   * 
   * <pre> -M
   *  Turns replacing of missing values on.
   *  (default: off)</pre>
   * 
   * <pre> -A &lt;SIMPLS|PLS1&gt;
   *  The algorithm to use.
   *  (default: PLS1)</pre>
   * 
   * <pre> -P &lt;none|center|standardize&gt;
   *  The type of preprocessing that is applied to the data.
   *  (default: center)</pre>
   * 
   <!-- options-end -->
   *
   * @param options	the options to use
   * @throws Exception	if setting of options fails
   */
  public void setOptions(String[] options) throws Exception {
    String	tmpStr;
    String[]	tmpOptions;
    
    super.setOptions(options);
    
    tmpStr     = Utils.getOption("filter", options);
    tmpOptions = Utils.splitOptions(tmpStr);
    if (tmpOptions.length != 0) {
      tmpStr        = tmpOptions[0];
      tmpOptions[0] = "";
      setFilter((Filter) Utils.forName(Filter.class, tmpStr, tmpOptions));
    }
  }
  
  /**
   * Returns the tip text for this property
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String filterTipText() {
    return "The PLS filter to be used (only used for setup).";
  }

  /**
   * Set the PLS filter (only used for setup).
   *
   * @param value	the kernel filter.
   * @throws Exception	if not PLSFilter
   */
  public void setFilter(Filter value) throws Exception {
    if (!(value instanceof PLSFilter))
      throw new Exception("Filter has to be PLSFilter!");
    else
      m_Filter = (PLSFilter) value;
  }

  /**
   * Get the PLS filter.
   *
   * @return 		the PLS filter
   */
  public Filter getFilter() {
    return m_Filter;
  }
  
  /**
   * Returns default capabilities of the classifier.
   *
   * @return		the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = getFilter().getCapabilities();

    // class
    result.enable(Capability.MISSING_CLASS_VALUES);

    return result;
  }

  /**
   * builds the classifier
   * 
   * @param data        the training instances
   * @throws Exception  if something goes wrong
   */
  public void buildClassifier(Instances data) throws Exception {
    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    // remove instances with missing class
    data = new Instances(data);
    data.deleteWithMissingClass();
    
    // initialize filter
    m_ActualFilter = (PLSFilter) Filter.makeCopy(m_Filter);
    m_ActualFilter.setPerformPrediction(false);
    m_ActualFilter.setInputFormat(data);
    Filter.useFilter(data, m_ActualFilter);
    m_ActualFilter.setPerformPrediction(true);
  }

  /**
   * Classifies the given test instance. The instance has to belong to a
   * dataset when it's being classified.
   *
   * @param instance 	the instance to be classified
   * @return 		the predicted most likely class for the instance or 
   * 			Instance.missingValue() if no prediction is made
   * @throws Exception 	if an error occurred during the prediction
   */
  public double classifyInstance(Instance instance) throws Exception {
    double	result;
    Instance	pred;
    
    m_ActualFilter.input(instance);
    m_ActualFilter.batchFinished();
    pred   = m_ActualFilter.output();
    result = pred.classValue();
    
    return result;
  }

  /**
   * returns a string representation of the classifier
   * 
   * @return		a string representation of the classifier
   */
  public String toString() {
    String	result;
    
    result =   this.getClass().getName() + "\n" 
             + this.getClass().getName().replaceAll(".", "=") + "\n\n";
    result += "# Components..........: " + m_Filter.getNumComponents() + "\n";
    result += "Algorithm.............: " + m_Filter.getAlgorithm().getSelectedTag().getReadable() + "\n";
    result += "Replace missing values: " + (m_Filter.getReplaceMissing() ? "yes" : "no") + "\n";
    result += "Preprocessing.........: " + m_Filter.getPreprocessing().getSelectedTag().getReadable() + "\n";
    
    return result;
  }
  
  /**
   * Main method for running this classifier from commandline.
   * 
   * @param args 	the options
   */
  public static void main(String[] args) {
    runClassifier(new PLSClassifier(), args);
  }
}
