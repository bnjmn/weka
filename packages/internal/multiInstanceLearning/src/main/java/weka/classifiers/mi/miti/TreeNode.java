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
 *    TreeNode.java
 *    Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.classifiers.mi.miti;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import weka.core.Attribute;
import weka.core.Instance;

/**
 * Represents a node in the decision tree.
 * 
 * @author Luke Bjerring
 * @version $Revision$
 */
public class TreeNode implements Serializable {

  /** ID added to avoid warning */
  private static final long serialVersionUID = 9050803921532593168L;

  // The instances associated with this node
  private ArrayList<Instance> instances;

  // The score for this node
  private double nodeScore;

  // Flag to indicate whether node is a leaf.
  private boolean leafNode;

  // Flag to indicate whether node is a positive leaf.
  private boolean positiveLeaf;

  // Reference to parent node
  private TreeNode parent = null;

  // The left child node in the case of a binary split
  private TreeNode left = null;

  // The right child node in the case of a binary split
  private TreeNode right = null;

  // The children in the case of a nominal-attribute split
  private TreeNode[] nominalNodes = null;

  // The actual split used
  public Split split;

  /**
   * Creates node based on given collection of instances and parent node.
   */
  public TreeNode(TreeNode parent, ArrayList<Instance> instances) {
    this.parent = parent;
    this.instances = instances;
  }

  /**
   * Returns the score for this node.
   */
  public double nodeScore() {
    return nodeScore;
  }

  /**
   * Removes deactivated instances from the node. Does NOT update the node
   * score.
   */
  public void removeDeactivatedInstances(HashMap<Instance, Bag> instanceBags) {

    ArrayList<Instance> newInstances = new ArrayList<Instance>();
    for (Instance i : instances) {
      if (instanceBags.get(i).isEnabled()) {
        newInstances.add(i);
      }
    }
    instances = newInstances;
  }

  /**
   * Calculates the node score based on the given arguments.
   */
  public void calculateNodeScore(HashMap<Instance, Bag> instanceBags,
    boolean unbiasedEstimate, int kBEPPConstant, boolean bagCount,
    double multiplier) {
    nodeScore = NextSplitHeuristic.getBepp(instances, instanceBags,
      unbiasedEstimate, kBEPPConstant, bagCount, multiplier);
  }

  /**
   * Is node a leaf?
   */
  public boolean isLeafNode() {
    return leafNode;
  }

  /**
   * Is node a positive leaf?
   */
  public boolean isPositiveLeaf() {
    return positiveLeaf;
  }

  /**
   * Checks whether all the instances at the node are associated with positive
   * bags.
   */
  public boolean isPureNegative(HashMap<Instance, Bag> instanceBags) {
    for (Instance i : instances) {
      Bag bag = instanceBags.get(i);
      if (bag.isEnabled() && bag.isPositive()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks whether all the instances at the node are associated with negative
   * bags.
   */
  public boolean isPurePositive(HashMap<Instance, Bag> instanceBags) {
    for (Instance i : instances) {
      Bag bag = instanceBags.get(i);
      if (bag.isEnabled() && !bag.isPositive()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Turns the node into a leaf node.
   */
  public void makeLeafNode(boolean positiveLeaf) {
    leafNode = true;
    this.positiveLeaf = positiveLeaf;
  }

  /**
   * Returns the parent.
   */
  public TreeNode parent() {
    return parent;
  }

  /**
   * Returns the left child in the case of a binary split.
   */
  public TreeNode left() {
    return left;
  }

  /**
   * Returns the right child in the case of a binary split.
   */
  public TreeNode right() {
    return right;
  }

  /**
   * Returns the children in the case of a nominal-attribute split.
   */
  public TreeNode[] nominals() {
    return nominalNodes;
  }

  /**
   * Deactives all instances associated with bags that have at least one
   * instance in the current node.
   */
  public void deactivateRelatedInstances(HashMap<Instance, Bag> instanceBags,
    List<String> deactivated) {
    for (Instance i : instances) {
      Bag container = instanceBags.get(i);
      container.disableInstances(deactivated);
    }
  }

  /**
   * @param a attribute to check
   * @return true if any parent branch used the same attribute to split on
   */
  private boolean hasSplitOnAttributePreviously(Attribute a) {

    TreeNode n = this;
    while (n != null) {
      if (n.split != null && n.split.attribute.equals(a)) {
        return true;
      }
      n = n.parent;
    }
    return false;
  }

  /**
   * Splits the instances on this node into the best possible child nodes,
   * according to the settings
   */
  public void splitInstances(HashMap<Instance, Bag> instanceBags,
    AlgorithmConfiguration settings, Random rand, boolean debug) {

    // All remaining instances are enabled
    ArrayList<Instance> enabled = instances;

    int totalAttributes = instances.get(0).numAttributes();
    Instance template = instances.get(0);

    // Filter to only use attributes that are not constant
    List<Attribute> attributes = new ArrayList<Attribute>();
    for (int index = 0; index < totalAttributes; index++) {
      double val = template.value(index);
      for (Instance i : instances) {
        if (i.value(index) != val) {
          attributes.add(template.attribute(index));
          break;
        }
      }
    }

    // Choose some random attributes
    int attributesToSplit = settings.attributesToSplit;
    if (settings.attributesToSplit == -1) {
      attributesToSplit = totalAttributes;
    }
    if (settings.attributesToSplit == -2) {
      attributesToSplit = (int) Math.sqrt(totalAttributes) + 1;
    }
    if (attributesToSplit < attributes.size()) {
      // Select a random set of attributes
      Collections.shuffle(attributes, rand);
      attributes = attributes.subList(0, attributesToSplit);
    }

    // Collect a list of the split scores
    ArrayList<Split> best = new ArrayList<Split>();

    for (Attribute a : attributes) {
      if (a.isNominal() && hasSplitOnAttributePreviously(a)) {
        continue;
      }

      Split splitPoint = Split.getBestSplitPoint(a, enabled, instanceBags,
        settings);
      if (splitPoint == null) {
        continue;
      }

      if (debug) {
        System.out.println(a.name() + " scored " + splitPoint.score);
      }

      best.add(splitPoint);
      continue;
    }

    // If we can't find a split point, make this a leaf node
    if (best.size() == 0) {
      makeImpureLeafNode(instanceBags, settings, debug);
      return;
    }

    Collections.sort(best, new Comparator<Split>() {
      @Override
      public int compare(Split o1, Split o2) {
        return Double.compare(o2.score, o1.score);
      }
    });

    // Get a random best split based on the setting
    int attributeSplitChoices = settings.attributeSplitChoices;
    if (settings.attributeSplitChoices == -1) {
      attributeSplitChoices = best.size();
    } else if (settings.attributeSplitChoices == -2) {
      attributeSplitChoices = (int) Math.sqrt(best.size()) + 1;
    }
    int pick = rand.nextInt(Math.min(attributeSplitChoices, best.size()));
    split = best.get(pick);

    if (debug) {
      System.out.println("Selected best is " + split.attribute.name());
    }

    Attribute splittingAttribute = split.attribute;
    if (splittingAttribute.isNominal()) {

      // Create a multi-valued nominal-attribute split
      int numNominalValues = splittingAttribute.numValues();
      nominalNodes = new TreeNode[numNominalValues];
      for (int i = 0; i < numNominalValues; i++) {
        ArrayList<Instance> list = new ArrayList<Instance>();
        for (Instance instance : enabled) {
          if (instance.value(splittingAttribute) == i) {
            list.add(instance);
          }
        }
        nominalNodes[i] = new TreeNode(this, list);
      }
    } else {

      // Create a binary split for a numeric attribute
      ArrayList<Instance> left = new ArrayList<Instance>();
      ArrayList<Instance> right = new ArrayList<Instance>();

      for (Instance instance : enabled) {
        if (instance.value(splittingAttribute) < split.splitPoint) {
          left.add(instance);
        } else {
          right.add(instance);
        }
      }
      this.left = new TreeNode(this, left);
      this.right = new TreeNode(this, right);
      if (debug) {
        System.out.println(left.size() + " went left and " + right.size()
          + " went right");
      }
    }
  }

  /**
   * Code to cover special case where impure leaf node needs to be created
   * because data cannot be split any further.
   */
  private void makeImpureLeafNode(HashMap<Instance, Bag> instanceBags,
    AlgorithmConfiguration settings, boolean debug) {
    SufficientStatistics ss;
    if (!settings.useBagStatistics) {
      ss = new SufficientInstanceStatistics(instances, instanceBags);
    } else {
      ss = new SufficientBagStatistics(instances, instanceBags,
        settings.bagCountMultiplier);
    }
    double bepp = BEPP.GetBEPP(ss.totalCountRight(), ss.positiveCountRight(),
      settings.kBEPPConstant, settings.unbiasedEstimate);
    makeLeafNode(ss.positiveCountRight() / ss.totalCountRight() > 0.5);

    if (debug) {
      System.out.println(bepp > 0.5);
    }

    // Deactivate the related instances if we decided this
    // is a positive instance
    if (!isPositiveLeaf()) {
      return;
    }
    ArrayList<String> deactivated = new ArrayList<String>();
    deactivateRelatedInstances(instanceBags, deactivated);

    // Print out any deactivated bags if we're debugging
    if (deactivated.size() > 0 && debug) {
      Bag.printDeactivatedInstances(deactivated);
    }
  }

  /**
   * Recursively renders this node and its branches as a tabbed out tree
   * 
   * @return a string containing the node and its children, tabbed to the given
   *         depth
   */
  public String render(int depth, HashMap<Instance, Bag> instanceBags) {
    String s = "";

    int pos = 0;
    for (Instance i : instances) {
      Bag bag = instanceBags.get(i);
      if (bag.isPositive()) {
        pos++;
      }
    }
    s += instances.size() + " [" + pos + " / " + (instances.size() - pos) + "]";

    if (isLeafNode()) {
      s += isPositiveLeaf() ? " (+)" : " (-)";
    }

    if (!isLeafNode() && split != null) {
      if (split.attribute.isNominal()) {
        for (int i = 0; i < nominalNodes.length; i++) {
          if (nominalNodes[i] != null) {
            // New line, tab it out.
            s += "\n";
            for (int t = 0; t < depth; t++) {
              s += "|\t";
            }
            s += split.attribute.name() + " = " + split.attribute.value(i)
              + " : ";
            s += nominalNodes[i].render(depth + 1, instanceBags);
          }
        }
      } else {
        if (left != null) {
          // New line, tab it out.
          s += "\n";
          for (int i = 0; i < depth; i++) {
            s += "|\t";
          }
          s += split.attribute.name() + " <= "
            + String.format("%.4g", split.splitPoint) + " : ";
          s += left.render(depth + 1, instanceBags);
        }

        if (right != null) {
          // New line, tab it out.
          s += "\n";
          for (int i = 0; i < depth; i++) {
            s += "|\t";
          }
          s += split.attribute.name() + " > "
            + String.format("%.4g", split.splitPoint) + " : ";
          s += right.render(depth + 1, instanceBags);
        }
      }
    }
    return s;
  }

  /**
   * Recursively removes all branches that do not contain a positive leaf. Used
   * to create partial tree for MIRI rule learner.
   * 
   * @return true if a positive leaf was encountered during the trim
   */
  public boolean trimNegativeBranches() {

    boolean positive = false;
    if (nominalNodes != null) {

      // Consider nominal split
      for (int i = 0; i < nominalNodes.length; i++) {
        TreeNode child = nominalNodes[i];
        if (child.isPositiveLeaf()) {
          positive = true;
        } else if (child.trimNegativeBranches()) {
          positive = true;
        } else {
          nominalNodes[i] = null;
        }
      }
    } else {

      // Consider numeric split
      if (left != null) {
        if (left.isPositiveLeaf()) {
          positive = true;
        } else if (left.trimNegativeBranches()) {
          positive = true;
        } else {
          left = null;
        }
      }

      if (right != null) {
        if (right.isPositiveLeaf()) {
          positive = true;
        } else if (right.trimNegativeBranches()) {
          positive = true;
        } else {
          right = null;
        }
      }
    }
    return positive;
  }
}
