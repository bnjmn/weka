/*
 *    Statistics.java
 *    Copyright (C) 1999 Eibe Frank
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
 * Class implementing some distributions, tests, etc. Most of the
 * code is adapted from Gary Perlman's unixstat.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */

public class Statistics {

  // =================
  // Private variables
  // =================

  private static double logSqrtPi = Math.log(Math.sqrt(Math.PI));
  private static double rezSqrtPi = 1/Math.sqrt(Math.PI);
  private static double bigx = 20.0;

  // ===============
  // Public methods.
  // ===============

  /**
   * Computes standard error for observed values of a binomial
   * random variable.
   *
   * @param p the probability of success
   * @param n the size of the sample
   * @return the standard error
   */

  public static double binomialStandardError(double p, int n) {

    if (n == 0)
      return 0; 
    return Math.sqrt((p*(1-p))/(double) n);
  }

  /**
   * Returns chi-squared probability for given value and degrees
   * of freedom. Adapted from unixstat by Gary Perlman.
   *
   * @param x the value
   * @param df the number of degrees of freedom
   */

  public static double chiSquaredProbability(double x, int df) {

    double a, y = 0, s, e, c, z, val;
    boolean even;
   
    if (x <= 0 || df < 1)
      return (1);
    a = 0.5 * x;
    even = (((int)(2*(df/2))) == df);
    if (df > 1)
      y = Math.exp(-a); //((-a < -bigx) ? 0.0 : Math.exp (-a));
    s = (even ? y : (2.0 * normalProbability(-Math.sqrt (x))));
    if (df > 2){
      x = 0.5 * (df - 1.0);
      z = (even ? 1.0 : 0.5);
      if (a > bigx){
	e = (even ? 0.0 : logSqrtPi);
	c = Math.log (a);
	while (z <= x){
	  e = Math.log (z) + e;
	  val = c*z-a-e;
	  s += Math.exp (val); //((val < -bigx) ? 0.0 : Math.exp (val));
	  z += 1.0;
	}
	return (s);
      }else{
	e = (even ? 1.0 : (rezSqrtPi / Math.sqrt (a)));
	c = 0.0;
	while (z <= x){
	  e = e * (a / z);
	  c = c + e;
	  z += 1.0;
	}
	return (c * y + s);
      }
    }else{
      return (s);
    }
  }

  /**
   * Critical value for given probability of F-distribution.
   * Adapted from unixstat by Gary Perlman.
   *
   * @param p the probability
   * @param df1 the first number of degrees of freedom
   * @param df2 the second number of degrees of freedom
   * @return the critical value for the given probability
   */

  private static double FCriticalValue(double p, int df1, int df2) {

    double  fval;
    double  maxf = 99999.0;     /* maximum possible F ratio */
    double  minf = .000001;     /* minimum possible F ratio */
    
    if (p <= 0.0 || p >= 1.0)
      return (0.0);
    
    fval = 1.0 / p; /* the smaller the p, the larger the F */
    
    while (Math.abs (maxf - minf) > .000001) {
      if (FProbability(fval, df1, df2) < p) /* F too large */
	maxf = fval;
      else /* F too small */
	minf = fval;
      fval = (maxf + minf) * 0.5;
    }
    
    return (fval);
  }

  /**
   * Computes probability of F-ratio.
   * Adapted from unixstat by Gary Perlman.
   * Collected Algorithms of the CACM 
   * Algorithm 322
   * Egon Dorrer
   *
   * @param F the F-ratio
   * @param df1 the first number of degrees of freedom
   * @param df2 the second number of degrees of freedom
   * @return the probability of the F-ratio.
   */
  
  private static double FProbability(double F, int df1, int df2) {

    int     i, j;
    int     a, b;
    double  w, y, z, d, p;

    if ((Math.abs(F) < 10e-10) || df1 <= 0 || df2 <= 0)
      return (1.0);
    a = (df1%2 == 1) ? 1 : 2;
    b = (df2%2 == 1) ? 1 : 2;
    w = (F * df1) / df2;
    z = 1.0 / (1.0 + w);
    if (a == 1)
      if (b == 1) {
	p = Math.sqrt (w);
	y = 1/Math.PI; /* 1 / 3.14159 */
	d = y * z / p;
	p = 2.0 * y * Math.atan (p);
      } else {
	p = Math.sqrt (w * z);
	d = 0.5 * p * z / w;
      } else if (b == 1) {
	p = Math.sqrt (z);
	d = 0.5 * z * p;
	p = 1.0 - p;
      } else {
	d = z * z;
	p = w * z;
      }
    y = 2.0 * w / z;
    for (j = b + 2; j <= df2; j += 2) {
      d *= (1.0 + a / (j - 2.0)) * z;
      p = (a == 1 ? p + d * y / (j - 1.0) : (p + w) * z);
    }
    y = w * z;
    z = 2.0 / z;
    b = df2 - 2;
    for (i = a + 2; i <= df1; i += 2) {
      j = i + b;
      d *= y * j / (i - 2.0);
      p -= z * d / j;
    }
    
    // correction for approximation errors suggested in certification
    
    if (p < 0.0)
      p = 0.0;
    else if (p > 1.0)
      p = 1.0;
    return (1.0-p);
  }
  
  /**
   * Returns probability that the standardized normal variate Z (mean = 0, standard
   * deviation = 1) is less than z.
   *
   * Adapted from unixstat by Gary Perlman.
   * @param the z-value
   * @return the probability of the z value according to the normal pdf
   */

  public static double normalProbability(double z) {

    double  y, x, w;
    
    if (z == 0.0)
      x = 0.0;
    else{
      y = 0.5 * Math.abs (z);
      if (y >= 3.0)
	x = 1.0;
      else if (y < 1.0){
	w = y*y;
	x = ((((((((0.000124818987 * w
                   -0.001075204047) * w +0.005198775019) * w
                   -0.019198292004) * w +0.059054035642) * w
                   -0.151968751364) * w +0.319152932694) * w
                   -0.531923007300) * w +0.797884560593) * y * 2.0;
      }else{
	y -= 2.0;
	x = (((((((((((((-0.000045255659 * y
                         +0.000152529290) * y -0.000019538132) * y
                         -0.000676904986) * y +0.001390604284) * y
                         -0.000794620820) * y -0.002034254874) * y
                         +0.006549791214) * y -0.010557625006) * y
                         +0.011630447319) * y -0.009279453341) * y
                         +0.005353579108) * y -0.002141268741) * y
                         +0.000535310849) * y +0.999936657524;
      }
    }
  
    return (z > 0.0 ? ((x + 1.0) / 2.0) : ((1.0 - x) / 2.0));
  }

  /**
   * Computes absolute size of half of a student-t confidence interval 
   * for degrees of freedom, probability, and standard error.
   *
   * @param df the number of degrees of freedom
   * @param p the probability
   * @param se the standard error
   * @return absolute size of half of a student-t confidence interval
   */

  public static double studentTConfidenceInterval(int df, double p,
						  double se) {

    return Math.sqrt(FCriticalValue(p, 1, df))*se;
  }
}








