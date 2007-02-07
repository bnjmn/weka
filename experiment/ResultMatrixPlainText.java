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
 * ResultMatrixPlainText.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.experiment;

import weka.core.Utils;

/**
 * This matrix is a container for the datasets and classifier setups and 
 * their statistics. It outputs the matrix in plain text (columns).
 *
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.7 $
 */
public class ResultMatrixPlainText
  extends ResultMatrix {

  /** for serialization */
  private static final long serialVersionUID = 1502934525382357937L;

  /**
   * initializes the matrix as 1x1 matrix
   */
  public ResultMatrixPlainText() {
    this(1, 1);
  }

  /**
   * initializes the matrix with the given dimensions
   */
  public ResultMatrixPlainText(int cols, int rows) {
    super(cols, rows);
  }

  /**
   * initializes the matrix with the values from the given matrix
   * @param matrix      the matrix to get the values from
   */
  public ResultMatrixPlainText(ResultMatrix matrix) {
    super(matrix);
  }

  /**
   * returns the name of the output format
   */
  public String getDisplayName() {
    return "Plain Text";
  }

  /**
   * removes the stored data but retains the dimensions of the matrix
   */
  public void clear() {
    super.clear();
    setRowNameWidth(25);
    setCountWidth(5);
  }
  
  /**
   * returns the header of the matrix as a string
   * @see #m_HeaderKeys
   * @see #m_HeaderValues
   */
  public String toStringHeader() {
    int         i;
    int         size;
    String[][]  data;
    String      result;

    result = "";
    
    // fill in data
    data = new String[m_HeaderKeys.size()][2];
    for (i = 0; i < m_HeaderKeys.size(); i++) {
      data[i][0] = m_HeaderKeys.get(i).toString() + ":";
      data[i][1] = m_HeaderValues.get(i).toString();
    }

    // pad
    size = getColSize(data, 0);
    for (i = 0; i < data.length; i++)
      data[i][0] = padString(data[i][0], size);

    // build result
    for (i = 0; i < data.length; i++)
      result += data[i][0] + " " + data[i][1] + "\n";

    return result;
  }

  /**
   * returns the matrix as plain text
   */
  public String toStringMatrix() {
    StringBuffer    result;
    String[][]      cells;
    int             i;
    int             j;
    int             n;
    int             k;
    int             size;
    String          line;
    int             indexBase;
    int             indexSecond;
    StringBuffer    head;
    StringBuffer    body;
    StringBuffer    foot;
    int[]           startMeans;
    int[]           startSigs;
    int             maxLength;

    result     = new StringBuffer();
    head       = new StringBuffer();
    body       = new StringBuffer();
    foot       = new StringBuffer();
    cells      = toArray();
    startMeans = new int[getColCount()];
    startSigs  = new int[getColCount() - 1];
    maxLength  = 0;

    // pad numbers
    for (n = 1; n < cells[0].length; n++) {
      size = getColSize(cells, n, true, true);
      for (i = 1; i < cells.length - 1; i++)
        cells[i][n] = padString(cells[i][n], size, true);
    }

    // index of base column in array
    indexBase = 1;
    if (getShowStdDev())
      indexBase++;

    // index of second column in array
    indexSecond = indexBase + 1;
    if (getShowStdDev())
      indexSecond++;

    // output data (without "(v/ /*)")
    j = 0;
    k = 0;
    for (i = 1; i < cells.length - 1; i++) {
      if (isAverage(i))
        body.append(padString("", maxLength).replaceAll(".", "-") + "\n");
      line = "";
      
      for (n = 0; n < cells[0].length; n++) {
        // record starts
        if (i == 1) {
          if (isMean(n)) {
            startMeans[j] = line.length();
            j++;
          }

          if (isSignificance(n)) {
            startSigs[k] = line.length();
            k++;
          }
        }
        
        if (n == 0) {
          line += padString(cells[i][n], getRowNameWidth());
          if (!isAverage(i))
            line += padString("(" +
                Utils.doubleToString(getCount(getDisplayRow(i-1)), 0) + ")",
                getCountWidth(), true);
          else
            line += padString("", getCountWidth(), true);
        }
        else {
          // additional space before means
          if (isMean(n))
            line += "  ";

          // print cell
          if (getShowStdDev()) {
            if (isMean(n - 1)) {
              if (!cells[i][n].trim().equals(""))              
                line += "(" + cells[i][n] + ")";
              else
                line += " " + cells[i][n] + " ";
            }
            else
              line += " " + cells[i][n];
          }
          else {
            line += " " + cells[i][n];
          }
        }

        // add separator after base column
        if (n == indexBase)
          line += " |";
      }

      // record overall length
      if (i == 1)
        maxLength = line.length();
      
      body.append(line + "\n");
    }

    // column names
    line = padString(cells[0][0], startMeans[0]);
    i    = -1;
    for (n = 1; n < cells[0].length; n++) {
      if (isMean(n)) {
        i++;

        if (i == 0)
          line = padString(line, startMeans[i] - getCountWidth());
        else if (i == 1)
          line = padString(line, startMeans[i] - " |".length());
        else if (i > 1)
          line = padString(line, startMeans[i]);
        
        if (i == 1)
          line += " |";
        
        line += " " + cells[0][n];
      }
    }
    line = padString(line, maxLength);
    head.append(line + "\n");
    head.append(line.replaceAll(".", "-") + "\n");
    body.append(line.replaceAll(".", "-") + "\n");

    // output wins/losses/ties
    if (getColCount() > 1) {
      line = padString(cells[cells.length - 1][0], startMeans[1]-2, true) + " |";
      i    = 0;
      for (n = 1; n < cells[cells.length - 1].length; n++) {
        if (isSignificance(n)) {
          line = padString(
                  line, startSigs[i] + 1 - cells[cells.length - 1][n].length());
          line += " " + cells[cells.length - 1][n];
          i++;
        }
      }
      line = padString(line, maxLength);
    }
    else {
      line = padString(cells[cells.length - 1][0], line.length() - 2) + " |";
    }
    foot.append(line + "\n");
    
    // assemble output
    result.append(head.toString());
    result.append(body.toString());
    result.append(foot.toString());

    return result.toString();
  }

  /**
   * returns returns a key for all the col names, for better readability if
   * the names got cut off
   */
  public String toStringKey() {
    String          result;
    int             i;

    result = "Key:\n";
    for (i = 0; i < getColCount(); i++) {
      if (getColHidden(i))
        continue;

      result +=   LEFT_PARENTHESES + (i+1) + RIGHT_PARENTHESES 
                + " " + removeFilterName(m_ColNames[i]) + "\n";
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

    if (m_NonSigWins == null)
      return "-summary data not set-";
    
    result = "";
    titles = "";
    resultsetLength = 1 + Math.max((int)(Math.log(getColCount())/Math.log(10)),
                                   (int)(Math.log(getRowCount())/Math.log(10)));

    for (i = 0; i < getColCount(); i++) {
      if (getColHidden(i))
        continue;
      titles += " " + Utils.padLeft("" + getSummaryTitle(i),
				    resultsetLength * 2 + 3);
    }
    result += titles + "  (No. of datasets where [col] >> [row])\n";

    for (i = 0; i < getColCount(); i++) {
      if (getColHidden(i))
        continue;

      for (j = 0; j < getColCount(); j++) {
        if (getColHidden(j))
          continue;

        result += " ";
	if (j == i)
	  result += Utils.padLeft("-", resultsetLength * 2 + 3);
	else
	  result += Utils.padLeft("" + m_NonSigWins[i][j] 
                                  + " (" + m_Wins[i][j] + ")",
				  resultsetLength * 2 + 3);
      }

      result += " | " + getSummaryTitle(i) + " = " + getColName(i) + '\n';
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
    result =   Utils.padLeft(">-<", width) + ' '
             + Utils.padLeft(">", width) + ' '
             + Utils.padLeft("<", width) + " Resultset\n";

    ranking = Utils.sort(m_RankingDiff);

    for (i = getColCount() - 1; i >= 0; i--) {
      curr = ranking[i];

      if (getColHidden(curr))
        continue;

      result += Utils.padLeft("" + m_RankingDiff[curr], width) + ' '
        + Utils.padLeft("" + m_RankingWins[curr], width) + ' '
        + Utils.padLeft("" + m_RankingLosses[curr], width) + ' '
        + removeFilterName(m_ColNames[curr]) + '\n';
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
    
    matrix = new ResultMatrixPlainText(3, 3);
    
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
