package weka;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;         

/**
 * Test class for all tests in this directory. Run from the command line 
 * with:<p>
 * java weka.AllTests
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.6 $
 */
public class AllTests extends TestSuite {

  public static Test suite() {
    TestSuite suite = new TestSuite();

    // Core components
    suite.addTest(weka.core.AllTests.suite());

    // associators
    suite.addTest(weka.associations.AllTests.suite());

    // attribute selection
    suite.addTest(weka.attributeSelection.AllTests.suite());

    // classifiers
    suite.addTest(weka.classifiers.AllTests.suite());

    // clusterers
    suite.addTest(weka.clusterers.AllTests.suite());

    // data generators
    suite.addTest(weka.datagenerators.AllTests.suite());

    // estimators
    //suite.addTest(weka.estimators.AllTests.suite());

    // filters
    suite.addTest(weka.filters.AllTests.suite());

    // High level applications
    //suite.addTest(weka.experiment.AllTests.suite());
    //suite.addTest(weka.gui.AllTests.suite());

    return suite;
  }

  public static void main(String []args) {
    junit.textui.TestRunner.run(suite());
  }
}
