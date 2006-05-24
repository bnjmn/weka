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
 *    IsotonicRegression.java
 *    Copyright (C) 2006 University of Waikato
 *
 */

package weka.classifiers.functions;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.core.Capabilities.Capability;

import java.util.Arrays;

/**
 <!-- globalinfo-start -->
 * Learns an isotonic regression model. Picks the attribute that results in the lowest squared error. Missing values are not allowed. Can only deal with numeric attributes.Considers the monotonically increasing case as well as the monotonicallydecreasing case
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
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public class IsotonicRegression extends Classifier implements WeightedInstancesHandler {

  /** for serialization */
  static final long serialVersionUID = 1679336022835454137L;
  
  /** The chosen attribute */
  private Attribute m_attribute;

  /** The array of cut points */
  private double[] m_cuts;
  
  /** The predicted value in each interval. */
  private double[] m_values;

  /** The minimum mean squared error that has been achieved. */
  private double m_minMsq;

  /**
   * Returns a string describing this classifier
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Learns an isotonic regression model. "
      +"Picks the attribute that results in the lowest squared error. "
      +"Missing values are not allowed. Can only deal with numeric attributes."
      +"Considers the monotonically increasing case as well as the monotonically"
      +"decreasing case";
  }

  /**
   * Generate a prediction for the supplied instance.
   *
   * @param inst the instance to predict.
   * @return the prediction
   * @throws Exception if an error occurs
   */
  public double classifyInstance(Instance inst) throws Exception {
    
    if (inst.isMissing(m_attribute.index())) {
      throw new Exception("IsotonicRegression: No missing values!");
    }
    int index = Arrays.binarySearch(m_cuts, inst.value(m_attribute));
    if (index < 0) {
      return m_values[-index - 1];
    } else { 
      return m_values[index + 1];
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
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);

    // class
    result.enable(Capability.NUMERIC_CLASS);
    result.enable(Capability.DATE_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);
    
    return result;
  }

  /**
   * Does the actual regression.
   */
  protected void regress(Attribute attribute, Instances insts, boolean ascending) 
    throws Exception {

    // Sort values according to current attribute
    insts.sort(attribute);
    
    // Initialize arrays
    double[] values = new double[insts.numInstances()];
    double[] weights = new double[insts.numInstances()];
    double[] cuts = new double[insts.numInstances() - 1];
    int size = 0;
    values[0] = insts.instance(0).classValue();
    weights[0] = insts.instance(0).weight();
    for (int i = 1; i < insts.numInstances(); i++) {
      if (insts.instance(i).value(attribute) >
          insts.instance(i - 1).value(attribute)) {
        cuts[size] = (insts.instance(i).value(attribute) +
                      insts.instance(i - 1).value(attribute)) / 2;
        size++;
      }
      values[size] += insts.instance(i).classValue();
      weights[size] += insts.instance(i).weight();
    }
    size++;
    
    // While there is a pair of adjacent violators
    boolean violators;
    do {
      violators = false;
      
      // Initialize arrays
      double[] tempValues = new double[size];
      double[] tempWeights = new double[size];
      double[] tempCuts = new double[size - 1];
      
      // Merge adjacent violators
      int newSize = 0;
      tempValues[0] = values[0];
      tempWeights[0] = weights[0];
      for (int j = 1; j < size; j++) {
        if ((ascending && (values[j] / weights[j] > 
                           tempValues[newSize] / tempWeights[newSize])) ||
            (!ascending && (values[j] / weights[j] < 
                            tempValues[newSize] / tempWeights[newSize]))) {
          tempCuts[newSize] = cuts[j - 1];
          newSize++;
          tempValues[newSize] = values[j];
          tempWeights[newSize] = weights[j];
        } else {
          tempWeights[newSize] += weights[j];
          tempValues[newSize] += values[j];
          violators = true;
        }
      }
      newSize++;
      
      // Copy references
      values = tempValues;
      weights = tempWeights;
      cuts = tempCuts;
      size = newSize;
    } while (violators);
    
    // Compute actual predictions
    for (int i = 0; i < size; i++) {
      values[i] /= weights[i];
    }
    
    // Backup best instance variables
    Attribute attributeBackedup = m_attribute;
    double[] cutsBackedup = m_cuts;
    double[] valuesBackedup = m_values;
    
    // Set instance variables to values computed for this attribute
    m_attribute = attribute;
    m_cuts = cuts;
    m_values = values;
    
    // Compute sum of squared errors
    Evaluation eval = new Evaluation(insts);
    eval.evaluateModel(this, insts);
    double msq = eval.rootMeanSquaredError();
    
    // Check whether this is the best attribute
    if (msq < m_minMsq) {
      m_minMsq = msq;
    } else {
      m_attribute = attributeBackedup;
      m_cuts = cutsBackedup;
      m_values = valuesBackedup;
    }
  }
  
  /**
   * Builds an isotonic regression model given the supplied training data.
   *
   * @param insts the training data.
   * @throws Exception if an error occurs
   */
  public void buildClassifier(Instances insts) throws Exception {

    // can classifier handle the data?
    getCapabilities().testWithFail(insts);

    // remove instances with missing class
    insts = new Instances(insts);
    insts.deleteWithMissingClass();

    // Choose best attribute and mode
    m_minMsq = Double.MAX_VALUE;
    m_attribute = null;
    for (int a = 0; a < insts.numAttributes(); a++) {
      if (a != insts.classIndex()) {
        regress(insts.attribute(a), insts, true);
        regress(insts.attribute(a), insts, false);
      }
    }
  }

  /**
   * Returns a description of this classifier as a string
   *
   * @return a description of the classifier.
   */
  public String toString() {

    StringBuffer text = new StringBuffer();
    text.append("Isotonic regression\n\n");
    text.append("Based on attribute: " + m_attribute.name() + "\n\n");
    for (int i = 0; i < m_values.length; i++) {
      text.append("prediction: " + Utils.doubleToString(m_values[i], 10, 2));
      if (i < m_cuts.length) {
        text.append("\t\tcut point: " + Utils.doubleToString(m_cuts[i], 10, 2) + "\n");
      }
    }
    return text.toString();
  }

  /**
   * Main method for testing this class
   *
   * @param argv options
   */
  public static void main(String [] argv){

    try{
      System.out.println(Evaluation.evaluateModel(new IsotonicRegression(), argv));
    } catch (Exception e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  } 
}

