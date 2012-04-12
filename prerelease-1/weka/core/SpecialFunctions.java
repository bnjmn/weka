/*
 *    SpecialFunctions.java
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

import java.lang.Math;

/**
 * Class implementing some mathematical functions methods.
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */

public final class SpecialFunctions {

  // ==================
  // Private variables.
  // ==================

  private static double log2 = Math.log(2);
  private static double[] cofLogGamma = {76.18009172947146,-86.50532032941677,
					 24.01409824083091,-1.231739572450155,
					 0.1208650973866179e-2,-0.5395239384953e-5};

  // ===============
  // Public methods.
  // ===============

  /**
   * Returns natural logarithm of factorial using gamma function.
   * @param x the value
   * @return natural logarithm of factorial
   */

  public static double lnFactorial(double x){

    return lnGamma(x+1);
  }

  /**
   * Returns natural logarithm of gamma function. Converted to java
   * from Numerical Recipes in C.
   * @param x the value
   * @return natural logarithm of gamma function
   */

  public static double lnGamma(double x){
    
    double y,tmp,ser;
    int j;
    
    y=x;
    tmp=x+5.5;
    tmp -= (x+0.5)*Math.log(tmp);
    ser=1.000000000190015;
    for (j=0;j<=5;j++) ser += cofLogGamma[j]/++y;
    return -tmp+Math.log(2.5066282746310005*ser/x);
  }

  /**
   * Returns base 2 logarithm of binomial coefficient using gamma function.
   * @param a upper part
   * @param b lower part
   * @return the base 2 logarithm of the binominal coefficient a over b
   */

  public static double log2Binomial(double a, double b) throws ArithmeticException{
    
    if (Utils.gr(b,a))
      throw new ArithmeticException("Can't compute binomial coefficient.");
    return (lnFactorial(a)-lnFactorial(b)-lnFactorial(a-b))/log2;
  }

  /**
   * Returns base 2 logarithm of multinomial using gamma function.
   * @param a upper part
   * @param bs lower part
   * @return multinomial coefficient of a over the bs
   */

  public static double log2Multinomial(double a, double[] bs)
       throws ArithmeticException{
    
    double sum = 0;
    int i;
    
    for (i=0;i<bs.length;i++)
      if (Utils.gr(bs[i],a))
	throw 
	  new ArithmeticException("Can't compute multinomial coefficient.");
      else
	sum = sum+lnFactorial(bs[i]);
    
    return (lnFactorial(a)-sum)/log2;
  }
}
