/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 * Copyright (C) 2006 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.supervised.attribute;

import weka.core.Attribute;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.core.TestInstances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests PLSFilter. Run from the command line with: <p/>
 * java weka.filters.supervised.attribute.PLSFilterTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.3 $
 */
public class PLSFilterTest 
  extends AbstractFilterTest {

  /** the default number of attributes to generate (apart from class) */
  protected final static int NUM_ATTS = 5;
  
  /** the number of numeric attributes in the test dataset */
  protected final static int NUM_NUMERIC_ATTS = 20;
  
  public PLSFilterTest(String name) { 
    super(name);  
  }

  /** 
   * Creates a default PLSFilter
   * 
   * @return		the configured filter
   */
  public Filter getFilter() {
    return getFilter(NUM_ATTS, PLSFilter.ALGORITHM_PLS1);
  }

  /** 
   * Creates a PLSFilter according to the parameters
   * 
   * @param numAtts	the number of attributes to generate
   * @param algorithm	the algorithm to use
   * @return		the configured filter
   */
  public Filter getFilter(int numAtts, int algorithm) {
    PLSFilter filter = new PLSFilter();
    
    filter.setNumComponents(numAtts);
    filter.setReplaceMissing(true);
    filter.setPreprocessing(new SelectedTag(PLSFilter.PREPROCESSING_CENTER, PLSFilter.TAGS_PREPROCESSING));
    filter.setAlgorithm(new SelectedTag(algorithm, PLSFilter.TAGS_ALGORITHM));

    return filter;
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

    test = new TestInstances();
    test.setNumNominal(0);
    test.setNumNumeric(NUM_NUMERIC_ATTS);
    test.setClassType(Attribute.NUMERIC);

    result = test.generate();
    
    return result;
  }

  /**
   * Called by JUnit before each test method. This implementation creates
   * the default filter to test and generates a test set of Instances.
   *
   * @throws Exception if an error occurs reading the example instances.
   */
  protected void setUp() throws Exception {
    super.setUp();

    TestInstances test = new TestInstances();
    test.setNumNominal(0);
    test.setNumNumeric(NUM_NUMERIC_ATTS);
    test.setClassType(Attribute.NUMERIC);
    m_Instances = test.generate();
  }
  
  /**
   * performs a test
   * 
   * @param algorithm	the algorithm to use
   */
  protected void performTest(int algorithm) {
    Instances icopy = new Instances(m_Instances);
    
    m_Filter = getFilter(NUM_ATTS, algorithm);
    Instances result = useFilter();
    assertEquals(result.numAttributes(), NUM_ATTS + 1);
    assertEquals(result.numInstances(), icopy.numInstances());
    
    m_Filter = getFilter(NUM_ATTS*2, algorithm);
    result = useFilter();
    assertEquals(result.numAttributes(), NUM_ATTS*2 + 1);
    assertEquals(result.numInstances(), icopy.numInstances());
  }

  /**
   * performs a test on PLS1
   */
  public void testPLS1() {
    performTest(PLSFilter.ALGORITHM_PLS1);
  }

  /**
   * performs a test on SIMPLS
   */
  public void testSIMPLS() {
    performTest(PLSFilter.ALGORITHM_SIMPLS);
  }

  public static Test suite() {
    return new TestSuite(PLSFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
