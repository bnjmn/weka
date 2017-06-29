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
 *    SparkDataSinkStepEditorDialog
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import weka.core.Utils;
import weka.distributed.spark.DataSink;
import weka.gui.PropertySheetPanel;
import weka.gui.knowledgeflow.StepEditorDialog;
import weka.knowledgeflow.steps.AbstractDataSink;

/**
 * GUI step editor dialog for the Knowledge Flow Spark data sinks
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class SparkDataSinkStepEditorDialog extends StepEditorDialog {

  private static final long serialVersionUID = 6593707423223296113L;

  /** Underlying data sink */
  protected DataSink m_dataSink;

  /** For restoring original state */
  protected String m_optionsOrig;

  @Override
  public void layoutEditor() {
    m_dataSink = ((AbstractDataSink) getStepToEdit()).getUnderlyingDatasink();
    m_optionsOrig = ((AbstractDataSink) getStepToEdit()).getDatasinkOptions();

    String dataSinkTitle = getStepToEdit().getClass().getName();
    dataSinkTitle = dataSinkTitle.substring(dataSinkTitle.lastIndexOf(".") + 1);
    JPanel dsHolder = new JPanel(new BorderLayout());
    PropertySheetPanel dsEditor = new PropertySheetPanel();
    dsEditor.setTarget(m_dataSink);
    dsHolder.add(dsEditor, BorderLayout.NORTH);
    add(dsHolder, BorderLayout.CENTER);
  }

  @Override
  public void okPressed() {
    String[] baseOptions = m_dataSink.getBaseAndDSOptionsOnly();
    ((AbstractDataSink) getStepToEdit()).setDatasinkOptions(Utils
      .joinOptions(baseOptions));
  }

  @Override
  public void cancelPressed() {
    ((AbstractDataSink) getStepToEdit()).setDatasinkOptions(m_optionsOrig);
  }
}
