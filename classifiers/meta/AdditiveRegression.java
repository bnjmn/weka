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
import weka.classifiers.meta.*;

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
 * Classifierstring should contain the full class name of a classifier
 * followed by options to the classifier.
 * (required).<p>
 *
 * -S shrinkage rate <br>
 * Smaller values help prevent overfitting and have a smoothing effect 
 * (but increase learning time).
 * (default = 1.0, ie no shrinkage). <p>
 *
 * -M max models <br>
 * Set the maximum number of models to generate. Values <= 0 indicate 
 * no maximum, ie keep going until the reduction in error threshold is 
 * reached.
 * (default = 10). <p>
 *
 * -D <br>
 * Debugging output. <p>
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.14 $
 */
public class AdditiveRegression extends Classifier 
  implements OptionHandler,
	     AdditionalMeasureProducer,
	     WeightedInstancesHandler {
  
  /**
   * Base classifier.
   */
  protected Classifier m_Classifier = new weka.classifiers.trees.DecisionStump();

  /**
   * Class index.
   */
  private int m_classIndex;

  /**
   * Shrinkage (Learning rate). Default = no shrinkage.
   */
  protected double m_shrinkage = 1.0;

  
  /**
   * The list of iteratively generated models.
   */
  private FastVector m_additiveModels = new FastVector();

  /**
   * Produce debugging output.
   */
  private boolean m_debug = false;

  /**
   * Maximum number of models to produce. -1 indicates keep going until the error
   * threshold is met.
   */
  protected int m_maxModels = 10;

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
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(4);

    newVector.addElement(new Option(
	      "\tFull class name of classifier to use, followed\n"
	      + "\tby scheme options. (required)\n"
	      + "\teg: \"weka.classifiers.bayes.NaiveBayes -D\"",
	      "W", 1, "-W <classifier specification>"));

    newVector.addElement(new Option(
	      "\tSpecify shrinkage rate. "
	      +"(default=1.0, ie. no shrinkage)\n", 
	      "S", 1, "-S"));

    newVector.addElement(new Option(
	      "\tTurn on debugging output.",
	      "D", 0, "-D"));

    newVector.addElement(new Option(
	      "\tSpecify max models to generate. "
	      +"(default = 10, ie. no max; keep going until error reduction threshold "
	      +"is reached)\n", 
	      "M", 1, "-M"));
     
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -W classifierstring <br>
   * Classifierstring should contain the full class name of a classifier
   * followed by options to the classifier.
   * (required).<p>
   *
   * -S shrinkage rate <br>
   * Smaller values help prevent overfitting and have a smoothing effect 
   * (but increase learning time).
   * (default = 1.0, ie. no shrinkage). <p>
   *
   * -D <br>
   * Debugging output. <p>
   *
   * -M max models <br>
   * Set the maximum number of models to generate. Values <= 0 indicate 
   * no maximum, ie keep going until the reduction in error threshold is 
   * reached.
   * (default = 10). <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    setDebug(Utils.getFlag('D', options));

    String classifierString = Utils.getOption('W', options);
    if (classifierString.length() == 0) {
      throw new Exception("A classifier must be specified"
			  + " with the -w option.");
    }
    String [] classifierSpec = Utils.splitOptions(classifierString);
    if (classifierSpec.length == 0) {
      throw new Exception("Invalid classifier specification string");
    }
    String classifierName = classifierSpec[0];
    classifierSpec[0] = "";
    setClassifier(Classifier.forName(classifierName, classifierSpec));

    String optionString = Utils.getOption('S', options);
    if (optionString.length() != 0) {
      Double temp;
      temp = Double.valueOf(optionString);
      setShrinkage(temp.doubleValue());
    }

    optionString = Utils.getOption('M', options);
    if (optionString.length() != 0) {
      setMaxModels(Integer.parseInt(optionString));
    }
    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {
    
    String [] options = new String [7];
    int current = 0;

    if (getDebug()) {
      options[current++] = "-D";
    }

    options[current++] = "-W";
    options[current++] = "" + getClassifierSpec();

    options[current++] = "-S"; options[current++] = ""+getShrinkage();
    options[current++] = "-M"; options[current++] = ""+getMaxModels();

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
  public String debugTipText() {
    return "Turn on debugging output";
  }

  /**
   * Set whether debugging output is produced.
   *
   * @param d true if debugging output is to be produced
   */
  public void setDebug(boolean d) {
    m_debug = d;
  }

  /**
   * Gets whether debugging has been turned on
   *
   * @return true if debugging has been turned on
   */
  public boolean getDebug() {
    return m_debug;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String classifierTipText() {
    return "Classifier to use";
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
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String maxModelsTipText() {
    return "Max models to generate. <= 0 indicates no maximum, ie. continue until "
      +"error reduction threshold is reached.";
  }

  /**
   * Set the maximum number of models to generate
   * @param maxM the maximum number of models
   */
  public void setMaxModels(int maxM) {
    m_maxModels = maxM;
  }

  /**
   * Get the max number of models to generate
   * @return the max number of models to generate
   */
  public int getMaxModels() {
    return m_maxModels;
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
     m_additiveModels = new FastVector();

    if (m_Classifier == null) {
      throw new Exception("No base classifiers have been set!");
    }
    if (data.classAttribute().isNominal()) {
      throw new UnsupportedClassTypeException("Class must be numeric!");
    }
    Instances newData = new Instances(data);
    newData.deleteWithMissingClass();
    m_classIndex = newData.classIndex();

    double sum = 0;
    double temp_sum = 0;
    // Add the model for the mean first
    ZeroR zr = new ZeroR();
    zr.buildClassifier(newData);
    m_additiveModels.addElement(zr);
    newData = residualReplace(newData, zr, false);
    for (int i = 0; i < newData.numInstances(); i++) {
      sum += newData.instance(i).weight() *
	newData.instance(i).classValue() *
	newData.instance(i).classValue();
    }
    if (m_debug) {
      System.err.println("Sum of squared residuals "
			 +"(predicting the mean) : "+sum);
    }

    int modelCount = 0;
    do {
      temp_sum = sum;
      Classifier nextC = Classifier.makeCopies(m_Classifier, 1)[0];
      nextC.buildClassifier(newData);
      m_additiveModels.addElement(nextC);
      newData = residualReplace(newData, nextC, true);
      sum = 0;
      for (int i = 0; i < newData.numInstances(); i++) {
	sum += newData.instance(i).weight() *
	  newData.instance(i).classValue() *
	  newData.instance(i).classValue();
      }
      if (m_debug) {
	System.err.println("Sum of squared residuals : "+sum);
      }
      modelCount++;
    } while (((temp_sum - sum) > Utils.SMALL) && 
	     (m_maxModels > 0 ? (modelCount < m_maxModels) : true));
  }

  /**
   * Classify an instance.
   *
   * @param inst the instance to predict
   * @return a prediction for the instance
   * @exception Exception if an error occurs
   */
  public double classifyInstance(Instance inst) throws Exception {
    double prediction = 0;

    for (int i = 0; i < m_additiveModels.size(); i++) {
      Classifier current = (Classifier)m_additiveModels.elementAt(i);
      double toAdd = current.classifyInstance(inst);
      if (i > 0) {
	toAdd *= getShrinkage();
      }
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
    if (additionalMeasureName.compareTo("measureNumIterations") == 0) {
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
    return m_additiveModels.size();
  }

  /**
   * Returns textual description of the classifier.
   *
   * @return a description of the classifier as a string
   */
  public String toString() {
    StringBuffer text = new StringBuffer();

    if (m_additiveModels.size() == 0) {
      return "Classifier hasn't been built yet!";
    }

    text.append("Additive Regression\n\n");
    text.append("Base classifier " 
		+ getClassifier().getClass().getName()
		+ "\n\n");
    text.append(""+m_additiveModels.size()+" models generated.\n");

    for (int i = 0; i < m_additiveModels.size(); i++) {
      text.append("\nModel number " + i + "\n\n" +
		  m_additiveModels.elementAt(i) + "\n");
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
      System.err.println(e.getMessage());
    }
  }
}
