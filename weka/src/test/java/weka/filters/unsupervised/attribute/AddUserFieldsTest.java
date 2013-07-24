package weka.filters.unsupervised.attribute;

import weka.core.Attribute;
import weka.core.Instances;
import weka.core.Environment;
import weka.core.Utils;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.AddUserFields;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class AddUserFieldsTest extends AbstractFilterTest {
 
  public AddUserFieldsTest(String name) {
    super(name);
  }
  
  public Filter getFilter() {
    AddUserFields temp = new AddUserFields();
    Environment env = new Environment();
    env.addVariable("NOM", "aNomValue");
    
    String params = "-A douglas@numeric@42 -A nomAtt@nominal@aValue -A "
      + "aDate@date:yyyy-MM-dd@2012-07-09 -A varTest@nominal@${NOM}";
    try {
      String[] opts = Utils.splitOptions(params);
      temp.setEnvironment(env);
      temp.setOptions(opts);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return temp;
  }
  
  protected void setUp() throws Exception {
    super.setUp();
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
    assertEquals(icopy.numAttributes() + 4, result.numAttributes());
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
    return new TestSuite(AddUserFieldsTest.class);
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
