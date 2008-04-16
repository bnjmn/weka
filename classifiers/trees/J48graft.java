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
 *    J48graft.java
 *    Copyright (C) 2007 Geoff Webb & Janice Boughton
 *    (adapted from code written by Eibe Frank).
 */

package weka.classifiers.trees;

import weka.classifiers.Classifier;
import weka.classifiers.Sourcable;
import weka.classifiers.trees.j48.BinC45ModelSelection;
import weka.classifiers.trees.j48.C45ModelSelection;
import weka.classifiers.trees.j48.C45PruneableClassifierTreeG;
import weka.classifiers.trees.j48.ClassifierTree;
import weka.classifiers.trees.j48.ModelSelection;
import weka.core.AdditionalMeasureProducer;
import weka.core.Capabilities;
import weka.core.Drawable;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Matchable;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.Summarizable;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;

import java.util.Enumeration;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * Class for generating a grafted (pruned or unpruned) C4.5 decision tree. For more information, see:<br/>
 * <br/>
 * Geoff Webb (1999). Decision Tree Grafting From the All-Tests-But-One Partition. Morgan Kaufmann, San Francisco, CA.
 * <br/>
 *  also:<br/>
 * Webb, G. I. (1996). Further Experimental Evidence Against The Utility Of Occams Razor. Journal of Artificial Intelligence Research 4. Menlo Park, CA: AAAI Press, pages 397-417.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;INPROCEEDINGS{Webb99,
 *   year = {1999},
 *   title = {Decision Tree Grafting From The All Tests But One Partition},
 *   booktitle = {Proceedings of the Sixteenth International Joint Conference on Artificial Intelligence (IJCAI 99)},
 *   publisher = {Morgan Kaufmann},
 *   editor = {T. Dean},
 *   address = {San Francisco},
 *   author = {G. I. Webb},
 *   location = {Stockholm, Sweden},
 *   pages = {702-707},
 * }
 * &#64:article{Webb96b,
 *   year = {1996},
 *   title = {Further Experimental Evidence Against The Utility Of Occams Razor},
 *   journal = {Journal of Artificial Intelligence Research},
 *   volume = {4},
 *   pages = {397-417},
 *   publisher = {AAAI Press},
 *   address = {Menlo Park, CA},
 *   author = {G. I. Webb}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 *
 * <pre> -U
 *  Use unpruned tree.</pre>
 *
 * <pre> -C &lt;pruning confidence&gt;
 *  Set confidence threshold for pruning.
 *  (default 0.25)</pre>
 *
 * <pre> -M &lt;minimum number of instances&gt;
 *  Set minimum number of instances per leaf.
 *  (default 2)</pre>
 *
 * <pre> -B
 *  Use binary splits only.</pre>
 *
 * <pre> -S
 *  Don't perform subtree raising.</pre>
 *
 * <pre> -L
 *  Do not clean up after the tree has been built.</pre>
 *
 * <pre> -A
 *  Laplace smoothing for predicted probabilities.
 *  (note: this option only affects initial tree; grafting process always uses laplace).</pre>
 *
 * <pre> -E
 * Option to allow relabeling during grafting.</pre>
 *
 <!-- options-end -->
 *
 * @author Janice Boughton (jrbought@csse.monash.edu.au)
 *  (based on J48.java written by Eibe Frank)
 * @version $Revision: 1.2 $
 */
public class J48graft 
  extends Classifier
  implements OptionHandler, Drawable, Matchable, Sourcable, 
             WeightedInstancesHandler, Summarizable,
             AdditionalMeasureProducer, TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = 8823716098042427799L;

  /** The decision tree */
  private ClassifierTree m_root;

  /** Unpruned tree? */
  private boolean m_unpruned = false;

  /** Confidence level */
  private float m_CF = 0.25f;

  /** Minimum number of instances */
  private int m_minNumObj = 2;

  /** Determines whether probabilities are smoothed using
      Laplace correction when predictions are generated */
  private boolean m_useLaplace = false;

  /** Number of folds for reduced error pruning. */
  private int m_numFolds = 3;

  /** Binary splits on nominal attributes? */
  private boolean m_binarySplits = false;

  /** Subtree raising to be performed? */
  private boolean m_subtreeRaising = true;

  /** Cleanup after the tree has been built. */
  private boolean m_noCleanup = false;

  /** relabel instances when grafting */
  private boolean m_relabel = false;

  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return  "Class for generating a grafted (pruned or unpruned) C4.5 "
      + "decision tree. For more information, see\n\n"
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
    TechnicalInformation        result;

    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "Geoff Webb");
    result.setValue(Field.YEAR, "1999");
    result.setValue(Field.TITLE, "Decision Tree Grafting From the All-Tests-But-One Partition");
    result.setValue(Field.PUBLISHER, "Morgan Kaufmann");
    result.setValue(Field.ADDRESS, "San Francisco, CA");

    return result;
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities      result;

    try {
     result = new C45PruneableClassifierTreeG(null, !m_unpruned, m_CF, m_subtreeRaising, m_relabel, !m_noCleanup).getCapabilities();
    }
    catch (Exception e) {
      result = new Capabilities(this);
    }

    result.setOwner(this);

    return result;
  }

  /**
   * Generates the classifier.
   *
   * @param instances the data to train the classifier with
   * @throws Exception if classifier can't be built successfully
   */
  public void buildClassifier(Instances instances)
       throws Exception {

    ModelSelection modSelection;

    if (m_binarySplits)
      modSelection = new BinC45ModelSelection(m_minNumObj, instances);
    else
      modSelection = new C45ModelSelection(m_minNumObj, instances);
      m_root = new C45PruneableClassifierTreeG(modSelection, 
                              !m_unpruned, m_CF, m_subtreeRaising, 
                               m_relabel, !m_noCleanup);
    m_root.buildClassifier(instances);

    if (m_binarySplits) {
      ((BinC45ModelSelection)modSelection).cleanup();
    } else {
      ((C45ModelSelection)modSelection).cleanup();
    }
  }

  /**
   * Classifies an instance.
   *
   * @param instance the instance to classify
   * @return the classification for the instance
   * @throws Exception if instance can't be classified successfully
   */
  public double classifyInstance(Instance instance) throws Exception {

    return m_root.classifyInstance(instance);
  }

  /** 
   * Returns class probabilities for an instance.
   *
   * @param instance the instance to calculate the class probabilities for
   * @return the class probabilities
   * @throws Exception if distribution can't be computed successfully
   */
  public final double [] distributionForInstance(Instance instance) 
       throws Exception {

    return m_root.distributionForInstance(instance, m_useLaplace);
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
   * Returns tree in prefix order.
   *
   * @return the tree in prefix order
   * @throws Exception if something goes wrong
   */
  public String prefix() throws Exception {
    
    return m_root.prefix();
  }


  /**
   * Returns tree as an if-then statement.
   *
   * @param className the name of the Java class
   * @return the tree as a Java if-then type statement
   * @throws Exception if something goes wrong
   */
  public String toSource(String className) throws Exception {

    StringBuffer [] source = m_root.toSource(className);
    return 
    "class " + className + " {\n\n"
    +"  public static double classify(Object [] i)\n"
    +"    throws Exception {\n\n"
    +"    double p = Double.NaN;\n"
    + source[0]  // Assignment code
    +"    return p;\n"
    +"  }\n"
    + source[1]  // Support code
    +"}\n";
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * Valid options are: <p>
   *
   * -U <br>
   * Use unpruned tree.<p>
   *
   * -C confidence <br>
   * Set confidence threshold for pruning. (Default: 0.25) <p>
   *
   * -M number <br>
   * Set minimum number of instances per leaf. (Default: 2) <p>
   *
   * -B <br>
   * Use binary splits for nominal attributes. <p>
   *
   * -S <br>
   * Don't perform subtree raising. <p>
   *
   * -L <br>
   * Do not clean up after the tree has been built.
   *
   * -A <br>
   * If set, Laplace smoothing is used for predicted probabilites. 
   *  (note: this option only affects initial tree; grafting process always uses laplace). <p>
   *
   * -E <br>
   * Allow relabelling when grafting. <p>
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(9);

    newVector.
       addElement(new Option("\tUse unpruned tree.",
			      "U", 0, "-U"));
    newVector.
       addElement(new Option("\tSet confidence threshold for pruning.\n" +
                             "\t(default 0.25)",
			     "C", 1, "-C <pruning confidence>"));
    newVector.
       addElement(new Option("\tSet minimum number of instances per leaf.\n" +
			      "\t(default 2)",
			      "M", 1, "-M <minimum number of instances>"));
    newVector.
       addElement(new Option("\tUse binary splits only.",
			      "B", 0, "-B"));
    newVector.
       addElement(new Option("\tDon't perform subtree raising.",
			      "S", 0, "-S"));
    newVector.
       addElement(new Option("\tDo not clean up after the tree has been built.",
			      "L", 0, "-L"));
    newVector.
       addElement(new Option("\tLaplace smoothing for predicted probabilities.  (note: this option only affects initial tree; grafting process always uses laplace).",
 
			      "A", 0, "-A"));
    newVector.
       addElement(new Option("\tRelabel when grafting.",
                             "E", 0, "-E"));
    return newVector.elements();
  }

  /**
   * Parses a given list of options.
   *
   <!-- options-start -->
   * Valid options are: <p/>
   *
   * <pre> -U
   *  Use unpruned tree.</pre>
   *
   * <pre> -C &lt;pruning confidence&gt;
   *  Set confidence threshold for pruning.
   *  (default 0.25)</pre>
   *
   * <pre> -M &lt;minimum number of instances&gt;
   *  Set minimum number of instances per leaf.
   *  (default 2)</pre>
   *
   * <pre> -B
   *  Use binary splits only.</pre>
   *
   * <pre> -S
   *  Don't perform subtree raising.</pre>
   *
   * <pre> -L
   *  Do not clean up after the tree has been built.</pre>
   *
   * <pre> -A
   *  Laplace smoothing for predicted probabilities.
   *  (note: this option only affects initial tree; grafting process always uses laplace). </pre>
   *
   * <pre> -E
   * Allow relabelling when performing grafting.</pre>
   *
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
 
    // Other options
    String minNumString = Utils.getOption('M', options);
    if (minNumString.length() != 0) {
      m_minNumObj = Integer.parseInt(minNumString);
    } else {
      m_minNumObj = 2;
    }
    m_binarySplits = Utils.getFlag('B', options);
    m_useLaplace = Utils.getFlag('A', options);

    // Pruning options
    m_unpruned = Utils.getFlag('U', options);
    m_subtreeRaising = !Utils.getFlag('S', options);
    m_noCleanup = Utils.getFlag('L', options);
		if ((m_unpruned) && (!m_subtreeRaising)) {
      throw new Exception("Subtree raising doesn't need to be unset for unpruned tree!");
    }
    m_relabel = Utils.getFlag('E', options);
    String confidenceString = Utils.getOption('C', options);
    if (confidenceString.length() != 0) {
      if (m_unpruned) {
	throw new Exception("Doesn't make sense to change confidence for unpruned "
			    +"tree!");
      } else {
	m_CF = (new Float(confidenceString)).floatValue();
	if ((m_CF <= 0) || (m_CF >= 1)) {
	  throw new Exception("Confidence has to be greater than zero and smaller " +
			      "than one!");
	}
      }
    } else {
      m_CF = 0.25f;
    }
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [10];
    int current = 0;

    if (m_noCleanup) {
      options[current++] = "-L";
    }
    if (m_unpruned) {
      options[current++] = "-U";
    } else {
      if (!m_subtreeRaising) {
	options[current++] = "-S";
      }
      options[current++] = "-C"; options[current++] = "" + m_CF;
    }
    if (m_binarySplits) {
      options[current++] = "-B";
    }
    options[current++] = "-M"; options[current++] = "" + m_minNumObj;
    if (m_useLaplace) {
      options[current++] = "-A";
    }

    if(m_relabel) {
       options[current++] = "-E";
    }

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String useLaplaceTipText() {
    return "Whether counts at leaves are smoothed based on Laplace.";
  }

  /**
   * Get the value of useLaplace.
   *
   * @return Value of useLaplace.
   */
  public boolean getUseLaplace() {
    
    return m_useLaplace;
  }
  
  /**
   * Set the value of useLaplace.
   *
   * @param newuseLaplace Value to assign to useLaplace.
   */
  public void setUseLaplace(boolean newuseLaplace) {
    
    m_useLaplace = newuseLaplace;
  }
  
  /**
   * Returns a description of the classifier.
   *
   * @return a description of the classifier
   */
  public String toString() {

    if (m_root == null) {
      return "No classifier built";
    }
    if (m_unpruned)
      return "J48graft unpruned tree\n------------------\n" + m_root.toString();
    else
      return "J48graft pruned tree\n------------------\n" + m_root.toString();
  }

  /**
   * Returns a superconcise version of the model
   *
   * @return a summary of the model
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
   * Returns the value of the named measure
   * @param additionalMeasureName the name of the measure to query for its value
   * @return the value of the named measure
   * @throws IllegalArgumentException if the named measure is not supported
   */
  public double getMeasure(String additionalMeasureName) {
    if (additionalMeasureName.compareTo("measureNumRules") == 0) {
      return measureNumRules();
    } else if (additionalMeasureName.compareTo("measureTreeSize") == 0) {
      return measureTreeSize();
    } else if (additionalMeasureName.compareTo("measureNumLeaves") == 0) {
      return measureNumLeaves();
    } else {
      throw new IllegalArgumentException(additionalMeasureName
			  + " not supported (j48)");
    }
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String unprunedTipText() {
    return "Whether pruning is performed.";
  }

  /**
   * Get the value of unpruned.
   *
   * @return Value of unpruned.
   */
  public boolean getUnpruned() {

    return m_unpruned;
  }

  /**
   * Set the value of unpruned.
   * @param v  Value to assign to unpruned.
   */
  public void setUnpruned(boolean v) {

    m_unpruned = v;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String relabelTipText() {
    return "Whether relabelling is allowed during grafting.";
  }

  /**
   * Get the value of relabelling
   *
   * @return Value of relabelling.
   */
  public boolean getRelabel() {

    return m_relabel;
  }

  /**
   * Set the value of relabelling. 
   *
   * @param v  Value to assign to relabelling flag.
   */
  public void setRelabel(boolean v) {
    m_relabel = v;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String confidenceFactorTipText() {
    return "The confidence factor used for pruning (smaller values incur "
      + "more pruning).";
  }

  /**
   * Get the value of CF.
   *
   * @return Value of CF.
   */
  public float getConfidenceFactor() {
    
    return m_CF;
  }
  
  /**
   * Set the value of CF.
   *
   * @param v  Value to assign to CF.
   */
  public void setConfidenceFactor(float v) {
    
    m_CF = v;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String minNumObjTipText() {
    return "The minimum number of instances per leaf.";
  }

  /**
   * Get the value of minNumObj.
   *
   * @return Value of minNumObj.
   */
  public int getMinNumObj() {
    
    return m_minNumObj;
  }
  
  /**
   * Set the value of minNumObj.
   *
   * @param v  Value to assign to minNumObj.
   */
  public void setMinNumObj(int v) {
    
    m_minNumObj = v;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String binarySplitsTipText() {
    return "Whether to use binary splits on nominal attributes when "
      + "building the trees.";
  }
  
  /**
   * Get the value of binarySplits.
   *
   * @return Value of binarySplits.
   */
  public boolean getBinarySplits() {

    return m_binarySplits;
  }

  /**
   * Set the value of binarySplits.
   *
   * @param v  Value to assign to binarySplits.
   */
  public void setBinarySplits(boolean v) {
    
    m_binarySplits = v;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String subtreeRaisingTipText() {
    return "Whether to consider the subtree raising operation when pruning.";
  }
 
  /**
   * Get the value of subtreeRaising.
   *
   * @return Value of subtreeRaising.
   */
  public boolean getSubtreeRaising() {
    
    return m_subtreeRaising;
  }
  
  /**
   * Set the value of subtreeRaising.
   *
   * @param v  Value to assign to subtreeRaising.
   */
  public void setSubtreeRaising(boolean v) {
    
    m_subtreeRaising = v;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String saveInstanceDataTipText() {
    return "Whether to save the training data for visualization.";
  }

  /**
   * Check whether instance data is to be saved.
   *
   * @return true if instance data is saved
   */
  public boolean getSaveInstanceData() {
    
    return m_noCleanup;
  }
  
  /**
   * Set whether instance data is to be saved.
   * @param v true if instance data is to be saved
   */
  public void setSaveInstanceData(boolean v) {

    m_noCleanup = v;
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 1.2 $");
  }
 
  /**
   * Main method for testing this class
   *
   * @param argv the commandline options
   */
  public static void main(String [] argv){
    runClassifier(new J48graft(), argv);
  }
}
