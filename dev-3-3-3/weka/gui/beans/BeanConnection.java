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
 *    BeanConnection.java
 *    Copyright (C) 2002 Mark Hall
 *
 */

package weka.gui.beans;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Vector;
import java.beans.Beans;
import java.beans.EventSetDescriptor;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Color;
import javax.swing.JComponent;
import java.beans.EventSetDescriptor;

/**
 * Class for encapsulating a connection between two beans. Also
 * maintains a list of all connections
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.1 $
 */
public class BeanConnection implements Serializable {

  /**
   * The list of connections
   */
  public static Vector CONNECTIONS = new Vector();

  // details for this connection
  private BeanInstance m_source;
  private BeanInstance m_target;

  /**
   * The name of the event for this connection
   */
  private String m_eventName;

  /**
   * Reset the list of connections
   */
  public static void reset() {
    CONNECTIONS = new Vector();
  }

  /**
   * Returns the list of connections
   *
   * @return the list of connections
   */
  public static Vector getConnections() {
    return CONNECTIONS;
  }

  /**
   * Describe <code>setConnections</code> method here.
   *
   * @param connections a <code>Vector</code> value
   */
  public static void setConnections(Vector connections) {
    CONNECTIONS = connections;
  }

  /**
   * Returns true if there is a link between the supplied source and
   * target BeanInstances at an earlier index than the supplied index
   *
   * @param source the source BeanInstance
   * @param target the target BeanInstance
   * @param index the index to compare to
   * @return true if there is already a link at an earlier index
   */
  private static boolean previousLink(BeanInstance source, BeanInstance target,
				      int index) {
    for (int i = 0; i < CONNECTIONS.size(); i++) {
      BeanConnection bc = (BeanConnection)CONNECTIONS.elementAt(i);
      BeanInstance compSource = bc.getSource();
      BeanInstance compTarget = bc.getTarget();

      if (compSource == source && compTarget == target && index < i) {
	return true;
      }
    }
    return false;
  }

  /**
   * Renders the connections and their names on the supplied graphics
   * context
   *
   * @param gx a <code>Graphics</code> value
   */
  public static void paintConnections(Graphics gx) {
    for (int i = 0; i < CONNECTIONS.size(); i++) {
      BeanConnection bc = (BeanConnection)CONNECTIONS.elementAt(i);
      BeanInstance source = bc.getSource();
      BeanInstance target = bc.getTarget();
      EventSetDescriptor srcEsd = bc.getSourceEventSetDescriptor();
      BeanVisual sourceVisual = (source.getBean() instanceof Visible) ?
	((Visible)source.getBean()).getVisual() :
	null;
      BeanVisual targetVisual = (target.getBean() instanceof Visible) ?
	((Visible)target.getBean()).getVisual() :
	null;
      if (sourceVisual != null && targetVisual != null) {
	Point bestSourcePt = 
	  sourceVisual.getClosestConnectorPoint(
		       new Point((target.getX()+(target.getWidth()/2)), 
				 (target.getY() + (target.getHeight() / 2))));
	Point bestTargetPt = 
	  targetVisual.getClosestConnectorPoint(
		       new Point((source.getX()+(source.getWidth()/2)), 
				 (source.getY() + (source.getHeight() / 2))));
	gx.setColor(Color.red);
	boolean active = true;
	if (source.getBean() instanceof EventConstraints) {
	  if (!((EventConstraints) source.getBean()).
	      eventGeneratable(srcEsd.getName())) {
	    gx.setColor(Color.gray); // link not active at this time
	    active = false;
	  }
	}
	gx.drawLine((int)bestSourcePt.getX(), (int)bestSourcePt.getY(),
		    (int)bestTargetPt.getX(), (int)bestTargetPt.getY());

	// paint the connection name
	int midx = (int)bestSourcePt.getX();
	midx += (int)((bestTargetPt.getX() - bestSourcePt.getX()) / 2);
	int midy = (int)bestSourcePt.getY();
	midy += (int)((bestTargetPt.getY() - bestSourcePt.getY()) / 2) - 2 ;
	gx.setColor((active) ? Color.blue : Color.gray);
	if (previousLink(source, target, i)) {
	  midy -= 15;
	}
	gx.drawString(srcEsd.getName(), midx, midy);
      }
    }
  }

  /**
   * Return a list of connections within some delta of a point
   *
   * @param pt the point at which to look for connections
   * @param delta connections have to be within this delta of the point
   * @return a list of connections
   */
  public static Vector getClosestConnections(Point pt, int delta) {
    Vector closestConnections = new Vector();
    
    for (int i = 0; i < CONNECTIONS.size(); i++) {
      BeanConnection bc = (BeanConnection)CONNECTIONS.elementAt(i);
      BeanInstance source = bc.getSource();
      BeanInstance target = bc.getTarget();
      EventSetDescriptor srcEsd = bc.getSourceEventSetDescriptor();
      BeanVisual sourceVisual = (source.getBean() instanceof Visible) ?
	((Visible)source.getBean()).getVisual() :
	null;
      BeanVisual targetVisual = (target.getBean() instanceof Visible) ?
	((Visible)target.getBean()).getVisual() :
	null;
      if (sourceVisual != null && targetVisual != null) {
	Point bestSourcePt = 
	  sourceVisual.getClosestConnectorPoint(
		       new Point((target.getX()+(target.getWidth()/2)), 
				 (target.getY() + (target.getHeight() / 2))));
	Point bestTargetPt = 
	  targetVisual.getClosestConnectorPoint(
		       new Point((source.getX()+(source.getWidth()/2)), 
				 (source.getY() + (source.getHeight() / 2))));

	int minx = (int) Math.min(bestSourcePt.getX(), bestTargetPt.getX());
	int maxx = (int) Math.max(bestSourcePt.getX(), bestTargetPt.getX());
	int miny = (int) Math.min(bestSourcePt.getY(), bestTargetPt.getY());
	int maxy = (int) Math.max(bestSourcePt.getY(), bestTargetPt.getY());
	// check to see if supplied pt is inside bounding box
	if (pt.getX() >= minx-delta && pt.getX() <= maxx+delta && 
	    pt.getY() >= miny-delta && pt.getY() <= maxy+delta) {
	  // now see if the point is within delta of the line
	  // formulate ax + by + c = 0
	  double a = bestSourcePt.getY() - bestTargetPt.getY();
	  double b = bestTargetPt.getX() - bestSourcePt.getX();
	  double c = (bestSourcePt.getX() * bestTargetPt.getY()) -
	    (bestTargetPt.getX() * bestSourcePt.getY());
	  
	  double distance = Math.abs((a * pt.getX()) + (b * pt.getY()) + c);
	  distance /= Math.abs(Math.sqrt((a*a) + (b*b)));

	  if (distance <= delta) {
	    closestConnections.addElement(bc);
	  }
	}
      }
    }
    return closestConnections;
  }

  /**
   * Remove all connections for a bean. If the bean is a target for
   * receiving events then it gets deregistered from the corresonding
   * source bean. If the bean is a source of events then all targets 
   * implementing BeanCommon are notified via their
   * disconnectionNotification methods that the source (and hence the
   * connection) is going away.
   *
   * @param instance the bean to remove connections to/from
   */
  public static void removeConnections(BeanInstance instance) {
    
    Vector removeVector = new Vector();
    for (int i = 0; i < CONNECTIONS.size(); i++) {
      // In cases where this instance is the target, deregister it
      // as a listener for the source
      BeanConnection bc = (BeanConnection)CONNECTIONS.elementAt(i);
      BeanInstance tempTarget = bc.getTarget();
      BeanInstance tempSource = bc.getSource();

      EventSetDescriptor tempEsd = bc.getSourceEventSetDescriptor();
      if (instance == tempTarget) {
	// try to deregister the target as a listener for the source
	try {
	  Method deregisterMethod = tempEsd.getRemoveListenerMethod();
	  Object targetBean = tempTarget.getBean();
	  Object [] args = new Object[1];
	  args[0] = targetBean;
	  deregisterMethod.invoke(tempSource.getBean(), args);
	  System.err.println("Deregistering listener");
	  removeVector.addElement(bc);
	} catch (Exception ex) {
	  ex.printStackTrace();
	}
      } else if (instance == tempSource) {
	removeVector.addElement(bc);
	if (tempTarget.getBean() instanceof BeanCommon) {
	  // tell the target that the source is going away, therefore
	  // this type of connection is as well
	  ((BeanCommon)tempTarget.getBean()).
	    disconnectionNotification(tempEsd.getName(),
				      tempSource.getBean());
	}
      }
    }
    for (int i = 0; i < removeVector.size(); i++) {
      System.err.println("removing connection");
      CONNECTIONS.removeElement((BeanConnection)removeVector.elementAt(i));
    }
  }

  /**
   * Creates a new <code>BeanConnection</code> instance.
   *
   * @param source the source bean
   * @param target the target bean
   * @param esd the EventSetDescriptor for the connection
   */
  public BeanConnection(BeanInstance source, BeanInstance target,
			EventSetDescriptor esd) {
    m_source = source;
    m_target = target;
    //    m_sourceEsd = sourceEsd;
    m_eventName = esd.getName();
    System.err.println(m_eventName);

    // attempt to connect source and target beans
    Method registrationMethod = 
      //      m_sourceEsd.getAddListenerMethod();
      //      getSourceEventSetDescriptor().getAddListenerMethod();
      esd.getAddListenerMethod();
    Object targetBean = m_target.getBean();
    Object [] args = new Object[1];
    args[0] = targetBean;
    Class listenerClass = esd.getListenerType();
    if (listenerClass.isInstance(targetBean)) {
      try {
	registrationMethod.invoke(m_source.getBean(), args);
	// if the target implements BeanCommon, then inform
	// it that it has been registered as a listener with a source via
	// the named listener interface
	if (targetBean instanceof BeanCommon) {
	  ((BeanCommon)targetBean).
	    connectionNotification(esd.getName(), m_source.getBean());
	}
	CONNECTIONS.addElement(this);
      } catch (Exception ex) {
	System.err.println("Unable to connect beans (BeanConnection)");
	ex.printStackTrace();
      }
    } else {
      System.err.println("Unable to connect beans (BeanConnection)");
    }
  }
  
  /**
   * Remove this connection
   */
  public void remove() {
    EventSetDescriptor tempEsd = getSourceEventSetDescriptor();
    // try to deregister the target as a listener for the source
    try {
      Method deregisterMethod = tempEsd.getRemoveListenerMethod();
      Object targetBean = getTarget().getBean();
      Object [] args = new Object[1];
      args[0] = targetBean;
      deregisterMethod.invoke(getSource().getBean(), args);
      System.err.println("Deregistering listener");
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    if (getTarget().getBean() instanceof BeanCommon) {
      // tell the target that this connection is going away
      ((BeanCommon)getTarget().getBean()).
	disconnectionNotification(tempEsd.getName(),
				  getSource().getBean());
    }

    CONNECTIONS.remove(this);
  }

  /**
   * returns the source BeanInstance for this connection
   *
   * @return a <code>BeanInstance</code> value
   */
  protected BeanInstance getSource() {
    return m_source;
  }

  /**
   * Returns the target BeanInstance for this connection
   *
   * @return a <code>BeanInstance</code> value
   */
  protected BeanInstance getTarget() {
    return m_target;
  }

  /**
   * Returns the event set descriptor for the event generated by the source
   * for this connection
   *
   * @return an <code>EventSetDescriptor</code> value
   */
  protected EventSetDescriptor getSourceEventSetDescriptor() {
    JComponent bc = (JComponent)m_source.getBean();
     try {
       BeanInfo sourceInfo = Introspector.getBeanInfo(bc.getClass());
       if (sourceInfo == null) {
       System.err.println("Error");
       } else {
	 EventSetDescriptor [] esds = sourceInfo.getEventSetDescriptors();
	 for (int i = 0; i < esds.length; i++) {
	   if (esds[i].getName().compareTo(m_eventName) == 0) {
	     return esds[i];
	   }
	 }
       }
     } catch (Exception ex) {
       System.err.println("Problem retrieving event set descriptor (BeanConnection)");
     }
     return null;
     
     //    return m_sourceEsd;
  }
}
