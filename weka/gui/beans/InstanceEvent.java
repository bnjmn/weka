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
 *    InstanceEvent.java
 *    Copyright (C) 2002 Mark Hall
 *
 */

package weka.gui.beans;

import java.util.EventObject;
import weka.core.Instance;

/**
 * Event that encapsulates a single instance
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.2 $
 * @see EventObject
 */
public class InstanceEvent extends EventObject {
  public static final int FORMAT_AVAILABLE = 0;
  public static final int INSTANCE_AVAILABLE = 1;
  public static final int BATCH_FINISHED = 2;
  
  private Instance m_instance;
  private int m_status;

  /**
   * Creates a new <code>InstanceEvent</code> instance.
   *
   * @param source the source of the event
   * @param instance the instance
   * @param status status code
   */
  public InstanceEvent(Object source, Instance instance, int status) {
    super(source);
    m_instance = instance;
    m_status = status;
  }

  public InstanceEvent(Object source) {
    super(source);
  }
  
  /**
   * Get the instance
   *
   * @return an <code>Instance</code> value
   */
  public Instance getInstance() {
    return m_instance;
  }
  
  /**
   * Set the instance
   *
   * @param i an <code>Instance</code> value
   */
  public void setInstance(Instance i) {
    m_instance = i;
  }

  /**
   * Get the status
   *
   * @return an <code>int</code> value
   */
  public int getStatus() {
    return m_status;
  }

  /**
   * Set the status
   *
   * @param s an <code>int</code> value
   */
  public void setStatus(int s) {
    m_status = s;
  }
}
