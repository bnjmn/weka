/*
 *    ClassifierDecList.java
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
import java.io.*;

/**
 * Class for handling a rule (partial tree) for a decision list.
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */

public class ClassifierDecList implements Serializable {
    
  // ====================
  // Protected variables.
  // ====================

  protected ModelSelection toSelectModeL;    // The model selection method.
  protected ClassifierSplitModel localModeL; // Local model at node.
  protected ClassifierDecList [] sonS;       // References to sons.
  protected boolean isLeaF;                  // True if node is leaf.
  protected boolean isEmptY;                 // True if node is empty.
  protected Instances traiN;                 // The training instances.
  protected Distribution tesT;               // The pruning instances.
  protected int indeX;                       // Which son to expand?
    
  // ===============
  // Public methods.
  // ===============

  /**
   * Constructor - just calls constructor of class DecList.
   */

  public ClassifierDecList(ModelSelection toSelectLocModel){

    toSelectModeL = toSelectLocModel;
   }

  /**
   * Builds the partial tree without hold out set.
   * @exception Exception if something goes wrong
   */

  public void buildDecList(Instances data, boolean leaf) throws Exception{
    
    Instances [] localInstances,localPruneInstances;
    int index,ind;
    int i,j;
    double sumOfWeights;
    NoSplit noSplit;
    
    traiN = null;
    tesT = null;
    isLeaF = false;
    isEmptY = false;
    sonS = null;
    indeX = 0;
    sumOfWeights = data.sumOfWeights();
    noSplit = new NoSplit (new Distribution((Instances)data));
    if (leaf)
      localModeL = noSplit;
    else
      localModeL = toSelectModeL.selectModel(data);
    if (localModeL.numSubsets() > 1) {
      localInstances = localModeL.split(data);
      data = null;
      sonS = new ClassifierDecList [localModeL.numSubsets()];
      i = 0;
      do {
	i++;
	ind = chooseIndex();
	if (ind == -1) {
	  for (j = 0; j < sonS.length; j++) 
	    if (sonS[j] == null)
	      sonS[j] = getNewDecList(localInstances[j],true);
	  if (i < 2) {
	    localModeL = noSplit;
	    isLeaF = true;
	    sonS = null;
	    if (Utils.eq(sumOfWeights,0))
	      isEmptY = true;
	    return;
	  }
	  ind = 0;
	  break;
	} else 
	  sonS[ind] = getNewDecList(localInstances[ind],false);
      } while ((i < sonS.length) && (sonS[ind].isLeaF));
      
      // Check if all successors are leaves

      for (j = 0; j < sonS.length; j++) 
	if ((sonS[j] == null) || (!sonS[j].isLeaF))
	  break;
      if (j == sonS.length) {
	pruneEnd();
	if (!isLeaF) 
	  indeX = chooseLastIndex();
      }else 
	indeX = chooseLastIndex();
    }else{
      isLeaF = true;
      if (Utils.eq(sumOfWeights, 0))
	isEmptY = true;
    }
  }

  /**
   * Builds the partial tree with hold out set
   * @exception Exception if something goes wrong
   */

  public void buildDecList(Instances train, Instances test, 
			   boolean leaf) throws Exception{
    
    Instances [] localTrain,localTest;
    int index,ind;
    int i,j;
    double sumOfWeights;
    NoSplit noSplit;
    
    traiN = null;
    isLeaF = false;
    isEmptY = false;
    sonS = null;
    indeX = 0;
    sumOfWeights = train.sumOfWeights();
    noSplit = new NoSplit (new Distribution((Instances)train));
    if (leaf)
      localModeL = noSplit;
    else
      localModeL = toSelectModeL.selectModel(train, test);
    tesT = new Distribution(test, localModeL);
    if (localModeL.numSubsets() > 1) {
      localTrain = localModeL.split(train);
      localTest = localModeL.split(test);
      train = null;
      test = null;
      sonS = new ClassifierDecList [localModeL.numSubsets()];
      i = 0;
      do {
	i++;
	ind = chooseIndex();
	if (ind == -1) {
	  for (j = 0; j < sonS.length; j++) 
	    if (sonS[j] == null)
	      sonS[j] = getNewDecList(localTrain[j],localTest[j],true);
	  if (i < 2) {
	    localModeL = noSplit;
	    isLeaF = true;
	    sonS = null;
	    if (Utils.eq(sumOfWeights,0))
	      isEmptY = true;
	    return;
	  }
	  ind = 0;
	  break;
	} else 
	  sonS[ind] = getNewDecList(localTrain[ind],localTest[ind],false);
      } while ((i < sonS.length) && (sonS[ind].isLeaF));
      
      // Check if all successors are leaves

      for (j = 0; j < sonS.length; j++) 
	if ((sonS[j] == null) || (!sonS[j].isLeaF))
	  break;
      if (j == sonS.length) {
	pruneEnd();
	if (!isLeaF) 
	  indeX = chooseLastIndex();
      }else 
	indeX = chooseLastIndex();
    }else{
      isLeaF = true;
      if (Utils.eq(sumOfWeights, 0))
	isEmptY = true;
    }
  }

  /** 
   * Classifies a weighted instance.
   * @exception Exception if something goes wrong
   */

  public double classifyInstance(Instance instance)
       throws Exception {

    double maxProb = -1;
    double currentProb;
    int maxIndex = 0;
    int j;

    for (j = 0; j < instance.numClasses();
	 j++){
      currentProb = getProbs(j,instance,1);
      if (Utils.gr(currentProb,maxProb)){
	maxIndex = j;
	maxProb = currentProb;
      }
    }
    if (Utils.eq(maxProb,0))
      return -1.0;
    else
      return (double)maxIndex;
  }

  /** 
   * Returns class probabilities for a weighted instance.
   * @exception Exception if something goes wrong
   */

  public final double [] distributionForInstance(Instance instance) 
       throws Exception {
		

    double [] doubles =
      new double[instance.numClasses()];

    for (int i = 0; i < doubles.length; i++)
      doubles[i] = getProbs(i,instance,1);
    
    return doubles;
  }
  
  /**
   * Returns the weight a rule assigns to an instance.
   * @exception Exception if something goes wrong
   */

  public double weight(Instance instance) throws Exception {

    int subset;

    if (isLeaF)
      return 1;
    subset = localModeL.whichSubset(instance);
    if (subset == -1)
      return (localModeL.weights(instance))[indeX]*
	sonS[indeX].weight(instance);
    if (subset == indeX)
      return sonS[indeX].weight(instance);
    return 0;
  }

  /**
   * Cleanup in order to save memory.
   */

  public final void cleanup(Instances justHeaderInfo) {

    traiN = justHeaderInfo;
    tesT = null;
    if (!isLeaF)
      for (int i = 0; i < sonS.length; i++)
	if (sonS[i] != null)
	  sonS[i].cleanup(justHeaderInfo);
  }

  /**
   * Prints rules.
   */

  public String toString(){

    try {
      StringBuffer text;
      
      text = new StringBuffer();
      if (isLeaF){
	text.append(": ");
	text.append(localModeL.dumpLabel(0,traiN)+"\n");
      }else{
      dumpDecList(text);
      //dumpTree(0,text);
      }
      return text.toString();
    } catch (Exception e) {
      return "Can't print rule.";
    }
  }

  // ==================
  // Protected methods.
  // ==================

  /**
   * Returns a newly created tree.
   * @exception Exception if something goes wrong
   */

  protected ClassifierDecList getNewDecList(Instances train, boolean leaf) 
    throws Exception{
	 
    ClassifierDecList newDecList = new ClassifierDecList(toSelectModeL);
    newDecList.buildDecList(train,leaf);
    
    return newDecList;
  }

  /**
   * Returns a newly created tree.
   * @exception Exception if something goes wrong
   */

  protected ClassifierDecList getNewDecList(Instances train, Instances test,
				  boolean leaf) 
       throws Exception{
	 
    ClassifierDecList newDecList = new ClassifierDecList(toSelectModeL);
    newDecList.buildDecList(train, test ,leaf);
    
    return newDecList;
  }

  // Three dummy methods

  protected int chooseLastIndex() {

    return 0;
  };

  protected int chooseIndex() {

    return 0;
  };
  
  protected void pruneEnd() throws Exception {
  };
  
  // ================
  // Private methods.
  // ================

  /**
   * Help method for printing tree structure.
   */

  private void dumpDecList(StringBuffer text) throws Exception {
    
    text.append(localModeL.leftSide(traiN));
    text.append(localModeL.rightSide(indeX, traiN));
    if (sonS[indeX].isLeaF){
      text.append(": ");
      text.append(localModeL.dumpLabel(indeX,traiN)+"\n");
    }else{
      text.append(" AND\n");
      sonS[indeX].dumpDecList(text);
    }
  }

  /**
   * Dumps the partial tree (only used for debugging)
   * @exception exception Exception if something goes wrong
   */

  private void dumpTree(int depth,StringBuffer text)
       throws Exception {
    
    int i,j;
    
    for (i=0;i<sonS.length;i++){
      text.append("\n");;
      for (j=0;j<depth;j++)
	text.append("|   ");
      text.append(localModeL.leftSide(traiN));
      text.append(localModeL.rightSide(i, traiN));
      if (sonS[i] == null)
	text.append("null");
      else if (sonS[i].isLeaF){
	text.append(": ");
	text.append(localModeL.dumpLabel(i,traiN));
      }else
	sonS[i].dumpTree(depth+1,text);
    }
  }

  /**
   * Help method for computing class probabilities of 
   * a given instance.
   * @exception exception Exception if something goes wrong
   */

  private double getProbs(int classIndex,Instance instance,
			  double weight) throws Exception {
    
    double [] weights;
    int treeIndex;

    if (isLeaF){
      return weight*localModel().classProb(classIndex,instance);
    }else{
      treeIndex = localModel().whichSubset(instance);
      if (treeIndex == -1){
	weights = localModel().weights(instance);
	return son(indeX).getProbs(classIndex,instance,weights[indeX]*weight);
      }else{
	if (treeIndex == indeX)
	  return son(indeX).getProbs(classIndex,instance,weight);
	return 0;
      }
    }
  }

  /**
   * Method just exists to make program easier to read.
   */
  
  private ClassifierSplitModel localModel(){
    
    return localModeL;
  }

  /**
   * Method just exists to make program easier to read.
   */

  private ClassifierDecList son(int index){
    
    return sonS[index];
  }
}





