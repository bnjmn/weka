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
 *    ADTree.java
 *    Copyright (C) 2001 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.trees;

import weka.classifiers.Classifier;
import weka.classifiers.IterativeClassifier;
import weka.classifiers.trees.adtree.PredictionNode;
import weka.classifiers.trees.adtree.ReferenceInstances;
import weka.classifiers.trees.adtree.Splitter;
import weka.classifiers.trees.adtree.TwoWayNominalSplit;
import weka.classifiers.trees.adtree.TwoWayNumericSplit;
import weka.core.AdditionalMeasureProducer;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Drawable;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.SerializedObject;
import weka.core.Tag;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;

import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * Class for generating an alternating decision tree. The basic algorithm is based on:<br/>
 * <br/>
 * Freund, Y., Mason, L.: The alternating decision tree learning algorithm. In: Proceeding of the Sixteenth International Conference on Machine Learning, Bled, Slovenia, 124-133, 1999.<br/>
 * <br/>
 * This version currently only supports two-class problems. The number of boosting iterations needs to be manually tuned to suit the dataset and the desired complexity/accuracy tradeoff. Induction of the trees has been optimized, and heuristic search methods have been introduced to speed learning.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{Freund1999,
 *    address = {Bled, Slovenia},
 *    author = {Freund, Y. and Mason, L.},
 *    booktitle = {Proceeding of the Sixteenth International Conference on Machine Learning},
 *    pages = {124-133},
 *    title = {The alternating decision tree learning algorithm},
 *    year = {1999}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -B &lt;number of boosting iterations&gt;
 *  Number of boosting iterations.
 *  (Default = 10)</pre>
 * 
 * <pre> -E &lt;-3|-2|-1|&gt;=0&gt;
 *  Expand nodes: -3(all), -2(weight), -1(z_pure), &gt;=0 seed for random walk
 *  (Default = -3)</pre>
 * 
 * <pre> -D
 *  Save the instance data with the model</pre>
 * 
 <!-- options-end -->
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Bernhard Pfahringer (bernhard@cs.waikato.ac.nz)
 * @version $Revision$
 */
public class ADTree
  extends Classifier 
  implements OptionHandler, Drawable, AdditionalMeasureProducer,
             WeightedInstancesHandler, IterativeClassifier, 
             TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = -1532264837167690683L;
  
  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return  "Class for generating an alternating decision tree. The basic "
      + "algorithm is based on:\n\n"
      + getTechnicalInformation().toString() + "\n\n"
      + "This version currently only supports two-class problems. The number of boosting "
      + "iterations needs to be manually tuned to suit the dataset and the desired "
      + "complexity/accuracy tradeoff. Induction of the trees has been optimized, and heuristic "
      + "search methods have been introduced to speed learning.";
  }

  /** search mode: Expand all paths */
  public static final int SEARCHPATH_ALL = 0;
  /** search mode: Expand the heaviest path */
  public static final int SEARCHPATH_HEAVIEST = 1;
  /** search mode: Expand the best z-pure path */
  public static final int SEARCHPATH_ZPURE = 2;
  /** search mode: Expand a random path */
  public static final int SEARCHPATH_RANDOM = 3;
  /** The search modes */
  public static final Tag [] TAGS_SEARCHPATH = {
    new Tag(SEARCHPATH_ALL, "Expand all paths"),
    new Tag(SEARCHPATH_HEAVIEST, "Expand the heaviest path"),
    new Tag(SEARCHPATH_ZPURE, "Expand the best z-pure path"),
    new Tag(SEARCHPATH_RANDOM, "Expand a random path")
  };

  /** The instances used to train the tree */
  protected Instances m_trainInstances;

  /** The root of the tree */
  protected PredictionNode m_root = null;

  /** The random number generator - used for the random search heuristic */
  protected Random m_random = null; 

  /** The number of the last splitter added to the tree */
  protected int m_lastAddedSplitNum = 0;

  /** An array containing the inidices to the numeric attributes in the data */
  protected int[] m_numericAttIndices;

  /** An array containing the inidices to the nominal attributes in the data */
  protected int[] m_nominalAttIndices;

  /** The total weight of the instances - used to speed Z calculations */
  protected double m_trainTotalWeight;

  /** The training instances with positive class - referencing the training dataset */
  protected ReferenceInstances m_posTrainInstances;

  /** The training instances with negative class - referencing the training dataset */
  protected ReferenceInstances m_negTrainInstances;

  /** The best node to insert under, as found so far by the latest search */
  protected PredictionNode m_search_bestInsertionNode;

  /** The best splitter to insert, as found so far by the latest search */
  protected Splitter m_search_bestSplitter;

  /** The smallest Z value found so far by the latest search */
  protected double m_search_smallestZ;

  /** The positive instances that apply to the best path found so far */
  protected Instances m_search_bestPathPosInstances;

  /** The negative instances that apply to the best path found so far */
  protected Instances m_search_bestPathNegInstances;

  /** Statistics - the number of prediction nodes investigated during search */
  protected int m_nodesExpanded = 0;

  /** Statistics - the number of instances processed during search */
  protected int m_examplesCounted = 0;

  /** Option - the number of boosting iterations o perform */
  protected int m_boostingIterations = 10;

  /** Option - the search mode */
  protected int m_searchPath = 0;

  /** Option - the seed to use for a random search */
  protected int m_randomSeed = 0; 

  /** Option - whether the tree should remember the instance data */
  protected boolean m_saveInstanceData = false; 

  /**
   * Returns an instance of a TechnicalInformation object, containing 
   * detailed information about the technical background of this class,
   * e.g., paper reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation 	result;
    
    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "Freund, Y. and Mason, L.");
    result.setValue(Field.YEAR, "1999");
    result.setValue(Field.TITLE, "The alternating decision tree learning algorithm");
    result.setValue(Field.BOOKTITLE, "Proceeding of the Sixteenth International Conference on Machine Learning");
    result.setValue(Field.ADDRESS, "Bled, Slovenia");
    result.setValue(Field.PAGES, "124-133");
    
    return result;
  }

  /**
   * Sets up the tree ready to be trained, using two-class optimized method.
   *
   * @param instances the instances to train the tree with
   * @exception Exception if training data is unsuitable
   */
  public void initClassifier(Instances instances) throws Exception {

    // clear stats
    m_nodesExpanded = 0;
    m_examplesCounted = 0;
    m_lastAddedSplitNum = 0;

    // prepare the random generator
    m_random = new Random(m_randomSeed);

    // create training set
    m_trainInstances = new Instances(instances);

    // create positive/negative subsets
    m_posTrainInstances = new ReferenceInstances(m_trainInstances,
						 m_trainInstances.numInstances());
    m_negTrainInstances = new ReferenceInstances(m_trainInstances,
						 m_trainInstances.numInstances());
    for (Enumeration e = m_trainInstances.enumerateInstances(); e.hasMoreElements(); ) {
      Instance inst = (Instance) e.nextElement();
      if ((int) inst.classValue() == 0)
	m_negTrainInstances.addReference(inst); // belongs in negative class
      else
	m_posTrainInstances.addReference(inst); // belongs in positive class
    }
    m_posTrainInstances.compactify();
    m_negTrainInstances.compactify();

    // create the root prediction node
    double rootPredictionValue = calcPredictionValue(m_posTrainInstances,
						     m_negTrainInstances);
    m_root = new PredictionNode(rootPredictionValue);

    // pre-adjust weights
    updateWeights(m_posTrainInstances, m_negTrainInstances, rootPredictionValue);
    
    // pre-calculate what we can
    generateAttributeIndicesSingle();
  }

  /**
   * Performs one iteration.
   * 
   * @param iteration the index of the current iteration (0-based)
   * @exception Exception if this iteration fails 
   */  
  public void next(int iteration) throws Exception {

    boost();
  }

  /**
   * Performs a single boosting iteration, using two-class optimized method.
   * Will add a new splitter node and two prediction nodes to the tree
   * (unless merging takes place).
   *
   * @exception Exception if try to boost without setting up tree first or there are no 
   * instances to train with
   */
  public void boost() throws Exception {

    if (m_trainInstances == null || m_trainInstances.numInstances() == 0)
      throw new Exception("Trying to boost with no training data");

    // perform the search
    searchForBestTestSingle();

    if (m_search_bestSplitter == null) return; // handle empty instances

    // create the new nodes for the tree, updating the weights
    for (int i=0; i<2; i++) {
      Instances posInstances =
	m_search_bestSplitter.instancesDownBranch(i, m_search_bestPathPosInstances);
      Instances negInstances =
	m_search_bestSplitter.instancesDownBranch(i, m_search_bestPathNegInstances);
      double predictionValue = calcPredictionValue(posInstances, negInstances);
      PredictionNode newPredictor = new PredictionNode(predictionValue);
      updateWeights(posInstances, negInstances, predictionValue);
      m_search_bestSplitter.setChildForBranch(i, newPredictor);
    }

    // insert the new nodes
    m_search_bestInsertionNode.addChild((Splitter) m_search_bestSplitter, this);

    // free memory
    m_search_bestPathPosInstances = null;
    m_search_bestPathNegInstances = null;
    m_search_bestSplitter = null;
  }

  /**
   * Generates the m_nominalAttIndices and m_numericAttIndices arrays to index
   * the respective attribute types in the training data.
   *
   */
  private void generateAttributeIndicesSingle() {

    // insert indices into vectors
    FastVector nominalIndices = new FastVector();
    FastVector numericIndices = new FastVector();

    for (int i=0; i<m_trainInstances.numAttributes(); i++) {
      if (i != m_trainInstances.classIndex()) {
	if (m_trainInstances.attribute(i).isNumeric())
	  numericIndices.addElement(new Integer(i));
	else
	  nominalIndices.addElement(new Integer(i));
      }
    }

    // create nominal array
    m_nominalAttIndices = new int[nominalIndices.size()];
    for (int i=0; i<nominalIndices.size(); i++)
      m_nominalAttIndices[i] = ((Integer)nominalIndices.elementAt(i)).intValue();
    
    // create numeric array
    m_numericAttIndices = new int[numericIndices.size()];
    for (int i=0; i<numericIndices.size(); i++)
      m_numericAttIndices[i] = ((Integer)numericIndices.elementAt(i)).intValue();
  }

  /**
   * Performs a search for the best test (splitter) to add to the tree, by aiming to
   * minimize the Z value.
   *
   * @exception Exception if search fails
   */
  private void searchForBestTestSingle() throws Exception {

    // keep track of total weight for efficient wRemainder calculations
    m_trainTotalWeight = m_trainInstances.sumOfWeights();
    
    m_search_smallestZ = Double.POSITIVE_INFINITY;
    searchForBestTestSingle(m_root, m_posTrainInstances, m_negTrainInstances);
  }

  /**
   * Recursive function that carries out search for the best test (splitter) to add to
   * this part of the tree, by aiming to minimize the Z value. Performs Z-pure cutoff to
   * reduce search space.
   *
   * @param currentNode the root of the subtree to be searched, and the current node 
   * being considered as parent of a new split
   * @param posInstances the positive-class instances that apply at this node
   * @param negInstances the negative-class instances that apply at this node
   * @exception Exception if search fails
   */
  private void searchForBestTestSingle(PredictionNode currentNode,
				       Instances posInstances, Instances negInstances)
    throws Exception {

    // don't investigate pure or empty nodes any further
    if (posInstances.numInstances() == 0 || negInstances.numInstances() == 0) return;

    // do z-pure cutoff
    if (calcZpure(posInstances, negInstances) >= m_search_smallestZ) return;

    // keep stats
    m_nodesExpanded++;
    m_examplesCounted += posInstances.numInstances() + negInstances.numInstances();

    // evaluate static splitters (nominal)
    for (int i=0; i<m_nominalAttIndices.length; i++)
      evaluateNominalSplitSingle(m_nominalAttIndices[i], currentNode,
				 posInstances, negInstances);

    // evaluate dynamic splitters (numeric)
    if (m_numericAttIndices.length > 0) {

      // merge the two sets of instances into one
      Instances allInstances = new Instances(posInstances);
      for (Enumeration e = negInstances.enumerateInstances(); e.hasMoreElements(); )
	allInstances.add((Instance) e.nextElement());
    
      // use method of finding the optimal Z split-point
      for (int i=0; i<m_numericAttIndices.length; i++)
	evaluateNumericSplitSingle(m_numericAttIndices[i], currentNode,
				   posInstances, negInstances, allInstances);
    }

    if (currentNode.getChildren().size() == 0) return;

    // keep searching
    switch (m_searchPath) {
    case SEARCHPATH_ALL:
      goDownAllPathsSingle(currentNode, posInstances, negInstances);
      break;
    case SEARCHPATH_HEAVIEST: 
      goDownHeaviestPathSingle(currentNode, posInstances, negInstances);
      break;
    case SEARCHPATH_ZPURE: 
      goDownZpurePathSingle(currentNode, posInstances, negInstances);
      break;
    case SEARCHPATH_RANDOM: 
      goDownRandomPathSingle(currentNode, posInstances, negInstances);
      break;
    }
  }

  /**
   * Continues single (two-class optimized) search by investigating every node in the
   * subtree under currentNode.
   *
   * @param currentNode the root of the subtree to be searched
   * @param posInstances the positive-class instances that apply at this node
   * @param negInstances the negative-class instances that apply at this node
   * @exception Exception if search fails
   */
  private void goDownAllPathsSingle(PredictionNode currentNode,
				    Instances posInstances, Instances negInstances)
    throws Exception {

    for (Enumeration e = currentNode.children(); e.hasMoreElements(); ) {
      Splitter split = (Splitter) e.nextElement();
      for (int i=0; i<split.getNumOfBranches(); i++)
	searchForBestTestSingle(split.getChildForBranch(i),
				split.instancesDownBranch(i, posInstances),
				split.instancesDownBranch(i, negInstances));
    }
  }

  /**
   * Continues single (two-class optimized) search by investigating only the path
   * with the most heavily weighted instances.
   *
   * @param currentNode the root of the subtree to be searched
   * @param posInstances the positive-class instances that apply at this node
   * @param negInstances the negative-class instances that apply at this node
   * @exception Exception if search fails
   */
  private void goDownHeaviestPathSingle(PredictionNode currentNode,
					Instances posInstances, Instances negInstances)
    throws Exception {

    Splitter heaviestSplit = null;
    int heaviestBranch = 0;
    double largestWeight = 0.0;
    for (Enumeration e = currentNode.children(); e.hasMoreElements(); ) {
      Splitter split = (Splitter) e.nextElement();
      for (int i=0; i<split.getNumOfBranches(); i++) {
	double weight =
	  split.instancesDownBranch(i, posInstances).sumOfWeights() +
	  split.instancesDownBranch(i, negInstances).sumOfWeights();
	if (weight > largestWeight) {
	  heaviestSplit = split;
	  heaviestBranch = i;
	  largestWeight = weight;
	}
      }
    }
    if (heaviestSplit != null)
      searchForBestTestSingle(heaviestSplit.getChildForBranch(heaviestBranch),
			      heaviestSplit.instancesDownBranch(heaviestBranch,
								posInstances),
			      heaviestSplit.instancesDownBranch(heaviestBranch,
								negInstances));
  }

  /**
   * Continues single (two-class optimized) search by investigating only the path
   * with the best Z-pure value at each branch.
   *
   * @param currentNode the root of the subtree to be searched
   * @param posInstances the positive-class instances that apply at this node
   * @param negInstances the negative-class instances that apply at this node
   * @exception Exception if search fails
   */
  private void goDownZpurePathSingle(PredictionNode currentNode,
				     Instances posInstances, Instances negInstances)
    throws Exception {

    double lowestZpure = m_search_smallestZ; // do z-pure cutoff
    PredictionNode bestPath = null;
    Instances bestPosSplit = null, bestNegSplit = null;

    // search for branch with lowest Z-pure
    for (Enumeration e = currentNode.children(); e.hasMoreElements(); ) {
      Splitter split = (Splitter) e.nextElement();
      for (int i=0; i<split.getNumOfBranches(); i++) {
	Instances posSplit = split.instancesDownBranch(i, posInstances);
	Instances negSplit = split.instancesDownBranch(i, negInstances);
	double newZpure = calcZpure(posSplit, negSplit);
	if (newZpure < lowestZpure) {
	  lowestZpure = newZpure;
	  bestPath = split.getChildForBranch(i);
	  bestPosSplit = posSplit;
	  bestNegSplit = negSplit;
	}
      }
    }

    if (bestPath != null)
      searchForBestTestSingle(bestPath, bestPosSplit, bestNegSplit);
  }

  /**
   * Continues single (two-class optimized) search by investigating a random path.
   *
   * @param currentNode the root of the subtree to be searched
   * @param posInstances the positive-class instances that apply at this node
   * @param negInstances the negative-class instances that apply at this node
   * @exception Exception if search fails
   */
  private void goDownRandomPathSingle(PredictionNode currentNode,
				      Instances posInstances, Instances negInstances)
    throws Exception {

    FastVector children = currentNode.getChildren();
    Splitter split = (Splitter) children.elementAt(getRandom(children.size()));
    int branch = getRandom(split.getNumOfBranches());
    searchForBestTestSingle(split.getChildForBranch(branch),
			    split.instancesDownBranch(branch, posInstances),
			    split.instancesDownBranch(branch, negInstances));
  }

  /**
   * Investigates the option of introducing a nominal split under currentNode. If it
   * finds a split that has a Z-value lower than has already been found it will
   * update the search information to record this as the best option so far. 
   *
   * @param attIndex index of a nominal attribute to create a split from
   * @param currentNode the parent under which a split is to be considered
   * @param posInstances the positive-class instances that apply at this node
   * @param negInstances the negative-class instances that apply at this node
   */
  private void evaluateNominalSplitSingle(int attIndex, PredictionNode currentNode,
					  Instances posInstances, Instances negInstances)
  {
    
    double[] indexAndZ = findLowestZNominalSplit(posInstances, negInstances, attIndex);

    if (indexAndZ[1] < m_search_smallestZ) {
      m_search_smallestZ = indexAndZ[1];
      m_search_bestInsertionNode = currentNode;
      m_search_bestSplitter = new TwoWayNominalSplit(attIndex, (int) indexAndZ[0]);
      m_search_bestPathPosInstances = posInstances;
      m_search_bestPathNegInstances = negInstances;
    }
  }

  /**
   * Investigates the option of introducing a two-way numeric split under currentNode.
   * If it finds a split that has a Z-value lower than has already been found it will
   * update the search information to record this as the best option so far. 
   *
   * @param attIndex index of a numeric attribute to create a split from
   * @param currentNode the parent under which a split is to be considered
   * @param posInstances the positive-class instances that apply at this node
   * @param negInstances the negative-class instances that apply at this node
   * @param allInstances all of the instances the apply at this node (pos+neg combined)
   * @throws Exception in case of an error
   */
  private void evaluateNumericSplitSingle(int attIndex, PredictionNode currentNode,
					  Instances posInstances, Instances negInstances,
					  Instances allInstances)
    throws Exception {
    
    double[] splitAndZ = findLowestZNumericSplit(allInstances, attIndex);

    if (splitAndZ[1] < m_search_smallestZ) {
      m_search_smallestZ = splitAndZ[1];
      m_search_bestInsertionNode = currentNode;
      m_search_bestSplitter = new TwoWayNumericSplit(attIndex, splitAndZ[0]);
      m_search_bestPathPosInstances = posInstances;
      m_search_bestPathNegInstances = negInstances;
    }
  }

  /**
   * Calculates the prediction value used for a particular set of instances.
   *
   * @param posInstances the positive-class instances
   * @param negInstances the negative-class instances
   * @return the prediction value
   */
  private double calcPredictionValue(Instances posInstances, Instances negInstances) {
    
    return 0.5 * Math.log( (posInstances.sumOfWeights() + 1.0)
			  / (negInstances.sumOfWeights() + 1.0) );
  }

  /**
   * Calculates the Z-pure value for a particular set of instances.
   *
   * @param posInstances the positive-class instances
   * @param negInstances the negative-class instances
   * @return the Z-pure value
   */
  private double calcZpure(Instances posInstances, Instances negInstances) {
    
    double posWeight = posInstances.sumOfWeights();
    double negWeight = negInstances.sumOfWeights();
    return (2.0 * (Math.sqrt(posWeight+1.0) + Math.sqrt(negWeight+1.0))) + 
      (m_trainTotalWeight - (posWeight + negWeight));
  }

  /**
   * Updates the weights of instances that are influenced by a new prediction value.
   *
   * @param posInstances positive-class instances to which the prediction value applies
   * @param negInstances negative-class instances to which the prediction value applies
   * @param predictionValue the new prediction value
   */
  private void updateWeights(Instances posInstances, Instances negInstances,
			     double predictionValue) {
    
    // do positives
    double weightMultiplier = Math.pow(Math.E, -predictionValue);
    for (Enumeration e = posInstances.enumerateInstances(); e.hasMoreElements(); ) {
      Instance inst = (Instance) e.nextElement();
      inst.setWeight(inst.weight() * weightMultiplier);
    }
    // do negatives
    weightMultiplier = Math.pow(Math.E, predictionValue);
    for (Enumeration e = negInstances.enumerateInstances(); e.hasMoreElements(); ) {
      Instance inst = (Instance) e.nextElement();
      inst.setWeight(inst.weight() * weightMultiplier);
    }
  }

  /**
   * Finds the nominal attribute value to split on that results in the lowest Z-value.
   *
   * @param posInstances the positive-class instances to split
   * @param negInstances the negative-class instances to split
   * @param attIndex the index of the nominal attribute to find a split for
   * @return a double array, index[0] contains the value to split on, index[1] contains
   * the Z-value of the split
   */
  private double[] findLowestZNominalSplit(Instances posInstances, Instances negInstances,
					   int attIndex)
  {
    
    double lowestZ = Double.MAX_VALUE;
    int bestIndex = 0;

    // set up arrays
    double[] posWeights = attributeValueWeights(posInstances, attIndex);
    double[] negWeights = attributeValueWeights(negInstances, attIndex);
    double posWeight = Utils.sum(posWeights);
    double negWeight = Utils.sum(negWeights);

    int maxIndex = posWeights.length;
    if (maxIndex == 2) maxIndex = 1; // avoid repeating due to 2-way symmetry

    for (int i = 0; i < maxIndex; i++) {
      // calculate Z
      double w1 = posWeights[i] + 1.0;
      double w2 = negWeights[i] + 1.0;
      double w3 = posWeight - w1 + 2.0;
      double w4 = negWeight - w2 + 2.0;
      double wRemainder = m_trainTotalWeight + 4.0 - (w1 + w2 + w3 + w4);
      double newZ = (2.0 * (Math.sqrt(w1 * w2) + Math.sqrt(w3 * w4))) + wRemainder;

      // record best option
      if (newZ < lowestZ) { 
	lowestZ = newZ;
	bestIndex = i;
      }
    }

    // return result
    double[] indexAndZ = new double[2];
    indexAndZ[0] = (double) bestIndex;
    indexAndZ[1] = lowestZ;
    return indexAndZ; 
  }

  /**
   * Simultanously sum the weights of all attribute values for all instances.
   *
   * @param instances the instances to get the weights from 
   * @param attIndex index of the attribute to be evaluated
   * @return a double array containing the weight of each attribute value
   */    
  private double[] attributeValueWeights(Instances instances, int attIndex)
  {
    
    double[] weights = new double[instances.attribute(attIndex).numValues()];
    for(int i = 0; i < weights.length; i++) weights[i] = 0.0;

    for (Enumeration e = instances.enumerateInstances(); e.hasMoreElements(); ) {
      Instance inst = (Instance) e.nextElement();
      if (!inst.isMissing(attIndex)) weights[(int)inst.value(attIndex)] += inst.weight();
    }
    return weights;
  }

  /**
   * Finds the numeric split-point that results in the lowest Z-value.
   *
   * @param instances the instances to find a split for
   * @param attIndex the index of the numeric attribute to find a split for
   * @return a double array, index[0] contains the split-point, index[1] contains the
   * Z-value of the split
   * @throws Exception in case of an error
   */
  private double[] findLowestZNumericSplit(Instances instances, int attIndex)
    throws Exception {
    
    double splitPoint = 0.0;
    double bestVal = Double.MAX_VALUE, currVal, currCutPoint;
    int numMissing = 0;
    double[][] distribution = new double[3][instances.numClasses()];   

    // compute counts for all the values
    for (int i = 0; i < instances.numInstances(); i++) {
      Instance inst = instances.instance(i);
      if (!inst.isMissing(attIndex)) {
	distribution[1][(int)inst.classValue()] += inst.weight();
      } else {
	distribution[2][(int)inst.classValue()] += inst.weight();
	numMissing++;
      }
    }

    // sort instances
    instances.sort(attIndex);
    
    // make split counts for each possible split and evaluate
    for (int i = 0; i < instances.numInstances() - (numMissing + 1); i++) {
      Instance inst = instances.instance(i);
      Instance instPlusOne = instances.instance(i + 1);
      distribution[0][(int)inst.classValue()] += inst.weight();
      distribution[1][(int)inst.classValue()] -= inst.weight();
      if (Utils.sm(inst.value(attIndex), instPlusOne.value(attIndex))) {
	currCutPoint = (inst.value(attIndex) + instPlusOne.value(attIndex)) / 2.0;
	currVal = conditionedZOnRows(distribution);
	if (currVal < bestVal) {
	  splitPoint = currCutPoint;
	  bestVal = currVal;
	}
      }
    }
	
    double[] splitAndZ = new double[2];
    splitAndZ[0] = splitPoint;
    splitAndZ[1] = bestVal;
    return splitAndZ;
  }

  /**
   * Calculates the Z-value from the rows of a weight distribution array.
   *
   * @param distribution the weight distribution
   * @return the Z-value
   */
  private double conditionedZOnRows(double [][] distribution) {
    
    double w1 = distribution[0][0] + 1.0;
    double w2 = distribution[0][1] + 1.0;
    double w3 = distribution[1][0] + 1.0; 
    double w4 = distribution[1][1] + 1.0;
    double wRemainder = m_trainTotalWeight + 4.0 - (w1 + w2 + w3 + w4);
    return (2.0 * (Math.sqrt(w1 * w2) + Math.sqrt(w3 * w4))) + wRemainder;
  }

  /**
   * Returns the class probability distribution for an instance.
   *
   * @param instance the instance to be classified
   * @return the distribution the tree generates for the instance
   */
  public double[] distributionForInstance(Instance instance) {
    
    double predVal = predictionValueForInstance(instance, m_root, 0.0);
    
    double[] distribution = new double[2];
    distribution[0] = 1.0 / (1.0 + Math.pow(Math.E, predVal));
    distribution[1] = 1.0 / (1.0 + Math.pow(Math.E, -predVal));

    return distribution;
  }

  /**
   * Returns the class prediction value (vote) for an instance.
   *
   * @param inst the instance
   * @param currentNode the root of the tree to get the values from
   * @param currentValue the current value before adding the value contained in the
   * subtree
   * @return the class prediction value (vote)
   */
  protected double predictionValueForInstance(Instance inst, PredictionNode currentNode,
					    double currentValue) {
    
    currentValue += currentNode.getValue();
    for (Enumeration e = currentNode.children(); e.hasMoreElements(); ) {
      Splitter split = (Splitter) e.nextElement();
      int branch = split.branchInstanceGoesDown(inst);
      if (branch >= 0)
	currentValue = predictionValueForInstance(inst, split.getChildForBranch(branch),
						  currentValue);
    }
    return currentValue;
  }

  /**
   * Returns a description of the classifier.
   *
   * @return a string containing a description of the classifier
   */
  public String toString() {
    
    if (m_root == null)
      return ("ADTree not built yet");
    else {
      return ("Alternating decision tree:\n\n" + toString(m_root, 1) +
	      "\nLegend: " + legend() +
	      "\nTree size (total number of nodes): " + numOfAllNodes(m_root) + 
	      "\nLeaves (number of predictor nodes): " + numOfPredictionNodes(m_root)
	      );
    }
  }

  /**
   * Traverses the tree, forming a string that describes it.
   *
   * @param currentNode the current node under investigation
   * @param level the current level in the tree
   * @return the string describing the subtree
   */      
  protected String toString(PredictionNode currentNode, int level) {
    
    StringBuffer text = new StringBuffer();
    
    text.append(": " + Utils.doubleToString(currentNode.getValue(),3));
    
    for (Enumeration e = currentNode.children(); e.hasMoreElements(); ) {
      Splitter split = (Splitter) e.nextElement();
	    
      for (int j=0; j<split.getNumOfBranches(); j++) {
	PredictionNode child = split.getChildForBranch(j);
	if (child != null) {
	  text.append("\n");
	  for (int k = 0; k < level; k++) {
	    text.append("|  ");
	  }
	  text.append("(" + split.orderAdded + ")");
	  text.append(split.attributeString(m_trainInstances) + " "
		      + split.comparisonString(j, m_trainInstances));
	  text.append(toString(child, level + 1));
	}
      }
    }
    return text.toString();
  }

  /**
   *  Returns the type of graph this classifier
   *  represents.
   *  @return Drawable.TREE
   */   
  public int graphType() {
      return Drawable.TREE;
  }

  /**
   * Returns graph describing the tree.
   *
   * @return the graph of the tree in dotty format
   * @exception Exception if something goes wrong
   */
  public String graph() throws Exception {
    
    StringBuffer text = new StringBuffer();
    text.append("digraph ADTree {\n");
    graphTraverse(m_root, text, 0, 0, m_trainInstances);
    return text.toString() +"}\n";
  }

  /**
   * Traverses the tree, graphing each node.
   *
   * @param currentNode the currentNode under investigation
   * @param text the string built so far
   * @param splitOrder the order the parent splitter was added to the tree
   * @param predOrder the order this predictor was added to the split
   * @param instances the data to work on
   * @exception Exception if something goes wrong
   */       
  protected void graphTraverse(PredictionNode currentNode, StringBuffer text,
			       int splitOrder, int predOrder, Instances instances)
    throws Exception {
    
    text.append("S" + splitOrder + "P" + predOrder + " [label=\"");
    text.append(Utils.doubleToString(currentNode.getValue(),3));
    if (splitOrder == 0) // show legend in root
      text.append(" (" + legend() + ")");
    text.append("\" shape=box style=filled");
    if (instances.numInstances() > 0) text.append(" data=\n" + instances + "\n,\n");
    text.append("]\n");
    for (Enumeration e = currentNode.children(); e.hasMoreElements(); ) {
      Splitter split = (Splitter) e.nextElement();
      text.append("S" + splitOrder + "P" + predOrder + "->" + "S" + split.orderAdded +
		  " [style=dotted]\n");
      text.append("S" + split.orderAdded + " [label=\"" + split.orderAdded + ": " +
		  split.attributeString(m_trainInstances) + "\"]\n");

      for (int i=0; i<split.getNumOfBranches(); i++) {
	PredictionNode child = split.getChildForBranch(i);
	if (child != null) {
	  text.append("S" + split.orderAdded + "->" + "S" + split.orderAdded + "P" + i +
		      " [label=\"" + split.comparisonString(i, m_trainInstances) + "\"]\n");
	  graphTraverse(child, text, split.orderAdded, i,
			split.instancesDownBranch(i, instances));
	}
      }
    }  
  }

  /**
   * Returns the legend of the tree, describing how results are to be interpreted.
   *
   * @return a string containing the legend of the classifier
   */
  public String legend() {
    
    Attribute classAttribute = null;
    if (m_trainInstances == null) return "";
    try {classAttribute = m_trainInstances.classAttribute();} catch (Exception x){};
    return ("-ve = " + classAttribute.value(0) +
	    ", +ve = " + classAttribute.value(1));
  }

  /**
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numOfBoostingIterationsTipText() {

    return "Sets the number of boosting iterations to perform. You will need to manually "
      + "tune this parameter to suit the dataset and the desired complexity/accuracy "
      + "tradeoff. More boosting iterations will result in larger (potentially more "
      + " accurate) trees, but will make learning slower. Each iteration will add 3 nodes "
      + "(1 split + 2 prediction) to the tree unless merging occurs.";
  }

  /**
   * Gets the number of boosting iterations.
   *
   * @return the number of boosting iterations
   */
  public int getNumOfBoostingIterations() {
    
    return m_boostingIterations;
  }

  /**
   * Sets the number of boosting iterations.
   *
   * @param b the number of boosting iterations to use
   */
  public void setNumOfBoostingIterations(int b) {
    
    m_boostingIterations = b; 
  }

  /**
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String searchPathTipText() {

    return "Sets the type of search to perform when building the tree. The default option"
      + " (Expand all paths) will do an exhaustive search. The other search methods are"
      + " heuristic, so they are not guaranteed to find an optimal solution but they are"
      + " much faster. Expand the heaviest path: searches the path with the most heavily"
      + " weighted instances. Expand the best z-pure path: searches the path determined"
      + " by the best z-pure estimate. Expand a random path: the fastest method, simply"
      + " searches down a single random path on each iteration.";
  }

  /**
   * Gets the method of searching the tree for a new insertion. Will be one of
   * SEARCHPATH_ALL, SEARCHPATH_HEAVIEST, SEARCHPATH_ZPURE, SEARCHPATH_RANDOM.
   *
   * @return the tree searching mode
   */
  public SelectedTag getSearchPath() {

    return new SelectedTag(m_searchPath, TAGS_SEARCHPATH);
  }
  
  /**
   * Sets the method of searching the tree for a new insertion. Will be one of
   * SEARCHPATH_ALL, SEARCHPATH_HEAVIEST, SEARCHPATH_ZPURE, SEARCHPATH_RANDOM.
   *
   * @param newMethod the new tree searching mode
   */
  public void setSearchPath(SelectedTag newMethod) {
    
    if (newMethod.getTags() == TAGS_SEARCHPATH) {
      m_searchPath = newMethod.getSelectedTag().getID();
    }
  }

  /**
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String randomSeedTipText() {

    return "Sets the random seed to use for a random search.";
  }

  /**
   * Gets random seed for a random walk.
   *
   * @return the random seed
   */
  public int getRandomSeed() {
    
    return m_randomSeed;
  }

  /**
   * Sets random seed for a random walk.
   *
   * @param seed the random seed
   */
  public void setRandomSeed(int seed) {
    
    // the actual random object is created when the tree is initialized
    m_randomSeed = seed; 
  }  

  /**
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String saveInstanceDataTipText() {

    return "Sets whether the tree is to save instance data - the model will take up more"
      + " memory if it does. If enabled you will be able to visualize the instances at"
      + " the prediction nodes when visualizing the tree.";
  }

  /**
   * Gets whether the tree is to save instance data.
   *
   * @return the random seed
   */
  public boolean getSaveInstanceData() {
    
    return m_saveInstanceData;
  }

  /**
   * Sets whether the tree is to save instance data.
   * 
   * @param v true then the tree saves instance data
   */
  public void setSaveInstanceData(boolean v) {
    
    m_saveInstanceData = v;
  }

  /**
   * Returns an enumeration describing the available options..
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    
    Vector newVector = new Vector(3);
    newVector.addElement(new Option(
				    "\tNumber of boosting iterations.\n"
				    +"\t(Default = 10)",
				    "B", 1,"-B <number of boosting iterations>"));
    newVector.addElement(new Option(
				    "\tExpand nodes: -3(all), -2(weight), -1(z_pure), "
				    +">=0 seed for random walk\n"
				    +"\t(Default = -3)",
				    "E", 1,"-E <-3|-2|-1|>=0>"));
    newVector.addElement(new Option(
				    "\tSave the instance data with the model",
				    "D", 0,"-D"));
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -B num <br>
   * Set the number of boosting iterations
   * (default 10) <p>
   *
   * -E num <br>
   * Set the nodes to expand: -3(all), -2(weight), -1(z_pure), >=0 seed for random walk
   * (default -3) <p>
   *
   * -D <br>
   * Save the instance data with the model <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    String bString = Utils.getOption('B', options);
    if (bString.length() != 0) setNumOfBoostingIterations(Integer.parseInt(bString));

    String eString = Utils.getOption('E', options);
    if (eString.length() != 0) {
      int value = Integer.parseInt(eString);
      if (value >= 0) {
	setSearchPath(new SelectedTag(SEARCHPATH_RANDOM, TAGS_SEARCHPATH));
	setRandomSeed(value);
      } else setSearchPath(new SelectedTag(value + 3, TAGS_SEARCHPATH));
    }

    setSaveInstanceData(Utils.getFlag('D', options));

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of ADTree.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions() {
    
    String[] options = new String[6];
    int current = 0;
    options[current++] = "-B"; options[current++] = "" + getNumOfBoostingIterations();
    options[current++] = "-E"; options[current++] = "" +
				 (m_searchPath == SEARCHPATH_RANDOM ?
				  m_randomSeed : m_searchPath - 3);
    if (getSaveInstanceData()) options[current++] = "-D";
    while (current < options.length) options[current++] = "";
    return options;
  }

  /**
   * Calls measure function for tree size - the total number of nodes.
   *
   * @return the tree size
   */
  public double measureTreeSize() {
    
    return numOfAllNodes(m_root);
  }

  /**
   * Calls measure function for leaf size - the number of prediction nodes.
   *
   * @return the leaf size
   */
  public double measureNumLeaves() {
    
    return numOfPredictionNodes(m_root);
  }

  /**
   * Calls measure function for prediction leaf size - the number of 
   * prediction nodes without children.
   *
   * @return the leaf size
   */
  public double measureNumPredictionLeaves() {
    
    return numOfPredictionLeafNodes(m_root);
  }

  /**
   * Returns the number of nodes expanded.
   *
   * @return the number of nodes expanded during search
   */
  public double measureNodesExpanded() {
    
    return m_nodesExpanded;
  }

  /**
   * Returns the number of examples "counted".
   *
   * @return the number of nodes processed during search
   */

  public double measureExamplesProcessed() {
    
    return m_examplesCounted;
  }

  /**
   * Returns an enumeration of the additional measure names.
   *
   * @return an enumeration of the measure names
   */
  public Enumeration enumerateMeasures() {
    
    Vector newVector = new Vector(4);
    newVector.addElement("measureTreeSize");
    newVector.addElement("measureNumLeaves");
    newVector.addElement("measureNumPredictionLeaves");
    newVector.addElement("measureNodesExpanded");
    newVector.addElement("measureExamplesProcessed");
    return newVector.elements();
  }
 
  /**
   * Returns the value of the named measure.
   *
   * @param additionalMeasureName the name of the measure to query for its value
   * @return the value of the named measure
   * @exception IllegalArgumentException if the named measure is not supported
   */
  public double getMeasure(String additionalMeasureName) {
    
    if (additionalMeasureName.equalsIgnoreCase("measureTreeSize")) {
      return measureTreeSize();
    }
    else if (additionalMeasureName.equalsIgnoreCase("measureNumLeaves")) {
      return measureNumLeaves();
    }
    else if (additionalMeasureName.equalsIgnoreCase("measureNumPredictionLeaves")) {
      return measureNumPredictionLeaves();
    }
    else if (additionalMeasureName.equalsIgnoreCase("measureNodesExpanded")) {
      return measureNodesExpanded();
    }
    else if (additionalMeasureName.equalsIgnoreCase("measureExamplesProcessed")) {
      return measureExamplesProcessed();
    }
    else {throw new IllegalArgumentException(additionalMeasureName 
			      + " not supported (ADTree)");
    }
  }

  /**
   * Returns the total number of nodes in a tree.
   *
   * @param root the root of the tree being measured
   * @return tree size in number of splitter + prediction nodes
   */       
  protected int numOfAllNodes(PredictionNode root) {
    
    int numSoFar = 0;
    if (root != null) {
      numSoFar++;
      for (Enumeration e = root.children(); e.hasMoreElements(); ) {
	numSoFar++;
	Splitter split = (Splitter) e.nextElement();
	for (int i=0; i<split.getNumOfBranches(); i++)
	    numSoFar += numOfAllNodes(split.getChildForBranch(i));
      }
    }
    return numSoFar;
  }

  /**
   * Returns the number of prediction nodes in a tree.
   *
   * @param root the root of the tree being measured
   * @return tree size in number of prediction nodes
   */       
  protected int numOfPredictionNodes(PredictionNode root) {
    
    int numSoFar = 0;
    if (root != null) {
      numSoFar++;
      for (Enumeration e = root.children(); e.hasMoreElements(); ) {
	Splitter split = (Splitter) e.nextElement();
	for (int i=0; i<split.getNumOfBranches(); i++)
	    numSoFar += numOfPredictionNodes(split.getChildForBranch(i));
      }
    }
    return numSoFar;
  }

  /**
   * Returns the number of leaf nodes in a tree - prediction nodes without
   * children.
   *
   * @param root the root of the tree being measured
   * @return tree leaf size in number of prediction nodes
   */       
  protected int numOfPredictionLeafNodes(PredictionNode root) {
    
    int numSoFar = 0;
    if (root.getChildren().size() > 0) {
      for (Enumeration e = root.children(); e.hasMoreElements(); ) {
	Splitter split = (Splitter) e.nextElement();
	for (int i=0; i<split.getNumOfBranches(); i++)
	    numSoFar += numOfPredictionLeafNodes(split.getChildForBranch(i));
      }
    } else numSoFar = 1;
    return numSoFar;
  }

  /**
   * Gets the next random value.
   *
   * @param max the maximum value (+1) to be returned
   * @return the next random value (between 0 and max-1)
   */
  protected int getRandom(int max) {
    
    return m_random.nextInt(max);
  }

  /**
   * Returns the next number in the order that splitter nodes have been added to
   * the tree, and records that a new splitter has been added.
   *
   * @return the next number in the order
   */
  public int nextSplitAddedOrder() {

    return ++m_lastAddedSplitNum;
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.BINARY_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);
    
    return result;
  }

  /**
   * Builds a classifier for a set of instances.
   *
   * @param instances the instances to train the classifier with
   * @exception Exception if something goes wrong
   */
  public void buildClassifier(Instances instances) throws Exception {

    // can classifier handle the data?
    getCapabilities().testWithFail(instances);

    // remove instances with missing class
    instances = new Instances(instances);
    instances.deleteWithMissingClass();

    // set up the tree
    initClassifier(instances);

    // build the tree
    for (int T = 0; T < m_boostingIterations; T++) boost();

    // clean up if desired
    if (!m_saveInstanceData) done();
  }

  /**
   * Frees memory that is no longer needed for a final model - will no longer be able
   * to increment the classifier after calling this.
   *
   */
  public void done() {

    m_trainInstances = new Instances(m_trainInstances, 0);
    m_random = null; 
    m_numericAttIndices = null;
    m_nominalAttIndices = null;
    m_posTrainInstances = null;
    m_negTrainInstances = null;
  }

  /**
   * Creates a clone that is identical to the current tree, but is independent.
   * Deep copies the essential elements such as the tree nodes, and the instances
   * (because the weights change.) Reference copies several elements such as the
   * potential splitter sets, assuming that such elements should never differ between
   * clones.
   *
   * @return the clone
   */
  public Object clone() {
    
    ADTree clone = new ADTree();

    if (m_root != null) { // check for initialization first
      clone.m_root = (PredictionNode) m_root.clone(); // deep copy the tree

      clone.m_trainInstances = new Instances(m_trainInstances); // copy training instances
      
      // deep copy the random object
      if (m_random != null) { 
	SerializedObject randomSerial = null;
	try {
	  randomSerial = new SerializedObject(m_random);
	} catch (Exception ignored) {} // we know that Random is serializable
	clone.m_random = (Random) randomSerial.getObject();
      }

      clone.m_lastAddedSplitNum = m_lastAddedSplitNum;
      clone.m_numericAttIndices = m_numericAttIndices;
      clone.m_nominalAttIndices = m_nominalAttIndices;
      clone.m_trainTotalWeight = m_trainTotalWeight;

      // reconstruct pos/negTrainInstances references
      if (m_posTrainInstances != null) { 
	clone.m_posTrainInstances =
	  new ReferenceInstances(m_trainInstances, m_posTrainInstances.numInstances());
	clone.m_negTrainInstances =
	  new ReferenceInstances(m_trainInstances, m_negTrainInstances.numInstances());
	for (Enumeration e = clone.m_trainInstances.enumerateInstances();
	     e.hasMoreElements(); ) {
	  Instance inst = (Instance) e.nextElement();
	  try { // ignore classValue() exception
	    if ((int) inst.classValue() == 0)
	      clone.m_negTrainInstances.addReference(inst); // belongs in negative class
	    else
	      clone.m_posTrainInstances.addReference(inst); // belongs in positive class
	  } catch (Exception ignored) {} 
	}
      }
    }
    clone.m_nodesExpanded = m_nodesExpanded;
    clone.m_examplesCounted = m_examplesCounted;
    clone.m_boostingIterations = m_boostingIterations;
    clone.m_searchPath = m_searchPath;
    clone.m_randomSeed = m_randomSeed;

    return clone;
  }

  /**
   * Merges two trees together. Modifies the tree being acted on, leaving tree passed
   * as a parameter untouched (cloned). Does not check to see whether training instances
   * are compatible - strange things could occur if they are not.
   *
   * @param mergeWith the tree to merge with
   * @exception Exception if merge could not be performed
   */
  public void merge(ADTree mergeWith) throws Exception {
    
    if (m_root == null || mergeWith.m_root == null)
      throw new Exception("Trying to merge an uninitialized tree");
    m_root.merge(mergeWith.m_root, this);
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
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {
    runClassifier(new ADTree(), argv);
  }
}
