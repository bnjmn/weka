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
 *    DistributionMetaClassifier.java
 *    Copyright (C) 2002 Richard Kirkby
 *
 */

package weka.classifiers.meta;

import weka.classifiers.DistributionClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.*;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Class for wrapping a Classifier to make it return a distribution. Simply outputs
 * a probabiltry of 1 for the predicted class and 0 for the others.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 1.8 $
 */
public class DistributionMetaClassifier extends DistributionClassifier 
  implements OptionHandler {

  /** The classifier being wrapped */
  private Classifier m_wrappedClassifier = new weka.classifiers.rules.ZeroR();

  /**
   * Default constructor.
   * 
   */  
  public DistributionMetaClassifier() {

  }
   
  /**
   * Contructs a DistributionMetaClassifier wrapping a given Classifier.
   * 
   * @param toWrap the classifier to wrap around
   */    
  public DistributionMetaClassifier(Classifier toWrap) {

    setClassifier(toWrap);
  }
  
  /**
   * Builds a classifier for a set of instances.
   *
   * @param instances the instances to train the classifier with
   * @exception Exception if the classifier hasn't been set or something goes wrong
   */  
  public void buildClassifier(Instances data) throws Exception {

    if (m_wrappedClassifier == null) {
      throw new Exception("No classifier has been set");
    }
    m_wrappedClassifier.buildClassifier(data);
  }
  
  /**
   * Returns the class probability distribution for an instance. Will simply have a
   * probability of 1 for the predicted class and 0 for the others.
   *
   * @param instance the instance to be classified
   * @return the probability distribution
   */  
  public double[] distributionForInstance(Instance instance) throws Exception {
    
    double predictedClass = m_wrappedClassifier.classifyInstance(instance);
    double[] distribution = new double[instance.numClasses()];
    if (!Instance.isMissingValue(predictedClass)) {
      if (instance.classAttribute().type() == Attribute.NOMINAL) {
	distribution[(int) predictedClass] = 1.0;
      } else {
	distribution[0] = predictedClass;
      }
    }
    return distribution;
  }
  
  /**
   * Returns a description of the classifier.
   *
   * @return a string containing a description of the classifier
   */
  public String toString() {

    return "DistributionMetaClassifier: " + m_wrappedClassifier.toString();
  }

  /**
   * Sets the classifier to wrap.
   *
   * @param toWrap the classifier
   */
  public void setClassifier(Classifier toWrap) {

    m_wrappedClassifier = toWrap;
  }

  /**
   * Gets the classifier being wrapped.
   *
   * @return the classifier
   */
  public Classifier getClassifier() {

    return m_wrappedClassifier;
  }

  /**
   * Returns an enumeration describing the available options..
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    
    Vector newVector = new Vector(1);
    newVector.addElement(new Option(
				    "\tClassifier to wrap. (required)\n",
				    "W", 1,"-W <classifier name>"));

    if ((m_wrappedClassifier != null) &&
	(m_wrappedClassifier instanceof OptionHandler)) {
      newVector.addElement(new Option(
				      "",
				      "", 0, "\nOptions specific to classifier "
				      + m_wrappedClassifier.getClass().getName() + ":"));
      Enumeration enum = ((OptionHandler)m_wrappedClassifier).listOptions();
      while (enum.hasMoreElements()) {
	newVector.addElement(enum.nextElement());
      }
    }
    
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -W classifier name <br>
   * Classifier to wrap. (required) <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    String wString = Utils.getOption('W', options);
    if (wString.length() != 0) {
      setClassifier(Classifier.forName(wString,
				       Utils.partitionOptions(options)));

    } else {
      throw new Exception("A classifier must be specified with the -W option.");
    }
  }

  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions() {

    String [] classifierOptions = new String [0];
    if ((m_wrappedClassifier != null) &&
	(m_wrappedClassifier instanceof OptionHandler)) {
      classifierOptions = ((OptionHandler)m_wrappedClassifier).getOptions();
    }
    String [] options = new String [classifierOptions.length + 3];
    int current = 0;

    if (getClassifier() != null) {
      options[current++] = "-W";
      options[current++] = getClassifier().getClass().getName();
    }
    options[current++] = "--";

    System.arraycopy(classifierOptions, 0, options, current, 
		     classifierOptions.length);
    current += classifierOptions.length;
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {
    
    try {
      System.out.println(Evaluation.evaluateModel(new DistributionMetaClassifier(), 
						  argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}
