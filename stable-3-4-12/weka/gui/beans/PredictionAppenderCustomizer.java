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
 *    PredictionAppenderCustomizer.java
 *    Copyright (C) 2003 Mark Hall
 *
 */

package weka.gui.beans;

import java.beans.*;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import weka.gui.PropertySheetPanel;

/**
 * GUI Customizer for the prediction appender bean
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.1 $
 */

public class PredictionAppenderCustomizer extends JPanel
  implements Customizer {

  private PropertyChangeSupport m_pcSupport = 
    new PropertyChangeSupport(this);
  
  private PropertySheetPanel m_paEditor = 
    new PropertySheetPanel();

  public PredictionAppenderCustomizer() {
    setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 5, 5));

    setLayout(new BorderLayout());
    add(m_paEditor, BorderLayout.CENTER);
    add(new javax.swing.JLabel("PredcitionAppenderCustomizer"), 
	BorderLayout.NORTH);
  }

  /**
   * Set the object to be edited
   *
   * @param object a PredictionAppender object
   */
  public void setObject(Object object) {
    m_paEditor.setTarget((PredictionAppender)object);
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
