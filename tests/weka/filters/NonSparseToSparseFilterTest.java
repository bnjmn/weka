/*
 * Copyright 2000 Webmind Inc. 
 */

package weka.filters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

/**
 * Tests NonSparseToSparseFilter. Run from the command line with:<p>
 * java weka.filters.NonSparseToSparseFilterTest
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.1 $
 */
public class NonSparseToSparseFilterTest extends AbstractFilterTest {
  
  public NonSparseToSparseFilterTest(String name) { super(name);  }

  /** Creates an example NonSparseToSparseFilter */
  public Filter getFilter() {
    NonSparseToSparseFilter f = new NonSparseToSparseFilter();
    return f;
  }

  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    // Check conversion is OK
    for (int i = 0; i < result.numInstances(); i++) {
      assert("Instance should be an instanceof SparseInstance",
             result.instance(i) instanceof SparseInstance);
    }
  }


  public static Test suite() {
    return new TestSuite(NonSparseToSparseFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
