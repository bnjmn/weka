/*
 * Copyright (C) 2002 University of Waikato 
 */

package weka.filters;

import weka.core.Instances;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests AllFilter. Run from the command line with:<p>
 * java weka.filters.AllFilterTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 1.3.2.1 $
 */
public class AllFilterTest extends AbstractFilterTest {
  
  public AllFilterTest(String name) { super(name);  }

  /** Creates a default AllFilter */
  public Filter getFilter() {
    return new AllFilter();
  }

  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
  }

  public static Test suite() {
    return new TestSuite(AllFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
