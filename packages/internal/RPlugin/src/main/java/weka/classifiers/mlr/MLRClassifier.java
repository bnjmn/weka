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
 *    MLRClassifier.java
 *    Copyright (C) 2012-2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.mlr;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.core.BatchPredictor;
import weka.core.Capabilities;
import weka.core.CapabilitiesHandler;
import weka.core.CommandlineRunnable;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.JRILoader;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.Tag;

/**
 * Wrapper classifier for the MLRClassifier package. This class delegates (via
 * reflection) to MLRClassifierImpl. MLRClassifierImpl uses REngine/JRI classes
 * so has to be injected into the root class loader.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class MLRClassifier extends AbstractClassifier implements OptionHandler,
  CapabilitiesHandler, BatchPredictor, RevisionHandler, CommandlineRunnable,
  Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -5715911392187197733L;

  static {
    try {
      JRILoader.load();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  // Classification
  public static final int R_CLASSIF_ADA = 0;
  public static final int R_CLASSIF_BOOSTING = 1;
  public static final int R_CLASSIF_BLACKBOOST = 2;
  public static final int R_CLASSIF_CFOREST = 3; // new: party, cforest
  public static final int R_CLASSIF_CTREE = 4;
  public static final int R_CLASSIF_FNN = 5;
  public static final int R_CLASSIF_GBM = 6;
  public static final int R_CLASSIF_GEODA = 7; // new: DiscriMiner,geoDA
  public static final int R_CLASSIF_GLMBOOST = 8;
  public static final int R_CLASSIF_GLMNET = 9; // new: glmnet,glmnet
  public static final int R_CLASSIF_KKNN = 10;
  public static final int R_CLASSIF_KSVM = 11;
  public static final int R_CLASSIF_LDA = 12;
  public static final int R_CLASSIF_LINDA = 13; // new: DiscriMiner,linDA
  public static final int R_CLASSIF_LOGREG = 14;
  public static final int R_CLASSIF_LSSVM = 15;
  public static final int R_CLASSIF_LVQ1 = 16;
  public static final int R_CLASSIF_MDA = 17;
  public static final int R_CLASSIF_MULTINOM = 18;
  public static final int R_CLASSIF_NAIVE_BAYES = 19;
  public static final int R_CLASSIF_NNET = 20;
  public static final int R_CLASSIF_PLR = 21; // new: stepPlr,plr
  public static final int R_CLASSIF_PLSDA = 22; // new: DiscriMiner,plsDA
  public static final int R_CLASSIF_PLSDACARET = 23; // new: caret, plsda
  public static final int R_CLASSIF_QDA = 24;
  public static final int R_CLASSIF_QUADA = 25; // new: DiscriMiner, quaDA
  public static final int R_CLASSIF_RANDOM_FOREST = 26;
  public static final int R_CLASSIF_RDA = 27;
  public static final int R_CLASSIF_RPART = 28;
  public static final int R_CLASSIF_SVM = 29;

  // public static final int R_CLASSIF_LOCLDA = 6;

  // Regression
  public static final int R_REGR_BLACKBOOST = 30;
  public static final int R_REGR_CFOREST = 31; // new: party, cforest
  public static final int R_REGR_CRS = 32; // new: crs, crs
  public static final int R_REGR_EARTH = 33;
  public static final int R_REGR_FNN = 34;
  public static final int R_REGR_GBM = 35;
  public static final int R_REGR_GLMNET = 36; // new: glmnet, glmnet
  public static final int R_REGR_KKNN = 37;
  public static final int R_REGR_KM = 38;
  public static final int R_REGR_KSVM = 39;
  public static final int R_REGR_LASSO = 40;
  public static final int R_REGR_LM = 41;
  public static final int R_REGR_MARS = 42;
  public static final int R_REGR_MOB = 43; // new: party, mob
  public static final int R_REGR_NNET = 44;
  public static final int R_REGR_PCR = 45; // new: pls, pcr
  public static final int R_REGR_RANDOM_FOREST = 46;
  public static final int R_REGR_RIDGE = 47;
  public static final int R_REGR_RPART = 48;
  public static final int R_REGR_RSM = 49;
  public static final int R_REGR_RVM = 50;
  public static final int R_REGR_SVM = 51; // new: e1071, svm


  /** Tags for the various types of learner */
  public static final Tag[] TAGS_LEARNER = {
    new Tag(R_CLASSIF_ADA, "ada", "classif.ada", false),
    new Tag(R_CLASSIF_BOOSTING, "adabag", "classif.boosting", false),
    new Tag(R_CLASSIF_BLACKBOOST, "a.mboost", "classif.blackboost", false),
    new Tag(R_CLASSIF_CFOREST, "a.party", "classif.cforest", false),
    new Tag(R_CLASSIF_CTREE, "b.party", "classif.ctree", false),
    new Tag(R_CLASSIF_FNN, "a.FNN", "classif.fnn", false),
    new Tag(R_CLASSIF_GBM, "a.gbm", "classif.gbm", false),
    new Tag(R_CLASSIF_GEODA, "a.DiscriMiner", "classif.geoDA", false),
    new Tag(R_CLASSIF_GLMBOOST, "b.mboost", "classif.glmboost", false),
    new Tag(R_CLASSIF_GLMNET, "a.glmnet", "classif.glmnet", false),
    new Tag(R_CLASSIF_KKNN, "a.kknn", "classif.kknn", false),
    new Tag(R_CLASSIF_KSVM, "a.kernlab", "classif.ksvm", false),
    new Tag(R_CLASSIF_LDA, "a.MASS", "classif.lda", false),
    new Tag(R_CLASSIF_LINDA, "b.DiscriMiner", "classif.linDA", false),
    new Tag(R_CLASSIF_LOGREG, "a.stats", "classif.logreg", false),
    new Tag(R_CLASSIF_LSSVM, "b.kernlab", "classif.lssvm", false),
    new Tag(R_CLASSIF_LVQ1, "class", "classif.lvq1", false),
    new Tag(R_CLASSIF_MDA, "a.mda", "classif.mda", false),
    new Tag(R_CLASSIF_MULTINOM, "a.nnet", "classif.multinom", false),
    new Tag(R_CLASSIF_NAIVE_BAYES, "a.e1071", "classif.naiveBayes", false),
    new Tag(R_CLASSIF_NNET, "b.nnet", "classif.nnet", false),
    new Tag(R_CLASSIF_PLR, "stepPlr", "classif.plr", false),
    new Tag(R_CLASSIF_PLSDA, "c.DiscriMiner", "classif.plsDA", false),
    new Tag(R_CLASSIF_PLSDACARET, "caret,pls", "classif.plsdaCaret", false),
    new Tag(R_CLASSIF_QDA, "b.MASS", "classif.qda", false),
    new Tag(R_CLASSIF_QUADA, "d.DiscriMiner", "classif.quaDA", false),
    new Tag(R_CLASSIF_RANDOM_FOREST, "a.randomForest", "classif.randomForest",
      false),
    new Tag(R_CLASSIF_RDA, "b.klaR", "classif.rda", false),
    new Tag(R_CLASSIF_RPART, "a.rpart", "classif.rpart", false),
    new Tag(R_CLASSIF_SVM, "b.e1071", "classif.svm", false),

    // ` new Tag(R_CLASSIF_LOCLDA, "a.klaR", "classif.loclda", false),

    new Tag(R_REGR_BLACKBOOST, "c.mboost", "regr.blackboost", false),
    new Tag(R_REGR_CFOREST, "c.party", "regr.cforest", false),
    new Tag(R_REGR_CRS, "crs", "regr.crs", false),
    new Tag(R_REGR_EARTH, "earth", "regr.earth", false),
    new Tag(R_REGR_FNN, "b.FNN", "regr.fnn", false),
    new Tag(R_REGR_GBM, "b.gbm", "regr.gbm", false),
    new Tag(R_REGR_GLMNET, "b.glmnet", "regr.glmnet", false),
    new Tag(R_REGR_KKNN, "b.kknn", "regr.kknn", false),
    new Tag(R_REGR_KM, "DiceKriging", "regr.km", false),
    new Tag(R_REGR_KSVM, "c.kernlab", "regr.ksvm", false),
    new Tag(R_REGR_LASSO, "a.penalized", "regr.penalized.lasso", false),
    new Tag(R_REGR_LM, "b.stats", "regr.lm", false),
    new Tag(R_REGR_MARS, "b.mda", "regr.mars", false),
    new Tag(R_REGR_MOB, "d.party", "regr.mob", false),
    new Tag(R_REGR_NNET, "c.nnet", "regr.nnet", false),
    new Tag(R_REGR_PCR, "pls", "regr.pcr", false),
    new Tag(R_REGR_RANDOM_FOREST, "b.randomForest", "regr.randomForest", false),
    new Tag(R_REGR_RIDGE, "b.penalized", "regr.penalized.ridge", false),
    new Tag(R_REGR_RPART, "b.rpart", "regr.rpart", false),
    new Tag(R_REGR_RSM, "a.rsm", "regr.rsm", false),
    new Tag(R_REGR_RVM, "d.kernlab", "regr.rvm", false), 
    new Tag(R_REGR_SVM, "c.e1071", "regr.svm", false) };

  protected static final String IMPL = "weka.classifiers.mlr.impl.MLRClassifierImpl";

  protected Object m_delegate;

  protected void init() {
    try {
      m_delegate = Class.forName(IMPL).newInstance();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Global info for this wrapper classifier.
   * 
   * @return the global info suitable for displaying in the GUI.
   */
  public String globalInfo() {
    return "Classifier that wraps the MLR R library for building "
      + "and making predictions with various R classifiers";
  }

  /**
   * Returns default capabilities of the classifier.
   * 
   * @return the capabilities of this classifier
   */
  @Override
  public Capabilities getCapabilities() {
    if (m_delegate == null) {
      init();
    }
    return ((CapabilitiesHandler) m_delegate).getCapabilities();
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {
    if (m_delegate == null) {
      init();
    }
    return ((OptionHandler) m_delegate).listOptions();
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   * <!-- options-start --> <!-- options-end -->
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    if (m_delegate == null) {
      init();
    }
    ((OptionHandler) m_delegate).setOptions(options);
  }

  /**
   * Gets the current settings of MLRClassifier.
   * 
   * @return an array of strings suitable for passing to setOptions()
   */
  @Override
  public String[] getOptions() {
    if (m_delegate == null) {
      init();
    }

    return ((OptionHandler) m_delegate).getOptions();
  }

  public void setLaunchedFromCommandLine(boolean l) {
    if (m_delegate == null) {
      init();
    }

    try {
      Method m = m_delegate.getClass().getDeclaredMethod(
        "setLaunchedFromCommandLine", new Class[] { Boolean.class });

      m.invoke(m_delegate, new Object[] { new Boolean(l) });
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  @Override
  public String debugTipText() {
    if (m_delegate == null) {
      init();
    }
    try {
      Method m = m_delegate.getClass().getDeclaredMethod("debugTipText",
        new Class[] {});

      Object result = m.invoke(m_delegate, new Object[] {});
      return result.toString();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return "";
  }

  /**
   * Set whether to output debugging info
   * 
   * @param d true if debugging info is to be output
   */
  @Override
  public void setDebug(boolean d) {
    if (m_delegate == null) {
      init();
    }
    try {
      Method m = m_delegate.getClass().getDeclaredMethod("setDebug",
        new Class[] { Boolean.TYPE });

      m.invoke(m_delegate, new Object[] { new Boolean(d) });
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Get whether to output debugging info
   * 
   * @return true if debugging info is to be output
   */
  @Override
  public boolean getDebug() {
    if (m_delegate == null) {
      init();
    }
    try {
      Method m = m_delegate.getClass().getDeclaredMethod("getDebug",
        new Class[] {});

      Object result = m.invoke(m_delegate, new Object[] {});
      return ((Boolean) result).booleanValue();
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return false;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String batchSizeTipText() {
    if (m_delegate == null) {
      init();
    }
    try {
      Method m = m_delegate.getClass().getDeclaredMethod("batchSizeTipText",
        new Class[] {});

      Object result = m.invoke(m_delegate, new Object[] {});
      return result.toString();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return "";
  }

  /**
   * Get the batch size for prediction (i.e. how many instances to push over
   * into an R data frame at a time).
   * 
   * @return the batch size for prediction
   */
  @Override
  public String getBatchSize() {
    if (m_delegate == null) {
      init();
    }
    try {
      Method m = m_delegate.getClass().getDeclaredMethod("getBatchSize",
        new Class[] {});

      Object result = m.invoke(m_delegate, new Object[] {});
      return result.toString();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return "";
  }

  /**
   * Set the batch size for prediction (i.e. how many instances to push over
   * into an R data frame at a time).
   * 
   * @return the batch size for prediction
   */
  @Override
  public void setBatchSize(String size) {
    if (m_delegate == null) {
      init();
    }
    try {
      Method m = m_delegate.getClass().getDeclaredMethod("setBatchSize",
        new Class[] { String.class });

      m.invoke(m_delegate, new Object[] { size });
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String RLearnerTipText() {
    if (m_delegate == null) {
      init();
    }
    try {
      Method m = m_delegate.getClass().getDeclaredMethod("RLearnerTipText",
        new Class[] {});

      Object result = m.invoke(m_delegate, new Object[] {});
      return result.toString();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return "";
  }

  /**
   * Set the base R learner to use
   * 
   * @param learner the learner to use
   */
  public void setRLearner(SelectedTag learner) {
    if (m_delegate == null) {
      init();
    }
    try {
      Method m = m_delegate.getClass().getDeclaredMethod("setRLearner",
        new Class[] { SelectedTag.class });

      m.invoke(m_delegate, new Object[] { learner });
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Get the base R learner to use
   * 
   * @return the learner to use
   */
  public SelectedTag getRLearner() {
    if (m_delegate == null) {
      init();
    }
    try {
      Method m = m_delegate.getClass().getDeclaredMethod("getRLearner",
        new Class[] {});

      Object result = m.invoke(m_delegate, new Object[] {});
      return (SelectedTag) result;
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String learnerParamsTipText() {
    if (m_delegate == null) {
      init();
    }
    try {
      Method m = m_delegate.getClass().getDeclaredMethod(
        "learnerParamsTipText", new Class[] {});

      Object result = m.invoke(m_delegate, new Object[] {});
      return result.toString();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return "";
  }

  /**
   * Set the parameters for the R learner. This should be specified in the same
   * way (i.e. comma separated) as they would be if using the R console.
   * 
   * @param learnerParams the parameters (comma separated) to pass to the R
   *          learner.
   */
  public void setLearnerParams(String learnerParams) {
    if (m_delegate == null) {
      init();
    }
    try {
      Method m = m_delegate.getClass().getDeclaredMethod("setLearnerParams",
        new Class[] { String.class });

      m.invoke(m_delegate, new Object[] { learnerParams });
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Get the parameters for the R learner. This should be specified in the same
   * way (i.e. comma separated) as they would be if using the R console.
   * 
   * @return the parameters (comma separated) to pass to the R learner.
   */
  public String getLearnerParams() {
    if (m_delegate == null) {
      init();
    }
    try {
      Method m = m_delegate.getClass().getDeclaredMethod("getLearnerParams",
        new Class[] {});

      Object result = m.invoke(m_delegate, new Object[] {});
      return result.toString();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return "";
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String dontReplaceMissingValuesTipText() {
    if (m_delegate == null) {
      init();
    }
    try {
      Method m = m_delegate.getClass().getDeclaredMethod(
        "dontReplaceMissingValuesTipText", new Class[] {});

      Object result = m.invoke(m_delegate, new Object[] {});
      return result.toString();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return "";
  }

  /**
   * Set whether to turn off replacement of missing values in the data before it
   * is passed into R.
   * 
   * @param d true if missing values should not be replaced.
   */
  public void setDontReplaceMissingValues(boolean d) {
    if (m_delegate == null) {
      init();
    }
    try {
      Method m = m_delegate.getClass().getDeclaredMethod(
        "setDontReplaceMissingValues", new Class[] { Boolean.TYPE });

      m.invoke(m_delegate, new Object[] { new Boolean(d) });
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Get whether to turn off replacement of missing values in the data before it
   * is passed into R.
   * 
   * @return true if missing values should not be replaced.
   */
  public boolean getDontReplaceMissingValues() {
    if (m_delegate == null) {
      init();
    }
    try {
      Method m = m_delegate.getClass().getDeclaredMethod(
        "getDontReplaceMissingValues", new Class[] {});

      Object result = m.invoke(m_delegate, new Object[] {});
      return ((Boolean) result).booleanValue();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return false;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String logMessagesFromRTipText() {
    if (m_delegate == null) {
      init();
    }
    try {
      Method m = m_delegate.getClass().getDeclaredMethod(
        "logMessagesFromRTipText", new Class[] {});

      Object result = m.invoke(m_delegate, new Object[] {});
      return result.toString();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return "";
  }

  /**
   * Set whether to log info/warning messages from R to the console.
   * 
   * @param l true if info/warning messages should be logged to the console.
   */
  public void setLogMessagesFromR(boolean l) {
    if (m_delegate == null) {
      init();
    }
    try {
      Method m = m_delegate.getClass().getDeclaredMethod("setLogMessagesFromR",
        new Class[] { Boolean.TYPE });

      m.invoke(m_delegate, new Object[] { new Boolean(l) });
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Get whether to log info/warning messages from R to the console.
   * 
   * @return true if info/warning messages should be logged to the console.
   */
  public boolean getLogMessagesFromR() {
    if (m_delegate == null) {
      init();
    }
    try {
      Method m = m_delegate.getClass().getDeclaredMethod("getLogMessagesFromR",
        new Class[] {});

      Object result = m.invoke(m_delegate, new Object[] {});
      return ((Boolean) result).booleanValue();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return false;
  }

  /**
   * Build the specified R learner on the incoming training data.
   * 
   * @param data the training data to be used for generating the R model.
   * @throws Exception if the classifier could not be built successfully.
   */
  @Override
  public void buildClassifier(Instances data) throws Exception {
    if (m_delegate == null) {
      init();
    }
    try {
      // m_delegate.buildClassifier(data);
      Method m = m_delegate.getClass().getDeclaredMethod("buildClassifier",
        new Class[] { Instances.class });
      m.invoke(m_delegate, new Object[] { data });
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      throw new Exception(cause);
    }
  }

  /**
   * Batch scoring method
   * 
   * @param insts the instances to push over to R and get predictions for
   * @return an array of probability distributions, one for each instance
   * @throws Exception if a problem occurs
   */
  @Override
  public double[][] distributionsForInstances(Instances insts) throws Exception {
    if (m_delegate == null) {
      init();
    }

    return ((BatchPredictor) m_delegate).distributionsForInstances(insts);
  }

  /**
   * Calculates the class membership probabilities for the given test instance.
   * 
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @throws Exception if distribution can't be computed successfully
   */
  @Override
  public double[] distributionForInstance(Instance inst) throws Exception {
    if (m_delegate == null) {
      init();
    }
    try {
      Method m = m_delegate.getClass().getDeclaredMethod(
        "distributionForInstance", new Class[] { Instance.class });

      Object result = m.invoke(m_delegate, new Object[] { inst });

      return (double[]) result;
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();

      throw new Exception(cause);
    }
  }

  @Override
  public String toString() {
    if (m_delegate == null) {
      return "MLRClassifier: model not built yet!";
    }

    return m_delegate.toString();
  }

  public void closeREngine() {
    try {
      Method m = m_delegate.getClass().getDeclaredMethod("closeREngine",
        new Class[] {});

      m.invoke(m_delegate, new Object[] {});
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  /**
   * Main method for testing this class.
   * 
   * @param argv the options
   */
  public static void main(String[] args) {
    MLRClassifier c = new MLRClassifier();
    c.run(c, args);
  }

  @Override
  public void run(Object toRun, String[] options)
    throws IllegalArgumentException {
    if (!(toRun instanceof MLRClassifier)) {
      throw new IllegalArgumentException(
        "Object to run is not an MLRClassifier!");
    }

    try {
      ((MLRClassifier) toRun).setLaunchedFromCommandLine(true);
      runClassifier((Classifier) toRun, options);

      ((MLRClassifier) toRun).closeREngine();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
