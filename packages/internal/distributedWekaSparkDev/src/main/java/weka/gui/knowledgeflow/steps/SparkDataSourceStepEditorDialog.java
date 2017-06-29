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
 *    SparkDataSourceStepEditorDialog
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import distributed.core.DistributedJob;
import distributed.spark.SparkJobConfig;
import weka.core.Utils;
import weka.distributed.spark.DataSource;
import weka.gui.PropertySheetPanel;
import weka.gui.knowledgeflow.StepEditorDialog;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.steps.AbstractDataSource;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * GUI step editor dialog for the Knowledge Flow Spark data sources
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class SparkDataSourceStepEditorDialog extends StepEditorDialog {

  private static final long serialVersionUID = 543882550996837619L;

  /** Underlying data source */
  protected DataSource m_dataSource;

  /**
   * Main config for the underlying Spark job. Settings here may be ignored if
   * this job receives a SparkContext from an upstream step
   */
  protected SparkJobConfig m_sjConfig;

  /** Editor for the main config */
  protected PropertySheetPanel m_sjConfigEditor = new PropertySheetPanel();

  /** Configuration property panel */
  protected SparkJobStepEditorDialog.SparkPropertyPanel m_propPanel;

  /** Tabs of the dialog */
  protected JTabbedPane m_configTabs = new JTabbedPane();

  /** For restoring original state */
  protected String m_optionsOrig;

  @Override
  public void layoutEditor() {
    m_dataSource =
      ((AbstractDataSource) getStepToEdit()).getUnderlyingDatasource();
    m_optionsOrig =
      ((AbstractDataSource) getStepToEdit()).getDatasourceOptions();
    m_sjConfig = m_dataSource.getSparkJobConfig();
    m_sjConfigEditor.setEnvironment(getEnvironment());
    m_sjConfigEditor.setTarget(m_sjConfig);

    // Only allow spark connection options if there is no
    // upstream step (i.e. this one is acting as a start point)
    StepManager manager = getStepToEdit().getStepManager();
    if (manager.numIncomingConnections() == 0) {
      JPanel configHolder = new JPanel(new BorderLayout());
      configHolder.add(m_sjConfigEditor, BorderLayout.NORTH);
      m_propPanel =
        new SparkJobStepEditorDialog.SparkPropertyPanel(
          m_sjConfig.getUserSuppliedProperties());
      configHolder.add(m_propPanel, BorderLayout.SOUTH);

      JPanel outerP = new JPanel();
      outerP.setLayout(new BorderLayout());
      outerP.add(configHolder, BorderLayout.NORTH);

      m_configTabs.addTab("Spark configuration", outerP);
    }

    String dataSourceTitle = getStepToEdit().getClass().getName();
    dataSourceTitle =
      dataSourceTitle.substring(dataSourceTitle.lastIndexOf(".") + 1);

    JPanel dsHolder = new JPanel(new BorderLayout());
    PropertySheetPanel dsEditor = new PropertySheetPanel();
    dsEditor.setEnvironment(m_env);
    dsEditor.setTarget(m_dataSource);
    dsHolder.add(dsEditor, BorderLayout.NORTH);

    if (manager.numIncomingConnections() == 0) {
      m_configTabs.addTab(dataSourceTitle, dsHolder);

      add(m_configTabs, BorderLayout.CENTER);
    } else {
      add(dsHolder, BorderLayout.CENTER);
    }
  }

  @Override
  public void okPressed() {
    if (getStepToEdit().getStepManager().numIncomingConnections() == 0) {
      m_sjConfig.clearUserSuppliedProperties();
      Map<String, String> userProps = m_propPanel.getProperties();
      for (Map.Entry<String, String> e : userProps.entrySet()) {
        // skip this one! As we'll get it via the base job stuff below
        if (e.getKey() != null
          && !e.getKey().equals(DistributedJob.WEKA_ADDITIONAL_PACKAGES_KEY)) {
          m_sjConfig.setUserSuppliedProperty(e.getKey(), e.getValue());
        }
      }
    }

    String[] baseJobOpts = m_dataSource.getBaseDSOptionsOnly();
    String[] mrConfigOpts = m_sjConfig.getOptions();

    List<String> opts = new ArrayList<String>();
    opts.addAll(Arrays.asList(baseJobOpts));
    opts.addAll(Arrays.asList(mrConfigOpts));
    String combined = Utils.joinOptions(opts.toArray(new String[opts.size()]));
    System.err.println("Combined: " + combined);
    ((AbstractDataSource) getStepToEdit()).setDatasourceOptions(combined);
  }

  @Override
  public void cancelPressed() {
    ((AbstractDataSource) getStepToEdit()).setDatasourceOptions(m_optionsOrig);
  }
}
