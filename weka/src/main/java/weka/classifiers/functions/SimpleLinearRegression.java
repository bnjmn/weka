/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    SimpleLinearRegression.java
 *    Copyright (C) 2002-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.functions;

import weka.classifiers.AbstractClassifier;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;

/**
 <!-- globalinfo-start -->
 * Learns a simple linear regression model. Picks the attribute that results in the lowest squared error. Missing values are not allowed. Can only deal with numeric attributes.
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
 * @version $Revision$
 */
public class SimpleLinearRegression extends AbstractClassifier 
  implements WeightedInstancesHandler {

  /** for serialization */
  static final long serialVersionUID = 1679336022895414137L;
  
  /** The chosen attribute */
  private Attribute m_attribute;

  /** The index of the chosen attribute */
  private int m_attributeIndex;

  /** The slope */
  private double m_slope;
  
  /** The intercept */
  private double m_intercept;

  /** If true, suppress error message if no useful attribute was found*/   
  private boolean m_suppressErrorMessage = false;  

  /**
   * Returns a string describing this classifier
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Learns a simple linear regression model. "
      +"Picks the attribute that results in the lowest squared error. "
      +"Missing values are not allowed. Can only deal with numeric attributes.";
  }

    /** 
     * Default constructor.
     */
    public SimpleLinearRegression() {

    }

    /**
     * Construct a simple linear regression model based on the given info.
     */
    public SimpleLinearRegression(Instances data, int attIndex, double slope, double intercept) {

        m_attributeIndex = attIndex;
        m_slope = slope;
        m_intercept = intercept;
        m_attribute = data.attribute(attIndex);
    }


  /**
   * Takes the given simple linear regression model and adds it to this one.
   * Does nothing if the given model is based on a different attribute.
   * Assumes the given model has been initialized.
   */
  public void addModel(SimpleLinearRegression slr) throws Exception {
    
      if (m_attribute == null || slr.m_attributeIndex == m_attributeIndex) {
          m_attributeIndex = slr.m_attributeIndex;
          m_attribute = slr.m_attribute;
          m_slope += slr.m_slope;
          m_intercept += slr.m_intercept;
      } else {
          throw new Exception("Could not add models. " +
                              m_attributeIndex + " " + slr.m_attributeIndex + " " +
                              m_attribute + " " + slr.m_attribute + " " +
                              m_slope + " " + slr.m_slope + " " +
                              m_intercept + " " + slr.m_intercept);
      }
  }


  /**
   * Generate a prediction for the supplied instance.
   *
   * @param inst the instance to predict.
   * @return the prediction
   * @throws Exception if an error occurs
   */
  public double classifyInstance(Instance inst) throws Exception {
    
    if (m_attribute == null) {
      return m_intercept;
    } else {
      if (inst.isMissing(m_attribute.index())) {
	throw new Exception("SimpleLinearRegression: No missing values!");
      }
      return m_intercept + m_slope * inst.value(m_attribute.index());
    }
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

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
   * Computes the attribute means.
   */
  protected double[] computeMeans(Instances insts) {

    // We can assume that all the attributes are numeric and that
    // we don't have any missing attribute values (excluding the class)
    double[] means = new double[insts.numAttributes()];
    double[] counts = new double[insts.numAttributes()];
    for (int j = 0; j < insts.numInstances(); j++) {
      Instance inst = insts.instance(j);
      if (!inst.classIsMissing()) {
        for (int i = 0; i < insts.numAttributes(); i++) {
          means[i] += inst.weight() * inst.value(i);
          counts[i] += inst.weight();
        }
      }
    }
    for (int i = 0; i < insts.numAttributes(); i++) {
      if (counts[i] > 0) {
        means[i] /= counts[i];
      } else {
        means[i] = 0.0;
      }
    }    
    return means;
  }

  /**
   * Builds a simple linear regression model given the supplied training data.
   *
   * @param insts the training data.
   * @throws Exception if an error occurs
   */
  public void buildClassifier(Instances insts) throws Exception {

    // can classifier handle the data?
    getCapabilities().testWithFail(insts);

    // Compute relevant statistics
    double[] means = computeMeans(insts);
    double[] slopes = new double[insts.numAttributes()];
    double[] sumWeightedDiffsSquared = new double[insts.numAttributes()];
    int classIndex = insts.classIndex();

    // For all instances
    for (int j = 0; j < insts.numInstances(); j++) {
      Instance inst = insts.instance(j);

      // Only need to do something if the class isn't missing
      if (!inst.classIsMissing()) {
        double yDiff = inst.value(classIndex) - means[classIndex];
        double weightedYDiff = inst.weight() * yDiff;

        // For all attributes
        for (int i = 0; i < insts.numAttributes(); i++) {
          double diff = inst.value(i) - means[i];
          double weightedDiff = inst.weight() * diff;

          // Doesn't mater if we compute this for the class
          slopes[i] += weightedYDiff * diff;

          // We need this for the class as well
          sumWeightedDiffsSquared[i] += weightedDiff * diff;
        }
      }
    }

    // Pick the best attribute
    double minSSE = Double.MAX_VALUE;
    m_attribute = null;
    int chosen = -1;
    double chosenSlope = Double.NaN;
    double chosenIntercept = Double.NaN;
    for (int i = 0; i < insts.numAttributes(); i++) {
      
      // Should we skip this attribute?
      if ((i == classIndex) || (sumWeightedDiffsSquared[i] == 0)) {
        continue;
      }

      // Compute final slope and intercept
      double numerator = slopes[i];
      slopes[i] /= sumWeightedDiffsSquared[i];
      double intercept = means[classIndex] - slopes[i] * means[i];
      
      // Compute sum of squared errors
      double sse = sumWeightedDiffsSquared[classIndex] - slopes[i] * numerator;
      
      // Check whether this is the best attribute
      if (sse < minSSE) {
        minSSE = sse;
        chosen = i;
        chosenSlope = slopes[i];
        chosenIntercept = intercept;
      }
    }

    // Set parameters
    if (chosen == -1) {
      if (!m_suppressErrorMessage) System.err.println("----- no useful attribute found");
      m_attribute = null;
      m_attributeIndex = 0;
      m_slope = 0;
      m_intercept = means[classIndex];
    } else {
      m_attribute = insts.attribute(chosen);
      m_attributeIndex = chosen;
      m_slope = chosenSlope;
      m_intercept = chosenIntercept;
    }
  }

  /**
   * Returns true if a usable attribute was found.
   *
   * @return true if a usable attribute was found.
   */
  public boolean foundUsefulAttribute(){
      return (m_attribute != null); 
  } 

  /**
   * Returns the index of the attribute used in the regression.
   *
   * @return the index of the attribute.
   */
  public int getAttributeIndex(){
      return m_attributeIndex;
  }

  /**
   * Returns the slope of the function.
   *
   * @return the slope.
   */
  public double getSlope(){
      return m_slope;
  }
    
  /**
   * Returns the intercept of the function.
   *
   * @return the intercept.
   */
  public double getIntercept(){
      return m_intercept;
  }  

  /**
   * Turn off the error message that is reported when no useful attribute is found.
   *
   * @param s if set to true turns off the error message
   */
  public void setSuppressErrorMessage(boolean s){
      m_suppressErrorMessage = s;
  }   

  /**
   * Returns a description of this classifier as a string
   *
   * @return a description of the classifier.
   */
  public String toString() {

    StringBuffer text = new StringBuffer();
    if (m_attribute == null) {
      text.append("Predicting constant " + m_intercept);
    } else {
      text.append("Linear regression on " + m_attribute.name() + "\n\n");
      text.append(Utils.doubleToString(m_slope,2) + " * " + 
		m_attribute.name());
      if (m_intercept > 0) {
	text.append(" + " + Utils.doubleToString(m_intercept, 2));
      } else {
      text.append(" - " + Utils.doubleToString((-m_intercept), 2)); 
      }
    }
    text.append("\n");
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

  /**
   * Main method for testing this class
   *
   * @param argv options
   */
  public static void main(String [] argv){
    runClassifier(new SimpleLinearRegression(), argv);
  } 
}
