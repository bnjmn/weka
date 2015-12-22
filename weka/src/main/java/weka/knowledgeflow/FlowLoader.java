package weka.knowledgeflow;

import weka.core.WekaException;
import weka.gui.Logger;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public interface FlowLoader {

  void setLog(Logger log);

  String getFlowFileExtension();

  String getFlowFileExtensionDescription();

  Flow readFlow(File flowFile) throws WekaException;

  Flow readFlow(InputStream is) throws WekaException;

  Flow readFlow(Reader r) throws WekaException;
}
