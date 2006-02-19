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
 *    FarthestFirst.java
 *    Copyright (C) 2002 Bernhard Pfahringer
 *    based on SimpleKMeans which is 
 *     Copyright (C) 2000 Mark Hall (mhall@cs.waikato.ac.nz)
 *
 */
package weka.clusterers;

import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

/**
 * Implements the "Farthest First Traversal Algorithm" by 
 * Hochbaum and Shmoys 1985: A best possible heuristic for the
 * k-center problem, Mathematics of Operations Research, 10(2):180-184,
 * as cited by Sanjoy Dasgupta "performance guarantees for hierarchical
 * clustering", colt 2002, sydney
 *
 * works as a fast simple approximate clusterer
 *
 * modelled after SimpleKMeans, might be a useful initializer for it
 *
 * Valid options are:<p>
 *
 * -N <number of clusters> <br>
 * Specify the number of clusters to generate. <p>
 *
 * -S <seed> <br>
 * Specify random number seed. <p>
 *
 * @author Bernhard Pfahringer (bernhard@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 * @see Clusterer
 * @see OptionHandler
 */

// Todo: rewrite to be fully incremental
//       cleanup, like deleting m_instances 
public class FarthestFirst extends Clusterer implements OptionHandler {

  static final long serialVersionUID = 7499838100631329509L;
  
  /**
   * training instances, not necessary to keep, 
   * could be replaced by m_ClusterCentroids where needed for header info
   */
  protected Instances m_instances;

  /**
   * replace missing values in training instances
   */
  protected ReplaceMissingValues m_ReplaceMissingFilter;

  /**
   * number of clusters to generate
   */
  protected int m_NumClusters = 2;

  /**
   * holds the cluster centroids
   */
  protected Instances m_ClusterCentroids;

  /**
   * attribute min values
   */
  private double [] m_Min;
  
  /**
   * attribute max values
   */
  private double [] m_Max;

  /**
   * random seed
   */
  protected int m_Seed = 1;

  /**
   * Returns a string describing this clusterer
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Cluster data using the FarthestFirst algorithm";
  }

  /**
   * Returns default capabilities of the clusterer.
   *
   * @return      the capabilities of this clusterer
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    return result;
  }

  /**
   * Generates a clusterer. Has to initialize all fields of the clusterer
   * that are not being set via options.
   *
   * @param data set of instances serving as training data 
   * @exception Exception if the clusterer has not been 
   * generated successfully
   */
  public void buildClusterer(Instances data) throws Exception {

    // can clusterer handle the data?
    getCapabilities().testWithFail(data);

    //long start = System.currentTimeMillis();

    m_ReplaceMissingFilter = new ReplaceMissingValues();
    m_ReplaceMissingFilter.setInputFormat(data);
    m_instances = Filter.useFilter(data, m_ReplaceMissingFilter);

    initMinMax(m_instances);

    m_ClusterCentroids = new Instances(m_instances, m_NumClusters);

    int n = m_instances.numInstances();
    Random r = new Random(m_Seed);
    boolean[] selected = new boolean[n];
    double[] minDistance = new double[n];

    for(int i = 0; i<n; i++) minDistance[i] = Double.MAX_VALUE;

    int firstI = r.nextInt(n);
    m_ClusterCentroids.add(m_instances.instance(firstI));
    selected[firstI] = true;

    updateMinDistance(minDistance,selected,m_instances,m_instances.instance(firstI));

    if (m_NumClusters > n) m_NumClusters = n;

    for(int i = 1; i < m_NumClusters; i++) {
      int nextI =  farthestAway(minDistance, selected);
      m_ClusterCentroids.add(m_instances.instance(nextI));
      selected[nextI] = true;
      updateMinDistance(minDistance,selected,m_instances,m_instances.instance(nextI));
    }

    m_instances = new Instances(m_instances,0);
    //long end = System.currentTimeMillis();
    //System.out.println("Clustering Time = " + (end-start));
  }


  protected void updateMinDistance(double[] minDistance, boolean[] selected, 
				   Instances data, Instance center) {
    for(int i = 0; i<selected.length; i++) 
      if (!selected[i]) {
	double d = distance(center,data.instance(i));
	if (d<minDistance[i]) 
	  minDistance[i] = d;
      }
  }

  protected int farthestAway(double[] minDistance, boolean[] selected) {
    double maxDistance = -1.0;
    int maxI = -1;
    for(int i = 0; i<selected.length; i++) 
      if (!selected[i]) 
	if (maxDistance < minDistance[i]) {
	  maxDistance = minDistance[i];
	  maxI = i;
	}
    return maxI;
  }

  protected void initMinMax(Instances data) {
    m_Min = new double [data.numAttributes()];
    m_Max = new double [data.numAttributes()];
    for (int i = 0; i < data.numAttributes(); i++) {
      m_Min[i] = m_Max[i] = Double.NaN;
    }

    for (int i = 0; i < data.numInstances(); i++) {
      updateMinMax(data.instance(i));
    }
  }


  /**
   * Updates the minimum and maximum values for all the attributes
   * based on a new instance.
   *
   * @param instance the new instance
   */
  private void updateMinMax(Instance instance) {  

    for (int j = 0;j < instance.numAttributes(); j++) {
      if (Double.isNaN(m_Min[j])) {
	m_Min[j] = instance.value(j);
	m_Max[j] = instance.value(j);
      } else {
	if (instance.value(j) < m_Min[j]) {
	  m_Min[j] = instance.value(j);
	} else {
	  if (instance.value(j) > m_Max[j]) {
	    m_Max[j] = instance.value(j);
	  }
	}
      }
    }
  }


  /**
   * clusters an instance that has been through the filters
   *
   * @param instance the instance to assign a cluster to
   * @return a cluster number
   */
  protected int clusterProcessedInstance(Instance instance) {
    double minDist = Double.MAX_VALUE;
    int bestCluster = 0;
    for (int i = 0; i < m_NumClusters; i++) {
      double dist = distance(instance, m_ClusterCentroids.instance(i));
      if (dist < minDist) {
	minDist = dist;
	bestCluster = i;
      }
    }
    return bestCluster;
  }

  /**
   * Classifies a given instance.
   *
   * @param instance the instance to be assigned to a cluster
   * @return the number of the assigned cluster as an integer
   * if the class is enumerated, otherwise the predicted value
   * @exception Exception if instance could not be classified
   * successfully
   */
  public int clusterInstance(Instance instance) throws Exception {
    m_ReplaceMissingFilter.input(instance);
    m_ReplaceMissingFilter.batchFinished();
    Instance inst = m_ReplaceMissingFilter.output();

    return clusterProcessedInstance(inst);
  }

  /**
   * Calculates the distance between two instances
   *
   * @param test the first instance
   * @param train the second instance
   * @return the distance between the two given instances, between 0 and 1
   */          
  protected double distance(Instance first, Instance second) {  

    double distance = 0;
    int firstI, secondI;

    for (int p1 = 0, p2 = 0; 
	 p1 < first.numValues() || p2 < second.numValues();) {
      if (p1 >= first.numValues()) {
	firstI = m_instances.numAttributes();
      } else {
	firstI = first.index(p1); 
      }
      if (p2 >= second.numValues()) {
	secondI = m_instances.numAttributes();
      } else {
	secondI = second.index(p2);
      }
      if (firstI == m_instances.classIndex()) {
	p1++; continue;
      } 
      if (secondI == m_instances.classIndex()) {
	p2++; continue;
      } 
      double diff;
      if (firstI == secondI) {
	diff = difference(firstI, 
			  first.valueSparse(p1),
			  second.valueSparse(p2));
	p1++; p2++;
      } else if (firstI > secondI) {
	diff = difference(secondI, 
			  0, second.valueSparse(p2));
	p2++;
      } else {
	diff = difference(firstI, 
			  first.valueSparse(p1), 0);
	p1++;
      }
      distance += diff * diff;
    }
    
    return Math.sqrt(distance / m_instances.numAttributes());
  }

  /**
   * Computes the difference between two given attribute
   * values.
   */
  protected double difference(int index, double val1, double val2) {

    switch (m_instances.attribute(index).type()) {
    case Attribute.NOMINAL:
      
      // If attribute is nominal
      if (Instance.isMissingValue(val1) || 
	  Instance.isMissingValue(val2) ||
	  ((int)val1 != (int)val2)) {
	return 1;
      } else {
	return 0;
      }
    case Attribute.NUMERIC:

      // If attribute is numeric
      if (Instance.isMissingValue(val1) || 
	  Instance.isMissingValue(val2)) {
	if (Instance.isMissingValue(val1) && 
	    Instance.isMissingValue(val2)) {
	  return 1;
	} else {
	  double diff;
	  if (Instance.isMissingValue(val2)) {
	    diff = norm(val1, index);
	  } else {
	    diff = norm(val2, index);
	  }
	  if (diff < 0.5) {
	    diff = 1.0 - diff;
	  }
	  return diff;
	}
      } else {
	return norm(val1, index) - norm(val2, index);
      }
    default:
      return 0;
    }
  }

  /**
   * Normalizes a given value of a numeric attribute.
   *
   * @param x the value to be normalized
   * @param i the attribute's index
   */
  protected double norm(double x, int i) {

    if (Double.isNaN(m_Min[i]) || Utils.eq(m_Max[i],m_Min[i])) {
      return 0;
    } else {
      return (x - m_Min[i]) / (m_Max[i] - m_Min[i]);
    }
  }

  /**
   * Returns the number of clusters.
   *
   * @return the number of clusters generated for a training dataset.
   * @exception Exception if number of clusters could not be returned
   * successfully
   */
  public int numberOfClusters() throws Exception {
    return m_NumClusters;
  }

  /**
   * Returns an enumeration describing the available options.. <p>
   *
   * Valid options are:<p>
   *
   * -N <number of clusters> <br>
   * Specify the number of clusters to generate. If omitted,
   * FarthestFirst will use cross validation to select the number of clusters
   * automatically. <p>
   *
   * -S <seed> <br>
   * Specify random number seed. <p>
   *
   * @return an enumeration of all the available options.
   *
   **/
  public Enumeration listOptions () {
    Vector newVector = new Vector(2);

     newVector.addElement(new Option("\tnumber of clusters. (default = 2)." 
				    , "N", 1, "-N <num>"));
     newVector.addElement(new Option("\trandom number seed.\n (default 10)"
				     , "S", 1, "-S <num>"));

     return  newVector.elements();
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numClustersTipText() {
    return "set number of clusters";
  }

  /**
   * set the number of clusters to generate
   *
   * @param n the number of clusters to generate
   */
  public void setNumClusters(int n) throws Exception {
    if (n < 0) {
      throw new Exception("Number of clusters must be > 0");
    }
    m_NumClusters = n;
  }

  /**
   * gets the number of clusters to generate
   *
   * @return the number of clusters to generate
   */
  public int getNumClusters() {
    return m_NumClusters;
  }
    
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String seedTipText() {
    return "random number seed";
  }


  /**
   * Set the random number seed
   *
   * @param s the seed
   */
  public void setSeed (int s) {
    m_Seed = s;
  }


  /**
   * Get the random number seed
   *
   * @return the seed
   */
  public int getSeed () {
    return  m_Seed;
  }

  /**
   * Parses a given list of options.
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   *
   **/
  public void setOptions (String[] options)
    throws Exception {

    String optionString = Utils.getOption('N', options);

    if (optionString.length() != 0) {
      setNumClusters(Integer.parseInt(optionString));
    }

    optionString = Utils.getOption('S', options);
    
    if (optionString.length() != 0) {
      setSeed(Integer.parseInt(optionString));
    }
  }

  /**
   * Gets the current settings of FarthestFirst
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions () {
    String[] options = new String[4];
    int current = 0;
    
    options[current++] = "-N";
    options[current++] = "" + getNumClusters();
    options[current++] = "-S";
    options[current++] = "" + getSeed();
    
    while (current < options.length) {
      options[current++] = "";
    }

    return  options;
  }

  /**
   * return a string describing this clusterer
   *
   * @return a description of the clusterer as a string
   */
  public String toString() {
    StringBuffer temp = new StringBuffer();

    temp.append("\n FarthestFirst\n==============\n");

    temp.append("\nCluster centroids:\n");
    for (int i = 0; i < m_NumClusters; i++) {
      temp.append("\nCluster "+i+"\n\t");
      for (int j = 0; j < m_ClusterCentroids.numAttributes(); j++) {
	if (m_ClusterCentroids.attribute(j).isNominal()) {
	  temp.append(" "+m_ClusterCentroids.attribute(j).
		      value((int)m_ClusterCentroids.instance(i).value(j)));
	} else {
	  temp.append(" "+m_ClusterCentroids.instance(i).value(j));
	}
      }
    }
    temp.append("\n\n");
    return temp.toString();
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain the following arguments: <p>
   * -t training file [-N number of clusters]
   */
  public static void main (String[] argv) {
    try {
      System.out.println(ClusterEvaluation.
			 evaluateClusterer(new FarthestFirst(), argv));
    }
    catch (Exception e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }
}
