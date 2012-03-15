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
 *    ReferenceInstances.java
 *    Copyright (C) 2001 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.trees.adtree;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.RevisionUtils;

/**
 * Simple class that extends the Instances class making it possible to create
 * subsets of instances that reference their source set. Is used by ADTree to
 * make reweighting of instances easy to manage.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision$
 */
public class ReferenceInstances
  extends Instances {

  /** for serialization */
  private static final long serialVersionUID = -8022666381920252997L;

  /**
   * Creates an empty set of instances.
   *
   * @param dataset the instances to get the header information from
   * @param capacity the initial storage capacity of the set
   */
  public ReferenceInstances(Instances dataset, int capacity) {

    super(dataset, capacity);
  }

  /**
   * Adds one instance reference to the end of the set. 
   * Does not copy instance before it is added. Increases the
   * size of the dataset if it is not large enough. Does not
   * check if the instance is compatible with the dataset.
   *
   * @param instance the instance to be added
   */
  public final void addReference(Instance instance) {

    m_Instances.add(instance);
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }
}
