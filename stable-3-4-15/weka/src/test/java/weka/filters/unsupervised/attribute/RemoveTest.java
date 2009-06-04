/*
 * Copyright (C) 2002 University of Waikato 
 */

package weka.filters.unsupervised.attribute;

import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests Remove. Run from the command line with:<p>
 * java weka.filters.RemoveTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 1.3.2.1 $
 */
public class RemoveTest extends AbstractFilterTest {
  
  public RemoveTest(String name) { super(name);  }

  /** Creates a default Remove */
  public Filter getFilter() {
    return getFilter("1-3");
  }

  /** Creates a specialized Remove */
  public Filter getFilter(String rangelist) {
    
    Remove af = new Remove();
    af.setAttributeIndices(rangelist);
    return af;
  }

  /**
   * returns the configured FilteredClassifier.
   * 
   * @return the configured FilteredClassifier
   */
  protected FilteredClassifier getFilteredClassifier() {
    FilteredClassifier	result;
    
    result = super.getFilteredClassifier();
    ((Remove) result.getFilter()).setAttributeIndices("2,4");
    
    return result;
  }

  public void testTypical() {
    m_Filter = getFilter("1,2");
    ((Remove)m_Filter).setInvertSelection(true);
    Instances result = useFilter();
    assertEquals(2, result.numAttributes());
    assertEquals(m_Instances.attribute(0).name(), result.attribute(0).name());
    assertEquals(m_Instances.attribute(1).name(), result.attribute(1).name());
  }

  public void testTypical2() {
    m_Filter = getFilter("3-4");
    ((Remove)m_Filter).setInvertSelection(true);
    Instances result = useFilter();
    assertEquals(2, result.numAttributes());
    assertEquals(m_Instances.attribute(2).name(), result.attribute(0).name());
    assertEquals(m_Instances.attribute(3).name(), result.attribute(1).name());
  }

  public void testNonInverted() {
    m_Filter = getFilter("1,2");
    ((Remove)m_Filter).setInvertSelection(false);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes() - 2, result.numAttributes());
    assertEquals(m_Instances.attribute(2).name(), result.attribute(0).name());
    assertEquals(m_Instances.attribute(3).name(), result.attribute(1).name());
  }

  public void testNonInverted2() {
    m_Filter = getFilter("first-3");
    ((Remove)m_Filter).setInvertSelection(false);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes() - 3, result.numAttributes());
    assertEquals(m_Instances.attribute(3).name(), result.attribute(0).name());
  }

  public static Test suite() {
    return new TestSuite(RemoveTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
