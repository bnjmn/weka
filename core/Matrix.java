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
 * @version $Revision: 1.14 $
 */
public class Matrix implements Cloneable, Serializable {

  /** The data in the matrix. */
  protected double [][] m_Elements;

  /**
   * Constructs a matrix.
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
   * @param nr the number of rows
   * @param nc the number of columns
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
   * Writes out a matrix
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
   * @return the value
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
   * @return the sum.
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
   * @return the transposition of this instance).
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
   * Reurns the multiplication of two matrices
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
   * @return the coefficients 
   * @exception IllegalArgumentException if not successful
   */
  public final double[] regression(Matrix y) {

    if (y.numColumns() > 1) {
      throw new IllegalArgumentException("Only one dependent variable allowed");
    }
    int nc = m_Elements[0].length;
    double[] b = new double[nc];
    Matrix xt = this.transpose();

    boolean success = true;
    double ridge = 1e-8;

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
   * @return the coefficients 
   * @exception IllegalArgumentException if the wrong number of weights were
   * provided.
   */
  public final double[] regression(Matrix y, double [] w) {

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
    return weightedThis.regression(weightedDep);
  }

  /**
   * Returns the L part of the matrix.
   * This does only make sense after LU decomposition.
   * Warning: The matrix object doesn't hold information if LU decomposition
   * is performed beforehand.
   *
   * @return L part of a matrix; 
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
   * Warning: The matrix object doesn't hold information if LU decomposition
   * is performed beforehand.
   *
   * @return U part of a matrix; 
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
  public int [] LUDecomposition() throws Exception{
    
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
   * Solve A*X = B using backward substitution
   *
   *  @param  b   A Vector with as many rows as this (=A).
   *  @return     X so that L*U*X = B(piv,:)
   *  @exception matrix is singulaer
   */
  public void solve(double [] bb) throws Exception{

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
   * Main method for testing this class.
   */
  public static void main(String[] ops) {

    double[] first = {2.3, 1.2, 5};
    double[] second = {5.2, 1.4, 9};
    double[] response = {4, 7, 8};
    double[] weights = {1, 2, 3};

    try {
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
      double[] coefficients = b.regression(r);
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
      coefficients = b.regression(r, weights);
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
