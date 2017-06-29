package weka.knowledgeflow.steps;

import weka.gui.knowledgeflow.KFGUIConsts;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "CSVDataSink", category = "Spark",
  toolTipText = "Saves DataFrames to CSV files",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "CSVSaver.gif")
public class CSVDataSink extends AbstractDataSink {

  private static final long serialVersionUID = 4628108752392846354L;

  public CSVDataSink() {
    super();

    m_dataSink = new weka.distributed.spark.CSVDataSink();
  }
}
