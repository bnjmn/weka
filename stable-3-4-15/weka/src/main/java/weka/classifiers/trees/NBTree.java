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
 *    NBTree.java
 *    Copyright (C) 2004 Mark Hall
 *
 */

package weka.classifiers.trees;

import weka.classifiers.trees.j48.*;
import java.util.*;
import weka.core.*;
import weka.classifiers.*;

/**
 * Class for generating a Naive Bayes tree (decision tree with
 * Naive Bayes classifiers at the leaves). <p>
 *
 * For more information, see<p>
 *
 * Ron Kohavi (1996). Scaling up the accuracy of naive-Bayes classifiers:
 * a decision tree hybrid. <i>Proceedings of the Second International
 * Conference on Knowledge Discovery and Data Mining</i>.</p>
 * @version $Revision: 1.3.2.1 $
 */
public class NBTree extends Classifier 
  implements WeightedInstancesHandler, Drawable, Summarizable,
	     AdditionalMeasureProducer {

  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Class for generating a decision tree with naive Bayes classifiers at "
      +"the leaves. For more information, see\n\nRon Kohavi (1996). Scaling up "
      +"the accuracy of naive-Bayes classifiers: a decision tree hybrid. Procedings "
      +"of the Second Internaltional Conference on Knoledge Discovery and Data Mining.";
  }

  /** Minimum number of instances */
  private int m_minNumObj = 30;

  /** The root of the tree */
  private NBTreeClassifierTree m_root;

  /**
   * Generates the classifier.
   *
   * @exception Exception if classifier can't be built successfully
   */
  public void buildClassifier(Instances instances) throws Exception {
    
    NBTreeModelSelection modSelection = 
      new NBTreeModelSelection(m_minNumObj, instances);

    m_root = new NBTreeClassifierTree(modSelection);
    m_root.buildClassifier(instances);
  }

  /**
   * Classifies an instance.
   *
   * @exception Exception if instance can't be classified successfully
   */
  public double classifyInstance(Instance instance) throws Exception {

    return m_root.classifyInstance(instance);
  }

  /** 
   * Returns class probabilities for an instance.
   *
   * @exception Exception if distribution can't be computed successfully
   */
  public final double [] distributionForInstance(Instance instance) 
       throws Exception {

    return m_root.distributionForInstance(instance, false);
  }

  /**
   * Returns a description of the classifier.
   */
  public String toString() {

    if (m_root == null) {
      return "No classifier built";
    }
    return "NBTree\n------------------\n" + m_root.toString();
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
   * @exception Exception if graph can't be computed
   */
  public String graph() throws Exception {

    return m_root.graph();
  }

  /**
   * Returns a superconcise version of the model
   */
  public String toSummaryString() {

    return "Number of leaves: " + m_root.numLeaves() + "\n"
         + "Size of the tree: " + m_root.numNodes() + "\n";
  }

  /**
   * Returns the size of the tree
   * @return the size of the tree
   */
  public double measureTreeSize() {
    return m_root.numNodes();
  }

  /**
   * Returns the number of leaves
   * @return the number of leaves
   */
  public double measureNumLeaves() {
    return m_root.numLeaves();
  }

  /**
   * Returns the number of rules (same as number of leaves)
   * @return the number of rules
   */
  public double measureNumRules() {
    return m_root.numLeaves();
  }

  /**
   * Returns the value of the named measure
   * @param measureName the name of the measure to query for its value
   * @return the value of the named measure
   * @exception IllegalArgumentException if the named measure is not supported
   */
  public double getMeasure(String additionalMeasureName) {
    if (additionalMeasureName.compareToIgnoreCase("measureNumRules") == 0) {
      return measureNumRules();
    } else if (additionalMeasureName.compareToIgnoreCase("measureTreeSize") == 0) {
      return measureTreeSize();
    } else if (additionalMeasureName.compareToIgnoreCase("measureNumLeaves") == 0) {
      return measureNumLeaves();
    } else {
      throw new IllegalArgumentException(additionalMeasureName 
			  + " not supported (j48)");
    }
  }
  
  /**
   * Returns an enumeration of the additional measure names
   * @return an enumeration of the measure names
   */
  public Enumeration enumerateMeasures() {
    Vector newVector = new Vector(3);
    newVector.addElement("measureTreeSize");
    newVector.addElement("measureNumLeaves");
    newVector.addElement("measureNumRules");
    return newVector.elements();
  }

  /**
   * Main method for testing this class
   *
   * @param String options 
   */
  public static void main(String [] argv){

    try {
      System.out.println(Evaluation.evaluateModel(new NBTree(), argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
}
