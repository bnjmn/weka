/*
 * Copyright 2002 Eibe Frank
 */

package weka.filters.unsupervised.attribute;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.AbstractFilterTest;

/**
 * Tests Normalize. Run from the command line with:<p>
 * java weka.filters.NormalizeTest
 *
 * @author <a href="mailto:len@reeltwo.com">Eibe Frank</a>
 * @version $Revision: 1.2 $
 */
public class StandardizeTest extends AbstractFilterTest {
  
  public StandardizeTest(String name) { super(name);  }

  /** Creates an example Standardize */
  public Filter getFilter() {
    Standardize f = new Standardize();
    return f;
  }

  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());

    // Check conversion is OK
    for (int j = 0; j < result.numAttributes(); j++) {
      if (result.attribute(j).isNumeric()) {
	double mean = result.meanOrMode(j);
	assertTrue("Mean should be 0", Utils.eq(mean, 0));
	double stdDev = Math.sqrt(result.variance(j));
	assertTrue("StdDev should be 1 (or 0)", 
		   Utils.eq(stdDev, 0) || Utils.eq(stdDev, 1));
      }
    }
  }


  public static Test suite() {
    return new TestSuite(StandardizeTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
