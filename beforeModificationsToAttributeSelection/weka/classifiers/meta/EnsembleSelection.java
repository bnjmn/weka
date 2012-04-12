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
 *    EnsembleSelection.java
 *    Copyright (C) 2006 David Michael
 *
 */

package weka.classifiers.meta;

import weka.classifiers.Evaluation;
import weka.classifiers.RandomizableClassifier;
import weka.classifiers.meta.ensembleSelection.EnsembleMetricHelper;
import weka.classifiers.meta.ensembleSelection.EnsembleSelectionLibrary;
import weka.classifiers.meta.ensembleSelection.EnsembleSelectionLibraryModel;
import weka.classifiers.meta.ensembleSelection.ModelBag;
import weka.classifiers.trees.REPTree;
import weka.classifiers.xml.XMLClassifier;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.xml.KOML;
import weka.core.xml.XMLOptions;
import weka.core.xml.XMLSerialization;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 <!-- globalinfo-start -->
 * Combines several classifiers using the ensemble selection method. For more information, see: Caruana, Rich, Niculescu, Alex, Crew, Geoff, and Ksikes, Alex, Ensemble Selection from Libraries of Models, The International Conference on Machine Learning (ICML'04), 2004.  Implemented in Weka by Bob Jung and David Michael.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{RichCaruana2004,
 *    author = {Rich Caruana, Alex Niculescu, Geoff Crew, and Alex Ksikes},
 *    booktitle = {21st International Conference on Machine Learning},
 *    title = {Ensemble Selection from Libraries of Models},
 *    year = {2004}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 * Our implementation of ensemble selection is a bit different from the other
 * classifiers because we assume that the list of models to be trained is too 
 * large to fit in memory and that our base classifiers will need to be
 * serialized to the file system (in the directory listed in the "workingDirectory
 * option).  We have adopted the term "model library" for this large set of 
 * classifiers keeping in line with the original paper. 
 * <p/>
 * 
 * If you are planning to use this classifier, we highly recommend you take a 
 * quick look at our FAQ/tutorial on the WIKI.  There are a few things that
 * are unique to this classifier that could trip you up.  Otherwise, this
 * method is a great way to get really great classifier performance without 
 * having to do too much parameter tuning.  What is nice is that in the worst 
 * case you get a nice summary of how s large number of diverse models 
 * performed on your data set.  
 * <p/>
 * 
 * This class relies on the package weka.classifiers.meta.ensembleSelection. 
 * <p/>
 * 
 * When run from the Explorer or another GUI, the classifier depends on the
 * package weka.gui.libraryEditor. 
 * <p/>
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -L &lt;/path/to/modelLibrary&gt;
 *  Specifies the Model Library File, continuing the list of all models.</pre>
 * 
 * <pre> -W &lt;/path/to/working/directory&gt;
 *  Specifies the Working Directory, where all models will be stored.</pre>
 * 
 * <pre> -B &lt;numModelBags&gt;
 *  Set the number of bags, i.e., number of iterations to run 
 *  the ensemble selection algorithm.</pre>
 * 
 * <pre> -E &lt;modelRatio&gt;
 *  Set the ratio of library models that will be randomly chosen 
 *  to populate each bag of models.</pre>
 * 
 * <pre> -V &lt;validationRatio&gt;
 *  Set the ratio of the training data set that will be reserved 
 *  for validation.</pre>
 * 
 * <pre> -H &lt;hillClimbIterations&gt;
 *  Set the number of hillclimbing iterations to be performed 
 *  on each model bag.</pre>
 * 
 * <pre> -I &lt;sortInitialization&gt;
 *  Set the the ratio of the ensemble library that the sort 
 *  initialization algorithm will be able to choose from while 
 *  initializing the ensemble for each model bag</pre>
 * 
 * <pre> -X &lt;numFolds&gt;
 *  Sets the number of cross-validation folds.</pre>
 * 
 * <pre> -P &lt;hillclimbMettric&gt;
 *  Specify the metric that will be used for model selection 
 *  during the hillclimbing algorithm.
 *  Valid metrics are: 
 *   accuracy, rmse, roc, precision, recall, fscore, all</pre>
 * 
 * <pre> -A &lt;algorithm&gt;
 *  Specifies the algorithm to be used for ensemble selection. 
 *  Valid algorithms are:
 *   "forward" (default) for forward selection.
 *   "backward" for backward elimination.
 *   "both" for both forward and backward elimination.
 *   "best" to simply print out top performer from the 
 *      ensemble library
 *   "library" to only train the models in the ensemble 
 *      library</pre>
 * 
 * <pre> -R
 *  Flag whether or not models can be selected more than once 
 *  for an ensemble.</pre>
 * 
 * <pre> -G
 *  Whether sort initialization greedily stops adding models 
 *  when performance degrades.</pre>
 * 
 * <pre> -O
 *  Flag for verbose output. Prints out performance of all 
 *  selected models.</pre>
 * 
 * <pre> -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)</pre>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 <!-- options-end -->
 * 
 * @author Robert Jung
 * @author David Michael
 * @version $Revision: 1.1 $
 */
public class EnsembleSelection 
  extends RandomizableClassifier 
  implements Serializable, TechnicalInformationHandler {

  /** for serialization */
  private static final long serialVersionUID = -1744155148765058511L;

  /**
   * The Library of models, from which we can select our ensemble. Usually
   * loaded from a model list file (.mlf or .model.xml) using the -L
   * command-line option.
   */
  protected EnsembleSelectionLibrary m_library = new EnsembleSelectionLibrary();
  
  /**
   * List of models chosen by EnsembleSelection. Populated by buildClassifier.
   */
  protected EnsembleSelectionLibraryModel[] m_chosen_models = null;
  
  /**
   * An array of weights for the chosen models. Elements are parallel to those
   * in m_chosen_models. That is, m_chosen_model_weights[i] is the weight
   * associated with the model at m_chosen_models[i].
   */
  protected int[] m_chosen_model_weights = null;
  
  /** Total weight of all chosen models. */
  protected int m_total_weight = 0;
  
  /**
   * ratio of library models that will be randomly chosen to be used for each
   * model bag
   */
  protected double m_modelRatio = 0.5;
  
  /**
   * Indicates the fraction of the given training set that should be used for
   * hillclimbing/validation. This fraction is set aside and not used for
   * training. It is assumed that any loaded models were also not trained on
   * set-aside data. (If the same percentage and random seed were used
   * previously to train the models in the library, this will work as expected -
   * i.e., those models will be valid)
   */
  protected double m_validationRatio = 0.25;
  
  /** defines metrics that can be chosen for hillclimbing */
  public static final Tag[] TAGS_METRIC = {
    new Tag(EnsembleMetricHelper.METRIC_ACCURACY, "Optimize with Accuracy"),
    new Tag(EnsembleMetricHelper.METRIC_RMSE, "Optimize with RMSE"),
    new Tag(EnsembleMetricHelper.METRIC_ROC, "Optimize with ROC"),
    new Tag(EnsembleMetricHelper.METRIC_PRECISION, "Optimize with precision"),
    new Tag(EnsembleMetricHelper.METRIC_RECALL, "Optimize with recall"),
    new Tag(EnsembleMetricHelper.METRIC_FSCORE, "Optimize with fscore"),
    new Tag(EnsembleMetricHelper.METRIC_ALL, "Optimize with all metrics"), };
  
  /**
   * The "enumeration" of the algorithms we can use. Forward - forward
   * selection. For hillclimb iterations,
   */
  public static final int ALGORITHM_FORWARD = 0;
  
  public static final int ALGORITHM_BACKWARD = 1;
  
  public static final int ALGORITHM_FORWARD_BACKWARD = 2;
  
  public static final int ALGORITHM_BEST = 3;
  
  public static final int ALGORITHM_BUILD_LIBRARY = 4;
  
  /** defines metrics that can be chosen for hillclimbing */
  public static final Tag[] TAGS_ALGORITHM = {
    new Tag(ALGORITHM_FORWARD, "Forward selection"),
    new Tag(ALGORITHM_BACKWARD, "Backward elimation"),
    new Tag(ALGORITHM_FORWARD_BACKWARD, "Forward Selection + Backward Elimination"),
    new Tag(ALGORITHM_BEST, "Best model"),
    new Tag(ALGORITHM_BUILD_LIBRARY, "Build Library Only") };
  
  /**
   * this specifies the number of "Ensembl-X" directories that are allowed to
   * be created in the users home directory where X is the number of the
   * ensemble
   */
  private static final int MAX_DEFAULT_DIRECTORIES = 1000;
  
  /**
   * The name of the Model Library File (if one is specified) which lists
   * models from which ensemble selection will choose. This is only used when
   * run from the command-line, as otherwise m_library is responsible for
   * this.
   */
  protected String m_modelLibraryFileName = null;
  
  /**
   * The number of "model bags". Using 1 is equivalent to no bagging at all.
   */
  protected int m_numModelBags = 10;
  
  /** The metric for which the ensemble will be optimized. */
  protected int m_hillclimbMetric = EnsembleMetricHelper.METRIC_RMSE;
  
  /** The algorithm used for ensemble selection. */
  protected int m_algorithm = ALGORITHM_FORWARD;
  
  /**
   * number of hillclimbing iterations for the ensemble selection algorithm
   */
  protected int m_hillclimbIterations = 100;
  
  /** ratio of library models to be used for sort initialization */
  protected double m_sortInitializationRatio = 1.0;
  
  /**
   * specifies whether or not the ensemble algorithm is allowed to include a
   * specific model in the library more than once in each ensemble
   */
  protected boolean m_replacement = true;
  
  /**
   * specifies whether we use "greedy" sort initialization. If false, we
   * simply add the best m_sortInitializationRatio models of the bag blindly.
   * If true, we add the best models in order up to m_sortInitializationRatio
   * until adding the next model would not help performance.
   */
  protected boolean m_greedySortInitialization = true;
  
  /**
   * Specifies whether or not we will output metrics for all models
   */
  protected boolean m_verboseOutput = false;
  
  /**
   * Hash map of cached predictions. The key is a stringified Instance. Each
   * entry is a 2d array, first indexed by classifier index (i.e., the one
   * used in m_chosen_model). The second index is the usual "distribution"
   * index across classes.
   */
  protected Map m_cachedPredictions = null;
  
  /**
   * This string will store the working directory where all models , temporary
   * prediction values, and modellist logs are to be built and stored.
   */
  protected File m_workingDirectory = new File(getDefaultWorkingDirectory());
  
  /**
   * Indicates the number of folds for cross-validation. A value of 1
   * indicates there is no cross-validation. Cross validation is done in the
   * "embedded" fashion described by Caruana, Niculescu, and Munson
   * (unpublished work - tech report forthcoming)
   */
  protected int m_NumFolds = 1;
  
  /**
   * Returns a string describing classifier
   * 
   * @return a description suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    
    return "Combines several classifiers using the ensemble "
    + "selection method. For more information, see: "
    + "Caruana, Rich, Niculescu, Alex, Crew, Geoff, and Ksikes, Alex, "
    + "Ensemble Selection from Libraries of Models, "
    + "The International Conference on Machine Learning (ICML'04), 2004.  "
    + "Implemented in Weka by Bob Jung and David Michael.";
  }
  
  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector result = new Vector();
    
    result.addElement(new Option(
	"\tSpecifies the Model Library File, continuing the list of all models.",
	"L", 1, "-L </path/to/modelLibrary>"));
    
    result.addElement(new Option(
	"\tSpecifies the Working Directory, where all models will be stored.",
	"W", 1, "-W </path/to/working/directory>"));
    
    result.addElement(new Option(
	"\tSet the number of bags, i.e., number of iterations to run \n"
	+ "\tthe ensemble selection algorithm.",
	"B", 1, "-B <numModelBags>"));
    
    result.addElement(new Option(
	"\tSet the ratio of library models that will be randomly chosen \n"
	+ "\tto populate each bag of models.",
	"E", 1, "-E <modelRatio>"));
    
    result.addElement(new Option(
	"\tSet the ratio of the training data set that will be reserved \n"
	+ "\tfor validation.",
	"V", 1, "-V <validationRatio>"));
    
    result.addElement(new Option(
	"\tSet the number of hillclimbing iterations to be performed \n"
	+ "\ton each model bag.",
	"H", 1, "-H <hillClimbIterations>"));
    
    result.addElement(new Option(
	"\tSet the the ratio of the ensemble library that the sort \n"
	+ "\tinitialization algorithm will be able to choose from while \n"
	+ "\tinitializing the ensemble for each model bag",
	"I", 1, "-I <sortInitialization>"));
    
    result.addElement(new Option(
	"\tSets the number of cross-validation folds.", 
	"X", 1, "-X <numFolds>"));
    
    result.addElement(new Option(
	"\tSpecify the metric that will be used for model selection \n"
	+ "\tduring the hillclimbing algorithm.\n"
	+ "\tValid metrics are: \n"
	+ "\t\taccuracy, rmse, roc, precision, recall, fscore, all",
	"P", 1, "-P <hillclimbMettric>"));
    
    result.addElement(new Option(
	"\tSpecifies the algorithm to be used for ensemble selection. \n"
	+ "\tValid algorithms are:\n"
	+ "\t\t\"forward\" (default) for forward selection.\n"
	+ "\t\t\"backward\" for backward elimination.\n"
	+ "\t\t\"both\" for both forward and backward elimination.\n"
	+ "\t\t\"best\" to simply print out top performer from the \n"
	+ "\t\t   ensemble library\n"
	+ "\t\t\"library\" to only train the models in the ensemble \n"
	+ "\t\t   library",
	"A", 1, "-A <algorithm>"));
    
    result.addElement(new Option(
	"\tFlag whether or not models can be selected more than once \n"
	+ "\tfor an ensemble.",
	"R", 0, "-R"));
    
    result.addElement(new Option(
	"\tWhether sort initialization greedily stops adding models \n"
	+ "\twhen performance degrades.",
	"G", 0, "-G"));
    
    result.addElement(new Option(
	"\tFlag for verbose output. Prints out performance of all \n"
	+ "\tselected models.",
	"O", 0, "-O"));
    
    // TODO - Add more options here
    Enumeration enu = super.listOptions();
    while (enu.hasMoreElements()) {
      result.addElement(enu.nextElement());
    }
    
    return result.elements();
  }
  
  /**
   * We return true for basically everything except for Missing class values,
   * because we can't really answer for all the models in our library. If any of
   * them don't work with the supplied data then we just trap the exception.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities(); // returns the object
    // from
    // weka.classifiers.Classifier
    
    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);
    result.enable(Capability.BINARY_ATTRIBUTES);
    
    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.NUMERIC_CLASS);
    result.enable(Capability.BINARY_CLASS);
    
    return result;
  }
  
  /**
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -L &lt;/path/to/modelLibrary&gt;
   *  Specifies the Model Library File, continuing the list of all models.</pre>
   * 
   * <pre> -W &lt;/path/to/working/directory&gt;
   *  Specifies the Working Directory, where all models will be stored.</pre>
   * 
   * <pre> -B &lt;numModelBags&gt;
   *  Set the number of bags, i.e., number of iterations to run 
   *  the ensemble selection algorithm.</pre>
   * 
   * <pre> -E &lt;modelRatio&gt;
   *  Set the ratio of library models that will be randomly chosen 
   *  to populate each bag of models.</pre>
   * 
   * <pre> -V &lt;validationRatio&gt;
   *  Set the ratio of the training data set that will be reserved 
   *  for validation.</pre>
   * 
   * <pre> -H &lt;hillClimbIterations&gt;
   *  Set the number of hillclimbing iterations to be performed 
   *  on each model bag.</pre>
   * 
   * <pre> -I &lt;sortInitialization&gt;
   *  Set the the ratio of the ensemble library that the sort 
   *  initialization algorithm will be able to choose from while 
   *  initializing the ensemble for each model bag</pre>
   * 
   * <pre> -X &lt;numFolds&gt;
   *  Sets the number of cross-validation folds.</pre>
   * 
   * <pre> -P &lt;hillclimbMettric&gt;
   *  Specify the metric that will be used for model selection 
   *  during the hillclimbing algorithm.
   *  Valid metrics are: 
   *   accuracy, rmse, roc, precision, recall, fscore, all</pre>
   * 
   * <pre> -A &lt;algorithm&gt;
   *  Specifies the algorithm to be used for ensemble selection. 
   *  Valid algorithms are:
   *   "forward" (default) for forward selection.
   *   "backward" for backward elimination.
   *   "both" for both forward and backward elimination.
   *   "best" to simply print out top performer from the 
   *      ensemble library
   *   "library" to only train the models in the ensemble 
   *      library</pre>
   * 
   * <pre> -R
   *  Flag whether or not models can be selected more than once 
   *  for an ensemble.</pre>
   * 
   * <pre> -G
   *  Whether sort initialization greedily stops adding models 
   *  when performance degrades.</pre>
   * 
   * <pre> -O
   *  Flag for verbose output. Prints out performance of all 
   *  selected models.</pre>
   * 
   * <pre> -S &lt;num&gt;
   *  Random number seed.
   *  (default 1)</pre>
   * 
   * <pre> -D
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console</pre>
   * 
   <!-- options-end -->
   *
   * @param options
   *            the list of options as an array of strings
   * @throws Exception
   *                if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    String	tmpStr;
    
    tmpStr = Utils.getOption('L', options);
    if (tmpStr.length() != 0) {
      m_modelLibraryFileName = tmpStr;
      m_library = new EnsembleSelectionLibrary(m_modelLibraryFileName);
    } else {
      setLibrary(new EnsembleSelectionLibrary());
      // setLibrary(new Library(super.m_Classifiers));
    }
    
    tmpStr = Utils.getOption('W', options);
    if (tmpStr.length() != 0 && validWorkingDirectory(tmpStr)) {
      m_workingDirectory = new File(tmpStr);
    } else {
      m_workingDirectory = new File(getDefaultWorkingDirectory());
    }
    m_library.setWorkingDirectory(m_workingDirectory);
    
    tmpStr = Utils.getOption('E', options);
    if (tmpStr.length() != 0) {
      setModelRatio(Double.parseDouble(tmpStr));
    } else {
      setModelRatio(1.0);
    }
    
    tmpStr = Utils.getOption('V', options);
    if (tmpStr.length() != 0) {
      setValidationRatio(Double.parseDouble(tmpStr));
    } else {
      setValidationRatio(0.25);
    }
    
    tmpStr = Utils.getOption('B', options);
    if (tmpStr.length() != 0) {
      setNumModelBags(Integer.parseInt(tmpStr));
    } else {
      setNumModelBags(10);
    }
    
    tmpStr = Utils.getOption('H', options);
    if (tmpStr.length() != 0) {
      setHillclimbIterations(Integer.parseInt(tmpStr));
    } else {
      setHillclimbIterations(100);
    }
    
    tmpStr = Utils.getOption('I', options);
    if (tmpStr.length() != 0) {
      setSortInitializationRatio(Double.parseDouble(tmpStr));
    } else {
      setSortInitializationRatio(1.0);
    }
    
    tmpStr = Utils.getOption('X', options);
    if (tmpStr.length() != 0) {
      setNumFolds(Integer.parseInt(tmpStr));
    } else {
      setNumFolds(10);
    }
    
    setReplacement(Utils.getFlag('R', options));
    
    setGreedySortInitialization(Utils.getFlag('G', options));
    
    setVerboseOutput(Utils.getFlag('O', options));
    
    tmpStr = Utils.getOption('P', options);
    // if (hillclimbMetricString.length() != 0) {
    
    if (tmpStr.toLowerCase().equals("accuracy")) {
      setHillclimbMetric(new SelectedTag(
	  EnsembleMetricHelper.METRIC_ACCURACY, TAGS_METRIC));
    } else if (tmpStr.toLowerCase().equals("rmse")) {
      setHillclimbMetric(new SelectedTag(
	  EnsembleMetricHelper.METRIC_RMSE, TAGS_METRIC));
    } else if (tmpStr.toLowerCase().equals("roc")) {
      setHillclimbMetric(new SelectedTag(
	  EnsembleMetricHelper.METRIC_ROC, TAGS_METRIC));
    } else if (tmpStr.toLowerCase().equals("precision")) {
      setHillclimbMetric(new SelectedTag(
	  EnsembleMetricHelper.METRIC_PRECISION, TAGS_METRIC));
    } else if (tmpStr.toLowerCase().equals("recall")) {
      setHillclimbMetric(new SelectedTag(
	  EnsembleMetricHelper.METRIC_RECALL, TAGS_METRIC));
    } else if (tmpStr.toLowerCase().equals("fscore")) {
      setHillclimbMetric(new SelectedTag(
	  EnsembleMetricHelper.METRIC_FSCORE, TAGS_METRIC));
    } else if (tmpStr.toLowerCase().equals("all")) {
      setHillclimbMetric(new SelectedTag(
	  EnsembleMetricHelper.METRIC_ALL, TAGS_METRIC));
    } else {
      setHillclimbMetric(new SelectedTag(
	  EnsembleMetricHelper.METRIC_RMSE, TAGS_METRIC));
    }
    
    tmpStr = Utils.getOption('A', options);
    if (tmpStr.toLowerCase().equals("forward")) {
      setAlgorithm(new SelectedTag(ALGORITHM_FORWARD, TAGS_ALGORITHM));
    } else if (tmpStr.toLowerCase().equals("backward")) {
      setAlgorithm(new SelectedTag(ALGORITHM_BACKWARD, TAGS_ALGORITHM));
    } else if (tmpStr.toLowerCase().equals("both")) {
      setAlgorithm(new SelectedTag(ALGORITHM_FORWARD_BACKWARD, TAGS_ALGORITHM));
    } else if (tmpStr.toLowerCase().equals("forward")) {
      setAlgorithm(new SelectedTag(ALGORITHM_FORWARD, TAGS_ALGORITHM));
    } else if (tmpStr.toLowerCase().equals("best")) {
      setAlgorithm(new SelectedTag(ALGORITHM_BEST, TAGS_ALGORITHM));
    } else if (tmpStr.toLowerCase().equals("library")) {
      setAlgorithm(new SelectedTag(ALGORITHM_BUILD_LIBRARY, TAGS_ALGORITHM));
    } else {
      setAlgorithm(new SelectedTag(ALGORITHM_FORWARD, TAGS_ALGORITHM));
    }
    
    super.setOptions(options);
    
    m_library.setDebug(m_Debug);
  }
  
  
  /**
   * Gets the current settings of the Classifier.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    Vector        result;
    String[]      options;
    int           i;
    
    result  = new Vector();
    
    if (m_library.getModelListFile() != null) {
      result.add("-L");
      result.add("" + m_library.getModelListFile());
    }
    
    if (!m_workingDirectory.equals("")) {
      result.add("-W");
      result.add("" + getWorkingDirectory());
    }
    
    result.add("-P");
    switch (getHillclimbMetric().getSelectedTag().getID()) {
      case (EnsembleMetricHelper.METRIC_ACCURACY):
	result.add("accuracy");
      break;
      case (EnsembleMetricHelper.METRIC_RMSE):
	result.add("rmse");
      break;
      case (EnsembleMetricHelper.METRIC_ROC):
	result.add("roc");
      break;
      case (EnsembleMetricHelper.METRIC_PRECISION):
	result.add("precision");
      break;
      case (EnsembleMetricHelper.METRIC_RECALL):
	result.add("recall");
      break;
      case (EnsembleMetricHelper.METRIC_FSCORE):
	result.add("fscore");
      break;
      case (EnsembleMetricHelper.METRIC_ALL):
	result.add("all");
      break;
    }
    
    result.add("-A");
    switch (getAlgorithm().getSelectedTag().getID()) {
      case (ALGORITHM_FORWARD):
	result.add("forward");
      break;
      case (ALGORITHM_BACKWARD):
	result.add("backward");
      break;
      case (ALGORITHM_FORWARD_BACKWARD):
	result.add("both");
      break;
      case (ALGORITHM_BEST):
	result.add("best");
      break;
      case (ALGORITHM_BUILD_LIBRARY):
	result.add("library");
      break;
    }
    
    result.add("-B");
    result.add("" + getNumModelBags());
    result.add("-V");
    result.add("" + getValidationRatio());
    result.add("-E");
    result.add("" + getModelRatio());
    result.add("-H");
    result.add("" + getHillclimbIterations());
    result.add("-I");
    result.add("" + getSortInitializationRatio());
    result.add("-X");
    result.add("" + getNumFolds());
    
    if (m_replacement)
      result.add("-R");
    if (m_greedySortInitialization)
      result.add("-G");
    if (m_verboseOutput)
      result.add("-O");
    
    options = super.getOptions();
    for (i = 0; i < options.length; i++)
      result.add(options[i]);
    
    return (String[]) result.toArray(new String[result.size()]);
  }
  
  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String numFoldsTipText() {
    return "The number of folds used for cross-validation.";
  }
  
  /**
   * Gets the number of folds for the cross-validation.
   * 
   * @return the number of folds for the cross-validation
   */
  public int getNumFolds() {
    return m_NumFolds;
  }
  
  /**
   * Sets the number of folds for the cross-validation.
   * 
   * @param numFolds
   *            the number of folds for the cross-validation
   * @throws Exception
   *                if parameter illegal
   */
  public void setNumFolds(int numFolds) throws Exception {
    if (numFolds < 0) {
      throw new IllegalArgumentException(
	  "EnsembleSelection: Number of cross-validation "
	  + "folds must be positive.");
    }
    m_NumFolds = numFolds;
  }
  
  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String libraryTipText() {
    return "An ensemble library.";
  }
  
  /**
   * Gets the ensemble library.
   * 
   * @return the ensemble library
   */
  public EnsembleSelectionLibrary getLibrary() {
    return m_library;
  }
  
  /**
   * Sets the ensemble library.
   * 
   * @param newLibrary
   *            the ensemble library
   */
  public void setLibrary(EnsembleSelectionLibrary newLibrary) {
    m_library = newLibrary;
    m_library.setDebug(m_Debug);
  }
  
  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String modelRatioTipText() {
    return "The ratio of library models that will be randomly chosen to be used for each iteration.";
  }
  
  /**
   * Get the value of modelRatio.
   * 
   * @return Value of modelRatio.
   */
  public double getModelRatio() {
    return m_modelRatio;
  }
  
  /**
   * Set the value of modelRatio.
   * 
   * @param v
   *            Value to assign to modelRatio.
   */
  public void setModelRatio(double v) {
    m_modelRatio = v;
  }
  
  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String validationRatioTipText() {
    return "The ratio of the training data set that will be reserved for validation.";
  }
  
  /**
   * Get the value of validationRatio.
   * 
   * @return Value of validationRatio.
   */
  public double getValidationRatio() {
    return m_validationRatio;
  }
  
  /**
   * Set the value of validationRatio.
   * 
   * @param v
   *            Value to assign to validationRatio.
   */
  public void setValidationRatio(double v) {
    m_validationRatio = v;
  }
  
  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String hillclimbMetricTipText() {
    return "the metric that will be used to optimizer the chosen ensemble..";
  }
  
  /**
   * Gets the hill climbing metric. Will be one of METRIC_ACCURACY,
   * METRIC_RMSE, METRIC_ROC, METRIC_PRECISION, METRIC_RECALL, METRIC_FSCORE,
   * METRIC_ALL
   * 
   * @return the hillclimbMetric
   */
  public SelectedTag getHillclimbMetric() {
    return new SelectedTag(m_hillclimbMetric, TAGS_METRIC);
  }
  
  /**
   * Sets the hill climbing metric. Will be one of METRIC_ACCURACY,
   * METRIC_RMSE, METRIC_ROC, METRIC_PRECISION, METRIC_RECALL, METRIC_FSCORE,
   * METRIC_ALL
   * 
   * @param newType
   *            the new hillclimbMetric
   */
  public void setHillclimbMetric(SelectedTag newType) {
    if (newType.getTags() == TAGS_METRIC) {
      m_hillclimbMetric = newType.getSelectedTag().getID();
    }
  }
  
  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String algorithmTipText() {
    return "the algorithm used to optimizer the ensemble";
  }
  
  /**
   * Gets the algorithm
   * 
   * @return the algorithm
   */
  public SelectedTag getAlgorithm() {
    return new SelectedTag(m_algorithm, TAGS_ALGORITHM);
  }
  
  /**
   * Sets the Algorithm to use
   * 
   * @param newType
   *            the new algorithm
   */
  public void setAlgorithm(SelectedTag newType) {
    if (newType.getTags() == TAGS_ALGORITHM) {
      m_algorithm = newType.getSelectedTag().getID();
    }
  }
  
  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String hillclimbIterationsTipText() {
    return "The number of hillclimbing iterations for the ensemble selection algorithm.";
  }
  
  /**
   * Gets the number of hillclimbIterations.
   * 
   * @return the number of hillclimbIterations
   */
  public int getHillclimbIterations() {
    return m_hillclimbIterations;
  }
  
  /**
   * Sets the number of hillclimbIterations.
   * 
   * @param n
   *            the number of hillclimbIterations
   * @throws Exception
   *                if parameter illegal
   */
  public void setHillclimbIterations(int n) throws Exception {
    if (n < 0) {
      throw new IllegalArgumentException(
	  "EnsembleSelection: Number of hillclimb iterations "
	  + "must be positive.");
    }
    m_hillclimbIterations = n;
  }
  
  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String numModelBagsTipText() {
    return "The number of \"model bags\" used in the ensemble selection algorithm.";
  }
  
  /**
   * Gets numModelBags.
   * 
   * @return numModelBags
   */
  public int getNumModelBags() {
    return m_numModelBags;
  }
  
  /**
   * Sets numModelBags.
   * 
   * @param n
   *            the new value for numModelBags
   * @throws Exception
   *                if parameter illegal
   */
  public void setNumModelBags(int n) throws Exception {
    if (n <= 0) {
      throw new IllegalArgumentException(
	  "EnsembleSelection: Number of model bags "
	  + "must be positive.");
    }
    m_numModelBags = n;
  }
  
  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String sortInitializationRatioTipText() {
    return "The ratio of library models to be used for sort initialization.";
  }
  
  /**
   * Get the value of sortInitializationRatio.
   * 
   * @return Value of sortInitializationRatio.
   */
  public double getSortInitializationRatio() {
    return m_sortInitializationRatio;
  }
  
  /**
   * Set the value of sortInitializationRatio.
   * 
   * @param v
   *            Value to assign to sortInitializationRatio.
   */
  public void setSortInitializationRatio(double v) {
    m_sortInitializationRatio = v;
  }
  
  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String replacementTipText() {
    return "Whether models in the library can be included more than once in an ensemble.";
  }
  
  /**
   * Get the value of replacement.
   * 
   * @return Value of replacement.
   */
  public boolean getReplacement() {
    return m_replacement;
  }
  
  /**
   * Set the value of replacement.
   * 
   * @param newReplacement
   *            Value to assign to replacement.
   */
  public void setReplacement(boolean newReplacement) {
    m_replacement = newReplacement;
  }
  
  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String greedySortInitializationTipText() {
    return "Whether sort initialization greedily stops adding models when performance degrades.";
  }
  
  /**
   * Get the value of greedySortInitialization.
   * 
   * @return Value of replacement.
   */
  public boolean getGreedySortInitialization() {
    return m_greedySortInitialization;
  }
  
  /**
   * Set the value of greedySortInitialization.
   * 
   * @param newGreedySortInitialization
   *            Value to assign to replacement.
   */
  public void setGreedySortInitialization(boolean newGreedySortInitialization) {
    m_greedySortInitialization = newGreedySortInitialization;
  }
  
  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String verboseOutputTipText() {
    return "Whether metrics are printed for each model.";
  }
  
  /**
   * Get the value of verboseOutput.
   * 
   * @return Value of verboseOutput.
   */
  public boolean getVerboseOutput() {
    return m_verboseOutput;
  }
  
  /**
   * Set the value of verboseOutput.
   * 
   * @param newVerboseOutput
   *            Value to assign to verboseOutput.
   */
  public void setVerboseOutput(boolean newVerboseOutput) {
    m_verboseOutput = newVerboseOutput;
  }
  
  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String workingDirectoryTipText() {
    return "The working directory of the ensemble - where trained models will be stored.";
  }
  
  /**
   * Get the value of working directory.
   * 
   * @return Value of working directory.
   */
  public File getWorkingDirectory() {
    return m_workingDirectory;
  }
  
  /**
   * Set the value of working directory.
   * 
   * @param newWorkingDirectory	directory Value.
   */
  public void setWorkingDirectory(File newWorkingDirectory) {
    if (m_Debug) {
      System.out.println("working directory changed to: "
	  + newWorkingDirectory);
    }
    m_library.setWorkingDirectory(newWorkingDirectory);
    
    m_workingDirectory = newWorkingDirectory;
  }
  
  /**
   * Buildclassifier selects a classifier from the set of classifiers by
   * minimising error on the training data.
   * 
   * @param trainData	the training data to be used for generating the boosted
   *            	classifier.
   * @throws Exception	if the classifier could not be built successfully
   */
  public void buildClassifier(Instances trainData) throws Exception {
    
    getCapabilities().testWithFail(trainData);
    
    // First we need to make sure that some library models
    // were specified. If not, then use the default list
    if (m_library.m_Models.size() == 0) {
      
      System.out
      .println("WARNING: No library file specified.  Using some default models.");
      System.out
      .println("You should specify a model list with -L <file> from the command line.");
      System.out
      .println("Or edit the list directly with the LibraryEditor from the GUI");
      
      for (int i = 0; i < 10; i++) {
	
	REPTree tree = new REPTree();
	tree.setSeed(i);
	m_library.addModel(new EnsembleSelectionLibraryModel(tree));
	
      }
      
    }
    
    if (m_library == null) {
      m_library = new EnsembleSelectionLibrary();
      m_library.setDebug(m_Debug);
    }
    
    m_library.setNumFolds(getNumFolds());
    m_library.setValidationRatio(getValidationRatio());
    // train all untrained models, and set "data" to the hillclimbing set.
    Instances data = m_library.trainAll(trainData, m_workingDirectory.getAbsolutePath(),
	m_algorithm);
    // We cache the hillclimb predictions from all of the models in
    // the library so that we can evaluate their performances when we
    // combine them
    // in various ways (without needing to keep the classifiers in memory).
    double predictions[][][] = m_library.getHillclimbPredictions();
    int numModels = predictions.length;
    int modelWeights[] = new int[numModels];
    m_total_weight = 0;
    Random rand = new Random(m_Seed);
    
    if (m_algorithm == ALGORITHM_BUILD_LIBRARY) {
      return;
      
    } else if (m_algorithm == ALGORITHM_BEST) {
      // If we want to choose the best model, just make a model bag that
      // includes all the models, then sort initialize to find the 1 that
      // performs best.
      ModelBag model_bag = new ModelBag(predictions, 1.0, m_Debug);
      int[] modelPicked = model_bag.sortInitialize(1, false, data,
	  m_hillclimbMetric);
      // Then give it a weight of 1, while all others remain 0.
      modelWeights[modelPicked[0]] = 1;
    } else {
      
      if (m_Debug)
	System.out.println("Starting hillclimbing algorithm: "
	    + m_algorithm);
      
      for (int i = 0; i < getNumModelBags(); ++i) {
	// For the number of bags,
	if (m_Debug)
	  System.out.println("Starting on ensemble bag: " + i);
	// Create a new bag of the appropriate size
	ModelBag modelBag = new ModelBag(predictions, getModelRatio(),
	    m_Debug);
	// And shuffle it.
	modelBag.shuffle(rand);
	if (getSortInitializationRatio() > 0.0) {
	  // Sort initialize, if the ratio greater than 0.
	  modelBag.sortInitialize((int) (getSortInitializationRatio()
	      * getModelRatio() * numModels),
	      getGreedySortInitialization(), data,
	      m_hillclimbMetric);
	}
	
	if (m_algorithm == ALGORITHM_BACKWARD) {
	  // If we're doing backwards elimination, we just give all
	  // models
	  // a weight of 1 initially. If the # of hillclimb iterations
	  // is too high, we'll end up with just one model in the end
	  // (we never delete all models from a bag). TODO - it might
	  // be
	  // smarter to base this weight off of how many models we
	  // have.
	  modelBag.weightAll(1); // for now at least, I'm just
	  // assuming 1.
	}
	// Now the bag is initialized, and we're ready to hillclimb.
	for (int j = 0; j < getHillclimbIterations(); ++j) {
	  if (m_algorithm == ALGORITHM_FORWARD) {
	    modelBag.forwardSelect(getReplacement(), data,
		m_hillclimbMetric);
	  } else if (m_algorithm == ALGORITHM_BACKWARD) {
	    modelBag.backwardEliminate(data, m_hillclimbMetric);
	  } else if (m_algorithm == ALGORITHM_FORWARD_BACKWARD) {
	    modelBag.forwardSelectOrBackwardEliminate(
		getReplacement(), data, m_hillclimbMetric);
	  }
	}
	// Now that we've done all the hillclimbing steps, we can just
	// get
	// the model weights that the bag determined, and add them to
	// our
	// running total.
	int[] bagWeights = modelBag.getModelWeights();
	for (int j = 0; j < bagWeights.length; ++j) {
	  modelWeights[j] += bagWeights[j];
	}
      }
    }
    // Now we've done the hard work of actually learning the ensemble. Now
    // we set up the appropriate data structures so that Ensemble Selection
    // can
    // make predictions for future test examples.
    Set modelNames = m_library.getModelNames();
    String[] modelNamesArray = new String[m_library.size()];
    Iterator iter = modelNames.iterator();
    // libraryIndex indexes over all the models in the library (not just
    // those
    // which we chose for the ensemble).
    int libraryIndex = 0;
    // chosenModels will count the total number of models which were
    // selected
    // by EnsembleSelection (those that have non-zero weight).
    int chosenModels = 0;
    while (iter.hasNext()) {
      // Note that we have to be careful of order. Our model_weights array
      // is in the same order as our list of models in m_library.
      
      // Get the name of the model,
      modelNamesArray[libraryIndex] = (String) iter.next();
      // and its weight.
      int weightOfModel = modelWeights[libraryIndex++];
      m_total_weight += weightOfModel;
      if (weightOfModel > 0) {
	// If the model was chosen at least once, increment the
	// number of chosen models.
	++chosenModels;
      }
    }
    if (m_verboseOutput) {
      // Output every model and its performance with respect to the
      // validation
      // data.
      ModelBag bag = new ModelBag(predictions, 1.0, m_Debug);
      int modelIndexes[] = bag.sortInitialize(modelNamesArray.length,
	  false, data, m_hillclimbMetric);
      double modelPerformance[] = bag.getIndividualPerformance(data,
	  m_hillclimbMetric);
      for (int i = 0; i < modelIndexes.length; ++i) {
	// TODO - Could do this in a more readable way.
	System.out.println("" + modelPerformance[i] + " "
	    + modelNamesArray[modelIndexes[i]]);
      }
    }
    // We're now ready to build our array of the models which were chosen
    // and there associated weights.
    m_chosen_models = new EnsembleSelectionLibraryModel[chosenModels];
    m_chosen_model_weights = new int[chosenModels];
    
    libraryIndex = 0;
    // chosenIndex indexes over the models which were chosen by
    // EnsembleSelection
    // (those which have non-zero weight).
    int chosenIndex = 0;
    iter = m_library.getModels().iterator();
    while (iter.hasNext()) {
      int weightOfModel = modelWeights[libraryIndex++];
      
      EnsembleSelectionLibraryModel model = (EnsembleSelectionLibraryModel) iter
      .next();
      
      if (weightOfModel > 0) {
	// If the model was chosen at least once, add it to our array
	// of chosen models and weights.
	m_chosen_models[chosenIndex] = model;
	m_chosen_model_weights[chosenIndex] = weightOfModel;
	// Note that the EnsembleSelectionLibraryModel may not be
	// "loaded" -
	// that is, its classifier(s) may be null pointers. That's okay
	// -
	// we'll "rehydrate" them later, if and when we need to.
	++chosenIndex;
      }
    }
  }
  
  /**
   * Calculates the class membership probabilities for the given test instance.
   *
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @throws Exception if instance could not be classified
   * successfully
   */
  public double[] distributionForInstance(Instance instance) throws Exception {
    String stringInstance = instance.toString();
    double cachedPreds[][] = null;
    
    if (m_cachedPredictions != null) {
      // If we have any cached predictions (i.e., if cachePredictions was
      // called), look for a cached set of predictions for this instance.
      if (m_cachedPredictions.containsKey(stringInstance)) {
	cachedPreds = (double[][]) m_cachedPredictions.get(stringInstance);
      }
    }
    double[] prediction = new double[instance.numClasses()];
    for (int i = 0; i < prediction.length; ++i) {
      prediction[i] = 0.0;
    }
    
    // Now do a weighted average of the predictions of each of our models.
    for (int i = 0; i < m_chosen_models.length; ++i) {
      double[] predictionForThisModel = null;
      if (cachedPreds == null) {
	// If there are no predictions cached, we'll load the model's
	// classifier(s) in to memory and get the predictions.
	m_chosen_models[i].rehydrateModel(m_workingDirectory.getAbsolutePath());
	predictionForThisModel = m_chosen_models[i].getAveragePrediction(instance);
	// We could release the model here to save memory, but we assume
	// that there is enough available since we're not using the
	// prediction caching functionality. If we load and release a
	// model
	// every time we need to get a prediction for an instance, it
	// can be
	// prohibitively slow.
      } else {
	// If it's cached, just get it from the array of cached preds
	// for this instance.
	predictionForThisModel = cachedPreds[i];
      }
      // We have encountered a bug where MultilayerPerceptron returns a
      // null
      // prediction array. If that happens, we just don't count that model
      // in
      // our ensemble prediction.
      if (predictionForThisModel != null) {
	// Okay, the model returned a valid prediction array, so we'll
	// add the appropriate fraction of this model's prediction.
	for (int j = 0; j < prediction.length; ++j) {
	  prediction[j] += m_chosen_model_weights[i] * predictionForThisModel[j] / m_total_weight;
	}
      }
    }
    // normalize to add up to 1.
    if (instance.classAttribute().isNominal()) {
      if (Utils.sum(prediction) > 0)
	Utils.normalize(prediction);
    }
    return prediction;
  }
  
  /**
   * This function tests whether or not a given path is appropriate for being
   * the working directory. Specifically, we care that we can write to the
   * path and that it doesn't point to a "non-directory" file handle.
   * 
   * @param dir		the directory to test
   * @return 		true if the directory is valid
   */
  private boolean validWorkingDirectory(String dir) {
    
    boolean valid = false;
    
    File f = new File((dir));
    
    if (f.exists()) {
      if (f.isDirectory() && f.canWrite())
	valid = true;
    } else {
      if (f.canWrite())
	valid = true;
    }
    
    return valid;
    
  }
  
  /**
   * This method tries to find a reasonable path name for the ensemble working
   * directory where models and files will be stored.
   * 
   * 
   * @return true if m_workingDirectory now has a valid file name
   */
  public static String getDefaultWorkingDirectory() {
    
    String defaultDirectory = new String("");
    
    boolean success = false;
    
    int i = 1;
    
    while (i < MAX_DEFAULT_DIRECTORIES && !success) {
      
      File f = new File(System.getProperty("user.home"), "Ensemble-" + i);
      
      if (!f.exists() && f.getParentFile().canWrite()) {
	defaultDirectory = f.getPath();
	success = true;
      }
      i++;
      
    }
    
    if (!success) {
      defaultDirectory = new String("");
      // should we print an error or something?
    }
    
    return defaultDirectory;
  }
  
  /**
   * Output a representation of this classifier
   * 
   * @return	a string representation of the classifier
   */
  public String toString() {
    // We just print out the models which were selected, and the number
    // of times each was selected.
    String result = new String();
    if (m_chosen_models != null) {
      for (int i = 0; i < m_chosen_models.length; ++i) {
	result += m_chosen_model_weights[i];
	result += " " + m_chosen_models[i].getStringRepresentation()
	+ "\n";
      }
    } else {
      result = "No models selected.";
    }
    return result;
  }
  
  /**
   * Cache predictions for the individual base classifiers in the ensemble
   * with respect to the given dataset. This is used so that when testing a
   * large ensemble on a test set, we don't have to keep the models in memory.
   * 
   * @param test 	The instances for which to cache predictions.
   * @throws Exception 	if somethng goes wrong
   */
  private void cachePredictions(Instances test) throws Exception {
    m_cachedPredictions = new HashMap();
    Evaluation evalModel = null;
    Instances originalInstances = null;
    // If the verbose flag is set, we'll also print out the performances of
    // all the individual models w.r.t. this test set while we're at it.
    boolean printModelPerformances = getVerboseOutput();
    if (printModelPerformances) {
      // To get performances, we need to keep the class attribute.
      originalInstances = new Instances(test);
    }
    
    // For each model, we'll go through the dataset and get predictions.
    // The idea is we want to only have one model in memory at a time, so
    // we'll
    // load one model in to memory, get all its predictions, and add them to
    // the
    // hash map. Then we can release it from memory and move on to the next.
    for (int i = 0; i < m_chosen_models.length; ++i) {
      if (printModelPerformances) {
	// If we're going to print predictions, we need to make a new
	// Evaluation object.
	evalModel = new Evaluation(originalInstances);
      }
      
      Date startTime = new Date();
      
      // Load the model in to memory.
      m_chosen_models[i].rehydrateModel(m_workingDirectory.getAbsolutePath());
      // Now loop through all the instances and get the model's
      // predictions.
      for (int j = 0; j < test.numInstances(); ++j) {
	Instance currentInstance = test.instance(j);
	// When we're looking for a cached prediction later, we'll only
	// have the non-class attributes, so we set the class missing
	// here
	// in order to make the string match up properly.
	currentInstance.setClassMissing();
	String stringInstance = currentInstance.toString();
	
	// When we come in here with the first model, the instance will
	// not
	// yet be part of the map.
	if (!m_cachedPredictions.containsKey(stringInstance)) {
	  // The instance isn't in the map yet, so add it.
	  // For each instance, we store a two-dimensional array - the
	  // first
	  // index is over all the models in the ensemble, and the
	  // second
	  // index is over the (i.e., typical prediction array).
	  int predSize = test.classAttribute().isNumeric() ? 1 : test
	      .classAttribute().numValues();
	  double predictionArray[][] = new double[m_chosen_models.length][predSize];
	  m_cachedPredictions.put(stringInstance, predictionArray);
	}
	// Get the array from the map which is associated with this
	// instance
	double predictions[][] = (double[][]) m_cachedPredictions
	.get(stringInstance);
	// And add our model's prediction for it.
	predictions[i] = m_chosen_models[i].getAveragePrediction(test
	    .instance(j));
	
	if (printModelPerformances) {
	  evalModel.evaluateModelOnceAndRecordPrediction(
	      predictions[i], originalInstances.instance(j));
	}
      }
      // Now we're done with model #i, so we can release it.
      m_chosen_models[i].releaseModel();
      
      Date endTime = new Date();
      long diff = endTime.getTime() - startTime.getTime();
      
      if (m_Debug)
	System.out.println("Test time for "
	    + m_chosen_models[i].getStringRepresentation()
	    + " was: " + diff);
      
      if (printModelPerformances) {
	String output = new String(m_chosen_models[i]
	                                           .getStringRepresentation()
	                                           + ": ");
	output += "\tRMSE:" + evalModel.rootMeanSquaredError();
	output += "\tACC:" + evalModel.pctCorrect();
	if (test.numClasses() == 2) {
	  // For multiclass problems, we could print these too, but
	  // it's
	  // not clear which class we should use in that case... so
	  // instead
	  // we only print these metrics for binary classification
	  // problems.
	  output += "\tROC:" + evalModel.areaUnderROC(1);
	  output += "\tPREC:" + evalModel.precision(1);
	  output += "\tFSCR:" + evalModel.fMeasure(1);
	}
	System.out.println(output);
      }
    }
  }
  
  /**
   * Return the technical information.  There is actually another
   * paper that describes our current method of CV for this classifier
   * TODO: Cite Technical report when published
   * 
   * @return the technical information about this class
   */
  public TechnicalInformation getTechnicalInformation() {
    
    TechnicalInformation result;
    
    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "Rich Caruana, Alex Niculescu, Geoff Crew, and Alex Ksikes");
    result.setValue(Field.TITLE, "Ensemble Selection from Libraries of Models");
    result.setValue(Field.BOOKTITLE, "21st International Conference on Machine Learning");
    result.setValue(Field.YEAR, "2004");
    
    return result;
  }
  
  /**
   * Executes the classifier from commandline.
   * 
   * @param argv
   *            should contain the following arguments: -t training file [-T
   *            test file] [-c class index]
   */
  public static void main(String[] argv) {
    
    try {
      
      String options[] = (String[]) argv.clone();
      
      // do we get the input from XML instead of normal parameters?
      String xml = Utils.getOption("xml", options);
      if (!xml.equals(""))
	options = new XMLOptions(xml).toArray();
      
      String trainFileName = Utils.getOption('t', options);
      String objectInputFileName = Utils.getOption('l', options);
      String testFileName = Utils.getOption('T', options);
      
      if (testFileName.length() != 0 && objectInputFileName.length() != 0
	  && trainFileName.length() == 0) {
	
	System.out.println("Caching predictions");
	
	EnsembleSelection classifier = null;
	
	BufferedReader testReader = new BufferedReader(new FileReader(
	    testFileName));
	
	// Set up the Instances Object
	Instances test;
	int classIndex = -1;
	String classIndexString = Utils.getOption('c', options);
	if (classIndexString.length() != 0) {
	  classIndex = Integer.parseInt(classIndexString);
	}
	
	test = new Instances(testReader, 1);
	if (classIndex != -1) {
	  test.setClassIndex(classIndex - 1);
	} else {
	  test.setClassIndex(test.numAttributes() - 1);
	}
	if (classIndex > test.numAttributes()) {
	  throw new Exception("Index of class attribute too large.");
	}
	
	while (test.readInstance(testReader)) {
	  
	}
	testReader.close();
	
	// Now yoink the EnsembleSelection Object from the fileSystem
	
	InputStream is = new FileInputStream(objectInputFileName);
	if (objectInputFileName.endsWith(".gz")) {
	  is = new GZIPInputStream(is);
	}
	
	// load from KOML?
	if (!(objectInputFileName.endsWith("UpdateableClassifier.koml") && KOML
	    .isPresent())) {
	  ObjectInputStream objectInputStream = new ObjectInputStream(
	      is);
	  classifier = (EnsembleSelection) objectInputStream
	  .readObject();
	  objectInputStream.close();
	} else {
	  BufferedInputStream xmlInputStream = new BufferedInputStream(
	      is);
	  classifier = (EnsembleSelection) KOML.read(xmlInputStream);
	  xmlInputStream.close();
	}
	
	String workingDir = Utils.getOption('W', argv);
	if (!workingDir.equals("")) {
	  classifier.setWorkingDirectory(new File(workingDir));
	}
	
	classifier.setDebug(Utils.getFlag('D', argv));
	classifier.setVerboseOutput(Utils.getFlag('O', argv));
	
	classifier.cachePredictions(test);
	
	// Now we write the model back out to the file system.
	String objectOutputFileName = objectInputFileName;
	OutputStream os = new FileOutputStream(objectOutputFileName);
	// binary
	if (!(objectOutputFileName.endsWith(".xml") || (objectOutputFileName
	    .endsWith(".koml") && KOML.isPresent()))) {
	  if (objectOutputFileName.endsWith(".gz")) {
	    os = new GZIPOutputStream(os);
	  }
	  ObjectOutputStream objectOutputStream = new ObjectOutputStream(
	      os);
	  objectOutputStream.writeObject(classifier);
	  objectOutputStream.flush();
	  objectOutputStream.close();
	}
	// KOML/XML
	else {
	  BufferedOutputStream xmlOutputStream = new BufferedOutputStream(
	      os);
	  if (objectOutputFileName.endsWith(".xml")) {
	    XMLSerialization xmlSerial = new XMLClassifier();
	    xmlSerial.write(xmlOutputStream, classifier);
	  } else
	    // whether KOML is present has already been checked
	    // if not present -> ".koml" is interpreted as binary - see
	    // above
	    if (objectOutputFileName.endsWith(".koml")) {
	      KOML.write(xmlOutputStream, classifier);
	    }
	  xmlOutputStream.close();
	}
	
      }
      
      System.out.println(Evaluation.evaluateModel(
	  new EnsembleSelection(), argv));
      
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}
