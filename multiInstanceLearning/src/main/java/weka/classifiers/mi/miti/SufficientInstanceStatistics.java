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
 *    SufficientInstanceStatistics.java
 *    Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.classifiers.mi.miti;

import java.util.HashMap;
import java.util.List;

import weka.core.Instance;

/**
 * Class that maintains sufficient statistics at the instance level.
 *
 * @author Luke Bjerring
 * @version $Revision$
 */
public class SufficientInstanceStatistics implements SufficientStatistics {

  // The number of positive and total cases respectively.
  private double positiveInstancesLeft;
  private double totalInstancesLeft;
  private double positiveInstancesRight;
  private double totalInstancesRight;

  /**
   * Constructs the object by initially assigning all instances to the right subset.
   */
  public SufficientInstanceStatistics(List<Instance> allInstances, HashMap<Instance, Bag> instanceBags)
  {
    positiveInstancesLeft = 0;
    positiveInstancesRight = 0;
    totalInstancesLeft = 0;
    totalInstancesRight = 0;
    for (Instance i : allInstances)
      {
        Bag bag = instanceBags.get(i);
        if (bag.isPositive())
          positiveInstancesRight += bag.bagWeight();
        totalInstancesRight += bag.bagWeight();
      }
  }
	
	
  /**
   * Updates the sufficient statistics assuming a shift of instance i from the right of the split to the left
   */
  public void updateStats(Instance i, HashMap<Instance, Bag> instanceBags)
  {
    Bag bag = instanceBags.get(i);
    boolean positive = bag.isPositive();
    if (positive)
      {
        positiveInstancesRight -= bag.bagWeight();
        positiveInstancesLeft += bag.bagWeight();
      }
    
    totalInstancesLeft += bag.bagWeight();
    totalInstancesRight -= bag.bagWeight();
  }
  
  /**
   * The number of positive cases on the left side.
   */
  @Override
    public double positiveCountLeft() {
    return positiveInstancesLeft;
  }

  /**
   * The number of positive cases on the right side.
   */
  @Override
    public double positiveCountRight() {
    return positiveInstancesRight;
  }

  /**
   * Number of cases on the left side.
   */  
  @Override
    public double totalCountLeft() {
    return totalInstancesLeft;
  }
  
  /**
   * Number of cases on the right side.
   */  
    @Override
    public double totalCountRight() {
    return totalInstancesRight;
  }
}
