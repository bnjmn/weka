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
 *    PurgeTaskServletServlet.java
 *    Copyright (C) 2011-2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.server;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import weka.core.LogHandler;
import weka.server.logging.ServerLogger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Purge one or more tasks from the server. Deletes all persisted copies too.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class PurgeTaskServlet extends WekaServlet {

  /** The context path for this servlet */
  public static final String CONTEXT_PATH = "/weka/purgeTask";

  /**
   * For serialization
   */
  private static final long serialVersionUID = 5411344605381835319L;

  /**
   * Constructs a new PurgeTaskServlet
   * 
   * @param taskMap the task map maintained by the server
   * @param server a reference to the server itself
   */
  public PurgeTaskServlet(WekaTaskMap taskMap, WekaServer server) {
    super(taskMap, server);
  }

  /**
   * Process a HTTP GET
   * 
   * @param request the request
   * @param response the response
   * 
   * @throws ServletException
   * @throws IOException
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {

    String taskName = request.getParameter("name");
    String clientParamLegacy = request.getParameter(Legacy.LEGACY_CLIENT_KEY);
    String jsonClientParam = request.getParameter(JSONProtocol.JSON_CLIENT_KEY);
    boolean clientLegacy =
      clientParamLegacy != null && clientParamLegacy.equalsIgnoreCase("y");
    boolean clientNew =
      jsonClientParam != null && jsonClientParam.equalsIgnoreCase("y");
    PrintWriter outWriter = null;
    ObjectOutputStream outStream = null;

    response.setStatus(HttpServletResponse.SC_OK);
    if (clientLegacy) {
      response.setContentType("application/octet-stream");
    } else if (clientNew) {
      response.setCharacterEncoding("UTF-8");
      response.setContentType("application/json");
    } else {
      response.setCharacterEncoding("UTF-8");
      response.setContentType("text/html;charset=UTF-8");
    }

    boolean allOK = true;
    List<String> taskNames = new ArrayList<String>();
    List<NamedTask> tasks = new ArrayList<NamedTask>();
    String[] nameParts = taskName.split(",");
    String unfoundTasks = "";
    for (String tn : nameParts) {
      taskNames.add(tn.trim());
      NamedTask t = m_taskMap.getTask(tn);
      if (t == null) {
        allOK = false;
        unfoundTasks += tn + ",";
      } else {
        tasks.add(t);
      }
    }

    // Get the task
    // NamedTask task = m_taskMap.getTask(taskName);
    try {
      if (!allOK) {
        if (clientLegacy) {
          String errorResult =
            WekaServlet.RESPONSE_ERROR + ": Can't find task(s) " + unfoundTasks;
          OutputStream out = response.getOutputStream();
          outStream = new ObjectOutputStream(new BufferedOutputStream(out));
          outStream.writeObject(errorResult);
          outStream.flush();
        } else if (clientNew) {
          outWriter = response.getWriter();
          Map<String, Object> errorResponse =
            JSONProtocol.createErrorResponseMap("Can't find task(s) "
              + unfoundTasks);
          String errorJ = JSONProtocol.encodeToJSONString(errorResponse);
          outWriter.println(errorJ);
          outWriter.flush();
        } else {
          outWriter = response.getWriter();

          outWriter.println("<HTML>");
          outWriter.println("<HEAD>");
          outWriter.println("<TITLE>Purge Task</TITLE>");
          outWriter.println("</HEAD>");
          outWriter.println("<BODY>\n<H3>");

          outWriter.println(WekaServlet.RESPONSE_ERROR + ": Unknown task(s) "
            + unfoundTasks + "</H3>");
          outWriter.println("<a href=\"" + RootServlet.CONTEXT_PATH + "\">"
            + "Back to status page</a></br>");
          outWriter.println("</BODY>\n</HTML>");
        }
      } else {
        // WekaTaskMap.WekaTaskEntry wte = new
        // WekaTaskMap.WekaTaskEntry(taskName);
        String remotePurgeProblems = "";

        for (int i = 0; i < taskNames.size(); i++) {
          String tn = taskNames.get(i);
          NamedTask task = tasks.get(i);
          task.freeMemory();
          task.purge();
          WekaTaskMap.WekaTaskEntry wte = m_taskMap.getTaskKey(tn);
          m_taskMap.removeTask(wte);

          // remove serialized task and log
          if (task instanceof LogHandler) {
            ServerLogger sl = (ServerLogger) ((LogHandler) task).getLog();
            sl.deleteLog();
          }
          m_server.cleanupTask(wte);

          String responseIfRemote = "";
          if (!wte.getServer().equals(
            m_server.getHostname() + ":" + m_server.getPort())) {
            // This task was executed remotely on a slave - try and purge it
            // from the slave...
            String slave = wte.getServer();
            String remoteTaskID = wte.getRemoteID();
            responseIfRemote = purgeRemote(slave, remoteTaskID, tn);
            if (responseIfRemote.length() > 0) {
              remotePurgeProblems += "<br>" + responseIfRemote;
            }
          }
        }

        if (clientLegacy) {
          String result = null;
          if (remotePurgeProblems.length() == 0) {
            result =
              WekaServlet.RESPONSE_OK + ": Task(s) '" + taskName + "' removed.";
          } else {
            result = WekaServlet.RESPONSE_ERROR + ": " + remotePurgeProblems;
          }
          OutputStream out = response.getOutputStream();
          outStream = new ObjectOutputStream(new BufferedOutputStream(out));
          outStream.writeObject(result);
          outStream.flush();
        } else if (clientNew) {
          outWriter = response.getWriter();
          Map<String, Object> resultResponse = null;
          if (remotePurgeProblems.length() == 0) {
            resultResponse =
              JSONProtocol.createOKResponseMap("Task(s) '" + taskName
                + "' removed.");
          } else {
            resultResponse =
              JSONProtocol.createErrorResponseMap(remotePurgeProblems);
          }
          String responseJ = JSONProtocol.encodeToJSONString(resultResponse);
          outWriter.println(responseJ);
          outWriter.flush();
        } else {
          outWriter = response.getWriter();

          outWriter.println("<HTML>");
          outWriter.println("<HEAD>");
          outWriter.println("<TITLE>Purge Task</TITLE>");
          outWriter.println("</HEAD>");
          outWriter.println("<BODY>\n<H3>");

          if (remotePurgeProblems.length() == 0) {
            outWriter.println(WekaServlet.RESPONSE_OK + ": Task(s) '"
              + taskName + "' removed</H3>");
          } else {
            outWriter
              .println("<H3>An error occurred while trying to purge task from "
                + "remote server:</H3>");
            outWriter.println("<b>" + remotePurgeProblems + "</b><p>");
          }
          outWriter.println("<a href=\"" + RootServlet.CONTEXT_PATH + "\">"
            + "Back to status page</a></br>");
          outWriter.println("</BODY>\n</HTML>");
        }
      }
    } catch (Exception ex) {
      if (clientLegacy && outStream != null) {
        String errorResult =
          WekaServlet.RESPONSE_ERROR + ": An error occurred while trying"
            + " to purge task: " + taskName;
        /*
         * OutputStream out = response.getOutputStream(); Ob = new
         * ObjectOutputStream(new BufferedOutputStream(out));
         */
        outStream.writeObject(errorResult);
        outStream.flush();
      } else {
        // PrintWriter out = response.getWriter();
        if (outWriter != null) {
          if (!clientNew) {
            outWriter.println(WekaServlet.RESPONSE_ERROR
              + ": An error occurred while " + "trying to purge task: "
              + taskName);
            outWriter.println("</BODY>\n</HTML>");
          } else {
            Map<String, Object> errorResponse =
              JSONProtocol.createErrorResponseMap("An error occurred "
                + "while trying to purge task " + taskName + ": "
                + ex.getMessage());
            String responseJ = JSONProtocol.encodeToJSONString(errorResponse);
            outWriter.println(responseJ);
          }
        }
      }
      ex.printStackTrace();
    } finally {
      if (outStream != null) {
        outStream.close();
        outStream = null;
      }

      if (outWriter != null) {
        outWriter.close();
        outWriter = null;
      }
    }
  }

  protected String purgeRemote(String slave, String remoteTaskID,
    String origTaskID) {

    String ok = "";
    InputStream is = null;
    PostMethod post = null;

    try {
      String url = "http://" + slave;
      url = url.replace(" ", "%20");
      url += CONTEXT_PATH;
      url += "/?name=" + URLEncoder.encode(remoteTaskID, "UTF-8") + "&client=Y";
      post = new PostMethod(url);
      post.setDoAuthentication(true);
      post.addRequestHeader(new Header("Content-Type", "text/plain"));

      // Get HTTP client
      HttpClient client =
        WekaServer.ConnectionManager.getSingleton().createHttpClient();
      WekaServer.ConnectionManager.addCredentials(client,
        m_server.getUsername(), m_server.getPassword());

      // Execute request
      int result = client.executeMethod(post);
      // System.out.println("[WekaServer] Response from master server : " +
      // result);
      if (result == 401) {
        System.err.println("[WekaServer] Unable to purge remote task '"
          + origTaskID + "' - authentication required.\n");
        ok =
          "Unable to purge remote task '" + origTaskID
            + "' - authentication required for slave (" + slave + ")";
      } else {

        // the response
        is = post.getResponseBodyAsStream();
        ObjectInputStream ois = new ObjectInputStream(is);
        Object response = ois.readObject();
        if (response.toString().startsWith(WekaServlet.RESPONSE_ERROR)) {
          System.err.println("[WekaServer] A problem occurred while "
            + "trying to purge task : '" + origTaskID
            + "' from remote server (" + slave + "). Remote " + "task ID : "
            + remoteTaskID);
          ok =
            "A problem occurred while " + "trying to purge task : '"
              + origTaskID + "' from remote server (" + slave + "). Remote "
              + "task ID : " + remoteTaskID;
        }
      }
    } catch (Exception ex) {
      System.err.println("[WekaServer] A problem occurred while "
        + "trying to purge task : '" + origTaskID + "' from remote server: "
        + slave + " (" + ex.getMessage() + ")");
      ok =
        "A problem occurred while " + "trying to purge task : '" + origTaskID
          + "' from remote server: " + slave + " (" + ex.getMessage() + ")";
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

    return ok;
  }
}
