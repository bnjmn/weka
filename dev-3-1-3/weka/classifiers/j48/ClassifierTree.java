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
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.6 $
 */
public class ClassifierTree implements Drawable, Serializable {

  /** The model selection method. */  
  protected ModelSelection m_toSelectModel;     

  /** Local model at node. */
  protected ClassifierSplitModel m_localModel;  

  /** References to sons. */
  protected ClassifierTree [] m_sons;           

  /** True if node is leaf. */
  protected boolean m_isLeaf;                   

  /** True if node is empty. */
  protected boolean m_isEmpty;                  

  /** The training instances. */
  protected Instances m_train;                  

  /** The pruning instances. */
  protected Distribution m_test;                

  /**
   * Constructor. 
   */
  public ClassifierTree(ModelSelection toSelectLocModel) {
    
    m_toSelectModel = toSelectLocModel;
  }

  /**
   * Method for building a classifier tree.
   *
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
   * Builds the tree structure.
   *
   * @param data the data for which the tree structure is to be
   * generated.
   * @param keepData is training data to be kept?
   * @exception Exception if something goes wrong
   */
  public void buildTree(Instances data, boolean keepData) throws Exception{
    
    Instances [] localInstances;

    if (keepData) {
      m_train = data;
    }
    m_test = null;
    m_isLeaf = false;
    m_isEmpty = false;
    m_sons = null;
    m_localModel = m_toSelectModel.selectModel(data);
    if (m_localModel.numSubsets() > 1) {
      localInstances = m_localModel.split(data);
      data = null;
      m_sons = new ClassifierTree [m_localModel.numSubsets()];
      for (int i = 0; i < m_sons.length; i++) {
	m_sons[i] = getNewTree(localInstances[i]);
	localInstances[i] = null;
      }
    }else{
      m_isLeaf = true;
      if (Utils.eq(data.sumOfWeights(), 0))
	m_isEmpty = true;
      data = null;
    }
  }

  /**
   * Builds the tree structure with hold out set
   *
   * @param train the data for which the tree structure is to be
   * generated.
   * @param test the test data for potential pruning
   * @param keepData is training Data to be kept?
   * @exception Exception if something goes wrong
   */
  public void buildTree(Instances train, Instances test, boolean keepData)
       throws Exception{
    
    Instances [] localTrain, localTest;
    int i;
    
    if (keepData) {
      m_train = train;
    }
    m_isLeaf = false;
    m_isEmpty = false;
    m_sons = null;
    m_localModel = m_toSelectModel.selectModel(train, test);
    m_test = new Distribution(test, m_localModel);
    if (m_localModel.numSubsets() > 1) {
      localTrain = m_localModel.split(train);
      localTest = m_localModel.split(test);
      train = test = null;
      m_sons = new ClassifierTree [m_localModel.numSubsets()];
      for (i=0;i<m_sons.length;i++) {
	m_sons[i] = getNewTree(localTrain[i], localTest[i]);
	localTrain[i] = null;
	localTest[i] = null;
      }
    }else{
      m_isLeaf = true;
      if (Utils.eq(train.sumOfWeights(), 0))
	m_isEmpty = true;
      train = test = null;
    }
  }

  /** 
   * Classifies an instance.
   *
   * @exception Exception if something goes wrong
   */
  public double classifyInstance(Instance instance) throws Exception {

    double maxProb = -1;
    double currentProb;
    int maxIndex = 0;
    int j;

    for (j = 0; j < instance.numClasses(); 
	 j++) {
      currentProb = getProbs(j,instance,1);
      if (Utils.gr(currentProb,maxProb)) {
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

    m_train = justHeaderInfo;
    m_test = null;
    if (!m_isLeaf)
      for (int i = 0; i < m_sons.length; i++)
	m_sons[i].cleanup(justHeaderInfo);
  }

  /** 
   * Returns class probabilities for a weighted instance.
   *
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
   *
   * @exception Exception if something goes wrong
   */
  public String graph() throws Exception {

    StringBuffer text = new StringBuffer();

    text.append("digraph J48Tree {\n" +
		"node [fontsize=10]\n" +
		"edge [fontsize=10 style=bold]\n");
    if (m_isLeaf) {
      text.append("N" + Integer.toHexString(m_localModel.hashCode())
		  + " [label=\"" + 
		  m_localModel.dumpLabel(0,m_train) + "\" "+ 
		  "shape=box style=filled color=gray95]\n");
    }else {
      text.append("N" + Integer.toHexString(m_localModel.hashCode())
		  + " [label=\"" + 
		  m_localModel.leftSide(m_train) + "\"]\n");
      graphTree(text);
    }
    
    return text.toString() +"}\n";
  }

  /**
   * Returns tree in prefix order.
   *
   * @exception Exception if something goes wrong
   */
  public String prefix() throws Exception {
    
    StringBuffer text;

    text = new StringBuffer();
    if (m_isLeaf) {
      text.append("["+m_localModel.dumpLabel(0,m_train)+"]");
    }else {
      prefixTree(text);
    }
    
    return text.toString();
  }

  /**
   * Returns source code for the tree as an if-then statement. The 
   * class is assigned to variable "p", and assumes the tested 
   * instance is named "i". The results are returned as two stringbuffers: 
   * a section of code for assignment of the class, and a section of
   * code containing support code (eg: other support methods).
   *
   * @param className the classname that this static classifier has
   * @return an array containing two stringbuffers, the first string containing
   * assignment code, and the second containing source for support code.
   * @exception Exception if something goes wrong
   */
  public StringBuffer [] toSource(String className) throws Exception {
    
    StringBuffer [] result = new StringBuffer [2];
    if (m_isLeaf) {
      result[0] = new StringBuffer("    p = " 
	+ m_localModel.distribution().maxClass(0) + ";\n");
      result[1] = new StringBuffer("");
    } else {
      StringBuffer text = new StringBuffer();
      String nextIndent = "      ";
      StringBuffer atEnd = new StringBuffer();

      text.append("  static double N") 
	.append(Integer.toHexString(m_localModel.hashCode()))
	.append("(Object []i) {\n")
	.append("    double p = Double.NaN;\n");

      text.append("    if (")
	.append(m_localModel.sourceExpression(-1, m_train))
	.append(") {\n");
      text.append("      p = ")
	.append(m_localModel.distribution().maxClass(0))
	.append(";\n");
      text.append("    } ");
      for (int i = 0; i < m_sons.length; i++) {
	text.append("else if (" + m_localModel.sourceExpression(i, m_train) 
		    + ") {\n");
	if (m_sons[i].m_isLeaf) {
	  text.append("      p = " 
		      + m_localModel.distribution().maxClass(i) + ";\n");
	} else {
	  StringBuffer [] sub = m_sons[i].toSource(className);
	  text.append(sub[0]);
	  atEnd.append(sub[1]);
	}
	text.append("    } ");
	if (i == m_sons.length - 1) {
	  text.append('\n');
	}
      }

      text.append("    return p;\n  }\n");

      result[0] = new StringBuffer("    p = " + className + ".N");
      result[0].append(Integer.toHexString(m_localModel.hashCode()))
	.append("(i);\n");
      result[1] = text.append(atEnd);
    }
    return result;
  }

  /**
   * Returns number of leaves in tree structure.
   */
  public int numLeaves() {
    
    int num = 0;
    int i;
    
    if (m_isLeaf)
      return 1;
    else
      for (i=0;i<m_sons.length;i++)
	num = num+m_sons[i].numLeaves();
        
    return num;
  }

  /**
   * Returns number of nodes in tree structure.
   */
  public int numNodes() {
    
    int no = 1;
    int i;
    
    if (!m_isLeaf)
      for (i=0;i<m_sons.length;i++)
	no = no+m_sons[i].numNodes();
    
    return no;
  }

  /**
   * Prints tree structure.
   */
  public String toString() {

    try {
      StringBuffer text = new StringBuffer();
      
      if (m_isLeaf) {
	text.append(": ");
	text.append(m_localModel.dumpLabel(0,m_train));
      }else
	dumpTree(0,text);
      text.append("\n\nNumber of Leaves  : \t"+numLeaves()+"\n");
      text.append("\nSize of the tree : \t"+numNodes()+"\n");
 
      return text.toString();
    } catch (Exception e) {
      return "Can't print classification tree.";
    }
  }

  /**
   * Returns a newly created tree.
   *
   * @param data the training data
   * @exception Exception if something goes wrong
   */
  protected ClassifierTree getNewTree(Instances data) throws Exception{
	 
    ClassifierTree newTree = new ClassifierTree(m_toSelectModel);
    newTree.buildTree(data, false);
    
    return newTree;
  }

  /**
   * Returns a newly created tree.
   *
   * @param data the training data
   * @param test the pruning data.
   * @exception Exception if something goes wrong
   */
  protected ClassifierTree getNewTree(Instances train, Instances test) 
       throws Exception{
	 
    ClassifierTree newTree = new ClassifierTree(m_toSelectModel);
    newTree.buildTree(train, test, false);
    
    return newTree;
  }

  /**
   * Help method for printing tree structure.
   *
   * @exception Exception if something goes wrong
   */
  private void dumpTree(int depth,StringBuffer text) 
       throws Exception {
    
    int i,j;
    
    for (i=0;i<m_sons.length;i++) {
      text.append("\n");;
      for (j=0;j<depth;j++)
	text.append("|   ");
      text.append(m_localModel.leftSide(m_train));
      text.append(m_localModel.rightSide(i, m_train));
      if (m_sons[i].m_isLeaf) {
	text.append(": ");
	text.append(m_localModel.dumpLabel(i,m_train));
      }else
	m_sons[i].dumpTree(depth+1,text);
    }
  }

  /**
   * Help method for printing tree structure as a graph.
   *
   * @exception Exception if something goes wrong
   */
  private void graphTree(StringBuffer text) throws Exception {
    
    for (int i = 0; i < m_sons.length; i++) {
      text.append("N" + Integer.toHexString(m_localModel.hashCode()) 
		  + "->" + 
		  "N" + Integer.toHexString(m_sons[i].m_localModel.hashCode())  +
		  " [label=\"" + m_localModel.rightSide(i,m_train).trim() + 
		  "\"]\n");
      if (m_sons[i].m_isLeaf) {
	text.append("N" + Integer.toHexString(m_sons[i].m_localModel.hashCode()) +
		    " [label=\""+m_localModel.dumpLabel(i,m_train)+"\" "+ 
		    "shape=box style=filled color=gray95]\n");
      } else {
	text.append("N" + Integer.toHexString(m_sons[i].m_localModel.hashCode()) +
		    " [label=\""+m_sons[i].m_localModel.leftSide(m_train)+"\"]\n");
	m_sons[i].graphTree(text);
      }
    }
  }

  /**
   * Prints the tree in prefix form
   */
  private void prefixTree(StringBuffer text) throws Exception {

    text.append("[");
    text.append(m_localModel.leftSide(m_train)+":");
    for (int i = 0; i < m_sons.length; i++) {
      if (i > 0) {
	text.append(",");
      }
      text.append(m_localModel.rightSide(i, m_train));
    }
    for (int i = 0; i < m_sons.length; i++) {
      if (m_sons[i].m_isLeaf) {
	text.append("[");
	text.append(m_localModel.dumpLabel(i,m_train));
	text.append("]");
      } else {
	m_sons[i].prefixTree(text);
      }
    }
    text.append("]");
  }

  /**
   * Help method for computing class probabilities of 
   * a given instance.
   *
   * @exception Exception if something goes wrong
   */
  private double getProbs(int classIndex, Instance instance, 
			  double weight) throws Exception {
    
    double [] weights;
    double prob = 0;
    int treeIndex;
    int i,j;
    
    if (m_isLeaf) {
      return weight*localModel().classProb(classIndex,instance);
    }else{
      treeIndex = localModel().whichSubset(instance);
      if (treeIndex == -1) {
	weights = localModel().weights(instance);
	for (i=0;i<m_sons.length;i++)
	  if (!son(i).m_isEmpty) {
	    prob += son(i).getProbs(classIndex, instance, weights[i]*weight);}
	return prob;
      }else
	if (son(treeIndex).m_isEmpty)
	  return weight*localModel().classProb(classIndex, instance);
	else
	  return son(treeIndex).getProbs(classIndex, instance, weight);
    }
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
  private ClassifierTree son(int index) {
    
    return (ClassifierTree)m_sons[index];
  }
}




