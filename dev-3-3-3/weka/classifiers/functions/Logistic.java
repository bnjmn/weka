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
 *    Logistic.java
 *    Copyright (C) 2002 Eibe Frank
 *
 */

package weka.classifiers.functions;

import weka.classifiers.meta.LogitBoost;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.Evaluation;
import weka.classifiers.DistributionClassifier;
import weka.classifiers.Classifier;
import weka.core.UnsupportedClassTypeException;
import weka.core.Instances;
import weka.core.Instance;
import weka.core.OptionHandler;
import weka.core.SelectedTag;
import weka.core.Utils;
import weka.core.Attribute;
import weka.core.Option;
import weka.core.UnsupportedAttributeTypeException;
import weka.filters.unsupervised.attribute.NominalToBinary;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.Filter;

import java.util.Enumeration;
import java.util.Vector;

/**
 * Implements linear logistic regression using LogitBoost and
 * LinearRegression.<p>
 *
 * Missing values are replaced using ReplaceMissingValues, and
 * nominal attributes are transformed into numeric attributes using
 * NominalToBinary.<p>
 *
 * -P precision <br>
 * Set the precision of stopping criterion based on average loglikelihood.
 * (default 1.0e-13) <p>
 *
 * -R ridge <br>
 * Set the ridge parameter for the linear regression models.
 * (default 1.0e-8)<p>
 *
 * -M num <br>
 * Set the maximum number of iterations.
 * (default 200)<p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.22 $ 
 */
public class Logistic extends DistributionClassifier 
  implements OptionHandler {

  /* The coefficients */
  private double[][] m_Coefficients = null;

  /* The index of the class */
  private int m_ClassIndex = -1;

  /* An attribute filter */
  private Remove m_AttFilter = null;

  /* The header info */
  private Instances m_Header = null;
    
  /** The filter used to make attributes numeric. */
  private NominalToBinary m_NominalToBinary = null;
  
  /** The filter used to get rid of missing values. */
  private ReplaceMissingValues m_ReplaceMissingValues = null;
    
  /** The ridge parameter. */
  private double m_Ridge = 1e-8;

  /** The precision parameter */   
  private double m_Precision = 1.0e-13;
  
  /** The maximum number of iterations. */
  private int m_MaxIts = 200;
    
  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    
    Vector newVector = new Vector(3);
    newVector.addElement(new Option("\tSet the precision of stopping criterion based on\n" + 
				    "\tchange in average loglikelihood (default 1.0e-13).",
				    "P", 1, "-P <precision>"));
    newVector.addElement(new Option("\tSet the ridge for the linear regression models (default 1.0e-8).",
				    "R", 1, "-R <ridge>"));
    newVector.addElement(new Option("\tSet the maximum number of iterations (default 200).",
				    "M", 1, "-M <number>"));
    return newVector.elements();
  }
  
  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -P precision <br>
   * Set the precision of stopping criterion based on average loglikelihood.
   * (default 1.0e-13) <p>
   *
   * -R ridge <br>
   * Set the ridge parameter for the linear regression models.
   * (default 1.0e-8)<p>
   *
   * -M num <br>
   * Set the maximum number of iterations.
   * (default 200)<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    String precisionString = Utils.getOption('P', options);
    if (precisionString.length() != 0) 
      m_Precision = Double.parseDouble(precisionString);
    else 
      m_Precision = 1.0e-13;
      
    String ridgeString = Utils.getOption('R', options);
    if (ridgeString.length() != 0) 
      m_Ridge = Double.parseDouble(ridgeString);
    else 
      m_Ridge = 1.0e-8;
      
    String maxItsString = Utils.getOption('M', options);
    if (maxItsString.length() != 0) 
      m_MaxIts = Integer.parseInt(maxItsString);
    else 
      m_MaxIts = 200;
  }
  
  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {
    
    String [] options = new String [6];
    int current = 0;
    
    options[current++] = "-P";
    options[current++] = ""+m_Precision;
    options[current++] = "-R";
    options[current++] = ""+m_Ridge;
    options[current++] = "-M";
    options[current++] = ""+m_MaxIts;
    
    while (current < options.length) 
      options[current++] = "";
    return options;
  }
  
  /**
   * Builds the model.
   */
  public void buildClassifier(Instances data) throws Exception {

    if (data.classAttribute().type() != Attribute.NOMINAL) {
      throw new UnsupportedClassTypeException("Class attribute must be nominal.");
    }
    if (data.checkForStringAttributes()) {
      throw new UnsupportedAttributeTypeException("Cannot handle string attributes!");
    }
    data = new Instances(data);
    data.deleteWithMissingClass();
    if (data.numInstances() == 0) {
      throw new Exception("No train instances without missing class value!");
    }
    m_ReplaceMissingValues = new ReplaceMissingValues();
    m_ReplaceMissingValues.setInputFormat(data);
    data = Filter.useFilter(data, m_ReplaceMissingValues);
    m_NominalToBinary = new NominalToBinary();
    m_NominalToBinary.setInputFormat(data);
    data = Filter.useFilter(data, m_NominalToBinary);

    // Find attributes that should be deleted because of
    // zero variance
    int[] indices = new int[data.numAttributes() - 1];
    int numDeleted = 0;
    for (int j = 0; j < data.numAttributes(); j++) {
      if (j != data.classIndex()) {
        double var = data.variance(j);
	if (var == 0) {
	  indices[numDeleted++] = j;
	}
      }
    }
    int[] temp = new int[numDeleted];
    System.arraycopy(indices, 0, temp, 0, numDeleted);
    indices = temp;

    // Remove useless attributes
    m_AttFilter = new Remove();
    m_AttFilter.setAttributeIndicesArray(indices);
    m_AttFilter.setInvertSelection(false);
    m_AttFilter.setInputFormat(data);
    data = Filter.useFilter(data, m_AttFilter);

    // Set class index
    m_ClassIndex = data.classIndex();

    // Standardize data
    double[][] values = 
      new double[data.numInstances()][data.numAttributes()];
    double[] means = new double[data.numAttributes()];
    double[] stdDevs = new double[data.numAttributes()];
    for (int j = 0; j < data.numAttributes(); j++) {
      if (j != data.classIndex()) {
	means[j] = data.meanOrMode(j);
	stdDevs[j] = Math.sqrt(data.variance(j));
	for (int i = 0; i < data.numInstances(); i++) {
	  values[i][j] = (data.instance(i).value(j) - means[j]) / 
	    stdDevs[j];
	}
      } else {
	for (int i = 0; i < data.numInstances(); i++) {
	  values[i][j] = data.instance(i).value(j);
	}
      }
    }
    Instances newData = new Instances(data, data.numInstances());
    for (int i = 0; i < data.numInstances(); i++) {
      newData.add(new Instance(data.instance(i).weight(), values[i]));
    }

    // Use LogitBoost to build model
    LogitBoost boostedModel = new LogitBoost();
    boostedModel.setLikelihoodThreshold(m_Precision);
    boostedModel.setMaxIterations(m_MaxIts);
    LinearRegression lr = new LinearRegression();
    lr.setEliminateColinearAttributes(false);
    lr.setAttributeSelectionMethod(new SelectedTag(LinearRegression.
						   SELECTION_NONE,
						   LinearRegression.
						   TAGS_SELECTION));
    lr.turnChecksOff();
    lr.setRidge(m_Ridge);
    boostedModel.setClassifier(lr);
    boostedModel.buildClassifier(newData);

    // Extract coefficients
    Classifier[][] models = boostedModel.classifiers();
    m_Coefficients = new double[newData.numClasses()]
      [newData.numAttributes() + 1];
    for (int j = 0; j < newData.numClasses(); j++) {
      for (int i = 0; i < models[j].length; i++) {
	double[] locCoefficients = 
	  ((LinearRegression)models[j][i]).coefficients();
	for (int k = 0; k <= newData.numAttributes(); k++) {
	  if (k != newData.classIndex()) {
	    m_Coefficients[j][k] += locCoefficients[k];
	  }
	}
      }
    }
	   
    // Convert coefficients into original scale
    for(int j = 0; j < data.numClasses(); j++){
      for(int i = 0; i < data.numAttributes(); i++) {
	if ((i != newData.classIndex()) &&
	    (stdDevs[i] > 0)) {
	  m_Coefficients[j][i] /= stdDevs[i];
	  m_Coefficients[j][data.numAttributes()] -= 
	    m_Coefficients[j][i] * means[i];
	}
      }
    }
    m_Header = new Instances(data, 0);
  }

  /**
   * Classifies an instance.
   */
  public double[] distributionForInstance(Instance inst) 
    throws Exception {

    // Filter instance
    m_ReplaceMissingValues.input(inst);
    inst = m_ReplaceMissingValues.output();
    m_NominalToBinary.input(inst);
    inst = m_NominalToBinary.output();
    m_AttFilter.input(inst);
    m_AttFilter.batchFinished();
    inst = m_AttFilter.output();

    // Compute prediction
    double[] preds = new double[m_Coefficients.length];
    for (int j = 0; j < inst.numClasses(); j++) {
      for (int i = 0; i < inst.numAttributes(); i++) {
	if (i != inst.classIndex()) {
	  preds[j] += inst.value(i) * m_Coefficients[j][i];
	}
      }
      preds[j] += m_Coefficients[j][inst.numAttributes()];
    }
    return probs(preds);
  }

  /**
   * Computes probabilities from F scores
   */
  private double[] probs(double[] Fs) {

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
    Utils.normalize(probs, sum);
    return probs;
  }

  /**
   * Prints the model.
   */
  public String toString() {

    if (m_Coefficients == null) {
      return "No model has been built yet!";
    } 
    StringBuffer text = new StringBuffer();
    for (int j = 0; j < m_Coefficients.length; j++) {
      text.append("\nModel for class: " + 
		  m_Header.classAttribute().value(j) + "\n\n");
      for (int i = 0; i < m_Coefficients[j].length; i++) {
	if (i != m_ClassIndex) {
	  if (i > 0) {
	    text.append(" + ");
	  } else {
	    text.append("   ");
	  }
	  text.append(Utils.doubleToString(m_Coefficients[j][i], 12, 4));
	  if (i < m_Coefficients[j].length - 1) {
	    text.append(" * " 
			+ m_Header.attribute(i).name() + "\n");
	  }
	}
      }
      text.append("\n");
    }
    return text.toString();
  }
  
  /**
   * Get the value of MaxIts.
   *
   * @return Value of MaxIts.
   */
  public int getMaxIts() {
    
    return m_MaxIts;
  }
  
  /**
   * Set the value of MaxIts.
   *
   * @param newMaxIts Value to assign to MaxIts.
   */
  public void setMaxIts(int newMaxIts) {
    
    m_MaxIts = newMaxIts;
  }
  
  /**
   * Sets the precision of stopping criterion in Newton method.
   *
   * @param precision the precision
   */
  public void setPrecision(double precision) {
    m_Precision = precision;
  }
    
  /**
   * Gets the precision of stopping criterion in Newton method.
   *
   * @return the precision
   */
  public double getPrecision() {
    return m_Precision;
  }

  /**
   * Sets the ridge parameter.
   *
   * @param ridge the ridge
   */
  public void setRidge(double ridge) {
    m_Ridge = ridge;
  }
    
  /**
   * Gets the ridge parameter.
   *
   * @return the ridge
   */
  public double getRidge() {
    return m_Ridge;
  }

  /**
   * Main method for testing this class.
   */
  public static void main(String[] argv) {

    try {
      System.out.println(Evaluation.evaluateModel(new Logistic(), argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
}
