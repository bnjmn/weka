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

/**
 * ResampleUtils.java
 * Copyright (C) 2015 University of Waikato, Hamilton, NZ
 */

package weka.core;

import java.util.Random;

/**
 * Helper class for resampling.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class ResampleUtils {

  /**
   * Checks whether there are any instance weights other than 1.0 set.
   *
   * @param insts	the dataset to check
   * @return		true if instance weights other than 1.0 are set
   */
  public static boolean hasInstanceWeights(Instances insts) {
    boolean result = false;
    for (int i = 0; i < insts.numInstances(); i++) {
      if (insts.instance(i).weight() != 1.0) {
        result = true;
        break;
      }
    }
    return result;
  }

  /**
   * Resamples the dataset using {@link Instances#resampleWithWeights(Random)}
   * if there are any instance weights other than 1.0 set. Simply returns the
   * dataset if no instance weights other than 1.0 are set.
   *
   * @param insts	the dataset to resample
   * @param rand	the random number generator to use
   * @return		the (potentially) resampled dataset
   */
  public static Instances resampleWithWeightIfNecessary(Instances insts, Random rand) {
    if (hasInstanceWeights(insts))
      return insts.resampleWithWeights(rand);
    else
      return insts;
  }
}
