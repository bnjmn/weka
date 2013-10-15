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
 *    CSVToARFFHeaderMapTaskTest.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import weka.core.Instances;

/**
 * Tests the CSVToARFFHeaderMapTask and the CSVToARFFHeaderReduceTask.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class CSVToARFFHeaderMapTaskTest {
  public static final String IRIS_HEADER = "petallength,petalwidth,sepallength,sepalwidth,class\n";

  public static final String IRIS = IRIS_HEADER
    + CorrelationMatrixMapTaskTest.IRIS_DATA;

  public static final double TOL = 1e-6;

  @Test
  public void testGetHeaderWithoutProcessing() throws Exception {
    // tests getting a header without any processing of incoming
    // CSV data - i.e. all attributes are assumed to be numeric

    CSVToARFFHeaderMapTask task = new CSVToARFFHeaderMapTask();
    // task.setOptions(args);
    task.setComputeSummaryStats(false);

    Instances i = task.getHeader(10, null);
    for (int j = 0; j < i.numAttributes(); j++) {
      assertTrue(i.attribute(j).isNumeric());
    }
  }

  @Test
  public void testProcessCSVNoSummaryAtts() throws Exception {
    CSVToARFFHeaderMapTask task = new CSVToARFFHeaderMapTask();
    task.setComputeSummaryStats(false);

    BufferedReader br = new BufferedReader(new StringReader(IRIS));

    String line = br.readLine();
    String[] names = line.split(",");
    List<String> attNames = new ArrayList<String>();
    for (String s : names) {
      attNames.add(s);
    }

    while ((line = br.readLine()) != null) {
      task.processRow(line, attNames);
    }

    br.close();

    Instances header = task.getHeader();

    assertEquals(5, header.numAttributes());
    assertTrue(header.attribute(4).isNominal());
    assertEquals(3, header.attribute(4).numValues());
  }

  @Test
  public void testProcessCSVSummaryAttributes() throws Exception {
    CSVToARFFHeaderMapTask task = new CSVToARFFHeaderMapTask();
    task.setComputeSummaryStats(true);

    BufferedReader br = new BufferedReader(new StringReader(IRIS));

    String line = br.readLine();
    String[] names = line.split(",");
    List<String> attNames = new ArrayList<String>();
    for (String s : names) {
      attNames.add(s);
    }

    while ((line = br.readLine()) != null) {
      task.processRow(line, attNames);
    }

    br.close();

    Instances header = task.getHeader();

    assertEquals(10, header.numAttributes()); // one meta attribute for each
                                              // actual attribute

    assertTrue(header.attribute(4).isNominal());
    for (int i = 5; i < header.numAttributes(); i++) {
      assertTrue(header.attribute(i).name().startsWith("arff_summary_"));
    }

    // check stats for petallength
    CSVToARFFHeaderMapTask.NumericStats s = CSVToARFFHeaderMapTask.NumericStats
      .attributeToStats(header.attribute(5));

    // derived metrics in summary attributes should all be zero
    assertTrue(s.getStats()[CSVToARFFHeaderMapTask.ArffSummaryNumericMetric.STDDEV
      .ordinal()] == 0);

    // reduce to compute derived metrics
    CSVToARFFHeaderReduceTask arffReduce = new CSVToARFFHeaderReduceTask();
    List<Instances> instList = new ArrayList<Instances>();
    instList.add(task.getHeader());
    header = arffReduce.aggregate(instList);

    s = CSVToARFFHeaderMapTask.NumericStats.attributeToStats(header
      .attribute(5));
    assertEquals(150,
      (int) s.getStats()[CSVToARFFHeaderMapTask.ArffSummaryNumericMetric.COUNT
        .ordinal()]);
    assertTrue(Math
      .abs(s.getStats()[CSVToARFFHeaderMapTask.ArffSummaryNumericMetric.SUM
        .ordinal()] - 876.5) < TOL);
    assertTrue(Math
      .abs(s.getStats()[CSVToARFFHeaderMapTask.ArffSummaryNumericMetric.SUMSQ
        .ordinal()] - 5223.849999999998) < TOL);
    assertTrue(Math
      .abs(s.getStats()[CSVToARFFHeaderMapTask.ArffSummaryNumericMetric.MIN
        .ordinal()] - 4.3) < TOL);
    assertTrue(Math
      .abs(s.getStats()[CSVToARFFHeaderMapTask.ArffSummaryNumericMetric.MAX
        .ordinal()] - 7.9) < TOL);
    assertTrue(Math
      .abs(s.getStats()[CSVToARFFHeaderMapTask.ArffSummaryNumericMetric.MEAN
        .ordinal()] - 5.843333333333335) < TOL);
    assertTrue(Math
      .abs(s.getStats()[CSVToARFFHeaderMapTask.ArffSummaryNumericMetric.MISSING
        .ordinal()] - 0) < TOL);
    assertTrue(Math
      .abs(s.getStats()[CSVToARFFHeaderMapTask.ArffSummaryNumericMetric.STDDEV
        .ordinal()] - 0.8280661279778435) < TOL);
  }

  @Test
  public void testProcessCSVSummaryAttributesTwoMapTasks() throws Exception {
    CSVToARFFHeaderMapTask task = new CSVToARFFHeaderMapTask();
    task.setComputeSummaryStats(true);

    CSVToARFFHeaderMapTask task2 = new CSVToARFFHeaderMapTask();
    task2.setComputeSummaryStats(true);

    BufferedReader br = new BufferedReader(new StringReader(IRIS));

    String line = br.readLine();
    String[] names = line.split(",");
    List<String> attNames = new ArrayList<String>();
    for (String s : names) {
      attNames.add(s);
    }

    int count = 0;
    while ((line = br.readLine()) != null) {
      if (count % 2 == 0) {
        task.processRow(line, attNames);
      } else {
        task2.processRow(line, attNames);
      }

      count++;
    }

    br.close();

    assertEquals(10, task.getHeader().numAttributes());
    assertEquals(10, task2.getHeader().numAttributes());
    assertTrue(task.getHeader().attribute(4).isNominal());
    assertTrue(task2.getHeader().attribute(4).isNominal());

    for (int i = 5; i < task.getHeader().numAttributes(); i++) {
      assertTrue(task.getHeader().attribute(i).name()
        .startsWith("arff_summary_"));
      assertTrue(task2.getHeader().attribute(i).name()
        .startsWith("arff_summary_"));
    }

    // reduce to compute derived metrics
    CSVToARFFHeaderReduceTask arffReduce = new CSVToARFFHeaderReduceTask();
    List<Instances> instList = new ArrayList<Instances>();
    instList.add(task.getHeader());
    instList.add(task2.getHeader());
    Instances header = arffReduce.aggregate(instList);

    CSVToARFFHeaderMapTask.NumericStats s = CSVToARFFHeaderMapTask.NumericStats
      .attributeToStats(header.attribute(5));
    assertEquals(150,
      (int) s.getStats()[CSVToARFFHeaderMapTask.ArffSummaryNumericMetric.COUNT
        .ordinal()]);
    assertTrue(Math
      .abs(s.getStats()[CSVToARFFHeaderMapTask.ArffSummaryNumericMetric.SUM
        .ordinal()] - 876.5) < TOL);
    assertTrue(Math
      .abs(s.getStats()[CSVToARFFHeaderMapTask.ArffSummaryNumericMetric.SUMSQ
        .ordinal()] - 5223.849999999998) < TOL);
    assertTrue(Math
      .abs(s.getStats()[CSVToARFFHeaderMapTask.ArffSummaryNumericMetric.MIN
        .ordinal()] - 4.3) < TOL);
    assertTrue(Math
      .abs(s.getStats()[CSVToARFFHeaderMapTask.ArffSummaryNumericMetric.MAX
        .ordinal()] - 7.9) < TOL);
    assertTrue(Math
      .abs(s.getStats()[CSVToARFFHeaderMapTask.ArffSummaryNumericMetric.MEAN
        .ordinal()] - 5.843333333333335) < TOL);
    assertTrue(Math
      .abs(s.getStats()[CSVToARFFHeaderMapTask.ArffSummaryNumericMetric.MISSING
        .ordinal()] - 0) < TOL);
    assertTrue(Math
      .abs(s.getStats()[CSVToARFFHeaderMapTask.ArffSummaryNumericMetric.STDDEV
        .ordinal()] - 0.8280661279778435) < TOL);
  }

  public static void main(String[] args) {
    try {
      CSVToARFFHeaderMapTaskTest t = new CSVToARFFHeaderMapTaskTest();

      t.testGetHeaderWithoutProcessing();
      t.testProcessCSVNoSummaryAtts();
      t.testProcessCSVSummaryAttributes();
      t.testProcessCSVSummaryAttributesTwoMapTasks();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
