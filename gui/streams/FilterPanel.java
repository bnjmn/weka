/*
 *    FilterPanel.java
 *    Copyright (C) 1999 Len Trigg
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

package weka.gui.streams;

import weka.core.Instance;
import weka.core.Instances;
import java.io.Serializable;
import java.util.Vector;
import javax.swing.JPanel;
import weka.filters.Filter;

/** 
 * A component that allows a user to select and configure a weka
 * filter, and then allows instances to be filtered through.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
public class FilterPanel extends JPanel
  implements Serializable, InstanceProducer, InstanceListener {
  
  /** The listeners */
  private Vector listeners = new Vector();

  /** Debugging mode */
  private boolean m_Debug = false;

  /** The filter to use */
  private Filter m_Filter = new AllFilter();
  
  /**
   * Gets the Filter to use
   *
   * @return the Filter being used.
   */
  public Filter getFilter() {
    
    return m_Filter;
  }
  
  /**
   * Sets the Filter to use.
   *
   * @param newFilter the new Filter.
   */
  public void setFilter(Filter newFilter) {
    
    m_Filter = newFilter;
  }
  
  /* Important bits
     
     Resetting the filter at appropriate times
     if (!secondData.equalHeaders(firstData)) {
	throw new Exception("Input file formats differ.\n");
      }
      
     Setting the filter options
      if (filter instanceof OptionHandler) {
	((OptionHandler)filter).parseOptions(options);
      }

    boolean printedHeader = false;
    if (filter.inputFormat(firstData)) {
      firstOutput.println(filter.outputFormat().toString());
      printedHeader = true;
    }
    
    // Pass all the instances to the filter
    while (firstData.readInstance(firstInput)) {
      if (filter.input(firstData.instance(0))) {
	if (!printedHeader) {
	  System.err.println("Filter didn't return true from inputFormat() "+
			     "earlier!");
	  System.exit(0);
	}
	firstOutput.println(filter.output().toString());
      }
      firstData.delete(0);
    }
    
    // Say that input has finished, and print any pending output instances
    if (filter.batchFinished()) {
      if (!printedHeader) {
	firstOutput.println(filter.outputFormat().toString());
      }
      while (filter.numPendingOutput() > 0) {
	firstOutput.println(filter.output().toString());
      }
    }
    
    if (firstOutput != null) {
      firstOutput.close();
    }    

  */

  
  public void setDebug(boolean debug) {
    m_Debug = debug;
  }

  public boolean getDebug() {
    return m_Debug;
  }

  public synchronized void addInstanceListener(InstanceListener ipl) {
    listeners.addElement(ipl);
  }

  public synchronized void removeInstanceListener(InstanceListener ipl) {
    listeners.removeElement(ipl);
  }

  protected void notifyInstanceProduced(InstanceEvent e) {

    if (listeners.size() > 0) {
      if (m_Debug) {
	System.err.println(m_Filter.getClass().getName()+
			   "::notifyInstanceProduced()");
      }
      Vector l;
      synchronized (this) {
	l = (Vector)listeners.clone();
      }
      for(int i = 0; i < l.size(); i++) {
	((InstanceListener)l.elementAt(i)).instanceProduced(e);
      }
      // If there are any listeners, and the event is an INSTANCE_AVAILABLE,
      // they should have retrieved the instance with outputPeek();
      // we must now pop the instance to stop the queue from building
      // up.
      try {
	if (e.getID() == InstanceEvent.INSTANCE_AVAILABLE) {
	  output();
	}
      } catch (Exception ex) {
	System.err.println("Problem: notifyInstanceProduced() was\n"
			   +"called with INSTANCE_AVAILABLE, but output()\n"
			   +"threw an exception: " + ex.getMessage());
      }
    }
  }

  public void instanceProduced(InstanceEvent e) {
    
    Object source = e.getSource();
    if (source instanceof InstanceProducer) { 
      int id = e.getID();
      if (m_Debug) {
	System.err.println(m_Filter.getClass().getName()
			   + "::instanceProduced() - "
			   + (id == InstanceEvent.FORMAT_AVAILABLE
			      ? "Format available"
			      : (id == InstanceEvent.INSTANCE_AVAILABLE
				 ? "Instance available"
				 : (id == InstanceEvent.BATCH_FINISHED
				    ? "End of instance batch"
				    : "Unknown event type"
				    ))));
      }
      
      try {
	InstanceProducer a = (InstanceProducer) source;
	switch (id) {
	case InstanceEvent.FORMAT_AVAILABLE:
	  m_Filter.inputFormat(a.outputFormat());
	  break;
	case InstanceEvent.INSTANCE_AVAILABLE:
	  m_Filter.input(a.outputPeek());
	  break;
	case InstanceEvent.BATCH_FINISHED:
	  m_Filter.batchFinished();
	  break;
	default:
	  break;
	}
      }
      catch (Exception ex) {
	System.err.println(ex.getMessage());
      }
    } else {
      System.err.println(m_Filter.getClass().getName()
			 + "::instanceProduced() - Unknown source object "+
			 "type");
    }
  }
}
