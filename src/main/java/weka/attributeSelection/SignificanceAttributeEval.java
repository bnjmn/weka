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
 *    SignificanceAttributeEval.java
 *    Copyright (C) 2009 Adrian Pino
 *    Copyright (C) 2009 University of Waikato, Hamilton, NZ
 *
 */
package weka.attributeSelection;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.filters.Filter;
import weka.filters.supervised.attribute.Discretize;

/**
 <!-- globalinfo-start -->
 * Significance :<br/>
 * <br/>
 * Evaluates the worth of an attribute by computing the Probabilistic Significance as a two-way function.<br/>
 * (attribute-classes and classes-attribute association)<br/>
 * <br/>
 * For more information see:<br/>
 * <br/>
 * Amir Ahmad, Lipika Dey (2004). A feature selection technique for classificatory analysis.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -M
 *  treat missing values as a separate value.</pre>
 * 
 <!-- options-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;phdthesis{Ahmad2004,
 *    author = {Amir Ahmad and Lipika Dey},
 *    month = {October},
 *    publisher = {ELSEVIER},
 *    title = {A feature selection technique for classificatory analysis},
 *    year = {2004}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 * @author Adrian Pino (apinoa@facinf.uho.edu.cu)
 * @version $Revision$
 */
public class SignificanceAttributeEval
extends ASEvaluation
implements AttributeEvaluator, OptionHandler, TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = -8504656625598579926L;

  /** The training instances */
  private Instances m_trainInstances;

  /** The class index */
  private int m_classIndex;

  /** The number of attributes */
  private int m_numAttribs;

  /** The number of instances */
  private int m_numInstances;

  /** The number of classes */
  private int m_numClasses;

  /** Merge missing values */
  private boolean m_missing_merge;

  /**
   * Returns a string describing this attribute evaluator
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Significance :\n\nEvaluates the worth of an attribute "
    +"by computing the Probabilistic Significance as a two-way function.\n"
    +"(atributte-classes and classes-atribute association)\n\n"
    + "For more information see:\n\n"
    + getTechnicalInformation().toString();
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing
   * detailed information about the technical background of this class,
   * e.g., paper reference or book this class is based on.
   *
   * @return the technical information about this class
   */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation        result;

    result = new TechnicalInformation(Type.PHDTHESIS);
    result.setValue(Field.AUTHOR, "Amir Ahmad and Lipika Dey");
    result.setValue(Field.YEAR, "2004");
    result.setValue(Field.MONTH, "October");
    result.setValue(Field.TITLE, "A feature selection technique for classificatory analysis");
    result.setValue(Field.PUBLISHER, "ELSEVIER");

    return result;
  }


  /**
   * Constructor
   */
  public SignificanceAttributeEval () {
    resetOptions();
  }


  /**
   * Returns an enumeration describing the available options.
   * @return an enumeration of all the available options.
   **/
  public Enumeration listOptions () {
    Vector newVector = new Vector(1);
    newVector.addElement(new Option("\ttreat missing values as a separate "
        + "value.", "M", 0, "-M"));
    return  newVector.elements();
  }


  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -M
   *  treat missing values as a separate value.</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   **/
  public void setOptions (String[] options)
  throws Exception {
    resetOptions();
    setMissingMerge(!(Utils.getFlag('M', options)));
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String missingMergeTipText() {
    return "Distribute counts for missing values. Counts are distributed "
    +"across other values in proportion to their frequency. Otherwise, "
    +"missing is treated as a separate value.";
  }

  /**
   * distribute the counts for missing values across observed values
   *
   * @param b true=distribute missing values.
   */
  public void setMissingMerge (boolean b) {
    m_missing_merge = b;
  }


  /**
   * get whether missing values are being distributed or not
   *
   * @return true if missing values are being distributed.
   */
  public boolean getMissingMerge () {
    return  m_missing_merge;
  }


  /**
   * Gets the current settings of WrapperSubsetEval.
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions () {
    String[] options = new String[1];
    int current = 0;

    if (!getMissingMerge()) {
      options[current++] = "-M";
    }

    while (current < options.length) {
      options[current++] = "";
    }

    return  options;
  }

  /**
   * Returns the capabilities of this evaluator.
   *
   * @return the capabilities of this evaluator
   * @see    Capabilities
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
    result.enable(Capability.MISSING_CLASS_VALUES);

    return result;
  }

  /**
   * Initializes the Significance attribute evaluator.
   * Discretizes all attributes that are numeric.
   *
   * @param data set of instances serving as training data
   * @throws Exception if the evaluator has not been
   * generated successfully
   */
  public void buildEvaluator (Instances data)
  throws Exception {

    // can evaluator handle data?
    getCapabilities().testWithFail(data);

    m_trainInstances = data;
    m_classIndex = m_trainInstances.classIndex();
    m_numAttribs = m_trainInstances.numAttributes();
    m_numInstances = m_trainInstances.numInstances();
    Discretize disTransform = new Discretize();
    disTransform.setUseBetterEncoding(true);
    disTransform.setInputFormat(m_trainInstances);
    m_trainInstances = Filter.useFilter(m_trainInstances, disTransform);
    m_numClasses = m_trainInstances.attribute(m_classIndex).numValues();
  }


  /**
   * reset options to default values
   */
  protected void resetOptions () {
    m_trainInstances = null;
    m_missing_merge = true;
  }


  /**
   * evaluates an individual attribute by measuring the Significance
   *
   * @param attribute the index of the attribute to be evaluated
   * @return the Significance of the attribute in the data base
   * @throws Exception if the attribute could not be evaluated
   */
  public double evaluateAttribute (int attribute)
  throws Exception {
    int i, j, ii, jj;
    int ni, nj;
    double sum = 0.0;
    ni = m_trainInstances.attribute(attribute).numValues() + 1;
    nj = m_numClasses + 1;
    double[] sumi, sumj;
    Instance inst;
    double temp = 0.0;
    sumi = new double[ni];
    sumj = new double[nj];
    double[][] counts = new double[ni][nj];

    for (i = 0; i < ni; i++) {
      sumi[i] = 0.0;

      for (j = 0; j < nj; j++) {
        sumj[j] = 0.0;
        counts[i][j] = 0.0;
      }
    }

    // Fill the contingency table
    for (i = 0; i < m_numInstances; i++) {
      inst = m_trainInstances.instance(i);

      if (inst.isMissing(attribute)) {
        ii = ni - 1;
      }
      else {
        ii = (int)inst.value(attribute);
      }

      if (inst.isMissing(m_classIndex)) {
        jj = nj - 1;
      }
      else {
        jj = (int)inst.value(m_classIndex);
      }

      counts[ii][jj]++;
    }

    // get the row totals
    for (i = 0; i < ni; i++) {
      sumi[i] = 0.0;

      for (j = 0; j < nj; j++) {
        sumi[i] += counts[i][j];
        sum += counts[i][j];
      }
    }

    // get the column totals
    for (j = 0; j < nj; j++) {
      sumj[j] = 0.0;

      for (i = 0; i < ni; i++) {
        sumj[j] += counts[i][j];
      }
    }


    // distribute missing counts
    if (m_missing_merge &&
        (sumi[ni-1] < m_numInstances) &&
        (sumj[nj-1] < m_numInstances)) {
      double[] i_copy = new double[sumi.length];
      double[] j_copy = new double[sumj.length];
      double[][] counts_copy = new double[sumi.length][sumj.length];

      for (i = 0; i < ni; i++) {
        System.arraycopy(counts[i], 0, counts_copy[i], 0, sumj.length);
      }

      System.arraycopy(sumi, 0, i_copy, 0, sumi.length);
      System.arraycopy(sumj, 0, j_copy, 0, sumj.length);
      double total_missing = (sumi[ni - 1] + sumj[nj - 1] -
          counts[ni - 1][nj - 1]);

      // do the missing i's
      if (sumi[ni - 1] > 0.0) {
        for (j = 0; j < nj - 1; j++) {
          if (counts[ni - 1][j] > 0.0) {
            for (i = 0; i < ni - 1; i++) {
              temp = ((i_copy[i]/(sum - i_copy[ni - 1]))*counts[ni - 1][j]);
              counts[i][j] += temp;
              sumi[i] += temp;
            }

            counts[ni - 1][j] = 0.0;
          }
        }
      }

      sumi[ni - 1] = 0.0;

      // do the missing j's
      if (sumj[nj - 1] > 0.0) {
        for (i = 0; i < ni - 1; i++) {
          if (counts[i][nj - 1] > 0.0) {
            for (j = 0; j < nj - 1; j++) {
              temp = ((j_copy[j]/(sum - j_copy[nj - 1]))*counts[i][nj - 1]);
              counts[i][j] += temp;
              sumj[j] += temp;
            }

            counts[i][nj - 1] = 0.0;
          }
        }
      }

      sumj[nj - 1] = 0.0;

      // do the both missing
      if (counts[ni - 1][nj - 1] > 0.0  && total_missing != sum) {
        for (i = 0; i < ni - 1; i++) {
          for (j = 0; j < nj - 1; j++) {
            temp = (counts_copy[i][j]/(sum - total_missing)) *
            counts_copy[ni - 1][nj - 1];
            counts[i][j] += temp;
            sumi[i] += temp;
            sumj[j] += temp;
          }
        }

        counts[ni - 1][nj - 1] = 0.0;
      }
    }

    /**Working on the ContingencyTables****/
    double discriminatingPower = associationAttributeClasses(counts);
    double separability = associationClassesAttribute(counts);
    /*...*/


    return  discriminatingPower + separability / 2;
  }

  /**
   * evaluates an individual attribute by measuring the attribute-classes
   * association
   *
   * @param counts the Contingency table where are the frecuency counts values
   * @return the discriminating power of the attribute
   */
  public double associationAttributeClasses(double[][] counts){

    List<Integer> supportSet = new ArrayList<Integer>();
    List<Integer> not_supportSet = new ArrayList<Integer>();

    double discriminatingPower = 0;


    int numValues = counts.length;
    int numClasses = counts[0].length;

    int total = 0;

    double[] sumRows = new double[numValues];
    double[] sumCols = new double[numClasses];

    // get the row totals
    for (int i = 0; i < numValues; i++) {
      sumRows[i] = 0.0;

      for (int j = 0; j < numClasses; j++) {
        sumRows[i] += counts[i][j];
        total += counts[i][j];
      }
    }

    // get the column totals
    for (int j = 0; j < numClasses; j++) {
      sumCols[j] = 0.0;

      for (int i = 0; i < numValues; i++) {
        sumCols[j] += counts[i][j];
      }
    }

    for (int i = 0; i < numClasses; i++) {
      for (int j = 0; j < numValues; j++) {

        //Computing Conditional Probability P(Clasei | Valuej)
        double numerator1 = counts[j][i];
        double denominator1 = sumRows[j];
        double result1;

        if(denominator1 != 0)
          result1 = numerator1/denominator1;
        else
          result1 = 0;

        //Computing Conditional Probability P(Clasei | ^Valuej)
        double numerator2 = sumCols[i] - counts[j][i];
        double denominator2 = total - sumRows[j];
        double result2;

        if(denominator2 != 0)
          result2 = numerator2/denominator2;
        else
          result2 = 0;


        if(result1 > result2){
          supportSet.add (i);
          discriminatingPower +=result1;
        }
        else{
          not_supportSet.add (i);
          discriminatingPower +=result2;
        }
      }

    }

    return discriminatingPower/numValues - 1.0;
  }

  /**
   * evaluates an individual attribute by measuring the classes-attribute
   * association
   *
   * @param counts the Contingency table where are the frecuency counts values
   * @return the separability power of the classes
   */
  public double associationClassesAttribute(double[][] counts){

    List<Integer> supportSet = new ArrayList<Integer>();
    List<Integer> not_supportSet = new ArrayList<Integer>();

    double separability = 0;


    int numValues = counts.length;
    int numClasses = counts[0].length;

    int total = 0;

    double[] sumRows = new double[numValues];
    double[] sumCols = new double[numClasses];

    // get the row totals
    for (int i = 0; i < numValues; i++) {
      sumRows[i] = 0.0;

      for (int j = 0; j < numClasses; j++) {
        sumRows[i] += counts[i][j];
        total += counts[i][j];
      }
    }

    // get the column totals
    for (int j = 0; j < numClasses; j++) {
      sumCols[j] = 0.0;

      for (int i = 0; i < numValues; i++) {
        sumCols[j] += counts[i][j];
      }
    }

    for (int i = 0; i < numValues; i++) {
      for (int j = 0; j < numClasses; j++) {

        //Computing Conditional Probability P(Valuei | Clasej)
        double numerator1 = counts[i][j];
        double denominator1 = sumCols[j];
        double result1;

        if(denominator1 != 0)
          result1 = numerator1/denominator1;
        else
          result1 = 0;

        //Computing Conditional Probability P(Valuei | ^Clasej)
        double numerator2 = sumRows[i] - counts[i][j];
        double denominator2 = total - sumCols[j];
        double result2;

        if(denominator2 != 0)
          result2 = numerator2/denominator2;
        else
          result2 = 0;


        if(result1 > result2){
          supportSet.add (i);
          separability +=result1;
        }
        else{
          not_supportSet.add (i);
          separability +=result2;
        }
      }

    }

    return separability/numClasses - 1.0;
  }


  /**
   * Return a description of the evaluator
   * @return description as a string
   */
  public String toString () {
    StringBuffer text = new StringBuffer();

    if (m_trainInstances == null) {
      text.append("\tSignificance evaluator has not been built");
    }
    else {
      text.append("\tSignificance feature evaluator");

      if (!m_missing_merge) {
        text.append("\n\tMissing values treated as seperate");
      }
    }

    text.append("\n");
    return  text.toString();
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
   * Main method for testing this class.
   *
   * @param args the options
   */
  public static void main (String[] args) {
    runEvaluator(new SignificanceAttributeEval(), args);
  }
}

