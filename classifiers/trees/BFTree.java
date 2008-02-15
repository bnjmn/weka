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
 * BFTree.java
 * Copyright (C) 2007 Haijian Shi
 *
 */

package weka.classifiers.trees;

import weka.classifiers.Evaluation;
import weka.classifiers.RandomizableClassifier;
import weka.core.AdditionalMeasureProducer;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.SelectedTag;
import weka.core.Tag;
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
 * Class for building a best-first decision tree classifier. This class uses binary split for both nominal and numeric attributes. For missing values, the method of 'fractional' instances is used.<br/>
 * <br/>
 * For more information, see:<br/>
 * <br/>
 * Haijian Shi (2007). Best-first decision tree learning. Hamilton, NZ.<br/>
 * <br/>
 * Jerome Friedman, Trevor Hastie, Robert Tibshirani (2000). Additive logistic regression : A statistical view of boosting. Annals of statistics. 28(2):337-407.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;mastersthesis{Shi2007,
 *    address = {Hamilton, NZ},
 *    author = {Haijian Shi},
 *    note = {COMP594},
 *    school = {University of Waikato},
 *    title = {Best-first decision tree learning},
 *    year = {2007}
 * }
 * 
 * &#64;article{Friedman2000,
 *    author = {Jerome Friedman and Trevor Hastie and Robert Tibshirani},
 *    journal = {Annals of statistics},
 *    number = {2},
 *    pages = {337-407},
 *    title = {Additive logistic regression : A statistical view of boosting},
 *    volume = {28},
 *    year = {2000},
 *    ISSN = {0090-5364}
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
 * <pre> -P &lt;UNPRUNED|POSTPRUNED|PREPRUNED&gt;
 *  The pruning strategy.
 *  (default: POSTPRUNED)</pre>
 * 
 * <pre> -M &lt;min no&gt;
 *  The minimal number of instances at the terminal nodes.
 *  (default 2)</pre>
 * 
 * <pre> -N &lt;num folds&gt;
 *  The number of folds used in the pruning.
 *  (default 5)</pre>
 * 
 * <pre> -H
 *  Don't use heuristic search for nominal attributes in multi-class
 *  problem (default yes).
 * </pre>
 * 
 * <pre> -G
 *  Don't use Gini index for splitting (default yes),
 *  if not information is used.</pre>
 * 
 * <pre> -R
 *  Don't use error rate in internal cross-validation (default yes), 
 *  but root mean squared error.</pre>
 * 
 * <pre> -A
 *  Use the 1 SE rule to make pruning decision.
 *  (default no).</pre>
 * 
 * <pre> -C
 *  Percentage of training data size (0-1]
 *  (default 1).</pre>
 * 
 <!-- options-end -->
 *
 * @author Haijian Shi (hs69@cs.waikato.ac.nz)
 * @version $Revision: 1.4 $
 */
public class BFTree
  extends RandomizableClassifier
  implements AdditionalMeasureProducer, TechnicalInformationHandler {

  /** For serialization.	 */
  private static final long serialVersionUID = -7035607375962528217L;

  /** pruning strategy: un-pruned */
  public static final int PRUNING_UNPRUNED = 0;
  /** pruning strategy: post-pruning */
  public static final int PRUNING_POSTPRUNING = 1;
  /** pruning strategy: pre-pruning */
  public static final int PRUNING_PREPRUNING = 2;
  /** pruning strategy */
  public static final Tag[] TAGS_PRUNING = {
    new Tag(PRUNING_UNPRUNED, "unpruned", "Un-pruned"),
    new Tag(PRUNING_POSTPRUNING, "postpruned", "Post-pruning"),
    new Tag(PRUNING_PREPRUNING, "prepruned", "Pre-pruning")
  };
  
  /** the pruning strategy */
  protected int m_PruningStrategy = PRUNING_POSTPRUNING;

  /** Successor nodes. */
  protected BFTree[] m_Successors;

  /** Attribute used for splitting. */
  protected Attribute m_Attribute;

  /** Split point (for numeric attributes). */
  protected double m_SplitValue;

  /** Split subset (for nominal attributes). */
  protected String m_SplitString;

  /** Class value for a node. */
  protected double m_ClassValue;

  /** Class attribute of a dataset. */
  protected Attribute m_ClassAttribute;

  /** Minimum number of instances at leaf nodes. */
  protected int m_minNumObj = 2;

  /** Number of folds for the pruning. */
  protected int m_numFoldsPruning = 5;

  /** If the ndoe is leaf node. */
  protected boolean m_isLeaf;

  /** Number of expansions. */
  protected static int m_Expansion;

  /** Fixed number of expansions (if no pruning method is used, its value is -1. Otherwise,
   *  its value is gotten from internal cross-validation).   */
  protected int m_FixedExpansion = -1;

  /** If use huristic search for binary split (default true). Note even if its value is true, it is only
   * used when the number of values of a nominal attribute is larger than 4. */
  protected boolean m_Heuristic = true;

  /** If use Gini index as the splitting criterion - default (if not, information is used). */
  protected boolean m_UseGini = true;

  /** If use error rate in internal cross-validation to fix the number of expansions - default
   *  (if not, root mean squared error is used). */
  protected boolean m_UseErrorRate = true;

  /** If use the 1SE rule to make the decision. */
  protected boolean m_UseOneSE = false;

  /** Class distributions.  */
  protected double[] m_Distribution;

  /** Branch proportions. */
  protected double[] m_Props;

  /** Sorted indices. */
  protected int[][] m_SortedIndices;

  /** Sorted weights. */
  protected double[][] m_Weights;

  /** Distributions of each attribute for two successor nodes. */
  protected double[][][] m_Dists;

  /** Class probabilities. */
  protected double[] m_ClassProbs;

  /** Total weights. */
  protected double m_TotalWeight;

  /** The training data size (0-1). Default 1. */
  protected double m_SizePer = 1;

  /**
   * Returns a string describing classifier
   * 
   * @return 		a description suitable for displaying in the 
   * 			explorer/experimenter gui
   */
  public String globalInfo() {
    return  
        "Class for building a best-first decision tree classifier. "
      + "This class uses binary split for both nominal and numeric attributes. "
      + "For missing values, the method of 'fractional' instances is used.\n\n"
      + "For more information, see:\n\n"
      + getTechnicalInformation().toString();
  }
  
  /**
   * Returns an instance of a TechnicalInformation object, containing 
   * detailed information about the technical background of this class,
   * e.g., paper reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation 	result;
    TechnicalInformation 	additional;
    
    result = new TechnicalInformation(Type.MASTERSTHESIS);
    result.setValue(Field.AUTHOR, "Haijian Shi");
    result.setValue(Field.YEAR, "2007");
    result.setValue(Field.TITLE, "Best-first decision tree learning");
    result.setValue(Field.SCHOOL, "University of Waikato");
    result.setValue(Field.ADDRESS, "Hamilton, NZ");
    result.setValue(Field.NOTE, "COMP594");
    
    additional = result.add(Type.ARTICLE);
    additional.setValue(Field.AUTHOR, "Jerome Friedman and Trevor Hastie and Robert Tibshirani");
    additional.setValue(Field.YEAR, "2000");
    additional.setValue(Field.TITLE, "Additive logistic regression : A statistical view of boosting");
    additional.setValue(Field.JOURNAL, "Annals of statistics");
    additional.setValue(Field.VOLUME, "28");
    additional.setValue(Field.NUMBER, "2");
    additional.setValue(Field.PAGES, "337-407");
    additional.setValue(Field.ISSN, "0090-5364");
    
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
   * Method for building a BestFirst decision tree classifier.
   *
   * @param data 	set of instances serving as training data
   * @throws Exception 	if decision tree cannot be built successfully
   */
  public void buildClassifier(Instances data) throws Exception {

    getCapabilities().testWithFail(data);
    data = new Instances(data);
    data.deleteWithMissingClass();

    // build an unpruned tree
    if (m_PruningStrategy == PRUNING_UNPRUNED) {

      // calculate sorted indices, weights and initial class probabilities
      int[][] sortedIndices = new int[data.numAttributes()][0];
      double[][] weights = new double[data.numAttributes()][0];
      double[] classProbs = new double[data.numClasses()];
      double totalWeight = computeSortedInfo(data,sortedIndices, weights,classProbs);

      // Compute information of the best split for this node (include split attribute,
      // split value and gini gain (or information gain)). At the same time, compute
      // variables dists, props and totalSubsetWeights.
      double[][][] dists = new double[data.numAttributes()][2][data.numClasses()];
      double[][] props = new double[data.numAttributes()][2];
      double[][] totalSubsetWeights = new double[data.numAttributes()][2];
      FastVector nodeInfo = computeSplitInfo(this, data, sortedIndices, weights, dists,
	  props, totalSubsetWeights, m_Heuristic, m_UseGini);

      // add the node (with all split info) into BestFirstElements
      FastVector BestFirstElements = new FastVector();
      BestFirstElements.addElement(nodeInfo);

      // Make the best-first decision tree.
      int attIndex = ((Attribute)nodeInfo.elementAt(1)).index();
      m_Expansion = 0;
      makeTree(BestFirstElements, data, sortedIndices, weights, dists, classProbs,
	  totalWeight, props[attIndex] ,m_minNumObj, m_Heuristic, m_UseGini, m_FixedExpansion);

      return;
    }

    // the following code is for pre-pruning and post-pruning methods

    // Compute train data, test data, sorted indices, sorted weights, total weights,
    // class probabilities, class distributions, branch proportions and total subset
    // weights for root nodes of each fold for prepruning and postpruning.
    int expansion = 0;

    Random random = new Random(m_Seed);
    Instances cvData = new Instances(data);
    cvData.randomize(random);
    cvData = new Instances(cvData,0,(int)(cvData.numInstances()*m_SizePer)-1);
    cvData.stratify(m_numFoldsPruning);

    Instances[] train = new Instances[m_numFoldsPruning];
    Instances[] test = new Instances[m_numFoldsPruning];
    FastVector[] parallelBFElements = new FastVector [m_numFoldsPruning];
    BFTree[] m_roots = new BFTree[m_numFoldsPruning];

    int[][][] sortedIndices = new int[m_numFoldsPruning][data.numAttributes()][0];
    double[][][] weights = new double[m_numFoldsPruning][data.numAttributes()][0];
    double[][] classProbs = new double[m_numFoldsPruning][data.numClasses()];
    double[] totalWeight = new double[m_numFoldsPruning];

    double[][][][] dists =
      new double[m_numFoldsPruning][data.numAttributes()][2][data.numClasses()];
    double[][][] props =
      new double[m_numFoldsPruning][data.numAttributes()][2];
    double[][][] totalSubsetWeights =
      new double[m_numFoldsPruning][data.numAttributes()][2];
    FastVector[] nodeInfo = new FastVector[m_numFoldsPruning];

    for (int i = 0; i < m_numFoldsPruning; i++) {
      train[i] = cvData.trainCV(m_numFoldsPruning, i);
      test[i] = cvData.testCV(m_numFoldsPruning, i);
      parallelBFElements[i] = new FastVector();
      m_roots[i] = new BFTree();

      // calculate sorted indices, weights, initial class counts and total weights for each training data
      totalWeight[i] = computeSortedInfo(train[i],sortedIndices[i], weights[i],
	  classProbs[i]);

      // compute information of the best split for this node (include split attribute,
      // split value and gini gain (or information gain)) in this fold
      nodeInfo[i] = computeSplitInfo(m_roots[i], train[i], sortedIndices[i],
	  weights[i], dists[i], props[i], totalSubsetWeights[i], m_Heuristic, m_UseGini);

      // compute information for root nodes

      int attIndex = ((Attribute)nodeInfo[i].elementAt(1)).index();

      m_roots[i].m_SortedIndices = new int[sortedIndices[i].length][0];
      m_roots[i].m_Weights = new double[weights[i].length][0];
      m_roots[i].m_Dists = new double[dists[i].length][0][0];
      m_roots[i].m_ClassProbs = new double[classProbs[i].length];
      m_roots[i].m_Distribution = new double[classProbs[i].length];
      m_roots[i].m_Props = new double[2];

      for (int j=0; j<m_roots[i].m_SortedIndices.length; j++) {
	m_roots[i].m_SortedIndices[j] = sortedIndices[i][j];
	m_roots[i].m_Weights[j] = weights[i][j];
	m_roots[i].m_Dists[j] = dists[i][j];
      }

      System.arraycopy(classProbs[i], 0, m_roots[i].m_ClassProbs, 0,
	  classProbs[i].length);
      if (Utils.sum(m_roots[i].m_ClassProbs)!=0)
	Utils.normalize(m_roots[i].m_ClassProbs);

      System.arraycopy(classProbs[i], 0, m_roots[i].m_Distribution, 0,
	  classProbs[i].length);
      System.arraycopy(props[i][attIndex], 0, m_roots[i].m_Props, 0,
	  props[i][attIndex].length);

      m_roots[i].m_TotalWeight = totalWeight[i];

      parallelBFElements[i].addElement(nodeInfo[i]);
    }

    // build a pre-pruned tree
    if (m_PruningStrategy == PRUNING_PREPRUNING) {

      double previousError = Double.MAX_VALUE;
      double currentError = previousError;
      double minError = Double.MAX_VALUE;
      int minExpansion = 0;
      FastVector errorList = new FastVector();
      while(true) {
	// compute average error
	double expansionError = 0;
	int count = 0;

	for (int i=0; i<m_numFoldsPruning; i++) {
	  Evaluation eval;

	  // calculate error rate if only root node
	  if (expansion==0) {
	    m_roots[i].m_isLeaf = true;
	    eval = new Evaluation(test[i]);
	    eval.evaluateModel(m_roots[i], test[i]);
	    if (m_UseErrorRate) expansionError += eval.errorRate();
	    else expansionError += eval.rootMeanSquaredError();
	    count ++;
	  }

	  // make tree - expand one node at a time
	  else {
	    if (m_roots[i] == null) continue; // if the tree cannot be expanded, go to next fold
	    m_roots[i].m_isLeaf = false;
	    BFTree nodeToSplit = (BFTree)
	    (((FastVector)(parallelBFElements[i].elementAt(0))).elementAt(0));
	    if (!m_roots[i].makeTree(parallelBFElements[i], m_roots[i], train[i],
		nodeToSplit.m_SortedIndices, nodeToSplit.m_Weights,
		nodeToSplit.m_Dists, nodeToSplit.m_ClassProbs,
		nodeToSplit.m_TotalWeight, nodeToSplit.m_Props, m_minNumObj,
		m_Heuristic, m_UseGini)) {
	      m_roots[i] = null; // cannot be expanded
	      continue;
	    }
	    eval = new Evaluation(test[i]);
	    eval.evaluateModel(m_roots[i], test[i]);
	    if (m_UseErrorRate) expansionError += eval.errorRate();
	    else expansionError += eval.rootMeanSquaredError();
	    count ++;
	  }
	}

	// no tree can be expanded any more
	if (count==0) break;

	expansionError /=count;
	errorList.addElement(new Double(expansionError));
	currentError = expansionError;

	if (!m_UseOneSE) {
	  if (currentError>previousError)
	    break;
	}

	else {
	  if (expansionError < minError) {
	    minError = expansionError;
	    minExpansion = expansion;
	  }

	  if (currentError>previousError) {
	    double oneSE = Math.sqrt(minError*(1-minError)/
		data.numInstances());
	    if (currentError > minError + oneSE) {
	      break;
	    }
	  }
	}

	expansion ++;
	previousError = currentError;
      }

      if (!m_UseOneSE) expansion = expansion - 1;
      else {
	double oneSE = Math.sqrt(minError*(1-minError)/data.numInstances());
	for (int i=0; i<errorList.size(); i++) {
	  double error = ((Double)(errorList.elementAt(i))).doubleValue();
	  if (error<=minError + oneSE) { // && counts[i]>=m_numFoldsPruning/2) {
	    expansion = i;
	    break;
	  }
	}
      }
    }

    // build a postpruned tree
    else {
      FastVector[] modelError = new FastVector[m_numFoldsPruning];

      // calculate error of each expansion for each fold
      for (int i = 0; i < m_numFoldsPruning; i++) {
	modelError[i] = new FastVector();

	m_roots[i].m_isLeaf = true;
	Evaluation eval = new Evaluation(test[i]);
	eval.evaluateModel(m_roots[i], test[i]);
	double error;
	if (m_UseErrorRate) error = eval.errorRate();
	else error = eval.rootMeanSquaredError();
	modelError[i].addElement(new Double(error));

	m_roots[i].m_isLeaf = false;
	BFTree nodeToSplit = (BFTree)
	(((FastVector)(parallelBFElements[i].elementAt(0))).elementAt(0));

	m_roots[i].makeTree(parallelBFElements[i], m_roots[i], train[i], test[i],
	    modelError[i],nodeToSplit.m_SortedIndices, nodeToSplit.m_Weights,
	    nodeToSplit.m_Dists, nodeToSplit.m_ClassProbs,
	    nodeToSplit.m_TotalWeight, nodeToSplit.m_Props, m_minNumObj,
	    m_Heuristic, m_UseGini, m_UseErrorRate);
	m_roots[i] = null;
      }

      // find the expansion with minimal error rate
      double minError = Double.MAX_VALUE;

      int maxExpansion = modelError[0].size();
      for (int i=1; i<modelError.length; i++) {
	if (modelError[i].size()>maxExpansion)
	  maxExpansion = modelError[i].size();
      }

      double[] error = new double[maxExpansion];
      int[] counts = new int[maxExpansion];
      for (int i=0; i<maxExpansion; i++) {
	counts[i] = 0;
	error[i] = 0;
	for (int j=0; j<m_numFoldsPruning; j++) {
	  if (i<modelError[j].size()) {
	    error[i] += ((Double)modelError[j].elementAt(i)).doubleValue();
	    counts[i]++;
	  }
	}
	error[i] = error[i]/counts[i]; //average error for each expansion

	if (error[i]<minError) {// && counts[i]>=m_numFoldsPruning/2) {
	  minError = error[i];
	  expansion = i;
	}
      }

      // the 1 SE rule choosen
      if (m_UseOneSE) {
	double oneSE = Math.sqrt(minError*(1-minError)/
	    data.numInstances());
	for (int i=0; i<maxExpansion; i++) {
	  if (error[i]<=minError + oneSE) { // && counts[i]>=m_numFoldsPruning/2) {
	    expansion = i;
	    break;
	  }
	}
      }
    }

    // make tree on all data based on the expansion caculated
    // from cross-validation

    // calculate sorted indices, weights and initial class counts
    int[][] prune_sortedIndices = new int[data.numAttributes()][0];
    double[][] prune_weights = new double[data.numAttributes()][0];
    double[] prune_classProbs = new double[data.numClasses()];
    double prune_totalWeight = computeSortedInfo(data, prune_sortedIndices,
	prune_weights, prune_classProbs);

    // compute information of the best split for this node (include split attribute,
    // split value and gini gain)
    double[][][] prune_dists = new double[data.numAttributes()][2][data.numClasses()];
    double[][] prune_props = new double[data.numAttributes()][2];
    double[][] prune_totalSubsetWeights = new double[data.numAttributes()][2];
    FastVector prune_nodeInfo = computeSplitInfo(this, data, prune_sortedIndices,
	prune_weights, prune_dists, prune_props, prune_totalSubsetWeights, m_Heuristic,m_UseGini);

    // add the root node (with its split info) to BestFirstElements
    FastVector BestFirstElements = new FastVector();
    BestFirstElements.addElement(prune_nodeInfo);

    int attIndex = ((Attribute)prune_nodeInfo.elementAt(1)).index();
    m_Expansion = 0;
    makeTree(BestFirstElements, data, prune_sortedIndices, prune_weights, prune_dists,
	prune_classProbs, prune_totalWeight, prune_props[attIndex] ,m_minNumObj,
	m_Heuristic, m_UseGini, expansion);
  }

  /**
   * Recursively build a best-first decision tree.
   * Method for building a Best-First tree for a given number of expansions.
   * preExpasion is -1 means that no expansion is specified (just for a
   * tree without any pruning method). Pre-pruning and post-pruning methods also
   * use this method to build the final tree on all training data based on the
   * expansion calculated from internal cross-validation.
   *
   * @param BestFirstElements 	list to store BFTree nodes
   * @param data 		training data
   * @param sortedIndices 	sorted indices of the instances
   * @param weights 		weights of the instances
   * @param dists 		class distributions for each attribute
   * @param classProbs 		class probabilities of this node
   * @param totalWeight 	total weight of this node (note if the node 
   * 				can not split, this value is not calculated.)
   * @param branchProps 	proportions of two subbranches
   * @param minNumObj 		minimal number of instances at leaf nodes
   * @param useHeuristic 	if use heuristic search for nominal attributes 
   * 				in multi-class problem
   * @param useGini 		if use Gini index as splitting criterion
   * @param preExpansion 	the number of expansions the tree to be expanded
   * @throws Exception 		if something goes wrong
   */
  protected void makeTree(FastVector BestFirstElements,Instances data,
      int[][] sortedIndices, double[][] weights, double[][][] dists,
      double[] classProbs, double totalWeight, double[] branchProps,
      int minNumObj, boolean useHeuristic, boolean useGini, int preExpansion)
  	throws Exception {

    if (BestFirstElements.size()==0) return;

    ///////////////////////////////////////////////////////////////////////
    // All information about the node to split (the first BestFirst object in
    // BestFirstElements)
    FastVector firstElement = (FastVector)BestFirstElements.elementAt(0);

    // split attribute
    Attribute att = (Attribute)firstElement.elementAt(1);

    // info of split value or split string
    double splitValue = Double.NaN;
    String splitStr = null;
    if (att.isNumeric())
      splitValue = ((Double)firstElement.elementAt(2)).doubleValue();
    else {
      splitStr=((String)firstElement.elementAt(2)).toString();
    }

    // the best gini gain or information gain of this node
    double gain = ((Double)firstElement.elementAt(3)).doubleValue();
    ///////////////////////////////////////////////////////////////////////

    if (m_ClassProbs==null) {
      m_SortedIndices = new int[sortedIndices.length][0];
      m_Weights = new double[weights.length][0];
      m_Dists = new double[dists.length][0][0];
      m_ClassProbs = new double[classProbs.length];
      m_Distribution = new double[classProbs.length];
      m_Props = new double[2];

      for (int i=0; i<m_SortedIndices.length; i++) {
	m_SortedIndices[i] = sortedIndices[i];
	m_Weights[i] = weights[i];
	m_Dists[i] = dists[i];
      }

      System.arraycopy(classProbs, 0, m_ClassProbs, 0, classProbs.length);
      System.arraycopy(classProbs, 0, m_Distribution, 0, classProbs.length);
      System.arraycopy(branchProps, 0, m_Props, 0, m_Props.length);
      m_TotalWeight = totalWeight;
      if (Utils.sum(m_ClassProbs)!=0) Utils.normalize(m_ClassProbs);
    }

    // If no enough data or this node can not be split, find next node to split.
    if (totalWeight < 2*minNumObj || branchProps[0]==0
	|| branchProps[1]==0) {
      // remove the first element
      BestFirstElements.removeElementAt(0);

      makeLeaf(data);
      if (BestFirstElements.size()!=0) {
	FastVector nextSplitElement = (FastVector)BestFirstElements.elementAt(0);
	BFTree nextSplitNode = (BFTree)nextSplitElement.elementAt(0);
	nextSplitNode.makeTree(BestFirstElements,data,
	    nextSplitNode.m_SortedIndices, nextSplitNode.m_Weights,
	    nextSplitNode.m_Dists,
	    nextSplitNode.m_ClassProbs, nextSplitNode.m_TotalWeight,
	    nextSplitNode.m_Props, minNumObj, useHeuristic, useGini, preExpansion);
      }
      return;
    }

    // If gini gain or information gain is 0, make all nodes in the BestFirstElements leaf nodes
    // because these nodes are sorted descendingly according to gini gain or information gain.
    // (namely, gini gain or information gain of all nodes in BestFirstEelements is 0).
    if (gain==0 || preExpansion==m_Expansion) {
      for (int i=0; i<BestFirstElements.size(); i++) {
	FastVector element = (FastVector)BestFirstElements.elementAt(i);
	BFTree node = (BFTree)element.elementAt(0);
	node.makeLeaf(data);
      }
      BestFirstElements.removeAllElements();
    }

    // gain is not 0
    else {
      // remove the first element
      BestFirstElements.removeElementAt(0);

      m_Attribute = att;
      if (m_Attribute.isNumeric()) m_SplitValue = splitValue;
      else m_SplitString = splitStr;

      int[][][] subsetIndices = new int[2][data.numAttributes()][0];
      double[][][] subsetWeights = new double[2][data.numAttributes()][0];

      splitData(subsetIndices, subsetWeights, m_Attribute, m_SplitValue,
	  m_SplitString, sortedIndices, weights, data);

      // If split will generate node(s) which has total weights less than m_minNumObj,
      // do not split.
      int attIndex = att.index();
      if (subsetIndices[0][attIndex].length<minNumObj ||
	  subsetIndices[1][attIndex].length<minNumObj) {
	makeLeaf(data);
      }

      // split the node
      else {
	m_isLeaf = false;
	m_Attribute = att;

	// if expansion is specified (if pruning method used)
	if (    (m_PruningStrategy == PRUNING_PREPRUNING) 
	     || (m_PruningStrategy == PRUNING_POSTPRUNING)
	     || (preExpansion != -1)) 
	  m_Expansion++;

	makeSuccessors(BestFirstElements,data,subsetIndices,subsetWeights,dists,
	    att,useHeuristic, useGini);
      }

      // choose next node to split
      if (BestFirstElements.size()!=0) {
	FastVector nextSplitElement = (FastVector)BestFirstElements.elementAt(0);
	BFTree nextSplitNode = (BFTree)nextSplitElement.elementAt(0);
	nextSplitNode.makeTree(BestFirstElements,data,
	    nextSplitNode.m_SortedIndices, nextSplitNode.m_Weights,
	    nextSplitNode.m_Dists,
	    nextSplitNode.m_ClassProbs, nextSplitNode.m_TotalWeight,
	    nextSplitNode.m_Props, minNumObj, useHeuristic, useGini, preExpansion);
      }

    }
  }

  /**
   * This method is to find the number of expansions based on internal 
   * cross-validation for just pre-pruning. It expands the first BestFirst 
   * node in the BestFirstElements if it is expansible, otherwise it looks 
   * for next exapansible node. If it finds a node is expansibel, expand the 
   * node, then return true. (note it just expands one node at a time).
   *
   * @param BestFirstElements 	list to store BFTree nodes
   * @param root 		root node of tree in each fold
   * @param train 		training data
   * @param sortedIndices 	sorted indices of the instances
   * @param weights 		weights of the instances
   * @param dists 		class distributions for each attribute
   * @param classProbs 		class probabilities of this node
   * @param totalWeight 	total weight of this node (note if the node 
   * 				can not split, this value is not calculated.)
   * @param branchProps 	proportions of two subbranches
   * @param minNumObj 	minimal number of instances at leaf nodes
   * @param useHeuristic 	if use heuristic search for nominal attributes 
   * 				in multi-class problem
   * @param useGini 		if use Gini index as splitting criterion
   * @return true 		if expand successfully, otherwise return false 
   * 				(all nodes in BestFirstElements cannot be 
   * 				expanded).
   * @throws Exception 		if something goes wrong
   */
  protected boolean makeTree(FastVector BestFirstElements, BFTree root,
      Instances train, int[][] sortedIndices, double[][] weights,
      double[][][] dists, double[] classProbs, double totalWeight,
      double[] branchProps, int minNumObj, boolean useHeuristic, boolean useGini)
  throws Exception {

    if (BestFirstElements.size()==0) return false;

    ///////////////////////////////////////////////////////////////////////
    // All information about the node to split (first BestFirst object in
    // BestFirstElements)
    FastVector firstElement = (FastVector)BestFirstElements.elementAt(0);

    // node to split
    BFTree nodeToSplit = (BFTree)firstElement.elementAt(0);

    // split attribute
    Attribute att = (Attribute)firstElement.elementAt(1);

    // info of split value or split string
    double splitValue = Double.NaN;
    String splitStr = null;
    if (att.isNumeric())
      splitValue = ((Double)firstElement.elementAt(2)).doubleValue();
    else {
      splitStr=((String)firstElement.elementAt(2)).toString();
    }

    // the best gini gain or information gain of this node
    double gain = ((Double)firstElement.elementAt(3)).doubleValue();
    ///////////////////////////////////////////////////////////////////////

    // If no enough data to split for this node or this node can not be split find next node to split.
    if (totalWeight < 2*minNumObj || branchProps[0]==0
	|| branchProps[1]==0) {
      // remove the first element
      BestFirstElements.removeElementAt(0);
      nodeToSplit.makeLeaf(train);
      BFTree nextNode = (BFTree)
      ((FastVector)BestFirstElements.elementAt(0)).elementAt(0);
      return root.makeTree(BestFirstElements, root, train,
	  nextNode.m_SortedIndices, nextNode.m_Weights, nextNode.m_Dists,
	  nextNode.m_ClassProbs, nextNode.m_TotalWeight,
	  nextNode.m_Props, minNumObj, useHeuristic, useGini);
    }

    // If gini gain or information is 0, make all nodes in the BestFirstElements leaf nodes
    // because these node sorted descendingly according to gini gain or information gain.
    // (namely, gini gain or information gain of all nodes in BestFirstEelements is 0).
    if (gain==0) {
      for (int i=0; i<BestFirstElements.size(); i++) {
	FastVector element = (FastVector)BestFirstElements.elementAt(i);
	BFTree node = (BFTree)element.elementAt(0);
	node.makeLeaf(train);
      }
      BestFirstElements.removeAllElements();
      return false;
    }

    else {
      // remove the first element
      BestFirstElements.removeElementAt(0);
      nodeToSplit.m_Attribute = att;
      if (att.isNumeric()) nodeToSplit.m_SplitValue = splitValue;
      else nodeToSplit.m_SplitString = splitStr;

      int[][][] subsetIndices = new int[2][train.numAttributes()][0];
      double[][][] subsetWeights = new double[2][train.numAttributes()][0];

      splitData(subsetIndices, subsetWeights, nodeToSplit.m_Attribute,
	  nodeToSplit.m_SplitValue, nodeToSplit.m_SplitString,
	  nodeToSplit.m_SortedIndices, nodeToSplit.m_Weights, train);

      // if split will generate node(s) which has total weights less than m_minNumObj,
      // do not split
      int attIndex = att.index();
      if (subsetIndices[0][attIndex].length<minNumObj ||
	  subsetIndices[1][attIndex].length<minNumObj) {

	nodeToSplit.makeLeaf(train);
	BFTree nextNode = (BFTree)
	((FastVector)BestFirstElements.elementAt(0)).elementAt(0);
	return root.makeTree(BestFirstElements, root, train,
	    nextNode.m_SortedIndices, nextNode.m_Weights, nextNode.m_Dists,
	    nextNode.m_ClassProbs, nextNode.m_TotalWeight,
	    nextNode.m_Props, minNumObj, useHeuristic, useGini);
      }

      // split the node
      else {
	nodeToSplit.m_isLeaf = false;
	nodeToSplit.m_Attribute = att;

	nodeToSplit.makeSuccessors(BestFirstElements,train,subsetIndices,
	    subsetWeights,dists, nodeToSplit.m_Attribute,useHeuristic,useGini);

	for (int i=0; i<2; i++){
	  nodeToSplit.m_Successors[i].makeLeaf(train);
	}

	return true;
      }
    }
  }

  /**
   * This method is to find the number of expansions based on internal 
   * cross-validation for just post-pruning. It expands the first BestFirst 
   * node in the BestFirstElements until no node can be split. When building 
   * the tree, stroe error for each temporary tree, namely for each expansion.
   *
   * @param BestFirstElements 	list to store BFTree nodes
   * @param root 		root node of tree in each fold
   * @param train 		training data in each fold
   * @param test 		test data in each fold
   * @param modelError 		list to store error for each expansion in 
   * 				each fold
   * @param sortedIndices 	sorted indices of the instances
   * @param weights 		weights of the instances
   * @param dists 		class distributions for each attribute
   * @param classProbs 		class probabilities of this node
   * @param totalWeight 	total weight of this node (note if the node 
   * 				can not split, this value is not calculated.)
   * @param branchProps 	proportions of two subbranches
   * @param minNumObj 		minimal number of instances at leaf nodes
   * @param useHeuristic 	if use heuristic search for nominal attributes 
   * 				in multi-class problem
   * @param useGini 		if use Gini index as splitting criterion
   * @param useErrorRate 	if use error rate in internal cross-validation
   * @throws Exception 		if something goes wrong
   */
  protected void makeTree(FastVector BestFirstElements, BFTree root,
      Instances train, Instances test, FastVector modelError, int[][] sortedIndices,
      double[][] weights, double[][][] dists, double[] classProbs, double totalWeight,
      double[] branchProps, int minNumObj, boolean useHeuristic, boolean useGini, boolean useErrorRate)
  throws Exception {

    if (BestFirstElements.size()==0) return;

    ///////////////////////////////////////////////////////////////////////
    // All information about the node to split (first BestFirst object in
    // BestFirstElements)
    FastVector firstElement = (FastVector)BestFirstElements.elementAt(0);

    // node to split
    //BFTree nodeToSplit = (BFTree)firstElement.elementAt(0);

    // split attribute
    Attribute att = (Attribute)firstElement.elementAt(1);

    // info of split value or split string
    double splitValue = Double.NaN;
    String splitStr = null;
    if (att.isNumeric())
      splitValue = ((Double)firstElement.elementAt(2)).doubleValue();
    else {
      splitStr=((String)firstElement.elementAt(2)).toString();
    }

    // the best gini gain or information of this node
    double gain = ((Double)firstElement.elementAt(3)).doubleValue();
    ///////////////////////////////////////////////////////////////////////

    if (totalWeight < 2*minNumObj || branchProps[0]==0
	|| branchProps[1]==0) {
      // remove the first element
      BestFirstElements.removeElementAt(0);
      makeLeaf(train);
      if (BestFirstElements.size() == 0) {
        return;
      }

      BFTree nextSplitNode = (BFTree)
      ((FastVector)BestFirstElements.elementAt(0)).elementAt(0);
      nextSplitNode.makeTree(BestFirstElements, root, train, test, modelError,
	  nextSplitNode.m_SortedIndices, nextSplitNode.m_Weights,
	  nextSplitNode.m_Dists, nextSplitNode.m_ClassProbs,
	  nextSplitNode.m_TotalWeight, nextSplitNode.m_Props, minNumObj,
	  useHeuristic, useGini, useErrorRate);
      return;

    }

    // If gini gain or information gain is 0, make all nodes in the BestFirstElements leaf nodes
    // because these node sorted descendingly according to gini gain or information gain.
    // (namely, gini gain or information gain of all nodes in BestFirstEelements is 0).
    if (gain==0) {
      for (int i=0; i<BestFirstElements.size(); i++) {
	FastVector element = (FastVector)BestFirstElements.elementAt(i);
	BFTree node = (BFTree)element.elementAt(0);
	node.makeLeaf(train);
      }
      BestFirstElements.removeAllElements();
    }

    // gini gain or information gain is not 0
    else {
      // remove the first element
      BestFirstElements.removeElementAt(0);
      m_Attribute = att;
      if (att.isNumeric()) m_SplitValue = splitValue;
      else m_SplitString = splitStr;

      int[][][] subsetIndices = new int[2][train.numAttributes()][0];
      double[][][] subsetWeights = new double[2][train.numAttributes()][0];

      splitData(subsetIndices, subsetWeights, m_Attribute,
	  m_SplitValue, m_SplitString,
	  sortedIndices, weights, train);

      // if split will generate node(s) which has total weights less than m_minNumObj,
      // do not split
      int attIndex = att.index();
      if (subsetIndices[0][attIndex].length<minNumObj ||
	  subsetIndices[1][attIndex].length<minNumObj) {
	makeLeaf(train);
      }

      // split the node and cauculate error rate of this temporary tree
      else {
	m_isLeaf = false;
	m_Attribute = att;

	makeSuccessors(BestFirstElements,train,subsetIndices,
	    subsetWeights,dists, m_Attribute, useHeuristic, useGini);
	for (int i=0; i<2; i++){
	  m_Successors[i].makeLeaf(train);
	}

	Evaluation eval = new Evaluation(test);
	eval.evaluateModel(root, test);
	double error;
	if (useErrorRate) error = eval.errorRate();
	else error = eval.rootMeanSquaredError();
	modelError.addElement(new Double(error));
      }

      if (BestFirstElements.size()!=0) {
	FastVector nextSplitElement = (FastVector)BestFirstElements.elementAt(0);
	BFTree nextSplitNode = (BFTree)nextSplitElement.elementAt(0);
	nextSplitNode.makeTree(BestFirstElements, root, train, test, modelError,
	    nextSplitNode.m_SortedIndices, nextSplitNode.m_Weights,
	    nextSplitNode.m_Dists, nextSplitNode.m_ClassProbs,
	    nextSplitNode.m_TotalWeight, nextSplitNode.m_Props, minNumObj,
	    useHeuristic, useGini,useErrorRate);
      }
    }
  }


  /**
   * Generate successor nodes for a node and put them into BestFirstElements 
   * according to gini gain or information gain in a descending order.
   *
   * @param BestFirstElements 	list to store BestFirst nodes
   * @param data 		training instance
   * @param subsetSortedIndices	sorted indices of instances of successor nodes
   * @param subsetWeights 	weights of instances of successor nodes
   * @param dists 		class distributions of successor nodes
   * @param att 		attribute used to split the node
   * @param useHeuristic 	if use heuristic search for nominal attributes in multi-class problem
   * @param useGini 		if use Gini index as splitting criterion
   * @throws Exception 		if something goes wrong 
   */
  protected void makeSuccessors(FastVector BestFirstElements,Instances data,
      int[][][] subsetSortedIndices, double[][][] subsetWeights,
      double[][][] dists,
      Attribute att, boolean useHeuristic, boolean useGini) throws Exception {

    m_Successors = new BFTree[2];

    for (int i=0; i<2; i++) {
      m_Successors[i] = new BFTree();
      m_Successors[i].m_isLeaf = true;

      // class probability and distribution for this successor node
      m_Successors[i].m_ClassProbs = new double[data.numClasses()];
      m_Successors[i].m_Distribution = new double[data.numClasses()];
      System.arraycopy(dists[att.index()][i], 0, m_Successors[i].m_ClassProbs,
	  0,m_Successors[i].m_ClassProbs.length);
      System.arraycopy(dists[att.index()][i], 0, m_Successors[i].m_Distribution,
	  0,m_Successors[i].m_Distribution.length);
      if (Utils.sum(m_Successors[i].m_ClassProbs)!=0)
	Utils.normalize(m_Successors[i].m_ClassProbs);

      // split information for this successor node
      double[][] props = new double[data.numAttributes()][2];
      double[][][] subDists = new double[data.numAttributes()][2][data.numClasses()];
      double[][] totalSubsetWeights = new double[data.numAttributes()][2];
      FastVector splitInfo = m_Successors[i].computeSplitInfo(m_Successors[i], data,
	  subsetSortedIndices[i], subsetWeights[i], subDists, props,
	  totalSubsetWeights, useHeuristic, useGini);

      // branch proportion for this successor node
      int splitIndex = ((Attribute)splitInfo.elementAt(1)).index();
      m_Successors[i].m_Props = new double[2];
      System.arraycopy(props[splitIndex], 0, m_Successors[i].m_Props, 0,
	  m_Successors[i].m_Props.length);

      // sorted indices and weights of each attribute for this successor node
      m_Successors[i].m_SortedIndices = new int[data.numAttributes()][0];
      m_Successors[i].m_Weights = new double[data.numAttributes()][0];
      for (int j=0; j<m_Successors[i].m_SortedIndices.length; j++) {
	m_Successors[i].m_SortedIndices[j] = subsetSortedIndices[i][j];
	m_Successors[i].m_Weights[j] = subsetWeights[i][j];
      }

      // distribution of each attribute for this successor node
      m_Successors[i].m_Dists = new double[data.numAttributes()][2][data.numClasses()];
      for (int j=0; j<subDists.length; j++) {
	m_Successors[i].m_Dists[j] = subDists[j];
      }

      // total weights for this successor node. 
      m_Successors[i].m_TotalWeight = Utils.sum(totalSubsetWeights[splitIndex]);

      // insert this successor node into BestFirstElements according to gini gain or information gain
      //  descendingly
      if (BestFirstElements.size()==0) {
	BestFirstElements.addElement(splitInfo);
      } else {
	double gGain = ((Double)(splitInfo.elementAt(3))).doubleValue();
	int vectorSize = BestFirstElements.size();
	FastVector lastNode = (FastVector)BestFirstElements.elementAt(vectorSize-1);

	// If gini gain is less than that of last node in FastVector
	if (gGain<((Double)(lastNode.elementAt(3))).doubleValue()) {
	  BestFirstElements.insertElementAt(splitInfo, vectorSize);
	} else {
	  for (int j=0; j<vectorSize; j++) {
	    FastVector node = (FastVector)BestFirstElements.elementAt(j);
	    double nodeGain = ((Double)(node.elementAt(3))).doubleValue();
	    if (gGain>=nodeGain) {
	      BestFirstElements.insertElementAt(splitInfo, j);
	      break;
	    }
	  }
	}
      }
    }
  }

  /**
   * Compute sorted indices, weights and class probabilities for a given 
   * dataset. Return total weights of the data at the node.
   * 
   * @param data 		training data
   * @param sortedIndices 	sorted indices of instances at the node
   * @param weights 		weights of instances at the node
   * @param classProbs 		class probabilities at the node
   * @return 			total weights of instances at the node
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
	// missing values instances are put to end (through Utils.sort() method)
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

    // Compute initial class counts and total weight
    double totalWeight = 0;
    for (int i = 0; i < data.numInstances(); i++) {
      Instance inst = data.instance(i);
      classProbs[(int)inst.classValue()] += inst.weight();
      totalWeight += inst.weight();
    }

    return totalWeight;
  }

  /**
   * Compute the best splitting attribute, split point or subset and the best
   * gini gain or iformation gain for a given dataset.
   *
   * @param node 		node to be split
   * @param data 		training data
   * @param sortedIndices 	sorted indices of the instances
   * @param weights 		weights of the instances
   * @param dists 		class distributions for each attribute
   * @param props 		proportions of two branches
   * @param totalSubsetWeights 	total weight of two subsets
   * @param useHeuristic 	if use heuristic search for nominal attributes 
   * 				in multi-class problem
   * @param useGini 		if use Gini index as splitting criterion
   * @return 			split information about the node
   * @throws Exception 		if something is wrong
   */
  protected FastVector computeSplitInfo(BFTree node, Instances data, int[][] sortedIndices,
      double[][] weights, double[][][] dists, double[][] props,
      double[][] totalSubsetWeights, boolean useHeuristic, boolean useGini) throws Exception {

    double[] splits = new double[data.numAttributes()];
    String[] splitString = new String[data.numAttributes()];
    double[] gains = new double[data.numAttributes()];

    for (int i = 0; i < data.numAttributes(); i++) {
      if (i==data.classIndex()) continue;
      Attribute att = data.attribute(i);
      if (att.isNumeric()) {
	// numeric attribute
	splits[i] = numericDistribution(props, dists, att, sortedIndices[i],
	    weights[i], totalSubsetWeights, gains, data, useGini);
      } else {
	// nominal attribute
	splitString[i] = nominalDistribution(props, dists, att, sortedIndices[i],
	    weights[i], totalSubsetWeights, gains, data, useHeuristic, useGini);
      }
    }

    int index = Utils.maxIndex(gains);
    double mBestGain = gains[index];

    Attribute att = data.attribute(index);
    double mValue =Double.NaN;
    String mString = null;
    if (att.isNumeric())  mValue= splits[index];
    else {
      mString = splitString[index];
      if (mString==null) mString = "";
    }

    // split information
    FastVector splitInfo = new FastVector();
    splitInfo.addElement(node);
    splitInfo.addElement(att);
    if (att.isNumeric()) splitInfo.addElement(new Double(mValue));
    else splitInfo.addElement(mString);
    splitInfo.addElement(new Double(mBestGain));

    return splitInfo;
  }

  /**
   * Compute distributions, proportions and total weights of two successor nodes for 
   * a given numeric attribute.
   *
   * @param props 		proportions of each two branches for each attribute
   * @param dists 		class distributions of two branches for each attribute
   * @param att 		numeric att split on
   * @param sortedIndices 	sorted indices of instances for the attirubte
   * @param weights 		weights of instances for the attirbute
   * @param subsetWeights 	total weight of two branches split based on the attribute
   * @param gains 		Gini gains or information gains for each attribute 
   * @param data 		training instances
   * @param useGini 		if use Gini index as splitting criterion
   * @return 			Gini gain or information gain for the given attribute
   * @throws Exception 		if something goes wrong
   */
  protected double numericDistribution(double[][] props, double[][][] dists,
      Attribute att, int[] sortedIndices, double[] weights, double[][] subsetWeights,
      double[] gains, Instances data, boolean useGini)
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
    double currGain;
    double bestGain = -Double.MAX_VALUE;

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

	if (useGini) currGain = computeGiniGain(parentDist,tempDist);
	else currGain = computeInfoGain(parentDist,tempDist);

	if (currGain > bestGain) {
	  bestGain = currGain;
	  // clean split point
	  splitPoint = Math.rint((inst.value(att) + currSplit)/2.0*100000)/100000.0;
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

    // clean gain
    gains[attIndex] = Math.rint(bestGain*10000000)/10000000.0;
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
   * @param gains 		Gini gains for each attribute 
   * @param data 		training instances
   * @param useHeuristic 	if use heuristic search
   * @param useGini 		if use Gini index as splitting criterion
   * @return 			Gini gain for the given attribute
   * @throws Exception 		if something goes wrong
   */
  protected String nominalDistribution(double[][] props, double[][][] dists,
      Attribute att, int[] sortedIndices, double[] weights, double[][] subsetWeights,
      double[] gains, Instances data, boolean useHeuristic, boolean useGini)
  throws Exception {

    String[] values = new String[att.numValues()];
    int numCat = values.length; // number of values of the attribute
    int numClasses = data.numClasses();

    String bestSplitString = "";
    double bestGain = -Double.MAX_VALUE;

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

    // attribute values which class frequency is not 0
    String[] nonEmptyValues = new String[nonEmpty];
    int nonEmptyIndex = 0;
    for (int j=0; j<numCat; j++) {
      if (classFreq[j]!=0) {
	nonEmptyValues[nonEmptyIndex] = att.value(j);
	nonEmptyIndex ++;
      }
    }

    // attribute values which class frequency is 0
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
      gains[att.index()] = 0;
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

      // sort category according to the probability of class 0.0
      String[] sortedValues = new String[nonEmpty];
      for (int j=0; j<nonEmpty; j++) {
	sortedValues[j] = nonEmptyValues[Utils.minIndex(pClass0)];
	pClass0[Utils.minIndex(pClass0)] = Double.MAX_VALUE;
      }

      // Find a subset of attribute values that maximize impurity decrease

      // for the attribute values that class frequency is not 0
      String tempStr = "";

      for (int j=0; j<nonEmpty-1; j++) {
	currDist = new double[2][numClasses];
	if (tempStr=="") tempStr="(" + sortedValues[j] + ")";
	else tempStr += "|"+ "(" + sortedValues[j] + ")";
	//System.out.println(sortedValues[j]);
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

	double currGain;
	if (useGini) currGain = computeGiniGain(parentDist,tempDist);
	else currGain = computeInfoGain(parentDist,tempDist);

	if (currGain>bestGain) {
	  bestGain = currGain;
	  bestSplitString = tempStr;
	  for (int jj = 0; jj < 2; jj++) {
	    System.arraycopy(tempDist[jj], 0, dist[jj], 0,
		dist[jj].length);
	  }
	}
      }
    }

    // multi-class problems (exhaustive search)
    else if (!useHeuristic || nonEmpty<=4) {
      //else if (!useHeuristic || nonEmpty==2) {

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

	double currGain;
	if (useGini) currGain = computeGiniGain(parentDist,tempDist);
	else currGain = computeInfoGain(parentDist,tempDist);

	if (currGain>bestGain) {
	  bestGain = currGain;
	  bestSplitString = tempStr;
	  for (int j = 0; j < 2; j++) {
	    //dist[jj] = new double[currDist[jj].length];
	    System.arraycopy(tempDist[j], 0, dist[j], 0,
		dist[j].length);
	  }
	}
      }
    }

    // huristic method to solve multi-classes problems
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

	double currGain;
	if (useGini) currGain = computeGiniGain(parentDist,tempDist);
	else currGain = computeInfoGain(parentDist,tempDist);

	if (currGain>bestGain) {
	  bestGain = currGain;
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

    // clean gain
    gains[attIndex] = Math.rint(bestGain*10000000)/10000000.0;

    dists[attIndex] = dist;
    return bestSplitString;
  }


  /**
   * Split data into two subsets and store sorted indices and weights for two
   * successor nodes.
   *
   * @param subsetIndices 	sorted indecis of instances for each attribute for two successor node
   * @param subsetWeights 	weights of instances for each attribute for two successor node
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
   * Compute and return information gain for given distributions of a node 
   * and its successor nodes.
   * 
   * @param parentDist 	class distributions of parent node
   * @param childDist 	class distributions of successor nodes
   * @return 		information gain computed
   */
  protected double computeInfoGain(double[] parentDist, double[][] childDist) {
    double totalWeight = Utils.sum(parentDist);
    if (totalWeight==0) return 0;

    double leftWeight = Utils.sum(childDist[0]);
    double rightWeight = Utils.sum(childDist[1]);

    double parentInfo = computeEntropy(parentDist, totalWeight);
    double leftInfo = computeEntropy(childDist[0],leftWeight);
    double rightInfo = computeEntropy(childDist[1], rightWeight);

    return parentInfo - leftWeight/totalWeight*leftInfo -
    rightWeight/totalWeight*rightInfo;
  }

  /**
   * Compute and return entropy for a given distribution of a node.
   * 
   * @param dist 	class distributions
   * @param total 	class distributions
   * @return 		entropy of the class distributions
   */
  protected double computeEntropy(double[] dist, double total) {
    if (total==0) return 0;
    double entropy = 0;
    for (int i=0; i<dist.length; i++) {
      if (dist[i]!=0) entropy -= dist[i]/total * Utils.log2(dist[i]/total);
    }
    return entropy;
  }

  /**
   * Make the node leaf node.
   * 
   * @param data 	training data
   */
  protected void makeLeaf(Instances data) {
    m_Attribute = null;
    m_isLeaf = true;
    m_ClassValue=Utils.maxIndex(m_ClassProbs);
    m_ClassAttribute = data.classAttribute();
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
   * Prints the decision tree using the protected toString method from below.
   * 
   * @return 		a textual description of the classifier
   */
  public String toString() {
    if ((m_Distribution == null) && (m_Successors == null)) {
      return "Best-First: No model built yet.";
    }
    return "Best-First Decision Tree\n" + toString(0)+"\n\n"
    +"Size of the Tree: "+numNodes()+"\n\n"
    +"Number of Leaf Nodes: "+numLeaves();
  }

  /**
   * Outputs a tree at a certain level.
   * 
   * @param level 	the level at which the tree is to be printed
   * @return 		a tree at a certain level.
   */
  protected String toString(int level) {
    StringBuffer text = new StringBuffer();
    // if leaf nodes
    if (m_Attribute == null) {
      if (Instance.isMissingValue(m_ClassValue)) {
	text.append(": null");
      } else {
	double correctNum = Math.rint(m_Distribution[Utils.maxIndex(m_Distribution)]*100)/
	100.0;
	double wrongNum = Math.rint((Utils.sum(m_Distribution) -
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
   * @return 		an enumeration describing the available options.
   */
  public Enumeration listOptions() {
    Vector 		result;
    Enumeration		en;
    
    result = new Vector();

    en = super.listOptions();
    while (en.hasMoreElements())
      result.addElement(en.nextElement());

    result.addElement(new Option(
	"\tThe pruning strategy.\n"
	+ "\t(default: " + new SelectedTag(PRUNING_POSTPRUNING, TAGS_PRUNING) + ")",
	"P", 1, "-P " + Tag.toOptionList(TAGS_PRUNING)));

    result.addElement(new Option(
	"\tThe minimal number of instances at the terminal nodes.\n" 
	+ "\t(default 2)",
	"M", 1, "-M <min no>"));
    
    result.addElement(new Option(
	"\tThe number of folds used in the pruning.\n"
	+ "\t(default 5)",
	"N", 5, "-N <num folds>"));
    
    result.addElement(new Option(
	"\tDon't use heuristic search for nominal attributes in multi-class\n"
	+ "\tproblem (default yes).\n",
	"H", 0, "-H"));
    
    result.addElement(new Option(
	"\tDon't use Gini index for splitting (default yes),\n"
	+ "\tif not information is used.", 
	"G", 0, "-G"));
    
    result.addElement(new Option(
	"\tDon't use error rate in internal cross-validation (default yes), \n"
	+ "\tbut root mean squared error.", 
	"R", 0, "-R"));
    
    result.addElement(new Option(
	"\tUse the 1 SE rule to make pruning decision.\n"
	+ "\t(default no).", 
	"A", 0, "-A"));
    
    result.addElement(new Option(
	"\tPercentage of training data size (0-1]\n"
	+ "\t(default 1).", 
	"C", 0, "-C"));

    return result.elements();
  }

  /**
   * Parses the options for this object. <p/>
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
   * <pre> -P &lt;UNPRUNED|POSTPRUNED|PREPRUNED&gt;
   *  The pruning strategy.
   *  (default: POSTPRUNED)</pre>
   * 
   * <pre> -M &lt;min no&gt;
   *  The minimal number of instances at the terminal nodes.
   *  (default 2)</pre>
   * 
   * <pre> -N &lt;num folds&gt;
   *  The number of folds used in the pruning.
   *  (default 5)</pre>
   * 
   * <pre> -H
   *  Don't use heuristic search for nominal attributes in multi-class
   *  problem (default yes).
   * </pre>
   * 
   * <pre> -G
   *  Don't use Gini index for splitting (default yes),
   *  if not information is used.</pre>
   * 
   * <pre> -R
   *  Don't use error rate in internal cross-validation (default yes), 
   *  but root mean squared error.</pre>
   * 
   * <pre> -A
   *  Use the 1 SE rule to make pruning decision.
   *  (default no).</pre>
   * 
   * <pre> -C
   *  Percentage of training data size (0-1]
   *  (default 1).</pre>
   * 
   <!-- options-end -->
   *
   * @param options	the options to use
   * @throws Exception	if setting of options fails
   */
  public void setOptions(String[] options) throws Exception {
    String 	tmpStr;
    
    super.setOptions(options);

    tmpStr = Utils.getOption('M', options);
    if (tmpStr.length() != 0) 
      setMinNumObj(Integer.parseInt(tmpStr));
    else
      setMinNumObj(2);

    tmpStr = Utils.getOption('N', options);
    if (tmpStr.length() != 0)
      setNumFoldsPruning(Integer.parseInt(tmpStr));
    else
      setNumFoldsPruning(5);

    tmpStr = Utils.getOption('C', options);
    if (tmpStr.length()!=0)
      setSizePer(Double.parseDouble(tmpStr));
    else
      setSizePer(1);

    tmpStr = Utils.getOption('P', options);
    if (tmpStr.length() != 0)
      setPruningStrategy(new SelectedTag(tmpStr, TAGS_PRUNING));
    else
      setPruningStrategy(new SelectedTag(PRUNING_POSTPRUNING, TAGS_PRUNING));

    setHeuristic(!Utils.getFlag('H',options));

    setUseGini(!Utils.getFlag('G',options));
    
    setUseErrorRate(!Utils.getFlag('R',options));
    
    setUseOneSE(Utils.getFlag('A',options));
  }

  /**
   * Gets the current settings of the Classifier.
   * 
   * @return 		the current settings of the Classifier
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

    if (!getHeuristic())
      result.add("-H");

    if (!getUseGini())
      result.add("-G");

    if (!getUseErrorRate())
      result.add("-R");

    if (getUseOneSE())
      result.add("-A");

    result.add("-C");
    result.add("" + getSizePer());

    result.add("-P");
    result.add("" + getPruningStrategy());

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
   * Returns the value of the named measure
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
	  + " not supported (Best-First)");
    }
  }

  /**
   * Returns the tip text for this property
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String pruningStrategyTipText() {
    return "Sets the pruning strategy.";
  }

  /**
   * Sets the pruning strategy. 
   *
   * @param value 	the strategy
   */
  public void setPruningStrategy(SelectedTag value) {
    if (value.getTags() == TAGS_PRUNING) {
      m_PruningStrategy = value.getSelectedTag().getID();
    }
  }

  /**
   * Gets the pruning strategy. 
   *
   * @return 		the current strategy.
   */
  public SelectedTag getPruningStrategy() {
    return new SelectedTag(m_PruningStrategy, TAGS_PRUNING);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String minNumObjTipText() {
    return "Set minimal number of instances at the terminal nodes.";
  }

  /**
   * Set minimal number of instances at the terminal nodes.
   * 
   * @param value 	minimal number of instances at the terminal nodes
   */
  public void setMinNumObj(int value) {
    m_minNumObj = value;
  }

  /**
   * Get minimal number of instances at the terminal nodes.
   * 
   * @return 		minimal number of instances at the terminal nodes
   */
  public int getMinNumObj() {
    return m_minNumObj;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String numFoldsPruningTipText() {
    return "Number of folds in internal cross-validation.";
  }

  /**
   * Set number of folds in internal cross-validation.
   * 
   * @param value 	the number of folds
   */
  public void setNumFoldsPruning(int value) {
    m_numFoldsPruning = value;
  }

  /**
   * Set number of folds in internal cross-validation.
   * 
   * @return 		number of folds in internal cross-validation
   */
  public int getNumFoldsPruning() {
    return m_numFoldsPruning;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui.
   */
  public String heuristicTipText() {
    return "If heuristic search is used for binary split for nominal attributes.";
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
  public boolean getHeuristic() {
    return m_Heuristic;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui.
   */
  public String useGiniTipText() {
    return "If true the Gini index is used for splitting criterion, otherwise the information is used.";
  }

  /**
   * Set if use Gini index as splitting criterion.
   * 
   * @param value 	if use Gini index splitting criterion
   */
  public void setUseGini(boolean value) {
    m_UseGini = value;
  }

  /**
   * Get if use Gini index as splitting criterion.
   * 
   * @return 		if use Gini index as splitting criterion
   */
  public boolean getUseGini() {
    return m_UseGini;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui.
   */
  public String useErrorRateTipText() {
    return "If error rate is used as error estimate. if not, root mean squared error is used.";
  }

  /**
   * Set if use error rate in internal cross-validation.
   * 
   * @param value 	if use error rate in internal cross-validation
   */
  public void setUseErrorRate(boolean value) {
    m_UseErrorRate = value;
  }

  /**
   * Get if use error rate in internal cross-validation.
   * 
   * @return 		if use error rate in internal cross-validation.
   */
  public boolean getUseErrorRate() {
    return m_UseErrorRate;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui.
   */
  public String useOneSETipText() {
    return "Use the 1SE rule to make pruning decision.";
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
   * Main method.
   *
   * @param args the options for the classifier
   */
  public static void main(String[] args) {
    runClassifier(new BFTree(), args);
  }
}
