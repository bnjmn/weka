/*
 * Copyright (C) 2002 University of Waikato 
 */

package weka.filters.supervised.instance;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.AbstractFilterTest;

/**
 * Tests StratifiedRemoveFolds. Run from the command line with:<p>
 * java weka.filters.StratifiedRemoveFoldsTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 1.2 $
 */
public class StratifiedRemoveFoldsTest extends AbstractFilterTest {
  
  public StratifiedRemoveFoldsTest(String name) { super(name);  }

  /** Creates a default StratifiedRemoveFolds */
  public Filter getFilter() {
    StratifiedRemoveFolds f = new StratifiedRemoveFolds();
    return f;
  }

  /** Remove string attributes from default fixture instances */
  protected void setUp() throws Exception {

    super.setUp();
    m_Instances.setClassIndex(1);
  }

  public void testAllFolds() {
    
    int totInstances = 0;
    for (int i = 0; i < 10; i++) {
      ((StratifiedRemoveFolds)m_Filter).setFold(i + 1);
      Instances result = useFilter();
      assertEquals(m_Instances.numAttributes(), result.numAttributes());
      totInstances += result.numInstances();
    }
    assertEquals("Expecting output number of instances to match",
                 m_Instances.numInstances(),  totInstances);
  }

  public static Test suite() {
    return new TestSuite(StratifiedRemoveFoldsTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
