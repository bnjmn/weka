/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    PairedStatsCorrected.java
 *    Copyright (C) 2003-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.experiment;

import weka.core.RevisionUtils;
import weka.core.Statistics;
import weka.core.Utils;

/**
 * A class for storing stats on a paired comparison. This version is
 * based on the corrected resampled t-test statistic, which uses the
 * ratio of the number of test examples/the number of training examples.<p>
 *
 * For more information see:<p>
 *
 * Claude Nadeau and Yoshua Bengio, "Inference for the Generalization Error,"
 * Machine Learning, 2001.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision$
 */
public class PairedStatsCorrected
  extends PairedStats {

  /** The ratio used to correct the significance test */
  protected double m_testTrainRatio;

  /**
   * Creates a new PairedStatsCorrected object with the supplied
   * significance level and train/test ratio.
   *
   * @param sig the significance level for comparisons
   * @param testTrainRatio the number test examples/training examples
   */
  public PairedStatsCorrected(double sig, double testTrainRatio) {
      
    super(sig);
    m_testTrainRatio = testTrainRatio;
  }

  /**
   * Calculates the derived statistics (significance etc).
   */
  public void calculateDerived() {

    xStats.calculateDerived();
    yStats.calculateDerived();
    differencesStats.calculateDerived();

    correlation = Double.NaN;
    if (!Double.isNaN(xStats.stdDev) && !Double.isNaN(yStats.stdDev)
            && (xStats.stdDev > 0) && (yStats.stdDev > 0) && (count > 1)) {
      correlation = (xySum - xStats.sum * yStats.sum / count)
              / ((count - 1) * xStats.stdDev * yStats.stdDev);
    }

    if (differencesStats.stdDev > 0) {

      double tval = differencesStats.mean
              / Math.sqrt((1 / count + m_testTrainRatio)
              * differencesStats.stdDev * differencesStats.stdDev);

      if (count > 1) {
        differencesProbability = Statistics.FProbability(tval * tval, 1,
                (int) count - 1);
      } else differencesProbability = 1;
    } else {
      if (differencesStats.sumSq == 0) {
        differencesProbability = 1.0;
      } else {
        differencesProbability = 0.0;
      }
    }
    differencesSignificance = 0;
    if (differencesProbability <= sigLevel) {
      if (xStats.mean > yStats.mean) {
        differencesSignificance = 1;
      } else {
        differencesSignificance = -1;
      }
    }
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  /**
   * Tests the paired stats object from the command line.
   * reads line from stdin, expecting two values per line.
   *
   * @param args ignored.
   */
  public static void main(String [] args) {

    try {
      PairedStatsCorrected ps = new PairedStatsCorrected(0.05, 1.0 / 9.0);
      java.io.LineNumberReader r = new java.io.LineNumberReader(
              new java.io.InputStreamReader(System.in));
      String line;
      while ((line = r.readLine()) != null) {
        line = line.trim();
        if (line.equals("") || line.startsWith("@") || line.startsWith("%")) {
          continue;
        }
        java.util.StringTokenizer s
                = new java.util.StringTokenizer(line, " ,\t\n\r\f");
        int count = 0;
        double v1 = 0, v2 = 0;
        while (s.hasMoreTokens()) {
          double val = (new Double(s.nextToken())).doubleValue();
          if (count == 0) {
            v1 = val;
          } else if (count == 1) {
            v2 = val;
          } else {
            System.err.println("MSG: Too many values in line \""
                    + line + "\", skipped.");
            break;
          }
          count++;
        }
        if (count == 2) {
          ps.add(v1, v2);
        }
      }
      ps.calculateDerived();
      System.err.println(ps);
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }}
