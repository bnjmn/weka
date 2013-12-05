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
 * MIEMDD.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.mi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

import weka.classifiers.RandomizableClassifier;
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
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import weka.filters.unsupervised.attribute.Standardize;

/**
 * <!-- globalinfo-start --> EMDD model builds heavily upon Dietterich's Diverse
 * Density (DD) algorithm.<br/>
 * It is a general framework for MI learning of converting the MI problem to a
 * single-instance setting using EM. In this implementation, we use most-likely
 * cause DD model and only use 3 random selected postive bags as initial
 * starting points of EM.<br/>
 * <br/>
 * For more information see:<br/>
 * <br/>
 * Qi Zhang, Sally A. Goldman: EM-DD: An Improved Multiple-Instance Learning
 * Technique. In: Advances in Neural Information Processing Systems 14,
 * 1073-108, 2001.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- technical-bibtex-start --> BibTeX:
 * 
 * <pre>
 * &#64;inproceedings{Zhang2001,
 *    author = {Qi Zhang and Sally A. Goldman},
 *    booktitle = {Advances in Neural Information Processing Systems 14},
 *    pages = {1073-108},
 *    publisher = {MIT Press},
 *    title = {EM-DD: An Improved Multiple-Instance Learning Technique},
 *    year = {2001}
 * }
 * </pre>
 * <p/>
 * <!-- technical-bibtex-end -->
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
 *  Random number seed.
 *  (default 1)
 * </pre>
 * 
 * <pre>
 * -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Lin Dong (ld21@cs.waikato.ac.nz)
 * @version $Revision$
 */
public class MIEMDD extends RandomizableClassifier implements OptionHandler,
  MultiInstanceCapabilitiesHandler, TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = 3899547154866223734L;

  /** The index of the class attribute */
  protected int m_ClassIndex;

  protected double[] m_Par;

  /** The number of the class labels */
  protected int m_NumClasses;

  /** Class labels for each bag */
  protected int[] m_Classes;

  /** MI data */
  protected double[][][] m_Data;

  /** All attribute names */
  protected Instances m_Attributes;

  /** MI data */
  protected double[][] m_emData;

  /** The filter used to standardize/normalize all values. */
  protected Filter m_Filter = null;

  /** Whether to normalize/standardize/neither, default:standardize */
  protected int m_filterType = FILTER_STANDARDIZE;

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

  /** The filter used to get rid of missing values. */
  protected ReplaceMissingValues m_Missing = new ReplaceMissingValues();

  /**
   * Returns a string describing this filter
   * 
   * @return a description of the filter suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "EMDD model builds heavily upon Dietterich's Diverse Density (DD) "
      + "algorithm.\nIt is a general framework for MI learning of converting "
      + "the MI problem to a single-instance setting using EM. In this "
      + "implementation, we use most-likely cause DD model and only use 3 "
      + "random selected postive bags as initial starting points of EM.\n\n"
      + "For more information see:\n\n" + getTechnicalInformation().toString();
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
    result.setValue(Field.AUTHOR, "Qi Zhang and Sally A. Goldman");
    result.setValue(Field.TITLE,
      "EM-DD: An Improved Multiple-Instance Learning Technique");
    result.setValue(Field.BOOKTITLE,
      "Advances in Neural Information Processing Systems 14");
    result.setValue(Field.YEAR, "2001");
    result.setValue(Field.PAGES, "1073-108");
    result.setValue(Field.PUBLISHER, "MIT Press");

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
   *  Random number seed.
   *  (default 1)
   * </pre>
   * 
   * <pre>
   * -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console
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

    tmpStr = Utils.getOption('N', options);
    if (tmpStr.length() != 0) {
      setFilterType(new SelectedTag(Integer.parseInt(tmpStr), TAGS_FILTER));
    } else {
      setFilterType(new SelectedTag(FILTER_STANDARDIZE, TAGS_FILTER));
    }

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

  private class OptEng extends Optimization {
    /**
     * Evaluate objective function
     * 
     * @param x the current values of variables
     * @return the value of the objective function
     */
    @Override
    protected double objectiveFunction(double[] x) {
      double nll = 0; // -LogLikelihood
      for (int i = 0; i < m_Classes.length; i++) { // ith bag
        double ins = 0.0;
        for (int k = 0; k < m_emData[i].length; k++) {
          ins += (m_emData[i][k] - x[k * 2]) * (m_emData[i][k] - x[k * 2])
            * x[k * 2 + 1] * x[k * 2 + 1];
        }
        ins = Math.exp(-ins); // Pr. of being positive

        if (m_Classes[i] == 1) {
          if (ins <= m_Zero) {
            ins = m_Zero;
          }
          nll -= Math.log(ins); // bag level -LogLikelihood
        } else {
          ins = 1.0 - ins; // Pr. of being negative
          if (ins <= m_Zero) {
            ins = m_Zero;
          }
          nll -= Math.log(ins);
        }
      }
      return nll;
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
        double[] numrt = new double[x.length];
        double exp = 0.0;
        for (int k = 0; k < m_emData[i].length; k++) {
          exp += (m_emData[i][k] - x[k * 2]) * (m_emData[i][k] - x[k * 2])
            * x[k * 2 + 1] * x[k * 2 + 1];
        }
        exp = Math.exp(-exp); // Pr. of being positive

        // Instance-wise update
        for (int p = 0; p < m_emData[i].length; p++) { // pth variable
          numrt[2 * p] = 2.0 * (x[2 * p] - m_emData[i][p]) * x[p * 2 + 1]
            * x[p * 2 + 1];
          numrt[2 * p + 1] = 2.0 * (x[2 * p] - m_emData[i][p])
            * (x[2 * p] - m_emData[i][p]) * x[p * 2 + 1];
        }

        // Bag-wise update
        for (int q = 0; q < m_emData[i].length; q++) {
          if (m_Classes[i] == 1) {// derivation of (-LogLikeliHood) for positive
                                  // bags
            grad[2 * q] += numrt[2 * q];
            grad[2 * q + 1] += numrt[2 * q + 1];
          } else { // derivation of (-LogLikeliHood) for negative bags
            grad[2 * q] -= numrt[2 * q] * exp / (1.0 - exp);
            grad[2 * q + 1] -= numrt[2 * q + 1] * exp / (1.0 - exp);
          }
        }
      } // one bag

      return grad;
    }

    /**
     * Returns the revision string.
     * 
     * @return the revision
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
    result.disableAll();

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
    int[] bagSize = new int[nC];
    Instances datasets = new Instances(train.attribute(1).relation(), 0);

    m_Data = new double[nC][nR][]; // Data values
    m_Classes = new int[nC]; // Class values
    m_Attributes = datasets.stringFreeStructure();
    if (m_Debug) {
      System.out.println("\n\nExtracting data...");
    }

    for (int h = 0; h < nC; h++) {// h_th bag
      Instance current = train.instance(h);
      m_Classes[h] = (int) current.classValue(); // Class value starts from 0
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
      System.out.println("\n\nIteration History...");
    }

    m_emData = new double[nC][nR];
    m_Par = new double[2 * nR];

    double[] x = new double[nR * 2];
    double[] tmp = new double[x.length];
    double[] pre_x = new double[x.length];
    double[] best_hypothesis = new double[x.length];
    double[][] b = new double[2][x.length];

    OptEng opt;
    double bestnll = Double.MAX_VALUE;
    double min_error = Double.MAX_VALUE;
    double nll, pre_nll;
    int iterationCount;

    for (int t = 0; t < x.length; t++) {
      b[0][t] = Double.NaN;
      b[1][t] = Double.NaN;
    }

    // random pick 3 positive bags
    Random r = new Random(getSeed());
    ArrayList<Integer> index = new ArrayList<Integer>();
    int n1, n2, n3;
    do {
      n1 = r.nextInt(nC - 1);
    } while (m_Classes[n1] == 0);
    index.add(new Integer(n1));

    do {
      n2 = r.nextInt(nC - 1);
    } while (n2 == n1 || m_Classes[n2] == 0);
    index.add(new Integer(n2));

    do {
      n3 = r.nextInt(nC - 1);
    } while (n3 == n1 || n3 == n2 || m_Classes[n3] == 0);
    index.add(new Integer(n3));

    for (int s = 0; s < index.size(); s++) {
      int exIdx = index.get(s).intValue();
      if (m_Debug) {
        System.out.println("\nH0 at " + exIdx);
      }

      for (int p = 0; p < m_Data[exIdx][0].length; p++) {
        // initialize a hypothesis
        for (int q = 0; q < nR; q++) {
          x[2 * q] = m_Data[exIdx][q][p];
          x[2 * q + 1] = 1.0;
        }

        pre_nll = Double.MAX_VALUE;
        nll = Double.MAX_VALUE / 10.0;
        iterationCount = 0;
        // while (Math.abs(nll-pre_nll)>0.01*pre_nll && iterationCount<10) {
        // //stop condition
        while (nll < pre_nll && iterationCount < 10) {
          iterationCount++;
          pre_nll = nll;

          if (m_Debug) {
            System.out.println("\niteration: " + iterationCount);
          }

          // E-step (find one instance from each bag with max likelihood )
          for (int i = 0; i < m_Data.length; i++) { // for each bag

            int insIndex = findInstance(i, x);

            for (int att = 0; att < m_Data[0].length; att++) {
              m_emData[i][att] = m_Data[i][att][insIndex];
            }
          }
          if (m_Debug) {
            System.out.println("E-step for new H' finished");
          }

          // M-step
          opt = new OptEng();
          tmp = opt.findArgmin(x, b);
          while (tmp == null) {
            tmp = opt.getVarbValues();
            if (m_Debug) {
              System.out.println("200 iterations finished, not enough!");
            }
            tmp = opt.findArgmin(tmp, b);
          }
          nll = opt.getMinFunction();

          pre_x = x;
          x = tmp; // update hypothesis

          // keep the track of the best target point which has the minimum nll
          /*
           * if (nll < bestnll) { bestnll = nll; m_Par = tmp; if (m_Debug)
           * System.out.println("!!!!!!!!!!!!!!!!Smaller NLL found: " + nll); }
           */

          // if (m_Debug)
          // System.out.println(exIdx+" "+p+": "+nll+" "+pre_nll+" " +bestnll);

        } // converged for one instance

        // evaluate the hypothesis on the training data and
        // keep the track of the hypothesis with minimum error on training data
        double distribution[] = new double[2];
        int error = 0;
        if (nll > pre_nll) {
          m_Par = pre_x;
        } else {
          m_Par = x;
        }

        for (int i = 0; i < train.numInstances(); i++) {
          distribution = distributionForInstance(train.instance(i));
          if (distribution[1] >= 0.5 && m_Classes[i] == 0) {
            error++;
          } else if (distribution[1] < 0.5 && m_Classes[i] == 1) {
            error++;
          }
        }
        if (error < min_error) {
          best_hypothesis = m_Par;
          min_error = error;
          if (nll > pre_nll) {
            bestnll = pre_nll;
          } else {
            bestnll = nll;
          }
          if (m_Debug) {
            System.out.println("error= " + error + "  nll= " + bestnll);
          }
        }
      }
      if (m_Debug) {
        System.out.println(exIdx + ":  -------------<Converged>--------------");
        System.out.println("current minimum error= " + min_error + "  nll= "
          + bestnll);
      }
    }
    m_Par = best_hypothesis;
  }

  /**
   * given x, find the instance in ith bag with the most likelihood probability,
   * which is most likely to responsible for the label of the bag For a positive
   * bag, find the instance with the maximal probability of being positive For a
   * negative bag, find the instance with the minimal probability of being
   * negative
   * 
   * @param i the bag index
   * @param x the current values of variables
   * @return index of the instance in the bag
   */
  protected int findInstance(int i, double[] x) {

    double min = Double.MAX_VALUE;
    int insIndex = 0;
    int nI = m_Data[i][0].length; // numInstances in ith bag

    for (int j = 0; j < nI; j++) {
      double ins = 0.0;
      for (int k = 0; k < m_Data[i].length; k++) {
        ins += (m_Data[i][k][j] - x[k * 2]) * (m_Data[i][k][j] - x[k * 2])
          * x[k * 2 + 1] * x[k * 2 + 1];
      }

      // the probability can be calculated as Math.exp(-ins)
      // to find the maximum Math.exp(-ins) is equivalent to find the minimum of
      // (ins)
      if (ins < min) {
        min = ins;
        insIndex = j;
      }
    }
    return insIndex;
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
    // find the concept instance in the exemplar
    double min = Double.MAX_VALUE;
    double maxProb = -1.0;
    for (int j = 0; j < nI; j++) {
      double exp = 0.0;
      for (int k = 0; k < nA; k++) {
        exp += (dat[j][k] - m_Par[k * 2]) * (dat[j][k] - m_Par[k * 2])
          * m_Par[k * 2 + 1] * m_Par[k * 2 + 1];
      }
      // the probability can be calculated as Math.exp(-exp)
      // to find the maximum Math.exp(-exp) is equivalent to find the minimum of
      // (exp)
      if (exp < min) {
        min = exp;
        maxProb = Math.exp(-exp); // maximum probability of being positive
      }
    }

    // Compute the probability of the bag
    double[] distribution = new double[2];
    distribution[1] = maxProb;
    distribution[0] = 1.0 - distribution[1]; // mininum prob. of being negative

    return distribution;
  }

  /**
   * Gets a string describing the classifier.
   * 
   * @return a string describing the classifer built.
   */
  @Override
  public String toString() {

    String result = "MIEMDD";
    if (m_Par == null) {
      return result + ": No model built yet.";
    }

    result += "\nCoefficients...\n" + "Variable       Point       Scale\n";
    for (int j = 0, idx = 0; j < m_Par.length / 2; j++, idx++) {
      result += m_Attributes.attribute(idx).name();
      result += " " + Utils.doubleToString(m_Par[j * 2], 12, 4);
      result += " " + Utils.doubleToString(m_Par[j * 2 + 1], 12, 4) + "\n";
    }

    return result;
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  /**
   * Main method for testing this class.
   * 
   * @param argv should contain the command line arguments to the scheme (see
   *          Evaluation)
   */
  public static void main(String[] argv) {
    runClassifier(new MIEMDD(), argv);
  }
}
