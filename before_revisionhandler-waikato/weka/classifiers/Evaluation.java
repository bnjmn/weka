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
 *    Evaluation.java
 *    Copyright (C) 1999 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers;

import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.classifiers.xml.XMLClassifier;
import weka.core.Drawable;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Range;
import weka.core.Summarizable;
import weka.core.Utils;
import weka.core.Version;
import weka.core.converters.ConverterUtils.DataSink;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.xml.KOML;
import weka.core.xml.XMLOptions;
import weka.core.xml.XMLSerialization;
import weka.estimators.Estimator;
import weka.estimators.KernelEstimator;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Date;
import java.util.Enumeration;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Class for evaluating machine learning models. <p/>
 *
 * ------------------------------------------------------------------- <p/>
 *
 * General options when evaluating a learning scheme from the command-line: <p/>
 *
 * -t filename <br/>
 * Name of the file with the training data. (required) <p/>
 *
 * -T filename <br/>
 * Name of the file with the test data. If missing a cross-validation 
 * is performed. <p/>
 *
 * -c index <br/>
 * Index of the class attribute (1, 2, ...; default: last). <p/>
 *
 * -x number <br/>
 * The number of folds for the cross-validation (default: 10). <p/>
 *
 * -no-cv <br/>
 * No cross validation.  If no test file is provided, no evaluation
 * is done. <p/>
 * 
 * -split-percentage percentage <br/>
 * Sets the percentage for the train/test set split, e.g., 66. <p/>
 * 
 * -preserve-order <br/>
 * Preserves the order in the percentage split instead of randomizing
 * the data first with the seed value ('-s'). <p/>
 *
 * -s seed <br/>
 * Random number seed for the cross-validation and percentage split
 * (default: 1). <p/>
 *
 * -m filename <br/>
 * The name of a file containing a cost matrix. <p/>
 *
 * -l filename <br/>
 * Loads classifier from the given file. In case the filename ends with ".xml" 
 * the options are loaded from XML. <p/>
 *
 * -d filename <br/>
 * Saves classifier built from the training data into the given file. In case 
 * the filename ends with ".xml" the options are saved XML, not the model. <p/>
 *
 * -v <br/>
 * Outputs no statistics for the training data. <p/>
 *
 * -o <br/>
 * Outputs statistics only, not the classifier. <p/>
 * 
 * -i <br/>
 * Outputs information-retrieval statistics per class. <p/>
 *
 * -k <br/>
 * Outputs information-theoretic statistics. <p/>
 *
 * -p range <br/>
 * Outputs predictions for test instances (or the train instances if no test
 * instances provided), along with the attributes in the specified range 
 * (and nothing else). Use '-p 0' if no attributes are desired. <p/>
 * 
 * -distribution <br/>
 * Outputs the distribution instead of only the prediction
 * in conjunction with the '-p' option (only nominal classes). <p/>
 *
 * -r <br/>
 * Outputs cumulative margin distribution (and nothing else). <p/>
 *
 * -g <br/> 
 * Only for classifiers that implement "Graphable." Outputs
 * the graph representation of the classifier (and nothing
 * else). <p/>
 * 
 * -xml filename | xml-string <br/>
 * Retrieves the options from the XML-data instead of the command line. <p/>
 * 
 * -threshold-file file <br/>
 * The file to save the threshold data to.
 * The format is determined by the extensions, e.g., '.arff' for ARFF
 * format or '.csv' for CSV. <p/>
 *         
 * -threshold-label label <br/>
 * The class label to determine the threshold data for
 * (default is the first label) <p/>
 *         
 * ------------------------------------------------------------------- <p/>
 *
 * Example usage as the main of a classifier (called FunkyClassifier):
 * <code> <pre>
 * public static void main(String [] args) {
 *   runClassifier(new FunkyClassifier(), args);
 * }
 * </pre> </code> 
 * <p/>
 *
 * ------------------------------------------------------------------ <p/>
 *
 * Example usage from within an application:
 * <code> <pre>
 * Instances trainInstances = ... instances got from somewhere
 * Instances testInstances = ... instances got from somewhere
 * Classifier scheme = ... scheme got from somewhere
 *
 * Evaluation evaluation = new Evaluation(trainInstances);
 * evaluation.evaluateModel(scheme, testInstances);
 * System.out.println(evaluation.toSummaryString());
 * </pre> </code> 
 *
 *
 * @author   Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author   Len Trigg (trigg@cs.waikato.ac.nz)
 * @version  $Revision: 1.81.2.2 $
 */
public class Evaluation
implements Summarizable {

  /** The number of classes. */
  protected int m_NumClasses;

  /** The number of folds for a cross-validation. */
  protected int m_NumFolds;

  /** The weight of all incorrectly classified instances. */
  protected double m_Incorrect;

  /** The weight of all correctly classified instances. */
  protected double m_Correct;

  /** The weight of all unclassified instances. */
  protected double m_Unclassified;

  /*** The weight of all instances that had no class assigned to them. */
  protected double m_MissingClass;

  /** The weight of all instances that had a class assigned to them. */
  protected double m_WithClass;

  /** Array for storing the confusion matrix. */
  protected double [][] m_ConfusionMatrix;

  /** The names of the classes. */
  protected String [] m_ClassNames;

  /** Is the class nominal or numeric? */
  protected boolean m_ClassIsNominal;

  /** The prior probabilities of the classes */
  protected double [] m_ClassPriors;

  /** The sum of counts for priors */
  protected double m_ClassPriorsSum;

  /** The cost matrix (if given). */
  protected CostMatrix m_CostMatrix;

  /** The total cost of predictions (includes instance weights) */
  protected double m_TotalCost;

  /** Sum of errors. */
  protected double m_SumErr;

  /** Sum of absolute errors. */
  protected double m_SumAbsErr;

  /** Sum of squared errors. */
  protected double m_SumSqrErr;

  /** Sum of class values. */
  protected double m_SumClass;

  /** Sum of squared class values. */
  protected double m_SumSqrClass;

  /*** Sum of predicted values. */
  protected double m_SumPredicted;

  /** Sum of squared predicted values. */
  protected double m_SumSqrPredicted;

  /** Sum of predicted * class values. */
  protected double m_SumClassPredicted;

  /** Sum of absolute errors of the prior */
  protected double m_SumPriorAbsErr;

  /** Sum of absolute errors of the prior */
  protected double m_SumPriorSqrErr;

  /** Total Kononenko & Bratko Information */
  protected double m_SumKBInfo;

  /*** Resolution of the margin histogram */
  protected static int k_MarginResolution = 500;

  /** Cumulative margin distribution */
  protected double m_MarginCounts [];

  /** Number of non-missing class training instances seen */
  protected int m_NumTrainClassVals;

  /** Array containing all numeric training class values seen */
  protected double [] m_TrainClassVals;

  /** Array containing all numeric training class weights */
  protected double [] m_TrainClassWeights;

  /** Numeric class error estimator for prior */
  protected Estimator m_PriorErrorEstimator;

  /** Numeric class error estimator for scheme */
  protected Estimator m_ErrorEstimator;

  /**
   * The minimum probablility accepted from an estimator to avoid
   * taking log(0) in Sf calculations.
   */
  protected static final double MIN_SF_PROB = Double.MIN_VALUE;

  /** Total entropy of prior predictions */
  protected double m_SumPriorEntropy;

  /** Total entropy of scheme predictions */
  protected double m_SumSchemeEntropy;

  /** The list of predictions that have been generated (for computing AUC) */
  private FastVector m_Predictions;

  /** enables/disables the use of priors, e.g., if no training set is
   * present in case of de-serialized schemes */
  protected boolean m_NoPriors = false;

  /**
   * Initializes all the counters for the evaluation. 
   * Use <code>useNoPriors()</code> if the dataset is the test set and you
   * can't initialize with the priors from the training set via 
   * <code>setPriors(Instances)</code>.
   *
   * @param data 	set of training instances, to get some header 
   * 			information and prior class distribution information
   * @throws Exception 	if the class is not defined
   * @see 		#useNoPriors()
   * @see 		#setPriors(Instances)
   */
  public Evaluation(Instances data) throws Exception {

    this(data, null);
  }

  /**
   * Initializes all the counters for the evaluation and also takes a
   * cost matrix as parameter.
   * Use <code>useNoPriors()</code> if the dataset is the test set and you
   * can't initialize with the priors from the training set via 
   * <code>setPriors(Instances)</code>.
   *
   * @param data 	set of training instances, to get some header 
   * 			information and prior class distribution information
   * @param costMatrix 	the cost matrix---if null, default costs will be used
   * @throws Exception 	if cost matrix is not compatible with 
   * 			data, the class is not defined or the class is numeric
   * @see 		#useNoPriors()
   * @see 		#setPriors(Instances)
   */
  public Evaluation(Instances data, CostMatrix costMatrix) 
  throws Exception {

    m_NumClasses = data.numClasses();
    m_NumFolds = 1;
    m_ClassIsNominal = data.classAttribute().isNominal();

    if (m_ClassIsNominal) {
      m_ConfusionMatrix = new double [m_NumClasses][m_NumClasses];
      m_ClassNames = new String [m_NumClasses];
      for(int i = 0; i < m_NumClasses; i++) {
	m_ClassNames[i] = data.classAttribute().value(i);
      }
    }
    m_CostMatrix = costMatrix;
    if (m_CostMatrix != null) {
      if (!m_ClassIsNominal) {
	throw new Exception("Class has to be nominal if cost matrix " + 
	"given!");
      }
      if (m_CostMatrix.size() != m_NumClasses) {
	throw new Exception("Cost matrix not compatible with data!");
      }
    }
    m_ClassPriors = new double [m_NumClasses];
    setPriors(data);
    m_MarginCounts = new double [k_MarginResolution + 1];
  }

  /**
   * Returns the area under ROC for those predictions that have been collected
   * in the evaluateClassifier(Classifier, Instances) method. Returns 
   * Instance.missingValue() if the area is not available.
   *
   * @param classIndex the index of the class to consider as "positive"
   * @return the area under the ROC curve or not a number
   */
  public double areaUnderROC(int classIndex) {

    // Check if any predictions have been collected
    if (m_Predictions == null) {
      return Instance.missingValue();
    } else {
      ThresholdCurve tc = new ThresholdCurve();
      Instances result = tc.getCurve(m_Predictions, classIndex);
      return ThresholdCurve.getROCArea(result);
    }
  }

  /**
   * Returns a copy of the confusion matrix.
   *
   * @return a copy of the confusion matrix as a two-dimensional array
   */
  public double[][] confusionMatrix() {

    double[][] newMatrix = new double[m_ConfusionMatrix.length][0];

    for (int i = 0; i < m_ConfusionMatrix.length; i++) {
      newMatrix[i] = new double[m_ConfusionMatrix[i].length];
      System.arraycopy(m_ConfusionMatrix[i], 0, newMatrix[i], 0,
	  m_ConfusionMatrix[i].length);
    }
    return newMatrix;
  }

  /**
   * Performs a (stratified if class is nominal) cross-validation 
   * for a classifier on a set of instances. Now performs
   * a deep copy of the classifier before each call to 
   * buildClassifier() (just in case the classifier is not
   * initialized properly).
   *
   * @param classifier the classifier with any options set.
   * @param data the data on which the cross-validation is to be 
   * performed 
   * @param numFolds the number of folds for the cross-validation
   * @param random random number generator for randomization 
   * @throws Exception if a classifier could not be generated 
   * successfully or the class is not defined
   */
  public void crossValidateModel(Classifier classifier,
      Instances data, int numFolds, Random random) 
  throws Exception {

    // Make a copy of the data we can reorder
    data = new Instances(data);
    data.randomize(random);
    if (data.classAttribute().isNominal()) {
      data.stratify(numFolds);
    }
    // Do the folds
    for (int i = 0; i < numFolds; i++) {
      Instances train = data.trainCV(numFolds, i, random);
      setPriors(train);
      Classifier copiedClassifier = Classifier.makeCopy(classifier);
      copiedClassifier.buildClassifier(train);
      Instances test = data.testCV(numFolds, i);
      evaluateModel(copiedClassifier, test);
    }
    m_NumFolds = numFolds;
  }

  /**
   * Performs a (stratified if class is nominal) cross-validation 
   * for a classifier on a set of instances.
   *
   * @param classifierString a string naming the class of the classifier
   * @param data the data on which the cross-validation is to be 
   * performed 
   * @param numFolds the number of folds for the cross-validation
   * @param options the options to the classifier. Any options
   * @param random the random number generator for randomizing the data
   * accepted by the classifier will be removed from this array.
   * @throws Exception if a classifier could not be generated 
   * successfully or the class is not defined
   */
  public void crossValidateModel(String classifierString,
      Instances data, int numFolds,
      String[] options, Random random) 
  throws Exception {

    crossValidateModel(Classifier.forName(classifierString, options),
	data, numFolds, random);
  }

  /**
   * Evaluates a classifier with the options given in an array of
   * strings. <p/>
   *
   * Valid options are: <p/>
   *
   * -t filename <br/>
   * Name of the file with the training data. (required) <p/>
   *
   * -T filename <br/>
   * Name of the file with the test data. If missing a cross-validation 
   * is performed. <p/>
   *
   * -c index <br/>
   * Index of the class attribute (1, 2, ...; default: last). <p/>
   *
   * -x number <br/>
   * The number of folds for the cross-validation (default: 10). <p/>
   *
   * -no-cv <br/>
   * No cross validation.  If no test file is provided, no evaluation
   * is done. <p/>
   * 
   * -split-percentage percentage <br/>
   * Sets the percentage for the train/test set split, e.g., 66. <p/>
   * 
   * -preserve-order <br/>
   * Preserves the order in the percentage split instead of randomizing
   * the data first with the seed value ('-s'). <p/>
   *
   * -s seed <br/>
   * Random number seed for the cross-validation and percentage split
   * (default: 1). <p/>
   *
   * -m filename <br/>
   * The name of a file containing a cost matrix. <p/>
   *
   * -l filename <br/>
   * Loads classifier from the given file. In case the filename ends with
   * ".xml" the options are loaded from XML. <p/>
   *
   * -d filename <br/>
   * Saves classifier built from the training data into the given file. In case 
   * the filename ends with ".xml" the options are saved XML, not the model. <p/>
   *
   * -v <br/>
   * Outputs no statistics for the training data. <p/>
   *
   * -o <br/>
   * Outputs statistics only, not the classifier. <p/>
   * 
   * -i <br/>
   * Outputs detailed information-retrieval statistics per class. <p/>
   *
   * -k <br/>
   * Outputs information-theoretic statistics. <p/>
   *
   * -p range <br/>
   * Outputs predictions for test instances (or the train instances if no test
   * instances provided), along with the attributes in the specified range (and 
   *  nothing else). Use '-p 0' if no attributes are desired. <p/>
   *
   * -distribution <br/>
   * Outputs the distribution instead of only the prediction
   * in conjunction with the '-p' option (only nominal classes). <p/>
   *
   * -r <br/>
   * Outputs cumulative margin distribution (and nothing else). <p/>
   *
   * -g <br/> 
   * Only for classifiers that implement "Graphable." Outputs
   * the graph representation of the classifier (and nothing
   * else). <p/>
   *
   * -xml filename | xml-string <br/>
   * Retrieves the options from the XML-data instead of the command line. <p/>
   * 
   * -threshold-file file <br/>
   * The file to save the threshold data to.
   * The format is determined by the extensions, e.g., '.arff' for ARFF
   * format or '.csv' for CSV. <p/>
   *         
   * -threshold-label label <br/>
   * The class label to determine the threshold data for
   * (default is the first label) <p/>
   *
   * @param classifierString class of machine learning classifier as a string
   * @param options the array of string containing the options
   * @throws Exception if model could not be evaluated successfully
   * @return a string describing the results 
   */
  public static String evaluateModel(String classifierString, 
      String [] options) throws Exception {

    Classifier classifier;	 

    // Create classifier
    try {
      classifier = 
	(Classifier)Class.forName(classifierString).newInstance();
    } catch (Exception e) {
      throw new Exception("Can't find class with name " 
	  + classifierString + '.');
    }
    return evaluateModel(classifier, options);
  }

  /**
   * A test method for this class. Just extracts the first command line
   * argument as a classifier class name and calls evaluateModel.
   * @param args an array of command line arguments, the first of which
   * must be the class name of a classifier.
   */
  public static void main(String [] args) {

    try {
      if (args.length == 0) {
	throw new Exception("The first argument must be the class name"
	    + " of a classifier");
      }
      String classifier = args[0];
      args[0] = "";
      System.out.println(evaluateModel(classifier, args));
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }

  /**
   * Evaluates a classifier with the options given in an array of
   * strings. <p/>
   *
   * Valid options are: <p/>
   *
   * -t name of training file <br/>
   * Name of the file with the training data. (required) <p/>
   *
   * -T name of test file <br/>
   * Name of the file with the test data. If missing a cross-validation 
   * is performed. <p/>
   *
   * -c class index <br/>
   * Index of the class attribute (1, 2, ...; default: last). <p/>
   *
   * -x number of folds <br/>
   * The number of folds for the cross-validation (default: 10). <p/>
   *
   * -no-cv <br/>
   * No cross validation.  If no test file is provided, no evaluation
   * is done. <p/>
   * 
   * -split-percentage percentage <br/>
   * Sets the percentage for the train/test set split, e.g., 66. <p/>
   * 
   * -preserve-order <br/>
   * Preserves the order in the percentage split instead of randomizing
   * the data first with the seed value ('-s'). <p/>
   *
   * -s seed <br/>
   * Random number seed for the cross-validation and percentage split
   * (default: 1). <p/>
   *
   * -m file with cost matrix <br/>
   * The name of a file containing a cost matrix. <p/>
   *
   * -l filename <br/>
   * Loads classifier from the given file. In case the filename ends with
   * ".xml" the options are loaded from XML. <p/>
   *
   * -d filename <br/>
   * Saves classifier built from the training data into the given file. In case 
   * the filename ends with ".xml" the options are saved XML, not the model. <p/>
   *
   * -v <br/>
   * Outputs no statistics for the training data. <p/>
   *
   * -o <br/>
   * Outputs statistics only, not the classifier. <p/>
   * 
   * -i <br/>
   * Outputs detailed information-retrieval statistics per class. <p/>
   *
   * -k <br/>
   * Outputs information-theoretic statistics. <p/>
   *
   * -p range <br/>
   * Outputs predictions for test instances (or the train instances if no test
   * instances provided), along with the attributes in the specified range 
   * (and nothing else). Use '-p 0' if no attributes are desired. <p/>
   *
   * -distribution <br/>
   * Outputs the distribution instead of only the prediction
   * in conjunction with the '-p' option (only nominal classes). <p/>
   *
   * -r <br/>
   * Outputs cumulative margin distribution (and nothing else). <p/>
   *
   * -g <br/> 
   * Only for classifiers that implement "Graphable." Outputs
   * the graph representation of the classifier (and nothing
   * else). <p/>
   *
   * -xml filename | xml-string <br/>
   * Retrieves the options from the XML-data instead of the command line. <p/>
   *
   * @param classifier machine learning classifier
   * @param options the array of string containing the options
   * @throws Exception if model could not be evaluated successfully
   * @return a string describing the results 
   */
  public static String evaluateModel(Classifier classifier,
      String [] options) throws Exception {

    Instances train = null, tempTrain, test = null, template = null;
    int seed = 1, folds = 10, classIndex = -1;
    boolean noCrossValidation = false;
    String trainFileName, testFileName, sourceClass, 
    classIndexString, seedString, foldsString, objectInputFileName, 
    objectOutputFileName, attributeRangeString;
    boolean noOutput = false,
    printClassifications = false, trainStatistics = true,
    printMargins = false, printComplexityStatistics = false,
    printGraph = false, classStatistics = false, printSource = false;
    StringBuffer text = new StringBuffer();
    DataSource trainSource = null, testSource = null;
    ObjectInputStream objectInputStream = null;
    BufferedInputStream xmlInputStream = null;
    CostMatrix costMatrix = null;
    StringBuffer schemeOptionsText = null;
    Range attributesToOutput = null;
    long trainTimeStart = 0, trainTimeElapsed = 0,
    testTimeStart = 0, testTimeElapsed = 0;
    String xml = "";
    String[] optionsTmp = null;
    Classifier classifierBackup;
    Classifier classifierClassifications = null;
    boolean printDistribution = false;
    int actualClassIndex = -1;  // 0-based class index
    String splitPercentageString = "";
    int splitPercentage = -1;
    boolean preserveOrder = false;
    boolean trainSetPresent = false;
    boolean testSetPresent = false;
    String thresholdFile;
    String thresholdLabel;

    // help requested?
    if (Utils.getFlag("h", options) || Utils.getFlag("help", options)) {
      throw new Exception("\nHelp requested." + makeOptionString(classifier));
    }
    
    try {
      // do we get the input from XML instead of normal parameters?
      xml = Utils.getOption("xml", options);
      if (!xml.equals(""))
	options = new XMLOptions(xml).toArray();

      // is the input model only the XML-Options, i.e. w/o built model?
      optionsTmp = new String[options.length];
      for (int i = 0; i < options.length; i++)
	optionsTmp[i] = options[i];

      if (Utils.getOption('l', optionsTmp).toLowerCase().endsWith(".xml")) {
	// load options from serialized data ('-l' is automatically erased!)
	XMLClassifier xmlserial = new XMLClassifier();
	Classifier cl = (Classifier) xmlserial.read(Utils.getOption('l', options));
	// merge options
	optionsTmp = new String[options.length + cl.getOptions().length];
	System.arraycopy(cl.getOptions(), 0, optionsTmp, 0, cl.getOptions().length);
	System.arraycopy(options, 0, optionsTmp, cl.getOptions().length, options.length);
	options = optionsTmp;
      }

      noCrossValidation = Utils.getFlag("no-cv", options);
      // Get basic options (options the same for all schemes)
      classIndexString = Utils.getOption('c', options);
      if (classIndexString.length() != 0) {
	if (classIndexString.equals("first"))
	  classIndex = 1;
	else if (classIndexString.equals("last"))
	  classIndex = -1;
	else
	  classIndex = Integer.parseInt(classIndexString);
      }
      trainFileName = Utils.getOption('t', options); 
      objectInputFileName = Utils.getOption('l', options);
      objectOutputFileName = Utils.getOption('d', options);
      testFileName = Utils.getOption('T', options);
      foldsString = Utils.getOption('x', options);
      if (foldsString.length() != 0) {
	folds = Integer.parseInt(foldsString);
      }
      seedString = Utils.getOption('s', options);
      if (seedString.length() != 0) {
	seed = Integer.parseInt(seedString);
      }
      if (trainFileName.length() == 0) {
	if (objectInputFileName.length() == 0) {
	  throw new Exception("No training file and no object "+
	  "input file given.");
	} 
	if (testFileName.length() == 0) {
	  throw new Exception("No training file and no test "+
	  "file given.");
	}
      } else if ((objectInputFileName.length() != 0) &&
	  ((!(classifier instanceof UpdateableClassifier)) ||
	      (testFileName.length() == 0))) {
	throw new Exception("Classifier not incremental, or no " +
	    "test file provided: can't "+
	"use both train and model file.");
      }
      try {
	if (trainFileName.length() != 0) {
	  trainSetPresent = true;
	  trainSource = new DataSource(trainFileName);
	}
	if (testFileName.length() != 0) {
	  testSetPresent = true;
	  testSource = new DataSource(testFileName);
	}
	if (objectInputFileName.length() != 0) {
	  InputStream is = new FileInputStream(objectInputFileName);
	  if (objectInputFileName.endsWith(".gz")) {
	    is = new GZIPInputStream(is);
	  }
	  // load from KOML?
	  if (!(objectInputFileName.endsWith(".koml") && KOML.isPresent()) ) {
	    objectInputStream = new ObjectInputStream(is);
	    xmlInputStream    = null;
	  }
	  else {
	    objectInputStream = null;
	    xmlInputStream    = new BufferedInputStream(is);
	  }
	}
      } catch (Exception e) {
	throw new Exception("Can't open file " + e.getMessage() + '.');
      }
      if (testSetPresent) {
	template = test = testSource.getStructure();
	if (classIndex != -1) {
	  test.setClassIndex(classIndex - 1);
	} else {
	  if ( (test.classIndex() == -1) || (classIndexString.length() != 0) )
	    test.setClassIndex(test.numAttributes() - 1);
	}
	actualClassIndex = test.classIndex();
      }
      else {
	// percentage split
	splitPercentageString = Utils.getOption("split-percentage", options);
	if (splitPercentageString.length() != 0) {
	  if (foldsString.length() != 0)
	    throw new Exception(
		"Percentage split cannot be used in conjunction with "
		+ "cross-validation ('-x').");
	  splitPercentage = Integer.parseInt(splitPercentageString);
	  if ((splitPercentage <= 0) || (splitPercentage >= 100))
	    throw new Exception("Percentage split value needs be >0 and <100.");
	}
	else {
	  splitPercentage = -1;
	}
	preserveOrder = Utils.getFlag("preserve-order", options);
	if (preserveOrder) {
	  if (splitPercentage == -1)
	    throw new Exception("Percentage split ('-percentage-split') is missing.");
	}
	// create new train/test sources
	if (splitPercentage > 0) {
	  testSetPresent = true;
	  Instances tmpInst = trainSource.getDataSet(actualClassIndex);
	  if (!preserveOrder)
	    tmpInst.randomize(new Random(seed));
	  int trainSize = tmpInst.numInstances() * splitPercentage / 100;
	  int testSize  = tmpInst.numInstances() - trainSize;
	  Instances trainInst = new Instances(tmpInst, 0, trainSize);
	  Instances testInst  = new Instances(tmpInst, trainSize, testSize);
	  trainSource = new DataSource(trainInst);
	  testSource  = new DataSource(testInst);
	  template = test = testSource.getStructure();
	  if (classIndex != -1) {
	    test.setClassIndex(classIndex - 1);
	  } else {
	    if ( (test.classIndex() == -1) || (classIndexString.length() != 0) )
	      test.setClassIndex(test.numAttributes() - 1);
	  }
	  actualClassIndex = test.classIndex();
	}
      }
      if (trainSetPresent) {
	template = train = trainSource.getStructure();
	if (classIndex != -1) {
	  train.setClassIndex(classIndex - 1);
	} else {
	  if ( (train.classIndex() == -1) || (classIndexString.length() != 0) )
	    train.setClassIndex(train.numAttributes() - 1);
	}
	actualClassIndex = train.classIndex();
	if ((testSetPresent) && !test.equalHeaders(train)) {
	  throw new IllegalArgumentException("Train and test file not compatible!");
	}
      }
      if (template == null) {
	throw new Exception("No actual dataset provided to use as template");
      }
      costMatrix = handleCostOption(
	  Utils.getOption('m', options), template.numClasses());

      classStatistics = Utils.getFlag('i', options);
      noOutput = Utils.getFlag('o', options);
      trainStatistics = !Utils.getFlag('v', options);
      printComplexityStatistics = Utils.getFlag('k', options);
      printMargins = Utils.getFlag('r', options);
      printGraph = Utils.getFlag('g', options);
      sourceClass = Utils.getOption('z', options);
      printSource = (sourceClass.length() != 0);
      printDistribution = Utils.getFlag("distribution", options);
      thresholdFile = Utils.getOption("threshold-file", options);
      thresholdLabel = Utils.getOption("threshold-label", options);

      // Check -p option
      try {
	attributeRangeString = Utils.getOption('p', options);
      }
      catch (Exception e) {
	throw new Exception(e.getMessage() + "\nNOTE: the -p option has changed. " +
	    "It now expects a parameter specifying a range of attributes " +
	"to list with the predictions. Use '-p 0' for none.");
      }
      if (attributeRangeString.length() != 0) {
	printClassifications = true;
	if (!attributeRangeString.equals("0")) 
	  attributesToOutput = new Range(attributeRangeString);
      }

      if (!printClassifications && printDistribution)
	throw new Exception("Cannot print distribution without '-p' option!");

      // if no training file given, we don't have any priors
      if ( (!trainSetPresent) && (printComplexityStatistics) )
	throw new Exception("Cannot print complexity statistics ('-k') without training file ('-t')!");

      // If a model file is given, we can't process 
      // scheme-specific options
      if (objectInputFileName.length() != 0) {
	Utils.checkForRemainingOptions(options);
      } else {

	// Set options for classifier
	if (classifier instanceof OptionHandler) {
	  for (int i = 0; i < options.length; i++) {
	    if (options[i].length() != 0) {
	      if (schemeOptionsText == null) {
		schemeOptionsText = new StringBuffer();
	      }
	      if (options[i].indexOf(' ') != -1) {
		schemeOptionsText.append('"' + options[i] + "\" ");
	      } else {
		schemeOptionsText.append(options[i] + " ");
	      }
	    }
	  }
	  ((OptionHandler)classifier).setOptions(options);
	}
      }
      Utils.checkForRemainingOptions(options);
    } catch (Exception e) {
      throw new Exception("\nWeka exception: " + e.getMessage()
	  + makeOptionString(classifier));
    }

    // Setup up evaluation objects
    Evaluation trainingEvaluation = new Evaluation(new Instances(template, 0), costMatrix);
    Evaluation testingEvaluation = new Evaluation(new Instances(template, 0), costMatrix);

    // disable use of priors if no training file given
    if (!trainSetPresent)
      testingEvaluation.useNoPriors();

    if (objectInputFileName.length() != 0) {
      // Load classifier from file
      if (objectInputStream != null) {
	classifier = (Classifier) objectInputStream.readObject();
        // try and read a header (if present)
        Instances savedStructure = null;
        try {
          savedStructure = (Instances) objectInputStream.readObject();
        } catch (Exception ex) {
          // don't make a fuss
        }
        if (savedStructure != null) {
          // test for compatibility with template
          if (!template.equalHeaders(savedStructure)) {
            throw new Exception("training and test set are not compatible");
          }
        }
	objectInputStream.close();
      }
      else {
	// whether KOML is available has already been checked (objectInputStream would null otherwise)!
	classifier = (Classifier) KOML.read(xmlInputStream);
	xmlInputStream.close();
      }
    }

    // backup of fully setup classifier for cross-validation
    classifierBackup = Classifier.makeCopy(classifier);

    // Build the classifier if no object file provided
    if ((classifier instanceof UpdateableClassifier) &&
	(testSetPresent || noCrossValidation) &&
	(costMatrix == null) &&
	(trainSetPresent)) {
      // Build classifier incrementally
      trainingEvaluation.setPriors(train);
      testingEvaluation.setPriors(train);
      trainTimeStart = System.currentTimeMillis();
      if (objectInputFileName.length() == 0) {
	classifier.buildClassifier(train);
      }
      Instance trainInst;
      while (trainSource.hasMoreElements(train)) {
	trainInst = trainSource.nextElement(train);
	trainingEvaluation.updatePriors(trainInst);
	testingEvaluation.updatePriors(trainInst);
	((UpdateableClassifier)classifier).updateClassifier(trainInst);
      }
      trainTimeElapsed = System.currentTimeMillis() - trainTimeStart;
    } else if (objectInputFileName.length() == 0) {
      // Build classifier in one go
      tempTrain = trainSource.getDataSet(actualClassIndex);
      trainingEvaluation.setPriors(tempTrain);
      testingEvaluation.setPriors(tempTrain);
      trainTimeStart = System.currentTimeMillis();
      classifier.buildClassifier(tempTrain);
      trainTimeElapsed = System.currentTimeMillis() - trainTimeStart;
    } 

    // backup of fully trained classifier for printing the classifications
    if (printClassifications)
      classifierClassifications = Classifier.makeCopy(classifier);

    // Save the classifier if an object output file is provided
    if (objectOutputFileName.length() != 0) {
      OutputStream os = new FileOutputStream(objectOutputFileName);
      // binary
      if (!(objectOutputFileName.endsWith(".xml") || (objectOutputFileName.endsWith(".koml") && KOML.isPresent()))) {
	if (objectOutputFileName.endsWith(".gz")) {
	  os = new GZIPOutputStream(os);
	}
	ObjectOutputStream objectOutputStream = new ObjectOutputStream(os);
	objectOutputStream.writeObject(classifier);
        if (template != null) {
          objectOutputStream.writeObject(template);
        }
	objectOutputStream.flush();
	objectOutputStream.close();
      }
      // KOML/XML
      else {
	BufferedOutputStream xmlOutputStream = new BufferedOutputStream(os);
	if (objectOutputFileName.endsWith(".xml")) {
	  XMLSerialization xmlSerial = new XMLClassifier();
	  xmlSerial.write(xmlOutputStream, classifier);
	}
	else
	  // whether KOML is present has already been checked
	  // if not present -> ".koml" is interpreted as binary - see above
	  if (objectOutputFileName.endsWith(".koml")) {
	    KOML.write(xmlOutputStream, classifier);
	  }
	xmlOutputStream.close();
      }
    }

    // If classifier is drawable output string describing graph
    if ((classifier instanceof Drawable) && (printGraph)){
      return ((Drawable)classifier).graph();
    }

    // Output the classifier as equivalent source
    if ((classifier instanceof Sourcable) && (printSource)){
      return wekaStaticWrapper((Sourcable) classifier, sourceClass);
    }

    // Output model
    if (!(noOutput || printMargins)) {
      if (classifier instanceof OptionHandler) {
	if (schemeOptionsText != null) {
	  text.append("\nOptions: "+schemeOptionsText);
	  text.append("\n");
	}
      }
      text.append("\n" + classifier.toString() + "\n");
    }

    if (!printMargins && (costMatrix != null)) {
      text.append("\n=== Evaluation Cost Matrix ===\n\n");
      text.append(costMatrix.toString());
    }

    // Output test instance predictions only
    if (printClassifications) {
      DataSource source = testSource;
      // no test set -> use train set
      if (source == null)
	source = trainSource;
      return printClassifications(classifierClassifications, new Instances(template, 0),
	  source, actualClassIndex + 1, attributesToOutput,
	  printDistribution);
    }

    // Compute error estimate from training data
    if ((trainStatistics) && (trainSetPresent)) {

      if ((classifier instanceof UpdateableClassifier) &&
	  (testSetPresent) &&
	  (costMatrix == null)) {

	// Classifier was trained incrementally, so we have to 
	// reset the source.
	trainSource.reset();

	// Incremental testing
	train = trainSource.getStructure(actualClassIndex);
	testTimeStart = System.currentTimeMillis();
	Instance trainInst;
	while (trainSource.hasMoreElements(train)) {
	  trainInst = trainSource.nextElement(train);
	  trainingEvaluation.evaluateModelOnce((Classifier)classifier, trainInst);
	}
	testTimeElapsed = System.currentTimeMillis() - testTimeStart;
      } else {
	testTimeStart = System.currentTimeMillis();
	trainingEvaluation.evaluateModel(
	    classifier, trainSource.getDataSet(actualClassIndex));
	testTimeElapsed = System.currentTimeMillis() - testTimeStart;
      }

      // Print the results of the training evaluation
      if (printMargins) {
	return trainingEvaluation.toCumulativeMarginDistributionString();
      } else {
	text.append("\nTime taken to build model: "
	    + Utils.doubleToString(trainTimeElapsed / 1000.0,2)
	    + " seconds");
	
	if (splitPercentage > 0)
	  text.append("\nTime taken to test model on training split: ");
	else
	  text.append("\nTime taken to test model on training data: ");
	text.append(Utils.doubleToString(testTimeElapsed / 1000.0,2) + " seconds");

	if (splitPercentage > 0)
	  text.append(trainingEvaluation.toSummaryString("\n\n=== Error on training"
	      + " split ===\n", printComplexityStatistics));
	else
	  text.append(trainingEvaluation.toSummaryString("\n\n=== Error on training"
	      + " data ===\n", printComplexityStatistics));
	
	if (template.classAttribute().isNominal()) {
	  if (classStatistics) {
	    text.append("\n\n" + trainingEvaluation.toClassDetailsString());
	  }
          if (!noCrossValidation)
            text.append("\n\n" + trainingEvaluation.toMatrixString());
	}

      }
    }

    // Compute proper error estimates
    if (testSource != null) {
      // Testing is on the supplied test data
      Instance testInst;
      while (testSource.hasMoreElements(test)) {
	testInst = testSource.nextElement(test);
	testingEvaluation.evaluateModelOnceAndRecordPrediction(
            (Classifier)classifier, testInst);
      }

      if (splitPercentage > 0)
	text.append("\n\n" + testingEvaluation.
	    toSummaryString("=== Error on test split ===\n",
		printComplexityStatistics));
      else
	text.append("\n\n" + testingEvaluation.
	    toSummaryString("=== Error on test data ===\n",
		printComplexityStatistics));

    } else if (trainSource != null) {
      if (!noCrossValidation) {
	// Testing is via cross-validation on training data
	Random random = new Random(seed);
	// use untrained (!) classifier for cross-validation
	classifier = Classifier.makeCopy(classifierBackup);
	testingEvaluation.crossValidateModel(
	    classifier, trainSource.getDataSet(actualClassIndex), folds, random);
	if (template.classAttribute().isNumeric()) {
	  text.append("\n\n\n" + testingEvaluation.
	      toSummaryString("=== Cross-validation ===\n",
		  printComplexityStatistics));
	} else {
	  text.append("\n\n\n" + testingEvaluation.
	      toSummaryString("=== Stratified " + 
		  "cross-validation ===\n",
		  printComplexityStatistics));
	}
      }
    }
    if (template.classAttribute().isNominal()) {
      if (classStatistics) {
	text.append("\n\n" + testingEvaluation.toClassDetailsString());
      }
      if (!noCrossValidation)
        text.append("\n\n" + testingEvaluation.toMatrixString());
    }

    if ((thresholdFile.length() != 0) && template.classAttribute().isNominal()) {
      int labelIndex = 0;
      if (thresholdLabel.length() != 0)
	labelIndex = template.classAttribute().indexOfValue(thresholdLabel);
      if (labelIndex == -1)
	throw new IllegalArgumentException(
	    "Class label '" + thresholdLabel + "' is unknown!");
      ThresholdCurve tc = new ThresholdCurve();
      Instances result = tc.getCurve(testingEvaluation.predictions(), labelIndex);
      DataSink.write(thresholdFile, result);
    }
    
    return text.toString();
  }

  /**
   * Attempts to load a cost matrix.
   *
   * @param costFileName the filename of the cost matrix
   * @param numClasses the number of classes that should be in the cost matrix
   * (only used if the cost file is in old format).
   * @return a <code>CostMatrix</code> value, or null if costFileName is empty
   * @throws Exception if an error occurs.
   */
  protected static CostMatrix handleCostOption(String costFileName, 
      int numClasses) 
  throws Exception {

    if ((costFileName != null) && (costFileName.length() != 0)) {
      System.out.println(
	  "NOTE: The behaviour of the -m option has changed between WEKA 3.0"
	  +" and WEKA 3.1. -m now carries out cost-sensitive *evaluation*"
	  +" only. For cost-sensitive *prediction*, use one of the"
	  +" cost-sensitive metaschemes such as"
	  +" weka.classifiers.meta.CostSensitiveClassifier or"
	  +" weka.classifiers.meta.MetaCost");

      Reader costReader = null;
      try {
	costReader = new BufferedReader(new FileReader(costFileName));
      } catch (Exception e) {
	throw new Exception("Can't open file " + e.getMessage() + '.');
      }
      try {
	// First try as a proper cost matrix format
	return new CostMatrix(costReader);
      } catch (Exception ex) {
	try {
	  // Now try as the poxy old format :-)
	  //System.err.println("Attempting to read old format cost file");
	  try {
	    costReader.close(); // Close the old one
	    costReader = new BufferedReader(new FileReader(costFileName));
	  } catch (Exception e) {
	    throw new Exception("Can't open file " + e.getMessage() + '.');
	  }
	  CostMatrix costMatrix = new CostMatrix(numClasses);
	  //System.err.println("Created default cost matrix");
	  costMatrix.readOldFormat(costReader);
	  return costMatrix;
	  //System.err.println("Read old format");
	} catch (Exception e2) {
	  // re-throw the original exception
	  //System.err.println("Re-throwing original exception");
	  throw ex;
	}
      }
    } else {
      return null;
    }
  }

  /**
   * Evaluates the classifier on a given set of instances. Note that
   * the data must have exactly the same format (e.g. order of
   * attributes) as the data used to train the classifier! Otherwise
   * the results will generally be meaningless.
   *
   * @param classifier machine learning classifier
   * @param data set of test instances for evaluation
   * @return the predictions
   * @throws Exception if model could not be evaluated 
   * successfully 
   */
  public double[] evaluateModel(Classifier classifier,
      Instances data) throws Exception {

    double predictions[] = new double[data.numInstances()];

    // Need to be able to collect predictions if appropriate (for AUC)

    for (int i = 0; i < data.numInstances(); i++) {
      predictions[i] = evaluateModelOnceAndRecordPrediction((Classifier)classifier, 
	  data.instance(i));
    }

    return predictions;
  }

  /**
   * Evaluates the classifier on a single instance and records the
   * prediction (if the class is nominal).
   *
   * @param classifier machine learning classifier
   * @param instance the test instance to be classified
   * @return the prediction made by the clasifier
   * @throws Exception if model could not be evaluated 
   * successfully or the data contains string attributes
   */
  public double evaluateModelOnceAndRecordPrediction(Classifier classifier,
      Instance instance) throws Exception {

    Instance classMissing = (Instance)instance.copy();
    double pred = 0;
    classMissing.setDataset(instance.dataset());
    classMissing.setClassMissing();
    if (m_ClassIsNominal) {
      if (m_Predictions == null) {
	m_Predictions = new FastVector();
      }
      double [] dist = classifier.distributionForInstance(classMissing);
      pred = Utils.maxIndex(dist);
      if (dist[(int)pred] <= 0) {
	pred = Instance.missingValue();
      }
      updateStatsForClassifier(dist, instance);
      m_Predictions.addElement(new NominalPrediction(instance.classValue(), dist, 
	  instance.weight()));
    } else {
      pred = classifier.classifyInstance(classMissing);
      updateStatsForPredictor(pred, instance);
    }
    return pred;
  }

  /**
   * Evaluates the classifier on a single instance.
   *
   * @param classifier machine learning classifier
   * @param instance the test instance to be classified
   * @return the prediction made by the clasifier
   * @throws Exception if model could not be evaluated 
   * successfully or the data contains string attributes
   */
  public double evaluateModelOnce(Classifier classifier,
      Instance instance) throws Exception {

    Instance classMissing = (Instance)instance.copy();
    double pred = 0;
    classMissing.setDataset(instance.dataset());
    classMissing.setClassMissing();
    if (m_ClassIsNominal) {
      double [] dist = classifier.distributionForInstance(classMissing);
      pred = Utils.maxIndex(dist);
      if (dist[(int)pred] <= 0) {
	pred = Instance.missingValue();
      }
      updateStatsForClassifier(dist, instance);
    } else {
      pred = classifier.classifyInstance(classMissing);
      updateStatsForPredictor(pred, instance);
    }
    return pred;
  }

  /**
   * Evaluates the supplied distribution on a single instance.
   *
   * @param dist the supplied distribution
   * @param instance the test instance to be classified
   * @return the prediction
   * @throws Exception if model could not be evaluated 
   * successfully
   */
  public double evaluateModelOnce(double [] dist, 
      Instance instance) throws Exception {
    double pred;
    if (m_ClassIsNominal) {
      pred = Utils.maxIndex(dist);
      if (dist[(int)pred] <= 0) {
	pred = Instance.missingValue();
      }
      updateStatsForClassifier(dist, instance);
    } else {
      pred = dist[0];
      updateStatsForPredictor(pred, instance);
    }
    return pred;
  }

  /**
   * Evaluates the supplied distribution on a single instance.
   *
   * @param dist the supplied distribution
   * @param instance the test instance to be classified
   * @return the prediction
   * @throws Exception if model could not be evaluated 
   * successfully
   */
  public double evaluateModelOnceAndRecordPrediction(double [] dist, 
      Instance instance) throws Exception {
    double pred;
    if (m_ClassIsNominal) {
      if (m_Predictions == null) {
	m_Predictions = new FastVector();
      }
      pred = Utils.maxIndex(dist);
      if (dist[(int)pred] <= 0) {
	pred = Instance.missingValue();
      }
      updateStatsForClassifier(dist, instance);
      m_Predictions.addElement(new NominalPrediction(instance.classValue(), dist, 
	  instance.weight()));
    } else {
      pred = dist[0];
      updateStatsForPredictor(pred, instance);
    }
    return pred;
  }

  /**
   * Evaluates the supplied prediction on a single instance.
   *
   * @param prediction the supplied prediction
   * @param instance the test instance to be classified
   * @throws Exception if model could not be evaluated 
   * successfully
   */
  public void evaluateModelOnce(double prediction,
      Instance instance) throws Exception {

    if (m_ClassIsNominal) {
      updateStatsForClassifier(makeDistribution(prediction), 
	  instance);
    } else {
      updateStatsForPredictor(prediction, instance);
    }
  }

  /**
   * Returns the predictions that have been collected.
   *
   * @return a reference to the FastVector containing the predictions
   * that have been collected. This should be null if no predictions
   * have been collected (e.g. if the class is numeric).
   */
  public FastVector predictions() {

    return m_Predictions;
  }

  /**
   * Wraps a static classifier in enough source to test using the weka
   * class libraries.
   *
   * @param classifier a Sourcable Classifier
   * @param className the name to give to the source code class
   * @return the source for a static classifier that can be tested with
   * weka libraries.
   * @throws Exception if code-generation fails
   */
  public static String wekaStaticWrapper(Sourcable classifier, String className)     
    throws Exception {

    StringBuffer result = new StringBuffer();
    String staticClassifier = classifier.toSource(className);
    
    result.append("// Generated with Weka " + Version.VERSION + "\n");
    result.append("//\n");
    result.append("// This code is public domain and comes with no warranty.\n");
    result.append("//\n");
    result.append("// Timestamp: " + new Date() + "\n");
    result.append("\n");
    result.append("package weka.classifiers;\n");
    result.append("\n");
    result.append("import weka.core.Attribute;\n");
    result.append("import weka.core.Capabilities;\n");
    result.append("import weka.core.Capabilities.Capability;\n");
    result.append("import weka.core.Instance;\n");
    result.append("import weka.core.Instances;\n");
    result.append("import weka.classifiers.Classifier;\n");
    result.append("\n");
    result.append("public class WekaWrapper\n");
    result.append("  extends Classifier {\n");
    
    // globalInfo
    result.append("\n");
    result.append("  /**\n");
    result.append("   * Returns only the toString() method.\n");
    result.append("   *\n");
    result.append("   * @return a string describing the classifier\n");
    result.append("   */\n");
    result.append("  public String globalInfo() {\n");
    result.append("    return toString();\n");
    result.append("  }\n");
    
    // getCapabilities
    result.append("\n");
    result.append("  /**\n");
    result.append("   * Returns the capabilities of this classifier.\n");
    result.append("   *\n");
    result.append("   * @return the capabilities\n");
    result.append("   */\n");
    result.append("  public Capabilities getCapabilities() {\n");
    result.append(((Classifier) classifier).getCapabilities().toSource("result", 4));
    result.append("    return result;\n");
    result.append("  }\n");
    
    // buildClassifier
    result.append("\n");
    result.append("  /**\n");
    result.append("   * only checks the data against its capabilities.\n");
    result.append("   *\n");
    result.append("   * @param i the training data\n");
    result.append("   */\n");
    result.append("  public void buildClassifier(Instances i) throws Exception {\n");
    result.append("    // can classifier handle the data?\n");
    result.append("    getCapabilities().testWithFail(i);\n");
    result.append("  }\n");
    
    // classifyInstance
    result.append("\n");
    result.append("  /**\n");
    result.append("   * Classifies the given instance.\n");
    result.append("   *\n");
    result.append("   * @param i the instance to classify\n");
    result.append("   * @return the classification result\n");
    result.append("   */\n");
    result.append("  public double classifyInstance(Instance i) throws Exception {\n");
    result.append("    Object[] s = new Object[i.numAttributes()];\n");
    result.append("    \n");
    result.append("    for (int j = 0; j < s.length; j++) {\n");
    result.append("      if (!i.isMissing(j)) {\n");
    result.append("        if (i.attribute(j).isNominal())\n");
    result.append("          s[j] = new String(i.stringValue(j));\n");
    result.append("        else if (i.attribute(j).isNumeric())\n");
    result.append("          s[j] = new Double(i.value(j));\n");
    result.append("      }\n");
    result.append("    }\n");
    result.append("    \n");
    result.append("    // set class value to missing\n");
    result.append("    s[i.classIndex()] = null;\n");
    result.append("    \n");
    result.append("    return " + className + ".classify(s);\n");
    result.append("  }\n");
    
    // toString
    result.append("\n");
    result.append("  /**\n");
    result.append("   * Returns only the classnames and what classifier it is based on.\n");
    result.append("   *\n");
    result.append("   * @return a short description\n");
    result.append("   */\n");
    result.append("  public String toString() {\n");
    result.append("    return \"Auto-generated classifier wrapper, based on " 
	+ classifier.getClass().getName() + " (generated with Weka " + Version.VERSION + ").\\n" 
	+ "\" + this.getClass().getName() + \"/" + className + "\";\n");
    result.append("  }\n");
    
    // main
    result.append("\n");
    result.append("  /**\n");
    result.append("   * Runs the classfier from commandline.\n");
    result.append("   *\n");
    result.append("   * @param args the commandline arguments\n");
    result.append("   */\n");
    result.append("  public static void main(String args[]) {\n");
    result.append("    runClassifier(new WekaWrapper(), args);\n");
    result.append("  }\n");
    result.append("}\n");
    
    // actual classifier code
    result.append("\n");
    result.append(staticClassifier);
    
    return result.toString();
  }

  /**
   * Gets the number of test instances that had a known class value
   * (actually the sum of the weights of test instances with known 
   * class value).
   *
   * @return the number of test instances with known class
   */
  public final double numInstances() {

    return m_WithClass;
  }

  /**
   * Gets the number of instances incorrectly classified (that is, for
   * which an incorrect prediction was made). (Actually the sum of the weights
   * of these instances)
   *
   * @return the number of incorrectly classified instances 
   */
  public final double incorrect() {

    return m_Incorrect;
  }

  /**
   * Gets the percentage of instances incorrectly classified (that is, for
   * which an incorrect prediction was made).
   *
   * @return the percent of incorrectly classified instances 
   * (between 0 and 100)
   */
  public final double pctIncorrect() {

    return 100 * m_Incorrect / m_WithClass;
  }

  /**
   * Gets the total cost, that is, the cost of each prediction times the
   * weight of the instance, summed over all instances.
   *
   * @return the total cost
   */
  public final double totalCost() {

    return m_TotalCost;
  }

  /**
   * Gets the average cost, that is, total cost of misclassifications
   * (incorrect plus unclassified) over the total number of instances.
   *
   * @return the average cost.  
   */
  public final double avgCost() {

    return m_TotalCost / m_WithClass;
  }

  /**
   * Gets the number of instances correctly classified (that is, for
   * which a correct prediction was made). (Actually the sum of the weights
   * of these instances)
   *
   * @return the number of correctly classified instances
   */
  public final double correct() {

    return m_Correct;
  }

  /**
   * Gets the percentage of instances correctly classified (that is, for
   * which a correct prediction was made).
   *
   * @return the percent of correctly classified instances (between 0 and 100)
   */
  public final double pctCorrect() {

    return 100 * m_Correct / m_WithClass;
  }

  /**
   * Gets the number of instances not classified (that is, for
   * which no prediction was made by the classifier). (Actually the sum
   * of the weights of these instances)
   *
   * @return the number of unclassified instances
   */
  public final double unclassified() {

    return m_Unclassified;
  }

  /**
   * Gets the percentage of instances not classified (that is, for
   * which no prediction was made by the classifier).
   *
   * @return the percent of unclassified instances (between 0 and 100)
   */
  public final double pctUnclassified() {

    return 100 * m_Unclassified / m_WithClass;
  }

  /**
   * Returns the estimated error rate or the root mean squared error
   * (if the class is numeric). If a cost matrix was given this
   * error rate gives the average cost.
   *
   * @return the estimated error rate (between 0 and 1, or between 0 and 
   * maximum cost)
   */
  public final double errorRate() {

    if (!m_ClassIsNominal) {
      return Math.sqrt(m_SumSqrErr / (m_WithClass - m_Unclassified));
    }
    if (m_CostMatrix == null) {
      return m_Incorrect / m_WithClass;
    } else {
      return avgCost();
    }
  }

  /**
   * Returns value of kappa statistic if class is nominal.
   *
   * @return the value of the kappa statistic
   */
  public final double kappa() {


    double[] sumRows = new double[m_ConfusionMatrix.length];
    double[] sumColumns = new double[m_ConfusionMatrix.length];
    double sumOfWeights = 0;
    for (int i = 0; i < m_ConfusionMatrix.length; i++) {
      for (int j = 0; j < m_ConfusionMatrix.length; j++) {
	sumRows[i] += m_ConfusionMatrix[i][j];
	sumColumns[j] += m_ConfusionMatrix[i][j];
	sumOfWeights += m_ConfusionMatrix[i][j];
      }
    }
    double correct = 0, chanceAgreement = 0;
    for (int i = 0; i < m_ConfusionMatrix.length; i++) {
      chanceAgreement += (sumRows[i] * sumColumns[i]);
      correct += m_ConfusionMatrix[i][i];
    }
    chanceAgreement /= (sumOfWeights * sumOfWeights);
    correct /= sumOfWeights;

    if (chanceAgreement < 1) {
      return (correct - chanceAgreement) / (1 - chanceAgreement);
    } else {
      return 1;
    }
  }

  /**
   * Returns the correlation coefficient if the class is numeric.
   *
   * @return the correlation coefficient
   * @throws Exception if class is not numeric
   */
  public final double correlationCoefficient() throws Exception {

    if (m_ClassIsNominal) {
      throw
      new Exception("Can't compute correlation coefficient: " + 
      "class is nominal!");
    }

    double correlation = 0;
    double varActual = 
      m_SumSqrClass - m_SumClass * m_SumClass / 
      (m_WithClass - m_Unclassified);
    double varPredicted = 
      m_SumSqrPredicted - m_SumPredicted * m_SumPredicted / 
      (m_WithClass - m_Unclassified);
    double varProd = 
      m_SumClassPredicted - m_SumClass * m_SumPredicted / 
      (m_WithClass - m_Unclassified);

    if (varActual * varPredicted <= 0) {
      correlation = 0.0;
    } else {
      correlation = varProd / Math.sqrt(varActual * varPredicted);
    }

    return correlation;
  }

  /**
   * Returns the mean absolute error. Refers to the error of the
   * predicted values for numeric classes, and the error of the 
   * predicted probability distribution for nominal classes.
   *
   * @return the mean absolute error 
   */
  public final double meanAbsoluteError() {

    return m_SumAbsErr / (m_WithClass - m_Unclassified);
  }

  /**
   * Returns the mean absolute error of the prior.
   *
   * @return the mean absolute error 
   */
  public final double meanPriorAbsoluteError() {

    if (m_NoPriors)
      return Double.NaN;

    return m_SumPriorAbsErr / m_WithClass;
  }

  /**
   * Returns the relative absolute error.
   *
   * @return the relative absolute error 
   * @throws Exception if it can't be computed
   */
  public final double relativeAbsoluteError() throws Exception {

    if (m_NoPriors)
      return Double.NaN;

    return 100 * meanAbsoluteError() / meanPriorAbsoluteError();
  }

  /**
   * Returns the root mean squared error.
   *
   * @return the root mean squared error 
   */
  public final double rootMeanSquaredError() {

    return Math.sqrt(m_SumSqrErr / (m_WithClass - m_Unclassified));
  }

  /**
   * Returns the root mean prior squared error.
   *
   * @return the root mean prior squared error 
   */
  public final double rootMeanPriorSquaredError() {

    if (m_NoPriors)
      return Double.NaN;

    return Math.sqrt(m_SumPriorSqrErr / m_WithClass);
  }

  /**
   * Returns the root relative squared error if the class is numeric.
   *
   * @return the root relative squared error 
   */
  public final double rootRelativeSquaredError() {

    if (m_NoPriors)
      return Double.NaN;

    return 100.0 * rootMeanSquaredError() / 
    rootMeanPriorSquaredError();
  }

  /**
   * Calculate the entropy of the prior distribution
   *
   * @return the entropy of the prior distribution
   * @throws Exception if the class is not nominal
   */
  public final double priorEntropy() throws Exception {

    if (!m_ClassIsNominal) {
      throw
      new Exception("Can't compute entropy of class prior: " + 
      "class numeric!");
    }

    if (m_NoPriors)
      return Double.NaN;

    double entropy = 0;
    for(int i = 0; i < m_NumClasses; i++) {
      entropy -= m_ClassPriors[i] / m_ClassPriorsSum 
      * Utils.log2(m_ClassPriors[i] / m_ClassPriorsSum);
    }
    return entropy;
  }

  /**
   * Return the total Kononenko & Bratko Information score in bits
   *
   * @return the K&B information score
   * @throws Exception if the class is not nominal
   */
  public final double KBInformation() throws Exception {

    if (!m_ClassIsNominal) {
      throw
      new Exception("Can't compute K&B Info score: " + 
      "class numeric!");
    }

    if (m_NoPriors)
      return Double.NaN;

    return m_SumKBInfo;
  }

  /**
   * Return the Kononenko & Bratko Information score in bits per 
   * instance.
   *
   * @return the K&B information score
   * @throws Exception if the class is not nominal
   */
  public final double KBMeanInformation() throws Exception {

    if (!m_ClassIsNominal) {
      throw
      new Exception("Can't compute K&B Info score: "
	  + "class numeric!");
    }

    if (m_NoPriors)
      return Double.NaN;

    return m_SumKBInfo / (m_WithClass - m_Unclassified);
  }

  /**
   * Return the Kononenko & Bratko Relative Information score
   *
   * @return the K&B relative information score
   * @throws Exception if the class is not nominal
   */
  public final double KBRelativeInformation() throws Exception {

    if (!m_ClassIsNominal) {
      throw
      new Exception("Can't compute K&B Info score: " + 
      "class numeric!");
    }

    if (m_NoPriors)
      return Double.NaN;

    return 100.0 * KBInformation() / priorEntropy();
  }

  /**
   * Returns the total entropy for the null model
   * 
   * @return the total null model entropy
   */
  public final double SFPriorEntropy() {

    if (m_NoPriors)
      return Double.NaN;

    return m_SumPriorEntropy;
  }

  /**
   * Returns the entropy per instance for the null model
   * 
   * @return the null model entropy per instance
   */
  public final double SFMeanPriorEntropy() {

    if (m_NoPriors)
      return Double.NaN;

    return m_SumPriorEntropy / m_WithClass;
  }

  /**
   * Returns the total entropy for the scheme
   * 
   * @return the total scheme entropy
   */
  public final double SFSchemeEntropy() {

    if (m_NoPriors)
      return Double.NaN;

    return m_SumSchemeEntropy;
  }

  /**
   * Returns the entropy per instance for the scheme
   * 
   * @return the scheme entropy per instance
   */
  public final double SFMeanSchemeEntropy() {

    if (m_NoPriors)
      return Double.NaN;

    return m_SumSchemeEntropy / (m_WithClass - m_Unclassified);
  }

  /**
   * Returns the total SF, which is the null model entropy minus
   * the scheme entropy.
   * 
   * @return the total SF
   */
  public final double SFEntropyGain() {

    if (m_NoPriors)
      return Double.NaN;

    return m_SumPriorEntropy - m_SumSchemeEntropy;
  }

  /**
   * Returns the SF per instance, which is the null model entropy
   * minus the scheme entropy, per instance.
   * 
   * @return the SF per instance
   */
  public final double SFMeanEntropyGain() {

    if (m_NoPriors)
      return Double.NaN;

    return (m_SumPriorEntropy - m_SumSchemeEntropy) / 
      (m_WithClass - m_Unclassified);
  }

  /**
   * Output the cumulative margin distribution as a string suitable
   * for input for gnuplot or similar package.
   *
   * @return the cumulative margin distribution
   * @throws Exception if the class attribute is nominal
   */
  public String toCumulativeMarginDistributionString() throws Exception {

    if (!m_ClassIsNominal) {
      throw new Exception("Class must be nominal for margin distributions");
    }
    String result = "";
    double cumulativeCount = 0;
    double margin;
    for(int i = 0; i <= k_MarginResolution; i++) {
      if (m_MarginCounts[i] != 0) {
	cumulativeCount += m_MarginCounts[i];
	margin = (double)i * 2.0 / k_MarginResolution - 1.0;
	result = result + Utils.doubleToString(margin, 7, 3) + ' ' 
	+ Utils.doubleToString(cumulativeCount * 100 
	    / m_WithClass, 7, 3) + '\n';
      } else if (i == 0) {
	result = Utils.doubleToString(-1.0, 7, 3) + ' ' 
	+ Utils.doubleToString(0, 7, 3) + '\n';
      }
    }
    return result;
  }


  /**
   * Calls toSummaryString() with no title and no complexity stats
   *
   * @return a summary description of the classifier evaluation
   */
  public String toSummaryString() {

    return toSummaryString("", false);
  }

  /**
   * Calls toSummaryString() with a default title.
   *
   * @param printComplexityStatistics if true, complexity statistics are
   * returned as well
   * @return the summary string
   */
  public String toSummaryString(boolean printComplexityStatistics) {

    return toSummaryString("=== Summary ===\n", printComplexityStatistics);
  }

  /**
   * Outputs the performance statistics in summary form. Lists 
   * number (and percentage) of instances classified correctly, 
   * incorrectly and unclassified. Outputs the total number of 
   * instances classified, and the number of instances (if any) 
   * that had no class value provided. 
   *
   * @param title the title for the statistics
   * @param printComplexityStatistics if true, complexity statistics are
   * returned as well
   * @return the summary as a String
   */
  public String toSummaryString(String title, 
      boolean printComplexityStatistics) { 

    StringBuffer text = new StringBuffer();

    if (printComplexityStatistics && m_NoPriors) {
      printComplexityStatistics = false;
      System.err.println("Priors disabled, cannot print complexity statistics!");
    }

    text.append(title + "\n");
    try {
      if (m_WithClass > 0) {
	if (m_ClassIsNominal) {

	  text.append("Correctly Classified Instances     ");
	  text.append(Utils.doubleToString(correct(), 12, 4) + "     " +
	      Utils.doubleToString(pctCorrect(),
		  12, 4) + " %\n");
	  text.append("Incorrectly Classified Instances   ");
	  text.append(Utils.doubleToString(incorrect(), 12, 4) + "     " +
	      Utils.doubleToString(pctIncorrect(),
		  12, 4) + " %\n");
	  text.append("Kappa statistic                    ");
	  text.append(Utils.doubleToString(kappa(), 12, 4) + "\n");

	  if (m_CostMatrix != null) {
	    text.append("Total Cost                         ");
	    text.append(Utils.doubleToString(totalCost(), 12, 4) + "\n");
	    text.append("Average Cost                       ");
	    text.append(Utils.doubleToString(avgCost(), 12, 4) + "\n");
	  }
	  if (printComplexityStatistics) {
	    text.append("K&B Relative Info Score            ");
	    text.append(Utils.doubleToString(KBRelativeInformation(), 12, 4) 
		+ " %\n");
	    text.append("K&B Information Score              ");
	    text.append(Utils.doubleToString(KBInformation(), 12, 4) 
		+ " bits");
	    text.append(Utils.doubleToString(KBMeanInformation(), 12, 4) 
		+ " bits/instance\n");
	  }
	} else {        
	  text.append("Correlation coefficient            ");
	  text.append(Utils.doubleToString(correlationCoefficient(), 12 , 4) +
	  "\n");
	}
	if (printComplexityStatistics) {
	  text.append("Class complexity | order 0         ");
	  text.append(Utils.doubleToString(SFPriorEntropy(), 12, 4) 
	      + " bits");
	  text.append(Utils.doubleToString(SFMeanPriorEntropy(), 12, 4) 
	      + " bits/instance\n");
	  text.append("Class complexity | scheme          ");
	  text.append(Utils.doubleToString(SFSchemeEntropy(), 12, 4) 
	      + " bits");
	  text.append(Utils.doubleToString(SFMeanSchemeEntropy(), 12, 4) 
	      + " bits/instance\n");
	  text.append("Complexity improvement     (Sf)    ");
	  text.append(Utils.doubleToString(SFEntropyGain(), 12, 4) + " bits");
	  text.append(Utils.doubleToString(SFMeanEntropyGain(), 12, 4) 
	      + " bits/instance\n");
	}

	text.append("Mean absolute error                ");
	text.append(Utils.doubleToString(meanAbsoluteError(), 12, 4) 
	    + "\n");
	text.append("Root mean squared error            ");
	text.append(Utils.
	    doubleToString(rootMeanSquaredError(), 12, 4) 
	    + "\n");
	if (!m_NoPriors) {
	  text.append("Relative absolute error            ");
	  text.append(Utils.doubleToString(relativeAbsoluteError(), 
	      12, 4) + " %\n");
	  text.append("Root relative squared error        ");
	  text.append(Utils.doubleToString(rootRelativeSquaredError(), 
	      12, 4) + " %\n");
	}
      }
      if (Utils.gr(unclassified(), 0)) {
	text.append("UnClassified Instances             ");
	text.append(Utils.doubleToString(unclassified(), 12,4) +  "     " +
	    Utils.doubleToString(pctUnclassified(),
		12, 4) + " %\n");
      }
      text.append("Total Number of Instances          ");
      text.append(Utils.doubleToString(m_WithClass, 12, 4) + "\n");
      if (m_MissingClass > 0) {
	text.append("Ignored Class Unknown Instances            ");
	text.append(Utils.doubleToString(m_MissingClass, 12, 4) + "\n");
      }
    } catch (Exception ex) {
      // Should never occur since the class is known to be nominal 
      // here
      System.err.println("Arggh - Must be a bug in Evaluation class");
    }

    return text.toString(); 
  }

  /**
   * Calls toMatrixString() with a default title.
   *
   * @return the confusion matrix as a string
   * @throws Exception if the class is numeric
   */
  public String toMatrixString() throws Exception {

    return toMatrixString("=== Confusion Matrix ===\n");
  }

  /**
   * Outputs the performance statistics as a classification confusion
   * matrix. For each class value, shows the distribution of 
   * predicted class values.
   *
   * @param title the title for the confusion matrix
   * @return the confusion matrix as a String
   * @throws Exception if the class is numeric
   */
  public String toMatrixString(String title) throws Exception {

    StringBuffer text = new StringBuffer();
    char [] IDChars = {'a','b','c','d','e','f','g','h','i','j',
	'k','l','m','n','o','p','q','r','s','t',
	'u','v','w','x','y','z'};
    int IDWidth;
    boolean fractional = false;

    if (!m_ClassIsNominal) {
      throw new Exception("Evaluation: No confusion matrix possible!");
    }

    // Find the maximum value in the matrix
    // and check for fractional display requirement 
    double maxval = 0;
    for(int i = 0; i < m_NumClasses; i++) {
      for(int j = 0; j < m_NumClasses; j++) {
	double current = m_ConfusionMatrix[i][j];
	if (current < 0) {
	  current *= -10;
	}
	if (current > maxval) {
	  maxval = current;
	}
	double fract = current - Math.rint(current);
	if (!fractional
	    && ((Math.log(fract) / Math.log(10)) >= -2)) {
	  fractional = true;
	}
      }
    }

    IDWidth = 1 + Math.max((int)(Math.log(maxval) / Math.log(10) 
	+ (fractional ? 3 : 0)),
	(int)(Math.log(m_NumClasses) / 
	    Math.log(IDChars.length)));
    text.append(title).append("\n");
    for(int i = 0; i < m_NumClasses; i++) {
      if (fractional) {
	text.append(" ").append(num2ShortID(i,IDChars,IDWidth - 3))
	.append("   ");
      } else {
	text.append(" ").append(num2ShortID(i,IDChars,IDWidth));
      }
    }
    text.append("   <-- classified as\n");
    for(int i = 0; i< m_NumClasses; i++) { 
      for(int j = 0; j < m_NumClasses; j++) {
	text.append(" ").append(
	    Utils.doubleToString(m_ConfusionMatrix[i][j],
		IDWidth,
		(fractional ? 2 : 0)));
      }
      text.append(" | ").append(num2ShortID(i,IDChars,IDWidth))
      .append(" = ").append(m_ClassNames[i]).append("\n");
    }
    return text.toString();
  }

  /**
   * Generates a breakdown of the accuracy for each class (with default title),
   * incorporating various information-retrieval statistics, such as
   * true/false positive rate, precision/recall/F-Measure.  Should be
   * useful for ROC curves, recall/precision curves.  
   * 
   * @return the statistics presented as a string
   * @throws Exception if class is not nominal
   */
  public String toClassDetailsString() throws Exception {

    return toClassDetailsString("=== Detailed Accuracy By Class ===\n");
  }

  /**
   * Generates a breakdown of the accuracy for each class,
   * incorporating various information-retrieval statistics, such as
   * true/false positive rate, precision/recall/F-Measure.  Should be
   * useful for ROC curves, recall/precision curves.  
   * 
   * @param title the title to prepend the stats string with 
   * @return the statistics presented as a string
   * @throws Exception if class is not nominal
   */
  public String toClassDetailsString(String title) throws Exception {

    if (!m_ClassIsNominal) {
      throw new Exception("Evaluation: No confusion matrix possible!");
    }
    StringBuffer text = new StringBuffer(title 
	+ "\nTP Rate   FP Rate"
	+ "   Precision   Recall"
	+ "  F-Measure   ROC Area  Class\n");
    for(int i = 0; i < m_NumClasses; i++) {
      text.append(Utils.doubleToString(truePositiveRate(i), 7, 3))
      .append("   ");
      text.append(Utils.doubleToString(falsePositiveRate(i), 7, 3))
      .append("    ");
      text.append(Utils.doubleToString(precision(i), 7, 3))
      .append("   ");
      text.append(Utils.doubleToString(recall(i), 7, 3))
      .append("   ");
      text.append(Utils.doubleToString(fMeasure(i), 7, 3))
      .append("    ");
      double rocVal = areaUnderROC(i);
      if (Instance.isMissingValue(rocVal)) {
	text.append("  ?    ")
	.append("    ");
      } else {
	text.append(Utils.doubleToString(rocVal, 7, 3))
	.append("    ");
      }
      text.append(m_ClassNames[i]).append('\n');
    }
    return text.toString();
  }

  /**
   * Calculate the number of true positives with respect to a particular class. 
   * This is defined as<p/>
   * <pre>
   * correctly classified positives
   * </pre>
   *
   * @param classIndex the index of the class to consider as "positive"
   * @return the true positive rate
   */
  public double numTruePositives(int classIndex) {

    double correct = 0;
    for (int j = 0; j < m_NumClasses; j++) {
      if (j == classIndex) {
	correct += m_ConfusionMatrix[classIndex][j];
      }
    }
    return correct;
  }

  /**
   * Calculate the true positive rate with respect to a particular class. 
   * This is defined as<p/>
   * <pre>
   * correctly classified positives
   * ------------------------------
   *       total positives
   * </pre>
   *
   * @param classIndex the index of the class to consider as "positive"
   * @return the true positive rate
   */
  public double truePositiveRate(int classIndex) {

    double correct = 0, total = 0;
    for (int j = 0; j < m_NumClasses; j++) {
      if (j == classIndex) {
	correct += m_ConfusionMatrix[classIndex][j];
      }
      total += m_ConfusionMatrix[classIndex][j];
    }
    if (total == 0) {
      return 0;
    }
    return correct / total;
  }

  /**
   * Calculate the number of true negatives with respect to a particular class. 
   * This is defined as<p/>
   * <pre>
   * correctly classified negatives
   * </pre>
   *
   * @param classIndex the index of the class to consider as "positive"
   * @return the true positive rate
   */
  public double numTrueNegatives(int classIndex) {

    double correct = 0;
    for (int i = 0; i < m_NumClasses; i++) {
      if (i != classIndex) {
	for (int j = 0; j < m_NumClasses; j++) {
	  if (j != classIndex) {
	    correct += m_ConfusionMatrix[i][j];
	  }
	}
      }
    }
    return correct;
  }

  /**
   * Calculate the true negative rate with respect to a particular class. 
   * This is defined as<p/>
   * <pre>
   * correctly classified negatives
   * ------------------------------
   *       total negatives
   * </pre>
   *
   * @param classIndex the index of the class to consider as "positive"
   * @return the true positive rate
   */
  public double trueNegativeRate(int classIndex) {

    double correct = 0, total = 0;
    for (int i = 0; i < m_NumClasses; i++) {
      if (i != classIndex) {
	for (int j = 0; j < m_NumClasses; j++) {
	  if (j != classIndex) {
	    correct += m_ConfusionMatrix[i][j];
	  }
	  total += m_ConfusionMatrix[i][j];
	}
      }
    }
    if (total == 0) {
      return 0;
    }
    return correct / total;
  }

  /**
   * Calculate number of false positives with respect to a particular class. 
   * This is defined as<p/>
   * <pre>
   * incorrectly classified negatives
   * </pre>
   *
   * @param classIndex the index of the class to consider as "positive"
   * @return the false positive rate
   */
  public double numFalsePositives(int classIndex) {

    double incorrect = 0;
    for (int i = 0; i < m_NumClasses; i++) {
      if (i != classIndex) {
	for (int j = 0; j < m_NumClasses; j++) {
	  if (j == classIndex) {
	    incorrect += m_ConfusionMatrix[i][j];
	  }
	}
      }
    }
    return incorrect;
  }

  /**
   * Calculate the false positive rate with respect to a particular class. 
   * This is defined as<p/>
   * <pre>
   * incorrectly classified negatives
   * --------------------------------
   *        total negatives
   * </pre>
   *
   * @param classIndex the index of the class to consider as "positive"
   * @return the false positive rate
   */
  public double falsePositiveRate(int classIndex) {

    double incorrect = 0, total = 0;
    for (int i = 0; i < m_NumClasses; i++) {
      if (i != classIndex) {
	for (int j = 0; j < m_NumClasses; j++) {
	  if (j == classIndex) {
	    incorrect += m_ConfusionMatrix[i][j];
	  }
	  total += m_ConfusionMatrix[i][j];
	}
      }
    }
    if (total == 0) {
      return 0;
    }
    return incorrect / total;
  }

  /**
   * Calculate number of false negatives with respect to a particular class. 
   * This is defined as<p/>
   * <pre>
   * incorrectly classified positives
   * </pre>
   *
   * @param classIndex the index of the class to consider as "positive"
   * @return the false positive rate
   */
  public double numFalseNegatives(int classIndex) {

    double incorrect = 0;
    for (int i = 0; i < m_NumClasses; i++) {
      if (i == classIndex) {
	for (int j = 0; j < m_NumClasses; j++) {
	  if (j != classIndex) {
	    incorrect += m_ConfusionMatrix[i][j];
	  }
	}
      }
    }
    return incorrect;
  }

  /**
   * Calculate the false negative rate with respect to a particular class. 
   * This is defined as<p/>
   * <pre>
   * incorrectly classified positives
   * --------------------------------
   *        total positives
   * </pre>
   *
   * @param classIndex the index of the class to consider as "positive"
   * @return the false positive rate
   */
  public double falseNegativeRate(int classIndex) {

    double incorrect = 0, total = 0;
    for (int i = 0; i < m_NumClasses; i++) {
      if (i == classIndex) {
	for (int j = 0; j < m_NumClasses; j++) {
	  if (j != classIndex) {
	    incorrect += m_ConfusionMatrix[i][j];
	  }
	  total += m_ConfusionMatrix[i][j];
	}
      }
    }
    if (total == 0) {
      return 0;
    }
    return incorrect / total;
  }

  /**
   * Calculate the recall with respect to a particular class. 
   * This is defined as<p/>
   * <pre>
   * correctly classified positives
   * ------------------------------
   *       total positives
   * </pre><p/>
   * (Which is also the same as the truePositiveRate.)
   *
   * @param classIndex the index of the class to consider as "positive"
   * @return the recall
   */
  public double recall(int classIndex) {

    return truePositiveRate(classIndex);
  }

  /**
   * Calculate the precision with respect to a particular class. 
   * This is defined as<p/>
   * <pre>
   * correctly classified positives
   * ------------------------------
   *  total predicted as positive
   * </pre>
   *
   * @param classIndex the index of the class to consider as "positive"
   * @return the precision
   */
  public double precision(int classIndex) {

    double correct = 0, total = 0;
    for (int i = 0; i < m_NumClasses; i++) {
      if (i == classIndex) {
	correct += m_ConfusionMatrix[i][classIndex];
      }
      total += m_ConfusionMatrix[i][classIndex];
    }
    if (total == 0) {
      return 0;
    }
    return correct / total;
  }

  /**
   * Calculate the F-Measure with respect to a particular class. 
   * This is defined as<p/>
   * <pre>
   * 2 * recall * precision
   * ----------------------
   *   recall + precision
   * </pre>
   *
   * @param classIndex the index of the class to consider as "positive"
   * @return the F-Measure
   */
  public double fMeasure(int classIndex) {

    double precision = precision(classIndex);
    double recall = recall(classIndex);
    if ((precision + recall) == 0) {
      return 0;
    }
    return 2 * precision * recall / (precision + recall);
  }

  /**
   * Sets the class prior probabilities
   *
   * @param train the training instances used to determine
   * the prior probabilities
   * @throws Exception if the class attribute of the instances is not
   * set
   */
  public void setPriors(Instances train) throws Exception {
    m_NoPriors = false;

    if (!m_ClassIsNominal) {

      m_NumTrainClassVals = 0;
      m_TrainClassVals = null;
      m_TrainClassWeights = null;
      m_PriorErrorEstimator = null;
      m_ErrorEstimator = null;

      for (int i = 0; i < train.numInstances(); i++) {
	Instance currentInst = train.instance(i);
	if (!currentInst.classIsMissing()) {
	  addNumericTrainClass(currentInst.classValue(), 
	      currentInst.weight());
	}
      }

    } else {
      for (int i = 0; i < m_NumClasses; i++) {
	m_ClassPriors[i] = 1;
      }
      m_ClassPriorsSum = m_NumClasses;
      for (int i = 0; i < train.numInstances(); i++) {
	if (!train.instance(i).classIsMissing()) {
	  m_ClassPriors[(int)train.instance(i).classValue()] += 
	    train.instance(i).weight();
	  m_ClassPriorsSum += train.instance(i).weight();
	}
      }
    }
  }

  /**
   * Get the current weighted class counts
   * 
   * @return the weighted class counts
   */
  public double [] getClassPriors() {
    return m_ClassPriors;
  }

  /**
   * Updates the class prior probabilities (when incrementally 
   * training)
   *
   * @param instance the new training instance seen
   * @throws Exception if the class of the instance is not
   * set
   */
  public void updatePriors(Instance instance) throws Exception {
    if (!instance.classIsMissing()) {
      if (!m_ClassIsNominal) {
	if (!instance.classIsMissing()) {
	  addNumericTrainClass(instance.classValue(), 
	      instance.weight());
	}
      } else {
	m_ClassPriors[(int)instance.classValue()] += 
	  instance.weight();
	m_ClassPriorsSum += instance.weight();
      }
    }    
  }

  /**
   * disables the use of priors, e.g., in case of de-serialized schemes
   * that have no access to the original training set, but are evaluated
   * on a set set.
   */
  public void useNoPriors() {
    m_NoPriors = true;
  }

  /**
   * Tests whether the current evaluation object is equal to another
   * evaluation object
   *
   * @param obj the object to compare against
   * @return true if the two objects are equal
   */
  public boolean equals(Object obj) {

    if ((obj == null) || !(obj.getClass().equals(this.getClass()))) {
      return false;
    }
    Evaluation cmp = (Evaluation) obj;
    if (m_ClassIsNominal != cmp.m_ClassIsNominal) return false;
    if (m_NumClasses != cmp.m_NumClasses) return false;

    if (m_Incorrect != cmp.m_Incorrect) return false;
    if (m_Correct != cmp.m_Correct) return false;
    if (m_Unclassified != cmp.m_Unclassified) return false;
    if (m_MissingClass != cmp.m_MissingClass) return false;
    if (m_WithClass != cmp.m_WithClass) return false;

    if (m_SumErr != cmp.m_SumErr) return false;
    if (m_SumAbsErr != cmp.m_SumAbsErr) return false;
    if (m_SumSqrErr != cmp.m_SumSqrErr) return false;
    if (m_SumClass != cmp.m_SumClass) return false;
    if (m_SumSqrClass != cmp.m_SumSqrClass) return false;
    if (m_SumPredicted != cmp.m_SumPredicted) return false;
    if (m_SumSqrPredicted != cmp.m_SumSqrPredicted) return false;
    if (m_SumClassPredicted != cmp.m_SumClassPredicted) return false;

    if (m_ClassIsNominal) {
      for (int i = 0; i < m_NumClasses; i++) {
	for (int j = 0; j < m_NumClasses; j++) {
	  if (m_ConfusionMatrix[i][j] != cmp.m_ConfusionMatrix[i][j]) {
	    return false;
	  }
	}
      }
    }

    return true;
  }

  /**
   * Prints the predictions for the given dataset into a String variable.
   * 
   * @param classifier		the classifier to use
   * @param train		the training data
   * @param testSource		the test set
   * @param classIndex		the class index (1-based), if -1 ot does not 
   * 				override the class index is stored in the data 
   * 				file (by using the last attribute)
   * @param attributesToOutput	the indices of the attributes to output
   * @return			the generated predictions for the attribute range
   * @throws Exception 		if test file cannot be opened
   */
  public static String printClassifications(Classifier classifier, 
      Instances train,
      DataSource testSource,
      int classIndex,
      Range attributesToOutput) throws Exception {
    
    return printClassifications(
	classifier, train, testSource, classIndex, attributesToOutput, false);
  }

  /**
   * Prints the predictions for the given dataset into a String variable.
   * 
   * @param classifier		the classifier to use
   * @param train		the training data
   * @param testSource		the test set
   * @param classIndex		the class index (1-based), if -1 ot does not 
   * 				override the class index is stored in the data 
   * 				file (by using the last attribute)
   * @param attributesToOutput	the indices of the attributes to output
   * @param printDistribution	prints the complete distribution for nominal 
   * 				classes, not just the predicted value
   * @return			the generated predictions for the attribute range
   * @throws Exception 		if test file cannot be opened
   */
  public static String printClassifications(Classifier classifier, 
      Instances train,
      DataSource testSource,
      int classIndex,
      Range attributesToOutput,
      boolean printDistribution) throws Exception {

    StringBuffer text = new StringBuffer();
    if (testSource != null) {
      Instances test = testSource.getStructure();
      if (classIndex != -1) {
	test.setClassIndex(classIndex - 1);
      } else {
	if (test.classIndex() == -1)
	  test.setClassIndex(test.numAttributes() - 1);
      }

      // print header
      if (test.classAttribute().isNominal())
	if (printDistribution)
	  text.append(" inst#     actual  predicted error distribution");
	else
	  text.append(" inst#     actual  predicted error prediction");
      else
	text.append(" inst#     actual  predicted      error");
      if (attributesToOutput != null) {
	attributesToOutput.setUpper(test.numAttributes() - 1);
	text.append(" (");
	boolean first = true;
	for (int i = 0; i < test.numAttributes(); i++) {
	  if (i == test.classIndex())
	    continue;

	  if (attributesToOutput.isInRange(i)) {
	    if (!first)
	      text.append(",");
	    text.append(test.attribute(i).name());
	    first = false;
	  }
	}
	text.append(")");
      }
      text.append("\n");

      // print predictions
      int i = 0;
      testSource.reset();
      test = testSource.getStructure(test.classIndex());
      while (testSource.hasMoreElements(test)) {
	Instance inst = testSource.nextElement(test);
	text.append(
	    predictionText(
		classifier, inst, i, attributesToOutput, printDistribution));
	i++;
      }
    }
    return text.toString();
  }

  /**
   * returns the prediction made by the classifier as a string
   * 
   * @param classifier		the classifier to use
   * @param inst		the instance to generate text from
   * @param instNum		the index in the dataset
   * @param attributesToOutput	the indices of the attributes to output
   * @param printDistribution	prints the complete distribution for nominal 
   * 				classes, not just the predicted value
   * @return			the generated text
   * @throws Exception		if something goes wrong
   * @see			#printClassifications(Classifier, Instances, String, int, Range, boolean)
   */
  protected static String predictionText(Classifier classifier, 
      Instance inst, 
      int instNum,
      Range attributesToOutput,
      boolean printDistribution) 
  throws Exception {

    StringBuffer result = new StringBuffer();
    int width = 10;
    int prec = 3;

    Instance withMissing = (Instance)inst.copy();
    withMissing.setDataset(inst.dataset());
    double predValue = ((Classifier)classifier).classifyInstance(withMissing);

    // index
    result.append(Utils.padLeft("" + (instNum+1), 6));

    if (inst.dataset().classAttribute().isNumeric()) {
      // actual
      if (inst.classIsMissing())
	result.append(" " + Utils.padLeft("?", width));
      else
	result.append(" " + Utils.doubleToString(inst.classValue(), width, prec));
      // predicted
      if (Instance.isMissingValue(predValue))
	result.append(" " + Utils.padLeft("?", width));
      else
	result.append(" " + Utils.doubleToString(predValue, width, prec));
      // error
      if (Instance.isMissingValue(predValue) || inst.classIsMissing())
	result.append(" " + Utils.padLeft("?", width));
      else
	result.append(" " + Utils.doubleToString(predValue - inst.classValue(), width, prec));
    } else {
      // actual
      result.append(" " + Utils.padLeft(((int) inst.classValue()+1) + ":" + inst.toString(inst.classIndex()), width));
      // predicted
      if (Instance.isMissingValue(predValue))
	result.append(" " + Utils.padLeft("?", width));
      else
	result.append(" " + Utils.padLeft(((int) predValue+1) + ":" + inst.dataset().classAttribute().value((int)predValue), width));
      // error?
      if ((int) predValue+1 != (int) inst.classValue()+1)
	result.append(" " + "  +  ");
      else
	result.append(" " + "     ");
      // prediction/distribution
      if (printDistribution) {
	if (Instance.isMissingValue(predValue)) {
	  result.append(" " + "?");
	}
	else {
	  result.append(" ");
	  double[] dist = classifier.distributionForInstance(withMissing);
	  for (int n = 0; n < dist.length; n++) {
	    if (n > 0)
	      result.append(",");
	    if (n == (int) predValue)
	      result.append("*");
            result.append(Utils.doubleToString(dist[n], prec));
	  }
	}
      }
      else {
	if (Instance.isMissingValue(predValue))
	  result.append(" " + "?");
	else
	  result.append(" " + Utils.doubleToString(classifier.distributionForInstance(withMissing) [(int)predValue], prec));
      }
    }

    // attributes
    result.append(" " + attributeValuesString(withMissing, attributesToOutput) + "\n");

    return result.toString();
  }

  /**
   * Builds a string listing the attribute values in a specified range of indices,
   * separated by commas and enclosed in brackets.
   *
   * @param instance the instance to print the values from
   * @param attRange the range of the attributes to list
   * @return a string listing values of the attributes in the range
   */
  protected static String attributeValuesString(Instance instance, Range attRange) {
    StringBuffer text = new StringBuffer();
    if (attRange != null) {
      boolean firstOutput = true;
      attRange.setUpper(instance.numAttributes() - 1);
      for (int i=0; i<instance.numAttributes(); i++)
	if (attRange.isInRange(i) && i != instance.classIndex()) {
	  if (firstOutput) text.append("(");
	  else text.append(",");
	  text.append(instance.toString(i));
	  firstOutput = false;
	}
      if (!firstOutput) text.append(")");
    }
    return text.toString();
  }

  /**
   * Make up the help string giving all the command line options
   *
   * @param classifier the classifier to include options for
   * @return a string detailing the valid command line options
   */
  protected static String makeOptionString(Classifier classifier) {

    StringBuffer optionsText = new StringBuffer("");

    // General options
    optionsText.append("\n\nGeneral options:\n\n");
    optionsText.append("-t <name of training file>\n");
    optionsText.append("\tSets training file.\n");
    optionsText.append("-T <name of test file>\n");
    optionsText.append("\tSets test file. If missing, a cross-validation will be performed\n");
    optionsText.append("\ton the training data.\n");
    optionsText.append("-c <class index>\n");
    optionsText.append("\tSets index of class attribute (default: last).\n");
    optionsText.append("-x <number of folds>\n");
    optionsText.append("\tSets number of folds for cross-validation (default: 10).\n");
    optionsText.append("-no-cv\n");
    optionsText.append("\tDo not perform any cross validation.\n");
    optionsText.append("-split-percentage <percentage>\n");
    optionsText.append("\tSets the percentage for the train/test set split, e.g., 66.\n");
    optionsText.append("-preserve-order\n");
    optionsText.append("\tPreserves the order in the percentage split.\n");
    optionsText.append("-s <random number seed>\n");
    optionsText.append("\tSets random number seed for cross-validation or percentage split\n");
    optionsText.append("\t(default: 1).\n");
    optionsText.append("-m <name of file with cost matrix>\n");
    optionsText.append("\tSets file with cost matrix.\n");
    optionsText.append("-l <name of input file>\n");
    optionsText.append("\tSets model input file. In case the filename ends with '.xml',\n");
    optionsText.append("\tthe options are loaded from the XML file.\n");
    optionsText.append("-d <name of output file>\n");
    optionsText.append("\tSets model output file. In case the filename ends with '.xml',\n");
    optionsText.append("\tonly the options are saved to the XML file, not the model.\n");
    optionsText.append("-v\n");
    optionsText.append("\tOutputs no statistics for training data.\n");
    optionsText.append("-o\n");
    optionsText.append("\tOutputs statistics only, not the classifier.\n");
    optionsText.append("-i\n");
    optionsText.append("\tOutputs detailed information-retrieval");
    optionsText.append(" statistics for each class.\n");
    optionsText.append("-k\n");
    optionsText.append("\tOutputs information-theoretic statistics.\n");
    optionsText.append("-p <attribute range>\n");
    optionsText.append("\tOnly outputs predictions for test instances (or the train\n"
	+ "\tinstances if no test instances provided), along with attributes\n"
	+ "\t(0 for none).\n");
    optionsText.append("-distribution\n");
    optionsText.append("\tOutputs the distribution instead of only the prediction\n");
    optionsText.append("\tin conjunction with the '-p' option (only nominal classes).\n");
    optionsText.append("-r\n");
    optionsText.append("\tOnly outputs cumulative margin distribution.\n");
    if (classifier instanceof Sourcable) {
      optionsText.append("-z <class name>\n");
      optionsText.append("\tOnly outputs the source representation"
	  + " of the classifier,\n\tgiving it the supplied"
	  + " name.\n");
    }
    if (classifier instanceof Drawable) {
      optionsText.append("-g\n");
      optionsText.append("\tOnly outputs the graph representation"
	  + " of the classifier.\n");
    }
    optionsText.append("-xml filename | xml-string\n");
    optionsText.append("\tRetrieves the options from the XML-data instead of the " 
	+ "command line.\n");
    optionsText.append("-threshold-file <file>\n");
    optionsText.append("\tThe file to save the threshold data to.\n"
	+ "\tThe format is determined by the extensions, e.g., '.arff' for ARFF \n"
	+ "\tformat or '.csv' for CSV.\n");
    optionsText.append("-threshold-label <label>\n");
    optionsText.append("\tThe class label to determine the threshold data for\n"
	+ "\t(default is the first label)\n");

    // Get scheme-specific options
    if (classifier instanceof OptionHandler) {
      optionsText.append("\nOptions specific to "
	  + classifier.getClass().getName()
	  + ":\n\n");
      Enumeration enu = ((OptionHandler)classifier).listOptions();
      while (enu.hasMoreElements()) {
	Option option = (Option) enu.nextElement();
	optionsText.append(option.synopsis() + '\n');
	optionsText.append(option.description() + "\n");
      }
    }
    return optionsText.toString();
  }

  /**
   * Method for generating indices for the confusion matrix.
   *
   * @param num 	integer to format
   * @param IDChars	the characters to use
   * @param IDWidth	the width of the entry
   * @return 		the formatted integer as a string
   */
  protected String num2ShortID(int num, char[] IDChars, int IDWidth) {

    char ID [] = new char [IDWidth];
    int i;

    for(i = IDWidth - 1; i >=0; i--) {
      ID[i] = IDChars[num % IDChars.length];
      num = num / IDChars.length - 1;
      if (num < 0) {
	break;
      }
    }
    for(i--; i >= 0; i--) {
      ID[i] = ' ';
    }

    return new String(ID);
  }

  /**
   * Convert a single prediction into a probability distribution
   * with all zero probabilities except the predicted value which
   * has probability 1.0;
   *
   * @param predictedClass the index of the predicted class
   * @return the probability distribution
   */
  protected double [] makeDistribution(double predictedClass) {

    double [] result = new double [m_NumClasses];
    if (Instance.isMissingValue(predictedClass)) {
      return result;
    }
    if (m_ClassIsNominal) {
      result[(int)predictedClass] = 1.0;
    } else {
      result[0] = predictedClass;
    }
    return result;
  } 

  /**
   * Updates all the statistics about a classifiers performance for 
   * the current test instance.
   *
   * @param predictedDistribution the probabilities assigned to 
   * each class
   * @param instance the instance to be classified
   * @throws Exception if the class of the instance is not
   * set
   */
  protected void updateStatsForClassifier(double [] predictedDistribution,
      Instance instance)
  throws Exception {

    int actualClass = (int)instance.classValue();

    if (!instance.classIsMissing()) {
      updateMargins(predictedDistribution, actualClass, instance.weight());

      // Determine the predicted class (doesn't detect multiple 
      // classifications)
      int predictedClass = -1;
      double bestProb = 0.0;
      for(int i = 0; i < m_NumClasses; i++) {
	if (predictedDistribution[i] > bestProb) {
	  predictedClass = i;
	  bestProb = predictedDistribution[i];
	}
      }

      m_WithClass += instance.weight();

      // Determine misclassification cost
      if (m_CostMatrix != null) {
	if (predictedClass < 0) {
	  // For missing predictions, we assume the worst possible cost.
	  // This is pretty harsh.
	  // Perhaps we could take the negative of the cost of a correct
	  // prediction (-m_CostMatrix.getElement(actualClass,actualClass)),
	  // although often this will be zero
	  m_TotalCost += instance.weight()
	  * m_CostMatrix.getMaxCost(actualClass, instance);
	} else {
	  m_TotalCost += instance.weight() 
	  * m_CostMatrix.getElement(actualClass, predictedClass,
	      instance);
	}
      }

      // Update counts when no class was predicted
      if (predictedClass < 0) {
	m_Unclassified += instance.weight();
	return;
      }

      double predictedProb = Math.max(MIN_SF_PROB,
	  predictedDistribution[actualClass]);
      double priorProb = Math.max(MIN_SF_PROB,
	  m_ClassPriors[actualClass]
	                / m_ClassPriorsSum);
      if (predictedProb >= priorProb) {
	m_SumKBInfo += (Utils.log2(predictedProb) - 
	    Utils.log2(priorProb))
	    * instance.weight();
      } else {
	m_SumKBInfo -= (Utils.log2(1.0-predictedProb) - 
	    Utils.log2(1.0-priorProb))
	    * instance.weight();
      }

      m_SumSchemeEntropy -= Utils.log2(predictedProb) * instance.weight();
      m_SumPriorEntropy -= Utils.log2(priorProb) * instance.weight();

      updateNumericScores(predictedDistribution, 
	  makeDistribution(instance.classValue()), 
	  instance.weight());

      // Update other stats
      m_ConfusionMatrix[actualClass][predictedClass] += instance.weight();
      if (predictedClass != actualClass) {
	m_Incorrect += instance.weight();
      } else {
	m_Correct += instance.weight();
      }
    } else {
      m_MissingClass += instance.weight();
    }
  }

  /**
   * Updates all the statistics about a predictors performance for 
   * the current test instance.
   *
   * @param predictedValue the numeric value the classifier predicts
   * @param instance the instance to be classified
   * @throws Exception if the class of the instance is not
   * set
   */
  protected void updateStatsForPredictor(double predictedValue,
      Instance instance) 
  throws Exception {

    if (!instance.classIsMissing()){

      // Update stats
      m_WithClass += instance.weight();
      if (Instance.isMissingValue(predictedValue)) {
	m_Unclassified += instance.weight();
	return;
      }
      m_SumClass += instance.weight() * instance.classValue();
      m_SumSqrClass += instance.weight() * instance.classValue()
      *	instance.classValue();
      m_SumClassPredicted += instance.weight() 
      * instance.classValue() * predictedValue;
      m_SumPredicted += instance.weight() * predictedValue;
      m_SumSqrPredicted += instance.weight() * predictedValue * predictedValue;

      if (m_ErrorEstimator == null) {
	setNumericPriorsFromBuffer();
      }
      double predictedProb = Math.max(m_ErrorEstimator.getProbability(
	  predictedValue 
	  - instance.classValue()),
	  MIN_SF_PROB);
      double priorProb = Math.max(m_PriorErrorEstimator.getProbability(
	  instance.classValue()),
	  MIN_SF_PROB);

      m_SumSchemeEntropy -= Utils.log2(predictedProb) * instance.weight();
      m_SumPriorEntropy -= Utils.log2(priorProb) * instance.weight();
      m_ErrorEstimator.addValue(predictedValue - instance.classValue(), 
	  instance.weight());

      updateNumericScores(makeDistribution(predictedValue),
	  makeDistribution(instance.classValue()),
	  instance.weight());

    } else
      m_MissingClass += instance.weight();
  }

  /**
   * Update the cumulative record of classification margins
   *
   * @param predictedDistribution the probability distribution predicted for
   * the current instance
   * @param actualClass the index of the actual instance class
   * @param weight the weight assigned to the instance
   */
  protected void updateMargins(double [] predictedDistribution, 
      int actualClass, double weight) {

    double probActual = predictedDistribution[actualClass];
    double probNext = 0;

    for(int i = 0; i < m_NumClasses; i++)
      if ((i != actualClass) &&
	  (predictedDistribution[i] > probNext))
	probNext = predictedDistribution[i];

    double margin = probActual - probNext;
    int bin = (int)((margin + 1.0) / 2.0 * k_MarginResolution);
    m_MarginCounts[bin] += weight;
  }

  /**
   * Update the numeric accuracy measures. For numeric classes, the
   * accuracy is between the actual and predicted class values. For 
   * nominal classes, the accuracy is between the actual and 
   * predicted class probabilities.
   *
   * @param predicted the predicted values
   * @param actual the actual value
   * @param weight the weight associated with this prediction
   */
  protected void updateNumericScores(double [] predicted, 
      double [] actual, double weight) {

    double diff;
    double sumErr = 0, sumAbsErr = 0, sumSqrErr = 0;
    double sumPriorAbsErr = 0, sumPriorSqrErr = 0;
    for(int i = 0; i < m_NumClasses; i++) {
      diff = predicted[i] - actual[i];
      sumErr += diff;
      sumAbsErr += Math.abs(diff);
      sumSqrErr += diff * diff;
      diff = (m_ClassPriors[i] / m_ClassPriorsSum) - actual[i];
      sumPriorAbsErr += Math.abs(diff);
      sumPriorSqrErr += diff * diff;
    }
    m_SumErr += weight * sumErr / m_NumClasses;
    m_SumAbsErr += weight * sumAbsErr / m_NumClasses;
    m_SumSqrErr += weight * sumSqrErr / m_NumClasses;
    m_SumPriorAbsErr += weight * sumPriorAbsErr / m_NumClasses;
    m_SumPriorSqrErr += weight * sumPriorSqrErr / m_NumClasses;
  }

  /**
   * Adds a numeric (non-missing) training class value and weight to 
   * the buffer of stored values.
   *
   * @param classValue the class value
   * @param weight the instance weight
   */
  protected void addNumericTrainClass(double classValue, double weight) {

    if (m_TrainClassVals == null) {
      m_TrainClassVals = new double [100];
      m_TrainClassWeights = new double [100];
    }
    if (m_NumTrainClassVals == m_TrainClassVals.length) {
      double [] temp = new double [m_TrainClassVals.length * 2];
      System.arraycopy(m_TrainClassVals, 0, 
	  temp, 0, m_TrainClassVals.length);
      m_TrainClassVals = temp;

      temp = new double [m_TrainClassWeights.length * 2];
      System.arraycopy(m_TrainClassWeights, 0, 
	  temp, 0, m_TrainClassWeights.length);
      m_TrainClassWeights = temp;
    }
    m_TrainClassVals[m_NumTrainClassVals] = classValue;
    m_TrainClassWeights[m_NumTrainClassVals] = weight;
    m_NumTrainClassVals++;
  }

  /**
   * Sets up the priors for numeric class attributes from the 
   * training class values that have been seen so far.
   */
  protected void setNumericPriorsFromBuffer() {

    double numPrecision = 0.01; // Default value
    if (m_NumTrainClassVals > 1) {
      double [] temp = new double [m_NumTrainClassVals];
      System.arraycopy(m_TrainClassVals, 0, temp, 0, m_NumTrainClassVals);
      int [] index = Utils.sort(temp);
      double lastVal = temp[index[0]];
      double deltaSum = 0;
      int distinct = 0;
      for (int i = 1; i < temp.length; i++) {
	double current = temp[index[i]];
	if (current != lastVal) {
	  deltaSum += current - lastVal;
	  lastVal = current;
	  distinct++;
	}
      }
      if (distinct > 0) {
	numPrecision = deltaSum / distinct;
      }
    }
    m_PriorErrorEstimator = new KernelEstimator(numPrecision);
    m_ErrorEstimator = new KernelEstimator(numPrecision);
    m_ClassPriors[0] = m_ClassPriorsSum = 0;
    for (int i = 0; i < m_NumTrainClassVals; i++) {
      m_ClassPriors[0] += m_TrainClassVals[i] * m_TrainClassWeights[i];
      m_ClassPriorsSum += m_TrainClassWeights[i];
      m_PriorErrorEstimator.addValue(m_TrainClassVals[i],
	  m_TrainClassWeights[i]);
    }
  }
}
