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

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.ContingencyTables;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Randomizable;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.core.Capabilities.Capability;

import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * Class for constructing a tree that considers K randomly  chosen attributes at each node. Performs no pruning.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -K &lt;number of attributes&gt;
 *  Number of attributes to randomly investigate
 *  (&lt;1 = int(log(#attributes)+1)).</pre>
 * 
 * <pre> -M &lt;minimum number of instances&gt;
 *  Set minimum number of instances per leaf.</pre>
 * 
 * <pre> -S &lt;num&gt;
 *  Seed for random number generator.
 *  (default 1)</pre>
 * 
 * <pre> -depth &lt;num&gt;
 *  The maximum depth of the tree, 0 for unlimited.
 *  (default 0)</pre>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 <!-- options-end -->
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 1.17 $
 */
public class RandomTree 
  extends Classifier 
  implements OptionHandler, WeightedInstancesHandler, Randomizable {

  /** for serialization */
  static final long serialVersionUID = 8934314652175299374L;
  
  /** The subtrees appended to this tree. */ 
  protected RandomTree[] m_Successors;
    
  /** The attribute to split on. */
  protected int m_Attribute = -1;
    
  /** The split point. */
  protected double m_SplitPoint = Double.NaN;
    
  /** The class distribution from the training data. */
  protected double[][] m_Distribution = null;
    
  /** The header information. */
  protected Instances m_Info = null;
    
  /** The proportions of training instances going down each branch. */
  protected double[] m_Prop = null;
    
  /** Class probabilities from the training data. */
  protected double[] m_ClassProbs = null;
    
  /** Minimum number of instances for leaf. */
  protected double m_MinNum = 1.0;
  
  /** The number of attributes considered for a split. */
  protected int m_KValue = 1;

  /** The random seed to use. */
  protected int m_randomSeed = 1;
  
  /** The maximum depth of the tree (0 = unlimited) */
  protected int m_MaxDepth = 0;

  /** a ZeroR model in case no model can be built from the data */
  protected Classifier m_ZeroR;
    
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
   * Returns the tip text for this property
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String maxDepthTipText() {
    return "The maximum depth of the tree, 0 for unlimited.";
  }

  /**
   * Get the maximum depth of trh tree, 0 for unlimited.
   *
   * @return 		the maximum depth.
   */
  public int getMaxDepth() {
    return m_MaxDepth;
  }
  
  /**
   * Set the maximum depth of the tree, 0 for unlimited.
   *
   * @param value 	the maximum depth.
   */
  public void setMaxDepth(int value) {
    m_MaxDepth = value;
  }
  
  /**
   * Lists the command-line options for this classifier.
   * 
   * @return an enumeration over all possible options
   */
  public Enumeration listOptions() {
    
    Vector newVector = new Vector();

    newVector.addElement(new Option(
	"\tNumber of attributes to randomly investigate\n"
	+"\t(<1 = int(log(#attributes)+1)).",
	"K", 1, "-K <number of attributes>"));

    newVector.addElement(new Option(
	"\tSet minimum number of instances per leaf.",
	"M", 1, "-M <minimum number of instances>"));

    newVector.addElement(new Option(
	"\tSeed for random number generator.\n"
	+ "\t(default 1)",
	"S", 1, "-S <num>"));

    newVector.addElement(new Option(
	"\tThe maximum depth of the tree, 0 for unlimited.\n"
	+ "\t(default 0)",
	"depth", 1, "-depth <num>"));

    Enumeration enu = super.listOptions();
    while (enu.hasMoreElements()) {
      newVector.addElement(enu.nextElement());
    }

    return newVector.elements();
  } 

  /**
   * Gets options from this classifier.
   * 
   * @return the options for the current setup
   */
  public String[] getOptions() {
    Vector        result;
    String[]      options;
    int           i;
    
    result = new Vector();
    
    result.add("-K");
    result.add("" + getKValue());
    
    result.add("-M");
    result.add("" + getMinNum());
    
    result.add("-S");
    result.add("" + getSeed());
    
    if (getMaxDepth() > 0) {
      result.add("-depth");
      result.add("" + getMaxDepth());
    }
    
    options = super.getOptions();
    for (i = 0; i < options.length; i++)
      result.add(options[i]);
    
    return (String[]) result.toArray(new String[result.size()]);
  }

  /**
   * Parses a given list of options. <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -K &lt;number of attributes&gt;
   *  Number of attributes to randomly investigate
   *  (&lt;1 = int(log(#attributes)+1)).</pre>
   * 
   * <pre> -M &lt;minimum number of instances&gt;
   *  Set minimum number of instances per leaf.</pre>
   * 
   * <pre> -S &lt;num&gt;
   *  Seed for random number generator.
   *  (default 1)</pre>
   * 
   * <pre> -depth &lt;num&gt;
   *  The maximum depth of the tree, 0 for unlimited.
   *  (default 0)</pre>
   * 
   * <pre> -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception{
    String	tmpStr;
    
    tmpStr = Utils.getOption('K', options);
    if (tmpStr.length() != 0) {
      m_KValue = Integer.parseInt(tmpStr);
    } else {
      m_KValue = 1;
    }
    
    tmpStr = Utils.getOption('M', options);
    if (tmpStr.length() != 0) {
      m_MinNum = Double.parseDouble(tmpStr);
    } else {
      m_MinNum = 1;
    }
    
    tmpStr = Utils.getOption('S', options);
    if (tmpStr.length() != 0) {
      setSeed(Integer.parseInt(tmpStr));
    } else {
      setSeed(1);
    }
    
    tmpStr = Utils.getOption("depth", options);
    if (tmpStr.length() != 0) {
      setMaxDepth(Integer.parseInt(tmpStr));
    } else {
      setMaxDepth(0);
    }
    
    super.setOptions(options);
    
    Utils.checkForRemainingOptions(options);
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

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
   * Builds classifier.
   * 
   * @param data the data to train with
   * @throws Exception if something goes wrong or the data doesn't fit
   */
  public void buildClassifier(Instances data) throws Exception {

    // Make sure K value is in range
    if (m_KValue > data.numAttributes()-1) m_KValue = data.numAttributes()-1;
    if (m_KValue < 1) m_KValue = (int) Utils.log2(data.numAttributes())+1;

    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    // remove instances with missing class
    data = new Instances(data);
    data.deleteWithMissingClass();
    
    // only class? -> build ZeroR model
    if (data.numAttributes() == 1) {
      System.err.println(
	  "Cannot build model (only class attribute present in data!), "
	  + "using ZeroR model instead!");
      m_ZeroR = new weka.classifiers.rules.ZeroR();
      m_ZeroR.buildClassifier(data);
      return;
    }
    else {
      m_ZeroR = null;
    }
    
    Instances train = data;

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

    // Create the attribute indices window
    int[] attIndicesWindow = new int[data.numAttributes()-1];
    int j=0;
    for (int i=0; i<attIndicesWindow.length; i++) {
      if (j == data.classIndex()) j++; // do not include the class
      attIndicesWindow[i] = j++;
    }

    // Build tree
    buildTree(sortedIndices, weights, train, classProbs,
	      new Instances(train, 0), m_MinNum, m_Debug,
	      attIndicesWindow, data.getRandomNumberGenerator(m_randomSeed), 0);

  }
  
  /**
   * Computes class distribution of an instance using the decision tree.
   * 
   * @param instance the instance to compute the distribution for
   * @return the computed class distribution
   * @throws Exception if computation fails
   */
  public double[] distributionForInstance(Instance instance) throws Exception {
    
    // default model?
    if (m_ZeroR != null) {
      return m_ZeroR.distributionForInstance(instance);
    }
    
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
   * Outputs the decision tree as a graph
   * 
   * @return the tree as a graph
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
   * 
   * @param text the buffer to append the output to
   * @param num unique node id
   * @return the next node id
   * @throws Exception if generation fails
   */
  public int toGraph(StringBuffer text, int num) throws Exception {
    
    int maxIndex = Utils.maxIndex(m_ClassProbs);
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
   * 
   * @return a string representation of the classifier
   */
  public String toString() {
    
    // only ZeroR model?
    if (m_ZeroR != null) {
      StringBuffer buf = new StringBuffer();
      buf.append(this.getClass().getName().replaceAll(".*\\.", "") + "\n");
      buf.append(this.getClass().getName().replaceAll(".*\\.", "").replaceAll(".", "=") + "\n\n");
      buf.append("Warning: No model could be built, hence ZeroR model is used:\n\n");
      buf.append(m_ZeroR.toString());
      return buf.toString();
    }
    
    if (m_Successors == null) {
      return "RandomTree: no model has been built yet.";
    } else {
      return     
	"\nRandomTree\n==========\n" + toString(0) + "\n" +
	"\nSize of the tree : " + numNodes() +
	(getMaxDepth() > 0 ? ("\nMax depth of tree: " + getMaxDepth()) : (""));
    }
  }

  /**
   * Outputs a leaf.
   * 
   * @return the leaf as string
   * @throws Exception if generation fails
   */
  protected String leafString() throws Exception {
    
    int maxIndex = Utils.maxIndex(m_Distribution[0]);

    return " : " + m_Info.classAttribute().value(maxIndex) + 
      " (" + Utils.doubleToString(Utils.sum(m_Distribution[0]), 2) + "/" + 
      Utils.doubleToString((Utils.sum(m_Distribution[0]) - 
			    m_Distribution[0][maxIndex]), 2) + ")";
  }
  
  /**
   * Recursively outputs the tree.
   * 
   * @param level the current level of the tree
   * @return the generated subtree
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
   * 
   * @param sortedIndices the indices of the instances
   * @param weights the weights of the instances
   * @param data the data to work with
   * @param classProbs the class distribution
   * @param header the header of the data
   * @param minNum the minimum number of instances per leaf
   * @param debug whether debugging is on
   * @param attIndicesWindow the attribute window to choose attributes from
   * @param random random number generator for choosing random attributes
   * @param depth the current depth
   * @throws Exception if generation fails
   */
  protected void buildTree(int[][] sortedIndices, double[][] weights,
			 Instances data, double[] classProbs, 
			 Instances header, double minNum, boolean debug,
			 int[] attIndicesWindow, Random random, int depth) 
    throws Exception {

    // Store structure of dataset, set minimum number of instances
    m_Info = header;
    m_Debug = debug;
    m_MinNum = minNum;

    // Make leaf if there are no training instances
    if (((data.classIndex() > 0) && (sortedIndices[0].length == 0)) ||
	((data.classIndex() == 0) && sortedIndices[1].length == 0)) {
      m_Distribution = new double[1][data.numClasses()];
      m_ClassProbs = null;
      return;
    }

    // Check if node doesn't contain enough instances or is pure 
    // or maximum depth reached
    m_ClassProbs = new double[classProbs.length];
    System.arraycopy(classProbs, 0, m_ClassProbs, 0, classProbs.length);
    if (Utils.sm(Utils.sum(m_ClassProbs), 2 * m_MinNum) ||
	Utils.eq(m_ClassProbs[Utils.maxIndex(m_ClassProbs)],
		 Utils.sum(m_ClassProbs)) || 
        ((getMaxDepth() > 0) && (depth >= getMaxDepth()))) {

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

    // Investigate K random attributes
    int attIndex = 0;
    int windowSize = attIndicesWindow.length;
    int k = m_KValue;
    boolean gainFound = false;
    while ((windowSize > 0) && (k-- > 0 || !gainFound)) {

      int chosenIndex = random.nextInt(windowSize);
      attIndex = attIndicesWindow[chosenIndex];
      
      // shift chosen attIndex out of window
      attIndicesWindow[chosenIndex] = attIndicesWindow[windowSize-1];
      attIndicesWindow[windowSize-1] = attIndex;
      windowSize--;

      splits[attIndex] = distribution(props, dists, attIndex,
				      sortedIndices[attIndex], 
				      weights[attIndex], data);
      vals[attIndex] = gain(dists[attIndex], priorVal(dists[attIndex]));

      if (Utils.gr(vals[attIndex], 0)) gainFound = true;
    }

    // Find best attribute
    m_Attribute = Utils.maxIndex(vals);
    m_Distribution = dists[m_Attribute];

    // Any useful split found?
    if (Utils.gr(vals[m_Attribute], 0)) {

      // Build subtrees
      m_SplitPoint = splits[m_Attribute];
      m_Prop = props[m_Attribute];
      int[][][] subsetIndices = 
	new int[m_Distribution.length][data.numAttributes()][0];
      double[][][] subsetWeights = 
	new double[m_Distribution.length][data.numAttributes()][0];
      splitData(subsetIndices, subsetWeights, m_Attribute, m_SplitPoint, 
		sortedIndices, weights, m_Distribution, data);
      m_Successors = new RandomTree[m_Distribution.length];
      for (int i = 0; i < m_Distribution.length; i++) {
	m_Successors[i] = new RandomTree();
	m_Successors[i].setKValue(m_KValue);
	m_Successors[i].setMaxDepth(getMaxDepth());
	m_Successors[i].buildTree(subsetIndices[i], subsetWeights[i], data, 
				  m_Distribution[i], header, m_MinNum, m_Debug,
				  attIndicesWindow, random, depth + 1);
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
   * 
   * @return the number of nodes
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
   * 
   * @param subsetIndices the sorted indices of the subset
   * @param subsetWeights the weights of the subset
   * @param att the attribute index
   * @param splitPoint the splitpoint for numeric attributes
   * @param sortedIndices the sorted indices of the whole set
   * @param weights the weights of the whole set
   * @param dist the distribution
   * @param data the data to work with
   * @throws Exception if something goes wrong
   */
  protected void splitData(int[][][] subsetIndices, double[][][] subsetWeights,
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
   * 
   * @param props
   * @param dists
   * @param att the attribute index
   * @param sortedIndices the sorted indices of the data
   * @param weights
   * @param data the data to work with
   * @throws Exception if something goes wrong
   */
  protected double distribution(double[][] props, double[][][] dists, int att, 
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
   * 
   * @param dist the distributions
   * @return the splitting criterion
   */
  protected double priorVal(double[][] dist) {

    return ContingencyTables.entropyOverColumns(dist);
  }

  /**
   * Computes value of splitting criterion after split.
   * 
   * @param dist the distributions
   * @param priorVal the splitting criterion
   * @return the gain after the split
   */
  protected double gain(double[][] dist, double priorVal) {

    return priorVal - ContingencyTables.entropyConditionedOnRows(dist);
  }

  /**
   * Main method for this class.
   * 
   * @param argv the commandline parameters
   */
  public static void main(String[] argv) {
    runClassifier(new RandomTree(), argv);
  }
}
