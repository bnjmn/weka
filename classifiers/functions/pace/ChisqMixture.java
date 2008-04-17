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
 *    ChisqMixture.java
 *    Copyright (C) 2002 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.functions.pace;

import weka.core.RevisionUtils;
import weka.core.matrix.DoubleVector;
import weka.core.matrix.Maths;

import java.util.Random;

/**
 * Class for manipulating chi-square mixture distributions. <p/>
 *
 * For more information see: <p/>
 * 
 <!-- technical-plaintext-start -->
 * Wang, Y (2000). A new approach to fitting linear models in high dimensional spaces. Hamilton, New Zealand.<br/>
 * <br/>
 * Wang, Y., Witten, I. H.: Modeling for optimal probability prediction. In: Proceedings of the Nineteenth International Conference in Machine Learning, Sydney, Australia, 650-657, 2002.
 <!-- technical-plaintext-end -->
 * 
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;phdthesis{Wang2000,
 *    address = {Hamilton, New Zealand},
 *    author = {Wang, Y},
 *    school = {Department of Computer Science, University of Waikato},
 *    title = {A new approach to fitting linear models in high dimensional spaces},
 *    year = {2000}
 * }
 * 
 * &#64;inproceedings{Wang2002,
 *    address = {Sydney, Australia},
 *    author = {Wang, Y. and Witten, I. H.},
 *    booktitle = {Proceedings of the Nineteenth International Conference in Machine Learning},
 *    pages = {650-657},
 *    title = {Modeling for optimal probability prediction},
 *    year = {2002}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 * @author Yong Wang (yongwang@cs.waikato.ac.nz)
 * @version $Revision: 1.4.2.1 $
 */
public class ChisqMixture 
  extends MixtureDistribution {
  
  /** the separating threshold value */
  protected double separatingThreshold = 0.05; 

  /** the triming thresholding */
  protected double trimingThreshold = 0.5;

  protected double supportThreshold = 0.5;

  protected int maxNumSupportPoints = 200; // for computational reason

  protected int fittingIntervalLength = 3;
    
  protected double fittingIntervalThreshold = 0.5;

  /** Contructs an empty ChisqMixture
   */
  public ChisqMixture() {}

  /** 
   * Gets the separating threshold value. This value is used by the method
   * separatable
   * 
   * @return the separating threshold
   */
  public double getSeparatingThreshold() {
    return separatingThreshold;
  }
  
  /**
   * Sets the separating threshold value 
   * 
   * @param t the threshold value 
   */
  public void setSeparatingThreshold( double t ) {
    separatingThreshold = t;
  }

  /** 
   * Gets the triming thresholding value. This value is usef by the method trim.
   * 
   * @return the triming threshold
   */
  public double getTrimingThreshold() {
    return trimingThreshold;
  }

  /** 
   * Sets the triming thresholding value.
   * 
   * @param t the triming threshold
   */
  public void setTrimingThreshold( double t ){
    trimingThreshold = t;
  }

  /** 
   *  Return true if a value can be considered for mixture estimation
   *  separately from the data indexed between i0 and i1 
   *  
   *  @param data the data supposedly generated from the mixture 
   *  @param i0 the index of the first element in the group
   *  @param i1 the index of the last element in the group
   *  @param x the value
   *  @return true if the value can be considered
   */
  public boolean separable( DoubleVector data, int i0, int i1, double x ) {

    DoubleVector dataSqrt = data.sqrt();
    double xh = Math.sqrt( x );

    NormalMixture m = new NormalMixture();
    m.setSeparatingThreshold( separatingThreshold );
    return m.separable( dataSqrt, i0, i1, xh );
  }

  /** 
   *  Contructs the set of support points for mixture estimation.
   *  
   *  @param data the data supposedly generated from the mixture 
   *  @param ne the number of extra data that are suppposedly discarded
   *  earlier and not passed into here
   *  @return the set of support points
   */
  public DoubleVector  supportPoints( DoubleVector data, int ne ) {

    DoubleVector sp = new DoubleVector();
    sp.setCapacity( data.size() + 1 );

    if( data.get(0) < supportThreshold || ne != 0 ) 
      sp.addElement( 0 );
    for( int i = 0; i < data.size(); i++ ) 
      if( data.get( i ) > supportThreshold ) 
	sp.addElement( data.get(i) );
	
    // The following will be fixed later???
    if( sp.size() > maxNumSupportPoints ) 
      throw new IllegalArgumentException( "Too many support points. " );

    return sp;
  }
    
  /** 
   *  Contructs the set of fitting intervals for mixture estimation.
   *  
   *  @param data the data supposedly generated from the mixture 
   *  @return the set of fitting intervals
   */
  public PaceMatrix  fittingIntervals( DoubleVector data ) {

    PaceMatrix a = new PaceMatrix( data.size() * 2, 2 );
    DoubleVector v = data.sqrt();
    int count = 0;
    double left, right;
    for( int i = 0; i < data.size(); i++ ) {
      left = v.get(i) - fittingIntervalLength; 
      if( left < fittingIntervalThreshold ) left = 0;
      left = left * left;
      right = data.get(i);
      if( right < fittingIntervalThreshold ) 
	right = fittingIntervalThreshold;
      a.set( count, 0, left );
      a.set( count, 1, right );
      count++;
    }
    for( int i = 0; i < data.size(); i++ ) {
      left = data.get(i);
      if( left < fittingIntervalThreshold ) left = 0;
      right = v.get(i) + fittingIntervalThreshold;
      right = right * right;
      a.set( count, 0, left );
      a.set( count, 1, right );
      count++;
    }
    a.setRowDimension( count );
	
    return a;
  }
    
  /** 
   *  Contructs the probability matrix for mixture estimation, given a set
   *  of support points and a set of intervals.
   *  
   *  @param s  the set of support points
   *  @param intervals the intervals
   *  @return the probability matrix
   */
  public PaceMatrix  probabilityMatrix(DoubleVector s, PaceMatrix intervals) {
    
    int ns = s.size();
    int nr = intervals.getRowDimension();
    PaceMatrix p = new PaceMatrix(nr, ns);
	
    for( int i = 0; i < nr; i++ ) {
      for( int j = 0; j < ns; j++ ) {
	p.set( i, j,
	       Maths.pchisq( intervals.get(i, 1), s.get(j) ) - 
	       Maths.pchisq( intervals.get(i, 0), s.get(j) ) );
      }
    }
	
    return p;
  }
    

  /** 
   *  Returns the pace6 estimate of a single value.
   *  
   *  @param x the value
   *  @return the pace6 estimate
   */
  public double  pace6 ( double x ) { 
    
    if( x > 100 ) return x; // pratical consideration. will modify later
    DoubleVector points = mixingDistribution.getPointValues();
    DoubleVector values = mixingDistribution.getFunctionValues(); 
    DoubleVector mean = points.sqrt();
	
    DoubleVector d = Maths.dchisqLog( x, points );
    d.minusEquals( d.max() );
    d = d.map("java.lang.Math", "exp").timesEquals( values );
    double atilde = mean.innerProduct( d ) / d.sum();
    return atilde * atilde;
  }

  /** 
   *  Returns the pace6 estimate of a vector.
   *  
   *  @param x the vector
   *  @return the pace6 estimate
   */
  public DoubleVector pace6( DoubleVector x ) {

    DoubleVector pred = new DoubleVector( x.size() );
    for(int i = 0; i < x.size(); i++ ) 
      pred.set(i, pace6(x.get(i)) );
    trim( pred );
    return pred;
  }

  /** 
   *  Returns the pace2 estimate of a vector.
   *  
   *  @param x the vector
   *  @return the pace2 estimate
   */
  public DoubleVector  pace2( DoubleVector x ) {
    
    DoubleVector chf = new DoubleVector( x.size() );
    for(int i = 0; i < x.size(); i++ ) chf.set( i, hf( x.get(i) ) );

    chf.cumulateInPlace();

    int index = chf.indexOfMax();

    DoubleVector copy = x.copy();
    if( index < x.size()-1 ) copy.set( index + 1, x.size()-1, 0 );
    trim( copy );
    return copy;
  }

  /** 
   *  Returns the pace4 estimate of a vector.
   *  
   *  @param x the vector
   *  @return the pace4 estimate
   */
  public DoubleVector  pace4( DoubleVector x ) {
    
    DoubleVector h = h( x );
    DoubleVector copy = x.copy();
    for( int i = 0; i < x.size(); i++ )
      if( h.get(i) <= 0 ) copy.set(i, 0);
    trim( copy );
    return copy;
  }

  /** 
   * Trims the small values of the estaimte
   * 
   * @param x the estimate vector
   */
  public void trim( DoubleVector x ) {
    
    for(int i = 0; i < x.size(); i++ ) {
      if( x.get(i) <= trimingThreshold ) x.set(i, 0);
    }
  }
    
  /**
   *  Computes the value of h(x) / f(x) given the mixture. The
   *  implementation avoided overflow.
   *  
   *  @param AHat the value
   *  @return the value of h(x) / f(x)
   */
  public double hf( double AHat ) {
    
    DoubleVector points = mixingDistribution.getPointValues();
    DoubleVector values = mixingDistribution.getFunctionValues(); 

    double x = Math.sqrt( AHat );
    DoubleVector mean = points.sqrt();
    DoubleVector d1 = Maths.dnormLog( x, mean, 1 );
    double d1max = d1.max();
    d1.minusEquals( d1max );
    DoubleVector d2 = Maths.dnormLog( -x, mean, 1 );
    d2.minusEquals( d1max );

    d1 = d1.map("java.lang.Math", "exp");
    d1.timesEquals( values );  
    d2 = d2.map("java.lang.Math", "exp");
    d2.timesEquals( values );  

    return ( ( points.minus(x/2)).innerProduct( d1 ) - 
	     ( points.plus(x/2)).innerProduct( d2 ) ) 
    / (d1.sum() + d2.sum());
  }
    
  /**
   *  Computes the value of h(x) given the mixture.
   *  
   *  @param AHat the value
   *  @return the value of h(x)
   */
  public double h( double AHat ) {
    
    if( AHat == 0.0 ) return 0.0;
    DoubleVector points = mixingDistribution.getPointValues();
    DoubleVector values = mixingDistribution.getFunctionValues();
	
    double aHat = Math.sqrt( AHat );
    DoubleVector aStar = points.sqrt();
    DoubleVector d1 = Maths.dnorm( aHat, aStar, 1 ).timesEquals( values );
    DoubleVector d2 = Maths.dnorm( -aHat, aStar, 1 ).timesEquals( values );

    return points.minus(aHat/2).innerProduct( d1 ) - 
           points.plus(aHat/2).innerProduct( d2 );
  }
    
  /**
   *  Computes the value of h(x) given the mixture, where x is a vector.
   *  
   *  @param AHat the vector
   *  @return the value of h(x)
   */
  public DoubleVector h( DoubleVector AHat ) {
    
    DoubleVector h = new DoubleVector( AHat.size() );
    for( int i = 0; i < AHat.size(); i++ ) 
      h.set( i, h( AHat.get(i) ) );
    return h;
  }
    
  /**
   *  Computes the value of f(x) given the mixture.
   *  
   *  @param x the value
   *  @return the value of f(x)
   */
  public double f( double x ) {
    
    DoubleVector points = mixingDistribution.getPointValues();
    DoubleVector values = mixingDistribution.getFunctionValues(); 

    return Maths.dchisq(x, points).timesEquals(values).sum();  
  }
    
  /**
   *  Computes the value of f(x) given the mixture, where x is a vector.
   *  
   *  @param x the vector
   *  @return the value of f(x)
   */
  public DoubleVector f( DoubleVector x ) {
    
    DoubleVector f = new DoubleVector( x.size() );
    for( int i = 0; i < x.size(); i++ ) 
      f.set( i, h( f.get(i) ) );
    return f;
  }
    
  /** 
   * Converts to a string
   * 
   * @return a string representation
   */
  public String  toString() {
    return mixingDistribution.toString();
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 1.4.2.1 $");
  }
    
  /** 
   * Method to test this class 
   * 
   * @param args the commandline arguments
   */
  public static void  main(String args[]) {
    
    int n1 = 50;
    int n2 = 50;
    double ncp1 = 0;
    double ncp2 = 10; 
    double mu1 = Math.sqrt( ncp1 );
    double mu2 = Math.sqrt( ncp2 );
    DoubleVector a = Maths.rnorm( n1, mu1, 1, new Random() );
    a = a.cat( Maths.rnorm(n2, mu2, 1, new Random()) );
    DoubleVector aNormal = a;
    a = a.square();
    a.sort();
	
    DoubleVector means = (new DoubleVector( n1, mu1 )).cat(new DoubleVector(n2, mu2));
	
    System.out.println("==========================================================");
    System.out.println("This is to test the estimation of the mixing\n" +
		       "distribution of the mixture of non-central Chi-square\n" + 
		       "distributions. The example mixture used is of the form: \n\n" + 
		       "   0.5 * Chi^2_1(ncp1) + 0.5 * Chi^2_1(ncp2)\n" );

    System.out.println("It also tests the PACE estimators. Quadratic losses of the\n" +
		       "estimators are given, measuring their performance.");
    System.out.println("==========================================================");
    System.out.println( "ncp1 = " + ncp1 + " ncp2 = " + ncp2 +"\n" );

    System.out.println( a.size() + " observations are: \n\n" + a );

    System.out.println( "\nQuadratic loss of the raw data (i.e., the MLE) = " + 
			aNormal.sum2( means ) );
    System.out.println("==========================================================");
	
    // find the mixing distribution
    ChisqMixture d = new ChisqMixture();
    d.fit( a, NNMMethod ); 
    System.out.println( "The estimated mixing distribution is\n" + d );  
	
    DoubleVector pred = d.pace2( a.rev() ).rev();
    System.out.println( "\nThe PACE2 Estimate = \n" + pred );
    System.out.println( "Quadratic loss = " + 
			pred.sqrt().times(aNormal.sign()).sum2( means ) );
    
    pred = d.pace4( a );
    System.out.println( "\nThe PACE4 Estimate = \n" + pred );
    System.out.println( "Quadratic loss = " + 
			pred.sqrt().times(aNormal.sign()).sum2( means ) );

    pred = d.pace6( a );
    System.out.println( "\nThe PACE6 Estimate = \n" + pred );
    System.out.println( "Quadratic loss = " + 
			pred.sqrt().times(aNormal.sign()).sum2( means ) );
  }
}
