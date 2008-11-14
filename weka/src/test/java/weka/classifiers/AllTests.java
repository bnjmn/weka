package weka.classifiers;

import weka.test.WekaTestSuite;

import junit.framework.Test;

/**
 * Test class for all classifiers. Run from the command line with: <p/>
 * java weka.classifiers.AllTests
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @author FracPete (frapcete at waikato dot ac dot nz)
 * @version $Revision: 1.11.2.1 $
 */
public class AllTests 
  extends WekaTestSuite {

  public static Test suite() {
    return suite("weka.classifiers.Classifier");
  }

  public static void main(String []args) {
    junit.textui.TestRunner.run(suite());
  }
}
