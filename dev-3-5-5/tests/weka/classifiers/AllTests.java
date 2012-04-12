package weka.classifiers;

import weka.test.WekaTestSuite;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test class for all classifiers. Run from the command line with: <p/>
 * java weka.classifiers.AllTests
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @author FracPete (frapcete at waikato dot ac dot nz)
 * @version $Revision: 1.13 $
 */
public class AllTests 
  extends WekaTestSuite {

  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(suite("weka.classifiers.Classifier"));
    suite.addTest(suite("weka.classifiers.functions.supportVector.Kernel"));

    return suite;
  }

  public static void main(String []args) {
    junit.textui.TestRunner.run(suite());
  }
}
