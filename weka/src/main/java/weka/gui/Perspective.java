package weka.gui;

import java.util.List;

import javax.swing.Icon;
import javax.swing.JMenu;

import weka.core.Defaults;
import weka.core.Instances;

public interface Perspective {

  /**
   * Gets called when startup of the application has completed. At this point,
   * and only at this point, is it guaranteed that a perspective has access to
   * its hosting application and the PerspectiveManager. Implementations can use
   * this method to complete their initialization in this method if this
   * requires access to information from the main application and/or the
   * PerspectiveManager (i.e. knowledge about what other perspectives are
   * available).
   */
  void instantiationComplete();

  /**
   * Returns true if this perspective is OK with being an active perspective -
   * i.e. the user can click on this perspective at this time in the perspective
   * toolbar. For example, a Perspective might return false from this method if
   * it needs a set of instances to operate but none have been supplied yet.
   * 
   * @return true if this perspective can be active at the current time
   */
  boolean okToBeActive();

  /**
   * Set active status of this perspective. True indicates that this perspective
   * is the visible active perspective in the application
   * 
   * @param active true if this perspective is the active one
   */
  void setActive(boolean active);

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
  void setLoaded(boolean loaded);

  /**
   * Set the main application. Gives other perspectives access to information
   * provided by the main application
   * 
   * @param main the main application
   */
  void setMainApplication(GUIApplication main);

  /**
   * Get the main application that this perspective belongs to
   *
   * @return the main application that this perspective belongs to
   */
  GUIApplication getMainApplication();

  /**
   * Get the ID of this perspective
   *
   * @return the ID of this perspective
   */
  String getPerspectiveID();

  /**
   * Get the title of this perspective
   * 
   * @return the title of this perspective
   */
  String getPerspectiveTitle();

  /**
   * Get the icon for this perspective
   *
   * @return the icon for this perspective
   */
  Icon getPerspectiveIcon();

  /**
   * Get the tool tip text for this perspective
   *
   * @return the tool tip text for this perspective
   */
  String getPerspectiveTipText();

  /**
   * Get an ordered list of menus to appear in the main menu bar. Return null
   * for no menus
   *
   * @return a list of menus to appear in the main menu bar or null for no menus
   */
  List<JMenu> getMenus();

  /**
   * Get the default settings for this perspective (or null if there are none)
   *
   * @return the default settings for this perspective, or null if the
   *         perspective does not have any settings
   */
  Defaults getDefaultSettings();

  /**
   * Called when the user alters settings. The settings altered by the user are
   * not necessarily ones related to this perspective
   */
  void settingsChanged();

  /**
   * Returns true if this perspective can do something meaningful with a set of
   * instances
   *
   * @return true if this perspective accepts instances
   */
  boolean acceptsInstances();

  /**
   * Set instances (if this perspective can use them)
   *
   * @param instances the instances
   */
  void setInstances(Instances instances);

  /**
   * Whether this perspective requires a graphical log to write to
   *
   * @return true if a log is needed by this perspective
   */
  boolean requiresLog();

  /**
   * Set a log to use (if required by the perspective)
   *
   * @param log the graphical log to use
   */
  void setLog(Logger log);
}
