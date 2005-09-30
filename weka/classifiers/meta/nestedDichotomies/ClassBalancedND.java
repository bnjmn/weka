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
 *    ClassBalancedND.java
 *    Copyright (C) 2005 University of Waikato
 *
 */

package weka.classifiers.meta.nestedDichotomies;

import weka.core.Range;
import weka.core.Instances;
import weka.core.Instance;
import weka.core.Utils;
import weka.core.OptionHandler;
import weka.core.Option;
import weka.core.Randomizable;
import weka.core.UnsupportedClassTypeException;

import weka.filters.Filter;
import weka.filters.unsupervised.attribute.MakeIndicator;
import weka.filters.unsupervised.instance.RemoveWithValues;

import weka.classifiers.Classifier;
import weka.classifiers.RandomizableSingleClassifierEnhancer;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.FilteredClassifier;

import java.util.Random;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Class that represents and builds a random class-balanced system of
 * nested dichotomies. For details, check<p>
 *
 * Lin Dong, Eibe Frank, and Stefan Kramer (2005). Ensembles of
 * Balanced Nested Dichotomies for Multi-Class Problems. PKDD,
 * Porto. Springer-Verlag.
 *
 * <p>and<p>
 * 
 * Eibe Frank and Stefan Kramer (2004). Ensembles of Nested
 * Dichotomies for Multi-class Problems. Proceedings of the
 * International Conference on Machine Learning, Banff. Morgan
 * Kaufmann.
 *
 * @author Lin Dong
 * @author Eibe Frank
 */
public class ClassBalancedND extends RandomizableSingleClassifierEnhancer {

  /** The filtered classifier in which the base classifier is wrapped. */
  protected FilteredClassifier m_FilteredClassifier;
    
  /** The hashtable for this node. */
  protected Hashtable m_classifiers;

  /** The first successor */
  protected ClassBalancedND m_FirstSuccessor = null;

  /** The second successor */
  protected ClassBalancedND m_SecondSuccessor = null;
  
  /** The classes that are grouped together at the current node */
  protected Range m_Range = null;
    
  /** Is Hashtable given from END? */
  protected boolean m_hashtablegiven = false;
    
  /**
   * Constructor.
   */
  public ClassBalancedND() {
    
    m_Classifier = new weka.classifiers.trees.J48();
  }
  
  /**
   * String describing default classifier.
   */
  protected String defaultClassifierString() {
    
    return "weka.classifiers.trees.J48";
  }

  /**
   * Set hashtable from END.
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
   */
  private void generateClassifierForNode(Instances data, Range classes,
                                         Random rand, Classifier classifier, Hashtable table) 
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
    int indicesLen = indices.length;
    int first = indices.length / 2;
    int second = indices.length - first;
    int[] firstInds = new int[first];
    int[] secondInds = new int[second];
    System.arraycopy(indices, 0, firstInds, 0, first);
    System.arraycopy(indices, first, secondInds, 0, second);
    	
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
    m_FirstSuccessor = new ClassBalancedND();
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
                                                 rand, classifier, m_classifiers);
    }
    m_SecondSuccessor = new ClassBalancedND();
    if (second == 1) {
      m_SecondSuccessor.m_Range = secondRange;
    } else {
      RemoveWithValues rwv = new RemoveWithValues();
      rwv.setInvertSelection(true);
      rwv.setNominalIndices(secondRange.getRanges());
      rwv.setAttributeIndex("" + (data.classIndex() + 1));
      rwv.setInputFormat(data);
      Instances secondSubset = Filter.useFilter(data, rwv);
      m_SecondSuccessor = new ClassBalancedND();
      
      m_SecondSuccessor.generateClassifierForNode(secondSubset, secondRange, 
                                                  rand, classifier, m_classifiers);
    }
  }
    
  /**
   * Builds tree recursively.
   *
   * @param data contains the (multi-class) instances
   */
  public void buildClassifier(Instances data) throws Exception {

    // Check for non-nominal classes
    if (!data.classAttribute().isNominal()) {
      throw new UnsupportedClassTypeException("ClassBalancedND: class must " +
					      "be nominal!");
    }

    data.deleteWithMissingClass();

    if (data.numInstances() == 0) {
      throw new Exception("No instances in training file!");
    }
    
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
      
    Range newRange = new Range(list.toString());
    newRange.setUpper(data.numClasses() - 1);
	
    generateClassifierForNode(data, newRange, random, m_Classifier, m_classifiers);
  }
    
  /**
   * Predicts the class distribution for a given instance
   *
   * @param inst the (multi-class) instance to be classified
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
      return newDist;
    }
  }
    
  /**
   * Returns the list of indices as a string.
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
	    
    return "A meta classifier for handling multi-class datasets with 2-class "
      + "classifiers by building a random class-balanced tree structure. For more info, check\n\n"
      + "Lin Dong, Eibe Frank, and Stefan Kramer (2005). Ensembles of "
      + "Balanced Nested Dichotomies for Multi-Class Problems. PKDD, Porto. Springer-Verlag\n\nand\n\n"
      + "Eibe Frank and Stefan Kramer (2004). Ensembles of Nested Dichotomies for Multi-class Problems. "
      + "Proceedings of the International Conference on Machine Learning, Banff. Morgan Kaufmann.";
  }
	
  /**
   * Outputs the classifier as a string.
   */
  public String toString() {
	    
    if (m_classifiers == null) {
      return "ClassBalancedND: No model built yet.";
    }
    StringBuffer text = new StringBuffer();
    text.append("ClassBalancedND");
    treeToString(text, 0);
	    
    return text.toString();
  }
	
  /**
   * Returns string description of the tree.
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
      Classifier scheme = new ClassBalancedND();
      System.out.println(Evaluation.evaluateModel(scheme, argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
}
