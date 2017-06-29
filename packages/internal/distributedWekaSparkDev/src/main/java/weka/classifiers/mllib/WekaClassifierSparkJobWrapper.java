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
 *    WekaClassifierSparkJobWrapper.java
 *    Copyright (C) 2017 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.mllib;

import java.io.IOException;
import java.io.Serializable;
import java.util.Enumeration;

import distributed.core.DistributedJobConfig;
import org.apache.log4j.WriterAppender;
import org.apache.spark.api.java.JavaSparkContext;
import spire.random.Dist;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;
import weka.core.Utils;
import weka.distributed.DistributedWekaException;
import weka.distributed.spark.Dataset;
import weka.distributed.spark.SparkJob;
import weka.distributed.spark.WekaClassifierSparkJob;
import weka.knowledgeflow.SingleThreadedExecution;

/**
 * Wraps the WekaClassifierSpark job in a Weka classifier instance, so that it
 * can be applied to standard datasets loaded into Weka on a desktop machine.
 * This allows the distributed Weka approach to building a classifier (i.e.
 * dagging for any base scheme that does not implement Aggregateable) to be
 * compared against standard Weka implementations and MLlib implementations.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@SingleThreadedExecution
public class WekaClassifierSparkJobWrapper extends AbstractClassifier implements
  Serializable {

  private static final long serialVersionUID = -5682154755889362598L;

  /** The user-set classifier */
  protected Classifier m_classifier = new NaiveBayes();

  /**
   * The final trained model (may differ in type to the user-specified
   * classifier as the underlying job might wrap multiple models in a Vote
   * classifier)
   */
  protected Classifier m_finalModel;

  /** Spark-related options */
  protected String m_sparkJobOptions = "-master local[*]";

  /** True to force a voted ensemble for aggregateable classifiers */
  protected boolean m_forceVotedEnsemble;

  /** True to force batch learning for updateable classifiers */
  protected boolean m_forceBatchForUpdateable;

  /**
   * Get a list of options
   *
   * @return an enumeration of options
   */
  @Override
  public Enumeration<Option> listOptions() {
    java.util.Vector<Option> opts =
      Option.listOptionsForClassHierarchy(this.getClass(),
        WekaClassifierSparkJobWrapper.class);

    return opts.elements();
  }

  /**
   * Get an array of currently set options
   *
   * @return an array of current options
   */
  @Override
  public String[] getOptions() {
    return Option.getOptionsForHierarchy(this,
      WekaClassifierSparkJobWrapper.class);
  }

  /**
   * Set options
   *
   * @param options an array of options to set
   * @throws Exception if a problem occurs
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    Option.setOptionsForHierarchy(options, this,
      WekaClassifierSparkJobWrapper.class);
  }

  /**
   * Set the Weka classifier to use
   *
   * @param classifier the Weka classifier to use
   */
  @OptionMetadata(displayName = "Classifier",
    description = "Classifier to use", displayOrder = 1,
    commandLineParamName = "W",
    commandLineParamSynopsis = "-W <classifier spec>")
  public void setClassifier(Classifier classifier) {
    m_classifier = classifier;
  }

  /**
   * Get the Weka classifier to use
   *
   * @return the Weka classifier to use
   */
  public Classifier getClassifier() {
    return m_classifier;
  }

  /**
   * Set the Spark-related options
   *
   * @param opts the Spark-related options
   */
  @OptionMetadata(displayName = "Spark options",
    description = "Spark-specific "
      + "options. Options include -master <spark master host> "
      + "-port <master port> "
      + "-min-slices <number> -max-slices <number>. When running from the "
      + "command-line, " + "enclose spark options in double quotes.",
    commandLineParamName = "spark-opts",
    commandLineParamSynopsis = "-spark-opts <spark options>", displayOrder = 2)
  public void setSparkJobOptions(String opts) {
    m_sparkJobOptions = opts;
  }

  /**
   * Get the Spark-related options
   *
   * @return the Spark-related options
   */
  public String getSparkJobOptions() {
    return m_sparkJobOptions;
  }

  /**
   * Get whether to force the creation of a Vote ensemble for Aggregateable
   * classifiers
   *
   * @return true if a Vote ensemble is to be created even in the case where the
   *         base classifier is directly aggregateable
   */
  public boolean getForceVotedEnsembleCreation() {
    return m_forceVotedEnsemble;
  }

  /**
   * Set whether to force the creation of a Vote ensemble for Aggregateable
   * classifiers
   *
   * @param f true if a Vote ensemble is to be created even in the case where
   *          the base classifier is directly aggregateable
   */
  @OptionMetadata(
    displayName = "Forced voted ensemble for aggregatable classifiers",
    description = "Force the final classifier to be a voted ensemble, "
      + "even if the base model is aggregatable",
    commandLineParamName = "force-vote",
    commandLineParamSynopsis = "-force-vote", commandLineParamIsFlag = true,
    displayOrder = 3)
  public void setForceVotedEnsembleCreation(boolean f) {
    m_forceVotedEnsemble = f;
  }

  /**
   * Get whether to force batch training for incremental (Updateable)
   * classifiers
   *
   * @return true if incremental classifiers should be batch trained
   */
  public boolean getForceBatchLearningForUpdateableClassifiers() {
    return m_forceBatchForUpdateable;
  }

  /**
   * Set whether to force batch training for incremental (Updateable)
   * classifiers
   *
   * @param force true if incremental classifiers should be batch trained
   */
  @OptionMetadata(
    displayName = "Force batch learning for incremental classifiers",
    description = "Force incremental classifiers to be trained in a batch "
      + "fashion", commandLineParamName = "force-batch",
    commandLineParamSynopsis = "-force-batch", commandLineParamIsFlag = true,
    displayOrder = 4)
  public void setForceBatchLearningForUpdateableClassifiers(boolean force) {
    m_forceBatchForUpdateable = force;
  }

  /**
   * Build the classifier
   *
   * @param data the training data
   * @throws Exception if a problem occurs
   */
  @Override
  public void buildClassifier(Instances data) throws Exception {
    ClassLoader orig = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(
        this.getClass().getClassLoader());

      data = new Instances(data);
      getCapabilities().testWithFail(data);
      data.deleteWithMissingClass();
      SimpleClassifierJob job = new SimpleClassifierJob();
      job.setWekaClassifier(m_classifier);
      job.setForceVote(getForceVotedEnsembleCreation());
      job.setForceBatch(getForceBatchLearningForUpdateableClassifiers());
      job.setInstances(data);
      job.setSparkJobOptions(getSparkJobOptions());

      JavaSparkContext currentContext =
        MLlibSparkContextManager.getSparkContext();
      WriterAppender currentWriterAppender =
        MLlibSparkContextManager.getWriterAppender();
      if (currentContext == null) {
        currentWriterAppender = job.initJob(null);
        currentContext = job.getSparkContext();
        MLlibSparkContextManager.setSparkContext(currentContext,
          currentWriterAppender);
      }
      job.runJobWithContext(currentContext);
      m_finalModel = job.getFinalModel();
    } finally {
      Thread.currentThread().setContextClassLoader(orig);
    }
  }

  /**
   * Get a textual description of the classifier
   *
   * @return a textual description of the classifier
   */
  public String toString() {
    if (m_finalModel == null) {
      return "No model built yet";
    }

    return m_finalModel.toString();
  }

  /**
   * Get a predicted class probability distribution for the given test instance
   *
   * @param inst the instance to predict
   * @return the probability distribution over the class values
   * @throws Exception if a problem occurs
   */
  @Override
  public double[] distributionForInstance(Instance inst) throws Exception {
    return m_finalModel.distributionForInstance(inst);
  }

  /**
   * Simple Spark job for building the model
   */
  protected static class SimpleClassifierJob extends SparkJob {

    private static final long serialVersionUID = -3674468690742891451L;
    protected Classifier m_wekaClassifier;

    protected Instances m_trainingData;

    protected String m_sparkJobOpts = "";

    protected boolean m_forceVote;

    protected boolean m_forceBatch;

    public SimpleClassifierJob() {
      super("WekaClassifier", "Builds a Weka Spark model");
    }

    public void setInstances(Instances instances) {
      m_trainingData = instances;
    }

    public void setWekaClassifier(Classifier classifier) {
      m_wekaClassifier = classifier;
    }

    public void setForceVote(boolean forceVote) {
      m_forceVote = forceVote;
    }

    public void setForceBatch(boolean forceBatch) {
      m_forceBatch = forceBatch;
    }

    public Classifier getFinalModel() {
      return m_wekaClassifier;
    }

    /**
     * Used from GUI apps
     *
     * @param jobOpts Options to set on the job
     * @throws Exception if a problem occurs
     */
    public void setSparkJobOptions(String jobOpts) throws Exception {
      m_sparkJobOpts = jobOpts;

      String[] optsToSet = Utils.splitOptions(jobOpts);
      super.setOptions(optsToSet);
    }

    @Override
    public boolean runJobWithContext(JavaSparkContext sparkContext)
      throws IOException, DistributedWekaException {
      WekaClassifierSparkJob wcsj = new WekaClassifierSparkJob();
      try {
        // set job options first (-master etc.)
        // wcsj.setOptions(super.getOptions());
        wcsj.setOptions(Utils.splitOptions(m_sparkJobOpts));
      } catch (Exception e) {
        throw new DistributedWekaException(e);
      }

      int maxPartitions =
        !DistributedJobConfig.isEmpty(m_sjConfig.getMaxInputSlices()) ? Integer
          .parseInt(environmentSubstitute(m_sjConfig.getMaxInputSlices()))
          : (int) Math.round(Math.log10(m_trainingData.numInstances()));
      int minPartions =
        !DistributedJobConfig.isEmpty(m_sjConfig.getMinInputSlices()) ? Integer
          .parseInt(environmentSubstitute(m_sjConfig.getMinInputSlices())) : 1;

      int numPartitions =
        minPartions > maxPartitions ? minPartions : maxPartitions;

      // randomly shuffle (and stratify if num partitions is greater than 0 and
      // class in nominal
      m_trainingData.randomize(m_trainingData.getRandomNumberGenerator(1));
      if (numPartitions > 0 && m_trainingData.classAttribute().isNominal()) {
        m_trainingData.stratify(numPartitions);
      }

      Dataset ds =
        Dataset.instancesToDataset(m_trainingData, sparkContext, numPartitions);

      // don't write out the model to an output directory - we'll retrieve it
      // from the underlying job programatically
      wcsj.setDontWriteClassifierToOutputDir(true);
      // set class name
      wcsj.setClassAttribute(m_trainingData.classAttribute().name());
      // set dataset for underlying job
      wcsj.setDataset(TRAINING_DATA, ds);
      // construct classifier map task options
      String classifierOpts =
        Utils.joinOptions(((OptionHandler) m_wekaClassifier).getOptions());
      String mapTaskOpts =
        "-W " + m_wekaClassifier.getClass().getCanonicalName();
      if (classifierOpts.length() > 0) {
        mapTaskOpts += " -- " + classifierOpts;
      }
      String additional = "";
      if (m_forceVote) {
        additional = "-force-vote ";
      }
      if (m_forceBatch) {
        additional += "-force-batch ";
      }

      wcsj.setClassifierMapTaskOptions(additional + mapTaskOpts);
      boolean result = wcsj.runJobWithContext(sparkContext);

      // Get the final model
      m_wekaClassifier = wcsj.getClassifier();

      return result;
    }
  }
}
