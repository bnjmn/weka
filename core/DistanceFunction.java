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
 *    DistanceFunction.java
 *    Copyright (C) 2002 University of Waikato
 *
 */

package weka.core;
import java.io.Serializable;
import java.io.*;

/**
 * Abstract class to implement a distance function.
 * @author Gabi Schmidberger (gabi@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public abstract class DistanceFunction implements Serializable {

  /** the data model */
  protected Instances m_Model;

  /** the range of the attributes */
  public double [][] m_Ranges;

  /**
   * Index in ranges for MIN and MAX and WIDTH
   */
  protected static final int R_MIN = 0;
  protected static final int R_MAX = 1;
  protected static final int R_WIDTH = 2;

  /**
   * Constructs a distance function object.
   */
  public DistanceFunction() {
  }

  /**
   * Constructs a distance function object.
   * @param data the instances the distance function should work on.
   */
  public DistanceFunction(Instances data) {
     
    // make list of indexes for m_Instances
    int [] allInstList = new int[data.numInstances()]; 
    for (int i = 0; i < data.numInstances(); i++) {
      allInstList[i] = i;
    }
    // prepare the min and max value    
    m_Ranges = data.initializeRanges(allInstList);
    // 
    m_Model = new Instances(data, 0);


  }

  /**
   * Constructs a distance function object. Ranges are already given
   * @param data the instances the distance function should work on.
   * @param ranges the min and max values of the attribute values
   */
  public DistanceFunction(Instances data, double [][] ranges) {
    
    // copy the ranges (the min and max values)    
    m_Ranges = ranges;
    // copy the data model
    m_Model = new Instances(data, 0);
  }

  /**
   * Calculates the distance (or similarity) between two instances.
   * @param first the first instance
   * @param second the second instance
   * @return the distance between the two given instances,
   */
  public abstract double distance(Instance first, Instance second)
    throws Exception;  


  /**
   * Returns value in the middle of the two parameter values.
   * @param range the ranges to this dimension
   * @return the middle value
   */
  public abstract double getMiddle(double[] ranges);

  /**
   * Checks the instances if compatibel with the distance function.
   * @param range the ranges to this dimension
   * @return the middle value
   */
  public abstract void checkInstances() throws Exception;

  /**
   * Returns true if the value of the given dimension is smaller or equal the 
   * value to be compared with.
   * @param instance the instance where the value should be taken of
   * @param dim the dimension of the value
   * @param the value to compare with
   * @return true is value of instance is smaller or equal value
   */
  public abstract boolean valueIsSmallerEqual(Instance instance, int dim,
					      double value);

   /**
   * Returns the index of the closest point to the current instance. 
   * Index is index in Instances object that is the second parameter.
   *
   * @param instance the instance to assign a cluster to
   * @param centers all centers 
   * @param centList the centers to cluster the instance to
   * @return a cluster index
   */
  public int closestPoint(Instance instance, 
			   Instances allPoints,
			   int [] pointList) throws Exception {
    double minDist = Integer.MAX_VALUE;
    int bestPoint = 0;
    for (int i = 0; i < pointList.length; i++) {
      double dist = distance(instance, allPoints.instance(pointList[i]));
      if (dist < minDist) {
	minDist = dist;     
	bestPoint = i;    
      }                     
    }                         
    return pointList[bestPoint];
  }

  /**
   * Normalises a given value of a numeric attribute.
   * @param ranges the min max values of the attributes
   * @param x the value to be normalized
   * @param i the attribute's index
   */
  private double norm(double x, int i) {

    if (Double.isNaN(m_Ranges[i][R_MIN]) || 
	Utils.eq(m_Ranges[i][R_MAX], m_Ranges[i][R_MIN])) {
      return 0;
    } else {
      return (x - m_Ranges[i][R_MIN]) / (m_Ranges[i][R_WIDTH]);
    }
  }

  /**
   * Update the ranges if a new instance comes.
   * @param instance the new instance
   */
  public void updateRanges(Instance instance) {
    m_Ranges = Instances.updateRanges(instance, m_Ranges); 
  }

  /** 
   * Converts a DistanceFunction object to a string
   * @return a string describing a distance function
   */
  public String toString() {

    StringBuffer text = new StringBuffer();
    //todo
    text.append("\n");
    return text.toString();
  }

}













