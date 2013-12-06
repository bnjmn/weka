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

/**
 *   NominalGenerator.java
 *   Copyright (C) 2008 K.Hempstalk, University of Waikato, Hamilton, New Zealand.
 */

package weka.classifiers.meta.generators;

import java.util.Enumeration;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

/**
 * <!-- globalinfo-start --> A generator for nominal attributes.<br/>
 * <br/>
 * Generates artificial data for nominal attributes. Each attribute value is
 * considered to be possible, i.e. the probability of any value is always
 * non-zero.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -S &lt;seed&gt;
 *  Sets the seed of the random number generator of the generator (default: 1)
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Kathryn Hempstalk (kah18 at cs.waikato.ac.nz)
 * @version $Revision$
 */
public class NominalGenerator extends RandomizableGenerator implements
  NominalAttributeGenerator {

  /** for serialization. */
  private static final long serialVersionUID = 5254947213887016283L;

  /**
   * Counts (turned into probabilities) of each attribute value.
   */
  protected double[] m_AttCounts;

  /**
   * Returns a string describing this class' ability.
   * 
   * @return A description of the class.
   */
  @Override
  public String globalInfo() {
    return "A generator for nominal attributes.\n"
      + "\n"
      + "Generates artificial data for nominal attributes.  Each attribute value "
      + "is considered to be possible, i.e. the probability of any value is "
      + "always non-zero.";
  }

  /**
   * Sets up the generator with the counts required for generation.
   * 
   * @param someinstances The instances to count up.
   * @param att The attribute to count up with.
   */
  @Override
  public void buildGenerator(Instances someinstances, Attribute att) {
    m_AttCounts = new double[att.numValues()];
    for (int i = 0; i < m_AttCounts.length; i++) {
      m_AttCounts[i] = 1;
    }

    // count up the number of each instance
    Enumeration<Instance> instancesEnum = someinstances.enumerateInstances();
    int totalCounts = m_AttCounts.length;
    while (instancesEnum.hasMoreElements()) {
      Instance aninst = instancesEnum.nextElement();
      if (!aninst.isMissing(att)) {
        m_AttCounts[(int) aninst.value(att)] += 1;
        totalCounts++;
      }
    }

    // calculate the probability of each.
    for (int i = 0; i < m_AttCounts.length; i++) {
      m_AttCounts[i] /= totalCounts;
    }
  }

  /**
   * Generates an index of a nominal attribute as artificial data.
   * 
   * @return The index of the nominal attribute's value.
   */
  @Override
  public double generate() {
    double prob = m_Random.nextDouble();
    // find the index of the attribute value with this position
    double probSoFar = 0;
    for (int i = 0; i < m_AttCounts.length; i++) {
      probSoFar += m_AttCounts[i];
      if (prob <= probSoFar) {
        return i;
      }
    }
    return 0;
  }

  /**
   * Gets the probability of a given attribute value (provided as an index).
   * 
   * @param valuex The index to the attribute value.
   * @return The probability of this value.
   */
  @Override
  public double getProbabilityOf(double valuex) {
    return m_AttCounts[(int) valuex];
  }

  /**
   * Gets the (natural) log of the probability of a given value.
   * 
   * @param valuex The index of the nominal value.
   * @return The natural log of the probability of valuex.
   */
  @Override
  public double getLogProbabilityOf(double valuex) {
    return Math.log(this.getProbabilityOf(valuex));
  }
}
