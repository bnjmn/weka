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
 *    CentroidSketch.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.clusterers;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import weka.classifiers.rules.DecisionTableHashKey;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.NormalizableDistance;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.WeightedReservoirSample;

/**
 <!-- globalinfo-start -->
 * Class for managing a sketch of centres for k-means, along with a weighted reservoir sample that is used over iterations to update the sketch. Used in the implementation of the k-means|| initialization method. For more information, see<br/>
 * <br/>
 * Bahman Bahmani, Benjamin Moseley, Andrea Vattani, Ravi Kumar, Sergei Vassilvitskii (2012). Scalable k-means++. Proceedings of the VLDB Endowment.:622-633.
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;article{Bahmani2012,
 *    author = {Bahman Bahmani and Benjamin Moseley and Andrea Vattani and Ravi Kumar and Sergei Vassilvitskii},
 *    journal = {Proceedings of the VLDB Endowment},
 *    pages = {622-633},
 *    title = {Scalable k-means++},
 *    year = {2012}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class CentroidSketch implements TechnicalInformationHandler,
  Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 2075286053120786008L;

  /** Instances in the current sketch */
  protected Instances m_currentSketch;

  /** The reservoir sample */
  protected WeightedReservoirSample m_weightedCenterSample;

  /**
   * The distance function to use for setting the weight for an instance to be
   * added to the reservoir. The distance is the distance between the instance
   * to be added and the the closest point in the current sketch
   */
  protected NormalizableDistance m_distanceFunction;

  /** The size of the reservoir */
  protected int m_size = 100;

  /** The seed for random number generation */
  protected int m_seed = 1;
  
  /**
   * Constructor.
   * 
   * @param initialSketch the initial starting point (typically one randomly
   *          chosen instance for the k-means|| algorithm)
   * @param distanceFunction the distance function to use
   * @param size the size of the reservoir (i.e. how many points to consider
   *          adding to the sketch at each iteration)
   * @param seed the seed for random number generation
   */
  public CentroidSketch(Instances initialSketch,
    NormalizableDistance distanceFunction, int size, int seed) {
    m_currentSketch = initialSketch;
    m_distanceFunction = distanceFunction;
    m_seed = seed;
    m_size = size;

    m_weightedCenterSample = new WeightedReservoirSample(m_size, m_seed);
  }

  /**
   * Overview information for this class
   * 
   * @return overview help information
   */
  public String globalInfo() {
    return "Class for managing a sketch of centres for k-means, along with a weighted reservoir "
      + "sample that is used over iterations to update the sketch. Used in the implementation "
      + "of the k-means|| initialization method. For more information, see\n\n"
      + getTechnicalInformation().toString();
  }

  /**
   * Processes an instance - basically updates the reservoir
   * 
   * @param toProcess the instance to process
   * @param updateDistanceFunction true if we should update the distance
   *          function with this instance (i.e. update the ranges for numeric
   *          attributes)
   */
  public void process(Instance toProcess, boolean updateDistanceFunction) {
    if (updateDistanceFunction) {
      m_distanceFunction.update(toProcess);
    }
    m_weightedCenterSample.add(toProcess, distanceToSketch(toProcess));
  }

  /**
   * Computes the distance between the supplied instance and the current sketch.
   * This is the distance to the closest point in the sketch.
   * 
   * @param toProcess the instance to process
   * @return the distance to the current sketch
   */
  public double distanceToSketch(Instance toProcess) {
    // find the min distance to the sketch
    double dist = Double.MAX_VALUE;

    for (int i = 0; i < m_currentSketch.numInstances(); i++) {
      double d =
        m_distanceFunction.distance(toProcess, m_currentSketch.instance(i));
      if (d < dist) {
        dist = d;
      }
    }

    return dist;
  }

  /**
   * Get the distance function being used
   * 
   * @return the distance function
   */
  public NormalizableDistance getDistanceFunction() {
    return m_distanceFunction;
  }

  /**
   * Set the distance function to use
   * 
   * @param distFunc the distance function to use
   */
  public void setDistanceFunction(NormalizableDistance distFunc) {
    m_distanceFunction = distFunc;
  }

  /**
   * Get the reservoir sample
   * 
   * @return the reservoir sample
   */
  public WeightedReservoirSample getReservoirSample() {
    return m_weightedCenterSample;
  }

  /**
   * Get the current sketch as a set of instances
   * 
   * @return the current sketch
   */
  public Instances getCurrentSketch() {
    return m_currentSketch;
  }

  /**
   * Aggregate the supplied reservoir into our reservoir. Does not increase the
   * size of the sample.
   * 
   * @param toAggregate the reservoir sample to aggregate
   * @throws Exception if the structure of the instances in the sample to
   *           aggregate does not match the structure of our sketch
   */
  public void aggregateReservoir(WeightedReservoirSample toAggregate)
    throws Exception {
    if (toAggregate.getSample().size() > 0) {
      Instance structureCheck = toAggregate.getSample().peek().m_instance;

      if (!m_currentSketch.equalHeaders(structureCheck.dataset())) {
        throw new Exception(
          "Can't aggregate - instances structure is different: "
            + m_currentSketch.equalHeadersMsg(structureCheck.dataset()));
      }
    }

    m_weightedCenterSample.aggregate(toAggregate);
  }

  /**
   * Clear the reservoir
   */
  public void resetReservoir() {
    m_weightedCenterSample.reset();
  }

  /**
   * Add the reservoir to the current sketch. Also resets the reservoir.
   */
  public void addReservoirToCurrentSketch() throws Exception {
    Instances reservoir = m_weightedCenterSample.getSampleAsInstances();

    // whilst it is true that a single reservoir will
    // never contain a duplicate of an instance in the current
    // sketch (due to instances with weight/distance 0 being skipped),
    // it is possible that after aggregating reservoirs
    // the combined reservoir may contain duplicates - need to
    // use DecisionTableHashKey to ensure that we only add each
    // reservoir member to the sketch if it isn't already in there

    Map<DecisionTableHashKey, Integer> lookup =
      new HashMap<DecisionTableHashKey, Integer>();
    // fill lookup with current sketch instances
    for (int i = 0; i < m_currentSketch.numInstances(); i++) {
      Instance c = m_currentSketch.instance(i);
      DecisionTableHashKey hk =
        new DecisionTableHashKey(c, m_currentSketch.numAttributes(), true);
      lookup.put(hk, null);
    }

    for (int i = 0; i < reservoir.numInstances(); i++) {
      DecisionTableHashKey hk =
        new DecisionTableHashKey(reservoir.instance(i),
          reservoir.numAttributes(), true);

      if (!lookup.containsKey(hk)) {
        m_currentSketch.add(reservoir.instance(i));
        lookup.put(hk, null);
      }
    }

    resetReservoir();
  }

  @Override
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation result;

    result = new TechnicalInformation(Type.ARTICLE);
    result
      .setValue(
        Field.AUTHOR,
        "Bahman Bahmani and Benjamin Moseley and Andrea Vattani and Ravi Kumar and Sergei Vassilvitskii");
    result.setValue(Field.TITLE, "Scalable k-means++");
    result.setValue(Field.JOURNAL, "Proceedings of the VLDB Endowment");
    result.setValue(Field.YEAR, "2012");
    result.setValue(Field.PAGES, "622-633");

    return result;
  }
}

