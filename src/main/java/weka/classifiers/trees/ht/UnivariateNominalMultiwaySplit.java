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
 *    UnivariateNominalMultiwaySplit.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.trees.ht;

import java.io.Serializable;

import weka.core.Attribute;
import weka.core.Instance;

/**
 * A multiway split based on a single nominal attribute
 * 
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class UnivariateNominalMultiwaySplit extends Split implements
    Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -9094590488097956665L;

  /**
   * Constructor
   * 
   * @param attName the name of the attribute to split on
   */
  public UnivariateNominalMultiwaySplit(String attName) {
    m_splitAttNames.add(attName);
  }

  @Override
  public String branchForInstance(Instance inst) {
    Attribute att = inst.dataset().attribute(m_splitAttNames.get(0));
    if (att == null || inst.isMissing(att)) {
      return null;
    }
    return att.value((int) inst.value(att));
  }

  @Override
  public String conditionForBranch(String branch) {
    return m_splitAttNames.get(0) + " = " + branch;
  }
}
