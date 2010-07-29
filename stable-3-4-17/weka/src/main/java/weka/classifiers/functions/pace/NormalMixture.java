/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or (at
 *    your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful, but
 *    WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.  */

/*
 *    NormalMixture.java
 *    Copyright (C) 2002 Yong Wang
 *
 */

package weka.classifiers.functions.pace;

import java.util.Random;
import weka.core.Statistics;

/**
 * Class for manipulating normal mixture distributions. <p>
 *
 * REFERENCES <p>
 * 
 * Wang, Y. (2000). "A new approach to fitting linear models in high
 * dimensional spaces." PhD Thesis. Department of Computer Science,
 * University of Waikato, New Zealand. <p>
 * 
 * Wang, Y. and Witten, I. H. (2002). "Modeling for optimal probability
 * prediction." Proceedings of ICML'2002. Sydney. <p>
 *
 * @author Yong Wang (yongwang@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $ */

public class  NormalMixture extends MixtureDistribution
{
  protected double separatingThreshold = 0.05;

  protected double trimingThreshold = 0.7;

  protected double fittingIntervalLength = 3;

  /** Contructs an empty NormalMixture
   */
  public NormalMixture() {}

  /** Gets the separating threshold value. This value is used by the method
   separatable */
  public double getSeparatingThreshold(){
    return separatingThreshold;
  }

  /** Sets the separating threshold value 
   *  @param t the threshold value 
   */
  public void setSeparatingThreshold( double t ){
    separatingThreshold = t;
  }

  /** Gets the triming thresholding value. This value is usef by the
      method trim.  */
  public double getTrimingThreshold(){ 
    return trimingThreshold; 
  }

  /** Sets the triming thresholding value. */
  public void setTrimingThreshold( double t ){
    trimingThreshold = t;
  }

  /** Return true if a value can be considered for mixture estimatino
   *  separately from the data indexed between i0 and i1 
   *  @param data the data supposedly generated from the mixture 
   *  @param i0 the index of the first element in the group
   *  @param i1 the index of the last element in the group
   *  @param x the value
   */
  public boolean separable( DoubleVector data, int i0, int i1, double x )
  {
    double p = 0;
    for( int i = i0; i <= i1; i++ ) {
      p += Maths.pnorm( - Math.abs(x - data.get(i)) );
    }
    if( p < separatingThreshold ) return true;
    else return false;
  }

  /** Contructs the set of support points for mixture estimation.
   *  @param data the data supposedly generated from the mixture 
   *  @param ne the number of extra data that are suppposedly discarded
   *  earlier and not passed into here */
  public DoubleVector  supportPoints( DoubleVector data, int ne ) 
  {
    if( data.size() < 2 )
      throw new IllegalArgumentException("data size < 2");
	
    return data.copy();
  }
    
  /** Contructs the set of fitting intervals for mixture estimation.
   *  @param data the data supposedly generated from the mixture 
   */
  public PaceMatrix  fittingIntervals( DoubleVector data )
  {
    DoubleVector left = data.cat( data.minus( fittingIntervalLength ) );
    DoubleVector right = data.plus( fittingIntervalLength ).cat( data );
	
    PaceMatrix a = new PaceMatrix(left.size(), 2);
	
    a.setMatrix(0, left.size()-1, 0, left);
    a.setMatrix(0, right.size()-1, 1, right);
	
    return a;
  }
    
  /** Contructs the probability matrix for mixture estimation, given a set
   *  of support points and a set of intervals.
   *  @param s  the set of support points
   *  @param intervals the intervals */
  public PaceMatrix  probabilityMatrix( DoubleVector s, 
					PaceMatrix intervals ) 
  {
    int ns = s.size();
    int nr = intervals.getRowDimension();
    PaceMatrix p = new PaceMatrix(nr, ns);
	
    for( int i = 0; i < nr; i++ ) {
      for( int j = 0; j < ns; j++ ) {
	p.set( i, j,
	       Maths.pnorm( intervals.get(i, 1), s.get(j), 1 ) - 
	       Maths.pnorm( intervals.get(i, 0), s.get(j), 1 ) );
      }
    }
	
    return p;
  }
    
  /** Returns the empirical Bayes estimate of a single value.
   * @param x the value
   */
  public double  empiricalBayesEstimate ( double x ) 
  { 
    if( Math.abs(x) > 10 ) return x; // pratical consideration; modify later
    DoubleVector d = 
    Maths.dnormLog( x, mixingDistribution.getPointValues(), 1 );
    
    d.minusEquals( d.max() );
    d = d.map("java.lang.Math", "exp");
    d.timesEquals( mixingDistribution.getFunctionValues() );
    return mixingDistribution.getPointValues().innerProduct( d ) / d.sum();
  }

  /** Returns the empirical Bayes estimate of a vector.
   * @param x the vector
   */
  public DoubleVector empiricalBayesEstimate( DoubleVector x ) 
  {
    DoubleVector pred = new DoubleVector( x.size() );
    for(int i = 0; i < x.size(); i++ ) 
      pred.set(i, empiricalBayesEstimate(x.get(i)) );
    trim( pred );
    return pred;
  }

  /** Returns the optimal nested model estimate of a vector.
   * @param x the vector */
  public DoubleVector  nestedEstimate( DoubleVector x ) 
  {
    
    DoubleVector chf = new DoubleVector( x.size() );
    for(int i = 0; i < x.size(); i++ ) chf.set( i, hf( x.get(i) ) );
    chf.cumulateInPlace();
    int index = chf.indexOfMax();
    DoubleVector copy = x.copy();
    if( index < x.size()-1 ) copy.set( index + 1, x.size()-1, 0 );
    trim( copy );
    return copy;
  }
  
  /** Returns the estimate of optimal subset selection.
   * @param x the vector */
  public DoubleVector  subsetEstimate( DoubleVector x ) {

    DoubleVector h = h( x );
    DoubleVector copy = x.copy();
    for( int i = 0; i < x.size(); i++ )
      if( h.get(i) <= 0 ) copy.set(i, 0);
    trim( copy );
    return copy;
  }
  
  /** Trims the small values of the estaimte
   * @param x the estimate vector */
  public void trim( DoubleVector x ) {
    for(int i = 0; i < x.size(); i++ ) {
      if( Math.abs(x.get(i)) <= trimingThreshold ) x.set(i, 0);
    }
  }
  
  /**
   *  Computes the value of h(x) / f(x) given the mixture. The
   *  implementation avoided overflow.
   *  @param x the value */
  public double hf( double x ) 
  {
    DoubleVector points = mixingDistribution.getPointValues();
    DoubleVector values = mixingDistribution.getFunctionValues(); 

    DoubleVector d = Maths.dnormLog( x, points, 1 );
    d.minusEquals( d.max() );

    d = (DoubleVector) d.map("java.lang.Math", "exp");
    d.timesEquals( values );  

    return ((DoubleVector) points.times(2*x).minusEquals(x*x))
    .innerProduct( d ) / d.sum();
  }
    
  /**
   *  Computes the value of h(x) given the mixture. 
   *  @param x the value */
  public double h( double x ) 
  {
    DoubleVector points = mixingDistribution.getPointValues();
    DoubleVector values = mixingDistribution.getFunctionValues(); 
    DoubleVector d = (DoubleVector) Maths.dnorm( x, points, 1 ).timesEquals( values );  
    return ((DoubleVector) points.times(2*x).minusEquals(x*x))
    .innerProduct( d );
  }
    
  /**
   *  Computes the value of h(x) given the mixture, where x is a vector.
   *  @param x the vector */
  public DoubleVector h( DoubleVector x ) 
  {
    DoubleVector h = new DoubleVector( x.size() );
    for( int i = 0; i < x.size(); i++ ) 
      h.set( i, h( x.get(i) ) );
    return h;
  }
    
  /**
   *  Computes the value of f(x) given the mixture.
   *  @param x the value */
  public double f( double x ) 
  {
    DoubleVector points = mixingDistribution.getPointValues();
    DoubleVector values = mixingDistribution.getFunctionValues(); 
    return Maths.dchisq( x, points ).timesEquals( values ).sum();
  }
    
  /**
   *  Computes the value of f(x) given the mixture, where x is a vector.
   *  @param x the vector */
  public DoubleVector f( DoubleVector x ) 
  {
    DoubleVector f = new DoubleVector( x.size() );
    for( int i = 0; i < x.size(); i++ ) 
      f.set( i, h( f.get(i) ) );
    return f;
  }
    
  /** Converts to a string
   */
  public String  toString() 
  {
    return mixingDistribution.toString();
  }
    
  /** Method to test this class 
   */
  public static void  main(String args[]) 
  {
    int n1 = 50;
    int n2 = 50;
    double mu1 = 0;
    double mu2 = 5; 
    DoubleVector a = Maths.rnorm( n1, mu1, 1, new Random() );
    a = a.cat( Maths.rnorm( n2, mu2, 1, new Random() ) );
    DoubleVector means = (new DoubleVector( n1, mu1 )).cat(new DoubleVector(n2, mu2));

    System.out.println("==========================================================");
    System.out.println("This is to test the estimation of the mixing\n" +
	    "distribution of the mixture of unit variance normal\n" + 
	    "distributions. The example mixture used is of the form: \n\n" + 
	    "   0.5 * N(mu1, 1) + 0.5 * N(mu2, 1)\n" );

    System.out.println("It also tests three estimators: the subset\n" +
	    "selector, the nested model selector, and the empirical Bayes\n" +
	    "estimator. Quadratic losses of the estimators are given, \n" +
	    "and are taken as the measure of their performance.");
    System.out.println("==========================================================");
    System.out.println( "mu1 = " + mu1 + " mu2 = " + mu2 +"\n" );

    System.out.println( a.size() + " observations are: \n\n" + a );

    System.out.println( "\nQuadratic loss of the raw data (i.e., the MLE) = " + 
	     a.sum2( means ) );
    System.out.println("==========================================================");

    // find the mixing distribution
    NormalMixture d = new NormalMixture();
    d.fit( a, NNMMethod ); 
    System.out.println( "The estimated mixing distribution is:\n" + d );
	
    DoubleVector pred = d.nestedEstimate( a.rev() ).rev();
    System.out.println( "\nThe Nested Estimate = \n" + pred );
    System.out.println( "Quadratic loss = " + pred.sum2( means ) );

    pred = d.subsetEstimate( a );
    System.out.println( "\nThe Subset Estimate = \n" + pred );
    System.out.println( "Quadratic loss = " + pred.sum2( means ) );

    pred = d.empiricalBayesEstimate( a );
    System.out.println( "\nThe Empirical Bayes Estimate = \n" + pred );
    System.out.println( "Quadratic loss = " + pred.sum2( means ) );
	
  }

}

