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
 *    NearestNeighbourSearch.java
 *    Copyright (C) 1999-2005 University of Waikato
 */

package weka.core;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.Instance;
import weka.core.Instances;

/**
 * Abstract class for nearest neighbour search. All algorithms (classes) that 
 * do nearest neighbour search should extend this class. 
 *
 * @author  Ashraf M. Kibriya (amk14@waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public abstract class NearestNeighbourSearch implements Serializable{
  
  /** The neighbourhood of instances to find neighbours in. */
  protected Instances m_Instances;
  
  /** The number of neighbours to find */
  protected int m_kNN;

  /** the distance function used */
  protected DistanceFunction m_DistanceFunction = new EuclideanDistance();

  /** Constructor */
  public NearestNeighbourSearch() {
  }
  
  /** Constructor 
   *  @param insts - The set of instances that constitute the neighbourhood.
   */
  public NearestNeighbourSearch(Instances insts) {
    m_Instances = insts;
  }

  /** Returns the nearest instance in the current neighbourhood to the supplied
   *  instance.
   * @param target - The instance to find the nearest neighbour for.
   * @exception - Throws an exception if the nearest neighbour could not be 
   *              found.
   */
  public abstract Instance nearestNeighbour(Instance target) throws Exception;
  
  /** Returns k nearest instances in the current neighbourhood to the supplied
   *  instance.
   * @param target - The instance to find the k nearest neighbours for.
   * @param k - The number of nearest neighbours to find.
   * @exception - Throws an exception if the neighbours could not be found.
   */
  public abstract Instances kNearestNeighbours(Instance target, int k) throws Exception;
 
  /** Returns the distances of the k nearest neighbours. The kNearestNeighbours
   *  or nearestNeighbour needs to be called first for this to work.
   *
   * @exception Throws an exception if called before calling kNearestNeighbours
   *            or nearestNeighbours.
   */
  public abstract double[] getDistances() throws Exception;
  
  /**
   * Updates the NearNeighbourSearch algorithm for the new added instance.
   */
  public abstract void update(Instance ins) throws Exception;

  /** 
   * Adds information from the given instance without modifying the 
   * datastructures a lot 
   */
  public void addInstanceInfo(Instance ins) {
  }
  
  /** Sets the instances */
  public void setInstances(Instances insts) throws Exception {
    m_Instances = insts;
  }
  
  /** returns the instances currently set */
  public Instances getInstances() {
    return m_Instances;
  }
  
   /**  Tip text for the property  */
  public String distanceFunctionTipText() {
    return "The distance function to use for finding neighbours " +
           "(default: weka.core.EuclideanDistance). ";
  }
  
  /** returns the distance function currently in use */
  public DistanceFunction getDistanceFunction() {
    return m_DistanceFunction;
  }
  
  /** sets the  distance function to use for nearest neighbour search */
  public void setDistanceFunction(DistanceFunction df) throws Exception {
    m_DistanceFunction = df;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector();
    newVector.add(new Option("\tDistance function to use.\n",
                             "A", 1,"-A"));
    return newVector.elements();
  }
  
  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    String nnSearchClass = Utils.getOption('A', options);
    if(nnSearchClass.length() != 0) {
      String nnSearchClassSpec[] = Utils.splitOptions(nnSearchClass);
      if(nnSearchClassSpec.length == 0) { 
        throw new Exception("Invalid DistanceFunction specification string."); 
      }
      String className = nnSearchClassSpec[0];
      nnSearchClassSpec[0] = "";

      setDistanceFunction( (DistanceFunction)
                            Utils.forName( DistanceFunction.class, 
                                           className, nnSearchClassSpec) );
    }
    else 
      this.setDistanceFunction(new EuclideanDistance());  
  }

  /**
   * Gets the current settings of IBk.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String [] getOptions() {

    String [] options = new String[2];

    options[0] = "-A";
    options[1] =   m_DistanceFunction.getClass().getName() 
                 + new String(" " + Utils.joinOptions(m_DistanceFunction.getOptions())).trim(); 
    
    return options;
  }

  /** 
   * sorts the two given arrays.
   * @param arrayToSort - The array sorting should be based on.
   * @param linkedArray - The array that should have the same ordering as 
   * arrayToSort.
   */
  public static void combSort11(double arrayToSort[], int linkedArray[]) {
    int switches, j, top, gap, size;
    double hold1; int hold2;
    gap = arrayToSort.length;
    do {
      gap=(int)(gap/1.3);
      switch(gap) {
        case 0:
          gap = 1;
          break;
        case 9:
        case 10:
          gap=11;
          break;
        default:
          break;
      }
      switches=0;
      top = arrayToSort.length-gap;
      for(int i=0; i<top; i++) {
        j=i+gap;
        if(arrayToSort[i] > arrayToSort[j]) {
          hold1=arrayToSort[i];
          hold2=linkedArray[i];
          arrayToSort[i]=arrayToSort[j];
          linkedArray[i]=linkedArray[j];
          arrayToSort[j]=hold1;
          linkedArray[j]=hold2;
          switches++;
        }//endif
      }//endfor
    } while(switches>0 || gap>1);
  }
   
}
