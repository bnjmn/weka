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
 *    C45PruneableClassifierTree.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package weka.classifiers.trees.j48;

import weka.core.*;

/**
 * Class for handling a tree structure that can
 * be pruned using C4.5 procedures.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.11 $
 */

public class C45PruneableClassifierTree extends ClassifierTree{

  /** True if the tree is to be pruned. */
  boolean m_pruneTheTree = false;

  /** The confidence factor for pruning. */
  float m_CF = 0.25f;

  /** Is subtree raising to be performed? */
  boolean m_subtreeRaising = true;

  /** Cleanup after the tree has been built. */
  boolean m_cleanup = true;

  /**
   * Constructor for pruneable tree structure. Stores reference
   * to associated training data at each node.
   *
   * @param toSelectLocModel selection method for local splitting model
   * @param pruneTree true if the tree is to be pruned
   * @param cf the confidence factor for pruning
   * @exception Exception if something goes wrong
   */
  public C45PruneableClassifierTree(ModelSelection toSelectLocModel,
				    boolean pruneTree,float cf,
				    boolean raiseTree,
				    boolean cleanup)
       throws Exception {

    super(toSelectLocModel);

    m_pruneTheTree = pruneTree;
    m_CF = cf;
    m_subtreeRaising = raiseTree;
    m_cleanup = cleanup;
  }

  /**
   * Method for building a pruneable classifier tree.
   *
   * @exception Exception if something goes wrong
   */
  public void buildClassifier(Instances data) throws Exception {

   if (data.classAttribute().isNumeric())
     throw new UnsupportedClassTypeException("Class is numeric!");
   if (data.checkForStringAttributes()) {
     throw new UnsupportedAttributeTypeException("Cannot handle string attributes!");
   }
   data = new Instances(data);
   data.deleteWithMissingClass();
   buildTree(data, m_subtreeRaising);
   collapse();
   if (m_pruneTheTree) {
     prune();
   }
   if (m_cleanup) {
     cleanup(new Instances(data, 0));
   }
  }

  /**
   * Collapses a tree to a node if training error doesn't increase.
   */
  public final void collapse(){

    double errorsOfSubtree;
    double errorsOfTree;
    int i;

    if (!m_isLeaf){
      errorsOfSubtree = getTrainingErrors();
      errorsOfTree = localModel().distribution().numIncorrect();
      if (errorsOfSubtree >= errorsOfTree-1E-3){

	// Free adjacent trees
	m_sons = null;
	m_isLeaf = true;
			
	// Get NoSplit Model for tree.
	m_localModel = new NoSplit(localModel().distribution());
      }else
	for (i=0;i<m_sons.length;i++)
	  son(i).collapse();
    }
  }

  /**
   * Prunes a tree using C4.5's pruning procedure.
   *
   * @exception Exception if something goes wrong
   */
  public void prune() throws Exception {

    double errorsLargestBranch;
    double errorsLeaf;
    double errorsTree;
    int indexOfLargestBranch;
    C45PruneableClassifierTree largestBranch;
    int i;

    if (!m_isLeaf){

      // Prune all subtrees.
      for (i=0;i<m_sons.length;i++)
	son(i).prune();

      // Compute error for largest branch
      indexOfLargestBranch = localModel().distribution().maxBag();
      if (m_subtreeRaising) {
	errorsLargestBranch = son(indexOfLargestBranch).
	  getEstimatedErrorsForBranch((Instances)m_train);
      } else {
	errorsLargestBranch = Double.MAX_VALUE;
      }

      // Compute error if this Tree would be leaf
      errorsLeaf = 
	getEstimatedErrorsForDistribution(localModel().distribution());

      // Compute error for the whole subtree
      errorsTree = getEstimatedErrors();

      // Decide if leaf is best choice.
      if (Utils.smOrEq(errorsLeaf,errorsTree+0.1) &&
	  Utils.smOrEq(errorsLeaf,errorsLargestBranch+0.1)){

	// Free son Trees
	m_sons = null;
	m_isLeaf = true;
		
	// Get NoSplit Model for node.
	m_localModel = new NoSplit(localModel().distribution());
	return;
      }

      // Decide if largest branch is better choice
      // than whole subtree.
      if (Utils.smOrEq(errorsLargestBranch,errorsTree+0.1)){
	largestBranch = son(indexOfLargestBranch);
	m_sons = largestBranch.m_sons;
	m_localModel = largestBranch.localModel();
	m_isLeaf = largestBranch.m_isLeaf;
	newDistribution(m_train);
	prune();
      }
    }
  }

  /**
   * Returns a newly created tree.
   *
   * @exception Exception if something goes wrong
   */
  protected ClassifierTree getNewTree(Instances data) throws Exception {
    
    C45PruneableClassifierTree newTree = 
      new C45PruneableClassifierTree(m_toSelectModel, m_pruneTheTree, m_CF,
				     m_subtreeRaising, m_cleanup);
    newTree.buildTree((Instances)data, m_subtreeRaising);

    return newTree;
  }

  /**
   * Computes estimated errors for tree.
   */
  private double getEstimatedErrors(){

    double errors = 0;
    int i;

    if (m_isLeaf)
      return getEstimatedErrorsForDistribution(localModel().distribution());
    else{
      for (i=0;i<m_sons.length;i++)
	errors = errors+son(i).getEstimatedErrors();
      return errors;
    }
  }
  
  /**
   * Computes estimated errors for one branch.
   *
   * @exception Exception if something goes wrong
   */
  private double getEstimatedErrorsForBranch(Instances data) 
       throws Exception {

    Instances [] localInstances;
    double errors = 0;
    int i;

    if (m_isLeaf)
      return getEstimatedErrorsForDistribution(new Distribution(data));
    else{
      Distribution savedDist = localModel().m_distribution;
      localModel().resetDistribution(data);
      localInstances = (Instances[])localModel().split(data);
      localModel().m_distribution = savedDist;
      for (i=0;i<m_sons.length;i++)
	errors = errors+
	  son(i).getEstimatedErrorsForBranch(localInstances[i]);
      return errors;
    }
  }

  /**
   * Computes estimated errors for leaf.
   */
  private double getEstimatedErrorsForDistribution(Distribution 
						   theDistribution){

    if (Utils.eq(theDistribution.total(),0))
      return 0;
    else
      return theDistribution.numIncorrect()+
	Stats.addErrs(theDistribution.total(),
		      theDistribution.numIncorrect(),m_CF);
  }

  /**
   * Computes errors of tree on training data.
   */
  private double getTrainingErrors(){

    double errors = 0;
    int i;

    if (m_isLeaf)
      return localModel().distribution().numIncorrect();
    else{
      for (i=0;i<m_sons.length;i++)
	errors = errors+son(i).getTrainingErrors();
      return errors;
    }
  }

  /**
   * Method just exists to make program easier to read.
   */
  private ClassifierSplitModel localModel(){
    
    return (ClassifierSplitModel)m_localModel;
  }

  /**
   * Computes new distributions of instances for nodes
   * in tree.
   *
   * @exception Exception if something goes wrong
   */
  private void newDistribution(Instances data) throws Exception {

    Instances [] localInstances;

    localModel().resetDistribution(data);
    m_train = data;
    if (!m_isLeaf){
      localInstances = 
	(Instances [])localModel().split(data);
      for (int i = 0; i < m_sons.length; i++)
	son(i).newDistribution(localInstances[i]);
    } else {

      // Check whether there are some instances at the leaf now!
      if (!Utils.eq(data.sumOfWeights(), 0)) {
	m_isEmpty = false;
      }
    }
  }

  /**
   * Method just exists to make program easier to read.
   */
  private C45PruneableClassifierTree son(int index){

    return (C45PruneableClassifierTree)m_sons[index];
  }
}







