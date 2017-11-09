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
 * SubsetByExpression.java
 * Copyright (C) 2008-2013 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.instance;

import weka.core.*;
import weka.core.Capabilities.Capability;
import weka.filters.SimpleBatchFilter;
import weka.classifiers.rules.DecisionTableHashKey;

import java.util.HashSet;

/**
 <!-- globalinfo-start -->
 * Removes all duplicate instances from the first batch of data it receives.
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -D
 *  Turns on output of debugging information.</pre>
 * 
 <!-- options-end -->
 *
 * @author  Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 9804 $
 */
public class RemoveDuplicates extends SimpleBatchFilter implements WeightedAttributesHandler, WeightedInstancesHandler{

  /** for serialization. */
  private static final long serialVersionUID = 4518686110979589602L;
  
  /**
   * Returns a string describing this filter.
   *
   * @return 		a description of the filter suitable for
   * 			displaying in the explorer/experimenter gui
   */
  @Override
  public String globalInfo() {
    return "Removes all duplicate instances from the first batch of data it receives.";
  }

  /**
   * Input an instance for filtering. Filter requires all
   * training instances be read before producing output (calling the method
   * batchFinished() makes the data available). If this instance is part of
   * a new batch, m_NewBatch is set to false.
   *
   * @param instance    the input instance
   * @return            true if the filtered instance may now be
   *                    collected with output().
   * @throws  IllegalStateException if no input structure has been defined
   * @throws Exception  if something goes wrong
   * @see               #batchFinished()
   */
  @Override
  public boolean input(Instance instance) throws Exception {
    if (getInputFormat() == null)
      throw new IllegalStateException("No input instance format defined");
    
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }

    if (isFirstBatchDone()) {
      push(instance);
      return true;
    } else {
      bufferInput(instance);
      return false;
    }
  }

  /** 
   * Returns the Capabilities of this filter.
   *
   * @return            the capabilities of this object
   * @see               Capabilities
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enable(Capability.STRING_ATTRIBUTES);
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);
    
    // class
    result.enable(Capability.STRING_CLASS);
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.NUMERIC_CLASS);
    result.enable(Capability.DATE_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);
    result.enable(Capability.NO_CLASS);
    
    return result;
  }

  /**
   * Determines the output format based on the input format and returns 
   * this.
   *
   * @param inputFormat     the input format to base the output format on
   * @return                the output format
   * @throws Exception      in case the determination goes wrong
   */
  @Override
  protected Instances determineOutputFormat(Instances inputFormat)
      throws Exception {
    
    return new Instances(inputFormat, 0);
  }

  /**
   * returns true if the output format is immediately available after the
   * input format has been set and not only after all the data has been
   * seen (see batchFinished())
   *
   * @return      true if the output format is immediately available
   * @see         #batchFinished()
   * @see         #setInputFormat(Instances)
   */
  protected boolean hasImmediateOutputFormat() {
    
    return true;
  }

  /**
   * Processes the given data (may change the provided dataset) and returns
   * the modified version. This method is called in batchFinished().
   *
   * @param instances   the data to process
   * @return            the modified data
   * @throws Exception  in case the processing goes wrong
   * @see               #batchFinished()
   */
  @Override
  protected Instances process(Instances instances) throws Exception {
    
    if (!isFirstBatchDone()) {
      HashSet<DecisionTableHashKey> hs = new HashSet<DecisionTableHashKey>();
      Instances newInstances = new Instances(instances, instances.numInstances());
      for (Instance inst : instances) {
        DecisionTableHashKey key = new DecisionTableHashKey(inst,
           instances.numAttributes(), true);
        if (hs.add(key)) {
          newInstances.add(inst);
        }
      }
      newInstances.compactify();
      return newInstances;
    }
    throw new Exception("The process method should never be called for subsequent batches.");
  }

  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 9804 $");
  }

  /**
   * Main method for running this filter.
   *
   * @param args 	arguments for the filter: use -h for help
   */
  public static void main(String[] args) {
    runFilter(new RemoveDuplicates(), args);
  }
}

