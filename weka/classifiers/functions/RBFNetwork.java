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
 *    RBFNetwork.java
 *    Copyright (C) 2004 Mark Hall
 *
 */
package weka.classifiers.functions;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.clusterers.MakeDensityBasedClusterer;
import weka.clusterers.SimpleKMeans;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.SelectedTag;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.ClusterMembership;
import weka.filters.unsupervised.attribute.Standardize;

import java.util.Enumeration;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * Class that implements a normalized Gaussian radial basisbasis function network.<br/>
 * It uses the k-means clustering algorithm to provide the basis functions and learns either a logistic regression (discrete class problems) or linear regression (numeric class problems) on top of that. Symmetric multivariate Gaussians are fit to the data from each cluster. If the class is nominal it uses the given number of clusters per class.It standardizes all numeric attributes to zero mean and unit variance.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -B &lt;number&gt;
 *  Set the number of clusters (basis functions) to generate. (default = 2).</pre>
 * 
 * <pre> -S &lt;seed&gt;
 *  Set the random seed to be used by K-means. (default = 1).</pre>
 * 
 * <pre> -R &lt;ridge&gt;
 *  Set the ridge value for the logistic or linear regression.</pre>
 * 
 * <pre> -M &lt;number&gt;
 *  Set the maximum number of iterations for the logistic regression. (default -1, until convergence).</pre>
 * 
 * <pre> -W &lt;number&gt;
 *  Set the minimum standard deviation for the clusters. (default 0.1).</pre>
 * 
 <!-- options-end -->
 *
 * @author Mark Hall
 * @author Eibe Frank
 * @version $Revision: 1.6 $
 */
public class RBFNetwork extends Classifier implements OptionHandler {

  /** for serialization */
  static final long serialVersionUID = -3669814959712675720L;
  
  /** The logistic regression for classification problems */
  private Logistic m_logistic;

  /** The linear regression for numeric problems */
  private LinearRegression m_linear;

  /** The filter for producing the meta data */
  private ClusterMembership m_basisFilter;

  /** Filter used for normalizing the data */
  private Standardize m_standardize;

  /** The number of clusters (basis functions to generate) */
  private int m_numClusters = 2;

  /** The ridge parameter for the logistic regression. */
  protected double m_ridge = 1e-8;

  /** The maximum number of iterations for logistic regression. */
  private int m_maxIts = -1;

  /** The seed to pass on to K-means */
  private int m_clusteringSeed = 1;

  /** The minimum standard deviation */
  private double m_minStdDev = 0.1;

  /**
   * Returns a string describing this classifier
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Class that implements a normalized Gaussian radial basis" 
      + "basis function network.\n"
      + "It uses the k-means clustering algorithm to provide the basis "
      + "functions and learns either a logistic regression (discrete "
      + "class problems) or linear regression (numeric class problems) "
      + "on top of that. Symmetric multivariate Gaussians are fit to "
      + "the data from each cluster. If the class is "
      + "nominal it uses the given number of clusters per class."
      + "It standardizes all numeric "
      + "attributes to zero mean and unit variance." ;
  }

  /**
   * Returns default capabilities of the classifier, i.e.,  and "or" of
   * Logistic and LinearRegression.
   *
   * @return      the capabilities of this classifier
   * @see         Logistic
   * @see         LinearRegression
   */
  public Capabilities getCapabilities() {
    Capabilities result = new Logistic().getCapabilities();
    result.or(new LinearRegression().getCapabilities());
    Capabilities classes = result.getClassCapabilities();
    result.and(new SimpleKMeans().getCapabilities());
    result.or(classes);
    return result;
  }

  /**
   * Builds the classifier
   *
   * @param instances the training data
   * @throws Exception if the classifier could not be built successfully
   */
  public void buildClassifier(Instances instances) throws Exception {

    // can classifier handle the data?
    getCapabilities().testWithFail(instances);

    // remove instances with missing class
    instances = new Instances(instances);
    instances.deleteWithMissingClass();
    
    m_standardize = new Standardize();
    m_standardize.setInputFormat(instances);
    instances = Filter.useFilter(instances, m_standardize);

    SimpleKMeans sk = new SimpleKMeans();
    sk.setNumClusters(m_numClusters);
    sk.setSeed(m_clusteringSeed);
    MakeDensityBasedClusterer dc = new MakeDensityBasedClusterer();
    dc.setClusterer(sk);
    dc.setMinStdDev(m_minStdDev);
    m_basisFilter = new ClusterMembership();
    m_basisFilter.setDensityBasedClusterer(dc);
    m_basisFilter.setInputFormat(instances);
    Instances transformed = Filter.useFilter(instances, m_basisFilter);

    if (instances.classAttribute().isNominal()) {
      m_linear = null;
      m_logistic = new Logistic();
      m_logistic.setRidge(m_ridge);
      m_logistic.setMaxIts(m_maxIts);
      m_logistic.buildClassifier(transformed);
    } else {
      m_logistic = null;
      m_linear = new LinearRegression();
      m_linear.setAttributeSelectionMethod(new SelectedTag(LinearRegression.SELECTION_NONE,
							   LinearRegression.TAGS_SELECTION));
      m_linear.setRidge(m_ridge);
      m_linear.buildClassifier(transformed);
    }
  }

  /**
   * Computes the distribution for a given instance
   *
   * @param instance the instance for which distribution is computed
   * @return the distribution
   * @throws Exception if the distribution can't be computed successfully
   */
  public double [] distributionForInstance(Instance instance) 
    throws Exception {

    m_standardize.input(instance);
    m_basisFilter.input(m_standardize.output());
    Instance transformed = m_basisFilter.output();
    
    return ((instance.classAttribute().isNominal()
	     ? m_logistic.distributionForInstance(transformed)
	     : m_linear.distributionForInstance(transformed)));
  }
  
  /**
   * Returns a description of this classifier as a String
   *
   * @return a description of this classifier
   */
  public String toString() {

    if (m_basisFilter == null) {
      return "No classifier built yet!";
    }

    StringBuffer sb = new StringBuffer();
    sb.append("Radial basis function network\n");
    sb.append((m_linear == null) 
	      ? "(Logistic regression "
	      : "(Linear regression ");
    sb.append("applied to K-means clusters as basis functions):\n\n");
    sb.append((m_linear == null)
	      ? m_logistic.toString()
	      : m_linear.toString());
    return sb.toString();
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String maxItsTipText() {
    return "Maximum number of iterations for the logistic regression to perform. "
      +"Only applied to discrete class problems.";
  }

  /**
   * Get the value of MaxIts.
   *
   * @return Value of MaxIts.
   */
  public int getMaxIts() {
	
    return m_maxIts;
  }
    
  /**
   * Set the value of MaxIts.
   *
   * @param newMaxIts Value to assign to MaxIts.
   */
  public void setMaxIts(int newMaxIts) {
	
    m_maxIts = newMaxIts;
  }    

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String ridgeTipText() {
    return "Set the Ridge value for the logistic or linear regression.";
  }

  /**
   * Sets the ridge value for logistic or linear regression.
   *
   * @param ridge the ridge
   */
  public void setRidge(double ridge) {
    m_ridge = ridge;
  }
    
  /**
   * Gets the ridge value.
   *
   * @return the ridge
   */
  public double getRidge() {
    return m_ridge;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numClustersTipText() {
    return "The number of clusters for K-Means to generate.";
  }

  /**
   * Set the number of clusters for K-means to generate.
   *
   * @param numClusters the number of clusters to generate.
   */
  public void setNumClusters(int numClusters) {
    if (numClusters > 0) {
      m_numClusters = numClusters;
    }
  }

  /**
   * Return the number of clusters to generate.
   *
   * @return the number of clusters to generate.
   */
  public int getNumClusters() {
    return m_numClusters;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String clusteringSeedTipText() {
    return "The random seed to pass on to K-means.";
  }
  
  /**
   * Set the random seed to be passed on to K-means.
   *
   * @param seed a seed value.
   */
  public void setClusteringSeed(int seed) {
    m_clusteringSeed = seed;
  }

  /**
   * Get the random seed used by K-means.
   *
   * @return the seed value.
   */
  public int getClusteringSeed() {
    return m_clusteringSeed;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String minStdDevTipText() {
    return "Sets the minimum standard deviation for the clusters.";
  }

  /**
   * Get the MinStdDev value.
   * @return the MinStdDev value.
   */
  public double getMinStdDev() {
    return m_minStdDev;
  }

  /**
   * Set the MinStdDev value.
   * @param newMinStdDev The new MinStdDev value.
   */
  public void setMinStdDev(double newMinStdDev) {
    m_minStdDev = newMinStdDev;
  }

  
  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {
    Vector newVector = new Vector(4);

    newVector.addElement(new Option("\tSet the number of clusters (basis functions) "
				    +"to generate. (default = 2).",
				    "B", 1, "-B <number>"));
    newVector.addElement(new Option("\tSet the random seed to be used by K-means. "
				    +"(default = 1).",
				    "S", 1, "-S <seed>"));
    newVector.addElement(new Option("\tSet the ridge value for the logistic or "
				    +"linear regression.",
				    "R", 1, "-R <ridge>"));
    newVector.addElement(new Option("\tSet the maximum number of iterations "
				    +"for the logistic regression."
				    + " (default -1, until convergence).",
				    "M", 1, "-M <number>"));
    newVector.addElement(new Option("\tSet the minimum standard "
				    +"deviation for the clusters."
				    + " (default 0.1).",
				    "W", 1, "-W <number>"));
    return newVector.elements();
  }

  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -B &lt;number&gt;
   *  Set the number of clusters (basis functions) to generate. (default = 2).</pre>
   * 
   * <pre> -S &lt;seed&gt;
   *  Set the random seed to be used by K-means. (default = 1).</pre>
   * 
   * <pre> -R &lt;ridge&gt;
   *  Set the ridge value for the logistic or linear regression.</pre>
   * 
   * <pre> -M &lt;number&gt;
   *  Set the maximum number of iterations for the logistic regression. (default -1, until convergence).</pre>
   * 
   * <pre> -W &lt;number&gt;
   *  Set the minimum standard deviation for the clusters. (default 0.1).</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    setDebug(Utils.getFlag('D', options));

    String ridgeString = Utils.getOption('R', options);
    if (ridgeString.length() != 0) {
      m_ridge = Double.parseDouble(ridgeString);
    } else {
      m_ridge = 1.0e-8;
    }
	
    String maxItsString = Utils.getOption('M', options);
    if (maxItsString.length() != 0) {
      m_maxIts = Integer.parseInt(maxItsString);
    } else {
      m_maxIts = -1;
    }

    String numClustersString = Utils.getOption('B', options);
    if (numClustersString.length() != 0) {
      setNumClusters(Integer.parseInt(numClustersString));
    }

    String seedString = Utils.getOption('S', options);
    if (seedString.length() != 0) {
      setClusteringSeed(Integer.parseInt(seedString));
    }
    String stdString = Utils.getOption('W', options);
    if (stdString.length() != 0) {
      setMinStdDev(Double.parseDouble(stdString));
    }
    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {
	
    String [] options = new String [10];
    int current = 0;
    
    options[current++] = "-B";
    options[current++] = "" + m_numClusters;
    options[current++] = "-S";
    options[current++] = "" + m_clusteringSeed;
    options[current++] = "-R";
    options[current++] = ""+m_ridge;	
    options[current++] = "-M";
    options[current++] = ""+m_maxIts;
    options[current++] = "-W";
    options[current++] = ""+m_minStdDev;

    while (current < options.length) 
      options[current++] = "";
    return options;
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain the command line arguments to the
   * scheme (see Evaluation)
   */
  public static void main(String [] argv) {
    try {
      System.out.println(Evaluation.evaluateModel(new RBFNetwork(), argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
}
