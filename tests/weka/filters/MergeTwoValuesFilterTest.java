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
 * Tests MergeTwoValuesFilter. Run from the command line with:<p>
 * java weka.filters.MergeTwoValuesFilterTest
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.2 $
 */
public class MergeTwoValuesFilterTest extends AbstractFilterTest {
  
  public MergeTwoValuesFilterTest(String name) { super(name);  }

  /** Creates an example MergeTwoValuesFilter */
  public Filter getFilter() {
    MergeTwoValuesFilter f = new MergeTwoValuesFilter();
    // Ensure the filter we return can run on the test dataset
    f.setAttributeIndex(1); 
    return f;
  }

  public void testInvalidAttributeTypes() {
    Instances icopy = new Instances(m_Instances);
    try {
      ((MergeTwoValuesFilter)m_Filter).setAttributeIndex(0);
      m_Filter.setInputFormat(icopy);
      fail("Should have thrown an exception selecting a STRING attribute!");
    } catch (Exception ex) {
      // OK
    }
    try {
      ((MergeTwoValuesFilter)m_Filter).setAttributeIndex(2);
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
    // Check that the merging is correct
    int mergedIndex = -1;
    for (int i = 0; i < result.numInstances(); i++) {
      if ((m_Instances.instance(i).value(1) == 0) || 
          (m_Instances.instance(i).value(1) == 2)) {
        if (mergedIndex == -1) {
          mergedIndex = (int)result.instance(i).value(1);
        } else {
          assertEquals("Checking merged value for instance: " + (i + 1),
                       mergedIndex, (int)result.instance(i).value(1));
        }
      }
    }
  }

  public void testFirstValueIndex() {
    ((MergeTwoValuesFilter)m_Filter).setFirstValueIndex(1);
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    // Check that the merging is correct
    int mergedIndex = -1;
    for (int i = 0; i < result.numInstances(); i++) {
      if ((m_Instances.instance(i).value(1) == 1) || 
          (m_Instances.instance(i).value(1) == 2)) {
        if (mergedIndex == -1) {
          mergedIndex = (int)result.instance(i).value(1);
        } else {
          assertEquals("Checking merged value for instance: " + (i + 1),
                       mergedIndex, (int)result.instance(i).value(1));
        }
      }
    }
  }

  public void testSecondValueIndex() {
    ((MergeTwoValuesFilter)m_Filter).setSecondValueIndex(1);
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    // Check that the merging is correct
    int mergedIndex = -1;
    for (int i = 0; i < result.numInstances(); i++) {
      if ((m_Instances.instance(i).value(1) == 0) || 
          (m_Instances.instance(i).value(1) == 1)) {
        if (mergedIndex == -1) {
          mergedIndex = (int)result.instance(i).value(1);
        } else {
          assertEquals("Checking merged value for instance: " + (i + 1),
                       mergedIndex, (int)result.instance(i).value(1));
        }
      }
    }
  }

  public void testAttributeWithMissing() {
    ((MergeTwoValuesFilter)m_Filter).setAttributeIndex(4);
    ((MergeTwoValuesFilter)m_Filter).setFirstValueIndex(0);
    ((MergeTwoValuesFilter)m_Filter).setSecondValueIndex(1);
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    // Check that the merging is correct
    int mergedIndex = -1;
    for (int i = 0; i < result.numInstances(); i++) {
      if (m_Instances.instance(i).isMissing(4)) {
        assertTrue("Missing in input should give missing in result",
               result.instance(i).isMissing(4));
      } else if ((m_Instances.instance(i).value(4) == 0) || 
                 (m_Instances.instance(i).value(4) == 1)) {
        if (mergedIndex == -1) {
          mergedIndex = (int)result.instance(i).value(4);
        } else {
          assertEquals("Checking merged value for instance: " + (i + 1),
                       mergedIndex, (int)result.instance(i).value(4));
        }
      }
    }
  }

  public static Test suite() {
    return new TestSuite(MergeTwoValuesFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
