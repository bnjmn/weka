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
 * SAXTransformerTest.java
 * Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.timeseries;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;
import weka.core.Instances;
import weka.core.Range;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;
import weka.filters.unsupervised.timeseries.SAXTransformer;

/**
 * Tests the {@link SAXTransformer} filter
 * 
 * @author Benjamin Weber ( benweber at student dot ethz dot ch )
 * @version $Revision: 1000 $
 */
public class SAXTransformerTest extends AbstractFilterTest {

  /**
   * Constructs the {@link SAXTransformerTest} with the name <code>name</code>
   * 
   * @param name name of the test
   */
  public SAXTransformerTest(String name) {
    super(name);
  }
  
  /**
   * Tests whether the SAX transformation calculates the correct values
   * 
   * @throws Exception if something goes wrong
   */
  public void testNumerics() throws Exception {
    checkEquivalence(getNumericsDataset10x20(),
        apply(getNumericsDataset(), getFilter(10, 20, "1")), "1");
    checkEquivalence(getNumericsDataset10x4(),
        apply(getNumericsDataset(), getFilter(10, 4, "1")), "1");
    checkEquivalence(getNumericsDataset7x20(),
        apply(getNumericsDataset(), getFilter(7, 20, "1")), "1");
    checkEquivalence(getNumericsDataset7x4(),
        apply(getNumericsDataset(), getFilter(7, 4, "1")), "1");
    
  }
  
  /**
   * Checks whether the test dataset conforms to the reference dataset on the
   * attribute ranges <code>range</code>
   * 
   * @param reference the reference dataset
   * @param test the test dataset
   * @param range the ranges where time series are to be found
   */
  private void checkEquivalence(Instances reference, Instances test, String range) {
    assertTrue("Headers are not equal!", reference.equalHeaders(test));
    assertEquals("Number of timeseries is not equal!",
        reference.numInstances(), test.numInstances());
    
    Range timeSeriesAttributes = new Range(range);
    timeSeriesAttributes.setUpper(reference.numAttributes());
    
    for (int att : timeSeriesAttributes.getSelection()) {
      assert(reference.attribute(att).isRelationValued());
      assert(test.attribute(att).isRelationValued());
      
      for (int i = 0; i < reference.numAttributes(); i++) {
        Instances referenceTimeSeries = reference.attribute(att).relation(
            (int) reference.get(i).value(att));
        Instances testTimeSeries = test.attribute(att).relation(
            (int) test.get(i).value(att));
        
        assertEquals("Number of attributes inside time series is not equal!",
            referenceTimeSeries.numAttributes(),
            testTimeSeries.numAttributes());
        assertEquals("Number of data points is not equal!",
            referenceTimeSeries.numInstances(),
            testTimeSeries.numInstances());
        
        for (int k = 0; k < referenceTimeSeries.numAttributes(); k++) {
          assert(referenceTimeSeries.attribute(k).isNominal());
          assert(testTimeSeries.attribute(k).isNominal());
      
          List<Object> referenceValues =
              Collections.list(referenceTimeSeries.attribute(k).enumerateValues());
          List<Object> testValues =
              Collections.list(testTimeSeries.attribute(k).enumerateValues());
      
          assertEquals("Nominal attribute values are not equal!",
              referenceValues, testValues);
          
          for (int m = 0; m < referenceTimeSeries.numInstances(); m++) {
            assertEquals("Values are not equal!",
                referenceTimeSeries.attribute(k).value((int) referenceTimeSeries.get(m).value(k)),
                testTimeSeries.attribute(k).value((int) testTimeSeries.get(m).value(k)));
          }   
        }
      }
      
      
    }
  }
  
  /**
   * Tests the SAX transformation on a simple time series dataset with different
   * parameters
   * 
   * @throws Exception if something goes wrong
   */
  public void testSimpleDataset() throws Exception {
    int[] ws = {1, 10, 7, 6};
    String[] ranges = {"", "3", "5", "3,5"};
    int[] as = {4, 20, 10, 26*26*26 + 1};
    for (int w : ws)
      for (String range : ranges)
        for (int a : as)
          doSimpleDatasetTest(w, a, range);
  }
  
  /**
   * Carries out the tests for the {@link #testSimpleDataset()} method
   */
  private void doSimpleDatasetTest(int w, int a, String range) throws Exception {
    m_Instances = getSimpleDataset();
    m_Filter = getFilter(w, a, range);
    testBuffered();
    m_Instances = getSimpleDataset();
    m_Filter = getFilter(w, a, range);
    testIncremental();
    m_Instances = getSimpleDataset();
    m_Filter = getFilter(w, a, range);
    testThroughput();
    m_Instances = getSimpleDataset();
    checkBasicProperties(m_Instances, apply(m_Instances, getFilter(w, a, range)), range);
  }

  /**
   * Checks that basic properties of the SAX transformation aren't violated</p>
   * 
   * Basic properties like that the number of instances hasn't changed etc</p>
   * 
   * @param input the dataset that was used for transformation
   * @param output the dataset that was produced during transformation
   * @param range the ranges where time series are to be found
   */
  private void checkBasicProperties(Instances input, Instances output,
      String range) {

    assertEquals("Number of attributes has changed!",
        input.numAttributes(), output.numAttributes());
    assertEquals("Number of instances has changed!",
        input.numInstances(), output.numInstances());
    
    Range nonTimeSeries = new Range(range);
    nonTimeSeries.setInvert(true);
    nonTimeSeries.setUpper(input.numAttributes() - 1);
    
    for (int i = 0; i < input.numAttributes(); i++) {
      
      if (nonTimeSeries.isInRange(i)) {
        assertEquals("Attribute type has changed!",
            input.attribute(i), output.attribute(i));
      }
      
      if (input.attribute(i).isRelationValued()) {
        Instances inputRelation = input.attribute(i).relation();
        Instances outputRelation = output.attribute(i).relation();
        
        assertEquals(
            "Number of attributes inside relational attribute has changed!",
            inputRelation.numAttributes(), outputRelation.numAttributes());
        
        for (int k = 0; k < inputRelation.numAttributes(); k++) {
          if (nonTimeSeries.isInRange(i)) {
            assertEquals("Attribute type inside relationl attribute has changed!",
                inputRelation.attribute(k), outputRelation.attribute(k));
          } else {
            assertTrue(outputRelation.attribute(k).isNominal());
          }
        }
      }
    }

    if (nonTimeSeries.getSelection().length == 0)
      return;
    
    for (int i = 0; i < input.numInstances(); i++) {
      for (int att : nonTimeSeries.getSelection()) {
        assertEquals("Non-timeseries value has changed!",
            input.get(i).value(att), output.get(i).value(att));
      }
    }
  }

  /**
   * Applies a filter on a dataset and returns the output
   * 
   * @param dataset the dataset to which the filter gets applied
   * @param filter the filter to be applied
   * @return the output dataset
   * @throws Exception if something goes wrong
   */
  private Instances apply(Instances dataset, Filter filter) throws Exception {
   filter.setInputFormat(dataset);
   return Filter.useFilter(dataset, filter);
  }

  /**
   * Gets a simple dataset with time series
   * 
   * @return a simple dataset with time series
   * @throws IOException if the dataset can't be loaded
   */
  private Instances getSimpleDataset() throws IOException {
    return new Instances(new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("weka/filters/unsupervised/timeseries/data/SimpleTimeSeries.arff"))));
  }

  /**
   * Gets a carefully prepared dataset that can be used to check whether the
   * correct values get calculated.
   * 
   * @return the prepared dataset
   * @throws IOException if the dataset can't be loaded
   */
  private Instances getNumericsDataset() throws IOException {
    return new Instances(new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("weka/filters/unsupervised/timeseries/data/NumericTimeSeries.arff"))));
  }

  /**
   * Gets the reference solution of the SAX transformation applied to the
   * numeric dataset (see {@link #getNumericsDataset()} with a parameter of
   * <code>W = 7</code> and an alphabet size of 4</p>
   * 
   * @return reference solution
   * @throws IOException if the reference solution can't be loaded
   */
  private Instances getNumericsDataset7x4() throws IOException {
    return new Instances(new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("weka/filters/unsupervised/timeseries/data/NumericTimeSeriesSAX7x4.ref"))));
  }

  /**
   * Gets the reference solution of the SAX transformation applied to the
   * numeric dataset (see {@link #getNumericsDataset()} with a parameter of
   * <code>W = 10</code> and an alphabet size of 20</p>
   * 
   * @return reference solution
   * @throws IOException if the reference solution can't be loaded
   */
  private Instances getNumericsDataset10x20() throws IOException {
    return new Instances(new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("weka/filters/unsupervised/timeseries/data/NumericTimeSeriesSAX10x20.ref"))));
  }
  
  /**
   * Gets the reference solution of the SAX transformation applied to the
   * numeric dataset (see {@link #getNumericsDataset()} with a parameter of
   * <code>W = 10</code> and an alphabet size of 4</p>
   * 
   * @return reference solution
   * @throws IOException if the reference solution can't be loaded
   */
  private Instances getNumericsDataset10x4() throws IOException {
    return new Instances(new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("weka/filters/unsupervised/timeseries/data/NumericTimeSeriesSAX10x4.ref"))));
  }
  
  /**
   * Gets the reference solution of the SAX transformation applied to the
   * numeric dataset (see {@link #getNumericsDataset()} with a parameter of
   * <code>W = 7</code> and an alphabet size of 20</p>
   * 
   * @return reference solution
   * @throws IOException if the reference solution can't be loaded
   */
  private Instances getNumericsDataset7x20() throws IOException {
    return new Instances(new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("weka/filters/unsupervised/timeseries/data/NumericTimeSeriesSAX7x20.ref"))));
  }
  
  /**
   * Creates a filter with the specified parameters
   * 
   * @param w the <code>W</code> parameter for the filter
   * @param a the alphabet size for the filter
   * @param range the ranges to which the filter should be applied
   * @return the configured filter
   * @throws Exception if something goes wrong
   */
  private Filter getFilter(int w, int a, String range) throws Exception {
    Filter filter = getFilter();
    filter.setOptions(new String[]{"-W", "" + w, "-A", "" + a, "-R", range});
    return filter;
  }

  /**
   * Gets a filter configured with the default parameters
   */
  @Override
  public Filter getFilter() {
    return new SAXTransformer();
  }

  /**
   * Returns a test suite.
   * 
   * @return test suite
   */
  public static Test suite() {
    return new TestSuite(SAXTransformerTest.class);
  }

  /**
   * Runs the test from command-line.
   * 
   * @param args ignored
   */
  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
