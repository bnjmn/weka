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
 * AbstractPerspective.java
 * Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 */

package weka.gui;

import weka.core.Defaults;
import weka.core.Instances;
import weka.gui.knowledgeflow.StepVisual;

import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.List;

/**
 * Base classes for GUI perspectives to extend. Clients that extend this class
 * and make use of the {@code @PerspectiveInfo} annotation will only need to
 * override/implement a few methods.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public abstract class AbstractPerspective extends JPanel implements Perspective {

  /** For serialization */
  private static final long serialVersionUID = 1919714661641262879L;

  /** True if this perspective is currently the active/visible one */
  protected boolean m_isActive;

  /** True if this perspective has been loaded */
  protected boolean m_isLoaded;

  /** The main application that is displaying this perspective */
  protected GUIApplication m_mainApplication;

  /** The title of the perspective */
  protected String m_perspectiveTitle = "";

  /** The ID of the perspective */
  protected String m_perspectiveID = "";

  /** Tip text for this perspective */
  protected String m_perspectiveTipText = "";

  /** Icon for this perspective */
  protected Icon m_perspectiveIcon;

  /** Logger for this perspective */
  protected Logger m_log;

  /**
   * Constructor
   */
  public AbstractPerspective() {
  }

  /**
   * Constructor
   *
   * @param ID the ID of the perspective
   * @param title the title of the the perspective
   */
  public AbstractPerspective(String ID, String title) {
    m_perspectiveTitle = title;
    m_perspectiveID = ID;
  }

  /**
   * No-opp implementation. Subclasses should override if they can only complete
   * initialization by accessing the main application and/or the
   * PerspectiveManager. References to these two things are guaranteed to be
   * available when this method is called during the startup process
   */
  @Override
  public void instantiationComplete() {
    // no-opp method. Subclasses should override
  }

  /**
   * Returns true if the perspective is usable at this time. This is a no-opp
   * implementation that always returns true. Subclasses should override if
   * there are specific conditions that need to be met (e.g. can't operate if
   * there are no instances set).
   * 
   * @return true if this perspective is usable at this time
   */
  @Override
  public boolean okToBeActive() {
    return true;
  }

  /**
   * Set active status of this perspective. True indicates that this perspective
   * is the visible active perspective in the application
   *
   * @param active true if this perspective is the active one
   */
  @Override
  public void setActive(boolean active) {
    m_isActive = active;
  }

  /**
   * Set whether this perspective is "loaded" - i.e. whether or not the user has
   * opted to have it available in the perspective toolbar. The perspective can
   * make the decision as to allocating or freeing resources on the basis of
   * this. Note that the main application and perspective manager instances are
   * not available to the perspective until the instantiationComplete() method
   * has been called.
   *
   * @param loaded true if the perspective is available in the perspective
   *          toolbar of the KnowledgeFlow
   */
  @Override
  public void setLoaded(boolean loaded) {
    m_isLoaded = loaded;
  }

  /**
   * Set the main application. Gives other perspectives access to information
   * provided by the main application
   *
   * @param main the main application
   */
  @Override
  public void setMainApplication(GUIApplication main) {
    m_mainApplication = main;
  }

  /**
   * Get the main application that this perspective belongs to
   *
   * @return the main application that this perspective belongs to
   */
  @Override
  public GUIApplication getMainApplication() {
    return m_mainApplication;
  }

  /**
   * Get the ID of this perspective
   *
   * @return the ID of this perspective
   */
  @Override
  public String getPerspectiveID() {
    if (m_perspectiveID != null && m_perspectiveID.length() > 0) {
      return m_perspectiveID;
    }

    PerspectiveInfo perspectiveA =
      this.getClass().getAnnotation(PerspectiveInfo.class);
    if (perspectiveA != null) {
      m_perspectiveID = perspectiveA.ID();
    }

    return m_perspectiveID;
  }

  /**
   * Get the title of this perspective
   *
   * @return the title of this perspective
   */
  @Override
  public String getPerspectiveTitle() {
    if (m_perspectiveTitle != null && m_perspectiveTitle.length() > 0) {
      return m_perspectiveTitle;
    }

    PerspectiveInfo perspectiveA =
      this.getClass().getAnnotation(PerspectiveInfo.class);
    if (perspectiveA != null) {
      m_perspectiveTitle = perspectiveA.title();
    }

    return m_perspectiveTitle;
  }

  /**
   * Get the tool tip text for this perspective
   *
   * @return the tool tip text for this perspective
   */
  @Override
  public String getPerspectiveTipText() {
    if (m_perspectiveTipText != null && m_perspectiveTipText.length() > 0) {
      return m_perspectiveTipText;
    }

    PerspectiveInfo perspectiveA =
      this.getClass().getAnnotation(PerspectiveInfo.class);
    if (perspectiveA != null) {
      m_perspectiveTipText = perspectiveA.toolTipText();
    }

    return m_perspectiveTipText;
  }

  /**
   * Get the icon for this perspective
   *
   * @return the icon for this perspective
   */
  @Override
  public Icon getPerspectiveIcon() {
    if (m_perspectiveIcon != null) {
      return m_perspectiveIcon;
    }

    PerspectiveInfo perspectiveA =
      this.getClass().getAnnotation(PerspectiveInfo.class);
    if (perspectiveA != null && perspectiveA.iconPath() != null
      && perspectiveA.iconPath().length() > 0) {
      m_perspectiveIcon =
        StepVisual.loadIcon(this.getClass().getClassLoader(),
          perspectiveA.iconPath());
    }

    return m_perspectiveIcon;
  }

  /**
   * Get an ordered list of menus to appear in the main menu bar. Return null
   * for no menus
   *
   * @return a list of menus to appear in the main menu bar or null for no menus
   */
  @Override
  public List<JMenu> getMenus() {
    return new ArrayList<JMenu>();
  }

  /**
   * Set instances (if this perspective can use them)
   *
   * @param instances the instances
   */
  @Override
  public void setInstances(Instances instances) {
    // subclasses to override as necessary
  }

  /**
   * Returns true if this perspective can do something meaningful with a set of
   * instances
   *
   * @return true if this perspective accepts instances
   */
  @Override
  public boolean acceptsInstances() {
    // subclasses to override as necessary
    return false;
  }

  /**
   * Whether this perspective requires a graphical log to write to
   *
   * @return true if a log is needed by this perspective
   */
  @Override
  public boolean requiresLog() {
    // subclasses to override as necessary
    return false;
  }

  /**
   * Get the default settings for this perspective (or null if there are none)
   *
   * @return the default settings for this perspective, or null if the
   *         perspective does not have any settings
   */
  @Override
  public Defaults getDefaultSettings() {
    // subclasses to override if they have default settings
    return null;
  }

  /**
   * Called when the user alters settings. The settings altered by the user are
   * not necessarily ones related to this perspective
   */
  @Override
  public void settingsChanged() {
    // no-op. subclasses to override if necessary
  }

  /**
   * Set a log to use (if required by the perspective)
   *
   * @param log the graphical log to use
   */
  @Override
  public void setLog(Logger log) {
    m_log = log;
  }

  /**
   * Returns the perspective's title
   *
   * @return the title of the perspective
   */
  public String toString() {
    return getPerspectiveTitle();
  }
}
