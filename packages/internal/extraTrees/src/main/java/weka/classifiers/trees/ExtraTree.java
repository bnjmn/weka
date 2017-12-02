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
 *    ExtraTree.java
 *    Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.classifiers.trees;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Vector;

import weka.classifiers.RandomizableClassifier;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.ContingencyTables;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.PartitionGenerator;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;

/**
 * <!-- globalinfo-start --> Class for generating a single Extra-Tree. Use with
 * the RandomCommittee meta classifier to generate an Extra-Trees forest for
 * classification or regression. This classifier requires all predictors to be
 * numeric. Missing values are not allowed. Instance weights are taken into
 * account. For more information, see<br/>
 * <br/>
 * Pierre Geurts, Damien Ernst, Louis Wehenkel (2006). Extremely randomized
 * trees. Machine Learning. 63(1):3-42.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- technical-bibtex-start --> BibTeX:
 * 
 * <pre>
 * &#64;article{Geurts2006,
 *    author = {Pierre Geurts and Damien Ernst and Louis Wehenkel},
 *    journal = {Machine Learning},
 *    number = {1},
 *    pages = {3-42},
 *    title = {Extremely randomized trees},
 *    volume = {63},
 *    year = {2006}
 * }
 * </pre>
 * <p/>
 * <!-- technical-bibtex-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -K &lt;number of attributes&gt;
 *  Number of attributes to randomly choose at a node. If values is -1, (m - 1) will be used for regression problems, and Math.rint(sqrt(m - 1)) for classification problems, where m is the number of predictors, as specified in Geurts et al. (default -1).
 * </pre>
 * 
 * <pre>
 * -N &lt;minimum number of instances&gt;
 *  The minimum number of instances required at a node for splitting to be considered. If value is -1, 5 will be used for regression problems and 2 for classification problems, as specified in Geurts et al. (default -1).
 * </pre>
 * 
 * <pre>
 * -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)
 * </pre>
 * 
 * <pre>
 * -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision$
 */
public class ExtraTree extends RandomizableClassifier implements Serializable,
  OptionHandler, TechnicalInformationHandler, WeightedInstancesHandler,
  PartitionGenerator {

  // We want this to make the classifier uniquely identifiable
  static final long serialVersionUID = 7354290459723928536L;

  // The actual trees
  protected Tree m_tree = null;

  // The minimum number of instances per leaf
  protected int m_n_min = -1;

  // The number of attributes to consider
  protected int m_K = -1;

  /**
   * Returns a string describing classifier
   * 
   * @return a description suitable for displaying in the explorer/experimenter
   *         gui
   */
  public String globalInfo() {

    return "Class for generating a single Extra-Tree. Use with the RandomCommittee meta "
      + "classifier to generate an Extra-Trees forest for classification or regression. This "
      + "classifier requires all predictors to be numeric. Missing values are not "
      + "allowed. Instance weights are taken into account. For more information, see\n\n"
      + getTechnicalInformation().toString();
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

    result = new TechnicalInformation(Type.ARTICLE);
    result.setValue(Field.AUTHOR,
      "Pierre Geurts and Damien Ernst and Louis Wehenkel");
    result.setValue(Field.TITLE, "Extremely randomized trees");
    result.setValue(Field.JOURNAL, "Machine Learning");
    result.setValue(Field.YEAR, "2006");
    result.setValue(Field.VOLUME, "63");
    result.setValue(Field.PAGES, "3-42");
    result.setValue(Field.NUMBER, "1");

    return result;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String kTipText() {
    return "Number of attributes to randomly choose at a node. If values is -1, "
      + "(m - 1) will be used for regression problems, and Math.rint(sqrt(m - 1)) "
      + "for classification problems, where m is the number of predictors, as "
      + "specified in Geurts et al.";
  }

  /**
   * Get the value of K.
   * 
   * @return Value of K.
   */
  public int getK() {

    return m_K;
  }

  /**
   * Set the value of K.
   * 
   * @param k value to assign to K.
   */
  public void setK(int k) {

    m_K = k;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String nminTipText() {
    return "The minimum number of instances required at a node for splitting "
      + "to be considered. If value is -1, 5 will be used for regression problems "
      + "and 2 for classification problems, as specified in Geurts et al.";
  }

  /**
   * Get the value of n_min.
   * 
   * @return Value of n_min.
   */
  public int getNmin() {

    return m_n_min;
  }

  /**
   * Set the value of n_min.
   * 
   * @param n value to assign to n_min.
   */
  public void setNmin(int n) {

    m_n_min = n;
  }

  /**
   * Lists the command-line options for this classifier.
   * 
   * @return an enumeration over all possible options
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>();

    newVector.addElement(new Option("\t" + kTipText() + " (default -1).", "K",
      1, "-K <number of attributes>"));

    newVector.addElement(new Option("\t" + nminTipText() + " (default -1).",
      "N", 1, "-N <minimum number of instances>"));

    newVector.addAll(Collections.list(super.listOptions()));

    return newVector.elements();
  }

  /**
   * Gets options from this classifier.
   * 
   * @return the options for the current setup
   */
  @Override
  public String[] getOptions() {

    Vector<String> result;

    result = new Vector<String>();

    result.add("-K");
    result.add("" + getK());

    result.add("-N");
    result.add("" + getNmin());

    Collections.addAll(result, super.getOptions());

    return result.toArray(new String[result.size()]);
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   * <!-- options-start --> Valid options are:
   * <p/>
   * 
   * <pre>
   * -K &lt;number of attributes&gt;
   *  Number of attributes to randomly choose at a node. If values is -1, (m - 1) will be used for regression problems, and Math.rint(sqrt(m - 1)) for classification problems, where m is the number of predictors, as specified in Geurts et al. (default -1).
   * </pre>
   * 
   * <pre>
   * -N &lt;minimum number of instances&gt;
   *  The minimum number of instances required at a node for splitting to be considered. If value is -1, 5 will be used for regression problems and 2 for classification problems, as specified in Geurts et al. (default -1).
   * </pre>
   * 
   * <pre>
   * -S &lt;num&gt;
   *  Random number seed.
   *  (default 1)
   * </pre>
   * 
   * <pre>
   * -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    String tmpStr;

    tmpStr = Utils.getOption('K', options);
    if (tmpStr.length() != 0) {
      m_K = Integer.parseInt(tmpStr);
    } else {
      m_K = -1;
    }

    tmpStr = Utils.getOption('N', options);
    if (tmpStr.length() != 0) {
      m_n_min = Integer.parseInt(tmpStr);
    } else {
      m_n_min = -1;
    }

    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Inner class for maintaining the tree.
   */
  protected class Tree implements Serializable {

    /** ID added to avoid warning */
    private static final long serialVersionUID = 2396257956703850154L;

    // The prediction
    protected double[] m_dist;

    // The split attribute
    protected int m_attIndex;

    // The split point
    protected double m_splitPoint;

    // The successors
    protected Tree[] m_successors;

    /**
     * Constructs a tree from data.
     */
    protected Tree(Instances data, Random rand) {

      // Should we split?
      ArrayList<Integer> al = eligibleAttributes(data);
      if (al == null) {

        // Make leaf node
        if (data.classAttribute().isNumeric()) {
          m_dist = new double[1];
          m_dist[0] = data.meanOrMode(data.classIndex());
        } else {
          m_dist = new double[data.numClasses()];
          for (int i = 0; i < data.numInstances(); i++) {
            m_dist[(int) data.instance(i).classValue()] += data.instance(i)
              .weight();
          }
          Utils.normalize(m_dist);
        }
      } else {

        // Set specific value for K
        int actualK = m_K;
        if (m_K == -1) {
          if (data.classAttribute().isNumeric()) {
            actualK = data.numAttributes() - 1;
          } else {
            actualK = (int) Math.rint(Math.sqrt(data.numAttributes() - 1));
          }
        }

        // Consider K possible attributes
        int k = 0;
        double bestQuality = -Double.MAX_VALUE;
        while ((k < actualK) && (al.size() > 0)) {
          k++;

          // Choose attribute index
          int randIndex = rand.nextInt(al.size());
          int attIndex = al.get(randIndex);
          al.remove(randIndex);

          // Choose split point
          double min = Double.MAX_VALUE;
          double max = -Double.MAX_VALUE;
          for (int i = 0; i < data.numInstances(); i++) {
            double val = data.instance(i).value(attIndex);
            if (val < min) {
              min = val;
            }
            if (val > max) {
              max = val;
            }
          }
          double splitPoint = (rand.nextDouble() * (max - min)) + min;

          // Compute quality
          double splitQuality = splitQuality(data, attIndex, splitPoint);

          // Update best quality
          if (splitQuality > bestQuality) {
            bestQuality = splitQuality;
            m_attIndex = attIndex;
            m_splitPoint = splitPoint;
          }
        }

        // Split data and recurse
        m_successors = new Tree[2];
        for (int i = 0; i < 2; i++) {
          Instances tempData = new Instances(data, data.numInstances());
          for (int j = 0; j < data.numInstances(); j++) {
            if ((i == 0) && (data.instance(j).value(m_attIndex) <= m_splitPoint)) {
              tempData.add(data.instance(j));
            }
            if ((i == 1)
              && (data.instance(j).value(m_attIndex) > m_splitPoint)) {
              tempData.add(data.instance(j));
            }
          }
          tempData.compactify();
          m_successors[i] = new Tree(tempData, rand);
        }
      }
    }

    /**
     * Compute quality of split.
     */
    protected double splitQuality(Instances data, int attIndex,
      double splitPoint) {

      // Compute required basic statistics
      double[][] dist = new double[2][data.numClasses()];
      double numLeft = 0;
      double mean = 0;
      double sumOfWeights = 0;
      for (int i = 0; i < data.numInstances(); i++) {
        Instance inst = data.instance(i);
        double weight = inst.weight();
        if (data.classAttribute().isNominal()) {
          if ((inst.value(attIndex) <= splitPoint)) {
            dist[0][(int) inst.classValue()] += weight;
          } else {
            dist[1][(int) inst.classValue()] += weight;
          }
        } else {
          sumOfWeights += weight;
          mean += weight * inst.classValue();
          if ((inst.value(attIndex) <= splitPoint)) {
            dist[0][0] += weight * inst.classValue();
            numLeft += weight;
          } else {
            dist[1][0] += weight * inst.classValue();
          }
        }
      }

      // Compute actual splitting criteria
      if (data.classAttribute().isNominal()) {
        double[][] table = new double[2][0];
        table[0] = dist[0];
        table[1] = dist[1];
        return ContingencyTables.symmetricalUncertainty(table);
      } else {
        double[] var = new double[2];
        double priorVar = 0;
        if (mean > 0) {
          mean /= sumOfWeights;
        }
        if (numLeft > 0) {
          dist[0][0] /= numLeft;
        }
        if (sumOfWeights - numLeft > 0) {
          dist[1][0] /= (sumOfWeights - numLeft);
        }
        for (int i = 0; i < data.numInstances(); i++) {
          Instance inst = data.instance(i);
          double weight = inst.weight();
          if ((inst.value(attIndex) <= splitPoint)) {
            double diff = (inst.classValue() - dist[0][0]);
            var[0] += weight * diff * diff;
          } else {
            double diff = (inst.classValue() - dist[1][0]);
            var[1] += weight * diff * diff;
          }
          double diffGlobal = (inst.classValue() - mean);
          priorVar += weight * diffGlobal * diffGlobal;
        }
        if (priorVar > 0) {
          return (priorVar - (var[0] + var[1])) / priorVar;
        } else {
          return 0;
        }
      }
    }

    /**
     * Returns leaf node info.
     */
    protected double[] distributionForInstance(Instance inst) {

      // Are we at a leaf?
      if (m_successors == null) {
        return m_dist;
      } else {
        double val = inst.value(m_attIndex);
        if (val <= m_splitPoint) {
          return m_successors[0].distributionForInstance(inst);
        } else {
          return m_successors[1].distributionForInstance(inst);
        }
      }
    }

    /**
     * Returns list of attributes eligible for splitting
     */
    protected ArrayList<Integer> eligibleAttributes(Instances data) {

      ArrayList<Integer> al = null;

      // Set specific value for Nmin
      int actual_min = m_n_min;
      if (m_n_min == -1) {
        if (data.classAttribute().isNumeric()) {
          actual_min = 5;
        } else {
          actual_min = 2;
        }
      }

      // Check for minimum number of instances (actually, sum of weights)
      if (data.sumOfWeights() >= actual_min) {

        // Check for constant class attribute
        double val = data.instance(0).classValue();
        boolean allTheSame = true;
        for (int i = 1; i < data.numInstances(); i++) {
          if (val != data.instance(i).classValue()) {
            allTheSame = false;
            break;
          }
        }
        if (!allTheSame) {

          // Check which attributes are eligible
          for (int j = 0; j < data.numAttributes(); j++) {
            if (j != data.classIndex()) {
              val = data.instance(0).value(j);
              for (int i = 1; i < data.numInstances(); i++) {
                if (val != data.instance(i).value(j)) {
                  if (al == null) {
                    al = new ArrayList<Integer>();
                  }
                  al.add(j);
                  break;
                }
              }
            }
          }
        }
      }
      return al;
    }
  }

  /**
   * Returns default capabilities of the classifier.
   * 
   * @return the capabilities of this classifier
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.NUMERIC_CLASS);

    return result;
  }

  /**
   * Builds one tree.
   */
  @Override
  public void buildClassifier(Instances data) throws Exception {

    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    m_tree = new Tree(data, data.getRandomNumberGenerator(m_Seed));
  }

  /**
   * Returns the distribution.
   */
  @Override
  public double[] distributionForInstance(Instance inst) {

    return m_tree.distributionForInstance(inst);
  }

  /**
   * Returns classifier description.
   */
  @Override
  public String toString() {

    if (m_tree == null) {
      return "No tree has been built yet.";
    } else {
      try {
        return "Extra-Tree with K = " + getK() + " and Nmin = " + getNmin()
          + " (" + numElements() + " nodes in tree)";
      } catch (Exception e) {
        return "Could not compute number of nodes in tree.";
      }
    }
  }

  /**
   * Builds the classifier to generate a partition.
   */
  @Override
  public void generatePartition(Instances data) throws Exception {

    buildClassifier(data);
  }

  /**
   * Computes array that indicates node membership. Array locations are
   * allocated based on breadth-first exploration of the tree.
   */
  @Override
  public double[] getMembershipValues(Instance instance) throws Exception {

    // Set up array for membership values
    double[] a = new double[numElements()];

    // Initialize queues
    Queue<Double> queueOfWeights = new LinkedList<Double>();
    Queue<Tree> queueOfNodes = new LinkedList<Tree>();
    queueOfWeights.add(instance.weight());
    queueOfNodes.add(m_tree);
    int index = 0;

    // While the queue is not empty
    while (!queueOfNodes.isEmpty()) {

      a[index++] = queueOfWeights.poll();
      Tree node = queueOfNodes.poll();

      // Is node a leaf?
      if (node.m_successors == null) {
        continue;
      }

      // Compute weight distribution
      double[] weights = new double[node.m_successors.length];
      if (instance.value(node.m_attIndex) <= node.m_splitPoint) {
        weights[0] = 1.0;
      } else {
        weights[1] = 1.0;
      }
      for (int i = 0; i < node.m_successors.length; i++) {
        queueOfNodes.add(node.m_successors[i]);
        queueOfWeights.add(a[index - 1] * weights[i]);
      }
    }
    return a;
  }

  /**
   * Returns the number of elements in the partition.
   */
  @Override
  public int numElements() throws Exception {

    int numNodes = 0;
    Queue<Tree> queueOfNodes = new LinkedList<Tree>();
    queueOfNodes.add(m_tree);
    while (!queueOfNodes.isEmpty()) {
      Tree node = queueOfNodes.poll();
      numNodes++;
      if (node.m_successors != null) {
        for (Tree m_successor : node.m_successors) {
          queueOfNodes.add(m_successor);
        }
      }
    }
    return numNodes;
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  /**
   * Main method for this class.
   */
  public static void main(String[] args) {

    runClassifier(new ExtraTree(), args);
  }
}
