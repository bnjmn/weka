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
 *    FilteredAttributeEval.java
 *    Copyright (C) 2008 Pentaho Corporation
 *
 */

package  weka.attributeSelection;

import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.filters.Filter;
import weka.core.Instances;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.Utils;

import java.util.Enumeration;
import java.util.Vector;
import java.util.ArrayList;
import java.io.Serializable;

/**
 <!-- globalinfo-start -->
 * Class for running an arbitrary attribute evaluator on data that has been passed through an 
 * arbitrary filter (note: filters that alter the order or number of attributes are not allowed). 
 * Like the evaluator, the structure of the filter is based exclusively on the training data.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -W &lt;evaluator specification&gt;
 *  Full name of base evaluator to use, followed by evaluator options.
 *  eg: "weka.attributeSelection.InfoGainAttributeEval -M"</pre>
 * 
 * <pre> -F &lt;filter specification&gt;
 *  Full class name of filter to use, followed
 *  by filter options.
 *  eg: "weka.filters.supervised.instance.SpreadSubsample -M 1"</pre>
 * 
 <!-- options-end -->
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 1.1.2.2 $
 */
public class FilteredAttributeEval
  extends ASEvaluation
  implements Serializable, AttributeEvaluator, OptionHandler {

  /** For serialization */
  static final long serialVersionUID = 2111121880778327334L;

  /** Base evaluator */
  protected AttributeEvaluator m_evaluator = new InfoGainAttributeEval();

  /** Filter */
  protected Filter m_filter = new weka.filters.supervised.instance.SpreadSubsample();

  /** Filtered instances structure */
  protected Instances m_filteredInstances;

  public FilteredAttributeEval() {
    m_filteredInstances = null;
  }

  /**
   * Returns default capabilities of the evaluator.
   *
   * @return      the capabilities of this evaluator.
   */
  public Capabilities getCapabilities() {
    Capabilities result;

    if (getFilter() == null) {
      result = super.getCapabilities();
    } else {
      result = getFilter().getCapabilities();
    }
    
    // set dependencies
    for (Capability cap: Capability.values()) {
      result.enableDependency(cap);
    }
    
    return result;
  }

  /**
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Class for running an arbitrary attribute evaluator on data that has been passed "
      + "through an arbitrary filter (note: filters that alter the order or number of "
      + "attributes are not allowed). Like the evaluator, the structure of the filter "
      + "is based exclusively on the training data.";
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector newVector = new Vector(2);

    newVector.addElement(new Option(
                                    "\tFull name of base evaluator to use, followed by "
                                    +"evaluator options.\n"
                                    + "\teg: \"weka.attributeSelection.InfoGainAttributeEval -M\"",
                                    "W", 1, "-W <evaluator specification>"));

    newVector.addElement(new Option(
	      "\tFull class name of filter to use, followed\n"
	      + "\tby filter options.\n"
	      + "\teg: \"weka.filters.supervised.instance.SpreadSubsample -M 1\"",
	      "F", 1, "-F <filter specification>"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -W &lt;evaluator specification&gt;
   *  Full name of base evaluator to use, followed by evaluator options.
   *  eg: "weka.attributeSelection.InfoGainAttributeEval -M"</pre>
   * 
   * <pre> -F &lt;filter specification&gt;
   *  Full class name of filter to use, followed
   *  by filter options.
   *  eg: "weka.filters.supervised.instance.SpreadSubsample -M 1"</pre>
   * 
   <!-- options-end -->  
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    String evaluator = Utils.getOption('W', options);
    
    if (evaluator.length() > 0) { 
      String[] evaluatorSpec = Utils.splitOptions(evaluator);
      if (evaluatorSpec.length == 0) {
        throw new IllegalArgumentException("Invalid evaluator specification string");
      }
      
      String evaluatorName = evaluatorSpec[0];
      evaluatorSpec[0] = "";
      setAttributeEvaluator((ASEvaluation)Utils.forName(AttributeEvaluator.class,
                                                        evaluatorName, evaluatorSpec));

    } else {      
      setAttributeEvaluator(new InfoGainAttributeEval());
    }

    // Same for filter
    String filterString = Utils.getOption('F', options);
    if (filterString.length() > 0) {
      String [] filterSpec = Utils.splitOptions(filterString);
      if (filterSpec.length == 0) {
	throw new IllegalArgumentException("Invalid filter specification string");
      }
      String filterName = filterSpec[0];
      filterSpec[0] = "";
      setFilter((Filter) Utils.forName(Filter.class, filterName, filterSpec));
    } else {
      setFilter(new weka.filters.unsupervised.attribute.Remove());
    }
  }

  /**
   * Gets the current settings of the subset evaluator.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    ArrayList<String> options = new ArrayList<String>();
    
    options.add("-W");
    options.add(getEvaluatorSpec());

    options.add("-F");
    options.add(getFilterSpec());
    
    return options.toArray(new String[0]);
  }

  /**
   * Get the evaluator + options as a string
   *
   * @return a String containing the name of the evalautor + any options
   */
  protected String getEvaluatorSpec() {
    AttributeEvaluator a = m_evaluator;
    if (a instanceof OptionHandler) {
      return a.getClass().getName() + " "
        + Utils.joinOptions(((OptionHandler)a).getOptions());
    }
    return a.getClass().getName();
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String attributeEvaluatorTipText() {
    return "The attribute evaluator to be used.";
  }
  
  /**
   * Set the attribute evaluator to use
   *
   * @param newEvaluator the attribute evaluator to use
   */
  public void setAttributeEvaluator(ASEvaluation newEvaluator) {
    if (!(newEvaluator instanceof AttributeEvaluator)) {
      throw new IllegalArgumentException("Evaluator must be an AttributeEvaluator!");
    }
    m_evaluator = (AttributeEvaluator)newEvaluator;
  }

  /**
   * Get the attribute evaluator to use
   *
   * @return the attribute evaluator to use
   */
  public ASEvaluation getAttributeEvaluator() {
    return (ASEvaluation)m_evaluator;
  }

  /**
   * Get the filter + options as a string
   *
   * @return a String containing the name of the filter + any options
   */
  protected String getFilterSpec() {
    Filter c = getFilter();
    if (c instanceof OptionHandler) {
      return c.getClass().getName() + " "
	+ Utils.joinOptions(((OptionHandler)c).getOptions());
    }
    return c.getClass().getName();
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String filterTipText() {
    return "The filter to be used.";
  }

  /**
   * Set the filter to use
   *
   * @param newFilter the filter to use
   */
  public void setFilter(Filter newFilter) {
    m_filter = newFilter;
  }

  /**
   * Get the filter to use
   *
   * @return the filter to use
   */
  public Filter getFilter() {
    return m_filter;
  }

  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 1.1.2.2 $");
  }

  /**
   * Initializes a filtered attribute evaluator.
   *
   * @param data set of instances serving as training data 
   * @throws Exception if the evaluator has not been 
   * generated successfully
   */
  public void buildEvaluator(Instances data) throws Exception {
    // can evaluator handle data?
    getCapabilities().testWithFail(data);

    // Structure of original
    Instances original = new Instances(data, 0);

    m_filter.setInputFormat(data);
    data = Filter.useFilter(data, m_filter);

    // Can only proceed if filter has not altered the order or
    // number of attributes in the data
    if (data.numAttributes() != original.numAttributes()) {
      throw new Exception("Filter must not alter the number of "
                          +"attributes in the data!");
    }

    // Check the class index (if set)
    if (original.classIndex() >= 0) {
      if (data.classIndex() != original.classIndex()) {
        throw new Exception("Filter must not change the class attribute!");
      }
    }

    // check the order
    for (int i = 0; i < original.numAttributes(); i++) {
      if (!data.attribute(i).name().equals(original.attribute(i).name())) {
        throw new Exception("Filter must not alter the order of the attributes!");
      }
    }

    // can the evaluator handle this data?
    ((ASEvaluation)getAttributeEvaluator()).getCapabilities().testWithFail(data);
    m_filteredInstances = data.stringFreeStructure();
    
    ((ASEvaluation)m_evaluator).buildEvaluator(data);
  }

  /**
   * Evaluates an individual attribute by delegating to the base
   * evaluator.
   *
   * @param attribute the index of the attribute to be evaluated
   * @return the merit of the attribute according to the base evaluator
   * @throws Exception if the attribute could not be evaluated
   */
  public double evaluateAttribute(int attribute) throws Exception {
    return m_evaluator.evaluateAttribute(attribute);
  }

  /**
   * Describe the attribute evaluator
   * @return a description of the attribute evaluator as a string
   */
  public String toString() {
    StringBuffer text = new StringBuffer();
    
    if (m_filteredInstances == null) {
      text.append("Filtered attribute evaluator has not been built");
    } else {
      text.append("Filtered Attribute Evaluator");
      text.append("\nFilter: " + getFilterSpec());
      text.append("\nAttribute evaluator: " + getEvaluatorSpec());
      text.append("\n\nFiltered header:\n");
      text.append(m_filteredInstances);
    }
    text.append("\n");
    return text.toString();
  }

  // ============
  // Test method.
  // ============
  /**
   * Main method for testing this class.
   *
   * @param args the options
   */
  public static void main (String[] args) {
    runEvaluator(new FilteredAttributeEval(), args);
  }
}

