/*
 * Copyright 2001 Malcolm Ware. 
 */

package weka.classifiers.functions;

import weka.classifiers.AbstractClassifierTest;
import weka.classifiers.CheckClassifier;
import weka.classifiers.Classifier;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests PaceRegression. Run from the command line with:<p>
 * java weka.classifiers.nn.PaceRegressionTest
 *
 * @author <a href="mailto:mfw4@cs.waikato.ac.nz">Malcolm Ware</a>
 * @version $Revision: 1.3 $
 */
public class PaceRegressionTest 
  extends AbstractClassifierTest {

  public PaceRegressionTest(String name) { 
    super(name);  
  }

  /**
   * configures the CheckClassifier instance used throughout the tests
   * 
   * @return	the fully configured CheckClassifier instance used for testing
   */
  protected CheckClassifier getTester() {
    CheckClassifier 	result;
    
    result = super.getTester();
    result.setNumInstances(60);
    
    return result;
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
