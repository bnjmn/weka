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
 *    TestSetEvent.java
 *    Copyright (C) 2002 Mark Hall
 *
 */

package weka.gui.beans;

import java.util.EventObject;
import weka.core.Instances;

/**
 * Event encapsulating a test set
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.2 $
 */
public class TestSetEvent extends EventObject {
  
  /**
   * The test set instances
   */
  protected Instances m_testSet;
  private boolean m_structureOnly;

  /**
   * what number is this test set (ie fold 2 of 10 folds)
   */
  protected int m_setNumber;

  /**
   * Maximum number of sets (ie 10 in a 10 fold)
   */
  protected int m_maxSetNumber;

  public TestSetEvent(Object source, Instances testSet) {
    super(source);
    m_testSet = testSet;
    if (m_testSet != null && m_testSet.numInstances() == 0) {
      m_structureOnly = true;
    }
  }

  /**
   * Get the test set instances
   *
   * @return an <code>Instances</code> value
   */
  public Instances getTestSet() {
    return m_testSet;
  }

  /**
   * Get the test set number (eg. fold 2 of a 10 fold split)
   *
   * @return an <code>int</code> value
   */
  public int getSetNumber() {
    return m_setNumber;
  }

  /**
   * Get the maximum set number
   *
   * @return an <code>int</code> value
   */
  public int getMaxSetNumber() {
    return m_maxSetNumber;
  }

  /**
   * Returns true if the encapsulated instances
   * contain just header information
   *
   * @return true if only header information is
   * available in this DataSetEvent
   */
  public boolean isStructureOnly() {
    return m_structureOnly;
  }
}
