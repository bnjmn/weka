package weka.gui.knowledgeflow;

import weka.core.Attribute;
import weka.core.Defaults;
import weka.core.Environment;
import weka.core.Instances;
import weka.core.Settings;
import weka.gui.AbstractPerspective;
import weka.gui.AttributeVisualizationPanel;
import weka.gui.PerspectiveInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@PerspectiveInfo(ID = AttributeSummaryPerspective.AttDefaults.ID,
  title = "Attribute summary", toolTipText = "Histogram summary charts",
  iconPath = "weka/gui/knowledgeflow/icons/chart_bar.png")
public class AttributeSummaryPerspective extends AbstractPerspective {

  protected Instances m_visualizeDataSet;
  protected transient List<AttributeVisualizationPanel> m_plots;

  /** Index on which to color the plots */
  protected int m_coloringIndex = -1;

  public AttributeSummaryPerspective() {
    setLayout(new BorderLayout());
  }

  protected void setup(Settings settings) {
    removeAll();
    if (m_visualizeDataSet == null) {
      return;
    }

    JScrollPane hp = makePanel(
      settings == null ? m_mainApplication.getApplicationSettings() : settings);
    add(hp, BorderLayout.CENTER);

    Vector<String> atts = new Vector<String>();
    for (int i = 0; i < m_visualizeDataSet.numAttributes(); i++) {
      atts
        .add("(" + Attribute.typeToStringShort(m_visualizeDataSet.attribute(i))
          + ") " + m_visualizeDataSet.attribute(i).name());
    }

    final JComboBox<String> classCombo = new JComboBox<String>();
    classCombo.setModel(new DefaultComboBoxModel<String>(atts));

    if (atts.size() > 0) {
      if (m_visualizeDataSet.classIndex() < 0) {
        classCombo.setSelectedIndex(atts.size() - 1);
      } else {
        classCombo.setSelectedIndex(m_visualizeDataSet.classIndex());
      }
      classCombo.setEnabled(true);
      for (int i = 0; i < m_plots.size(); i++) {
        m_plots.get(i).setColoringIndex(classCombo.getSelectedIndex());
      }
    }

    JPanel comboHolder = new JPanel();
    comboHolder.setLayout(new BorderLayout());
    JPanel tempHolder = new JPanel();
    tempHolder.setLayout(new BorderLayout());
    tempHolder.add(new JLabel("Class: "), BorderLayout.WEST);
    tempHolder.add(classCombo, BorderLayout.EAST);
    comboHolder.add(tempHolder, BorderLayout.WEST);
    add(comboHolder, BorderLayout.NORTH);

    classCombo.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int selected = classCombo.getSelectedIndex();
        if (selected >= 0) {
          for (int i = 0; i < m_plots.size(); i++) {
            m_plots.get(i).setColoringIndex(selected);
          }
        }
      }
    });
  }

  private JScrollPane makePanel(Settings settings) {
    String fontFamily = this.getFont().getFamily();
    Font newFont = new Font(fontFamily, Font.PLAIN, 10);
    JPanel hp = new JPanel();
    hp.setFont(newFont);
    int gridWidth =
      settings.getSetting(AttDefaults.ID, AttDefaults.GRID_WIDTH_KEY,
        AttDefaults.GRID_WIDTH, Environment.getSystemWide());
    int maxPlots =
      settings.getSetting(AttDefaults.ID, AttDefaults.MAX_PLOTS_KEY,
        AttDefaults.MAX_PLOTS, Environment.getSystemWide());
    int numPlots = Math.min(m_visualizeDataSet.numAttributes(), maxPlots);
    int gridHeight = numPlots / gridWidth;

    if (numPlots % gridWidth != 0) {
      gridHeight++;
    }
    hp.setLayout(new GridLayout(gridHeight, 4));

    m_plots = new ArrayList<AttributeVisualizationPanel>();

    for (int i = 0; i < numPlots; i++) {
      JPanel temp = new JPanel();
      temp.setLayout(new BorderLayout());
      temp.setBorder(BorderFactory
        .createTitledBorder(m_visualizeDataSet.attribute(i).name()));

      AttributeVisualizationPanel ap = new AttributeVisualizationPanel();
      m_plots.add(ap);
      ap.setInstances(m_visualizeDataSet);
      if (m_coloringIndex < 0 && m_visualizeDataSet.classIndex() >= 0) {
        ap.setColoringIndex(m_visualizeDataSet.classIndex());
      } else {
        ap.setColoringIndex(m_coloringIndex);
      }
      temp.add(ap, BorderLayout.CENTER);
      ap.setAttribute(i);
      hp.add(temp);
    }

    Dimension d = new Dimension(830, gridHeight * 100);
    hp.setMinimumSize(d);
    hp.setMaximumSize(d);
    hp.setPreferredSize(d);

    JScrollPane scroller = new JScrollPane(hp);

    return scroller;
  }

  @Override
  public Defaults getDefaultSettings() {
    return new AttDefaults();
  }

  @Override
  public void setInstances(Instances instances) {
    m_visualizeDataSet = instances;
    setup(null);
  }

  public void setInstances(Instances instances, Settings settings) {
    m_visualizeDataSet = instances;
    setup(settings);
  }

  @Override
  public boolean okToBeActive() {
    return m_visualizeDataSet != null;
  }

  @Override
  public boolean acceptsInstances() {
    return true;
  }

  public static class AttDefaults extends Defaults {

    public static final String ID = "attributesummary";

    protected static final Settings.SettingKey GRID_WIDTH_KEY =
      new Settings.SettingKey("weka.knowledgeflow.attributesummary.gridWidth",
        "Number of plots to display horizontally", "");

    protected static final int GRID_WIDTH = 4;

    protected static final Settings.SettingKey MAX_PLOTS_KEY =
      new Settings.SettingKey("weka.knowledgeflow.attributesummary.maxPlots",
        "Maximum number of plots to render", "");

    protected static final int MAX_PLOTS = 100;
    private static final long serialVersionUID = -32801466385262321L;

    public AttDefaults() {
      super(ID);

      m_defaults.put(GRID_WIDTH_KEY, GRID_WIDTH);
      m_defaults.put(MAX_PLOTS_KEY, MAX_PLOTS);
    }
  }
}
