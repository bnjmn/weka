/*
 * Copyright (C) 2002 University of Waikato 
 */

package weka.filters.unsupervised.attribute;

import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests FirstOrder. Run from the command line with:<p>
 * java weka.filters.FirstOrderTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 1.2.2.1 $
 */
public class FirstOrderTest extends AbstractFilterTest {
  
  private static double EXPR_DELTA = 0.001;

  public FirstOrderTest(String name) { super(name);  }

  /** Creates a default FirstOrder */
  public Filter getFilter() {
    return getFilter("6,3");
  }

  /** Creates a specialized FirstOrder */
  public Filter getFilter(String rangelist) {
    
    try {
      FirstOrder af = new FirstOrder();
      af.setAttributeIndices(rangelist);
      return af;
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception setting attribute range: " + rangelist 
           + "\n" + ex.getMessage()); 
    }
    return null;
  }

  /**
   * returns the configured FilteredClassifier.
   * 
   * @return the configured FilteredClassifier
   */
  protected FilteredClassifier getFilteredClassifier() {
    FilteredClassifier	result;
    
    result = super.getFilteredClassifier();
    try {
      ((FirstOrder) result.getFilter()).setAttributeIndices("2,4");
    }
    catch (Exception e) {
      fail("Problem setting up FilteredClassifier");
    }
    
    return result;
  }

  public void testTypical() {
    m_Filter = getFilter("6,3");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes() - 1, result.numAttributes());
    for (int i = 0; i < result.numInstances(); i++) {
      Instance orig = m_Instances.instance(i);
      if (orig.isMissing(5) || orig.isMissing(2)) {
        assertTrue("Instance " + (i + 1) + " should have been ?" , 
               result.instance(i).isMissing(4));
      } else {
        assertEquals(orig.value(5) - orig.value(2), 
                     result.instance(i).value(4), 
                     EXPR_DELTA);
      }
    }
  }

  public void testTypical2() {
    m_Filter = getFilter("3,6");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes() - 1, result.numAttributes());
    for (int i = 0; i < result.numInstances(); i++) {
      Instance orig = m_Instances.instance(i);
      if (orig.isMissing(5) || orig.isMissing(2)) {
        assertTrue("Instance " + (i + 1) + " should have been ?" , 
               result.instance(i).isMissing(4));
      } else {
        assertEquals(orig.value(5) - orig.value(2), 
                     result.instance(i).value(4), 
                     EXPR_DELTA);
      }
    }
  }

  public static Test suite() {
    return new TestSuite(FirstOrderTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
