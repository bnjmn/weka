/*
 * Copyright 2000 Webmind Inc. 
 */

package weka.filters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Attribute;

/**
 * Tests NominalToBinaryFilter. Run from the command line with:<p>
 * java weka.filters.NominalToBinaryFilterTest
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.1 $
 */
public class NominalToBinaryFilterTest extends AbstractFilterTest {
  
  public NominalToBinaryFilterTest(String name) { super(name);  }

  /** Creates an example NominalToBinaryFilter */
  public Filter getFilter() {
    NominalToBinaryFilter f = new NominalToBinaryFilter();
    return f;
  }

  /** Remove string attributes from default fixture instances */
  protected void setUp() throws Exception {

    super.setUp();
    // NominalToBinary requires a class attribute be set
    m_Instances.setClassIndex(m_Instances.numAttributes() - 1);
  }

  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes() + 3, result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    // Eibe can enhance this to check the binarizing is correct.
  }


  public static Test suite() {
    return new TestSuite(NominalToBinaryFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
