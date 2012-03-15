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
 *    SufficientBagStatistics.java
 *    Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.classifiers.mi.miti;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import weka.core.Instance;


/**
 * Class that maintains sufficient statistics at the bag level.
 *
 * @author Luke Bjerring
 * @version $Revision$
 */
public class SufficientBagStatistics implements SufficientStatistics {

  // The number of each instances from each (positive) bag in each subset
  private HashMap<Bag, Integer> leftPositiveBags;
  private HashMap<Bag, Integer> rightPositiveBags;
  private HashMap<Bag, Integer> leftTotalBags;
  private HashMap<Bag, Integer> rightTotalBags;
  
  // The parameter that determines the influence of the number of instances
  // that the bag has in the corresponding subset.
  private double m_instanceConstant;
  
  // The (weighted) number of positive and total cases respectively.
  private double positiveCountLeft;
  private double totalCountLeft;
  private double positiveCountRight;
  private double totalCountRight;
  
  /**
   * Sets up the object initially by assigning all cases to the right subset.
   */
  public SufficientBagStatistics(List<Instance> allInstances, HashMap<Instance, Bag> instanceBags, double instanceConstant)
  {
    m_instanceConstant = instanceConstant;

    leftPositiveBags = new HashMap<Bag, Integer>();
    rightPositiveBags = new HashMap<Bag, Integer>();
    leftTotalBags = new HashMap<Bag, Integer>();
    rightTotalBags = new HashMap<Bag, Integer>();
    
    for (Instance i : allInstances)
      {
        Bag bag = instanceBags.get(i);
        if (bag.isPositive())
          {
            if (rightPositiveBags.containsKey(bag))
              rightPositiveBags.put(bag, rightPositiveBags.get(bag) + 1);
            else
              rightPositiveBags.put(bag, 1);				
          }
        
        if (rightTotalBags.containsKey(bag))
          rightTotalBags.put(bag, rightTotalBags.get(bag) + 1);
        else
          rightTotalBags.put(bag, 1);	
      }
    
    totalCountRight = 0;
    positiveCountRight = 0;
    for (Entry<Bag, Integer> e : rightTotalBags.entrySet())
      {

        // Rather than giving each bag a weight of one, we weight
        // it based on the number of instances it has in the subset
        // based on instanceConstant^(number of instances)
        double weight = 1 - Math.pow(instanceConstant, e.getValue()); 
        if (e.getKey().isPositive()) {
          positiveCountRight += weight;
        }
        totalCountRight += weight;
      }
    totalCountLeft = 0;
    positiveCountLeft = 0;
  }
	
  /**
   * Updates the sufficient statistics assuming a shift of instance i from the right of the split to the left
   */
  public void updateStats(Instance i, HashMap<Instance, Bag> instanceBags)
  {

    Bag bag = instanceBags.get(i);
    boolean positive = bag.isPositive();
    double prob = m_instanceConstant;
    
    if (positive)
      {
        int countRP = rightPositiveBags.get(bag);
        positiveCountRight += Math.pow(prob, countRP - 1) * (prob - 1);
        //                          positiveCountRight -= (1 - Math.pow(prob, countRP));
        //                          positiveCountRight += (1 - Math.pow(prob, countRP - 1));
        rightPositiveBags.put(bag, countRP - 1);
        
        if (leftPositiveBags.containsKey(bag)) {
          int countLP = leftPositiveBags.get(bag);
          positiveCountLeft += Math.pow(prob, countLP) * (1 - prob);
          //                            positiveCountLeft -= (1 - Math.pow(prob, countLP));
          //                            positiveCountLeft += (1 - Math.pow(prob, countLP + 1));
          leftPositiveBags.put(bag, countLP + 1);
        } else {
          leftPositiveBags.put(bag, 1);	
          positiveCountLeft += (1 - prob);
        }
      }

    int countRT = rightTotalBags.get(bag);
    totalCountRight += Math.pow(prob, countRT - 1) * (prob - 1);
    
    //                  totalCountRight -= (1 - Math.pow(prob, countRT));
    //                  totalCountRight += (1 - Math.pow(prob, countRT - 1));
    rightTotalBags.put(bag, countRT - 1);
    
    if (leftTotalBags.containsKey(bag)) {
      int countLT = leftTotalBags.get(bag);
      totalCountLeft += Math.pow(prob, countLT) * (1 - prob);
      //                    totalCountLeft -= (1 - Math.pow(prob, countLT));
      //                    totalCountLeft += (1 - Math.pow(prob, countLT + 1));
      leftTotalBags.put(bag, countLT + 1);
    } else {
      leftTotalBags.put(bag, 1);	
      totalCountLeft += (1 - prob);
    }
  }

  /**
   * The number of positive cases on the left side.
   */
  @Override
    public double positiveCountLeft() {
    return positiveCountLeft;
  }


  /**
   * The number of positive cases on the right side.
   */
  @Override
    public double positiveCountRight() {
    return positiveCountRight;
  }
  

  /**
   * Number of cases on the left side.
   */  
  @Override
    public double totalCountLeft() {
    return totalCountLeft;
  }

  /**
   * Number of cases on the right side.
   */  
  @Override
    public double totalCountRight() {
    return totalCountRight;
  }
}
