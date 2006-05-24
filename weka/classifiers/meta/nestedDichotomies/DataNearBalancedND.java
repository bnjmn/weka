/*
 *    This program is free software; you can redistribsute it and/or modify
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
 *    DataNearBalancedND.java
 *    Copyright (C) 2005 University of Waikato
 *
 */
package weka.classifiers.meta.nestedDichotomies;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.RandomizableSingleClassifierEnhancer;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Range;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.MakeIndicator;
import weka.filters.unsupervised.instance.RemoveWithValues;

import java.util.Hashtable;
import java.util.Random;


/**
 <!-- globalinfo-start -->
 * A meta classifier for handling multi-class datasets with 2-class classifiers by building a random data-balanced tree structure.<br/>
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
 * @author Lin Dong
 * @author Eibe Frank
 */
public class DataNearBalancedND 
  extends RandomizableSingleClassifierEnhancer
  implements TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = 5117477294209496368L;
  
  /** The filtered classifier in which the base classifier is wrapped. */
  protected FilteredClassifier m_FilteredClassifier;
    
  /** The hashtable for this node. */
  protected Hashtable m_classifiers=new Hashtable();

  /** The first successor */
  protected DataNearBalancedND m_FirstSuccessor = null;

  /** The second successor */
  protected DataNearBalancedND m_SecondSuccessor = null;
  
  /** The classes that are grouped together at the current node */
  protected Range m_Range = null;
    
  /** Is Hashtable given from END? */
  protected boolean m_hashtablegiven = false;
    
  /**
   * Constructor.
   */
  public DataNearBalancedND() {
    
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
   * Generates a classifier for the current node and proceeds recursively.
   *
   * @param data contains the (multi-class) instances
   * @param classes contains the indices of the classes that are present
   * @param rand the random number generator to use
   * @param classifier the classifier to use
   * @param table the Hashtable to use
   * @param instsNumAllClasses
   * @throws Exception if anything goes worng
   */
  private void generateClassifierForNode(Instances data, Range classes,
                                         Random rand, Classifier classifier, Hashtable table,
                                         double[] instsNumAllClasses) 
    throws Exception {
	
    // Get the indices
    int[] indices = classes.getSelection();

    // Randomize the order of the indices
    for (int j = indices.length - 1; j > 0; j--) {
      int randPos = rand.nextInt(j + 1);
      int temp = indices[randPos];
      indices[randPos] = indices[j];
      indices[j] = temp;
    }

    // Pick the classes for the current split
    double total = 0;
    for (int j = 0; j < indices.length; j++) {
      total += instsNumAllClasses[indices[j]];
    }
    double halfOfTotal = total / 2;
	
    // Go through the list of classes until the either the left or
    // right subset exceeds half the total weight
    double sumLeft = 0, sumRight = 0;
    int i = 0, j = indices.length - 1;
    do {
      if (i == j) {
        if (rand.nextBoolean()) {
          sumLeft += instsNumAllClasses[indices[i++]];
        } else {
          sumRight += instsNumAllClasses[indices[j--]];
        }
      } else {
        sumLeft += instsNumAllClasses[indices[i++]];
        sumRight += instsNumAllClasses[indices[j--]];
      }
    } while (Utils.sm(sumLeft, halfOfTotal) && Utils.sm(sumRight, halfOfTotal));

    int first = 0, second = 0;
    if (!Utils.sm(sumLeft, halfOfTotal)) {
      first = i;
    } else {
      first = j + 1;
    }
    second = indices.length - first;

    int[] firstInds = new int[first];
    int[] secondInds = new int[second];
    System.arraycopy(indices, 0, firstInds, 0, first);
    System.arraycopy(indices, first, secondInds, 0, second);
    	
    // Sort the indices (important for hash key)!
    int[] sortedFirst = Utils.sort(firstInds);
    int[] sortedSecond = Utils.sort(secondInds);
    int[] firstCopy = new int[first];
    int[] secondCopy = new int[second];
       for (int k = 0; k < sortedFirst.length; k++) {
      firstCopy[k] = firstInds[sortedFirst[k]];
    }
    firstInds = firstCopy;
    for (int k = 0; k < sortedSecond.length; k++) {
      secondCopy[k] = secondInds[sortedSecond[k]];
    }
    secondInds = secondCopy;
		
    // Unify indices to improve hashing
    if (firstInds[0] > secondInds[0]) {
      int[] help = secondInds;
      secondInds = firstInds;
      firstInds = help;
      int help2 = second;
      second = first;
      first = help2;
    }

    m_Range = new Range(Range.indicesToRangeList(firstInds));
    m_Range.setUpper(data.numClasses() - 1);

    Range secondRange = new Range(Range.indicesToRangeList(secondInds));
    secondRange.setUpper(data.numClasses() - 1);
       
    // Change the class labels and build the classifier
    MakeIndicator filter = new MakeIndicator();
    filter.setAttributeIndex("" + (data.classIndex() + 1));
    filter.setValueIndices(m_Range.getRanges());
    filter.setNumeric(false);
    filter.setInputFormat(data);
    m_FilteredClassifier = new FilteredClassifier();
    if (data.numInstances() > 0) {
      m_FilteredClassifier.setClassifier(Classifier.makeCopies(classifier, 1)[0]);
    } else {
      m_FilteredClassifier.setClassifier(new weka.classifiers.rules.ZeroR());
    }
    m_FilteredClassifier.setFilter(filter);

    // Save reference to hash table at current node
    m_classifiers=table;
	
    if (!m_classifiers.containsKey( getString(firstInds) + "|" + getString(secondInds))) {
      m_FilteredClassifier.buildClassifier(data);
      m_classifiers.put(getString(firstInds) + "|" + getString(secondInds), m_FilteredClassifier);
    } else {
      m_FilteredClassifier=(FilteredClassifier)m_classifiers.get(getString(firstInds) + "|" + 
								 getString(secondInds));	
    }
				
    // Create two successors if necessary
    m_FirstSuccessor = new DataNearBalancedND();
    if (first == 1) {
      m_FirstSuccessor.m_Range = m_Range;
    } else {
      RemoveWithValues rwv = new RemoveWithValues();
      rwv.setInvertSelection(true);
      rwv.setNominalIndices(m_Range.getRanges());
      rwv.setAttributeIndex("" + (data.classIndex() + 1));
      rwv.setInputFormat(data);
      Instances firstSubset = Filter.useFilter(data, rwv);
      m_FirstSuccessor.generateClassifierForNode(firstSubset, m_Range, 
                                                 rand, classifier, m_classifiers,
                                                 instsNumAllClasses);
    }
    m_SecondSuccessor = new DataNearBalancedND();
    if (second == 1) {
      m_SecondSuccessor.m_Range = secondRange;
    } else {
      RemoveWithValues rwv = new RemoveWithValues();
      rwv.setInvertSelection(true);
      rwv.setNominalIndices(secondRange.getRanges());
      rwv.setAttributeIndex("" + (data.classIndex() + 1));
      rwv.setInputFormat(data);
      Instances secondSubset = Filter.useFilter(data, rwv);
      m_SecondSuccessor = new DataNearBalancedND();
      
      m_SecondSuccessor.generateClassifierForNode(secondSubset, secondRange, 
                                                  rand, classifier, m_classifiers,
                                                  instsNumAllClasses);
    }
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
   * Builds tree recursively.
   *
   * @param data contains the (multi-class) instances
   * @throws Exception if the building fails
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
	
    // Check which classes are present in the
    // data and construct initial list of classes
    boolean[] present = new boolean[data.numClasses()];
    for (int i = 0; i < data.numInstances(); i++) {
      present[(int)data.instance(i).classValue()] = true;
    }
    StringBuffer list = new StringBuffer();
    for (int i = 0; i < present.length; i++) {
      if (present[i]) {
        if (list.length() > 0) {
          list.append(",");
        }
        list.append(i + 1);
      }
    }

    // Determine the number of instances in each class
    double[] instsNum = new double[data.numClasses()];
    for (int i = 0; i < data.numInstances(); i++) {
      instsNum[(int)data.instance(i).classValue()] += data.instance(i).weight();
    }
      
    Range newRange = new Range(list.toString());
    newRange.setUpper(data.numClasses() - 1);
	
    generateClassifierForNode(data, newRange, random, m_Classifier, m_classifiers, instsNum);
  }
    
  /**
   * Predicts the class distribution for a given instance
   *
   * @param inst the (multi-class) instance to be classified
   * @return the class distribution
   * @throws Exception if computing fails
   */
  public double[] distributionForInstance(Instance inst) throws Exception {
	
    double[] newDist = new double[inst.numClasses()];
    if (m_FirstSuccessor == null) {
      for (int i = 0; i < inst.numClasses(); i++) {
        if (m_Range.isInRange(i)) {
          newDist[i] = 1;
        }
      }
      return newDist;
    } else {
      double[] firstDist = m_FirstSuccessor.distributionForInstance(inst);
      double[] secondDist = m_SecondSuccessor.distributionForInstance(inst);
      double[] dist = m_FilteredClassifier.distributionForInstance(inst);
      for (int i = 0; i < inst.numClasses(); i++) {
        if ((firstDist[i] > 0) && (secondDist[i] > 0)) {
          System.err.println("Panik!!");
        }
        if (m_Range.isInRange(i)) {
          newDist[i] = dist[1] * firstDist[i];
        } else {
          newDist[i] = dist[0] * secondDist[i];
        }
      }
      if  (!Utils.eq(Utils.sum(newDist), 1)) {
        System.err.println(Utils.sum(newDist));
        for (int j = 0; j < dist.length; j++) {
          System.err.print(dist[j] + " ");
        }
        System.err.println();
        for (int j = 0; j < newDist.length; j++) {
          System.err.print(newDist[j] + " ");
        }
        System.err.println();
        System.err.println(inst);
        System.err.println(m_FilteredClassifier);
        //System.err.println(m_Data);
        System.err.println("bad");
      }
      return newDist;
    }
  }
    
  /**
   * Returns the list of indices as a string.
   * 
   * @param indices the indices to return as string
   * @return the indices as string
   */
  public String getString(int [] indices) {

    StringBuffer string = new StringBuffer();
    for (int i = 0; i < indices.length; i++) {
      if (i > 0) {
        string.append(',');
      }
      string.append(indices[i]);
    }
    return string.toString();
  }
	
  /**
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
	    
    return 
        "A meta classifier for handling multi-class datasets with 2-class "
      + "classifiers by building a random data-balanced tree structure.\n\n"
      + "For more info, check\n\n"
      + getTechnicalInformation().toString();
  }
	
  /**
   * Outputs the classifier as a string.
   * 
   * @return a string representation of the classifier
   */
  public String toString() {
	    
    if (m_classifiers == null) {
      return "DataNearBalancedND: No model built yet.";
    }
    StringBuffer text = new StringBuffer();
    text.append("DataNearBalancedND");
    treeToString(text, 0);
	    
    return text.toString();
  }
	
  /**
   * Returns string description of the tree.
   * 
   * @param text the buffer to add the node to
   * @param nn the node number
   * @return the next node number
   */
  private int treeToString(StringBuffer text, int nn) {
	    
    nn++;
    text.append("\n\nNode number: " + nn + "\n\n");
    if (m_FilteredClassifier != null) {
      text.append(m_FilteredClassifier);
    } else {
      text.append("null");
    }
    if (m_FirstSuccessor != null) {
      nn = m_FirstSuccessor.treeToString(text, nn);
      nn = m_SecondSuccessor.treeToString(text, nn);
    }
    return nn;
  }
    	
  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {
	    
    try {
      Classifier scheme = new DataNearBalancedND();
      System.out.println(Evaluation.evaluateModel(scheme, argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
}

