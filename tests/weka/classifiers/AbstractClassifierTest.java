/*
 * Copyright 2000 Webmind Inc. 
 */

package weka.classifiers;

import weka.classifiers.meta.DistributionMetaClassifier;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Random;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.classifiers.evaluation.EvaluationUtils;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.test.Regression;
import weka.core.UnsupportedClassTypeException;
import weka.core.UnsupportedAttributeTypeException;
import weka.core.NoSupportForMissingValuesException;
import weka.filters.unsupervised.attribute.RemoveType;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import weka.filters.Filter;

/**
 * Abstract Test class for Classifiers.
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.5 $
 */
public abstract class AbstractClassifierTest extends TestCase {

  /** Set to true to print out extra info during testing */
  protected static boolean VERBOSE = false;

  /** The filter to be tested */
  protected Classifier m_Classifier;

  /** A set of instances to test with */
  protected Instances m_Instances;

  /** Used to generate various types of predictions */
  protected EvaluationUtils m_Evaluation;

  /**
   * Constructs the <code>AbstractClassifierTest</code>. Called by subclasses.
   *
   * @param name the name of the test class
   */
  public AbstractClassifierTest(String name) { super(name); }

  /**
   * Called by JUnit before each test method. This implementation creates
   * the default classifier to test and loads a test set of Instances.
   *
   * @exception Exception if an error occurs reading the example instances.
   */
  protected void setUp() throws Exception {
    m_Classifier = getClassifier();
    m_Evaluation = new EvaluationUtils();
    m_Instances = new Instances(new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("weka/classifiers/data/ClassifierTest.arff"))));
  }

  /** Called by JUnit after each test method */
  protected void tearDown() {
    m_Classifier = null;
    m_Evaluation = null;
    m_Instances = null;
  }

  /**
   * Used to create an instance of a specific classifier. The classifier
   * should be configured to operate on a dataset that contains
   * attributes in this order:<p>
   *
   * String, Nominal, Numeric, String, Nominal, Numeric<p>
   *
   * Where the first three attributes do not contain any missing values,
   * but the last three attributes do. If the classifier is for some reason
   * incapable of accepting a dataset of this type, override setUp() to 
   * either manipulate the default dataset to be compatible, or load another
   * test dataset. <p>
   *
   * The configured classifier should preferrably do something
   * meaningful, since the results of classification are used as the default
   * regression output.
   *
   * @return a suitably configured <code>Classifier</code> value
   */
  public abstract Classifier getClassifier();

  /**
   * Builds a model using the current classifier using the first
   * half of the current data for training, and generates a bunch of
   * predictions using the remaining half of the data for testing.
   *
   * @return a <code>FastVector</code> containing the predictions.
   */
  protected FastVector useClassifier() throws Exception {

    DistributionClassifier dc = null;
    int tot = m_Instances.numInstances();
    int mid = tot / 2;
    Instances train = null;
    Instances test = null;
    try {
      train = new Instances(m_Instances, 0, mid);
      test = new Instances(m_Instances, mid, tot - mid);
      if (m_Classifier instanceof DistributionClassifier) {
        dc = (DistributionClassifier)m_Classifier;
      } else {
        dc = new DistributionMetaClassifier(m_Classifier);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Problem setting up to use classifier: " + ex);
    }
    int counter = 0;
    do {
      try {
	return m_Evaluation.getTrainTestPredictions(dc, train, test);
      } catch (UnsupportedAttributeTypeException ex) {
	SelectedTag tag = null;
	boolean invert = false;
	String msg = ex.getMessage();
	if ((msg.indexOf("string") != -1) && 
	    (msg.indexOf("attributes") != -1)) {
	  System.err.println("\nDeleting string attributes.");
	  tag = new SelectedTag(Attribute.STRING,
				RemoveType.TAGS_ATTRIBUTETYPE);
	} else if ((msg.indexOf("only") != -1) && 
		   (msg.indexOf("nominal") != -1)) {
	  System.err.println("\nDeleting non-nominal attributes.");
	  tag = new SelectedTag(Attribute.NOMINAL,
				RemoveType.TAGS_ATTRIBUTETYPE);
	  invert = true;
	} else {
	  throw ex;
	}
	RemoveType attFilter = new RemoveType();
	attFilter.setAttributeType(tag);
	attFilter.setInvertSelection(invert);
	attFilter.setInputFormat(train);
	train = Filter.useFilter(train, attFilter);
	attFilter.batchFinished();
	test = Filter.useFilter(test, attFilter);
	counter++;
	if (counter > 2) {
	  throw ex;
	}
      } catch (NoSupportForMissingValuesException ex2) {
	System.err.println("\nReplacing missing values.");
	ReplaceMissingValues rmFilter = new ReplaceMissingValues();
	rmFilter.setInputFormat(train);
	train = Filter.useFilter(train, rmFilter);
	rmFilter.batchFinished();
	test = Filter.useFilter(test, rmFilter);
      }
    } while (true);
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
   * Returns a short summary string for the dataset characteristics
   *
   * @param nominalPredictor true if nominal predictor attributes are present
   * @param stringPredictor true if string predictor attributes are present
   * @param numericPredictor true if numeric predictor attributes are present
   * @param numericClass true if the class attribute is numeric
   */
  protected String attributeSummary(boolean nominalPredictor, 
                                    boolean stringPredictor, 
                                    boolean numericPredictor, 
                                    boolean numericClass) {
    
    StringBuffer sb = new StringBuffer();
    if (numericClass) {
      sb.append(" (numeric class,");
    } else {
      sb.append(" (nominal class,");
    }
    if (numericPredictor) {
      sb.append(" numeric");
      if (nominalPredictor || stringPredictor) {
	sb.append(" &");
      }
    }
    if (nominalPredictor) {
      sb.append(" nominal");
      if (stringPredictor) {
	sb.append(" &");
      }
    }
    if (stringPredictor) {
      sb.append(" string");
    }
    sb.append(" predictors)");
    return sb.toString();
  }

  /**
   * Returns a string containing all the predictions.
   *
   * @param predictions a <code>FastVector</code> containing the predictions
   * @return a <code>String</code> representing the vector of predictions.
   */
  protected String predictionsToString(FastVector predictions) {

    StringBuffer sb = new StringBuffer();
    sb.append(predictions.size()).append(" predictions\n");
    for (int i = 0; i < predictions.size(); i++) {
      sb.append(predictions.elementAt(i)).append('\n');
    }
    return sb.toString();
  }

  // TODO:
  // Test building with various combinations of attributes:
  // testClassMultiNominalPredictorsNone()
  // testClassMultiNominalPredictorsNominal()
  // testClassMultiNominalPredictorsNominalNumeric()
  // testClassMultiNominalPredictorsNominalNumericString()
  // testClassMultiNominalPredictorsNominalNumeric()
  // testClassBinaryPredictorsNone()
  // testClassBinaryPredictorsNominal()
  // testClassBinaryPredictorsNominalNumeric()
  // testClassBinaryPredictorsNominalNumericString()
  // testClassBinaryPredictorsNominalNumeric()
  // testClassNumericPredictorsNone()
  // testClassNumericPredictorsNominal()
  // testClassNumericPredictorsNominalNumeric()
  // testClassNumericPredictorsNominalNumericString()
  // testClassNumericPredictorsNominalNumeric()

  // With default classifier:
  // testBuildZeroTrainingInstances()
  // testBuildOneTrainingInstance()
  // testBuildMissingPredictorValues()
  // testBuildAllMissingPredictorValues()
  // testBuildMissingClassValues()
  // testBuildAllMissingClassValues()
  // testBuildInitialization() (i.e. no result changes when build called repeatedly)
  // testBuildNoTamper() Classifier doesn't alter training instances
  // Test prediction with:
  // testPredictionNoPeeking() Ignores test instance class values
  // testPredictionNoTamper() Doesn't alter test instances
  // 

  /** 
   * Tests whether the classifier doesn't barf when you call toString
   * before a model has been built. 
   */
  public void testToString_NoModel() {
    
    m_Classifier.toString();
  }

  /** 
   * Tests whether the classifier doesn't barf when you call toString
   * after a model has been built. 
   */
  public void testToString() throws Exception {
    
    try { // Try a regression model
      m_Instances.setClassIndex(2);
      useClassifier();
      m_Classifier.toString();
    } catch (UnsupportedClassTypeException ex) {
    }
    try { // Try a classification model
      m_Instances.setClassIndex(1);
      useClassifier();
      System.err.println(m_Classifier.toString());
    } catch (UnsupportedClassTypeException ex) {
    }
  }

  /**
   * Runs a regression test -- this checks that the output of the tested
   * object matches that in a reference version. When this test is
   * run without any pre-existing reference output, the reference version
   * is created.
   */
  public void testRegression() throws Exception {

    Regression reg = new Regression(this.getClass());
    FastVector resultNum = null;
    try {
      m_Instances.setClassIndex(2);
      resultNum = useClassifier();
    } catch (UnsupportedClassTypeException ex) {
    }
    FastVector resultNom = null;
    try {
      m_Instances.setClassIndex(1);
      resultNom = useClassifier();
    } catch (UnsupportedClassTypeException ex) {
    }
    if ((resultNum == null) && (resultNom == null)) {
      fail("Problem during regression testing: no successful predictions for "
           + "either numeric or nominal class");
    }
    if (resultNum != null) {
      reg.println(predictionsToString(resultNum));
    }
    if (resultNom != null) {
      reg.println(predictionsToString(resultNom));
    }
    try {
      String diff = reg.diff();
      if (diff == null) {
        System.err.println("Warning: No reference available, creating."); 
      } else if (!diff.equals("")) {
        fail("Regression test failed. Difference:\n" + diff);
      }
    } catch (java.io.IOException ex) {
      fail("Problem during regression testing.\n" + ex);
    }
  }

  /**
   * Performs some timing tests on the current classifier. This timing
   * is only carried out if VERBOSE is true. The only way this test
   * should actually fail is if there is a problem building the model
   * or generating predictions.
   *
   * @exception Exception if an error occurs.  
   */
  public void testThroughput() throws Exception {

    if (VERBOSE) {
      Instances icopy = new Instances(m_Instances);
      // Make a bigger dataset
      Instances result = null;
      for (int i = 0; i < 20000; i++) {
        icopy.add(m_Instances.instance(i%m_Instances.numInstances()));
      }
      long starttime, endtime;
      double secs, rate;

      // Time model building
      starttime = System.currentTimeMillis();
      // Build the model
      m_Classifier.buildClassifier(icopy);
      endtime = System.currentTimeMillis();
      secs = (double)(endtime - starttime) / 1000;
      System.err.println("\n" + m_Classifier.getClass().getName() 
                         + " built model from " + icopy.numInstances() 
                         + " in " + secs + " sec"); 
      
      // Time testing
      starttime = System.currentTimeMillis();
      // Generate the predictions
      for (int i = 0; i < icopy.numInstances(); i++) {
        m_Classifier.classifyInstance(icopy.instance(i));
      }
      endtime = System.currentTimeMillis();
      secs = (double)(endtime - starttime) / 1000;
      rate = (double)icopy.numInstances() / secs;
      System.err.println("\n" + m_Classifier.getClass().getName() 
                         + " made " + icopy.numInstances() 
                         + " predictions in " + secs + " sec (" 
                         + rate + " inst/sec)"); 
    }
  }
}
