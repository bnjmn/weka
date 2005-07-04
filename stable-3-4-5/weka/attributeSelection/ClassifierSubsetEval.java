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
 *    ClassifierSubsetEval.java
 *    Copyright (C) 2000 Mark Hall
 *
 */

package weka.attributeSelection;

import java.io.*;
import java.util.*;
import weka.core.*;
import weka.classifiers.*;
import weka.classifiers.rules.ZeroR;
import weka.classifiers.Evaluation;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;


/**
 * Classifier subset evaluator. Uses a classifier to estimate the "merit"
 * of a set of attributes.
 *
 * Valid options are:<p>
 *
 * -B <classifier> <br>
 * Class name of the classifier to use for accuracy estimation.
 * Place any classifier options last on the command line following a
 * "--". Eg  -B weka.classifiers.bayes.NaiveBayes ... -- -K <p>
 *
 * -T <br>
 * Use the training data for accuracy estimation rather than a hold out/
 * test set. <p>
 *
 * -H <filename> <br>
 * The file containing hold out/test instances to use for accuracy estimation
 * <p>
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.12.2.1 $
 */
public class ClassifierSubsetEval 
  extends HoldOutSubsetEvaluator
  implements OptionHandler, ErrorBasedMeritEvaluator {

  /** training instances */
  private Instances m_trainingInstances;

  /** class index */
  private int m_classIndex;

  /** number of attributes in the training data */
  private int m_numAttribs;
  
  /** number of training instances */
  private int m_numInstances;

  /** holds the classifier to use for error estimates */
  private Classifier m_Classifier = new ZeroR();

  /** holds the evaluation object to use for evaluating the classifier */
  private Evaluation m_Evaluation;

  /** the file that containts hold out/test instances */
  private File m_holdOutFile = new File("Click to set hold out or "
					+"test instances");

  /** the instances to test on */
  private Instances m_holdOutInstances = null;

  /** evaluate on training data rather than seperate hold out/test set */
  private boolean m_useTraining = true;

  /**
   * Returns a string describing this attribute evaluator
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Evaluates attribute subsets on training data or a seperate "
      +"hold out testing set";
  }

  /**
   * Returns an enumeration describing the available options. <p>
   *
   * -B <classifier> <br>
   * Class name of the classifier to use for accuracy estimation.
   * Place any classifier options last on the command line following a
   * "--". Eg  -B weka.classifiers.bayes.NaiveBayes ... -- -K <p>
   *
   * -T <br>
   * Use the training data for accuracy estimation rather than a hold out/
   * test set. <p>
   *
   * -H <filename> <br>
   * The file containing hold out/test instances to use for accuracy estimation
   * <p>
   *
   * @return an enumeration of all the available options.
   **/
  public Enumeration listOptions () {
    Vector newVector = new Vector(3);
    newVector.addElement(new Option("\tclass name of the classifier to use for" 
				    + "\n\taccuracy estimation. Place any" 
				    + "\n\tclassifier options LAST on the" 
				    + "\n\tcommand line following a \"--\"." 
				    + "\n\teg. -C weka.classifiers.bayes.NaiveBayes ... " 
				    + "-- -K", "B", 1, "-B <classifier>"));
    
    newVector.addElement(new Option("\tUse the training data to estimate"
				    +" accuracy."
				    ,"T",0,"-T"));
    
    newVector.addElement(new Option("\tName of the hold out/test set to "
				    +"\n\testimate accuracy on."
				    ,"H", 1,"-H <filename>"));

    if ((m_Classifier != null) && 
	(m_Classifier instanceof OptionHandler)) {
      newVector.addElement(new Option("", "", 0, "\nOptions specific to " 
				      + "scheme " 
				      + m_Classifier.getClass().getName() 
				      + ":"));
      Enumeration enu = ((OptionHandler)m_Classifier).listOptions();

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
   * -C <classifier> <br>
   * Class name of classifier to use for accuracy estimation.
   * Place any classifier options last on the command line following a
   * "--". Eg  -B weka.classifiers.bayes.NaiveBayes ... -- -K <p>
   *
   * -T <br>
   * Use training data instead of a hold out/test set for accuracy estimation.
   * <p>
   *
   * -H <filname> <br>
   * Name of the hold out/test set to estimate classifier accuracy on.
   * <p>
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
      throw new Exception("A classifier must be specified with -B option");
    }

    setClassifier(Classifier.forName(optionString,
				     Utils.partitionOptions(options)));

    optionString = Utils.getOption('H',options);
    if (optionString.length() != 0) {
      setHoldOutFile(new File(optionString));
    }

    setUseTraining(Utils.getFlag('T',options));
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
    m_Classifier = newClassifier;
  }


  /**
   * Get the classifier used as the base learner.
   *
   * @return the classifier used as the classifier
   */
  public Classifier getClassifier () {
    return  m_Classifier;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String holdOutFileTipText() {
    return "File containing hold out/test instances.";
  }

  /**
   * Gets the file that holds hold out/test instances.
   * @return File that contains hold out instances
   */
  public File getHoldOutFile() {
    return m_holdOutFile;
  }


  /**
   * Set the file that contains hold out/test instances
   * @param h the hold out file
   */
  public void setHoldOutFile(File h) {
    m_holdOutFile = h;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String useTrainingTipText() {
    return "Use training data instead of hold out/test instances.";
  }

  /**
   * Get if training data is to be used instead of hold out/test data
   * @return true if training data is to be used instead of hold out data
   */
  public boolean getUseTraining() {
    return m_useTraining;
  }

  /**
   * Set if training data is to be used instead of hold out/test data
   * @return true if training data is to be used instead of hold out data
   */
  public void setUseTraining(boolean t) {
    m_useTraining = t;
  }

  /**
   * Gets the current settings of ClassifierSubsetEval
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions () {
    String[] classifierOptions = new String[0];

    if ((m_Classifier != null) && 
	(m_Classifier instanceof OptionHandler)) {
      classifierOptions = ((OptionHandler)m_Classifier).getOptions();
    }

    String[] options = new String[6 + classifierOptions.length];
    int current = 0;

    if (getClassifier() != null) {
      options[current++] = "-B";
      options[current++] = getClassifier().getClass().getName();
    }

    if (getUseTraining()) {
      options[current++] = "-T";
    }
    options[current++] = "-H"; options[current++] = getHoldOutFile().getPath();
    options[current++] = "--";
    System.arraycopy(classifierOptions, 0, options, current, 
		     classifierOptions.length);
    current += classifierOptions.length;
        while (current < options.length) {
      options[current++] = "";
    }

    return  options;
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

    m_trainingInstances = data;
    m_classIndex = m_trainingInstances.classIndex();
    m_numAttribs = m_trainingInstances.numAttributes();
    m_numInstances = m_trainingInstances.numInstances();

    // load the testing data
    if (!m_useTraining && 
	(!getHoldOutFile().getPath().startsWith("Click to set"))) {
      java.io.Reader r = new java.io.BufferedReader(
			 new java.io.FileReader(getHoldOutFile().getPath()));
	m_holdOutInstances = new Instances(r);
	m_holdOutInstances.setClassIndex(m_trainingInstances.classIndex());
	if (m_trainingInstances.equalHeaders(m_holdOutInstances) == false) {
	  throw new Exception("Hold out/test set is not compatable with "
			      +"training data.");
	}
    }
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
    int i,j;
    double errorRate = 0;
    int numAttributes = 0;
    Instances trainCopy=null;
    Instances testCopy=null;

    Remove delTransform = new Remove();
    delTransform.setInvertSelection(true);
    // copy the training instances
    trainCopy = new Instances(m_trainingInstances);
    
    if (!m_useTraining) {
      if (m_holdOutInstances == null) {
	throw new Exception("Must specify a set of hold out/test instances "
			    +"with -H");
      } 
      // copy the test instances
      testCopy = new Instances(m_holdOutInstances);
    }
    
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
    if (!m_useTraining) {
      testCopy = Filter.useFilter(testCopy, delTransform);
    }

    // build the classifier
    m_Classifier.buildClassifier(trainCopy);

    m_Evaluation = new Evaluation(trainCopy);
    if (!m_useTraining) {
      m_Evaluation.evaluateModel(m_Classifier, testCopy);
    } else {
      m_Evaluation.evaluateModel(m_Classifier, trainCopy);
    }

    if (m_trainingInstances.classAttribute().isNominal()) {
      errorRate = m_Evaluation.errorRate();
    } else {
      errorRate = m_Evaluation.meanAbsoluteError();
    }

    m_Evaluation = null;
    // return the negative of the error rate as search methods  need to
    // maximize something
    return -errorRate;
  }

  /**
   * Evaluates a subset of attributes with respect to a set of instances.
   * Calling this function overides any test/hold out instancs set from
   * setHoldOutFile.
   * @param subset a bitset representing the attribute subset to be
   * evaluated
   * @param holdOut a set of instances (possibly seperate and distinct
   * from those use to build/train the evaluator) with which to
   * evaluate the merit of the subset
   * @return the "merit" of the subset on the holdOut data
   * @exception Exception if the subset cannot be evaluated
   */
  public double evaluateSubset(BitSet subset, Instances holdOut) 
    throws Exception {
    int i,j;
    double errorRate;
    int numAttributes = 0;
    Instances trainCopy=null;
    Instances testCopy=null;

    if (m_trainingInstances.equalHeaders(holdOut) == false) {
      throw new Exception("evaluateSubset : Incompatable instance types.");
    }

    Remove delTransform = new Remove();
    delTransform.setInvertSelection(true);
    // copy the training instances
    trainCopy = new Instances(m_trainingInstances);
    
    testCopy = new Instances(holdOut);

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
    testCopy = Filter.useFilter(testCopy, delTransform);

    // build the classifier
    m_Classifier.buildClassifier(trainCopy);

    m_Evaluation = new Evaluation(trainCopy);
    m_Evaluation.evaluateModel(m_Classifier, testCopy);

    if (m_trainingInstances.classAttribute().isNominal()) {
      errorRate = m_Evaluation.errorRate();
    } else {
      errorRate = m_Evaluation.meanAbsoluteError();
    }

    m_Evaluation = null;
    // return the negative of the error as search methods need to
    // maximize something
   return -errorRate;
  }

  /**
   * Evaluates a subset of attributes with respect to a single instance.
   * Calling this function overides any hold out/test instances set
   * through setHoldOutFile.
   * @param subset a bitset representing the attribute subset to be
   * evaluated
   * @param holdOut a single instance (possibly not one of those used to
   * build/train the evaluator) with which to evaluate the merit of the subset
   * @param retrain true if the classifier should be retrained with respect
   * to the new subset before testing on the holdOut instance.
   * @return the "merit" of the subset on the holdOut instance
   * @exception Exception if the subset cannot be evaluated
   */
  public double evaluateSubset(BitSet subset, Instance holdOut,
			       boolean retrain) 
    throws Exception {
    int i,j;
    double error;
    int numAttributes = 0;
    Instances trainCopy=null;
    Instance testCopy=null;

    if (m_trainingInstances.equalHeaders(holdOut.dataset()) == false) {
      throw new Exception("evaluateSubset : Incompatable instance types.");
    }

    Remove delTransform = new Remove();
    delTransform.setInvertSelection(true);
    // copy the training instances
    trainCopy = new Instances(m_trainingInstances);
    
    testCopy = (Instance)holdOut.copy();

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

    if (retrain) {
      trainCopy = Filter.useFilter(trainCopy, delTransform);
      // build the classifier
      m_Classifier.buildClassifier(trainCopy);
    }

    delTransform.input(testCopy);
    testCopy = delTransform.output();

    double pred;
    double [] distrib;
    distrib = m_Classifier.distributionForInstance(testCopy);
    if (m_trainingInstances.classAttribute().isNominal()) {
      pred = distrib[(int)testCopy.classValue()];
    } else {
      pred = distrib[0];
    }

    if (m_trainingInstances.classAttribute().isNominal()) {
      error = 1.0 - pred;
    } else {
      error = testCopy.classValue() - pred;
    }

    // return the negative of the error as search methods need to
    // maximize something
    return -error;
  }

  /**
   * Returns a string describing classifierSubsetEval
   *
   * @return the description as a string
   */
  public String toString() {
    StringBuffer text = new StringBuffer();
    
    if (m_trainingInstances == null) {
      text.append("\tClassifier subset evaluator has not been built yet\n");
    }
    else {
      text.append("\tClassifier Subset Evaluator\n");
      text.append("\tLearning scheme: " 
		  + getClassifier().getClass().getName() + "\n");
      text.append("\tScheme options: ");
      String[] classifierOptions = new String[0];

      if (m_Classifier instanceof OptionHandler) {
        classifierOptions = ((OptionHandler)m_Classifier).getOptions();

        for (int i = 0; i < classifierOptions.length; i++) {
          text.append(classifierOptions[i] + " ");
        }
      }

      text.append("\n");
      text.append("\tHold out/test set: ");
      if (!m_useTraining) {
	if (getHoldOutFile().getPath().startsWith("Click to set")) {
	  text.append("none\n");
	} else {
	  text.append(getHoldOutFile().getPath()+'\n');
	}
      } else {
	text.append("Training data\n");
      }
      if (m_trainingInstances.attribute(m_classIndex).isNumeric()) {
	text.append("\tAccuracy estimation: MAE\n");
      } else {
	text.append("\tAccuracy estimation: classification error\n");
      }
    }
    return text.toString();
  }
  
  /**
   * reset to defaults
   */
  protected void resetOptions () {
    m_trainingInstances = null;
    m_Evaluation = null;
    m_Classifier = new ZeroR();
    m_holdOutFile = new File("Click to set hold out or test instances");
    m_holdOutInstances = null;
    m_useTraining = false;
  }
  
  /**
   * Main method for testing this class.
   *
   * @param args the options
   */
  public static void main (String[] args) {
    try {
      System.out.println(AttributeSelection.
			 SelectAttributes(new ClassifierSubsetEval(), args));
    }
    catch (Exception e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }
  }
}
