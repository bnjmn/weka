/*
 *    RemoteEngine.java
 *    Copyright (C) 2000 Mark Hall
 *
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

package weka.experiment;

import java.rmi.*;
import java.rmi.server.*;
import java.net.InetAddress;

/**
 * A general purpose server for executing Task objects sent via RMI.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
public class RemoteEngine extends UnicastRemoteObject
  implements Compute {

  /**
   * Constructor
   * @exception RemoteException if something goes wrong
   */
  public RemoteEngine() throws RemoteException {
                      super();
  }
  
  /**
   * Takes a task object and calls its executeTask method
   * @param t the Task object to execute
   */
  public Object executeTask(Task t) {
    return t.execute();
  }
  
  /**
   * Main method. Gets address of the local host, creates a remote engine
   * object and binds it in the RMI registry. If there is no RMI registry,
   * then it tries to create one with default port 1099.
   *
   * @param args 
   */
  public static void main(String[] args) {
    if (System.getSecurityManager() == null) {
      System.setSecurityManager(new RMISecurityManager());
    }
    InetAddress localhost = null;
    try {
      localhost = InetAddress.getLocalHost();
      System.err.println("Host name : "+localhost.getHostName());
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    String name;
    if (localhost != null) {
      name = "//"+localhost.getHostName()+"/RemoteEngine";
    } else {
      name = "//localhost/RemoteEngine";
    }
    
    try {
      Compute engine = new RemoteEngine();
      Naming.rebind(name, engine);
      System.out.println("RemoteEngine bound in RMI registry");
    } catch (Exception e) {
      System.err.println("RemoteEngine exception: " + 
			 e.getMessage());
      // try to bootstrap a new registry
      try {
	System.err.println("Attempting to start rmi registry...");
	java.rmi.registry.LocateRegistry.createRegistry(1099);
	Compute engine = new RemoteEngine();
	Naming.rebind(name, engine);
	System.out.println("RemoteEngine bound in RMI registry");
      } catch (Exception ex) {
	ex.printStackTrace();
      }
    }
  }
}
