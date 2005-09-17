/*
 * Copyright 2001 Malcolm Ware. 
 */

package weka.classifiers.functions;

import java.util.Random;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.*;

/**
 * Tests PaceRegression. Run from the command line with:<p>
 * java weka.classifiers.nn.PaceRegressionTest
 *
 * @author <a href="mailto:mfw4@cs.waikato.ac.nz">Malcolm Ware</a>
 * @version $Revision: 1.1.2.1 $
 */
public class PaceRegressionTest extends AbstractClassifierTest {


  public PaceRegressionTest(String name) { super(name);  }

  /**
   * Called by JUnit before each test method. This implementation creates
   * the default classifier to test and loads a test set of Instances.
   *
   * @exception Exception if an error occurs reading the example instances.
   */
  protected void setUp() throws Exception {
    super.setUp();
    m_Tester.setNumInstances(40);
  }

  /** Creates a default ThresholdSelector */
  public Classifier getClassifier() {
    return new PaceRegression();
  }

  public static Test suite() {
    return new TestSuite(PaceRegressionTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
