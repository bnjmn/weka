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
 * Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.attribute;

import weka.core.Attribute;
import weka.core.Instances;
import weka.core.TestInstances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;
import weka.classifiers.meta.FilteredClassifier;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests MultiInstanceWrapper. Run from the command line with:<p>
 * java weka.filters.unsupervised.attribute.MultiInstanceWrapperTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class MultiInstanceWrapperTest
  extends AbstractFilterTest {
  
  public MultiInstanceWrapperTest(String name) { 
    super(name);
  }

  /** Creates a default filter */
  public Filter getFilter() {
    return new MultiInstanceWrapper();
  }

  /**
   * returns the configured FilteredClassifier. Since the base classifier is
   * determined heuristically, derived tests might need to adjust it.
   * 
   * @return the configured FilteredClassifier
   */
  protected FilteredClassifier getFilteredClassifier() {
    FilteredClassifier	result;
    
    result = new FilteredClassifier();
    
    result.setFilter(getFilter());
    result.setClassifier(new weka.classifiers.rules.ZeroR());
    
    return result;
  }
  
  /**
   * returns data generated for the FilteredClassifier test
   * 
   * @return		the dataset for the FilteredClassifier
   * @throws Exception	if generation of data fails
   */
  protected Instances getFilteredClassifierData() throws Exception{
    TestInstances	test;
    Instances		result;

    test = TestInstances.forCapabilities(m_FilteredClassifier.getCapabilities());
    test.setClassIndex(TestInstances.CLASS_IS_LAST);

    result = test.generate();
    
    return result;
  }

  /**
   * Create a multi-instance dataset to work with
   */
  protected void setUp() throws Exception {
    super.setUp();

    TestInstances test = new TestInstances();
    test.setNumNominal(1);
    test.setClassType(Attribute.NOMINAL);
    test.setMultiInstance(true);
    m_Instances = test.generate();
  }

  public void testTypical() {
    m_Filter = getFilter();
    Instances result = useFilter();

    assertEquals(m_Instances.numInstances(), result.numInstances());
  }
  
  public static Test suite() {
    return new TestSuite(MultiInstanceWrapperTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
