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
 *    ND.java
 *    Copyright (C) 2003-2005 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.meta.nestedDichotomies;

import weka.classifiers.Classifier;
import weka.classifiers.RandomizableSingleClassifierEnhancer;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.rules.ZeroR;
import weka.core.Capabilities;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.MakeIndicator;
import weka.filters.unsupervised.instance.RemoveWithValues;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Random;

/**
 <!-- globalinfo-start -->
 * A meta classifier for handling multi-class datasets with 2-class classifiers by building a random tree structure.<br/>
 * <br/>
 * For more info, check<br/>
 * <br/>
 * Lin Dong, Eibe Frank, Stefan Kramer: Ensembles of Balanced Nested Dichotomies for Multi-class Problems. In: PKDD, 84-95, 2005.<br/>
 * <br/>
 * Eibe Frank, Stefan Kramer: Ensembles of nested dichotomies for multi-class problems. In: Twenty-first International Conference on Machine Learning, 2004.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{Dong2005,
 *    author = {Lin Dong and Eibe Frank and Stefan Kramer},
 *    booktitle = {PKDD},
 *    pages = {84-95},
 *    publisher = {Springer},
 *    title = {Ensembles of Balanced Nested Dichotomies for Multi-class Problems},
 *    year = {2005}
 * }
 * 
 * &#64;inproceedings{Frank2004,
 *    author = {Eibe Frank and Stefan Kramer},
 *    booktitle = {Twenty-first International Conference on Machine Learning},
 *    publisher = {ACM},
 *    title = {Ensembles of nested dichotomies for multi-class problems},
 *    year = {2004}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)</pre>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -W
 *  Full name of base classifier.
 *  (default: weka.classifiers.trees.J48)</pre>
 * 
 * <pre> 
 * Options specific to classifier weka.classifiers.trees.J48:
 * </pre>
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
 * <pre> -R
 *  Use reduced error pruning.</pre>
 * 
 * <pre> -N &lt;number of folds&gt;
 *  Set number of folds for reduced error
 *  pruning. One fold is used as pruning set.
 *  (default 3)</pre>
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
 *  Laplace smoothing for predicted probabilities.</pre>
 * 
 * <pre> -Q &lt;seed&gt;
 *  Seed for random data shuffling (default 1).</pre>
 * 
 <!-- options-end -->
 *
 * @author Eibe Frank
 * @author Lin Dong
 */
public class ND 
  extends RandomizableSingleClassifierEnhancer
  implements TechnicalInformationHandler {
  
  /** for serialization */
  static final long serialVersionUID = -6355893369855683820L;

  /**
   * a node class
   */
  protected class NDTree
    implements Serializable, RevisionHandler {

    /** for serialization */
    private static final long serialVersionUID = 4284655952754474880L;
    
    /** The indices associated with this node */
    protected FastVector m_indices = null;
    
    /** The parent */
    protected NDTree m_parent = null;
    
    /** The left successor */
    protected NDTree m_left = null;
    
    /** The right successor */
    protected NDTree m_right = null;
    
    /**
     * Constructor.
     */
    protected NDTree() {
      
      m_indices = new FastVector(1);
      m_indices.addElement(new Integer(Integer.MAX_VALUE));
    }
    
    /**
     * Locates the node with the given index (depth-first traversal).
     */
    protected NDTree locateNode(int nodeIndex, int[] currentIndex) {
      
      if (nodeIndex == currentIndex[0]) {
	return this;
      } else if (m_left == null) {
	return null;
      } else {
	currentIndex[0]++;
	NDTree leftresult = m_left.locateNode(nodeIndex, currentIndex);
	if (leftresult != null) {
	  return leftresult;
	} else {
	  currentIndex[0]++;
	  return m_right.locateNode(nodeIndex, currentIndex);
	}
      }
    }
      
    /**
     * Inserts a class index into the tree. 
     * 
     * @param classIndex the class index to insert
     */
    protected void insertClassIndex(int classIndex) {

      // Create new nodes
      NDTree right = new NDTree();
      if (m_left != null) {
	m_right.m_parent = right;
	m_left.m_parent = right;
	right.m_right = m_right;
	right.m_left = m_left;
      }
      m_right = right;
      m_right.m_indices = (FastVector)m_indices.copy();
      m_right.m_parent = this;
      m_left = new NDTree();
      m_left.insertClassIndexAtNode(classIndex);
      m_left.m_parent = this; 

      // Propagate class Index
      propagateClassIndex(classIndex);
    }

    /**
     * Propagates class index to the root.
     * 
     * @param classIndex the index to propagate to the root
     */
    protected void propagateClassIndex(int classIndex) {

      insertClassIndexAtNode(classIndex);
      if (m_parent != null) {
	m_parent.propagateClassIndex(classIndex);
      }
    }
    
    /**
     * Inserts the class index at a given node.
     * 
     * @param classIndex the classIndex to insert
     */
    protected void insertClassIndexAtNode(int classIndex) {

      int i = 0;
      while (classIndex > ((Integer)m_indices.elementAt(i)).intValue()) {
	i++;
      }
      m_indices.insertElementAt(new Integer(classIndex), i);
    }

    /**
     * Gets the indices in an array of ints.
     * 
     * @return the indices
     */
    protected int[] getIndices() {

      int[] ints = new int[m_indices.size() - 1];
      for (int i = 0; i < m_indices.size() - 1; i++) {
	ints[i] = ((Integer)m_indices.elementAt(i)).intValue();
      }
      return ints;
    }

    /**
     * Checks whether an index is in the array.
     * 
     * @param index the index to check
     * @return true of the index is in the array
     */
    protected boolean contains(int index) {

      for (int i = 0; i < m_indices.size() - 1; i++) {
	if (index == ((Integer)m_indices.elementAt(i)).intValue()) {
	  return true;
	}
      }
      return false;
    }

    /**
     * Returns the list of indices as a string.
     * 
     * @return the indices as string
     */
    protected String getString() {

      StringBuffer string = new StringBuffer();
      for (int i = 0; i < m_indices.size() - 1; i++) {
	if (i > 0) {
	  string.append(',');
	}
	string.append(((Integer)m_indices.elementAt(i)).intValue() + 1);
      }
      return string.toString();
    }

    /**
     * Unifies tree for improve hashing.
     */
    protected void unifyTree() {

      if (m_left != null) {
        if (((Integer)m_left.m_indices.elementAt(0)).intValue() >
            ((Integer)m_right.m_indices.elementAt(0)).intValue()) {
          NDTree temp = m_left;
          m_left = m_right;
          m_right = temp;
        }
        m_left.unifyTree();
        m_right.unifyTree();
      }
    }

    /**
     * Returns a description of the tree rooted at this node.
     * 
     * @param text the buffer to add the node to
     * @param id the node id
     * @param level the level of the tree
     */
    protected void toString(StringBuffer text, int[] id, int level) {

      for (int i = 0; i < level; i++) {
	text.append("   | ");
      }
      text.append(id[0] + ": " + getString() + "\n");
      if (m_left != null) {
	id[0]++;
	m_left.toString(text, id, level + 1);
	id[0]++;
	m_right.toString(text, id, level + 1);
      }
    }
    
    /**
     * Returns the revision string.
     * 
     * @return		the revision
     */
    public String getRevision() {
      return RevisionUtils.extract("$Revision: 1.8.2.1 $");
    }
  }

  /** The tree of classes */
  protected NDTree m_ndtree = null;
  
  /** The hashtable containing all the classifiers */
  protected Hashtable m_classifiers = null;

  /** Is Hashtable given from END? */
  protected boolean m_hashtablegiven = false;
    
  /**
   * Constructor.
   */
  public ND() {
    
    m_Classifier = new weka.classifiers.trees.J48();
  }
  
  /**
   * String describing default classifier.
   * 
   * @return the default classifier classname
   */
  protected String defaultClassifierString() {
    
    return "weka.classifiers.trees.J48";
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
    TechnicalInformation 	additional;
    
    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "Lin Dong and Eibe Frank and Stefan Kramer");
    result.setValue(Field.TITLE, "Ensembles of Balanced Nested Dichotomies for Multi-class Problems");
    result.setValue(Field.BOOKTITLE, "PKDD");
    result.setValue(Field.YEAR, "2005");
    result.setValue(Field.PAGES, "84-95");
    result.setValue(Field.PUBLISHER, "Springer");

    additional = result.add(Type.INPROCEEDINGS);
    additional.setValue(Field.AUTHOR, "Eibe Frank and Stefan Kramer");
    additional.setValue(Field.TITLE, "Ensembles of nested dichotomies for multi-class problems");
    additional.setValue(Field.BOOKTITLE, "Twenty-first International Conference on Machine Learning");
    additional.setValue(Field.YEAR, "2004");
    additional.setValue(Field.PUBLISHER, "ACM");
    
    return result;
  }

  /**
   * Set hashtable from END.
   * 
   * @param table the hashtable to use
   */
  public void setHashtable(Hashtable table) {

    m_hashtablegiven = true;
    m_classifiers = table;
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // class
    result.disableAllClasses();
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    // instances
    result.setMinimumNumberInstances(1);
    
    return result;
  }

  /**
   * Builds the classifier.
   * 
   * @param data the data to train the classifier with
   * @throws Exception if anything goes wrong
   */
  public void buildClassifier(Instances data) throws Exception {

    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    // remove instances with missing class
    data = new Instances(data);
    data.deleteWithMissingClass();
    
    Random random = data.getRandomNumberGenerator(m_Seed);

    if (!m_hashtablegiven) {
      m_classifiers = new Hashtable();
    }

    // Generate random class hierarchy
    int[] indices = new int[data.numClasses()];
    for (int i = 0; i < indices.length; i++) {
      indices[i] = i;
    }

    // Randomize list of class indices
    for (int i = indices.length - 1; i > 0; i--) {
      int help = indices[i];
      int index = random.nextInt(i + 1);
      indices[i] = indices[index];
      indices[index] = help;
    }

    // Insert random class index at randomly chosen node
    m_ndtree = new NDTree();
    m_ndtree.insertClassIndexAtNode(indices[0]);
    for (int i = 1; i < indices.length; i++) {
      int nodeIndex = random.nextInt(2 * i - 1);
     
      NDTree node = m_ndtree.locateNode(nodeIndex, new int[1]);
      node.insertClassIndex(indices[i]);
    }
    m_ndtree.unifyTree();
    

    // Build classifiers
    buildClassifierForNode(m_ndtree, data);
  }

  /**
   * Builds the classifier for one node.
   * 
   * @param node the node to build the classifier for
   * @param data the data to work with
   * @throws Exception if anything goes wrong
   */
  public void buildClassifierForNode(NDTree node, Instances data) throws Exception {

    // Are we at a leaf node ?
    if (node.m_left != null) {
      
      // Create classifier
      MakeIndicator filter = new MakeIndicator();
      filter.setAttributeIndex("" + (data.classIndex() + 1));
      filter.setValueIndices(node.m_right.getString());
      filter.setNumeric(false);
      filter.setInputFormat(data);
      FilteredClassifier classifier = new FilteredClassifier();
      if (data.numInstances() > 0) {
	classifier.setClassifier(Classifier.makeCopies(m_Classifier, 1)[0]);
      } else {
	classifier.setClassifier(new ZeroR());
      }
      classifier.setFilter(filter);
      
      if (!m_classifiers.containsKey(node.m_left.getString() + "|" + node.m_right.getString())) {
	classifier.buildClassifier(data);
	m_classifiers.put(node.m_left.getString() + "|" + node.m_right.getString(), classifier);
      } else {
	classifier=(FilteredClassifier)m_classifiers.get(node.m_left.getString() + "|" + 
							 node.m_right.getString());
      }
      
      // Generate successors
      if (node.m_left.m_left != null) {
        RemoveWithValues rwv = new RemoveWithValues();
        rwv.setInvertSelection(true);
        rwv.setNominalIndices(node.m_left.getString());
        rwv.setAttributeIndex("" + (data.classIndex() + 1));
        rwv.setInputFormat(data);
        Instances firstSubset = Filter.useFilter(data, rwv);
        buildClassifierForNode(node.m_left, firstSubset);
      }
      if (node.m_right.m_left != null) {
        RemoveWithValues rwv = new RemoveWithValues();
        rwv.setInvertSelection(true);
        rwv.setNominalIndices(node.m_right.getString());
        rwv.setAttributeIndex("" + (data.classIndex() + 1));
        rwv.setInputFormat(data);
        Instances secondSubset = Filter.useFilter(data, rwv);
        buildClassifierForNode(node.m_right, secondSubset);
      }
    }
  }
    
  /**
   * Predicts the class distribution for a given instance
   *
   * @param inst the (multi-class) instance to be classified
   * @return the class distribution
   * @throws Exception if computing fails
   */
  public double[] distributionForInstance(Instance inst) throws Exception {
	
    return distributionForInstance(inst, m_ndtree);
  }

  /**
   * Predicts the class distribution for a given instance
   *
   * @param inst the (multi-class) instance to be classified
   * @param node the node to do get the distribution for
   * @return the class distribution
   * @throws Exception if computing fails
   */
  protected double[] distributionForInstance(Instance inst, NDTree node) throws Exception {

    double[] newDist = new double[inst.numClasses()];
    if (node.m_left == null) {
      newDist[node.getIndices()[0]] = 1.0;
      return newDist;
    } else {
      Classifier classifier = (Classifier)m_classifiers.get(node.m_left.getString() + "|" +
							    node.m_right.getString());
      double[] leftDist = distributionForInstance(inst, node.m_left);
      double[] rightDist = distributionForInstance(inst, node.m_right);
      double[] dist = classifier.distributionForInstance(inst);

      for (int i = 0; i < inst.numClasses(); i++) {
	if (node.m_right.contains(i)) {
	  newDist[i] = dist[1] * rightDist[i];
	} else {
	  newDist[i] = dist[0] * leftDist[i];
	}
      }
      return newDist;
    }
  }

  /**
   * Outputs the classifier as a string.
   * 
   * @return a string representation of the classifier
   */
  public String toString() {
	
    if (m_classifiers == null) {
      return "ND: No model built yet.";
    }
    StringBuffer text = new StringBuffer();
    text.append("ND\n\n");
    m_ndtree.toString(text, new int[1], 0);
	
    return text.toString();
  }
	
  /**
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
	    
    return 
        "A meta classifier for handling multi-class datasets with 2-class "
      + "classifiers by building a random tree structure.\n\n"
      + "For more info, check\n\n"
      + getTechnicalInformation().toString();
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 1.8.2.1 $");
  }
    
  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {
    runClassifier(new ND(), argv);
  }
}
