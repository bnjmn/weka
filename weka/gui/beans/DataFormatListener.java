package weka.gui.beans;

/**
 * Listener interface that customizer classes that are interested
 * in data format changes can implement.
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.1.2.1 $
 */
public interface DataFormatListener {
  
  /**
   * Recieve a DataSetEvent that encapsulates a new data format. The
   * DataSetEvent may contain null for the encapsulated format. This indicates
   * that there is no data format available (ie. user may have disconnected
   * an input source of data in the KnowledgeFlow).
   *
   * @param e a <code>DataSetEvent</code> value
   */
  void newDataFormat(DataSetEvent e);
}
