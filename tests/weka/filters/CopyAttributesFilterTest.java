/*
 * Copyright 2000 Webmind Inc. 
 */

package weka.filters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;
import weka.core.Instance;

/**
 * Tests CopyAttributesFilter. Run from the command line with:<p>
 * java weka.filters.CopyAttributesFilterTest
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.2 $
 */
public class CopyAttributesFilterTest extends AbstractFilterTest {
  
  public CopyAttributesFilterTest(String name) { super(name);  }

  /** Creates a default CopyAttributesFilter */
  public Filter getFilter() {
    return getFilter("1-3");
  }

  /** Creates a specialized CopyAttributesFilter */
  public Filter getFilter(String rangelist) {
    
    try {
      CopyAttributesFilter af = new CopyAttributesFilter();
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
    assert(result.attribute(origNum).name().endsWith(m_Instances.attribute(0).name()));
    assert(result.attribute(origNum + 1).name().endsWith(m_Instances.attribute(1).name()));
  }

  public void testTypical2() {
    m_Filter = getFilter("3-4");
    Instances result = useFilter();
    int origNum = m_Instances.numAttributes();
    assertEquals(origNum + 2, result.numAttributes());
    assert(result.attribute(origNum).name().endsWith(m_Instances.attribute(2).name()));
    assert(result.attribute(origNum + 1).name().endsWith(m_Instances.attribute(3).name()));
  }

  public void testNonInverted() {
    m_Filter = getFilter("1,2");
    ((CopyAttributesFilter)m_Filter).setInvertSelection(true);
    Instances result = useFilter();
    int origNum = m_Instances.numAttributes();
    assertEquals(origNum + origNum - 2, result.numAttributes());
    assert(result.attribute(origNum).name().endsWith(m_Instances.attribute(2).name()));
    assert(result.attribute(origNum + 1).name().endsWith(m_Instances.attribute(3).name()));
  }

  public void testNonInverted2() {
    m_Filter = getFilter("first-3");
    ((CopyAttributesFilter)m_Filter).setInvertSelection(true);
    Instances result = useFilter();
    int origNum = m_Instances.numAttributes();
    assertEquals(origNum + origNum - 3, result.numAttributes());
    assert(result.attribute(origNum).name().endsWith(m_Instances.attribute(3).name()));
  }

  public static Test suite() {
    return new TestSuite(CopyAttributesFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
