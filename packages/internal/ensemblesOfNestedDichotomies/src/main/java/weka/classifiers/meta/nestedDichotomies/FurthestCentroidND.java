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
 *    FurthestCentroidND.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.meta.nestedDichotomies;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.RandomizableSingleClassifierEnhancer;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.*;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.filters.Filter;
import weka.filters.supervised.attribute.NominalToBinary;
import weka.filters.unsupervised.attribute.MakeIndicator;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.instance.RemoveWithValues;

import java.util.*;

/**
 * <!-- globalinfo-start --> A meta classifier for handling multi-class datasets
 * with 2-class classifiers by building a nested dichotomy, selecting the class
 * subsets based on the furthest centroids.<br/>
 * <br/>
 * For more info, check<br/>
 * <br/>
 * Duarte-Villasenor, Miriam Monica, et al: Nested Dichotomies Based on Clustering.
 * In: Progress in Pattern Recognition, Image Analysis, Computer Vision, and Applications,
 * 2012.
 * <br/>
 * Eibe Frank, Stefan Kramer: Ensembles of nested dichotomies for multi-class
 * problems. In: Twenty-first International Conference on Machine Learning,
 * 2004.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- technical-bibtex-start --> BibTeX:
 * 
 * <pre>
 * &#64;incollection{duarte2012nested,
 *    	title={Nested Dichotomies Based on Clustering},
 * 		author={Duarte-Villasenor, Miriam Monica and Carrasco-Ochoa, Jesus Ariel and Martinez-Trinidad, Jose Francisco and Flores-Garrido, Marisol},
 * 		booktitle={Progress in Pattern Recognition, Image Analysis, Computer Vision, and Applications},
 * 		pages={162--169},
 * 		year={2012},
 * 		publisher={Springer}
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
 * <!-- technical-bibtex-end -->
 *
 * <!-- options-start --> Valid options are:
 * <p/>
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
 * <pre>
 * -W
 *  Full name of base classifier.
 *  (default: weka.classifiers.trees.J48)
 * </pre>
 *
 * <!-- options-end -->
 *
 * @author Tim Leathart
 *
 * Extended from ClassBalancedND.java
 */
public class FurthestCentroidND extends RandomizableSingleClassifierEnhancer
  implements TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = 5944063630650811903L;

  /** The filtered classifier in which the base classifier is wrapped. */
  protected FilteredClassifier m_FilteredClassifier;

  /** The hashtable for this node. */
  protected Hashtable<String, Classifier> m_classifiers;

  /** The first successor */
  protected FurthestCentroidND m_FirstSuccessor = null;

  /** The second successor */
  protected FurthestCentroidND m_SecondSuccessor = null;

  /** The classes that are grouped together at the current node */
  protected Range m_Range = null;

  /** Is Hashtable given from END? */
  protected boolean m_hashtablegiven = false;

  /** List of centroids for each class */
  private double[][] m_Centroids;

  /**
   * Constructor.
   */
  public FurthestCentroidND() {

    m_Classifier = new weka.classifiers.trees.J48();
  }

  public FurthestCentroidND(double[][] centroids) {
    this();
    m_Centroids = centroids;
  }

  /**
   * String describing default classifier.
   * 
   * @return the default classifier classname
   */
  @Override
  protected String defaultClassifierString() {
    return "weka.classifiers.trees.J48";
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
    result.setValue(Field.AUTHOR, "Duarte-Villasenor, Miriam Monica and Carrasco-Ochoa, " 
       + "Jesus Ariel and Martinez-Trinidad, Jose Francisco and Flores-Garrido, Marisol");
    result.setValue(Field.TITLE,
      "Nested Dichotomies Based on Clustering");
    result.setValue(Field.BOOKTITLE, "Progress in Pattern Recognition, Image Analysis, Computer Vision, and Applications");
    result.setValue(Field.YEAR, "2012");
    result.setValue(Field.PAGES, "162-169");
    result.setValue(Field.PUBLISHER, "Springer");

    additional = result.add(Type.INPROCEEDINGS);
    additional.setValue(Field.AUTHOR, "Eibe Frank and Stefan Kramer");
    additional.setValue(Field.TITLE,
      "Ensembles of nested dichotomies for multi-class problems");
    additional.setValue(Field.BOOKTITLE,
      "Twenty-first International Conference on Machine Learning");
    additional.setValue(Field.YEAR, "2004");
    additional.setValue(Field.PUBLISHER, "ACM");

    return result;
  }

  /**
   * Set hashtable from END.
   * 
   * @param table the hashtable to use
   */
  public void setHashtable(Hashtable<String, Classifier> table) {

    m_hashtablegiven = true;
    m_classifiers = table;
  }

  private void computeCentroids(Instances data) throws Exception{
    NominalToBinary nominalToBinary = new NominalToBinary();
    nominalToBinary.setInputFormat(data);
    nominalToBinary.setBinaryAttributesNominal(false);
    Instances numericData = Filter.useFilter(data, nominalToBinary);

    m_Centroids = new double[numericData.numClasses()][numericData.numAttributes()-1];

    Attribute classAttribute = numericData.classAttribute();

    for (int index = 0; index < classAttribute.numValues(); index++) {

      RemoveWithValues removeWithValues = new RemoveWithValues();
      removeWithValues.setInvertSelection(true);
      removeWithValues.setAttributeIndex("" + (numericData.classIndex()+1));
      removeWithValues.setNominalIndicesArr(new int[]{index});
      removeWithValues.setInputFormat(numericData);

      Instances oneClass = Filter.useFilter(numericData, removeWithValues);

      Remove remove = new Remove();
      remove.setInvertSelection(false);
      remove.setAttributeIndicesArray(new int[] {oneClass.classIndex()});
      remove.setInputFormat(oneClass);

      oneClass = Filter.useFilter(oneClass, remove);

      // Initialise an array for the mean of each attribute
      double[] mean = new double[oneClass.numAttributes()];
      for (int i = 0; i < mean.length; i++) {
        mean[i] = 0.0;
      }

      // Go through all instances of this class and sum each attribute
      Enumeration<Instance> instances = oneClass.enumerateInstances();
      while(instances.hasMoreElements()) {
        Instance inst = instances.nextElement();
        for (int i = 0; i < mean.length; i++) {
          mean[i] += inst.value(i);
        }
      }

      // Divide each attribute with number of instances
      for (int i = 0; i < mean.length; i++) {
        mean[i] /= (double)oneClass.numInstances();
      }

      m_Centroids[index] = mean;
    }

    // Remove NaN values by averaging the rest of the centroids
    double[] averages = new double[numericData.numAttributes()-1];
    int[] numPer = new int[numericData.numAttributes()-1];

    for (int i = 0; i < numPer.length; i++) {
      numPer[i] = 1;
    }

    for (int j = 0; j < numericData.numAttributes()-1; j++) {
      for (int i = 0; i < numericData.numClasses(); i++) {
        if (!Double.isNaN(m_Centroids[i][j])) {
          numPer[j] ++;
          averages[j] += m_Centroids[i][j];
        }
      }
    }

    for (int i = 0; i < numPer.length; i++) {
      if (!Double.isNaN(averages[i])) {
        averages[i] /= numPer[i];
      } else {
        averages[i] = 0.0;
      }
    }

    for (int j = 0; j < numericData.numAttributes()-1; j++) {
      for (int i = 0; i < numericData.numClasses(); i++) {
        if (Double.isNaN(m_Centroids[i][j])) {
          m_Centroids[i][j] = averages[j];
        }
      }
    }
  }

  private double getDistance(double[] p1, double[] p2) throws Exception {
    if (p1.length != p2.length) {
      throw new IllegalArgumentException();
    }

    double sum = 0.0;

    for (int i = 0; i < p1.length; i++) {
      double diff = p1[i] - p2[i];
      sum += diff * diff;
    }

    return Math.sqrt(sum);
  }

  /**
   * Generates a classifier for the current node and proceeds recursively.
   * 
   * @param data contains the (multi-class) instances
   * @param classes contains the indices of the classes that are present
   * @param rand the random number generator to use
   * @param classifier the classifier to use
   * @param table the Hashtable to use
   * @throws Exception if anything goes worng
   */
  private void generateClassifierForNode(Instances data, Range classes,
    Random rand, Classifier classifier, Hashtable<String, Classifier> table)
    throws Exception {

    if (m_Centroids == null) {
      computeCentroids(data);
    }

    // Get the indices
    int[] indices = classes.getSelection();
    double greatestDistance = -1.0;
    int ind1 = 0;
    int ind2 = 0;

    // compute furthest classes
    for (int i = 0; i < indices.length; i++) {
      for (int j = i+1; j < indices.length; j++) {
        int i1 = indices[i];
        int i2 = indices[j];

        double dist = getDistance(m_Centroids[i1], m_Centroids[i2]);
        if (dist > greatestDistance) {
          ind1 = i1;
          ind2 = i2;
          greatestDistance = dist;
        }
      }
    }

    // Cluster remaining classes
    Set<Integer> set1 = new HashSet<Integer>();
    Set<Integer> set2 = new HashSet<Integer>();

    set1.add(ind1);
    set2.add(ind2);

    // Cluster the classes
    if (indices.length > 2) {
      for (int i = 0; i < indices.length; i++) {
        int ind = indices[i];

        if (ind == ind1 || ind == ind2) {
          continue;
        }

        double dist1 = getDistance(m_Centroids[ind], m_Centroids[ind1]);
        double dist2 = getDistance(m_Centroids[ind], m_Centroids[ind2]);

        if (dist1 < dist2) {
          set1.add(ind);
        } else {
          set2.add(ind);
        }
      }
    }

    // Pick the classes for the current split
    int first = set1.size();
    int second = set2.size();
    int[] firstInds = collectionToIntArray(set1);
    int[] secondInds = collectionToIntArray(set2);

    // Sort the indices (important for hash key)!
    int[] sortedFirst = Utils.sort(firstInds);
    int[] sortedSecond = Utils.sort(secondInds);
    int[] firstCopy = new int[first];
    int[] secondCopy = new int[second];
    for (int i = 0; i < sortedFirst.length; i++) {
      firstCopy[i] = firstInds[sortedFirst[i]];
    }
    firstInds = firstCopy;
    for (int i = 0; i < sortedSecond.length; i++) {
      secondCopy[i] = secondInds[sortedSecond[i]];
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
    m_FilteredClassifier.setDoNotCheckForModifiedClassAttribute(true);
    if (data.numInstances() > 0) {
      m_FilteredClassifier.setClassifier(AbstractClassifier.makeCopies(
        classifier, 1)[0]);
    } else {
      m_FilteredClassifier.setClassifier(new weka.classifiers.rules.ZeroR());
    }
    m_FilteredClassifier.setFilter(filter);

    // Save reference to hash table at current node
    m_classifiers = table;

    if (!m_classifiers.containsKey(getString(firstInds) + "|"
      + getString(secondInds))) {
      m_FilteredClassifier.buildClassifier(data);
      m_classifiers.put(getString(firstInds) + "|" + getString(secondInds),
        m_FilteredClassifier);
    } else {
      m_FilteredClassifier = (FilteredClassifier) m_classifiers
        .get(getString(firstInds) + "|" + getString(secondInds));
    }

    // Create two successors if necessary
    m_FirstSuccessor = new FurthestCentroidND(m_Centroids);
    if (first == 1) {
      m_FirstSuccessor.m_Range = m_Range;
    } else {
      RemoveWithValues rwv = new RemoveWithValues();
      rwv.setInvertSelection(true);
      rwv.setNominalIndices(m_Range.getRanges());
      rwv.setAttributeIndex("" + (data.classIndex() + 1));
      rwv.setInputFormat(data);
      Instances firstSubset = Filter.useFilter(data, rwv);
      m_FirstSuccessor.generateClassifierForNode(firstSubset, m_Range, rand,
        classifier, m_classifiers);
    }
    m_SecondSuccessor = new FurthestCentroidND(m_Centroids);
    if (second == 1) {
      m_SecondSuccessor.m_Range = secondRange;
    } else {
      RemoveWithValues rwv = new RemoveWithValues();
      rwv.setInvertSelection(true);
      rwv.setNominalIndices(secondRange.getRanges());
      rwv.setAttributeIndex("" + (data.classIndex() + 1));
      rwv.setInputFormat(data);
      Instances secondSubset = Filter.useFilter(data, rwv);
      m_SecondSuccessor = new FurthestCentroidND();

      m_SecondSuccessor.generateClassifierForNode(secondSubset, secondRange,
        rand, classifier, m_classifiers);
    }
  }

  public int[] collectionToIntArray(Collection<Integer> collection) {
    int[] ret = new int[collection.size()];
    int index = 0;
    Iterator<Integer> it = collection.iterator();

    while(it.hasNext()) {
      ret[index++] = it.next();
    }

    return ret;
  }

  /**
   * Returns default capabilities of the classifier.
   * 
   * @return the capabilities of this classifier
   */
  @Override
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
  @Override
  public void buildClassifier(Instances data) throws Exception {

    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    // remove instances with missing class
    data = new Instances(data);
    data.deleteWithMissingClass();

    Random random = data.getRandomNumberGenerator(m_Seed);

    if (!m_hashtablegiven) {
      m_classifiers = new Hashtable<String, Classifier>();
    }

    // Check which classes are present in the
    // data and construct initial list of classes
    boolean[] present = new boolean[data.numClasses()];
    for (int i = 0; i < data.numInstances(); i++) {
      present[(int) data.instance(i).classValue()] = true;
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

    Range newRange = new Range(list.toString());
    newRange.setUpper(data.numClasses() - 1);

    generateClassifierForNode(data, newRange, random, m_Classifier,
      m_classifiers);
  }

  /**
   * Predicts the class distribution for a given instance
   * 
   * @param inst the (multi-class) instance to be classified
   * @return the class distribution
   * @throws Exception if computing fails
   */
  @Override
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
      return newDist;
    }
  }

  /**
   * Returns the list of indices as a string.
   * 
   * @param indices the indices to return as string
   * @return the indices as string
   */
  public String getString(int[] indices) {

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
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>(3);

    newVector.addAll(Collections.list(super.listOptions()));

    return newVector.elements();
  }

  @Override
  public void setOptions(String[] options) throws Exception {

    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String [] getOptions() {

    Vector<String> options = new Vector<String>();

    Collections.addAll(options, super.getOptions());

    return options.toArray(new String[0]);
  }

  /**
   * @return a description of the classifier suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {

    return "A meta classifier for handling multi-class datasets with 2-class "
      + "classifiers by building a nested dichotomy with furthest centroid "
      + "class subset selection.\n\n"
      + "For more info, check\n\n" + getTechnicalInformation().toString();
  }

  /**
   * Outputs the classifier as a string.
   * 
   * @return a string representation of the classifier
   */
  @Override
  public String toString() {

    if (m_classifiers == null) {
      return "FurthestCentroidND: No model built yet.";
    }
    StringBuffer text = new StringBuffer();
    text.append("FurthestCentroidND");
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
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 10342 $");
  }

  /**
   * Main method for testing this class.
   * 
   * @param argv the options
   */
  public static void main(String[] argv) {
    runClassifier(new FurthestCentroidND(), argv);
  }
}
