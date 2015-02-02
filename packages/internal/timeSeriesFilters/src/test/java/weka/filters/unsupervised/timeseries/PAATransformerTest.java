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
 * PAATransformerTest.java
 * Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.timeseries;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import junit.framework.Test;
import junit.framework.TestSuite;
import weka.core.Instances;
import weka.core.Range;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;
import weka.filters.unsupervised.timeseries.PAATransformer;

/**
 * Tests the {@link PAATransformer} filter
 * 
 * @author Benjamin Weber ( benweber at student dot ethz dot ch )
 * @version $Revision: 1000 $
 */
public class PAATransformerTest extends AbstractFilterTest {
  
  private static final double RELATIVE_ACCURACY = 1e-10;
  
  /**
   * Constructs a {@link PAATransformerTest} with the name <code>name</code>
   * 
   * @param name name of the test
   */
  public PAATransformerTest(String name) {
    super(name);
  }
  
  /**
   * Tests whether the PAA transformation calculates the correct values
   * 
   * @throws Exception if something goes wrong
   */
  public void testNumerics() throws Exception {
    Instances output10 = apply(getNumericsDataset(), getFilter(10, "1"));
    Instances output7 = apply(getNumericsDataset(), getFilter(7, "1"));
    
    checkNumericSimilarity(getNumericsDataset10(), output10, "1");
    checkNumericSimilarity(getNumericsDataset7(), output7, "1");

  }
  
  /**
   * Checks that the reference dataset and the test datatset are equal within
   * a relative accuracy of {@value #RELATIVE_ACCURACY}
   * 
   * @param reference the reference dataset
   * @param test the dataset to be tested
   * @param range the range where time series are to be found
   */
  private void checkNumericSimilarity(Instances reference, Instances test, String range) {
    
    checkBasicProperties(reference, test, range);
    
    Range timeSeries = new Range(range);
    timeSeries.setUpper(reference.numAttributes());
    
    for (int i = 0; i < reference.numInstances(); i++) {
      for (int att : timeSeries.getSelection()) {
        Instances inputSeries = reference.attribute(att).relation(
            (int) reference.get(i).value(att));
        Instances outputSeries = test.attribute(att).relation(
            (int) test.get(i).value(att));
        
        checkBasicProperties(inputSeries, outputSeries, "first-last");
        
        for (int k = 0; k < inputSeries.numInstances(); k++) {
          for (int l = 0; l < inputSeries.numAttributes(); l++) {
            assertTrue(inputSeries.attribute(l).isNumeric());
            assertTrue(outputSeries.attribute(l).isNumeric());
            
            double in = inputSeries.get(k).value(l);
            double out = outputSeries.get(k).value(l);
            
            assertEquals(
                String.format("Transformation isn't within %f%% accurate!",
                    RELATIVE_ACCURACY*100),
                in, out, Math.abs(in*RELATIVE_ACCURACY));
          }
        }
      }
    }
  }
  
  /**
   * Tests the PAA transformation with a dataset with different weights
   * 
   * @throws Exception if something goes wrong
   */
  public void testWeightedCase() throws Exception {
    Instances test = new Instances(new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("weka/filters/unsupervised/timeseries/data/PAAweightsTest.arff"))));
    Instances reference = new Instances(new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("weka/filters/unsupervised/timeseries/data/PAAweightsTest.ref"))));
    
    Instances out = apply(test, getFilter(7, "1"));
    checkNumericSimilarity(reference, out, "1");
  }
  
  /**
   * Tests the PAA transformation on a simple time series dataset with different
   * parameters
   * 
   * @throws Exception if something goes wrong
   */
  public void testSimpleDataset() throws Exception {
    int[] ws = {1, 10, 7, 6};
    String[] ranges = {"", "3", "5", "3,5"};
    for (int w : ws)
      for (String range : ranges)
        doSimpleDatasetTest(w, range);
  }
  
  /**
   * Carries out the tests for the {@link #testSimpleDataset()} method
   */
  private void doSimpleDatasetTest(int w, String range) throws Exception {
    m_Instances = getSimpleDataset();
    m_Filter = getFilter(w, range);
    testBuffered();
    m_Instances = getSimpleDataset();
    m_Filter = getFilter(w, range);
    testIncremental();
    m_Instances = getSimpleDataset();
    m_Filter = getFilter(w, range);
    testThroughput();
    m_Instances = getSimpleDataset();
    checkBasicProperties(m_Instances, apply(m_Instances, getFilter(w, range)), range);
  }
  
  /**
   * Checks that basic properties of the PAA transformation aren't violated</p>
   * 
   * Basic properties like that the number of instances hasn't changed etc</p>
   * 
   * @param input the dataset that was used for transformation
   * @param output the dataset that was produced during transformation
   * @param range the ranges where time series are to be found
   */
  private void checkBasicProperties(Instances input, Instances output, String range) {
    assertEquals("Number of attributes has changed!",
        input.numAttributes(), output.numAttributes());
    assertEquals("Number of instances has changed!",
        input.numInstances(), output.numInstances());
    
    Range nonTimeSeries = new Range(range);
    nonTimeSeries.setInvert(true);
    nonTimeSeries.setUpper(input.numAttributes() - 1);
    
    for (int i = 0; i < input.numAttributes(); i++) {
      assertEquals("Attribute type has changed!",
          input.attribute(i), output.attribute(i));
      
      if (input.attribute(i).isRelationValued()) {
        Instances inputRelation = input.attribute(i).relation();
        Instances outputRelation = output.attribute(i).relation();
        
        assertEquals(
            "Number of attributes inside relational attribute has changed!",
            inputRelation.numAttributes(), outputRelation.numAttributes());
        
        for (int k = 0; k < inputRelation.numAttributes(); k++) {
          assertEquals("Attribute type inside relationl attribute has changed!",
              inputRelation.attribute(k), outputRelation.attribute(k));
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
   * Gets the reference solution of the PAA transformation applied to the
   * numeric dataset (see {@link #getNumericsDataset()} with a parameter of
   * <code>W = 10</code></p>
   * 
   * Note: 10 was chosen as it's not co-prime with many time series lengths
   * of the numeric dataset.</p>
   * 
   * @return reference solution
   * @throws IOException if the reference solution can't be loaded
   */
  private Instances getNumericsDataset10() throws IOException {
    return new Instances(new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("weka/filters/unsupervised/timeseries/data/NumericTimeSeriesPAA10.ref"))));
  }
  
  /**
   * Gets the reference solution of the PAA transformation applied to the
   * numeric dataset (see {@link #getNumericsDataset()} with a parameter of
   * <code>W = 7</code>
   * 
   * Note: 7 was chosen as it's co-prime with many time series lengths
   * of the numeric dataset.</p>
   * 
   * @return reference solution
   * @throws IOException if the reference solution can't be loaded
   */
  private Instances getNumericsDataset7() throws IOException {
    return new Instances(new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("weka/filters/unsupervised/timeseries/data/NumericTimeSeriesPAA7.ref"))));
  }
  
  /**
   * Creates a filter with the specified parameters
   * 
   * @param w the <code>W</code> parameter for the filter
   * @param range the ranges to which the filter should be applied
   * @return the configured filter
   * @throws Exception if something goes wrong
   */
  private Filter getFilter(int w, String range) throws Exception {
    Filter filter = getFilter();
    
    filter.setOptions(new String[]{"-W", "" + w, "-R", range});
    
    return filter;
  }

  /**
   * Gets a filter configured with the default parameters
   */
  @Override
  public Filter getFilter() {
    return new PAATransformer();
  }

  /**
   * Returns a test suite.
   * 
   * @return		test suite
   */
  public static Test suite() {
    return new TestSuite(PAATransformerTest.class);
  }

  /**
   * Runs the test from command-line.
   * 
   * @param args	ignored
   */
  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
