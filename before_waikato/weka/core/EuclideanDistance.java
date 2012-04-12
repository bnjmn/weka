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
 *    Copyright (C) 1999-2007 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.neighboursearch.PerformanceStats;

/**
 <!-- globalinfo-start -->
 * Implementing Euclidean distance (or similarity) function.<br/>
 * <br/>
 * One object defines not one distance but the data model in which the distances between objects of that data model can be computed.<br/>
 * <br/>
 * Attention: For efficiency reasons the use of consistency checks (like are the data models of the two instances exactly the same), is low.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -D
 *  Turns off the normalization of attribute 
 *  values in distance calculation.</pre>
 * 
 <!-- options-end --> 
 *
 * @author Gabi Schmidberger (gabi@cs.waikato.ac.nz)
 * @author Ashraf M. Kibriya (amk14@cs.waikato.ac.nz)
 * @version $Revision: 1.11 $
 */
public class EuclideanDistance
  implements DistanceFunction, OptionHandler, Cloneable, Serializable {

  /** for serialization. */
  private static final long serialVersionUID = 1068606253458807903L;

  /** the data. */
  protected Instances m_Data;

  /** True if normalization is turned off (default false).*/
  protected boolean m_DontNormalize = false;
  
  /** The number of attributes the contribute to a prediction. */
  protected double m_NumAttributesUsed;

  /**
   * Constructs an Euclidean Distance object.
   */
  public EuclideanDistance() {
  }

  /**
   * Constructs an Euclidean Distance object.
   * 
   * @param data 	the instances the distance function should work on
   */
  public EuclideanDistance(Instances data) {
    m_Data = data;
    initializeRanges();
    setNumAttributesUsed();
  }

  /**
   * Returns a string describing this object.
   * 
   * @return 		a description of the evaluator suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return 
        "Implementing Euclidean distance (or similarity) function.\n\n"
      + "One object defines not one distance but the data model in which "
      + "the distances between objects of that data model can be computed.\n\n"
      + "Attention: For efficiency reasons the use of consistency checks "
      + "(like are the data models of the two instances exactly the same), "
      + "is low.";
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return 		an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector newVector = new Vector();
    
    newVector.add(new Option(
	"\tTurns off the normalization of attribute \n"
	+ "\tvalues in distance calculation.",
	"D", 0, "-D"));
    
    return newVector.elements();
  }
  
  /**
   * Parses a given list of options. Valid options are:<p/>
   *
   * @param options 	the list of options as an array of strings
   * @throws Exception 	if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    setDontNormalize(Utils.getFlag('D', options));
  }

  /**
   * Gets the current settings of IBk.
   *
   * @return 		an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions() {
    String[] options = new String[1];

    if (getDontNormalize())
      options[0] = "-D";
    else
      options[0] = "";
    
    return options;
  }
  
  /**
   * Sets the instances.
   * 
   * @param insts	the instances to use
   */
  public void setInstances(Instances insts) {
    m_Data = insts;
    initializeRanges();
    setNumAttributesUsed();
  }
  
  /**
   * returns the instances currently set.
   * 
   * @return		the current instances
   */
  public Instances getInstances() {
    return m_Data;
  }
  
  /** 
   * Returns the tip text for this property.
   * 
   * @return 		tip text for this property suitable for
   *         		displaying in the explorer/experimenter gui
   */
  public String dontNormalizeTipText() {
    return "Whether if the normalization of attributes should be turned off " +
           "for distance calculation (Default: false i.e. attribute values " +
           "are normalized). ";
  }
  
  /** 
   * Sets whether if the attribute values are to be normalized in distance
   * calculation.
   * 
   * @param dontNormalize	if true the values are not normalized
   */
  public void setDontNormalize(boolean dontNormalize) {
    m_DontNormalize = dontNormalize;
  }
  
  /**
   * Gets whether if the attribute values are to be normazlied in distance
   * calculation. (default false i.e. attribute values are normalized.)
   * 
   * @return		false if values get normalized
   */
  public boolean getDontNormalize() {
    return m_DontNormalize;
  }
  
  /**
   * Update the distance function (if necessary) for the newly added instance.
   * 
   * @param ins		the instance to add
   */
  public void update(Instance ins) {
    updateRanges(ins);
  }
  
  /**
   * Calculates the distance between two instances.
   * 
   * @param first 	the first instance
   * @param second 	the second instance
   * @return 		the distance between the two given instances
   */
  public double distance(Instance first, Instance second) {
    return Math.sqrt(distance(first, second, Double.POSITIVE_INFINITY));
  }
  
  /**
   * Calculates the distance (or similarity) between two instances. Need to
   * pass this returned distance later on to postprocess method to set it on
   * correct scale. <br/>
   * P.S.: Please don't mix the use of this function with
   * distance(Instance first, Instance second), as that already does post
   * processing. Please consider passing Double.POSITIVE_INFINITY as the cutOffValue to
   * this function and then later on do the post processing on all the
   * distances.
   *
   * @param first 	the first instance
   * @param second 	the second instance
   * @param stats 	the structure for storing performance statistics.
   * @return 		the distance between the two given instances or 
   * 			Double.POSITIVE_INFINITY.
   */
  public double distance(Instance first, Instance second, PerformanceStats stats) { //debug method pls remove after use
    return Math.sqrt(distance(first, second, Double.POSITIVE_INFINITY, stats, false));
  }
  
  /**
   * Calculates the distance (or similarity) between two instances. Need to
   * pass this returned distance later on to postprocess method to set it on
   * correct scale. <br/>
   * P.S.: Please don't mix the use of this function with
   * distance(Instance first, Instance second), as that already does post
   * processing. Please consider passing Double.POSITIVE_INFINITY as the cutOffValue to
   * this function and then later on do the post processing on all the
   * distances.
   *
   * @param first 	the first instance
   * @param second 	the second instance
   * @param cutOffValue	If the distance being calculated becomes larger than 
   * 			cutOffValue then the rest of the calculation is skipped 
   * 			and Double.POSITIVE_INFINITY is returned. Otherwise 
   * 			the correct disntance is returned.
   * @return 		the distance between the two given instances or 
   * 			Double.POSITIVE_INFINITY.
   */
  public double distance(Instance first, Instance second, double cutOffValue) { //debug method pls remove after use
    return distance(first, second, cutOffValue, null, false);
  }
  
  /**
   * Calculates the distance (or similarity) between two instances. Need to
   * pass this returned distance later on to postprocess method to set it on
   * correct scale. <br/>
   * P.S.: Please don't mix the use of this function with
   * distance(Instance first, Instance second), as that already does post
   * processing. Please consider passing Double.POSITIVE_INFINITY as the 
   * cutOffValue to this function and then later on do the post processing on 
   * all the distances.
   *
   * @param first 	the first instance
   * @param second 	the second instance
   * @param cutOffValue	If the distance being calculated becomes larger than 
   * 			cutOffValue then the rest of the calculation is skipped 
   * 			and Double.POSITIVE_INFINITY is returned. Otherwise 
   * 			the correct disntance is returned.
   * @param print 	whether to print some debugging output
   * @return 		the distance between the two given instances or 
   * 			Double.POSITIVE_INFINITY.
   */
  public double distance(Instance first, Instance second, double cutOffValue, boolean print) {
    return distance(first, second, cutOffValue, null, print);
  }
  
  /**
   * Calculates the distance (or similarity) between two instances. Need to
   * pass this returned distance later on to postprocess method to set it on
   * correct scale. <br/>
   * P.S.: Please don't mix the use of this function with
   * distance(Instance first, Instance second), as that already does post
   * processing. Please consider passing Double.POSITIVE_INFINITY as the cutOffValue to
   * this function and then later on do the post processing on all the
   * distances.
   *
   * @param first 	the first instance
   * @param second 	the second instance
   * @param cutOffValue	If the distance being calculated becomes larger than 
   * 			cutOffValue then the rest of the calculation is skipped 
   * 			and Double.POSITIVE_INFINITY is returned. Otherwise 
   * 			the correct disntance is returned.
   * @param stats 	the structure for storing performance statistics.
   * @return 		the distance between the two given instances or 
   * 			Double.POSITIVE_INFINITY.
   */
  public double distance(Instance first, Instance second, double cutOffValue, PerformanceStats stats) { //debug method pls remove after use
    return distance(first, second, cutOffValue, stats, false);
  }  

  /**
   * Calculates the distance (or similarity) between two instances. Need to
   * pass this returned distance later on to postprocess method to set it on
   * correct scale. <br/>
   * P.S.: Please don't mix the use of this function with
   * distance(Instance first, Instance second), as that already does post
   * processing. Please consider passing Double.POSITIVE_INFINITY as the cutOffValue to
   * this function and then later on do the post processing on all the
   * distances.
   *
   * @param first 	the first instance
   * @param second 	the second instance
   * @param cutOffValue	If the distance being calculated becomes larger than 
   * 			cutOffValue then the rest of the calculation is skipped 
   * 			and Double.POSITIVE_INFINITY is returned. Otherwise 
   * 			the correct disntance is returned.
   * @param stats 	the structure for storing performance statistics.
   * @param print 	whether to print some debugging output
   * @return 		the distance between the two given instances or 
   * 			Double.POSITIVE_INFINITY.
   */
  public double distance(Instance first, Instance second, double cutOffValue, PerformanceStats stats, boolean print) {
    
    double distance = 0;
    int firstI, secondI;
    
    if(print) {
      OOPS("Instance1: "+first);
      OOPS("Instance2: "+second);
      OOPS("cutOffValue: "+cutOffValue);
    }
    
    for (int p1 = 0, p2 = 0;
         p1 < first.numValues() || p2 < second.numValues();) {
      if (p1 >= first.numValues()) {
	firstI = m_Data.numAttributes();
      } else {
	firstI = first.index(p1); 
      }
      if (p2 >= second.numValues()) {
	secondI = m_Data.numAttributes();
      } else {
	secondI = second.index(p2);
      }
      if (firstI == m_Data.classIndex()) {
	p1++; continue;
      }
      if (secondI == m_Data.classIndex()) {
	p2++; continue;
      }
      double diff;
      if(print)
        System.out.println("valueSparse(p1): "+first.valueSparse(p1)+" valueSparse(p2): "+second.valueSparse(p2));
      
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
      if(print)
        System.out.println("diff: "+diff);
      if(stats!=null)
	stats.incrCoordCount();
      
      distance += diff * diff;
      if(distance > cutOffValue) //Utils.gr(distance, cutOffValue))
        return Double.POSITIVE_INFINITY;
      if(print)
        System.out.println("distance: "+distance);
    }
    if(print) {
      OOPS("Instance 1: "+first);
      OOPS("Instance 2: "+second);
      OOPS("distance: "+distance);
      OOPS("AttribsUsed: "+m_NumAttributesUsed);
      OOPS("distance/AttribsUsed: "+Math.sqrt(distance / m_NumAttributesUsed));
    }
    return distance;
  }
  
  /**
   * Does post processing of the distances (if necessary) returned by
   * distance(distance(Instance first, Instance second, double cutOffValue). It
   * is necessary to do so to get the correct distances if
   * distance(distance(Instance first, Instance second, double cutOffValue) is
   * used. This is because that function actually returns the squared distance
   * to avoid inaccuracies arising from floating point comparison.
   * 
   * @param distances	the distances to post-process
   */
  public void postProcessDistances(double distances[]) {
    for(int i=0; i<distances.length; i++) {
      distances[i] = Math.sqrt(distances[i]);
    }
  }

  /**
   * Computes the difference between two given attribute
   * values.
   * 
   * @param index	the attribute index
   * @param val1	the first value
   * @param val2	the second value
   * @return		the difference
   */
  private double difference(int index, double val1, double val2) {
    
    switch (m_Data.attribute(index).type()) {
      case Attribute.NOMINAL:
        
        // If attribute is nominal
        if(Instance.isMissingValue(val1) ||
           Instance.isMissingValue(val2) ||
           ((int)val1 != (int)val2)) {
          return 1;
        } else {
          return 0;
        }
      case Attribute.NUMERIC:
        // If attribute is numeric
        if(Instance.isMissingValue(val1) ||
           Instance.isMissingValue(val2)) {
          if(Instance.isMissingValue(val1) &&
             Instance.isMissingValue(val2)) {
            if(!m_DontNormalize)  //We are doing normalization
              return 1;
            else
              return (m_Ranges[index][R_MAX] - m_Ranges[index][R_MIN]);
          } else {
            double diff;
            if (Instance.isMissingValue(val2)) {
              diff = (!m_DontNormalize) ? norm(val1, index) : val1;
            } else {
              diff = (!m_DontNormalize) ? norm(val2, index) : val2;
            }
            if (!m_DontNormalize && diff < 0.5) {
              diff = 1.0 - diff;
            }
            else if (m_DontNormalize) {
              if((m_Ranges[index][R_MAX]-diff) > (diff-m_Ranges[index][R_MIN]))
                return m_Ranges[index][R_MAX]-diff;
              else
                return diff-m_Ranges[index][R_MIN];
            }
            return diff;
          }
        } else {
          return (!m_DontNormalize) ? 
              	 (norm(val1, index) - norm(val2, index)) :
              	 (val1 - val2);
        }
      default:
        return 0;
    }
  }
  
  /**
   * Returns the squared difference of two values of an attribute.
   * 
   * @param index	the attribute index
   * @param val1	the first value
   * @param val2	the second value
   * @return		the squared difference
   */
  public double sqDifference(int index, double val1, double val2) {
    double val = difference(index, val1, val2);
    return val*val;
  }
  
  /**
   * Normalizes a given value of a numeric attribute.
   *
   * @param x 		the value to be normalized
   * @param i 		the attribute's index
   * @return		the normalized value
   */
  private double norm(double x,int i) {

    if (Double.isNaN(m_Ranges[i][R_MIN]) || m_Ranges[i][R_MAX]==m_Ranges[i][R_MIN]) { //Utils.eq(m_Ranges[i][R_MAX], m_Ranges[i][R_MIN])) {
      return 0;
    } else {
      return (x - m_Ranges[i][R_MIN]) / (m_Ranges[i][R_WIDTH]);
    }
  }
  
  /**
   * Returns value in the middle of the two parameter values.
   * 
   * @param ranges 	the ranges to this dimension
   * @return 		the middle value
   */
  public double getMiddle(double[] ranges) {

    double middle = ranges[R_MIN] + ranges[R_WIDTH] * 0.5;
    return middle;
  }
  
  /**
   * Returns the index of the closest point to the current instance.
   * Index is index in Instances object that is the second parameter.
   *
   * @param instance 	the instance to assign a cluster to
   * @param allPoints 	all points
   * @param pointList 	the list of points
   * @return 		the index of the closest point
   * @throws Exception	if something goes wrong
   */
  public int closestPoint(Instance instance, Instances allPoints,
      			  int[] pointList) throws Exception {
    double minDist = Integer.MAX_VALUE;
    int bestPoint = 0;
    for (int i = 0; i < pointList.length; i++) {
      double dist = distance(instance, allPoints.instance(pointList[i]), Double.POSITIVE_INFINITY);
      if (dist < minDist) {
        minDist = dist;
        bestPoint = i;
      }
    }
    return pointList[bestPoint];
  }
  
  /**
   * Returns true if the value of the given dimension is smaller or equal the
   * value to be compared with.
   * 
   * @param instance 	the instance where the value should be taken of
   * @param dim 	the dimension of the value
   * @param value 	the value to compare with
   * @return 		true if value of instance is smaller or equal value
   */
  public boolean valueIsSmallerEqual(Instance instance, int dim,
      				     double value) {  //This stays
    return instance.value(dim) <= value;
  }
  
  /**
   * Documents the content of an EuclideanDistance object in a string.
   * 
   * @return 		the converted string
   */
  public String toString() {
    
    StringBuffer text = new StringBuffer();
    //todo
    text.append("\n");
    return text.toString();
  }
  
  /**
   * Used for debug println's.
   * 
   * @param output 	string that is printed
   */
  private void OOPS(String output) {
    System.out.println(output);
  }
  
  /**
   * Computes and sets the number of attributes used.
   */
  private void setNumAttributesUsed() {
    
    m_NumAttributesUsed = 0.0;
    if(m_Data!=null) {
      for (int i = 0; i < m_Data.numAttributes(); i++) {
	if ((i != m_Data.classIndex()) &&
	    (m_Data.attribute(i).isNominal() ||
	     m_Data.attribute(i).isNumeric())) {
	  m_NumAttributesUsed += 1.0;
	}
      }
    }
  }
  
  
  /*============================Ranges related functions=====================*/
  
  /** The range of the attributes. */ //being used in KDTree and EuclideanDistance
  protected double[][] m_Ranges;
  
  /** Index in ranges for MIN. */
  public static final int R_MIN = 0;
  /** Index in ranges for MAX. */
  public static final int R_MAX = 1;
  /** Index in ranges for WIDTH. */
  public static final int R_WIDTH = 2;
  
  /**
   * Initializes the ranges using all instances of the dataset.
   * Sets m_Ranges.
   * 
   * @return the ranges
   */  //Being used in other classes (KDTree).
  public double[][] initializeRanges() {
    
    if(m_Data == null) {
      m_Ranges = null;
      return null;
    }
    
    int numAtt = m_Data.numAttributes();
    double[][] ranges = new double [numAtt][3];
    
    if (m_Data.numInstances() <= 0) {
      initializeRangesEmpty(numAtt, ranges);
      m_Ranges = ranges;
      return ranges;
    }
    else
      // initialize ranges using the first instance
      updateRangesFirst(m_Data.instance(0), numAtt, ranges);
    
    // update ranges, starting from the second
    for (int i = 1; i < m_Data.numInstances(); i++) {
      updateRanges(m_Data.instance(i), numAtt, ranges);
    }
    m_Ranges = ranges;
    return ranges;
  }
  
  /**
   * Initializes the ranges of a subset of the instances of this dataset.
   * Therefore m_Ranges is not set.
   * 
   * @param instList 	list of indexes of the subset
   * @return 		the ranges
   * @throws Exception	if something goes wrong
   */  //being used in other classes (KDTree and XMeans)
  public double[][] initializeRanges(int[] instList) throws Exception {
    
    if(m_Data == null) {
      throw new Exception("No instances supplied.");
    }
    
    int numAtt = m_Data.numAttributes();
    double[][] ranges = new double [numAtt][3];
    
    if (m_Data.numInstances() <= 0) {
      initializeRangesEmpty(numAtt, ranges);
      return ranges;
    }
    else {
      // initialize ranges using the first instance
      updateRangesFirst(m_Data.instance(instList[0]), numAtt, ranges);
      // update ranges, starting from the second
      for (int i = 1; i < instList.length; i++) {
        updateRanges(m_Data.instance(instList[i]), numAtt, ranges);
      }
    }
    return ranges;
  }

  /**
   * Initializes the ranges of a subset of the instances of this dataset.
   * Therefore m_Ranges is not set.
   * The caller of this method should ensure that the supplied start and end 
   * indices are valid (start &lt;= end, end&lt;instList.length etc) and
   * correct.
   *
   * @param instList 	list of indexes of the instances
   * @param startIdx 	start index of the subset of instances in the indices array
   * @param endIdx 	end index of the subset of instances in the indices array
   * @return 		the ranges
   * @throws Exception	if something goes wrong
   */  //being used in other classes (KDTree and XMeans)
  public double[][] initializeRanges(int[] instList, int startIdx, int endIdx) 
      throws Exception {
    
    if(m_Data == null) {
      throw new Exception("No instances supplied.");
    }
    
    int numAtt = m_Data.numAttributes();
    double[][] ranges = new double [numAtt][3];
    
    if (m_Data.numInstances() <= 0) {
      initializeRangesEmpty(numAtt, ranges);
      return ranges;
    }
    else {
      // initialize ranges using the first instance
      updateRangesFirst(m_Data.instance(instList[startIdx]), numAtt, ranges);
      // update ranges, starting from the second
      for (int i = startIdx+1; i <= endIdx; i++) {
        updateRanges(m_Data.instance(instList[i]), numAtt, ranges);
      }
    }
    return ranges;
  }
  
  /**
   * Used to initialize the ranges.
   * 
   * @param numAtt 	number of attributes in the model
   * @param ranges 	low, high and width values for all attributes
   */ //being used in the functions above
  public void initializeRangesEmpty(int numAtt, double[][] ranges) {
    
    for (int j = 0; j < numAtt; j++) {
      ranges[j][R_MIN] = Double.POSITIVE_INFINITY;
      ranges[j][R_MAX] = -Double.POSITIVE_INFINITY;
      ranges[j][R_WIDTH] = Double.POSITIVE_INFINITY;
    }
  }
  
  /**
   * Used to initialize the ranges. For this the values of the first
   * instance is used to save time.
   * Sets low and high to the values of the first instance and
   * width to zero.
   * 
   * @param instance 	the new instance
   * @param numAtt 	number of attributes in the model
   * @param ranges 	low, high and width values for all attributes
   */  //being used in the functions above
  public void updateRangesFirst(Instance instance, int numAtt,
                                double[][] ranges) {
    
    for (int j = 0; j < numAtt; j++) {
      if (!instance.isMissing(j)) {
        ranges[j][R_MIN] = instance.value(j);
        ranges[j][R_MAX] = instance.value(j);
        ranges[j][R_WIDTH] = 0.0;
      }
      else { // if value was missing
        ranges[j][R_MIN] = Double.POSITIVE_INFINITY;
        ranges[j][R_MAX] = -Double.POSITIVE_INFINITY;
        ranges[j][R_WIDTH] = Double.POSITIVE_INFINITY;
      }
    }
  }
  
  /**
   * Updates the minimum and maximum and width values for all the attributes
   * based on a new instance.
   * 
   * @param instance 	the new instance
   * @param numAtt 	number of attributes in the model
   * @param ranges 	low, high and width values for all attributes
   */ //Being used in the functions above
  private void updateRanges(Instance instance, int numAtt, double[][] ranges) {
    
    // updateRangesFirst must have been called on ranges
    for (int j = 0; j < numAtt; j++) {
      double value = instance.value(j);
      if (!instance.isMissing(j)) {
        if (value < ranges[j][R_MIN]) {
          ranges[j][R_MIN] = value;
          ranges[j][R_WIDTH] = ranges[j][R_MAX] - ranges[j][R_MIN];
          if(value > ranges[j][R_MAX]) { //if this is the first value that is
            ranges[j][R_MAX] = value;    //not missing. The,0
            ranges[j][R_WIDTH] = ranges[j][R_MAX] - ranges[j][R_MIN];
          }
        } else {
          if (value > ranges[j][R_MAX]) {
            ranges[j][R_MAX] = value;
            ranges[j][R_WIDTH] = ranges[j][R_MAX] - ranges[j][R_MIN];
          }
        }
      }
    }
  }
  
  /**
   * Updates the ranges given a new instance.
   * 
   * @param instance 	the new instance
   * @param ranges 	low, high and width values for all attributes
   * @return		the updated ranges
   */  //being used in other classes (KDTree)
  public double[][] updateRanges(Instance instance, double[][] ranges) {
    
    // updateRangesFirst must have been called on ranges
    for (int j = 0; j < ranges.length; j++) {
      double value = instance.value(j);
      if (!instance.isMissing(j)) {
        if (value < ranges[j][R_MIN]) {
          ranges[j][R_MIN] = value;
          ranges[j][R_WIDTH] = ranges[j][R_MAX] - ranges[j][R_MIN];
        } else {
          if (instance.value(j) > ranges[j][R_MAX]) {
            ranges[j][R_MAX] = value;
            ranges[j][R_WIDTH] = ranges[j][R_MAX] - ranges[j][R_MIN];
          }
        }
      }
    }
    return ranges;
  }
  
  /**
   * Update the ranges if a new instance comes.
   * 
   * @param instance 	the new instance
   */ //Being used in KDTree
  public void updateRanges(Instance instance) {
    m_Ranges = updateRanges(instance, m_Ranges);
  }
  
  /**
   * prints the ranges.
   * 
   * @param ranges 	low, high and width values for all attributes
   */  //Not being used in any other class. Not even being used in this class.
  public void printRanges(double[][] ranges) {
    
    OOPS("printRanges");
    // updateRangesFirst must have been called on ranges
    for (int j = 0; j < ranges.length; j++) {
      OOPS(" "+j+"-MIN "+ranges[j][R_MIN]);
      OOPS(" "+j+"-MAX "+ranges[j][R_MAX]);
      OOPS(" "+j+"-WIDTH "+ranges[j][R_WIDTH]);
    }
  }
  
  
  /**
   * Test if an instance is within the given ranges.
   * 
   * @param instance 	the instance
   * @param ranges 	the ranges the instance is tested to be in
   * @return true 	if instance is within the ranges
   */  //being used in IBk but better to remove from there.
  public boolean inRanges(Instance instance, double[][] ranges) {
    boolean isIn = true;
    
    // updateRangesFirst must have been called on ranges
    for (int j = 0; isIn && (j < ranges.length); j++) {
      if (!instance.isMissing(j)) {
        double value = instance.value(j);
        isIn = value <= ranges[j][R_MAX];
        if (isIn) isIn = value >= ranges[j][R_MIN];
      }
    }
    return isIn;
  }
  
  /**
   * Prints a range to standard output.
   * 
   * @param model	the instances this ranges are for
   * @param ranges 	the ranges to print
   */   //Not being used in any other class. Not even being used in this class.
  public void printRanges(Instances model, double[][] ranges) {
    System.out.println("printRanges");
    for (int j = 0; j < model.numAttributes(); j++) {
      System.out.print("Attribute "+ j +" MIN: " + ranges[j][R_MIN]);
      System.out.print(" MAX: " + ranges[j][R_MAX]);
      System.out.print(" WIDTH: " + ranges[j][R_WIDTH]);
      System.out.println(" ");
    }
  }
  
  /**
   * Check if ranges are set.
   * 
   * @return 		true if ranges are set
   */  //Not being used in any other class
  public boolean rangesSet() {
    return (m_Ranges != null);
  }
  
  /**
   * Method to get the ranges.
   * 
   * @return 		the ranges
   * @throws Exception	if no randes are set yet
   */  //Not being used in any other class
  public double[][] getRanges() throws Exception {
    if (m_Ranges == null)
      throw new Exception("Ranges not yet set.");
    return m_Ranges;
  }
  
  /**
   * Main method for testing this class.
   * 
   * @param args	the commandline parameters
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
