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
 *    RandomTree.java
 *    Copyright (C) 2001 Richard Kirkby, Eibe Frank
 *
 */

package weka.classifiers.trees;

import weka.classifiers.*;
import weka.core.*;
import java.util.*;

/**
 * Class for constructing a tree that considers K random features at each node.
 * Performs no pruning.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision$
 */
public class RandomTree extends Classifier 
  implements OptionHandler, WeightedInstancesHandler, Randomizable {

  /** The subtrees appended to this tree. */ 
  protected RandomTree[] m_Successors;
    
  /** The attribute to split on. */
  protected int m_Attribute = -1;
    
  /** The split point. */
  protected double m_SplitPoint = Double.NaN;
    
  /** The class distribution from the training data. */
  protected double[] m_ClassDistribution = null;
    
  /** The header information. */
  protected Instances m_Info = null;
    
  /** The proportions of training instances going down each branch. */
  protected double[] m_Prop = null;
    
  /** Minimum number of instances for leaf. */
  protected double m_MinNum = 1.0;
    
  /** Debug info */
  protected boolean m_Debug = false;
  
  /** The number of attributes considered for a split. */
  protected int m_KValue = 1;

  /** The random seed to use. */
  protected int m_randomSeed = 1;

  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return  "Class for constructing a tree that considers K randomly " +
      " chosen attributes at each node. Performs no pruning.";
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String minNumTipText() {
    return "The minimum total weight of the instances in a leaf.";
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
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String KValueTipText() {
    return "Sets the number of randomly chosen attributes.";
  }
  
  /**
   * Get the value of K.
   *
   * @return Value of K.
   */
  public int getKValue() {
    
    return m_KValue;
  }
  
  /**
   * Set the value of K.
   *
   * @param k Value to assign to K.
   */
  public void setKValue(int k) {
    
    m_KValue = k;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String debugTipText() {
    return "Whether debug information is output to the console.";
  }

  /**
   * Get the value of Debug.
   *
   * @return Value of Debug.
   */
  public boolean getDebug() {
    
    return m_Debug;
  }
  
  /**
   * Set the value of Debug.
   *
   * @param newDebug Value to assign to Debug.
   */
  public void setDebug(boolean newDebug) {
    
    m_Debug = newDebug;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String seedTipText() {
    return "The random number seed used for selecting attributes.";
  }

  /**
   * Set the seed for random number generation.
   *
   * @param seed the seed 
   */
  public void setSeed(int seed) {

    m_randomSeed = seed;
  }
  
  /**
   * Gets the seed for the random number generations
   *
   * @return the seed for the random number generation
   */
  public int getSeed() {

    return m_randomSeed;
  }
  
  /**
   * Lists the command-line options for this classifier.
   */
  public Enumeration listOptions() {
    
    Vector newVector = new Vector(6);

    newVector.
      addElement(new Option("\tNumber of attributes to randomly investigate\n"
                            +"\t(<1 = int(log(#attributes)+1)).",
			    "K", 1, "-K <number of attributes>"));

    newVector.
      addElement(new Option("\tSet minimum number of instances per leaf.",
			    "M", 1, "-M <minimum number of instances>"));

    newVector.
      addElement(new Option("\tTurns debugging info on.",
			    "D", 0, "-D"));

    newVector
      .addElement(new Option("\tSeed for random number generator.\n"
			     + "\t(default 1)",
			     "S", 1, "-S"));

    return newVector.elements();
  } 

  /**
   * Gets options from this classifier.
   */
  public String[] getOptions() {
    
    String [] options = new String [10];
    int current = 0;
    options[current++] = "-K"; 
    options[current++] = "" + getKValue();
    options[current++] = "-M"; 
    options[current++] = "" + getMinNum();
    options[current++] = "-S";
    options[current++] = "" + getSeed();
    if (getDebug()) {
      options[current++] = "-D";
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
    
    String kValueString = Utils.getOption('K', options);
    if (kValueString.length() != 0) {
      m_KValue = Integer.parseInt(kValueString);
    } else {
      m_KValue = 1;
    }
    String minNumString = Utils.getOption('M', options);
    if (minNumString.length() != 0) {
      m_MinNum = Double.parseDouble(minNumString);
    } else {
      m_MinNum = 1;
    }
    String seed = Utils.getOption('S', options);
    if (seed.length() != 0) {
      setSeed(Integer.parseInt(seed));
    } else {
      setSeed(1);
    }
    m_Debug = Utils.getFlag('D', options);
    Utils.checkForRemainingOptions(options);
  }

  /**
   * Builds classifier.
   */
  public void buildClassifier(Instances data) throws Exception {

    // Make sure K value is in range
    if (m_KValue > data.numAttributes()-1) m_KValue = data.numAttributes()-1;
    if (m_KValue < 1) m_KValue = (int) Utils.log2(data.numAttributes())+1;

    // Check for non-nominal classes
    if (!data.classAttribute().isNominal()) {
      throw new UnsupportedClassTypeException("RandomTree: Nominal class, please.");
    }

    // Delete instances with missing class
    data = new Instances(data);
    data.deleteWithMissingClass();

    if (data.numInstances() == 0) {
      throw new IllegalArgumentException("RandomTree: zero training instances or all " +
					 "instances have missing class!");
    }

    if (data.numAttributes() == 1) {
      throw new IllegalArgumentException("RandomTree: Attribute missing. Need at least " +
					 "one attribute other than class attribute!");
    }

    if (data.checkForStringAttributes()) {
      throw new UnsupportedAttributeTypeException("Cannot handle string attributes!");
    }

    Instances train = data;

    // Create the attribute indices window
    int[] attIndicesWindow = new int[data.numAttributes() - 1];
    int j = 0;
    for (int i = 0; i < attIndicesWindow.length; i++) {
      if (j == data.classIndex())
        j++; // do not include the class
      attIndicesWindow[i] = j++;
    }

    // Compute initial class counts
    double[] classProbs = new double[train.numClasses()];
    for (int i = 0; i < train.numInstances(); i++) {
      Instance inst = train.instance(i);
      classProbs[(int) inst.classValue()] += inst.weight();
    }

    // Build tree 
    Random rand = data.getRandomNumberGenerator(m_randomSeed);
    buildTree(train, classProbs, new Instances(data, 0), m_MinNum, m_Debug, 
              attIndicesWindow, rand);
  }
  
  /**
   * Computes class distribution of an instance using the decision tree.
   */
  public double[] distributionForInstance(Instance instance) throws Exception {
    
    double[] returnedDist = null;
    
    if (m_Attribute > -1) {
      
      // Node is not a leaf
      if (instance.isMissing(m_Attribute)) {
        
        // Value is missing
        returnedDist = new double[m_Info.numClasses()];

        // Split instance up
        for (int i = 0; i < m_Successors.length; i++) {
          double[] help = m_Successors[i]
                                       .distributionForInstance(instance);
          if (help != null) {
            for (int j = 0; j < help.length; j++) {
              returnedDist[j] += m_Prop[i] * help[j];
            }
          }
        }
      } else if (m_Info.attribute(m_Attribute).isNominal()) {

        // For nominal attributes
        returnedDist = m_Successors[(int) instance.value(m_Attribute)]
                                    .distributionForInstance(instance);
      } else {

        // For numeric attributes
        if (instance.value(m_Attribute) < m_SplitPoint) {
          returnedDist = m_Successors[0]
                                      .distributionForInstance(instance);
        } else {
          returnedDist = m_Successors[1]
                                      .distributionForInstance(instance);
        }
      }
    }

    // Node is a leaf or successor is empty?
    if ((m_Attribute == -1) || (returnedDist == null)) {
 
      // Is node empty?
      if (m_ClassDistribution == null) {
        return null;
      }

      // Else return normalized distribution
      double[] normalizedDistribution = (double[]) m_ClassDistribution.clone();
      Utils.normalize(normalizedDistribution);
      return normalizedDistribution;
    } else {
      return returnedDist;
    }
  }

  /**
   * Outputs the decision tree as a graph
   */
  public String toGraph() {

    try {
      StringBuffer resultBuff = new StringBuffer();
      toGraph(resultBuff, 0);
      String result = "digraph Tree {\n" + "edge [style=bold]\n" + resultBuff.toString()
	+ "\n}\n";
      return result;
    } catch (Exception e) {
      return null;
    }
  }
  
  /**
   * Outputs one node for graph.
   */
  public int toGraph(StringBuffer text, int num) throws Exception {
    
    int maxIndex = Utils.maxIndex(m_ClassDistribution);
    String classValue = m_Info.classAttribute().value(maxIndex);
    
    num++;
    if (m_Attribute == -1) {
      text.append("N" + Integer.toHexString(hashCode()) +
		  " [label=\"" + num + ": " + classValue + "\"" +
		  "shape=box]\n");
    }else {
      text.append("N" + Integer.toHexString(hashCode()) +
		  " [label=\"" + num + ": " + classValue + "\"]\n");
      for (int i = 0; i < m_Successors.length; i++) {
	text.append("N" + Integer.toHexString(hashCode()) 
		    + "->" + 
		    "N" + Integer.toHexString(m_Successors[i].hashCode())  +
		    " [label=\"" + m_Info.attribute(m_Attribute).name());
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
	num = m_Successors[i].toGraph(text, num);
      }
    }
    
    return num;
  }
  
  /**
   * Outputs the decision tree.
   */
  public String toString() {
    
    if (m_Successors == null) {
      return "RandomTree: no model has been built yet.";
    } else {
      return     
	"\nRandomTree\n==========\n" + toString(0) + "\n" +
	"\nSize of the tree : " + numNodes();
    }
  }

  /**
   * Outputs a leaf.
   */
  protected String leafString() throws Exception {

    double sum = 0, maxCount = 0;
    int maxIndex = 0;
    if (m_ClassDistribution != null) {
      sum = Utils.sum(m_ClassDistribution);
      maxIndex = Utils.maxIndex(m_ClassDistribution);
      maxCount = m_ClassDistribution[maxIndex];
    } 
    return " : "
    + m_Info.classAttribute().value(maxIndex)
    + " ("
    + Utils.doubleToString(sum, 2)
    + "/"
    + Utils.doubleToString(sum - maxCount, 2) + ")";
  }
  
  /**
   * Recursively outputs the tree.
   */
  protected String toString(int level) {

    try {
      StringBuffer text = new StringBuffer();
      
      if (m_Attribute == -1) {
	
	// Output leaf info
	return leafString();
      } else if (m_Info.attribute(m_Attribute).isNominal()) {
	
	// For nominal attributes
	for (int i = 0; i < m_Successors.length; i++) {
	  text.append("\n");
	  for (int j = 0; j < level; j++) {
	    text.append("|   ");
	  }
	  text.append(m_Info.attribute(m_Attribute).name() + " = " +
		      m_Info.attribute(m_Attribute).value(i));
	  text.append(m_Successors[i].toString(level + 1));
	}
      } else {
	
	// For numeric attributes
	text.append("\n");
	for (int j = 0; j < level; j++) {
	  text.append("|   ");
	}
	text.append(m_Info.attribute(m_Attribute).name() + " < " +
		    Utils.doubleToString(m_SplitPoint, 2));
	text.append(m_Successors[0].toString(level + 1));
	text.append("\n");
	for (int j = 0; j < level; j++) {
	  text.append("|   ");
	}
	text.append(m_Info.attribute(m_Attribute).name() + " >= " +
		    Utils.doubleToString(m_SplitPoint, 2));
	text.append(m_Successors[1].toString(level + 1));
      }
      
      return text.toString();
    } catch (Exception e) {
      e.printStackTrace();
      return "RandomTree: tree can't be printed";
    }
  }     

  /**
   * Recursively generates a tree.
   */
  protected void buildTree(Instances data, double[] classProbs, Instances header,
                           double minNum, boolean debug, int[] attIndicesWindow,
                           Random random) throws Exception {

    // Store structure of dataset, set minimum number of instances
    m_Info = header;
    m_Debug = debug;
    m_MinNum = minNum;

    // Make leaf if there are no training instances
    if (data.numInstances() == 0) {
      m_Attribute = -1;
      m_ClassDistribution = null;
      m_Prop = null;
      return;
    }

    // Check if node doesn't contain enough instances or is pure
    m_ClassDistribution = (double[]) classProbs.clone();

    if (Utils.sum(m_ClassDistribution) < 2 * m_MinNum
        || Utils.eq(m_ClassDistribution[Utils.maxIndex(m_ClassDistribution)], Utils
                    .sum(m_ClassDistribution))) {

      // Make leaf
      m_Attribute = -1;
      m_Prop = null;
      return;
    }

    // Compute class distributions and value of splitting
    // criterion for each attribute
    double[] vals = new double[data.numAttributes()];
    double[][][] dists = new double[data.numAttributes()][0][0];
    double[][] props = new double[data.numAttributes()][0];
    double[] splits = new double[data.numAttributes()];
    
    // Investigate K random attributes
    int attIndex = 0;
    int windowSize = attIndicesWindow.length;
    int k = m_KValue;
    boolean gainFound = false;
    while ((windowSize > 0) && (k-- > 0 || !gainFound)) {
      
      int chosenIndex = random.nextInt(windowSize);
      attIndex = attIndicesWindow[chosenIndex];
      
      // shift chosen attIndex out of window
      attIndicesWindow[chosenIndex] = attIndicesWindow[windowSize - 1];
      attIndicesWindow[windowSize - 1] = attIndex;
      windowSize--;
      
      splits[attIndex] = distribution(props, dists, attIndex, data);
      vals[attIndex] = gain(dists[attIndex], priorVal(dists[attIndex]));
      
      if (Utils.gr(vals[attIndex], 0))
        gainFound = true;
    }
      
    // Find best attribute
    m_Attribute = Utils.maxIndex(vals);
    double[][] distribution = dists[m_Attribute];

    // Any useful split found? 
    if (Utils.gr(vals[m_Attribute], 0)) {

      // Build subtrees
      m_SplitPoint = splits[m_Attribute];
      m_Prop = props[m_Attribute];
      Instances[] subsets = splitData(data);
      m_Successors = new RandomTree[distribution.length];
      for (int i = 0; i < distribution.length; i++) {
        m_Successors[i] = new RandomTree();
        m_Successors[i].setKValue(m_KValue);
        m_Successors[i].buildTree(subsets[i], distribution[i], header, m_MinNum, m_Debug,
                                  attIndicesWindow, random);
      }

      // If all successors are non-empty, we don't need to store the class distribution
      boolean emptySuccessor = false;
      for (int i = 0; i < subsets.length; i++) {
        if (m_Successors[i].m_ClassDistribution == null) {
          emptySuccessor = true;
          break;
        }
      }
      if (!emptySuccessor) {
        m_ClassDistribution = null;
      }
    } else {

      // Make leaf
      m_Attribute = -1;
    }
  }

  /**
   * Computes size of the tree.
   */
  public int numNodes() {
    
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
  protected Instances[] splitData(Instances data) throws Exception {

    // Allocate array of Instances objects
    Instances[] subsets = new Instances[m_Prop.length];
    for (int i = 0; i < m_Prop.length; i++) {
      subsets[i] = new Instances(data, data.numInstances());
    }

    // Go through the data
    for (int i = 0; i < data.numInstances(); i++) {

      // Get instance
      Instance inst = data.instance(i);

      // Does the instance have a missing value?
      if (inst.isMissing(m_Attribute)) {
        
        // Split instance up
        for (int k = 0; k < m_Prop.length; k++) {
          if (m_Prop[k] > 0) {
            Instance copy = (Instance)inst.copy();
            copy.setWeight(m_Prop[k] * inst.weight());
            subsets[k].add(copy);
          }
        }

        // Proceed to next instance
        continue;
      }

      // Do we have a nominal attribute?
      if (data.attribute(m_Attribute).isNominal()) {
        subsets[(int)inst.value(m_Attribute)].add(inst);

        // Proceed to next instance
        continue;
      }

      // Do we have a numeric attribute?
      if (data.attribute(m_Attribute).isNumeric()) {
        subsets[(inst.value(m_Attribute) < m_SplitPoint) ? 0 : 1].add(inst);

        // Proceed to next instance
        continue;
      }
      
      // Else throw an exception
      throw new IllegalArgumentException("Unknown attribute type");
    }

    // Save memory
    for (int i = 0; i < m_Prop.length; i++) {
      subsets[i].compactify();
    }

    // Return the subsets
    return subsets;
  }

  /**
   * Computes class distribution for an attribute.
   */
  protected double distribution(double[][] props, double[][][] dists, int att, Instances data)
  throws Exception {

    double splitPoint = Double.NaN;
    Attribute attribute = data.attribute(att);
    double[][] dist = null;
    int indexOfFirstMissingValue = -1;

    if (attribute.isNominal()) {

      // For nominal attributes
      dist = new double[attribute.numValues()][data.numClasses()];
      for (int i = 0; i < data.numInstances(); i++) {
        Instance inst = data.instance(i);
        if (inst.isMissing(att)) {

          // Skip missing values at this stage
          if (indexOfFirstMissingValue < 0) {
            indexOfFirstMissingValue = i;
          }
          continue;
        }
        dist[(int) inst.value(att)][(int) inst.classValue()] += inst.weight();
      }
    } else {

      // For numeric attributes
      double[][] currDist = new double[2][data.numClasses()];
      dist = new double[2][data.numClasses()];

      // Sort data
      data.sort(att);

      // Move all instances into second subset
      for (int j = 0; j < data.numInstances(); j++) {
        Instance inst = data.instance(j);
        if (inst.isMissing(att)) {

          // Can stop as soon as we hit a missing value
          indexOfFirstMissingValue = j;
          break;
        }
        currDist[1][(int) inst.classValue()] += inst.weight();
      }

      // Value before splitting
      double priorVal = priorVal(currDist);

      // Save initial distribution
      for (int j = 0; j < currDist.length; j++) {
        System.arraycopy(currDist[j], 0, dist[j], 0, dist[j].length);
      }

      // Try all possible split points
      double currSplit = data.instance(0).value(att);
      double currVal, bestVal = -Double.MAX_VALUE;
      for (int i = 0; i < data.numInstances(); i++) {
        Instance inst = data.instance(i);
        if (inst.isMissing(att)) {

          // Can stop as soon as we hit a missing value
          break;
        }

        // Can we place a sensible split point here?
        if (inst.value(att) > currSplit) {

          // Compute gain for split point
          currVal = gain(currDist, priorVal);

          // Is the current split point the best point so far?
          if (currVal > bestVal) {

            // Store value of current point
            bestVal = currVal;

            // Save split point
            splitPoint = (inst.value(att) + currSplit) / 2.0;

            // Save distribution
            for (int j = 0; j < currDist.length; j++) {
              System.arraycopy(currDist[j], 0, dist[j], 0, dist[j].length);
            }
          }
        }
        currSplit = inst.value(att);

        // Shift over the weight
        currDist[0][(int) inst.classValue()] += inst.weight();
        currDist[1][(int) inst.classValue()] -= inst.weight();
      }
    }

    // Compute weights for subsets
    props[att] = new double[dist.length];
    for (int k = 0; k < props[att].length; k++) {
      props[att][k] = Utils.sum(dist[k]);
    }
    if (Utils.eq(Utils.sum(props[att]), 0)) {
      for (int k = 0; k < props[att].length; k++) {
        props[att][k] = 1.0 / (double) props[att].length;
      }
    } else {
      Utils.normalize(props[att]);
    }

    // Any instances with missing values ?
    if (indexOfFirstMissingValue > -1) {

      // Distribute weights for instances with missing values
      for (int i = indexOfFirstMissingValue; i < data.numInstances(); i++) {
        Instance inst = data.instance(i);
        if (attribute.isNominal()) {

          // Need to check if attribute value is missing
          if (inst.isMissing(att)) {
            for (int j = 0; j < dist.length; j++) {
              dist[j][(int) inst.classValue()] += props[att][j] * inst.weight();
            }
          }
        } else {

          // Can be sure that value is missing, so no test required
          for (int j = 0; j < dist.length; j++) {
            dist[j][(int) inst.classValue()] += props[att][j] * inst.weight();
          }
        }
      }
    }

    // Return distribution and split point
    dists[att] = dist;
    return splitPoint;
  }

  /**
   * Computes value of splitting criterion before split.
   */
  protected double priorVal(double[][] dist) {

    return ContingencyTables.entropyOverColumns(dist);
  }

  /**
   * Computes value of splitting criterion after split.
   */
  protected double gain(double[][] dist, double priorVal) {

    return priorVal - ContingencyTables.entropyConditionedOnRows(dist);
  }

  /**
   * Main method for this class.
   */
  public static void main(String[] argv) {

    try {
      System.out.println(Evaluation.evaluateModel(new RandomTree(), argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
}
