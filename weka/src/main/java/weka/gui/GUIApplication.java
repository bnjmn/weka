package weka.gui;

import weka.core.Settings;

import javax.swing.*;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public interface GUIApplication {

  String getApplicationName();

  String getApplicationID();

  PerspectiveManager getPerspectiveManager();

  Perspective getMainPerspective();

  boolean isPerspectivesToolBarVisible();

  void hidePerspectivesToolBar();

  void showPerspectivesToolBar();

  void showErrorDialog(Exception cause);

  void showInfoDialog(Object information, String title, boolean isWarning);

  Settings getApplicationSettings();

  void settingsChanged();

  void revalidate();

  void notifyIsDirty();

  void showMenuBar(JFrame topLevelAncestor);
}
