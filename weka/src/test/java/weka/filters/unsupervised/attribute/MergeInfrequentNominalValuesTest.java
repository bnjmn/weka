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

package weka.filters.unsupervised.attribute;

import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests MergeInfrequentNominalValues. Run from the command line with:<p>
 * java weka.filters.unsupervised.attribute.MergeInfrequentNominalValuesTest
 *
 * @author Len Trigg (original MergeTwoValues code)
 * @author Eibe Frank
 * @version $Revision: 8034 $
 */
public class MergeInfrequentNominalValuesTest
  extends AbstractFilterTest {
  
  /**
   * Initializes the test.
   * 
   * @param name	the name of the test
   */
  public MergeInfrequentNominalValuesTest(String name) {
    super(name);
  }

  /**
   * Creates an default MergeInfrequentNominalValues.
   * 
   * @return		the default filter.
   */
  public Filter getFilter() {
    MergeInfrequentNominalValues f = new MergeInfrequentNominalValues();
    return f;
  }

  /**
   * Tests default setup.
   */
  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
  }

  /**
   * Tests a different range.
   */
  public void testDifferentRange() {
    ((MergeInfrequentNominalValues)m_Filter).setMinimumFrequency(5);
    ((MergeInfrequentNominalValues)m_Filter).setInvertSelection(true);
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    // Check that the merging is correct
    int mergedIndex = -1;
    for (int i = 0; i < result.numInstances(); i++) {
      if ((m_Instances.instance(i).value(4) == 1) || 
          (m_Instances.instance(i).value(4) == 2)) {
        if (mergedIndex == -1) {
          mergedIndex = (int)result.instance(i).value(4);
        } else {
          assertEquals("Checking merged value for instance: " + (i + 1),
                       mergedIndex, (int)result.instance(i).value(4));
        }
      }
    }
  }

  /**
   * Test merging all labels.
   */
  public void testMergeAll() {
    ((MergeInfrequentNominalValues)m_Filter).setMinimumFrequency(100);
    ((MergeInfrequentNominalValues)m_Filter).setInvertSelection(true);
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    assertEquals(1, result.attribute(1).numValues());
    assertEquals(1, result.attribute(4).numValues());
  }

  /**
   * Tests attribute with missing values.
   */
  public void testAttributeWithMissing() {
    ((MergeInfrequentNominalValues)m_Filter).setAttributeIndices("5");
    ((MergeInfrequentNominalValues)m_Filter).setMinimumFrequency(100);
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    // Check that the merging is correct
    int mergedIndex = -1;
    for (int i = 0; i < result.numInstances(); i++) {
      if (m_Instances.instance(i).isMissing(4)) {
        assertTrue("Missing in input should give missing in result",
               result.instance(i).isMissing(4));
      } else if ((m_Instances.instance(i).value(4) == 1) || 
          (m_Instances.instance(i).value(4) == 2)) {
        if (mergedIndex == -1) {
          mergedIndex = (int)result.instance(i).value(4);
        } else {
          assertEquals("Checking merged value for instance: " + (i + 1),
                       mergedIndex, (int)result.instance(i).value(4));
        }
      }
    }
  }
  
  /**
   * tests the filter in conjunction with the FilteredClassifier
   */
  public void testFilteredClassifier() {
    try {
      Instances data = getFilteredClassifierData();

      for (int i = 0; i < data.numAttributes(); i++) {
	if (data.classIndex() == i)
	  continue;
	if (data.attribute(i).isNominal()) {
	  ((MergeInfrequentNominalValues) m_FilteredClassifier.getFilter()).setAttributeIndices(
	      "" + (i + 1));
	  break;
	}
      }
    }
    catch (Exception e) {
      fail("Problem setting up test for FilteredClassifier: " + e.toString());
    }
    
    super.testFilteredClassifier();
  }

  /**
   * Returns a test suite.
   * 
   * @return		the suite
   */
  public static Test suite() {
    return new TestSuite(MergeInfrequentNominalValuesTest.class);
  }

  /**
   * Runs the test from commandline.
   * 
   * @param args	ignored
   */
  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
