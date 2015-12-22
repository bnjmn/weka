package weka.gui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JPanel;

import weka.core.Defaults;
import weka.core.Instances;
import weka.gui.knowledgeflow.StepVisual;

public abstract class AbstractPerspective extends JPanel implements Perspective {

  protected boolean m_isActive;

  protected boolean m_isLoaded;

  protected GUIApplication m_mainApplication;

  protected String m_perspectiveTitle = "";

  protected String m_perspectiveID = "";

  protected String m_perspectiveTipText = "";

  protected Icon m_perspectiveIcon;

  protected Logger m_log;

  public AbstractPerspective() {

  }

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

  @Override
  public void setActive(boolean active) {
    m_isActive = active;
  }

  @Override
  public void setLoaded(boolean loaded) {
    m_isLoaded = loaded;
  }

  @Override
  public void setMainApplication(GUIApplication main) {
    m_mainApplication = main;
  }

  @Override
  public GUIApplication getMainApplication() {
    return m_mainApplication;
  }

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

  @Override
  public Icon getPerspectiveIcon() {
    if (m_perspectiveIcon != null) {
      return m_perspectiveIcon;
    }

    PerspectiveInfo perspectiveA =
      this.getClass().getAnnotation(PerspectiveInfo.class);
    if (perspectiveA != null && perspectiveA.iconPath() != null
      && perspectiveA.iconPath().length() > 0) {
      m_perspectiveIcon = StepVisual.loadIcon(perspectiveA.iconPath());
    }

    return m_perspectiveIcon;
  }

  @Override
  public List<JMenu> getMenus() {
    return new ArrayList<JMenu>();
  }

  @Override
  public void setInstances(Instances instances) {
    // subclasses to override as necessary
  }

  @Override
  public boolean acceptsInstances() {
    // subclasses to override as necessary
    return false;
  }

  @Override
  public boolean requiresLog() {
    // subclasses to override as necessary
    return false;
  }

  @Override
  public Defaults getDefaultSettings() {
    // subclasses to override if they have default settings
    return null;
  }

  public void settingsChanged() {
    // no-op. subclasses to override if necessary
  }

  public void setLog(Logger log) {
    m_log = log;
  }

  public String toString() {
    return getPerspectiveTitle();
  }
}
