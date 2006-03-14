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
 *    FilterCustomizer.java
 *    Copyright (C) 2002 Mark Hall
 *
 */

package weka.gui.beans;

import weka.core.Utils;
import weka.core.OptionHandler;
import java.beans.*;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import weka.gui.GenericObjectEditor;
import weka.gui.PropertyPanel;
import weka.filters.Filter;

/**
 * GUI customizer for the filter bean
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.4.2.1 $
 */
public class FilterCustomizer extends JPanel
  implements Customizer {
  
  static {
    GenericObjectEditor.registerEditors();
  }

  private PropertyChangeSupport m_pcSupport = 
    new PropertyChangeSupport(this);

  private weka.gui.beans.Filter m_filter;
  private GenericObjectEditor m_filterEditor = 
    new GenericObjectEditor(true);
 
  public FilterCustomizer() {
    try {
      m_filterEditor.
	setClassType(Filter.class);      
      m_filterEditor.setValue(new weka.filters.unsupervised.attribute.Add());
      m_filterEditor.addPropertyChangeListener(new PropertyChangeListener() {
	  public void propertyChange(PropertyChangeEvent e) {
	    repaint();
	    if (m_filter != null) {
	      Filter editedF = (Filter)m_filterEditor.getValue();
	      m_filter.setFilter(editedF);
	      // should pass on the property change to any other interested
	      // listeners
	    }
	  }
	});
      //      System.out.println("Here");
      repaint();
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    setLayout(new BorderLayout());
    add(m_filterEditor.getCustomEditor(), BorderLayout.CENTER);
  }
  
  /**
   * Set the filter bean to be edited
   *
   * @param object a Filter bean
   */
  public void setObject(Object object) {
    m_filter = (weka.gui.beans.Filter)object;
    m_filterEditor.setValue(m_filter.getFilter());
  }

  /**
   * Add a property change listener
   *
   * @param pcl a <code>PropertyChangeListener</code> value
   */
  public void addPropertyChangeListener(PropertyChangeListener pcl) {
    m_pcSupport.addPropertyChangeListener(pcl);
  }

  /**
   * Remove a property change listener
   *
   * @param pcl a <code>PropertyChangeListener</code> value
   */
  public void removePropertyChangeListener(PropertyChangeListener pcl) {
    m_pcSupport.removePropertyChangeListener(pcl);
  }
}

