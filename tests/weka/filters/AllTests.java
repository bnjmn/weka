package weka.filters;

import weka.test.WekaTestSuite;

import junit.framework.Test;

/**
 * Test class for all filters. Run from the command line with:<p/>
 * java weka.filters.AllTests
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.12 $
 */
public class AllTests 
  extends WekaTestSuite {

  public static Test suite() {
    return suite("weka.filters.Filter");
  }

  public static void main(String []args) {
    junit.textui.TestRunner.run(suite());
  }
}
