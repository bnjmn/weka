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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;

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

    String clientParamLegacy = request.getParameter(Legacy.LEGACY_CLIENT_KEY);
    String clientParamNew = request.getParameter(JSONProtocol.JSON_CLIENT_KEY);
    boolean clientLegacy =
      clientParamLegacy != null && clientParamLegacy.equalsIgnoreCase("y");
    boolean clientNew =
      clientParamNew != null && clientParamNew.equalsIgnoreCase("y");

    String taskName = request.getParameter("name");

    NamedTask task = m_taskMap.getTask(taskName);

    if (clientLegacy) {
      // response.setCharacterEncoding("UTF-8");
      // response.setContentType("text/plain");
      response.setContentType("application/octet-stream");
      OutputStream outS = response.getOutputStream();
      oos = new ObjectOutputStream(new BufferedOutputStream(outS));
    } else if (clientNew) {
      out = response.getWriter();
      response.setCharacterEncoding("UTF-8");
      response.setContentType("application/json");
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
        if (clientLegacy) {
          String errorResult =
            WekaServlet.RESPONSE_ERROR + ": Can't find task " + taskName;
          oos.writeObject(errorResult);
          oos.flush();
        } else if (clientNew) {
          Map<String, Object> errorJ =
            JSONProtocol.createErrorResponseMap("Can't find task " + taskName);
          String encodedResponse = JSONProtocol.encodeToJSONString(errorJ);
          out.println(encodedResponse);
          out.flush();
        } else {
          out
            .println(WekaServlet.RESPONSE_ERROR + ": Unknown task " + taskName);
        }
      } else if (!(task instanceof Scheduled)) {
        if (clientLegacy) {
          String errorResult =
            WekaServlet.RESPONSE_ERROR + "'" + taskName + "' "
              + "is not a scheduled task.";
          oos.writeObject(errorResult);
          oos.flush();
        } else if (clientNew) {
          Map<String, Object> errorJ =
            JSONProtocol.createErrorResponseMap("'" + taskName
              + "' is not a scheduled task");
          String encodedResponse = JSONProtocol.encodeToJSONString(errorJ);
          out.println(encodedResponse);
          out.flush();
        } else {
          out.println(WekaServlet.RESPONSE_ERROR + "'" + taskName + "' "
            + "is not a scheduled task.");
        }
      } else {
        Schedule sched = ((Scheduled) task).getSchedule();
        if (clientLegacy) {
          oos.writeObject(sched);
          oos.flush();
        } else if (clientNew) {
          Map<String, Object> scheduleJ = JSONProtocol.scheduleToJsonMap(sched);
          Map<String, Object> jResponse =
            JSONProtocol.createOKResponseMap("OK. Schedule");
          JSONProtocol.addPayloadMap(jResponse, scheduleJ,
            JSONProtocol.SCHEDULE_PAYLOAD_ID);

          String encodedResonse = JSONProtocol.encodeToJSONString(jResponse);
          out.println(encodedResonse);
          out.flush();
        } else {
          String optionsString =
            weka.core.Utils.joinOptions(sched.getOptions());
          out.println(optionsString + "<p>");
        }
      }
    } catch (Exception ex) {
      if (clientLegacy && oos != null) {
        oos.writeObject(WekaServlet.RESPONSE_ERROR + " " + ex.getMessage());
        oos.flush();
      } else if (out != null && !clientNew) {
        out.println("<p><pre>");
        ex.printStackTrace(out);
        out.println("</pre>\n");
      }
      ex.printStackTrace();
    } finally {
      if (!clientLegacy && !clientNew && out != null) {
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
