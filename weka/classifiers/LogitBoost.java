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
 *    LogitBoost.java
 *    Copyright (C) 1999 Len Trigg
 *
 */

package weka.classifiers;

import java.io.*;
import java.util.*;
import weka.core.*;

/**
 * Class for boosting any classifier that can handle weighted instances.
 * This class performs classification using a regression scheme as the 
 * base learner, and can handle multi-class problems.  For more
 * information, see<p>
 * 
 * Friedman, J., T. Hastie and R. Tibshirani (1998) <i>Additive Logistic
 * Regression: a Statistical View of Boosting</i> 
 * <a href="ftp://stat.stanford.edu/pub/friedman/boost.ps">download 
 * postscript</a>. <p>
 *
 * Valid options are:<p>
 *
 * -D <br>
 * Turn on debugging output.<p>
 *
 * -W classname <br>
 * Specify the full class name of a weak learner as the basis for 
 * boosting (required).<p>
 *
 * -I num <br>
 * Set the number of boost iterations (default 10). <p>
 *
 * -Q <br>
 * Use resampling instead of reweighting.<p>
 *
 * -S seed <br>
 * Random number seed for resampling (default 1).<p>
 *
 * -P num <br>
 * Set the percentage of weight mass used to build classifiers
 * (default 100). <p>
 *
 * Options after -- are passed to the designated learner.<p>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.19 $
 */
public class LogitBoost extends DistributionClassifier 
  implements OptionHandler, Sourcable {

  // To maintain the same version number after adding m_ClassAttribute
  static final long serialVersionUID = -217733168393629381L;

  /** Array for storing the generated base classifiers. */
  protected Classifier [][] m_Classifiers;

  /** An instantiated base classifier used for getting and testing options */
  protected Classifier m_Classifier = new weka.classifiers.DecisionStump();

  /** The maximum number of boost iterations */
  protected int m_MaxIterations = 10;

  /** The number of classes */
  protected int m_NumClasses;

  /** The number of successfully generated base classifiers. */
  protected int m_NumIterations;

  /** Weight thresholding. The percentage of weight mass used in training */
  protected int m_WeightThreshold = 100;

  /** Debugging mode, gives extra output if true */
  protected boolean m_Debug;

  /** A very small number, below which weights cannot fall */
  protected static final double VERY_SMALL = 2 * Double.MIN_VALUE;

  /** A threshold for responses (Friedman suggests between 2 and 4) */
  protected static final double Z_MAX = 4;

  /** Dummy dataset with a numeric class */
  protected Instances m_NumericClassData;

  /** The actual class attribute (for getting class names) */
  protected Attribute m_ClassAttribute;

  /** Use boosting with reweighting? */
  protected boolean m_UseResampling;
  
  /** Seed for boosting with resampling. */
  protected int m_Seed = 1;

  /**
   * Select only instances with weights that contribute to 
   * the specified quantile of the weight distribution
   *
   * @param data the input instances
   * @param quantile the specified quantile eg 0.9 to select 
   * 90% of the weight mass
   * @return the selected instances
   */
  protected Instances selectWeightQuantile(Instances data, double quantile) { 

    int numInstances = data.numInstances();
    Instances trainData = new Instances(data, numInstances);
    double [] weights = new double [numInstances];

    double sumOfWeights = 0;
    for (int i = 0; i < numInstances; i++) {
      weights[i] = data.instance(i).weight();
      sumOfWeights += weights[i];
    }
    double weightMassToSelect = sumOfWeights * quantile;
    int [] sortedIndices = Utils.sort(weights);

    // Select the instances
    sumOfWeights = 0;
    for (int i = numInstances-1; i >= 0; i--) {
      Instance instance = (Instance)data.instance(sortedIndices[i]).copy();
      trainData.add(instance);
      sumOfWeights += weights[sortedIndices[i]];
      if ((sumOfWeights > weightMassToSelect) && 
	  (i > 0) && 
	  (weights[sortedIndices[i]] != weights[sortedIndices[i-1]])) {
	break;
      }
    }
    if (m_Debug) {
      System.err.println("Selected " + trainData.numInstances()
			 + " out of " + numInstances);
    }
    return trainData;
  }

  /**
   * Convert from function responses to probabilities
   *
   * @param R an array containing the responses from each function
   * @param j the class value of interest
   * @return the probability prediction for j
   */
  protected static double RtoP(double []R, int j) {

    double Rcenter = 0;
    for (int i = 0; i < R.length; i++) {
      Rcenter += R[i];
    }
    Rcenter /= R.length;
    double Rsum = 0;
    for (int i = 0; i < R.length; i++) {
      Rsum += Math.exp(R[i] - Rcenter);
    }
   return Math.exp(R[j]) / Rsum;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(3);

    newVector.addElement(new Option(
	      "\tTurn on debugging output.",
	      "D", 0, "-D"));
    newVector.addElement(new Option(
	      "\tMaximum number of boost iterations.\n"
	      +"\t(default 10)",
	      "I", 1, "-I <num>"));
    newVector.addElement(new Option(
	      "\tUse resampling for boosting.",
	      "Q", 0, "-Q"));
    newVector.addElement(new Option(
	      "\tSeed for resampling. (Default 1)",
	      "S", 1, "-S <num>"));
    newVector.addElement(new Option(
	      "\tPercentage of weight mass to base training on.\n"
	      +"\t(default 100, reduce to around 90 speed up)",
	      "P", 1, "-P <percent>"));
    newVector.addElement(new Option(
	      "\tFull name of 'weak' learner to boost.\n"
	      +"\teg: weka.classifiers.DecisionStump",
	      "W", 1, "-W <learner class name>"));

    if ((m_Classifier != null) &&
	(m_Classifier instanceof OptionHandler)) {
      newVector.addElement(new Option(
	  "",
	  "", 0, "\nOptions specific to weak learner "
	  + m_Classifier.getClass().getName() + ":"));
      Enumeration enum = ((OptionHandler)m_Classifier).listOptions();
      while (enum.hasMoreElements()) {
	newVector.addElement(enum.nextElement());
      }
    }
    return newVector.elements();
  }


  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -D <br>
   * Turn on debugging output.<p>
   *
   * -W classname <br>
   * Specify the full class name of a weak learner as the basis for 
   * boosting (required).<p>
   *
   * -I num <br>
   * Set the number of boost iterations (default 10). <p>
   *
   * -Q <br>
   * Use resampling instead of reweighting.<p>
   * -S seed <br>
   * Random number seed for resampling (default 1).<p>
   *
   * -P num <br>
   * Set the percentage of weight mass used to build classifiers
   * (default 100). <p>
   *
   * Options after -- are passed to the designated learner.<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    setDebug(Utils.getFlag('D', options));
    
    String boostIterations = Utils.getOption('I', options);
    if (boostIterations.length() != 0) {
      setMaxIterations(Integer.parseInt(boostIterations));
    } else {
      setMaxIterations(10);
    }

    String thresholdString = Utils.getOption('P', options);
    if (thresholdString.length() != 0) {
      setWeightThreshold(Integer.parseInt(thresholdString));
    } else {
      setWeightThreshold(100);
    }

    setUseResampling(Utils.getFlag('Q', options));
    if (m_UseResampling && (thresholdString.length() != 0)) {
      throw new Exception("Weight pruning with resampling"+
			  "not allowed.");
    }

    String seedString = Utils.getOption('S', options);
    if (seedString.length() != 0) {
      setSeed(Integer.parseInt(seedString));
    } else {
      setSeed(1);
    }

    String classifierName = Utils.getOption('W', options);
    if (classifierName.length() == 0) {
      throw new Exception("A classifier must be specified with"
			  + " the -W option.");
    }
    setClassifier(Classifier.forName(classifierName,
				     Utils.partitionOptions(options)));
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] classifierOptions = new String [0];
    if ((m_Classifier != null) && 
	(m_Classifier instanceof OptionHandler)) {
      classifierOptions = ((OptionHandler)m_Classifier).getOptions();
    }

    String [] options = new String [classifierOptions.length + 9];
    int current = 0;
    if (getDebug()) {
      options[current++] = "-D";
    }
    
    if (getUseResampling()) {
      options[current++] = "-Q";
    } else {
      options[current++] = "-P"; 
      options[current++] = "" + getWeightThreshold();
    }
    options[current++] = "-I"; options[current++] = "" + getMaxIterations();

    if (getClassifier() != null) {
      options[current++] = "-W";
      options[current++] = getClassifier().getClass().getName();
    }
    options[current++] = "--";

    System.arraycopy(classifierOptions, 0, options, current, 
		     classifierOptions.length);
    current += classifierOptions.length;
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Set resampling mode
   *
   * @param resampling true if resampling should be done
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
   * Set seed for resampling.
   *
   * @param seed the seed for resampling
   */
  public void setSeed(int seed) {

    m_Seed = seed;
  }

  /**
   * Get seed for resampling.
   *
   * @return the seed for resampling
   */
  public int getSeed() {

    return m_Seed;
  }

  /**
   * Set the classifier for boosting. The learner should be able to
   * handle numeric class attributes.
   *
   * @param newClassifier the Classifier to use.
   */
  public void setClassifier(Classifier newClassifier) {

    m_Classifier = newClassifier;
  }

  /**
   * Get the classifier used as the classifier
   *
   * @return the classifier used as the classifier
   */
  public Classifier getClassifier() {

    return m_Classifier;
  }


  /**
   * Set the maximum number of boost iterations
   *
   * @param maxIterations the maximum number of boost iterations
   */
  public void setMaxIterations(int maxIterations) {

    m_MaxIterations = maxIterations;
  }

  /**
   * Get the maximum number of boost iterations
   *
   * @return the maximum number of boost iterations
   */
  public int getMaxIterations() {

    return m_MaxIterations;
  }


  /**
   * Set weight thresholding
   *
   * @param thresholding the percentage of weight mass used for training
   */
  public void setWeightThreshold(int threshold) {

    m_WeightThreshold = threshold;
  }

  /**
   * Get the degree of weight thresholding
   *
   * @return the percentage of weight mass used for training
   */
  public int getWeightThreshold() {

    return m_WeightThreshold;
  }

  /**
   * Set debugging mode
   *
   * @param debug true if debug output should be printed
   */
  public void setDebug(boolean debug) {

    m_Debug = debug;
  }

  /**
   * Get whether debugging is turned on
   *
   * @return true if debugging output is on
   */
  public boolean getDebug() {

    return m_Debug;
  }

  /**
   * Boosting method. Boosts any classifier that can handle weighted
   * instances.
   *
   * @param data the training data to be used for generating the
   * boosted classifier.
   * @exception Exception if the classifier could not be built successfully
   */
  public void buildClassifier(Instances data) throws Exception {

    Random randomInstance = new Random(m_Seed);
    Instances boostData, trainData;
    int classIndex = data.classIndex();

    if (data.classAttribute().isNumeric()) {
      throw new Exception("LogitBoost can't handle a numeric class!");
    }
    if (m_Classifier == null) {
      throw new Exception("A base classifier has not been specified!");
    }
    
    if (!(m_Classifier instanceof WeightedInstancesHandler) &&
	!m_UseResampling) {
      m_UseResampling = true;
    }
    if (data.checkForStringAttributes()) {
      throw new Exception("Can't handle string attributes!");
    }
    if (m_Debug) {
      System.err.println("Creating copy of the training data");
    }

    m_NumClasses = data.numClasses();
    m_ClassAttribute = data.classAttribute();

    // Create a copy of the data with the class transformed into numeric
    boostData = new Instances(data);
    boostData.deleteWithMissingClass();
    int numInstances = boostData.numInstances();

    // Temporarily unset the class index
    boostData.setClassIndex(-1);
    boostData.deleteAttributeAt(classIndex);
    boostData.insertAttributeAt(new Attribute("'pseudo class'"), classIndex);
    boostData.setClassIndex(classIndex);
    m_NumericClassData = new Instances(boostData, 0);
    double [][] trainFs = new double [numInstances][m_NumClasses];
    double [][] trainYs = new double [numInstances][m_NumClasses];
    for (int j = 0; j < m_NumClasses; j++) {
      for (int i = 0, k = 0; i < numInstances; i++, k++) {
	while (data.instance(k).classIsMissing()) k++;
	trainYs[i][j] = (data.instance(k).classValue() == j) ? 1 : 0;
      }
    }
    if (m_Debug) {
      System.err.println("Creating base classifiers");
    }

    // Create the base classifiers
    m_Classifiers = new Classifier [m_NumClasses][];
    for (int j = 0; j < m_NumClasses; j++) {
      m_Classifiers[j] = Classifier.makeCopies(m_Classifier,
					       getMaxIterations());
    }

    // Do boostrap iterations
    for (m_NumIterations = 0; m_NumIterations < getMaxIterations(); 
	 m_NumIterations++) {
      if (m_Debug) {
	System.err.println("Training classifier " + (m_NumIterations + 1));
      }
      
      for (int j = 0; j < m_NumClasses; j++) {
	if (m_Debug) {
	  System.err.println("\t...for class " + (j + 1)
			     + " (" + m_ClassAttribute.name() 
			     + "=" + m_ClassAttribute.value(j) + ")");
	}

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
	  double w = Math.max(p * (1 - p), VERY_SMALL);
	  current.setValue(classIndex, z);
	  current.setWeight(numInstances * w);
	}

	// Select instances to train the classifier on
	if (m_WeightThreshold < 100) {
	  trainData = selectWeightQuantile(boostData, 
					   (double)m_WeightThreshold/100);
	} else {
	  trainData = new Instances(boostData,0,numInstances);
	  if (m_UseResampling) {
	     double[] weights = new double[boostData.numInstances()];
	     for (int kk = 0; kk < weights.length; kk++) {
	       weights[kk] = boostData.instance(kk).weight();
	     }
	     trainData = boostData.resampleWithWeights(randomInstance, 
						       weights);
	  }
	}
      
	// Build the classifier
	m_Classifiers[j][m_NumIterations].buildClassifier(trainData);
      }      

      // Evaluate / increment trainFs from the classifier
      for (int i = 0; i < numInstances; i++) {
	double [] pred = new double [m_NumClasses];
	double predSum = 0;
	for (int j = 0; j < m_NumClasses; j++) {
	  pred[j] = m_Classifiers[j][m_NumIterations]
	    .classifyInstance(boostData.instance(i));
	  predSum += pred[j];
	}
	predSum /= m_NumClasses;
	for (int j = 0; j < m_NumClasses; j++) {
	  trainFs[i][j] += (pred[j] - predSum) * (m_NumClasses-1) 
	    / m_NumClasses;
	}
      }
    }
  }

  /**
   * Calculates the class membership probabilities for the given test instance.
   *
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @exception Exception if instance could not be classified
   * successfully
   */
  public double [] distributionForInstance(Instance instance) 
    throws Exception {

    instance = (Instance)instance.copy();
    instance.setDataset(m_NumericClassData);
    double [] Fs = new double [m_NumClasses]; 
    for (int i = 0; i < m_NumIterations; i++) {
      double [] Fi = new double [m_NumClasses];
      double Fsum = 0;
      for (int j = 0; j < m_NumClasses; j++) {
	Fi[j] = m_Classifiers[j][i].classifyInstance(instance);
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
    Utils.normalize(distribution);
    return distribution;
  }

  /**
   * Returns the boosted model as Java source code.
   *
   * @return the tree as Java source code
   * @exception Exception if something goes wrong
   */
  public String toSource(String className) throws Exception {

    if (m_NumIterations == 0) {
      throw new Exception("No model built yet");
    }
    if (!(m_Classifiers[0][0] instanceof Sourcable)) {
      throw new Exception("Base learner " + m_Classifier.getClass().getName()
			  + " is not Sourcable");
    }

    StringBuffer text = new StringBuffer("class ");
    text.append(className).append(" {\n\n");
    text.append("  private static double RtoP(double []R, int j) {\n"+
		"    double Rcenter = 0;\n"+
		"    for (int i = 0; i < R.length; i++) {\n"+
		"      Rcenter += R[i];\n"+
		"    }\n"+
		"    Rcenter /= R.length;\n"+
		"    double Rsum = 0;\n"+
		"    for (int i = 0; i < R.length; i++) {\n"+
		"      Rsum += Math.exp(R[i] - Rcenter);\n"+
		"    }\n"+
		"    return Math.exp(R[j]) / Rsum;\n"+
		"  }\n\n");

    text.append("  public static double classify(Object [] i) {\n" +
                "    double [] d = distribution(i);\n" +
                "    double maxV = d[0];\n" +
		"    int maxI = 0;\n"+
		"    for (int j = 1; j < " + m_NumClasses + "; j++) {\n"+
		"      if (d[j] > maxV) { maxV = d[j]; maxI = j; }\n"+
		"    }\n    return (double) maxI;\n  }\n\n");

    text.append("  public static double [] distribution(Object [] i) {\n");
    text.append("    double [] Fs = new double [" + m_NumClasses + "];\n");
    text.append("    double [] Fi = new double [" + m_NumClasses + "];\n");
    text.append("    double Fsum;\n");
    for (int i = 0; i < m_NumIterations; i++) {
      text.append("    Fsum = 0;\n");
      for (int j = 0; j < m_NumClasses; j++) {
	text.append("    Fi[" + j + "] = " + className + '_' +j + '_' + i 
		    + ".classify(i); Fsum += Fi[" + j + "];\n");
      }
      text.append("    Fsum /= " + m_NumClasses + ";\n");
      text.append("    for (int j = 0; j < " + m_NumClasses + "; j++) {");
      text.append(" Fs[j] += (Fi[j] - Fsum) * "
		  + (m_NumClasses - 1) + " / " + m_NumClasses + "; }\n");
    }
    
    text.append("    double [] dist = new double [" + m_NumClasses + "];\n" +
		"    for (int j = 0; j < " + m_NumClasses + "; j++) {\n"+
		"      dist[j] = RtoP(Fs, j);\n"+
		"    }\n    return dist;\n");
    text.append("  }\n}\n");

    for (int i = 0; i < m_Classifiers.length; i++) {
      for (int j = 0; j < m_Classifiers[i].length; j++) {
	text.append(((Sourcable)m_Classifiers[i][j])
		    .toSource(className + '_' + i + '_' + j));
      }
    }
    return text.toString();
  }

  /**
   * Returns description of the boosted classifier.
   *
   * @return description of the boosted classifier as a string
   */
  public String toString() {
    
    StringBuffer text = new StringBuffer();
    
    if (m_NumIterations == 0) {
      text.append("LogitBoost: No model built yet.");
      //      text.append(m_Classifiers[0].toString()+"\n");
    } else {
      text.append("LogitBoost: Base classifiers and their weights: \n");
      for (int i = 0; i < m_NumIterations; i++) {
	text.append("\nIteration "+(i+1));
	for (int j = 0; j < m_NumClasses; j++) {
	  text.append("\n\tClass " + (j + 1) 
		      + " (" + m_ClassAttribute.name() 
		      + "=" + m_ClassAttribute.value(j) + ")\n\n"
		      + m_Classifiers[j][i].toString() + "\n");
	}
      }
      text.append("Number of performed iterations: " +
		    m_NumIterations + "\n");
    }
    
    return text.toString();
  }

  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {

    try {
      System.out.println(Evaluation.evaluateModel(new LogitBoost(), argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}


  
