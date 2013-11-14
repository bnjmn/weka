/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Copyright (C) 2013 University of Waikato 
 */

package weka.filters.supervised.attribute;

import junit.framework.Test;
import junit.framework.TestSuite;
import weka.core.Attribute;
import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

/**
 * Tests MergeNominalValues. Run from the command line with:
 * <p>
 * java weka.filters.unsupervised.attribute.MergeNominalValuesTest
 * 
 * @author Eibe Frank
 * @version $Revision$
 */
public class MergeNominalValuesTest extends AbstractFilterTest {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m_Instances.setClassIndex(1);
  }

  /**
   * Initializes the test.
   * 
   * @param name the name of the test
   */
  public MergeNominalValuesTest(String name) {
    super(name);
  }

  /**
   * Creates an default MergeInfrequentNominalValues.
   * 
   * @return the default filter.
   */
  @Override
  public Filter getFilter() {
    MergeNominalValues f = new MergeNominalValues();
    return f;
  }

  /**
   * Tests default setup.
   */
  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());

    Attribute mergedAtt = result.attribute(4);
    // All values should be merged for this attribute
    assertTrue("Attribute 5 has all values merged in result", mergedAtt
      .value(0).equals("a_or_b_or_c_or_d"));
  }

  /**
   * Tests a different range.
   */
  public void testDifferentRange() {
    // ((MergeNominalValues)m_Filter).setMinimumFrequency(5);
    ((MergeNominalValues) m_Filter).setAttributeIndices("1,3");
    ((MergeNominalValues) m_Filter).setInvertSelection(true);
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
  }

  /**
   * Tests attribute with missing values.
   */
  public void testAttributeWithMissing() {
    ((MergeNominalValues) m_Filter).setAttributeIndices("5");
    // ((MergeNominalValues)m_Filter).setMinimumFrequency(100);
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
    for (int i = 0; i < result.numInstances(); i++) {
      if (m_Instances.instance(i).isMissing(4)) {
        assertTrue("Missing in input should give missing in result", result
          .instance(i).isMissing(4));
      }
    }
  }

  /**
   * tests the filter in conjunction with the FilteredClassifier
   */
  @Override
  public void testFilteredClassifier() {
    try {
      Instances data = getFilteredClassifierData();

      for (int i = 0; i < data.numAttributes(); i++) {
        if (data.classIndex() == i) {
          continue;
        }
        if (data.attribute(i).isNominal()) {
          ((MergeNominalValues) m_FilteredClassifier.getFilter())
            .setAttributeIndices("" + (i + 1));
          break;
        }
      }
    } catch (Exception e) {
      fail("Problem setting up test for FilteredClassifier: " + e.toString());
    }

    super.testFilteredClassifier();
  }

  /**
   * Returns a test suite.
   * 
   * @return the suite
   */
  public static Test suite() {
    return new TestSuite(MergeNominalValuesTest.class);
  }

  /**
   * Runs the test from commandline.
   * 
   * @param args ignored
   */
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
