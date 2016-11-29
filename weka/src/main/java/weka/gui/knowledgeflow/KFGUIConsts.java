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
 *    KFGUIConsts.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow;

/**
 * Class that holds constants that are used within the GUI side of the
 * Knowledge Flow. Housing them here allows classes outside of the GUI
 * part of the Knowledge Flow to access them without unecessary loading
 * of GUI-related classes.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class KFGUIConsts {

  /** Base path for step icons */
  public static final String BASE_ICON_PATH = "weka/gui/knowledgeflow/icons/";

  /** Flow file directory key */
  public static final String FLOW_DIRECTORY_KEY =
    "Internal.knowledgeflow.directory";

  /** Group identifier for built-in knowledge flow templates */
  public static final String KF_BUILTIN_TEMPLATE_KEY =
    "weka.knowledgeflow.templates";

  /**
   * Group identifier for plugin knowledge flow templates - packages supplying
   * templates should use this
   */
  public static final String KF_PLUGIN_TEMPLATE_KEY =
    "weka.knowledgeflow.plugin.templates";

  /** Props for templates */
  protected static final String TEMPLATE_PROPERTY_FILE =
    "weka/gui/knowledgeflow/templates/templates.props";

  /** Constant for an open dialog (same as JFileChooser.OPEN_DIALOG) */
  public static final int OPEN_DIALOG = 0;

  /** Constant for a save dialog (same as JFileChooser.SAVE_DIALOG) */
  public static final int SAVE_DIALOG = 1;
}
