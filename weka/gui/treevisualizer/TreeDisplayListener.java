/*
 *    TreeDisplayListener.java
 *    Copyright (C) 1999 Malcolm Ware
 *
 */

package weka.gui.treevisualizer;

//this is simply used to get some user changes from the displayer to an actual
//class
//that contains the actual structure of the data the displayer is displaying

/**
 * Interface implemented by classes that wish to recieve user selection events
 * from a tree displayer.
 *
 * @author Malcolm Ware (mfw4@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public interface TreeDisplayListener {

  /**
   * Gets called when the user selects something, in the tree display.
   * @param e Contains what the user selected with what it was selected for.
   */
  public void userCommand(TreeDisplayEvent e);
}




