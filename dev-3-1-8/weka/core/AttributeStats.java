/*
 *    AttributeStats.java
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
package weka.core;

/**
 * A Utility class that contains summary information on an
 * the values that appear in a dataset for a particular attribute.
 */
public class AttributeStats {    
  
  /** The number of int-like values */
  public int intCount = 0;
  
  /** The number of real-like values (i.e. have a fractional part) */
  public int realCount = 0;
  
  /** The number of missing values */
  public int missingCount = 0;
  
  /** The number of distinct values */
  public int distinctCount = 0;
  
  /** The number of values that only appear once */
  public int uniqueCount = 0;
  
  /** The total number of values (i.e. number of instances) */
  public int totalCount = 0;
  
  /** Stats on numeric value distributions */
  // perhaps Stats should be moved from weka.experiment to weka.core
  public weka.experiment.Stats numericStats;
  
  /** Counts of each nominal value */
  public int [] nominalCounts;
    
  /**
   * Updates the counters for one more observed distinct value.
   *
   * @param value the value that has just been seen
   * @param count the number of times the value appeared
   */
  protected void addDistinct(double value, int count) {
    
    if (count > 0) {
      if (count == 1) {
	uniqueCount++;
	}
      if (Utils.eq(value, (double)((int)value))) {
	intCount += count;
      } else {
	realCount += count;
      }
      if (nominalCounts != null) {
	nominalCounts[(int)value] = count;
      }
      if (numericStats != null) {
	  numericStats.add(value, count);
	  numericStats.calculateDerived();
      }
    }
    distinctCount++;
  }
}
