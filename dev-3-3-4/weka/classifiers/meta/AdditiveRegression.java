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
 * <pre>
 * Analysing:  Root_relative_squared_error
 * Datasets:   36
 * Resultsets: 2
 * Confidence: 0.05 (two tailed)
 * Date:       10/13/00 10:00 AM
 *
 *
 * Dataset                   (1) m5.M5Prim | (2) AdditiveRegression -S 0.7 \
 *                                         |    -B weka.classifiers.meta.m5.M5Prime 
 *                          ----------------------------
 * auto93.names              (10)    54.4  |    49.41 * 
 * autoHorse.names           (10)    32.76 |    26.34 * 
 * autoMpg.names             (10)    35.32 |    34.84 * 
 * autoPrice.names           (10)    40.01 |    36.57 * 
 * baskball                  (10)    79.46 |    79.85   
 * bodyfat.names             (10)    10.38 |    11.41 v 
 * bolts                     (10)    19.29 |    12.61 * 
 * breastTumor               (10)    96.95 |    96.23 * 
 * cholesterol               (10)   101.03 |    98.88 * 
 * cleveland                 (10)    71.29 |    70.87 * 
 * cloud                     (10)    38.82 |    39.18   
 * cpu                       (10)    22.26 |    14.74 * 
 * detroit                   (10)   228.16 |    83.7  * 
 * echoMonths                (10)    71.52 |    69.15 * 
 * elusage                   (10)    48.94 |    49.03   
 * fishcatch                 (10)    16.61 |    15.36 * 
 * fruitfly                  (10)   100    |   100    * 
 * gascons                   (10)    18.72 |    14.26 * 
 * housing                   (10)    38.62 |    36.53 * 
 * hungarian                 (10)    74.67 |    72.19 * 
 * longley                   (10)    31.23 |    28.26 * 
 * lowbwt                    (10)    62.26 |    61.48 * 
 * mbagrade                  (10)    89.2  |    89.2    
 * meta                      (10)   163.15 |   188.28 v 
 * pbc                       (10)    81.35 |    79.4  * 
 * pharynx                   (10)   105.41 |   105.03   
 * pollution                 (10)    72.24 |    68.16 * 
 * pwLinear                  (10)    32.42 |    33.33 v 
 * quake                     (10)   100.21 |    99.93   
 * schlvote                  (10)    92.41 |    98.23 v 
 * sensory                   (10)    88.03 |    87.94   
 * servo                     (10)    37.07 |    35.5  * 
 * sleep                     (10)    70.17 |    71.65   
 * strike                    (10)    84.98 |    83.96 * 
 * veteran                   (10)    90.61 |    88.77 * 
 * vineyard                  (10)    79.41 |    73.95 * 
 *                        ----------------------------
 *                              (v| |*) |   (4|8|24) 
 *
 * </pre> <p>
 *
 * For more information see: <p>
 *
 * Friedman, J.H. (1999). Stochastic Gradient Boosting. Technical Report
 * Stanford University. http://www-stat.stanford.edu/~jhf/ftp/stobst.ps. <p>
 *
 * Valid options from the command line are: <p>
 * 
 * -B classifierstring <br>
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
 * (default = -1). <p>
 *
 * -D <br>
 * Debugging output. <p>
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.10 $
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
  protected int m_maxModels = -1;

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
	      "B", 1, "-B <classifier specification>"));

    newVector.addElement(new Option(
	      "\tSpecify shrinkage rate. "
	      +"(default=1.0, ie. no shrinkage)\n", 
	      "S", 1, "-S"));

    newVector.addElement(new Option(
	      "\tTurn on debugging output.",
	      "D", 0, "-D"));

    newVector.addElement(new Option(
	      "\tSpecify max models to generate. "
	      +"(default = -1, ie. no max; keep going until error reduction threshold "
	      +"is reached)\n", 
	      "M", 1, "-M"));
     
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
   * (default = -1). <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    setDebug(Utils.getFlag('D', options));

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

    options[current++] = "-B";
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
    newData = residualReplace(newData, zr);
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
      newData = residualReplace(newData, nextC);
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

    // remove last classifier
    m_additiveModels.removeElementAt(m_additiveModels.size()-1);
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
      prediction += (current.classifyInstance(inst) * getShrinkage());
    }

    return prediction;
  }

  /**
   * Replace the class values of the instances from the current iteration
   * with residuals ater predicting with the supplied classifier.
   *
   * @param data the instances to predict
   * @param c the classifier to use
   * @return a new set of instances with class values replaced by residuals
   */
  private Instances residualReplace(Instances data, Classifier c) {
    double pred,residual;
    Instances newInst = new Instances(data);

    for (int i = 0; i < newInst.numInstances(); i++) {
      try {
	pred = c.classifyInstance(newInst.instance(i)) * getShrinkage();
	residual = newInst.instance(i).classValue() - pred;
	//	System.err.println("Residual : "+residual);
	newInst.instance(i).setClassValue(residual);
      } catch (Exception ex) {
	// continue
      }
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
