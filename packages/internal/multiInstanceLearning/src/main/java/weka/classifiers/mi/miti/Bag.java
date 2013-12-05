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
 *    Bag.java
 *    Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.mi.miti;

import java.io.Serializable;
import java.util.List;

import weka.core.Instance;
import weka.core.Instances;

/**
 * Class for maintaining a bag of instances, including its ID and whether it's
 * enabled.
 * 
 * @author Luke Bjerring
 * @version $Revision$
 */
public class Bag implements Serializable {

  /** ID added to avoid warning */
  private static final long serialVersionUID = 3890971117182125677L;

  // Whether the bag is positive
  private final boolean positive;

  // The instances in the bag
  private final Instances instances;

  // The bag's id
  private final String id;

  // The bag weight
  private final double bagWeight;

  // The multiplier for the bag weight
  private double bagWeightMultiplied;

  // Flag to indicate whether bag is enabled
  private boolean enabled = true;

  /**
   * The constructor.
   */
  public Bag(Instance bagInstance) {
    instances = bagInstance.relationalValue(1);
    positive = bagInstance.classValue() == 1.0;
    bagWeight = 1.0 / instances.numInstances();
    bagWeightMultiplied = bagWeight;
    id = bagInstance.stringValue(0);
  }

  /**
   * Returns the instances in the bag.
   */
  public Instances instances() {
    return instances;
  }

  /**
   * Is the bag positive?
   */
  public boolean isPositive() {
    return positive;
  }

  /**
   * The bag's weight.
   */
  public double bagWeight() {
    return bagWeightMultiplied;
  }

  /**
   * The multiplier for the bag weight.
   */
  public void setBagWeightMultiplier(double multiplier) {
    bagWeightMultiplied = multiplier * bagWeight;
  }

  /**
   * Is the bag enabled?
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Disables the bag.
   */
  public void disableInstances(List<String> deactivated) {
    if (enabled) {
      deactivated.add(id);
    }
    enabled = false;
  }

  /**
   * Prints all the deactivated instances to standard out.
   */
  public static void printDeactivatedInstances(List<String> deactivated) {
    System.out.print("DEACTIVATING examples [");
    System.out.print(deactivated.get(0));
    for (int i = 1; i < deactivated.size(); i++) {
      System.out.print(", " + deactivated.get(i));
    }
    System.out.println("]");
  }
}
