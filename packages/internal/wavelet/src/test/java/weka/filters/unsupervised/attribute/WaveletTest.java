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

import weka.core.Instances;
import weka.core.SelectedTag;
import weka.core.TestInstances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests Wavelet. Run from the command line with:<p>
 * java weka.filters.unsupervised.attribute.WaveletTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class WaveletTest extends AbstractFilterTest {
  
  public WaveletTest(String name) { 
    super(name);
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
    int		i;
    
    super.setUp();
    
    // set class attribute
    m_Instances.setClassIndex(2);
    
    // delete all non-numeric attribute
    i = 0;
    while (i < m_Instances.numAttributes()) {
      if (m_Instances.classIndex() == i) {
	i++;
	continue;
      }
      
      if (!m_Instances.attribute(i).isNumeric())
	m_Instances.deleteAttributeAt(i);
      else
	i++;
    }
  }

  /** Creates a default Wavelet */
  public Filter getFilter() {
    return getFilter(Wavelet.ALGORITHM_HAAR, Wavelet.PADDING_ZERO);
  }
  
  /**
   * returns a custom filter
   * 
   * @param algorithm	the type of algorithm to use
   * @param padding	the type of padding to use
   * @return		the configured filter
   */
  protected Filter getFilter(int algorithm, int padding) {
    Wavelet	filter;
    
    filter = new Wavelet();
    filter.setAlgorithm(new SelectedTag(algorithm, Wavelet.TAGS_ALGORITHM));
    filter.setAlgorithm(new SelectedTag(padding, Wavelet.TAGS_PADDING));
    
    return filter;
  }

  /**
   * tests the HAAR algorithm
   */
  public void testTypicalHAAR() {
    Instances 	icopy;
    Instances 	result;

    m_Filter = getFilter(Wavelet.ALGORITHM_HAAR, Wavelet.PADDING_ZERO);
    icopy    = new Instances(m_Instances);

    // 1. with class attribute
    result = useFilter();
    // Number of attributes will be power of 2 + class, number of instances won't change
    assertEquals(Wavelet.nextPowerOf2(m_Instances.numAttributes()) + 1, result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());

    // 2. without class attribute
    m_Instances = new Instances(icopy);
    m_Instances.setClassIndex(-1);
    result = useFilter();
    // Number of attributes will be power of 2 + class, number of instances won't change
    assertEquals(Wavelet.nextPowerOf2(m_Instances.numAttributes()), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
  }

  public static Test suite() {
    return new TestSuite(WaveletTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
