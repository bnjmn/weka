/*
 * Copyright 2000 Webmind Inc. 
 */

package weka.filters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;
import weka.core.Attribute;

/**
 * Tests ObfuscateFilter. Run from the command line with:<p>
 * java weka.filters.ObfuscateFilterTest
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.3 $
 */
public class ObfuscateFilterTest extends AbstractFilterTest {
  
  public ObfuscateFilterTest(String name) { super(name);  }

  /** Creates a default ObfuscateFilter */
  public Filter getFilter() {
    return new ObfuscateFilter();
  }

  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
    
    assertTrue(!m_Instances.relationName().equals(result.relationName()));
    for (int i = 0; i < m_Instances.numAttributes(); i++) {
      Attribute inatt = m_Instances.attribute(i);
      Attribute outatt = result.attribute(i);
      if (!inatt.isString() && !inatt.isDate()) {
        assertTrue("Attribute names should be changed",
               !inatt.name().equals(outatt.name()));
        if (inatt.isNominal()) {
          assertEquals("Number of nominal values shouldn't change",
                       inatt.numValues(), outatt.numValues());
          for (int j = 0; j < inatt.numValues(); j++) {
            assertTrue("Nominal labels should be changed",
                   !inatt.value(j).equals(outatt.value(j)));
          }
        }
      }
    }
  }

  public static Test suite() {
    return new TestSuite(ObfuscateFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
