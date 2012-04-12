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
 * Tests MakeIndicatorFilter. Run from the command line with:<p>
 * java weka.filters.MakeIndicatorFilterTest
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision:
 */
public class MakeIndicatorFilterTest extends AbstractFilterTest {
  
  public MakeIndicatorFilterTest(String name) { super(name);  }

  /** Creates a default MakeIndicatorFilter */
  public Filter getFilter() {
    MakeIndicatorFilter f = new MakeIndicatorFilter();
    // Ensure the default we return can run on the test dataset
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
    // Check that default indication is correct
  }


  public static Test suite() {
    return new TestSuite(MakeIndicatorFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
