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
 *    CheckClassifier.java
 *    Copyright (C) 1999 Len Trigg
 *
 */

package weka.classifiers;

import java.io.*;
import java.util.*;
import weka.core.*;

/**
 * Class for examining the capabilities and finding problems with 
 * classifiers. If you implement a classifier using the WEKA.libraries,
 * you should run the checks on it to ensure robustness and correct
 * operation. Passing all the tests of this object does not mean
 * bugs in the classifier don't exist, but this will help find some
 * common ones. <p>
 * 
 * Typical usage: <p>
 * <code>java weka.classifiers.CheckClassifier -W classifier_name 
 * classifier_options </code><p>
 * 
 * CheckClassifier reports on the following:
 * <ul>
 *    <li> Classifier abilities <ul>
 *         <li> Possible command line options to the classifier
 *         <li> Whether the classifier is a distributionClassifier
 *         <li> Whether the classifier can predict nominal and/or predict 
 *              numeric class attributes. Warnings will be displayed if 
 *              performance is worse than ZeroR
 *         <li> Whether the classifier can be trained incrementally
 *         <li> Whether the classifier can handle numeric predictor attributes
 *         <li> Whether the classifier can handle nominal predictor attributes
 *         <li> Whether the classifier can handle string predictor attributes
 *         <li> Whether the classifier can handle missing predictor values
 *         <li> Whether the classifier can handle missing class values
 *         <li> Whether a nominal classifier only handles 2 class problems
 *         <li> Whether the classifier can handle instance weights
 *         </ul>
 *    <li> Correct functioning <ul>
 *         <li> Correct initialisation during buildClassifier (i.e. no result
 *              changes when buildClassifier called repeatedly)
 *         <li> Whether incremental training produces the same results
 *              as during non-incremental training (which may or may not 
 *              be OK)
 *         <li> Whether the classifier alters the data pased to it 
 *              (number of instances, instance order, instance weights, etc)
 *         </ul>
 *    <li> Degenerate cases <ul>
 *         <li> building classifier with zero training instances
 *         <li> all but one predictor attribute values missing
 *         <li> all predictor attribute values missing
 *         <li> all but one class values missing
 *         <li> all class values missing
 *         </ul>
 *    </ul>
 * Running CheckClassifier with the debug option set will output the 
 * training and test datasets for any failed tests.<p>
 *
 * Valid options are:<p>
 *
 * -D <br>
 * Turn on debugging output.<p>
 *
 * -W classname <br>
 * Specify the full class name of a classifier to perform the 
 * tests on (required).<p>
 *
 * Options after -- are passed to the designated classifier.<p>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.13 $
 */
public class CheckClassifier implements OptionHandler {

  /*** The classifier to be examined */
  protected Classifier m_Classifier = new weka.classifiers.ZeroR();

  /** The options to be passed to the base classifier. */
  protected String [] m_ClassifierOptions;

  /** The results of the analysis as a string */
  protected String m_AnalysisResults;

  /** Debugging mode, gives extra output if true */
  protected boolean m_Debug;

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(2);

    newVector.addElement(new Option(
	      "\tTurn on debugging output.",
	      "D", 0, "-D"));
    newVector.addElement(new Option(
	      "\tFull name of the classifier analysed.\n"
	      +"\teg: weka.classifiers.NaiveBayes",
	      "W", 1, "-W"));

    if ((m_Classifier != null) 
	&& (m_Classifier instanceof OptionHandler)) {
      newVector.addElement(new Option("", "", 0, 
				      "\nOptions specific to classifier "
				      + m_Classifier.getClass().getName()
				      + ":"));
      Enumeration enum = ((OptionHandler)m_Classifier).listOptions();
      while (enum.hasMoreElements())
	newVector.addElement(enum.nextElement());
    }
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -D <br>
   * Turn on debugging output.<p>
   *
   * -W classname <br>
   * Specify the full class name of a classifier to perform the 
   * tests on (required).<p>
   *
   * Options after -- are passed to the designated classifier 
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    setDebug(Utils.getFlag('D', options));
    
    String classifierName = Utils.getOption('W', options);
    if (classifierName.length() == 0) {
      throw new Exception("A classifier must be specified with"
			  + " the -W option.");
    }
    setClassifier(Classifier.forName(classifierName,
				     Utils.partitionOptions(options)));
  }

  /**
   * Gets the current settings of the CheckClassifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] classifierOptions = new String [0];
    if ((m_Classifier != null) && 
	(m_Classifier instanceof OptionHandler)) {
      classifierOptions = ((OptionHandler)m_Classifier).getOptions();
    }
    String [] options = new String [classifierOptions.length + 4];
    int current = 0;
    if (getDebug()) {
      options[current++] = "-D";
    }
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
   * Begin the tests, reporting results to System.out
   */
  public void doTests() {

    if (getClassifier() == null) {
      System.out.println("\n=== No classifier set ===");
      return;
    }
    System.out.println("\n=== Check on Classifier: "
		       + getClassifier().getClass().getName()
		       + " ===\n");

    // Start tests
    canTakeOptions();
    boolean updateableClassifier = updateableClassifier();
    boolean distributionClassifier = distributionClassifier();
    boolean weightedInstancesHandler = weightedInstancesHandler();
    testsPerClassType(false, updateableClassifier, weightedInstancesHandler);
    testsPerClassType(true, updateableClassifier, weightedInstancesHandler);

  }


  /**
   * Set debugging mode
   *
   * @param debug true if debug output should be printed
   */
  public void setDebug(boolean debug) {

    m_Debug = debug;
  }

  /**
   * Get whether debugging is turned on
   *
   * @return true if debugging output is on
   */
  public boolean getDebug() {

    return m_Debug;
  }

  /**
   * Set the classifier for boosting. 
   *
   * @param newClassifier the Classifier to use.
   */
  public void setClassifier(Classifier newClassifier) {

    m_Classifier = newClassifier;
  }

  /**
   * Get the classifier used as the classifier
   *
   * @return the classifier used as the classifier
   */
  public Classifier getClassifier() {

    return m_Classifier;
  }


  /**
   * Test method for this class
   */
  public static void main(String [] args) {

    try {
      CheckClassifier check = new CheckClassifier();

      try {
	check.setOptions(args);
	Utils.checkForRemainingOptions(args);
      } catch (Exception ex) {
	String result = ex.getMessage() + "\nCheckClassifier Options:\n\n";
	Enumeration enum = check.listOptions();
	while (enum.hasMoreElements()) {
	  Option option = (Option) enum.nextElement();
	  result += option.synopsis() + "\n" + option.description() + "\n";
	}
	throw new Exception(result);
      }

      check.doTests();
    } catch (Exception ex) {
      System.err.println(ex.getMessage());
    }
  }

  /**
   * Run a battery of tests for a given class attribute type
   *
   * @param numericClass true if the class attribute should be numeric
   * @param updateable true if the classifier is updateable
   * @param weighted true if the classifier says it handles weights
   */
  protected void testsPerClassType(boolean numericClass, boolean updateable,
				   boolean weighted) {

    boolean PNom = canPredict(true, false, numericClass);
    boolean PNum = canPredict(false, true, numericClass);
    if (PNom || PNum) {
      if (weighted) {
	instanceWeights(PNom, PNum, numericClass);
      }
      if (!numericClass) {
	canHandleNClasses(PNom, PNum, 4);
      }
      canHandleZeroTraining(PNom, PNum, numericClass);
      boolean handleMissingPredictors = canHandleMissing(PNom, PNum, 
							 numericClass, 
							 true, false, 20);
      if (handleMissingPredictors) {
	canHandleMissing(PNom, PNum, numericClass, true, false, 100);
      }
      boolean handleMissingClass = canHandleMissing(PNom, PNum, numericClass, 
						    false, true, 20);
      if (handleMissingClass) {
	canHandleMissing(PNom, PNum, numericClass, false, true, 100);
      }
      correctBuildInitialisation(PNom, PNum, numericClass);
      datasetIntegrity(PNom, PNum, numericClass,
		       handleMissingPredictors, handleMissingClass);
      doesntUseTestClassVal(PNom, PNum, numericClass);
      if (updateable) {
	updatingEquality(PNom, PNum, numericClass);
      }
    }
    /*
     * Robustness / Correctness:
     *    Whether the classifier can handle string predictor attributes
     */
  }

  /**
   * Checks whether the scheme can take command line options.
   *
   * @return true if the classifier can take options
   */
  protected boolean canTakeOptions() {

    System.out.print("options...");
    if (m_Classifier instanceof OptionHandler) {
      System.out.println("yes");
      if (m_Debug) {
	System.out.println("\n=== Full report ===");
	Enumeration enum = ((OptionHandler)m_Classifier).listOptions();
	while (enum.hasMoreElements()) {
	  Option option = (Option) enum.nextElement();
	  System.out.print(option.synopsis() + "\n" 
			   + option.description() + "\n");
	}
	System.out.println("\n");
      }
      return true;
    }
    System.out.println("no");
    return false;
  }
  
  /**
   * Checks whether the scheme is a distribution classifier.
   *
   * @return true if the classifier produces distributions
   */
  protected boolean distributionClassifier() {

    System.out.print("distribution classifier...");
    if (m_Classifier instanceof DistributionClassifier) {
      System.out.println("yes");
      return true;
    }
    System.out.println("no");
    return false;
  }

  /**
   * Checks whether the scheme can build models incrementally.
   *
   * @return true if the classifier can train incrementally
   */
  protected boolean updateableClassifier() {

    System.out.print("updateable classifier...");
    if (m_Classifier instanceof UpdateableClassifier) {
      System.out.println("yes");
      return true;
    }
    System.out.println("no");
    return false;
  }

  /**
   * Checks whether the scheme says it can handle instance weights.
   *
   * @return true if the classifier handles instance weights
   */
  protected boolean weightedInstancesHandler() {

    System.out.print("weighted instances classifier...");
    if (m_Classifier instanceof WeightedInstancesHandler) {
      System.out.println("yes");
      return true;
    }
    System.out.println("no");
    return false;
  }

  /**
   * Checks basic prediction of the scheme, for simple non-troublesome
   * datasets.
   *
   * @param nominalPredictor if true use nominal predictor attributes
   * @param numericPredictor if true use numeric predictor attributes
   * @param numericClass if true use a numeric class attribute otherwise a
   * nominal class attribute
   * @return true if the test was passed
   */
  protected boolean canPredict(boolean nominalPredictor,
			       boolean numericPredictor, 
			       boolean numericClass) {

    System.out.print("basic predict");
    printAttributeSummary(nominalPredictor, numericPredictor, numericClass);
    System.out.print("...");
    FastVector accepts = new FastVector();
    accepts.addElement("nominal");
    accepts.addElement("numeric");
    int numTrain = 20, numTest = 20, numClasses = 2, missingLevel = 0;
    boolean predictorMissing = false, classMissing = false;

    return runBasicTest(nominalPredictor, numericPredictor, numericClass, 
			missingLevel, predictorMissing, classMissing,
			numTrain, numTest, numClasses, 
			accepts);
  }

  /**
   * Checks whether nominal schemes can handle more than two classes.
   * If a scheme is only designed for two-class problems it should
   * throw an appropriate exception for multi-class problems.
   *
   * @param nominalPredictor if true use nominal predictor attributes
   * @param numericPredictor if true use numeric predictor attributes
   * @param numClasses the number of classes to test
   * @return true if the test was passed
   */
  protected boolean canHandleNClasses(boolean nominalPredictor,
				      boolean numericPredictor, 
				      int numClasses) {

    System.out.print("more than two class problems");
    printAttributeSummary(nominalPredictor, numericPredictor, false);
    System.out.print("...");
    FastVector accepts = new FastVector();
    accepts.addElement("number");
    accepts.addElement("class");
    int numTrain = 20, numTest = 20, missingLevel = 0;
    boolean predictorMissing = false, classMissing = false;

    return runBasicTest(nominalPredictor, numericPredictor, false, 
			missingLevel, predictorMissing, classMissing,
			numTrain, numTest, numClasses, 
			accepts);
  }

  /**
   * Checks whether the scheme can handle zero training instances.
   *
   * @param nominalPredictor if true use nominal predictor attributes
   * @param numericPredictor if true use numeric predictor attributes
   * @param numericClass if true use a numeric class attribute otherwise a
   * nominal class attribute
   * @return true if the test was passed
   */
  protected boolean canHandleZeroTraining(boolean nominalPredictor,
					  boolean numericPredictor, 
					  boolean numericClass) {

    System.out.print("handle zero training instances");
    printAttributeSummary(nominalPredictor, numericPredictor, numericClass);
    System.out.print("...");
    FastVector accepts = new FastVector();
    accepts.addElement("train");
    accepts.addElement("value");
    int numTrain = 0, numTest = 20, numClasses = 2, missingLevel = 0;
    boolean predictorMissing = false, classMissing = false;

    return runBasicTest(nominalPredictor, numericPredictor, numericClass, 
			missingLevel, predictorMissing, classMissing,
			numTrain, numTest, numClasses, 
			accepts);
  }


  /**
   * Checks whether the scheme correctly initialises models when 
   * buildClassifier is called. This test calls buildClassifier with
   * one training dataset and records performance on a test set. 
   * buildClassifier is then called on a training set with different
   * structure, and then again with the original training set. The
   * performance on the test set is compared with the original results
   * and any performance difference noted as incorrect build initialisation.
   *
   * @param nominalPredictor if true use nominal predictor attributes
   * @param numericPredictor if true use numeric predictor attributes
   * @param numericClass if true use a numeric class attribute otherwise a
   * nominal class attribute
   * @return true if the test was passed
   */
  protected boolean correctBuildInitialisation(boolean nominalPredictor,
					       boolean numericPredictor, 
					       boolean numericClass) {

    System.out.print("correct initialisation during buildClassifier");
    printAttributeSummary(nominalPredictor, numericPredictor, numericClass);
    System.out.print("...");
    int numTrain = 20, numTest = 20, numClasses = 2, missingLevel = 0;
    boolean predictorMissing = false, classMissing = false;

    Instances train1 = null;
    Instances test1 = null;
    Instances train2 = null;
    Instances test2 = null;
    Classifier classifier = null;
    Evaluation evaluation1A = null;
    Evaluation evaluation1B = null;
    Evaluation evaluation2 = null;
    boolean built = false;
    int stage = 0;
    try {

      // Make two sets of train/test splits with different 
      // numbers of attributes
      train1 = makeTestDataset(42, numTrain, 
			       nominalPredictor ? 2 : 0,
			       numericPredictor ? 1 : 0, 
			       numClasses, 
			       numericClass);
      train2 = makeTestDataset(84, numTrain, 
			       nominalPredictor ? 3 : 0,
			       numericPredictor ? 2 : 0, 
			       numClasses, 
			       numericClass);
      test1 = makeTestDataset(24, numTest,
			      nominalPredictor ? 2 : 0,
			      numericPredictor ? 1 : 0, 
			      numClasses, 
			      numericClass);
      test2 = makeTestDataset(48, numTest,
			      nominalPredictor ? 3 : 0,
			      numericPredictor ? 2 : 0, 
			      numClasses, 
			      numericClass);
      if (nominalPredictor) {
	train1.deleteAttributeAt(0);
	test1.deleteAttributeAt(0);
	train2.deleteAttributeAt(0);
	test2.deleteAttributeAt(0);
      }
      if (missingLevel > 0) {
	addMissing(train1, missingLevel, predictorMissing, classMissing);
	addMissing(test1, Math.min(missingLevel,50), predictorMissing, 
		   classMissing);
	addMissing(train2, missingLevel, predictorMissing, classMissing);
	addMissing(test2, Math.min(missingLevel,50), predictorMissing, 
		   classMissing);
      }

      classifier = Classifier.makeCopies(getClassifier(), 1)[0];
      evaluation1A = new Evaluation(train1);
      evaluation1B = new Evaluation(train1);
      evaluation2 = new Evaluation(train2);
    } catch (Exception ex) {
      throw new Error("Error setting up for tests: " + ex.getMessage());
    }
    try {
      stage = 0;
      classifier.buildClassifier(train1);
      built = true;
      if (!testWRTZeroR(classifier, evaluation1A, train1, test1)) {
	throw new Exception("Scheme performs worse than ZeroR");
      }

      stage = 1;
      built = false;
      classifier.buildClassifier(train2);
      built = true;
      if (!testWRTZeroR(classifier, evaluation2, train2, test2)) {
	throw new Exception("Scheme performs worse than ZeroR");
      }

      stage = 2;
      built = false;
      classifier.buildClassifier(train1);
      built = true;
      if (!testWRTZeroR(classifier, evaluation1B, train1, test1)) {
	throw new Exception("Scheme performs worse than ZeroR");
      }

      stage = 3;
      if (!evaluation1A.equals(evaluation1B)) {
	if (m_Debug) {
	  System.out.println("\n=== Full report ===\n"
		+ evaluation1A.toSummaryString("\nFirst buildClassifier()",
					       true)
		+ "\n\n");
	  System.out.println(
                evaluation1B.toSummaryString("\nSecond buildClassifier()",
					     true)
		+ "\n\n");
	}
	throw new Exception("Results differ between buildClassifier calls");
      }
      System.out.println("yes");

      if (false && m_Debug) {
	System.out.println("\n=== Full report ===\n"
                + evaluation1A.toSummaryString("\nFirst buildClassifier()",
					       true)
		+ "\n\n");
	System.out.println(
                evaluation1B.toSummaryString("\nSecond buildClassifier()",
					     true)
		+ "\n\n");
      }
      return true;
    } catch (Exception ex) {
      String msg = ex.getMessage().toLowerCase();
      if (msg.indexOf("worse than zeror") >= 0) {
	System.out.println("warning: performs worse than ZeroR");
      } else {
	System.out.println("no");
      }
      if (m_Debug) {
	System.out.println("\n=== Full Report ===");
	System.out.print("Problem during");
	if (built) {
	  System.out.print(" testing");
	} else {
	  System.out.print(" training");
	}
	switch (stage) {
	case 0:
	  System.out.print(" of dataset 1");
	  break;
	case 1:
	  System.out.print(" of dataset 2");
	  break;
	case 2:
	  System.out.print(" of dataset 1 (2nd build)");
	  break;
	case 3:
	  System.out.print(", comparing results from builds of dataset 1");
	  break;	  
	}
	System.out.println(": " + ex.getMessage() + "\n");
	System.out.println("here are the datasets:\n");
	System.out.println("=== Train1 Dataset ===\n"
			   + train1.toString() + "\n");
	System.out.println("=== Test1 Dataset ===\n"
			   + test1.toString() + "\n\n");
	System.out.println("=== Train2 Dataset ===\n"
			   + train2.toString() + "\n");
	System.out.println("=== Test2 Dataset ===\n"
			   + test2.toString() + "\n\n");
      }
    }
    return false;
  }


  /**
   * Checks basic missing value handling of the scheme. If the missing
   * values cause an exception to be thrown by the scheme, this will be
   * recorded.
   *
   * @param nominalPredictor if true use nominal predictor attributes
   * @param numericPredictor if true use numeric predictor attributes
   * @param numericClass if true use a numeric class attribute otherwise a
   * nominal class attribute
   * @param predictorMissing true if the missing values may be in 
   * the predictors
   * @param classMissing true if the missing values may be in the class
   * @param level the percentage of missing values
   * @return true if the test was passed
   */
  protected boolean canHandleMissing(boolean nominalPredictor,
				     boolean numericPredictor, 
				     boolean numericClass,
				     boolean predictorMissing,
				     boolean classMissing,
				     int missingLevel) {

    if (missingLevel == 100) {
      System.out.print("100% ");
    }
    System.out.print("missing");
    if (predictorMissing) {
      System.out.print(" predictor");
      if (classMissing) {
	System.out.print(" and");
      }
    }
    if (classMissing) {
      System.out.print(" class");
    }
    System.out.print(" values");
    printAttributeSummary(nominalPredictor, numericPredictor, numericClass);
    System.out.print("...");
    FastVector accepts = new FastVector();
    accepts.addElement("missing");
    accepts.addElement("value");
    accepts.addElement("train");
    int numTrain = 20, numTest = 20, numClasses = 2;

    return runBasicTest(nominalPredictor, numericPredictor, numericClass, 
			missingLevel, predictorMissing, classMissing,
			numTrain, numTest, numClasses, 
			accepts);
  }


  /**
   * Checks whether an updateable scheme produces the same model when
   * trained incrementally as when batch trained. The model itself
   * cannot be compared, so we compare the evaluation on test data
   * for both models. It is possible to get a false positive on this
   * test (likelihood depends on the classifier).
   *
   * @param nominalPredictor if true use nominal predictor attributes
   * @param numericPredictor if true use numeric predictor attributes
   * @param numericClass if true use a numeric class attribute otherwise a
   * nominal class attribute
   * @return true if the test was passed
   */
  protected boolean updatingEquality(boolean nominalPredictor,
				     boolean numericPredictor, 
				     boolean numericClass) {

    System.out.print("incremental training produces the same results"
		     + " as batch training");
    printAttributeSummary(nominalPredictor, numericPredictor, numericClass);
    System.out.print("...");
    int numTrain = 20, numTest = 20, numClasses = 2, missingLevel = 0;
    boolean predictorMissing = false, classMissing = false;

    Instances train = null;
    Instances test = null;
    Classifier [] classifiers = null;
    Evaluation evaluationB = null;
    Evaluation evaluationI = null;
    boolean built = false;
    try {
      train = makeTestDataset(42, numTrain, 
			      nominalPredictor ? 2 : 0,
			      numericPredictor ? 1 : 0, 
			      numClasses, 
			      numericClass);
      test = makeTestDataset(24, numTest,
			     nominalPredictor ? 2 : 0,
			     numericPredictor ? 1 : 0, 
			     numClasses, 
			     numericClass);
      if (nominalPredictor) {
	train.deleteAttributeAt(0);
	test.deleteAttributeAt(0);
      }
      if (missingLevel > 0) {
	addMissing(train, missingLevel, predictorMissing, classMissing);
	addMissing(test, Math.min(missingLevel, 50), predictorMissing, 
		   classMissing);
      }
      classifiers = Classifier.makeCopies(getClassifier(), 2);
      evaluationB = new Evaluation(train);
      evaluationI = new Evaluation(train);
      classifiers[0].buildClassifier(train);
      testWRTZeroR(classifiers[0], evaluationB, train, test);
    } catch (Exception ex) {
      throw new Error("Error setting up for tests: " + ex.getMessage());
    }
    try {
      classifiers[1].buildClassifier(new Instances(train, 0));
      for (int i = 0; i < train.numInstances(); i++) {
	((UpdateableClassifier)classifiers[1]).updateClassifier(
             train.instance(i));
      }
      built = true;
      testWRTZeroR(classifiers[1], evaluationI, train, test);
      if (!evaluationB.equals(evaluationI)) {
	System.out.println("no");
	if (m_Debug) {
	  System.out.println("\n=== Full Report ===");
	  System.out.println("Results differ between batch and "
			     + "incrementally built models.\n"
			     + "Depending on the classifier, this may be OK");
	  System.out.println("Here are the results:\n");
	  System.out.println(evaluationB.toSummaryString(
			     "\nbatch built results\n", true));
	  System.out.println(evaluationI.toSummaryString(
                             "\nincrementally built results\n", true));
	  System.out.println("Here are the datasets:\n");
	  System.out.println("=== Train Dataset ===\n"
			     + train.toString() + "\n");
	  System.out.println("=== Test Dataset ===\n"
			     + test.toString() + "\n\n");
	}
	return false;
      }
      System.out.println("yes");
      return true;
    } catch (Exception ex) {
      System.out.print("Problem during");
      if (built) {
	System.out.print(" testing");
      } else {
	System.out.print(" training");
      }
      System.out.println(": " + ex.getMessage() + "\n");
    }
    return false;
  }

  /**
   * Checks whether the classifier erroneously uses the class
   * value of test instances (if provided). Runs the classifier with
   * test instance class values set to missing and compares with results
   * when test instance class values are left intact.
   *
   * @param nominalPredictor if true use nominal predictor attributes
   * @param numericPredictor if true use numeric predictor attributes
   * @param numericClass if true use a numeric class attribute otherwise a
   * nominal class attribute
   * @return true if the test was passed
   */
  protected boolean doesntUseTestClassVal(boolean nominalPredictor,
					  boolean numericPredictor, 
					  boolean numericClass) {

    System.out.print("classifier ignores test instance class vals");
    printAttributeSummary(nominalPredictor, numericPredictor, numericClass);
    System.out.print("...");
    int numTrain = 40, numTest = 20, numClasses = 2, missingLevel = 0;
    boolean predictorMissing = false, classMissing = false;

    Instances train = null;
    Instances test = null;
    Classifier [] classifiers = null;
    Evaluation evaluationB = null;
    Evaluation evaluationI = null;
    boolean evalFail = false;
    try {
      train = makeTestDataset(43, numTrain, 
			      nominalPredictor ? 3 : 0,
			      numericPredictor ? 2 : 0, 
			      numClasses, 
			      numericClass);
      test = makeTestDataset(24, numTest,
			     nominalPredictor ? 3 : 0,
			     numericPredictor ? 2 : 0, 
			     numClasses, 
			     numericClass);
      if (nominalPredictor) {
	train.deleteAttributeAt(0);
	test.deleteAttributeAt(0);
      }
      if (missingLevel > 0) {
	addMissing(train, missingLevel, predictorMissing, classMissing);
	addMissing(test, Math.min(missingLevel, 50), predictorMissing, 
		   classMissing);
      }
      classifiers = Classifier.makeCopies(getClassifier(), 2);
      evaluationB = new Evaluation(train);
      evaluationI = new Evaluation(train);
      classifiers[0].buildClassifier(train);
      classifiers[1].buildClassifier(train);
    } catch (Exception ex) {
      throw new Error("Error setting up for tests: " + ex.getMessage());
    }
    try {

      // Now set test values to missing when predicting
      for (int i = 0; i < test.numInstances(); i++) {
	Instance testInst = test.instance(i);
	Instance classMissingInst = (Instance)testInst.copy();
        classMissingInst.setDataset(test);
	classMissingInst.setClassMissing();
	if (classifiers[0] instanceof DistributionClassifier) {
	  double [] dist0 = ((DistributionClassifier)classifiers[0]).
	    distributionForInstance(testInst);
	  double [] dist1 = ((DistributionClassifier)classifiers[1]).
	    distributionForInstance(classMissingInst);
	  for (int j = 0; j < dist0.length; j++) {
	    if (dist0[j] != dist1[j]) {
	      throw new Exception("Prediction different for instance " 
				  + (i + 1));
	    }
	  }
	} else {
	  double pred0 = classifiers[0].classifyInstance(testInst);
	  double pred1 = classifiers[1].classifyInstance(classMissingInst);
	  if (pred0 != pred1) {
	    throw new Exception("Prediction different for instance " 
				+ (i + 1));
	  }
	}
      }
      System.out.println("yes");
      return true;
    } catch (Exception ex) {
      System.out.println("no");
      if (m_Debug) {
	System.out.println("\n=== Full Report ===");
	
	if (evalFail) {
	  System.out.println("Results differ between non-missing and "
			     + "missing test class values.");
	} else {
	  System.out.print("Problem during testing");
	  System.out.println(": " + ex.getMessage() + "\n");
	}
	System.out.println("Here are the datasets:\n");
	System.out.println("=== Train Dataset ===\n"
			   + train.toString() + "\n");
	System.out.println("=== Train Weights ===\n");
	for (int i = 0; i < train.numInstances(); i++) {
	  System.out.println(" " + (i + 1) 
			     + "    " + train.instance(i).weight());
	}
	System.out.println("=== Test Dataset ===\n"
			   + test.toString() + "\n\n");	
	System.out.println("(test weights all 1.0\n");
      }
    }
    return false;
  }

  /**
   * Checks whether the classifier can handle instance weights.
   * This test compares the classifier performance on two datasets
   * that are identical except for the training weights. If the 
   * results change, then the classifier must be using the weights. It
   * may be possible to get a false positive from this test if the 
   * weight changes aren't significant enough to induce a change
   * in classifier performance (but the weights are chosen to minimize
   * the likelihood of this).
   *
   * @param nominalPredictor if true use nominal predictor attributes
   * @param numericPredictor if true use numeric predictor attributes
   * @param numericClass if true use a numeric class attribute otherwise a
   * nominal class attribute
   * @return true if the test was passed
   */
  protected boolean instanceWeights(boolean nominalPredictor,
				    boolean numericPredictor, 
				    boolean numericClass) {

    System.out.print("classifier uses instance weights");
    printAttributeSummary(nominalPredictor, numericPredictor, numericClass);
    System.out.print("...");
    int numTrain = 40, numTest = 20, numClasses = 2, missingLevel = 0;
    boolean predictorMissing = false, classMissing = false;

    Instances train = null;
    Instances test = null;
    Classifier [] classifiers = null;
    Evaluation evaluationB = null;
    Evaluation evaluationI = null;
    boolean built = false;
    boolean evalFail = false;
    try {
      train = makeTestDataset(43, numTrain, 
			      nominalPredictor ? 3 : 0,
			      numericPredictor ? 2 : 0, 
			      numClasses, 
			      numericClass);
      test = makeTestDataset(24, numTest,
			     nominalPredictor ? 3 : 0,
			     numericPredictor ? 2 : 0, 
			     numClasses, 
			     numericClass);
      if (nominalPredictor) {
	train.deleteAttributeAt(0);
	test.deleteAttributeAt(0);
      }
      if (missingLevel > 0) {
	addMissing(train, missingLevel, predictorMissing, classMissing);
	addMissing(test, Math.min(missingLevel, 50), predictorMissing, 
		   classMissing);
      }
      classifiers = Classifier.makeCopies(getClassifier(), 2);
      evaluationB = new Evaluation(train);
      evaluationI = new Evaluation(train);
      classifiers[0].buildClassifier(train);
      testWRTZeroR(classifiers[0], evaluationB, train, test);
    } catch (Exception ex) {
      throw new Error("Error setting up for tests: " + ex.getMessage());
    }
    try {

      // Now modify instance weights and re-built/test
      for (int i = 0; i < train.numInstances(); i++) {
	train.instance(i).setWeight(0);
      }
      Random random = new Random(1);
      for (int i = 0; i < train.numInstances() / 2; i++) {
	int inst = Math.abs(random.nextInt()) % train.numInstances();
	int weight = Math.abs(random.nextInt()) % 10 + 1;
	train.instance(inst).setWeight(weight);
      }
      classifiers[1].buildClassifier(train);
      built = true;
      testWRTZeroR(classifiers[1], evaluationI, train, test);
      if (evaluationB.equals(evaluationI)) {
	//	System.out.println("no");
	evalFail = true;
	throw new Exception("evalFail");
      }
      System.out.println("yes");
      return true;
    } catch (Exception ex) {
      System.out.println("no");
      if (m_Debug) {
	System.out.println("\n=== Full Report ===");
	
	if (evalFail) {
	  System.out.println("Results don't differ between non-weighted and "
			     + "weighted instance models.");
	  System.out.println("Here are the results:\n");
	  System.out.println(evaluationB.toSummaryString("\nboth methods\n",
							 true));
	} else {
	  System.out.print("Problem during");
	  if (built) {
	    System.out.print(" testing");
	  } else {
	    System.out.print(" training");
	  }
	  System.out.println(": " + ex.getMessage() + "\n");
	}
	System.out.println("Here are the datasets:\n");
	System.out.println("=== Train Dataset ===\n"
			   + train.toString() + "\n");
	System.out.println("=== Train Weights ===\n");
	for (int i = 0; i < train.numInstances(); i++) {
	  System.out.println(" " + (i + 1) 
			     + "    " + train.instance(i).weight());
	}
	System.out.println("=== Test Dataset ===\n"
			   + test.toString() + "\n\n");	
	System.out.println("(test weights all 1.0\n");
      }
    }
    return false;
  }

  /**
   * Checks whether the scheme alters the training dataset during
   * training. If the scheme needs to modify the training
   * data it should take a copy of the training data. Currently checks
   * for changes to header structure, number of instances, order of
   * instances, instance weights.
   *
   * @param nominalPredictor if true use nominal predictor attributes
   * @param numericPredictor if true use numeric predictor attributes
   * @param numericClass if true use a numeric class attribute otherwise a
   * nominal class attribute
   * @param predictorMissing true if we know the classifier can handle
   * (at least) moderate missing predictor values
   * @param classMissing true if we know the classifier can handle
   * (at least) moderate missing class values
   * @return true if the test was passed
   */
  protected boolean datasetIntegrity(boolean nominalPredictor,
				     boolean numericPredictor, 
				     boolean numericClass,
				     boolean predictorMissing,
				     boolean classMissing) {

    System.out.print("classifier doesn't alter original datasets");
    printAttributeSummary(nominalPredictor, numericPredictor, numericClass);
    System.out.print("...");
    int numTrain = 20, numTest = 20, numClasses = 2, missingLevel = 20;

    Instances train = null;
    Instances test = null;
    Classifier classifier = null;
    Evaluation evaluation = null;
    boolean built = false;
    try {
      train = makeTestDataset(42, numTrain, 
			      nominalPredictor ? 2 : 0,
			      numericPredictor ? 1 : 0, 
			      numClasses, 
			      numericClass);
      test = makeTestDataset(24, numTest,
			     nominalPredictor ? 2 : 0,
			     numericPredictor ? 1 : 0, 
			     numClasses, 
			     numericClass);
      if (nominalPredictor) {
	train.deleteAttributeAt(0);
	test.deleteAttributeAt(0);
      }
      if (missingLevel > 0) {
	addMissing(train, missingLevel, predictorMissing, classMissing);
	addMissing(test, Math.min(missingLevel, 50), predictorMissing, 
		   classMissing);
      }
      classifier = Classifier.makeCopies(getClassifier(), 1)[0];
      evaluation = new Evaluation(train);
    } catch (Exception ex) {
      throw new Error("Error setting up for tests: " + ex.getMessage());
    }
    try {
      Instances trainCopy = new Instances(train);
      Instances testCopy = new Instances(test);
      classifier.buildClassifier(trainCopy);
      compareDatasets(train, trainCopy);
      built = true;
      testWRTZeroR(classifier, evaluation, trainCopy, testCopy);
      compareDatasets(test, testCopy);
      System.out.println("yes");
      return true;
    } catch (Exception ex) {
      System.out.println("no");
      if (m_Debug) {
	System.out.println("\n=== Full Report ===");
	System.out.print("Problem during");
	if (built) {
	  System.out.print(" testing");
	} else {
	  System.out.print(" training");
	}
	System.out.println(": " + ex.getMessage() + "\n");
	System.out.println("Here are the datasets:\n");
	System.out.println("=== Train Dataset ===\n"
			   + train.toString() + "\n");
	System.out.println("=== Test Dataset ===\n"
			   + test.toString() + "\n\n");
      }
    }
    return false;
  }


  /**
   * Runs a text on the datasets with the given characteristics.
   */
  protected boolean runBasicTest(boolean nominalPredictor,
				 boolean numericPredictor, 
				 boolean numericClass,
				 int missingLevel,
				 boolean predictorMissing,
				 boolean classMissing,
				 int numTrain,
				 int numTest,
				 int numClasses,
				 FastVector accepts) {

    Instances train = null;
    Instances test = null;
    Classifier classifier = null;
    Evaluation evaluation = null;
    boolean built = false;
    try {
      train = makeTestDataset(42, numTrain, 
			      nominalPredictor ? 2 : 0,
			      numericPredictor ? 1 : 0, 
			      numClasses, 
			      numericClass);
      test = makeTestDataset(24, numTest,
			     nominalPredictor ? 2 : 0,
			     numericPredictor ? 1 : 0, 
			     numClasses, 
			     numericClass);
      if (nominalPredictor) {
	train.deleteAttributeAt(0);
	test.deleteAttributeAt(0);
      }
      if (missingLevel > 0) {
	addMissing(train, missingLevel, predictorMissing, classMissing);
	addMissing(test, Math.min(missingLevel, 50), predictorMissing, 
		   classMissing);
      }
      classifier = Classifier.makeCopies(getClassifier(), 1)[0];
      evaluation = new Evaluation(train);
    } catch (Exception ex) {
      throw new Error("Error setting up for tests: " + ex.getMessage());
    }
    try {
      classifier.buildClassifier(train);
      built = true;
      if (!testWRTZeroR(classifier, evaluation, train, test)) {
	throw new Exception("Scheme performs worse than ZeroR");
      }
      System.out.println("yes");
      return true;
    } catch (Exception ex) {
      boolean acceptable = false;
      String msg = ex.getMessage().toLowerCase();
      if (msg.indexOf("worse than zeror") >= 0) {
	System.out.println("warning: performs worse than ZeroR");
      } else {
	for (int i = 0; i < accepts.size(); i++) {
	  if (msg.indexOf((String)accepts.elementAt(i)) >= 0) {
	    acceptable = true;
	  }
	}
	System.out.println("no" + (acceptable ? " (OK error message)" : ""));
      }
      if (m_Debug) {
	System.out.println("\n=== Full Report ===");
	System.out.print("Problem during");
	if (built) {
	  System.out.print(" testing");
	} else {
	  System.out.print(" training");
	}
	System.out.println(": " + ex.getMessage() + "\n");
	if (!acceptable) {
	  if (accepts.size() > 0) {
	    System.out.print("Error message doesn't mention ");
	    for (int i = 0; i < accepts.size(); i++) {
	      if (i != 0) {
		System.out.print(" or ");
	      }
	      System.out.print('"' + (String)accepts.elementAt(i) + '"');
	    }
	  }
	  System.out.println("here are the datasets:\n");
	  System.out.println("=== Train Dataset ===\n"
			     + train.toString() + "\n");
	  System.out.println("=== Test Dataset ===\n"
			     + test.toString() + "\n\n");
	}
      }
    }
    return false;
  }

  /**
   * Determine whether the scheme performs worse than ZeroR during testing
   *
   * @param classifier the pre-trained classifier
   * @param evaluation the classifier evaluation object
   * @param train the training data
   * @param test the test data
   * @return true if the scheme performs better than ZeroR
   * @exception Exception if there was a problem during the scheme's testing
   */
  protected boolean testWRTZeroR(Classifier classifier,
				 Evaluation evaluation,
				 Instances train, Instances test) 
    throws Exception {
	 
    evaluation.evaluateModel(classifier, test);
    try {

      // Tested OK, compare with ZeroR
      Classifier zeroR = new weka.classifiers.ZeroR();
      zeroR.buildClassifier(train);
      Evaluation zeroREval = new Evaluation(train);
      zeroREval.evaluateModel(zeroR, test);
      return Utils.grOrEq(zeroREval.errorRate(), evaluation.errorRate());
    } catch (Exception ex) {
      throw new Error("Problem determining ZeroR performance: "
		      + ex.getMessage());
    }
  }

  /**
   * Compare two datasets to see if they differ.
   *
   * @param data1 one set of instances
   * @param data2 the other set of instances
   * @exception Exception if the datasets differ
   */
  protected void compareDatasets(Instances data1, Instances data2)
    throws Exception {
    if (!data2.equalHeaders(data1)) {
      throw new Exception("header has been modified");
    }
    if (!(data2.numInstances() == data1.numInstances())) {
      throw new Exception("number of instances has changed");
    }
    for (int i = 0; i < data2.numInstances(); i++) {
      Instance orig = data1.instance(i);
      Instance copy = data2.instance(i);
      for (int j = 0; j < orig.numAttributes(); j++) {
	if (orig.isMissing(j)) {
	  if (!copy.isMissing(j)) {
	    throw new Exception("instances have changed");
	  }
	} else if (orig.value(j) != copy.value(j)) {
	    throw new Exception("instances have changed");
	}
	if (orig.weight() != copy.weight()) {
	  throw new Exception("instance weights have changed");
	}	  
      }
    }
  }

  /**
   * Add missing values to a dataset.
   *
   * @param data the instances to add missing values to
   * @param level the level of missing values to add (if positive, this
   * is the probability that a value will be set to missing, if negative
   * all but one value will be set to missing (not yet implemented))
   * @param predictorMissing if true, predictor attributes will be modified
   * @param classMissing if true, the class attribute will be modified
   */
  protected void addMissing(Instances data, int level,
			    boolean predictorMissing, boolean classMissing) {

    int classIndex = data.classIndex();
    Random random = new Random(1);
    for (int i = 0; i < data.numInstances(); i++) {
      Instance current = data.instance(i);
      for (int j = 0; j < data.numAttributes(); j++) {
	if (((j == classIndex) && classMissing) ||
	    ((j != classIndex) && predictorMissing)) {
	  if (Math.abs(random.nextInt()) % 100 < level)
	    current.setMissing(j);
	}
      }
    }
  }

  /**
   * Make a simple set of instances, which can later be modified
   * for use in specific tests.
   *
   * @param seed the random number seed
   * @param numInstances the number of instances to generate
   * @param numNominal the number of nominal attributes
   * @param numNumeric the number of numeric attributes
   * @param numClasses the number of classes (if nominal class)
   * @param numericClass true if the class attribute should be numeric
   * @return the test dataset
   * @exception Exception if the dataset couldn't be generated
   */
  protected Instances makeTestDataset(int seed, int numInstances, 
				      int numNominal, int numNumeric, 
				      int numClasses, boolean numericClass)
    throws Exception {

    int numAttributes = numNominal + numNumeric + 1;
    Random random = new Random(seed);
    FastVector attributes = new FastVector(numAttributes);

    // Add Nominal attributes
    for (int i = 0; i < numNominal; i++) {
      FastVector nomStrings = new FastVector(i + 1);
      for(int j = 0; j <= i; j++) {
	nomStrings.addElement("a" + (i + 1) + "l" + (j + 1));
      }
      attributes.addElement(new Attribute("Nominal" + (i + 1), nomStrings));
    }

    // Add Numeric attributes
    for (int i = 0; i < numNumeric; i++) {
      attributes.addElement(new Attribute("Numeric" + (i + 1)));
    }

    // TODO: Add some String attributes...

    // Add class attribute
    if (numericClass) {
      attributes.addElement(new Attribute("Class"));
    } else {
      FastVector nomStrings = new FastVector();
      for(int j = 0; j <numClasses; j++) {
	nomStrings.addElement("cl" + (j + 1));
      }
      attributes.addElement(new Attribute("Class",nomStrings));
    }    

    Instances data = new Instances("CheckSet", attributes, numInstances);
    data.setClassIndex(data.numAttributes() - 1);

    // Generate the instances
    for (int i = 0; i < numInstances; i++) {
      Instance current = new Instance(numAttributes);
      current.setDataset(data);
      if (numericClass) {
	
	current.setClassValue(random.nextFloat() * 0.25
			      + Math.abs(random.nextInt())
			      % Math.max(2, numNominal));
      } else {
	current.setClassValue(Math.abs(random.nextInt()) % data.numClasses());
      }
      double classVal = current.classValue();
      double newVal = 0;
      for (int j = 0; j < numAttributes - 1; j++) {
	switch (data.attribute(j).type()) {
	case Attribute.NUMERIC:
	  newVal = classVal * 4 + random.nextFloat() * 1 - 0.5;
	  current.setValue(j, newVal);
	  break;
	case Attribute.NOMINAL:
	  if (random.nextFloat() < 0.2) {
	    newVal = Math.abs(random.nextInt())
	      % data.attribute(j).numValues();
	  } else {
	    newVal = ((int)classVal) % data.attribute(j).numValues();
	  }
	  current.setValue(j, newVal);
	  break;
	case Attribute.STRING:
	  System.err.println("Huh? this bit isn't implemented yet");
	  break;
	}
      }
      data.add(current);
    }
    return data;
  }

  /**
   * Print out a short summary string for the dataset characteristics
   *
   * @param nominalPredictor true if nominal predictor attributes are present
   * @param numericPredictor true if numeric predictor attributes are present
   * @param numericClass true if the class attribute is numeric
   */
  protected void printAttributeSummary(boolean nominalPredictor, 
				       boolean numericPredictor, 
				       boolean numericClass) {
    
    if (numericClass) {
      System.out.print(" (numeric class,");
    } else {
      System.out.print(" (nominal class,");
    }
    if (numericPredictor) {
      System.out.print(" numeric");
      if (nominalPredictor) {
	System.out.print(" &");
      }
    }
    if (nominalPredictor) {
      System.out.print(" nominal");
    }
    System.out.print(" predictors)");
  }
}


