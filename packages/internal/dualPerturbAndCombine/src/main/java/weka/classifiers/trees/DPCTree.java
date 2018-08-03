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
 *    DPCTree.java
 *    Copyright (C) 2017-18 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.classifiers.trees;

import weka.classifiers.AbstractClassifier;
import weka.core.*;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

/**
 *
 * <!-- globalinfo-start -->
 * Class for building and using classification and regression trees based on the closed-form dual perturb and combine algorithm described in<br>
 * <br>
 * Pierre Geurts, Lous Wehenkel: Closed-form dual perturb and combine for tree-based models. In: Proceedings of the 22nd International Conference on Machine Learning, 233-240, 2005.
 * <br><br>
 * <!-- globalinfo-end -->
 *
 * <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{Geurts2005,
 *    author = {Pierre Geurts and Lous Wehenkel},
 *    booktitle = {Proceedings of the 22nd International Conference on Machine Learning},
 *    pages = {233-240},
 *    publisher = {ACM},
 *    title = {Closed-form dual perturb and combine for tree-based models},
 *    year = {2005}
 * }
 * </pre>
 *
 * <!-- technical-bibtex-end -->
 *
 * <!-- options-start -->
 * Valid options are: <br><br>
 *
 * <pre> -lambda &lt;double&gt;
 *  The value for the lambda parameter determining the amount of smoothing (default = 0.2).</pre>
 *
 * <pre> -output-debug-info
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 *
 * <pre> -do-not-check-capabilities
 *  If set, classifier capabilities are not checked before classifier is built
 *  (use with caution).</pre>
 *
 * <pre> -num-decimal-places
 *  The number of decimal places for the output of numbers in the model (default 2).</pre>
 *
 * <pre> -batch-size
 *  The desired batch size for batch prediction  (default 100).</pre>
 *
 * <!-- options-end -->
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 *
 * @version $Revision: ? $
 */
public class DPCTree extends AbstractClassifier implements TechnicalInformationHandler {

  static final long serialVersionUID = 2767537273715121226L;

  // The root node of the decision tree
  protected Node RootNode;

  // The lambda parameter of the DPC algorithm
  protected double Lambda = 0.2;

  // We need to store the standard deviation of each attribute in the full training set
  protected double[] globalStdDevs;

  @OptionMetadata(
          displayName = "lambda",
          description = "The value for the lambda parameter determining the amount of smoothing (default = 0.2).",
          commandLineParamName = "lambda", commandLineParamSynopsis = "-lambda <double>",
          displayOrder = 1)
  public double getLambda() {
    return Lambda;
  }
  public void setLambda(double lambda) {
    this.Lambda = lambda;
  }

  /**
   * Returns a string describing this classifier
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return
            "Class for building and using classification and regression trees based on the closed-form dual perturb and "+
            "combine algorithm described in\n\n" + getTechnicalInformation().toString();
  }

  /**
   * Returns an instance of a TechnicalInformation object.
   *
   * @return the technical information about this class
   */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation 	result;

    result = new TechnicalInformation(TechnicalInformation.Type.INPROCEEDINGS);
    result.setValue(TechnicalInformation.Field.AUTHOR, "Pierre Geurts and Lous Wehenkel");
    result.setValue(TechnicalInformation.Field.YEAR, "2005");
    result.setValue(TechnicalInformation.Field.TITLE, "Closed-form dual perturb and combine for tree-based models");
    result.setValue(TechnicalInformation.Field.BOOKTITLE, "Proceedings of the 22nd International Conference on Machine Learning");
    result.setValue(TechnicalInformation.Field.PAGES, "233-240");
    result.setValue(TechnicalInformation.Field.PUBLISHER, "ACM");

    return result;
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enable(Capabilities.Capability.NUMERIC_ATTRIBUTES);

    // class
    result.enable(Capabilities.Capability.NOMINAL_CLASS);
    result.enable(Capabilities.Capability.NUMERIC_CLASS);
    result.enable(Capabilities.Capability.MISSING_CLASS_VALUES);

    return result;
  }

  /**
   * An interface indicating objects storing node information, implemented by three node info classes.
   */
  protected interface NodeInfo extends Serializable {};

  /**
   * Class whose objects represent split nodes.
   */
  protected class SplitNodeInfo implements NodeInfo {

    // The attribute used for splitting
    protected Attribute SplitAttribute;

    // The split value
    protected double SplitValue;

    // The array of successor nodes
    protected Node[] Successors;

    /**
     * Constructs a SplitNodeInfo object
     *
     * @param splitAttribute the attribute that defines the split
     * @param splitValue the value used for the split
     * @param successors the array of successor nodes
     */
    public SplitNodeInfo(Attribute splitAttribute, double splitValue, Node[] successors) {
      SplitAttribute = splitAttribute;
      SplitValue = splitValue;
      Successors = successors;
    }
  }

  /**
   * Class whose objects represent leaf nodes.
   */
  protected class LeafNodeInfo implements NodeInfo {

    // The array of predictions
    protected double[] Prediction;

    /**
     * Constructs a LeafNodeInfo object.
     *
     * @param prediction the array of predictions to store at this node
     */
    public LeafNodeInfo(double[] prediction) {
      Prediction = prediction;
    }
  }

  /**
   * Class whose objects represent unexpanded nodes.
   */
  protected class UnexpandedNodeInfo implements NodeInfo {

    // The data to be used for expanding the node.
    protected Instances Data;

    /**
     * Constructs an UnexpandedNodeInfo object.
     *
     * @param data the data to be used for turning this node into an expanded node.
     */
    public UnexpandedNodeInfo(Instances data) {
      Data = data;
    }
  }

  /**
   * Class representing a node in the decision tree.
   */
  protected class Node implements Serializable {

    // The node information object that stores the actual information for this node.
    protected NodeInfo NodeInfo;

    /**
     * Constructs a node based on the give node info.
     *
     * @param nodeInfo an appropriate node information object
     */
    public Node(DPCTree.NodeInfo nodeInfo) {
      NodeInfo = nodeInfo;
    }
  }

  /**
   * Method for incrementally updating sufficient statistics based on a given instance.
   *
   * @param sufficientStatistics the array of sufficient statistics to update
   * @param instance the instance to use for updating
   */
  protected void updateSufficientStatistics(double[] sufficientStatistics, Instance instance) {

    if (instance.classAttribute().isNumeric()) {
      sufficientStatistics[0] += instance.classValue();
      sufficientStatistics[1] += instance.classValue() * instance.classValue();
      sufficientStatistics[2]++;
    } else {
      sufficientStatistics[(int)instance.classValue()]++;
    }
  }

  /**
   * Method for incrementally downdating sufficient statistics based on a given instance.
   *
   * @param sufficientStatistics the array of sufficient statistics to downdate
   * @param instance the instance to use for downdating
   */
  protected void downdateSufficientStatistics(double[] sufficientStatistics, Instance instance) {

    if (instance.classAttribute().isNumeric()) {
      sufficientStatistics[0] -= instance.classValue();
      sufficientStatistics[1] -= instance.classValue() * instance.classValue();
      sufficientStatistics[2]--;
    } else {
      sufficientStatistics[(int)instance.classValue()]--;
    }
  }

  /**
   * Method for caculating the sum of squared errors based on the given sufficient statistics.
   * Note that the calculation as implemented is not numerically stable.
   *
   * @param sum the sum of values
   * @param sumOfSquares the sum of squared values
   * @param numberOfSamples the number of samples
   * @return the sum of squared errors
   */
  protected double SSE(double sum, double sumOfSquares, double numberOfSamples) {

    return sumOfSquares - (sum * sum / numberOfSamples);
  }

  /**
   * Method that calculates the worth of a given split based on the sufficient statistics provided.
   *
   * @param suffStats the sufficient statistics to use for calculating the worth of the split
   * @param classAttribute the class attribute, used to check whether target is numeric or nominal
   * @return if the class is nominal, the symmetric uncertainty; otherwise, the reduction in the sum of squared errors
   */
  protected double computeWorth(double[][] suffStats, Attribute classAttribute) {

    if (classAttribute.isNumeric()) {
      return (SSE(suffStats[0][0] + suffStats[1][0], suffStats[0][1] + suffStats[1][1],
              suffStats[0][2] + suffStats[1][2]) -
              (SSE(suffStats[0][0], suffStats[0][1], suffStats[0][2]) +
                      SSE(suffStats[1][0], suffStats[1][1], suffStats[1][2]))) /
              (suffStats[0][2] + suffStats[1][2]);

    } else {
      return ContingencyTables.symmetricalUncertainty(suffStats);
    }
  }

  /**
   * Method that makes the given node into a leaf node by replacing the node information.
   *
   * @param node the node to turn into a leaf node
   * @return the leaf node
   */
  protected Node makeLeaf(Node node) {

    Instances data = ((UnexpandedNodeInfo)node.NodeInfo).Data;
    double[] pred;
    if (data.classAttribute().isNumeric()) {
      double sum = 0;
      for (Instance instance : data) {
        sum += instance.classValue();
      }
      pred = new double[1];
      pred[0] = sum / (double) data.numInstances();
    } else {
      pred = new double[data.numClasses()];
      for (Instance instance : data) {
        pred[(int)instance.classValue()]++;
      }
      Utils.normalize(pred);
    }
    node.NodeInfo = new LeafNodeInfo(pred);
    return node;
  }

  /**
   * Method that processes a node. Assumes that the given node is unexpanded. Turns the node
   * into a leaf node or split node as appropriate by replacing the node information.
   *
   * @param node the unexpanded node to process
   * @return the node with updated node information, turning it into a split node or leaf node
   */
  protected Node processNode(Node node) {

    Instances data = ((UnexpandedNodeInfo)node.NodeInfo).Data;
    if ((data.classAttribute().isNumeric() && data.numInstances() < 5) || (data.numInstances() < 2)) {
      return makeLeaf(node);
    }
    double bestWorth = 0;
    Attribute splitAttribute = null;
    double splitValue = Double.NaN;
    for (Attribute attribute : Collections.list(data.enumerateAttributes())) {
      data.sort(attribute);
      double[][] sufficientStatistics;
      if (data.classAttribute().isNumeric()) {
        sufficientStatistics = new double[2][3];
      } else {
        sufficientStatistics = new double[2][data.numClasses()];
      }
      for (Instance instance : data) {
        updateSufficientStatistics(sufficientStatistics[1], instance);
      }
      double oldValue = data.instance(0).value(attribute);
      for (Instance instance : data) {
        if (instance.value(attribute) > oldValue) {
          double worth = computeWorth(sufficientStatistics, data.classAttribute());
          if (worth > bestWorth) {
            splitAttribute = attribute;
            splitValue = (instance.value(attribute) + oldValue) / 2.0;
            bestWorth = worth;
          }
          oldValue = instance.value(attribute);
        }
        updateSufficientStatistics(sufficientStatistics[0], instance);
        downdateSufficientStatistics(sufficientStatistics[1], instance);
      }
    }
    if (splitAttribute == null) {
      return makeLeaf(node);
    }
    Instances[] subsets = new Instances[2];
    subsets[0] = new Instances(data, data.numInstances());
    subsets[1] = new Instances(data, data.numInstances());
    for (Instance instance : data) {
      subsets[instance.value(splitAttribute) < splitValue ? 0 : 1].add(instance);
    }
    Node[] successors = new Node[2];
    successors[0] = new Node(new UnexpandedNodeInfo(subsets[0]));
    successors[1] = new Node(new UnexpandedNodeInfo(subsets[1]));
    node.NodeInfo = new SplitNodeInfo(splitAttribute, splitValue, successors);
    return node;
  }

  /**
   * Method that builds the classifier from the given set of training instances.
   *
   * @param instances the training instances
   */
  public void buildClassifier(Instances instances) throws Exception {

    // can classifier handle the data?
    getCapabilities().testWithFail(instances);

    instances = new Instances(instances);
    instances.deleteWithMissingClass();
    Queue<Node> nodes = new LinkedList<Node>();
    RootNode = new Node(new UnexpandedNodeInfo(instances));
    nodes.add(RootNode);
    while (!nodes.isEmpty()) {
      Node node = processNode(nodes.remove());
      if (node.NodeInfo instanceof SplitNodeInfo) {
        nodes.addAll(Arrays.asList(((SplitNodeInfo)node.NodeInfo).Successors));
      }
    }
    globalStdDevs = instances.variances();
    for (int i = 0; i < globalStdDevs.length; i++) {
      globalStdDevs[i] = Math.sqrt(globalStdDevs[i]);
    }
  }

  /**
   * Method that updates the given estimates based on the given instance and the subtree attached to the
   * given node. The weight from the DPC calculation is the last parameter.
   *
   * @param distribution the estimates to be updated
   * @param instance the instance for which estimates are to be updated
   * @param node the node whose subtree we are considering
   * @param weight from the DPC calculation
   */
  protected void distributionForInstance(double[] distribution, Instance instance, Node node, double weight) {

    if (weight > 0) {
      if (node.NodeInfo instanceof LeafNodeInfo) {
        for (int i = 0; i < distribution.length; i++) {
          distribution[i] += weight * ((LeafNodeInfo) node.NodeInfo).Prediction[i];
        }
      } else {
        SplitNodeInfo splitInfo = (SplitNodeInfo) node.NodeInfo;
        if (Lambda <= 0) {
          int successorIndex = instance.value(splitInfo.SplitAttribute) < splitInfo.SplitValue ? 0 : 1;
          distributionForInstance(distribution, instance, splitInfo.Successors[successorIndex], 1.0);
        } else {
          double weightLeft = Statistics.normalProbability((splitInfo.SplitValue - instance.value(splitInfo.SplitAttribute)) /
                  (Lambda * globalStdDevs[splitInfo.SplitAttribute.index()]));
          distributionForInstance(distribution, instance, splitInfo.Successors[0], weight * weightLeft);
          distributionForInstance(distribution, instance, splitInfo.Successors[1], weight * (1.0 - weightLeft));
        }
      }
    }
  }

  /**
   * Method that returns estimated class probabilities for the given instance if the class is nominal. If the
   * class is numeric, it will return a single-element array with the estimated target value.
   *
   * @param instance the instance for which a prediction is to be generated.
   * @return the estimates obtained from the tree
   */
  public double[] distributionForInstance(Instance instance) {

    double[] distribution = new double[instance.numClasses()];
    distributionForInstance(distribution, instance, RootNode, 1.0);
    if (instance.classAttribute().isNominal()) {
      Utils.normalize(distribution);
    }
    return distribution;
  }

  /**
   * Method that returns a textual description of the subtree attached to the given node. The description is
   * returned in a string buffer.
   *
   * @param stringBuffer buffer to hold the description
   * @param node the node whose subtree is to be described
   * @param levelString the level of the node in the overall tree structure
   */
  protected void toString(StringBuffer stringBuffer, Node node, String levelString) {

    if (node.NodeInfo instanceof SplitNodeInfo) {
      stringBuffer.append("\n" +levelString + ((SplitNodeInfo) node.NodeInfo).SplitAttribute.name() + " < " +
              Utils.doubleToString(((SplitNodeInfo) node.NodeInfo).SplitValue, getNumDecimalPlaces()));
      toString(stringBuffer, ((SplitNodeInfo) node.NodeInfo).Successors[0], levelString + "|   ");
      stringBuffer.append("\n" + levelString + ((SplitNodeInfo) node.NodeInfo).SplitAttribute.name() + " >= " +
              Utils.doubleToString(((SplitNodeInfo) node.NodeInfo).SplitValue, getNumDecimalPlaces()));
      toString(stringBuffer, ((SplitNodeInfo) node.NodeInfo).Successors[1], levelString + "|   ");
    } else {
      double[] dist = ((LeafNodeInfo) node.NodeInfo).Prediction;
      stringBuffer.append(":");
      for (double pred : dist) {
        stringBuffer.append(" " + Utils.doubleToString(pred, getNumDecimalPlaces()));
      }
    }
  }

  /**
   * Method that returns a textual description of the classifier.
   *
   * @return the textual description as a string
   */
  public String toString() {

    if (RootNode == null) {
      return "DPCTree: No classifier built yet.";
    }
    StringBuffer stringBuffer = new StringBuffer();
    toString(stringBuffer, RootNode, "");
    return stringBuffer.toString();
  }

  /**
   * Returns the revision string.
   *
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: ? $");
  }

  /**
   * Main method to run this classifier from the command-line with the standard option handling.
   *
   * @param args the command-line options
   */
  public static void main(String[] args) {

    runClassifier(new DPCTree(), args);
  }
}

