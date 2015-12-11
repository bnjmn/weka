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
 *    Copyright (C) 2012-2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.mlr;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;

import weka.classifiers.RandomizableClassifier;
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
public class MLRClassifier extends RandomizableClassifier implements OptionHandler,
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
  public static final int R_CLASSIF_AVNNET = 1; // new in 2.7(?): nnet
  public static final int R_CLASSIF_BDK = 2; // new in 2.4(?): kohonen, bdk
  public static final int R_CLASSIF_BINOMIAL = 3; // new in 2.4(?): stats, binomial
  public static final int R_CLASSIF_BLACKBOOST = 4;
  public static final int R_CLASSIF_BOOSTING = 5;
  public static final int R_CLASSIF_BST = 6; // new in 2.4(?): bst, bst
  public static final int R_CLASSIF_CFOREST = 7; // new: party, cforest
  public static final int R_CLASSIF_CLUSTERSVM = 8; // new in 2.7(?): SwarmSVM, LiblineaR
  public static final int R_CLASSIF_CTREE = 9;
  public static final int R_CLASSIF_DBNDNN = 10; // new in 2.7(?): deepnet
  public static final int R_CLASSIF_DCSVM = 11; // new in 2.7(?): SwarmSVM
  public static final int R_CLASSIF_FNN = 12;
  public static final int R_CLASSIF_GATERSVM = 13; // new in 2.7(?): SwarmSVM, e1071
  public static final int R_CLASSIF_GBM = 14;
  public static final int R_CLASSIF_GEODA = 15; // new: DiscriMiner, geoDA
  public static final int R_CLASSIF_GLMBOOST = 16;
  public static final int R_CLASSIF_GLMNET = 17; // new: glmnet, glmnet
  public static final int R_CLASSIF_HDRDA = 18; // new in 2.4(?): sparsediscrim, hdrda
  public static final int R_CLASSIF_KKNN = 19;
  public static final int R_CLASSIF_KNN = 20; // new in 2.4(?): class, knn
  public static final int R_CLASSIF_KSVM = 21;
  public static final int R_CLASSIF_LDA = 22;
  public static final int R_CLASSIF_LIBLINEARL1L2SVC = 23; // new in 2.7(?): LiblineaR
  public static final int R_CLASSIF_LIBLINEARL1LOGREG = 24; // new in 2.7(?): LiblineaR
  public static final int R_CLASSIF_LIBLINEARL2L1SVC = 25; // new in 2.7(?): LiblineaR
  public static final int R_CLASSIF_LIBLINEARL2LOGREG = 26; // new in 2.7(?): LiblineaR
  public static final int R_CLASSIF_LIBLINEARL2SVC = 27; // new in 2.7(?): LiblineaR
  public static final int R_CLASSIF_LIBLINEARMULTICLASSSVC = 28; // new in 2.7(?): LiblineaR
  public static final int R_CLASSIF_LINDA = 29; // new: DiscriMiner, linDA
  public static final int R_CLASSIF_LOGREG = 30;
  public static final int R_CLASSIF_LQA = 31; // new in 2.2: lqa, lqa
  public static final int R_CLASSIF_LSSVM = 32;
  public static final int R_CLASSIF_LVQ1 = 33;
  public static final int R_CLASSIF_MDA = 34;
  public static final int R_CLASSIF_MLP = 35; // new in 2.7(?): RSNNS
  public static final int R_CLASSIF_MULTINOM = 36;
  public static final int R_CLASSIF_NAIVE_BAYES = 37;
  public static final int R_CLASSIF_NEURALNET = 38; // new in 2.7(?): neuralnet
  public static final int R_CLASSIF_NNET = 39;
  public static final int R_CLASSIF_NNTRAIN = 40; // new in 2.7(?): deepnet
  public static final int R_CLASSIF_NODEHARVEST = 41; // new in 2.4(?)
  public static final int R_CLASSIF_PAMR = 42; // new in 2.3: pamr, pamr
  public static final int R_CLASSIF_PLR = 43; // new: stepPlr, plr
  public static final int R_CLASSIF_PLSDACARET = 44; // new: caret, plsda
  public static final int R_CLASSIF_PROBIT = 45; // new in 2.4(?): stats, probit
  public static final int R_CLASSIF_QDA = 46;
  public static final int R_CLASSIF_QUADA = 47; // new: DiscriMiner, quaDA
  public static final int R_CLASSIF_RANDOM_FOREST = 48;
  public static final int R_CLASSIF_RANDOM_FOREST_SRC = 49; // new in 2.2: randomForestSRC
  public static final int R_CLASSIF_RANGER = 50; // new in 2.7(?): ranger
  public static final int R_CLASSIF_RDA = 51;
  public static final int R_CLASSIF_RFERNS = 52; // new in 2.4(?): rFerns, rFerns
  public static final int R_CLASSIF_RKNN = 53; // new in 2.7(?): rknn
  public static final int R_CLASSIF_ROTATIONFOREST = 54; // new in 2.7(?): rotationForest
  public static final int R_CLASSIF_RPART = 55;
  public static final int R_CLASSIF_RRLDA = 56; // new in 2.4(?): rrlda, rrlda
  public static final int R_CLASSIF_SAEDNN = 57; // new in 2.7(?): deepnet
  public static final int R_CLASSIF_SDA = 58; // new in 2.2: sda, sda
  public static final int R_CLASSIF_SPARSELDA = 59; // new in 2.4(?): sparseLDA, MASS, elasticnet, sparseLDA NOTE: result probably not correct
  public static final int R_CLASSIF_SVM = 60;
  public static final int R_CLASSIF_XGBOOST = 61; // new in 2.7(?): xgboost
  public static final int R_CLASSIF_XYF = 62; // new in 2.3: kohonen, xyf

  // Regression
  public static final int R_REGR_AVNNET = 63; // new in 2.7(?): nnet
  public static final int R_REGR_BCART = 64; // new in 2.4(?): tgp, bcart
  public static final int R_REGR_BDK = 65; // new in 2.4(?): kohonen, bdk
  public static final int R_REGR_BGP = 66; // new in 2.4(?): tgp, bgp
  public static final int R_REGR_BGPLLM = 67; // new in 2.4(?): tgp, bgpllm
  public static final int R_REGR_BLACKBOOST = 68;
  public static final int R_REGR_BLM = 69; // new in 2.4(?): tgp, blm
  public static final int R_REGR_BRNN = 70; // new in 2.4(?): brnn, brnn
  public static final int R_REGR_BST = 71; // new in 2.4(?): bst, bst
  public static final int R_REGR_BTGP = 72; // new in 2.4(?): tgp, btgp
  public static final int R_REGR_BTGPLLM = 73; // new in 2.4(?): tgp, btgpllm
  public static final int R_REGR_BTLM = 74; // new in 2.4(?): tgp, btlm
  public static final int R_REGR_CFOREST = 75; // new: party, cforest
  public static final int R_REGR_CRS = 76; // new: crs, crs
  public static final int R_REGR_CTREE = 77; // new in 2.2: party, ctree
  public static final int R_REGR_CUBIST = 78; // new in 2.4(?): Cubist, cubist
  public static final int R_REGR_EARTH = 79;
  public static final int R_REGR_ELMNN = 80; // new in 2.4(?): elmNN, elmNN
  public static final int R_REGR_FNN = 81;
  public static final int R_REGR_FRBS = 82; // new in 2.4(?): frbs, frbs
  public static final int R_REGR_GBM = 83;
  public static final int R_REGR_GLMBOOST = 84; // new in 2.7(?): mboost
  public static final int R_REGR_GLMNET = 85; // new: glmnet, glmnet
  public static final int R_REGR_KKNN = 86;
  public static final int R_REGR_KM = 87;
  public static final int R_REGR_KSVM = 88;
  public static final int R_REGR_LAGP = 89; // new in 2.7(?): laGP
  public static final int R_REGR_LIBLINEARL2L1SVR = 90; // new in 2.7(?): LiblineaR
  public static final int R_REGR_LIBLINEARL2L2SVR = 91; // new in 2.7(?): LiblineaR
  public static final int R_REGR_LM = 92;
  public static final int R_REGR_MARS = 93;
  public static final int R_REGR_MOB = 94; // new: party, mob
  public static final int R_REGR_NNET = 95;
  public static final int R_REGR_NODEHARVEST = 96; // new in 2.4(?): nodeHarvest
  public static final int R_REGR_PCR = 97; // new: pls, pcr
  public static final int R_REGR_PENALIZED_LASSO = 98; // new in 2.7(?): penalized
  public static final int R_REGR_PENALIZED_RIDGE = 99; // new in 2.7(?): penalized
  public static final int R_REGR_PLSR = 100; // new in 2.2: pls, pls
  public static final int R_REGR_RANDOM_FOREST = 101;
  public static final int R_REGR_RANDOM_FOREST_SRC = 102; // new in 2.2: randomForestSRC
  public static final int R_REGR_RANGER = 103; // new in 2.7(?): ranger
  public static final int R_REGR_RKNN = 104; // new in 2.7(?): rknn
  public static final int R_REGR_RPART = 105;
  public static final int R_REGR_RSM = 106;
  public static final int R_REGR_RVM = 107;
  public static final int R_REGR_SLIM = 108; // new in 2.4(?): flare, slim
  public static final int R_REGR_SVM = 109; // new: e1071, svm
  public static final int R_REGR_XGBOOST = 110; // new in 2.7(?): xgboost
  public static final int R_REGR_XYF = 111; // new in 2.4(?): kohonen, xyf

  /** Tags for the various types of learner */
  public static final Tag[] TAGS_LEARNER = {
          new Tag(R_CLASSIF_ADA, "ada", "classif.ada", false),
          new Tag(R_CLASSIF_AVNNET, "a.nnet", "classif.avNNet", false),
          new Tag(R_CLASSIF_BDK, "a.kohonen,class", "classif.bdk", false),
          new Tag(R_CLASSIF_BINOMIAL, "", "classif.binomial"),
          new Tag(R_CLASSIF_BLACKBOOST, "a.mboost, party", "classif.blackboost", false),
          new Tag(R_CLASSIF_BOOSTING, "adabag, rpart", "classif.boosting", false),
          new Tag(R_CLASSIF_BST, "a.bst", "classif.bst", false),
          new Tag(R_CLASSIF_CFOREST, "b.party", "classif.cforest", false),
          new Tag(R_CLASSIF_CLUSTERSVM, "a.SwarmSVM, LiblineaR", "classif.clusterSVM", false),
          new Tag(R_CLASSIF_CTREE, "c.party", "classif.ctree", false),
          new Tag(R_CLASSIF_DBNDNN, "a.deepnet", "classif.dbnDNN", false),
          new Tag(R_CLASSIF_DCSVM, "b.SwarmSVM", "classif.dcSVM", false),
          new Tag(R_CLASSIF_FNN, "a.FNN", "classif.fnn", false),
          new Tag(R_CLASSIF_GATERSVM, "c.SwarmSVM, e1071", "classif.gaterSVM", false),
          new Tag(R_CLASSIF_GBM, "a.gbm", "classif.gbm", false),
          new Tag(R_CLASSIF_GEODA, "a.DiscriMiner", "classif.geoDA", false),
          new Tag(R_CLASSIF_GLMBOOST, "b.mboost", "classif.glmboost", false),
          new Tag(R_CLASSIF_GLMNET, "a.glmnet", "classif.glmnet", false),
          new Tag(R_CLASSIF_HDRDA, "sparsediscrim", "classif.hdrda", false),
          new Tag(R_CLASSIF_KKNN, "a.kknn", "classif.kknn", false),
          new Tag(R_CLASSIF_KNN, "a.class", "classif.knn", false),
          new Tag(R_CLASSIF_KSVM, "a.kernlab", "classif.ksvm", false),
          new Tag(R_CLASSIF_LDA, "a.MASS", "classif.lda", false),
          new Tag(R_CLASSIF_LIBLINEARL1L2SVC, "a.LiblineaR", "classif.LiblineaRL1L2SVC", false),
          new Tag(R_CLASSIF_LIBLINEARL1LOGREG, "b.LiblineaR", "classif.LiblineaRL1LogReg", false),
          new Tag(R_CLASSIF_LIBLINEARL2L1SVC, "c.LiblineaR", "classif.LiblineaRL2L2SVC", false),
          new Tag(R_CLASSIF_LIBLINEARL2LOGREG, "d.LiblineaR", "classif.LiblineaRL2LogReg", false),
          new Tag(R_CLASSIF_LIBLINEARL2SVC, "e.LiblineaR", "classif.LiblineaRL2SVC", false),
          new Tag(R_CLASSIF_LIBLINEARMULTICLASSSVC, "f.LiblineaR", "classif.LiblineaRMultiClassSVC", false),
          new Tag(R_CLASSIF_LINDA, "b.DiscriMiner", "classif.linDA", false),
          new Tag(R_CLASSIF_LOGREG, "", "classif.logreg", false),
          new Tag(R_CLASSIF_LQA, "lqa", "classif.lqa", false),
          new Tag(R_CLASSIF_LSSVM, "b.kernlab", "classif.lssvm", false),
          new Tag(R_CLASSIF_LVQ1, "b.class", "classif.lvq1", false),
          new Tag(R_CLASSIF_MDA, "a.mda", "classif.mda", false),
          new Tag(R_CLASSIF_MLP, "RSNNS", "classif.mlp", false),
          new Tag(R_CLASSIF_MULTINOM, "b.nnet", "classif.multinom", false),
          new Tag(R_CLASSIF_NAIVE_BAYES, "a.e1071", "classif.naiveBayes", false),
          new Tag(R_CLASSIF_NEURALNET, "neuralnet", "classif.neuralnet", false),
          new Tag(R_CLASSIF_NNET, "c.nnet", "classif.nnet", false),
          new Tag(R_CLASSIF_NNTRAIN, "b.deepnet", "classif.nnTrain", false),
          new Tag(R_CLASSIF_NODEHARVEST, "a.nodeHarvest", "classif.nodeHarvest", false),
          new Tag(R_CLASSIF_PAMR, "pamr", "classif.pamr", false),
          new Tag(R_CLASSIF_PLR, "stepPlr", "classif.plr", false),
          new Tag(R_CLASSIF_PLSDACARET, "caret,pls", "classif.plsdaCaret", false),
          new Tag(R_CLASSIF_PROBIT, "", "classif.probit", false),
          new Tag(R_CLASSIF_QDA, "b.MASS", "classif.qda", false),
          new Tag(R_CLASSIF_QUADA, "d.DiscriMiner", "classif.quaDA", false),
          new Tag(R_CLASSIF_RANDOM_FOREST, "a.randomForest", "classif.randomForest", false),
          new Tag(R_CLASSIF_RANDOM_FOREST_SRC, "a.randomForestSRC", "classif.randomForestSRC", false),
          new Tag(R_CLASSIF_RANGER, "a.ranger", "classif.ranger", false),
          new Tag(R_CLASSIF_RDA, "b.klaR", "classif.rda", false),
          new Tag(R_CLASSIF_RFERNS, "rFerns", "classif.rFerns", false),
          new Tag(R_CLASSIF_RKNN, "a.rknn", "classif.rknn", false),
          new Tag(R_CLASSIF_ROTATIONFOREST, "rotationForest", "classif.rotationForest", false),
          new Tag(R_CLASSIF_RPART, "a.rpart", "classif.rpart", false),
          new Tag(R_CLASSIF_RRLDA, "rrlda", "classif.rrlda", false),
          new Tag(R_CLASSIF_SAEDNN, "c.deepnet", "classif.saeDNN", false),
          new Tag(R_CLASSIF_SDA, "sda", "classif.sda", false),
          new Tag(R_CLASSIF_SPARSELDA, "sparseLDA, MASS, elasticnet", "classif.sparseLDA", false),
          new Tag(R_CLASSIF_SVM, "b.e1071", "classif.svm", false),
          new Tag(R_CLASSIF_XGBOOST, "a.xgboost", "classif.xgboost", false),
          new Tag(R_CLASSIF_XYF, "b.kohonen,class", "classif.xyf", false),

          new Tag(R_REGR_AVNNET, "d.nnet", "regr.avNNet", false),
          new Tag(R_REGR_BCART, "a.tgp", "regr.bcart", false),
          new Tag(R_REGR_BDK, "c.kohonen,class", "regr.bdk", false),
          new Tag(R_REGR_BGP, "b.tgp", "regr.bgp", false),
          new Tag(R_REGR_BGPLLM, "c.tgp", "regr.bgpllm", false),
          new Tag(R_REGR_BLACKBOOST, "c.mboost", "regr.blackboost", false),
          new Tag(R_REGR_BLM, "d.tgp", "regr.blm", false),
          new Tag(R_REGR_BRNN, "brnn", "regr.brnn", false),
          new Tag(R_REGR_BST, "b.bst", "regr.bst", false),
          new Tag(R_REGR_BTGP, "e.tgp", "regr.btgp", false),
          new Tag(R_REGR_BTGPLLM, "f.tgp", "regr.btgpllm", false),
          new Tag(R_REGR_BTLM, "g.tgp", "regr.btlm", false),
          new Tag(R_REGR_CFOREST, "e.party", "regr.cforest", false),
          new Tag(R_REGR_CRS, "crs", "regr.crs", false),
          new Tag(R_REGR_CTREE, "f.party", "regr.ctree", false),
          new Tag(R_REGR_CUBIST, "Cubist", "regr.cubist", false),
          new Tag(R_REGR_EARTH, "earth", "regr.earth", false),
          new Tag(R_REGR_ELMNN, "elmNN", "regr.elmNN", false),
          new Tag(R_REGR_FNN, "b.FNN", "regr.fnn", false),
          new Tag(R_REGR_FRBS, "frbs", "regr.frbs", false),
          new Tag(R_REGR_GBM, "b.gbm", "regr.gbm", false),
          new Tag(R_REGR_GLMBOOST, "d.mboost", "regr.glmboost", false),
          new Tag(R_REGR_GLMNET, "b.glmnet", "regr.glmnet", false),
          new Tag(R_REGR_KKNN, "b.kknn", "regr.kknn", false),
          new Tag(R_REGR_KM, "DiceKriging", "regr.km", false),
          new Tag(R_REGR_KSVM, "c.kernlab", "regr.ksvm", false),
          new Tag(R_REGR_LAGP, "laGP", "regr.laGP", false),
          new Tag(R_REGR_LIBLINEARL2L1SVR, "g.LiblineaR", "classif.LiblineaRL2L1SVR", false),
          new Tag(R_REGR_LIBLINEARL2L2SVR, "h.LiblineaR", "classif.LiblineaRL2L2SVR", false),
          new Tag(R_REGR_LM, "", "regr.lm", false),
          new Tag(R_REGR_MARS, "b.mda", "regr.mars", false),
          new Tag(R_REGR_MOB, "g.party", "regr.mob", false),
          new Tag(R_REGR_NNET, "e.nnet", "regr.nnet", false),
          new Tag(R_REGR_NODEHARVEST, "b.nodeHarvest", "regr.nodeHarvest", false),
          new Tag(R_REGR_PCR, "a.pls", "regr.pcr", false),
          new Tag(R_REGR_PENALIZED_LASSO, "a.penalized", "regr.penalized.lasso", false),
          new Tag(R_REGR_PLSR, "b.pls", "regr.plsr", false),
          new Tag(R_REGR_RANDOM_FOREST, "b.randomForest", "regr.randomForest", false),
          new Tag(R_REGR_RANDOM_FOREST_SRC, "b.randomForestSRC", "regr.randomForestSRC", false),
          new Tag(R_REGR_RANGER, "b.ranger", "regr.ranger", false),
          new Tag(R_REGR_RKNN, "b.rknn", "regr.rknn", false),
          new Tag(R_REGR_RPART, "b.rpart", "regr.rpart", false),
          new Tag(R_REGR_RSM, "a.rsm", "regr.rsm", false),
          new Tag(R_REGR_RVM, "d.kernlab", "regr.rvm", false),
          new Tag(R_REGR_SLIM, "flare", "regr.slim", false),
          new Tag(R_REGR_SVM, "c.e1071", "regr.svm", false),
          new Tag(R_REGR_XGBOOST, "b.xgboost", "regr.xgboost", false),
          new Tag(R_REGR_XYF, "d.kohonen,class", "regr.xyf", false)};

  protected static final String IMPL =
    "weka.classifiers.mlr.impl.MLRClassifierImpl";

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
      + "and making predictions with various R classifiers and regression methods. Check\n\n"
      + "  http://berndbischl.github.io/mlr/tutorial/html/integrated_learners\n\n"
      + "for the list of algorithms in MLR, and\n\n"
      + "  http://http://cran.r-project.org/web/packages/mlr\n\n"
      + "for further information on the package and its algorithms.\n\n"
      + "Note that a classifier/regressor will be installed in R when it is selected as the "
      + "learner parameter for MLRClassifier and has not been installed before in R. This may take a little while "
      + "because it involves downloading and processing R packages.";
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
      Method m =
        m_delegate.getClass().getDeclaredMethod("setLaunchedFromCommandLine",
          new Class[] { Boolean.class });

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
      Method m =
        m_delegate.getClass().getDeclaredMethod("debugTipText", new Class[] {});

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
      Method m =
        m_delegate.getClass().getDeclaredMethod("setDebug",
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
      Method m =
        m_delegate.getClass().getDeclaredMethod("getDebug", new Class[] {});

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
      Method m =
        m_delegate.getClass().getDeclaredMethod("batchSizeTipText",
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
      Method m =
        m_delegate.getClass().getDeclaredMethod("getBatchSize", new Class[] {});

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
      Method m =
        m_delegate.getClass().getDeclaredMethod("setBatchSize",
          new Class[] { String.class });

      m.invoke(m_delegate, new Object[] { size });
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Returns true, as R schemes have to predict in batches and we push entire
   * test sets over into R
   *
   * @return true
   */
  public boolean implementsMoreEfficientBatchPrediction() {
    return true;
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
      Method m =
        m_delegate.getClass().getDeclaredMethod("RLearnerTipText",
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
      Method m =
        m_delegate.getClass().getDeclaredMethod("setRLearner",
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
      Method m =
        m_delegate.getClass().getDeclaredMethod("getRLearner", new Class[] {});

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
      Method m =
        m_delegate.getClass().getDeclaredMethod("learnerParamsTipText",
          new Class[] {});

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
      Method m =
        m_delegate.getClass().getDeclaredMethod("setLearnerParams",
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
      Method m =
        m_delegate.getClass().getDeclaredMethod("getLearnerParams",
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
      Method m =
        m_delegate.getClass().getDeclaredMethod(
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
      Method m =
        m_delegate.getClass().getDeclaredMethod("setDontReplaceMissingValues",
          new Class[] { Boolean.TYPE });

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
      Method m =
        m_delegate.getClass().getDeclaredMethod("getDontReplaceMissingValues",
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
  public String logMessagesFromRTipText() {
    if (m_delegate == null) {
      init();
    }
    try {
      Method m =
        m_delegate.getClass().getDeclaredMethod("logMessagesFromRTipText",
          new Class[] {});

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
      Method m =
        m_delegate.getClass().getDeclaredMethod("setLogMessagesFromR",
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
      Method m =
        m_delegate.getClass().getDeclaredMethod("getLogMessagesFromR",
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
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String seedTipText() {
    if (m_delegate == null) {
      init();
    }
    try {
      Method m =
              m_delegate.getClass().getDeclaredMethod("seedTipText",
                      new Class[] {});

      Object result = m.invoke(m_delegate, new Object[] {});
      return result.toString();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return "";
  }

  /**
   * Set the seed for random number generation.
   *
   * @param seed the seed
   */
  public void setSeed(int seed) {
    if (m_delegate == null) {
      init();
    }
    try {
      Method m =
              m_delegate.getClass().getDeclaredMethod("setSeed",
                      new Class[] { Integer.TYPE });

      m.invoke(m_delegate, new Object[] { new Integer(seed) });
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Gets the seed for the random number generations
   *
   * @return the seed for the random number generation
   */
  public int getSeed() {
    if (m_delegate == null) {
      init();
    }
    try {
      Method m =
              m_delegate.getClass().getDeclaredMethod("getSeed",
                      new Class[]{});

      Object result = m.invoke(m_delegate, new Object[]{});
      return ((Integer) result).intValue();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return -1;
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
      Method m =
        m_delegate.getClass().getDeclaredMethod("buildClassifier",
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
   * @param inst the instance to be classified
   * @return predicted class probability distribution
   * @throws Exception if distribution can't be computed successfully
   */
  @Override
  public double[] distributionForInstance(Instance inst) throws Exception {
    if (m_delegate == null) {
      init();
    }
    try {
      Method m =
        m_delegate.getClass().getDeclaredMethod("distributionForInstance",
          new Class[] { Instance.class });

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
      Method m =
        m_delegate.getClass().getDeclaredMethod("closeREngine", new Class[] {});

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
   * @param args the options
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
