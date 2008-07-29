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
 *    Discriminative Multinomial Naive Bayes for Text Classification
 *    Copyright (C) 2008 Jiang Su
 */

package weka.classifiers.bayes;


import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.classifiers.UpdateableClassifier;
import java.util.*;
import java.io.Serializable;
import weka.core.Capabilities;
import weka.core.OptionHandler;


/**
 <!-- globalinfo-start -->
 * Class for building and using a Discriminative Multinomial Naive Bayes classifier. For more information see,<br/>
 * <br/>
 * Jiang Su,Harry Zhang,Charles X. Ling,Stan Matwin: Discriminative Parameter Learning for Bayesian Networks. In: ICML 2008', 2008.<br/>
 * <br/>
 * The core equation for this classifier:<br/>
 * <br/>
 * P[Ci|D] = (P[D|Ci] x P[Ci]) / P[D] (Bayes rule)<br/>
 * <br/>
 * where Ci is class i and D is a document.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{JiangSu2008,
 *    author = {Jiang Su,Harry Zhang,Charles X. Ling,Stan Matwin},
 *    booktitle = {ICML 2008'},
 *    title = {Discriminative Parameter Learning for Bayesian Networks},
 *    year = {2008}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
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
 * @author Jiang Su (Jiang.Su@unb.ca) 2008
 * @version $Revision: 1.3 $
 */
public class DMNBtext extends Classifier
    implements OptionHandler, WeightedInstancesHandler, 
               TechnicalInformationHandler, UpdateableClassifier {

  /** for serialization */
  static final long serialVersionUID = 5932177450183457085L;
  /** The number of iterations. */
  protected int m_NumIterations = 1;
  protected boolean m_BinaryWord = true;
  int m_numClasses=-1;
  protected Instances m_headerInfo;
  DNBBinary[] m_binaryClassifiers = null;

  /**
   * Returns a string describing this classifier
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return
      "Class for building and using a Discriminative Multinomial Naive Bayes classifier. "
      + "For more information see,\n\n"
      + getTechnicalInformation().toString() + "\n\n"
      + "The core equation for this classifier:\n\n"
      + "P[Ci|D] = (P[D|Ci] x P[Ci]) / P[D] (Bayes rule)\n\n"
      + "where Ci is class i and D is a document.";
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing
   * detailed information about the technical background of this class,
   * e.g., paper reference or book this class is based on.
   *
   * @return the technical information about this class
   */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation 	result;

    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "Jiang Su,Harry Zhang,Charles X. Ling,Stan Matwin");
    result.setValue(Field.YEAR, "2008");
    result.setValue(Field.TITLE, "Discriminative Parameter Learning for Bayesian Networks");
    result.setValue(Field.BOOKTITLE, "ICML 2008'");

    return result;
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capability.NUMERIC_ATTRIBUTES);

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    return result;
  }

  /**
   * Generates the classifier.
   *
   * @param instances set of instances serving as training data
   * @exception Exception if the classifier has not been generated successfully
   */
  public void buildClassifier(Instances data) throws Exception {
    // can classifier handle the data?
    getCapabilities().testWithFail(data);
    // remove instances with missing class
    Instances instances =  new Instances(data);
    instances.deleteWithMissingClass();

    m_binaryClassifiers = new DNBBinary[instances.numClasses()];
    m_numClasses=instances.numClasses();
    m_headerInfo = new Instances(instances, 0);
    for (int i = 0; i < instances.numClasses(); i++) {
      m_binaryClassifiers[i] = new DNBBinary();
      m_binaryClassifiers[i].setTargetClass(i);
      m_binaryClassifiers[i].initClassifier(instances);
    }

    if (instances.numInstances() == 0)
      return;
    //Iterative update
    Random random = new Random();
    for (int it = 0; it < m_NumIterations; it++) {
      for (int i = 0; i < instances.numInstances(); i++) {
        updateClassifier(instances.instance(i));
      }
    }

    //  Utils.normalize(m_oldClassDis);
    // Utils.normalize(m_ClassDis);
    // m_originalPositive = m_oldClassDis[0];
    //   m_positive = m_ClassDis[0];

  }

  /**
   * Updates the classifier with the given instance.
   *
   * @param instance the new training instance to include in the model
   * @exception Exception if the instance could not be incorporated in
   * the model.
   */

  public void updateClassifier(Instance instance) throws Exception {

    if (m_numClasses == 2) {
      m_binaryClassifiers[0].updateClassifier(instance);
    } else {
      for (int i = 0; i < instance.numClasses(); i++)
        m_binaryClassifiers[i].updateClassifier(instance);
    }
  }

  /**
   * Calculates the class membership probabilities for the given test
   * instance.
   *
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @exception Exception if there is a problem generating the prediction
   */
  public double[] distributionForInstance(Instance instance) throws Exception {
    if (m_numClasses == 2) {
      // System.out.println(m_binaryClassifiers[0].getProbForTargetClass(instance));
      return m_binaryClassifiers[0].distributionForInstance(instance);
    }
    double[] logDocGivenClass = new double[instance.numClasses()];
    for (int i = 0; i < m_numClasses; i++)
      logDocGivenClass[i] = m_binaryClassifiers[i].getLogProbForTargetClass(instance);


    double max = logDocGivenClass[Utils.maxIndex(logDocGivenClass)];
    for(int i = 0; i<m_numClasses; i++)
      logDocGivenClass[i] = Math.exp(logDocGivenClass[i] - max);


    try {
      Utils.normalize(logDocGivenClass);
    } catch (Exception e) {
      e.printStackTrace();


    }

    return logDocGivenClass;
  }
  /**
   * Returns a string representation of the classifier.
   *
   * @return a string representation of the classifier
   */
  public String toString() {
    StringBuffer result = new StringBuffer("");
    result.append("The log ratio of two conditional probabilities of a word w_i: log(p(w_i)|+)/p(w_i)|-)) in decent order based on their absolute values\n");
    result.append("Can be used to measure the discriminative power of each word.\n");
    if (m_numClasses == 2) {
      // System.out.println(m_binaryClassifiers[0].getProbForTargetClass(instance));
      return result.append(m_binaryClassifiers[0].toString()).toString();
    }
    for (int i = 0; i < m_numClasses; i++)
      { result.append(i+" against the rest classes\n");
        result.append(m_binaryClassifiers[i].toString()+"\n");
      }
    return result.toString();
  }

  /*
   * Options after -- are passed to the designated classifier.<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String iterations = Utils.getOption('I', options);
    if (iterations.length() != 0) {
      setNumIterations(Integer.parseInt(iterations));
    } else {
      setNumIterations(m_NumIterations);
    }
    iterations = Utils.getOption('B', options);
    if (iterations.length() != 0) {
      setBinaryWord(Boolean.parseBoolean(iterations));
    } else {
      setBinaryWord(m_BinaryWord);
    }

  }

  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {

    String[] options = new String[4];

    int current = 0;
    options[current++] = "-I";
    options[current++] = "" + getNumIterations();

    options[current++] = "-B";
    options[current++] = "" + getBinaryWord();

    return options;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numIterationsTipText() {
    return "The number of iterations that the classifier will scan the training data";
  }

  /**
   * Sets the number of iterations to be performed
   */
  public void setNumIterations(int numIterations) {

    m_NumIterations = numIterations;
  }

  /**
   * Gets the number of iterations to be performed
   *
   * @return the iterations to be performed
   */
  public int getNumIterations() {

    return m_NumIterations;
  }
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String binaryWordTipText() {
    return " whether ingore the frequency information in data";
  }
  /**
   * Sets whether use binary text representation
   */
  public void setBinaryWord(boolean val) {

    m_BinaryWord = val;
  }

  /**
   * Gets whether use binary text representation
   *
   * @return whether use binary text representation
   */
  public boolean getBinaryWord() {

    return m_BinaryWord;
  }

  /**
   * Returns the revision string.
   *
   * @return		the revision
   */
  public String getRevision() {
    return "$Revision: 1.0";
  }

  public class DNBBinary implements Serializable {

    /** The number of iterations. */
    private double[][] m_perWordPerClass;
    private double[] m_wordsPerClass;
    int m_classIndex = -1;
    private double[] m_classDistribution;
    /** number of unique words */
    private int m_numAttributes;
    //set the target class
    private int m_targetClass = -1;

    private double m_WordLaplace=1;

    private double[] m_coefficient;
    private double m_classRatio;
    private double m_wordRatio;

    public void initClassifier(Instances instances) throws Exception {
      m_numAttributes = instances.numAttributes();
      m_perWordPerClass = new double[2][m_numAttributes];
      m_coefficient = new double[m_numAttributes];
      m_wordsPerClass = new double[2];
      m_classDistribution = new double[2];
      m_WordLaplace = Math.log(m_numAttributes);
      m_classIndex = instances.classIndex();

      //Laplace
      for (int c = 0; c < 2; c++) {
        m_classDistribution[c] = 1;
        m_wordsPerClass[c] = m_WordLaplace * m_numAttributes;
        java.util.Arrays.fill(m_perWordPerClass[c], m_WordLaplace);
      }

    }

    public void updateClassifier(Instance ins) throws
      Exception {
      //c=0 is 1, which is the target class, and c=1 is the rest
      int classIndex = 0;
      if (ins.value(ins.classIndex()) != m_targetClass)
        classIndex = 1;
      double prob = 1 -
        distributionForInstance(ins)[classIndex];


      double weight = prob * ins.weight();

      for (int a = 0; a < ins.numValues(); a++) {
        if (ins.index(a) != m_classIndex )
          {

            if (m_BinaryWord) {
              if (ins.valueSparse(a) > 0) {
                m_wordsPerClass[classIndex] +=
                  weight;
                m_perWordPerClass[classIndex][ins.
                                              index(a)] +=
                  weight;
              }
            } else {
              double t = ins.valueSparse(a) * weight;
              m_wordsPerClass[classIndex] += t;
              m_perWordPerClass[classIndex][ins.index(a)] += t;
            }
            //update coefficient
            m_coefficient[ins.index(a)] = Math.log(m_perWordPerClass[0][
                                                                        ins.index(a)] /
                                                   m_perWordPerClass[1][ins.index(a)]);
          }
      }
      m_wordRatio = Math.log(m_wordsPerClass[0] / m_wordsPerClass[1]);
      m_classDistribution[classIndex] += weight;
      m_classRatio = Math.log(m_classDistribution[0] /
                              m_classDistribution[1]);
    }


    /**
     * Calculates the class membership probabilities for the given test
     * instance.
     *
     * @param instance the instance to be classified
     * @return predicted class probability distribution
     * @exception Exception if there is a problem generating the prediction
     */
    public double getLogProbForTargetClass(Instance ins) throws Exception {

      double probLog = m_classRatio;
      for (int a = 0; a < ins.numValues(); a++) {
        if (ins.index(a) != m_classIndex )
          {

            if (m_BinaryWord) {
              if (ins.valueSparse(a) > 0) {
                probLog += m_coefficient[ins.index(a)] -
                  m_wordRatio;
              }
            } else {
              probLog += ins.valueSparse(a) *
                (m_coefficient[ins.index(a)] - m_wordRatio);
            }
          }
      }
      return probLog;
    }

    /**
     * Calculates the class membership probabilities for the given test
     * instance.
     *
     * @param instance the instance to be classified
     * @return predicted class probability distribution
     * @exception Exception if there is a problem generating the prediction
     */
    public double[] distributionForInstance(Instance instance) throws
      Exception {
      double[] probOfClassGivenDoc = new double[2];
      double ratio=getLogProbForTargetClass(instance);
      if (ratio > 709)
        probOfClassGivenDoc[0]=1;
      else
        {
          ratio = Math.exp(ratio);
          probOfClassGivenDoc[0]=ratio / (1 + ratio);
        }

      probOfClassGivenDoc[1] = 1 - probOfClassGivenDoc[0];
      return probOfClassGivenDoc;
    }

    /**
     * Returns a string representation of the classifier.
     *
     * @return a string representation of the classifier
     */
    public String toString() {
      //            StringBuffer result = new StringBuffer("The cofficiency of a naive Bayes classifier, can be considered as the discriminative power of a word\n--------------------------------------\n");
      StringBuffer result = new StringBuffer();

      result.append("\n");
      TreeMap sort=new TreeMap();
      double[] absCoeff=new double[m_numAttributes];
      for(int w = 0; w<m_numAttributes; w++)
        {
          if(w==m_headerInfo.classIndex())continue;
          String val= m_headerInfo.attribute(w).name()+": "+m_coefficient[w];
          sort.put((-1)*Math.abs(m_coefficient[w]),val);
        }
      Iterator it=sort.values().iterator();
      while(it.hasNext())
        {
          result.append((String)it.next());
          result.append("\n");
        }

      return result.toString();
    }

    /**
     * Sets the Target Class
     */
    public void setTargetClass(int targetClass) {

      m_targetClass = targetClass;
    }

    /**
     * Gets the Target Class
     *
     * @return the Target Class Index
     */
    public int getTargetClass() {

      return m_targetClass;
    }

  }


  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String[] argv) {

    DMNBtext c = new DMNBtext();

    runClassifier(c, argv);
  }
}

