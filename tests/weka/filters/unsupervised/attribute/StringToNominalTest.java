/*
 * Copyright (C) 2002 University of Waikato 
 */

package weka.filters.unsupervised.attribute;

import weka.core.Attribute;
import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests StringToNominal. Run from the command line with:<p>
 * java weka.filters.StringToNominalTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 1.3.2.1 $
 */
public class StringToNominalTest extends AbstractFilterTest {
  
  public StringToNominalTest(String name) { super(name);  }

  /** Creates an example StringToNominal */
  public Filter getFilter() {
    StringToNominal f = new StringToNominal();
    f.setAttributeIndex("1");
    return f;
  }
  
  /**
   * returns data generated for the FilteredClassifier test
   * 
   * @return		the dataset for the FilteredClassifier
   * @throws Exception	if generation of data fails
   */
  protected Instances getFilteredClassifierData() throws Exception {
    Instances	result;
    int		i;

    result = new Instances(m_Instances);
    result.deleteAttributeAt(3);
    for (i = 0; i < result.numAttributes(); i++) {
      if (result.attribute(i).isNominal()) {
	result.setClassIndex(i);
	break;
      }
    }
    
    return result;
  }

  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    assertEquals("Attribute type should now be NOMINAL",
                 Attribute.NOMINAL, result.attribute(0).type());

    assertEquals(14, result.attribute(0).numValues());
  }

  public void testMissing() {
    ((StringToNominal)m_Filter).setAttributeIndex("4");
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    assertEquals("Attribute type should now be NOMINAL",
                 Attribute.NOMINAL, result.attribute(3).type());

    assertEquals(8, result.attribute(3).numValues());
    for (int i = 0; i < result.numInstances(); i++) {
      assertTrue("Missing values should be preserved",
             m_Instances.instance(i).isMissing(3) ==
             result.instance(i).isMissing(3));
    }
  }


  public static Test suite() {
    return new TestSuite(StringToNominalTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
