/*
 *    AttributePanelListener.java
 *    Copyright (C) 2000 Mark Hall
 *
 */


package weka.gui.visualize;

/**
 * Interface for classes that want to listen for Attribute selection
 * changes in the attribute panel
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public interface AttributePanelListener {

  /**
   * Called when the user clicks on an attribute bar
   * @param e the event encapsulating what happened
   */
  public void attributeSelectionChange(AttributePanelEvent e);

}
