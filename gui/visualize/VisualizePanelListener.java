/*
 *    VisualizePanelListener.java
 *    Copyright (C) 1999 Malcolm Ware
 *
 */


package weka.gui.visualize;

/**
 * Interface implemented by a class that is interested in receiving
 * submited shapes from a visualize panel.
 * @author Malcolm Ware (mfw4@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public interface VisualizePanelListener {

  /**
   * This method receives an object containing the shapes, instances
   * inside and outside these shapes and the attributes these shapes were
   * created in.
   * @param e The Event containing the data.
   */
  public void userDataEvent(VisualizePanelEvent e);


}
