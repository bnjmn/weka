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
 *    Loader.java
 *    Copyright (C) 2002 Mark Hall
 *
 */

package weka.gui.beans;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.InputEvent;
import java.awt.*;
import java.io.Serializable;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;
import java.util.Vector;
import java.util.Enumeration;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.*;
import java.io.IOException;


/**
 * Loads data sets using weka.core.converter classes
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.2 $
 * @since 1.0
 * @see AbstractDataSource
 * @see UserRequestAcceptor
 */
public class Loader extends AbstractDataSource 
  implements UserRequestAcceptor, WekaWrapper,
	     EventConstraints {

  /**
   * Holds the instances loaded
   */
  private transient Instances m_dataSet;

  /**
   * Thread for doing IO in
   */
  private LoadThread m_ioThread;

  /**
   * Loader
   */
  private weka.core.converters.Loader m_Loader = new ArffLoader();

  private InstanceEvent m_ie = new InstanceEvent(this);

  private class LoadThread extends Thread {
    private DataSource m_DP;

    public LoadThread(DataSource dp) {
      m_DP = dp;
    }

    public void run() {
      try {
	m_visual.setAnimated();
	boolean instanceGeneration = true;
	// determine if we are going to produce data set or instance events
	for (int i = 0; i < m_listeners.size(); i++) {
	  if (m_listeners.elementAt(i) instanceof DataSourceListener) {
	    instanceGeneration = false;
	    break;
	  }
	}

	if (instanceGeneration) {
	  boolean start = true;
	  Instance nextInstance = null;
	  try {
	    nextInstance = m_Loader.getNextInstance();
	  } catch (IOException e) {
	    e.printStackTrace();
	  }
	  int z = 0;
	  while (nextInstance != null) {
	    //	    format.add(nextInstance);
	    /*	    InstanceEvent ie = (start)
	      ? new InstanceEvent(m_DP, nextInstance, 
				  InstanceEvent.FORMAT_AVAILABLE)
		: new InstanceEvent(m_DP, nextInstance, 
		InstanceEvent.INSTANCE_AVAILABLE); */
	    if (start) {
	      m_ie.setStatus(InstanceEvent.FORMAT_AVAILABLE);
	    } else {
	      m_ie.setStatus(InstanceEvent.INSTANCE_AVAILABLE);
	    }
	    m_ie.setInstance(nextInstance);
	    start = false;
	    notifyInstanceLoaded(m_ie);
	    z++;
	    //	    System.err.println(z);
	    nextInstance = m_Loader.getNextInstance();
	  }
	  m_visual.setStatic();
	} else {
	  m_dataSet = m_Loader.getDataSet();
	  m_visual.setStatic();
	  m_visual.setText(m_dataSet.relationName());
	  notifyDataSetLoaded(new DataSetEvent(m_DP, m_dataSet));
	}
      } catch (Exception ex) {
	ex.printStackTrace();
      } finally {
	m_ioThread = null;
	//	m_visual.setText("Finished");
	//	m_visual.setIcon(m_inactive.getVisual());
	m_visual.setStatic();
      }
    }
  }

  public Loader() {
    super();
    setLoader(m_Loader);
  }

  /**
   * Set the loader to use
   *
   * @param loader a <code>weka.core.converters.Loader</code> value
   */
  public void setLoader(weka.core.converters.Loader loader) {
    boolean loadImages = true;
    if (loader.getClass().getName().
	compareTo(m_Loader.getClass().getName()) == 0) {
      loadImages = false;
    }
    m_Loader = loader;
    String loaderName = loader.getClass().toString();
    loaderName = loaderName.substring(loaderName.
				      lastIndexOf('.')+1, 
				      loaderName.length());
    if (loadImages) {

      if (!m_visual.loadIcons(BeanVisual.ICON_PATH+loaderName+".gif",
			    BeanVisual.ICON_PATH+loaderName+"_animated.gif")) {
	useDefaultVisual();
      }
    }
    m_visual.setText(loaderName);
  }

  /**
   * Get the loader
   *
   * @return a <code>weka.core.converters.Loader</code> value
   */
  public weka.core.converters.Loader getLoader() {
    return m_Loader;
  }

  /**
   * Set the loader
   *
   * @param algorithm a Loader
   * @exception IllegalArgumentException if an error occurs
   */
  public void setWrappedAlgorithm(Object algorithm) 
    {

    if (!(algorithm instanceof weka.core.converters.Loader)) { 
      throw new IllegalArgumentException(algorithm.getClass()+" : incorrect "
					 +"type of algorithm (Loader)");
    }
    setLoader((weka.core.converters.Loader)algorithm);
  }

  /**
   * Get the loader
   *
   * @return a Loader
   */
  public Object getWrappedAlgorithm() {
    return getLoader();
  }

  /**
   * Notify all Data source listeners that a data set has been loaded
   *
   * @param e a <code>DataSetEvent</code> value
   */
  protected void notifyDataSetLoaded(DataSetEvent e) {
    Vector l;
    synchronized (this) {
      l = (Vector)m_listeners.clone();
    }
    
    if (l.size() > 0) {
      for(int i = 0; i < l.size(); i++) {
	((DataSourceListener)l.elementAt(i)).acceptDataSet(e);
      }
      m_dataSet = null;
    }
  }

  /**
   * Notify all instance listeners that a new instance is available
   *
   * @param e an <code>InstanceEvent</code> value
   */
  protected void notifyInstanceLoaded(InstanceEvent e) {
    Vector l;
    synchronized (this) {
      l = (Vector)m_listeners.clone();
    }
    
    if (l.size() > 0) {
      for(int i = 0; i < l.size(); i++) {
	((InstanceListener)l.elementAt(i)).acceptInstance(e);
      }
      m_dataSet = null;
    }
  }

 
  /**
   * Start loading data
   */
  public void startLoading() {
    if (m_ioThread == null) {
      //      m_visual.setText(m_dataSetFile.getName());
      m_ioThread = new LoadThread(Loader.this);
      m_ioThread.setPriority(Thread.MIN_PRIORITY);
      m_ioThread.start();
    } else {
      m_ioThread = null;
    }
  }

  /**
   * Get a list of user requests
   *
   * @return an <code>Enumeration</code> value
   */
  public Enumeration enumerateRequests() {
    Vector newVector = new Vector(0);
    if (m_ioThread == null) {
      newVector.addElement("Start loading");
    }
    return newVector.elements();
  }

  /**
   * Perform the named request
   *
   * @param request a <code>String</code> value
   * @exception IllegalArgumentException if an error occurs
   */
  public void performRequest(String request) {
    if (request.compareTo("Start loading") == 0) {
      startLoading();
    } else {
      throw new IllegalArgumentException(request
					 + " not supported (Loader)");
    }
  }

  /**
   * Returns true if the named event can be generated at this time
   *
   * @param eventName the event
   * @return a <code>boolean</code> value
   */
  public boolean eventGeneratable(String eventName) {
    if (eventName.compareTo("instance") == 0) {
      if (!(m_Loader instanceof weka.core.converters.IncrementalLoader)) {
	return false;
      }
      for (int i = 0; i < m_listeners.size(); i++) {
	if (m_listeners.elementAt(i) instanceof DataSourceListener) {
	  return false;
	}
      }
    }

    if (eventName.compareTo("dataSet") == 0) {
      if (!(m_Loader instanceof weka.core.converters.BatchLoader)) {
	return false;
      }
      for (int i = 0; i < m_listeners.size(); i++) {
	if (m_listeners.elementAt(i) instanceof InstanceListener) {
	  return false;
	}
      }
    }
    return true;
  }
}

