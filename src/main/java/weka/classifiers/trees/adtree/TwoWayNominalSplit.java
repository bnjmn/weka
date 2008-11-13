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
 *    TwoWayNominalSplit.java
 *    Copyright (C) 2001 Richard Kirkby
 *
 */

package weka.classifiers.trees.adtree;

import weka.core.*;
import java.util.*;

/**
 * Class representing a two-way split on a nominal attribute, of the form:
 * either 'is some_value' or 'is not some_value'.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public class TwoWayNominalSplit extends Splitter {

  /** The index of the attribute the split depends on */
  private int attIndex;

  /** The attribute value that is compared against */
  private int trueSplitValue;

  /** The children of this split */
  private PredictionNode[] children;

  /**
   * Creates a new two-way nominal splitter.
   *
   * @param _attIndex the index of the attribute this split depeneds on
   * @param _trueSplitValue the attribute value that the splitter splits on
   */
  public TwoWayNominalSplit(int _attIndex, int _trueSplitValue) {

    attIndex = _attIndex; trueSplitValue = _trueSplitValue;
    children = new PredictionNode[2];
  }

  /**
   * Gets the number of branches of the split.
   *
   * @return the number of branches (always = 2)
   */
  public int getNumOfBranches() { 
  
    return 2;
  }

  /**
   * Gets the index of the branch that an instance applies to. Returns -1 if no branches
   * apply.
   *
   * @param i the instance
   * @return the branch index
   */
  public int branchInstanceGoesDown(Instance inst) {
    
    if (inst.isMissing(attIndex)) return -1;
    else if (inst.value(attIndex) == trueSplitValue) return 0;
    else return 1;
  }

  /**
   * Gets the subset of instances that apply to a particluar branch of the split. If the
   * branch index is -1, the subset will consist of those instances that don't apply to
   * any branch.
   *
   * @param branch the index of the branch
   * @param sourceInstances the instances from which to find the subset 
   * @return the set of instances that apply
   */
  public ReferenceInstances instancesDownBranch(int branch, Instances instances) {
    
    ReferenceInstances filteredInstances = new ReferenceInstances(instances, 1);
    if (branch == -1) {
      for (Enumeration e = instances.enumerateInstances(); e.hasMoreElements(); ) {
	Instance inst = (Instance) e.nextElement();
	if (inst.isMissing(attIndex)) filteredInstances.addReference(inst);
      }
    } else if (branch == 0) {
      for (Enumeration e = instances.enumerateInstances(); e.hasMoreElements(); ) {
	Instance inst = (Instance) e.nextElement();
	if (!inst.isMissing(attIndex) && inst.value(attIndex) == trueSplitValue)
	  filteredInstances.addReference(inst);
      }
    } else {
      for (Enumeration e = instances.enumerateInstances(); e.hasMoreElements(); ) {
	Instance inst = (Instance) e.nextElement();
	if (!inst.isMissing(attIndex) && inst.value(attIndex) != trueSplitValue)
	  filteredInstances.addReference(inst);
      }
    }
    return filteredInstances;
  }

  /**
   * Gets the string describing the attributes the split depends on.
   * i.e. the left hand side of the description of the split.
   *
   * @param dataset the dataset that the split is based on
   * @return a string describing the attributes
   */  
  public String attributeString(Instances dataset) {
    
    return dataset.attribute(attIndex).name();
  }

  /**
   * Gets the string describing the comparision the split depends on for a particular
   * branch. i.e. the right hand side of the description of the split.
   *
   * @param branchNum the branch of the split
   * @param dataset the dataset that the split is based on
   * @return a string describing the comparison
   */
  public String comparisonString(int branchNum, Instances dataset) {

    Attribute att = dataset.attribute(attIndex);
    if (att.numValues() != 2) 
      return ((branchNum == 0 ? "= " : "!= ") + att.value(trueSplitValue));
    else return ("= " + (branchNum == 0 ?
			 att.value(trueSplitValue) :
			 att.value(trueSplitValue == 0 ? 1 : 0)));
  }

  /**
   * Tests whether two splitters are equivalent.
   *
   * @param compare the splitter to compare with
   * @return whether or not they match
   */
  public boolean equalTo(Splitter compare) {

    if (compare instanceof TwoWayNominalSplit) { // test object type
      TwoWayNominalSplit compareSame = (TwoWayNominalSplit) compare;
      return (attIndex == compareSame.attIndex &&
	      trueSplitValue == compareSame.trueSplitValue);
    } else return false;
  }

  /**
   * Sets the child for a branch of the split.
   *
   * @param branchNum the branch to set the child for
   * @param childPredictor the new child
   */
  public void setChildForBranch(int branchNum, PredictionNode childPredictor) {

    children[branchNum] = childPredictor;
  }

  /**
   * Gets the child for a branch of the split.
   *
   * @param branchNum the branch to get the child for
   * @return the child
   */
  public PredictionNode getChildForBranch(int branchNum) {

    return children[branchNum];
  }

  /**
   * Clones this node. Performs a deep copy, recursing through the tree.
   *
   * @return a clone
   */
  public Object clone() {

    TwoWayNominalSplit clone = new TwoWayNominalSplit(attIndex, trueSplitValue);
    clone.orderAdded = orderAdded;
    if (children[0] != null)
      clone.setChildForBranch(0, (PredictionNode) children[0].clone());
    if (children[1] != null)
      clone.setChildForBranch(1, (PredictionNode) children[1].clone());
    return clone;
  }
}

