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
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.attribute;

import weka.core.Instances;
import weka.core.TestInstances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests MILESFilter. Run from the command line with:<p>
 * java weka.filters.unsupervised.attribute.MILESFilterTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class MILESFilterTest
  extends AbstractFilterTest {
  
  public MILESFilterTest(String name) { 
    super(name);
  }

  /**
   * Create a multi-instance dataset to work with
   */
  protected void setUp() throws Exception {
    TestInstances	test;

    super.setUp();

    test = new TestInstances();
    
    test.setNumNominal(1);
    test.setNumNominalValues(10);
    test.setNumRelational(1);
    test.setNumString(0);
    test.setNumRelationalDate(2);
    test.setNumRelationalNominal(2);
    test.setNumRelationalNumeric(2);
    test.setNumRelationalString(0);
    test.setNumRelationalNominalValues(10);

    m_Instances = test.generate();
  }

  /** Creates a default MILESFilter */
  public Filter getFilter() {
    return new MILESFilter();
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

    assertEquals(m_Instances.numInstances(), result.numInstances());
  }
  
  public static Test suite() {
    return new TestSuite(MILESFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
