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
   *    GUIApplication
   *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
   *
   */

package weka.gui;

import weka.core.Defaults;
import weka.core.Settings;

import javax.swing.*;

/**
 * Interface to a GUIApplication that can have multiple "perspectives" and
 * provide application-level and perspective-level settings. Implementations
 * would typically extend {@code AbstractGUIApplication}.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public interface GUIApplication {

  /**
   * Get the name of this application
   *
   * @return the name of this application
   */
  String getApplicationName();

  /**
   * Get the ID of this application - any string unique to this application can
   * be used
   *
   * @return the ID of this application
   */
  String getApplicationID();

  /**
   * Get the {@code PerspectiveManager} in use by this application
   *
   * @return the {@code Perspective Manager}
   */
  PerspectiveManager getPerspectiveManager();

  /**
   * Get the main {@code Perspective} of this application - i.e. this is the
   * panel, tab, screen etc. that is visible first at start-up.
   *
   * @return the main perspective
   */
  Perspective getMainPerspective();

  /**
   * Returns true if the perspectives toolbar is visible at the current time
   *
   * @return true if the perspectives toolbar is visible
   */
  boolean isPerspectivesToolBarVisible();

  /**
   * Hide the perspectives toolbar
   */
  void hidePerspectivesToolBar();

  /**
   * Show the perspectives toolbar
   */
  void showPerspectivesToolBar();

  /**
   * Popup a dialog displaying the supplied Exception
   *
   * @param cause the exception to show
   */
  void showErrorDialog(Exception cause);

  /**
   * Popup an information dialog
   *
   * @param information the "information" (typically some text) to display
   * @param title the title for the dialog
   * @param isWarning true if this is a warning rather than just information
   */
  void showInfoDialog(Object information, String title, boolean isWarning);

  /**
   * Get the default values of settings for this application
   *
   * @return the default values of the settings for this applications
   */
  Defaults getApplicationDefaults();

  /**
   * Get the current settings for this application
   *
   * @return the current settings for this application
   */
  Settings getApplicationSettings();

  /**
   * Called when settings are changed by the user
   */
  void settingsChanged();

  /**
   * Force a re-validation and repaint() of the application
   */
  void revalidate();

  /**
   * Show the menu bar for the application
   *
   * @param topLevelAncestor the JFrame that contains the application
   */
  void showMenuBar(JFrame topLevelAncestor);
}
