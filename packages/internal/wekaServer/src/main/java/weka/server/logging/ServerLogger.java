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
 *    ServerLogger.java
 *    Copyright (C) 2011-2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.server.logging;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import weka.gui.Logger;
import weka.server.WekaServer;
import weka.server.WekaTaskMap;

/**
 * Class that provides logging to a file and also maintains the most recent 100
 * log lines and 50 status lines in memory.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class ServerLogger implements Logger {

  /** Directory for logs */
  protected static final String SERVER_LOG_DIRECTORY = WekaServer.SERVER_ROOT_DIRECTORY
    + "logs" + File.separator;

  protected static boolean s_logRootDirCreated;
  protected static boolean s_logSubDirCreated;

  static {
    File logDir = new File(SERVER_LOG_DIRECTORY);
    if (!logDir.exists()) {
      if (!logDir.mkdir()) {
        System.err.println("Unable to create server logging " + "directory ("
          + SERVER_LOG_DIRECTORY + ")");
        s_logRootDirCreated = false;
      } else {
        s_logRootDirCreated = true;
      }
    } else {
      s_logRootDirCreated = true;
    }
  }

  /** Formatter for dates */
  SimpleDateFormat m_dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  /** The maximum number of log lines to cache in memory */
  protected int m_maxLogLines = 100;

  /** The maximum number of status message lines to cache in memory */
  protected int m_maxStatusLines = 50;

  /** Cache of log lines */
  protected LinkedList<String> m_logCache = new LinkedList<String>();

  /** Cache of status lines */
  protected LinkedList<String> m_statusCache = new LinkedList<String>();

  /** Linefeed */
  protected String m_lineFeed;

  /** Log file */
  protected File m_logFile;

  /** The ID of the task using this log instance */
  protected String m_taskNameID;

  /** The subdirectory for storing the log */
  protected String m_logSubDir = "";

  private void checkLogSubDir() {
    File subDir = new File(m_logSubDir);
    if (!subDir.exists()) {
      if (!subDir.mkdir()) {
        System.err.println("[ServerLogger] Unable to create logging directory "
          + "(" + subDir.toString() + ")");
        s_logSubDirCreated = false;
      } else {
        s_logSubDirCreated = true;
      }
    } else {
      s_logSubDirCreated = true;
    }
  }

  /**
   * Constructs a new server logger
   * 
   * @param taskEntry the task entry for the task using this logger
   */
  public ServerLogger(WekaTaskMap.WekaTaskEntry taskEntry) {
    // m_entry = taskEntry;
    m_taskNameID = taskEntry.toString();

    m_logSubDir = SERVER_LOG_DIRECTORY
      + taskEntry.getOriginatingServer().replace(":", "_") + File.separator;

    checkLogSubDir();

    if (s_logRootDirCreated && s_logSubDirCreated) {
      m_lineFeed = System.getProperty("line.separator");
      m_logFile = new File(m_logSubDir + m_taskNameID + ".log");
    }
  }

  /**
   * Write a message to the log
   * 
   * @param message the message to write
   */
  @Override
  public synchronized void logMessage(String message) {
    logMessage(message, false);
  }

  protected synchronized void logMessage(String message, boolean fromFile) {
    if (!s_logRootDirCreated || !s_logSubDirCreated) {
      return;
    }

    if (!fromFile) {
      // log to the main weka.log file
      weka.core.logging.Logger.log(weka.core.logging.Logger.Level.INFO,
        m_taskNameID + " - " + message);
    }

    if (m_logCache.size() == m_maxLogLines) {
      // remove the first (oldest line)
      m_logCache.remove();
    }

    String formatted = message;

    if (!fromFile) {
      formatted = m_dateFormat.format(new Date()) + ": " + m_taskNameID + " - "
        + message;
      // append the line to the entry-specific log
      doServerLog(formatted);
    }

    m_logCache.add(formatted);
  }

  /**
   * Write a message to the status
   * 
   * @param message the message to write
   */
  @Override
  public synchronized void statusMessage(String message) {
    statusMessage(message, false);
  }

  protected synchronized void statusMessage(String message, boolean fromFile) {
    if (!s_logRootDirCreated || !s_logSubDirCreated) {
      return;
    }

    if (!fromFile) {
      // log to the main weka.log file
      weka.core.logging.Logger
        .log(weka.core.logging.Logger.Level.INFO, message);
    }

    if (m_statusCache.size() == m_maxStatusLines) {
      // remove the first (oldest) status line
      m_statusCache.remove();
    }

    // String formatted = m_dateFormat.format(new Date()) + ": " + message;
    String formatted = message;

    if (!fromFile) {
      formatted = message + " (" + m_taskNameID + ")";
      // append the line to the entry-specific log
      doServerLog(formatted);
    }
    m_statusCache.add(formatted);
  }

  /**
   * Get the in-memory cache of the log messages
   * 
   * @return the in-memory cache of the log messages
   */
  public synchronized List<String> getLogCache() {
    List<String> result = new ArrayList<String>(m_logCache);

    // clear the cache
    // m_logCache.clear();

    return result;
  }

  /**
   * Get the in-memory cache of the status
   * 
   * @return the in-memory cache of the status
   */
  public synchronized List<String> getStatusCache() {
    List<String> result = new ArrayList<String>(m_statusCache);

    // clear the cache
    // m_statusCache.clear();

    return result;
  }

  protected void doServerLog(String message) {
    BufferedWriter writer;

    if (m_logFile == null) {
      return;
    }

    try {
      writer = new BufferedWriter(new FileWriter(m_logFile, true));
      writer.write(message + m_lineFeed);
      writer.flush();
      writer.close();
    } catch (Exception ex) {
      // ignored
    }
  }

  /**
   * Load the most recent log and status messages from the log file into the
   * in-memory buffer
   */
  public void loadLog() {
    if (!s_logRootDirCreated || !s_logSubDirCreated || m_taskNameID == null) {
      return;
    }
    BufferedReader br = null;
    if (m_logFile.exists()) {
      try {
        br = new BufferedReader(new FileReader(m_logFile));
        String line = null;
        while ((line = br.readLine()) != null) {
          if (line.indexOf("(" + m_taskNameID + ")") >= 0) {
            // this is a status line
            statusMessage(line, true);
          } else {
            logMessage(line, true);
          }
        }
      } catch (Exception ex) {
        System.err.println("[WekaServer] A problem occurred while reading log "
          + "file for task '" + m_taskNameID + "'");
        ex.printStackTrace();
      } finally {
        if (br != null) {
          try {
            br.close();
          } catch (IOException e) {
          }
        }
      }
    }
  }

  /**
   * Clean up the log file
   */
  public void deleteLog() {
    if (!s_logRootDirCreated || !s_logSubDirCreated || m_taskNameID == null) {
      return;
    }

    if (m_logFile.exists()) {
      if (!m_logFile.delete()) {
        m_logFile.deleteOnExit();
      }
    }
  }
}
