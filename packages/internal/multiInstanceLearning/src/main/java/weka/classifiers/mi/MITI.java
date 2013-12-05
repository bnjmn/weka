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
 *    MITI.java
 *    Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.classifiers.mi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

import weka.classifiers.RandomizableClassifier;
import weka.classifiers.mi.miti.AlgorithmConfiguration;
import weka.classifiers.mi.miti.Bag;
import weka.classifiers.mi.miti.NextSplitHeuristic;
import weka.classifiers.mi.miti.TreeNode;
import weka.core.AdditionalMeasureProducer;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.MultiInstanceCapabilitiesHandler;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;

/**
 * <!-- globalinfo-start --> MITI (Multi Instance Tree Inducer): multi-instance
 * classification based a decision tree learned using Blockeel et al.'s
 * algorithm. For more information, see<br/>
 * <br/>
 * Hendrik Blockeel, David Page, Ashwin Srinivasan: Multi-instance Tree
 * Learning. In: Proceedings of the International Conference on Machine
 * Learning, 57-64, 2005.<br/>
 * <br/>
 * Luke Bjerring, Eibe Frank: Beyond Trees: Adopting MITI to Learn Rules and
 * Ensemble Classifiers for Multi-instance Data. In: Proceedings of the
 * Australasian Joint Conference on Artificial Intelligence, 2011.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- technical-bibtex-start --> BibTeX:
 * 
 * <pre>
 * &#64;inproceedings{Blockeel2005,
 *    author = {Hendrik Blockeel and David Page and Ashwin Srinivasan},
 *    booktitle = {Proceedings of the International Conference on Machine Learning},
 *    pages = {57-64},
 *    publisher = {ACM},
 *    title = {Multi-instance Tree Learning},
 *    year = {2005}
 * }
 * 
 * &#64;inproceedings{Bjerring2011,
 *    author = {Luke Bjerring and Eibe Frank},
 *    booktitle = {Proceedings of the Australasian Joint Conference on Artificial Intelligence},
 *    publisher = {Springer},
 *    title = {Beyond Trees: Adopting MITI to Learn Rules and Ensemble Classifiers for Multi-instance Data},
 *    year = {2011}
 * }
 * </pre>
 * <p/>
 * <!-- technical-bibtex-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -M [1|2|3]
 *  The method used to determine best split:
 *  1. Gini; 2. MaxBEPP; 3. SSBEPP
 * </pre>
 * 
 * <pre>
 * -K [kBEPPConstant]
 *  The constant used in the tozero() hueristic
 * </pre>
 * 
 * <pre>
 * -L
 *  Scales the value of K to the size of the bags
 * </pre>
 * 
 * <pre>
 * -U
 *  Use unbiased estimate rather than BEPP, i.e. UEPP.
 * </pre>
 * 
 * <pre>
 * -B
 *  Uses the instances present for the bag counts at each node when splitting,
 *  weighted according to 1 - Ba ^ n, where n is the number of instances
 *  present which belong to the bag, and Ba is another parameter (default 0.5)
 * </pre>
 * 
 * <pre>
 * -Ba [multiplier]
 *  Multiplier for count influence of a bag based on the number of its instances
 * </pre>
 * 
 * <pre>
 * -A [number of attributes]
 *  The number of randomly selected attributes to split
 *  -1: All attributes
 *  -2: square root of the total number of attributes
 * </pre>
 * 
 * <pre>
 * -An [number of splits]
 *  The number of top scoring attribute splits to randomly pick from
 *  -1: All splits (completely random selection)
 *  -2: square root of the number of splits
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
 * @author Luke Bjerring
 * @author Eibe Frank
 */
public class MITI extends RandomizableClassifier implements OptionHandler,
  AdditionalMeasureProducer, TechnicalInformationHandler,
  MultiInstanceCapabilitiesHandler {

  /** for serialization */
  static final long serialVersionUID = -217735168397644244L;

  // Reference to the actual tree
  protected MultiInstanceDecisionTree tree;

  // Used to select the split selection measure.
  public static final int SPLITMETHOD_GINI = 1;
  public static final int SPLITMETHOD_MAXBEPP = 2;
  public static final int SPLITMETHOD_SSBEPP = 3;

  public static final Tag[] TAGS_SPLITMETHOD = {
    new Tag(SPLITMETHOD_GINI, "Gini: E * (1 - E)"),
    new Tag(SPLITMETHOD_MAXBEPP, "MaxBEPP: E"),
    new Tag(SPLITMETHOD_SSBEPP, "Sum Squared BEPP: E * E") };

  // The chosen splitting method.
  protected int m_SplitMethod = SPLITMETHOD_MAXBEPP;

  // Wether to scale based on the number of instances
  protected boolean m_scaleK = false;

  // Whether to use bag-based statistics for subset scoring
  protected boolean m_useBagCount = false;

  // Whether to use BEPP or EPP
  protected boolean m_unbiasedEstimate = false;

  // The constant used in BEPP
  protected int m_kBEPPConstant = 5;

  // The number of random attributes to consider for splitting
  protected int m_AttributesToSplit = -1;

  // The number of top-N attributes to choose from randomly
  protected int m_AttributeSplitChoices = 1;

  // Determines the influence of the number of instances in a bag that are
  // present in a subset when applying bag-based statistics.
  protected double m_bagInstanceMultiplier = 0.5;

  /**
   * Returns a string describing classifier
   * 
   * @return a description suitable for displaying in the explorer/experimenter
   *         gui
   */
  public String globalInfo() {

    return "MITI (Multi Instance Tree Inducer): multi-instance classification "
      + " based a decision tree learned using Blockeel et al.'s algorithm. For more "
      + "information, see\n\n" + getTechnicalInformation().toString();
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
    TechnicalInformation additional;

    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR,
      "Hendrik Blockeel and David Page and Ashwin Srinivasan");
    result.setValue(Field.TITLE, "Multi-instance Tree Learning");
    result.setValue(Field.BOOKTITLE,
      "Proceedings of the International Conference on Machine Learning");
    result.setValue(Field.YEAR, "2005");
    result.setValue(Field.PAGES, "57-64");
    result.setValue(Field.PUBLISHER, "ACM");

    additional = result.add(Type.INPROCEEDINGS);
    additional.setValue(Field.AUTHOR, "Luke Bjerring and Eibe Frank");
    additional
      .setValue(
        Field.TITLE,
        "Beyond Trees: Adopting MITI to Learn Rules and Ensemble Classifiers for Multi-instance Data");
    additional
      .setValue(Field.BOOKTITLE,
        "Proceedings of the Australasian Joint Conference on Artificial Intelligence");
    additional.setValue(Field.YEAR, "2011");
    additional.setValue(Field.PUBLISHER, "Springer");

    return result;
  }

  /**
   * Returns the capabilities of this classifier.
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.RELATIONAL_ATTRIBUTES);
    result.disable(Capability.MISSING_VALUES);

    // class
    result.disableAllClasses();
    result.disableAllClassDependencies();
    result.enable(Capability.BINARY_CLASS);

    // Only multi instance data
    result.enable(Capability.ONLY_MULTIINSTANCE);

    return result;
  }

  /**
   * Returns the capabilities of this multi-instance classifier for the
   * relational data.
   * 
   * @return the capabilities of this object
   * @see Capabilities
   */
  @Override
  public Capabilities getMultiInstanceCapabilities() {
    Capabilities result = super.getCapabilities();

    // class
    result.disableAllClasses();
    result.enable(Capability.NO_CLASS);

    return result;
  }

  /**
   * Learns the classifier from the training data.
   */
  @Override
  public void buildClassifier(Instances trainingData) throws Exception {
    // can classifier handle the data?
    getCapabilities().testWithFail(trainingData);

    tree = new MultiInstanceDecisionTree(trainingData);
  }

  /**
   * Returns an enumeration of the additional measure names.
   * 
   * @return an enumeration of the measure names
   */
  @Override
  public Enumeration<String> enumerateMeasures() {

    Vector<String> newVector = new Vector<String>(3);
    newVector.addElement("measureNumRules");
    newVector.addElement("measureNumPositiveRules");
    newVector.addElement("measureNumConditionsInPositiveRules");
    return newVector.elements();
  }

  /**
   * Returns the value of the named measure.
   * 
   * @param additionalMeasureName the name of the measure to query for its value
   * @return the value of the named measure
   * @throws IllegalArgumentException if the named measure is not supported
   */
  @Override
  public double getMeasure(String additionalMeasureName) {

    if (additionalMeasureName.equalsIgnoreCase("measureNumRules")) {
      return tree.getNumLeaves();
    }
    if (additionalMeasureName.equalsIgnoreCase("measureNumPositiveRules")) {
      return tree.numPosRulesAndNumPosConditions()[0];
    }
    if (additionalMeasureName
      .equalsIgnoreCase("measureNumConditionsInPositiveRules")) {
      return tree.numPosRulesAndNumPosConditions()[1];
    } else {
      throw new IllegalArgumentException(additionalMeasureName
        + " not supported (MultiInstanceRuleLearner)");
    }
  }

  /**
   * Returns the "class distribution" for the given bag.
   */
  @Override
  public double[] distributionForInstance(Instance newBag) throws Exception {

    double[] distribution = new double[2];
    Instances contents = newBag.relationalValue(1);
    boolean positive = false;
    for (Instance i : contents) {
      if (tree.isPositive(i)) {
        positive = true;
        break;
      }
    }

    distribution[1] = positive ? 1 : 0;
    distribution[0] = 1 - distribution[1];

    return distribution;
  }

  /**
   * Class for learning and representing the tree.
   */
  protected class MultiInstanceDecisionTree implements Serializable {

    /** ID added to avoid warning */
    private static final long serialVersionUID = 4037700809781784985L;

    // The root of the tree.
    private TreeNode root;

    // A hash map that tell us to which bag a particular instance belongs
    private final HashMap<Instance, Bag> m_instanceBags;

    // The number of leaves in the tree
    private int numLeaves = 0;

    // Returns the number of leaves in the tree
    public int getNumLeaves() {
      return numLeaves;
    }

    /**
     * Constructs the tree from the given set of instances.
     */
    protected MultiInstanceDecisionTree(Instances instances) {

      m_instanceBags = new HashMap<Instance, Bag>();
      ArrayList<Instance> all = new ArrayList<Instance>();
      double totalInstances = 0;
      double totalBags = 0;
      for (Instance i : instances) {
        Bag bag = new Bag(i);
        for (Instance bagged : bag.instances()) {
          m_instanceBags.put(bagged, bag);
          all.add(bagged);
        }
        totalBags++;
        totalInstances += bag.instances().numInstances();
      }

      double b_multiplier = totalInstances / totalBags;
      if (m_scaleK) {
        for (Bag bag : m_instanceBags.values()) {
          bag.setBagWeightMultiplier(b_multiplier);
        }
      }

      makeTree(m_instanceBags, all, false);
    }

    /**
     * Constructs tree based on given arguments.
     */
    public MultiInstanceDecisionTree(HashMap<Instance, Bag> instanceBags,
      ArrayList<Instance> all, boolean stopOnFirstPositiveLeaf) {

      m_instanceBags = instanceBags;
      makeTree(instanceBags, all, stopOnFirstPositiveLeaf);
    }

    /**
     * Method that actually makes the tree.
     */
    private void makeTree(HashMap<Instance, Bag> instanceBags,
      ArrayList<Instance> all, boolean stopOnFirstPositiveLeaf) {

      Random r = new Random(getSeed());

      AlgorithmConfiguration settings = getSettings();

      ArrayList<TreeNode> toSplit = new ArrayList<TreeNode>();

      root = new TreeNode(null, all);
      toSplit.add(root);
      numLeaves = 0;

      while (toSplit.size() > 0) {

        // The next two lines are here solely to reproduce the results from the
        // paper
        // (i.e. so that the same random number sequence is used.
        int nextIndex = Math.min(1, toSplit.size());
        nextIndex = r.nextInt(nextIndex);

        TreeNode next = toSplit.remove(nextIndex);
        if (next == null) {
          continue;
        }

        if (next.isPurePositive(instanceBags)) {
          next.makeLeafNode(true);
          ArrayList<String> deactivated = new ArrayList<String>();
          next.deactivateRelatedInstances(instanceBags, deactivated);

          if (m_Debug && deactivated.size() > 0) {
            Bag.printDeactivatedInstances(deactivated);
          }

          // Need to re-calculate scores if positive leaf has been
          // created
          for (TreeNode n : toSplit) {
            n.removeDeactivatedInstances(instanceBags);
            n.calculateNodeScore(instanceBags, m_unbiasedEstimate,
              m_kBEPPConstant, m_useBagCount, m_bagInstanceMultiplier);
          }

          if (stopOnFirstPositiveLeaf && deactivated.size() > 0) {
            return;
          }

        } else if (next.isPureNegative(instanceBags)) {
          next.makeLeafNode(false);
        } else {
          next.splitInstances(instanceBags, settings, r, m_Debug);
          if (!next.isLeafNode()) {
            if (next.split.isNominal) {
              TreeNode[] nominals = next.nominals();
              for (TreeNode nominal : nominals) {
                nominal.calculateNodeScore(instanceBags, m_unbiasedEstimate,
                  m_kBEPPConstant, m_useBagCount, m_bagInstanceMultiplier);
                toSplit.add(nominal);
              }
            } else {
              next.left().calculateNodeScore(instanceBags, m_unbiasedEstimate,
                m_kBEPPConstant, m_useBagCount, m_bagInstanceMultiplier);
              toSplit.add(next.left());
              next.right().calculateNodeScore(instanceBags, m_unbiasedEstimate,
                m_kBEPPConstant, m_useBagCount, m_bagInstanceMultiplier);
              toSplit.add(next.right());
            }
          } else {
            // Need to re-calculate scores if positive leaf has been
            // created
            if (next.isPositiveLeaf()) {
              for (TreeNode n : toSplit) {
                n.removeDeactivatedInstances(instanceBags);
                n.calculateNodeScore(instanceBags, m_unbiasedEstimate,
                  m_kBEPPConstant, m_useBagCount, m_bagInstanceMultiplier);
              }

              if (stopOnFirstPositiveLeaf) {
                return;
              }
            }
          }
        }

        // Increment number of leaves if necessary
        if (next.isLeafNode()) {
          numLeaves++;
        }

        // Re-evaluate the best next node, because we've most likely
        // added new nodes or disabled bags
        Comparator<TreeNode> sh = Collections
          .reverseOrder(new NextSplitHeuristic());
        Collections.sort(toSplit, sh);
      }

      if (m_Debug) {
        System.out.println(root.render(1, instanceBags));
      }
    }

    /**
     * Is instance positive given tree?
     */
    protected boolean isPositive(Instance i) {
      TreeNode leaf = traverseTree(i);
      return leaf != null && leaf.isPositiveLeaf();
    }

    /**
     * Traverse to a leaf for the given instance.
     */
    private TreeNode traverseTree(Instance i) {
      TreeNode next = root;
      while (next != null && !next.isLeafNode()) {
        Attribute a = next.split.attribute;
        if (a.isNominal()) {
          next = next.nominals()[(int) i.value(a)];
        } else {
          if (i.value(a) < next.split.splitPoint) {
            next = next.left();
          } else {
            next = next.right();
          }
        }
      }
      return next;
    }

    /**
     * Render the tree as a string.
     */
    public String render() {
      return root.render(0, m_instanceBags);
    }

    /**
     * Trim negative branches for MIRI's parial trees.
     */
    public boolean trimNegativeBranches() {
      return root.trimNegativeBranches();
    }

    /**
     * Determines the number of positive rules and the number of conditions used
     * in the positive rules, for the given subtree.
     */
    public int[] numPosRulesAndNumPosConditions() {

      return numPosRulesAndNumPosConditions(root);
    }

    /**
     * Determines the number of positive rules and the number of conditions used
     * in the positive rules, for the given subtree.
     */
    private int[] numPosRulesAndNumPosConditions(TreeNode next) {

      int[] numPosRulesAndNumPosConditions = new int[2];
      if ((next != null) && next.isLeafNode()) {

        // Do we have a positive leaf node? Then there's one positive rule.
        if (next.isPositiveLeaf()) {
          numPosRulesAndNumPosConditions[0] = 1;
        }
      } else if (next != null) {

        // We must be at an internal node
        Attribute a = next.split.attribute;
        int[] fromBelow = null;
        if (a.isNominal()) {
          for (TreeNode child : next.nominals()) {
            fromBelow = numPosRulesAndNumPosConditions(child);

            // Need to keep track of the number of positive rules
            numPosRulesAndNumPosConditions[0] += fromBelow[0];

            // One test is added for each positive rule
            numPosRulesAndNumPosConditions[1] += fromBelow[1] + fromBelow[0];
          }
        } else {
          fromBelow = numPosRulesAndNumPosConditions(next.left());

          // Need to keep track of the number of positive rules
          numPosRulesAndNumPosConditions[0] += fromBelow[0];

          // One test is added for each positive rule
          numPosRulesAndNumPosConditions[1] += fromBelow[1] + fromBelow[0];

          fromBelow = numPosRulesAndNumPosConditions(next.right());

          // Need to keep track of the number of positive rules
          numPosRulesAndNumPosConditions[0] += fromBelow[0];

          // One test is added for each positive rule
          numPosRulesAndNumPosConditions[1] += fromBelow[1] + fromBelow[0];
        }
      }
      return numPosRulesAndNumPosConditions;
    }
  }

  /**
   * Gets the user-specified settings as a configuration object.
   */
  protected AlgorithmConfiguration getSettings() {
    return new AlgorithmConfiguration(m_SplitMethod, m_unbiasedEstimate,
      m_kBEPPConstant, m_useBagCount, m_bagInstanceMultiplier,
      m_AttributesToSplit, m_AttributeSplitChoices);
  }

  /**
   * Lists the options for this classifier. Valid options are: <!--
   * options-start --> Valid options are:
   * <p/>
   * 
   * <pre>
   * -M [1|2|3]
   *  The method used to determine best split:
   *  1. Gini; 2. MaxBEPP; 3. SSBEPP
   * </pre>
   * 
   * <pre>
   * -K [kBEPPConstant]
   *  The constant used in the tozero() hueristic
   * </pre>
   * 
   * <pre>
   * -L
   *  Scales the value of K to the size of the bags
   * </pre>
   * 
   * <pre>
   * -U
   *  Use unbiased estimate rather than BEPP, i.e. UEPP.
   * </pre>
   * 
   * <pre>
   * -B
   *  Uses the instances present for the bag counts at each node when splitting,
   *  weighted according to 1 - Ba ^ n, where n is the number of instances
   *  present which belong to the bag, and Ba is another parameter (default 0.5)
   * </pre>
   * 
   * <pre>
   * -Ba [multiplier]
   *  Multiplier for count influence of a bag based on the number of its instances
   * </pre>
   * 
   * <pre>
   * -A [number of attributes]
   *  The number of randomly selected attributes to split
   *  -1: All attributes
   *  -2: square root of the total number of attributes
   * </pre>
   * 
   * <pre>
   * -An [number of splits]
   *  The number of top scoring attribute splits to randomly pick from
   *  -1: All splits (completely random selection)
   *  -2: square root of the number of splits
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
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> result = new Vector<Option>();

    result.addElement(new Option("\tThe method used to determine best split:\n"
      + "\t1. Gini; 2. MaxBEPP; 3. SSBEPP", "M", 1, "-M [1|2|3]"));

    result.addElement(new Option(
      "\tThe constant used in the tozero() hueristic", "K", 1,
      "-K [kBEPPConstant]"));

    result.addElement(new Option(
      "\tScales the value of K to the size of the bags", "L", 0, "-L"));

    result.addElement(new Option(
      "\tUse unbiased estimate rather than BEPP, i.e. UEPP.", "U", 0, "-U"));

    result
      .addElement(new Option(
        "\tUses the instances present for the bag counts at each node when splitting,\n"
          + "\tweighted according to 1 - Ba ^ n, where n is the number of instances\n"
          + "\tpresent which belong to the bag, and Ba is another parameter (default 0.5)",
        "B", 0, "-B"));

    result
      .addElement(new Option(
        "\tMultiplier for count influence of a bag based on the number of its instances",
        "Ba", 1, "-Ba [multiplier]"));

    result
      .addElement(new Option(
        "\tThe number of randomly selected attributes to split\n\t-1: All attributes\n\t-2: square root of the total number of attributes",
        "A", 1, "-A [number of attributes]"));

    result
      .addElement(new Option(
        "\tThe number of top scoring attribute splits to randomly pick from\n\t-1: All splits (completely random selection)\n\t-2: square root of the number of splits",
        "An", 1, "-An [number of splits]"));

    result.addAll(Collections.list(super.listOptions()));

    return result.elements();
  }

  /**
   * Determines the settings of the Classifier.
   * 
   * @return an array of strings specifying settings.
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    String methodString = Utils.getOption('M', options);
    if (methodString.length() != 0) {
      setSplitMethod(new SelectedTag(Integer.parseInt(methodString),
        TAGS_SPLITMETHOD));
    } else {
      setSplitMethod(new SelectedTag(SPLITMETHOD_MAXBEPP, TAGS_SPLITMETHOD));
    }

    methodString = Utils.getOption('K', options);
    if (methodString.length() != 0) {
      setK(Integer.parseInt(methodString));
    } else {
      setK(5);
    }

    setL(Utils.getFlag('L', options));

    setUnbiasedEstimate(Utils.getFlag('U', options));

    methodString = Utils.getOption('A', options);
    if (methodString.length() != 0) {
      setAttributesToSplit(Integer.parseInt(methodString));
    } else {
      setAttributesToSplit(-1);
    }

    methodString = Utils.getOption("An", options);
    if (methodString.length() != 0) {
      setTopNAttributesToSplit(Integer.parseInt(methodString));
    } else {
      setTopNAttributesToSplit(1);
    }

    setB(Utils.getFlag('B', options));

    methodString = Utils.getOption("Ba", options);
    if (methodString.length() != 0) {
      setBa(Double.parseDouble(methodString));
    } else {
      setBa(0.5);
    }

    super.setOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> result = new Vector<String>();

    result.add("-K");
    result.add("" + m_kBEPPConstant);

    if (getL()) {
      result.add("-L");
    }

    if (getUnbiasedEstimate()) {
      result.add("-U");
    }

    if (getB()) {
      result.add("-B");
    }

    result.add("-Ba");
    result.add("" + m_bagInstanceMultiplier);

    result.add("-M");
    result.add("" + m_SplitMethod);

    result.add("-A");
    result.add("" + m_AttributesToSplit);

    result.add("-An");
    result.add("" + m_AttributeSplitChoices);

    Collections.addAll(result, super.getOptions());

    return result.toArray(new String[result.size()]);
  }

  /**
   * Help for K parameter.
   */
  public String kTipText() {
    return "The value used in the tozero() method.";
  }

  /**
   * Getter for K.
   */
  public int getK() {
    return m_kBEPPConstant;
  }

  /**
   * Setter for K.
   */
  public void setK(int newValue) {
    m_kBEPPConstant = newValue;
  }

  /**
   * Help for scale parameter.
   */
  public String lTipText() {
    return "Whether to scale based on the number of instances.";
  }

  /**
   * Getter for L.
   */
  public boolean getL() {
    return m_scaleK;
  }

  /**
   * Setter for L.
   */
  public void setL(boolean newValue) {
    m_scaleK = newValue;
  }

  /**
   * Help for unbiased estimate flag.
   */
  public String unbiasedEstimateTipText() {
    return "Whether to used unbiased estimate (EPP instead of BEPP).";
  }

  /**
   * Getter for unbiased estimate flag.
   */
  public boolean getUnbiasedEstimate() {
    return m_unbiasedEstimate;
  }

  /**
   * Setter for unbiased estimate flag.
   */
  public void setUnbiasedEstimate(boolean newValue) {
    m_unbiasedEstimate = newValue;
  }

  /**
   * Help for bag-based stats flag.
   */
  public String bTipText() {
    return "Whether to use bag-based statistics for estimates of proportion.";
  }

  /**
   * Getter for B.
   */
  public boolean getB() {
    return m_useBagCount;
  }

  /**
   * Setter for B.
   */
  public void setB(boolean newValue) {
    m_useBagCount = newValue;
  }

  /**
   * Help for bag-based stats parameter.
   */
  public String baTipText() {
    return "Multiplier for count influence of a bag based on the number of its instances.";
  }

  /**
   * Getter for Ba.
   */
  public double getBa() {
    return m_bagInstanceMultiplier;
  }

  /**
   * Setter for Ba.
   */
  public void setBa(double newValue) {
    m_bagInstanceMultiplier = newValue;
  }

  /**
   * Help for attributes to split
   */
  public String attributesToSplitTipText() {
    return "The number of randomly chosen attributes to consider for splitting.";
  }

  /**
   * Getter method.
   */
  public int getAttributesToSplit() {
    return m_AttributesToSplit;
  }

  /**
   * Setter method.
   */
  public void setAttributesToSplit(int newValue) {
    m_AttributesToSplit = newValue;
  }

  /**
   * Help for top-N attributes to split
   */
  public String topNAttributesToSplitTipText() {
    return "Value of N to use for top-N attributes to choose randomly from.";
  }

  /**
   * Getter method.
   */
  public int getTopNAttributesToSplit() {
    return m_AttributeSplitChoices;
  }

  /**
   * Setter method.
   */
  public void setTopNAttributesToSplit(int newValue) {
    m_AttributeSplitChoices = newValue;
  }

  /**
   * Help for split measure selection.
   */
  public String splitMethodTipText() {
    return "The method used to determine best split: 1. Gini; 2. MaxBEPP; 3. SSBEPP";
  }

  /**
   * Setter method.
   */
  public void setSplitMethod(SelectedTag newMethod) {
    if (newMethod.getTags() == TAGS_SPLITMETHOD) {
      m_SplitMethod = newMethod.getSelectedTag().getID();
    }
  }

  /**
   * Getter method.
   */
  public SelectedTag getSplitMethod() {
    return new SelectedTag(m_SplitMethod, TAGS_SPLITMETHOD);
  }

  /**
   * Outputs tree as a string.
   */
  @Override
  public String toString() {
    if (tree != null) {

      String s = tree.render();

      s += "\n\nNumber of positive rules: "
        + getMeasure("measureNumPositiveRules") + "\n";
      s += "Number of conditions in positive rules: "
        + getMeasure("measureNumConditionsInPositiveRules") + "\n";

      return s;
    } else {
      return "No model built yet!";
    }
  }

  /**
   * Used to run the classifier from the command-line.
   */
  public static void main(String[] options) {
    runClassifier(new MITI(), options);
  }
}
