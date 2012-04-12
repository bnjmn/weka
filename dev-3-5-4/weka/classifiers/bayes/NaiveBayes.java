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
 *    NaiveBayes.java
 *    Copyright (C) 1999 Eibe Frank,Len Trigg
 *
 */

package weka.classifiers.bayes;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.estimators.DiscreteEstimator;
import weka.estimators.Estimator;
import weka.estimators.KernelEstimator;
import weka.estimators.NormalEstimator;

import java.util.Enumeration;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * Class for a Naive Bayes classifier using estimator classes. Numeric estimator precision values are chosen based on analysis of the  training data. For this reason, the classifier is not an UpdateableClassifier (which in typical usage are initialized with zero training instances) -- if you need the UpdateableClassifier functionality, use the NaiveBayesUpdateable classifier. The NaiveBayesUpdateable classifier will  use a default precision of 0.1 for numeric attributes when buildClassifier is called with zero training instances.<br/>
 * <br/>
 * For more information on Naive Bayes classifiers, see<br/>
 * <br/>
 * George H. John, Pat Langley: Estimating Continuous Distributions in Bayesian Classifiers. In: Eleventh Conference on Uncertainty in Artificial Intelligence, San Mateo, 338-345, 1995.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{John1995,
 *    address = {San Mateo},
 *    author = {George H. John and Pat Langley},
 *    booktitle = {Eleventh Conference on Uncertainty in Artificial Intelligence},
 *    pages = {338-345},
 *    publisher = {Morgan Kaufmann},
 *    title = {Estimating Continuous Distributions in Bayesian Classifiers},
 *    year = {1995}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -K
 *  Use kernel density estimator rather than normal
 *  distribution for numeric attributes</pre>
 * 
 * <pre> -D
 *  Use supervised discretization to process numeric attributes
 * </pre>
 * 
 <!-- options-end -->
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.20 $
 */
public class NaiveBayes extends Classifier 
  implements OptionHandler, WeightedInstancesHandler, TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = 5995231201785697655L;
  
  /** The attribute estimators. */
  protected Estimator [][] m_Distributions;
  
  /** The class estimator. */
  protected Estimator m_ClassDistribution;

  /**
   * Whether to use kernel density estimator rather than normal distribution
   * for numeric attributes
   */
  protected boolean m_UseKernelEstimator = false;

  /**
   * Whether to use discretization than normal distribution
   * for numeric attributes
   */
  protected boolean m_UseDiscretization = false;

  /** The number of classes (or 1 for numeric class) */
  protected int m_NumClasses;

  /**
   * The dataset header for the purposes of printing out a semi-intelligible 
   * model 
   */
  protected Instances m_Instances;

  /*** The precision parameter used for numeric attributes */
  protected static final double DEFAULT_NUM_PRECISION = 0.01;

  /**
   * The discretization filter.
   */
  protected weka.filters.supervised.attribute.Discretize m_Disc = null;

  /**
   * Returns a string describing this classifier
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Class for a Naive Bayes classifier using estimator classes. Numeric"
      +" estimator precision values are chosen based on analysis of the "
      +" training data. For this reason, the classifier is not an"
      +" UpdateableClassifier (which in typical usage are initialized with zero"
      +" training instances) -- if you need the UpdateableClassifier functionality,"
      +" use the NaiveBayesUpdateable classifier. The NaiveBayesUpdateable"
      +" classifier will  use a default precision of 0.1 for numeric attributes"
      +" when buildClassifier is called with zero training instances.\n\n"
      +"For more information on Naive Bayes classifiers, see\n\n"
      + getTechnicalInformation().toString();
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing 
   * detailed information about the technical background of this class,
   * e.g., paper reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation 	result;
    
    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "George H. John and Pat Langley");
    result.setValue(Field.TITLE, "Estimating Continuous Distributions in Bayesian Classifiers");
    result.setValue(Field.BOOKTITLE, "Eleventh Conference on Uncertainty in Artificial Intelligence");
    result.setValue(Field.YEAR, "1995");
    result.setValue(Field.PAGES, "338-345");
    result.setValue(Field.PUBLISHER, "Morgan Kaufmann");
    result.setValue(Field.ADDRESS, "San Mateo");
    
    return result;
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    // instances
    result.setMinimumNumberInstances(0);
    
    return result;
  }

  /**
   * Generates the classifier.
   *
   * @param instances set of instances serving as training data 
   * @exception Exception if the classifier has not been generated 
   * successfully
   */
  public void buildClassifier(Instances instances) throws Exception {

    // can classifier handle the data?
    getCapabilities().testWithFail(instances);

    // remove instances with missing class
    instances = new Instances(instances);
    instances.deleteWithMissingClass();
    
    m_NumClasses = instances.numClasses();
    
    // Copy the instances
    m_Instances = new Instances(instances);

    // Discretize instances if required
    if (m_UseDiscretization) {
      m_Disc = new weka.filters.supervised.attribute.Discretize();
      m_Disc.setInputFormat(m_Instances);
      m_Instances = weka.filters.Filter.useFilter(m_Instances, m_Disc);
    } else {
      m_Disc = null;
    }

    // Reserve space for the distributions
    m_Distributions = new Estimator[m_Instances.numAttributes() - 1]
    [m_Instances.numClasses()];
    m_ClassDistribution = new DiscreteEstimator(m_Instances.numClasses(), 
						true);
    int attIndex = 0;
    Enumeration enu = m_Instances.enumerateAttributes();
    while (enu.hasMoreElements()) {
      Attribute attribute = (Attribute) enu.nextElement();

      // If the attribute is numeric, determine the estimator 
      // numeric precision from differences between adjacent values
      double numPrecision = DEFAULT_NUM_PRECISION;
      if (attribute.type() == Attribute.NUMERIC) {
	m_Instances.sort(attribute);
	if ((m_Instances.numInstances() > 0)
	    && !m_Instances.instance(0).isMissing(attribute)) {
	  double lastVal = m_Instances.instance(0).value(attribute);
	  double currentVal, deltaSum = 0;
	  int distinct = 0;
	  for (int i = 1; i < m_Instances.numInstances(); i++) {
	    Instance currentInst = m_Instances.instance(i);
	    if (currentInst.isMissing(attribute)) {
	      break;
	    }
	    currentVal = currentInst.value(attribute);
	    if (currentVal != lastVal) {
	      deltaSum += currentVal - lastVal;
	      lastVal = currentVal;
	      distinct++;
	    }
	  }
	  if (distinct > 0) {
	    numPrecision = deltaSum / distinct;
	  }
	}
      }


      for (int j = 0; j < m_Instances.numClasses(); j++) {
	switch (attribute.type()) {
	case Attribute.NUMERIC: 
	  if (m_UseKernelEstimator) {
	    m_Distributions[attIndex][j] = 
	    new KernelEstimator(numPrecision);
	  } else {
	    m_Distributions[attIndex][j] = 
	    new NormalEstimator(numPrecision);
	  }
	  break;
	case Attribute.NOMINAL:
	  m_Distributions[attIndex][j] = 
	  new DiscreteEstimator(attribute.numValues(), true);
	  break;
	default:
	  throw new Exception("Attribute type unknown to NaiveBayes");
	}
      }
      attIndex++;
    }

    // Compute counts
    Enumeration enumInsts = m_Instances.enumerateInstances();
    while (enumInsts.hasMoreElements()) {
      Instance instance = 
	(Instance) enumInsts.nextElement();
      updateClassifier(instance);
    }

    // Save space
    m_Instances = new Instances(m_Instances, 0);
  }


  /**
   * Updates the classifier with the given instance.
   *
   * @param instance the new training instance to include in the model 
   * @exception Exception if the instance could not be incorporated in
   * the model.
   */
  public void updateClassifier(Instance instance) throws Exception {

    if (!instance.classIsMissing()) {
      Enumeration enumAtts = m_Instances.enumerateAttributes();
      int attIndex = 0;
      while (enumAtts.hasMoreElements()) {
	Attribute attribute = (Attribute) enumAtts.nextElement();
	if (!instance.isMissing(attribute)) {
	  m_Distributions[attIndex][(int)instance.classValue()].
	    addValue(instance.value(attribute), instance.weight());
	}
	attIndex++;
      }
      m_ClassDistribution.addValue(instance.classValue(),
				   instance.weight());
    }
  }


  /**
   * Calculates the class membership probabilities for the given test 
   * instance.
   *
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @exception Exception if there is a problem generating the prediction
   */
  public double [] distributionForInstance(Instance instance) 
  throws Exception { 
    
    if (m_UseDiscretization) {
      m_Disc.input(instance);
      instance = m_Disc.output();
    }
    double [] probs = new double[m_NumClasses];
    for (int j = 0; j < m_NumClasses; j++) {
      probs[j] = m_ClassDistribution.getProbability(j);
    }
    Enumeration enumAtts = instance.enumerateAttributes();
    int attIndex = 0;
    while (enumAtts.hasMoreElements()) {
      Attribute attribute = (Attribute) enumAtts.nextElement();
      if (!instance.isMissing(attribute)) {
	double temp, max = 0;
	for (int j = 0; j < m_NumClasses; j++) {
	  temp = Math.max(1e-75, m_Distributions[attIndex][j].
	  getProbability(instance.value(attribute)));
	  probs[j] *= temp;
	  if (probs[j] > max) {
	    max = probs[j];
	  }
	  if (Double.isNaN(probs[j])) {
	    throw new Exception("NaN returned from estimator for attribute "
				+ attribute.name() + ":\n"
				+ m_Distributions[attIndex][j].toString());
	  }
	}
	if ((max > 0) && (max < 1e-75)) { // Danger of probability underflow
	  for (int j = 0; j < m_NumClasses; j++) {
	    probs[j] *= 1e75;
	  }
	}
      }
      attIndex++;
    }

    // Display probabilities
    Utils.normalize(probs);
    return probs;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(2);

    newVector.addElement(
    new Option("\tUse kernel density estimator rather than normal\n"
	       +"\tdistribution for numeric attributes",
	       "K", 0,"-K"));
    newVector.addElement(
    new Option("\tUse supervised discretization to process numeric attributes\n",
	       "D", 0,"-D"));
    return newVector.elements();
  }

  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -K
   *  Use kernel density estimator rather than normal
   *  distribution for numeric attributes</pre>
   * 
   * <pre> -D
   *  Use supervised discretization to process numeric attributes
   * </pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    boolean k = Utils.getFlag('K', options);
    boolean d = Utils.getFlag('D', options);
    if (k && d) {
      throw new IllegalArgumentException("Can't use both kernel density " +
					 "estimation and discretization!");
    }
    setUseSupervisedDiscretization(d);
    setUseKernelEstimator(k);
    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [2];
    int current = 0;

    if (m_UseKernelEstimator) {
      options[current++] = "-K";
    }

    if (m_UseDiscretization) {
      options[current++] = "-D";
    }

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Returns a description of the classifier.
   *
   * @return a description of the classifier as a string.
   */
  public String toString() {
    
    StringBuffer text = new StringBuffer();

    text.append("Naive Bayes Classifier");
    if (m_Instances == null) {
      text.append(": No model built yet.");
    } else {
      try {
	for (int i = 0; i < m_Distributions[0].length; i++) {
	  text.append("\n\nClass " + m_Instances.classAttribute().value(i) +
		      ": Prior probability = " + Utils.
		      doubleToString(m_ClassDistribution.getProbability(i),
				     4, 2) + "\n\n");
	  Enumeration enumAtts = m_Instances.enumerateAttributes();
	  int attIndex = 0;
	  while (enumAtts.hasMoreElements()) {
	    Attribute attribute = (Attribute) enumAtts.nextElement();
	    text.append(attribute.name() + ":  " 
			+ m_Distributions[attIndex][i]);
	    attIndex++;
	  }
	}
      } catch (Exception ex) {
	text.append(ex.getMessage());
      }
    }

    return text.toString();
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String useKernelEstimatorTipText() {
    return "Use a kernel estimator for numeric attributes rather than a "
      +"normal distribution.";
  }
  /**
   * Gets if kernel estimator is being used.
   *
   * @return Value of m_UseKernelEstimatory.
   */
  public boolean getUseKernelEstimator() {
    
    return m_UseKernelEstimator;
  }
  
  /**
   * Sets if kernel estimator is to be used.
   *
   * @param v  Value to assign to m_UseKernelEstimatory.
   */
  public void setUseKernelEstimator(boolean v) {
    
    m_UseKernelEstimator = v;
    if (v) {
      setUseSupervisedDiscretization(false);
    }
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String useSupervisedDiscretizationTipText() {
    return "Use supervised discretization to convert numeric attributes to nominal "
      +"ones.";
  }

  /**
   * Get whether supervised discretization is to be used.
   *
   * @return true if supervised discretization is to be used.
   */
  public boolean getUseSupervisedDiscretization() {
    
    return m_UseDiscretization;
  }
  
  /**
   * Set whether supervised discretization is to be used.
   *
   * @param newblah true if supervised discretization is to be used.
   */
  public void setUseSupervisedDiscretization(boolean newblah) {
    
    m_UseDiscretization = newblah;
    if (newblah) {
      setUseKernelEstimator(false);
    }
  }
  
  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {
    runClassifier(new NaiveBayes(), argv);
  }
}

