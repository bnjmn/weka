/*
 * Copyright (C) 2002 University of Waikato 
 */

package weka.filters.unsupervised.attribute;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;
import weka.core.Instance;
import weka.filters.Filter;
import weka.filters.AbstractFilterTest;

/**
 * Tests Copy. Run from the command line with:<p>
 * java weka.filters.CopyTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 1.2 $
 */
public class CopyTest extends AbstractFilterTest {
  
  public CopyTest(String name) { super(name);  }

  /** Creates a default Copy */
  public Filter getFilter() {
    return getFilter("1-3");
  }

  /** Creates a specialized Copy */
  public Filter getFilter(String rangelist) {
    
    try {
      Copy af = new Copy();
      af.setAttributeIndices(rangelist);
      return af;
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception setting attribute range: " + rangelist 
           + "\n" + ex.getMessage()); 
    }
    return null;
  }

  public void testTypical() {
    m_Filter = getFilter("1,2");
    Instances result = useFilter();
    int origNum = m_Instances.numAttributes();
    assertEquals(origNum + 2, result.numAttributes());
    assertTrue(result.attribute(origNum).name().endsWith(m_Instances.attribute(0).name()));
    assertTrue(result.attribute(origNum + 1).name().endsWith(m_Instances.attribute(1).name()));
  }

  public void testTypical2() {
    m_Filter = getFilter("3-4");
    Instances result = useFilter();
    int origNum = m_Instances.numAttributes();
    assertEquals(origNum + 2, result.numAttributes());
    assertTrue(result.attribute(origNum).name().endsWith(m_Instances.attribute(2).name()));
    assertTrue(result.attribute(origNum + 1).name().endsWith(m_Instances.attribute(3).name()));
  }

  public void testNonInverted() {
    m_Filter = getFilter("1,2");
    ((Copy)m_Filter).setInvertSelection(true);
    Instances result = useFilter();
    int origNum = m_Instances.numAttributes();
    assertEquals(origNum + origNum - 2, result.numAttributes());
    assertTrue(result.attribute(origNum).name().endsWith(m_Instances.attribute(2).name()));
    assertTrue(result.attribute(origNum + 1).name().endsWith(m_Instances.attribute(3).name()));
  }

  public void testNonInverted2() {
    m_Filter = getFilter("first-3");
    ((Copy)m_Filter).setInvertSelection(true);
    Instances result = useFilter();
    int origNum = m_Instances.numAttributes();
    assertEquals(origNum + origNum - 3, result.numAttributes());
    assertTrue(result.attribute(origNum).name().endsWith(m_Instances.attribute(3).name()));
  }

  public static Test suite() {
    return new TestSuite(CopyTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
