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
 * ResultMatrixSignificance.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */


package weka.experiment;

import weka.core.Utils;

/**
 * This matrix is a container for the datasets and classifier setups and 
 * their statistics. It outputs only the significance indicators - sometimes
 * good for recognizing patterns.
 *
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.3 $
 */

public class ResultMatrixSignificance extends ResultMatrix {
  /**
   * initializes the matrix as 1x1 matrix
   */
  public ResultMatrixSignificance() {
    this(1, 1);
  }

  /**
   * initializes the matrix with the given dimensions
   */
  public ResultMatrixSignificance(int cols, int rows) {
    super(cols, rows);
  }

  /**
   * initializes the matrix with the values from the given matrix
   * @param matrix      the matrix to get the values from
   */
  public ResultMatrixSignificance(ResultMatrix matrix) {
    super(matrix);
  }

  /**
   * returns the name of the output format
   */
  public String getDisplayName() {
    return "Significance only";
  }

  /**
   * removes the stored data but retains the dimensions of the matrix
   */
  public void clear() {
    super.clear();
    setPrintColNames(false);
    setRowNameWidth(40);
    super.setShowStdDev(false);
  }

  /**
   * sets whether to display the std deviations or not - always false!
   */
  public void setShowStdDev(boolean show) {
    // ignore
  }

  /**
   * returns the matrix as plain text
   */
  public String toStringMatrix() {
    StringBuffer        result;
    String[][]          cells;
    int                 i;
    int                 n;
    int                 nameWidth;
    String              line;
    String              colStr;
    int                 rows;

    result = new StringBuffer();
    cells  = toArray();

    // pad names
    nameWidth = getColSize(cells, 0);
    for (i = 0; i < cells.length - 1; i++)
      cells[i][0] = padString(cells[i][0], nameWidth);
    
    // determine number of displayed rows
    rows = cells.length - 1;
    if (getShowAverage())
      rows--;
    
    for (i = 0; i < rows; i++) {
      line   = "";
      colStr = "";

      for (n = 0; n < cells[i].length; n++) {
        // the header of the column
        if (isMean(n) || isRowName(n))
          colStr = cells[0][n];
        
        if ( (n > 1) && (!isSignificance(n)) )
          continue;
        
        // padding between cols 
        if (n > 0)
          line += " "; 
        // padding for "(" below dataset line
        if ( (i > 0) && (n > 1) )
          line += " ";
        
        if (i == 0) {
          line += colStr;
        }
        else {    
          if (n == 0) {
            line += cells[i][n];
          }
          else if (n == 1) {
            line += colStr.replaceAll(".", " ");   // base column has no significance!
          }
          else {
            line += cells[i][n];
            // add blanks dep. on length of #
            line += colStr.replaceAll(".", " ").substring(2);
          }
        }
      }
      result.append(line + "\n");
      
      // separator line
      if (i == 0)
        result.append(line.replaceAll(".", "-") + "\n");
    }
    
    return result.toString();
  }
  
  /**
   * returns the header of the matrix as a string
   * @see #m_HeaderKeys
   * @see #m_HeaderValues
   */
  public String toStringHeader() {
    return new ResultMatrixPlainText(this).toStringHeader();
  }

  /**
   * returns returns a key for all the col names, for better readability if
   * the names got cut off
   */
  public String toStringKey() {
    return new ResultMatrixPlainText(this).toStringKey();
  }

  /**
   * returns the summary as string
   */
  public String toStringSummary() {
    return new ResultMatrixPlainText(this).toStringSummary();
  }

  /**
   * returns the ranking in a string representation
   */
  public String toStringRanking() {
    return new ResultMatrixPlainText(this).toStringRanking();
  }

  /**
   * for testing only
   */
  public static void main(String[] args) {
    ResultMatrix        matrix;
    int                 i;
    int                 n;
    
    matrix = new ResultMatrixSignificance(3, 3);
    
    // set header
    matrix.addHeader("header1", "value1");
    matrix.addHeader("header2", "value2");
    matrix.addHeader("header2", "value3");
    
    // set values
    for (i = 0; i < matrix.getRowCount(); i++) {
      for (n = 0; n < matrix.getColCount(); n++) {
        matrix.setMean(n, i, (i+1)*n);
        matrix.setStdDev(n, i, ((double) (i+1)*n) / 100);
        if (i == n) {
          if (i % 2 == 1)
            matrix.setSignificance(n, i, SIGNIFICANCE_WIN);
          else
            matrix.setSignificance(n, i, SIGNIFICANCE_LOSS);
        }
      }
    }

    System.out.println("\n\n--> " + matrix.getDisplayName());
    
    System.out.println("\n1. complete\n");
    System.out.println(matrix.toStringHeader() + "\n");
    System.out.println(matrix.toStringMatrix() + "\n");
    System.out.println(matrix.toStringKey());
    
    System.out.println("\n2. complete with std deviations\n");
    matrix.setShowStdDev(true);
    System.out.println(matrix.toStringMatrix());
    
    System.out.println("\n3. cols numbered\n");
    matrix.setPrintColNames(false);
    System.out.println(matrix.toStringMatrix());
    
    System.out.println("\n4. second col missing\n");
    matrix.setColHidden(1, true);
    System.out.println(matrix.toStringMatrix());
    
    System.out.println("\n5. last row missing, rows numbered too\n");
    matrix.setRowHidden(2, true);
    matrix.setPrintRowNames(false);
    System.out.println(matrix.toStringMatrix());
    
    System.out.println("\n6. mean prec to 3\n");
    matrix.setMeanPrec(3);
    matrix.setPrintRowNames(false);
    System.out.println(matrix.toStringMatrix());
  }
}
