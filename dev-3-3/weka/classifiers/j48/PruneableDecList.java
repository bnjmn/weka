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
 *    PruneableDecList.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package weka.classifiers.j48;

import weka.core.*;

/**
 * Class for handling a partial tree structure that
 * can be pruned using a pruning set.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.4 $
 */
public class PruneableDecList extends ClassifierDecList{

  /** Minimum number of objects */
  private int m_MinNumObj;
 
  /** To compute the entropy. */
  private static EntropySplitCrit m_splitCrit = new EntropySplitCrit();
  
  /**
   * Constructor for pruneable partial tree structure. 
   *
   * @param toSelectLocModel selection method for local splitting model
   * @param minNum minimum number of objects in leaf
   */
  public PruneableDecList(ModelSelection toSelectLocModel,
			  int minNum) {
			       
    super(toSelectLocModel);

    m_MinNumObj = minNum;
  }
  
  /**
   * Method for building a pruned partial tree.
   *
   * @exception Exception if tree can't be built successfully
   */
  public void buildRule(Instances train,
			Instances test) throws Exception { 
    
    buildDecList(train, test, false);

    cleanup(new Instances(train, 0));
  }
  
  /**
   * Method for choosing a subset to expand.
   */
  public final int chooseIndex() {
    
    int minIndex = -1;
    double estimated, min = Double.MAX_VALUE;
    int i, j;

    for (i = 0; i < m_sons.length; i++)
      if (son(i) == null){
	if (Utils.sm(localModel().distribution().perBag(i),
		     (double)m_MinNumObj))
	  estimated = Double.MAX_VALUE;
	else{
	  estimated = 0;
	  for (j = 0; j < localModel().distribution().numClasses(); j++) 
	    estimated -= m_splitCrit.logFunc(localModel().distribution().
				     perClassPerBag(i,j));
	  estimated += m_splitCrit.logFunc(localModel().distribution().
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
   * Choose last index (ie. choose rule).
   */
  public final int chooseLastIndex() {
    
    int minIndex = 0;
    double estimated, min = Double.MAX_VALUE;
    
    if (!m_isLeaf) 
      for (int i = 0; i < m_sons.length; i++)
	if (son(i) != null){
	  if (Utils.grOrEq(localModel().distribution().perBag(i),
			   (double)m_MinNumObj)) {
	    estimated = son(i).getSizeOfBranch();
	    if (Utils.sm(estimated,min)){
	      min = estimated;
	      minIndex = i;
	    }
	  }
	}

    return minIndex;
  }
  
  /**
   * Returns a newly created tree.
   *
   * @param data and selection method for local models.
   * @exception Exception if something goes wrong
   */
  protected ClassifierDecList getNewDecList(Instances train, Instances test, 
					    boolean leaf) throws Exception {
	 
    PruneableDecList newDecList = 
      new PruneableDecList(m_toSelectModel, m_MinNumObj);
    
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
      m_isLeaf = true;
      m_sons = null;
      m_localModel = new NoSplit(localModel().distribution());
    }
  }

  /**
   * Computes error estimate for tree.
   */
  private double errorsForTree() throws Exception {

    Distribution test;

    if (m_isLeaf)
      return errorsForLeaf();
    else {
      double error = 0;
      for (int i = 0; i < m_sons.length; i++) 
	if (Utils.eq(son(i).localModel().distribution().total(),0)) {
	  error += m_test.perBag(i)-
	    m_test.perClassPerBag(i,localModel().distribution().
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

    return m_test.total()-
	    m_test.perClass(localModel().distribution().maxClass());
  }
 
  /**
   * Returns the number of instances covered by a branch
   */
  private double getSizeOfBranch() {
    
    if (m_isLeaf) {
      return -localModel().distribution().total();
    } else
      return son(indeX).getSizeOfBranch();
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
  private PruneableDecList son(int index) {
    
    return (PruneableDecList)m_sons[index];
  }
}








