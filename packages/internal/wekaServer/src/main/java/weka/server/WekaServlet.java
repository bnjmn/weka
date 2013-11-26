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
 *    WekaServlet.java
 *    Copyright (C) 2011-2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Base class for Weka servlets.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public abstract class WekaServlet extends HttpServlet {

  /** Response string for OK */
  public static final String RESPONSE_OK = "OK";

  /** Response string for error */
  public static final String RESPONSE_ERROR = "ERROR";

  /**
   * For serialization
   */
  private static final long serialVersionUID = 54767699564657650L;

  /** The map of tasks maintained by the server */
  protected WekaTaskMap m_taskMap;

  /** A reference to the server itself */
  protected WekaServer m_server;

  /**
   * Constructor
   */
  public WekaServlet() {
  }

  /**
   * Constructor
   * 
   * @param taskMap the task map maintained by the server
   * @param server a reference to the server itself
   */
  public WekaServlet(WekaTaskMap taskMap, WekaServer server) {
    m_taskMap = taskMap;
    m_server = server;
  }

  /**
   * Process a HTTP PUT
   * 
   * @param request the request
   * @param response the response
   * 
   * @throws ServletException
   * @throws IOException
   */
  @Override
  protected void doPut(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    doGet(request, response);
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
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    doGet(request, response);
  }
}
