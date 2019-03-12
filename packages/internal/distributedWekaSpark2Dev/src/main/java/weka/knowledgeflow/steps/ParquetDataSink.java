package weka.knowledgeflow.steps;

import weka.gui.knowledgeflow.KFGUIConsts;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "ParquetDataSink", category = "Spark",
  toolTipText = "Saves DataFrames to Parquet files",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "DefaultDataSink.gif")
public class ParquetDataSink extends AbstractDataSink {

  private static final long serialVersionUID = -146644971239871206L;

  public ParquetDataSink() {
    super();

    m_dataSink = new weka.distributed.spark.ParquetDataSink();
  }
}
