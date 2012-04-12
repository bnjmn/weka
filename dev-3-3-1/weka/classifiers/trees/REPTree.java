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
 *    REPTree.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package weka.classifiers.trees;

import weka.classifiers.*;
import weka.core.*;
import java.util.*;
import java.io.*;

/**
 * Fast decision tree learner. Builds a decision tree using
 * information gain and prunes it using reduced-error pruning. Only
 * sorts values for numeric attributes once. Missing values are dealt
 * with by splitting the corresponding instances into pieces (i.e. as
 * in C4.5).
 *
 * Valid options are: <p>
 *
 * -M number <br>
 * Set minimum number of instances per leaf (default 2). <p>
 *		    
 * -N number <br>
 * Number of folds for reduced error pruning (default 3). <p>
 *
 * -S number <br> 
 * Seed for random data shuffling (default 1). <p>
 *
 * -P <br>
 * No pruning. <p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $ 
 */
public class REPTree extends DistributionClassifier 
  implements OptionHandler, WeightedInstancesHandler, Drawable {

  /** An inner class for building and storing the tree structure */
  private class Tree implements Serializable {

    /** The subtrees of this tree. */ 
    private Tree[] m_Successors;
    
    /** The attribute to split on. */
    private int m_Attribute = -1;
    
    /** The split point. */
    private double m_SplitPoint = Double.NaN;
    
    /** The class distribution from the training data. */
    private double[][] m_Distribution = null;
    
    /** The header information (for printing the tree). */
    private Instances m_Info = null;
    
    /** The proportions of training instances going down each branch. */
    private double[] m_Prop = null;
    
    /** Class distribution of hold-out set at node. */
    private double[] m_HoldOutDist = null;
    
    /** Class probabilities from the training data. */
    private double[] m_ClassProbs = null;
  
    /**
     * Computes class distribution of an instance using the tree.
     */
    private double[] distributionForInstance(Instance instance) 
      throws Exception {
    
      double[] returnedDist = null;
      
      if (m_Attribute > -1) {
	
	// Node is not a leaf
	if (instance.isMissing(m_Attribute)) {
	  
	  // Value is missing
	  returnedDist = new double[m_Info.numClasses()];
	  // Split instance up
	  for (int i = 0; i < m_Successors.length; i++) {
	    double[] help = m_Successors[i].distributionForInstance(instance);
	    if (help != null) {
	      for (int j = 0; j < help.length; j++) {
		returnedDist[j] += m_Prop[i] * help[j];
	      }
	    }
	  }
	} else if (m_Info.attribute(m_Attribute).isNominal()) {
	  
	  // For nominal attributes
	  returnedDist =  m_Successors[(int)instance.value(m_Attribute)].
	    distributionForInstance(instance);
	} else {
	  
	  // For numeric attributes
	  if (Utils.sm(instance.value(m_Attribute), m_SplitPoint)) {
	    returnedDist = m_Successors[0].distributionForInstance(instance);
	  } else {
	    returnedDist = m_Successors[1].distributionForInstance(instance);
	  }
	}
      }
      if ((m_Attribute == -1) || (returnedDist == null)) {
	
	// Node is a leaf or successor is empty
	return m_ClassProbs;
      } else {
	return returnedDist;
      }
    }
  
    /**
     * Outputs one node for graph.
     */
    private int toGraph(StringBuffer text, int num,
			Tree parent) throws Exception {
      
      num++;
      if (m_Attribute == -1) {
	text.append("N" + Integer.toHexString(hashCode()) +
		    " [label=\"" + num + leafString(parent) +"\"" +
		    "shape=box]\n");
      } else {
	text.append("N" + Integer.toHexString(hashCode()) +
		    " [label=\"" + num + ": " + m_Info.attribute(m_Attribute).name() + 
		    "\"]\n");
	for (int i = 0; i < m_Successors.length; i++) {
	  text.append("N" + Integer.toHexString(hashCode()) 
		      + "->" + 
		      "N" + Integer.toHexString(m_Successors[i].hashCode())  +
		      " [label=\"");
	  if (m_Info.attribute(m_Attribute).isNumeric()) {
	    if (i == 0) {
	      text.append(" < " +
			  Utils.doubleToString(m_SplitPoint, 2));
	    } else {
	      text.append(" >= " +
			  Utils.doubleToString(m_SplitPoint, 2));
	    }
	  } else {
	    text.append(" = " + m_Info.attribute(m_Attribute).value(i));
	  }
	  text.append("\"]\n");
	  num = m_Successors[i].toGraph(text, num, this);
	}
      }
      
      return num;
    }

    /**
     * Outputs a leaf.
     */
    private String leafString(Tree parent) throws Exception {
    
      int maxIndex;
      if (Utils.eq(Utils.sum(m_Distribution[0]), 0)) {
	maxIndex = Utils.maxIndex(parent.m_ClassProbs);
      } else {
	maxIndex = Utils.maxIndex(m_ClassProbs);
      }
      return " : " + m_Info.classAttribute().value(maxIndex) + 
	" (" + Utils.doubleToString(Utils.sum(m_Distribution[0]), 2) + "/" + 
	Utils.doubleToString((Utils.sum(m_Distribution[0]) - 
			      m_Distribution[0][maxIndex]), 2) + ")" +
	" [" + Utils.doubleToString(Utils.sum(m_HoldOutDist), 2) + "/" + 
	Utils.doubleToString((Utils.sum(m_HoldOutDist) - 
			      m_HoldOutDist[maxIndex]), 2) + "]";
    }
  
    /**
     * Recursively outputs the tree.
     */
    private String toString(int level, Tree parent) {

      try {
	StringBuffer text = new StringBuffer();
      
	if (m_Attribute == -1) {
	
	  // Output leaf info
	  return leafString(parent);
	} else if (m_Info.attribute(m_Attribute).isNominal()) {
	
	  // For nominal attributes
	  for (int i = 0; i < m_Successors.length; i++) {
	    text.append("\n");
	    for (int j = 0; j < level; j++) {
	      text.append("|   ");
	    }
	    text.append(m_Info.attribute(m_Attribute).name() + " = " +
			m_Info.attribute(m_Attribute).value(i));
	    text.append(m_Successors[i].toString(level + 1, this));
	  }
	} else {
	
	  // For numeric attributes
	  text.append("\n");
	  for (int j = 0; j < level; j++) {
	    text.append("|   ");
	  }
	  text.append(m_Info.attribute(m_Attribute).name() + " < " +
		      Utils.doubleToString(m_SplitPoint, 2));
	  text.append(m_Successors[0].toString(level + 1, this));
	  text.append("\n");
	  for (int j = 0; j < level; j++) {
	    text.append("|   ");
	  }
	  text.append(m_Info.attribute(m_Attribute).name() + " >= " +
		      Utils.doubleToString(m_SplitPoint, 2));
	  text.append(m_Successors[1].toString(level + 1, this));
	}
      
	return text.toString();
      } catch (Exception e) {
	e.printStackTrace();
	return "Decision tree: tree can't be printed";
      }
    }     

    /**
     * Recursively generates a tree.
     */
    private void buildTree(int[][] sortedIndices, double[][] weights,
			   Instances data, double[] classProbs, 
			   Instances header, double minNum) 
      throws Exception {

      // Store structure of dataset, set minimum number of instances
      // and make space for potential info from pruning data
      m_Info = header;
      m_HoldOutDist = new double[data.numClasses()];

      // Make leaf if there are no training instances
      if (sortedIndices[0].length == 0) {
	m_Distribution = new double[1][data.numClasses()];
	m_ClassProbs = null;
	return;
      }

      // Check if node doesn't contain enough instances or is pure
      m_ClassProbs = new double[classProbs.length];
      System.arraycopy(classProbs, 0, m_ClassProbs, 0, classProbs.length);
      if (Utils.sm(Utils.sum(m_ClassProbs), 2 * minNum) ||
	  Utils.eq(m_ClassProbs[Utils.maxIndex(m_ClassProbs)],
		   Utils.sum(m_ClassProbs))) {

	// Make leaf
	m_Attribute = -1;
	m_Distribution = new double[1][m_ClassProbs.length];
	for (int i = 0; i < m_ClassProbs.length; i++) {
	  m_Distribution[0][i] = m_ClassProbs[i];
	}
	Utils.normalize(m_ClassProbs);
	return;
      }


      // Compute class distributions and value of splitting
      // criterion for each attribute
      double[] vals = new double[data.numAttributes()];
      double[][][] dists = new double[data.numAttributes()][0][0];
      double[][] props = new double[data.numAttributes()][0];
      double[] splits = new double[data.numAttributes()];
      for (int i = 0; i < data.numAttributes(); i++) {
	if (i != data.classIndex()) {
	  splits[i] = distribution(props, dists, i, sortedIndices[i], 
				   weights[i], data);
	  vals[i] = gain(dists[i], priorVal(dists[i]));
	}
      }

      // Find best attribute
      m_Attribute = Utils.maxIndex(vals);
      m_Distribution = dists[m_Attribute];

      // Check if there are at least two subsets with
      // required minimum number of instances
      int count = 0;
      for (int i = 0; i < m_Distribution.length; i++) {
	if (Utils.grOrEq(Utils.sum(m_Distribution[i]), minNum)) {
	  count++;
	}
	if (count > 1) {
	  break;
	}
      }

      // Any useful split found?
      if (Utils.gr(vals[m_Attribute], 0) && (count > 1)) {

	// Build subtrees
	m_SplitPoint = splits[m_Attribute];
	m_Prop = props[m_Attribute];
	int[][][] subsetIndices = 
	  new int[m_Distribution.length][data.numAttributes()][0];
	double[][][] subsetWeights = 
	  new double[m_Distribution.length][data.numAttributes()][0];
	splitData(subsetIndices, subsetWeights, m_Attribute, m_SplitPoint, 
		  sortedIndices, weights, m_Distribution, data);
	m_Successors = new Tree[m_Distribution.length];
	for (int i = 0; i < m_Distribution.length; i++) {
	  m_Successors[i] = new Tree();
	  m_Successors[i].buildTree(subsetIndices[i], subsetWeights[i], data, 
				    m_Distribution[i], header, minNum);
	}
      } else {
      
	// Make leaf
	m_Attribute = -1;
	m_Distribution = new double[1][m_ClassProbs.length];
	for (int i = 0; i < m_ClassProbs.length; i++) {
	  m_Distribution[0][i] = m_ClassProbs[i];
	}
      }

      // Normalize class counts
      Utils.normalize(m_ClassProbs);
    }

    /**
     * Computes size of the tree.
     */
    private int numNodes() {
    
      if (m_Attribute == -1) {
	return 1;
      } else {
	int size = 1;
	for (int i = 0; i < m_Successors.length; i++) {
	  size += m_Successors[i].numNodes();
	}
	return size;
      }
    }

    /**
     * Splits instances into subsets.
     */
    private void splitData(int[][][] subsetIndices, double[][][] subsetWeights,
			   int att, double splitPoint, 
			   int[][] sortedIndices, double[][] weights,
			   double[][] dist, Instances data) throws Exception {
    
      int j;
      int[] num;
   
      // For each attribute
      for (int i = 0; i < data.numAttributes(); i++) {
	if (i != data.classIndex()) {
	  if (data.attribute(att).isNominal()) {

	    // For nominal attributes
	    num = new int[data.attribute(att).numValues()];
	    for (int k = 0; k < num.length; k++) {
	      subsetIndices[k][i] = new int[sortedIndices[i].length];
	      subsetWeights[k][i] = new double[sortedIndices[i].length];
	    }
	    for (j = 0; j < sortedIndices[i].length; j++) {
	      Instance inst = data.instance(sortedIndices[i][j]);
	      if (inst.isMissing(att)) {

		// Split instance up
		for (int k = 0; k < num.length; k++) {
		  if (Utils.gr(m_Prop[k], 0)) {
		    subsetIndices[k][i][num[k]] = sortedIndices[i][j];
		    subsetWeights[k][i][num[k]] = m_Prop[k] * weights[i][j];
		    num[k]++;
		  }
		}
	      } else {
		int subset = (int)inst.value(att);
		subsetIndices[subset][i][num[subset]] = sortedIndices[i][j];
		subsetWeights[subset][i][num[subset]] = weights[i][j];
		num[subset]++;
	      }
	    }
	  } else {

	    // For numeric attributes
	    num = new int[2];
	    for (int k = 0; k < 2; k++) {
	      subsetIndices[k][i] = new int[sortedIndices[i].length];
	      subsetWeights[k][i] = new double[weights[i].length];
	    }
	    for (j = 0; j < sortedIndices[i].length; j++) {
	      Instance inst = data.instance(sortedIndices[i][j]);
	      if (inst.isMissing(att)) {

		// Split instance up
		for (int k = 0; k < num.length; k++) {
		  if (Utils.gr(m_Prop[k], 0)) {
		    subsetIndices[k][i][num[k]] = sortedIndices[i][j];
		    subsetWeights[k][i][num[k]] = m_Prop[k] * weights[i][j];
		    num[k]++;
		  }
		}
	      } else {
		int subset = Utils.sm(inst.value(att), splitPoint) ? 0 : 1;
		subsetIndices[subset][i][num[subset]] = sortedIndices[i][j];
		subsetWeights[subset][i][num[subset]] = weights[i][j];
		num[subset]++;
	      } 
	    }
	  }
	
	  // Trim arrays
	  for (int k = 0; k < num.length; k++) {
	    int[] copy = new int[num[k]];
	    System.arraycopy(subsetIndices[k][i], 0, copy, 0, num[k]);
	    subsetIndices[k][i] = copy;
	    double[] copyWeights = new double[num[k]];
	    System.arraycopy(subsetWeights[k][i], 0, copyWeights, 0, num[k]);
	    subsetWeights[k][i] = copyWeights;
	  }
	}
      }
    }

    /**
     * Computes class distribution for an attribute.
     */
    private double distribution(double[][] props, double[][][] dists, int att, 
				int[] sortedIndices,
				double[] weights, Instances data) 
      throws Exception {

      double splitPoint = Double.NaN;
      Attribute attribute = data.attribute(att);
      double[][] dist = null;
      int i;

      if (attribute.isNominal()) {

	// For nominal attributes
	dist = new double[attribute.numValues()][data.numClasses()];
	for (i = 0; i < sortedIndices.length; i++) {
	  Instance inst = data.instance(sortedIndices[i]);
	  if (inst.isMissing(att)) {
	    break;
	  }
	  dist[(int)inst.value(att)][(int)inst.classValue()] += weights[i];
	}
      } else {

	// For numeric attributes
	double[][] currDist = new double[2][data.numClasses()];
	dist = new double[2][data.numClasses()];

	// Move all instances into second subset
	for (int j = 0; j < sortedIndices.length; j++) {
	  Instance inst = data.instance(sortedIndices[j]);
	  if (inst.isMissing(att)) {
	    break;
	  }
	  currDist[1][(int)inst.classValue()] += weights[j];
	}
	double priorVal = priorVal(currDist);
	for (int j = 0; j < currDist.length; j++) {
	  System.arraycopy(currDist[j], 0, dist[j], 0, dist[j].length);
	}

	// Try all possible split points
	double currSplit = data.instance(sortedIndices[0]).value(att);
	double currVal, bestVal = -Double.MAX_VALUE;
	for (i = 0; i < sortedIndices.length; i++) {
	  Instance inst = data.instance(sortedIndices[i]);
	  if (inst.isMissing(att)) {
	    break;
	  }
	  if (Utils.gr(inst.value(att), currSplit)) {
	    currVal = gain(currDist, priorVal);
	    if (Utils.gr(currVal, bestVal)) {
	      bestVal = currVal;
	      splitPoint = (inst.value(att) + currSplit) / 2.0;
	      for (int j = 0; j < currDist.length; j++) {
		System.arraycopy(currDist[j], 0, dist[j], 0, dist[j].length);
	      }
	    } 
	  } 
	  currSplit = inst.value(att);
	  currDist[0][(int)inst.classValue()] += weights[i];
	  currDist[1][(int)inst.classValue()] -= weights[i];
	}
      }

      // Compute weights
      props[att] = new double[dist.length];
      for (int k = 0; k < props[att].length; k++) {
	props[att][k] = Utils.sum(dist[k]);
      }
      if (Utils.eq(Utils.sum(props[att]), 0)) {
	for (int k = 0; k < props[att].length; k++) {
	  props[att][k] = 1.0 / (double)props[att].length;
	}
      } else {
	Utils.normalize(props[att]);
      }
    
      // Any instances with missing values ?
      if (i < sortedIndices.length) {
	
	// Distribute counts
	while (i < sortedIndices.length) {
	  Instance inst = data.instance(sortedIndices[i]);
	  for (int j = 0; j < dist.length; j++) {
	    dist[j][(int)inst.classValue()] += props[att][j] * weights[i];
	  }
	  i++;
	}
      }

      // Return distribution and split point
      dists[att] = dist;
      return splitPoint;
    }      

    /**
     * Computes value of splitting criterion before split.
     */
    private double priorVal(double[][] dist) {

      return ContingencyTables.entropyOverColumns(dist);
    }

    /**
     * Computes value of splitting criterion after split.
     */
    private double gain(double[][] dist, double priorVal) {

      return priorVal - ContingencyTables.entropyConditionedOnRows(dist);
    }

    /**
     * Prunes the tree using the hold-out data (bottom-up).
     */
    private double reducedErrorPrune() throws Exception {

      // Compute error for leaf
      double errorLeaf = Utils.sum(m_HoldOutDist) - 
	m_HoldOutDist[Utils.maxIndex(m_ClassProbs)];

      // Is node leaf ? 
      if (m_Attribute == -1) {
	return errorLeaf;
      }

      // Prune all sub trees
      double errorTree = 0;
      for (int i = 0; i < m_Successors.length; i++) {
	if (m_Successors[i].m_ClassProbs == null) {
	  errorTree += Utils.sum(m_Successors[i].m_HoldOutDist) - 
	    m_Successors[i].m_HoldOutDist[Utils.maxIndex(m_ClassProbs)];
	} else {
	  errorTree += m_Successors[i].reducedErrorPrune();
	}
      }

      // Replace sub tree with leaf if error doesn't get worse
      if (Utils.grOrEq(errorTree, errorLeaf)) {
	m_Attribute = -1;
	double[][] newDist = new double[1][m_Info.numClasses()];
	for (int i = 0; i < m_Distribution.length; i++) {
	  for (int j = 0; j < m_Distribution[i].length; j++) {
	    newDist[0][j] += m_Distribution[i][j];
	  }
	}
	m_Distribution = newDist;
	return errorLeaf;
      } else {
	return errorTree;
      }
    }

    /**
     * Returns the error on the pruning data for the subtree.
     */
    private double errorTree() {

      // Is node leaf ? 
      if (m_Attribute == -1) {
	return  Utils.sum(m_HoldOutDist) - 
	  m_HoldOutDist[Utils.maxIndex(m_ClassProbs)];
      }

      // Sum errors from subtrees
      double errorTree = 0;
      for (int i = 0; i < m_Successors.length; i++) {
	if (m_Successors[i].m_ClassProbs == null) {
	  errorTree += Utils.sum(m_Successors[i].m_HoldOutDist) - 
	    m_Successors[i].m_HoldOutDist[Utils.maxIndex(m_ClassProbs)];
	} else {
	  errorTree += m_Successors[i].errorTree();
	}
      }
      return errorTree;
    }   

    /**
     * Inserts hold-out set into tree.
     */
    private void insertHoldOutSet(Instances data) throws Exception{

      for (int i = 0; i < data.numInstances(); i++) {
	insertHoldOutInstance(data.instance(i), data.instance(i).weight());
      }
    }

    /**
     * Inserts an instance from the hold-out set into the tree.
     */
    private void insertHoldOutInstance(Instance inst, double weight) 
      throws Exception {
    
      // Insert instance into hold-out class distribution
      m_HoldOutDist[(int)inst.classValue()] += weight;
      if (m_Attribute != -1) {
      
	// If node is not a leaf
	if (inst.isMissing(m_Attribute)) {
	  
	  // Distribute instance
	  for (int i = 0; i < m_Successors.length; i++) {
	    if (Utils.gr(m_Prop[i], 0)) {
	      m_Successors[i].insertHoldOutInstance(inst, weight * m_Prop[i]);
	    }
	  }
	} else {
	
	  if (m_Info.attribute(m_Attribute).isNominal()) {

	    // Treat nominal attributes
	    m_Successors[(int)inst.value(m_Attribute)].
	      insertHoldOutInstance(inst, weight);
	  } else {

	    // Treat numeric attributes
	    if (Utils.sm(inst.value(m_Attribute), m_SplitPoint)) {
	      m_Successors[0].insertHoldOutInstance(inst, weight);
	    } else {
	      m_Successors[1].insertHoldOutInstance(inst, weight);
	    }
	  }
	}
      }
    }
  }

  /** The Tree object */
  private Tree m_Tree = null;
    
  /** Number of folds for reduced error pruning. */
  private int m_NumFolds = 3;
    
  /** Seed for random data shuffling. */
  private int m_Seed = 1;
    
  /** Don't prune */
  private boolean m_NoPruning = false;

  /** The minimum number of instances per leaf. */
  private double m_MinNum = 2;
  
  /**
   * Get the value of NoPruning.
   *
   * @return Value of NoPruning.
   */
  public boolean getNoPruning() {
    
    return m_NoPruning;
  }
  
  /**
   * Set the value of NoPruning.
   *
   * @param newNoPruning Value to assign to NoPruning.
   */
  public void setNoPruning(boolean newNoPruning) {
    
    m_NoPruning = newNoPruning;
  }
  
  /**
   * Get the value of MinNum.
   *
   * @return Value of MinNum.
   */
  public double getMinNum() {
    
    return m_MinNum;
  }
  
  /**
   * Set the value of MinNum.
   *
   * @param newMinNum Value to assign to MinNum.
   */
  public void setMinNum(double newMinNum) {
    
    m_MinNum = newMinNum;
  }
  
  /**
   * Get the value of Seed.
   *
   * @return Value of Seed.
   */
  public int getSeed() {
    
    return m_Seed;
  }
  
  /**
   * Set the value of Seed.
   *
   * @param newSeed Value to assign to Seed.
   */
  public void setSeed(int newSeed) {
    
    m_Seed = newSeed;
  }
  
  /**
   * Get the value of NumFolds.
   *
   * @return Value of NumFolds.
   */
  public int getNumFolds() {
    
    return m_NumFolds;
  }
  
  /**
   * Set the value of NumFolds.
   *
   * @param newNumFolds Value to assign to NumFolds.
   */
  public void setNumFolds(int newNumFolds) {
    
    m_NumFolds = newNumFolds;
  }
  
  /**
   * Lists the command-line options for this classifier.
   */
  public Enumeration listOptions() {
    
    Vector newVector = new Vector(4);

    newVector.
      addElement(new Option("\tSet minimum number of instances per leaf " +
			    "(default 2).",
			    "M", 1, "-M <minimum number of instances>"));
    newVector.
      addElement(new Option("\tNumber of folds for reduced error pruning " +
			    "(default 3).",
			    "N", 1, "-N <number of folds>"));
    newVector.
      addElement(new Option("\tSeed for random data shuffling (default 1).",
			    "S", 1, "-S <seed>"));
    newVector.
      addElement(new Option("\tNo pruning.",
			    "P", 0, "-P"));

    return newVector.elements();
  } 

  /**
   * Gets options from this classifier.
   */
  public String[] getOptions() {
    
    String [] options = new String [7];
    int current = 0;
    options[current++] = "-M"; 
    options[current++] = "" + getMinNum();
    options[current++] = "-N"; 
    options[current++] = "" + getNumFolds();
    options[current++] = "-S"; 
    options[current++] = "" + getSeed();
    if (getNoPruning()) {
      options[current++] = "-P";
    }
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Parses a given list of options.
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception{
    
    String minNumString = Utils.getOption('M', options);
    if (minNumString.length() != 0) {
      m_MinNum = (double)Integer.parseInt(minNumString);
    } else {
      m_MinNum = 2;
    }
    String numFoldsString = Utils.getOption('N', options);
    if (numFoldsString.length() != 0) {
      m_NumFolds = Integer.parseInt(numFoldsString);
    } else {
      m_NumFolds = 3;
    }
    String seedString = Utils.getOption('S', options);
    if (seedString.length() != 0) {
      m_Seed = Integer.parseInt(seedString);
    } else {
      m_Seed = 1;
    }
    m_NoPruning = Utils.getFlag('P', options);
    Utils.checkForRemainingOptions(options);
  }

  /**
   * Computes size of the tree.
   */
  public int numNodes() {

    return m_Tree.numNodes();
  }

  /**
   * Builds classifier.
   */
  public void buildClassifier(Instances data) throws Exception {

    Random random = new Random(m_Seed);

    // Check for non-nominal classes
    if (!data.classAttribute().isNominal()) {
      throw new Exception("REPTree: nominal class, please.");
    }

    // Delete instances with missing class
    data = new Instances(data);
    data.deleteWithMissingClass();

    // Check for empty datasets
    if (data.numInstances() == 0) {
      throw new Exception("REPTree: zero training instances or all instances " +
			  "have missing class!");
    }

    // Randomize and stratify
    data.randomize(random);
    data.stratify(m_NumFolds);

    // Split data into training and pruning set
    Instances train = null;
    Instances prune = null;
    if (!m_NoPruning) {
      train = data.trainCV(m_NumFolds, 0);
      prune = data.testCV(m_NumFolds, 0);
    } else {
      train = data;
    }

    // Create array of sorted indices and weights
    int[][] sortedIndices = new int[train.numAttributes()][0];
    double[][] weights = new double[train.numAttributes()][0];
    double[] vals = new double[train.numInstances()];
    for (int j = 0; j < train.numAttributes(); j++) {
      if (j != train.classIndex()) {
	weights[j] = new double[train.numInstances()];
	if (train.attribute(j).isNominal()) {

	  // Handling nominal attributes. Putting indices of
	  // instances with missing values at the end.
	  sortedIndices[j] = new int[train.numInstances()];
	  int count = 0;
	  for (int i = 0; i < train.numInstances(); i++) {
	    Instance inst = train.instance(i);
	    if (!inst.isMissing(j)) {
	      sortedIndices[j][count] = i;
	      weights[j][count] = inst.weight();
	      count++;
	    }
	  }
	  for (int i = 0; i < train.numInstances(); i++) {
	    Instance inst = train.instance(i);
	    if (inst.isMissing(j)) {
	      sortedIndices[j][count] = i;
	      weights[j][count] = inst.weight();
	      count++;
	    }
	  }
	} else {
	  
	  // Sorted indices are computed for numeric attributes
	  for (int i = 0; i < train.numInstances(); i++) {
	    Instance inst = train.instance(i);
	    vals[i] = inst.value(j);
	  }
	  sortedIndices[j] = Utils.sort(vals);
	  for (int i = 0; i < train.numInstances(); i++) {
	    weights[j][i] = train.instance(sortedIndices[j][i]).weight();
	  }
	}
      }
    }

    // Compute initial class counts
    double[] classProbs = new double[train.numClasses()];
    for (int i = 0; i < train.numInstances(); i++) {
      Instance inst = train.instance(i);
      classProbs[(int)inst.classValue()] += inst.weight();
    }

    // Build tree
    m_Tree = new Tree();
    m_Tree.buildTree(sortedIndices, weights, train, classProbs,
		     new Instances(train, 0), m_MinNum);
    
    // Insert pruning data and perform reduced error pruning
    if (!m_NoPruning) {
      m_Tree.insertHoldOutSet(prune);
      m_Tree.reducedErrorPrune();
    }
  }

  /**
   * Computes class distribution of an instance using the tree.
   */
  public double[] distributionForInstance(Instance instance) 
    throws Exception {
  
    return m_Tree.distributionForInstance(instance);
  }

  /**
   * Outputs the decision tree as a graph
   */
  public String graph() throws Exception {

    StringBuffer resultBuff = new StringBuffer();
    m_Tree.toGraph(resultBuff, 0, null);
    String result = "digraph Tree {\n" + "edge [style=bold]\n" + resultBuff.toString()
      + "\n}\n";
    return result;
  }
  
  /**
   * Outputs the decision tree.
   */
  public String toString() {
    
    return     
      "\nREPTree\n============\n" + m_Tree.toString(0, null) + "\n" +
      "\nSize of the tree : " + numNodes();
  }

  /**
   * Main method for this class.
   */
  public static void main(String[] argv) {

    try {
      System.out.println(Evaluation.evaluateModel(new REPTree(), argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
}
