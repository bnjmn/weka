/*
 * Copyright 2000 Webmind Inc. 
 */

package weka.filters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;
import weka.core.Instance;

/**
 * Tests FirstOrderFilter. Run from the command line with:<p>
 * java weka.filters.FirstOrderFilterTest
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.4 $
 */
public class FirstOrderFilterTest extends AbstractFilterTest {
  
  private static double EXPR_DELTA = 0.001;

  public FirstOrderFilterTest(String name) { super(name);  }

  /** Creates a default FirstOrderFilter */
  public Filter getFilter() {
    return getFilter("6,3");
  }

  /** Creates a specialized FirstOrderFilter */
  public Filter getFilter(String rangelist) {
    
    try {
      FirstOrderFilter af = new FirstOrderFilter();
      af.setAttributeIndices(rangelist);
      return af;
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception setting attribute range: " + rangelist 
           + "\n" + ex.getMessage()); 
    }
    return null;
  }

  public void testTypical() {
    m_Filter = getFilter("6,3");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes() - 1, result.numAttributes());
    for (int i = 0; i < result.numInstances(); i++) {
      Instance orig = m_Instances.instance(i);
      if (orig.isMissing(5) || orig.isMissing(2)) {
        assertTrue("Instance " + (i + 1) + " should have been ?" , 
               result.instance(i).isMissing(4));
      } else {
        assertEquals(orig.value(5) - orig.value(2), 
                     result.instance(i).value(4), 
                     EXPR_DELTA);
      }
    }
  }

  public void testTypical2() {
    m_Filter = getFilter("3,6");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes() - 1, result.numAttributes());
    for (int i = 0; i < result.numInstances(); i++) {
      Instance orig = m_Instances.instance(i);
      if (orig.isMissing(5) || orig.isMissing(2)) {
        assertTrue("Instance " + (i + 1) + " should have been ?" , 
               result.instance(i).isMissing(4));
      } else {
        assertEquals(orig.value(5) - orig.value(2), 
                     result.instance(i).value(4), 
                     EXPR_DELTA);
      }
    }
  }

  public static Test suite() {
    return new TestSuite(FirstOrderFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
