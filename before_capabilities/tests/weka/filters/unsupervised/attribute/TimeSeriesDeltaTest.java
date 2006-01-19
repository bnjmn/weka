/*
 * Copyright (C) 2002 University of Waikato 
 */

package weka.filters.unsupervised.attribute;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;
import weka.core.Instance;
import weka.filters.Filter;
import weka.filters.AbstractTimeSeriesFilterTest;

/**
 * Tests TimeSeriesDelta. Run from the command line with:<p>
 * java weka.filters.TimeSeriesDeltaTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 1.2 $
 */
public class TimeSeriesDeltaTest extends AbstractTimeSeriesFilterTest {

  public TimeSeriesDeltaTest(String name) { super(name);  }

  /** Creates a default TimeSeriesTranslateFilter */
  public Filter getFilter() {
    return getFilter("3");
  }

  /** Creates a specialized TimeSeriesTranslateFilter */
  public Filter getFilter(String rangelist) {
    
    TimeSeriesDelta af = new TimeSeriesDelta();
    af.setAttributeIndices(rangelist);
    return af;
  }

  public void testInverted() {
    m_Filter = getFilter("1,2,3,4,5,6");
    ((TimeSeriesDelta)m_Filter).setInvertSelection(true);
    Instances result = useFilter();
    // Number of attributes shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances() - 1, result.numInstances());
    // Check conversion looks OK
    for (int i = 0; i < result.numInstances(); i++) {
      Instance in = m_Instances.instance(i + 1);
      Instance out = result.instance(i);
      for (int j = 0; j < result.numAttributes(); j++) {
        if ((j != 4) && (j != 5) && (j != 6)) {
          if (in.isMissing(j)) {
            assertTrue("Nonselected missing values should pass through",
                   out.isMissing(j));
          } else if (result.attribute(j).isString()) {
            assertEquals("Nonselected attributes shouldn't change. "
                         + in + " --> " + out,
                         m_Instances.attribute(j).value((int)in.value(j)),
                         result.attribute(j).value((int)out.value(j)));
          } else {
            assertEquals("Nonselected attributes shouldn't change. "
                         + in + " --> " + out,
                         in.value(j),
                         out.value(j), TOLERANCE);
          }
        }
      }
    }    
  }

  public static Test suite() {
    return new TestSuite(TimeSeriesDeltaTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
