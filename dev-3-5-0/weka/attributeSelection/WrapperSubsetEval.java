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
 *    WrapperSubsetEval.java
 *    Copyright (C) 1999 Mark Hall
 *
 */

package  weka.attributeSelection;

import  java.util.*;
import  weka.core.*;
import  weka.classifiers.*;
import  weka.classifiers.rules.ZeroR;
import  weka.filters.unsupervised.attribute.Remove;
import  weka.filters.Filter;

/** 
 * Wrapper attribute subset evaluator. <p>
 * For more information see: <br>
 * 
 * Kohavi, R., John G., Wrappers for Feature Subset Selection. 
 * In <i>Artificial Intelligence journal</i>, special issue on relevance, 
 * Vol. 97, Nos 1-2, pp.273-324. <p>
 *
 * Valid options are:<p>
 *
 * -B <base learner> <br>
 * Class name of base learner to use for accuracy estimation.
 * Place any classifier options last on the command line following a
 * "--". Eg  -B weka.classifiers.bayes.NaiveBayes ... -- -K <p>
 *
 * -F <num> <br>
 * Number of cross validation folds to use for estimating accuracy.
 * <default=5> <p>
 *
 * -T <num> <br>
 * Threshold by which to execute another cross validation (standard deviation
 * ---expressed as a percentage of the mean). <p>
 *
 * -R <seed> <br>
 * Seed for cross validation accuracy estimation.
 * (default = 1) <p>
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.24 $
 */
public class WrapperSubsetEval
  extends SubsetEvaluator
  implements OptionHandler
{

  /** training instances */
  private Instances m_trainInstances;
  /** class index */
  private int m_classIndex;
  /** number of attributes in the training data */
  private int m_numAttribs;
  /** number of instances in the training data */
  private int m_numInstances;
  /** holds an evaluation object */
  private Evaluation m_Evaluation;
  /** holds the base classifier object */
  private Classifier m_BaseClassifier;
  /** number of folds to use for cross validation */
  private int m_folds;
  /** random number seed */
  private int m_seed;
  /** 
   * the threshold by which to do further cross validations when
   * estimating the accuracy of a subset
   */
  private double m_threshold;

  /**
   * Returns a string describing this attribute evaluator
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "WrapperSubsetEval:\n\n"
      +"Evaluates attribute sets by using a learning scheme. Cross "
      +"validation is used to estimate the accuracy of the learning "
      +"scheme for a set of attributes.\n";
  }

  /**
   * Constructor. Calls restOptions to set default options
   **/
  public WrapperSubsetEval () {
    resetOptions();
  }


  /**
   * Returns an enumeration describing the available options.
   * @return an enumeration of all the available options.
   **/
  public Enumeration listOptions () {
    Vector newVector = new Vector(4);
    newVector.addElement(new Option("\tclass name of base learner to use for" 
				    + "\n\taccuracy estimation. Place any" 
				    + "\n\tclassifier options LAST on the" 
				    + "\n\tcommand line following a \"--\"." 
				    + "\n\teg. -B weka.classifiers.bayes.NaiveBayes ... " 
				    + "-- -K", "B", 1, "-B <base learner>"));
    newVector.addElement(new Option("\tnumber of cross validation folds to " 
				    + "use\n\tfor estimating accuracy." 
				    + "\n\t(default=5)", "F", 1, "-F <num>"));
    newVector.addElement(new Option("\tSeed for cross validation accuracy "
				    +"\n\testimation."
				    +"\n\t(default = 1)", "R", 1,"-R <seed>"));
    newVector.addElement(new Option("\tthreshold by which to execute " 
				    + "another cross validation" 
				    + "\n\t(standard deviation---" 
				    + "expressed as a percentage of the " 
				    + "mean).\n\t(default=0.01(1%))"
				    , "T", 1, "-T <num>"));

    if ((m_BaseClassifier != null) && 
	(m_BaseClassifier instanceof OptionHandler)) {
      newVector.addElement(new Option("", "", 0, "\nOptions specific to" 
				      + "scheme " 
				      + m_BaseClassifier.getClass().getName() 
				      + ":"));
      Enumeration enu = ((OptionHandler)m_BaseClassifier).listOptions();

      while (enu.hasMoreElements()) {
        newVector.addElement(enu.nextElement());
      }
    }

    return  newVector.elements();
  }


  /**
   * Parses a given list of options.
   *
   * Valid options are:<p>
   *
   * -B <base learner> <br>
   * Class name of base learner to use for accuracy estimation.
   * Place any classifier options last on the command line following a
   * "--". Eg  -B weka.classifiers.bayes.NaiveBayes ... -- -K <p>
   *
   * -F <num> <br>
   * Number of cross validation folds to use for estimating accuracy.
   * <default=5> <p>
   *
   * -T <num> <br>
   * Threshold by which to execute another cross validation (standard deviation
   * ---expressed as a percentage of the mean). <p>
   *
   * -R <seed> <br>
   * Seed for cross validation accuracy estimation.
   * (default = 1) <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   *
   **/
  public void setOptions (String[] options)
    throws Exception {
    String optionString;
    resetOptions();
    optionString = Utils.getOption('B', options);

    if (optionString.length() == 0) {
      throw  new Exception("A learning scheme must be specified with" 
			   + "-B option");
    }

    setClassifier(Classifier.forName(optionString, 
				     Utils.partitionOptions(options)));
    optionString = Utils.getOption('F', options);

    if (optionString.length() != 0) {
      setFolds(Integer.parseInt(optionString));
    }

    optionString = Utils.getOption('R', options);
    if (optionString.length() != 0) {
      setSeed(Integer.parseInt(optionString));
    }

    //       optionString = Utils.getOption('S',options);
    //       if (optionString.length() != 0)
    //         {
    //  	 seed = Integer.parseInt(optionString);
    //         }
    optionString = Utils.getOption('T', options);

    if (optionString.length() != 0) {
      Double temp;
      temp = Double.valueOf(optionString);
      setThreshold(temp.doubleValue());
    }
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String thresholdTipText() {
    return "Repeat xval if stdev of mean exceeds this value.";
  }

  /**
   * Set the value of the threshold for repeating cross validation
   *
   * @param t the value of the threshold
   */
  public void setThreshold (double t) {
    m_threshold = t;
  }


  /**
   * Get the value of the threshold
   *
   * @return the threshold as a double
   */
  public double getThreshold () {
    return  m_threshold;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String foldsTipText() {
    return "Number of xval folds to use when estimating subset accuracy.";
  }

  /**
   * Set the number of folds to use for accuracy estimation
   *
   * @param f the number of folds
   */
  public void setFolds (int f) {
    m_folds = f;
  }


  /**
   * Get the number of folds used for accuracy estimation
   *
   * @return the number of folds
   */
  public int getFolds () {
    return  m_folds;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String seedTipText() {
    return "Seed to use for randomly generating xval splits.";
  }

  /**
   * Set the seed to use for cross validation
   *
   * @param s the seed
   */
  public void setSeed (int s) {
    m_seed = s;
  }


  /**
   * Get the random number seed used for cross validation
   *
   * @return the seed
   */
  public int getSeed () {
    return  m_seed;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String classifierTipText() {
    return "Classifier to use for estimating the accuracy of subsets";
  }

  /**
   * Set the classifier to use for accuracy estimation
   *
   * @param newClassifier the Classifier to use.
   */
  public void setClassifier (Classifier newClassifier) {
    m_BaseClassifier = newClassifier;
  }


  /**
   * Get the classifier used as the base learner.
   *
   * @return the classifier used as the classifier
   */
  public Classifier getClassifier () {
    return  m_BaseClassifier;
  }


  /**
   * Gets the current settings of WrapperSubsetEval.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions () {
    String[] classifierOptions = new String[0];

    if ((m_BaseClassifier != null) && 
	(m_BaseClassifier instanceof OptionHandler)) {
      classifierOptions = ((OptionHandler)m_BaseClassifier).getOptions();
    }

    String[] options = new String[9 + classifierOptions.length];
    int current = 0;

    if (getClassifier() != null) {
      options[current++] = "-B";
      options[current++] = getClassifier().getClass().getName();
    }

    options[current++] = "-F";
    options[current++] = "" + getFolds();
    options[current++] = "-T";
    options[current++] = "" + getThreshold();
    options[current++] = "-R";
    options[current++] = "" + getSeed();
    options[current++] = "--";
    System.arraycopy(classifierOptions, 0, options, current, 
		     classifierOptions.length);
    current += classifierOptions.length;

    while (current < options.length) {
      options[current++] = "";
    }

    return  options;
  }


  protected void resetOptions () {
    m_trainInstances = null;
    m_Evaluation = null;
    m_BaseClassifier = new ZeroR();
    m_folds = 5;
    m_seed = 1;
    m_threshold = 0.01;
  }


  /**
   * Generates a attribute evaluator. Has to initialize all fields of the 
   * evaluator that are not being set via options.
   *
   * @param data set of instances serving as training data 
   * @exception Exception if the evaluator has not been 
   * generated successfully
   */
  public void buildEvaluator (Instances data)
    throws Exception {
    if (data.checkForStringAttributes()) {
      throw  new UnsupportedAttributeTypeException("Can't handle string attributes!");
    }

    m_trainInstances = data;
    m_classIndex = m_trainInstances.classIndex();
    m_numAttribs = m_trainInstances.numAttributes();
    m_numInstances = m_trainInstances.numInstances();
  }


  /**
   * Evaluates a subset of attributes
   *
   * @param subset a bitset representing the attribute subset to be 
   * evaluated 
   * @exception Exception if the subset could not be evaluated
   */
  public double evaluateSubset (BitSet subset)
    throws Exception {
    double errorRate = 0;
    double[] repError = new double[5];
    boolean ok = true;
    int numAttributes = 0;
    int i, j;
    Random Rnd = new Random(m_seed);
    Remove delTransform = new Remove();
    delTransform.setInvertSelection(true);
    // copy the instances
    Instances trainCopy = new Instances(m_trainInstances);

    // count attributes set in the BitSet
    for (i = 0; i < m_numAttribs; i++) {
      if (subset.get(i)) {
        numAttributes++;
      }
    }

    // set up an array of attribute indexes for the filter (+1 for the class)
    int[] featArray = new int[numAttributes + 1];

    for (i = 0, j = 0; i < m_numAttribs; i++) {
      if (subset.get(i)) {
        featArray[j++] = i;
      }
    }

    featArray[j] = m_classIndex;
    delTransform.setAttributeIndicesArray(featArray);
    delTransform.setInputFormat(trainCopy);
    trainCopy = Filter.useFilter(trainCopy, delTransform);

    // max of 5 repititions ofcross validation
    for (i = 0; i < 5; i++) {
      m_Evaluation = new Evaluation(trainCopy);
      m_Evaluation.crossValidateModel(m_BaseClassifier, trainCopy, m_folds, Rnd);
      repError[i] = m_Evaluation.errorRate();

      // check on the standard deviation
      if (!repeat(repError, i + 1)) {
        i++;
        break;
      }
    }

    for (j = 0; j < i; j++) {
      errorRate += repError[j];
    }

    errorRate /= (double)i;
    m_Evaluation = null;
    return  -errorRate;
  }


  /**
   * Returns a string describing the wrapper
   *
   * @return the description as a string
   */
  public String toString () {
    StringBuffer text = new StringBuffer();

    if (m_trainInstances == null) {
      text.append("\tWrapper subset evaluator has not been built yet\n");
    }
    else {
      text.append("\tWrapper Subset Evaluator\n");
      text.append("\tLearning scheme: " 
		  + getClassifier().getClass().getName() + "\n");
      text.append("\tScheme options: ");
      String[] classifierOptions = new String[0];

      if (m_BaseClassifier instanceof OptionHandler) {
        classifierOptions = ((OptionHandler)m_BaseClassifier).getOptions();

        for (int i = 0; i < classifierOptions.length; i++) {
          text.append(classifierOptions[i] + " ");
        }
      }

      text.append("\n");
      if (m_trainInstances.attribute(m_classIndex).isNumeric()) {
	text.append("\tAccuracy estimation: RMSE\n");
      } else {
	text.append("\tAccuracy estimation: classification error\n");
      }
      
      text.append("\tNumber of folds for accuracy estimation: " 
		  + m_folds 
		  + "\n");
    }

    return  text.toString();
  }


  /**
   * decides whether to do another repeat of cross validation. If the
   * standard deviation of the cross validations
   * is greater than threshold% of the mean (default 1%) then another 
   * repeat is done. 
   *
   * @param repError an array of cross validation results
   * @param entries the number of cross validations done so far
   * @return true if another cv is to be done
   */
  private boolean repeat (double[] repError, int entries) {
    int i;
    double mean = 0;
    double variance = 0;

    if (entries == 1) {
      return  true;
    }

    for (i = 0; i < entries; i++) {
      mean += repError[i];
    }

    mean /= (double)entries;

    for (i = 0; i < entries; i++) {
      variance += ((repError[i] - mean)*(repError[i] - mean));
    }

    variance /= (double)entries;

    if (variance > 0) {
      variance = Math.sqrt(variance);
    }

    if ((variance/mean) > m_threshold) {
      return  true;
    }

    return  false;
  }


  /**
   * Main method for testing this class.
   *
   * @param args the options
   */
  public static void main (String[] args) {
    try {
      System.out.println(AttributeSelection.
			 SelectAttributes(new WrapperSubsetEval(), args));
    }
    catch (Exception e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }
  }

}

