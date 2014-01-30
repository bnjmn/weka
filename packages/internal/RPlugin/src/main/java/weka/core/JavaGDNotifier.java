/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    JavaGDNotifier.java
 *    Copyright (C) 2012-2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

/**
 * Interface to something that maintains a list of listeners that are interested
 * in graphics produced from R via the JavaGD graphics device.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public interface JavaGDNotifier {

  /**
   * Add a listener
   * 
   * @param l the listener to add
   */
  void addListener(JavaGDListener l);

  /**
   * Remove a listener
   * 
   * @param l the listener to remove
   */
  void removeListener(JavaGDListener l);

  /**
   * Tell the notifier to notify the listeners of any cached graphics
   * 
   * @param additional a varargs list of additional listeners (beyond those
   *          maintained by this notifier) to notify.
   */
  void notifyListeners(JavaGDListener... additional);
}
