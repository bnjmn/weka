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
 *    DistributionMetaClusterer.java
 *    Copyright (C) 2002 Richard Kirkby
 *
 */

package weka.clusterers;

import weka.core.*;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Class for wrapping a Clusterer to make it return a distribution. Simply
 * outputs a probabiltry of 1 for the chosen cluster and 0 for the others.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 1.6 $
 */
public class DistributionMetaClusterer extends DistributionClusterer 
  implements OptionHandler {

  /** The clusterer being wrapped */
  private Clusterer m_wrappedClusterer = new weka.clusterers.EM();

  /**
   * Default constructor.
   * 
   */  
  public DistributionMetaClusterer() {

  }
   
  /**
   * Contructs a DistributionMetaClusterer wrapping a given Clusterer.
   * 
   * @param toWrap the clusterer to wrap around
   */    
  public DistributionMetaClusterer(Clusterer toWrap) {

    setClusterer(toWrap);
  }
  
  /**
   * Builds a clusterer for a set of instances.
   *
   * @param instances the instances to train the clusterer with
   * @exception Exception if the clusterer hasn't been set or something goes wrong
   */  
  public void buildClusterer(Instances data) throws Exception {

    if (m_wrappedClusterer == null) {
      throw new Exception("No clusterer has been set");
    }
    m_wrappedClusterer.buildClusterer(data);
  }
  
  /**
   * Computes the density for a given instance.
   * 
   * @param instance the instance to compute the density for
   * @return the density.
   * @exception Exception if the density could not be computed successfully
   */
  public double densityForInstance(Instance instance) throws Exception {

    return Utils.sum(distributionForInstance(instance));
  }

  /**
   * Returns the cluster probability distribution for an instance. Will simply have a
   * probability of 1 for the chosen cluster and 0 for the others.
   *
   * @param instance the instance to be clustered
   * @return the probability distribution
   */  
  public double[] distributionForInstance(Instance instance) throws Exception {
    
    double[] distribution = new double[m_wrappedClusterer.numberOfClusters()];
    distribution[m_wrappedClusterer.clusterInstance(instance)] = 1.0;
    return distribution;
  }
  
  /**
   * Returns the number of clusters.
   *
   * @return the number of clusters generated for a training dataset.
   * @exception Exception if number of clusters could not be returned successfully
   */
  public int numberOfClusters() throws Exception {

    return m_wrappedClusterer.numberOfClusters();
  }

  /**
   * Returns a description of the clusterer.
   *
   * @return a string containing a description of the clusterer
   */
  public String toString() {

    return "DistributionMetaClusterer: " + m_wrappedClusterer.toString();
  }

  /**
   * Sets the clusterer to wrap.
   *
   * @param toWrap the clusterer
   */
  public void setClusterer(Clusterer toWrap) {

    m_wrappedClusterer = toWrap;
  }

  /**
   * Gets the clusterer being wrapped.
   *
   * @return the clusterer
   */
  public Clusterer getClusterer() {

    return m_wrappedClusterer;
  }

  /**
   * Returns an enumeration describing the available options..
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    
    Vector newVector = new Vector(1);
    newVector.addElement(new Option(
				    "\tClusterer to wrap. (required)\n",
				    "W", 1,"-W <clusterer name>"));

    if ((m_wrappedClusterer != null) &&
	(m_wrappedClusterer instanceof OptionHandler)) {
      newVector.addElement(new Option(
				      "",
				      "", 0, "\nOptions specific to clusterer "
				      + m_wrappedClusterer.getClass().getName() + ":"));
      Enumeration enum = ((OptionHandler)m_wrappedClusterer).listOptions();
      while (enum.hasMoreElements()) {
	newVector.addElement(enum.nextElement());
      }
    }
    
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -W clusterer name <br>
   * Clusterer to wrap. (required) <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    String wString = Utils.getOption('W', options);
    if (wString.length() != 0) {
      setClusterer(Clusterer.forName(wString,
				       Utils.partitionOptions(options)));

    } else {
      throw new Exception("A clusterer must be specified with the -W option.");
    }
  }

  /**
   * Gets the current settings of the clusterer.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions() {

    String [] clustererOptions = new String [0];
    if ((m_wrappedClusterer != null) &&
	(m_wrappedClusterer instanceof OptionHandler)) {
      clustererOptions = ((OptionHandler)m_wrappedClusterer).getOptions();
    }
    String [] options = new String [clustererOptions.length + 3];
    int current = 0;

    if (getClusterer() != null) {
      options[current++] = "-W";
      options[current++] = getClusterer().getClass().getName();
    }
    options[current++] = "--";

    System.arraycopy(clustererOptions, 0, options, current, 
		     clustererOptions.length);
    current += clustererOptions.length;
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {
    
    try {
      System.out.println(ClusterEvaluation.
			 evaluateClusterer(new DistributionMetaClusterer(), 
					   argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}
