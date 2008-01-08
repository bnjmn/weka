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

/**
 * Small utility class for executing KnowledgeFlow
 * flows outside of the KnowledgeFlow application
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}org
 * @version $Revision: 1.1 $
 */
public class FlowRunner {

  protected void launchThread(final Startable s) {
    Thread t = new Thread() {
        public void run() {
          try {
            s.start();
          } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println(ex.getMessage());
          }
        }
      };
    
    t.setPriority(Thread.MIN_PRIORITY);
    t.start();
  }

  protected void loadAndRun(String fileName) throws Exception {
    if (!fileName.endsWith(".kf")) {
      throw new Exception("Can only load and run binary serialized KnowledgeFlows (*.kf)");
    }

    Vector beans = null;
    InputStream is = new FileInputStream(fileName);
    ObjectInputStream ois = new ObjectInputStream(is);
    beans = (Vector)ois.readObject();
    
    // don't need the graphical connections
    ois.close();
    
    int numFlows = 1;

    // look for a Startable bean...
    for (int i = 0; i < beans.size(); i++) {
      BeanInstance tempB = (BeanInstance)beans.elementAt(i);
      if (tempB.getBean() instanceof Startable) {
        Startable s = (Startable)tempB.getBean();
        // start that sucker...
        System.out.println("Launching flow "+numFlows+"...");
        numFlows++;
        launchThread(s);
      }
    }
  }

  public static void main(String[] args) {
    
    if (args.length != 1) {
      System.err.println("Usage:\n\nFlowRunner <serialized kf file>");
    } else {
      try {
        FlowRunner fr = new FlowRunner();
        fr.loadAndRun(args[0]);
      } catch (Exception ex) {
        ex.printStackTrace();
        System.err.println(ex.getMessage());
      }
    }                         
  }
}