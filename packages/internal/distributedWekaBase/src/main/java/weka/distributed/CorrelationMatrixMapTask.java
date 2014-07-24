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
 *    CorrelationMatrixMapTask.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.core.stats.ArffSummaryNumericMetric;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

/**
 * A map task that computes partial covariance sums for a covariance/correlation
 * matrix from the data it gets via its processInstance() method. Expects to be
 * initialized with a training header that includes summary meta attributes. Can
 * replace missing values with means or ommit them from the updates.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class CorrelationMatrixMapTask implements Serializable, OptionHandler {

  /** For serialization */
  private static final long serialVersionUID = 3437000574208204515L;

  /** Final result is covariance rather than correlation? */
  protected boolean m_covariance;

  /** Whether to replace any missing values with the mean or just ignore */
  protected boolean m_replaceMissingWithMean = true;

  /** Holds the version of the header that contains the summary meta attributes */
  protected Instances m_headerWithSummary;

  /** The header without the summary meta attributes */
  protected Instances m_header;

  /**
   * Whether to delete the class attribute if set in the data (so that if it is
   * numeric it doesn't become part of the correlation matrix)
   */
  protected boolean m_deleteClassIfSet = true;

  /** Remove filter for removing attributes */
  protected Remove m_remove;

  /** Holds the partial covariance sums matrix */
  protected double[][] m_corrMatrix;

  /**
   * Co-occurrence counts when ignoring missings rather than replacing with
   * means
   */
  protected int[][] m_coOccurrenceCounts;

  /** Holds the mean for each numeric attribute */
  protected double[] m_means;

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> opts = new Vector<Option>();

    opts.add(new Option(
      "\tIgnore missing values (rather than replace with mean).",
      "ignore-missing", 0, "-ignore-missing"));
    opts.add(new Option("\tKeep class attribute (if set).", "keep-class", 0,
      "-keep-class"));
    opts.add(new Option(
      "\tFinal result is covariance rather than correlation.", "covariance", 0,
      "-covariance"));

    return opts.elements();
  }

  @Override
  public void setOptions(String[] options) throws Exception {
    setIgnoreMissingValues(Utils.getFlag("ignore-missing", options));
    setKeepClassAttributeIfSet(Utils.getFlag("keep-class", options));
    setCovariance(Utils.getFlag("covariance", options));
  }

  @Override
  public String[] getOptions() {
    List<String> options = new ArrayList<String>();
    if (getIgnoreMissingValues()) {
      options.add("-ignore-missing");
    }

    if (getKeepClassAttributeIfSet()) {
      options.add("-keep-class");
    }

    if (getCovariance()) {
      options.add("-covariance");
    }

    return options.toArray(new String[options.size()]);
  }

  /**
   * Tool tip text for this property.
   * 
   * @return a string describing this property
   */
  public String ignoreMissingValuesTipText() {
    return "Ignore missing values rather than replace with the mean";
  }

  /**
   * Set whether to ignore missing values
   * 
   * @param ignore true if missing values are to be ignored
   */
  public void setIgnoreMissingValues(boolean ignore) {
    m_replaceMissingWithMean = !ignore;
  }

  /**
   * Get whether to ignore missing values
   * 
   * @return true if missing values are to be ignored
   */
  public boolean getIgnoreMissingValues() {
    return !m_replaceMissingWithMean;
  }

  /**
   * Tool tip text for this property.
   * 
   * @return a string describing this property
   */
  public String keepClassAttributeIfSetTipText() {
    return "If the class is set, and it is numeric, keep it as part "
      + "of the correlation analysis.";
  }

  /**
   * Set whether to keep the class attribute as part of the correlation analysis
   * 
   * @param keep true if the class attribute is to be kept
   */
  public void setKeepClassAttributeIfSet(boolean keep) {
    m_deleteClassIfSet = !keep;
  }

  /**
   * Get whether to keep the class attribute as part of the correlation analysis
   * 
   * @return true if the class attribute is to be kept
   */
  public boolean getKeepClassAttributeIfSet() {
    return !m_deleteClassIfSet;
  }

  /**
   * Tool tip text for this property.
   * 
   * @return a string describing this property
   */
  public String covarianceTipText() {
    return "Compute a covariance matrix rather than correlation";
  }

  /**
   * Set whether to compute a covariance matrix rather than a correlation one
   * 
   * @param cov true if a covariance matrix is to be computed rather than
   *          correlation
   */
  public void setCovariance(boolean cov) {
    m_covariance = cov;
  }

  /**
   * Get whether to compute a covariance matrix rather than a correlation one
   * 
   * @return true if a covariance matrix is to be computed rather than
   *         correlation
   */
  public boolean getCovariance() {
    return m_covariance;
  }

  /**
   * Returns the matrix
   * 
   * @return the matrix
   */
  public double[][] getMatrix() {
    return m_corrMatrix;
  }

  /**
   * The co-occurrence counts (will be null if missings are replaced by means)
   * 
   * @return the co-occurrence counts, or null if missings are being replaced by
   *         means
   */
  public int[][] getCoOccurrenceCounts() {
    return m_coOccurrenceCounts;
  }

  /**
   * Computes the partial covariance for two attributes on the current instance
   * 
   * @param inst the instance to update from
   * @param i attribute i
   * @param j attribute j
   * @return the covariance update
   */
  protected double cov(Instance inst, int i, int j) {
    double iV = inst.value(i);
    double jV = inst.value(j);

    if (Utils.isMissingValue(iV)) {
      if (m_replaceMissingWithMean) {
        iV = m_means[i];
      } else {
        return 0; // skip missing
      }
    }

    if (Utils.isMissingValue(jV)) {
      if (m_replaceMissingWithMean) {
        jV = m_means[j];
      } else {
        return 0; // skip missing
      }
    }

    iV -= m_means[i];
    jV -= m_means[j];

    if (!m_replaceMissingWithMean) {
      m_coOccurrenceCounts[i][j]++;
    }

    return iV * jV;
  }

  /**
   * Process an instance
   * 
   * @param inst the instance to process
   * @throws Exception if a problem occurs
   */
  public void processInstance(Instance inst) throws Exception {
    if (m_remove != null) {
      m_remove.input(inst);
      inst = m_remove.output();
    }

    for (int i = 0; i < inst.numAttributes(); i++) {
      for (int j = 0; j < (i + 1); j++) {
        m_corrMatrix[i][j] += cov(inst, i, j);
      }
    }
  }

  /**
   * Initialize the map task
   * 
   * @param trainingHeader the training header for the data (including summary
   *          meta attributes)
   * @throws DistributedWekaException if a problem occurs
   */
  public void setup(Instances trainingHeader) throws DistributedWekaException {
    m_headerWithSummary = new Instances(trainingHeader, 0);
    trainingHeader = CSVToARFFHeaderReduceTask.stripSummaryAtts(trainingHeader);

    m_remove = null;

    StringBuilder rem = new StringBuilder();
    if (trainingHeader.classIndex() >= 0 && m_deleteClassIfSet) {
      rem.append("" + (trainingHeader.classIndex() + 1)).append(",");
    }

    // remove all nominal attributes
    for (int i = 0; i < trainingHeader.numAttributes(); i++) {
      if (!trainingHeader.attribute(i).isNumeric()) {
        rem.append("" + (i + 1)).append(",");
      }
    }
    if (rem.length() > 0) {
      m_remove = new Remove();
      rem.deleteCharAt(rem.length() - 1); // remove the trailing ,
      String attIndices = rem.toString();
      m_remove.setAttributeIndices(attIndices);
      m_remove.setInvertSelection(false);

      try {
        m_remove.setInputFormat(trainingHeader);

        m_header = Filter.useFilter(trainingHeader, m_remove);
      } catch (Exception ex) {
        throw new DistributedWekaException(ex);
      }

      if (m_header.numAttributes() == 1) {
        throw new DistributedWekaException(
          "Only one numeric attribute in the input data!");
      }
      if (m_header.numAttributes() == 0) {
        throw new DistributedWekaException(
          "No numeric attributes in the input data!");
      }
    } else {
      m_header = trainingHeader;
    }

    // if (trainingHeader.classIndex() >= 0 && m_deleteClassIfSet) {
    // m_remove = new Remove();
    // m_remove.setAttributeIndices("" + (trainingHeader.classIndex() + 1));
    // m_remove.setInvertSelection(false);
    // try {
    // m_remove.setInputFormat(trainingHeader);
    //
    // m_header = Filter.useFilter(trainingHeader, m_remove);
    // } catch (Exception ex) {
    // throw new DistributedWekaException(ex);
    // }
    // } else {
    // m_header = trainingHeader;
    // }

    m_means = new double[m_header.numAttributes()];

    for (int i = 0; i < m_header.numAttributes(); i++) {
      String origName = m_header.attribute(i).name();
      Attribute summary = m_headerWithSummary
        .attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
          + origName);

      if (summary == null) {
        throw new DistributedWekaException(
          "Can't find summary stats attribute for attribute " + origName);
      }

      double[] stats = CSVToARFFHeaderReduceTask.attributeToStatsArray(summary);

      m_means[i] = stats[ArffSummaryNumericMetric.MEAN.ordinal()];
    }

    m_corrMatrix = new double[m_header.numAttributes()][];
    if (!m_replaceMissingWithMean) {
      m_coOccurrenceCounts = new int[m_header.numAttributes()][];
    }

    for (int i = 0; i < m_header.numAttributes(); i++) {
      m_corrMatrix[i] = new double[i + 1];

      if (!m_replaceMissingWithMean) {
        m_coOccurrenceCounts[i] = new int[i + 1];
      }
    }

    for (int i = 0; i < m_header.numAttributes(); i++) {
      m_corrMatrix[i][i] = 1.0;
    }
  }

  /**
   * Main method for testing this class
   * 
   * @param args command line arguments
   */
  public static void main(String[] args) {
    try {
      Instances orig = new Instances(new java.io.BufferedReader(
        new java.io.FileReader(args[0])));
      orig.setClassIndex(orig.numAttributes() - 1);

      java.util.List<String> attNames = new java.util.ArrayList<String>();
      for (int i = 0; i < orig.numAttributes(); i++) {
        attNames.add(orig.attribute(i).name());
      }

      CSVToARFFHeaderMapTask arffTask = new CSVToARFFHeaderMapTask();
      arffTask.setOptions(args);
      // arffTask.setComputeSummaryStats(true);
      for (int i = 0; i < orig.numInstances(); i++) {
        arffTask.processRow(orig.instance(i).toString(), attNames);
      }
      Instances withSummary = arffTask.getHeader();
      CSVToARFFHeaderReduceTask arffReduce = new CSVToARFFHeaderReduceTask();
      List<Instances> instList = new ArrayList<Instances>();
      instList.add(withSummary);
      withSummary = arffReduce.aggregate(instList);

      System.err.println(withSummary);
      withSummary.setClassIndex(orig.classIndex());

      CorrelationMatrixMapTask corrTask = new CorrelationMatrixMapTask();
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
        for (int j = 0; j < matrix[i].length; j++) {
          System.err.print(computed[j] + " ");
        }
        System.err.println();
      }
      System.err.println();

      for (int i = 0; i < orig.numAttributes(); i++) {
        double[] row = new double[i + 1];
        for (int j = 0; j <= i; j++) {
          if (i != orig.classIndex() && j != orig.classIndex()) {
            if (i == j) {
              row[j] = 1.0;
            } else {
              double[] ii = new double[orig.numInstances()];
              double[] jj = new double[orig.numInstances()];
              for (int k = 0; k < orig.numInstances(); k++) {
                ii[k] = orig.instance(k).value(i);
                jj[k] = orig.instance(k).value(j);
              }

              row[j] = Utils.correlation(ii, jj, orig.numInstances());
            }
            System.err.print(row[j] + " ");
          }
        }
        System.err.println();
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
