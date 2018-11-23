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
 * OneDimensionalTimeSeriesToStringTest.java
 * Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.timeseries;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests for the {@link OneDimensionalTimeSeriesToString} filter
 * 
 * @author Benjamin Weber ( benweber at student dot ethz dot ch )
 * @version $Revision: 1000 $
 */
public class OneDimensionalTimeSeriesToStringTest extends AbstractFilterTest {

  /**
   * Constructs the {@link OneDimensionalTimeSeriesToStringTest} test with the
   * provided name
   * 
   * @param name the name of the test
   */
  public OneDimensionalTimeSeriesToStringTest(String name) {
    super(name);
  }
  
  /**
   * Tests the filter on the created test datasets and checks its output against
   * the reference solutions
   * 
   * @throws Exception if something goes wrong
   */
  public void testSimpleConversions() throws Exception {
    checkEquivalence(getReferenceSet1(), apply(getTestSet1(), getFilter("1")));
    checkEquivalence(getReferenceSet2(), apply(getTestSet2(), getFilter("1")));
  }
  
  /**
   * Tests the filter on a fairly generic dataset with many different cases
   * 
   * @throws Exception if something goes wrong
   */
  public void testGenericConversions() throws Exception {
    Instances reference = getGenericReferenceSet();
    Instances test = apply(getGenericSet(), getFilter("3"));
    
    final int timeSeriesIndex = 2;
    
    assertEquals("Incorrect amount of attribute",
        reference.numAttributes(), test.numAttributes());
    assertEquals("Incorrect amount of instances",
        reference.numInstances(), test.numInstances());

        
    for (int att = 0; att < reference.numAttributes(); att++) {
      if (att == timeSeriesIndex) {
        // check time series
        assertTrue("Time series wasn't converted to String",
            test.attribute(att).isString());
      }
    }

    for (int i = 0; i < reference.numInstances(); i++) {
      for (int att = 0; att < reference.numAttributes(); att++) {
        
        // non-time series simply must have the same value
        if (att != timeSeriesIndex) {
          assertEquals(
              "Incorrect value in instance " + i + " at Attribute #" + (att + 1) + "!",
              reference.get(i).value(att), test.get(i).value(att)
              );
          continue;
        }
        
        assertEquals("Incorrect output string",
            reference.get(i).stringValue(att), test.get(i).stringValue(att));
      }
    }
  }
  
  /**
   * Checks the test dataset for equivalence against the reference dataset</p>
   * 
   * Assumes that each dataset only contains one attribute of string type
   * 
   * @param reference the reference dataset
   * @param test the to be tested dataset
   */
  private void checkEquivalence(Instances reference, Instances test) {
    assert reference.numAttributes() == 1;
    assert reference.attribute(0).isString();
    assert test.numAttributes() == 1;
    assert test.attribute(0).isString();
    
    assertEquals("Same number of instances",
        reference.numInstances(), test.numInstances());
    
    for (int i = 0; i < reference.numInstances(); i++) {
      assertEquals("Incorrect string value",
          reference.get(i).stringValue(0), test.get(i).stringValue(0));
    }
  }
  
  /**
   * Loads a very generic test dataset
   * 
   * @return the dataset
   * @throws IOException if the dataset can't be loaded
   */
  private Instances getGenericSet() throws IOException {
    return new Instances(new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("weka/filters/unsupervised/timeseries/data/SimpleStringTimeSeries.arff"))));
  }

  /**
   * Loads a very generic test dataset
   * 
   * @return the dataset
   * @throws IOException if the dataset can't be loaded
   */
  private Instances getGenericReferenceSet() throws IOException {
    return new Instances(new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("weka/filters/unsupervised/timeseries/data/SimpleStringTimeSeriesToString.ref"))));
  }

  /**
   * Loads the first test dataset
   * 
   * @return the dataset
   * @throws IOException if the dataset can't be loaded
   */
  private Instances getTestSet1() throws IOException {
    return new Instances(new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("weka/filters/unsupervised/timeseries/data/NumericTimeSeriesSAX10x20.ref"))));
  }

  /**
   * Loads the second test dataset
   * 
   * @return the dataset
   * @throws IOException if the dataset can't be loaded
   */
  private Instances getTestSet2() throws IOException {
    return new Instances(new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("weka/filters/unsupervised/timeseries/data/NumericTimeSeriesSAX7x4.ref"))));
  }

  /**
   * Loads the reference dataset for the first test set
   * 
   * @return the dataset
   * @throws IOException if the dataset can't be loaded
   */
  private Instances getReferenceSet1() throws IOException {
    return new Instances(new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("weka/filters/unsupervised/timeseries/data/NumericTimeSeriesSAX10x20String.ref"))));
  }

  /**
   * Loads the reference dataset for the second test set
   * 
   * @return the dataset
   * @throws IOException if the dataset can't be loaded
   */
  private Instances getReferenceSet2() throws IOException {
    return new Instances(new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("weka/filters/unsupervised/timeseries/data/NumericTimeSeriesSAX7x4String.ref"))));
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
   * Gets a filter configured with the default parameters
   */
  @Override
  public OneDimensionalTimeSeriesToString getFilter() {
    return new OneDimensionalTimeSeriesToString();
  }
  
  /**
   * Gets the filter configured with the provided ranges
   * 
   * @param ranges the range to which the filter should be applied
   * @return the configured filer
   */
  public Filter getFilter(String ranges) {
    OneDimensionalTimeSeriesToString filter = getFilter();
    filter.setRange(ranges);
    return filter;
  }

  /**
   * Returns a test suite.
   * 
   * @return		test suite
   */
  public static Test suite() {
    return new TestSuite(OneDimensionalTimeSeriesToStringTest.class);
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
