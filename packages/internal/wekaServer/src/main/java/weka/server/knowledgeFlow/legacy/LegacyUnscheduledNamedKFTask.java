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
 *    UnscheduledNamedTask.java
 *    Copyright (C) 2011-2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.server.knowledgeFlow.legacy;

import weka.core.Environment;
import weka.core.LogHandler;
import weka.experiment.TaskStatusInfo;
import weka.gui.Logger;
import weka.gui.beans.BeanCommon;
import weka.gui.beans.BeanConnection;
import weka.gui.beans.BeanInstance;
import weka.gui.beans.FlowRunner;
import weka.gui.beans.HeadlessEventCollector;
import weka.gui.beans.xml.XMLBeans;
import weka.server.NamedTask;
import weka.server.WekaServer;
import weka.server.WekaTaskMap;
import weka.server.logging.ServerLogger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 10248 $
 */
public class LegacyUnscheduledNamedKFTask implements NamedTask, LogHandler,
  Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 2940892868162482784L;

  /** The name of the task */
  protected String m_name;

  /** The flow XML to execute */
  protected StringBuffer m_flowXML;

  /**
   * True if start points in the flow are to be launched sequentially rather
   * than in parallel
   */
  protected boolean m_sequential;

  /** Log */
  protected transient ServerLogger m_log;

  /**
   * flow parameters (i.e. environment variables set on the client side that need
   * to be set on the server side for the flow)
   */
  protected Map<String, String> m_parameters;

  /** Status info */
  protected TaskStatusInfo m_result = new TaskStatusInfo();

  /** The FlowRunner used to actually execute the flow */
  protected FlowRunner m_fr;

  /** File to persist results to */
  protected File m_persistedResult;

  /**
   * Constructor
   * 
   * @param name name of the flow
   * @param xmlFlow the actual XML flow to execute
   * @param sequential true if start points are to run sequentially
   * @param parameters parameters for the flow
   */
  public LegacyUnscheduledNamedKFTask(String name, StringBuffer xmlFlow,
    boolean sequential, Map<String, String> parameters) {
    m_name = name;
    m_flowXML = xmlFlow;
    m_sequential = sequential;
    m_parameters = parameters;
  }

  @Override
  public void execute() {
    ObjectOutputStream oos = null;

    try {
      // deserialize the flow
      m_log.logMessage("UnscheduledNamedTask - deserializing the flow");
      java.io.StringReader sr = new java.io.StringReader(m_flowXML.toString());
      BeanConnection.init();
      BeanInstance.init();
      XMLBeans xml = new XMLBeans(null, null, 0);
      Vector v = (Vector) xml.read(sr);
      Vector beans = (Vector) v.get(XMLBeans.INDEX_BEANINSTANCES);

      m_log.logMessage("Deserialized flow successfully");

      m_fr = new FlowRunner(true, true);
      m_fr.setFlows(beans);
      m_fr.setLog(m_log);
      m_fr.setStartSequentially(m_sequential);
      Environment env = new Environment();
      if (m_parameters != null && m_parameters.size() > 0) {
        m_log.logMessage("Setting parameters for the flow");

        for (String key : m_parameters.keySet()) {
          String value = m_parameters.get(key);
          env.addVariable(key, value);
        }
      }
      m_fr.setEnvironment(env);

      try {
        m_result.setExecutionStatus(TaskStatusInfo.PROCESSING);
        m_fr.run();
        m_fr.waitUntilFinished();
      } catch (InterruptedException ie) {
      }

      if (m_result.getExecutionStatus() != WekaTaskMap.WekaTaskEntry.STOPPED) {

        // look for HeadlessEventCollectors and store their event lists as
        // part of the results
        Map<String, List<EventObject>> results = new HashMap<String, List<EventObject>>();
        for (int i = 0; i < beans.size(); i++) {
          BeanInstance temp = (BeanInstance) beans.get(i);
          if (temp.getBean() instanceof HeadlessEventCollector) {
            // System.out.println("Found a headless event collector....");
            List<EventObject> events = ((HeadlessEventCollector) temp.getBean())
              .retrieveHeadlessEvents();

            // only store if we can get a name for this component
            if (temp.getBean() instanceof BeanCommon && events != null) {
              // System.out.println("Storing " + events.size() + " events....");
              results
                .put(((BeanCommon) temp.getBean()).getCustomName(), events);
            }
          }
        }

        if (results.size() > 0) {
          m_result.setTaskResult(results);
        }

        try {
          m_persistedResult = WekaServer.getTempFile();
          oos = new ObjectOutputStream(new BufferedOutputStream(
            new GZIPOutputStream(new FileOutputStream(m_persistedResult))));
          oos.writeObject(results);
          oos.flush();
          // successfully saved result - now save memory
          m_result.setTaskResult(null);
        } catch (Exception e) {
          m_persistedResult = null;
        } finally {
          if (oos != null) {
            try {
              oos.close();
            } catch (Exception ee) {
            }
          }
        }

        m_result.setExecutionStatus(TaskStatusInfo.FINISHED);
      }

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
      m_fr = null;
    }
  }

  @Override
  public void stop() {
    // attempt to stop all flows
    FlowRunner temp = m_fr;
    if (temp != null) {
      temp.stopAllFlows();
      m_result.setExecutionStatus(WekaTaskMap.WekaTaskEntry.STOPPED);
      temp = null;
    }
  }

  @Override
  public synchronized TaskStatusInfo getTaskStatus() {

    // set up the status message by pulling current logging info
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

    return m_result;
  }

  @Override
  public void setName(String name) {
    m_name = name;
  }

  @Override
  public String getName() {
    return m_name;
  }

  @Override
  public void setLog(Logger log) {
    m_log = (ServerLogger) log;
  }

  @Override
  public Logger getLog() {
    return m_log;
  }

  @Override
  public void freeMemory() {
  }

  @Override
  public void persistResources() {
  }

  @Override
  public void loadResources() {
  }

  @Override
  public void loadResult() throws Exception {
    if (m_persistedResult == null || !m_persistedResult.exists()) {
      throw new Exception("Result file seems to have disapeared!");
    }

    ObjectInputStream ois = null;
    try {
      ois = new ObjectInputStream(new BufferedInputStream(new GZIPInputStream(
        new FileInputStream(m_persistedResult))));
      Map<String, List<EventObject>> results = (Map<String, List<EventObject>>) ois
        .readObject();
      m_result.setTaskResult(results);
    } finally {
      if (ois != null) {
        ois.close();
      }
    }
  }

  @Override
  public void purge() {
    if (m_persistedResult != null && m_persistedResult.exists()) {
      if (!m_persistedResult.delete()) {
        m_persistedResult.deleteOnExit();
      }
    }
  }
}
