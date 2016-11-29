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
 * LibSVM.java
 * Copyright (C) 2005 Yasser EL-Manzalawy (original code)
 * Copyright (C) 2005-16 University of Waikato, Hamilton, NZ (adapted code)
 * 
 */

package weka.classifiers.functions;

import libsvm.*;
import weka.classifiers.RandomizableClassifier;
import weka.core.*;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Type;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NominalToBinary;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

import java.io.File;
import java.util.*;

/** 
 <!-- globalinfo-start -->
 * A wrapper class for the libsvm library. This wrapper supports the classifiers implemented in the libsvm
 * library, including one-class SVMs.<br>
 * Note: To be consistent with other SVMs in WEKA, the target attribute is now normalized before "
 * SVM regression is performed, if normalization is turned on.<br>
 * <br>
 * Yasser EL-Manzalawy (2005). WLSVM. URL http://www.cs.iastate.edu/~yasser/wlsvm/.<br/>
 * <br>
 * Chih-Chung Chang, Chih-Jen Lin (2001). LIBSVM - A Library for Support Vector Machines. URL http://www.csie.ntu.edu.tw/~cjlin/libsvm/.
 * <p>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;misc{EL-Manzalawy2005,
 *    author = {Yasser EL-Manzalawy},
 *    note = {You don't need to include the WLSVM package in the CLASSPATH},
 *    title = {WLSVM},
 *    year = {2005},
 *    URL = {http://www.cs.iastate.edu/\~yasser/wlsvm/}
 * }
 * 
 * &#64;misc{Chang2001,
 *    author = {Chih-Chung Chang and Chih-Jen Lin},
 *    note = {The Weka classifier works with version 2.82 of LIBSVM},
 *    title = {LIBSVM - A Library for Support Vector Machines},
 *    year = {2001},
 *    URL = {http://www.csie.ntu.edu.tw/\~cjlin/libsvm/}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -S &lt;int&gt;
 *  Set type of SVM (default: 0)
 *    0 = C-SVC
 *    1 = nu-SVC
 *    2 = one-class SVM
 *    3 = epsilon-SVR
 *    4 = nu-SVR</pre>
 * 
 * <pre> -K &lt;int&gt;
 *  Set type of kernel function (default: 2)
 *    0 = linear: u'*v
 *    1 = polynomial: (gamma*u'*v + coef0)^degree
 *    2 = radial basis function: exp(-gamma*|u-v|^2)
 *    3 = sigmoid: tanh(gamma*u'*v + coef0)</pre>
 * 
 * <pre> -D &lt;int&gt;
 *  Set degree in kernel function (default: 3)</pre>
 * 
 * <pre> -G &lt;double&gt;
 *  Set gamma in kernel function (default: 1/k)</pre>
 * 
 * <pre> -R &lt;double&gt;
 *  Set coef0 in kernel function (default: 0)</pre>
 * 
 * <pre> -C &lt;double&gt;
 *  Set the parameter C of C-SVC, epsilon-SVR, and nu-SVR
 *   (default: 1)</pre>
 * 
 * <pre> -N &lt;double&gt;
 *  Set the parameter nu of nu-SVC, one-class SVM, and nu-SVR
 *   (default: 0.5)</pre>
 * 
 * <pre> -Z
 *  Turns on normalization of input data (default: off)</pre>
 * 
 * <pre> -J
 *  Turn off nominal to binary conversion.
 *  WARNING: use only if your data is all numeric!</pre>
 * 
 * <pre> -V
 *  Turn off missing value replacement.
 *  WARNING: use only if your data has no missing values.</pre>
 * 
 * <pre> -P &lt;double&gt;
 *  Set the epsilon in loss function of epsilon-SVR (default: 0.1)</pre>
 * 
 * <pre> -M &lt;double&gt;
 *  Set cache memory size in MB (default: 40)</pre>
 * 
 * <pre> -E &lt;double&gt;
 *  Set tolerance of termination criterion (default: 0.001)</pre>
 * 
 * <pre> -H
 *  Turns the shrinking heuristics off (default: on)</pre>
 * 
 * <pre> -W &lt;double&gt;
 *  Set the parameters C of class i to weight[i]*C, for C-SVC.
 *  E.g., for a 3-class problem, you could use "1 1 1" for equally
 *  weighted classes.
 *  (default: 1 for all classes)</pre>
 * 
 * <pre> -B
 *  Trains a SVC model instead of a SVR one (default: SVR)</pre>
 * 
 * <pre> -model &lt;file&gt;
 *  Specifies the filename to save the libsvm-internal model to.
 *  Gets ignored if a directory is provided.</pre>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 *
 * <pre> -seed &lt;num&gt;
 *  Seed for the random number generator when -B is used.
 *  (default = 1)</pre>
 *
 <!-- options-end -->
 *
 * @author  Yasser EL-Manzalawy
 * @author  FracPete (fracpete at waikato dot ac dot nz)
 * @author  Eibe Frank
 * @version $Revision$
 * @see     weka.core.converters.LibSVMLoader
 * @see     weka.core.converters.LibSVMSaver
 */
public class LibSVM 
  extends RandomizableClassifier
  implements TechnicalInformationHandler {

  /**
   * serial UID.
   */
  protected static final long serialVersionUID = 14172;

  /**
   * LibSVM Model.
   */
  protected svm_model m_Model;

  /**
   * for normalizing the data.
   */
  protected Filter m_Filter = null;

  /**
   * for converting mult-valued nominal attributes to binary
   */
  protected Filter m_NominalToBinary;

  /**
   * The filter used to get rid of missing values.
   */
  protected ReplaceMissingValues m_ReplaceMissingValues;

  /**
   * normalize input data.
   */
  protected boolean m_Normalize = false;

  /**
   * coefficients used by normalization filter for doing its linear transformation
   **/
  protected double m_x1 = 1.0;
  protected double m_x0 = 0.0;

  /**
   * If true, the replace missing values filter is not applied.
   */
  private boolean m_noReplaceMissingValues;

  /**
   * SVM type C-SVC (classification).
   */
  public static final int SVMTYPE_C_SVC = 0;
  /**
   * SVM type nu-SVC (classification).
   */
  public static final int SVMTYPE_NU_SVC = 1;
  /**
   * SVM type one-class SVM (classification).
   */
  public static final int SVMTYPE_ONE_CLASS_SVM = 2;
  /**
   * SVM type epsilon-SVR (regression).
   */
  public static final int SVMTYPE_EPSILON_SVR = 3;
  /**
   * SVM type nu-SVR (regression).
   */
  public static final int SVMTYPE_NU_SVR = 4;
  /**
   * SVM types.
   */
  public static final Tag[] TAGS_SVMTYPE = {
          new Tag(SVMTYPE_C_SVC, "C-SVC (classification)"),
          new Tag(SVMTYPE_NU_SVC, "nu-SVC (classification)"),
          new Tag(SVMTYPE_ONE_CLASS_SVM, "one-class SVM (classification)"),
          new Tag(SVMTYPE_EPSILON_SVR, "epsilon-SVR (regression)"),
          new Tag(SVMTYPE_NU_SVR, "nu-SVR (regression)")
  };

  /**
   * the SVM type.
   */
  protected int m_SVMType = SVMTYPE_C_SVC;

  /**
   * kernel type linear: u'*v.
   */
  public static final int KERNELTYPE_LINEAR = 0;
  /**
   * kernel type polynomial: (gamma*u'*v + coef0)^degree.
   */
  public static final int KERNELTYPE_POLYNOMIAL = 1;
  /**
   * kernel type radial basis function: exp(-gamma*|u-v|^2).
   */
  public static final int KERNELTYPE_RBF = 2;
  /**
   * kernel type sigmoid: tanh(gamma*u'*v + coef0).
   */
  public static final int KERNELTYPE_SIGMOID = 3;
  /**
   * the different kernel types.
   */
  public static final Tag[] TAGS_KERNELTYPE = {
          new Tag(KERNELTYPE_LINEAR, "linear: u'*v"),
          new Tag(KERNELTYPE_POLYNOMIAL, "polynomial: (gamma*u'*v + coef0)^degree"),
          new Tag(KERNELTYPE_RBF, "radial basis function: exp(-gamma*|u-v|^2)"),
          new Tag(KERNELTYPE_SIGMOID, "sigmoid: tanh(gamma*u'*v + coef0)")
  };

  /**
   * the kernel type.
   */
  protected int m_KernelType = KERNELTYPE_RBF;

  /**
   * for poly - in older versions of libsvm declared as a double.
   * At least since 2.82 it is an int.
   */
  protected int m_Degree = 3;

  /**
   * for poly/rbf/sigmoid.
   */
  protected double m_Gamma = 0;

  /**
   * for poly/rbf/sigmoid (the actual gamma).
   */
  protected double m_GammaActual = 0;

  /**
   * for poly/sigmoid.
   */
  protected double m_Coef0 = 0;

  /**
   * in MB.
   */
  protected double m_CacheSize = 40;

  /**
   * stopping criteria.
   */
  protected double m_eps = 1e-3;

  /**
   * cost, for C_SVC, EPSILON_SVR and NU_SVR.
   */
  protected double m_Cost = 1;

  /**
   * for C_SVC.
   */
  protected int[] m_WeightLabel = new int[0];

  /**
   * for C_SVC.
   */
  protected double[] m_Weight = new double[0];

  /**
   * for NU_SVC, ONE_CLASS, and NU_SVR.
   */
  protected double m_nu = 0.5;

  /**
   * loss, for EPSILON_SVR.
   */
  protected double m_Loss = 0.1;

  /**
   * use the shrinking heuristics.
   */
  protected boolean m_Shrinking = true;

  /**
   * whether to generate probability estimates instead of +1/-1 in case of
   * classification problems.
   */
  protected boolean m_ProbabilityEstimates = false;

  /**
   * the file to save the libsvm-internal model to.
   */
  protected File m_ModelFile = new File(System.getProperty("user.dir"));

  /**
   * Returns a string describing classifier.
   *
   * @return a description suitable for displaying in the
   * explorer/experimenter gui
   */
  public String globalInfo() {
    return "A wrapper class for the libsvm library. This wrapper supports the classifiers implemented in the libsvm "
            + "library, including one-class SVMs.\n"
            + "Note: To be consistent with other SVMs in WEKA, the target attribute is now normalized before "
            + "SVM regression is performed, if normalization is turned on.\n"
            + "\n"
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
    TechnicalInformation result;
    TechnicalInformation additional;

    result = new TechnicalInformation(Type.MISC);
    result.setValue(TechnicalInformation.Field.AUTHOR, "Yasser EL-Manzalawy");
    result.setValue(TechnicalInformation.Field.YEAR, "2005");
    result.setValue(TechnicalInformation.Field.TITLE, "WLSVM");
    result.setValue(TechnicalInformation.Field.NOTE, "LibSVM was originally developed as 'WLSVM'");
    result.setValue(TechnicalInformation.Field.URL, "http://www.cs.iastate.edu/~yasser/wlsvm/");
    result.setValue(TechnicalInformation.Field.NOTE, "You don't need to include the WLSVM package in the CLASSPATH");

    additional = result.add(Type.MISC);
    additional.setValue(TechnicalInformation.Field.AUTHOR, "Chih-Chung Chang and Chih-Jen Lin");
    additional.setValue(TechnicalInformation.Field.TITLE, "LIBSVM - A Library for Support Vector Machines");
    additional.setValue(TechnicalInformation.Field.YEAR, "2001");
    additional.setValue(TechnicalInformation.Field.URL, "http://www.csie.ntu.edu.tw/~cjlin/libsvm/");
    additional.setValue(TechnicalInformation.Field.NOTE, "The Weka classifier works with version 2.82 of LIBSVM");

    return result;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector result;

    result = new Vector();

    result.addElement(
            new Option(
                    "\tSet type of SVM (default: 0)\n"
                            + "\t\t 0 = C-SVC\n"
                            + "\t\t 1 = nu-SVC\n"
                            + "\t\t 2 = one-class SVM\n"
                            + "\t\t 3 = epsilon-SVR\n"
                            + "\t\t 4 = nu-SVR",
                    "S", 1, "-S <int>"));

    result.addElement(
            new Option(
                    "\tSet type of kernel function (default: 2)\n"
                            + "\t\t 0 = linear: u'*v\n"
                            + "\t\t 1 = polynomial: (gamma*u'*v + coef0)^degree\n"
                            + "\t\t 2 = radial basis function: exp(-gamma*|u-v|^2)\n"
                            + "\t\t 3 = sigmoid: tanh(gamma*u'*v + coef0)",
                    "K", 1, "-K <int>"));

    result.addElement(
            new Option(
                    "\tSet degree in kernel function (default: 3)",
                    "D", 1, "-D <int>"));

    result.addElement(
            new Option(
                    "\tSet gamma in kernel function (default: 1/k)",
                    "G", 1, "-G <double>"));

    result.addElement(
            new Option(
                    "\tSet coef0 in kernel function (default: 0)",
                    "R", 1, "-R <double>"));

    result.addElement(
            new Option(
                    "\tSet the parameter C of C-SVC, epsilon-SVR, and nu-SVR\n"
                            + "\t (default: 1)",
                    "C", 1, "-C <double>"));

    result.addElement(
            new Option(
                    "\tSet the parameter nu of nu-SVC, one-class SVM, and nu-SVR\n"
                            + "\t (default: 0.5)",
                    "N", 1, "-N <double>"));

    result.addElement(
            new Option(
                    "\tTurns on normalization of input data (default: off)",
                    "Z", 0, "-Z"));

    result.addElement(
            new Option("\tTurn off nominal to binary conversion."
                    + "\n\tWARNING: use only if your data is all numeric!",
                    "J", 0, "-J"));

    result.addElement(
            new Option("\tTurn off missing value replacement."
                    + "\n\tWARNING: use only if your data has no missing "
                    + "values.", "V", 0, "-V"));

    result.addElement(
            new Option(
                    "\tSet the epsilon in loss function of epsilon-SVR (default: 0.1)",
                    "P", 1, "-P <double>"));

    result.addElement(
            new Option(
                    "\tSet cache memory size in MB (default: 40)",
                    "M", 1, "-M <double>"));

    result.addElement(
            new Option(
                    "\tSet tolerance of termination criterion (default: 0.001)",
                    "E", 1, "-E <double>"));

    result.addElement(
            new Option(
                    "\tTurns the shrinking heuristics off (default: on)",
                    "H", 0, "-H"));

    result.addElement(
            new Option(
                    "\tSet the parameters C of class i to weight[i]*C, for C-SVC.\n"
                            + "\tE.g., for a 3-class problem, you could use \"1 1 1\" for equally\n"
                            + "\tweighted classes.\n"
                            + "\t(default: 1 for all classes)",
                    "W", 1, "-W <double>"));

    result.addElement(
            new Option(
                    "\tGenerate probability estimates for classification",
                    "B", 0, "-B"));

    result.addElement(
            new Option(
                    "\tSpecifies the filename to save the libsvm-internal model to.\n"
                            + "\tGets ignored if a directory is provided.",
                    "model", 1, "-model <file>"));

    result.addElement(
            new Option("\tSeed for the random number generator when -B is used.\n\t(default = 1)", "seed", 1, "-seed <num>"));

    Enumeration en = super.listOptions();
    while (en.hasMoreElements()) {
      Option op = (Option) en.nextElement();
      if (!op.name().equals("S")) {
        result.addElement(op); // Need to skip -S flag from RandomizableClassifier
      }
    }

    return result.elements();
  }

  /**
   * Sets the classifier options <p/>
   * <p/>
   * <!-- options-start -->
   * Valid options are: <p/>
   * <p/>
   * <pre> -S &lt;int&gt;
   *  Set type of SVM (default: 0)
   *    0 = C-SVC
   *    1 = nu-SVC
   *    2 = one-class SVM
   *    3 = epsilon-SVR
   *    4 = nu-SVR</pre>
   * <p/>
   * <pre> -K &lt;int&gt;
   *  Set type of kernel function (default: 2)
   *    0 = linear: u'*v
   *    1 = polynomial: (gamma*u'*v + coef0)^degree
   *    2 = radial basis function: exp(-gamma*|u-v|^2)
   *    3 = sigmoid: tanh(gamma*u'*v + coef0)</pre>
   * <p/>
   * <pre> -D &lt;int&gt;
   *  Set degree in kernel function (default: 3)</pre>
   * <p/>
   * <pre> -G &lt;double&gt;
   *  Set gamma in kernel function (default: 1/k)</pre>
   * <p/>
   * <pre> -R &lt;double&gt;
   *  Set coef0 in kernel function (default: 0)</pre>
   * <p/>
   * <pre> -C &lt;double&gt;
   *  Set the parameter C of C-SVC, epsilon-SVR, and nu-SVR
   *   (default: 1)</pre>
   * <p/>
   * <pre> -N &lt;double&gt;
   *  Set the parameter nu of nu-SVC, one-class SVM, and nu-SVR
   *   (default: 0.5)</pre>
   * <p/>
   * <pre> -Z
   *  Turns on normalization of input data (default: off)</pre>
   * <p/>
   * <pre> -J
   *  Turn off nominal to binary conversion.
   *  WARNING: use only if your data is all numeric!</pre>
   * <p/>
   * <pre> -V
   *  Turn off missing value replacement.
   *  WARNING: use only if your data has no missing values.</pre>
   * <p/>
   * <pre> -P &lt;double&gt;
   *  Set the epsilon in loss function of epsilon-SVR (default: 0.1)</pre>
   * <p/>
   * <pre> -M &lt;double&gt;
   *  Set cache memory size in MB (default: 40)</pre>
   * <p/>
   * <pre> -E &lt;double&gt;
   *  Set tolerance of termination criterion (default: 0.001)</pre>
   * <p/>
   * <pre> -H
   *  Turns the shrinking heuristics off (default: on)</pre>
   * <p/>
   * <pre> -W &lt;double&gt;
   *  Set the parameters C of class i to weight[i]*C, for C-SVC.
   *  E.g., for a 3-class problem, you could use "1 1 1" for equally
   *  weighted classes.
   *  (default: 1 for all classes)</pre>
   * <p/>
   * <pre> -B
   *  Trains a SVC model instead of a SVR one (default: SVR)</pre>
   * <p/>
   * <pre> -model &lt;file&gt;
   *  Specifies the filename to save the libsvm-internal model to.
   *  Gets ignored if a directory is provided.</pre>
   * <p/>
   * <pre> -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * <p/>
   * <pre> -seed &lt;num&gt;
   *  Seed for the random number generator when -B is used.
   *  (default = 1)</pre>
   * <p/>
   * <!-- options-end -->
   *
   * @param options the options to parse
   * @throws Exception if parsing fails
   */
  public void setOptions(String[] options) throws Exception {
    String tmpStr;

    tmpStr = Utils.getOption('S', options);
    if (tmpStr.length() != 0)
      setSVMType(
              new SelectedTag(Integer.parseInt(tmpStr), TAGS_SVMTYPE));
    else
      setSVMType(
              new SelectedTag(SVMTYPE_C_SVC, TAGS_SVMTYPE));

    tmpStr = Utils.getOption('K', options);
    if (tmpStr.length() != 0)
      setKernelType(
              new SelectedTag(Integer.parseInt(tmpStr), TAGS_KERNELTYPE));
    else
      setKernelType(
              new SelectedTag(KERNELTYPE_RBF, TAGS_KERNELTYPE));

    tmpStr = Utils.getOption('D', options);
    if (tmpStr.length() != 0)
      setDegree(Integer.parseInt(tmpStr));
    else
      setDegree(3);

    tmpStr = Utils.getOption('G', options);
    if (tmpStr.length() != 0)
      setGamma(Double.parseDouble(tmpStr));
    else
      setGamma(0);

    tmpStr = Utils.getOption('R', options);
    if (tmpStr.length() != 0)
      setCoef0(Double.parseDouble(tmpStr));
    else
      setCoef0(0);

    tmpStr = Utils.getOption('N', options);
    if (tmpStr.length() != 0)
      setNu(Double.parseDouble(tmpStr));
    else
      setNu(0.5);

    tmpStr = Utils.getOption('M', options);
    if (tmpStr.length() != 0)
      setCacheSize(Double.parseDouble(tmpStr));
    else
      setCacheSize(40);

    tmpStr = Utils.getOption('C', options);
    if (tmpStr.length() != 0)
      setCost(Double.parseDouble(tmpStr));
    else
      setCost(1);

    tmpStr = Utils.getOption('E', options);
    if (tmpStr.length() != 0)
      setEps(Double.parseDouble(tmpStr));
    else
      setEps(1e-3);

    setNormalize(Utils.getFlag('Z', options));

    setDoNotReplaceMissingValues(Utils.getFlag("V", options));

    tmpStr = Utils.getOption('P', options);
    if (tmpStr.length() != 0)
      setLoss(Double.parseDouble(tmpStr));
    else
      setLoss(0.1);

    setShrinking(!Utils.getFlag('H', options));

    setWeights(Utils.getOption('W', options));

    setProbabilityEstimates(Utils.getFlag('B', options));

    tmpStr = Utils.getOption("model", options);
    if (tmpStr.length() == 0)
      m_ModelFile = new File(System.getProperty("user.dir"));
    else
      m_ModelFile = new File(tmpStr);

    String seedString = Utils.getOption("seed", options);
    if (seedString.length() > 0) {
      setSeed(Integer.parseInt(seedString.trim()));
    }

    if (Utils.getOption('S', options).length() != 0) {
      throw new IllegalArgumentException("Cannot use -S option twice in LibSVM. Use -seed to specify the seed " +
              "for the random number generator.");
    }

    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Returns the current options.
   *
   * @return the current setup
   */
  public String[] getOptions() {
    Vector result;

    result = new Vector();

    result.add("-S");
    result.add("" + m_SVMType);

    result.add("-K");
    result.add("" + m_KernelType);

    result.add("-D");
    result.add("" + getDegree());

    result.add("-G");
    result.add("" + getGamma());

    result.add("-R");
    result.add("" + getCoef0());

    result.add("-N");
    result.add("" + getNu());

    result.add("-M");
    result.add("" + getCacheSize());

    result.add("-C");
    result.add("" + getCost());

    result.add("-E");
    result.add("" + getEps());

    result.add("-P");
    result.add("" + getLoss());

    if (!getShrinking())
      result.add("-H");

    if (getNormalize())
      result.add("-Z");

    if (getDoNotReplaceMissingValues())
      result.add("-V");

    if (getWeights().length() != 0) {
      result.add("-W");
      result.add("" + getWeights());
    }

    if (getProbabilityEstimates())
      result.add("-B");

    result.add("-model");
    result.add(m_ModelFile.getAbsolutePath());

    result.add("-seed");
    result.add("" + getSeed());

    Vector<String> classifierOptions = new Vector<String>();
    Collections.addAll(classifierOptions, super.getOptions());
    Option.deleteOptionString(classifierOptions, "-S");
    result.addAll(classifierOptions);

    return (String[]) result.toArray(new String[result.size()]);
  }

  /**
   * Sets type of SVM (default SVMTYPE_C_SVC).
   *
   * @param value the type of the SVM
   */
  public void setSVMType(SelectedTag value) {
    if (value.getTags() == TAGS_SVMTYPE)
      m_SVMType = value.getSelectedTag().getID();
  }

  /**
   * Gets type of SVM.
   *
   * @return the type of the SVM
   */
  public SelectedTag getSVMType() {
    return new SelectedTag(m_SVMType, TAGS_SVMTYPE);
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String SVMTypeTipText() {
    return "The type of SVM to use.";
  }

  /**
   * Sets type of kernel function (default KERNELTYPE_RBF).
   *
   * @param value the kernel type
   */
  public void setKernelType(SelectedTag value) {
    if (value.getTags() == TAGS_KERNELTYPE)
      m_KernelType = value.getSelectedTag().getID();
  }

  /**
   * Gets type of kernel function.
   *
   * @return the kernel type
   */
  public SelectedTag getKernelType() {
    return new SelectedTag(m_KernelType, TAGS_KERNELTYPE);
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String kernelTypeTipText() {
    return "The type of kernel to use";
  }

  /**
   * Sets the degree of the kernel.
   *
   * @param value the degree of the kernel
   */
  public void setDegree(int value) {
    m_Degree = value;
  }

  /**
   * Gets the degree of the kernel.
   *
   * @return the degree of the kernel
   */
  public int getDegree() {
    return m_Degree;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String degreeTipText() {
    return "The degree of the kernel.";
  }

  /**
   * Sets gamma (default = 1/no of attributes).
   *
   * @param value the gamma value
   */
  public void setGamma(double value) {
    m_Gamma = value;
  }

  /**
   * Gets gamma.
   *
   * @return the current gamma
   */
  public double getGamma() {
    return m_Gamma;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String gammaTipText() {
    return "The gamma to use, if 0 then 1/max_index is used.";
  }

  /**
   * Sets coef (default 0).
   *
   * @param value the coef
   */
  public void setCoef0(double value) {
    m_Coef0 = value;
  }

  /**
   * Gets coef.
   *
   * @return the coef
   */
  public double getCoef0() {
    return m_Coef0;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String coef0TipText() {
    return "The coefficient to use.";
  }

  /**
   * Sets nu of nu-SVC, one-class SVM, and nu-SVR (default 0.5).
   *
   * @param value the new nu value
   */
  public void setNu(double value) {
    m_nu = value;
  }

  /**
   * Gets nu of nu-SVC, one-class SVM, and nu-SVR (default 0.5).
   *
   * @return the current nu value
   */
  public double getNu() {
    return m_nu;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String nuTipText() {
    return "The value of nu for nu-SVC, one-class SVM and nu-SVR.";
  }

  /**
   * Sets cache memory size in MB (default 40).
   *
   * @param value the memory size in MB
   */
  public void setCacheSize(double value) {
    m_CacheSize = value;
  }

  /**
   * Gets cache memory size in MB.
   *
   * @return the memory size in MB
   */
  public double getCacheSize() {
    return m_CacheSize;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String cacheSizeTipText() {
    return "The cache size in MB.";
  }

  /**
   * Sets the parameter C of C-SVC, epsilon-SVR, and nu-SVR (default 1).
   *
   * @param value the cost value
   */
  public void setCost(double value) {
    m_Cost = value;
  }

  /**
   * Sets the parameter C of C-SVC, epsilon-SVR, and nu-SVR.
   *
   * @return the cost value
   */
  public double getCost() {
    return m_Cost;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String costTipText() {
    return "The cost parameter C for C-SVC, epsilon-SVR and nu-SVR.";
  }

  /**
   * Sets tolerance of termination criterion (default 0.001).
   *
   * @param value the tolerance
   */
  public void setEps(double value) {
    m_eps = value;
  }

  /**
   * Gets tolerance of termination criterion.
   *
   * @return the current tolerance
   */
  public double getEps() {
    return m_eps;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String epsTipText() {
    return "The tolerance of the termination criterion.";
  }

  /**
   * Sets the epsilon in loss function of epsilon-SVR (default 0.1).
   *
   * @param value the loss epsilon
   */
  public void setLoss(double value) {
    m_Loss = value;
  }

  /**
   * Gets the epsilon in loss function of epsilon-SVR.
   *
   * @return the loss epsilon
   */
  public double getLoss() {
    return m_Loss;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String lossTipText() {
    return "The epsilon for the loss function in epsilon-SVR.";
  }

  /**
   * whether to use the shrinking heuristics.
   *
   * @param value true uses shrinking
   */
  public void setShrinking(boolean value) {
    m_Shrinking = value;
  }

  /**
   * whether to use the shrinking heuristics.
   *
   * @return true, if shrinking is used
   */
  public boolean getShrinking() {
    return m_Shrinking;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String shrinkingTipText() {
    return "Whether to use the shrinking heuristic.";
  }

  /**
   * whether to normalize input data.
   *
   * @param value whether to normalize the data
   */
  public void setNormalize(boolean value) {
    m_Normalize = value;
  }

  /**
   * whether to normalize input data.
   *
   * @return true, if the data is normalized
   */
  public boolean getNormalize() {
    return m_Normalize;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String normalizeTipText() {
    return "Whether to normalize the data.";
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String doNotReplaceMissingValuesTipText() {
    return "Whether to turn off automatic replacement of missing "
            + "values. WARNING: set to true only if the data does not "
            + "contain missing values.";
  }

  /**
   * Whether to turn off automatic replacement of missing values.
   * Set to true only if the data does not contain missing values.
   *
   * @param b true if automatic missing values replacement is
   *          to be disabled.
   */
  public void setDoNotReplaceMissingValues(boolean b) {
    m_noReplaceMissingValues = b;
  }

  /**
   * Gets whether automatic replacement of missing values is
   * disabled.
   *
   * @return true if automatic replacement of missing values
   * is disabled.
   */
  public boolean getDoNotReplaceMissingValues() {
    return m_noReplaceMissingValues;
  }

  /**
   * Sets the parameters C of class i to weight[i]*C, for C-SVC (default 1).
   * Blank separated list of doubles.
   *
   * @param weightsStr the weights (doubles, separated by blanks)
   */
  public void setWeights(String weightsStr) {
    StringTokenizer tok;
    int i;

    tok = new StringTokenizer(weightsStr, " ");
    m_Weight = new double[tok.countTokens()];
    m_WeightLabel = new int[tok.countTokens()];

    if (m_Weight.length == 0)
      System.out.println(
              "Zero Weights processed. Default weights will be used");

    for (i = 0; i < m_Weight.length; i++) {
      m_Weight[i] = Double.parseDouble(tok.nextToken());
      m_WeightLabel[i] = i;
    }
  }

  /**
   * Gets the parameters C of class i to weight[i]*C, for C-SVC (default 1).
   * Blank separated doubles.
   *
   * @return the weights (doubles separated by blanks)
   */
  public String getWeights() {

    StringBuffer result = new StringBuffer("");

    for (int i = 0; i < m_Weight.length; i++) {
      if (i > 0)
        result.append(" ");
      result.append(m_Weight[i]);
    }

    return result.toString();
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String weightsTipText() {
    return "The weights to use for the classes (blank-separated list, eg, \"1 1 1\" for a 3-class problem), if empty 1 is used by default.";
  }

  /**
   * Sets whether probability estimates are generated instead of -1/+1 for
   * classification problems.
   *
   * @param value whether to predict probabilities
   */
  public void setProbabilityEstimates(boolean value) {
    m_ProbabilityEstimates = value;
  }

  /**
   * Returns whether to generate probability estimates instead of -1/+1 for
   * classification problems.
   *
   * @return true, if probability estimates should be returned
   */
  public boolean getProbabilityEstimates() {
    return m_ProbabilityEstimates;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String probabilityEstimatesTipText() {
    return "Whether to generate probability estimates instead of -1/+1 for classification problems.";
  }

  /**
   * Sets the file to save the libsvm-internal model to. No model is saved if
   * pointing to a directory.
   *
   * @param value the filename/directory
   */
  public void setModelFile(File value) {
    if (value == null)
      m_ModelFile = new File(System.getProperty("user.dir"));
    else
      m_ModelFile = value;
  }

  /**
   * Returns the file to save the libsvm-internal model to. No model is saved
   * if pointing to a directory.
   *
   * @return the file object
   */
  public File getModelFile() {
    return m_ModelFile;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String modelFileTipText() {
    return "The file to save the libsvm-internal model to; no model is saved if pointing to a directory.";
  }

  /**
   * transfers the local variables into a svm_parameter object.
   *
   * @return the configured svm_parameter object
   */
  protected svm_parameter getParameters() {


    svm_parameter p = new svm_parameter();

    p.svm_type = m_SVMType;
    p.kernel_type = m_KernelType;
    p.degree = m_Degree;
    p.gamma = m_GammaActual;
    p.coef0 = m_Coef0;
    p.nu = m_nu;
    p.cache_size = m_CacheSize;
    p.C = m_Cost;
    p.eps = m_eps;
    p.p = m_Loss;
    p.shrinking = m_Shrinking ? 1 : 0;
    p.nr_weight = m_Weight.length;
    p.probability = m_ProbabilityEstimates ? 1 : 0;

    p.weight = new double[m_Weight.length];
    p.weight_label = new int[m_Weight.length];
    for (int i = 0; i < m_Weight.length; i++) {
      p.weight[i] = m_Weight[i];
      p.weight_label[i] = m_WeightLabel[i];
    }
    return p;
  }

  /**
   * returns the svm_problem.
   *
   * @param vx the x values
   * @param vy the y values
   * @return the svm_problem object
   */
  protected svm_problem getProblem(svm_node[][] vx, double[] vy) {

    svm_problem p = new svm_problem();
    p.l = vy.length;
    p.y = vy;
    p.x = vx;
    return p;
  }
  
  /**
   * returns an instance into a sparse libsvm array.
   * 
   * @param instance	the instance to work on
   * @return		the libsvm array
   * @throws Exception	if setup of array fails
   */
  protected svm_node[] instanceToArray(Instance instance) throws Exception {

    int count = 0;
    for (int i = 0; i < instance.numValues(); i++) {
      if (instance.index(i) == instance.classIndex())
        continue;
      if (instance.valueSparse(i) != 0)
        count++;
    }
    
    svm_node[] r = new svm_node[count];
    int index  = 0;
    for (int i = 0; i < instance.numValues(); i++) {
      
      int idx = instance.index(i);
      if (idx == instance.classIndex())
        continue;
      if (instance.valueSparse(i) == 0)
        continue;

      svm_node node = new svm_node();
      node.index = idx + 1;
      node.value = instance.valueSparse(i);
      r[index] = node;
      index++;
    }
    
    return r;
  }
  
  /**
   * Computes the distribution for a given instance. 
   * In case of 1-class classification, 1 is returned at index 0 if libsvm 
   * returns 1 and NaN (= missing) if libsvm returns -1.
   *
   * @param instance 		the instance for which distribution is computed
   * @return 			the distribution
   * @throws Exception 		if the distribution can't be computed successfully
   */
  public double[] distributionForInstance (Instance instance) throws Exception {

    int[] labels = new int[instance.numClasses()];
    double[] prob_estimates = null;

    if (m_ProbabilityEstimates) {
      svm.svm_get_labels(m_Model, labels);

      prob_estimates = new double[instance.numClasses()];
    }

    if (!getDoNotReplaceMissingValues()) {
      m_ReplaceMissingValues.input(instance);
      m_ReplaceMissingValues.batchFinished();
      instance = m_ReplaceMissingValues.output();
    }

    if (m_Filter != null) {
      m_Filter.input(instance);
      m_Filter.batchFinished();
      instance = m_Filter.output();
    }

    m_NominalToBinary.input(instance);
    m_NominalToBinary.batchFinished();
    instance = m_NominalToBinary.output();

    svm_node[] x = instanceToArray(instance);
    double v;
    double[] result = new double[instance.numClasses()];
    if (m_ProbabilityEstimates
            && ((m_SVMType == SVMTYPE_C_SVC) || (m_SVMType == SVMTYPE_NU_SVC))) {
      v = svm.svm_predict_probability(m_Model, x, prob_estimates);

      // Return order of probabilities to canonical weka attribute order
      for (int k = 0; k < prob_estimates.length; k++) {
        result[labels[k]] = prob_estimates[k];
      }
    } else {
      v = svm.svm_predict(m_Model, x);

      if (instance.classAttribute().isNominal()) {
        if (m_SVMType == SVMTYPE_ONE_CLASS_SVM) {
          if (v > 0)
            result[0] = 1;
          else
            // outlier (interface for Classifier specifies that unclassified instances
            // should return a distribution of all zeros)
            result[0] = 0;
        } else {
          result[(int) v] = 1;
        }
      } else {
        result[0] = v * m_x1 + m_x0;
      }
    }

    return result;
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
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
    result.enableDependency(Capability.UNARY_CLASS);
    result.enableDependency(Capability.NOMINAL_CLASS);
    result.enableDependency(Capability.NUMERIC_CLASS);
    result.enableDependency(Capability.DATE_CLASS);

    switch (m_SVMType) {
      case SVMTYPE_C_SVC:
      case SVMTYPE_NU_SVC:
        result.enable(Capability.NOMINAL_CLASS);
        break;

      case SVMTYPE_ONE_CLASS_SVM:
        result.enable(Capability.UNARY_CLASS);
        break;

      case SVMTYPE_EPSILON_SVR:
      case SVMTYPE_NU_SVR:
        result.enable(Capability.NUMERIC_CLASS);
        result.enable(Capability.DATE_CLASS);
        break;

      default:
        throw new IllegalArgumentException("SVMType " + m_SVMType + " is not supported!");
    }
    result.enable(Capability.MISSING_CLASS_VALUES);

    return result;
  }
  
  /**
   * builds the classifier.
   * 
   * @param insts       the training instances
   * @throws Exception  if libsvm encountered a problem
   */
  public void buildClassifier(Instances insts) throws Exception {
    m_Filter = null;

    // remove instances with missing class
    insts = new Instances(insts);
    insts.deleteWithMissingClass();
    
    if (!getDoNotReplaceMissingValues()) {
      m_ReplaceMissingValues = new ReplaceMissingValues();
      m_ReplaceMissingValues.setInputFormat(insts);
      insts = Filter.useFilter(insts, m_ReplaceMissingValues);
    }
    
    // can classifier handle the data?
    // we check this here so that if the user turns off
    // replace missing values filtering, it will fail
    // if the data actually does have missing values
    getCapabilities().testWithFail(insts);

    double y0 = Double.NaN;
    double y1 = y0;
    int index = -1;
    if (!insts.classAttribute().isNominal()) {
      y0 = insts.instance(0).classValue();
      index = 1;
      while (index < insts.numInstances() && insts.instance(index).classValue() == y0) {
        index++;
      }
      if (index == insts.numInstances()) {
        // degenerate case, all class values are equal
        // we don't want to deal with this, too much hassle
        throw new Exception("All class values are the same. At least two class values should be different");
      }
      y1 = insts.instance(index).classValue();
    }

    if (getNormalize()) {
      m_Filter = new Normalize();
      ((Normalize)m_Filter).setIgnoreClass(true); // Normalize class as well
      m_Filter.setInputFormat(insts);
      insts = Filter.useFilter(insts, m_Filter);
    }

    if (!insts.classAttribute().isNominal()) {
      if (m_Filter != null) {
        double z0 = insts.instance(0).classValue();
        double z1 = insts.instance(index).classValue();
        m_x1 = (y0 - y1) / (z0 - z1); // no division by zero, since y0 != y1 guaranteed => z0 != z1 ???
        m_x0 = (y0 - m_x1 * z0); // = y1 - m_x1 * z1
      } else {
        m_x1 = 1.0;
        m_x0 = 0.0;
      }
    } else {
      m_x0 = Double.NaN;
      m_x1 = m_x0;
    }

    // nominal to binary
    m_NominalToBinary = new NominalToBinary();
    m_NominalToBinary.setInputFormat(insts);
    insts = Filter.useFilter(insts, m_NominalToBinary);
    
    double[] vy = new double[insts.numInstances()];
    svm_node[][] vx = new svm_node[insts.numInstances()][];
    int max_index = 0;
    for (int d = 0; d < insts.numInstances(); d++) {
      Instance inst = insts.instance(d);
      vx[d] = instanceToArray(inst);
      if (vx[d].length > 0) {
        max_index = Math.max(max_index, vx[d][vx[d].length - 1].index);
      }
      vy[d] = inst.classValue();
    }
    
    // calculate actual gamma
    if (getGamma() == 0)
      m_GammaActual = 1.0 / max_index;
    else
      m_GammaActual = m_Gamma;

    svm_problem p = getProblem(vx, vy);
    svm_parameter pars = getParameters();

    // check parameters
    String error_msg = svm.svm_check_parameter(p, pars);
    
    if (error_msg != null)
      throw new Exception("Error: " + error_msg);

    // make probability estimates deterministic from run to run
    svm.rand.setSeed(m_Seed);

    // Change printing function if no debugging output is required
    if (!getDebug()) {
      svm.svm_set_print_string_function(new svm_print_interface() {
        @Override
        public void print(String s) {
          // Do nothing
        }
      });
    }

    // train model
    m_Model = svm.svm_train(p, pars);
    
    // save internal model?
    if (!m_ModelFile.isDirectory()) {
      svm.svm_save_model(m_ModelFile.getAbsolutePath(), m_Model);
    }
  }
    
  /**
   * returns a string representation.
   * 
   * @return            a string representation
   */
  public String toString() {
    return "LibSVM wrapper, original code by Yasser EL-Manzalawy (= WLSVM)";
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
  public static void main(String[] args) {
    runClassifier(new LibSVM(), args);
  }
}
