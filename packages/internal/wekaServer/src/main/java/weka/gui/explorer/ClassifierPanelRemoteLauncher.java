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
 *    ClassifierPanelRemoteLauncher.java
 *    Copyright (C) 2011-2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.explorer;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.mortbay.jetty.security.Password;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.AggregateableEvaluation;
import weka.classifiers.Classifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.Evaluation;
import weka.classifiers.Sourcable;
import weka.classifiers.evaluation.output.prediction.AbstractOutput;
import weka.classifiers.evaluation.output.prediction.Null;
import weka.classifiers.misc.InputMappedClassifier;
import weka.core.Environment;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.LogHandler;
import weka.core.OptionHandler;
import weka.core.SerializationHelper;
import weka.core.SerializedObject;
import weka.core.Utils;
import weka.core.WekaPackageManager;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.converters.FileSourcedConverter;
import weka.core.converters.IncrementalConverter;
import weka.core.converters.Loader;
import weka.core.converters.URLSourcedLoader;
import weka.experiment.TaskStatusInfo;
import weka.gui.Logger;
import weka.gui.TaskLogger;
import weka.gui.visualize.PlotData2D;
import weka.server.ExecuteTaskServlet;
import weka.server.GetTaskResultServlet;
import weka.server.GetTaskStatusServlet;
import weka.server.NamedTask;
import weka.server.PurgeTaskServlet;
import weka.server.WekaServer;
import weka.server.WekaServlet;
import weka.server.logging.ServerLogger;

/**
 * Plugin launch handler for the Explorer's Classify panel to launch evaluation
 * tasks on Weka Server instances.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class ClassifierPanelRemoteLauncher extends JPanel implements
  ClassifierPanelLaunchHandlerPlugin {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 7011549770584691491L;

  /** A reference to the ClassifierPanel */
  protected ClassifierPanel m_classifierPanel;

  /** OK button to launch the remote task(s) */
  protected JButton m_okButton = new JButton("OK");

  /** Cancel button to cancel the dialog */
  protected JButton m_cancelButton = new JButton("Cancel");

  /** Panel to configure the host name and port of the server to send to */
  protected HostPanel m_hostPanel = new HostPanel();

  /** Dialog to pop up us up in */
  protected JDialog m_popupD;

  /** Username to authenticate against the server with */
  protected String m_username;

  /** Password to authenticate against the server with */
  protected String m_password;

  /**
   * Construct the panel.
   */
  public ClassifierPanelRemoteLauncher() {
    setLayout(new BorderLayout());
    JPanel holderP = new JPanel();
    holderP.setLayout(new BorderLayout());
    holderP.add(m_hostPanel, BorderLayout.NORTH);
    JPanel butHolder = new JPanel();
    butHolder.setLayout(new FlowLayout(FlowLayout.LEFT));
    butHolder.add(m_okButton);
    butHolder.add(m_cancelButton);
    holderP.add(butHolder, BorderLayout.SOUTH);
    add(holderP, BorderLayout.NORTH);

    /*
     * m_launchButton.addActionListener(new ActionListener() { public void
     * actionPerformed(ActionEvent e) { popupUI(); } });
     */

    m_okButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_popupD != null) {
          m_popupD.dispose();
          m_popupD = null;
          launchRemote();
          // m_launchButton.setEnabled(true);
        }
      }
    });

    m_cancelButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_popupD != null) {
          m_popupD.dispose();
          m_popupD = null;
          // m_launchButton.setEnabled(true);
        }
      }
    });

    String wekaServerPasswordPath =
      WekaPackageManager.WEKA_HOME.toString() + File.separator + "server"
        + File.separator + "weka.pwd";
    File wekaServerPasswordFile = new File(wekaServerPasswordPath);
    boolean useAuth = wekaServerPasswordFile.exists();

    if (useAuth) {
      BufferedReader br = null;
      try {
        br = new BufferedReader(new FileReader(wekaServerPasswordFile));
        String line = null;
        while ((line = br.readLine()) != null) {
          // not a comment character, then assume its the data
          if (!line.startsWith("#")) {

            String[] parts = line.split(":");
            if (parts.length > 3 || parts.length < 2) {
              continue;
            }
            m_username = parts[0].trim();
            m_password = parts[1].trim();
            if (parts.length == 3 && parts[1].trim().startsWith("OBF")) {
              m_password = m_password + ":" + parts[2];
              String deObbs = Password.deobfuscate(m_password);
              m_password = deObbs;
            }
            break;
          }
        }
      } catch (Exception ex) {
        System.err.println("[SchedulePerspective] Error reading "
          + "password file: " + ex.getMessage());
      } finally {
        if (br != null) {
          try {
            br.close();
          } catch (Exception e) {
          }
        }
      }
    }
    m_hostPanel.setUsername(m_username);
    m_hostPanel.setPassword(m_password);
  }

  /**
   * Allows the classifier panel to pass in a reference to itself
   * 
   * @param p the ClassifierPanel
   */
  @Override
  public void setClassifierPanel(ClassifierPanel p) {
    m_classifierPanel = p;
  }

  /**
   * Gets called when the user clicks the button or selects this plugin's entry
   * from the popup menu.
   */
  @Override
  public void launch() {
    popupUI();
  }

  /**
   * Get the name of the launch command (to appear as the button text or in the
   * popup menu)
   * 
   * @return the name of the launch command
   */
  @Override
  public String getLaunchCommand() {
    return "Run on server";
  }

  /**
   * Print a status message to the ClassifierPanel's log.
   * 
   * @param message the message to display
   */
  protected synchronized void statusMessage(String message) {
    m_classifierPanel.getLog().statusMessage("[RemoteExceutor] " + message);
  }

  /**
   * Print a log message to the ClassifierPanel's log
   * 
   * @param message the message to display.
   */
  protected synchronized void logMessage(String message) {
    m_classifierPanel.getLog().logMessage("[RemoteExecutor] " + message);
  }

  /**
   * Inner class that monitors the status of a list of executing tasks.
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   * @version $Revision$
   */
  protected class TaskMonitor {

    /**
     * Monitor tasks and return a status that indicates whether all finished
     * without errors or not.
     * 
     * @param taskIDs the list of task IDs to monitor.
     * @return true if all tasks completed successfully
     */
    public boolean monitorTasks(List<String> taskIDs) {

      boolean allFinishedOK = true;
      InputStream is = null;
      PostMethod post = null;
      TaskStatusInfo resultInfo = null;
      // Get HTTP client
      HttpClient client =
        WekaServer.ConnectionManager.getSingleton().createHttpClient();
      WekaServer.ConnectionManager.addCredentials(client, m_username,
        m_password);
      int interval = m_hostPanel.getMonitorInterval();

      try {
        boolean[] finished = new boolean[taskIDs.size()];
        while (true) {
          try {
            Thread.sleep(1000 * interval);
            int numFinished = 0;
            for (int i = 0; i < taskIDs.size(); i++) {
              if (finished[i]) {
                numFinished++;
                continue;
              }
              // only check on those that haven't finished yet
              String taskID = taskIDs.get(i);
              String service =
                GetTaskStatusServlet.CONTEXT_PATH + "/?name=" + taskID
                  + "&client=Y";
              post = new PostMethod(constructURL(service));
              post.setDoAuthentication(true);
              post.addRequestHeader(new Header("Content-Type", "text/plain"));
              int result = client.executeMethod(post);
              if (result == 401) {
                logMessage("Unable to monitor task on server - authentication "
                  + "required");
                allFinishedOK = false;
                break;
              } else {
                is = post.getResponseBodyAsStream();
                ObjectInputStream ois =
                  SerializationHelper
                    .getObjectInputStream(new BufferedInputStream(
                      new GZIPInputStream(is)));
                Object response = ois.readObject();
                if (response.toString().startsWith(WekaServlet.RESPONSE_ERROR)) {

                  logMessage("Server returned an error: " + "for task : '"
                    + taskID + "'. (" + response.toString() + ")."
                    + " Check logs on server.");
                  allFinishedOK = false;
                  break;
                } else {
                  resultInfo = ((TaskStatusInfo) response);
                  if (resultInfo.getExecutionStatus() == TaskStatusInfo.FINISHED) {
                    numFinished++;
                    finished[i] = true;
                    if (m_classifierPanel.getLog() instanceof TaskLogger) {
                      ((TaskLogger) m_classifierPanel.getLog()).taskFinished();
                    }
                  }
                }
              }

              if (is != null) {
                try {
                  is.close();
                  is = null;
                } catch (IOException e) {
                  e.printStackTrace();
                }
              }

              if (post != null) {
                post.releaseConnection();
                post = null;
              }
            }

            if (numFinished == taskIDs.size()) {
              break;
            }

            if (!allFinishedOK) {

              // make sure we decrease the running task count by the number
              // of failed/unchecked tasks
              for (int i = 0; i < taskIDs.size() - numFinished; i++) {
                if (m_classifierPanel.getLog() instanceof TaskLogger) {
                  ((TaskLogger) m_classifierPanel.getLog()).taskFinished();
                }
              }
              break;
            }
          } catch (InterruptedException ex) {
            allFinishedOK = false;
          }
        }
      } catch (Exception ex) {
        allFinishedOK = false;
        logMessage("A problem occurred while "
          + "trying to retrieve remote status of a task " + "("
          + ex.getMessage() + ")");
      } finally {
        if (is != null) {
          try {
            is.close();
            is = null;
          } catch (IOException e) {
            e.printStackTrace();
          }
        }

        if (post != null) {
          post.releaseConnection();
          post = null;
        }
      }

      return allFinishedOK;
    }
  }

  /**
   * Base class for Classifier panel related tasks.
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   * @version $Revision$
   */
  public abstract static class ClassifierTask implements NamedTask, LogHandler,
    Serializable {

    /**
     * For serialization
     */
    private static final long serialVersionUID = 3891114415717418296L;

    /** The name of the task */
    protected String m_name;

    /** The task status info */
    protected TaskStatusInfo m_result = new TaskStatusInfo();

    /** Log */
    protected transient ServerLogger m_log;

    /** The classifier */
    protected Classifier m_classifier;

    /** Template for the classifier being used */
    protected Classifier m_template;

    /** True if visualizations are to be saved */
    protected boolean m_saveVis;

    /** Output collector */
    protected AbstractOutput m_outputCollector;

    /** Cost matrix */
    protected CostMatrix m_costMatrix;

    /**
     * Constructor.
     * 
     * @param classifier the classifier to use
     * @param saveVis whether to save visualization data or not
     * @param outputCollector output collector for accumulating textual output
     * @param costMatrix cost matrix to use (if any)
     */
    public ClassifierTask(Classifier classifier, boolean saveVis,
      AbstractOutput outputCollector, CostMatrix costMatrix) {
      m_template = classifier;
      m_saveVis = saveVis;
      m_outputCollector = outputCollector;
      m_costMatrix = costMatrix;
    }

    /**
     * Clients should be able to call this method at any time to obtain
     * information on a current task.
     * 
     * @return a TaskStatusInfo object holding info and result (if available)
     *         for this task
     */
    @Override
    public TaskStatusInfo getTaskStatus() {

      if (m_result.getExecutionStatus() == TaskStatusInfo.FINISHED
        || m_result.getExecutionStatus() == TaskStatusInfo.FAILED) {
        // once finished or failed set up the status message by
        // pulling current logging info
        StringBuffer temp = new StringBuffer();
        List<String> statusCache = m_log.getStatusCache();
        List<String> logCache = m_log.getLogCache();

        temp.append("@@@ Status messages:\n\n");
        for (String status : statusCache) {
          String entry = status + "\n";
          temp.append(entry);
        }
        temp.append("\n@@@ Log messages:\n\n");
        for (String log : logCache) {
          String entry = log + "\n";
          temp.append(entry);
        }
        m_result.setStatusMessage(temp.toString());
      }

      return m_result;
    }

    /**
     * Set the log to use
     * 
     * @param log the log to use
     */
    @Override
    public void setLog(Logger log) {
      m_log = (ServerLogger) log;
    }

    /**
     * Get the log in use
     * 
     * @return the log in use
     */
    @Override
    public Logger getLog() {
      return m_log;
    }

    /**
     * Set the name of this task
     * 
     * @param name the name of this task
     */
    @Override
    public void setName(String name) {
      m_name = name;
    }

    /**
     * Get the name of this task
     * 
     * @return the name of this task
     */
    @Override
    public String getName() {
      return m_name;
    }

    /**
     * Tell the task that it can free any resources (memory, results etc.) that
     * would not be needed for another execution run.
     */
    @Override
    public void freeMemory() {
      if (m_result != null) {
        m_result.setTaskResult(null);
      }
    }
  }

  /**
   * Task for evaluating a classifier on the training data.
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   * @version $Revision$
   */
  protected static class TestOnTrainTask extends ClassifierTask implements
    NamedTask, LogHandler, Serializable {

    /** For serialization */
    private static final long serialVersionUID = -2012120697675381974L;

    /** The training data */
    protected Instances m_training;

    /** The compressed training data */
    protected SerializedObject m_compressedTrain;

    /** Latest file used to persist the training data */
    protected File m_persistedTrain;

    /** File used to persist the results */
    protected File m_persistedResult;

    /** True if the model is to be output */
    protected boolean m_outputModel;

    /**
     * Constructor.
     * 
     * @param classifier the classifier to use
     * @param training the training data
     * @param saveVis whether to save the visualization data or not
     * @param outputCollector the collector used to accumulate textual results
     * @param costMatrix the cost matrix to use (if any)
     * @param outputModel whether the model is to be output
     * @throws Exception if a problem occurs
     */
    public TestOnTrainTask(Classifier classifier, Instances training,
      boolean saveVis, AbstractOutput outputCollector, CostMatrix costMatrix,
      boolean outputModel) throws Exception {
      super(classifier, saveVis, outputCollector, costMatrix);

      m_compressedTrain = new SerializedObject(training, true);
      m_outputModel = outputModel;
    }

    /**
     * Run the task
     */
    @Override
    public void execute() {
      ObjectOutputStream oos = null;
      try {
        if (m_compressedTrain == null) {
          // loadResources() must have failed
          throw new Exception("Unable to load compressed training data!");
        }
        m_training = (Instances) m_compressedTrain.getObject();
        m_compressedTrain = null;

        ClassifierErrorsPlotInstances plotInstances =
          ExplorerDefaults.getClassifierErrorsPlotInstances();

        String classifierName = m_template.getClass().getSimpleName();
        m_log.logMessage("Training classifier '" + classifierName
          + "' on training data '" + m_training.relationName() + "'");
        m_result.setExecutionStatus(TaskStatusInfo.PROCESSING);

        m_classifier = AbstractClassifier.makeCopy(m_template);
        plotInstances.setInstances(m_training);
        plotInstances.setClassifier(m_classifier);
        plotInstances.setClassIndex(m_training.classIndex());
        plotInstances.setSaveForVisualization(m_saveVis);

        if (m_outputCollector != null) {
          Instances header = new Instances(m_training, 0);
          header.setClassIndex(m_training.classIndex());
          m_outputCollector.setHeader(header);
          m_outputCollector.setBuffer(new StringBuffer());
        }

        Evaluation eval = new Evaluation(m_training, m_costMatrix);
        eval =
          ClassifierPanel.setupEval(eval, m_classifier, m_training,
            m_costMatrix, plotInstances, m_outputCollector, false);

        plotInstances.setUp();

        m_classifier.buildClassifier(m_training);

        m_log.logMessage("Testing classifier '" + classifierName
          + "' on training data '" + m_training.relationName() + "'");

        for (int i = 0; i < m_training.numInstances(); i++) {
          plotInstances.process(m_training.instance(i), m_classifier, eval);
          if (m_outputCollector != null) {
            m_outputCollector.printClassification(m_classifier,
              m_training.instance(i), i);
          }
        }

        // save memory
        m_training = null;

        List<Object> results = new ArrayList<Object>();
        results.add(eval);
        results.add(plotInstances);
        results.add(m_outputCollector);

        // return the model as part of the results (if specified)
        if (m_outputModel) {
          results.add(m_classifier);
        }

        m_result.setTaskResult(results);

        try {
          m_persistedResult = WekaServer.getTempFile();
          oos =
            new ObjectOutputStream(new BufferedOutputStream(
              new GZIPOutputStream(new FileOutputStream(m_persistedResult))));
          oos.writeObject(results);
          oos.flush();
          // successfully saved result - now save memory
          m_result.setTaskResult(null);
        } catch (Exception e) {
          m_persistedResult = null;
          // TODO should we set the in-memory result to null here?
          // loadResult() throws an exception if the file does not
          // exist or m_persistedResult is null, so the client can't
          // get the result anyway.
        }

        m_result.setExecutionStatus(TaskStatusInfo.FINISHED);
      } catch (Exception ex) {
        m_result.setExecutionStatus(TaskStatusInfo.FAILED);
        // log this
        StringWriter sr = new StringWriter();
        PrintWriter pr = new PrintWriter(sr);
        ex.printStackTrace(pr);
        pr.flush();
        m_log.logMessage(ex.getMessage() + "\n" + sr.getBuffer().toString());
        pr.close();
      } finally {
        // save memory
        m_training = null;
        m_outputCollector = null;
        if (oos != null) {
          try {
            oos.close();
          } catch (Exception e) {
          }
        }
      }
    }

    /**
     * Stop the running task (not implemente yet).
     */
    @Override
    public void stop() {
      // TODO
    }

    /**
     * Tell the task that it should persist any resources to disk (e.g. training
     * data, etc.). WekaServer.getTempFile() can be used to get a file to save
     * to.
     */
    @Override
    public void persistResources() {
      ObjectOutputStream oos = null;
      try {
        /*
         * if (m_persistedTrain != null) { // we've already saved previously -
         * no need to // do so again. Just save memory m_compressedTrain = null;
         * 
         * return; }
         */

        // try and delete any previously persisted
        // file (if we've been moved to a slave this
        // file won't necessarily exist)
        if (m_persistedTrain != null && m_persistedTrain.exists()) {
          m_persistedTrain.delete();
        }

        m_persistedTrain = WekaServer.getTempFile();
        oos =
          new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(
            m_persistedTrain)));
        oos.writeObject(m_compressedTrain);
        oos.flush();

        // save memory
        m_compressedTrain = null;
      } catch (Exception ex) {
        // don't panic, just can't save our resources
        // that's all
        m_persistedTrain = null;
      } finally {
        if (oos != null) {
          try {
            oos.close();
          } catch (Exception e) {
          }
        }
      }
    }

    /**
     * Tell the task that it should load any stored resources from disk into
     * memory.
     */
    @Override
    public void loadResources() {
      if (m_persistedTrain != null && m_compressedTrain == null) {
        ObjectInputStream ois = null;
        try {
          ois =
            SerializationHelper.getObjectInputStream(new FileInputStream(
              m_persistedTrain));
          m_compressedTrain = (SerializedObject) ois.readObject();
        } catch (Exception ex) {
          // OK, we will fail to run now
        } finally {
          if (ois != null) {
            try {
              ois.close();
            } catch (Exception e) {
            }
          }
        }
      }
    }

    /**
     * Tell the task to load its result object (if it has one) from disk (if it
     * has persisted it in order to save memory). This method is called when a
     * client has requested to fetch the result.
     * 
     * @throws Exception if the result can't be loaded for some reason
     */
    @Override
    public void loadResult() throws Exception {
      if (m_persistedResult == null || !m_persistedResult.exists()) {
        throw new Exception("Result file seems to have disapeared!");
      }

      ObjectInputStream ois = null;
      try {
        ois =
          SerializationHelper.getObjectInputStream(new GZIPInputStream(
            new FileInputStream(m_persistedResult)));
        /*
         * ois = new ObjectInputStream(new BufferedInputStream(new
         * GZIPInputStream( new FileInputStream(m_persistedResult))));
         */
        List results = (List) ois.readObject();
        m_result.setTaskResult(results);
      } finally {
        if (ois != null) {
          ois.close();
        }
      }
    }

    /**
     * Tell the task to delete any disk-based resources.
     */
    @Override
    public void purge() {
      try {
        if (m_persistedTrain != null && m_persistedTrain.exists()) {
          if (!m_persistedTrain.delete()) {
            m_persistedTrain.deleteOnExit();
          }
        }
        if (m_persistedResult != null && m_persistedResult.exists()) {
          if (!m_persistedResult.delete()) {
            m_persistedResult.deleteOnExit();
          }
        }
      } catch (Exception ex) {

      }
    }
  }

  /**
   * Task for evaluating a classifier on a separate test set.
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   * @version $Revision$
   */
  protected static class SeparateTestSetTask extends ClassifierTask implements
    NamedTask, LogHandler, Serializable {

    /** For serialization */
    private static final long serialVersionUID = 2230771783733694244L;

    /** The training data */
    protected Instances m_train;

    /** The compressed training data */
    protected SerializedObject m_compressedTrain;

    /** The persisted training data */
    protected File m_persistedData;

    /** The persisited results */
    protected File m_persistedResult;

    /** The loader for loading the test data */
    protected Loader m_testLoader;

    /**
     * The file holding the test data (only one of this or the URL should be
     * non-null
     */
    protected File m_testFile;

    /** The URL from which to load the test data */
    protected String m_testURL;

    /** Index of the class in the test data */
    protected int m_testClassIndex;

    /**
     * Constructor
     * 
     * @param classifier the classifier
     * @param train the training data
     * @param testLoader the loader to load test data
     * @param testFile the file of the test data (null if URL is non-null)
     * @param testURL the URL of the test data (null if file is non-null)
     * @param testClassIndex the index of the class in the test data
     * @param saveVis whether to save the visualization data
     * @param outputCollector the collector to accumulate textual output
     * @param costMatrix cost matrix to use (if any)
     * @throws Exception
     */
    public SeparateTestSetTask(Classifier classifier, Instances train,
      Loader testLoader, File testFile, String testURL, int testClassIndex,
      boolean saveVis, AbstractOutput outputCollector, CostMatrix costMatrix)
      throws Exception {

      super(classifier, saveVis, outputCollector, costMatrix);

      m_testLoader = testLoader;
      m_testFile = testFile;
      m_testURL = testURL;
      m_testClassIndex = testClassIndex;

      m_compressedTrain = new SerializedObject(train, true);
    }

    /**
     * Run the task
     */
    @Override
    public void execute() {
      ObjectOutputStream oos = null;
      try {
        if (m_compressedTrain == null) {
          // loadResources() must have failed
          throw new Exception("Unable to load compressed data!");
        }

        // check on the status of access to the separate test file (if not URL)
        // first
        // before loading the training data up.
        if (m_testFile != null && !m_testFile.exists()) {
          throw new Exception("The test data file '"
            + m_testFile.getAbsolutePath()
            + "' does not seem to be available to the server.");
        }

        if (m_testFile != null) {
          ((FileSourcedConverter) m_testLoader).setFile(m_testFile);
        } else {
          ((URLSourcedLoader) m_testLoader).setURL(m_testURL);
        }

        // set the class index
        Instances testStructure = m_testLoader.getStructure();
        testStructure.setClassIndex(m_testClassIndex);

        // decompress the training fold
        m_train = (Instances) m_compressedTrain.getObject();
        m_compressedTrain = null;

        ClassifierErrorsPlotInstances plotInstances =
          ExplorerDefaults.getClassifierErrorsPlotInstances();

        String classifierName = m_template.getClass().getSimpleName();
        m_log.logMessage("Training classifier '" + classifierName
          + "' on training data '" + m_train.relationName() + "'");
        m_result.setExecutionStatus(TaskStatusInfo.PROCESSING);

        m_classifier = AbstractClassifier.makeCopy(m_template);
        plotInstances.setInstances(m_train);
        plotInstances.setClassifier(m_classifier);
        plotInstances.setClassIndex(m_train.classIndex());
        plotInstances.setSaveForVisualization(m_saveVis);

        if (m_outputCollector != null) {
          Instances header = new Instances(m_train, 0);
          header.setClassIndex(m_train.classIndex());
          m_outputCollector.setHeader(header);
          m_outputCollector.setBuffer(new StringBuffer());
        }

        Evaluation eval = new Evaluation(m_train, m_costMatrix);
        eval =
          ClassifierPanel.setupEval(eval, m_classifier, m_train, m_costMatrix,
            plotInstances, m_outputCollector, false);

        plotInstances.setUp();

        m_classifier.buildClassifier(m_train);

        DataSource source = new DataSource(m_testLoader);
        m_log.logMessage("Testing classifier '" + classifierName
          + "' on test set '" + testStructure.relationName() + "'");

        Instance instance;
        int jj = 0;
        while (source.hasMoreElements(testStructure)) {
          instance = source.nextElement(testStructure);
          plotInstances.process(instance, m_classifier, eval);

          if (m_outputCollector != null) {
            m_outputCollector.printClassification(m_classifier, instance, jj);
          }
          if ((++jj % 1000) == 0) {
            m_log.statusMessage("Evaluating on test data. Processed " + jj
              + " instances...");
          }
        }

        // save memory
        m_train = null;
        source = null;
        m_testLoader.reset();

        List<Object> results = new ArrayList<Object>();
        results.add(eval);
        results.add(plotInstances);
        results.add(m_outputCollector);

        m_result.setTaskResult(results);

        try {
          m_persistedResult = WekaServer.getTempFile();
          oos =
            new ObjectOutputStream(new BufferedOutputStream(
              new GZIPOutputStream(new FileOutputStream(m_persistedResult))));
          oos.writeObject(results);
          oos.flush();
          // successfully saved result - now save memory
          m_result.setTaskResult(null);
        } catch (Exception e) {
          m_persistedResult = null;
          // TODO should we set the in-memory result to null here?
          // loadResult() throws an exception if the file does not
          // exist or m_persistedResult is null, so the client can't
          // get the result anyway.
        }

        m_result.setExecutionStatus(TaskStatusInfo.FINISHED);
      } catch (Exception ex) {
        m_result.setExecutionStatus(TaskStatusInfo.FAILED);
        // log this
        StringWriter sr = new StringWriter();
        PrintWriter pr = new PrintWriter(sr);
        ex.printStackTrace(pr);
        pr.flush();
        m_log.logMessage(ex.getMessage() + "\n" + sr.getBuffer().toString());
        pr.close();
      } finally {
        // save memory
        m_train = null;
        m_outputCollector = null;
        if (oos != null) {
          try {
            oos.close();
          } catch (Exception e) {
          }
        }
      }
    }

    /**
     * Stop the task (not implemented yet)
     */
    @Override
    public void stop() {
      // TODO
    }

    /**
     * Tell the task that it should persist any resources to disk (e.g. training
     * data, etc.). WekaServer.getTempFile() can be used to get a file to save
     * to.
     */
    @Override
    public void persistResources() {
      ObjectOutputStream oos = null;
      try {
        // try and delete any previously persisted
        // file (if we've been moved to a slave this
        // file won't necessarily exist)
        if (m_persistedData != null && m_persistedData.exists()) {
          m_persistedData.delete();
        }

        m_persistedData = WekaServer.getTempFile();
        oos =
          new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(
            m_persistedData)));
        oos.writeObject(m_compressedTrain);
        oos.flush();

        // save memory
        m_compressedTrain = null;
      } catch (Exception ex) {
        // don't panic, just can't save our resources
        // that's all
        m_persistedData = null;
      } finally {
        if (oos != null) {
          try {
            oos.close();
          } catch (Exception e) {
          }
        }
      }
    }

    /**
     * Tell the task that it should load any stored resources from disk into
     * memory.
     */
    @Override
    public void loadResources() {
      if (m_persistedData != null && m_compressedTrain == null) {
        ObjectInputStream ois = null;
        try {
          ois =
            SerializationHelper.getObjectInputStream(new FileInputStream(
              m_persistedData));
          m_compressedTrain = (SerializedObject) ois.readObject();
        } catch (Exception ex) {
          // OK, we will fail to run now
        } finally {
          if (ois != null) {
            try {
              ois.close();
            } catch (Exception e) {
            }
          }
        }
      }
    }

    /**
     * Tell the task to load its result object (if it has one) from disk (if it
     * has persisted it in order to save memory). This method is called when a
     * client has requested to fetch the result.
     * 
     * @throws Exception if the result can't be loaded for some reason
     */
    @Override
    public void loadResult() throws Exception {
      if (m_persistedResult == null || !m_persistedResult.exists()) {
        throw new Exception("Result file seems to have disapeared!");
      }

      ObjectInputStream ois = null;
      try {
        ois =
          SerializationHelper.getObjectInputStream(new GZIPInputStream(
            new FileInputStream(m_persistedResult)));
        List results = (List) ois.readObject();
        m_result.setTaskResult(results);
      } finally {
        if (ois != null) {
          ois.close();
        }
      }
    }

    /**
     * Tell the task to delete any disk-based resources.
     */
    @Override
    public void purge() {
      try {
        if (m_persistedData != null && m_persistedData.exists()) {
          if (!m_persistedData.delete()) {
            m_persistedData.deleteOnExit();
          }
        }
        if (m_persistedResult != null && m_persistedResult.exists()) {
          if (!m_persistedResult.delete()) {
            m_persistedResult.deleteOnExit();
          }
        }
      } catch (Exception ex) {

      }
    }
  }

  /**
   * Task for evaluating a classifier under cross-validation. Evaluates one fold
   * of a k-fold cross-validation.
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   * @version $Revision$
   */
  protected static class FoldTask extends ClassifierTask implements NamedTask,
    LogHandler, Serializable {

    /** For serialization */
    private static final long serialVersionUID = -3028724228684581304L;

    /** The training fold to use */
    protected Instances m_trainingFold;

    /** The test fold to use */
    protected Instances m_testFold;

    /** The compressed training fold */
    protected SerializedObject m_compressedTrain;

    /** The compressed test fold */
    protected SerializedObject m_compressedTest;

    /** The persisted training and test folds */
    protected File m_persistedData;

    /** The persisted result(s) */
    protected File m_persistedResult;

    /** The number of this fold */
    protected int m_foldNumber;

    /**
     * Constructor.
     * 
     * @param classifier the classifier
     * @param trainingFold the training fold
     * @param testFold the test fold
     * @param foldNumber the number of this fold (-1 indicates this is actually
     *          a percentage split rather than a cross-val fold)
     * @param saveVis whether to save the visualization data
     * @param outputCollector the collector to use to accumulate textual results
     * @param costMatrix the cost matrix (if any)
     * @throws Exception if a problem occurs
     */
    public FoldTask(Classifier classifier, Instances trainingFold,
      Instances testFold, int foldNumber, boolean saveVis,
      AbstractOutput outputCollector, CostMatrix costMatrix) throws Exception {

      super(classifier, saveVis, outputCollector, costMatrix);

      m_compressedTrain = new SerializedObject(trainingFold, true);
      m_compressedTest = new SerializedObject(testFold, true);
    }

    /**
     * Run this task
     */
    @Override
    public void execute() {
      ObjectOutputStream oos = null;
      try {
        if (m_compressedTrain == null) {
          // loadResources() must have failed
          throw new Exception("Unable to load compressed data!");
        }

        // decompress the training fold
        m_trainingFold = (Instances) m_compressedTrain.getObject();
        m_compressedTrain = null;

        ClassifierErrorsPlotInstances plotInstances =
          ExplorerDefaults.getClassifierErrorsPlotInstances();

        String classifierName = m_template.getClass().getSimpleName();

        if (m_foldNumber > 0) {
          m_log.logMessage("Training classifier '" + classifierName
            + "' on fold " + m_foldNumber + " of '"
            + m_trainingFold.relationName() + "'");
        } else {
          // this is a percentage split
          m_log.logMessage("Building model on training split ("
            + m_trainingFold.numInstances() + " instances)...");
        }
        m_result.setExecutionStatus(TaskStatusInfo.PROCESSING);

        m_classifier = AbstractClassifier.makeCopy(m_template);
        plotInstances.setInstances(m_trainingFold);
        plotInstances.setClassifier(m_classifier);
        plotInstances.setClassIndex(m_trainingFold.classIndex());
        plotInstances.setSaveForVisualization(m_saveVis);

        if (m_outputCollector != null) {
          Instances header = new Instances(m_trainingFold, 0);
          header.setClassIndex(m_trainingFold.classIndex());
          m_outputCollector.setHeader(header);
          m_outputCollector.setBuffer(new StringBuffer());
        }

        Evaluation eval = new Evaluation(m_trainingFold, m_costMatrix);
        eval =
          ClassifierPanel.setupEval(eval, m_classifier, m_trainingFold,
            m_costMatrix, plotInstances, m_outputCollector, false);

        plotInstances.setUp();

        m_classifier.buildClassifier(m_trainingFold);
        // save memory
        m_trainingFold = null;

        // decompress the test fold
        m_testFold = (Instances) m_compressedTest.getObject();
        m_compressedTest = null;
        if (m_foldNumber > 0) {
          m_log.logMessage("Testing classifier '" + classifierName
            + "' on fold " + m_foldNumber + " of '" + m_testFold.relationName()
            + "'");
        } else {
          // this is a percentage split
          m_log.logMessage("Testing model on test split ("
            + m_testFold.numInstances() + " instances)...");
        }

        for (int i = 0; i < m_testFold.numInstances(); i++) {
          plotInstances.process(m_testFold.instance(i), m_classifier, eval);
          if (m_outputCollector != null) {
            m_outputCollector.printClassification(m_classifier,
              m_testFold.instance(i), i);
          }
        }
        m_log.logMessage("Finished testing");
        m_testFold = null;

        List<Object> results = new ArrayList<Object>();
        results.add(eval);
        results.add(plotInstances);
        results.add(m_outputCollector);

        m_result.setTaskResult(results);

        // save memory
        m_classifier = null;

        try {
          m_persistedResult = WekaServer.getTempFile();
          oos =
            new ObjectOutputStream(new BufferedOutputStream(
              new GZIPOutputStream(new FileOutputStream(m_persistedResult))));
          oos.writeObject(results);
          oos.flush();

          // successfully saved the result - now save memory
          m_result.setTaskResult(null);
        } catch (Exception e) {
          m_persistedResult = null;
          // TODO should we set the in-memory result to null here?
          // loadResult() throws an exception if the file does not
          // exist or m_persistedResult is null, so the client can't
          // get the result anyway.
        }
        m_result.setExecutionStatus(TaskStatusInfo.FINISHED);
      } catch (Exception ex) {
        m_result.setExecutionStatus(TaskStatusInfo.FAILED);
        // log this
        StringWriter sr = new StringWriter();
        PrintWriter pr = new PrintWriter(sr);
        ex.printStackTrace(pr);
        pr.flush();
        m_log.logMessage(ex.getMessage() + "\n" + sr.getBuffer().toString());
        pr.close();
      } finally {
        // save memory
        m_trainingFold = null;
        m_testFold = null;
        m_outputCollector = null;
        m_outputCollector = null;
        if (oos != null) {
          try {
            oos.close();
          } catch (Exception e) {
          }
        }
      }
    }

    /**
     * Stop this task (not implemented yet)
     */
    @Override
    public void stop() {
      // TODO
    }

    /**
     * Tell the task that it should persist any resources to disk (e.g. training
     * data, etc.). WekaServer.getTempFile() can be used to get a file to save
     * to.
     */
    @Override
    public void persistResources() {
      ObjectOutputStream oos = null;
      try {
        /*
         * if (m_persistedData != null) { // we've already saved previously - no
         * need to // do so again. Just save memory m_compressedTrain = null;
         * m_compressedTest = null;
         * 
         * return; }
         */

        // try and delete any previously persisted
        // file (if we've been moved to a slave this
        // file won't necessarily exist)
        if (m_persistedData != null && m_persistedData.exists()) {
          m_persistedData.delete();
        }

        m_persistedData = WekaServer.getTempFile();
        oos =
          new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(
            m_persistedData)));
        oos.writeObject(m_compressedTrain);
        oos.writeObject(m_compressedTest);
        oos.flush();

        // save memory
        m_compressedTrain = null;
        m_compressedTest = null;
      } catch (Exception ex) {
        // don't panic, just can't save our resources
        // that's all
        m_persistedData = null;
      } finally {
        if (oos != null) {
          try {
            oos.close();
          } catch (Exception e) {
          }
        }
      }
    }

    /**
     * Tell the task that it should load any stored resources from disk into
     * memory.
     */
    @Override
    public void loadResources() {
      if (m_persistedData != null && m_compressedTrain == null
        && m_compressedTest == null) {
        ObjectInputStream ois = null;
        try {
          ois =
            SerializationHelper.getObjectInputStream(new FileInputStream(
              m_persistedData));
          m_compressedTrain = (SerializedObject) ois.readObject();
          m_compressedTest = (SerializedObject) ois.readObject();
        } catch (Exception ex) {
          // OK, we will fail to run now
        } finally {
          if (ois != null) {
            try {
              ois.close();
            } catch (Exception e) {
            }
          }
        }
      }
    }

    /**
     * Tell the task to load its result object (if it has one) from disk (if it
     * has persisted it in order to save memory). This method is called when a
     * client has requested to fetch the result.
     * 
     * @throws Exception if the result can't be loaded for some reason
     */
    @Override
    public void loadResult() throws Exception {
      if (m_persistedResult == null || !m_persistedResult.exists()) {
        throw new Exception("Result file seems to have disapeared!");
      }

      ObjectInputStream ois = null;
      try {
        ois =
          SerializationHelper.getObjectInputStream(new GZIPInputStream(
            new FileInputStream(m_persistedResult)));
        List results = (List) ois.readObject();
        m_result.setTaskResult(results);
      } finally {
        if (ois != null) {
          ois.close();
        }
      }
    }

    /**
     * Tell the task to delete any disk-based resources.
     */
    @Override
    public void purge() {
      try {
        if (m_persistedData != null && m_persistedData.exists()) {
          if (!m_persistedData.delete()) {
            m_persistedData.deleteOnExit();
          }
        }
        if (m_persistedResult != null && m_persistedResult.exists()) {
          if (!m_persistedResult.delete()) {
            m_persistedResult.deleteOnExit();
          }
        }
      } catch (Exception ex) {

      }
    }
  }

  /**
   * Runs an evaluation. Sets up individual tasks, sends them to the server and
   * waits for results.
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   * 
   */
  protected class ExecuteThread extends Thread {

    protected List<Instances> m_testFolds = new ArrayList<Instances>();
    protected List<Instances> m_trainFolds = new ArrayList<Instances>();

    @Override
    public void run() {
      boolean allSuccessful = true;
      boolean tasksStopped = false;

      statusMessage("Setting up...");
      CostMatrix costMatrix = null;
      Instances inst = new Instances(m_classifierPanel.getInstances());
      ClassifierErrorsPlotInstances plotInstances = null;
      Instances aggregatedPlotInstances = null;
      ArrayList<Object> aggregatedPlotSizes = null;
      ArrayList<Integer> aggregatedPlotShapes = null;
      PlotData2D plotData = null;

      if (m_classifierPanel.isSelectedEvalWithRespectToCosts()) {
        costMatrix = new CostMatrix(m_classifierPanel.getCostMatrix());
      }

      boolean outputModel = m_classifierPanel.isSelectedOutputModel();
      boolean outputConfusion = m_classifierPanel.isSelectedOutputConfusion();
      boolean outputPerClass =
        m_classifierPanel.isSelectedOutputPerClassStats();
      boolean outputSummary = true;
      boolean outputEntropy = m_classifierPanel.isSelectedOutputEntropy();
      boolean saveVis = m_classifierPanel.isSelectedStorePredictions();
      boolean outputPredictionsText =
        m_classifierPanel.getClassificationOutputFormatter().getClass() != Null.class;

      String grph = null;

      int testMode = 1;
      int numFolds = 10;
      double percent = 66;

      int classIndex = m_classifierPanel.getSelectedClassIndex();
      inst.setClassIndex(classIndex);
      Classifier classifier = m_classifierPanel.getClassifier();
      Classifier template = null;
      try {
        template = AbstractClassifier.makeCopy(classifier);
      } catch (Exception ex) {
        logMessage("Problem copying classifier: " + ex.getMessage());
      }

      // TODO make a FullClassifier task
      Classifier fullClassifier = null;
      StringBuffer outBuff = new StringBuffer();
      AbstractOutput classificationOutput = null;
      if (outputPredictionsText) {
        classificationOutput =
          (AbstractOutput) m_classifierPanel.getClassificationOutputFormatter();
        Instances header = new Instances(inst, 0);
        header.setClassIndex(classIndex);
        classificationOutput.setHeader(header);
        classificationOutput.setBuffer(outBuff);
      }

      String name = (new SimpleDateFormat("HH:mm:ss - ")).format(new Date());
      String cname = "";
      String cmd = "";
      Evaluation eval = null;
      AggregateableEvaluation evalA = null;
      List<String> taskIDs = null;

      try {
        if (m_classifierPanel.isSelectedCV()) {
          // numFolds = Integer.parseInt(m_classifierPanel.m_CVText.getText());
          numFolds = m_classifierPanel.getNumCVFolds();
          if (numFolds <= 1) {
            throw new Exception("Number of folds must be greater than 1");
          }
          testMode = 1;
        } else if (m_classifierPanel.isSelectedTestOnTrain()) {
          testMode = 3;
        } else if (m_classifierPanel.isSelectedPercentageSplit()) {
          testMode = 2;
          percent = m_classifierPanel.getPercentageSplit();
          // percent =
          // Double.parseDouble(m_classifierPanel.m_PercentText.getText());
          if ((percent <= 0) || (percent >= 100)) {
            throw new Exception("Percentage must be between 0 and 100");
          }
        } else if (m_classifierPanel.isSelectedSeparateTestSet()) {
          testMode = 4;
        }

        cname = classifier.getClass().getName();
        if (cname.startsWith("weka.classifiers.")) {
          name += cname.substring("weka.classifiers.".length());
        } else {
          name += cname;
        }
        name += " (Remote)";
        cmd = classifier.getClass().getName();
        if (classifier instanceof OptionHandler) {
          cmd +=
            " " + Utils.joinOptions(((OptionHandler) classifier).getOptions());
        }

        // set up the structure of the plottable instances for
        // visualization
        plotInstances = ExplorerDefaults.getClassifierErrorsPlotInstances();
        plotInstances.setInstances(inst);
        plotInstances.setClassifier(classifier);
        plotInstances.setClassIndex(inst.classIndex());
        plotInstances.setSaveForVisualization(saveVis);

        // Output some header information
        logMessage("Started remote execution of " + cname);
        logMessage("Command: " + cmd);

        outBuff.append("=== Run information ===\n\n");
        outBuff.append("Scheme:       " + cname);
        if (classifier instanceof OptionHandler) {
          String[] o = ((OptionHandler) classifier).getOptions();
          outBuff.append(" " + Utils.joinOptions(o));
        }
        outBuff.append("\n");
        outBuff.append("Relation:     " + inst.relationName() + '\n');
        outBuff.append("Instances:    " + inst.numInstances() + '\n');
        outBuff.append("Attributes:   " + inst.numAttributes() + '\n');
        if (inst.numAttributes() < 100) {
          for (int i = 0; i < inst.numAttributes(); i++) {
            outBuff.append("              " + inst.attribute(i).name() + '\n');
          }
        } else {
          outBuff.append("              [list of attributes omitted]\n");
        }
        outBuff.append("Test mode:    ");

        if (costMatrix != null) {
          outBuff.append("Evaluation cost matrix:\n")
            .append(costMatrix.toString()).append("\n");
        }
        outBuff.append("\n");
        m_classifierPanel.getResultHistory().addResult(name, outBuff);
        m_classifierPanel.getResultHistory().setSingle(name);

        if (testMode == 4) {
          Loader testLoader = m_classifierPanel.getSeparateTestSetLoader();
          File sourceFile = null;
          String url = null;
          if (testLoader instanceof FileSourcedConverter) {
            sourceFile = ((FileSourcedConverter) testLoader).retrieveFile();
            if (sourceFile == null || sourceFile.isDirectory()) {
              sourceFile = null;
            }
          }
          if (testLoader instanceof URLSourcedLoader) {
            url = ((URLSourcedLoader) testLoader).retrieveURL();
            if (url.equals("http://")) {
              url = null;
            }
          }
          if (url == null && sourceFile == null) {
            throw new Exception("No test file/url has been configured!");
          }
          Instances testStructure = testLoader.getStructure();
          int testClassIndex = m_classifierPanel.getSeparateTestSetClassIndex();
          testStructure.setClassIndex(testClassIndex);

          if (!(template instanceof InputMappedClassifier)) {
            if (!inst.equalHeaders(testStructure)) {
              statusMessage("Train and test sets differ in structure - wrapping "
                + "classifier");
              logMessage("Train and test sets differ in structure - wrapping the "
                + "classifier up in an InputMappedClassifier");
              InputMappedClassifier temp = new InputMappedClassifier();
              temp.setClassifier(template);
              temp.setTestStructure(testStructure);
              template = temp;
            }
          }

          if (testLoader instanceof IncrementalConverter) {
            outBuff.append("user supplied test set: "
              + " size unknown (reading incrementally)\n");
          } else {
            outBuff.append("user supplied test set: "
              + testLoader.getDataSet().numInstances() + " instances\n");
          }

          statusMessage("Evaluating on test data...");
          eval = new Evaluation(inst, costMatrix);
          // make adjustments if the classifier is an InputMappedClassifier
          eval =
            ClassifierPanel.setupEval(eval, template, inst, costMatrix,
              plotInstances, classificationOutput, false);
          // copy the setup
          evalA = new AggregateableEvaluation(eval);

          plotInstances.setUp();

          if (outputPredictionsText) {
            printPredictionsHeader(outBuff, classificationOutput, "test split");
          }

          taskIDs =
            runSeparateTestSet(template, inst, testLoader, sourceFile, url,
              testClassIndex, saveVis, outputPredictionsText, costMatrix);

          if (taskIDs == null) {
            outBuff
              .append("\n\nThere was a problem sending separate test set task to the "
                + "server - check the log.");
            allSuccessful = false;
          } else {
            TaskMonitor tm = new TaskMonitor();
            statusMessage("Waiting for remote task to complete...");
            logMessage("Waiting for remote task to complete...");
            if (!tm.monitorTasks(taskIDs)) {
              outBuff
                .append("\n\nA problem occurred during the execution of the separate test set "
                  + "task on the server - check the log.");
              allSuccessful = false;
            } else {
              statusMessage("Retrieving classification results for separate test set task"
                + " from the server...");
              TaskStatusInfo classifierResult =
                getResultFromServer(taskIDs.get(0));
              if (classifierResult == null
                || classifierResult.getTaskResult() == null) {
                outBuff
                  .append("\n\nA problem occurred while trying to retrieve "
                    + "a task result from the server - check the log.");
                allSuccessful = false;
              } else {
                List results = (List) classifierResult.getTaskResult();
                Evaluation taskEval = (Evaluation) results.get(0);
                evalA.aggregate(taskEval);

                if (results.size() > 1 && plotInstances != null && saveVis) {
                  ClassifierErrorsPlotInstances taskPlot =
                    (ClassifierErrorsPlotInstances) results.get(1);
                  aggregatedPlotInstances =
                    new Instances(taskPlot.getPlotInstances());
                  aggregatedPlotShapes = taskPlot.getPlotShapes();
                  aggregatedPlotSizes = taskPlot.getPlotSizes();
                }

                if (results.size() > 2 && classificationOutput != null) {
                  AbstractOutput taskOut = (AbstractOutput) results.get(2);
                  if (taskOut != null) {
                    classificationOutput.getBuffer()
                      .append(taskOut.getBuffer());
                  }
                }
              }
            }
          }
        } else if (testMode == 2) {
          outBuff.append("split " + percent + "% train, remainder test\n");

          statusMessage("Evaluating on percentage split...");

          eval = new Evaluation(inst, costMatrix);
          // make adjustments if the classifier is an InputMappedClassifier
          eval =
            ClassifierPanel.setupEval(eval, classifier, inst, costMatrix,
              plotInstances, classificationOutput, false);
          // copy the setup
          evalA = new AggregateableEvaluation(eval);

          plotInstances.setUp();

          if (outputPredictionsText) {
            printPredictionsHeader(outBuff, classificationOutput, "test split");
          }

          taskIDs =
            runPercentageSplit(template, inst, percent, saveVis,
              outputPredictionsText, costMatrix);

          if (taskIDs == null) {
            outBuff
              .append("\n\nThere was a problem sending percentage split task to the "
                + "server - check the log.");
            allSuccessful = false;
          } else {
            TaskMonitor tm = new TaskMonitor();
            statusMessage("Waiting for remote task to complete...");
            logMessage("Waiting for remote task to complete...");
            if (!tm.monitorTasks(taskIDs)) {
              outBuff
                .append("\n\nA problem occurred during the execution of the percentage split "
                  + "task on the server - check the log.");
              allSuccessful = false;
            } else {
              statusMessage("Retrieving classification results for test on train"
                + " from the server...");
              TaskStatusInfo classifierResult =
                getResultFromServer(taskIDs.get(0));
              if (classifierResult == null
                || classifierResult.getTaskResult() == null) {
                outBuff
                  .append("\n\nA problem occurred while trying to retrieve "
                    + "a task result from the server - check the log.");
                allSuccessful = false;
              } else {
                List results = (List) classifierResult.getTaskResult();
                Evaluation taskEval = (Evaluation) results.get(0);
                evalA.aggregate(taskEval);

                if (results.size() > 1 && plotInstances != null && saveVis) {
                  ClassifierErrorsPlotInstances taskPlot =
                    (ClassifierErrorsPlotInstances) results.get(1);
                  aggregatedPlotInstances =
                    new Instances(taskPlot.getPlotInstances());
                  aggregatedPlotShapes = taskPlot.getPlotShapes();
                  aggregatedPlotSizes = taskPlot.getPlotSizes();
                }

                if (results.size() > 2 && classificationOutput != null) {
                  AbstractOutput taskOut = (AbstractOutput) results.get(2);
                  if (taskOut != null) {
                    classificationOutput.getBuffer()
                      .append(taskOut.getBuffer());
                  }
                }
              }
            }
          }
        } else if (outputModel && testMode == 3) {
          outBuff.append("evaluate on training data\n");

          // launch full classifier test on train task
          statusMessage("Evaluating on training data...");
          eval = new Evaluation(inst, costMatrix);
          // make adjustments if the classifier is an InputMappedClassifier
          eval =
            ClassifierPanel.setupEval(eval, classifier, inst, costMatrix,
              plotInstances, classificationOutput, false);
          // copy the setup
          evalA = new AggregateableEvaluation(eval);

          plotInstances.setUp();

          if (outputPredictionsText) {
            printPredictionsHeader(outBuff, classificationOutput,
              "training set");
          }

          taskIDs =
            runTestOnTrain(template, inst, saveVis, outputPredictionsText,
              costMatrix, outputModel);
          if (taskIDs == null) {
            outBuff
              .append("\n\nThere was a problem sending test on train task to the "
                + "server - check the log.");
            allSuccessful = false;
          } else {
            TaskMonitor tm = new TaskMonitor();
            statusMessage("Waiting for remote task to complete...");
            logMessage("Waiting for remote task to complete...");
            if (!tm.monitorTasks(taskIDs)) {
              outBuff
                .append("\n\nA problem occurred during the execution of the test on train "
                  + "task on the server - check the log.");
              allSuccessful = false;
            } else {
              statusMessage("Retrieving classification results for test on train"
                + " from the server...");
              TaskStatusInfo classifierResult =
                getResultFromServer(taskIDs.get(0));
              if (classifierResult == null
                || classifierResult.getTaskResult() == null) {
                outBuff
                  .append("\n\nA problem occurred while trying to retrieve "
                    + "a task result from the server - check the log.");
                allSuccessful = false;
              } else {
                List results = (List) classifierResult.getTaskResult();
                Evaluation taskEval = (Evaluation) results.get(0);
                evalA.aggregate(taskEval);

                if (results.size() > 1 && plotInstances != null && saveVis) {
                  ClassifierErrorsPlotInstances taskPlot =
                    (ClassifierErrorsPlotInstances) results.get(1);
                  aggregatedPlotInstances =
                    new Instances(taskPlot.getPlotInstances());
                  aggregatedPlotShapes = taskPlot.getPlotShapes();
                  aggregatedPlotSizes = taskPlot.getPlotSizes();
                }

                if (results.size() > 2 && classificationOutput != null) {
                  AbstractOutput taskOut = (AbstractOutput) results.get(2);
                  if (taskOut != null) {
                    classificationOutput.getBuffer()
                      .append(taskOut.getBuffer());
                  }
                }

                if (results.size() > 3 && outputModel) {
                  fullClassifier = (Classifier) results.get(3);
                  outBuff
                    .append("=== Classifier model (full training set) ===\n\n");
                  outBuff.append(fullClassifier.toString() + "\n");
                  m_classifierPanel.getResultHistory().updateResult(name);
                }
              }
            }
          }
        } else if (testMode == 1) {

          outBuff.append("" + numFolds + "-fold cross-validation\n");

          statusMessage("Randomizing instances...");
          int rnd = 1;
          try {
            rnd = m_classifierPanel.getRandomSeed();
            // Integer.parseInt(m_classifierPanel.m_RandomSeedText.getText()
            // .trim());
            // System.err.println("Using random seed "+rnd);
          } catch (Exception ex) {
            m_classifierPanel.getLog().logMessage(
              "Trouble parsing random seed value");
            rnd = 1;
          }
          Random random = new Random(rnd);
          inst.randomize(random);
          if (inst.attribute(classIndex).isNominal()) {
            statusMessage("Stratifying instances...");
            inst.stratify(numFolds);
          }
          eval = new Evaluation(inst, costMatrix);
          // make adjustments if the classifier is an InputMappedClassifier
          eval =
            ClassifierPanel.setupEval(eval, classifier, inst, costMatrix,
              plotInstances, classificationOutput, false);
          // copy the setup
          evalA = new AggregateableEvaluation(eval);

          plotInstances.setUp();

          if (outputPredictionsText) {
            printPredictionsHeader(outBuff, classificationOutput, "test data");
          }

          taskIDs =
            runCV(template, inst, numFolds, random, saveVis,
              outputPredictionsText, costMatrix);
          if (taskIDs == null) {
            outBuff.append("\n\nThere was a problem sending fold tasks to the "
              + "server - check the log.");
            allSuccessful = false;
          } else {
            TaskMonitor tm = new TaskMonitor();
            statusMessage("Waiting for remote tasks to complete...");
            logMessage("Waiting for remote tasks to complete...");
            if (!tm.monitorTasks(taskIDs)) {
              outBuff
                .append("\n\nA problem occurred during the execution of the fold "
                  + "tasks on the server - check the log.");
              allSuccessful = false;
            } else {
              // collect the predictions and evaluation stats

              for (int i = 0; i < taskIDs.size(); i++) {
                statusMessage("Retrieving classification results for fold "
                  + (i + 1) + " from the server...");
                TaskStatusInfo classifierResult =
                  getResultFromServer(taskIDs.get(i));
                if (classifierResult == null
                  || classifierResult.getTaskResult() == null) {
                  outBuff
                    .append("\n\nA problem occurred while trying to retrieve "
                      + "a task result from the server - check the log.");
                  allSuccessful = false;
                  break;
                }
                List results = (List) classifierResult.getTaskResult();
                Evaluation foldEval = (Evaluation) results.get(0);
                evalA.aggregate(foldEval);

                if (results.size() > 1 && plotInstances != null && saveVis) {
                  ClassifierErrorsPlotInstances foldPlot =
                    (ClassifierErrorsPlotInstances) results.get(1);

                  if (aggregatedPlotInstances == null) {
                    aggregatedPlotInstances =
                      new Instances(foldPlot.getPlotInstances());
                    aggregatedPlotShapes = foldPlot.getPlotShapes();
                    aggregatedPlotSizes = foldPlot.getPlotSizes();
                  } else {
                    Instances temp = foldPlot.getPlotInstances();
                    for (int j = 0; j < temp.numInstances(); j++) {
                      aggregatedPlotInstances.add(temp.get(j));
                      aggregatedPlotShapes.add(foldPlot.getPlotShapes().get(j));
                      aggregatedPlotSizes.add(foldPlot.getPlotSizes().get(j));
                    }
                  }
                }

                if (results.size() > 2 && classificationOutput != null) {
                  AbstractOutput foldOut = (AbstractOutput) results.get(2);
                  if (foldOut != null) {
                    classificationOutput.getBuffer()
                      .append(foldOut.getBuffer());
                  }
                }

              }
            }

            //
          }
        }

        if (allSuccessful) {
          if (aggregatedPlotInstances != null) {
            plotData = new PlotData2D(aggregatedPlotInstances);
            plotData.setShapeSize(aggregatedPlotSizes);
            plotData.setShapeType(aggregatedPlotShapes);
            plotData.setPlotName(cname + " (" + inst.relationName() + ")");
          } else {
            plotData = plotInstances.getPlotData(cname);
          }

          if (outputPredictionsText) {
            classificationOutput.printFooter();
            outBuff.append("\n");
          }

          if (testMode == 1) {
            if (inst.attribute(classIndex).isNominal()) {
              outBuff.append("=== Stratified cross-validation ===\n");
            } else {
              outBuff.append("=== Cross-validation ===\n");
            }
          } else if (testMode == 3) {
            outBuff.append("=== Evaluation on training set ===\n");
          }

          if (outputSummary) {
            outBuff.append(evalA.toSummaryString(outputEntropy) + "\n");
          }

          if (inst.attribute(classIndex).isNominal()) {
            if (outputPerClass) {
              outBuff.append(evalA.toClassDetailsString() + "\n");
            }

            if (outputConfusion) {
              outBuff.append(evalA.toMatrixString() + "\n");
            }
          }

          if (fullClassifier != null && (fullClassifier instanceof Sourcable)
            && m_classifierPanel.isSelectedOutputSourceCode()) {
            outBuff.append("=== Source code ===\n\n");
            outBuff.append(Evaluation.wekaStaticWrapper(
              ((Sourcable) fullClassifier),
              m_classifierPanel.getSourceCodeClassName()));
          }

          m_classifierPanel.getResultHistory().updateResult(name);
          logMessage("Finished " + cname);
          m_classifierPanel.getLog().statusMessage("OK");
        }
      } catch (Exception ex) {
        logMessage(ex.getMessage());
        ex.printStackTrace();
        allSuccessful = false;
      } finally {
        if (tasksStopped) {
          statusMessage("Task(s) stopped.");
          logMessage("Task(s) stopped.");
        } else if (!allSuccessful) {
          JOptionPane.showMessageDialog(ClassifierPanelRemoteLauncher.this,
            "Problem evaluating classifier - see log.", "Evaluate classifier",
            JOptionPane.ERROR_MESSAGE);
        } else {
          try {
            if (!saveVis && outputModel) {

              ArrayList<Object> vv = new ArrayList<Object>();
              vv.add(fullClassifier);
              Instances trainHeader =
                new Instances(m_classifierPanel.getInstances(), 0);
              trainHeader.setClassIndex(classIndex);
              vv.add(trainHeader);
              if (grph != null) {
                vv.add(grph);
              }
              m_classifierPanel.getResultHistory().addObject(name, vv);
            } else if (saveVis && plotData != null
              && plotData.getPlotInstances().numInstances() > 0) {
              m_classifierPanel
                .setCurrentVisualization(new weka.gui.visualize.VisualizePanel());
              m_classifierPanel.getCurrentVisualization().setName(
                name + " (" + inst.relationName() + ")");
              m_classifierPanel.getCurrentVisualization().setLog(
                m_classifierPanel.getLog());
              m_classifierPanel.getCurrentVisualization().addPlot(plotData);
              // m_CurrentVis.setColourIndex(plotInstances.getPlotInstances().classIndex()+1);
              m_classifierPanel.getCurrentVisualization().setColourIndex(
                plotData.getPlotInstances().classIndex());
              plotInstances.cleanUp();

              ArrayList<Object> vv = new ArrayList<Object>();
              if (outputModel) {
                vv.add(fullClassifier);
                Instances trainHeader =
                  new Instances(m_classifierPanel.getInstances(), 0);
                trainHeader.setClassIndex(classIndex);
                vv.add(trainHeader);
                if (grph != null) {
                  vv.add(grph);
                }
              }
              vv.add(m_classifierPanel.getCurrentVisualization());

              if ((evalA != null) && (evalA.predictions() != null)) {
                vv.add(evalA.predictions());
                vv.add(inst.classAttribute());
              }
              m_classifierPanel.getResultHistory().addObject(name, vv);
            }

            // purge all the tasks from the server.
            if (taskIDs != null && taskIDs.size() > 0) {
              statusMessage("Purging completed tasks from server");
              logMessage("Purging completed tasks from server");
              purgeTasksFromServer(taskIDs);
              m_classifierPanel.getLog().statusMessage("OK");
            }
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        }
      }
    }

    /**
     * Configure and launch a separate test set task
     * 
     * @param template classifier template
     * @param inst the instances
     * @param testLoader the loader to use for the test data
     * @param sourceFile the file to load test data from
     * @param url the URL to load test data from (null if file is non-null)
     * @param testClassIndex the index of the class attribute in the test data
     * @param saveVis whether to save the visualization data or not
     * @param outputPredictionsText whether to output predictions for individual
     *          instances
     * @param costMatrix the cost matrix (if any) to use
     * @return a list of IDs of running tasks
     */
    protected List<String> runSeparateTestSet(Classifier template,
      Instances inst, Loader testLoader, File sourceFile, String url,
      int testClassIndex, boolean saveVis, boolean outputPredictionsText,
      CostMatrix costMatrix) {
      List<String> taskIDs = new ArrayList<String>();

      Classifier current = null;
      try {
        current = AbstractClassifier.makeCopy(template);
      } catch (Exception ex) {
        logMessage("Problem copying classifier: " + ex.getMessage());
      }

      AbstractOutput outputCollector = null;
      if (outputPredictionsText) {
        try {
          outputCollector =
            (AbstractOutput) m_classifierPanel
              .getClassificationOutputFormatter().getClass().newInstance();
        } catch (InstantiationException e) {
          e.printStackTrace();
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        }
      }

      SeparateTestSetTask tTask = null;
      try {
        tTask =
          new SeparateTestSetTask(current, inst, testLoader, sourceFile, url,
            testClassIndex, saveVis, outputCollector, costMatrix);

      } catch (Exception ex) {
        ex.printStackTrace();
        taskIDs = null;
      }

      tTask.setName(current.getClass().getSimpleName() + "-TestSet");
      String taskID = sendTaskToServer(tTask);

      if (taskID != null && taskID.length() > 0) {
        taskIDs.add(taskID);
      } else {
        // bail out here...
        taskIDs = null;
      }

      if (m_classifierPanel.getLog() instanceof TaskLogger) {
        ((TaskLogger) m_classifierPanel.getLog()).taskStarted();
      }

      return taskIDs;
    }

    /**
     * Configure and launch a percentage split task
     * 
     * @param template the classifier template
     * @param inst the data
     * @param percent percentage of the data for training
     * @param saveVis whether to save the visualization data or not
     * @param outputPredictionsText whether to output predictions for individual
     *          instances
     * @param costMatrix the cost matrix to use (if any)
     * @return a list of IDs for running tasks
     */
    protected List<String> runPercentageSplit(Classifier template,
      Instances inst, double percent, boolean saveVis,
      boolean outputPredictionsText, CostMatrix costMatrix) {

      List<String> taskIDs = new ArrayList<String>();
      Classifier current = null;
      try {
        current = AbstractClassifier.makeCopy(template);
      } catch (Exception ex) {
        logMessage("Problem copying classifier: " + ex.getMessage());
      }

      AbstractOutput outputCollector = null;
      if (outputPredictionsText) {
        try {
          outputCollector =
            (AbstractOutput) m_classifierPanel
              .getClassificationOutputFormatter().getClass().newInstance();
        } catch (InstantiationException e) {
          e.printStackTrace();
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        }
      }

      if (!m_classifierPanel.isSelectedPreserveOrder()) {
        statusMessage("Randomizing instances...");
        int rnd = 1;
        try {
          rnd = m_classifierPanel.getRandomSeed();
        } catch (Exception ex) {
          logMessage("Trouble parsing random seed value");
        }
        inst.randomize(new Random(rnd));
      }

      int trainSize = (int) Math.round(inst.numInstances() * percent / 100);
      int testSize = inst.numInstances() - trainSize;
      Instances train = new Instances(inst, 0, trainSize);
      Instances test = new Instances(inst, trainSize, testSize);

      FoldTask fTask = null;
      try {
        fTask =
          new FoldTask(current, train, test, -1, saveVis, outputCollector,
            costMatrix);
      } catch (Exception ex) {
        ex.printStackTrace();
        taskIDs = null;
      }
      fTask.setName(current.getClass().getSimpleName() + "-PercentSplit_"
        + percent);
      String taskID = sendTaskToServer(fTask);

      if (taskID != null && taskID.length() > 0) {
        taskIDs.add(taskID);
      } else {
        // bail out here...
        taskIDs = null;
      }

      if (m_classifierPanel.getLog() instanceof TaskLogger) {
        ((TaskLogger) m_classifierPanel.getLog()).taskStarted();
      }

      return taskIDs;
    }

    /**
     * Configure an launch a test on train task
     * 
     * @param template the classifier template
     * @param inst the data
     * @param saveVis whether to save the visualization data
     * @param outputPredictionsText whether to output predictions for individual
     *          instances
     * @param costMatrix the cost matrix to use (if any)
     * @param outputModel whether to output/return the model
     * @return a list of IDs of running tasks
     */
    protected List<String> runTestOnTrain(Classifier template, Instances inst,
      boolean saveVis, boolean outputPredictionsText, CostMatrix costMatrix,
      boolean outputModel) {

      List<String> taskIDs = new ArrayList<String>();
      Classifier current = null;
      try {
        current = AbstractClassifier.makeCopy(template);
      } catch (Exception ex) {
        logMessage("Problem copying classifier: " + ex.getMessage());
      }

      AbstractOutput outputCollector = null;
      if (outputPredictionsText) {
        try {
          outputCollector =
            (AbstractOutput) m_classifierPanel
              .getClassificationOutputFormatter().getClass().newInstance();
        } catch (InstantiationException e) {
          e.printStackTrace();
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        }
      }

      TestOnTrainTask ttask = null;
      try {
        ttask =
          new TestOnTrainTask(current, inst, saveVis, outputCollector,
            costMatrix, outputModel);
      } catch (Exception ex) {
        ex.printStackTrace();
        taskIDs = null;
      }
      ttask.setName(current.getClass().getSimpleName() + "_test_on_train");
      String taskID = sendTaskToServer(ttask);
      if (taskID != null && taskID.length() > 0) {
        taskIDs.add(taskID);
      } else {
        // bail out here...
        taskIDs = null;
      }
      if (m_classifierPanel.getLog() instanceof TaskLogger) {
        ((TaskLogger) m_classifierPanel.getLog()).taskStarted();
      }

      return taskIDs;
    }

    /**
     * Configure and launch a set of cross-validation fold tasks
     * 
     * @param template the classifier template
     * @param inst the data
     * @param numFolds the number of folds (i.e tasks to create)
     * @param random a random number generator for creating folds
     * @param saveVis whether to save the visualization data or not
     * @param outputPredictionsText whether to output predictions for individual
     *          instances
     * @param costMatrix the cost matrix to use (if any)
     * @return a list of IDs of running tasks
     */
    protected List<String> runCV(Classifier template, Instances inst,
      int numFolds, Random random, boolean saveVis,
      boolean outputPredictionsText, CostMatrix costMatrix) {

      List<String> taskIDs = new ArrayList<String>();
      for (int fold = 0; fold < numFolds; fold++) {
        statusMessage("Creating splits for fold " + (fold + 1) + "...");

        Instances train = inst.trainCV(numFolds, fold, random);
        Instances test = inst.testCV(numFolds, fold);
        m_trainFolds.add(train);
        m_testFolds.add(test);

        Classifier current = null;
        try {
          current = AbstractClassifier.makeCopy(template);
        } catch (Exception ex) {
          logMessage("Problem copying classifier: " + ex.getMessage());
        }

        AbstractOutput outputCollector = null;
        if (outputPredictionsText) {
          try {
            outputCollector =
              (AbstractOutput) m_classifierPanel
                .getClassificationOutputFormatter().getClass().newInstance();
          } catch (InstantiationException e) {
            e.printStackTrace();
          } catch (IllegalAccessException e) {
            e.printStackTrace();
          }
        }
        FoldTask fTask = null;
        try {
          fTask =
            new FoldTask(current, train, test, fold + 1, saveVis,
              outputCollector, costMatrix);
        } catch (Exception ex) {
          ex.printStackTrace();
          taskIDs = null;
          break;
        }
        fTask.setName(current.getClass().getSimpleName() + "-CV_fold_"
          + (fold + 1));
        String taskID = sendTaskToServer(fTask);
        if (taskID != null && taskID.length() > 0) {
          taskIDs.add(taskID);
        } else {
          // bail out here...
          taskIDs = null;
          break;
        }

        if (m_classifierPanel.getLog() instanceof TaskLogger) {
          ((TaskLogger) m_classifierPanel.getLog()).taskStarted();
        }
      }

      // TODO will probably return a boolean for alls well
      // TODO Note, we should only fetch a fold's result (classifier) when
      // we are to process its predictions (i.e. one at a time so
      // as to be memory efficient
      return taskIDs;
    }
  } // FoldThread

  protected String constructURL(String serviceAndArguments) {
    String realHostname = m_hostPanel.getHostName();
    String realPort = m_hostPanel.getPort();
    try {
      realHostname =
        Environment.getSystemWide().substitute(m_hostPanel.getHostName());
      realPort = Environment.getSystemWide().substitute(m_hostPanel.getPort());
    } catch (Exception ex) {
    }

    if (realPort.equals("80")) {
      realPort = "";
    } else {
      realPort = ":" + realPort;
    }

    String retVal = "http://" + realHostname + realPort + serviceAndArguments;

    retVal = retVal.replace(" ", "%20");

    return retVal;
  }

  /**
   * Send a task to the server
   * 
   * @param task the task to send
   * @return the task ID returned by the server
   */
  protected String sendTaskToServer(NamedTask task) {

    InputStream is = null;
    PostMethod post = null;
    String taskID = null;

    try {
      byte[] serializedTask = WekaServer.serializeTask(task);

      String service = ExecuteTaskServlet.CONTEXT_PATH + "/?client=Y";
      String url = constructURL(service);
      post = new PostMethod(url);
      RequestEntity entity = new ByteArrayRequestEntity(serializedTask);
      post.setRequestEntity(entity);

      post.setDoAuthentication(true);
      post.addRequestHeader(new Header("Content-Type",
        "application/octet-stream"));

      // Get HTTP client
      HttpClient client =
        WekaServer.ConnectionManager.getSingleton().createHttpClient();
      WekaServer.ConnectionManager.addCredentials(client, m_username,
        m_password);

      int result = client.executeMethod(post);
      if (result == 401) {
        logMessage("Unable to send fold task to server - authentication "
          + "required");
      } else {
        is = post.getResponseBodyAsStream();
        ObjectInputStream ois = SerializationHelper.getObjectInputStream(is);
        // System.out.println("Number of bytes in response " + ois.available());
        Object response = ois.readObject();
        if (response.toString().startsWith(WekaServlet.RESPONSE_ERROR)) {
          logMessage("A problem occurred at the sever : \n" + "\t"
            + response.toString());
        } else {
          taskID = response.toString();
        }
      }
    } catch (Exception ex) {
      logMessage("An error occurred while sending fold task to server: "
        + ex.getMessage());
      ex.printStackTrace();
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (Exception e) {
        }
      }

      if (post != null) {
        // Release current connection to the connection pool
        post.releaseConnection();
      }
    }

    return taskID;
  }

  /**
   * Purge the supplied tasks from the server
   * 
   * @param taskIDs a list of task IDs of tasks to purge
   */
  protected void purgeTasksFromServer(List<String> taskIDs) {
    InputStream is = null;
    PostMethod post = null;
    StringBuffer tasks = new StringBuffer();
    for (String id : taskIDs) {
      tasks.append(id + ",");
    }
    String taskList = tasks.substring(0, tasks.lastIndexOf(","));

    try {
      String service =
        PurgeTaskServlet.CONTEXT_PATH + "/?name=" + taskList + "&client=Y";
      post = new PostMethod(constructURL(service));
      post.setDoAuthentication(true);
      post.addRequestHeader(new Header("Content-Type", "text/plain"));

      // Get HTTP client
      HttpClient client =
        WekaServer.ConnectionManager.getSingleton().createHttpClient();
      WekaServer.ConnectionManager.addCredentials(client, m_username,
        m_password);

      int result = client.executeMethod(post);
      if (result == 401) {
        logMessage("Unable to purge tasks from server - authentication "
          + "required");
      } else {
        is = post.getResponseBodyAsStream();
        ObjectInputStream ois =
          SerializationHelper.getObjectInputStream(new BufferedInputStream(is));
        Object response = ois.readObject();
        if (response.toString().startsWith(WekaServlet.RESPONSE_ERROR)) {

          logMessage("Server returned an error: "
            + "while trying to purge completed tasks: '" + taskList + "'. ("
            + response.toString() + ")." + " Check logs on server.");
        }
      }
    } catch (Exception ex) {
      logMessage("An error occurred while trying to purge completed tasks from "
        + "server: " + ex.getMessage());
      ex.printStackTrace();
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (Exception e) {
        }
      }

      if (post != null) {
        // Release current connection to the connection pool
        post.releaseConnection();
      }
    }
  }

  /**
   * Get a result from the server
   * 
   * @param taskID the ID of the task to retrieve the result for
   * @return the TaskStatusInfo object encapsulating the result
   */
  protected TaskStatusInfo getResultFromServer(String taskID) {
    InputStream is = null;
    PostMethod post = null;
    TaskStatusInfo resultInfo = null;

    try {
      String service =
        GetTaskResultServlet.CONTEXT_PATH + "/?name=" + taskID + "&client=Y";
      post = new PostMethod(constructURL(service));
      post.setDoAuthentication(true);
      post.addRequestHeader(new Header("Content-Type", "text/plain"));

      // Get HTTP client
      HttpClient client =
        WekaServer.ConnectionManager.getSingleton().createHttpClient();
      WekaServer.ConnectionManager.addCredentials(client, m_username,
        m_password);

      int result = client.executeMethod(post);
      if (result == 401) {
        logMessage("Unable to retrieve task from server - authentication "
          + "required");
      } else {
        is = post.getResponseBodyAsStream();
        ObjectInputStream ois =
          SerializationHelper.getObjectInputStream(new BufferedInputStream(
            new GZIPInputStream(is)));
        Object response = ois.readObject();
        if (response.toString().startsWith(WekaServlet.RESPONSE_ERROR)) {

          logMessage("Server returned an error: "
            + "while trying to retrieve task result for task: '" + taskID
            + "'. (" + response.toString() + ")." + " Check logs on server.");
        } else {
          resultInfo = ((TaskStatusInfo) response);
        }
      }
    } catch (Exception ex) {
      logMessage("An error occurred while trying to retrieve task result from "
        + "server: " + ex.getMessage());
      ex.printStackTrace();
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (Exception e) {
        }
      }

      if (post != null) {
        // Release current connection to the connection pool
        post.releaseConnection();
      }
    }

    return resultInfo;
  }

  /**
   * outputs the header for the predictions on the data.
   *
   * @param outBuff the buffer to add the output to
   * @param classificationOutput for generating the classification output
   * @param title the title to print
   */
  protected static void printPredictionsHeader(StringBuffer outBuff,
    AbstractOutput classificationOutput, String title) {
    if (classificationOutput.generatesOutput()) {
      outBuff.append("=== Predictions on " + title + " ===\n\n");
    }
    classificationOutput.printHeader();
  }

  protected synchronized void launchRemote() {
    if (m_classifierPanel.isSelectedCV()
      || m_classifierPanel.isSelectedTestOnTrain()
      || m_classifierPanel.isSelectedPercentageSplit()) {
      ExecuteThread eThread = new ExecuteThread();
      eThread.setPriority(Thread.MIN_PRIORITY);
      logMessage("Starting remote execution...");
      eThread.start();
    } else {
      if (m_classifierPanel.getSeparateTestSetLoader() != null) {
        ExecuteThread eThread = new ExecuteThread();
        eThread.setPriority(Thread.MIN_PRIORITY);
        logMessage("Starting remote execution...");
        eThread.start();
      }
    }
  }

  protected void popupUI() {
    // m_launchButton.setEnabled(false);

    m_popupD =
      new JDialog((java.awt.Frame) m_classifierPanel.getTopLevelAncestor(),
        true);
    m_popupD.setLayout(new BorderLayout());
    m_popupD.getContentPane().add(this, BorderLayout.CENTER);

    m_popupD.addWindowListener(new java.awt.event.WindowAdapter() {
      @Override
      public void windowClosing(java.awt.event.WindowEvent e) {
        m_popupD.dispose();
        m_popupD = null;
        // m_launchButton.setEnabled(true);
      }
    });
    m_popupD.pack();
    m_popupD.setLocationRelativeTo(m_classifierPanel);
    m_popupD.setVisible(true);
  }
}
