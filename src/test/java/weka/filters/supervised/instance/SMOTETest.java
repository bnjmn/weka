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
 * Copyright (C) 2008 University of Waikato Hamilton, New Zealand
 */

package weka.filters.supervised.instance;

import weka.core.Instances;
import weka.core.TestInstances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests SMOTE. Run from the command line with: <p/>
 * java weka.filters.supervised.instance.SMOTETest
 *
 * @author fracpete (FracPete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class SMOTETest
  extends AbstractFilterTest {
  
  /**
   * Initializes the test.
   * 
   * @param name	the name of the test
   */
  public SMOTETest(String name) {
    super(name);
  }

  /**
   * Need to set the class index
   * 
   * @throws Exception 	if setup fails
   */
  protected void setUp() throws Exception {
    super.setUp();
    m_Instances.setClassIndex(1);
  }
  
  /**
   * returns data generated for the FilteredClassifier test
   * 
   * @return		the dataset for the FilteredClassifier
   * @throws Exception	if generation of data fails
   */
  protected Instances getFilteredClassifierData() throws Exception {
    TestInstances	test;
    Instances		result;

    // NB: in order to make sure that the classifier can handle the data,
    //     we're using the classifier's capabilities to generate the data.
    test = TestInstances.forCapabilities(
  	m_FilteredClassifier.getClassifier().getCapabilities());
    test.setNumInstances(40);
    test.setClassIndex(TestInstances.CLASS_IS_LAST);

    result = test.generate();
    
    return result;
  }

  /**
   * Creates a default SMOTE
   * 
   * @return		the default filter
   */
  public Filter getFilter() {
    SMOTE f = new SMOTE();
    return f;
  }

  /**
   * Returns a test suite.
   * 
   * @return		the test suite
   */
  public static Test suite() {
    return new TestSuite(SMOTETest.class);
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
