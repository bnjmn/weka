package weka.gui.knowledgeflow;

import weka.core.Defaults;
import weka.core.Environment;
import weka.core.Instances;
import weka.core.Settings;
import weka.gui.AbstractPerspective;
import weka.gui.PerspectiveInfo;
import weka.gui.visualize.MatrixPanel;

import java.awt.*;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@PerspectiveInfo(ID = ScatterPlotMatrixPerspective.ScatterDefaults.ID,
  title = "Scatter plot matrix", toolTipText = "Scatter plots",
  iconPath = "weka/gui/knowledgeflow/icons/application_view_tile.png")
public class ScatterPlotMatrixPerspective extends AbstractPerspective {

  private static final long serialVersionUID = 5661598509822826837L;

  protected MatrixPanel m_matrixPanel;

  protected Instances m_visualizeDataSet;

  public ScatterPlotMatrixPerspective() {
    setLayout(new BorderLayout());
    m_matrixPanel = new MatrixPanel();
    add(m_matrixPanel, BorderLayout.CENTER);
  }

  @Override
  public Defaults getDefaultSettings() {
    return new ScatterDefaults();
  }

  @Override
  public boolean acceptsInstances() {
    return true;
  }

  @Override
  public void setActive(boolean active) {
    super.setActive(active);
    if (m_isActive && m_visualizeDataSet != null) {
      int pointSize = m_mainApplication.getApplicationSettings().getSetting(
        ScatterDefaults.ID, ScatterDefaults.POINT_SIZE_KEY,
        ScatterDefaults.POINT_SIZE, Environment.getSystemWide());
      int plotSize = m_mainApplication.getApplicationSettings().getSetting(
        ScatterDefaults.ID, ScatterDefaults.PLOT_SIZE_KEY,
        ScatterDefaults.PLOT_SIZE, Environment.getSystemWide());
      m_matrixPanel.setPointSize(pointSize);
      m_matrixPanel.setPlotSize(plotSize);
      m_matrixPanel.updatePanel();
    }
  }

  @Override
  public void setInstances(Instances instances) {
    m_visualizeDataSet = instances;
    m_matrixPanel.setInstances(m_visualizeDataSet);
  }

  @Override
  public boolean okToBeActive() {
    return m_visualizeDataSet != null;
  }

  public static class ScatterDefaults extends Defaults {
    public static final String ID = "weka.gui.knowledgeflow.scatterplotmatrix";

    public static final Settings.SettingKey POINT_SIZE_KEY =
      new Settings.SettingKey(ID + ".pointSize", "Point size for scatter plots",
        "");
    public static final int POINT_SIZE = 1;

    public static final Settings.SettingKey PLOT_SIZE_KEY =
      new Settings.SettingKey(ID + ".plotSize",
        "Size (in pixels) of the cells in the matrix", "");
    public static final int PLOT_SIZE = 100;

    public static final long serialVersionUID = -6890761195767034507L;

    public ScatterDefaults() {
      super(ID);

      m_defaults.put(POINT_SIZE_KEY, POINT_SIZE);
      m_defaults.put(PLOT_SIZE_KEY, PLOT_SIZE);
    }
  }
}
