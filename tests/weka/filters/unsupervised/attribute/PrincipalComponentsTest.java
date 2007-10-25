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
 * Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.attribute;

import weka.core.Attribute;
import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Tests PrincipalComponents. Run from the command line with: <p/>
 * java weka.filters.unsupervised.attribute.PrincipalComponentsTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.1 $
 */
public class PrincipalComponentsTest 
  extends AbstractFilterTest {
  
  /**
   * Initializes the test.
   * 
   * @param name	the name of the test
   */
  public PrincipalComponentsTest(String name) { 
    super(name);  
  }

  /**
   * Need to remove non-numeric attributes.
   * 
   * @throws Exception	if something goes wrong in setup
   */
  protected void setUp() throws Exception {
    super.setUp();

    m_Instances.deleteAttributeType(Attribute.STRING);
    
    // class index
    m_Instances.setClassIndex(1);
  }

  /**
   * Creates a default PrincipalComponents filter.
   * 
   * @return		the default filter
   */
  public Filter getFilter() {
    return new PrincipalComponents();
  }

  /**
   * performs the actual test.
   */
  protected void performTest() {
    Instances icopy = new Instances(m_Instances);
    Instances result = null;
    try {
      m_Filter.setInputFormat(icopy);
    } 
    catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on setInputFormat(): \n" + ex.getMessage());
    }
    try {
      result = Filter.useFilter(icopy, m_Filter);
      assertNotNull(result);
    } 
    catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on useFilter(): \n" + ex.getMessage());
    }

    assertEquals(icopy.numInstances(), result.numInstances());
  }

  /**
   * Only tests whether the number of instances stay the same, with default 
   * setup of filter.
   */
  public void testTypical() {
    m_Filter = getFilter();
    performTest();
  }

  /**
   * Runs filter with no normalization.
   */
  public void testNoNormalization() {
    m_Filter = getFilter();
    ((PrincipalComponents) m_Filter).setNormalize(false);
    performTest();
  }

  /**
   * Runs filter with different variance.
   */
  public void testVariance() {
    m_Filter = getFilter();
    ((PrincipalComponents) m_Filter).setVarianceCovered(0.8);
    performTest();
  }

  /**
   * Runs filter with a maximum number of attributes.
   */
  public void testMaxAttributes() {
    m_Filter = getFilter();
    ((PrincipalComponents) m_Filter).setMaximumAttributeNames(2);
    performTest();
  }
  
  /**
   * Returns a configures test suite.
   * 
   * @return		a configured test suite
   */
  public static Test suite() {
    return new TestSuite(PrincipalComponentsTest.class);
  }

  /**
   * For running the test from commandline.
   * 
   * @param args	ignored
   */
  public static void main(String[] args){
    TestRunner.run(suite());
  }
}
