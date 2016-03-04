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
 *    WekaClassifierMapTask.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed;

import distributed.core.DistributedJob;
import distributed.core.DistributedJobConfig;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.UpdateableBatchProcessor;
import weka.classifiers.UpdateableClassifier;
import weka.classifiers.meta.AggregateableFilteredClassifier;
import weka.classifiers.meta.AggregateableFilteredClassifierUpdateable;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.meta.FilteredClassifierUpdateable;
import weka.core.Aggregateable;
import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.MakePreconstructedFilter;
import weka.filters.MultiFilter;
import weka.filters.PreconstructedFilter;
import weka.filters.StreamableFilter;
import weka.filters.unsupervised.instance.ReservoirSample;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.Vector;

/**
 * A map task for building classifiers. Can handle batch and incremental
 * classifiers, which are either Aggregateable or not. Non-aggregateable
 * classifiers are wrapped up in a Vote meta classifeir by the reduce task.
 * Incremental classifiers are trained as instances are presented to the
 * processInstance() method. Batch classifiers are trained when finalizeTask()
 * is called. Instances are collected and held in memory for batch classifiers,
 * although reservior sampling may be used to ensure that a fixed number of
 * instances is used for batch learning. There are options to force batch
 * learning for updateable classifiers and to force the generation of a Vote
 * ensemble for Aggregateable classifiers.
 * <p>
 * 
 * Classifiers may be trained on all the incoming data or on a particular
 * cross-validation fold (this functionality is used directly by the evaluation
 * map and reduce tasks). In the case of batch classifiers, the data for the map
 * will be stratified (if the class is nominal) and randomized before extracting
 * the fold to train on. In the case of incremental classifiers, a modulus
 * operation is used to pull out the instance corresponding to the selected fold
 * from the incoming instance stream.
 * <p>
 * 
 * Classifiers can optionally have their training data passed through one or
 * more filters as a pre-processing step. The class will determine how to wrap
 * the base classifier and filters based on the nature of the filters specified
 * and whether the classifier is batch/incremental and Aggregateable.
 * Aggregateable classifiers (batch or incremental) can only be aggregated to
 * one final model if the filters used with them (if using filters) are all
 * StreamableFilters (i.e. they can determine their output structure immediately
 * without having to see any instances).
 * <p>
 * 
 * It is also possible to specify a special "preconstructed" filter to use in
 * conjunction with, or instead of, regular filters. At present, there is just
 * one Preconstructed filter implemented by the distributed system.
 * PreConstructedPCA can produce a "trained" PCA filter using a correlation
 * matrix produced by the CorrelationMatrixMap/Reduce tasks.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class WekaClassifierMapTask implements OptionHandler,
  EnvironmentHandler, Serializable {

  /**
   * If this property is set then we can adjust the total number of requested
   * iterations for IteratedSingleClassifierEnhancers according to the number of
   * maps that are going to run. This is useful for schemes that build
   * independent base models (e.g. Bagging) in order to get approximately the
   * requested number of models in the end. For boosting methods it will be
   * necessary to set their number of iterations to a higher value than required
   * as it will be adjusted downwards for each map.
   */
  public static final String TOTAL_NUMBER_OF_MAPS = "total.num.maps";

  /** For serialization */
  private static final long serialVersionUID = -5953696466790594368L;

  /** The classifier to use */
  protected Classifier m_classifier = new weka.classifiers.trees.REPTree();

  /** If true then incremental classifiers will be batch trained */
  protected boolean m_forceBatchForUpdateable;

  /**
   * Option value that is determined by whether the classifier is updateable and
   * whether this iteration through the data is > 1
   */
  protected boolean m_continueTrainingUpdateable;

  /**
   * Total folds - only used if m_foldNumber != -1. Use this to train the
   * classifier on a particular fold of the incoming data set for this map
   */
  protected int m_totalFolds = 1; // default = use all data

  /**
   * The fold number to train on. Use in conjunction with m_totalFolds. Default
   * is to train on all the data entering this map
   */
  protected int m_foldNumber = -1; // 1-based. default - use all data

  /** Number of training instances processed by the classifier in this map */
  protected int m_numTrainingInstances;

  /** Total number of instances seen by this map */
  protected int m_numInstances;

  /** Training header */
  protected Instances m_trainingHeader;

  /** Environment variables */
  protected transient Environment m_env = Environment.getSystemWide();

  /** Whether to use reservoir sampling for batch learning */
  protected boolean m_useReservoirSampling;

  /** Reservoir sampling (if requested) for batch learning in this map */
  protected ReservoirSample m_reservoir;

  /** Sample size if reservoir sampling is being used for batch learning */
  protected int m_sampleSize = -1;

  /**
   * True if a Vote ensemble is to be produced in the case when the base
   * classifier is Aggregateable
   */
  protected boolean m_forceVotedEnsemble;

  /**
   * Filters to use. How these are handled depends on whether the base
   * classifier is Aggregateable, incremental etc. These only have an effect if
   * not continueing the training of an updateable classifier - in this case it
   * is assumed that the updatebble classifier would have been configured with
   * these filters when first constructed.
   */
  protected List<Filter> m_filtersToUse = new ArrayList<Filter>();

  /** Random seed for fold generation */
  protected String m_seed = "1";

  public static void main(String[] args) {
    try {
      WekaClassifierMapTask task = new WekaClassifierMapTask();
      if (Utils.getFlag('h', args)) {
        String help = DistributedJob.makeOptionsStr(task);
        System.err.println(help);
        System.exit(1);
      }

      String trainingPath = Utils.getOption("t", args);
      Instances train =
        new Instances(new java.io.BufferedReader(new java.io.FileReader(
          trainingPath)));
      train.setClassIndex(train.numAttributes() - 1);

      task.setOptions(args);
      task.setup(new Instances(train, 0));
      for (int i = 0; i < train.numInstances(); i++) {
        task.processInstance(train.instance(i));
      }
      task.finalizeTask();

      System.err.println("Batch trained classifier:\n"
        + task.getClassifier().toString());

      // now configure for an incremental classifier and
      // train it for two passes over the data
      task = new WekaClassifierMapTask();
      task.setClassifier(new weka.classifiers.bayes.NaiveBayesUpdateable());
      task.setup(new Instances(train, 0));
      for (int i = 0; i < train.numInstances(); i++) {
        task.processInstance(train.instance(i));
      }
      // task.finalizeTask(); // not needed as training is done in
      // processInstance()

      System.err.println("Incremental training (iteration 1):\n"
        + task.getClassifier().toString());

      task.setContinueTrainingUpdateableClassifier(true);
      task.setup(new Instances(train, 0));
      for (int i = 0; i < train.numInstances(); i++) {
        task.processInstance(train.instance(i));
      }
      System.err.println("Incremental training (iteration 2):\n"
        + task.getClassifier().toString());

    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> options = new Vector<Option>();

    options.add(new Option(
      "\tThe fully qualified base classifier to use. Classifier options\n\t"
        + "can be supplied after a '--'", "W", 1, "-W"));
    options.add(new Option(
      "\tForce batch learning for Updateable classifiers.", "force-batch", 1,
      "-force-batch"));
    options.add(new Option(
      "\tForce Vote-based ensemble creation (i.e. ignore\n\t"
        + "a classifier's Aggregateable status and create a final Vote\n\t"
        + "ensemble)", "force-vote", 0, "-force-vote"));
    options.add(new Option("\tUse reservoir sampling for batch learning",
      "use-sampling", 0, "-use-sampling"));
    options
      .add(new Option(
        "\tSpecify a filter to pre-process the data with.\n\t"
          + "For Aggregateable classifiers the filter must be a StreamableFilter,\n\t"
          + "meaning that the output format produced by the filter must be able to\n\t"
          + " be determined directly from the input data format (this makes the data format\n\t"
          + "compatible across map tasks). Many unsupervised attribute-based\n\t"
          + "filters are StreamableFilters. If a Vote ensemble is being produced,\n\t"
          + "this constraint does not apply and any filter may be used.\n\t"
          + "This option may be supplied multiple times in order to apply more\n\t"
          + "than one filter.", "filter", 1, "-filter"));
    options.add(new Option("\tSample size if reservoir sampling is being used",
      "sample-size", 1, "-sample-size <num instances>"));
    options.add(new Option(
      "\tTraining fold to use (default = -1, i.e. use all the data)",
      "fold-number", 1, "-fold-number <fold num>"));
    options.add(new Option("\tTotal number of folds. Use in conjunction with "
      + "-fold-number (default = 1, i.e. use all the data)", "total-folds", 1,
      "-total-folds <num folds>"));
    options.add(new Option("\tRandom seed for fold generation.", "seed", 1,
      "-seed <integer>"));

    return options.elements();
  }

  /**
   * Create an option specification string for a filter
   * 
   * @param f the filter
   * @return the option spec
   */
  protected String getFilterSpec(Filter f) {

    return f.getClass().getName()
      + (f instanceof OptionHandler ? " "
        + Utils.joinOptions(((OptionHandler) f).getOptions()) : "");
  }

  @Override
  public String[] getOptions() {
    List<String> options = new ArrayList<String>();

    options.add("-W");
    options.add(m_classifier.getClass().getName());

    if (getForceBatchLearningForUpdateableClassifiers()) {
      options.add("-force-batch");
    }

    if (getForceVotedEnsembleCreation()) {
      options.add("-force-vote");
    }

    options.add("-fold-number");
    options.add("" + getFoldNumber());

    options.add("-total-folds");
    options.add("" + getTotalNumFolds());

    if (getUseReservoirSamplingWhenBatchLearning()) {
      options.add("-use-sampling");

      options.add("-sample-size");
      options.add("" + getReservoirSampleSize());
    }

    options.add("-seed");
    options.add(getSeed());

    if (m_filtersToUse != null) {
      for (Filter f : m_filtersToUse) {
        options.add("-filter");
        options.add(getFilterSpec(f));
      }
    }

    if (m_classifier instanceof OptionHandler) {
      String[] classifierOpts = ((OptionHandler) m_classifier).getOptions();
      options.add("--");
      for (String o : classifierOpts) {
        options.add(o);
      }
    }

    return options.toArray(new String[options.size()]);
  }

  @Override
  public void setOptions(String[] options) throws Exception {
    String classifier = Utils.getOption("W", options);

    if (!DistributedJobConfig.isEmpty(classifier)) {
      String[] classifierOpts = Utils.partitionOptions(options);
      setClassifier(AbstractClassifier.forName(classifier, classifierOpts));
    }

    setForceBatchLearningForUpdateableClassifiers(Utils.getFlag("force-batch",
      options));

    String foldNum = Utils.getOption("fold-number", options);
    if (!DistributedJobConfig.isEmpty(foldNum)) {
      setFoldNumber(Integer.parseInt(foldNum));
    }

    String totalFolds = Utils.getOption("total-folds", options);
    if (!DistributedJobConfig.isEmpty(totalFolds)) {
      setTotalNumFolds(Integer.parseInt(totalFolds));
    }

    setUseReservoirSamplingWhenBatchLearning(Utils.getFlag("use-sampling",
      options));

    setForceVotedEnsembleCreation(Utils.getFlag("force-vote", options));

    String sampleSize = Utils.getOption("sample-size", options);
    if (!DistributedJobConfig.isEmpty(sampleSize)) {
      setReservoirSampleSize(Integer.parseInt(sampleSize));
    }

    String seed = Utils.getOption("seed", options);
    if (!DistributedJobConfig.isEmpty(seed)) {
      setSeed(seed);
    }

    while (true) {
      String filterString = Utils.getOption("filter", options);
      if (DistributedJobConfig.isEmpty(filterString)) {
        break;
      }

      String[] spec = Utils.splitOptions(filterString);
      if (spec.length == 0) {
        throw new IllegalArgumentException(
          "Invalid filter specification string");
      }
      String filterClass = spec[0];
      Filter f = (Filter) Class.forName(filterClass).newInstance();
      spec[0] = "";
      if (f instanceof OptionHandler) {
        ((OptionHandler) f).setOptions(spec);
      }
      m_filtersToUse.add(f);
    }
  }

  /**
   * Checks to see if the base classifier is an IteratedSingleClassifierEnhancer
   * and whether the environment variable TOTAL_NUMBER_OF_MAPS has been set. If
   * both of these is the case then the user-requested number of iterations
   * (base classifiers) is divided (as close as possible) by the number of maps
   * that will run in order to divide up the work involved in building base
   * classifiers.
   */
  protected void checkForAndConfigureIteratedMetaClassifier() {
    if (m_classifier instanceof weka.classifiers.IteratedSingleClassifierEnhancer
      || m_classifier instanceof weka.classifiers.trees.RandomForest) {

      boolean isRandomForest =
        m_classifier instanceof weka.classifiers.trees.RandomForest;

      // see if we need to adjust the number of iterations on
      // the basis of the number of maps
      String numMaps = m_env.getVariableValue(TOTAL_NUMBER_OF_MAPS);
      if (numMaps != null && numMaps.length() > 0) {
        int nM = Integer.parseInt(numMaps);

        // if (nM > 1) {
        // // Err on the safe side just in case a classifier gets dropped from
        // // the ensemble due to seeing less data than the rest (i.e.
        // // considerable
        // // smaller input split than max split size).
        // nM--;
        // }

        int totalClassifiersRequested =
          ((weka.classifiers.IteratedSingleClassifierEnhancer) m_classifier)
            .getNumIterations();

        int classifiersPerMap = totalClassifiersRequested / nM;
        if (classifiersPerMap < 1) {
          classifiersPerMap = 1;
        }
        ((weka.classifiers.IteratedSingleClassifierEnhancer) m_classifier)
          .setNumIterations(classifiersPerMap);

        System.err.println("[ClassifierMapTask] total maps to be run " + nM
          + " total base classifiers requested " + totalClassifiersRequested
          + " classifiers per map " + classifiersPerMap);
      }
    }
  }

  /**
   * Get the classifier to use
   * 
   * @return the classifier to use
   */
  public Classifier getClassifier() {
    return m_classifier;
  }

  /**
   * Set the classifier to use
   *
   * @param classifier the classifier to use
   */
  public void setClassifier(Classifier classifier) {
    m_classifier = classifier;
  }

  /**
   * The tool tip text for this property.
   *
   * @return the tool tip text for this property
   */
  public String classifierTipText() {
    return "The classifier to use";
  }

  /**
   * Get the seed for randomizing the data when batch learning and for reservoir
   * sampling.
   * 
   * @return the seed to use
   */
  public String getSeed() {
    return m_seed;
  }

  /**
   * Set the seed for randomizing the data when batch learning and for reservoir
   * sampling.
   *
   * @param seed the seed to use
   */
  public void setSeed(String seed) {
    m_seed = seed;
  }

  /**
   * The tool tip text for this property.
   *
   * @return the tool tip text for this property
   */
  public String seedTipText() {
    return "Random seed for shuffling the training data and reservoir sampling "
      + "(batch learning only).";
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
  public void setForceVotedEnsembleCreation(boolean f) {
    m_forceVotedEnsemble = f;
  }

  /**
   * The tool tip text for this property.
   *
   * @return the tool tip text for this property
   */
  public String forceVotedEnsembleCreation() {
    return "Force the creation of a Vote ensemble even if the "
      + "base classifier is directly aggregateable";

  }

  /**
   * Get the filters to wrap up with the base classifier
   *
   * @return the filters to wrap up with the base classifier
   */
  public Filter[] getFiltersToUse() {
    List<Filter> finalList = new ArrayList<Filter>();
    for (Filter f : m_filtersToUse) {
      if (!(f instanceof PreconstructedFilter)) {
        finalList.add(f);
      }
    }

    Filter[] result = new Filter[finalList.size()];
    int count = 0;
    for (Filter f : m_filtersToUse) {
      if (!(f instanceof PreconstructedFilter)) {
        result[count++] = f;
      }
    }

    return result;
  }

  /**
   * Set the filters to wrap up with the base classifier
   *
   * @param toUse filters to wrap up with the base classifier
   */
  public void setFiltersToUse(Filter[] toUse) {
    m_filtersToUse.clear();

    if (toUse != null && toUse.length > 0) {
      for (Filter f : toUse) {
        if (!(f instanceof PreconstructedFilter)) {
          m_filtersToUse.add(f);
        }
      }
    }
  }

  /**
   * The tool tip text for this property.
   *
   * @return the tool tip text for this property
   */
  public String filtersToUseTipText() {
    return "Filters to pre-process the data with before "
      + "passing it to the classifier. Note that in order "
      + "to remain directly aggregateable to a single model "
      + "StreamableFilters must be used with Aggregateable classifiers.";
  }

  /**
   * Add a Preconstructed filter (such as PreConstructedPCA) to use with the
   * classifier. This can be used instead of, or in conjunction with, a set of
   * standard filters.
   *
   * @param f the Preconstructed filter to use.
   */
  public void addPreconstructedFilterToUse(PreconstructedFilter f) {
    // add to the start because these preconstructed filters most
    // likely operate on the raw incoming data
    m_filtersToUse.add(0, (Filter) f);
  }

  /**
   * Get whether to use reservoir sampling when batch learning
   * 
   * @return true if reservoir sampling is to be used
   */
  public boolean getUseReservoirSamplingWhenBatchLearning() {
    return m_useReservoirSampling;
  }

  /**
   * Set whether to use reservoir sampling when batch learning
   *
   * @param r true if reservoir sampling is to be used
   */
  public void setUseReservoirSamplingWhenBatchLearning(boolean r) {
    m_useReservoirSampling = r;
  }

  /**
   * The tool tip text for this property.
   *
   * @return the tool tip text for this property
   */
  public String useReservoirSamplingWhenBatchLearningTipText() {
    return "Apply reservoir sampling to enforce a maximum number "
      + "of training instances when batch learning";
  }

  /**
   * Get the sample size for reservoir sampling
   * 
   * @return the sample size to use for reservoir sampling
   */
  public int getReservoirSampleSize() {
    return m_sampleSize;
  }

  /**
   * Set the sample size for reservoir sampling
   *
   * @param size the sample size to use for reservoir sampling
   */
  public void setReservoirSampleSize(int size) {
    m_sampleSize = size;
  }

  /**
   * The tool tip text for this property.
   *
   * @return the tool tip text for this property
   */
  public String reservoirSampleSizeTipText() {
    return "The sample size (number of instances/rows) for "
      + "reservoir sampling";
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
  public void setForceBatchLearningForUpdateableClassifiers(boolean force) {
    m_forceBatchForUpdateable = force;
  }

  /**
   * The tool tip text for this property.
   *
   * @return the tool tip text for this property
   */
  public String forceBatchLearningForUpdateableClassifiersTipText() {
    return "Use batch training even if the base classifier is an "
      + "incremental (Updateable) one.";
  }

  /**
   * Get whether to continue training an incremental (updateable) classifier.
   * Assumes that the classifier set via setClassifier() has already been
   * trained (or at least initialized via buildClassifier()).
   * 
   * @return true if continuing to train an incremental classifier rather than
   *         starting from scratch
   */
  public boolean getContinueTrainingUpdateableClassifier() {
    return m_continueTrainingUpdateable;
  }

  /**
   * Set whether to continue training an incremental (updateable) classifier.
   * Assumes that the classifier set via setClassifier() has already been
   * trained (or at least initialized via buildClassifier()).
   *
   * @param u true to continue training an incremental classifier rather than
   *          starting from scratch
   */
  public void setContinueTrainingUpdateableClassifier(boolean u) {
    m_continueTrainingUpdateable = u;
  }

  /**
   * The tool tip text for this property.
   *
   * @return the tool tip text for this property
   */
  public String continueTrainingUpdateableClassifierTipText() {
    return "Continue training (updating) an incremental classifier "
      + "with the incoming data (rather than start from scratch)";
  }

  /**
   * Get the fold number to train the classifier with. -1 indicates that all the
   * data is to be used for training.
   * 
   * @return the fold number to train on.
   */
  public int getFoldNumber() {
    return m_foldNumber;
  }

  /**
   * Set the fold number to train the classifier with. -1 indicates that all the
   * data is to be used for training.
   *
   * @param fn the fold number to train on.
   */
  public void setFoldNumber(int fn) {
    m_foldNumber = fn;
  }

  /**
   * The tool tip text for this property.
   *
   * @return the tool tip text for this property
   */
  public String foldNumberTipText() {
    return "Set the fold number to train the classifier with. Default (-1) is "
      + "to use all the data for training the classifier. Use in conjunction "
      + "with setTotalNumberOfFolds";
  }

  /**
   * Get the total number of folds to use. Use in conjunction with
   * setFoldNumber(). Only has an effect if setFoldNumber() is set to something
   * other than -1.
   * 
   * @return the total number of folds
   */
  public int getTotalNumFolds() {
    return m_totalFolds;
  }

  /**
   * Set the total number of folds to use. Use in conjunction with
   * setFoldNumber(). Only has an effect if setFoldNumber() is set to something
   * other than -1.
   *
   * @param t the total number of folds
   */
  public void setTotalNumFolds(int t) {
    m_totalFolds = t;
  }

  /**
   * The tool tip text for this property.
   *
   * @return the tool tip text for this property
   */
  public String totalNumberOfFoldsTipText() {
    return "The total number of folds. Use in conjunction with setFoldNumber(). "
      + "Only has an effect if setFoldNumber() is set to something "
      + "other than -1.";
  }

  /**
   * Get the number of training instances actually used to train the classifier.
   *
   * @return the number of training instances used to train the classifier
   */
  public int getNumTrainingInstances() {
    return m_numTrainingInstances;
  }

  /**
   * Add the supplied instances to the training header
   *
   * @param toAdd the instances to add
   */
  public void addToTrainingHeader(Instances toAdd) {
    for (int i = 0; i < toAdd.numInstances(); i++) {
      m_trainingHeader.add(toAdd.instance(i));
    }
  }

  /**
   * Add the supplied instance to the training header
   *
   * @param toAdd the instance to add
   */
  public void addToTrainingHeader(Instance toAdd) {
    m_trainingHeader.add(toAdd);
  }

  /**
   * Determines if it is necessary to wrap the base classifier in the case where
   * filters are specified. What type of meta classifier to use depends on
   * whether the base classifier is Aggregateable and/or Updateable and the
   * nature of the filters to use
   */
  protected void determineWrapperClassifierToUse(Instances trainingHeader)
    throws DistributedWekaException {
    if (m_continueTrainingUpdateable || m_filtersToUse.size() == 0) {
      return; // nothing to do
    }

    if (m_classifier instanceof Aggregateable && !m_forceVotedEnsemble) {
      // all filters *must* be PreconstructedFilters. Those that are not,
      // and are StreamableFilter, can be wrapped in MakePreconstructedFilter
      Filter baseFilter = null;
      List<Filter> finalList = new ArrayList<Filter>();
      for (Filter f : m_filtersToUse) {
        if (f instanceof PreconstructedFilter) {
          finalList.add(f);
        } else {
          if (!(f instanceof StreamableFilter)) {
            throw new DistributedWekaException(
              "Base classifier is Aggregateable. "
                + "In this case all filters must be StreamableFilters but "
                + f.getClass().getName() + " is not Streamable");
          }

          MakePreconstructedFilter wrapper = new MakePreconstructedFilter(f);
          finalList.add(wrapper);
        }
      }

      if (finalList.size() > 1) {
        MultiFilter mf = new MultiFilter();
        mf.setFilters(finalList.toArray(new Filter[finalList.size()]));
        baseFilter = new MakePreconstructedFilter(mf);
      } else {
        baseFilter = finalList.get(0);
      }

      try {
        baseFilter.setInputFormat(trainingHeader);
      } catch (Exception ex) {
        throw new DistributedWekaException(ex);
      }

      if (m_classifier instanceof UpdateableClassifier
        && !getForceBatchLearningForUpdateableClassifiers()) {
        AggregateableFilteredClassifierUpdateable ag =
          new AggregateableFilteredClassifierUpdateable();
        ag.setClassifier(getClassifier());
        ag.setPreConstructedFilter(baseFilter);
        m_classifier = ag;
      } else {
        AggregateableFilteredClassifier ag =
          new AggregateableFilteredClassifier();
        ag.setClassifier(getClassifier());
        ag.setPreConstructedFilter(baseFilter);
        // we don't use setClassifier() here as this would now screw up
        // the potential configuration done for
        // IteratedSingleClassifierEnhancers with
        // respect to the number of map tasks
        m_classifier = ag;
      }
    } else {
      // not aggreateable, but could be updateable
      FilteredClassifier fc = null;
      if (getClassifier() instanceof UpdateableClassifier
        && !m_forceBatchForUpdateable) {
        // initial run with an updateable classifier. Need to use
        // FilteredClassifierUpdateable

        fc = new FilteredClassifierUpdateable();

        // check that all filters are StreamableFilter
        for (Filter f : m_filtersToUse) {
          if (!(f instanceof StreamableFilter)) {
            throw new DistributedWekaException(
              "Base classifier is Updateable. In this "
                + "case all filters must be StreamableFilters but "
                + f.getClass().getName() + " is not.");
          }
        }
      } else {
        // standard FilteredClassifier case
        fc = new FilteredClassifier();
      }
      fc.setClassifier(getClassifier());
      if (m_filtersToUse.size() > 1) {
        MultiFilter mf = new MultiFilter();
        mf.setFilters(m_filtersToUse.toArray(new Filter[m_filtersToUse.size()]));
        fc.setFilter(mf);
      } else {
        fc.setFilter(m_filtersToUse.get(0));
      }

      m_classifier = fc;
    }
  }

  /**
   * Initialize the map task
   *
   * @param trainingHeader the header of the incoming training data.
   * @throws DistributedWekaException if something goes wrong
   */
  public void setup(Instances trainingHeader) throws DistributedWekaException {
    if (m_classifier == null) {
      throw new DistributedWekaException("No classifier has been configured!");
    }

    checkForAndConfigureIteratedMetaClassifier();

    m_trainingHeader = new Instances(trainingHeader, 0);
    if (m_trainingHeader.classIndex() < 0) {
      throw new DistributedWekaException("No class index set in the data!");
    }

    // See if we need to wrap the base classifier
    determineWrapperClassifierToUse(trainingHeader);

    if (m_classifier instanceof UpdateableClassifier
      && !m_forceBatchForUpdateable && !m_continueTrainingUpdateable) {

      // initialize an incremental classifier
      try {
        m_classifier.buildClassifier(trainingHeader);
      } catch (Exception e) {
        throw new DistributedWekaException(e);
      }
    }

    if (getUseReservoirSamplingWhenBatchLearning()) {
      if (getReservoirSampleSize() <= 0) {
        throw new DistributedWekaException(
          "Reservoir sampling requested, but no sample size set.");
      }
      m_reservoir = new ReservoirSample();
      m_reservoir.setSampleSize(getReservoirSampleSize());
      // random seed for sampling
      int seed = 1;
      if (!DistributedJobConfig.isEmpty(getSeed())) {
        String sSeed = getSeed();
        try {
          sSeed = m_env.substitute(sSeed);
        } catch (Exception ex) {
        }

        try {
          seed = Integer.parseInt(sSeed);
        } catch (NumberFormatException n) {
          System.err.println("Trouble parsing random seed value: " + sSeed);
        }
      }
      m_reservoir.setRandomSeed(seed);
      try {
        m_reservoir.setInputFormat(m_trainingHeader);
      } catch (Exception e) {
        throw new DistributedWekaException(e);
      }
    }

    m_numTrainingInstances = 0;
    m_numInstances = 0;
  }

  /**
   * Process the supplied instance. Incremental classifiers are updated
   * immediately with the instance. In the case of batch learning, the instance
   * is cached in memory and the entire data set is processed in the
   * finalizeTask() method.
   *
   * @param inst the instance to train with
   * @throws DistributedWekaException if a problem occurs
   */
  public void processInstance(Instance inst) throws DistributedWekaException {

    if (m_classifier instanceof UpdateableClassifier
      && !m_forceBatchForUpdateable) {
      boolean ok = true;
      if (m_totalFolds > 1 && m_foldNumber >= 1) {
        int fn = m_numInstances % m_totalFolds;

        // is this instance in our test fold?
        if (fn == (m_foldNumber - 1)) {
          ok = false;
        }
      }

      if (ok) {
        try {
          ((UpdateableClassifier) m_classifier).updateClassifier(inst);
        } catch (Exception e) {
          throw new DistributedWekaException(e);
        }
        m_numTrainingInstances++;
      }
    } else {
      // store the instance
      if (m_reservoir != null) {
        m_reservoir.input(inst);
      } else {
        m_trainingHeader.add(inst);
      }
    }

    m_numInstances++;
  }

  /**
   * Finish up the map task. If the classifier is being batch trained then this
   * is where the action happens.
   *
   * @throws DistributedWekaException if something goes wrong
   */
  public void finalizeTask() throws DistributedWekaException {

    // System.gc();
    Runtime currR = Runtime.getRuntime();
    long freeM = currR.freeMemory();
    long totalM = currR.totalMemory();
    long maxM = currR.maxMemory();
    System.err
      .println("[ClassifierMapTask] Memory (free/total/max.) in bytes: "
        + String.format("%,d", freeM) + " / " + String.format("%,d", totalM)
        + " / " + String.format("%,d", maxM));

    if (m_classifier instanceof UpdateableClassifier
      && !m_forceBatchForUpdateable) {

      if (m_classifier instanceof UpdateableBatchProcessor) {
        try {
          System.err
            .println("Calling batch finished on updateable classifier...");
          ((UpdateableBatchProcessor) m_classifier).batchFinished();
        } catch (Exception ex) {
          throw new DistributedWekaException(ex);
        }
      }

      // nothing else to do
      return;
    }

    if (m_reservoir != null) {
      m_reservoir.batchFinished();

      while (m_reservoir.numPendingOutput() > 0) {
        m_trainingHeader.add(m_reservoir.output());
      }
    }

    m_trainingHeader.compactify();

    // randomize the order of the instances
    long seed = 1;
    if (!DistributedJobConfig.isEmpty(getSeed())) {
      String sSeed = getSeed();
      try {
        sSeed = m_env.substitute(sSeed);
      } catch (Exception ex) {
      }

      try {
        seed = Long.parseLong(sSeed);
      } catch (NumberFormatException n) {
        System.err.println("Trouble parsing random seed value: " + sSeed);
      }
    }
    Random r = new Random(seed);
    m_trainingHeader.randomize(r);
    if (m_trainingHeader.classAttribute().isNominal() && m_totalFolds > 1) {
      m_trainingHeader.stratify(m_totalFolds);
    }
    Instances train = m_trainingHeader;

    if (m_totalFolds > 1 && m_foldNumber >= 1) {
      train = train.trainCV(m_totalFolds, m_foldNumber - 1, r);
    }
    m_numTrainingInstances = train.numInstances();

    // build the classifier
    try {
      m_classifier.buildClassifier(train);
    } catch (Exception e) {
      throw new DistributedWekaException(e);
    }
  }

  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
  }
}
