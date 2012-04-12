/*
 *    Instances.java
 *    Copyright (C) 1999 Eibe Frank
 *
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

package weka.core;

import java.io.*;
import java.util.*;

/**
 * Class for handling an ordered set of weighted instances.
 *
 * All methods that change a set of instances are
 * safe, ie. a change of a set of instances does
 * not affect any other sets of instances. All
 * methods that change a datasets's header 
 * information or structure clone the dataset before
 * it is changed.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.1 September 1998 (Eibe)
 */

public class Instances implements Serializable {
 
  // =================
  // Private variables
  // =================

  /**
   * The dataset's name.
   */

  private String theRelationName;         

  /**
   * The attribute information.
   */

  private FastVector theAttributes;

  /**
   * The instances. 
   */

  private FastVector theInstances;

  /**
   * The class attribute's index
   */

  private int theClassIndex;

  // ===============
  // Public methods.
  // ===============

  /**
   * Reads an ARFF file from a reader, assigns a weight of
   * one to each instance and lets the index of the class 
   * attribute undefined.
   * @param reader the reader
   * @exception Exception if the ARFF file is not read 
   * successfully
   */

  public Instances(Reader reader) throws Exception {

    StreamTokenizer tokenizer;

    tokenizer = new StreamTokenizer(reader);
    initTokenizer(tokenizer);
    readHeader(tokenizer);
    theClassIndex = -1;
    theInstances = new FastVector(1000);
    while (getInstance(tokenizer, true)) {};
    compactify();
  }
 
  /**
   * Reads the header of an ARFF file from a reader and 
   * reserves space for the given number of instances. Lets
   * the class index undefined.
   * @param reader the reader
   * @param capacity the capacity
   * @exception Exception if the header is not read successfully
   * or the capacity is not positive or zero
   */

   public Instances(Reader reader, int capacity) 
       throws Exception {

    StreamTokenizer tokenizer;

    if (capacity < 0) 
      throw new Exception("Capacity has to be positive!");
    tokenizer = new StreamTokenizer(reader); 
    initTokenizer(tokenizer);
    readHeader(tokenizer);
    theClassIndex = -1;
    theInstances = new FastVector(capacity);
  }

  /**
   * Constructor copying all instances and references to
   * the header information from the given set of instances.
   * @param instances the set to be copied
   */

  public Instances(Instances dataset) {

    this(dataset, dataset.numInstances());

    dataset.copyInstances(0, this, dataset.numInstances());
  }

  /**
   * Constructor creating an empty set of instances. Sets the 
   * capacity of the set of instances to 0 if its negative.
   * @param instances the instances from which the header 
   * information is to be taken
   * @param capacity the capacity of the new dataset
   */

  public Instances(Instances dataset, int capacity) {
    
    if (capacity < 0) {
      capacity = 0;
    }
    
    // Strings only have to be "shallow" copied because
    // they can't be modified.

    theClassIndex = dataset.theClassIndex;
    theRelationName = dataset.theRelationName;
    theAttributes = dataset.theAttributes;
    theInstances = new FastVector(capacity);
  }

  /**
   * Creates a new ordered set of instances by copying a 
   * subset of another ordered set. 
   * @param source the set of instances from which a subset 
   * is to be created
   * @param first the index of the first instance to be copied
   * @param toCopy the number of instances to be copied
   * @exception Exception if first and toCopy are not within range
   */

  public Instances(Instances source, int first, int toCopy)
       throws Exception {
    
    this(source, toCopy);

    if ((first < 0) || ((first + toCopy) > source.numInstances())) {
      throw new Exception("Parameters first and/or toCopy out "+
			  "of range");
    }
    source.copyInstances(first, this, toCopy);
  }

  /**
   * Creates an empty ordered set of instances. Uses the given
   * attribute information. Sets the capacity of the set of 
   * instances to 0 if its negative. Given attribute information
   * must not be changed after this method has been called.
   * @param name the name of the relation
   * @param attInfo the attribute information
   * @param capacity the capacity of the set
   */

  public Instances(String name, FastVector attInfo, int capacity) {

    theRelationName = name;
    theClassIndex = -1;
    theAttributes = attInfo;
    for (int i = 0; i < numAttributes(); i++) {
      attribute(i).setIndex(i);
    }
    theInstances = new FastVector(capacity);
  }
 
  /**
   * Adds one instance to the end of the ordered set. 
   * Shallow copies instance before it is added. Increases the
   * size of the dataset if it is not large enough. Does not
   * check if the instance is compatible with the dataset.
   * @param instance the instance to be added
   */

  public final void add(Instance instance) {

    Instance newInstance = new Instance(instance);

    newInstance.setDataset(this);
    theInstances.addElement(newInstance);
  }

  /** 
   * Sets the weights of instances according to a misclassification
   * cost matrix, or does resampling (if a random number generator
   * is provided). Returns a new dataset.
   * @param matrix the matrix containing the misclassification 
   * costs 
   * @param random a random number generator
   * @return the new dataset
   * @exception Exception if the cost matrix does not have the 
   * right format
   */

  public Instances applyCostMatrix(double[][] weights, Random random) 
       throws Exception {

    double sumOfWeightFactors = 0, sumOfMissClassWeights,
      sumOfWeights;
    double [] weightOfInstancesInClass, weightFactor, weightOfInstances;
    Instances newData;

    if (theClassIndex < 0) {
      throw new Exception("Class index is negativ (not set)!");
    }
    if (weights.length != numClasses()) {
      throw new Exception("Misclassification cost matrix has "+
			  "wrong format!");
    }
    weightFactor = new double [numClasses()];
    weightOfInstancesInClass = new double [numClasses()];
    for (int j = 0; j < numInstances(); j++) {
      weightOfInstancesInClass[(int)instance(j).classValue()] += 
	instance(j).weight();
    }
    sumOfWeights = Utils.sum(weightOfInstancesInClass);
    for (int i = 0; i < numClasses(); i++) {
      if (weights[i].length != numClasses()) {
	throw new Exception("Misclassification cost matrix has "+
			    "wrong format!");
      }
      if (!Utils.eq(weights[i][i],0)) {
	throw new Exception("Diagonal of misclassification cost "+
			    "matrix not zero!");
      }
      // Using Kai Ming Ting's formula for deriving weights for 
      // the classes and Breiman's heuristic for multiclass 
      // problems.
      
      sumOfMissClassWeights = 0;
      for (int j = 0; j < numClasses(); j++) {
	if (Utils.sm(weights[i][j],0)) {
	  throw new Exception("Neg. weights in misclassification "+
			      "cost matrix!"); 
	}
	sumOfMissClassWeights += weights[i][j];
      }
      weightFactor[i] = sumOfMissClassWeights * sumOfWeights;
      sumOfWeightFactors += sumOfMissClassWeights * 
	weightOfInstancesInClass[i];
    }
    for (int i = 0; i < numClasses(); i++) {
      weightFactor[i] /= sumOfWeightFactors;
    }
    
    // Store new weights
    weightOfInstances = new double[numInstances()];
    for (int i = 0; i < numInstances(); i++) {
      weightOfInstances[i] =instance(i).weight()*
	weightFactor[(int)instance(i).classValue()];
    } 

    // Change instances weight or do resampling
    if (random != null) {
      return resampleWithWeights(random, weightOfInstances);
    } else { 
      Instances instances = new Instances(this);
      for (int i = 0; i < numInstances(); i++) {
	instances.instance(i).setWeight(weightOfInstances[i]);
      }
      return instances;
    }
  }

  /**
   * Returns an attribute.
   * @param index the attribute's index
   * @return the attribute at the given position
   */ 

  public final Attribute attribute(int index) {
    
    return (Attribute) theAttributes.elementAt(index);
  }

  /**
   * Returns an attribute given its name. If there is more than
   * one attribute with the same name, it returns the first one.
   * Returns null if the attribute can't be found.
   * @param name the attribute's name
   * @return the attribute with the given name, null if the
   * attribute can't be found
   */ 

  public final Attribute attribute(String name) {
    
    for (int i = 0; i < numAttributes(); i++) {
      if (attribute(i).name().equals(name)) {
	return attribute(i);
      }
    }
    return null;
  }

  /**
   * Checks if the given instance is compatible
   * with this dataset. Only looks at the size of
   * the instance and the ranges of the values for 
   * nominal and string attributes.
   */

  public final boolean checkInstance(Instance instance) {

    if (instance.numAttributes() != numAttributes()) {
      return false;
    }
    for (int i = 0; i < numAttributes(); i++) {
      if (instance.isMissing(i)) {
	continue;
      } else if (attribute(i).isNominal() ||
		 attribute(i).isString()) {
	if (!(Utils.eq(instance.value(i),
		       (double)(int)instance.value(i)))) {
	  return false;
	} else if (Utils.sm(instance.value(i), 0) ||
		   Utils.gr(instance.value(i),
			    attribute(i).numValues())) {
	  return false;
	}
      }
    }
    return true;
  }
	
  /**
   * Returns class attribute.
   * @return the class attribute
   * @exception Exception if the class is not set
   */

  public final Attribute classAttribute() throws Exception {

    if (theClassIndex < 0) {
      throw new Exception("Class index is negativ (not set)!");
    }
    return attribute(theClassIndex);
  }

  /**
   * Returns the class attribute's index. Returns negative number
   * if undefined.
   * @return the class index as an integer
   */

  public final int classIndex() {
    
    return theClassIndex;
  }
 
  /**
   * Compactifies the set of instances. Decreases the capacity of
   * the set so that it matches the number of instances in the set.
   */

  public final void compactify() {

    theInstances.trimToSize();
  }

  /**
   * Removes an instance at the given position from the set.
   * @param index the instance's position
   */
  
  public final void delete(int index) {
    
    theInstances.removeElementAt(index);
  }

  /**
   * Removes all instances with missing values for a particular
   * attribute from the dataset and returns the resulting dataset.
   * @param attIndex the attribute's index
   */

  public final void deleteWithMissing(int attIndex) {

    FastVector newInstances = new FastVector(numInstances());

    for (int i = 0; i < numInstances(); i++) 
      if (!instance(i).isMissing(attIndex)) 
	newInstances.addElement(instance(i));
    theInstances = newInstances;
  }

  /**
   * Removes all instances with missing values for a particular
   * attribute from the dataset and returns the resulting dataset.
   * @param att the attribute
   */

  public final void deleteWithMissing(Attribute att) {

    deleteWithMissing(att.index());
  }

  /**
   * Removes all instances with a missing class value
   * from the dataset.
   * @exception Exception if class is not set
   */

  public final void deleteWithMissingClass() throws Exception {

    if (theClassIndex < 0) {
      throw new Exception("Class index is negativ (not set)!");
    }
    deleteWithMissing(theClassIndex);
  }

  /**
   * Returns an enumeration of all the attributes.
   * @return enumeration of all the attributes.
   */

  public Enumeration enumerateAttributes() {

    return theAttributes.elements(theClassIndex);
  }

  /**
   * Returns an enumeration of all instances in the dataset.
   * @return enumeration of all instances in the dataset
   */

  public final Enumeration enumerateInstances() {

    return theInstances.elements();
  }

  /**
   * Checks if two headers are equivalent.
   * @param dataset another dataset
   * @return true if the header of the given dataset is equivalent 
   * to this header
   */

  public final boolean equalHeaders(Instances dataset){

    // Check class and all attributes

    if (theClassIndex != dataset.theClassIndex)
      return false;
    if (theAttributes.size() != dataset.theAttributes.size())
      return false;
    for (int i = 0; i < theAttributes.size(); i++) 
      if (!(attribute(i).equals(dataset.attribute(i))))
	return false;
    return true;
  }

  /**
   * Deletes an attribute at the given position 
   * (0 to numAttributes() - 1).
   * @param pos the attribute's position
   * @exception Exception if the given index is out of range
   */

  public void deleteAttributeAt(int position) throws Exception {
	 
    if ((position < 0) || (position >= theAttributes.size()))
      throw new Exception("Can't delete attribute: index out of "+
			  "range");
    if (position == theClassIndex)
      throw new Exception("Can't delete class attribute");
    freshAttributeInfo();
    if (theClassIndex > position)
      theClassIndex--;
    theAttributes.removeElementAt(position);
    for (int i = position; i < theAttributes.size(); i++) {
      Attribute current = (Attribute)theAttributes.elementAt(i);
      current.setIndex(current.index() - 1);
    }
    for (int i = 0; i < numInstances(); i++) 
      instance(i).forceDeleteAttributeAt(position);
  }

  /**
   * Deletes all string attributes in the dataset.
   * @exception Exception if string attribute couldn't be 
   * successfully deleted.
   */

  public void deleteStringAttributes() throws Exception {

    int i = 0;
   
    while (i < theAttributes.size()) 
      if (attribute(i).isString())
	deleteAttributeAt(i);
      else
	i++;
  }
  
  /**
   * Inserts an attribute at the given position (0 to 
   * numAttributes()) and sets all values to be missing.
   * Clones the attribute before it is inserted.
   * @param att the attribute to be inserted
   * @param pos the attribute's position
   * @exception Exception if the given index is out of range
   */

  public void insertAttributeAt(Attribute att, int position) 
       throws Exception {
	 
    if ((position < 0) ||
	(position > theAttributes.size()))
      throw new Exception("Can't insert attribute: index out "+
			  "of range");
    att = (Attribute)att.copy();
    freshAttributeInfo();
    att.setIndex(position);
    theAttributes.insertElementAt(att, position);
    for (int i = position + 1; i < theAttributes.size(); i++) {
      Attribute current = (Attribute)theAttributes.elementAt(i);
      current.setIndex(current.index() + 1);
    }
    for (int i = 0; i < numInstances(); i++) 
      instance(i).forceInsertAttributeAt(position);
    if (theClassIndex >= position)
      theClassIndex++;
  }

  /**
   * Returns the instance with the given position.
   * @param index the instance's index
   * @returns the instance at the given position
   */

  public final Instance instance(int index) {

    return (Instance)theInstances.elementAt(index);
  }

  /**
   * Returns the last instance in the set.
   * @return the last instance in the set
   */

  public final Instance lastInstance() {
    
    return (Instance)theInstances.lastElement();
  }

  /**
   * Returns the mean (mode) for a numeric (nominal) attribute.
   * Returns 0 if the attribute is neither nominal nor numeric.
   * If all values are missing it returns zero.
   * @param attIndex the attribute's index
   * @return the mean or the mode
   */

  public final double meanOrMode(int attIndex) {

    double result, found;
    int [] counts;

    if (attribute(attIndex).isNumeric()) {
      result = found = 0;
      for (int j = 0; j < numInstances(); j++) 
	if (!instance(j).isMissing(attIndex)) {
	  found += instance(j).weight();
	  result += instance(j).weight()*instance(j).value(attIndex);
	}
      if (Utils.eq(found, 0))
	return 0;
      else
	return result / found;
    } else if (attribute(attIndex).isNominal()) {
      counts = new int[attribute(attIndex).numValues()];
      for (int j = 0; j < numInstances(); j++) 
	if (!instance(j).isMissing(attIndex)) 
	  counts[(int) instance(j).value(attIndex)] += instance(j).weight();
      return (double)Utils.maxIndex(counts);
    } else 
      return 0;
  }

  /**
   * Returns the mean (mode) for a numeric (nominal) attribute.
   * Returns 0 if the attribute is neither nominal nor numeric.
   * If all values are missing it returns zero.
   * @param att the attribute
   * @return the mean or the mode
   */

  public final double meanOrMode(Attribute att) {

    return meanOrMode(att.index());
  }

  /**
   * Returns the number of attributes.
   * @return the number of attributes as an integer
   */

  public final int numAttributes(){

    return theAttributes.size();
  }

  /**
   * Returns the number of class labels.
   * @return the number of class labels as an integer if the class 
   * attribute is nominal, 1 otherwise.
   * @exception Exception if the class is not set
   */
  
  public final int numClasses() throws Exception {
    
    if (theClassIndex < 0)
      throw new Exception("Class index is negativ (not set)!");
    if (!classAttribute().isNominal())
      return 1;
    else
      return classAttribute().numValues();
  }

  /**
   * Returns the number of distinct values of a given attribute.
   * Returns the number of instances if the attribute is a
   * string attribute.
   * The value 'missing' is not counted.
   * @param attIndex the attribute
   * @return the number of distinct values of a given attribute
   */

  public final int numDistinctValues(int attIndex) {

    double prev = 0;
    int counter = 0;

    if (attribute(attIndex).isNumeric()) {
      sort(attIndex);
      for (int i = 0; i < numInstances(); i++) {
	if (instance(i).isMissing(attIndex)) 
	  break;
	if ((i == 0) || 
	    Utils.gr(instance(i).value(attIndex), prev)) {
	  prev = instance(i).value(attIndex);
	  counter++;
	}
      }
      return counter;
    } else
      return attribute(attIndex).numValues();
  }

  /**
   * Returns the number of distinct values of a given attribute.
   * Returns the number of instances if the attribute is a
   * string attribute.
   * The value 'missing' is not counted.
   * @param att the attribute
   * @return the number of distinct values of a given attribute
   */

  public final int numDistinctValues(Attribute att) {

    return numDistinctValues(att.index());
  }
  
  /**
   * Returns the number of instances in the dataset.
   * @return the number of instances in the dataset as an integer
   */

  public final int numInstances(){

    return theInstances.size();
  }

  /**
   * Reads misclassification cost matrix from given reader. 
   * Each line has to contain three numbers: the index of the true 
   * class, the index of the incorrectly assigned class, and the 
   * weight, separated by white space characters. Comments can be 
   * appended to the end of a line by using the '%' character.
   * @param reader the reader from which the cost 
   * matrix is to be read
   * @return the matrix containing the misclassification costs
   * @exception Exception if the cost matrix does not have the 
   * right format or
   * the class is not set
   */

  public final double[][] readCostMatrix(Reader reader) 
       throws Exception {

    StreamTokenizer tokenizer;
    double [][] costMatrix = 
      new double [numClasses()][numClasses()];
    int currentToken;
    double firstIndex, secondIndex, weight;

    tokenizer = new StreamTokenizer(reader);
    if (theClassIndex < 0)
      throw new Exception("Class index is negativ (not set)!");
    for (int i = 0; i < numClasses(); i++)
      for (int j = 0; j < numClasses(); j++)
	if (i != j) costMatrix[i][j] = 1.0;
    tokenizer.commentChar('%');
    tokenizer.eolIsSignificant(true);
    while (StreamTokenizer.TT_EOF != 
	   (currentToken = tokenizer.nextToken())) {

      // Skip empty lines 
      
      if (currentToken == StreamTokenizer.TT_EOL)  
	continue;

      // Get index of first class.

      if (currentToken != StreamTokenizer.TT_NUMBER)
	throw new Exception("Only numbers and comments allowed "+
			    "in cost file!");
      firstIndex = tokenizer.nval;
      if (!Utils.eq((double)(int)firstIndex,firstIndex))
	throw new Exception("First number in line has to be "+
			    "index of a class!");
      if ((int)firstIndex >= numClasses())
	throw new Exception("Class index out of range!");

      // Get index of second class.

      if (StreamTokenizer.TT_EOF == 
	  (currentToken = tokenizer.nextToken()))
	throw new Exception("Premature end of file!");
      if (currentToken == StreamTokenizer.TT_EOL)
	throw new Exception("Premature end of line!");
      if (currentToken != StreamTokenizer.TT_NUMBER)
	throw new Exception("Only numbers and comments allowed "+
			    "in cost file!");
      secondIndex = tokenizer.nval;
      if (!Utils.eq((double)(int)secondIndex,secondIndex))
	throw new Exception("Second number in line has to be "+
			    "index of a class!");
      if ((int)secondIndex >= numClasses())
	throw new Exception("Class index out of range!");
      if ((int)secondIndex == (int)firstIndex)
	throw new Exception("Diagonal of cost matrix non-zero!");

      // Get cost factor.

      if (StreamTokenizer.TT_EOF == 
	  (currentToken = tokenizer.nextToken()))
	throw new Exception("Premature end of file!");
      if (currentToken == StreamTokenizer.TT_EOL)
	throw new Exception("Premature end of line!");
      if (currentToken != StreamTokenizer.TT_NUMBER)
	throw new Exception("Only numbers and comments allowed "+
			    "in cost file!");
      weight = tokenizer.nval;
      if (!Utils.gr(weight,0))
	throw new Exception("Only positive weights allowed!");
      costMatrix[(int)firstIndex][(int)secondIndex] = weight;
    }

    return costMatrix;
  }

  /**
   * Reads a single instance from the reader and appends it
   * to the dataset.  Automatically expands the dataset if it
   * is not large enough to hold the instance. This method does
   * not check for carriage return at the end of the line.
   * @param reader the reader 
   * @return false if end of file has been reached
   * @exception IOException if the information is not read 
   * successfully
   */ 

  public final boolean readInstance(Reader reader) 
       throws IOException{

    StreamTokenizer tokenizer = new StreamTokenizer(reader);
    
    initTokenizer(tokenizer);
    return getInstance(tokenizer, false);
  }    

  /**
   * Shuffles the instances in the set so that they are ordered 
   * randomly.
   */

  public final void randomize(Random random){

    for (int j = numInstances() - 1; j > 0; j--)
      swap(j,(int)(random.nextDouble()*(double)j));
  }

  /**
   * Returns the relation's name.
   * @return the relation's name as a string
   */

  public final String relationName(){

    return theRelationName;
  }

  /**
   * Sets the relation's name.
   * @param newName the new relation name.
   */
  public final void setRelationName(String newName) {
    
    theRelationName = newName;
  }

  /**
   * Creates a new dataset of the same size using random sampling
   * with replacement.
   *
   * @param random a random number generator
   * @return the new dataset
   */
  public final Instances resample(Random random) {

    Instances newData = new Instances(this, numInstances());
    while (newData.numInstances() < numInstances()) {
      int i = (int) (random.nextDouble() * (double) numInstances());
      newData.add(instance(i));
    }
    return newData;
  }

  /**
   * Creates a new dataset of the same size using random sampling
   * with replacement according to the given weight vector. The
   * weights of the instances in the new dataset are set to one.
   * The length of the weight vector has to be the same as the
   * number of instances in the dataset, and all weights have to
   * be positive.
   *
   * @param random a random number generator
   * @param weights the weight vector
   * @return the new dataset
   * @exception Exception if something goes wrong
   */
  public final Instances resampleWithWeights(Random random, 
					     double[] weights) 
    throws Exception {

    if (weights.length != numInstances()) {
      throw new Exception("Length of weight vector incompatible.");
    }
    Instances newData = new Instances(this, numInstances());
    double[] probabilities = new double[numInstances()];
    double sumProbs = 0, sumOfWeights = Utils.sum(weights);
    for (int i = 0; i < numInstances(); i++) {
      sumProbs += random.nextDouble();
      probabilities[i] = sumProbs;
    }
    Utils.normalize(probabilities, sumProbs / sumOfWeights);

    // Make sure that rounding errors don't mess things up
    probabilities[numInstances() - 1] = sumOfWeights;
    int k = 0; int l = 0;
    sumProbs = 0;
    while ((k < numInstances() && (l < numInstances()))) {
      if (weights[l] < 0) {
	throw new Exception("Weights have to be positive.");
      }
      sumProbs += weights[l];
      while ((k < numInstances()) &&
	     (probabilities[k] <= sumProbs)) { 
	newData.add(instance(l));
	newData.instance(k).setWeight(1);
	k++;
      }
      l++;
    }
    return newData;
  }

  /** 
   * Sets the class index of the set.
   * If the class index is negative there is assumed to be no class.
   *
   * @param classIndex the new class index
   * @exception Exception if the class index is too big
   */

  public final void setClassIndex(int classIndex) throws Exception {

    if (classIndex >= numAttributes()) {
      throw new Exception("Class index to large!");
    }
    theClassIndex = classIndex;
  }

  /**
   * Sorts the instances based on an attribute. For numeric attributes, 
   * instances are sorted into ascending order. For nominal attributes, 
   * instances are sorted based on the attribute label ordering 
   * specified in the header. Instances with missing values for the 
   * attribute are placed at the end of the dataset.
   *
   * @param attIndex the attribute
   */

  public final void sort(int attIndex) {

    int i,j;

    // move all instances with missing values to end
    
    j = numInstances() - 1;
    i = 0;
    while (i <= j) {
      if (instance(j).isMissing(attIndex)) {
	j--;
      } else {
	if (instance(i).isMissing(attIndex)) {
	  swap(i,j);
	  j--;
	}
	i++;
      }
    }
    quickSort(attIndex, 0, j);
  }

  /**
   * Sorts the instances based on an attribute. For numeric attributes, 
   * instances are sorted into ascending order. For nominal attributes, 
   * instances are sorted based on the attribute label ordering 
   * specified in the header. Instances with missing values for the 
   * attribute are placed at the end of the dataset.
   *
   * @param att the attribute
   */

  public final void sort(Attribute att){

    sort(att.index());
  }

  /**
   * Stratifies a set of instances according to their class values 
   * if the class attribute is nominal so that afterwards a 
   * stratified cross-validation can be performed.
   *
   * @param numFolds the numer of folds in the cross-validation
   * @exception Exception if the class is not set
   */
  
  public final void stratify(int numFolds) throws Exception {
    
    Instance instance1, instance2;
    int j, index;
    
    if (theClassIndex < 0) {
      throw new Exception("Class index is negative (not set)!");
    }
    if (classAttribute().isNominal()) {

      // sort by class
      
      index = 1;
      while (index < numInstances()) {
	instance1 = instance(index - 1);
	for (j = index; j < numInstances(); j++) {
	  instance2 = instance(j);
	  if ((instance1.classValue() == instance2.classValue()) ||
	      (instance1.classIsMissing() && 
	       instance2.classIsMissing())) {
	    swap(index,j);
	    index++;
	  }
	}
	index++;
      }
      stratStep(numFolds);
    }
  }
 
  /**
   * Computes the sum of all the instances' weights.
   *
   * @return the sum of all the instances' weights as a double
   */

  public final double sumOfWeights() {
    
    double sum = 0;

    for (int i = 0; i < numInstances(); i++)
      sum += instance(i).weight();
  
    return sum;
  }

  /**
   * Creates the test set for one fold of a cross-validation on 
   * the dataset.
   *
   * @param numFolds the number of folds in the cross-validation. Must
   * be greater than 1.
   * @param numFold 0 for the first fold, 1 for the second, ...
   * @return the test set as an ordered set of weighted instances
   * @exception Exception if dataset can't be generated 
   * successfully
   */

  public Instances testCV(int numFolds, int numFold) 
       throws Exception {

    int numInstForFold, first, offset;
    Instances test;
    
    numInstForFold = numInstances() / numFolds;
    if (numFold < numInstances() % numFolds){
      numInstForFold++;
      offset = numFold;
    }else
      offset = numInstances() % numFolds;
    test = new Instances(this, numInstForFold);
    first = numFold * (numInstances() / numFolds) + offset;
    copyInstances(first, test, numInstForFold);

    return test;
  }
 
  /**
   * Returns the dataset in ARFF format.
   * @return the dataset in ARFF format as a string
   */

  public final String toString() {
    
    StringBuffer text = new StringBuffer();
    
    text.append("@relation " + theRelationName + "\n\n");
    for (int i = 0; i < numAttributes(); i++) 
      text.append(attribute(i) + "\n");
    text.append("\n@data\n");
    for (int i = 0; i < numInstances(); i++) {
      text.append(instance(i));
      if (i < numInstances() - 1)
	text.append('\n');
    }
        
    return text.toString();
  }

  /**
   * Creates the training set for one fold of a cross-validation 
   * on the dataset.
   * @param numFolds the number of folds in the cross-validation. Must
   * be greater than 1.
   * @param numFold 0 for the first fold, 1 for the second, ...
   * @return the training set as an ordered set of weighted 
   * instances
   * @exception Exception if dataset can't be generated 
   * successfully
   */

  public Instances trainCV(int numFolds, int numFold) 
       throws Exception {

    int numInstForFold, first, offset;
    Instances train;
 
    numInstForFold = numInstances() / numFolds;
    if (numFold < numInstances() % numFolds) {
      numInstForFold++;
      offset = numFold;
    }else
      offset = numInstances() % numFolds;
    train = new Instances(this, numInstances() - numInstForFold);
    first = numFold * (numInstances() / numFolds) + offset;
    copyInstances(0, train, first);
    copyInstances(first + numInstForFold, train,
		  numInstances() - first - numInstForFold);

    return train;
  }

  /**
   * Computes the variance for a numeric attribute.
   * @param attIndex the numeric attribute
   * @return the variance if the attribute is numeric
   * @exception Exception if the attribute is not numeric
   */

  public final double variance(int attIndex) throws Exception {
  
    double sum = 0, sumSquared = 0, sumOfWeights = 0;

    if (!attribute(attIndex).isNumeric())
      throw new Exception("Can't compute variance for numeric "
			  + "attribute!");
    for (int i = 0; i < numInstances(); i++) {
      if (!instance(i).isMissing(attIndex)) {
	sum += instance(i).weight() * 
	  instance(i).value(attIndex);
	sumSquared += instance(i).weight() * 
	  instance(i).value(attIndex) *
	  instance(i).value(attIndex);
	sumOfWeights += instance(i).weight();
      }
    }
    if (Utils.smOrEq(sumOfWeights, 1))
      return 0;
    return (sumSquared - (sum * sum / sumOfWeights)) / 
      (sumOfWeights - 1);
  }

  /**
   * Computes the variance for a numeric attribute.
   * @param att the numeric attribute
   * @return the variance if the attribute is numeric
   * @exception Exception if the attribute is not numeric
   */

  public final double variance(Attribute att) throws Exception {
    
    return variance(att.index());
  }

  // ==================
  // Protected methods.
  // ==================
 
  /**
   * Reads a single instance using the tokenizer and appends it
   * to the dataset. Automatically expands the dataset if it
   * is not large enough to hold the instance.
   * @param tokenizer the tokenizer to be used
   * @param flag if method should test for carriage return after 
   * each instance
   * @return false if end of file has been reached
   * @exception IOException if the information is not read 
   * successfully
   */ 

  protected boolean getInstance(StreamTokenizer tokenizer, 
				boolean flag) 
       throws IOException {

    double[] instance;
    int index;
    
    // Check if any attributes have been declared.
    
    if (theAttributes.size() == 0)
      errms(tokenizer,"no header information available");
      
    // Reserve space for new instance.
    
    instance = new double [numAttributes()];
      
      // Get values for all attributes.

    for (int i = 0; i < numAttributes(); i++){

      // Check if end of file reached.
      
      if (i == 0){
	getFirstToken(tokenizer);
	if (tokenizer.ttype == StreamTokenizer.TT_EOF)
	  return false;
      }else
	getNextToken(tokenizer);

      // Check if token is valid.
      
      if (tokenizer.ttype != StreamTokenizer.TT_WORD)
	errms(tokenizer,"not a valid value");
      
      // Check if value is missing.
      
      if  (tokenizer.sval.equals("?"))
	instance[i] = Instance.missingValue();
      else
	if (attribute(i).isNominal()) {
	  
	  // Check if value appears in header.
	  
	  index = attribute(i).indexOfValue(tokenizer.sval);
	  if (index == -1)
	    errms(tokenizer,"nominal value not declared in header");
	  instance[i] = (double)index;
	} else if (attribute(i).isNumeric()) {
	  
	  // Check if value is really a number.
	  
	  try{
	    instance[i] = Double.valueOf(tokenizer.sval).
	      doubleValue();
	  }catch (NumberFormatException e){
	    errms(tokenizer,"number expected");
	  }
	} else { 
	  attribute(i).forceAddValue(tokenizer.sval);
	  instance[i] = (double)attribute(i).numValues() - 1;
	}
    }
    if (flag)
      getLastToken(tokenizer);
    
    // Add instance to dataset
    
    add(new Instance(1, instance));
    
    return true;
  }

  /**
   * Reads and stores header of an ARFF file.
   * @param tokenizer the stream tokenizer
   * @exception IOException if the information is not read 
   * successfully
   */ 

  protected void readHeader(StreamTokenizer tokenizer) 
     throws IOException{
    
    String attributeName;
    FastVector attributeValues;
    int i;

    // Get name of relation.

    getFirstToken(tokenizer);
    if (tokenizer.ttype == StreamTokenizer.TT_EOF)
      errms(tokenizer,"premature end of file");
     if (tokenizer.sval.equalsIgnoreCase("@relation")){
      getNextToken(tokenizer);
      theRelationName = tokenizer.sval;
      getLastToken(tokenizer);
    }else
      errms(tokenizer,"keyword @relation expected");

    // Create vectors to hold information temporarily.

    theAttributes = new FastVector();
 
    // Get attribute declarations.

    getFirstToken(tokenizer);
    if (tokenizer.ttype == StreamTokenizer.TT_EOF)
      errms(tokenizer,"premature end of file");
    while (tokenizer.sval.equalsIgnoreCase("@attribute")){

      // Get attribute name.

      getNextToken(tokenizer);
      attributeName = tokenizer.sval;
      getNextToken(tokenizer);

      // Check if attribute is nominal.

      if (tokenizer.ttype == StreamTokenizer.TT_WORD){

	// Attribute is real, integer, or string.

	if (tokenizer.sval.equalsIgnoreCase("real") ||
	    tokenizer.sval.equalsIgnoreCase("integer") ||
	    tokenizer.sval.equalsIgnoreCase("numeric")) {
	  theAttributes.addElement(new Attribute(attributeName,
						 numAttributes()));
	  readTillEOL(tokenizer);
	} else if (tokenizer.sval.equalsIgnoreCase("string")) {
	  theAttributes.
	    addElement(new Attribute(attributeName, null,
				     numAttributes()));
	  readTillEOL(tokenizer);
	} else
	  errms(tokenizer,"no valid attribute type or invalid "+
		"enumeration");
      } else {

	// Attribute is nominal.

	attributeValues = new FastVector();
	tokenizer.pushBack();
	
	// Get values for nominal attribute.

	if (tokenizer.nextToken() != '{')
	  errms(tokenizer,"{ expected at beginning of enumeration");
	while (tokenizer.nextToken() != '}')
	  if (tokenizer.ttype == StreamTokenizer.TT_EOL)
	    errms(tokenizer,"} expected at end of enumeration");
	  else{
	    if (tokenizer.ttype == '\'')
	      tokenizer.sval = quote(tokenizer.sval);
	    attributeValues.addElement(tokenizer.sval);
	  }
	if (attributeValues.size() == 0)
	  errms(tokenizer,"no nominal values found");
	theAttributes.
	  addElement(new Attribute(attributeName, attributeValues,
				   numAttributes()));
      }
      getLastToken(tokenizer);
      getFirstToken(tokenizer);
      if (tokenizer.ttype == StreamTokenizer.TT_EOF)
	errms(tokenizer,"premature end of file");
    }

    // Check if data part follows. We can't easily check for EOL.
    
    if (!tokenizer.sval.equalsIgnoreCase("@data"))
      errms(tokenizer,"keyword @data expected");
    
    // Check if any attributes have been declared.
    
    if (theAttributes.size() == 0)
      errms(tokenizer,"no attributes declared");
  }
   
  // ================
  // Private methods.
  // ================

  /**
   * Copies instances from one ordered set to the end of another 
   * one.
   * @param source the source of the instances
   * @param from the position of the first instance to be copied
   * @param dest the destination for the instances
   * @param num the number of instances to be copied
   */
  
  private void copyInstances(int from, Instances dest, int num) {
    
    
    for (int i = 0; i < num; i++) 
      dest.add(instance(from + i));
  }
  
  /**
   * Throws error message with line number and last token read.
   * @param theMsg the error message to be thrown
   * @param tokenizer the stream tokenizer
   * @throws IOExcpetion containing the error message
   */

  private void errms(StreamTokenizer tokenizer, String theMsg) 
       throws IOException {
    
    throw new IOException(theMsg + ", read " + tokenizer.toString());
  }
  
  /**
   * Replaces the attribute information by a clone of
   * itself.
   */

  private void freshAttributeInfo() {

    theAttributes = (FastVector) theAttributes.copyElements();
  }

  /**
   * Gets next token and quotes it if necessary, skipping empty 
   * lines.
   * @param tokenizer the stream tokenizer
   * @exception IOException if reading the next token fails
   */

  private void getFirstToken(StreamTokenizer tokenizer) 
       throws IOException{

    while (tokenizer.nextToken() == StreamTokenizer.TT_EOL){};
    if (tokenizer.ttype == '\''){
      tokenizer.sval = quote(tokenizer.sval);
      tokenizer.ttype = StreamTokenizer.TT_WORD;
    }
  }
	
  /**
   * Gets token and checks if its end of line.
   * @param tokenizer the stream tokenizer
   * @exception IOException if it doesn't find an end of line
   */

  private void getLastToken(StreamTokenizer tokenizer) 
       throws IOException{

    if (tokenizer.nextToken() != StreamTokenizer.TT_EOL)
      errms(tokenizer,"end of line expected");
  }

  /**
   * Gets next token and quotes it if necessary, checking
   * for a premature and of line.
   * @param tokenizer the stream tokenizer
   * @exception IOException if it finds a premature end of line
   */

  private void getNextToken(StreamTokenizer tokenizer) 
       throws IOException{
    
    if (tokenizer.nextToken() == StreamTokenizer.TT_EOL)
      errms(tokenizer,"premature end of line");
    if (tokenizer.ttype == StreamTokenizer.TT_EOF)
      errms(tokenizer,"premature end of file");
    else if (tokenizer.ttype == '\''){
      tokenizer.sval = quote(tokenizer.sval);
      tokenizer.ttype = StreamTokenizer.TT_WORD;
    }
  }
	
  /**
   * Initializes the StreamTokenizer used for reading the ARFF file.
   * @param tokenizer the stream tokenizer
   */

  private void initTokenizer(StreamTokenizer tokenizer){

    tokenizer.resetSyntax();         
    tokenizer.whitespaceChars(0, ' ');    
    tokenizer.wordChars(' '+1,'\u00FF');
    tokenizer.whitespaceChars(',',',');
    tokenizer.commentChar('%');
    tokenizer.quoteChar('\'');
    tokenizer.ordinaryChar('{');
    tokenizer.ordinaryChar('}');
    tokenizer.eolIsSignificant(true);
  }
 
  /**
   * Returns string including all instances, their weights and
   * their indices in the original dataset.
   * @return description of instance and its weight as a string
   */

  private String instancesAndWeights(){

    StringBuffer text = new StringBuffer();

    for (int i = 0; i < numInstances(); i++) {
      text.append(instance(i) + " " + instance(i).weight());
      if (i < numInstances() - 1)
	text.append("\n");
    }
    
    return text.toString();
  }
  
  /**
   * Implements quicksort for sorting on a numeric attribute.
   * @param attIndex the attribute's index
   * @param lo0 the first index of the subset to be sorted
   * @param hi0 the last index of the subset to be sorted
   */

  private void quickSort(int attIndex, int lo0, int hi0) {
    
    int lo = lo0, hi = hi0;
    double mid, midPlus, midMinus;
    
    if (hi0 > lo0){
      
      // Arbitrarily establishing partition element as the 
      // midpoint of the array.
            
      mid = instance((lo0 + hi0) / 2).value(attIndex);
      midPlus = mid + 1e-6;
      midMinus = mid - 1e-6;

      // loop through the array until indices cross
      
      while(lo <= hi){
	
	// find the first element that is greater than or equal to 
	// the partition element starting from the left Index.
		
	while ((instance(lo).value(attIndex) < 
		midMinus) && (lo < hi0))
	  ++lo;
	
	// find an element that is smaller than or equal to
	// the partition element starting from the right Index.
	
	while ((instance(hi).value(attIndex)  > 
		midPlus) && (hi > lo0))
	  --hi;
	
	// if the indexes have not crossed, swap
	
	if(lo <= hi) {
	  swap(lo,hi);
	  ++lo;
	  --hi;
	}
      }
      
      // If the right index has not reached the left side of array
      // must now sort the left partition.
      
      if(lo0 < hi)
	quickSort(attIndex,lo0,hi);
      
      // If the left index has not reached the right side of array
      // must now sort the right partition.
      
      if(lo < hi0)
	quickSort(attIndex,lo,hi0);
      
    }
  }

  /**
   * Quotes a string using single quotes.
   * @param toQuote the string to be quoted
   * @return the quoted string
   */

  private String quote(String toQuote){

    return ("'".concat(toQuote)).concat("'");
  }

  /**
   * Reads and skips all tokens before next end of line token.
   * @param tokenizer the stream tokenizer
   */

  private void readTillEOL(StreamTokenizer tokenizer) 
       throws IOException{
    
    while (tokenizer.nextToken() != StreamTokenizer.TT_EOL) {};
    tokenizer.pushBack();
  }

  /**
   * Help function needed for stratification of set.
   */

  private void stratStep (int numFolds){
    
    FastVector newVec = new FastVector(theInstances.capacity());
    int start = 0, j;

    // create stratified batch

    while (newVec.size() < numInstances()) {
      j = start;
      while (j < numInstances()) {
	newVec.addElement(instance(j));
	j = j + numFolds;
      }
      start++;
    }
    theInstances = newVec;
  }
  
  /**
   * Swaps two instances in the ordered set.
   * @param i the first instance's index
   * @param j the second instance's index
   */
  
  private void swap(int i, int j){
    
    theInstances.swap(i, j);
  }

  // ============
  // Test method.
  // ============

  /**
   * Main method for testing this class.
   * @param argv should contain one element: the name of an ARFF 
   * file
   */

  public static void main(String [] argv){

    Instances instances, secondInstances, train, test, transformed,
      empty;
    Instance instance;
    Random random = new Random(2);
    Reader reader;
    int classIndex, start, num;
    double newWeight;
    FastVector testAtts, testVals;
    int i,j;
    
    try{
      if (argv.length > 2)
	throw (new Exception("Usage: Instances [<filename>] [<class index>]"));
      
      // Creating set of instances from scratch
      
      testVals = new FastVector(2);
      testVals.addElement("first_value");
      testVals.addElement("second_value");
      testAtts = new FastVector(2);
      testAtts.addElement(new Attribute("nominal_attribute", testVals));
      testAtts.addElement(new Attribute("numeric_attribute"));
      instances = new Instances("test_set", testAtts, 10);
      instances.add(new Instance(instances.numAttributes()));
      instances.add(new Instance(instances.numAttributes()));
      System.out.println("\nSet of instances created from scratch:\n");
      System.out.println(instances);
      
      if (argv.length >= 1) {
	reader = new FileReader(argv[0]);
	if (argv.length == 2)
	  classIndex = Integer.parseInt(argv[1]);
	else
	  classIndex = 0;
	
	// Read first five instances and print them
	
	System.out.println("\nFirst five instances from file:\n");
	instances = new Instances(reader, 1);
	instances.setClassIndex(classIndex - 1);
	i = 0;
	while ((i < 5) && (instances.readInstance(reader))) {
	  i++;
	}
	System.out.println(instances);
	
	reader = new FileReader(argv[0]);
	instances = new Instances(reader);
	instances.setClassIndex(classIndex - 1);
	
	// Prints header and instances.
	
	System.out.println("\nDataset:\n");
	System.out.println(instances);
	System.out.println("\nClass index: "+instances.classIndex());
      }
      
      // Test basic methods based on class index.
      
      System.out.println("\nClass name: "+instances.classAttribute().name());
      System.out.println("\nClass index: "+instances.classIndex());
      System.out.println("\nClass is nominal: " +
			 instances.classAttribute().isNominal());
      System.out.println("\nClass is numeric: " +
			 instances.classAttribute().isNumeric());
      System.out.println("\nClasses:\n");
      for (i = 0; i < instances.numClasses(); i++)
	System.out.println(instances.classAttribute().value(i));
      System.out.println("\nClass values and labels of instances:\n");
      for (i = 0; i < instances.numInstances(); i++) {
	System.out.print(instances.instance(i).classValue() + "\t");
	System.out.print(instances.instance(i).classLabel());
	if (instances.instance(i).classIsMissing())
	  System.out.println("\tis missing");
	else
	  System.out.println();
      }
      
      // Creates random weights.
      
      System.out.println("\nCreating random weights for instances.");
      for (i = 0; i < instances.numInstances(); i++)
	instances.instance(i).setWeight(random.nextDouble()); 
      
      // Prints all instances and their weights (and the sum of weights).
      
      System.out.println("\nInstances and their weights:\n");
      System.out.println(instances.instancesAndWeights());
      System.out.print("\nSum of weights: ");
      System.out.println(instances.sumOfWeights());
      
      // Inserts an attribute
      
      secondInstances = new Instances(instances);
      Attribute testAtt = new Attribute("Inserted");
      secondInstances.insertAttributeAt(testAtt, 0);
      System.out.println("\nSet with inserted attribute:\n");
      System.out.println(secondInstances);
      System.out.println("\nClass name: "+secondInstances.classAttribute().name());
      
      // Deletes the attribute
      
      secondInstances.deleteAttributeAt(0);
      System.out.println("\nSet with attribute deleted:\n");
      System.out.println(secondInstances);
      System.out.println("\nClass name: "+secondInstances.classAttribute().name());
      
      // Test if headers are equal
      
      System.out.println("\nHeaders equal: "+
			 instances.equalHeaders(secondInstances) + "\n");
      
      // Print data in internal format.
      
      System.out.println("\nData (internal values):\n");
      for (i = 0; i < instances.numInstances(); i++) {
	for (j = 0; j < instances.numAttributes(); j++)
	  if (instances.instance(i).isMissing(j))
	    System.out.print("? ");
	  else
	    System.out.print(instances.instance(i).value(j) + " ");
	System.out.println();
      }
      
      // Just print header

      System.out.println("\nEmpty dataset:\n");
      empty = new Instances(instances, 0);
      System.out.println(empty);
      System.out.println("\nClass name: "+empty.classAttribute().name());

      // Creates and prints subset of instances.

      start = instances.numInstances() / 4;
      num = instances.numInstances() / 2;
      System.out.print("\nSubset of dataset: ");
      System.out.println(num + " instances from " + (start + 1) 
			 + ". instance");
      secondInstances = new Instances(instances, start, num);
      System.out.println("\nClass name: "+secondInstances.classAttribute().name());

      // Prints all instances and their weights (and the sum of weights).

      System.out.println("\nInstances and their weights:\n");
      System.out.println(secondInstances.instancesAndWeights());
      System.out.print("\nSum of weights: ");
      System.out.println(secondInstances.sumOfWeights());
      
      // Creates and prints training and test sets for 3-fold
      // cross-validation.

      System.out.println("\nTrain and test folds for 3-fold CV:");
      if (instances.classAttribute().isNominal())
	instances.stratify(3);
      for (j = 0; j < 3; j++){
        train = instances.trainCV(3,j);
	test = instances.testCV(3,j);
                      
	// Prints all instances and their weights (and the sum of weights).

	System.out.println("\nTrain: ");
	System.out.println("\nInstances and their weights:\n");
	System.out.println(train.instancesAndWeights());
	System.out.print("\nSum of weights: ");
	System.out.println(train.sumOfWeights());
	System.out.println("\nClass name: "+train.classAttribute().name());
	System.out.println("\nTest: ");
	System.out.println("\nInstances and their weights:\n");
	System.out.println(test.instancesAndWeights());
	System.out.print("\nSum of weights: ");
	System.out.println(test.sumOfWeights());
	System.out.println("\nClass name: "+test.classAttribute().name());
      }

      // Randomizes instances and prints them.

      System.out.println("\nRandomized dataset:");
      instances.randomize(random);
      
      // Prints all instances and their weights (and the sum of weights).

      System.out.println("\nInstances and their weights:\n");
      System.out.println(instances.instancesAndWeights());
      System.out.print("\nSum of weights: ");
      System.out.println(instances.sumOfWeights());

      // Sorts instances according to first attribute and
      // prints them.

      System.out.print("\nInstances sorted according to first attribute:\n ");
      instances.sort(0);
        
      // Prints all instances and their weights (and the sum of weights).
      
      System.out.println("\nInstances and their weights:\n");
      System.out.println(instances.instancesAndWeights());
      System.out.print("\nSum of weights:");
      System.out.println(instances.sumOfWeights());
    } catch (Exception e) {
      e.printStackTrace(); 
    }
  }
}

     

