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
 *    InteractiveTablePanel.java
 *    Copyright (C) 2011-2013 University of Waikato, Hamilton, New Zealand
 */

package weka.gui.beans;

/**
 * Provides a panel using an interactive table model.
 * 
 * @author Mark Hall (mhall{[at]}penthao{[dot]}com)
 * @version $Revision$
 * @deprecated Use {@code weka.gui.InteractiveTablePanel} instead. Retained for
 * backward compatibility
 */
@Deprecated
public class InteractiveTablePanel extends weka.gui.InteractiveTablePanel {

  private static final long serialVersionUID = -5331129312037269302L;

  /**
   * Constructor
   * 
   * @param colNames the names of the columns
   */
  public InteractiveTablePanel(String[] colNames) {
    super(colNames);
  }
}
