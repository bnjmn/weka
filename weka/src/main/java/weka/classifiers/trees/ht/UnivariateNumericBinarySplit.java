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
 *    UnivariateNumericBinarySplit.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.trees.ht;

import java.io.Serializable;

import weka.core.Attribute;
import weka.core.Instance;

/**
 * A binary split based on a single numeric attribute
 * 
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class UnivariateNumericBinarySplit extends Split implements Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -7392204582942741097L;

  /** The split point */
  protected double m_splitPoint;

  /**
   * Constructor
   * 
   * @param attName the name of the attribute to split on
   * @param splitPoint the split point
   */
  public UnivariateNumericBinarySplit(String attName, double splitPoint) {
    m_splitAttNames.add(attName);
    m_splitPoint = splitPoint;
  }

  @Override
  public String branchForInstance(Instance inst) {

    Attribute att = inst.dataset().attribute(m_splitAttNames.get(0));
    if (att == null || inst.isMissing(att)) {
      // TODO -------------
      return null;
    }

    if (inst.value(att) <= m_splitPoint) {
      return "left";
    }

    return "right";
  }

  @Override
  public String conditionForBranch(String branch) {
    String result = m_splitAttNames.get(0);

    if (branch.equalsIgnoreCase("left")) {
      result += " <= ";
    } else {
      result += " > ";
    }

    result += String.format("%-9.3f", m_splitPoint);

    return result;
  }
}
