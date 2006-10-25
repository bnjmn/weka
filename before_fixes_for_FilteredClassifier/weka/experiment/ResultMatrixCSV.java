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
 * ResultMatrixCSV.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */


package weka.experiment;

import weka.core.Utils;

/**
 * This matrix is a container for the datasets and classifier setups and 
 * their statistics. It outputs the data in CSV format.
 *
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.2 $
 */

public class ResultMatrixCSV extends ResultMatrix {
  /**
   * initializes the matrix as 1x1 matrix
   */
  public ResultMatrixCSV() {
    this(1, 1);
  }

  /**
   * initializes the matrix with the given dimensions
   */
  public ResultMatrixCSV(int cols, int rows) {
    super(cols, rows);
  }

  /**
   * initializes the matrix with the values from the given matrix
   * @param matrix      the matrix to get the values from
   */
  public ResultMatrixCSV(ResultMatrix matrix) {
    super(matrix);
  }

  /**
   * returns the name of the output format
   */
  public String getDisplayName() {
    return "CSV";
  }

  /**
   * removes the stored data but retains the dimensions of the matrix
   */
  public void clear() {
    super.clear();
    setRowNameWidth(25);
    setPrintColNames(false);
    setEnumerateColNames(true);
    LEFT_PARENTHESES = "[";
    RIGHT_PARENTHESES = "]";
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
   * returns the matrix in CSV format
   */
  public String toStringMatrix() {
    StringBuffer        result;
    String[][]          cells;
    int                 i;
    int                 n;

    result = new StringBuffer();
    cells  = toArray();

    for (i = 0; i < cells.length; i++) {
      for (n = 0; n < cells[i].length; n++) {
        if (n > 0)
          result.append(",");
        result.append(Utils.quote(cells[i][n]));
      }
      result.append("\n");
    }
    
    return result.toString();
  }

  /**
   * returns returns a key for all the col names, for better readability if
   * the names got cut off
   */
  public String toStringKey() {
    String          result;
    int             i;

    result = "Key,\n";
    for (i = 0; i < getColCount(); i++) {
      if (getColHidden(i))
        continue;

      result +=   LEFT_PARENTHESES + (i+1) + RIGHT_PARENTHESES
                + "," + Utils.quote(removeFilterName(m_ColNames[i])) + "\n";
    }

    return result;
  }

  /**
   * returns the summary as string
   */
  public String toStringSummary() {
    String      result;
    String      titles;
    int         resultsetLength;
    int         i;
    int         j;
    String      line;

    if (m_NonSigWins == null)
      return "-summary data not set-";
    
    result = "";
    titles = "";
    resultsetLength = 1 + Math.max((int)(Math.log(getColCount())/Math.log(10)),
                                   (int)(Math.log(getRowCount())/Math.log(10)));

    for (i = 0; i < getColCount(); i++) {
      if (getColHidden(i))
        continue;
      if (!titles.equals(""))
        titles += ",";
      titles += getSummaryTitle(i);
    }
    result += titles + ",'(No. of datasets where [col] >> [row])'\n";

    for (i = 0; i < getColCount(); i++) {
      if (getColHidden(i))
        continue;

      line = "";
      for (j = 0; j < getColCount(); j++) {
        if (getColHidden(j))
          continue;

        if (!line.equals(""))
          line += ",";

	if (j == i)
	  line += "-";
	else
	  line += m_NonSigWins[i][j] 
                    + " (" + m_Wins[i][j] + ")";
      }

      result += line + "," + getSummaryTitle(i) + " = " + removeFilterName(m_ColNames[i]) + '\n';
    }

    return result;
  }

  /**
   * returns the ranking in a string representation
   */
  public String toStringRanking() {
    int           biggest;
    int           width;
    String        result;
    int[]         ranking;
    int           i;
    int           curr;

    if (m_RankingWins == null)
      return "-ranking data not set-";

    biggest = Math.max(m_RankingWins[Utils.maxIndex(m_RankingWins)],
                       m_RankingLosses[Utils.maxIndex(m_RankingLosses)]);
    width = Math.max(2 + (int)(Math.log(biggest) / Math.log(10)),
			 ">-<".length());
    result = ">-<,>,<,Resultset\n";

    ranking = Utils.sort(m_RankingDiff);

    for (i = getColCount() - 1; i >= 0; i--) {
      curr = ranking[i];

      if (getColHidden(curr))
        continue;

      result += m_RankingDiff[curr] + ","
        + m_RankingWins[curr] + ","
        + m_RankingLosses[curr] + ","
        + removeFilterName(m_ColNames[curr]) + "\n";
    }

    return result;
  }

  /**
   * for testing only
   */
  public static void main(String[] args) {
    ResultMatrix        matrix;
    int                 i;
    int                 n;
    
    matrix = new ResultMatrixCSV(3, 3);

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
