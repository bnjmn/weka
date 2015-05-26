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
 *    HDFSSaverCustomizer
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
import weka.core.converters.AbstractFileSaver;
import weka.core.converters.HDFSSaver;
import weka.gui.GenericObjectEditor;
import weka.gui.PropertyPanel;
import weka.gui.beans.EnvironmentField;
import weka.gui.beans.GOECustomizer;

/**
 * Customizer GUI for the HDFSSaver Knowledge Flow component
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class HDFSSaverCustomizer extends JPanel implements GOECustomizer,
  EnvironmentHandler {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -6653590945775053697L;

  /** Something wanting to know if we've modified the object we're editing */
  protected ModifyListener m_modifyL;

  /** Environment variables */
  protected Environment m_env = Environment.getSystemWide();

  /** the saver being edited */
  protected HDFSSaver m_saver;

  /** True if we won't show ok and cancel buttons */
  protected boolean m_dontShowButs;

  /** Field for editing the host property */
  protected EnvironmentField m_hdfsHostText;

  /** Field for editing the port property */
  protected EnvironmentField m_hdfsPortText;

  /** Field for editing the path property */
  protected EnvironmentField m_hdfsPathText;

  /** Field for editing the prefix property */
  protected EnvironmentField m_filePrefixText;

  /** Field for editing the dfs replication property */
  protected EnvironmentField m_dfsReplicationText;

  /** Editor component for the base saver */
  protected GenericObjectEditor m_baseSaverEditor = new GenericObjectEditor();

  /** Property panel for the base saver */
  protected PropertyPanel m_sPanel = new PropertyPanel(m_baseSaverEditor);

  /**
   * 
   */
  public HDFSSaverCustomizer() {
    setLayout(new BorderLayout());
  }

  /**
   * Setup the layout of the customizer
   */
  protected void setup() {
    m_baseSaverEditor.setClassType(AbstractFileSaver.class);
    m_baseSaverEditor.setValue(m_saver.getSaver());

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
    m_hdfsHostText.setText(m_saver.getConfig().getHDFSHost());
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
    m_hdfsPortText.setText(m_saver.getConfig().getHDFSPort());
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
    m_hdfsPathText.setText(m_saver.getHDFSPath());
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.fill = GridBagConstraints.BOTH;
    gbC.gridy = 2;
    gbC.gridx = 1;
    gbC.weightx = 100;
    gl.setConstraints(m_hdfsPathText, gbC);
    fieldsPanel.add(m_hdfsPathText);

    JLabel dfsRepLab = new JLabel("DFS replication", SwingConstants.RIGHT);
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 3;
    gbC.gridx = 0;
    gl.setConstraints(dfsRepLab, gbC);
    fieldsPanel.add(dfsRepLab);

    m_dfsReplicationText = new EnvironmentField(m_env);
    m_dfsReplicationText.setText(m_saver.getDFSReplicationFactor());
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.fill = GridBagConstraints.BOTH;
    gbC.gridy = 3;
    gbC.gridx = 1;
    gbC.weightx = 100;
    gl.setConstraints(m_dfsReplicationText, gbC);
    fieldsPanel.add(m_dfsReplicationText);

    JLabel prefixLab = new JLabel("File prefix", SwingConstants.RIGHT);
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 4;
    gbC.gridx = 0;
    gl.setConstraints(prefixLab, gbC);
    fieldsPanel.add(prefixLab);

    m_filePrefixText = new EnvironmentField(m_env);
    m_filePrefixText.setText(m_saver.filePrefix());
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.WEST;
    gbC.fill = GridBagConstraints.BOTH;
    gbC.gridy = 4;
    gbC.gridx = 1;
    gbC.weightx = 100;
    gl.setConstraints(m_filePrefixText, gbC);
    fieldsPanel.add(m_filePrefixText);

    JLabel baseSaverLab = new JLabel("Saver", SwingConstants.RIGHT);
    gbC = new GridBagConstraints();
    gbC.anchor = GridBagConstraints.EAST;
    gbC.fill = GridBagConstraints.HORIZONTAL;
    gbC.gridy = 5;
    gbC.gridx = 0;
    gl.setConstraints(baseSaverLab, gbC);
    fieldsPanel.add(baseSaverLab);

    gbC.anchor = GridBagConstraints.WEST;
    gbC.fill = GridBagConstraints.BOTH;
    gbC.gridy = 5;
    gbC.gridx = 1;
    gbC.weightx = 100;
    gl.setConstraints(m_sPanel, gbC);
    fieldsPanel.add(m_sPanel);

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
    if (o instanceof HDFSSaver) {
      m_saver = (HDFSSaver) o;
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
    m_saver.getConfig().setHDFSHost(m_hdfsHostText.getText());
    m_saver.getConfig().setHDFSPort(m_hdfsPortText.getText());
    m_saver.setHDFSPath(m_hdfsPathText.getText());
    m_saver.setDFSReplicationFactor(m_dfsReplicationText.getText());
    m_saver.setSaver((AbstractFileSaver) m_baseSaverEditor.getValue());

    m_saver.setFilePrefix(m_filePrefixText.getText());
  }

  @Override
  public void closingCancel() {
    // nothing to do
  }
}
