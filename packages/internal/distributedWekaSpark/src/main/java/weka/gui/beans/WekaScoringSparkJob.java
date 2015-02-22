package weka.gui.beans;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
@KFStep(category = "Spark", toolTipText = "Scores data using a Weka model")
public class WekaScoringSparkJob extends AbstractSparkJob {

  public WekaScoringSparkJob() {
    super();
    m_job = new weka.distributed.spark.WekaScoringSparkJob();
    m_visual.setText("WekaScoringSparkJob");
  }

  /**
   * Help information
   *
   * @return help information
   */
  public String globalInfo() {
    return "Scores data using a Weka model. Handles Weka classifiers and "
      + "clusterers. User can opt to output all or some of the "
      + "original input attributes in the scored data.";
  }

  @Override
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH + "WekaClassifierSparkJob.gif",
      BeanVisual.ICON_PATH + "WekaClassifierSparkJob.gif");
  }
}
