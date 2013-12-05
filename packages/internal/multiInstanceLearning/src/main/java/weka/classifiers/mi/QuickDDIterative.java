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
 * QuickDDIterative.java
 * Copyright (C) 2008-10 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.mi;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import weka.classifiers.AbstractClassifier;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.MultiInstanceCapabilitiesHandler;
import weka.core.Optimization;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import weka.filters.unsupervised.attribute.Standardize;

/**
 * <!-- globalinfo-start --> Modified, faster, iterative version of the basic
 * diverse density algorithm. Uses only instances from positive bags as
 * candidate diverse density maxima. Picks best instance based on current
 * scaling vector, then optimizes scaling vector. Once vector has been found,
 * picks new best point based on new scaling vector (if the number of desired
 * iterations is greater than one). Performs one iteration by default (Scaling
 * Once). For good results, try boosting it with RealAdaBoost, setting the
 * maximum probability of the negative class to 0.5 and enabling consideration
 * of both classes as the positive class. Note that standardization of
 * attributes is default, but normalization can work better.<br/>
 * <br/>
 * James R. Foulds, Eibe Frank: Speeding up and boosting diverse density
 * learning. In: Proc 13th International Conference on Discovery Science,
 * 102-116, 2010.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- technical-bibtex-start --> BibTeX:
 * 
 * <pre>
 * &#64;inproceedings{Foulds2010,
 *    author = {James R. Foulds and Eibe Frank},
 *    booktitle = {Proc 13th International Conference on Discovery Science},
 *    pages = {102-116},
 *    publisher = {Springer},
 *    title = {Speeding up and boosting diverse density learning},
 *    year = {2010}
 * }
 * </pre>
 * <p/>
 * <!-- technical-bibtex-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -D
 *  Turn on debugging output.
 * </pre>
 * 
 * <pre>
 * -N &lt;num&gt;
 *  Whether to 0=normalize/1=standardize/2=neither.
 *  (default 1=standardize)
 * </pre>
 * 
 * <pre>
 * -S &lt;num&gt;
 *  The initial scaling factor (constant for all attributes).
 * </pre>
 * 
 * <pre>
 * -M &lt;num&gt;
 *  Maximum probability of negative class (default 1).
 * </pre>
 * 
 * <pre>
 * -I &lt;num&gt;
 *  The maximum number of iterations to perform (default 1).
 * </pre>
 * 
 * <pre>
 * -C
 *  Consider both classes as positive classes. (default: only last class).
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author James Foulds
 * @author Xin Xu
 * @author Eibe Frank
 * @version $Revision$
 */
public class QuickDDIterative extends AbstractClassifier implements
  OptionHandler, MultiInstanceCapabilitiesHandler, TechnicalInformationHandler,
  WeightedInstancesHandler {

  /** for serialization */
  static final long serialVersionUID = 4263507733600536170L;

  /** The index of the class attribute */
  protected int m_ClassIndex;

  /**
   * The target point and scaling vector learned by the algorithm. (comment by
   * Jimmy)
   **/
  protected double[] m_Par;

  /** The current guess at the target point, without scaling information. -Jimmy **/
  protected double[] m_CurrentCandidate;

  /** The number of the class labels */
  protected int m_NumClasses;

  /** The weights for each bag */
  protected double[] m_BagWeights;

  /** Class labels for each bag */
  protected int[] m_Classes;

  /** MI data */
  protected double[][][] m_Data;

  /** All attribute names */
  protected Instances m_Attributes;

  /** The filter used to standardize/normalize all values. */
  protected Filter m_Filter = null;

  /** Whether to normalize/standardize/neither, default:standardize */
  protected int m_filterType = FILTER_STANDARDIZE;

  /** Initial scaling factor for Gaussian-like function at target point. */
  protected double m_scaleFactor = 1.0;

  /** The maximum number of iterations to perform */
  protected int m_maxIterations = 1;

  /** The maximum probability for the negative class */
  protected double m_maxProbNegativeClass = 1.0;

  /** Whether to consider both classes as "positive" class in turn */
  protected boolean m_considerBothClasses = false;

  /** The index of the positive class */
  protected byte m_posClass = 1;

  /** Normalize training data */
  public static final int FILTER_NORMALIZE = 0;
  /** Standardize training data */
  public static final int FILTER_STANDARDIZE = 1;
  /** No normalization/standardization */
  public static final int FILTER_NONE = 2;
  /** The filter to apply to the training data */
  public static final Tag[] TAGS_FILTER = {
    new Tag(FILTER_NORMALIZE, "Normalize training data"),
    new Tag(FILTER_STANDARDIZE, "Standardize training data"),
    new Tag(FILTER_NONE, "No normalization/standardization"), };

  /** Compute machine precision */
  protected static double m_Epsilon, m_Zero;
  static {
    m_Epsilon = 1.0;
    while (1.0 + m_Epsilon > 1.0) {
      m_Epsilon /= 2.0;
    }
    m_Epsilon *= 2.0;
    m_Zero = Math.sqrt(m_Epsilon);
  }

  /** The filter used to get rid of missing values. */
  protected ReplaceMissingValues m_Missing = new ReplaceMissingValues();

  /**
   * Returns a string describing this filter
   * 
   * @return a description of the filter suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "Modified, faster, iterative version of the basic diverse density algorithm. Uses only "
      + "instances from positive bags as candidate diverse density maxima. Picks "
      + "best instance based on current scaling vector, then optimizes scaling vector. "
      + "Once vector has been found, picks new best point based on new scaling vector (if the "
      + "number of desired iterations is greater than one). Performs "
      + "one iteration by default (Scaling Once). For good results, try "
      + "boosting it with RealAdaBoost, setting the maximum probability of the negative "
      + "class to 0.5 and enabling consideration of both classes as the positive class. Note "
      + "that standardization of attributes is default, but normalization can work better.\n\n"
      + getTechnicalInformation().toString();
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing detailed
   * information about the technical background of this class, e.g., paper
   * reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  @Override
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation result;

    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "James R. Foulds and Eibe Frank");
    result.setValue(Field.TITLE,
      "Speeding up and boosting diverse density learning");
    result.setValue(Field.BOOKTITLE,
      "Proc 13th International Conference on Discovery Science");
    result.setValue(Field.YEAR, "2010");
    result.setValue(Field.PAGES, "102-116");
    result.setValue(Field.PUBLISHER, "Springer");

    return result;
  }

  /**
   * Returns an enumeration describing the available options
   * 
   * @return an enumeration of all the available options
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> result = new Vector<Option>();

    result.addElement(new Option(
      "\tWhether to 0=normalize/1=standardize/2=neither.\n"
        + "\t(default 1=standardize)", "N", 1, "-N <num>"));

    result.addElement(new Option(
      "\tThe initial scaling factor (constant for all attributes).", "S", 1,
      "-S <num>"));

    result.addElement(new Option(
      "\tMaximum probability of negative class (default 1).", "M", 1,
      "-M <num>"));

    result.addElement(new Option(
      "\tThe maximum number of iterations to perform (default 1).", "I", 1,
      "-I <num>"));

    result
      .addElement(new Option(
        "\tConsider both classes as positive classes. (default: only last class).",
        "C", 0, "-C"));

    result.addAll(Collections.list(super.listOptions()));

    return result.elements();
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   * <!-- options-start --> Valid options are:
   * <p/>
   * 
   * <pre>
   * -N &lt;num&gt;
   *  Whether to 0=normalize/1=standardize/2=neither.
   *  (default 1=standardize)
   * </pre>
   * 
   * <pre>
   * -S &lt;num&gt;
   *  The initial scaling factor (constant for all attributes).
   * </pre>
   * 
   * <pre>
   * -M &lt;num&gt;
   *  Maximum probability of negative class (default 1).
   * </pre>
   * 
   * <pre>
   * -I &lt;num&gt;
   *  The maximum number of iterations to perform (default 1).
   * </pre>
   * 
   * <pre>
   * -C
   *  Consider both classes as positive classes. (default: only last class).
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    String nString = Utils.getOption('N', options);
    if (nString.length() != 0) {
      setFilterType(new SelectedTag(Integer.parseInt(nString), TAGS_FILTER));
    } else {
      setFilterType(new SelectedTag(FILTER_STANDARDIZE, TAGS_FILTER));
    }
    String sString = Utils.getOption('S', options);
    if (sString.length() != 0) {
      setScalingFactor(Double.parseDouble(sString));
    } else {
      setScalingFactor(1.0);
    }
    String rString = Utils.getOption('M', options);
    if (rString.length() != 0) {
      setMaxProbNegativeClass(Double.parseDouble(rString));
    } else {
      setMaxProbNegativeClass(1);
    }
    String iString = Utils.getOption('I', options);
    if (iString.length() != 0) {
      setMaxIterations(Integer.parseInt(iString));
    } else {
      setMaxIterations(1);
    }

    setConsiderBothClasses(Utils.getFlag('C', options));

    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the classifier.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> result = new Vector<String>();

    result.add("-N");
    result.add("" + m_filterType);
    result.add("-S");
    result.add("" + m_scaleFactor);
    result.add("-M");
    result.add("" + m_maxProbNegativeClass);
    result.add("-I");
    result.add("" + m_maxIterations);
    if (getConsiderBothClasses()) {
      result.add("-C");
    }

    Collections.addAll(result, super.getOptions());

    return result.toArray(new String[result.size()]);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String filterTypeTipText() {
    return "The filter type for transforming the training data.";
  }

  /**
   * Gets how the training data will be transformed. Will be one of
   * FILTER_NORMALIZE, FILTER_STANDARDIZE, FILTER_NONE.
   * 
   * @return the filtering mode
   */
  public SelectedTag getFilterType() {
    return new SelectedTag(m_filterType, TAGS_FILTER);
  }

  /**
   * Sets how the training data will be transformed. Should be one of
   * FILTER_NORMALIZE, FILTER_STANDARDIZE, FILTER_NONE.
   * 
   * @param newType the new filtering mode
   */
  public void setFilterType(SelectedTag newType) {

    if (newType.getTags() == TAGS_FILTER) {
      m_filterType = newType.getSelectedTag().getID();
    }
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String scalingFactorTipText() {
    return "The initial value of the scaling factor for all attributes.";
  }

  /**
   * Set the scaling factor for the Gaussian-like function at the target point.
   * Squared distances between the target point and other points will be divided
   * by the square of this value.
   * 
   * @param scale
   */
  public void setScalingFactor(double scale) {
    m_scaleFactor = scale;
  }

  /**
   * Get the scaling factor for the Gaussian-like function at the target point.
   * 
   * @return the scaling factor.
   */
  public double getScalingFactor() {
    return m_scaleFactor;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String maxProbNegativeClassTipText() {
    return "The maximum probability for the negative class.";
  }

  /**
   * Set the maximum probability for the negative class.
   */
  public void setMaxProbNegativeClass(double r) {
    m_maxProbNegativeClass = r;
  }

  /**
   * Get the maximum probability for the negative class.
   */
  public double getMaxProbNegativeClass() {
    return m_maxProbNegativeClass;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String considerBothClassesTipText() {
    return "Whether to run the algorithm once for each class.";
  }

  /**
   * Set wether to consider both classes as "positive" class in turn.
   */
  public void setConsiderBothClasses(boolean b) {
    m_considerBothClasses = b;
  }

  /**
   * Get wether to consider both classes as "positive" class in turn.
   */
  public boolean getConsiderBothClasses() {
    return m_considerBothClasses;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String maxIterationsTipText() {
    return "The maximum number of iterations to perform.";
  }

  /**
   * @param m_maxIterations the m_maxIterations to set
   */
  public void setMaxIterations(int maxIterations) {
    this.m_maxIterations = maxIterations;
  }

  /**
   * @return the m_maxIterations
   */
  public int getMaxIterations() {
    return m_maxIterations;
  }

  /**
   * Computes the negative log likelihood (i.e. negative log diverse density) at
   * point m_CurrentCandidate, using scaling vector x
   * 
   * @param x the scaling vector to use
   * @pre m_CurrentCandidate is set to the point in instance space for which we
   *      wish to compute the nll of
   * @return
   */
  protected double computeNegativeLogLikelihood(double[] x) {

    double nll = 0; // -LogLikelihood
    for (int i = 0; i < m_Classes.length; i++) { // ith bag
      int nI = m_Data[i][0].length; // numInstances in ith bag
      double bag = 0.0; // NLL of pos bag

      for (int j = 0; j < nI; j++) {
        double ins = 0.0;
        for (int k = 0; k < m_Data[i].length; k++) {
          double temp = (m_Data[i][k][j] - m_CurrentCandidate[k]) * x[k];
          ins += temp * temp;
        }
        ins = Math.exp(-ins);
        ins = 1.0 - ins;

        if (m_Classes[i] == m_posClass) {
          bag += Math.log(ins);
        } else {
          if (ins <= m_Zero) {
            ins = m_Zero;
          }
          bag += Math.log(ins);
        }
      }

      if (m_Classes[i] == m_posClass) {
        bag = 1.0 - m_maxProbNegativeClass * Math.exp(bag);
        if (bag <= m_Zero) {
          bag = m_Zero;
        }
        nll -= m_BagWeights[i] * Math.log(bag);
      } else {
        bag = m_maxProbNegativeClass * Math.exp(bag);
        if (bag <= m_Zero) {
          bag = m_Zero;
        }
        nll -= m_BagWeights[i] * Math.log(bag);
      }
    }

    return nll;
  }

  /**
   * The derived version of the optimization class.
   */
  private class OptEng extends Optimization {

    /**
     * Evaluate objective function
     * 
     * @requires m_CurrentCandidate to be set to the current candidate target
     *           point
     * @param x the current values of the scaling factors for each dimension
     * @return the value of the objective function, ie the negative-log diverse
     *         density at m_CurrentCandidate when the scaling vector is
     *         optimized.
     */
    @Override
    protected double objectiveFunction(double[] x) {
      return computeNegativeLogLikelihood(x);
    }

    /**
     * Evaluate Jacobian vector
     * 
     * @param x the current values of variables
     * @return the gradient vector
     */
    @Override
    protected double[] evaluateGradient(double[] x) {

      double[] grad = new double[x.length];
      for (int i = 0; i < m_Classes.length; i++) { // ith bag
        int nI = m_Data[i][0].length; // numInstances in ith bag

        double denom = 0.0;
        double[] numrt = new double[x.length];

        for (int j = 0; j < nI; j++) {
          double exp = 0.0;
          for (int k = 0; k < m_Data[i].length; k++) {
            double temp = (m_Data[i][k][j] - m_CurrentCandidate[k]) * x[k];
            exp += temp * temp;
          }
          exp = Math.exp(-exp);
          exp = 1.0 - exp;
          if (m_Classes[i] == m_posClass) {
            denom += Math.log(exp);
          }

          if (exp <= m_Zero) {
            exp = m_Zero;
          }
          // Instance-wise update
          double fact = 2.0 * (1.0 - exp) / exp;
          for (int p = 0; p < m_Data[i].length; p++) { // pth variable
            double temp = (m_CurrentCandidate[p] - m_Data[i][p][j]);
            numrt[p] += fact * temp * temp * x[p];
          }
        }

        // Bag-wise update
        denom = 1.0 - m_maxProbNegativeClass * Math.exp(denom);
        if (denom <= m_Zero) {
          denom = m_Zero;
        }
        for (int q = 0; q < m_Data[i].length; q++) {
          if (m_Classes[i] == m_posClass) {
            grad[q] += m_BagWeights[i] * numrt[q] * (1.0 - denom) / denom;
          } else {
            grad[q] -= m_BagWeights[i] * numrt[q];
          }
        }

      } // one bag

      return grad;
    }

    /**
     * Returns the revision string
     */
    @Override
    public String getRevision() {
      return RevisionUtils.extract("$Revision$");
    }
  }

  /**
   * Returns default capabilities of the classifier.
   * 
   * @return the capabilities of this classifier
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.RELATIONAL_ATTRIBUTES);

    // class
    result.enable(Capability.BINARY_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    // other
    result.enable(Capability.ONLY_MULTIINSTANCE);

    return result;
  }

  /**
   * Returns the capabilities of this multi-instance classifier for the
   * relational data.
   * 
   * @return the capabilities of this object
   * @see Capabilities
   */
  @Override
  public Capabilities getMultiInstanceCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.disableAllClasses();
    result.enable(Capability.NO_CLASS);

    return result;
  }

  /**
   * Builds the classifier
   * 
   * @param train the training data to be used for generating the boosted
   *          classifier.
   * @throws Exception if the classifier could not be built successfully
   */
  @Override
  public void buildClassifier(Instances train) throws Exception {
    // can classifier handle the data?
    getCapabilities().testWithFail(train);

    // remove instances with missing class
    train = new Instances(train);
    train.deleteWithMissingClass();

    m_ClassIndex = train.classIndex();
    m_NumClasses = train.numClasses();

    int nR = train.attribute(1).relation().numAttributes();
    int nC = train.numInstances();
    // ArrayList<Integer> maxSzIdx = new ArrayList<Integer>(); NOT USED
    // int maxSz = 0; NOT USED
    int[] bagSize = new int[nC];
    Instances datasets = new Instances(train.attribute(1).relation(), 0);

    m_Data = new double[nC][nR][]; // Data values
    m_Classes = new int[nC]; // Class values
    m_BagWeights = new double[nC]; // Bag weights
    m_Attributes = datasets.stringFreeStructure();
    if (m_Debug) {
      System.out.println("Extracting data...");
    }

    for (int h = 0; h < nC; h++) {// h_th bag
      Instance current = train.instance(h);
      m_Classes[h] = (int) current.classValue(); // Class value starts from 0
      m_BagWeights[h] = current.weight();
      Instances currInsts = current.relationalValue(1);
      for (int i = 0; i < currInsts.numInstances(); i++) {
        Instance inst = currInsts.instance(i);
        datasets.add(inst);
      }

      int nI = currInsts.numInstances();
      bagSize[h] = nI;
    }

    /* filter the training data */
    if (m_filterType == FILTER_STANDARDIZE) {
      m_Filter = new Standardize();
    } else if (m_filterType == FILTER_NORMALIZE) {
      m_Filter = new Normalize();
    } else {
      m_Filter = null;
    }

    if (m_Filter != null) {
      m_Filter.setInputFormat(datasets);
      datasets = Filter.useFilter(datasets, m_Filter);
    }

    m_Missing.setInputFormat(datasets);
    datasets = Filter.useFilter(datasets, m_Missing);

    int instIndex = 0;
    int start = 0;
    for (int h = 0; h < nC; h++) {
      for (int i = 0; i < datasets.numAttributes(); i++) {
        // initialize m_data[][][]
        m_Data[h][i] = new double[bagSize[h]];
        instIndex = start;
        for (int k = 0; k < bagSize[h]; k++) {
          m_Data[h][i][k] = datasets.instance(instIndex).value(i);
          instIndex++;
        }
      }
      start = instIndex;
    }

    if (m_Debug) {
      System.out.println("\nIteration History...");
    }

    double bestOverall = Double.MAX_VALUE;
    double[] bestPar = new double[2 * nR];
    byte bestPosClass = 1;
    for (m_posClass = 1; (m_posClass >= 0 && m_considerBothClasses)
      || (m_posClass == 1); m_posClass--) {

      double[] tmp = new double[nR];
      m_CurrentCandidate = new double[nR];
      double[][] b = new double[2][nR];

      OptEng opt;
      double nll, bestnll = Double.MAX_VALUE;
      for (int t = 0; t < nR; t++) {
        b[0][t] = Double.NaN;
        b[1][t] = Double.NaN;
      }

      double[] scalingVector = new double[nR];
      for (int i = 0; i < scalingVector.length; i++) {
        scalingVector[i] = m_scaleFactor;
      }

      double lastnll;
      int numIterations = 0;
      do {
        numIterations++;
        if (m_Debug) {
          System.err.println("iteration " + numIterations);
        }
        lastnll = bestnll;

        // find best target point with current scaling vector
        for (int exIdx = 0; exIdx < m_Data.length; exIdx++) // for each bag.
                                                            // -jimmy
        {
          if (m_Classes[exIdx] != m_posClass) // if not positive, skip it.
                                              // -jimmy
          {
            if (m_Debug) {
              System.err.println(exIdx + " " + m_BagWeights[exIdx]);
            }
            continue;
          }
          for (int p = 0; p < m_Data[exIdx][0].length; p++) {
            for (int q = 0; q < nR; q++) {
              m_CurrentCandidate[q] = m_Data[exIdx][q][p]; // pick one instance
            }

            // int t = 0; NOT USED
            nll = computeNegativeLogLikelihood(scalingVector);
            if (m_Debug) {
              System.err.print(exIdx + " " + p + " " + m_BagWeights[exIdx]
                + " " + nll + " ");
              for (int i = 0; i < nR; i++) {
                System.err.print(m_Data[exIdx][i][p] + ", ");
              }
              System.err.println();
            }

            if (nll < bestnll) {
              bestnll = nll;
              m_Par = new double[m_CurrentCandidate.length * 2];
              for (int i = 0; i < m_CurrentCandidate.length; i++) {
                m_Par[2 * i] = m_CurrentCandidate[i];
                m_Par[2 * i + 1] = scalingVector[i];
              }
            }
          }
        }

        // Retrieve the best point for the current scaling, found in the last
        // loop
        for (int i = 0; i < nR; i++) {
          m_CurrentCandidate[i] = m_Par[2 * i];
        }
        if (m_Debug) {
          System.err.println("********* Finding best scaling vector");
        }

        // find best scale vector at that point
        opt = new OptEng();
        // opt.setDebug(m_Debug);
        tmp = opt.findArgmin(scalingVector, b);
        while (tmp == null) {
          tmp = opt.getVarbValues();
          if (m_Debug) {
            System.out.println("200 iterations finished, not enough!");
          }
          tmp = opt.findArgmin(tmp, b);
        }
        nll = opt.getMinFunction();
        scalingVector = tmp;// TODO should this be in or out of the if?
        if (nll < bestnll) {
          bestnll = nll;
          m_Par = new double[m_CurrentCandidate.length * 2];
          for (int i = 0; i < m_CurrentCandidate.length; i++) {
            m_Par[2 * i] = m_CurrentCandidate[i];
            m_Par[2 * i + 1] = scalingVector[i];
          }
        }

        if (m_Debug) {
          System.err.println("---------------     " + bestnll);
        }

      } while (bestnll < lastnll && numIterations < m_maxIterations);

      // Is this the new best class?
      if (bestnll < bestOverall) {
        bestPosClass = m_posClass;
        bestOverall = bestnll;
        System.arraycopy(m_Par, 0, bestPar, 0, bestPar.length);
        if (m_Debug) {
          System.err.println("New best class: " + bestPosClass);
          System.err.println("New best nll: " + bestnll);
          for (double element : bestPar) {
            System.err.print(element + ",");
          }
          System.err.println();
        }
      }
    }

    m_Par = bestPar;
    m_posClass = bestPosClass;

    m_Data = null;
    m_Classes = null;
    m_CurrentCandidate = null;
    m_BagWeights = null;
  }

  /**
   * Computes the distribution for a given exemplar
   * 
   * @param exmp the exemplar for which distribution is computed
   * @return the distribution
   * @throws Exception if the distribution can't be computed successfully
   */
  @Override
  public double[] distributionForInstance(Instance exmp) throws Exception {

    // Extract the data
    Instances ins = exmp.relationalValue(1);
    if (m_Filter != null) {
      ins = Filter.useFilter(ins, m_Filter);
    }

    ins = Filter.useFilter(ins, m_Missing);

    int nI = ins.numInstances(), nA = ins.numAttributes();
    double[][] dat = new double[nI][nA];
    for (int j = 0; j < nI; j++) {
      for (int k = 0; k < nA; k++) {
        dat[j][k] = ins.instance(j).value(k);
      }
    }

    // Compute the probability of the bag
    double[] distribution = new double[2];
    distribution[1 - m_posClass] = 0.0; // log-Prob. for class 0

    for (int i = 0; i < nI; i++) {
      double exp = 0.0;
      for (int r = 0; r < nA; r++) {
        exp += (m_Par[r * 2] - dat[i][r]) * (m_Par[r * 2] - dat[i][r])
          * m_Par[r * 2 + 1] * m_Par[r * 2 + 1];
      }
      exp = Math.exp(-exp);

      // Prob. updated for one instance
      distribution[1 - m_posClass] += Math.log(1.0 - exp);
    }

    distribution[1 - m_posClass] = m_maxProbNegativeClass
      * Math.exp(distribution[1 - m_posClass]);
    distribution[m_posClass] = 1.0 - distribution[1 - m_posClass];

    return distribution;
  }

  /**
   * Gets a string describing the classifier.
   * 
   * @return a string describing the classifer built.
   */
  @Override
  public String toString() {

    String result = "Diverse Density";
    if (m_Par == null) {
      return result + ": No model built yet.";
    }

    result += "\nPositive Class:" + m_posClass + "\n";

    result += "\nCoefficients...\n" + "Variable       Point       Scale\n";
    for (int j = 0, idx = 0; j < m_Par.length / 2; j++, idx++) {
      result += m_Attributes.attribute(idx).name();
      result += " " + Utils.doubleToString(m_Par[j * 2], 12, 4);
      result += " " + Utils.doubleToString(m_Par[j * 2 + 1], 12, 4) + "\n";
    }

    return result;
  }

  /**
   * Main method for testing this class.
   * 
   * @param argv should contain the command line arguments to the scheme (see
   *          Evaluation)
   */
  public static void main(String[] argv) {
    runClassifier(new QuickDDIterative(), argv);
  }

  /**
   * Returns the revision string
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }
}
