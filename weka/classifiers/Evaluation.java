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
 *    Copyright (C) 1999 Eibe Frank,Len Trigg
 *
 */

package weka.classifiers;

import java.util.*;
import java.io.*;
import weka.core.*;
import weka.estimators.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Class for evaluating machine learning models. <p>
 *
 * ------------------------------------------------------------------- <p>
 *
 * General options when evaluating a learning scheme from the command-line: <p>
 *
 * -t filename <br>
 * Name of the file with the training data. (required) <p>
 *
 * -T filename <br>
 * Name of the file with the test data. If missing a cross-validation 
 * is performed. <p>
 *
 * -c index <br>
 * Index of the class attribute (1, 2, ...; default: last). <p>
 *
 * -x number <br>
 * The number of folds for the cross-validation (default: 10). <p>
 *
 * -s seed <br>
 * Random number seed for the cross-validation (default: 1). <p>
 *
 * -m filename <br>
 * The name of a file containing a cost matrix. <p>
 *
 * -l filename <br>
 * Loads classifier from the given file. <p>
 *
 * -d filename <br>
 * Saves classifier built from the training data into the given file. <p>
 *
 * -v <br>
 * Outputs no statistics for the training data. <p>
 *
 * -o <br>
 * Outputs statistics only, not the classifier. <p>
 * 
 * -i <br>
 * Outputs information-retrieval statistics per class. <p>
 *
 * -k <br>
 * Outputs information-theoretic statistics. <p>
 *
 * -p range <br>
 * Outputs predictions for test instances, along with the attributes in 
 * the specified range (and nothing else). Use '-p 0' if no attributes are
 * desired. <p>
 *
 * -r <br>
 * Outputs cumulative margin distribution (and nothing else). <p>
 *
 * -g <br> 
 * Only for classifiers that implement "Graphable." Outputs
 * the graph representation of the classifier (and nothing
 * else). <p>
 *
 * ------------------------------------------------------------------- <p>
 *
 * Example usage as the main of a classifier (called FunkyClassifier):
 * <code> <pre>
 * public static void main(String [] args) {
 *   try {
 *     Classifier scheme = new FunkyClassifier();
 *     System.out.println(Evaluation.evaluateModel(scheme, args));
 *   } catch (Exception e) {
 *     System.err.println(e.getMessage());
 *   }
 * }
 * </pre> </code> 
 * <p>
 *
 * ------------------------------------------------------------------ <p>
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
 * @version  $Revision: 1.53.2.4 $
  */
public class Evaluation implements Summarizable {

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
  
  /**
   * Initializes all the counters for the evaluation.
   *
   * @param data set of training instances, to get some header 
   * information and prior class distribution information
   * @exception Exception if the class is not defined
   */
  public Evaluation(Instances data) throws Exception {
    
    this(data, null);
  }

  /**
   * Initializes all the counters for the evaluation and also takes a
   * cost matrix as parameter.
   *
   * @param data set of instances, to get some header information
   * @param costMatrix the cost matrix---if null, default costs will be used
   * @exception Exception if cost matrix is not compatible with 
   * data, the class is not defined or the class is numeric
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
   * for a classifier on a set of instances.
   *
   * @param classifier the classifier with any options set.
   * @param data the data on which the cross-validation is to be 
   * performed 
   * @param numFolds the number of folds for the cross-validation
   * @param random random number generator for randomization 
   * @exception Exception if a classifier could not be generated 
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
   * @param classifier a string naming the class of the classifier
   * @param data the data on which the cross-validation is to be 
   * performed 
   * @param numFolds the number of folds for the cross-validation
   * @param options the options to the classifier. Any options
   * @param random the random number generator for randomizing the data
   * accepted by the classifier will be removed from this array.
   * @exception Exception if a classifier could not be generated 
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
   * strings. <p>
   *
   * Valid options are: <p>
   *
   * -t filename <br>
   * Name of the file with the training data. (required) <p>
   *
   * -T filename <br>
   * Name of the file with the test data. If missing a cross-validation 
   * is performed. <p>
   *
   * -c index <br>
   * Index of the class attribute (1, 2, ...; default: last). <p>
   *
   * -x number <br>
   * The number of folds for the cross-validation (default: 10). <p>
   *
   * -s seed <br>
   * Random number seed for the cross-validation (default: 1). <p>
   *
   * -m filename <br>
   * The name of a file containing a cost matrix. <p>
   *
   * -l filename <br>
   * Loads classifier from the given file. <p>
   *
   * -d filename <br>
   * Saves classifier built from the training data into the given file. <p>
   *
   * -v <br>
   * Outputs no statistics for the training data. <p>
   *
   * -o <br>
   * Outputs statistics only, not the classifier. <p>
   * 
   * -i <br>
   * Outputs detailed information-retrieval statistics per class. <p>
   *
   * -k <br>
   * Outputs information-theoretic statistics. <p>
   *
   * -p range <br>
   * Outputs predictions for test instances, along with the attributes in 
   * the specified range (and nothing else). Use '-p 0' if no attributes are
   * desired. <p>
   *
   * -r <br>
   * Outputs cumulative margin distribution (and nothing else). <p>
   *
   * -g <br> 
   * Only for classifiers that implement "Graphable." Outputs
   * the graph representation of the classifier (and nothing
   * else). <p>
   *
   * @param classifierString class of machine learning classifier as a string
   * @param options the array of string containing the options
   * @exception Exception if model could not be evaluated successfully
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
   * strings. <p>
   *
   * Valid options are: <p>
   *
   * -t name of training file <br>
   * Name of the file with the training data. (required) <p>
   *
   * -T name of test file <br>
   * Name of the file with the test data. If missing a cross-validation 
   * is performed. <p>
   *
   * -c class index <br>
   * Index of the class attribute (1, 2, ...; default: last). <p>
   *
   * -x number of folds <br>
   * The number of folds for the cross-validation (default: 10). <p>
   *
   * -s random number seed <br>
   * Random number seed for the cross-validation (default: 1). <p>
   *
   * -m file with cost matrix <br>
   * The name of a file containing a cost matrix. <p>
   *
   * -l name of model input file <br>
   * Loads classifier from the given file. <p>
   *
   * -d name of model output file <br>
   * Saves classifier built from the training data into the given file. <p>
   *
   * -v <br>
   * Outputs no statistics for the training data. <p>
   *
   * -o <br>
   * Outputs statistics only, not the classifier. <p>
   * 
   * -i <br>
   * Outputs detailed information-retrieval statistics per class. <p>
   *
   * -k <br>
   * Outputs information-theoretic statistics. <p>
   *
   * -p <br>
   * Outputs predictions for test instances (and nothing else). <p>
   *
   * -r <br>
   * Outputs cumulative margin distribution (and nothing else). <p>
   *
   * -g <br> 
   * Only for classifiers that implement "Graphable." Outputs
   * the graph representation of the classifier (and nothing
   * else). <p>
   *
   * @param classifier machine learning classifier
   * @param options the array of string containing the options
   * @exception Exception if model could not be evaluated successfully
   * @return a string describing the results */
  public static String evaluateModel(Classifier classifier,
				     String [] options) throws Exception {
			      
    Instances train = null, tempTrain, test = null, template = null;
    int seed = 1, folds = 10, classIndex = -1;
    String trainFileName, testFileName, sourceClass, 
      classIndexString, seedString, foldsString, objectInputFileName, 
      objectOutputFileName, attributeRangeString;
    boolean noOutput = false,
      printClassifications = false, trainStatistics = true,
      printMargins = false, printComplexityStatistics = false,
      printGraph = false, classStatistics = false, printSource = false;
    StringBuffer text = new StringBuffer();
    BufferedReader trainReader = null, testReader = null;
    ObjectInputStream objectInputStream = null;
    CostMatrix costMatrix = null;
    StringBuffer schemeOptionsText = null;
    Range attributesToOutput = null;
    long trainTimeStart = 0, trainTimeElapsed = 0,
      testTimeStart = 0, testTimeElapsed = 0;
    Classifier classifierBackup;
    
    try {

      // Get basic options (options the same for all schemes)
      classIndexString = Utils.getOption('c', options);
      if (classIndexString.length() != 0) {
	classIndex = Integer.parseInt(classIndexString);
      }
      trainFileName = Utils.getOption('t', options); 
      objectInputFileName = Utils.getOption('l', options);
      objectOutputFileName = Utils.getOption('d', options);
      testFileName = Utils.getOption('T', options);
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
	  trainReader = new BufferedReader(new FileReader(trainFileName));
	}
	if (testFileName.length() != 0) {
	  testReader = new BufferedReader(new FileReader(testFileName));
	}
	if (objectInputFileName.length() != 0) {
          InputStream is = new FileInputStream(objectInputFileName);
          if (objectInputFileName.endsWith(".gz")) {
            is = new GZIPInputStream(is);
          }
	  objectInputStream = new ObjectInputStream(is);
	}
      } catch (Exception e) {
	throw new Exception("Can't open file " + e.getMessage() + '.');
      }
      if (testFileName.length() != 0) {
	template = test = new Instances(testReader, 1);
	if (classIndex != -1) {
	  test.setClassIndex(classIndex - 1);
	} else {
	  test.setClassIndex(test.numAttributes() - 1);
	}
	if (classIndex > test.numAttributes()) {
	  throw new Exception("Index of class attribute too large.");
	}
      }
      if (trainFileName.length() != 0) {
	if ((classifier instanceof UpdateableClassifier) &&
	    (testFileName.length() != 0)) {
	  train = new Instances(trainReader, 1);
	} else {
	  train = new Instances(trainReader);
	}
        template = train;
	if (classIndex != -1) {
	  train.setClassIndex(classIndex - 1);
	} else {
	  train.setClassIndex(train.numAttributes() - 1);
	}
	if ((testFileName.length() != 0) && !test.equalHeaders(train)) {
	  throw new IllegalArgumentException("Train and test file not compatible!");
	}
	if (classIndex > train.numAttributes()) {
	  throw new Exception("Index of class attribute too large.");
	}
	//train = new Instances(train);
      }
      if (template == null) {
        throw new Exception("No actual dataset provided to use as template");
      }
      seedString = Utils.getOption('s', options);
      if (seedString.length() != 0) {
	seed = Integer.parseInt(seedString);
      }
      foldsString = Utils.getOption('x', options);
      if (foldsString.length() != 0) {
	folds = Integer.parseInt(foldsString);
      }
      costMatrix = handleCostOption(Utils.getOption('m', options), template.numClasses());

      classStatistics = Utils.getFlag('i', options);
      noOutput = Utils.getFlag('o', options);
      trainStatistics = !Utils.getFlag('v', options);
      printComplexityStatistics = Utils.getFlag('k', options);
      printMargins = Utils.getFlag('r', options);
      printGraph = Utils.getFlag('g', options);
      sourceClass = Utils.getOption('z', options);
      printSource = (sourceClass.length() != 0);
      
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
    
    if (objectInputFileName.length() != 0) {
      
      // Load classifier from file
      classifier = (Classifier) objectInputStream.readObject();
      objectInputStream.close();
    }
    
    // backup of fully setup classifier for cross-validation
    classifierBackup = Classifier.makeCopy(classifier);
    
    // Build the classifier if no object file provided
    if ((classifier instanceof UpdateableClassifier) &&
	(testFileName.length() != 0) &&
	(costMatrix == null) &&
	(trainFileName.length() != 0)) {
      
      // Build classifier incrementally
      trainingEvaluation.setPriors(train);
      testingEvaluation.setPriors(train);
      trainTimeStart = System.currentTimeMillis();
      if (objectInputFileName.length() == 0) {
	classifier.buildClassifier(train);
      }
      while (train.readInstance(trainReader)) {
	
	trainingEvaluation.updatePriors(train.instance(0));
	testingEvaluation.updatePriors(train.instance(0));
	((UpdateableClassifier)classifier).
	  updateClassifier(train.instance(0));
	train.delete(0);
      }
      trainTimeElapsed = System.currentTimeMillis() - trainTimeStart;
      trainReader.close();
    } else if (objectInputFileName.length() == 0) {
      
      // Build classifier in one go
      tempTrain = new Instances(train);
      trainingEvaluation.setPriors(tempTrain);
      testingEvaluation.setPriors(tempTrain);
      trainTimeStart = System.currentTimeMillis();
      classifier.buildClassifier(tempTrain);
      trainTimeElapsed = System.currentTimeMillis() - trainTimeStart;
    } 

    // Save the classifier if an object output file is provided
    if (objectOutputFileName.length() != 0) {
      OutputStream os = new FileOutputStream(objectOutputFileName);
      if (objectOutputFileName.endsWith(".gz")) {
        os = new GZIPOutputStream(os);
      }
      ObjectOutputStream objectOutputStream = new ObjectOutputStream(os);
      objectOutputStream.writeObject(classifier);
      objectOutputStream.flush();
      objectOutputStream.close();
    }

    // If classifier is drawable output string describing graph
    if ((classifier instanceof Drawable)
	&& (printGraph)){
      return ((Drawable)classifier).graph();
    }

    // Output the classifier as equivalent source
    if ((classifier instanceof Sourcable)
	&& (printSource)){
      return wekaStaticWrapper((Sourcable) classifier, sourceClass);
    }

    // Output test instance predictions only
    if (printClassifications) {
      return printClassifications(classifier, new Instances(template, 0),
				  testFileName, classIndex, attributesToOutput);
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
      text.append("\n=== Evaluation Cost Matrix ===\n\n")
        .append(costMatrix.toString());
    }

    // Compute error estimate from training data
    if ((trainStatistics) &&
	(trainFileName.length() != 0)) {

      if ((classifier instanceof UpdateableClassifier) &&
	  (testFileName.length() != 0) &&
	  (costMatrix == null)) {

	// Classifier was trained incrementally, so we have to 
	// reopen the training data in order to test on it.
	trainReader = new BufferedReader(new FileReader(trainFileName));

	// Incremental testing
	train = new Instances(trainReader, 1);
	if (classIndex != -1) {
	  train.setClassIndex(classIndex - 1);
	} else {
	  train.setClassIndex(train.numAttributes() - 1);
	}
	testTimeStart = System.currentTimeMillis();
	while (train.readInstance(trainReader)) {

	  trainingEvaluation.
	  evaluateModelOnce((Classifier)classifier, 
			    train.instance(0));
	  train.delete(0);
	}
	testTimeElapsed = System.currentTimeMillis() - testTimeStart;
	trainReader.close();
      } else {
	testTimeStart = System.currentTimeMillis();
	trainingEvaluation.evaluateModel(classifier, 
					 train);
	testTimeElapsed = System.currentTimeMillis() - testTimeStart;
      }

      // Print the results of the training evaluation
      if (printMargins) {
	return trainingEvaluation.toCumulativeMarginDistributionString();
      } else {
	text.append("\nTime taken to build model: " +
		    Utils.doubleToString(trainTimeElapsed / 1000.0,2) +
		    " seconds");
	text.append("\nTime taken to test model on training data: " +
		    Utils.doubleToString(testTimeElapsed / 1000.0,2) +
		    " seconds");
	text.append(trainingEvaluation.
		    toSummaryString("\n\n=== Error on training" + 
				    " data ===\n", printComplexityStatistics));
	if (template.classAttribute().isNominal()) {
	  if (classStatistics) {
	    text.append("\n\n" + trainingEvaluation.toClassDetailsString());
	  }
	  text.append("\n\n" + trainingEvaluation.toMatrixString());
	}
	
      }
    }

    // Compute proper error estimates
    if (testFileName.length() != 0) {

      // Testing is on the supplied test data
      while (test.readInstance(testReader)) {
	  
	testingEvaluation.evaluateModelOnce((Classifier)classifier, 
                                            test.instance(0));
	test.delete(0);
      }
      testReader.close();

      text.append("\n\n" + testingEvaluation.
		  toSummaryString("=== Error on test data ===\n",
				  printComplexityStatistics));
    } else if (trainFileName.length() != 0) {

      // Testing is via cross-validation on training data
      Random random = new Random(seed);
      // use untrained (!) classifier for cross-validation
      classifier = Classifier.makeCopy(classifierBackup);
      testingEvaluation.crossValidateModel(classifier, train, folds, random);
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
    if (template.classAttribute().isNominal()) {
      if (classStatistics) {
	text.append("\n\n" + testingEvaluation.toClassDetailsString());
      }
      text.append("\n\n" + testingEvaluation.toMatrixString());
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
   * @exception Exception if an error occurs.
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
   * @exception Exception if model could not be evaluated 
   * successfully 
   */
  public double[] evaluateModel(Classifier classifier,
			    Instances data) throws Exception {
    
    double predictions[] = new double[data.numInstances()];

    for (int i = 0; i < data.numInstances(); i++) {
      predictions[i] = evaluateModelOnce((Classifier)classifier, 
                                data.instance(i));
    }
    return predictions;
  }
  
  /**
   * Evaluates the classifier on a single instance.
   *
   * @param classifier machine learning classifier
   * @param instance the test instance to be classified
   * @return the prediction made by the clasifier
   * @exception Exception if model could not be evaluated 
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
   * @exception Exception if model could not be evaluated 
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
   * Evaluates the supplied prediction on a single instance.
   *
   * @param prediction the supplied prediction
   * @param instance the test instance to be classified
   * @exception Exception if model could not be evaluated 
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
   * Wraps a static classifier in enough source to test using the weka
   * class libraries.
   *
   * @param classifier a Sourcable Classifier
   * @param className the name to give to the source code class
   * @return the source for a static classifier that can be tested with
   * weka libraries.
   */
  protected static String wekaStaticWrapper(Sourcable classifier, 
                                            String className) 
    throws Exception {
    
    //String className = "StaticClassifier";
    String staticClassifier = classifier.toSource(className);
    return "package weka.classifiers;\n"
    +"import weka.core.Attribute;\n"
    +"import weka.core.Instance;\n"
    +"import weka.core.Instances;\n"
    +"import weka.classifiers.Classifier;\n\n"
    +"public class WekaWrapper extends Classifier {\n\n"
    +"  public void buildClassifier(Instances i) throws Exception {\n"
    +"  }\n\n"
    +"  public double classifyInstance(Instance i) throws Exception {\n\n"
    +"    Object [] s = new Object [i.numAttributes()];\n"
    +"    for (int j = 0; j < s.length; j++) {\n"
    +"      if (!i.isMissing(j)) {\n"
    +"        if (i.attribute(j).type() == Attribute.NOMINAL) {\n"
    +"          s[j] = i.attribute(j).value((int) i.value(j));\n"
    +"        } else if (i.attribute(j).type() == Attribute.NUMERIC) {\n"
    +"          s[j] = new Double(i.value(j));\n"
    +"        }\n"
    +"      }\n"
    +"    }\n"
    +"    return " + className + ".classify(s);\n"
    +"  }\n\n"
    +"}\n\n"
    +staticClassifier; // The static classifer class
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
      return Math.sqrt(m_SumSqrErr / m_WithClass);
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
   * @exception Exception if class is not numeric
   */
  public final double correlationCoefficient() throws Exception {

    if (m_ClassIsNominal) {
      throw
	new Exception("Can't compute correlation coefficient: " + 
		      "class is nominal!");
    }

    double correlation = 0;
    double varActual = 
      m_SumSqrClass - m_SumClass * m_SumClass / m_WithClass;
    double varPredicted = 
      m_SumSqrPredicted - m_SumPredicted * m_SumPredicted / 
      m_WithClass;
    double varProd = 
      m_SumClassPredicted - m_SumClass * m_SumPredicted / m_WithClass;

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

    return m_SumAbsErr / m_WithClass;
  }

  /**
   * Returns the mean absolute error of the prior.
   *
   * @return the mean absolute error 
   */
  public final double meanPriorAbsoluteError() {

    return m_SumPriorAbsErr / m_WithClass;
  }

  /**
   * Returns the relative absolute error.
   *
   * @return the relative absolute error 
   * @exception Exception if it can't be computed
   */
  public final double relativeAbsoluteError() throws Exception {

    return 100 * meanAbsoluteError() / meanPriorAbsoluteError();
  }
  
  /**
   * Returns the root mean squared error.
   *
   * @return the root mean squared error 
   */
  public final double rootMeanSquaredError() {

    return Math.sqrt(m_SumSqrErr / m_WithClass);
  }
  
  /**
   * Returns the root mean prior squared error.
   *
   * @return the root mean prior squared error 
   */
  public final double rootMeanPriorSquaredError() {

    return Math.sqrt(m_SumPriorSqrErr / m_WithClass);
  }
  
  /**
   * Returns the root relative squared error if the class is numeric.
   *
   * @return the root relative squared error 
   */
  public final double rootRelativeSquaredError() {

    return 100.0 * rootMeanSquaredError() / 
      rootMeanPriorSquaredError();
  }

  /**
   * Calculate the entropy of the prior distribution
   *
   * @return the entropy of the prior distribution
   * @exception Exception if the class is not nominal
   */
  public final double priorEntropy() throws Exception {

    if (!m_ClassIsNominal) {
      throw
	new Exception("Can't compute entropy of class prior: " + 
		      "class numeric!");
    }

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
   * @exception Exception if the class is not nominal
   */
  public final double KBInformation() throws Exception {

    if (!m_ClassIsNominal) {
      throw
	new Exception("Can't compute K&B Info score: " + 
		      "class numeric!");
    }
    return m_SumKBInfo;
  }

  /**
   * Return the Kononenko & Bratko Information score in bits per 
   * instance.
   *
   * @return the K&B information score
   * @exception Exception if the class is not nominal
   */
  public final double KBMeanInformation() throws Exception {

    if (!m_ClassIsNominal) {
      throw
	new Exception("Can't compute K&B Info score: "
		       + "class numeric!");
    }
    return m_SumKBInfo / m_WithClass;
  }

  /**
   * Return the Kononenko & Bratko Relative Information score
   *
   * @return the K&B relative information score
   * @exception Exception if the class is not nominal
   */
  public final double KBRelativeInformation() throws Exception {

    if (!m_ClassIsNominal) {
      throw
	new Exception("Can't compute K&B Info score: " + 
		      "class numeric!");
    }
    return 100.0 * KBInformation() / priorEntropy();
  }

  /**
   * Returns the total entropy for the null model
   * 
   * @return the total null model entropy
   */
  public final double SFPriorEntropy() {

    return m_SumPriorEntropy;
  }

  /**
   * Returns the entropy per instance for the null model
   * 
   * @return the null model entropy per instance
   */
  public final double SFMeanPriorEntropy() {

    return m_SumPriorEntropy / m_WithClass;
  }

  /**
   * Returns the total entropy for the scheme
   * 
   * @return the total scheme entropy
   */
  public final double SFSchemeEntropy() {

    return m_SumSchemeEntropy;
  }

  /**
   * Returns the entropy per instance for the scheme
   * 
   * @return the scheme entropy per instance
   */
  public final double SFMeanSchemeEntropy() {

    return m_SumSchemeEntropy / m_WithClass;
  }

  /**
   * Returns the total SF, which is the null model entropy minus
   * the scheme entropy.
   * 
   * @return the total SF
   */
  public final double SFEntropyGain() {

    return m_SumPriorEntropy - m_SumSchemeEntropy;
  }

  /**
   * Returns the SF per instance, which is the null model entropy
   * minus the scheme entropy, per instance.
   * 
   * @return the SF per instance
   */
  public final double SFMeanEntropyGain() {
    
    return (m_SumPriorEntropy - m_SumSchemeEntropy) / m_WithClass;
  }

  /**
   * Output the cumulative margin distribution as a string suitable
   * for input for gnuplot or similar package.
   *
   * @return the cumulative margin distribution
   * @exception Exception if the class attribute is nominal
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
    
    double mae, mad = 0;
    StringBuffer text = new StringBuffer();

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
	text.append("Relative absolute error            ");
	text.append(Utils.doubleToString(relativeAbsoluteError(), 
					 12, 4) + " %\n");
	text.append("Root relative squared error        ");
	text.append(Utils.doubleToString(rootRelativeSquaredError(), 
					 12, 4) + " %\n");
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
   * @exception Exception if the class is numeric
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
   * @exception Exception if the class is numeric
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
   */
  public String toClassDetailsString(String title) throws Exception {

    if (!m_ClassIsNominal) {
      throw new Exception("Evaluation: No confusion matrix possible!");
    }
    StringBuffer text = new StringBuffer(title 
					 + "\nTP Rate   FP Rate"
                                         + "   Precision   Recall"
                                         + "  F-Measure   Class\n");
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
      text.append(m_ClassNames[i]).append('\n');
    }
    return text.toString();
  }

  /**
   * Calculate the number of true positives with respect to a particular class. 
   * This is defined as<p>
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
   * This is defined as<p>
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
   * This is defined as<p>
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
   * This is defined as<p>
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
   * This is defined as<p>
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
   * This is defined as<p>
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
   * This is defined as<p>
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
   * This is defined as<p>
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
   * This is defined as<p>
   * <pre>
   * correctly classified positives
   * ------------------------------
   *       total positives
   * </pre><p>
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
   * This is defined as<p>
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
   * This is defined as<p>
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
   * @exception Exception if the class attribute of the instances is not
   * set
   */
  public void setPriors(Instances train) throws Exception {

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
   * Updates the class prior probabilities (when incrementally 
   * training)
   *
   * @param instance the new training instance seen
   * @exception Exception if the class of the instance is not
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
   */
  protected static String printClassifications(Classifier classifier, 
					     Instances train,
					     String testFileName,
					     int classIndex,
					     Range attributesToOutput) throws Exception {

    StringBuffer text = new StringBuffer();
    if (testFileName.length() != 0) {
      BufferedReader testReader = null;
      try {
	testReader = new BufferedReader(new FileReader(testFileName));
      } catch (Exception e) {
	throw new Exception("Can't open file " + e.getMessage() + '.');
      }
      Instances test = new Instances(testReader, 1);
      if (classIndex != -1) {
	test.setClassIndex(classIndex - 1);
      } else {
	test.setClassIndex(test.numAttributes() - 1);
      }
      int i = 0;
      while (test.readInstance(testReader)) {
	Instance instance = test.instance(0);    
	Instance withMissing = (Instance)instance.copy();
	withMissing.setDataset(test);
	double predValue = 
	  ((Classifier)classifier).classifyInstance(withMissing);
	if (test.classAttribute().isNumeric()) {
	  if (Instance.isMissingValue(predValue)) {
	    text.append(i + " missing ");
	  } else {
	    text.append(i + " " + predValue + " ");
	  }
	  if (instance.classIsMissing()) {
	    text.append("missing");
	  } else {
	    text.append(instance.classValue());
	  }
	  text.append(" " + attributeValuesString(withMissing, attributesToOutput) + "\n");
	} else {
	  if (Instance.isMissingValue(predValue)) {
	    text.append(i + " missing ");
	  } else {
	    text.append(i + " "
			+ test.classAttribute().value((int)predValue) + " ");
	  }
	  if (Instance.isMissingValue(predValue)) {
	    text.append("missing ");
	  } else {
	    text.append(classifier.distributionForInstance(withMissing)
			[(int)predValue] + " ");
	  }
	  text.append(instance.toString(instance.classIndex()) + " "
		      + attributeValuesString(withMissing, attributesToOutput) + "\n");
	}
	test.delete(0);
	i++;
      }
      testReader.close();
    }
    return text.toString();
  }

  /**
   * Builds a string listing the attribute values in a specified range of indices,
   * separated by commas and enclosed in brackets.
   *
   * @param instance the instance to print the values from
   * @param attributes the range of the attributes to list
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
    optionsText.append("\tSets test file. If missing, a cross-validation");
    optionsText.append(" will be performed on the training data.\n");
    optionsText.append("-c <class index>\n");
    optionsText.append("\tSets index of class attribute (default: last).\n");
    optionsText.append("-x <number of folds>\n");
    optionsText.append("\tSets number of folds for cross-validation (default: 10).\n");
    optionsText.append("-s <random number seed>\n");
    optionsText.append("\tSets random number seed for cross-validation (default: 1).\n");
    optionsText.append("-m <name of file with cost matrix>\n");
    optionsText.append("\tSets file with cost matrix.\n");
    optionsText.append("-l <name of input file>\n");
    optionsText.append("\tSets model input file.\n");
    optionsText.append("-d <name of output file>\n");
    optionsText.append("\tSets model output file.\n");
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
    optionsText.append("\tOnly outputs predictions for test instances, along with attributes "
		       + "(0 for none).\n");
    optionsText.append("-r\n");
    optionsText.append("\tOnly outputs cumulative margin distribution.\n");
    if (classifier instanceof Sourcable) {
      optionsText.append("-z <class name>\n");
      optionsText.append("\tOnly outputs the source representation"
			 + " of the classifier, giving it the supplied"
			 + " name.\n");
    }
    if (classifier instanceof Drawable) {
      optionsText.append("-g\n");
      optionsText.append("\tOnly outputs the graph representation"
			 + " of the classifier.\n");
    }

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
   * @param num integer to format
   * @return the formatted integer as a string
   */
  protected String num2ShortID(int num,char [] IDChars,int IDWidth) {
    
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
   * @exception Exception if the class of the instance is not
   * set
   */
  protected void updateStatsForClassifier(double [] predictedDistribution,
					Instance instance)
       throws Exception {

    int actualClass = (int)instance.classValue();
    double costFactor = 1;

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
            * m_CostMatrix.getMaxCost(actualClass);
        } else {
          m_TotalCost += instance.weight() 
            * m_CostMatrix.getElement(actualClass, predictedClass);
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
   * @exception Exception if the class of the instance is not
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
      double currentVal, deltaSum = 0;
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




