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
 *    Matrix.java
 *    Copyright (C) 1999 Yong Wang, Eibe Frank, Len Trigg, Gabi Schmidberger
 *
 *    The code contains some functions from the CERN Jet Java libraries
 *    for these the following copyright applies:
 *
 *    Copyright (C) 1999 CERN - European Organization for Nuclear Research.
 *    Permission to use, copy, modify, distribute and sell this software and
 *    its documentation for any purpose is hereby granted without fee, provided
 *    that the above copyright notice appear in all copies and that both that
 *    copyright notice and this permission notice appear in supporting documentation. 
 *    CERN and the University of Waikato make no representations about the
 *    suitability of this software for any purpose.
 *    It is provided "as is" without expressed or implied warranty.
 *
 */

package weka.core;

import java.io.Writer;
import java.io.Reader;
import java.io.LineNumberReader;
import java.io.Serializable;
import java.util.StringTokenizer;

/**
 * Class for performing operations on a matrix of floating-point values.
 *
 * @author Gabi Schmidberger (gabi@cs.waikato.ac.nz)
 * @author Yong Wang (yongwang@cs.waikato.ac.nz)
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Len Trigg (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.17 $
 */
public class Matrix implements Cloneable, Serializable {

  /**
   * The values of the matrix */
  protected double [][] m_Elements;

  /**
   * Constructs a matrix and initializes it with default values.
   *
   * @param nr the number of rows
   * @param nc the number of columns
   */
  public Matrix(int nr, int nc) {

    m_Elements = new double[nr][nc];
    initialize();
  }

  /**
   * Constructs a matrix using a given array.
   *
   * @param array the values of the matrix
   */
  public Matrix(double[][] array) throws Exception {
    
    m_Elements = new double[array.length][array[0].length];
    for (int i = 0; i < array.length; i++) {
      for (int j = 0; j < array[0].length; j++) {
	m_Elements[i][j] = array[i][j];
      }
    } 
  }

  /**
   * Reads a matrix from a reader. The first line in the file should
   * contain the number of rows and columns. Subsequent lines
   * contain elements of the matrix.
   *
   * @param r the reader containing the matrix
   * @exception Exception if an error occurs
   */
  public Matrix(Reader r) throws Exception {

    LineNumberReader lnr = new LineNumberReader(r);
    String line;
    int currentRow = -1;

    while ((line = lnr.readLine()) != null) {

      if (line.startsWith("%")) { // Comments
	continue;
      }
      StringTokenizer st = new StringTokenizer(line);
      if (!st.hasMoreTokens()) {  // Ignore blank lines
	continue;
      }

      if (currentRow < 0) {
	int rows = Integer.parseInt(st.nextToken());
	if (!st.hasMoreTokens()) {
	  throw new Exception("Line " + lnr.getLineNumber() 
			      + ": expected number of columns");
	}
	int cols = Integer.parseInt(st.nextToken());
	m_Elements = new double [rows][cols];
	initialize();
	currentRow ++;
	continue;

      } else {
	if (currentRow == numRows()) {
	  throw new Exception("Line " + lnr.getLineNumber() 
			      + ": too many rows provided");
	}
	for (int i = 0; i < numColumns(); i++) {
	  if (!st.hasMoreTokens()) {
	    throw new Exception("Line " + lnr.getLineNumber() 
				+ ": too few matrix elements provided");
	  }
	  m_Elements[currentRow][i] = Double.valueOf(st.nextToken())
	    .doubleValue();
	}
	currentRow ++;
      }
    }
    if (currentRow == -1) {
      throw new Exception("Line " + lnr.getLineNumber() 
			      + ": expected number of rows");
    } else if (currentRow != numRows()) {
      throw new Exception("Line " + lnr.getLineNumber() 
			  + ": too few rows provided");
    }
  }

  /**
   * Creates and returns a clone of this object.
   *
   * @return a clone of this instance.
   * @exception CloneNotSupportedException if an error occurs
   */
  public Object clone() throws CloneNotSupportedException {

    Matrix m = (Matrix)super.clone();
    m.m_Elements = new double[numRows()][numColumns()];
    for (int r = 0; r < numRows(); r++) {
      for (int c = 0; c < numColumns(); c++) {
        m.m_Elements[r][c] = m_Elements[r][c];
      }
    }
    return m;
  }

  /**
   * Writes out a matrix.
   *
   * @param w the output Writer
   * @exception Exception if an error occurs
   */
  public void write(Writer w) throws Exception {

    w.write("% Rows\tColumns\n");
    w.write("" + numRows() + "\t" + numColumns() + "\n");
    w.write("% Matrix elements\n");
    for(int i = 0; i < numRows(); i++) {
      for(int j = 0; j < numColumns(); j++) {
	w.write("" + m_Elements[i][j] + "\t");
      }
      w.write("\n");
    }
    w.flush();
  }

  /**
   * Resets the elements to default values (i.e. 0).
   */
  protected void initialize() {

    for (int i = 0; i < numRows(); i++) {
      for (int j = 0; j < numColumns(); j++) {
	m_Elements[i][j] = 0;
      }
    }
  }

  /**
   * Returns the value of a cell in the matrix.
   *
   * @param rowIndex the row's index
   * @param columnIndex the column's index
   * @return the value of the cell of the matrix
   */
  public final double getElement(int rowIndex, int columnIndex) {
    
    return m_Elements[rowIndex][columnIndex];
  }

  /**
   * Add a value to an element.
   *
   * @param rowIndex the row's index.
   * @param columnIndex the column's index.
   * @param value the value to add.
   */
  public final void addElement(int rowIndex, int columnIndex, double value) {

    m_Elements[rowIndex][columnIndex] += value;
  }

  /**
   * Returns the number of rows in the matrix.
   *
   * @return the number of rows
   */
  public final int numRows() {
  
    return m_Elements.length;
  }

  /**
   * Returns the number of columns in the matrix.
   *
   * @return the number of columns
   */
  public final int numColumns() {
  
    return m_Elements[0].length;
  }

  /**
   * Sets an element of the matrix to the given value.
   *
   * @param rowIndex the row's index
   * @param columnIndex the column's index
   * @param value the value
   */
  public final void setElement(int rowIndex, int columnIndex, double value) {
    
    m_Elements[rowIndex][columnIndex] = value;
  }

  /**
   * Sets a row of the matrix to the given row. Performs a deep copy.
   *
   * @param index the row's index
   * @param newRow an array of doubles
   */
  public final void setRow(int index, double[] newRow) {

    for (int i = 0; i < newRow.length; i++) {
      m_Elements[index][i] = newRow[i];
    }
  }
  
  /**
   * Gets a row of the matrix and returns it as double array.
   *
   * @param index the row's index
   * @return an array of doubles
   */
  public double[] getRow(int index) {

    double [] newRow = new double[this.numColumns()];
    for (int i = 0; i < newRow.length; i++) {
      newRow[i] = m_Elements[index][i];
    }
    return newRow;
  }

  /**
   * Gets a column of the matrix and returns it as a double array.
   *
   * @param index the column's index
   * @return an array of doubles
   */
  public double[] getColumn(int index) {

    double [] newColumn = new double[this.numRows()];
    for (int i = 0; i < newColumn.length; i++) {
      newColumn[i] = m_Elements[i][index];
    }
    return newColumn;
  }

  /**
   * Sets a column of the matrix to the given column. Performs a deep copy.
   *
   * @param index the column's index
   * @param newColumn an array of doubles
   */
  public final void setColumn(int index, double[] newColumn) {

    for (int i = 0; i < m_Elements.length; i++) {
      m_Elements[i][index] = newColumn[i];
    }
  }

  /** 
   * Converts a matrix to a string
   *
   * @return the converted string
   */
  public String toString() {

    // Determine the width required for the maximum element,
    // and check for fractional display requirement.
    double maxval = 0;
    boolean fractional = false;
    for(int i = 0; i < m_Elements.length; i++) {
      for(int j = 0; j < m_Elements[i].length; j++) {
	double current = m_Elements[i][j];
        if (current < 0) {
          current *= -10;
        }
	if (current > maxval) {
	  maxval = current;
	}
	double fract = current - Math.rint(current);
	if (!fractional
	    && ((Math.log(fract) / Math.log(10)) >= -2)) {
	  fractional = true;
	}
      }
    }
    int width = (int)(Math.log(maxval) / Math.log(10) 
                      + (fractional ? 4 : 1));

    StringBuffer text = new StringBuffer();   
    for(int i = 0; i < m_Elements.length; i++) {
      for(int j = 0; j < m_Elements[i].length; j++) {
        text.append(" ").append(Utils.doubleToString(m_Elements[i][j],
                                                     width,
                                                     (fractional ? 2 : 0)));
      }
      text.append("\n");
    }
    
    return text.toString();
  } 
    
  /**
   * Returns the sum of this matrix with another.
   *
   * @return a matrix containing the sum.
   */
  public final Matrix add(Matrix other) {

    int nr = m_Elements.length, nc = m_Elements[0].length;
    Matrix b;
    try {
      b = (Matrix)clone();
    } catch (CloneNotSupportedException ex) {
      b = new Matrix(nr, nc);
    }
    
    for(int i = 0;i < nc; i++) {
      for(int j = 0; j < nr; j++) {
        b.m_Elements[i][j] = m_Elements[j][i] + other.m_Elements[j][i];
      }
    }
    return b;
  }
  
  /**
   * Returns the transpose of a matrix.
   *
   * @return the transposition of this instance.
   */
  public final Matrix transpose() {

    int nr = m_Elements.length, nc = m_Elements[0].length;
    Matrix b = new Matrix(nc, nr);

    for(int i = 0;i < nc; i++) {
      for(int j = 0; j < nr; j++) {
	b.m_Elements[i][j] = m_Elements[j][i];
      }
    }

    return b;
  }
  
  /**
   * Returns true if the matrix is symmetric.
   *
   * @return boolean true if matrix is symmetric.
   */
  public boolean isSymmetric() {

    int nr = m_Elements.length, nc = m_Elements[0].length;
    if (nr != nc)
      return false;

    for(int i = 0; i < nc; i++) {
      for(int j = 0; j < i; j++) {
	if (m_Elements[i][j] != m_Elements[j][i])
	  return false;
      }
    }
    return true;
  }

  /**
   * Returns the multiplication of two matrices
   *
   * @param b the multiplication matrix
   * @return the product matrix
   */
  public final Matrix multiply(Matrix b) {
   
    int nr = m_Elements.length, nc = m_Elements[0].length;
    int bnr = b.m_Elements.length, bnc = b.m_Elements[0].length;
    Matrix c = new Matrix(nr, bnc);

    for(int i = 0; i < nr; i++) {
      for(int j = 0; j< bnc; j++) {
	for(int k = 0; k < nc; k++) {
	  c.m_Elements[i][j] += m_Elements[i][k] * b.m_Elements[k][j];
	}
      }
    }

    return c;
  }

  /**
   * Performs a (ridged) linear regression.
   *
   * @param y the dependent variable vector
   * @param ridge the ridge parameter
   * @return the coefficients 
   * @exception IllegalArgumentException if not successful
   */
  public final double[] regression(Matrix y, double ridge) {

    if (y.numColumns() > 1) {
      throw new IllegalArgumentException("Only one dependent variable allowed");
    }
    int nc = m_Elements[0].length;
    double[] b = new double[nc];
    Matrix xt = this.transpose();

    boolean success = true;

    do {
      Matrix ss = xt.multiply(this);
      
      // Set ridge regression adjustment
      for (int i = 0; i < nc; i++) {
	ss.setElement(i, i, ss.getElement(i, i) + ridge);
      }

      // Carry out the regression
      Matrix bb = xt.multiply(y);
      for(int i = 0; i < nc; i++) {
	b[i] = bb.m_Elements[i][0];
      }
      try {
	ss.solve(b);
	success = true;
      } catch (Exception ex) {
	ridge *= 10;
	success = false;
      }
    } while (!success);

    return b;
  }

  /**
   * Performs a weighted (ridged) linear regression. 
   *
   * @param y the dependent variable vector
   * @param w the array of data point weights
   * @param ridge the ridge parameter
   * @return the coefficients 
   * @exception IllegalArgumentException if the wrong number of weights were
   * provided.
   */
  public final double[] regression(Matrix y, double [] w, double ridge) {

    if (w.length != numRows()) {
      throw new IllegalArgumentException("Incorrect number of weights provided");
    }
    Matrix weightedThis = new Matrix(numRows(), numColumns());
    Matrix weightedDep = new Matrix(numRows(), 1);
    for (int i = 0; i < w.length; i++) {
      double sqrt_weight = Math.sqrt(w[i]);
      for (int j = 0; j < numColumns(); j++) {
	weightedThis.setElement(i, j, getElement(i, j) * sqrt_weight);
      }
      weightedDep.setElement(i, 0, y.getElement(i, 0) * sqrt_weight);
    }
    return weightedThis.regression(weightedDep, ridge);
  }

  /**
   * Returns the L part of the matrix.
   * This does only make sense after LU decomposition.
   *
   * @return matrix with the L part of the matrix; 
   */
  public Matrix getL() throws Exception {

    int nr = m_Elements.length;    // num of rows
    int nc = m_Elements[0].length; // num of columns
    double[][] ld = new double[nr][nc];
    
    for (int i = 0; i < nr; i++) {
      for (int j = 0; (j < i) && (j < nc); j++) {
	ld[i][j] = m_Elements[i][j];
      }
      if (i < nc) ld[i][i] = 1;
    }
    Matrix l = new Matrix(ld);
    return l;
  }

  /**
   * Returns the U part of the matrix.
   * This does only make sense after LU decomposition.
   *
   * @return matrix with the U part of a matrix; 
   */
  public Matrix getU() throws Exception {

    int nr = m_Elements.length;    // num of rows
    int nc = m_Elements[0].length; // num of columns
    double[][] ud = new double[nr][nc];
    
    for (int i = 0; i < nr; i++) {
      for (int j = i; j < nc ; j++) {
	ud[i][j] = m_Elements[i][j];
      }
    }
    Matrix u = new Matrix(ud);
    return u;
  }
  
  /**
   * Performs a LUDecomposition on the matrix.
   * It changes the matrix into its LU decomposition using the
   * Crout algorithm
   *
   * @return the indices of the row permutation
   * @exception Exception if the matrix is singular 
   */
  public int [] LUDecomposition() throws Exception {
    
    int nr = m_Elements.length;    // num of rows
    int nc = m_Elements[0].length; // num of columns

    int [] piv = new int[nr];

    double [] factor = new double[nr];
    double dd;
    for (int row = 0; row < nr; row++) {
      double max = Math.abs(m_Elements[row][0]);
      for (int col = 1; col < nc; col++) {
	if ((dd = Math.abs(m_Elements[row][col])) > max)
	  max = dd;
      }
      if (max < 0.000000001) {
	throw new Exception("Matrix is singular!");
      }
      factor[row] = 1 / max;
    }

    for (int i = 1; i < nr; i++) piv[i] = i;

    for (int col = 0; col < nc; col++) {

      // compute beta i,j
      for (int row = 0; (row <= col) && (row < nr); row ++) {
	double sum = 0.0;

	for (int k = 0; k < row; k++) {
	  sum += m_Elements[row][k] * m_Elements[k][col];
	}
	m_Elements[row][col] = m_Elements[row][col] - sum; // beta i,j
;
      }

      // compute alpha i,j
      for (int row = col + 1; row < nr; row ++) {
	double sum = 0.0;
	for (int k = 0; k < col; k++) {

	  sum += m_Elements[row][k] * m_Elements[k][col];
	}
        // alpha i,j before division
	m_Elements[row][col] = (m_Elements[row][col] - sum);

      }
      
      // find row for division:see if any of the alphas are larger then b j,j
      double maxFactor = Math.abs(m_Elements[col][col]) * factor[col];
      int newrow = col;

      for (int ii = col + 1; ii < nr; ii++) {
	if ((Math.abs(m_Elements[ii][col]) * factor[ii]) > maxFactor) {
	  newrow = ii; // new row
	  maxFactor = Math.abs(m_Elements[ii][col]) * factor[ii];
	}  
      }
      if (newrow != col) {
        // swap the rows
	for (int kk = 0; kk < nc; kk++) {
	  double hh = m_Elements[col][kk];
	  m_Elements[col][kk] = m_Elements[newrow][kk];
	  m_Elements[newrow][kk] = hh;
	}
	// remember pivoting
	int help = piv[col]; piv[col] = piv[newrow]; piv[newrow] = help;
	double hh  = factor[col]; factor[col] = factor[newrow]; factor[newrow] = help;
      }

      if (m_Elements[col][col] == 0.0) {
	throw new Exception("Matrix is singular");
      }

      for (int row = col + 1; row < nr; row ++) {
	// divide all 
	m_Elements[row][col] = m_Elements[row][col] / 
	  m_Elements[col][col];
      }
   }

    return piv;
  }
  
  /**
   * Solve A*X = B using backward substitution.
   * A is current object (this) 
   * B parameter bb
   * X returned in parameter bb
   *
   * @param bb first vektor B in above equation then X in same equation.
   * @exception matrix is singulaer
   */
  public void solve(double [] bb) throws Exception {

    int nr = m_Elements.length;
    int nc = m_Elements[0].length;

    double [] b = new double[bb.length];
    double [] x = new double[bb.length];
    double [] y = new double[bb.length];

    int [] piv = this.LUDecomposition();

    // change b according to pivot vector
    for (int i = 0; i < piv.length; i++) {
      b[i] = bb[piv[i]];
    }
    
    y[0] = b[0];

    for (int row = 1; row < nr; row++) {
      double sum = 0.0;
      for (int col = 0; col < row; col++) {

	sum += m_Elements[row][col] * y[col];
      }
      y[row] = b[row] - sum;
    }

    x[nc - 1] = y[nc - 1] / m_Elements[nc - 1][nc - 1];
    for (int row = nc - 2; row >= 0; row--) {
      double sum = 0.0;
      for (int col = row + 1; col < nc; col++) {

	sum += m_Elements[row][col] * x[col];
      }
      x[row] = (y[row] - sum) / m_Elements[row][row];
    }

    // change x according to pivot vector
    for (int i = 0; i < piv.length; i++) {

      //bb[piv[i]] = x[i];
      bb[i] = x[i];
    }
  }

  /**
   * Performs Eigenvalue Decomposition using Householder QR Factorization
   *
   * This function is adapted from the CERN Jet Java libraries, for it 
   * the following copyright applies (see also, text on top of file)
   * Copyright (C) 1999 CERN - European Organization for Nuclear Research.
   *
   * Matrix must be symmetrical.
   * Eigenvectors are return in parameter V, as columns of the 2D array.
   * Eigenvalues are returned in parameter d.
   *
   *
   * @param V double array in which the eigenvectors are returned 
   * @param d array in which the eigenvalues are returned
   * @exception if matrix is not symmetric
   */

  
  public void eigenvalueDecomposition(double [][] V, double [] d) throws Exception {

    if (!this.isSymmetric())
      throw new Exception("EigenvalueDecomposition: Matrix must be symmetric.");
    
    int n = this.numRows();
    double[]  e = new double [n]; //todo, don't know what this e is really for!
    
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
	V[i][j] = m_Elements[i][j];
      }
    }
      
    // Tridiagonalize using householder reduction
    tred2(V, d, e, n);
      
    // Diagonalize.
    tql2(V, d, e, n);

    // Matrix v = new Matrix (V);
    // testEigen(v, d);
  } 


  /**
   * Symmetric Householder reduction to tridiagonal form.
   *
   * This function is adapted from the CERN Jet Java libraries, for it 
   * the following copyright applies (see also, text on top of file)
   * Copyright (C) 1999 CERN - European Organization for Nuclear Research.
   *
   * @param V containing  a copy of the values of the matrix
   * @param d 
   * @param e
   * @param n size of matrix
   * @exception if reduction doesn't work
   */
  private void tred2 (double [][] V, double [] d, double [] e, int n) throws Exception {
    //  This is derived from the Algol procedures tred2 by
    //  Bowdler, Martin, Reinsch, and Wilkinson, Handbook for
    //  Auto. Comp., Vol.ii-Linear Algebra, and the corresponding
    //  Fortran subroutine in EISPACK.
    
    for (int j = 0; j < n; j++) {
      d[j] = V[n-1][j];
    }
    
    // Householder reduction to tridiagonal form.

    // Matrix v = new Matrix(V);
    // System.out.println("before household - Matrix V \n" + v);
    for (int i = n-1; i > 0; i--) {
   
      // Matrix v = new Matrix(V);
      // System.out.println("Matrix V \n" + v);

      // Scale to avoid under/overflow.
      double scale = 0.0;
      double h = 0.0;
      for (int k = 0; k < i; k++) {
	scale = scale + Math.abs(d[k]);
      }
      if (scale == 0.0) {
	e[i] = d[i-1];
	for (int j = 0; j < i; j++) {
	  d[j] = V[i-1][j];
	  V[i][j] = 0.0;
	  V[j][i] = 0.0;
	}
      } else {
	
	// Generate Householder vector.
	for (int k = 0; k < i; k++) {
	  d[k] /= scale;
	  h += d[k] * d[k];
	}
	double f = d[i-1];
	double g = Math.sqrt(h);
	if (f > 0) {
	  g = -g;
	}
	e[i] = scale * g;
	h = h - f * g;
	d[i-1] = f - g;
	for (int j = 0; j < i; j++) {
	  e[j] = 0.0;
	}
	
	// Apply similarity transformation to remaining columns.
	for (int j = 0; j < i; j++) {
	  f = d[j];
	  V[j][i] = f;
	  g = e[j] + V[j][j] * f;
	  for (int k = j+1; k <= i-1; k++) {
	    g += V[k][j] * d[k];
	    e[k] += V[k][j] * f;
	  }
	  e[j] = g;
	}
	f = 0.0;
	for (int j = 0; j < i; j++) {
	  e[j] /= h;
	  f += e[j] * d[j];
	}
	double hh = f / (h + h);
	for (int j = 0; j < i; j++) {
	  e[j] -= hh * d[j];
	}
	for (int j = 0; j < i; j++) {
	  f = d[j];
	  g = e[j];
	  for (int k = j; k <= i-1; k++) {
	    V[k][j] -= (f * e[k] + g * d[k]);
	  }
	  d[j] = V[i-1][j];
	  V[i][j] = 0.0;
	}
      }
      d[i] = h;

    }

    // v = new Matrix(V);
    // System.out.println("before accumulate Matrix V \n" + v);

    // Accumulate transformations.
    
    for (int i = 0; i < n-1; i++) {
      V[n-1][i] = V[i][i];
      V[i][i] = 1.0;
      double h = d[i+1];
      if (h != 0.0) {
	for (int k = 0; k <= i; k++) {
	  d[k] = V[k][i+1] / h;
	}
	for (int j = 0; j <= i; j++) {
	  double g = 0.0;
	  for (int k = 0; k <= i; k++) {
	    g += V[k][i+1] * V[k][j];
	  }
	  for (int k = 0; k <= i; k++) {
	    V[k][j] -= g * d[k];
	  }
	}
      }
      for (int k = 0; k <= i; k++) {
	V[k][i+1] = 0.0;
      }
      
      // v = new Matrix(V);
      // System.out.println(" accumulate " + i + " Matrix V \n" + v);
    }
    for (int j = 0; j < n; j++) {
      d[j] = V[n-1][j];
      V[n-1][j] = 0.0;
    }
    V[n-1][n-1] = 1.0;
    e[0] = 0.0;
    // v = new Matrix(V);
    // System.out.println(" end accumulate  Matrix V \n" + v);
  }   

  /**
   * Symmetric tridiagonal QL algorithm.
   *
   * This function is adapted from the CERN Jet Java libraries, for it 
   * the following copyright applies (see also, text on top of file)
   * Copyright (C) 1999 CERN - European Organization for Nuclear Research.
   *
   * @param V tridiagonalized matrix
   * @param d
   * @param e
   * @param n size of matrix
   * @Exception if factorization fails
   */
  private void tql2 (double [][] V, double [] d, double [] e, int n)
    throws Exception {
    
    //  This is derived from the Algol procedures tql2, by
    //  Bowdler, Martin, Reinsch, and Wilkinson, Handbook for
    //  Auto. Comp., Vol.ii-Linear Algebra, and the corresponding
    //  Fortran subroutine in EISPACK.
    
    for (int i = 1; i < n; i++) {
      e[i-1] = e[i];
    }
    e[n-1] = 0.0;
    
    double f = 0.0;
    double tst1 = 0.0;
    double eps = Math.pow(2.0,-52.0);
    for (int l = 0; l < n; l++) {
      
      // Find small subdiagonal element
      
      tst1 = Math.max(tst1,Math.abs(d[l]) + Math.abs(e[l]));
      int m = l;
      while (m < n) {
	if (Math.abs(e[m]) <= eps*tst1) {
	  break;
	}
	m++;
      }
      
      // If m == l, d[l] is an eigenvalue,
      // otherwise, iterate.
      
      if (m > l) {
	int iter = 0;
	do {
	  iter = iter + 1;  // (Could check iteration count here.)
	  
	  // Compute implicit shift
	  
	  double g = d[l];
	  double p = (d[l+1] - g) / (2.0 * e[l]);
	  double r = hypot(p,1.0);
	  if (p < 0) {
	    r = -r;
	  }
	  d[l] = e[l] / (p + r);
	  d[l+1] = e[l] * (p + r);
	  double dl1 = d[l+1];
	  double h = g - d[l];
	  for (int i = l+2; i < n; i++) {
	    d[i] -= h;
	  }
	  f = f + h;
	  
	  // Implicit QL transformation.
	  
	  p = d[m];
	  double c = 1.0;
	  double c2 = c;
	  double c3 = c;
	  double el1 = e[l+1];
	  double s = 0.0;
	  double s2 = 0.0;
	  for (int i = m-1; i >= l; i--) {
	    c3 = c2;
	    c2 = c;
	    s2 = s;
	    g = c * e[i];
	    h = c * p;
	    r = Matrix.hypot(p,e[i]);
	    e[i+1] = s * r;
	    s = e[i] / r;
	    c = p / r;
	    p = c * d[i] - s * g;
	    d[i+1] = h + s * (c * g + s * d[i]);
	    
	    // Accumulate transformation.
	    
	    for (int k = 0; k < n; k++) {
	      h = V[k][i+1];
	      V[k][i+1] = s * V[k][i] + c * h;
	      V[k][i] = c * V[k][i] - s * h;
	    }
	    // Matrix v = new Matrix(V);
	    // System.out.println("Matrix V \n" + v);
	  }
	  p = -s * s2 * c3 * el1 * e[l] / dl1;
	  e[l] = s * p;
	  d[l] = c * p;
	  
	  // Check for convergence.
	  
	} while (Math.abs(e[l]) > eps*tst1);
      }
      d[l] = d[l] + f;
      e[l] = 0.0;
    }
    
    // Sort eigenvalues and corresponding vectors.
    /* 
    for (int i = 0; i < n-1; i++) {
      int k = i;
      double p = d[i];
      for (int j = i+1; j < n; j++) {
	if (d[j] < p) {
	  k = j;
	  p = d[j];
	}
      }
      if (k != i) {
	d[k] = d[i];
	d[i] = p;
	for (int j = 0; j < n; j++) {
	  p = V[j][i];
	  V[j][i] = V[j][k];
	  V[j][k] = p;
	}
      }
    }*/
    
  }   

  /**
   * Returns sqrt(a^2 + b^2) without under/overflow.
   *   
   * This function is adapted from the CERN Jet Java libraries, for it 
   * the following copyright applies (see also, text on top of file)
   * Copyright (C) 1999 CERN - European Organization for Nuclear Research.
   *
   * @param a length of one side of rectangular triangle
   * @param b length of other side of rectangular triangle
   * @return lenght of third side
   */
  protected static double hypot(double a, double b) {
    double r;
    if (Math.abs(a) > Math.abs(b)) {
      r = b/a;
      r = Math.abs(a)*Math.sqrt(1+r*r);
    } else if (b != 0) {
      r = a/b;
      r = Math.abs(b)*Math.sqrt(1+r*r);
    } else {
      r = 0.0;
    }
    return r;
  }

  /**
   * Test eigenvectors and eigenvalues.
   * function is used for debugging
   *
   * @param V matrix with eigenvectors of A
   * @param d array with eigenvalues of A
   * @exception if new matrix cannot be made
   */
  public boolean testEigen(Matrix V, double [] d, boolean verbose)
    throws Exception {

    boolean equal = true;
    if (verbose) {
      System.out.println("--- test Eigenvectors and Eigenvalues of Matrix A --------");
      System.out.println("Matrix A \n" + this);
      System.out.println("Matrix V, the columns are the Eigenvectors\n" + V);
      System.out.println("the Eigenvalues are");
      for (int k = 0; k < d.length; k++) {
	System.out.println( Utils.doubleToString(d[k], 2));
      }
      System.out.println("\n---");
    }
    double [][] f = new double[V.numRows()][1];
    Matrix F = new Matrix(f);
    for (int i = 0; i < V.numRows(); i++) {
      double [] col =  V.getColumn(i);
      double norm = 0.0;
      for (int j = 0; j < col.length; j++) {
	norm += Math.pow(col[j], 2.0);
      }
      norm = Math.pow(norm, 0.5);
      
      F.setColumn(0, V.getColumn(i));
      if (verbose)
	System.out.println("Eigenvektor " + i + " =\n" + F + "\nNorm " + norm);
      Matrix R = this.multiply(F);
      if (verbose) {
	System.out.println("this x Eigenvektor " + i + " =\n");
	for (int k = 0; k < V.numRows(); k++) {
	  System.out.print(Utils.doubleToString(R.getElement(k, 0), 2) + "  ");
	}
	System.out.println(" ");
	System.out.println(" ");
	System.out.println("Eigenvektor "+ i + " x Eigenvalue " +
			   Utils.doubleToString(d[i], 2) +" =");
      }
      for (int k = 0; k < V.numRows() && equal; k++) {
	double dd = F.getElement(k, 0) * d[i];
	double diff = dd - R.getElement(k, 0);
	equal = Math.abs(diff) < Utils.SMALL;
	if (Math.abs(diff) > Utils.SMALL)
	  System.out.println("OOOOOOps");
	if (verbose) {
	  System.out.print( Utils.doubleToString(dd, 2) + "  ");
	}
      }
      if (verbose) {
	System.out.println(" ");
	System.out.println("---");
      }
    }
    return equal;
  }
  
  /**
   * Main method for testing this class.
   */
  public static void main(String[] ops) {

    double[] first = {2.3, 1.2, 5};
    double[] second = {5.2, 1.4, 9};
    double[] response = {4, 7, 8};
    double[] weights = {1, 2, 3};

    try {
      // test eigenvaluedecomposition
      double [][] m = {{1, 2, 3}, {2, 5, 6},{3, 6, 9}};
      Matrix M = new Matrix(m);
      int n = M.numRows();
      double [][] V = new double[n][n];
      double [] d = new double[n];
      double [] e = new double[n];
      M.eigenvalueDecomposition(V, d);
      Matrix v = new Matrix(V);
      // M.testEigen(v, d, );
      // end of test-eigenvaluedecomposition
      
      Matrix a = new Matrix(2, 3);
      Matrix b = new Matrix(3, 2);
      System.out.println("Number of columns for a: " + a.numColumns());
      System.out.println("Number of rows for a: " + a.numRows());
      a.setRow(0, first);
      a.setRow(1, second);
      b.setColumn(0, first);
      b.setColumn(1, second);
      System.out.println("a:\n " + a);
      System.out.println("b:\n " + b);
      System.out.println("a (0, 0): " + a.getElement(0, 0));
      System.out.println("a transposed:\n " + a.transpose());
      System.out.println("a * b:\n " + a.multiply(b));
      Matrix r = new Matrix(3, 1);
      r.setColumn(0, response);
      System.out.println("r:\n " + r);
      System.out.println("Coefficients of regression of b on r: ");
      double[] coefficients = b.regression(r, 1.0e-8);
      for (int i = 0; i < coefficients.length; i++) {
	System.out.print(coefficients[i] + " ");
      }
      System.out.println();
      System.out.println("Weights: ");
      for (int i = 0; i < weights.length; i++) {
	System.out.print(weights[i] + " ");
      }
      System.out.println();
      System.out.println("Coefficients of weighted regression of b on r: ");
      coefficients = b.regression(r, weights, 1.0e-8);
      for (int i = 0; i < coefficients.length; i++) {
	System.out.print(coefficients[i] + " ");
      }
      System.out.println();
      a.setElement(0, 0, 6);
      System.out.println("a with (0, 0) set to 6:\n " + a);
      a.write(new java.io.FileWriter("main.matrix"));
      System.out.println("wrote matrix to \"main.matrix\"\n" + a);
      a = new Matrix(new java.io.FileReader("main.matrix"));
      System.out.println("read matrix from \"main.matrix\"\n" + a);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
}




