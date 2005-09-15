package weka.filters;

import weka.test.WekaTestSuite;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test class for all filters. Run from the command line with:<p/>
 * java weka.filters.AllTests
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.9.2.1 $
 */
public class AllTests 
  extends WekaTestSuite {

  public static Test suite() {
    TestSuite suite = (TestSuite) suite("weka.filters.Filter");
    
    suite.addTest(AllFilterTest.suite());
    suite.addTest(NullFilterTest.suite());

    return suite;
  }

  public static void main(String []args) {
    junit.textui.TestRunner.run(suite());
  }
}
