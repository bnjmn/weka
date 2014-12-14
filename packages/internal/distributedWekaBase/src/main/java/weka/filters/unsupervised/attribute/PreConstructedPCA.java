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
 * PreConstructedPCA.java
 * Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.attribute;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.CommandlineRunnable;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.SparseInstance;
import weka.core.Utils;
import weka.core.matrix.EigenvalueDecomposition;
import weka.core.matrix.Matrix;
import weka.core.stats.ArffSummaryNumericMetric;
import weka.core.stats.NumericStats;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.CorrelationMatrixRowReduceTask;
import weka.filters.Filter;
import weka.filters.PreconstructedFilter;
import weka.filters.StreamableFilter;
import weka.gui.GPCIgnore;
import weka.gui.beans.KFIgnore;
import distributed.core.DistributedJobConfig;

/**
 * <!-- globalinfo-start --> Performs a principal components analysis and
 * transformation of the data.<br/>
 * Dimensionality reduction is accomplished by choosing enough eigenvectors to
 * account for some percentage of the variance in the original data -- default
 * 0.95 (95%).<br/>
 * Based on code of the attribute selection scheme 'PrincipalComponents' by Mark
 * Hall and Gabi Schmidberger.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -header &lt;path to ARFF header&gt;
 *  Path to the ARFF header used when the matrix
 *  was constructed. Must contain summary attributes.
 * </pre>
 * 
 * <pre>
 * -matrix &lt;path to matrix file&gt;
 *  Path to the correlation/covariance matrix.
 * </pre>
 * 
 * <pre>
 * -covariance
 *  Matrix is a covariance matrix (rather than correlation).
 * </pre>
 * 
 * <pre>
 * -keep-class
 *  Keep the class (if set). Set this if the
 *  class was retained when computing the matrix (i.e. there is a column
 *  in the matrix corresponding to the class).
 * </pre>
 * 
 * <pre>
 * -R &lt;num&gt;
 *  Retain enough PC attributes to account
 *  for this proportion of variance in the original data.
 *  (default: 0.95)
 * </pre>
 * 
 * <pre>
 * -A &lt;num&gt;
 *  Maximum number of attributes to include in 
 *  transformed attribute names.
 *  (-1 = include all, default: 5)
 * </pre>
 * 
 * <pre>
 * -M &lt;num&gt;
 *  Maximum number of PC attributes to retain.
 *  (-1 = include all, default: -1)
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
@KFIgnore
@GPCIgnore
public class PreConstructedPCA extends Filter implements StreamableFilter,
  Serializable, PreconstructedFilter, OptionHandler, CommandlineRunnable {

  /**
   * Default number of original attribute names to include in the transformed
   * attribute names
   */
  public static final int MAX_ATTRIB_NAMES = 5;

  /** Default proportion of variance covered by PCs */
  public static final double DEFAULT_VARIANCE_COVERED = 0.95;

  /** For serialization. */
  private static final long serialVersionUID = -5338363571761947879L;

  /** The correlation/covariance matrix - to be supplied */
  protected Matrix m_matrix;

  /**
   * Will hold the unordered linear transformations of the (normalized) original
   * data.
   */
  protected double[][] m_eigenvectors;

  /** Eigenvalues for the corresponding eigenvectors. */
  protected double[] m_eigenvalues;

  /** Sorted eigenvalues. */
  protected int[] m_sortedEigens;

  /** Sum of the eigenvalues. */
  protected double m_sumOfEigenvalues;

  /** Original header with summary attributes. */
  protected Instances m_originalHeader;

  /** Header with summary attributes (and possibly class) removed. */
  protected Instances m_header;

  /** Stats for the attributes that are input to the PCA. */
  protected List<NumericStats> m_stats;

  /**
   * Holds the indexes of any attributes/columns that might need to be removed
   * from the matrix (i.e. if the data was all missing or a constant value for
   * that attribute).
   */
  protected Set<Integer> m_useless;

  /** True if the class is to be part of the PCA. */
  protected boolean m_keepClassIfSet;

  /** True if the matrix is a covariance rather than correlation one. */
  protected boolean m_matrixIsCovariance;

  /** True if the data has a class attribute and we've removed it */
  protected boolean m_hasClass;

  /** Holds the transformed output format */
  protected Instances m_transformedFormat;

  /** The number of attributes in the pc transformed data. */
  protected int m_outputNumAtts = -1;

  /**
   * the amount of variance to cover in the original data when retaining the
   * best n PC's.
   */
  protected double m_coverVariance = DEFAULT_VARIANCE_COVERED;

  /** maximum number of attributes in the transformed attribute name. */
  protected int m_maxAttrsInName = MAX_ATTRIB_NAMES;

  /** maximum number of attributes in the transformed data (-1 for all). */
  protected int m_maxAttributes = -1;

  /**
   * Path to the matrix file (format readable by weka.core.Matrix) if we are
   * loading it
   */
  protected String m_pathToMatrix = "";

  /** Path to the ARFF header with summary attributes (if we are loading it) */
  protected String m_pathToHeaderWithSummaryAtts = "";

  /**
   * Default constructor. All settings, including matrix and header, must be set
   * via mutator methods before use
   */
  public PreConstructedPCA() {
  }

  /**
   * Returns the capabilities of this evaluator.
   * 
   * @return the capabilities of this evaluator
   * @see Capabilities
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.BINARY_ATTRIBUTES);

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.NUMERIC_CLASS);
    result.enable(Capability.DATE_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);
    result.enable(Capability.NO_CLASS);

    return result;
  }

  /**
   * Construct a new PreConstructedPCA.
   * 
   * @param headerWithSummaryAtts header of the data used to construct the
   *          matrix from. Must contain the meta summary attributes.
   * @param matrix the correlation or covariance matrix
   * @param keepClassIfSet true if the class attribute was retained when
   *          computing the matrix (i.e. there is a column in the matrix for the
   *          class)
   * @param isCovariance true if the matrix is a covariance matrix rather than
   *          correlation matrix
   * @throws Exception if a problem occurs
   */
  public PreConstructedPCA(Instances headerWithSummaryAtts, Matrix matrix,
    boolean keepClassIfSet, boolean isCovariance) throws Exception {

    m_matrix = matrix;
    m_keepClassIfSet = keepClassIfSet;
    m_matrixIsCovariance = isCovariance;

    setupStatsFromInstancesWithSummaryAtts(headerWithSummaryAtts);
  }

  /**
   * Construct a new PreConstructedPCA.
   * 
   * @param header header of the data used to construct the matrix from. Should
   *          NOT contain any meta summary attributes.
   * @param matrix the correlation or covariance matrix
   * @param stats a list of NumericStats objects corresponding to the attributes
   *          in the header
   * @param keepClassIfSet true if the class attribute was retained when
   *          computing the matrix (i.e. there is a column in the matrix for the
   *          class)
   * @param isCovariance true if the matrix is a covariance matrix rather than
   *          correlation matrix
   * @throws Exception if a problem occurs
   */
  public PreConstructedPCA(Instances header, Matrix matrix,
    List<NumericStats> stats, boolean keepClassIfSet, boolean isCovariance)
    throws Exception {
    m_matrix = matrix;
    m_header = header;
    m_stats = stats;
    m_keepClassIfSet = keepClassIfSet;
    m_matrixIsCovariance = isCovariance;
    m_originalHeader = new Instances(m_header, 0);
  }

  /**
   * Returns a string describing this filter.
   * 
   * @return a description of the filter suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "Performs a principal components analysis and transformation of "
      + "the data.\n"
      + "Dimensionality reduction is accomplished by choosing enough eigenvectors "
      + "to account for some percentage of the variance in the original data -- "
      + "default 0.95 (95%).\n"
      + "Based on code of the attribute selection scheme 'PrincipalComponents' "
      + "by Mark Hall and Gabi Schmidberger.";
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.addElement(new Option(
      "\tPath to the ARFF header used when the matrix\n\t"
        + "was constructed. Must contain summary attributes.", "header", 1,
      "-header <path to ARFF header>"));

    result.addElement(new Option(
      "\tPath to the correlation/covariance matrix.", "matrix", 1,
      "-matrix <path to matrix file>"));

    result.addElement(new Option(
      "\tMatrix is a covariance matrix (rather than correlation).",
      "covariance", 0, "-covariance"));

    result
      .addElement(new Option(
        "\tKeep the class (if set). Set this if the\n\t"
          + "class was retained when computing the matrix (i.e. there is a column\n\t"
          + "in the matrix corresponding to the class).", "keep-class", 0,
        "-keep-class"));

    result.addElement(new Option("\tRetain enough PC attributes to account\n"
      + "\tfor this proportion of variance in the original data.\n"
      + "\t(default: 0.95)", "R", 1, "-R <num>"));

    result.addElement(new Option(
      "\tMaximum number of attributes to include in \n"
        + "\ttransformed attribute names.\n"
        + "\t(-1 = include all, default: 5)", "A", 1, "-A <num>"));

    result.addElement(new Option(
      "\tMaximum number of PC attributes to retain.\n"
        + "\t(-1 = include all, default: -1)", "M", 1, "-M <num>"));

    return result.elements();
  }

  /**
   * Parses a list of options for this object.
   * <p/>
   * 
   * <!-- options-start --> Valid options are:
   * <p/>
   * 
   * <pre>
   * -header &lt;path to ARFF header&gt;
   *  Path to the ARFF header used when the matrix
   *  was constructed. Must contain summary attributes.
   * </pre>
   * 
   * <pre>
   * -matrix &lt;path to matrix file&gt;
   *  Path to the correlation/covariance matrix.
   * </pre>
   * 
   * <pre>
   * -covariance
   *  Matrix is a covariance matrix (rather than correlation).
   * </pre>
   * 
   * <pre>
   * -keep-class
   *  Keep the class (if set). Set this if the
   *  class was retained when computing the matrix (i.e. there is a column
   *  in the matrix corresponding to the class).
   * </pre>
   * 
   * <pre>
   * -R &lt;num&gt;
   *  Retain enough PC attributes to account
   *  for this proportion of variance in the original data.
   *  (default: 0.95)
   * </pre>
   * 
   * <pre>
   * -A &lt;num&gt;
   *  Maximum number of attributes to include in 
   *  transformed attribute names.
   *  (-1 = include all, default: 5)
   * </pre>
   * 
   * <pre>
   * -M &lt;num&gt;
   *  Maximum number of PC attributes to retain.
   *  (-1 = include all, default: -1)
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    String tmpStr;

    setPathToHeaderWithSummaryAtts(Utils.getOption("header", options));
    setPathToMatrix(Utils.getOption("matrix", options));

    setMatrixIsCovariance(Utils.getFlag("covariance", options));
    setKeepClassIfSet(Utils.getFlag("keep-class", options));

    tmpStr = Utils.getOption('R', options);
    if (tmpStr.length() != 0) {
      setVarianceCovered(Double.parseDouble(tmpStr));
    } else {
      setVarianceCovered(DEFAULT_VARIANCE_COVERED);
    }

    tmpStr = Utils.getOption('A', options);
    if (tmpStr.length() != 0) {
      setMaximumAttributeNames(Integer.parseInt(tmpStr));
    } else {
      setMaximumAttributeNames(MAX_ATTRIB_NAMES);
    }

    tmpStr = Utils.getOption('M', options);
    if (tmpStr.length() != 0) {
      setMaximumAttributes(Integer.parseInt(tmpStr));
    } else {
      setMaximumAttributes(-1);
    }
  }

  /**
   * Gets the current settings of the filter.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {
    List<String> result = new ArrayList<String>();

    if (!DistributedJobConfig.isEmpty(m_pathToHeaderWithSummaryAtts)) {
      result.add("-header");
      result.add(m_pathToHeaderWithSummaryAtts);
    }

    if (!DistributedJobConfig.isEmpty(m_pathToMatrix)) {
      result.add("-matrix");
      result.add(m_pathToMatrix);
    }

    if (getMatrixIsCovariance()) {
      result.add("-covariance");
    }

    if (getKeepClassIfSet()) {
      result.add("-keep-class");
    }

    result.add("-R");
    result.add("" + getVarianceCovered());

    result.add("-A");
    result.add("" + getMaximumAttributeNames());

    result.add("-M");
    result.add("" + getMaximumAttributes());

    return result.toArray(new String[result.size()]);

  }

  /**
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String pathToMatrixTipText() {
    return "Path to the correlation/covariance matrix";
  }

  /**
   * Set the path to the correlation/covariance matrix
   * 
   * @param path path to the matrix to use
   */
  public void setPathToMatrix(String path) {
    m_pathToMatrix = path;
  }

  /**
   * Get the path to the correlation/covariance matrix
   * 
   * @return path to the matrix to use
   */
  public String getPathToMatrix() {
    return m_pathToMatrix;
  }

  /**
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String pathToHeaderWithSummaryAttsTipText() {
    return "Path to the ARFF header (with summary attributes) that was "
      + "used when the matrix was calculated.";
  }

  /**
   * Set the path to the ARFF header (including summary attributes) used when
   * the matrix was constructed.
   * 
   * @param path the path to the ARFF header.
   */
  public void setPathToHeaderWithSummaryAtts(String path) {
    m_pathToHeaderWithSummaryAtts = path;
  }

  /**
   * Get the path to the ARFF header (including summary attributes) used when
   * the matrix was constructed.
   * 
   * @return the path to the ARFF header.
   */
  public String getPathToHeaderWihtSummaryAtts() {
    return m_pathToHeaderWithSummaryAtts;
  }

  /**
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String keepClassIfSet() {
    return "Keep the class attribute (if set). I.e. the class is part of the PCA";
  }

  /**
   * Set whether the class should be kept
   * 
   * @param keep true if the class is to be kept
   */
  public void setKeepClassIfSet(boolean keep) {
    m_keepClassIfSet = keep;
  }

  /**
   * Get whether the class should be kept
   * 
   * @return true if the class is to be kept
   */
  public boolean getKeepClassIfSet() {
    return m_keepClassIfSet;
  }

  /**
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String matrixIsCovarianceTipText() {
    return "True if the matrix is a covariance matrix rather than a correlation "
      + "matrix";
  }

  /**
   * Set whether the matrix is a covariance rather than correlation one
   * 
   * @param c true if the matrix is a covariance matrix
   */
  public void setMatrixIsCovariance(boolean c) {
    m_matrixIsCovariance = c;
  }

  /**
   * Get whether the matrix is a covariance rather than correlation one
   * 
   * @return true if the matrix is a covariance matrix
   */
  public boolean getMatrixIsCovariance() {
    return m_matrixIsCovariance;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String varianceCoveredTipText() {
    return "Retain enough PC attributes to account for this proportion of variance.";
  }

  /**
   * Sets the amount of variance to account for when retaining principal
   * components.
   * 
   * @param value the proportion of total variance to account for
   */
  public void setVarianceCovered(double value) {
    m_coverVariance = value;
  }

  /**
   * Gets the proportion of total variance to account for when retaining
   * principal components.
   * 
   * @return the proportion of variance to account for
   */
  public double getVarianceCovered() {
    return m_coverVariance;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String maximumAttributeNamesTipText() {
    return "The maximum number of attributes to include in transformed attribute names.";
  }

  /**
   * Sets maximum number of attributes to include in transformed attribute
   * names.
   * 
   * @param value the maximum number of attributes
   */
  public void setMaximumAttributeNames(int value) {
    m_maxAttrsInName = value;
  }

  /**
   * Gets maximum number of attributes to include in transformed attribute
   * names.
   * 
   * @return the maximum number of attributes
   */
  public int getMaximumAttributeNames() {
    return m_maxAttrsInName;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String maximumAttributesTipText() {
    return "The maximum number of PC attributes to retain.";
  }

  /**
   * Sets maximum number of PC attributes to retain.
   * 
   * @param value the maximum number of attributes
   */
  public void setMaximumAttributes(int value) {
    m_maxAttributes = value;
  }

  /**
   * Gets maximum number of PC attributes to retain.
   * 
   * @return the maximum number of attributes
   */
  public int getMaximumAttributes() {
    return m_maxAttributes;
  }

  /**
   * Sets the format of the input instances.
   * 
   * @param instancesInfo an Instances object containing the input instance
   *          structure (any instances contained in the object are ignored -
   *          only the structure is required).
   * @return true if the outputFormat may be collected immediately
   * @throws Exception if the input format can't be set successfully
   */
  @Override
  public boolean setInputFormat(Instances instancesInfo) throws Exception {

    if (!isConstructed()) {
      setup(instancesInfo);
    }

    return true;
  }

  /**
   * Initializes statistics using summary attributes in the header instances.
   * Also sets up headers.
   * 
   * @param insts the header instances (including summary attributes)
   * @throws Exception if a problem occurs
   */
  protected void setupStatsFromInstancesWithSummaryAtts(Instances insts)
    throws Exception {

    int classAdjust = (insts.classIndex() >= 0 && !m_keepClassIfSet && insts
      .classAttribute().isNumeric()) ? -1 : 0;
    String className = insts.classIndex() < 0 ? null : insts.classAttribute()
      .name();

    m_originalHeader = CSVToARFFHeaderReduceTask.stripSummaryAtts(insts);
    m_header = new Instances(m_originalHeader, 0);

    m_stats = new ArrayList<NumericStats>();

    for (int i = 0; i < insts.numAttributes(); i++) {
      if (insts.attribute(i).name()
        .startsWith(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX)) {
        String name = insts.attribute(i).name()
          .replace(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX, "")
          .trim();
        if (insts.attribute(name).isNumeric()) {

          if (insts.classIndex() < 0
            || (insts.classIndex() >= 0 && !insts.attribute(i).name()
              .endsWith(className)) || m_keepClassIfSet) {
            m_stats.add(NumericStats.attributeToStats(insts.attribute(i)));
          }
        }
      }
    }

    int numNumeric = 0;
    for (int i = 0; i < m_header.numAttributes(); i++) {
      if (m_header.attribute(i).isNumeric()) {
        numNumeric++;
      }
    }

    if (m_stats.size() != (numNumeric + classAdjust)) {
      throw new Exception(
        "Incorrect number of summary attributes - was expecting "
          + (numNumeric + classAdjust) + " but found " + m_stats.size());
    }
  }

  /**
   * Loads the ARFF header (with summary attributes).
   * 
   * @throws IOException if a problem occurs.
   */
  protected void loadHeaderWithSummaryAtts() throws IOException {
    File f = new File(m_pathToHeaderWithSummaryAtts);

    if (!f.exists()) {
      throw new IOException("The ARFF header file '"
        + m_pathToHeaderWithSummaryAtts
        + "' does not seem to exist on the file system!");
    }

    Instances insts = null;
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(m_pathToHeaderWithSummaryAtts));
      insts = new Instances(br);
      br = null;

      if (getInputFormat() != null) {
        insts.setClassIndex(getInputFormat().classIndex());
      }

      try {
        setupStatsFromInstancesWithSummaryAtts(insts);
      } catch (Exception ex) {
        throw new IOException(ex);
      }
    } finally {
      if (br != null) {
        br.close();
      }
    }
  }

  /**
   * Loads the correlation/covariance matrix from a file.
   * 
   * @throws IOException if a problem occurs
   */
  protected void loadMatrix() throws IOException {
    File f = new File(m_pathToMatrix);

    if (!f.exists()) {
      throw new IOException("The matrix file '" + m_pathToMatrix
        + "' does not seem to exist on the file system!");
    }

    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(m_pathToMatrix));
      try {
        m_matrix = new Matrix(br);
      } catch (Exception e) {
        throw new IOException(e);
      }
      br.close();
      br = null;
    } finally {
      if (br != null) {
        br.close();
      }
    }
  }

  /**
   * Remove useless attributes and adjust matrix accordingly
   * 
   * @param matrix the matrix to adjust
   * @param stats summary statistics for the attributes represented in the
   *          matrix
   * @param useless a set in which to store the indexes of attributes/columns
   *          deemed useless
   * @return the adjusted matrix
   */
  protected static double[][] removeUseless(double[][] matrix,
    List<NumericStats> stats, Set<Integer> useless) {

    for (int i = 0; i < stats.size(); i++) {
      NumericStats s = stats.get(i);
      double count = s.getStats()[ArffSummaryNumericMetric.COUNT.ordinal()];
      double missing = s.getStats()[ArffSummaryNumericMetric.MISSING.ordinal()];
      double stdDev = s.getStats()[ArffSummaryNumericMetric.STDDEV.ordinal()];

      if (count == missing || stdDev == 0.0) {
        useless.add(i);
      }
    }

    if (useless.size() > 0) {
      // have to adjust the matrix - remove rows/cols
      double[][] newMatrix = new double[matrix.length - useless.size()][];

      int ii = 0;
      int jj = 0;
      for (int i = 0; i < matrix.length; i++) {
        jj = 0;
        if (!useless.contains(i)) {
          newMatrix[ii] = new double[matrix.length - useless.size()];
          for (int j = 0; j < matrix.length; j++) {
            if (!useless.contains(j)) {
              newMatrix[ii][jj++] = matrix[i][j];
            }
          }
          ii++;
        }
      }

      return newMatrix;
    }

    return matrix;
  }

  /**
   * Setup and perform the PCA
   * 
   * @param instancesInfo the incoming instances format
   * @throws Exception if a problem occurs
   */
  protected void setup(Instances instancesInfo) throws Exception {
    if (!DistributedJobConfig.isEmpty(m_pathToHeaderWithSummaryAtts)) {
      loadHeaderWithSummaryAtts();
    }

    if (!DistributedJobConfig.isEmpty(m_pathToMatrix)) {
      // load the matrix
      loadMatrix();

      // reset
      m_eigenvalues = null;
      m_outputNumAtts = -1;
      m_sumOfEigenvalues = 0;
      m_eigenvectors = null;
    }

    if (m_eigenvalues != null && getOutputFormat() != null) {
      // already configured
      return;
    }

    super.setInputFormat(instancesInfo);

    if (m_header == null || m_matrix == null) {
      throw new Exception("No matrix and/or header supplied!");
    }

    if (m_header.classIndex() >= 0 && !m_keepClassIfSet) {
      m_hasClass = true;
    }

    StringBuilder rem = new StringBuilder();
    // remove all nominal attributes
    for (int i = 0; i < m_header.numAttributes(); i++) {
      if (m_header.attribute(i).isNominal()
        || (i == m_header.classIndex() && !m_keepClassIfSet)) {
        rem.append("" + (i + 1)).append(",");
      }
    }

    if (rem.length() > 0) {
      Remove remove = new Remove();
      rem.deleteCharAt(rem.length() - 1); // remove the trailing ,
      String attIndices = rem.toString();
      remove.setAttributeIndices(attIndices);
      remove.setInvertSelection(false);

      try {
        remove.setInputFormat(m_header);

        m_header = Filter.useFilter(m_header, remove);
      } catch (Exception ex) {
        throw new Exception(ex);
      }
    }

    // if (m_header.classIndex() >= 0 && !m_keepClassIfSet) {
    // Remove remove = new Remove();
    //
    // remove.setAttributeIndices("" + (m_header.classIndex() + 1));
    // remove.setInvertSelection(false);
    // remove.setInputFormat(m_header);
    // m_header = Filter.useFilter(m_header, remove);
    // m_hasClass = true;
    // }

    // check (and adjust) for useless attributes
    StringBuilder removeCols = new StringBuilder();
    Set<Integer> useless = new HashSet<Integer>();
    double[][] newM = removeUseless(m_matrix.getArray(), m_stats, useless);
    if (useless.size() > 0) {
      m_useless = useless;
      boolean first = true;
      for (Integer i : useless) {
        if (first) {
          removeCols.append("" + (i + 1));
          first = false;
        } else {
          removeCols.append("," + (i + 1));
        }
      }

      m_matrix = new Matrix(newM);

      Remove remove = new Remove();
      remove.setAttributeIndices(removeCols.toString());
      remove.setInvertSelection(false);
      remove.setInputFormat(m_header);
      m_header = Filter.useFilter(m_header, remove);
    }

    // check matrix size against number of attributes
    if (m_matrix.getRowDimension() != m_matrix.getColumnDimension()
      || m_matrix.getRowDimension() != m_header.numAttributes()) {
      throw new Exception("Matrix dimensions do not match number of attributes");
    }

    double[][] v;
    Matrix corr;
    EigenvalueDecomposition eig;
    Matrix mV;

    corr = new Matrix(m_matrix.getArray());
    eig = corr.eig();
    mV = eig.getV();
    v = new double[m_header.numAttributes()][m_header.numAttributes()];
    for (int i = 0; i < v.length; i++) {
      for (int j = 0; j < v[0].length; j++) {
        v[i][j] = mV.get(i, j);
      }
    }
    m_eigenvectors = v.clone();
    m_eigenvalues = eig.getRealEigenvalues().clone();

    // any eigenvalues less than 0 are not worth anything - change to 0
    for (int i = 0; i < m_eigenvalues.length; i++) {
      if (m_eigenvalues[i] < 0) {
        m_eigenvalues[i] = 0;
      }
    }
    m_sortedEigens = Utils.sort(m_eigenvalues);
    m_sumOfEigenvalues = Utils.sum(m_eigenvalues);

    m_transformedFormat = determineOutputFormat();

    setOutputFormat(m_transformedFormat);
  }

  /**
   * Determine the output format for this filter
   * 
   * @return an Instances object that defines the output format
   * @throws Exception if a problem occurs
   */
  protected Instances determineOutputFormat() throws Exception {
    double cumulative;
    ArrayList<Attribute> attributes;
    int i;
    int j;
    StringBuffer attName;
    double[] coefMags;
    int numAttrs;
    int[] coeffInds;
    double coeffValue;
    int numAttsLowerBound;

    if (m_eigenvalues == null) {
      return m_header;
    }

    if (m_maxAttributes > 0) {
      numAttsLowerBound = m_header.numAttributes() - m_maxAttributes;
    } else {
      numAttsLowerBound = 0;
    }

    if (numAttsLowerBound < 0) {
      numAttsLowerBound = 0;
    }

    cumulative = 0.0;
    attributes = new ArrayList<Attribute>();
    for (i = m_header.numAttributes() - 1; i >= numAttsLowerBound; i--) {
      attName = new StringBuffer();
      // build array of coefficients
      coefMags = new double[m_header.numAttributes()];
      for (j = 0; j < m_header.numAttributes(); j++) {
        coefMags[j] = -Math.abs(m_eigenvectors[j][m_sortedEigens[i]]);
      }
      numAttrs = (m_maxAttrsInName > 0) ? Math.min(m_header.numAttributes(),
        m_maxAttrsInName) : m_header.numAttributes();

      // this array contains the sorted indices of the coefficients
      if (m_header.numAttributes() > 0) {
        // if m_maxAttrsInName > 0, sort coefficients by decreasing magnitude
        coeffInds = Utils.sort(coefMags);
      } else {
        // if m_maxAttrsInName <= 0, use all coeffs in original order
        coeffInds = new int[m_header.numAttributes()];
        for (j = 0; j < m_header.numAttributes(); j++) {
          coeffInds[j] = j;
        }
      }
      // build final attName string
      for (j = 0; j < numAttrs; j++) {
        coeffValue = m_eigenvectors[coeffInds[j]][m_sortedEigens[i]];
        if (j > 0 && coeffValue >= 0) {
          attName.append("+");
        }

        attName.append(Utils.doubleToString(coeffValue, 5, 3)
          + m_header.attribute(coeffInds[j]).name());
      }
      if (numAttrs < m_header.numAttributes()) {
        attName.append("...");
      }

      attributes.add(new Attribute(attName.toString()));
      cumulative += m_eigenvalues[m_sortedEigens[i]];

      if ((cumulative / m_sumOfEigenvalues) >= m_coverVariance) {
        break;
      }
    }

    // add in any non-numeric (untransformed) attributes
    for (i = 0; i < m_originalHeader.numAttributes(); i++) {
      if (i != m_originalHeader.classIndex()
        && !m_originalHeader.attribute(i).isNumeric()) {
        if (m_originalHeader.attribute(i).name()
          .startsWith(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX)) {
          break; // don't process the summary atts!
        }
        attributes.add((Attribute) m_originalHeader.attribute(i).copy());
      }
    }

    if (m_hasClass) {
      attributes.add((Attribute) m_originalHeader.classAttribute().copy());
    }

    Instances outputFormat = new Instances(m_originalHeader.relationName()
      + "_principal components", attributes, 0);

    if (m_hasClass) {
      outputFormat.setClassIndex(outputFormat.numAttributes() - 1);
    }

    m_outputNumAtts = outputFormat.numAttributes();

    return outputFormat;
  }

  @Override
  public boolean input(Instance instance) throws Exception {
    // check input format set
    if (getInputFormat() == null) {
      throw new Exception("No input instance format defined");
    }

    // check that PCA has been built successfully
    if (m_eigenvalues == null) {
      throw new Exception("Principal components has not been performed yet!");
    }

    // transform
    Instance inst = convertInstance(instance);
    push(inst);

    return true;
  }

  /**
   * Convert an input instance into the transformed (PCA) space.
   * 
   * @param instance the input instance
   * @return the transformed instance
   * @throws Exception if a problem occurs
   */
  protected Instance convertInstance(Instance instance) throws Exception {
    Instance result;
    double[] newVals;
    double cumulative;
    int i;
    int j;
    double tempval;
    int numAttsLowerBound;

    newVals = new double[m_outputNumAtts];
    double[] vals = instance.toDoubleArray();
    int valsSize = m_stats.size() - (m_useless != null ? m_useless.size() : 0);
    double[] vals2 = new double[valsSize];

    // replace missing with mean
    int count = 0;
    int statCount = 0;
    for (i = 0; i < vals.length; i++) {
      if (m_hasClass && i == instance.classIndex()) {
        continue;
      }

      if (!instance.attribute(i).isNumeric()) {
        continue;
      }

      NumericStats s = m_stats.get(statCount++);
      double mean = s.getStats()[ArffSummaryNumericMetric.MEAN.ordinal()];
      double stddev = s.getStats()[ArffSummaryNumericMetric.STDDEV.ordinal()];
      double countS = s.getStats()[ArffSummaryNumericMetric.COUNT.ordinal()];
      double missing = s.getStats()[ArffSummaryNumericMetric.MISSING.ordinal()];

      if (countS == missing || stddev == 0.0) {
        // this one would have been removed as a "useless"
        // attribute
        continue;
      }
      if (Utils.isMissingValue(vals[i])) {
        vals2[count] = mean;
      } else {
        vals2[count] = vals[i];
      }

      if (!m_matrixIsCovariance) {
        // standardize
        stddev = s.getStats()[ArffSummaryNumericMetric.STDDEV.ordinal()];

        // just subtract the mean if the std deviation is zero
        if (stddev > 0) {
          vals2[count] = (vals2[count] - mean) / stddev;
        } else {
          vals2[count] -= mean;
        }

        if (Double.isNaN(vals2[count])) {
          throw new Exception("A NaN value was generated "
            + "while standardizing attribute " + instance.attribute(i).name());
        }

      } else {
        // center the data
        vals2[count] -= mean;
      }
      count++;
    }

    if (m_hasClass) {
      newVals[m_outputNumAtts - 1] = instance.value(instance.classIndex());
    }

    if (m_maxAttributes > 0) {
      numAttsLowerBound = m_header.numAttributes() - m_maxAttributes;
    } else {
      numAttsLowerBound = 0;
    }

    if (numAttsLowerBound < 0) {
      numAttsLowerBound = 0;
    }

    cumulative = 0;
    count = 0;
    for (i = m_header.numAttributes() - 1; i >= numAttsLowerBound; i--) {
      tempval = 0.0;
      for (j = 0; j < m_header.numAttributes(); j++) {
        tempval += m_eigenvectors[j][m_sortedEigens[i]] * vals2[j];
      }

      newVals[m_header.numAttributes() - i - 1] = tempval;
      cumulative += m_eigenvalues[m_sortedEigens[i]];
      count++;
      if ((cumulative / m_sumOfEigenvalues) >= m_coverVariance) {
        break;
      }
    }

    // add in any untransformed attributes
    for (i = 0; i < instance.numAttributes(); i++) {
      if (!instance.attribute(i).isNumeric() && i != instance.classIndex()) {
        newVals[count++] = instance.value(i);
      }
    }

    // create instance
    if (instance instanceof SparseInstance) {
      result = new SparseInstance(instance.weight(), newVals);
    } else {
      result = new DenseInstance(instance.weight(), newVals);
    }

    return result;
  }

  /**
   * Return a matrix as a String
   * 
   * @param matrix that is decribed as a string
   * @return a String describing a matrix
   */
  private static String matrixToString(double[][] matrix) {
    StringBuffer result = new StringBuffer();
    int last = matrix.length - 1;

    for (int i = 0; i <= last; i++) {
      for (int j = 0; j <= last; j++) {
        result.append(Utils.doubleToString(matrix[i][j], 6, 2) + " ");
        if (j == last) {
          result.append('\n');
        }
      }
    }
    return result.toString();
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();

    if (m_useless != null && m_useless.size() > 0) {
      result
        .append("Attributes removed from the analysis (all missing values or "
          + "constant values):\n\n");

      for (Integer i : m_useless) {
        result.append(
          m_stats.get(i).getName()
            .replace(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX, ""))
          .append(" ");
      }
      result.append("\n\n");
    }

    double cumulative = 0.0;
    Instances output = m_transformedFormat;
    int numNonNumeric = 0;
    for (int i = 0; i < output.numAttributes(); i++) {
      if (!output.attribute(i).isNumeric()) {
        numNonNumeric++;
      }
    }

    int classAdjust = 0;
    if (output.classIndex() >= 0 && output.classAttribute().isNumeric()) {
      classAdjust = 1;
    }
    int numVectors = (output.classIndex() < 0) ? output.numAttributes()
      - numNonNumeric : output.numAttributes() - numNonNumeric - classAdjust;

    String corrCov = (m_matrixIsCovariance) ? "Covariance " : "Correlation ";
    result.append(corrCov + "matrix\n" + matrixToString(m_matrix.getArray())
      + "\n\n");
    result.append("eigenvalue\tproportion\tcumulative\n");
    for (int i = m_header.numAttributes() - 1; i > (m_header.numAttributes()
      - numVectors - 1); i--) {
      cumulative += m_eigenvalues[m_sortedEigens[i]];
      result.append(Utils
        .doubleToString(m_eigenvalues[m_sortedEigens[i]], 9, 5)
        + "\t"
        + Utils.doubleToString(m_eigenvalues[m_sortedEigens[i]]
          / m_sumOfEigenvalues, 9, 5)
        + "\t"
        + Utils.doubleToString(cumulative / m_sumOfEigenvalues, 9, 5)
        + "\t"
        + output.attribute(m_header.numAttributes() - i - 1).name() + "\n");
    }

    result.append("\nEigenvectors\n");
    for (int j = 1; j <= numVectors; j++) {
      result.append(" V" + j + '\t');
    }
    result.append("\n");
    for (int j = 0; j < m_header.numAttributes(); j++) {

      for (int i = m_header.numAttributes() - 1; i > (m_header.numAttributes()
        - numVectors - 1); i--) {
        result.append(Utils.doubleToString(
          m_eigenvectors[j][m_sortedEigens[i]], 7, 4) + "\t");
      }
      result.append(m_header.attribute(j).name() + '\n');
    }

    return result.toString();
  }

  /**
   * Method for testing this class
   * 
   * @param args arguments to the filter
   */
  public static void test(String[] args) {
    try {
      Instances orig = new Instances(new java.io.BufferedReader(
        new java.io.FileReader(args[0])));
      orig.setClassIndex(orig.numAttributes() - 1);

      java.util.List<String> attNames = new java.util.ArrayList<String>();
      for (int i = 0; i < orig.numAttributes(); i++) {
        attNames.add(orig.attribute(i).name());
      }

      weka.distributed.CSVToARFFHeaderMapTask arffTask =
        new weka.distributed.CSVToARFFHeaderMapTask();
      arffTask.setOptions(args);
      // arffTask.setComputeSummaryStats(true);
      for (int i = 0; i < orig.numInstances(); i++) {
        arffTask.processRow(orig.instance(i).toString(), attNames);
      }
      Instances withSummary = arffTask.getHeader();
      weka.distributed.CSVToARFFHeaderReduceTask arffReduce =
        new weka.distributed.CSVToARFFHeaderReduceTask();
      List<Instances> instList = new ArrayList<Instances>();
      instList.add(withSummary);
      withSummary = arffReduce.aggregate(instList);

      System.err.println(withSummary);
      withSummary.setClassIndex(orig.classIndex());

      weka.distributed.CorrelationMatrixMapTask corrTask =
        new weka.distributed.CorrelationMatrixMapTask();
      corrTask.setup(withSummary);

      for (int i = 0; i < orig.numInstances(); i++) {
        corrTask.processInstance(orig.instance(i));
      }

      double[][] matrix = corrTask.getMatrix();
      CorrelationMatrixRowReduceTask reduce =
        new CorrelationMatrixRowReduceTask();
      for (int i = 0; i < matrix.length; i++) {
        List<double[]> toAgg = new ArrayList<double[]>();
        toAgg.add(matrix[i]);
        double[] computed = reduce.aggregate(i, toAgg, null, withSummary, true,
          false, true);
        matrix[i] = computed;
      }

      double[][] finalMatrix = new double[matrix.length][matrix.length];
      for (int i = 0; i < matrix.length; i++) {
        for (int j = 0; j < matrix[i].length; j++) {
          finalMatrix[i][j] = matrix[i][j];
          finalMatrix[j][i] = matrix[i][j];
        }
      }

      // temporary
      Matrix m = new Matrix(finalMatrix);
      m.write(new java.io.FileWriter("test.matrix"));
      java.io.FileWriter fr = new java.io.FileWriter("test.arff");
      fr.write(withSummary.toString());
      fr.close();

      PreConstructedPCA pca = new PreConstructedPCA(withSummary, m, false,
        false);

      pca.setInputFormat(CSVToARFFHeaderReduceTask
        .stripSummaryAtts(withSummary));

      System.err.println(pca.toString());

      System.err.println(pca.getOutputFormat());

      Instances inputHeaderNoSummary = CSVToARFFHeaderReduceTask
        .stripSummaryAtts(withSummary);

      for (int i = 0; i < orig.numInstances(); i++) {
        double[] mappedVals = new double[orig.instance(i).numAttributes()];
        for (int j = 0; j < mappedVals.length; j++) {
          if (orig.instance(i).isMissing(j)) {
            mappedVals[j] = Utils.missingValue();
          } else if (orig.instance(i).attribute(j).isNumeric()) {
            mappedVals[j] = orig.instance(i).value(j);
          } else {
            String val = orig.instance(i).stringValue(j);
            double newIndex = inputHeaderNoSummary.attribute(j).indexOfValue(
              val);
            mappedVals[j] = newIndex;
          }
        }

        Instance mapped = new DenseInstance(1.0, mappedVals);
        mapped.setDataset(inputHeaderNoSummary);

        // pca.input(orig.instance(i));
        pca.input(mapped);
        System.err.println(pca.output());
      }

      pca.setInputFormat(CSVToARFFHeaderReduceTask
        .stripSummaryAtts(withSummary));

      System.err.println(pca.toString());

      System.err.println(pca.getOutputFormat());

    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Main method for command line execution
   * 
   * @param args options to the filter
   */
  public static void main(String[] args) {
    test(args);
    // try {
    // PreConstructedPCA pca = new PreConstructedPCA();
    // pca.run(pca, args);
    // } catch (IllegalArgumentException ex) {
    // ex.printStackTrace();
    // }
    // // runFilter(new PreConstructedPCA(), args);
    // test(args);
  }

  @Override
  public void run(Object toRun, String[] options) {

    if (!(toRun instanceof PreConstructedPCA)) {
      throw new IllegalArgumentException("Object is not a PreConstructedPCA!");
    }

    runFilter((PreConstructedPCA) toRun, options);
  }

  @Override
  public boolean isConstructed() {
    return getInputFormat() != null && m_eigenvalues != null;
  }

  @Override
  public void resetPreconstructed() {
    m_matrix = null;
    m_eigenvectors = null;
    m_sortedEigens = null;
  }
}
