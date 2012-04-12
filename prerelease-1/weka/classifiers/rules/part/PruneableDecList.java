/*
 *    PruneableDecList.java
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

/**
 * Class for handling a partial tree structure that
 * can be pruned using a pruning set.
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */

public class PruneableDecList extends ClassifierDecList{
  
  // =================
  // Private variables
  // =================

  /**
   * Minimum number of objects
   */

  private int minNumObj;
 
  /**
   * To compute the entropy.
   */
  
  private static EntropySplitCrit splitCrit = new EntropySplitCrit();
 
  // ===============
  // Public methods.
  // ===============
  
  /**
   * Constructor for pruneable tree structure. Stores reference
   * to associated training data at each node.
   * @param toSelectLocModel selection method for local splitting model
   */
  
  public PruneableDecList(ModelSelection toSelectLocModel,
			  int minNum) {
			       
    super(toSelectLocModel);

    minNumObj = minNum;
  }
  
  /**
   * Method for building a pruned partial tree.
   * @exception Exception if rule can't be built successfully
   */
  
  public void buildRule(Instances train,
			Instances test) throws Exception{
    
    buildDecList(train, test, false);

    cleanup(new Instances(train, 0));
  }
  
  /**
   * Method for choosing a subset to expand.
   */

  public final int chooseIndex(){
    
    int minIndex = -1;
    double estimated, min = Double.MAX_VALUE;
    int i, j;

    for (i = 0; i < sonS.length; i++)
      if (son(i) == null){
	if (Utils.sm(localModel().distribution().perBag(i),
		     (double)minNumObj))
	  estimated = Double.MAX_VALUE;
	else{
	  estimated = 0;
	  for (j = 0; j < localModel().distribution().numClasses(); j++) 
	    estimated -= splitCrit.logFunc(localModel().distribution().
				     perClassPerBag(i,j));
	  estimated += splitCrit.logFunc(localModel().distribution().
				   perBag(i));
	  estimated /= localModel().distribution().perBag(i);
	}
	if (Utils.smOrEq(estimated,0)) // This is certainly a good one.
	  return i;
	if (Utils.sm(estimated,min)){
	  min = estimated;
	  minIndex = i;
	}
      }

    return minIndex;
  }
  
  /**
   * Choose last index.
   */
  
  public final int chooseLastIndex(){
    
    int minIndex = 0;
    double estimated, min = Double.MAX_VALUE;
    
    if (!isLeaF) 
      for (int i = 0; i < sonS.length; i++)
	if (son(i) != null){
	  if (Utils.grOrEq(localModel().distribution().perBag(i),
			   (double)minNumObj)) {
	    estimated = son(i).getSizeOfBranch();
	    if (Utils.sm(estimated,min)){
	      min = estimated;
	      minIndex = i;
	    }
	  }
	}

    return minIndex;
  }
  
  // ==================
  // Protected methods.
  // ==================
  
  /**
   * Returns a newly created tree.
   * @param data and selection method for local models.
   * @exception Exception if something goes wrong
   */
  
  protected ClassifierDecList getNewDecList(Instances train, Instances test, 
					    boolean leaf) throws Exception{
	 
    PruneableDecList newDecList = 
      new PruneableDecList(toSelectModeL, minNumObj);
    
    newDecList.buildDecList((Instances)train, test, leaf);
    
    return newDecList;
  }

  /**
   * Prunes the end of the rule.
   */
  
  protected void pruneEnd() throws Exception {
    
    double errorsLeaf, errorsTree;
    
    errorsTree = errorsForTree();
    errorsLeaf = errorsForLeaf();
    if (Utils.smOrEq(errorsLeaf,errorsTree)){ 
      isLeaF = true;
      sonS = null;
      localModeL = new NoSplit(localModel().distribution());
    }
  }
   
  // ================
  // Private methods.
  // ================
 
  private double errorsForTree() throws Exception{

    Distribution test;

    if (isLeaF)
      return errorsForLeaf();
    else {
      double error = 0;
      for (int i = 0; i < sonS.length; i++) 
	if (Utils.eq(son(i).localModel().distribution().total(),0)) {
	  error += tesT.perBag(i)-
	    tesT.perClassPerBag(i,localModel().distribution().
				maxClass());
	} else
	  error += son(i).errorsForTree();

      return error;
    }
  }

  /**
   * Computes estimated errors for leaf.
   */

  private double errorsForLeaf() throws Exception {

    return tesT.total()-
	    tesT.perClass(localModel().distribution().maxClass());
  }
 
  /**
   * Returns the number of instances covered by a branch
   */

  private double getSizeOfBranch(){
    
    if (isLeaF) {
      return -localModel().distribution().total();
    } else
      return son(indeX).getSizeOfBranch();
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
  
  private PruneableDecList son(int index){
    
    return (PruneableDecList)sonS[index];
  }
}








