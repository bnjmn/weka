package weka.distributed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import weka.core.Attribute;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.stats.ArffSummaryNumericMetric;

public class KMeansTaskTest {
  public static final String IRIS_HEADER =
    "sepallength,sepalwidth,petallength,petalwidth,class\n";

  public static final String IRIS = IRIS_HEADER
    + CorrelationMatrixMapTaskTest.IRIS_DATA;

  protected static Instances getIrisSummaryHeader() throws IOException,
    DistributedWekaException {
    CSVToARFFHeaderMapTask task = new CSVToARFFHeaderMapTask();

    BufferedReader br =
      new BufferedReader(new StringReader(IRIS));

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

    return header;
  }

  @Test
  public void testInitializationNoFilters() throws Exception {

    // no filters apart from missing values

    Instances irisData = CorrelationMatrixMapTaskTest.getIris();
    irisData.firstInstance().setMissing(0); // set one value to missing
    Instances irisSummaryHeader = getIrisSummaryHeader();

    KMeansMapTask task = new KMeansMapTask();
    try {
      task.processInstance(irisData.firstInstance());
      fail("Should have thrown an exception as we have not yet called init()");
    } catch (DistributedWekaException e) {
      // expected
    }

    task.init(irisSummaryHeader);
    assertFalse(task.getDontReplaceMissingValues());

    try {
      task.processInstance(irisData.firstInstance());
      fail("Should have thrown an exception as we have not set any starting centroids to use yet");
    } catch (DistributedWekaException e) {
      // expected
    }

    Instances initialCenters = new Instances(irisData, 0);
    initialCenters.add(irisData.firstInstance());
    initialCenters.add(irisData.instance(50));
    initialCenters.add(irisData.instance(100));

    initialCenters = task.applyFilters(initialCenters);
    task.setCentroids(initialCenters);

    // processInstance() should not raise exceptions now
    for (int i = 0; i < irisData.numInstances(); i++) {
      task.processInstance(irisData.instance(i));
    }

    // System.err.println("Full:\n" + irisSummaryHeader);
  }

  @Test
  public void testInitializationDontReplaceMissing() throws Exception {
    Instances irisData = CorrelationMatrixMapTaskTest.getIris();
    irisData.firstInstance().setMissing(0); // set one value to missing
    Instances irisSummaryHeader = getIrisSummaryHeader();

    KMeansMapTask task = new KMeansMapTask();
    task.setDontReplaceMissingValues(true);
    try {
      task.processInstance(irisData.firstInstance());
      fail("Should have thrown an exception as we have not yet called init()");
    } catch (DistributedWekaException e) {
      // expected
    }

    task.init(irisSummaryHeader);
    assertTrue(task.getDontReplaceMissingValues());

    try {
      task.processInstance(irisData.firstInstance());
      fail("Should have thrown an exception as we have not set any starting centroids to use yet");
    } catch (DistributedWekaException e) {
      // expected
    }

    Instances initialCenters = new Instances(irisData, 0);
    initialCenters.add(irisData.firstInstance());
    initialCenters.add(irisData.instance(50));
    initialCenters.add(irisData.instance(100));

    initialCenters = task.applyFilters(initialCenters);
    task.setCentroids(initialCenters);

    for (int i = 0; i < irisData.numInstances(); i++) {
      task.processInstance(irisData.instance(i));
    }

    List<Instances> centroidStats = task.getCentroidStats();
    assertEquals(3, centroidStats.size());

    // iris setosa cluster should have 49 non-missing values and 1 missing
    // for attributes
    assertEquals(49,
      (int) ArffSummaryNumericMetric.COUNT
        .valueFromAttribute(centroidStats.get(0).attribute(5)));

    // size of the other two clusters should be 50
    for (int i = 1; i < centroidStats.size(); i++) {
      Attribute summary = centroidStats.get(i).attribute(5);
      assertEquals(50,
        (int) ArffSummaryNumericMetric.COUNT
          .valueFromAttribute(summary));
    }
  }

  @Test
  public void testClusteringMissingValuesReplacementOnly() throws Exception {

    Instances irisData = CorrelationMatrixMapTaskTest.getIris();
    Instances irisSummaryHeader = getIrisSummaryHeader();

    KMeansMapTask task = new KMeansMapTask();
    task.init(irisSummaryHeader);
    assertFalse(task.getDontReplaceMissingValues());

    Instances initialCenters = new Instances(irisData, 0);
    initialCenters.add(irisData.firstInstance());
    initialCenters.add(irisData.instance(50));
    initialCenters.add(irisData.instance(100));

    initialCenters = task.applyFilters(initialCenters);
    task.setCentroids(initialCenters);

    // processInstance() should not raise exceptions now
    for (int i = 0; i < irisData.numInstances(); i++) {
      task.processInstance(irisData.instance(i));
    }

    List<Instances> centroidStats = task.getCentroidStats();
    assertEquals(3, centroidStats.size());

    // size of each should be 50 instances
    for (Instances i : centroidStats) {
      Attribute summary = i.attribute(5);
      assertEquals(50,
        (int) ArffSummaryNumericMetric.COUNT
          .valueFromAttribute(summary));
    }
  }

  @Test
  public void testClusteringWithRemoveFilter() throws Exception {

    Instances irisData = CorrelationMatrixMapTaskTest.getIris();
    Instances irisSummaryHeader = getIrisSummaryHeader();

    KMeansMapTask task = new KMeansMapTask();
    task
      .setOptions(Utils
        .splitOptions("-filter \"weka.filters.unsupervised.attribute.Remove -R last\""));

    Instances irisTransformedNoSummary = task.init(irisSummaryHeader);
    assertEquals(4, irisTransformedNoSummary.numAttributes());
    assertFalse(task.getDontReplaceMissingValues());

    Instances initialCenters = new Instances(irisData, 0);
    initialCenters.add(irisData.firstInstance());
    initialCenters.add(irisData.instance(50));
    initialCenters.add(irisData.instance(100));

    initialCenters = task.applyFilters(initialCenters);
    assertEquals(4, initialCenters.numAttributes());

    task.setCentroids(initialCenters);

    // processInstance() should not raise exceptions now
    for (int i = 0; i < irisData.numInstances(); i++) {
      task.processInstance(irisData.instance(i));
    }

    List<Instances> centroidStats = task.getCentroidStats();
    assertEquals(3, centroidStats.size());

    // clusters are different this time because we
    // removed the last attribute
    assertEquals(53,
      (int) ArffSummaryNumericMetric.COUNT
        .valueFromAttribute(centroidStats.get(0).attribute(4)));
    assertEquals(66,
      (int) ArffSummaryNumericMetric.COUNT
        .valueFromAttribute(centroidStats.get(1).attribute(4)));
    assertEquals(31,
      (int) ArffSummaryNumericMetric.COUNT
        .valueFromAttribute(centroidStats.get(2).attribute(4)));
  }

  @Test
  public void testReduceClustersNoFilters() throws Exception {
    Instances irisData = CorrelationMatrixMapTaskTest.getIris();
    Instances irisSummaryHeader = getIrisSummaryHeader();

    KMeansMapTask[] task = new KMeansMapTask[2];
    task[0] = new KMeansMapTask();
    task[1] = new KMeansMapTask();

    Instances irisTransformedNoSummary = task[0].init(irisSummaryHeader);
    task[1].init(irisSummaryHeader);

    Instances initialCenters = new Instances(irisData, 0);
    initialCenters.add(irisData.firstInstance());
    initialCenters.add(irisData.instance(50));
    initialCenters.add(irisData.instance(100));

    // randomize the iris data
    irisData.randomize(new Random(1));

    initialCenters = task[0].applyFilters(initialCenters);
    task[0].setCentroids(initialCenters);
    task[1].setCentroids(initialCenters);

    // processInstance() should not raise exceptions now
    for (int i = 0; i < irisData.numInstances(); i++) {
      if (i < 75) {
        task[0].processInstance(irisData.instance(i));
      } else {
        task[1].processInstance(irisData.instance(i));
      }
    }

    List<Instances> centroidStats1 = task[0].getCentroidStats();
    List<Instances> centroidStats2 = task[1].getCentroidStats();
    assertEquals(3, centroidStats1.size());
    assertEquals(3, centroidStats2.size());

    // partial clusters will have counts less than 50
    assertTrue((int) ArffSummaryNumericMetric.COUNT
      .valueFromAttribute(centroidStats1.get(0).attribute(5)) < 50);
    assertTrue((int) ArffSummaryNumericMetric.COUNT
      .valueFromAttribute(centroidStats1.get(1).attribute(5)) < 50);
    assertTrue((int) ArffSummaryNumericMetric.COUNT
      .valueFromAttribute(centroidStats1.get(2).attribute(5)) < 50);

    assertTrue((int) ArffSummaryNumericMetric.COUNT
      .valueFromAttribute(centroidStats2.get(0).attribute(5)) < 50);
    assertTrue((int) ArffSummaryNumericMetric.COUNT
      .valueFromAttribute(centroidStats2.get(1).attribute(5)) < 50);
    assertTrue((int) ArffSummaryNumericMetric.COUNT
      .valueFromAttribute(centroidStats2.get(2).attribute(5)) < 50);

    KMeansReduceTask reduceTask = new KMeansReduceTask();
    List<List<Instances>> clusterSummaries = new ArrayList<List<Instances>>();
    clusterSummaries.add(centroidStats1);
    clusterSummaries.add(centroidStats2);

    reduceTask =
      reduceTask.reduceClusters(0, 0, irisTransformedNoSummary,
        clusterSummaries);

    assertTrue(reduceTask != null);

    List<Instances> aggregatedCentroidSummaries =
      reduceTask.getAggregatedCentroidSummaries();
    assertTrue(aggregatedCentroidSummaries != null);
    assertEquals(3, aggregatedCentroidSummaries.size());

    // aggregated cluster stats will have counts of 50 (in this case)
    assertEquals(50,
      (int) ArffSummaryNumericMetric.COUNT
        .valueFromAttribute(aggregatedCentroidSummaries.get(0).attribute(5)));
    assertEquals(50,
      (int) ArffSummaryNumericMetric.COUNT
        .valueFromAttribute(aggregatedCentroidSummaries.get(1).attribute(5)));
    assertEquals(50,
      (int) ArffSummaryNumericMetric.COUNT
        .valueFromAttribute(aggregatedCentroidSummaries.get(2).attribute(5)));

    // dummy distance function priming data should contain global min/max
    // for numeric attributes
    Instances dummyPriming = reduceTask.getGlobalDistanceFunctionPrimingData();
    assertTrue(dummyPriming != null);
    assertEquals(2, dummyPriming.numInstances());

    assertEquals(4.3, dummyPriming.instance(0).value(0), 0.0001);
    assertEquals(7.9, dummyPriming.instance(1).value(0), 0.0001);

    double totalError = reduceTask.getTotalWithinClustersError();
    assertEquals(17.712, totalError, 0.0001);
  }

  @Test
  public void testReduceClustersNoFiltersEmptyClusters() throws Exception {
    Instances irisData = CorrelationMatrixMapTaskTest.getIris();
    Instances irisSummaryHeader = getIrisSummaryHeader();

    KMeansMapTask[] task = new KMeansMapTask[2];
    task[0] = new KMeansMapTask();
    task[1] = new KMeansMapTask();

    Instances irisTransformedNoSummary = task[0].init(irisSummaryHeader);
    task[1].init(irisSummaryHeader);

    Instances initialCenters = new Instances(irisData, 0);
    initialCenters.add(irisData.firstInstance());
    initialCenters.add(irisData.instance(50));
    initialCenters.add(irisData.instance(100));

    initialCenters = task[0].applyFilters(initialCenters);
    task[0].setCentroids(initialCenters);
    task[1].setCentroids(initialCenters);

    // processInstance() should not raise exceptions now
    for (int i = 0; i < irisData.numInstances(); i++) {
      if (i < 75) {
        task[0].processInstance(irisData.instance(i));
      } else {
        task[1].processInstance(irisData.instance(i));
      }
    }

    List<Instances> centroidStats1 = task[0].getCentroidStats();
    List<Instances> centroidStats2 = task[1].getCentroidStats();
    assertEquals(3, centroidStats1.size());
    assertEquals(3, centroidStats2.size());

    // since we didn't randomly shuffle iris there will be no
    // virginica instances seen by task[0] and no
    // setosas seen by task[1]
    assertTrue(centroidStats1.get(2) == null);
    assertTrue(centroidStats2.get(0) == null);

    KMeansReduceTask reduceTask = new KMeansReduceTask();
    List<List<Instances>> clusterSummaries = new ArrayList<List<Instances>>();
    clusterSummaries.add(centroidStats1);
    clusterSummaries.add(centroidStats2);

    reduceTask =
      reduceTask.reduceClusters(0, 0, irisTransformedNoSummary,
        clusterSummaries);

    assertTrue(reduceTask != null);

    List<Instances> aggregatedCentroidSummaries =
      reduceTask.getAggregatedCentroidSummaries();
    assertTrue(aggregatedCentroidSummaries != null);
    assertEquals(3, aggregatedCentroidSummaries.size());

    // aggregated cluster stats will have counts of 50 (in this case)
    assertEquals(50,
      (int) ArffSummaryNumericMetric.COUNT
        .valueFromAttribute(aggregatedCentroidSummaries.get(0).attribute(5)));
    assertEquals(50,
      (int) ArffSummaryNumericMetric.COUNT
        .valueFromAttribute(aggregatedCentroidSummaries.get(1).attribute(5)));
    assertEquals(50,
      (int) ArffSummaryNumericMetric.COUNT
        .valueFromAttribute(aggregatedCentroidSummaries.get(2).attribute(5)));

    // for (Instances i : aggregatedCentroidSummaries) {
    // System.err.println(i);
    // }

    // dummy distance function priming data should contain global min/max
    // for numeric attributes
    Instances dummyPriming = reduceTask.getGlobalDistanceFunctionPrimingData();
    assertTrue(dummyPriming != null);
    assertEquals(2, dummyPriming.numInstances());

    assertEquals(4.3, dummyPriming.instance(0).value(0), 0.0001);
    assertEquals(7.9, dummyPriming.instance(1).value(0), 0.0001);

    double totalError = reduceTask.getTotalWithinClustersError();
    assertEquals(17.712, totalError, 0.0001);
  }

  @Test
  public void testReduceClustersRemoveFilter() throws Exception {
    Instances irisData = CorrelationMatrixMapTaskTest.getIris();
    Instances irisSummaryHeader = getIrisSummaryHeader();

    KMeansMapTask[] task = new KMeansMapTask[2];
    task[0] = new KMeansMapTask();
    task[1] = new KMeansMapTask();

    task[0]
      .setOptions(Utils
        .splitOptions("-filter \"weka.filters.unsupervised.attribute.Remove -R last\""));
    task[1]
      .setOptions(Utils
        .splitOptions("-filter \"weka.filters.unsupervised.attribute.Remove -R last\""));

    Instances irisTransformedNoSummary = task[0].init(irisSummaryHeader);
    task[1].init(irisSummaryHeader);

    Instances initialCenters = new Instances(irisData, 0);
    initialCenters.add(irisData.firstInstance());
    initialCenters.add(irisData.instance(50));
    initialCenters.add(irisData.instance(100));

    // randomize the iris data
    irisData.randomize(new Random(1));

    initialCenters = task[0].applyFilters(initialCenters);
    task[0].setCentroids(initialCenters);
    task[1].setCentroids(initialCenters);

    // processInstance() should not raise exceptions now
    for (int i = 0; i < irisData.numInstances(); i++) {
      if (i < 75) {
        task[0].processInstance(irisData.instance(i));
      } else {
        task[1].processInstance(irisData.instance(i));
      }
    }

    List<Instances> centroidStats1 = task[0].getCentroidStats();
    List<Instances> centroidStats2 = task[1].getCentroidStats();
    assertEquals(3, centroidStats1.size());
    assertEquals(3, centroidStats2.size());

    // partial clusters will have counts less than 50
    assertTrue((int) ArffSummaryNumericMetric.COUNT
      .valueFromAttribute(centroidStats1.get(0).attribute(5)) < 50);
    assertTrue((int) ArffSummaryNumericMetric.COUNT
      .valueFromAttribute(centroidStats1.get(1).attribute(5)) < 50);
    assertTrue((int) ArffSummaryNumericMetric.COUNT
      .valueFromAttribute(centroidStats1.get(2).attribute(5)) < 50);

    assertTrue((int) ArffSummaryNumericMetric.COUNT
      .valueFromAttribute(centroidStats2.get(0).attribute(5)) < 50);
    assertTrue((int) ArffSummaryNumericMetric.COUNT
      .valueFromAttribute(centroidStats2.get(1).attribute(5)) < 50);
    assertTrue((int) ArffSummaryNumericMetric.COUNT
      .valueFromAttribute(centroidStats2.get(2).attribute(5)) < 50);

    KMeansReduceTask reduceTask = new KMeansReduceTask();
    List<List<Instances>> clusterSummaries = new ArrayList<List<Instances>>();
    clusterSummaries.add(centroidStats1);
    clusterSummaries.add(centroidStats2);

    reduceTask =
      reduceTask.reduceClusters(0, 0, irisTransformedNoSummary,
        clusterSummaries);

    assertTrue(reduceTask != null);

    List<Instances> aggregatedCentroidSummaries =
      reduceTask.getAggregatedCentroidSummaries();
    assertTrue(aggregatedCentroidSummaries != null);
    assertEquals(3, aggregatedCentroidSummaries.size());

    assertEquals(56,
      (int) ArffSummaryNumericMetric.COUNT
        .valueFromAttribute(aggregatedCentroidSummaries.get(0).attribute(5)));
    assertEquals(66,
      (int) ArffSummaryNumericMetric.COUNT
        .valueFromAttribute(aggregatedCentroidSummaries.get(1).attribute(5)));
    assertEquals(28,
      (int) ArffSummaryNumericMetric.COUNT
        .valueFromAttribute(aggregatedCentroidSummaries.get(2).attribute(5)));

    // dummy distance function priming data should contain global min/max
    // for numeric attributes
    Instances dummyPriming = reduceTask.getGlobalDistanceFunctionPrimingData();
    assertTrue(dummyPriming != null);
    assertEquals(2, dummyPriming.numInstances());

    assertEquals(4.3, dummyPriming.instance(0).value(0), 0.0001);
    assertEquals(7.9, dummyPriming.instance(1).value(0), 0.0001);

    double totalError = reduceTask.getTotalWithinClustersError();
    assertEquals(29.6998, totalError, 0.0001);
  }

  public static void main(String[] args) {
    try {
      KMeansTaskTest test = new KMeansTaskTest();
      test.testInitializationNoFilters();
      test.testClusteringMissingValuesReplacementOnly();
      test.testInitializationDontReplaceMissing();
      test.testClusteringWithRemoveFilter();
      test.testReduceClustersNoFilters();
      test.testReduceClustersNoFiltersEmptyClusters();
      test.testReduceClustersRemoveFilter();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
