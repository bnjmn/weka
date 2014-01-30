package weka.classifiers.mlr;

import weka.classifiers.AbstractClassifierTest;
import weka.classifiers.Classifier;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests MLRClassifier. Run from the command line with:<p>
 * java weka.classifiers.functions.MLRClassifierTest
 *
 * @author Mark Hall
 * @version $Revision$
 */
public class MLRClassifierTest extends AbstractClassifierTest {

  public MLRClassifierTest(String name) { super(name);  }

  /** Creates a default MLRClassifier */
  public Classifier getClassifier() {
    MLRClassifier p = new MLRClassifier();
    return p;
  }

  public static Test suite() {
    return new TestSuite(MLRClassifierTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
