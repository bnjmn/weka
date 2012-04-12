/*
 *    PruneableClassifierTree.java
 *    Copyright (C) 1999 Eibe Frank
 *
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

package weka.classifiers.j48;

import weka.core.*;
import java.util.*;

/**
 * Class for handling a tree structure that can
 * be pruned using a pruning set. 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */

public class PruneableClassifierTree extends ClassifierTree{
    
  // =================
  // Private variables
  // =================

  /**
   * True if the tree is to be pruned.
   */

  private boolean pruneTheTree = false;

  /**
   * How many subsets of equal size?
   * One used for pruning, the rest for training
   */

  private int numSets = 3;

  // ===============
  // Public methods.
  // ===============

  /**
   * Constructor for pruneable tree structure. Stores reference
   * to associated training data at each node.
   * @param toSelectLocModel selection method for local splitting model
   * @param pruneTree true if the tree is to be pruned
   * @param num number of subsets of equal size
   * @exception Exception if something goes wrong
   */

  public PruneableClassifierTree(ModelSelection toSelectLocModel,
				 boolean pruneTree, int num)
       throws Exception{

    super(toSelectLocModel);

    pruneTheTree = pruneTree;
    numSets = num;
  }

  /**
   * Method for building a pruneable classifier tree.
   * @exception Exception if tree can't be built successfully
   */

  public void buildClassifier(Instances data) 
       throws Exception{

   if (data.classAttribute().isNumeric())
      throw new Exception("Class is numeric!");
   
   data = new Instances(data);
   data.deleteWithMissingClass();
   data.stratify(numSets);
   buildTree(data.trainCV(numSets, numSets - 1),
	     data.testCV(numSets, numSets - 1), false);
   if (pruneTheTree)
     prune();
   cleanup(new Instances(data, 0));
  }

  /**
   * Prunes a tree.
   * @exception Exception if tree can't be pruned successfully
   */

  public void prune() throws Exception {
  
    if (!isLeaF){
      
      // Prune all subtrees.
      
      for (int i = 0; i < sonS.length; i++)
	son(i).prune();
      
      // Decide if leaf is best choice.
      
      if (Utils.smOrEq(errorsForLeaf(),errorsForTree())) {
	
	// Free son Trees
	
	sonS = null;
	isLeaF = true;
	
	// Get NoSplit Model for node.
	
	localModeL = new NoSplit(localModel().distribution());
      }
    }
  }

  // ==================
  // Protected methods.
  // ==================

  /**
   * Returns a newly created tree.
   * @param data and selection method for local models.
   */

  protected ClassifierTree getNewTree(Instances data) throws Exception{
    
    PruneableClassifierTree newTree = 
      new PruneableClassifierTree(toSelectModeL,pruneTheTree,numSets);
    newTree.buildTree((Instances)data, false);

    return newTree;
  }

  /**
   * Returns a newly created tree.
   * @param data and selection method for local models.
   */

  protected ClassifierTree getNewTree(Instances train, Instances test) 
       throws Exception{

    PruneableClassifierTree newTree = 
      new PruneableClassifierTree(toSelectModeL,pruneTheTree,numSets);
    newTree.buildTree(train, test, false);
    return newTree;
  }

  // ================
  // Private methods.
  // ================

  /**
   * Computes estimated errors for tree.
   * @exception Exception if error estimate can't be computed
   */

  private double errorsForTree() throws Exception {

    double errors = 0;

    if (isLeaF)
      return errorsForLeaf();
    else{
      for (int i = 0; i < sonS.length; i++)
	if (Utils.eq(localModel().distribution().perBag(i), 0)) {
	  errors += tesT.perBag(i)-
	    tesT.perClassPerBag(i,localModel().distribution().
				maxClass());
	} else
	  errors += son(i).errorsForTree();

      return errors;
    }
  }

  /**
   * Computes estimated errors for leaf.
   * @exception Exception if error estimate can't be computed
   */

  private double errorsForLeaf() throws Exception {

    return tesT.total()-
      tesT.perClass(localModel().distribution().maxClass());
  }

  /**
   * Method just exists to make program easier to read.
   */
  
  private ClassifierSplitModel localModel(){
    
    return (ClassifierSplitModel)localModeL;
  }

  /**
   * Method just exists to make program easier to read.
   */
 
  private PruneableClassifierTree son(int index){

    return (PruneableClassifierTree)sonS[index];
  }
}







