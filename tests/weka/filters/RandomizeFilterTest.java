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
 * Tests RandomizeFilter. Run from the command line with:<p>
 * java weka.filters.RandomizeFilterTest
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.2 $
 */
public class RandomizeFilterTest extends AbstractFilterTest {
  
  public RandomizeFilterTest(String name) { super(name);  }

  /** Creates a default RandomizeFilter */
  public Filter getFilter() {
    return new RandomizeFilter();
  }

  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
    boolean diff = false;
    for (int i = 0; i < m_Instances.numInstances(); i++) {
      String i1 = m_Instances.instance(i).toString();
      String i2 = result.instance(i).toString();
      if (!i1.equals(i2)) {
        diff = true;
      }
    }
    assertTrue("All instances seem to be in the same positions", diff);
  }

  public static Test suite() {
    return new TestSuite(RandomizeFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
