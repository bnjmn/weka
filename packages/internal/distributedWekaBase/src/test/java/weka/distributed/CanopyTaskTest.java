package weka.distributed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.StringReader;

import org.junit.Test;

import weka.clusterers.Canopy;
import weka.core.Instances;
import weka.core.Utils;
import weka.filters.MakePreconstructedFilter;
import distributed.core.DistributedUtils;

public class CanopyTaskTest {

  @Test
  public void testBasicInit() throws Exception {
    Instances train = new Instances(new BufferedReader(new StringReader(
      CorrelationMatrixMapTaskTest.IRIS)));

    Instances headerWithSummary = DistributedUtils.makeHeaderWithSummaryAtts(
      train, false);

    CanopyMapTask task = new CanopyMapTask();
    task.init(headerWithSummary);

    assertTrue(task.m_missingValuesReplacer != null);

    assertEquals(task.getT1MapPhase(), "" + Canopy.DEFAULT_T1);
    assertEquals(task.getT2MapPhase(), "" + Canopy.DEFAULT_T2);
    assertTrue(task.m_filtersToUse == null);
  }

  @Test
  public void testInitWithFiltersNoMissingValuesReplacement() throws Exception {
    Instances train = new Instances(new BufferedReader(new StringReader(
      CorrelationMatrixMapTaskTest.IRIS)));

    Instances headerWithSummary = DistributedUtils.makeHeaderWithSummaryAtts(
      train, false);

    CanopyMapTask task = new CanopyMapTask();
    task
      .setOptions(Utils
        .splitOptions("-dont-replace-missing -filter \"weka.filters.unsupervised.attribute.Remove -R 1\""));
    task.init(headerWithSummary);

    assertTrue(task.m_missingValuesReplacer == null);
    assertTrue(task.m_filtersToUse != null);
    assertEquals(task.m_filtersToUse.size(), 1);

    assertTrue(task.m_finalFullPreprocess instanceof MakePreconstructedFilter);
    assertTrue(((MakePreconstructedFilter) task.m_finalFullPreprocess)
      .getBaseFilter() instanceof weka.filters.unsupervised.attribute.Remove);
  }

  @Test
  public void testInitWithFiltersAndMissingValuesReplacement() throws Exception {
    Instances train = new Instances(new BufferedReader(new StringReader(
      CorrelationMatrixMapTaskTest.IRIS)));

    Instances headerWithSummary = DistributedUtils.makeHeaderWithSummaryAtts(
      train, false);

    CanopyMapTask task = new CanopyMapTask();
    task
      .setOptions(Utils
        .splitOptions("-filter \"weka.filters.unsupervised.attribute.Remove -R 1\""));
    task.init(headerWithSummary);

    assertTrue(task.m_missingValuesReplacer != null);
    assertTrue(task.m_filtersToUse != null);
    assertEquals(task.m_filtersToUse.size(), 1);

    assertTrue(task.m_finalFullPreprocess instanceof MakePreconstructedFilter);

    // Expecting a MultiFilter here because the first filter used *must* be the
    // missing values replacer so that missing values are replaced on the
    // original data format before all other filtering happens
    assertTrue(((MakePreconstructedFilter) task.m_finalFullPreprocess)
      .getBaseFilter() instanceof weka.filters.MultiFilter);
  }

  public static void main(String[] args) {
    try {
      CanopyTaskTest t = new CanopyTaskTest();

      t.testBasicInit();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
