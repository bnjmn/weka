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
 *    HDFSLoaderCustomizer
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.converters;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.converters.AbstractFileLoader;
import weka.core.converters.HDFSLoader;
import weka.gui.GenericObjectEditor;
import weka.gui.PropertyPanel;
import weka.gui.beans.EnvironmentField;
import weka.gui.beans.GOECustomizer;

/**
 * Customizer GUI for the HDFSLoader Knowledge Flow component
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * 
 */
public class HDFSLoaderCustomizer extends JPanel implements GOECustomizer,
  EnvironmentHandler {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 1643079004729849260L;

  /** Something wanting to know if we've modified the object we're editing */
  protected ModifyListener m_modifyL;

  /** Environment variables */
  protected Environment m_env = Environment.getSystemWide();

  /** The loader being edited */
  protected HDFSLoader m_loader;

  /** True if we won't show ok and cancel buttons */
  protected boolean m_dontShowButs;

  /** Field for editing the host property */
  protected EnvironmentField m_hdfsHostText;

  /** Field for editing the port property */
  protected EnvironmentField m_hdfsPortText;

  /** Field for editing the path property */
  protected EnvironmentField m_hdfsPathText;

  /** Editor component for the base loader */
  protected GenericObjectEditor m_baseLoaderEditor = new GenericObjectEditor();

  /** Property panel for the base loader */
  protected PropertyPanel m_lPanel = new PropertyPanel(m_baseLoaderEditor);

  /**
   * Constructor
   */
  public HDFSLoaderCustomizer() {
    setLayout(new BorderLayout());
  }

  /**
   * Setup the layout of the customizer
   */
  protected void setup() {
    m_baseLoaderEditor.setClassType(AbstractFileLoader.class);
    m_baseLoaderEditor.setValue(m_loader.getLoader());

    JPanel fieldsPanel = new JPanel();
    GridBagLayout gl = new GridBagLayout();
    fieldsPanel.setLayout(gl);

    JLabel hostLab = new JLabel("HDFS host", SwingConstants.RIGHT);
    GridBagConstraints gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 0;
    gbC.gridx = 0;
    gl.setConstraints(hostLab, gbC);
    fieldsPanel.add(hostLab);

    m_hdfsHostText = new EnvironmentField(m_env);
    m_hdfsHostText.setText(m_loader.getConfig().getHDFSHost());
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.fill = GridBagConstraints.BOTH;
    gbC.gridy = 0;
    gbC.gridx = 1;
    gbC.weightx = 100;
    gl.setConstraints(m_hdfsHostText, gbC);

    fieldsPanel.add(m_hdfsHostText);

    JLabel portLab = new JLabel("HDFS port", SwingConstants.RIGHT);
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 1;
    gbC.gridx = 0;
    gl.setConstraints(portLab, gbC);
    fieldsPanel.add(portLab);

    m_hdfsPortText = new EnvironmentField(m_env);
    m_hdfsPortText.setText(m_loader.getConfig().getHDFSPort());
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.fill = GridBagConstraints.BOTH;
    gbC.gridy = 1;
    gbC.gridx = 1;
    gbC.weightx = 100;
    gl.setConstraints(m_hdfsPortText, gbC);
    fieldsPanel.add(m_hdfsPortText);

    JLabel pathLab = new JLabel("HDFS file path", SwingConstants.RIGHT);
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 2;
    gbC.gridx = 0;
    gl.setConstraints(pathLab, gbC);
    fieldsPanel.add(pathLab);

    m_hdfsPathText = new EnvironmentField(m_env);
    m_hdfsPathText.setText(m_loader.getHDFSPath());
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.fill = GridBagConstraints.BOTH;
    gbC.gridy = 2;
    gbC.gridx = 1;
    gbC.weightx = 100;
    gl.setConstraints(m_hdfsPathText, gbC);
    fieldsPanel.add(m_hdfsPathText);

    JLabel baseLoaderLab = new JLabel("Loader", SwingConstants.RIGHT);
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 3;
    gbC.gridx = 0;
    gl.setConstraints(baseLoaderLab, gbC);
    fieldsPanel.add(baseLoaderLab);

    gbC.anchor = GridBagConstraints.WEST;
    gbC.fill = GridBagConstraints.BOTH;
    gbC.gridy = 3;
    gbC.gridx = 1;
    gbC.weightx = 100;
    gl.setConstraints(m_lPanel, gbC);
    fieldsPanel.add(m_lPanel);

    add(fieldsPanel, BorderLayout.CENTER);

    if (m_dontShowButs) {
      return;
    }

    addButtons();
  }

  /**
   * Add OK and Cancel buttons
   */
  private void addButtons() {
    JButton okBut = new JButton("OK");
    JButton cancelBut = new JButton("Cancel");

    JPanel butHolder = new JPanel();
    butHolder.setLayout(new GridLayout(1, 2));
    butHolder.add(okBut);
    butHolder.add(cancelBut);
    add(butHolder, BorderLayout.SOUTH);

    okBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        closingOK();
      }
    });

    cancelBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        closingCancel();
      }
    });
  }

  @Override
  public void setModifiedListener(ModifyListener l) {
    m_modifyL = l;
  }

  @Override
  public void setObject(Object o) {
    if (o instanceof HDFSLoader) {
      m_loader = (HDFSLoader) o;
    }

    setup();
  }

  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
  }

  @Override
  public void dontShowOKCancelButtons() {
    m_dontShowButs = true;
  }

  @Override
  public void closingOK() {
    m_loader.getConfig().setHDFSHost(m_hdfsHostText.getText());
    m_loader.getConfig().setHDFSPort(m_hdfsPortText.getText());
    m_loader.setHDFSPath(m_hdfsPathText.getText());
    m_loader.setLoader((AbstractFileLoader) m_baseLoaderEditor.getValue());
  }

  @Override
  public void closingCancel() {
    // nothing to do
  }
}
