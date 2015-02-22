package weka.gui.beans;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
@KFStep(category = "Spark",
  toolTipText = "Creates a randomly shuffled (and stratified) dataset")
public class RandomizedDataSparkJob extends AbstractSparkJob {

  public RandomizedDataSparkJob() {
    super();
    m_job = new weka.distributed.spark.RandomizedDataSparkJob();
    m_visual.setText("RandomlyShuffleDataSparkJob");
  }

  public String globalInfo() {
    return "Creates randomly shuffled (and stratified if a nominal class is set)"
      + " data chunks. As a sub-task, also computes quartiles for numeric"
      + " attributes and updates the summary attributes in the ARFF header";
  }

  @Override
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH
      + "RandomizedDataSparkJob.gif", BeanVisual.ICON_PATH
      + "RandomizedDataSparkJob.gif");
  }
}
