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

/**
 *   GaussianGenerator.java
 *   Copyright (C) 2008 K.Hempstalk, University of Waikato, Hamilton, New Zealand.
 */

package weka.classifiers.meta.generators;

/**
 <!-- globalinfo-start -->
 * An artificial data generator that uses a single Gaussian distribution.<br/>
 * <br/>
 * If a mixture of Gaussians is required, use the EM Generator.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -D
 *  If set, generator is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -S &lt;seed&gt;
 *  Sets the seed of the random number generator of the generator (default: 1)</pre>
 * 
 * <pre> -M &lt;num&gt;
 *  Sets the mean of the generator
 *  (default: 0)</pre>
 * 
 * <pre> -SD &lt;num&gt;
 *  Sets the standard deviation of the generator
 *  (default: 1)</pre>
 * 
 <!-- options-end -->
 *
 * @author Kathryn Hempstalk (kah18 at cs.waikato.ac.nz)
 * @version $Revision$
 * @see EMGenerator
 */
public class GaussianGenerator
    extends RandomizableDistributionGenerator
    implements NumericAttributeGenerator {

  /** for serialization. */
  private static final long serialVersionUID = 4860675869078046797L;

  /**
   * Returns a string describing this class' ability.
   *
   * @return A description of the class.
   */
  public String globalInfo() {
    return 
        "An artificial data generator that uses a single Gaussian distribution.\n"
      + "\n"
      + "If a mixture of Gaussians is required, use the EM Generator.";
  }

  /**
   * Generates a value that falls under this distribution.
   *
   * @return A generated value.
   */
  public double generate() {
    double gaussian = m_Random.nextGaussian();
    double value = m_Mean + (gaussian * m_StandardDeviation);
    return value;		
  }

  /**
   * Gets the probability that a value falls under
   * this distribution.
   *
   * @param valuex The value to get the probability of.
   * @return The probability of the given value.
   */
  public double getProbabilityOf(double valuex) {	
    double twopisqrt = Math.sqrt(2 * Math.PI);
    double left = 1 / (m_StandardDeviation * twopisqrt);
    double diffsquared = Math.pow((valuex - m_Mean), 2);
    double bottomright = 2 * Math.pow(m_StandardDeviation, 2);
    double brackets = -1 * (diffsquared / bottomright);

    double probx = left * Math.exp(brackets);

    return probx;
  }

  /**
   * Gets the (natural) log of the probability of a given value.
   *
   * @param valuex The value to get the log probability of.
   * @return The (natural) log of the probability.
   */
  public double getLogProbabilityOf(double valuex) {
    double twopisqrt = Math.log(Math.sqrt(2 * Math.PI));
    double left = - (Math.log(m_StandardDeviation) + twopisqrt);
    double diffsquared = Math.pow((valuex - m_Mean), 2);
    double bottomright = 2 * Math.pow(m_StandardDeviation, 2);
    double brackets = -1 * (diffsquared / bottomright);

    double probx = left + brackets;

    return probx;
  }
}
