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
 * Tests InstanceFilter. Run from the command line with:<p>
 * java weka.filters.InstanceFilterTest
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.2 $
 */
public class InstanceFilterTest extends AbstractFilterTest {
  
  public InstanceFilterTest(String name) { super(name);  }

  /** Creates a default InstanceFilter */
  public Filter getFilter() {
    InstanceFilter f = new InstanceFilter();
    f.setAttributeIndex(2);
    return f;
  }

  public void testString() {
    Instances icopy = new Instances(m_Instances);
    try {
      ((InstanceFilter)m_Filter).setAttributeIndex(0);
      m_Filter.setInputFormat(icopy);
      fail("Should have thrown an exception selecting on a STRING attribute!");
    } catch (Exception ex) {
      // OK
    }
  }

  public void testNominal() {
    ((InstanceFilter)m_Filter).setAttributeIndex(1);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals("Default nominal selection matches all values",
                 m_Instances.numInstances(),  result.numInstances());

    try {
      ((InstanceFilter)m_Filter).setNominalIndices("1-2");
    } catch (Exception ex) {
      fail("Shouldn't ever get here unless Range chamges incompatibly");
    }
    result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assert(m_Instances.numInstances() > result.numInstances());

    try {
      ((InstanceFilter)m_Filter).setNominalIndices("3-last");
    } catch (Exception ex) {
      fail("Shouldn't ever get here unless Range chamges incompatibly");
    }
    Instances result2 = useFilter();
    assertEquals(m_Instances.numAttributes(), result2.numAttributes());
    assert(m_Instances.numInstances() > result2.numInstances());
    assertEquals(m_Instances.numInstances(), result.numInstances() + result2.numInstances());

    ((InstanceFilter)m_Filter).setInvertSelection(true);
    result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances() + result2.numInstances());
  }

  public void testNumeric() {
    ((InstanceFilter)m_Filter).setAttributeIndex(2);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals("Default split point matches values less than 0",
                 0,  result.numInstances());

    ((InstanceFilter)m_Filter).setSplitPoint(3);
    result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assert(m_Instances.numInstances() > result.numInstances());

    // Test inversion is working.
    ((InstanceFilter)m_Filter).setInvertSelection(true);
    Instances result2 = useFilter();
    assertEquals(m_Instances.numAttributes(), result2.numAttributes());
    assert(m_Instances.numInstances() > result2.numInstances());
    assertEquals(m_Instances.numInstances(), result.numInstances() + result2.numInstances());
  }

  public void testMatchMissingValues() {
    ((InstanceFilter)m_Filter).setAttributeIndex(4);
    ((InstanceFilter)m_Filter).setInvertSelection(true);
    ((InstanceFilter)m_Filter).setMatchMissingValues(true);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assert(result.numInstances() > 0);
    for (int i = 0; i < result.numInstances(); i++) {
      assert("Should select only instances with missing values",
             result.instance(i).isMissing(4));
    }
  }

  public static Test suite() {
    return new TestSuite(InstanceFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
