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
 * Tests SparseToNonSparseFilter. Run from the command line with:<p>
 * java weka.filters.SparseToNonSparseFilterTest
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.1 $
 */
public class SparseToNonSparseFilterTest extends AbstractFilterTest {
  
  public SparseToNonSparseFilterTest(String name) { super(name);  }

  /** Creates an example SparseToNonSparseFilter */
  public Filter getFilter() {
    SparseToNonSparseFilter f = new SparseToNonSparseFilter();
    return f;
  }

  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    // Check conversion is OK
    for (int i = 0; i < result.numInstances(); i++) {
      assert("Instance should not be an instanceof SparseInstance:" + (i + 1),
             !(result.instance(i) instanceof SparseInstance));
    }
  }


  public static Test suite() {
    return new TestSuite(SparseToNonSparseFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
