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
 *    LatentSemanticAnalysis.java
 *    Copyright (C) 2008 Amri Napolitano
 *
 */

package weka.attributeSelection;

import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Check;
import weka.core.CheckOptionHandler;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.matrix.Matrix;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.SparseInstance;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.matrix.SingularValueDecomposition;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NominalToBinary;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Enumeration;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * Performs latent semantic analysis and transformation of the data. 
 * Use in conjunction with a Ranker search. A low-rank approximation 
 * of the full data is found by specifying the number of singular values 
 * to use. The dataset may be transformed to give the relation of either 
 * the attributes or the instances (default) to the concept space created 
 * by the transformation.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -N
 *  Normalize input data.</pre>
 * 
 * <pre> -R
 *  Rank approximation used in LSA. May be actual number of 
 *  LSA attributes to include (if greater than 1) or a proportion 
 *  of total singular values to account for (if between 0 and 1). 
 *  A value less than or equal to zero means use all latent variables.
 *  (default = 0.95)</pre>
 * 
 * <pre> -A
 *  Maximum number of attributes to include in 
 *  transformed attribute names. (-1 = include all)</pre>
 * 
 <!-- options-end -->
 *
 * @author Amri Napolitano
 * @version $Revision$
 */

public class LatentSemanticAnalysis 
extends UnsupervisedAttributeEvaluator 
implements AttributeTransformer, OptionHandler {
  
  /** For serialization */
  static final long serialVersionUID = -8712112988018106198L;
  
  /** The data to transform analyse/transform */
  private Instances m_trainInstances;
  
  /** Keep a copy for the class attribute (if set) */
  private Instances m_trainHeader;
  
  /** The header for the transformed data format */
  private Instances m_transformedFormat;
  
  /** Data has a class set */
  private boolean m_hasClass;
  
  /** Class index */
  private int m_classIndex;
  
  /** Number of attributes */
  private int m_numAttributes;
  
  /** Number of instances */
  private int m_numInstances;
  
  /** Is transpose necessary because numAttributes < numInstances? */
  private boolean m_transpose = false;
  
  /** Will hold the left singular vectors */
  private Matrix m_u = null;
  
  /** Will hold the singular values */
  private Matrix m_s = null;
  
  /** Will hold the right singular values */
  private Matrix m_v = null;
  
  /** Will hold the matrix used to transform instances to the new feature space */
  private Matrix m_transformationMatrix = null;
  
  /** Filters for original data */
  private ReplaceMissingValues m_replaceMissingFilter;
  private Normalize m_normalizeFilter;
  private NominalToBinary m_nominalToBinaryFilter;
  private Remove m_attributeFilter;
  
  /** The number of attributes in the LSA transformed data */
  private int m_outputNumAttributes = -1;
  
  /** Normalize the input data? */
  private boolean m_normalize = false;
  
  /** The approximation rank to use (between 0 and 1 means coverage proportion) */
  private double m_rank = 0.95;
  
  /** The sum of the squares of the singular values */
  private double m_sumSquaredSingularValues = 0.0;
  
  /** The actual rank number to use for computation */
  private int m_actualRank = -1;
  
  /** Maximum number of attributes in the transformed attribute name */
  private int m_maxAttributesInName = 5;
  
  /**
   * Returns a string describing this attribute transformer
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Performs latent semantic analysis and transformation of the data. Use in " +
            "conjunction with a Ranker search. A low-rank approximation of the full data is " +
            "found by either specifying the number of singular values to use or specifying a " +
            "proportion of the singular values to cover.";
  }
  
  /**
   * Returns an enumeration describing the available options. <p>
   *
   * @return an enumeration of all the available options.
   **/
  public Enumeration listOptions () {
    Vector options = new Vector(4);
    options.addElement(new Option("\tNormalize input data.", "N", 0, "-N"));
    
    options.addElement(new Option("\tRank approximation used in LSA. \n" +
                                   "\tMay be actual number of LSA attributes \n" +
                                   "\tto include (if greater than 1) or a \n" +
                                   "\tproportion of total singular values to \n" +
                                   "\taccount for (if between 0 and 1). \n" +
                                   "\tA value less than or equal to zero means \n" +
                                   "\tuse all latent variables.(default = 0.95)",
                                   "R",1,"-R"));
    
    options.addElement(new Option("\tMaximum number of attributes to include\n" +
                                   "\tin transformed attribute names.\n" +
                                   "\t(-1 = include all)"
                                   , "A", 1, "-A"));
    return  options.elements();
  }
  
  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -N
   *  Normalize input data.</pre>
   * 
   * <pre> -R
   *  Rank approximation used in LSA. May be actual number of 
   *  LSA attributes to include (if greater than 1) or a proportion 
   *  of total singular values to account for (if between 0 and 1). 
   *  A value less than or equal to zero means use all latent variables.
   *  (default = 0.95)</pre>
   * 
   * <pre> -A
   *  Maximum number of attributes to include in 
   *  transformed attribute names. (-1 = include all)</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions (String[] options)
  throws Exception {
    resetOptions();
    String optionString;
    
    //set approximation rank
    optionString = Utils.getOption('R', options);
    if (optionString.length() != 0) {
      double temp;
      temp = Double.valueOf(optionString).doubleValue();
      setRank(temp);
    }
    
    //set number of attributes to use in transformed names
    optionString = Utils.getOption('A', options);
    if (optionString.length() != 0) {
      setMaximumAttributeNames(Integer.parseInt(optionString));
    }
    
    //set normalize option
    setNormalize(Utils.getFlag('N', options));
  }
  
  /**
   * Reset to defaults
   */
  private void resetOptions() {
    m_rank = 0.95;
    m_normalize = true;
    m_maxAttributesInName = 5;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String normalizeTipText() {
    return "Normalize input data.";
  }
  
  /**
   * Set whether input data will be normalized.
   * @param newNormalize true if input data is to be normalized
   */
  public void setNormalize(boolean newNormalize) {
    m_normalize = newNormalize;
  }
  
  /**
   * Gets whether or not input data is to be normalized
   * @return true if input data is to be normalized
   */
  public boolean getNormalize() {
    return m_normalize;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String rankTipText() {
    return "Matrix rank to use for data reduction. Can be a" +
    " proportion to indicate desired coverage";
  }
  
  /**
   * Sets the desired matrix rank (or coverage proportion) for feature-space reduction
   * @param newRank the desired rank (or coverage) for feature-space reduction
   */
  public void setRank(double newRank) {
      m_rank = newRank;
  }
  
  /**
   * Gets the desired matrix rank (or coverage proportion) for feature-space reduction
   * @return the rank (or coverage) for feature-space reduction
   */
  public double getRank() {
    return m_rank;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String maximumAttributeNamesTipText() {
    return "The maximum number of attributes to include in transformed attribute names.";
  }
  
  /**
   * Sets maximum number of attributes to include in
   * transformed attribute names.
   * @param newMaxAttributes the maximum number of attributes
   */
  public void setMaximumAttributeNames(int newMaxAttributes) {
    m_maxAttributesInName = newMaxAttributes;
  }
  
  /**
   * Gets maximum number of attributes to include in
   * transformed attribute names.
   * @return the maximum number of attributes
   */
  public int getMaximumAttributeNames() {
    return m_maxAttributesInName;
  }
  
  /**
   * Gets the current settings of LatentSemanticAnalysis
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions () {
    
    String[] options = new String[5];
    int current = 0;
    
    if (getNormalize()) {
      options[current++] = "-N";
    }
    
    options[current++] = "-R";
    options[current++] = "" + getRank();
    
    options[current++] = "-A";
    options[current++] = "" + getMaximumAttributeNames();
    
    while (current < options.length) {
      options[current++] = "";
    }
    
    return  options;
  }
  
  /**
   * Returns the capabilities of this evaluator.
   *
   * @return            the capabilities of this evaluator
   * @see               Capabilities
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();
    
    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);
    
    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.NUMERIC_CLASS);
    result.enable(Capability.DATE_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);
    result.enable(Capability.NO_CLASS);
    
    return result;
  }
  
  /**
   * Initializes the singular values/vectors and performs the analysis
   * @param data the instances to analyse/transform
   * @throws Exception if analysis fails
   */
  public void buildEvaluator(Instances data) throws Exception {
    // can evaluator handle data?
    getCapabilities().testWithFail(data);
    
    buildAttributeConstructor(data);
  }
  
  /**
   * Initializes the singular values/vectors and performs the analysis
   * @param data the instances to analyse/transform
   * @throws Exception if analysis fails
   */
  private void buildAttributeConstructor (Instances data) throws Exception {
    // initialize attributes for performing analysis
    m_transpose = false;
    m_s = null;
    m_u = null;
    m_v = null;
    m_outputNumAttributes = -1;
    m_actualRank = -1;
    m_sumSquaredSingularValues = 0.0;
    
    m_trainInstances = new Instances(data);
    m_trainHeader = null;
    
    m_attributeFilter = null;
    m_nominalToBinaryFilter = null;
    
    m_replaceMissingFilter = new ReplaceMissingValues();
    m_replaceMissingFilter.setInputFormat(m_trainInstances);
    m_trainInstances = Filter.useFilter(m_trainInstances, m_replaceMissingFilter);
    
    // vector to hold indices of attributes to delete (class attribute, 
    // attributes that are all missing, or attributes with one distinct value)
    Vector attributesToRemove = new Vector();
    
    // if data has a class attribute
    if (m_trainInstances.classIndex() >= 0) {
      // make copy of training data so the class values can be appended to final 
      // transformed instances
      m_trainHeader = new Instances(m_trainInstances);
      
      m_hasClass = true;
      m_classIndex = m_trainInstances.classIndex();
      
      // set class attribute to be removed
      attributesToRemove.addElement(new Integer(m_classIndex));
    }
    
    // normalize data if desired
    if (m_normalize) {
      m_normalizeFilter = new Normalize();
      m_normalizeFilter.setInputFormat(m_trainInstances);
      m_trainInstances = Filter.useFilter(m_trainInstances, m_normalizeFilter);
    }
    
    // convert any nominal attributes to binary numeric attributes
    m_nominalToBinaryFilter = new NominalToBinary();
    m_nominalToBinaryFilter.setInputFormat(m_trainInstances);
    m_trainInstances = Filter.useFilter(m_trainInstances, m_nominalToBinaryFilter);
    
    // delete any attributes with only one distinct value or are all missing
    for (int i = 0; i < m_trainInstances.numAttributes(); i++) {
      if (m_trainInstances.numDistinctValues(i) <= 1) {
        attributesToRemove.addElement(new Integer(i));
      }
    }
    
    // remove columns from the data if necessary
    if (attributesToRemove.size() > 0) {
      m_attributeFilter = new Remove();
      int [] todelete = new int[attributesToRemove.size()];
      for (int i = 0; i < attributesToRemove.size(); i++) {
        todelete[i] = ((Integer)(attributesToRemove.elementAt(i))).intValue();
      }
      m_attributeFilter.setAttributeIndicesArray(todelete);
      m_attributeFilter.setInvertSelection(false);
      m_attributeFilter.setInputFormat(m_trainInstances);
      m_trainInstances = Filter.useFilter(m_trainInstances, m_attributeFilter);
    }
    
    // can evaluator handle the processed data ? e.g., enough attributes?
    getCapabilities().testWithFail(m_trainInstances);
    
    // record properties of final, ready-to-process data
    m_numInstances = m_trainInstances.numInstances();
    m_numAttributes = m_trainInstances.numAttributes();
    
    // create matrix of attribute values and compute singular value decomposition
    double [][] trainValues = new double[m_numAttributes][m_numInstances];
    for (int i = 0; i < m_numAttributes; i++) {
      trainValues[i] = m_trainInstances.attributeToDoubleArray(i);
    }
    Matrix trainMatrix = new Matrix(trainValues);
    // svd requires rows >= columns, so transpose data if necessary
    if (m_numAttributes < m_numInstances) {
      m_transpose = true;
      trainMatrix = trainMatrix.transpose();
    }
    SingularValueDecomposition trainSVD = trainMatrix.svd();
    m_u = trainSVD.getU(); // left singular vectors
    m_s = trainSVD.getS(); // singular values
    m_v = trainSVD.getV(); // right singular vectors
    
    // find actual rank to use
    int maxSingularValues = trainSVD.rank();
    for (int i = 0; i < m_s.getRowDimension(); i++) {
      m_sumSquaredSingularValues += m_s.get(i, i) * m_s.get(i, i);
    }
    if (maxSingularValues == 0) { // no nonzero singular values (shouldn't happen)
      // reset values from computation
      m_s = null;
      m_u = null;
      m_v = null;
      m_sumSquaredSingularValues = 0.0;
      
      throw new Exception("SVD computation produced no non-zero singular values.");
    }
    if (m_rank > maxSingularValues || m_rank <= 0) { // adjust rank if too high or too low
      m_actualRank = maxSingularValues;
    } else if (m_rank < 1.0) { // determine how many singular values to include for desired coverage
      double currentSumOfSquaredSingularValues = 0.0;
      for (int i = 0; i < m_s.getRowDimension() && m_actualRank == -1; i++) {
        currentSumOfSquaredSingularValues += m_s.get(i, i) * m_s.get(i, i);
        if (currentSumOfSquaredSingularValues / m_sumSquaredSingularValues >= m_rank) {
          m_actualRank = i + 1;
        }
      }
    } else {
      m_actualRank = (int) m_rank;
    }
    
    // lower matrix ranks, adjust for transposition (if necessary), and
    // compute matrix for transforming future instances
    if (m_transpose) {
      Matrix tempMatrix = m_u;
      m_u = m_v;
      m_v = tempMatrix;
    }
    m_u = m_u.getMatrix(0, m_u.getRowDimension() - 1, 0, m_actualRank - 1);
    m_s = m_s.getMatrix(0, m_actualRank - 1, 0, m_actualRank - 1);
    m_v = m_v.getMatrix(0, m_v.getRowDimension() - 1, 0, m_actualRank - 1);
    m_transformationMatrix = m_u.times(m_s.inverse());
    
    //create dataset header for transformed instances
    m_transformedFormat = setOutputFormat();
  }
  
  /**
   * Set the format for the transformed data
   * @return a set of empty Instances (header only) in the new format
   */
  private Instances setOutputFormat() {
    // if analysis hasn't been performed (successfully) yet
    if (m_s == null) {
      return null;
    }
    
    // set up transformed attributes
    if (m_hasClass) {
      m_outputNumAttributes = m_actualRank + 1;
    } else {
      m_outputNumAttributes = m_actualRank;
    }
    int numAttributesInName = m_maxAttributesInName;
    if (numAttributesInName <= 0 || numAttributesInName >= m_numAttributes) {
      numAttributesInName = m_numAttributes;
    }
    FastVector attributes = new FastVector(m_outputNumAttributes);
    for (int i = 0; i < m_actualRank; i++) {
      // create attribute name
      String attributeName = "";
      double [] attributeCoefficients = 
        m_transformationMatrix.getMatrix(0, m_numAttributes - 1, i, i).getColumnPackedCopy();
      for (int j = 0; j < numAttributesInName; j++) {
        if (j > 0) {
          attributeName += "+";
        }
        attributeName += Utils.doubleToString(attributeCoefficients[j], 5, 3);
        attributeName += m_trainInstances.attribute(j).name();
      }
      if (numAttributesInName < m_numAttributes) {
        attributeName += "...";
      }
      // add attribute
      attributes.addElement(new Attribute(attributeName));
    }
    // add original class attribute if present
    if (m_hasClass) {
      attributes.addElement(m_trainHeader.classAttribute().copy());
    }
    // create blank header
    Instances outputFormat = new Instances(m_trainInstances.relationName() + "_LSA", 
        attributes, 0);
    m_outputNumAttributes = outputFormat.numAttributes();
    // set class attribute if applicable
    if (m_hasClass) {
      outputFormat.setClassIndex(m_outputNumAttributes - 1);
    }
    
    return outputFormat;
  }
  
  /**
   * Returns just the header for the transformed data (ie. an empty
   * set of instances. This is so that AttributeSelection can
   * determine the structure of the transformed data without actually
   * having to get all the transformed data through getTransformedData().
   * @return the header of the transformed data.
   * @throws Exception if the header of the transformed data can't
   * be determined.
   */
  public Instances transformedHeader() throws Exception {
    if (m_s == null) {
      throw new Exception("Latent Semantic Analysis hasn't been successfully performed.");
    }
    return m_transformedFormat;
  }
  
  /**
   * Transform the supplied data set (assumed to be the same format
   * as the training data)
   * @return the transformed training data
   * @throws Exception if transformed data can't be returned
   */
  public Instances transformedData(Instances data) throws Exception {
    if (m_s == null) {
      throw new Exception("Latent Semantic Analysis hasn't been built yet");
    }
    
    Instances output = new Instances(m_transformedFormat, m_numInstances);
    
    // the transformed version of instance i from the training data
    // is stored as the i'th row vector in v (the right singular vectors)
    for (int i = 0; i < data.numInstances(); i++) {
      Instance currentInstance = data.instance(i);
      // record attribute values for converted instance
      double [] newValues = new double[m_outputNumAttributes];
      for (int j = 0; j < m_actualRank; j++) { // fill in values from v
        newValues[j] = m_v.get(i, j);
      }
      if (m_hasClass) { // copy class value if applicable
        newValues[m_outputNumAttributes - 1] = currentInstance.classValue();
      }
      //create new instance with recorded values and add to output dataset
      Instance newInstance;
      if (currentInstance instanceof SparseInstance) {
        newInstance = new SparseInstance(currentInstance.weight(), newValues);
      } else {
        newInstance = new Instance(currentInstance.weight(), newValues);
      }
      output.add(newInstance);
    }
    
    return output;
  }
  
  /**
   * Evaluates the merit of a transformed attribute. This is defined
   * to be the square of the singular value for the latent variable 
   * corresponding to the transformed attribute.
   * @param att the attribute to be evaluated
   * @return the merit of a transformed attribute
   * @throws Exception if attribute can't be evaluated
   */
  public double evaluateAttribute(int att) throws Exception {
    if (m_s == null) {
      throw new Exception("Latent Semantic Analysis hasn't been successfully" +
                            " performed yet!");
    }
    
    //return the square of the corresponding singular value
    return (m_s.get(att, att) * m_s.get(att, att)) / m_sumSquaredSingularValues;
  }
  
  /**
   * Transform an instance in original (unnormalized) format
   * @param instance an instance in the original (unnormalized) format
   * @return a transformed instance
   * @throws Exception if instance can't be transformed
   */
  public Instance convertInstance(Instance instance) throws Exception {
    if (m_s == null) {
      throw new Exception("convertInstance: Latent Semantic Analysis not " +
                           "performed yet.");
    }
    
    // array to hold new attribute values
    double [] newValues = new double[m_outputNumAttributes];
    
    // apply filters so new instance is in same format as training instances
    Instance tempInstance = (Instance)instance.copy();
    if (!instance.dataset().equalHeaders(m_trainHeader)) {
      throw new Exception("Can't convert instance: headers don't match: " +
      "LatentSemanticAnalysis\n" + instance.dataset().equalHeadersMsg(m_trainHeader));
    }
    // replace missing values
    m_replaceMissingFilter.input(tempInstance);
    m_replaceMissingFilter.batchFinished();
    tempInstance = m_replaceMissingFilter.output();
    // normalize
    if (m_normalize) {
      m_normalizeFilter.input(tempInstance);
      m_normalizeFilter.batchFinished();
      tempInstance = m_normalizeFilter.output();
    }
    // convert nominal attributes to binary
    m_nominalToBinaryFilter.input(tempInstance);
    m_nominalToBinaryFilter.batchFinished();
    tempInstance = m_nominalToBinaryFilter.output();
    // remove class/other attributes
    if (m_attributeFilter != null) {
      m_attributeFilter.input(tempInstance);
      m_attributeFilter.batchFinished();
      tempInstance = m_attributeFilter.output();
    }
    
    // record new attribute values
    if (m_hasClass) { // copy class value
      newValues[m_outputNumAttributes - 1] = instance.classValue();
    }
    double [][] oldInstanceValues = new double[1][m_numAttributes];
    oldInstanceValues[0] = tempInstance.toDoubleArray();
    Matrix instanceVector = new Matrix(oldInstanceValues); // old attribute values
    instanceVector = instanceVector.times(m_transformationMatrix); // new attribute values
    for (int i = 0; i < m_actualRank; i++) {
      newValues[i] = instanceVector.get(0, i);
    }
    
    // return newly transformed instance
    if (instance instanceof SparseInstance) {
      return new SparseInstance(instance.weight(), newValues);
    } else {
      return new Instance(instance.weight(), newValues);
    }
  }
  
  /**
   * Returns a description of this attribute transformer
   * @return a String describing this attribute transformer
   */
  public String toString() {
    if (m_s == null) {
      return "Latent Semantic Analysis hasn't been built yet!";
    } else {
      return "\tLatent Semantic Analysis Attribute Transformer\n\n"
      + lsaSummary();
    }
  }
  
  /**
   * Return a summary of the analysis
   * @return a summary of the analysis.
   */
  private String lsaSummary() {
    StringBuffer result = new StringBuffer();
    
    // print number of latent variables used
    result.append("Number of latent variables utilized: " + m_actualRank);
    
    // print singular values
    result.append("\n\nSingularValue\tLatentVariable#\n");
    // create single array of singular values rather than diagonal matrix
    for (int i = 0; i < m_actualRank; i++) {
      result.append(Utils.doubleToString(m_s.get(i, i), 9, 5) + "\t" + (i + 1) + "\n");
    }
    
    // print attribute vectors
    result.append("\nAttribute vectors (left singular vectors) -- row vectors show\n" +
                  "the relation between the original attributes and the latent \n" +
                  "variables computed by the singular value decomposition:\n");
    for (int i = 0; i < m_actualRank; i++) {
      result.append("LatentVariable#" + (i + 1) + "\t");
    }
    result.append("AttributeName\n");
    for (int i = 0; i < m_u.getRowDimension(); i++) { // for each attribute
      for (int j = 0; j < m_u.getColumnDimension(); j++) { // for each latent variable
        result.append(Utils.doubleToString(m_u.get(i, j), 9, 5) + "\t\t");
      }
      result.append(m_trainInstances.attribute(i).name() + "\n");
    }
    
    // print instance vectors
    result.append("\n\nInstance vectors (right singular vectors) -- column\n" +
                  "vectors show the relation between the original instances and the\n" +
                  "latent variables computed by the singular value decomposition:\n");
    for (int i = 0; i < m_numInstances; i++) {
      result.append("Instance#" + (i + 1) + "\t");
    }
    result.append("LatentVariable#\n");
    for (int i = 0; i < m_v.getColumnDimension(); i++) { // for each instance
      for (int j = 0; j < m_v.getRowDimension(); j++) { // for each latent variable
        // going down columns instead of across rows because we're
        // printing v' but have v stored
        result.append(Utils.doubleToString(m_v.get(j, i), 9, 5) + "\t");
      }
      result.append((i + 1) + "\n");
    }
    
    return result.toString();
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
   * @param argv should contain the command line arguments to the
   * evaluator/transformer (see AttributeSelection)
   */
  public static void main(String [] argv) {
    runEvaluator(new LatentSemanticAnalysis(), argv);
  }
}
