/*
 *    Instance.java
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

import java.util.*;
import java.io.*;

/**
 * Class for handling an instance.
 *
 * All methods that change an instance are
 * safe, ie. a change of an instance does
 * not affect any other instances. All
 * methods that change an instance's
 * attribute values clone the attribute value
 * vector before it is changed.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.1 Sep 1998 Eibe
 */

public class Instance implements Copyable, Serializable {

  // =================
  // Private variables
  // =================
  
  /**
   * Constant representing a missing value.
   */

  private final static double MISSING_VALUE = Double.NaN;

  /**
   * The dataset the instance belongs to. Null
   * if the instance does not belong to any dataset.
   */

  private Instances theDataset;

  /**
   * The instance's attribute values.
   */

  private double[] theAttValues;

  /**
   * The instance's weight.
   */

  private double theWeight;

  // ===============
  // Public methods.
  // ===============

  /**
   * Constructor that copies the attributeinformation 
   * from the given instance. Reference to a dataset is set to null.
   * @param instance the instance from which the attribute
   * information is to be copied
   */

  public Instance(Instance instance) {
    
    theAttValues = instance.theAttValues;
    theWeight = instance.theWeight;
    theDataset = null;
  }

  /**
   * Constructor that inititalizes instance variable with given 
   * values. Reference to a dataset is set to null.
   * @param weight the instance's weight
   * @param attValues a vector of attribute values
   */
  
  public Instance(double weight, double[] attValues){
    
    theAttValues = attValues;
    theWeight = weight;
    theDataset = null;
  }

  /**
   * Constructor that inititalizes variables, sets weight
   * to one, and sets all values to be missing.
   * Reference to a dataset is set to null.
   * @param numAttributes the size of the instance
   */
  
  public Instance(int numAttributes) {
    
    theAttValues = new double[numAttributes];
    for (int i = 0; i < theAttValues.length; i++)
      theAttValues[i] = MISSING_VALUE;
    theWeight = 1;
    theDataset = null;
  }

  /**
   * Returns an attribute.
   * @param index the attribute's index
   * @return the attribute with the given position
   * @exception Exception if instance doesn't belong to any
   * dataset
   */ 

  public final Attribute attribute(int index) throws Exception {
   
    if (theDataset == null)
      throw new Exception("Instance doesn't belong to a dataset!");
    return theDataset.attribute(index);
  }

  /**
   * Returns class attribute.
   * @return the class attribute
   * @exception Exception if the class is not set or the
   * instance doesn't belong to any dataset
   */

  public final Attribute classAttribute() throws Exception {

    if (theDataset == null)
      throw new Exception("Instance doesn't belong to a dataset!");
    return theDataset.classAttribute();
  }

  /**
   * Returns the class attribute's index.
   * @return the class index as an integer
   * @exception Exception if instance doesn't belong to any
   * dataset
   */

  public final int classIndex() throws Exception {
    
    if (theDataset == null)
      throw new Exception("Instance doesn't belong to a dataset!");
    return theDataset.classIndex();
  }

  /**
   * Tests if an instance's class is missing.
   * @return true if the instance's class is missing
   * @exception Exception if the class is not set
   */

  public final boolean classIsMissing() throws Exception {

    if (classIndex() < 0)
      throw new Exception("Class is not set!");
    return isMissing(classIndex());
  }

  /**
   * Returns the class label of an instance.
   * otherwise an empty string.
   * @return the instance's class as a string
   * @exception Exception if the class is not set
   */

  public final String classLabel() throws Exception {
    
    if (classIndex() < 0)
      throw new Exception("Class is not set!");
    return toString(classIndex());
  }

  /**
   * Returns an instance's class value in internal format.
   * @return the corresponding value as a double (If the 
   * corresponding attribute is nominal then it returns the 
   * value's index as a double; if the corresponding attribute 
   * is integer-valued then it returns the integer value as 
   * a double).
   * @exception Exception if the class is not set
   */

  public final double classValue() throws Exception {
    
    if (classIndex() < 0)
      throw new Exception("Class is not set!");
    return value(classIndex());
  }

  /**
   * Produces a shallow copy of this instance.
   * @return the shallow copy
   */

  public final Object copy() {

    Object newInstance = new Instance(this);

    return newInstance;
  }

  /**
   * Returns the dataset this instance belongs to.
   * @return the dataset the instance belongs to
   */

  public final Instances dataset() {

    return theDataset;
  }

  /**
   * Deletes an attribute at the given position (0 to 
   * numAttributes() - 1) if the instance does not
   * belong to any dataset. 
   * @param pos the attribute's position
   * @exception Exception if the instance belongs to a
   * dataset
   */

  public final void deleteAttributeAt(int position) 
       throws Exception {

    if (theDataset != null)
      throw new Exception("Instance belongs to a dataset!");
    forceDeleteAttributeAt(position);
  }

  /**
   * Returns an enumeration of all the attributes.
   * @return enumeration of all the attributes
   */

  public Enumeration enumerateAttributes() {

    return theDataset.enumerateAttributes();
  }

  /**
   * Tests if the headers of two instances are equivalent.
   * @param instance another instance
   * @return true if the header of the given instance is 
   * equivalent to this header
   * @exception Exception if instance doesn't belong to any
   * dataset
   */

  public final boolean equalHeaders(Instance inst) 
       throws Exception {

    if (theDataset == null)
      throw new Exception("Instance doesn't belong to a dataset!");
    return theDataset.equalHeaders(inst.theDataset);
  }

  /**
   * Inserts an attribute at the given position (0 to 
   * numAttributes() - 1) if the instance does not
   * belong to any dataset.
   * @param pos the attribute's position
   * @exception Exception if the instance belongs to a
   * dataset
   */

  public final void insertAttributeAt(int position) 
       throws  Exception {

    if (theDataset != null)
      throw new Exception("Instance belongs to a dataset!");
    forceInsertAttributeAt(position);
  }

  /**
   * Returns the dataset the instance belongs to.
   * @return the dataset the instance belongs to
   */

  public final Instances instances() {

    return theDataset;
  }

  /**
   * Tests if a specific value is "missing".
   * @param attIndex the attribute's index
   */

  public final boolean isMissing(int attIndex) {

    if (Double.isNaN(theAttValues[attIndex]))
      return true;
    return false;
  }

  /**
   * Tests if a specific value is "missing".
   * @param att the attribute
   */

  public final boolean isMissing(Attribute att) {

    return isMissing(att.index());
  }

  /**
   * Tests if the given value codes "missing".
   * @param val the value to be tested
   * @return true if val codes "missing"
   */

  public static boolean isMissingValue(double val) {

    return Double.isNaN(val);
  }

  /**
   * Returns the double that codes "missing".
   * @return the double that codes "missing"
   */

  public static double missingValue() {

    return MISSING_VALUE;
  }

  /**
   * Returns the number of attributes.
   * @return the number of attributes as an integer
   */

  public final int numAttributes() {

    return theAttValues.length;
  }

  /**
   * Returns the number of class labels.
   * @return the number of class labels as an integer if the 
   * class attribute is nominal, 1 otherwise.
   * @exception Exception if instance doesn't belong to any
   * dataset
   */
  
  public final int numClasses() throws Exception {
    
    if (theDataset == null)
      throw new Exception("Instance doesn't belong to a dataset!");
    return theDataset.numClasses();
  }

  /** 
   * Replaces all missing values in the instance with the modes 
   * and means contained in the given array and returns a new 
   * instance.
   * @param array containing the means and modes
   * @exception Exception if numbers of attributes are unequal
   */
  
  public final void replaceMissingValues(double[] array) 
       throws Exception {
	 
    if ((array == null) || 
	(array.length != theAttValues.length))
      throw new Exception("Unequal number of attributes!");
    freshAttributeVector();
    for (int i = 0; i < theAttValues.length; i++) 
      if (isMissing(i))
	theAttValues[i] = array[i];
  }

  /**
   * Sets the class value of an instance to be "missing".
   * @exception Exception if the class is not set
   */

  public final void setClassMissing() throws Exception {

    if (classIndex() < 0)
      throw new Exception("Class is not set!");
    freshAttributeVector();
    setMissing(classIndex());
  }

  /**
   * Sets the class value of an instance to the given value 
   * (internal format).
   * @param value the new attribute value (If the corresponding 
   * attribute is nominal then this is the new value's index as 
   * a double. If the corresponding attribute is integer-valued 
   * then this is the new integer value as a double).
   * @exception Exception if the class is not set
   */

  public final void setClassValue(double value) throws Exception {

    if (classIndex() < 0)
      throw new Exception("Class is not set!");
    freshAttributeVector();
    theAttValues[classIndex()] = value;
  }

  /**
   * Sets the class value of an instance to the given value.
   * @param value the new class value (If the class
   * is a string attribute and the value can't be found,
   * the value is added to the attribute.)
   * @exception Exception if the dataset or the class is not set, the 
   * attribute is not nominal or a string or the value couldn't 
   * be found for a nominal attribute
   */

  public final void setClassValue(String value) throws Exception {

    if (classIndex() < 0)
      throw new Exception("Class is not set!");
    setValue(classIndex(), value);
  }

  /**
   * Sets the reference to a dataset. Checks if the instance
   * has the right format for the dataset. Note: the dataset
   * does not know about this instance. If the structure of
   * the dataset's header gets changed, this instance will not
   * be adjusted automatically. Does not check if the instance
   * is compatible with the dataset.
   * @param instances the reference to the dataset
   */

  public final void setDataset(Instances instances) {
    
    theDataset = instances;
  }

  /**
   * Sets a specific value to be "missing".
   * @param attIndex the attribute's index
   */

  public final void setMissing(int attIndex) {

    freshAttributeVector();
    theAttValues[attIndex] = MISSING_VALUE;
  }

  /**
   * Sets a specific value to be "missing".
   * @param att the attribute
   */

  public final void setMissing(Attribute att) {

    setMissing(att.index());
  }

  /**
   * Sets a specific value in the instance to the given value 
   * (internal format).
   * @param attIndex the attribute's index
   * @param value the new attribute value (If the corresponding 
   * attribute is nominal then this is the new value's index as 
   * a double. If the corresponding attribute is integer-valued 
   * then this is the new integer value as a double).
   */

  public final void setValue(int attIndex, double value) {
    
    freshAttributeVector();
    theAttValues[attIndex] = value;
  }

  /**
   * Sets a value of an nominal or string attribute to the 
   * given value.
   * @param attIndex the attribute's index
   * @param value the new attribute value (If the attribute
   * is a string attribute and the value can't be found,
   * the value is added to the attribute.)
   * @exception Exception if the dataset is not set, the 
   * attribute is not nominal or a string or the value couldn't 
   * be found for a nominal attribute
   */

  public final void setValue(int attIndex, String value) throws Exception {
    
    int valIndex;

    if (theDataset == null)
      throw new Exception("Instance doesn't belong to a dataset!");
    if (!attribute(attIndex).isNominal() &&
	!attribute(attIndex).isString())
      throw new Exception("Attribute neither nominal nor string!");
    valIndex = attribute(attIndex).indexOfValue(value);
    if (valIndex == -1)
      if (attribute(attIndex).isNominal())
	throw new Exception("Value not defined for given nominal attribute!");
      else {
	attribute(attIndex).forceAddValue(value);
	valIndex = attribute(attIndex).indexOfValue(value);
      }
    freshAttributeVector();
    theAttValues[attIndex] = (double)valIndex;
  }

  /**
   * Sets a specific value in the instance to the given value 
   * (internal format).
   * @param att the attribute
   * @param value the new attribute value (If the corresponding
   * attribute is nominal then this is the new value's index as
   * a double. If the corresponding attribute is integer-valued 
   * then this is the new integer value as a double).
   */

  public final void setValue(Attribute att, double value){
    
    setValue(att.index(), value);
  }

  /**
   * Sets a value of an nominal or string attribute to the 
   * given value.
   * @param att the attribute
   * @param value the new attribute value (If the attribute
   * is a string attribute and the value can't be found,
   * the value is added to the attribute.)
   * @exception Exception if the dataset is not set, the 
   * attribute is not nominal or a string or the value couldn't 
   * be found for a nominal attribute
   */

  public final void setValue(Attribute att, String value) throws Exception {
    
    setValue(att.index(), value);
  }

  /**
   * Sets the weight of an instance to the given value.
   * @param weight the weight
   */

  public final void setWeight(double weight) {

    theWeight = weight;
  }

  /**
   * Returns the description of one instance. If the instance
   * doesn't belong to any dataset it returns the internal
   * values.
   * @return the instance's description as a string
   */

  public final String toString() {

    StringBuffer text = new StringBuffer();
    
    for (int i = 0; i < theAttValues.length; i++){
      if (i > 0) text.append(",");
      text.append(toString(i));
    }

    return text.toString();
  }

  /**
   * Returns the description of one value of the instance as a 
   * string. If the instance doesn't belong to any dataset it 
   * returns the internal values.
   * @param attIndex the attribute's index
   * @return the value's description as a string
   */

  public final String toString(int attIndex) {

   StringBuffer text = new StringBuffer();
   
   if (isMissing(attIndex))
     text.append("?");
   else
     if (theDataset == null)
       text.append(Utils.doubleToString(theAttValues[attIndex],6));
     else
       if (theDataset.attribute(attIndex).isNominal() || 
	   theDataset.attribute(attIndex).isString())
	 text.append(theDataset.attribute(attIndex).
		     value((int) theAttValues[attIndex]));
       else
	 text.append(Utils.doubleToString(theAttValues[attIndex],6));
   
   return text.toString();
  }

  /**
   * Returns the description of one value of the instance as a 
   * string.
   * @param att the attribute
   * @return the value's description as a string
   */

  public final String toString(Attribute att){
   
   return toString(att.index());
  }

  /**
   * Returns an instance's attribute value in internal format.
   * @param attIndex the attribute's index
   * @return the specified value as a double (If the corresponding
   * attribute is nominal then it returns the value's index as a 
   * double. If the corresponding attribute is integer-valued then 
   * it returns the integer value as a double).
   */

  public final double value(int attIndex) {

    return theAttValues[attIndex];
  }

  /**
   * Returns an instance's attribute value in internal format.
   * @param att the attribute
   * @return the specified value as a double (If the corresponding
   * attribute is nominal then it returns the value's index as a
   * double. If the corresponding attribute is integer-valued then 
   * it returns the integer value as a double).
   */

  public final double value(Attribute att) {

    return theAttValues[att.index()];
  }

  /**
   * Returns an instance's weight.
   * @return the instance's weight as a double
   */

  public final double weight(){

    return theWeight;
  }

  // ===============
  // Package methods
  // ===============

  /**
   * Deletes an attribute at the given position (0 to 
   * numAttributes() - 1).
   * @param pos the attribute's position
   */

  final void forceDeleteAttributeAt(int position) {

    double[] newValues = new double[theAttValues.length - 1];

    System.arraycopy(theAttValues, 0, newValues, 0, position);
    if (position < theAttValues.length - 1)
      System.arraycopy(theAttValues, position + 1, 
		       newValues, position, 
		       theAttValues.length - (position + 1));
    theAttValues = newValues;
  }

  /**
   * Inserts an attribute at the given position
   * (0 to numAttributes()) and sets its value to be 
   * missing. 
   * @param pos the attribute's position
   */

  final void forceInsertAttributeAt(int position)  {

    double[] newValues = new double[theAttValues.length + 1];

    System.arraycopy(theAttValues, 0, newValues, 0, position);
    newValues[position] = MISSING_VALUE;
    System.arraycopy(theAttValues, position, newValues, 
		     position + 1, theAttValues.length - position);
    theAttValues = newValues;
  }

  // ===============
  // Private methods
  // ===============

  /**
   * Clones the attribute vector of the instance and
   * overwrites it with the clone.
   */

  private void freshAttributeVector() {

    double[] newValues;

    newValues = new double[theAttValues.length];
    System.arraycopy(theAttValues, 0, newValues, 0, 
		     theAttValues.length);
    theAttValues = newValues;
  }
}
