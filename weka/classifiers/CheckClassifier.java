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

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;

import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

/**
 * Class for examining the capabilities and finding problems with 
 * classifiers. If you implement a classifier using the WEKA.libraries,
 * you should run the checks on it to ensure robustness and correct
 * operation. Passing all the tests of this object does not mean
 * bugs in the classifier don't exist, but this will help find some
 * common ones. <p/>
 * 
 * Typical usage: <p/>
 * <code>java weka.classifiers.CheckClassifier -W classifier_name 
 * classifier_options </code><p/>
 * 
 * CheckClassifier reports on the following:
 * <ul>
 *    <li> Classifier abilities <ul>
 *         <li> Possible command line options to the classifier
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
 * training and test datasets for any failed tests.<p/>
 *
 * The <code>weka.classifiers.AbstractClassifierTest</code> uses this
 * class to test all the classifiers. Any changes here, have to be 
 * checked in that abstract test class, too. <p/>
 *
 * Valid options are:<p/>
 *
 * -D <br/>
 * Turn on debugging output.<p/>
 *
 * -S <br/>
 * Silent mode, i.e., no output at all.<p/>
 *
 * -N num <br/>
 * Number of instances to use for datasets (default 20).<p/>
 *
 * -W classname <br/>
 * Specify the full class name of a classifier to perform the 
 * tests on (required).<p/>
 *
 * Options after -- are passed to the designated classifier.<p/>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.16.2.1 $
 */

/*
 * Note about test methods:
 * - return array of booleans
 * - first index: success or not
 * - second index: acceptable or not (e.g., Exception is OK)
 *
 * FracPete (fracpete at waikato dot ac dot nz)
 */
public class CheckClassifier implements OptionHandler {

  /*** The classifier to be examined */
  protected Classifier m_Classifier = new weka.classifiers.rules.ZeroR();

  /** The options to be passed to the base classifier. */
  protected String [] m_ClassifierOptions;

  /** The results of the analysis as a string */
  protected String m_AnalysisResults;

  /** Debugging mode, gives extra output if true */
  protected boolean m_Debug = false;

  /** Silent mode, for no output at all to stdout */
  protected boolean m_Silent = false;

  /** The number of instances in the datasets */
  protected int m_NumInstances = 20;
  
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
              "\tSilent mode - prints nothing to stdout.",
              "S", 0, "-S"));

    newVector.addElement(new Option(
	      "\tThe number of instances in the datasets (default 20).",
	      "N", 1, "-N <num>"));

    newVector.addElement(new Option(
	      "\tFull name of the classifier analysed.\n"
	      +"\teg: weka.classifiers.bayes.NaiveBayes",
	      "W", 1, "-W"));

    if ((m_Classifier != null) 
	&& (m_Classifier instanceof OptionHandler)) {
      newVector.addElement(new Option("", "", 0, 
				      "\nOptions specific to classifier "
				      + m_Classifier.getClass().getName()
				      + ":"));
      Enumeration enu = ((OptionHandler)m_Classifier).listOptions();
      while (enu.hasMoreElements())
	newVector.addElement(enu.nextElement());
    }
    
    return newVector.elements();
  }

  /**
   * Parses a given list of options. 
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    String      tmpStr;
    
    setDebug(Utils.getFlag('D', options));
    
    setSilent(Utils.getFlag('S', options));

    tmpStr = Utils.getOption('N', options);
    if (tmpStr.length() != 0)
      setNumInstances(Integer.parseInt(tmpStr));
    else
      setNumInstances(20);
    
    tmpStr = Utils.getOption('W', options);
    if (tmpStr.length() == 0)
      throw new Exception("A classifier must be specified with"
			  + " the -W option.");
    setClassifier(Classifier.forName(tmpStr, Utils.partitionOptions(options)));
  }

  /**
   * Gets the current settings of the CheckClassifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    Vector        result;
    String[]      options;
    int           i;
    
    result = new Vector();

    if (getDebug())
      result.add("-D");

    if (getSilent())
      result.add("-S");

    result.add("-N");
    result.add("" + getNumInstances());
    
    if (getClassifier() != null) {
      result.add("-W");
      result.add(getClassifier().getClass().getName());
    }
    
    if ((m_Classifier != null) && (m_Classifier instanceof OptionHandler))
      options = ((OptionHandler) m_Classifier).getOptions();
    else
      options = new String[0];

    if (options.length > 0) {
      result.add("--");
      for (i = 0; i < options.length; i++)
        result.add(options[i]);
    }
    
    return (String[]) result.toArray(new String[result.size()]);
  }

  /**
   * Begin the tests, reporting results to System.out
   */
  public void doTests() {

    if (getClassifier() == null) {
      println("\n=== No classifier set ===");
      return;
    }
    println("\n=== Check on Classifier: "
		       + getClassifier().getClass().getName()
		       + " ===\n");

    // Start tests
    canTakeOptions();
    boolean updateableClassifier = updateableClassifier()[0];
    boolean weightedInstancesHandler = weightedInstancesHandler()[0];
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
    // disable silent mode, if necessary
    if (getDebug())
      setSilent(false);
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
   * Set slient mode, i.e., no output at all to stdout
   *
   * @param value whether silent mode is active or not
   */
  public void setSilent(boolean value) {
    m_Silent = value;
  }

  /**
   * Get whether silent mode is turned on
   *
   * @return true if silent mode is on
   */
  public boolean getSilent() {
    return m_Silent;
  }

  /**
   * Sets the number of instances to use in the datasets (some classifiers
   * might require more instances).
   *
   * @param value the number of instances to use
   */
  public void setNumInstances(int value) {
    m_NumInstances = value;
  }

  /**
   * Gets the current number of instances to use for the datasets.
   *
   * @return the number of instances
   */
  public int getNumInstances() {
    return m_NumInstances;
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
   * prints the given message to stdout, if not silent mode
   * 
   * @param msg         the text to print to stdout
   */
  protected void print(Object msg) {
    if (!getSilent())
      System.out.print(msg);
  }
  
  /**
   * prints the given message (+ LF) to stdout, if not silent mode
   * 
   * @param msg         the message to println to stdout
   */
  protected void println(Object msg) {
    print(msg + "\n");
  }
  
  /**
   * prints a LF to stdout, if not silent mode
   */
  protected void println() {
    print("\n");
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

    boolean PNom = canPredict(true, false, false, numericClass)[0];
    boolean PNum = canPredict(false, true, false, numericClass)[0];
    boolean PStr = canPredict(false, false, true, numericClass)[0];
    if (PNom || PNum || PStr) {
      if (weighted)
	instanceWeights(PNom, PNum, PStr, numericClass);

      if (!numericClass)
	canHandleNClasses(PNom, PNum, PStr, 4);

      canHandleZeroTraining(PNom, PNum, PStr, numericClass);
      boolean handleMissingPredictors = canHandleMissing(PNom, PNum, PStr, 
							 numericClass, 
							 true, false, 20)[0];
      if (handleMissingPredictors)
	canHandleMissing(PNom, PNum, PStr, numericClass, true, false, 100);

      boolean handleMissingClass = canHandleMissing(PNom, PNum, PStr,
                                                    numericClass, 
						    false, true, 20)[0];
      if (handleMissingClass)
	canHandleMissing(PNom, PNum, PStr, numericClass, false, true, 100);

      correctBuildInitialisation(PNom, PNum, PStr, numericClass);
      datasetIntegrity(PNom, PNum, PStr, numericClass,
		       handleMissingPredictors, handleMissingClass);
      doesntUseTestClassVal(PNom, PNum, PStr, numericClass);
      if (updateable)
	updatingEquality(PNom, PNum, PStr, numericClass);
    }
  }

  /**
   * Checks whether the scheme can take command line options.
   *
   * @return index 0 is true if the classifier can take options
   */
  protected boolean[] canTakeOptions() {

    boolean[] result = new boolean[2];
    
    print("options...");
    if (m_Classifier instanceof OptionHandler) {
      println("yes");
      if (m_Debug) {
	println("\n=== Full report ===");
	Enumeration enu = ((OptionHandler)m_Classifier).listOptions();
	while (enu.hasMoreElements()) {
	  Option option = (Option) enu.nextElement();
	  print(option.synopsis() + "\n" 
			   + option.description() + "\n");
	}
	println("\n");
      }
      result[0] = true;
    }
    else {
      println("no");
      result[0] = false;
    }

    return result;
  }

  /**
   * Checks whether the scheme can build models incrementally.
   *
   * @return index 0 is true if the classifier can train incrementally
   */
  protected boolean[] updateableClassifier() {

    boolean[] result = new boolean[2];
    
    print("updateable classifier...");
    if (m_Classifier instanceof UpdateableClassifier) {
      println("yes");
      result[0] = true;
    }
    else {
      println("no");
      result[0] = false;
    }

    return result;
  }

  /**
   * Checks whether the scheme says it can handle instance weights.
   *
   * @return true if the classifier handles instance weights
   */
  protected boolean[] weightedInstancesHandler() {

    boolean[] result = new boolean[2];
    
    print("weighted instances classifier...");
    if (m_Classifier instanceof WeightedInstancesHandler) {
      println("yes");
      result[0] = true;
    }
    else {
      println("no");
      result[0] = false;
    }

    return result;
  }

  /**
   * Checks basic prediction of the scheme, for simple non-troublesome
   * datasets.
   *
   * @param nominalPredictor if true use nominal predictor attributes
   * @param numericPredictor if true use numeric predictor attributes
   * @param stringPredictor if true use string predictor attributes
   * @param numericClass if true use a numeric class attribute otherwise a
   * nominal class attribute
   * @return index 0 is true if the test was passed, index 1 is true if test 
   *         was acceptable
   */
  protected boolean[] canPredict(boolean nominalPredictor,
			         boolean numericPredictor, 
                                 boolean stringPredictor, 
			         boolean numericClass) {

    print("basic predict");
    printAttributeSummary(
        nominalPredictor, numericPredictor, stringPredictor, numericClass);
    print("...");
    FastVector accepts = new FastVector();
    accepts.addElement("nominal");
    accepts.addElement("numeric");
    accepts.addElement("string");
    int numTrain = getNumInstances(), numTest = getNumInstances(), 
        numClasses = 2, missingLevel = 0;
    boolean predictorMissing = false, classMissing = false;

    return runBasicTest(nominalPredictor, numericPredictor, stringPredictor, 
                        numericClass, 
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
   * @param stringPredictor if true use string predictor attributes
   * @param numClasses the number of classes to test
   * @return index 0 is true if the test was passed, index 1 is true if test 
   *         was acceptable
   */
  protected boolean[] canHandleNClasses(boolean nominalPredictor,
				        boolean numericPredictor, 
                                        boolean stringPredictor, 
				        int numClasses) {

    print("more than two class problems");
    printAttributeSummary(
        nominalPredictor, numericPredictor, stringPredictor, false);
    print("...");
    FastVector accepts = new FastVector();
    accepts.addElement("number");
    accepts.addElement("class");
    int numTrain = getNumInstances(), numTest = getNumInstances(), 
        missingLevel = 0;
    boolean predictorMissing = false, classMissing = false;

    return runBasicTest(nominalPredictor, numericPredictor, stringPredictor, 
                        false,
			missingLevel, predictorMissing, classMissing,
			numTrain, numTest, numClasses, 
			accepts);
  }

  /**
   * Checks whether the scheme can handle zero training instances.
   *
   * @param nominalPredictor if true use nominal predictor attributes
   * @param numericPredictor if true use numeric predictor attributes
   * @param stringPredictor if true use string predictor attributes
   * @param numericClass if true use a numeric class attribute otherwise a
   * nominal class attribute
   * @return index 0 is true if the test was passed, index 1 is true if test 
   *         was acceptable
   */
  protected boolean[] canHandleZeroTraining(boolean nominalPredictor,
					    boolean numericPredictor, 
                                            boolean stringPredictor, 
					    boolean numericClass) {

    print("handle zero training instances");
    printAttributeSummary(
        nominalPredictor, numericPredictor, stringPredictor, numericClass);
    print("...");
    FastVector accepts = new FastVector();
    accepts.addElement("train");
    accepts.addElement("value");
    int numTrain = 0, numTest = getNumInstances(), numClasses = 2, 
        missingLevel = 0;
    boolean predictorMissing = false, classMissing = false;

    return runBasicTest(nominalPredictor, numericPredictor, stringPredictor, 
                        numericClass, 
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
   * @param stringPredictor if true use string predictor attributes
   * @param numericClass if true use a numeric class attribute otherwise a
   * nominal class attribute
   * @return index 0 is true if the test was passed, index 1 is true if the
   *         scheme performs worse than ZeroR, but without error (index 0 is
   *         false)
   */
  protected boolean[] correctBuildInitialisation(boolean nominalPredictor,
					         boolean numericPredictor, 
                                                 boolean stringPredictor, 
					         boolean numericClass) {

    boolean[] result = new boolean[2];

    print("correct initialisation during buildClassifier");
    printAttributeSummary(
        nominalPredictor, numericPredictor, stringPredictor, numericClass);
    print("...");
    int numTrain = getNumInstances(), numTest = getNumInstances(), 
        numClasses = 2, missingLevel = 0;
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
			       stringPredictor ? 1 : 0, 
			       numClasses, 
			       numericClass);
      train2 = makeTestDataset(84, numTrain, 
			       nominalPredictor ? 3 : 0,
			       numericPredictor ? 2 : 0, 
			       stringPredictor ? 1 : 0, 
			       numClasses, 
			       numericClass);
      test1 = makeTestDataset(24, numTest,
			      nominalPredictor ? 2 : 0,
			      numericPredictor ? 1 : 0, 
			      stringPredictor ? 1 : 0, 
			      numClasses, 
			      numericClass);
      test2 = makeTestDataset(48, numTest,
			      nominalPredictor ? 3 : 0,
			      numericPredictor ? 2 : 0, 
			      stringPredictor ? 1 : 0, 
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
      if (!testWRTZeroR(classifier, evaluation1A, train1, test1)[0]) {
	throw new Exception("Scheme performs worse than ZeroR");
      }

      stage = 1;
      built = false;
      classifier.buildClassifier(train2);
      built = true;
      if (!testWRTZeroR(classifier, evaluation2, train2, test2)[0]) {
	throw new Exception("Scheme performs worse than ZeroR");
      }

      stage = 2;
      built = false;
      classifier.buildClassifier(train1);
      built = true;
      if (!testWRTZeroR(classifier, evaluation1B, train1, test1)[0]) {
	throw new Exception("Scheme performs worse than ZeroR");
      }

      stage = 3;
      if (!evaluation1A.equals(evaluation1B)) {
	if (m_Debug) {
	  println("\n=== Full report ===\n"
		+ evaluation1A.toSummaryString("\nFirst buildClassifier()",
					       true)
		+ "\n\n");
	  println(
                evaluation1B.toSummaryString("\nSecond buildClassifier()",
					     true)
		+ "\n\n");
	}
	throw new Exception("Results differ between buildClassifier calls");
      }
      println("yes");
      result[0] = true;

      if (false && m_Debug) {
	println("\n=== Full report ===\n"
                + evaluation1A.toSummaryString("\nFirst buildClassifier()",
					       true)
		+ "\n\n");
	println(
                evaluation1B.toSummaryString("\nSecond buildClassifier()",
					     true)
		+ "\n\n");
      }
    } 
    catch (Exception ex) {
      String msg = ex.getMessage().toLowerCase();
      if (msg.indexOf("worse than zeror") >= 0) {
	println("warning: performs worse than ZeroR");
        result[1] = true;
      } else {
	println("no");
        result[0] = false;
      }
      if (m_Debug) {
	println("\n=== Full Report ===");
	print("Problem during");
	if (built) {
	  print(" testing");
	} else {
	  print(" training");
	}
	switch (stage) {
	case 0:
	  print(" of dataset 1");
	  break;
	case 1:
	  print(" of dataset 2");
	  break;
	case 2:
	  print(" of dataset 1 (2nd build)");
	  break;
	case 3:
	  print(", comparing results from builds of dataset 1");
	  break;	  
	}
	println(": " + ex.getMessage() + "\n");
	println("here are the datasets:\n");
	println("=== Train1 Dataset ===\n"
			   + train1.toString() + "\n");
	println("=== Test1 Dataset ===\n"
			   + test1.toString() + "\n\n");
	println("=== Train2 Dataset ===\n"
			   + train2.toString() + "\n");
	println("=== Test2 Dataset ===\n"
			   + test2.toString() + "\n\n");
      }
    }

    return result;
  }

  /**
   * Checks basic missing value handling of the scheme. If the missing
   * values cause an exception to be thrown by the scheme, this will be
   * recorded.
   *
   * @param nominalPredictor if true use nominal predictor attributes
   * @param numericPredictor if true use numeric predictor attributes
   * @param stringPredictor if true use string predictor attributes
   * @param numericClass if true use a numeric class attribute otherwise a
   * nominal class attribute
   * @param predictorMissing true if the missing values may be in 
   * the predictors
   * @param classMissing true if the missing values may be in the class
   * @param level the percentage of missing values
   * @return index 0 is true if the test was passed, index 1 is true if test 
   *         was acceptable
   */
  protected boolean[] canHandleMissing(boolean nominalPredictor,
				       boolean numericPredictor, 
                                       boolean stringPredictor, 
				       boolean numericClass,
				       boolean predictorMissing,
				       boolean classMissing,
				       int missingLevel) {

    if (missingLevel == 100)
      print("100% ");
    print("missing");
    if (predictorMissing) {
      print(" predictor");
      if (classMissing)
	print(" and");
    }
    if (classMissing)
      print(" class");
    print(" values");
    printAttributeSummary(
        nominalPredictor, numericPredictor, stringPredictor, numericClass);
    print("...");
    FastVector accepts = new FastVector();
    accepts.addElement("missing");
    accepts.addElement("value");
    accepts.addElement("train");
    int numTrain = getNumInstances(), numTest = getNumInstances(), 
        numClasses = 2;

    return runBasicTest(nominalPredictor, numericPredictor, stringPredictor, 
                        numericClass, 
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
   * @param stringPredictor if true use string predictor attributes
   * @param numericClass if true use a numeric class attribute otherwise a
   * nominal class attribute
   * @return index 0 is true if the test was passed
   */
  protected boolean[] updatingEquality(boolean nominalPredictor,
				       boolean numericPredictor, 
                                       boolean stringPredictor, 
				       boolean numericClass) {

    print("incremental training produces the same results"
		     + " as batch training");
    printAttributeSummary(
        nominalPredictor, numericPredictor, stringPredictor, numericClass);
    print("...");
    int numTrain = getNumInstances(), numTest = getNumInstances(), 
        numClasses = 2, missingLevel = 0;
    boolean predictorMissing = false, classMissing = false;

    boolean[] result = new boolean[2];
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
			      stringPredictor ? 1 : 0, 
			      numClasses, 
			      numericClass);
      test = makeTestDataset(24, numTest,
			     nominalPredictor ? 2 : 0,
			     numericPredictor ? 1 : 0, 
			     stringPredictor ? 1 : 0, 
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
	println("no");
        result[0] = false;

	if (m_Debug) {
	  println("\n=== Full Report ===");
	  println("Results differ between batch and "
			     + "incrementally built models.\n"
			     + "Depending on the classifier, this may be OK");
	  println("Here are the results:\n");
	  println(evaluationB.toSummaryString(
			     "\nbatch built results\n", true));
	  println(evaluationI.toSummaryString(
                             "\nincrementally built results\n", true));
	  println("Here are the datasets:\n");
	  println("=== Train Dataset ===\n"
			     + train.toString() + "\n");
	  println("=== Test Dataset ===\n"
			     + test.toString() + "\n\n");
	}
      }
      else {
        println("yes");
        result[0] = true;
      }
    } catch (Exception ex) {
      result[0] = false;

      print("Problem during");
      if (built)
	print(" testing");
      else
	print(" training");
      println(": " + ex.getMessage() + "\n");
    }

    return result;
  }

  /**
   * Checks whether the classifier erroneously uses the class
   * value of test instances (if provided). Runs the classifier with
   * test instance class values set to missing and compares with results
   * when test instance class values are left intact.
   *
   * @param nominalPredictor if true use nominal predictor attributes
   * @param numericPredictor if true use numeric predictor attributes
   * @param stringPredictor if true use string predictor attributes
   * @param numericClass if true use a numeric class attribute otherwise a
   * nominal class attribute
   * @return index 0 is true if the test was passed
   */
  protected boolean[] doesntUseTestClassVal(boolean nominalPredictor,
					    boolean numericPredictor, 
                                            boolean stringPredictor, 
					    boolean numericClass) {

    print("classifier ignores test instance class vals");
    printAttributeSummary(
        nominalPredictor, numericPredictor, stringPredictor, numericClass);
    print("...");
    int numTrain = 2*getNumInstances(), numTest = getNumInstances(), 
        numClasses = 2, missingLevel = 0;
    boolean predictorMissing = false, classMissing = false;

    boolean[] result = new boolean[2];
    Instances train = null;
    Instances test = null;
    Classifier [] classifiers = null;
    Evaluation evaluationB = null;
    Evaluation evaluationI = null;
    boolean evalFail = false;
    try {
      train = makeTestDataset(42, numTrain, 
			      nominalPredictor ? 3 : 0,
			      numericPredictor ? 2 : 0, 
			      stringPredictor ? 1 : 0, 
			      numClasses, 
			      numericClass);
      test = makeTestDataset(24, numTest,
			     nominalPredictor ? 3 : 0,
			     numericPredictor ? 2 : 0, 
			     stringPredictor ? 1 : 0, 
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
	double [] dist0 = classifiers[0].distributionForInstance(testInst);
	double [] dist1 = classifiers[1].distributionForInstance(classMissingInst);
	for (int j = 0; j < dist0.length; j++) {
	  if (dist0[j] != dist1[j]) {
	    throw new Exception("Prediction different for instance " 
				+ (i + 1));
	  }
	}
      }

      println("yes");
      result[0] = true;
    } catch (Exception ex) {
      println("no");
      result[0] = false;

      if (m_Debug) {
	println("\n=== Full Report ===");
	
	if (evalFail) {
	  println("Results differ between non-missing and "
			     + "missing test class values.");
	} else {
	  print("Problem during testing");
	  println(": " + ex.getMessage() + "\n");
	}
	println("Here are the datasets:\n");
	println("=== Train Dataset ===\n"
			   + train.toString() + "\n");
	println("=== Train Weights ===\n");
	for (int i = 0; i < train.numInstances(); i++) {
	  println(" " + (i + 1) 
			     + "    " + train.instance(i).weight());
	}
	println("=== Test Dataset ===\n"
			   + test.toString() + "\n\n");	
	println("(test weights all 1.0\n");
      }
    }

    return result;
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
   * @param stringPredictor if true use string predictor attributes
   * @param numericClass if true use a numeric class attribute otherwise a
   * nominal class attribute
   * @return index 0 true if the test was passed
   */
  protected boolean[] instanceWeights(boolean nominalPredictor,
				      boolean numericPredictor, 
                                      boolean stringPredictor, 
				      boolean numericClass) {

    print("classifier uses instance weights");
    printAttributeSummary(
        nominalPredictor, numericPredictor, stringPredictor, numericClass);
    print("...");
    int numTrain = 2*getNumInstances(), numTest = getNumInstances(), 
        numClasses = 2, missingLevel = 0;
    boolean predictorMissing = false, classMissing = false;

    boolean[] result = new boolean[2];
    Instances train = null;
    Instances test = null;
    Classifier [] classifiers = null;
    Evaluation evaluationB = null;
    Evaluation evaluationI = null;
    boolean built = false;
    boolean evalFail = false;
    try {
      train = makeTestDataset(42, numTrain, 
			      nominalPredictor ? 3 : 0,
			      numericPredictor ? 2 : 0, 
			      stringPredictor ? 1 : 0, 
			      numClasses, 
			      numericClass);
      test = makeTestDataset(24, numTest,
			     nominalPredictor ? 3 : 0,
			     numericPredictor ? 2 : 0, 
			     stringPredictor ? 1 : 0, 
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
	//	println("no");
	evalFail = true;
	throw new Exception("evalFail");
      }

      println("yes");
      result[0] = true;
    } catch (Exception ex) {
      println("no");
      result[0] = false;

      if (m_Debug) {
	println("\n=== Full Report ===");
	
	if (evalFail) {
	  println("Results don't differ between non-weighted and "
			     + "weighted instance models.");
	  println("Here are the results:\n");
	  println(evaluationB.toSummaryString("\nboth methods\n",
							 true));
	} else {
	  print("Problem during");
	  if (built) {
	    print(" testing");
	  } else {
	    print(" training");
	  }
	  println(": " + ex.getMessage() + "\n");
	}
	println("Here are the datasets:\n");
	println("=== Train Dataset ===\n"
			   + train.toString() + "\n");
	println("=== Train Weights ===\n");
	for (int i = 0; i < train.numInstances(); i++) {
	  println(" " + (i + 1) 
			     + "    " + train.instance(i).weight());
	}
	println("=== Test Dataset ===\n"
			   + test.toString() + "\n\n");	
	println("(test weights all 1.0\n");
      }
    }

    return result;
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
   * @param stringPredictor if true use string predictor attributes
   * @param numericClass if true use a numeric class attribute otherwise a
   * nominal class attribute
   * @param predictorMissing true if we know the classifier can handle
   * (at least) moderate missing predictor values
   * @param classMissing true if we know the classifier can handle
   * (at least) moderate missing class values
   * @return index 0 is true if the test was passed
   */
  protected boolean[] datasetIntegrity(boolean nominalPredictor,
				       boolean numericPredictor, 
                                       boolean stringPredictor, 
				       boolean numericClass,
				       boolean predictorMissing,
				       boolean classMissing) {

    print("classifier doesn't alter original datasets");
    printAttributeSummary(
        nominalPredictor, numericPredictor, stringPredictor, numericClass);
    print("...");
    int numTrain = getNumInstances(), numTest = getNumInstances(), 
        numClasses = 2, missingLevel = 20;

    boolean[] result = new boolean[2];
    Instances train = null;
    Instances test = null;
    Classifier classifier = null;
    Evaluation evaluation = null;
    boolean built = false;
    try {
      train = makeTestDataset(42, numTrain, 
			      nominalPredictor ? 2 : 0,
			      numericPredictor ? 1 : 0, 
			      stringPredictor ? 1 : 0, 
			      numClasses, 
			      numericClass);
      test = makeTestDataset(24, numTest,
			     nominalPredictor ? 2 : 0,
			     numericPredictor ? 1 : 0, 
			     stringPredictor ? 1 : 0, 
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

      println("yes");
      result[0] = true;
    } catch (Exception ex) {
      println("no");
      result[0] = false;

      if (m_Debug) {
	println("\n=== Full Report ===");
	print("Problem during");
	if (built) {
	  print(" testing");
	} else {
	  print(" training");
	}
	println(": " + ex.getMessage() + "\n");
	println("Here are the datasets:\n");
	println("=== Train Dataset ===\n"
			   + train.toString() + "\n");
	println("=== Test Dataset ===\n"
			   + test.toString() + "\n\n");
      }
    }

    return result;
  }

  /**
   * Runs a text on the datasets with the given characteristics.
   * @return index 0 is true if the test was passed, index 1 is true if test 
   *         was acceptable
   */
  protected boolean[] runBasicTest(boolean nominalPredictor,
				 boolean numericPredictor, 
                                 boolean stringPredictor,
				 boolean numericClass,
				 int missingLevel,
				 boolean predictorMissing,
				 boolean classMissing,
				 int numTrain,
				 int numTest,
				 int numClasses,
				 FastVector accepts) {

    boolean[] result = new boolean[2];
    Instances train = null;
    Instances test = null;
    Classifier classifier = null;
    Evaluation evaluation = null;
    boolean built = false;
    try {
      train = makeTestDataset(42, numTrain, 
                              nominalPredictor ? 2 : 0,
                              numericPredictor ? 1 : 0, 
                              stringPredictor  ? 1 : 0,
                              numClasses, 
                              numericClass);
      test = makeTestDataset(24, numTest,
                             nominalPredictor ? 2 : 0,
                             numericPredictor ? 1 : 0, 
                             stringPredictor  ? 1 : 0,
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
      ex.printStackTrace();
      throw new Error("Error setting up for tests: " + ex.getMessage());
    }
    try {
      classifier.buildClassifier(train);
      built = true;
      if (!testWRTZeroR(classifier, evaluation, train, test)[0]) {
        result[1] = true;
	throw new Exception("Scheme performs worse than ZeroR");
      }
      
      println("yes");
      result[0] = true;
    } 
    catch (Exception ex) {
      boolean acceptable = false;
      String msg = ex.getMessage().toLowerCase();
      if (msg.indexOf("worse than zeror") >= 0) {
	println("warning: performs worse than ZeroR");
        result[1] = true;
      } else {
	for (int i = 0; i < accepts.size(); i++) {
	  if (msg.indexOf((String)accepts.elementAt(i)) >= 0) {
	    acceptable = true;
	  }
	}
	
        println("no" + (acceptable ? " (OK error message)" : ""));
        result[1] = acceptable;
      }

      if (m_Debug) {
	println("\n=== Full Report ===");
	print("Problem during");
	if (built) {
	  print(" testing");
	} else {
	  print(" training");
	}
	println(": " + ex.getMessage() + "\n");
	if (!acceptable) {
	  if (accepts.size() > 0) {
	    print("Error message doesn't mention ");
	    for (int i = 0; i < accepts.size(); i++) {
	      if (i != 0) {
		print(" or ");
	      }
	      print('"' + (String)accepts.elementAt(i) + '"');
	    }
	  }
	  println("here are the datasets:\n");
	  println("=== Train Dataset ===\n"
			     + train.toString() + "\n");
	  println("=== Test Dataset ===\n"
			     + test.toString() + "\n\n");
	}
      }
    }

    return result;
  }

  /**
   * Determine whether the scheme performs worse than ZeroR during testing
   *
   * @param classifier the pre-trained classifier
   * @param evaluation the classifier evaluation object
   * @param train the training data
   * @param test the test data
   * @return index 0 is true if the scheme performs better than ZeroR
   * @throws Exception if there was a problem during the scheme's testing
   */
  protected boolean[] testWRTZeroR(Classifier classifier,
				   Evaluation evaluation,
				   Instances train, Instances test) 
    throws Exception {
	 
    boolean[] result = new boolean[2];
    
    evaluation.evaluateModel(classifier, test);
    try {

      // Tested OK, compare with ZeroR
      Classifier zeroR = new weka.classifiers.rules.ZeroR();
      zeroR.buildClassifier(train);
      Evaluation zeroREval = new Evaluation(train);
      zeroREval.evaluateModel(zeroR, test);
      result[0] = Utils.grOrEq(zeroREval.errorRate(), evaluation.errorRate());
    } 
    catch (Exception ex) {
      throw new Error("Problem determining ZeroR performance: "
		      + ex.getMessage());
    }

    return result;
  }

  /**
   * Compare two datasets to see if they differ.
   *
   * @param data1 one set of instances
   * @param data2 the other set of instances
   * @throws Exception if the datasets differ
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
   * @param numString the number of string attributes
   * @param numClasses the number of classes (if nominal class)
   * @param numericClass true if the class attribute should be numeric
   * @return the test dataset
   * @throws Exception if the dataset couldn't be generated
   */
  protected Instances makeTestDataset(int seed, int numInstances, 
                                      int numNominal, int numNumeric, 
                                      int numString,
                                      int numClasses, boolean numericClass)
    throws Exception {

    String[] words = new String[]{"The", "quick", "brown", "fox", "jumps", "over", "the", "lazy", "dog"};
    int numAttributes = numNominal + numNumeric + numString + 1;
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

    // Add some String attributes...
    for (int i = 0; i < numString; i++) {
      attributes.addElement(new Attribute("String" + (i + 1), (FastVector) null));
    }

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
          String str = "";
          for (int n = 0; n < words.length; n++) {
            if (n > 0)
              str += " ";
            str += words[random.nextInt(words.length)];
          }
          current.setValue(j, data.attribute(j).addStringValue(str));
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
   * @param stringPredictor true if string predictor attributes are present
   * @param numericClass true if the class attribute is numeric
   */
  protected void printAttributeSummary(boolean nominalPredictor, 
                                       boolean numericPredictor, 
                                       boolean stringPredictor, 
				       boolean numericClass) {
    
    String str = "";
    
    if (numericPredictor)
      str += " numeric";

    if (nominalPredictor) {
      if (str.length() > 0)
        str += " &";
      str += " nominal";
    }
    
    if (stringPredictor) {
      if (str.length() > 0)
        str += " &";
      str += " string";
    }

    str += " predictors)";

    if (numericClass)
      str = " (numeric class," + str;
    else
      str = " (nominal class," + str;
  
    print(str);
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
        Enumeration enu = check.listOptions();
        while (enu.hasMoreElements()) {
          Option option = (Option) enu.nextElement();
          result += option.synopsis() + "\n" + option.description() + "\n";
        }
        throw new Exception(result);
      }

      check.doTests();
    } catch (Exception ex) {
      System.err.println(ex.getMessage());
    }
  }
}
