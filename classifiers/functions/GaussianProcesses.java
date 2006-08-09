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
 *    LinearRegression.java
 *    Copyright (C) 1999 Eibe Frank,Len Trigg
 *
 */

package weka.classifiers.functions;

import weka.classifiers.Classifier;
import weka.classifiers.IntervalEstimator;
import weka.classifiers.functions.supportVector.Kernel;
import weka.classifiers.functions.supportVector.PolyKernel;
import weka.classifiers.functions.supportVector.RBFKernel;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.SelectedTag;
import weka.core.Statistics;
import weka.core.Tag;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NominalToBinary;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import weka.filters.unsupervised.attribute.Standardize;

import java.util.Enumeration;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * Implements Gaussian Processes for regression without hyperparameter-tuning. For more information see<br/>
 * <br/>
 * David J.C. Mackay (1998). Introduction to Gaussian Processes. Dept. of Physics, Cambridge University, UK.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;misc{Mackay1998,
 *    address = {Dept. of Physics, Cambridge University, UK},
 *    author = {David J.C. Mackay},
 *    title = {Introduction to Gaussian Processes},
 *    year = {1998},
 *    PS = {http://wol.ra.phy.cam.ac.uk/mackay/gpB.ps.gz}
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
 * <pre> -L &lt;double&gt;
 *  Level of Gaussian Noise. (default 0.1)</pre>
 * 
 * <pre> -N
 *  Whether to 0=normalize/1=standardize/2=neither. (default 0=normalize)</pre>
 * 
 * <pre> -K &lt;classname and parameters&gt;
 *  The Kernel to use.
 *  (default: weka.classifiers.functions.supportVector.PolyKernel)</pre>
 * 
 * <pre> 
 * Options specific to kernel weka.classifiers.functions.supportVector.RBFKernel:
 * </pre>
 * 
 * <pre> -D
 *  Enables debugging output (if available) to be printed.
 *  (default: off)</pre>
 * 
 * <pre> -no-checks
 *  Turns off all checks - use with caution!
 *  (default: checks on)</pre>
 * 
 * <pre> -C &lt;num&gt;
 *  The size of the cache (a prime number).
 *  (default: 250007)</pre>
 * 
 * <pre> -G &lt;num&gt;
 *  The Gamma parameter.
 *  (default: 0.01)</pre>
 * 
 <!-- options-end -->
 * 
 * @author Kurt Driessens (kurtd@cs.waikato.ac.nz)
 * @version $Revision: 1.6 $
 */
public class GaussianProcesses 
  extends Classifier 
  implements OptionHandler, IntervalEstimator, TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = -8620066949967678545L;
  
  /** The filter used to make attributes numeric. */
  protected NominalToBinary m_NominalToBinary;
  
  /** normalizes the data */
  public static final int FILTER_NORMALIZE = 0;
  /** standardizes the data */
  public static final int FILTER_STANDARDIZE = 1;
  /** no filter */
  public static final int FILTER_NONE = 2;
  /** The filter to apply to the training data */
  public static final Tag [] TAGS_FILTER = {
    new Tag(FILTER_NORMALIZE, "Normalize training data"),
    new Tag(FILTER_STANDARDIZE, "Standardize training data"),
    new Tag(FILTER_NONE, "No normalization/standardization"),
  };
    
  /** The filter used to standardize/normalize all values. */
  protected Filter m_Filter = null;
    
  /** Whether to normalize/standardize/neither */
  protected int m_filterType = FILTER_NORMALIZE;
  
  /** The filter used to get rid of missing values. */
  protected ReplaceMissingValues m_Missing;
    
  /** Turn off all checks and conversions? Turning them off assumes
      that data is purely numeric, doesn't contain any missing values,
      and has a numeric class. */
  protected boolean m_checksTurnedOff = false;

  /** Gaussian Noise Value. */
  protected double m_delta = 1.0;

  /** The class index from the training data */
  protected int m_classIndex = -1;

  /** The parameters of the linear transforamtion realized 
   * by the filter on the class attribute */
  protected double m_Alin;
  protected double m_Blin;

  /** Kernel to use **/
  protected Kernel m_kernel = null;
    
  /** The number of training instances */
  protected int m_NumTrain = 0;
  
  /** The training data. */
  protected double m_avg_target;
    
  /** The covariance matrix. */
  protected weka.core.matrix.Matrix m_C;
    
  /** The vector of target values. */
  protected weka.core.matrix.Matrix m_t;
  
  /** whether the kernel is a linear one */
  protected boolean m_KernelIsLinear = false;

  /**
   * the default constructor
   */
  public GaussianProcesses() {
    super();
    
    m_kernel = new RBFKernel();
    ((RBFKernel) m_kernel).setGamma(1.0);
  }
  
  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return  "Implements Gaussian Processes for regression "
	+ "without hyperparameter-tuning. "
	+ "For more information see\n\n"
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
    
    result = new TechnicalInformation(Type.MISC);
    result.setValue(Field.AUTHOR, "David J.C. Mackay");
    result.setValue(Field.YEAR, "1998");
    result.setValue(Field.TITLE, "Introduction to Gaussian Processes");
    result.setValue(Field.ADDRESS, "Dept. of Physics, Cambridge University, UK");
    result.setValue(Field.PS, "http://wol.ra.phy.cam.ac.uk/mackay/gpB.ps.gz");
    
    return result;
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = getKernel().getCapabilities();
    result.setOwner(this);

    // attribute
    result.enableAllAttributeDependencies();
    // with NominalToBinary we can also handle nominal attributes, but only
    // if the kernel can handle numeric attributes
    if (result.handles(Capability.NUMERIC_ATTRIBUTES))
      result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);
    
    // class
    result.disableAllClasses();
    result.disableAllClassDependencies();
    result.enable(Capability.NUMERIC_CLASS);
    result.enable(Capability.DATE_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);
    
    return result;
  }

  /**
   * Method for building the classifier. 
   *
   * @param insts the set of training instances
   * @throws Exception if the classifier can't be built successfully
   */
  public void buildClassifier(Instances insts) throws Exception {

    /* check the set of training instances */
    if (!m_checksTurnedOff) {
      // can classifier handle the data?
      getCapabilities().testWithFail(insts);

      // remove instances with missing class
      insts = new Instances(insts);
      insts.deleteWithMissingClass();
    }
      
    if (!m_checksTurnedOff) {
      m_Missing = new ReplaceMissingValues();
      m_Missing.setInputFormat(insts);
      insts = Filter.useFilter(insts, m_Missing); 
    } else {
      m_Missing = null;
    }

    if (getCapabilities().handles(Capability.NUMERIC_ATTRIBUTES)) {
      boolean onlyNumeric = true;
      if (!m_checksTurnedOff) {
	for (int i = 0; i < insts.numAttributes(); i++) {
	  if (i != insts.classIndex()) {
	    if (!insts.attribute(i).isNumeric()) {
	      onlyNumeric = false;
	      break;
	    }
	  }
	}
      }
      
      if (!onlyNumeric) {
	m_NominalToBinary = new NominalToBinary();
	m_NominalToBinary.setInputFormat(insts);
	insts = Filter.useFilter(insts, m_NominalToBinary);
      } else {
	m_NominalToBinary = null;
      }
    }
    else {
      m_NominalToBinary = null;
    }

    m_classIndex = insts.classIndex();
    if (m_filterType == FILTER_STANDARDIZE) {
      m_Filter = new Standardize();
      //((Standardize)m_Filter).setIgnoreClass(true);
      m_Filter.setInputFormat(insts);
      insts = Filter.useFilter(insts, m_Filter); 
    } else if (m_filterType == FILTER_NORMALIZE) {
      m_Filter = new Normalize();
      //((Normalize)m_Filter).setIgnoreClass(true);
      m_Filter.setInputFormat(insts);
      insts = Filter.useFilter(insts, m_Filter); 
    } else {
      m_Filter = null;
    }

    m_NumTrain = insts.numInstances();

    // determine which linear transformation has been 
    // applied to the class by the filter
    if (m_Filter != null) {
      Instance witness = (Instance)insts.instance(0).copy();
      witness.setValue(m_classIndex, 0);
      m_Filter.input(witness);
      m_Filter.batchFinished();
      Instance res = m_Filter.output();
      m_Blin = res.value(m_classIndex);
      witness.setValue(m_classIndex, 1);
      m_Filter.input(witness);
      m_Filter.batchFinished();
      res = m_Filter.output();
      m_Alin = res.value(m_classIndex) - m_Blin;
    } else {
      m_Alin = 1.0;
      m_Blin = 0.0;
    }

    // Initialize kernel
    m_kernel.buildKernel(insts);
    m_KernelIsLinear = (m_kernel instanceof PolyKernel) && (((PolyKernel) m_kernel).getExponent() == 1.0);

    // Build Inverted Covariance Matrix

    m_C = new weka.core.matrix.Matrix(insts.numInstances(),insts.numInstances());
    double kv;
    double sum = 0.0;

    for (int i = 0; i < insts.numInstances(); i++) {
	sum += insts.instance(i).classValue();
      for (int j = 0; j < i; j++) {
	kv = m_kernel.eval(i,j,insts.instance(i));
	m_C.set(i,j,kv);
	m_C.set(j,i,kv);
      }
      kv = m_kernel.eval(i,i,insts.instance(i));
      m_C.set(i,i,kv+(m_delta*m_delta));
    }

    m_avg_target = sum/insts.numInstances();

    //weka.core.matrix.CholeskyDecomposition cd = new weka.core.matrix.CholeskyDecomposition(m_C);

    //if (!cd.isSPD())
    //throw new Exception("No semi-positive-definite kernel?!?");

    weka.core.matrix.LUDecomposition lu = new weka.core.matrix.LUDecomposition(m_C);
    if (!lu.isNonsingular())
	throw new Exception("Singular Matrix?!?");

    weka.core.matrix.Matrix iMat = weka.core.matrix.Matrix.identity(insts.numInstances(),insts.numInstances());

    m_C = lu.solve(iMat);

    m_t = new weka.core.matrix.Matrix(insts.numInstances(),1);

    for (int i = 0; i < insts.numInstances(); i++) 
	m_t.set(i,0,insts.instance(i).classValue()-m_avg_target);

    m_t = m_C.times(m_t);

  }

  /**
   * Classifies a given instance.
   *
   * @param inst the instance to be classified
   * @return the classification
   * @throws Exception if instance could not be classified
   * successfully
   */
  public double classifyInstance(Instance inst) throws Exception {

    // Filter instance
    if (!m_checksTurnedOff) {
      m_Missing.input(inst);
      m_Missing.batchFinished();
      inst = m_Missing.output();
    }

    if (m_NominalToBinary != null) {
      m_NominalToBinary.input(inst);
      m_NominalToBinary.batchFinished();
      inst = m_NominalToBinary.output();
    }
	
    if (m_Filter != null) {
      m_Filter.input(inst);
      m_Filter.batchFinished();
      inst = m_Filter.output();
    }

    // Build K vector

    weka.core.matrix.Matrix k = new weka.core.matrix.Matrix(m_NumTrain,1);
    for (int i = 0; i < m_NumTrain; i++) 
      k.set(i,0,m_kernel.eval(-1,i,inst));
      
    double result = k.transpose().times(m_t).get(0,0)+m_avg_target;

    return result;

  }

  /**
   * Predicts a confidence interval for the given instance and confidence level.
   *
   * @param inst the instance to make the prediction for
   * @param confidenceLevel the percentage of cases the interval should cover
   * @return a 1*2 array that contains the boundaries of the interval
   * @throws Exception if interval could not be estimated
   * successfully
   */
  public double[][] predictInterval(Instance inst, double confidenceLevel) throws Exception {

    // Filter instance
    if (!m_checksTurnedOff) {
      m_Missing.input(inst);
      m_Missing.batchFinished();
      inst = m_Missing.output();
    }

    if (m_NominalToBinary != null) {
      m_NominalToBinary.input(inst);
      m_NominalToBinary.batchFinished();
      inst = m_NominalToBinary.output();
    }
	
    if (m_Filter != null) {
      m_Filter.input(inst);
      m_Filter.batchFinished();
      inst = m_Filter.output();
    }

    // Build K vector (and Kappa)

    weka.core.matrix.Matrix k = new weka.core.matrix.Matrix(m_NumTrain,1);
    for (int i = 0; i < m_NumTrain; i++) 
      k.set(i,0,m_kernel.eval(-1,i,inst));
      
    double kappa = m_kernel.eval(-1,-1,inst) + m_delta*m_delta;
      
    double estimate = k.transpose().times(m_t).get(0,0)+m_avg_target;

    double sigma = Math.sqrt(kappa - k.transpose().times(m_C).times(k).get(0,0));

    confidenceLevel = 1.0 - ((1.0 - confidenceLevel)/2.0);

    double z = Statistics.normalInverse(confidenceLevel);
    
    double[][] interval = new double[1][2];

    interval[0][0] = estimate - z * sigma;
    interval[0][1] = estimate + z * sigma;

    return interval;
    
  }
  
  /**
   * Gives the variance of the prediction at the given instance
   *
   * @param inst the instance to get the variance for
   * @return tha variance
   * @throws Exception if computation fails
   */
    public double getStandardDeviation(Instance inst) throws Exception {

    // Filter instance
    if (!m_checksTurnedOff) {
      m_Missing.input(inst);
      m_Missing.batchFinished();
      inst = m_Missing.output();
    }

    if (m_NominalToBinary != null) {
      m_NominalToBinary.input(inst);m_Alin = 1.0;
      m_Blin = 0.0;

      m_NominalToBinary.batchFinished();
      inst = m_NominalToBinary.output();
    }
	
    if (m_Filter != null) {
      m_Filter.input(inst);
      m_Filter.batchFinished();
      inst = m_Filter.output();
    }

    weka.core.matrix.Matrix k = new weka.core.matrix.Matrix(m_NumTrain,1);
    for (int i = 0; i < m_NumTrain; i++) 
      k.set(i,0,m_kernel.eval(-1,i,inst));
      
    double kappa = m_kernel.eval(-1,-1,inst) + m_delta*m_delta;
    
    double var = kappa - k.transpose().times(m_C).times(k).get(0,0);

    if (var < 0) System.out.println("Aiaiai: variance is negative (" + var + ")!!!");
  
    double sigma = Math.sqrt(var);

    return sigma;
    }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
	
    Vector result = new Vector();

    Enumeration enm = super.listOptions();
    while (enm.hasMoreElements())
      result.addElement(enm.nextElement());

    result.addElement(new Option(
	"\tLevel of Gaussian Noise."
	+ " (default 0.1)",
	"L", 1, "-L <double>"));
    
    result.addElement(new Option(
	"\tWhether to 0=normalize/1=standardize/2=neither. " +
	"(default 0=normalize)",
	"N", 1, "-N"));
    
    result.addElement(new Option(
	"\tThe Kernel to use.\n"
	+ "\t(default: weka.classifiers.functions.supportVector.PolyKernel)",
	"K", 1, "-K <classname and parameters>"));

    result.addElement(new Option(
	"",
	"", 0, "\nOptions specific to kernel "
	+ getKernel().getClass().getName() + ":"));
    
    enm = ((OptionHandler) getKernel()).listOptions();
    while (enm.hasMoreElements())
      result.addElement(enm.nextElement());

    return result.elements();
  }
    
    
  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   * <pre> -L &lt;double&gt;
   *  Level of Gaussian Noise. (default 0.1)</pre>
   * 
   * <pre> -N
   *  Whether to 0=normalize/1=standardize/2=neither. (default 0=normalize)</pre>
   * 
   * <pre> -K &lt;classname and parameters&gt;
   *  The Kernel to use.
   *  (default: weka.classifiers.functions.supportVector.PolyKernel)</pre>
   * 
   * <pre> 
   * Options specific to kernel weka.classifiers.functions.supportVector.RBFKernel:
   * </pre>
   * 
   * <pre> -D
   *  Enables debugging output (if available) to be printed.
   *  (default: off)</pre>
   * 
   * <pre> -no-checks
   *  Turns off all checks - use with caution!
   *  (default: checks on)</pre>
   * 
   * <pre> -C &lt;num&gt;
   *  The size of the cache (a prime number).
   *  (default: 250007)</pre>
   * 
   * <pre> -G &lt;num&gt;
   *  The Gamma parameter.
   *  (default: 0.01)</pre>
   * 
   <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported 
   */
  public void setOptions(String[] options) throws Exception {
    String	tmpStr;
    String[]	tmpOptions;
    
    tmpStr = Utils.getOption('L', options);
    if (tmpStr.length() != 0)
      setNoise(Double.parseDouble(tmpStr));
    else
      setNoise(0.1);

    tmpStr = Utils.getOption('N', options);
    if (tmpStr.length() != 0)
      setFilterType(new SelectedTag(Integer.parseInt(tmpStr), TAGS_FILTER));
    else
      setFilterType(new SelectedTag(FILTER_NORMALIZE, TAGS_FILTER));

    tmpStr     = Utils.getOption('K', options);
    tmpOptions = Utils.splitOptions(tmpStr);
    if (tmpOptions.length != 0) {
      tmpStr        = tmpOptions[0];
      tmpOptions[0] = "";
      setKernel(Kernel.forName(tmpStr, tmpOptions));
    }
    
    super.setOptions(options);
  }

  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    int       i;
    Vector    result;
    String[]  options;

    result = new Vector();
    options = super.getOptions();
    for (i = 0; i < options.length; i++)
      result.add(options[i]);

    result.add("-L");
    result.add("" + getNoise());
    
    result.add("-N");
    result.add("" + m_filterType);
    
    result.add("-K");
    result.add("" + m_kernel.getClass().getName() + " " + Utils.joinOptions(m_kernel.getOptions()));
    
    return (String[]) result.toArray(new String[result.size()]);	  
  }

  /**
   * Returns the tip text for this property
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String kernelTipText() {
    return "The kernel to use.";
  }

  /**
   * Gets the kernel to use.
   *
   * @return 		the kernel
   */
  public Kernel getKernel() {
    return m_kernel;
  }
    
  /**
   * Sets the kernel to use.
   *
   * @param value	the new kernel
   */
  public void setKernel(Kernel value) {
    m_kernel = value;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String filterTypeTipText() {
    return "Determines how/if the data will be transformed.";
  }

  /**
   * Gets how the training data will be transformed. Will be one of
   * FILTER_NORMALIZE, FILTER_STANDARDIZE, FILTER_NONE.2200Instances
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
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String noiseTipText() {
    return "The level of Gaussian Noise (added to the diagonal of the Covariance Matrix).";
  }
  
  /**
   * Get the value of noise. 
   *
   * @return Value of noise.
   */
  public double getNoise() {
    return m_delta;
  }
  
  /**
   * Set the level of Gaussian Noise. 
   *
   * @param v  Value to assign to noise.
   */
  public void setNoise(double v) {
    m_delta = v;
  }

  /**
   * Prints out the classifier.
   *
   * @return a description of the classifier as a string
   */
  public String toString() {

    StringBuffer text = new StringBuffer();

    if (m_t == null)
      return "Gaussian Processes: No model built yet.";

    try {

      text.append("Gaussian Processes\n\n");
      text.append("Kernel used:\n  " + m_kernel.toString() + "\n\n");

      text.append("Average Target Value : " + m_avg_target + "\n");

      text.append("Inverted Covariance Matrix:\n");
      double min = m_C.get(0,0);
      double max = m_C.get(0,0);
      for (int i = 0; i < m_NumTrain; i++)
	for (int j = 0; j < m_NumTrain; j++) {
	    if (m_C.get(i,j) < min) min = m_C.get(i,j);
	    else if (m_C.get(i,j) > max) max = m_C.get(i,j);
	}
      text.append("    Lowest Value = " + min + "\n");
      text.append("    Highest Value = " + max + "\n");
      text.append("Inverted Covariance Matrix * Target-value Vector:\n");
      min = m_t.get(0,0);
      max = m_t.get(0,0);
      for (int i = 0; i < m_NumTrain; i++) {
	    if (m_t.get(i,0) < min) min = m_t.get(i,0);
	    else if (m_t.get(i,0) > max) max = m_t.get(i,0);
	}
      text.append("    Lowest Value = " + min + "\n");
      text.append("    Highest Value = " + max + "\n \n");   
      
    } catch (Exception e) {
      return "Can't print the classifier.";
    }

    return text.toString();
  }
 
 /**
   * Main method for testing this class.
   * 
   * @param argv the commandline parameters
   */
  public static void main(String[] argv) {
    runClassifier(new GaussianProcesses(), argv);
  }
}
