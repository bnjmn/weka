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
 *    Copyright (C) 2002 Yong Wang
 *
 */

package weka.classifiers.functions.pace;

import weka.core.matrix.DoubleVector;
import weka.core.matrix.FlexibleDecimalFormat;
import weka.core.matrix.IntVector;


/** Class for handling discrete functions. <p>
 * 
 * A discrete function here is one that takes non-zero values over a finite
 * set of points. <p>
 * 
 * @author Yong Wang (yongwang@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $ */

public class  DiscreteFunction {
    
  protected DoubleVector  points;
  protected DoubleVector  values;

  /** Constructs an empty discrete function */
  public DiscreteFunction() 
  {
    this(null, null);
  }
    
  /** Constructs a discrete function with the point values provides and the
   *  function values are all 1/n. 
   * @param p the point values
   */
  public DiscreteFunction( DoubleVector p ) 
  {
    this( p, null );
  }
    
  /** Constructs a discrete function with both the point values and
   *  function values provided.
   * @param p the point values
   * @param v the function values */
  public DiscreteFunction( DoubleVector p, DoubleVector v ) 
  {
    points = p;
    values = v;
    formalize();
  }
    
  private DiscreteFunction  formalize() 
  {
    if( points == null ) points = new DoubleVector();
    if( values == null ) values = new DoubleVector();
	
    if( points.isEmpty() ) {
      if( ! values.isEmpty() )
	throw new IllegalArgumentException("sizes not match");
    }
    else {
      int n = points.size();
      if( values.isEmpty() ) {
	values = new DoubleVector( n, 1./n );
      }
      else {
	if( values.size() != n )
	  throw new IllegalArgumentException("sizes not match");
      }
    }
    return this;
  }
    
  /** 
   * Normalizes the function values with L1-norm.
   */
  public DiscreteFunction  normalize() 
  {
    if ( ! values.isEmpty() ) {
      double s = values.sum();
      if( s != 0.0 && s != 1.0 ) values.timesEquals( 1. / s ); 
    }
    return this;
  }
  
  /** 
   * Sorts the point values of the discrete function.
   */
  public void  sort() 
  {
    IntVector index = points.sortWithIndex();
    values = values.subvector( index );
  }
  
  /**
   * Clones the discrete function
   */
  public Object  clone() 
  {
    DiscreteFunction d = new DiscreteFunction();
    d.points = (DoubleVector) points.clone();
    d.values = (DoubleVector) values.clone();
    return d;
  }
  
  /**
   * Makes each individual point value unique 
   */
  public DiscreteFunction  unique() 
  {
    int count = 0;
    
    if( size() < 2 ) return this;
    for(int i = 1; i <= size() - 1; i++ ) {
      if( points.get( count ) != points.get( i ) ) {
	count++;
	points.set( count, points.get( i ) );
	values.set( count, values.get( i ) );
      } 
      else {
	values.set( count, values.get(count) + values.get(i) );
      } 
    }
    points = (DoubleVector) points.subvector(0, count);
    values = (DoubleVector) values.subvector(0, count);
    return this;
  }

  /** 
   * Returns the size of the point set.
   */
  public int  size() 
  {
    if( points == null ) return 0;
    return points.size();
  }
  
  /**
   * Gets a particular point value
   * @param i the index
   */
  public double  getPointValue( int i ) 
  {
    return points.get(i);
  }
  
  /**
   * Gets a particular function value
   * @param i the index
   */
  public double  getFunctionValue( int i ) 
  {
    return values.get(i);
  }
    
  /**
   * Sets a particular point value
   * @param i the index
   */
  public void  setPointValue( int i, double p )
  {
    points.set(i, p);
  }
    
  /**
   * Sets a particular function value
   * @param i the index
   */
  public void  setFunctionValue( int i, double v )
  {
    values.set(i, v);
  }
    
  /**
   * Gets all point values
   */
  protected DoubleVector  getPointValues() 
  {
    return points;
  }
    
  /**
   * Gets all function values
   */
  protected DoubleVector  getFunctionValues() 
  {
    return values;
  }
  
  /**
   * Returns true if it is empty.
   */
  public boolean  isEmpty() 
  {
    if( size() == 0 ) return true;
    return false;
  }
  
  //    public void  addPoint( double x, double y ) {
  //	  points.addPoint( x );
  //	  values.addPoint( y );
  //    }
  
  /** 
   * Returns the combined of two discrete functions
   * @param d the second discrete function
   * @return the combined discrte function
   */
  public DiscreteFunction  plus( DiscreteFunction d ) 
  {
    return ((DiscreteFunction) clone()).plusEquals( d );
  }
  
  /** 
   * Returns the combined of two discrete functions. The first function is
   * replaced with the new one.
   * @param d the second discrete function
   * @return the combined discrte function */
  public DiscreteFunction  plusEquals( DiscreteFunction d ) 
  {
    points = points.cat( d.points );
    values = values.cat( d.values );
    return this;
  }
  
  /**
   * All function values are multiplied by a double
   * @param x the multiplier
   */
  public DiscreteFunction  timesEquals( double x ) 
  {
    values.timesEquals( x );
    return this;
  }

  /**
   * Converts the discrete function to string.
   */
  public String  toString() 
  {
    StringBuffer text = new StringBuffer();
    FlexibleDecimalFormat nf1 = new FlexibleDecimalFormat( 5 );
    nf1.grouping( true ); 
    FlexibleDecimalFormat nf2 = new FlexibleDecimalFormat( 5 );
    nf2.grouping( true );
    for(int i = 0; i < size(); i++) {
      nf1.update( points.get(i) );
      nf2.update( values.get(i) );
    }

    text.append("\t" + nf1.formatString("Points") + 
		"\t" + nf2.formatString("Values") + "\n\n");
    for(int i = 0; i <= size() - 1; i++) {
      text.append( "\t" + nf1.format( points.get(i) ) + "\t" + 
		   nf2.format( values.get(i) ) + "\n" );
    }
	
    return text.toString();
  }

  public static void main( String args[] )
  {
	
    double points[] = {2,1,2,3,3};
    double values[] = {3,2,4,1,3};
    DiscreteFunction d = new DiscreteFunction( new DoubleVector( points ), 
					       new DoubleVector( values ));
    System.out.println( d );
    d.normalize();
    System.out.println( "d (after normalize) = \n" + d );
    points[1] = 10;
    System.out.println( "d (after setting [1]) = \n" + d);
    d.sort();
    System.out.println( "d (after sorting) = \n" + d);
    d.unique();
    System.out.println( "d (after unique) = \n" + d );
  }
}

