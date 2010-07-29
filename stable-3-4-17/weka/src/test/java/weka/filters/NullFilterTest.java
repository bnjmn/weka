/*
 * Copyright (C) 2002 University of Waikato 
 */

package weka.filters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;

/**
 * Tests NullFilter. Run from the command line with:<p>
 * java weka.filters.NullFilterTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 1.2.2.1 $
 */
public class NullFilterTest extends AbstractFilterTest {
  
  public NullFilterTest(String name) { super(name);  }

  /** Creates a default NullFilter */
  public Filter getFilter() {
    return new NullFilter();
  }

  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(0, result.numInstances());
  }
  
  /**
   * doesn't make sense, since the null filter doesn't return any instances
   */
  public void testFilteredClassifier() {
    // do nothing
  }

  public static Test suite() {
    return new TestSuite(NullFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
