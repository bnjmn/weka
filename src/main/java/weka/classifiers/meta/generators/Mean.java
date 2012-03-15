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
 *   Mean.java
 *   Copyright (C) 2008 K.Hempstalk, University of Waikato, Hamilton, New Zealand.
 */

package weka.classifiers.meta.generators;

/**
 * A interface indicating that a class expects the 
 * mean and standard deviation to be set.
 *
 * @author Kathryn Hempstalk (kah18 at cs.waikato.ac.nz)
 * @version $Revision$
 */
public interface Mean {

  /**
   * Sets the mean of the Gaussian distribution to a new 
   * mean.
   *
   * @param newmean The new mean for the distribution.
   */
  public void setMean(double newmean);

  /**
   * Sets the standard deviation of the Gaussian distribution
   * to a new value.
   *
   * @param newsd The new standard deviation.
   */
  public void setStandardDeviation(double newsd);
}
