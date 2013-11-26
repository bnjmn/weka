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
 *    RootServlet.java
 *    Copyright (C) 2011-2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;

import weka.experiment.TaskStatusInfo;

/**
 * The main servlet. Shows status of the server, tasks and slaves.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class RootServlet extends WekaServlet {

  /** The context path for this servlet */
  public static final String CONTEXT_PATH = "/";

  /**
   * For serialization
   */
  private static final long serialVersionUID = -3765961462227609179L;

  /**
   * Constructs a new RootServlet
   * 
   * @param taskMap the task map maintained by the server
   * @param server a reference to the server itself
   */
  public RootServlet(WekaTaskMap taskMap, WekaServer server) {
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
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    // TODO

    if (!request.getRequestURI().equals(CONTEXT_PATH)) {
      return;
    }

    response.setContentType("text/html;charset=UTF-8");
    response.setStatus(HttpServletResponse.SC_OK);

    PrintWriter out = response.getWriter();

    try {
      out.println("<HTML>\n<HEAD>\n<TITLE>Weka server</TITLE>");
      out.println("<META http-equiv=\"Refresh\" content=\"30;url="
        + CONTEXT_PATH + "\">");
      out.println("</HEAD>\n<BODY>");
      out.println("<H2> Weka Server (" + m_server.getHostname() + ":"
        + m_server.getPort() + ")</H2>");
      if (m_server.getMaster() != null) {
        out.println("Registered with master server: " + m_server.getMaster()
          + "<br>");
      }
      out.println("Number of execution slots: "
        + m_server.getNumExecutionSlots() + "<br>");
      out.println("Number of tasks executing: " + m_server.numRunningTasks()
        + "<br>");
      out.println("Number of tasks queued: " + m_server.numQueuedTasks()
        + "<br>");
      out.println("Load adjust factor: "
        + String.format("%.3f", m_server.getLoadAdjust()) + "<br>");
      out
        .println("Server load ((#executing + #queued) * loadFactor / #execution_slots): "
          + m_server.getServerLoad() + "<p>");
      System.gc();
      Runtime run = Runtime.getRuntime();
      long freeM = run.freeMemory();
      long totalM = run.totalMemory();
      long maxM = run.maxMemory();
      out.println("Memory (free/total/max) in bytes: "
        + String.format("%,d", freeM) + " / " + String.format("%,d", totalM)
        + " / " + String.format("%,d", maxM) + "<p>");

      if (m_server.getSlaves().size() > 0) {
        out.println("<H3>Slaves</H3>");
        out.println("<p>");

        out.println("<table border=\"1\">");
        out.print("<tr><th>");
        out.print("Host</th><th>Port</th><th>Status</th><th>Load</th></tr>\n");
        Set<String> slaves = m_server.getSlaves();
        for (String slave : slaves) {
          String[] parts = slave.split(":");
          out.print("<tr>");
          out.print("<td><a href=\"http://" + parts[0] + ":" + parts[1] + "\">"
            + parts[0] + "</a></td><td>" + parts[1] + "</td>");
          double load = getSlaveLoad(m_server, slave);
          String okString = (load < 0) ? "connection error" : "OK";

          out.print("<td>" + okString + "</td><td>" + load + "</td></tr>\n");
        }
        out.print("</table><p>");
      }

      out.println("<p>");

      out.println("<H3>Tasks</H3>");

      out.println("<table border=\"1\">");
      out.print("<tr><th>");
      out
        .print("Task name</th><th>ID</th><th>Server</th><th>Last execution</th><th>Next execution</th>"
          + "<th>Status</th><th>Purge</th></tr>\n");
      List<WekaTaskMap.WekaTaskEntry> taskList = m_taskMap.getTaskList();
      for (WekaTaskMap.WekaTaskEntry entry : taskList) {
        String name = entry.getName();
        String id = entry.getID();
        NamedTask task = m_taskMap.getTask(entry);
        TaskStatusInfo tsi = task.getTaskStatus();
        // Date lastExecuted = m_taskMap.getExecutionTime(entry);
        Date lastExecuted = entry.getLastExecution();
        Date nextExecution = null;
        if (task instanceof Scheduled) {
          nextExecution = ((Scheduled) task).getSchedule().nextExecution(
            lastExecuted);
        }

        out.print("<tr>");
        out.print("<td><a href=\"" + GetTaskStatusServlet.CONTEXT_PATH
          + "?name=" + URLEncoder.encode(entry.toString(), "UTF-8") + "\">"
          + name + "</a></td>");
        out.print("<td>" + id + "</td>");
        String server = entry.getServer();
        if (server.equals(m_server.getHostname() + ":" + m_server.getPort())) {
          server = "local";
        }
        out.print("<td>");
        if (!server.equals("local")) {
          out
            .print("<a href=\"http://" + server + "\">" + server + "</a></td>");
        } else {
          out.print("" + server + "</td>");
        }

        String formattedLastDate = " - ";
        String formattedNextDate = " - ";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        if (lastExecuted != null) {
          formattedLastDate = sdf.format(lastExecuted);
        }
        out.print("<td>" + formattedLastDate + "</td>");

        if (nextExecution != null) {
          formattedNextDate = sdf.format(nextExecution);
        }
        out.print("<td>" + formattedNextDate + "</td>");

        out.print("<td>" + GetTaskStatusServlet.taskStatusInfoToString(tsi)
          + "</td>");

        if (tsi.getExecutionStatus() != TaskStatusInfo.PROCESSING) {
          out.print("<td><a href=\"" + PurgeTaskServlet.CONTEXT_PATH + "?name="
            + URLEncoder.encode(entry.toString(), "UTF-8")
            + "\">Remove</a></td>");
        } else {
          out.print("<td> -- </td>");
        }
        out.print("</tr>\n");
      }
      out.print("</table><p>");

      out.println("</BODY>\n</HTML>");
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      if (out != null) {
        out.flush();
        out.close();
        out = null;
      }
    }
  }

  /**
   * Utility method to get the load of a named slave
   * 
   * @param server the local server instance
   * @param slave the name of the remote slave
   * @return the load of the slave
   */
  public static double getSlaveLoad(WekaServer server, String slave) {
    return getSlaveLoad(slave, server.getUsername(), server.getPassword());
  }

  /**
   * Utility method to get the load of a named slave
   * 
   * @param slave the name of the remote slave
   * @param username the username to authenticate with
   * @param password the password to authenticate with
   * @return the load of the slave
   */
  public static double getSlaveLoad(String slave, String username,
    String password) {
    double load = -1;

    InputStream is = null;
    PostMethod post = null;
    try {
      String url = "http://" + slave;
      url = url.replace(" ", "%20");
      url += GetServerLoadServlet.CONTEXT_PATH;
      url += "/?client=Y";

      post = new PostMethod(url);
      post.setDoAuthentication(true);
      post.addRequestHeader(new Header("Content-Type", "text/plain"));

      // Get HTTP client
      HttpClient client = WekaServer.ConnectionManager.getSingleton()
        .createHttpClient();
      WekaServer.ConnectionManager.addCredentials(client, username, password);

      // Execute request
      int result = client.executeMethod(post);
      // the response
      is = post.getResponseBodyAsStream();
      ObjectInputStream ois = new ObjectInputStream(is);
      Object response = ois.readObject();

      if (!(response instanceof Double)) {
        throw new Exception(
          "[WekaServer] Unexpected result from slave (reqeust load)!");
      }

      load = ((Double) response).doubleValue();
    } catch (Exception ex) {
      // ex.printStackTrace();
      System.err.println("Error getting slave load: " + ex.getMessage());
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

    return load;
  }

  @Override
  public String toString() {
    return "Root servlet";
  }
}
