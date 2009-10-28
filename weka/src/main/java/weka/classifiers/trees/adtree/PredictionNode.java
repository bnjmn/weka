/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    PredictionNode.java
 *    Copyright (C) 2001 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.trees.adtree;

import weka.classifiers.trees.ADTree;
import weka.core.FastVector;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;

import java.io.Serializable;
import java.util.Enumeration;

/**
 * Class representing a prediction node in an alternating tree.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 1.6.2.1 $
 */
public final class PredictionNode
  implements Serializable, Cloneable, RevisionHandler {

  /** for serialization */
  private static final long serialVersionUID = 6018958856358698814L;

  /** The prediction value stored in this node */
  private double value;

  /** The children of this node - any number of splitter nodes */
  private FastVector children;
  
  /**
   * Creates a new prediction node.
   *
   * @param newValue the value that the node should store
   */
  public PredictionNode(double newValue) {

    value = newValue;
    children = new FastVector();  
  }

  /**
   * Sets the prediction value of the node.
   *
   * @param newValue the value that the node should store
   */
  public final void setValue(double newValue) {

    value = newValue;
  }

  /**
   * Gets the prediction value of the node.
   *
   * @return the value stored in the node
   */
  public final double getValue() {

    return value;
  }

  /**
   * Gets the children of this node.
   *
   * @return a FastVector containing child Splitter object references
   */ 
  public final FastVector getChildren() {

    return children;
  }

  /**
   * Enumerates the children of this node.
   *
   * @return an enumeration of child Splitter object references
   */ 
  public final Enumeration children() {

    return children.elements();
  }
  
  /**
   * Adds a child to this node. If possible will merge, and will perform a deep copy
   * of the child tree.
   *
   * @param newChild the new child to add (will be cloned)
   * @param addingTo the tree that this node belongs to
   */
  public final void addChild(Splitter newChild, ADTree addingTo) {

    // search for an equivalent child
    Splitter oldEqual = null;
    for (Enumeration e = children(); e.hasMoreElements(); ) {
      Splitter split = (Splitter) e.nextElement();
      if (newChild.equalTo(split)) { oldEqual = split; break; }
    }
    if (oldEqual == null) { // didn't find one so just add
      Splitter addChild = (Splitter) newChild.clone();
      setOrderAddedSubtree(addChild, addingTo);
      children.addElement(addChild);
    }
    else { // found one, so do a merge
      for (int i=0; i<newChild.getNumOfBranches(); i++) {
	PredictionNode oldPred = oldEqual.getChildForBranch(i);
	PredictionNode newPred = newChild.getChildForBranch(i);
	if (oldPred != null && newPred != null)
	  oldPred.merge(newPred, addingTo);
      }
    }
  }

  /**
   * Clones this node. Performs a deep copy, recursing through the tree.
   *
   * @return a clone
   */ 
  public final Object clone() {

    PredictionNode clone = new PredictionNode(value);
    for (Enumeration e = children.elements(); e.hasMoreElements(); )
      clone.children.addElement((Splitter)((Splitter) e.nextElement()).clone());
    return clone;
  }

  /**
   * Merges this node with another.
   *
   * @param merger the node that is merging with this node - will not be affected,
   * will instead be cloned
   * @param mergingTo the tree that this node belongs to 
   */ 
  public final void merge(PredictionNode merger, ADTree mergingTo) {

    value += merger.value;
    for (Enumeration e = merger.children(); e.hasMoreElements(); ) {
      addChild((Splitter)e.nextElement(), mergingTo);
    }
  }

  /**
   * Sets the order added values of the subtree rooted at this splitter node.
   *
   * @param addChild the root of the subtree
   * @param addingTo the tree that this node will belong to
   */
  private final void setOrderAddedSubtree(Splitter addChild, ADTree addingTo) {

    addChild.orderAdded = addingTo.nextSplitAddedOrder();
    for (int i=0; i<addChild.getNumOfBranches(); i++) {
      PredictionNode node = addChild.getChildForBranch(i);
      if (node != null)
	for (Enumeration e = node.children(); e.hasMoreElements(); )
	  setOrderAddedSubtree((Splitter) e.nextElement(), addingTo);
    }
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 1.6.2.1 $");
  }
}
