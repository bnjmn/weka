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
 *    ModelPerformanceChartStepEditorDialog.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import weka.core.PluginManager;
import weka.gui.EnvironmentField;
import weka.gui.PropertySheetPanel;
import weka.gui.beans.OffscreenChartRenderer;
import weka.gui.beans.WekaOffscreenChartRenderer;
import weka.gui.knowledgeflow.GOEStepEditorDialog;
import weka.knowledgeflow.steps.ModelPerformanceChart;
import weka.knowledgeflow.steps.Step;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

/**
 * Step editor dialog for the ModelPerformanceChart step
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class ModelPerformanceChartStepEditorDialog extends GOEStepEditorDialog {

  private static final long serialVersionUID = -3031265139980301695L;

  /** For editing the renderer options */
  protected EnvironmentField m_rendererOptions = new EnvironmentField();

  /** For selecting the renderer to use */
  protected JComboBox<String> m_offscreenSelector = new JComboBox<String>();

  /** Currently selected renderer name */
  protected String m_currentRendererName;

  /** Current renderer options */
  protected String m_currentRendererOptions;

  /**
   * Set the step to edit. Also constructs the layout of the editor based on the
   * step's settings
   *
   * @param step the step to edit in this editor
   */
  @Override
  protected void setStepToEdit(Step step) {
    copyOriginal(step);
    createAboutPanel(step);
    m_editor = new PropertySheetPanel(false);
    m_editor.setUseEnvironmentPropertyEditors(true);
    m_editor.setEnvironment(m_env);
    m_editor.setTarget(m_stepToEdit);

    m_primaryEditorHolder.setLayout(new BorderLayout());
    m_primaryEditorHolder.add(m_editor, BorderLayout.CENTER);

    GridBagLayout gbLayout = new GridBagLayout();
    JPanel p = new JPanel(gbLayout);
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.anchor = GridBagConstraints.EAST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridy = 0;
    gbc.gridx = 0;
    gbc.insets = new Insets(0, 5, 0, 5);
    JLabel renderLabel = new JLabel("Renderer", SwingConstants.RIGHT);
    gbLayout.setConstraints(renderLabel, gbc);
    p.add(renderLabel);

    JPanel newPanel = new JPanel(new BorderLayout());
    gbc = new GridBagConstraints();
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridy = 0;
    gbc.gridx = 1;
    gbc.weightx = 100;
    newPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 0, 10));
    newPanel.add(m_offscreenSelector, BorderLayout.CENTER);
    gbLayout.setConstraints(newPanel, gbc);
    p.add(newPanel);

    gbc = new GridBagConstraints();
    gbc.anchor = GridBagConstraints.EAST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridy = 1;
    gbc.gridx = 0;
    gbc.insets = new Insets(0, 5, 0, 5);
    final JLabel rendererOptsLabel =
      new JLabel("Renderer options", SwingConstants.RIGHT);
    gbLayout.setConstraints(rendererOptsLabel, gbc);
    p.add(rendererOptsLabel);

    newPanel = new JPanel(new BorderLayout());
    gbc = new GridBagConstraints();
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridy = 1;
    gbc.gridx = 1;
    gbc.weightx = 100;
    newPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 0, 10));
    newPanel.add(m_rendererOptions, BorderLayout.CENTER);

    gbLayout.setConstraints(newPanel, gbc);
    p.add(newPanel);

    m_primaryEditorHolder.add(p, BorderLayout.NORTH);

    m_editorHolder.add(m_primaryEditorHolder, BorderLayout.NORTH);
    add(m_editorHolder, BorderLayout.CENTER);

    m_offscreenSelector.addItem("Weka Chart Renderer");
    Set<String> pluginRenderers =
      PluginManager
        .getPluginNamesOfType("weka.gui.beans.OffscreenChartRenderer");
    if (pluginRenderers != null) {
      for (String plugin : pluginRenderers) {
        m_offscreenSelector.addItem(plugin);
      }
    }

    m_offscreenSelector.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        setupRendererOptsTipText(rendererOptsLabel);
      }
    });

    getCurrentSettings();
    m_offscreenSelector.setSelectedItem(m_currentRendererName);
    m_rendererOptions.setText(m_currentRendererOptions);
    setupRendererOptsTipText(rendererOptsLabel);
  }

  /**
   * Get the name of the offscreen renderer and any options for it from the step
   * being edited.
   */
  protected void getCurrentSettings() {
    m_currentRendererName =
      ((ModelPerformanceChart) getStepToEdit()).getOffscreenRendererName();
    m_currentRendererOptions =
      ((ModelPerformanceChart) getStepToEdit()).getOffscreenAdditionalOpts();
  }

  private void setupRendererOptsTipText(JLabel optsLab) {
    String renderer = m_offscreenSelector.getSelectedItem().toString();
    if (renderer.equalsIgnoreCase("weka chart renderer")) {
      // built-in renderer
      WekaOffscreenChartRenderer rcr = new WekaOffscreenChartRenderer();
      String tipText = rcr.optionsTipTextHTML();
      tipText =
        tipText.replace("<html>", "<html>Comma separated list of options:<br>");
      optsLab.setToolTipText(tipText);
    } else {
      try {
        Object rendererO =
          PluginManager.getPluginInstance(
            "weka.gui.beans.OffscreenChartRenderer", renderer);

        if (rendererO != null) {
          String tipText =
            ((OffscreenChartRenderer) rendererO).optionsTipTextHTML();
          if (tipText != null && tipText.length() > 0) {
            optsLab.setToolTipText(tipText);
          }
        }
      } catch (Exception ex) {
        showErrorDialog(ex);
      }
    }
  }

  /**
   * Stuff to do when the user accepts the current settings
   */
  @Override
  protected void okPressed() {
    ((ModelPerformanceChart) getStepToEdit())
      .setOffscreenRendererName(m_offscreenSelector.getSelectedItem()
        .toString());
    ((ModelPerformanceChart) getStepToEdit())
      .setOffscreenAdditionalOpts(m_rendererOptions.getText());
  }
}
