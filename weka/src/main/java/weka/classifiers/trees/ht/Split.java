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
 *    Split.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.trees.ht;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import weka.core.Instance;

import java.io.Serializable;

/**
 * Base class for different split types
 * 
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public abstract class Split implements Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 5390368487675958092L;

  /** name(s) of attribute(s) involved in the split */
  protected List<String> m_splitAttNames = new ArrayList<String>();

  /**
   * Returns the name of the branch that the supplied instance would go down
   * 
   * @param inst the instance to find the branch for
   * @return the name of the branch that the instance would go down
   */
  public abstract String branchForInstance(Instance inst);

  /**
   * Returns the condition for the supplied branch name
   * 
   * @param branch the name of the branch to get the condition for
   * @return the condition (test) that corresponds to the named branch
   */
  public abstract String conditionForBranch(String branch);

  public List<String> splitAttributes() {
    return m_splitAttNames;
  }
}
