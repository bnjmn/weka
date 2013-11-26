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
 *    GetScheduleServlet.java
 *    Copyright (C) 2011-2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.server;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Returns the schedule associated with a task. An error message is returned if
 * the named task is not an instance of Scheduled.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class GetScheduleServlet extends WekaServlet {

  /** The context path for this servlet */
  public static final String CONTEXT_PATH = "/weka/getSchedule";

  /**
   * For serialization
   */
  private static final long serialVersionUID = 3855196170486334648L;

  /**
   * Constructs a new GetScheduleServlet
   * 
   * @param taskMap the task map maintained by the server
   * @param server a reference to the server itself
   */
  public GetScheduleServlet(WekaTaskMap taskMap, WekaServer server) {
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

    PrintWriter out = null;
    InputStream in = request.getInputStream();
    ObjectOutputStream oos = null;

    String clientParam = request.getParameter("client");
    boolean client = (clientParam != null && clientParam.equalsIgnoreCase("y"));
    String taskName = request.getParameter("name");

    NamedTask task = m_taskMap.getTask(taskName);

    if (client) {
      // response.setCharacterEncoding("UTF-8");
      // response.setContentType("text/plain");
      response.setContentType("application/octet-stream");
      OutputStream outS = response.getOutputStream();
      oos = new ObjectOutputStream(new BufferedOutputStream(outS));
    } else {
      out = response.getWriter();
      response.setCharacterEncoding("UTF-8");
      response.setContentType("text/html;charset=UTF-8");
      out.println("<HTML>");
      out.println("<HEAD><TITLE>Schedule</TITLE></HEAD>");
      out.println("<BODY>");
    }

    response.setStatus(HttpServletResponse.SC_OK);

    try {
      if (task == null) {
        if (client) {
          String errorResult = WekaServlet.RESPONSE_ERROR
            + ": Can't find task " + taskName;
          oos.writeObject(errorResult);
          oos.flush();
        } else {
          out
            .println(WekaServlet.RESPONSE_ERROR + ": Unknown task " + taskName);
        }
      } else if (!(task instanceof Scheduled)) {
        if (client) {
          String errorResult = WekaServlet.RESPONSE_ERROR + "'" + taskName
            + "' " + "is not a scheduled task.";
          oos.writeObject(errorResult);
          oos.flush();
        } else {
          out.println(WekaServlet.RESPONSE_ERROR + "'" + taskName + "' "
            + "is not a scheduled task.");
        }
      } else {
        Schedule sched = ((Scheduled) task).getSchedule();
        if (client) {
          oos.writeObject(sched);
          oos.flush();
        } else {
          String optionsString = weka.core.Utils
            .joinOptions(sched.getOptions());
          out.println(optionsString + "<p>");
        }
      }
    } catch (Exception ex) {
      if (client && oos != null) {
        oos.writeObject(WekaServlet.RESPONSE_ERROR + " " + ex.getMessage());
        oos.flush();
      } else if (out != null) {
        out.println("<p><pre>");
        ex.printStackTrace(out);
        out.println("</pre>\n");
      }
      ex.printStackTrace();
    } finally {
      if (!client && out != null) {
        out.println("</BODY>\n</HTML>");
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
}
