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
 *    AttributeSummaryPerspective.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

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
 * Knowledge Flow perspective that provides a matrix of
 * AttributeVisualizationPanels
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@PerspectiveInfo(ID = AttributeSummaryPerspective.AttDefaults.ID,
  title = "Attribute summary", toolTipText = "Histogram summary charts",
  iconPath = "weka/gui/knowledgeflow/icons/chart_bar.png")
public class AttributeSummaryPerspective extends AbstractPerspective {

  private static final long serialVersionUID = 6697308901346612850L;

  /** The dataset being visualized */
  protected Instances m_visualizeDataSet;

  /** Holds the grid of attribute histogram panels */
  protected transient List<AttributeVisualizationPanel> m_plots;

  /** Index on which to color the plots */
  protected int m_coloringIndex = -1;

  /**
   * Constructor
   */
  public AttributeSummaryPerspective() {
    setLayout(new BorderLayout());
  }

  /**
   * Setup the panel using the supplied settings
   *
   * @param settings the settings to use
   */
  protected void setup(Settings settings) {
    removeAll();
    if (m_visualizeDataSet == null) {
      return;
    }

    JScrollPane hp =
      makePanel(settings == null ? m_mainApplication.getApplicationSettings()
        : settings);
    add(hp, BorderLayout.CENTER);

    Vector<String> atts = new Vector<String>();
    for (int i = 0; i < m_visualizeDataSet.numAttributes(); i++) {
      atts.add("("
        + Attribute.typeToStringShort(m_visualizeDataSet.attribute(i)) + ") "
        + m_visualizeDataSet.attribute(i).name());
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

  /**
   * Makes the scrollable panel containing the grid of attribute
   * visualizations
   *
   * @param settings the settings to use
   * @return the configured panel
   */
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
      temp.setBorder(BorderFactory.createTitledBorder(m_visualizeDataSet
        .attribute(i).name()));

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

  /**
   * Get the default settings for this perspective
   *
   * @return the default settings for this perspective
   */
  @Override
  public Defaults getDefaultSettings() {
    return new AttDefaults();
  }

  /**
   * Set the instances to visualize
   *
   * @param instances the instances the instances to visualize
   */
  @Override
  public void setInstances(Instances instances) {
    m_visualizeDataSet = instances;
    setup(null);
  }

  /**
   * Set the instances to visualize
   *
   * @param instances the instances to visualize
   * @param settings the settings to use
   */
  public void setInstances(Instances instances, Settings settings) {
    m_visualizeDataSet = instances;
    setup(settings);
  }

  /**
   * Returns true if this perspective is OK with being an active perspective -
   * i.e. the user can click on this perspective at this time in the perspective
   * toolbar. For example, a Perspective might return false from this method if
   * it needs a set of instances to operate but none have been supplied yet.
   *
   * @return true if this perspective can be active at the current time
   */
  @Override
  public boolean okToBeActive() {
    return m_visualizeDataSet != null;
  }

  /**
   * Returns true, as this perspective does accept instances
   *
   * @return true
   */
  @Override
  public boolean acceptsInstances() {
    return true;
  }

  /**
   * Default settings for the AttributeSummaryPerspective
   */
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
