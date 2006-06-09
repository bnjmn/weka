/*
 * Copyright 2001 Malcolm Ware. 
 */

package weka.classifiers.functions;

import weka.classifiers.AbstractClassifierTest;
import weka.classifiers.CheckClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.CheckClassifier.PostProcessor;
import weka.core.Instances;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests PaceRegression. Run from the command line with:<p>
 * java weka.classifiers.nn.PaceRegressionTest
 *
 * @author <a href="mailto:mfw4@cs.waikato.ac.nz">Malcolm Ware</a>
 * @version $Revision: 1.1.2.2 $
 */
public class PaceRegressionTest extends AbstractClassifierTest {
  
  /** 
   * a class for postprocessing the test-data: all non-binary nominal attributes
   * are deleted from the dataset.
   * 
   * @author  FracPete (fracpete at waikato dot ac dot nz)
   * @version $Revision: 1.1.2.2 $
   */
  public static class BinaryPostProcessor 
    extends PostProcessor {
    
    /**
     * initializes the PostProcessor
     */
    public BinaryPostProcessor() {
      super();
    }
    
    /**
     * Provides a hook for derived classes to further modify the data. Deletes
     * all non-binary nominal attributes.
     * 
     * @param data	the data to process
     * @return		the processed data
     */
    public Instances process(Instances data) {
      Instances	result;
      int		i;
      
      result = new Instances(super.process(data));
      
      i = 0;
      while (i < result.numAttributes()) {
        if (result.attribute(i).isNominal() && (result.attribute(i).numValues() != 2))
  	result.deleteAttributeAt(i);
        else
  	i++;
      }
      
      return result;
    }
  }

  public PaceRegressionTest(String name) { 
    super(name);
  }

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

  /**
   * configures the CheckClassifier instance used throughout the tests
   * 
   * @return	the fully configured CheckClassifier instance used for testing
   */
  protected CheckClassifier getTester() {
    CheckClassifier 	result;
    
    result = super.getTester();
    result.setNumInstances(60);
    result.setPostProcessor(new BinaryPostProcessor());
    
    return result;
  }
  
  /**
   * Provides a hook for derived classes to further modify the data. Deletes
   * all non-binary nominal attributes.
   * 
   * @param data	the data to process
   * @return		the processed data
   */
  protected Instances process(Instances data) {
    return m_Tester.getPostProcessor().process(data);
  }

  public static Test suite() {
    return new TestSuite(PaceRegressionTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
