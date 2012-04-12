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
 * Tests Reorder. Run from the command line with:<p>
 * java weka.filters.ReorderTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.1 $
 */
public class ReorderTest extends AbstractFilterTest {
  
  public ReorderTest(String name) { 
    super(name);  
  }

  /** Creates a default Reorder */
  public Filter getFilter() {
    return getFilter("first-last");
  }

  /** Creates a specialized Reorder */
  public Filter getFilter(String rangelist) {
    
    try {
      Reorder af = new Reorder();
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
    m_Filter = getFilter("2,1");
    Instances result = useFilter();
    int origNum = m_Instances.numAttributes();
    assertEquals(2, result.numAttributes());
    assertTrue(result.attribute(1).name().endsWith(m_Instances.attribute(0).name()));
    assertTrue(result.attribute(0).name().endsWith(m_Instances.attribute(1).name()));
  }

  public void testTypical2() {
    m_Filter = getFilter("3-4");
    Instances result = useFilter();
    int origNum = m_Instances.numAttributes();
    assertEquals(2, result.numAttributes());
    assertTrue(result.attribute(0).name().endsWith(m_Instances.attribute(2).name()));
    assertTrue(result.attribute(1).name().endsWith(m_Instances.attribute(3).name()));
  }

  public void testTypical3() {
    m_Filter = getFilter("2-last,1");
    Instances result = useFilter();
    int origNum = m_Instances.numAttributes();
    assertEquals(origNum, result.numAttributes());
    assertTrue(result.attribute(0).name().endsWith(m_Instances.attribute(1).name()));
    assertTrue(result.attribute(1).name().endsWith(m_Instances.attribute(2).name()));
    assertTrue(result.attribute(origNum - 1).name().endsWith(m_Instances.attribute(0).name()));
  }

  public static Test suite() {
    return new TestSuite(ReorderTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
