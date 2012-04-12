/*
 * Copyright (C) 2002 University of Waikato 
 */

package weka.filters.supervised.instance;

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

  /** Need to set the class index */
  protected void setUp() throws Exception {

    super.setUp();
    m_Instances.setClassIndex(1);
  }

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

  public void testNoBias() throws Exception {
    m_Instances.setClassIndex(1);
    AttributeStats origs = m_Instances.attributeStats(1);
    assertNotNull(origs.nominalCounts);

    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    AttributeStats outs = result.attributeStats(1);

    // Check distributions are pretty similar
    assertNotNull(outs.nominalCounts);
    assertEquals(origs.nominalCounts.length, outs.nominalCounts.length);
    for (int i = 0; i < origs.nominalCounts.length; i++) {
      int est = origs.nominalCounts[i] / 2 - 1;
      assertTrue("Counts for value:" + i 
             + " orig:" + origs.nominalCounts[i] 
             + " out50%:" + outs.nominalCounts[i], 
             (est <= outs.nominalCounts[i]) &&
             (outs.nominalCounts[i] <= (est + 3)));
    }
  }

  public void testBiasToUniform() throws Exception {
    m_Instances.setClassIndex(1);
    AttributeStats origs = m_Instances.attributeStats(1);
    assertNotNull(origs.nominalCounts);
    
    ((Resample)m_Filter).setBiasToUniformClass(1.0);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    AttributeStats outs = result.attributeStats(1);

    // Check distributions are pretty similar
    assertNotNull(outs.nominalCounts);
    assertEquals(origs.nominalCounts.length, outs.nominalCounts.length);
    int est = (origs.totalCount - origs.missingCount) / origs.distinctCount;
    est = est / 2 - 1;
    for (int i = 0; i < origs.nominalCounts.length; i++) {
      assertTrue("Counts for value:" + i 
             + " orig:" + origs.nominalCounts[i] 
             + " out50%:" + outs.nominalCounts[i]
             + " ~wanted:" + est,
             (est <= outs.nominalCounts[i]) &&
             (outs.nominalCounts[i] <= (est + 3)));
    }
  }

  public static Test suite() {
    return new TestSuite(ResampleTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
