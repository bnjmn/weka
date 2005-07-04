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
 * Tests SpreadSubsample. Run from the command line with:<p>
 * java weka.filters.SpreadSubsampleTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 1.2 $
 */
public class SpreadSubsampleTest extends AbstractFilterTest {

  private static double TOLERANCE = 0.001;

  public SpreadSubsampleTest(String name) { super(name);  }

  /** Creates a default SpreadSubsample */
  public Filter getFilter() {
    SpreadSubsample f = new SpreadSubsample();
    f.setDistributionSpread(0);
    return f;
  }

  /** Remove string attributes from default fixture instances */
  protected void setUp() throws Exception {

    super.setUp();
    m_Instances.setClassIndex(1);
  }

  public void testDistributionSpread() throws Exception {
    
    testDistributionSpread_X(1.0);
    testDistributionSpread_X(2.0);
    testDistributionSpread_X(3.0);
  }

  public void testAdjustWeights() {

    ((SpreadSubsample)m_Filter).setAdjustWeights(true);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    double origWeight = 0;
    for (int i = 0; i < m_Instances.numInstances(); i++) {
      origWeight += m_Instances.instance(i).weight();
    }
    double outWeight = 0;
    for (int i = 0; i < result.numInstances(); i++) {
      outWeight += result.instance(i).weight();
    }
    assertEquals(origWeight, outWeight, TOLERANCE);
  }

  private void testDistributionSpread_X(double factor) throws Exception {
    AttributeStats origs = m_Instances.attributeStats(1);
    assertNotNull(origs.nominalCounts);
    
    ((SpreadSubsample)m_Filter).setDistributionSpread(factor);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    AttributeStats outs = result.attributeStats(1);

    // Check distributions are pretty similar
    assertNotNull(outs.nominalCounts);
    assertEquals(origs.nominalCounts.length, outs.nominalCounts.length);
    int min = outs.nominalCounts[0];
    int max = outs.nominalCounts[0];
    for (int i = 1; i < outs.nominalCounts.length; i++) {
      if (outs.nominalCounts[i] < min) {
        min = outs.nominalCounts[i];
      }
      if (outs.nominalCounts[i] > max) {
        max = outs.nominalCounts[i];
      }
    }
    assertTrue(max / factor <= min);
  }

  public static Test suite() {
    return new TestSuite(SpreadSubsampleTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
