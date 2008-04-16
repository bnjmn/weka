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
 *    MixtureDistribution.java
 *    Copyright (C) 2002 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.functions.pace;

import weka.core.RevisionHandler;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.matrix.DoubleVector;
import weka.core.matrix.IntVector;

/**
 * Abtract class for manipulating mixture distributions. <p>
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
 * @version $Revision: 1.5 $ */

public abstract class MixtureDistribution
  implements TechnicalInformationHandler, RevisionHandler {
  
  protected DiscreteFunction mixingDistribution;

  /** The nonnegative-measure-based method */
  public static final int NNMMethod = 1; 
    
  /** The probability-measure-based method */
  public static final int PMMethod = 2;

  // The CDF-based method
  // public static final int CDFMethod = 3;
    
  // The method based on the Kolmogrov and von Mises measure
  // public static final int ModifiedCDFMethod = 4; 

  /**
   * Returns an instance of a TechnicalInformation object, containing 
   * detailed information about the technical background of this class,
   * e.g., paper reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation 	result;
    TechnicalInformation 	additional;
    
    result = new TechnicalInformation(Type.PHDTHESIS);
    result.setValue(Field.AUTHOR, "Wang, Y");
    result.setValue(Field.YEAR, "2000");
    result.setValue(Field.TITLE, "A new approach to fitting linear models in high dimensional spaces");
    result.setValue(Field.SCHOOL, "Department of Computer Science, University of Waikato");
    result.setValue(Field.ADDRESS, "Hamilton, New Zealand");

    additional = result.add(Type.INPROCEEDINGS);
    additional.setValue(Field.AUTHOR, "Wang, Y. and Witten, I. H.");
    additional.setValue(Field.YEAR, "2002");
    additional.setValue(Field.TITLE, "Modeling for optimal probability prediction");
    additional.setValue(Field.BOOKTITLE, "Proceedings of the Nineteenth International Conference in Machine Learning");
    additional.setValue(Field.YEAR, "2002");
    additional.setValue(Field.PAGES, "650-657");
    additional.setValue(Field.ADDRESS, "Sydney, Australia");
    
    return result;
  }
    
  /** 
   * Gets the mixing distribution
   * 
   * @return the mixing distribution
   */
  public DiscreteFunction getMixingDistribution() {
    return mixingDistribution;
  }

  /** Sets the mixing distribution
   *  @param d the mixing distribution
   */
  public void  setMixingDistribution( DiscreteFunction d ) {
    mixingDistribution = d;
  }

  /** Fits the mixture (or mixing) distribution to the data. The default
   *  method is the nonnegative-measure-based method.
   * @param data the data, supposedly generated from the mixture model */
  public void fit( DoubleVector data ) {
    fit( data, NNMMethod );
  }

  /** Fits the mixture (or mixing) distribution to the data.
   *  @param data the data supposedly generated from the mixture 
   *  @param method the method to be used. Refer to the static final
   *  variables of this class. */
  public void fit( DoubleVector data, int method ) {
    DoubleVector data2 = (DoubleVector) data.clone();
    if( data2.unsorted() ) data2.sort();

    int n = data2.size();
    int start = 0;
    DoubleVector subset;
    DiscreteFunction d = new DiscreteFunction();
    for( int i = 0; i < n-1; i++ ) {
      if( separable( data2, start, i, data2.get(i+1) ) &&
	  separable( data2, i+1, n-1, data2.get(i) ) ) {
	subset = (DoubleVector) data2.subvector( start, i );
	d.plusEquals( fitForSingleCluster( subset, method ).
		      timesEquals(i - start + 1) );
	start = i + 1;
      }
    }
    subset = (DoubleVector) data2.subvector( start, n-1 );
    d.plusEquals( fitForSingleCluster( subset, method ).
		  timesEquals(n - start) ); 
    d.sort();
    d.normalize();
    mixingDistribution = d;
  }
    
  /** 
   *  Fits the mixture (or mixing) distribution to the data. The data is
   *  not pre-clustered for computational efficiency.
   *  
   *  @param data the data supposedly generated from the mixture 
   *  @param method the method to be used. Refer to the static final
   *  variables of this class.
   *  @return the generated distribution
   */
  public DiscreteFunction fitForSingleCluster( DoubleVector data, 
					       int method ) {
    
    if( data.size() < 2 ) return new DiscreteFunction( data );
    DoubleVector sp = supportPoints( data, 0 );
    PaceMatrix fi = fittingIntervals( data );
    PaceMatrix pm = probabilityMatrix( sp, fi );
    PaceMatrix epm = new 
      PaceMatrix( empiricalProbability( data, fi ).
		  timesEquals( 1. / data.size() ) );
    
    IntVector pvt = (IntVector) IntVector.seq(0, sp.size()-1);
    DoubleVector weights;
    
    switch( method ) {
    case NNMMethod: 
      weights = pm.nnls( epm, pvt );
      break;
    case PMMethod:
      weights = pm.nnlse1( epm, pvt );
      break;
    default: 
      throw new IllegalArgumentException("unknown method");
    }
    
    DoubleVector sp2 = new DoubleVector( pvt.size() );
    for( int i = 0; i < sp2.size(); i++ ){
      sp2.set( i, sp.get(pvt.get(i)) );
    }
    
    DiscreteFunction d = new DiscreteFunction( sp2, weights );
    d.sort();
    d.normalize();
    return d;
  }
    
  /** 
   *  Return true if a value can be considered for mixture estimatino
   *  separately from the data indexed between i0 and i1 
   *  
   *  @param data the data supposedly generated from the mixture 
   *  @param i0 the index of the first element in the group
   *  @param i1 the index of the last element in the group
   *  @param x the value
   *  @return true if a value can be considered
   */
  public abstract boolean separable( DoubleVector data, 
				     int i0, int i1, double x );
    
  /** 
   *  Contructs the set of support points for mixture estimation.
   *  
   *  @param data the data supposedly generated from the mixture 
   *  @param ne the number of extra data that are suppposedly discarded
   *  earlier and not passed into here
   *  @return the set of support points
   */
  public abstract DoubleVector  supportPoints( DoubleVector data, int ne );
    
  /** 
   *  Contructs the set of fitting intervals for mixture estimation.
   *  
   *  @param data the data supposedly generated from the mixture 
   *  @return the set of fitting intervals
   */
  public abstract PaceMatrix  fittingIntervals( DoubleVector data );
  
  /** 
   *  Contructs the probability matrix for mixture estimation, given a set
   *  of support points and a set of intervals.
   *  
   *  @param s  the set of support points
   *  @param intervals the intervals
   *  @return the probability matrix
   */
  public abstract PaceMatrix  probabilityMatrix( DoubleVector s, 
						 PaceMatrix intervals );
    
  /** 
   *  Computes the empirical probabilities of the data over a set of
   *  intervals.
   *  
   *  @param data the data
   *  @param intervals the intervals 
   *  @return the empirical probabilities
   */
  public PaceMatrix  empiricalProbability( DoubleVector data, 
					   PaceMatrix intervals )
  {
    int n = data.size();
    int k = intervals.getRowDimension();
    PaceMatrix epm = new PaceMatrix( k, 1, 0 );
    
    double point;
    for( int j = 0; j < n; j ++ ) {
      for(int i = 0; i < k; i++ ) {
	point = 0.0;
	if( intervals.get(i, 0) == data.get(j) || 
	    intervals.get(i, 1) == data.get(j) ) point = 0.5;
	else if( intervals.get(i, 0) < data.get(j) && 
		 intervals.get(i, 1) > data.get(j) ) point = 1.0;
	epm.setPlus( i, 0, point);
      }
    }
    return epm;
  }
  
  /** 
   * Converts to a string
   * 
   * @return a string representation
   */
  public String  toString() 
  {
    return "The mixing distribution:\n" + mixingDistribution.toString();
  }
    
}

