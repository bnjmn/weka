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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.boon.json.JsonFactory;
import org.boon.json.ObjectMapper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;

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
    String clientParamLegacy = request.getParameter(Legacy.LEGACY_CLIENT_KEY);
    String clientParamNew = request.getParameter(JSONProtocol.JSON_CLIENT_KEY);

    boolean clientLegacy =
      clientParamLegacy != null && clientParamLegacy.equalsIgnoreCase("y");
    boolean clientNew =
      clientParamNew != null && clientParamNew.equalsIgnoreCase("y");

    PrintWriter out = null;
    ObjectOutputStream oos = null;

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

    boolean ok = slaveToAdd.lastIndexOf(":") > 0;

    try {
      if (clientLegacy) {
        if (!ok) {
          String errorResult =
            WekaServlet.RESPONSE_ERROR
              + ": malformed host address (need host:port)" + " - "
              + slaveToAdd;
          OutputStream outS = response.getOutputStream();
          oos = new ObjectOutputStream(new BufferedOutputStream(outS));
          oos.writeObject(errorResult);
          oos.flush();
        } else {
          System.out.println("[WekaServer] Adding slave server " + slaveToAdd);
          m_server.addSlave(slaveToAdd);
          String result =
            WekaServlet.RESPONSE_OK + ": slave '" + slaveToAdd
              + "' registered successfully";
          OutputStream outS = response.getOutputStream();
          oos = new ObjectOutputStream(new BufferedOutputStream(outS));
          oos.writeObject(result);
          oos.flush();
          System.out.println("[AddSlave] done");
        }
      } else if (clientNew) {
        if (!ok) {
          Map<String, Object> errorResponse =
            JSONProtocol
              .createErrorResponseMap("Malformed host address (need host:port) - "
                + slaveToAdd);
          String encodedResponse = JSONProtocol.encodeToJSONString(errorResponse);
          out = response.getWriter();
          out.println(encodedResponse);
          out.flush();
        } else {
          System.out.println("[WekaServer] Adding slave server " + slaveToAdd);
          m_server.addSlave(slaveToAdd);
          Map<String, Object> okResponse =
            JSONProtocol.createOKResponseMap("Slave '" + slaveToAdd
              + "' registered successfully");
          String encodedResponse = JSONProtocol.encodeToJSONString(okResponse);
          out = response.getWriter();
          out.println(encodedResponse);
          out.flush();
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

  public static void main(String[] args) {
    try {
      System.err.println("Adding dummy server: zaphod.beeblebrox:9001");
      String url =
        "http://" + args[0] + CONTEXT_PATH + "/?slave="
          + "zaphod.beeblebrox:9001&clientJ=y";
      PostMethod post = new PostMethod(url);
      HttpClient client =
        WekaServer.ConnectionManager.getSingleton().createHttpClient();
      // Execute request
      int result = client.executeMethod(post);
      System.out
        .println("[WekaServer] Response from master server : " + result);

      String response = post.getResponseBodyAsString();
      ObjectMapper mapper = JsonFactory.create();
      Map<String, Object> resultMap =
        (Map<String, Object>) mapper.readValue(post.getResponseBodyAsStream(),
          Map.class);
      System.err.println("Result message: "
        + resultMap.get(JSONProtocol.RESPONSE_MESSAGE_KEY));
    } catch (Exception ex) {
      ex.printStackTrace();
      ;
    }
  }
}
