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
 * Tests RemoveFolds. Run from the command line with:<p>
 * java weka.filters.RemoveFoldsTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 1.3 $
 */
public class RemoveFoldsTest extends AbstractFilterTest {
  
  public RemoveFoldsTest(String name) { super(name);  }

  /** Creates a default RemoveFolds */
  public Filter getFilter() {
    RemoveFolds f = new RemoveFolds();
    return f;
  }

  public void testAllFolds() {
    
    int totInstances = 0;
    for (int i = 0; i < 10; i++) {
      ((RemoveFolds)m_Filter).setFold(i + 1);
      Instances result = useFilter();
      assertEquals(m_Instances.numAttributes(), result.numAttributes());
      totInstances += result.numInstances();
    }
    assertEquals("Expecting output number of instances to match",
                 m_Instances.numInstances(),  totInstances);
  }

  public static Test suite() {
    return new TestSuite(RemoveFoldsTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
