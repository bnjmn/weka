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
 * Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.attribute;

import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Tests Transpose. Run from the command line with:<p>
 * java weka.filters.unsupervised.attribute.TransposeTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class TransposeTest extends AbstractFilterTest {
  
  public TransposeTest(String name) { 
    super(name);
  }

  /** Creates a default Transpose */
  public Filter getFilter() {
    return new Transpose();
  }

  protected void setUp() throws Exception {
    m_Filter             = getFilter();
    m_Instances          = new Instances(new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("weka/filters/data/FilterTest.arff"))));
    Remove r = new Remove();
    r.setAttributeIndices("1, 2, 4, 5");
    r.setInputFormat(m_Instances);
    m_Instances = Filter.useFilter(m_Instances, r);
    m_OptionTester       = getOptionTester();
    m_GOETester          = getGOETester();
    m_FilteredClassifier = null;
  }

  /** This filter does not support batch filtering. */
  public void testBatchFiltering() {
    return;
  }
  public void testBatchFilteringSmaller() {
    return;
  }
  public void testBatchFilteringLarger() {
    return;
  }
  public void testChangesInputData() { return; }

  public void testTypical() {
    Instances result = useFilter();
    // Number of instances should be number of attributes
    assertEquals(m_Instances.numAttributes(), result.numInstances());
  }

  public static Test suite() {
    return new TestSuite(TransposeTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
