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
 * Copyright (C) 2006 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.attribute;

import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.TestInstances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests MultiInstanceToPropositional. Run from the command line with: <p/>
 * java weka.filters.unsupervised.attribute.MultiInstanceToPropositionalTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class MultiInstanceToPropositionalTest 
  extends AbstractFilterTest {
  
  public MultiInstanceToPropositionalTest(String name) { 
    super(name);  
  }

  /** Creates a default MultiInstanceToPropositional */
  public Filter getFilter() {
    return new MultiInstanceToPropositional();
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
   * Called by JUnit before each test method. This implementation creates
   * the default filter to test and loads a test set of Instances.
   *
   * @throws Exception if an error occurs reading the example instances.
   */
  protected void setUp() throws Exception {
    super.setUp();

    TestInstances test = new TestInstances();
    test.setNumNominal(1);
    test.setClassType(Attribute.NOMINAL);
    test.setMultiInstance(true);
    m_Instances = test.generate();
  }

  /**
   * performs a typical test
   */
  public void testTypical() {
    Instances icopy = new Instances(m_Instances);
    Instances result = useFilter();
    // # of instances
    int count = 0;
    for (int i = 0; i < icopy.numInstances(); i++)
      count += icopy.instance(i).relationalValue(1).numInstances();
    assertEquals(result.numInstances(), count);
    // # of attributes
    count =   icopy.numAttributes() 
            + icopy.attribute(1).relation().numAttributes()
            - 1;
    assertEquals(result.numAttributes(), count);
  }
  
  /**
   * filter cannot be used in conjunction with the FilteredClassifier, since
   * it can produce more than one instance out of a bag. This will of course 
   * not with the distributionForInstance/classifyInstance methods.
   */
  public void testFilteredClassifier() {
    // nothing
  }

  public static Test suite() {
    return new TestSuite(MultiInstanceToPropositionalTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
