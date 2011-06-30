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
 *    FTLeavesNode.java
 *    Copyright (C) 2007 University of Porto, Porto, Portugal
 *
 */

package weka.classifiers.trees.ft;

import weka.classifiers.functions.SimpleLinearRegression;
import weka.classifiers.trees.j48.C45ModelSelection;
import weka.classifiers.trees.j48.NoSplit;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.RevisionUtils;
import weka.core.Utils;

/**
 * Class for Functional Leaves tree version. 
 * 
 * @author Jo\~{a}o Gama
 * @author Carlos Ferreira
 *
 * @version $Revision$
 */
public class FTLeavesNode 
  extends FTtree {   

  /** for serialization. */
  private static final long serialVersionUID = 950601378326259315L;

  /**
   * Constructor for Functional Leaves tree node. 
   *
   * @param errorOnProbabilities Use error on probabilities for stopping criterion of LogitBoost?
   * @param numBoostingIterations sets the numBoostingIterations parameter
   * @param minNumInstances minimum number of instances at which a node is considered for splitting
   */
  public FTLeavesNode( boolean errorOnProbabilities, int numBoostingIterations, int minNumInstances,
                       double weightTrimBeta, boolean useAIC) {
    m_errorOnProbabilities = errorOnProbabilities;
    m_fixedNumIterations = numBoostingIterations;      
    m_minNumInstances = minNumInstances;
    m_maxIterations = 200;
    setWeightTrimBeta(weightTrimBeta);
    setUseAIC(useAIC);
  }         
    
  /**
   * Method for building a Functional Leaves tree (only called for the root node).
   * Grows an initial Functional Tree.
   *
   * @param data the data to train with
   * @throws Exception if something goes wrong
   */
  public void buildClassifier(Instances data) throws Exception{
	
    buildTree(data, null, data.numInstances(), 0);
	
  }

  /**
   * Method for building the tree structure.
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
  public void buildTree(Instances data, SimpleLinearRegression[][] higherRegressions, 
                        double totalInstanceWeight, double higherNumParameters) throws Exception{

    //save some stuff
    m_totalInstanceWeight = totalInstanceWeight;
    m_train = new Instances(data);
	
        
    m_isLeaf = true;
    m_sons = null;
	
    m_numInstances = m_train.numInstances();
    m_numClasses = m_train.numClasses();				
	
    //init 
    m_numericData = getNumericData(m_train);		  
    m_numericDataHeader = new Instances(m_numericData, 0);
	
    m_regressions = initRegressions();
    m_numRegressions = 0;
	
    if (higherRegressions != null) m_higherRegressions = higherRegressions;
    else m_higherRegressions = new SimpleLinearRegression[m_numClasses][0];	

    m_numHigherRegressions = m_higherRegressions[0].length;	
        
    m_numParameters = higherNumParameters;
        
    //build logistic model
    if (m_numInstances >= m_numFoldsBoosting) {
      if (m_fixedNumIterations > 0){
        performBoosting(m_fixedNumIterations);
      } else if (getUseAIC()) {
        performBoostingInfCriterion();
      } else {
        performBoostingCV();
      }
    }
        
    m_numParameters += m_numRegressions;
	
    //only keep the simple regression functions that correspond to the selected number of LogitBoost iterations
    m_regressions = selectRegressions(m_regressions);
        
    boolean grow;
       
    //Compute logistic probs
    double[][] FsConst;
    double[] probsConst;
    int j;
    FsConst = getFs(m_numericData);
        
    for (j = 0; j < data.numInstances(); j++)
      {
        probsConst=probs(FsConst[j]);
        // Computes constructor error
        if (data.instance(j).classValue()!=getConstError(probsConst)) m_constError=m_constError +1;
      }
        
    //to choose split point on the node data
    m_modelSelection=new  C45ModelSelection(m_minNumInstances, data, true);
    m_localModel = m_modelSelection.selectModel(data);
       
    //split node if more than minNumInstances...
    if (m_numInstances > m_minNumInstances) {
      grow = (m_localModel.numSubsets() > 1);
    } else {
      grow = false;
    }
        
    // logitboost uses distribution for instance
    m_hasConstr=false;
    if (grow) {	
      //create and build children of node
      m_isLeaf = false;
      Instances[] localInstances = m_localModel.split(data);
      m_sons = new FTLeavesNode[m_localModel.numSubsets()];
      
      for (int i = 0; i < m_sons.length; i++) {
        m_sons[i] = new FTLeavesNode(m_errorOnProbabilities, m_fixedNumIterations, 
                                     m_minNumInstances,getWeightTrimBeta(), getUseAIC());
        m_sons[i].buildTree(localInstances[i],
                            mergeArrays(m_regressions, m_higherRegressions), m_totalInstanceWeight, m_numParameters);		
        localInstances[i] = null;
      }	    
    } 
    else{
      m_leafclass=m_localModel.distribution().maxClass();

    }
  }
    
  /**
   * Prunes a tree using C4.5 pruning procedure.
   *
   * @exception Exception if something goes wrong
   */
  public double prune() throws Exception {

    double errorsLeaf;
    double errorsTree;
    double errorsConstModel;
    double treeError=0;
    int i;
    double probBranch;

    // Compute error if this Tree would be leaf without contructor
    errorsLeaf = getEstimatedErrorsForDistribution(m_localModel.distribution());
    if (m_isLeaf ) { 
      return  errorsLeaf;
    } else {
      //Computes da error of the constructor model
      errorsConstModel = getEtimateConstModel(m_localModel.distribution());
      errorsTree=0;
      for (i = 0; i < m_sons.length; i++) {
        probBranch = m_localModel.distribution().perBag(i) /
          m_localModel.distribution().total();
        errorsTree += probBranch* m_sons[i].prune();
      }
      // Decide if leaf is best choice.

      if (Utils.smOrEq(errorsLeaf, errorsTree) && Utils.smOrEq(errorsLeaf, errorsConstModel)) {
        // Free son Trees
        m_sons = null;
        m_isLeaf = true;
        m_hasConstr=false;
        m_leafclass=m_localModel.distribution().maxClass();
        // Get NoSplit Model for node.
        m_localModel = new NoSplit(m_localModel.distribution());
        treeError=errorsLeaf;

      }else{
        // Decide if Constructor is best choice.
        if (Utils.smOrEq(errorsConstModel, errorsTree)) {
          // Free son Trees
          m_sons = null;
          m_isLeaf = true;
          m_hasConstr =true;
          // Get NoSplit Model for node.
          m_localModel = new NoSplit(m_localModel.distribution());
          treeError=errorsConstModel;
        } else
          treeError=errorsTree;
      }
    }
    return  treeError;
  }

  /**
   * Returns the class probabilities for an instance given by the Functional Leaves tree.
   * @param instance the instance
   * @return the array of probabilities
   */
  public double[] distributionForInstance(Instance instance) throws Exception {
    double[] probs;
       
    if (m_isLeaf && m_hasConstr) { //leaf
      //leaf: use majoraty class or constructor model
      probs = modelDistributionForInstance(instance);
    } else {
      if (m_isLeaf && !m_hasConstr)
        {
          probs=new double[instance.numClasses()];
          probs[m_leafclass]=(double)1;   
        }else{
                              
        int branch = m_localModel.whichSubset(instance); //split
        probs = m_sons[branch].distributionForInstance(instance);
      }
    }
    return probs;
	
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }
}
