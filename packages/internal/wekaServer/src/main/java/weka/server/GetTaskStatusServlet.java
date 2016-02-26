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
 *    GetTaskStatusServlet.java
 *    Copyright (C) 2011-2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.server;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import weka.experiment.TaskStatusInfo;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Get the status of a task.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class GetTaskStatusServlet extends WekaServlet {

  /** The context path for this servlet */
  public static final String CONTEXT_PATH = "/weka/taskStatus";

  /**
   * For serialization
   */
  private static final long serialVersionUID = -7158444880154383241L;

  /**
   * Constructs a new GetTaskStatusServlet
   * 
   * @param taskMap the task map maintained by the server
   * @param server a reference to the server itself
   */
  public GetTaskStatusServlet(WekaTaskMap taskMap, WekaServer server) {
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

    if (!request.getRequestURI().startsWith(CONTEXT_PATH)) {
      return;
    }

    String taskName = request.getParameter("name");
    String clientParamLegacy = request.getParameter(Legacy.LEGACY_CLIENT_KEY);
    String jsonClientParam = request.getParameter(JSONProtocol.JSON_CLIENT_KEY);
    boolean clientLegacy =
      clientParamLegacy != null && clientParamLegacy.equalsIgnoreCase("y");
    boolean clientNew =
      jsonClientParam != null && jsonClientParam.equalsIgnoreCase("y");

    response.setStatus(HttpServletResponse.SC_OK);
    if (clientLegacy) {
      response.setCharacterEncoding("UTF-8");
      response.setContentType("application/octet-stream");
    }
    if (clientNew) {
      response.setContentType("application/json");
    } else {
      response.setCharacterEncoding("UTF-8");
      response.setContentType("text/html;charset=UTF-8");
    }

    ObjectOutputStream oos = null;
    PrintWriter out = null;

    // Get the task
    NamedTask task = m_taskMap.getTask(taskName);
    try {
      if (task == null) {
        if (clientLegacy) {
          String errorResult =
            WekaServlet.RESPONSE_ERROR + ": Can't find task " + taskName;
          OutputStream outS = response.getOutputStream();
          oos =
            new ObjectOutputStream(new BufferedOutputStream(
              new GZIPOutputStream(outS)));
          oos.writeObject(errorResult);
          oos.flush();
        } else if (clientNew) {
          out = response.getWriter();
          Map<String, Object> errorResponseJ =
            JSONProtocol.createErrorResponseMap("Can't find task " + taskName);
          String errorResponse =
            JSONProtocol.encodeToJSONString(errorResponseJ);
          out.println(errorResponse);
          out.flush();
        } else {
          out = response.getWriter();

          out.println("<HTML>");
          out.println("<HEAD>");
          out.println("<TITLE>Task Status</TITLE>");
          out.println("</HEAD>");
          out.println("<BODY>");

          out
            .println(WekaServlet.RESPONSE_ERROR + ": Unknown task " + taskName);
        }
      } else {
        TaskStatusInfo status = null;

        WekaTaskMap.WekaTaskEntry te = m_taskMap.getTaskKey(taskName);

        if (te.getServer().equals(
          m_server.getHostname() + ":" + m_server.getPort())) {
          TaskStatusInfo temp_status = task.getTaskStatus();

          // make sure we only send back status stuff (and not a result object)
          status = new TaskStatusInfo();
          status.setExecutionStatus(temp_status.getExecutionStatus());
          status.setStatusMessage(temp_status.getStatusMessage());

        } else {
          // need to ask the slave for it (and handle error if slave is down...)
          String slave = te.getServer();
          String remoteTaskID = te.getRemoteID();
          status = getStatusRemote(m_server, slave, remoteTaskID, taskName);
        }

        if (clientLegacy) {
          OutputStream outS = response.getOutputStream();

          // send status back to client
          oos =
            new ObjectOutputStream(new BufferedOutputStream(
              new GZIPOutputStream(outS)));
          oos.writeObject(status);
          oos.flush();
        } else if (clientNew) {
          // send the status back to the client
          Map<String, Object> responseJ =
            JSONProtocol.createOKResponseMap("OK. TaskStatus");
          responseJ.put(JSONProtocol.RESPONSE_PAYLOAD_KEY,
            JSONProtocol.taskStatusInfoToJsonMap(status, false));
          String encodedResponse = JSONProtocol.encodeToJSONString(responseJ);
          out = response.getWriter();
          out.println(encodedResponse);
          out.flush();
        } else {
          out = response.getWriter();

          out.println("<HTML>");
          out.println("<HEAD>");
          out.println("<TITLE>Task Status</TITLE>");
          out.println("<META http-equiv=\"Refresh\" content=\"20;url="
            + CONTEXT_PATH + "?name=" + URLEncoder.encode(taskName, "UTF-8")
            + "\">");
          out
            .println("<META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
          out.println("</HEAD>");
          out.println("<BODY>");
          out.println("<H1>" + taskName + "</H1>");

          String executionStatus = "Unavailable";
          if (status != null) {
            executionStatus = taskStatusInfoToString(status);
          }
          /*
           * switch(status.getExecutionStatus()) { case
           * TaskStatusInfo.TO_BE_RUN: executionStatus = "To be executed";
           * break; case TaskStatusInfo.FINISHED: executionStatus =
           * "Finished executing"; break; case TaskStatusInfo.PROCESSING:
           * executionStatus = "Processing..."; break; case
           * TaskStatusInfo.FAILED: executionStatus = "Last execution failed";
           * break; }
           */
          out.println("Task status: " + executionStatus);
          out.println("<p>");
          if (status != null) {
            String statusMessage = status.getStatusMessage();
            if (statusMessage != null && statusMessage.length() > 0
              && !statusMessage.equals("New Task")) {
              out.println("<H2>Current status messages</H2>");
              out.println("<pre>");
              out.println(statusMessage);
              out.println("</pre>");
            }
          }
        }
      }
    } catch (Exception ex) {
      if (clientLegacy && oos != null) {
        oos.writeObject(WekaServlet.RESPONSE_ERROR + " " + ex.getMessage());
        oos.flush();
      } else if (clientNew && out != null) {
        Map<String, Object> errorJ = JSONProtocol.createErrorResponseMap(ex.getMessage());
        out.println(JSONProtocol.encodeToJSONString(errorJ));
        out.flush();
      } else if (out != null) {
        out.println("<p><pre>");
        ex.printStackTrace(out);
        out.println("</pre>\n");
      }
      ex.printStackTrace();
    } finally {
      if (out != null) {
        if (!clientNew) {
          out.println("</BODY>\n</HTML>");
        }
      }

      if (out != null) {
        out.close();
        out = null;
      }
      if (oos != null) {
        oos.close();
        oos = null;
      }
    }
  }

  protected static TaskStatusInfo getStatusRemote(WekaServer server,
    String slave, String remoteTaskID, String origTaskID) {

    InputStream is = null;
    PostMethod post = null;
    TaskStatusInfo resultInfo = null;

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
      WekaServer.ConnectionManager.addCredentials(client, server.getUsername(),
        server.getPassword());

      // Execute request
      int result = client.executeMethod(post);
      // System.out.println("[WekaServer] Response from master server : " +
      // result);
      if (result == 401) {
        System.err.println("[WekaServer] Unable to get remote status of task'"
          + origTaskID + "' - authentication required.\n");
      } else {

        // the response
        is = post.getResponseBodyAsStream();
        ObjectInputStream ois =
          new ObjectInputStream(
            new BufferedInputStream(new GZIPInputStream(is)));
        Object response = ois.readObject();
        if (response.toString().startsWith(WekaServlet.RESPONSE_ERROR)) {
          System.err
            .println("[WekaServer] A problem occurred while "
              + "trying to retrieve remote status of task : '" + origTaskID
              + "'");

        } else {
          if (response instanceof TaskStatusInfo) {
            resultInfo = ((TaskStatusInfo) response);
          } else {
            System.err.println("[WekaServer] A problem occurred while "
              + "trying to retrieve remote status of task : '" + origTaskID
              + "'");
          }
        }
      }
    } catch (Exception ex) {
      System.err.println("[WekaServer] A problem occurred while "
        + "trying to retrieve remote status of task : '" + origTaskID + "' ("
        + ex.getMessage() + ")");
      System.err.println("Remote task id: " + remoteTaskID);
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

    return resultInfo;
  }

  /**
   * Converts a TaskStatusInfo constant to a string description.
   * 
   * @param status the status code to convert
   * @return a string description of the status
   */
  public static String taskStatusInfoToString(TaskStatusInfo status) {
    String executionStatus = "";
    switch (status.getExecutionStatus()) {
    case TaskStatusInfo.TO_BE_RUN:
      executionStatus = "To be executed";
      break;
    case TaskStatusInfo.FINISHED:
      executionStatus = "Finished executing";
      break;
    case TaskStatusInfo.PROCESSING:
      executionStatus = "Processing...";
      break;
    case TaskStatusInfo.FAILED:
      executionStatus = "Last execution failed";
      break;
    case WekaTaskMap.WekaTaskEntry.PENDING:
      executionStatus = "Pending";
      break;
    case WekaTaskMap.WekaTaskEntry.STOPPED:
      executionStatus = "Stopped";
      break;
    }

    return executionStatus;
  }
}
