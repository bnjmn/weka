/*
 * Copyright 2000 Webmind Inc. 
 */

package weka.filters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;
import weka.core.Instance;

/**
 * Tests TimeSeriesTranslateFilter. Run from the command line with:<p>
 * java weka.filters.TimeSeriesTranslateFilterTest
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.2 $
 */
public abstract class AbstractTimeSeriesFilterTest extends AbstractFilterTest {

  /** Tolerance allowed in double comparisons */
  protected static final double TOLERANCE = 0.001;

  public AbstractTimeSeriesFilterTest(String name) { super(name);  }

  /** Creates a default TimeSeriesTranslateFilter */
  abstract public Filter getFilter();

  public void testDefault() {
    testInstanceRange_X(((TimeSeriesTranslateFilter)m_Filter).getInstanceRange());
  }

  public void testInstanceRange() {

    testInstanceRange_X(-5);
    testInstanceRange_X(-2);
    testInstanceRange_X(2);
    testInstanceRange_X(5);
  }

  public void testFillWithMissing() {

    ((TimeSeriesTranslateFilter)m_Filter).setFillWithMissing(true);
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
    // Check conversion looks OK
    for (int i = 0; i < result.numInstances(); i++) {
      Instance in = m_Instances.instance(i);
      Instance out = result.instance(i);
      for (int j = 0; j < result.numAttributes(); j++) {
        if ((j != 1) && (j != 2)) {
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

  private void testInstanceRange_X(int range) {
    ((TimeSeriesTranslateFilter)m_Filter).setInstanceRange(range);
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances() - Math.abs(range), result.numInstances());
    // Check conversion looks OK
    for (int i = 0; i < result.numInstances(); i++) {
      Instance in = m_Instances.instance(i - ((range > 0) ? 0 : range));
      Instance out = result.instance(i);
      for (int j = 0; j < result.numAttributes(); j++) {
        if ((j != 1) && (j != 2)) {
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
    return new TestSuite(TimeSeriesTranslateFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
