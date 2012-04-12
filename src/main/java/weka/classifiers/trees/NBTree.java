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
 *    Copyright (C) 2004 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.trees;

import weka.classifiers.Classifier;
import weka.classifiers.trees.j48.NBTreeClassifierTree;
import weka.classifiers.trees.j48.NBTreeModelSelection;
import weka.core.AdditionalMeasureProducer;
import weka.core.Capabilities;
import weka.core.Drawable;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.RevisionUtils;
import weka.core.Summarizable;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.WeightedInstancesHandler;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;

import java.util.Enumeration;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * Class for generating a decision tree with naive Bayes classifiers at the leaves.<br/>
 * <br/>
 * For more information, see<br/>
 * <br/>
 * Ron Kohavi: Scaling Up the Accuracy of Naive-Bayes Classifiers: A Decision-Tree Hybrid. In: Second International Conference on Knoledge Discovery and Data Mining, 202-207, 1996.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{Kohavi1996,
 *    author = {Ron Kohavi},
 *    booktitle = {Second International Conference on Knoledge Discovery and Data Mining},
 *    pages = {202-207},
 *    title = {Scaling Up the Accuracy of Naive-Bayes Classifiers: A Decision-Tree Hybrid},
 *    year = {1996}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 <!-- options-end -->
 *
 * @author Mark Hall
 * @version $Revision: 1.10 $
 */
public class NBTree 
  extends Classifier 
  implements WeightedInstancesHandler, Drawable, Summarizable,
	     AdditionalMeasureProducer, TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = -4716005707058256086L;

  /** Minimum number of instances */
  private int m_minNumObj = 30;

  /** The root of the tree */
  private NBTreeClassifierTree m_root;
  
  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Class for generating a decision tree with naive Bayes classifiers at "
      + "the leaves.\n\n"
      + "For more information, see\n\n"
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
    result.setValue(Field.AUTHOR, "Ron Kohavi");
    result.setValue(Field.TITLE, "Scaling Up the Accuracy of Naive-Bayes Classifiers: A Decision-Tree Hybrid");
    result.setValue(Field.BOOKTITLE, "Second International Conference on Knoledge Discovery and Data Mining");
    result.setValue(Field.YEAR, "1996");
    result.setValue(Field.PAGES, "202-207");
    
    return result;
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    return new NBTreeClassifierTree(null).getCapabilities();
  }

  /**
   * Generates the classifier.
   *
   * @param instances the data to train with
   * @throws Exception if classifier can't be built successfully
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
   * @param instance the instance to classify
   * @return the classification
   * @throws Exception if instance can't be classified successfully
   */
  public double classifyInstance(Instance instance) throws Exception {

    return m_root.classifyInstance(instance);
  }

  /** 
   * Returns class probabilities for an instance.
   *
   * @param instance the instance to get the distribution for
   * @return the class probabilities
   * @throws Exception if distribution can't be computed successfully
   */
  public final double[] distributionForInstance(Instance instance) 
       throws Exception {

    return m_root.distributionForInstance(instance, false);
  }

  /**
   * Returns a description of the classifier.
   * 
   * @return a string representation of the classifier
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
   * @return the graph describing the tree
   * @throws Exception if graph can't be computed
   */
  public String graph() throws Exception {

    return m_root.graph();
  }

  /**
   * Returns a superconcise version of the model
   * 
   * @return a description of the model
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
   * @param additionalMeasureName the name of the measure to query for its value
   * @return the value of the named measure
   * @throws IllegalArgumentException if the named measure is not supported
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
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 1.10 $");
  }

  /**
   * Main method for testing this class
   *
   * @param argv the commandline options
   */
  public static void main(String[] argv){
    runClassifier(new NBTree(), argv);
  }
}
