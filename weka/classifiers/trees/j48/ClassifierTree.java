/*
 *    ClassifierTree.java
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
import weka.classifiers.*;
import java.io.*;

/**
 * Class for handling a tree structure used for
 * classification.
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.1 September 1998 (Eibe)
 */

public class ClassifierTree implements Drawable, Serializable {
  
  // ====================
  // Protected variables.
  // ====================
  
  protected ModelSelection toSelectModeL;    // The model selection method.
  protected ClassifierSplitModel localModeL; // Local model at node.
  protected ClassifierTree [] sonS;          // References to sons.
  protected boolean isLeaF;                  // True if node is leaf.
  protected boolean isEmptY;                 // True if node is empty.
  protected Instances traiN;                 // The training instances.
  protected Distribution tesT;               // The pruning instances.
  
  // ===============
  // Public methods.
  // ===============

  /**
   * Constructor 
   */

  public ClassifierTree(ModelSelection toSelectLocModel){
    
    toSelectModeL = toSelectLocModel;
  }

  /**
   * Method for building a classifier tree.
   * @exception Exception if something goes wrong
   */

  public void buildClassifier(Instances data) throws Exception{

    if (data.checkForStringAttributes()) {
      throw new Exception("Can't handle string attributes!");
    }
    data = new Instances(data);
    data.deleteWithMissingClass();
    buildTree(data, false);
  }

  /**
   * Builds the tree structure
   * @param data the data for which the tree structure is to be
   * generated.
   * @param keepData is training data to be kept?
   * @exception Exception if something goes wrong
   */

  public void buildTree(Instances data, boolean keepData) throws Exception{
    
    Instances [] localInstances;

    if (keepData) {
      traiN = data;
    }
    tesT = null;
    isLeaF = false;
    isEmptY = false;
    sonS = null;
    localModeL = toSelectModeL.selectModel(data);
    if (localModeL.numSubsets() > 1){
      localInstances = localModeL.split(data);
      data = null;
      sonS = new ClassifierTree [localModeL.numSubsets()];
      for (int i = 0; i < sonS.length; i++) {
	sonS[i] = getNewTree(localInstances[i]);
	localInstances[i] = null;
      }
    }else{
      isLeaF = true;
      if (Utils.eq(data.sumOfWeights(), 0))
	isEmptY = true;
      data = null;
    }
  }

  /**
   * Builds the tree structure with hold out set
   * @param traiN the data for which the tree structure is to be
   * generated.
   * @param keepData is training Data to be kept?
   * @exception Exception if something goes wrong
   */

  public void buildTree(Instances train, Instances test, boolean keepData)
       throws Exception{
    
    Instances [] localTrain, localTest;
    int i;
    
    if (keepData) {
      traiN = train;
    }
    isLeaF = false;
    isEmptY = false;
    sonS = null;
    localModeL = toSelectModeL.selectModel(train, test);
    tesT = new Distribution(test, localModeL);
    if (localModeL.numSubsets() > 1){
      localTrain = localModeL.split(train);
      localTest = localModeL.split(test);
      train = test = null;
      sonS = new ClassifierTree [localModeL.numSubsets()];
      for (i=0;i<sonS.length;i++) {
	sonS[i] = getNewTree(localTrain[i], localTest[i]);
	localTrain[i] = null;
	localTest[i] = null;
      }
    }else{
      isLeaF = true;
      if (Utils.eq(train.sumOfWeights(), 0))
	isEmptY = true;
      train = test = null;
    }
  }

  /** 
   * Classifies a weighted instance.
   * @exception Exception if something goes wrong
   */

  public double classifyInstance(Instance instance) throws Exception {

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

    return (double)maxIndex;
  }

  /**
   * Cleanup in order to save memory.
   */

  public final void cleanup(Instances justHeaderInfo) {

    traiN = justHeaderInfo;
    tesT = null;
    if (!isLeaF)
      for (int i = 0; i < sonS.length; i++)
	sonS[i].cleanup(justHeaderInfo);
  }

  /** 
   * Returns class probabilities for a weighted instance.
   * @exception Exception if something goes wrong
   */

  public final double [] distributionForInstance(Instance instance) 
       throws Exception {

    double [] doubles = 
      new double[instance.numClasses()];

    for (int i = 0; i < doubles.length; i++) {
      doubles[i] = getProbs(i,instance,1);
    }

    return doubles;
  }

  /**
   * Returns graph describing the tree.
   * @exception Exception if something goes wrong
   */

  public String graph() throws Exception {

    StringBuffer text = new StringBuffer();

    text.append("digraph J48Tree {\n" +
		"node [fontsize=10]\n" +
		"edge [fontsize=10 style=bold]\n");
    if (isLeaF){
      text.append("N" + Integer.toHexString(localModeL.hashCode())
		  + " [label=\"" + 
		  localModeL.dumpLabel(0,traiN) + "\" "+ 
		  "shape=box style=filled color=gray95]\n");
    }else {
      text.append("N" + Integer.toHexString(localModeL.hashCode())
		  + " [label=\"" + 
		  localModeL.leftSide(traiN) + "\"]\n");
      graphTree(text);
    }
    
    return text.toString() +"}\n";
  }

  /**
   * Returns tree in prefix order.
   * @exception Exception if something goes wrong
   */

  public String prefix() throws Exception {
    
    StringBuffer text;

    text = new StringBuffer();
    if (isLeaF){
      text.append("["+localModeL.dumpLabel(0,traiN)+"]");
    }else {
      prefixTree(text);
    }
    
    return text.toString();
  }


  /**
   * Returns number of leaves in tree structure.
   */
  
  public int numLeaves(){
    
    int num = 0;
    int i;
    
    if (isLeaF)
      return 1;
    else
      for (i=0;i<sonS.length;i++)
	num = num+sonS[i].numLeaves();
        
    return num;
  }

  /**
   * Returns number of nodes in tree structure.
   */
  
  public int numNodes(){
    
    int no = 1;
    int i;
    
    if (!isLeaF)
      for (i=0;i<sonS.length;i++)
	no = no+sonS[i].numNodes();
    
    return no;
  }

  /**
   * Prints tree structure.
   */

  public String toString(){

    try {
      StringBuffer text = new StringBuffer();
      
      if (isLeaF){
	text.append(": ");
	text.append(localModeL.dumpLabel(0,traiN));
      }else
	dumpTree(0,text);
      text.append("\n\nNumber of Leaves  : \t"+numLeaves()+"\n");
      text.append("\nSize of the tree : \t"+numNodes()+"\n");
 
      return text.toString();
    } catch (Exception e) {
      return "Can't print classification tree.";
    }
  }

  // ==================
  // Protected methods.
  // ==================

  /**
   * Returns a newly created tree.
   * @param data and selection method for local models.
   * @exception Exception if something goes wrong
   */

  protected ClassifierTree getNewTree(Instances data) throws Exception{
	 
    ClassifierTree newTree = new ClassifierTree(toSelectModeL);
    newTree.buildTree(data, false);
    
    return newTree;
  }

  /**
   * Returns a newly created tree.
   * @param data and selection method for local models.
   * @exception Exception if something goes wrong
   */

  protected ClassifierTree getNewTree(Instances train, Instances test) 
       throws Exception{
	 
    ClassifierTree newTree = new ClassifierTree(toSelectModeL);
    newTree.buildTree(train, test, false);
    
    return newTree;
  }

  // ================
  // Private methods.
  // ================

  /**
   * Help method for printing tree structure.
   * @exception Exception if something goes wrong
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
      if (sonS[i].isLeaF){
	text.append(": ");
	text.append(localModeL.dumpLabel(i,traiN));
      }else
	sonS[i].dumpTree(depth+1,text);
    }
  }

  /**
   * Help method for printing tree structure.
   * @exception Exception if something goes wrong
   */

  private void graphTree(StringBuffer text) throws Exception {
    
    for (int i = 0; i < sonS.length; i++) {
      text.append("N" + Integer.toHexString(localModeL.hashCode()) 
		  + "->" + 
		  "N" + Integer.toHexString(sonS[i].localModeL.hashCode())  +
		  " [label=\"" + localModeL.rightSide(i,traiN).trim() + 
		  "\"]\n");
      if (sonS[i].isLeaF) {
	text.append("N" + Integer.toHexString(sonS[i].localModeL.hashCode()) +
		    " [label=\""+localModeL.dumpLabel(i,traiN)+"\" "+ 
		    "shape=box style=filled color=gray95]\n");
      } else {
	text.append("N" + Integer.toHexString(sonS[i].localModeL.hashCode()) +
		    " [label=\""+sonS[i].localModeL.leftSide(traiN)+"\"]\n");
	sonS[i].graphTree(text);
      }
    }
  }

  /**
   * Prints the tree in prefix form
   */

  private void prefixTree(StringBuffer text) throws Exception {

    text.append("[");
    text.append(localModeL.leftSide(traiN)+":");
    for (int i = 0; i < sonS.length; i++) {
      if (i > 0) {
	text.append(",");
      }
      text.append(localModeL.rightSide(i, traiN));
    }
    for (int i = 0; i < sonS.length; i++){
      if (sonS[i].isLeaF){
	text.append("[");
	text.append(localModeL.dumpLabel(i,traiN));
	text.append("]");
      } else {
	sonS[i].prefixTree(text);
      }
    }
    text.append("]");
  }

  /**
   * Help method for computing class probabilities of 
   * a given instance.
   * @exception Exception if something goes wrong
   */

  private double getProbs(int classIndex, Instance instance, 
			  double weight) throws Exception {
    
    double [] weights;
    double prob = 0;
    int treeIndex;
    int i,j;
    
    if (isLeaF){
      return weight*localModel().classProb(classIndex,instance);
    }else{
      treeIndex = localModel().whichSubset(instance);
      if (treeIndex == -1){
	weights = localModel().weights(instance);
	for (i=0;i<sonS.length;i++)
	  if (!son(i).isEmptY){
	    prob += son(i).getProbs(classIndex, instance, weights[i]*weight);}
	return prob;
      }else
	if (son(treeIndex).isEmptY)
	  return weight*localModel().classProb(classIndex, instance);
	else
	  return son(treeIndex).getProbs(classIndex, instance, weight);
    }
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

  private ClassifierTree son(int index){
    
    return (ClassifierTree)sonS[index];
  }
}




