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
 *    IterativeClassifier.java
 *    Copyright (C) 2001-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers;

import weka.core.Instances;

/**
 * Interface for classifiers that can induce models of growing complexity one
 * step at a time.
 *
 * @author Gabi Schmidberger (gabi@cs.waikato.ac.nz)
 * @author Bernhard Pfahringer (bernhard@cs.waikato.ac.nz)
 * @version $Revision$
 */

public interface IterativeClassifier extends Classifier {

  /**
   * Initializes an iterative classifier.
   *
   * @param instances the instances to be used in induction
   * @exception Exception if the model cannot be initialized
   */
  void initializeClassifier(Instances instances) throws Exception;

  /**
   * If called with argument true then the classifier will be able to be trained
   * further (with more iterations) at a later date. Note that this can
   * potentially make serialized model structures larger, because certain data
   * and data structures might need to be retained to facilitate continued
   * training. When set to false, and done() is called, then the classifier will
   * be cleaned up, resulting in (potentially) a more compact serialized model.
   *
   * @param resume true to enable training to continue at a later point after
   *          the initial model is built.
   * @throws Exception if finalization cannot be set
   */
  void setResume(boolean resume) throws Exception;

  /**
   * Returns true if the classifier will be able to be trained further (with
   * more iterations) at a later date. Note that this can potentially make
   * serialized model structures larger, because certain data and data
   * structures might need to be retained to facilitate continued training. When
   * set to false, and done() is called, then the classifier will be cleaned up,
   * resulting in (potentially) a more compact serialized model.
   *
   * @return true to enable training to continue at a later point after
   *          the initial model is built.
   * @throws Exception if finalization cannot be set
   */
  boolean getResume();

  /**
   * Performs one iteration.
   *
   * @return false if no further iterations could be performed, true otherwise
   * @exception Exception if this iteration fails for unexpected reasons
   */
  boolean next() throws Exception;

  /**
   * Signal end of iterating, for either the time being or permanently if
   * setFinalized(true) has been called. Useful for any house-keeping/cleanup.
   *
   * @exception Exception if cleanup fails
   */
  void done() throws Exception;
}
