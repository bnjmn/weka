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
 * Copyright (C) 2014 University of Waikato, Hamilton, NZ
 */

package weka.experiment;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Comprehensive test for the Stats class
 * 
 * @author Benjamin Weber ( benweber at student dot ethz dot ch )
 * @version $Revision 11409 $
 */
public class StatsTest extends TestCase {
  
  /**
   * The default relative tolerance to use.
   * In some places a hard coded values was needed
   */
  private final static double TOLERANCE = 1e-13;
  
  /** NaNs with different bit patterns */
  /* NaNs have the bit pattern
   * 0x7ffxxxxxxxxxxxxyL
   * 0xfffxxxxxxxxxxxxyL
   * where x in [0,f] and y in [1, f]
   */
  private final static double[] NaNs = {
    Double.NaN, Math.sqrt(-1), Math.log(-1), Math.acos(2.0),
    Double.POSITIVE_INFINITY - Double.POSITIVE_INFINITY,
    Double.longBitsToDouble(0x7ff0000000000001L),
    Double.longBitsToDouble(0x7ff0000000000003L),
    Double.longBitsToDouble(0x7ff0000000000004L),
    Double.longBitsToDouble(0x7ffffffffffffff1L),
    Double.longBitsToDouble(0x7fffffffffffffffL),
    Double.longBitsToDouble(0x7ff000fffff00001L),
    Double.longBitsToDouble(0xfff0000000000001L),
    Double.longBitsToDouble(0xfff0000000000003L),
    Double.longBitsToDouble(0xfff0000000000004L),
    Double.longBitsToDouble(0xfffffffffffffff1L),
    Double.longBitsToDouble(0xffffffffffffffffL),
    Double.longBitsToDouble(0xfff000fffff00001L)
  };
  
  private int NaN_counter = 0;
  
  private double getNaN() {
    NaN_counter = (NaN_counter + 1) % NaNs.length;
    assertTrue("Assumption violated!", Double.isNaN(NaNs[NaN_counter]));
    return NaNs[NaN_counter];
  }
  
  /** Rather arbitraty values, contains primes, 0, big numbers, no 0 weights */
  private final static double[] weightedValues1 = {
    1.0, 1.0,
    2.0, 2.0,
    3.0, 3.0,
    4.0, 4.0,
    5.0, 5.0,
    10.0, 1.0,
    100.0, 0.5,
    0.0, 1.2,
    -0.0, 1.3,
    7.0, 1.4,
    13.0, 1.5,
    1e6, 0.1,
    -1e6, 0.1
  };

  /** Rather arbitraty values, contains primes, 0, no 0 weights */
  private final static double[] weightedValues2 = {
    -0.0, 1.0,
    -1.0, 0.1,
    3.0, 3.2,
    -5.0, 1.11111,
    47, 0.5,
    -10, 1.0,
  };

  /** Rather arbitraty values, no 0 weights */
  private final static double[] weightedValues3 = {
    15.0, 2.1,
    17.34, 1.8,
    56.1452, 0.5,
    85.4758, 0.6,
    -56.8954, 1.2,
    -47.2391, 1.5846
  };
  
  /** contains big values with weights */
  private final static double[] bigWeightedValues = {
    1e6, 1.0,
    1.23456e8, 1.0/3.0,
    -4.5678e7, 3.0/5.0,
    2*2*2*2*2*2*2*2, 2.0,
    2*2*2*2*2*2*2*2*2*2*2, 0.54321,
    -1.3e8, 2.0/3.0
  };
  
  /** contains small values with weights */
  private final static double[] smallWeightedValues = {
    1e-20, 1.0,
    1e-10, 2.0,
    1.23456e-8, 0.01,
    1e-15/3.0, 0.4567,
    -1e-15, 1.0,
    -4.29384e-10, 1.5,
    1.0/2.0/2.0/2.0/2.0/2.0/2.0/2.0/2.0/2.0/2.0/2.0, 0.03
  };
 
  /**
   * The Stats class should be initialized as follows:<p>
   * 
   * Stats.count = 0<br />
   * Stats.sum = 0<br />
   * Stats.sumSq = 0<br />
   * Stats.stdDev = Double.NaN<br />
   * Stats.mean = Double.NaN<br />
   * Stats.min = Double.NaN<br />
   * stats.max = Double.NaN<br />
   */
  public void testInitialization() {
    checkStatsInitialized(getStats());
  }
  
  /**
   * Stats.stdDev should behave as follows:<p>
   * 
   * At initialization: Stats.stdDev = Double.NaN<br />
   * With 0 or 1 value(s) seen or if Stats.count <= 1: Stats.stdDev = Double.NaN<br />
   * Otherwise: Stats.stdDev >= 0 and best effort by implementation<br />
   */
  public void testStdDev() {
    Stats stats;
    
    String descr = "Incorrect stdDev!";
    /* We'll only focus on count > 0
     * because count <= 0 gets tested in testNegativeCount
     * And we'll only focus on values seen > 0
     * because values seen <= 0 gets tested in testNegativeCount
     */
    
    // test count <= 1 and values seen = 1
    stats = getStats();
    stats.add(1.0, 1.0);
    stats.calculateDerived();
    assertNaN(descr, stats.stdDev);

    stats = getStats();
    stats.add(2.0, 0.5);
    stats.calculateDerived();
    assertNaN(descr, stats.stdDev);
    
    stats = getStats();
    stats.add(-10.0, 0.10986);
    stats.calculateDerived();
    assertNaN(descr, stats.stdDev);
    
    // first add then subtract
    stats = getStats();
    addWeightedStats(stats, 1.0, 1.0, 2.0, 2.0, 3.0, 1.0);
    subtractWeightedStats(stats, 1.0, 1.0, 2.0, 2.0);
    stats.calculateDerived();
    assertNaN(descr, stats.stdDev);
    
    // first subtract then add
    stats = getStats();
    subtractWeightedStats(stats, 1.0, 1.0, 2.0, 2.0);
    addWeightedStats(stats, 1.0, 1.0, 2.0, 2.0, 3.0, 1.0);
    stats.calculateDerived();
    assertNaN(descr, stats.stdDev);
   
    // test count <= 1 and values seen > 1
    stats = getStats();
    addWeightedStats(stats, 1.0, 0.1, 2.0, 0.2, 3.0, 0.3);
    stats.calculateDerived();
    assertNaN(descr, stats.stdDev);

    stats = getStats();
    addWeightedStats(stats, 1.0, 0.2, 2.0, 0.2, 3.0, 0.4);
    stats.calculateDerived();
    assertNaN(descr, stats.stdDev);
    
    // first add then subtract
    stats = getStats();
    addWeightedStats(stats, 1.0, 0.4, 2.0, 0.2, 3.0, 0.3, 4.0, 0.2);
    subtractWeightedStats(stats, 2.0, 0.2, 4.0, 0.2);
    stats.calculateDerived();
    assertNaN(descr, stats.stdDev);
    
    // first subtract then add
    stats = getStats();
    subtractWeightedStats(stats, 2.0, 0.2, 4.0, 0.2);
    addWeightedStats(stats, 1.0, 0.4, 2.0, 0.2, 3.0, 0.3, 4.0, 0.2);
    stats.calculateDerived();
    assertNaN(descr, stats.stdDev);
    
    // test count > 1 and values seen > 1
    // We won't test this case as it's tested in other testcases
    
  }
  
  /**
   * If the count is negative then all fields except the count are set to
   * <code>Double.NaN</code> and the count gets tracked normally.<p>
   * 
   * If count = 0, then the Stats object gets reset.<p>
   * 
   * If count > 0 for the first time then the Stats object gets reset and
   * updated with the corresponding value and weight.<p>
   */
  public void testNegativeCount() {
    
    Stats stats;
    Stats reference;
    
    // go negative right from the start
    stats = getStats();
    subtractWeightedStats(stats, weightedValues1);
    checkStatsNegativeCount(stats);
    stats.calculateDerived();
    checkStatsNegativeCount(stats);
    
    // go first positive and only afterwards negative
    stats = getStats();
    addWeightedStats(stats, weightedValues1);
    stats.calculateDerived();
    checkStatsValidState(stats);
    subtractWeightedStats(stats, weightedValues1);
    checkStatsInitialized(stats);
    stats.calculateDerived();
    checkStatsInitialized(stats);
    subtractWeightedStats(stats, weightedValues2);
    checkStatsNegativeCount(stats);
    stats.calculateDerived();
    checkStatsNegativeCount(stats);

    // go first negative and then recover
    reference = getStats();
    stats = getStats();
    subtractWeightedStats(stats, weightedValues1);
    checkStatsNegativeCount(stats);
    addWeightedStats(stats, weightedValues1);
    checkStatsInitialized(stats);
    stats.calculateDerived();
    checkStatsInitialized(stats);
    addWeightedStats(stats, weightedValues1);
    stats.calculateDerived();
    checkStatsValidState(stats);
    addWeightedStats(reference, weightedValues1);
    reference.calculateDerived();
    checkStats(stats, "Incorrect behaviour after negative count recovry!",
        reference, 0.0);
    
    // go first positive, then negative, then recover
    stats = getStats();
    reference = getStats();
    addWeightedStats(stats, weightedValues3);
    stats.calculateDerived();
    checkStatsValidState(stats);
    subtractWeightedStats(stats, weightedValues3);
    checkStatsInitialized(stats);
    stats.calculateDerived();
    checkStatsInitialized(stats);
    subtractWeightedStats(stats, weightedValues2);
    checkStatsNegativeCount(stats);
    stats.calculateDerived();
    checkStatsNegativeCount(stats);
    subtractWeightedStats(stats, weightedValues1);
    checkStatsNegativeCount(stats);
    stats.calculateDerived();
    checkStatsNegativeCount(stats);
    addWeightedStats(stats, weightedValues2);
    checkStatsNegativeCount(stats);
    stats.calculateDerived();
    checkStatsNegativeCount(stats);
    addWeightedStats(stats, weightedValues1);
    checkStatsInitialized(stats);
    stats.calculateDerived();
    checkStatsInitialized(stats);
    addWeightedStats(stats, weightedValues3);
    stats.calculateDerived();
    checkStatsValidState(stats);
    addWeightedStats(reference, weightedValues3);
    reference.calculateDerived();
    checkStats(stats, "Incorrect behaviour after negative count recovery!",
        reference, 0.0);
    
  }
  
  /**
   * If values have only been added then Stats.min and Stats.max should be
   * completely accurate.<p>
   * If values have also been removed then<p>
   * <p>
   * min(values_added \ values_subtracted) >= Stats.min >= min(values_added)<p>
   * max(values_added \ values_subtracted) <= Stats.max <= max(values_added)<p>
   * <p>
   * where \ is the set difference.<p>
   */
  public void testMinMax() {
    
    Stats stats;
    stats = getStats();

    // start off with small values:
    addWeightedStats(stats, smallWeightedValues);
    double max_simple = getWeightedMax(smallWeightedValues);
    double min_simple = getWeightedMin(smallWeightedValues);
    assertEquals("Incorrect min!", min_simple, stats.min);
    assertEquals("Incorrect max!", max_simple, stats.max);

    // change min/max
    assertTrue("Assumption violated in testcase!",
        max_simple < getWeightedMax(weightedValues1));
    assertTrue("Assumption violated in testcase!",
        min_simple > getWeightedMin(weightedValues1));
    addWeightedStats(stats, weightedValues1);
    max_simple = Math.max(max_simple, getWeightedMax(weightedValues1));
    min_simple = Math.min(min_simple, getWeightedMin(weightedValues1));
    assertEquals("Incorrect min!", min_simple, stats.min);
    assertEquals("Incorrect max!", max_simple, stats.max);

    // check that min/max don't change if existing values have been added
    addWeightedStats(stats, weightedValues1);
    assertEquals("Incorrect min!", min_simple, stats.min);
    assertEquals("Incorrect max!", max_simple, stats.max);

    // add different values, but don't change min/max
    addWeightedStats(stats, weightedValues2);
    // keep track of min/max of values_added \ values_subtracted
    double max_real = max_simple;
    double min_real = min_simple;
    max_simple = Math.max(max_simple, getWeightedMax(weightedValues2));
    min_simple = Math.min(min_simple, getWeightedMin(weightedValues2));
    assertEquals("Incorrect min!", min_simple, stats.min);
    assertEquals("Incorrect max!", max_simple, stats.max);

    // subtract existing values
    subtractWeightedStats(stats, weightedValues2);
    assertEquals("Assumption violated in testcase!", max_simple, max_real);
    assertEquals("Assumption violated in testcase!", min_simple, min_real);
    assertTrue("Incorrect min!", min_real >= stats.min && stats.min >= min_simple);
    assertTrue("Incorrect max!", max_real <= stats.max && stats.max <= max_simple);
    
    // add values after subtracting
    addWeightedStats(stats, weightedValues3);
    max_simple = Math.max(max_simple, getWeightedMax(weightedValues3));
    min_simple = Math.min(min_simple, getWeightedMin(weightedValues3));
    max_real = Math.max(max_real, getWeightedMax(weightedValues3));
    min_real = Math.min(min_real, getWeightedMin(weightedValues3));
    assertTrue("Incorrect min!", min_real >= stats.min && stats.min >= min_simple);
    assertTrue("Incorrect max!", max_real <= stats.max && stats.max <= max_simple);
    
    // add bigger values
    addWeightedStats(stats, bigWeightedValues);
    max_simple = Math.max(max_simple, getWeightedMax(bigWeightedValues));
    min_simple = Math.min(min_simple, getWeightedMin(bigWeightedValues));
    assertEquals("Incorrect min!", min_simple, stats.min);
    assertEquals("Incorrect max!", max_simple, stats.max);
    
    // subtract bigger values
    subtractWeightedStats(stats, weightedValues2);
    assertTrue("Assumption violated in testcase!", max_simple > max_real);
    assertTrue("Assumption violated in testcase!", min_simple < min_real);
    assertTrue("Incorrect min!", min_real >= stats.min && stats.min >= min_simple);
    assertTrue("Incorrect max!", max_real <= stats.max && stats.max <= max_simple);

  }
  
  /**
   * weight = +-Inf or weight = NaN: go into invalid state<br />
   * Value = +-Inf or value = NaN: go into invalid state<br />
   */
  public void testInvalidState() {
    
    final double[] invalidValues = {
        Double.POSITIVE_INFINITY,
        Double.NEGATIVE_INFINITY,
        getNaN(), getNaN(), getNaN(), getNaN(), getNaN()
    };
    
    final double[] validValues = {
        0.0, 1.0, 2.0, 2.0, 3.0, 10.0,
        -0.0, -1.0, -2.0, -2.0, -3.0, -10.0
    };

    for (double value : invalidValues) {
      for (double weight : validValues) {
        goIntoInvalidState(value, weight);
      }
      for (double weight : invalidValues) {
        goIntoInvalidState(value, weight);
      }
    }
    for (double value : validValues) {
      for (double weight : invalidValues) {
        goIntoInvalidState(value, weight);
      }
    }
    
  }
  
  /**
   * weight = 0.0: Should be ignored<br />
   */
  public void testZeroWeights() {

    String descr = "Ignoring 0 weights";
    Stats reference = getStats();
    Stats test = getStats();

    addWeightedStats(test, 1.0, 0.0, 2.0, 0.0, 3.0, 0.0, -10.0, 0.0, 10.0, 0.0);
    
    checkStatsInitialized(test);
    checkStats(test, descr, reference, 0.0);
    reference.calculateDerived();
    test.calculateDerived();
    checkStats(test, descr, reference, 0.0);

    addWeightedStats(reference, weightedValues1);
    addWeightedStats(test, weightedValues1);

    addWeightedStats(test, 1.0, 0.0, 2.0, 0.0, 3.0, 0.0, -10.0, 0.0, 10.0, 0.0);
    
    checkStats(test, descr, reference, 0.0);
    reference.calculateDerived();
    test.calculateDerived();
    checkStats(test, descr, reference, 0.0);
    
    addWeightedStats(reference, weightedValues2);
    addWeightedStats(test, weightedValues2);

    subtractWeightedStats(test,
        Double.MAX_VALUE, 0.0,
        Double.MAX_VALUE, -0.0, -Double.MAX_VALUE, 0.0, -Double.MAX_VALUE, -0.0,
        Double.MIN_VALUE, 0.0, Double.MIN_VALUE, -0.0, -Double.MIN_VALUE, 0.0,
        -Double.MIN_VALUE, -0.0
    );

    checkStats(test, descr, reference, 0.0);
    reference.calculateDerived();
    test.calculateDerived();
    checkStats(test, descr, reference, 0.0);
    
    subtractWeightedStats(reference, weightedValues1);
    subtractWeightedStats(test, weightedValues1);

    subtractWeightedStats(test, 
        0.0, 0.0, -0.0, -0.0, 1e20, -0.0, 30, -0.0
    );

    checkStats(test, descr, reference, 0.0);
    reference.calculateDerived();
    test.calculateDerived();
    checkStats(test, descr, reference, 0.0);
    
    addWeightedStats(reference, smallWeightedValues);
    addWeightedStats(test, smallWeightedValues);

    addWeightedStats(test,
        Double.MAX_VALUE, 0.0,
        Double.MAX_VALUE, -0.0, -Double.MAX_VALUE, 0.0, -Double.MAX_VALUE, -0.0,
        Double.MIN_VALUE, 0.0, Double.MIN_VALUE, -0.0, -Double.MIN_VALUE, 0.0,
        -Double.MIN_VALUE, -0.0
    );

    checkStats(test, descr, reference, 0.0);
    reference.calculateDerived();
    test.calculateDerived();
    checkStats(test, descr, reference, 0.0);

    addWeightedStats(reference, bigWeightedValues);
    addWeightedStats(test, bigWeightedValues);

    addWeightedStats(test, 
        0.0, 0.0, 0.0, -0.0, -0.0, 0.0, -0.0, -0.0, 20, 0.0, 20, -0.0
    );

    checkStats(test, descr, reference, 0.0);
    reference.calculateDerived();
    test.calculateDerived();
    checkStats(test, descr, reference, 0.0);
    
  }

  /**
   * weight < 0: For add(...) should be like subtract(...) and vice versa<br />
   */
  public void testNegativeWeights() {
    
    String descr = "Incorrect handling of negative weights";
    Stats reference = getStats();
    Stats test = getStats();
    
    final double[] positiveWeights = {
        1.0, 10.0, 2.0, 2.0, 3.0, 3.0, 4.0, 4.0, 5.0, 5.0, 10.0, 0.1, -10.0, 0.1,
        100.0, 2.3, -100.0, 2.4
    };
    final double[] negativeWeights = {
        1.0, -10.0, 2.0, -2.0, 3.0, -3.0, 4.0, -4.0, 5.0, -5.0, 10.0, -0.1, -10.0, -0.1,
        100.0, -2.3, -100.0, -2.4
    };

    addWeightedStats(reference, positiveWeights);
    subtractWeightedStats(test, negativeWeights);

    reference.calculateDerived();
    test.calculateDerived();
    checkStats(test, descr, reference, 0.0);

    addWeightedStats(reference, weightedValues1);
    addWeightedStats(test, weightedValues1);

    subtractWeightedStats(reference, positiveWeights);
    addWeightedStats(test, negativeWeights);

    reference.calculateDerived();
    test.calculateDerived();
    checkStats(test, descr, reference, 0.0);
    
    addWeightedStats(reference, weightedValues2);
    addWeightedStats(test, weightedValues2);

    addWeightedStats(reference, positiveWeights);
    subtractWeightedStats(test, negativeWeights);

    reference.calculateDerived();
    test.calculateDerived();
    checkStats(test, descr, reference, 0.0);
    
    subtractWeightedStats(reference, weightedValues1);
    subtractWeightedStats(test, weightedValues1);

    reference.calculateDerived();
    test.calculateDerived();
    checkStats(test, descr, reference, 0.0);

  }
  
  /**
   * Tests whether stdDev and mean are accurate
   */
  public void testAccuracy() {

    checkAccuracy(
        generatedWeightedValues1,
        generatedMean1,
        generatedStdDev1
        );

    checkAccuracy(
        generatedWeightedValues2,
        generatedMean2,
        generatedStdDev2
        );

    checkAccuracy(
        generatedWeightedValues3,
        generatedMean3,
        generatedStdDev3
        );

    checkAccuracy(
        generatedWeightedValues4,
        generatedMean4,
        generatedStdDev4,
        1e-2
        );

    checkAccuracy(
        generatedWeightedValues5,
        generatedMean5,
        generatedStdDev5,
        1e-2
        );

    checkAccuracy(
        generatedWeightedValues6,
        generatedMean6,
        generatedStdDev6,
        1e-2
        );

    checkAccuracy(
        generatedWeightedValues7,
        generatedMean7,
        generatedStdDev7
        );

    checkAccuracy(
        generatedWeightedValues8,
        generatedMean8,
        generatedStdDev8
        );

    checkAccuracy(
        generatedWeightedValues9,
        generatedMean9,
        generatedStdDev9
        );

    checkAccuracy(
        generatedWeightedValues10,
        generatedMean10,
        generatedStdDev10
        );

    checkAccuracy(
        generatedWeightedValues11,
        generatedMean11,
        generatedStdDev11
        );

    checkAccuracy(
        generatedWeightedValues12,
        generatedMean12,
        generatedStdDev12
        );

    checkAccuracy(
        generatedWeightedValues13,
        generatedMean13,
        generatedStdDev13
        );

    checkAccuracy(
        generatedWeightedValues14,
        generatedMean14,
        generatedStdDev14,
        1
        );

    checkAccuracy(
        generatedWeightedValues15,
        generatedMean15,
        generatedStdDev15
        );

    checkAccuracy(
        generatedWeightedValues16,
        generatedMean16,
        generatedStdDev16,
        1
        );

    checkAccuracy(
        generatedWeightedValues17,
        generatedMean17,
        generatedStdDev17
        );

    checkAccuracy(
        generatedWeightedValues18,
        generatedMean18,
        generatedStdDev18,
        2
        );

    checkAccuracy(
        generatedWeightedValues19,
        generatedMean19,
        generatedStdDev19
        );

    checkAccuracy(
        generatedWeightedValues20,
        generatedMean20,
        generatedStdDev20,
        1
        );

    checkAccuracy(
        generatedWeightedValues21,
        generatedMean21,
        generatedStdDev21
        );

    checkAccuracy(
        generatedWeightedValues22,
        generatedMean22,
        generatedStdDev22
        );

  }
  
  private void checkAccuracy(long[] weightedValues, long mean, long stdDev) {
    checkAccuracy(weightedValues, mean, stdDev, TOLERANCE);
  }
  
  private void checkAccuracy(long[] weightedValues, long mean, long stdDev,
      double tolerance) {

    Stats stats = getStats();
    addWeightedStats(stats, weightedValues);
    stats.calculateDerived();
    
    Stats subtracted1 = getStats();
    addWeightedStatsWithSubtracts(subtracted1, weightedValues, weightedValues,
        3, 3);
    subtracted1.calculateDerived();

    Stats subtracted2 = getStats();
    addWeightedStatsWithSubtracts(subtracted2, weightedValues, weightedValues,
        2, 4);
    subtracted2.calculateDerived();

    
    double meanDouble = Double.longBitsToDouble(mean);
    double stdDevDouble = Double.longBitsToDouble(stdDev);

    assertEquals("Inaccurate mean calculation!", meanDouble,
        stats.mean, Math.abs(meanDouble*tolerance));
    assertEquals("Inaccurate stdDev calculation!", stdDevDouble,
        stats.stdDev, Math.abs(stdDevDouble*tolerance));

    assertEquals("Inaccurate mean calculation when using subtract!", meanDouble,
        subtracted1.mean, Math.abs(meanDouble*tolerance));
    assertEquals("Inaccurate stdDev calculation when using subtract!", stdDevDouble,
        subtracted1.stdDev, Math.abs(stdDevDouble*tolerance));

    assertEquals("Inaccurate mean calculation when using subtract!", meanDouble,
        subtracted2.mean, Math.abs(meanDouble*tolerance));
    assertEquals("Inaccurate stdDev calculation when using subtract!", stdDevDouble,
        subtracted2.stdDev, Math.abs(stdDevDouble*tolerance));
  }
  
  private Stats getStats() {
    return new Stats();
  }
  
  private void addWeightedStats(Stats stats, long... values) {
    assert values.length%2 == 0;
    
    for (int i = 0; i < values.length; i += 2) {
      stats.add(Double.longBitsToDouble(values[i]),
          Double.longBitsToDouble(values[i + 1])
          );
    }
    
  }
  
  /**
   * Adds the values from 'adds' to 'stats'. However every 'jump' elements one
   * value from 'subtracts' gets added as well. Once 'buffer' elements from 
   * 'subtracts' have been added the earliest value gets removed again.
   * At the end only the 'adds' have been added and 'subtracts' will have been
   * added but later subtracted.
   * 
   * @param stats the stats to modify
   * @param adds the values that will be added by the end
   * @param subtracts the values that will have been added and subtracted
   * @param jump how many values to add from 'adds' before adding from 'subtracts'
   * @param buffer how many values to add from 'subtracts' before removing them
   */
  private void addWeightedStatsWithSubtracts(Stats stats, long[] adds,
      long[] subtracts, int jump, int buffer) {

    assert adds.length%2 == 0;
    assert subtracts.length%2 == 0;
    assert subtracts.length > 0;
    assert jump > 0;
    assert buffer > 0;
    
    List<Double> b = new ArrayList<Double>(2*buffer);
    for (int i = 0, k = 0; i < adds.length; k += 2, i += 2*jump) {

      for (int j = i; j < adds.length && j < i + jump*2; j += 2) {
        double value = Double.longBitsToDouble(adds[j]);
        double weight = Double.longBitsToDouble(adds[j + 1]);
        stats.add(value, weight);
      }
      
      // wrap around if we go beyond the bound
      if (k >= subtracts.length)
        k = 0;

      double value = Double.longBitsToDouble(subtracts[k]);
      double weight = Double.longBitsToDouble(subtracts[k + 1]);
      stats.add(value, weight);
      b.add(value); b.add(weight);
      
      while (b.size() >= buffer*2) {
        stats.subtract(b.get(0), b.get(1));
        b.remove(1); b.remove(0);
      }
    }
    
    for (int i = 0; i < b.size(); i += 2) {
      stats.subtract(b.get(i), b.get(i + 1));
    }
  }
  
  private void addWeightedStats(Stats stats, double... values) {
    assert values.length % 2 == 0;
    for (int i = 0; i < values.length; i += 2) {
      stats.add(values[i], values[i + 1]);
    }
  }
  
  private void subtractWeightedStats(Stats stats, double... values) {
    assert values.length %2 == 0;
    for (int i = 0; i < values.length; i += 2) {
      stats.subtract(values[i], values[i + 1]);
    }
  }
  
  private void checkStats(Stats stats, String descr, Stats reference,
      double tolerance) {
    
    checkStats(stats, descr, reference.count, reference.sum, reference.sumSq,
        reference.mean, reference.stdDev, reference.min, reference.max, tolerance);
  }

  private void checkStats(Stats stats, String descr, double count, double sum,
      double sumSq, double mean, double stdDev, double min, double max,
      double tolerance) {
    
    if (descr == null)
      descr = "";
    else
      descr = " (" + descr + ")";
    
    assertEquals("Incorrect count" + descr + "!",
        count, stats.count, Math.abs(count*tolerance));
    assertEquals("Incorrect sum" + descr + "!",
        sum, stats.sum, Math.abs(sum*tolerance));
    assertEquals("Incorrect sumSq" + descr + "!",
        sumSq, stats.sumSq, Math.abs(sumSq*tolerance));
    assertEquals("Incorrect mean" + descr + "!",
        mean, stats.mean, Math.abs(mean*tolerance));
    assertEquals("Incorrect stdDev" + descr + "!",
        stdDev, stats.stdDev, Math.abs(stdDev*tolerance));
    assertEquals("Incorrect min" + descr + "!",
         min, stats.min, Math.abs(min*tolerance));
    assertEquals("Incorrect max" + descr + "!",
         max, stats.max, Math.abs(max*tolerance));

  }
  
  /**
   * Can be used after add/subtract/calculateDerived()
   */
  private void checkStatsInitialized(Stats stats) {
    assertTrue("Incorrect initialization for count!", 0.0 == stats.count);
    assertTrue("Incorrect initialization for sum!", 0.0 == stats.sum);
    assertTrue("Incorrect initialization for sumSq!", 0.0 == stats.sumSq);
    assertNaN("Incorrect initialization for stdDev!", stats.stdDev);
    assertNaN("Incorrect initialization for mean!", stats.mean);
    assertNaN("Incorrect initialization for min!", stats.min);
    assertNaN("Incorrect initialization for max!", stats.max);
  }
  
  private void goIntoInvalidState(double value, double weight) {
    Stats stats;
    
    // go from initialized state into invalid state via add
    stats = getStats();
    stats.add(value, weight);
    checkStatsInvalidState(stats);
    
    // go from initialized state into invalid state via subtract
    stats = getStats();
    stats.subtract(value, weight);
    checkStatsInvalidState(stats);
    
    // go from valid state into invalid state via add
    stats = getStats();
    addWeightedStats(stats, weightedValues2);
    stats.add(value, weight);
    checkStatsInvalidState(stats);
    
    // go from valid state into invalid state via subtract
    stats = getStats();
    addWeightedStats(stats, weightedValues2);
    stats.subtract(value, weight);
    checkStatsInvalidState(stats);
    
    // go from negative count state into invalid state via add
    stats = getStats();
    subtractWeightedStats(stats, weightedValues2);
    stats.add(value, weight);
    checkStatsInvalidState(stats);
    
    // go from negative count state into invalid state via subtract
    stats = getStats();
    subtractWeightedStats(stats, weightedValues2);
    stats.subtract(value, weight);
    checkStatsInvalidState(stats);
  }
  
  /**
   * Can be used after add/subtract/calculateDerived()
   */
  private void checkStatsInvalidState(Stats stats) {
    
    final double[] values = {
        0.0, 0.0, 1.0, 1.0, 2.0, 2.0, 3.0, 3.0, 4.0, 4.0,
        getNaN(), getNaN(), Double.POSITIVE_INFINITY, 1.0,
        Double.NEGATIVE_INFINITY, 1.0, getNaN(), 0.0, getNaN(), 1.0,
        0.0, getNaN(), 1.0, getNaN()
    };

    int i = 0;
    do {
      assertNaN("Incorrect count in invalid state!", stats.count);
      assertNaN("Incorrect sum in invalid state", stats.sum);
      assertNaN("Incorrect sumSq in invalid state", stats.sumSq);
      assertNaN("Incorrect mean in invalid state", stats.mean);
      assertNaN("Incorrect stdDev in invalid state", stats.stdDev);
      assertNaN("Incorrect min in invalid state", stats.min);
      assertNaN("Incorrect max in invalid state", stats.max);
      
      if (i < values.length) {
        stats.add(values[i], values[i + 1]);
      } else {
        break;
      }
      
      i += 2;

    } while (true);
  }
  
  /**
   * Can be used after add/subtract/calculateDerived()
   */
  private void checkStatsNegativeCount(Stats stats) {
    assertTrue("Incorrect negative count!", stats.count < 0);
    assertNaN("Incorrect negative count for sum!", stats.sum);
    assertNaN("Incorrect negative count for sumSq!", stats.sumSq);
    assertNaN("Incorrect negative count for stdDev!", stats.stdDev);
    assertNaN("Incorrect negative count for mean!", stats.mean);
    assertNaN("Incorrect negative count for min!", stats.min);
    assertNaN("Incorrect negative count for max!", stats.max);
  }
  
  /**
   * Should only be used after calculateDerived()
   */
  private void checkStatsValidState(Stats stats) {
    assertTrue("Incorrect count in valid state!", stats.count > 0);
    if (stats.count <= 1)
      assertTrue("Incorrect stdDev in valid state!", Double.isNaN(stats.stdDev));
    if (!Double.isNaN(stats.stdDev))
      assertTrue("Incorrect stdDev in valid state!", stats.stdDev >= 0);
    assertFalse("Incorrect mean in valid state!", Double.isNaN(stats.mean));
    assertFalse("Incorrect min in valid state!", Double.isNaN(stats.min));
    assertFalse("Incorrect max in valid state!", Double.isNaN(stats.max));
  }
  
  private double getWeightedMax(double... values) {
    assert values.length%2 == 0;

    if (values.length == 0)
      return Double.NaN;

    double max = Double.NaN;
    for (int i = 0; i < values.length; i += 2) {
      if (values[i] > max || Double.isNaN(max))
        max = values[i];
    }
    
    return max;
  }
 
  private double getWeightedMin(double... values) {
    assert values.length%2 == 0;

    if (values.length == 0)
      return Double.NaN;

    double min = Double.NaN;
    for (int i = 0; i < values.length; i += 2) {
      if (values[i] < min || Double.isNaN(min))
        min = values[i];
    }
    
    return min;
  }
  
  private void assertNaN(String msg, double nan) {
    // using Double.doubleToRawLongBits to enforce that it's Double.NaN
    // instead of e.g. 1/0, Math.sqrt(-1) etc
    assertTrue(msg,
        Double.doubleToRawLongBits(nan) == 
        Double.doubleToRawLongBits(Double.NaN));
  }
  
  public static Test suite() {
    return new TestSuite(StatsTest.class);
  }
  
  public static void main(String[] args) {
    TestRunner.run(suite());
  }
  
  /*
   * These values have been generated through python.
   * 
   * For the calculations an unlimited precision type (fractions.Fraction) has
   * been used and the values were then converted to the closest double.
   * 
   * For the standard deviation a correction factor of N-1 was used.
   * 
   * To ensure that the bit pattern doesn't get changed the values are in long
   * format.
   */

  // Generated values with parameters:
  // Count = 100
  // values_mean = 1.000000e+08; values_stdDev = 1.000000e+08; values_ordering = Random
  // weights_mean = 1.000000e+00; weights_stdDev = 0.000000e+00; weights_ordering = Random
  private static final long generatedMean1 = 4725926740328808143L; // approx ~ 9.170668e+07
  private static final long generatedStdDev1 = 4726440293820860005L; // approx ~ 9.935922e+07
  private static final long[] generatedWeightedValues1 = {4727197470564142565L, 4607182418800017408L, 4729213349068576669L, 4607182418800017408L, 4730667431839251754L, 4607182418800017408L, 4718115809603214852L, 4607182418800017408L, 4721289458254944898L, 4607182418800017408L, 4722749842532802962L, 4607182418800017408L, 4732420845593682558L, 4607182418800017408L, -4504094663693111016L, 4607182418800017408L, 4730588375177661881L, 4607182418800017408L, 4731453153138141246L, 4607182418800017408L, 4722427950751052167L, 4607182418800017408L, -4498783887221396014L, 4607182418800017408L, -4499501381621599656L, 4607182418800017408L, -4511778159487503912L, 4607182418800017408L, 4729482491038152722L, 4607182418800017408L, 4726208241789071922L, 4607182418800017408L, -4500664469574862148L, 4607182418800017408L, 4728485445167934664L, 4607182418800017408L, 4728075294746024934L, 4607182418800017408L, 4731161149560714474L, 4607182418800017408L, 4722442993638624172L, 4607182418800017408L, 4716428111926728188L, 4607182418800017408L, 4727506948740904534L, 4607182418800017408L, 4731076091249840437L, 4607182418800017408L, 4732078054916613230L, 4607182418800017408L, 4731315293515916848L, 4607182418800017408L, 4717469534979553196L, 4607182418800017408L, -4501962408553064064L, 4607182418800017408L, -4498971391449006904L, 4607182418800017408L, -4517470142127862304L, 4607182418800017408L, -4500827264460028472L, 4607182418800017408L, 4725662840764779187L, 4607182418800017408L, 4709486170107237600L, 4607182418800017408L, 4729221695337831402L, 4607182418800017408L, 4725002273267993158L, 4607182418800017408L, 4729404932930831670L, 4607182418800017408L, 4728426224032563314L, 4607182418800017408L, 4730331115003745688L, 4607182418800017408L, 4732940906287685903L, 4607182418800017408L, 4730240646212388056L, 4607182418800017408L, 4731115302300784632L, 4607182418800017408L, 4720452980321740052L, 4607182418800017408L, -4499520469529443528L, 4607182418800017408L, 4729909489847097902L, 4607182418800017408L, 4731757467372880658L, 4607182418800017408L, 4730120188882154794L, 4607182418800017408L, 4732501321640125257L, 4607182418800017408L, 4731587008324069191L, 4607182418800017408L, -4512828731113688640L, 4607182418800017408L, 4725615677950611666L, 4607182418800017408L, 4720125215655265890L, 4607182418800017408L, 4733233698035585608L, 4607182418800017408L, 4721338830567512894L, 4607182418800017408L, 4729738666856357681L, 4607182418800017408L, -4510173328326629368L, 4607182418800017408L, 4709026740999704192L, 4607182418800017408L, 4725189543013851042L, 4607182418800017408L, 4733356190121042256L, 4607182418800017408L, 4729589277289258297L, 4607182418800017408L, -4500504508754688748L, 4607182418800017408L, 4729406860670979598L, 4607182418800017408L, 4733029739933710415L, 4607182418800017408L, -4499719889346365696L, 4607182418800017408L, 4709288460736544432L, 4607182418800017408L, 4731363401160409644L, 4607182418800017408L, -4500116038518969688L, 4607182418800017408L, 4718875915391408832L, 4607182418800017408L, 4718050007644005216L, 4607182418800017408L, 4731646377664202368L, 4607182418800017408L, 4716387108984332300L, 4607182418800017408L, -4503981242231244656L, 4607182418800017408L, 4722280975468814238L, 4607182418800017408L, 4720027323355135684L, 4607182418800017408L, 4720964603968483562L, 4607182418800017408L, 4730304330114633060L, 4607182418800017408L, 4712804179952405456L, 4607182418800017408L, 4731871294621056543L, 4607182418800017408L, 4724706037480902610L, 4607182418800017408L, 4729480103722287472L, 4607182418800017408L, 4724855457048092574L, 4607182418800017408L, 4719208702626576492L, 4607182418800017408L, 4732706122313188972L, 4607182418800017408L, 4718184014598066248L, 4607182418800017408L, 4727267118954606933L, 4607182418800017408L, 4728662370771707476L, 4607182418800017408L, 4723496356224724478L, 4607182418800017408L, -4511898151078443256L, 4607182418800017408L, -4502529718903536676L, 4607182418800017408L, 4730402996051584924L, 4607182418800017408L, -4502886883645857944L, 4607182418800017408L, 4721509012631898246L, 4607182418800017408L, 4731823726141976598L, 4607182418800017408L, 4726672596662565723L, 4607182418800017408L, 4729308345674521642L, 4607182418800017408L, -4506083234788572380L, 4607182418800017408L, 4727178364529878862L, 4607182418800017408L, 4728851101205811482L, 4607182418800017408L, 4731849179570217032L, 4607182418800017408L, 4697438321460183616L, 4607182418800017408L, -4502483531729018988L, 4607182418800017408L};

  // Generated values with parameters:
  // Count = 100
  // values_mean = 1.000000e+08; values_stdDev = 1.000000e+08; values_ordering = Random
  // weights_mean = 1.000000e+00; weights_stdDev = 4.000000e-01; weights_ordering = Random
  private static final long generatedMean2 = 4726585625579560290L; // approx ~ 1.015248e+08
  private static final long generatedStdDev2 = 4726542825223660391L; // approx ~ 1.008871e+08
  private static final long[] generatedWeightedValues2 = {4694479981834994432L, 4606702733316010249L, -4506985236334947932L, 4610130503134381735L, -4505725529193906836L, 4610153189977922522L, -4522822250393517632L, 4604321993447703922L, 4723615698162512327L, 4605546028220160722L, 4731028244759764905L, 4607361861604598424L, 4704038439290591072L, 4605429262338375729L, 4731054392033192628L, 4609910177027831643L, 4730366966825734298L, 4606410345666350568L, 4723603766306122823L, 4601507368464051492L, 4712998860761788512L, 4602968247553793268L, 4730803339638679294L, 4607704604523450197L, 4725745933757770455L, 4609392021348011058L, 4730695645781837466L, 4602644627878594242L, 4727578291575622443L, 4610180974050607007L, 4720085645765860795L, 4606728934566896523L, 4725567064660901683L, 4604708174407846921L, 4730184126998998397L, 4609308483193148990L, 4729176817990612116L, 4608960618990369729L, 4728224373459431381L, 4602793046497011054L, 4732905053784881517L, 4607598355414501626L, 4733143079003588387L, 4603203043827842476L, 4725471350145221787L, 4609543736311508659L, 4721126154405309516L, 4609342830914938182L, 4727379687256672212L, 4607422148810038199L, 4730256148895791260L, 4610235265264569874L, 4731938423931252842L, 4610035211540527580L, 4733305309449672940L, 4604168620936435398L, 4720167406435309870L, 4609712865819211412L, 4726172130053507517L, 4609881430752825144L, 4725853479439151146L, 4609964485733904560L, 4728489407609962400L, 4602931700366047036L, -4527480625972852096L, 4608457333607124596L, 4725765160759848127L, 4603843534622943589L, 4719116964850466092L, 4606034588755006974L, 4732641783215260995L, 4609706049040733688L, -4503273653728510636L, 4605864277954128543L, -4500721854406242952L, 4608092061317813477L, -4500400018405913944L, 4602896802900647834L, 4730017688827832808L, 4608534926033497181L, 4725989161189144067L, 4609138964855770716L, -4507927313501289752L, 4608498164334555188L, 4724275334311780474L, 4607244862463588298L, 4728953380259341588L, 4599232547301451500L, 4727462526154790618L, 4609873176165045288L, 4730418119785071855L, 4610219175168270884L, -4499247435661664372L, 4608532541450984144L, -4501820948182655944L, 4603532999334431051L, 4730996917598831372L, 4605903056664652571L, 4729475405906005000L, 4608716978484303908L, 4733282356762789220L, 4600485039684702724L, 4733298270094283792L, 4604744779066421926L, 4731068301057981810L, 4610124483132435445L, 4729512358979907930L, 4608444679506090970L, 4724693529474397358L, 4608721992105139624L, -4499032552730096894L, 4605539394749501031L, -4501322451230451248L, 4606692730094333407L, 4729941016976190374L, 4609161534027905933L, -4510574291536478488L, 4599371333093157306L, 4725327189541556314L, 4609080553281245934L, 4717377591117732316L, 4607990379979800275L, 4718066195501928072L, 4606258314390355528L, -4499407332972377148L, 4599256260955383952L, 4730797019531960776L, 4608128459243986410L, -4499880823889839148L, 4602662178724487908L, 4732693770073401850L, 4609973691468502692L, 4723121916124619194L, 4608735947014243540L, 4733181348428410460L, 4608472000242216440L, 4733304598875740446L, 4607905104233956012L, -4504741789701416024L, 4602165152212282782L, 4726089986869061894L, 4608221080148176602L, 4732091637232901022L, 4604081173835491421L, 4728393614022718684L, 4608499098334960740L, 4732124514014078826L, 4602791608351155408L, 4726426854828172274L, 4606290927251763694L, 4731104139875827444L, 4609755866663428532L, 4729600813700955105L, 4602686055161592646L, -4504743367894852464L, 4609506015370950039L, 4730454774661282315L, 4602878248549511691L, 4723954111464209943L, 4607317703525692055L, 4731069180616405948L, 4608174381859182869L, 4720066118794984106L, 4606696355665645747L, -4507549920870341532L, 4601214684381871504L, 4714889870032293840L, 4607340584851297984L, 4719955233577813543L, 4610221584955149700L, 4728177582736867028L, 4609723310147972451L, -4500933918941553132L, 4608257718149148089L, 4731365527800973994L, 4609326381002335719L, 4729237898608599441L, 4607913797075222486L, 4730495488602048054L, 4606288604593969585L, -4503712539418673972L, 4607914139308568474L, 4702063137430182688L, 4609055557730816964L, -4503629784565936620L, 4608567038143579654L, -4500486825185373548L, 4608515226764471247L, 4723822602114437286L, 4607086465400115306L, 4732659053489319135L, 4607834218809692972L, 4731348922274616987L, 4609963069239801243L, 4732006455895497234L, 4609331719306558354L, -4501552380654955512L, 4603608228210815468L, -4501348165228413396L, 4609538001598576772L};

  // Generated values with parameters:
  // Count = 100
  // values_mean = 1.000000e+08; values_stdDev = 1.000000e+08; values_ordering = Random
  // weights_mean = 1.500000e+01; weights_stdDev = 7.000000e+00; weights_ordering = Random
  private static final long generatedMean3 = 4726767506158416807L; // approx ~ 1.042351e+08
  private static final long generatedStdDev3 = 4726289645349817288L; // approx ~ 9.711438e+07
  private static final long[] generatedWeightedValues3 = {4724727042437385731L, 4627306794293644079L, 4732637898140224243L, 4621625164239493928L, 4730884100028832053L, 4624646930776699730L, 4732143980784681006L, 4619367720179006672L, 4729424204624420419L, 4625526096348962054L, 4731213932026007608L, 4616251477747834594L, -4515058092465401344L, 4627182809687987926L, 4732996915721129426L, 4621537556117889766L, 4733091339908445468L, 4620733028851262908L, 4730489533802238826L, 4620968437316910230L, 4725483794931243578L, 4627370449186966132L, 4727402380391728411L, 4624007483549942571L, 4730342393765323727L, 4623263409996896618L, 4728923041016668766L, 4626992484751239455L, 4731435741375920714L, 4619990311530089762L, 4729288207074935518L, 4623095742044850655L, 4722138157683301650L, 4616373594035916006L, 4717455127001280000L, 4618200960432326026L, -4506905085514827448L, 4625419966740287249L, 4729259000565799828L, 4627271794696785122L, 4732449942701738984L, 4622598075030635404L, -4508009016567338872L, 4614416467259233876L, 4724040521643298247L, 4627199794947548684L, -4499743099880897408L, 4626788266164691661L, 4728282641571701270L, 4625394241316572612L, 4719897662340490135L, 4624068892099231043L, 4729211163103567898L, 4625327981870753299L, 4723960748643296985L, 4626114354548217760L, -4501312843961140440L, 4626357494416679104L, 4732905572793738426L, 4621531983453907309L, -4503460702740745096L, 4627902046072104989L, 4730645381652060270L, 4622155293382782932L, 4730193069923539949L, 4616196339849883580L, 4729967472760526745L, 4624758851321881883L, 4731774729213186056L, 4627724062822656307L, 4729985711576172688L, 4628273573576531244L, -4503679635628283956L, 4625045898908259559L, 4728038155481527564L, 4625889547209067785L, 4709932683753073392L, 4626165148305600402L, 4728573573113862918L, 4623171195349866747L, -4518618344345516320L, 4627374358244629311L, 4729463306110280704L, 4626371175291487768L, -4506433489295583580L, 4615522652829941712L, 4724694055210947640L, 4621250749008573170L, 4732985059359278712L, 4623469272801740924L, -4504490236635866160L, 4620839110572261128L, 4731640709692471372L, 4626225229837258095L, 4732736678536996997L, 4623148650548827296L, 4724040592691035254L, 4626737123640668402L, -4506233580223021144L, 4622562669853864672L, 4727481575730904360L, 4627270960122310036L, 4724840225394574066L, 4627100690261940912L, -4504972522896194832L, 4626398439482280310L, 4729864445723844240L, 4616483113913428382L, 4730926656684969124L, 4622474378075584228L, 4731887838739532080L, 4625432488834937667L, -4501977069834360388L, 4625506375313271726L, 4731610852321172951L, 4625903827088758444L, -4502104984091934588L, 4624581288830289139L, 4729981431118155362L, 4627956428914672943L, 4733025979858392724L, 4626567332655878333L, 4727203289093183752L, 4626805708833243444L, 4716537462236086072L, 4622620197073610599L, 4726552332137979630L, 4626024163785904682L, 4733309986430050168L, 4614047128786926536L, -4505095526995961116L, 4627890722550165577L, -4509306669582798504L, 4624194343288978113L, -4499711009480007764L, 4624666017661848473L, 4731053295172019607L, 4626730573241855403L, -4501912558690441068L, 4618604044550015786L, -4502426007603225712L, 4626884956024668262L, 4727305714767788356L, 4623008405967419300L, -4500707328519633028L, 4625565416836374544L, -4507657764537607356L, 4627809602770395200L, 4732662913932125438L, 4623353578326949906L, 4721982644535056975L, 4626406746416390189L, 4733280229339347875L, 4621981397578059657L, 4725070563455038176L, 4626634315320347344L, -4506206517244194512L, 4628112998880131748L, 4730768223664628693L, 4627965824843638740L, 4732293291095414662L, 4623412874789976032L, 4724937268147744356L, 4624777237555831442L, 4730180362830077411L, 4622337822655983692L, 4724353965834810820L, 4623271624427785970L, 4731561811740329248L, 4616713043629403014L, 4726375372411688671L, 4628147563077969999L, 4729630440990787186L, 4615629633692393728L, 4726750375561244683L, 4621047967120088342L, 4729834762546813725L, 4626775305700866630L, 4726693260995896460L, 4625242432427958374L, 4728054618865592947L, 4616181562211733380L, 4731100848187464638L, 4620784875012270524L, 4727035080274280333L, 4624622322853544027L, 4730890313883750375L, 4617015922451656458L, 4722142613982555906L, 4618528713195508708L, 4731420085477707436L, 4626379057695784226L, 4729400539906063022L, 4627128007404168183L, 4729472876428051598L, 4628024708940657500L, 4731896696966625537L, 4621796946311551926L, 4729240220726970170L, 4626810317012976562L};

  // Generated values with parameters:
  // Count = 100
  // values_mean = 1.000000e+08; values_stdDev = 1.000000e-08; values_ordering = Random
  // weights_mean = 1.000000e+00; weights_stdDev = 0.000000e+00; weights_ordering = Random
  private static final long generatedMean4 = 4726483295884279808L; // approx ~ 1.000000e+08
  private static final long generatedStdDev4 = 4487973487878516845L; // approx ~ 1.140162e-08
  private static final long[] generatedWeightedValues4 = {4726483295884279807L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279809L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279807L, 4607182418800017408L, 4726483295884279807L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279807L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279809L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279807L, 4607182418800017408L, 4726483295884279807L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279809L, 4607182418800017408L, 4726483295884279809L, 4607182418800017408L, 4726483295884279807L, 4607182418800017408L, 4726483295884279809L, 4607182418800017408L, 4726483295884279807L, 4607182418800017408L, 4726483295884279807L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279809L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279809L, 4607182418800017408L, 4726483295884279809L, 4607182418800017408L, 4726483295884279809L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279809L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279807L, 4607182418800017408L, 4726483295884279807L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279809L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279809L, 4607182418800017408L, 4726483295884279809L, 4607182418800017408L, 4726483295884279807L, 4607182418800017408L, 4726483295884279809L, 4607182418800017408L, 4726483295884279807L, 4607182418800017408L, 4726483295884279807L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279807L, 4607182418800017408L, 4726483295884279809L, 4607182418800017408L, 4726483295884279807L, 4607182418800017408L, 4726483295884279809L, 4607182418800017408L, 4726483295884279809L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279809L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279809L, 4607182418800017408L, 4726483295884279807L, 4607182418800017408L, 4726483295884279809L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279809L, 4607182418800017408L, 4726483295884279807L, 4607182418800017408L, 4726483295884279807L, 4607182418800017408L, 4726483295884279809L, 4607182418800017408L, 4726483295884279809L, 4607182418800017408L, 4726483295884279807L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279809L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279807L, 4607182418800017408L, 4726483295884279807L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279807L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279807L, 4607182418800017408L, 4726483295884279809L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279809L, 4607182418800017408L, 4726483295884279807L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279809L, 4607182418800017408L, 4726483295884279807L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279807L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279807L, 4607182418800017408L, 4726483295884279809L, 4607182418800017408L, 4726483295884279809L, 4607182418800017408L, 4726483295884279808L, 4607182418800017408L, 4726483295884279809L, 4607182418800017408L};

  // Generated values with parameters:
  // Count = 100
  // values_mean = 1.000000e+08; values_stdDev = 1.000000e-08; values_ordering = Random
  // weights_mean = 1.000000e+00; weights_stdDev = 4.000000e-01; weights_ordering = Random
  private static final long generatedMean5 = 4726483295884279808L; // approx ~ 1.000000e+08
  private static final long generatedStdDev5 = 4488372772687846348L; // approx ~ 1.206219e-08
  private static final long[] generatedWeightedValues5 = {4726483295884279809L, 4607793994747318138L, 4726483295884279807L, 4607635536189584363L, 4726483295884279807L, 4604704809713622902L, 4726483295884279808L, 4606128380572011186L, 4726483295884279809L, 4608041368584019527L, 4726483295884279807L, 4607690059144984519L, 4726483295884279809L, 4606363398007199276L, 4726483295884279809L, 4606964045647938412L, 4726483295884279809L, 4600381716896466568L, 4726483295884279809L, 4601065241909569838L, 4726483295884279807L, 4609728226568796014L, 4726483295884279807L, 4609994876210387070L, 4726483295884279807L, 4605147423920065782L, 4726483295884279808L, 4610085967682835335L, 4726483295884279808L, 4608723995864258141L, 4726483295884279807L, 4603111576241152334L, 4726483295884279807L, 4607917082322414595L, 4726483295884279807L, 4608153179806434366L, 4726483295884279808L, 4603983239306431526L, 4726483295884279809L, 4609571591771771706L, 4726483295884279808L, 4605723862645150758L, 4726483295884279809L, 4604834813649300724L, 4726483295884279808L, 4603582135291969502L, 4726483295884279808L, 4603222141319294512L, 4726483295884279807L, 4610107593310140454L, 4726483295884279808L, 4609945820742628708L, 4726483295884279808L, 4604579458553867640L, 4726483295884279807L, 4600292932872274972L, 4726483295884279808L, 4601081839561547080L, 4726483295884279807L, 4603119932557120277L, 4726483295884279809L, 4605237018027330504L, 4726483295884279807L, 4604611679994007476L, 4726483295884279809L, 4599738431013580876L, 4726483295884279808L, 4607900077231104413L, 4726483295884279807L, 4608732971019033264L, 4726483295884279807L, 4608667631423094617L, 4726483295884279807L, 4605784769406393702L, 4726483295884279807L, 4606924357162199988L, 4726483295884279807L, 4610040145737496727L, 4726483295884279807L, 4601126327339302608L, 4726483295884279808L, 4605211228285086920L, 4726483295884279807L, 4607756964632497662L, 4726483295884279809L, 4608316865674636945L, 4726483295884279809L, 4609542762720113270L, 4726483295884279809L, 4606440619739516761L, 4726483295884279808L, 4603399620249400468L, 4726483295884279808L, 4607433084296719944L, 4726483295884279807L, 4609065103726023537L, 4726483295884279809L, 4606839451037078185L, 4726483295884279807L, 4608948246608641968L, 4726483295884279807L, 4610112207277236454L, 4726483295884279808L, 4606632704520175183L, 4726483295884279807L, 4607195533872136354L, 4726483295884279808L, 4608078310497841814L, 4726483295884279808L, 4608449653716849710L, 4726483295884279808L, 4603159903824236250L, 4726483295884279807L, 4604207172389445104L, 4726483295884279809L, 4609552148238873664L, 4726483295884279808L, 4608903256821417584L, 4726483295884279808L, 4607909513471175707L, 4726483295884279808L, 4605082692202512878L, 4726483295884279807L, 4607777975172736850L, 4726483295884279809L, 4607247465063241275L, 4726483295884279808L, 4607023449176719880L, 4726483295884279808L, 4607355090281350568L, 4726483295884279807L, 4606360124512428550L, 4726483295884279808L, 4608292971865284120L, 4726483295884279807L, 4609218414485392697L, 4726483295884279809L, 4609572131032698764L, 4726483295884279807L, 4604166379064374524L, 4726483295884279809L, 4603108709728693522L, 4726483295884279807L, 4610002540390980832L, 4726483295884279809L, 4607948289647668937L, 4726483295884279808L, 4602700606322304300L, 4726483295884279809L, 4608059099885874476L, 4726483295884279808L, 4606858181566952657L, 4726483295884279809L, 4604861534805946316L, 4726483295884279808L, 4609991024085281186L, 4726483295884279808L, 4603869913755154916L, 4726483295884279808L, 4610035215714536610L, 4726483295884279809L, 4602092856420774126L, 4726483295884279808L, 4602504561358909992L, 4726483295884279809L, 4605409937613234137L, 4726483295884279808L, 4603248133278019568L, 4726483295884279807L, 4609057028508925348L, 4726483295884279808L, 4601381735248542040L, 4726483295884279808L, 4609095780287341910L, 4726483295884279808L, 4600974094898476260L, 4726483295884279809L, 4608934090026074228L, 4726483295884279808L, 4609899222336623074L, 4726483295884279807L, 4607430266089500665L, 4726483295884279809L, 4608057750911533949L, 4726483295884279809L, 4608399142220741709L, 4726483295884279809L, 4607597152504722848L, 4726483295884279809L, 4608274960404454176L, 4726483295884279807L, 4607567702621945884L, 4726483295884279807L, 4607545856580159215L, 4726483295884279807L, 4608602154335600406L, 4726483295884279808L, 4607324357846908004L, 4726483295884279807L, 4603365558364534144L};

  // Generated values with parameters:
  // Count = 100
  // values_mean = 1.000000e+08; values_stdDev = 1.000000e-08; values_ordering = Random
  // weights_mean = 1.500000e+01; weights_stdDev = 7.000000e+00; weights_ordering = Random
  private static final long generatedMean6 = 4726483295884279808L; // approx ~ 1.000000e+08
  private static final long generatedStdDev6 = 4488445690501450847L; // approx ~ 1.218282e-08
  private static final long[] generatedWeightedValues6 = {4726483295884279808L, 4621682663573148126L, 4726483295884279807L, 4627092978650822842L, 4726483295884279807L, 4627878012305938442L, 4726483295884279809L, 4628041448611925700L, 4726483295884279809L, 4625276722149882808L, 4726483295884279809L, 4625386312087957168L, 4726483295884279808L, 4622309783965206034L, 4726483295884279809L, 4620050535687723265L, 4726483295884279809L, 4623439353928501593L, 4726483295884279807L, 4626010941064867204L, 4726483295884279807L, 4624521690684095800L, 4726483295884279807L, 4613864275584551240L, 4726483295884279809L, 4627573891350766702L, 4726483295884279807L, 4613737819731950976L, 4726483295884279807L, 4626617116102344787L, 4726483295884279808L, 4620241426376482528L, 4726483295884279807L, 4626865911256878130L, 4726483295884279809L, 4623996988455778330L, 4726483295884279809L, 4626920036067669688L, 4726483295884279808L, 4626286581565377422L, 4726483295884279809L, 4616290142270657888L, 4726483295884279807L, 4626803617201612219L, 4726483295884279808L, 4623869520112934626L, 4726483295884279808L, 4617189477793750072L, 4726483295884279808L, 4626246062277213776L, 4726483295884279808L, 4623427840057572729L, 4726483295884279807L, 4624242631168282096L, 4726483295884279807L, 4627912336108929960L, 4726483295884279807L, 4627046213445234443L, 4726483295884279807L, 4626737588469330060L, 4726483295884279808L, 4627497331582128005L, 4726483295884279808L, 4619973614537241071L, 4726483295884279809L, 4616478946598365186L, 4726483295884279808L, 4618833484426549000L, 4726483295884279808L, 4626759321258087208L, 4726483295884279808L, 4621067569155814768L, 4726483295884279808L, 4620836673986270408L, 4726483295884279809L, 4621280362185847410L, 4726483295884279808L, 4616801497319715640L, 4726483295884279807L, 4614893427519661860L, 4726483295884279807L, 4626324432703066922L, 4726483295884279809L, 4627209390961901225L, 4726483295884279807L, 4617007997922782370L, 4726483295884279808L, 4627080711717909732L, 4726483295884279807L, 4623297640084487758L, 4726483295884279807L, 4623224230814465369L, 4726483295884279808L, 4627356710192475633L, 4726483295884279807L, 4626694602948720654L, 4726483295884279808L, 4626189905078010285L, 4726483295884279807L, 4618911980209978050L, 4726483295884279809L, 4623962093629027195L, 4726483295884279808L, 4618210784933373644L, 4726483295884279807L, 4618797495892367248L, 4726483295884279808L, 4617188139082000026L, 4726483295884279809L, 4622001642202285850L, 4726483295884279808L, 4626059778879673940L, 4726483295884279808L, 4623656157495611760L, 4726483295884279808L, 4627166902716581730L, 4726483295884279809L, 4618185920694493078L, 4726483295884279809L, 4623427757211097455L, 4726483295884279808L, 4619751153580792654L, 4726483295884279807L, 4624910500485263835L, 4726483295884279808L, 4625812983694977694L, 4726483295884279807L, 4621144582196501414L, 4726483295884279809L, 4616328958258525284L, 4726483295884279807L, 4626492178604934005L, 4726483295884279807L, 4622914941460139314L, 4726483295884279808L, 4618436391765688220L, 4726483295884279809L, 4626369047897744171L, 4726483295884279807L, 4627340926050623263L, 4726483295884279807L, 4625670457264795712L, 4726483295884279808L, 4617971262357536564L, 4726483295884279809L, 4627668232748584974L, 4726483295884279808L, 4627332728360421844L, 4726483295884279807L, 4620898714698333412L, 4726483295884279809L, 4628142772549788105L, 4726483295884279808L, 4618861794152286408L, 4726483295884279808L, 4625048435463961908L, 4726483295884279807L, 4618668178092306674L, 4726483295884279807L, 4624299540714687056L, 4726483295884279808L, 4622823998648830796L, 4726483295884279809L, 4622646145013520603L, 4726483295884279807L, 4627060669324326898L, 4726483295884279808L, 4626874458669121654L, 4726483295884279807L, 4617888079528756178L, 4726483295884279807L, 4626862451978044292L, 4726483295884279807L, 4617319162723816792L, 4726483295884279808L, 4623705998579605264L, 4726483295884279807L, 4625386059980238489L, 4726483295884279808L, 4620079727970033056L, 4726483295884279808L, 4622591288443823259L, 4726483295884279809L, 4628059609799097134L, 4726483295884279809L, 4623343442711554116L, 4726483295884279807L, 4626800261488210151L, 4726483295884279807L, 4625407995532770920L, 4726483295884279809L, 4627793470224132926L, 4726483295884279809L, 4623071483972060634L, 4726483295884279809L, 4616689302860016938L, 4726483295884279809L, 4628277256964563110L, 4726483295884279808L, 4617054028884147296L};

  // Generated values with parameters:
  // Count = 100
  // values_mean = -1.000000e-08; values_stdDev = 1.000000e+08; values_ordering = Random
  // weights_mean = 1.000000e+00; weights_stdDev = 0.000000e+00; weights_ordering = Random
  private static final long generatedMean7 = -4525717876434209840L; // approx ~ -1.141650e+06
  private static final long generatedStdDev7 = 4726402011001405004L; // approx ~ 9.878876e+07
  private static final long[] generatedWeightedValues7 = {-4497599012045978646L, 4607182418800017408L, -4499068209036675933L, 4607182418800017408L, -4498441723830639158L, 4607182418800017408L, 4730066826117485651L, 4607182418800017408L, 4711973856889723488L, 4607182418800017408L, 4729073185559173411L, 4607182418800017408L, 4721328487547023118L, 4607182418800017408L, 4729326129198708968L, 4607182418800017408L, -4493928943287201028L, 4607182418800017408L, 4726723903139333214L, 4607182418800017408L, -4501330624773743109L, 4607182418800017408L, -4493932013445407858L, 4607182418800017408L, -4501677843322446532L, 4607182418800017408L, 4730052294215963864L, 4607182418800017408L, -4493380566933652750L, 4607182418800017408L, -4495651665536858599L, 4607182418800017408L, 4726548299521804362L, 4607182418800017408L, -4500522046307930222L, 4607182418800017408L, -4494991341927841082L, 4607182418800017408L, -4513916642057445163L, 4607182418800017408L, -4499327617434278731L, 4607182418800017408L, -4497548635358718681L, 4607182418800017408L, 4724493276436189331L, 4607182418800017408L, -4498262030933360974L, 4607182418800017408L, -4497461754714150009L, 4607182418800017408L, -4501920694849107681L, 4607182418800017408L, -4494404482704688696L, 4607182418800017408L, 4727453706871971832L, 4607182418800017408L, -4508821690167678580L, 4607182418800017408L, -4498650413680541133L, 4607182418800017408L, -4508143020312571889L, 4607182418800017408L, -4495689582124839377L, 4607182418800017408L, -4495926291173978957L, 4607182418800017408L, -4510338085455487921L, 4607182418800017408L, 4727044439652761972L, 4607182418800017408L, -4499184978341908471L, 4607182418800017408L, 4725994756753802037L, 4607182418800017408L, 4721159028027284516L, 4607182418800017408L, 4725413464331788450L, 4607182418800017408L, 4725482081884162582L, 4607182418800017408L, 4730041417703686617L, 4607182418800017408L, 4727482750085361788L, 4607182418800017408L, 4718681753575337607L, 4607182418800017408L, -4504256171524288931L, 4607182418800017408L, -4495143834876185170L, 4607182418800017408L, 4703866129335175444L, 4607182418800017408L, 4721517434111926139L, 4607182418800017408L, 4715473955364391162L, 4607182418800017408L, -4503232493398971519L, 4607182418800017408L, 4714587091496382379L, 4607182418800017408L, -4494495903815305334L, 4607182418800017408L, 4727790141148888208L, 4607182418800017408L, 4729927183808112113L, 4607182418800017408L, 4704841209137210491L, 4607182418800017408L, 4729599214275101217L, 4607182418800017408L, -4496257485977487150L, 4607182418800017408L, 4728497706249695588L, 4607182418800017408L, -4499791136090305292L, 4607182418800017408L, -4496168918962814111L, 4607182418800017408L, -4497893037898358820L, 4607182418800017408L, -4502224066135457414L, 4607182418800017408L, 4729243901019869976L, 4607182418800017408L, -4502146381897249524L, 4607182418800017408L, 4727279305468347876L, 4607182418800017408L, 4721418425545835753L, 4607182418800017408L, -4513723200265176861L, 4607182418800017408L, 4728141789482295252L, 4607182418800017408L, -4498368608458321729L, 4607182418800017408L, -4505872810749245453L, 4607182418800017408L, 4717027771709410107L, 4607182418800017408L, 4718068918340123935L, 4607182418800017408L, 4713742088616728138L, 4607182418800017408L, -4494002109699310632L, 4607182418800017408L, -4495800057324210785L, 4607182418800017408L, 4729839562276479720L, 4607182418800017408L, 4727055178495523662L, 4607182418800017408L, -4496652862087856857L, 4607182418800017408L, -4500910668005651904L, 4607182418800017408L, 4725509554688801391L, 4607182418800017408L, 4727151632038306659L, 4607182418800017408L, -4497986694589220345L, 4607182418800017408L, -4494821123010655256L, 4607182418800017408L, -4497035281085709981L, 4607182418800017408L, 4712675780622280791L, 4607182418800017408L, -4503135412946024729L, 4607182418800017408L, -4494088125365622413L, 4607182418800017408L, -4510253827788627215L, 4607182418800017408L, 4722589223130468524L, 4607182418800017408L, 4728423536769349374L, 4607182418800017408L, -4500037917575815532L, 4607182418800017408L, -4497340553711874256L, 4607182418800017408L, -4495667221483095630L, 4607182418800017408L, 4728804224398123774L, 4607182418800017408L, 4721030093034240710L, 4607182418800017408L, 4722748676047621063L, 4607182418800017408L, -4508016287291243613L, 4607182418800017408L, 4722784787239544262L, 4607182418800017408L, 4730048363109706248L, 4607182418800017408L, -4494332894846264844L, 4607182418800017408L, 4726563173655524157L, 4607182418800017408L};

  // Generated values with parameters:
  // Count = 100
  // values_mean = -1.000000e-08; values_stdDev = 1.000000e+08; values_ordering = Random
  // weights_mean = 1.000000e+00; weights_stdDev = 4.000000e-01; weights_ordering = Random
  private static final long generatedMean8 = 4714139532180502591L; // approx ~ 1.467377e+07
  private static final long generatedStdDev8 = 4726944556265871006L; // approx ~ 1.068733e+08
  private static final long[] generatedWeightedValues8 = {-4501798648756710824L, 4602327531855410892L, -4498800967174648360L, 4605057375640033816L, 4715673111270286476L, 4607223190249774760L, 4729450737407319568L, 4600187294700686996L, 4724846893052959331L, 4604396686890960168L, 4729871113213303955L, 4603135345505363953L, 4729864639072392707L, 4607130625151235130L, 4726966498225797166L, 4609945256472789261L, 4729095835117960210L, 4607597325570756316L, 4706796191067576901L, 4605425947849213162L, -4498191159900718695L, 4604399385726019530L, 4710671847892976439L, 4609618210987004694L, -4493935137773291740L, 4605450667965188284L, 4722760466537482131L, 4604264823358025370L, 4728853981486751790L, 4606289915161922092L, -4494027997192316529L, 4602963201615075576L, -4493960911020477714L, 4608321929730981317L, 4725420496760337490L, 4605123971644380017L, 4729057168417064452L, 4608439407673021050L, 4725493224981101178L, 4607558995542390045L, -4499183345451170650L, 4607877264078190275L, -4497461367172779889L, 4606915169298963107L, -4499557787190470053L, 4601059056005041762L, -4497581268326773232L, 4601190539459996446L, 4723945366734798640L, 4607352962345215647L, 4729085874344888497L, 4609501517024454928L, 4727518498290049512L, 4606520815338275931L, -4505396562784031098L, 4602230768574643422L, 4727841085227497048L, 4609538898718614200L, -4505847346163987244L, 4599453490012617070L, -4500979026200517936L, 4604990825322550199L, 4714585633049566931L, 4605638474188714583L, -4504025253953479587L, 4609270197071454758L, 4728446183463409368L, 4609458280801370394L, -4502528288621175129L, 4608726802841193043L, 4726365055852295443L, 4607274203495911195L, 4730031068255275081L, 4607389575678526240L, 4729648989007690482L, 4603095055559170890L, 4725239643905735031L, 4608271874624929800L, -4496053445102305001L, 4606721145223014142L, -4494808240296120613L, 4608582543994175128L, 4726719196929764046L, 4603439527750450020L, -4497866649680497348L, 4607571788527830425L, 4729084365439290041L, 4608056311339710776L, 4726466389201863693L, 4607839647278538848L, 4729007277607598780L, 4610198338713327692L, -4502062389112589675L, 4608696453631235931L, -4502472102410827114L, 4602431566969658238L, 4729131224924478101L, 4603584700267761528L, -4497214682473357140L, 4602892566101959584L, 4729066774743556081L, 4610130622161039334L, 4719917597499229781L, 4608855195808648503L, 4727686685616180609L, 4604332032800749682L, 4725365312743210299L, 4603306268010802246L, 4728171985388098225L, 4608052487193419249L, -4523201562177463216L, 4608997654993744190L, 4727300231659216236L, 4607734811245330472L, -4499664076835920893L, 4609472072449442243L, -4495189075098608159L, 4605968657640599466L, -4496068884717061149L, 4608775008198725811L, -4495053867192797965L, 4608943852330714603L, -4504392470211507838L, 4600280996986771916L, -4494516089157707679L, 4609975993011618118L, 4721668117591546571L, 4609784374821150510L, -4495377096102502622L, 4607229441720629193L, 4720826463899939236L, 4602776618836550691L, 4718444023112260051L, 4602776434589351888L, 4728086921240043217L, 4602649464135029306L, 4728046131726545243L, 4608874707953333855L, -4494989178719590036L, 4606681056366857299L, 4727436637259664467L, 4607219927281012690L, 4728167676251160548L, 4604870501943873046L, 4728993417853605244L, 4607403822076436724L, 4724233330262482657L, 4607551630217464670L, 4724718050200440879L, 4609538859565167670L, 4697838796536416899L, 4602874153006993342L, 4724289182424345517L, 4599469904192882362L, 4729890259680017754L, 4606856684100541596L, -4496157592186053361L, 4607184927960225010L, -4497141107905322814L, 4604153702001848815L, -4496595785271752837L, 4609005619363029948L, -4493745706761084443L, 4606123021673133314L, -4495794623475144228L, 4609710408542032918L, 4729727625707990038L, 4606461548949580102L, -4494283477472773781L, 4609122553628963749L, 4721600690872819765L, 4606899678985627939L, 4715449995772529070L, 4602385218161461042L, 4721722756263202730L, 4601478402902361632L, -4501088593090017585L, 4604354034024614507L, -4514526278119053191L, 4604418616021591592L, -4494190321443545668L, 4610154476241364348L, -4493581791440665722L, 4605022281288890213L, 4728268231177601343L, 4606068762237985875L, -4498873870676943412L, 4610107196172877515L, 4729552503949309903L, 4607942838311054459L, -4498813449229903620L, 4609858203138049792L, -4496454182072141968L, 4606307831594354217L, 4723909009091007331L, 4608294497890274315L, -4501963798474054413L, 4609275135071603264L, 4723810759994322778L, 4603535680870589929L};

  // Generated values with parameters:
  // Count = 100
  // values_mean = -1.000000e-08; values_stdDev = 1.000000e+08; values_ordering = Random
  // weights_mean = 1.500000e+01; weights_stdDev = 7.000000e+00; weights_ordering = Random
  private static final long generatedMean9 = -4536350704700227996L; // approx ~ -2.264662e+05
  private static final long generatedStdDev9 = 4726082658838020690L; // approx ~ 9.403004e+07
  private static final long[] generatedWeightedValues9 = {4702992352431222909L, 4626973990196220689L, -4516997636033148508L, 4619406141421233324L, 4712954636849378024L, 4622467609769516227L, 4721752552384203545L, 4625288381578319322L, 4729792241837434868L, 4622500959810741140L, -4494519599826334044L, 4620734824716521557L, 4721120349931931126L, 4621292148282826471L, 4715412298228642367L, 4616570639576930732L, 4715321858286184519L, 4626000925275050086L, 4727638928770507223L, 4628240274989983278L, -4493390097397567738L, 4621091652972007108L, 4725485416448125969L, 4622282147022588794L, 4727706803949631355L, 4623255561530832401L, -4494249989047355263L, 4618903781891272690L, -4497587329302744828L, 4626329424075379950L, 4729913028035360426L, 4621678954335658976L, 4724395297001131721L, 4618884360849099466L, -4497400779142098023L, 4619261317622271964L, -4498153590548764828L, 4615665269525104928L, -4498372564216266246L, 4615683037210766520L, -4503113368683122975L, 4623500589752016865L, 4729983238816500257L, 4624366544507282867L, 4719978512862733127L, 4627110068173907346L, 4710856182556356527L, 4627800802355753754L, -4527233055889801911L, 4625545322531902633L, -4507274336081405564L, 4627269008661187684L, 4724061219660280660L, 4622907155721943826L, -4523504317813638455L, 4627316745474754454L, 4726533182903019681L, 4627284287557988072L, 4719503863443235172L, 4618862362293628572L, 4710282724918807710L, 4624505523512057654L, -4498921636934783323L, 4625882366001964913L, 4720779791363192655L, 4617937769388875530L, 4726102524780220506L, 4616639881275724250L, 4724647208304945302L, 4623370107690619526L, 4728807463262553657L, 4625874411157991440L, 4721761062513210466L, 4620774918501173330L, 4717307508063760418L, 4627916711048363568L, -4498087405110530190L, 4627692915814048736L, 4722306738813111253L, 4625402539727595558L, -4498922663677553743L, 4621049827841188360L, 4729679262771548259L, 4623739812708617141L, 4729489191062388866L, 4626851607486827331L, 4726086700587386951L, 4625900818399831101L, -4516266409267526698L, 4625799272807629122L, -4512621577592491728L, 4627542773436917677L, 4706711132044994006L, 4622907644490600520L, -4529654966975540163L, 4625047315187391729L, 4725757742169696406L, 4623135414100605216L, 4727247623777480790L, 4625918183738371159L, -4511397951884899959L, 4622694092363818671L, -4508583727318787134L, 4627378483891368768L, -4493786886814375315L, 4626572050022845633L, 4727403132744068297L, 4621016738335363950L, 4717181393047410238L, 4626775002814107768L, -4497735155991379639L, 4622594134110882755L, 4727808002010860697L, 4626168500580002633L, -4528099770843039471L, 4624747745940760424L, -4500486454230183152L, 4626108262803081347L, 4730033150115895570L, 4613973825061616536L, 4726130924610204800L, 4625640900483976620L, -4494144308307367323L, 4624914185826182123L, -4494493541742869668L, 4626563468468433328L, -4499369309638644597L, 4626098909653891537L, -4494167870859889019L, 4628282322957470157L, -4498381240244050642L, 4619943615894214007L, -4494109674410966820L, 4622551251633837194L, -4501540448309397705L, 4625517662212549434L, -4500541826984569442L, 4627753267203008852L, -4499510841668034684L, 4625247765869672216L, -4500668759893056558L, 4624700046436906705L, 4713580791843692960L, 4626317332754494008L, 4729153326437669724L, 4621656274697739844L, 4727103034134125190L, 4627455887393992868L, -4493731843372100511L, 4625934739666013350L, -4493399311040892809L, 4625578419686981754L, 4723880448038938221L, 4618111780395222294L, 4709042220208815894L, 4625594788823371372L, 4723996856271366889L, 4621312979860698944L, 4729543217934622762L, 4621077919300517317L, -4498351299028099392L, 4625235662957459832L, 4716721823076309550L, 4626307556167384961L, 4723014000487069435L, 4624287772340502112L, 4724981264240877922L, 4625235617806538085L, 4729284558968440391L, 4626438492447495788L, -4496688653918303550L, 4618292575589773080L, 4724241491516351091L, 4628303935256861536L, -4508945106743689149L, 4623482218915951941L, -4494033620160584215L, 4627879745985480907L, 4725585798646825092L, 4622460129992928020L, -4496799624085798936L, 4625357525407796035L, -4494565968913348747L, 4626398231894330376L, 4729571519537855259L, 4622280219811547584L, 4710121470838878859L, 4619621642982802978L, -4494232505981427323L, 4626267730524894216L, -4497587526961771145L, 4628121180959271979L, -4497125107955645862L, 4627261672254966804L, 4727339584414877845L, 4621070670601175546L, -4494803573619415120L, 4620475578921908650L, 4713318428548611998L, 4627731980645052952L};

  // Generated values with parameters:
  // Count = 100
  // values_mean = -1.000000e-08; values_stdDev = 1.000000e-08; values_ordering = Random
  // weights_mean = 1.000000e+00; weights_stdDev = 0.000000e+00; weights_ordering = Random
  private static final long generatedMean10 = -4735585925492998230L; // approx ~ -1.109164e-08
  private static final long generatedStdDev10 = 4487191975148851810L; // approx ~ 1.010872e-08
  private static final long[] generatedWeightedValues10 = {-4733228074963334090L, 4607182418800017408L, -4746895037738179744L, 4607182418800017408L, -4746734382598854304L, 4607182418800017408L, -4740872076139047882L, 4607182418800017408L, -4733751592554165626L, 4607182418800017408L, 4483088175588682128L, 4607182418800017408L, -4734094032582464914L, 4607182418800017408L, 4481899087360056430L, 4607182418800017408L, -4735447791450737619L, 4607182418800017408L, -4741397813275788892L, 4607182418800017408L, -4734150152116453955L, 4607182418800017408L, -4737102196335515301L, 4607182418800017408L, -4729991471084817305L, 4607182418800017408L, -4730803857852674950L, 4607182418800017408L, -4731562616958824517L, 4607182418800017408L, -4737508926009298315L, 4607182418800017408L, -4730683553262866232L, 4607182418800017408L, -4756491593799672160L, 4607182418800017408L, -4731205778371895668L, 4607182418800017408L, -4730241236239542309L, 4607182418800017408L, -4734511144236538853L, 4607182418800017408L, -4730697588461144440L, 4607182418800017408L, 4485245720632912140L, 4607182418800017408L, -4735408181142307642L, 4607182418800017408L, -4741532430914895731L, 4607182418800017408L, -4732831458105005909L, 4607182418800017408L, -4732936490574176454L, 4607182418800017408L, -4730825251955529274L, 4607182418800017408L, 4478166365057395588L, 4607182418800017408L, -4731513195271902880L, 4607182418800017408L, -4734771211777260456L, 4607182418800017408L, -4732480882202255216L, 4607182418800017408L, -4730196530956282513L, 4607182418800017408L, -4731619925126465842L, 4607182418800017408L, 4477466053752866488L, 4607182418800017408L, -4750282977626348936L, 4607182418800017408L, -4731066563133109796L, 4607182418800017408L, -4731193619065935673L, 4607182418800017408L, 4473710927496956880L, 4607182418800017408L, -4731891531253940668L, 4607182418800017408L, -4735855551702951380L, 4607182418800017408L, 4482809417004599528L, 4607182418800017408L, -4734971007383598641L, 4607182418800017408L, -4731313918379519294L, 4607182418800017408L, -4741118412226358699L, 4607182418800017408L, -4731207564762206473L, 4607182418800017408L, 4481723355241720206L, 4607182418800017408L, 4466448579384149632L, 4607182418800017408L, -4733464138673598802L, 4607182418800017408L, 4483151416366566772L, 4607182418800017408L, -4731260252224949334L, 4607182418800017408L, -4735676329037383710L, 4607182418800017408L, -4737743549438914075L, 4607182418800017408L, -4747876937780564976L, 4607182418800017408L, -4735275047919507047L, 4607182418800017408L, -4735840952498636696L, 4607182418800017408L, -4732850044376070701L, 4607182418800017408L, -4729980850160256151L, 4607182418800017408L, -4731472002072031312L, 4607182418800017408L, -4734962479309224318L, 4607182418800017408L, 4479314366227216612L, 4607182418800017408L, -4731372590950156882L, 4607182418800017408L, 4473316845165246208L, 4607182418800017408L, -4742905064471053740L, 4607182418800017408L, 4462248714887739200L, 4607182418800017408L, 4483776020353667172L, 4607182418800017408L, -4731421798858601021L, 4607182418800017408L, -4729868797332500331L, 4607182418800017408L, -4737052813520979714L, 4607182418800017408L, 4485114191312279040L, 4607182418800017408L, -4736812283818693717L, 4607182418800017408L, -4731051147006004849L, 4607182418800017408L, -4741081736369106994L, 4607182418800017408L, -4730298191730619405L, 4607182418800017408L, -4737974813174416904L, 4607182418800017408L, -4732546123187598033L, 4607182418800017408L, -4735014906381565408L, 4607182418800017408L, 4483654724841249344L, 4607182418800017408L, -4730031925733899214L, 4607182418800017408L, -4732543952784191647L, 4607182418800017408L, -4753287364040387920L, 4607182418800017408L, -4745500059107995280L, 4607182418800017408L, -4730655077425312690L, 4607182418800017408L, -4731060156320081496L, 4607182418800017408L, -4748743669512823824L, 4607182418800017408L, -4730487916870216154L, 4607182418800017408L, -4732705254929128532L, 4607182418800017408L, -4742168036569592415L, 4607182418800017408L, -4750293961904383128L, 4607182418800017408L, -4736547675742321542L, 4607182418800017408L, -4734573261032488172L, 4607182418800017408L, -4732717994568904956L, 4607182418800017408L, 4483853980852377096L, 4607182418800017408L, -4731089399813589608L, 4607182418800017408L, -4729861782429168150L, 4607182418800017408L, -4739231986686496020L, 4607182418800017408L, -4734897909728455491L, 4607182418800017408L, -4751719198915161408L, 4607182418800017408L, -4750580358803215248L, 4607182418800017408L, -4731414314791783387L, 4607182418800017408L};

  // Generated values with parameters:
  // Count = 100
  // values_mean = -1.000000e-08; values_stdDev = 1.000000e-08; values_ordering = Random
  // weights_mean = 1.000000e+00; weights_stdDev = 4.000000e-01; weights_ordering = Random
  private static final long generatedMean11 = -4736941539800803638L; // approx ~ -8.848960e-09
  private static final long generatedStdDev11 = 4487562795472249496L; // approx ~ 1.072219e-08
  private static final long[] generatedWeightedValues11 = {-4731970164519209950L, 4606504222575447356L, 4483788724626476084L, 4608997958814260711L, 4478747935649177848L, 4608861836715193363L, -4735755240491535174L, 4601413346917795234L, -4729829895821709597L, 4607269043543771844L, -4745145606256300508L, 4601284189072682182L, -4750972651841556440L, 4601386887928017132L, -4730919208552094682L, 4603677491584014610L, -4747400402290490184L, 4600891904406229116L, -4732855395604929581L, 4600089690450380382L, -4733369335320280438L, 4608590131265333460L, -4730274827393536198L, 4607712971725701370L, -4736505931333623385L, 4605435468279124513L, -4730390374692242402L, 4601130047223079166L, 4479739955561478552L, 4610235751317296230L, -4730964714578276607L, 4605077599977672514L, -4735572849382542043L, 4599909189262835804L, -4735635886045771578L, 4604031571617857433L, 4468246653861943328L, 4604443164123443068L, 4465230406958976160L, 4606158918341675610L, -4731412938683026808L, 4606111195447313602L, -4730111549872512577L, 4609672394516603890L, -4735797123597099314L, 4605002621570607025L, 4483911452571936988L, 4608948177419252678L, 4478575609434544336L, 4609631312789943398L, -4729842036894115491L, 4602247483294127446L, -4733265861010734689L, 4600817144299588824L, 4481904361572573714L, 4604939141762047100L, -4738290250231923508L, 4608640937439994990L, 4484532539321499892L, 4609799997265342621L, -4732107922431895156L, 4610013715564020694L, -4749006568689353656L, 4608825969176455535L, -4734338770639293009L, 4604420785653667675L, -4734528958648136344L, 4604404481386115827L, 4478825377524278732L, 4609078821674938192L, -4732696245471985469L, 4608437895009581196L, -4729639507487339562L, 4599770823779691482L, -4734808618477695038L, 4602868725185529930L, -4733310408125359093L, 4605901141667631470L, -4736973894137580041L, 4606830620163956179L, 4484086393750021432L, 4607601399878434742L, -4732905003051061338L, 4607945675598845103L, 4480648856885909984L, 4604221645692602420L, 4484791116961530524L, 4604603014171443895L, -4730542192379742727L, 4609290563904269085L, -4731169222618241962L, 4609053264468357051L, -4729905134017010394L, 4605212988115264686L, -4733512834959746174L, 4603752449667200150L, -4747830858611283888L, 4604623045359487877L, -4734006231270509248L, 4608438969338403124L, 4483667232070602756L, 4610042005625589936L, -4737837082271967474L, 4606532328888484097L, -4730591365990268238L, 4608384878399573012L, 4480531292220843148L, 4609476590493827790L, -4741181971727831525L, 4606899193943290978L, -4733098908592466670L, 4604778515936518732L, 4455011597718168192L, 4606839631424289432L, -4738845079720755359L, 4601802876481143028L, -4735343507613228952L, 4606128781385800575L, -4733141435583413679L, 4604394945798750788L, -4736040569055528001L, 4604823429442528456L, -4743888216210102794L, 4608928261374485889L, -4732331729076652504L, 4608062334781696926L, -4740747213773594137L, 4604986240628607188L, 4478408129379164496L, 4607879692850938258L, -4731845598275853704L, 4608885547159935399L, 4459931424279783104L, 4608848254802450969L, -4737963196443833288L, 4606548129004543290L, -4735433693576384496L, 4602690156809961064L, -4730659791953635758L, 4602795500841317422L, -4730783963758089486L, 4605442956997303129L, -4729720468467142613L, 4607455961729959053L, -4740562674937154539L, 4601728940510510640L, 4483827404771770968L, 4607376189605412279L, 4473326742418650304L, 4608320326254132368L, -4732414872609919968L, 4600971878919694364L, -4738666742186108402L, 4605736056111593380L, 4477764872357270320L, 4603338928411021483L, -4734891774135915358L, 4607867556055683247L, -4731444033316519616L, 4607953989684102570L, 4471512597245768448L, 4605717895101065316L, -4731655620715494524L, 4605118901960728658L, -4740537964760291493L, 4604343471863456963L, 4475801751225096792L, 4610109077797585016L, -4745631182243251012L, 4608025093416450759L, -4741626938001329478L, 4602104927922738368L, -4729825353033233917L, 4608204235843208571L, -4741037058480828502L, 4603994081676168122L, 4484567669706747528L, 4607369658287338911L, -4749238312926786384L, 4608157056836898460L, -4742689757158751230L, 4603200486930558240L, -4739336515635142543L, 4602724187915522770L, -4730214989105260180L, 4602666624732049590L, -4733145667916605514L, 4600070114757067244L, -4730934857780064302L, 4609845387777887220L, -4741157693740788539L, 4607823016748512278L, -4734207069116858062L, 4610263968284383473L, -4739540212595859155L, 4608390139072047662L, 4482607490561479364L, 4602705210134157266L, -4734855204985422333L, 4604766223482999917L};

  // Generated values with parameters:
  // Count = 100
  // values_mean = -1.000000e-08; values_stdDev = 1.000000e-08; values_ordering = Random
  // weights_mean = 1.500000e+01; weights_stdDev = 7.000000e+00; weights_ordering = Random
  private static final long generatedMean12 = -4736076059759940359L; // approx ~ -1.028078e-08
  private static final long generatedStdDev12 = 4487594672731162099L; // approx ~ 1.077493e-08
  private static final long[] generatedWeightedValues12 = {-4731819791214192734L, 4627239970587229966L, -4746718488527709484L, 4623451697796914646L, -4730679722535140162L, 4622427326396931361L, -4753628692851110528L, 4622766981605528870L, -4729925483428682960L, 4627701298397178248L, -4729800518517134589L, 4628324745759580090L, 4477337086777367780L, 4616319769193466950L, 4475121923150614736L, 4625330761366683619L, -4737298485221116628L, 4619202161215311864L, -4731094447413428802L, 4624126324650396089L, 4480422729391212700L, 4621878208661311522L, -4741195648488354501L, 4625499380295648912L, -4761273896978964160L, 4626103761683419697L, -4732814788985319591L, 4623789289580197484L, -4734501036836551316L, 4625534582318982538L, -4731844020764836204L, 4620901543664904665L, -4729675173715600889L, 4626717806212488423L, 4472934281245909296L, 4623069343506969138L, 4470095469579421808L, 4625582670449604558L, -4734565514623306782L, 4628048912339068914L, -4742264261121120010L, 4625850600780748652L, 4469705266492501456L, 4615369607513245808L, -4732285367789665595L, 4617294340787747356L, -4730279965093099584L, 4626605490646626270L, 4485046087219684404L, 4625548817353835263L, -4730862682291037472L, 4614105938696281628L, -4736034381234773703L, 4625899670425500416L, 4483975487684257504L, 4622035052728661352L, -4733195886969597371L, 4627755839602698899L, 4483301048856827380L, 4621839324646785475L, 4485064827998481572L, 4627628664104624040L, -4732727117324070064L, 4626340021061702301L, -4730203299082789443L, 4626869053796822620L, 4484341128638601720L, 4627726500292644426L, -4736909291701382252L, 4619100003938074418L, -4729627551089873474L, 4625947346379655027L, -4732486967035598230L, 4613689724465244324L, -4730933927572032154L, 4620878839686658113L, 4483039810588717580L, 4622288776491938818L, -4733000365037556537L, 4627101779850645878L, -4737621168329710576L, 4626163352083752094L, -4750944526795770728L, 4625371501969833220L, -4732927603508333974L, 4627896177474513224L, -4741275129345525811L, 4628130378957667840L, -4732190316904919224L, 4623461367736230557L, 4482439655373944438L, 4621652497637476010L, -4732931275684327217L, 4620012690471612881L, 4481983848042269498L, 4625365028978259802L, -4732836868763897872L, 4613668208875730748L, 4476162450845247184L, 4618117580191460504L, -4735176575994238299L, 4626933680575126358L, -4746022036389938492L, 4621386507530313910L, -4732478906581183235L, 4625808343992662177L, -4735691594546552521L, 4614210228997102268L, -4730740977138135178L, 4626476977441828219L, -4736697132652423262L, 4617391617247781956L, 4474664175178318408L, 4622584415437146777L, -4730593186054724103L, 4619712237834311012L, -4768940280080505728L, 4615726198387219008L, -4732534699516560214L, 4626903566224573730L, -4733963618400060538L, 4619145417263222040L, -4731578954372769279L, 4623063073146997153L, -4734771525381336847L, 4625809435066195121L, 4477809824935543540L, 4618113427628045648L, -4732443809905022796L, 4625883441628867877L, -4731747222081737122L, 4615926322171698120L, -4736356907829779707L, 4626755227846810906L, -4750759380120958360L, 4614336858847702168L, -4733015821615474380L, 4622703026439759811L, 4475222636705615504L, 4615055442232704156L, -4737602430829007713L, 4621820822586355830L, 4485075138971194120L, 4626461437627304956L, 4485015836385435784L, 4627622892704596596L, -4746254954569808700L, 4620882143665871171L, -4731878143631901566L, 4622118858155292694L, 4476910807315836940L, 4622114400145899300L, -4733164978341421225L, 4617080634353046340L, -4739450747514454189L, 4628281301252024618L, -4730249212423061589L, 4627864107571955207L, -4732545662870090100L, 4623646512339409408L, -4734386758695737630L, 4622867228019459112L, 4483764466460723612L, 4626919228434569225L, 4482429402742967022L, 4620526985975090333L, 4475709571772501704L, 4617504871552640224L, -4731706654187703540L, 4623191551038263693L, -4736124477575951212L, 4627078925159701337L, -4732891763247161938L, 4621264735502207022L, -4764291968755568320L, 4628215899227337134L, -4731481945828479350L, 4622174474935219812L, 4483692661881625484L, 4615310570345272920L, -4729549348830167181L, 4627213249512828230L, -4772796002652960000L, 4627292643012053336L, -4735893334765707203L, 4621683686402324444L, -4740879170312482088L, 4627352275725955074L, -4734893003805591115L, 4615134727156255660L, -4730861197847016734L, 4620793607707917970L, -4730997312547060590L, 4621488841302970498L, -4733961314692628468L, 4623114113082368409L, -4734384539482712112L, 4627112399438052802L, -4733647747728691702L, 4622033100886956547L};

  // Generated values with parameters:
  // Count = 50
  // values_mean = 1.000000e+08; values_stdDev = 1.000000e+08; values_ordering = Sorted
  // weights_mean = 1.000000e+00; weights_stdDev = 4.000000e-01; weights_ordering = Sorted
  private static final long generatedMean13 = 4729051599852265372L; // approx ~ 1.423237e+08
  private static final long generatedStdDev13 = 4726778649371091879L; // approx ~ 1.044011e+08
  private static final long[] generatedWeightedValues13 = {-4498790496082827730L, 4599275936162744400L, -4499056799981824198L, 4600630994320248992L, -4500378053004556964L, 4601273001859072446L, -4500875390973165060L, 4601283613129521612L, -4501121785421031568L, 4602094488760280782L, -4501191984544177628L, 4602212285106759398L, -4501698582161439928L, 4602691603448433400L, -4501968490977805312L, 4602757587350780589L, -4503239593763652640L, 4603005291119268900L, -4503412975023229312L, 4603284731331422638L, -4503494901804211396L, 4603472936810538958L, -4505623316498292828L, 4603731764031581608L, 4685098761582990848L, 4603857307735721230L, 4709470866758177216L, 4604232831758453540L, 4711349251742181432L, 4604356622994297856L, 4717510041347826456L, 4604618454316354188L, 4719748358118274372L, 4604919408207177288L, 4719794619070979233L, 4605110743369297429L, 4719933874749720561L, 4605441029496872776L, 4720270090097307104L, 4605612353339963590L, 4720328552827321030L, 4605697907769128614L, 4722228808294265754L, 4605870556287970342L, 4723444154871136610L, 4605917853987323492L, 4726690124731515183L, 4606061482469664122L, 4726920060975346667L, 4606421920671899022L, 4727576504481627219L, 4606446528955122879L, 4727605590260975389L, 4606696365534350647L, 4727814024250061825L, 4607458054078434803L, 4728865365414604230L, 4607536494885682852L, 4729100817373691247L, 4607544807447737224L, 4729104643066546266L, 4607757076089803986L, 4729409595543418359L, 4607858874697377613L, 4729537328261229268L, 4608014261338932104L, 4730469539560719294L, 4608036299302395098L, 4730567354793888688L, 4608069898129752914L, 4730896684618208225L, 4608426620317018498L, 4730916146863948808L, 4608587276990262438L, 4730963621633912583L, 4608682792186191648L, 4731007133455452109L, 4608867238653706150L, 4731380956161639112L, 4608956867867251751L, 4731739695475711827L, 4609056197186269396L, 4731778463458223915L, 4609208505734138336L, 4731961009381648336L, 4609372216266730381L, 4732457649013259867L, 4609588406264005090L, 4732488799373069760L, 4609630035478553645L, 4732652140384090435L, 4609640556751530102L, 4733023743871770453L, 4609640967132111116L, 4733181935472715180L, 4609845483302624182L, 4733266183407845399L, 4609905485212466385L, 4733332144429532527L, 4610127054521035404L};

  // Generated values with parameters:
  // Count = 50
  // values_mean = -1.000000e+08; values_stdDev = 1.000000e-08; values_ordering = Sorted
  // weights_mean = 1.000000e+00; weights_stdDev = 4.000000e-01; weights_ordering = Sorted
  private static final long generatedMean14 = -4496888740970496000L; // approx ~ -1.000000e+08
  private static final long generatedStdDev14 = 4488335521830263174L; // approx ~ 1.200056e-08
  private static final long[] generatedWeightedValues14 = {-4496888740970495999L, 4599267382625371078L, -4496888740970495999L, 4599754846246670450L, -4496888740970495999L, 4600353263373259070L, -4496888740970495999L, 4600661300848045084L, -4496888740970495999L, 4600726739971249200L, -4496888740970495999L, 4600759185611438816L, -4496888740970495999L, 4600776156758077356L, -4496888740970495999L, 4601216412535475012L, -4496888740970495999L, 4601439727329956070L, -4496888740970495999L, 4602685850635586297L, -4496888740970495999L, 4603353132332060192L, -4496888740970495999L, 4603821973533974030L, -4496888740970495999L, 4604104046411683018L, -4496888740970495999L, 4604722518942769555L, -4496888740970495999L, 4604798724768761392L, -4496888740970495999L, 4605282740029675507L, -4496888740970495999L, 4605552435654980499L, -4496888740970495999L, 4605843364553114683L, -4496888740970495999L, 4605850548402540242L, -4496888740970495999L, 4605968930335504319L, -4496888740970495999L, 4605983368720604960L, -4496888740970495999L, 4606233669580709204L, -4496888740970496000L, 4606860441779713150L, -4496888740970496000L, 4607192786475277715L, -4496888740970496000L, 4607274488937076086L, -4496888740970496000L, 4607597539733172828L, -4496888740970496000L, 4607688096775288481L, -4496888740970496000L, 4607701579051196887L, -4496888740970496000L, 4607741000385182611L, -4496888740970496000L, 4607821963228483260L, -4496888740970496000L, 4608087171277684110L, -4496888740970496000L, 4608149521320043623L, -4496888740970496000L, 4608204073037976119L, -4496888740970496000L, 4608547105638509111L, -4496888740970496000L, 4608621724439225322L, -4496888740970496000L, 4608645272427013263L, -4496888740970496000L, 4608802451347140576L, -4496888740970496001L, 4608926497741150787L, -4496888740970496001L, 4609044578139920195L, -4496888740970496001L, 4609100383471706184L, -4496888740970496001L, 4609133091019540953L, -4496888740970496001L, 4609161540484415970L, -4496888740970496001L, 4609468036324080145L, -4496888740970496001L, 4609488642486987267L, -4496888740970496001L, 4609564882938692290L, -4496888740970496001L, 4609751712577413372L, -4496888740970496001L, 4609759695128222694L, -4496888740970496001L, 4609915197130436890L, -4496888740970496001L, 4609955452091560410L, -4496888740970496001L, 4610007433408664530L};

  // Generated values with parameters:
  // Count = 50
  // values_mean = 1.000000e+08; values_stdDev = 1.000000e+08; values_ordering = Sorted
  // weights_mean = 1.000000e+00; weights_stdDev = 4.000000e-01; weights_ordering = Reverse Sorted
  private static final long generatedMean15 = 4721323355502750142L; // approx ~ 4.510988e+07
  private static final long generatedStdDev15 = 4725284227044954873L; // approx ~ 8.213248e+07
  private static final long[] generatedWeightedValues15 = {-4498708832151547020L, 4610125236430112328L, -4499537817240010416L, 4610000852655947742L, -4499994571630096692L, 4609948513620443780L, -4501630053182308428L, 4609901380296734262L, -4501813136224299168L, 4609874040616797491L, -4502392362067513724L, 4609865198916307785L, -4502868023239593688L, 4609784483595542263L, -4504736382346397300L, 4609770415987538370L, -4513160105352294608L, 4609501759171858998L, -4518381555869422240L, 4609405013176373138L, 4693519549061922304L, 4609253925019257473L, 4707860838447620336L, 4609155984857497035L, 4708690947488801792L, 4609155976965519151L, 4718012606977865916L, 4609090782116713944L, 4718467811276654668L, 4609002328535070690L, 4718625249948427716L, 4608917900005208518L, 4718979605146786664L, 4608851568254733140L, 4719407289384104304L, 4608831471033393547L, 4719745344358551758L, 4608768803296625854L, 4719880754312488202L, 4608633925624474555L, 4720199151334013008L, 4608508134065476465L, 4720569262314070776L, 4608412406687697758L, 4721186689146714382L, 4608408458711104192L, 4721396128331670349L, 4608386319006124390L, 4721405873610756818L, 4607957849048663611L, 4723940941343138926L, 4607642644484692636L, 4724440439810267813L, 4607552810965921769L, 4724459732284616694L, 4607416548116053114L, 4725034103754586308L, 4607334953157052290L, 4726186278802463642L, 4607218017775999321L, 4727305788107576820L, 4606992621999602350L, 4727357028838434807L, 4606900215226791506L, 4727516847198837598L, 4606554251816959359L, 4728292746316264385L, 4606231350438203011L, 4728779351560775471L, 4605644847443409106L, 4728818179144451732L, 4605387303961664960L, 4729525280968368358L, 4605307731541922149L, 4730084783636382618L, 4605307242477900751L, 4730268553255041282L, 4603933982512212042L, 4730359584378582908L, 4603586358196875096L, 4730422217478299822L, 4603213330329977070L, 4730452644498290702L, 4602918267245385347L, 4730554076599452472L, 4602825066494445470L, 4730665140103670634L, 4602747387284856592L, 4731614830957391030L, 4602563063057540562L, 4732202848198239781L, 4602504437723904684L, 4732320388370021321L, 4601527186523196708L, 4732815509988439111L, 4601253754203207822L, 4732849431717724086L, 4599362595284241710L, 4733062249941087415L, 4599226168684935306L};

  // Generated values with parameters:
  // Count = 50
  // values_mean = -1.000000e+08; values_stdDev = 1.000000e-08; values_ordering = Sorted
  // weights_mean = 1.000000e+00; weights_stdDev = 4.000000e-01; weights_ordering = Reverse Sorted
  private static final long generatedMean16 = -4496888740970496000L; // approx ~ -1.000000e+08
  private static final long generatedStdDev16 = 4487359508570121021L; // approx ~ 1.038588e-08
  private static final long[] generatedWeightedValues16 = {-4496888740970495999L, 4610256126855692240L, -4496888740970495999L, 4609954021497695702L, -4496888740970495999L, 4609874900564318248L, -4496888740970495999L, 4609797772896896248L, -4496888740970495999L, 4609602307536175191L, -4496888740970495999L, 4609326736047669100L, -4496888740970495999L, 4609317124239500478L, -4496888740970495999L, 4609277833781761919L, -4496888740970495999L, 4609110886509214102L, -4496888740970495999L, 4609084363687144562L, -4496888740970495999L, 4609052954742430680L, -4496888740970495999L, 4609036969759010780L, -4496888740970495999L, 4608978241486270546L, -4496888740970495999L, 4608975618497663256L, -4496888740970496000L, 4608745554744997173L, -4496888740970496000L, 4608382157930784002L, -4496888740970496000L, 4608178032479487650L, -4496888740970496000L, 4607814457930302236L, -4496888740970496000L, 4607527842503725495L, -4496888740970496000L, 4607515798680674277L, -4496888740970496000L, 4607461253268629364L, -4496888740970496000L, 4607424113797579517L, -4496888740970496000L, 4607247899017686410L, -4496888740970496000L, 4606503100585852031L, -4496888740970496000L, 4606435666112280784L, -4496888740970496000L, 4606146567289228877L, -4496888740970496000L, 4605939002556625164L, -4496888740970496000L, 4605921792194159115L, -4496888740970496000L, 4605642841625553112L, -4496888740970496000L, 4605577533242961839L, -4496888740970496000L, 4605462139357143412L, -4496888740970496000L, 4605346342858310354L, -4496888740970496000L, 4605332935617651614L, -4496888740970496000L, 4605193302681892950L, -4496888740970496000L, 4604960525086512693L, -4496888740970496000L, 4604883810487205351L, -4496888740970496000L, 4604294854113221915L, -4496888740970496001L, 4604121431889627422L, -4496888740970496001L, 4604071009277723666L, -4496888740970496001L, 4603917313852275737L, -4496888740970496001L, 4603546237988289766L, -4496888740970496001L, 4603283101611946425L, -4496888740970496001L, 4603228306188099463L, -4496888740970496001L, 4603016531448683142L, -4496888740970496001L, 4602077440516454318L, -4496888740970496001L, 4601786268142839972L, -4496888740970496001L, 4600894813682818360L, -4496888740970496001L, 4600722973007034492L, -4496888740970496001L, 4600474592338983120L, -4496888740970496001L, 4600215458549439262L};

  // Generated values with parameters:
  // Count = 50
  // values_mean = 1.000000e+08; values_stdDev = 1.000000e+08; values_ordering = Reverse Sorted
  // weights_mean = 1.000000e+00; weights_stdDev = 4.000000e-01; weights_ordering = Sorted
  private static final long generatedMean17 = 4725473077495821264L; // approx ~ 8.494657e+07
  private static final long generatedStdDev17 = 4725922220870042505L; // approx ~ 9.163933e+07
  private static final long[] generatedWeightedValues17 = {4732999184481648652L, 4599243000096578850L, 4732792766880200008L, 4599927777450968186L, 4732499020183557418L, 4600049864257216138L, 4732468193782821042L, 4600719359017659538L, 4732386513442127579L, 4602086077896742986L, 4732298564798172077L, 4602934107325457275L, 4732287163454186172L, 4603011575740784428L, 4732193510084504082L, 4603053137251412778L, 4732057801159601555L, 4603349977721647091L, 4731236591113298270L, 4603362940485937680L, 4730904278765077806L, 4603624117418923927L, 4730746801401505280L, 4603751250567369752L, 4730639644914376348L, 4603909855504843864L, 4730617146166737974L, 4603937186236141139L, 4730592801449105004L, 4603961541594049997L, 4730500051558333874L, 4604688106463007656L, 4730235583981417854L, 4604827619374395115L, 4730039201405140034L, 4604998869738664864L, 4729937804784705020L, 4605158504708757437L, 4729836379274032662L, 4605485235569433937L, 4729801244855971145L, 4605659100264735183L, 4729703928340720910L, 4605811472626176976L, 4729639981943874259L, 4605862258219350637L, 4729483221762485541L, 4605973070224887213L, 4729210755928419277L, 4605999509961913610L, 4728934952419061580L, 4606591414952891105L, 4728895201581632566L, 4606929819761937217L, 4728052172814219329L, 4607374546810790522L, 4727333000302804422L, 4607457398511376179L, 4726871188670176383L, 4607699299323884212L, 4726605608253401518L, 4607770798293014706L, 4725404765943578035L, 4607771806167223524L, 4725115285078925437L, 4607890479043302911L, 4724904309250903757L, 4608045724225018858L, 4724601813860850658L, 4608141465663619835L, 4723860288423761131L, 4608220311273892420L, 4722601278825393742L, 4608290121910284329L, 4721756123221677713L, 4608372099280150091L, 4720849856370175961L, 4608450202610101048L, 4717525806679247684L, 4608771882405347334L, 4716502614147062300L, 4608857299001465405L, 4713194585934863184L, 4609168405339044199L, 4712258386820426760L, 4609336289650991062L, -4507377284151308568L, 4609486542672982388L, -4507058343231250572L, 4609575452287605652L, -4506131907571839728L, 4609595566269383068L, -4505861813910281884L, 4609745699542838962L, -4502795881642822280L, 4609869556062246935L, -4501228419389080244L, 4609962194848522955L, -4499838337429775692L, 4610142560480519694L};

  // Generated values with parameters:
  // Count = 50
  // values_mean = -1.000000e+08; values_stdDev = 1.000000e-08; values_ordering = Reverse Sorted
  // weights_mean = 1.000000e+00; weights_stdDev = 4.000000e-01; weights_ordering = Sorted
  private static final long generatedMean18 = -4496888740970496000L; // approx ~ -1.000000e+08
  private static final long generatedStdDev18 = 4487158870438346313L; // approx ~ 1.005395e-08
  private static final long[] generatedWeightedValues18 = {-4496888740970496001L, 4600219643936984676L, -4496888740970496001L, 4601534809829485866L, -4496888740970496001L, 4601641918154672008L, -4496888740970496001L, 4602696342165826508L, -4496888740970496001L, 4602748738195045987L, -4496888740970496001L, 4602898852711662041L, -4496888740970496001L, 4603374757467166060L, -4496888740970496001L, 4603781803059077143L, -4496888740970496001L, 4604111516698167629L, -4496888740970496001L, 4604198685927667285L, -4496888740970496001L, 4604504260924120390L, -4496888740970496001L, 4604830444814475999L, -4496888740970496000L, 4604974701600611477L, -4496888740970496000L, 4604998716036201310L, -4496888740970496000L, 4605030708411997662L, -4496888740970496000L, 4605174495685204207L, -4496888740970496000L, 4605763247302084562L, -4496888740970496000L, 4606136237801137768L, -4496888740970496000L, 4606413819176615204L, -4496888740970496000L, 4606721244735438284L, -4496888740970496000L, 4607332314804780677L, -4496888740970496000L, 4607357951240766840L, -4496888740970496000L, 4607503176247763861L, -4496888740970496000L, 4607638399785074127L, -4496888740970496000L, 4607808420131340360L, -4496888740970496000L, 4607812277116120548L, -4496888740970496000L, 4607875868873574794L, -4496888740970496000L, 4608093282852712227L, -4496888740970496000L, 4608209328382551078L, -4496888740970496000L, 4608298576670251141L, -4496888740970496000L, 4608318708930327098L, -4496888740970496000L, 4608324361304592008L, -4496888740970496000L, 4608417104321290779L, -4496888740970496000L, 4608983499661753358L, -4496888740970496000L, 4609109472047484783L, -4496888740970496000L, 4609343591254705658L, -4496888740970495999L, 4609369989465014642L, -4496888740970495999L, 4609383968979277797L, -4496888740970495999L, 4609445754908824372L, -4496888740970495999L, 4609575483796807098L, -4496888740970495999L, 4609602069927774430L, -4496888740970495999L, 4609626907198536402L, -4496888740970495999L, 4609645104761463483L, -4496888740970495999L, 4609754246041222141L, -4496888740970495999L, 4609896679791692006L, -4496888740970495999L, 4609926591914505874L, -4496888740970495999L, 4610104109132476690L, -4496888740970495999L, 4610118116324841758L, -4496888740970495999L, 4610159023623905515L, -4496888740970495999L, 4610201008281028597L};

  // Generated values with parameters:
  // Count = 50
  // values_mean = 1.000000e+08; values_stdDev = 1.000000e+08; values_ordering = Reverse Sorted
  // weights_mean = 1.000000e+00; weights_stdDev = 4.000000e-01; weights_ordering = Reverse Sorted
  private static final long generatedMean19 = 4729763595327053497L; // approx ~ 1.635428e+08
  private static final long generatedStdDev19 = 4726280932835906274L; // approx ~ 9.698456e+07
  private static final long[] generatedWeightedValues19 = {4733312957545331075L, 4610291029668768042L, 4733308692090024124L, 4610248824101147100L, 4733171486248734819L, 4610057026033602089L, 4733124237203978556L, 4609958467696884207L, 4733069473611451462L, 4609812293734112780L, 4733028565605317347L, 4609661827202087854L, 4733007911693242696L, 4609358253511998760L, 4732909488173508196L, 4609344860573852129L, 4732538667121141991L, 4609140610288250444L, 4731993885211411190L, 4608934873599715587L, 4731985138608213840L, 4608851005604478688L, 4731907272525953502L, 4608764753135148041L, 4731784850446906718L, 4608609604723632968L, 4731770955748556561L, 4608531579790996598L, 4731690582246928918L, 4608487705985662321L, 4731304705795061470L, 4608238000203457613L, 4731155752830728836L, 4608226865067307694L, 4730801524748876708L, 4608151618107194272L, 4730053239346280152L, 4607921226070773459L, 4729338782661369680L, 4607757988956072990L, 4729331634998767013L, 4607457860491590302L, 4728745700387915202L, 4607379006672661612L, 4728168191079320214L, 4607359474213854403L, 4727553903011260134L, 4607043238268105134L, 4726296368394567859L, 4607041499016315206L, 4725485056334663450L, 4606282813491686328L, 4725275698924794714L, 4606207243067013740L, 4725038044418993537L, 4606110835041323621L, 4724924634878609463L, 4606036968489691858L, 4724648197537457752L, 4605998443092721192L, 4723707407486817127L, 4605016745594498758L, 4723592640628771082L, 4604007901254436590L, 4722310727228597508L, 4603804278787235116L, 4722133200277045038L, 4603609779226099578L, 4721900873059503054L, 4603594702599562834L, 4719415524614745308L, 4603505498850385580L, 4716908633564382572L, 4603448178847696007L, 4715882709357019192L, 4603329349776946346L, 4714764457560315272L, 4603136744145288528L, 4713886205231812440L, 4602761155816108446L, 4713246639258028224L, 4602395771064089014L, 4712207897508770640L, 4602337853387033456L, 4711044429284260192L, 4602083655526287874L, 4704433831010032448L, 4601104105989320264L, -4514166752890659968L, 4600971536090107114L, -4508326751420768616L, 4600629609988657482L, -4507188757947419804L, 4600486053188834556L, -4504723476069759536L, 4600189770331389142L, -4500496976694656396L, 4600015245688429840L, -4499979187342138016L, 4600014500928933834L};

  // Generated values with parameters:
  // Count = 50
  // values_mean = -1.000000e+08; values_stdDev = 1.000000e-08; values_ordering = Reverse Sorted
  // weights_mean = 1.000000e+00; weights_stdDev = 4.000000e-01; weights_ordering = Reverse Sorted
  private static final long generatedMean20 = -4496888740970496000L; // approx ~ -1.000000e+08
  private static final long generatedStdDev20 = 4488437861994039139L; // approx ~ 1.216987e-08
  private static final long[] generatedWeightedValues20 = {-4496888740970496001L, 4609987484620225122L, -4496888740970496001L, 4609963731507295762L, -4496888740970496001L, 4609932717712172531L, -4496888740970496001L, 4609863506549751178L, -4496888740970496001L, 4609832838323187908L, -4496888740970496001L, 4609698412626350796L, -4496888740970496001L, 4609485213319673755L, -4496888740970496001L, 4609214231136599506L, -4496888740970496001L, 4609128009430682722L, -4496888740970496001L, 4609085363433082052L, -4496888740970496001L, 4609017114136408197L, -4496888740970496001L, 4609007573223631854L, -4496888740970496001L, 4608963172417166933L, -4496888740970496001L, 4608927703417114035L, -4496888740970496001L, 4608834343597127195L, -4496888740970496001L, 4608796511470792714L, -4496888740970496000L, 4608623095908166476L, -4496888740970496000L, 4608435055171268659L, -4496888740970496000L, 4608231703694719666L, -4496888740970496000L, 4608204300454253974L, -4496888740970496000L, 4608117510925256542L, -4496888740970496000L, 4607755348931395595L, -4496888740970496000L, 4607718081691843948L, -4496888740970496000L, 4607654847314612158L, -4496888740970496000L, 4607566126817944956L, -4496888740970496000L, 4607002074476898443L, -4496888740970496000L, 4606886182906628809L, -4496888740970496000L, 4606735755371994068L, -4496888740970496000L, 4606506390063603848L, -4496888740970496000L, 4606094150726185171L, -4496888740970495999L, 4606057467756467830L, -4496888740970495999L, 4605995941483255511L, -4496888740970495999L, 4605738827305882368L, -4496888740970495999L, 4605648554546998406L, -4496888740970495999L, 4605286567884648384L, -4496888740970495999L, 4604998018960317336L, -4496888740970495999L, 4604542842800166357L, -4496888740970495999L, 4604441347458913750L, -4496888740970495999L, 4604276097401260748L, -4496888740970495999L, 4604042907961911272L, -4496888740970495999L, 4604013205194616075L, -4496888740970495999L, 4603627183320266971L, -4496888740970495999L, 4603483536616963786L, -4496888740970495999L, 4603162764834169792L, -4496888740970495999L, 4602193867971575358L, -4496888740970495999L, 4601416240080739944L, -4496888740970495999L, 4601186760687761834L, -4496888740970495999L, 4600545588752667526L, -4496888740970495999L, 4600526847511027730L, -4496888740970495999L, 4599295393003518968L};

  // Generated values with parameters:
  // Count = 1000
  // values_mean = 1.000000e-08; values_stdDev = 1.000000e-08; values_ordering = Random
  // weights_mean = 1.000000e+00; weights_stdDev = 4.000000e-01; weights_ordering = Random
  private static final long generatedMean21 = 4486936556081660245L; // approx ~ 9.686164e-09
  private static final long generatedStdDev21 = 4487127061615094098L; // approx ~ 1.000133e-08
  private static final long[] generatedWeightedValues21 = {4491540233991173268L, 4599804933170123798L, 4490153283215333999L, 4600655284505474174L, -4747478281542325896L, 4609243355559018474L, -4779064718829616128L, 4609772679525585356L, -4742139489544398730L, 4605350667340689268L, 4487646339256629631L, 4609221416057802864L, 4474022614560258192L, 4605882418008086958L, 4492671601531069228L, 4603893078909979710L, 4491870802262653885L, 4609842875500032950L, 4480075980171318570L, 4607852642864509198L, 4483984502951314592L, 4609887219745113336L, 4492700287133668600L, 4609636813941128916L, 4481966613408838375L, 4603278445378035606L, 4488228489831536273L, 4607153180243800678L, 4479843212973248444L, 4605624252984141075L, -4750919758412792352L, 4599209605131677942L, 4481776888310972420L, 4606229908515187405L, 4483349488493245497L, 4607072734529676090L, -4739572471320616704L, 4608705628562934122L, 4478264032112437680L, 4600561998515727506L, 4490665583294077788L, 4609457530557120513L, 4489345984460929928L, 4606748595092868858L, 4493492856013038485L, 4607858172842111416L, -4740734605447331128L, 4606468087096291101L, 4493024973396033209L, 4609973307141544954L, 4493778189232192441L, 4599646013660961576L, -4749514365202775392L, 4609567856097815044L, -4740252231249761352L, 4608778599343033665L, 4491828372884852872L, 4605783933730936618L, 4486784948239047479L, 4606834561589935862L, 4492597595498135696L, 4603665146476615096L, 4486673651680893260L, 4604890486576718827L, 4480547574800609450L, 4608856024762748914L, 4491095486357917974L, 4609512748233354711L, 4483292911631332728L, 4609657253103066332L, 4479841092213890038L, 4601610358015034704L, 4443526151097305088L, 4606120871796208872L, -4739793419735438244L, 4609393076641231297L, 4488355852638528857L, 4608024132832682797L, 4491862711634156600L, 4602943312497601681L, -4752884349290978016L, 4600831494352007706L, 4489296196102298724L, 4604758912932816856L, -4739098736416704944L, 4609327303918855465L, 4492396617481601467L, 4601702951657848426L, 4490416494881954573L, 4603467440002486054L, 4493224743229409570L, 4609116487946977804L, 4486495618844620111L, 4599908259192819958L, -4750258814804544792L, 4608356883427076402L, 4485044809074326584L, 4609322818031187358L, 4473034869079357872L, 4609236227713094926L, 4471860473816477904L, 4607525978470644565L, 4481463414266357447L, 4609079670068741788L, 4479681298298790440L, 4602791793153776688L, 4487589232130744199L, 4605685521676040444L, 4493506913615497083L, 4608029702696107048L, -4749128770267233368L, 4605702754157425703L, -4746395582155967720L, 4602288321665042112L, 4485863330348189705L, 4607228027909363312L, 4490147669695205562L, 4606355028632877613L, 4493436195818781605L, 4600243901168877462L, 4493539812073768801L, 4608033529744568879L, 4490810183545920703L, 4605431474473457378L, 4492292317792004792L, 4602752209873971742L, 4491055556510858322L, 4608383351330146691L, 4478276336694259442L, 4610028452358289074L, 4489604858604895470L, 4610108141115651184L, 4487414217637595642L, 4608912187050344868L, -4750201380012250056L, 4603408561602436214L, 4484582492531418851L, 4610108681520653326L, 4487117012729471032L, 4605503393312672154L, -4744227786578279144L, 4606120865818886838L, 4490085083431074851L, 4604156621675846928L, 4493231768416401437L, 4605738520011411353L, 4492519377372176934L, 4607395703639422733L, 4473125039292664480L, 4604262929998408022L, 4478479076999049878L, 4607280886689622428L, 4493353170909876706L, 4607231136160172475L, 4490543174337524202L, 4608183806218652904L, -4737989737513110108L, 4609460401059532060L, -4746037370701957436L, 4606690317406941898L, 4482708663574686989L, 4606618130823373653L, -4738282561385551240L, 4608552023475741971L, 4493193723538333956L, 4600899624897877450L, 4490349660049316656L, 4609975696766981920L, 4477880187305077380L, 4605539516178652904L, 4488131523120324571L, 4608642648117065945L, 4489369069540697356L, 4599762032699624136L, 4492251524691902570L, 4607911804478847391L, -4739166075949717140L, 4610104045964866452L, -4740613081022833152L, 4607410386269873721L, 4493831544901157703L, 4607921192605806678L, -4739387183076188888L, 4609767643822646283L, 4493072997683243366L, 4604429503593621366L, 4490547987995450139L, 4606433823609659553L, 4491075562792062470L, 4608274138795250158L, 4492765612947257618L, 4609098727424512094L, -4739645222683544320L, 4609107612593458665L, 4486056594220993681L, 4607155884258231424L, 4485768594300396347L, 4608333719720550955L, 4493044603479562510L, 4607991957831332897L, 4482323309525729641L, 4601992623210377060L, -4742494358832681364L, 4605159640349438848L, 4491962847006575623L, 4610169709529305024L, 4492049746389248609L, 4609429217074385614L, 4488896553101028020L, 4604979925740632881L, 4486761388804408123L, 4604243116809192780L, 4493741495558145317L, 4609291515103821966L, -4738388858879707288L, 4608235564631730841L, 4491222782532860032L, 4607485277446947648L, -4741265254635147500L, 4610117756552159740L, -4746846098074721544L, 4602144451048610216L, 4492599602524299590L, 4610010671976058708L, 4493431164764692761L, 4609175823558981664L, 4472156808059434032L, 4610257469614072265L, 4487703550603143299L, 4603628848230427226L, -4739129859490846092L, 4610200638238436829L, 4490507837736752254L, 4609288633678495238L, -4748893199842720152L, 4600470176156704586L, 4486851298223760174L, 4609099450014603238L, 4488292563579958522L, 4608932078678241210L, 4493412210864463778L, 4603066902103559142L, 4491062859036394120L, 4608092653011388733L, 4493734172680161485L, 4606431561079576174L, 4485142927047860828L, 4609312941938640946L, 4491916991602780206L, 4610188293294304198L, 4493102140188825622L, 4605436100907495869L, 4485932057359057554L, 4609834075796115949L, 4489615080406102651L, 4605453760989435201L, -4744422756961027532L, 4607216098081813139L, 4487776614156515858L, 4609828328333102728L, 4484928595126430866L, 4600836676385907408L, 4467158253954376544L, 4604656801482005850L, 4490122605532705682L, 4609124907234840742L, -4740887875168209658L, 4609923482314683377L, -4753147523685231824L, 4608055515674971035L, -4741463144225600262L, 4605735028475278146L, 4483244978654785992L, 4603906327280950436L, 4483413467644372126L, 4604066657290376445L, -4752727778388198048L, 4606459537456048635L, 4471506227930082176L, 4602917062343588784L, -4753841915533830480L, 4609199772831740714L, 4492072232026213264L, 4608976147721659626L, -4744509280578612144L, 4607267908139239754L, -4740273348995155332L, 4607871852664745706L, 4482723942732794314L, 4601413472990207836L, 4489030762499176604L, 4607809860044082949L, 4492573308351774052L, 4601228847469157500L, 4477220124420009020L, 4607838907650271998L, 4477234756756792592L, 4599298432121871956L, 4490978079231401736L, 4607848041494041652L, 4485906219115031391L, 4609903770457146964L, 4490350481318362775L, 4609541876210816410L, 4484478665287570171L, 4608652810492104196L, 4489364590716534274L, 4608787084535611555L, 4492900278016836815L, 4610022486047846316L, -4758121753916623104L, 4600776787440655206L, 4489996171875967854L, 4599759497117094456L, 4487584021793976594L, 4603916721920672944L, -4762854985943233984L, 4607943722709904460L, 4493718184537845559L, 4610212805131425134L, 4491199998618888420L, 4608095239749981744L, 4485193676719400073L, 4604174871526329158L, 4487753854132842647L, 4609446856991966982L, -4744513262954804884L, 4604199711931763873L, 4488212137858950931L, 4607213550425510795L, 4484150689789234214L, 4609660443819611227L, -4740748795038225336L, 4608438931742363808L, 4488419956526319004L, 4601174179625050280L, 4491701253423872858L, 4601289406375651832L, 4488023872458694778L, 4604819772077420098L, -4748825876100980280L, 4609905987837159889L, 4483515104540094044L, 4608525981867719338L, 4486878563293492490L, 4608174217652403505L, 4490676680361968270L, 4610066819151758466L, 4483297967488409092L, 4606768546205607678L, -4739176583559752844L, 4610091988072384582L, 4490569838742053679L, 4607254370030455061L, 4492551908830270106L, 4610149144755530174L, -4739938781870797316L, 4605241155865790474L, 4492145647779248755L, 4609710754910839770L, 4486084797651991527L, 4608912524516388101L, 4488490125663015458L, 4610041610192598318L, 4493223508731127638L, 4602709631698440952L, 4487747636844471886L, 4609983578747627573L, 4486737492187215820L, 4609591251064005684L, 4492687297084310251L, 4608397680802240385L, -4752562370390483104L, 4606638197914504712L, 4485138807534624486L, 4602835966433406497L, 4486822034355279714L, 4610119613278125974L, 4480356645318296612L, 4608970494141599116L, 4488925391648465600L, 4603592285360460464L, 4491022596719937774L, 4610042848356875658L, -4745154838914709388L, 4606425538390430582L, 4484555241423841856L, 4602998943686323446L, 4491438658318773518L, 4600913917945395138L, 4492498532657988592L, 4605391273029626845L, -4738756218210765664L, 4601381035399121864L, 4492317535392559584L, 4605742857474543880L, 4491091154242375654L, 4609688088730151462L, 4481110014019838967L, 4599895326450452774L, 4487814353046822209L, 4605920939787858522L, 4491097273892903056L, 4607652014448552963L, -4751130073678533304L, 4609260772361962703L, -4743147046226435108L, 4602717516561043976L, 4478735697642203496L, 4606961673987357698L, 4491449599508098514L, 4603615556795953008L, 4488017082387549479L, 4602657158816673102L, 4492259538952935928L, 4604875944282634102L, 4462869001271443840L, 4608977305065937286L, 4484657975290363720L, 4609224413852086751L, 4470328640570808048L, 4606411702744920250L, 4482828101287737137L, 4607805206600620270L, -4755314573237613712L, 4608253152574923296L, 4486476940457560230L, 4609919320085303746L, 4493527905553736870L, 4603383932248594950L, -4741314402471708176L, 4609433218228185108L, 4493145312583480509L, 4608503424005044521L, -4754778782021219792L, 4609197108737519664L, 4491395244794023858L, 4610194514724016056L, 4481832998560889789L, 4605429375328774780L, 4486139619416971768L, 4609160603116937422L, -4741626187396684992L, 4609748245989198536L, 4493494092471602076L, 4603640048568428811L, -4763348126766187648L, 4605087508151386605L, -4758914329491800320L, 4609666881130626174L, 4492095851418667256L, 4608776846206563680L, -4740687455280058556L, 4607653233984652277L, 4482835526133701324L, 4608847746244849540L, 4487957094625780139L, 4609021991969466257L, 4490186270710224476L, 4608927122214020614L, -4743163879084876228L, 4604167157767453567L, 4491305303431766333L, 4608459202608064466L, 4486515876962578815L, 4603113476354189165L, 4489584553692927336L, 4610199835483768174L, 4493372528254189761L, 4606044443891205642L, -4742231276616475422L, 4610106725094018806L, -4743191885628795940L, 4603258549193211116L, 4492069438984257654L, 4607419049412134163L, 4482657795326372969L, 4605397862021594686L, 4488252130136811812L, 4602707208016877832L, 4490745183742659533L, 4609877576606978391L, 4475275352030155000L, 4609021652543822642L, 4492324545458300919L, 4604982302740154795L, 4480950235862718250L, 4609769544581520182L, 4481468443809698299L, 4602205503304646074L, 4480567423202632476L, 4607550257067529872L, 4491090311330630610L, 4604894977855457166L, -4745631255737209872L, 4610026534451822532L, 4490417351940186800L, 4608228101601255223L, -4739577192921919132L, 4602747794051280348L, 4485805169553873035L, 4608343274878438115L, 4487404000593426593L, 4601376071715628410L, 4492728053516318136L, 4609561419394674824L, 4490745918694151884L, 4608410540211677664L, 4478178352439486848L, 4604033085228278676L, 4478607787616829758L, 4601204367565804600L, 4490636814241707945L, 4608705448217673936L, 4491668732311237447L, 4607019400841518777L, -4743392462392811780L, 4606515939300222275L, 4483601881860876489L, 4608380000655381098L, 4493514640125452076L, 4608266978514160391L, 4480911201547938044L, 4610040387965287328L, -4745786826320496292L, 4601708105025649424L, 4491421068532816796L, 4603905541602211319L, 4489320033577670800L, 4609671666852128772L, 4491661985214085548L, 4601668748555213700L, -4742144417865542772L, 4600863341051640104L, 4493130926321417072L, 4608977525984690816L, 4486532510886922389L, 4599689199214666880L, 4490139035321729968L, 4606314190893562004L, -4748325492541966032L, 4609652294696528436L, 4493704250633462092L, 4609031372039613123L, -4748711662129231000L, 4604161231736073040L, 4489309889245741560L, 4604322042767421484L, 4482489223634703079L, 4609630040567154304L, 4492492717656372554L, 4602923460935665948L, 4489827021269557830L, 4599575584481026548L, 4487739021872754080L, 4602939747619130014L, 4478652834782485472L, 4602253594990491824L, 4491514977513892383L, 4607719768413558173L, -4749710629312193512L, 4604946012392166313L, 4490317239760911310L, 4608637927159881389L, 4485323264511054231L, 4605716108199758670L, -4743799915964195696L, 4601385520599525002L, 4491872885658156634L, 4606676660370489815L, 4460531460644019264L, 4606254770988330800L, 4493067146737679000L, 4606716697210410465L, 4481179284749393733L, 4607919243232099745L, -4742524746703079736L, 4600683398048746636L, 4487593197646583030L, 4599881526384891492L, -4738467329852626176L, 4607754799188076566L, 4493533725336066838L, 4607652786099295774L, 4481860277151659650L, 4609346200665548908L, 4491111028649186678L, 4607810376167231342L, 4493074642797824262L, 4606458856220873918L, 4492456983168438894L, 4608999695790983978L, 4474272040453482520L, 4607135483745042199L, 4486692144433867270L, 4603812434193782737L, 4481774073265018866L, 4602176786225110718L, 4493366153767592270L, 4609034237083821454L, 4489250603814101406L, 4603761551682828662L, 4491798474710070966L, 4602718647673489118L, 4486614301135933338L, 4600774467798102346L, -4738978481603205592L, 4609990740024029878L, -4753573885884262912L, 4607962955701455062L, 4492002329420363706L, 4600373186833261350L, 4489637583929962260L, 4608132292095151006L, 4481486944748455726L, 4607186683123482740L, -4772586780604647168L, 4609065456998158326L, -4738574022820360592L, 4607470776937925603L, -4750036797548820320L, 4609235256444701300L, 4485218175240985392L, 4606486776792106321L, 4489836940222422626L, 4608295500084683822L, 4492772322508202215L, 4600573681760364952L, 4489445280838985184L, 4602964342403705060L, -4741215123009366538L, 4609240388431521528L, 4485577356870544322L, 4607427199299493611L, 4490740884402070619L, 4600239228866442806L, 4473549359063230736L, 4608589717489162592L, 4482550279814253268L, 4599726375156754620L, 4477098851622994356L, 4610247204513275813L, 4492988802603327810L, 4607069568256119840L, -4741779113679242744L, 4608743538862346173L, 4486679479955417039L, 4610025044557711752L, 4490781018101584473L, 4607664783307984185L, 4486097394513757522L, 4609972250966737147L, 4488982091940403499L, 4607968964516778247L, -4754016728701022512L, 4607308666391979179L, 4490950723377147218L, 4608247311966371761L, 4492247476050169298L, 4608917067783872534L, 4478127383476727844L, 4601819686492556412L, 4491838172657909788L, 4606255068812004753L, 4490233252178563962L, 4608261317352620734L, 4480612912377411992L, 4609631664101698854L, 4488880412948304730L, 4609611096351948134L, -4744073311333288200L, 4605516735562729390L, 4491121624028021954L, 4605731456492192572L, 4480210107316939904L, 4610209297138770820L, 4484387493631002954L, 4610076247328957308L, 4493638025582209541L, 4604766501896694642L, 4491841898309679122L, 4607193424264701001L, 4490030331309492118L, 4609414812109566594L, 4492248902459554108L, 4603634434357581380L, 4490315413540606538L, 4602506607612609820L, 4493068274850024587L, 4609284395291318072L, 4490934634600289663L, 4604349260685494360L, -4738627404326553968L, 4604309988795813965L, 4492770407328431387L, 4605345140223924430L, 4490711621105634212L, 4607211544165218115L, 4472399322421403048L, 4603145311950429217L, 4478780915744630454L, 4608233468243603394L, 4489041528201398609L, 4608962333581902169L, 4489867230582406000L, 4609506979652694944L, 4486025925538331529L, 4603564397157962862L, 4487967506540079367L, 4607963270991941019L, 4469498273935827920L, 4608327394035470657L, -4740567497423446156L, 4604578854509758352L, 4492882265716011574L, 4609608327080369343L, 4493785951589487341L, 4607167001071702993L, 4488392133478797784L, 4604591692304745392L, 4489431546401177213L, 4608202471135732706L, 4491757903237279288L, 4609168537375377696L, -4745246392878076376L, 4607781679050769696L, 4491299270272747794L, 4608495355202880715L, 4484625604466518478L, 4608339544611940572L, 4489436478145575556L, 4599800129753943034L, 4491643021450498431L, 4603512820435960107L, 4486125976621755518L, 4602833847167102962L, 4485885613237039774L, 4608537645817249538L, 4492592022747403678L, 4608849020608576049L, 4481481028575585446L, 4607878230083561243L, 4489736439369057820L, 4608022473918102694L, 4493093844784629610L, 4600507894795597088L, 4493138542786125228L, 4609073551635639727L, 4468526617154594112L, 4606403194519912654L, 4491071048251673820L, 4607288793841440638L, 4482538976011074098L, 4606774836951447206L, -4756407483959971776L, 4607389728091059396L, 4488962620783385543L, 4607732326126942985L, 4479455679342547104L, 4609605782813399012L, 4487092347306587501L, 4608275958631353254L, -4755858419408824224L, 4606332025532082447L, -4738690265864648892L, 4608727918405421039L, 4485294957714001664L, 4607882879062767580L, 4479335643006089440L, 4602993010845749970L, -4740543085060038320L, 4609325799694481768L, 4488646615019093646L, 4607390706026300436L, 4490131803461454648L, 4608802780408840726L, 4490173742209546985L, 4604212570956013234L, 4474362521696173344L, 4606627364235288886L, 4492474472385553924L, 4603855683472638388L, 4493388151246946017L, 4607370517144376828L, 4492752995326453002L, 4609154405867754912L, 4488874695447252798L, 4603526524167812531L, 4483106818916496850L, 4609183394307512302L, 4490155305446747196L, 4609312126660393738L, 4490150005722643611L, 4603378143980641590L, 4491649424824298634L, 4608188681241317614L, -4740540460517871688L, 4608430764200819492L, 4492375447496717380L, 4607925029775985143L, -4738111780174931936L, 4609923458687786286L, 4486129620155017817L, 4607515629214331466L, 4488220161621818901L, 4603360968315702128L, 4480139799201626744L, 4606930135349818016L, 4490151774063657933L, 4605477236486227642L, 4490715676486271118L, 4608811868754564722L, 4493820566356973873L, 4608344348548671958L, 4491700154494142392L, 4609991560706691114L, 4484783066971053206L, 4600150262756543080L, -4740078747009524412L, 4607311846826035058L, -4742120476650580276L, 4607859443181276696L, 4493783129070523766L, 4600437849357788598L, -4738560796398316540L, 4609787933231151309L, 4489724380883048268L, 4606907708700809952L, -4745445194377593796L, 4609252820446841902L, 4489560123056593824L, 4609812637799628816L, 4488050760269057860L, 4604591701670984444L, 4470994554316768208L, 4606129464087266928L, 4487939410016245603L, 4606651250064908264L, -4738991043007471372L, 4607436640418709683L, 4487828994079121732L, 4608128856629551846L, -4752669650759303456L, 4603664324911271430L, -4743797116471809544L, 4608884873194434548L, 4487649730213921302L, 4607771886035188168L, 4491833840171208799L, 4603624252962080484L, 4481965780169554527L, 4608355788060809201L, 4491657352603001471L, 4602752778604530448L, -4744683287431528000L, 4602577953837608276L, 4489512537983837719L, 4601979863845078908L, 4489562916233994274L, 4607542328553496672L, 4487738948935640708L, 4610204260776557805L, 4489464963789484932L, 4608869071220159706L, -4740544098863507596L, 4606870869751945010L, 4490349073981912939L, 4602085386248236020L, 4490389317954222596L, 4608805846630600973L, -4746497826504541764L, 4610258729932924603L, 4486529366907230494L, 4607201995343542167L, 4486785584338787016L, 4608551034431689763L, 4493460258343969725L, 4608381365473820499L, -4746512877770325932L, 4608513798389786356L, 4491177456643779023L, 4608712661773455294L, 4474097033509920008L, 4608250924222652954L, 4491679239473192576L, 4609659591592589172L, 4485117224419469472L, 4607582603765590694L, -4743381074021252544L, 4606568115073280004L, -4746743300323503244L, 4606026805971897900L, 4488818466001519756L, 4608863943019565570L, 4493067402452177797L, 4608218382464037268L, 4489575952016632387L, 4607544581632483524L, 4491078563887050492L, 4607501437729671551L, 4486570946847185436L, 4607204685460420392L, 4486144977944878736L, 4608053534141997946L, 4492291493343612882L, 4609028864777807530L, 4484194658191869044L, 4606360111923813153L, -4745895892051351080L, 4608260787307066333L, 4491849905555770056L, 4609094453137842582L, 4478190735034664608L, 4605917502108699364L, -4741501697303823930L, 4609645614112636013L, -4742535072561768512L, 4608415651754787558L, 4490806882965058528L, 4600898172337660572L, -4745936633515665340L, 4606332079581173506L, 4490864757251967416L, 4605035076551401213L, 4492848087462736093L, 4609090346618585550L, 4491109940269809299L, 4608445734067790432L, 4478088480421849732L, 4604754652342392114L, 4490364054992791579L, 4609764440989752348L, -4747913969851430712L, 4604202466817190496L, 4485728933279879678L, 4609837272815324366L, 4491151007332803742L, 4604978327115063282L, 4493158728801796239L, 4602575595011330732L, 4491307718467641775L, 4603012381916864860L, -4739986443018486076L, 4604770419879972603L, -4750871971059485280L, 4610111436621955999L, 4482064471200493770L, 4609628463989643068L, 4490714690584258047L, 4607203536278653330L, -4743615565215895836L, 4603644308702058577L, 4488660651367700314L, 4604246239127145356L, 4490932478246172963L, 4609416783450954388L, 4483924066409948216L, 4605978201560995058L, 4493307520519386503L, 4605276158729987715L, -4738926174208754916L, 4604660001250485441L, 4483592925219993519L, 4608882770992286120L, 4487541269098839961L, 4601450943556931234L, 4491461749726020651L, 4607897254004770343L, 4485057567904893319L, 4608127582467100662L, 4490172870954529583L, 4609264210615609632L, 4493700147173703388L, 4602456350057031966L, -4740647086915556764L, 4605474374094678746L, 4484352261670375020L, 4602981970956394896L, 4454390986613330816L, 4609846522960239150L, 4486543000065051867L, 4600236345082693702L, 4492732547625481002L, 4602021945010109554L, 4474481358041100288L, 4609861273127531995L, 4488147990355224221L, 4608891230506980816L, 4492471065178813696L, 4600700099976242704L, 4480152328351137344L, 4606602859011290787L, 4489849895209946648L, 4607757664234586394L, -4741757925798894938L, 4604592483202200154L, 4492789295240121056L, 4605586972371041239L, 4481334130655505051L, 4607602484356951179L, -4749895901828618200L, 4602915412979139810L, 4489025520402465026L, 4609848010162407169L, 4490293255617935125L, 4608278605120028464L, 4492350055727900499L, 4605920883351541027L, 4448977870628698624L, 4605659969530938383L, 4450785074210997760L, 4602746348312614998L, 4493764779730149660L, 4605069435588656344L, 4493086810822278908L, 4605675220232617296L, 4480736802817469970L, 4605649247540012732L, 4482518777897672855L, 4604289104128199159L, 4489151274900097661L, 4605477766731369296L, 4487524140453526724L, 4609604113974968608L, 4486208072003423889L, 4602228465024217748L, 4480047927994994444L, 4603929030648868300L, 4491074008068824619L, 4606548635131922702L, 4489363718966336686L, 4608135493656444048L, 4489034771954866935L, 4608963486495106982L, 4490715236669829017L, 4608092373903423515L, 4491263126023488442L, 4609555916253766746L, 4490375921536084778L, 4609178619844233273L, -4745799152551024448L, 4608492760597821141L, -4741694847303743726L, 4605143339366080611L, 4486766150270760656L, 4609754627947904884L, 4488481167444268887L, 4608601955556696405L, 4480775669000262750L, 4608697033486892864L, -4754454602564197440L, 4607583250990604915L, 4493305693995508442L, 4607851926732592498L, -4739804096458696324L, 4607295355633110473L, 4485412459326089586L, 4603559875927084446L, 4493783903647486712L, 4610234977837369606L, 4493340854083010581L, 4603254220159855730L, -4741014819341894480L, 4609914358291457940L, 4492627890999241543L, 4601443195339724602L, -4761977051892519488L, 4603712090129615427L, 4492754931910354488L, 4609394365746399645L, 4485447634339394932L, 4608785533816281888L, 4486916646643065834L, 4608452573395974140L, 4483862742815966484L, 4599432025842872594L, 4487353670519487785L, 4608294351286141971L, 4490959397481288356L, 4599513840822503214L, -4747724845829270112L, 4607768823814772085L, 4488008019407361193L, 4606080971727815949L, 4486221466357659451L, 4603663433677532169L, 4464876238831157344L, 4608485381580528507L, 4490672486694940541L, 4607563024603711357L, -4738191173486512632L, 4608394157850748672L, 4492712977751196190L, 4605759943182183047L, 4490154814570648732L, 4604981758675363802L, 4463169381398093248L, 4607585893400195161L, -4738745589206186924L, 4606120142140621627L, 4478179633185781644L, 4609075247146740179L, -4744039355352524480L, 4601716031575938526L, -4745780730161312712L, 4605355736778445716L, 4493512930952332435L, 4608507937350453700L, -4754163822889349040L, 4609101400854787613L, 4488163864423579199L, 4604342793445676712L, 4493419678118577776L, 4607788345073992354L, 4492715474864706711L, 4608785466304179634L, 4474435864252021312L, 4600010193520755604L, 4491981259311420900L, 4604695003717488108L, 4484411039261992709L, 4608984807922601673L, 4492044287439159307L, 4607380202573488095L, -4763724032610089792L, 4603969084438996427L, 4490336344347454859L, 4608704792672834352L, 4487327967446470305L, 4599854309749994682L, 4489362711191304237L, 4604753979963926018L, 4486791209814400352L, 4604541152139977126L, 4484661661324540606L, 4610166794245687814L, -4749452324105989776L, 4609820573212547034L, 4491308898654550221L, 4603105771309029142L, 4493174374151979128L, 4604408196682722220L, 4492210282556132358L, 4604798252792618104L, -4750501231633593528L, 4608496770924009332L, 4491638843861392152L, 4606721090920511283L, 4488892910687716084L, 4605182938576539920L, 4492026918480612857L, 4608456113796900108L, 4487148369980158390L, 4608673117002376296L, -4739595395952968848L, 4607516013792833772L, 4488204412248674504L, 4601801511092767884L, -4738441093706110516L, 4604698921857685576L, -4739797021189809160L, 4609525582753826146L, 4492703120428458326L, 4607673388166697316L, -4743296355609995204L, 4603833965193133049L, -4751346519183710320L, 4607668659977158181L, -4742740190379272764L, 4600541204672243506L, 4492638792640590405L, 4602591006378314938L, 4490515863651552482L, 4608790704952482418L, -4745475849859682936L, 4607513119625209832L, 4492734680740924303L, 4607194563293955051L, 4475105362805385096L, 4605220859488469429L, 4484885679954250006L, 4602573887147166736L, 4493251253579365674L, 4604410823393605838L, 4493060253076183090L, 4606079967652381214L, 4492507105117973694L, 4605983645549654124L, -4745396461185694308L, 4608221721932260934L, 4490095068076494831L, 4608146038049774045L, 4491021777347119418L, 4610011376453926431L, -4749884915314028544L, 4608247855641988221L, 4486932039247105652L, 4599559877781922168L, -4755809016487009216L, 4605656758698356689L, 4491522419552125136L, 4603320365664028964L, 4489350631031248558L, 4608114497255566600L, 4491701004565653390L, 4601529534249963484L, 4483643734707356087L, 4599909799272955670L, 4493492210602126098L, 4600368468070881170L, 4481815993727621598L, 4601904519118329510L, -4746130688193875356L, 4605023663674375719L, 4491108834144014206L, 4609687927728206070L, 4489933360203026224L, 4609825340375807594L, 4492986568557138352L, 4602987832527252176L, -4742890282620715344L, 4606449696698774773L, -4740525423410255892L, 4603705757603647582L, -4738941447444423976L, 4608591646755156146L, 4492589784690909677L, 4609128046954155745L, 4491638493937934865L, 4606647756134087718L, 4487678078344391083L, 4608831750065652525L, 4488490823013606570L, 4610110740273930758L, 4466070453780697184L, 4601518238853579550L, 4481211668463580539L, 4600661891118332572L, 4490437873121626505L, 4608649182178554375L, 4486061427797029157L, 4609977927224739498L, 4489606669115565668L, 4605960504462589357L, 4481739305828201977L, 4602509266805734664L, -4738761160891913088L, 4607853613439568123L, 4491956144356780250L, 4607518950841444669L, 4474457433654112752L, 4607097298308744347L, 4489445432089259427L, 4604845793034239566L, 4483105235204363540L, 4609462897349375630L, 4489100876750572302L, 4609346940352905772L, -4752378743019136160L, 4603497018600915151L, -4744344293260070188L, 4608722294215997622L, 4481908693828855616L, 4609669236598753846L, 4493517315787206537L, 4599530238411109844L, 4491233728892959537L, 4605579423665335055L, 4480461694447923374L, 4603007668509520815L, 4487978038558785589L, 4607720150537904864L, -4739315957487431988L, 4607774073159445636L, -4742776698444409364L, 4608760866984848371L, 4488491847706639980L, 4601323457794399992L, 4488084796157686798L, 4606610546702842961L, 4490107900506734246L, 4601493138087893672L, -4743754947386378128L, 4608819881184237062L, 4493618293995899458L, 4605705588454072058L, -4738446518816393972L, 4609980040936499481L, 4489947860842786222L, 4606495473466523956L, 4481097500300788079L, 4608089165327349815L, 4492956033482742606L, 4605137043587816693L, 4486242336869605212L, 4609374835415494532L, 4491712507713869208L, 4608707027159926242L, 4481740056603352970L, 4609503857246762235L, 4481436076725286965L, 4599615063397967626L, 4488587339446324989L, 4604690167152993002L, 4492335565583436558L, 4608617928283746560L, 4480269238509688860L, 4610195859845673174L, -4740540805954000488L, 4607985651257947078L, 4490624737497638812L, 4609563168575941578L, 4487191277775309122L, 4607177829728768276L, -4760898093905047808L, 4607167174166609721L, 4491931305487539949L, 4606444054904302577L, 4482734353424158365L, 4609313984979474100L, 4486879332932498708L, 4603045845925879212L, 4470616851346094336L, 4603473792773594990L, 4493816025387389444L, 4602816857525488880L, 4477507416830795188L, 4603624662254641152L, 4488997543806014036L, 4607279226283354406L, 4492991150949277052L, 4609839801682354154L, 4493361109847407059L, 4608065895185149742L, 4484017950823988972L, 4607207757221025280L, 4491942173813024656L, 4609881718507119069L, -4738455594275536260L, 4608991434126992702L, 4487566669062394676L, 4609674149076779507L, 4490596885493521216L, 4603946394521097506L, 4487672938027011841L, 4609234597332678118L, 4483083807986925455L, 4608494519635673617L, 4492280146000966576L, 4609063213903264004L, -4740632943752531456L, 4607965170251439500L, -4739811247844003756L, 4606221664980162708L, 4464645889092505056L, 4609207077578588504L, 4492623956082325146L, 4601565522215928576L, 4490874816917582175L, 4606417771658992825L, 4488183704109576292L, 4607364286132093407L, 4489538301397857508L, 4600837841368817380L, 4491911050553493878L, 4604535419347165924L, 4440872094899996672L, 4610300485236118262L, 4482403237155009843L, 4606429853273098042L, 4490378568509987364L, 4609835332878963614L, -4744505679265208632L, 4602723712706227281L, -4738508685544362344L, 4603625024813742117L, 4492132560015942352L, 4605222901399215691L, 4491625356280831722L, 4604782324708645382L, 4480818189928561218L, 4609860295675851886L, -4748144434839752728L, 4609570675788920752L, -4744714016711682784L, 4609102130617343053L, 4490405210201100297L, 4603637491511932685L, 4483601068753611932L, 4608959461678866052L, 4491948078745995425L, 4607688031174055964L, 4493808622349513003L, 4607564896693257339L, 4480520889332804102L, 4602730668693330859L, 4474790333989549696L, 4606676089961091919L, 4474311577195069856L, 4609921076188124505L, 4485999945584732143L, 4606931503075854981L, 4488208835208272120L, 4605276149700209321L, -4744721768535468572L, 4607324373644855230L, 4485428711253060708L, 4604407610919142529L, 4487487169380144390L, 4607704377681761392L, -4742366726290134076L, 4607529066820324996L, 4490249109718607180L, 4608315760700018140L, -4739440896874767684L, 4609471426130540174L, 4485799795164502662L, 4603408512439851504L, 4477390174966373572L, 4609680940210008162L, -4746371892935897268L, 4609626108087864138L, 4492022023939812902L, 4605687264107515120L, 4493519566791141200L, 4609276151629416580L, -4743042387366480356L, 4603133045502364000L, 4492004063776902072L, 4606374225559819955L, 4491181928288562256L, 4607761857744332373L, -4738490109941715916L, 4610283061613601769L, 4474947796347411232L, 4600464934729075594L, 4490302145795263536L, 4599265781898338358L, 4490085683271029446L, 4608357938237414670L, -4754734816175731296L, 4602648639654699976L, 4491148330837819874L, 4603991713446716620L, 4493001846489662288L, 4602413082262297282L, 4493150017663950426L, 4602774697764889853L, 4460200520989884224L, 4609058285322062638L, -4738321910356242220L, 4609822617831036707L, 4476351338843567184L, 4605809308765712924L, 4475689276458199096L, 4604590366794058848L, 4490826552291933577L, 4609389832582657416L, -4746187428340816232L, 4609752409387823313L, 4483533037932850958L, 4606489634587140070L, 4485790890733183369L, 4602813432508069405L, 4477230227845744020L, 4603277546186029572L, 4490494643929818252L, 4599792380561987786L, 4493652180885040642L, 4605738267382158900L, 4486539366552292068L, 4600627662694105528L, 4479926331279980198L, 4603616594575930754L, 4490504344025755449L, 4600856797234663692L, 4477914311240424100L, 4609336195135578527L, 4492294419466892850L, 4607697743400215315L, -4740860490100111084L, 4603635827911240776L, 4492541381542304902L, 4608201456336250025L, 4464786895908605152L, 4607602560968257718L, -4742717577272124652L, 4602701649375524162L, 4482705965928154583L, 4609762319093555587L, 4493440555782520436L, 4608814100989824481L, 4491227874218642536L, 4608932200383297968L, 4491652321501144273L, 4605048552087831007L, 4490453506646352882L, 4608133713372578312L, 4489512336469847720L, 4605884839028318008L, 4483698151126010243L, 4603165615634074726L, -4748994141290258880L, 4610142234300438086L, 4477558449681006264L, 4604386697360756252L, 4491082424620246854L, 4607453380054794406L, -4738454414738365796L, 4605329915171855376L, 4479908674962518146L, 4602207294954013330L, 4489239201845730536L, 4610094605175791558L, 4490568348908125318L, 4609123128424157551L, 4493529494613318404L, 4608184659688759849L, 4477423179014295164L, 4609522811368787712L, 4488781600251763824L, 4601989115177499684L, -4740400262575085776L, 4606277174635432388L, -4742976731829500516L, 4604726914010073268L, 4493743114466990298L, 4604540266923028308L, -4743782843900634048L, 4599717842396259874L, 4493370205752523632L, 4608689948178527313L, -4742821528789630332L, 4603939801059589063L, -4742832468938448920L, 4602731968919827562L, 4491113524369684966L, 4607770617676185831L, 4492143312320799543L, 4603851530998605702L, 4489034854967686864L, 4609440814860402056L, 4490595763082109017L, 4606739718709709874L, 4491901965000985073L, 4606154877852804092L, 4476864919191701012L, 4606477760015177236L, 4493180688002010392L, 4600643579610713522L, -4738049464235184668L, 4603929594014934289L, 4475555509900359608L, 4601266548395726722L, 4491755545637645016L, 4604861636653956844L, -4754663783943972704L, 4599427277247285318L, 4487199774025794950L, 4608010644084824567L, -4748418004799298488L, 4609733693781030006L, 4493075764899931054L, 4608272663759530410L, 4489279214525753902L, 4609417920721731188L, 4492416617428455038L, 4609558322585771646L, 4489452342963708995L, 4607987372048170748L, 4492588854131342910L, 4604061976803347244L, 4489842360431203902L, 4607815490067812148L, -4744312112677052636L, 4603320016604807683L, 4491437125278183108L, 4610100710722185967L, 4488998007441076203L, 4607239399232556776L, 4484455104996317799L, 4607095745619981926L, 4489173836093216427L, 4608601096439730982L, -4741367861575669928L, 4602221470954315812L, -4742273802241742748L, 4609219333668324726L, 4488658199163845970L, 4603597924682471216L, 4491358927403216080L, 4605452639936275813L, 4482603908519801367L, 4609149570841188201L, 4476580330118489012L, 4609110646424581531L, -4749998109044528800L, 4608731133784833898L, 4492305579807366932L, 4605765496747334286L, 4493037268683204031L, 4608646134288252701L, 4470675302416391744L, 4609831171257960572L, 4487729238672510868L, 4608086445814310550L, 4492701428528520862L, 4603318170209041944L, 4489536614689573094L, 4609263641084540155L, 4492423736711563406L, 4604335630060881468L, 4492946559382460664L, 4609341093966940310L, 4482519703838830731L, 4604752216843012972L, 4493152785123080846L, 4601949172741255772L, 4487011657840767107L, 4606956835628627071L, 4487068766160192176L, 4608653127293386511L, 4490715700748019966L, 4608485564828268811L, 4490830909377345853L, 4601938733337744858L, 4489110362469836148L, 4602326480649269370L, 4477299476294728976L, 4608924745410984262L, 4474838075384789104L, 4607023404755360582L, 4488448665796355474L, 4609417155783036034L, 4490620483684683224L, 4600518600216315776L, 4468450240007351360L, 4603926074309177492L, 4483374306823377889L, 4609857877850594209L, 4492550755367703690L, 4602955511115534970L, 4491447299660878814L, 4608105957580352568L, 4493342919367275922L, 4601566586179581464L, -4740175574363977308L, 4610161168776711554L, 4486742962600374131L, 4599658883574443776L, 4484113609780039829L, 4602846397765479122L, 4493301444022702715L, 4604380632796083408L, -4740417554148394436L, 4608464992575116251L, 4493474228817073767L, 4609003811503236611L, 4490939085427550664L, 4609236368751971239L, 4481017566042777444L, 4606275124332659208L, 4492596941125705580L, 4602859620516328046L, -4738752362248888784L, 4605017752437750548L, -4758667818555743552L, 4605080246488165843L, 4488285171017253645L, 4607326609279677657L, 4489993223283246645L, 4608908103048456639L, 4488296726720803495L, 4605870012220601094L, 4491723502131962311L, 4608253413930154217L, 4488429713238799872L, 4605870325025010462L, 4482610393942786657L, 4609897272485327248L, 4487315862100143412L, 4607575890951259820L, 4487678931135471058L, 4609897740423094446L, 4491568471494238850L, 4606363798257392236L, 4476785679751122472L, 4609349505978980891L, 4484559097597599482L, 4607833598139783779L, 4487738658313232299L, 4607522547192905697L, 4485454467998156358L, 4608655100177781860L, 4493011307055947157L, 4606539091560384571L, -4742496261942084384L, 4609626360687488134L, 4490869377799256340L, 4609835942981197298L, 4477062409973659292L, 4607505716700539394L, 4486208097376799201L, 4599987409538834444L, 4490273589640792059L, 4608328333769716037L, -4738623471950217076L, 4608941868827967267L, 4482510242258575442L, 4600347098479143958L, 4492221477424992132L, 4606469718531016577L, 4491775363905906056L, 4607898640763004671L, 4493398203208391335L, 4605334278309646911L, -4750303223978279968L, 4601585756633596444L, -4751800825895328384L, 4609556588310672080L, 4481326679704954163L, 4610167092274720348L, 4492880203196244610L, 4606483355459377520L, 4490469506309565178L, 4602955453087909186L, 4490669937377033186L, 4607428294309659360L, 4487673215237339234L, 4603607723656272905L, 4489532634637188574L, 4607828612021081757L, -4741859263816570500L, 4605240582661577168L, 4485898650357054429L, 4607911216074457858L, -4744999815751689620L, 4607562939565856589L, 4488670863037750544L, 4604207170957342080L, -4738328679044485660L, 4606608591809642985L, 4483927250350252793L, 4600463626026488892L, 4487616935152087385L, 4603032237927634422L, 4490551970506287814L, 4608917918227608938L, 4490871472495346796L, 4607851699046762254L, 4490132927054288771L, 4606376593240693389L, 4473684227528036528L, 4603501489503513774L, 4486423201964426298L, 4602648603367273088L, 4488675987194215842L, 4606063521497556837L, 4490699355425974593L, 4606810384775243859L, 4456409570916248064L, 4608683617505456959L, 4482899454957747801L, 4603460923460549279L, 4490258429496205842L, 4607405425401189934L, 4478677979027179938L, 4605697773481984844L, -4739790948864691388L, 4609148545007699511L, 4491716363606478926L, 4603218300531984596L, 4479155641343536664L, 4604443165260303454L, 4491429790084323952L, 4606242789425131859L, 4485783855461483584L, 4608060867490556518L, 4483124661683151014L, 4605176187216624308L, 4483304779533695535L, 4609255043490986417L, 4490764260579784500L, 4604710252474169031L, 4487522220191291768L, 4607161760834587380L, 4489079076725757030L, 4602536021719838952L, 4491307657321834059L, 4609728704033687724L, 4488007724977197047L, 4604690032544499972L, -4745904363763894484L, 4603970292852713548L, 4490071244060644498L, 4607701743038683143L, -4737969947401373476L, 4609658200237039657L, 4491999340882977990L, 4607442438969650419L, 4490703993053066042L, 4609377812549145714L, 4490579025020608622L, 4609525379034419390L, 4486764331906392638L, 4608923895250098938L, 4491434606214520186L, 4601521258727805572L, 4483185169741848068L, 4608952183783640737L, 4490735065698493527L, 4606969384564871161L, 4490169386287278760L, 4607732679926810388L, 4477937748686934980L, 4608960709757888823L, 4491073388668633964L, 4606237707480895460L, 4493775474874590908L, 4609099644051780999L, 4480204805509487966L, 4602466445676015354L, 4476125320367430848L, 4603404862141864441L, 4493729609795862957L, 4607341338801181895L, 4486211550943988396L, 4606539416482405300L, 4491750263736343587L, 4608396300621265522L, -4746698500282320384L, 4610225780561185194L, 4489971306469711944L, 4610150842410896010L, 4491973242653343122L, 4603259030621787028L, 4485421533846938596L, 4603966648981167812L, 4490221017480009622L, 4602869472826682378L, -4742736516119423280L, 4607881016137773520L, -4741022470305403420L, 4602246006453621144L, 4492713075939836484L, 4600320604307164698L, -4741067410626499776L, 4605747543566088226L, 4490905046962465204L, 4605839786177353294L, 4490489343582604486L, 4606862191522207668L, 4481553383737831645L, 4603478103981801124L, 4487548784388343031L, 4608639296197199463L, 4476104804869933496L, 4608192308272758281L, 4487520907605303156L, 4607864683054035824L, 4489342806839135377L, 4603185064421136519L, 4476774888850520664L, 4601911692374385916L, 4492614256256949896L, 4607790367112899734L, -4739035435692867892L, 4609024988634051719L, 4486949182224646587L, 4601800796825680292L, 4487168909308957810L, 4599418745992352478L, 4490277709719597619L, 4604604163876690650L, 4489979405064038463L, 4609923119796034480L, 4492045206658198236L, 4608288812675131635L, 4487824865131125749L, 4606400767965114426L, 4493374371610516507L, 4607935142280854566L, -4744640950123847956L, 4602203221446205354L, 4491327373259930638L, 4600409927496571078L, -4742940032207746480L, 4608470488142155145L, 4490663376129570555L, 4606204663636840426L, 4483391711212199861L, 4602949329123282604L, 4493786818855020765L, 4610265572048611863L, 4480295368053259130L, 4609715527538561364L, 4485690071484386333L, 4609073751620509730L, 4488713919257564402L, 4607420609115347277L, 4484057835402831613L, 4609176938742434207L, 4489724356584019680L, 4607804408610152384L, -4753514556643470992L, 4608438885615179994L, -4740438100241311884L, 4603586313062864976L, -4745587793158110764L, 4607233917404942576L, 4486983943604246142L, 4607508866702279157L, 4483878851004834082L, 4607123773852035980L, -4739898240681551900L, 4601077654378727432L, 4491759357018696746L, 4604102040898259306L, -4753851999661169600L, 4609457031152991628L, 4493776768917157085L, 4600540034496168678L, 4488141446981519614L, 4599581485841675150L, 4492432895193422161L, 4608813917745075626L, 4493388204912802106L, 4607997919048391655L, -4739160326749837484L, 4607513826541951387L, 4481666030131508817L, 4605318289897269857L, -4739885939184105872L, 4610199744061551604L, 4490044410939923986L, 4609813048603618684L, 4488239338476661726L, 4606873282630983666L, 4487414247482871222L, 4601612047488647802L, 4493536492285614228L, 4600571569180550542L, -4746411328527192904L, 4605902781742643508L, 4486297960124176608L, 4605487728079873046L, 4471146630860405952L, 4599848010875103842L, 4485841492857814406L, 4605016468877600711L, 4484525159973106272L, 4608267050129995981L, 4490633181904040784L, 4607429913555465478L, 4479833378438558690L, 4608372853870760708L, 4491201187306750365L, 4605535450011844495L, 4487974852505187564L, 4604174064775400508L, 4470231089859958448L, 4602505660725557594L, -4740918309511100486L, 4605743268851978462L, 4487049249410990592L, 4606952721920126137L, -4769726159074461184L, 4603828090528066574L, 4488826426519289832L, 4609085318260617151L, 4491869906678550983L, 4600687448982933550L, 4492809099564497568L, 4605945285269715295L, 4491003979789277886L, 4604511186330973961L, 4491645621655055354L, 4602134677639646284L, 4478752536241408926L, 4599623521996274426L, 4493587047184732470L, 4608222985853967026L, -4745909515033301192L, 4609584792685493484L, -4748756434701535600L, 4607308433655508320L, -4740945687140339812L, 4608719541174119332L, 4488310776964169086L, 4609703480618817879L, 4476586578975800032L, 4602010161188982228L, 4492905415180764318L, 4604771215122270252L, 4485681829681560749L, 4609466438526867912L, -4745547072681887340L, 4609157371110256468L, 4492594813116731065L, 4600965719352963898L};

  // Generated values with parameters:
  // Count = 1000
  // values_mean = 1.000000e+08; values_stdDev = 1.000000e+08; values_ordering = Random
  // weights_mean = 1.000000e+00; weights_stdDev = 4.000000e-01; weights_ordering = Random
  private static final long generatedMean22 = 4726705036813918811L; // approx ~ 1.033042e+08
  private static final long generatedStdDev22 = 4726407319818760615L; // approx ~ 9.886787e+07
  private static final long[] generatedWeightedValues22 = {4725836250534348312L, 4599886994714070562L, 4722415929511815035L, 4608006539324274731L, 4725557106991409250L, 4601327155176438538L, -4498741296865622082L, 4609711489153659389L, 4716535840616736076L, 4609037207067509786L, 4721450764621891016L, 4606845559575176686L, 4726524735500950854L, 4604353208982976165L, 4716229491361611888L, 4607811531247356214L, -4503286484125992076L, 4604292372132072560L, 4726447452604788079L, 4602075553195911096L, -4501765083551091924L, 4609309036122205032L, -4500427476087949400L, 4609977964301334669L, 4725592725887799898L, 4607584551874394568L, 4733019987062832888L, 4605310582783161365L, 4722760869833019904L, 4608997494097438017L, -4514001761286084432L, 4609176442670129369L, -4499309696040162648L, 4604788587727536563L, 4730479333972404206L, 4608498917811537016L, 4719489944407036456L, 4603847314653826358L, -4507372287769230364L, 4605764506879679824L, 4730759686282728292L, 4600160239410162550L, 4724767293051560588L, 4609879281903779332L, 4728600599698336474L, 4608135940635735420L, 4731839599923095534L, 4609853613982924849L, 4730890917796433252L, 4601045352010969260L, -4498836349206726230L, 4609059480035008267L, 4731899970276157168L, 4607339878201282663L, 4726089173826266591L, 4608950360861617059L, 4730352617742223579L, 4606345873562577080L, 4727075452052564111L, 4609214591264202722L, -4502857037858147336L, 4602552664565160986L, 4733175096473136638L, 4602512367798429216L, 4727780664276602238L, 4604993313871689775L, -4508302524972857968L, 4601711415184110252L, 4722277566313191251L, 4604191855178099904L, -4502619130770087360L, 4607244709311794417L, 4729161818060320997L, 4602149213366387194L, 4720592194073870601L, 4606695903370400311L, 4720204844986113124L, 4601301997352536100L, 4729855649621116762L, 4604363987969356640L, 4728408014128064185L, 4606721011355419937L, 4729148759412857614L, 4609760159668947998L, -4517862437278606560L, 4603630768054570713L, -4502079071641651772L, 4603960986316344126L, 4727792977754249310L, 4607196432104885621L, 4726207068259994339L, 4608601354983617037L, 4729122655436628030L, 4607308328286403871L, 4730849526224095384L, 4607283701482408238L, 4720442442600264385L, 4605630912131042682L, 4720555388259552570L, 4609218142535355685L, 4725906684731881988L, 4604878101927225934L, -4520530264289909952L, 4605548683628004351L, 4732309334117721853L, 4606160594938624438L, 4715624614070894312L, 4609954870930787984L, 4733284665293026330L, 4610108306244021894L, 4728887669050208233L, 4608507685751891562L, 4729463350492178204L, 4607644613071629693L, 4727698373817539230L, 4608263282707366112L, 4718655927870106988L, 4607943680148787522L, 4726494602500996068L, 4609902139935744896L, -4507342243222916652L, 4605786186226182228L, 4730952078529370330L, 4604652730903840056L, -4502687029867432052L, 4608151874722843822L, 4731705004837112144L, 4608548898111922322L, -4499115907392801796L, 4604394687883287137L, 4725396294781850162L, 4602889321026287437L, 4731297366074571871L, 4605889716505473517L, 4731439315138003744L, 4599964225516655704L, -4499148732339516592L, 4608112558140795215L, 4726836692986663209L, 4609084649357424034L, 4732600259243511659L, 4600819072864857496L, 4722556095441661369L, 4607955006481032604L, 4711538810005229520L, 4608092202291117774L, 4721545546952855934L, 4605590391193914263L, -4504092113890998408L, 4603100566888427236L, 4724865930714910228L, 4603668980683384946L, 4710170076224935792L, 4608230510452105943L, -4505549855580098204L, 4606869250186842088L, 4719956409510728815L, 4603614479083430497L, -4498793882804921062L, 4607861987084778120L, -4506813447332195852L, 4601340702811035890L, 4724291064834980999L, 4602962846125913464L, 4731068647241513648L, 4603758033491265870L, 4716201237141962132L, 4608560206756468060L, 4732092967927487902L, 4599825755168149500L, -4504095972979607432L, 4601377586204200908L, 4731329621432404503L, 4604472210728008730L, 4730528934022174816L, 4609573317840061082L, 4732306086025651302L, 4605006554533254852L, -4506433209045231368L, 4608943431932282932L, 4730191560851137430L, 4601494595173775566L, 4725304853306199065L, 4609562020233502974L, 4732357236018686391L, 4607361523218688230L, 4731384327077956309L, 4608107647751792227L, 4728664434996486824L, 4601401207930050918L, 4732209676606598771L, 4603008813461578055L, -4499744305147099212L, 4609086797121020730L, 4729795039016923789L, 4607914054275531975L, 4725899241507933688L, 4607480651841736631L, -4500934030010567992L, 4603285620152941014L, 4726718345724894591L, 4608223236442878153L, 4728356024415265239L, 4607161731824198192L, 4732042547576406808L, 4608297545309845296L, 4729551532806059572L, 4605502131296688102L, -4499283377642430876L, 4601758528590966032L, 4726407504150577974L, 4599215751496904308L, 4717713623223696304L, 4600516237620675942L, 4723080480006692671L, 4605208857131648156L, 4704452478500742784L, 4602191124493752756L, 4726949843862841842L, 4606900750547418278L, 4729322849134211778L, 4607606483728222086L, -4499404814937712020L, 4604412656840279528L, 4721668196827327364L, 4609115837766320794L, -4500020844265394972L, 4600874246943519770L, 4730446094594336962L, 4604397138896624672L, 4733079069137492166L, 4604772662811618882L, 4729411001363907470L, 4603781888258450404L, 4725282516581503295L, 4606799691127632282L, -4502209473521769960L, 4599588995553261094L, 4724481636571746324L, 4607522627458851218L, 4731576958278388586L, 4602941285760404378L, 4728311299559098426L, 4602818365527326195L, 4710099537846672672L, 4606543880897314619L, 4722726449930724724L, 4606981337173498479L, -4505925496783785576L, 4610067372995212538L, -4501136700854403432L, 4610025064487391428L, 4722596149918325964L, 4604719186318086954L, 4729951990654606732L, 4606123841325463910L, 4726059209678521088L, 4601205672394648082L, 4724372475768185392L, 4610246162413242919L, 4721091170267722780L, 4599614997859556916L, 4725336134481981733L, 4610240720163201400L, 4733120917040847153L, 4606235013072938002L, 4706163175171037664L, 4609429380756283627L, 4730854860503262818L, 4606703117098808859L, 4730985139223980442L, 4604393881530957474L, 4722627556463350303L, 4606427633812113896L, 4730820902752303687L, 4599831275392511846L, 4726462676649310663L, 4603886425803948016L, 4722324287442554207L, 4603285820276044048L, 4731921936629917420L, 4605235653220662838L, 4731690384825146854L, 4609318201091027660L, -4504645925681664792L, 4604693291602310236L, 4732595412483015382L, 4607496278699821759L, -4504238985669224828L, 4603106516906242712L, 4725518747004167088L, 4605532850856494141L, 4732486157381528398L, 4607020003622428194L, 4720159934813350211L, 4609621931200851714L, 4732896926246569199L, 4602287412324867112L, 4717269333277405896L, 4604194653568140071L, 4724229977084852726L, 4604376930239942554L, 4726014697232405621L, 4599970923281814832L, 4722217886907868584L, 4610066323399414440L, 4730814293288659016L, 4607885478301023319L, 4732414150397476362L, 4604379033669147446L, 4728511681686620134L, 4601224678420990524L, 4732530706496621626L, 4609314409072670944L, 4719104537073625580L, 4599431840832397672L, 4732223850365539670L, 4605994952898599927L, -4521461764170358688L, 4603388782874215524L, 4726903266804731614L, 4601113968821045150L, 4718874998043068424L, 4604468480544674024L, 4724664769756369224L, 4604974418378034031L, -4498734480335312044L, 4600848442627805266L, 4724711050437551990L, 4609370993535561670L, 4728042805371277107L, 4608080039800097344L, -4499274745598358944L, 4602942081863262514L, 4732943278934395212L, 4602217058161211284L, 4723077233528410144L, 4601910071972734720L, 4714341780223992888L, 4604216961827687404L, 4733099229179392388L, 4600834157588997132L, 4732837416829765952L, 4609208911011489416L, 4726759514516391991L, 4606497269860010390L, 4716635824003125656L, 4609560718390958252L, 4732070419467790274L, 4603401875497080000L, 4711448636489794304L, 4604562733882334460L, 4732756343732706876L, 4604873114619909228L, 4728578206220796082L, 4609973783542923042L, 4732101975770832442L, 4607570326576042855L, 4732499151329997245L, 4606927442142530993L, 4722584982917123159L, 4606806232999866390L, 4731043886548612610L, 4609504632514712907L, 4726880382366796522L, 4606504594356546026L, 4730580460407511148L, 4606483835870033746L, 4733357558908124802L, 4609522649816794632L, 4729252012730236826L, 4609334780938629001L, 4723537322427066455L, 4606031689840003732L, 4728802331856040631L, 4599911894324410180L, -4502155133643575884L, 4602912419294881560L, 4724131843170189210L, 4606660653487956419L, 4732973459586031998L, 4607980687133228269L, 4730615653683472804L, 4607460288533169137L, 4728178726226774762L, 4602828760757345664L, 4723623689829460875L, 4609219612737699397L, 4707634693817774096L, 4604833755266159874L, -4504733722004434452L, 4607285895437920622L, 4711006438409611448L, 4600269111224624794L, 4725604199103262727L, 4607296735273851233L, -4519242511046345344L, 4607956640691004372L, 4724784851453132110L, 4604778283183548878L, 4720903221184049034L, 4604149135325891556L, -4503049881631775544L, 4608300526097327164L, 4729981725933614341L, 4609956059951502196L, 4727319849143433655L, 4599581031361288304L, 4722571735701998733L, 4599224875515435188L, 4720925562941935649L, 4599683375478339502L, 4722805796443070600L, 4606535725482771808L, -4514695776639200384L, 4606437102258528405L, 4730960964241934157L, 4600879258208544650L, 4726442238194609326L, 4609679844740512567L, -4501671765519631040L, 4608143646650936425L, 4733359513879160938L, 4605013987123473703L, 4726333514723039692L, 4608413763790802495L, 4733361640132610058L, 4608461986203047986L, 4721112730018532875L, 4608776002119152550L, 4731694404482249556L, 4609246992866244115L, 4730671053728865344L, 4610239184607595428L, 4720335724261255305L, 4605631416751429261L, 4721169865954571392L, 4601273365713351498L, -4501520976509373644L, 4602817025138268417L, 4728365822788539226L, 4604472270249759232L, 4727109185883326516L, 4607144790665329954L, -4503535265178385024L, 4608227944816795107L, 4731519622063068960L, 4606464539369156113L, 4733022561703652767L, 4609830483873607060L, -4502903932750920380L, 4599468004004769632L, 4723684314752202127L, 4601965035319221670L, 4728257112220268420L, 4610122733957498619L, 4719746985750433240L, 4607308310146896173L, 4731250514641552719L, 4604440025931586418L, 4729302041719396916L, 4609677051377143911L, 4731920085942715928L, 4607626748913243154L, 4730517451353474514L, 4608374884557728800L, 4731193430108555071L, 4608194869445159039L, 4731787120804095954L, 4605414631654760600L, 4731072462352812624L, 4604772260613308883L, 4728358905459499343L, 4606297403770762480L, 4733018515972442211L, 4602783315294010592L, 4731280561189996396L, 4604786983832073522L, 4728311448188060419L, 4607663026999677990L, 4727928208278769234L, 4607959930729468673L, 4723243546405209000L, 4606398057280475014L, 4732657238656687665L, 4610244444285759008L, -4506125094391530764L, 4601353077295424710L, -4498978934011369922L, 4602738570928995114L, 4718559629666180488L, 4609254185542989839L, 4716879922465959840L, 4600639039215843590L, -4513904835826063632L, 4604548570497018266L, 4729246734185553726L, 4606155646920532128L, 4730528011122954836L, 4606555611365859975L, 4731255209216468128L, 4602742880690582266L, 4723978359434913398L, 4610152365359538948L, 4719514777136714824L, 4602846843105184728L, 4729341793832848822L, 4608303546148216575L, 4733220319330647390L, 4602209356395177948L, 4731213470853199388L, 4605961489300655658L, 4718760534939154324L, 4609792751491106275L, 4731450133359016584L, 4608677117422220650L, -4503987170927105956L, 4604739134532561322L, 4732170188244918535L, 4610003188214056538L, 4723061155451587927L, 4608219209832244140L, -4499585375623586672L, 4609948852995114394L, 4729129062766154016L, 4604480706319042832L, 4726913675334565371L, 4608325301957507228L, 4730601579676577808L, 4602959991846003539L, 4724871610377609708L, 4609264722310736826L, 4725054808579078638L, 4606091987721176083L, -4500862136206012844L, 4607626708584922706L, -4513087901092443904L, 4610018103836188406L, 4725887598946582257L, 4608368731936953070L, 4726645922118383547L, 4610009083251966666L, -4504488075514831744L, 4603188743334543862L, 4725046877137907606L, 4609195460658320012L, 4733015340269448871L, 4610227260655526096L, -4499534902254203496L, 4608676736957289633L, 4725111394007863512L, 4607545429345569359L, 4733350459288768279L, 4602422736872108958L, 4727629621303931403L, 4605548085302049020L, 4732312541833392705L, 4605373630086491290L, -4505118920882909568L, 4608550527142981791L, 4728981522572686452L, 4610257635466933002L, 4731310236036590484L, 4606324030748404218L, 4732593491296345139L, 4603634040710351461L, 4719090672665159612L, 4604536278396461784L, 4730000625813775413L, 4602237478004235622L, 4732101104811422293L, 4603786018665526748L, 4726253069002262666L, 4603797444531116815L, 4731066718385827466L, 4606794617429415705L, -4498908375966858616L, 4601507041166688740L, 4726070751345723481L, 4605573628233057820L, 4732121509503057852L, 4609777862148758113L, 4723730460612016495L, 4608975597863785028L, 4724983051156687392L, 4608560562934609211L, 4719917313423965545L, 4607926307874886642L, -4501217200569658796L, 4610128606963002483L, 4729677675750632819L, 4609137432150856426L, 4733269348573382378L, 4603736959881266562L, -4510837995223289672L, 4604188515066984510L, 4730162329077234246L, 4607189513063094733L, 4713096246318681920L, 4603511902549898812L, 4726193562636006848L, 4600695614758035688L, 4717220657116797636L, 4607597404054238526L, 4728055830252724299L, 4608712533950428532L, 4719770597118990340L, 4606505306928845641L, 4730679732280267978L, 4604822136914441894L, 4730706079208757747L, 4600387426554950184L, 4721976239931540837L, 4605194722073041879L, -4505849209604294868L, 4603934830966719155L, 4732281196755478721L, 4609300399052406093L, 4731375939444296438L, 4610020637399372804L, -4526213785917656576L, 4609308538174585729L, 4717721144231211440L, 4600145249848895760L, 4727768236825239021L, 4607402276385927250L, -4511957282049586736L, 4603279989748246372L, 4724051800605447090L, 4607815246972414700L, 4728829162364012632L, 4608581362092147412L, 4732692707951776750L, 4600030450295925930L, 4733138300679286839L, 4606495698525836354L, -4512395972315475032L, 4607329372546794907L, 4725123242834250674L, 4599521955255645718L, 4732317542925737497L, 4606184186442759988L, 4732178673400785596L, 4600761541223886466L, 4728815756559092734L, 4599833058452024432L, 4730430177412715648L, 4603698916044932959L, 4715951866128111128L, 4610086993230829200L, 4728922866919730602L, 4608195803583761179L, 4733265619824706375L, 4604866648819809920L, 4728743907098405630L, 4602746463619760548L, 4725827559658826666L, 4608109122484484554L, 4726773846669685247L, 4608308638845118300L, 4730695107575493669L, 4607232175940556110L, 4730288618445194286L, 4600722167490923286L, 4724596410418385146L, 4608513670601043138L, 4724057049707816790L, 4601154134395228006L, 4720112169702853779L, 4609957627411663248L, 4725679455881355372L, 4609292630100830495L, 4726617834540588965L, 4604403979997016702L, -4499660442032198584L, 4605105109995741919L, -4521481025306403520L, 4605622049445047144L, 4732661814161717741L, 4606611918930252689L, -4499215229748794596L, 4602697038096393208L, 4727739658022920528L, 4601636205670005356L, 4732132530654283279L, 4608815801388241138L, 4730038643227281612L, 4605783193912198558L, 4729827487200771928L, 4608122740648359346L, 4715754872240011548L, 4603716867960308178L, 4713007848666730504L, 4609525331187465780L, 4728435976887777136L, 4605903781220976996L, 4733119696853107186L, 4603312321067553896L, 4727472070622192450L, 4601721729635578378L, 4726006677870283493L, 4607507973263335233L, -4500015000853894724L, 4603023782156196084L, -4504500444241945936L, 4609578295753146804L, -4510725696070299848L, 4605977113624514988L, -4505365534835600740L, 4606392541557673677L, 4732721357144876112L, 4600919534539578202L, -4502781988301011424L, 4606508029900057145L, 4728956576023688814L, 4608804821512049795L, 4728638184260424662L, 4604029640659227594L, 4731310511031278932L, 4607241076054684611L, -4507775246696511160L, 4604917614243438032L, 4726852714854559936L, 4604323120973606296L, 4731913861123280494L, 4603247400242916189L, 4718968810892415904L, 4604211673013127332L, 4729788159569699618L, 4600576662773805880L, 4730088612082977553L, 4602042491720221974L, 4724305044970889093L, 4604505614042010560L, -4508129100141207408L, 4607908945529081942L, 4732062571966997184L, 4606151026149019802L, 4718385553292068904L, 4603932116971953034L, 4729846280522332842L, 4608809317467378495L, 4733133766987840720L, 4602182983900198536L, 4726877815047931191L, 4600391147513341014L, 4729042785899997514L, 4609380255845567792L, 4730522241163805992L, 4607363939934598384L, -4508632997365958912L, 4607320063032845019L, 4730447897311285272L, 4605464241786897113L, -4511070546454621888L, 4609224339236059840L, 4732831229734624742L, 4605498477994302814L, -4509612100167432400L, 4609802570760419414L, 4730378689503264523L, 4609292625229059548L, 4729624657480899007L, 4609316197199882564L, 4728928479446576659L, 4609156233742885863L, 4722545362394286705L, 4608473790598505756L, 4720817649852737861L, 4607658681699765272L, 4723447257986732010L, 4609860355537368036L, 4724818087640736582L, 4610280660912966682L, 4722535165264635658L, 4603798971125534682L, 4722061789420385254L, 4599414465716916550L, 4729105714989922713L, 4605056927882724937L, 4717727762084323384L, 4601018269829540118L, 4724458804365694503L, 4610022305203486540L, 4713293570458937616L, 4604285837815971092L, 4719279193823181616L, 4609783671555678148L, 4721019179095053336L, 4609804194640325014L, -4500048064718673864L, 4608719872620685242L, 4711071604955398760L, 4608451229247777812L, 4720350801221128995L, 4607870804545779277L, 4730070885060896860L, 4603344389003383065L, 4728706179099465735L, 4600231885079622824L, 4729524781621334256L, 4601658654850316648L, -4507264078440627584L, 4608679221904704151L, 4724761487559039538L, 4605210887517345383L, -4506524982577998836L, 4610256417144613612L, -4500247311528503644L, 4604880326422177184L, 4726065879682102505L, 4609094456852027840L, 4721008334770552682L, 4606822395815565995L, 4730331115561085780L, 4609443008151711044L, 4726059041979394046L, 4605687212140948674L, 4731504767106962692L, 4608151203699622268L, 4720204347155983472L, 4602581595991374792L, -4502237589241960420L, 4604198465914317492L, -4510081028287913712L, 4607578371650076011L, 4729549416073044114L, 4608865227021024582L, 4725530518203441931L, 4606153441921978946L, 4718198913365893680L, 4609593023270365082L, -4503089228681324320L, 4607972149959420554L, 4728747244169180310L, 4606302516513840657L, 4727125630656533470L, 4606669403238129632L, -4509375659278278312L, 4601062422132217700L, -4507022118462108160L, 4609931641390898087L, 4718158419564856380L, 4605999396643613178L, 4723218911820667685L, 4604867722464654824L, 4717951488070093700L, 4608245022158598582L, 4718080500660946212L, 4601624658241633170L, 4720824710576715282L, 4606637619216126466L, 4713838022518283720L, 4610190867571435504L, 4710937888448775032L, 4603862440171624062L, 4731713055344146311L, 4608842437170962499L, 4729107888479491157L, 4606636891546128779L, -4502006970936445420L, 4601212277048016786L, 4728330964664509388L, 4602560673792755050L, 4728597650544424120L, 4601653907725532628L, 4722175949097028395L, 4603135975145660018L, 4729993202915793910L, 4610191255788839594L, 4731642644509215395L, 4608592835793887738L, 4731005951445492110L, 4607287504794517250L, 4724127869001606776L, 4609994586213338054L, 4731627709814960057L, 4604027019132932682L, 4721694918776998727L, 4606882645143891156L, 4732481712602875461L, 4609594709876416508L, 4727508073037740194L, 4609501520660742644L, 4731719573422070553L, 4610151750715361092L, 4704944204468982784L, 4608053773471810354L, 4730967682988084322L, 4608959143707196804L, 4724211395558266840L, 4606167801083689954L, 4729728667232304927L, 4608791680023806438L, -4499756248040098300L, 4607356870110110419L, -4500214186530755576L, 4609520655968993852L, 4733175922001456190L, 4607403415501573797L, -4514714731905035696L, 4605508571432380306L, -4525477653556631616L, 4602840542198917664L, 4730755246533255058L, 4605958840176145853L, 4712847547394489504L, 4603083878763682594L, 4729439934653302219L, 4607890221916309350L, 4732070932661402352L, 4608602048895824624L, 4729554305170755690L, 4608471665717999598L, 4733135521379323185L, 4604517345208252618L, 4730747439891391160L, 4603779188583696434L, 4728743141899460456L, 4609712715247531846L, -4503094382497429484L, 4610033493362159904L, 4731750564104922821L, 4607616096745842664L, 4731103212077489372L, 4608380926419787247L, 4732320432757385632L, 4607209588449022446L, 4732418187496417702L, 4608263500032746628L, 4732552108557425934L, 4609111711942628809L, 4729237649958829333L, 4606350359061957153L, 4732615318766551103L, 4604585632631207136L, 4732349824033941839L, 4601876456705327354L, 4730226308983812825L, 4607997591980397982L, 4709730237542537264L, 4605339441816386110L, 4710145569183634800L, 4603916347660932690L, 4732178454401739479L, 4608948110438337026L, 4729684382739025994L, 4606242249386435164L, -4502036118408335812L, 4607288927684235820L, -4517498696996148864L, 4601732292736305536L, 4732341232214576082L, 4608767422312085900L, 4730839386923454836L, 4610024765465438362L, -4503527019007302828L, 4609283897919394557L, -4515480607233439200L, 4606772296128702317L, -4507612868229154716L, 4603830974848750842L, 4728302421248675911L, 4608408378363528243L, 4722330638034336580L, 4607801621350717700L, -4507435757111813004L, 4609718234035987860L, 4727726722828086692L, 4600195475718762356L, -4501057853928801144L, 4603337572096473448L, 4731004798641581030L, 4607949989278924663L, 4731241870173632680L, 4599501499288513230L, 4718050342813576848L, 4603333337826376396L, 4731106675119787982L, 4601958018684193696L, -4501610606452588348L, 4602862980152560200L, 4731320690965038214L, 4602536309714917630L, 4733187462206289001L, 4606504381965899845L, 4728637746631688173L, 4608754248458647870L, 4732703543501685500L, 4607497173064198701L, 4731965495334900400L, 4602674718690002138L, 4725776111074854168L, 4601269300962360778L, 4730123436022103522L, 4605661463331865800L, -4505572865026961940L, 4608361660606564957L, 4725950008209228852L, 4604320789783623534L, 4727030381808591687L, 4606887495212769889L, -4509528000130536344L, 4605337542793894609L, -4502496755355355884L, 4610090143018699782L, 4727746315601388342L, 4608082126129364820L, 4730638152492885472L, 4604973937775512766L, 4732017220149473327L, 4602989723981703964L, 4730421786633558157L, 4609109939000587550L, 4733300103284425680L, 4603564876970351529L, 4728812063189204240L, 4601266562142310952L, -4499182797527578556L, 4607916277767270469L, 4716913676400101652L, 4606111827970291392L, 4731257961107320912L, 4607208828318245512L, 4727880953931493907L, 4603107079747850528L, 4732745690159459019L, 4609712803189825217L, -4511443831741754616L, 4605220925707197333L, 4729241648005217830L, 4601804936437142410L, 4727915175574836096L, 4602072200363513834L, 4730644554169725676L, 4608060319777801402L, 4732882235776263429L, 4605478982526731877L, 4722816623815314957L, 4609544710949629302L, 4732920934893596118L, 4606092532196554547L, 4724028849071239032L, 4603865847338626462L, 4708825992946258272L, 4607621302372384565L, 4729336867787794736L, 4606499744090402078L, 4732874603367260061L, 4605139823031467828L, -4499550480016365436L, 4609760150601203400L, 4729212413045673269L, 4608258203076591171L, 4732124663312309655L, 4606047306095249311L, -4506281672653123964L, 4609188467559680252L, 4718997816631055068L, 4606628421892471481L, 4732891053102473341L, 4606587050468367083L, -4502628761053110896L, 4609980787508329250L, 4712589293448368232L, 4606126906756603932L, 4725006094821954558L, 4607710626096247425L, 4728157064605215997L, 4602740933482086542L, 4728670033264631649L, 4609299676514683555L, 4729981295763994552L, 4603881465628043818L, 4709250246051214528L, 4608146589989720408L, 4723028391391092986L, 4606843754303088104L, 4733302838577135502L, 4607459160511549319L, 4729074500574665183L, 4600163053217403444L, 4730798482527873723L, 4609492172729862020L, 4727851736483892614L, 4604719707354913188L, 4730951083937898012L, 4605397093235334681L, -4500536696597074476L, 4605852777083720898L, 4730262615901665182L, 4605332658166666061L, -4510878438753499560L, 4607299030734606458L, 4725678129004527105L, 4606426907104675913L, -4500896901701608056L, 4609176586068144576L, 4721722533433137249L, 4609474442486492456L, 4729650856011331256L, 4608406785858710158L, 4724435073036946850L, 4602696805974747066L, -4499866542924797188L, 4609223383964343395L, 4730002524323807361L, 4609197251674021667L, 4720152122383089591L, 4608460539549106189L, 4726845405614250428L, 4605529190708627109L, 4729658607322497686L, 4609082794601986909L, 4729722083834882851L, 4606033033175931070L, -4502474330713536680L, 4608316785733070177L, 4730736413752933638L, 4600564862260094678L, 4728427154402960288L, 4608954243687816761L, 4730887574245863490L, 4609264086191794640L, 4722273989602002612L, 4609559525615218258L, 4721844551006393166L, 4609401457895645635L, 4730829490738068528L, 4600189479389305726L, -4501013998688634708L, 4607287372146928594L, 4731159064422504236L, 4604169704227821386L, 4730192480295429774L, 4603476709470132354L, -4502784046288287324L, 4606292744627589670L, -4501970765045979908L, 4605049317898882266L, 4728282614336805980L, 4607868151645711287L, 4728863377355543519L, 4609980365307773696L, 4731632686700212536L, 4601115116192655348L, 4731196044255645114L, 4602020783056400340L, 4731263412944705898L, 4610180030476477990L, 4724651692039921648L, 4608230094384714933L, 4727155242444756510L, 4607379553369156160L, 4727114864957890092L, 4605250945607703561L, 4732063219375449234L, 4606991389255839889L, 4725912869547683418L, 4604951477054501839L, 4726512216906480149L, 4609602658666373156L, 4714556850372815392L, 4608966546735894608L, 4713189538653726384L, 4609790152401445625L, 4730505054585654872L, 4607307906980407722L, 4730735251786550692L, 4609272006283243083L, 4728956366703321053L, 4604649812354506314L, 4726512940549936777L, 4606136425838585657L, 4697038556192686592L, 4607326355431353170L, 4706093811637217248L, 4599420929608544386L, -4510901049490972464L, 4608092948608544710L, 4731513848277035490L, 4603250251025936218L, 4729830121865431620L, 4609463751859133219L, 4730448363664925446L, 4609551814558107496L, 4731026820854129618L, 4602053968345478144L, 4729268798248926984L, 4600582066408903546L, 4731421597471455788L, 4606713304301817558L, 4725217444927762794L, 4608572880600668256L, 4730227999493538562L, 4609991371961188346L, 4731297311033318168L, 4609081459199204930L, 4718106570231918888L, 4602899085182929608L, -4505898996842389520L, 4603551278508546700L, 4721085487704573378L, 4604486088035752508L, 4729359648822448204L, 4608509915790647234L, 4720570184988980892L, 4606316007525321510L, 4728583647118518863L, 4605774660214997320L, -4504468842532266532L, 4601611064891417556L, 4733197133300163913L, 4600017057586420818L, 4732288958927593407L, 4603240588829684028L, 4724886671638940159L, 4609383606798733055L, 4728012595402847614L, 4602264920099940322L, 4731640886048485356L, 4609151200163655509L, -4507399526349922444L, 4599684524288030464L, 4730613193248690768L, 4605533828756260891L, 4732584241953835739L, 4607634219577987132L, 4732283112710937632L, 4608155559189119776L, 4733266300421121752L, 4604521581297956845L, 4728017062583325384L, 4608457195771317668L, -4504769035370900888L, 4607515856079790728L, 4733071006858851631L, 4607631233379880034L, 4731003071594931506L, 4609200719606942678L, -4507717123801171396L, 4600644265084065624L, -4514468807673953408L, 4609291960406798336L, 4723177119096330206L, 4609488151358631444L, 4728664657991767387L, 4608356848965121422L, -4506016620232869352L, 4607901910493589090L, -4499211169170336200L, 4609129196907643080L, 4732588887422053840L, 4608641068347419477L, 4731041496649181700L, 4609150394101641408L, 4732349175863186638L, 4607624413494433337L, 4728931660510113093L, 4605388928520101546L, 4702265975372119872L, 4609396504059325288L, 4726497526200618793L, 4609272815253318064L, 4733247037053146084L, 4608552594138461675L, 4725975553485761946L, 4601557523133866252L, -4506281151808903532L, 4601877785875121376L, 4732999178092302806L, 4602942089801985916L, 4731925154284261774L, 4610023811485615769L, 4710792832114878840L, 4610064215027741713L, 4730172664344044700L, 4609386923453303912L, 4722289492104749789L, 4607243261606762898L, -4503183766200342384L, 4608759915192968910L, 4725432381364867030L, 4608284302152623788L, 4729220767885186890L, 4604091526301346352L, 4729301886053957087L, 4604917067902752212L, 4732332443832464066L, 4600723514580905718L, 4719932672349748888L, 4609363605439525191L, -4501439593813739768L, 4605103952485354040L, -4500732267284661884L, 4606907964429518318L, -4507091298716264100L, 4603734481049582840L, 4733310027207026731L, 4608324806180360088L, 4732003391113961196L, 4609089602313182730L, 4729116035526750505L, 4607235721877651450L, -4515149644721426752L, 4607613716319773447L, 4725134764117291406L, 4604143037609670056L, 4733107661987629630L, 4601405472703011350L, 4725828988782618055L, 4605998359333805965L, 4731712035277011383L, 4609433878184269982L, -4504519629297062904L, 4605617111633311765L, 4731689789529811414L, 4599285426880364652L, 4731958015270373802L, 4606612592862707115L, 4710386383069830112L, 4602907232912580821L, 4731606204672922674L, 4608598787415948622L, 4717421069672309944L, 4605274381956475804L, 4731413715818781635L, 4601744987247794648L, 4713976867188654176L, 4607522938473952345L, 4725933156678763208L, 4607207290601004192L, 4719348592614120592L, 4607632107398099460L, 4729819158124152150L, 4606704359331967995L, -4507805494631639168L, 4603429373451595733L, -4520093195514744416L, 4603169990898872408L, 4714476945181994096L, 4605883661344983216L, -4502398130189513416L, 4609236878232903599L, 4731994525088404330L, 4609162417413205108L, -4509873716916997624L, 4603044516925721918L, 4724251778630593974L, 4605456010754740883L, 4731344274487060715L, 4603037148399730714L, -4499441040675017644L, 4605188591383886205L, 4726423133163539727L, 4599937823920473128L, 4732328357675506157L, 4606626992891090087L, 4725426028319887534L, 4602874866821811654L, -4509432517449769384L, 4608242389575063058L, -4501652486372108212L, 4607720912128681711L, 4722527608809505175L, 4601189395471902590L, 4731860700039248876L, 4609794533895838396L, 4729420971067796624L, 4607769426654793432L, -4498873806842350700L, 4606311931552269116L, 4731146438522362260L, 4602714163047374234L, 4729057093407770903L, 4604937265357175368L, 4730808860836364309L, 4606743232267407899L, 4724719790232778430L, 4602642031574317792L, -4507456515577113620L, 4609607277236756066L, -4531215004864140032L, 4610197066529458297L, 4731820407186486218L, 4603790957126894520L, 4727988352853262135L, 4608334541330811028L, 4725740064635511619L, 4607363155833788594L, -4503556607159876160L, 4605382797214300974L, 4722776495114729231L, 4605619213711696616L, -4502033724484998316L, 4602446861185225058L, 4730924564315568068L, 4605190423247061582L, 4725684773085166243L, 4604249174401339400L, 4728457752567686064L, 4606480155181243346L, 4710964918194194456L, 4601330558074579708L, 4731932273136642530L, 4609882599125425699L, 4726522636065102691L, 4603663357591104526L, -4502382255765354320L, 4607749834865495417L, 4730875070642677032L, 4603050532237922688L, 4724103296396491253L, 4607501700414744774L, -4518828230216755648L, 4607580225695599831L, 4720477353946350966L, 4609943726802764617L, 4728987764780993612L, 4608908835548419231L, 4728596119388232530L, 4603952244657596635L, 4723105202437791715L, 4606105620160210308L, 4729925076642044771L, 4608287339300256782L, -4501462789743609604L, 4610098501375796352L, 4730787294414563903L, 4605434234338739362L, -4499187877475383420L, 4603589212285603266L, -4499803615112583584L, 4603789521633250822L, 4730585645184758610L, 4609460111029362280L, -4500173234987057644L, 4604901884908564242L, 4731890482902214762L, 4603997334516943510L, 4731314716453709789L, 4601031893837658276L, 4720153430277206609L, 4599754562142504438L, -4506593581012128548L, 4607710045014110467L, 4729426286126122264L, 4609320137286844614L, 4733146996230305020L, 4604342392952785876L, 4720188450143065163L, 4606554070510400805L, -4505043422902558648L, 4609805357815608124L, 4729311520938847753L, 4605929085102745610L, 4729245648095311427L, 4608198293329054823L, 4724290726241841634L, 4603552416115150772L, 4722210784436378783L, 4604719209565168996L, 4719438334172022632L, 4609977893500964906L, 4730476576454691730L, 4608015834545013231L, 4730636648993711414L, 4601566560306486046L, 4724449659736414512L, 4604418786445713588L, -4498842231289851020L, 4601280769475282690L, 4717501425064485904L, 4604473995158014816L, 4733033643397333548L, 4605911937161457476L, 4730200800716199037L, 4603961743236756287L, 4729695704994509972L, 4609511956017092579L, 4727402719173001620L, 4609913079939669809L, 4732951917792593913L, 4603425324356059304L, -4516955017345927296L, 4609376949292050960L, 4730358155869721980L, 4599341819844081040L, 4723036934324606560L, 4604022544000367224L, -4516349774920501280L, 4606273602222732864L, 4722246040006938800L, 4607313321835384335L, 4721618501841149400L, 4608002047896261200L, 4726807022276300196L, 4601143716777437254L, 4720500785358910353L, 4599430237766657144L, 4721602090305103911L, 4609641772396626254L, -4502599623335719592L, 4608300989432705541L, 4728245398232210806L, 4609899541675952728L, 4720151972961765121L, 4609179654121591276L, 4732893420278180572L, 4610061267814318414L, 4726157261257870799L, 4607014686391477447L, 4730125068153873056L, 4609309375359634744L, 4724763009125469738L, 4604059080809926950L, 4732702142057689834L, 4600371361473015860L, 4730936017072823005L, 4609588216710469276L, 4721224168666505647L, 4608375171395340916L, 4732282332418118760L, 4608569007135697542L, 4731778048864517666L, 4607588854817826990L, 4728946041406234916L, 4604388549859067472L, 4733230136998675186L, 4607511185788899211L, 4720936335113015778L, 4608809122278060562L, 4732931062267909918L, 4609473283232062562L, 4729074807485285647L, 4600100676744705596L, 4733126838201417857L, 4607563718241678374L, 4731546482504890014L, 4609498806077674208L, 4694660384947916672L, 4605794582823363898L, 4733226978464927988L, 4610272240467958186L, 4731767153623230926L, 4602885733359331396L, 4718700950325326364L, 4607953004626158098L, -4505832946406062708L, 4609991776434797657L, 4728475606019424774L, 4605777752396238429L, 4719959408572382632L, 4602996878669825076L, 4719641756549666360L, 4603469794234574917L, -4503363553840649500L, 4607362058931145406L, 4732394173520072503L, 4601520257916698508L, -4500529412032997956L, 4609301102186850892L, 4727545678017318046L, 4608466712779011445L, 4709165061310851200L, 4605731239467649128L, 4729522898045412840L, 4603859848799890338L, 4724409542192002836L, 4610091237695115124L, 4721558316381646530L, 4606959643936490931L, 4731164695050510060L, 4609637189386878028L, 4730091869468950050L, 4606156244331762843L, -4501540120907289976L, 4608313566281013106L, 4718243013371050952L, 4608102399224994368L, -4506452521322628092L, 4601167006011456218L, 4720723839763780483L, 4607664669642918234L, 4725446829585695499L, 4599674277951018244L, -4500583021765120124L, 4609977973747624510L, 4727454693581312284L, 4609861483485753992L, 4731411020796734088L, 4601878708155297132L, 4732389549159372351L, 4607316775639911668L, 4729947204807341763L, 4609282051314722619L, 4730536404247100892L, 4609058894294647793L, 4707892635773702512L, 4610144863441526480L, 4729657513416760404L, 4601020318086759598L, 4721102439529724028L, 4609907821375048978L, 4727152231837630188L, 4607521457597760187L, 4730647389282307368L, 4609029125160835989L, -4506519716061084876L, 4608538148505826603L, 4712946892654661976L, 4607288935455478089L, 4721439460806189615L, 4607842199816117948L, -4500607743790786332L, 4609264990045117798L, 4725052941322985275L, 4604934497395397952L, 4726812812306831492L, 4602948214836038425L, 4732557794173094394L, 4605966393635288805L, 4732580785940151486L, 4609264096519881596L, 4728947045019744438L, 4603418414531516031L, -4513016758438071072L, 4606745798025564174L, 4732861355121111309L, 4608558970267654306L, 4732114310119830498L, 4605692334344729071L, 4728483878782103033L, 4609458869159096136L, 4727181976366832902L, 4604542662706827751L, 4710051336663405744L, 4608500087837114414L, 4733113126156255701L, 4607570492837635746L, 4728538202254013990L, 4604694424198875640L, -4521666334176678144L, 4606888269881413384L, -4502818419170396764L, 4609458133404942110L, 4729643637287787349L, 4605687700710288356L, 4711967047005856504L, 4609519112540485986L, -4514587539764137504L, 4608916099887440097L, 4726023191366807120L, 4608669372789176004L, 4725538336524199980L, 4606569419211173796L, 4729945447558905082L, 4608112916890976139L, 4728710382214666255L, 4609995419882295854L, 4728735300816881418L, 4604552769384380242L, 4731479396766409928L, 4608399663207182577L, 4726619987459855344L, 4603552849834682698L, -4507524499677218644L, 4605719205285406849L, 4732892814033835522L, 4608943969498689936L, 4732344594060928548L, 4607928511166060566L, 4728810314221530733L, 4603729101491642335L, 4721358351267695958L, 4607944050516882297L, 4726497387259870513L, 4603369195295095166L, 4732899640572074509L, 4609478416443100030L, -4505818750654609504L, 4609809201183405658L, 4720608430124330171L, 4607743932019899201L, 4725214394559612436L, 4599853901856133146L, 4719813702860556380L, 4604597949641689528L, 4724969912645751749L, 4607741207175497735L, -4502371004139354756L, 4600428258107628752L, 4725426706125992584L, 4609246637167020669L, -4501351746750786820L, 4609627893866935444L, 4728953101526074242L, 4610098412320059602L, 4722899643892710100L, 4608720974058718460L, 4718774733907159912L, 4603058985228947408L, 4730317453846003363L, 4603525650426697559L, 4728504474758692716L, 4607535165215652978L, -4499079690857833064L, 4600811884651624404L, 4733093752388093367L, 4608587346371579443L, 4730520765665630128L, 4603704227723156667L, -4504369990018956292L, 4602566004925912222L, 4697706747941570112L, 4608904912143444624L, 4725465166127661480L, 4607510692253046627L, 4716416167795090368L, 4600242838622057298L, 4730303140011094545L, 4606722582562933898L, 4732511339621534605L, 4609137811870036400L, 4729556566633590098L, 4609897723879974624L, 4729822568217256261L, 4608412342550562288L, 4733323544397780260L, 4606679620481318986L, 4722889243184500228L, 4609201812711026778L, 4715120739063460632L, 4604757241910991174L, -4509441104057100808L, 4601653898671488056L, 4733345688892025166L, 4610035491145974721L, 4729909048833058738L, 4609275531439598132L, 4726751739406984751L, 4606912222151690457L, 4728841533598791088L, 4605481698373836724L, 4716686334568695704L, 4607111840298911367L, 4731199227092195503L, 4603196842630830206L, 4682075594211058688L, 4601802527808214084L, 4722012662987348996L, 4604705517083605498L, 4732413934405958054L, 4608049803171269955L, 4729383830214468497L, 4610043365874934270L, 4714260970898921176L, 4606867442026072168L, 4732961311618322305L, 4609651717572834134L, -4543739968434072576L, 4602714791997843151L, 4723908600173640341L, 4606702995232585143L, 4729254827628661205L, 4609629988685285723L, 4723135996194028553L, 4608004259124473819L, 4733214298858394007L, 4609145365154441714L, 4729677768004364977L, 4608973040784255186L, 4723551942015099837L, 4600934925386367616L, 4732090734153999216L, 4609451402606769798L, 4733235313814204860L, 4608296282471455788L, 4729194341823771120L, 4610166522251677208L, 4729147089265956535L, 4606854406820054778L, 4676928318836654080L, 4609034996566498398L, 4722607592027150757L, 4604601338245141862L, 4723929781422996731L, 4604755974998329167L, -4501437935189647416L, 4607677123156512624L, 4733283815731579178L, 4610301685536052436L, 4733061744274457529L, 4604853900857628922L, 4715593983943892704L, 4609279558873794487L, 4727464298284532545L, 4609804948276282388L, -4510041762435613536L, 4610237071078460072L, 4725771898350687263L, 4609669369073752516L, 4731573912948836690L, 4603262474248109190L, 4723850682931137230L, 4602095061171817818L, 4727288340649589840L, 4602125260863923646L, 4732452683184535969L, 4603230094640036318L, 4731069467157459016L, 4609772182281076320L, 4729955972495440390L, 4609148309039891257L, 4732059887805683568L, 4604633181243045962L, 4720383087748183658L, 4609086545445342044L, -4502500636735412424L, 4605450491791876477L, 4732205457131313667L, 4607630578401543031L, 4732116713251978184L, 4608803876047006431L, 4667740939697250304L, 4609210986902568738L, -4502281129601869548L, 4609248632024805444L, 4730072506194867436L, 4607970341174518952L, 4731439665221116670L, 4602128200181302924L, 4730327093496536680L, 4607850553132973670L, 4729014207868740902L, 4609841784528113130L, 4708339551595463312L, 4610023860747199014L, 4726231110964310879L, 4609037505116858926L, 4733040413008387166L, 4607217618014170498L, 4729931694956853494L, 4609297574441506384L, -4504786231221207344L, 4605485219379008212L, 4729357948300887827L, 4606305732049277307L, 4691449975702959616L, 4608900180764159791L, 4723736627154041949L, 4603493800170704262L, 4729819649013455095L, 4609491049452206925L, 4731624670039280106L, 4608989970787853963L, 4730354664413075837L, 4608306199882423901L, 4730184795399847460L, 4608179906079165072L, 4732012197744418502L, 4607964391553920730L, 4714677762422592792L, 4608152068770213973L, 4731500315725440277L, 4610281131434936589L, 4716467820511746148L, 4604732127313602849L, 4710390772721864896L, 4602061386112293024L, -4502658835710145604L, 4603743143538242737L, -4503396868752991392L, 4609538030895540676L, 4725495221678616259L, 4607743179181478722L, 4729371724665931899L, 4609478211750398245L, 4729200991721282565L, 4603283478848531812L, 4710174113412100240L, 4599389028823924826L, 4721636590854673663L, 4603647156317891362L, 4720257841672151656L, 4605426074195018964L, 4725738630874028030L, 4604653201110598799L, 4731326866709228198L, 4608791597295289703L, 4711843534313227224L, 4609468198187903492L, -4515155640910161280L, 4608190057867524381L, 4725284826422486410L, 4607882271296674620L, 4729836746507170985L, 4608130498148976479L, 4722822518601050539L, 4605775283430456757L, 4711184168743392440L, 4603387584446742930L, 4730548158028822936L, 4606396081907113670L, 4725006038012004244L, 4604904074000319274L, 4723973122072157926L, 4599831145449966766L, -4517381314796741248L, 4603430996093947403L, -4502604625775770624L, 4602776957500720514L, 4732476044648253338L, 4608935108426878575L, 4730555292018365666L, 4607780392842043331L, 4724279731173145744L, 4599689572103011666L, 4732193689128130545L, 4608800214603615707L, 4731237232762666272L, 4603920569994927490L, 4729642791752516749L, 4606506193110819632L, 4712417439628697192L, 4609804149093051318L, -4511338983896826336L, 4607031930667072798L, 4728010405067990242L, 4607187284454968461L, -4503033133514286888L, 4609689037804457969L, 4728106169625328179L, 4606799785836667823L, -4498906225225446924L, 4602997940374103133L, 4730488358371579560L, 4609323472188093541L, -4504806570118003112L, 4602982455169646456L, 4714059450966127976L, 4603771797722752829L, 4732790842745220676L, 4603369153622304546L, 4731490248041583684L, 4608864319414662138L, 4731992519533158614L, 4601044599155314150L, -4504001946382653688L, 4608331722409089349L, 4730559225080544399L, 4604131312892806976L, 4715894676098723164L, 4599492756211889168L, 4724279841206187433L, 4609935694279462198L, 4724998048602874406L, 4600401086026133730L, 4730513923676184070L, 4609586278193292033L, 4724817937965064003L, 4607514642458009917L, 4729746865716171904L, 4607538595729339091L, 4729445643120411568L, 4610090706558775754L, 4727204082873171033L, 4607133252771125402L, 4719985954378170029L, 4605223598232196814L, 4723620088823152580L, 4607820642904212681L, -4506833986390215248L, 4605483036614089847L, -4515969700784863824L, 4603007393249973240L, 4731039481723300920L, 4605430009827336004L, 4723209363585086746L, 4606029012908045117L, -4501815820892763964L, 4600721475404848706L, 4731235555111178229L, 4602689769600291313L, 4729719151703397162L, 4609402060161563288L, -4499673995161762704L, 4600554616755659120L, 4728917202778841270L, 4608860170498184040L, 4730882729558139800L, 4603592530391037351L, -4512077998804474792L, 4608903571972003614L, 4731625822858127636L, 4607958719029382990L, -4503814539933563928L, 4600981743893896490L, 4728286762243486591L, 4604432318932247100L};

}
