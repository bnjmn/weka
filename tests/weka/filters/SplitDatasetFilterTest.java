/*
 * Copyright 2000 Webmind Inc. 
 */

package weka.filters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Tests SplitDatasetFilter. Run from the command line with:<p>
 * java weka.filters.SplitDatasetFilterTest
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.1 $
 */
public class SplitDatasetFilterTest extends AbstractFilterTest {
  
  public SplitDatasetFilterTest(String name) { super(name);  }

  /** Creates a default SplitDatasetFilter */
  public Filter getFilter() {
    SplitDatasetFilter f = new SplitDatasetFilter();
    return f;
  }

  public void testAllFolds() {
    
    int totInstances = 0;
    for (int i = 0; i < 10; i++) {
      ((SplitDatasetFilter)m_Filter).setFold(i + 1);
      Instances result = useFilter();
      assertEquals(m_Instances.numAttributes(), result.numAttributes());
      totInstances += result.numInstances();
    }
    assertEquals("Expecting output number of instances to match",
                 m_Instances.numInstances(),  totInstances);
  }

  public void testSpecifiedRange() {
    
    ((SplitDatasetFilter)m_Filter).setInstancesIndices("1-10");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(10,  result.numInstances());
    for (int i = 0; i < 10; i++) {
      assertEquals(m_Instances.instance(i).toString(), result.instance(i).toString());
    }
  }

  public static Test suite() {
    return new TestSuite(SplitDatasetFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
