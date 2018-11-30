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
 * RandomSample.java
 * Copyright (C) 2018 University of Waikato, Hamilton, New Zealand
 */

package weka.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

/**
 * Class holding static utility methods for drawing random samples.
 *
 * @author eibe@cs.waikato.ac.nz
 * @version $Revision: 0$
 */
public class RandomSample {

  /**
   * Returns a sorted list of n distinct integers that are randomly chosen from 0, 1, ..., N - 1.
   *
   * It uses the drawSortedDenseSample() in this class if n > (int)(0.2 * N), which has complexity O(N).
   * Otherwise, it uses drawSortedSparseSample(), which has complexity O(n).
   *
   * @param n the number of samples to take
   * @param N one greater than the largest integer that can possibly be included in the sample
   * @param r the random number generator to use
   * @return
   * @throws IllegalArgumentException if a sample cannot be taken based on the given parameters
   */
  public static int[] drawSortedSample(int n, int N, Random r) throws IllegalArgumentException {

    if (n > (int)(0.2 * N)) {
      return drawSortedDenseSample(n, N, r);
    } else {
      return drawSortedSparseSample(n, N, r);
    }
  }

  /**
   * Draws a sorted list of n distinct integers from 0, 1, ..., N - 1 based on the simple Algorithm A in
   * <p>
   * J.S. Vitter (1987) "An Efficient Algorithm for Sequential Random Sampling". ACM Trans Math Software, 13(1).
   * <p>
   * This algorithm has time complexity O(N) but only requires O(n) random numbers to be generated. Space
   * complexity is O(n). Useful if 0 << n / N.
   */
  public static int[] drawSortedDenseSample(int n, int N, Random r) throws IllegalArgumentException {

    if ((n > N) || (n < 0) || (N < 0)) {
      throw new IllegalArgumentException("drawSortedDenseSample: cannot sample" + n + " points from " + N + " points.");
    }

    int[] vals = new int[n]; // Set aside space for storing selected indices
    double toBeSkipped = N - n; // Number of values available for skipping
    double toProcess = N; // Number of values that have not yet been processed
    while (n > 1) { // While more than two values remain to be selected
      double rv = 1.0 - r.nextDouble(); // Random value in (0, 1]
      double p = toBeSkipped / toProcess; // Probability of skipping current value
      while (rv < p) {
        toBeSkipped--; // Number of values available for skipping needs to be reduced by one
        toProcess--; // Number of values that have not yet been processed needs to be reduced by one
        p = (p * toBeSkipped) / toProcess; // Probability of skipping the next value as well
      }
      vals[vals.length - n] = N - (int) toProcess; // Add to list of selected values
      toProcess--; // Number of values that have not yet been processed needs to be reduced by one
      n--; // Number of values that still have to be selected is reduced by one
    }
    if (vals.length > 0) {
      vals[vals.length - 1] = N - (int) toProcess + (int) (toProcess * r.nextDouble()); // Select last value
    }

    return vals;
  }

  /**
   * Draws a sorted list of n distinct integers from 0, 1, ..., N - 1 based on drawSparseSample() followed
   * by radix sort. The time complexity of this method is O(n). Useful if n << N.
   */
  public static int[] drawSortedSparseSample(int n, int N, Random r) throws IllegalArgumentException {

    if ((n > N) || (n < 0) || (N < 0)) {
      throw new IllegalArgumentException("drawSortedSparseSample: cannot sample" + n + " points from " + N + " points.");
    }

    final int[] unsorted = drawSparseSample(n, N, r);
    return radixSortOfPositiveIntegers(unsorted);
  }

  /**
   * Sorts the given array of non-negative integers in ascending order using LSD radix sort. The result will
   * be undefined if negative integers are included in the input.
   *
   * @param a the array to be sorted
   * @return the array with the result;
   */
  public static int[] radixSortOfPositiveIntegers(int[] a) {

    final int n = a.length;
    int[] aa = new int[n];
    final int[] counts = new int[257];
    final byte shiftRight = 24;

    for (byte s = 0; s < 32; s += 8) { // We can assume positive integers
      final byte shiftLeft = (byte) (24 - s);

      Arrays.fill(counts, 0);
      for (int i = 0; i < n; i++) {
        //int c = Byte.toUnsignedInt((byte)(a[i] >>> shift));
        counts[((a[i] << shiftLeft) >>> shiftRight) + 1]++;
      }
      for (int i = 0; i < 255; i++) {
        counts[i + 1] += counts[i];
      }
      for (int i = 0; i < n; i++) {
        //int c = Byte.toUnsignedInt((byte)(a[i] >>> shift));
        aa[counts[(a[i] << shiftLeft) >>> shiftRight]++] = a[i];
      }
      int[] temp = a;
      a = aa;
      aa = temp;
    }
    return a;
  }

  /**
   * Draws n distinct integers from 0, 1, ..., N - 1, randomly ordered, using a partial Fisher-Yates shuffle
   * and a hash map. The idea of using a hash map is from
   * <p>
   * D.N. Bui (2015) "CacheDiff: Fast Random Sampling" https://arxiv.org/abs/1512.00501
   * <p>
   * This algorithm has time and space complexity O(n). Useful if n << N.
   */
  public static int[] drawSparseSample(int n, int N, Random r) throws IllegalArgumentException {

    if ((n > N) || (n < 0) || (N < 0)) {
      throw new IllegalArgumentException("drawSparseSample: cannot sample" + n + " points from " + N + " points.");
    }

    final int[] vals = new int[n]; // This will hold the n selected indices
    final HashMap<Integer, Integer> map = new HashMap<>(2 * n);
    int selected = 0;

    // Do partial Fisher-Yates shuffle and use HashMap to keep track of what has been moved
    for (int i = N; i > N - n; i--) {
      final Integer index_rand = r.nextInt(i);
      final Integer iObj = i - 1;
      final Integer stored_at_index_from_end = map.remove(iObj);
      if (index_rand.equals(iObj)) { // Last element selected? (Making sure we use correct comparison!)
        vals[selected++] = (stored_at_index_from_end != null) ? stored_at_index_from_end : iObj;
      } else {
        final Integer stored_at_index_rand = map.put(index_rand,
                (stored_at_index_from_end != null) ? stored_at_index_from_end : iObj);
        vals[selected++] = (stored_at_index_rand != null) ? stored_at_index_rand : index_rand;
      }
    }
    return vals;
  }
}