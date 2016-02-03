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
 *    AbstractGUIApplication
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui;

import weka.core.Settings;
import weka.knowledgeflow.LogManager;

import javax.swing.*;
import java.awt.*;

/**
 * Base class for GUI applications in Weka
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public abstract class AbstractGUIApplication extends JPanel implements
  GUIApplication {

  private static final long serialVersionUID = -2116770422043462730L;

  /** Manages perspectives and provides the perspectives toolbar */
  protected PerspectiveManager m_perspectiveManager;

  /** The settings for the application */
  protected Settings m_applicationSettings;

  /**
   * Default constructor
   */
  public AbstractGUIApplication() {
    this(true);
  }

  /**
   * Constructor
   * 
   * @param layoutComponent true if the application should layout the component
   *          with the "default" layout - i.e. the perspectives toolbar at the
   *          north of a {@code BorderLayout} and the {@code PerspectiveManager}
   *          at the center
   * @param allowedPerspectiveClassPrefixes {@code Perspective}s (loaded via the
   *          PluginManager) whose fully qualified class names start with these
   *          prefixes will be displayed in this application
   * @param disallowedPerspectiveClassPrefixes {@code Perspective}s (loaded via
   *          the PluginManager) whose fully qualified class names start with
   *          these prefixes will not be displayed in this application. Note
   *          that disallowedPerspectiveClassPrefixes override
   *          allowedPerspectivePrefixes
   */
  public AbstractGUIApplication(boolean layoutComponent,
    String[] allowedPerspectiveClassPrefixes,
    String[] disallowedPerspectiveClassPrefixes) {

    m_perspectiveManager =
      new PerspectiveManager(this, allowedPerspectiveClassPrefixes,
        disallowedPerspectiveClassPrefixes);
    m_perspectiveManager.setMainApplicationForAllPerspectives();
    if (layoutComponent) {
      setLayout(new BorderLayout());
      add(m_perspectiveManager, BorderLayout.CENTER);
      if (m_perspectiveManager.perspectiveToolBarIsVisible()) {
        add(m_perspectiveManager.getPerspectiveToolBar(), BorderLayout.NORTH);
      }
    }
  }

  /**
   * Constructor
   * 
   * @param layoutComponent true if the application should layout the component
   *          with the "default" layout - i.e. the perspectives toolbar at the
   *          north of a {@code BorderLayout} and the {@code PerspectiveManager}
   *          at the center
   * @param allowedPerspectiveClassPrefixes {@code Perspective}s (loaded via the
   *          PluginManager) whose fully qualified class names start with these
   *          prefixes will be displayed in this application
   */
  public AbstractGUIApplication(boolean layoutComponent,
    String... allowedPerspectiveClassPrefixes) {

    this(layoutComponent, allowedPerspectiveClassPrefixes, new String[0]);
  }

  /**
   * Get the {@code PerspectiveManager} in use by this application
   *
   * @return the {@code Perspective Manager}
   */
  @Override
  public PerspectiveManager getPerspectiveManager() {
    return m_perspectiveManager;
  }

  /**
   * Get the current settings for this application
   *
   * @return the current settings for this application
   */
  @Override
  public Settings getApplicationSettings() {
    if (m_applicationSettings == null) {
      m_applicationSettings = new Settings("weka", getApplicationID());
      m_applicationSettings.applyDefaults(getApplicationDefaults());
    }
    return m_applicationSettings;
  }

  /**
   * Returns true if the perspectives toolbar is visible at the current time
   *
   * @return true if the perspectives toolbar is visible
   */
  @Override
  public boolean isPerspectivesToolBarVisible() {
    return m_perspectiveManager.perspectiveToolBarIsVisible();
  }

  /**
   * Hide the perspectives toolbar
   */
  @Override
  public void hidePerspectivesToolBar() {
    if (isPerspectivesToolBarVisible()) {
      m_perspectiveManager.setPerspectiveToolBarIsVisible(false);
      remove(m_perspectiveManager.getPerspectiveToolBar());
    }
  }

  /**
   * Show the perspectives toolbar
   */
  @Override
  public void showPerspectivesToolBar() {
    if (!isPerspectivesToolBarVisible()) {
      m_perspectiveManager.setPerspectiveToolBarIsVisible(true);
      add(m_perspectiveManager.getPerspectiveToolBar(), BorderLayout.NORTH);
    }
  }

  /**
   * Called when settings are changed by the user
   */
  @Override
  public void settingsChanged() {
    // no-op. Subclasses to override if necessary
  }

  /**
   * Show the menu bar for the application
   *
   * @param topLevelAncestor the JFrame that contains the application
   */
  @Override
  public void showMenuBar(JFrame topLevelAncestor) {
    m_perspectiveManager.showMenuBar(topLevelAncestor);
  }

  /**
   * Popup a dialog displaying the supplied Exception
   *
   * @param cause the exception to show
   */
  @Override
  public void showErrorDialog(Exception cause) {
    String stackTrace = LogManager.stackTraceToString(cause);

    Object[] options = null;

    if (stackTrace != null && stackTrace.length() > 0) {
      options = new Object[2];
      options[0] = "OK";
      options[1] = "Show error";
    } else {
      options = new Object[1];
      options[0] = "OK";
    }
    int result =
      JOptionPane.showOptionDialog(this,
        "An error has occurred: " + cause.getMessage(), getApplicationName(),
        JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, null, options,
        options[0]);

    if (result == 1) {
      JTextArea jt = new JTextArea(stackTrace, 10, 40);
      JOptionPane.showMessageDialog(this, new JScrollPane(jt),
        getApplicationName(), JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Popup an information dialog
   *
   * @param information the "information" (typically some text) to display
   * @param title the title for the dialog
   * @param isWarning true if this is a warning rather than just information
   */
  @Override
  public void
    showInfoDialog(Object information, String title, boolean isWarning) {
    JOptionPane
      .showMessageDialog(this, information, title,
        isWarning ? JOptionPane.WARNING_MESSAGE
          : JOptionPane.INFORMATION_MESSAGE);
  }

  /**
   * Force a re-validation and repaint() of the application
   */
  @Override
  public void revalidate() {
    if (getTopLevelAncestor() != null) {
      getTopLevelAncestor().revalidate();
      getTopLevelAncestor().repaint();
    } else {
      super.revalidate();
    }
    repaint();
  }
}
