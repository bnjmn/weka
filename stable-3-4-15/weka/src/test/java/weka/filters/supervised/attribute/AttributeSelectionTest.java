/*
 * Copyright (C) 2002 University of Waikato 
 */

package weka.filters.supervised.attribute;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Attribute;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.RemoveType;

/**
 * Tests AttributeSelection. Run from the command line with:<p>
 * java weka.filters.AttributeSelectionTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 1.2 $
 */
public class AttributeSelectionTest extends AbstractFilterTest {
  
  public AttributeSelectionTest(String name) { super(name);  }

  /** Creates a default AttributeSelection */
  public Filter getFilter() {
    return new AttributeSelection();
  }

  /** Creates a specialized AttributeSelection */
  public Filter getFilter(ASEvaluation evaluator, ASSearch search) {
    
    AttributeSelection af = new AttributeSelection();
    if (evaluator != null) {
      af.setEvaluator(evaluator);
    }
    if (search != null) {
      af.setSearch(search);
    }
    return af;
  }

  /** Remove string attributes from default fixture instances */
  protected void setUp() throws Exception {

    super.setUp();
    RemoveType af = new RemoveType();
    af.setInputFormat(m_Instances);
    m_Instances = Filter.useFilter(m_Instances, af);
    for (int i = 0; i < m_Instances.numAttributes(); i++) {
      assertTrue("Problem with AttributeTypeFilter in setup", 
             m_Instances.attribute(i).type() != Attribute.STRING);
    }
  }

  public void testPrincipalComponent() {
    m_Filter = getFilter(new weka.attributeSelection.PrincipalComponents(), 
                         new weka.attributeSelection.Ranker());
    Instances result = useFilter();
    assertTrue(m_Instances.numAttributes() != result.numAttributes());
  }


  public static Test suite() {
    return new TestSuite(AttributeSelectionTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
