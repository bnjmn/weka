/*
 *    NodePlace.java
 *    Copyright (C) 1999 Malcolm Ware
 *
 */

package weka.gui.treevisualizer;

/**
 * This is an interface for classes that wish to take a node structure and 
 * arrange them
 *
 * @author Malcolm F Ware (mfw4@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public interface NodePlace {
 
  /**
   * The function to call to postion the tree that starts at Node r
   *
   * @param r The top of the tree.
   */
   public void place(Node r);
  
} 
