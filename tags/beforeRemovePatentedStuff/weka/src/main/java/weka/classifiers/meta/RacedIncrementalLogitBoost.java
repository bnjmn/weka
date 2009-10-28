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
 *    RacedIncrementalLogitBoost.java
 *    Copyright (C) 2002 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.meta;

import weka.classifiers.Classifier;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.RandomizableSingleClassifierEnhancer;
import weka.classifiers.UpdateableClassifier;
import weka.classifiers.rules.ZeroR;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.core.Capabilities.Capability;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * Classifier for incremental learning of large datasets by way of racing logit-boosted committees.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -C &lt;num&gt;
 *  Minimum size of chunks.
 *  (default 500)</pre>
 * 
 * <pre> -M &lt;num&gt;
 *  Maximum size of chunks.
 *  (default 2000)</pre>
 * 
 * <pre> -V &lt;num&gt;
 *  Size of validation set.
 *  (default 1000)</pre>
 * 
 * <pre> -P &lt;pruning type&gt;
 *  Committee pruning to perform.
 *  0=none, 1=log likelihood (default)</pre>
 * 
 * <pre> -Q
 *  Use resampling for boosting.</pre>
 * 
 * <pre> -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)</pre>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -W
 *  Full name of base classifier.
 *  (default: weka.classifiers.trees.DecisionStump)</pre>
 * 
 * <pre> 
 * Options specific to classifier weka.classifiers.trees.DecisionStump:
 * </pre>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 <!-- options-end -->
 *
 * Options after -- are passed to the designated learner.<p>
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision$ 
 */
public class RacedIncrementalLogitBoost 
  extends RandomizableSingleClassifierEnhancer
  implements UpdateableClassifier {
  
  /** for serialization */
  static final long serialVersionUID = 908598343772170052L;

  /** no pruning */
  public static final int PRUNETYPE_NONE = 0;
  /** log likelihood pruning */
  public static final int PRUNETYPE_LOGLIKELIHOOD = 1;
  /** The pruning types */
  public static final Tag [] TAGS_PRUNETYPE = {
    new Tag(PRUNETYPE_NONE, "No pruning"),
    new Tag(PRUNETYPE_LOGLIKELIHOOD, "Log likelihood pruning")
  };

  /** The committees */   
  protected FastVector m_committees;

  /** The pruning type used */
  protected int m_PruningType = PRUNETYPE_LOGLIKELIHOOD;

  /** Whether to use resampling */
  protected boolean m_UseResampling = false;

  /** The number of classes */
  protected int m_NumClasses;

  /** A threshold for responses (Friedman suggests between 2 and 4) */
  protected static final double Z_MAX = 4;

  /** Dummy dataset with a numeric class */
  protected Instances m_NumericClassData;

  /** The actual class attribute (for getting class names) */
  protected Attribute m_ClassAttribute;  

  /** The minimum chunk size used for training */
  protected int m_minChunkSize = 500;

  /** The maimum chunk size used for training */
  protected int m_maxChunkSize = 2000;

  /** The size of the validation set */
  protected int m_validationChunkSize = 1000;

  /** The number of instances consumed */  
  protected int m_numInstancesConsumed;

  /** The instances used for validation */    
  protected Instances m_validationSet;

  /** The instances currently in memory for training */   
  protected Instances m_currentSet;

  /** The current best committee */   
  protected Committee m_bestCommittee;

  /** The default scheme used when committees aren't ready */    
  protected ZeroR m_zeroR = null;

  /** Whether the validation set has recently been changed */ 
  protected boolean m_validationSetChanged;

  /** The maximum number of instances required for processing */   
  protected int m_maxBatchSizeRequired;

  /** The random number generator used */
  protected Random m_RandomInstance = null;

    
  /**
   * Constructor.
   */
  public RacedIncrementalLogitBoost() {
    
    m_Classifier = new weka.classifiers.trees.DecisionStump();
  }

  /**
   * String describing default classifier.
   * 
   * @return the default classifier classname
   */
  protected String defaultClassifierString() {
    
    return "weka.classifiers.trees.DecisionStump";
  }


  /** 
   * Class representing a committee of LogitBoosted models
   */
  protected class Committee 
    implements Serializable, RevisionHandler {
    
    /** for serialization */
    static final long serialVersionUID = 5559880306684082199L;

    protected int m_chunkSize;
    
    /** number eaten from m_currentSet */
    protected int m_instancesConsumed; 
    
    protected FastVector m_models;
    protected double m_lastValidationError;
    protected double m_lastLogLikelihood;
    protected boolean m_modelHasChanged;
    protected boolean m_modelHasChangedLL;
    protected double[][] m_validationFs;
    protected double[][] m_newValidationFs;

    /** 
     * constructor 
     * 
     * @param chunkSize the size of the chunk
     */
    public Committee(int chunkSize) {

      m_chunkSize = chunkSize;
      m_instancesConsumed = 0;
      m_models = new FastVector();
      m_lastValidationError = 1.0;
      m_lastLogLikelihood = Double.MAX_VALUE;
      m_modelHasChanged = true;
      m_modelHasChangedLL = true;
      m_validationFs = new double[m_validationChunkSize][m_NumClasses];
      m_newValidationFs = new double[m_validationChunkSize][m_NumClasses];
    } 

    /** 
     * update the committee 
     * 
     * @return true if the committee has changed
     * @throws Exception if anything goes wrong
     */
    public boolean update() throws Exception {

      boolean hasChanged = false;
      while (m_currentSet.numInstances() - m_instancesConsumed >= m_chunkSize) {
	Classifier[] newModel = boost(new Instances(m_currentSet, m_instancesConsumed, m_chunkSize));
	for (int i=0; i<m_validationSet.numInstances(); i++) {
	  m_newValidationFs[i] = updateFS(m_validationSet.instance(i), newModel, m_validationFs[i]);
	}
	m_models.addElement(newModel);
	m_instancesConsumed += m_chunkSize;
	hasChanged = true;
      }
      if (hasChanged) {
	m_modelHasChanged = true;
	m_modelHasChangedLL = true;
      }
      return hasChanged;
    }

    /** reset consumation counts */
    public void resetConsumed() {

      m_instancesConsumed = 0;
    }

    /** remove the last model from the committee */
    public void pruneLastModel() {

      if (m_models.size() > 0) {
	m_models.removeElementAt(m_models.size()-1);
	m_modelHasChanged = true;
	m_modelHasChangedLL = true;
      }
    }

    /** 
     * decide to keep the last model in the committee 
     * @throws Exception if anything goes wrong
     */
    public void keepLastModel() throws Exception {

      m_validationFs = m_newValidationFs;
      m_newValidationFs = new double[m_validationChunkSize][m_NumClasses];
      m_modelHasChanged = true;
      m_modelHasChangedLL = true;
    }

    /** 
     * calculate the log likelihood on the validation data 
     * @return the log likelihood
     * @throws Exception if computation fails
     */        
    public double logLikelihood() throws Exception {

      if (m_modelHasChangedLL) {

	Instance inst;
	double llsum = 0.0;
	for (int i=0; i<m_validationSet.numInstances(); i++) {
	  inst = m_validationSet.instance(i);
	  llsum += (logLikelihood(m_validationFs[i],(int) inst.classValue()));
	}
	m_lastLogLikelihood = llsum / (double) m_validationSet.numInstances();
	m_modelHasChangedLL = false;
      }
      return m_lastLogLikelihood;
    }

    /** 
     * calculate the log likelihood on the validation data after adding the last model 
     * @return the log likelihood
     * @throws Exception if computation fails
     */
    public double logLikelihoodAfter() throws Exception {

	Instance inst;
	double llsum = 0.0;
	for (int i=0; i<m_validationSet.numInstances(); i++) {
	  inst = m_validationSet.instance(i);
	  llsum += (logLikelihood(m_newValidationFs[i],(int) inst.classValue()));
	}
	return llsum / (double) m_validationSet.numInstances();
    }

    
    /** 
     * calculates the log likelihood of an instance 
     * @param Fs the Fs values
     * @param classIndex the class index
     * @return the log likelihood
     * @throws Exception if computation fails
     */
    private double logLikelihood(double[] Fs, int classIndex) throws Exception {

      return -Math.log(distributionForInstance(Fs)[classIndex]);
    }

    /** 
     * calculates the validation error of the committee 
     * @return the validation error
     * @throws Exception if computation fails
     */
    public double validationError() throws Exception {

      if (m_modelHasChanged) {

	Instance inst;
	int numIncorrect = 0;
	for (int i=0; i<m_validationSet.numInstances(); i++) {
	  inst = m_validationSet.instance(i);
	  if (classifyInstance(m_validationFs[i]) != inst.classValue())
	    numIncorrect++;
	}
	m_lastValidationError = (double) numIncorrect / (double) m_validationSet.numInstances();
	m_modelHasChanged = false;
      }
      return m_lastValidationError;
    }

    /** 
     * returns the chunk size used by the committee 
     * 
     * @return the chunk size
     */
    public int chunkSize() {

      return m_chunkSize;
    }

    /** 
     * returns the number of models in the committee 
     * 
     * @return the committee size
     */
    public int committeeSize() {

      return m_models.size();
    }

    
    /** 
     * classifies an instance (given Fs values) with the committee 
     * 
     * @param Fs the Fs values
     * @return the classification
     * @throws Exception if anything goes wrong
     */
    public double classifyInstance(double[] Fs) throws Exception {
      
      double [] dist = distributionForInstance(Fs);

      double max = 0;
      int maxIndex = 0;
      
      for (int i = 0; i < dist.length; i++) {
	if (dist[i] > max) {
	  maxIndex = i;
	  max = dist[i];
	}
      }
      if (max > 0) {
	return maxIndex;
      } else {
	return Utils.missingValue();
      }
    }

    /** 
     * classifies an instance with the committee 
     * 
     * @param instance the instance to classify
     * @return the classification
     * @throws Exception if anything goes wrong
     */
    public double classifyInstance(Instance instance) throws Exception {
      
      double [] dist = distributionForInstance(instance);
      switch (instance.classAttribute().type()) {
      case Attribute.NOMINAL:
	double max = 0;
	int maxIndex = 0;
	
	for (int i = 0; i < dist.length; i++) {
	  if (dist[i] > max) {
	    maxIndex = i;
	    max = dist[i];
	  }
	}
	if (max > 0) {
	  return maxIndex;
	} else {
	  return Utils.missingValue();
	}
      case Attribute.NUMERIC:
	return dist[0];
      default:
	return Utils.missingValue();
      }
    }

    /** 
     * returns the distribution the committee generates for an instance (given Fs values) 
     * 
     * @param Fs the Fs values
     * @return the distribution
     * @throws Exception if anything goes wrong
     */
    public double[] distributionForInstance(double[] Fs) throws Exception {
      
      double [] distribution = new double [m_NumClasses];
      for (int j = 0; j < m_NumClasses; j++) {
	distribution[j] = RtoP(Fs, j);
      }
      return distribution;
    }
    
    /** 
     * updates the Fs values given a new model in the committee 
     * 
     * @param instance the instance to use
     * @param newModel the new model
     * @param Fs the Fs values to update
     * @return the updated Fs values
     * @throws Exception if anything goes wrong
     */
    public double[] updateFS(Instance instance, Classifier[] newModel, double[] Fs) throws Exception {
      
      instance = (Instance)instance.copy();
      instance.setDataset(m_NumericClassData);
      
      double [] Fi = new double [m_NumClasses];
      double Fsum = 0;
      for (int j = 0; j < m_NumClasses; j++) {
	Fi[j] = newModel[j].classifyInstance(instance);
	Fsum += Fi[j];
      }
      Fsum /= m_NumClasses;
      
      double[] newFs = new double[Fs.length];
      for (int j = 0; j < m_NumClasses; j++) {
	newFs[j] = Fs[j] + ((Fi[j] - Fsum) * (m_NumClasses - 1) / m_NumClasses);
      }
      return newFs;
    }

    /** 
     * returns the distribution the committee generates for an instance
     * 
     * @param instance the instance to get the distribution for
     * @return the distribution
     * @throws Exception if anything goes wrong
     */
    public double[] distributionForInstance(Instance instance) throws Exception {

      instance = (Instance)instance.copy();
      instance.setDataset(m_NumericClassData);
      double [] Fs = new double [m_NumClasses]; 
      for (int i = 0; i < m_models.size(); i++) {
	double [] Fi = new double [m_NumClasses];
	double Fsum = 0;
	Classifier[] model = (Classifier[]) m_models.elementAt(i);
	for (int j = 0; j < m_NumClasses; j++) {
	  Fi[j] = model[j].classifyInstance(instance);
	  Fsum += Fi[j];
	}
	Fsum /= m_NumClasses;
	for (int j = 0; j < m_NumClasses; j++) {
	  Fs[j] += (Fi[j] - Fsum) * (m_NumClasses - 1) / m_NumClasses;
	}
      }
      double [] distribution = new double [m_NumClasses];
      for (int j = 0; j < m_NumClasses; j++) {
	distribution[j] = RtoP(Fs, j);
      }
      return distribution;
    }

    /** 
     * performs a boosting iteration, returning a new model for the committee
     * 
     * @param data the data to boost on
     * @return the new model
     * @throws Exception if anything goes wrong
     */
    protected Classifier[] boost(Instances data) throws Exception {
      
      Classifier[] newModel = AbstractClassifier.makeCopies(m_Classifier, m_NumClasses);
      
      // Create a copy of the data with the class transformed into numeric
      Instances boostData = new Instances(data);
      boostData.deleteWithMissingClass();
      int numInstances = boostData.numInstances();
      
      // Temporarily unset the class index
      int classIndex = data.classIndex();
      boostData.setClassIndex(-1);
      boostData.deleteAttributeAt(classIndex);
      boostData.insertAttributeAt(new Attribute("'pseudo class'"), classIndex);
      boostData.setClassIndex(classIndex);
      double [][] trainFs = new double [numInstances][m_NumClasses];
      double [][] trainYs = new double [numInstances][m_NumClasses];
      for (int j = 0; j < m_NumClasses; j++) {
	for (int i = 0, k = 0; i < numInstances; i++, k++) {
	  while (data.instance(k).classIsMissing()) k++;
	  trainYs[i][j] = (data.instance(k).classValue() == j) ? 1 : 0;
	}
      }
      
      // Evaluate / increment trainFs from the classifiers
      for (int x = 0; x < m_models.size(); x++) {
	for (int i = 0; i < numInstances; i++) {
	  double [] pred = new double [m_NumClasses];
	  double predSum = 0;
	  Classifier[] model = (Classifier[]) m_models.elementAt(x);
	  for (int j = 0; j < m_NumClasses; j++) {
	    pred[j] = model[j].classifyInstance(boostData.instance(i));
	    predSum += pred[j];
	  }
	  predSum /= m_NumClasses;
	  for (int j = 0; j < m_NumClasses; j++) {
	    trainFs[i][j] += (pred[j] - predSum) * (m_NumClasses-1) 
	      / m_NumClasses;
	  }
	}
      }

      for (int j = 0; j < m_NumClasses; j++) {
	
	// Set instance pseudoclass and weights
	for (int i = 0; i < numInstances; i++) {
	  double p = RtoP(trainFs[i], j);
	  Instance current = boostData.instance(i);
	  double z, actual = trainYs[i][j];
	  if (actual == 1) {
	    z = 1.0 / p;
	    if (z > Z_MAX) { // threshold
	      z = Z_MAX;
	    }
	  } else if (actual == 0) {
	    z = -1.0 / (1.0 - p);
	    if (z < -Z_MAX) { // threshold
	      z = -Z_MAX;
	    }
	  } else {
	    z = (actual - p) / (p * (1 - p));
	  }

	  double w = (actual - p) / z;
	  current.setValue(classIndex, z);
	  current.setWeight(numInstances * w);
	}
	
	Instances trainData = boostData;
	if (m_UseResampling) {
	  double[] weights = new double[boostData.numInstances()];
	  for (int kk = 0; kk < weights.length; kk++) {
	    weights[kk] = boostData.instance(kk).weight();
	  }
	  trainData = boostData.resampleWithWeights(m_RandomInstance, 
						    weights);
	}
	
	// Build the classifier
	newModel[j].buildClassifier(trainData);
      }      
      
      return newModel;
    }

    /** 
     * outputs description of the committee
     * 
     * @return a string representation of the classifier
     */
    public String toString() {
      
      StringBuffer text = new StringBuffer();
      
      text.append("RacedIncrementalLogitBoost: Best committee on validation data\n");
      text.append("Base classifiers: \n");
      
      for (int i = 0; i < m_models.size(); i++) {
	text.append("\nModel "+(i+1));
	Classifier[] cModels = (Classifier[]) m_models.elementAt(i);
	for (int j = 0; j < m_NumClasses; j++) {
	  text.append("\n\tClass " + (j + 1) 
		      + " (" + m_ClassAttribute.name() 
		      + "=" + m_ClassAttribute.value(j) + ")\n\n"
		      + cModels[j].toString() + "\n");
	}
      }
      text.append("Number of models: " +
		  m_models.size() + "\n");      
      text.append("Chunk size per model: " + m_chunkSize + "\n");
      
      return text.toString();
    }
    
    /**
     * Returns the revision string.
     * 
     * @return		the revision
     */
    public String getRevision() {
      return RevisionUtils.extract("$Revision$");
    }
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // class
    result.disableAllClasses();
    result.disableAllClassDependencies();
    result.enable(Capability.NOMINAL_CLASS);

    // instances
    result.setMinimumNumberInstances(0);
    
    return result;
  }

 /**
   * Builds the classifier.
   *
   * @param data the instances to train the classifier with
   * @throws Exception if something goes wrong
   */
  public void buildClassifier(Instances data) throws Exception {

    m_RandomInstance = new Random(m_Seed);

    Instances boostData;
    int classIndex = data.classIndex();

    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    // remove instances with missing class
    data = new Instances(data);
    data.deleteWithMissingClass();
    
    if (m_Classifier == null) {
      throw new Exception("A base classifier has not been specified!");
    }

    if (!(m_Classifier instanceof WeightedInstancesHandler) &&
	!m_UseResampling) {
      m_UseResampling = true;
    }

    m_NumClasses = data.numClasses();
    m_ClassAttribute = data.classAttribute();

    // Create a copy of the data with the class transformed into numeric
    boostData = new Instances(data);

    // Temporarily unset the class index
    boostData.setClassIndex(-1);
    boostData.deleteAttributeAt(classIndex);
    boostData.insertAttributeAt(new Attribute("'pseudo class'"), classIndex);
    boostData.setClassIndex(classIndex);
    m_NumericClassData = new Instances(boostData, 0);

    data.randomize(m_RandomInstance);

    // create the committees
    int cSize = m_minChunkSize;
    m_committees = new FastVector();
    while (cSize <= m_maxChunkSize) {
      m_committees.addElement(new Committee(cSize));
      m_maxBatchSizeRequired = cSize;
      cSize *= 2;
    }

    // set up for consumption
    m_validationSet = new Instances(data, m_validationChunkSize);
    m_currentSet = new Instances(data, m_maxBatchSizeRequired);
    m_bestCommittee = null;
    m_numInstancesConsumed = 0;

    // start eating what we've been given
    for (int i=0; i<data.numInstances(); i++) updateClassifier(data.instance(i));
  }

 /**
   * Updates the classifier.
   *
   * @param instance the next instance in the stream of training data
   * @throws Exception if something goes wrong
   */
  public void updateClassifier(Instance instance) throws Exception {

    m_numInstancesConsumed++;

    if (m_validationSet.numInstances() < m_validationChunkSize) {
      m_validationSet.add(instance);
      m_validationSetChanged = true;
    } else {
      m_currentSet.add(instance);
      boolean hasChanged = false;
      
      // update each committee
      for (int i=0; i<m_committees.size(); i++) {
	Committee c = (Committee) m_committees.elementAt(i);
	if (c.update()) {
	  
	  hasChanged = true;
	  
	  if (m_PruningType == PRUNETYPE_LOGLIKELIHOOD) {
	    double oldLL = c.logLikelihood();
	    double newLL = c.logLikelihoodAfter();
	    if (newLL >= oldLL && c.committeeSize() > 1) {
	      c.pruneLastModel();
	      if (m_Debug) System.out.println("Pruning " + c.chunkSize()+ " committee (" +
					      oldLL + " < " + newLL + ")");
	    } else c.keepLastModel();
	  } else c.keepLastModel(); // no pruning
	} 
      }
      if (hasChanged) {

	if (m_Debug) System.out.println("After consuming " + m_numInstancesConsumed
					+ " instances... (" + m_validationSet.numInstances()
					+ " + " + m_currentSet.numInstances()
					+ " instances currently in memory)");
	
	// find best committee
	double lowestError = 1.0;
	for (int i=0; i<m_committees.size(); i++) {
	  Committee c = (Committee) m_committees.elementAt(i);

	  if (c.committeeSize() > 0) {

	    double err = c.validationError();
	    double ll = c.logLikelihood();

	    if (m_Debug) System.out.println("Chunk size " + c.chunkSize() + " with "
					    + c.committeeSize() + " models, has validation error of "
					    + err + ", log likelihood of " + ll);
	    if (err < lowestError) {
	      lowestError = err;
	      m_bestCommittee = c;
	    }
	  }
	}
      }
      if (m_currentSet.numInstances() >= m_maxBatchSizeRequired) {
	m_currentSet = new Instances(m_currentSet, m_maxBatchSizeRequired);

	// reset consumation counts
	for (int i=0; i<m_committees.size(); i++) {
	  Committee c = (Committee) m_committees.elementAt(i);
	  c.resetConsumed();
	}
      }
    }
  }

  /**
   * Convert from function responses to probabilities
   *
   * @param Fs an array containing the responses from each function
   * @param j the class value of interest
   * @return the probability prediction for j
   * @throws Exception if can't normalize
   */
  protected static double RtoP(double []Fs, int j) 
    throws Exception {

    double maxF = -Double.MAX_VALUE;
    for (int i = 0; i < Fs.length; i++) {
      if (Fs[i] > maxF) {
	maxF = Fs[i];
      }
    }
    double sum = 0;
    double[] probs = new double[Fs.length];
    for (int i = 0; i < Fs.length; i++) {
      probs[i] = Math.exp(Fs[i] - maxF);
      sum += probs[i];
    }
    if (sum == 0) {
      throw new Exception("Can't normalize");
    }
    return probs[j] / sum;
  }

  /**
   * Computes class distribution of an instance using the best committee.
   * 
   * @param instance the instance to get the distribution for
   * @return the distribution
   * @throws Exception if anything goes wrong
   */
  public double[] distributionForInstance(Instance instance) throws Exception {

    if (m_bestCommittee != null) return m_bestCommittee.distributionForInstance(instance);
    else {
      if (m_validationSetChanged || m_zeroR == null) {
	m_zeroR = new ZeroR();
	m_zeroR.buildClassifier(m_validationSet);
	m_validationSetChanged = false;
      }
      return m_zeroR.distributionForInstance(instance);
    }
  }

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(9);

    newVector.addElement(new Option(
	      "\tMinimum size of chunks.\n"
	      +"\t(default 500)",
	      "C", 1, "-C <num>"));

    newVector.addElement(new Option(
	      "\tMaximum size of chunks.\n"
	      +"\t(default 2000)",
	      "M", 1, "-M <num>"));

    newVector.addElement(new Option(
	      "\tSize of validation set.\n"
	      +"\t(default 1000)",
	      "V", 1, "-V <num>"));

    newVector.addElement(new Option(
	      "\tCommittee pruning to perform.\n"
	      +"\t0=none, 1=log likelihood (default)",
	      "P", 1, "-P <pruning type>"));

    newVector.addElement(new Option(
	      "\tUse resampling for boosting.",
	      "Q", 0, "-Q"));


    Enumeration enu = super.listOptions();
    while (enu.hasMoreElements()) {
      newVector.addElement(enu.nextElement());
    }
    return newVector.elements();
  }


  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -C &lt;num&gt;
   *  Minimum size of chunks.
   *  (default 500)</pre>
   * 
   * <pre> -M &lt;num&gt;
   *  Maximum size of chunks.
   *  (default 2000)</pre>
   * 
   * <pre> -V &lt;num&gt;
   *  Size of validation set.
   *  (default 1000)</pre>
   * 
   * <pre> -P &lt;pruning type&gt;
   *  Committee pruning to perform.
   *  0=none, 1=log likelihood (default)</pre>
   * 
   * <pre> -Q
   *  Use resampling for boosting.</pre>
   * 
   * <pre> -S &lt;num&gt;
   *  Random number seed.
   *  (default 1)</pre>
   * 
   * <pre> -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   * <pre> -W
   *  Full name of base classifier.
   *  (default: weka.classifiers.trees.DecisionStump)</pre>
   * 
   * <pre> 
   * Options specific to classifier weka.classifiers.trees.DecisionStump:
   * </pre>
   * 
   * <pre> -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String minChunkSize = Utils.getOption('C', options);
    if (minChunkSize.length() != 0) {
      setMinChunkSize(Integer.parseInt(minChunkSize));
    } else {
      setMinChunkSize(500);
    }

    String maxChunkSize = Utils.getOption('M', options);
    if (maxChunkSize.length() != 0) {
      setMaxChunkSize(Integer.parseInt(maxChunkSize));
    } else {
      setMaxChunkSize(2000);
    }

    String validationChunkSize = Utils.getOption('V', options);
    if (validationChunkSize.length() != 0) {
      setValidationChunkSize(Integer.parseInt(validationChunkSize));
    } else {
      setValidationChunkSize(1000);
    }

    String pruneType = Utils.getOption('P', options);
    if (pruneType.length() != 0) {
      setPruningType(new SelectedTag(Integer.parseInt(pruneType), TAGS_PRUNETYPE));
    } else {
      setPruningType(new SelectedTag(PRUNETYPE_LOGLIKELIHOOD, TAGS_PRUNETYPE));
    }

    setUseResampling(Utils.getFlag('Q', options));

    super.setOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] superOptions = super.getOptions();
    String [] options = new String [superOptions.length + 9];

    int current = 0;

    if (getUseResampling()) {
      options[current++] = "-Q";
    }
    options[current++] = "-C"; options[current++] = "" + getMinChunkSize();

    options[current++] = "-M"; options[current++] = "" + getMaxChunkSize();

    options[current++] = "-V"; options[current++] = "" + getValidationChunkSize();

    options[current++] = "-P"; options[current++] = "" + m_PruningType;

    System.arraycopy(superOptions, 0, options, current, 
		     superOptions.length);

    current += superOptions.length;
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "Classifier for incremental learning of large datasets by way of racing logit-boosted committees.";
  }

  /**
   * Set the base learner.
   *
   * @param newClassifier 		the classifier to use.
   * @throws IllegalArgumentException 	if base classifier cannot handle numeric 
   * 					class
   */
  public void setClassifier(Classifier newClassifier) {
    Capabilities cap = newClassifier.getCapabilities();
    
    if (!cap.handles(Capability.NUMERIC_CLASS))
      throw new IllegalArgumentException("Base classifier cannot handle numeric class!");
      
    super.setClassifier(newClassifier);
  }

  /**
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String minChunkSizeTipText() {

    return "The minimum number of instances to train the base learner with.";
  }

  /**
   * Set the minimum chunk size
   *
   * @param chunkSize the minimum chunk size
   */
  public void setMinChunkSize(int chunkSize) {

    m_minChunkSize = chunkSize;
  }

  /**
   * Get the minimum chunk size
   *
   * @return the chunk size
   */
  public int getMinChunkSize() {

    return m_minChunkSize;
  }

  /**
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String maxChunkSizeTipText() {

    return "The maximum number of instances to train the base learner with. The chunk sizes used will start at minChunkSize and grow twice as large for as many times as they are less than or equal to the maximum size.";
  }

  /**
   * Set the maximum chunk size
   *
   * @param chunkSize the maximum chunk size
   */
  public void setMaxChunkSize(int chunkSize) {

    m_maxChunkSize = chunkSize;
  }

  /**
   * Get the maximum chunk size
   *
   * @return the chunk size
   */
  public int getMaxChunkSize() {

    return m_maxChunkSize;
  }

  /**
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String validationChunkSizeTipText() {

    return "The number of instances to hold out for validation. These instances will be taken from the beginning of the stream, so learning will not start until these instances have been consumed first.";
  }

  /**
   * Set the validation chunk size
   *
   * @param chunkSize the validation chunk size
   */
  public void setValidationChunkSize(int chunkSize) {

    m_validationChunkSize = chunkSize;
  }

  /**
   * Get the validation chunk size
   *
   * @return the chunk size
   */
  public int getValidationChunkSize() {

    return m_validationChunkSize;
  }

  /**
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String pruningTypeTipText() {

    return "The pruning method to use within each committee. Log likelihood pruning will discard new models if they have a negative effect on the log likelihood of the validation data.";
  }

  /**
   * Set the pruning type
   *
   * @param pruneType the pruning type
   */
  public void setPruningType(SelectedTag pruneType) {

    if (pruneType.getTags() == TAGS_PRUNETYPE) {
      m_PruningType = pruneType.getSelectedTag().getID();
    }
  }

  /**
   * Get the pruning type
   *
   * @return the type
   */
  public SelectedTag getPruningType() {

    return new SelectedTag(m_PruningType, TAGS_PRUNETYPE);
  }

  /**
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String useResamplingTipText() {

    return "Force the use of resampling data rather than using the weight-handling capabilities of the base classifier. Resampling is always used if the base classifier cannot handle weighted instances.";
  }

  /**
   * Set resampling mode
   *
   * @param r true if resampling should be done
   */
  public void setUseResampling(boolean r) {
    
    m_UseResampling = r;
  }

  /**
   * Get whether resampling is turned on
   *
   * @return true if resampling output is on
   */
  public boolean getUseResampling() {
    
    return m_UseResampling;
  }

  /**
   * Get the best committee chunk size
   * 
   * @return the best committee chunk size
   */
  public int getBestCommitteeChunkSize() {

    if (m_bestCommittee != null) {
      return m_bestCommittee.chunkSize();
    }
    else return 0;
  }

  /**
   * Get the number of members in the best committee
   * 
   * @return the number of members
   */
  public int getBestCommitteeSize() {

    if (m_bestCommittee != null) {
      return m_bestCommittee.committeeSize();
    }
    else return 0;
  }

  /**
   * Get the best committee's error on the validation data
   * 
   * @return the best committee's error
   */
  public double getBestCommitteeErrorEstimate() {

    if (m_bestCommittee != null) {
      try {
	return m_bestCommittee.validationError() * 100.0;
      } catch (Exception e) {
	System.err.println(e.getMessage());
	return 100.0;
      }
    }
    else return 100.0;
  }

  /**
   * Get the best committee's log likelihood on the validation data
   * 
   * @return best committee's log likelihood
   */
  public double getBestCommitteeLLEstimate() {

    if (m_bestCommittee != null) {
      try {
	return m_bestCommittee.logLikelihood();
      } catch (Exception e) {
	System.err.println(e.getMessage());
	return Double.MAX_VALUE;
      }
    }
    else return Double.MAX_VALUE;
  }
  
  /**
   * Returns description of the boosted classifier.
   *
   * @return description of the boosted classifier as a string
   */
  public String toString() {
        
    if (m_bestCommittee != null) {
      return m_bestCommittee.toString();
    } else {
      if ((m_validationSetChanged || m_zeroR == null) && m_validationSet != null
	  && m_validationSet.numInstances() > 0) {
	m_zeroR = new ZeroR();
	try {
	  m_zeroR.buildClassifier(m_validationSet);
	} catch (Exception e) {}
	m_validationSetChanged = false;
      }
      if (m_zeroR != null) {
	return ("RacedIncrementalLogitBoost: insufficient data to build model, resorting to ZeroR:\n\n"
		+ m_zeroR.toString());
      }
      else return ("RacedIncrementalLogitBoost: no model built yet.");
    }
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  /**
   * Main method for this class.
   * 
   * @param argv the commandline parameters
   */
  public static void main(String[] argv) {
    runClassifier(new RacedIncrementalLogitBoost(), argv);
  }
}
