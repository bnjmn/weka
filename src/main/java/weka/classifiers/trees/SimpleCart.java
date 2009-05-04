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
 * SimpleCart.java
 * Copyright (C) 2007 Haijian Shi
 *
 */

package weka.classifiers.trees;

import weka.classifiers.Evaluation;
import weka.classifiers.RandomizableClassifier;
import weka.core.AdditionalMeasureProducer;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.matrix.Matrix;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * Class implementing minimal cost-complexity pruning.<br/>
 * Note when dealing with missing values, use "fractional instances" method instead of surrogate split method.<br/>
 * <br/>
 * For more information, see:<br/>
 * <br/>
 * Leo Breiman, Jerome H. Friedman, Richard A. Olshen, Charles J. Stone (1984). Classification and Regression Trees. Wadsworth International Group, Belmont, California.
 * <p/>
 <!-- globalinfo-end -->	
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;book{Breiman1984,
 *    address = {Belmont, California},
 *    author = {Leo Breiman and Jerome H. Friedman and Richard A. Olshen and Charles J. Stone},
 *    publisher = {Wadsworth International Group},
 *    title = {Classification and Regression Trees},
 *    year = {1984}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)</pre>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -M &lt;min no&gt;
 *  The minimal number of instances at the terminal nodes.
 *  (default 2)</pre>
 * 
 * <pre> -N &lt;num folds&gt;
 *  The number of folds used in the minimal cost-complexity pruning.
 *  (default 5)</pre>
 * 
 * <pre> -U
 *  Don't use the minimal cost-complexity pruning.
 *  (default yes).</pre>
 * 
 * <pre> -H
 *  Don't use the heuristic method for binary split.
 *  (default true).</pre>
 * 
 * <pre> -A
 *  Use 1 SE rule to make pruning decision.
 *  (default no).</pre>
 * 
 * <pre> -C
 *  Percentage of training data size (0-1].
 *  (default 1).</pre>
 * 
 <!-- options-end -->
 *
 * @author Haijian Shi (hs69@cs.waikato.ac.nz)
 * @version $Revision$
 */
public class SimpleCart
  extends RandomizableClassifier
  implements AdditionalMeasureProducer, TechnicalInformationHandler {

  /** For serialization.	 */
  private static final long serialVersionUID = 4154189200352566053L;

  /** Training data.  */
  protected Instances m_train;

  /** Successor nodes. */
  protected SimpleCart[] m_Successors;

  /** Attribute used to split data. */
  protected Attribute m_Attribute;

  /** Split point for a numeric attribute. */
  protected double m_SplitValue;

  /** Split subset used to split data for nominal attributes. */
  protected String m_SplitString;

  /** Class value if the node is leaf. */
  protected double m_ClassValue;

  /** Class attriubte of data. */
  protected Attribute m_ClassAttribute;

  /** Minimum number of instances in at the terminal nodes. */
  protected double m_minNumObj = 2;

  /** Number of folds for minimal cost-complexity pruning. */
  protected int m_numFoldsPruning = 5;

  /** Alpha-value (for pruning) at the node. */
  protected double m_Alpha;

  /** Number of training examples misclassified by the model (subtree rooted). */
  protected double m_numIncorrectModel;

  /** Number of training examples misclassified by the model (subtree not rooted). */
  protected double m_numIncorrectTree;

  /** Indicate if the node is a leaf node. */
  protected boolean m_isLeaf;

  /** If use minimal cost-compexity pruning. */
  protected boolean m_Prune = true;

  /** Total number of instances used to build the classifier. */
  protected int m_totalTrainInstances;

  /** Proportion for each branch. */
  protected double[] m_Props;

  /** Class probabilities. */
  protected double[] m_ClassProbs = null;

  /** Distributions of leaf node (or temporary leaf node in minimal cost-complexity pruning) */
  protected double[] m_Distribution;

  /** If use huristic search for nominal attributes in multi-class problems (default true). */
  protected boolean m_Heuristic = true;

  /** If use the 1SE rule to make final decision tree. */
  protected boolean m_UseOneSE = false;

  /** Training data size. */
  protected double m_SizePer = 1;

  /**
   * Return a description suitable for displaying in the explorer/experimenter.
   * 
   * @return 		a description suitable for displaying in the 
   * 			explorer/experimenter
   */
  public String globalInfo() {
    return  
        "Class implementing minimal cost-complexity pruning.\n"
      + "Note when dealing with missing values, use \"fractional "
      + "instances\" method instead of surrogate split method.\n\n"
      + "For more information, see:\n\n"
      + getTechnicalInformation().toString();
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing 
   * detailed information about the technical background of this class,
   * e.g., paper reference or book this class is based on.
   * 
   * @return 		the technical information about this class
   */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation 	result;
    
    result = new TechnicalInformation(Type.BOOK);
    result.setValue(Field.AUTHOR, "Leo Breiman and Jerome H. Friedman and Richard A. Olshen and Charles J. Stone");
    result.setValue(Field.YEAR, "1984");
    result.setValue(Field.TITLE, "Classification and Regression Trees");
    result.setValue(Field.PUBLISHER, "Wadsworth International Group");
    result.setValue(Field.ADDRESS, "Belmont, California");
    
    return result;
  }

  /**
   * Returns default capabilities of the classifier.
   * 
   * @return 		the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.NOMINAL_CLASS);

    return result;
  }

  /**
   * Build the classifier.
   * 
   * @param data 	the training instances
   * @throws Exception 	if something goes wrong
   */
  public void buildClassifier(Instances data) throws Exception {

    getCapabilities().testWithFail(data);
    data = new Instances(data);        
    data.deleteWithMissingClass();

    // unpruned CART decision tree
    if (!m_Prune) {

      // calculate sorted indices and weights, and compute initial class counts.
      int[][] sortedIndices = new int[data.numAttributes()][0];
      double[][] weights = new double[data.numAttributes()][0];
      double[] classProbs = new double[data.numClasses()];
      double totalWeight = computeSortedInfo(data,sortedIndices, weights,classProbs);

      makeTree(data, data.numInstances(),sortedIndices,weights,classProbs,
	  totalWeight,m_minNumObj, m_Heuristic);
      return;
    }

    Random random = new Random(m_Seed);
    Instances cvData = new Instances(data);
    cvData.randomize(random);
    cvData = new Instances(cvData,0,(int)(cvData.numInstances()*m_SizePer)-1);
    cvData.stratify(m_numFoldsPruning);

    double[][] alphas = new double[m_numFoldsPruning][];
    double[][] errors = new double[m_numFoldsPruning][];

    // calculate errors and alphas for each fold
    for (int i = 0; i < m_numFoldsPruning; i++) {

      //for every fold, grow tree on training set and fix error on test set.
      Instances train = cvData.trainCV(m_numFoldsPruning, i);
      Instances test = cvData.testCV(m_numFoldsPruning, i);

      // calculate sorted indices and weights, and compute initial class counts for each fold
      int[][] sortedIndices = new int[train.numAttributes()][0];
      double[][] weights = new double[train.numAttributes()][0];
      double[] classProbs = new double[train.numClasses()];
      double totalWeight = computeSortedInfo(train,sortedIndices, weights,classProbs);

      makeTree(train, train.numInstances(),sortedIndices,weights,classProbs,
	  totalWeight,m_minNumObj, m_Heuristic);

      int numNodes = numInnerNodes();
      alphas[i] = new double[numNodes + 2];
      errors[i] = new double[numNodes + 2];

      // prune back and log alpha-values and errors on test set
      prune(alphas[i], errors[i], test);
    }

    // calculate sorted indices and weights, and compute initial class counts on all training instances
    int[][] sortedIndices = new int[data.numAttributes()][0];
    double[][] weights = new double[data.numAttributes()][0];
    double[] classProbs = new double[data.numClasses()];
    double totalWeight = computeSortedInfo(data,sortedIndices, weights,classProbs);

    //build tree using all the data
    makeTree(data, data.numInstances(),sortedIndices,weights,classProbs,
	totalWeight,m_minNumObj, m_Heuristic);

    int numNodes = numInnerNodes();

    double[] treeAlphas = new double[numNodes + 2];

    // prune back and log alpha-values
    int iterations = prune(treeAlphas, null, null);

    double[] treeErrors = new double[numNodes + 2];

    // for each pruned subtree, find the cross-validated error
    for (int i = 0; i <= iterations; i++){
      //compute midpoint alphas
      double alpha = Math.sqrt(treeAlphas[i] * treeAlphas[i+1]);
      double error = 0;
      for (int k = 0; k < m_numFoldsPruning; k++) {
	int l = 0;
	while (alphas[k][l] <= alpha) l++;
	error += errors[k][l - 1];
      }
      treeErrors[i] = error/m_numFoldsPruning;
    }

    // find best alpha
    int best = -1;
    double bestError = Double.MAX_VALUE;
    for (int i = iterations; i >= 0; i--) {
      if (treeErrors[i] < bestError) {
	bestError = treeErrors[i];
	best = i;
      }
    }

    // 1 SE rule to choose expansion
    if (m_UseOneSE) {
      double oneSE = Math.sqrt(bestError*(1-bestError)/(data.numInstances()));
      for (int i = iterations; i >= 0; i--) {
	if (treeErrors[i] <= bestError+oneSE) {
	  best = i;
	  break;
	}
      }
    }

    double bestAlpha = Math.sqrt(treeAlphas[best] * treeAlphas[best + 1]);

    //"unprune" final tree (faster than regrowing it)
    unprune();
    prune(bestAlpha);        
  }

  /**
   * Make binary decision tree recursively.
   * 
   * @param data 		the training instances
   * @param totalInstances 	total number of instances
   * @param sortedIndices 	sorted indices of the instances
   * @param weights 		weights of the instances
   * @param classProbs 		class probabilities
   * @param totalWeight 	total weight of instances
   * @param minNumObj 		minimal number of instances at leaf nodes
   * @param useHeuristic 	if use heuristic search for nominal attributes in multi-class problem
   * @throws Exception 		if something goes wrong
   */
  protected void makeTree(Instances data, int totalInstances, int[][] sortedIndices,
      double[][] weights, double[] classProbs, double totalWeight, double minNumObj,
      boolean useHeuristic) throws Exception{

    // if no instances have reached this node (normally won't happen)
    if (totalWeight == 0){
      m_Attribute = null;
      m_ClassValue = Instance.missingValue();
      m_Distribution = new double[data.numClasses()];
      return;
    }

    m_totalTrainInstances = totalInstances;
    m_isLeaf = true;

    m_ClassProbs = new double[classProbs.length];
    m_Distribution = new double[classProbs.length];
    System.arraycopy(classProbs, 0, m_ClassProbs, 0, classProbs.length);
    System.arraycopy(classProbs, 0, m_Distribution, 0, classProbs.length);
    if (Utils.sum(m_ClassProbs)!=0) Utils.normalize(m_ClassProbs);

    // Compute class distributions and value of splitting
    // criterion for each attribute
    double[][][] dists = new double[data.numAttributes()][0][0];
    double[][] props = new double[data.numAttributes()][0];
    double[][] totalSubsetWeights = new double[data.numAttributes()][2];
    double[] splits = new double[data.numAttributes()];
    String[] splitString = new String[data.numAttributes()];
    double[] giniGains = new double[data.numAttributes()];

    // for each attribute find split information
    for (int i = 0; i < data.numAttributes(); i++) {
      Attribute att = data.attribute(i);
      if (i==data.classIndex()) continue;
      if (att.isNumeric()) {
	// numeric attribute
	splits[i] = numericDistribution(props, dists, att, sortedIndices[i],
	    weights[i], totalSubsetWeights, giniGains, data);
      } else {
	// nominal attribute
	splitString[i] = nominalDistribution(props, dists, att, sortedIndices[i],
	    weights[i], totalSubsetWeights, giniGains, data, useHeuristic);
      }
    }

    // Find best attribute (split with maximum Gini gain)
    int attIndex = Utils.maxIndex(giniGains);
    m_Attribute = data.attribute(attIndex);

    m_train = new Instances(data, sortedIndices[attIndex].length);
    for (int i=0; i<sortedIndices[attIndex].length; i++) {
      Instance inst = data.instance(sortedIndices[attIndex][i]);
      Instance instCopy = (Instance)inst.copy();
      instCopy.setWeight(weights[attIndex][i]);
      m_train.add(instCopy);
    }

    // Check if node does not contain enough instances, or if it can not be split,
    // or if it is pure. If does, make leaf.
    if (totalWeight < 2 * minNumObj || giniGains[attIndex]==0 ||
	props[attIndex][0]==0 || props[attIndex][1]==0) {
      makeLeaf(data);
    }
    
    else {            
      m_Props = props[attIndex];
      int[][][] subsetIndices = new int[2][data.numAttributes()][0];
      double[][][] subsetWeights = new double[2][data.numAttributes()][0];

      // numeric split
      if (m_Attribute.isNumeric()) m_SplitValue = splits[attIndex];

      // nominal split
      else m_SplitString = splitString[attIndex];

      splitData(subsetIndices, subsetWeights, m_Attribute, m_SplitValue,
	  m_SplitString, sortedIndices, weights, data);
      
      // If split of the node results in a node with less than minimal number of isntances, 
      // make the node leaf node.
      if (subsetIndices[0][attIndex].length<minNumObj ||
	  subsetIndices[1][attIndex].length<minNumObj) {
	makeLeaf(data);
	return;
      }

      // Otherwise, split the node.
      m_isLeaf = false;
      m_Successors = new SimpleCart[2];
      for (int i = 0; i < 2; i++) {
	m_Successors[i] = new SimpleCart();
	m_Successors[i].makeTree(data, m_totalTrainInstances, subsetIndices[i],
	    subsetWeights[i],dists[attIndex][i], totalSubsetWeights[attIndex][i],
	    minNumObj, useHeuristic);
      }
    }
  }

  /**
   * Prunes the original tree using the CART pruning scheme, given a 
   * cost-complexity parameter alpha.
   * 
   * @param alpha 	the cost-complexity parameter
   * @throws Exception 	if something goes wrong
   */
  public void prune(double alpha) throws Exception {

    Vector nodeList;

    // determine training error of pruned subtrees (both with and without replacing a subtree),
    // and calculate alpha-values from them
    modelErrors();
    treeErrors();
    calculateAlphas();

    // get list of all inner nodes in the tree
    nodeList = getInnerNodes();

    boolean prune = (nodeList.size() > 0);
    double preAlpha = Double.MAX_VALUE;
    while (prune) {

      // select node with minimum alpha
      SimpleCart nodeToPrune = nodeToPrune(nodeList);

      // want to prune if its alpha is smaller than alpha
      if (nodeToPrune.m_Alpha > alpha) {
	break;
      }

      nodeToPrune.makeLeaf(nodeToPrune.m_train);

      // normally would not happen
      if (nodeToPrune.m_Alpha==preAlpha) {
	nodeToPrune.makeLeaf(nodeToPrune.m_train);
	treeErrors();
	calculateAlphas();
	nodeList = getInnerNodes();
	prune = (nodeList.size() > 0);
	continue;
      }
      preAlpha = nodeToPrune.m_Alpha;

      //update tree errors and alphas
      treeErrors();
      calculateAlphas();

      nodeList = getInnerNodes();
      prune = (nodeList.size() > 0);
    }
  }

  /**
   * Method for performing one fold in the cross-validation of minimal 
   * cost-complexity pruning. Generates a sequence of alpha-values with error 
   * estimates for the corresponding (partially pruned) trees, given the test 
   * set of that fold.
   *
   * @param alphas 	array to hold the generated alpha-values
   * @param errors 	array to hold the corresponding error estimates
   * @param test 	test set of that fold (to obtain error estimates)
   * @return 		the iteration of the pruning
   * @throws Exception 	if something goes wrong
   */
  public int prune(double[] alphas, double[] errors, Instances test) 
    throws Exception {

    Vector nodeList;

    // determine training error of subtrees (both with and without replacing a subtree), 
    // and calculate alpha-values from them
    modelErrors();
    treeErrors();
    calculateAlphas();

    // get list of all inner nodes in the tree
    nodeList = getInnerNodes();

    boolean prune = (nodeList.size() > 0);

    //alpha_0 is always zero (unpruned tree)
    alphas[0] = 0;

    Evaluation eval;

    // error of unpruned tree
    if (errors != null) {
      eval = new Evaluation(test);
      eval.evaluateModel(this, test);
      errors[0] = eval.errorRate();
    }

    int iteration = 0;
    double preAlpha = Double.MAX_VALUE;
    while (prune) {

      iteration++;

      // get node with minimum alpha
      SimpleCart nodeToPrune = nodeToPrune(nodeList);

      // do not set m_sons null, want to unprune
      nodeToPrune.m_isLeaf = true;

      // normally would not happen
      if (nodeToPrune.m_Alpha==preAlpha) {
	iteration--;
	treeErrors();
	calculateAlphas();
	nodeList = getInnerNodes();
	prune = (nodeList.size() > 0);
	continue;
      }

      // get alpha-value of node
      alphas[iteration] = nodeToPrune.m_Alpha;

      // log error
      if (errors != null) {
	eval = new Evaluation(test);
	eval.evaluateModel(this, test);
	errors[iteration] = eval.errorRate();
      }
      preAlpha = nodeToPrune.m_Alpha;

      //update errors/alphas
      treeErrors();
      calculateAlphas();

      nodeList = getInnerNodes();
      prune = (nodeList.size() > 0);
    }

    //set last alpha 1 to indicate end
    alphas[iteration + 1] = 1.0;
    return iteration;
  }

  /**
   * Method to "unprune" the CART tree. Sets all leaf-fields to false.
   * Faster than re-growing the tree because CART do not have to be fit again.
   */
  protected void unprune() {
    if (m_Successors != null) {
      m_isLeaf = false;
      for (int i = 0; i < m_Successors.length; i++) m_Successors[i].unprune();
    }
  }

  /**
   * Compute distributions, proportions and total weights of two successor 
   * nodes for a given numeric attribute.
   * 
   * @param props 		proportions of each two branches for each attribute
   * @param dists 		class distributions of two branches for each attribute
   * @param att 		numeric att split on
   * @param sortedIndices 	sorted indices of instances for the attirubte
   * @param weights 		weights of instances for the attirbute
   * @param subsetWeights 	total weight of two branches split based on the attribute
   * @param giniGains 		Gini gains for each attribute 
   * @param data 		training instances
   * @return 			Gini gain the given numeric attribute
   * @throws Exception 		if something goes wrong
   */
  protected double numericDistribution(double[][] props, double[][][] dists,
      Attribute att, int[] sortedIndices, double[] weights, double[][] subsetWeights,
      double[] giniGains, Instances data)
    throws Exception {

    double splitPoint = Double.NaN;
    double[][] dist = null;
    int numClasses = data.numClasses();
    int i; // differ instances with or without missing values

    double[][] currDist = new double[2][numClasses];
    dist = new double[2][numClasses];

    // Move all instances without missing values into second subset
    double[] parentDist = new double[numClasses];
    int missingStart = 0;
    for (int j = 0; j < sortedIndices.length; j++) {
      Instance inst = data.instance(sortedIndices[j]);
      if (!inst.isMissing(att)) {
	missingStart ++;
	currDist[1][(int)inst.classValue()] += weights[j];
      }
      parentDist[(int)inst.classValue()] += weights[j];
    }
    System.arraycopy(currDist[1], 0, dist[1], 0, dist[1].length);

    // Try all possible split points
    double currSplit = data.instance(sortedIndices[0]).value(att);
    double currGiniGain;
    double bestGiniGain = -Double.MAX_VALUE;

    for (i = 0; i < sortedIndices.length; i++) {
      Instance inst = data.instance(sortedIndices[i]);
      if (inst.isMissing(att)) {
	break;
      }
      if (inst.value(att) > currSplit) {

	double[][] tempDist = new double[2][numClasses];
	for (int k=0; k<2; k++) {
	  //tempDist[k] = currDist[k];
	  System.arraycopy(currDist[k], 0, tempDist[k], 0, tempDist[k].length);
	}

	double[] tempProps = new double[2];
	for (int k=0; k<2; k++) {
	  tempProps[k] = Utils.sum(tempDist[k]);
	}

	if (Utils.sum(tempProps) !=0) Utils.normalize(tempProps);

	// split missing values
	int index = missingStart;
	while (index < sortedIndices.length) {
	  Instance insta = data.instance(sortedIndices[index]);
	  for (int j = 0; j < 2; j++) {
	    tempDist[j][(int)insta.classValue()] += tempProps[j] * weights[index];
	  }
	  index++;
	}

	currGiniGain = computeGiniGain(parentDist,tempDist);

	if (currGiniGain > bestGiniGain) {
	  bestGiniGain = currGiniGain;

	  // clean split point
//	  splitPoint = Math.rint((inst.value(att) + currSplit)/2.0*100000)/100000.0;
	  splitPoint = (inst.value(att) + currSplit) / 2.0;

	  for (int j = 0; j < currDist.length; j++) {
	    System.arraycopy(tempDist[j], 0, dist[j], 0,
		dist[j].length);
	  }
	}
      }
      currSplit = inst.value(att);
      currDist[0][(int)inst.classValue()] += weights[i];
      currDist[1][(int)inst.classValue()] -= weights[i];
    }

    // Compute weights
    int attIndex = att.index();
    props[attIndex] = new double[2];
    for (int k = 0; k < 2; k++) {
      props[attIndex][k] = Utils.sum(dist[k]);
    }
    if (Utils.sum(props[attIndex]) != 0) Utils.normalize(props[attIndex]);

    // Compute subset weights
    subsetWeights[attIndex] = new double[2];
    for (int j = 0; j < 2; j++) {
      subsetWeights[attIndex][j] += Utils.sum(dist[j]);
    }

    // clean Gini gain
    //giniGains[attIndex] = Math.rint(bestGiniGain*10000000)/10000000.0;
    giniGains[attIndex] = bestGiniGain;
    dists[attIndex] = dist;

    return splitPoint;
  }

  /**
   * Compute distributions, proportions and total weights of two successor 
   * nodes for a given nominal attribute.
   * 
   * @param props 		proportions of each two branches for each attribute
   * @param dists 		class distributions of two branches for each attribute
   * @param att 		numeric att split on
   * @param sortedIndices 	sorted indices of instances for the attirubte
   * @param weights 		weights of instances for the attirbute
   * @param subsetWeights 	total weight of two branches split based on the attribute
   * @param giniGains 		Gini gains for each attribute 
   * @param data 		training instances
   * @param useHeuristic 	if use heuristic search
   * @return 			Gini gain for the given nominal attribute
   * @throws Exception 		if something goes wrong
   */
  protected String nominalDistribution(double[][] props, double[][][] dists,
      Attribute att, int[] sortedIndices, double[] weights, double[][] subsetWeights,
      double[] giniGains, Instances data, boolean useHeuristic)
    throws Exception {

    String[] values = new String[att.numValues()];
    int numCat = values.length; // number of values of the attribute
    int numClasses = data.numClasses();

    String bestSplitString = "";
    double bestGiniGain = -Double.MAX_VALUE;

    // class frequency for each value
    int[] classFreq = new int[numCat];
    for (int j=0; j<numCat; j++) classFreq[j] = 0;

    double[] parentDist = new double[numClasses];
    double[][] currDist = new double[2][numClasses];
    double[][] dist = new double[2][numClasses];
    int missingStart = 0;

    for (int i = 0; i < sortedIndices.length; i++) {
      Instance inst = data.instance(sortedIndices[i]);
      if (!inst.isMissing(att)) {
	missingStart++;
	classFreq[(int)inst.value(att)] ++;
      }
      parentDist[(int)inst.classValue()] += weights[i];
    }

    // count the number of values that class frequency is not 0
    int nonEmpty = 0;
    for (int j=0; j<numCat; j++) {
      if (classFreq[j]!=0) nonEmpty ++;
    }

    // attribute values that class frequency is not 0
    String[] nonEmptyValues = new String[nonEmpty];
    int nonEmptyIndex = 0;
    for (int j=0; j<numCat; j++) {
      if (classFreq[j]!=0) {
	nonEmptyValues[nonEmptyIndex] = att.value(j);
	nonEmptyIndex ++;
      }
    }

    // attribute values that class frequency is 0
    int empty = numCat - nonEmpty;
    String[] emptyValues = new String[empty];
    int emptyIndex = 0;
    for (int j=0; j<numCat; j++) {
      if (classFreq[j]==0) {
	emptyValues[emptyIndex] = att.value(j);
	emptyIndex ++;
      }
    }

    if (nonEmpty<=1) {
      giniGains[att.index()] = 0;
      return "";
    }

    // for tow-class probloms
    if (data.numClasses()==2) {

      //// Firstly, for attribute values which class frequency is not zero

      // probability of class 0 for each attribute value
      double[] pClass0 = new double[nonEmpty];
      // class distribution for each attribute value
      double[][] valDist = new double[nonEmpty][2];

      for (int j=0; j<nonEmpty; j++) {
	for (int k=0; k<2; k++) {
	  valDist[j][k] = 0;
	}
      }

      for (int i = 0; i < sortedIndices.length; i++) {
	Instance inst = data.instance(sortedIndices[i]);
	if (inst.isMissing(att)) {
	  break;
	}

	for (int j=0; j<nonEmpty; j++) {
	  if (att.value((int)inst.value(att)).compareTo(nonEmptyValues[j])==0) {
	    valDist[j][(int)inst.classValue()] += inst.weight();
	    break;
	  }
	}
      }

      for (int j=0; j<nonEmpty; j++) {
	double distSum = Utils.sum(valDist[j]);
	if (distSum==0) pClass0[j]=0;
	else pClass0[j] = valDist[j][0]/distSum;
      }

      // sort category according to the probability of the first class
      String[] sortedValues = new String[nonEmpty];
      for (int j=0; j<nonEmpty; j++) {
	sortedValues[j] = nonEmptyValues[Utils.minIndex(pClass0)];
	pClass0[Utils.minIndex(pClass0)] = Double.MAX_VALUE;
      }

      // Find a subset of attribute values that maximize Gini decrease

      // for the attribute values that class frequency is not 0
      String tempStr = "";

      for (int j=0; j<nonEmpty-1; j++) {
	currDist = new double[2][numClasses];
	if (tempStr=="") tempStr="(" + sortedValues[j] + ")";
	else tempStr += "|"+ "(" + sortedValues[j] + ")";
	for (int i=0; i<sortedIndices.length;i++) {
	  Instance inst = data.instance(sortedIndices[i]);
	  if (inst.isMissing(att)) {
	    break;
	  }

	  if (tempStr.indexOf
	      ("(" + att.value((int)inst.value(att)) + ")")!=-1) {
	    currDist[0][(int)inst.classValue()] += weights[i];
	  } else currDist[1][(int)inst.classValue()] += weights[i];
	}

	double[][] tempDist = new double[2][numClasses];
	for (int kk=0; kk<2; kk++) {
	  tempDist[kk] = currDist[kk];
	}

	double[] tempProps = new double[2];
	for (int kk=0; kk<2; kk++) {
	  tempProps[kk] = Utils.sum(tempDist[kk]);
	}

	if (Utils.sum(tempProps)!=0) Utils.normalize(tempProps);

	// split missing values
	int mstart = missingStart;
	while (mstart < sortedIndices.length) {
	  Instance insta = data.instance(sortedIndices[mstart]);
	  for (int jj = 0; jj < 2; jj++) {
	    tempDist[jj][(int)insta.classValue()] += tempProps[jj] * weights[mstart];
	  }
	  mstart++;
	}

	double currGiniGain = computeGiniGain(parentDist,tempDist);

	if (currGiniGain>bestGiniGain) {
	  bestGiniGain = currGiniGain;
	  bestSplitString = tempStr;
	  for (int jj = 0; jj < 2; jj++) {
	    //dist[jj] = new double[currDist[jj].length];
	    System.arraycopy(tempDist[jj], 0, dist[jj], 0,
		dist[jj].length);
	  }
	}
      }
    }

    // multi-class problems - exhaustive search
    else if (!useHeuristic || nonEmpty<=4) {

      // Firstly, for attribute values which class frequency is not zero
      for (int i=0; i<(int)Math.pow(2,nonEmpty-1); i++) {
	String tempStr="";
	currDist = new double[2][numClasses];
	int mod;
	int bit10 = i;
	for (int j=nonEmpty-1; j>=0; j--) {
	  mod = bit10%2; // convert from 10bit to 2bit
	  if (mod==1) {
	    if (tempStr=="") tempStr = "("+nonEmptyValues[j]+")";
	    else tempStr += "|" + "("+nonEmptyValues[j]+")";
	  }
	  bit10 = bit10/2;
	}
	for (int j=0; j<sortedIndices.length;j++) {
	  Instance inst = data.instance(sortedIndices[j]);
	  if (inst.isMissing(att)) {
	    break;
	  }

	  if (tempStr.indexOf("("+att.value((int)inst.value(att))+")")!=-1) {
	    currDist[0][(int)inst.classValue()] += weights[j];
	  } else currDist[1][(int)inst.classValue()] += weights[j];
	}

	double[][] tempDist = new double[2][numClasses];
	for (int k=0; k<2; k++) {
	  tempDist[k] = currDist[k];
	}

	double[] tempProps = new double[2];
	for (int k=0; k<2; k++) {
	  tempProps[k] = Utils.sum(tempDist[k]);
	}

	if (Utils.sum(tempProps)!=0) Utils.normalize(tempProps);

	// split missing values
	int index = missingStart;
	while (index < sortedIndices.length) {
	  Instance insta = data.instance(sortedIndices[index]);
	  for (int j = 0; j < 2; j++) {
	    tempDist[j][(int)insta.classValue()] += tempProps[j] * weights[index];
	  }
	  index++;
	}

	double currGiniGain = computeGiniGain(parentDist,tempDist);

	if (currGiniGain>bestGiniGain) {
	  bestGiniGain = currGiniGain;
	  bestSplitString = tempStr;
	  for (int j = 0; j < 2; j++) {
	    //dist[jj] = new double[currDist[jj].length];
	    System.arraycopy(tempDist[j], 0, dist[j], 0,
		dist[j].length);
	  }
	}
      }
    }

    // huristic search to solve multi-classes problems
    else {
      // Firstly, for attribute values which class frequency is not zero
      int n = nonEmpty;
      int k = data.numClasses();  // number of classes of the data
      double[][] P = new double[n][k];      // class probability matrix
      int[] numInstancesValue = new int[n]; // number of instances for an attribute value
      double[] meanClass = new double[k];   // vector of mean class probability
      int numInstances = data.numInstances(); // total number of instances

      // initialize the vector of mean class probability
      for (int j=0; j<meanClass.length; j++) meanClass[j]=0;

      for (int j=0; j<numInstances; j++) {
	Instance inst = (Instance)data.instance(j);
	int valueIndex = 0; // attribute value index in nonEmptyValues
	for (int i=0; i<nonEmpty; i++) {
	  if (att.value((int)inst.value(att)).compareToIgnoreCase(nonEmptyValues[i])==0){
	    valueIndex = i;
	    break;
	  }
	}
	P[valueIndex][(int)inst.classValue()]++;
	numInstancesValue[valueIndex]++;
	meanClass[(int)inst.classValue()]++;
      }

      // calculate the class probability matrix
      for (int i=0; i<P.length; i++) {
	for (int j=0; j<P[0].length; j++) {
	  if (numInstancesValue[i]==0) P[i][j]=0;
	  else P[i][j]/=numInstancesValue[i];
	}
      }

      //calculate the vector of mean class probability
      for (int i=0; i<meanClass.length; i++) {
	meanClass[i]/=numInstances;
      }

      // calculate the covariance matrix
      double[][] covariance = new double[k][k];
      for (int i1=0; i1<k; i1++) {
	for (int i2=0; i2<k; i2++) {
	  double element = 0;
	  for (int j=0; j<n; j++) {
	    element += (P[j][i2]-meanClass[i2])*(P[j][i1]-meanClass[i1])
	    *numInstancesValue[j];
	  }
	  covariance[i1][i2] = element;
	}
      }

      Matrix matrix = new Matrix(covariance);
      weka.core.matrix.EigenvalueDecomposition eigen =
	new weka.core.matrix.EigenvalueDecomposition(matrix);
      double[] eigenValues = eigen.getRealEigenvalues();

      // find index of the largest eigenvalue
      int index=0;
      double largest = eigenValues[0];
      for (int i=1; i<eigenValues.length; i++) {
	if (eigenValues[i]>largest) {
	  index=i;
	  largest = eigenValues[i];
	}
      }

      // calculate the first principle component
      double[] FPC = new double[k];
      Matrix eigenVector = eigen.getV();
      double[][] vectorArray = eigenVector.getArray();
      for (int i=0; i<FPC.length; i++) {
	FPC[i] = vectorArray[i][index];
      }

      // calculate the first principle component scores
      //System.out.println("the first principle component scores: ");
      double[] Sa = new double[n];
      for (int i=0; i<Sa.length; i++) {
	Sa[i]=0;
	for (int j=0; j<k; j++) {
	  Sa[i] += FPC[j]*P[i][j];
	}
      }

      // sort category according to Sa(s)
      double[] pCopy = new double[n];
      System.arraycopy(Sa,0,pCopy,0,n);
      String[] sortedValues = new String[n];
      Arrays.sort(Sa);

      for (int j=0; j<n; j++) {
	sortedValues[j] = nonEmptyValues[Utils.minIndex(pCopy)];
	pCopy[Utils.minIndex(pCopy)] = Double.MAX_VALUE;
      }

      // for the attribute values that class frequency is not 0
      String tempStr = "";

      for (int j=0; j<nonEmpty-1; j++) {
	currDist = new double[2][numClasses];
	if (tempStr=="") tempStr="(" + sortedValues[j] + ")";
	else tempStr += "|"+ "(" + sortedValues[j] + ")";
	for (int i=0; i<sortedIndices.length;i++) {
	  Instance inst = data.instance(sortedIndices[i]);
	  if (inst.isMissing(att)) {
	    break;
	  }

	  if (tempStr.indexOf
	      ("(" + att.value((int)inst.value(att)) + ")")!=-1) {
	    currDist[0][(int)inst.classValue()] += weights[i];
	  } else currDist[1][(int)inst.classValue()] += weights[i];
	}

	double[][] tempDist = new double[2][numClasses];
	for (int kk=0; kk<2; kk++) {
	  tempDist[kk] = currDist[kk];
	}

	double[] tempProps = new double[2];
	for (int kk=0; kk<2; kk++) {
	  tempProps[kk] = Utils.sum(tempDist[kk]);
	}

	if (Utils.sum(tempProps)!=0) Utils.normalize(tempProps);

	// split missing values
	int mstart = missingStart;
	while (mstart < sortedIndices.length) {
	  Instance insta = data.instance(sortedIndices[mstart]);
	  for (int jj = 0; jj < 2; jj++) {
	    tempDist[jj][(int)insta.classValue()] += tempProps[jj] * weights[mstart];
	  }
	  mstart++;
	}

	double currGiniGain = computeGiniGain(parentDist,tempDist);

	if (currGiniGain>bestGiniGain) {
	  bestGiniGain = currGiniGain;
	  bestSplitString = tempStr;
	  for (int jj = 0; jj < 2; jj++) {
	    //dist[jj] = new double[currDist[jj].length];
	    System.arraycopy(tempDist[jj], 0, dist[jj], 0,
		dist[jj].length);
	  }
	}
      }
    }

    // Compute weights
    int attIndex = att.index();        
    props[attIndex] = new double[2];
    for (int k = 0; k < 2; k++) {
      props[attIndex][k] = Utils.sum(dist[k]);
    }

    if (!(Utils.sum(props[attIndex]) > 0)) {
      for (int k = 0; k < props[attIndex].length; k++) {
	props[attIndex][k] = 1.0 / (double)props[attIndex].length;
      }
    } else {
      Utils.normalize(props[attIndex]);
    }


    // Compute subset weights
    subsetWeights[attIndex] = new double[2];
    for (int j = 0; j < 2; j++) {
      subsetWeights[attIndex][j] += Utils.sum(dist[j]);
    }

    // Then, for the attribute values that class frequency is 0, split it into the
    // most frequent branch
    for (int j=0; j<empty; j++) {
      if (props[attIndex][0]>=props[attIndex][1]) {
	if (bestSplitString=="") bestSplitString = "(" + emptyValues[j] + ")";
	else bestSplitString += "|" + "(" + emptyValues[j] + ")";
      }
    }

    // clean Gini gain for the attribute
    //giniGains[attIndex] = Math.rint(bestGiniGain*10000000)/10000000.0;
    giniGains[attIndex] = bestGiniGain;

    dists[attIndex] = dist;
    return bestSplitString;
  }


  /**
   * Split data into two subsets and store sorted indices and weights for two
   * successor nodes.
   * 
   * @param subsetIndices 	sorted indecis of instances for each attribute 
   * 				for two successor node
   * @param subsetWeights 	weights of instances for each attribute for 
   * 				two successor node
   * @param att 		attribute the split based on
   * @param splitPoint 		split point the split based on if att is numeric
   * @param splitStr 		split subset the split based on if att is nominal
   * @param sortedIndices 	sorted indices of the instances to be split
   * @param weights 		weights of the instances to bes split
   * @param data 		training data
   * @throws Exception 		if something goes wrong  
   */
  protected void splitData(int[][][] subsetIndices, double[][][] subsetWeights,
      Attribute att, double splitPoint, String splitStr, int[][] sortedIndices,
      double[][] weights, Instances data) throws Exception {

    int j;
    // For each attribute
    for (int i = 0; i < data.numAttributes(); i++) {
      if (i==data.classIndex()) continue;
      int[] num = new int[2];
      for (int k = 0; k < 2; k++) {
	subsetIndices[k][i] = new int[sortedIndices[i].length];
	subsetWeights[k][i] = new double[weights[i].length];
      }

      for (j = 0; j < sortedIndices[i].length; j++) {
	Instance inst = data.instance(sortedIndices[i][j]);
	if (inst.isMissing(att)) {
	  // Split instance up
	  for (int k = 0; k < 2; k++) {
	    if (m_Props[k] > 0) {
	      subsetIndices[k][i][num[k]] = sortedIndices[i][j];
	      subsetWeights[k][i][num[k]] = m_Props[k] * weights[i][j];
	      num[k]++;
	    }
	  }
	} else {
	  int subset;
	  if (att.isNumeric())  {
	    subset = (inst.value(att) < splitPoint) ? 0 : 1;
	  } else { // nominal attribute
	    if (splitStr.indexOf
		("(" + att.value((int)inst.value(att.index()))+")")!=-1) {
	      subset = 0;
	    } else subset = 1;
	  }
	  subsetIndices[subset][i][num[subset]] = sortedIndices[i][j];
	  subsetWeights[subset][i][num[subset]] = weights[i][j];
	  num[subset]++;
	}
      }

      // Trim arrays
      for (int k = 0; k < 2; k++) {
	int[] copy = new int[num[k]];
	System.arraycopy(subsetIndices[k][i], 0, copy, 0, num[k]);
	subsetIndices[k][i] = copy;
	double[] copyWeights = new double[num[k]];
	System.arraycopy(subsetWeights[k][i], 0 ,copyWeights, 0, num[k]);
	subsetWeights[k][i] = copyWeights;
      }
    }
  }

  /**
   * Updates the numIncorrectModel field for all nodes when subtree (to be 
   * pruned) is rooted. This is needed for calculating the alpha-values.
   * 
   * @throws Exception 	if something goes wrong
   */
  public void modelErrors() throws Exception{
    Evaluation eval = new Evaluation(m_train);

    if (!m_isLeaf) {
      m_isLeaf = true; //temporarily make leaf

      // calculate distribution for evaluation
      eval.evaluateModel(this, m_train);
      m_numIncorrectModel = eval.incorrect();

      m_isLeaf = false;

      for (int i = 0; i < m_Successors.length; i++)
	m_Successors[i].modelErrors();

    } else {
      eval.evaluateModel(this, m_train);
      m_numIncorrectModel = eval.incorrect();
    }       
  }

  /**
   * Updates the numIncorrectTree field for all nodes. This is needed for
   * calculating the alpha-values.
   * 
   * @throws Exception 	if something goes wrong
   */
  public void treeErrors() throws Exception {
    if (m_isLeaf) {
      m_numIncorrectTree = m_numIncorrectModel;
    } else {
      m_numIncorrectTree = 0;
      for (int i = 0; i < m_Successors.length; i++) {
	m_Successors[i].treeErrors();
	m_numIncorrectTree += m_Successors[i].m_numIncorrectTree;
      }
    }
  }

  /**
   * Updates the alpha field for all nodes.
   * 
   * @throws Exception 	if something goes wrong
   */
  public void calculateAlphas() throws Exception {

    if (!m_isLeaf) {
      double errorDiff = m_numIncorrectModel - m_numIncorrectTree;
      if (errorDiff <=0) {
	//split increases training error (should not normally happen).
	//prune it instantly.
	makeLeaf(m_train);
	m_Alpha = Double.MAX_VALUE;
      } else {
	//compute alpha
	errorDiff /= m_totalTrainInstances;
	m_Alpha = errorDiff / (double)(numLeaves() - 1);
	long alphaLong = Math.round(m_Alpha*Math.pow(10,10));
	m_Alpha = (double)alphaLong/Math.pow(10,10);
	for (int i = 0; i < m_Successors.length; i++) {
	  m_Successors[i].calculateAlphas();
	}
      }
    } else {
      //alpha = infinite for leaves (do not want to prune)
      m_Alpha = Double.MAX_VALUE;
    }
  }

  /**
   * Find the node with minimal alpha value. If two nodes have the same alpha, 
   * choose the one with more leave nodes.
   * 
   * @param nodeList 	list of inner nodes
   * @return 		the node to be pruned
   */
  protected SimpleCart nodeToPrune(Vector nodeList) {
    if (nodeList.size()==0) return null;
    if (nodeList.size()==1) return (SimpleCart)nodeList.elementAt(0);
    SimpleCart returnNode = (SimpleCart)nodeList.elementAt(0);
    double baseAlpha = returnNode.m_Alpha;
    for (int i=1; i<nodeList.size(); i++) {
      SimpleCart node = (SimpleCart)nodeList.elementAt(i);
      if (node.m_Alpha < baseAlpha) {
	baseAlpha = node.m_Alpha;
	returnNode = node;
      } else if (node.m_Alpha == baseAlpha) { // break tie
	if (node.numLeaves()>returnNode.numLeaves()) {
	  returnNode = node;
	}
      }
    }
    return returnNode;
  }

  /**
   * Compute sorted indices, weights and class probabilities for a given 
   * dataset. Return total weights of the data at the node.
   * 
   * @param data 		training data
   * @param sortedIndices 	sorted indices of instances at the node
   * @param weights 		weights of instances at the node
   * @param classProbs 		class probabilities at the node
   * @return total 		weights of instances at the node
   * @throws Exception 		if something goes wrong
   */
  protected double computeSortedInfo(Instances data, int[][] sortedIndices, double[][] weights,
      double[] classProbs) throws Exception {

    // Create array of sorted indices and weights
    double[] vals = new double[data.numInstances()];
    for (int j = 0; j < data.numAttributes(); j++) {
      if (j==data.classIndex()) continue;
      weights[j] = new double[data.numInstances()];

      if (data.attribute(j).isNominal()) {

	// Handling nominal attributes. Putting indices of
	// instances with missing values at the end.
	sortedIndices[j] = new int[data.numInstances()];
	int count = 0;
	for (int i = 0; i < data.numInstances(); i++) {
	  Instance inst = data.instance(i);
	  if (!inst.isMissing(j)) {
	    sortedIndices[j][count] = i;
	    weights[j][count] = inst.weight();
	    count++;
	  }
	}
	for (int i = 0; i < data.numInstances(); i++) {
	  Instance inst = data.instance(i);
	  if (inst.isMissing(j)) {
	    sortedIndices[j][count] = i;
	    weights[j][count] = inst.weight();
	    count++;
	  }
	}
      } else {

	// Sorted indices are computed for numeric attributes
	// missing values instances are put to end 
	for (int i = 0; i < data.numInstances(); i++) {
	  Instance inst = data.instance(i);
	  vals[i] = inst.value(j);
	}
	sortedIndices[j] = Utils.sort(vals);
	for (int i = 0; i < data.numInstances(); i++) {
	  weights[j][i] = data.instance(sortedIndices[j][i]).weight();
	}
      }
    }

    // Compute initial class counts
    double totalWeight = 0;
    for (int i = 0; i < data.numInstances(); i++) {
      Instance inst = data.instance(i);
      classProbs[(int)inst.classValue()] += inst.weight();
      totalWeight += inst.weight();
    }

    return totalWeight;
  }

  /**
   * Compute and return gini gain for given distributions of a node and its 
   * successor nodes.
   * 
   * @param parentDist 	class distributions of parent node
   * @param childDist 	class distributions of successor nodes
   * @return 		Gini gain computed
   */
  protected double computeGiniGain(double[] parentDist, double[][] childDist) {
    double totalWeight = Utils.sum(parentDist);
    if (totalWeight==0) return 0;

    double leftWeight = Utils.sum(childDist[0]);
    double rightWeight = Utils.sum(childDist[1]);

    double parentGini = computeGini(parentDist, totalWeight);
    double leftGini = computeGini(childDist[0],leftWeight);
    double rightGini = computeGini(childDist[1], rightWeight);

    return parentGini - leftWeight/totalWeight*leftGini -
    rightWeight/totalWeight*rightGini;
  }

  /**
   * Compute and return gini index for a given distribution of a node.
   * 
   * @param dist 	class distributions
   * @param total 	class distributions
   * @return 		Gini index of the class distributions
   */
  protected double computeGini(double[] dist, double total) {
    if (total==0) return 0;
    double val = 0;
    for (int i=0; i<dist.length; i++) {
      val += (dist[i]/total)*(dist[i]/total);
    }
    return 1- val;
  }

  /**
   * Computes class probabilities for instance using the decision tree.
   * 
   * @param instance 	the instance for which class probabilities is to be computed
   * @return 		the class probabilities for the given instance
   * @throws Exception 	if something goes wrong
   */
  public double[] distributionForInstance(Instance instance)
  throws Exception {
    if (!m_isLeaf) {
      // value of split attribute is missing
      if (instance.isMissing(m_Attribute)) {
	double[] returnedDist = new double[m_ClassProbs.length];

	for (int i = 0; i < m_Successors.length; i++) {
	  double[] help =
	    m_Successors[i].distributionForInstance(instance);
	  if (help != null) {
	    for (int j = 0; j < help.length; j++) {
	      returnedDist[j] += m_Props[i] * help[j];
	    }
	  }
	}
	return returnedDist;
      }

      // split attribute is nonimal
      else if (m_Attribute.isNominal()) {
	if (m_SplitString.indexOf("(" +
	    m_Attribute.value((int)instance.value(m_Attribute)) + ")")!=-1)
	  return  m_Successors[0].distributionForInstance(instance);
	else return  m_Successors[1].distributionForInstance(instance);
      }

      // split attribute is numeric
      else {
	if (instance.value(m_Attribute) < m_SplitValue)
	  return m_Successors[0].distributionForInstance(instance);
	else
	  return m_Successors[1].distributionForInstance(instance);
      }
    }

    // leaf node
    else return m_ClassProbs;
  }

  /**
   * Make the node leaf node.
   * 
   * @param data 	trainging data
   */
  protected void makeLeaf(Instances data) {
    m_Attribute = null;
    m_isLeaf = true;
    m_ClassValue=Utils.maxIndex(m_ClassProbs);
    m_ClassAttribute = data.classAttribute();
  }

  /**
   * Prints the decision tree using the protected toString method from below.
   * 
   * @return 		a textual description of the classifier
   */
  public String toString() {
    if ((m_ClassProbs == null) && (m_Successors == null)) {
      return "CART Tree: No model built yet.";
    }

    return "CART Decision Tree\n" + toString(0)+"\n\n"
    +"Number of Leaf Nodes: "+numLeaves()+"\n\n" +
    "Size of the Tree: "+numNodes();
  }

  /**
   * Outputs a tree at a certain level.
   * 
   * @param level 	the level at which the tree is to be printed
   * @return 		a tree at a certain level
   */
  protected String toString(int level) {

    StringBuffer text = new StringBuffer();
    // if leaf nodes
    if (m_Attribute == null) {
      if (Instance.isMissingValue(m_ClassValue)) {
	text.append(": null");
      } else {
	double correctNum = (int)(m_Distribution[Utils.maxIndex(m_Distribution)]*100)/
	100.0;
	double wrongNum = (int)((Utils.sum(m_Distribution) -
	    m_Distribution[Utils.maxIndex(m_Distribution)])*100)/100.0;
	String str = "("  + correctNum + "/" + wrongNum + ")";
	text.append(": " + m_ClassAttribute.value((int) m_ClassValue)+ str);
      }
    } else {
      for (int j = 0; j < 2; j++) {
	text.append("\n");
	for (int i = 0; i < level; i++) {
	  text.append("|  ");
	}
	if (j==0) {
	  if (m_Attribute.isNumeric())
	    text.append(m_Attribute.name() + " < " + m_SplitValue);
	  else
	    text.append(m_Attribute.name() + "=" + m_SplitString);
	} else {
	  if (m_Attribute.isNumeric())
	    text.append(m_Attribute.name() + " >= " + m_SplitValue);
	  else
	    text.append(m_Attribute.name() + "!=" + m_SplitString);
	}
	text.append(m_Successors[j].toString(level + 1));
      }
    }
    return text.toString();
  }

  /**
   * Compute size of the tree.
   * 
   * @return 		size of the tree
   */
  public int numNodes() {
    if (m_isLeaf) {
      return 1;
    } else {
      int size =1;
      for (int i=0;i<m_Successors.length;i++) {
	size+=m_Successors[i].numNodes();
      }
      return size;
    }
  }

  /**
   * Method to count the number of inner nodes in the tree.
   * 
   * @return 		the number of inner nodes
   */
  public int numInnerNodes(){
    if (m_Attribute==null) return 0;
    int numNodes = 1;
    for (int i = 0; i < m_Successors.length; i++)
      numNodes += m_Successors[i].numInnerNodes();
    return numNodes;
  }

  /**
   * Return a list of all inner nodes in the tree.
   * 
   * @return 		the list of all inner nodes
   */
  protected Vector getInnerNodes(){
    Vector nodeList = new Vector();
    fillInnerNodes(nodeList);
    return nodeList;
  }

  /**
   * Fills a list with all inner nodes in the tree.
   * 
   * @param nodeList 	the list to be filled
   */
  protected void fillInnerNodes(Vector nodeList) {
    if (!m_isLeaf) {
      nodeList.add(this);
      for (int i = 0; i < m_Successors.length; i++)
	m_Successors[i].fillInnerNodes(nodeList);
    }
  }

  /**
   * Compute number of leaf nodes.
   * 
   * @return 		number of leaf nodes
   */
  public int numLeaves() {
    if (m_isLeaf) return 1;
    else {
      int size=0;
      for (int i=0;i<m_Successors.length;i++) {
	size+=m_Successors[i].numLeaves();
      }
      return size;
    }
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return 		an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector 	result;
    Enumeration	en;
    
    result = new Vector();
    
    en = super.listOptions();
    while (en.hasMoreElements())
      result.addElement(en.nextElement());

    result.addElement(new Option(
	"\tThe minimal number of instances at the terminal nodes.\n" 
	+ "\t(default 2)",
	"M", 1, "-M <min no>"));
    
    result.addElement(new Option(
	"\tThe number of folds used in the minimal cost-complexity pruning.\n"
	+ "\t(default 5)",
	"N", 1, "-N <num folds>"));
    
    result.addElement(new Option(
	"\tDon't use the minimal cost-complexity pruning.\n"
	+ "\t(default yes).",
	"U", 0, "-U"));
    
    result.addElement(new Option(
	"\tDon't use the heuristic method for binary split.\n"
	+ "\t(default true).",
	"H", 0, "-H"));
    
    result.addElement(new Option(
	"\tUse 1 SE rule to make pruning decision.\n"
	+ "\t(default no).",
	"A", 0, "-A"));
    
    result.addElement(new Option(
	"\tPercentage of training data size (0-1].\n" 
	+ "\t(default 1).",
	"C", 1, "-C"));

    return result.elements();
  }

  /**
   * Parses a given list of options. <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -S &lt;num&gt;
   *  Random number seed.
   *  (default 1)</pre>
   * 
   * <pre> -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   * <pre> -M &lt;min no&gt;
   *  The minimal number of instances at the terminal nodes.
   *  (default 2)</pre>
   * 
   * <pre> -N &lt;num folds&gt;
   *  The number of folds used in the minimal cost-complexity pruning.
   *  (default 5)</pre>
   * 
   * <pre> -U
   *  Don't use the minimal cost-complexity pruning.
   *  (default yes).</pre>
   * 
   * <pre> -H
   *  Don't use the heuristic method for binary split.
   *  (default true).</pre>
   * 
   * <pre> -A
   *  Use 1 SE rule to make pruning decision.
   *  (default no).</pre>
   * 
   * <pre> -C
   *  Percentage of training data size (0-1].
   *  (default 1).</pre>
   * 
   <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an options is not supported
   */
  public void setOptions(String[] options) throws Exception {
    String	tmpStr;
    
    super.setOptions(options);
    
    tmpStr = Utils.getOption('M', options);
    if (tmpStr.length() != 0)
      setMinNumObj(Double.parseDouble(tmpStr));
    else
      setMinNumObj(2);

    tmpStr = Utils.getOption('N', options);
    if (tmpStr.length()!=0)
      setNumFoldsPruning(Integer.parseInt(tmpStr));
    else
      setNumFoldsPruning(5);

    setUsePrune(!Utils.getFlag('U',options));
    setHeuristic(!Utils.getFlag('H',options));
    setUseOneSE(Utils.getFlag('A',options));

    tmpStr = Utils.getOption('C', options);
    if (tmpStr.length()!=0)
      setSizePer(Double.parseDouble(tmpStr));
    else
      setSizePer(1);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the classifier.
   * 
   * @return 		the current setting of the classifier
   */
  public String[] getOptions() {
    int       	i;
    Vector    	result;
    String[]  	options;

    result = new Vector();

    options = super.getOptions();
    for (i = 0; i < options.length; i++)
      result.add(options[i]);

    result.add("-M");
    result.add("" + getMinNumObj());
    
    result.add("-N");
    result.add("" + getNumFoldsPruning());
    
    if (!getUsePrune())
      result.add("-U");
    
    if (!getHeuristic())
      result.add("-H");
    
    if (getUseOneSE())
      result.add("-A");
    
    result.add("-C");
    result.add("" + getSizePer());

    return (String[]) result.toArray(new String[result.size()]);	  
  }

  /**
   * Return an enumeration of the measure names.
   * 
   * @return 		an enumeration of the measure names
   */
  public Enumeration enumerateMeasures() {
    Vector result = new Vector();
    
    result.addElement("measureTreeSize");
    
    return result.elements();
  }

  /**
   * Return number of tree size.
   * 
   * @return 		number of tree size
   */
  public double measureTreeSize() {
    return numNodes();
  }

  /**
   * Returns the value of the named measure.
   * 
   * @param additionalMeasureName 	the name of the measure to query for its value
   * @return 				the value of the named measure
   * @throws IllegalArgumentException 	if the named measure is not supported
   */
  public double getMeasure(String additionalMeasureName) {
    if (additionalMeasureName.compareToIgnoreCase("measureTreeSize") == 0) {
      return measureTreeSize();
    } else {
      throw new IllegalArgumentException(additionalMeasureName
	  + " not supported (Cart pruning)");
    }
  }

  /**
   * Returns the tip text for this property
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String minNumObjTipText() {
    return "The minimal number of observations at the terminal nodes (default 2).";
  }

  /**
   * Set minimal number of instances at the terminal nodes.
   * 
   * @param value 	minimal number of instances at the terminal nodes
   */
  public void setMinNumObj(double value) {
    m_minNumObj = value;
  }

  /**
   * Get minimal number of instances at the terminal nodes.
   * 
   * @return 		minimal number of instances at the terminal nodes
   */
  public double getMinNumObj() {
    return m_minNumObj;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String numFoldsPruningTipText() {
    return "The number of folds in the internal cross-validation (default 5).";
  }

  /** 
   * Set number of folds in internal cross-validation.
   * 
   * @param value 	number of folds in internal cross-validation.
   */
  public void setNumFoldsPruning(int value) {
    m_numFoldsPruning = value;
  }

  /**
   * Set number of folds in internal cross-validation.
   * 
   * @return 		number of folds in internal cross-validation.
   */
  public int getNumFoldsPruning() {
    return m_numFoldsPruning;
  }

  /**
   * Return the tip text for this property
   * 
   * @return 		tip text for this property suitable for displaying in 
   * 			the explorer/experimenter gui.
   */
  public String usePruneTipText() {
    return "Use minimal cost-complexity pruning (default yes).";
  }

  /** 
   * Set if use minimal cost-complexity pruning.
   * 
   * @param value 	if use minimal cost-complexity pruning
   */
  public void setUsePrune(boolean value) {
    m_Prune = value;
  }

  /** 
   * Get if use minimal cost-complexity pruning.
   * 
   * @return 		if use minimal cost-complexity pruning
   */
  public boolean getUsePrune() {
    return m_Prune;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui.
   */
  public String heuristicTipText() {
    return 
        "If heuristic search is used for binary split for nominal attributes "
      + "in multi-class problems (default yes).";
  }

  /**
   * Set if use heuristic search for nominal attributes in multi-class problems.
   * 
   * @param value 	if use heuristic search for nominal attributes in 
   * 			multi-class problems
   */
  public void setHeuristic(boolean value) {
    m_Heuristic = value;
  }

  /** 
   * Get if use heuristic search for nominal attributes in multi-class problems.
   * 
   * @return 		if use heuristic search for nominal attributes in 
   * 			multi-class problems
   */
  public boolean getHeuristic() {return m_Heuristic;}

  /**
   * Returns the tip text for this property
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui.
   */
  public String useOneSETipText() {
    return "Use the 1SE rule to make pruning decisoin.";
  }

  /** 
   * Set if use the 1SE rule to choose final model.
   * 
   * @param value 	if use the 1SE rule to choose final model
   */
  public void setUseOneSE(boolean value) {
    m_UseOneSE = value;
  }

  /**
   * Get if use the 1SE rule to choose final model.
   * 
   * @return 		if use the 1SE rule to choose final model
   */
  public boolean getUseOneSE() {
    return m_UseOneSE;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui.
   */
  public String sizePerTipText() {
    return "The percentage of the training set size (0-1, 0 not included).";
  }

  /** 
   * Set training set size.
   * 
   * @param value 	training set size
   */  
  public void setSizePer(double value) {
    if ((value <= 0) || (value > 1))
      System.err.println(
	  "The percentage of the training set size must be in range 0 to 1 "
	  + "(0 not included) - ignored!");
    else
      m_SizePer = value;
  }

  /**
   * Get training set size.
   * 
   * @return 		training set size
   */
  public double getSizePer() {
    return m_SizePer;
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  /**
   * Main method.
   * @param args the options for the classifier
   */
  public static void main(String[] args) {
    runClassifier(new SimpleCart(), args);
  }
}
