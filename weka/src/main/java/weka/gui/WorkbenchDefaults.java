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
 *    WorkbenchDefaults.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui;

import weka.core.Defaults;
import weka.core.Settings;

import java.util.List;

/**
 * Default settings for the Workbench app.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class WorkbenchDefaults extends Defaults {

  public static final String APP_NAME = "Workbench";
  public static final String APP_ID = "workbench";

  protected static final Settings.SettingKey LAF_KEY = new Settings.SettingKey(
    APP_ID + ".lookAndFeel", "Look and feel for UI",
    "Note: a restart is required for this setting to come into effect");
  protected static final String LAF = "javax.swing.plaf.nimbus.NimbusLookAndFeel";
  protected static final Settings.SettingKey SHOW_JTREE_TIP_TEXT_KEY =
    new Settings.SettingKey(APP_ID + ".showGlobalInfoTipText",
      "Show scheme tool tips in tree view", "");
  protected static final boolean SHOW_JTREE_GLOBAL_INFO_TIPS = true;
  protected static final Settings.SettingKey LOG_MESSAGE_FONT_SIZE_KEY =
    new Settings.SettingKey(APP_ID + ".logMessageFontSize",
      "Size of font for log " + "messages",
      "Size of font for log messages (-1 = system default)");
  protected static final int LOG_MESSAGE_FONT_SIZE = -1;
  private static final long serialVersionUID = 7881327795923189743L;

  /**
   * Constructor
   */
  public WorkbenchDefaults() {
    super(APP_ID);

    List<String> lafs = LookAndFeel.getAvailableLookAndFeelClasses();
    lafs.add(0, "<use platform default>");
    LAF_KEY.setPickList(lafs);
    m_defaults.put(LAF_KEY, LAF);
    m_defaults.put(SHOW_JTREE_TIP_TEXT_KEY, SHOW_JTREE_GLOBAL_INFO_TIPS);
    m_defaults.put(LOG_MESSAGE_FONT_SIZE_KEY, LOG_MESSAGE_FONT_SIZE);
  }
}
