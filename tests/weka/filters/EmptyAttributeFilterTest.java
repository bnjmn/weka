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
 * Tests EmptyAttributeFilter. Run from the command line with:<p>
 * java weka.filters.EmptyAttributeFilterTest
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.2 $
 */
public class EmptyAttributeFilterTest extends AbstractFilterTest {
  
  public EmptyAttributeFilterTest(String name) { super(name);  }

  /** Creates a default EmptyAttributeFilter */
  public Filter getFilter() {

    EmptyAttributeFilter f= new EmptyAttributeFilter();
    return f;
  }

  /**
   * Make one of the test data attributes empty so the superclass tests
   * are less trivial 
   */
  protected void setUp() throws Exception {

    super.setUp();
    makeEmpty(1, Instance.missingValue());
  }

  /** Make some of the attributes empty */
  protected void makeEmpty(int index, double value) {

    for (int i = 0; i < m_Instances.numInstances(); i++) {
      if (!m_Instances.instance(i).isMissing(index)) {
        m_Instances.instance(i).setValue(index, value);
      }
    }
    if (VERBOSE) System.err.println(m_Instances);
  }


  public void testTypical() {

    int del = 1; // setUp already makes one empty
    for (int i = 0; i < m_Instances.numAttributes(); i++) {
      makeEmpty(i, 0);
      if (!m_Instances.attribute(i).isString() && (i != 1)) {
        del++;
      }
      Instances result = useFilter();
      assertEquals("Emptied <= " + i,
                   m_Instances.numAttributes() - del, 
                   result.numAttributes());
    }
  }

  public static Test suite() {
    return new TestSuite(EmptyAttributeFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
