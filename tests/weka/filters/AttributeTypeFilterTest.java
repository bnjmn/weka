/*
 * Copyright 2000 Webmind Inc. 
 */

package weka.filters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Attribute;
import weka.core.SelectedTag;

/**
 * Tests AttributeTypeFilter. Run from the command line with:<p>
 * java weka.filters.AttributeTypeFilterTest
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.5 $
 */
public class AttributeTypeFilterTest extends AbstractFilterTest {
  
  public AttributeTypeFilterTest(String name) { super(name);  }

  /** Creates a default AttributeTypeFilter */
  public Filter getFilter() {
    return new AttributeTypeFilter();
  }

  /** Creates a specialized AttributeTypeFilter */
  public Filter getFilter(int attType) {
    
    AttributeTypeFilter af = new AttributeTypeFilter();
    try {
      af.setAttributeType(new SelectedTag(attType,
                                          AttributeTypeFilter.TAGS_ATTRIBUTETYPE));
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
    return new TestSuite(AttributeTypeFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
