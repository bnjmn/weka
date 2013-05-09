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
 *    SplitCandidate.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.trees.ht;

import java.util.List;
import java.util.Map;

/**
 * Encapsulates a candidate split
 * 
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class SplitCandidate implements Comparable<SplitCandidate> {

  public Split m_splitTest;

  /**
   * list of class distributions resulting from a split - 2 entries in the outer
   * list for numeric splits and n for nominal splits
   */
  public List<Map<String, WeightMass>> m_postSplitClassDistributions;

  /** The merit of the split */
  public double m_splitMerit;

  /**
   * Constructor
   * 
   * @param splitTest the splitting test
   * @param postSplitDists the distributions resulting from the split
   * @param merit the merit of the split
   */
  public SplitCandidate(Split splitTest,
      List<Map<String, WeightMass>> postSplitDists, double merit) {
    m_splitTest = splitTest;
    m_postSplitClassDistributions = postSplitDists;
    m_splitMerit = merit;
  }

  /**
   * Number of branches resulting from the split
   * 
   * @return the number of subsets of instances resulting from the split
   */
  public int numSplits() {
    return m_postSplitClassDistributions.size();
  }

  @Override
  public int compareTo(SplitCandidate comp) {
    return Double.compare(m_splitMerit, comp.m_splitMerit);
  }
}
