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
 *    AddSlaveServlet.java
 *    Copyright (C) 2011-2013 University of Waikato, Hamilton, New Zealand
 */

package weka.server;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Registers a slave server with this Weka server instance.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class AddSlaveServlet extends WekaServlet {

  /** The context path of this servlet */
  public static final String CONTEXT_PATH = "/weka/addSlave";

  /**
   * For serialization
   */
  private static final long serialVersionUID = -799055679889116723L;

  /**
   * Constructs a new AddSlaveServlet
   * 
   * @param taskMap the map of tasks maintained by the server
   * @param server a reference to the server itself
   */
  public AddSlaveServlet(WekaTaskMap taskMap, WekaServer server) {
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
    String slaveToAdd = request.getParameter("slave");
    String clientParam = request.getParameter("client");

    boolean client = (clientParam != null && clientParam.equalsIgnoreCase("y"));

    PrintWriter out = null;
    ObjectOutputStream oos = null;

    response.setStatus(HttpServletResponse.SC_OK);
    if (client) {
      response.setContentType("application/octet-stream");
    } else {
      response.setCharacterEncoding("UTF-8");
      response.setContentType("text/html;charset=UTF-8");
    }

    boolean ok = (slaveToAdd.lastIndexOf(":") > 0);

    try {
      if (client) {
        if (!ok) {
          String errorResult = WekaServlet.RESPONSE_ERROR
            + ": malformed host address (need host:port)" + " - " + slaveToAdd;
          OutputStream outS = response.getOutputStream();
          oos = new ObjectOutputStream(new BufferedOutputStream(outS));
          oos.writeObject(errorResult);
          oos.flush();
        } else {
          System.out.println("[WekaServer] Adding slave server " + slaveToAdd);
          m_server.addSlave(slaveToAdd);
          String result = WekaServlet.RESPONSE_OK + ": slave '" + slaveToAdd
            + "' registered successfully";
          OutputStream outS = response.getOutputStream();
          oos = new ObjectOutputStream(new BufferedOutputStream(outS));
          oos.writeObject(result);
          oos.flush();
          System.out.println("[AddSlave] done");
        }
      } else {
        if (!ok) {
          out = response.getWriter();

          out.println("<HTML>");
          out.println("<HEAD>");
          out.println("<TITLE>Add Slave</TITLE>");
          out.println("</HEAD>");
          out.println("<BODY>\n<H3>");

          out.println(WekaServlet.RESPONSE_ERROR + ": Malformed host address "
            + "(need host:port) - " + slaveToAdd + "</H3>");
          out.println("<a href=\"" + RootServlet.CONTEXT_PATH + "\">"
            + "Back to status page</a></br>");
          out.println("</BODY>\n</HTML>");
        } else {
          out = response.getWriter();

          out.println("<HTML>");
          out.println("<HEAD>");
          out.println("<TITLE>Add Slave</TITLE>");
          out.println("</HEAD>");
          out.println("<BODY>\n<H3>");

          out.println(WekaServlet.RESPONSE_OK + ": Slave '" + slaveToAdd
            + "' added successfully. </H3>");
          out.println("<a href=\"" + RootServlet.CONTEXT_PATH + "\">"
            + "Back to status page</a></br>");
          out.println("</BODY>\n</HTML>");
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      if (oos != null) {
        oos.close();
        oos = null;
      }

      if (out != null) {
        out.close();
        out = null;
      }
    }
  }
}
