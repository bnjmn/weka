/*
 * Copyright (C) 2002 University of Waikato 
 */

package weka.filters.unsupervised.attribute;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;
import weka.core.Attribute;
import weka.filters.Filter;
import weka.filters.AbstractFilterTest;

/**
 * Tests Obfuscate. Run from the command line with:<p>
 * java weka.filters.ObfuscateTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 1.2 $
 */
public class ObfuscateTest extends AbstractFilterTest {
  
  public ObfuscateTest(String name) { super(name);  }

  /** Creates a default Obfuscate */
  public Filter getFilter() {
    return new Obfuscate();
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
    return new TestSuite(ObfuscateTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
