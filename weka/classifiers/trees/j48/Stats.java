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
 *    Stats.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package weka.classifiers.j48;

import weka.core.*;

/**
 * Class implementing a statistical routine needed by J48.
 * This code is adapted from Ross Quinlan's implementation of C4.5.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.5 $
 */
public class Stats {

  /** Some constants for the interpolation. */
  private static final double Val[] = 
  {  0, 0.000000001, 0.00000001, 0.0000001, 0.000001, 0.00001, 0.00005, 0.0001, 
     0.0005, 0.001, 0.005, 0.01, 0.05, 0.10, 0.20, 0.40, 1.00};
  private static final double Dev[] = 
  {100,         6.0,       5.61,       5.2,     4.75,    4.26,    3.89,   3.72,   
       3.29,  3.09,  2.58, 2.33, 1.65, 1.28, 0.84, 0.25, 0.00};

  /**
   * Computes estimated extra error for given total number of instances
   * and errors.
   *
   * @param N number of instances
   * @param e observed error
   * @param CF confidence value
   */
  public static double addErrs(double N, double e, float CF){
    
    double Val0, Pr, Coeff = 0;
    int i;
    
    i = 0;
    while (CF > Val[i]) {
      i++;
    }
    Coeff = Dev[i-1] +
      (Dev[i] - Dev[i-1]) * (CF-Val[i-1]) / (Val[i] - Val[i-1]);
    Coeff = Coeff * Coeff;
    if (e == 0) {
      return N * (1 - Math.exp(Math.log(CF) / N));
    } else {
      if (e < 0.9999) {
        Val0 = N * (1 - Math.exp(Math.log(CF) / N));
        return Val0 + e * (addErrs(N, 1.0, CF) - Val0);
      } else {
	if (e + 0.5 >= N) {
	  return 0.67 * (N - e);
        } else {
	  Pr = (e + 0.5 + Coeff / 
		2 + Math.sqrt(Coeff * ((e + 0.5) * (1 - (e + 0.5) / N) + Coeff / 4))) / 
	    (N + Coeff);
	  return (N * Pr - e);
	}
      }
    }
  }
}








