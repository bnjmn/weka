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
 *    Copyright (C) 2000 Intelligenesis Corp.
 *
 */

package weka.clusterers;

import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;

/**
 * Class that wraps up a Clusterer and presents it as a DistributionClusterer
 * for ease of programmatically handling Clusterers in general -- only the
 * one predict method (distributionForInstance) need be worried about. The
 * distributions produced by this clusterer place a probability of 1 on the
 * class value predicted by the sub-clusterer.<p>
 *
 * Valid options are:<p>
 *
 * -W classname <br>
 * Specify the full class name of a sub-clusterer (required).<p>
 *
 * @author Richard Littin (richard@intelligenesis.net)
 * @version $Revision: 1.5 $
 */
public class DistributionMetaClusterer extends DistributionClusterer
  implements OptionHandler {

  /** The clusterer. */
  private Clusterer m_Clusterer = new weka.clusterers.EM();

  /**
   * Builds the clusterer.
   *
   * @param insts the training data.
   * @exception Exception if a clusterer can't be built
   */
  public void buildClusterer(Instances insts) throws Exception {

    if (m_Clusterer == null) {
      throw new Exception("No base clusterer has been set!");
    }
    m_Clusterer.buildClusterer(insts);
  }

  /**
   * Returns the distribution for an instance.
   *
   * @exception Exception if the distribution can't be computed successfully
   */
  public double[] distributionForInstance(Instance inst) throws Exception {
    
    double[] result = new double[m_Clusterer.numberOfClusters()];
    int predictedCluster = m_Clusterer.clusterInstance(inst);
    result[predictedCluster] = 1.0;
    return result;
  }

  /**
   * Returns the density for an instance.
   *
   * @exception Exception if the distribution can't be computed successfully
   */
  public double densityForInstance(Instance inst) throws Exception {
    return Utils.sum(distributionForInstance(inst));
  }

  /**
   * Returns the number of clusters.
   *
   * @return the number of clusters generated for a training dataset.
   * @exception Exception if number of clusters could not be returned
   * successfully
   */
  public int numberOfClusters() throws Exception {
    return  m_Clusterer.numberOfClusters();
  }

  /**
   * Prints the clusterers.
   */
  public String toString() {
    return "DistributionMetaClusterer: " + m_Clusterer.toString() + "\n";
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector vec = new Vector(1);
    vec.addElement(new Option("\tSets the base clusterer.",
			      "W", 1, "-W <base clusterer>"));
    
    if (m_Clusterer != null) {
      try {
	vec.addElement(new Option("",
				  "", 0, "\nOptions specific to clusterer "
				  + m_Clusterer.getClass().getName() + ":"));
	Enumeration enum = ((OptionHandler)m_Clusterer).listOptions();
	while (enum.hasMoreElements()) {
	  vec.addElement(enum.nextElement());
	}
      } catch (Exception e) {
      }
    }
    return vec.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -W classname <br>
   * Specify the full class name of a learner as the basis for 
   * the multiclassclusterer (required).<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
  
    String clustererName = Utils.getOption('W', options);
    if (clustererName.length() == 0) {
      throw new Exception("A clusterer must be specified with"
			  + " the -W option.");
    }
    setClusterer(Clusterer.forName(clustererName,
				     Utils.partitionOptions(options)));
  }

  /**
   * Gets the current settings of the Clusterer.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {
    
    String [] clustererOptions = new String [0];
    if ((m_Clusterer != null) &&
	(m_Clusterer instanceof OptionHandler)) {
      clustererOptions = ((OptionHandler)m_Clusterer).getOptions();
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
   * Set the base clusterer. 
   *
   * @param newClusterer the Clusterer to use.
   */
  public void setClusterer(Clusterer newClusterer) {

    m_Clusterer = newClusterer;
  }

  /**
   * Get the clusterer used as the clusterer
   *
   * @return the clusterer used as the clusterer
   */
  public Clusterer getClusterer() {

    return m_Clusterer;
  }

  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {

    try {
      DistributionClusterer scheme = new DistributionMetaClusterer();
      System.out.println(ClusterEvaluation.evaluateClusterer(scheme, argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}
