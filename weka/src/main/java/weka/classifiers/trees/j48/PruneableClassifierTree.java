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
 *    PruneableClassifierTree.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package weka.classifiers.trees.j48;

import weka.core.*;
import java.util.*;

/**
 * Class for handling a tree structure that can
 * be pruned using a pruning set. 
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.8 $
 */
public class PruneableClassifierTree extends ClassifierTree{

  /** True if the tree is to be pruned. */
  private boolean pruneTheTree = false;

  /** How many subsets of equal size? One used for pruning, the rest for training. */
  private int numSets = 3;

  /** Cleanup after the tree has been built. */
  private boolean m_cleanup = true;

  /** The random number seed. */
  private int m_seed = 1;

  /**
   * Constructor for pruneable tree structure. Stores reference
   * to associated training data at each node.
   *
   * @param toSelectLocModel selection method for local splitting model
   * @param pruneTree true if the tree is to be pruned
   * @param num number of subsets of equal size
   * @exception Exception if something goes wrong
   */
  public PruneableClassifierTree(ModelSelection toSelectLocModel,
				 boolean pruneTree, int num, boolean cleanup,
				 int seed)
       throws Exception {

    super(toSelectLocModel);

    pruneTheTree = pruneTree;
    numSets = num;
    m_cleanup = cleanup;
    m_seed = seed;
  }

  /**
   * Method for building a pruneable classifier tree.
   *
   * @exception Exception if tree can't be built successfully
   */
  public void buildClassifier(Instances data) 
       throws Exception {

   if (data.classAttribute().isNumeric())
      throw new Exception("Class is numeric!");
   
   data = new Instances(data);
   Random random = new Random(m_seed);
   data.deleteWithMissingClass();
   data.stratify(numSets);
   buildTree(data.trainCV(numSets, numSets - 1, random),
	     data.testCV(numSets, numSets - 1), false);
   if (pruneTheTree) {
     prune();
   }
   if (m_cleanup) {
     cleanup(new Instances(data, 0));
   }
  }

  /**
   * Prunes a tree.
   *
   * @exception Exception if tree can't be pruned successfully
   */
  public void prune() throws Exception {
  
    if (!m_isLeaf) {
      
      // Prune all subtrees.
      for (int i = 0; i < m_sons.length; i++)
	son(i).prune();
      
      // Decide if leaf is best choice.
      if (Utils.smOrEq(errorsForLeaf(),errorsForTree())) {
	
	// Free son Trees
	m_sons = null;
	m_isLeaf = true;
	
	// Get NoSplit Model for node.
	m_localModel = new NoSplit(localModel().distribution());
      }
    }
  }

  /**
   * Returns a newly created tree.
   *
   * @param data and selection method for local models.
   */
  protected ClassifierTree getNewTree(Instances train, Instances test) 
       throws Exception {

    PruneableClassifierTree newTree = 
      new PruneableClassifierTree(m_toSelectModel, pruneTheTree, numSets, m_cleanup,
				  m_seed);
    newTree.buildTree(train, test, false);
    return newTree;
  }

  /**
   * Computes estimated errors for tree.
   *
   * @exception Exception if error estimate can't be computed
   */
  private double errorsForTree() throws Exception {

    double errors = 0;

    if (m_isLeaf)
      return errorsForLeaf();
    else{
      for (int i = 0; i < m_sons.length; i++)
	if (Utils.eq(localModel().distribution().perBag(i), 0)) {
	  errors += m_test.perBag(i)-
	    m_test.perClassPerBag(i,localModel().distribution().
				maxClass());
	} else
	  errors += son(i).errorsForTree();

      return errors;
    }
  }

  /**
   * Computes estimated errors for leaf.
   *
   * @exception Exception if error estimate can't be computed
   */
  private double errorsForLeaf() throws Exception {

    return m_test.total()-
      m_test.perClass(localModel().distribution().maxClass());
  }

  /**
   * Method just exists to make program easier to read.
   */
  private ClassifierSplitModel localModel() {
    
    return (ClassifierSplitModel)m_localModel;
  }

  /**
   * Method just exists to make program easier to read.
   */
  private PruneableClassifierTree son(int index) {

    return (PruneableClassifierTree)m_sons[index];
  }
}







