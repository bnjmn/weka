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
 *    HyperPipes.java
 *    Copyright (C) 2002 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.misc;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.UnsupportedAttributeTypeException;
import weka.core.Utils;
import weka.core.Capabilities.Capability;

import java.io.Serializable;

/**
 <!-- globalinfo-start -->
 * Class implementing a HyperPipe classifier. For each category a HyperPipe is constructed that contains all points of that category (essentially records the attribute bounds observed for each category). Test instances are classified according to the category that "most contains the instance".<br/>
 * Does not handle numeric class, or missing values in test cases. Extremely simple algorithm, but has the advantage of being extremely fast, and works quite well when you have "smegloads" of attributes.
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
 <!-- options-end -->
 *
 * @author Lucio de Souza Coelho (lucio@intelligenesis.net)
 * @author Len Trigg (len@reeltwo.com)
 * @version $Revision: 1.20 $
 */ 
public class HyperPipes 
  extends Classifier {

  /** for serialization */
  static final long serialVersionUID = -7527596632268975274L;
  
  /** The index of the class attribute */
  protected int m_ClassIndex;

  /** The structure of the training data */
  protected Instances m_Instances;

  /** Stores the HyperPipe for each class */
  protected HyperPipe [] m_HyperPipes;

  /** a ZeroR model in case no model can be built from the data */
  protected Classifier m_ZeroR;
    
  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "Class implementing a HyperPipe classifier. For each category a "
    +  "HyperPipe is constructed that contains all points of that category "
      + "(essentially records the attribute bounds observed for each category). "
      + "Test instances are classified according to the category that \"most "
      + "contains the instance\".\n" 
      + "Does not handle numeric class, or missing values in test cases. Extremely "
      + "simple algorithm, but has the advantage of being extremely fast, and "
      + "works quite well when you have \"smegloads\" of attributes.";
  }

  /**
   * Represents an n-dimensional structure that bounds all instances 
   * passed to it (generally all of a given class value).
   */
  class HyperPipe 
    implements Serializable {
    
    /** for serialization */
    static final long serialVersionUID = 3972254260367902025L;

    /** Contains the numeric bounds of all instances in the HyperPipe */
    protected double [][] m_NumericBounds;

    /** Contains the nominal bounds of all instances in the HyperPipe */
    protected boolean [][] m_NominalBounds;

    /**
     * Creates the HyperPipe as the n-dimensional parallel-piped 
     * with minimum volume containing all the points in
     * pointSet.
     *
     * @param instances all instances belonging to the same class
     * @throws Exception if missing values are found
     */
    public HyperPipe(Instances instances) throws Exception {
      
      m_NumericBounds = new double [instances.numAttributes()][];
      m_NominalBounds = new boolean [instances.numAttributes()][];

      for (int i = 0; i < instances.numAttributes(); i++) {
	switch (instances.attribute(i).type()) {
	case Attribute.NUMERIC:
	  m_NumericBounds[i] = new double [2];
	  m_NumericBounds[i][0] = Double.POSITIVE_INFINITY;
	  m_NumericBounds[i][1] = Double.NEGATIVE_INFINITY;
	  break;
	case Attribute.NOMINAL:
	  m_NominalBounds[i] = new boolean [instances.attribute(i).numValues()];
	  break;
	default:
	  throw new UnsupportedAttributeTypeException("Cannot process string attributes!");
	}
      }

      for (int i = 0; i < instances.numInstances(); i++) {
	addInstance(instances.instance(i));
      }
    }


    /**
     * Updates the bounds arrays with a single instance. Missing values
     * are ignored (i.e. they don't change the bounds for that attribute)
     *
     * @param instance the instance
     * @throws Exception if any missing values are encountered
     */
    public void addInstance(Instance instance) throws Exception {

      for (int j = 0; j < instance.numAttributes(); j++) {
	if ((j != m_ClassIndex) && (!instance.isMissing(j))) {

	  double current = instance.value(j);

	  if (m_NumericBounds[j] != null) { // i.e. a numeric attribute
	    if (current < m_NumericBounds[j][0])
	      m_NumericBounds[j][0] = current;
	    if (current > m_NumericBounds[j][1])
	      m_NumericBounds[j][1] = current;

	  } else { // i.e. a nominal attribute
	    m_NominalBounds[j][(int) current] = true;
	  }
	}
      }
    }


    /**
     * Returns the fraction of the dimensions of a given instance with
     * values lying within the corresponding bounds of the HyperPipe.
     *
     * @param instance the instance
     * @return the fraction of dimensions
     * @throws Exception if any missing values are encountered
     */
    public double partialContains(Instance instance) throws Exception {
      
      int count = 0;
      for (int i = 0; i < instance.numAttributes(); i++) {

	if (i == m_ClassIndex) {
	  continue;
	}
	if (instance.isMissing(i)) {
	  continue;
	}

	double current = instance.value(i);

	if (m_NumericBounds[i] != null) { // i.e. a numeric attribute
	  if ((current >= m_NumericBounds[i][0]) 
	      && (current <= m_NumericBounds[i][1])) {
	    count++;
	  }
	} else { // i.e. a nominal attribute
	  if (m_NominalBounds[i][(int) current]) {
	    count++;
	  }
	}
      }

      return ((double)count) / (instance.numAttributes() - 1);
    }
  }


  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    // instances
    result.setMinimumNumberInstances(0);
    
    return result;
  }

  /**
   * Generates the classifier.
   *
   * @param instances set of instances serving as training data 
   * @throws Exception if the classifier has not been generated successfully
   */
  public void buildClassifier(Instances instances) throws Exception {
    
    // can classifier handle the data?
    getCapabilities().testWithFail(instances);

    // remove instances with missing class
    instances = new Instances(instances);
    instances.deleteWithMissingClass();
    
    // only class? -> build ZeroR model
    if (instances.numAttributes() == 1) {
      System.err.println(
	  "Cannot build model (only class attribute present in data!), "
	  + "using ZeroR model instead!");
      m_ZeroR = new weka.classifiers.rules.ZeroR();
      m_ZeroR.buildClassifier(instances);
      return;
    }
    else {
      m_ZeroR = null;
    }
    
    m_ClassIndex = instances.classIndex();
    m_Instances = new Instances(instances, 0); // Copy the structure for ref

    // Create the HyperPipe for each class
    m_HyperPipes = new HyperPipe [instances.numClasses()];
    for (int i = 0; i < m_HyperPipes.length; i++) {
      m_HyperPipes[i] = new HyperPipe(new Instances(instances, 0));
    }

    // Add the instances
    for (int i = 0; i < instances.numInstances(); i++) {
      updateClassifier(instances.instance(i));
    }
  }


  /**
   * Updates the classifier.
   *
   * @param instance the instance to be put into the classifier
   * @throws Exception if the instance could not be included successfully
   */
  public void updateClassifier(Instance instance) throws Exception {
  
    if (instance.classIsMissing()) {
      return;
    }
    m_HyperPipes[(int) instance.classValue()].addInstance(instance);
  }


  /**
   * Classifies the given test instance.
   *
   * @param instance the instance to be classified
   * @return the predicted class for the instance 
   * @throws Exception if the instance can't be classified
   */
  public double [] distributionForInstance(Instance instance) throws Exception {
        
    // default model?
    if (m_ZeroR != null) {
      return m_ZeroR.distributionForInstance(instance);
    }
    
    double [] dist = new double[m_HyperPipes.length];

    for (int j = 0; j < m_HyperPipes.length; j++) {
      dist[j] = m_HyperPipes[j].partialContains(instance);
    }

    double sum = Utils.sum(dist);
    if (sum <= 0) {
      for (int j = 0; j < dist.length; j++) {
	dist[j] = 1.0 / (double)dist.length;
      }
      return dist;
    } else {
      Utils.normalize(dist, sum);
      return dist;
    }
  }


  /**
   * Returns a description of this classifier.
   *
   * @return a description of this classifier as a string.
   */
  public String toString() {

    // only ZeroR model?
    if (m_ZeroR != null) {
      StringBuffer buf = new StringBuffer();
      buf.append(this.getClass().getName().replaceAll(".*\\.", "") + "\n");
      buf.append(this.getClass().getName().replaceAll(".*\\.", "").replaceAll(".", "=") + "\n\n");
      buf.append("Warning: No model could be built, hence ZeroR model is used:\n\n");
      buf.append(m_ZeroR.toString());
      return buf.toString();
    }
    
    if (m_HyperPipes == null) {
      return ("HyperPipes classifier");
    }

    StringBuffer text = new StringBuffer("HyperPipes classifier\n");

    /* Perhaps print out the bounds for each HyperPipe.
    for (int i = 0; i < m_HyperPipes.length; i++) {
      text.append("HyperPipe for class: " 
		  + m_Instances.attribute(m_ClassIndex).value(i) + "\n");
      text.append(m_HyperPipes[i] + "\n\n");
    }
    */

    return text.toString();
  }


  /**
   * Main method for testing this class.
   *
   * @param argv should contain command line arguments for evaluation
   * (see Evaluation).
   */
  public static void main(String [] argv) {
    runClassifier(new HyperPipes(), argv);
  }
}
