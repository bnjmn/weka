/*
 * Copyright 2000 Webmind Inc. 
 */

package weka.filters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Tests NormalizationFilter. Run from the command line with:<p>
 * java weka.filters.NormalizationFilterTest
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.1 $
 */
public class NormalizationFilterTest extends AbstractFilterTest {
  
  public NormalizationFilterTest(String name) { super(name);  }

  /** Creates an example NormalizationFilter */
  public Filter getFilter() {
    NormalizationFilter f = new NormalizationFilter();
    return f;
  }

  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());

    // Check conversion is OK
    for (int j = 0; j < result.numAttributes(); j++) {
      if (result.attribute(j).isNumeric()) {
        for (int i = 0; i < result.numInstances(); i++) {
          if (!result.instance(i).isMissing(j)) {
            assert("Value should be between 0 and 1",
                   (result.instance(i).value(j) >= 0) &&
                   (result.instance(i).value(j) <= 1));
          }
        }
      }
    }
  }


  public static Test suite() {
    return new TestSuite(NormalizationFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
