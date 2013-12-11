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
 *    SVMAttributeEval.java
 *    Copyright (C) 2002 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.attributeSelection;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import weka.classifiers.functions.SMO;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.MakeIndicator;

/**
 * <!-- globalinfo-start --> SVMAttributeEval :<br/>
 * <br/>
 * Evaluates the worth of an attribute by using an SVM classifier. Attributes
 * are ranked by the square of the weight assigned by the SVM. Attribute
 * selection for multiclass problems is handled by ranking attributes for each
 * class seperately using a one-vs-all method and then "dealing" from the top of
 * each pile to give a final ranking.<br/>
 * <br/>
 * For more information see:<br/>
 * <br/>
 * I. Guyon, J. Weston, S. Barnhill, V. Vapnik (2002). Gene selection for cancer
 * classification using support vector machines. Machine Learning. 46:389-422.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- technical-bibtex-start --> BibTeX:
 * 
 * <pre>
 * &#64;article{Guyon2002,
 *    author = {I. Guyon and J. Weston and S. Barnhill and V. Vapnik},
 *    journal = {Machine Learning},
 *    pages = {389-422},
 *    title = {Gene selection for cancer classification using support vector machines},
 *    volume = {46},
 *    year = {2002}
 * }
 * </pre>
 * <p/>
 * <!-- technical-bibtex-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -X &lt;constant rate of elimination&gt;
 *  Specify the constant rate of attribute
 *  elimination per invocation of
 *  the support vector machine.
 *  Default = 1.
 * </pre>
 * 
 * <pre>
 * -Y &lt;percent rate of elimination&gt;
 *  Specify the percentage rate of attributes to
 *  elimination per invocation of
 *  the support vector machine.
 *  Trumps constant rate (above threshold).
 *  Default = 0.
 * </pre>
 * 
 * <pre>
 * -Z &lt;threshold for percent elimination&gt;
 *  Specify the threshold below which 
 *  percentage attribute elimination
 *  reverts to the constant method.
 * </pre>
 * 
 * <pre>
 * -P &lt;epsilon&gt;
 *  Specify the value of P (epsilon
 *  parameter) to pass on to the
 *  support vector machine.
 *  Default = 1.0e-25
 * </pre>
 * 
 * <pre>
 * -T &lt;tolerance&gt;
 *  Specify the value of T (tolerance
 *  parameter) to pass on to the
 *  support vector machine.
 *  Default = 1.0e-10
 * </pre>
 * 
 * <pre>
 * -C &lt;complexity&gt;
 *  Specify the value of C (complexity
 *  parameter) to pass on to the
 *  support vector machine.
 *  Default = 1.0
 * </pre>
 * 
 * <pre>
 * -N
 *  Whether the SVM should 0=normalize/1=standardize/2=neither.
 *  (default 0=normalize)
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @author Kieran Holland
 * @version $Revision$
 */
public class SVMAttributeEval extends ASEvaluation implements
  AttributeEvaluator, OptionHandler, TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = -6489975709033967447L;

  /** The attribute scores */
  private double[] m_attScores;

  /** Constant rate of attribute elimination per iteration */
  private int m_numToEliminate = 1;

  /**
   * Percentage rate of attribute elimination, trumps constant rate (above
   * threshold), ignored if = 0
   */
  private int m_percentToEliminate = 0;

  /**
   * Threshold below which percent elimination switches to constant elimination
   */
  private int m_percentThreshold = 0;

  /** Complexity parameter to pass on to SMO */
  private double m_smoCParameter = 1.0;

  /** Tolerance parameter to pass on to SMO */
  private double m_smoTParameter = 1.0e-10;

  /** Epsilon parameter to pass on to SMO */
  private double m_smoPParameter = 1.0e-25;

  /** Filter parameter to pass on to SMO */
  private int m_smoFilterType = 0;

  /**
   * Returns a string describing this attribute evaluator
   * 
   * @return a description of the evaluator suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "SVMAttributeEval :\n\nEvaluates the worth of an attribute by "
      + "using an SVM classifier. Attributes are ranked by the square of the "
      + "weight assigned by the SVM. Attribute selection for multiclass "
      + "problems is handled by ranking attributes for each class seperately "
      + "using a one-vs-all method and then \"dealing\" from the top of "
      + "each pile to give a final ranking.\n\n"
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

    result = new TechnicalInformation(Type.ARTICLE);
    result.setValue(Field.AUTHOR,
      "I. Guyon and J. Weston and S. Barnhill and V. Vapnik");
    result.setValue(Field.YEAR, "2002");
    result.setValue(Field.TITLE,
      "Gene selection for cancer classification using support vector machines");
    result.setValue(Field.JOURNAL, "Machine Learning");
    result.setValue(Field.VOLUME, "46");
    result.setValue(Field.PAGES, "389-422");

    return result;
  }

  /**
   * Constructor
   */
  public SVMAttributeEval() {
    resetOptions();
  }

  /**
   * Returns an enumeration describing all the available options
   * 
   * @return an enumeration of options
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>(7);

    newVector.addElement(new Option(
      "\tSpecify the constant rate of attribute\n"
        + "\telimination per invocation of\n"
        + "\tthe support vector machine.\n" + "\tDefault = 1.", "X", 1,
      "-X <constant rate of elimination>"));

    newVector.addElement(new Option(
      "\tSpecify the percentage rate of attributes to\n"
        + "\telimination per invocation of\n"
        + "\tthe support vector machine.\n"
        + "\tTrumps constant rate (above threshold).\n" + "\tDefault = 0.",
      "Y", 1, "-Y <percent rate of elimination>"));

    newVector.addElement(new Option("\tSpecify the threshold below which \n"
      + "\tpercentage attribute elimination\n"
      + "\treverts to the constant method.", "Z", 1,
      "-Z <threshold for percent elimination>"));

    newVector.addElement(new Option("\tSpecify the value of P (epsilon\n"
      + "\tparameter) to pass on to the\n" + "\tsupport vector machine.\n"
      + "\tDefault = 1.0e-25", "P", 1, "-P <epsilon>"));

    newVector.addElement(new Option("\tSpecify the value of T (tolerance\n"
      + "\tparameter) to pass on to the\n" + "\tsupport vector machine.\n"
      + "\tDefault = 1.0e-10", "T", 1, "-T <tolerance>"));

    newVector.addElement(new Option("\tSpecify the value of C (complexity\n"
      + "\tparameter) to pass on to the\n" + "\tsupport vector machine.\n"
      + "\tDefault = 1.0", "C", 1, "-C <complexity>"));

    newVector.addElement(new Option("\tWhether the SVM should "
      + "0=normalize/1=standardize/2=neither.\n" + "\t(default 0=normalize)",
      "N", 1, "-N"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   * <!-- options-start --> Valid options are:
   * <p/>
   * 
   * <pre>
   * -X &lt;constant rate of elimination&gt;
   *  Specify the constant rate of attribute
   *  elimination per invocation of
   *  the support vector machine.
   *  Default = 1.
   * </pre>
   * 
   * <pre>
   * -Y &lt;percent rate of elimination&gt;
   *  Specify the percentage rate of attributes to
   *  elimination per invocation of
   *  the support vector machine.
   *  Trumps constant rate (above threshold).
   *  Default = 0.
   * </pre>
   * 
   * <pre>
   * -Z &lt;threshold for percent elimination&gt;
   *  Specify the threshold below which 
   *  percentage attribute elimination
   *  reverts to the constant method.
   * </pre>
   * 
   * <pre>
   * -P &lt;epsilon&gt;
   *  Specify the value of P (epsilon
   *  parameter) to pass on to the
   *  support vector machine.
   *  Default = 1.0e-25
   * </pre>
   * 
   * <pre>
   * -T &lt;tolerance&gt;
   *  Specify the value of T (tolerance
   *  parameter) to pass on to the
   *  support vector machine.
   *  Default = 1.0e-10
   * </pre>
   * 
   * <pre>
   * -C &lt;complexity&gt;
   *  Specify the value of C (complexity
   *  parameter) to pass on to the
   *  support vector machine.
   *  Default = 1.0
   * </pre>
   * 
   * <pre>
   * -N
   *  Whether the SVM should 0=normalize/1=standardize/2=neither.
   *  (default 0=normalize)
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an error occurs
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    String optionString;

    optionString = Utils.getOption('X', options);
    if (optionString.length() != 0) {
      setAttsToEliminatePerIteration(Integer.parseInt(optionString));
    }

    optionString = Utils.getOption('Y', options);
    if (optionString.length() != 0) {
      setPercentToEliminatePerIteration(Integer.parseInt(optionString));
    }

    optionString = Utils.getOption('Z', options);
    if (optionString.length() != 0) {
      setPercentThreshold(Integer.parseInt(optionString));
    }

    optionString = Utils.getOption('P', options);
    if (optionString.length() != 0) {
      setEpsilonParameter((new Double(optionString)).doubleValue());
    }

    optionString = Utils.getOption('T', options);
    if (optionString.length() != 0) {
      setToleranceParameter((new Double(optionString)).doubleValue());
    }

    optionString = Utils.getOption('C', options);
    if (optionString.length() != 0) {
      setComplexityParameter((new Double(optionString)).doubleValue());
    }

    optionString = Utils.getOption('N', options);
    if (optionString.length() != 0) {
      setFilterType(new SelectedTag(Integer.parseInt(optionString),
        SMO.TAGS_FILTER));
    } else {
      setFilterType(new SelectedTag(SMO.FILTER_NORMALIZE, SMO.TAGS_FILTER));
    }

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of SVMAttributeEval
   * 
   * @return an array of strings suitable for passing to setOptions()
   */
  @Override
  public String[] getOptions() {

    Vector<String> options = new Vector<String>();

    options.add("-X");
    options.add("" + getAttsToEliminatePerIteration());

    options.add("-Y");
    options.add("" + getPercentToEliminatePerIteration());

    options.add("-Z");
    options.add("" + getPercentThreshold());

    options.add("-P");
    options.add("" + getEpsilonParameter());

    options.add("-T");
    options.add("" + getToleranceParameter());

    options.add("-C");
    options.add("" + getComplexityParameter());

    options.add("-N");
    options.add("" + m_smoFilterType);

    return options.toArray(new String[0]);
  }

  // ________________________________________________________________________

  /**
   * Returns a tip text for this property suitable for display in the GUI
   * 
   * @return tip text string describing this property
   */
  public String attsToEliminatePerIterationTipText() {
    return "Constant rate of attribute elimination.";
  }

  /**
   * Returns a tip text for this property suitable for display in the GUI
   * 
   * @return tip text string describing this property
   */
  public String percentToEliminatePerIterationTipText() {
    return "Percent rate of attribute elimination.";
  }

  /**
   * Returns a tip text for this property suitable for display in the GUI
   * 
   * @return tip text string describing this property
   */
  public String percentThresholdTipText() {
    return "Threshold below which percent elimination reverts to constant elimination.";
  }

  /**
   * Returns a tip text for this property suitable for display in the GUI
   * 
   * @return tip text string describing this property
   */
  public String epsilonParameterTipText() {
    return "P epsilon parameter to pass to the SVM";
  }

  /**
   * Returns a tip text for this property suitable for display in the GUI
   * 
   * @return tip text string describing this property
   */
  public String toleranceParameterTipText() {
    return "T tolerance parameter to pass to the SVM";
  }

  /**
   * Returns a tip text for this property suitable for display in the GUI
   * 
   * @return tip text string describing this property
   */
  public String complexityParameterTipText() {
    return "C complexity parameter to pass to the SVM";
  }

  /**
   * Returns a tip text for this property suitable for display in the GUI
   * 
   * @return tip text string describing this property
   */
  public String filterTypeTipText() {
    return "filtering used by the SVM";
  }

  // ________________________________________________________________________

  /**
   * Set the constant rate of attribute elimination per iteration
   * 
   * @param cRate the constant rate of attribute elimination per iteration
   */
  public void setAttsToEliminatePerIteration(int cRate) {
    m_numToEliminate = cRate;
  }

  /**
   * Get the constant rate of attribute elimination per iteration
   * 
   * @return the constant rate of attribute elimination per iteration
   */
  public int getAttsToEliminatePerIteration() {
    return m_numToEliminate;
  }

  /**
   * Set the percentage of attributes to eliminate per iteration
   * 
   * @param pRate percent of attributes to eliminate per iteration
   */
  public void setPercentToEliminatePerIteration(int pRate) {
    m_percentToEliminate = pRate;
  }

  /**
   * Get the percentage rate of attribute elimination per iteration
   * 
   * @return the percentage rate of attribute elimination per iteration
   */
  public int getPercentToEliminatePerIteration() {
    return m_percentToEliminate;
  }

  /**
   * Set the threshold below which percentage elimination reverts to constant
   * elimination.
   * 
   * @param pThresh percent of attributes to eliminate per iteration
   */
  public void setPercentThreshold(int pThresh) {
    m_percentThreshold = pThresh;
  }

  /**
   * Get the threshold below which percentage elimination reverts to constant
   * elimination.
   * 
   * @return the threshold below which percentage elimination stops
   */
  public int getPercentThreshold() {
    return m_percentThreshold;
  }

  /**
   * Set the value of P for SMO
   * 
   * @param svmP the value of P
   */
  public void setEpsilonParameter(double svmP) {
    m_smoPParameter = svmP;
  }

  /**
   * Get the value of P used with SMO
   * 
   * @return the value of P
   */
  public double getEpsilonParameter() {
    return m_smoPParameter;
  }

  /**
   * Set the value of T for SMO
   * 
   * @param svmT the value of T
   */
  public void setToleranceParameter(double svmT) {
    m_smoTParameter = svmT;
  }

  /**
   * Get the value of T used with SMO
   * 
   * @return the value of T
   */
  public double getToleranceParameter() {
    return m_smoTParameter;
  }

  /**
   * Set the value of C for SMO
   * 
   * @param svmC the value of C
   */
  public void setComplexityParameter(double svmC) {
    m_smoCParameter = svmC;
  }

  /**
   * Get the value of C used with SMO
   * 
   * @return the value of C
   */
  public double getComplexityParameter() {
    return m_smoCParameter;
  }

  /**
   * The filtering mode to pass to SMO
   * 
   * @param newType the new filtering mode
   */
  public void setFilterType(SelectedTag newType) {

    if (newType.getTags() == SMO.TAGS_FILTER) {
      m_smoFilterType = newType.getSelectedTag().getID();
    }
  }

  /**
   * Get the filtering mode passed to SMO
   * 
   * @return the filtering mode
   */
  public SelectedTag getFilterType() {

    return new SelectedTag(m_smoFilterType, SMO.TAGS_FILTER);
  }

  // ________________________________________________________________________

  /**
   * Returns the capabilities of this evaluator.
   * 
   * @return the capabilities of this evaluator
   * @see Capabilities
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result;

    result = new SMO().getCapabilities();

    result.setOwner(this);

    // only binary attributes are allowed, otherwise the NominalToBinary
    // filter inside SMO will increase the number of attributes which in turn
    // will lead to ArrayIndexOutOfBounds-Exceptions.
    result.disable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.BINARY_ATTRIBUTES);
    result.disableAllAttributeDependencies();

    return result;
  }

  /**
   * Initializes the evaluator.
   * 
   * @param data set of instances serving as training data
   * @throws Exception if the evaluator has not been generated successfully
   */
  @Override
  public void buildEvaluator(Instances data) throws Exception {
    // can evaluator handle data?
    getCapabilities().testWithFail(data);

    // System.out.println("Class attribute: " +
    // data.attribute(data.classIndex()).name());
    // Check settings
    m_numToEliminate = (m_numToEliminate > 1) ? m_numToEliminate : 1;
    m_percentToEliminate = (m_percentToEliminate < 100) ? m_percentToEliminate
      : 100;
    m_percentToEliminate = (m_percentToEliminate > 0) ? m_percentToEliminate
      : 0;
    m_percentThreshold = (m_percentThreshold < data.numAttributes()) ? m_percentThreshold
      : data.numAttributes() - 1;
    m_percentThreshold = (m_percentThreshold > 0) ? m_percentThreshold : 0;

    // Get ranked attributes for each class seperately, one-vs-all
    int[][] attScoresByClass;
    int numAttr = data.numAttributes() - 1;
    if (data.numClasses() > 2) {
      attScoresByClass = new int[data.numClasses()][numAttr];
      for (int i = 0; i < data.numClasses(); i++) {
        attScoresByClass[i] = rankBySVM(i, data);
      }
    } else {
      attScoresByClass = new int[1][numAttr];
      attScoresByClass[0] = rankBySVM(0, data);
    }

    // Cycle through class-specific ranked lists, poping top one off for each
    // class
    // and adding it to the overall ranked attribute list if it's not there
    // already
    ArrayList<Integer> ordered = new ArrayList<Integer>(numAttr);
    for (int i = 0; i < numAttr; i++) {
      for (int j = 0; j < (data.numClasses() > 2 ? data.numClasses() : 1); j++) {
        Integer rank = new Integer(attScoresByClass[j][i]);
        if (!ordered.contains(rank)) {
          ordered.add(rank);
        }
      }
    }
    m_attScores = new double[data.numAttributes()];
    Iterator<Integer> listIt = ordered.iterator();
    for (double i = numAttr; listIt.hasNext(); i = i - 1.0) {
      m_attScores[listIt.next().intValue()] = i;
    }
  }

  /**
   * Get SVM-ranked attribute indexes (best to worst) selected for the class
   * attribute indexed by classInd (one-vs-all).
   */
  private int[] rankBySVM(int classInd, Instances data) {
    // Holds a mapping into the original array of attribute indices
    int[] origIndices = new int[data.numAttributes()];
    for (int i = 0; i < origIndices.length; i++) {
      origIndices[i] = i;
    }

    // Count down of number of attributes remaining
    int numAttrLeft = data.numAttributes() - 1;
    // Ranked attribute indices for this class, one vs.all (highest->lowest)
    int[] attRanks = new int[numAttrLeft];

    try {
      MakeIndicator filter = new MakeIndicator();
      filter.setAttributeIndex("" + (data.classIndex() + 1));
      filter.setNumeric(false);
      filter.setValueIndex(classInd);
      filter.setInputFormat(data);
      Instances trainCopy = Filter.useFilter(data, filter);
      double pctToElim = (m_percentToEliminate) / 100.0;
      while (numAttrLeft > 0) {
        int numToElim;
        if (pctToElim > 0) {
          numToElim = (int) (trainCopy.numAttributes() * pctToElim);
          numToElim = (numToElim > 1) ? numToElim : 1;
          if (numAttrLeft - numToElim <= m_percentThreshold) {
            pctToElim = 0;
            numToElim = numAttrLeft - m_percentThreshold;
          }
        } else {
          numToElim = (numAttrLeft >= m_numToEliminate) ? m_numToEliminate
            : numAttrLeft;
        }

        // Build the linear SVM with default parameters
        SMO smo = new SMO();

        // SMO seems to get stuck if data not normalised when few attributes
        // remain
        // smo.setNormalizeData(numAttrLeft < 40);
        smo.setFilterType(new SelectedTag(m_smoFilterType, SMO.TAGS_FILTER));
        smo.setEpsilon(m_smoPParameter);
        smo.setToleranceParameter(m_smoTParameter);
        smo.setC(m_smoCParameter);
        smo.buildClassifier(trainCopy);

        // Find the attribute with maximum weight^2
        double[] weightsSparse = smo.sparseWeights()[0][1];
        int[] indicesSparse = smo.sparseIndices()[0][1];
        double[] weights = new double[trainCopy.numAttributes()];
        for (int j = 0; j < weightsSparse.length; j++) {
          weights[indicesSparse[j]] = weightsSparse[j] * weightsSparse[j];
        }
        weights[trainCopy.classIndex()] = Double.MAX_VALUE;
        int minWeightIndex;
        int[] featArray = new int[numToElim];
        boolean[] eliminated = new boolean[origIndices.length];
        for (int j = 0; j < numToElim; j++) {
          minWeightIndex = Utils.minIndex(weights);
          attRanks[--numAttrLeft] = origIndices[minWeightIndex];
          featArray[j] = minWeightIndex;
          eliminated[minWeightIndex] = true;
          weights[minWeightIndex] = Double.MAX_VALUE;
        }

        // Delete the worst attributes.
        weka.filters.unsupervised.attribute.Remove delTransform = new weka.filters.unsupervised.attribute.Remove();
        delTransform.setInvertSelection(false);
        delTransform.setAttributeIndicesArray(featArray);
        delTransform.setInputFormat(trainCopy);
        trainCopy = Filter.useFilter(trainCopy, delTransform);

        // Update the array of remaining attribute indices
        int[] temp = new int[origIndices.length - numToElim];
        int k = 0;
        for (int j = 0; j < origIndices.length; j++) {
          if (!eliminated[j]) {
            temp[k++] = origIndices[j];
          }
        }
        origIndices = temp;
      }
      // Carefully handle all exceptions
    } catch (Exception e) {
      e.printStackTrace();
    }
    return attRanks;
  }

  /**
   * Resets options to defaults.
   */
  protected void resetOptions() {
    m_attScores = null;
  }

  /**
   * Evaluates an attribute by returning the rank of the square of its
   * coefficient in a linear support vector machine.
   * 
   * @param attribute the index of the attribute to be evaluated
   * @throws Exception if the attribute could not be evaluated
   */
  @Override
  public double evaluateAttribute(int attribute) throws Exception {
    return m_attScores[attribute];
  }

  /**
   * Return a description of the evaluator
   * 
   * @return description as a string
   */
  @Override
  public String toString() {

    StringBuffer text = new StringBuffer();
    if (m_attScores == null) {
      text.append("\tSVM feature evaluator has not been built yet");
    } else {
      text.append("\tSVM feature evaluator");
    }

    text.append("\n");
    return text.toString();
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
   * @param args the options
   */
  public static void main(String[] args) {
    runEvaluator(new SVMAttributeEval(), args);
  }
}
