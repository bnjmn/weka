/*
 *    Instances.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package weka.core;

import java.io.*;
import java.util.*;

/**
 * Class for handling an ordered set of weighted instances. <p>
 *
 * Typical usage (code from the main() method of this class): <p>
 *
 * <code>
 * ... <br>
 * 
 * // Read all the instances in the file <br>
 * reader = new FileReader(filename); <br>
 * instances = new Instances(reader); <br><br>
 *
 * // Make the last attribute be the class <br>
 * instances.setClassIndex(instances.numAttributes() - 1); <br><br>
 * 
 * // Print header and instances. <br>
 * System.out.println("\nDataset:\n"); <br> 
 * System.out.println(instances); <br><br>
 *
 * ... <br>
 * </code><p>
 *
 * All methods that change a set of instances are safe, ie. a change
 * of a set of instances does not affect any other sets of
 * instances. All methods that change a datasets's attribute
 * information clone the dataset before it is changed.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.28 $ 
 */
public class Instances implements Serializable {
 
  /** The filename extension that should be used for arff files */
  public static String FILE_EXTENSION = ".arff";

  /** The dataset's name. */
  private String m_RelationName;         

  /** The attribute information. */
  private FastVector m_Attributes;

  /** The instances. */
  private FastVector m_Instances;

  /** The class attribute's index */
  private int m_ClassIndex;

  /** Buffer of values for sparse instance */
  private double[] m_ValueBuffer;

  /** Buffer of indices for sparse instance */
  private int[] m_IndicesBuffer;

  /**
   * Reads an ARFF file from a reader, and assigns a weight of
   * one to each instance. Lets the index of the class 
   * attribute be undefined (negative).
   *
   * @param reader the reader
   * @exception Exception if the ARFF file is not read 
   * successfully
   */
  public Instances(Reader reader) throws Exception {

    StreamTokenizer tokenizer;

    tokenizer = new StreamTokenizer(reader);
    initTokenizer(tokenizer);
    readHeader(tokenizer);
    m_ClassIndex = -1;
    m_Instances = new FastVector(1000);
    while (getInstance(tokenizer, true)) {};
    compactify();
  }
 
  /**
   * Reads the header of an ARFF file from a reader and 
   * reserves space for the given number of instances. Lets
   * the class index be undefined (negative).
   *
   * @param reader the reader
   * @param capacity the capacity
   * @exception IllegalArgumentException if the header is not read successfully
   * or the capacity is not positive or zero
   * @exception IOException if there is a problem with the reader.
   */
   public Instances(Reader reader, int capacity) throws IOException {

    StreamTokenizer tokenizer;

    if (capacity < 0) {
      throw new IllegalArgumentException("Capacity has to be positive!");
    }
    tokenizer = new StreamTokenizer(reader); 
    initTokenizer(tokenizer);
    readHeader(tokenizer);
    m_ClassIndex = -1;
    m_Instances = new FastVector(capacity);
  }

  /**
   * Constructor copying all instances and references to
   * the header information from the given set of instances.
   *
   * @param instances the set to be copied
   */
  public Instances(Instances dataset) {

    this(dataset, dataset.numInstances());

    dataset.copyInstances(0, this, dataset.numInstances());
  }

  /**
   * Constructor creating an empty set of instances. Copies references
   * to the header information from the given set of instances. Sets
   * the capacity of the set of instances to 0 if its negative.
   *
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
    m_ClassIndex = dataset.m_ClassIndex;
    m_RelationName = dataset.m_RelationName;
    m_Attributes = dataset.m_Attributes;
    m_Instances = new FastVector(capacity);
  }

  /**
   * Creates a new set of instances by copying a 
   * subset of another set.
   *
   * @param source the set of instances from which a subset 
   * is to be created
   * @param first the index of the first instance to be copied
   * @param toCopy the number of instances to be copied
   * @exception IllegalArgumentException if first and toCopy are out of range
   */
  public Instances(Instances source, int first, int toCopy) {
    
    this(source, toCopy);

    if ((first < 0) || ((first + toCopy) > source.numInstances())) {
      throw new IllegalArgumentException("Parameters first and/or toCopy out "+
                                         "of range");
    }
    source.copyInstances(first, this, toCopy);
  }

  /**
   * Creates an empty set of instances. Uses the given
   * attribute information. Sets the capacity of the set of 
   * instances to 0 if its negative. Given attribute information
   * must not be changed after this constructor has been used.
   *
   * @param name the name of the relation
   * @param attInfo the attribute information
   * @param capacity the capacity of the set
   */
  public Instances(String name, FastVector attInfo, int capacity) {

    m_RelationName = name;
    m_ClassIndex = -1;
    m_Attributes = attInfo;
    for (int i = 0; i < numAttributes(); i++) {
      attribute(i).setIndex(i);
    }
    m_Instances = new FastVector(capacity);
  }
 
  /**
   * Create a copy of the structure, but "cleanse" string types (i.e.
   * doesn't contain references to the strings seen in the past).
   *
   * @return a copy of the instance structure.
   */
  public Instances stringFreeStructure() {

    FastVector atts = (FastVector)m_Attributes.copy();
    for (int i = 0 ; i < atts.size(); i++) {
      Attribute att = (Attribute)atts.elementAt(i);
      if (att.type() == Attribute.STRING) {
        atts.setElementAt(new Attribute(att.name(), null), i);
      }
    }
    Instances result = new Instances(relationName(), atts, 0);
    result.m_ClassIndex = m_ClassIndex;
    return result;
  }

  /**
   * Adds one instance to the end of the set. 
   * Shallow copies instance before it is added. Increases the
   * size of the dataset if it is not large enough. Does not
   * check if the instance is compatible with the dataset.
   *
   * @param instance the instance to be added
   */
  public final void add(Instance instance) {

    Instance newInstance = (Instance)instance.copy();

    newInstance.setDataset(this);
    m_Instances.addElement(newInstance);
  }

  /**
   * Returns an attribute.
   *
   * @param index the attribute's index
   * @return the attribute at the given position
   */ 
  public final Attribute attribute(int index) {
    
    return (Attribute) m_Attributes.elementAt(index);
  }

  /**
   * Returns an attribute given its name. If there is more than
   * one attribute with the same name, it returns the first one.
   * Returns null if the attribute can't be found.
   *
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
   * Checks for string attributes in the dataset
   *
   * @return true if string attributes are present, false otherwise
   */
  public boolean checkForStringAttributes() {

    int i = 0;
   
    while (i < m_Attributes.size()) {
      if (attribute(i++).isString()) {
	return true;
      }
    }
    return false;
  }

  /**
   * Checks if the given instance is compatible
   * with this dataset. Only looks at the size of
   * the instance and the ranges of the values for 
   * nominal and string attributes.
   *
   * @return true if the instance is compatible with the dataset 
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
   * Returns the class attribute.
   *
   * @return the class attribute
   * @exception Exception if the class is not set
   */
  public final Attribute classAttribute() throws Exception {

    if (m_ClassIndex < 0) {
      throw new Exception("Class index is negative (not set)!");
    }
    return attribute(m_ClassIndex);
  }

  /**
   * Returns the class attribute's index. Returns negative number
   * if it's undefined.
   *
   * @return the class index as an integer
   */
  public final int classIndex() {
    
    return m_ClassIndex;
  }
 
  /**
   * Compactifies the set of instances. Decreases the capacity of
   * the set so that it matches the number of instances in the set.
   */
  public final void compactify() {

    m_Instances.trimToSize();
  }

  /**
   * Removes all instances from the set.
   */
  public final void delete() {
    
    m_Instances = new FastVector();
  }

  /**
   * Removes an instance at the given position from the set.
   *
   * @param index the instance's position
   */
  public final void delete(int index) {
    
    m_Instances.removeElementAt(index);
  }

  /**
   * Deletes an attribute at the given position 
   * (0 to numAttributes() - 1). A deep copy of the attribute
   * information is performed before the attribute is deleted.
   *
   * @param pos the attribute's position
   * @exception IllegalArgumentException if the given index is out of range or the
   * class attribute is being deleted
   */
  public void deleteAttributeAt(int position) {
	 
    if ((position < 0) || (position >= m_Attributes.size())) {
      throw new IllegalArgumentException("Index out of range");
    }
    if (position == m_ClassIndex) {
      throw new IllegalArgumentException("Can't delete class attribute");
    }
    freshAttributeInfo();
    if (m_ClassIndex > position) {
      m_ClassIndex--;
    }
    m_Attributes.removeElementAt(position);
    for (int i = position; i < m_Attributes.size(); i++) {
      Attribute current = (Attribute)m_Attributes.elementAt(i);
      current.setIndex(current.index() - 1);
    }
    for (int i = 0; i < numInstances(); i++) {
      instance(i).forceDeleteAttributeAt(position); 
    }
  }

  /**
   * Deletes all string attributes in the dataset. A deep copy of the attribute
   * information is performed before an attribute is deleted.
   *
   * @exception IllegalArgumentException if string attribute couldn't be 
   * successfully deleted (probably because it is the class attribute).
   */
  public void deleteStringAttributes() {

    int i = 0;
    while (i < m_Attributes.size()) {
      if (attribute(i).isString()) {
	deleteAttributeAt(i);
      } else {
	i++;
      }
    }
  }

  /**
   * Removes all instances with missing values for a particular
   * attribute from the dataset.
   *
   * @param attIndex the attribute's index
   */
  public final void deleteWithMissing(int attIndex) {

    FastVector newInstances = new FastVector(numInstances());

    for (int i = 0; i < numInstances(); i++) {
      if (!instance(i).isMissing(attIndex)) {
	newInstances.addElement(instance(i));
      }
    }
    m_Instances = newInstances;
  }

  /**
   * Removes all instances with missing values for a particular
   * attribute from the dataset.
   *
   * @param att the attribute
   */
  public final void deleteWithMissing(Attribute att) {

    deleteWithMissing(att.index());
  }

  /**
   * Removes all instances with a missing class value
   * from the dataset.
   *
   * @exception Exception if class is not set
   */
  public final void deleteWithMissingClass() throws Exception {

    if (m_ClassIndex < 0) {
      throw new Exception("Class index is negative (not set)!");
    }
    deleteWithMissing(m_ClassIndex);
  }

  /**
   * Returns an enumeration of all the attributes.
   *
   * @return enumeration of all the attributes.
   */
  public Enumeration enumerateAttributes() {

    return m_Attributes.elements(m_ClassIndex);
  }

  /**
   * Returns an enumeration of all instances in the dataset.
   *
   * @return enumeration of all instances in the dataset
   */
  public final Enumeration enumerateInstances() {

    return m_Instances.elements();
  }

  /**
   * Checks if two headers are equivalent.
   *
   * @param dataset another dataset
   * @return true if the header of the given dataset is equivalent 
   * to this header
   */
  public final boolean equalHeaders(Instances dataset){

    // Check class and all attributes
    if (m_ClassIndex != dataset.m_ClassIndex) {
      return false;
    }
    if (m_Attributes.size() != dataset.m_Attributes.size()) {
      return false;
    }
    for (int i = 0; i < m_Attributes.size(); i++) {
      if (!(attribute(i).equals(dataset.attribute(i)))) {
	return false;
      }
    }
    return true;
  }
 
  /**
   * Returns the first instance in the set.
   *
   * @return the first instance in the set
   */
  public final Instance firstInstance() {
    
    return (Instance)m_Instances.firstElement();
  }
 
  /**
   * Inserts an attribute at the given position (0 to 
   * numAttributes()) and sets all values to be missing.
   * Shallow copies the attribute before it is inserted, and performs
   * a deep copy of the existing attribute information.
   *
   * @param att the attribute to be inserted
   * @param pos the attribute's position
   * @exception IllegalArgumentException if the given index is out of range
   */
  public void insertAttributeAt(Attribute att, int position) 
       throws Exception {
	 
    if ((position < 0) ||
	(position > m_Attributes.size())) {
      throw new IllegalArgumentException("Index out of range");
    }
    att = (Attribute)att.copy();
    freshAttributeInfo();
    att.setIndex(position);
    m_Attributes.insertElementAt(att, position);
    for (int i = position + 1; i < m_Attributes.size(); i++) {
      Attribute current = (Attribute)m_Attributes.elementAt(i);
      current.setIndex(current.index() + 1);
    }
    for (int i = 0; i < numInstances(); i++) {
      instance(i).forceInsertAttributeAt(position);
    }
    if (m_ClassIndex >= position) {
      m_ClassIndex++;
    }
  }

  /**
   * Returns the instance at the given position.
   *
   * @param index the instance's index
   * @return the instance at the given position
   */
  public final Instance instance(int index) {

    return (Instance)m_Instances.elementAt(index);
  }

  /**
   * Returns the last instance in the set.
   *
   * @return the last instance in the set
   */
  public final Instance lastInstance() {
    
    return (Instance)m_Instances.lastElement();
  }

  /**
   * Returns the mean (mode) for a numeric (nominal) attribute as
   * a floating-point value. Returns 0 if the attribute is neither nominal nor 
   * numeric. If all values are missing it returns zero.
   *
   * @param attIndex the attribute's index
   * @return the mean or the mode
   */
  public final double meanOrMode(int attIndex) {

    double result, found;
    int [] counts;

    if (attribute(attIndex).isNumeric()) {
      result = found = 0;
      for (int j = 0; j < numInstances(); j++) {
	if (!instance(j).isMissing(attIndex)) {
	  found += instance(j).weight();
	  result += instance(j).weight()*instance(j).value(attIndex);
	}
      }
      if (Utils.eq(found, 0)) {
	return 0;
      } else {
	return result / found;
      }
    } else if (attribute(attIndex).isNominal()) {
      counts = new int[attribute(attIndex).numValues()];
      for (int j = 0; j < numInstances(); j++) {
	if (!instance(j).isMissing(attIndex)) {
	  counts[(int) instance(j).value(attIndex)] += instance(j).weight();
	}
      }
      return (double)Utils.maxIndex(counts);
    } else {
      return 0;
    }
  }

  /**
   * Returns the mean (mode) for a numeric (nominal) attribute as a
   * floating-point value.  Returns 0 if the attribute is neither
   * nominal nor numeric.  If all values are missing it returns zero.
   *
   * @param att the attribute
   * @return the mean or the mode 
   */
  public final double meanOrMode(Attribute att) {

    return meanOrMode(att.index());
  }

  /**
   * Returns the number of attributes.
   *
   * @return the number of attributes as an integer
   */
  public final int numAttributes() {

    return m_Attributes.size();
  }

  /**
   * Returns the number of class labels.
   *
   * @return the number of class labels as an integer if the class 
   * attribute is nominal, 1 otherwise.
   * @exception Exception if the class is not set
   */
  public final int numClasses() throws Exception {
    
    if (m_ClassIndex < 0) {
      throw new Exception("Class index is negative (not set)!");
    }
    if (!classAttribute().isNominal()) {
      return 1;
    } else {
      return classAttribute().numValues();
    }
  }

  /**
   * Returns the number of distinct values of a given attribute.
   * Returns the number of instances if the attribute is a
   * string attribute. The value 'missing' is not counted.
   *
   * @param attIndex the attribute
   * @return the number of distinct values of a given attribute
   */
  public final int numDistinctValues(int attIndex) {

    if (attribute(attIndex).isNumeric()) {
      double [] attVals = attributeToDoubleArray(attIndex);
      int [] sorted = Utils.sort(attVals);
      double prev = 0;
      int counter = 0;
      for (int i = 0; i < sorted.length; i++) {
	Instance current = instance(sorted[i]);
	if (current.isMissing(attIndex)) {
	  break;
	}
	if ((i == 0) || 
	    Utils.gr(current.value(attIndex), prev)) {
	  prev = current.value(attIndex);
	  counter++;
	}
      }
      return counter;
    } else {
      return attribute(attIndex).numValues();
    }
  }

  /**
   * Returns the number of distinct values of a given attribute.
   * Returns the number of instances if the attribute is a
   * string attribute. The value 'missing' is not counted.
   *
   * @param att the attribute
   * @return the number of distinct values of a given attribute
   */
  public final int numDistinctValues(Attribute att) {

    return numDistinctValues(att.index());
  }
  
  /**
   * Returns the number of instances in the dataset.
   *
   * @return the number of instances in the dataset as an integer
   */
  public final int numInstances() {

    return m_Instances.size();
  }

  /**
   * Shuffles the instances in the set so that they are ordered 
   * randomly.
   *
   * @param random a random number generator
   */
  public final void randomize(Random random) {

    for (int j = numInstances() - 1; j > 0; j--)
      swap(j,(int)(random.nextDouble()*(double)j));
  }

  /**
   * Reads a single instance from the reader and appends it
   * to the dataset.  Automatically expands the dataset if it
   * is not large enough to hold the instance. This method does
   * not check for carriage return at the end of the line.
   *
   * @param reader the reader 
   * @return false if end of file has been reached
   * @exception IOException if the information is not read 
   * successfully
   */ 
  public final boolean readInstance(Reader reader) 
       throws IOException {

    StreamTokenizer tokenizer = new StreamTokenizer(reader);
    
    initTokenizer(tokenizer);
    return getInstance(tokenizer, false);
  }    

  /**
   * Returns the relation's name.
   *
   * @return the relation's name as a string
   */
  public final String relationName() {

    return m_RelationName;
  }

  /**
   * Renames an attribute. This change only affects this
   * dataset.
   *
   * @param att the attribute's index
   * @param name the new name
   */
  public final void renameAttribute(int att, String name) {

    Attribute newAtt = attribute(att).copy(name);
    FastVector newVec = new FastVector(numAttributes());

    for (int i = 0; i < numAttributes(); i++) {
      if (i == att) {
	newVec.addElement(newAtt);
      } else {
	newVec.addElement(attribute(i));
      }
    }
    m_Attributes = newVec;
  }

  /**
   * Renames an attribute. This change only affects this
   * dataset.
   *
   * @param att the attribute
   * @param name the new name
   */
  public final void renameAttribute(Attribute att, String name) {

    renameAttribute(att.index(), name);
  }

  /**
   * Renames the value of a nominal (or string) attribute value. This
   * change only affects this dataset.
   *
   * @param att the attribute's index
   * @param val the value's index
   * @param name the new name 
   * @exception Exception if renaming fails
   */
  public final void renameAttributeValue(int att, int val, String name) 
    throws Exception {

    Attribute newAtt = (Attribute)attribute(att).copy();
    FastVector newVec = new FastVector(numAttributes());

    newAtt.setValue(val, name);
    for (int i = 0; i < numAttributes(); i++) {
      if (i == att) {
	newVec.addElement(newAtt);
      } else {
	newVec.addElement(attribute(i));
      }
    }
    m_Attributes = newVec;
  }

  /**
   * Renames the value of a nominal (or string) attribute value. This
   * change only affects this dataset.
   *
   * @param att the attribute
   * @param val the value
   * @param name the new name
   * @exception Exception if renaming fails
   */
  public final void renameAttributeValue(Attribute att, String val, String name) 
    throws Exception {

    renameAttributeValue(att.index(), att.indexOfValue(val), name);
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
   * with replacement according to the current instance weights. The
   * weights of the instances in the new dataset are set to one.
   *
   * @param random a random number generator
   * @return the new dataset
   * @exception Exception if something goes wrong
   */
  public final Instances resampleWithWeights(Random random) 
    throws Exception {

    double [] weights = new double[numInstances()];
    boolean foundOne = false;
    for (int i = 0; i < weights.length; i++) {
      weights[i] = instance(i).weight();
      if (!Utils.eq(weights[i], weights[0])) {
        foundOne = true;
      }
    }
    if (foundOne) {
      return resampleWithWeights(random, weights);
    } else {
      return new Instances(this);
    }
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
   * @exception IllegalArgumentException if the weights array is of the wrong
   * length or contains negative weights.
   */
  public final Instances resampleWithWeights(Random random, 
					     double[] weights) {

    if (weights.length != numInstances()) {
      throw new IllegalArgumentException("weights.length != numInstances.");
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
	throw new IllegalArgumentException("Weights have to be positive.");
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
   * Sets the class attribute.
   *
   * @param att attribute to be the class
   */
  public final void setClass(Attribute att) {

    m_ClassIndex = att.index();
  }

  /** 
   * Sets the class index of the set.
   * If the class index is negative there is assumed to be no class.
   * (ie. it is undefined)
   *
   * @param classIndex the new class index
   * @exception IllegalArgumentException if the class index is too big or < 0
   */
  public final void setClassIndex(int classIndex) {

    if (classIndex >= numAttributes() || (classIndex < 0)) {
      throw new IllegalArgumentException("Class index to large!");
    }
    m_ClassIndex = classIndex;
  }

  /**
   * Sets the relation's name.
   *
   * @param newName the new relation name.
   */
  public final void setRelationName(String newName) {
    
    m_RelationName = newName;
  }

  /**
   * Sorts the instances based on an attribute. For numeric attributes, 
   * instances are sorted in ascending order. For nominal attributes, 
   * instances are sorted based on the attribute label ordering 
   * specified in the header. Instances with missing values for the 
   * attribute are placed at the end of the dataset.
   *
   * @param attIndex the attribute's index
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
  public final void sort(Attribute att) {

    sort(att.index());
  }

  /**
   * Stratifies a set of instances according to its class values 
   * if the class attribute is nominal (so that afterwards a 
   * stratified cross-validation can be performed).
   *
   * @param numFolds the number of folds in the cross-validation
   * @exception Exception if the class is not set
   */
  public final void stratify(int numFolds) throws Exception {
    
    if (numFolds <= 0) {
      throw new IllegalArgumentException("Number of folds must be greater than 1");
    }
    if (m_ClassIndex < 0) {
      throw new Exception("Class index is negative (not set)!");
    }
    if (classAttribute().isNominal()) {

      // sort by class
      int index = 1;
      while (index < numInstances()) {
	Instance instance1 = instance(index - 1);
	for (int j = index; j < numInstances(); j++) {
	  Instance instance2 = instance(j);
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

    for (int i = 0; i < numInstances(); i++) {
      sum += instance(i).weight();
    }
    return sum;
  }

  /**
   * Creates the test set for one fold of a cross-validation on 
   * the dataset.
   *
   * @param numFolds the number of folds in the cross-validation. Must
   * be greater than 1.
   * @param numFold 0 for the first fold, 1 for the second, ...
   * @return the test set as a set of weighted instances
   * @exception IllegalArgumentException if the number of folds is less than 2
   * or greater than the number of instances.
   */
  public Instances testCV(int numFolds, int numFold) {

    int numInstForFold, first, offset;
    Instances test;
    
    if (numFolds < 2) {
      throw new IllegalArgumentException("Number of folds must be at least 2!");
    }
    if (numFolds > numInstances()) {
      throw new IllegalArgumentException("Can't have more folds than instances!");
    }
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
   * Returns the dataset as a string in ARFF format. Strings
   * are quoted if they contain whitespace characters, or if they
   * are a question mark.
   *
   * @return the dataset in ARFF format as a string
   */
  public final String toString() {
    
    StringBuffer text = new StringBuffer();
    
    text.append("@relation " + Utils.quote(m_RelationName) + "\n\n");
    for (int i = 0; i < numAttributes(); i++) {
      text.append(attribute(i) + "\n");
    }
    text.append("\n@data\n");
    for (int i = 0; i < numInstances(); i++) {
      text.append(instance(i));
      if (i < numInstances() - 1) {
	text.append('\n');
      }
    }
    return text.toString();
  }

  /**
   * Creates the training set for one fold of a cross-validation 
   * on the dataset.
   *
   * @param numFolds the number of folds in the cross-validation. Must
   * be greater than 1.
   * @param numFold 0 for the first fold, 1 for the second, ...
   * @return the training set as a set of weighted 
   * instances
   * @exception IllegalArgumentException if the number of folds is less than 2
   * or greater than the number of instances.
   */
  public Instances trainCV(int numFolds, int numFold) 
       throws Exception {

    int numInstForFold, first, offset;
    Instances train;
 
    if (numFolds < 2) {
      throw new IllegalArgumentException("Number of folds must be at least 2!");
    }
    if (numFolds > numInstances()) {
      throw new IllegalArgumentException("Can't have more folds than instances!");
    }
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
   *
   * @param attIndex the numeric attribute
   * @return the variance if the attribute is numeric
   * @exception IllegalArgumentException if the attribute is not numeric
   */
  public final double variance(int attIndex) {
  
    double sum = 0, sumSquared = 0, sumOfWeights = 0;

    if (!attribute(attIndex).isNumeric()) {
      throw new IllegalArgumentException("Can't compute variance because attribute is " +
			  "not numeric!");
    }
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
    if (Utils.smOrEq(sumOfWeights, 1)) {
      return 0;
    }
    return (sumSquared - (sum * sum / sumOfWeights)) / 
      (sumOfWeights - 1);
  }

  /**
   * Computes the variance for a numeric attribute.
   *
   * @param att the numeric attribute
   * @return the variance if the attribute is numeric
   * @exception Exception if the attribute is not numeric
   */
  public final double variance(Attribute att) throws Exception {
    
    return variance(att.index());
  }
  
  /**
   * Calculates summary statistics on the values that appear in this
   * set of instances for a specified attribute.
   *
   * @param index the index of the attribute to summarize.
   * @return an AttributeStats object with it's fields calculated.
   */
  public AttributeStats attributeStats(int index) {

    AttributeStats result = new AttributeStats();
    if (attribute(index).isNominal()) {
      result.nominalCounts = new int [attribute(index).numValues()];
    }
    if (attribute(index).isNumeric()) {
      result.numericStats = new weka.experiment.Stats();
    }
    result.totalCount = numInstances();

    double [] attVals = attributeToDoubleArray(index);
    int [] sorted = Utils.sort(attVals);
    int currentCount = 0;
    double prev = Instance.missingValue();
    for (int j = 0; j < numInstances(); j++) {
      Instance current = instance(sorted[j]);
      if (current.isMissing(index)) {
	result.missingCount = numInstances() - j;
	break;
      }
      if (Utils.eq(current.value(index), prev)) {
	currentCount++;
      } else {
	result.addDistinct(prev, currentCount);
	currentCount = 1;
	prev = current.value(index);
      }
    }
    result.addDistinct(prev, currentCount);
    result.distinctCount--; // So we don't count "missing" as a value 
    return result;
  }
  
  /**
   * Gets the value of all instances in this dataset for a particular
   * attribute. Useful in conjunction with Utils.sort to allow iterating
   * through the dataset in sorted order for some attribute.
   *
   * @param index the index of the attribute.
   * @return an array containing the value of the desired attribute for
   * each instance in the dataset. 
   */
  public double [] attributeToDoubleArray(int index) {

    double [] result = new double[numInstances()];
    for (int i = 0; i < result.length; i++) {
      result[i] = instance(i).value(index);
    }
    return result;
  }

  /**
   * Generates a string summarizing the set of instances. Gives a breakdown
   * for each attribute indicating the number of missing/discrete/unique
   * values and other information.
   *
   * @return a string summarizing the dataset
   */
  public String toSummaryString() {

    StringBuffer result = new StringBuffer();
    result.append("Relation Name:  ").append(relationName()).append('\n');
    result.append("Num Instances:  ").append(numInstances()).append('\n');
    result.append("Num Attributes: ").append(numAttributes()).append('\n');
    result.append('\n');

    result.append(Utils.padLeft("", 5)).append(Utils.padRight("Name", 25));
    result.append(Utils.padLeft("Type", 5)).append(Utils.padLeft("Nom", 5));
    result.append(Utils.padLeft("Int", 5)).append(Utils.padLeft("Real", 5));
    result.append(Utils.padLeft("Missing", 12));
    result.append(Utils.padLeft("Unique", 12));
    result.append(Utils.padLeft("Dist", 6)).append('\n');
    for (int i = 0; i < numAttributes(); i++) {
      Attribute a = attribute(i);
      AttributeStats as = attributeStats(i);
      result.append(Utils.padLeft("" + (i + 1), 4)).append(' ');
      result.append(Utils.padRight(a.name(), 25)).append(' ');
      long percent;
      switch (a.type()) {
      case Attribute.NOMINAL:
	result.append(Utils.padLeft("Nom", 4)).append(' ');
	percent = Math.round(100.0 * as.intCount / as.totalCount);
	result.append(Utils.padLeft("" + percent, 3)).append("% ");
	result.append(Utils.padLeft("" + 0, 3)).append("% ");
	percent = Math.round(100.0 * as.realCount / as.totalCount);
	result.append(Utils.padLeft("" + percent, 3)).append("% ");
	break;
      case Attribute.NUMERIC:
	result.append(Utils.padLeft("Num", 4)).append(' ');
	result.append(Utils.padLeft("" + 0, 3)).append("% ");
	percent = Math.round(100.0 * as.intCount / as.totalCount);
	result.append(Utils.padLeft("" + percent, 3)).append("% ");
	percent = Math.round(100.0 * as.realCount / as.totalCount);
	result.append(Utils.padLeft("" + percent, 3)).append("% ");
	break;
      case Attribute.STRING:
	result.append(Utils.padLeft("Str", 4)).append(' ');
	percent = Math.round(100.0 * as.intCount / as.totalCount);
	result.append(Utils.padLeft("" + percent, 3)).append("% ");
	result.append(Utils.padLeft("" + 0, 3)).append("% ");
	percent = Math.round(100.0 * as.realCount / as.totalCount);
	result.append(Utils.padLeft("" + percent, 3)).append("% ");
	break;
      default:
	result.append(Utils.padLeft("???", 4)).append(' ');
	result.append(Utils.padLeft("" + 0, 3)).append("% ");
	percent = Math.round(100.0 * as.intCount / as.totalCount);
	result.append(Utils.padLeft("" + percent, 3)).append("% ");
	percent = Math.round(100.0 * as.realCount / as.totalCount);
	result.append(Utils.padLeft("" + percent, 3)).append("% ");
	break;
      }
      result.append(Utils.padLeft("" + as.missingCount, 5)).append(" /");
      percent = Math.round(100.0 * as.missingCount / as.totalCount);
      result.append(Utils.padLeft("" + percent, 3)).append("% ");
      result.append(Utils.padLeft("" + as.uniqueCount, 5)).append(" /");
      percent = Math.round(100.0 * as.uniqueCount / as.totalCount);
      result.append(Utils.padLeft("" + percent, 3)).append("% ");
      result.append(Utils.padLeft("" + as.distinctCount, 5)).append(' ');
      result.append('\n');
    }
    return result.toString();
  }
  
  /**
   * Reads a single instance using the tokenizer and appends it
   * to the dataset. Automatically expands the dataset if it
   * is not large enough to hold the instance.
   *
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
    
    // Check if any attributes have been declared.
    if (m_Attributes.size() == 0) {
      errms(tokenizer,"no header information available");
    }

    // Check if end of file reached.
    getFirstToken(tokenizer);
    if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
      return false;
    }
    
    // Parse instance
    if (tokenizer.ttype == '{') {
      return getInstanceSparse(tokenizer, flag);
    } else {
      return getInstanceFull(tokenizer, flag);
    }
  }

  /**
   * Reads a single instance using the tokenizer and appends it
   * to the dataset. Automatically expands the dataset if it
   * is not large enough to hold the instance.
   *
   * @param tokenizer the tokenizer to be used
   * @param flag if method should test for carriage return after 
   * each instance
   * @return false if end of file has been reached
   * @exception IOException if the information is not read 
   * successfully
   */ 
  protected boolean getInstanceSparse(StreamTokenizer tokenizer, 
				      boolean flag) 
       throws IOException {

    int valIndex, numValues = 0, maxIndex = -1;
    
    // Get values
    do {
      
      // Get index
      getIndex(tokenizer);
      if (tokenizer.ttype == '}') {
	break;
      }
       
      // Is index valid?
      try{
	m_IndicesBuffer[numValues] = Integer.valueOf(tokenizer.sval).intValue();
      } catch (NumberFormatException e) {
	errms(tokenizer,"index number expected");
      }
      if (m_IndicesBuffer[numValues] <= maxIndex) {
	errms(tokenizer,"indices have to be ordered");
      }
      if ((m_IndicesBuffer[numValues] < 0) || 
	  (m_IndicesBuffer[numValues] >= numAttributes())) {
	errms(tokenizer,"index out of bounds");
      }
      maxIndex = m_IndicesBuffer[numValues];

      // Get value;
      getNextToken(tokenizer);

      // Check if value is missing.
      if  (tokenizer.ttype == '?') {
	m_ValueBuffer[numValues] = Instance.missingValue();
      } else {

	// Check if token is valid.
	if (tokenizer.ttype != StreamTokenizer.TT_WORD) {
	  errms(tokenizer,"not a valid value");
	}
	if (attribute(m_IndicesBuffer[numValues]).isNominal()) {
	  
	  // Check if value appears in header.
	  valIndex = 
	    attribute(m_IndicesBuffer[numValues]).indexOfValue(tokenizer.sval);
	  if (valIndex == -1) {
	    errms(tokenizer,"nominal value not declared in header");
	  }
	  m_ValueBuffer[numValues] = (double)valIndex;
	} else if (attribute(m_IndicesBuffer[numValues]).isNumeric()) {
	  
	  // Check if value is really a number.
	  try{
	    m_ValueBuffer[numValues] = Double.valueOf(tokenizer.sval).
	      doubleValue();
	  } catch (NumberFormatException e) {
	    errms(tokenizer,"number expected");
	  }
	} else { 
	  m_ValueBuffer[numValues] = 
	    attribute(m_IndicesBuffer[numValues]).addStringValue(tokenizer.sval);
	}
      }
      numValues++;
    } while (true);
    if (flag) {
      getLastToken(tokenizer,true);
    }
      
    // Add instance to dataset
    double[] tempValues = new double[numValues];
    int[] tempIndices = new int[numValues];
    System.arraycopy(m_ValueBuffer, 0, tempValues, 0, numValues);
    System.arraycopy(m_IndicesBuffer, 0, tempIndices, 0, numValues);
    add(new SparseInstance(1, tempValues, tempIndices, numAttributes()));
    return true;
  }

  /**
   * Reads a single instance using the tokenizer and appends it
   * to the dataset. Automatically expands the dataset if it
   * is not large enough to hold the instance.
   *
   * @param tokenizer the tokenizer to be used
   * @param flag if method should test for carriage return after 
   * each instance
   * @return false if end of file has been reached
   * @exception IOException if the information is not read 
   * successfully
   */ 
  protected boolean getInstanceFull(StreamTokenizer tokenizer, 
				    boolean flag) 
       throws IOException {

    double[] instance = new double[numAttributes()];
    int index;
    
    // Get values for all attributes.
    for (int i = 0; i < numAttributes(); i++){
      
      // Get next token
      if (i > 0) {
	getNextToken(tokenizer);
      }
            
      // Check if value is missing.
      if  (tokenizer.ttype == '?') {
	instance[i] = Instance.missingValue();
      } else {

	// Check if token is valid.
	if (tokenizer.ttype != StreamTokenizer.TT_WORD) {
	  errms(tokenizer,"not a valid value");
	}
	if (attribute(i).isNominal()) {
	  
	  // Check if value appears in header.
	  index = attribute(i).indexOfValue(tokenizer.sval);
	  if (index == -1) {
	    errms(tokenizer,"nominal value not declared in header");
	  }
	  instance[i] = (double)index;
	} else if (attribute(i).isNumeric()) {
	  
	  // Check if value is really a number.
	  try{
	    instance[i] = Double.valueOf(tokenizer.sval).
	      doubleValue();
	  } catch (NumberFormatException e) {
	    errms(tokenizer,"number expected");
	  }
	} else { 
	  instance[i] = attribute(i).addStringValue(tokenizer.sval);
	}
      }
    }
    if (flag) {
      getLastToken(tokenizer,true);
    }
      
    // Add instance to dataset
    add(new Instance(1, instance));
    return true;
  }

  /**
   * Reads and stores header of an ARFF file.
   *
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
    if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
      errms(tokenizer,"premature end of file");
    }
    if (tokenizer.sval.equalsIgnoreCase("@relation")){
      getNextToken(tokenizer);
      m_RelationName = tokenizer.sval;
      getLastToken(tokenizer,false);
    } else {
      errms(tokenizer,"keyword @relation expected");
    }

    // Create vectors to hold information temporarily.
    m_Attributes = new FastVector();
 
    // Get attribute declarations.
    getFirstToken(tokenizer);
    if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
      errms(tokenizer,"premature end of file");
    }
    while (tokenizer.sval.equalsIgnoreCase("@attribute")) {

      // Get attribute name.
      getNextToken(tokenizer);
      attributeName = tokenizer.sval;
      getNextToken(tokenizer);

      // Check if attribute is nominal.
      if (tokenizer.ttype == StreamTokenizer.TT_WORD) {

	// Attribute is real, integer, or string.
	if (tokenizer.sval.equalsIgnoreCase("real") ||
	    tokenizer.sval.equalsIgnoreCase("integer") ||
	    tokenizer.sval.equalsIgnoreCase("numeric")) {
	  m_Attributes.addElement(new Attribute(attributeName,
						 numAttributes()));
	  readTillEOL(tokenizer);
	} else if (tokenizer.sval.equalsIgnoreCase("string")) {
	  m_Attributes.
	    addElement(new Attribute(attributeName, null,
				     numAttributes()));
	  readTillEOL(tokenizer);
	} else {
	  errms(tokenizer,"no valid attribute type or invalid "+
		"enumeration");
	}
      } else {

	// Attribute is nominal.
	attributeValues = new FastVector();
	tokenizer.pushBack();
	
	// Get values for nominal attribute.
	if (tokenizer.nextToken() != '{') {
	  errms(tokenizer,"{ expected at beginning of enumeration");
	}
	while (tokenizer.nextToken() != '}') {
	  if (tokenizer.ttype == StreamTokenizer.TT_EOL) {
	    errms(tokenizer,"} expected at end of enumeration");
	  } else {
	    attributeValues.addElement(tokenizer.sval);
	  }
	}
	if (attributeValues.size() == 0) {
	  errms(tokenizer,"no nominal values found");
	}
	m_Attributes.
	  addElement(new Attribute(attributeName, attributeValues,
				   numAttributes()));
      }
      getLastToken(tokenizer,false);
      getFirstToken(tokenizer);
      if (tokenizer.ttype == StreamTokenizer.TT_EOF)
	errms(tokenizer,"premature end of file");
    }

    // Check if data part follows. We can't easily check for EOL.
    if (!tokenizer.sval.equalsIgnoreCase("@data")) {
      errms(tokenizer,"keyword @data expected");
    }
    
    // Check if any attributes have been declared.
    if (m_Attributes.size() == 0) {
      errms(tokenizer,"no attributes declared");
    }

    // Allocate buffers in case sparse instances have to be read
    m_ValueBuffer = new double[numAttributes()];
    m_IndicesBuffer = new int[numAttributes()];
  }

  /**
   * Copies instances from one set to the end of another 
   * one.
   *
   * @param source the source of the instances
   * @param from the position of the first instance to be copied
   * @param dest the destination for the instances
   * @param num the number of instances to be copied
   */
  private void copyInstances(int from, Instances dest, int num) {
    
    for (int i = 0; i < num; i++) {
      dest.add(instance(from + i));
    }
  }
  
  /**
   * Throws error message with line number and last token read.
   *
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

    m_Attributes = (FastVector) m_Attributes.copyElements();
  }

  /**
   * Gets next token, skipping empty lines.
   *
   * @param tokenizer the stream tokenizer
   * @exception IOException if reading the next token fails
   */
  private void getFirstToken(StreamTokenizer tokenizer) 
    throws IOException{
    
    while (tokenizer.nextToken() == StreamTokenizer.TT_EOL){};
    if ((tokenizer.ttype == '\'') ||
	(tokenizer.ttype == '"')) {
      tokenizer.ttype = StreamTokenizer.TT_WORD;
    } else if ((tokenizer.ttype == StreamTokenizer.TT_WORD) &&
	       (tokenizer.sval.equals("?"))){
      tokenizer.ttype = '?';
    }
  }

  /**
   * Gets index, checking for a premature and of line.
   *
   * @param tokenizer the stream tokenizer
   * @exception IOException if it finds a premature end of line
   */
  private void getIndex(StreamTokenizer tokenizer) throws IOException{
    
    if (tokenizer.nextToken() == StreamTokenizer.TT_EOL) {
      errms(tokenizer,"premature end of line");
    }
    if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
      errms(tokenizer,"premature end of file");
    }
  }
  
  /**
   * Gets token and checks if its end of line.
   *
   * @param tokenizer the stream tokenizer
   * @exception IOException if it doesn't find an end of line
   */
  private void getLastToken(StreamTokenizer tokenizer, boolean endOfFileOk) 
       throws IOException{

    if ((tokenizer.nextToken() != StreamTokenizer.TT_EOL) &&
	((tokenizer.nextToken() != StreamTokenizer.TT_EOF) || !endOfFileOk)) {
      errms(tokenizer,"end of line expected");
    }
  }

  /**
   * Gets next token, checking for a premature and of line.
   *
   * @param tokenizer the stream tokenizer
   * @exception IOException if it finds a premature end of line
   */
  private void getNextToken(StreamTokenizer tokenizer) 
       throws IOException{
    
    if (tokenizer.nextToken() == StreamTokenizer.TT_EOL) {
      errms(tokenizer,"premature end of line");
    }
    if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
      errms(tokenizer,"premature end of file");
    } else if ((tokenizer.ttype == '\'') ||
	       (tokenizer.ttype == '"')) {
      tokenizer.ttype = StreamTokenizer.TT_WORD;
    } else if ((tokenizer.ttype == StreamTokenizer.TT_WORD) &&
	       (tokenizer.sval.equals("?"))){
      tokenizer.ttype = '?';
    }
  }
	
  /**
   * Initializes the StreamTokenizer used for reading the ARFF file.
   *
   * @param tokenizer the stream tokenizer
   */
  private void initTokenizer(StreamTokenizer tokenizer){

    tokenizer.resetSyntax();         
    tokenizer.whitespaceChars(0, ' ');    
    tokenizer.wordChars(' '+1,'\u00FF');
    tokenizer.whitespaceChars(',',',');
    tokenizer.commentChar('%');
    tokenizer.quoteChar('"');
    tokenizer.quoteChar('\'');
    tokenizer.ordinaryChar('{');
    tokenizer.ordinaryChar('}');
    tokenizer.eolIsSignificant(true);
  }
 
  /**
   * Returns string including all instances, their weights and
   * their indices in the original dataset.
   *
   * @return description of instance and its weight as a string
   */
  private String instancesAndWeights(){

    StringBuffer text = new StringBuffer();

    for (int i = 0; i < numInstances(); i++) {
      text.append(instance(i) + " " + instance(i).weight());
      if (i < numInstances() - 1) {
	text.append("\n");
      }
    }
    return text.toString();
  }
  
  /**
   * Implements quicksort.
   *
   * @param attIndex the attribute's index
   * @param lo0 the first index of the subset to be sorted
   * @param hi0 the last index of the subset to be sorted
   */
  private void quickSort(int attIndex, int lo0, int hi0) {
    
    int lo = lo0, hi = hi0;
    double mid, midPlus, midMinus;
    
    if (hi0 > lo0) {
      
      // Arbitrarily establishing partition element as the 
      // midpoint of the array.
      mid = instance((lo0 + hi0) / 2).value(attIndex);
      midPlus = mid + 1e-6;
      midMinus = mid - 1e-6;

      // loop through the array until indices cross
      while(lo <= hi) {
	
	// find the first element that is greater than or equal to 
	// the partition element starting from the left Index.
	while ((instance(lo).value(attIndex) < 
		midMinus) && (lo < hi0)) {
	  ++lo;
	}
	
	// find an element that is smaller than or equal to
	// the partition element starting from the right Index.
	while ((instance(hi).value(attIndex)  > 
		midPlus) && (hi > lo0)) {
	  --hi;
	}
	
	// if the indexes have not crossed, swap
	if(lo <= hi) {
	  swap(lo,hi);
	  ++lo;
	  --hi;
	}
      }
      
      // If the right index has not reached the left side of array
      // must now sort the left partition.
      if(lo0 < hi) {
	quickSort(attIndex,lo0,hi);
      }
      
      // If the left index has not reached the right side of array
      // must now sort the right partition.
      if(lo < hi0) {
	quickSort(attIndex,lo,hi0);
      }
    }
  }

  /**
   * Reads and skips all tokens before next end of line token.
   *
   * @param tokenizer the stream tokenizer
   */
  private void readTillEOL(StreamTokenizer tokenizer) 
       throws IOException{
    
    while (tokenizer.nextToken() != StreamTokenizer.TT_EOL) {};
    tokenizer.pushBack();
  }

  /**
   * Help function needed for stratification of set.
   *
   * @param numFolds the number of folds for the stratification
   */
  private void stratStep (int numFolds){
    
    FastVector newVec = new FastVector(m_Instances.capacity());
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
    m_Instances = newVec;
  }
  
  /**
   * Swaps two instances in the set.
   *
   * @param i the first instance's index
   * @param j the second instance's index
   */
  private void swap(int i, int j){
    
    m_Instances.swap(i, j);
  }

  /**
   * Merges two sets of Instances together. The resulting set will have
   * all the attributes of the first set plus all the attributes of the 
   * second set. The number of instances in both sets must be the same.
   *
   * @param first the first set of Instances
   * @param second the second set of Instances
   * @return the merged set of Instances
   * @exception IllegalArgumentException if the datasets are not the same size
   */
  public static Instances mergeInstances(Instances first, Instances second)
    throws Exception {

    if (first.numInstances() != second.numInstances()) {
      throw new IllegalArgumentException("Instance sets must be of the same size");
    }

    // Create the vector of merged attributes
    FastVector newAttributes = new FastVector();
    for (int i = 0; i < first.numAttributes(); i++) {
      newAttributes.addElement(first.attribute(i));
    }
    for (int i = 0; i < second.numAttributes(); i++) {
      newAttributes.addElement(second.attribute(i));
    }
    
    // Create the set of Instances
    Instances merged = new Instances(first.relationName() + '_'
				     + second.relationName(), 
				     newAttributes, 
				     first.numInstances());
    // Merge each instance
    for (int i = 0; i < first.numInstances(); i++) {
      merged.add(first.instance(i).mergeInstance(second.instance(i)));
    }
    return merged;
  }

  /**
   * Method for testing this class.
   *
   * @param argv should contain one element: the name of an ARFF file
   */
  public static void test(String [] argv) {

    Instances instances, secondInstances, train, test, transformed, empty;
    Instance instance;
    Random random = new Random(2);
    Reader reader;
    int start, num;
    double newWeight;
    FastVector testAtts, testVals;
    int i,j;
    
    try{
      if (argv.length > 1) {
	throw (new Exception("Usage: Instances [<filename>]"));
      }
      
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
      instances.add(new Instance(instances.numAttributes()));
      instances.setClassIndex(0);
      System.out.println("\nSet of instances created from scratch:\n");
      System.out.println(instances);
      
      if (argv.length == 1) {
	String filename = argv[0];
	reader = new FileReader(filename);
	
	// Read first five instances and print them
	System.out.println("\nFirst five instances from file:\n");
	instances = new Instances(reader, 1);
	instances.setClassIndex(instances.numAttributes() - 1);
	i = 0;
	while ((i < 5) && (instances.readInstance(reader))) {
	  i++;
	}
	System.out.println(instances);

	// Read all the instances in the file
	reader = new FileReader(filename);
	instances = new Instances(reader);

	// Make the last attribute be the class 
	instances.setClassIndex(instances.numAttributes() - 1);
	
	// Print header and instances.
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
      for (i = 0; i < instances.numClasses(); i++) {
	System.out.println(instances.classAttribute().value(i));
      }
      System.out.println("\nClass values and labels of instances:\n");
      for (i = 0; i < instances.numInstances(); i++) {
	Instance inst = instances.instance(i);
	System.out.print(inst.classValue() + "\t");
	System.out.print(inst.toString(inst.classIndex()));
	if (instances.instance(i).classIsMissing()) {
	  System.out.println("\tis missing");
	} else {
	  System.out.println();
	}
      }
      
      // Create random weights.
      System.out.println("\nCreating random weights for instances.");
      for (i = 0; i < instances.numInstances(); i++) {
	instances.instance(i).setWeight(random.nextDouble()); 
      }
      
      // Print all instances and their weights (and the sum of weights).
      System.out.println("\nInstances and their weights:\n");
      System.out.println(instances.instancesAndWeights());
      System.out.print("\nSum of weights: ");
      System.out.println(instances.sumOfWeights());
      
      // Insert an attribute
      secondInstances = new Instances(instances);
      Attribute testAtt = new Attribute("Inserted");
      secondInstances.insertAttributeAt(testAtt, 0);
      System.out.println("\nSet with inserted attribute:\n");
      System.out.println(secondInstances);
      System.out.println("\nClass name: "
			 + secondInstances.classAttribute().name());
      
      // Delete the attribute
      secondInstances.deleteAttributeAt(0);
      System.out.println("\nSet with attribute deleted:\n");
      System.out.println(secondInstances);
      System.out.println("\nClass name: "
			 + secondInstances.classAttribute().name());
      
      // Test if headers are equal
      System.out.println("\nHeaders equal: "+
			 instances.equalHeaders(secondInstances) + "\n");
      
      // Print data in internal format.
      System.out.println("\nData (internal values):\n");
      for (i = 0; i < instances.numInstances(); i++) {
	for (j = 0; j < instances.numAttributes(); j++) {
	  if (instances.instance(i).isMissing(j)) {
	    System.out.print("? ");
	  } else {
	    System.out.print(instances.instance(i).value(j) + " ");
	  }
	}
	System.out.println();
      }
      
      // Just print header
      System.out.println("\nEmpty dataset:\n");
      empty = new Instances(instances, 0);
      System.out.println(empty);
      System.out.println("\nClass name: "+empty.classAttribute().name());

      // Create copy and rename an attribute and a value (if possible)
      if (empty.classAttribute().isNominal()) {
	Instances copy = new Instances(empty, 0);
	copy.renameAttribute(copy.classAttribute(), "new_name");
	copy.renameAttributeValue(copy.classAttribute(), 
				  copy.classAttribute().value(0), 
				  "new_val_name");
	System.out.println("\nDataset with names changed:\n" + copy);
	System.out.println("\nOriginal dataset:\n" + empty);
      }

      // Create and prints subset of instances.
      start = instances.numInstances() / 4;
      num = instances.numInstances() / 2;
      System.out.print("\nSubset of dataset: ");
      System.out.println(num + " instances from " + (start + 1) 
			 + ". instance");
      secondInstances = new Instances(instances, start, num);
      System.out.println("\nClass name: "
			 + secondInstances.classAttribute().name());

      // Print all instances and their weights (and the sum of weights).
      System.out.println("\nInstances and their weights:\n");
      System.out.println(secondInstances.instancesAndWeights());
      System.out.print("\nSum of weights: ");
      System.out.println(secondInstances.sumOfWeights());
      
      // Create and print training and test sets for 3-fold
      // cross-validation.
      System.out.println("\nTrain and test folds for 3-fold CV:");
      if (instances.classAttribute().isNominal()) {
	instances.stratify(3);
      }
      for (j = 0; j < 3; j++) {
        train = instances.trainCV(3,j);
	test = instances.testCV(3,j);
                      
	// Print all instances and their weights (and the sum of weights).
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

      // Randomize instances and print them.
      System.out.println("\nRandomized dataset:");
      instances.randomize(random);
      
      // Print all instances and their weights (and the sum of weights).
      System.out.println("\nInstances and their weights:\n");
      System.out.println(instances.instancesAndWeights());
      System.out.print("\nSum of weights: ");
      System.out.println(instances.sumOfWeights());

      // Sort instances according to first attribute and
      // print them.
      System.out.print("\nInstances sorted according to first attribute:\n ");
      instances.sort(0);
        
      // Print all instances and their weights (and the sum of weights).
      System.out.println("\nInstances and their weights:\n");
      System.out.println(instances.instancesAndWeights());
      System.out.print("\nSum of weights: ");
      System.out.println(instances.sumOfWeights());
    } catch (Exception e) {
      e.printStackTrace(); 
    }
  }

  /**
   * Main method for this class -- just prints a summary of a set
   * of instances.
   *
   * @param argv should contain one element: the name of an ARFF file
   */
  public static void main(String [] args) {

    try {
      Reader r = null;
      if (args.length > 1) {
	throw (new Exception("Usage: Instances <filename>"));
      } else if (args.length == 0) {
        r = new BufferedReader(new InputStreamReader(System.in));
      } else {
        r = new BufferedReader(new FileReader(args[0]));
      }
      Instances i = new Instances(r);
      System.out.println(i.toSummaryString());
    } catch (Exception ex) {
      System.err.println(ex.getMessage());
    }
  }
}

     

