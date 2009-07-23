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
 *   MixedGaussianGenerator.java
 *   Copyright (C) 2008 K.Hempstalk, University of Waikato, Hamilton, New Zealand.
 */

package weka.classifiers.meta.generators;

import weka.core.Option;
import weka.core.Utils;

import java.util.Enumeration;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * A mixed Gaussian artificial data generator.<br/>
 * <br/>
 * This generator only has two Gaussians, each sitting 3 standard deviations (by default) away from the mean of the main distribution.  Each model has half of the probability.  The idea is that the two sub-models form a boundary either side of the main distribution.
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
 * <pre> -di &lt;distance&gt;
 *  Sets the difference between the mean and what will be used
 *  on the lower and higher distributions for the generator. (default: 3)</pre>
 * 
 * <pre> -da
 *  If set, the generator will use the absolute value of the
 *  difference. If not set, it will multiply the difference by
 *  the standard deviation.</pre>
 * 
 <!-- options-end -->
 *
 * @author Kathryn Hempstalk (kah18 at cs.waikato.ac.nz)
 * @version $Revision$
 */
public class MixedGaussianGenerator
  extends RandomizableDistributionGenerator 
  implements NumericAttributeGenerator {

  /** for serialization. */
  private static final long serialVersionUID = 1516470615315381362L;

  /**
   * The distance between the main distribution and each model.
   */
  protected double m_Distance = 3;

  /**
   * Whether the difference is absolute, or a modifier to the
   * standard deviation.
   */
  protected boolean m_DistanceAbsolute = false;

  /**
   * Returns a string describing this class' ability.
   *
   * @return A description of the class.
   */
  public String globalInfo() {
    return 
        "A mixed Gaussian artificial data generator.\n"
      + "\n"
      + "This generator only has two Gaussians, each sitting "
      + "3 standard deviations (by default) away from the mean "
      + "of the main distribution.  Each model has half of the "
      + "probability.  The idea is that the two sub-models "
      + "form a boundary either side of the main distribution.";
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector result = new Vector();   

    Enumeration enu = super.listOptions();
    while (enu.hasMoreElements())
      result.addElement(enu.nextElement());

    result.addElement(new Option(
	"\tSets the difference between the mean and what will be used\n"
	+ "\ton the lower and higher distributions for the generator."
	+ "\t(default: 3)",
	"di", 1, "-di <distance>"));
    
    result.addElement(new Option(
	"\tIf set, the generator will use the absolute value of the\n"
	+ "\tdifference. If not set, it will multiply the difference by\n"
	+ "\tthe standard deviation.",
	"da", 0, "-da"));

    return result.elements();
  }

  /**
   * Parses a given list of options. <p/>
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
   * <pre> -di &lt;distance&gt;
   *  Sets the difference between the mean and what will be used
   *  on the lower and higher distributions for the generator. (default: 3)</pre>
   * 
   * <pre> -da
   *  If set, the generator will use the absolute value of the
   *  difference. If not set, it will multiply the difference by
   *  the standard deviation.</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    String	tmpStr;
    
    super.setOptions(options);

    setDistanceAbsolute(Utils.getFlag("da", options));

    tmpStr = Utils.getOption("di", options);
    if (tmpStr.length() != 0)
      setDistance(Double.parseDouble(tmpStr));
    else
      setDistance(3.0);
  }

  /**
   * Gets the current settings of the generator.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {
    Vector<String>	result;
    String[]		options;
    int			i;

    result = new Vector<String>();

    options = super.getOptions();
    for (i = 0; i < options.length; i++)
      result.add(options[i]);
    
    if (getDistanceAbsolute()) 
      result.add("-da");
 
    result.add("-di");
    result.add("" + m_Distance);

    return result.toArray(new String[result.size()]);
  }

  /**
   * Gets the difference between the main distribution and each
   * of the models.  The default difference is 3, and will
   * cause each model to be 3 standard deviations away from the mean.
   * One model is created either side of the mean.
   *
   * @return The difference between the main distribution and a model.
   */
  public double getDistance() {
    return m_Distance;
  }

  /**
   * Sets the difference between the main distribution and the models.
   * See getDistance() for a longer explanation.
   *
   * @param diff The new difference.
   */
  public void setDistance(double diff) {
    m_Distance = diff;
  }
  
  /**
   * Returns the tip text for this property.
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String distanceTipText() {
    return "The difference between the main distribution and the models.";
  }

  /**
   * Gets whether the difference will be an absolute value,
   * or something that is used as a multiplier to the 
   * standard deviation.
   *
   * @return Whether the difference will be absolute or not.
   */
  public boolean getDistanceAbsolute() {
    return m_DistanceAbsolute;
  }

  /**
   * Sets the difference to be absolute (or not).
   *
   * @param newdiff Whether the difference should be absolute or
   * a standard deviation modifier.
   */
  public void setDistanceAbsolute(boolean newdiff) {
    m_DistanceAbsolute = newdiff;
  }
  
  /**
   * Returns the tip text for this property.
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String distanceAbsoluteTipText() {
    return "If true, then the distance is absolute.";
  }

  /**
   * Generates a value that falls under this distribution.
   *
   * @return A generated value.
   */
  public double generate() {
    double difference = m_Distance;
    if(!m_DistanceAbsolute)
      difference = m_StandardDeviation * m_Distance;

    if(m_Random.nextBoolean()) {
      //lower distribution
      double gaussian = m_Random.nextGaussian();
      double value = (m_Mean - difference) + (gaussian * m_StandardDeviation);
      return value;		
    } else {
      //higher distribution
      double gaussian = m_Random.nextGaussian();
      double value = (m_Mean + difference) + (gaussian * m_StandardDeviation);
      return value;
    }
  }


  /**
   * Gets the probability that a value falls under
   * this distribution.
   * 
   *
   * @param valuex The value to get the probability of.
   * @return The probability of the given value.
   */
  public double getProbabilityOf(double valuex) {
    double difference = m_Distance;
    if(!m_DistanceAbsolute)
      difference = m_StandardDeviation * m_Distance;

    double prob1 = 0.5 * this.getProbability(valuex, m_Mean - difference, m_StandardDeviation);
    double prob2 = 0.5 * this.getProbability(valuex, m_Mean + difference, m_StandardDeviation);
    return prob1 + prob2;
  }

  /**
   * Gets the probability that a value falls under
   * a given Gaussian distribution.
   * 
   *
   * @param valuex The value to get the probability of.
   * @param mean The mean of the Gaussian distribution.
   * @param stddev The standard deviation of the Gaussian distribution.
   * @return The probability of the given value.
   */
  public double getProbability(double valuex, double mean, double stddev) {
    double twopisqrt = Math.sqrt(2 * Math.PI);
    double left = 1 / (stddev * twopisqrt);
    double diffsquared = Math.pow((valuex - mean), 2);
    double bottomright = 2 * Math.pow(stddev, 2);
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
    return Math.log(this.getProbabilityOf(valuex));
  }
}
