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
 *   EMGenerator.java
 *   Copyright (C) 2008 K.Hempstalk, University of Waikato, Hamilton, New Zealand.
 */

package weka.classifiers.meta.generators;

import weka.clusterers.EM;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.Capabilities.Capability;

/**
 <!-- globalinfo-start -->
 * A generator that uses EM as an underlying model.
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
 */
public class EMGenerator
  extends RandomizableGenerator
    implements InstanceHandler, NumericAttributeGenerator {

  /** for serialization. */
  private static final long serialVersionUID = 2769416817955024550L;

  /**
   * The underlying EM model.
   */
  protected EM m_EMModel;

  /**
   * Returns a string describing this class' ability.
   *
   * @return A description of the class.
   */
  public String globalInfo() {
    return "A generator that uses EM as an underlying model.";
  }

  /** 
   * Returns the Capabilities of this object
   *
   * @return            the capabilities of this object
   * @see               Capabilities
   */
  public Capabilities getCapabilities() {
    Capabilities result = new Capabilities(this);

    // TODO: shouldn't that return EM's capabilities?
    
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
    
    m_EMModel = new EM();	
    m_EMModel.setMaxIterations(10);
    m_EMModel.buildClusterer(someinstances);
  }

  /**
   * Generates a value that falls under this distribution.
   *
   * @return A generated value.
   */
  public double generate() {	
    //one attribute
    //get the cluster priors
    double[] clusterProbabilities = m_EMModel.getClusterPriors();
    double clusterPicked = m_Random.nextDouble();

    //find the cluster we are going to generate data for
    double sum = 0;
    int clusterID = 0;
    for(int i = 0; i < clusterProbabilities.length; i++) {
      if(clusterPicked > sum && 
	  clusterPicked <= (sum + clusterProbabilities[i])) {
	//it's this one
	clusterID = i;
	break;
      } else {
	sum = sum + clusterProbabilities[i];
	clusterID = i;
      }
    }
    //System.out.println("Selecting cluster: " + clusterID + " of " + numClusters);
    //get the mean and standard deviation of this cluster
    double[][][] normalDists = m_EMModel.getClusterModelsNumericAtts();
    double mean = normalDists[clusterID][0][0];
    double sd = normalDists[clusterID][0][1];

    double gaussian = m_Random.nextGaussian();
    double value = mean + (gaussian * sd);

    return value;	
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
    //find the cluster closest to the value of x
    Instance inst = new DenseInstance(1);
    inst.setValue(0, valuex);
    try{			
      return Math.exp(m_EMModel.logDensityForInstance(inst));			
    }catch(Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }

    return 0;
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
