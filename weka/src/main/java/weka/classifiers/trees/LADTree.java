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
 *    LADTree.java
 *    Copyright (C) 2001 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.trees;

import weka.classifiers.*;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.*;
import weka.classifiers.trees.adtree.ReferenceInstances;
import java.util.*;
import java.io.*;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;

/*
 <!-- globalinfo-start -->
 * Class for generating a multi-class alternating decision tree using the LogitBoost strategy. For more info, see<br/>
 * <br/>
 * Geoffrey Holmes, Bernhard Pfahringer, Richard Kirkby, Eibe Frank, Mark Hall: Multiclass alternating decision trees. In: ECML, 161-172, 2001.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{Holmes2001,
 *    author = {Geoffrey Holmes and Bernhard Pfahringer and Richard Kirkby and Eibe Frank and Mark Hall},
 *    booktitle = {ECML},
 *    pages = {161-172},
 *    publisher = {Springer},
 *    title = {Multiclass alternating decision trees},
 *    year = {2001}
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
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 <!-- options-end -->
 *
 * @author Richard Kirkby
 * @version $Revision$
*/

public class LADTree
  extends AbstractClassifier implements Drawable,
                                AdditionalMeasureProducer,
                                TechnicalInformationHandler {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -4940716114518300302L;

  // Constant from LogitBoost
  protected double Z_MAX = 4;

  // Number of classes
  protected int m_numOfClasses;

  // Instances as reference instances
  protected ReferenceInstances m_trainInstances;

  // Root of the tree
  protected PredictionNode m_root = null; 

  // To keep track of the order in which splits are added
  protected int m_lastAddedSplitNum = 0;

  // Indices for numeric attributes
  protected int[] m_numericAttIndices;

  // Variables to keep track of best options
  protected double m_search_smallestLeastSquares;
  protected PredictionNode m_search_bestInsertionNode;
  protected Splitter m_search_bestSplitter;
  protected Instances m_search_bestPathInstances;

  // A collection of splitter nodes
  protected FastVector m_staticPotentialSplitters2way;

  // statistics
  protected int m_nodesExpanded = 0;
  protected int m_examplesCounted = 0;

  // options
  protected int m_boostingIterations = 10;

  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return  "Class for generating a multi-class alternating decision tree using " +
      "the LogitBoost strategy. For more info, see\n\n"
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
        
    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "Geoffrey Holmes and Bernhard Pfahringer and Richard Kirkby and Eibe Frank and Mark Hall");
    result.setValue(Field.TITLE, "Multiclass alternating decision trees");
    result.setValue(Field.BOOKTITLE, "ECML");
    result.setValue(Field.YEAR, "2001");
    result.setValue(Field.PAGES, "161-172");
    result.setValue(Field.PUBLISHER, "Springer");
    
    return result;
  }

  /** helper classes ********************************************************************/

  protected class LADInstance extends DenseInstance {
    public double[] fVector;
    public double[] wVector;
    public double[] pVector;
    public double[] zVector;
    public LADInstance(Instance instance) {
    
      super(instance);
      
      setDataset(instance.dataset()); // preserve dataset

      // set up vectors
      fVector = new double[m_numOfClasses];
      wVector = new double[m_numOfClasses];
      pVector = new double[m_numOfClasses];
      zVector = new double[m_numOfClasses];

      // set initial probabilities
      double initProb = 1.0 / ((double) m_numOfClasses);
      for (int i=0; i<m_numOfClasses; i++) {
	pVector[i] = initProb;
      }
      updateZVector();
      updateWVector();
    }
    public void updateWeights(double[] fVectorIncrement) {
      for (int i=0; i<fVector.length; i++) {
	fVector[i] += fVectorIncrement[i];
      }
      updateVectors(fVector);
    }
    public void updateVectors(double[] newFVector) {
      updatePVector(newFVector);
      updateZVector();
      updateWVector();
    }
    public void updatePVector(double[] newFVector) {
      double max = newFVector[Utils.maxIndex(newFVector)];
      for (int i=0; i<pVector.length; i++) {
	pVector[i] = Math.exp(newFVector[i] - max);
      }
      Utils.normalize(pVector);
    }
    public void updateWVector() {
      for (int i=0; i<wVector.length; i++) {
	wVector[i] = (yVector(i) - pVector[i]) / zVector[i];
      }
    }
    public void updateZVector() {

      for (int i=0; i<zVector.length; i++) {
	if (yVector(i) == 1) {
	  zVector[i] = 1.0 / pVector[i];
	  if (zVector[i] > Z_MAX) { // threshold
	    zVector[i] = Z_MAX;
	  }
	} else {
	  zVector[i] = -1.0 / (1.0 - pVector[i]);
	  if (zVector[i] < -Z_MAX) { // threshold
	    zVector[i] = -Z_MAX;
	  }
	}
      }
    }
    public double yVector(int index) {
      return (index == (int) classValue() ? 1.0 : 0.0); 
    }
    public Object copy() {
      LADInstance copy = new LADInstance((Instance) super.copy());
      System.arraycopy(fVector, 0, copy.fVector, 0, fVector.length);
      System.arraycopy(wVector, 0, copy.wVector, 0, wVector.length);
      System.arraycopy(pVector, 0, copy.pVector, 0, pVector.length);
      System.arraycopy(zVector, 0, copy.zVector, 0, zVector.length);
      return copy;
    }
    public String toString() {

      StringBuffer text = new StringBuffer();
      text.append(" * F(");
      for (int i=0; i<fVector.length; i++) {
	text.append(Utils.doubleToString(fVector[i], 3));
	if (i<fVector.length-1) text.append(",");
      }
      text.append(") P(");
      for (int i=0; i<pVector.length; i++) {
	text.append(Utils.doubleToString(pVector[i], 3));
	if (i<pVector.length-1) text.append(",");
      }
      text.append(") W(");
      for (int i=0; i<wVector.length; i++) {
	text.append(Utils.doubleToString(wVector[i], 3));
	if (i<wVector.length-1) text.append(",");
      }
      text.append(")");
      return super.toString() + text.toString();

    }
  }

  protected class PredictionNode implements Serializable, Cloneable{
    private double[] values;
    private FastVector children; // any number of splitter nodes
    
    public PredictionNode(double[] newValues) {
      values = new double[m_numOfClasses];
      setValues(newValues);
      children = new FastVector();
    }
    public void setValues(double[] newValues) {
      System.arraycopy(newValues, 0, values, 0, m_numOfClasses);
    }
    public double[] getValues() {
      return values;
    }
    public FastVector getChildren() { return children; }
    public Enumeration children() { return children.elements(); }
    public void addChild(Splitter newChild) { // merges, adds a clone (deep copy)
      Splitter oldEqual = null;
      for (Enumeration e = children(); e.hasMoreElements(); ) {
	Splitter split = (Splitter) e.nextElement();
	if (newChild.equalTo(split)) { oldEqual = split; break; }
      }
      if (oldEqual == null) {
	Splitter addChild = (Splitter) newChild.clone();
	addChild.orderAdded = ++m_lastAddedSplitNum;
	children.addElement(addChild);
      }
      else { // do a merge
	for (int i=0; i<newChild.getNumOfBranches(); i++) {
	  PredictionNode oldPred = oldEqual.getChildForBranch(i);
	  PredictionNode newPred = newChild.getChildForBranch(i);
	  if (oldPred != null && newPred != null)
	    oldPred.merge(newPred);
	}
      }
    }
    public Object clone() { // does a deep copy (recurses through tree)
      PredictionNode clone = new PredictionNode(values);
      // should actually clone once merges are enabled!
      for (Enumeration e = children.elements(); e.hasMoreElements(); )
	clone.children.addElement((Splitter)((Splitter) e.nextElement()).clone());
      return clone;
    }
    public void merge(PredictionNode merger) {
      // need to merge linear models here somehow
      for (int i=0; i<m_numOfClasses; i++) values[i] += merger.values[i];
      for (Enumeration e = merger.children(); e.hasMoreElements(); ) {
	addChild((Splitter)e.nextElement());
      }
    }
  }

  /** splitter classes ******************************************************************/

  protected abstract class Splitter implements Serializable, Cloneable {
      protected int attIndex;
    public int orderAdded;
    public abstract int getNumOfBranches();
    public abstract int branchInstanceGoesDown(Instance i);
    public abstract Instances instancesDownBranch(int branch, Instances sourceInstances);
    public abstract String attributeString();
    public abstract String comparisonString(int branchNum);
    public abstract boolean equalTo(Splitter compare);
    public abstract void setChildForBranch(int branchNum, PredictionNode childPredictor);
    public abstract PredictionNode getChildForBranch(int branchNum);
    public abstract Object clone();
  }

  protected class TwoWayNominalSplit extends Splitter {
      //private int attIndex;
    private int trueSplitValue;
    private PredictionNode[] children;
    public TwoWayNominalSplit(int _attIndex, int _trueSplitValue) {
      attIndex = _attIndex; trueSplitValue = _trueSplitValue;
      children = new PredictionNode[2];
    }
    public int getNumOfBranches() { return 2; }
    public int branchInstanceGoesDown(Instance inst) {
      if (inst.isMissing(attIndex)) return -1;
      else if (inst.value(attIndex) == trueSplitValue) return 0;
      else return 1;
    }
    public Instances instancesDownBranch(int branch, Instances instances) {
      ReferenceInstances filteredInstances = new ReferenceInstances(instances, 1);
      if (branch == -1) {
	for (Enumeration e = instances.enumerateInstances(); e.hasMoreElements(); ) {
	  Instance inst = (Instance) e.nextElement();
	  if (inst.isMissing(attIndex)) filteredInstances.addReference(inst);
	}
      } else if (branch == 0) {
	for (Enumeration e = instances.enumerateInstances(); e.hasMoreElements(); ) {
	  Instance inst = (Instance) e.nextElement();
	  if (!inst.isMissing(attIndex) && inst.value(attIndex) == trueSplitValue)
	    filteredInstances.addReference(inst);
	}
      } else {
	for (Enumeration e = instances.enumerateInstances(); e.hasMoreElements(); ) {
	  Instance inst = (Instance) e.nextElement();
	  if (!inst.isMissing(attIndex) && inst.value(attIndex) != trueSplitValue)
	    filteredInstances.addReference(inst);
	}
      }
      return filteredInstances;
    }
    public String attributeString() {
      return m_trainInstances.attribute(attIndex).name();
    }
    public String comparisonString(int branchNum) {
      Attribute att = m_trainInstances.attribute(attIndex);
      if (att.numValues() != 2) 
	return ((branchNum == 0 ? "= " : "!= ") + att.value(trueSplitValue));
      else return ("= " + (branchNum == 0 ?
			   att.value(trueSplitValue) :
			   att.value(trueSplitValue == 0 ? 1 : 0)));
    }
    public boolean equalTo(Splitter compare) {
      if (compare instanceof TwoWayNominalSplit) { // test object type
	TwoWayNominalSplit compareSame = (TwoWayNominalSplit) compare;
	return (attIndex == compareSame.attIndex &&
		trueSplitValue == compareSame.trueSplitValue);
      } else return false;
    }
    public void setChildForBranch(int branchNum, PredictionNode childPredictor) {
      children[branchNum] = childPredictor;
    }
    public PredictionNode getChildForBranch(int branchNum) {
      return children[branchNum];
    }
    public Object clone() { // deep copy
      TwoWayNominalSplit clone = new TwoWayNominalSplit(attIndex, trueSplitValue);
      if (children[0] != null)
	clone.setChildForBranch(0, (PredictionNode) children[0].clone());
      if (children[1] != null)
	clone.setChildForBranch(1, (PredictionNode) children[1].clone());
      return clone;
    }
  }

  protected class TwoWayNumericSplit extends Splitter implements Cloneable {
      //private int attIndex;
    private double splitPoint;
    private PredictionNode[] children;
    public TwoWayNumericSplit(int _attIndex, double _splitPoint) {
      attIndex = _attIndex;
      splitPoint = _splitPoint;
      children = new PredictionNode[2];
    }
    public TwoWayNumericSplit(int _attIndex, Instances instances) throws Exception {
      attIndex = _attIndex;
      splitPoint = findSplit(instances, attIndex);
      children = new PredictionNode[2];
    }
    public int getNumOfBranches() { return 2; }
    public int branchInstanceGoesDown(Instance inst) {
      if (inst.isMissing(attIndex)) return -1;
      else if (inst.value(attIndex) < splitPoint) return 0;
      else return 1;
    }
    public Instances instancesDownBranch(int branch, Instances instances) {
      ReferenceInstances filteredInstances = new ReferenceInstances(instances, 1);
      if (branch == -1) {
	for (Enumeration e = instances.enumerateInstances(); e.hasMoreElements(); ) {
	  Instance inst = (Instance) e.nextElement();
	  if (inst.isMissing(attIndex)) filteredInstances.addReference(inst);
	}
      } else if (branch == 0) {
	for (Enumeration e = instances.enumerateInstances(); e.hasMoreElements(); ) {
	  Instance inst = (Instance) e.nextElement();
	  if (!inst.isMissing(attIndex) && inst.value(attIndex) < splitPoint)
	    filteredInstances.addReference(inst);
	}
      } else {
	for (Enumeration e = instances.enumerateInstances(); e.hasMoreElements(); ) {
	  Instance inst = (Instance) e.nextElement();
	  if (!inst.isMissing(attIndex) && inst.value(attIndex) >= splitPoint)
	    filteredInstances.addReference(inst);
	}
      }
      return filteredInstances;
    }
    public String attributeString() {
      return m_trainInstances.attribute(attIndex).name();
    }
    public String comparisonString(int branchNum) {
      return ((branchNum == 0 ? "< " : ">= ") + Utils.doubleToString(splitPoint, 3));
    }
    public boolean equalTo(Splitter compare) {
      if (compare instanceof TwoWayNumericSplit) { // test object type
	TwoWayNumericSplit compareSame = (TwoWayNumericSplit) compare;
	return (attIndex == compareSame.attIndex &&
		splitPoint == compareSame.splitPoint);
      } else return false;
    }
    public void setChildForBranch(int branchNum, PredictionNode childPredictor) {
      children[branchNum] = childPredictor;
    }
    public PredictionNode getChildForBranch(int branchNum) {
      return children[branchNum];
    }
    public Object clone() { // deep copy
      TwoWayNumericSplit clone = new TwoWayNumericSplit(attIndex, splitPoint);
      if (children[0] != null)
	clone.setChildForBranch(0, (PredictionNode) children[0].clone());
      if (children[1] != null)
	clone.setChildForBranch(1, (PredictionNode) children[1].clone());
      return clone;
    }
    private double findSplit(Instances instances, int index) throws Exception {
      double splitPoint = 0;
      double bestVal = Double.MAX_VALUE, currVal, currCutPoint;
      int numMissing = 0;
      double[][] distribution = new double[3][instances.numClasses()];   

      // Compute counts for all the values
      for (int i = 0; i < instances.numInstances(); i++) {
	Instance inst = instances.instance(i);
	if (!inst.isMissing(index)) {
	  distribution[1][(int)inst.classValue()] ++;
	} else {
	  distribution[2][(int)inst.classValue()] ++;
	  numMissing++;
	}
      }
      
      // Sort instances
      instances.sort(index);
      
      // Make split counts for each possible split and evaluate
      for (int i = 0; i < instances.numInstances() - (numMissing + 1); i++) {
	Instance inst = instances.instance(i);
	Instance instPlusOne = instances.instance(i + 1);
	distribution[0][(int)inst.classValue()] += inst.weight();
	distribution[1][(int)inst.classValue()] -= inst.weight();
	if (Utils.sm(inst.value(index), instPlusOne.value(index))) {
	  currCutPoint = (inst.value(index) + instPlusOne.value(index)) / 2.0;
	  currVal = ContingencyTables.entropyConditionedOnRows(distribution);
	  if (Utils.sm(currVal, bestVal)) {
	    splitPoint = currCutPoint;
	    bestVal = currVal;
	  }
	}
      }

      return splitPoint;
    }
  }

  /**
   * Sets up the tree ready to be trained.
   *
   * @param instances the instances to train the tree with
   * @exception Exception if training data is unsuitable
   */
  public void initClassifier(Instances instances) throws Exception {

    // clear stats
    m_nodesExpanded = 0;
    m_examplesCounted = 0;
    m_lastAddedSplitNum = 0;

    m_numOfClasses = instances.numClasses();

    // make sure training data is suitable
    if (instances.checkForStringAttributes()) {
      throw new Exception("Can't handle string attributes!");
    }
    if (!instances.classAttribute().isNominal()) {
      throw new Exception("Class must be nominal!");
    }

    // create training set (use LADInstance class)
    m_trainInstances =
      new ReferenceInstances(instances, instances.numInstances());
    for (Enumeration e = instances.enumerateInstances(); e.hasMoreElements(); ) {
      Instance inst = (Instance) e.nextElement();
      if (!inst.classIsMissing()) {
	LADInstance adtInst = new LADInstance(inst);
	m_trainInstances.addReference(adtInst);
	adtInst.setDataset(m_trainInstances);
      }
    }

    // create the root prediction node
    m_root = new PredictionNode(new double[m_numOfClasses]);
    
    // pre-calculate what we can
    generateStaticPotentialSplittersAndNumericIndices();
  }

    public void next(int iteration) throws Exception {
	boost();
    }

    public void done() throws Exception {}

  /**
   * Performs a single boosting iteration.
   * Will add a new splitter node and two prediction nodes to the tree
   * (unless merging takes place).
   *
   * @exception Exception if try to boost without setting up tree first
   */
  private void boost() throws Exception {

    if (m_trainInstances == null)
      throw new Exception("Trying to boost with no training data");

    // perform the search
    searchForBestTest();

    if (m_Debug) {
      System.out.println("Best split found: "
			 + m_search_bestSplitter.getNumOfBranches() + "-way split on "
			 + m_search_bestSplitter.attributeString()
			 //+ "\nsmallestLeastSquares = " + m_search_smallestLeastSquares);
			 + "\nBestGain = " + m_search_smallestLeastSquares);
    }

    if (m_search_bestSplitter == null) return; // handle empty instances

    // create the new nodes for the tree, updating the weights
    for (int i=0; i<m_search_bestSplitter.getNumOfBranches(); i++) {
      Instances applicableInstances =
	m_search_bestSplitter.instancesDownBranch(i, m_search_bestPathInstances);
      double[] predictionValues = calcPredictionValues(applicableInstances);
      PredictionNode newPredictor = new PredictionNode(predictionValues);
      updateWeights(applicableInstances, predictionValues);
      m_search_bestSplitter.setChildForBranch(i, newPredictor);
    }

    // insert the new nodes
    m_search_bestInsertionNode.addChild((Splitter) m_search_bestSplitter);

    if (m_Debug) {
      System.out.println("Tree is now:\n" + toString(m_root, 1) + "\n");
      //System.out.println("Instances are now:\n" + m_trainInstances + "\n");
    }

    // free memory
    m_search_bestPathInstances = null;
  }

  private void updateWeights(Instances instances, double[] newPredictionValues) {

    for (int i=0; i<instances.numInstances(); i++)
      ((LADInstance) instances.instance(i)).updateWeights(newPredictionValues);
  }

  /**
   * Generates the m_staticPotentialSplitters2way 
   * vector to contain all possible nominal splits, and the m_numericAttIndices array to
   * index the numeric attributes in the training data.
   *
   */
  private void generateStaticPotentialSplittersAndNumericIndices() {
    
    m_staticPotentialSplitters2way = new FastVector();
    FastVector numericIndices = new FastVector();

    for (int i=0; i<m_trainInstances.numAttributes(); i++) {
      if (i == m_trainInstances.classIndex()) continue;
      if (m_trainInstances.attribute(i).isNumeric())
	numericIndices.addElement(new Integer(i));
      else {
	int numValues = m_trainInstances.attribute(i).numValues();
	if (numValues == 2) // avoid redundancy due to 2-way symmetry
	  m_staticPotentialSplitters2way.addElement(new TwoWayNominalSplit(i, 0));
	else for (int j=0; j<numValues; j++)
	  m_staticPotentialSplitters2way.addElement(new TwoWayNominalSplit(i, j));
      }
    }

    m_numericAttIndices = new int[numericIndices.size()];
    for (int i=0; i<numericIndices.size(); i++)
      m_numericAttIndices[i] = ((Integer)numericIndices.elementAt(i)).intValue();
  }

  /**
   * Performs a search for the best test (splitter) to add to the tree, by looking
   * for the largest weight change.
   *
   * @exception Exception if search fails
   */
  private void searchForBestTest() throws Exception {
    
    if (m_Debug) {
      System.out.println("Searching for best split...");
    }

    m_search_smallestLeastSquares = 0.0; //Double.POSITIVE_INFINITY;
    searchForBestTest(m_root, m_trainInstances);
  }

  /**
   * Recursive function that carries out search for the best test (splitter) to add to
   * this part of the tree, by looking for the largest weight change. Will try 2-way
   * and/or multi-way splits depending on the current state.
   *
   * @param currentNode the root of the subtree to be searched, and the current node 
   * being considered as parent of a new split
   * @param instances the instances that apply at this node
   * @exception Exception if search fails
   */
  private void searchForBestTest(PredictionNode currentNode, Instances instances)
    throws Exception
  {

    // keep stats
    m_nodesExpanded++;
    m_examplesCounted += instances.numInstances();
      
    // evaluate static splitters (nominal)
    for (Enumeration e = m_staticPotentialSplitters2way.elements();
         e.hasMoreElements(); ) {
      evaluateSplitter((Splitter) e.nextElement(), currentNode, instances);
    }

    if (m_Debug) {
	//System.out.println("Instances considered are: " + instances);
    }

    // evaluate dynamic splitters (numeric)
    for (int i=0; i<m_numericAttIndices.length; i++) {
      evaluateNumericSplit(currentNode, instances, m_numericAttIndices[i]);
    }

    if (currentNode.getChildren().size() == 0) return;

    // keep searching
    goDownAllPaths(currentNode, instances);
  }

  /**
   * Continues general multi-class search by investigating every node in the
   * subtree under currentNode.
   *
   * @param currentNode the root of the subtree to be searched
   * @param instances the instances that apply at this node
   * @exception Exception if search fails
   */
  private void goDownAllPaths(PredictionNode currentNode, Instances instances)
    throws Exception
  {
    
    for (Enumeration e = currentNode.children(); e.hasMoreElements(); ) {
      Splitter split = (Splitter) e.nextElement();
      for (int i=0; i<split.getNumOfBranches(); i++)
	searchForBestTest(split.getChildForBranch(i),
			  split.instancesDownBranch(i, instances));
    }
  }

  /**
   * Investigates the option of introducing a split under currentNode. If the 
   * split creates a weight change that is larger than has already been found it will
   * update the search information to record this as the best option so far. 
   *
   * @param split the splitter node to evaluate
   * @param currentNode the parent under which the split is to be considered
   * @param instances the instances that apply at this node
   * @exception Exception if something goes wrong 
   */
  private void evaluateSplitter(Splitter split, PredictionNode currentNode,
				Instances instances)
    throws Exception
  {
    
    double leastSquares = leastSquaresNonMissing(instances,split.attIndex);

    for (int i=0; i<split.getNumOfBranches(); i++)
      leastSquares -= leastSquares(split.instancesDownBranch(i, instances));

    if (m_Debug) {
      //System.out.println("Instances considered are: " + instances);
      System.out.print(split.getNumOfBranches() + "-way split on " + split.attributeString()
		       + " has leastSquares value of "
		       + Utils.doubleToString(leastSquares,3));
    }

    if (leastSquares > m_search_smallestLeastSquares) {
      if (m_Debug) {
	System.out.print(" (best so far)");
      }
      m_search_smallestLeastSquares = leastSquares;
      m_search_bestInsertionNode = currentNode;
      m_search_bestSplitter = split;
      m_search_bestPathInstances = instances;
    }
    if (m_Debug) {
      System.out.print("\n");
    }
  }

  private void evaluateNumericSplit(PredictionNode currentNode,
				    Instances instances, int attIndex)
  {
  
    double[] splitAndLS = findNumericSplitpointAndLS(instances, attIndex);
    double gain = leastSquaresNonMissing(instances,attIndex) - splitAndLS[1];
 
   if (m_Debug) {
     //System.out.println("Instances considered are: " + instances);
     System.out.print("Numeric split on " + instances.attribute(attIndex).name()
		      + " has leastSquares value of " 
		      //+ Utils.doubleToString(splitAndLS[1],3));
		      + Utils.doubleToString(gain,3));
    }

   if (gain > m_search_smallestLeastSquares) {
      if (m_Debug) {
	System.out.print(" (best so far)");
      }
      m_search_smallestLeastSquares = gain; //splitAndLS[1];
      m_search_bestInsertionNode = currentNode;
      m_search_bestSplitter = new TwoWayNumericSplit(attIndex, splitAndLS[0]);;
      m_search_bestPathInstances = instances;
    }
    if (m_Debug) {
      System.out.print("\n");
    }
  }

  private double[] findNumericSplitpointAndLS(Instances instances, int attIndex) {

      double allLS = leastSquares(instances);

    // all instances in right subset
    double[] term1L = new double[m_numOfClasses];
    double[] term2L = new double[m_numOfClasses];
    double[] term3L = new double[m_numOfClasses];
    double[] meanNumL = new double[m_numOfClasses];
    double[] meanDenL = new double[m_numOfClasses];

    double[] term1R = new double[m_numOfClasses];
    double[] term2R = new double[m_numOfClasses];
    double[] term3R = new double[m_numOfClasses];
    double[] meanNumR = new double[m_numOfClasses];
    double[] meanDenR = new double[m_numOfClasses];

    double temp1, temp2, temp3;

    double[] classMeans = new double[m_numOfClasses];
    double[] classTotals = new double[m_numOfClasses];

    // fill up RHS
    for (int j=0; j<m_numOfClasses; j++) { 
      for (int i=0; i<instances.numInstances(); i++) {
	LADInstance inst = (LADInstance) instances.instance(i);
	temp1 = inst.wVector[j] * inst.zVector[j];
	term1R[j] += temp1 * inst.zVector[j];
	term2R[j] += temp1;
	term3R[j] += inst.wVector[j];
	meanNumR[j] += inst.wVector[j] * inst.zVector[j];
      }
    }

    //leastSquares = term1 - (2.0 * u * term2) + (u * u * term3);

    double leastSquares;
    boolean newSplit;
    double smallestLeastSquares = Double.POSITIVE_INFINITY;
    double bestSplit = 0.0;
    double meanL, meanR;

    instances.sort(attIndex);

    for (int i=0; i<instances.numInstances()-1; i++) {// shift inst from right to left
      if (instances.instance(i+1).isMissing(attIndex)) break;
      if (instances.instance(i+1).value(attIndex) > instances.instance(i).value(attIndex))
	newSplit = true;
      else newSplit = false;
      LADInstance inst = (LADInstance) instances.instance(i);
      leastSquares = 0.0;
      for (int j=0; j<m_numOfClasses; j++) {   
	temp1 = inst.wVector[j] * inst.zVector[j];
	temp2 = temp1 * inst.zVector[j];
	temp3 = inst.wVector[j] * inst.zVector[j];
	term1L[j] += temp2;
	term2L[j] += temp1;
	term3L[j] += inst.wVector[j];
	term1R[j] -= temp2;
	term2R[j] -= temp1;
	term3R[j] -= inst.wVector[j];
	meanNumL[j] += temp3;
	meanNumR[j] -= temp3;
	if (newSplit) {
	  meanL = meanNumL[j] / term3L[j];
	  meanR = meanNumR[j] / term3R[j];
	  leastSquares += term1L[j] - (2.0 * meanL * term2L[j])
	    + (meanL * meanL * term3L[j]);
	  leastSquares += term1R[j] - (2.0 * meanR * term2R[j])
	    + (meanR * meanR * term3R[j]);
	}
      }
      if (m_Debug && newSplit)
      System.out.println(attIndex + "/" + 
			 ((instances.instance(i).value(attIndex) +
			   instances.instance(i+1).value(attIndex)) / 2.0) +
			 " = " + (allLS - leastSquares));

      if (newSplit && leastSquares < smallestLeastSquares) {
	bestSplit = (instances.instance(i).value(attIndex) +
		     instances.instance(i+1).value(attIndex)) / 2.0;
	smallestLeastSquares = leastSquares;
      }
    }
    double[] result = new double[2];
    result[0] = bestSplit;
    result[1] = smallestLeastSquares > 0 ? smallestLeastSquares : 0;
    return result;
  }

  private double leastSquares(Instances instances) {

    double numerator=0, denominator=0, w, t;
    double[] classMeans = new double[m_numOfClasses];
    double[] classTotals = new double[m_numOfClasses];

    for (int i=0; i<instances.numInstances(); i++) {
      LADInstance inst = (LADInstance) instances.instance(i);
      for (int j=0; j<m_numOfClasses; j++) {
	classMeans[j] += inst.zVector[j] * inst.wVector[j];
	classTotals[j] += inst.wVector[j];
      }
    }

    double numInstances = (double) instances.numInstances();
    for (int j=0; j<m_numOfClasses; j++) {
      if (classTotals[j] != 0) classMeans[j] /= classTotals[j];
    }

    for (int i=0; i<instances.numInstances(); i++) 
      for (int j=0; j<m_numOfClasses; j++) {
	LADInstance inst = (LADInstance) instances.instance(i);
	w = inst.wVector[j];
	t = inst.zVector[j] - classMeans[j];
	numerator += w * (t * t);
	denominator += w;
      }
    //System.out.println(numerator + " / " + denominator);
    return numerator > 0 ? numerator : 0;//  / denominator;
  }


  private double leastSquaresNonMissing(Instances instances, int attIndex) {

    double numerator=0, denominator=0, w, t;
    double[] classMeans = new double[m_numOfClasses];
    double[] classTotals = new double[m_numOfClasses];

    for (int i=0; i<instances.numInstances(); i++) {
      LADInstance inst = (LADInstance) instances.instance(i);
      for (int j=0; j<m_numOfClasses; j++) {
	  classMeans[j] += inst.zVector[j] * inst.wVector[j];
	  classTotals[j] += inst.wVector[j];
      }
    }

    double numInstances = (double) instances.numInstances();
    for (int j=0; j<m_numOfClasses; j++) {
      if (classTotals[j] != 0) classMeans[j] /= classTotals[j];
    }

    for (int i=0; i<instances.numInstances(); i++) 
      for (int j=0; j<m_numOfClasses; j++) {
	LADInstance inst = (LADInstance) instances.instance(i);
	if(!inst.isMissing(attIndex)) {
	    w = inst.wVector[j];
	    t = inst.zVector[j] - classMeans[j];
	    numerator += w * (t * t);
	    denominator += w;
	}
      }
    //System.out.println(numerator + " / " + denominator);
    return numerator > 0 ? numerator : 0;//  / denominator;
  }

  private double[] calcPredictionValues(Instances instances) {

    double[] classMeans = new double[m_numOfClasses];
    double meansSum = 0;
    double multiplier = ((double) (m_numOfClasses-1)) / ((double) (m_numOfClasses));

    double[] classTotals = new double[m_numOfClasses];

    for (int i=0; i<instances.numInstances(); i++) {
      LADInstance inst = (LADInstance) instances.instance(i);
      for (int j=0; j<m_numOfClasses; j++) {
	classMeans[j] += inst.zVector[j] * inst.wVector[j];
	classTotals[j] += inst.wVector[j];
      }
    }
    double numInstances = (double) instances.numInstances();
    for (int j=0; j<m_numOfClasses; j++) {
      if (classTotals[j] != 0) classMeans[j] /= classTotals[j];
      meansSum += classMeans[j];
    }
    meansSum /= m_numOfClasses;

    for (int j=0; j<m_numOfClasses; j++) {
      classMeans[j] = multiplier * (classMeans[j] - meansSum);
    }
    return classMeans;
  }

  /**
   * Returns the class probability distribution for an instance.
   *
   * @param instance the instance to be classified
   * @return the distribution the tree generates for the instance
   */
  public double[] distributionForInstance(Instance instance) {
    
    double[] predValues = new double[m_numOfClasses];
    for (int i=0; i<m_numOfClasses; i++) predValues[i] = 0.0;
    double[] distribution = predictionValuesForInstance(instance, m_root, predValues);
    double max = distribution[Utils.maxIndex(distribution)];
    for (int i=0; i<m_numOfClasses; i++) {
      distribution[i] = Math.exp(distribution[i] - max);
    }
    double sum = Utils.sum(distribution);
    if (sum > 0.0) Utils.normalize(distribution, sum);
    return distribution;
  }

  /**
   * Returns the class prediction values (votes) for an instance.
   *
   * @param inst the instance
   * @param currentNode the root of the tree to get the values from
   * @param currentValues the current values before adding the values contained in the
   * subtree
   * @return the class prediction values (votes)
   */
  private double[] predictionValuesForInstance(Instance inst, PredictionNode currentNode,
					       double[] currentValues) {
    
    double[] predValues = currentNode.getValues();
    for (int i=0; i<m_numOfClasses; i++) currentValues[i] += predValues[i];
    //for (int i=0; i<m_numOfClasses; i++) currentValues[i] = predValues[i];
    for (Enumeration e = currentNode.children(); e.hasMoreElements(); ) {
      Splitter split = (Splitter) e.nextElement();
      int branch = split.branchInstanceGoesDown(inst);
      if (branch >= 0)
	currentValues = predictionValuesForInstance(inst, split.getChildForBranch(branch),
						    currentValues);
    }
    return currentValues;
  }



  /** model output functions ************************************************************/

  /**
   * Returns a description of the classifier.
   *
   * @return a string containing a description of the classifier
   */
  public String toString() {
    
    String className = getClass().getName();
    if (m_root == null)
      return (className +" not built yet");
    else {
      return (className + ":\n\n" + toString(m_root, 1) +
	      "\nLegend: " + legend() +
	      "\n#Tree size (total): " +
	      numOfAllNodes(m_root) + 
	      "\n#Tree size (number of predictor nodes): " +
	      numOfPredictionNodes(m_root) + 
	      "\n#Leaves (number of predictor nodes): " +
	      numOfLeafNodes(m_root) + 
	      "\n#Expanded nodes: " +
	      m_nodesExpanded +
	      "\n#Processed examples: " +
	      m_examplesCounted + 
	      "\n#Ratio e/n: " + 
	      ((double)m_examplesCounted/(double)m_nodesExpanded)
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
  private String toString(PredictionNode currentNode, int level) {
    
    StringBuffer text = new StringBuffer();
    
    text.append(": ");
    double[] predValues = currentNode.getValues();
    for (int i=0; i<m_numOfClasses; i++) {
      text.append(Utils.doubleToString(predValues[i],3));
      if (i<m_numOfClasses-1) text.append(",");
    }
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
	  text.append(split.attributeString() + " " + split.comparisonString(j));
	  text.append(toString(child, level + 1));
	}
      }
    }
    return text.toString();
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
    //text.append("center=true\nsize=\"8.27,11.69\"\n");
    graphTraverse(m_root, text, 0, 0);
    return text.toString() +"}\n";
  }


  /**
   * Traverses the tree, graphing each node.
   *
   * @param currentNode the currentNode under investigation
   * @param text the string built so far
   * @param splitOrder the order the parent splitter was added to the tree
   * @param predOrder the order this predictor was added to the split
   * @exception Exception if something goes wrong
   */       
  protected void graphTraverse(PredictionNode currentNode, StringBuffer text,
			       int splitOrder, int predOrder)
    throws Exception
  {
    
    text.append("S" + splitOrder + "P" + predOrder + " [label=\"");
    double[] predValues = currentNode.getValues();
    for (int i=0; i<m_numOfClasses; i++) {
      text.append(Utils.doubleToString(predValues[i],3));
      if (i<m_numOfClasses-1) text.append(",");
    }
    if (splitOrder == 0) // show legend in root
      text.append(" (" + legend() + ")");
    text.append("\" shape=box style=filled]\n");
    for (Enumeration e = currentNode.children(); e.hasMoreElements(); ) {
      Splitter split = (Splitter) e.nextElement();
      text.append("S" + splitOrder + "P" + predOrder + "->" + "S" + split.orderAdded +
		  " [style=dotted]\n");
      text.append("S" + split.orderAdded + " [label=\"" + split.orderAdded + ": " +
		  split.attributeString() + "\"]\n");

      for (int i=0; i<split.getNumOfBranches(); i++) {
	PredictionNode child = split.getChildForBranch(i);
	if (child != null) {
	  text.append("S" + split.orderAdded + "->" + "S" + split.orderAdded + "P" + i +
		      " [label=\"" + split.comparisonString(i) + "\"]\n");
	  graphTraverse(child, text, split.orderAdded, i);
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
    if (m_numOfClasses == 1) {
      return ("-ve = " + classAttribute.value(0)
	      + ", +ve = " + classAttribute.value(1));
    } else {
      StringBuffer text = new StringBuffer();
      for (int i=0; i<m_numOfClasses; i++) {
	if (i>0) text.append(", ");
	text.append(classAttribute.value(i));
      }
      return text.toString();
    }
  }



  /** option handling  ******************************************************************/

  /**
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numOfBoostingIterationsTipText() {

    return "The number of boosting iterations to use, which determines the size of the tree.";
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
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {
    
    Vector newVector = new Vector(1);
    newVector.addElement(new Option(
				    "\tNumber of boosting iterations.\n"
				    +"\t(Default = 10)",
				    "B", 1,"-B <number of boosting iterations>"));

    Enumeration enu = super.listOptions();
    while (enu.hasMoreElements()) {
      newVector.addElement(enu.nextElement());
    }

    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -B num <br>
   * Set the number of boosting iterations
   * (default 10) <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    String bString = Utils.getOption('B', options);
    if (bString.length() != 0) setNumOfBoostingIterations(Integer.parseInt(bString));

    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of ADTree.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions() {
    
    String[] options = new String[2  + super.getOptions().length];

    int current = 0;
    options[current++] = "-B"; options[current++] = "" + getNumOfBoostingIterations();

    System.arraycopy(super.getOptions(), 0, options, current, super.getOptions().length);

    while (current < options.length) options[current++] = "";
    return options;
  }



  /** additional measures ***************************************************************/

  /**
   * Calls measure function for tree size.
   *
   * @return the tree size
   */
  public double measureTreeSize() {
    
    return numOfAllNodes(m_root);
  }

  /**
   * Calls measure function for leaf size.
   *
   * @return the leaf size
   */
  public double measureNumLeaves() {
    
    return numOfPredictionNodes(m_root);
  }

  /**
   * Calls measure function for leaf size.
   *
   * @return the leaf size
   */
  public double measureNumPredictionLeaves() {
    
    return numOfLeafNodes(m_root);
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
  public double measureExamplesCounted() {
    
    return m_examplesCounted;
  }

  /**
   * Returns an enumeration of the additional measure names.
   *
   * @return an enumeration of the measure names
   */
  public Enumeration enumerateMeasures() {
    
    Vector newVector = new Vector(5);
    newVector.addElement("measureTreeSize");
    newVector.addElement("measureNumLeaves");
    newVector.addElement("measureNumPredictionLeaves");
    newVector.addElement("measureNodesExpanded");
    newVector.addElement("measureExamplesCounted");
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
    
    if (additionalMeasureName.equals("measureTreeSize")) {
      return measureTreeSize();
    }
    else if (additionalMeasureName.equals("measureNodesExpanded")) {
      return measureNodesExpanded();
    }
    else if (additionalMeasureName.equals("measureNumLeaves")) {
      return measureNumLeaves();
    }
    else if (additionalMeasureName.equals("measureNumPredictionLeaves")) {
      return measureNumPredictionLeaves();
    }
    else if (additionalMeasureName.equals("measureExamplesCounted")) {
      return measureExamplesCounted();
    }
    else {throw new IllegalArgumentException(additionalMeasureName 
			      + " not supported (ADTree)");
    }
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
   * Returns the number of leaf nodes in a tree.
   *
   * @param root the root of the tree being measured
   * @return tree leaf size in number of prediction nodes
   */       
  protected int numOfLeafNodes(PredictionNode root) {
    
    int numSoFar = 0;
    if (root.getChildren().size() > 0) {
      for (Enumeration e = root.children(); e.hasMoreElements(); ) {
	Splitter split = (Splitter) e.nextElement();
	for (int i=0; i<split.getNumOfBranches(); i++)
	    numSoFar += numOfLeafNodes(split.getChildForBranch(i));
      }
    } else numSoFar = 1;
    return numSoFar;
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
  
  /** main functions ********************************************************************/

  /**
   * Builds a classifier for a set of instances.
   *
   * @param instances the instances to train the classifier with
   * @exception Exception if something goes wrong
   */
  public void buildClassifier(Instances instances) throws Exception {

    // set up the tree
    initClassifier(instances);

    // build the tree
    for (int T = 0; T < m_boostingIterations; T++) {
	boost();    
    }
  }

    public int predictiveError(Instances test) {
	int error = 0;
	for(int i = test.numInstances()-1; i>=0; i--) {
	    Instance inst = test.instance(i);
	    try {
		if (classifyInstance(inst) != inst.classValue())
		    error++;
	    } catch (Exception e) { error++;}
	}
	return error;
    }

  /**
   * Merges two trees together. Modifies the tree being acted on, leaving tree passed
   * as a parameter untouched (cloned). Does not check to see whether training instances
   * are compatible - strange things could occur if they are not.
   *
   * @param mergeWith the tree to merge with
   * @exception Exception if merge could not be performed
   */
  public void merge(LADTree mergeWith) throws Exception {
    
    if (m_root == null || mergeWith.m_root == null)
      throw new Exception("Trying to merge an uninitialized tree");
    if (m_numOfClasses != mergeWith.m_numOfClasses)
      throw new Exception("Trees not suitable for merge - "
			  + "different sized prediction nodes");
    m_root.merge(mergeWith.m_root);
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
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
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
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);
    
    return result;
  }

  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {    
    runClassifier(new LADTree(), argv);
  }

}

