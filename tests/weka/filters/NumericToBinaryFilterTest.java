/*
 * Copyright 2000 Webmind Inc. 
 */

package weka.filters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Tests NumericToBinaryFilter. Run from the command line with:<p>
 * java weka.filters.NumericToBinaryFilterTest
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.2 $
 */
public class NumericToBinaryFilterTest extends AbstractFilterTest {
  
  public NumericToBinaryFilterTest(String name) { super(name);  }

  /** Creates an example NumericToBinaryFilter */
  public Filter getFilter() {
    NumericToBinaryFilter f = new NumericToBinaryFilter();
    return f;
  }

  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());

    // Check conversion is OK
    for (int j = 0; j < result.numAttributes(); j++) {
      if (m_Instances.attribute(j).isNumeric()) {
        assertTrue("Numeric attribute should now be nominal",
               result.attribute(j).isNominal());
        for (int i = 0; i < result.numInstances(); i++) {
          if (m_Instances.instance(i).isMissing(j)) {
            assertTrue(result.instance(i).isMissing(j));
          } else if (m_Instances.instance(i).value(j) == 0) {
            assertTrue("Output value should be 0", 
                   result.instance(i).value(j) == 0);
          } else {
            assertTrue("Output value should be 1", 
                   result.instance(i).value(j) == 1);
          }
        }
      }
    }
  }


  public static Test suite() {
    return new TestSuite(NumericToBinaryFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
