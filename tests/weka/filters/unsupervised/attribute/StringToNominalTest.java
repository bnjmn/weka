/*
 * Copyright 2000 Webmind Inc. 
 */

package weka.filters.unsupervised.attribute;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Attribute;
import weka.filters.Filter;
import weka.filters.AbstractFilterTest;

/**
 * Tests StringToNominal. Run from the command line with:<p>
 * java weka.filters.StringToNominalTest
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.1 $
 */
public class StringToNominalTest extends AbstractFilterTest {
  
  public StringToNominalTest(String name) { super(name);  }

  /** Creates an example StringToNominal */
  public Filter getFilter() {
    StringToNominal f = new StringToNominal();
    f.setAttributeIndex(0);
    return f;
  }

  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    assertEquals("Attribute type should now be NOMINAL",
                 Attribute.NOMINAL, result.attribute(0).type());

    assertEquals(14, result.attribute(0).numValues());
  }

  public void testMissing() {
    ((StringToNominal)m_Filter).setAttributeIndex(3);
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    assertEquals("Attribute type should now be NOMINAL",
                 Attribute.NOMINAL, result.attribute(3).type());

    assertEquals(8, result.attribute(3).numValues());
    for (int i = 0; i < result.numInstances(); i++) {
      assertTrue("Missing values should be preserved",
             m_Instances.instance(i).isMissing(3) ==
             result.instance(i).isMissing(3));
    }
  }


  public static Test suite() {
    return new TestSuite(StringToNominalTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
