/*
 *    Evaluation.java
 *    Copyright (C) 1999 Eibe Frank,Len Trigg
 *
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
package weka.classifiers;

import java.util.*;
import java.io.*;
import weka.core.*;
import weka.estimators.*;

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
 * Outputs information-retrieval statistics for two-class problems. <p>
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
 * @version  $Revision: 1.21 $
  */
public class Evaluation implements Summarizable {

  /** The number of classes. */
  private int m_NumClasses;

  /** The number of folds for a cross-validation. */
  private int m_NumFolds;
 
  /** The weight of all incorrectly classified instances. */
  private double m_Incorrect;

  /** The weight of all correctly classified instances. */
  private double m_Correct;

  /** The weight of all unclassified instances. */
  private double m_Unclassified;

  /*** The weight of all instances that had no class assigned to them. */
  private double m_MissingClass;

  /** The weight of all instances that had a class assigned to them. */
  private double m_WithClass;

  /** Array for storing the confusion matrix. */
  private double [][] m_ConfusionMatrix;

  /** The names of the classes. */
  private String [] m_ClassNames;

  /** Is the class nominal or numeric? */
  private boolean m_ClassIsNominal;
  
  /** The prior probabilities of the classes */
  private double [] m_ClassPriors;

  /** The sum of counts for priors */
  private double m_ClassPriorsSum;

  /** The cost matrix (if given). */
  private CostMatrix m_CostMatrix;

  /** The weight of all correctly classified instances weighted by a cost. */
  private double m_CorrectWithCost;
  
  /** The weight of all incorrectly classified instances weighted by a cost. */
  private double m_IncorrectWithCost;
  
  /** The weight of all unclassified instances weighted by a cost. */
  private double m_UnclassifiedWithCost;

  /** The weight of all instances with a class weighted by a cost. */
  private double m_WithClassWithCost;

  /** Sum of errors. */
  private double m_SumErr;
  
  /** Sum of absolute errors. */
  private double m_SumAbsErr;

  /** Sum of squared errors. */
  private double m_SumSqrErr;

  /** Sum of class values. */
  private double m_SumClass;
  
  /** Sum of squared class values. */
  private double m_SumSqrClass;

  /*** Sum of predicted values. */
  private double m_SumPredicted;

  /** Sum of squared predicted values. */
  private double m_SumSqrPredicted;

  /** Sum of predicted * class values. */
  private double m_SumClassPredicted;

  /** Sum of absolute errors of the prior */
  private double m_SumPriorAbsErr;

  /** Sum of absolute errors of the prior */
  private double m_SumPriorSqrErr;

  /** Total Kononenko & Bratko Information */
  private double m_SumKBInfo;

  /*** Resolution of the margin histogram */
  private static int k_MarginResolution = 500;

  /** Cumulative margin distribution */
  private double m_MarginCounts [];

  /** Number of non-missing class training instances seen */
  private int m_NumTrainClassVals;

  /** Array containing all numeric training class values seen */
  private double [] m_TrainClassVals;

  /** Array containing all numeric training class weights */
  private double [] m_TrainClassWeights;

  /** Numeric class error estimator for prior */
  private Estimator m_PriorErrorEstimator;

  /** Numeric class error estimator for scheme */
  private Estimator m_ErrorEstimator;

  /**
   * The minimum probablility accepted from an estimator to avoid
   * taking log(0) in Sf calculations.
   */
  private static final double MIN_SF_PROB = Double.MIN_VALUE;

  /** Total entropy of prior predictions */
  private double m_SumPriorEntropy;
  
  /** Total entropy of scheme predictions */
  private double m_SumSchemeEntropy;
  
  /** Random instance for dataset shuffling and resampling */
  private Random m_Random = null;

  /**
   * Initializes all the counters for the evaluation.
   *
   * @param data set of training instances, to get some header 
   * information and prior class distribution information
   * @exception Exception if the class is not defined
   */
  public Evaluation(Instances data) throws Exception {
    
    this(data, null, null);
  }

  /**
   * Initializes all the counters for the evaluation and also takes a
   * cost matrix as parameter.
   *
   * @param data set of instances, to get some header information
   * @param costMatrix the cost matrix---if null, default costs will be used
   * @param random a random number generator for cost matrix-based
   * resampling---if set to null, no resampling is performed
   * @exception Exception if cost matrix is not compatible with 
   * data, the class is not defined or the class is numeric
   */
  public Evaluation(Instances data, CostMatrix costMatrix,
		    Random random) 
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
    m_Random = random;
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
   * @exception Exception if a classifier could not be generated 
   * successfully or the class is not defined
   */
  public void crossValidateModel(Classifier classifier,
				 Instances data, int numFolds) 
    throws Exception {
    
    // Make a copy of the data we can reorder
    data = new Instances(data);
    if (data.classAttribute().isNominal()) {
      data.stratify(numFolds);
    }
    // Do the folds
    for (int i = 0; i < numFolds; i++) {
      Instances train = data.trainCV(numFolds, i);
      if (m_CostMatrix != null) {
	train = m_CostMatrix.applyCostMatrix(train, m_Random);
      }
      setPriors(train);
      classifier.buildClassifier(train);
      Instances test = data.testCV(numFolds, i);
      evaluateModel(classifier, test);
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
   * accepted by the classifier will be removed from this array.
   * @exception Exception if a classifier could not be generated 
   * successfully or the class is not defined
   */
  public void crossValidateModel(String classifierString,
				 Instances data, int numFolds,
				 String[] options) 
       throws Exception {
    
    crossValidateModel(Classifier.forName(classifierString, options),
		       data, numFolds);
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
   * Outputs information-retrieval statistics for two-class problems. <p>
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
   * Outputs information-retrieval statistics for two-class problems. <p>
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
			      
    Instances train = null, tempTrain, test = null, 
      trainWithoutStrings = null, testWithoutStrings = null, template;
    Instance instanceWithoutStrings;
    CostMatrix costMatrix = null;
    int seed = 1, folds = 10, classIndex = -1;
    double predValue;
    double[] results;
    String trainFileName, testFileName, costFileName, classIndexString,
      seedString, foldsString, objectInputFileName, objectOutputFileName;
    boolean IRstatistics = false, noOutput = false,
      printClassifications = false, trainStatistics = true,
      printMargins = false, printComplexityStatistics = false,
      printGraph = false, ROCStatistics = false, printSource = false;
    Evaluation trainingEvaluation, testingEvaluation;
    StringBuffer text = new StringBuffer();
    BufferedReader trainReader = null, testReader = null;
    Reader costReader;
    ObjectInputStream objectInputStream = null;
    ObjectOutputStream objectOutputStream = null;
    Random random;
    StringBuffer schemeOptionsText = null;
    
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
	  throw new Exception("Not training file and no test "+
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
	  objectInputStream = 
	    new ObjectInputStream(new FileInputStream(objectInputFileName));
	}
	if (objectOutputFileName.length() != 0) {
	  objectOutputStream = 
	    new ObjectOutputStream(new FileOutputStream(objectOutputFileName));
	}
      } catch (Exception e) {
	throw new Exception("Can't open file " + e.getMessage() + '.');
      }
      if (trainFileName.length() != 0) {
	if ((classifier instanceof UpdateableClassifier) &&
	    (testFileName.length() != 0) &&
	    (costMatrix == null)) {
	  train = new Instances(trainReader, 1);
	} else {
	  train = new Instances(trainReader);
	}
	if (classIndex != -1) {
	  train.setClassIndex(classIndex - 1);
	} else {
	  train.setClassIndex(train.numAttributes() - 1);
	}
	if (classIndex > train.numAttributes()) {
	  throw new Exception("Index of class attribute too large.");
	}
	trainWithoutStrings = new Instances(train);
      }
      if (testFileName.length() != 0) {
	test = new Instances(testReader, 1);
	if (classIndex != -1) {
	  test.setClassIndex(classIndex - 1);
	} else {
	  test.setClassIndex(test.numAttributes() - 1);
	}
	if (classIndex > test.numAttributes()) {
	  throw new Exception("Index of class attribute too large.");
	}
	testWithoutStrings = new Instances(test);
      }
      if (trainFileName.length() != 0) {
	template = trainWithoutStrings;
      } else {
	template = testWithoutStrings;
      }
      seedString = Utils.getOption('s', options);
      if (seedString.length() != 0) {
	seed = Integer.parseInt(seedString);
      }
      foldsString = Utils.getOption('x', options);
      if (foldsString.length() != 0) {
	folds = Integer.parseInt(foldsString);
      }
      costFileName = Utils.getOption('m', options);
      if (costFileName.length() != 0) {

	try {
	  costReader = new FileReader(costFileName);
	} catch (Exception e) {
	  throw new Exception("Can't open file " + e.getMessage() + '.');
	}
	try {
	  // First try as a proper cost matrix format
	  costMatrix = new CostMatrix(costReader);
	} catch (Exception ex) {
	  try {
	    // Now try as the poxy old format :-)
	    System.err.println("Attempting to read old format");
	    try {
	      costReader.close(); // Close the old one
	      costReader = new FileReader(costFileName);
	    } catch (Exception e) {
	      throw new Exception("Can't open file " + e.getMessage() + '.');
	    }
	    costMatrix = new CostMatrix(template.numClasses());
	    System.err.println("Created default cost matrix");
	    costMatrix.readOldFormat(costReader);
	    System.err.println("Read old format");
	  } catch (Exception e2) {
	    // re-throw the original exception
	    System.err.println("Re-throwing original exception");
	    throw ex;
	  }
	}
      }
      IRstatistics = Utils.getFlag('i', options);
      noOutput = Utils.getFlag('o', options);
      trainStatistics = !Utils.getFlag('v', options);
      printComplexityStatistics = Utils.getFlag('k', options);
      printClassifications = Utils.getFlag('p', options);
      printMargins = Utils.getFlag('r', options);
      printGraph = Utils.getFlag('g', options);
      ROCStatistics = Utils.getFlag('y', options);
      printSource = Utils.getFlag('z', options);
      
      // If a model file is given, we can't process 
      // scheme-specific options
      if (objectInputFileName.length() != 0)
	Utils.checkForRemainingOptions(options);

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
      Utils.checkForRemainingOptions(options);
    } catch (Exception e) {
      throw new Exception("\nWeka exception: " + e.getMessage()
			   + makeOptionString(classifier));
    }

    // Setup up evaluation objects
    if (costMatrix == null) {
      trainingEvaluation = new Evaluation(new Instances(template, 0));
      testingEvaluation = new Evaluation(new Instances(template, 0));
    } else {
      if (classifier instanceof WeightedInstancesHandler) {
	random = null;
      } else {
	random = new Random(seed);
      }
      trainingEvaluation = new Evaluation(new Instances(template, 0),
					  costMatrix, random);
      testingEvaluation = new Evaluation(new Instances(template, 0), 
					 costMatrix, random);
    }
    
    if (objectInputFileName.length() != 0) {
      
      // Load classifier from file
      classifier = (Classifier) objectInputStream.readObject();
      objectInputStream.close();
    }
    
    // Build the classifier if no object file provided
    if ((classifier instanceof UpdateableClassifier) &&
	(testFileName.length() != 0) &&
	(costMatrix == null) &&
	(trainFileName.length() != 0)) {
      
      // Build classifier incrementally
      trainingEvaluation.setPriors(trainWithoutStrings);
      testingEvaluation.setPriors(trainWithoutStrings);
      if (objectInputFileName.length() == 0) {
	classifier.buildClassifier(trainWithoutStrings);
      }
      instanceWithoutStrings = 
	new Instance(trainWithoutStrings.numAttributes());
      instanceWithoutStrings.setDataset(trainWithoutStrings);
      while (train.readInstance(trainReader)) {
	
	// Copy instance from format with strings to format without
	// strings.
	int j = 0;
	for (int i = 0; i < train.numAttributes(); i++) {
	  if (!train.attribute(i).isString()) {
	    instanceWithoutStrings.
	      setValue(j, train.instance(0).value(i));
	    j++;
	  }
	}
	trainingEvaluation.updatePriors(instanceWithoutStrings);
	testingEvaluation.updatePriors(instanceWithoutStrings);
	((UpdateableClassifier)classifier).
	  updateClassifier(instanceWithoutStrings);
	train.delete(0);
      }
      trainReader.close();
    } else if (objectInputFileName.length() == 0) {
      
      // Build classifier in one go
      if (costMatrix != null) {
	tempTrain = costMatrix.applyCostMatrix(trainWithoutStrings, 
					       trainingEvaluation.m_Random);
      } else {
	tempTrain = new Instances(train);
      }
      trainingEvaluation.setPriors(tempTrain);
      testingEvaluation.setPriors(tempTrain);
      classifier.buildClassifier(tempTrain);
    } 

    // Save the classifier if an object output file is provided
    if (objectOutputFileName.length() != 0) {
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
      return wekaStaticWrapper((Sourcable) classifier);
    }

    // Output test instance predictions only
    if (printClassifications) {
      return printClassifications(classifier, new Instances(template, 0),
				  testFileName, classIndex);
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
	instanceWithoutStrings = 
	  new Instance(trainWithoutStrings.numAttributes());
	instanceWithoutStrings.setDataset(trainWithoutStrings);
	while (train.readInstance(trainReader)) {

	  // Copy instance from format with strings to format 
	  // without strings.
	  int j = 0;
	  for (int i = 0; i < train.numAttributes(); i++) {
	    if (!train.attribute(i).isString()) {
	      instanceWithoutStrings.
	      setValue(j, train.instance(0).value(i));
	      j++;
	    }
	  }
	  trainingEvaluation.
	  evaluateModelOnce((Classifier)classifier, 
			    instanceWithoutStrings);
	  train.delete(0);
	}
	trainReader.close();
      } else {
	trainingEvaluation.evaluateModel(classifier, 
					 trainWithoutStrings);
      }

      // Print the results of the training evaluation
      if (printMargins) {
	return trainingEvaluation.toCumulativeMarginDistributionString();
      } else {
	text.append(trainingEvaluation.
		    toSummaryString("\n=== Error on training" + 
				    " data ===\n", printComplexityStatistics));
	if (template.classAttribute().isNominal()) {
	  if (ROCStatistics) {
	    text.append("\n\n" + trainingEvaluation.toClassDetailsString());
	  }
	  text.append("\n\n" + trainingEvaluation.toMatrixString());
	  if (IRstatistics) {
	    text.append("\n\n" + trainingEvaluation.
			toInformationRetrievalStatisticsString() + 
			"\n");
	  }
	}
      }
    }

    // Compute proper error estimates
    if (testFileName.length() != 0) {

      // Testing is on the supplied test data
      instanceWithoutStrings = 
	  new Instance(testWithoutStrings.numAttributes());
      instanceWithoutStrings.setDataset(testWithoutStrings);
      while (test.readInstance(testReader)) {
	  
	// Copy instance from format with strings to format 
	// without strings.
	int j = 0;
	for (int i = 0; i < test.numAttributes(); i++) {
	  if (!test.attribute(i).isString()) {
	    instanceWithoutStrings.
		setValue(j, test.instance(0).value(i));
	    j++;
	  }
	}
	testingEvaluation.
	  evaluateModelOnce((Classifier)classifier, 
			    instanceWithoutStrings);
	test.delete(0);
      }
      testReader.close();
      text.append("\n\n" + testingEvaluation.
		  toSummaryString("=== Error on test data ===\n",
				  printComplexityStatistics));
    } else if (trainFileName.length() != 0) {

      // Testing is via cross-validation on training data
      random = new Random(seed);
      random.setSeed(seed);
      trainWithoutStrings.randomize(random);
      testingEvaluation.
      crossValidateModel(classifier, trainWithoutStrings, folds);
      if (template.classAttribute().isNumeric()) {
	text.append("\n\n" + testingEvaluation.
		    toSummaryString("=== Cross-validation ===\n",
				    printComplexityStatistics));
      } else {
	text.append("\n\n" + testingEvaluation.
		    toSummaryString("=== Stratified " + 
				    "cross-validation ===\n",
				    printComplexityStatistics));
      }
    }
    if (template.classAttribute().isNominal()) {
      if (ROCStatistics) {
	text.append("\n\n" + testingEvaluation.toClassDetailsString());
      }
      text.append("\n\n" + testingEvaluation.toMatrixString());
      if (IRstatistics) {
	text.append("\n\n" + testingEvaluation.
		    toInformationRetrievalStatisticsString() + "\n");
      }
    }
    return text.toString();
  }

  /**
   * Evaluates the classifier on a given set of instances.
   *
   * @param classifier machine learning classifier
   * @param data set of test instances for evaluation
   * @exception Exception if model could not be evaluated 
   * successfully
   */
  public void evaluateModel(Classifier classifier,
			    Instances data) throws Exception {
    
    double [] predicted;

    for (int j = 0; j < data.numAttributes(); j++) {
      if (data.attribute(j).isString()) {
	throw new Exception("Can't evaluate on data with " + 
			    "string attributes!");
      }
    }
    for (int i = 0; i < data.numInstances(); i++) {
      evaluateModelOnce((Classifier)classifier, 
			data.instance(i));
    }
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
  
    Instance classMissing = new Instance(instance);				  
    double pred=0;
    classMissing.setDataset(instance.dataset());
    classMissing.setClassMissing();
    for (int i = 0; i < classMissing.numAttributes(); i++)
      if (classMissing.attribute(i).isString())
	throw new Exception("Can't evaluate on data with string " + 
			    "attributes!");
    if (m_ClassIsNominal) {
      if (classifier instanceof DistributionClassifier) {
	double [] dist = ((DistributionClassifier)classifier).
				 distributionForInstance(classMissing);
	pred = Utils.maxIndex(dist);
	updateStatsForClassifier(dist,
				 instance);
      } else {
	pred = classifier.classifyInstance(classMissing);
	updateStatsForClassifier(makeDistribution(pred),
				 instance);
      }
    } else {
      pred = classifier.classifyInstance(classMissing);
      updateStatsForPredictor(pred,
			      instance);
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
   * @return the source for a static classifier that can be tested with
   * weka libraries.
   */
  protected static String wekaStaticWrapper(Sourcable classifier) 
    throws Exception {
    
    String className = "StaticClassifier";
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
   * Gets the number of instances incorrectly classified (that is, for
   * which an incorrect prediction was made), including misclassification costs.
   *
   * @return the number of incorrectly classified instances 
   */
  public final double incorrectWithCost() {

    return m_IncorrectWithCost;
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
   * Gets the percentage of instances incorrectly classified (that is, for
   * which an incorrect prediction was made), including misclassification costs.
   *
   * @return the percent of incorrectly classified instances 
   * (between 0 and 100)
   */
  public final double pctIncorrectWithCost() {

    return 100 * m_IncorrectWithCost / m_WithClassWithCost;
  }

  /**
   * Gets the average cost, that is, total cost of misclassifications
   * (incorrect plus unclassified) over the total number of instances.
   *
   * @return the average cost.  
   */
  public final double avgCost() {

    return (m_IncorrectWithCost + m_UnclassifiedWithCost) / m_WithClass;
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
   * Gets the number of instances correctly classified (that is, for
   * which a correct prediction was made), including misclassification costs.
   *
   * @return the number of correctly classified instances
   */
  public final double correctWithCost() {
    
    return m_CorrectWithCost;
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
   * Gets the percentage of instances correctly classified (that is, for
   * which a correct prediction was made), including misclassification costs.
   *
   * @return the percent of correctly classified instances (between 0 and 100)
   */
  public final double pctCorrectWithCost() {

    return 100.0 * m_CorrectWithCost / m_WithClassWithCost;
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
   * Gets the number of instances not classified (that is, for
   * which no prediction was made by the classifier), including the 
   * misclassification costs.
   *
   * @return the number of unclassified instances
   */
  public final double unclassifiedWithCost() {
    
    return m_UnclassifiedWithCost;
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
   * Gets the percentage of instances not classified (that is, for
   * which no prediction was made by the classifier), including
   * misclassification costs.
   *
   * @return the percent of unclassified instances (between 0 and 100)
   */
  public final double pctUnclassifiedWithCost() {
    
    return 100 * m_UnclassifiedWithCost / m_WithClassWithCost;
  }

  /**
   * Returns the estimated error rate or the root mean squared error
   * (if the class is numeric). If a cost matrix was given this
   * error rate involves weights from the cost matrix.
   *
   * @return the estimated error rate (between 0 and 1)
   */
  public final double errorRate() {

    if (!m_ClassIsNominal) {
      return Math.sqrt(m_SumSqrErr / m_WithClass);
    }
    if (m_CostMatrix == null) {
      return m_Incorrect / m_WithClass;
    } else {
      return m_IncorrectWithCost / m_WithClassWithCost;
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

    if (Utils.smOrEq(varActual * varPredicted, 0.0)) {
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
   * Calls toInformationRetrievalStatisticsString() with a 
   * default title.
   *
   * @exception Exception if the dataset is not a two-class dataset.
   */
  public String toInformationRetrievalStatisticsString() 
       throws Exception {

    if (m_NumClasses != 2) {
      throw 
      new Exception("Can only compute IR statistics for " + 
		      "2-class problems!");
    }
    return toInformationRetrievalStatisticsString("=== IR " + 
						  "statistics ===\n");
  }

  /**
   * Outputs information retrieval statistics (precision, recall,
   * f-measure) for two-class problems.
   *
   * @param title the title for the statistics
   * @return the summary as a String
   * @exception Exception if the dataset is not a two-class dataset.
   */
  public String toInformationRetrievalStatisticsString(String title) 
       throws Exception{

    double precision, recall, f_measure;
    StringBuffer text = new StringBuffer();
    int maj, min;
    
    if (m_NumClasses != 2) {
      throw new Exception("Can only compute IR statistics " + 
			  "for 2-class problems!");
    }
    if (Utils.gr(m_ConfusionMatrix[1][1] + m_ConfusionMatrix[1][0],
		 m_ConfusionMatrix[0][0] + m_ConfusionMatrix[0][1])) {
      maj = 1; min = 0;
    } else {
      maj = 0; min = 1;
    }
    precision = m_ConfusionMatrix[min][min] / 
      (m_ConfusionMatrix[min][min] + m_ConfusionMatrix[maj][min]);
    recall = m_ConfusionMatrix[min][min] /
      (m_ConfusionMatrix[min][min] + m_ConfusionMatrix[min][maj]);
    f_measure = 2 * precision * recall / (precision + recall);
    text.append("Precision " + Utils.doubleToString(precision,7,2) + "\n");
    text.append("Recall    " + Utils.doubleToString(recall,7,2) + "\n");
    text.append("F-measure " + Utils.doubleToString(f_measure,7,2));
    
    return text.toString();
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
	  if (m_CostMatrix != null) {
	    text.append("Correctly Classified With Cost     ");
	    text.append(Utils.doubleToString(correctWithCost(), 12 ,4) + 
			"     " +
			Utils.doubleToString(pctCorrectWithCost(), 12, 4) 
			+ " %\n");
	    text.append("Incorrectly Classified With Cost   ");
	    text.append(Utils.doubleToString(incorrectWithCost(), 12, 4) +
			"     " +
			Utils.doubleToString(pctIncorrectWithCost(), 12, 4) 
			+ " %\n");
	    text.append("Average Cost                       ");
	    text.append(Utils.doubleToString(avgCost(), 12, 4) + "\n");
	    if (Utils.gr(m_UnclassifiedWithCost, 0)) {
	      text.append("UnClassified With Cost             ");
	      text.append(Utils.doubleToString(unclassifiedWithCost(), 12, 4) +
			  "     " +
			  Utils.doubleToString(pctUnclassifiedWithCost(), 
                                               12, 4) 
			  + " %\n");
	    }
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
      if (m_CostMatrix != null) {
	text.append("Total Number With Cost             ");
	text.append(Utils.doubleToString(m_WithClassWithCost, 12, 4) + "\n");
      }    
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
    double maxval;
    char [] IDChars = {'a','b','c','d','e','f','g','h','i','j',
		       'k','l','m','n','o','p','q','r','s','t',
		       'u','v','w','x','y','z'};
    int IDWidth;
    int i, j;
    boolean fractional = false;

    if (!m_ClassIsNominal) {
      throw new Exception("Evaluation: No confusion matrix possible!");
    }

    // Find the maximum value in the matrix
    // and check for fractional display requirement 
    for(maxval = 0, i = 0; i < m_NumClasses; i++) {
      for(j = 0; j < m_NumClasses; j++) {
	double current = m_ConfusionMatrix[i][j];
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
    text.append(title + "\n");
    for(i = 0; i < m_NumClasses; i++) {
      if (fractional) {
	text.append(" " + num2ShortID(i,IDChars,IDWidth - 3) + "   ");
      } else {
	text.append(" " + num2ShortID(i,IDChars,IDWidth));
      }
    }
    text.append("   <-- classified as\n");
    for(i = 0; i< m_NumClasses; i++) { 
      for(j = 0; j < m_NumClasses; j++) {
	text.append(" " + 
		    Utils.doubleToString(m_ConfusionMatrix[i][j],
					 IDWidth,
					 (fractional ? 2 : 0)));
      }
      text.append(" | " + num2ShortID(i,IDChars,IDWidth) + " = " 
		  + m_ClassNames[i]);
      if (i != m_NumClasses - 1) {
	text.append("\n");
      }
    }
    return text.toString();
  }

  public String toClassDetailsString() throws Exception {

    return toClassDetailsString("=== Accuracy By Class ===\n");
  }

  /**
   * For the following confusion matrix<br>
   *  <pre>
   * A B C
   * 5 1 0  A
   * 2 7 1  B
   * 1 1 9  C
   * </pre>
   * Will print out a breakdown of the accuracy for each class, eg:<br>
   * <pre>
   *   TP    FP  Class
   * 0.85  0.14   A
   * 0.70  0.11   B
   * 0.82  0.06   C
   * </pre>
   * Should be useful for ROC curves.
   */
  public String toClassDetailsString(String title) throws Exception {

    if (!m_ClassIsNominal) {
      throw new Exception("Evaluation: No confusion matrix possible!");
    }
    StringBuffer text = new StringBuffer(title 
					 + "\nTruePos  FalsePos  Class\n");
    for(int i = 0; i < m_NumClasses; i++) {
      text.append(Utils.doubleToString(truePositives(i), 7, 3)).append("   ");
      text.append(Utils.doubleToString(falsePositives(i), 7, 3)).append("  ");
      text.append(m_ClassNames[i]).append('\n');
    }
    return text.toString();
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
  public double truePositives(int classIndex) {

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
  public double falsePositives(int classIndex) {

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
  public void updatePriors(Instance instance) throws Exception
  {
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

    if ((obj == null) || !(obj instanceof Evaluation)) return false;
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
  private static String printClassifications(Classifier classifier, 
					     Instances trainWithoutStrings,
					     String testFileName,
					     int classIndex) throws Exception {

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
      Instance instanceWithoutStrings = 
	new Instance(trainWithoutStrings.numAttributes());
      instanceWithoutStrings.setDataset(trainWithoutStrings);
      int i = 0;
      while (test.readInstance(testReader)) {
	    
	// Copy instance from format with strings to format 
	// without strings.
	int j = 0;
	for (int k = 0; k < test.numAttributes(); k++) {
	  if (!test.attribute(k).isString()) {
	    instanceWithoutStrings.
	      setValue(j, test.instance(0).value(k));
	    j++;
	  }
	}
	Instance withMissing = new Instance(instanceWithoutStrings);
	withMissing.setDataset(instanceWithoutStrings.dataset());
	double predValue = 
	  ((Classifier)classifier).classifyInstance(withMissing);
	if (test.classAttribute().isNumeric()) {
	  if (Instance.isMissingValue(predValue)) {
	    text.append(i + " missing ");
	  } else {
	    text.append(i + " " + predValue + " ");
	  }
	  if (instanceWithoutStrings.classIsMissing()) {
	    text.append("missing\n");
	  } else {
	    text.append(instanceWithoutStrings.classValue() + "\n");
	  }
	} else {
	  if (Instance.isMissingValue(predValue)) {
	    text.append(i + " missing ");
	  } else {
	    text.append(i + " "
	      	  + test.classAttribute().value((int)predValue) + " ");
	  }
	  if (classifier instanceof DistributionClassifier) {
	    if (Instance.isMissingValue(predValue)) {
	      text.append("missing ");
	    } else {
	      text.append(((DistributionClassifier)classifier).
	      	    distributionForInstance(withMissing)
	      	    [(int)predValue]+" ");
	    }
	  }
	  text.append(instanceWithoutStrings.toString(instanceWithoutStrings.
						      classIndex()) + "\n");
	}
	test.delete(0);
	i++;
      }
      testReader.close();
    }
    return text.toString();
  }

  /**
   * Make up the help string giving all the command line options
   *
   * @param classifier the classifier to include options for
   * @return a string detailing the valid command line options
   */
  private static String makeOptionString(Classifier classifier) {

    StringBuffer optionsText = new StringBuffer("");

    // General options
    optionsText.append("\n\nGeneral options:\n\n");
    optionsText.append("-t <name of training file>\n");
    optionsText.append("\tSets training file.\n");
    optionsText.append("-T <name of test file>\n");
    optionsText.append("\tSets test file. If missing, a cross-validation\n");
    optionsText.append("\twill be performed on the training data.\n");
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
    optionsText.append("\tOutputs information retrieval statistics for\n");
    optionsText.append("\ttwo-class problems.\n");
    optionsText.append("-k\n");
    optionsText.append("\tOutputs information-theoretic statistics.\n");
    optionsText.append("-p\n");
    optionsText.append("\tOnly outputs predictions for test instances.\n");
    optionsText.append("-r\n");
    optionsText.append("\tOnly outputs cumulative margin distribution.\n");
    optionsText.append("-y\n");
    optionsText.append("\tOutputs detailed class breakdown.\n");
    optionsText.append("\tyy? y not?\n");
    if (classifier instanceof Sourcable) {
      optionsText.append("-z\n");
      optionsText.append("\tOnly outputs the source representation"
			 + " of the classifier.\n");
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
      Enumeration enum = ((OptionHandler)classifier).listOptions();
      while (enum.hasMoreElements()) {
	Option option = (Option) enum.nextElement();
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
  private String num2ShortID(int num,char [] IDChars,int IDWidth) {
    
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
  private double [] makeDistribution(double predictedClass) {

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
  private void updateStatsForClassifier(double [] predictedDistribution,
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

      // Determine misclassification cost
      if (m_CostMatrix != null) {
	// If no prediction was made, this is taken as being maximally wrong.
	// If the prediction was correct, assume this is maximally right.
	if ((predictedClass < 0) || (predictedClass == actualClass)) {
	  costFactor = m_CostMatrix.getMaxCost(actualClass); 
	} else {
	  costFactor = m_CostMatrix.getElement(actualClass, predictedClass);
	}
      }

      m_WithClass += instance.weight();
      m_WithClassWithCost += costFactor * instance.weight();

      // Update counts when no class was predicted
      if (predictedClass < 0) {
	m_Unclassified += instance.weight();
	m_UnclassifiedWithCost += costFactor * instance.weight();
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
      m_ConfusionMatrix[actualClass][predictedClass] += 
	instance.weight() * costFactor;
      if (predictedClass != actualClass) {
	m_Incorrect += instance.weight();
	m_IncorrectWithCost += costFactor * instance.weight();
      } else {
	m_Correct += instance.weight();
	m_CorrectWithCost += costFactor * instance.weight();
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
  private void updateStatsForPredictor(double predictedValue,
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
      m_SumPredicted += predictedValue;
      m_SumSqrPredicted += predictedValue * predictedValue;

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
  private void updateMargins(double [] predictedDistribution, 
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
  private void updateNumericScores(double [] predicted, 
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
  private void addNumericTrainClass(double classValue, double weight) {

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
  private void setNumericPriorsFromBuffer() {
    
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
    m_ClassPriors[0] = m_ClassPriorsSum = 0.0001; // zf correction
    for (int i = 0; i < m_NumTrainClassVals; i++) {
      m_ClassPriors[0] += m_TrainClassVals[i] * m_TrainClassWeights[i];
      m_ClassPriorsSum += m_TrainClassWeights[i];
      m_PriorErrorEstimator.addValue(m_TrainClassVals[i],
				     m_TrainClassWeights[i]);
    }
  }

}




