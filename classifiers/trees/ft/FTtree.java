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
 *    FTNode.java
 *    Copyright (C) 2007 University of Porto, Porto, Portugal
 *
 */

package weka.classifiers.trees.ft;

import weka.classifiers.Evaluation;
import weka.classifiers.functions.SimpleLinearRegression;
import weka.classifiers.trees.j48.BinC45ModelSelection;
import weka.classifiers.trees.j48.BinC45Split;
import weka.classifiers.trees.j48.ClassifierSplitModel;
import weka.classifiers.trees.j48.ModelSelection;
import weka.classifiers.trees.j48.C45Split;
import weka.classifiers.trees.j48.Distribution;
import weka.classifiers.trees.j48.NoSplit;
import weka.classifiers.trees.j48.Stats;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.SupervisedFilter;
import weka.filters.supervised.attribute.NominalToBinary;
import weka.classifiers.trees.lmt.LogisticBase;

import java.util.Collections;
import java.util.Vector;

/**
 * Abstract class for Functional tree structure. 
 * 
 * @author Jo\~{a}o Gama
 * @author Carlos Ferreira
 *
 * @version $Revision: 1.3 $
 */
public abstract class FTtree 
    extends LogisticBase {   
  
  /** for serialization */
  static final long serialVersionUID = 1862737145870398755L;
    
  /** Total number of training instances. */
  protected double m_totalInstanceWeight;
    
  /** Node id*/
  protected int m_id;
    
  /** ID of logistic model at leaf*/
  protected int m_leafModelNum;
 
  /**minimum number of instances at which a node is considered for splitting*/
  protected int m_minNumInstances;

  /**ModelSelection object (for splitting)*/
  protected ModelSelection m_modelSelection;     

  /**Filter to convert nominal attributes to binary*/
  protected NominalToBinary m_nominalToBinary;  
   
  /**Simple regression functions fit by LogitBoost at higher levels in the tree*/
  protected SimpleLinearRegression[][] m_higherRegressions;
    
  /**Number of simple regression functions fit by LogitBoost at higher levels in the tree*/
  protected int m_numHigherRegressions = 0;
    
  /**Number of instances at the node*/
  protected int m_numInstances;   

  /**The ClassifierSplitModel (for splitting)*/
  protected ClassifierSplitModel m_localModel; 
    
  /**Auxiliary copy ClassifierSplitModel (for splitting)*/
  protected ClassifierSplitModel m_auxLocalModel; 
 
  /**Array of children of the node*/
  protected FTtree[] m_sons; 
   
  /** Stores leaf class value */ 
  protected int m_leafclass;
    
  /**True if node is leaf*/
  protected boolean m_isLeaf;
    
  /**True if node has or splits on constructor */
  protected boolean m_hasConstr=true;
    
  /** Constructor error */
  protected double  m_constError=0;
    
  /** Confidence level */
  protected float m_CF = 0.10f;  
                       
  /**
   * Method for building a Functional Tree (only called for the root node).
   * Grows an initial Functional Tree.
   *
   * @param data the data to train with
   * @throws Exception if something goes wrong
   */
  public abstract void buildClassifier(Instances data) throws Exception;

  /**
   * Abstract method for building the tree structure.
   * Builds a logistic model, splits the node and recursively builds tree for child nodes.
   * @param data the training data passed on to this node
   * @param higherRegressions An array of regression functions produced by LogitBoost at higher 
   * levels in the tree. They represent a logistic regression model that is refined locally 
   * at this node.
   * @param totalInstanceWeight the total number of training examples
   * @param higherNumParameters effective number of parameters in the logistic regression model built
   * in parent nodes
   * @throws Exception if something goes wrong
   */
  public abstract void buildTree(Instances data, SimpleLinearRegression[][] higherRegressions, 
                                 double totalInstanceWeight, double higherNumParameters) throws Exception;
    
  /**
   * Abstract Method that prunes a tree using C4.5 pruning procedure.
   *
   * @exception Exception if something goes wrong
   */
  public abstract double prune() throws Exception; 
 
  /** Inserts new attributes in current dataset or instance 
   *
   * @exception Exception if something goes wrong
   */
  protected Instances insertNewAttr(Instances data) throws Exception{
    
    int i;
    for (i=0; i<data.classAttribute().numValues(); i++)
      {
        data.insertAttributeAt( new Attribute("N"+ i), i); 
      }
    return data;
  }

  /** Removes extended attributes in current dataset or instance 
   *
   * @exception Exception if something goes wrong
   */
  protected Instances removeExtAttributes(Instances  data) throws Exception{
    
    for (int i=0; i< data.classAttribute().numValues(); i++)
      {
        data.deleteAttributeAt(0);
      }
    return data;
  }

  /**
   * Computes estimated errors for tree.
   */
  protected double getEstimatedErrors(){

    double errors = 0;
    int i;

    if (m_isLeaf)
      return getEstimatedErrorsForDistribution(m_localModel.distribution());
    else{
      for (i=0;i<m_sons.length;i++)
        errors = errors+ m_sons[i].getEstimatedErrors();

      return errors;
    }
  }

  /**
   * Computes estimated errors for one branch.
   *
   * @exception Exception if something goes wrong
   */
  protected double getEstimatedErrorsForBranch(Instances data)
    throws Exception {

    Instances [] localInstances;
    double errors = 0;
    int i;

    if (m_isLeaf)
      return getEstimatedErrorsForDistribution(new Distribution(data));
    else{
      Distribution savedDist = m_localModel.distribution();
      m_localModel.resetDistribution(data);
      localInstances = (Instances[])m_localModel.split(data);
      //m_localModel.m_distribution=savedDist;
      for (i=0;i<m_sons.length;i++)
        errors = errors+
          m_sons[i].getEstimatedErrorsForBranch(localInstances[i]);
      return errors;
    }
  }

  /**
   * Computes estimated errors for leaf.
   */
  protected double getEstimatedErrorsForDistribution(Distribution
                                                     theDistribution){
    double numInc;
    double numTotal;
    if (Utils.eq(theDistribution.total(),0))
      return 0;
    else// stats.addErrs returns p - numberofincorrect.=p
      {
        numInc=theDistribution.numIncorrect();
        numTotal=theDistribution.total();
        return ((Stats.addErrs(numTotal, numInc,m_CF)) + numInc)/numTotal;
      }

  }

  /**
   * Computes estimated errors for Constructor Model.
   */
  protected double getEtimateConstModel(Distribution theDistribution){
    double numInc;
    double numTotal;
    if (Utils.eq(theDistribution.total(),0))
      return 0;
    else// stats.addErrs returns p - numberofincorrect.=p
      {
        numTotal=theDistribution.total();
        return ((Stats.addErrs(numTotal,m_constError,m_CF)) + m_constError)/numTotal;
      }
  }
    

  /**
   * Method to count the number of inner nodes in the tree
   * @return the number of inner nodes
   */
  public int getNumInnerNodes(){
    if (m_isLeaf) return 0;
    int numNodes = 1;
    for (int i = 0; i < m_sons.length; i++) numNodes += m_sons[i].getNumInnerNodes();
    return numNodes;
  }

  /**
   * Returns the number of leaves in the tree.
   * Leaves are only counted if their logistic model has changed compared to the one of the parent node.
   * @return the number of leaves
   */
  public int getNumLeaves(){
    int numLeaves;
    if (!m_isLeaf) {
      numLeaves = 0;
      int numEmptyLeaves = 0;
      for (int i = 0; i < m_sons.length; i++) {
        numLeaves += m_sons[i].getNumLeaves();
        if (m_sons[i].m_isLeaf && !m_sons[i].hasModels()) numEmptyLeaves++;
      }
      if (numEmptyLeaves > 1) {
        numLeaves -= (numEmptyLeaves - 1);
      }
    } else {
      numLeaves = 1;
    }	   
    return numLeaves;	
  }


     
  /**
   * Merges two arrays of regression functions into one
   * @param a1 one array
   * @param a2 the other array
   *
   * @return an array that contains all entries from both input arrays
   */
  protected SimpleLinearRegression[][] mergeArrays(SimpleLinearRegression[][] a1,	
                                                   SimpleLinearRegression[][] a2){
    int numModels1 = a1[0].length;
    int numModels2 = a2[0].length;		
	
    SimpleLinearRegression[][] result =
      new SimpleLinearRegression[m_numClasses][numModels1 + numModels2];
	
    for (int i = 0; i < m_numClasses; i++)
      for (int j = 0; j < numModels1; j++) {
        result[i][j]  = a1[i][j];
      }
    for (int i = 0; i < m_numClasses; i++)
      for (int j = 0; j < numModels2; j++) result[i][j+numModels1] = a2[i][j];
    return result;
  }

  /**
   * Return a list of all inner nodes in the tree
   * @return the list of nodes
   */
  public Vector getNodes(){
    Vector nodeList = new Vector();
    getNodes(nodeList);
    return nodeList;
  }

  /**
   * Fills a list with all inner nodes in the tree
   * 
   * @param nodeList the list to be filled
   */
  public void getNodes(Vector nodeList) {
    if (!m_isLeaf) {
      nodeList.add(this);
      for (int i = 0; i < m_sons.length; i++) m_sons[i].getNodes(nodeList);
    }	
  }
    
  /**
   * Returns a numeric version of a set of instances.
   * All nominal attributes are replaced by binary ones, and the class variable is replaced
   * by a pseudo-class variable that is used by LogitBoost.
   */
  protected Instances getNumericData(Instances train) throws Exception{
	
    Instances filteredData = new Instances(train);	
    m_nominalToBinary = new NominalToBinary();			
    m_nominalToBinary.setInputFormat(filteredData);
    filteredData = Filter.useFilter(filteredData, m_nominalToBinary);	

    return super.getNumericData(filteredData);
  }

  /**
   * Computes the F-values of LogitBoost for an instance from the current logistic model at the node
   * Note that this also takes into account the (partial) logistic model fit at higher levels in 
   * the tree.
   * @param instance the instance
   * @return the array of F-values 
   */
  protected double[] getFs(Instance instance) throws Exception{
	
    double [] pred = new double [m_numClasses];
	
    //Need to take into account partial model fit at higher levels in the tree (m_higherRegressions) 
    //and the part of the model fit at this node (m_regressions).

    //Fs from m_regressions (use method of LogisticBase)
    double [] instanceFs = super.getFs(instance);		

    //Fs from m_higherRegressions
    for (int i = 0; i < m_numHigherRegressions; i++) {
      double predSum = 0;
      for (int j = 0; j < m_numClasses; j++) {
        pred[j] = m_higherRegressions[j][i].classifyInstance(instance);
        predSum += pred[j];
      }
      predSum /= m_numClasses;
      for (int j = 0; j < m_numClasses; j++) {
        instanceFs[j] += (pred[j] - predSum) * (m_numClasses - 1) 
          / m_numClasses;
      }
    }
    return instanceFs; 
  }
     
  /**
   *
   * @param <any> probsConst
   */
  public int getConstError(double[] probsConst)
  {
    return Utils.maxIndex(probsConst);
  }
    
  /**
   *Returns true if the logistic regression model at this node has changed compared to the
   *one at the parent node.
   *@return whether it has changed
   */
  public boolean hasModels() {
    return (m_numRegressions > 0);
  }

  /**
   * Returns the class probabilities for an instance according to the logistic model at the node.
   * @param instance the instance
   * @return the array of probabilities
   */
  public double[] modelDistributionForInstance(Instance instance) throws Exception {
	
    //make copy and convert nominal attributes
    instance = (Instance)instance.copy();		
    m_nominalToBinary.input(instance);
    instance = m_nominalToBinary.output();	
	
    //set numeric pseudo-class
    instance.setDataset(m_numericDataHeader);		
	
    return probs(getFs(instance));
  }

  /**
   * Returns the class probabilities for an instance given by the Functional tree.
   * @param instance the instance
   * @return the array of probabilities
   */
  public abstract double[] distributionForInstance(Instance instance) throws Exception;
  
  
    
  /**
   * Returns a description of the Functional tree (tree structure and logistic models)
   * @return describing string
   */
  public String toString(){	
    //assign numbers to logistic regression functions at leaves
    assignLeafModelNumbers(0);	
    try{
      StringBuffer text = new StringBuffer();
	    
      if (m_isLeaf && !m_hasConstr) {
        text.append(": ");
        text.append("Class"+"="+ m_leafclass);
        //text.append("FT_"+m_leafModelNum+":"+getModelParameters());
      } else {
                
        if (m_isLeaf && m_hasConstr) {
          text.append(": ");
          text.append("FT_"+m_leafModelNum+":"+getModelParameters());
                    
        } else {
          dumpTree(0,text);  
        }	    	    
      }
      text.append("\n\nNumber of Leaves  : \t"+numLeaves()+"\n");
      text.append("\nSize of the Tree : \t"+numNodes()+"\n");	
	        
      //This prints logistic models after the tree, comment out if only tree should be printed
      text.append(modelsToString());
      return text.toString();
    } catch (Exception e){
      return "Can't print logistic model tree";
    }
  }
    
  /**
   * Returns the number of leaves (normal count).
   * @return the number of leaves
   */
  public int numLeaves() {	
    if (m_isLeaf) return 1;	
    int numLeaves = 0;
    for (int i = 0; i < m_sons.length; i++) numLeaves += m_sons[i].numLeaves();
    return numLeaves;
  }
    
  /**
   * Returns the number of nodes.
   * @return the number of nodes
   */
  public int numNodes() {
    if (m_isLeaf) return 1;	
    int numNodes = 1;
    for (int i = 0; i < m_sons.length; i++) numNodes += m_sons[i].numNodes();
    return numNodes;
  }
   
  /**
   * Returns a string describing the number of LogitBoost iterations performed at this node, the total number
   * of LogitBoost iterations performed (including iterations at higher levels in the tree), and the number
   * of training examples at this node.
   * @return the describing string
   */
  public String getModelParameters(){
	
    StringBuffer text = new StringBuffer();
    int numModels = m_numRegressions+m_numHigherRegressions;
    text.append(m_numRegressions+"/"+numModels+" ("+m_numInstances+")");
    return text.toString();
  }
       
  /**
   * Help method for printing tree structure.
   *
   * @throws Exception if something goes wrong
   */
  protected void dumpTree(int depth,StringBuffer text) 
    throws Exception {
	
    for (int i = 0; i < m_sons.length; i++) {
      text.append("\n");
      for (int j = 0; j < depth; j++)
        text.append("|   ");
      if(m_hasConstr)
        text.append(m_localModel.leftSide(m_train)+ "#" + m_id);
      else 
        text.append(m_localModel.leftSide(m_train)); 
      text.append(m_localModel.rightSide(i, m_train) );
      if (m_sons[i].m_isLeaf && m_sons[i].m_hasConstr ) {
        text.append(": ");
        text.append("FT_"+m_sons[i].m_leafModelNum+":"+m_sons[i].getModelParameters());
      }else {                
        if(m_sons[i].m_isLeaf && !m_sons[i].m_hasConstr)
          {
            text.append(": ");
            text.append("Class"+"="+ m_sons[i].m_leafclass);  
          }
        else{
            
          m_sons[i].dumpTree(depth+1,text);
        }
      }
    }
  }

  /**
   * Assigns unique IDs to all nodes in the tree
   */
  public int assignIDs(int lastID) {
	
    int currLastID = lastID + 1;
	
    m_id = currLastID;
    if (m_sons != null) {
      for (int i = 0; i < m_sons.length; i++) {
        currLastID = m_sons[i].assignIDs(currLastID);
      }
    }
    return currLastID;
  }
    
  /**
   * Assigns numbers to the logistic regression models at the leaves of the tree
   */
  public int assignLeafModelNumbers(int leafCounter) {
    if (!m_isLeaf) {
      m_leafModelNum = 0;
      for (int i = 0; i < m_sons.length; i++){
        leafCounter = m_sons[i].assignLeafModelNumbers(leafCounter);
      }
    } else {
      leafCounter++;
      m_leafModelNum = leafCounter;
    } 
    return leafCounter;
  }

  /**
   * Returns an array containing the coefficients of the logistic regression function at this node.
   * @return the array of coefficients, first dimension is the class, second the attribute. 
   */
  protected double[][] getCoefficients(){
       
    //Need to take into account partial model fit at higher levels in the tree (m_higherRegressions) 
    //and the part of the model fit at this node (m_regressions).
	
    //get coefficients from m_regressions: use method of LogisticBase
    double[][] coefficients = super.getCoefficients();
    //get coefficients from m_higherRegressions:
    double constFactor = (double)(m_numClasses - 1) / (double)m_numClasses; // (J - 1)/J
    for (int j = 0; j < m_numClasses; j++) {
      for (int i = 0; i < m_numHigherRegressions; i++) {		
        double slope = m_higherRegressions[j][i].getSlope();
        double intercept = m_higherRegressions[j][i].getIntercept();
        int attribute = m_higherRegressions[j][i].getAttributeIndex();
        coefficients[j][0] += constFactor * intercept;
        coefficients[j][attribute + 1] += constFactor * slope;
      }
    }

    return coefficients;
  }
    
  /**
   * Returns a string describing the logistic regression function at the node.
   */
  public String modelsToString(){
	
    StringBuffer text = new StringBuffer();
    if (m_isLeaf && m_hasConstr) {
      text.append("FT_"+m_leafModelNum+":"+super.toString());
            
    }else{
      if (!m_isLeaf && m_hasConstr) {
        if (m_modelSelection instanceof BinC45ModelSelection){
          text.append("FT_N"+((BinC45Split)m_localModel).attIndex()+"#"+m_id +":"+super.toString()); 
        }else{
          text.append("FT_N"+((C45Split)m_localModel).attIndex()+"#"+m_id +":"+super.toString());
        }
        for (int i = 0; i < m_sons.length; i++) { 
          text.append("\n"+ m_sons[i].modelsToString());
        }
      }else{
        if (!m_isLeaf && !m_hasConstr) 
          {
            for (int i = 0; i < m_sons.length; i++) { 
              text.append("\n"+ m_sons[i].modelsToString());
            }
          }else{
          if (m_isLeaf && !m_hasConstr)
            {
              text.append("");
            }
        }
                
      }
    }
        
    return text.toString();
  }

  /**
   * Returns graph describing the tree.
   *
   * @throws Exception if something goes wrong
   */
  public String graph() throws Exception {
	
    StringBuffer text = new StringBuffer();
	
    assignIDs(-1);
    assignLeafModelNumbers(0);
    text.append("digraph FTree {\n");
    if (m_isLeaf && m_hasConstr) {
      text.append("N" + m_id + " [label=\"FT_"+m_leafModelNum+":"+getModelParameters()+"\" " + 
                  "shape=box style=filled");
      text.append("]\n");
    }else{
      if (m_isLeaf && !m_hasConstr){
        text.append("N" + m_id + " [label=\"Class="+m_leafclass+ "\" " + 
                    "shape=box style=filled");
        text.append("]\n");
             
      }else {
        text.append("N" + m_id 
                    + " [label=\"" + 
                    m_localModel.leftSide(m_train) + "\" ");
        text.append("]\n");
        graphTree(text);
      }
    }
    return text.toString() +"}\n";
  }

  /**
   * Helper function for graph description of tree
   *
   * @throws Exception if something goes wrong
   */
  protected void graphTree(StringBuffer text) throws Exception {
	
    for (int i = 0; i < m_sons.length; i++) {
      text.append("N" + m_id  
                  + "->" + 
                  "N" + m_sons[i].m_id +
                  " [label=\"" + m_localModel.rightSide(i,m_train).trim() + 
                  "\"]\n");
      if (m_sons[i].m_isLeaf && m_sons[i].m_hasConstr) {
        text.append("N" +m_sons[i].m_id + " [label=\"FT_"+m_sons[i].m_leafModelNum+":"+
                    m_sons[i].getModelParameters()+"\" " + "shape=box style=filled");
        text.append("]\n");
      } else { 
        if (m_sons[i].m_isLeaf && !m_sons[i].m_hasConstr) {
          text.append("N" +m_sons[i].m_id + " [label=\"Class="+m_sons[i].m_leafclass+"\" " + "shape=box style=filled");
          text.append("]\n");
        }else{
          text.append("N" + m_sons[i].m_id +
                      " [label=\""+m_sons[i].m_localModel.leftSide(m_train) + 
                      "\" ");
          text.append("]\n");
          m_sons[i].graphTree(text);
        }
      }
    } 
  }  

  /**
   * Cleanup in order to save memory.
   */
  public void cleanup() {
    super.cleanup();
    if (!m_isLeaf) {
      for (int i = 0; i < m_sons.length; i++) m_sons[i].cleanup();
    }
  }
}
