/*
 *    Stats.java
 *    Copyright (C) 1999 Len Trigg
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

package weka.experiment;
import weka.core.Utils;

/**
 * A class to store simple statistics
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public class Stats {
  
  /** The number of values seen */
  public double count;

  /** The sum of values seen */
  public double sum;

  /** The sum of values squared seen */
  public double sumSq;

  /** The std deviation of values at the last calculateDerived() call */    
  public double stdDev;

  /** The mean of values at the last calculateDerived() call */    
  public double mean;

  /** The minimum value seen */
  public double min;

  /** The maximum value seen */
  public double max;
    
  /**
   * Adds a value to the observed values
   *
   * @param value the observed value
   */
  public void add(double value) {

    sum += value;
    sumSq += value * value;
    count ++;
    if (count == 1) {
      min = max = value;
    } else if (value < min) {
      min = value;
    } else if (value > max) {
      max = value;
    }
  }

  /**
   * Removes a value to the observed values (no checking is done
   * that the value being removed was actually added). 
   *
   * @param value the observed value
   */
  public void subtract(double value) {

    sum -= value;
    sumSq -= value * value;
    count --;
  }

  /**
   * Tells the object to calculate any statistics that don't have their
   * values automatically updated during add. Currently updates the mean
   * and standard deviation.
   */
  public void calculateDerived() {

    mean = Double.NaN;
    stdDev = Double.NaN;
    if (count > 0) {
      mean = sum / count;
      if (count > 1) {
	stdDev = sumSq - (sum * sum) / count;
	stdDev /= (count - 1);
	stdDev = Math.sqrt(stdDev);
      }
    }
  }
    
  /**
   * Returns a string summarising the stats so far.
   *
   * @return the summary string
   */
  public String toString() {

    calculateDerived();
    return
      "Count   " + Utils.doubleToString(count, 8) + '\n'
      + "Min     " + Utils.doubleToString(min, 8) + '\n'
      + "Max     " + Utils.doubleToString(max, 8) + '\n'
      + "Sum     " + Utils.doubleToString(sum, 8) + '\n'
      + "SumSq   " + Utils.doubleToString(sumSq, 8) + '\n'
      + "Mean    " + Utils.doubleToString(mean, 8) + '\n'
      + "StdDev  " + Utils.doubleToString(stdDev, 8) + '\n';
  }
} // Stats

