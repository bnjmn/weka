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
 *   DiscreteUniformGenerator.java
 *   Copyright (C) 2008 K.Hempstalk, University of Waikato, Hamilton, New Zealand.
 */

package weka.classifiers.meta.generators;

import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Capabilities.Capability;

import java.util.Arrays;

/**
 <!-- globalinfo-start -->
 * An artificial data generator that uses discrete buckets for values.<br/>
 * <br/>
 * In this discrete uniform generator, all buckets are given the same probability, regardless of how many values fall into each bucket.  This is not to be confused with the discrete generator which gives every bucket a probability based on how full the bucket is.
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
 <!-- options-end -->
 *
 * @author Kathryn Hempstalk (kah18 at cs.waikato.ac.nz)
 * @version $Revision$
 * @see DiscreteGenerator
 */
public class DiscreteUniformGenerator
  extends RandomizableGenerator
    implements InstanceHandler, NumericAttributeGenerator {

  /** for serialization. */
  private static final long serialVersionUID = 3639933602298510366L;

  /**
   * The array of probabilities for this generator.
   */
  protected double[] m_Probabilities;

  /**
   * Returns a string describing this class' ability.
   *
   * @return A description of the class.
   */
  public String globalInfo() {
    return 
        "An artificial data generator that uses discrete buckets "
      + "for values.\n"
      + "\n"
      + "In this discrete uniform generator, all buckets are given "
      + "the same probability, regardless of how many values fall into "
      + "each bucket.  This is not to be confused with the "
      + "discrete generator which gives every bucket a probability "
      + "based on how full the bucket is.";
  }

  /** 
   * Returns the Capabilities of this object
   *
   * @return            the capabilities of this object
   * @see               Capabilities
   */
  public Capabilities getCapabilities() {
    Capabilities result = new Capabilities(this);

    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enableAllClasses();
    result.enable(Capability.MISSING_CLASS_VALUES);
    result.enable(Capability.NO_CLASS);

    return result;
  }

  /**
   * Builds the generator with a given set of instances.
   *
   * @param someinstances The instances that will be used to 
   * build up the probabilities for this generator.
   * @throws Exception if data cannot be processed
   */
  public void buildGenerator(Instances someinstances) throws Exception {
    // can generator handle the data?
    getCapabilities().testWithFail(someinstances);
    
    someinstances = new Instances(someinstances);
    someinstances.deleteWithMissing(0);

    //put all the values in an array
    double[] values = new double[someinstances.numInstances()];

    for(int i = 0; i < someinstances.numInstances(); i++) {
      Instance aninst = someinstances.instance(i);
      values[i] = aninst.value(0);
    }

    Arrays.sort(values);

    double count = 1;
    for(int i = 1; i < values.length; i++) {
      if(values[i] != values[i - 1])
	count++;
    }

    //now we know how many values we have
    double[] allvals = new double[(int)count];
    int position = 0;
    allvals [0] = values[0];


    for(int i = 1; i < values.length; i++) {
      if(values[i] != values[i - 1]) {
	position++;
	allvals[position]= values[i];	    
      }
    }	

    m_Probabilities = allvals;
  }

  /**
   * Generates a value that falls under this distribution.
   *
   * @return A generated value.
   */
  public double generate() {	
    double aprob = m_Random.nextDouble();
    double gap = 1 / ((double)m_Probabilities.length);
    int position = (int) (aprob / gap);
    if(position < 0)
      position = 0;
    if(position > m_Probabilities.length - 1)
      position = m_Probabilities.length - 1;

    return m_Probabilities[position];		
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
    for(int i = 0; i < m_Probabilities.length; i++) {
      if(valuex == m_Probabilities[i])
	return 1 / ((double)m_Probabilities.length + 1);
    }

    return 1 / ((double) m_Probabilities.length + 1);
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
