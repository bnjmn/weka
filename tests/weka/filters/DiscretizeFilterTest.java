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
 * Tests DiscretizeFilter. Run from the command line with:<p>
 * java weka.filters.DiscretizeFilterTest
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision:
 */
public class DiscretizeFilterTest extends AbstractFilterTest {
  
  public DiscretizeFilterTest(String name) { super(name);  }

  /** Creates a default DiscretizeFilter */
  public Filter getFilter() {
    DiscretizeFilter f= new DiscretizeFilter();
    f.setUseMDL(false);
    return f;
  }

  /** Creates a specialized DiscretizeFilter */
  public Filter getFilter(String rangelist) {
    
    try {
      DiscretizeFilter f = new DiscretizeFilter();
      f.setUseMDL(false);
      f.setAttributeIndices(rangelist);
      return f;
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
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    // None of the attributes should have changed, since 1,2 aren't numeric
    for (int i = 0; i < result.numAttributes(); i++) {
      assertEquals(m_Instances.attribute(i).type(), result.attribute(i).type());
      assertEquals(m_Instances.attribute(i).name(), result.attribute(i).name());
    }
  }

  public void testTypical2() {
    m_Filter = getFilter("3-4");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    for (int i = 0; i < result.numAttributes(); i++) {
      if (i != 2) {
        assertEquals(m_Instances.attribute(i).type(), result.attribute(i).type());
        assertEquals(m_Instances.attribute(i).name(), result.attribute(i).name());
      } else {
        assertEquals(Attribute.NOMINAL, result.attribute(i).type());
        assertEquals(10, result.attribute(i).numValues());
      }
    }
  }

  public void testInverted() {
    m_Filter = getFilter("1,2");
    ((DiscretizeFilter)m_Filter).setInvertSelection(true);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    for (int i = 0; i < result.numAttributes(); i++) {
      if ((i < 2) || (m_Instances.attribute(i).type() != Attribute.NUMERIC)) {
        assertEquals(m_Instances.attribute(i).type(), result.attribute(i).type());
        assertEquals(m_Instances.attribute(i).name(), result.attribute(i).name());
      } else {
        assertEquals(Attribute.NOMINAL, result.attribute(i).type());
        assertEquals(10, result.attribute(i).numValues());
      }
    }
  }

  public void testNonInverted2() {
    m_Filter = getFilter("first-3");
    ((DiscretizeFilter)m_Filter).setInvertSelection(true);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    for (int i = 0; i < result.numAttributes(); i++) {
      if ((i < 3) || (m_Instances.attribute(i).type() != Attribute.NUMERIC)) {
        assertEquals(m_Instances.attribute(i).type(), result.attribute(i).type());
        assertEquals(m_Instances.attribute(i).name(), result.attribute(i).name());
      } else {
        assertEquals(Attribute.NOMINAL, result.attribute(i).type());
        assertEquals(10, result.attribute(i).numValues());
      }
    }
  }

  public void testBins() {
    m_Filter = getFilter("3");
    ((DiscretizeFilter)m_Filter).setBins(5);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(Attribute.NOMINAL, result.attribute(2).type());
    assertEquals(5, result.attribute(2).numValues());

    ((DiscretizeFilter)m_Filter).setBins(20);
    result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(Attribute.NOMINAL, result.attribute(2).type());
    assertEquals(20, result.attribute(2).numValues());
  }

  public void testFindNumBins() {
    m_Filter = getFilter("3");
    ((DiscretizeFilter)m_Filter).setFindNumBins(true);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(Attribute.NOMINAL, result.attribute(2).type());
    assert(5 >= result.attribute(2).numValues());
  }

  public void testMDL() {
    m_Filter = getFilter("3");
    try {
      m_Instances.setClassIndex(1);
    } catch (Exception ex) {
      fail("Shouldn't get here: " + ex.getMessage());
    }
    ((DiscretizeFilter)m_Filter).setUseMDL(true);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(Attribute.NOMINAL, result.attribute(2).type());
  }

  public void testBetterEncoding() {
    m_Filter = getFilter("3");
    try {
      m_Instances.setClassIndex(1);
    } catch (Exception ex) {
      fail("Shouldn't get here: " + ex.getMessage());
    }
    ((DiscretizeFilter)m_Filter).setUseBetterEncoding(true);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(Attribute.NOMINAL, result.attribute(2).type());
  }

  public void testUseKononenko() {
    m_Filter = getFilter("3");
    try {
      m_Instances.setClassIndex(1);
    } catch (Exception ex) {
      fail("Shouldn't get here: " + ex.getMessage());
    }
    ((DiscretizeFilter)m_Filter).setUseKononenko(true);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(Attribute.NOMINAL, result.attribute(2).type());
  }

  public static Test suite() {
    return new TestSuite(DiscretizeFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
