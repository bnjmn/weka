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
 * Tests ReplaceMissingValues. Run from the command line with:<p>
 * java weka.filters.ReplaceMissingValuesTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 1.2 $
 */
public class ReplaceMissingValuesTest extends AbstractFilterTest {
  
  public ReplaceMissingValuesTest(String name) { super(name);  }

  /** Creates a default ReplaceMissingValues */
  public Filter getFilter() {
    return new ReplaceMissingValues();
  }

  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
    for (int j = 0; j < m_Instances.numAttributes(); j++) {
      Attribute inatt = m_Instances.attribute(j);
      Attribute outatt = result.attribute(j);
      for (int i = 0; i < m_Instances.numInstances(); i++) {
        if (m_Instances.attribute(j).isString()) {
          if (m_Instances.instance(i).isMissing(j)) {
            assertTrue("Missing values in strings cannot be replaced",
                   result.instance(i).isMissing(j));
          } else {
            assertEquals("String values should not have changed",
                         inatt.value((int)m_Instances.instance(i).value(j)),
                         outatt.value((int)result.instance(i).value(j)));
          }
        } else {
          assertTrue("All non-string missing values should have been replaced",
                 !result.instance(i).isMissing(j));
        }
      }
    }
  }

  public static Test suite() {
    return new TestSuite(ReplaceMissingValuesTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
