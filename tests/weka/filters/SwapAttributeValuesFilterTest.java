/*
 * Copyright 2000 Webmind Inc. 
 */

package weka.filters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Attribute;

/**
 * Tests SwapAttributeValuesFilter. Run from the command line with:<p>
 * java weka.filters.SwapAttributeValuesFilterTest
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.2 $
 */
public class SwapAttributeValuesFilterTest extends AbstractFilterTest {
  
  public SwapAttributeValuesFilterTest(String name) { super(name);  }

  /** Creates an example SwapAttributeValuesFilter */
  public Filter getFilter() {
    SwapAttributeValuesFilter f = new SwapAttributeValuesFilter();
    // Ensure the filter we return can run on the test dataset
    f.setAttributeIndex(1); 
    return f;
  }

  public void testInvalidAttributeTypes() {
    Instances icopy = new Instances(m_Instances);
    try {
      ((SwapAttributeValuesFilter)m_Filter).setAttributeIndex(0);
      m_Filter.setInputFormat(icopy);
      fail("Should have thrown an exception selecting a STRING attribute!");
    } catch (Exception ex) {
      // OK
    }
    try {
      ((SwapAttributeValuesFilter)m_Filter).setAttributeIndex(2);
      m_Filter.setInputFormat(icopy);
      fail("Should have thrown an exception indicating a NUMERIC attribute!");
    } catch (Exception ex) {
      // OK
    }
  }

  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    // Check that the swapping is correct
    int first = 0, second = 2;
    for (int i = 0; i < result.numInstances(); i++) {
      if (m_Instances.instance(i).value(1) == first) {
        assertTrue("Value should be swapped", result.instance(i).value(1) == second);
      } else if (m_Instances.instance(i).value(1) == second) {
        assertTrue("Value should be swapped", result.instance(i).value(1) == first);
      }
    }
  }

  public void testFirstValueIndex() {
    ((SwapAttributeValuesFilter)m_Filter).setFirstValueIndex(1);
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    // Check that the swapping is correct
    int first = 1, second = 2;
    for (int i = 0; i < result.numInstances(); i++) {
      if (m_Instances.instance(i).value(1) == first) {
        assertTrue("Value should be swapped", result.instance(i).value(1) == second);
      } else if (m_Instances.instance(i).value(1) == second) {
        assertTrue("Value should be swapped", result.instance(i).value(1) == first);
      }
    }
  }

  public void testSecondValueIndex() {
    ((SwapAttributeValuesFilter)m_Filter).setSecondValueIndex(1);
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    // Check that the swapping is correct
    int first = 0, second = 1;
    for (int i = 0; i < result.numInstances(); i++) {
      if (m_Instances.instance(i).value(1) == first) {
        assertTrue("Value should be swapped", result.instance(i).value(1) == second);
      } else if (m_Instances.instance(i).value(1) == second) {
        assertTrue("Value should be swapped", result.instance(i).value(1) == first);
      }
    }
  }

  public void testAttributeWithMissing() {
    ((SwapAttributeValuesFilter)m_Filter).setAttributeIndex(4);
    ((SwapAttributeValuesFilter)m_Filter).setFirstValueIndex(0);
    ((SwapAttributeValuesFilter)m_Filter).setSecondValueIndex(1);
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    // Check that the swapping is correct
    int first = 0, second = 1;
    for (int i = 0; i < result.numInstances(); i++) {
      if (m_Instances.instance(i).isMissing(4)) {
        assertTrue("Missing in input should give missing in result:" 
               + m_Instances.instance(i) + " --> "
               + result.instance(i),
               result.instance(i).isMissing(4));
      } else if (m_Instances.instance(i).value(4) == first) {
        assertTrue("Value should be swapped", result.instance(i).value(4) == second);
      } else if (m_Instances.instance(i).value(4) == second) {
        assertTrue("Value should be swapped", result.instance(i).value(4) == first);
      }
    }
  }

  public static Test suite() {
    return new TestSuite(SwapAttributeValuesFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
