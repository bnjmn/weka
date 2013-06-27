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

import weka.core.Instances;
import weka.core.Attribute;
import weka.core.SelectedTag;

import weka.filters.unsupervised.attribute.RemoveType;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests SupervisedAttributeScaler. Run from the command line with:<p>
 * java weka.filters.supervised.attribute.SupervisedAttributeScalerTest
 *
 * @author Eibe Frank
 * @version $Revision: 8034 $
 */
public class SupervisedAttributeScalerTest extends AbstractFilterTest {
  
  /**
   * Initializes the test.
   * 
   * @param name	the name of the test
   */
  public SupervisedAttributeScalerTest(String name) {

    super(name);
  }

  /**
   * Creates an default SupervisedAttributeScaler.
   * 
   * @return		the default filter.
   */
  public Filter getFilter() {

    SupervisedAttributeScaler f = new SupervisedAttributeScaler();
    return f;
  }

  /** Need to remove non-nominal attributes, set class index */
  protected void setUp() throws Exception {
    super.setUp();

    // class index
    m_Instances.setClassIndex(1);

    // Only keep numeric attributes
    RemoveType af = new RemoveType();
    af.setInvertSelection(true);
    af.setAttributeType(new SelectedTag(Attribute.NUMERIC,
                                        RemoveType.TAGS_ATTRIBUTETYPE));
    af.setInputFormat(m_Instances);
    m_Instances = Filter.useFilter(m_Instances, af);

    // Replace missing values
    ReplaceMissingValues rpv = new ReplaceMissingValues();
    rpv.setInputFormat(m_Instances);
    m_Instances = Filter.useFilter(m_Instances, rpv);
  }

  /**
   * Tests default setup.
   */
  public void testTypical() {

    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
  }
  
  /**
   * tests the filter in conjunction with the FilteredClassifier
   */
  public void testFilteredClassifier() {
    Instances		data;
    int			i;
    
    // skip this test if a subclass has set the
    // filtered classifier to null
    if (m_FilteredClassifier == null) {
      return;
    }
    
    try {
      // generate data
      data = getFilteredClassifierData();

      // Only keep numeric attributes
      RemoveType af = new RemoveType();
      af.setInvertSelection(true);
      af.setAttributeType(new SelectedTag(Attribute.NUMERIC,
                                          RemoveType.TAGS_ATTRIBUTETYPE));
      af.setInputFormat(data);
      data = Filter.useFilter(data, af);
      
      // Replace missing values
      ReplaceMissingValues rpv = new ReplaceMissingValues();
      rpv.setInputFormat(data);
      data = Filter.useFilter(data, rpv);
      
      // build classifier
      m_FilteredClassifier.buildClassifier(data);

      // test classifier
      for (i = 0; i < data.numInstances(); i++) {
	m_FilteredClassifier.classifyInstance(data.instance(i));
      }
    }
    catch (Exception e) {
      fail("Problem with FilteredClassifier: " + e.toString());
    }
  }

  /**
   * Returns a test suite.
   * 
   * @return		the suite
   */
  public static Test suite() {
    return new TestSuite(SupervisedAttributeScalerTest.class);
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
