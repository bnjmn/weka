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
 *    BitMatrix.java
 *    Copyright (C) 2004 Stijn Lievens
 *
 */

package weka.classifiers.misc.monotone;

/**
 * Interface specifying a simple matrix of booleans.  Operations are
 * limited to setting, getting, clearing and counting.
 * <p>
 * This implementation is part of the master's thesis: "Studie
 * en implementatie van instantie-gebaseerde algoritmen voor gesuperviseerd
 * rangschikken", Stijn Lievens, Ghent University, 2004. 
 * </p>
 * 
 * @author Stijn Lievens (stijn.lievens@ugent.be)
 * @version $Revision: 1.1 $
 */
public interface BitMatrix {

  /**
   * Return the element a the specified position.
   *
   * @param row the row of the position
   * @param column the column of the position
   * @return <code> true </code> if the bit at the 
   * specified position is set, <code> false </code>
   * otherwise
   */
  public boolean get(int row, int column);

  /** 
   * Sets the bit at the specified position to the specified
   * value.
   *
   * @param row the row of the position
   * @param column the column of the position
   * @param bool the value to fill in
   * @return the value of <code> bool </code>
   */
  public boolean set(int row, int column, boolean bool);

  /**
   * Sets the bit at the specified position to <code> true. </code>
   * The return value indicates whether anything has changed, 
   * i.e.&nbsp; if the bit at the specified position was <code> true
   * </code> before calling this method, then <code> false </code> is
   * returned (and the bit remains <code> true </code> of course).
   * In the other case <code> true </code> is returned.
   * 
   * @param row the row of the position
   * @param column the column of the position
   * @return <code> true </code> if the bit was actually
   * set, <code> false </code> otherwise
   */
  public boolean set(int row, int column);

  /**
   * Clears the bit at the specified position.  The return value indicates
   * whether the bit was actually cleared, i.e.&nbsp; if the bit was 
   * originally <code> true </code> then <code> true </code> is returned.
   * In the other case <code> false </code> is returned.
   *
   * @param row the row of the position
   * @param column the column of the position
   * @return <code> true </code> if the bit was actually
   * cleared, <code> false </code> otherwise
   */
  public boolean clear(int row, int column);

  /** 
   * Gets the number of rows.
   * 
   * @return the number of rows of the matrix
   */
  public int rows();

  /**
   * Gets the number of columns.
   * 
   * @return the number of columns of the matrix
   */
  public int columns();

  /**
   * Counts the number of bits that are set in the specified column. 
   *
   * @param column index of the column
   * @return the number of bits that are set in the requested column 
   */
  public int columnCount(int column);

  /**
   * Counts the number of bits that are set in the specified row.
   *
   * @param row index of the row
   * @return the number of bits that are set in the requested row
   */
  public int rowCount(int row);
}
