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
 *    ClassifierPerformanceEvaluator.java
 *    Copyright (C) 2002-2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.classifiers.AggregateableEvaluation;
import weka.classifiers.CostMatrix;
import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.core.BatchPredictor;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;
import weka.core.Utils;
import weka.core.WekaException;
import weka.gui.ProgrammaticProperty;
import weka.gui.explorer.ClassifierErrorsPlotInstances;
import weka.gui.explorer.ExplorerDefaults;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.gui.visualize.PlotData2D;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.ExecutionResult;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.StepTask;
import weka.knowledgeflow.StepTaskCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Step that implements batch classifier evaluation
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "ClassifierPerformanceEvaluator", category = "Evaluation",
  toolTipText = "Evaluates batch classifiers",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "ClassifierPerformanceEvaluator.gif")
public class ClassifierPerformanceEvaluator extends BaseStep {

  private static final long serialVersionUID = -2679292079974676672L;

  /**
   * Evaluation object used for evaluating a classifier
   */
  private transient AggregateableEvaluation m_eval;

  /** Plotting instances */
  private transient Instances m_aggregatedPlotInstances = null;

  /** Sizes of points in plotting data */
  private transient ArrayList<Object> m_aggregatedPlotSizes = null;

  /** Plotting shapes */
  private transient ArrayList<Integer> m_aggregatedPlotShapes = null;

  /**
   * True if plot point sizes are to be rendered proportional to the size of the
   * prediction margin
   */
  protected boolean m_errorPlotPointSizeProportionalToMargin;

  /** True to perform cost sensitive evaluation */
  protected boolean m_costSensitiveEval;

  /** The cost matrix (string form) */
  protected String m_costString = "";

  /** The cost matrix */
  protected CostMatrix m_matrix;

  /** Evaluation metrics to output */
  protected String m_selectedEvalMetrics = "";

  /** Holds a list of metric names */
  protected List<String> m_metricsList = new ArrayList<String>();

  /** True if the step has been reset */
  protected boolean m_isReset;

  /** For counting down the sets left to process */
  protected AtomicInteger m_setsToGo;

  /** The maximum set number in the batch of sets being processed */
  protected int m_maxSetNum;

  protected AtomicInteger m_taskCount;

  protected void stringToList(String l) {
    if (l != null && l.length() > 0) {
      String[] parts = l.split(",");
      m_metricsList.clear();
      for (String s : parts) {
        m_metricsList.add(s.trim());
      }
    }
  }

  /**
   * Get whether the size of plot data points will be proportional to the
   * prediction margin
   *
   * @return true if plot data points will be rendered proportional to the size
   *         of the prediction margin
   */
  @OptionMetadata(displayName = "Error plot point size proportional to margin",
    description = "Set the point size proportional to the prediction "
      + "margin for classification error plots")
  public boolean getErrorPlotPointSizeProportionalToMargin() {
    return m_errorPlotPointSizeProportionalToMargin;
  }

  /**
   * Set whether the size of plot data points will be proportional to the
   * prediction margin
   *
   * @param e true if plot data points will be rendered proportional to the size
   *          of the prediction margin
   */
  public void setErrorPlotPointSizeProportionalToMargin(boolean e) {
    m_errorPlotPointSizeProportionalToMargin = e;
  }

  /**
   * Get the evaluation metrics to output (as a comma-separated list).
   *
   * @return the evaluation metrics to output
   */
  @ProgrammaticProperty
  public String getEvaluationMetricsToOutput() {
    return m_selectedEvalMetrics;
  }

  /**
   * Set the evaluation metrics to output (as a comma-separated list).
   *
   * @param m the evaluation metrics to output
   */
  public void setEvaluationMetricsToOutput(String m) {
    m_selectedEvalMetrics = m;
    stringToList(m);
  }

  /**
   * Set whether to evaluate with respoect to costs
   *
   * @param useCosts true to use cost-sensitive evaluation
   */
  @ProgrammaticProperty
  public void setEvaluateWithRespectToCosts(boolean useCosts) {
    m_costSensitiveEval = useCosts;
  }

  /**
   * Get whether to evaluate with respoect to costs
   *
   * @return true to use cost-sensitive evaluation
   */
  public boolean getEvaluateWithRespectToCosts() {
    return m_costSensitiveEval;
  }

  /**
   * Set the cost matrix to use as a string
   *
   * @param cms the cost matrix to use
   */
  @ProgrammaticProperty
  public void setCostMatrixString(String cms) {
    m_costString = cms;
  }

  /**
   * Get the cost matrix to use as a string
   *
   * @return the cost matrix
   */
  public String getCostMatrixString() {
    return m_costString;
  }

  /**
   * Get a list of incoming connection types that this step can accept. Ideally
   * (and if appropriate), this should take into account the state of the step
   * and any existing incoming connections. E.g. a step might be able to accept
   * one (and only one) incoming batch data connection.
   *
   * @return a list of incoming connections that this step can accept given its
   *         current state
   */
  @Override
  public List<String> getIncomingConnectionTypes() {
    List<String> result = new ArrayList<String>();

    if (getStepManager().numIncomingConnectionsOfType(
      StepManager.CON_BATCH_CLASSIFIER) == 0) {
      result.add(StepManager.CON_BATCH_CLASSIFIER);
    }
    return result;
  }

  /**
   * Get a list of outgoing connection types that this step can produce. Ideally
   * (and if appropriate), this should take into account the state of the step
   * and the incoming connections. E.g. depending on what incoming connection is
   * present, a step might be able to produce a trainingSet output, a testSet
   * output or neither, but not both.
   *
   * @return a list of outgoing connections that this step can produce
   */
  @Override
  public List<String> getOutgoingConnectionTypes() {
    List<String> result = new ArrayList<String>();
    if (getStepManager().numIncomingConnections() > 0) {
      result.add(StepManager.CON_TEXT);
      result.add(StepManager.CON_THRESHOLD_DATA);
      result.add(StepManager.CON_VISUALIZABLE_ERROR);
    }

    return result;
  }

  /**
   * Constructor
   */
  public ClassifierPerformanceEvaluator() {
    super();
    m_metricsList = Evaluation.getAllEvaluationMetricNames();
    m_metricsList.remove("Coverage");
    m_metricsList.remove("Region size");
    StringBuilder b = new StringBuilder();
    for (String s : m_metricsList) {
      b.append(s).append(",");
    }
    m_selectedEvalMetrics = b.substring(0, b.length() - 1);
  }

  @Override
  public void stepInit() throws WekaException {
    m_isReset = true;
    m_PlotInstances = null;
    m_aggregatedPlotInstances = null;
    m_taskCount = new AtomicInteger(0);
    if (m_costSensitiveEval && m_costString != null
      && m_costString.length() > 0) {
      try {
        m_matrix = CostMatrix.parseMatlab(getCostMatrixString());
      } catch (Exception e) {
        throw new WekaException(e);
      }
    }
  }

  @Override
  public void stop() {
    super.stop();

    if ((m_taskCount == null || m_taskCount.get() == 0) && isStopRequested()) {
      getStepManager().interrupted();
    }
  }

  /** for generating plottable instance with predictions appended. */
  private transient ClassifierErrorsPlotInstances m_PlotInstances = null;

  /**
   * Process an incoming data payload (if the step accepts incoming connections)
   *
   * @param data the payload to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public synchronized void processIncoming(Data data) throws WekaException {
    try {
      int setNum =
        (Integer) data.getPayloadElement(StepManager.CON_AUX_DATA_SET_NUM);
      Instances trainingData =
        (Instances) data
          .getPayloadElement(StepManager.CON_AUX_DATA_TRAININGSET);
      Instances testData =
        (Instances) data.getPayloadElement(StepManager.CON_AUX_DATA_TESTSET);

      if (testData == null || testData.numInstances() == 0) {
        // can't evaluate empty/non-existent test instances
        getStepManager().logDetailed(
          "No test set available - unable to evaluate");
        return;
      }

      weka.classifiers.Classifier classifier =
        (weka.classifiers.Classifier) data
          .getPayloadElement(StepManager.CON_BATCH_CLASSIFIER);
      String evalLabel =
        data.getPayloadElement(StepManager.CON_AUX_DATA_LABEL).toString();
      if (classifier == null) {
        throw new WekaException("Classifier is null!!");
      }

      if (m_isReset) {
        m_isReset = false;
        getStepManager().processing();

        m_maxSetNum =
          (Integer) data
            .getPayloadElement(StepManager.CON_AUX_DATA_MAX_SET_NUM);
        m_setsToGo = new AtomicInteger(0);
        if (trainingData == null) {
          // no training data to estimate majority class/mean target from
          Evaluation eval =
            new Evaluation(testData, m_costSensitiveEval ? m_matrix : null);
          m_PlotInstances = ExplorerDefaults.getClassifierErrorsPlotInstances();
          m_PlotInstances.setInstances(testData);
          m_PlotInstances.setClassifier(classifier);
          m_PlotInstances.setClassIndex(testData.classIndex());
          m_PlotInstances.setEvaluation(eval);

          eval =
            adjustForInputMappedClassifier(eval, classifier, testData,
              m_PlotInstances, m_costSensitiveEval ? m_matrix : null);
          eval.useNoPriors();
          m_eval = new AggregateableEvaluation(eval);
          m_eval.setMetricsToDisplay(m_metricsList);
        } else {
          Evaluation eval =
            new Evaluation(trainingData, m_costSensitiveEval ? m_matrix : null);
          m_PlotInstances = ExplorerDefaults.getClassifierErrorsPlotInstances();
          m_PlotInstances.setInstances(trainingData);
          m_PlotInstances.setClassifier(classifier);
          m_PlotInstances.setClassIndex(trainingData.classIndex());
          m_PlotInstances.setEvaluation(eval);

          eval =
            adjustForInputMappedClassifier(eval, classifier, trainingData,
              m_PlotInstances, m_costSensitiveEval ? m_matrix : null);
          m_eval = new AggregateableEvaluation(eval);
          m_eval.setMetricsToDisplay(m_metricsList);
        }
        m_PlotInstances.setUp();
        m_aggregatedPlotInstances = null;
      }

      if (!isStopRequested()) {
        getStepManager().logBasic(
          "Scheduling evaluation of fold/set " + setNum + " for execution");

        // submit the task
        EvaluationTask evalTask =
          new EvaluationTask(this, classifier, trainingData, testData, setNum,
            m_metricsList, getErrorPlotPointSizeProportionalToMargin(),
            evalLabel, new EvaluationCallback(), m_costSensitiveEval ? m_matrix
              : null);
        getStepManager().getExecutionEnvironment().submitTask(evalTask);
        m_taskCount.incrementAndGet();
      } else {
        getStepManager().interrupted();
      }
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Aggregates a single evaluation task into the overall evaluation
   *
   * @param eval the partial evaluation to aggregate
   * @param classifier the classifier used for evaluation
   * @param testData the test data evaluated on
   * @param plotInstances plotting instances
   * @param setNum the set number processed
   * @param evalLabel evaluation type
   * @throws Exception if a problem occurs
   */
  protected synchronized void aggregateEvalTask(Evaluation eval,
    weka.classifiers.Classifier classifier, Instances testData,
    ClassifierErrorsPlotInstances plotInstances, int setNum, String evalLabel)
    throws Exception {

    m_eval.aggregate(eval);

    if (m_aggregatedPlotInstances == null) {
      // get these first so that the post-processing does not scale the sizes!!
      m_aggregatedPlotShapes =
        (ArrayList<Integer>) plotInstances.getPlotShapes().clone();
      m_aggregatedPlotSizes =
        (ArrayList<Object>) plotInstances.getPlotSizes().clone();

      // this calls the post-processing, so do this last
      m_aggregatedPlotInstances =
        new Instances(plotInstances.getPlotInstances());
    } else {
      // get these first so that post-processing does not scale sizes
      ArrayList<Object> tmpSizes =
        (ArrayList<Object>) plotInstances.getPlotSizes().clone();
      ArrayList<Integer> tmpShapes =
        (ArrayList<Integer>) plotInstances.getPlotShapes().clone();

      Instances temp = plotInstances.getPlotInstances();
      for (int i = 0; i < temp.numInstances(); i++) {
        m_aggregatedPlotInstances.add(temp.get(i));
        m_aggregatedPlotShapes.add(tmpShapes.get(i));
        m_aggregatedPlotSizes.add(tmpSizes.get(i));
      }
    }

    getStepManager().statusMessage(
      "Completed folds/sets " + m_setsToGo.incrementAndGet());

    if (m_setsToGo.get() == m_maxSetNum) {
      AggregateableClassifierErrorsPlotInstances aggPlot =
        new AggregateableClassifierErrorsPlotInstances();
      aggPlot.setInstances(testData);
      aggPlot.setPlotInstances(m_aggregatedPlotInstances);
      aggPlot.setPlotShapes(m_aggregatedPlotShapes);
      aggPlot.setPlotSizes(m_aggregatedPlotSizes);
      aggPlot
        .setPointSizeProportionalToMargin(m_errorPlotPointSizeProportionalToMargin);

      // triggers scaling of shape sizes
      aggPlot.getPlotInstances();

      String textTitle = "";
      textTitle += classifier.getClass().getName();
      String textOptions = "";
      if (classifier instanceof OptionHandler) {
        textOptions =
          Utils.joinOptions(((OptionHandler) classifier).getOptions());
      }
      textTitle =
        textTitle.substring(textTitle.lastIndexOf('.') + 1, textTitle.length());
      if (evalLabel != null && evalLabel.length() > 0) {
        if (!textTitle.toLowerCase().startsWith(evalLabel.toLowerCase())) {
          textTitle = evalLabel + " : " + textTitle;
        }
      }

      CostMatrix cm =
        m_costSensitiveEval ? CostMatrix.parseMatlab(getCostMatrixString())
          : null;
      String resultT =
        "=== Evaluation result ===\n\n"
          + "Scheme: "
          + textTitle
          + "\n"
          + ((textOptions.length() > 0) ? "Options: " + textOptions + "\n" : "")
          + "Relation: " + testData.relationName() + "\n\n"
          + (cm != null ? "Cost matrix:\n" + cm.toString() + "\n" : "")
          + m_eval.toSummaryString();

      if (testData.classAttribute().isNominal()) {
        resultT +=
          "\n" + m_eval.toClassDetailsString() + "\n" + m_eval.toMatrixString();
      }

      Data text = new Data(StepManager.CON_TEXT);
      text.setPayloadElement(StepManager.CON_TEXT, resultT);
      text.setPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE, textTitle);
      getStepManager().outputData(text);

      // set up visualizable errors
      if (getStepManager().numOutgoingConnectionsOfType(
        StepManager.CON_VISUALIZABLE_ERROR) > 0) {
        PlotData2D errorD = new PlotData2D(m_aggregatedPlotInstances);
        errorD.setShapeSize(m_aggregatedPlotSizes);
        errorD.setShapeType(m_aggregatedPlotShapes);
        errorD.setPlotName(textTitle + " " + textOptions);

        Data visErr = new Data(StepManager.CON_VISUALIZABLE_ERROR);
        visErr.setPayloadElement(StepManager.CON_VISUALIZABLE_ERROR, errorD);
        getStepManager().outputData(visErr);
      }

      // threshold data
      if (testData.classAttribute().isNominal()
        && getStepManager().numOutgoingConnectionsOfType(
          StepManager.CON_THRESHOLD_DATA) > 0) {
        ThresholdCurve tc = new ThresholdCurve();
        Instances result = tc.getCurve(m_eval.predictions(), 0);
        result.setRelationName(testData.relationName());
        PlotData2D pd = new PlotData2D(result);
        String htmlTitle = "<html><font size=-2>" + textTitle;
        String newOptions = "";
        if (classifier instanceof OptionHandler) {
          String[] options = ((OptionHandler) classifier).getOptions();
          if (options.length > 0) {
            for (int ii = 0; ii < options.length; ii++) {
              if (options[ii].length() == 0) {
                continue;
              }
              if (options[ii].charAt(0) == '-'
                && !(options[ii].charAt(1) >= '0' && options[ii].charAt(1) <= '9')) {
                newOptions += "<br>";
              }
              newOptions += options[ii];
            }
          }
        }
        htmlTitle +=
          " " + newOptions + "<br>" + " (class: "
            + testData.classAttribute().value(0) + ")" + "</font></html>";
        pd.setPlotName(textTitle + " (class: "
          + testData.classAttribute().value(0) + ")");
        pd.setPlotNameHTML(htmlTitle);
        boolean[] connectPoints = new boolean[result.numInstances()];
        for (int jj = 1; jj < connectPoints.length; jj++) {
          connectPoints[jj] = true;
        }

        pd.setConnectPoints(connectPoints);
        Data threshData = new Data(StepManager.CON_THRESHOLD_DATA);
        threshData.setPayloadElement(StepManager.CON_THRESHOLD_DATA, pd);
        threshData.setPayloadElement(StepManager.CON_AUX_DATA_CLASS_ATTRIBUTE,
          testData.classAttribute());
        getStepManager().outputData(threshData);
      }
      getStepManager().finished();
    }
    if (isStopRequested()) {
      getStepManager().interrupted();
    }
  }

  /**
   * Return the fully qualified name of a custom editor component (JComponent)
   * to use for editing the properties of the step. This method can return null,
   * in which case the system will dynamically generate an editor using the
   * GenericObjectEditor
   *
   * @return the fully qualified name of a step editor component
   */
  @Override
  public String getCustomEditorForStep() {
    return "weka.gui.knowledgeflow.steps.ClassifierPerformanceEvaluatorStepEditorDialog";
  }

  /**
   * Adjust evaluation configuration if an {@code InputMappedClassifier} is
   * being used
   *
   * @param eval the evaluation object ot adjust
   * @param classifier the classifier being used
   * @param inst the instances being evaluated on
   * @param plotInstances plotting instances
   * @param matrix the CostMatrix to use, or null for no cost-sensitive
   *          evaluation
   * @return the adjusted {@code Evaluation} object
   * @throws Exception if a problem occurs
   */
  protected static Evaluation adjustForInputMappedClassifier(Evaluation eval,
    weka.classifiers.Classifier classifier, Instances inst,
    ClassifierErrorsPlotInstances plotInstances, CostMatrix matrix)
    throws Exception {

    if (classifier instanceof weka.classifiers.misc.InputMappedClassifier) {
      Instances mappedClassifierHeader =
        ((weka.classifiers.misc.InputMappedClassifier) classifier)
          .getModelHeader(new Instances(inst, 0));

      eval = new Evaluation(new Instances(mappedClassifierHeader, 0));

      if (!eval.getHeader().equalHeaders(inst)) {
        // When the InputMappedClassifier is loading a model,
        // we need to make a new dataset that maps the test instances to
        // the structure expected by the mapped classifier - this is only
        // to ensure that the ClassifierPlotInstances object is configured
        // in accordance with what the embeded classifier was trained with
        Instances mappedClassifierDataset =
          ((weka.classifiers.misc.InputMappedClassifier) classifier)
            .getModelHeader(new Instances(mappedClassifierHeader, 0));
        for (int zz = 0; zz < inst.numInstances(); zz++) {
          Instance mapped =
            ((weka.classifiers.misc.InputMappedClassifier) classifier)
              .constructMappedInstance(inst.instance(zz));
          mappedClassifierDataset.add(mapped);
        }

        eval.setPriors(mappedClassifierDataset);
        plotInstances.setInstances(mappedClassifierDataset);
        plotInstances.setClassifier(classifier);
        plotInstances.setClassIndex(mappedClassifierDataset.classIndex());
        plotInstances.setEvaluation(eval);
      }
    }

    return eval;
  }

  /**
   * Subclass of ClassifierErrorsPlotInstances to allow plot point sizes to be
   * scaled according to global min/max values.
   *
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  protected static class AggregateableClassifierErrorsPlotInstances extends
    ClassifierErrorsPlotInstances {

    /**
     * For serialization
     */
    private static final long serialVersionUID = 2012744784036684168L;

    /**
     * Set the vector of plot shapes to use;
     *
     * @param plotShapes
     */
    @Override
    public void setPlotShapes(ArrayList<Integer> plotShapes) {
      m_PlotShapes = plotShapes;
    }

    /**
     * Set the vector of plot sizes to use
     *
     * @param plotSizes the plot sizes to use
     */
    @Override
    public void setPlotSizes(ArrayList<Object> plotSizes) {
      m_PlotSizes = plotSizes;
    }

    public void setPlotInstances(Instances inst) {
      m_PlotInstances = inst;
    }

    @Override
    protected void finishUp() {
      m_FinishUpCalled = true;

      if (!m_SaveForVisualization) {
        return;
      }

      if (m_Instances.classAttribute().isNumeric()
        || m_pointSizeProportionalToMargin) {
        scaleNumericPredictions();
      }
    }
  }

  /**
   * Class that performs the actual evaluation of a set/fold
   */
  protected static class EvaluationTask extends StepTask<Object[]> {

    private static final long serialVersionUID = -686972773536075889L;

    protected weka.classifiers.Classifier m_classifier;
    protected CostMatrix m_cMatrix;
    protected Instances m_trainData;
    protected Instances m_testData;
    protected int m_setNum;
    protected List<String> m_metricsList;
    protected boolean m_errPlotPtSizePropToMarg;
    protected String m_evalLabel;
    protected String m_classifierDesc = "";

    public EvaluationTask(Step source, weka.classifiers.Classifier classifier,
      Instances trainData, Instances testData, int setNum,
      List<String> metricsList, boolean errPlotPtSizePropToMarg,
      String evalLabel, EvaluationCallback callback, CostMatrix matrix) {

      super(source, callback);
      m_classifier = classifier;
      m_cMatrix = matrix;
      m_trainData = trainData;
      m_testData = testData;
      m_setNum = setNum;
      m_metricsList = metricsList;
      m_errPlotPtSizePropToMarg = errPlotPtSizePropToMarg;
      m_evalLabel = evalLabel;

      m_classifierDesc = m_classifier.getClass().getCanonicalName();
      m_classifierDesc =
        m_classifierDesc.substring(m_classifierDesc.lastIndexOf(".") + 1);
      if (m_classifier instanceof OptionHandler) {
        String optsString =
          Utils.joinOptions(((OptionHandler) m_classifier).getOptions());
        m_classifierDesc += " " + optsString;
      }
    }

    @Override
    public void process() throws Exception {
      Object[] r = new Object[6];
      r[4] = m_setNum;
      getExecutionResult().setResult(r);

      getLogHandler().statusMessage(
        "Evaluating " + m_classifierDesc + " on fold/set " + m_setNum);
      getLogHandler().logDetailed(
        "Evaluating " + m_classifierDesc + " on " + m_testData.relationName()
          + " fold/set " + m_setNum);
      ClassifierErrorsPlotInstances plotInstances =
        ExplorerDefaults.getClassifierErrorsPlotInstances();
      Evaluation eval = null;

      if (m_trainData == null) {
        eval = new Evaluation(m_testData, m_cMatrix);
        plotInstances.setInstances(m_testData);
        plotInstances.setClassifier(m_classifier);
        plotInstances.setClassIndex(m_testData.classIndex());
        plotInstances.setEvaluation(eval);
        plotInstances
          .setPointSizeProportionalToMargin(m_errPlotPtSizePropToMarg);
        eval =
          adjustForInputMappedClassifier(eval, m_classifier, m_testData,
            plotInstances, m_cMatrix);

        eval.useNoPriors();
        eval.setMetricsToDisplay(m_metricsList);
      } else {
        eval = new Evaluation(m_trainData, m_cMatrix);
        plotInstances.setInstances(m_trainData);
        plotInstances.setClassifier(m_classifier);
        plotInstances.setClassIndex(m_trainData.classIndex());
        plotInstances.setEvaluation(eval);
        plotInstances
          .setPointSizeProportionalToMargin(m_errPlotPtSizePropToMarg);
        eval =
          adjustForInputMappedClassifier(eval, m_classifier, m_trainData,
            plotInstances, m_cMatrix);
        eval.setMetricsToDisplay(m_metricsList);
      }

      plotInstances.setUp();
      if (m_classifier instanceof BatchPredictor
        && ((BatchPredictor) m_classifier)
          .implementsMoreEfficientBatchPrediction()) {
        double[][] predictions =
          ((BatchPredictor) m_classifier).distributionsForInstances(m_testData);
        plotInstances.process(m_testData, predictions, eval);
      } else {
        for (int i = 0; i < m_testData.numInstances(); i++) {
          Instance temp = m_testData.instance(i);
          plotInstances.process(temp, m_classifier, eval);
        }
      }

      r[0] = eval;
      r[1] = m_classifier;
      r[2] = m_testData;
      r[3] = plotInstances;
      r[5] = m_evalLabel;
    }
  }

  /**
   * Callback that gets notified when an evaluation task completes. Passes on
   * the partial evaluation results to be aggregated with the overall results
   */
  protected class EvaluationCallback implements StepTaskCallback<Object[]> {

    @Override
    public void taskFinished(ExecutionResult<Object[]> result) throws Exception {

      if (!isStopRequested()) {
        Evaluation eval = (Evaluation) result.getResult()[0];
        weka.classifiers.Classifier classifier =
          (weka.classifiers.Classifier) result.getResult()[1];
        Instances testData = (Instances) result.getResult()[2];
        ClassifierErrorsPlotInstances plotInstances =
          (ClassifierErrorsPlotInstances) result.getResult()[3];
        int setNum = (Integer) result.getResult()[4];
        String evalLabel = result.getResult()[5].toString();

        aggregateEvalTask(eval, classifier, testData, plotInstances, setNum,
          evalLabel);
      } else {
        getStepManager().interrupted();
      }
      m_taskCount.decrementAndGet();
    }

    @Override
    public void taskFailed(StepTask<Object[]> failedTask,
      ExecutionResult<Object[]> failedResult) throws Exception {
      Integer setNum = (Integer) failedResult.getResult()[4];
      getStepManager().logError("Evaluation for fold " + setNum + " failed",
        failedResult.getError());
      m_taskCount.decrementAndGet();
    }
  }
}
