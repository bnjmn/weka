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

package weka.classifiers.trees.lmt;

import java.io.Serializable;

import weka.core.Instance;
import weka.core.Instances;

/**
 * Stripped down version of SimpleLinearRegression. Assumes that there are no
 * missing class values.
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 10169 $
 */
public class SimpleLinearRegression implements Serializable {

  /** for serialization */
  static final long serialVersionUID = 1779336022895414137L;

  /** The index of the chosen attribute */
  private int m_attributeIndex = -1;

  /** The slope */
  private double m_slope = Double.NaN;

  /** The intercept */
  private double m_intercept = Double.NaN;

  /**
   * Default constructor.
   */
  public SimpleLinearRegression() {

  }

  /**
   * Construct a simple linear regression model based on the given info.
   */
  public SimpleLinearRegression(int attIndex, double slope, double intercept) {

    m_attributeIndex = attIndex;
    m_slope = slope;
    m_intercept = intercept;
  }

  /**
   * Takes the given simple linear regression model and adds it to this one.
   * Does nothing if the given model is based on a different attribute. Assumes
   * the given model has been initialized.
   */
  public void addModel(SimpleLinearRegression slr) throws Exception {

    m_attributeIndex = slr.m_attributeIndex;
    if (m_attributeIndex != -1) {
      m_slope += slr.m_slope;
      m_intercept += slr.m_intercept;
    } else {
      m_slope = slr.m_slope;
      m_intercept = slr.m_intercept;
    }
  }

  /**
   * Generate a prediction for the supplied instance.
   * 
   * @param inst the instance to predict.
   * @return the prediction
   */
  public double classifyInstance(Instance inst) {

    return m_intercept + m_slope * inst.value(m_attributeIndex);
  }

  /**
   * Computes the attribute means.
   */
  protected double[] computeMeans(Instances insts) {

    // We can assume that all the attributes are numeric and that
    // we don't have any missing attribute values (including the class)
    double[] means = new double[insts.numAttributes()];
    double[] counts = new double[insts.numAttributes()];
    for (int j = 0; j < insts.numInstances(); j++) {
      Instance inst = insts.instance(j);
      for (int i = 0; i < insts.numAttributes(); i++) {
        means[i] += inst.weight() * inst.value(i);
        counts[i] += inst.weight();
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
   */
  public void buildClassifier(Instances insts) {

    // Compute relevant statistics
    double[] means = computeMeans(insts);
    double[] slopes = new double[insts.numAttributes()];
    double[] sumWeightedDiffsSquared = new double[insts.numAttributes()];
    int classIndex = insts.classIndex();

    // For all instances
    for (int j = 0; j < insts.numInstances(); j++) {
      Instance inst = insts.instance(j);

      double yDiff = inst.value(classIndex) - means[classIndex];
      double weightedYDiff = inst.weight() * yDiff;

      // For all attributes
      for (int i = 0; i < insts.numAttributes(); i++) {
        double diff = inst.value(i) - means[i];
        double weightedDiff = inst.weight() * diff;

        // Doesn't matter if we compute this for the class
        slopes[i] += weightedYDiff * diff;

        // We need this for the class as well
        sumWeightedDiffsSquared[i] += weightedDiff * diff;
      }
    }

    // Pick the best attribute
    double minSSE = Double.MAX_VALUE;
    m_attributeIndex = -1;
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
        m_attributeIndex = i;
        m_slope = slopes[i];
        m_intercept = intercept;
      }
    }
  }

  /**
   * Returns true if a usable attribute was found.
   * 
   * @return true if a usable attribute was found.
   */
  public boolean foundUsefulAttribute() {
    return (m_attributeIndex != -1);
  }

  /**
   * Returns the index of the attribute used in the regression.
   * 
   * @return the index of the attribute.
   */
  public int getAttributeIndex() {
    return m_attributeIndex;
  }

  /**
   * Returns the slope of the function.
   * 
   * @return the slope.
   */
  public double getSlope() {
    return m_slope;
  }

  /**
   * Returns the intercept of the function.
   * 
   * @return the intercept.
   */
  public double getIntercept() {
    return m_intercept;
  }
}
