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
 * Tests NumericTransformFilter. Run from the command line with:<p>
 * java weka.filters.NumericTransformFilterTest
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.1 $
 */
public class NumericTransformFilterTest extends AbstractFilterTest {

  /** Tolerance allowed in double comparisons */
  private static final double TOLERANCE = 0.001;

  public NumericTransformFilterTest(String name) { super(name);  }

  /** Creates a default NumericTransformFilter */
  public Filter getFilter() {
    return getFilter("first-last");
  }

  /** Creates a specialized NumericTransformFilter */
  public Filter getFilter(String rangelist) {
    
    try {
      NumericTransformFilter af = new NumericTransformFilter();
      af.setAttributeIndices(rangelist);
      return af;
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception setting attribute range: " + rangelist 
           + "\n" + ex.getMessage()); 
    }
    return null;
  }

  public void testDefault() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
    // Check conversion is OK
    for (int i = 0; i < result.numInstances(); i++) {
      for (int j = 0; j < result.numAttributes(); j++) {
        if (m_Instances.instance(i).isMissing(j)) {
          assert(result.instance(i).isMissing(j));
        } else if (result.attribute(j).isNumeric()) {
          assertEquals("Value should be same as Math.abs()",
                       Math.abs(m_Instances.instance(i).value(j)),
                       result.instance(i).value(j), TOLERANCE);
        } else {
          assertEquals("Value shouldn't have changed",
                       m_Instances.instance(i).value(j),
                       result.instance(i).value(j), TOLERANCE);
        }
      }
    }    
  }

  public void testInverted() {
    m_Filter = getFilter("1-3");
    ((NumericTransformFilter)m_Filter).setInvertSelection(true);
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
    // Check conversion is OK
    for (int i = 0; i < result.numInstances(); i++) {
      for (int j = 0; j < result.numAttributes(); j++) {
        if (m_Instances.instance(i).isMissing(j)) {
          assert(result.instance(i).isMissing(j));
        } else if (result.attribute(j).isNumeric() && (j >=3)) {
          assertEquals("Value should be same as Math.abs()",
                       Math.abs(m_Instances.instance(i).value(j)),
                       result.instance(i).value(j), TOLERANCE);
        } else {
          assertEquals("Value shouldn't have changed",
                       m_Instances.instance(i).value(j),
                       result.instance(i).value(j), TOLERANCE);
        }
      }
    }
  }

  public void testClassNameAndMethodName() throws Exception {
    ((NumericTransformFilter)m_Filter).setClassName("java.lang.Math");
    ((NumericTransformFilter)m_Filter).setMethodName("rint");
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
    // Check conversion is OK
    for (int i = 0; i < result.numInstances(); i++) {
      for (int j = 0; j < result.numAttributes(); j++) {
        if (m_Instances.instance(i).isMissing(j)) {
          assert(result.instance(i).isMissing(j));
        } else if (result.attribute(j).isNumeric()) {
          assertEquals("Value should be same as Math.rint()",
                       Math.rint(m_Instances.instance(i).value(j)),
                       result.instance(i).value(j), TOLERANCE);
        } else {
          assertEquals("Value shouldn't have changed",
                       m_Instances.instance(i).value(j),
                       result.instance(i).value(j), TOLERANCE);
        }
      }
    }
  }

  public static Test suite() {
    return new TestSuite(NumericTransformFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
