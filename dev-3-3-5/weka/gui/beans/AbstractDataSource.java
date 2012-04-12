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
 *    AbstractDataSource.java
 *    Copyright (C) 2002 Mark Hall
 *
 */

package weka.gui.beans;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;
import java.util.Vector;
import java.awt.*;
import java.io.Serializable;


/**
 * Abstract class for objects that can provide instances from some source
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.1 $
 * @since 1.0
 * @see JPanel
 * @see DataSource
 * @see Serializable
 */
public abstract class AbstractDataSource extends JPanel
  implements DataSource, Visible, Serializable {

  /**
   * Default visual for data sources
   */
  protected BeanVisual m_visual = 
    new BeanVisual("AbstractDataSource", 
		   BeanVisual.ICON_PATH+"DefaultDataSource.gif",
		   BeanVisual.ICON_PATH+"DefaultDataSource_animated.gif");
  
  /**
   * Objects listening for events from data sources
   */
  protected Vector m_listeners;

  /**
   * Creates a new <code>AbstractDataSource</code> instance.
   *
   */
  public AbstractDataSource() {    
    useDefaultVisual();
    setLayout(new BorderLayout());
    add(m_visual, BorderLayout.CENTER);
    m_listeners = new Vector();
  }

  /**
   * Add a listener
   *
   * @param dsl a <code>DataSourceListener</code> value
   */
  public synchronized void addDataSourceListener(DataSourceListener dsl) {
    m_listeners.addElement(dsl);
  }
  
  /**
   * Remove a listener
   *
   * @param dsl a <code>DataSourceListener</code> value
   */
  public synchronized void removeDataSourceListener(DataSourceListener dsl) {
    m_listeners.remove(dsl);
  }

  /**
   * Add an instance listener
   *
   * @param dsl a <code>InstanceListener</code> value
   */
  public synchronized void addInstanceListener(InstanceListener dsl) {
    m_listeners.addElement(dsl);
  }
  
  /**
   * Remove an instance listener
   *
   * @param dsl a <code>InstanceListener</code> value
   */
  public synchronized void removeInstanceListener(InstanceListener dsl) {
    m_listeners.remove(dsl);
  }

  /**
   * Set the visual for this data source
   *
   * @param newVisual a <code>BeanVisual</code> value
   */
  public void setVisual(BeanVisual newVisual) {
    m_visual = newVisual;
  }

  /**
   * Get the visual being used by this data source.
   *
   */
  public BeanVisual getVisual() {
    return m_visual;
  }

  /**
   * Use the default images for a data source
   *
   */
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH+"DefaultDataSource.gif",
		       BeanVisual.ICON_PATH+"DefaultDataSource_animated.gif");
  }
}
