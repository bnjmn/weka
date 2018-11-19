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

import weka.core.Attribute;
import weka.core.Instances;
import weka.core.TestInstances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests PropositionalToMultiInstance. Run from the command line with: <p/>
 * java weka.filters.unsupervised.attribute.PropositionalToMultiInstanceTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class PropositionalToMultiInstanceTest 
  extends AbstractFilterTest {
  
  public PropositionalToMultiInstanceTest(String name) { 
    super(name);  
  }

  /** Creates a default PropositionalToMultiInstance */
  public Filter getFilter() {
    return new PropositionalToMultiInstance();
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
    test.setNumNominal(2);
    test.setClassType(Attribute.NOMINAL);
    test.setNumInstances(400);
    m_Instances = test.generate();
    for (int i = 0; i < m_Instances.numInstances(); i++) {
      m_Instances.instance(i).setClassValue(((int) m_Instances.instance(i).value(0) % m_Instances.numClasses()));
    }
  }

  /**
   * performs a typical test
   */
  public void testTypical() {
    Instances icopy = new Instances(m_Instances);
    Instances result = useFilter();
    // # of instances
    int count = 0;
    for (int i = 0; i < result.numInstances(); i++)
      count += result.instance(i).relationalValue(1).numInstances();
    assertEquals(icopy.numInstances(), count);
    // # of attributes
    count =   result.numAttributes() 
            + result.attribute(1).relation().numAttributes()
            - 1;
    assertEquals(icopy.numAttributes(), count);
  }
  
  /**
   * filter cannot be used in conjunction with the FilteredClassifier, since
   * it makes no sense creating bags containing only one instance in the
   * distributionForInstance/classifyInstance methods.
   */
  public void testFilteredClassifier() {
    // nothing
  }

  public static Test suite() {
    return new TestSuite(PropositionalToMultiInstanceTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
