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
 *    SparkJobCustomizer
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;

import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.Utils;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CorrelationMatrixMapTask;
import weka.distributed.KMeansMapTask;
import weka.distributed.WekaClassifierMapTask;
import weka.distributed.spark.SparkJob;
import weka.distributed.spark.WekaClassifierSparkJob;
import weka.gui.PropertySheetPanel;
import distributed.core.DistributedJob;
import distributed.core.DistributedJobConfig;
import distributed.spark.SparkJobConfig;

/**
 * GUI customizer for the Knowledge Flow Spark jobs
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class SparkJobCustomizer extends JPanel implements BeanCustomizer,
  CustomizerCloseRequester, EnvironmentHandler {

  /** Bean being edited */
  protected AbstractSparkJob m_bean;

  /** Underlying job */
  protected SparkJob m_job;

  /**
   * Main config for the underlying Spark job. Settings here may be ignored if
   * this job receives a SparkContext from an upstream step
   */
  protected SparkJobConfig m_sjConfig;

  /** Environment variables */
  protected Environment m_env = Environment.getSystemWide();

  /** Listener for modifications to the step being edited */
  protected ModifyListener m_modifyListener;

  /** The parent window of this customizer */
  protected Window m_parentWindow;

  /** Editor for the main config */
  protected PropertySheetPanel m_sjConfigEditor = new PropertySheetPanel();

  /** Configuration property panel */
  protected SparkPropertyPanel m_propPanel;

  /** ARFF job specific */
  protected CSVToARFFHeaderMapTask m_arffMapTask;

  /** Classifier map task for editing the classifier job */
  protected WekaClassifierMapTask m_classifierMapTask;

  /** Temporary ARFF header job for jobs that use this */
  protected weka.distributed.spark.ArffHeaderSparkJob m_tempArffJob;

  /** Additionally for evaluaton job */
  protected weka.distributed.spark.WekaClassifierSparkJob m_tempClassifierJob;

  /** Correlation map task for editing the correlation job */
  protected CorrelationMatrixMapTask m_correlationMapTask;

  /** KMeans map task for editing the KMeans job */
  protected KMeansMapTask m_kMeansMapTask;

  /** Tabs of the dialog */
  protected JTabbedPane m_configTabs = new JTabbedPane();

  /** For restoring original state */
  protected String m_optionsOrig;

  public SparkJobCustomizer() {
    setLayout(new BorderLayout());
  }

  @Override
  public void setObject(Object bean) {
    m_bean = (AbstractSparkJob) bean;
    m_job = m_bean.getUnderlyingJob();
    m_optionsOrig = m_bean.getJobOptions();
    m_sjConfig = m_job.getSparkJobConfig();
    m_sjConfigEditor.setEnvironment(m_env);
    m_sjConfigEditor.setTarget(m_sjConfig);

    setup();
  }

  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
  }

  @Override
  public void setParentWindow(Window parent) {
    m_parentWindow = parent;
  }

  @Override
  public void setModifiedListener(ModifyListener l) {
    m_modifyListener = l;
  }

  protected void setup() {
    removeAll();

    // Only allow spark connection options if there is no
    // upstream step (i.e. this one is acting as a start point)
    if (!m_bean.hasUpstreamConnection()) {
      JPanel configHolder = new JPanel();
      configHolder.setLayout(new BorderLayout());
      configHolder.add(m_sjConfigEditor, BorderLayout.NORTH);
      m_propPanel = new SparkPropertyPanel(
        m_sjConfig.getUserSuppliedProperties());
      configHolder.add(m_propPanel, BorderLayout.SOUTH);

      JPanel outerP = new JPanel();
      outerP.setLayout(new BorderLayout());
      outerP.add(configHolder, BorderLayout.NORTH);

      m_configTabs.addTab("Spark configuration", outerP);
    }

    String jobTitle = m_bean.getClass().getName();
    jobTitle = jobTitle.substring(jobTitle.lastIndexOf(".") + 1,
      jobTitle.length());

    if (m_job instanceof weka.distributed.spark.ArffHeaderSparkJob) {
      addTabForArffHeaderJob(jobTitle, m_job);
    } else if (m_job instanceof WekaClassifierSparkJob) {
      m_tempArffJob = new weka.distributed.spark.ArffHeaderSparkJob();
      try {
        m_tempArffJob.setOptions(Utils.splitOptions(m_optionsOrig));
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      addTabForArffHeaderJob("ARFF header/CSV parsing", m_tempArffJob);
      addTabForClassifierJob(jobTitle, m_job);
    } else if (m_job instanceof weka.distributed.spark.WekaClassifierEvaluationSparkJob) {
      m_tempArffJob = new weka.distributed.spark.ArffHeaderSparkJob();
      try {
        m_tempArffJob.setOptions(Utils.splitOptions(m_optionsOrig));
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      addTabForArffHeaderJob("ARFF header/CSV parsing", m_tempArffJob);
      addTabForEvaluationJob(jobTitle, m_job);
    } else if (m_job instanceof weka.distributed.spark.CorrelationMatrixSparkJob) {
      m_tempArffJob = new weka.distributed.spark.ArffHeaderSparkJob();
      try {
        m_tempArffJob.setOptions(Utils.splitOptions(m_optionsOrig));
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      addTabForArffHeaderJob("ARFF header/CSV parsing", m_tempArffJob);
      addTabForCorrelationMatrixJob(jobTitle, m_job);
    } else if (m_job instanceof weka.distributed.spark.WekaScoringSparkJob) {
      m_tempArffJob = new weka.distributed.spark.ArffHeaderSparkJob();
      try {
        m_tempArffJob.setOptions(Utils.splitOptions(m_optionsOrig));
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      addTabForArffHeaderJob("ARFF header/CSV parsing", m_tempArffJob);
      addTabForScoringJob(jobTitle, m_job);
    } else if (m_job instanceof weka.distributed.spark.RandomizedDataSparkJob) {
      m_tempArffJob = new weka.distributed.spark.ArffHeaderSparkJob();
      try {
        m_tempArffJob.setOptions(Utils.splitOptions(m_optionsOrig));
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      addTabForArffHeaderJob("ARFF header/CSV parsing", m_tempArffJob);
      addTabForRandomizedDataChunkJob("Random shuffle options", m_job);
    } else if (m_job instanceof weka.distributed.spark.KMeansClustererSparkJob) {
      m_tempArffJob = new weka.distributed.spark.ArffHeaderSparkJob();
      try {
        m_tempArffJob.setOptions(Utils.splitOptions(m_optionsOrig));
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      addTabForArffHeaderJob("ARFF header/CSV parsing", m_tempArffJob);
      addTabForKMeansJob(jobTitle, m_job);
    } else if (m_job instanceof weka.distributed.spark.CanopyClustererSparkJob) {
      // TODO
    }

    add(m_configTabs, BorderLayout.CENTER);

    addButtons();
  }

  private void addButtons() {
    JButton okBut = new JButton("OK");
    JButton cancelBut = new JButton("Cancel");

    JPanel butHolder = new JPanel();
    butHolder.setLayout(new GridLayout(1, 2));
    butHolder.add(okBut);
    butHolder.add(cancelBut);
    add(butHolder, BorderLayout.SOUTH);

    okBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        closingOK();
      }
    });

    cancelBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        closingCancel();
      }
    });
  }

  /**
   * Adds a tab for the ARFF header job
   *
   * @param tabTitle the title for the tab
   * @param arffJob the ARFF job
   */
  protected void addTabForArffHeaderJob(String tabTitle, SparkJob arffJob) {
    // only need this if we don't have an upstream connection (where RDD
    // datasets and contexts are coming from)

    if (!m_bean.hasUpstreamConnection()) {
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

      m_configTabs.addTab(tabTitle, jobHolder);
    }
  }

  /**
   * Adds a tab for editing a classifier job
   *
   * @param tabTitle the title for the tab
   * @param classifierJob the classifier job to edit
   */
  protected void addTabForClassifierJob(String tabTitle, SparkJob classifierJob) {
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
  protected JPanel makeClassifierJobPanel(SparkJob classifierJob,
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
  public void addTabForEvaluationJob(String tabTitle, SparkJob evaluationJob) {
    JPanel jobHolder = new JPanel();
    jobHolder.setLayout(new BorderLayout());

    PropertySheetPanel evaluationJobEditor = new PropertySheetPanel();
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

    m_configTabs.addTab(tabTitle, scroller);
  }

  /**
   * Add a tab for editing a correlation job
   *
   * @param tabTitle the title of the tab
   * @param correlationJob the correlation job to edit
   */
  public void addTabForCorrelationMatrixJob(String tabTitle,
    SparkJob correlationJob) {
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
  public void addTabForScoringJob(String tabTitle, SparkJob scoringJob) {
    JPanel jobHolder = new JPanel();
    jobHolder.setLayout(new BorderLayout());

    PropertySheetPanel scoringJobEditor = new PropertySheetPanel();
    scoringJobEditor.setEnvironment(m_env);
    scoringJobEditor.setTarget(scoringJob);
    jobHolder.add(scoringJobEditor, BorderLayout.NORTH);

    m_configTabs.addTab(tabTitle, jobHolder);
  }

  /**
   * Add a tab for editing the randomly shuffle data job
   *
   * @param tabTitle the title of the tab
   * @param randomizeJob the randomize job to edit
   */
  protected void addTabForRandomizedDataChunkJob(String tabTitle,
    SparkJob randomizeJob) {
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
  protected void addTabForKMeansJob(String tabTitle, SparkJob kmeansJob) {
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

  protected void closingOK() {
    // TODO
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

  /**
   * Gets the base options from the underlying job and stores them in a list
   *
   * @param job the job to extract base options from
   * @return a list of options
   */
  protected List<String> getBaseConfig(SparkJob job) {

    if (!m_bean.hasUpstreamConnection()) {
      m_sjConfig.clearUserSuppliedProperties();
      Map<String, String> userProps = m_propPanel.getProperties();
      for (Map.Entry<String, String> e : userProps.entrySet()) {
        // skip this one! As we'll get it via the base job stuff below
        if (e.getKey() != null && !e.getKey()
          .equals(DistributedJob.WEKA_ADDITIONAL_PACKAGES_KEY)) {
          m_sjConfig.setUserSuppliedProperty(e.getKey(), e.getValue());
        }
      }
    }
    String[] baseJobOpts = job.getBaseOptionsOnly();
    String[] mrConfigOpts = m_sjConfig.getOptions();

    List<String> opts = new ArrayList<String>();
    for (String s : baseJobOpts) {
      opts.add(s);
    }

    for (String s : mrConfigOpts) {
      opts.add(s);
    }

    return opts;
  }

  /**
   * Adds options from the ARFF map task to the supplied list of options
   *
   * @param opts the list of options to add the ARFF options to
   */
  protected void addArffMapTaskOpts(List<String> opts) {
    if (!m_bean.hasUpstreamConnection()) {
      String[] arffMapOpts = m_arffMapTask.getOptions();

      for (String s : arffMapOpts) {
        opts.add(s);
      }
    }
  }

  /**
   * Add options from the classifier map task to the supplied list
   *
   * @param opts the list of options to add to
   */
  protected void addClassifierMapTaskOpts(List<String> opts) {
    String[] classifierMapOpts = m_classifierMapTask.getOptions();

    for (String s : classifierMapOpts) {
      opts.add(s);
    }
  }

  /**
   * Adds options from the ARFF job only to the supplied list
   *
   * @param opts the list of options to add to
   * @param arffJob the ARFF job to grab options from
   */
  protected void addArffJobOptionsOnly(List<String> opts,
    weka.distributed.spark.ArffHeaderSparkJob arffJob) {

    if (!m_bean.hasUpstreamConnection()) {
      String[] arffJobOpts = arffJob.getJobOptionsOnly();

      for (String s : arffJobOpts) {
        opts.add(s);
      }
    }
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

    for (String s : clustererOpts) {
      opts.add(s);
    }
  }

  /**
   * Add options from the correlation job only to the supplied list
   *
   * @param opts the list of options to add to
   */
  protected void addCorrelationJobOptionsOnly(List<String> opts,
    weka.distributed.spark.CorrelationMatrixSparkJob correlationJob) {
    String[] corrOpts = correlationJob.getJobOptionsOnly();

    for (String o : corrOpts) {
      opts.add(o);
    }
  }

  protected void addKMeansMapTaskOpts(List<String> opts) {
    String[] clustererMapOpts = m_kMeansMapTask.getOptions();

    for (String s : clustererMapOpts) {
      opts.add(s);
    }
  }

  /**
   * Add options from the correlation map task to the supplied list
   *
   * @param opts the list of options to add to
   */
  protected void addCorrelationMapTaskOpts(List<String> opts) {
    String[] corrOpts = m_correlationMapTask.getOptions();

    for (String s : corrOpts) {
      opts.add(s);
    }
  }

  /**
   * Add options from the randomize job only to the supplied list
   *
   * @param opts the list of options to add to
   */
  protected void addRandomizeJobOptionsOnly(List<String> opts,
    weka.distributed.spark.RandomizedDataSparkJob randomizeJob) {
    String[] randomizeOps = randomizeJob.getJobOptionsOnly();

    for (String o : randomizeOps) {
      opts.add(o);
    }
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

    for (String s : classifierOpts) {
      opts.add(s);
    }
  }

  /**
   * Add options from the evaluation job only to the supplied list
   *
   * @param opts the list of options to add to
   */
  protected void addEvaluationJobOptionsOnly(List<String> opts) {
    String[] evalOpts = ((weka.distributed.spark.WekaClassifierEvaluationSparkJob) m_job)
      .getJobOptionsOnly();

    for (String o : evalOpts) {
      opts.add(o);
    }
  }

  /**
   * Add options from the scoring job only to the supplied list
   *
   * @param opts the list of options to add to
   */
  protected void addScoringJobOptionsOnly(List<String> opts) {
    String[] scoringOpts =
            ((weka.distributed.spark.WekaScoringSparkJob) m_job)
                    .getJobOptionsOnly();

    for (String o : scoringOpts) {
      opts.add(o);
    }
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
    addArffJobOptionsOnly(opts,
      (weka.distributed.spark.ArffHeaderSparkJob) m_tempArffJob);
    addArffMapTaskOpts(opts);
    addClassifierJobOptionsOnly(opts,
      (weka.distributed.spark.WekaClassifierSparkJob) m_job);
    addClassifierMapTaskOpts(opts);

    applyOptionsToJob(opts);
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
      (weka.distributed.spark.CorrelationMatrixSparkJob) m_job);
    addCorrelationMapTaskOpts(opts);

    applyOptionsToJob(opts);
  }

  /**
   * Actions to apply to the k-means job when closing under the "OK" condition
   */
  protected void okKMeansJob() {
    List<String> opts = getBaseConfig(m_job);
    addArffJobOptionsOnly(opts, m_tempArffJob);
    addArffMapTaskOpts(opts);

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
    addArffJobOptionsOnly(opts, m_tempArffJob);
    addArffMapTaskOpts(opts);

    addRandomizeJobOptionsOnly(opts,
      (weka.distributed.spark.RandomizedDataSparkJob) m_job);

    applyOptionsToJob(opts);
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
   * Actions to perform when closing under the "Cancel" condition
   */
  protected void closingCancel() {
    m_bean.setJobOptions(m_optionsOrig);

    m_parentWindow.dispose();
  }

  /**
   * Apply the complete list of options to the current underlying job
   *
   * @param opts the options to apply
   */
  protected void applyOptionsToJob(List<String> opts) {
    String combined = Utils.joinOptions(opts.toArray(new String[opts.size()]));
    System.err.println("Combined: " + combined);

    if (!combined.equals(m_optionsOrig)) {
      m_modifyListener.setModifiedStatus(this, true);
    }

    m_bean.setJobOptions(combined);

    m_parentWindow.dispose();
  }

  protected class SparkPropertyPanel extends JPanel {

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
}
