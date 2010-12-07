/*
 * Copyright (C) 2002 University of Waikato 
 */

package weka.filters.unsupervised.instance;

import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests Randomize. Run from the command line with:<p>
 * java weka.filters.RandomizeTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 1.2.2.1 $
 */
public class RandomizeTest extends AbstractFilterTest {
  
  public RandomizeTest(String name) { super(name);  }

  /** Creates a default Randomize */
  public Filter getFilter() {
    return new Randomize();
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
    return new TestSuite(RandomizeTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
