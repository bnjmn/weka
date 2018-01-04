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
 *    ScikitLearnClusterer.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.clusterers;

import java.util.List;

import weka.core.BatchPredictor;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionMetadata;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.WekaException;
import weka.filters.Filter;
import weka.python.PythonSession;

/**
 * Wrapper clusterer fro clusterers implemented in the scikit-learn Python
 * package.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class ScikitLearnClusterer extends AbstractClusterer implements
  BatchPredictor {

  protected static final String TRAINING_DATA_ID = "scikit_clusterer_training";
  protected static final String TEST_DATA_ID = "scikit_clusterer_test";
  protected static final String MODEL_ID = "weka_scikit_clusterer";

  /** For serialization */
  private static final long serialVersionUID = -1292576437716874848L;

  /** Holds info on the different learners available */
  public static enum Clusterer {

    AffinityPropagation("cluster",
      "\taffinity='euclidean', convergence_iter=15, copy=True,\n"
        + "\tdamping=0.5, max_iter=200, preference=None, verbose=False", true),
    AgglomerativeClustering("cluster",
      "\taffinity='euclidean', compute_full_tree='auto',\n"
        + "\tconnectivity=None, linkage='ward',\n"
        + "\tmemory=Memory(cachedir=None), n_clusters=2, n_components=None,\n"
        + "\tpooling_func=<function mean at 0x10c4dc6e0>", false),
    Birch("cluster",
      "\tbranching_factor=50, compute_labels=True, copy=True, n_clusters=3,\n"
        + "\tthreshold=0.5", true),
    DBSCAN("cluster",
      "\talgorithm='auto', eps=0.5, leaf_size=30, metric='euclidean',\n"
        + "\tmin_samples=5, p=None, random_state=None", false),
    KMeans(
      "\tcluster",
      "copy_x=True, "
        + "init='k-means++', max_iter=300, n_clusters=8, n_init=10,\n"
        + "\tn_jobs=1, precompute_distances='auto', random_state=None, tol=0.0001,\n"
        + "\tverbose=0", true),
    MiniBatchKMeans(
      "cluster",
      "\tbatch_size=100, compute_labels=True, init='k-means++',\n"
        + "\tinit_size=None, max_iter=100, max_no_improvement=10, n_clusters=8,\n"
        + "\tn_init=3, random_state=None, reassignment_ratio=0.01, tol=0.0,\n"
        + "\tverbose=0", true),
    MeanShift("cluster",
      "\tbandwidth=None, bin_seeding=False, cluster_all=True, min_bin_freq=1,\n"
        + "\tseeds=None", true),
    SpectralClustering(
      "cluster",
      "\taffinity='rbf', assign_labels='kmeans', coef0=1, degree=3,\n"
        + "\teigen_solver=None, eigen_tol=0.0, gamma=1.0, kernel_params=None,\n"
        + "\tn_clusters=8, n_init=10, n_neighbors=10, random_state=None", false),
    Ward("cluster", "\tcompute_full_tree='auto', connectivity=None,\n"
      + "\tmemory=Memory(cachedir=None), n_clusters=2, n_components=None,\n"
      + "\tpooling_func=<function mean at 0x10130d6e0>", false);

    private String m_defaultParameters;
    private String m_module;
    private boolean m_canClusterNewData;

    Clusterer(String module, String defaultParams, boolean canClusterNewData) {
      m_module = module;
      m_defaultParameters = defaultParams;
      m_canClusterNewData = canClusterNewData;
    }

    public String getModule() {
      return m_module;
    }

    public String getDefaultParameters() {
      return m_defaultParameters;
    }

    public boolean canClusterNewData() {
      return m_canClusterNewData;
    }
  }

  /** The tags for the GUI drop-down for learner selection */
  public static final Tag[] TAGS_LEARNER = new Tag[Clusterer.values().length];

  static {
    for (Clusterer l : Clusterer.values()) {
      TAGS_LEARNER[l.ordinal()] = new Tag(l.ordinal(), l.toString());
    }
  }

  /** The scikit clusterer to use */
  protected Clusterer m_clusterer = Clusterer.KMeans;

  /** The parameters to pass to the clusterer */
  protected String m_learnerOpts = "";

  /** The nominal to binary filter to use */
  protected Filter m_nominalToBinary =
    new weka.filters.unsupervised.attribute.NominalToBinary();

  /** For replacing missing values */
  protected Filter m_replaceMissing =
    new weka.filters.unsupervised.attribute.ReplaceMissingValues();

  /** Holds the python serialized model */
  protected String m_pickledModel;

  /** Holds the textual description of the clusterer */
  protected String m_learnerToString = "";

  /** For making this model unique in python */
  protected String m_modelHash;

  /**
   * Batch prediction size (default: 100 instances)
   */
  protected String m_batchPredictSize = "100";

  /**
   * Whether to try and continue after script execution reports output on system
   * error from Python.
   */
  protected boolean m_continueOnSysErr;

  /**
   * Will hold the number of clusters learned by the underlying scikit-learn
   * clusterer
   */
  protected int m_numberOfClustersLearned = -1;

  /**
   * If the underlying clusterer can't predict a new dataset then this will hold
   * the predictions on the training data, which is the only thing that can be
   * returned from distributionsForInstances() in this case
   */
  protected double[][] m_trainingPreds;

  /** For mapping cluster number/label to zero-based indices */
  protected int m_minClusterNum = 0;

  /**
   * Global help info
   *
   * @return the global help info for this scheme
   */
  public String globalInfo() {
    StringBuilder b = new StringBuilder();
    b.append("A wrapper for clusterers implemented in the scikit-learn "
      + "python library. The following learners are available:\n\n");
    for (Clusterer l : Clusterer.values()) {
      b.append(l.toString()).append("\n");
      b.append("\nDefault parameters:\n");
      b.append(l.getDefaultParameters()).append("\n");
    }
    return b.toString();
  }

  /**
   * Get the capabilities of this learner
   *
   * @return the capabilities of this scheme
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    boolean pythonAvailable = true;
    if (!PythonSession.pythonAvailable()) {
      // try initializing
      try {
        if (!PythonSession.initSession("python", getDebug())) {
          pythonAvailable = false;
        }
      } catch (WekaException ex) {
        pythonAvailable = false;
      }
    }

    if (pythonAvailable) {
      result.enable(Capabilities.Capability.NO_CLASS);

      // attributes
      result.enable(Capabilities.Capability.NOMINAL_ATTRIBUTES);
      result.enable(Capabilities.Capability.NUMERIC_ATTRIBUTES);
      result.enable(Capabilities.Capability.MISSING_VALUES);
    } else {
      System.err.println("The python environment is either not available or "
        + "is not configured correctly:\n\n"
        + PythonSession.getPythonEnvCheckResults());
    }

    return result;
  }

  @OptionMetadata(displayName = "Scikit-learn clusterer",
    description = "Scikit-learn clusterer to use.\nAvailable clusterers:\n"
      + "AffinityPropagation, KMeans, DBSCAN",
    commandLineParamName = "clusterer",
    commandLineParamSynopsis = "-clusterer <clusterer name>", displayOrder = 1)
  public SelectedTag getClusterer() {
    return new SelectedTag(m_clusterer.ordinal(), TAGS_LEARNER);
  }

  /**
   * Set the scikit-learn clustering scheme to use
   *
   * @param learner the scikit-learn clustering scheme to use
   */
  public void setClusterer(SelectedTag learner) {
    int learnerID = learner.getSelectedTag().getID();
    for (Clusterer c : Clusterer.values()) {
      if (c.ordinal() == learnerID) {
        m_clusterer = c;
        break;
      }
    }
  }

  /**
   * Get the parameters to pass to the scikit-learn scheme
   *
   * @return the parameters to use
   */
  @OptionMetadata(
    displayName = "Learner parameters",
    description = "learner parameters to use",
    displayOrder = 2,
    commandLineParamName = "parameters",
    commandLineParamSynopsis = "-parameters <comma-separated list of name=value pairs>")
  public
    String getLearnerOpts() {
    return m_learnerOpts;
  }

  /**
   * Set the parameters to pass to the scikit-learn scheme
   *
   * @param opts the parameters to use
   */
  public void setLearnerOpts(String opts) {
    m_learnerOpts = opts;
  }

  @Override
  public void setBatchSize(String size) {
    m_batchPredictSize = size;
  }

  @OptionMetadata(
    displayName = "Batch size",
    description = "The preferred "
      + "number of instances to transfer into python for prediction\n(if operating"
      + "in batch prediction mode). More or fewer instances than this will be "
      + "accepted.", commandLineParamName = "batch",
    commandLineParamSynopsis = "-batch <batch size>", displayOrder = 4)
  @Override
  public
    String getBatchSize() {
    return m_batchPredictSize;
  }

  /**
   * Return true as we send entire test sets over to python for prediction
   *
   * @return true
   */
  @Override
  public boolean implementsMoreEfficientBatchPrediction() {
    return true;
  }

  /**
   * Set whether to try and continue after seeing output on the sys error
   * stream.
   *
   * @param c true if we should try to continue after seeing output on the sys
   *          error stream
   */
  public void setContinueOnSysErr(boolean c) {
    m_continueOnSysErr = c;
  }

  /**
   * Get whether to try and continue after seeing output on the sys error
   * stream.
   *
   * @return true if we should try to continue after seeing output on the sys
   *         error stream
   */
  @OptionMetadata(
    displayName = "Try to continue after sys err output from script",
    description = "Try to continue after sys err output from script.\nSome schemes"
      + "report warnings to the system error stream.", displayOrder = 5,
    commandLineParamName = "continue-on-err",
    commandLineParamSynopsis = "-continue-on-err",
    commandLineParamIsFlag = true)
  public
    boolean getContinueOnSysErr() {
    return m_continueOnSysErr;
  }

  /**
   * Build the clusterer
   *
   * @param data set of instances serving as training data
   * @throws Exception if a problem occurs
   */
  @SuppressWarnings("unchecked")
  @Override
  public void buildClusterer(Instances data) throws Exception {
    getCapabilities().testWithFail(data);

    if (!PythonSession.pythonAvailable()) {
      // try initializing
      if (!PythonSession.initSession("python", getDebug())) {
        String envEvalResults = PythonSession.getPythonEnvCheckResults();
        throw new WekaException("Was unable to start python environment: "
          + envEvalResults);
      }
    }

    if (m_modelHash == null) {
      m_modelHash = "" + hashCode();
    }

    data = new Instances(data);

    m_replaceMissing.setInputFormat(data);
    data = Filter.useFilter(data, m_replaceMissing);
    m_nominalToBinary.setInputFormat(data);
    data = Filter.useFilter(data, m_nominalToBinary);

    try {
      PythonSession session = PythonSession.acquireSession(this);

      session
        .instancesToPythonAsScikitLearn(data, TRAINING_DATA_ID, getDebug());

      // learn the clusterer and predict the training data + get the
      // set of unique clusters (so that we know how many clusters were
      // learned)
      StringBuilder learnScript = new StringBuilder();
      learnScript.append("from sklearn import *").append("\n");
      learnScript.append(
        MODEL_ID + m_modelHash + " = " + m_clusterer.getModule() + "."
          + m_clusterer.toString() + "("
          + (getLearnerOpts().length() > 0 ? getLearnerOpts() : "") + ")")
        .append("\n");
      learnScript
        .append("preds = " + MODEL_ID + m_modelHash + ".fit_predict(X)\n")
        .append("preds = preds.tolist()\n").append("\np_set = set(preds)\n")
        .append("unique_clusters = list(p_set)\n");

      List<String> outAndErr =
        session.executeScript(learnScript.toString(), getDebug());
      if (outAndErr.size() == 2 && outAndErr.get(1).length() > 0) {
        if (m_continueOnSysErr) {
          System.err.println(outAndErr.get(1));
        } else {
          throw new WekaException(outAndErr.get(1));
        }
      }

      m_learnerToString =
        session.getVariableValueFromPythonAsPlainString(MODEL_ID + m_modelHash,
          getDebug()) + "\n\n";

      // retrieve the model from python
      m_pickledModel =
        session.getVariableValueFromPythonAsPickledObject(MODEL_ID
          + m_modelHash, getDebug());

      // obtain the number of clusters found
      List<Object> uniqueClusters =
        (List<Object>) session.getVariableValueFromPythonAsJson(
          "unique_clusters", getDebug());
      m_minClusterNum = Integer.MAX_VALUE;
      for (Object o : uniqueClusters) {
        if (((Number) o).intValue() < m_minClusterNum) {
          m_minClusterNum = ((Number) o).intValue();
        }
      }

      if (uniqueClusters == null) {
        throw new Exception(
          "Unable to determine the number of clusters learned!");
      }

      m_numberOfClustersLearned = uniqueClusters.size();
      if (!m_clusterer.canClusterNewData()) {
        // get the predictions for the training data
        List<Object> trainingPreds =
          (List<Object>) session.getVariableValueFromPythonAsJson("preds",
            getDebug());
        if (trainingPreds == null) {
          throw new WekaException(
            "Was unable to get predictions for the training " + "data");
        }
        if (trainingPreds.size() != data.numInstances()) {
          throw new WekaException(
            "The number of predictions obtained does not "
              + "match the number of training instances!");
        }

        m_trainingPreds =
          new double[data.numInstances()][m_numberOfClustersLearned];
        int j = 0;
        for (Object o : trainingPreds) {
          Number p = (Number) o;
          m_trainingPreds[j++][p.intValue() - m_minClusterNum] = 1;
        }
      }
    } finally {
      // release the session
      PythonSession.releaseSession(this);
    }
  }

  @Override
  public int numberOfClusters() throws Exception {
    return m_numberOfClustersLearned;
  }

  /**
   * Return the predicted cluster memberships for the supplied instances
   *
   * @param insts the instances to get predictions for
   * @return the predicted cluster memberships for the supplied instances
   * @throws Exception if a problem occurs
   */
  @SuppressWarnings("unchecked")
  @Override
  public double[][] distributionsForInstances(Instances insts) throws Exception {

    if (m_trainingPreds != null) {
      // TODO clusterer can't produce predictions for test data...
      if (insts.numInstances() != m_trainingPreds.length) {
        throw new WekaException(
          "This scikit-learn clusterer cannot produce predictions for "
            + "new data. We can only return predictions that were stored for the "
            + "training data (and the supplied test set does not seem to match "
            + "the training data)");
      }
      return m_trainingPreds;
    }

    if (!PythonSession.pythonAvailable()) {
      // try initializing
      if (!PythonSession.initSession("python", getDebug())) {
        String envEvalResults = PythonSession.getPythonEnvCheckResults();
        throw new Exception("Was unable to start python environment: "
          + envEvalResults);
      }
    }
    insts = Filter.useFilter(insts, m_replaceMissing);
    insts = Filter.useFilter(insts, m_nominalToBinary);

    try {
      PythonSession session = PythonSession.acquireSession(this);
      session.instancesToPythonAsScikitLearn(insts, TEST_DATA_ID, getDebug());
      StringBuilder predictScript = new StringBuilder();

      // check if model exists in python. If not, then transfer it over
      if (!session.checkIfPythonVariableIsSet(MODEL_ID + m_modelHash,
        getDebug())) {
        if (m_pickledModel == null || m_pickledModel.length() == 0) {
          throw new Exception("There is no model to transfer into Python!");
        }
        session.setPythonPickledVariableValue(MODEL_ID + m_modelHash,
          m_pickledModel, getDebug());
      }

      predictScript.append("from sklearn import *").append("\n");
      predictScript.append("preds = " + MODEL_ID + m_modelHash + ".predict(X)")
        .append("\npreds = preds.tolist()\n");

      List<String> outAndErr =
        session.executeScript(predictScript.toString(), getDebug());
      if (outAndErr.size() == 2 && outAndErr.get(1).length() > 0) {
        if (m_continueOnSysErr) {
          System.err.println(outAndErr.get(1));
        } else {
          throw new Exception(outAndErr.get(1));
        }
      }

      List<Object> preds =
        (List<Object>) session.getVariableValueFromPythonAsJson("preds",
          getDebug());
      if (preds == null) {
        throw new Exception("Was unable to retrieve predictions from python");
      }

      if (preds.size() != insts.numInstances()) {
        throw new Exception(
          "Learner did not return as many predictions as there "
            + "are test instances");
      }

      double[][] dists =
        new double[insts.numInstances()][m_numberOfClustersLearned];
      int j = 0;
      for (Object o : preds) {
        Number p = (Number) o;
        dists[j++][p.intValue() - m_minClusterNum] = 1;
      }
      return dists;
    } finally {
      PythonSession.releaseSession(this);
    }
  }

  /**
   * Return the predicted cluster memberships for the supplied instance
   *
   * @param inst the instance to be classified
   * @return the predicted cluster memberships
   * @throws Exception if a problem occurs
   */
  @Override
  public double[] distributionForInstance(Instance inst) throws Exception {
    if (m_trainingPreds != null) {
      throw new WekaException(
        "distributionForInstance() can only be used with "
          + "scikit-learn clusterers that support predicting new data");
    }

    Instances temp = new Instances(inst.dataset(), 0);
    temp.add(inst);

    return distributionsForInstances(temp)[0];
  }

  /**
   * Get a textual description of this scheme
   *
   * @return a textual description of this scheme
   */
  public String toString() {
    if (m_learnerToString == null || m_learnerToString.length() == 0) {
      return "ScikitLearnClusterer: model not built yet!";
    }

    return m_learnerToString;
  }

  /**
   * Main method for testing this class
   *
   * @param args command line args
   */
  public static void main(String[] args) {
    runClusterer(new ScikitLearnClusterer(), args);
  }
}
