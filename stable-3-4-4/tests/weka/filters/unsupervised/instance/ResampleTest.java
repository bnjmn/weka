/*
 * Copyright (C) 2002 University of Waikato 
 */

package weka.filters.unsupervised.instance;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.AttributeStats;
import weka.filters.Filter;
import weka.filters.AbstractFilterTest;

/**
 * Tests Resample. Run from the command line with:<p>
 * java weka.filters.ResampleTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 1.2 $
 */
public class ResampleTest extends AbstractFilterTest {
  
  public ResampleTest(String name) { super(name);  }

  /** Creates a default Resample */
  public Filter getFilter() {
    Resample f = new Resample();
    f.setSampleSizePercent(50);
    return f;
  }

  public void testSampleSizePercent() {
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals("Expecting output to be 50% of input",
                 m_Instances.numInstances() / 2,  result.numInstances());

    ((Resample)m_Filter).setSampleSizePercent(200);
    result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals("Expecting output to be 200% of input",
                 m_Instances.numInstances() * 2,  result.numInstances());
  }

  public static Test suite() {
    return new TestSuite(ResampleTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
