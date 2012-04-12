/*
 * Copyright (C) 2002 University of Waikato 
 */

package weka.classifiers;

import junit.framework.TestCase;

/**
 * Abstract Test class for Classifiers. Internally it uses the class
 * <code>CheckClassifier</code> to determine success or failure of the
 * tests. It follows basically the <code>testsPerClassType</code> method.
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.10 $
 *
 * @see CheckClassifier
 * @see CheckClassifier#testsPerClassType(boolean,boolean,boolean)
 */
public abstract class AbstractClassifierTest 
  extends TestCase {

  /**
   * Class that performs the actual testing. Only publishes the necessary 
   * protected methods of the <code>CheckClassifier</code> class.
   */
  protected class TestClassifier
    extends CheckClassifier {

    /**
     * Checks whether the scheme can take command line options.
     *
     * @return index 0 is true if the classifier can take options
     */
    public boolean[] canTakeOptions() {
      return super.canTakeOptions();
    }

    /**
     * Checks whether the scheme can build models incrementally.
     *
     * @return index 0 is true if the classifier can train incrementally
     */
    public boolean[] updateableClassifier() {
      return super.updateableClassifier();
    }

    /**
     * Checks whether the scheme says it can handle instance weights.
     *
     * @return true if the classifier handles instance weights
     */
    public boolean[] weightedInstancesHandler() {
      return super.weightedInstancesHandler();
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
    public boolean[] canPredict(boolean nominalPredictor,
                                boolean numericPredictor, 
                                boolean stringPredictor, 
                                boolean numericClass) {
      return super.canPredict(
          nominalPredictor, numericPredictor, stringPredictor, numericClass);
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
    public boolean[] canHandleNClasses(boolean nominalPredictor,
                                       boolean numericPredictor, 
                                       boolean stringPredictor, 
                                       int numClasses) {
      return super.canHandleNClasses(
          nominalPredictor, numericPredictor, stringPredictor, numClasses);
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
    public boolean[] canHandleZeroTraining(boolean nominalPredictor,
                                           boolean numericPredictor, 
                                           boolean stringPredictor, 
                                           boolean numericClass) {
      return super.canHandleZeroTraining(
          nominalPredictor, numericPredictor, stringPredictor, numericClass);
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
    public boolean[] correctBuildInitialisation(boolean nominalPredictor,
                                                boolean numericPredictor, 
                                                boolean stringPredictor, 
                                                boolean numericClass) {
      return super.correctBuildInitialisation(
          nominalPredictor, numericPredictor, stringPredictor, numericClass);
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
    public boolean[] canHandleMissing(boolean nominalPredictor,
                                      boolean numericPredictor, 
                                      boolean stringPredictor, 
                                      boolean numericClass,
                                      boolean predictorMissing,
                                      boolean classMissing,
                                      int missingLevel) {
      return super.canHandleMissing(
          nominalPredictor, numericPredictor, stringPredictor, 
          numericClass, predictorMissing,
          classMissing, missingLevel);
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
    public boolean[] updatingEquality(boolean nominalPredictor,
                                      boolean numericPredictor, 
                                      boolean stringPredictor, 
                                      boolean numericClass) {
      return super.updatingEquality(
          nominalPredictor, numericPredictor, stringPredictor, numericClass);
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
    public boolean[] doesntUseTestClassVal(boolean nominalPredictor,
                                           boolean numericPredictor, 
                                           boolean stringPredictor, 
                                           boolean numericClass) {
      return super.doesntUseTestClassVal(
          nominalPredictor, numericPredictor, stringPredictor, numericClass);
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
    public boolean[] instanceWeights(boolean nominalPredictor,
                                     boolean numericPredictor, 
                                     boolean stringPredictor, 
                                     boolean numericClass) {
      return super.instanceWeights(
          nominalPredictor, numericPredictor, stringPredictor, numericClass);
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
    public boolean[] datasetIntegrity(boolean nominalPredictor,
                                      boolean numericPredictor, 
                                      boolean stringPredictor, 
                                      boolean numericClass,
                                      boolean predictorMissing,
                                      boolean classMissing) {
      return super.datasetIntegrity(
          nominalPredictor, numericPredictor, stringPredictor, 
          numericClass, predictorMissing,
          classMissing);
    }
  }

  /** The classifier to be tested */
  protected Classifier m_Classifier;

  /** For testing the classifier */
  protected TestClassifier m_Tester;
  
  /** whether classifier is updateable */
  protected boolean m_updateableClassifier;

  /** whether classifier handles weighted instances */
  protected boolean m_weightedInstancesHandler;

  /** the number of classes to test with testNClasses() 
   * @see #testNClasses() */
  protected int m_NClasses;

  /** used as index for boolean arrays with capabilities */
  protected final static int NOMINAL = 0;

  /** used as index for boolean arrays with capabilities */
  protected final static int NUMERIC = 1;

  /** whether to run CheckClassifier in DEBUG mode */
  protected boolean DEBUG = false;
  
  /** wether classifier can predict nominal  */
  protected boolean[] m_canPredictNominal;
  protected boolean[] m_canPredictNumeric;
  protected boolean[] m_canPredictString;
  
  /** whether classifier handles missing values */
  protected boolean[] m_handleMissingPredictors;

  /** whether classifier handles class with only missing values */
  protected boolean[] m_handleMissingClass;
  
  /**
   * Constructs the <code>AbstractClassifierTest</code>. Called by subclasses.
   *
   * @param name the name of the test class
   */
  public AbstractClassifierTest(String name) { 
    super(name); 
  }

  /**
   * Called by JUnit before each test method. This implementation creates
   * the default classifier to test and loads a test set of Instances.
   *
   * @exception Exception if an error occurs reading the example instances.
   */
  protected void setUp() throws Exception {
    m_Classifier = getClassifier();
    m_Tester     = new TestClassifier();
    m_Tester.setSilent(true);
    m_Tester.setClassifier(m_Classifier);
    m_Tester.setNumInstances(20);
    m_Tester.setDebug(DEBUG);

    m_updateableClassifier     = m_Tester.updateableClassifier()[0];
    m_weightedInstancesHandler = m_Tester.weightedInstancesHandler()[0];
    m_canPredictNominal        = new boolean[2];
    m_canPredictNumeric        = new boolean[2];
    m_canPredictString         = new boolean[2];
    m_handleMissingPredictors  = new boolean[2];
    m_handleMissingClass       = new boolean[2];
    m_NClasses                 = 4;

    // initialize attributes
    checkAttributes(true, false, false, false);
    checkAttributes(false, true, false, false);
    checkAttributes(false, false, true, false);
  }

  /** Called by JUnit after each test method */
  protected void tearDown() {
    m_Classifier = null;
    m_Tester     = null;

    m_updateableClassifier     = false;
    m_weightedInstancesHandler = false;
    m_canPredictNominal        = new boolean[2];
    m_canPredictNumeric        = new boolean[2];
    m_canPredictString         = new boolean[2];
    m_handleMissingPredictors  = new boolean[2];
    m_handleMissingClass       = new boolean[2];
    m_NClasses                 = 4;
  }

  /**
   * Used to create an instance of a specific classifier.
   *
   * @return a suitably configured <code>Classifier</code> value
   */
  public abstract Classifier getClassifier();

  /**
   * checks whether at least one attribute type can be handled with the
   * given class type
   *
   * @param type      the class type to check for, NOMINAL or NUMERIC
   */
  protected boolean canPredict(int type) {
    return    m_canPredictNominal[type]
           || m_canPredictNumeric[type]
           || m_canPredictString[type];
  }

  /** 
   * returns a string for the class type
   */
  protected String getClassTypeString(int type) {
    if (type == NOMINAL)
      return "Nominal";
    else if (type == NUMERIC)
      return "Numeric";
    else
      throw new IllegalArgumentException("Class type '" + type + "' unknown!");
  }

  /**
   * tests whether the classifier can handle certain attributes and if not,
   * if the exception is OK
   *
   * @param nom         to check for nominal attributes
   * @param num         to check for numeric attributes
   * @param str         to check for string attributes
   * @param allowFail   whether a junit fail can be executed
   * @see CheckClassifier#canPredict(boolean,boolean,boolean,boolean)
   * @see CheckClassifier#testsPerClassType(boolean,boolean,boolean)
   */
  protected void checkAttributes(boolean nom, boolean num, boolean str, 
                                 boolean allowFail) {
    boolean[]     result;
    String        att;
    int           i;

    // determine text for type of attributes
    att = "";
    if (nom)
      att = "nominal";
    else if (num)
      att = "numeric";
    else if (str)
      att = "string";
    
    for (i = NOMINAL; i <= NUMERIC; i++) {
      result = m_Tester.canPredict(nom, num, str, (i == NUMERIC));
      if (nom)
        m_canPredictNominal[i] = result[0];
      else if (num)
        m_canPredictNumeric[i] = result[0];
      else if (str)
        m_canPredictString[i] = result[0];

      if (!result[0] && !result[1] && allowFail)
        fail("Error handling " + att + " attributes (" + getClassTypeString(i) 
            + " class)!");
    }
  }

  /**
   * tests whether the classifier can handle different types of attributes and
   * if not, if the exception is OK
   *
   * @see #checkAttributes(boolean,boolean,boolean)
   */
  public void testAttributes() {
    // nominal
    checkAttributes(true, false, false, true);
    // numeric
    checkAttributes(false, true, false, true);
    // string
    checkAttributes(false, false, true, true);
  }

  /**
   * tests whether the classifier handles instance weights correctly
   *
   * @see CheckClassifier#instanceWeights(boolean,boolean,boolean,boolean)
   * @see CheckClassifier#testsPerClassType(boolean,boolean,boolean)
   */
  public void testInstanceWeights() {
    boolean[]     result;
    int           i;
    
    if (m_weightedInstancesHandler) {
      for (i = NOMINAL; i <= NUMERIC; i++) {
        // does the classifier support this type of class at all?
        if (!canPredict(i))
          continue;
        
        result = m_Tester.instanceWeights(
            m_canPredictNominal[i], 
            m_canPredictNumeric[i], 
            m_canPredictString[i], 
            (i == NUMERIC));

        if (!result[0])
          System.err.println("Error handling instance weights (" + getClassTypeString(i) 
              + " class)!");
      }
    }
  }

  /**
   * tests whether classifier handles N classes
   *
   * @see CheckClassifier#canHandleNClasses(boolean,boolean,boolean,int)
   * @see CheckClassifier#testsPerClassType(boolean,boolean,boolean)
   * @see #m_NClasses
   */
  public void testNClasses() {
    boolean[]     result;

    if (!canPredict(NOMINAL))
      return;

    result = m_Tester.canHandleNClasses(
        m_canPredictNominal[NOMINAL],
        m_canPredictNumeric[NOMINAL],
        m_canPredictString[NOMINAL],
        m_NClasses);

    if (!result[0] && !result[1])
      fail("Error handling " + m_NClasses + " classes!");
  }

  /**
   * tests whether the classifier can handle zero training instances
   *
   * @see CheckClassifier#canHandleZeroTraining(boolean,boolean,boolean,boolean)
   * @see CheckClassifier#testsPerClassType(boolean,boolean,boolean)
   */
  public void testZeroTraining() {
    boolean[]     result;
    int           i;
    
    for (i = NOMINAL; i <= NUMERIC; i++) {
      // does the classifier support this type of class at all?
      if (!canPredict(i))
        continue;
      
      result = m_Tester.canHandleZeroTraining(
          m_canPredictNominal[i], 
          m_canPredictNumeric[i], 
          m_canPredictString[i], 
          (i == NUMERIC));

      if (!result[0] && !result[1])
        fail("Error handling zero training instances (" + getClassTypeString(i) 
            + " class)!");
    }
  }

  /**
   * checks whether the classifier can handle the given percentage of
   * missing predictors
   *
   * @param type        the class type
   * @param percent     the percentage of missing predictors
   * @return            true if the classifier can handle it
   */
  protected boolean checkMissingPredictors(int type, int percent) {
    boolean[]     result;
    
    result = m_Tester.canHandleMissing(
        m_canPredictNominal[type], 
        m_canPredictNumeric[type], 
        m_canPredictString[type], 
        (type == NUMERIC),
        true,
        false,
        percent);

    if (!result[0] && !result[1])
      fail("Error handling " + percent + "% missing predictors (" 
          + getClassTypeString(type) + " class)!");
    
    return result[0];
  }

  /**
   * tests whether the classifier can handle missing predictors (20% and 100%)
   *
   * @see CheckClassifier#canHandleMissing(boolean,boolean,boolean,boolean,boolean,boolean,int)
   * @see CheckClassifier#testsPerClassType(boolean,boolean,boolean)
   */
  public void testMissingPredictors() {
    int           i;
    
    for (i = NOMINAL; i <= NUMERIC; i++) {
      // does the classifier support this type of class at all?
      if (!canPredict(i))
        continue;
      
      // 20% missing
      m_handleMissingPredictors[i] = checkMissingPredictors(i, 20);

      // 100% missing
      if (m_handleMissingPredictors[i])
        checkMissingPredictors(i, 100);
    }
  }

  /**
   * checks whether the classifier can handle the given percentage of
   * missing class labels
   *
   * @param type        the class type
   * @param percent     the percentage of missing class labels
   * @return            true if the classifier can handle it
   */
  protected boolean checkMissingClass(int type, int percent) {
    boolean[]     result;
    
    result = m_Tester.canHandleMissing(
        m_canPredictNominal[type], 
        m_canPredictNumeric[type], 
        m_canPredictString[type], 
        (type == NUMERIC),
        false,
        true,
        percent);

    if (!result[0] && !result[1])
      fail("Error handling " + percent + "% missing class labels (" 
          + getClassTypeString(type) + " class)!");
    
    return result[0];
  }

  /**
   * tests whether the classifier can handle missing class values (20% and
   * 100%)
   *
   * @see CheckClassifier#canHandleMissing(boolean,boolean,boolean,boolean,boolean,boolean,int)
   * @see CheckClassifier#testsPerClassType(boolean,boolean,boolean)
   */
  public void testMissingClass() {
    int           i;
    
    for (i = NOMINAL; i <= NUMERIC; i++) {
      // does the classifier support this type of class at all?
      if (!canPredict(i))
        continue;
      
      // 20% missing
      m_handleMissingClass[i] = checkMissingClass(i, 20);

      // 100% missing
      if (m_handleMissingClass[i])
        checkMissingClass(i, 100);
    }
  }

  /**
   * tests whether the classifier correctly initializes in the
   * buildClassifier method
   *
   * @see CheckClassifier#correctBuildInitialisation(boolean,boolean,boolean,boolean)
   * @see CheckClassifier#testsPerClassType(boolean,boolean,boolean)
   */
  public void testBuildInitialization() {
    boolean[]     result;
    int           i;
    
    for (i = NOMINAL; i <= NUMERIC; i++) {
      // does the classifier support this type of class at all?
      if (!canPredict(i))
        continue;
      
      result = m_Tester.correctBuildInitialisation(
          m_canPredictNominal[i], 
          m_canPredictNumeric[i], 
          m_canPredictString[i], 
          (i == NUMERIC));

      if (!result[0] && !result[1])
        fail("Incorrect build initialization (" + getClassTypeString(i) 
            + " class)!");
    }
  }

  /**
   * tests whether the classifier alters the training set during training.
   *
   * @see CheckClassifier#datasetIntegrity(boolean,boolean,boolean,boolean,boolean,boolean)
   * @see CheckClassifier#testsPerClassType(boolean,boolean,boolean)
   */
  public void testDatasetIntegrity() {
    boolean[]     result;
    int           i;
    
    for (i = NOMINAL; i <= NUMERIC; i++) {
      // does the classifier support this type of class at all?
      if (!canPredict(i))
        continue;
      
      result = m_Tester.datasetIntegrity(
          m_canPredictNominal[i], 
          m_canPredictNumeric[i], 
          m_canPredictString[i], 
          (i == NUMERIC),
          m_handleMissingPredictors[i],
          m_handleMissingClass[i]);

      if (!result[0] && !result[1])
        fail("Training set is altered during training (" 
            + getClassTypeString(i) + " class)!");
    }
  }

  /**
   * tests whether the classifier erroneously uses the class value of test
   * instances (if provided)
   *
   * @see CheckClassifier#doesntUseTestClassVal(boolean,boolean,boolean,boolean)
   * @see CheckClassifier#testsPerClassType(boolean,boolean,boolean)
   */
  public void testUseOfTestClassValue() {
    boolean[]     result;
    int           i;
    
    for (i = NOMINAL; i <= NUMERIC; i++) {
      // does the classifier support this type of class at all?
      if (!canPredict(i))
        continue;
      
      result = m_Tester.datasetIntegrity(
          m_canPredictNominal[i], 
          m_canPredictNumeric[i], 
          m_canPredictString[i], 
          (i == NUMERIC),
          m_handleMissingPredictors[i],
          m_handleMissingClass[i]);

      if (!result[0])
        fail("Uses test class values (" + getClassTypeString(i) + " class)!");
    }
  }

  /**
   * tests whether the classifier produces the same model when trained
   * incrementally as when batch trained.
   *
   * @see CheckClassifier#updatingEquality(boolean,boolean,boolean,boolean)
   * @see CheckClassifier#testsPerClassType(boolean,boolean,boolean)
   */
  public void testUpdatingEquality() {
    boolean[]     result;
    int           i;
    
    if (m_updateableClassifier) {
      for (i = NOMINAL; i <= NUMERIC; i++) {
        // does the classifier support this type of class at all?
        if (!canPredict(i))
          continue;
        
        result = m_Tester.updatingEquality(
            m_canPredictNominal[i], 
            m_canPredictNumeric[i], 
            m_canPredictString[i], 
            (i == NUMERIC));

        if (!result[0])
          System.err.println("Incremental training does not produce same result as "
              + "batch training (" + getClassTypeString(i) + " class)!");
      }
    }
  }
}
