/*
 *    Matrix.java
 *    Copyright (C) 1999 Yong Wang, Eibe Frank, Len Trigg
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

import java.io.*;

/**
 * Class for performing operations on a matrix of floating-point values.
 * Some of the code is adapted from Numerical Recipes in C.
 *
 * @author Yong Wang (yongwang@cs.waikato.ac.nz)
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Len Trigg (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public final class Matrix {

  /** The data in the matrix. */
  private double [][] elements;

  /**
   * Constructs a matrix.
   *
   * @param nr the number of the rows
   * @param nc the number of the columns
   */
  public  Matrix(int nr, int nc){

    elements = new double[nr][nc];
  }

  /**
   * Returns the value of a cell in the matrix.
   *
   * @param rowIndex the row's index
   * @param columnIndex the column's index
   * @return the value
   */
  public final double getElement(int rowIndex, int columnIndex) {
    
    return elements[rowIndex][columnIndex];
  }

  /**
   * Returns the number of rows in the matrix.
   *
   * @return the number of rows
   */
  public final int numRows() {
  
    return elements.length;
  }

  /**
   * Returns the number of columns in the matrix.
   *
   * @return the number of columns
   */
  public final int numColumns() {
  
    return elements[0].length;
  }

  /**
   * Sets an element of the matrix to the given value.
   *
   * @param rowIndex the row's index
   * @param columnIndex the column's index
   * @param value the value
   */
  public final void setElement(int rowIndex, int columnIndex, double value) {
    
    elements[rowIndex][columnIndex] = value;
  }

  /**
   * Sets a row of the matrix to the given row. Performs a deep copy.
   *
   * @param index the row's index
   * @param newRow an array of doubles
   */
  public final void setRow(int index, double[] newRow) {

    for (int i = 0; i < newRow.length; i++)
      elements[index][i] = newRow[i];
  }
  
  /**
   * Sets a column of the matrix to the given column. Performs a deep copy.
   *
   * @param index the column's index
   * @param newColumn an array of doubles
   */
  public final void setColumn(int index, double[] newColumn) {

    for (int i = 0; i < elements.length; i++)
      elements[i][index] = newColumn[i];
  }

  /** 
   * Converts a matrix to a string
   *
   * @return the converted string
   */
  public final String toString() {
    
    StringBuffer text = new StringBuffer();
   
    for(int i = 0; i < elements.length; i++) {
      for(int j = 0; j < elements[i].length; j++)
	text.append("\t" + Utils.doubleToString(elements[i][j],5,3));
      text.append("\n");
    }
    
    return text.toString();
  } 
    
  /**
   * Returns the transpose of a matrix 
   *
   * @return the transposed matrix
   */
  public final Matrix transpose() {

    int nr = elements.length, nc = elements[0].length;
    Matrix b = new Matrix(nc, nr);

    for(int i = 0;i < nc; i++) {
      for(int j = 0; j < nr; j++) {
	b.elements[i][j] = elements[j][i];
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
   
    int nr = elements.length, nc = elements[0].length;
    int bnr = b.elements.length, bnc = b.elements[0].length;
    Matrix c = new Matrix(nr, bnc);

    for(int i = 0; i < nr; i++) {
      for(int j = 0; j< bnc; j++) {
	for(int k = 0; k < nc; k++) {
	  c.elements[i][j] += elements[i][k] * b.elements[k][j];
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
   * @exception Exception if not successful
   */
  public final double[] regression(Matrix y) throws Exception {

    if (y.numColumns() > 1) {
      throw new Exception("Only one dependent variable allowed");
    }
    int nc = elements[0].length;
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
	b[i] = bb.elements[i][0];
      }
      try {
	ss.lubksb(ss.ludcmp(), b);
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
   * @exception Exception if not successful
   */
  public final double[] regression(Matrix y, double [] w) throws Exception {

    if (w.length != numRows()) {
      throw new Exception("Incorrect number of weights provided");
    }
    Matrix weightedThis = new Matrix(numRows(), numColumns());
    Matrix weightedDep = new Matrix(numRows(), 1);
    for (int i = 0; i < w.length; i++) {
      for (int j = 0; j < numColumns(); j++) {
	weightedThis.setElement(i, j, getElement(i, j) * w[i]);
      }
      weightedDep.setElement(i, 0, y.getElement(i, 0) * w[i]);
    }
    return weightedThis.regression(weightedDep);
  }

  /**
   * Performs LU backward substitution.  Adapted from Numerical Recipes in C.
   *
   * @param indx the indices of the permutation
   * @param b the double vector, storing constant terms in the equation set; 
   * it later stores the computed coefficients' values
   */
  public final void lubksb(int[] indx, double b[]) {
    
    int nc = elements[0].length;
    int ii = -1, ip;
    double sum;
    
    for (int i = 0; i < nc; i++) {
      ip = indx[i];
      sum = b[ip];
      b[ip] = b[i];
      if (ii != -1) {
	for (int j = ii; j < i; j++) {
	  sum -= elements[i][j] * b[j];
	}
      } else if (sum != 0.0) {
	ii = i;
      }
      b[i] = sum;
    }
    for (int i = nc-1 ; i >= 0; i--) {
      sum = b[i];
      for (int j = i+1; j < nc; j++) {
	sum -= elements[i][j] * b[j];
      }
      b[i] = sum / elements[i][i];
    }
  }
  
  /**
   * Performs LU decomposition. Adapted from Numerical Recipes in C.
   *
   * @return the indices of the permutation
   * @exception Exception if the matrix is singular
   */
  public final int[] ludcmp() throws Exception {

    int nc = elements[0].length;
    int[] indx = new int[elements.length];
    int imax = -1;
    double big,dum,sum,temp;
    double vv[];
    
    vv = new double[nc];
    for (int i = 0; i < nc; i++) {
      big=0.0;
      for (int j = 0; j < nc; j++)
	if ((temp = Math.abs(elements[i][j])) > big)
	  big = temp;
      if (big < 0.000000001) 
	throw new Exception("Matrix is singular!");
      vv[i] = 1.0 / big;
    }
    for (int j = 0; j < nc; j++) {
      for (int i = 0; i < j; i++) {
	sum = elements[i][j];
	for (int k = 0; k < i; k++) 
	  sum -= elements[i][k] * elements[k][j];
	elements[i][j] = sum;
      }
      big = 0.0;
      for (int i = j; i < nc; i++) {
	sum = elements[i][j];
	for (int k = 0; k < j; k++)
	  sum -= elements[i][k] * elements[k][j];
	elements[i][j] = sum;
	if ((dum=vv[i] * Math.abs(sum)) >= big) {
	  big=dum;
	  imax=i;
	}
      } 
      if (j != imax) {
	for (int k = 0; k < nc; k++) {
	  dum = elements[imax][k];
	  elements[imax][k] = elements[j][k];
	  elements[j][k] = dum;
	}
	vv[imax] = vv[j];
      }
      indx[j] = imax;
      if (elements[j][j] == 0.0) 
	throw new Exception("Matrix is singular");
      if (j != nc-1) {
	dum = 1.0 / (elements[j][j]);
	for (int i = j+1; i < nc; i++) 
	  elements[i][j] *= dum;
      }
    }
    return indx;
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
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}






