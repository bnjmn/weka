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

/**
 * Tests AttributeSelectionFilter. Run from the command line with:<p>
 * java weka.filters.AttributeSelectionFilterTest
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.2 $
 */
public class AttributeSelectionFilterTest extends AbstractFilterTest {
  
  public AttributeSelectionFilterTest(String name) { super(name);  }

  /** Creates a default AttributeSelectionFilter */
  public Filter getFilter() {
    return new AttributeSelectionFilter();
  }

  /** Creates a specialized AttributeSelectionFilter */
  public Filter getFilter(ASEvaluation evaluator, ASSearch search) {
    
    AttributeSelectionFilter af = new AttributeSelectionFilter();
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
    AttributeTypeFilter af = new AttributeTypeFilter();
    af.setInputFormat(m_Instances);
    m_Instances = Filter.useFilter(m_Instances, af);
    for (int i = 0; i < m_Instances.numAttributes(); i++) {
      assert("Problem with AttributeTypeFilter in setup", 
             m_Instances.attribute(i).type() != Attribute.STRING);
    }
  }

  public void testPrincipalComponent() {
    m_Filter = getFilter(new weka.attributeSelection.PrincipalComponents(), 
                         new weka.attributeSelection.Ranker());
    Instances result = useFilter();
    assert(m_Instances.numAttributes() != result.numAttributes());
  }


  public static Test suite() {
    return new TestSuite(AttributeSelectionFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
