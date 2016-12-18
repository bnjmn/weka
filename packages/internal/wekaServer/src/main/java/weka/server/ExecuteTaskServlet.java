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
 *    ExecuteTaskServlet.java
 *    Copyright (C) 2011-2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.server;

import weka.core.LogHandler;
import weka.core.SerializationHelper;
import weka.experiment.Task;
import weka.server.WekaTaskMap.WekaTaskEntry;
import weka.server.logging.ServerLogger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Accepts a task for execution. Tasks are added to a Map and may get executed
 * immediately (if unscheduled) or according to their schedule.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class ExecuteTaskServlet extends WekaServlet {

  /** The context path for this servlet */
  public static final String CONTEXT_PATH = "/weka/executeTask";

  /**
   * For serialization
   */
  private static final long serialVersionUID = -8027098846235150265L;

  /**
   * Constructs a new ExecuteTaskServlet
   * 
   * @param taskMap the task map maintained by the server
   * @param server a reference to the server itself
   */
  public ExecuteTaskServlet(WekaTaskMap taskMap, WekaServer server) {
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

    InputStream in = null;
    BufferedReader inReader = null;
    ObjectOutputStream oos = null;
    PrintWriter out = null;

    String legacyClientParam = request.getParameter(Legacy.LEGACY_CLIENT_KEY);
    String jsonClientParam = request.getParameter(JSONProtocol.JSON_CLIENT_KEY);
    boolean clientLegacy =
      legacyClientParam != null && legacyClientParam.equalsIgnoreCase("y");
    boolean clientNew =
      jsonClientParam != null && jsonClientParam.equalsIgnoreCase("y");
    String masterParam = request.getParameter("master");
    boolean fromMaster =
      masterParam != null && masterParam.equalsIgnoreCase("y");

    if (clientLegacy) {
      in = request.getInputStream();
      // response.setCharacterEncoding("UTF-8");
      // response.setContentType("text/plain");
      response.setContentType("application/octet-stream");
      OutputStream outS = response.getOutputStream();
      oos = new ObjectOutputStream(new BufferedOutputStream(outS));
    } else if (clientNew) {
      inReader = request.getReader();
      out = response.getWriter();
      response.setCharacterEncoding("UTF-8");
      response.setContentType("application/json");
    } else {
      out = response.getWriter();
      response.setCharacterEncoding("UTF-8");
      response.setContentType("text/html;charset=UTF-8");
      out.println("<HTML>");
      out.println("<HEAD><TITLE>Execute task</TITLE></HEAD>");
      out.println("<BODY>");
    }

    response.setStatus(HttpServletResponse.SC_OK);

    ObjectInputStream ois = null;
    Object task = null;
    WekaTaskEntry entry = null;
    try {
      if (clientLegacy) {
        // Deserialize the task
	GZIPInputStream gzi = new GZIPInputStream(in);
	ois = SerializationHelper.getObjectInputStream(gzi);        

        task = ois.readObject();
      } else if (clientNew) {
        // Deserialize the json
        StringBuilder b = new StringBuilder();
        String line = "";
        while ((line = inReader.readLine()) != null) {
          b.append(line);
        }
        task = JSONProtocol.jsonToNamedTask(b.toString());
      }

      if (!(task instanceof Task)) {
        throw new Exception(
          "Submitted task does not implement weka.experiment.Task!");
      }

      if (task instanceof NamedTask) {
        entry = m_taskMap.addTask((NamedTask) task);
      } else {
        entry = m_taskMap.addTask((Task) task);
      }

      // set the originating server to this WekaServer instance so that the
      // logging object
      // can create the appopriate logging subdirectory (if necessary)
      entry.setOriginatingServer(m_server.getHostname() + ":"
        + m_server.getPort());
      entry.setServer(entry.getOriginatingServer());
      entry.setCameFromMaster(fromMaster);

      if (task instanceof LogHandler) {
        ServerLogger sl = new ServerLogger(entry);
        ((LogHandler) task).setLog(sl);
      }

      if (task instanceof Scheduled) {
        // make sure we save this task in case we go down...
        m_server.persistTask(entry, (NamedTask) task);
      }

      // ask the task to persist any resources
      if (task instanceof NamedTask) {
        ((NamedTask) task).persistResources();
      }

      // send the task name + id to the client
      if (clientLegacy) {
        oos.writeObject(entry.toString());
        oos.flush();
      } else if (clientNew) {
        Map<String, Object> responseJ =
          JSONProtocol.createOKResponseMap(entry.toString());
        String encodedResponse = JSONProtocol.encodeToJSONString(responseJ);
        out.println(encodedResponse);
        out.flush();
      } else {
        // out = response.getWriter();
        String startOrScheduled = " started";
        if (task instanceof Scheduled) {
          startOrScheduled = " scheduled";
        }
        out.print("<H1>");
        out.print("Task '" + entry.getName() + "' with ID '" + entry.getID()
          + startOrScheduled);
        out.println("</H1>");
      }
    } catch (Exception ex) {
      if (clientLegacy) {
        oos.writeObject(WekaServlet.RESPONSE_ERROR + " " + ex.getMessage());
        oos.flush();
      } else {
        out.println("<p><pre>");
        ex.printStackTrace(out);
        out.println("</pre>");
      }
      ex.printStackTrace();
    } finally {
      if (ois != null) {
        ois.close();
        ois = null;
      }

      if (oos != null) {
        oos.close();
        oos = null;
      }

      if (!clientLegacy && !clientNew && out != null) {
        out.println("<p>");
        out.println("</BODY>");
        out.println("</HTML>");
      }

      if (out != null) {
        out.close();
        out = null;
      }

      // If this task is unscheduled, ask the server to run now
      if (task != null && entry != null && !(task instanceof Scheduled)) {
        m_server.executeTask(entry);
      }
    }
  }
}
