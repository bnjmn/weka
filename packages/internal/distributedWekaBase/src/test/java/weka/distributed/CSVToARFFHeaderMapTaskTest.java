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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import weka.core.Attribute;
import weka.core.Instances;
import weka.core.stats.ArffSummaryNumericMetric;
import weka.core.stats.NumericAttributeBinData;
import weka.core.stats.NumericStats;
import weka.core.stats.QuantileCalculator;
import weka.core.stats.StringStats;
import weka.distributed.CSVToARFFHeaderMapTask.HeaderAndQuantileDataHolder;

import weka.core.stats.TDigest;

/**
 * Tests the CSVToARFFHeaderMapTask and the CSVToARFFHeaderReduceTask.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class CSVToARFFHeaderMapTaskTest {
  public static final String IRIS_HEADER =
    "sepallength,sepalwidth,petallength,petalwidth,class\n";

  public static final String IRIS = IRIS_HEADER
    + CorrelationMatrixMapTaskTest.IRIS_DATA;

  public static final double TOL = 1e-6;

  @Test
  public void testGetHeaderWithoutProcessing() throws Exception {
    // tests getting a header without any processing of incoming
    // CSV data - i.e. all attributes are assumed to be numeric

    CSVToARFFHeaderMapTask task = new CSVToARFFHeaderMapTask();
    // task.setOptions(args);

    Instances i = task.getHeader(10, null);
    for (int j = 0; j < i.numAttributes(); j++) {
      assertTrue(i.attribute(j).isNumeric());
    }
  }

  // @Test
  // public void testProcessCSVNoSummaryAtts() throws Exception {
  // CSVToARFFHeaderMapTask task = new CSVToARFFHeaderMapTask();
  // task.setComputeSummaryStats(false);
  //
  // BufferedReader br = new BufferedReader(new StringReader(IRIS));
  //
  // String line = br.readLine();
  // String[] names = line.split(",");
  // List<String> attNames = new ArrayList<String>();
  // for (String s : names) {
  // attNames.add(s);
  // }
  //
  // while ((line = br.readLine()) != null) {
  // task.processRow(line, attNames);
  // }
  //
  // br.close();
  //
  // Instances header = task.getHeader();
  //
  // assertEquals(5, header.numAttributes());
  // assertTrue(header.attribute(4).isNominal());
  // assertEquals(3, header.attribute(4).numValues());
  // }

  @Test
  public void testProcessCSVSummaryAttributesWithStringAttribute()
    throws Exception {
    CSVToARFFHeaderMapTask task = new CSVToARFFHeaderMapTask();
    task.setStringAttributes("last");

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

    // check that the summary att has 16 values - i.e.
    // 8 summary stats for the string length and 8 for
    // the word count
    assertEquals(16, header.attribute(9).numValues());

    StringStats stats = StringStats.attributeToStats(header.attribute(9));

    assertEquals(
      150,
      (int) stats.getStringLengthStats().getStats()[ArffSummaryNumericMetric.COUNT
        .ordinal()]);

    assertEquals(150,
      (int) stats.getWordCountStats().getStats()[ArffSummaryNumericMetric.COUNT
        .ordinal()]);

    // derived metrics should all be zero at this stage
    assertTrue(stats.getStringLengthStats().getStats()[ArffSummaryNumericMetric.STDDEV
      .ordinal()] == 0);

    // reduce to compute derived metrics
    CSVToARFFHeaderReduceTask arffReduce = new CSVToARFFHeaderReduceTask();
    List<Instances> instList = new ArrayList<Instances>();
    instList.add(task.getHeader());
    header = arffReduce.aggregate(instList);

    stats = StringStats.attributeToStats(header.attribute(9));
    assertTrue(Math
      .abs(stats.getStringLengthStats().getStats()[ArffSummaryNumericMetric.MEAN
        .ordinal()] - 13.333333) < TOL);

    assertTrue(Math
      .abs(stats.getStringLengthStats().getStats()[ArffSummaryNumericMetric.STDDEV
        .ordinal()] - 1.7053672) < TOL);
  }

  @Test
  public void testProcessCSVSummaryAttributes() throws Exception {
    CSVToARFFHeaderMapTask task = new CSVToARFFHeaderMapTask();

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
    NumericStats s = NumericStats.attributeToStats(header.attribute(5));

    // derived metrics in summary attributes should all be zero
    assertTrue(s.getStats()[ArffSummaryNumericMetric.STDDEV.ordinal()] == 0);

    // reduce to compute derived metrics
    CSVToARFFHeaderReduceTask arffReduce = new CSVToARFFHeaderReduceTask();
    List<Instances> instList = new ArrayList<Instances>();
    instList.add(task.getHeader());
    header = arffReduce.aggregate(instList);

    s = NumericStats.attributeToStats(header.attribute(5));
    assertEquals(150,
      (int) s.getStats()[ArffSummaryNumericMetric.COUNT.ordinal()]);
    assertTrue(Math
      .abs(s.getStats()[ArffSummaryNumericMetric.SUM.ordinal()] - 876.5) < TOL);
    assertTrue(Math
      .abs(s.getStats()[ArffSummaryNumericMetric.SUMSQ.ordinal()] - 5223.849999999998) < TOL);
    assertTrue(Math
      .abs(s.getStats()[ArffSummaryNumericMetric.MIN.ordinal()] - 4.3) < TOL);
    assertTrue(Math
      .abs(s.getStats()[ArffSummaryNumericMetric.MAX.ordinal()] - 7.9) < TOL);
    assertTrue(Math
      .abs(s.getStats()[ArffSummaryNumericMetric.MEAN.ordinal()] - 5.843333333333335) < TOL);
    assertTrue(Math
      .abs(s.getStats()[ArffSummaryNumericMetric.MISSING.ordinal()] - 0) < TOL);
    assertTrue(Math
      .abs(s.getStats()[ArffSummaryNumericMetric.STDDEV.ordinal()] - 0.8280661279778435) < TOL);
  }

  @Test
  public void testProcessCSVSummaryAttributesPlusTDigestQuartiles()
    throws Exception {
    CSVToARFFHeaderMapTask task = new CSVToARFFHeaderMapTask();
    task.setComputeQuartilesAsPartOfSummaryStats(true);
    task.setCompressionLevelForQuartileEstimation(75);

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

    HeaderAndQuantileDataHolder holder = task.getHeaderAndQuantileEstimators();
    List<HeaderAndQuantileDataHolder> holderList =
      new ArrayList<HeaderAndQuantileDataHolder>();
    holderList.add(holder);

    // reduce to compute derived metrics
    CSVToARFFHeaderReduceTask arffReduce = new CSVToARFFHeaderReduceTask();

    Instances finalHeader = arffReduce.aggregateHeadersAndQuartiles(holderList);

    // System.err.println(finalHeader);

    // test a few quartiles
    Attribute sepallengthSummary =
      finalHeader
        .attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
          + "sepallength");
    double median =
      ArffSummaryNumericMetric.MEDIAN.valueFromAttribute(sepallengthSummary);
    assertEquals(5.80000021, median, 0.0001);

    double lowerQuartile =
      ArffSummaryNumericMetric.FIRSTQUARTILE
        .valueFromAttribute(sepallengthSummary);
    assertEquals(5.10000020, lowerQuartile, 0.0001);

    double upperQuartile =
      ArffSummaryNumericMetric.THIRDQUARTILE
        .valueFromAttribute(sepallengthSummary);
    assertEquals(6.40000022, upperQuartile, 0.0001);
  }

  @Test
  public void testProcessWithTDigestQuartilesTwoMapTasks() throws Exception {
    CSVToARFFHeaderMapTask task = new CSVToARFFHeaderMapTask();
    task.setComputeQuartilesAsPartOfSummaryStats(true);

    CSVToARFFHeaderMapTask task2 = new CSVToARFFHeaderMapTask();
    task2.setComputeQuartilesAsPartOfSummaryStats(true);

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

    HeaderAndQuantileDataHolder holder = task.getHeaderAndQuantileEstimators();
    HeaderAndQuantileDataHolder holder2 =
      task2.getHeaderAndQuantileEstimators();
    List<HeaderAndQuantileDataHolder> holderList =
      new ArrayList<HeaderAndQuantileDataHolder>();
    holderList.add(holder);
    holderList.add(holder2);

    Instances finalHeader = arffReduce.aggregateHeadersAndQuartiles(holderList);

    // System.err.println(finalHeader);

    // test a few quartiles
    Attribute sepallengthSummary =
      finalHeader
        .attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
          + "sepallength");
    double median =
      ArffSummaryNumericMetric.MEDIAN.valueFromAttribute(sepallengthSummary);
    assertEquals(5.80000021, median, 0.0001);

    double lowerQuartile =
      ArffSummaryNumericMetric.FIRSTQUARTILE
        .valueFromAttribute(sepallengthSummary);
    assertEquals(5.10000020, lowerQuartile, 0.0001);

    double upperQuartile =
      ArffSummaryNumericMetric.THIRDQUARTILE
        .valueFromAttribute(sepallengthSummary);
    assertEquals(6.40000022, upperQuartile, 0.0001);
  }

  @Test
  public void testProcessCSVSummaryAttributesPlusPSquaredQuartiles()
    throws Exception {
    CSVToARFFHeaderMapTask task = new CSVToARFFHeaderMapTask();

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

    // reduce to compute derived metrics
    CSVToARFFHeaderReduceTask arffReduce = new CSVToARFFHeaderReduceTask();
    List<Instances> instList = new ArrayList<Instances>();
    instList.add(task.getHeader());
    header = arffReduce.aggregate(instList);

    Instances headerNoSummary =
      CSVToARFFHeaderReduceTask.stripSummaryAtts(header);
    QuantileCalculator quartiles =
      new QuantileCalculator(headerNoSummary, new double[] { 0.25, 0.5, 0.75 });

    Map<Integer, NumericAttributeBinData> histMaps =
      new HashMap<Integer, NumericAttributeBinData>();
    for (int i = 0; i < headerNoSummary.numAttributes(); i++) {
      if (headerNoSummary.attribute(i).isNumeric()) {
        Attribute summary =
          header.attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
            + headerNoSummary.attribute(i).name());
        histMaps.put(i, new NumericAttributeBinData(headerNoSummary
          .attribute(i).name(), summary, -1));
      }
    }

    quartiles.setHistogramMap(histMaps);

    br = new BufferedReader(new StringReader(IRIS));
    br.readLine();

    while ((line = br.readLine()) != null) {
      String[] split = line.split(",");

      quartiles.update(split, "?");
    }
    br.close();

    Instances updatedHeader =
      CSVToARFFHeaderReduceTask.updateSummaryAttsWithQuartilesAndHistograms(
        header, quartiles, histMaps);

    // test a few quartiles
    Attribute sepallengthSummary =
      updatedHeader
        .attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
          + "sepallength");
    double median =
      ArffSummaryNumericMetric.MEDIAN.valueFromAttribute(sepallengthSummary);
    assertEquals(5.87387, median, 0.0001);

    double lowerQuartile =
      ArffSummaryNumericMetric.FIRSTQUARTILE
        .valueFromAttribute(sepallengthSummary);
    assertEquals(5.31787, lowerQuartile, 0.0001);

    double upperQuartile =
      ArffSummaryNumericMetric.THIRDQUARTILE
        .valueFromAttribute(sepallengthSummary);
    assertEquals(6.49559, upperQuartile, 0.0001);

    NumericStats stats = NumericStats.attributeToStats(sepallengthSummary);
  }

  @Test
  public void testProcessCSVSummaryAttributesUnparsableNumericValue()
    throws Exception {
    CSVToARFFHeaderMapTask task = new CSVToARFFHeaderMapTask();

    BufferedReader br = new BufferedReader(new StringReader(IRIS));

    String line = br.readLine();
    String[] names = line.split(",");
    List<String> attNames = new ArrayList<String>();
    for (String s : names) {
      attNames.add(s);
    }

    int count = 0;
    while ((line = br.readLine()) != null) {
      if (count == 4) {
        line = line.replace("5.0", "bob");
      }
      task.processRow(line, attNames);
      count++;
    }

    assertEquals(10, task.getHeader().numAttributes());
    for (int i = 5; i < task.getHeader().numAttributes(); i++) {
      assertTrue(task.getHeader().attribute(i).name()
        .startsWith("arff_summary_"));
    }

    // reduce to compute derived metrics
    CSVToARFFHeaderReduceTask arffReduce = new CSVToARFFHeaderReduceTask();
    List<Instances> instList = new ArrayList<Instances>();
    instList.add(task.getHeader());
    Instances header = arffReduce.aggregate(instList);

    // first attribute is now of type string because of unparsable number
    StringStats s = StringStats.attributeToStats(header.attribute(5));
    assertEquals(150,
      (int) s.getStringLengthStats().getStats()[ArffSummaryNumericMetric.COUNT
        .ordinal()]);

    br.close();
  }

  @Test
  public void
    testProcessCSVSummaryAttributesUnparsableNumericValueTwoMapTasks()
      throws Exception {
    CSVToARFFHeaderMapTask task = new CSVToARFFHeaderMapTask();

    CSVToARFFHeaderMapTask task2 = new CSVToARFFHeaderMapTask();

    BufferedReader br = new BufferedReader(new StringReader(IRIS));

    String line = br.readLine();
    String[] names = line.split(",");
    List<String> attNames = new ArrayList<String>();
    for (String s : names) {
      attNames.add(s);
    }

    int count = 0;
    while ((line = br.readLine()) != null) {
      if (count == 4) {
        line = line.replace("5.0", "bob");
      }
      if (count % 2 == 0) {
        task.processRow(line, attNames);
      } else {
        task2.processRow(line, attNames);
      }

      count++;
    }

    assertEquals(10, task.getHeader().numAttributes());
    assertEquals(10, task2.getHeader().numAttributes());
    assertTrue(task.getHeader().attribute(0).isString());
    assertTrue(task2.getHeader().attribute(0).isNumeric());

    for (int i = 5; i < task.getHeader().numAttributes(); i++) {
      assertTrue(task.getHeader().attribute(i).name()
        .startsWith("arff_summary_"));
    }

    // reduce to compute derived metrics
    CSVToARFFHeaderReduceTask arffReduce = new CSVToARFFHeaderReduceTask();
    List<Instances> instList = new ArrayList<Instances>();
    instList.add(task.getHeader());
    instList.add(task2.getHeader());
    Instances header = arffReduce.aggregate(instList);

    // first attribute is now of type string because of unparsable number
    StringStats s = StringStats.attributeToStats(header.attribute(5));

    // We expect a count of only 75 because one task will have its stats dropped
    // due to being forced to be of type string in the aggregation process.
    assertEquals(75,
      (int) s.getStringLengthStats().getStats()[ArffSummaryNumericMetric.COUNT
        .ordinal()]);

    br.close();
  }

  @Test
  public void testTreatUnparsableAsMissing() throws Exception {
    CSVToARFFHeaderMapTask task = new CSVToARFFHeaderMapTask();
    task.setTreatUnparsableNumericValuesAsMissing(true);

    BufferedReader br = new BufferedReader(new StringReader(IRIS));

    String line = br.readLine();
    String[] names = line.split(",");
    List<String> attNames = new ArrayList<String>();
    for (String s : names) {
      attNames.add(s);
    }

    int count = 0;
    while ((line = br.readLine()) != null) {
      if (count == 4) {
        line = line.replace("5.0", "bob");
      }
      task.processRow(line, attNames);
      count++;
    }

    assertEquals(10, task.getHeader().numAttributes());
    for (int i = 5; i < task.getHeader().numAttributes(); i++) {
      assertTrue(task.getHeader().attribute(i).name()
        .startsWith("arff_summary_"));
    }
    assertTrue(task.getHeader().attribute(0).isNumeric());

    // reduce to compute derived metrics
    CSVToARFFHeaderReduceTask arffReduce = new CSVToARFFHeaderReduceTask();
    List<Instances> instList = new ArrayList<Instances>();
    instList.add(task.getHeader());
    Instances header = arffReduce.aggregate(instList);
    NumericStats s = NumericStats.attributeToStats(header.attribute(5));
    assertEquals(149,
      (int) s.getStats()[ArffSummaryNumericMetric.COUNT.ordinal()]);
    assertEquals(1,
      (int) s.getStats()[ArffSummaryNumericMetric.MISSING.ordinal()]);
  }

  @Test
  public void testProcessCSVSummaryAttributesTwoMapTasks() throws Exception {
    CSVToARFFHeaderMapTask task = new CSVToARFFHeaderMapTask();

    CSVToARFFHeaderMapTask task2 = new CSVToARFFHeaderMapTask();

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

    NumericStats s = NumericStats.attributeToStats(header.attribute(5));
    assertEquals(150,
      (int) s.getStats()[ArffSummaryNumericMetric.COUNT.ordinal()]);
    assertTrue(Math
      .abs(s.getStats()[ArffSummaryNumericMetric.SUM.ordinal()] - 876.5) < TOL);
    assertTrue(Math
      .abs(s.getStats()[ArffSummaryNumericMetric.SUMSQ.ordinal()] - 5223.849999999998) < TOL);
    assertTrue(Math
      .abs(s.getStats()[ArffSummaryNumericMetric.MIN.ordinal()] - 4.3) < TOL);
    assertTrue(Math
      .abs(s.getStats()[ArffSummaryNumericMetric.MAX.ordinal()] - 7.9) < TOL);
    assertTrue(Math
      .abs(s.getStats()[ArffSummaryNumericMetric.MEAN.ordinal()] - 5.843333333333335) < TOL);
    assertTrue(Math
      .abs(s.getStats()[ArffSummaryNumericMetric.MISSING.ordinal()] - 0) < TOL);
    assertTrue(Math
      .abs(s.getStats()[ArffSummaryNumericMetric.STDDEV.ordinal()] - 0.8280661279778435) < TOL);
  }

  @Test
  public void testNumericStatsWithHistograms() throws Exception {
    CSVToARFFHeaderMapTask task = new CSVToARFFHeaderMapTask();

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

    // reduce to compute derived metrics
    CSVToARFFHeaderReduceTask arffReduce = new CSVToARFFHeaderReduceTask();
    List<Instances> instList = new ArrayList<Instances>();
    instList.add(task.getHeader());
    header = arffReduce.aggregate(instList);

    Attribute numericAtt =
      header.attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
        + header.attribute(0).name());

    NumericStats s = NumericStats.attributeToStats(numericAtt);

    assertTrue(s.getHistogramBinLabels() == null);
    assertTrue(s.getHistogramFrequencies() == null);

    List<String> binLabs = new ArrayList<String>();
    List<Double> binFreqs = new ArrayList<Double>();
    binLabs.add("Label1");
    binLabs.add("Label2");
    binLabs.add("Label3");
    binFreqs.add(2.0);
    binFreqs.add(0.0);
    binFreqs.add(10.0);

    s.setHistogramData(binLabs, binFreqs);

    Attribute newAtt = s.makeAttribute();

    assertEquals("Label1:2.0!Label2:0.0!Label3:10.0",
      newAtt.value(newAtt.numValues() - 1));

  }

  @Test
  public void testProcessFromHeaderRoundTrip() throws Exception {
    CSVToARFFHeaderMapTask task = new CSVToARFFHeaderMapTask();

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

    // reduce to compute derived metrics
    CSVToARFFHeaderReduceTask arffReduce = new CSVToARFFHeaderReduceTask();
    List<Instances> instList = new ArrayList<Instances>();
    instList.add(task.getHeader());
    header = CSVToARFFHeaderReduceTask.aggregate(instList);

    // set state from header
    task.fromHeader(header, null);
    instList.clear();
    instList.add(task.getHeader());
    Instances header2 = CSVToARFFHeaderReduceTask.aggregate(instList);
    assertTrue(header.equalHeaders(header2));

    // this time use a fresh map task object
    CSVToARFFHeaderMapTask newTask = new CSVToARFFHeaderMapTask();
    newTask.fromHeader(header, null);
    instList.clear();
    instList.add(newTask.getHeader());
    header2 = CSVToARFFHeaderReduceTask.aggregate(instList);
    assertTrue(header.equalHeaders(header2));
  }

  @Test
  public void testProcessRoundTripWithQuantiles() throws Exception {
    CSVToARFFHeaderMapTask task = new CSVToARFFHeaderMapTask();
    task.setComputeQuartilesAsPartOfSummaryStats(true);
    task.setCompressionLevelForQuartileEstimation(80.0);

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

    HeaderAndQuantileDataHolder holder = task.getHeaderAndQuantileEstimators();
    List<HeaderAndQuantileDataHolder> holderList =
      new ArrayList<HeaderAndQuantileDataHolder>();
    holderList.add(holder);
    Instances header =
      CSVToARFFHeaderReduceTask.aggregateHeadersAndQuartiles(holderList);

    Map<String, TDigest> estimators = new HashMap<String, TDigest>();
    Instances headerNoSummary =
      CSVToARFFHeaderReduceTask.stripSummaryAtts(header);
    for (int i = 0; i < headerNoSummary.numAttributes(); i++) {
      if (header.attribute(i).isNumeric()) {
        estimators.put(headerNoSummary.attribute(i).name(),
          holder.getQuantileEstimator(header.attribute(i).name()));
      }
    }

    CSVToARFFHeaderMapTask fresh = new CSVToARFFHeaderMapTask();
    fresh.setComputeQuartilesAsPartOfSummaryStats(true);
    fresh.setCompressionLevelForQuartileEstimation(80.0);
    fresh.fromHeader(header, estimators);
    holderList.clear();
    holder = fresh.getHeaderAndQuantileEstimators();
    holderList.add(holder);
    Instances freshHeader =
      CSVToARFFHeaderReduceTask.aggregateHeadersAndQuartiles(holderList);

    // check a few quantiles
    Attribute origSepallength =
      header.attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
        + "sepallength");
    Attribute freshSepallength =
      freshHeader
        .attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
          + "sepallength");

    assertEquals(
      ArffSummaryNumericMetric.FIRSTQUARTILE
        .valueFromAttribute(origSepallength),
      ArffSummaryNumericMetric.FIRSTQUARTILE
        .valueFromAttribute(freshSepallength), 0.000001);

    assertEquals(
      ArffSummaryNumericMetric.THIRDQUARTILE
        .valueFromAttribute(origSepallength),
      ArffSummaryNumericMetric.THIRDQUARTILE
        .valueFromAttribute(freshSepallength), 0.000001);

    assertEquals(
      ArffSummaryNumericMetric.MEDIAN.valueFromAttribute(origSepallength),
      ArffSummaryNumericMetric.MEDIAN.valueFromAttribute(freshSepallength),
      0.000001);
  }

  @Test
  public void testCombine() throws Exception {
    CSVToARFFHeaderMapTask task = new CSVToARFFHeaderMapTask();
    CSVToARFFHeaderMapTask task2 = new CSVToARFFHeaderMapTask();
    CSVToARFFHeaderMapTask full = new CSVToARFFHeaderMapTask();

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
      full.processRow(line, attNames);
      count++;
    }

    br.close();

    Instances fullHeader = full.getHeader();

    List<CSVToARFFHeaderMapTask> toCombine =
      new ArrayList<CSVToARFFHeaderMapTask>();
    toCombine.add(task);
    toCombine.add(task2);

    task = CSVToARFFHeaderMapTask.combine(toCombine);
    // assertTrue(fullHeader.equalHeaders(task.getHeader()));
    Instances combinedHeader = task.getHeader();

    // check a few numeric summary stats
    Attribute fullSepallength =
      fullHeader.attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
        + "sepallength");
    Attribute combinedSepallength =
      combinedHeader
        .attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
          + "sepallength");
    assertEquals(
      ArffSummaryNumericMetric.SUM.valueFromAttribute(fullSepallength),
      ArffSummaryNumericMetric.SUM.valueFromAttribute(combinedSepallength),
      0.000001);

    assertEquals(
      ArffSummaryNumericMetric.SUMSQ.valueFromAttribute(fullSepallength),
      ArffSummaryNumericMetric.SUMSQ.valueFromAttribute(combinedSepallength),
      0.000001);

    assertEquals(
      ArffSummaryNumericMetric.MAX.valueFromAttribute(fullSepallength),
      ArffSummaryNumericMetric.MAX.valueFromAttribute(combinedSepallength),
      0.000001);
  }

  public static void main(String[] args) {
    try {
      CSVToARFFHeaderMapTaskTest t = new CSVToARFFHeaderMapTaskTest();

      t.testGetHeaderWithoutProcessing();
      t.testProcessCSVSummaryAttributes();
      t.testProcessCSVSummaryAttributesTwoMapTasks();
      t.testProcessCSVSummaryAttributesPlusPSquaredQuartiles();
      t.testNumericStatsWithHistograms();
      t.testProcessCSVSummaryAttributesWithStringAttribute();
      t.testProcessCSVSummaryAttributesPlusTDigestQuartiles();
      t.testProcessWithTDigestQuartilesTwoMapTasks();
      t.testProcessFromHeaderRoundTrip();
      t.testCombine();
      t.testProcessRoundTripWithQuantiles();
      t.testProcessCSVSummaryAttributesUnparsableNumericValue();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
