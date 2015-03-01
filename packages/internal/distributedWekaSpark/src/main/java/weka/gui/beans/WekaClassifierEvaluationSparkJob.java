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
 *    WekaClassifierEvaluationSparkJob
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.util.ArrayList;
import java.util.List;

import weka.core.Instances;
import distributed.core.DistributedJobConfig;

/**
 * Knowledge Flow step for the classifier evaluation Spark job
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
@KFStep(category = "Spark",
  toolTipText = "Builds and evaluates an aggregated Weka classifier")
public class WekaClassifierEvaluationSparkJob extends AbstractSparkJob {

  /** For serialization */
  private static final long serialVersionUID = 1090562622978636120L;

  /** Downstream listeners for textual output */
  protected List<TextListener> m_textListeners = new ArrayList<TextListener>();

  /** Downstream listeners for data set output */
  protected List<DataSourceListener> m_dataSetListeners =
    new ArrayList<DataSourceListener>();

  public WekaClassifierEvaluationSparkJob() {
    super();
    m_job = new weka.distributed.spark.WekaClassifierEvaluationSparkJob();

    m_visual.setText("WekaClassifierEvaluationSparkJob");
  }

  @Override
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH
      + "WekaClassifierEvaluationSparkJob.gif", BeanVisual.ICON_PATH
      + "WekaClassifierEvaluationSparkJob.gif");
  }

  /**
   * Help information
   *
   * @return help information
   */
  public String globalInfo() {
    return "Builds and evaluates an aggregated classifier via cross-valdiation "
      + "in Spark.";
  }

  @Override
  protected void notifyJobOutputListeners() {
    String evalText =
      ((weka.distributed.spark.WekaClassifierEvaluationSparkJob) m_runningJob)
        .getText();

    if (!DistributedJobConfig.isEmpty(evalText)) {
      for (TextListener t : m_textListeners) {
        t.acceptText(new TextEvent(this, evalText, "Spark - evaluation result"));
      }
    } else {
      if (m_log != null) {
        m_log.logMessage(statusMessagePrefix()
          + "No evaluation results produced!");
      }
    }

    Instances evalInstances =
      ((weka.distributed.spark.WekaClassifierEvaluationSparkJob) m_runningJob)
        .getInstances();

    if (evalInstances != null) {
      for (DataSourceListener l : m_dataSetListeners) {
        l.acceptDataSet(new DataSetEvent(this, evalInstances));
      }
    } else {
      m_log.logMessage(statusMessagePrefix()
        + "No evaluation results produced!");
    }
  }

  /**
   * Add a text listener
   *
   * @param l a text listener
   */
  public synchronized void addTextListener(TextListener l) {
    m_textListeners.add(l);
  }

  /**
   * Remove a text listener
   *
   * @param l a text listener
   */
  public synchronized void removeTextListener(TextListener l) {
    m_textListeners.remove(l);
  }

  /**
   * Add a data source listener
   *
   * @param l a data source listener
   */
  public synchronized void addDataSourceListener(DataSourceListener l) {
    m_dataSetListeners.add(l);
  }

  /**
   * Remove a data source listener
   *
   * @param l a data source listener
   */
  public synchronized void removeDataSourceListener(DataSourceListener l) {
    m_dataSetListeners.remove(l);
  }
}
