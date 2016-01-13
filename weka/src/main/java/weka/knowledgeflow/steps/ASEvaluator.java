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
 *    ASEvaluator.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.AttributeEvaluator;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.AttributeTransformer;
import weka.attributeSelection.RankedOutputSearch;
import weka.attributeSelection.Ranker;
import weka.attributeSelection.SubsetEvaluator;
import weka.attributeSelection.UnsupervisedAttributeEvaluator;
import weka.attributeSelection.UnsupervisedSubsetEvaluator;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;
import weka.core.Utils;
import weka.core.WekaException;
import weka.filters.unsupervised.attribute.Remove;
import weka.gui.ProgrammaticProperty;
import weka.gui.knowledgeflow.StepVisual;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Step that wraps a Weka attribute or subset evaluator. Handles training and
 * test set connections. Requires an ASSearchStrategy step to be connected via
 * an "info" connection. Will output both attribute selection results (via text
 * connections) and transformed data (via outgoing train or test set
 * connections). When processing multiple incoming training and test folds, the
 * step can either output a cross-validation style summary over all the folds or
 * individual attribute selection results for each fold.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 * @see ASSearchStrategy
 */
@KFStep(name = "ASEvaluator", category = "AttSelection",
  toolTipText = "Weka attribute selection evaluator wrapper", iconPath = "",
  resourceIntensive = true)
public class ASEvaluator extends WekaAlgorithmWrapper {

  private static final long serialVersionUID = -1280208826860871742L;

  /** The evaluator (attribute or subset) being used */
  protected ASEvaluation m_evaluatorTemplate;

  /**
   * The search strategy being used (retrieved via an incoming "info" connection
   */
  protected ASSearch m_searchTemplate;

  /**
   * Any test folds waiting to be processed (i.e. have their dimensionality
   * reduced
   */
  protected Map<Integer, Instances> m_waitingTestData =
    new HashMap<Integer, Instances>();

  /** Holds selected attribute indices corresponding to training folds */
  protected Map<Integer, int[]> m_selectedAttsStore =
    new HashMap<Integer, int[]>();

  /**
   * Holds the calculated number of attributes to select (may depend on
   * thresholds) for each training fold
   */
  protected Map<Integer, Integer> m_numToSelectStore =
    new HashMap<Integer, Integer>();

  /**
   * Holds the evaluator trained per fold in the case when it is a transformer
   * (such as PCA)
   */
  protected Map<Integer, AttributeTransformer> m_transformerStore =
    new HashMap<Integer, AttributeTransformer>();

  /** True if we've been reset */
  protected boolean m_isReset;

  /**
   * True if we are processing cross-validation folds to produce a summary over
   * the folds (as opposed to producing separate results per fold).
   */
  protected boolean m_isDoingXVal;

  /** Keeps count of the folds processed */
  protected AtomicInteger m_setCount;

  /**
   * Whether to output separate evaluation results for each fold of a xval or
   * report the cross-validation summary
   */
  protected boolean m_treatXValFoldsSeparately;

  /** Whether a ranking is being produced by the attribute selection */
  protected boolean m_isRanking;

  /**
   * Eval to use when performing a cross-validation and not outputting separate
   * results for each fold
   */
  protected AttributeSelection m_eval;

  /**
   * Get the class of Weka algorithm wrapped by this wrapper
   *
   * @return the wrapped algorithm class
   */
  @Override
  public Class getWrappedAlgorithmClass() {
    return weka.attributeSelection.ASEvaluation.class;
  }

  /**
   * Set an instance of the wrapped algorithm to use
   *
   * @param algo the algorithm to use
   */
  @Override
  public void setWrappedAlgorithm(Object algo) {
    super.setWrappedAlgorithm(algo);
    m_defaultIconPath = StepVisual.BASE_ICON_PATH
      + "filters.supervised.attribute.AttributeSelection.gif";
  }

  /**
   * Get the evaluator wrapped by this step
   *
   * @return the attribute or subset evaluator wrapped by this step
   */
  public ASEvaluation getEvaluator() {
    return (weka.attributeSelection.ASEvaluation) getWrappedAlgorithm();
  }

  /**
   * Set the evaluator to wrap (just calls setWrappedAlgorithm)
   *
   * @param eval the evaluator to use
   */
  @ProgrammaticProperty
  public void setEvaluator(ASEvaluation eval) {
    setWrappedAlgorithm(eval);
  }

  /**
   * Set whether to output separate results for each fold of a cross-validation,
   * rather than averaging over folds.
   *
   * @param treatSeparately true if each fold will have results output
   */
  @OptionMetadata(displayName = "Treat x-val folds separately",
    description = "Output separate attribute selection results for each fold "
      + "of a cross-validation (rather than averaging across folds)")
  public void setTreatXValFoldsSeparately(boolean treatSeparately) {
    m_treatXValFoldsSeparately = treatSeparately;
  }

  /**
   * Get whether to output separate results for each fold of a cross-validation,
   * rather than averaging over folds.
   *
   * @return true if each fold will have results output
   */
  public boolean getTreatXValFoldsSeparately() {
    return m_treatXValFoldsSeparately;
  }

  /**
   * Initialize at the start of a run
   * 
   * @throws WekaException if there is an illegal configuration (i.e. Ranker
   *           search with subset evaluator or regular search with attribute
   *           evaluator
   */
  @Override
  public void stepInit() throws WekaException {
    if (!(getWrappedAlgorithm() instanceof ASEvaluation)) {
      throw new WekaException("Incorrect type of algorithm");
    }

    try {
      m_evaluatorTemplate =
        ASEvaluation.makeCopies((ASEvaluation) getWrappedAlgorithm(), 1)[0];
    } catch (Exception ex) {
      throw new WekaException(ex);
    }

    List<StepManager> infos = getStepManager()
      .getIncomingConnectedStepsOfConnectionType(StepManager.CON_INFO);
    if (infos.size() == 0) {
      throw new WekaException(
        "A search strategy needs to be supplied via an 'info' "
          + "connection type");
    }

    ASSearchStrategy searchStrategy =
      (ASSearchStrategy) infos.get(0).getInfoStep(ASSearchStrategy.class);
    m_searchTemplate = searchStrategy.getSearchStrategy();

    if (m_searchTemplate instanceof RankedOutputSearch) {

      m_isRanking =
        ((RankedOutputSearch) m_searchTemplate).getGenerateRanking();
    }

    if (m_evaluatorTemplate instanceof SubsetEvaluator
      && m_searchTemplate instanceof Ranker) {
      throw new WekaException(
        "The Ranker search strategy cannot be used with a "
          + "subset evaluator");
    }

    if (m_evaluatorTemplate instanceof AttributeEvaluator
      && !(m_searchTemplate instanceof Ranker)) {
      throw new WekaException("The Ranker search strategy must be used in "
        + "conjunction with an attribute evaluator");
    }

    m_isReset = true;
    m_waitingTestData.clear();
    m_selectedAttsStore.clear();
    m_numToSelectStore.clear();
    m_transformerStore.clear();
    m_eval = new AttributeSelection();
  }

  /**
   * Process an incoming Data object
   *
   * @param data the data object to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public void processIncoming(Data data) throws WekaException {

    Instances train = data.getPayloadElement(StepManager.CON_TRAININGSET);
    Instances test = data.getPayloadElement(StepManager.CON_TESTSET);
    Integer setNum = data.getPayloadElement(StepManager.CON_AUX_DATA_SET_NUM);
    Integer maxSetNum =
      data.getPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM);
    if (m_isReset) {
      m_isReset = false;
      getStepManager().processing();
      m_setCount = new AtomicInteger(maxSetNum != null ? maxSetNum : 1);
      if (setNum != null && maxSetNum != null) {
        m_isDoingXVal = maxSetNum > 1 && !m_treatXValFoldsSeparately;

        if (m_evaluatorTemplate instanceof AttributeTransformer && m_isDoingXVal
          && !m_treatXValFoldsSeparately) {
          throw new WekaException(
            "Can't cross-validate an attribute transformer");
        }

        if (m_isDoingXVal) {
          m_eval.setFolds(maxSetNum);
        }
        if (m_isRanking) {
          m_eval.setRanking(m_isRanking);
        }
      }
    }

    if (m_isDoingXVal) {
      processXVal(train, test, setNum, maxSetNum);
    } else {
      processNonXVal(train, test, setNum, maxSetNum);
    }

    if (isStopRequested()) {
      getStepManager().interrupted();
    } else if (m_setCount.get() == 0) {
      if (m_isDoingXVal) {
        // output xval summary
        try {
          StringBuilder builder = new StringBuilder();
          builder.append("Search method: ");
          String evalS = m_evaluatorTemplate.getClass().getCanonicalName();
          evalS = evalS.substring(evalS.lastIndexOf('.') + 1, evalS.length());
          builder.append(evalS).append(" ")
            .append(m_evaluatorTemplate instanceof OptionHandler ? Utils
              .joinOptions(((OptionHandler) m_evaluatorTemplate).getOptions())
              : "")
            .append("\nEvaluator: ");
          String searchS = m_searchTemplate.getClass().getCanonicalName();
          searchS =
            searchS.substring(searchS.lastIndexOf('.') + 1, searchS.length());
          builder.append(searchS).append(" ")
            .append(m_searchTemplate instanceof OptionHandler ? Utils
              .joinOptions(((OptionHandler) m_searchTemplate).getOptions())
              : "")
            .append("\n");

          builder.append(m_eval.CVResultsString());

          outputTextData(builder.toString(), null);
        } catch (Exception ex) {
          throw new WekaException(ex);
        }
      }

      getStepManager().finished();
      // save memory
      m_waitingTestData.clear();
      m_selectedAttsStore.clear();
      m_numToSelectStore.clear();
    }
  }

  /**
   * Output Data to outgoing text connections
   *
   * @param text the text to output
   * @param setNum the fold/set number that this text is associated with
   * @throws WekaException if a problem occurs
   */
  protected void outputTextData(String text, Integer setNum)
    throws WekaException {
    if (isStopRequested()) {
      return;
    }
    if (getStepManager()
      .numOutgoingConnectionsOfType(StepManager.CON_TEXT) == 0) {
      return;
    }

    Data textData = new Data(StepManager.CON_TEXT, text);
    String titleString = m_evaluatorTemplate.getClass().getCanonicalName();
    titleString = titleString.substring(titleString.lastIndexOf('.') + 1,
      titleString.length());
    String searchString = m_searchTemplate.getClass().getCanonicalName();
    searchString = searchString.substring(searchString.lastIndexOf('.') + 1,
      searchString.length());
    titleString += " (" + searchString + ")";
    textData.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE,
      titleString);
    if (setNum != null) {
      textData.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, setNum);
    }

    getStepManager().outputData(textData);
  }

  /**
   * Handles processing for single train sets, single train/test pairs or
   * cross-validation folds when results are output for each separately
   *
   * @param train the training data
   * @param test the test data
   * @param setNum the set number for the training/test data
   * @param maxSetNum the maximum set number
   * @throws WekaException if a problem occurs
   */
  protected void processNonXVal(Instances train, Instances test, Integer setNum,
    Integer maxSetNum) throws WekaException {

    if (train != null) {
      try {
        AttributeSelection eval = new AttributeSelection();
        ASEvaluation evalCopy =
          ASEvaluation.makeCopies(m_evaluatorTemplate, 1)[0];
        ASSearch searchCopy = ASSearch.makeCopies(m_searchTemplate, 1)[0];
        eval.setEvaluator(evalCopy);
        eval.setSearch(searchCopy);
        eval.setRanking(m_isRanking);

        if (!isStopRequested()) {
          String message = "Selecting attributes (" + train.relationName();
          if (setNum != null && maxSetNum != null) {
            message += ", set " + setNum + " of " + maxSetNum;
          }
          message += ")";
          getStepManager().statusMessage(message);
          getStepManager().logBasic(message);
          eval.SelectAttributes(train);
          if (evalCopy instanceof AttributeTransformer) {
            m_transformerStore.put(setNum != null ? setNum : -1,
              ((AttributeTransformer) evalCopy));
          }

          // this will be the final set of selected (and potentially ranked)
          // attributes including class attribute (if appropriate)
          int[] selectedAtts = eval.selectedAttributes();

          if (m_isRanking) {
            m_numToSelectStore.put(setNum != null ? setNum : -1,
              ((RankedOutputSearch) searchCopy).getCalculatedNumToSelect());
          }

          // > 2 here as the info connection counts as 1
          if (getStepManager().numIncomingConnections() > 2) {
            m_selectedAttsStore.put(setNum != null ? setNum : -1, selectedAtts);
          }
          String results = eval.toResultsString();
          outputTextData(results, setNum);
          applyFiltering(StepManager.CON_TRAININGSET, selectedAtts, train,
            setNum, maxSetNum);

          // > 2 here because the info connection counts as 1
          if (getStepManager().numIncomingConnections() > 2) {
            Instances waitingTest =
              m_waitingTestData.get(setNum != null ? setNum : -1);
            if (waitingTest != null) {
              checkTestFiltering(waitingTest, setNum != null ? setNum : -1,
                maxSetNum);
            }
          } else {
            m_setCount.decrementAndGet();
          }
          evalCopy.clean();
        }
      } catch (Exception ex) {
        throw new WekaException(ex);
      }
    } else {
      checkTestFiltering(test, setNum != null ? setNum : -1, maxSetNum);
    }
  }

  /**
   * Handles processing in the cross-validation case when results are to be
   * accumulated/averaged over the folds
   * 
   * @param train the training data
   * @param test the test data
   * @param setNum the set number of this train/test pair
   * @param maxSetNum the maximum set number
   * @throws WekaException if a problem occurs
   */
  protected void processXVal(Instances train, Instances test, Integer setNum,
    Integer maxSetNum) throws WekaException {

    if (train != null) {
      try {
        ASEvaluation evalCopy =
          ASEvaluation.makeCopies(m_evaluatorTemplate, 1)[0];
        ASSearch searchCopy = ASSearch.makeCopies(m_searchTemplate, 1)[0];
        if (!isStopRequested()) {
          String message =
            "Selecting attributes x-val mode (" + train.relationName();
          if (setNum != null && maxSetNum != null) {
            message += ", set " + setNum + " of " + maxSetNum;
          }
          message += ")";
          getStepManager().statusMessage(message);
          getStepManager().logBasic(message);
          evalCopy.buildEvaluator(train);
          if (evalCopy instanceof AttributeTransformer) {
            m_transformerStore.put(setNum != null ? setNum : -1,
              ((AttributeTransformer) evalCopy));
          }

          int[] selectedAtts = searchCopy.search(evalCopy, train);
          selectedAtts = evalCopy.postProcess(selectedAtts);
          if (m_isRanking) {
            double[][] ranked =
              ((RankedOutputSearch) searchCopy).rankedAttributes();
            selectedAtts = new int[ranked.length];
            for (int i = 0; i < ranked.length; i++) {
              selectedAtts[i] = (int) ranked[i][0];
            }
          }

          updateXValStats(train, evalCopy, searchCopy, selectedAtts);

          // > 2 here because the info connection counts as 1
          if (getStepManager().numIncomingConnections() > 2) {
            m_selectedAttsStore.put(setNum, selectedAtts);
          }
          if (m_isRanking) {
            m_numToSelectStore.put(setNum,
              ((RankedOutputSearch) searchCopy).getCalculatedNumToSelect());
          }

          applyFiltering(StepManager.CON_TRAININGSET, selectedAtts, train,
            setNum, maxSetNum);

          // > 2 here because the info connection counts as 1
          if (getStepManager().numIncomingConnections() > 2) {
            Instances waitingTest = m_waitingTestData.get(setNum);
            if (waitingTest != null) {
              checkTestFiltering(waitingTest, setNum, maxSetNum);
            }
          } else {
            m_setCount.decrementAndGet();
          }
          evalCopy.clean();
        }
      } catch (Exception ex) {
        throw new WekaException(ex);
      }
    } else {
      checkTestFiltering(test, setNum, maxSetNum);
    }
  }

  /**
   * Check to see if there is a waiting set of selected attributes that can be
   * used to reduce the dimensionality of the supplied test set
   *
   * @param test the test set to potentially filter
   * @param setNum the set number of the test set
   * @param maxSetNum the maximum set number
   * @throws WekaException if a problem occurs
   */
  protected synchronized void checkTestFiltering(Instances test, Integer setNum,
    Integer maxSetNum) throws WekaException {
    if (isStopRequested()) {
      return;
    }

    int[] selectedForSet = m_selectedAttsStore.get(setNum);
    if (selectedForSet == null) {
      m_waitingTestData.put(setNum, test);
    } else {
      applyFiltering(StepManager.CON_TESTSET, selectedForSet, test, setNum,
        maxSetNum);
      m_setCount.decrementAndGet();
    }
  }

  /**
   * Apply a filter to reduce the dimensionality of the supplied data. Outputs
   * the reduced data to downstream steps on the given connection type
   *
   * @param connType the connection type to output on
   * @param selectedAtts selected attribute indices to use when filtering
   * @param data the instances to filter
   * @param setNum the set number of the instances
   * @param maxSetNum the maximum set number
   * @throws WekaException if a problem occurs
   */
  protected void applyFiltering(String connType, int[] selectedAtts,
    Instances data, Integer setNum, Integer maxSetNum) throws WekaException {
    if (isStopRequested()) {
      return;
    }

    if (getStepManager().numOutgoingConnectionsOfType(connType) == 0) {
      return;
    }

    int[] finalSet = new int[selectedAtts.length];
    boolean adjust = (m_isDoingXVal || m_isRanking)
      && ((!(m_evaluatorTemplate instanceof UnsupervisedSubsetEvaluator)
        && !(m_evaluatorTemplate instanceof UnsupervisedAttributeEvaluator))
        || m_evaluatorTemplate instanceof AttributeTransformer);

    if (m_isRanking) {
      int numToSelect = m_numToSelectStore.get(setNum != null ? setNum : -1);
      finalSet = new int[numToSelect];
      if (data.classIndex() >= 0) {
        if (adjust) {
          // one more for the class
          finalSet = new int[numToSelect + 1];
          finalSet[numToSelect] = data.classIndex();
        } else {
          finalSet = new int[numToSelect];
        }
      }
      for (int i = 0; i < numToSelect; i++) {
        finalSet[i] = selectedAtts[i];
      }
    } else {
      if (adjust) {
        // one more for the class
        finalSet = new int[selectedAtts.length + 1];
        finalSet[selectedAtts.length] = data.classIndex();
      }
      for (int i = 0; i < selectedAtts.length; i++) {
        finalSet[i] = selectedAtts[i];
      }
    }
    try {
      Instances reduced = null;
      AttributeTransformer transformer =
        m_transformerStore.get(setNum != null ? setNum : -1);
      if (transformer != null) {
        reduced =
          new Instances(transformer.transformedHeader(), data.numInstances());
        for (int i = 0; i < data.numInstances(); i++) {
          reduced.add(transformer.convertInstance(data.instance(i)));
        }
      } else {
        Remove r = new Remove();
        r.setAttributeIndicesArray(finalSet);
        r.setInvertSelection(true);
        r.setInputFormat(data);

        reduced = weka.filters.Filter.useFilter(data, r);
      }
      if (!isStopRequested()) {
        String message = "Filtering " + connType + " (" + data.relationName();
        if (setNum != null && maxSetNum != null) {
          message += ", set " + setNum + " of " + maxSetNum;
        }
        message += ")";
        getStepManager().statusMessage(message);
        getStepManager().logBasic(message);

        Data output = new Data(connType, reduced);
        output.setPayloadElement(StepManager.CON_AUX_DATA_SET_NUM, setNum);
        output.setPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM,
          maxSetNum);
        getStepManager().outputData(output);
      }
    } catch (Exception ex) {
      throw new WekaException(ex);
    }

  }

  /**
   * Updates stats in the cross-validation case
   *
   * @param train the training data processed
   * @param evaluator the evaluator used
   * @param search the search strategy
   * @param selectedAtts the attributes selected on this training data
   * @throws Exception if a problem occurs
   */
  protected synchronized void updateXValStats(Instances train,
    ASEvaluation evaluator, ASSearch search, int[] selectedAtts)
      throws Exception {

    m_eval.updateStatsForModelCVSplit(train, evaluator, search, selectedAtts,
      m_isRanking);
  }

  /**
   * Get incoming connections accepted given the current state of the step
   *
   * @return a list of acceptable incoming connections
   */
  @Override
  public List<String> getIncomingConnectionTypes() {
    List<String> result = new ArrayList<String>();

    if (getStepManager()
      .numIncomingConnectionsOfType(StepManager.CON_TRAININGSET) == 0) {
      result.add(StepManager.CON_TRAININGSET);
    }

    if (getStepManager()
      .numIncomingConnectionsOfType(StepManager.CON_TESTSET) == 0
      && getStepManager()
        .numIncomingConnectionsOfType(StepManager.CON_TRAININGSET) == 1) {
      result.add(StepManager.CON_TESTSET);
    }

    if (getStepManager()
      .numIncomingConnectionsOfType(StepManager.CON_INFO) == 0) {
      result.add(StepManager.CON_INFO);
    }

    return result;
  }

  /**
   * Get a list of output connections that can be produced given the current
   * state of the step
   *
   * @return a list of output connections
   */
  @Override
  public List<String> getOutgoingConnectionTypes() {
    List<String> result = new ArrayList<String>();

    if (getStepManager().numIncomingConnections() > 1 && getStepManager()
      .numIncomingConnectionsOfType(StepManager.CON_INFO) == 1) {
      result.add(StepManager.CON_TEXT);
    }

    if (getStepManager()
      .numIncomingConnectionsOfType(StepManager.CON_TRAININGSET) == 1
      && getStepManager()
        .numIncomingConnectionsOfType(StepManager.CON_INFO) == 1) {
      result.add(StepManager.CON_TRAININGSET);
    }

    if (getStepManager()
      .numIncomingConnectionsOfType(StepManager.CON_TESTSET) == 1
      && getStepManager()
        .numIncomingConnectionsOfType(StepManager.CON_INFO) == 1) {
      result.add(StepManager.CON_TESTSET);
    }

    return result;
  }

  /**
   * Get the class name of the custom editor for this step
   *
   * @return the class name of the custom editor for this step
   */
  @Override
  public String getCustomEditorForStep() {
    return "weka.gui.knowledgeflow.steps.ASEvaluatorStepEditorDialog";
  }
}
