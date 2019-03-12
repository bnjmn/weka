package weka.knowledgeflow.steps;

import weka.gui.knowledgeflow.KFGUIConsts;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "WekaScoringSparkStep", category = "Spark",
  toolTipText = "Builds and an aggregated Weka classifier ",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "WekaClassifierSparkJob.gif")
public class WekaScoringSparkStep extends AbstractSparkJob {

  private static final long serialVersionUID = -5385682308118345256L;

  /**
   * Constructor
   */
  public WekaScoringSparkStep() {
    super();
    m_job = new weka.distributed.spark.WekaScoringSparkJob();
  }
}
