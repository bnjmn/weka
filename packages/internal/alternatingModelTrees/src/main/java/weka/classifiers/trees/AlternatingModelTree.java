/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    AlternatingModelTree.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.trees;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.functions.SimpleLinearRegression;
import weka.classifiers.rules.ZeroR;
import weka.classifiers.Classifier;
import weka.classifiers.IterativeClassifier;

import weka.core.*;
import weka.filters.Filter;
import weka.filters.supervised.attribute.NominalToBinary;
import weka.filters.unsupervised.attribute.RemoveUseless;

import weka.core.Capabilities.Capability;

import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Collections;

import java.io.Serializable;

/**
 <!-- globalinfo-start -->
 * Grows an alternating model tree by minimising squared error. Nominal attributes are converted to binary numeric ones before the tree is built, using the supervised version of NominalToBinary.<br/>
 * <br/>
 * For more information see<br/>
 * <br/>
 * Eibe Frank, Michael Mayo, Stefan Kramer: Alternating Model Trees. In: Proceedings of the ACM Symposium on Applied Computing, Data Mining Track, 2015.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -I &lt;number of iterations&gt;
 *  Set the number of iterations to perform. (default 10).</pre>
 * 
 * <pre> -H &lt;double&gt;
 *  Set shrinkage parameter (default 1.0).</pre>
 * 
 * <pre> -B
 *  Build a decision tree instead of an alternating tree.</pre>
 * 
 * <pre> -output-debug-info
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -do-not-check-capabilities
 *  If set, classifier capabilities are not checked before classifier is built
 *  (use with caution).</pre>
 * 
 <!-- options-end -->
 *
 * @version $Revision: $
 */
public class AlternatingModelTree extends AbstractClassifier 
  implements WeightedInstancesHandler, IterativeClassifier, Drawable, TechnicalInformationHandler, RevisionHandler {

  /** For serialization */
  static final long serialVersionUID = -7716785668198681288L;

  /** A reference to the training data used at training time */
  protected Instances m_Data;

  /** The number of iterations to use */
  protected int m_NumberOfIterations = 10;

  /** The  number of boosting iterations performed in this session of iterating */
  protected int m_numBoostItsPerformedThisSession;

  /** The shrinkage parameter */
  protected double m_Shrinkage = 1.0;

  /** Whether to build a decision tree instance of an alternating tree */
  protected boolean m_BuildDecisionTree;

  /** List of prediction nodes */
  protected ArrayList<PredictionNode> m_PredictionNodes;

  /** Filter to convert nominal attributes to binary */
  private NominalToBinary m_nominalToBinary;

  /** Filter for removing useless attributes */
  private RemoveUseless m_removeUseless;

  /** Flag used to enable the ability to resume boosting at a later date */
  protected boolean m_resume;

  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return  "Grows an alternating model tree by minimising squared error. Nominal attributes are converted " +
            "to binary numeric ones before the tree is built, using the supervised version of NominalToBinary.\n\n" +
            "For more information see\n\n" +
            getTechnicalInformation();
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing detailed
   * information about the technical background of this class, e.g., paper
   * reference or book this class is based on.
   *
   * @return the technical information about this class
   */
  @Override
  public TechnicalInformation getTechnicalInformation() {

    TechnicalInformation result;

    result = new TechnicalInformation(TechnicalInformation.Type.INPROCEEDINGS);
    result.setValue(TechnicalInformation.Field.AUTHOR, "Eibe Frank, Michael Mayo and Stefan Kramer");
    result.setValue(TechnicalInformation.Field.TITLE, "Alternating Model Trees");
    result.setValue(TechnicalInformation.Field.YEAR, "2015");
    result.setValue(TechnicalInformation.Field.BOOKTITLE, "Proceedings of the ACM Symposium on Applied Computing, Data Mining Track");
    result.setValue(TechnicalInformation.Field.PUBLISHER, "ACM Press");

    return result;
  }

  /**
   * Lists the command-line options for this classifier.
   * 
   * @return an enumeration over all commandline options
   */
  public Enumeration<Option> listOptions() {
    
    Vector<Option> newVector = new Vector<Option>(3);

    newVector.
      addElement(new Option("\tSet the number of iterations to perform. " +
			    "(default 10).",
			    "I", 1, "-I <number of iterations>"));
    
    newVector.addElement(
	new Option(
	    "\tSet shrinkage parameter (default 1.0).",
	    "H", 1, "-H <double>"));
    
    newVector.addElement(
	new Option(
	    "\tBuild a decision tree instead of an alternating tree.",
	    "B", 0, "-B"));

    newVector.addElement(new Option("\t" + resumeTipText() + "\n",
      "resume", 0, "-resume"));

    newVector.addAll(Collections.list(super.listOptions()));

    return newVector.elements();
  } 

  /**
   * Gets options from this classifier.
   * 
   * @return the options for the current setup
   */
  public String[] getOptions() {

    Vector<String> options = new Vector<String>();
    
    Collections.addAll(options, super.getOptions());

    options.add("-I"); 
    options.add("" + getNumberOfIterations());

    options.add("-H");
    options.add("" + getShrinkage());

    if (getBuildDecisionTree()) {
      options.add("-B");
    }

    if (getResume()) {
      options.add("-resume");
    }

    return options.toArray(new String[0]);
  }

  /**
   * Parses a given list of options. <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -I &lt;number of iterations&gt;
   *  Set the number of iterations to perform. (default 10).</pre>
   * 
   * <pre> -H &lt;double&gt;
   *  Set shrinkage parameter (default 1.0).
   * </pre>
   * 
   * <pre> -B
   *  Build a decision tree instead of an alternating tree. .
   * </pre>
   * 
   * <pre> -output-debug-info
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   * <pre> -do-not-check-capabilities
   *  If set, classifier capabilities are not checked before classifier is built
   *  (use with caution).</pre>
   * 
   <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    super.setOptions(options);
    String numIterationsString = Utils.getOption('I', options);
    if (numIterationsString.length() != 0) {
      m_NumberOfIterations = Integer.parseInt(numIterationsString);
    } else {
      m_NumberOfIterations = 10;
    }
    String shrinkageString = Utils.getOption('H', options);
    if (shrinkageString.length() != 0) {
      setShrinkage(new Double(shrinkageString).doubleValue());
    } else {
      setShrinkage(1.0);
    }
    setBuildDecisionTree(Utils.getFlag('B', options));

    setResume(Utils.getFlag("resume", options));

    Utils.checkForRemainingOptions(options);
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numberOfIterationsTipText() {

    return "Sets the number of iterations to perform.";
  }

  /**
   * Get the value of NumberOfIterations.
   *
   * @return Value of NumberOfIterations.
   */
  public int getNumberOfIterations() {
    
    return m_NumberOfIterations;
  }
  
  /**
   * Set the value of NumberOfIterations.
   *
   * @param newNumberOfIterations Value to assign to NumberOfIterations.
   */
  public void setNumberOfIterations(int newNumberOfIterations) {
    
    m_NumberOfIterations = newNumberOfIterations;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String shrinkageTipText() {
    return "The value of the shrinkage parameter.";
  }

  /**
   * Get the value of Shrinkage.
   *
   * @return Value of Shrinkage.
   */
  public double getShrinkage() {
    
    return m_Shrinkage;
  }
  
  /**
   * Set the value of Shrinkage.
   *
   * @param newShrinkage Value to assign to Shrinkage.
   */
  public void setShrinkage(double newShrinkage) {
    
    m_Shrinkage = newShrinkage;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String buildDecisionTreeTipText() {
    return "Set to true if a decision tree is to be built.";
  }

  /**
   * Get the value of BuildDecisionTree.
   *
   * @return Value of BuildDecisionTree.
   */
  public boolean getBuildDecisionTree() {
    
    return m_BuildDecisionTree;
  }
  
  /**
   * Set the value of BuildDecisionTree.
   *
   * @param newBuildDecisionTree Value to assign to BuildDecisionTree.
   */
  public void setBuildDecisionTree(boolean newBuildDecisionTree) {
    
    m_BuildDecisionTree = newBuildDecisionTree;
  }

  /**
   * Tool tip text for the resume property
   *
   * @return the tool tip text for the finalize property
   */
  public String resumeTipText() {
    return "Set whether classifier can continue training after performing the"
      + "requested number of iterations. \n\tNote that setting this to true will "
      + "retain certain data structures which can increase the \n\t"
      + "size of the model.";
  }

  /**
   * If called with argument true, then the next time done() is called the model is effectively
   * "frozen" and no further iterations can be performed
   *
   * @param resume true if the model is to be finalized after performing iterations
   */
  public void setResume(boolean resume) {
    m_resume = resume;
  }

  /**
   * Returns true if the model is to be finalized (or has been finalized) after
   * training.
   *
   * @return the current value of finalize
   */
  public boolean getResume() {
    return m_resume;
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
    result.enable(Capability.NUMERIC_CLASS);
    result.enable(Capability.DATE_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);
    
    return result;
  }

  /** 
   * Class that stores split info.
   */
  protected class SplitInfo implements Serializable {

    /** Attribute to use for splitting the data */
    protected int m_AttributeIndex = -1;
    
    /** Split point or split value */
    protected double m_Split = -Double.MAX_VALUE;

    /** Worth of the split */
    protected double m_Worth = -Double.MAX_VALUE;

    /**
     * Used for debugging purposes.
     */
    public String toString() {

      return m_Data.attribute(m_AttributeIndex).name() + " " + m_Split + " " + 
        m_Worth;
    }
  }

  /**
   * Class that represents a splitter node.
   */
  protected class SplitterNode implements Serializable {

    /** Attribute to use for splitting the data */
    protected int m_AttributeIndex;
    
    /** Split point */
    protected double m_Split;

    /** References of tree attached prediction nodes. */
    protected PredictionNode m_Left;
    protected PredictionNode m_Right;
    protected PredictionNode m_Missing;

    /**
     * Outputs textual description of subtree attached to this node.
     */
    protected String toString(String prefix) {

      StringBuilder sb = new StringBuilder();

      sb.append(prefix + m_Data.attribute(m_AttributeIndex).name() + " <= " + 
                m_Split + "\n");
      sb.append(m_Left.toString(prefix + "  | "));
      sb.append(prefix + m_Data.attribute(m_AttributeIndex).name() + " > " + 
                m_Split + "\n");
      sb.append(m_Right.toString(prefix + "  | "));
      sb.append(prefix + m_Data.attribute(m_AttributeIndex).name() + " = ?\n");
      sb.append(m_Missing.toString(prefix + "  | "));
      return sb.toString();
    }
  }

  /**
   * Calculate residuals.
   */
  protected void calculateResiduals(Classifier c, int[] indices, double shrinkage) 
    throws Exception {
    
    // Update residuals
    for (int i = 0; i < indices.length; i++) {
      Instance inst = m_Data.instance(indices[i]);
      inst.setClassValue(inst.classValue() -
                         shrinkage * c.classifyInstance(m_Data.instance(indices[i])));
    }
  }

  /**
   * Class that represents a prediction node.
   */
  protected class PredictionNode implements Serializable {

    /** Model for this node. */
    protected Classifier m_Model = null;

    /** List of successors (if any) */
    protected List<SplitterNode> m_Successors;

    /** The indices of the instances at this node */
    protected int[] m_Indices;

    /** The size of this node.*/
    protected int m_Size;

    /**
     * Outputs textual description of subtree attached to this node.
     */
    protected String toString(String prefix) {

      StringBuilder sb = new StringBuilder();
      sb.append(prefix);
      if (m_Model instanceof ZeroR) {
        sb.append("Pred = " + m_Model.toString().replaceAll("ZeroR predicts class value: ", "") +
                " (" + m_Size + ")\n");
      } else {
        sb.append("Pred = " + m_Model.toString().replaceAll("Linear(.*\\n)", "").
                replaceAll("Predicting", " :").replaceAll("if attribute value is missing.", "for ?").
                replaceAll("(\\r|\\n)", "") +
                " (" + m_Size + ")\n");
      }
      for (SplitterNode splitter : m_Successors) {
        sb.append(splitter.toString(prefix));
      }
      return sb.toString();
    }

    /**
     * Constructs a new prediction node using a ZeroR model.
     */
    protected PredictionNode(Instances data) throws Exception {
      
      // Initialize indices
      int[] indices = new int[data.numInstances()];
      for (int i = 0; i < indices.length; i++) {
        indices[i] = i;
      }
      m_Indices = indices;
      
      // Build ZeroR model that predicts the mean target value
      m_Model = new ZeroR();
      m_Model.buildClassifier(data);

      // Update class values
      calculateResiduals(m_Model, indices, 1.0);

      // Set size of node
      m_Size = m_Indices.length;

      // Create empty list of successors
      m_Successors = new LinkedList<SplitterNode>();
    }

    /**
     * Constructs a new prediction node.
     */
    protected PredictionNode(int[] indices) throws Exception {
      
      // Indices to the instances that make it to this node
      m_Indices = indices;

      // Compute linear regression model
      m_Model = buildModel(getData(indices));

      // Update class values
      calculateResiduals(m_Model, indices, m_Shrinkage);

      // Set size of node
      m_Size = m_Indices.length;

      // Create empty list of successors
      m_Successors = new LinkedList<SplitterNode>();
    }

    /**
     * Produces a dataset with residuals as class values.
     */
    protected Instances getData(int[] indices) {

      // Get training data
      Instances data = new Instances(m_Data, 0);
      for (int i = 0; i < indices.length; i++) {
        data.add(m_Data.instance(indices[i]));
      }
      return data;
    }

    /**
     * Builds model for given subset of data.
     */
    protected Classifier buildModel(Instances data) throws Exception {

      // Build and evaluate model
      if (data.numInstances() == 0) {
        ZeroR c = new ZeroR();
        c.buildClassifier(data);
        return c;
      } else {
        SimpleLinearRegression c = new SimpleLinearRegression(); // Could use arbitrary numeric base learner here
        c.setSuppressErrorMessage(true);
        c.setDoNotCheckCapabilities(true);
        c.buildClassifier(data);
        return c;
      }
    }

    /** 
     * Evaluates subset of data by building model for it
     * and returning SSE.
     */
    protected double evaluateModel(Instances data) throws Exception {

      Classifier c = buildModel(data);
      double SSE = 0;
      for (Instance inst : data) {
        double diff = inst.classValue() - m_Shrinkage * c.classifyInstance(inst);
        SSE += inst.weight() * diff * diff;
      }
      return SSE;
    }

    /**
     * Evaluate node expansion.
     */
    protected SplitInfo evaluateNodeExpansion() throws Exception {

      if (m_Debug) {
        System.out.println(this.toString(""));
      }

      // If there's no data, we can't make a split
      if (m_Indices.length == 0) {
        return null;
      }

      // If we build a decision tree, we only allow one successor
      if (m_BuildDecisionTree && m_Successors.size() >= 1) {
        return null;
      }

      // Object to keep track of best split
      SplitInfo split = new SplitInfo();

      // Calculate current error
      double currentSSE = 0;
      for (int j = 0; j < m_Indices.length; j++) {
        Instance inst = m_Data.instance(m_Indices[j]);
        currentSSE += inst.weight() * 
          inst.classValue() * inst.classValue();
      }

      if (m_Debug) {
        System.err.println("Current SSE: " + currentSSE);
      }

      // Find best attribute to split on
      for (int attIndex = 0; attIndex < m_Data.numAttributes(); attIndex++) {

        // Skip class attribute
        if (attIndex != m_Data.classIndex()) {

          if (m_Debug) {
            System.err.println(m_Data.attribute(attIndex));
          }

          // Collect attribute values for instances at node
          double[] vals = new double[m_Indices.length];
          for (int j = 0; j < m_Indices.length; j++) {
            vals[j] = m_Data.instance(m_Indices[j]).value(attIndex);
          }     
            
          // Find median
          double median = Utils.kthSmallestValue(vals, vals.length / 2);
          
          if (m_Debug) {
            System.err.println("median: " + median);
          }

          // Create three subsets of data
          ArrayList<Integer> firstSubset = new ArrayList<Integer>(vals.length);
          ArrayList<Integer> secondSubset = new ArrayList<Integer>(vals.length);
          ArrayList<Integer> missingSubset = new ArrayList<Integer>(vals.length);
          for (int j = 0; j < vals.length; j++) {
            if (Utils.isMissingValue(vals[j])) {
              missingSubset.add(m_Indices[j]);
            } else if (vals[j] <= median) {
              firstSubset.add(m_Indices[j]);
            } else {
              secondSubset.add(m_Indices[j]);
            }
          }
          if ((firstSubset.size() == 0 && missingSubset.size() == 0) ||
                  (secondSubset.size() == 0 && missingSubset.size() == 0) ||
                  (secondSubset.size() == 0 && firstSubset.size() == 0)) {
            continue;
          }

          // Calculate reduction in error
          double firstSSE = evaluateModel(getData(toIntArray(firstSubset)));
          double secondSSE = evaluateModel(getData(toIntArray(secondSubset)));
          double missingSSE = evaluateModel(getData(toIntArray(missingSubset)));
          double errorReduction = currentSSE - (firstSSE + secondSSE + missingSSE);
          if (m_Debug) {
            System.err.println("firstSSE " + firstSSE);
            System.err.println("secondSSE " + secondSSE);
            System.err.println("missingSSE " + missingSSE);
            System.err.println("errorReduction " + errorReduction);
          }
          
          // Best one observed so far?
          if (errorReduction > split.m_Worth) {
            split.m_Worth = errorReduction;
            split.m_AttributeIndex = attIndex;
            split.m_Split = median;
          }
        }
      }

      // Any useful attribute found?
      if (split.m_AttributeIndex < 0) {
        return null;
      }

      if (m_Debug) {
        System.err.println(split);
      }

      return split;
    }

    /**
     * Expand node.
     */
    protected SplitterNode expandNode(SplitInfo split) throws Exception {

      // Initialize splitter node
      SplitterNode splitterNode = new SplitterNode();
      splitterNode.m_Split = split.m_Split;
      splitterNode.m_AttributeIndex = split.m_AttributeIndex;

      // Indices of data in left and right subset
      ArrayList<Integer> leftIndices = new ArrayList<Integer>();
      ArrayList<Integer> rightIndices = new ArrayList<Integer>();
      ArrayList<Integer> missingIndices = new ArrayList<Integer>();
        
      // Go through all the instances at this node
      for (int i = 0; i < m_Indices.length; i++) {
        if (m_Data.instance(m_Indices[i]).isMissing(split.m_AttributeIndex)) {
          missingIndices.add(m_Indices[i]);
        } else {
          if (m_Data.instance(m_Indices[i]).value(split.m_AttributeIndex) <= split.m_Split) {
            leftIndices.add(m_Indices[i]);
          } else {
            rightIndices.add(m_Indices[i]);
          }
        }
      }

      // Create prediction nodes
      splitterNode.m_Left = new PredictionNode(toIntArray(leftIndices));
      splitterNode.m_Right = new PredictionNode(toIntArray(rightIndices));
      splitterNode.m_Missing = new PredictionNode(toIntArray(missingIndices));

      // Attach splitter node to this prediction node
      this.m_Successors.add(splitterNode);

      // Return splitter node
      return splitterNode;
    }

    /** 
     * Returns given array list of integer objets into int[] array.
     */
    protected int[] toIntArray(ArrayList<Integer> al) {
      
      int[] arr = new int[al.size()];
      int index = 0;
      for (Integer i : al) {
        arr[index++] = i;
      }
      return arr;
    }
  }

  /**
   * Returns the type of graph this classifier represents.
   * 
   * @return Drawable.TREE
   */
  @Override
  public int graphType() {
    return Drawable.TREE;
  }

  /**
   * Returns graph describing the tree.
   * 
   * @return the graph of the tree in dotty format
   * @exception Exception if something goes wrong
   */
  @Override
  public String graph() throws Exception {

    StringBuffer text = new StringBuffer();
    text.append("digraph AMTree {\n");
    graphTraverse(m_PredictionNodes.get(0), text, "P");
    return text.toString() + "}\n";
  }

  /**
   * Traverses the tree, graphing each node.
   * 
   * @param currentNode the currentNode under investigation
   * @param text the string built so far
   * @exception Exception if something goes wrong
   */
  protected void graphTraverse(PredictionNode currentNode, StringBuffer text,
    String id) throws Exception {

    text.append(id + " [label=\"");
    if (currentNode.m_Model instanceof ZeroR) {
      text.append(currentNode.m_Model.toString().replaceAll("ZeroR predicts class value: ", "") +
              " (" + currentNode.m_Size + ")");
    } else {
      text.append(currentNode.m_Model.toString().replaceAll("Linear(.*\\n)", "").
              replaceAll("Predicting", " :").replaceAll("if attribute value is missing.", "for ?").
              replaceAll("(\\r|\\n)", "") +
              " (" + currentNode.m_Size + ")");
    }
    text.append("\" shape=box style=filled");
    text.append("]\n");
    int splitIndex = 0;
    for (SplitterNode split : currentNode.m_Successors) {
      String nId = id + "S" + (splitIndex++);
      text.append(id + "->" + nId + " [style=dotted]\n");
      text.append(nId + " [label=\"" + 
                  Utils.backQuoteChars(m_Data.attribute(split.m_AttributeIndex).name())
                  + "\"]\n");
      
      if (split.m_Left != null) {
        text.append(nId + "->" + nId + "1P" + " [label=\""
                    +  " <= " + split.m_Split
                    + "\"]\n");
        graphTraverse(split.m_Left, text, nId + "1P");
      }
      if (split.m_Right != null) {
        text.append(nId + "->" + nId + "2P" + " [label=\""
                    +  " > " + split.m_Split
                    + "\"]\n");
        graphTraverse(split.m_Right, text, nId + "2P");
      }
      if (split.m_Missing != null) {
        text.append(nId + "->" + nId + "3P" + " [label=\""
                    +  " == ? " 
                    + "\"]\n");
        graphTraverse(split.m_Missing, text, nId + "3P");
      }
    }
  }

  /**
   * Outputs a textual description of the classifier.
   */
  public String toString() {

    if (m_PredictionNodes == null) {
      return "No model built yet.";
    }
    
    return m_PredictionNodes.get(0).toString("");
  }

  /**
   * Method used to make a prediction for an instance.
   */
  public double classifyInstance(Instance inst) throws Exception{

    m_nominalToBinary.input(inst);
    inst = m_nominalToBinary.output();
    m_removeUseless.input(inst);
    inst = m_removeUseless.output();

    // Construct queue and insert successors of root node
    Queue<PredictionNode> queue = new LinkedList<PredictionNode>();
    queue.add(m_PredictionNodes.get(0));

    // Traverse tree using queue
    PredictionNode predNode;
    double pred = 0;
    double shrinkage = 1.0;
    while ((predNode = queue.poll()) != null) {

      // Add node prediction to running sum
      pred += shrinkage * predNode.m_Model.classifyInstance(inst);

      // Go through all splitter nodes attached to the prediction node
      for (SplitterNode splitterNode : predNode.m_Successors) {

        // Add appropriate prediction node to queue
        if (inst.isMissing(splitterNode.m_AttributeIndex)) {
          queue.add(splitterNode.m_Missing);
        } else {
          if (inst.value(splitterNode.m_AttributeIndex) <= splitterNode.m_Split) {
            queue.add(splitterNode.m_Left);
          } else {
            queue.add(splitterNode.m_Right);
          }
        }
      }
      shrinkage = m_Shrinkage;
    }
    return pred;
  }

  /**
   * Initialize the classifier.
   */
  public void initializeClassifier(Instances data) throws Exception {

    m_numBoostItsPerformedThisSession = 0;

    if (m_Data == null || m_Data.numInstances() == 0) {
      // Can classifier handle the data?
      getCapabilities().testWithFail(data);

      // Prepare data
      data = new Instances(data);
      data.deleteWithMissingClass();

      m_nominalToBinary = new NominalToBinary();
      m_nominalToBinary.setInputFormat(data);
      data = Filter.useFilter(data, m_nominalToBinary);

      m_removeUseless = new RemoveUseless();
      m_removeUseless.setInputFormat(data);
      data = Filter.useFilter(data, m_removeUseless);

      // Store reference as global variable, store data as unsafe instances
      m_Data = new Instances(data, data.numInstances());
      for (Instance inst : data) {
        m_Data.add(new UnsafeInstance(inst));
      }

      // Initialize list of prediction nodes
      m_PredictionNodes = new ArrayList<PredictionNode>();

      // Add root node with ZeroR model that contains all the data
      m_PredictionNodes.add(new PredictionNode(m_Data));
    }
  }

  /**
   * Performs the next iteration.
   */
  public boolean next() throws Exception {

    if (m_numBoostItsPerformedThisSession >= m_NumberOfIterations) {
      return false;
    }

    // Try adding a splitter node to each prediction node
    SplitInfo bestSplit = null;
    PredictionNode bestNode = null;
    for (PredictionNode predNode : m_PredictionNodes) {
      
      // Compute best expansion
      SplitInfo split = predNode.evaluateNodeExpansion();
      if (split == null) {
        continue;
      }
      
      // Store best value
      if ((bestSplit == null) || (split.m_Worth > bestSplit.m_Worth)) {
        bestSplit = split;
        bestNode = predNode;
      }
    }
    
    // Have we found a best split?
    if (bestSplit == null) {
      return false;
    }
    
    // Expand node for good
    SplitterNode splitterNode = bestNode.expandNode(bestSplit);
    
    // Add resulting three prediction nodes to global list
    m_PredictionNodes.add(splitterNode.m_Left);
    m_PredictionNodes.add(splitterNode.m_Right);
    m_PredictionNodes.add(splitterNode.m_Missing);

    // Iteration was completely successfully
    m_numBoostItsPerformedThisSession++;
    return true;
  }

  /**
   * Clean up.
   */
  public void done() throws Exception {

    if (!m_resume) {
      // Don't need full dataset anymore
      m_Data = new Instances(m_Data, 0);

      // Clear indices in prediction nodes to save memory
      for (PredictionNode predNode : m_PredictionNodes) {
        predNode.m_Indices = null;
      }
    }
  }

  /**
   * Method used to build the classifier.
   */
  public void buildClassifier(Instances data) throws Exception {

    // Initialize classifier
    initializeClassifier(data);

    // Stop if there is only a class attribute
    if (data.numAttributes() == 1) {
      done();
      return;
    }

    // For the given number of iterations
    while(next()) {
    }
    /*for (int i = 0; i < m_NumberOfIterations; i++) {
      next();
    }*/

    // Clean up
    done();
  }

  /**
   * Private class implementing a DenseInstance with an unsafe setValue()
   * operation.
   */
  private class UnsafeInstance extends DenseInstance {

    /**
     * Added ID to avoid warning
     */
    private static final long serialVersionUID = 3210674215118962869L;

    /**
     * The constructor.
     * 
     * @param vals The instance whose value we want to copy.
     */
    public UnsafeInstance(Instance vals) {

      super(vals.numAttributes());
      for (int i = 0; i < vals.numAttributes(); i++) {
        m_AttValues[i] = vals.value(i);
      }
      m_Weight = vals.weight();
    }

    /**
     * Unsafe setValue() method.
     */
    @Override
    public void setValue(int attIndex, double value) {

      m_AttValues[attIndex] = value;
    }

    /**
     * We need a copy method that doesn't do anything...
     */
    @Override
    public Object copy() {

      return this;
    }
  }

  /**
   * Returns the revision string.
   *
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: $");
  }

  /**
   * Main class used to run this classifier from the command-line.
   */
  public static void main(String[] args) {

    runClassifier(new AlternatingModelTree(), args);
  }
}

