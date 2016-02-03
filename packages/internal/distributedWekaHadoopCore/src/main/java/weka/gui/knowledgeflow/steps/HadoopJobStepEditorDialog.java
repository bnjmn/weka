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
 *    HadoopJobStepEditorDialog
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import distributed.core.DistributedJob;
import distributed.core.DistributedJobConfig;
import distributed.hadoop.MapReduceJobConfig;
import weka.core.Environment;
import weka.core.Utils;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CorrelationMatrixMapTask;
import weka.distributed.KMeansMapTask;
import weka.distributed.WekaClassifierMapTask;
import weka.distributed.hadoop.ArffHeaderHadoopJob;
import weka.distributed.hadoop.HadoopJob;
import weka.gui.HadoopPropertyPanel;
import weka.gui.PropertySheetPanel;
import weka.gui.knowledgeflow.StepEditorDialog;
import weka.knowledgeflow.steps.AbstractHadoopJob;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Step editor dialog for the Hadoop Knowledge Flow steps
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 */
public class HadoopJobStepEditorDialog extends StepEditorDialog {

  private static final long serialVersionUID = -2106967193384849985L;

  /** Underlying job */
  protected HadoopJob m_job;

  /** Main config for the underlying Hadoop job */
  protected MapReduceJobConfig m_mrConfig;

  /** Environment variables */
  protected Environment m_env = Environment.getSystemWide();

  /** Editor for the main config */
  protected PropertySheetPanel m_mrConfigEditor = new PropertySheetPanel();

  /** Configuration property panel */
  protected HadoopPropertyPanel m_propPanel;

  /** ARFF job specific */
  protected CSVToARFFHeaderMapTask m_arffMapTask;

  /** Temporary ARFF header job for jobs that use this */
  protected weka.distributed.hadoop.ArffHeaderHadoopJob m_tempArffJob;

  /** Classifier map task for editing the classifier job */
  protected WekaClassifierMapTask m_classifierMapTask;

  /** Correlation map task for editing the correlation job */
  protected CorrelationMatrixMapTask m_correlationMapTask;

  /** KMeans map task for editing the KMeans job */
  protected KMeansMapTask m_kMeansMapTask;

  /** Additionally for evaluaton job */
  protected weka.distributed.hadoop.WekaClassifierHadoopJob m_tempClassifierJob;

  /** Tabs of the dialog */
  protected JTabbedPane m_configTabs = new JTabbedPane();

  /** For restoring original state */
  protected String m_optionsOrig;

  @Override
  public void layoutEditor() {
    m_job = ((AbstractHadoopJob) getStepToEdit()).getUnderlyingJob();
    m_optionsOrig = ((AbstractHadoopJob) getStepToEdit()).getJobOptions();
    m_mrConfig = m_job.getMapReduceJobConfig();
    m_mrConfigEditor.setEnvironment(getEnvironment());
    m_mrConfigEditor.setTarget(m_mrConfig);

    m_propPanel =
      new HadoopPropertyPanel(m_mrConfig.getUserSuppliedProperties());
    JPanel outerP = new JPanel();
    JPanel configHolder = new JPanel();
    configHolder.setLayout(new BorderLayout());
    configHolder.add(m_propPanel, BorderLayout.SOUTH);

    configHolder.add(m_mrConfigEditor, BorderLayout.NORTH);
    outerP.setLayout(new BorderLayout());
    outerP.add(configHolder, BorderLayout.NORTH);

    m_configTabs.addTab("Hadoop configuration", outerP);
    String jobTitle = getStepToEdit().getClass().getName();
    jobTitle =
      jobTitle.substring(jobTitle.lastIndexOf(".") + 1, jobTitle.length());

    if (m_job instanceof ArffHeaderHadoopJob) {
      addTabForArffHeaderJob(jobTitle, m_job);
    } else if (m_job instanceof weka.distributed.hadoop.WekaClassifierHadoopJob) {
      m_tempArffJob = new weka.distributed.hadoop.ArffHeaderHadoopJob();
      try {
        m_tempArffJob.setOptions(Utils.splitOptions(m_optionsOrig));
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      addTabForArffHeaderJob("ARFF header/CSV parsing", m_tempArffJob);
      addTabForClassifierJob(jobTitle, m_job);
    } else if (m_job instanceof weka.distributed.hadoop.WekaClassifierEvaluationHadoopJob) {
      m_tempArffJob = new weka.distributed.hadoop.ArffHeaderHadoopJob();
      try {
        m_tempArffJob.setOptions(Utils.splitOptions(m_optionsOrig));
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      addTabForArffHeaderJob("ARFF header/CSV parsing", m_tempArffJob);
      addTabForEvaluationJob(jobTitle, m_job);
    } else if (m_job instanceof weka.distributed.hadoop.CorrelationMatrixHadoopJob) {
      m_tempArffJob = new weka.distributed.hadoop.ArffHeaderHadoopJob();
      try {
        m_tempArffJob.setOptions(Utils.splitOptions(m_optionsOrig));
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      addTabForArffHeaderJob("ARFF header/CSV parsing", m_tempArffJob);
      addTabForCorrelationMatrixJob(jobTitle, m_job);
    } else if (m_job instanceof weka.distributed.hadoop.WekaScoringHadoopJob) {
      m_tempArffJob = new weka.distributed.hadoop.ArffHeaderHadoopJob();
      try {
        m_tempArffJob.setOptions(Utils.splitOptions(m_optionsOrig));
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      addTabForArffHeaderJob("ARFF header/CSV parsing", m_tempArffJob);
      addTabForScoringJob(jobTitle, m_job);
    } else if (m_job instanceof weka.distributed.hadoop.RandomizedDataChunkHadoopJob) {
      m_tempArffJob = new weka.distributed.hadoop.ArffHeaderHadoopJob();
      try {
        m_tempArffJob.setOptions(Utils.splitOptions(m_optionsOrig));
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      addTabForArffHeaderJob("ARFF header/CSV parsing", m_tempArffJob);
      addTabForRandomizedDataChunkJob("Random shuffle options", m_job);
    } else if (m_job instanceof weka.distributed.hadoop.KMeansClustererHadoopJob) {
      m_tempArffJob = new weka.distributed.hadoop.ArffHeaderHadoopJob();
      try {
        m_tempArffJob.setOptions(Utils.splitOptions(m_optionsOrig));
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      addTabForArffHeaderJob("ARFF header/CSV parsing", m_tempArffJob);
      addTabForKMeansJob(jobTitle, m_job);
    }

    add(m_configTabs, BorderLayout.CENTER);
  }

  /**
   * Adds a tab for the ARFF header job
   *
   * @param tabTitle the title for the tab
   * @param arffJob the ARFF job
   */
  protected void addTabForArffHeaderJob(String tabTitle, HadoopJob arffJob) {
    JPanel jobHolder = new JPanel();
    jobHolder.setLayout(new BorderLayout());

    PropertySheetPanel arffJobEditor = new PropertySheetPanel();
    arffJobEditor.setEnvironment(m_env);
    arffJobEditor.setTarget(arffJob);
    jobHolder.add(arffJobEditor, BorderLayout.NORTH);

    m_arffMapTask = new CSVToARFFHeaderMapTask();
    try {
      m_arffMapTask.setOptions(Utils.splitOptions(m_optionsOrig));
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    PropertySheetPanel mapTaskEditor = new PropertySheetPanel();
    mapTaskEditor.setTarget(m_arffMapTask);

    mapTaskEditor.setEnvironment(m_env);
    jobHolder.add(mapTaskEditor, BorderLayout.CENTER);

    // JScrollPane scroller = new JScrollPane(jobHolder);

    m_configTabs.addTab(tabTitle, jobHolder);
  }

  /**
   * Adds a tab for editing a classifier job
   *
   * @param tabTitle the title for the tab
   * @param classifierJob the classifier job to edit
   */
  protected void
    addTabForClassifierJob(String tabTitle, HadoopJob classifierJob) {
    JPanel jobHolder = makeClassifierJobPanel(classifierJob, false);
    JScrollPane scroller = new JScrollPane(jobHolder);

    m_configTabs.addTab(tabTitle, scroller);
  }

  /**
   * Makes a panel for editing a classifier job
   *
   * @param classifierJob the classifier job to edit
   * @param hideAbout true if the help info is not to be displayed
   * @return a JPanel setup for editing a classifier job
   */
  protected JPanel makeClassifierJobPanel(HadoopJob classifierJob,
    boolean hideAbout) {
    JPanel jobHolder = new JPanel();
    jobHolder.setLayout(new BorderLayout());

    PropertySheetPanel classifierJobEditor = new PropertySheetPanel();
    classifierJobEditor.setEnvironment(m_env);
    classifierJobEditor.setTarget(classifierJob);
    jobHolder.add(classifierJobEditor, BorderLayout.NORTH);

    if (hideAbout) {
      classifierJobEditor.getAboutPanel().setVisible(false);
    }

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

  /**
   * Adds a tab for editing an evaluation job
   *
   * @param tabTitle the title for the tab
   * @param evaluationJob the evaluation job to edit
   */
  public void addTabForEvaluationJob(String tabTitle, HadoopJob evaluationJob) {
    JPanel jobHolder = new JPanel();
    jobHolder.setLayout(new BorderLayout());

    PropertySheetPanel evaluationJobEditor = new PropertySheetPanel();
    evaluationJobEditor.setEnvironment(m_env);
    evaluationJobEditor.setTarget(evaluationJob);
    jobHolder.add(evaluationJobEditor, BorderLayout.NORTH);

    m_tempClassifierJob = new weka.distributed.hadoop.WekaClassifierHadoopJob();
    try {
      m_tempClassifierJob.setOptions(Utils.splitOptions(m_optionsOrig));
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    JPanel classifierJobP = makeClassifierJobPanel(m_tempClassifierJob, true);

    jobHolder.add(classifierJobP, BorderLayout.CENTER);
    JScrollPane scroller = new JScrollPane(jobHolder);

    m_configTabs.addTab(tabTitle, scroller);
  }

  /**
   * Add a tab for editing a correlation job
   *
   * @param tabTitle the title of the tab
   * @param correlationJob the correlation job to edit
   */
  public void addTabForCorrelationMatrixJob(String tabTitle,
    HadoopJob correlationJob) {
    JPanel jobHolder = new JPanel();
    jobHolder.setLayout(new BorderLayout());

    PropertySheetPanel matrixJobEditor = new PropertySheetPanel();
    matrixJobEditor.setEnvironment(m_env);
    matrixJobEditor.setTarget(correlationJob);
    jobHolder.add(matrixJobEditor, BorderLayout.NORTH);

    m_correlationMapTask = new CorrelationMatrixMapTask();
    try {
      m_correlationMapTask.setOptions(Utils.splitOptions(m_optionsOrig));
    } catch (Exception e) {
      e.printStackTrace();
    }

    PropertySheetPanel matrixTaskEditor = new PropertySheetPanel();
    matrixTaskEditor.setEnvironment(m_env);
    matrixTaskEditor.setTarget(m_correlationMapTask);
    jobHolder.add(matrixTaskEditor, BorderLayout.CENTER);

    m_configTabs.addTab(tabTitle, jobHolder);
  }

  /**
   * Add a tab for editing the scoring job
   *
   * @param tabTitle the title of the tab
   * @param scoringJob the scoring job to edit
   */
  public void addTabForScoringJob(String tabTitle, HadoopJob scoringJob) {
    JPanel jobHolder = new JPanel();
    jobHolder.setLayout(new BorderLayout());

    PropertySheetPanel scoringJobEditor = new PropertySheetPanel();
    scoringJobEditor.setEnvironment(m_env);
    scoringJobEditor.setTarget(scoringJob);
    jobHolder.add(scoringJobEditor, BorderLayout.NORTH);

    m_configTabs.addTab(tabTitle, jobHolder);
  }

  protected void addTabForRandomizedDataChunkJob(String tabTitle,
    HadoopJob randomizeJob) {
    JPanel jobHolder = new JPanel();
    jobHolder.setLayout(new BorderLayout());
    PropertySheetPanel randomizeJobEditor = new PropertySheetPanel();
    randomizeJobEditor.setEnvironment(m_env);

    randomizeJobEditor.setTarget(randomizeJob);
    jobHolder.add(randomizeJobEditor, BorderLayout.NORTH);

    JScrollPane scroller = new JScrollPane(jobHolder);
    m_configTabs.addTab(tabTitle, scroller);
  }

  /**
   * Adds a tab for editing a k-means job
   *
   * @param tabTitle the title for the tab
   * @param kmeansJob the k-means job to edit
   */
  protected void addTabForKMeansJob(String tabTitle, HadoopJob kmeansJob) {
    JPanel jobHolder = new JPanel();

    jobHolder.setLayout(new BorderLayout());

    PropertySheetPanel clustererJobEditor = new PropertySheetPanel();
    clustererJobEditor.setEnvironment(m_env);
    clustererJobEditor.setTarget(kmeansJob);
    jobHolder.add(clustererJobEditor, BorderLayout.NORTH);

    // if (hideAbout) {
    // classifierJobEditor.getAboutPanel().setVisible(false);
    // }

    m_kMeansMapTask = new KMeansMapTask();
    try {
      m_kMeansMapTask.setOptions(Utils.splitOptions(m_optionsOrig));
    } catch (Exception e) {
      e.printStackTrace();
    }
    PropertySheetPanel clustererTaskEditor = new PropertySheetPanel();
    clustererTaskEditor.setEnvironment(m_env);
    clustererTaskEditor.setTarget(m_kMeansMapTask);
    jobHolder.add(clustererTaskEditor, BorderLayout.CENTER);

    JScrollPane scroller = new JScrollPane(jobHolder);
    m_configTabs.addTab(tabTitle, scroller);
  }

  @Override
  public void okPressed() {
    if (m_job instanceof weka.distributed.hadoop.ArffHeaderHadoopJob) {
      okARFFJob();
    } else if (m_job instanceof weka.distributed.hadoop.WekaClassifierHadoopJob) {
      okClassifierJob();
    } else if (m_job instanceof weka.distributed.hadoop.WekaClassifierEvaluationHadoopJob) {
      okEvaluationJob();
    } else if (m_job instanceof weka.distributed.hadoop.CorrelationMatrixHadoopJob) {
      okCorrelationJob();
    } else if (m_job instanceof weka.distributed.hadoop.RandomizedDataChunkHadoopJob) {
      okRandomizeJob();
    } else if (m_job instanceof weka.distributed.hadoop.KMeansClustererHadoopJob) {
      okKMeansJob();
    } else {
      okScoringJob();
    }
  }

  @Override
  public void cancelPressed() {
    ((AbstractHadoopJob) getStepToEdit()).setJobOptions(m_optionsOrig);
  }

  protected void okARFFJob() {
    List<String> opts = getBaseConfig(m_job);
    addArffJobOptionsOnly(opts,
      (weka.distributed.hadoop.ArffHeaderHadoopJob) m_job);
    addArffMapTaskOpts(opts);
    applyOptionsToJob(opts);
  }

  /**
   * Actions to apply to the classifier job when closing under the "OK"
   * condition
   */
  protected void okClassifierJob() {
    List<String> opts = getBaseConfig(m_job);
    addArffJobOptionsOnly(opts, m_tempArffJob);
    addArffMapTaskOpts(opts);
    addClassifierJobOptionsOnly(opts,
      (weka.distributed.hadoop.WekaClassifierHadoopJob) m_job);
    addClassifierMapTaskOpts(opts);

    applyOptionsToJob(opts);
  }

  /**
   * Gets the base options from the underlying job and stores them in a list
   *
   * @param job the job to extract base options from
   * @return a list of options
   */
  protected List<String> getBaseConfig(HadoopJob job) {

    String additionalPackages =
      m_mrConfig
        .getUserSuppliedProperty(DistributedJob.WEKA_ADDITIONAL_PACKAGES_KEY);
    m_mrConfig.clearUserSuppliedProperties();
    Map<String, String> userProps = m_propPanel.getProperties();
    for (Map.Entry<String, String> e : userProps.entrySet()) {
      // skip this one! As we'll get it via the base job stuff below
      if (e.getKey() != null
        && !e.getKey().equals(DistributedJob.WEKA_ADDITIONAL_PACKAGES_KEY)) {
        m_mrConfig.setUserSuppliedProperty(e.getKey(), e.getValue());
      }
    }
    if (!DistributedJobConfig.isEmpty(additionalPackages)) {
      m_mrConfig.setUserSuppliedProperty(
        DistributedJob.WEKA_ADDITIONAL_PACKAGES_KEY, additionalPackages);
    }

    String[] baseJobOpts = job.getBaseOptionsOnly();
    String[] mrConfigOpts = m_mrConfig.getOptions();

    List<String> opts = new ArrayList<String>();
    opts.addAll(Arrays.asList(baseJobOpts));
    opts.addAll(Arrays.asList(mrConfigOpts));

    return opts;
  }

  /**
   * Adds options from the ARFF job only to the supplied list
   *
   * @param opts the list of options to add to
   * @param arffJob the ARFF job to grab options from
   */
  protected void addArffJobOptionsOnly(List<String> opts,
    ArffHeaderHadoopJob arffJob) {
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
   * Add options from the classifier job only to the supplied list
   *
   * @param opts the list of options to add to
   * @param classifierJob the classifier job to grab options from
   */
  protected void addClassifierJobOptionsOnly(List<String> opts,
    weka.distributed.hadoop.WekaClassifierHadoopJob classifierJob) {
    String[] classifierOpts = classifierJob.getJobOptionsOnly();
    opts.addAll(Arrays.asList(classifierOpts));
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
   * Actions to apply to the evaluation job when closing under the "OK"
   * condition
   */
  protected void okEvaluationJob() {
    List<String> opts = getBaseConfig(m_tempClassifierJob);
    addArffJobOptionsOnly(opts, m_tempArffJob);
    addArffMapTaskOpts(opts);
    addEvaluationJobOptionsOnly(opts);

    addClassifierJobOptionsOnly(opts, m_tempClassifierJob);
    addClassifierMapTaskOpts(opts);

    applyOptionsToJob(opts);
  }

  /**
   * Add options from the evaluation job only to the supplied list
   *
   * @param opts the list of options to add to
   */
  protected void addEvaluationJobOptionsOnly(List<String> opts) {
    String[] evalOpts =
      ((weka.distributed.hadoop.WekaClassifierEvaluationHadoopJob) m_job)
        .getJobOptionsOnly();
    opts.addAll(Arrays.asList(evalOpts));
  }

  /**
   * Actions to apply to the correlation job when closing under the "OK"
   * condition
   */
  protected void okCorrelationJob() {
    List<String> opts = getBaseConfig(m_job);
    addArffJobOptionsOnly(opts, m_tempArffJob);
    addArffMapTaskOpts(opts);
    addCorrelationJobOptionsOnly(opts,
      (weka.distributed.hadoop.CorrelationMatrixHadoopJob) m_job);
    addCorrelationMapTaskOpts(opts);

    applyOptionsToJob(opts);
  }

  /**
   * Add options from the correlation job only to the supplied list
   *
   * @param opts the list of options to add to
   */
  protected void addCorrelationJobOptionsOnly(List<String> opts,
    weka.distributed.hadoop.CorrelationMatrixHadoopJob correlationJob) {
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
   * Actions to apply to the randomize job when closing under the "OK" condition
   */
  protected void okRandomizeJob() {
    List<String> opts = getBaseConfig(m_job);
    addArffJobOptionsOnly(opts, m_tempArffJob);
    addArffMapTaskOpts(opts);

    addRandomizeJobOptionsOnly(opts,
      (weka.distributed.hadoop.RandomizedDataChunkHadoopJob) m_job);

    applyOptionsToJob(opts);
  }

  /**
   * Add options from the randomize job only to the supplied list
   *
   * @param opts the list of options to add to
   */
  protected void addRandomizeJobOptionsOnly(List<String> opts,
    weka.distributed.hadoop.RandomizedDataChunkHadoopJob randomizeJob) {
    String[] randomizeOps = randomizeJob.getJobOptionsOnly();
    opts.addAll(Arrays.asList(randomizeOps));
  }

  /**
   * Actions to apply to the k-means job when closing under the "OK" condition
   */
  protected void okKMeansJob() {
    List<String> opts = getBaseConfig(m_job);
    addArffJobOptionsOnly(opts, m_tempArffJob);
    addArffMapTaskOpts(opts);

    addKMeansJobOptionsOnly(opts,
      (weka.distributed.hadoop.KMeansClustererHadoopJob) m_job);
    addKMeansMapTaskOpts(opts);

    applyOptionsToJob(opts);
  }

  /**
   * Add options from the k-means job only to the supplied list
   *
   * @param opts the list of options to add to
   * @param kMeansJob the classifier job to grab options from
   */
  protected void addKMeansJobOptionsOnly(List<String> opts,
    weka.distributed.hadoop.KMeansClustererHadoopJob kMeansJob) {
    String[] clustererOpts = kMeansJob.getJobOptionsOnly();
    opts.addAll(Arrays.asList(clustererOpts));
  }

  protected void addKMeansMapTaskOpts(List<String> opts) {
    String[] clustererMapOpts = m_kMeansMapTask.getOptions();
    opts.addAll(Arrays.asList(clustererMapOpts));
  }

  /**
   * Actions to apply to the scoring job when closing under the "OK" condition
   */
  protected void okScoringJob() {
    List<String> opts = getBaseConfig(m_job);
    addArffJobOptionsOnly(opts, m_tempArffJob);
    addArffMapTaskOpts(opts);
    addScoringJobOptionsOnly(opts);

    applyOptionsToJob(opts);
  }

  /**
   * Add options from the scoring job only to the supplied list
   *
   * @param opts the list of options to add to
   */
  protected void addScoringJobOptionsOnly(List<String> opts) {
    String[] scoringOpts =
      ((weka.distributed.hadoop.WekaScoringHadoopJob) m_job)
        .getJobOptionsOnly();
    opts.addAll(Arrays.asList(scoringOpts));
  }

  /**
   * Apply the complete list of options to the current underlying job
   *
   * @param opts the options to apply
   */
  protected void applyOptionsToJob(List<String> opts) {
    String combined = Utils.joinOptions(opts.toArray(new String[opts.size()]));
    System.err.println("Combined: " + combined);

    ((AbstractHadoopJob) getStepToEdit()).setJobOptions(combined);
  }
}
