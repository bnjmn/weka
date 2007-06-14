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
 * ClassificationViaClustering.java
 * Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.meta;

import weka.classifiers.Classifier;
import weka.classifiers.rules.ZeroR;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.Clusterer;
import weka.clusterers.SimpleKMeans;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;

import java.util.Enumeration;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * A simple meta-classifier that uses a clusterer for classification. For cluster algorithms that use a fixed number of clusterers, like SimpleKMeans, the user has to make sure that the number of clusters to generate are the same as the number of class labels in the dataset in order to obtain a useful model.<br/>
 * <br/>
 * Note: at prediction time, a missing value is returned if no cluster is found for the instance.<br/>
 * <br/>
 * The code is based on the 'clusters to classes' functionality of the weka.clusterers.ClusterEvaluation class by Mark Hall.
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -W
 *  Full name of clusterer.
 *  (default: weka.clusterers.SimpleKMeans)</pre>
 * 
 * <pre> 
 * Options specific to clusterer weka.clusterers.SimpleKMeans:
 * </pre>
 * 
 * <pre> -N &lt;num&gt;
 *  number of clusters.
 *  (default 2).</pre>
 * 
 * <pre> -S &lt;num&gt;
 *  Random number seed.
 *  (default 10)</pre>
 * 
 <!-- options-end -->
 *
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.2 $
 */
public class ClassificationViaClustering
  extends Classifier {
  
  /** for serialization */
  private static final long serialVersionUID = -5687069451420259135L;

  /** the cluster algorithm used (template) */
  protected Clusterer m_Clusterer;

  /** the actual cluster algorithm being used */
  protected Clusterer m_ActualClusterer;
  
  /** the original training data header */
  protected Instances m_OriginalHeader;
  
  /** the modified training data header */
  protected Instances m_ClusteringHeader;
  
  /** the mapping between clusters and classes */
  protected double[] m_ClustersToClasses;
  
  /** the default model */
  protected Classifier m_ZeroR;
  
  /**
   * default constructor
   */
  public ClassificationViaClustering() {
    super();
    
    m_Clusterer = new SimpleKMeans();
  }
  
  /**
   * Returns a string describing classifier
   * 
   * @return 		a description suitable for displaying in the
   *         		explorer/experimenter gui
   */
  public String globalInfo() {
    return 
        "A simple meta-classifier that uses a clusterer for classification. "
      + "For cluster algorithms that use a fixed number of clusterers, like "
      + "SimpleKMeans, the user has to make sure that the number of clusters "
      + "to generate are the same as the number of class labels in the dataset "
      + "in order to obtain a useful model.\n"
      + "\n"
      + "Note: at prediction time, a missing value is returned if no cluster "
      + "is found for the instance.\n"
      + "\n"
      + "The code is based on the 'clusters to classes' functionality of the "
      + "weka.clusterers.ClusterEvaluation class by Mark Hall.";
  }

  /**
   * Gets an enumeration describing the available options.
   *
   * @return 		an enumeration of all the available options.
   */
  public Enumeration listOptions(){
    Vector 		result;
    Enumeration		enm;

    result = new Vector();

    enm = super.listOptions();
    while (enm.hasMoreElements())
      result.addElement(enm.nextElement());

    result.addElement(new Option(
	"\tFull name of clusterer.\n"
	+ "\t(default: " + defaultClustererString() +")",
	"W", 1, "-W"));

    result.addElement(new Option(
	"",
	"", 0, "\nOptions specific to clusterer "
	+ m_Clusterer.getClass().getName() + ":"));
    enm = ((OptionHandler) m_Clusterer).listOptions();
    while (enm.hasMoreElements())
      result.addElement(enm.nextElement());

    return result.elements();
  }
  
  /**
   * returns the options of the current setup
   *
   * @return		the current options
   */
  public String[] getOptions(){
    int       		i;
    Vector<String>    	result;
    String[]  		options;

    result = new Vector<String>();

    result.add("-W");
    result.add("" + getClusterer().getClass().getName());
    
    options = super.getOptions();
    for (i = 0; i < options.length; i++)
      result.add(options[i]);

    if (getClusterer() instanceof OptionHandler) {
      result.add("--");
      options = ((OptionHandler) getClusterer()).getOptions();
      for (i = 0; i < options.length; i++)
        result.add(options[i]);
    }

    return result.toArray(new String[result.size()]);	  
  }

  /**
   * Parses the options for this object. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   * <pre> -W
   *  Full name of clusterer.
   *  (default: weka.clusterers.SimpleKMeans)</pre>
   * 
   * <pre> 
   * Options specific to clusterer weka.clusterers.SimpleKMeans:
   * </pre>
   * 
   * <pre> -N &lt;num&gt;
   *  number of clusters.
   *  (default 2).</pre>
   * 
   * <pre> -S &lt;num&gt;
   *  Random number seed.
   *  (default 10)</pre>
   * 
   <!-- options-end -->
   *
   * @param options	the options to use
   * @throws Exception	if setting of options fails
   */
  public void setOptions(String[] options) throws Exception {
    String	tmpStr;

    super.setOptions(options);

    tmpStr = Utils.getOption('W', options);
    if (tmpStr.length() > 0) { 
      // This is just to set the classifier in case the option 
      // parsing fails.
      setClusterer(Clusterer.forName(tmpStr, null));
      setClusterer(Clusterer.forName(tmpStr, Utils.partitionOptions(options)));
    }
    else {
      // This is just to set the classifier in case the option 
      // parsing fails.
      setClusterer(Clusterer.forName(defaultClustererString(), null));
      setClusterer(Clusterer.forName(defaultClustererString(), Utils.partitionOptions(options)));
    }
  }

  /**
   * String describing default clusterer.
   * 
   * @return		the classname
   */
  protected String defaultClustererString() {
    return SimpleKMeans.class.getName();
  }
  
  /**
   * Returns the tip text for this property
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String clustererTipText() {
    return "The clusterer to be used.";
  }

  /**
   * Set the base clusterer.
   *
   * @param value 	the clusterer to use.
   */
  public void setClusterer(Clusterer value) {
    m_Clusterer = value;
  }

  /**
   * Get the clusterer used as the base learner.
   *
   * @return 		the current clusterer
   */
  public Clusterer getClusterer() {
    return m_Clusterer;
  }

  /**
   * Classifies the given test instance.
   *
   * @param instance 	the instance to be classified
   * @return 		the predicted most likely class for the instance or 
   * 			Instance.missingValue() if no prediction is made
   * @throws Exception 	if an error occurred during the prediction
   */
  public double classifyInstance(Instance instance) throws Exception {
    double	result;
    double[]	values;
    Instance	newInst;
    int		i;
    int		n;
    
    if (m_ZeroR != null) {
      result = m_ZeroR.classifyInstance(instance);
    }
    else {
      if (m_ActualClusterer != null) {
	// build new instance
	values = new double[m_ClusteringHeader.numAttributes()];
	n = 0;
	for (i = 0; i < instance.numAttributes(); i++) {
	  if (i == instance.classIndex())
	    continue;
	  values[n] = instance.value(i);
	  n++;
	}
	newInst = new Instance(instance.weight(), values);
	newInst.setDataset(m_ClusteringHeader);

	// determine cluster/class
	result = m_ClustersToClasses[m_ActualClusterer.clusterInstance(newInst)];
	if (result == -1)
	  result = Instance.missingValue();
      }
      else {
	result = Instance.missingValue();
      }
    }
    
    return result;
  }
  
  /**
   * Returns default capabilities of the classifier.
   *
   * @return		the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities	result;
    
    result = m_Clusterer.getCapabilities();
    
    // class
    result.disableAllClasses();
    result.disable(Capability.NO_CLASS);
    result.enable(Capability.NOMINAL_CLASS);
    
    return result;
  }

  /**
   * builds the classifier
   * 
   * @param data        the training instances
   * @throws Exception  if something goes wrong
   */
  public void buildClassifier(Instances data) throws Exception {
    Instances		clusterData;
    ClusterEvaluation	eval;
    int			i;
    Instance		instance;
    int[][] 		counts;
    int[] 		clusterTotals;
    double[] 		best;
    double[] 		current;
    double[] 		clusterAssignments;
    
    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    // remove instances with missing class
    data = new Instances(data);
    data.deleteWithMissingClass();

    // save original header (needed for clusters to classes output)
    m_OriginalHeader = new Instances(data, 0);
    
    // remove class attribute for clusterer
    clusterData = new Instances(data);
    clusterData.setClassIndex(-1);
    clusterData.deleteAttributeAt(m_OriginalHeader.classIndex());
    m_ClusteringHeader = new Instances(clusterData, 0);

    if (m_ClusteringHeader.numAttributes() == 0) {
      System.err.println("Data contains only class attribute, defaulting to ZeroR model.");
      m_ZeroR = new ZeroR();
      m_ZeroR.buildClassifier(data);
    }
    else {
      m_ZeroR = null;
      
      // build clusterer
      m_ActualClusterer = Clusterer.makeCopy(m_Clusterer);
      m_ActualClusterer.buildClusterer(clusterData);

      // evaluate clusterer on training set
      eval = new ClusterEvaluation();
      eval.setClusterer(m_ActualClusterer);
      eval.evaluateClusterer(clusterData);
      clusterAssignments = eval.getClusterAssignments();

      // determine classes-to-clusters mapping
      counts        = new int [eval.getNumClusters()][m_OriginalHeader.numClasses()];
      clusterTotals = new int[eval.getNumClusters()];
      best          = new double[eval.getNumClusters()+1];
      current       = new double[eval.getNumClusters()+1];
      for (i = 0; i < data.numInstances(); i++) {
	instance = data.instance(i);
	counts[(int) clusterAssignments[i]][(int) instance.classValue()]++;
	clusterTotals[(int) clusterAssignments[i]]++;
	i++;
      }
      best[eval.getNumClusters()] = Double.MAX_VALUE;
      ClusterEvaluation.mapClasses(eval.getNumClusters(), 0, counts, clusterTotals, current, best, 0);
      m_ClustersToClasses = new double[best.length];
      System.arraycopy(best, 0, m_ClustersToClasses, 0, best.length);
    }
  }

  /**
   * Returns a string representation of the classifier.
   * 
   * @return		a string representation of the classifier.
   */
  public String toString() {
    StringBuffer	result;
    int			i;
    int			n;
    boolean		found;
    
    result = new StringBuffer();
    
    // title
    result.append(this.getClass().getName().replaceAll(".*\\.", "") + "\n");
    result.append(this.getClass().getName().replaceAll(".*\\.", "").replaceAll(".", "=") + "\n");
    
    // model
    if (m_ActualClusterer != null) {
      // output clusterer
      result.append(m_ActualClusterer + "\n");
      
      // clusters to classes
      result.append("Clusters to classes mapping:\n");
      for (i = 0; i < m_ClustersToClasses.length - 1; i++) {
	result.append("  " + (i+1) + ". Cluster: ");
	if (m_ClustersToClasses[i] < 0)
	  result.append("no class");
	else
	  result.append(
	      m_OriginalHeader.classAttribute().value((int) m_ClustersToClasses[i])
	      + " (" + ((int) m_ClustersToClasses[i] + 1) + ")");
	result.append("\n");
      }
      result.append("\n");
      
      // classes to clusters
      result.append("Classes to clusters mapping:\n");
      for (i = 0; i < m_OriginalHeader.numClasses(); i++) {
	result.append(
	    "  " + (i+1) + ". Class (" 
	    + m_OriginalHeader.classAttribute().value(i) + "): ");
	
	found = false;
	for (n = 0; n < m_ClustersToClasses.length - 1; n++) {
	  if (((int) m_ClustersToClasses[n]) == i) {
	    found = true;
	    result.append((n+1) + ". Cluster");
	    break;
	  }
	}
	
	if (!found)
	  result.append("no cluster");

	result.append("\n");
      }
      
      result.append("\n");
    }
    else {
      result.append("no model built yet\n");
    }
    
    return result.toString();
  }
  
  /**
   * Runs the classifier with the given options
   * 
   * @param args	the commandline options
   */
  public static void main(String[] args) {
    runClassifier(new ClassificationViaClustering(), args);
  }
}
