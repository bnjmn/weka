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
 *    GetTaskListServlet.java
 *    Copyright (C) 2011-2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.server;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Returns a list of tasks registered with this Weka server instance.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class GetTaskListServlet extends WekaServlet {

  /** The context path for this servlet */
  public static final String CONTEXT_PATH = "/weka/getTaskList";

  /**
   * For serialization
   */
  private static final long serialVersionUID = 5415885337850220144L;

  /**
   * Constructs a new GetTaskListServlet
   * 
   * @param taskMap the task map maintained by the server
   * @param server a reference to the server itself
   */
  public GetTaskListServlet(WekaTaskMap taskMap, WekaServer server) {
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

    String clientParamLegacy = request.getParameter(Legacy.LEGACY_CLIENT_KEY);
    String clientParamNew = request.getParameter(JSONProtocol.JSON_CLIENT_KEY);
    boolean clientLegacy =
      clientParamLegacy != null && clientParamLegacy.equalsIgnoreCase("y");
    boolean clientNew =
      clientParamNew != null && clientParamNew.equalsIgnoreCase("y");

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

    List<WekaTaskMap.WekaTaskEntry> taskList = m_taskMap.getTaskList();

    List<String> taskNames = new ArrayList<String>();
    for (WekaTaskMap.WekaTaskEntry te : taskList) {
      taskNames.add(te.toString());
    }

    ObjectOutputStream oos = null;
    PrintWriter out = null;

    try {
      if (clientLegacy) {
        OutputStream outS = response.getOutputStream();

        oos = new ObjectOutputStream(new BufferedOutputStream(outS));
        oos.writeObject(taskNames);
        oos.flush();
      } else if (clientNew) {
        Map<String, Object> jResponse =
          JSONProtocol.createOKResponseMap("OK. Task list");
        jResponse.put(JSONProtocol.RESPONSE_PAYLOAD_KEY, taskNames);
        String encodedResponse = JSONProtocol.encodeToJSONString(jResponse);
        out = response.getWriter();
        out.println(encodedResponse);
        out.flush();
      } else {
        out = response.getWriter();

        out.println("<HTML>");
        out.println("<HEAD>");
        out.println("<TITLE>Server Load</TITLE>");
        out.println("</HEAD>");
        out.println("<BODY>\n<H3>");
        out.println("Task List</H3>");
        for (String task : taskNames) {
          out.println(task + "<br>");
        }
        out.println("<p>");

        out.println("<a href=\"" + RootServlet.CONTEXT_PATH + "\">"
          + "Back to status page</a></br>");
        out.println("</BODY>\n</HTML>\n");
      }
    } catch (Exception ex) {
      if (oos != null) {
        oos.writeObject(WekaServlet.RESPONSE_ERROR + " " + ex.getMessage());
        oos.flush();
      } else if (out != null && clientNew) {
        Map<String, Object> jError =
          JSONProtocol.createErrorResponseMap(ex.getMessage());
        String encodedError = JSONProtocol.encodeToJSONString(jError);
        out.println(encodedError);
      } else if (out != null) {
        out.println("An error occured while getting task list:<br><br>");
        out.println("<pre>\n" + ex.getMessage() + "</pre>");
      }
      ex.printStackTrace();
    } finally {
      if (oos != null) {
        oos.close();
        oos = null;
      }

      if (out != null) {
        out.println("</BODY>\n</HTML>\n");
        out.close();
        out = null;
      }
    }
  }
}
