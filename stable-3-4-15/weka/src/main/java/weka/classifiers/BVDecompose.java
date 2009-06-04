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
 *    BVDecompose.java
 *    Copyright (C) 1999 Len Trigg
 *
 */

package weka.classifiers;

import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.rules.ZeroR;
import java.io.*;
import java.util.*;
import weka.core.*;

/**
 * Class for performing a Bias-Variance decomposition on any classifier 
 * using the method specified in:<p>
 * 
 * R. Kohavi & D. Wolpert (1996), <i>Bias plus variance decomposition for 
 * zero-one loss functions</i>, in Proc. of the Thirteenth International 
 * Machine Learning Conference (ICML96) 
 * <a href="http://robotics.stanford.edu/~ronnyk/biasVar.ps">
 * download postscript</a>.<p>
 *
 * Valid options are:<p>
 *
 * -D <br>
 * Turn on debugging output.<p>
 *
 * -W classname <br>
 * Specify the full class name of a learner to perform the 
 * decomposition on (required).<p>
 *
 * -t filename <br>
 * Set the arff file to use for the decomposition (required).<p>
 *
 * -T num <br>
 * Specify the number of instances in the training pool (default 100).<p>
 *
 * -c num <br>
 * Specify the index of the class attribute (default last).<p>
 *
 * -x num <br>
 * Set the number of train iterations (default 50). <p>
 *
 * -s num <br>
 * Set the seed for the dataset randomisation (default 1). <p>
 *
 * Options after -- are passed to the designated sub-learner. <p>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.9.2.1 $
 */
public class BVDecompose implements OptionHandler {

  /** Debugging mode, gives extra output if true */
  protected boolean m_Debug;

  /** An instantiated base classifier used for getting and testing options. */
  protected Classifier m_Classifier = new weka.classifiers.rules.ZeroR();

  /** The options to be passed to the base classifier. */
  protected String [] m_ClassifierOptions;

  /** The number of train iterations */
  protected int m_TrainIterations = 50;

  /** The name of the data file used for the decomposition */
  protected String m_DataFileName;

  /** The index of the class attribute */
  protected int m_ClassIndex = -1;

  /** The random number seed */
  protected int m_Seed = 1;

  /** The calculated bias (squared) */
  protected double m_Bias;

  /** The calculated variance */
  protected double m_Variance;

  /** The calculated sigma (squared) */
  protected double m_Sigma;

  /** The error rate */
  protected double m_Error;

  /** The number of instances used in the training pool */
  protected int m_TrainPoolSize = 100;

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(7);

    newVector.addElement(new Option(
	      "\tThe index of the class attribute.\n"+
	      "\t(default last)",
	      "c", 1, "-c <class index>"));
    newVector.addElement(new Option(
	      "\tThe name of the arff file used for the decomposition.",
	      "t", 1, "-t <name of arff file>"));
    newVector.addElement(new Option(
	      "\tThe number of instances placed in the training pool.\n"
	      + "\tThe remainder will be used for testing. (default 100)",
	      "T", 1, "-T <training pool size>"));
    newVector.addElement(new Option(
	      "\tThe random number seed used.",
	      "s", 1, "-s <seed>"));
    newVector.addElement(new Option(
	      "\tThe number of training repetitions used.\n"
	      +"\t(default 50)",
	      "x", 1, "-x <num>"));
    newVector.addElement(new Option(
	      "\tTurn on debugging output.",
	      "D", 0, "-D"));
    newVector.addElement(new Option(
	      "\tFull class name of the learner used in the decomposition.\n"
	      +"\teg: weka.classifiers.bayes.NaiveBayes",
	      "W", 1, "-W <classifier class name>"));

    if ((m_Classifier != null) &&
	(m_Classifier instanceof OptionHandler)) {
      newVector.addElement(new Option(
				      "",
				      "", 0, "\nOptions specific to learner "
				      + m_Classifier.getClass().getName()
				      + ":"));
      Enumeration enu = ((OptionHandler)m_Classifier).listOptions();
      while (enu.hasMoreElements()) {
	newVector.addElement(enu.nextElement());
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
   * Specify the full class name of a learner to perform the 
   * decomposition on (required).<p>
   *
   * -t filename <br>
   * Set the arff file to use for the decomposition (required).<p>
   *
   * -T num <br>
   * Specify the number of instances in the training pool (default 100).<p>
   *
   * -c num <br>
   * Specify the index of the class attribute (default last).<p>
   *
   * -x num <br>
   * Set the number of train iterations (default 50). <p>
   *
   * -s num <br>
   * Set the seed for the dataset randomisation (default 1). <p>
   *
   * Options after -- are passed to the designated sub-learner. <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    setDebug(Utils.getFlag('D', options));
        
    String classIndex = Utils.getOption('c', options);
    if (classIndex.length() != 0) {
      if (classIndex.toLowerCase().equals("last")) {
	setClassIndex(0);
      } else if (classIndex.toLowerCase().equals("first")) {
	setClassIndex(1);
      } else {
	setClassIndex(Integer.parseInt(classIndex));
      }
    } else {
      setClassIndex(0);
    }

    String trainIterations = Utils.getOption('x', options);
    if (trainIterations.length() != 0) {
      setTrainIterations(Integer.parseInt(trainIterations));
    } else {
      setTrainIterations(50);
    }

    String trainPoolSize = Utils.getOption('T', options);
    if (trainPoolSize.length() != 0) {
      setTrainPoolSize(Integer.parseInt(trainPoolSize));
    } else {
      setTrainPoolSize(100);
    }

    String seedString = Utils.getOption('s', options);
    if (seedString.length() != 0) {
      setSeed(Integer.parseInt(seedString));
    } else {
      setSeed(1);
    }

    String dataFile = Utils.getOption('t', options);
    if (dataFile.length() == 0) {
      throw new Exception("An arff file must be specified"
			  + " with the -t option.");
    }
    setDataFileName(dataFile);

    String classifierName = Utils.getOption('W', options);
    if (classifierName.length() == 0) {
      throw new Exception("A learner must be specified with the -W option.");
    }
    setClassifier(Classifier.forName(classifierName,
				     Utils.partitionOptions(options)));
  }

  /**
   * Gets the current settings of the CheckClassifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] classifierOptions = new String [0];
    if ((m_Classifier != null) && 
	(m_Classifier instanceof OptionHandler)) {
      classifierOptions = ((OptionHandler)m_Classifier).getOptions();
    }
    String [] options = new String [classifierOptions.length + 14];
    int current = 0;
    if (getDebug()) {
      options[current++] = "-D";
    }
    options[current++] = "-c"; options[current++] = "" + getClassIndex();
    options[current++] = "-x"; options[current++] = "" + getTrainIterations();
    options[current++] = "-T"; options[current++] = "" + getTrainPoolSize();
    options[current++] = "-s"; options[current++] = "" + getSeed();
    if (getDataFileName() != null) {
      options[current++] = "-t"; options[current++] = "" + getDataFileName();
    }
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
   * Get the number of instances in the training pool.
   *
   * @return number of instances in the training pool.
   */
  public int getTrainPoolSize() {
    
    return m_TrainPoolSize;
  }
  
  /**
   * Set the number of instances in the training pool.
   *
   * @param numTrain number of instances in the training pool.
   */
  public void setTrainPoolSize(int numTrain) {
    
    m_TrainPoolSize = numTrain;
  }
  
  /**
   * Set the classifiers being analysed
   *
   * @param newClassifier the Classifier to use.
   */
  public void setClassifier(Classifier newClassifier) {

    m_Classifier = newClassifier;
  }

  /**
   * Gets the name of the classifier being analysed
   *
   * @return the classifier being analysed.
   */
  public Classifier getClassifier() {

    return m_Classifier;
  }

  /**
   * Sets debugging mode
   *
   * @param debug true if debug output should be printed
   */
  public void setDebug(boolean debug) {

    m_Debug = debug;
  }

  /**
   * Gets whether debugging is turned on
   *
   * @return true if debugging output is on
   */
  public boolean getDebug() {

    return m_Debug;
  }

  /**
   * Sets the random number seed
   */
  public void setSeed(int seed) {

    m_Seed = seed;
  }

  /**
   * Gets the random number seed
   *
   * @return the random number seed
   */
  public int getSeed() {

    return m_Seed;
  }

  /**
   * Sets the maximum number of boost iterations
   */
  public void setTrainIterations(int trainIterations) {

    m_TrainIterations = trainIterations;
  }

  /**
   * Gets the maximum number of boost iterations
   *
   * @return the maximum number of boost iterations
   */
  public int getTrainIterations() {

    return m_TrainIterations;
  }

  /**
   * Sets the maximum number of boost iterations
   */
  public void setDataFileName(String dataFileName) {

    m_DataFileName = dataFileName;
  }

  /**
   * Get the name of the data file used for the decomposition
   *
   * @return the name of the data file
   */
  public String getDataFileName() {

    return m_DataFileName;
  }

  /**
   * Get the index (starting from 1) of the attribute used as the class.
   *
   * @return the index of the class attribute
   */
  public int getClassIndex() {

    return m_ClassIndex + 1;
  }

  /**
   * Sets index of attribute to discretize on
   *
   * @param index the index (starting from 1) of the class attribute
   */
  public void setClassIndex(int classIndex) {

    m_ClassIndex = classIndex - 1;
  }

  /**
   * Get the calculated bias squared
   *
   * @return the bias squared
   */
  public double getBias() {

    return m_Bias;
  } 

  /**
   * Get the calculated variance
   *
   * @return the variance
   */
  public double getVariance() {

    return m_Variance;
  }

  /**
   * Get the calculated sigma squared
   *
   * @return the sigma squared
   */
  public double getSigma() {

    return m_Sigma;
  }

  /**
   * Get the calculated error rate
   *
   * @return the error rate
   */
  public double getError() {

    return m_Error;
  }

  /**
   * Carry out the bias-variance decomposition
   *
   * @exception Exception if the decomposition couldn't be carried out
   */
  public void decompose() throws Exception {

    Reader dataReader = new BufferedReader(new FileReader(m_DataFileName));
    Instances data = new Instances(dataReader);

    if (m_ClassIndex < 0) {
      data.setClassIndex(data.numAttributes() - 1);
    } else {
      data.setClassIndex(m_ClassIndex);
    }
    if (data.classAttribute().type() != Attribute.NOMINAL) {
      throw new Exception("Class attribute must be nominal");
    }
    int numClasses = data.numClasses();

    data.deleteWithMissingClass();
    if (data.checkForStringAttributes()) {
      throw new Exception("Can't handle string attributes!");
    }

    if (data.numInstances() < 2 * m_TrainPoolSize) {
      throw new Exception("The dataset must contain at least "
			  + (2 * m_TrainPoolSize) + " instances");
    }
    Random random = new Random(m_Seed);
    data.randomize(random);
    Instances trainPool = new Instances(data, 0, m_TrainPoolSize);
    Instances test = new Instances(data, m_TrainPoolSize, 
				   data.numInstances() - m_TrainPoolSize);
    int numTest = test.numInstances();
    double [][] instanceProbs = new double [numTest][numClasses];

    m_Error = 0;
    for (int i = 0; i < m_TrainIterations; i++) {
      if (m_Debug) {
	System.err.println("Iteration " + (i + 1));
      }
      trainPool.randomize(random);
      Instances train = new Instances(trainPool, 0, m_TrainPoolSize / 2);

      Classifier current = Classifier.makeCopy(m_Classifier);
      current.buildClassifier(train);

      //// Evaluate the classifier on test, updating BVD stats
      for (int j = 0; j < numTest; j++) {
	int pred = (int)current.classifyInstance(test.instance(j));
	if (pred != test.instance(j).classValue()) {
	  m_Error++;
	}
	instanceProbs[j][pred]++;
      }
    }
    m_Error /= (m_TrainIterations * numTest);

    // Average the BV over each instance in test.
    m_Bias = 0;
    m_Variance = 0;
    m_Sigma = 0;
    for (int i = 0; i < numTest; i++) {
      Instance current = test.instance(i);
      double [] predProbs = instanceProbs[i];
      double pActual, pPred;
      double bsum = 0, vsum = 0, ssum = 0;
      for (int j = 0; j < numClasses; j++) {
	pActual = (current.classValue() == j) ? 1 : 0; // Or via 1NN from test data?
	pPred = predProbs[j] / m_TrainIterations;
	bsum += (pActual - pPred) * (pActual - pPred) 
	- pPred * (1 - pPred) / (m_TrainIterations - 1);
	vsum += pPred * pPred;
	ssum += pActual * pActual;
      }
      m_Bias += bsum;
      m_Variance += (1 - vsum);
      m_Sigma += (1 - ssum);
    }
    m_Bias /= (2 * numTest);
    m_Variance /= (2 * numTest);
    m_Sigma /= (2 * numTest);

    if (m_Debug) {
      System.err.println("Decomposition finished");
    }
  }


  /**
   * Returns description of the bias-variance decomposition results.
   *
   * @return the bias-variance decomposition results as a string
   */
  public String toString() {

    String result = "\nBias-Variance Decomposition\n";

    if (getClassifier() == null) {
      return "Invalid setup";
    }

    result += "\nClassifier   : " + getClassifier().getClass().getName();
    if (getClassifier() instanceof OptionHandler) {
      result += Utils.joinOptions(((OptionHandler)m_Classifier).getOptions());
    }
    result += "\nData File    : " + getDataFileName();
    result += "\nClass Index  : ";
    if (getClassIndex() == 0) {
      result += "last";
    } else {
      result += getClassIndex();
    }
    result += "\nTraining Pool: " + getTrainPoolSize();
    result += "\nIterations   : " + getTrainIterations();
    result += "\nSeed         : " + getSeed();
    result += "\nError        : " + Utils.doubleToString(getError(), 6, 4);
    result += "\nSigma^2      : " + Utils.doubleToString(getSigma(), 6, 4);
    result += "\nBias^2       : " + Utils.doubleToString(getBias(), 6, 4);
    result += "\nVariance     : " + Utils.doubleToString(getVariance(), 6, 4);

    return result + "\n";
  }
  

  /**
   * Test method for this class
   *
   * @param args the command line arguments
   */
  public static void main(String [] args) {

    try {
      BVDecompose bvd = new BVDecompose();

      try {
	bvd.setOptions(args);
	Utils.checkForRemainingOptions(args);
      } catch (Exception ex) {
	String result = ex.getMessage() + "\nBVDecompose Options:\n\n";
	Enumeration enu = bvd.listOptions();
	while (enu.hasMoreElements()) {
	  Option option = (Option) enu.nextElement();
	  result += option.synopsis() + "\n" + option.description() + "\n";
	}
	throw new Exception(result);
      }

      bvd.decompose();
      System.out.println(bvd.toString());
    } catch (Exception ex) {
      System.err.println(ex.getMessage());
    }
  }
}


