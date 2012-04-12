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
 *    AdditiveRegression.java
 *    Copyright (C) 2000 Mark Hall
 *
 */

package weka.classifiers.meta;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.DecisionStump;
import weka.classifiers.rules.ZeroR;
import java.io.*;
import java.util.*;
import weka.core.*;
import weka.classifiers.*;

/**
 * Meta classifier that enhances the performance of a regression base
 * classifier. Each iteration fits a model to the residuals left by the
 * classifier on the previous iteration. Prediction is accomplished by
 * adding the predictions of each classifier. Smoothing is accomplished
 * through varying the shrinkage (learning rate) parameter. <p>
 *
 * For more information see: <p>
 *
 * Friedman, J.H. (1999). Stochastic Gradient Boosting. Technical Report
 * Stanford University. http://www-stat.stanford.edu/~jhf/ftp/stobst.ps. <p>
 *
 * Valid options from the command line are: <p>
 * 
 * -W classifierstring <br>
 * Classifierstring should contain the full class name of a classifier.<p>
 *
 * -S shrinkage rate <br>
 * Smaller values help prevent overfitting and have a smoothing effect 
 * (but increase learning time).
 * (default = 1.0, ie no shrinkage). <p>
 *
 * -I max models <br>
 * Set the maximum number of models to generate.
 * (default = 10). <p>
 *
 * -D <br>
 * Debugging output. <p>
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.17 $
 */
public class AdditiveRegression extends IteratedSingleClassifierEnhancer 
  implements OptionHandler,
	     AdditionalMeasureProducer,
	     WeightedInstancesHandler {

  /**
   * Class index.
   */
  private int m_classIndex;

  /**
   * Shrinkage (Learning rate). Default = no shrinkage.
   */
  protected double m_shrinkage = 1.0;

  /** The number of successfully generated base classifiers. */
  protected int m_NumIterationsPerformed;

  /** The model for the mean */
  protected ZeroR m_zeroR;

  /**
   * Returns a string describing this attribute evaluator
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return " Meta classifier that enhances the performance of a regression "
      +"base classifier. Each iteration fits a model to the residuals left "
      +"by the classifier on the previous iteration. Prediction is "
      +"accomplished by adding the predictions of each classifier. "
      +"Reducing the shrinkage (learning rate) parameter helps prevent "
      +"overfitting and has a smoothing effect but increases the learning "
      +"time.  For more information see: Friedman, J.H. (1999). Stochastic "
      +"Gradient Boosting. Technical Report Stanford University. "
      +"http://www-stat.stanford.edu/~jhf/ftp/stobst.ps.";
  }

  /**
   * Default constructor specifying DecisionStump as the classifier
   */
  public AdditiveRegression() {

    this(new weka.classifiers.trees.DecisionStump());
  }

  /**
   * Constructor which takes base classifier as argument.
   *
   * @param classifier the base classifier to use
   */
  public AdditiveRegression(Classifier classifier) {

    m_Classifier = classifier;
  }

  /**
   * String describing default classifier.
   */
  protected String defaultClassifierString() {
    
    return "weka.classifiers.trees.DecisionStump";
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(4);

    newVector.addElement(new Option(
	      "\tSpecify shrinkage rate. "
	      +"(default = 1.0, ie. no shrinkage)\n", 
	      "S", 1, "-S"));

    Enumeration enu = super.listOptions();
    while (enu.hasMoreElements()) {
      newVector.addElement(enu.nextElement());
    }
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -W classifierstring <br>
   * Classifierstring should contain the full class name of a classifier.<p>
   *
   * -S shrinkage rate <br>
   * Smaller values help prevent overfitting and have a smoothing effect 
   * (but increase learning time).
   * (default = 1.0, ie. no shrinkage). <p>
   *
   * -D <br>
   * Debugging output. <p>
   *
   * -I max models <br>
   * Set the maximum number of models to generate. <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String optionString = Utils.getOption('S', options);
    if (optionString.length() != 0) {
      Double temp = Double.valueOf(optionString);
      setShrinkage(temp.doubleValue());
    }

    super.setOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {
    
    String [] superOptions = super.getOptions();
    String [] options = new String [superOptions.length + 2];
    int current = 0;

    options[current++] = "-S"; options[current++] = "" + getShrinkage();

    System.arraycopy(superOptions, 0, options, current, 
		     superOptions.length);

    current += superOptions.length;
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String shrinkageTipText() {
    return "Shrinkage rate. Smaller values help prevent overfitting and "
      + "have a smoothing effect (but increase learning time). "
      +"Default = 1.0, ie. no shrinkage."; 
  }

  /**
   * Set the shrinkage parameter
   *
   * @param l the shrinkage rate.
   */
  public void setShrinkage(double l) {
    m_shrinkage = l;
  }

  /**
   * Get the shrinkage rate.
   *
   * @return the value of the learning rate
   */
  public double getShrinkage() {
    return m_shrinkage;
  }

  /**
   * Build the classifier on the supplied data
   *
   * @param data the training data
   * @exception Exception if the classifier could not be built successfully
   */
  public void buildClassifier(Instances data) throws Exception {

    super.buildClassifier(data);

    if (data.classAttribute().isNominal()) {
      throw new UnsupportedClassTypeException("Class must be numeric!");
    }
    Instances newData = new Instances(data);
    newData.deleteWithMissingClass();
    m_classIndex = newData.classIndex();

    double sum = 0;
    double temp_sum = 0;
    // Add the model for the mean first
    m_zeroR = new ZeroR();
    m_zeroR.buildClassifier(newData);
    newData = residualReplace(newData, m_zeroR, false);
    for (int i = 0; i < newData.numInstances(); i++) {
      sum += newData.instance(i).weight() *
	newData.instance(i).classValue() * newData.instance(i).classValue();
    }
    if (m_Debug) {
      System.err.println("Sum of squared residuals "
			 +"(predicting the mean) : " + sum);
    }

    m_NumIterationsPerformed = 0;
    do {
      temp_sum = sum;

      // Build the classifier
      m_Classifiers[m_NumIterationsPerformed].buildClassifier(newData);

      newData = residualReplace(newData, m_Classifiers[m_NumIterationsPerformed], true);
      sum = 0;
      for (int i = 0; i < newData.numInstances(); i++) {
	sum += newData.instance(i).weight() *
	  newData.instance(i).classValue() * newData.instance(i).classValue();
      }
      if (m_Debug) {
	System.err.println("Sum of squared residuals : "+sum);
      }
      m_NumIterationsPerformed++;
    } while (((temp_sum - sum) > Utils.SMALL) && 
	     (m_NumIterationsPerformed < m_Classifiers.length));
  }

  /**
   * Classify an instance.
   *
   * @param inst the instance to predict
   * @return a prediction for the instance
   * @exception Exception if an error occurs
   */
  public double classifyInstance(Instance inst) throws Exception {

    double prediction = m_zeroR.classifyInstance(inst);

    for (int i = 0; i < m_NumIterationsPerformed; i++) {
      double toAdd = m_Classifiers[i].classifyInstance(inst);
      toAdd *= getShrinkage();
      prediction += toAdd;
    }

    return prediction;
  }

  /**
   * Replace the class values of the instances from the current iteration
   * with residuals ater predicting with the supplied classifier.
   *
   * @param data the instances to predict
   * @param c the classifier to use
   * @param useShrinkage whether shrinkage is to be applied to the model's output
   * @return a new set of instances with class values replaced by residuals
   */
  private Instances residualReplace(Instances data, Classifier c, 
				    boolean useShrinkage) throws Exception {
    double pred,residual;
    Instances newInst = new Instances(data);

    for (int i = 0; i < newInst.numInstances(); i++) {
      pred = c.classifyInstance(newInst.instance(i));
      if (useShrinkage) {
	pred *= getShrinkage();
      }
      residual = newInst.instance(i).classValue() - pred;
      newInst.instance(i).setClassValue(residual);
    }
    //    System.err.print(newInst);
    return newInst;
  }

  /**
   * Returns an enumeration of the additional measure names
   * @return an enumeration of the measure names
   */
  public Enumeration enumerateMeasures() {
    Vector newVector = new Vector(1);
    newVector.addElement("measureNumIterations");
    return newVector.elements();
  }

  /**
   * Returns the value of the named measure
   * @param measureName the name of the measure to query for its value
   * @return the value of the named measure
   * @exception IllegalArgumentException if the named measure is not supported
   */
  public double getMeasure(String additionalMeasureName) {
    if (additionalMeasureName.compareToIgnoreCase("measureNumIterations") == 0) {
      return measureNumIterations();
    } else {
      throw new IllegalArgumentException(additionalMeasureName 
			  + " not supported (AdditiveRegression)");
    }
  }

  /**
   * return the number of iterations (base classifiers) completed
   * @return the number of iterations (same as number of base classifier
   * models)
   */
  public double measureNumIterations() {
    return m_NumIterationsPerformed;
  }

  /**
   * Returns textual description of the classifier.
   *
   * @return a description of the classifier as a string
   */
  public String toString() {
    StringBuffer text = new StringBuffer();

    if (m_NumIterations == 0) {
      return "Classifier hasn't been built yet!";
    }

    text.append("Additive Regression\n\n");

    text.append("ZeroR model\n\n" + m_zeroR + "\n\n");

    text.append("Base classifier " 
		+ getClassifier().getClass().getName()
		+ "\n\n");
    text.append("" + m_NumIterationsPerformed + " models generated.\n");

    for (int i = 0; i < m_NumIterationsPerformed; i++) {
      text.append("\nModel number " + i + "\n\n" +
		  m_Classifiers[i] + "\n");
    }

    return text.toString();
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain the following arguments:
   * -t training file [-T test file] [-c class index]
   */
  public static void main(String [] argv) {

    try {
      System.out.println(Evaluation.evaluateModel(new AdditiveRegression(),
						  argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
}
