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
 *    KnowledgeFlowApp.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow;

import weka.core.Defaults;
import weka.core.Environment;
import weka.core.Memory;
import weka.core.PluginManager;
import weka.core.Settings;
import weka.gui.AbstractGUIApplication;
import weka.gui.GenericObjectEditor;
import weka.gui.LookAndFeel;
import weka.gui.Perspective;
import weka.gui.PerspectiveManager;
import weka.knowledgeflow.BaseExecutionEnvironment;
import weka.knowledgeflow.ExecutionEnvironment;
import weka.knowledgeflow.KFDefaults;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Main Knowledge Flow application class
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class KnowledgeFlowApp extends AbstractGUIApplication {

  private static final long serialVersionUID = -1460599392623083983L;

  /** for monitoring the Memory consumption */
  protected static Memory m_Memory = new Memory(true);

  /**
   * variable for the KnowledgeFlowApp class which would be set to null by the
   * memory monitoring thread to free up some memory if we running out of memory
   */
  protected static KnowledgeFlowApp m_kfApp;

  /** Settings for the Knowledge Flow */
  protected Settings m_kfProperties;

  /** Main perspective of the Knowledge Flow */
  protected MainKFPerspective m_mainPerspective;

  /**
   * Constructor
   */
  public KnowledgeFlowApp() {
    this(true);
  }

  /**
   * Constructor
   * 
   * @param layoutComponent true if the Knowledge Flow should layout the
   *          application using the default layout - i.e. the perspectives
   *          toolbar at the north of a {@code BorderLayout} and the
   *          {@code PerspectiveManager} at the center
   */
  public KnowledgeFlowApp(boolean layoutComponent) {
    super(layoutComponent, "weka.gui.knowledgeflow", "weka.gui.SimpleCLIPanel");

    // add an initial "untitled" tab
    ((MainKFPerspective) m_perspectiveManager.getMainPerspective())
      .addUntitledTab();

    m_perspectiveManager
      .addSettingsMenuItemToProgramMenu(getApplicationSettings());

    if (m_perspectiveManager
      .userRequestedPerspectiveToolbarVisibleOnStartup(getApplicationSettings())) {
      showPerspectivesToolBar();
    }
  }

  /**
   * Get the name of this application
   *
   * @return the name of the application
   */
  @Override
  public String getApplicationName() {
    return KFDefaults.APP_NAME;
  }

  /**
   * Get the ID of this application
   *
   * @return the ID of the application
   */
  @Override
  public String getApplicationID() {
    return KFDefaults.APP_ID;
  }

  /**
   * Get the main perspective of this application
   *
   * @return the main perspective of the application
   */
  @Override
  public Perspective getMainPerspective() {
    if (m_mainPerspective == null) {
      m_mainPerspective = new MainKFPerspective();
    }
    return m_mainPerspective;
  }

  /**
   * Get the {@code PerspectiveManager} used by this application
   *
   * @return the {@code PerspectiveManager}
   */
  @Override
  public PerspectiveManager getPerspectiveManager() {
    return m_perspectiveManager;
  }

  /**
   * Get the settings for this application
   *
   * @return the settings for this application
   */
  @Override
  public Settings getApplicationSettings() {
    if (m_kfProperties == null) {
      m_kfProperties = new Settings("weka", KFDefaults.APP_ID);
      Defaults kfDefaults = new KnowledgeFlowGeneralDefaults();

      String envName =
        m_kfProperties.getSetting(KFDefaults.APP_ID,
          KnowledgeFlowGeneralDefaults.EXECUTION_ENV_KEY,
          KnowledgeFlowGeneralDefaults.EXECUTION_ENV,
          Environment.getSystemWide());
      try {
        ExecutionEnvironment envForDefaults =
          (ExecutionEnvironment) (envName
            .equals(BaseExecutionEnvironment.DESCRIPTION) ? new BaseExecutionEnvironment()
            : PluginManager.getPluginInstance(
              ExecutionEnvironment.class.getCanonicalName(), envName));

        Defaults envDefaults = envForDefaults.getDefaultSettings();
        if (envDefaults != null) {
          kfDefaults.add(envDefaults);
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }

      m_kfProperties.applyDefaults(kfDefaults);
    }
    return m_kfProperties;
  }

  /**
   * Get the default settings for this application
   *
   * @return the default settings
   */
  @Override
  public Defaults getApplicationDefaults() {
    return new KFDefaults();
  }

  /**
   * Apply (changed) settings
   */
  @Override
  public void settingsChanged() {
    boolean showTipText =
      getApplicationSettings().getSetting(KFDefaults.APP_ID,
        KFDefaults.SHOW_JTREE_TIP_TEXT_KEY,
        KFDefaults.SHOW_JTREE_GLOBAL_INFO_TIPS, Environment.getSystemWide());
    GenericObjectEditor.setShowGlobalInfoToolTips(showTipText);

    m_mainPerspective.m_stepTree.setShowLeafTipText(showTipText);
  }

  /**
   * General default settings for the Knowledge Flow
   */
  public static class KnowledgeFlowGeneralDefaults extends Defaults {

    private static final long serialVersionUID = 6957165806947500265L;

    public static final Settings.SettingKey LAF_KEY = new Settings.SettingKey(
      KFDefaults.APP_ID + ".lookAndFeel", "Look and feel for UI",
      "Note: a restart " + "is required for this setting ot come into effect");
    public static final String LAF = "";

    public static final Settings.SettingKey EXECUTION_ENV_KEY =
      new Settings.SettingKey(KFDefaults.APP_ID + ".exec_env",
        "Execution environment", "Executor for flow processes");

    public static final String EXECUTION_ENV =
      BaseExecutionEnvironment.DESCRIPTION;

    public KnowledgeFlowGeneralDefaults() {
      super(KFDefaults.APP_ID);

      List<String> lafs = LookAndFeel.getAvailableLookAndFeelClasses();
      lafs.add(0, "<use platform default>");
      LAF_KEY.setPickList(lafs);
      m_defaults.put(LAF_KEY, LAF);
      m_defaults.put(KFDefaults.SHOW_JTREE_TIP_TEXT_KEY,
        KFDefaults.SHOW_JTREE_GLOBAL_INFO_TIPS);

      Set<String> execs =
        PluginManager.getPluginNamesOfType(ExecutionEnvironment.class
          .getCanonicalName());
      List<String> execList = new LinkedList<String>();
      // make sure the default is listed first
      execList.add(BaseExecutionEnvironment.DESCRIPTION);
      if (execs != null) {
        for (String e : execs) {
          if (!e.equals(BaseExecutionEnvironment.DESCRIPTION)) {
            execList.add(e);
          }
        }
      }
      EXECUTION_ENV_KEY.setPickList(execList);
      m_defaults.put(EXECUTION_ENV_KEY, EXECUTION_ENV);
    }
  }

  /**
   * Main method for launching this application
   *
   * @param args command line args
   */
  public static void main(String[] args) {
    try {
      LookAndFeel.setLookAndFeel(KFDefaults.APP_ID, KFDefaults.APP_ID
        + ".lookAndFeel", KFDefaults.LAF);
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    weka.gui.GenericObjectEditor.determineClasses();

    try {
      if (System.getProperty("os.name").contains("Mac")) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
      }
      m_kfApp = new KnowledgeFlowApp();

      if (args.length == 1) {
        File toLoad = new File(args[0]);
        if (toLoad.exists() && toLoad.isFile()) {
          ((MainKFPerspective) m_kfApp.getMainPerspective()).loadLayout(toLoad,
            false);
        }
      }
      final javax.swing.JFrame jf =
        new javax.swing.JFrame("Weka " + m_kfApp.getApplicationName());
      jf.getContentPane().setLayout(new java.awt.BorderLayout());

      Image icon =
        Toolkit.getDefaultToolkit().getImage(
          KnowledgeFlowApp.class.getClassLoader().getResource(
            "weka/gui/weka_icon_new_48.png"));
      jf.setIconImage(icon);

      jf.getContentPane().add(m_kfApp, BorderLayout.CENTER);

      jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      jf.pack();
      m_kfApp.showMenuBar(jf);
      jf.setSize(1023, 768);
      jf.setVisible(true);
      // weird effect where, if there are more perspectives than would fit
      // in one row horizontally in the perspective manager, then the WrapLayout
      // does not wrap when the Frame is first pack()ed. No amount of
      // invalidating/revalidating/repainting components
      // and ancestors seems to make a difference. Resizing - even by one pixel
      // -
      // however, does force it to re-layout and wrap. Perhaps this is an OSX
      // bug...
      jf.setSize(1024, 768);

      Thread memMonitor = new Thread() {
        @Override
        public void run() {
          while (true) {
            // try {
            // System.out.println("Before sleeping.");
            // Thread.sleep(10);

            if (m_Memory.isOutOfMemory()) {
              // clean up
              jf.dispose();
              m_kfApp = null;
              System.gc();

              // display error
              System.err.println("\ndisplayed message:");
              m_Memory.showOutOfMemory();
              System.err.println("\nexiting");
              System.exit(-1);
            }
          }
        }
      };

      memMonitor.setPriority(Thread.MAX_PRIORITY);
      memMonitor.start();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
