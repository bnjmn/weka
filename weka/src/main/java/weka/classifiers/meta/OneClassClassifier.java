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

/**
 *   OneClassClassifier.java
 *   Copyright (C) 2008 K.Hempstalk, University of Waikato, Hamilton, New Zealand.
 */

package weka.classifiers.meta;

import weka.classifiers.RandomizableSingleClassifierEnhancer;
import weka.classifiers.meta.generators.GaussianGenerator;
import weka.classifiers.meta.generators.Generator;
import weka.classifiers.meta.generators.InstanceHandler;
import weka.classifiers.meta.generators.Mean;
import weka.classifiers.meta.generators.NominalGenerator;
import weka.classifiers.meta.generators.NominalAttributeGenerator;
import weka.classifiers.meta.generators.NumericAttributeGenerator;
import weka.classifiers.meta.generators.Ranged;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.AddValues;
import weka.filters.unsupervised.attribute.MergeManyValues;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * Performs one-class classification on a dataset.<br/>
 * <br/>
 * Classifier reduces the class being classified to just a single class, and learns the datawithout using any information from other classes.  The testing stage will classify as 'target'or 'outlier' - so in order to calculate the outlier pass rate the dataset must contain informationfrom more than one class.<br/>
 * <br/>
 * Also, the output varies depending on whether the label 'outlier' exists in the instances usedto build the classifier.  If so, then 'outlier' will be predicted, if not, then the label willbe considered missing when the prediction does not favour the target class.  The 'outlier' classwill not be used to build the model if there are instances of this class in the dataset.  It cansimply be used as a flag, you do not need to relabel any classes.<br/>
 * <br/>
 * For more information, see:<br/>
 * <br/>
 * Kathryn Hempstalk, Eibe Frank, Ian H. Witten: One-Class Classification by Combining Density and Class Probability Estimation. In: Proceedings of the 12th European Conference on Principles and Practice of Knowledge Discovery in Databases and 19th European Conference on Machine Learning, ECMLPKDD2008, Berlin, 505--519, 2008.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;conference{Hempstalk2008,
 *    address = {Berlin},
 *    author = {Kathryn Hempstalk and Eibe Frank and Ian H. Witten},
 *    booktitle = {Proceedings of the 12th European Conference on Principles and Practice of Knowledge Discovery in Databases and 19th European Conference on Machine Learning, ECMLPKDD2008},
 *    month = {September},
 *    pages = {505--519},
 *    publisher = {Springer},
 *    series = {Lecture Notes in Computer Science},
 *    title = {One-Class Classification by Combining Density and Class Probability Estimation},
 *    volume = {Vol. 5211},
 *    year = {2008},
 *    location = {Antwerp, Belgium}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -trr &lt;rate&gt;
 *  Sets the target rejection rate
 *  (default: 0.1)</pre>
 * 
 * <pre> -tcl &lt;label&gt;
 *  Sets the target class label
 *  (default: 'target')</pre>
 * 
 * <pre> -cvr &lt;rep&gt;
 *  Sets the number of times to repeat cross validation
 *  to find the threshold
 *  (default: 10)</pre>
 * 
 * <pre> -P &lt;prop&gt;
 *  Sets the proportion of generated data
 *  (default: 0.5)</pre>
 * 
 * <pre> -cvf &lt;perc&gt;
 *  Sets the percentage of heldout data for each cross validation
 *  fold
 *  (default: 10)</pre>
 * 
 * <pre> -num &lt;classname + options&gt;
 *  Sets the numeric generator
 *  (default: weka.classifiers.meta.generators.GaussianGenerator)</pre>
 * 
 * <pre> -nom &lt;classname + options&gt;
 *  Sets the nominal generator
 *  (default: weka.classifiers.meta.generators.NominalGenerator)</pre>
 * 
 * <pre> -L
 *  Sets whether to correct the number of classes to two,
 *  if omitted no correction will be made.</pre>
 * 
 * <pre> -E
 *  Sets whether to exclusively use the density estimate.</pre>
 * 
 * <pre> -I
 *  Sets whether to use instance weights.</pre>
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
 *  (default: weka.classifiers.meta.Bagging)</pre>
 * 
 * <pre> 
 * Options specific to classifier weka.classifiers.meta.Bagging:
 * </pre>
 * 
 * <pre> -P
 *  Size of each bag, as a percentage of the
 *  training set size. (default 100)</pre>
 * 
 * <pre> -O
 *  Calculate the out of bag error.</pre>
 * 
 * <pre> -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)</pre>
 * 
 * <pre> -I &lt;num&gt;
 *  Number of iterations.
 *  (default 10)</pre>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -W
 *  Full name of base classifier.
 *  (default: weka.classifiers.trees.REPTree)</pre>
 * 
 * <pre> 
 * Options specific to classifier weka.classifiers.trees.REPTree:
 * </pre>
 * 
 * <pre> -M &lt;minimum number of instances&gt;
 *  Set minimum number of instances per leaf (default 2).</pre>
 * 
 * <pre> -V &lt;minimum variance for split&gt;
 *  Set minimum numeric class variance proportion
 *  of train variance for split (default 1e-3).</pre>
 * 
 * <pre> -N &lt;number of folds&gt;
 *  Number of folds for reduced error pruning (default 3).</pre>
 * 
 * <pre> -S &lt;seed&gt;
 *  Seed for random data shuffling (default 1).</pre>
 * 
 * <pre> -P
 *  No pruning.</pre>
 * 
 * <pre> -L
 *  Maximum tree depth (default -1, no maximum)</pre>
 * 
 <!-- options-end -->
 *
 * Options after -- are passed to the designated classifier.
 *
 * @author Kathryn Hempstalk (kah18 at cs.waikato.ac.nz)
 * @author Eibe Frank (eibe at cs.waikato.ac.nz)
 * @version $Revision$
 */
public class OneClassClassifier
  extends RandomizableSingleClassifierEnhancer
  implements TechnicalInformationHandler {

  /** for serialization. */
  private static final long serialVersionUID = 6199125385010158931L;

  /**
   * The rejection rate of valid target objects (used to set the threshold).
   */
  protected double m_TargetRejectionRate = 0.1;

  /**
   * The probability threshold (only classes above this will be considered target).
   */
  protected double m_Threshold = 0.5;

  /**
   * The generators for the numeric attributes.
   */
  protected ArrayList m_Generators;

  /**
   * The value of the class attribute to consider the target class.
   */
  protected String m_TargetClassLabel = "target";

  /**
   * The number of times to repeat cross validation during learning.
   */
  protected int m_NumRepeats = 10;  

  /**
   * The percentage of heldout data.
   */
  protected double m_PercentHeldout = 10;

  /**
   * The proportion of the data that will be generated.
   */
  protected double m_ProportionGenerated = 0.5;

  /**
   * The default data generator for numeric attributes.
   */
    protected NumericAttributeGenerator m_DefaultNumericGenerator = (NumericAttributeGenerator) new GaussianGenerator();

  /**
   * The default data generator for nominal attributes.
   */
    protected NominalAttributeGenerator m_DefaultNominalGenerator = (NominalAttributeGenerator) new NominalGenerator();

  /**
   * Adds the outlier class if it doesn't already exist.
   */
  protected AddValues m_AddOutlierFilter;

  /**
   * Whether to include laplace correction so if there are multiple
   * values for a class, it is reduced to just two so that any laplace
   * correction in another classifier corrects with one possible other class
   * rather than several.  
   */
  protected boolean m_UseLaplaceCorrection = false;

  /**
   * The filter that merges the instances down to two values.
   */
  protected MergeManyValues m_MergeFilter;

  /**
   * The label for the outlier class.
   */
  public static final String OUTLIER_LABEL = "outlier";


  /**
   * Whether to use only the density estimate, or to include the
   * base classifier in the probability estimates.
   */
  protected boolean m_UseDensityOnly = false;

  /**
   * Whether to weight instances based on their prevalence in the
   * test set used for calculating P(X|T).
   */
  protected boolean m_UseInstanceWeights = false;
  
  /** The random number generator used internally. */
  protected Random m_Random;

  
  /**
   * Default constructor.
   */
  public OneClassClassifier() {
    super();
    
    m_Classifier = new weka.classifiers.meta.Bagging();
  }

  /**
   * Returns a string describing this classes ability.
   *
   * @return A description of the method.
   */
  public String globalInfo() {
    return 
       "Performs one-class classification on a dataset.\n\n"
     + "Classifier reduces the class being classified to just a single class, and learns the data"
     + "without using any information from other classes.  The testing stage will classify as 'target'"
     + "or 'outlier' - so in order to calculate the outlier pass rate the dataset must contain information"
     + "from more than one class.\n"
     + "\n"
     + "Also, the output varies depending on whether the label 'outlier' exists in the instances used"
     + "to build the classifier.  If so, then 'outlier' will be predicted, if not, then the label will"
     + "be considered missing when the prediction does not favour the target class.  The 'outlier' class"
     + "will not be used to build the model if there are instances of this class in the dataset.  It can"
     + "simply be used as a flag, you do not need to relabel any classes.\n"
     + "\n"
     + "For more information, see:\n"
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

    result = new TechnicalInformation(Type.CONFERENCE);
    result.setValue(Field.AUTHOR, "Kathryn Hempstalk and Eibe Frank and Ian H. Witten");
    result.setValue(Field.YEAR, "2008");
    result.setValue(Field.TITLE, "One-Class Classification by Combining Density and Class Probability Estimation");
    result.setValue(Field.BOOKTITLE, "Proceedings of the 12th European Conference on Principles and Practice of Knowledge Discovery in Databases and 19th European Conference on Machine Learning, ECMLPKDD2008");
    result.setValue(Field.VOLUME, "Vol. 5211");
    result.setValue(Field.PAGES, "505--519");
    result.setValue(Field.PUBLISHER, "Springer");
    result.setValue(Field.ADDRESS, "Berlin");
    result.setValue(Field.SERIES, "Lecture Notes in Computer Science");
    result.setValue(Field.LOCATION, "Antwerp, Belgium");
    result.setValue(Field.MONTH, "September");

    return result;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return An enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector result = new Vector();   

    result.addElement(new Option(
	"\tSets the target rejection rate\n"
	+ "\t(default: 0.1)",
	"trr", 1, "-trr <rate>"));

    result.addElement(new Option(
	"\tSets the target class label\n"
	+ "\t(default: 'target')",
	"tcl", 1, "-tcl <label>"));

    result.addElement(new Option(
	"\tSets the number of times to repeat cross validation\n" 
	+ "\tto find the threshold\n"
	+ "\t(default: 10)",
	"cvr", 1, "-cvr <rep>"));

    result.addElement(new Option(
	"\tSets the proportion of generated data\n"
	+ "\t(default: 0.5)",
	"P", 1, "-P <prop>"));

    result.addElement(new Option(
	"\tSets the percentage of heldout data for each cross validation\n"
	+ "\tfold\n"
	+ "\t(default: 10)",
	"cvf", 1, "-cvf <perc>"));

    result.addElement(new Option(
	"\tSets the numeric generator\n"
	+ "\t(default: " + GaussianGenerator.class.getName() + ")",
	"num", 1, "-num <classname + options>"));

    result.addElement(new Option(
	"\tSets the nominal generator\n"
	+ "\t(default: " + NominalGenerator.class.getName() + ")",
	"nom", 1, "-nom <classname + options>"));

    result.addElement(new Option(
	"\tSets whether to correct the number of classes to two,\n"
	+ "\tif omitted no correction will be made.",
	"L", 1, "-L"));

    result.addElement(new Option(
	"\tSets whether to exclusively use the density estimate.",
	"E", 0, "-E"));

    result.addElement(new Option(
	"\tSets whether to use instance weights.",
	"I", 0, "-I"));
    
    Enumeration enu = super.listOptions();
    while (enu.hasMoreElements())
      result.addElement(enu.nextElement());

    return result.elements();
  }

  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -trr &lt;rate&gt;
   *  Sets the target rejection rate
   *  (default: 0.1)</pre>
   * 
   * <pre> -tcl &lt;label&gt;
   *  Sets the target class label
   *  (default: 'target')</pre>
   * 
   * <pre> -cvr &lt;rep&gt;
   *  Sets the number of times to repeat cross validation
   *  to find the threshold
   *  (default: 10)</pre>
   * 
   * <pre> -P &lt;prop&gt;
   *  Sets the proportion of generated data
   *  (default: 0.5)</pre>
   * 
   * <pre> -cvf &lt;perc&gt;
   *  Sets the percentage of heldout data for each cross validation
   *  fold
   *  (default: 10)</pre>
   * 
   * <pre> -num &lt;classname + options&gt;
   *  Sets the numeric generator
   *  (default: weka.classifiers.meta.generators.GaussianGenerator)</pre>
   * 
   * <pre> -nom &lt;classname + options&gt;
   *  Sets the nominal generator
   *  (default: weka.classifiers.meta.generators.NominalGenerator)</pre>
   * 
   * <pre> -L
   *  Sets whether to correct the number of classes to two,
   *  if omitted no correction will be made.</pre>
   * 
   * <pre> -E
   *  Sets whether to exclusively use the density estimate.</pre>
   * 
   * <pre> -I
   *  Sets whether to use instance weights.</pre>
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
   *  (default: weka.classifiers.meta.Bagging)</pre>
   * 
   * <pre> 
   * Options specific to classifier weka.classifiers.meta.Bagging:
   * </pre>
   * 
   * <pre> -P
   *  Size of each bag, as a percentage of the
   *  training set size. (default 100)</pre>
   * 
   * <pre> -O
   *  Calculate the out of bag error.</pre>
   * 
   * <pre> -S &lt;num&gt;
   *  Random number seed.
   *  (default 1)</pre>
   * 
   * <pre> -I &lt;num&gt;
   *  Number of iterations.
   *  (default 10)</pre>
   * 
   * <pre> -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   * <pre> -W
   *  Full name of base classifier.
   *  (default: weka.classifiers.trees.REPTree)</pre>
   * 
   * <pre> 
   * Options specific to classifier weka.classifiers.trees.REPTree:
   * </pre>
   * 
   * <pre> -M &lt;minimum number of instances&gt;
   *  Set minimum number of instances per leaf (default 2).</pre>
   * 
   * <pre> -V &lt;minimum variance for split&gt;
   *  Set minimum numeric class variance proportion
   *  of train variance for split (default 1e-3).</pre>
   * 
   * <pre> -N &lt;number of folds&gt;
   *  Number of folds for reduced error pruning (default 3).</pre>
   * 
   * <pre> -S &lt;seed&gt;
   *  Seed for random data shuffling (default 1).</pre>
   * 
   * <pre> -P
   *  No pruning.</pre>
   * 
   * <pre> -L
   *  Maximum tree depth (default -1, no maximum)</pre>
   * 
   <!-- options-end -->
   *
   * @param options The list of options as an array of strings.
   * @throws Exception If an option is not supported.
   */
  public void setOptions(String[] options) throws Exception {
    String	tmpStr;
    String[]	tmpOptions;

    // numeric generator
    tmpStr = Utils.getOption("num", options);
    if (tmpStr.length() != 0) { 
      tmpOptions    = Utils.splitOptions(tmpStr);
      tmpStr        = tmpOptions[0];
      tmpOptions[0] = "";
      setNumericGenerator((NumericAttributeGenerator) Utils.forName(Generator.class, tmpStr, tmpOptions));
    }
    else {
      setNumericGenerator((NumericAttributeGenerator) Utils.forName(Generator.class, defaultNumericGeneratorString(), null));
    }

    // nominal generator
    tmpStr = Utils.getOption("nom", options);
    if (tmpStr.length() != 0) { 
      tmpOptions    = Utils.splitOptions(tmpStr);
      tmpStr        = tmpOptions[0];
      tmpOptions[0] = "";
      setNominalGenerator((NominalAttributeGenerator) Utils.forName(Generator.class, tmpStr, tmpOptions));
    }
    else {
      setNominalGenerator((NominalAttributeGenerator) Utils.forName(Generator.class, defaultNominalGeneratorString(), null));
    }

    //target rejection rate
    tmpStr = Utils.getOption("trr", options);
    if (tmpStr.length() != 0)
      setTargetRejectionRate(Double.parseDouble(tmpStr));
    else
      setTargetRejectionRate(0.1);

    //target class label
    tmpStr = Utils.getOption("tcl", options);
    if (tmpStr.length() != 0)
      setTargetClassLabel(tmpStr);
    else
      setTargetClassLabel("target");

    //cross validation repeats
    tmpStr = Utils.getOption("cvr", options);
    if (tmpStr.length() != 0)
      setNumRepeats(Integer.parseInt(tmpStr));
    else
      setNumRepeats(10);

    //cross validation fold size
    tmpStr = Utils.getOption("cvf", options);
    if (tmpStr.length() != 0)
      setPercentageHeldout(Double.parseDouble(tmpStr));
    else
      setPercentageHeldout(10.0);

    //proportion generated
    tmpStr = Utils.getOption("P", options);
    if (tmpStr.length() != 0)
      setProportionGenerated(Double.parseDouble(tmpStr));
    else
      setProportionGenerated(0.5);

    //use laplace
    setUseLaplaceCorrection(Utils.getFlag('L',options));

    //set whether to exclusively use the density estimate
    setDensityOnly(Utils.getFlag('E', options));

    //use instance weights
    setUseInstanceWeights(Utils.getFlag('I', options));
    
    // set the parent's options first
    super.setOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return An array of strings suitable for passing to setOptions.
   */
  public String[] getOptions() {
    Vector<String>	result;
    String[]		options;
    int			i;

    result = new Vector<String>();
    
    result.add("-num");
    result.add(
	  m_DefaultNumericGenerator.getClass().getName() 
	+ " " 
	  + Utils.joinOptions(((Generator)m_DefaultNumericGenerator).getOptions()));

    result.add("-nom");
    result.add(
	  m_DefaultNominalGenerator.getClass().getName() 
	+ " " 
	  + Utils.joinOptions(((Generator)m_DefaultNominalGenerator).getOptions()));
    
    result.add("-trr");
    result.add("" + m_TargetRejectionRate);
    
    result.add("-tcl");
    result.add("" + m_TargetClassLabel);
    
    result.add("-cvr");
    result.add("" + m_NumRepeats);
    
    result.add("-cvf");
    result.add("" + m_PercentHeldout);
    
    result.add("-P");
    result.add("" + m_ProportionGenerated);
    
    if (m_UseLaplaceCorrection)
      result.add("-L");    
  
    if (m_UseDensityOnly)
      result.add("-E");
    
    if (m_UseInstanceWeights)
      result.add("-I");

    options = super.getOptions();
    for (i = 0; i < options.length; i++)
      result.add(options[i]);

    return result.toArray(new String[result.size()]);
  }

  /**
   * Gets whether only the density estimate should be used by the classifier.  If false,
   * the base classifier's estimate will be incorporated using bayes rule for two classes.
   *
   * @return Whether to use only the density estimate.
   */
  public boolean getDensityOnly() {
    return m_UseDensityOnly;
  }

  /**
   * Sets whether the density estimate will be used by itself.
   *
   * @param density Whether to use the density estimate exclusively or not.
   */
  public void setDensityOnly(boolean density) {
    m_UseDensityOnly = density;
  }
  
  /**
   * Returns the tip text for this property.
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String densityOnlyTipText() {
    return "If true, the density estimate will be used by itself.";
  }

  /**
   * Gets the target rejection rate - the proportion of target class samples
   * that will be rejected in order to build a threshold.
   *
   * @return The target rejection rate.
   */
  public double getTargetRejectionRate() {
    return m_TargetRejectionRate;
  }

  /**
   * Sets the target rejection rate.
   *
   * @param rate The new target rejection rate.
   */
  public void setTargetRejectionRate(double rate) {
    m_TargetRejectionRate = rate;
  }
  
  /**
   * Returns the tip text for this property.
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String targetRejectionRateTipText() {
    return 
        "The target rejection rate, ie, the proportion of target class "
      + "samples that will be rejected in order to build a threshold.";
  }

  /**
   * Gets the target class label - the class label to perform one
   * class classification on.
   *
   * @return The target class label.
   */
  public String getTargetClassLabel() {
    return m_TargetClassLabel;
  }

  /**
   * Sets the target class label to a new value.
   *
   * @param label The target class label to classify for.
   */
  public void setTargetClassLabel(String label) {
    m_TargetClassLabel = label;
  }
  
  /**
   * Returns the tip text for this property.
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String targetClassLabelTipText() {
    return "The class label to perform one-class classification on.";
  }

  /**
   * Gets the number of repeats for (internal) cross validation.
   *
   * @return The number of repeats for internal cross validation.
   */
  public int getNumRepeats() {
    return m_NumRepeats;
  }

  /**
   * Sets the number of repeats for (internal) cross validation to a new value.
   *
   * @param repeats The new number of repeats for cross validation.
   */
  public void setNumRepeats(int repeats) {
    m_NumRepeats = repeats;
  }
  
  /**
   * Returns the tip text for this property.
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String numRepeatsTipText() {
    return "The number of repeats for (internal) cross-validation.";
  }

  /**
   * Sets the proportion of generated data to a new value.
   *
   * @param prop The new proportion.
   */
  public void setProportionGenerated(double prop) {
    m_ProportionGenerated = prop;
  }

  /**
   * Gets the proportion of data that will be generated compared to the 
   * target class label.
   *
   * @return The proportion of generated data.
   */
  public double getProportionGenerated() {
    return m_ProportionGenerated;
  }
  
  /**
   * Returns the tip text for this property.
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String proportionGeneratedTipText() {
    return 
        "The proportion of data that will be generated compared to the "
      + "target class label.";
  }

  /**
   * Sets the percentage heldout in each CV fold.
   *
   * @param percent The new percent of heldout data.
   */
  public void setPercentageHeldout(double percent) {
    m_PercentHeldout = percent;
  }

  /**
   * Gets the percentage of data that will be heldout in each
   * iteration of cross validation.
   *
   * @return The percentage of heldout data.
   */
  public double getPercentageHeldout() {
    return m_PercentHeldout;
  }
  
  /**
   * Returns the tip text for this property.
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String percentageHeldoutTipText() {
    return 
        "The percentage of data that will be heldout in each iteration "
      + "of (internal) cross-validation.";
  }

  /**
   * Gets thegenerator that will be used by default to generate 
   * numeric outlier data.
   *
   * @return The numeric data generator.
   */
  public NumericAttributeGenerator getNumericGenerator() {
    return m_DefaultNumericGenerator;
  }

  /**
   * Sets the generator that will be used by default to generate
   * numeric outlier data.
   *
   * @param agen The new numeric data generator to use.
   */
  public void setNumericGenerator(NumericAttributeGenerator agen) {
    m_DefaultNumericGenerator = agen;
  }
  
  /**
   * Returns the tip text for this property.
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String numericGeneratorTipText() {
    return "The numeric data generator to use.";
  }

  /**
   * Gets the generator that will be used by default to generate 
   * nominal outlier data.
   *
   * @return The nominal data generator.
   */
  public NominalAttributeGenerator getNominalGenerator() {
    return m_DefaultNominalGenerator;
  }

  /**
   * Sets the generator that will be used by default to generate
   * nominal outlier data.
   *
   * @param agen The new nominal data generator to use.
   */
  public void setNominalGenerator(NominalAttributeGenerator agen) {
    m_DefaultNominalGenerator = agen;
  }
  
  /**
   * Returns the tip text for this property.
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String nominalGeneratorTipText() {
    return "The nominal data generator to use.";
  }

  /**
   * Gets whether a laplace correction should be used.
   *
   * @return Whether a laplace correction should be used.
   */
  public boolean getUseLaplaceCorrection() {
    return m_UseLaplaceCorrection;
  }

  /**
   * Sets whether a laplace correction should be used.  A laplace
   * correction will reduce the number of class labels to two, the 
   * target and outlier classes, regardless of how many labels 
   * actually exist.  This is useful for classifiers that use
   * the number of class labels to make use a laplace value
   * based on the unseen class.
   *
   * @param newuse Whether to use the laplace correction (default: true).
   */
  public void setUseLaplaceCorrection(boolean newuse) {
    m_UseLaplaceCorrection = newuse;
  }
  
  /**
   * Returns the tip text for this property.
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String useLaplaceCorrectionTipText() {
    return 
        "If true, then Laplace correction will be used (reduces the "
      + "number of class labels to two, target and outlier class, regardless "
      + "of how many class labels actually exist) - useful for classifiers "
      + "that use the number of class labels to make use of a Laplace value "
      + "based on the unseen class.";
  }


  /**
   * Sets whether to perform weighting on instances based on their
   * prevalence in the data.
   *
   * @param newuse Whether or not to use instance weighting.
   */
  public void setUseInstanceWeights(boolean newuse) {
    m_UseInstanceWeights = newuse;
  }

  /**
   * Gets whether instance weighting will be performed.
   *
   * @return Whether instance weighting will be performed.
   */
  public boolean getUseInstanceWeights() {
    return m_UseInstanceWeights;
  }
  
  /**
   * Returns the tip text for this property.
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String useInstanceWeightsTipText() {
    return 
        "If true, the weighting on instances is based on their prevalence "
      + "in the data.";
  }

  /**
   * String describing default internal classifier.
   * 
   * @return The default classifier classname.
   */
  protected String defaultClassifierString() {    
    return "weka.classifiers.meta.Bagging";
  }

  /**
   * String describing default generator / density estimator.
   * 
   * @return The default numeric generator classname.
   */
  protected String defaultNumericGeneratorString() {    
    return "weka.classifiers.meta.generators.GaussianGenerator";
  }

  /**
   * String describing default generator / density estimator.
   * 
   * @return The default nominal generator classname.
   */
  protected String defaultNominalGeneratorString() {    
    return "weka.classifiers.meta.generators.NominalGenerator";
  }

  /**
   * Returns default capabilities of the base classifier.
   *
   * @return      the capabilities of the base classifier
   */
  public Capabilities getCapabilities() {
    Capabilities	result;
    
    result = super.getCapabilities();
    
    // only nominal classes can be processed!
    result.disableAllClasses();
    result.disableAllClassDependencies();
    result.enable(Capability.NOMINAL_CLASS);
    
    return result;
  }

  /**
   * Build the one-class classifier, any non-target data values
   * are ignored.  The target class label must exist in the arff
   * file or else an exception will be thrown.
   *
   * @param data The training data.
   * @throws Exception If the classifier could not be built successfully.
   */
  public void buildClassifier(Instances data) throws Exception {
    if (m_Classifier == null) {
      throw new Exception("No base classifier has been set!");
    }
    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    // remove instances with missing class
    Instances newData = new Instances(data);

    m_Random = new Random(m_Seed);
    
    //delete the data that's not of the class we are trying to classify.
    Attribute classAttribute = newData.classAttribute();
    double targetClassValue = classAttribute.indexOfValue(m_TargetClassLabel);	     
    if (targetClassValue == -1) {
      throw new Exception("Target class value doesn't exist!");
    }

    int index = 0;
    while(index < newData.numInstances()) {
      Instance aninst = newData.instance(index);
      if (aninst.classValue() != targetClassValue) {
	newData.delete(index);
      } else
	index++;
    }

    if (newData.numInstances() == 0) {
      throw new Exception("No instances found belonging to the target class!");
    }

    //now we need to add the "outlier" attribute if it doesn't already exist.
    m_AddOutlierFilter = new AddValues();
    m_AddOutlierFilter.setAttributeIndex("" + (newData.classIndex() + 1));
    m_AddOutlierFilter.setLabels(OneClassClassifier.OUTLIER_LABEL);
    m_AddOutlierFilter.setInputFormat(newData);
    newData = Filter.useFilter(newData, m_AddOutlierFilter);

    if (m_UseLaplaceCorrection) {
      newData = this.mergeToTwo(newData);   
    }	

    //make sure the generators are created.
    m_Generators = new ArrayList();

    //one for each column
    //need to work out the range or mean/stddev for each attribute
    int numAttributes = newData.numAttributes();

    //work out the ranges
    double lowranges[] = new double[numAttributes - 1];
    double highranges[] = new double[numAttributes - 1];
    double means[] = new double[numAttributes - 1];
    double stddevs[] = new double[numAttributes - 1];
    double instanceCount[] = new double[numAttributes - 1];
    int attrIndexes[] = new int[numAttributes - 1];

    //initialise
    for (int i = 0; i < numAttributes - 1; i++) {
      lowranges[i] = Double.MAX_VALUE;
      highranges[i] = -1 * Double.MAX_VALUE;
      means[i] = 0;
      stddevs[i] = 0;
      attrIndexes[i] = 0;
      instanceCount[i] = 0;
    }

    //calculate low/high ranges and means.
    //missing attributes are ignored
    for (int i = 0; i < newData.numInstances(); i++) {
      Instance anInst = newData.instance(i);
      int attCount = 0;
      for (int j = 0; j < numAttributes; j++) {
	if (j != newData.classIndex()) {
	  double attVal = anInst.value(j);
	  if (!anInst.isMissing(j)) {
	    if (attVal > highranges[attCount])
	      highranges[attCount] = attVal;
	    if (attVal < lowranges[attCount])
	      lowranges[attCount] = attVal;

	    means[attCount] += attVal;
	    instanceCount[attCount] += 1;			
	  }
	  attrIndexes[attCount] = j;
	  attCount++;

	}
      }
    }

    //calculate means...
    for (int i = 0; i < numAttributes - 1; i++) {
      if (instanceCount[i] > 0) {
	means[i] = means[i] / instanceCount[i];
      }
    }

    //and now standard deviations
    for (int i = 0; i < newData.numInstances(); i++) {
      Instance anInst = newData.instance(i);
      int attCount = 0;
      for (int j = 0; j < numAttributes - 1; j++) {
	if (instanceCount[j] > 0) {
	  stddevs[attCount] += Math.pow(anInst.value(j) - means[attCount], 2);
	  attCount++;
	}
      }
    }

    for (int i = 0; i < numAttributes - 1; i++) {
      if (instanceCount[i] > 0) {
	stddevs[i] = Math.sqrt(stddevs[i] / instanceCount[i]);		
      }
    }


    //ok, now we have everything, need to make a generator for each column
    for (int i = 0; i < numAttributes - 1; i++) {
      Generator agen;
      if (newData.attribute(attrIndexes[i]).isNominal()) {
	agen = ((Generator)m_DefaultNominalGenerator).copy();
	((NominalAttributeGenerator)agen).buildGenerator(newData, newData.attribute(attrIndexes[i]));
      } else {
	agen = ((Generator)m_DefaultNumericGenerator).copy();
	
	if (agen instanceof Ranged) {
	  ((Ranged)agen).setLowerRange(lowranges[i]);
	  ((Ranged)agen).setUpperRange(highranges[i]);
	}
	
	if (agen instanceof Mean) {
	  ((Mean)agen).setMean(means[i]);
	  ((Mean)agen).setStandardDeviation(stddevs[i]);
	}

	if (agen instanceof InstanceHandler) {
	  //generator needs to be setup with the instances, 
	  //need to pass over a set of instances with just the current 
	  //attribute.
	  StringBuffer sb = new StringBuffer("@relation OneClass-SingleAttribute\n\n");
	  sb.append("@attribute tempName numeric\n\n");
	  sb.append("@data\n\n");
	  Enumeration instancesEnum = newData.enumerateInstances();
	  while(instancesEnum.hasMoreElements()) {
	    Instance aninst = (Instance)instancesEnum.nextElement();
	    if (!aninst.isMissing(attrIndexes[i]))
	      sb.append("" + aninst.value(attrIndexes[i]) + "\n");
	  }
	  sb.append("\n\n");
	  Instances removed = new Instances(new StringReader(sb.toString()));
	  removed.deleteWithMissing(0);
	  ((InstanceHandler)agen).buildGenerator(removed);
	}
      }

      m_Generators.add(agen);
    }


    //REPEAT
    ArrayList thresholds = new ArrayList();
    for (int i = 0; i < m_NumRepeats; i++) {

      //hold some data out 
      Instances copyData = new Instances(newData);
      Instances heldout = new Instances(newData, 0);
      for (int k = 0; k < newData.numInstances() / m_PercentHeldout; k++) {
	int anindex = m_Random.nextInt(copyData.numInstances());
	heldout.add(copyData.instance(anindex));
	copyData.delete(anindex);
      }


      //generate some data
      this.generateData(copyData);

      //build the classifier on the generated data   
      if (!m_UseDensityOnly)
	m_Classifier.buildClassifier(copyData);

      //test the generated data, work out the threshold (average it later)
      double[] scores = new double[heldout.numInstances()];
      Enumeration iterInst = heldout.enumerateInstances();
      int classIndex = heldout.classAttribute().indexOfValue(m_TargetClassLabel);
      int count = 0; 
      while(iterInst.hasMoreElements()) {
	Instance anInst = (Instance)iterInst.nextElement();
	scores[count] = this.getProbXGivenC(anInst, classIndex);	
	count++;
      }

      Arrays.sort(scores);
      //work out the where the threshold should be
      //higher probabilities = passes
      //sorted into ascending order (getting bigger)
      int passposition = (int)((double)heldout.numInstances() * m_TargetRejectionRate);
      if (passposition >=  heldout.numInstances())
	passposition = heldout.numInstances() - 1;

      thresholds.add(new Double(scores[passposition]));
    }
    //END REPEAT



    //build the classifier on the generated data  

    //set the threshold
    m_Threshold = 0;
    for (int k = 0; k < thresholds.size(); k++) {
      m_Threshold += ((Double)thresholds.get(k)).doubleValue();
    }
    m_Threshold /= (double)thresholds.size();

    //rebuild the classifier using all the data
    this.generateData(newData);

    if (!m_UseDensityOnly)
      m_Classifier.buildClassifier(newData);

  }
  

  /**
   * Merges the class values of the instances down to two values,
   * the target class and the "outlier" class.
   *
   * @param newData The data to merge.
   * @return The merged data.
   */
  protected Instances mergeToTwo(Instances newData) throws Exception{

    m_MergeFilter = new MergeManyValues();
    m_MergeFilter.setAttributeIndex("" + (newData.classIndex() + 1));

    //figure out the indexes that aren't the outlier label or
    //the target label
    StringBuffer sb = new StringBuffer("");

    Attribute theAttr = newData.classAttribute();
    for (int i = 0; i < theAttr.numValues(); i++) {
      if (! (theAttr.value(i).equalsIgnoreCase(OneClassClassifier.OUTLIER_LABEL) 
	  || theAttr.value(i).equalsIgnoreCase(m_TargetClassLabel))) {
	//add it to the merge list
	sb.append((i + 1) + ",");
      }
    }
    String mergeList = sb.toString();
    if (mergeList.length() != 0) {
      mergeList = mergeList.substring(0, mergeList.length() - 1);

      m_MergeFilter.setMergeValueRange(mergeList);
      m_MergeFilter.setLabel(OneClassClassifier.OUTLIER_LABEL);
      m_MergeFilter.setInputFormat(newData);
      newData = Filter.useFilter(newData, m_MergeFilter);
    } else {
      m_MergeFilter = null;
    }

    return newData;
  }

  /**
   * Gets the probability that an instance, X, belongs to the target class, C.  
   *
   * @param instance The instance X.
   * @param targetClassIndex The index of the target class label for the class attribute.
   * @return The probability of X given C, P(X|C).
   */
  protected double getProbXGivenC(Instance instance, int targetClassIndex) throws Exception{
    double probC = 1 - m_ProportionGenerated;	
    double probXgivenA = 0;
    int count = 0;
    for (int i = 0; i < instance.numAttributes(); i++) {
      if (i != instance.classIndex()) {
	Generator agen = (Generator)m_Generators.get(count);
	if (!instance.isMissing(i)) {
	  probXgivenA += agen.getLogProbabilityOf(instance.value(i));
	}
	count++;	
      }
    }

    if (m_UseDensityOnly)
      return probXgivenA;

    double[] distribution = m_Classifier.distributionForInstance(instance);
    double probCgivenX = distribution[targetClassIndex];
    if(probCgivenX == 1)
	return Double.POSITIVE_INFINITY;

    //final calculation
    double top = Math.log(1 - probC) + Math.log(probCgivenX);
    double bottom = Math.log(probC) + Math.log(1 - probCgivenX);   

    return (top - bottom) + probXgivenA;	
  }


  /**
   * Generates some outlier data and returns the targetData with some outlier data included.
   *
   * @param targetData The data for the target class.
   * @return The outlier and target data together.
   */
  protected Instances generateData(Instances targetData) {
    double totalInstances = ((double)targetData.numInstances()) / (1 - m_ProportionGenerated);

    int numInstances = (int)(totalInstances - (double)targetData.numInstances());

    //first reweight the target data
    if (m_UseInstanceWeights) {
      for (int i = 0; i < targetData.numInstances(); i++) {
	targetData.instance(i).setWeight(0.5 * (1 / (1 - m_ProportionGenerated)));
      }
    }

    for (int j = 0; j < numInstances; j++) {
      //add to the targetData the instances that we generate...
      Instance anInst = new Instance(targetData.numAttributes());
      anInst.setDataset(targetData);
      int position = 0;
      for (int i = 0; i < targetData.numAttributes(); i++) {
	if (targetData.classIndex() != i) {
	  //not the class attribute
	  Generator agen = (Generator)m_Generators.get(position);
	  anInst.setValue(i, agen.generate());
	  position++;
	} else {
	  //is the class attribute
	  anInst.setValue(i, OneClassClassifier.OUTLIER_LABEL);
	  if (m_UseInstanceWeights)
	    anInst.setWeight(0.5 * (1 / m_ProportionGenerated));
	}

      }
      targetData.add(anInst);	    
    }

    return targetData;
  }

  /**
   * Returns a probability distribution for a given instance.
   * 
   * @param instance The instance to calculate the probability distribution for.
   * @return The probability for each class.
   */
  public double[] distributionForInstance(Instance instance) throws Exception {
    Instance filtered = new Instance(instance);

    m_AddOutlierFilter.input(instance);
    filtered = m_AddOutlierFilter.output();

    if (m_UseLaplaceCorrection && m_MergeFilter != null) {
      m_MergeFilter.input(filtered);
      filtered = m_MergeFilter.output();
    }

    double[] dist = new double[instance.numClasses()];
    double probForOutlierClass = 1 / (1 + Math.exp(this.getProbXGivenC(filtered, filtered.classAttribute().indexOfValue(m_TargetClassLabel)) - m_Threshold));
    if(this.getProbXGivenC(filtered, filtered.classAttribute().indexOfValue(m_TargetClassLabel)) == Double.POSITIVE_INFINITY)
	probForOutlierClass = 0;

    dist[instance.classAttribute().indexOfValue(m_TargetClassLabel)] = 1 - probForOutlierClass; 
    if (instance.classAttribute().indexOfValue(OneClassClassifier.OUTLIER_LABEL) == -1) {
      if (this.getProbXGivenC(filtered, filtered.classAttribute().indexOfValue(m_TargetClassLabel)) >= m_Threshold)
	dist[instance.classAttribute().indexOfValue(m_TargetClassLabel)] = 1;
      else
	dist[instance.classAttribute().indexOfValue(m_TargetClassLabel)] = 0;
    } else
      dist[instance.classAttribute().indexOfValue(OneClassClassifier.OUTLIER_LABEL)] = probForOutlierClass;

    return dist;
  }

  /**
   * Output a representation of this classifier
   * 
   * @return a representation of this classifier
   */
  public String toString() {

    StringBuffer result = new StringBuffer();
    result.append("\n\nClassifier Model\n"+m_Classifier.toString());

    return result.toString();
  }

  /**
   * Returns the revision string.
   *
   * @return The revision string.
   */
  public String getRevision() {
    return "$Revision$";
  }

  /**
   * Main method for executing this classifier.
   *
   * @param args 	use -h to see all available options
   */
  public static void main(String[] args) {
    runClassifier(new OneClassClassifier(), args);
  }
}

