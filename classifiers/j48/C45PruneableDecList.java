/*
 *    C45PruneableDecList.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package weka.classifiers.j48;

import weka.core.*;

/**
 * Class for handling a partial tree structure pruned using C4.5's
 * pruning heuristic.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public class C45PruneableDecList extends ClassifierDecList{
    
  /** CF */
  private double CF = 0.25;

  /** Minimum number of objects */
  private int minNumObj;

  /** To compute the entropy. */
  private static EntropySplitCrit splitCrit = new EntropySplitCrit();
  
  /**
   * Constructor for pruneable tree structure. Stores reference
   * to associated training data at each node.
   *
   * @param toSelectLocModel selection method for local splitting model
   * @param cf the confidence factor for pruning
   * @param minNum the minimum number of objects in a leaf
   * @exception Exception if something goes wrong
   */
  public C45PruneableDecList(ModelSelection toSelectLocModel, 
			     double cf, int minNum) 
       throws Exception {
			       
    super(toSelectLocModel);
    
    CF = cf;
    minNumObj = minNum;
  }
  
  /**
   * Method for building a pruned partial tree.
   *
   * @exception Exception if something goes wrong
   */
  public void buildRule(Instances data) throws Exception {
    
    buildDecList(data, false);

    cleanup(new Instances(data, 0));
  }
  
  /**
   * Method for choosing a subset to expand.
   */
  public final int chooseIndex() {
    
    int minIndex = -1;
    double estimated, min = Double.MAX_VALUE;
    int i, j;

    for (i = 0; i < m_sons.length; i++)
      if (son(i) == null) {
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
	if (Utils.smOrEq(estimated,0))
	  return i;
	if (Utils.sm(estimated,min)) {
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
	if (son(i) != null) {
	  if (Utils.grOrEq(localModel().distribution().perBag(i),
			   (double)minNumObj)) {
	    estimated = son(i).getSizeOfBranch();
	    if (Utils.sm(estimated,min)) {
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
   * @exception Exception if something goes wrong
   */
  protected ClassifierDecList getNewDecList(Instances data, boolean leaf) 
       throws Exception {
	 
    C45PruneableDecList newDecList = 
      new C45PruneableDecList(m_toSelectModel,CF, minNumObj);
    
    newDecList.buildDecList((Instances)data, leaf);
    
    return newDecList;
  }

  /**
   * Prunes the end of the rule.
   */
  protected void pruneEnd() {
    
    double errorsLeaf, errorsTree;
    
    errorsTree = getEstimatedErrorsForTree();
    errorsLeaf = getEstimatedErrorsForLeaf();
    if (Utils.smOrEq(errorsLeaf,errorsTree+0.1)) { // +0.1 as in C4.5
      m_isLeaf = true;
      m_sons = null;
      m_localModel = new NoSplit(localModel().distribution());
    }
  }
  
  /**
   * Computes estimated errors for tree.
   */
  private double getEstimatedErrorsForTree() {

    if (m_isLeaf)
      return getEstimatedErrorsForLeaf();
    else {
      double error = 0;
      for (int i = 0; i < m_sons.length; i++) 
	if (!Utils.eq(son(i).localModel().distribution().total(),0))
	  error += son(i).getEstimatedErrorsForTree();
      return error;
    }
  }
  
  /**
   * Computes estimated errors for leaf.
   */
  public double getEstimatedErrorsForLeaf() {
  
    double errors = localModel().distribution().numIncorrect();

    return errors+Stats.addErrs(localModel().distribution().total(),
				errors,(float)CF);
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
  private C45PruneableDecList son(int index) {
    
    return (C45PruneableDecList)m_sons[index];
  }
}








