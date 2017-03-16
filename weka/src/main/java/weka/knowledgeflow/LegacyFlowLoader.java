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
 *    LegacyFlowLoader.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow;

import weka.core.WekaException;
import weka.core.WekaPackageClassLoaderManager;
import weka.gui.Logger;
import weka.gui.beans.BeanCommon;
import weka.gui.beans.BeanConnection;
import weka.gui.beans.BeanInstance;
import weka.core.PluginManager;
import weka.gui.beans.WekaWrapper;
import weka.gui.beans.xml.XMLBeans;
import weka.knowledgeflow.steps.ClassAssigner;
import weka.knowledgeflow.steps.ClassValuePicker;
import weka.knowledgeflow.steps.Classifier;
import weka.knowledgeflow.steps.ClassifierPerformanceEvaluator;
import weka.knowledgeflow.steps.CrossValidationFoldMaker;
import weka.knowledgeflow.steps.DataVisualizer;
import weka.knowledgeflow.steps.FlowByExpression;
import weka.knowledgeflow.steps.ImageSaver;
import weka.knowledgeflow.steps.IncrementalClassifierEvaluator;
import weka.knowledgeflow.steps.Join;
import weka.knowledgeflow.steps.ModelPerformanceChart;
import weka.knowledgeflow.steps.Note;
import weka.knowledgeflow.steps.PredictionAppender;
import weka.knowledgeflow.steps.Saver;
import weka.knowledgeflow.steps.SerializedModelSaver;
import weka.knowledgeflow.steps.Sorter;
import weka.knowledgeflow.steps.Step;
import weka.knowledgeflow.steps.StripChart;
import weka.knowledgeflow.steps.SubstringLabeler;
import weka.knowledgeflow.steps.SubstringReplacer;
import weka.knowledgeflow.steps.TextSaver;
import weka.knowledgeflow.steps.TrainTestSplitMaker;
import weka.knowledgeflow.steps.WekaAlgorithmWrapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.Vector;

/**
 * Flow loader that reads legacy .kfml files and translates them to the new
 * implementation.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class LegacyFlowLoader implements FlowLoader {

  /** File extension for the format handled by this flow loader */
  public static final String EXTENSION = "kfml";

  /** Path to the steps.props file */
  protected static final String STEP_LIST_PROPS =
    "weka/knowledgeflow/steps/steps.props";

  static {
    // Add the built-in steps to the plugin manager
    try {
      PluginManager.addFromProperties(LegacyFlowLoader.class.getClassLoader()
        .getResourceAsStream(STEP_LIST_PROPS), true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /** Legacy beans loaded */
  protected Vector<Object> m_beans;

  /** Legacy bean connections loaded */
  protected Vector<BeanConnection> m_connections;

  /** Log to use */
  protected LogManager m_log;

  /**
   * Constructor
   */
  public LegacyFlowLoader() {
  }

  /**
   * Set the log to use
   * 
   * @param log log to use
   */
  @Override
  public void setLog(Logger log) {
    m_log = new LogManager(log, false);
  }

  /**
   * Get the flow file extension of the file format handled by this flow loader
   * 
   * @return the file extension
   */
  @Override
  public String getFlowFileExtension() {
    return EXTENSION;
  }

  /**
   * Get the description of the file format handled by this flow loader
   *
   * @return the description of the file format
   */
  @Override
  public String getFlowFileExtensionDescription() {
    return "Legacy XML-based Knowledge Flow configuration files";
  }

  /**
   * Deserialize a legacy flow from the supplied file
   *
   * @param flowFile the file to load from
   * @return the legacy flow translated to a new {@code Flow} object
   * @throws WekaException if a problem occurs
   */
  @Override
  public Flow readFlow(File flowFile) throws WekaException {
    try {
      loadLegacy(new BufferedReader(new FileReader(flowFile)));
    } catch (Exception ex) {
      throw new WekaException(ex);
    }

    String name = flowFile.getName();
    name = name.substring(0, name.lastIndexOf('.'));
    return makeFlow(name);
  }

  /**
   * Deserialize a legacy flow from the supplied input stream
   *
   * @param is the input stream to load from
   * @return the legacy flow translated to a new {@code Flow} object
   * @throws WekaException if a problem occurs
   */
  @Override
  public Flow readFlow(InputStream is) throws WekaException {
    loadLegacy(new InputStreamReader(is));

    return makeFlow("Untitled");
  }

  /**
   * Deserialize a legacy flow from the supplied reader
   *
   * @param r the reader to load from
   * @return the legacy flow translated to a new {@code Flow} object
   * @throws WekaException if a problem occurs
   */
  @Override
  public Flow readFlow(Reader r) throws WekaException {
    loadLegacy(r);

    return makeFlow("Untitled");
  }

  /**
   * Makes a new {@code Flow} by translating the legacy beans and connections to
   * the new Step implementations
   *
   * @param name the name to use for the flow
   * @return a {@code Flow} object
   * @throws WekaException if a problem occurs
   */
  protected Flow makeFlow(String name) throws WekaException {
    Flow flow = new Flow();
    flow.setFlowName(name != null ? name : "Untitled");

    // process beans first
    for (Object o : m_beans) {
      BeanInstance bean = (BeanInstance) o;
      StepManagerImpl newManager = handleStep(bean);
      if (newManager != null) {
        flow.addStep(newManager);
      }
    }

    // now connections
    for (BeanConnection conn : m_connections) {
      handleConnection(flow, conn);
    }

    return flow;
  }

  /**
   * Handles a legacy {@BeanConnection}
   *
   * @param flow the new {@code Flow} to add the translated connection to
   * @param conn the legacy bean connection to process
   */
  protected void handleConnection(Flow flow, BeanConnection conn) {

    BeanInstance source = conn.getSource();
    BeanInstance target = conn.getTarget();
    if (!(source.getBean() instanceof BeanCommon)
      && !(target.getBean() instanceof BeanCommon)) {
      return;
    }
    BeanCommon sourceC = (BeanCommon) source.getBean();
    BeanCommon targetC = (BeanCommon) target.getBean();

    StepManagerImpl sourceNew = flow.findStep(sourceC.getCustomName());
    StepManagerImpl targetNew = flow.findStep(targetC.getCustomName());
    if (sourceNew == null || targetNew == null) {
      if (m_log != null) {
        m_log
          .logWarning("Unable to make connection in new flow between legacy "
            + "steps " + sourceC.getCustomName() + " and "
            + targetC.getCustomName() + " for connection '"
            + conn.getEventName());
      }
      return;
    }

    String evntName = conn.getEventName();

    flow.connectSteps(sourceNew, targetNew, evntName, true);
  }

  /**
   * Handles a legacy {@BeanInstance} step component
   * 
   * @param bean the bean instance to process
   * @return a configured {@code StepManagerImpl} that is the equivalent of the
   *         legacy bean
   * @throws WekaException if a problem occurs
   */
  protected StepManagerImpl handleStep(BeanInstance bean) throws WekaException {

    Object comp = bean.getBean();
    String name = "";
    if (comp instanceof BeanCommon) {
      BeanCommon beanCommon = (BeanCommon) comp;
      name = beanCommon.getCustomName();
    }
    int x = bean.getX();
    int y = bean.getY();

    Step match = findStepMatch(comp.getClass().getCanonicalName());
    if (match != null) {
      StepManagerImpl manager = new StepManagerImpl(match);
      manager.m_x = x;
      manager.m_y = y;

      // copy settings...
      if (!(comp instanceof WekaWrapper)) {
        copySettingsNonWrapper(comp, match);
      } else {
        copySettingsWrapper((WekaWrapper) comp, (WekaAlgorithmWrapper) match);
      }

      if (!(match instanceof Note)) {
        match.setName(name);
      }

      return manager;
    } else {
      if (m_log != null) {
        m_log.logWarning("Unable to find an equivalent for legacy step: "
          + comp.getClass().getCanonicalName());
      }
    }

    return null;
  }

  /**
   * Copy settings from a legacy {@code WekaWrapper} bean to the new
   * {@code WekaAlgorithmWrapper} equivalent
   *
   * @param legacy the legacy wrapper
   * @param current the new step equivalent
   * @throws WekaException if a problem occurs
   */
  protected void copySettingsWrapper(WekaWrapper legacy,
    WekaAlgorithmWrapper current) throws WekaException {

    Object wrappedAlgo = legacy.getWrappedAlgorithm();
    current.setWrappedAlgorithm(wrappedAlgo);

    if (legacy instanceof weka.gui.beans.Classifier
      && current instanceof Classifier) {
      ((Classifier) current).setLoadClassifierFileName(new File(
        ((weka.gui.beans.Classifier) legacy).getLoadClassifierFileName()));
      ((Classifier) current)
        .setUpdateIncrementalClassifier(((weka.gui.beans.Classifier) legacy)
          .getUpdateIncrementalClassifier());
      ((Classifier) current)
        .setResetIncrementalClassifier(((weka.gui.beans.Classifier) legacy)
          .getResetIncrementalClassifier());
    } else if (legacy instanceof weka.gui.beans.Saver
      && current instanceof Saver) {
      (((Saver) current))
        .setRelationNameForFilename((((weka.gui.beans.Saver) legacy)
          .getRelationNameForFilename()));
    }
  }

  /**
   * Copy the settings from a legacy non-algorithm wrapper bean to the new step
   * equivalent
   *
   * @param legacy the legacy bean to copy settings from
   * @param current the new {@code Step} to copy to
   * @throws WekaException if a problem occurs
   */
  protected void copySettingsNonWrapper(Object legacy, Step current)
    throws WekaException {

    if (current instanceof Note && legacy instanceof weka.gui.beans.Note) {
      ((Note) current)
        .setNoteText(((weka.gui.beans.Note) legacy).getNoteText());
    } else if (current instanceof TrainTestSplitMaker
      && legacy instanceof weka.gui.beans.TrainTestSplitMaker) {
      ((TrainTestSplitMaker) current).setSeed(""
        + ((weka.gui.beans.TrainTestSplitMaker) legacy).getSeed());
      ((TrainTestSplitMaker) current).setTrainPercent(""
        + ((weka.gui.beans.TrainTestSplitMaker) legacy).getTrainPercent());
    } else if (current instanceof CrossValidationFoldMaker
      && legacy instanceof weka.gui.beans.CrossValidationFoldMaker) {
      ((CrossValidationFoldMaker) current).setSeed(""
        + ((weka.gui.beans.CrossValidationFoldMaker) legacy).getSeed());
      ((CrossValidationFoldMaker) current).setNumFolds(""
        + ((weka.gui.beans.CrossValidationFoldMaker) legacy).getFolds());
      ((CrossValidationFoldMaker) current)
        .setPreserveOrder(((weka.gui.beans.CrossValidationFoldMaker) legacy)
          .getPreserveOrder());
    } else if (current instanceof ClassAssigner
      && legacy instanceof weka.gui.beans.ClassAssigner) {
      ((ClassAssigner) current)
        .setClassColumn(((weka.gui.beans.ClassAssigner) legacy)
          .getClassColumn());
    } else if (current instanceof ClassValuePicker
      && legacy instanceof weka.gui.beans.ClassValuePicker) {
      ((ClassValuePicker) current)
        .setClassValue(((weka.gui.beans.ClassValuePicker) legacy)
          .getClassValue());
    } else if (current instanceof ClassifierPerformanceEvaluator
      && legacy instanceof weka.gui.beans.ClassifierPerformanceEvaluator) {
      ((ClassifierPerformanceEvaluator) current)
        .setEvaluationMetricsToOutput(((weka.gui.beans.ClassifierPerformanceEvaluator) legacy)
          .getEvaluationMetricsToOutput());
      ((ClassifierPerformanceEvaluator) current)
        .setErrorPlotPointSizeProportionalToMargin(((weka.gui.beans.ClassifierPerformanceEvaluator) legacy)
          .getErrorPlotPointSizeProportionalToMargin());
    } else if (current instanceof IncrementalClassifierEvaluator
      && legacy instanceof weka.gui.beans.IncrementalClassifierEvaluator) {
      ((IncrementalClassifierEvaluator) current)
        .setChartingEvalWindowSize(((weka.gui.beans.IncrementalClassifierEvaluator) legacy)
          .getChartingEvalWindowSize());
      ((IncrementalClassifierEvaluator) current)
        .setOutputPerClassInfoRetrievalStats(((weka.gui.beans.IncrementalClassifierEvaluator) legacy)
          .getOutputPerClassInfoRetrievalStats());
      ((IncrementalClassifierEvaluator) current)
        .setStatusFrequency(((weka.gui.beans.IncrementalClassifierEvaluator) legacy)
          .getStatusFrequency());
    } else if (current instanceof PredictionAppender
      && legacy instanceof weka.gui.beans.PredictionAppender) {
      ((PredictionAppender) current)
        .setAppendProbabilities(((weka.gui.beans.PredictionAppender) legacy)
          .getAppendPredictedProbabilities());
    } else if (current instanceof SerializedModelSaver
      && legacy instanceof weka.gui.beans.SerializedModelSaver) {
      ((SerializedModelSaver) current)
        .setFilenamePrefix(((weka.gui.beans.SerializedModelSaver) legacy)
          .getPrefix());
      ((SerializedModelSaver) current)
        .setIncludeRelationNameInFilename(((weka.gui.beans.SerializedModelSaver) legacy)
          .getIncludeRelationName());
      ((SerializedModelSaver) current)
        .setOutputDirectory(((weka.gui.beans.SerializedModelSaver) legacy)
          .getDirectory());
      ((SerializedModelSaver) current)
        .setIncrementalSaveSchedule(((weka.gui.beans.SerializedModelSaver) legacy)
          .getIncrementalSaveSchedule());
    } else if (current instanceof ImageSaver
      && legacy instanceof weka.gui.beans.ImageSaver) {
      ((ImageSaver) current).setFile(new File(
        ((weka.gui.beans.ImageSaver) legacy).getFilename()));
    } else if (current instanceof TextSaver
      && legacy instanceof weka.gui.beans.TextSaver) {
      ((TextSaver) current).setFile(new File(
        ((weka.gui.beans.TextSaver) legacy).getFilename()));
      ((TextSaver) current).setAppend(((weka.gui.beans.TextSaver) legacy)
        .getAppend());
    } else if (current instanceof StripChart
      && legacy instanceof weka.gui.beans.StripChart) {
      ((StripChart) current)
        .setRefreshFreq(((weka.gui.beans.StripChart) legacy).getRefreshFreq());
      ((StripChart) current)
        .setRefreshWidth(((weka.gui.beans.StripChart) legacy).getRefreshWidth());
      ((StripChart) current).setXLabelFreq(((weka.gui.beans.StripChart) legacy)
        .getXLabelFreq());
    } else if (current instanceof ModelPerformanceChart
      && legacy instanceof weka.gui.beans.ModelPerformanceChart) {
      ((ModelPerformanceChart) current)
        .setOffscreenAdditionalOpts(((weka.gui.beans.ModelPerformanceChart) legacy)
          .getOffscreenAdditionalOpts());
      ((ModelPerformanceChart) current)
        .setOffscreenRendererName(((weka.gui.beans.ModelPerformanceChart) legacy)
          .getOffscreenRendererName());
      ((ModelPerformanceChart) current)
        .setOffscreenHeight(((weka.gui.beans.ModelPerformanceChart) legacy)
          .getOffscreenHeight());
      ((ModelPerformanceChart) current)
        .setOffscreenWidth(((weka.gui.beans.ModelPerformanceChart) legacy)
          .getOffscreenWidth());
      ((ModelPerformanceChart) current)
        .setOffscreenXAxis(((weka.gui.beans.ModelPerformanceChart) legacy)
          .getOffscreenXAxis());
      ((ModelPerformanceChart) current)
        .setOffscreenYAxis(((weka.gui.beans.ModelPerformanceChart) legacy)
          .getOffscreenYAxis());
    } else if (current instanceof DataVisualizer
      && legacy instanceof weka.gui.beans.DataVisualizer) {
      ((DataVisualizer) current)
        .setOffscreenHeight(((weka.gui.beans.DataVisualizer) legacy)
          .getOffscreenHeight());
      ((DataVisualizer) current)
        .setOffscreenWidth(((weka.gui.beans.DataVisualizer) legacy)
          .getOffscreenWidth());
      ((DataVisualizer) current)
        .setOffscreenXAxis(((weka.gui.beans.DataVisualizer) legacy)
          .getOffscreenXAxis());
      ((DataVisualizer) current)
        .setOffscreenRendererName(((weka.gui.beans.DataVisualizer) legacy)
          .getOffscreenRendererName());
      ((DataVisualizer) current)
        .setOffscreenAdditionalOpts(((weka.gui.beans.DataVisualizer) legacy)
          .getOffscreenAdditionalOpts());
    } else if (current instanceof FlowByExpression
      && legacy instanceof weka.gui.beans.FlowByExpression) {
      ((FlowByExpression) current)
        .setExpressionString(((weka.gui.beans.FlowByExpression) legacy)
          .getExpressionString());
      ((FlowByExpression) current)
        .setTrueStepName(((weka.gui.beans.FlowByExpression) legacy)
          .getTrueStepName());
      ((FlowByExpression) current)
        .setFalseStepName(((weka.gui.beans.FlowByExpression) legacy)
          .getFalseStepName());
    } else if (current instanceof Join && legacy instanceof weka.gui.beans.Join) {
      ((Join) current).setKeySpec(((weka.gui.beans.Join) legacy).getKeySpec());
    } else if (current instanceof Sorter
      && legacy instanceof weka.gui.beans.Sorter) {
      ((Sorter) current).setSortDetails(((weka.gui.beans.Sorter) legacy)
        .getSortDetails());
      ((Sorter) current).setBufferSize(((weka.gui.beans.Sorter) legacy)
        .getBufferSize());
      ((Sorter) current).setTempDirectory(new File(
        ((weka.gui.beans.Sorter) legacy).getTempDirectory()));
    } else if (current instanceof SubstringReplacer
      && legacy instanceof weka.gui.beans.SubstringReplacer) {
      ((SubstringReplacer) current)
        .setMatchReplaceDetails(((weka.gui.beans.SubstringReplacer) legacy)
          .getMatchReplaceDetails());
    } else if (current instanceof SubstringLabeler
      && legacy instanceof weka.gui.beans.SubstringLabeler) {
      ((SubstringLabeler) current)
        .setMatchDetails(((weka.gui.beans.SubstringLabeler) legacy)
          .getMatchDetails());
      ((SubstringLabeler) current)
        .setConsumeNonMatching(((weka.gui.beans.SubstringLabeler) legacy)
          .getConsumeNonMatching());
      ((SubstringLabeler) current)
        .setMatchAttributeName(((weka.gui.beans.SubstringLabeler) legacy)
          .getMatchAttributeName());
      ((SubstringLabeler) current)
        .setNominalBinary(((weka.gui.beans.SubstringLabeler) legacy)
          .getNominalBinary());
    } else {
      // configure plugin steps
      configurePluginStep(legacy, current);
    }
  }

  /**
   * Transfer a single setting
   *
   * @param legacy the legacy bean to transfer from
   * @param current the new step to transfer to
   * @param propName the property name of the setting
   * @param propType the type of the setting
   * @throws WekaException if a problem occurs
   */
  protected void transferSetting(Object legacy, Step current, String propName,
    Class propType) throws WekaException {
    try {
      Method getM =
        legacy.getClass().getMethod("get" + propName, new Class[] {});
      Object value = getM.invoke(legacy, new Object[] {});
      Method setM =
        current.getClass()
          .getMethod("set" + propName, new Class[] { propType });
      setM.invoke(current, new Object[] { value });
    } catch (Exception ex) {
      throw new WekaException(ex);
    }
  }

  /**
   * Handles configuration of settings in the case of plugin steps
   *
   * @param legacy the legacy bean to copy settings from
   * @param current the plugin step to copy to
   * @throws WekaException if a problem occurs
   */
  protected void configurePluginStep(Object legacy, Step current)
    throws WekaException {
    if (legacy.getClass().toString().endsWith("PythonScriptExecutor")
      && current.getClass().toString().endsWith("PythonScriptExecutor")) {

      try {
        transferSetting(legacy, current, "Debug", Boolean.TYPE);
        transferSetting(legacy, current, "PythonScript", String.class);

        Method getM =
          legacy.getClass().getDeclaredMethod("getScriptFile", new Class[] {});
        Object value = getM.invoke(legacy, new Object[] {});
        Method setM =
          current.getClass().getDeclaredMethod("setScriptFile",
            new Class[] { File.class });
        setM.invoke(current, new Object[] { new File(value.toString()) });

        transferSetting(legacy, current, "VariablesToGetFromPython",
          String.class);
      } catch (Exception ex) {
        throw new WekaException(ex);
      }
    } else if (legacy.getClass().toString().endsWith("RScriptExecutor")
      && current.getClass().toString().endsWith("RScriptExecutor")) {
      try {

        transferSetting(legacy, current, "RScript", String.class);

        Method getM =
          legacy.getClass().getDeclaredMethod("getScriptFile", new Class[] {});
        Object value = getM.invoke(legacy, new Object[] {});
        Method setM =
          current.getClass().getDeclaredMethod("setScriptFile",
            new Class[] { File.class });
        setM.invoke(current, new Object[] { new File(value.toString()) });
      } catch (Exception ex) {
        throw new WekaException(ex);
      }
    } else if (legacy.getClass().toString().endsWith("JsonFieldExtractor")
      && current.getClass().toString().endsWith("JsonFieldExtractor")) {
      try {
        transferSetting(legacy, current, "PathDetails", String.class);
      } catch (Exception ex) {
        throw new WekaException(ex);
      }
    } else if (legacy.getClass().toString().endsWith("TimeSeriesForecasting")
      && current.getClass().toString().endsWith("TimeSeriesForecasting")) {
      try {
        transferSetting(legacy, current, "EncodedForecaster", String.class);
        transferSetting(legacy, current, "NumStepsToForecast", String.class);
        transferSetting(legacy, current, "ArtificialTimeStartOffset",
          String.class);
        transferSetting(legacy, current, "RebuildForecaster", Boolean.TYPE);

        Method getM =
          legacy.getClass().getDeclaredMethod("getFilename", new Class[] {});
        Object value = getM.invoke(legacy, new Object[] {});
        Method setM =
          current.getClass().getDeclaredMethod("setFilename",
            new Class[] { File.class });
        setM.invoke(current, new Object[] { new File(value.toString()) });

        getM =
          legacy.getClass()
            .getDeclaredMethod("getSaveFilename", new Class[] {});
        value = getM.invoke(legacy, new Object[] {});
        setM =
          current.getClass().getDeclaredMethod("setSaveFilename",
            new Class[] { File.class });
        setM.invoke(current, new Object[] { new File(value.toString()) });
      } catch (Exception ex) {
        throw new WekaException(ex);
      }
    } else if (legacy.getClass().toString().endsWith("GroovyComponent")
      && current.getClass().toString().endsWith("GroovyStep")) {
      transferSetting(legacy, current, "Script", String.class);
    } else if (legacy.getClass().getSuperclass().toString()
      .endsWith("AbstractSparkJob")
      && current.getClass().getSuperclass().toString()
        .endsWith("AbstractSparkJob")) {
      transferSetting(legacy, current, "JobOptions", String.class);
    } else if (legacy.getClass().getSuperclass().toString()
      .endsWith("AbstractHadoopJob")) {
      transferSetting(legacy, current, "JobOptions", String.class);
    }
  }

  /**
   * Attempts to find a matching {@code Step} implementation for a supplied
   * legacy class name
   *
   * @param legacyFullyQualified the fully qualified class name of the legacy
   *          bean to find a match for
   * @return an instantiated {@code Step} that is equivalent to the legacy one
   * @throws WekaException if a match can't be found
   */
  protected Step findStepMatch(String legacyFullyQualified)
    throws WekaException {
    String clazzNameOnly =
      legacyFullyQualified.substring(legacyFullyQualified.lastIndexOf('.') + 1,
        legacyFullyQualified.length());

    // Note is a special case
    if (clazzNameOnly.equals("Note")) {
      return new Note();
    }

    Set<String> steps =
      PluginManager.getPluginNamesOfType(weka.knowledgeflow.steps.Step.class
        .getCanonicalName());

    Step result = null;
    if (steps != null) {
      for (String s : steps) {
        String sClazzNameOnly = s.substring(s.lastIndexOf(".") + 1);
        if (sClazzNameOnly.equals(clazzNameOnly)) {
          try {
            result =
              (Step) PluginManager.getPluginInstance(
                weka.knowledgeflow.steps.Step.class.getCanonicalName(), s);
            break;
          } catch (Exception ex) {
            throw new WekaException(ex);
          }
        }
      }
    }

    if (result == null) {
      // one more last-ditch attempt
      String lastDitch = "weka.knowledgeflow.steps." + clazzNameOnly;
      try {
        result = (Step) WekaPackageClassLoaderManager.objectForName(lastDitch);
      } catch (Exception e) {
        //
      }
    }

    return result;
  }

  /**
   * Load the legacy flow using the supplied reader
   *
   * @param r the reader to load from
   * @throws WekaException if a problem occurs
   */
  @SuppressWarnings("unchecked")
  protected void loadLegacy(Reader r) throws WekaException {
    BeanConnection.init();
    BeanInstance.init();
    try {
      XMLBeans xml = new XMLBeans(null, null, 0);
      Vector<?> v = (Vector<?>) xml.read(r);

      m_beans = (Vector<Object>) v.get(XMLBeans.INDEX_BEANINSTANCES);
      m_connections =
        (Vector<BeanConnection>) v.get(XMLBeans.INDEX_BEANCONNECTIONS);
    } catch (Exception ex) {
      throw new WekaException(ex);
    }

    /*
     * m_beans = new Vector<Object>(); m_connections = new
     * Vector<BeanConnection>();
     */
  }
}
