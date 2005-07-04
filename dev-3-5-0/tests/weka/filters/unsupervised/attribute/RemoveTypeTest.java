/*
 * Copyright (C) 2002 University of Waikato 
 */

package weka.filters.unsupervised.attribute;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Attribute;
import weka.core.SelectedTag;
import weka.filters.Filter;
import weka.filters.AbstractFilterTest;

/**
 * Tests RemoveType. Run from the command line with:<p>
 * java weka.filters.RemoveTypeTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 1.2 $
 */
public class RemoveTypeTest extends AbstractFilterTest {
  
  public RemoveTypeTest(String name) { super(name);  }

  /** Creates a default RemoveType */
  public Filter getFilter() {
    return new RemoveType();
  }

  /** Creates a specialized RemoveType */
  public Filter getFilter(int attType) {
    
    RemoveType af = new RemoveType();
    try {
      af.setAttributeType(new SelectedTag(attType,
                                          RemoveType.TAGS_ATTRIBUTETYPE));
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Couldn't set up filter with attribute type: " + attType);
    }
    return af;
  }

  public void testNominalFiltering() {
    m_Filter = getFilter(Attribute.NOMINAL);
    Instances result = useFilter();
    for (int i = 0; i < result.numAttributes(); i++) {
      assertTrue(result.attribute(i).type() != Attribute.NOMINAL);
    }
  }

  public void testStringFiltering() {
    m_Filter = getFilter(Attribute.STRING);
    Instances result = useFilter();
    for (int i = 0; i < result.numAttributes(); i++) {
      assertTrue(result.attribute(i).type() != Attribute.STRING);
    }
  }

  public void testNumericFiltering() {
    m_Filter = getFilter(Attribute.NUMERIC);
    Instances result = useFilter();
    for (int i = 0; i < result.numAttributes(); i++) {
      assertTrue(result.attribute(i).type() != Attribute.NUMERIC);
    }
  }

  public void testDateFiltering() {
    m_Filter = getFilter(Attribute.DATE);
    Instances result = useFilter();
    for (int i = 0; i < result.numAttributes(); i++) {
      assertTrue(result.attribute(i).type() != Attribute.DATE);
    }
  }

  public static Test suite() {
    return new TestSuite(RemoveTypeTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
