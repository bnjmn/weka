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
 *    InactiveHNode.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.trees.ht;

import java.io.Serializable;
import java.util.Map;

import weka.core.Instance;

/**
 * Class implementing an inactive node (i.e. one that does not allow growth)
 * 
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class InactiveHNode extends LeafNode implements LearningNode,
    Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -8747567733141700911L;

  /**
   * Constructor
   * 
   * @param classDistrib the class distribution at this node
   */
  public InactiveHNode(Map<String, WeightMass> classDistrib) {
    m_classDistribution = classDistrib;
  }

  @Override
  public void updateNode(Instance inst) {
    super.updateDistribution(inst);
  }
}
