/*
 *    MultiClassClassifier.java
 *    Copyright (C) 1999 Eibe Frank,Len Trigg
 *
 */

package weka.classifiers;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;
import weka.core.Attribute;
import weka.core.AttributeStats;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.MakeIndicatorFilter;

/**
 * Class for handling multi-class datasets with 2-class distribution
 * classifiers.<p>
 *
 * Valid options are:<p>
 *
 * -E num <br>
 * Sets the error-correction mode. Valid values are 0 (no correction),
 * 1 (random codes), and 2 (exhaustive code). (default 0) <p>
 *
 * -R num <br>
 * Sets the multiplier when using random codes. (default 2.0)<p>
 *
 * -W classname <br>
 * Specify the full class name of a classifier as the basis for 
 * the multi-class classifier (required).<p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Len Trigg (len@webmind.com)
 * @version $Revision: 1.20 $
 */
public class MultiClassClassifier extends DistributionClassifier 
  implements OptionHandler {

  /** The classifiers. (One for each class.) */
  private Classifier [] m_Classifiers;

  /** The filters used to transform the class. */
  private MakeIndicatorFilter[] m_ClassFilters;

  /** The class name of the base classifier. */
  private DistributionClassifier m_Classifier = new weka.classifiers.ZeroR();

  /** ZeroR classifier for when all base classifier return zero probability. */
  private ZeroR m_ZeroR;

  /** Internal copy of the class attribute for output purposes */
  private Attribute m_ClassAttribute;
  
  /** 
   * The multiplier when generating random codes. Will generate
   * numClasses * m_RandomWidthFactor codes
   */
  private double m_RandomWidthFactor = 2.0;
  
  /** The error-correcting output code method to use */
  private int m_ErrorMode = ERROR_NONE;

  /** The error correction modes */
  public final static int ERROR_NONE       = 0;
  public final static int ERROR_RANDOM     = 1;
  public final static int ERROR_EXHAUSTIVE = 2;
  public static final Tag [] TAGS_ERROR = {
    new Tag(ERROR_NONE       , "No correction"),
    new Tag(ERROR_RANDOM     , "Random correction code"),
    new Tag(ERROR_EXHAUSTIVE , "Exhaustive correction code")
  };

  /** Interface for the code constructors */
  private abstract class Code implements Serializable {

    /**
     * Subclasses must allocate and fill these. 
     * First dimension is number of codes.
     * Second dimension is number of classes.
     */
    protected boolean [][]m_Codebits;

    /** Returns the number of codes. */
    public int size() {
      return m_Codebits.length;
    }

    /** 
     * Returns the indices of the values set to true for this code, 
     * using 1-based indexing (for input to Range).
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

    /** Returns a human-readable representation of the codes. */
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

  /** Constructs a code with no error correction */
  private class StandardCode extends Code {
    public StandardCode(int numClasses) {
      m_Codebits = new boolean[numClasses][numClasses];
      for (int i = 0; i < numClasses; i++) {
        m_Codebits[i][i] = true;
      }
      System.err.println("Code:\n" + this);
    }
  }

  /** Constructs a random code assignment */
  private class RandomCode extends Code {
    Random r = new Random();
    public RandomCode(int numClasses, int numCodes) {
      numCodes = Math.max(numClasses, numCodes);
      m_Codebits = new boolean[numCodes][numClasses];
      int i = 0;
      do {
        randomize();
        //System.err.println(this);
      } while (!good() && (i++ < 100));
      System.err.println("Code:\n" + this);
    }

    private boolean good() {
      boolean [] ninClass = new boolean[m_Codebits[0].length];
      boolean [] ainClass = new boolean[m_Codebits[0].length];
      java.util.Arrays.fill(ainClass, true);
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

    private void randomize() {
      for (int i = 0; i < m_Codebits.length; i++) {
        for (int j = 0; j < m_Codebits[i].length; j++) {
          m_Codebits[i][j] = r.nextBoolean();
        }
      }
    }
  }

  /**
   * TODO: Constructs codes as per:
   * Bose, R.C., Ray Chaudhuri (1960), On a class of error-correcting
   * binary group codes, Information and Control, 3, 68-79.
   * Hocquenghem, A. (1959) Codes corecteurs d'erreurs, Chiffres, 2, 147-156. 
   */
  //private class BCHCode extends Code {...}

  /** Constructs an exhaustive code assignment */
  private class ExhaustiveCode extends Code {
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
      System.err.println("Code:\n" + this);
    }
  }



  /**
   * Builds the classifiers.
   *
   * @param insts the training data.
   * @exception Exception if a classifier can't be built
   */
  public void buildClassifier(Instances insts) throws Exception {

    Instances newInsts;

    if (m_Classifier == null) {
      throw new Exception("No base classifier has been set!");
    }
    m_ZeroR = new ZeroR();
    m_ZeroR.buildClassifier(insts);

    int numClassifiers = insts.numClasses();
    if (numClassifiers <= 2) {

      m_Classifiers = Classifier.makeCopies(m_Classifier, 1);
      m_Classifiers[0].buildClassifier(insts);

    } else {
      Code code = null;
      switch (m_ErrorMode) {
      case ERROR_EXHAUSTIVE:
        code = new ExhaustiveCode(numClassifiers);
        break;
      case ERROR_RANDOM:
        code = new RandomCode(numClassifiers, 
                              (int)(numClassifiers * m_RandomWidthFactor));
        break;
      case ERROR_NONE:
        code = new StandardCode(numClassifiers);
        break;
      default:
        throw new Exception("Unrecognized correction code type");
      }
      numClassifiers = code.size();
      m_Classifiers = Classifier.makeCopies(m_Classifier, numClassifiers);
      m_ClassFilters = new MakeIndicatorFilter[numClassifiers];
      AttributeStats classStats = insts.attributeStats(insts.classIndex());
      for (int i = 0; i < m_Classifiers.length; i++) {
        if ((m_ErrorMode == ERROR_NONE) && 
            (classStats.nominalCounts[i] == 0)) {
          m_Classifiers[i] = null;
        } else {
          m_ClassFilters[i] = new MakeIndicatorFilter();
          m_ClassFilters[i].setAttributeIndex(insts.classIndex());
          m_ClassFilters[i].setValueIndices(code.getIndices(i));
          m_ClassFilters[i].setNumeric(false);
          m_ClassFilters[i].inputFormat(insts);
          newInsts = Filter.useFilter(insts, m_ClassFilters[i]);
          m_Classifiers[i].buildClassifier(newInsts);
        }
      }
    }
    m_ClassAttribute = insts.classAttribute();
  }

  /**
   * Returns the distribution for an instance.
   *
   * @exception Exception if the distribution can't be computed successfully
   */
  public double[] distributionForInstance(Instance inst) throws Exception {
    
    if (m_Classifiers.length == 1) {
      return ((DistributionClassifier)m_Classifiers[0])
        .distributionForInstance(inst);
    }
    
    double[] probs = new double[inst.numClasses()];
    for(int i = 0; i < m_ClassFilters.length; i++) {
      if (m_Classifiers[i] != null) {
        m_ClassFilters[i].input(inst);
        m_ClassFilters[i].batchFinished();
        double [] current = ((DistributionClassifier)m_Classifiers[i])
          .distributionForInstance(m_ClassFilters[i].output());
        for (int j = 0; j < m_ClassAttribute.numValues(); j++) {
          if (m_ClassFilters[i].getValueRange().isInRange(j)) {
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
          text.append(", using indicator values: ");
          text.append(m_ClassFilters[i].getValueRange());
        }
        text.append('\n');
        text.append(m_Classifiers[i].toString() + "\n");
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
    Object c;
    
    vec.addElement(new Option(
       "\tSets the error-correction mode. Valid values are 0 (no correction),\n"
       +"\t1 (random codes), and 2 (exhaustive code). (default 0)\n",
       "E", 1, "-E <num>"));
    vec.addElement(new Option(
       "\tSets the multiplier when using random codes. (default 2.0)",
       "R", 1, "-R <num>"));
    vec.addElement(new Option(
       "\tSets the base classifier.",
       "W", 1, "-W <base classifier>"));
    
    if (m_Classifier != null) {
      try {
	vec.addElement(new Option("",
				  "", 0, "\nOptions specific to classifier "
				  + m_Classifier.getClass().getName() + ":"));
	Enumeration enum = ((OptionHandler)m_Classifier).listOptions();
	while (enum.hasMoreElements()) {
	  vec.addElement(enum.nextElement());
	}
      } catch (Exception e) {
      }
    }
    return vec.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -E num <br>
   * Sets the error-correction mode. Valid values are 0 (no correction),
   * 1 (random codes), and 2 (exhaustive code). (default 0) <p>
   *
   * -R num <br>
   * Sets the multiplier when using random codes. (default 2.0)<p>
   *
   * -W classname <br>
   * Specify the full class name of a learner as the basis for 
   * the multiclassclassifier (required).<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
  
    String errorString = Utils.getOption('E', options);
    if (errorString.length() != 0) {
      setErrorCorrectionMode(new SelectedTag(Integer.parseInt(errorString), 
                                             TAGS_ERROR));
    } else {
      setErrorCorrectionMode(new SelectedTag(ERROR_NONE,
                                             TAGS_ERROR));
    }

    String rfactorString = Utils.getOption('R', options);
    if (rfactorString.length() != 0) {
      setRandomWidthFactor(Double.parseDouble(rfactorString));
    } else {
      setRandomWidthFactor(2.0);
    }

    String classifierName = Utils.getOption('W', options);
    if (classifierName.length() == 0) {
      throw new Exception("A classifier must be specified with"
			  + " the -W option.");
    }
    setDistributionClassifier((DistributionClassifier)
                              Classifier.forName(classifierName,
                                                 Utils.partitionOptions(options)));
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {
    
    String [] classifierOptions = new String [0];
    if ((m_Classifier != null) &&
	(m_Classifier instanceof OptionHandler)) {
      classifierOptions = ((OptionHandler)m_Classifier).getOptions();
    }
    String [] options = new String [classifierOptions.length + 7];
    int current = 0;

    options[current++] = "-E";
    options[current++] = "" + m_ErrorMode;
    
    options[current++] = "-R";
    options[current++] = "" + m_RandomWidthFactor;
    
    if (getDistributionClassifier() != null) {
      options[current++] = "-W";
      options[current++] = getDistributionClassifier().getClass().getName();
    }
    options[current++] = "--";

    System.arraycopy(classifierOptions, 0, options, current, 
		     classifierOptions.length);
    current += classifierOptions.length;
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
      + "distribution classifiers. This classifier is also capable of "
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
  public String errorCorrectionModeTipText() {
    return "Sets whether error correction will be used. The default method "
      + "is no error correction: one classifier will be built per class value. "
      + "Increased accuracy can be obtained by using error correcting output "
      + "codes. \"Random\" generates random output codes (the number of "
      + "which is determined by the number of classes and the width "
      + "multiplier). \"Exhaustive\" generates one classifier for each "
      + "(non-redundant) combination of class values - beware that this "
      + "increases exponentially in the number of class values. We have yet "
      + "to implement BCH codes (feel free to do so)."; 
  }

  /**
   * Gets the error correction mode used. Will be one of
   * ERROR_NONE, ERROR_RANDOM, or ERROR_EXHAUSTIVE.
   *
   * @return the current error correction mode.
   */
  public SelectedTag getErrorCorrectionMode() {
      
    return new SelectedTag(m_ErrorMode, TAGS_ERROR);
  }

  /**
   * Sets the error correction mode used. Will be one of
   * ERROR_NONE, ERROR_RANDOM, or ERROR_EXHAUSTIVE.
   *
   * @param newMethod the new error correction mode.
   */
  public void setErrorCorrectionMode(SelectedTag newMethod) {
    
    if (newMethod.getTags() == TAGS_ERROR) {
      m_ErrorMode = newMethod.getSelectedTag().getID();
    }
  }

  /**
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String distributionClassifierTipText() {
    return "Sets the DistributionClassifier used as the basis for "
      + "the multi-class classifier.";
  }

  /**
   * Set the base classifier. 
   *
   * @param newClassifier the Classifier to use.
   */
  public void setDistributionClassifier(DistributionClassifier newClassifier) {

    m_Classifier = newClassifier;
  }

  /**
   * Get the classifier used as the classifier
   *
   * @return the classifier used as the classifier
   */
  public DistributionClassifier getDistributionClassifier() {

    return m_Classifier;
  }


  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {

    DistributionClassifier scheme;

    try {
      scheme = new MultiClassClassifier();
      System.out.println(Evaluation.evaluateModel(scheme, argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}
