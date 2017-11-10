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
 * ClassConditionalProbabilities
 * Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.supervised.attribute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import weka.classifiers.bayes.NaiveBayes;
import weka.core.*;
import weka.estimators.Estimator;
import weka.filters.Filter;
import weka.filters.SimpleBatchFilter;
import weka.filters.unsupervised.attribute.Remove;
import weka.gui.ProgrammaticProperty;

/**
 <!-- globalinfo-start -->
 * Converts the values of nominal and/or numeric attributes into class conditional probabilities. If there are k classes, then k new attributes are created for each of the original ones, giving pr(att val | class k).<br/>
 * <br/>
 * Can be useful for converting nominal attributes with a lot of distinct values into something more manageable for learning schemes that can't handle nominal attributes (as opposed to creating binary indicator attributes). For nominal attributes, the user can specify the number values above which an attribute will be converted by this method. Normal distributions are assumed for numeric attributes.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -N
 *  Don't apply this transformation to numeric attributes</pre>
 * 
 * <pre> -C
 *  Don't apply this transformation to nominal attributes</pre>
 * 
 * <pre> -min-values &lt;integer&gt;
 *  Transform nominal attributes with at least this many values.
 *  -1 means always transform.</pre>
 * 
 * <pre> -output-debug-info
 *  If set, filter is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -do-not-check-capabilities
 *  If set, filter capabilities are not checked before filter is built
 *  (use with caution).</pre>
 *
 * <pre>-spread-attribute-weight
 *  When generating binary attributes, spread weight of old
 *  attribute across new attributes. Do not give each new attribute the old weight.</pre>
 *
 <!-- options-end -->
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class ClassConditionalProbabilities extends SimpleBatchFilter
  implements WeightedAttributesHandler, WeightedInstancesHandler{

  /** For serialization */
  private static final long serialVersionUID = 1684310720200284263L;

  /** True if numeric attributes are to be excluded from the transformation */
  protected boolean m_excludeNumericAttributes;

  /** True if nominal attributes are to be excluded from the transformation */
  protected boolean m_excludeNominalAttributes;

  /**
   * Don't convert nominal attributes with fewer than this number of values. -1
   * means always convert
   */
  protected int m_nominalConversionThreshold = -1;

  /** The Naive Bayes classifier to use for class conditional estimation */
  protected NaiveBayes m_estimator;

  /** Remove filter to use for creating a set of untouched attributes */
  protected Remove m_remove;

  /**
   * The attributes from the original data that are untouched by this
   * transformation
   */
  protected Instances m_unchanged;

  /** A lookup of estimators from Naive Bayes */
  protected Map<String, Estimator[]> m_estimatorLookup;

  /** Whether to spread attribute weight when creating binary attributes */
  protected boolean m_SpreadAttributeWeight = false;

  /**
   * Main method for testing this class
   *
   * @param args args
   */
  public static void main(String[] args) {
    runFilter(new ClassConditionalProbabilities(), args);
  }

  /**
   * Global help info for this method
   *
   * @return the global help info
   */
  @Override
  public String globalInfo() {
    return "Converts the values of nominal and/or numeric attributes into "
      + "class conditional probabilities. If there are k classes, then k "
      + "new attributes are created for each of the original ones, giving "
      + "pr(att val | class k).\n\nCan be useful for converting nominal attributes "
      + "with a lot of distinct values into something more manageable for learning "
      + "schemes that can't handle nominal attributes (as opposed to creating "
      + "binary indicator attributes). For nominal attributes, the user can "
      + "specify the number values above which an attribute will be converted "
      + "by this method. Normal distributions are assumed for numeric attributes.";
  }

  /**
   * Get whether numeric attributes are being excluded from the transformation
   *
   * @return true if numeric attributes are to be excluded
   */
  @OptionMetadata(displayName = "Exclude numeric attributes",
    description = "Don't apply this transformation to numeric attributes",
    commandLineParamName = "N", commandLineParamIsFlag = true,
    commandLineParamSynopsis = "-N", displayOrder = 1)
  public boolean getExcludeNumericAttributes() {
    return m_excludeNumericAttributes;
  }

  /**
   * Set whether numeric attributes are being excluded from the transformation
   *
   * @param e true if numeric attributes are to be excluded
   */
  public void setExcludeNumericAttributes(boolean e) {
    m_excludeNumericAttributes = e;
  }

  /**
   * Get whether nominal attributes are to be excluded from the transformation
   *
   * @return true if nominal attributes are to be excluded
   */
  @OptionMetadata(displayName = "Exclude nominal attributes",
    description = "Don't apply this transformation to nominal attributes",
    commandLineParamName = "C", commandLineParamIsFlag = true,
    commandLineParamSynopsis = "-C", displayOrder = 2)
  public boolean getExcludeNominalAttributes() {
    return m_excludeNominalAttributes;
  }

  /**
   * Set whether nominal attributes are to be excluded from the transformation
   *
   * @param e true if nominal attributes are to be excluded
   */
  public void setExcludeNominalAttributes(boolean e) {
    m_excludeNominalAttributes = e;
  }

  /**
   * If true, when generating attributes, spread weight of old
   * attribute across new attributes. Do not give each new attribute the old weight.
   *
   * @param p whether weight is spread
   */
  @OptionMetadata(displayName = "Spread weight across new attributes",
          description = "When generating attributes, spread weight of old\n" +
                  "attribute across new attributes. Do not give each new attribute the old weight.",
          commandLineParamName = "spread-attribute-weight", commandLineParamIsFlag = true,
          commandLineParamSynopsis = "-spread-attribute-weight", displayOrder = 3)
  public void setSpreadAttributeWeight(boolean p) {
    m_SpreadAttributeWeight = p;
  }

  /**
   * If true, when generating attributes, spread weight of old
   * attribute across new attributes. Do not give each new attribute the old weight.
   *
   * @return whether weight is spread
   */
  public boolean getSpreadAttributeWeight() {
    return m_SpreadAttributeWeight;
  }

  /**
   * Get the minimum number of values a nominal attribute must have in order to
   * be transformed. -1 indicates no minimum (i.e. transform all nominal
   * attributes)
   *
   * @return the number of values of a nominal attribute after which the
   *          transformation applies
   */
  @OptionMetadata(displayName = "Nominal conversion threshold",
    description = "Transform nominal attributes with at least this many"
      + " values.\n-1 means always transform.",
    commandLineParamName = "min-values",
    commandLineParamSynopsis = "-min-values <integer>", displayOrder = 3)
  public int getNominalConversionThreshold() {
    return m_nominalConversionThreshold;
  }

  /**
   * Set the minimum number of values a nominal attribute must have in order to
   * be transformed. -1 indicates no minimum (i.e. transform all nominal
   * attributes)
   *
   * @param n the number of values of a nominal attribute after which the
   *          transformation applies
   */
  public void setNominalConversionThreshold(int n) {
    m_nominalConversionThreshold = n;
  }

  @Override
  protected Instances determineOutputFormat(Instances inputFormat)
    throws Exception {

    if (m_excludeNominalAttributes && m_excludeNumericAttributes) {
      throw new Exception("No transformation will be done if both nominal and "
        + "numeric attributes are excluded!");
    }

    if (m_remove == null) {
      List<Integer> attsToExclude = new ArrayList<Integer>();
      if (m_excludeNumericAttributes) {
        for (int i = 0; i < inputFormat.numAttributes(); i++) {
          if (inputFormat.attribute(i).isNumeric()
            && i != inputFormat.classIndex()) {
            attsToExclude.add(i);
          }
        }
      }

      if (m_excludeNominalAttributes || m_nominalConversionThreshold > 1) {
        for (int i = 0; i < inputFormat.numAttributes(); i++) {
          if (inputFormat.attribute(i).isNominal()
            && i != inputFormat.classIndex()) {
            if (m_excludeNominalAttributes
              || inputFormat.attribute(i).numValues() < m_nominalConversionThreshold) {
              attsToExclude.add(i);
            }
          }
        }
      }

      if (attsToExclude.size() > 0) {
        int[] r = new int[attsToExclude.size()];
        for (int i = 0; i < attsToExclude.size(); i++) {
          r[i] = attsToExclude.get(i);
        }
        m_remove = new Remove();
        m_remove.setAttributeIndicesArray(r);
        m_remove.setInputFormat(inputFormat);

        Remove forRetaining = new Remove();
        forRetaining.setAttributeIndicesArray(r);
        forRetaining.setInvertSelection(true);
        forRetaining.setInputFormat(inputFormat);
        m_unchanged = Filter.useFilter(inputFormat, forRetaining);
      }
    }

    ArrayList<Attribute> atts = new ArrayList<Attribute>();
    for (int i = 0; i < inputFormat.numAttributes(); i++) {
      if (i != inputFormat.classIndex()) {
        if (m_unchanged != null
          && m_unchanged.attribute(inputFormat.attribute(i).name()) != null) {
          atts.add((Attribute) m_unchanged.attribute(
            inputFormat.attribute(i).name()).copy());
          continue;
        }

        for (int j = 0; j < inputFormat.classAttribute().numValues(); j++) {
          String name =
            "pr_" + inputFormat.attribute(i).name() + "|"
              + inputFormat.classAttribute().value(j);
          Attribute a = new Attribute(name);
          if (getSpreadAttributeWeight()) {
            a.setWeight(inputFormat.attribute(i).weight() / inputFormat.classAttribute().numValues());
          } else {
            a.setWeight(inputFormat.attribute(i).weight());
          }
          atts.add(a);
        }
      }
    }

    atts.add((Attribute) inputFormat.classAttribute().copy());
    Instances data = new Instances(inputFormat.relationName(), atts, 0);
    data.setClassIndex(data.numAttributes() - 1);

    return data;
  }

  @Override
  protected Instances process(Instances instances) throws Exception {
    if (m_estimator == null) {
      m_estimator = new NaiveBayes();

      Instances trainingData = new Instances(instances);
      if (m_remove != null) {
        trainingData = Filter.useFilter(instances, m_remove);
      }
      m_estimator.buildClassifier(trainingData);
    }

    if (m_estimatorLookup == null) {
      m_estimatorLookup = new HashMap<String, Estimator[]>();
      Estimator[][] estimators = m_estimator.getConditionalEstimators();
      Instances header = m_estimator.getHeader();
      int index = 0;
      for (int i = 0; i < header.numAttributes(); i++) {
        if (i != header.classIndex()) {
          m_estimatorLookup.put(header.attribute(i).name(), estimators[index]);
          index++;
        }
      }
    }

    Instances result =
      new Instances(getOutputFormat(), instances.numInstances());
    for (int i = 0; i < instances.numInstances(); i++) {

      Instance current = instances.instance(i);
      Instance instNew = convertInstance(current);

      // add instance to output
      result.add(instNew);
    }

    return result;
  }

  /**
   * Convert an input instance
   *
   * @param current the input instance to convert
   * @return a transformed instance
   * @throws Exception if a problem occurs
   */
  protected Instance convertInstance(Instance current) throws Exception {
    double[] vals = new double[getOutputFormat().numAttributes()];
    int index = 0;
    for (int j = 0; j < current.numAttributes(); j++) {
      if (j != current.classIndex()) {
        if (m_unchanged != null
          && m_unchanged.attribute(current.attribute(j).name()) != null) {
          vals[index++] = current.value(j);
        } else {
          Estimator[] estForAtt =
            m_estimatorLookup.get(current.attribute(j).name());
          for (int k = 0; k < current.classAttribute().numValues(); k++) {
            if (current.isMissing(j)) {
              vals[index++] = Utils.missingValue();
            } else {
              double e = estForAtt[k].getProbability(current.value(j));
              vals[index++] = e;
            }
          }
        }
      }
    }

    vals[vals.length - 1] = current.classValue();
    DenseInstance instNew = new DenseInstance(current.weight(), vals);

    return instNew;
  }

  @Override public boolean input(Instance inst) throws Exception {
    if (!isFirstBatchDone()) {
      return super.input(inst);
    }

    Instance converted = convertInstance(inst);
    push(converted);

    return true;
  }

  /**
   * Returns the Capabilities of this filter.
   *
   * @return the capabilities of this object
   * @see Capabilities
   */
  @Override
  public Capabilities getCapabilities() {
    return new NaiveBayes().getCapabilities();
  }

  /**
   * Returns the revision string.
   *
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: $");
  }

  /**
   * Get the naive Bayes estimator in use
   *
   * @return the naive Bayes estimator
   */
  @ProgrammaticProperty
  public NaiveBayes getEstimator() {
    return m_estimator;
  }

  /**
   * Set the naive Bayes estimator to use
   *
   * @param nb the naive Bayes estimator to use
   */
  public void setEstimator(NaiveBayes nb) {
    m_estimator = nb;
  }

  /**
   * Get the remove filter in use
   *
   * @return
   */
  @ProgrammaticProperty
  public Remove getRemoveFilter() {
    return m_remove;
  }

  public void setRemoveFilter(Remove r) {
    m_remove = r;
    m_unchanged = r.getOutputFormat();
  }
}

