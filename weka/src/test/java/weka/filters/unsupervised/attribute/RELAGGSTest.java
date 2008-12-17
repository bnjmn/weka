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

import weka.core.Instances;
import weka.core.TestInstances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests RELAGGS. Run from the command line with:<p>
 * java weka.filters.unsupervised.attribute.RELAGGSTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.1 $
 */
public class RELAGGSTest
  extends AbstractFilterTest {
  
  public RELAGGSTest(String name) { 
    super(name);
  }

  /**
   * Create a dataset with relational attributes to work on
   */
  protected void setUp() throws Exception {
    TestInstances	test;

    super.setUp();

    test = new TestInstances();
    
    test.setNumDate(1);
    test.setNumNominal(1);
    test.setNumNominalValues(10);
    test.setNumNumeric(1);
    test.setNumRelational(2);
    test.setNumString(0);
    test.setNumRelationalDate(2);
    test.setNumRelationalNominal(2);
    test.setNumRelationalNumeric(2);
    test.setNumRelationalString(0);
    test.setNumRelationalNominalValues(10);

    m_Instances = test.generate();
  }

  /** Creates a default RELAGGS */
  public Filter getFilter() {
    return new RELAGGS();
  }
  
  /**
   * returns data generated for the FilteredClassifier test
   * 
   * @return		the dataset for the FilteredClassifier
   * @throws Exception	if generation of data fails
   */
  protected Instances getFilteredClassifierData() throws Exception {
    return m_Instances;
  }

  public void testTypical() {
    m_Filter = getFilter();
    Instances result = useFilter();

    // explanation of "(-1 + (2 + 2)*5 + 2*10)*2"
    // -1: rel. attribute gets removed
    // (2 + 2)*5: 2 date and 2 numeric each generate 5 attributes
    // 2*10: 2 nominal attributes with 10 values
    // (...)*2: 2 relational attributes
    assertEquals(m_Instances.numAttributes() + (-1 + (2 + 2)*5 + 2*10)*2, result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
  }
  
  /**
   * tests the max. cardinality parameter
   */
  public void testMaxCardinality() {
    m_Filter = getFilter();
    ((RELAGGS) m_Filter).setMaxCardinality(5);  // this skips the nominal atts. with 10 values
    Instances result = useFilter();

    // explanation of "(-1 + (2 + 2)*5)*2"
    // -1: rel. attribute gets removed
    // (2 + 2)*5: 2 date and 2 numeric each generate 5 attributes
    // (...)*2: 2 relational attributes
    assertEquals(m_Instances.numAttributes() + (-1 + (2 + 2)*5)*2, result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
  }

  /**
   * tests whether we can handle data that contains NO relational attribute
   * at all
   */
  public void testNoRelationalAttribute() {
    m_Filter = getFilter();
    
    // delete the rel. attributes
    int i = 0;
    while (i < m_Instances.numAttributes()) {
      if (m_Instances.attribute(i).isRelationValued())
	m_Instances.deleteAttributeAt(i);
      else
	i++;
    }
    
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
  }
  
  public static Test suite() {
    return new TestSuite(RELAGGSTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
