/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    Splitter.java
 *    Copyright (C) 2001 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.trees.adtree;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.RevisionHandler;

import java.io.Serializable;

/**
 * Abstract class representing a splitter node in an alternating tree.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision$
 */
public abstract class Splitter
  implements Serializable, Cloneable, RevisionHandler {

  /** for serialization */
  private static final long serialVersionUID = 8190449848490055L;

  /** The number this node was in the order of nodes added to the tree */
  public int orderAdded;

  /**
   * Gets the number of branches of the split.
   *
   * @return the number of branches
   */
  public abstract int getNumOfBranches();

  /**
   * Gets the index of the branch that an instance applies to. Returns -1 if no branches
   * apply.
   *
   * @param i the instance
   * @return the branch index
   */
  public abstract int branchInstanceGoesDown(Instance i);

  /**
   * Gets the subset of instances that apply to a particluar branch of the split. If the
   * branch index is -1, the subset will consist of those instances that don't apply to
   * any branch.
   *
   * @param branch the index of the branch
   * @param sourceInstances the instances from which to find the subset 
   * @return the set of instances that apply
   */
  public abstract ReferenceInstances instancesDownBranch(int branch, Instances sourceInstances);

  /**
   * Gets the string describing the attributes the split depends on.
   * i.e. the left hand side of the description of the split.
   *
   * @param dataset the dataset that the split is based on
   * @return a string describing the attributes
   */
  public abstract String attributeString(Instances dataset);

  /**
   * Gets the string describing the comparision the split depends on for a particular
   * branch. i.e. the right hand side of the description of the split.
   *
   * @param branchNum the branch of the split
   * @param dataset the dataset that the split is based on
   * @return a string describing the comparison
   */
  public abstract String comparisonString(int branchNum, Instances dataset);

  /**
   * Tests whether two splitters are equivalent.
   *
   * @param compare the splitter to compare with
   * @return whether or not they match
   */
  public abstract boolean equalTo(Splitter compare);

  /**
   * Sets the child for a branch of the split.
   *
   * @param branchNum the branch to set the child for
   * @param childPredictor the new child
   */
  public abstract void setChildForBranch(int branchNum, PredictionNode childPredictor);

  /**
   * Gets the child for a branch of the split.
   *
   * @param branchNum the branch to get the child for
   * @return the child
   */
  public abstract PredictionNode getChildForBranch(int branchNum);

  /**
   * Clones this node. Performs a deep copy, recursing through the tree.
   *
   * @return a clone
   */
  public abstract Object clone();
}
