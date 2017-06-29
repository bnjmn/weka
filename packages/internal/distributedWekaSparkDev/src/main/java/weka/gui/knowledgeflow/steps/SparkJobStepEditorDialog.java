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
 *    SparkJobStepEditorDialog
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import distributed.core.DistributedJobConfig;
import weka.core.OptionMetadata;
import weka.core.Utils;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CorrelationMatrixMapTask;
import weka.distributed.KMeansMapTask;
import weka.distributed.WekaClassifierMapTask;
import weka.distributed.spark.SparkJob;
import weka.distributed.spark.WekaClassifierSparkJob;
import weka.gui.InteractiveTableModel;
import weka.gui.InteractiveTablePanel;
import weka.gui.ProgrammaticProperty;
import weka.gui.PropertySheetPanel;
import weka.gui.knowledgeflow.StepEditorDialog;
import weka.knowledgeflow.steps.AbstractSparkJob;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI step editor dialog for the Knowledge Flow Spark jobs
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class SparkJobStepEditorDialog extends StepEditorDialog {

  private static final long serialVersionUID = -7739811342584352927L;

  /** Underlying job */
  protected SparkJob m_job;

  /** Configuration property panel */
  protected SparkPropertyPanel m_propPanel;

  /** ARFF job specific */
  protected OptsHiddenCSVMapTask m_arffMapTask;

  /** Classifier map task for editing the classifier job */
  protected WekaClassifierMapTask m_classifierMapTask;

  /** Additionally for evaluaton job */
  protected WekaClassifierSparkJob m_tempClassifierJob;

  /** Correlation map task for editing the correlation job */
  protected CorrelationMatrixMapTask m_correlationMapTask;

  /** KMeans map task for editing the KMeans job */
  protected KMeansMapTask m_kMeansMapTask;

  /** For restoring original state */
  protected String m_optionsOrig;

  @Override
  public void layoutEditor() {
    m_job = ((AbstractSparkJob) getStepToEdit()).getUnderlyingJob();
    m_optionsOrig = ((AbstractSparkJob) getStepToEdit()).getJobOptions();

    String jobTitle = getStepToEdit().getClass().getName();
    jobTitle =
      jobTitle.substring(jobTitle.lastIndexOf(".") + 1, jobTitle.length());

    if (m_job instanceof weka.distributed.spark.ArffHeaderSparkJob) {
      addPanelForArffHeaderJob(m_job);
    } else if (m_job instanceof weka.distributed.spark.WekaClassifierSparkJob) {
      addPanelForClassifierJob(m_job);
    } else if (m_job instanceof weka.distributed.spark.WekaClassifierEvaluationSparkJob) {
      addPanelForEvaluationJob(m_job);
    } else if (m_job instanceof weka.distributed.spark.CorrelationMatrixSparkJob) {
      addPanelForCorrelationMatrixJob(m_job);
    } else if (m_job instanceof weka.distributed.spark.WekaScoringSparkJob) {
      addPanelForScoringJob(m_job);
    } else if (m_job instanceof weka.distributed.spark.RandomizedDataSparkJob) {
      addPanelForRandomizedDataChunkJob(m_job);
    } else if (m_job instanceof weka.distributed.spark.KMeansClustererSparkJob) {
      addPanelForKMeansJob(m_job);
    } else if (m_job instanceof weka.distributed.spark.CanopyClustererSparkJob) {
      // TODO
    }
  }

  /**
   * Adds a panel for the ARFF header job
   *
   * @param arffJob the ARFF job
   */
  protected void addPanelForArffHeaderJob(SparkJob arffJob) {
    // only need this if we don't have an upstream connection (where RDD
    // datasets and contexts are coming from)

    JPanel jobHolder = new JPanel();
    jobHolder.setLayout(new BorderLayout());

    PropertySheetPanel arffJobEditor = new PropertySheetPanel();
    arffJobEditor.setEnvironment(m_env);
    arffJobEditor.setTarget(arffJob);
    jobHolder.add(arffJobEditor, BorderLayout.NORTH);

    m_arffMapTask = new OptsHiddenCSVMapTask();
    try {
      m_arffMapTask.setOptions(Utils.splitOptions(m_optionsOrig));
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    PropertySheetPanel mapTaskEditor = new PropertySheetPanel();
    mapTaskEditor.setTarget(m_arffMapTask);

    mapTaskEditor.setEnvironment(m_env);
    jobHolder.add(mapTaskEditor, BorderLayout.CENTER);

    // m_configTabs.addTab(tabTitle, jobHolder);
    add(jobHolder, BorderLayout.CENTER);
  }

  /**
   * Adds a panel for editing a k-means job
   *
   * @param kmeansJob the k-means job to edit
   */
  protected void addPanelForKMeansJob(SparkJob kmeansJob) {
    JPanel jobHolder = new JPanel();

    jobHolder.setLayout(new BorderLayout());

    PropertySheetPanel clustererJobEditor = new PropertySheetPanel(true);
    clustererJobEditor.setEnvironment(m_env);
    clustererJobEditor.setTarget(kmeansJob);
    jobHolder.add(clustererJobEditor, BorderLayout.NORTH);

    m_kMeansMapTask = new KMeansMapTask();
    try {
      m_kMeansMapTask.setOptions(Utils.splitOptions(m_optionsOrig));
    } catch (Exception e) {
      e.printStackTrace();
    }
    PropertySheetPanel clustererTaskEditor = new PropertySheetPanel(true);
    clustererTaskEditor.setEnvironment(m_env);
    clustererTaskEditor.setTarget(m_kMeansMapTask);
    jobHolder.add(clustererTaskEditor, BorderLayout.CENTER);

    JScrollPane scroller = new JScrollPane(jobHolder);
    add(scroller, BorderLayout.CENTER);
  }

  /**
   * Add a panel for editing the randomly shuffle data job
   *
   * @param randomizeJob the randomize job to edit
   */
  protected void addPanelForRandomizedDataChunkJob(SparkJob randomizeJob) {
    JPanel jobHolder = new JPanel();
    jobHolder.setLayout(new BorderLayout());
    PropertySheetPanel randomizeJobEditor = new PropertySheetPanel();
    randomizeJobEditor.setEnvironment(m_env);

    randomizeJobEditor.setTarget(randomizeJob);
    jobHolder.add(randomizeJobEditor, BorderLayout.NORTH);

    JScrollPane scroller = new JScrollPane(jobHolder);
    add(scroller, BorderLayout.CENTER);
  }

  /**
   * Add a tab for editing the scoring job
   *
   * @param scoringJob the scoring job to edit
   */
  public void addPanelForScoringJob(SparkJob scoringJob) {
    JPanel jobHolder = new JPanel();
    jobHolder.setLayout(new BorderLayout());

    PropertySheetPanel scoringJobEditor = new PropertySheetPanel(false);
    scoringJobEditor.setEnvironment(m_env);
    scoringJobEditor.setTarget(scoringJob);
    jobHolder.add(scoringJobEditor, BorderLayout.NORTH);

    add(jobHolder, BorderLayout.CENTER);
  }

  /**
   * Add a panel for editing a correlation job
   *
   * @param correlationJob the correlation job to edit
   */
  public void addPanelForCorrelationMatrixJob(SparkJob correlationJob) {
    JPanel jobHolder = new JPanel();
    jobHolder.setLayout(new BorderLayout());

    PropertySheetPanel matrixJobEditor = new PropertySheetPanel(false);
    matrixJobEditor.setEnvironment(m_env);
    matrixJobEditor.setTarget(correlationJob);
    jobHolder.add(matrixJobEditor, BorderLayout.NORTH);

    m_correlationMapTask = new CorrelationMatrixMapTask();
    try {
      m_correlationMapTask.setOptions(Utils.splitOptions(m_optionsOrig));
    } catch (Exception e) {
      e.printStackTrace();
    }

    PropertySheetPanel matrixTaskEditor = new PropertySheetPanel(false);
    matrixTaskEditor.setEnvironment(m_env);
    matrixTaskEditor.setTarget(m_correlationMapTask);
    jobHolder.add(matrixTaskEditor, BorderLayout.CENTER);

    add(jobHolder, BorderLayout.CENTER);
  }

  /**
   * Adds a tab for editing an evaluation job
   *
   * @param evaluationJob the evaluation job to edit
   */
  public void addPanelForEvaluationJob(SparkJob evaluationJob) {
    JPanel jobHolder = new JPanel();
    jobHolder.setLayout(new BorderLayout());

    PropertySheetPanel evaluationJobEditor = new PropertySheetPanel(false);
    evaluationJobEditor.setEnvironment(m_env);
    evaluationJobEditor.setTarget(evaluationJob);
    jobHolder.add(evaluationJobEditor, BorderLayout.NORTH);

    m_tempClassifierJob = new weka.distributed.spark.WekaClassifierSparkJob();
    try {
      m_tempClassifierJob.setOptions(Utils.splitOptions(m_optionsOrig));
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    JPanel classifierJobP = makeClassifierJobPanel(m_tempClassifierJob, true);

    jobHolder.add(classifierJobP, BorderLayout.CENTER);
    JScrollPane scroller = new JScrollPane(jobHolder);

    add(scroller, BorderLayout.CENTER);
  }

  /**
   * Adds a panel for editing a classifier job
   *
   * @param classifierJob the classifier job to edit
   */
  protected void addPanelForClassifierJob(SparkJob classifierJob) {
    JPanel jobHolder = makeClassifierJobPanel(classifierJob, true);
    JScrollPane scroller = new JScrollPane(jobHolder);

    add(scroller, BorderLayout.CENTER);
  }

  /**
   * Makes a panel for editing a classifier job
   *
   * @param classifierJob the classifier job to edit
   * @param hideAbout true if the help info is not to be displayed
   * @return a JPanel setup for editing a classifier job
   */
  protected JPanel makeClassifierJobPanel(SparkJob classifierJob,
    boolean hideAbout) {
    JPanel jobHolder = new JPanel();
    jobHolder.setLayout(new BorderLayout());

    PropertySheetPanel classifierJobEditor = new PropertySheetPanel(!hideAbout);
    classifierJobEditor.setEnvironment(m_env);
    classifierJobEditor.setTarget(classifierJob);
    jobHolder.add(classifierJobEditor, BorderLayout.NORTH);

    m_classifierMapTask = new WekaClassifierMapTask();
    try {
      m_classifierMapTask.setOptions(Utils.splitOptions(m_optionsOrig));
    } catch (Exception e) {
      e.printStackTrace();
    }
    PropertySheetPanel classifierTaskEditor = new PropertySheetPanel();
    classifierTaskEditor.setEnvironment(m_env);
    classifierTaskEditor.setTarget(m_classifierMapTask);
    jobHolder.add(classifierTaskEditor, BorderLayout.CENTER);

    return jobHolder;
  }

  @Override
  public void okPressed() {
    if (m_job instanceof weka.distributed.spark.ArffHeaderSparkJob) {
      okARFFJob();
    } else if (m_job instanceof weka.distributed.spark.WekaClassifierSparkJob) {
      okClassifierJob();
    } else if (m_job instanceof weka.distributed.spark.WekaClassifierEvaluationSparkJob) {
      okEvaluationJob();
    } else if (m_job instanceof weka.distributed.spark.CorrelationMatrixSparkJob) {
      okCorrelationJob();
    } else if (m_job instanceof weka.distributed.spark.RandomizedDataSparkJob) {
      okRandomizeJob();
    } else if (m_job instanceof weka.distributed.spark.KMeansClustererSparkJob) {
      okKMeansJob();
    } else if (m_job instanceof weka.distributed.spark.WekaScoringSparkJob) {
      okScoringJob();
    } else if (m_job instanceof weka.distributed.spark.CanopyClustererSparkJob) {
      // TODO okCanopyJob()
    }
  }

  @Override
  public void cancelPressed() {
    ((AbstractSparkJob) getStepToEdit()).setJobOptions(m_optionsOrig);
  }

  /**
   * Actions to apply to the scoring job when closing under the "OK" condition
   */
  protected void okScoringJob() {
    List<String> opts = getBaseConfig(m_job);
    addScoringJobOptionsOnly(opts);

    applyOptionsToJob(opts);
  }

  /**
   * Actions to apply to the k-means job when closing under the "OK" condition
   */
  protected void okKMeansJob() {
    List<String> opts = getBaseConfig(m_job);
    addKMeansJobOptionsOnly(opts,
      (weka.distributed.spark.KMeansClustererSparkJob) m_job);
    addKMeansMapTaskOpts(opts);

    applyOptionsToJob(opts);
  }

  /**
   * Actions to apply to the randomize job when closing under the "OK" condition
   */
  protected void okRandomizeJob() {
    List<String> opts = getBaseConfig(m_job);
    addRandomizeJobOptionsOnly(opts,
      (weka.distributed.spark.RandomizedDataSparkJob) m_job);

    applyOptionsToJob(opts);
  }

  /**
   * Actions to apply to the ARFF job when closing under the "OK" condition
   */
  protected void okARFFJob() {
    List<String> opts = getBaseConfig(m_job);
    addArffJobOptionsOnly(opts,
      (weka.distributed.spark.ArffHeaderSparkJob) m_job);
    addArffMapTaskOpts(opts);

    applyOptionsToJob(opts);
  }

  /**
   * Actions to apply to the classifier job when closing under the "OK"
   * condition
   */
  protected void okClassifierJob() {
    List<String> opts = getBaseConfig(m_job);
    addClassifierJobOptionsOnly(opts,
      (weka.distributed.spark.WekaClassifierSparkJob) m_job);
    addClassifierMapTaskOpts(opts);

    applyOptionsToJob(opts);
  }

  /**
   * Actions to apply to the evaluation job when closing under the "OK"
   * condition
   */
  protected void okEvaluationJob() {
    List<String> opts = getBaseConfig(m_tempClassifierJob);
    addEvaluationJobOptionsOnly(opts);

    addClassifierJobOptionsOnly(opts, m_tempClassifierJob);
    addClassifierMapTaskOpts(opts);

    applyOptionsToJob(opts);
  }

  /**
   * Actions to apply to the correlation job when closing under the "OK"
   * condition
   */
  protected void okCorrelationJob() {
    List<String> opts = getBaseConfig(m_job);
    addCorrelationJobOptionsOnly(opts,
      (weka.distributed.spark.CorrelationMatrixSparkJob) m_job);
    addCorrelationMapTaskOpts(opts);

    applyOptionsToJob(opts);
  }

  /**
   * Add options from the scoring job only to the supplied list
   *
   * @param opts the list of options to add to
   */
  protected void addScoringJobOptionsOnly(List<String> opts) {
    String[] scoringOpts =
      ((weka.distributed.spark.WekaScoringSparkJob) m_job).getJobOptionsOnly();

    opts.addAll(Arrays.asList(scoringOpts));
  }

  /**
   * Add options from the k-means job only to the supplied list
   *
   * @param opts the list of options to add to
   * @param kMeansJob the classifier job to grab options from
   */
  protected void addKMeansJobOptionsOnly(List<String> opts,
    weka.distributed.spark.KMeansClustererSparkJob kMeansJob) {
    String[] clustererOpts = kMeansJob.getJobOptionsOnly();

    opts.addAll(Arrays.asList(clustererOpts));
  }

  /**
   * Adds options from the k-means map task to the supplied list
   *
   * @param opts the list of options to add to
   */
  protected void addKMeansMapTaskOpts(List<String> opts) {
    String[] clustererMapOpts = m_kMeansMapTask.getOptions();

    opts.addAll(Arrays.asList(clustererMapOpts));
  }

  /**
   * Add options from the randomize job only to the supplied list
   *
   * @param opts the list of options to add to
   */
  protected void addRandomizeJobOptionsOnly(List<String> opts,
    weka.distributed.spark.RandomizedDataSparkJob randomizeJob) {
    String[] randomizeOps = randomizeJob.getJobOptionsOnly();

    opts.addAll(Arrays.asList(randomizeOps));
  }

  /**
   * Add options from the correlation job only to the supplied list
   *
   * @param opts the list of options to add to
   */
  protected void addCorrelationJobOptionsOnly(List<String> opts,
    weka.distributed.spark.CorrelationMatrixSparkJob correlationJob) {
    String[] corrOpts = correlationJob.getJobOptionsOnly();

    opts.addAll(Arrays.asList(corrOpts));
  }

  /**
   * Add options from the correlation map task to the supplied list
   *
   * @param opts the list of options to add to
   */
  protected void addCorrelationMapTaskOpts(List<String> opts) {
    String[] corrOpts = m_correlationMapTask.getOptions();

    opts.addAll(Arrays.asList(corrOpts));
  }

  /**
   * Add options from the evaluation job only to the supplied list
   *
   * @param opts the list of options to add to
   */
  protected void addEvaluationJobOptionsOnly(List<String> opts) {
    String[] evalOpts =
      ((weka.distributed.spark.WekaClassifierEvaluationSparkJob) m_job)
        .getJobOptionsOnly();

    opts.addAll(Arrays.asList(evalOpts));
  }

  /**
   * Add options from the classifier map task to the supplied list
   *
   * @param opts the list of options to add to
   */
  protected void addClassifierMapTaskOpts(List<String> opts) {
    String[] classifierMapOpts = m_classifierMapTask.getOptions();

    opts.addAll(Arrays.asList(classifierMapOpts));
  }

  /**
   * Add options from the classifier job only to the supplied list
   *
   * @param opts the list of options to add to
   * @param classifierJob the classifier job to grab options from
   */
  protected void addClassifierJobOptionsOnly(List<String> opts,
    weka.distributed.spark.WekaClassifierSparkJob classifierJob) {
    String[] classifierOpts = classifierJob.getJobOptionsOnly();

    opts.addAll(Arrays.asList(classifierOpts));
  }

  /**
   * Apply the complete list of options to the current underlying job
   *
   * @param opts the options to apply
   */
  protected void applyOptionsToJob(List<String> opts) {
    String combined = Utils.joinOptions(opts.toArray(new String[opts.size()]));
    System.err.println("Combined: " + combined);

    ((AbstractSparkJob) getStepToEdit()).setJobOptions(combined);
  }

  /**
   * Adds options from the ARFF job only to the supplied list
   *
   * @param opts the list of options to add to
   * @param arffJob the ARFF job to grab options from
   */
  protected void addArffJobOptionsOnly(List<String> opts,
    weka.distributed.spark.ArffHeaderSparkJob arffJob) {

    String[] arffJobOpts = arffJob.getJobOptionsOnly();

    opts.addAll(Arrays.asList(arffJobOpts));
  }

  /**
   * Adds options from the ARFF map task to the supplied list of options
   *
   * @param opts the list of options to add the ARFF options to
   */
  protected void addArffMapTaskOpts(List<String> opts) {
    String[] arffMapOpts = m_arffMapTask.getOptions();

    opts.addAll(Arrays.asList(arffMapOpts));
  }

  /**
   * Gets the base options from the underlying job and stores them in a list
   *
   * @param job the job to extract base options from
   * @return a list of options
   */
  protected List<String> getBaseConfig(SparkJob job) {
    String[] baseJobOpts = job.getBaseOptionsOnly();
    // String[] mrConfigOpts = m_sjConfig.getOptions();

    List<String> opts = new ArrayList<String>();
    opts.addAll(Arrays.asList(baseJobOpts));

    return opts;
  }

  protected static class SparkPropertyPanel extends JPanel {

    private static final long serialVersionUID = -2863723701565395258L;

    /** The JTable for configuring properties */
    protected InteractiveTablePanel m_table = new InteractiveTablePanel(
      new String[] { "Property", "Value", "" });

    public SparkPropertyPanel(Map<String, String> properties) {
      setLayout(new BorderLayout());
      setBorder(BorderFactory.createTitledBorder("User defined properties"));
      add(m_table, BorderLayout.CENTER);

      // populate table with supplied properties
      if (properties != null) {
        int row = 0;
        JTable table = m_table.getTable();
        for (Map.Entry<String, String> e : properties.entrySet()) {
          String prop = e.getKey();
          String val = e.getValue();

          // make sure to skip internal weka properties!!
          if (!DistributedJobConfig.isEmpty(val) && !prop.startsWith("*")) {
            table.getModel().setValueAt(prop, row, 0);
            table.getModel().setValueAt(val, row, 1);
            ((InteractiveTableModel) table.getModel()).addEmptyRow();
            row++;
          }
        }
      }
    }

    /**
     * Get the properties being edited
     *
     * @return the map of properties being edited
     */
    public Map<String, String> getProperties() {
      Map<String, String> result = new HashMap<String, String>();
      JTable table = m_table.getTable();
      int numRows = table.getModel().getRowCount();

      for (int i = 0; i < numRows; i++) {
        String paramName = table.getValueAt(i, 0).toString();
        String paramValue = table.getValueAt(i, 1).toString();
        if (paramName.length() > 0 && paramValue.length() > 0) {
          result.put(paramName, paramValue);
        }
      }

      return result;
    }
  }

  /**
   * Helper class for hiding a few of the options in CSVToARFFHeaderMapTask that
   * are not necessary now that DataFrames are used for handling CSV files.
   * 
   * Note that the simple way of doing this would be to extend
   * CSVToARFFHeaderMapTask and override the set/get methods to be hidden,
   * adding @ProgramaticProperty annotations to hide them. This works in Java
   * 1.7, but not in 1.8 (on the Mac at least) for some reason -
   * getDeclaredAnnotations()/getAnnotations() returns an empty array for a
   * method with a annotations when that method overrides a superclass one under
   * Java 1.8. Whether this is a bug or intentional I don't know. Hence the
   * delegation approach used here.
   */
  protected static class OptsHiddenCSVMapTask {

    private static final long serialVersionUID = 8261901664035011253L;

    protected CSVToARFFHeaderMapTask m_delegate = new CSVToARFFHeaderMapTask(
      false, true);

    public void setOptions(String[] opts) throws Exception {
      m_delegate.setOptions(opts);
    }

    public String[] getOptions() {
      return m_delegate.getOptions();
    }

    @OptionMetadata(
      displayName = "Nominal attributes",
      description = "The range of attributes to force to be of type NOMINAL, example "
        + "ranges: 'first-last', '1,4,7-14,50-last'.", displayOrder = 1)
    public
      void setNominalAttributes(String value) {
      m_delegate.setNominalAttributes(value);
    }

    public String getNominalAttributes() {
      return m_delegate.getNominalAttributes();
    }

    @OptionMetadata(
      displayName = "String attributes",
      description = "The range of attributes to force to be of type STRING, example "
        + "ranges: 'first-last', '1,4,7-14,50-last'.", displayOrder = 2)
    public
      void setStringAttributes(String value) {
      m_delegate.setStringAttributes(value);
    }

    public String getStringAttributes() {
      return m_delegate.getStringAttributes();
    }

    @OptionMetadata(displayName = "Date attributes",
      description = "The range of attributes to force to type DATE, example "
        + "ranges: 'first-last', '1,4,7-14, 50-last'.", displayOrder = 3)
    public void setDateAttributes(String value) {
      m_delegate.setDateAttributes(value);
    }

    public String getDateAttributes() {
      return m_delegate.getDateAttributes();
    }

    @OptionMetadata(displayName = "Date format",
      description = "The format to use for parsing date values.",
      displayOrder = 4)
    public void setDateFormat(String format) {
      m_delegate.setDateFormat(format);
    }

    public String getDateFormat() {
      return m_delegate.getDateFormat();
    }

    /**
     * Set whether, for hitherto thought to be numeric columns, to treat any
     * unparsable values as missing value.
     *
     * @param unparsableNumericValuesToMissing
     */
    @OptionMetadata(
      displayName = "Treat unparsable numeric values as missing",
      description = "For columns identified as numeric, treat any values that can't"
        + "be parsed as missing", displayOrder = 4)
    public
      void setTreatUnparsableNumericValuesAsMissing(
        boolean unparsableNumericValuesToMissing) {
      m_delegate
        .setTreatUnparsableNumericValuesAsMissing(unparsableNumericValuesToMissing);
    }

    /**
     * Get whether, for hitherto thought to be numeric columns, to treat any
     * unparsable values as missing value.
     *
     * @return true if unparsable numeric values are to be treated as missing
     */
    public boolean getTreatUnparsableNumericValuesAsMissing() {
      return m_delegate.getTreatUnparsableNumericValuesAsMissing();
    }

    public Object[] getNominalLabelSpecs() {
      return m_delegate.getNominalLabelSpecs();
    }

    @OptionMetadata(displayName = "Nominal label specification",
      description = "Optional specification of legal labels for nominal "
        + "attributes. May be specified multiple times. " + "The "
        + "spec contains two parts separated by a \":\". The "
        + "first part can be a range of attribute indexes or "
        + "a comma-separated list off attruibute names; the "
        + "second part is a comma-separated list of labels. E.g "
        + "\"1,2,4-6:red,green,blue\" or \"att1,att2:red,green,blue\"",
      displayOrder = 5)
    public void setNominalLabelSpecs(Object[] specs) {
      m_delegate.setNominalLabelSpecs(specs);
    }

    public Object[] getNominalDefaultLabelSpecs() {
      return m_delegate.getNominalDefaultLabelSpecs();
    }

    @OptionMetadata(
      displayName = "Default nominal label specification",
      description = "Specificaton of an optional 'default' label for nominal attributes. "
        + "To be used in conjuction with nominalLabelSpecs in the case where "
        + "you only want to specify some of the legal values that "
        + "a given attribute can take on. Any remaining values are then "
        + "assigned to this 'default' category. One use-case is to "
        + "easily convert a multi-class problem into a binary one - "
        + "in this case, only the positive class label need be specified "
        + "via nominalLabelSpecs and then the default label acts as a "
        + "catch-all for the rest. The specification format is the "
        + "same as for nominalLabelSpecs, namely "
        + "[index range | attribute name list]:<default label>",
      displayOrder = 6)
    public
      void setNominalDefaultLabelSpecs(Object[] specs) {
      m_delegate.setNominalDefaultLabelSpecs(specs);
    }

    public boolean getComputeQuartilesAsPartOfSummaryStats() {
      return m_delegate.getComputeQuartilesAsPartOfSummaryStats();
    }

    @OptionMetadata(
      displayName = "Compute quartiles as part of summary statistics",
      description = "Include estimated quartiles and histograms in summary statistics (note "
        + "that this increases run time).", displayOrder = 7)
    public
      void setComputeQuartilesAsPartOfSummaryStats(boolean c) {
      m_delegate.setComputeQuartilesAsPartOfSummaryStats(c);
    }

    public double getCompressionLevelForQuartileEstimation() {
      return m_delegate.getCompressionLevelForQuartileEstimation();
    }

    @OptionMetadata(
      displayName = "Compression level for quartile estimation",
      description = "Level of compression to use when computing estimated quantiles "
        + "(smaller is more compression). Less compression gives more accurate "
        + "estimates at the expense of time and space.", displayOrder = 8)
    public
      void setCompressionLevelForQuartileEstimation(double compression) {
      m_delegate.setCompressionLevelForQuartileEstimation(compression);
    }

    /**
     * Set the number of decimal places for outputting summary stats
     *
     * @param numDecimalPlaces number of decimal places to use
     */
    @OptionMetadata(
      displayName = "Number of decimal places",
      description = "The number of decimal places to use when formatting summary "
        + "statistics", displayOrder = 9)
    public
      void setNumDecimalPlaces(int numDecimalPlaces) {
      m_delegate.setNumDecimalPlaces(numDecimalPlaces);
    }

    /**
     * Get the number of decimal places for outputting summary stats
     *
     * @return number of decimal places to use
     */
    public int getNumDecimalPlaces() {
      return m_delegate.getNumDecimalPlaces();
    }
  }
}
