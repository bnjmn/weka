package weka.gui.knowledgeflow;

import weka.core.Defaults;
import weka.core.Environment;
import weka.core.Settings;
import weka.gui.AbstractGUIApplication;
import weka.gui.GenericObjectEditor;
import weka.gui.LookAndFeel;
import weka.gui.Perspective;
import weka.gui.PerspectiveManager;
import weka.gui.beans.PluginManager;
import weka.knowledgeflow.BaseExecutionEnvironment;
import weka.knowledgeflow.ExecutionEnvironment;
import weka.knowledgeflow.KFDefaults;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class KnowledgeFlowApp extends AbstractGUIApplication {

  private static final long serialVersionUID = -1460599392623083983L;

  protected Settings m_kfProperties;

  protected MainKFPerspective m_mainPerspective;

  public KnowledgeFlowApp() {
    this(true);
  }

  public KnowledgeFlowApp(boolean layoutComponent) {
    super(layoutComponent, "weka.gui.knowledgeflow", "weka.gui.SimpleCLIPanel");

    // add an initial "untitled" tab
    ((MainKFPerspective) m_perspectiveManager.getMainPerspective())
      .addUntitledTab();

    m_perspectiveManager
      .addSettingsMenuItemToProgramMenu(getApplicationSettings());

    if (m_perspectiveManager.userRequestedPerspectiveToolbarVisibleOnStartup(
      getApplicationSettings())) {
      showPerspectivesToolBar();
    }
  }

  public String getApplicationName() {
    return KFDefaults.APP_NAME;
  }

  @Override
  public String getApplicationID() {
    return KFDefaults.APP_ID;
  }

  @Override
  public Perspective getMainPerspective() {
    if (m_mainPerspective == null) {
      m_mainPerspective = new MainKFPerspective();
    }
    return m_mainPerspective;
  }

  @Override
  public PerspectiveManager getPerspectiveManager() {
    return m_perspectiveManager;
  }

  @Override
  public Settings getApplicationSettings() {
    if (m_kfProperties == null) {
      m_kfProperties = new Settings("weka", KFDefaults.APP_ID);
      Defaults kfDefaults = new KnowledgeFlowGeneralDefaults();

      String envName = m_kfProperties.getSetting(KFDefaults.APP_ID,
        KnowledgeFlowGeneralDefaults.EXECUTION_ENV_KEY,
        KnowledgeFlowGeneralDefaults.EXECUTION_ENV,
        Environment.getSystemWide());
      try {
        ExecutionEnvironment envForDefaults = (ExecutionEnvironment) (envName
          .equals(BaseExecutionEnvironment.DESCRIPTION)
            ? new BaseExecutionEnvironment()
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

  @Override
  public void settingsChanged() {
    boolean showTipText = getApplicationSettings().getSetting(KFDefaults.APP_ID,
      KFDefaults.SHOW_JTREE_TIP_TEXT_KEY,
      KFDefaults.SHOW_JTREE_GLOBAL_INFO_TIPS, Environment.getSystemWide());
    GenericObjectEditor.setShowGlobalInfoToolTips(showTipText);

    m_mainPerspective.m_stepTree.setShowLeafTipText(showTipText);
  }

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
      m_defaults.put(KFDefaults.LOGGING_LEVEL_KEY, KFDefaults.LOGGING_LEVEL);

      Set<String> execs = PluginManager
        .getPluginNamesOfType(ExecutionEnvironment.class.getCanonicalName());
      List<String> execList = new ArrayList<String>();
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

  public static void main(String[] args) {
    try {
      LookAndFeel.setLookAndFeel(KFDefaults.APP_ID,
        KFDefaults.APP_ID + ".lookAndFeel");
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    weka.gui.GenericObjectEditor.determineClasses();

    try {
      if (System.getProperty("os.name").contains("Mac")) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
      }
      KnowledgeFlowApp app = new KnowledgeFlowApp();

      if (args.length == 1) {
        File toLoad = new File(args[0]);
        if (toLoad.exists() && toLoad.isFile()) {
          ((MainKFPerspective) app.getMainPerspective()).loadLayout(toLoad,
            false);
        }
      }
      final javax.swing.JFrame jf =
        new javax.swing.JFrame("Weka " + app.getApplicationName());
      jf.getContentPane().setLayout(new java.awt.BorderLayout());

      Image icon = Toolkit.getDefaultToolkit().getImage(KnowledgeFlowApp.class
        .getClassLoader().getResource("weka/gui/weka_icon_new_48.png"));
      jf.setIconImage(icon);

      jf.getContentPane().add(app, BorderLayout.CENTER);

      jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      jf.pack();
      app.showMenuBar(jf);
      jf.setSize(1024, 768);
      jf.setVisible(true);

    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
