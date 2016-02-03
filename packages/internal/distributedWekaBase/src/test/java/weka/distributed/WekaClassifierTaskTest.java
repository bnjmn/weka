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
 *    WekaScoringTaskTest.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed;

import org.junit.Test;
import weka.classifiers.Classifier;
import weka.classifiers.meta.AggregateableFilteredClassifier;
import weka.classifiers.meta.AggregateableFilteredClassifierUpdateable;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.meta.FilteredClassifierUpdateable;
import weka.classifiers.meta.Vote;
import weka.core.Instances;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.MakePreconstructedFilter;
import weka.filters.MultiFilter;
import weka.filters.unsupervised.attribute.NominalToBinary;
import weka.filters.unsupervised.attribute.Remove;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test class for classifier building map and reduce tasks
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class WekaClassifierTaskTest {

  protected WekaClassifierMapTask setupBatchClassifier() {
    WekaClassifierMapTask task = new WekaClassifierMapTask();

    task.setClassifier(new weka.classifiers.trees.J48());

    return task;
  }

  protected WekaClassifierMapTask setupIncrementalClassifier() {
    WekaClassifierMapTask task = new WekaClassifierMapTask();

    task.setClassifier(new weka.classifiers.bayes.NaiveBayesUpdateable());

    return task;
  }

  protected WekaClassifierMapTask setupIncrementalNonAggregateableClassifier() {
    WekaClassifierMapTask task = new WekaClassifierMapTask();

    task.setClassifier(new weka.classifiers.lazy.LWL());

    return task;
  }

  protected WekaClassifierMapTask setupAggregateableBatchClassifier() {
    WekaClassifierMapTask task = new WekaClassifierMapTask();

    task.setClassifier(new weka.classifiers.bayes.NaiveBayes());

    return task;
  }

  protected WekaClassifierMapTask setupAggregateableIncrementalClassifier() {
    return setupIncrementalClassifier();
  }

  protected WekaClassifierMapTask setupBatchRegressor() {
    WekaClassifierMapTask task = new WekaClassifierMapTask();

    task.setClassifier(new weka.classifiers.trees.M5P());

    return task;
  }

  protected WekaClassifierMapTask setupIncrementalRegressor() {
    WekaClassifierMapTask task = new WekaClassifierMapTask();

    weka.classifiers.functions.SGD sgd = new weka.classifiers.functions.SGD();
    try {
      sgd.setOptions(Utils.splitOptions("-F 2"));

      task.setClassifier(sgd);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return task;
  }

  @Test
  public void testSimpleBatchClassifierTraining() throws Exception {
    Instances train = new Instances(new BufferedReader(new StringReader(
      CorrelationMatrixMapTaskTest.IRIS)));

    train.setClassIndex(train.numAttributes() - 1);
    WekaClassifierMapTask task = setupBatchClassifier();

    task.setup(new Instances(train, 0));

    for (int i = 0; i < train.numInstances(); i++) {
      task.processInstance(train.instance(i));
    }

    // classifier training should happen in the finalizeTask() method for
    // batch learners
    assertTrue(task.getClassifier().toString()
      .startsWith("No classifier built"));

    task.finalizeTask();

    assertTrue(task.getClassifier().toString().startsWith("J48 pruned tree"));
  }

  @Test
  public void testSimpleBatchRegressorTraining() throws Exception {
    Instances train = new Instances(new BufferedReader(new StringReader(
      CorrelationMatrixMapTaskTest.BOLTS)));

    train.setClassIndex(train.numAttributes() - 1);

    WekaClassifierMapTask task = setupBatchRegressor();
    task.setup(new Instances(train, 0));

    for (int i = 0; i < train.numInstances(); i++) {
      task.processInstance(train.instance(i));
    }

    assertTrue(task.getClassifier().toString()
      .startsWith("Classifier hasn't been built yet"));

    task.finalizeTask();

    assertTrue(task.getClassifier().toString()
      .startsWith("M5 pruned model tree"));
  }

  @Test
  public void testIncrementalRegressorTraining() throws Exception {
    Instances train = new Instances(new BufferedReader(new StringReader(
      CorrelationMatrixMapTaskTest.BOLTS)));

    train.setClassIndex(train.numAttributes() - 1);

    WekaClassifierMapTask task = setupIncrementalRegressor();
    assertTrue(task.getClassifier().toString()
      .startsWith("SGD: No model built yet"));

    task.setup(new Instances(train, 0));

    for (int i = 0; i < train.numInstances(); i++) {
      task.processInstance(train.instance(i));
    }

    assertTrue(task.getClassifier().toString()
      .startsWith("Loss function: Squared loss (linear regression)"));
  }

  @Test
  public void testBatchClassifierWithReservoirSampling() throws Exception {

    // first classifier on all the data
    Instances train = new Instances(new BufferedReader(new StringReader(
      CorrelationMatrixMapTaskTest.IRIS)));

    train.setClassIndex(train.numAttributes() - 1);
    WekaClassifierMapTask task = setupBatchClassifier();

    task.setup(new Instances(train, 0));

    for (int i = 0; i < train.numInstances(); i++) {
      task.processInstance(train.instance(i));
    }

    // classifier training should happen in the finalizeTask() method for
    // batch learners
    assertTrue(task.getClassifier().toString()
      .startsWith("No classifier built"));

    task.finalizeTask();

    assertTrue(task.getClassifier().toString().startsWith("J48 pruned tree"));

    // second classifier on a 50% sample
    WekaClassifierMapTask task50 = setupBatchClassifier();
    task50.setUseReservoirSamplingWhenBatchLearning(true);
    task50.setReservoirSampleSize(75);
    task50.setup(new Instances(train, 0));

    for (int i = 0; i < train.numInstances(); i++) {
      task50.processInstance(train.instance(i));
    }

    assertTrue(task50.getClassifier().toString()
      .startsWith("No classifier built"));

    task50.finalizeTask();

    assertFalse(task.getClassifier().toString()
      .equals(task50.getClassifier().toString()));
  }

  @Test
  public void testIncrementalRegressorTrainingMultipleIterations()
    throws Exception {
    Instances train = new Instances(new BufferedReader(new StringReader(
      CorrelationMatrixMapTaskTest.BOLTS)));

    train.setClassIndex(train.numAttributes() - 1);

    WekaClassifierMapTask task = setupIncrementalRegressor();
    assertTrue(task.getClassifier().toString()
      .startsWith("SGD: No model built yet"));

    task.setup(new Instances(train, 0));

    String firstIteration = "";
    String secondIteration = "";

    for (int j = 0; j < 2; j++) {
      for (int i = 0; i < train.numInstances(); i++) {
        task.processInstance(train.instance(i));
      }

      if (j == 0) {
        firstIteration = task.getClassifier().toString();
        assertTrue(firstIteration
          .startsWith("Loss function: Squared loss (linear regression)"));

        // make a new class and initialize it with our trained classifier
        // from iteration 1
        Classifier c = task.getClassifier();
        task = setupIncrementalRegressor();
        task.setClassifier(c);
        task.setContinueTrainingUpdateableClassifier(true);
      }
    }
    secondIteration = task.getClassifier().toString();
    assertFalse(firstIteration.equals(secondIteration));
  }

  @Test
  public void testSimpleIncrementalClassifierTraining() throws Exception {
    Instances train = new Instances(new BufferedReader(new StringReader(
      CorrelationMatrixMapTaskTest.IRIS)));

    train.setClassIndex(train.numAttributes() - 1);
    WekaClassifierMapTask task = setupIncrementalClassifier();

    assertTrue(task.getClassifier().toString()
      .startsWith("Naive Bayes Classifier: No model built yet"));

    task.setup(new Instances(train, 0));

    // classifier is updated on each instance
    String model70 = "";
    String modelFull = "";
    for (int i = 0; i < train.numInstances(); i++) {
      task.processInstance(train.instance(i));

      if (i == 70) {
        model70 = task.getClassifier().toString();
      }
    }
    modelFull = task.getClassifier().toString();

    assertFalse(modelFull.equals(model70));
  }

  @Test
  public void testForceBatchTrainingForUpdateableClassifier() throws Exception {
    Instances train = new Instances(new BufferedReader(new StringReader(
      CorrelationMatrixMapTaskTest.IRIS)));

    train.setClassIndex(train.numAttributes() - 1);
    WekaClassifierMapTask task = setupIncrementalClassifier();
    task.setForceBatchLearningForUpdateableClassifiers(true);

    task.setup(new Instances(train, 0));

    assertTrue(task.getClassifier().toString()
      .startsWith("Naive Bayes Classifier: No model built yet"));

    for (int i = 0; i < train.numInstances(); i++) {
      task.processInstance(train.instance(i));
    }

    // forcing batch training, so classifier should not be built yet
    assertTrue(task.getClassifier().toString()
      .startsWith("Naive Bayes Classifier: No model built yet"));

    // build classifier
    task.finalizeTask();

    assertFalse(task.getClassifier().toString()
      .startsWith("Naive Bayes Classifier: No model built yet"));
  }

  @Test
  public void testFilterWrappingAggregateableClassifier() throws Exception {
    Instances train = new Instances(new BufferedReader(new StringReader(
      CorrelationMatrixMapTaskTest.IRIS)));

    train.setClassIndex(train.numAttributes() - 1);
    WekaClassifierMapTask task = setupAggregateableBatchClassifier();

    // Single filter with non-incremental Aggregateable classifier
    Filter oneF = new Remove();

    Filter[] filters = new Filter[1];
    filters[0] = oneF;
    task.setFiltersToUse(filters);

    task.setup(new Instances(train, 0));

    Classifier wrapped = task.getClassifier();
    assertTrue(wrapped instanceof AggregateableFilteredClassifier);

    // multiple filters with non-incremental Aggregateable classifier
    filters = new Filter[2];
    Filter twoF = new NominalToBinary();
    filters[0] = twoF;
    filters[1] = oneF;
    task.setFiltersToUse(filters);

    task.setup(new Instances(train, 0));

    wrapped = task.getClassifier();
    assertTrue(wrapped instanceof AggregateableFilteredClassifier);
    Filter f = ((AggregateableFilteredClassifier) wrapped)
      .getPreConstructedFilter();
    assertTrue(f instanceof MakePreconstructedFilter);

    // base filter should be a MultiFilter
    Filter f2 = ((MakePreconstructedFilter) f).getBaseFilter();
    assertTrue(f2 instanceof MultiFilter);

    // single filter with incremental Aggregateable classifier
    task = setupAggregateableIncrementalClassifier();
    filters = new Filter[1];
    filters[0] = oneF;
    task.setFiltersToUse(filters);
    task.setup(new Instances(train, 0));

    wrapped = task.getClassifier();

    assertTrue(wrapped instanceof AggregateableFilteredClassifierUpdateable);

    // single MakePreconstructedFilter
    assertTrue(((AggregateableFilteredClassifierUpdateable) wrapped)
      .getPreConstructedFilter() instanceof MakePreconstructedFilter);

    f = ((AggregateableFilteredClassifierUpdateable) wrapped)
      .getPreConstructedFilter();
    assertTrue(((MakePreconstructedFilter) f).getBaseFilter() instanceof Remove);

    // multiple filters with incremental Aggregateable classifier
    filters = new Filter[2];
    filters[0] = twoF;
    filters[1] = oneF;
    task.setFiltersToUse(filters);
    task.setup(new Instances(train, 0));

    wrapped = task.getClassifier();
    assertTrue(wrapped instanceof AggregateableFilteredClassifierUpdateable);
    assertTrue(((AggregateableFilteredClassifierUpdateable) wrapped)
      .getPreConstructedFilter() instanceof MakePreconstructedFilter);
    f = ((AggregateableFilteredClassifierUpdateable) wrapped)
      .getPreConstructedFilter();
    assertTrue(((MakePreconstructedFilter) f).getBaseFilter() instanceof MultiFilter);
  }

  @Test
  public void testFilterWrappingNonAggregateableClassifier() throws Exception {
    Instances train = new Instances(new BufferedReader(new StringReader(
      CorrelationMatrixMapTaskTest.IRIS)));

    train.setClassIndex(train.numAttributes() - 1);
    WekaClassifierMapTask task = setupBatchClassifier();

    // Single filter with non-incremental non-aggregateable classifier
    Filter oneF = new Remove();

    Filter[] filters = new Filter[1];
    filters[0] = oneF;
    task.setFiltersToUse(filters);

    task.setup(new Instances(train, 0));
    Classifier wrapped = task.getClassifier();

    assertTrue(wrapped instanceof FilteredClassifier);
    assertTrue(((FilteredClassifier) wrapped).getFilter() instanceof Remove);

    // Single filter with incremental non-aggregateable classifier
    task = setupIncrementalNonAggregateableClassifier();
    task.setFiltersToUse(filters);
    task.setup(new Instances(train, 0));

    wrapped = task.getClassifier();
    assertTrue(wrapped instanceof FilteredClassifierUpdateable);
    assertTrue(((FilteredClassifierUpdateable) wrapped).getFilter() instanceof Remove);
  }

  @Test
  public void testAggregatingAggregateableClassifiers() throws Exception {
    Instances train = new Instances(new BufferedReader(new StringReader(
      CorrelationMatrixMapTaskTest.IRIS)));

    train.setClassIndex(train.numAttributes() - 1);
    WekaClassifierMapTask task = setupAggregateableBatchClassifier();
    task.setup(new Instances(train, 0));

    for (int i = 0; i < train.numInstances(); i++) {
      task.processInstance(train.instance(i));
    }
    task.finalizeTask();

    Classifier c1 = task.getClassifier();
    String c1S = c1.toString();

    task = setupAggregateableBatchClassifier();
    task.setUseReservoirSamplingWhenBatchLearning(true);
    task.setReservoirSampleSize(75);
    task.setup(new Instances(train, 0));

    for (int i = 0; i < train.numInstances(); i++) {
      task.processInstance(train.instance(i));
    }
    task.finalizeTask();

    Classifier c2 = task.getClassifier();
    String c2S = c2.toString();

    // different classifiers
    assertFalse(c1S.equals(c2S));

    WekaClassifierReduceTask reduce = new WekaClassifierReduceTask();
    List<Classifier> toAgg = new ArrayList<Classifier>();
    toAgg.add(c1);
    toAgg.add(c2);
    Classifier aggregated = reduce.aggregate(toAgg);
    String aggregatedS = aggregated.toString();

    // aggregated classifier differs from both base classifier
    assertFalse(aggregatedS.equals(c1S));
    assertFalse(aggregatedS.equals(c2S));
  }

  @Test
  public void testAggregatingNonAggregateableClassifiers() throws Exception {
    Instances train = new Instances(new BufferedReader(new StringReader(
      CorrelationMatrixMapTaskTest.IRIS)));

    train.setClassIndex(train.numAttributes() - 1);
    WekaClassifierMapTask task = setupBatchClassifier();
    task.setup(new Instances(train, 0));

    for (int i = 0; i < train.numInstances(); i++) {
      task.processInstance(train.instance(i));
    }
    task.finalizeTask();

    Classifier c1 = task.getClassifier();
    String c1S = c1.toString();

    task = setupBatchClassifier();
    task.setUseReservoirSamplingWhenBatchLearning(true);
    task.setReservoirSampleSize(75);
    task.setup(new Instances(train, 0));

    for (int i = 0; i < train.numInstances(); i++) {
      task.processInstance(train.instance(i));
    }
    task.finalizeTask();

    Classifier c2 = task.getClassifier();
    String c2S = c2.toString();

    // different classifiers
    assertFalse(c1S.equals(c2S));

    WekaClassifierReduceTask reduce = new WekaClassifierReduceTask();
    List<Classifier> toAgg = new ArrayList<Classifier>();
    toAgg.add(c1);
    toAgg.add(c2);
    Classifier aggregated = reduce.aggregate(toAgg);

    assertTrue(aggregated instanceof Vote);
  }

  @Test
  public void testAggregatingAggregateableClassifiersForceVote()
    throws Exception {
    Instances train = new Instances(new BufferedReader(new StringReader(
      CorrelationMatrixMapTaskTest.IRIS)));

    train.setClassIndex(train.numAttributes() - 1);
    WekaClassifierMapTask task = setupAggregateableBatchClassifier();
    task.setup(new Instances(train, 0));

    for (int i = 0; i < train.numInstances(); i++) {
      task.processInstance(train.instance(i));
    }
    task.finalizeTask();

    Classifier c1 = task.getClassifier();
    String c1S = c1.toString();

    task = setupAggregateableBatchClassifier();
    task.setUseReservoirSamplingWhenBatchLearning(true);
    task.setReservoirSampleSize(75);
    task.setup(new Instances(train, 0));

    for (int i = 0; i < train.numInstances(); i++) {
      task.processInstance(train.instance(i));
    }
    task.finalizeTask();

    Classifier c2 = task.getClassifier();
    String c2S = c2.toString();

    // different classifiers
    assertFalse(c1S.equals(c2S));

    WekaClassifierReduceTask reduce = new WekaClassifierReduceTask();
    List<Classifier> toAgg = new ArrayList<Classifier>();
    toAgg.add(c1);
    toAgg.add(c2);
    Classifier aggregated = reduce.aggregate(toAgg, null, true);

    assertTrue(aggregated instanceof Vote);
  }

  @Test
  public void testAggregatingWithMinTrainingFraction() throws Exception {
    Instances train = new Instances(new BufferedReader(new StringReader(
      CorrelationMatrixMapTaskTest.IRIS)));

    train.setClassIndex(train.numAttributes() - 1);
    WekaClassifierMapTask task = setupAggregateableBatchClassifier();
    task.setup(new Instances(train, 0));

    for (int i = 0; i < train.numInstances(); i++) {
      task.processInstance(train.instance(i));
    }
    task.finalizeTask();

    Classifier c1 = task.getClassifier();
    String c1S = c1.toString();

    task = setupAggregateableBatchClassifier();
    task.setUseReservoirSamplingWhenBatchLearning(true);
    task.setReservoirSampleSize(75);
    task.setup(new Instances(train, 0));

    for (int i = 0; i < train.numInstances(); i++) {
      task.processInstance(train.instance(i));
    }
    task.finalizeTask();

    Classifier c2 = task.getClassifier();
    String c2S = c2.toString();

    // different classifiers
    assertFalse(c1S.equals(c2S));

    task = setupAggregateableBatchClassifier();
    task.setUseReservoirSamplingWhenBatchLearning(true);
    task.setReservoirSampleSize(60);
    task.setup(new Instances(train, 0));

    for (int i = 0; i < train.numInstances(); i++) {
      task.processInstance(train.instance(i));
    }
    task.finalizeTask();

    Classifier c3 = task.getClassifier();
    WekaClassifierReduceTask reduce = new WekaClassifierReduceTask();
    List<Classifier> toAgg = new ArrayList<Classifier>();
    toAgg.add(c1);
    toAgg.add(c2);
    toAgg.add(c3);

    reduce.setMinTrainingFraction(0.5);
    List<Integer> numTraining = new ArrayList<Integer>();
    numTraining.add(150);
    numTraining.add(75);
    numTraining.add(60);

    Classifier aggregated = reduce.aggregate(toAgg, numTraining, false);

    assertFalse(aggregated instanceof Vote);

    List<Integer> discarded = reduce.getDiscarded();
    assertTrue(discarded != null);

    // should be one classifier discarded (< minTrainingFraction)
    assertTrue(discarded.size() == 1);
  }

  public static final void main(String[] args) {
    try {
      WekaClassifierTaskTest t = new WekaClassifierTaskTest();
      t.testSimpleBatchClassifierTraining();
      t.testSimpleIncrementalClassifierTraining();
      t.testForceBatchTrainingForUpdateableClassifier();
      t.testSimpleBatchRegressorTraining();
      t.testIncrementalRegressorTraining();
      t.testIncrementalRegressorTrainingMultipleIterations();
      t.testBatchClassifierWithReservoirSampling();
      t.testFilterWrappingAggregateableClassifier();
      t.testFilterWrappingNonAggregateableClassifier();
      t.testAggregatingAggregateableClassifiers();
      t.testAggregatingNonAggregateableClassifiers();
      t.testAggregatingAggregateableClassifiersForceVote();
      t.testAggregatingWithMinTrainingFraction();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

}
