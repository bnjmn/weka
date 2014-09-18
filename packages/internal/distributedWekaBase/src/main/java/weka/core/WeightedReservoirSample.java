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
 *    WeigtedReservoirSample.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import java.io.Serializable;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * Class implementing weighted reservoir sampling. Can also do unweighted
 * reservoir sampling too if the supplied weights are all 1. Is based on the
 * idea that one way of implementing reservoir sampling is to just generate a
 * random number (between 0 and 1) for each data point and keep the n points
 * with the highest values.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class WeightedReservoirSample implements
  Aggregateable<WeightedReservoirSample>, Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 6206527064021195606L;

  /** The size of the reservoir */
  protected int m_sampleSize = 100;

  /** The random seed to use */
  protected int m_seed;

  /** For generating random numbers */
  protected Random m_random;

  /** Holds the actual reservoir */
  protected PriorityQueue<InstanceHolder> m_sample;

  /**
   * Constructor
   * 
   * @param size the size of the reservoir to use
   * @param seed the seed for random number generation
   */
  public WeightedReservoirSample(int size, int seed) {
    m_sampleSize = size;
    m_seed = seed;

    m_sample =
      new PriorityQueue<InstanceHolder>(m_sampleSize,
        new InstanceHolderComparator());

    m_random = new Random(m_seed);
    for (int i = 0; i < 100; i++) {
      m_random.nextDouble();
    }
  }

  /**
   * (Potentially) add an instance to the reservoir
   * 
   * @param toAdd the instance to add
   * @param weight the weight of the instance (use 1 for all instances for
   *          unweighted sampling)
   */
  public void add(Instance toAdd, double weight) {
    if (weight > 0) {
      add(toAdd, m_random.nextDouble(), weight);
    }
  }

  /**
   * (Potentially) add an instance to the reservoir
   * 
   * @param toAdd the instance to add
   * @param r the random number to use when setting the weight (between 0 and 1)
   * @param weight the weight of the instance to add
   */
  protected void add(Instance toAdd, double r, double weight) {
    double weightToUse = Math.pow(r, 1.0 / weight);

    boolean addIt =
      m_sample.size() < m_sampleSize
        || m_sample.peek().m_weight < weightToUse;
    if (addIt) {
      m_sample.add(new InstanceHolder(toAdd, weightToUse));
      if (m_sample.size() > m_sampleSize) {
        m_sample.poll();
      }
    }
  }

  @Override
  public WeightedReservoirSample aggregate(WeightedReservoirSample toAggregate)
    throws Exception {
    PriorityQueue<InstanceHolder> toAgg = toAggregate.getSample();

    for (InstanceHolder i : toAgg) {
      boolean addIt =
        m_sample.size() < m_sampleSize || m_sample.peek().m_weight < i.m_weight;

      if (addIt) {
        m_sample.add(i);

        if (m_sample.size() > m_sampleSize) {
          m_sample.poll();
        }
      }
    }
    return this;
  }

  @Override
  public void finalizeAggregation() throws Exception {
    // nothing to do
  }

  /**
   * Get the sample
   * 
   * @return the priority queue that holds the sample
   */
  public PriorityQueue<InstanceHolder> getSample() {
    return m_sample;
  }

  /**
   * Get the current sample as a set of unweighted instances
   * 
   * @return the current sample as a set of unweighted instances
   * @throws Exception if we haven't seen any instances yet
   */
  public Instances getSampleAsInstances() throws Exception {
    if (m_sample.size() == 0) {
      throw new Exception("Can't get the sample as a set of Instnaces because "
        + "we haven't seen any instances yet!");
    }

    Instances insts =
      new Instances(m_sample.peek().m_instance.dataset(), m_sampleSize);

    for (InstanceHolder i : m_sample) {
      // no need to copy here as add() does a shallow copy
      insts.add(i.m_instance);
    }

    insts.compactify();
    return insts;
  }

  /**
   * Get the current sample as a set of weighted instances
   * 
   * @return the current sample as a set of weighted instances
   * @throws Exception if we haven't seen any instances yet
   */
  public Instances getSampleAsWeightedInstances() throws Exception {
    if (m_sample.size() == 0) {
      throw new Exception(
        "Can't get the sample as a set of weighted Instnaces because "
          + "we haven't seen any instances yet!");
    }

    Instances insts =
      new Instances(m_sample.peek().m_instance.dataset(), m_sampleSize);

    for (InstanceHolder i : m_sample) {
      // copy here as we are setting the weight
      Instance toAdd = (Instance) i.m_instance.copy();
      toAdd.setWeight(i.m_weight);
      insts.add(toAdd);
    }

    insts.compactify();
    return insts;
  }

  /**
   * Reset (clear) the reservoir
   */
  public void reset() {
    m_sample.clear();
  }

  /**
   * Comparator for InstanceHolder
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  public static class InstanceHolderComparator implements
    Comparator<InstanceHolder>, Serializable {

    /** For serialization */
    private static final long serialVersionUID = 9000229069919615672L;

    @Override
    public int compare(InstanceHolder one, InstanceHolder two) {
      if (one.m_weight < two.m_weight) {
        return -1;
      } else if (one.m_weight > two.m_weight) {
        return 1;
      } else {
        return 0;
      }
    }
  }

  /**
   * Small inner class to hold an instance an its weight.
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  public static class InstanceHolder implements Serializable {

    /**
     * For serialization
     */
    private static final long serialVersionUID = 5822967369845371020L;

    public Instance m_instance;
    public double m_weight;

    public InstanceHolder(Instance instance, double weight) {
      m_instance = instance;
      m_weight = weight;
    }
  }
}
