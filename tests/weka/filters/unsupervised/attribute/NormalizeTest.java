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
 * Tests Normalize. Run from the command line with:<p>
 * java weka.filters.NormalizeTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 1.2 $
 */
public class NormalizeTest extends AbstractFilterTest {
  
  public NormalizeTest(String name) { super(name);  }

  /** Creates an example Normalize */
  public Filter getFilter() {
    Normalize f = new Normalize();
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
        for (int i = 0; i < result.numInstances(); i++) {
          if (!result.instance(i).isMissing(j)) {
            assertTrue("Value should be between 0 and 1",
                   (result.instance(i).value(j) >= 0) &&
                   (result.instance(i).value(j) <= 1));
          }
        }
      }
    }
  }


  public static Test suite() {
    return new TestSuite(NormalizeTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
