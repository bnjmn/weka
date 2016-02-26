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
 *    FlowRunnerRemote.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.server.knowledgeFlow;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.boon.json.JsonFactory;
import org.boon.json.ObjectMapper;
import weka.core.CommandlineRunnable;
import weka.core.Environment;
import weka.core.Option;
import weka.core.Utils;
import weka.knowledgeflow.Flow;
import weka.knowledgeflow.FlowRunner;
import weka.server.ExecuteTaskServlet;
import weka.server.JSONProtocol;
import weka.server.NamedTask;
import weka.server.Schedule;
import weka.server.WekaServer;
import weka.server.WekaServlet;

import java.io.File;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Execute a KnowledgeFlow on a remote server from the command line
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class FlowRunnerRemote implements CommandlineRunnable {

  /** Prefix for the job on the server */
  public static final String NAME_PREFIX = "KF_";

  /** The name of the task when executing on the server */
  protected String m_name;

  /** Holds the JSON flow to execute */
  protected String m_flowJSON;

  /** Scheduling information */
  protected Schedule m_schedule;

  /**
   * True if the flow's start points are to be launched one at a time (rather
   * than in parallel)
   */
  protected boolean m_sequential;

  /** Parameters for the flow */
  protected Map<String, String> m_parameters;

  /** Host that the server is running on */
  protected String m_host;

  /** Port that the server is listening on */
  protected String m_port = "" + WekaServer.PORT;

  /** Username (if necessary) */
  protected String m_username;

  /** Password (if necessary) */
  protected String m_password;

  /**
   * Load a serialized KnowledgeFlow
   * 
   * @param fileName the name of the file to load from
   * @throws Exception if something goes wrong
   */
  public void loadFlow(String fileName) throws Exception {
    File file = new File(fileName);
    m_flowJSON =
      Flow.loadFlow(file, new FlowRunner.SimpleLogger()).toJSON();
    m_name = file.getName().substring(0, file.getName().lastIndexOf("."));
  }

  /**
   * Construct a URL for the server
   * 
   * @param serviceAndArguments the name of the service to access on the server
   * @return the URL
   */
  public String constructURL(String serviceAndArguments) {
    String realHostname = m_host;
    String realPort = m_port;
    try {
      realHostname = Environment.getSystemWide().substitute(m_host);
      realPort = Environment.getSystemWide().substitute(m_port);
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
   * Execute the current flow remotely on the server
   * 
   * @throws Exception if a problem occurs
   */
  @SuppressWarnings("unchecked")
  public void executeFlowRemote() throws Exception {
    Exception exception = null;

    NamedTask taskToRun =
      new UnscheduledNamedKnowledgeFlowTask(NAME_PREFIX + m_name, m_flowJSON,
        m_sequential, m_parameters);

    if (m_schedule != null) {
      taskToRun =
        new ScheduledNamedKnowledgeFlowTask(
          (UnscheduledNamedKnowledgeFlowTask) taskToRun, m_schedule);
    }

    PostMethod post = null;
    try {

      // JSON task definition
      Map<String, Object> jsonMap = JSONProtocol.kFTaskToJsonMap(taskToRun);
      String json = JSONProtocol.encodeToJSONString(jsonMap);

      // serialized and compressed
      System.out.println("Sending json task definition...");

      String service =
        ExecuteTaskServlet.CONTEXT_PATH + "/?" + JSONProtocol.JSON_CLIENT_KEY
          + "=Y";
      post = new PostMethod(constructURL(service));
      RequestEntity jsonRequest =
        new StringRequestEntity(json, JSONProtocol.JSON_MIME_TYPE,
          JSONProtocol.CHARACTER_ENCODING);

      post.setRequestEntity(jsonRequest);

      post.setDoAuthentication(true);
      post.addRequestHeader(new Header("Content-Type",
        JSONProtocol.JSON_MIME_TYPE));

      // Get HTTP client
      HttpClient client =
        WekaServer.ConnectionManager.getSingleton().createHttpClient();
      WekaServer.ConnectionManager.addCredentials(client, m_username,
        m_password);

      // Execute request

      int result = client.executeMethod(post);
      System.out.println("Response status from server : " + result);

      // the response
      if (result == 401) {
        // Security problem - authentication required
        /*
         * bodyB.setLength(0); bodyB.append("Unable to send task to server - " +
         * "authentication required.\n");
         */
        System.err
          .println("Unable to send task to server - authentication required.\n");
      } else {
        String responseS = post.getResponseBodyAsString();
        ObjectMapper mapper = JsonFactory.create();
        Map<String, Object> responseMap =
          mapper.readValue(responseS, Map.class);
        Object responseType = responseMap.get(JSONProtocol.RESPONSE_TYPE_KEY);
        if (responseType == null
          || responseType.toString().startsWith(WekaServlet.RESPONSE_ERROR)) {
          System.err.println("A problem occurred at the sever : \n");
          if (responseType == null) {
            System.err.println("Response was null!");
          } else {
            System.err.println(responseMap
              .get(JSONProtocol.RESPONSE_MESSAGE_KEY));
          }
        } else {
          String taskID = responseMap.get(JSONProtocol.RESPONSE_MESSAGE_KEY).toString();
          System.out.println("Task ID from server: " + taskID);
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      exception = ex;
    } finally {
      if (post != null) {
        // Release current connection to the connection pool
        post.releaseConnection();
      }
    }

    if (exception != null) {
      throw new Exception(exception.fillInStackTrace());
    }
  }

  /**
   * Get a string encapsulating command line usage instructions
   * 
   * @return a command line help string
   */
  public static String commandLineUsage() {
    StringBuffer result = new StringBuffer();
    result.append("Usage: FlowRunnerRemote -file <flowFile.[kf | kfml]> "
      + "[-sequential] -server <host:[port]> [-username <username>] "
      + "[-password <password>] [-param <name=value>, "
      + "-param <name=value>, ... -- [<schedule options>]");

    result.append("\n\n");
    result.append("Scheduling options:\n\n");
    Schedule temp = new Schedule();
    Enumeration<Option> opts = temp.listOptions();
    while (opts.hasMoreElements()) {
      Option o = opts.nextElement();
      result.append(o.synopsis() + "\n");
      result.append(o.description() + "\n");
    }

    return result.toString();
  }

  @Override
  public void preExecution() throws Exception {

  }

  @Override
  public void run(Object toRun, String[] args) {

    if (args.length < 1) {
      System.out.println(FlowRunnerRemote.commandLineUsage());
      System.exit(1);
    } else {

      String[] scheduleOpts = Utils.partitionOptions(args);
      if (scheduleOpts.length > 0) {
        m_schedule = new Schedule();
        try {
          m_schedule.setOptions(scheduleOpts);
        } catch (Exception e) {
          throw new IllegalArgumentException(e.getMessage());
        }
      }

      String flowFile = null;
      String host = null;
      String port = "" + WekaServer.PORT;
      m_parameters = new HashMap<String, String>();

      for (int i = 0; i < args.length; i++) {
        if (args[i].equalsIgnoreCase("-file")) {
          if (++i == args.length) {
            System.out.println(FlowRunnerRemote.commandLineUsage());
            System.exit(1);
          }
          flowFile = args[i];
        } else if (args[i].equalsIgnoreCase("-sequential")) {
          m_sequential = true;
          i++;
        } else if (args[i].equalsIgnoreCase("-server")) {
          if (++i == args.length) {
            System.out.println(FlowRunnerRemote.commandLineUsage());
            System.exit(1);
          }
          String[] parts = args[i].split(":");
          host = parts[0];
          if (parts.length == 2) {
            port = "" + Integer.parseInt(parts[1]);
          }
          m_host = host;
          m_port = port;
        } else if (args[i].equalsIgnoreCase("-username")) {
          if (++i == args.length) {
            System.out.println(FlowRunnerRemote.commandLineUsage());
            System.exit(1);
          }
          m_username = args[i];
        } else if (args[i].equalsIgnoreCase("-password")) {
          if (++i == args.length) {
            System.out.println(FlowRunnerRemote.commandLineUsage());
            System.exit(1);
          }
          m_password = args[i];
        } else if (args[i].equalsIgnoreCase("-param")) {
          if (++i == args.length) {
            System.out.println(FlowRunnerRemote.commandLineUsage());
            System.exit(1);
          }
          String[] parts = args[i].split("=");
          if (parts.length != 2) {
            System.out.println(FlowRunnerRemote.commandLineUsage());
            System.exit(1);
          }
          m_parameters.put(parts[0], parts[1]);
        } else if (args[i].length() > 0) {
          System.out.println(FlowRunnerRemote.commandLineUsage());
          System.exit(1);
        }
      }

      try {
        loadFlow(flowFile);
      } catch (Exception e) {
        throw new IllegalArgumentException("Unable to load flow '" + args[0]
          + "'");
      }

      try {
        executeFlowRemote();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void postExecution() throws Exception {

  }

  /**
   * Main method for executing this class
   * 
   * @param args command line args
   */
  public static void main(String[] args) {
    try {
      FlowRunnerRemote fr = new FlowRunnerRemote();
      fr.run(fr, args);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
