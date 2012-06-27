package weka.filters.unsupervised.attribute;

import weka.core.Attribute;
import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.LOF;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class LOFTest extends AbstractFilterTest {
 
  public LOFTest(String name) {
    super(name);
  }
  
  public Filter getFilter() {
    LOF temp = new LOF();
    temp.setMinPointsLowerBound("5");
    temp.setMinPointsUpperBound("10");
    return temp;
  }
  
  protected void setUp() throws Exception {
    super.setUp();

    m_Instances.deleteAttributeType(Attribute.STRING);
    
    // class index
   // m_Instances.setClassIndex(1);
  }
  
  protected void performTest() {
    Instances icopy = new Instances(m_Instances);
    Instances result = null;
    try {
      m_Filter.setInputFormat(icopy);
    } 
    catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on setInputFormat(): \n" + ex.getMessage());
    }
    try {
      result = Filter.useFilter(icopy, m_Filter);
      assertNotNull(result);
    } 
    catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on useFilter(): \n" + ex.getMessage());
    }

    assertEquals(icopy.numInstances(), result.numInstances());
    assertEquals(icopy.numAttributes() + 1, result.numAttributes());
  }
  
  public void testTypical() {
    m_Filter = getFilter();
    performTest();
  }
  
  /**
   * Returns a configures test suite.
   * 
   * @return            a configured test suite
   */
  public static Test suite() {
    return new TestSuite(LOFTest.class);
  }
  
  /**
   * For running the test from commandline.
   * 
   * @param args        ignored
   */
  public static void main(String[] args){
    TestRunner.run(suite());
  }
}
