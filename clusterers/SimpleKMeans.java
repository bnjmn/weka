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
 *    SimpleKMeans.java
 *    Copyright (C) 2000 Mark Hall
 *
 */
package weka.clusterers;

import  java.io.*;
import  java.util.*;
import  weka.core.*;
import  weka.filters.Filter;
import  weka.filters.unsupervised.attribute.ReplaceMissingValues;
import weka.experiment.Stats;

/**
 * Simple k means clustering class.
 *
 * Valid options are:<p>
 *
 * -N <number of clusters> <br>
 * Specify the number of clusters to generate. <p>
 *
 * -S <seed> <br>
 * Specify random number seed. <p>
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.10 $
 * @see Clusterer
 * @see OptionHandler
 */
public class SimpleKMeans extends Clusterer implements OptionHandler {

  /**
   * training instances
   */
  private Instances m_instances;

  /**
   * replace missing values in training instances
   */
  private ReplaceMissingValues m_ReplaceMissingFilter;

  /**
   * number of clusters to generate
   */
  private int m_NumClusters = 2;

  /**
   * holds the cluster centroids
   */
  private Instances m_ClusterCentroids;

  /**
   * Holds the standard deviations of attributes in each cluster
   */
  private Instances m_ClusterStdDevs;

  /**
   * temporary variable holding cluster assignments while iterating
   */
  private int [] m_ClusterAssignments;

  /**
   * random seed
   */
  private int m_Seed = 10;

  /**
   * attribute min values
   */
  private double [] m_Min;
  
  /**
   * attribute max values
   */
  private double [] m_Max;

  /**
   * Keep track of the number of iterations completed before convergence
   */
  private int m_Iterations = 0;

  /**
   * Returns a string describing this clusterer
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Cluster data using the k means algorithm";
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
    m_Iterations = 0;
    if (data.checkForStringAttributes()) {
      throw  new Exception("Can't handle string attributes!");
    }

    m_ReplaceMissingFilter = new ReplaceMissingValues();
    m_ReplaceMissingFilter.setInputFormat(data);
    m_instances = Filter.useFilter(data, m_ReplaceMissingFilter);

    m_Min = new double [m_instances.numAttributes()];
    m_Max = new double [m_instances.numAttributes()];
    for (int i = 0; i < m_instances.numAttributes(); i++) {
      m_Min[i] = m_Max[i] = Double.NaN;
    }

    for (int i = 0; i < m_instances.numInstances(); i++) {
      updateMinMax(m_instances.instance(i));
    }
    
    m_ClusterCentroids = new Instances(m_instances, m_NumClusters);
    m_ClusterStdDevs = new Instances(m_instances, m_NumClusters);
    m_ClusterAssignments = new int [m_instances.numInstances()];

    Random RandomO = new Random(m_Seed);
    boolean [] selected = new boolean[m_instances.numInstances()];
    int instIndex;
    for (int i = 0; i < m_NumClusters; i++) {
      do {
	instIndex = Math.abs(RandomO.nextInt()) % 
	  m_instances.numInstances();
      } while (selected[instIndex]);
      m_ClusterCentroids.add(m_instances.instance(instIndex));
      selected[instIndex] = true;
    }
    selected = null;

    boolean converged = false;
    while (!converged) {
      m_Iterations++;
      converged = true;
      for (int i = 0; i < m_instances.numInstances(); i++) {
	Instance toCluster = m_instances.instance(i);
	int newC = clusterProcessedInstance(toCluster);
	if (newC != m_ClusterAssignments[i]) {
	  converged = false;
	}
	m_ClusterAssignments[i] = newC;
	//	System.out.println(newC);
      }
      
      Instances [] tempI = new Instances[m_NumClusters];
      // update centroids
      m_ClusterCentroids = new Instances(m_instances, m_NumClusters);
      for (int i = 0; i < m_NumClusters; i++) {
	tempI[i] = new Instances(m_instances, 0);
      }
      for (int i = 0; i < m_instances.numInstances(); i++) {
	tempI[m_ClusterAssignments[i]].add(m_instances.instance(i));
      }
      for (int i = 0; i < m_NumClusters; i++) {
	double [] vals = new double[m_instances.numAttributes()];
	double [] vals2 = new double[m_instances.numAttributes()];
	for (int j = 0; j < m_instances.numAttributes(); j++) {
	  vals[j] = tempI[i].meanOrMode(j);
	  if (m_instances.attribute(j).isNumeric()) {
	    Stats ns = tempI[i].attributeStats(j).numericStats;
	    vals2[j] = ns.stdDev;
	  } else {
	    vals2[j] = Instance.missingValue();
	  }
	}
	m_ClusterCentroids.add(new Instance(1.0, vals));
	m_ClusterStdDevs.add(new Instance(1.0, vals2));
      }
    }
  }

  /**
   * clusters an instance that has been through the filters
   *
   * @param instance the instance to assign a cluster to
   * @return a cluster number
   */
  private int clusterProcessedInstance(Instance instance) {
    double minDist = Integer.MAX_VALUE;
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
   * @return the number of the assigned cluster as an interger
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
  private double distance(Instance first, Instance second) {  

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
  private double difference(int index, double val1, double val2) {

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
  private double norm(double x, int i) {

    if (Double.isNaN(m_Min[i]) || Utils.eq(m_Max[i],m_Min[i])) {
      return 0;
    } else {
      return (x - m_Min[i]) / (m_Max[i] - m_Min[i]);
    }
  }

  /**
   * Updates the minimum and maximum values for all the attributes
   * based on a new instance.
   *
   * @param instance the new instance
   */
  private void updateMinMax(Instance instance) {  

    for (int j = 0;j < m_instances.numAttributes(); j++) {
      if (!instance.isMissing(j)) {
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
   * EM will use cross validation to select the number of clusters
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
  public void setNumClusters(int n) {
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
   * Gets the current settings of SimpleKMeans
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
    int maxWidth = 0;
    for (int i = 0; i < m_NumClusters; i++) {
      for (int j = 0 ;j < m_ClusterCentroids.numAttributes(); j++) {
	if (m_ClusterCentroids.attribute(j).isNumeric()) {
	  double width = Math.log(Math.abs(m_ClusterCentroids.instance(i).value(j))) /
	    Math.log(10.0);
	  width += 1.0;
	  if ((int)width > maxWidth) {
	    maxWidth = (int)width;
	  }
	}
      }
    }
    StringBuffer temp = new StringBuffer();
    String naString = "N/A";
    for (int i = 0; i < maxWidth+2; i++) {
      naString += " ";
    }
    temp.append("\nkMeans\n======\n");
    temp.append("\nNumber of iterations: " + m_Iterations+"\n");

    temp.append("\nCluster centroids:\n");
    for (int i = 0; i < m_NumClusters; i++) {
      temp.append("\nCluster "+i+"\n\t");
      temp.append("Mean/Mode: ");
      for (int j = 0; j < m_ClusterCentroids.numAttributes(); j++) {
	if (m_ClusterCentroids.attribute(j).isNominal()) {
	  temp.append(" "+m_ClusterCentroids.attribute(j).
		      value((int)m_ClusterCentroids.instance(i).value(j)));
	} else {
	  temp.append(" "+Utils.doubleToString(m_ClusterCentroids.instance(i).value(j),
					       maxWidth+5, 4));
	}
      }
      temp.append("\n\tStd Devs:  ");
      for (int j = 0; j < m_ClusterStdDevs.numAttributes(); j++) {
	if (m_ClusterStdDevs.attribute(j).isNumeric()) {
	  temp.append(" "+Utils.doubleToString(m_ClusterStdDevs.instance(i).value(j), 
					       maxWidth+5, 4));
	} else {
	  temp.append(" "+naString);
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
			 evaluateClusterer(new SimpleKMeans(), argv));
    }
    catch (Exception e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }
}
