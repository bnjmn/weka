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
 *    PaceMatrix.java
 *    Copyright (C) 2002 Yong Wang
 *
 */

package weka.classifiers.functions.pace;

import weka.core.matrix.DoubleVector;
import weka.core.matrix.FlexibleDecimalFormat;
import weka.core.matrix.IntVector;
import weka.core.matrix.Matrix;
import weka.core.matrix.Maths;

import java.util.Random;
import java.text.DecimalFormat;

/**
 * Class for matrix manipulation used for pace regression. <p>
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
 * @version $Revision: 1.2 $ */

public class PaceMatrix extends Matrix {
    
  /* ------------------------
     Constructors
     * ------------------------ */
  
  /** Construct an m-by-n PACE matrix of zeros. 
      @param m    Number of rows.
      @param n    Number of colums.
  */
  public PaceMatrix( int m, int n ) {
    super( m, n );
  }

  /** Construct an m-by-n constant PACE matrix.
      @param m    Number of rows.
      @param n    Number of colums.
      @param s    Fill the matrix with this scalar value.
  */
  public PaceMatrix( int m, int n, double s ) {
    super( m, n, s );
  }
    
  /** Construct a PACE matrix from a 2-D array.
      @param A    Two-dimensional array of doubles.
      @exception  IllegalArgumentException All rows must have the same length
  */
  public PaceMatrix( double[][] A ) {
    super( A );
  }

  /** Construct a PACE matrix quickly without checking arguments.
      @param A    Two-dimensional array of doubles.
      @param m    Number of rows.
      @param n    Number of colums.
  */
  public PaceMatrix( double[][] A, int m, int n ) {
    super( A, m, n );
  }
    
  /** Construct a PaceMatrix from a one-dimensional packed array
      @param vals One-dimensional array of doubles, packed by columns (ala Fortran).
      @param m    Number of rows.
      @exception  IllegalArgumentException Array length must be a multiple of m.
  */
  public PaceMatrix( double vals[], int m ) {
    super( vals, m );
  }
    
  /** Construct a PaceMatrix with a single column from a DoubleVector 
      @param v    DoubleVector
  */
  public PaceMatrix( DoubleVector v ) {
    this( v.size(), 1 );
    setMatrix( 0, v.size()-1, 0, v );
  }
    
  /** Construct a PaceMatrix from a Matrix 
      @param X    Matrix 
  */
  public PaceMatrix( Matrix X ) {
    super( X.getRowDimension(), X.getColumnDimension() );
    A = X.getArray();
  }
    
  /* ------------------------
     Public Methods
     * ------------------------ */

  /** Set the row dimenion of the matrix
   *  @param rowDimension the row dimension
   */
  public void setRowDimension( int rowDimension ) 
  {
    m = rowDimension;
  }

  /** Set the column dimenion of the matrix
   *  @param columnDimension the column dimension
   */
  public void setColumnDimension( int columnDimension ) 
  {
    n = columnDimension;
  }

  /** Clone the PaceMatrix object.
   */
  public Object clone () {
    PaceMatrix X = new PaceMatrix(m,n);
    double[][] C = X.getArray();
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
	C[i][j] = A[i][j];
      }
    }
    return (Object) X;
  }
    
  /** Add a value to an element and reset the element
   *  @param i    the row number of the element
   *  @param j    the column number of the element
   *  @param s    the double value to be added with
   */
  public void setPlus(int i, int j, double s) {
    A[i][j] += s;
  }

  /** Multiply a value with an element and reset the element
   *  @param i    the row number of the element
   *  @param j    the column number of the element
   *  @param s    the double value to be multiplied with
   */
  public void setTimes(int i, int j, double s) {
    A[i][j] *= s;
  }

  /** Set the submatrix A[i0:i1][j0:j1] with a same value 
   *  @param i0 the index of the first element of the column
   *  @param i1 the index of the last element of the column
   *  @param j0 the index of the first column
   *  @param j1 the index of the last column
   *  @param s the value to be set to
   */
  public void setMatrix( int i0, int i1, int j0, int j1, double s ) {
    try {
      for( int i = i0; i <= i1; i++ ) {
	for( int j = j0; j <= j1; j++ ) {
	  A[i][j] = s;
	}
      }
    } catch( ArrayIndexOutOfBoundsException e ) {
      throw new ArrayIndexOutOfBoundsException( "Index out of bounds" );
    }
  }
  
  /** Set the submatrix A[i0:i1][j] with the values stored in a
   *  DoubleVector
   *  @param i0 the index of the first element of the column
   *  @param i1 the index of the last element of the column
   *  @param j  the index of the column
   *  @param v the vector that stores the values*/
  public void setMatrix( int i0, int i1, int j, DoubleVector v ) {
    for( int i = i0; i <= i1; i++ ) {
      A[i][j] = v.get(i-i0);
    }
  }

  /** Set the whole matrix from a 1-D array 
   *  @param v    1-D array of doubles
   *  @param columnFirst   Whether to fill the column first or the row.
   * 	@exception  ArrayIndexOutOfBoundsException Submatrix indices
   */
  public void setMatrix ( double[] v, boolean columnFirst ) {
    try {
      if( v.length != m * n ) 
	throw new IllegalArgumentException("sizes not match.");
      int i, j, count = 0;
      if( columnFirst ) {
	for( i = 0; i < m; i++ ) {
	  for( j = 0; j < n; j++ ) {
	    A[i][j] = v[count];
	    count ++;
	  }
	}
      }
      else {
	for( j = 0; j < n; j++ ) {
	  for( i = 0; i < m; i++ ){
	    A[i][j] = v[count];
	    count ++;
	  }
	}
      }

    } catch( ArrayIndexOutOfBoundsException e ) {
      throw new ArrayIndexOutOfBoundsException( "Submatrix indices" );
    }
  }

  /** Returns the maximum absolute value of all elements 
      @return the maximum value
  */
  public double maxAbs () {
    double ma = Math.abs(A[0][0]);
    for (int j = 0; j < n; j++) {
      for (int i = 0; i < m; i++) {
	ma = Math.max(ma, Math.abs(A[i][j]));
      }
    }
    return ma;
  }

  /** Returns the maximum absolute value of some elements of a column,
      that is, the elements of A[i0:i1][j].
      @param i0 the index of the first element of the column
      @param i1 the index of the last element of the column
      @param j  the index of the column
      @return the maximum value */
  public double maxAbs ( int i0, int i1, int j ) {
    double m = Math.abs(A[i0][j]);
    for (int i = i0+1; i <= i1; i++) {
      m = Math.max(m, Math.abs(A[i][j]));
    }
    return m;
  }

  /** Returns the minimum absolute value of some elements of a column,
      that is, the elements of A[i0:i1][j].
      @param i0 the index of the first element of the column
      @param i1 the index of the last element of the column
      @param j  the index of the column
      @return the minimum value 
  */
  public double minAbs ( int i0, int i1, int column ) {
    double m = Math.abs(A[i0][column]);
    for (int i = i0+1; i <= i1; i++) {
      m = Math.min(m, Math.abs(A[i][column]));
    }
    return m;
  }
    
  /** Check if the matrix is empty
   *   @return true if the matrix is empty
   */
  public boolean  isEmpty(){
    if(m == 0 || n == 0) return true;
    if(A == null) return true;
    return false;
  }
    
  /** Return a DoubleVector that stores a column of the matrix 
   *  @param j the index of the column
   */
  public DoubleVector  getColumn( int j ) {
    DoubleVector v = new DoubleVector( m );
    double [] a = v.getArray();
    for(int i = 0; i < m; i++)
      a[i] = A[i][j];
    return v;
  }

  /** Return a DoubleVector that stores some elements of a column of the
   *  matrix 
   *  @param i0 the index of the first element of the column
   *  @param i1 the index of the last element of the column
   *  @param j  the index of the column
   *  @return the DoubleVector
   */
  public DoubleVector  getColumn( int i0, int i1, int j ) {
    DoubleVector v = new DoubleVector( i1-i0+1 );
    double [] a = v.getArray();
    int count = 0;
    for( int i = i0; i <= i1; i++ ) {
      a[count] = A[i][j];
      count++;
    }
    return v;
  }
  
  
  /** Multiplication between a row (or part of a row) of the first matrix
   *  and a column (or part or a column) of the second matrix.
   *  @param i the index of the row in the first matrix
   *  @param j0 the index of the first column in the first matrix
   *  @param j1 the index of the last column in the first matrix
   *  @param B the second matrix
   *  @param l the index of the column in the second matrix
   */
  public double  times( int i, int j0, int j1, PaceMatrix B, int l ) {
    double s = 0.0;
    for(int j = j0; j <= j1; j++ ) {
      s += A[i][j] * B.A[j][l];
    }
    return s;
  }
  
  /** Decimal format for converting a matrix into a string
   *  @return the default decimal format
   */
  protected DecimalFormat []  format() {
    return format(0, m-1, 0, n-1, 7, false );
  }
  
  /** Decimal format for converting a matrix into a string
   *  @param digits the number of digits
   */
  protected DecimalFormat []  format( int digits ) {
    return format(0, m-1, 0, n-1, digits, false);
  }

  /** Decimal format for converting a matrix into a string
   */
  protected DecimalFormat []  format( int digits, boolean trailing ) {
    return format(0, m-1, 0, n-1, digits, trailing);
  }
  
  /** Decimal format for converting a matrix into a string
   */
  protected DecimalFormat  format(int i0, int i1, int j, int digits, 
				  boolean trailing) {
    FlexibleDecimalFormat df = new FlexibleDecimalFormat(digits, trailing);
    df.grouping( true );
    for(int i = i0; i <= i1; i ++ )
      df.update( A[i][j] );
    return df;
  }
  
  /** Decimal format for converting a matrix into a string
   */
  protected DecimalFormat []  format(int i0, int i1, int j0, int j1, 
				     int digits, boolean trailing) {
    DecimalFormat [] f = new DecimalFormat[j1-j0+1];
    for( int j = j0; j <= j1; j++ ) {
      f[j] = format(i0, i1, j, digits, trailing);
    }
    return f;
  }
  
  /** Converts matrix to string
   */ 
  public String  toString() {
    return toString( 5, false );
  }
  
  /** Converts matrix to string
   * @param digits number of digits after decimal point
   * @param trailing true if trailing zeros are padded
   */ 
  public String  toString( int digits, boolean trailing ) {
    
    if( isEmpty() ) return "null matrix";
    
    StringBuffer text = new StringBuffer();
    DecimalFormat [] nf = format( digits, trailing );
    int numCols = 0;
    int count = 0;
    int width = 80;
    int lenNumber;
    
    int [] nCols = new int[n];
    int nk=0;
    for( int j = 0; j < n; j++ ) {
      lenNumber = nf[j].format( A[0][j]).length(); 
      if( count + 1 + lenNumber > width -1 ) {
	nCols[nk++]  = numCols;
	count = 0;
	numCols = 0;
      }
      count += 1 + lenNumber;
      ++numCols;
    }
    nCols[nk] = numCols;
    
    nk = 0;
    for( int k = 0; k < n; ) {
      for( int i = 0; i < m; i++ ) {
	for( int j = k; j < k + nCols[nk]; j++)
	  text.append( " " + nf[j].format( A[i][j]) );
	text.append("\n");
      }
      k += nCols[nk];
      ++nk;
      text.append("\n");
    }
    
    return text.toString();
  }
  
  /** Squared sum of a column or row in a matrix
   *
   @param j the index of the column or row
   @param i0 the index of the first element
   @param i1 the index of the last element
   @param col if true, sum over a column; otherwise, over a row */
  public double sum2( int j, int i0, int i1, boolean col ) {
    double s2 = 0;
    if( col ) {   // column 
      for( int i = i0; i <= i1; i++ ) 
	s2 += A[i][j] * A[i][j];
    }
    else {
      for( int i = i0; i <= i1; i++ ) 
	s2 += A[j][i] * A[j][i];
    }
    return s2;
  }
  
  /** Squared sum of columns or rows of a matrix
   *
   @param col if true, sum over columns; otherwise, over rows */
  public double[] sum2( boolean col ) {
    int l = col ? n : m;
    int p = col ? m : n;
    double [] s2 = new double[l];
    for( int i = 0; i < l; i++ ) 
      s2[i] = sum2( i, 0, p-1, col );
    return s2;
  }

  /** Constructs single Householder transformation for a column
   *
   @param j    the index of the column
   @param k    the index of the row
   @return     d and q 
  */
  public double [] h1( int j, int k ) {
    double dq[] = new double[2];
    double s2 = sum2(j, k, m-1, true);  
    dq[0] = A[k][j] >= 0 ? - Math.sqrt( s2 ) : Math.sqrt( s2 );
    A[k][j] -= dq[0];
    dq[1] = A[k][j] * dq[0];
    return dq;
  }
  
  /** Performs single Householder transformation on one column of a matrix
   *
   @param j    the index of the column 
   @param k    the index of the row
   @param q    q = - u'u/2; must be negative
   @param b    the matrix to be transformed
   @param l    the column of the matrix b
  */
  public void h2( int j, int k, double q, PaceMatrix b, int l ) {
    double s = 0, alpha;
    for( int i = k; i < m; i++ )
      s += A[i][j] * b.A[i][l];
    alpha = s / q;
    for( int i = k; i < m; i++ )
      b.A[i][l] += alpha * A[i][j];
  }
  
  /** Constructs the Givens rotation
   *  @param a 
   *  @param b
   *  @return a double array that stores the cosine and sine values
   */
  public double []  g1( double a, double b ) {
    double cs[] = new double[2];
    double r = Maths.hypot(a, b);
    if( r == 0.0 ) {
      cs[0] = 1;
      cs[1] = 0;
    }
    else {
      cs[0] = a / r;
      cs[1] = b / r;
    }
    return cs;
  }
  
  /* Performs the Givens rotation
   * @param cs a array storing the cosine and sine values
   * @param i0 the index of the row of the first element
   * @param i1 the index of the row of the second element
   * @param j the index of the column
   */
  public void  g2( double cs[], int i0, int i1, int j ){
    double w =   cs[0] * A[i0][j] + cs[1] * A[i1][j];
    A[i1][j] = - cs[1] * A[i0][j] + cs[0] * A[i1][j];
    A[i0][j] = w;
  }
  
  /** Forward ordering of columns in terms of response explanation.  On
   *  input, matrices A and b are already QR-transformed. The indices of
   *  transformed columns are stored in the pivoting vector.
   *  
   *@param b     the PaceMatrix b
   *@param pvt   the pivoting vector
   *@param k0    the first k0 columns (in pvt) of A are not to be changed
   **/
  public void forward( PaceMatrix b, IntVector pvt, int k0 ) {
    for( int j = k0; j < Math.min(pvt.size(), m); j++ ) {
      steplsqr( b, pvt, j, mostExplainingColumn(b, pvt, j), true );
    }
  }

  /** Returns the index of the column that has the largest (squared)
   *  response, when each of columns pvt[ks:] is moved to become the
   *  ks-th column. On input, A and b are both QR-transformed.
   *  
   *@param b    response
   *@param pvt  pivoting index of A
   *@param ks   columns pvt[ks:] of A are to be tested 
   **/
  public int  mostExplainingColumn( PaceMatrix b, IntVector pvt, int ks ) {
    double val;
    int [] p = pvt.getArray();
    double ma = columnResponseExplanation( b, pvt, ks, ks );
    int jma = ks;
    for( int i = ks+1; i < pvt.size(); i++ ) {
      val = columnResponseExplanation( b, pvt, i, ks );
      if( val > ma ) {
	ma = val;
	jma = i;
      }
    }
    return jma;
  }
  
  /** Backward ordering of columns in terms of response explanation.  On
   *  input, matrices A and b are already QR-transformed. The indices of
   *  transformed columns are stored in the pivoting vector.
   * 
   *  A and b must have the same number of rows, being the (pseudo-)rank. 
   *  
   *@param b     PaceMatrix b
   *@param pvt   pivoting vector
   *@param ks    number of QR-transformed columns; psuedo-rank of A 
   *@param k0    first k0 columns in pvt[] are not to be ordered.
   *@return      void
   */
  public void backward( PaceMatrix b, IntVector pvt, int ks, int k0 ) {
    for( int j = ks; j > k0; j-- ) {
      steplsqr( b, pvt, j, leastExplainingColumn(b, pvt, j, k0), false );
    }
  }

  /** Returns the index of the column that has the smallest (squared)
   *  response, when the column is moved to become the (ks-1)-th
   *  column. On input, A and b are both QR-transformed.
   *  
   *@param b    response
   *@param pvt  pivoting index of A
   *@param ks   psudo-rank of A
   *@param k0   A[][pvt[0:(k0-1)]] are excluded from the testing.  */
  public int  leastExplainingColumn( PaceMatrix b, IntVector pvt, int ks, 
				     int k0 ) {
    double val;
    int [] p = pvt.getArray();
    double mi = columnResponseExplanation( b, pvt, ks-1, ks );
    int jmi = ks-1;
    for( int i = k0; i < ks - 1; i++ ) {
      val = columnResponseExplanation( b, pvt, i, ks );
      if( val <= mi ) {
	mi = val;
	jmi = i;
      }
    }
    return jmi;
  }
  
  /** Returns the squared ks-th response value if the j-th column becomes
   *  the ks-th after orthogonal transformation.  A[][pvt[ks:j]] (or
   *  A[][pvt[j:ks]], if ks > j) and b[] are already QR-transformed
   *  on input and will remain unchanged on output.
   *
   *  More generally, it returns the inner product of the corresponding
   *  row vector of the response PaceMatrix. (To be implemented.)
   *
   *@param b    PaceMatrix b
   *@param pvt  pivoting vector
   *@param j    the column A[pvt[j]][] is to be moved
   *@param ks   the target column A[pvt[ks]][]
   *@return     the squared response value */
  public double  columnResponseExplanation( PaceMatrix b, IntVector pvt,
					    int j, int ks ) {
    /*  Implementation: 
     *
     *  If j == ks - 1, returns the squared ks-th response directly.
     *
     *  If j > ks -1, returns the ks-th response after
     *  Householder-transforming the j-th column and the response.
     *
     *  If j < ks - 1, returns the ks-th response after a sequence of
     *  Givens rotations starting from the j-th row. */

    int k, l;
    double [] xxx = new double[n];
    int [] p = pvt.getArray();
    double val;
    
    if( j == ks -1 ) val = b.A[j][0];
    else if( j > ks - 1 ) {
      int jm = Math.min(n-1, j);
      DoubleVector u = getColumn(ks,jm,p[j]);
      DoubleVector v = b.getColumn(ks,jm,0);
      val = v.innerProduct(u) / u.norm2();
    }
    else {                 // ks > j
      for( k = j+1; k < ks; k++ ) // make a copy of A[j][]
	xxx[k] = A[j][p[k]];
      val = b.A[j][0];
      double [] cs;
      for( k = j+1; k < ks; k++ ) {
	cs = g1( xxx[k], A[k][p[k]] );
	for( l = k+1; l < ks; l++ ) 
	  xxx[l] = - cs[1] * xxx[l] + cs[0] * A[k][p[l]];
	val = - cs[1] * val + cs[0] * b.A[k][0];
      }
    }
    return val * val;  // or inner product in later implementation???
  }

  /** QR transformation for a least squares problem
   *            A x = b
   *  
   *@param b    PaceMatrix b
   *@param pvt  pivoting vector
   *@param k0   the first k0 columns of A (indexed by pvt) are pre-chosen. 
   *            (But subject to rank examination.) 
   * 
   *            For example, the constant term may be reserved, in which
   *            case k0 = 1.
   *@return     void; implicitly both A and b are transformed. pvt.size() is 
   *            is the psuedo-rank of A.
   **/
  public void  lsqr( PaceMatrix b, IntVector pvt, int k0 ) {
    final double TINY = 1e-15;
    int [] p = pvt.getArray();
    int ks = 0;  // psuedo-rank
    for(int j = 0; j < k0; j++ )   // k0 pre-chosen columns
      if( sum2(p[j],ks,m-1,true) > TINY ){ // large diagonal element 
	steplsqr(b, pvt, ks, j, true);
	ks++;
      }
      else {                     // collinear column
	pvt.shiftToEnd( j );
	pvt.setSize(pvt.size()-1);
	k0--;
	j--;
      }
	
    // initial QR transformation
    for(int j = k0; j < Math.min( pvt.size(), m ); j++ ) {
      if( sum2(p[j], ks, m-1, true) > TINY ) { 
	steplsqr(b, pvt, ks, j, true);
	ks++;
      }
      else {                     // collinear column
	pvt.shiftToEnd( j );
	pvt.setSize(pvt.size()-1);
	j--;
      }
    }
	
    b.m = m = ks;           // reset number of rows
    pvt.setSize( ks );
  }
    
  /** QR transformation for a least squares problem
   *            A x = b
   *  
   *@param b    PaceMatrix b
   *@param pvt  pivoting vector
   *@param k0   the first k0 columns of A (indexed by pvt) are pre-chosen. 
   *            (But subject to rank examination.) 
   * 
   *            For example, the constant term may be reserved, in which
   *            case k0 = 1.
   *@return     void; implicitly both A and b are transformed. pvt.size() is 
   *            is the psuedo-rank of A.
   **/
  public void  lsqrSelection( PaceMatrix b, IntVector pvt, int k0 ) {
    int numObs = m;         // number of instances
    int numXs = pvt.size();

    lsqr( b, pvt, k0 );

    if( numXs > 200 || numXs > numObs ) { // too many columns.  
      forward(b, pvt, k0);
    }
    backward(b, pvt, pvt.size(), k0);
  }
    
  /** 
   * Sets all diagonal elements to be positive (or nonnegative) without
   * changing the least squares solution 
   * @param Y the response
   * @param pvt the pivoted column index
   */
  public void positiveDiagonal( PaceMatrix Y, IntVector pvt ) {
     
    int [] p = pvt.getArray();
    for( int i = 0; i < pvt.size(); i++ ) {
      if( A[i][p[i]] < 0.0 ) {
	for( int j = i; j < pvt.size(); j++ ) 
	  A[i][p[j]] = - A[i][p[j]];
	Y.A[i][0] = - Y.A[i][0];
      }
    }
  }

  /** Stepwise least squares QR-decomposition of the problem
   *	          A x = b
   @param b    PaceMatrix b
   @param pvt  pivoting vector
   @param ks   number of transformed columns
   @param j    pvt[j], the column to adjoin or delete
   @param adjoin   to adjoin if true; otherwise, to delete */
  public void  steplsqr( PaceMatrix b, IntVector pvt, int ks, int j, 
			 boolean adjoin ) {
    final int kp = pvt.size(); // number of columns under consideration
    int [] p = pvt.getArray();
	
    if( adjoin ) {     // adjoining 
      int pj = p[j];
      pvt.swap( ks, j );
      double dq[] = h1( pj, ks );
      int pk;
      for( int k = ks+1; k < kp; k++ ){
	pk = p[k];
	h2( pj, ks, dq[1], this, pk);
      }
      h2( pj, ks, dq[1], b, 0 ); // for matrix. ???
      A[ks][pj] = dq[0];
      for( int k = ks+1; k < m; k++ )
	A[k][pj] = 0;
    }
    else {          // removing 
      int pj = p[j];
      for( int i = j; i < ks-1; i++ ) 
	p[i] = p[i+1];
      p[ks-1] = pj;
      double [] cs;
      for( int i = j; i < ks-1; i++ ){
	cs = g1( A[i][p[i]], A[i+1][p[i]] );
	for( int l = i; l < kp; l++ ) 
	  g2( cs, i, i+1, p[l] );
	for( int l = 0; l < b.n; l++ )
	  b.g2( cs, i, i+1, l );
      }
    }
  }
    
  /** Solves upper-triangular equation
   *   	R x = b
   *  @param b the response
   *  @param pvt the pivoting vector
   *  @param kp the number of the first columns involved 
   *  @return  void. On output, the solution is stored in b 
   */
  public void  rsolve( PaceMatrix b, IntVector pvt, int kp) {
    if(kp == 0) b.m = 0;
    int i, j, k;
    int [] p = pvt.getArray();
    double s;
    double [][] ba = b.getArray();
    for( k = 0; k < b.n; k++ ) {
      ba[kp-1][k] /= A[kp-1][p[kp-1]];
      for( i = kp - 2; i >= 0; i-- ){
	s = 0;
	for( j = i + 1; j < kp; j++ )
	  s += A[i][p[j]] * ba[j][k];
	ba[i][k] -= s;
	ba[i][k] /= A[i][p[i]];
      }
    } 
    b.m = kp;
  }
    
  /** Returns a new matrix which binds two matrices together with rows. 
   *  @param b  the second matrix
   *  @return the combined matrix
   */
  public PaceMatrix  rbind( PaceMatrix b ){
    if( n != b.n ) 
      throw new IllegalArgumentException("unequal numbers of rows.");
    PaceMatrix c = new PaceMatrix( m + b.m, n );
    c.setMatrix( 0, m - 1, 0, n - 1, this );
    c.setMatrix( m, m + b.m - 1, 0, n - 1, b );
    return c;
  }

  /** Returns a new matrix which binds two matrices with columns.
   *  @param b the second matrix 
   *  @return the combined matrix
   */
  public PaceMatrix  cbind( PaceMatrix b ) {
    if( m != b.m ) 
      throw new IllegalArgumentException("unequal numbers of rows: " + 
					 m + " and " + b.m);
    PaceMatrix c = new PaceMatrix(m, n + b.n);
    c.setMatrix( 0, m - 1, 0, n - 1, this );
    c.setMatrix( 0, m - 1, n, n + b.n - 1, b );
    return c;
  }

  /** Solves the nonnegative linear squares problem. That is, <p>
   *   <center>   min || A x - b||, subject to x >= 0.  </center> <p>
   * 
   *  For algorithm, refer to P161, Chapter 23 of C. L. Lawson and
   *  R. J. Hanson (1974).  "Solving Least Squares
   *  Problems". Prentice-Hall.
   * 	@param b the response
   *  @param pvt vector storing pivoting column indices
   *	@return solution */
  public DoubleVector nnls( PaceMatrix b, IntVector pvt ) {
    int j, t, counter = 0, jm = -1, n = pvt.size();
    double ma, max, alpha, wj;
    int [] p = pvt.getArray();
    DoubleVector x = new DoubleVector( n );
    double [] xA = x.getArray();
    PaceMatrix z = new PaceMatrix(n, 1);
    PaceMatrix bt;
	
    // step 1 
    int kp = 0; // #variables in the positive set P
    while ( true ) {         // step 2 
      if( ++counter > 3*n )  // should never happen
	throw new RuntimeException("Does not converge");
      t = -1;
      max = 0.0;
      bt = new PaceMatrix( b.transpose() );
      for( j = kp; j <= n-1; j++ ) {   // W = A' (b - A x) 
	wj = bt.times( 0, kp, m-1, this, p[j] );
	if( wj > max ) {        // step 4
	  max = wj;
	  t = j;
	}
      }
	    
      // step 3 
      if ( t == -1) break; // optimum achieved 
	    
      // step 5 
      pvt.swap( kp, t );       // move variable from set Z to set P
      kp++;
      xA[kp-1] = 0;
      steplsqr( b, pvt, kp-1, kp-1, true );
      // step 6
      ma = 0;
      while ( ma < 1.5 ) {
	for( j = 0; j <= kp-1; j++ ) z.A[j][0] = b.A[j][0];
	rsolve(z, pvt, kp); 
	ma = 2; jm = -1;
	for( j = 0; j <= kp-1; j++ ) {  // step 7, 8 and 9
	  if( z.A[j][0] <= 0.0 ) { // alpha always between 0 and 1
	    alpha = xA[j] / ( xA[j] - z.A[j][0] ); 
	    if( alpha < ma ) {
	      ma = alpha; jm = j;
	    }
	  }
	}
	if( ma > 1.5 ) 
	  for( j = 0; j <= kp-1; j++ ) xA[j] = z.A[j][0];  // step 7 
	else { 
	  for( j = kp-1; j >= 0; j-- ) { // step 10
	    // Modified to avoid round-off error (which seemingly 
	    // can cause infinite loop).
	    if( j == jm ) { // step 11 
	      xA[j] = 0.0;
	      steplsqr( b, pvt, kp, j, false );
	      kp--;  // move variable from set P to set Z
	    }
	    else xA[j] += ma * ( z.A[j][0] - xA[j] );
	  }
	}
      }
    }
    x.setSize(kp);
    pvt.setSize(kp);
    return x;
  }

  /** Solves the nonnegative least squares problem with equality
   *	constraint. That is, <p>
   *  <center> min ||A x - b||, subject to x >= 0 and c x = d. </center> <p>
   *
   *	@param b the response
   *  @param c coeficients of equality constraints
   *  @param d constants of equality constraints
   *	@param pvt vector storing pivoting column indices */
  public DoubleVector nnlse( PaceMatrix b, PaceMatrix c, PaceMatrix d, 
			     IntVector pvt ) {
    double eps = 1e-10 * Math.max( c.maxAbs(), d.maxAbs() ) /
    Math.max( maxAbs(), b.maxAbs() );
	
    PaceMatrix e = c.rbind( new PaceMatrix( times(eps) ) );
    PaceMatrix f = d.rbind( new PaceMatrix( b.times(eps) ) );

    return e.nnls( f, pvt );
  }

  /** Solves the nonnegative least squares problem with equality
   *	constraint. That is, <p>
   *  <center> min ||A x - b||,  subject to x >= 0 and || x || = 1. </center>
   *  <p>
   *  @param b the response 
   *	@param pvt vector storing pivoting column indices */
  public DoubleVector nnlse1( PaceMatrix b, IntVector pvt ) {
    PaceMatrix c = new PaceMatrix( 1, n, 1 );
    PaceMatrix d = new PaceMatrix( 1, b.n, 1 );
	
    return nnlse(b, c, d, pvt);
  }

  /** Generate matrix with standard-normally distributed random elements
      @param m    Number of rows.
      @param n    Number of colums.
      @return An m-by-n matrix with random elements.  */
  public static Matrix randomNormal( int m, int n ) {
    Random random = new Random();
     
    Matrix A = new Matrix(m,n);
    double[][] X = A.getArray();
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
	X[i][j] = random.nextGaussian();
      }
    }
    return A;
  }

  public static void  main( String args[] ) {
    System.out.println("================================================" + 
		       "===========");
    System.out.println("To test the pace estimators of linear model\n" + 
		       "coefficients.\n");

    double sd = 2;     // standard deviation of the random error term
    int n = 200;       // total number of observations
    double beta0 = 100;   // intercept
    int k1 = 20;       // number of coefficients of the first cluster
    double beta1 = 0;  // coefficient value of the first cluster
    int k2 = 20;      // number of coefficients of the second cluster
    double beta2 = 5; // coefficient value of the second cluster 
    int k = 1 + k1 + k2;

    DoubleVector beta = new DoubleVector( 1 + k1 + k2 );
    beta.set( 0, beta0 );
    beta.set( 1, k1, beta1 );
    beta.set( k1+1, k1+k2, beta2 );

    System.out.println("The data set contains " + n + 
		       " observations plus " + (k1 + k2) + 
		       " variables.\n\nThe coefficients of the true model"
		       + " are:\n\n" + beta );
	
    System.out.println("\nThe standard deviation of the error term is " + 
		       sd );
	
    System.out.println("===============================================" 
		       + "============");
		
    PaceMatrix X = new PaceMatrix( n, k1+k2+1 );
    X.setMatrix( 0, n-1, 0, 0, 1 );
    X.setMatrix( 0, n-1, 1, k1+k2, random(n, k1+k2) );
	
    PaceMatrix Y = new 
      PaceMatrix( X.times( new PaceMatrix(beta) ).
		  plusEquals( randomNormal(n,1).times(sd) ) );

    IntVector pvt = (IntVector) IntVector.seq(0, k1+k2);

    /*System.out.println( "The OLS estimate (by jama.Matrix.solve()) is:\n\n" + 
      (new PaceMatrix(X.solve(Y))).getColumn(0) );*/
	
    X.lsqrSelection( Y, pvt, 1 );
    X.positiveDiagonal( Y, pvt );

    PaceMatrix sol = (PaceMatrix) Y.clone();
    X.rsolve( sol, pvt, pvt.size() );
    DoubleVector betaHat = sol.getColumn(0).unpivoting( pvt, k );
    System.out.println( "\nThe OLS estimate (through lsqr()) is: \n\n" + 
			betaHat );

    System.out.println( "\nQuadratic loss of the OLS estimate (||X b - X bHat||^2) = " + 
			( new PaceMatrix( X.times( new 
			  PaceMatrix(beta.minus(betaHat)) )))
			.getColumn(0).sum2() );

    System.out.println("=============================================" + 
		       "==============");
    System.out.println("             *** Pace estimation *** \n");
    DoubleVector r = Y.getColumn( pvt.size(), n-1, 0);
    double sde = Math.sqrt(r.sum2() / r.size());
	
    System.out.println( "Estimated standard deviation = " + sde );

    DoubleVector aHat = Y.getColumn( 0, pvt.size()-1, 0).times( 1./sde );
    System.out.println("\naHat = \n" + aHat );
	
    System.out.println("\n========= Based on chi-square mixture ============");

    ChisqMixture d2 = new ChisqMixture();
    int method = MixtureDistribution.NNMMethod;
    DoubleVector AHat = aHat.square();
    d2.fit( AHat, method ); 
    System.out.println( "\nEstimated mixing distribution is:\n" + d2 );
	
    DoubleVector ATilde = d2.pace2( AHat );
    DoubleVector aTilde = ATilde.sqrt().times(aHat.sign());
    PaceMatrix YTilde = new 
      PaceMatrix((new PaceMatrix(aTilde)).times( sde ));
    X.rsolve( YTilde, pvt, pvt.size() );
    DoubleVector betaTilde = 
    YTilde.getColumn(0).unpivoting( pvt, k );
    System.out.println( "\nThe pace2 estimate of coefficients = \n" + 
			betaTilde );
    System.out.println( "Quadratic loss = " + 
			( new PaceMatrix( X.times( new 
			  PaceMatrix(beta.minus(betaTilde)) )))
			.getColumn(0).sum2() );
	
    ATilde = d2.pace4( AHat );
    aTilde = ATilde.sqrt().times(aHat.sign());
    YTilde = new PaceMatrix((new PaceMatrix(aTilde)).times( sde ));
    X.rsolve( YTilde, pvt, pvt.size() );
    betaTilde = YTilde.getColumn(0).unpivoting( pvt, k );
    System.out.println( "\nThe pace4 estimate of coefficients = \n" + 
			betaTilde );
    System.out.println( "Quadratic loss = " + 
			( new PaceMatrix( X.times( new 
			  PaceMatrix(beta.minus(betaTilde)) )))
			.getColumn(0).sum2() );
	
    ATilde = d2.pace6( AHat );
    aTilde = ATilde.sqrt().times(aHat.sign());
    YTilde = new PaceMatrix((new PaceMatrix(aTilde)).times( sde ));
    X.rsolve( YTilde, pvt, pvt.size() );
    betaTilde = YTilde.getColumn(0).unpivoting( pvt, k );
    System.out.println( "\nThe pace6 estimate of coefficients = \n" + 
			betaTilde );
    System.out.println( "Quadratic loss = " + 
			( new PaceMatrix( X.times( new 
			  PaceMatrix(beta.minus(betaTilde)) )))
			.getColumn(0).sum2() );
	
    System.out.println("\n========= Based on normal mixture ============");
	
    NormalMixture d = new NormalMixture();
    d.fit( aHat, method ); 
    System.out.println( "\nEstimated mixing distribution is:\n" + d );
	
    aTilde = d.nestedEstimate( aHat );
    YTilde = new PaceMatrix((new PaceMatrix(aTilde)).times( sde ));
    X.rsolve( YTilde, pvt, pvt.size() );
    betaTilde = YTilde.getColumn(0).unpivoting( pvt, k );
    System.out.println( "The nested estimate of coefficients = \n" + 
			betaTilde );
    System.out.println( "Quadratic loss = " + 
			( new PaceMatrix( X.times( new 
			  PaceMatrix(beta.minus(betaTilde)) )))
			.getColumn(0).sum2() );
	
	
    aTilde = d.subsetEstimate( aHat );
    YTilde = new PaceMatrix((new PaceMatrix(aTilde)).times( sde ));
    X.rsolve( YTilde, pvt, pvt.size() );
    betaTilde = 
    YTilde.getColumn(0).unpivoting( pvt, k );
    System.out.println( "\nThe subset estimate of coefficients = \n" + 
			betaTilde );
    System.out.println( "Quadratic loss = " + 
			( new PaceMatrix( X.times( new 
			  PaceMatrix(beta.minus(betaTilde)) )))
			.getColumn(0).sum2() );
	
    aTilde = d.empiricalBayesEstimate( aHat );
    YTilde = new PaceMatrix((new PaceMatrix(aTilde)).times( sde ));
    X.rsolve( YTilde, pvt, pvt.size() );
    betaTilde = YTilde.getColumn(0).unpivoting( pvt, k );
    System.out.println( "\nThe empirical Bayes estimate of coefficients = \n"+
			betaTilde );
	
    System.out.println( "Quadratic loss = " + 
			( new PaceMatrix( X.times( new 
			  PaceMatrix(beta.minus(betaTilde)) )))
			.getColumn(0).sum2() );
	
  }
}



