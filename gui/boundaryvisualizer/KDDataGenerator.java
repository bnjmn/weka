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
 *   KDDataGenerator.java
 *   Copyright (C) 2002 Mark Hall
 *
 */

package weka.gui.boundaryvisualizer;

import weka.core.*;
import java.util.Random;
import java.io.*;

/**
 * KDDataGenerator. Class that uses kernels to generate new random
 * instances based on a supplied set of instances.
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.1 $
 * @since 1.0
 * @see DataGenerator
 * @see Serializable
 */
public class KDDataGenerator implements DataGenerator, Serializable {

  // the instances to use
  private Instances m_instances;

  // standard deviations of the normal distributions for numeric attributes in
  // each KD estimator
  private double [] m_standardDeviations;

  // global means or modes to use for missing values
  private double [] m_globalMeansOrModes;

  // minimum standard deviation for numeric attributes
  private double m_minStdDev = 1e-5;

  // Laplace correction for discrete distributions
  private double m_laplaceConst = 1.0;

  // random number seed
  private int m_seed = 1;

  // random number generator
  private Random m_random;

  // the kernel estimator from which to generate the next instance from
  private int m_KDToGenerateFrom;

  // which dimensions to use for computing a weight for each generated
  // instance
  private boolean [] m_weightingDimensions;
  
  // the values for the weighting dimensions to use for computing the weight
  // for the next instance to be generated
  private double [] m_weightingValues;
  
  // created once only - for generating instances fast
  private Instance m_instance;

  private double [] m_instanceVals;

  private static double m_normConst = Math.sqrt(2*Math.PI);

  /**
   * Initialize the generator using the supplied instances
   *
   * @param inputInstances the instances to use as the basis of the kernels
   * @exception Exception if an error occurs
   */
  public void buildGenerator(Instances inputInstances) throws Exception {
    m_random = new Random(m_seed);
    
    m_KDToGenerateFrom = 0;
    m_instances = inputInstances;
    m_standardDeviations = new double [m_instances.numAttributes()];
    m_globalMeansOrModes = new double [m_instances.numAttributes()];
    if (m_weightingDimensions == null) {
      m_weightingDimensions = new boolean[m_instances.numAttributes()];
    }
    for (int i = 0; i < m_instances.numAttributes(); i++) {
      if (m_instances.attribute(i).isNumeric()) {
	// global standard deviations
	double var = m_instances.variance(i);
	if (var == 0) {
	  var = m_minStdDev;
	} else {
	  var = Math.sqrt(var);
	  //  heuristic to take into account # instances and dimensions
	  double adjust = Math.pow((double) m_instances.numInstances(), 
				   1.0 / m_instances.numAttributes());
		  //	  double adjust = m_instances.numInstances();
	  var /= adjust;
	}
	m_standardDeviations[i] = var;
      } else {
	m_globalMeansOrModes[i] = m_instances.meanOrMode(i);
      }
    }
    m_instanceVals = new double [m_instances.numAttributes()];
    m_instance = new Instance(1.0, m_instanceVals);
  }  

  /**
   * Return a cumulative distribution from a discrete distribution
   *
   * @param dist the distribution to use
   * @return the cumulative distribution
   */
  private double [] computeCumulativeDistribution(double [] dist) {
    double [] cumDist = new double[dist.length];
    double sum = 0;
    for (int i = 0; i < dist.length; i++) {
      sum += dist[i];
      cumDist[i] = sum;
    }
    
    return cumDist;
  }

  /**
   * Generate a new instance. Returns the instance in an brand new
   * Instance object.
   *
   * @return an <code>Instance</code> value
   * @exception Exception if an error occurs
   */
  public Instance generateInstance() throws Exception {
    return generateInstance(false);
  }
  
  /**
   * Generate a new instance. Reuses an existing instance object to
   * speed up the process.
   *
   * @return an <code>Instance</code> value
   * @exception Exception if an error occurs
   */
  public Instance generateInstanceFast() throws Exception {
    return generateInstance(true);
  }

  /**
   * Generates a new instance using one kernel estimator. Each successive
   * call to this method incremets the index of the kernel to use.
   *
   * @param fast generate the instance quickly
   * @return the new random instance
   * @exception Exception if an error occurs
   */
  private Instance generateInstance(boolean fast) throws Exception {
    if (m_weightingDimensions.length != m_instances.numAttributes()) {
      throw new Exception("Weighting dimension array != num attributes!");
    }

    Instance newInst;

    if (fast) {
      newInst = m_instance;
    } else {
      m_instanceVals = new double [m_instances.numAttributes()];
      newInst = new Instance(1.0, m_instanceVals);
    }
    
    double weight = 1;
    for (int i = 0; i < m_instances.numAttributes(); i++) {
      if (!m_weightingDimensions[i]) {
	if (m_instances.attribute(i).isNumeric()) {
	  double mean = 0;
	  double val = m_random.nextGaussian();
	  if (!m_instances.instance(m_KDToGenerateFrom).isMissing(i)) {
	    mean = m_instances.instance(m_KDToGenerateFrom).value(i);
	  } else {
	    mean = m_globalMeansOrModes[i];
	  }
	  val *= m_standardDeviations[i];
	  val += mean;
	  m_instanceVals[i] = val;
	} else {
	  // nominal attribute
	  double [] dist = new double[m_instances.attribute(i).numValues()];
	  for (int j = 0; j < dist.length; j++) {
	    dist[j] = m_laplaceConst;
	  }
	  if (!m_instances.instance(m_KDToGenerateFrom).isMissing(i)) {
	    dist[(int)m_instances.instance(m_KDToGenerateFrom).value(i)]++;
	  } else {
	    dist[(int)m_globalMeansOrModes[i]]++;
	  }
	  Utils.normalize(dist);
	  double [] cumDist = computeCumulativeDistribution(dist);
	  double randomVal = m_random.nextDouble();
	  int instVal = 0;
	  for (int j = 0; j < cumDist.length; j++) {
	    if (randomVal <= cumDist[j]) {
	      instVal = j;
	      break;
	    }
	  }
	  m_instanceVals[i] = (double)instVal;
	}
      } else {
	double mean = 0;
	if (!m_instances.instance(m_KDToGenerateFrom).isMissing(i)) {
	  mean = m_instances.instance(m_KDToGenerateFrom).value(i);
	} else {
	  mean = m_globalMeansOrModes[i];
	}
	double wm = 1.0;
	if (m_instances.attribute(i).isNumeric()) {
	  wm = normalDens(m_weightingValues[i], mean,
				 m_standardDeviations[i]);
	} else {
	  wm = (1.0 + m_laplaceConst) / 
	    (m_instances.attribute(i).numValues() * m_laplaceConst); 
	}
	if (wm > 0) {
	  weight *= wm;
	}
	m_instanceVals[i] = m_weightingValues[i];
      }
    }
    newInst.setWeight(weight);
    // next kernel to generate from
    m_KDToGenerateFrom++;
    m_KDToGenerateFrom %= m_instances.numInstances();
    return newInst;
  }

  /**
   * Density function of normal distribution.
   * @param x input value
   * @param mean mean of distribution
   * @param stdDev standard deviation of distribution
   */
  private double normalDens (double x, double mean, double stdDev) {
    double diff = x - mean;
   
    return  (1/(m_normConst*stdDev))*Math.exp(-(diff*diff/(2*stdDev*stdDev)));
  }

  /**
   * Set which dimensions to use when computing a weight for the next
   * instance to generate
   *
   * @param dims an array of booleans indicating which dimensions to use
   */
  public void setWeightingDimensions(boolean [] dims) {
    m_weightingDimensions = dims;
  }

  /**
   * Set the values for the weighting dimensions to be used when computing
   * the weight for the next instance to be generated
   *
   * @param vals an array of doubles containing the values of the
   * weighting dimensions (corresponding to the entries that are set to
   * true throw setWeightingDimensions)
   */
  public void setWeightingValues(double [] vals) {
    m_weightingValues = vals;
  }

  /**
   * Return the number of kernels (there is one per training instance)
   *
   * @return the number of kernels
   */
  public int getNumGeneratingModels() {
    if (m_instances != null) {
      return m_instances.numInstances();
    }
    return 0;
  }

  /**
   * Main method for tesing this class
   *
   * @param args a <code>String[]</code> value
   */
  public static void main(String [] args) {
    try {
      Reader r = null;
      if (args.length != 1) {
	throw new Exception("Usage: KDDataGenerator <filename>");
      } else {
	r = new BufferedReader(new FileReader(args[0]));
	Instances insts = new Instances(r);
	KDDataGenerator dg = new KDDataGenerator();
	dg.buildGenerator(insts);
	Instances header = new Instances(insts,0);
	System.out.println(header);
	for (int i = 0; i < insts.numInstances(); i++) {
	  Instance newInst = dg.generateInstance();
	  newInst.setDataset(header);
	  System.out.println(newInst);
	}
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
