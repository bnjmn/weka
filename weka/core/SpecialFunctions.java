/*
 *    SpecialFunctions.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package weka.core;

import java.lang.Math;

/**
 * Class implementing some mathematical functions.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public final class SpecialFunctions {

  /** Some constants */
  private static double log2 = Math.log(2);
  private static double[] cofLogGamma = {76.18009172947146,-86.50532032941677,
					 24.01409824083091,-1.231739572450155,
					 0.1208650973866179e-2,-0.5395239384953e-5};

  /**
   * Returns natural logarithm of factorial using gamma function.
   *
   * @param x the value
   * @return natural logarithm of factorial
   */
  public static double lnFactorial(double x){

    return lnGamma(x+1);
  }

  /**
   * Returns natural logarithm of gamma function. Converted to java
   * from Numerical Recipes in C.
   *
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
    for (j=0;j<=5;j++) {
      ser += cofLogGamma[j]/++y;
    }
    return -tmp+Math.log(2.5066282746310005*ser/x);
  }

  /**
   * Returns base 2 logarithm of binomial coefficient using gamma function.
   *
   * @param a upper part of binomial coefficient
   * @param b lower part
   * @return the base 2 logarithm of the binominal coefficient a over b
   */
  public static double log2Binomial(double a, double b) throws ArithmeticException{
    
    if (Utils.gr(b,a)) {
      throw new ArithmeticException("Can't compute binomial coefficient.");
    }
    return (lnFactorial(a)-lnFactorial(b)-lnFactorial(a-b))/log2;
  }

  /**
   * Returns base 2 logarithm of multinomial using gamma function.
   *
   * @param a upper part of multinomial coefficient
   * @param bs lower part
   * @return multinomial coefficient of a over the bs
   */
  public static double log2Multinomial(double a, double[] bs)
       throws ArithmeticException{
    
    double sum = 0;
    int i;
    
    for (i=0;i<bs.length;i++) {
      if (Utils.gr(bs[i],a)) {
	throw 
	  new ArithmeticException("Can't compute multinomial coefficient.");
      } else {
	sum = sum+lnFactorial(bs[i]);
      }
    }
    return (lnFactorial(a)-sum)/log2;
  }

  /**
   * Main method for testing this class.
   */
  public static void main(String[] ops) {

    double[] doubles = {1, 2, 3};

    System.out.println("6!: " + Math.exp(SpecialFunctions.lnFactorial(6)));
    System.out.println("lnGamma(6): "+ SpecialFunctions.lnGamma(6));
    System.out.println("Binomial 6 over 2: " +
		       Math.pow(2, SpecialFunctions.log2Binomial(6, 2)));
    System.out.println("Multinomial 6 over 1, 2, 3: " +
		       Math.pow(2, SpecialFunctions.log2Multinomial(6, doubles)));
  }    
}
