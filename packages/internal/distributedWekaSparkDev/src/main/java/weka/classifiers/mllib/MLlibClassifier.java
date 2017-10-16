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
 *    MLlibClassifier.java
 *    Copyright (C) 2017 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.mllib;

import distributed.core.DistributedJobConfig;
import org.apache.log4j.WriterAppender;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.mllib.linalg.Vector;
import weka.classifiers.AbstractClassifier;
import weka.core.Attribute;
import weka.core.Environment;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionMetadata;
import weka.core.Utils;
import weka.distributed.DistributedWekaException;
import weka.distributed.spark.CachingStrategy;
import weka.distributed.spark.Dataset;
import weka.distributed.spark.SparkJob;
import weka.distributed.spark.WekaClassifierSparkJob;
import weka.distributed.spark.mllib.util.MLlibDatasetMaker;
import weka.filters.Filter;
import weka.filters.StreamableFilter;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.List;

/**
 * Abstract base class for Weka MLlib wrapper classifiers.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 */
public abstract class MLlibClassifier extends AbstractClassifier implements
  Serializable {

  private static final long serialVersionUID = 3448173396789723946L;

  /** Helper for creating datasets that MLlib algorithms can process */
  protected MLlibDatasetMaker m_datasetMaker;

  /** The class attribute */
  protected Attribute m_classAtt;

  /** Any pre-constructed filter to use */
  protected String m_pathToPreconstructedFilter = "";

  /** True if missing values should not be replaced */
  protected boolean m_dontReplaceMissingValues;

  /**
   * Spark job options. Only used when this classifier is running in
   * "desktop Weka" mode - i.e. in the Explorer, Experimenter, Knowledge Flow or
   * command line on datasets that have been loaded conventionally (i.e. as a
   * set of Weka Instances). In "cluster" mode, cluster config stuff, such as
   * spark master etc., will have been specified in the data source
   * configuration
   */
  protected String m_sparkJobOptions = "-master local[*]";

  /**
   * List valid command-line options
   *
   * @return an enumeration of command-line options
   */
  @Override
  public Enumeration<Option> listOptions() {
    java.util.Vector<Option> opts =
      Option.listOptionsForClassHierarchy(this.getClass(),
        MLlibClassifier.class);

    return opts.elements();
  }

  /**
   * Get current option settings
   *
   * @return an array of option settings
   */
  @Override
  public String[] getOptions() {
    return Option.getOptionsForHierarchy(this, MLlibClassifier.class);
  }

  /**
   * Set options
   *
   * @param options an array of option settings
   * @throws Exception if a problem occurs
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    Option.setOptionsForHierarchy(options, this, MLlibClassifier.class);
  }

  /**
   * Set whether to replace missing values (via the ReplaceMissing filter) or
   * not
   *
   * @param replaceMissingValues true to replace missing values
   */
  @OptionMetadata(displayName = "Don't replace missing values",
    description = "Don't replace any missing values with global means/modes",
    commandLineParamName = "dont-replace-missing",
    commandLineParamSynopsis = "-dont-replace-missing",
    commandLineParamIsFlag = true, displayOrder = 2)
  public void setDontReplaceMissingValues(boolean replaceMissingValues) {
    m_dontReplaceMissingValues = replaceMissingValues;
  }

  /**
   * Get whether to replace missing values (via the ReplaceMissing filter) or
   * not
   *
   * @return true to replace missing values
   */
  public boolean getDontReplaceMissingValues() {
    return m_dontReplaceMissingValues;
  }

  @OptionMetadata(displayName = "Path to preconstructed filter",
    description = "Path to a preconstructed filter (if any)", displayOrder = 3,
    commandLineParamName = "filter",
    commandLineParamSynopsis = "-filter <path to preconstructed filter>")
  public void setPathToPreconstructedFilter(String path) {
    m_pathToPreconstructedFilter = path;
  }

  /**
   * Get the path to the preconstructed filter to use
   *
   * @return the path to the preconstructed filter
   */
  public String getPathToPreconstructedFilter() {
    return m_pathToPreconstructedFilter;
  }

  /**
   * Set the spark job options
   *
   * @param opts the spark job options
   */
  @OptionMetadata(displayName = "Spark options",
    description = "Spark-specific "
      + "options. Options include -master <spark master host> "
      + "-port <master port> "
      + "-min-slices <number> -max-slices <number>. When running from the "
      + "command-line, " + "enclose spark options in double quotes.",
    commandLineParamName = "spark-opts",
    commandLineParamSynopsis = "-spark-opts <spark options>", displayOrder = 0)
  public void setSparkJobOptions(String opts) {
    m_sparkJobOptions = opts;
  }

  /**
   * Get the spark job options
   *
   * @return the spark job options
   */
  public String getSparkJobOptions() {
    return m_sparkJobOptions;
  }

  /**
   * Build the classifier. Takes an RDD of {@code Instance} objects and creates
   * a dataset that can be processed by the underlying MLlib scheme.
   *
   * @param wekaData an RDD of {@code Instance} objects
   * @param headerWithSummary the header of the data (with summary attributes)
   * @param preprocessors streamable filters to apply to the data. Used when
   *          this classifier is executed on data sourced by big data sources in
   *          a cluster - e.g. data frame read from a CSV file in HDFS via the
   *          CSV data frame source. When the classifier is running in desktop
   *          Weka, and data has been loaded into main memory (and then
   *          parallelized) all of Weka's filters can be used in the normal way
   *          (i.e. via a FilteredClassifier wrapped around the filter(s) and
   *          this classifier).
   * @param strategy the caching strategy to use
   * @throws DistributedWekaException if a problem occurs
   */
  public void buildClassifier(JavaRDD<Instance> wekaData,
    Instances headerWithSummary, List<StreamableFilter> preprocessors,
    CachingStrategy strategy) throws DistributedWekaException {

    m_datasetMaker = new MLlibDatasetMaker();
    learnModel(wekaData, headerWithSummary, preprocessors, strategy);
  }

  /**
   * Learn the underlying MLlib model. Subclasses to implement for their
   * specific MLlib scheme
   *
   * @param wekaData an RDD of {@code Instance} objects
   * @param headerWithSummary
   * @param preprocessors streamable filters for preprocessing the data
   * @param strategy the header of the data (with summary attributes)
   * @throws DistributedWekaException if a problem occurs
   */
  public abstract void learnModel(JavaRDD<Instance> wekaData,
    Instances headerWithSummary, List<StreamableFilter> preprocessors,
    CachingStrategy strategy) throws DistributedWekaException;

  /**
   * Predict a test point using the learned model
   * 
   * @param test a {@code Vector} containing the test point
   * @return the predicted value (index of class for classification, or actual
   *         value for regression)
   * @throws DistributedWekaException if a problem occurs
   */
  public double predict(Vector test) throws DistributedWekaException {
    double[] dist = distributionForVector(test);
    if (dist == null) {
      throw new DistributedWekaException("Null distribution predicted");
    }

    if (m_classAtt.isNominal()) {
      double max = 0;
      int maxIndex = 0;

      for (int i = 0; i < dist.length; i++) {
        if (dist[i] > max) {
          maxIndex = i;
          max = dist[i];
        }
      }
      if (max > 0) {
        return maxIndex;
      } else {
        return Utils.missingValue();
      }
    } else {
      return dist[0];
    }
  }

  /**
   * Predict a test point and return a distribution
   * 
   * @param test a {@code Vector} containing the test point
   * @return the predicted distribution (this will be a single element array for
   *         regression problems)
   * @throws DistributedWekaException if a problem occurs
   */
  public double[] distributionForVector(Vector test)
    throws DistributedWekaException {
    double prediction = predict(test);

    double[] dist =
      new double[m_classAtt.isNominal() ? m_classAtt.numValues() : 1];
    if (m_classAtt.isNominal()) {
      dist[(int) prediction] = 1.0;
    } else {
      dist[0] = prediction;
    }

    return dist;
  }

  /**
   * Helper method for loading a serialized preconstructed filter
   *
   * @return the loaded Filter
   * @throws DistributedWekaException if a problem occurs
   */
  public Filter loadPreconstructedFilterIfNecessary()
    throws DistributedWekaException {
    if (DistributedJobConfig.isEmpty(m_pathToPreconstructedFilter)) {
      return null;
    }

    try {
      return (Filter) WekaClassifierSparkJob
        .loadPreconstructedFilter(Environment.getSystemWide().substitute(
          m_pathToPreconstructedFilter));
    } catch (Exception e) {
      throw new DistributedWekaException(e);
    }
  }

  /**
   * Build this MLlib classifier.
   * 
   * @param instances the instances to train with
   * @throws Exception if a problem occurs
   */
  public void buildClassifier(Instances instances) throws Exception {
    ClassLoader orig = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(
        this.getClass().getClassLoader());

      // can classifier handle the data?
      getCapabilities().testWithFail(instances);

      instances.deleteWithMissingClass();
      SimpleClassifierJob job = new SimpleClassifierJob();
      job.setMLlibClassifier(this);
      job.setInstances(instances);
      job.setSparkJobOptions(getSparkJobOptions());

      JavaSparkContext currentContext =
        MLlibSparkContextManager.getSparkContext();
      WriterAppender currentWriterAppender =
        MLlibSparkContextManager.getWriterAppender();
      if (currentContext == null) {
        currentWriterAppender = job.initJob(null);
        currentContext = job.getSparkContext();
      }
      job.runJobWithContext(currentContext);
    } finally {
      Thread.currentThread().setContextClassLoader(orig);
    }
  }

  /**
   * Predict a test instance
   * 
   * @param test the test instance to predict
   * @return an array containing a probability distribution, or single value in
   *         the case of a regression problem
   * @throws Exception if a problem occurs
   */
  @Override
  public double[] distributionForInstance(Instance test) throws Exception {
    if (m_datasetMaker == null) {
      throw new DistributedWekaException("Model not built yet!");
    }

    Vector testVec = m_datasetMaker.vectorizeInstance(test);
    return distributionForVector(testVec);
  }

  /**
   * Very simple implementation of a
   * {@code SparkJob for executing an MLlib wrapper classifier}
   */
  protected static class SimpleClassifierJob extends SparkJob {

    private static final long serialVersionUID = 1851736315970274990L;

    protected MLlibClassifier m_classifier;

    protected Instances m_trainingData;

    protected String m_sparkJobOpts = "";

    public SimpleClassifierJob() {
      super("MLlibClassifier", "Builds an MLlibClassifier model");
    }

    public void setMLlibClassifier(MLlibClassifier classifier) {
      m_classifier = classifier;

      String jobName = m_classifier.getClass().getCanonicalName();
      jobName = jobName.substring(jobName.lastIndexOf("."));
      setJobName(jobName);
      setJobDescription("Builds a " + jobName + " model");
    }

    public void setInstances(Instances instances) {
      m_trainingData = instances;
    }

    /**
     * Used from GUI apps
     *
     * @param jobOpts
     * @throws Exception
     */
    public void setSparkJobOptions(String jobOpts) throws Exception {
      m_sparkJobOpts = jobOpts;

      String[] optsToSet = Utils.splitOptions(jobOpts);
      super.setOptions(optsToSet);
    }

    @Override
    public boolean runJobWithContext(JavaSparkContext sparkContext)
      throws IOException, DistributedWekaException {
      int maxPartitions =
        !DistributedJobConfig.isEmpty(m_sjConfig.getMaxInputSlices()) ? Integer
          .parseInt(environmentSubstitute(m_sjConfig.getMaxInputSlices()))
          : (int) Math.round(Math.log10(m_trainingData.numInstances()));
      int minPartions =
        !DistributedJobConfig.isEmpty(m_sjConfig.getMinInputSlices()) ? Integer
          .parseInt(environmentSubstitute(m_sjConfig.getMinInputSlices())) : 1;

      int numPartitions =
        minPartions > maxPartitions ? minPartions : maxPartitions;

      Dataset ds =
        Dataset.instancesToDataset(m_trainingData, sparkContext, maxPartitions);
      Instances headerWithSummary = ds.getHeaderWithSummary();
      headerWithSummary.setClassIndex(m_trainingData.classIndex());
      m_classifier.buildClassifier(ds.getRDD(), headerWithSummary, null,
        getCachingStrategy());

      // save memory
      ds.getRDD().unpersist();
      return true;
    }
  }
}
