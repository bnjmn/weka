/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    DistributionClusterer.java
 *    Copyright (C) 1999 Mark Hall
 *
 */

package weka.clusterers;

import weka.core.*;

/** 
 * Abstract clustering model that produces (for each test instance)
 * an estimate of the membership in each cluster 
 * (ie. a probability distribution).
 *
 * @author   Mark Hall (mhall@cs.waikato.ac.nz)
 * @version  $Revision: 1.10 $
 */
public abstract class DistributionClusterer extends Clusterer {

  // ===============
  // Public methods.
  // ===============

  /**
   * Computes the log of the density for a given instance.
   * 
   * @param instance the instance to compute the density for
   * @return the density.
   * @exception Exception if the density could not be computed
   * successfully
   */
  public abstract double logDensityForInstance(Instance instance) 
    throws Exception;

  /**
   * Predicts the cluster memberships for a given instance.
   *
   * @param instance the instance to be assigned a cluster.
   * @return an array containing the estimated membership 
   * probabilities of the test instance in each cluster (this 
   * should sum to at most 1)
   * @exception Exception if distribution could not be 
   * computed successfully
   */
  public abstract double[] distributionForInstance(Instance instance) 
       throws Exception;

  /**
   * Assigns an instance to a Cluster.
   *
   * @param instance the instance to be classified
   * @return the predicted most likely cluster for the instance. 
   * @exception Exception if an error occurred during the prediction
   */
  public int clusterInstance(Instance instance) throws Exception {
    double [] dist = distributionForInstance(instance);

    if (dist == null) {
      throw new Exception("Null distribution predicted");
    }

    if (Utils.sum(dist) <= 0) {
      throw new Exception("Unable to cluster instance");
    }
    return Utils.maxIndex(dist);
  }
}

