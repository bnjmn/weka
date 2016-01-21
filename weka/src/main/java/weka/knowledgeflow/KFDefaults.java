package weka.knowledgeflow;

import weka.core.Defaults;
import weka.core.Settings;

import javax.swing.JPanel;
import java.awt.Color;

/**
 * Created by mhall on 22/01/15.
 */
public class KFDefaults extends Defaults {

  public static final String APP_NAME = "Knowledge Flow";
  public static final String APP_ID = "knowledgeflow";
  public static final String MAIN_PERSPECTIVE_ID = "knowledgeflow.main";

  // Main perspective settings
  public static final Settings.SettingKey MAX_UNDO_POINTS_KEY =
    new Settings.SettingKey(MAIN_PERSPECTIVE_ID + ".maxUndoPoints",
      "Maximum undo points", "Maximum number of states to keep in the undo"
        + "buffer");
  public static final int MAX_UNDO_POINTS = 20;

  public static final Settings.SettingKey LAYOUT_COLOR_KEY =
    new Settings.SettingKey(MAIN_PERSPECTIVE_ID + ".layoutcolor",
      "Layout background color", "");
  private static Color JP_COLOR = new JPanel().getBackground();
  public static final Color LAYOUT_COLOR = new Color(JP_COLOR.getRGB());

  public static final Settings.SettingKey SHOW_GRID_KEY =
    new Settings.SettingKey(MAIN_PERSPECTIVE_ID + ".showgrid", "Show grid",
      "The snap-to-grid grid");

  public static final boolean SHOW_GRID = false;

  public static final Settings.SettingKey GRID_COLOR_KEY =
    new Settings.SettingKey(MAIN_PERSPECTIVE_ID + ".gridcolor",
      "Grid line color", "The snap-to-grid line color");
  public static final Color GRID_COLOR = Color.LIGHT_GRAY;

  public static final Settings.SettingKey GRID_SPACING_KEY =
    new Settings.SettingKey(MAIN_PERSPECTIVE_ID + ".gridSpacing",
      "Grid spacing", "The spacing for snap-to-grid");
  public static final int GRID_SPACING = 40;

  public static final int SCROLL_BAR_INCREMENT_LAYOUT = 20;

  public static final Settings.SettingKey LAYOUT_WIDTH_KEY =
    new Settings.SettingKey(MAIN_PERSPECTIVE_ID + ".layoutWidth",
      "Layout width", "The width (in pixels) of the flow layout");
  public static final Settings.SettingKey LAYOUT_HEIGHT_KEY =
    new Settings.SettingKey(MAIN_PERSPECTIVE_ID + ".layoutHeight",
      "Layout height", "The height (in pixels) of the flow layout");

  public static final int LAYOUT_WIDTH = 2560;
  public static final int LAYOUT_HEIGHT = 1440;

  public static final Settings.SettingKey STEP_LABEL_FONT_SIZE_KEY =
    new Settings.SettingKey(MAIN_PERSPECTIVE_ID + ".stepLabelFontSize",
      "Font size for step/connection labels",
      "The point size of the font used to render "
        + "the names of steps and connections on the layout");
  public static final int STEP_LABEL_FONT_SIZE = 9;

  // Global app settings
  public static final Settings.SettingKey SHOW_JTREE_TIP_TEXT_KEY =
    new Settings.SettingKey(APP_ID + ".showGlobalInfoTipText",
      "Show scheme tool tips in tree view", "");
  public static final boolean SHOW_JTREE_GLOBAL_INFO_TIPS = true;

  public static final Settings.SettingKey LOGGING_LEVEL_KEY =
    new Settings.SettingKey(APP_ID + ".loggingLevel", "Logging level",
      "The logging level to use");
  public static final LoggingLevel LOGGING_LEVEL = LoggingLevel.BASIC;

  protected static final Settings.SettingKey[] DEFAULT_KEYS = {
    MAX_UNDO_POINTS_KEY, LAYOUT_COLOR_KEY, SHOW_GRID_KEY, GRID_COLOR_KEY,
    GRID_SPACING_KEY, LAYOUT_WIDTH_KEY, LAYOUT_HEIGHT_KEY,
    STEP_LABEL_FONT_SIZE_KEY };
  protected static final Object[] DEFAULT_VALUES = { MAX_UNDO_POINTS,
    LAYOUT_COLOR, SHOW_GRID, GRID_COLOR, GRID_SPACING, LAYOUT_WIDTH,
    LAYOUT_HEIGHT, STEP_LABEL_FONT_SIZE };

  /*
   * protected static void setDefaults(KFSettings props) { for (int i = 0; i <
   * DEFAULT_KEYS.length; i++) { props.setKFSetting(DEFAULT_KEYS[i],
   * DEFAULT_VALUES[i]); } }
   */

  public KFDefaults() {
    super(MAIN_PERSPECTIVE_ID);
    for (int i = 0; i < DEFAULT_KEYS.length; i++) {
      m_defaults.put(DEFAULT_KEYS[i], DEFAULT_VALUES[i]);
    }
  }
}
