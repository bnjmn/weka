/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    FlowRunner.java
 *    Copyright (C) 2008 Pentaho Corporation
 *
 */

package weka.gui.beans;

import java.util.Vector;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import weka.gui.Logger;
import weka.gui.beans.xml.*;

/**
 * Small utility class for executing KnowledgeFlow
 * flows outside of the KnowledgeFlow application
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}org
 * @version $Revision: 1.1.2.11 $
 */
public class FlowRunner {

  /** The potential flow(s) to execute */
  protected Vector m_beans;

  protected int m_runningCount = 0;

  protected transient Logger m_log = null;

  /**
   * Constructor
   */
  public FlowRunner() {
    // make sure that properties and plugins are loaded
    KnowledgeFlowApp.loadProperties();
  }

  public void setLog(Logger log) {
    m_log = log;
  }

  protected synchronized void launchThread(final Startable s, final int flowNum) {
    Thread t = new Thread() {
        private int m_num = flowNum;
        public void run() {
          try {
            s.start();
          } catch (Exception ex) {
            ex.printStackTrace();
            if (m_log != null) {
              m_log.logMessage(ex.getMessage());
            } else {
              System.err.println(ex.getMessage());
            }
          } finally {
            if (m_log != null) { 
              m_log.logMessage("[FlowRunner] flow " + m_num + " finished.");
            } else {
              System.out.println("[FlowRunner] Flow " + m_num + " finished.");
            }
            decreaseCount();
          }
        }
      };
    m_runningCount++;
    t.setPriority(Thread.MIN_PRIORITY);
    t.start();
  }

  protected synchronized void decreaseCount() {
    m_runningCount--;
  }

  public synchronized void stopAllFlows() {
    for (int i = 0; i < m_beans.size(); i++) {
      BeanInstance temp = (BeanInstance)m_beans.elementAt(i);
      if (temp.getBean() instanceof BeanCommon) {
        // try to stop any execution
        ((BeanCommon)temp.getBean()).stop();
      }
    }
  }

  /**
   * Waits until all flows have finished executing before returning
   *
   */
  public void waitUntilFinished() {
    try {
      while (m_runningCount > 0) {
        Thread.sleep(200);
      }
    } catch (Exception ex) {
      if (m_log != null) {
        m_log.logMessage("[FlowRunner] Attempting to stop all flows...");
      } else {
        System.err.println("[FlowRunner] Attempting to stop all flows...");
      }
      stopAllFlows();
      //      ex.printStackTrace();
    }
  }

  /**
   * Load a serialized KnowledgeFlow (either binary or xml)
   *
   * @param fileName the name of the file to load from
   * @throws Exception if something goes wrong
   */
  public void load(String fileName) throws Exception {
    if (!fileName.endsWith(".kf") && !fileName.endsWith(".kfml")) {
      throw new Exception("Can only load and run binary or xml serialized KnowledgeFlows "
                          + "(*.kf | *.kfml)");
    }
    
    if (fileName.endsWith(".kf")) {
      loadBinary(fileName);
    } else if (fileName.endsWith(".kfml")) {
      loadXML(fileName);
    }
  }

  /**
   * Load a binary serialized KnowledgeFlow
   *
   * @param fileName the name of the file to load from
   * @throws Exception if something goes wrong
   */
  public void loadBinary(String fileName) throws Exception {
    if (!fileName.endsWith(".kf")) {
      throw new Exception("File must be a binary flow (*.kf)");
    }

    InputStream is = new FileInputStream(fileName);
    ObjectInputStream ois = new ObjectInputStream(is);
    m_beans = (Vector)ois.readObject();
    
    // don't need the graphical connections
    ois.close();
  }

  /**
   * Load an XML serialized KnowledgeFlow
   *
   * @param fileName the name of the file to load from
   * @throws Exception if something goes wrong
   */
  public void loadXML(String fileName) throws Exception {
    if (!fileName.endsWith(".kfml")) {
      throw new Exception("File must be an XML flow (*.kfml)");
    }

    XMLBeans xml = new XMLBeans(null, null);
    Vector v = (Vector) xml.read(new File(fileName));
    m_beans = (Vector) v.get(XMLBeans.INDEX_BEANINSTANCES);
  }
  
  /**
   * Get the vector holding the flow(s)
   *
   * @return the Vector holding the flow(s)
   */
  public Vector getFlows() {
    return m_beans;
  }

  /**
   * Set the vector holding the flows(s) to run
   *
   * @param beans the Vector holding the flows to run
   */
  public void setFlows(Vector beans) {
    m_beans = beans;
  }

  /**
   * Launch all loaded KnowledgeFlow
   *
   * @throws Exception if something goes wrong during execution
   */
  public void run() throws Exception {
    if (m_beans == null) {
      throw new Exception("Don't seem to have any beans I can execute.");
    }
    
    int numFlows = 1;

    // look for a Startable bean...
    for (int i = 0; i < m_beans.size(); i++) {
      BeanInstance tempB = (BeanInstance)m_beans.elementAt(i);
      if (tempB.getBean() instanceof Startable) {
        Startable s = (Startable)tempB.getBean();
        // start that sucker...
        if (m_log != null) {
          m_log.logMessage("[FlowRunner] Launching flow "+numFlows+"...");
        } else {
          System.out.println("[FlowRunner] Launching flow "+numFlows+"...");
        }
        launchThread(s, numFlows);
        numFlows++;
      }
    }
  }

  /**
   * Main method for testing this class. <p>
   * <br>Usage:<br><br>
   * <pre>Usage:\n\nFlowRunner <serialized kf file></pre>
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    
    if (args.length != 1) {
      System.err.println("Usage:\n\nFlowRunner <serialized kf file>");
    } else {
      try {
        FlowRunner fr = new FlowRunner();
        String fileName = args[0];

        fr.load(fileName);
        fr.run();
        fr.waitUntilFinished();
        System.out.println("Finished all flows.");
      } catch (Exception ex) {
        ex.printStackTrace();
        System.err.println(ex.getMessage());
      }
    }                         
  }
}