/*
 * Copyright 2000 Webmind Inc. 
 */

package weka.filters.unsupervised.instance;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.AbstractFilterTest;

/**
 * Tests RemoveRange. Run from the command line with:<p>
 * java weka.filters.RemoveRangeTest
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.1 $
 */
public class RemoveRangeTest extends AbstractFilterTest {
  
  public RemoveRangeTest(String name) { super(name);  }

  /** Creates a default RemoveRange */
  public Filter getFilter() {
    RemoveRange f = new RemoveRange();
    return f;
  }

  public void testSpecifiedRange() {
    
    ((RemoveRange)m_Filter).setInstancesIndices("1-10");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(10,  result.numInstances());
    for (int i = 0; i < 10; i++) {
      assertEquals(m_Instances.instance(i).toString(), result.instance(i).toString());
    }
  }

  public static Test suite() {
    return new TestSuite(RemoveRangeTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
