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
 *    BayesianLogisticRegression.java
 *    Copyright (C) 2008 Illinois Institute of Technology
 *
 */

package weka.classifiers.bayes;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.blr.GaussianPriorImpl;
import weka.classifiers.bayes.blr.LaplacePriorImpl;
import weka.classifiers.bayes.blr.Prior;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.SerializedObject;
import weka.core.Tag;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;

import java.util.Enumeration;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * Implements Bayesian Logistic Regression for both Gaussian and Laplace Priors.<br/>
 * <br/>
 * For more information, see<br/>
 * <br/>
 * Alexander Genkin, David D. Lewis, David Madigan (2004). Large-scale bayesian logistic regression for text categorization. URL http://www.stat.rutgers.edu/~madigan/PAPERS/shortFat-v3a.pdf.
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;techreport{Genkin2004,
 *    author = {Alexander Genkin and David D. Lewis and David Madigan},
 *    institution = {DIMACS},
 *    title = {Large-scale bayesian logistic regression for text categorization},
 *    year = {2004},
 *    URL = {http://www.stat.rutgers.edu/\~madigan/PAPERS/shortFat-v3a.pdf}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 *
 *  @author Navendu Garg (gargnav at iit dot edu)
 *  @version $Revision: 1.3 $
 */
public class BayesianLogisticRegression extends Classifier
  implements OptionHandler, TechnicalInformationHandler {
  
  static final long serialVersionUID = -8013478897911757631L;

  /** Log-likelihood values to be used to choose the best hyperparameter. */
  public static double[] LogLikelihood;

  /** Set of values to be used as hyperparameter values during Cross-Validation. */
  public static double[] InputHyperparameterValues;

  /** DEBUG Mode*/
  boolean debug = false;

  /** Choose whether to normalize data or not */
  public boolean NormalizeData = false;

  /** Tolerance criteria for the stopping criterion. */
  public double Tolerance = 0.0005;

  /** Threshold for binary classification of probabilisitic estimate*/
  public double Threshold = 0.5;

  /** Distributions available */
  public static final int GAUSSIAN = 1;
  public static final int LAPLACIAN = 2;
  
  public static final Tag[] TAGS_PRIOR = {
    new Tag(GAUSSIAN, "Gaussian"),
    new Tag(LAPLACIAN, "Laplacian")
  };

  /** Distribution Prior class */
  public int PriorClass = GAUSSIAN;

  /** NumFolds for CV based Hyperparameters selection*/
  public int NumFolds = 2;

  /** Methods for selecting the hyperparameter value */
  public static final int NORM_BASED = 1;
  public static final int CV_BASED = 2;
  public static final int SPECIFIC_VALUE = 3;

  public static final Tag[] TAGS_HYPER_METHOD = {
    new Tag(NORM_BASED, "Norm-based"),
    new Tag(CV_BASED, "CV-based"),
    new Tag(SPECIFIC_VALUE, "Specific value")
  };

  /** Hyperparameter selection method */
  public int HyperparameterSelection = NORM_BASED;

  /** The class index from the training data */
  public int ClassIndex = -1;

  /** Best hyperparameter for test phase */
  public double HyperparameterValue = 0.27;

  /** CV Hyperparameter Range */
  public String HyperparameterRange = "R:0.01-316,3.16";

  /** Maximum number of iterations */
  public int maxIterations = 100;

  /**Iteration counter */
  public int iterationCounter = 0;

  /** Array for storing coefficients of Bayesian regression model. */
  public double[] BetaVector;

  /** Array to store Regression Coefficient updates. */
  public double[] DeltaBeta;

  /**        Trust Region Radius Update*/
  public double[] DeltaUpdate;

  /** Trust Region Radius*/
  public double[] Delta;

  /**  Array to store Hyperparameter values for each feature. */
  public double[] Hyperparameters;

  /** R(i)= BetaVector X x(i) X y(i).
   * This an intermediate value with respect to vector BETA, input values and corresponding class labels*/
  public double[] R;

  /** This vector is used to store the increments on the R(i). It is also used to determining the stopping criterion.*/
  public double[] DeltaR;

  /**
   * This variable is used to keep track of change in
   * the value of delta summation of r(i).
   */
  public double Change;

  /**
   * Bayesian Logistic Regression returns the probability of a given instance will belong to a certain
   * class (p(y=+1|Beta,X). To obtain a binary value the Threshold value is used.
   * <pre>
   * p(y=+1|Beta,X)>Threshold ? 1 : -1
   * </pre>
   */

  /** Filter interface used to point to weka.filters.unsupervised.attribute.Normalize object
   *
   */
  public Filter m_Filter;

  /** Dataset provided to do Training/Test set.*/
  protected Instances m_Instances;

  /**        Prior class object interface*/
  protected Prior m_PriorUpdate;

  public String globalInfo() {
    return "Implements Bayesian Logistic Regression "
      + "for both Gaussian and Laplace Priors.\n\n"
      + "For more information, see\n\n"
      + getTechnicalInformation();
  }

  /**
   * <pre>
   * (1)Initialize m_Beta[j] to 0.
   * (2)Initialize m_DeltaUpdate[j].
   * </pre>
   *
   * */
  public void initialize() throws Exception {
    int numOfAttributes;
    int numOfInstances;
    int i;
    int j;

    Change = 0.0;

    //Manipulate Data
    if (NormalizeData) {
      m_Filter = new Normalize();
      m_Filter.setInputFormat(m_Instances);
      m_Instances = Filter.useFilter(m_Instances, m_Filter);
    }

    //Set the intecept coefficient.
    Attribute att = new Attribute("(intercept)");
    Instance instance;

    m_Instances.insertAttributeAt(att, 0);

    for (i = 0; i < m_Instances.numInstances(); i++) {
      instance = m_Instances.instance(i);
      instance.setValue(0, 1.0);
    }

    //Get the number of attributes
    numOfAttributes = m_Instances.numAttributes();
    numOfInstances = m_Instances.numInstances();
    ClassIndex = m_Instances.classIndex();
    iterationCounter = 0;

    //Initialize Arrays.
    switch (HyperparameterSelection) {
    case 1:
      HyperparameterValue = normBasedHyperParameter();

      if (debug) {
        System.out.println("Norm-based Hyperparameter: " + HyperparameterValue);
      }

      break;

    case 2:
      HyperparameterValue = CVBasedHyperparameter();

      if (debug) {
        System.out.println("CV-based Hyperparameter: " + HyperparameterValue);
      }

      break;
    }

    BetaVector = new double[numOfAttributes];
    Delta = new double[numOfAttributes];
    DeltaBeta = new double[numOfAttributes];
    Hyperparameters = new double[numOfAttributes];
    DeltaUpdate = new double[numOfAttributes];

    for (j = 0; j < numOfAttributes; j++) {
      BetaVector[j] = 0.0;
      Delta[j] = 1.0;
      DeltaBeta[j] = 0.0;
      DeltaUpdate[j] = 0.0;

      //TODO: Change the way it takes values.
      Hyperparameters[j] = HyperparameterValue;
    }

    DeltaR = new double[numOfInstances];
    R = new double[numOfInstances];

    for (i = 0; i < numOfInstances; i++) {
      DeltaR[i] = 0.0;
      R[i] = 0.0;
    }

    //Set the Prior interface to the appropriate prior implementation.
    if (PriorClass == GAUSSIAN) {
      m_PriorUpdate = new GaussianPriorImpl();
    } else {
      m_PriorUpdate = new LaplacePriorImpl();
    }
  }

  /**
   * This method tests what kind of data this classifier can handle.
   * return Capabilities
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capability.NUMERIC_ATTRIBUTES);

    result.enable(Capability.BINARY_ATTRIBUTES);

    // class
    result.enable(Capability.BINARY_CLASS);

    // instances
    result.setMinimumNumberInstances(0);

    return result;
  }

  /**
   * <ul>
   *         <li>(1) Set the data to the class attribute m_Instances.</li>
   *  <li>(2)Call the method initialize() to initialize the values.</li>
   * </ul>
   *        @param data training data
   *        @exception Exception if classifier can't be built successfully.
   */
  public void buildClassifier(Instances data) throws Exception {
    Instance instance;
    int i;
    int j;

    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    //(1) Set the data to the class attribute m_Instances.
    m_Instances = new Instances(data);

    //(2)Call the method initialize() to initialize the values.
    initialize();

    do {
      //Compute the prior Trust Region Radius Update;
      for (j = 0; j < m_Instances.numAttributes(); j++) {
        if (j != ClassIndex) {
          DeltaUpdate[j] = m_PriorUpdate.update(j, m_Instances, BetaVector[j],
              Hyperparameters[j], R, Delta[j]);
          //limit step to trust region.
          DeltaBeta[j] = Math.min(Math.max(DeltaUpdate[j], 0 - Delta[j]),
              Delta[j]);

          //Update the 
          for (i = 0; i < m_Instances.numInstances(); i++) {
            instance = m_Instances.instance(i);

            if (instance.value(j) != 0) {
              DeltaR[i] = DeltaBeta[j] * instance.value(j) * classSgn(instance.classValue());
              R[i] += DeltaR[i];
            }
          }

          //Updated Beta values.
          BetaVector[j] += DeltaBeta[j];

          //Update size of trust region.
          Delta[j] = Math.max(2 * Math.abs(DeltaBeta[j]), Delta[j] / 2.0);
        }
      }
    } while (!stoppingCriterion());

    m_PriorUpdate.computelogLikelihood(BetaVector, m_Instances);
    m_PriorUpdate.computePenalty(BetaVector, Hyperparameters);
  }

  /**
   * This class is used to mask the internal class labels.
   *
   * @param value internal class label
   * @return
   * <pre>
   * <ul><li>
   *  -1 for internal class label 0
   *  </li>
   *  <li>
   *  +1 for internal class label 1
   *  </li>
   *  </ul>
   *  </pre>
   */
  public static double classSgn(double value) {
    if (value == 0.0) {
      return -1.0;
    } else {
      return 1.0;
    }
  }

  /**
    * Returns an instance of a TechnicalInformation object, containing
    * detailed information about the technical background of this class,
    * e.g., paper reference or book this class is based on.
    *
    * @return the technical information about this class
    */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation result = null;

    result = new TechnicalInformation(Type.TECHREPORT);
    result.setValue(Field.AUTHOR, "Alexander Genkin and David D. Lewis and David Madigan");
    result.setValue(Field.YEAR, "2004");
    result.setValue(Field.TITLE, "Large-scale bayesian logistic regression for text categorization");
    result.setValue(Field.INSTITUTION, "DIMACS");
    result.setValue(Field.URL, "http://www.stat.rutgers.edu/~madigan/PAPERS/shortFat-v3a.pdf");
    return result;
  }

  /**
   * This is a convient function that defines and upper bound
   * (Delta>0) for values of r(i) reachable by updates in the
   * trust region.
   *
   * r BetaVector X x(i)y(i).
   * delta A parameter where sigma > 0
   * @return double function value
   */
  public static double bigF(double r, double sigma) {
    double funcValue = 0.25;
    double absR = Math.abs(r);

    if (absR > sigma) {
      funcValue = 1.0 / (2.0 + Math.exp(absR - sigma) + Math.exp(sigma - absR));
    }

    return funcValue;
  }

  /**
   * This method implements the stopping criterion
   * function.
   *
   * @return boolean whether to stop or not.
   */
  public boolean stoppingCriterion() {
    int i;
    double sum_deltaR = 0.0;
    double sum_R = 1.0;
    boolean shouldStop;
    double value = 0.0;
    double delta;

    //Summation of changes in R(i) vector.
    for (i = 0; i < m_Instances.numInstances(); i++) {
      sum_deltaR += Math.abs(DeltaR[i]); //Numerator (deltaR(i))
      sum_R += Math.abs(R[i]); // Denominator (1+sum(R(i))
    }

    delta = Math.abs(sum_deltaR - Change);
    Change = delta / sum_R;

    if (debug) {
      System.out.println(Change + " <= " + Tolerance);
    }

    shouldStop = ((Change <= Tolerance) || (iterationCounter >= maxIterations))
      ? true : false;
    iterationCounter++;
    Change = sum_deltaR;

    return shouldStop;
  }

  /**
   *  This method computes the values for the logistic link function.
   *  <pre>f(r)=exp(r)/(1+exp(r))</pre>
   *
   * @return output value
   */
  public static double logisticLinkFunction(double r) {
    return Math.exp(r) / (1.0 + Math.exp(r));
  }

  /**
   * Sign for a given value.
   * @param r
   * @return double +1 if r>0, -1 if r<0
   */
  public static double sgn(double r) {
    double sgn = 0.0;

    if (r > 0) {
      sgn = 1.0;
    } else if (r < 0) {
      sgn = -1.0;
    }

    return sgn;
  }

  /**
   *        This function computes the norm-based hyperparameters
   *        and stores them in the m_Hyperparameters.
   */
  public double normBasedHyperParameter() {
    //TODO: Implement this method.
    Instance instance;

    double mean = 0.0;

    for (int i = 0; i < m_Instances.numInstances(); i++) {
      instance = m_Instances.instance(i);

      double sqr_sum = 0.0;

      for (int j = 0; j < m_Instances.numAttributes(); j++) {
        if (j != ClassIndex) {
          sqr_sum += (instance.value(j) * instance.value(j));
        }
      }

      //sqr_sum=Math.sqrt(sqr_sum);
      mean += sqr_sum;
    }

    mean = mean / (double) m_Instances.numInstances();

    return ((double) m_Instances.numAttributes()) / mean;
  }

  /**
   * Classifies the given instance using the Bayesian Logistic Regression function.
   *
   * @param instance the test instance
   * @return the classification
   * @throws Exception if classification can't be done successfully
   */
  public double classifyInstance(Instance instance) throws Exception {
    //TODO: Implement
    double sum_R = 0.0;
    double classification = 0.0;

    sum_R = BetaVector[0];

    for (int j = 0; j < instance.numAttributes(); j++) {
      if (j != (ClassIndex - 1)) {
        sum_R += (BetaVector[j + 1] * instance.value(j));
      }
    }

    sum_R = logisticLinkFunction(sum_R);

    if (sum_R > Threshold) {
      classification = 1.0;
    } else {
      classification = 0.0;
    }

    return classification;
  }

  /**
   * Outputs the linear regression model as a string.
   *
   * @return the model as string
   */
  public String toString() {

    if (m_Instances == null) {
      return "Bayesian logistic regression: No model built yet.";
    }

    StringBuffer buf = new StringBuffer();
    String text = "";

    switch (HyperparameterSelection) {
    case 1:
      text = "Norm-Based Hyperparameter Selection: ";

      break;

    case 2:
      text = "Cross-Validation Based Hyperparameter Selection: ";

      break;

    case 3:
      text = "Specified Hyperparameter: ";

      break;
    }

    buf.append(text).append(HyperparameterValue).append("\n\n");

    buf.append("Regression Coefficients\n");
    buf.append("=========================\n\n");

    for (int j = 0; j < m_Instances.numAttributes(); j++) {
      if (j != ClassIndex) {
        if (BetaVector[j] != 0.0) {
          buf.append(m_Instances.attribute(j).name()).append(" : ")
             .append(BetaVector[j]).append("\n");
        }
      }
    }

    buf.append("===========================\n\n");
    buf.append("Likelihood: " + m_PriorUpdate.getLoglikelihood() + "\n\n");
    buf.append("Penalty: " + m_PriorUpdate.getPenalty() + "\n\n");
    buf.append("Regularized Log Posterior: " + m_PriorUpdate.getLogPosterior() +
      "\n");
    buf.append("===========================\n\n");

    return buf.toString();
  }

  /**
   * Method computes the best hyperparameter value by doing cross
   * -validation on the training data and compute the likelihood.
   * The method can parse a range of values or a list of values.
   * @return Best hyperparameter value with the max likelihood value on the training data.
   * @throws Exception
   */
  public double CVBasedHyperparameter() throws Exception {
    //TODO: Method incomplete.
    double start;

    //TODO: Method incomplete.
    double end;

    //TODO: Method incomplete.
    double multiplier;
    int size = 0;
    double[] list = null;
    double MaxHypeValue = 0.0;
    double MaxLikelihood = 0.0;
    StringTokenizer tokenizer = new StringTokenizer(HyperparameterRange);
    String rangeType = tokenizer.nextToken(":");

    if (rangeType.equals("R")) {
      String temp = tokenizer.nextToken();
      tokenizer = new StringTokenizer(temp);
      start = Double.parseDouble(tokenizer.nextToken("-"));
      tokenizer = new StringTokenizer(tokenizer.nextToken());
      end = Double.parseDouble(tokenizer.nextToken(","));
      multiplier = Double.parseDouble(tokenizer.nextToken());

      int steps = (int) (((Math.log10(end) - Math.log10(start)) / Math.log10(multiplier)) +
        1);
      list = new double[steps];

      int count = 0;

      for (double i = start; i <= end; i *= multiplier) {
        list[count++] = i;
      }
    } else if (rangeType.equals("L")) {
      Vector vec = new Vector();

      while (tokenizer.hasMoreTokens()) {
        vec.add(tokenizer.nextToken(","));
      }

      list = new double[vec.size()];

      for (int i = 0; i < vec.size(); i++) {
        list[i] = Double.parseDouble((String) vec.get(i));
      }
    } else {
      //throw exception.  
    }

    // Perform two-fold cross-validation to collect
    // unbiased predictions
    if (list != null) {
      int numFolds = (int) NumFolds;
      Random random = new Random();
      m_Instances.randomize(random);
      m_Instances.stratify(numFolds);

      for (int k = 0; k < list.length; k++) {
        for (int i = 0; i < numFolds; i++) {
          Instances train = m_Instances.trainCV(numFolds, i, random);
          SerializedObject so = new SerializedObject(this);
          BayesianLogisticRegression blr = (BayesianLogisticRegression) so.getObject();
          //          blr.setHyperparameterSelection(3);
          blr.setHyperparameterSelection(new SelectedTag(SPECIFIC_VALUE, 
                                                         TAGS_HYPER_METHOD));
          blr.setHyperparameterValue(list[k]);
          //          blr.setPriorClass(PriorClass);
          blr.setPriorClass(new SelectedTag(PriorClass,
                                            TAGS_PRIOR));
          blr.setThreshold(Threshold);
          blr.setTolerance(Tolerance);
          blr.buildClassifier(train);

          Instances test = m_Instances.testCV(numFolds, i);
          double val = blr.getLoglikeliHood(blr.BetaVector, test);

          if (debug) {
            System.out.println("Fold " + i + "Hyperparameter: " + list[k]);
            System.out.println("===================================");
            System.out.println(" Likelihood: " + val);
          }

          if ((k == 0) | (val > MaxLikelihood)) {
            MaxLikelihood = val;
            MaxHypeValue = list[k];
          }
        }
      }
    } else {
      return HyperparameterValue;
    }

    return MaxHypeValue;
  }

  /**
   *
   * @return likelihood for a given set of betas and instances
   */
  public double getLoglikeliHood(double[] betas, Instances instances) {
    m_PriorUpdate.computelogLikelihood(betas, instances);

    return m_PriorUpdate.getLoglikelihood();
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector newVector = new Vector();

    newVector.addElement(new Option("\tShow Debugging Output\n", "D", 0, "-D"));
    newVector.addElement(new Option("\tDistribution of the Prior "
                                    +"(1=Gaussian, 2=Laplacian)"
                                    +"\n\t(default: 1=Gaussian)"
                                    , "P", 1,
                                    "-P <integer>"));
    newVector.addElement(new Option("\tHyperparameter Selection Method "
                                    +"(1=Norm-based, 2=CV-based, 3=specific value)\n"
                                    +"\t(default: 1=Norm-based)", 
                                    "H",
                                    1, 
                                    "-H <integer>"));
    newVector.addElement(new Option("\tSpecified Hyperparameter Value (use in conjunction with -H 3)\n"
                                    +"\t(default: 0.27)", 
                                    "V", 
                                    1,
                                    "-V <double>"));
    newVector.addElement(new Option(
        "\tHyperparameter Range (use in conjunction with -H 2)\n"
        +"\t(format: R:start-end,multiplier OR L:val(1), val(2), ..., val(n))\n"
        +"\t(default: R:0.01-316,3.16)", 
        "R", 
        1,
        "-R <string>"));
    newVector.addElement(new Option("\tTolerance Value\n\t(default: 0.0005)", 
                                    "Tl", 
                                    1,
                                    "-Tl <double>"));
    newVector.addElement(new Option("\tThreshold Value\n\t(default: 0.5)", 
                                    "S", 
                                    1, 
                                    "-S <double>"));
    newVector.addElement(new Option("\tNumber Of Folds (use in conjuction with -H 2)\n"
                                    +"\t(default: 2)", 
                                    "F", 
                                    1,
                                    "-F <integer>"));
    newVector.addElement(new Option("\tMax Number of Iterations\n\t(default: 100)", 
                                    "I", 
                                    1,
                                    "-I <integer>"));
    newVector.addElement(new Option("\tNormalize the data",
                                    "N", 0, "-N"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -D
   *  Show Debugging Output
   * </pre>
   * 
   * <pre> -P &lt;integer&gt;
   *  Distribution of the Prior (1=Gaussian, 2=Laplacian)
   *  (default: 1=Gaussian)</pre>
   * 
   * <pre> -H &lt;integer&gt;
   *  Hyperparameter Selection Method (1=Norm-based, 2=CV-based, 3=specific value)
   *  (default: 1=Norm-based)</pre>
   * 
   * <pre> -V &lt;double&gt;
   *  Specified Hyperparameter Value (use in conjunction with -H 3)
   *  (default: 0.27)</pre>
   * 
   * <pre> -R &lt;string&gt;
   *  Hyperparameter Range (use in conjunction with -H 2)
   *  (format: R:start-end,multiplier OR L:val(1), val(2), ..., val(n))
   *  (default: R:0.01-316,3.16)</pre>
   * 
   * <pre> -Tl &lt;double&gt;
   *  Tolerance Value
   *  (default: 0.0005)</pre>
   * 
   * <pre> -S &lt;double&gt;
   *  Threshold Value
   *  (default: 0.5)</pre>
   * 
   * <pre> -F &lt;integer&gt;
   *  Number Of Folds (use in conjuction with -H 2)
   *  (default: 2)</pre>
   * 
   * <pre> -I &lt;integer&gt;
   *  Max Number of Iterations
   *  (default: 100)</pre>
   * 
   * <pre> -N
   *  Normalize the data</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    //Debug Option
    debug = Utils.getFlag('D', options);

    // Set Tolerance.
    String Tol = Utils.getOption("Tl", options);

    if (Tol.length() != 0) {
      Tolerance = Double.parseDouble(Tol);
    }

    //Set Threshold
    String Thres = Utils.getOption('S', options);

    if (Thres.length() != 0) {
      Threshold = Double.parseDouble(Thres);
    }

    //Set Hyperparameter Type 
    String Hype = Utils.getOption('H', options);

    if (Hype.length() != 0) {
      HyperparameterSelection = Integer.parseInt(Hype);
    }

    //Set Hyperparameter Value 
    String HyperValue = Utils.getOption('V', options);

    if (HyperValue.length() != 0) {
      HyperparameterValue = Double.parseDouble(HyperValue);
    }

    // Set hyper parameter range or list.
    String HyperparameterRange = Utils.getOption("R", options);

    //Set Prior class.
    String strPrior = Utils.getOption('P', options);

    if (strPrior.length() != 0) {
      PriorClass = Integer.parseInt(strPrior);
    }

    String folds = Utils.getOption('F', options);

    if (folds.length() != 0) {
      NumFolds = Integer.parseInt(folds);
    }

    String iterations = Utils.getOption('I', options);

    if (iterations.length() != 0) {
      maxIterations = Integer.parseInt(iterations);
    }

    NormalizeData = Utils.getFlag('N', options);

    //TODO: Implement this method for other options.
    Utils.checkForRemainingOptions(options);
  }

  /**
   *
   */
  public String[] getOptions() {
    Vector result = new Vector();

    //Add Debug Mode to options.
    result.add("-D");

    //Add Tolerance value to options
    result.add("-Tl");
    result.add("" + Tolerance);

    //Add Threshold value to options
    result.add("-S");
    result.add("" + Threshold);

    //Add Hyperparameter value to options
    result.add("-H");
    result.add("" + HyperparameterSelection);

    result.add("-V");
    result.add("" + HyperparameterValue);

    result.add("-R");
    result.add("" + HyperparameterRange);

    //Add Prior Class to options
    result.add("-P");
    result.add("" + PriorClass);

    result.add("-F");
    result.add("" + NumFolds);

    result.add("-I");
    result.add("" + maxIterations);

    result.add("-N");

    return (String[]) result.toArray(new String[result.size()]);
  }

  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String[] argv) {
    runClassifier(new BayesianLogisticRegression(), argv);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String debugTipText() {
    return "Turns on debugging mode.";
  }

  /**
   *
   */
  public void setDebug(boolean debugMode) {
    debug = debugMode;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String hyperparameterSelectionTipText() {
    return "Select the type of Hyperparameter to be used.";
  }

  /**
   * Get the method used to select the hyperparameter
   *
   * @return the method used to select the hyperparameter
   */
  public SelectedTag getHyperparameterSelection() {
    return new SelectedTag(HyperparameterSelection, 
                           TAGS_HYPER_METHOD);
  }

  /**
   * Set the method used to select the hyperparameter
   *
   * @param newMethod the method used to set the hyperparameter
   */
  public void setHyperparameterSelection(SelectedTag newMethod) {
    if (newMethod.getTags() == TAGS_HYPER_METHOD) {
      int c = newMethod.getSelectedTag().getID();
      if (c >= 1 && c <= 3) {
        HyperparameterSelection = c;
      } else {
        throw new IllegalArgumentException("Wrong selection type, -H value should be: "
                                           + "1 for norm-based, 2 for CV-based and "
                                         + "3 for specific value");
      }
    }
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String priorClassTipText() {
    return "The type of prior to be used.";
  }

  /**
   * Set the type of prior to use.
   *
   * @param newMethod the type of prior to use.
   */
  public void setPriorClass(SelectedTag newMethod) {
    if (newMethod.getTags() == TAGS_PRIOR) {
      int c = newMethod.getSelectedTag().getID();
      if (c == GAUSSIAN || c == LAPLACIAN) {
        PriorClass = c;
      } else {
        throw new IllegalArgumentException("Wrong selection type, -P value should be: "
                                           + "1 for Gaussian or 2 for Laplacian");
      }
    }
  }

  /**
   * Get the type of prior to use.
   *
   * @return the type of prior to use
   */
  public SelectedTag getPriorClass() {
    return new SelectedTag(PriorClass,
                           TAGS_PRIOR);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String thresholdTipText() {
    return "Set the threshold for classifiction. The logistic function doesn't "
      + "return a class label but an estimate of p(y=+1|B,x(i)). "
      + "These estimates need to be converted to binary class label predictions. "
      + "values above the threshold are assigned class +1.";
  }

  /**
   * Return the threshold being used.
   *
   * @return the threshold
   */
  public double getThreshold() {
    return Threshold;
  }

  /**
   * Set the threshold to use.
   *
   * @param threshold the threshold to use
   */
  public void setThreshold(double threshold) {
    Threshold = threshold;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String toleranceTipText() {
    return "This value decides the stopping criterion.";
  }

  /**
   * Get the tolerance value
   *
   * @return the tolerance value
   */
  public double getTolerance() {
    return Tolerance;
  }

  /**
   * Set the tolerance value
   *
   * @param tolerance the tolerance value to use
   */
  public void setTolerance(double tolerance) {
    Tolerance = tolerance;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String hyperparameterValueTipText() {
    return "Specific hyperparameter value. Used when the hyperparameter "
      + "selection method is set to specific value";
  }

  /**
   * Get the hyperparameter value. Used when the hyperparameter
   * selection method is set to specific value
   *
   * @return the hyperparameter value
   */
  public double getHyperparameterValue() {
    return HyperparameterValue;
  }

  /**
   * Set the hyperparameter value. Used when the hyperparameter
   * selection method is set to specific value
   *
   * @param hyperparameterValue the value of the hyperparameter
   */
  public void setHyperparameterValue(double hyperparameterValue) {
    HyperparameterValue = hyperparameterValue;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numFoldsTipText() {
    return "The number of folds to use for CV-based hyperparameter selection.";
  }

  /**
   * Return the number of folds for CV-based hyperparameter selection
   *
   * @return the number of CV folds
   */
  public int getNumFolds() {
    return NumFolds;
  }

  /**
   * Set the number of folds to use for CV-based hyperparameter
   * selection
   *
   * @param numFolds number of folds to select
   */
  public void setNumFolds(int numFolds) {
    NumFolds = numFolds;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String maxIterationsTipText() {
    return "The maximum number of iterations to perform.";
  }

  /**
   * Get the maximum number of iterations to perform
   *
   * @return the maximum number of iterations
   */
  public int getMaxIterations() {
    return maxIterations;
  }

  /**
   * Set the maximum number of iterations to perform
   *
   * @param maxIterations maximum number of iterations
   */
  public void setMaxIterations(int maxIterations) {
    this.maxIterations = maxIterations;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String normalizeDataTipText() {
    return "Normalize the data.";
  }

  /**
   * Returns true if the data is to be normalized first
   *
   * @return true if the data is to be normalized
   */
  public boolean isNormalizeData() {
    return NormalizeData;
  }

  /**
   * Set whether to normalize the data or not
   *
   * @param normalizeData true if data is to be normalized
   */
  public void setNormalizeData(boolean normalizeData) {
    NormalizeData = normalizeData;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String hyperparameterRangeTipText() {
    return "Hyperparameter value range. In case of CV-based Hyperparameters, "
      + "you can specify the range in two ways: \n"
      + "Comma-Separated: L: 3,5,6 (This will be a list of possible values.)\n"
      + "Range: R:0.01-316,3.16 (This will take values from 0.01-316 (inclusive) "
      + "in multiplications of 3.16";
  }

  /**
   * Get the range of hyperparameter values to consider
   * during CV-based selection.
   *
   * @return the range of hyperparameters as a Stringe
   */
  public String getHyperparameterRange() {
    return HyperparameterRange;
  }

  /**
   * Set the range of hyperparameter values to consider
   * during CV-based selection
   *
   * @param hyperparameterRange the range of hyperparameter values
   */
  public void setHyperparameterRange(String hyperparameterRange) {
    HyperparameterRange = hyperparameterRange;
  }

  /**
   * Returns true if debug is turned on.
   *
   * @return true if debug is turned on
   */
  public boolean isDebug() {
    return debug;
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 1.3 $");
  }
}

