package weka.gui;

import java.util.List;

import weka.core.Defaults;
import weka.core.Settings;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class WorkbenchDefaults extends Defaults {

  public static final String APP_NAME = "Workbench";
  public static final String APP_ID = "workbench";

  protected static final Settings.SettingKey LAF_KEY = new Settings.SettingKey(
    APP_ID + ".lookAndFeel", "Look and feel for UI",
    "Note: a restart is required for this setting to come into effect");
  protected static final String LAF = "";
  protected static final Settings.SettingKey SHOW_JTREE_TIP_TEXT_KEY =
    new Settings.SettingKey(APP_ID + ".showGlobalInfoTipText",
      "Show scheme tool tips in tree view", "");
  protected static final boolean SHOW_JTREE_GLOBAL_INFO_TIPS = true;
  private static final long serialVersionUID = 7881327795923189743L;

  public WorkbenchDefaults() {
    super(APP_ID);

    List<String> lafs = LookAndFeel.getAvailableLookAndFeelClasses();
    lafs.add(0, "<use platform default>");
    LAF_KEY.setPickList(lafs);
    m_defaults.put(LAF_KEY, LAF);
    m_defaults.put(SHOW_JTREE_TIP_TEXT_KEY, SHOW_JTREE_GLOBAL_INFO_TIPS);
  }
}
