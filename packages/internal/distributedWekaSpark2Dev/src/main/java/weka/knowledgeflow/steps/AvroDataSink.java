package weka.knowledgeflow.steps;

import weka.gui.knowledgeflow.KFGUIConsts;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "AvroDataSink", category = "Spark",
  toolTipText = "Saves DataFrames to Avro files",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "DefaultDataSink.gif")
public class AvroDataSink extends AbstractDataSink {

  private static final long serialVersionUID = -8164996483889518713L;

  public AvroDataSink() {
    super();

    m_dataSink = new weka.distributed.spark.AvroDataSink();
  }
}
