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
 * LibSVM.java
 * Copyright (C) 2005 Yasser EL-Manzalawy (original code)
 * Copyright (C) 2005 University of Waikato, Hamilton, NZ (adapted code)
 * 
 */

package weka.classifiers.functions;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

/*
 * Modifications by FracPete:
 * - complete overhaul to make it useable in Weka
 * - accesses libsvm classes only via Reflection to make Weka compile without
 *   the libsvm classes
 */

/** 
 <!-- globalinfo-start -->
 * A wrapper class for the libsvm tools (the libsvm classes, typically the jar file, need to be in the classpath to use this classifier).<br/>
 * LibSVM runs faster than SMO since it uses LibSVM to build the SVM classifier.<br/>
 * LibSVM allows users to experiment with One-class SVM, Regressing SVM, and nu-SVM supported by LibSVM tool. LibSVM reports many useful statistics about LibSVM classifier (e.g., confusion matrix,precision, recall, ROC score, etc.).<br/>
 * <br/>
 * Yasser EL-Manzalawy (2005). WLSVM. URL http://www.cs.iastate.edu/~yasser/wlsvm/.<br/>
 * <br/>
 * Chih-Chung Chang, Chih-Jen Lin (2001). LIBSVM - A Library for Support Vector Machines. URL http://www.csie.ntu.edu.tw/~cjlin/libsvm/.
 * <p/>
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
 *    URL = {http://www.cs.iastate.edu/~yasser/wlsvm/}
 * }
 * 
 * &#64;misc{Chang2001,
 *    author = {Chih-Chung Chang and Chih-Jen Lin},
 *    note = {The Weka classifier works with version 2.82 of LIBSVM},
 *    title = {LIBSVM - A Library for Support Vector Machines},
 *    year = {2001},
 *    URL = {http://www.csie.ntu.edu.tw/~cjlin/libsvm/}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -S &lt;int&gt;
 *   set type of SVM (default 0)
 *    0 = C-SVC
 *    1 = nu-SVC
 *    2 = one-class SVM
 *    3 = epsilon-SVR
 *    4 = nu-SVR</pre>
 * 
 * <pre> -K &lt;int&gt;
 *   set type of kernel function (default 2)
 *    0 = linear: u'*v
 *    1 = polynomial: (gamma*u'*v + coef0)^degree
 *    2 = radial basis function: exp(-gamma*|u-v|^2)
 *    3 = sigmoid: tanh(gamma*u'*v + coef0)</pre>
 * 
 * <pre> -D &lt;int&gt;
 *   set degree in kernel function (default 3)</pre>
 * 
 * <pre> -G &lt;double&gt;
 *   set gamma in kernel function (default 1/k)</pre>
 * 
 * <pre> -R &lt;double&gt;
 *   set coef0 in kernel function (default 0)</pre>
 * 
 * <pre> -C &lt;double&gt;
 *   set the parameter C of C-SVC, epsilon-SVR, and nu-SVR
 *   (default 1)</pre>
 * 
 * <pre> -N &lt;double&gt;
 *   set the parameter nu of nu-SVC, one-class SVM, and nu-SVR
 *   (default 0.5)</pre>
 * 
 * <pre> -Z
 *   turns on normalization of input data (default off)</pre>
 * 
 * <pre> -P &lt;double&gt;
 *   set the epsilon in loss function of epsilon-SVR (default 0.1)</pre>
 * 
 * <pre> -M &lt;double&gt;
 *   set cache memory size in MB (default 40)</pre>
 * 
 * <pre> -E &lt;double&gt;
 *   set tolerance of termination criterion (default 0.001)</pre>
 * 
 * <pre> -H
 *   turns the shrinking heuristics off (default on)</pre>
 * 
 * <pre> -W &lt;double&gt;
 *   set the parameters C of class i to weight[i]*C, for C-SVC
 *   (default 1)</pre>
 * 
 <!-- options-end -->
 *
 * @author  Yasser EL-Manzalawy
 * @author  FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.5 $
 */
public class LibSVM 
  extends Classifier
  implements TechnicalInformationHandler {
  
  /** the svm classname */
  protected final static String CLASS_SVM = "libsvm.svm";
  
  /** the svm_model classname */
  protected final static String CLASS_SVMMODEL = "libsvm.svm_model";
  
  /** the svm_problem classname */
  protected final static String CLASS_SVMPROBLEM = "libsvm.svm_problem";
  
  /** the svm_parameter classname */
  protected final static String CLASS_SVMPARAMETER = "libsvm.svm_parameter";
  
  /** the svm_node classname */
  protected final static String CLASS_SVMNODE = "libsvm.svm_node";
  
  /** serial UID */
  protected static final long serialVersionUID = 14172;
  
  /** LibSVM Model */
  protected Object m_Model;
  
  /** for normalizing the data */
  protected Filter m_Filter = null;
  
  /** normalize input data */
  protected boolean m_Normalize = false;
  
  /** SVM type C-SVC */
  public static final int SVMTYPE_C_SVC = 0;
  /** SVM type nu-SVC */
  public static final int SVMTYPE_NU_SVC = 1;
  /** SVM type one-class SVM */
  public static final int SVMTYPE_ONE_CLASS_SVM = 2;
  /** SVM type epsilon-SVR */
  public static final int SVMTYPE_EPSILON_SVR = 3;
  /** SVM type nu-SVR */
  public static final int SVMTYPE_NU_SVR = 4;
  /** SVM types */
  public static final Tag[] TAGS_SVMTYPE = {
    new Tag(SVMTYPE_C_SVC, "C-SVC"),
    new Tag(SVMTYPE_NU_SVC, "nu-SVC"),
    new Tag(SVMTYPE_ONE_CLASS_SVM, "one-class SVM"),
    new Tag(SVMTYPE_EPSILON_SVR, "epsilon-SVR"),
    new Tag(SVMTYPE_NU_SVR, "nu-SVR")
  };
  
  /** the SVM type */
  protected int m_SVMType = SVMTYPE_C_SVC;
  
  /** kernel type linear: u'*v */
  public static final int KERNELTYPE_LINEAR = 0;
  /** kernel type polynomial: (gamma*u'*v + coef0)^degree */
  public static final int KERNELTYPE_POLYNOMIAL = 1;
  /** kernel type radial basis function: exp(-gamma*|u-v|^2) */
  public static final int KERNELTYPE_RBF = 2;
  /** kernel type sigmoid: tanh(gamma*u'*v + coef0) */
  public static final int KERNELTYPE_SIGMOID = 3;
  /** the different kernel types */
  public static final Tag[] TAGS_KERNELTYPE = {
    new Tag(KERNELTYPE_LINEAR, "linear: u'*v"),
    new Tag(KERNELTYPE_POLYNOMIAL, "polynomial: (gamma*u'*v + coef0)^degree"),
    new Tag(KERNELTYPE_RBF, "radial basis function: exp(-gamma*|u-v|^2)"),
    new Tag(KERNELTYPE_SIGMOID, "sigmoid: tanh(gamma*u'*v + coef0)")
  };
  
  /** the kernel type */
  protected int m_KernelType = KERNELTYPE_RBF;
  
  /** for poly - in older versions of libsvm declared as a double.
   * At least since 2.82 it is an int. */
  protected int m_Degree = 3;
  
  /** for poly/rbf/sigmoid */
  protected double m_Gamma = 0;
  
  /** for poly/rbf/sigmoid (the actual gamma) */
  protected double m_GammaActual = 0;
  
  /** for poly/sigmoid */
  protected double m_Coef0 = 0;
  
  /** in MB */
  protected double m_CacheSize = 40;
  
  /** stopping criteria */
  protected double m_eps = 1e-3;
  
  /** cost, for C_SVC, EPSILON_SVR and NU_SVR */
  protected double m_Cost = 1;
  
  /** for C_SVC */
  protected int[] m_WeightLabel = new int[0];
  
  /** for C_SVC */
  protected double[] m_Weight = new double[0];
  
  /** for NU_SVC, ONE_CLASS, and NU_SVR */
  protected double m_nu = 0.5;
  
  /** loss, for EPSILON_SVR */
  protected double m_Loss = 0.1;
  
  /** use the shrinking heuristics */
  protected boolean m_Shrinking = true;	
  
  /** YASSER: don't predict probabilities */
  protected boolean m_predict_probability = false;
    
  /** whether the libsvm classes are in the Classpath */
  protected static boolean m_Present = false;
  static {
    try {
      Class.forName(CLASS_SVM);
      m_Present = true;
    }
    catch (Exception e) {
      m_Present = false;
    }
  }
  
  /**
   * Returns a string describing classifier
   * 
   * @return a description suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return 
      "A wrapper class for the libsvm tools (the libsvm classes, typically "
    + "the jar file, need to be in the classpath to use this classifier).\n"
    + "LibSVM runs faster than SMO since it uses LibSVM to build the SVM "
    + "classifier.\n"
    + "LibSVM allows users to experiment with One-class SVM, Regressing SVM, "
    + "and nu-SVM supported by LibSVM tool. LibSVM reports many useful "
    + "statistics about LibSVM classifier (e.g., confusion matrix,"
    + "precision, recall, ROC score, etc.).\n"
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
    TechnicalInformation 	result;
    TechnicalInformation 	additional;
    
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
    Vector      result;
    
    result = new Vector();
    
    result.addElement(
        new Option(
            "\t set type of SVM (default 0)\n"
            + "\t\t 0 = C-SVC\n" 
            + "\t\t 1 = nu-SVC\n"
            + "\t\t 2 = one-class SVM\n" 
            + "\t\t 3 = epsilon-SVR\n"
            + "\t\t 4 = nu-SVR", 
            "S", 1, "-S <int>"));
    
    result.addElement(
        new Option(
            "\t set type of kernel function (default 2)\n"
            + "\t\t 0 = linear: u'*v\n"
            + "\t\t 1 = polynomial: (gamma*u'*v + coef0)^degree\n"
            + "\t\t 2 = radial basis function: exp(-gamma*|u-v|^2)\n"
            + "\t\t 3 = sigmoid: tanh(gamma*u'*v + coef0)",
            "K", 1, "-K <int>"));
    
    result.addElement(
        new Option(
            "\t set degree in kernel function (default 3)", 
            "D", 1, "-D <int>"));
    
    result.addElement(
        new Option(
            "\t set gamma in kernel function (default 1/k)", 
            "G", 1, "-G <double>"));
    
    result.addElement(
        new Option(
            "\t set coef0 in kernel function (default 0)", 
            "R", 1, "-R <double>"));
    
    result.addElement(
        new Option(
            "\t set the parameter C of C-SVC, epsilon-SVR, and nu-SVR\n"
            + "\t (default 1)",
            "C", 1, "-C <double>"));
    
    result.addElement(
        new Option(
            "\t set the parameter nu of nu-SVC, one-class SVM, and nu-SVR\n"
            + "\t (default 0.5)",
            "N", 1, "-N <double>"));
    
    result.addElement(
        new Option(
            "\t turns on normalization of input data (default off)", 
            "Z", 0, "-Z"));
    
    result.addElement(
        new Option(
            "\t set the epsilon in loss function of epsilon-SVR (default 0.1)",
            "P", 1, "-P <double>"));
    
    result.addElement(
        new Option(
            "\t set cache memory size in MB (default 40)", 
            "M", 1, "-M <double>"));
    
    result.addElement(
        new Option(
            "\t set tolerance of termination criterion (default 0.001)",
            "E", 1, "-E <double>"));
    
    result.addElement(
        new Option(
            "\t turns the shrinking heuristics off (default on)",
            "H", 0, "-H"));
    
    result.addElement(
        new Option(
            "\t set the parameters C of class i to weight[i]*C, for C-SVC\n" 
            + "\t (default 1)",
            "W", 1, "-W <double>"));
    
    return result.elements();
  }
  
  /**
   * Sets the classifier options <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -S &lt;int&gt;
   *   set type of SVM (default 0)
   *    0 = C-SVC
   *    1 = nu-SVC
   *    2 = one-class SVM
   *    3 = epsilon-SVR
   *    4 = nu-SVR</pre>
   * 
   * <pre> -K &lt;int&gt;
   *   set type of kernel function (default 2)
   *    0 = linear: u'*v
   *    1 = polynomial: (gamma*u'*v + coef0)^degree
   *    2 = radial basis function: exp(-gamma*|u-v|^2)
   *    3 = sigmoid: tanh(gamma*u'*v + coef0)</pre>
   * 
   * <pre> -D &lt;int&gt;
   *   set degree in kernel function (default 3)</pre>
   * 
   * <pre> -G &lt;double&gt;
   *   set gamma in kernel function (default 1/k)</pre>
   * 
   * <pre> -R &lt;double&gt;
   *   set coef0 in kernel function (default 0)</pre>
   * 
   * <pre> -C &lt;double&gt;
   *   set the parameter C of C-SVC, epsilon-SVR, and nu-SVR
   *   (default 1)</pre>
   * 
   * <pre> -N &lt;double&gt;
   *   set the parameter nu of nu-SVC, one-class SVM, and nu-SVR
   *   (default 0.5)</pre>
   * 
   * <pre> -Z
   *   turns on normalization of input data (default off)</pre>
   * 
   * <pre> -P &lt;double&gt;
   *   set the epsilon in loss function of epsilon-SVR (default 0.1)</pre>
   * 
   * <pre> -M &lt;double&gt;
   *   set cache memory size in MB (default 40)</pre>
   * 
   * <pre> -E &lt;double&gt;
   *   set tolerance of termination criterion (default 0.001)</pre>
   * 
   * <pre> -H
   *   turns the shrinking heuristics off (default on)</pre>
   * 
   * <pre> -W &lt;double&gt;
   *   set the parameters C of class i to weight[i]*C, for C-SVC
   *   (default 1)</pre>
   * 
   <!-- options-end -->
   *
   * @param options     the options to parse
   * @throws Exception  if parsing fails
   */
  public void setOptions(String[] options) throws Exception {
    String      tmpStr;
    
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
    
    tmpStr = Utils.getOption('P', options);
    if (tmpStr.length() != 0)
      setLoss(Double.parseDouble(tmpStr));
    else
      setLoss(0.1);
    
    setShrinking(!Utils.getFlag('H', options));
    
    setWeights(Utils.getOption('W', options));
  }
  
  /**
   * Returns the current options
   * 
   * @return            the current setup
   */
  public String[] getOptions() {
    Vector        result;
    
    result  = new Vector();
    
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
    
    if (getWeights().length() != 0) {
      result.add("-W");
      result.add("" + getWeights());
    }
    
    return (String[]) result.toArray(new String[result.size()]);
  }
  
  /**
   * returns whether the libsvm classes are present or not, i.e. whether the 
   * classes are in the classpath or not
   *
   * @return whether the libsvm classes are available
   */
  public static boolean isPresent() {
    return m_Present;
  }
  
  /**
   * Sets type of SVM (default SVMTYPE_C_SVC)
   * 
   * @param value       the type of the SVM
   */
  public void setSVMType(SelectedTag value) {
    if (value.getTags() == TAGS_SVMTYPE)
      m_SVMType = value.getSelectedTag().getID();
  }
  
  /**
   * Gets type of SVM
   * 
   * @return            the type of the SVM
   */
  public SelectedTag getSVMType() {
    return new SelectedTag(m_SVMType, TAGS_SVMTYPE);
  }
  
  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   *         displaying in the explorer/experimenter gui
   */
  public String SVMTypeTipText() {
    return "The type of SVM to use.";
  }
  
  /**
   * Sets type of kernel function (default KERNELTYPE_RBF)
   * 
   * @param value       the kernel type
   */
  public void setKernelType(SelectedTag value) {
    if (value.getTags() == TAGS_KERNELTYPE)
      m_KernelType = value.getSelectedTag().getID();
  }
  
  /**
   * Gets type of kernel function
   * 
   * @return            the kernel type
   */
  public SelectedTag getKernelType() {
    return new SelectedTag(m_KernelType, TAGS_KERNELTYPE);
  }
  
  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   *         displaying in the explorer/experimenter gui
   */
  public String kernelTypeTipText() {
    return "The type of kernel to use";
  }
  
  /**
   * Sets the degree of the kernel
   * 
   * @param value       the degree of the kernel
   */
  public void setDegree(int value) {
    m_Degree = value;
  }
  
  /**
   * Gets the degree of the kernel
   * 
   * @return            the degree of the kernel
   */
  public int getDegree() {
    return m_Degree;
  }
  
  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   *         displaying in the explorer/experimenter gui
   */
  public String degreeTipText() {
    return "The degree of the kernel.";
  }
  
  /**
   * Sets gamma (default = 1/no of attributes)
   * 
   * @param value       the gamma value
   */
  public void setGamma(double value) {
    m_Gamma = value;
  }
  
  /**
   * Gets gamma
   * 
   * @return            the current gamma
   */
  public double getGamma() {
    return m_Gamma;
  }
  
  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   *         displaying in the explorer/experimenter gui
   */
  public String gammaTipText() {
    return "The gamma to use, if 0 then 1/max_index is used.";
  }
  
  /**
   * Sets coef (default 0)
   * 
   * @param value       the coef
   */
  public void setCoef0(double value) {
    m_Coef0 = value;
  }
  
  /**
   * Gets coef
   * 
   * @return            the coef
   */
  public double getCoef0() {
    return m_Coef0;
  }
  
  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   *         displaying in the explorer/experimenter gui
   */
  public String coef0TipText() {
    return "The coefficient to use.";
  }
  
  /**
   * Sets nu of nu-SVC, one-class SVM, and nu-SVR (default 0.5)
   * 
   * @param value       the new nu value
   */
  public void setNu(double value) {
    m_nu = value;
  }
  
  /**
   * Gets nu of nu-SVC, one-class SVM, and nu-SVR (default 0.5)
   * 
   * @return            the current nu value
   */
  public double getNu() {
    return m_nu;
  }
  
  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   *         displaying in the explorer/experimenter gui
   */
  public String nuTipText() {
    return "The value of nu for nu-SVC, one-class SVM and nu-SVR.";
  }
  
  /**
   * Sets cache memory size in MB (default 40)
   * 
   * @param value       the memory size in MB
   */
  public void setCacheSize(double value) {
    m_CacheSize = value;
  }
  
  /**
   * Gets cache memory size in MB
   * 
   * @return            the memory size in MB
   */
  public double getCacheSize() {
    return m_CacheSize;
  }
  
  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   *         displaying in the explorer/experimenter gui
   */
  public String cacheSizeTipText() {
    return "The cache size in MB.";
  }
  
  /**
   * Sets the parameter C of C-SVC, epsilon-SVR, and nu-SVR (default 1)
   * 
   * @param value       the cost value
   */
  public void setCost(double value) {
    m_Cost = value;
  }
  
  /**
   * Sets the parameter C of C-SVC, epsilon-SVR, and nu-SVR
   * 
   * @return            the cost value
   */
  public double getCost() {
    return m_Cost;
  }
  
  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   *         displaying in the explorer/experimenter gui
   */
  public String costTipText() {
    return "The cost parameter C for C-SVC, epsilon-SVR and nu-SVR.";
  }
  
  /**
   * Sets tolerance of termination criterion (default 0.001)
   * 
   * @param value       the tolerance
   */
  public void setEps(double value) {
    m_eps = value;
  }
  
  /**
   * Gets tolerance of termination criterion
   * 
   * @return            the current tolerance
   */
  public double getEps() {
    return m_eps;
  }
  
  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   *         displaying in the explorer/experimenter gui
   */
  public String epsTipText() {
    return "The tolerance of the termination criterion.";
  }
  
  /**
   * Sets the epsilon in loss function of epsilon-SVR (default 0.1)
   * 
   * @param value       the loss epsilon
   */
  public void setLoss(double value) {
    m_Loss = value;
  }
  
  /**
   * Gets the epsilon in loss function of epsilon-SVR
   * 
   * @return            the loss epsilon
   */
  public double getLoss() {
    return m_Loss;
  }
  
  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   *         displaying in the explorer/experimenter gui
   */
  public String lossTipText() {
    return "The epsilon for the loss function in epsilon-SVR.";
  }
  
  /**
   * whether to use the shrinking heuristics
   * 
   * @param value       true uses shrinking
   */
  public void setShrinking(boolean value) {
    m_Shrinking = value;
  }
  
  /**
   * whether to use the shrinking heuristics
   * 
   * @return            true, if shrinking is used
   */
  public boolean getShrinking() {
    return m_Shrinking;
  }
  
  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   *         displaying in the explorer/experimenter gui
   */
  public String shrinkingTipText() {
    return "Whether to use the shrinking heuristic.";
  }
  
  /**
   * whether to normalize input data
   * 
   * @param value       whether to normalize the data
   */
  public void setNormalize(boolean value) {
    m_Normalize = value;
  }
  
  /**
   * whether to normalize input data
   * 
   * @return            true, if the data is normalized
   */
  public boolean getNormalize() {
    return m_Normalize;
  }
  
  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   *         displaying in the explorer/experimenter gui
   */
  public String normalizeTipText() {
    return "Whether to normalize the data.";
  }
  
  /**
   * Sets the parameters C of class i to weight[i]*C, for C-SVC (default 1).
   * Blank separated list of doubles.
   * 
   * @param weightsStr          the weights (doubles, separated by blanks)
   */
  public void setWeights(String weightsStr) {
    StringTokenizer       tok;
    int                   i;
    
    tok           = new StringTokenizer(weightsStr, " ");
    m_Weight      = new double[tok.countTokens()];
    m_WeightLabel = new int[tok.countTokens()];
    
    if (m_Weight.length == 0)
      System.out.println(
          "Zero Weights processed. Default weights will be used");
    
    for (i = 0; i < m_Weight.length; i++) {
      m_Weight[i] = Double.parseDouble(tok.nextToken());
      if (i == 0)
        m_WeightLabel[i] = -1;  // label of first class
      else
        m_WeightLabel[i] = i;
    }
  }
  
  /**
   * Gets the parameters C of class i to weight[i]*C, for C-SVC (default 1).
   * Blank separated doubles.
   * 
   * @return            the weights (doubles separated by blanks)
   */
  public String getWeights() {
    String      result;
    int         i;
    
    result = "";
    for (i = 0; i < m_Weight.length; i++) {
      if (i > 0)
        result += " ";
      result += Double.toString(m_Weight[i]);
    }
    
    return result;
  }
  
  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   *         displaying in the explorer/experimenter gui
   */
  public String weightsTipText() {
    return "The weights to use for the classes, if empty 1 is used by default.";
  }
  
  /**
   * sets the specified field
   * 
   * @param o           the object to set the field for
   * @param name        the name of the field
   * @param value       the new value of the field
   */
  protected void setField(Object o, String name, Object value) {
    Field       f;
    
    try {
      f = o.getClass().getField(name);
      f.set(o, value);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  /**
   * sets the specified field in an array
   * 
   * @param o           the object to set the field for
   * @param name        the name of the field
   * @param index       the index in the array
   * @param value       the new value of the field
   */
  protected void setField(Object o, String name, int index, Object value) {
    Field       f;
    
    try {
      f = o.getClass().getField(name);
      Array.set(f.get(o), index, value);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  /**
   * returns the current value of the specified field
   * 
   * @param o           the object the field is member of
   * @param name        the name of the field
   * @return            the value
   */
  protected Object getField(Object o, String name) {
    Field       f;
    Object      result;
    
    try {
      f      = o.getClass().getField(name);
      result = f.get(o);
    }
    catch (Exception e) {
      e.printStackTrace();
      result = null;
    }
    
    return result;
  }
  
  /**
   * sets a new array for the field
   * 
   * @param o           the object to set the array for
   * @param name        the name of the field
   * @param type        the type of the array
   * @param length      the length of the one-dimensional array
   */
  protected void newArray(Object o, String name, Class type, int length) {
    newArray(o, name, type, new int[]{length});
  }
  
  /**
   * sets a new array for the field
   * 
   * @param o           the object to set the array for
   * @param name        the name of the field
   * @param type        the type of the array
   * @param dimensions  the dimensions of the array
   */
  protected void newArray(Object o, String name, Class type, int[] dimensions) {
    Field       f;
    
    try {
      f = o.getClass().getField(name);
      f.set(o, Array.newInstance(type, dimensions));
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  /**
   * executes the specified method and returns the result, if any
   * 
   * @param o                   the object the method should be called from
   * @param name                the name of the method
   * @param paramClasses        the classes of the parameters
   * @param paramValues         the values of the parameters
   * @return                    the return value of the method, if any (in that case null)
   */
  protected Object invokeMethod(Object o, String name, Class[] paramClasses, Object[] paramValues) {
    Method      m;
    Object      result;
    
    result = null;
    
    try {
      m      = o.getClass().getMethod(name, paramClasses);
      result = m.invoke(o, paramValues);
    }
    catch (Exception e) {
      e.printStackTrace();
      result = null;
    }
    
    return result;
  }
  
  /**
   * transfers the local variables into a svm_parameter object
   * 
   * @return the configured svm_parameter object
   */
  protected Object getParameters() {
    Object      result;
    int         i;
    
    try {
      result = Class.forName(CLASS_SVMPARAMETER).newInstance();
      
      setField(result, "svm_type", new Integer(m_SVMType));
      setField(result, "kernel_type", new Integer(m_KernelType));
      setField(result, "degree", new Integer(m_Degree));
      setField(result, "gamma", new Double(m_GammaActual));
      setField(result, "coef0", new Double(m_Coef0));
      setField(result, "nu", new Double(m_nu));
      setField(result, "cache_size", new Double(m_CacheSize));
      setField(result, "C", new Double(m_Cost));
      setField(result, "eps", new Double(m_eps));
      setField(result, "p", new Double(m_Loss));
      setField(result, "shrinking", new Integer(m_Shrinking ? 1 : 0));
      setField(result, "nr_weight", new Integer(m_Weight.length));
      
      newArray(result, "weight", Double.TYPE, m_Weight.length);
      newArray(result, "weight_label", Integer.TYPE, m_Weight.length);
      for (i = 0; i < m_Weight.length; i++) {
        setField(result, "weight", i, new Double(m_Weight[i]));
        setField(result, "weight_label", i, new Integer(m_WeightLabel[i]));
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      result = null;
    }
    
    return result;
  }
  
  /**
   * returns the svm_problem
   * 
   * @param vx the x values
   * @param vy the y values
   * @return the svm_problem object
   */
  protected Object getProblem(Vector vx, Vector vy) {
    Object      result;
    
    try {
      result = Class.forName(CLASS_SVMPROBLEM).newInstance();
      
      setField(result, "l", new Integer(vy.size()));
      
      newArray(result, "x", Class.forName(CLASS_SVMNODE), new int[]{vy.size(), 0});
      for (int i = 0; i < vy.size(); i++)
        setField(result, "x", i, vx.elementAt(i));
      
      newArray(result, "y", Double.TYPE, vy.size());
      for (int i = 0; i < vy.size(); i++)
        setField(result, "y", i, new Double((String) vy.elementAt(i)));
    }
    catch (Exception e) {
      e.printStackTrace();
      result = null;
    }
    
    return result;
  }
  
  /**
   * Converts and ARFF Instance into a string in the sparse format accepted by
   * LIBSVM
   * 
   * @param instance            the instance to turn into sparse format
   * @return                    the sparse String representation
   */
  protected String instanceToSparse(Instance instance) {
    String line = new String();
    int c = (int) instance.classValue();
    if (c == 0)
      c = -1;
    line = c + " ";
    for (int j = 1; j < instance.numAttributes(); j++) {
      if (instance.value(j - 1) != 0)
        line += " " + j + ":" + instance.value(j - 1);
    }
    
    return (line + "\n");
  }
  
  /**
   * converts an ARFF dataset into sparse format
   * 
   * @param data                the dataset to process
   * @return                    the processed data
   */
  protected Vector dataToSparse(Instances data) {
    Vector sparse = new Vector(data.numInstances() + 1);
    
    for (int i = 0; i < data.numInstances(); i++)
      sparse.add(instanceToSparse(data.instance(i)));
    
    return sparse;
  }
  
  /**
   * classifies the given instance
   * 
   * @param instance            the instance to classify
   * @return                    the class label
   * @throws Exception          if an error occurs
   */
  public double classifyInstance(Instance instance) throws Exception {
    
    int[] labels = new int[instance.numClasses()];
    double[] prob_estimates = null;
    
    // FracPete: the following block is NOT tested!
    if (m_predict_probability) {
      if (    (m_SVMType == SVMTYPE_EPSILON_SVR)
           || (m_SVMType == SVMTYPE_NU_SVR) ) {
        
        double prob = ((Double) invokeMethod(
            Class.forName(CLASS_SVM).newInstance(),
            "svm_get_svr_probability",
            new Class[]{Class.forName(CLASS_SVMMODEL)},
            new Object[]{m_Model})).doubleValue();

        System.out.print(
            "Prob. model for test data: target value = predicted value + z,\n"
            + "z: Laplace distribution e^(-|z|/sigma)/(2sigma),sigma="
            + prob + "\n");
      } 
      else {
        invokeMethod(
            Class.forName(CLASS_SVM).newInstance(),
            "svm_get_labels",
            new Class[]{
              Class.forName(CLASS_SVMMODEL), 
              Array.newInstance(Integer.TYPE, instance.numClasses()).getClass()},
            new Object[]{
              m_Model, 
              labels});

        prob_estimates = new double[instance.numClasses()];
      }
    }
    
    if (m_Filter != null) {
      m_Filter.input(instance);
      m_Filter.batchFinished();
      instance = m_Filter.output();
    }
    
    String line = instanceToSparse(instance);
    
    StringTokenizer st = new StringTokenizer(line, " \t\n\r\f:");
    
    st.nextToken();   // skip class
    int m = st.countTokens() / 2;
    Object x = Array.newInstance(Class.forName(CLASS_SVMNODE), m);
    for (int j = 0; j < m; j++) {
      Array.set(x, j, Class.forName(CLASS_SVMNODE).newInstance());
      setField(Array.get(x, j), "index", new Integer(st.nextToken()));
      setField(Array.get(x, j), "value", new Double(st.nextToken()));
    }
    
    double v;
    if (    m_predict_probability
         && (    (m_SVMType == SVMTYPE_C_SVC) 
              || (m_SVMType == SVMTYPE_NU_SVC) ) ) {
      v = ((Double) invokeMethod(
          Class.forName(CLASS_SVM).newInstance(),
          "svm_predict_probability",
          new Class[]{
            Class.forName(CLASS_SVMMODEL), 
            Array.newInstance(Class.forName(CLASS_SVMNODE), Array.getLength(x)).getClass(),
            Array.newInstance(Double.TYPE, prob_estimates.length).getClass()},
          new Object[]{
            m_Model, 
            x,
            prob_estimates})).doubleValue();
    } 
    else {
      v = ((Double) invokeMethod(
          Class.forName(CLASS_SVM).newInstance(),
          "svm_predict",
          new Class[]{
            Class.forName(CLASS_SVMMODEL), 
            Array.newInstance(Class.forName(CLASS_SVMNODE), Array.getLength(x)).getClass()},
          new Object[]{
            m_Model, 
            x})).doubleValue();
    }

    // transform frist class label into Weka format
    if (v == -1)
      v = 0;
    
    return v;
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

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
    
    return result;
  }
  
  /**
   * builds the classifier
   * 
   * @param insts       the training instances
   * @throws Exception  if libsvm classes not in classpath or libsvm
   *                    encountered a problem
   */
  public void buildClassifier(Instances insts) throws Exception {
    
    if (!isPresent())
      throw new Exception("libsvm classes not in CLASSPATH!");

    // can classifier handle the data?
    getCapabilities().testWithFail(insts);

    // remove instances with missing class
    insts = new Instances(insts);
    insts.deleteWithMissingClass();
    
    if (getNormalize()) {
      m_Filter = new Normalize();
      m_Filter.setInputFormat(insts);
      insts = Filter.useFilter(insts, m_Filter);
    }
    
    Vector sparseData = dataToSparse(insts);
    Vector vy = new Vector();
    Vector vx = new Vector();
    int max_index = 0;
    
    for (int d = 0; d < sparseData.size(); d++) {
      String line = (String) sparseData.get(d);
      
      StringTokenizer st = new StringTokenizer(line, " \t\n\r\f:");
      
      vy.addElement(st.nextToken());
      int m = st.countTokens() / 2;
      Object x = Array.newInstance(Class.forName(CLASS_SVMNODE), m);
      for (int j = 0; j < m; j++) {
        Array.set(x, j, Class.forName(CLASS_SVMNODE).newInstance());
        setField(Array.get(x, j), "index", new Integer(st.nextToken()));
        setField(Array.get(x, j), "value", new Double(st.nextToken()));
      }
      if (m > 0)
        max_index = Math.max(max_index, ((Integer) getField(Array.get(x, m - 1), "index")).intValue());
      vx.addElement(x);
    }
    
    // calculate actual gamma
    if (getGamma() == 0)
      m_GammaActual = 1.0 / max_index;
    else
      m_GammaActual = m_Gamma;

    // check parameter
    String error_msg = (String) invokeMethod(
        Class.forName(CLASS_SVM).newInstance(), 
        "svm_check_parameter", 
        new Class[]{
          Class.forName(CLASS_SVMPROBLEM), 
          Class.forName(CLASS_SVMPARAMETER)},
        new Object[]{
          getProblem(vx, vy), 
          getParameters()});
    
    if (error_msg != null)
      throw new Exception("Error: " + error_msg);
    
    // train model
    m_Model = invokeMethod(
        Class.forName(CLASS_SVM).newInstance(), 
        "svm_train", 
        new Class[]{
          Class.forName(CLASS_SVMPROBLEM), 
          Class.forName(CLASS_SVMPARAMETER)},
        new Object[]{
          getProblem(vx, vy), 
          getParameters()});
  }
  
  /**
   * returns a string representation
   * 
   * @return            a string representation
   */
  public String toString() {
    return "LibSVM wrapper, original code by Yasser EL-Manzalawy (= WLSVM)";
  }
  
  /**
   * Main method for testing this class.
   * 
   * @param args the options
   */
  public static void main(String[] args) {
    try {
      System.out.println(Evaluation.evaluateModel(new LibSVM(), args));
    } 
    catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
}
