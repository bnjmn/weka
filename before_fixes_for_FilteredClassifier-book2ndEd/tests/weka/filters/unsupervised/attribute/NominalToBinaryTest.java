/*
 * Copyright (C) 2002 University of Waikato 
 */

package weka.filters.unsupervised.attribute;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Attribute;
import weka.filters.Filter;
import weka.filters.AbstractFilterTest;

/**
 * Tests NominalToBinary. Run from the command line with:<p>
 * java weka.filters.NominalToBinaryTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 1.2 $
 */
public class NominalToBinaryTest extends AbstractFilterTest {
  
  public NominalToBinaryTest(String name) { super(name);  }

  /** Creates an example NominalToBinary */
  public Filter getFilter() {
    NominalToBinary f = new NominalToBinary();
    return f;
  }

  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes() + 5, result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    // Eibe can enhance this to check the binarizing is correct.
  }


  public static Test suite() {
    return new TestSuite(NominalToBinaryTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
