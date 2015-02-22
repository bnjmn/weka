package weka.gui.beans;

import java.util.ArrayList;
import java.util.List;

import weka.core.Instances;
import distributed.core.DistributedJobConfig;

/**
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
