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
 *    BooleanBitMatrix.java
 *    Copyright (C) 2004 Stijn Lievens
 *
 */

package weka.classifiers.misc.monotone;

import weka.core.RevisionHandler;
import weka.core.RevisionUtils;

/**
 * This class is a very simple implementation of a BitMatrix.
 * In fact, it uses a two-dimensional array of booleans, so it is 
 * not very space efficient.
 * 
 * <p>
 * This implementation is part of the master's thesis: "Studie
 * en implementatie van instantie-gebaseerde algoritmen voor gesuperviseerd
 * rangschikken", Stijn Lievens, Ghent University, 2004. 
 * </p>
 * 
 * @author Stijn Lievens (stijn.lievens@ugent.be)
 * @version $Revision: 1.2 $
 */
public class BooleanBitMatrix
  implements BitMatrix, RevisionHandler {

  /** The two-dimensional array of booleans. */
  private boolean[][] m_bits;

  /** The number of rows. */
  private int m_rows;

  /** The number of columns */
  private int m_columns;

  /**
   * Construct a <code> BitMatrix </code> with the indicated
   * number of rows and columns.  All bits are initially 
   * set to <code> false </code>.
   *
   * @param rows the number of rows
   * @param columns the number of column
   */
  public BooleanBitMatrix(int rows, int columns) {
    m_bits = new boolean[rows][columns];
    m_rows = rows;
    m_columns = columns;
  }

  /**
   * A copy constructor.  Constructs a copy of the given 
   * <code> BitMatrix </code>.
   *
   * @param bm the <code> BitMatrix </code> to be copied.
   */
  public BooleanBitMatrix(BooleanBitMatrix bm) {
    this(bm.m_rows, bm.m_columns);
    for (int i = 0; i < m_rows; i++) {
      System.arraycopy(bm.m_bits[i], 0, m_bits[i], 0, m_columns);
    }
  }

  /**
   * Returns the element a the specified position.
   *
   * @param row the row of the position
   * @param column the column of the position
   * @return <code> true </code> if the bit at the 
   * specified position is set, <code> false </code>
   * otherwise
   */
  public boolean get(int row, int column) {
    return m_bits[row][column];
  }

  /** 
   * Sets the bit at the specified position to the specified
   * value.
   *
   * @param row the row of the position
   * @param column the column of the position
   * @param bool the value to fill in
   * @return the value of <code> bool </code>
   */
  public boolean set(int row, int column, boolean bool) {
    m_bits[row][column] = bool;
    return bool;
  }

  /**
   * Sets the bit at the specified position to <code> true. </code>
   *
   * @param row the row of the position
   * @param column the column of the position
   * @return <code> true </code> if the bit was actually
   * set, <code> false </code> otherwise
   */
  public boolean set(int row, int column) {
    return !get(row, column) && set(row, column, true);
  }

  /**
   * Clears the bit at the specified position.
   *
   * @param row the row of the position
   * @param column the column of the position
   * @return <code> true </code> if the bit was actually
   * cleared, <code> false </code> otherwise
   */
  public boolean clear(int row, int column) {
    return get(row, column) && !set(row, column, false);
  }

  /** 
   * Gets the number of rows.
   *
   * @return the number of rows of the matrix
   */
  public int rows() {
    return m_rows;
  }

  /**
   * Gets the number of columns.
   *
   * @return the number of columns of the matrix
   */
  public int columns() {
    return m_columns;
  }

  /**
   * Counts the number of bits that are set in the specified column. 
   *
   * @param column index of the column of which the bits are to be counted
   * @return the number of bits that are set in the requested column 
   */
  public int columnCount(int column) {
    int count = 0;
    for (int i = 0; i < m_rows; i++) {
      count += (m_bits[i][column] ? 1 : 0);
    }
    return count;
  }

  /**
   * Counts the number of bits that are set in the specified row.
   * 
   * @param row index of the row of which the bits are to be counted
   * @return the number of bits that are set in the requested row
   */
  public int rowCount(int row) {
    int count = 0;
    for (int i = 0; i < m_columns; i++) {
      count += (m_bits[row][i] ? 1 : 0);
    }
    return count;
  }

  /**
   * Swap the rows and the columns of the <code> BooleanBitMatrix. </code>
   *
   * @return the transposed matrix
   */
  public BooleanBitMatrix transpose() {
    BooleanBitMatrix transposed = new BooleanBitMatrix(m_columns, m_rows);

    for (int i = 0; i < m_rows; i++) {
      for (int j = 0; j < m_columns; j++) {
	transposed.set(j, i, get(i, j));
      }
    }

    return transposed;
  }

  /**
   * Swaps the rows and the columns of the <code> BooleanBitMatrix, </code>
   * without creating a new object.
   * The <code> BooleanBitMatrix </code> must be a square matrix.
   *
   * @throws IllegalArgumentException if the <code> BooleanBitMatrix </code>
   * is not square.
   */
  public void transposeInPlace() throws IllegalArgumentException {
    if (m_rows != m_columns) {
      throw new IllegalArgumentException
      ("The BooleanBitMatrix is not square"); 
    }
    for (int i = 0; i < m_rows; i++) {
      for (int j = i + 1; j < m_columns; j++) {
	swap(i, j, j, i);
      }
    }
  }

  /**
   * Swap the elements with coordinates <code> (r1,c1) </code>  and 
   * <code> (r2,c2). </code>
   *
   * @param r1 index of first row
   * @param c1 index of first column
   * @param r2 index of second row
   * @param c2 index of second column
   */
  private void swap(int r1, int c1, int r2, int c2) {
    boolean tmp = get(r1, c1);
    set(r1, c1, get(r2, c2));
    set(r2, c2, tmp);
  }

  /**
   * Create a compact string representation of the matrix.
   * 
   * @return a <code> String </code> representing the matrix,
   * row by row.
   */
  public String toString() {
    StringBuffer sb = new StringBuffer(m_rows * (m_columns + 1) );
    for (int i = 0; i < m_rows; i++) {
      for (int j = 0; j < m_columns; j++) { 
	sb.append(get(i, j) ? 1 : 0);
      }
      sb.append("\n");
    }
    return sb.toString();
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 1.2 $");
  }
}
