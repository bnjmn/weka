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
 *    EuclideanDistance.java
 *    Copyright (C) 2002 University of Waikato
 *
 */

package weka.core;

import java.io.Serializable;
import java.io.*;

/**
 * Implementing Euclidean distance (or similarity) function.
 *
 * One object defines not one distance but the data model in which 
 * the distances between objects of that data model can be computed.
 *
 * Attention: For efficiency reasons the use of consistency checks (like are 
 * the data models of the two instances exactly the same), is low. 
 *
 * @author Gabi Schmidberger (gabi@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
public class EuclideanDistance extends DistanceFunction
                               implements 
                               Cloneable, Serializable {


  /** True if normalization should be done */
  protected boolean m_Normalize = true;

  /** The number of attributes the contribute to a prediction */
  protected double m_NumAttributesUsed;

  /**
   * Constructs an Euclidean Distance object.
   * @param data the instances the distance function should work on
    */
  public EuclideanDistance() {
    
  }

  /**
   * Constructs an Euclidean Distance object.
   * @param data the instances the distance function should work on
    */
  public EuclideanDistance(Instances data) {
    
    super(data);
    setNumAttributesUsed();
  }

  /**
   * Constructs an Euclidean Distance object.
   * @param data the instances the distance function should work on
   * @param normalize if true normalization is done
    */
  public EuclideanDistance(Instances data, boolean normalize) {
    
    super(data);
    m_Normalize = normalize;
    setNumAttributesUsed();
  }

  /**
   * Constructs an Euclidean Distance object. Ranges are already given.
   * @param data the instances the distance function should work on
   * @param ranges the min and max values of the attribute values
   */
  public EuclideanDistance(Instances data, double [][] ranges) {
    
    super(data, ranges); 
    setNumAttributesUsed();
  }

  /**
   * Constructs an Euclidean Distance object. Ranges are already given.
   * @param data the instances the distance function should work on
   * @param ranges the min and max values of the attribute values
   * @param normalize if true normalization is done
   */
  public EuclideanDistance(Instances data, double [][] ranges, boolean normalize) {
    
    super(data, ranges); 
    m_Normalize = normalize;
    setNumAttributesUsed();
  }
 
  /**
   * Computes and sets the number of attributes used.
   */
  public void setNumAttributesUsed() {  

    m_NumAttributesUsed = 0.0;
    for (int i = 0; i < m_Model.numAttributes(); i++) {
      if ((i != m_Model.classIndex()) && 
	  (m_Model.attribute(i).isNominal() ||
	   m_Model.attribute(i).isNumeric())) {
	m_NumAttributesUsed += 1.0;
      }
    }
  }

  /**
   * Calculates the distance (or similarity) between two instances.
   * @param first the first instance
   * @param second the second instance
   * @return the distance between the two given instances,
   */
  public double distance(Instance first, Instance second) throws Exception {

    if (!Instances.inRanges(first,m_Ranges))
	OOPS("Not in ranges");
    //OOPS(" dist first  "+ first);
    if (!Instances.inRanges(second,m_Ranges))
	OOPS("Not in ranges");
    //OOPS(" dist second "+ second);
    double distance = 0;
    int firstI, secondI;

    for (int p1 = 0, p2 = 0; 
	 p1 < first.numValues() || p2 < second.numValues();) {
      if (p1 >= first.numValues()) {
	// model !! todo numinstances might change
	firstI = m_Model.numAttributes();
      } else {
	// only in case instance is sparseInstance, firstI is different to p1 
	firstI = first.index(p1); 
      }
      if (p2 >= second.numValues()) {
	secondI = m_Model.numAttributes();
      } else {
	secondI = second.index(p2);
      }
      // ignore class values
      if (firstI == m_Model.classIndex()) {
	p1++; continue;
      } 
      if (secondI == m_Model.classIndex()) {
	p2++; continue;
      } 
      double diff;
      if (firstI == secondI) {
	diff = difference(firstI, 
			  first.valueSparse(p1),
			  second.valueSparse(p2));
	p1++; p2++;
      } else if (firstI > secondI) {
	diff = difference(secondI, 
			  0, second.valueSparse(p2));
	p2++;
      } else {
	diff = difference(firstI, 
			  first.valueSparse(p1), 0);
	p1++;
      }
      distance += diff * diff;
    }

    distance = Math.sqrt(distance / m_NumAttributesUsed);
    return distance;
  }

  /**
   * Computes the difference between two given attribute values.
   * @param index the index of the current attribute
   * @param val1 the first attribute value
   * @param val2 the second attribute value
   * @return the distance between the two given attribute values
   */
  private double difference(int index, double val1, double val2) 
    throws Exception{
    
    // If attribute is numeric
    if (Instance.isMissingValue(val1) || 
	Instance.isMissingValue(val2)) {
      throw new Exception("Missing value not allowed.");
    } else {
      return norm(val1, index) - norm(val2, index);
    }
  }

  /**
   * Returns value in the middle of the two parameter values.
   * @param range the ranges to this dimension
   * @return the middle value
   */
  public double getMiddle(double[] ranges) {

    double middle = ranges[R_MIN] + ranges[R_WIDTH] * 0.5;
    return middle;
  }

  /**
   * Checks the instances.
   * Dataset should only contain nominal or stringumeric attributes.
   */
  public void checkInstances() throws Exception {
    
    for (int i = 0; i < m_Model.numAttributes(); i++) {
      if (m_Model.classIndex() != i) {
	if (!m_Model.attribute(i).isNumeric())
	  throw new Exception("Euclidean Distance allows only numeric attributes.");
      }
    }
  }

  /**
   * Returns true if the value of the given dimension is smaller or equal the 
   * value to be compared with.
   * @param instance the instance where the value should be taken of
   * @param dim the dimension of the value
   * @param the value to compare with
   * @return true is value of instance is smaller or equal value
   */
  public boolean valueIsSmallerEqual(Instance instance, int dim,
                                     double value) {
    return instance.value(dim) <= value;
  }

  /**
   * Normalises a given value of a numeric attribute.
   * @param ranges the min max values of the attributes
   * @param x the value to be normalized
   * @param i the attribute's index
   */
  private double norm(double x, int i) {

    if (!m_Normalize) {
      return x;
    } else if (Double.isNaN(m_Ranges[i][R_MIN]) || 
	       Utils.eq(m_Ranges[i][R_MAX], m_Ranges[i][R_MIN])) {
      return 0;
    } else {

      return (x - m_Ranges[i][R_MIN]) / (m_Ranges[i][R_WIDTH]);
    }
  }

  /** 
   * Documents the content of an EuclideanDistance object in a string.
   * @return the converted string
   */
  public String toString() {

    StringBuffer text = new StringBuffer();
    //todo
    text.append("\n");
    return text.toString();
  }

  /**
   * Used for debug println's.
   * @param output string that is printed
   */
  private void OOPS(String output) {
    System.out.println(output);
  }

  /**
   * Main method for testing this class.
   */
  public static void main(String[] args) {
  
    try {
      Reader r = null;
      if (args.length > 1) {
	throw (new Exception("Usage: EuclideanDistance <filename>"));
      } else if (args.length == 0) {
        r = new BufferedReader(new InputStreamReader(System.in));
      } else {
        r = new BufferedReader(new FileReader(args[0]));
      }
      Instances i = new Instances(r);
      EuclideanDistance test = new EuclideanDistance(i);
      System.out.println("test:\n " + test);
     
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}



