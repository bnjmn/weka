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
 *    FilteredClassifier.java
 *    Copyright (C) 1999 Len Trigg
 *
 */

package weka.classifiers;

import java.util.Enumeration;
import java.util.Vector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.filters.Filter;
import weka.core.Attribute;


/**
 * Class for running an arbitrary classifier on data that has been passed
 * through an arbitrary filter.<p>
 *
 * Valid options from the command line are:<p>
 *
 * -B classifierstring <br>
 * Classifierstring should contain the full class name of a classifier
 * followed by options to the classifier.
 * (required).<p>
 *
 * -F filterstring <br>
 * Filterstring should contain the full class name of a filter
 * followed by options to the filter.
 * (required).<p>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.14 $
 */
public class FilteredClassifier extends DistributionClassifier
  implements OptionHandler {

  /** The classifier */
  protected Classifier m_Classifier = new weka.classifiers.ZeroR();

  /** The filter */
  protected Filter m_Filter = new weka.filters.AllFilter();

  /** The instance structure of the filtered instances */
  protected Instances m_FilteredInstances;
  
  /**
   * Default constructor specifying ZeroR as the classifier and
   * AllFilter as the filter. Both of these are just placeholders
   * for more useful selections.
   */
  public FilteredClassifier() {

    this(new weka.classifiers.ZeroR(), new weka.filters.AllFilter());
  }

  /**
   * Constructor that specifies the subclassifier and filter to use.
   *
   * @param classifier the Classifier to receive filtered instances.
   * @param filter the Filter that will process instances before
   * passing to the Classifier.
   */
  public FilteredClassifier(Classifier classifier, Filter filter) {

    m_Classifier = classifier;
    m_Filter = filter;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(2);

    newVector.addElement(new Option(
	      "\tFull class name of classifier to use, followed\n"
	      + "\tby scheme options. (required)\n"
	      + "\teg: \"weka.classifiers.NaiveBayes -D\"",
	      "B", 1, "-B <classifier specification>"));
    newVector.addElement(new Option(
	      "\tFull class name of filter to use, followed\n"
	      + "\tby filter options. (required)\n"
	      + "\teg: \"weka.filters.AttributeFilter -V -R 1,2\"",
	      "F", 1, "-F <filter specification>"));
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -B classifierstring <br>
   * Classifierstring should contain the full class name of a classifier
   * followed by options to the classifier.
   * (required).<p>
   *
   * -F filterstring <br>
   * Filterstring should contain the full class name of a filter
   * followed by options to the filter.
   * (required).<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String classifierString = Utils.getOption('B', options);
    if (classifierString.length() == 0) {
      throw new Exception("A classifier must be specified"
			  + " with the -B option.");
    }
    String [] classifierSpec = Utils.splitOptions(classifierString);
    if (classifierSpec.length == 0) {
      throw new Exception("Invalid classifier specification string");
    }
    String classifierName = classifierSpec[0];
    classifierSpec[0] = "";
    setClassifier(Classifier.forName(classifierName, classifierSpec));
    
    // Same for filter
    String filterString = Utils.getOption('F', options);
    if (filterString.length() == 0) {
      throw new Exception("A filter must be specified"
			  + " with the -F option.");
    }
    String [] filterSpec = Utils.splitOptions(filterString);
    if (filterSpec.length == 0) {
      throw new Exception("Invalid filter specification string");
    }
    String filterName = filterSpec[0];
    filterSpec[0] = "";
    setFilter((Filter) Utils.forName(Filter.class, filterName, filterSpec));
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [4];
    int current = 0;

    options[current++] = "-B";
    options[current++] = "" + getClassifierSpec();

    // Same for filter
    options[current++] = "-F";
    options[current++] = "" + getFilterSpec();
    
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Sets the classifier
   *
   * @param classifier the classifier with all options set.
   */
  public void setClassifier(Classifier classifier) {

    m_Classifier = classifier;
  }

  /**
   * Gets the classifier used.
   *
   * @return the classifier
   */
  public Classifier getClassifier() {

    return m_Classifier;
  }
  
  /**
   * Gets the classifier specification string, which contains the class name of
   * the classifier and any options to the classifier
   *
   * @return the classifier string.
   */
  protected String getClassifierSpec() {
    
    Classifier c = getClassifier();
    if (c instanceof OptionHandler) {
      return c.getClass().getName() + " "
	+ Utils.joinOptions(((OptionHandler)c).getOptions());
    }
    return c.getClass().getName();
  }

  /**
   * Sets the filter
   *
   * @param filter the filter with all options set.
   */
  public void setFilter(Filter filter) {

    m_Filter = filter;
  }

  /**
   * Gets the filter used.
   *
   * @return the filter
   */
  public Filter getFilter() {

    return m_Filter;
  }
  
  /**
   * Gets the filter specification string, which contains the class name of
   * the filter and any options to the filter
   *
   * @return the filter string.
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
   * Build the classifier on the filtered data.
   *
   * @param data the training data
   * @exception Exception if the classifier could not be built successfully
   */
  public void buildClassifier(Instances data) throws Exception {

    if (m_Classifier == null) {
      throw new Exception("No base classifiers have been set!");
    }
    /*
    String fname = m_Filter.getClass().getName();
    fname = fname.substring(fname.lastIndexOf('.') + 1);
    util.Timer t = util.Timer.getTimer("FilteredClassifier::" + fname);
    t.start();
    */
    m_Filter.setInputFormat(data);
    data = Filter.useFilter(data, m_Filter);
    //t.stop();
    m_FilteredInstances = data.stringFreeStructure();
    m_Classifier.buildClassifier(data);
  }

  /**
   * Classifies a given instance after filtering.
   *
   * @param instance the instance to be classified
   * @exception Exception if instance could not be classified
   * successfully
   */
  public double [] distributionForInstance(Instance instance)
    throws Exception {

    /*
      System.err.println("FilteredClassifier:: " 
                         + m_Filter.getClass().getName()
                         + " in: " + instance);
    */
    if (m_Filter.numPendingOutput() > 0) {
      throw new Exception("Filter output queue not empty!");
    }
    /*
    String fname = m_Filter.getClass().getName();
    fname = fname.substring(fname.lastIndexOf('.') + 1);
    util.Timer t = util.Timer.getTimer("FilteredClassifier::" + fname);
    t.start();
    */
    if (!m_Filter.input(instance)) {
      throw new Exception("Filter didn't make the test instance"
			  + " immediately available!");
    }
    m_Filter.batchFinished();
    Instance newInstance = m_Filter.output();
    //t.stop();
    /*
    System.err.println("FilteredClassifier:: " 
                       + m_Filter.getClass().getName()
                       + " out: " + newInstance);
    */
    if (m_Classifier instanceof DistributionClassifier) {
      return ((DistributionClassifier)m_Classifier)
	.distributionForInstance(newInstance);
    }
    double pred = m_Classifier.classifyInstance(newInstance);
    double [] result = new double [m_FilteredInstances.numClasses()];
    if (Instance.isMissingValue(pred)) {
      return result;
    }
    switch (instance.classAttribute().type()) {
    case Attribute.NOMINAL:
      result[(int) pred] = 1.0;
      break;
    case Attribute.NUMERIC:
      result[0] = pred;
      break;
    default:
      throw new Exception("Unknown class type");
    }
    return result;
  }

  /**
   * Output a representation of this classifier
   */
  public String toString() {

    if (m_FilteredInstances == null) {
      return "FilteredClassifier: No model built yet.";
    }

    String result = "FilteredClassifier using "
      + getClassifierSpec()
      + " on data filtered through "
      + getFilterSpec()
      + "\n\nFiltered Header\n"
      + m_FilteredInstances.toString()
      + "\n\nClassifier Model\n"
      + m_Classifier.toString();
    return result;
  }


  /**
   * Main method for testing this class.
   *
   * @param argv should contain the following arguments:
   * -t training file [-T test file] [-c class index]
   */
  public static void main(String [] argv) {

    try {
      System.out.println(Evaluation.evaluateModel(new FilteredClassifier(),
						  argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }

}
