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
 *    TimeSeriesPerspective.java
 *    Copyright (C) 2010-2016 University of Waikato, Hamilton, New Zealand
 */

package weka.gui.knowledgeflow;

import weka.classifiers.timeseries.WekaForecaster;
import weka.classifiers.timeseries.gui.ForecastingPanel;
import weka.core.Defaults;
import weka.core.Environment;
import weka.core.Instances;
import weka.core.Settings;
import weka.gui.AbstractPerspective;
import weka.gui.Logger;
import weka.gui.PerspectiveInfo;
import weka.gui.WorkbenchDefaults;
import weka.knowledgeflow.KFDefaults;
import weka.knowledgeflow.StepManagerImpl;
import weka.knowledgeflow.steps.TimeSeriesForecasting;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Knowledge Flow Perspective for the time series forecasting environment
 *
 * @author Mark Hall
 * @version $Revision: $
 */
@PerspectiveInfo(ID = TimeSeriesPerspective.TimeSeriesDefaults.ID,
  title = "Time series forecasting",
  toolTipText = "Time series forecasting environment",
  iconPath = "weka/gui/knowledgeflow/icons/chart_line.png")
public class TimeSeriesPerspective extends AbstractPerspective {

  private static final long serialVersionUID = 9120813916333393028L;

  /** The forecasting panel to wrap */
  protected ForecastingPanel m_forecastingPanel;

  /** The current dataset */
  protected Instances m_dataSet;

  public TimeSeriesPerspective() {
    setLayout(new BorderLayout());
    m_forecastingPanel = new ForecastingPanel(null, false, false, false);
    m_forecastingPanel
      .setTimeSeriesModelListener(new TimeSeriesModelListener() {
        @Override
        public void acceptForecaster(WekaForecaster forecaster,
          Instances trainingStruct) {
          setForecasterInKFPasteBuffer(forecaster, trainingStruct);
        }
      });
    add(m_forecastingPanel, BorderLayout.CENTER);
  }

  /**
   * Requires a log when running in the Workbench application
   *
   * @return true if running in the Workbench application
   */
  @Override
  public boolean requiresLog() {
    return getMainApplication().getApplicationID()
      .equals(WorkbenchDefaults.APP_ID);
  }

  @Override
  public void setLog(Logger newLog) {
    m_forecastingPanel.setLog(newLog);
  }

  protected void setForecasterInKFPasteBuffer(WekaForecaster forecaster,
    Instances structureToSave) {
    if (getMainApplication().getMainPerspective().getMainApplication()
      .getApplicationID().equals(KFDefaults.APP_ID)) {
      try {
        String encoded = TimeSeriesForecasting
          .encodeForecasterToBase64(forecaster, structureToSave);

        TimeSeriesForecasting step = new TimeSeriesForecasting();
        step.setEncodedForecaster(encoded);
        StepManagerImpl manager = new StepManagerImpl(step);
        StepVisual visualStep = StepVisual.createVisual(manager);
        List<StepVisual> steps = new ArrayList<StepVisual>();
        steps.add(visualStep);

        ((MainKFPerspective) getMainApplication().getMainPerspective())
          .copyStepsToClipboard(steps);

        if (getMainApplication().getApplicationSettings().getSetting(
          TimeSeriesDefaults.ID, TimeSeriesDefaults.SHOW_CLIPBOARD_POPUP_KEY,
          TimeSeriesDefaults.SHOW_CLIPBOARD_POPUP,
          Environment.getSystemWide())) {
          getMainApplication().showInfoDialog(
            "Configured forecasting "
              + "step has been transferred to the clipboard",
            "Time series", false);
        }
      } catch (Exception ex) {
        getMainApplication().showErrorDialog(ex);
      }
    }
  }

  /**
   * Returns true, as this panel sends instances into the python environment
   *
   * @return true
   */
  @Override
  public boolean acceptsInstances() {
    return true;
  }

  @Override
  public boolean okToBeActive() {
    return m_dataSet != null;
  }

  @Override
  public void setInstances(Instances insts) {
    try {
      m_dataSet = insts;
      m_forecastingPanel.setInstances(m_dataSet);
    } catch (Exception ex) {
      getMainApplication().showErrorDialog(ex);
    }
  }

  @Override
  public Defaults getDefaultSettings() {
    return new TimeSeriesDefaults();
  }

  public interface TimeSeriesModelListener {
      void acceptForecaster(WekaForecaster forecaster,
        Instances trainingStruct);
  }

  public static class TimeSeriesDefaults extends Defaults {
    private static final long serialVersionUID = 912598893182636566L;

    public static final String ID = "weka.gui.knowledgeflow.timeseries";

    public static final Settings.SettingKey SHOW_CLIPBOARD_POPUP_KEY =
      new Settings.SettingKey(ID + ".showCopyPopup",
        "Show clipboard copy popup",
        "Whether to show "
          + "the popup dialog when copying a forecaster to the Knowledge Flow "
          + "clipboard");

    public static final boolean SHOW_CLIPBOARD_POPUP = true;

    public TimeSeriesDefaults() {
      super(ID);

      m_defaults.put(SHOW_CLIPBOARD_POPUP_KEY, SHOW_CLIPBOARD_POPUP);
    }
  }
}
