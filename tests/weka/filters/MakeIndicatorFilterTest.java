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
 * Tests MakeIndicatorFilter. Run from the command line with:<p>
 * java weka.filters.MakeIndicatorFilterTest
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.3 $
 */
public class MakeIndicatorFilterTest extends AbstractFilterTest {
  
  public MakeIndicatorFilterTest(String name) { super(name);  }

  /** Creates an example MakeIndicatorFilter */
  public Filter getFilter() {
    MakeIndicatorFilter f = new MakeIndicatorFilter();
    // Ensure the filter we return can run on the test dataset
    f.setAttributeIndex(1); 
    return f;
  }


  public void testInvalidAttributeTypes() {
    Instances icopy = new Instances(m_Instances);
    try {
      ((MakeIndicatorFilter)m_Filter).setAttributeIndex(0);
      m_Filter.setInputFormat(icopy);
      fail("Should have thrown an exception selecting a STRING attribute!");
    } catch (Exception ex) {
      // OK
    }
    try {
      ((MakeIndicatorFilter)m_Filter).setAttributeIndex(2);
      m_Filter.setInputFormat(icopy);
      fail("Should have thrown an exception indicating a NUMERIC attribute!");
    } catch (Exception ex) {
      // OK
    }
  }

  public void testDefault() {
    ((MakeIndicatorFilter)m_Filter).setAttributeIndex(1);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    // Check that default attribute type is numeric
    assertEquals("Default attribute encoding should be NUMERIC",
                 Attribute.NUMERIC, result.attribute(1).type());
    // Check that default indication is correct
    for (int i = 0; i < result.numInstances(); i++) {
      assertTrue("Checking indicator for instance: " + (i + 1),
             (m_Instances.instance(i).value(1) == 2) ==
             (result.instance(i).value(1) == 1));
    }
  }

  public void testNominalEncoding() {
    ((MakeIndicatorFilter)m_Filter).setAttributeIndex(1);
    ((MakeIndicatorFilter)m_Filter).setNumeric(false);    
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    // Check that default attribute type is numeric
    assertEquals("New attribute encoding should be NOMINAL",
                 Attribute.NOMINAL, result.attribute(1).type());
    // Check that default indication is correct
    for (int i = 0; i < result.numInstances(); i++) {
      assertTrue("Checking indicator for instance: " + (i + 1),
             (m_Instances.instance(i).value(1) == 2) ==
             (result.instance(i).value(1) == 1));
    }
  }

  public void testMultiValueIndication() {
    ((MakeIndicatorFilter)m_Filter).setAttributeIndex(1);
    try {
      ((MakeIndicatorFilter)m_Filter).setValueIndices("1,3");
    } catch (Exception ex) {
      fail("Is Range broken?");
    }
    ((MakeIndicatorFilter)m_Filter).setNumeric(false);    
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    // Check that default attribute type is numeric
    assertEquals("New attribute encoding should be NOMINAL",
                 Attribute.NOMINAL, result.attribute(1).type());
    // Check that default indication is correct
    for (int i = 0; i < result.numInstances(); i++) {
      assertTrue("Checking indicator for instance: " + (i + 1),
             ((m_Instances.instance(i).value(1) == 0) ||
              (m_Instances.instance(i).value(1) == 2)) 
             ==
             (result.instance(i).value(1) == 1));
    }
  }

  public static Test suite() {
    return new TestSuite(MakeIndicatorFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
