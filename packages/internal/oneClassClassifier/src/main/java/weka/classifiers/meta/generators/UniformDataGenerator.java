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
 *   UniformDataGenerator.java
 *   Copyright (C) 2008 K.Hempstalk, University of Waikato, Hamilton, New Zealand.
 */

package weka.classifiers.meta.generators;

/**
 * <!-- globalinfo-start --> A uniform artificial data generator.<br/>
 * <br/>
 * This generator uses a uniform data model - all values have the same
 * probability, and generated values must fall within the range given to the
 * generator.
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
 * <pre>
 * -L &lt;num&gt;
 *  Sets the lower range of the generator
 *  (default: 0)
 * </pre>
 * 
 * <pre>
 * -U &lt;num&gt;
 *  Sets the upper range of the generator
 *  (default: 1)
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Kathryn Hempstalk (kah18 at cs.waikato.ac.nz)
 * @version $Revision$
 */
public class UniformDataGenerator extends RandomizableRangedGenerator implements
  NumericAttributeGenerator {

  /** for serialization. */
  private static final long serialVersionUID = -6390354660638644832L;

  /**
   * Returns a string describing this class' ability.
   * 
   * @return A description of the class.
   */
  @Override
  public String globalInfo() {
    return "A uniform artificial data generator.\n" + "\n"
      + "This generator uses a uniform data model - all values have "
      + "the same probability, and generated values must fall within "
      + "the range given to the generator.";
  }

  /**
   * Generates a value that falls under this distribution.
   * 
   * @return A generated value.
   */
  @Override
  public double generate() {
    double range = (m_UpperRange - m_LowerRange);
    return (m_Random.nextDouble() * range) + m_LowerRange;
  }

  /**
   * Gets the probability that a value falls under this distribution.
   * 
   * 
   * @param somedata The value to get the probability of.
   * @return The probability of the given value.
   */
  @Override
  public double getProbabilityOf(double somedata) {
    double range = (m_UpperRange - m_LowerRange);
    if (range <= 0 || somedata > m_UpperRange || somedata < m_LowerRange) {
      return Double.MIN_VALUE;
    }

    return 1 / (range);
  }

  /**
   * Gets the (natural) log of the probability of a given value.
   * 
   * @param somedata The value to get the log probability of.
   * @return The (natural) log of the probability.
   */
  @Override
  public double getLogProbabilityOf(double somedata) {
    double range = (m_UpperRange - m_LowerRange);
    if ((range <= 0)
      || (((somedata < m_LowerRange) || (somedata > m_UpperRange)))) {
      return Math.log(Double.MIN_VALUE);
    }
    return -Math.log(range);
  }
}
