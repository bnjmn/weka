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
 *    MultiClassClassifier.java
 *    Copyright (C) 1999 Eibe Frank,Len Trigg
 *
 */

package weka.classifiers.meta;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.RandomizableSingleClassifierEnhancer;
import weka.classifiers.rules.ZeroR;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Range;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.MakeIndicator;
import weka.filters.unsupervised.instance.RemoveWithValues;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * A metaclassifier for handling multi-class datasets with 2-class classifiers. This classifier is also capable of applying error correcting output codes for increased accuracy.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -M &lt;num&gt;
 *  Sets the method to use. Valid values are 0 (1-against-all),
 *  1 (random codes), 2 (exhaustive code), and 3 (1-against-1). (default 0)
 * </pre>
 * 
 * <pre> -R &lt;num&gt;
 *  Sets the multiplier when using random codes. (default 2.0)</pre>
 * 
 * <pre> -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)</pre>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -W
 *  Full name of base classifier.
 *  (default: weka.classifiers.functions.Logistic)</pre>
 * 
 * <pre> 
 * Options specific to classifier weka.classifiers.functions.Logistic:
 * </pre>
 * 
 * <pre> -D
 *  Turn on debugging output.</pre>
 * 
 * <pre> -R &lt;ridge&gt;
 *  Set the ridge in the log-likelihood.</pre>
 * 
 * <pre> -M &lt;number&gt;
 *  Set the maximum number of iterations (default -1, until convergence).</pre>
 * 
 <!-- options-end -->
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Len Trigg (len@reeltwo.com)
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 1.41 $
 */
public class MultiClassClassifier 
  extends RandomizableSingleClassifierEnhancer 
  implements OptionHandler {

  /** for serialization */
  static final long serialVersionUID = -3879602011542849141L;
  
  /** The classifiers. */
  private Classifier [] m_Classifiers;

  /** The filters used to transform the class. */
  private Filter[] m_ClassFilters;

  /** ZeroR classifier for when all base classifier return zero probability. */
  private ZeroR m_ZeroR;

  /** Internal copy of the class attribute for output purposes */
  private Attribute m_ClassAttribute;
  
  /** A transformed dataset header used by the  1-against-1 method */
  private Instances m_TwoClassDataset;

  /** 
   * The multiplier when generating random codes. Will generate
   * numClasses * m_RandomWidthFactor codes
   */
  private double m_RandomWidthFactor = 2.0;

  /** The multiclass method to use */
  private int m_Method = METHOD_1_AGAINST_ALL;

  /** 1-against-all */
  public static final int METHOD_1_AGAINST_ALL    = 0;
  /** random correction code */
  public static final int METHOD_ERROR_RANDOM     = 1;
  /** exhaustive correction code */
  public static final int METHOD_ERROR_EXHAUSTIVE = 2;
  /** 1-against-1 */
  public static final int METHOD_1_AGAINST_1      = 3;
  /** The error correction modes */
  public static final Tag [] TAGS_METHOD = {
    new Tag(METHOD_1_AGAINST_ALL, "1-against-all"),
    new Tag(METHOD_ERROR_RANDOM, "Random correction code"),
    new Tag(METHOD_ERROR_EXHAUSTIVE, "Exhaustive correction code"),
    new Tag(METHOD_1_AGAINST_1, "1-against-1")
  };
    
  /**
   * Constructor.
   */
  public MultiClassClassifier() {
    
    m_Classifier = new weka.classifiers.functions.Logistic();
  }

  /**
   * String describing default classifier.
   * 
   * @return the default classifier classname
   */
  protected String defaultClassifierString() {
    
    return "weka.classifiers.functions.Logistic";
  }

  /** 
   * Interface for the code constructors 
   */
  private abstract class Code 
    implements Serializable {

    /** for serialization */
    static final long serialVersionUID = 418095077487120846L;
    
    /**
     * Subclasses must allocate and fill these. 
     * First dimension is number of codes.
     * Second dimension is number of classes.
     */
    protected boolean [][]m_Codebits;

    /** 
     * Returns the number of codes. 
     * @return the number of codes
     */
    public int size() {
      return m_Codebits.length;
    }

    /** 
     * Returns the indices of the values set to true for this code, 
     * using 1-based indexing (for input to Range).
     * 
     * @param which the index
     * @return the 1-based indices
     */
    public String getIndices(int which) {
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < m_Codebits[which].length; i++) {
        if (m_Codebits[which][i]) {
          if (sb.length() != 0) {
            sb.append(',');
          }
          sb.append(i + 1);
        }
      }
      return sb.toString();
    }

    /** 
     * Returns a human-readable representation of the codes. 
     * @return a string representation of the codes
     */
    public String toString() {
      StringBuffer sb = new StringBuffer();
      for(int i = 0; i < m_Codebits[0].length; i++) {
        for (int j = 0; j < m_Codebits.length; j++) {
          sb.append(m_Codebits[j][i] ? " 1" : " 0");
        }
        sb.append('\n');
      }
      return sb.toString();
    }
  }

  /** 
   * Constructs a code with no error correction 
   */
  private class StandardCode 
    extends Code {
    
    /** for serialization */
    static final long serialVersionUID = 3707829689461467358L;
    
    /**
     * constructor
     * 
     * @param numClasses the number of classes
     */
    public StandardCode(int numClasses) {
      m_Codebits = new boolean[numClasses][numClasses];
      for (int i = 0; i < numClasses; i++) {
        m_Codebits[i][i] = true;
      }
      //System.err.println("Code:\n" + this);
    }
  }

  /** 
   * Constructs a random code assignment 
   */
  private class RandomCode 
    extends Code {

    /** for serialization */
    static final long serialVersionUID = 4413410540703926563L;
    
    /** random number generator */
    Random r = null;
   
    /**
     * constructor
     * 
     * @param numClasses the number of classes
     * @param numCodes the number of codes
     * @param data the data to use
     */
    public RandomCode(int numClasses, int numCodes, Instances data) {
      r = data.getRandomNumberGenerator(m_Seed);
      numCodes = Math.max(2, numCodes); // Need at least two classes
      m_Codebits = new boolean[numCodes][numClasses];
      int i = 0;
      do {
        randomize();
        //System.err.println(this);
      } while (!good() && (i++ < 100));
      //System.err.println("Code:\n" + this);
    }

    private boolean good() {
      boolean [] ninClass = new boolean[m_Codebits[0].length];
      boolean [] ainClass = new boolean[m_Codebits[0].length];
      for (int i = 0; i < ainClass.length; i++) {
	ainClass[i] = true;
      }

      for (int i = 0; i < m_Codebits.length; i++) {
        boolean ninCode = false;
        boolean ainCode = true;
        for (int j = 0; j < m_Codebits[i].length; j++) {
          boolean current = m_Codebits[i][j];
          ninCode = ninCode || current;
          ainCode = ainCode && current;
          ninClass[j] = ninClass[j] || current;
          ainClass[j] = ainClass[j] && current;
        }
        if (!ninCode || ainCode) {
          return false;
        }
      }
      for (int j = 0; j < ninClass.length; j++) {
        if (!ninClass[j] || ainClass[j]) {
          return false;
        }
      }
      return true;
    }

    /**
     * randomizes
     */
    private void randomize() {
      for (int i = 0; i < m_Codebits.length; i++) {
        for (int j = 0; j < m_Codebits[i].length; j++) {
	  double temp = r.nextDouble();
          m_Codebits[i][j] = (temp < 0.5) ? false : true;
        }
      }
    }
  }

  /*
   * TODO: Constructs codes as per:
   * Bose, R.C., Ray Chaudhuri (1960), On a class of error-correcting
   * binary group codes, Information and Control, 3, 68-79.
   * Hocquenghem, A. (1959) Codes corecteurs d'erreurs, Chiffres, 2, 147-156. 
   */
  //private class BCHCode extends Code {...}

  /** Constructs an exhaustive code assignment */
  private class ExhaustiveCode 
    extends Code {

    /** for serialization */
    static final long serialVersionUID = 8090991039670804047L;
    
    /**
     * constructor
     * 
     * @param numClasses the number of classes
     */
    public ExhaustiveCode(int numClasses) {
      int width = (int)Math.pow(2, numClasses - 1) - 1;
      m_Codebits = new boolean[width][numClasses];
      for (int j = 0; j < width; j++) {
        m_Codebits[j][0] = true;
      }
      for (int i = 1; i < numClasses; i++) {
        int skip = (int) Math.pow(2, numClasses - (i + 1));
        for(int j = 0; j < width; j++) {
          m_Codebits[j][i] = ((j / skip) % 2 != 0);
        }
      }
      //System.err.println("Code:\n" + this);
    }
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // class
    result.disableAllClasses();
    result.disableAllClassDependencies();
    result.enable(Capability.NOMINAL_CLASS);
    
    return result;
  }

  /**
   * Builds the classifiers.
   *
   * @param insts the training data.
   * @throws Exception if a classifier can't be built
   */
  public void buildClassifier(Instances insts) throws Exception {

    Instances newInsts;

    // can classifier handle the data?
    getCapabilities().testWithFail(insts);

    // remove instances with missing class
    insts = new Instances(insts);
    insts.deleteWithMissingClass();
    
    if (m_Classifier == null) {
      throw new Exception("No base classifier has been set!");
    }
    m_ZeroR = new ZeroR();
    m_ZeroR.buildClassifier(insts);

    m_TwoClassDataset = null;

    int numClassifiers = insts.numClasses();
    if (numClassifiers <= 2) {

      m_Classifiers = Classifier.makeCopies(m_Classifier, 1);
      m_Classifiers[0].buildClassifier(insts);

      m_ClassFilters = null;

    } else if (m_Method == METHOD_1_AGAINST_1) {
 
      // generate fastvector of pairs
      FastVector pairs = new FastVector();
      for (int i=0; i<insts.numClasses(); i++) {
	for (int j=0; j<insts.numClasses(); j++) {
	  if (j<=i) continue;
	  int[] pair = new int[2];
	  pair[0] = i; pair[1] = j;
	  pairs.addElement(pair);
	}
      }

      numClassifiers = pairs.size();
      m_Classifiers = Classifier.makeCopies(m_Classifier, numClassifiers);
      m_ClassFilters = new Filter[numClassifiers];

      // generate the classifiers
      for (int i=0; i<numClassifiers; i++) {
	RemoveWithValues classFilter = new RemoveWithValues();
	classFilter.setAttributeIndex("" + (insts.classIndex() + 1));
	classFilter.setModifyHeader(true);
	classFilter.setInvertSelection(true);
	classFilter.setNominalIndicesArr((int[])pairs.elementAt(i));
	Instances tempInstances = new Instances(insts, 0);
	tempInstances.setClassIndex(-1);
	classFilter.setInputFormat(tempInstances);
	newInsts = Filter.useFilter(insts, classFilter);
	if (newInsts.numInstances() > 0) {
	  newInsts.setClassIndex(insts.classIndex());
	  m_Classifiers[i].buildClassifier(newInsts);
	  m_ClassFilters[i] = classFilter;
	} else {
	  m_Classifiers[i] = null;
	  m_ClassFilters[i] = null;
	}
      }

      // construct a two-class header version of the dataset
      m_TwoClassDataset = new Instances(insts, 0);
      int classIndex = m_TwoClassDataset.classIndex();
      m_TwoClassDataset.setClassIndex(-1);
      m_TwoClassDataset.deleteAttributeAt(classIndex);
      FastVector classLabels = new FastVector();
      classLabels.addElement("class0");
      classLabels.addElement("class1");
      m_TwoClassDataset.insertAttributeAt(new Attribute("class", classLabels),
					  classIndex);
      m_TwoClassDataset.setClassIndex(classIndex);

    } else { // use error correcting code style methods
      Code code = null;
      switch (m_Method) {
      case METHOD_ERROR_EXHAUSTIVE:
        code = new ExhaustiveCode(numClassifiers);
        break;
      case METHOD_ERROR_RANDOM:
        code = new RandomCode(numClassifiers, 
                              (int)(numClassifiers * m_RandomWidthFactor),
			      insts);
        break;
      case METHOD_1_AGAINST_ALL:
        code = new StandardCode(numClassifiers);
        break;
      default:
        throw new Exception("Unrecognized correction code type");
      }
      numClassifiers = code.size();
      m_Classifiers = Classifier.makeCopies(m_Classifier, numClassifiers);
      m_ClassFilters = new MakeIndicator[numClassifiers];
      for (int i = 0; i < m_Classifiers.length; i++) {
	m_ClassFilters[i] = new MakeIndicator();
	MakeIndicator classFilter = (MakeIndicator) m_ClassFilters[i];
	classFilter.setAttributeIndex("" + (insts.classIndex() + 1));
	classFilter.setValueIndices(code.getIndices(i));
	classFilter.setNumeric(false);
	classFilter.setInputFormat(insts);
	newInsts = Filter.useFilter(insts, m_ClassFilters[i]);
	m_Classifiers[i].buildClassifier(newInsts);
      }
    }
    m_ClassAttribute = insts.classAttribute();
  }

  /**
   * Returns the individual predictions of the base classifiers
   * for an instance. Used by StackedMultiClassClassifier.
   * Returns the probability for the second "class" predicted
   * by each base classifier.
   *
   * @param inst the instance to get the prediction for
   * @return the individual predictions
   * @throws Exception if the predictions can't be computed successfully
   */
  public double[] individualPredictions(Instance inst) throws Exception {
    
    double[] result = null;

    if (m_Classifiers.length == 1) {
      result = new double[1];
      result[0] = m_Classifiers[0].distributionForInstance(inst)[1];
    } else {
      result = new double[m_ClassFilters.length];
      for(int i = 0; i < m_ClassFilters.length; i++) {
	if (m_Classifiers[i] != null) {
	  if (m_Method == METHOD_1_AGAINST_1) {    
	    Instance tempInst = (Instance)inst.copy(); 
	    tempInst.setDataset(m_TwoClassDataset);
	    result[i] = m_Classifiers[i].distributionForInstance(tempInst)[1];  
	  } else {
	    m_ClassFilters[i].input(inst);
	    m_ClassFilters[i].batchFinished();
	    result[i] = m_Classifiers[i].
	      distributionForInstance(m_ClassFilters[i].output())[1];
	  }
	}
      }
    }
    return result;
  }

  /**
   * Returns the distribution for an instance.
   *
   * @param inst the instance to get the distribution for
   * @return the distribution
   * @throws Exception if the distribution can't be computed successfully
   */
  public double[] distributionForInstance(Instance inst) throws Exception {
    
    if (m_Classifiers.length == 1) {
      return m_Classifiers[0].distributionForInstance(inst);
    }
    
    double[] probs = new double[inst.numClasses()];

    if (m_Method == METHOD_1_AGAINST_1) {    
      for(int i = 0; i < m_ClassFilters.length; i++) {
	if (m_Classifiers[i] != null) {
	  Instance tempInst = (Instance)inst.copy(); 
	  tempInst.setDataset(m_TwoClassDataset);
	  double [] current = m_Classifiers[i].distributionForInstance(tempInst);  
	  Range range = new Range(((RemoveWithValues)m_ClassFilters[i])
				  .getNominalIndices());
	  range.setUpper(m_ClassAttribute.numValues());
	  int[] pair = range.getSelection();
	  if (current[0] > current[1]) probs[pair[0]] += 1.0;
	  else probs[pair[1]] += 1.0;
	}
      }
    } else {
      // error correcting style methods
      for(int i = 0; i < m_ClassFilters.length; i++) {
	m_ClassFilters[i].input(inst);
	m_ClassFilters[i].batchFinished();
	double [] current = m_Classifiers[i].
	  distributionForInstance(m_ClassFilters[i].output());
	for (int j = 0; j < m_ClassAttribute.numValues(); j++) {
	  if (((MakeIndicator)m_ClassFilters[i]).getValueRange().isInRange(j)) {
	    probs[j] += current[1];
	  } else {
	    probs[j] += current[0];
	  }
	}
      }
    }
    
    if (Utils.gr(Utils.sum(probs), 0)) {
      Utils.normalize(probs);
      return probs;
    } else {
      return m_ZeroR.distributionForInstance(inst);
    }
  }

  /**
   * Prints the classifiers.
   * 
   * @return a string representation of the classifier
   */
  public String toString() {

    if (m_Classifiers == null) {
      return "MultiClassClassifier: No model built yet.";
    }
    StringBuffer text = new StringBuffer();
    text.append("MultiClassClassifier\n\n");
    for (int i = 0; i < m_Classifiers.length; i++) {
      text.append("Classifier ").append(i + 1);
      if (m_Classifiers[i] != null) {
        if ((m_ClassFilters != null) && (m_ClassFilters[i] != null)) {
	  if (m_ClassFilters[i] instanceof RemoveWithValues) {
	    Range range = new Range(((RemoveWithValues)m_ClassFilters[i])
				    .getNominalIndices());
	    range.setUpper(m_ClassAttribute.numValues());
	    int[] pair = range.getSelection();
	    text.append(", " + (pair[0]+1) + " vs " + (pair[1]+1));
	  } else if (m_ClassFilters[i] instanceof MakeIndicator) {
	    text.append(", using indicator values: ");
	    text.append(((MakeIndicator)m_ClassFilters[i]).getValueRange());
	  }
        }
        text.append('\n');
        text.append(m_Classifiers[i].toString() + "\n\n");
      } else {
        text.append(" Skipped (no training examples)\n");
      }
    }

    return text.toString();
  }

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions()  {

    Vector vec = new Vector(3);
    
    vec.addElement(new Option(
       "\tSets the method to use. Valid values are 0 (1-against-all),\n"
       +"\t1 (random codes), 2 (exhaustive code), and 3 (1-against-1). (default 0)\n",
       "M", 1, "-M <num>"));
    vec.addElement(new Option(
       "\tSets the multiplier when using random codes. (default 2.0)",
       "R", 1, "-R <num>"));

    Enumeration enu = super.listOptions();
    while (enu.hasMoreElements()) {
      vec.addElement(enu.nextElement());
    }
    return vec.elements();
  }

  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -M &lt;num&gt;
   *  Sets the method to use. Valid values are 0 (1-against-all),
   *  1 (random codes), 2 (exhaustive code), and 3 (1-against-1). (default 0)
   * </pre>
   * 
   * <pre> -R &lt;num&gt;
   *  Sets the multiplier when using random codes. (default 2.0)</pre>
   * 
   * <pre> -S &lt;num&gt;
   *  Random number seed.
   *  (default 1)</pre>
   * 
   * <pre> -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   * <pre> -W
   *  Full name of base classifier.
   *  (default: weka.classifiers.functions.Logistic)</pre>
   * 
   * <pre> 
   * Options specific to classifier weka.classifiers.functions.Logistic:
   * </pre>
   * 
   * <pre> -D
   *  Turn on debugging output.</pre>
   * 
   * <pre> -R &lt;ridge&gt;
   *  Set the ridge in the log-likelihood.</pre>
   * 
   * <pre> -M &lt;number&gt;
   *  Set the maximum number of iterations (default -1, until convergence).</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
  
    String errorString = Utils.getOption('M', options);
    if (errorString.length() != 0) {
      setMethod(new SelectedTag(Integer.parseInt(errorString), 
                                             TAGS_METHOD));
    } else {
      setMethod(new SelectedTag(METHOD_1_AGAINST_ALL, TAGS_METHOD));
    }

    String rfactorString = Utils.getOption('R', options);
    if (rfactorString.length() != 0) {
      setRandomWidthFactor((new Double(rfactorString)).doubleValue());
    } else {
      setRandomWidthFactor(2.0);
    }

    super.setOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] superOptions = super.getOptions();
    String [] options = new String [superOptions.length + 4];

    int current = 0;


    options[current++] = "-M";
    options[current++] = "" + m_Method;
    
    options[current++] = "-R";
    options[current++] = "" + m_RandomWidthFactor;

    System.arraycopy(superOptions, 0, options, current, 
		     superOptions.length);

    current += superOptions.length;
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "A metaclassifier for handling multi-class datasets with 2-class "
      + "classifiers. This classifier is also capable of "
      + "applying error correcting output codes for increased accuracy.";
  }

  /**
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String randomWidthFactorTipText() {

    return "Sets the width multiplier when using random codes. The number "
      + "of codes generated will be thus number multiplied by the number of "
      + "classes.";
  }

  /**
   * Gets the multiplier when generating random codes. Will generate
   * numClasses * m_RandomWidthFactor codes.
   *
   * @return the width multiplier
   */
  public double getRandomWidthFactor() {

    return m_RandomWidthFactor;
  }
  
  /**
   * Sets the multiplier when generating random codes. Will generate
   * numClasses * m_RandomWidthFactor codes.
   *
   * @param newRandomWidthFactor the new width multiplier
   */
  public void setRandomWidthFactor(double newRandomWidthFactor) {

    m_RandomWidthFactor = newRandomWidthFactor;
  }
  
  /**
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String methodTipText() {
    return "Sets the method to use for transforming the multi-class problem into "
      + "several 2-class ones."; 
  }

  /**
   * Gets the method used. Will be one of METHOD_1_AGAINST_ALL,
   * METHOD_ERROR_RANDOM, METHOD_ERROR_EXHAUSTIVE, or METHOD_1_AGAINST_1.
   *
   * @return the current method.
   */
  public SelectedTag getMethod() {
      
    return new SelectedTag(m_Method, TAGS_METHOD);
  }

  /**
   * Sets the method used. Will be one of METHOD_1_AGAINST_ALL,
   * METHOD_ERROR_RANDOM, METHOD_ERROR_EXHAUSTIVE, or METHOD_1_AGAINST_1.
   *
   * @param newMethod the new method.
   */
  public void setMethod(SelectedTag newMethod) {
    
    if (newMethod.getTags() == TAGS_METHOD) {
      m_Method = newMethod.getSelectedTag().getID();
    }
  }

  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {

    Classifier scheme;

    try {
      scheme = new MultiClassClassifier();
      System.out.println(Evaluation.evaluateModel(scheme, argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
    }
  }
}
