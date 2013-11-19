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
 * PruneCAR.java
 * Copyright (C) 2004 Stefan Mutter
 *
 */

package weka.associations.classification;

import java.io.Serializable;
import java.util.ArrayList;

import weka.core.FastVector;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.Utils;

/**
 * Abstract scheme for storing and pruning class associations. All schemes for
 * storing and pruning class associations implemement this class
 * 
 * 
 * @author Stefan Mutter (mutter@cs.waikato.ac.nz)
 * @version $Revision$
 */

public abstract class PruneCAR implements OptionHandler, Serializable {

  /**
   * Tests if there are rules
   * 
   * @return true if there is no rule, false otherwise
   */
  public abstract boolean isEmpty();

  /**
   * Sets the instances without the class attribute
   * 
   * @param instances the instances
   */
  public abstract void setInstancesNoClass(Instances instances);

  /**
   * Sets the instances where all attributes except for the class attribute are
   * deleted
   * 
   * @param instances the instances
   */
  public abstract void setInstancesOnlyClass(Instances instances);

  /**
   * Preprocesses rules before inserting them into the structure
   * 
   * @param premises the premises
   * @param consequences the consequences
   * @param confidences the interestingness measures
   * @throws Exception throws eception if preprocessing is not possible
   */
  public abstract void preprocess(ArrayList<Object> premises,
    ArrayList<Object> consequences, ArrayList<Object> confidences)
    throws Exception;

  /**
   * Inserts a consequence and the according interestingness measures into a
   * node
   * 
   * @param node the node
   * @param input the consequence and the interestingness measures
   */
  public abstract void insertContent(CrNode node, FastVector input);

  /**
   * Deletes a consequence from a node
   * 
   * @param node the node
   * @param index the index of the consequence
   */
  public abstract void deleteContent(CrNode node, int index);

  /**
   * FastVector defining additional pruning criteria
   * 
   * @param input the criteria
   * @return FastVector
   */
  public abstract FastVector pruningCriterions(FastVector input);

  /**
   * Pruning step before a rule is inserted into the structure
   * 
   * @param prem the premise
   * 
   * @param cons the consequence
   */
  public abstract void pruneBeforeInsertion(FastVector prem, FastVector cons);

  /**
   * Prunes rules out of tree
   */
  public abstract void prune();

  /**
   * Gets the number of pruned rules.
   * 
   * @return the number of pruned rules
   */
  public abstract int prunedRules();

  /**
   * Outputs the rules
   * 
   * @param metricType the metrci type of the sort order
   * @return a string
   */
  public abstract String toString(String metricType);

  /**
   * Creates a new instance of a PruneCAR given it's class name and (optional)
   * arguments to pass to it's setOptions method. If the associator implements
   * OptionHandler and the options parameter is non-null, the associator will
   * have it's options set.
   * 
   * @param pruningName the fully qualified class name of the PruneCAR
   * @param options an array of options suitable for passing to setOptions. May
   *          be null.
   * @return the newly created PruneCAR, ready for use.
   * @exception Exception if the PruneC>R name is invalid, or the options
   *              supplied are not acceptable to the PruneCAR
   */
  public static PruneCAR forName(String pruningName, String[] options)
    throws Exception {

    return (PruneCAR) Utils.forName(PruneCAR.class, pruningName, options);
  }

}
