/*
 * Copyright (C) 2002 University of Waikato 
 */

package weka.filters.unsupervised.attribute;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.AbstractFilterTest;

/**
 * Tests NumericToBinary. Run from the command line with:<p>
 * java weka.filters.NumericToBinaryTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 1.2 $
 */
public class NumericToBinaryTest extends AbstractFilterTest {
  
  public NumericToBinaryTest(String name) { super(name);  }

  /** Creates an example NumericToBinary */
  public Filter getFilter() {
    NumericToBinary f = new NumericToBinary();
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
    return new TestSuite(NumericToBinaryTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
