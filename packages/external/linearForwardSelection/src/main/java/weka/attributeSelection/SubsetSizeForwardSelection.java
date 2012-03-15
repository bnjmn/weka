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
 *    SubsetSizeForwardSelection.java
 *    Copyright (C) 2007 Martin Guetlein
 *
 */
package weka.attributeSelection;

import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.TechnicalInformation;
import weka.core.Utils;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;

import java.util.BitSet;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;


/**
 <!-- globalinfo-start -->
 * SubsetSizeForwardSelection:<br/>
 * <br/>
 * Extension of LinearForwardSelection. The search performs an interior cross-validation (seed and number of folds can be specified). A LinearForwardSelection is performed on each foldto determine the optimal subset-size (using the given SubsetSizeEvaluator). Finally, a LinearForwardSelection up to the optimal subset-size is performed on the whole data.<br/>
 * <br/>
 * For more information see:<br/>
 * <br/>
 * Martin Guetlein (2006). Large Scale Attribute Selection Using Wrappers. Freiburg, Germany.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -I
 *  Perform initial ranking to select the
 *  top-ranked attributes.</pre>
 * 
 * <pre> -K &lt;num&gt;
 *  Number of top-ranked attributes that are 
 *  taken into account by the search.</pre>
 * 
 * <pre> -T &lt;0 = fixed-set | 1 = fixed-width&gt;
 *  Type of Linear Forward Selection (default = 0).</pre>
 * 
 * <pre> -S &lt;num&gt;
 *  Size of lookup cache for evaluated subsets.
 *  Expressed as a multiple of the number of
 *  attributes in the data set. (default = 1)</pre>
 * 
 * <pre> -E &lt;subset evaluator&gt;
 *  Subset-evaluator used for subset-size determination.-- -M</pre>
 * 
 * <pre> -F &lt;num&gt;
 *  Number of cross validation folds
 *  for subset size determination (default = 5).</pre>
 * 
 * <pre> -R &lt;num&gt;
 *  Seed for cross validation
 *  subset size determination. (default = 1)</pre>
 * 
 * <pre> -Z
 *  verbose on/off</pre>
 * 
 * <pre> 
 * Options specific to evaluator weka.attributeSelection.ClassifierSubsetEval:
 * </pre>
 * 
 * <pre> -B &lt;classifier&gt;
 *  class name of the classifier to use for accuracy estimation.
 *  Place any classifier options LAST on the command line
 *  following a "--". eg.:
 *   -B weka.classifiers.bayes.NaiveBayes ... -- -K
 *  (default: weka.classifiers.rules.ZeroR)</pre>
 * 
 * <pre> -T
 *  Use the training data to estimate accuracy.</pre>
 * 
 * <pre> -H &lt;filename&gt;
 *  Name of the hold out/test set to 
 *  estimate accuracy on.</pre>
 * 
 * <pre> 
 * Options specific to scheme weka.classifiers.rules.ZeroR:
 * </pre>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 <!-- options-end -->
 *
 * @author Martin Guetlein (martin.guetlein@gmail.com)
 * @version $Revision$
 */
public class SubsetSizeForwardSelection extends ASSearch
  implements OptionHandler {
  /** search directions */
  protected static final int TYPE_FIXED_SET = 0;
  protected static final int TYPE_FIXED_WIDTH = 1;
  public static final Tag[] TAGS_TYPE = {
    new Tag(TYPE_FIXED_SET, "Fixed-set"),
    new Tag(TYPE_FIXED_WIDTH, "Fixed-width"),
  };

  // member variables
  /** perform initial ranking to select top-ranked attributes */
  protected boolean m_performRanking;

  /**
   * number of top-ranked attributes that are taken into account for the
   * search
   */
  protected int m_numUsedAttributes;

  /** 0 == fixed-set, 1 == fixed-width */
  protected int m_linearSelectionType;

  /** the subset evaluator to use for subset size determination */
  private ASEvaluation m_setSizeEval;

  /**
   * Number of cross validation folds for subset size determination (default =
   * 5).
   */
  protected int m_numFolds;

  /** Seed for cross validation subset size determination. (default = 1) */
  protected int m_seed;

  /** number of attributes in the data */
  protected int m_numAttribs;

  /** total number of subsets evaluated during a search */
  protected int m_totalEvals;

  /** for debugging */
  protected boolean m_verbose;

  /** holds the merit of the best subset found */
  protected double m_bestMerit;

  /** holds the maximum size of the lookup cache for evaluated subsets */
  protected int m_cacheSize;

  /**
   * Constructor
   */
  public SubsetSizeForwardSelection() {
    resetOptions();
  }

  /**
   * Returns a string describing this search method
   *
   * @return a description of the search method suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "SubsetSizeForwardSelection:\n\n" +
      "Extension of LinearForwardSelection. The search performs an interior " +
      "cross-validation (seed and number of folds can be specified). A " +
      "LinearForwardSelection is performed on each foldto determine the optimal " +
      "subset-size (using the given SubsetSizeEvaluator). Finally, a " +
      "LinearForwardSelection up to the optimal subset-size is performed on " +
      "the whole data.\n\n"
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
    TechnicalInformation        additional;
    
    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "Martin Guetlein and Eibe Frank and Mark Hall");
    result.setValue(Field.YEAR, "2009");
    result.setValue(Field.TITLE, "Large Scale Attribute Selection Using Wrappers");
    result.setValue(Field.BOOKTITLE, "Proc IEEE Symposium on Computational Intelligence and Data Mining");
    result.setValue(Field.PAGES, "332-339");
    result.setValue(Field.PUBLISHER, "IEEE");
    
    additional = result.add(Type.MASTERSTHESIS);
    additional.setValue(Field.AUTHOR, "Martin Guetlein");
    additional.setValue(Field.YEAR, "2006");
    additional.setValue(Field.TITLE, "Large Scale Attribute Selection Using Wrappers");
    additional.setValue(Field.SCHOOL, "Albert-Ludwigs-Universitaet");
    additional.setValue(Field.ADDRESS, "Freiburg, Germany");
    
    return result;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   *
   */
  public Enumeration listOptions() {
    Vector newVector = new Vector(9);

    newVector.addElement(new Option("\tPerform initial ranking to select the" +
                                    "\n\ttop-ranked attributes.", "I", 0, "-I"));
    newVector.addElement(new Option(
                                    "\tNumber of top-ranked attributes that are " +
                                    "\n\ttaken into account by the search.", "K", 1, "-K <num>"));
    newVector.addElement(new Option(
                                    "\tType of Linear Forward Selection (default = 0).", "T", 1,
                                    "-T <0 = fixed-set | 1 = fixed-width>"));
    newVector.addElement(new Option(
                                    "\tSize of lookup cache for evaluated subsets." +
                                    "\n\tExpressed as a multiple of the number of" +
                                    "\n\tattributes in the data set. (default = 1)", "S", 1, "-S <num>"));
    newVector.addElement(new Option(
                                    "\tSubset-evaluator used for subset-size determination." + "-- -M",
                                    "E", 1, "-E <subset evaluator>"));
    newVector.addElement(new Option("\tNumber of cross validation folds" +
                                    "\n\tfor subset size determination (default = 5).", "F", 1, "-F <num>"));
    newVector.addElement(new Option("\tSeed for cross validation" +
                                    "\n\tsubset size determination. (default = 1)", "R", 1, "-R <num>"));
    newVector.addElement(new Option("\tverbose on/off", "Z", 0, "-Z"));

    if ((m_setSizeEval != null) && (m_setSizeEval instanceof OptionHandler)) {
      newVector.addElement(new Option("", "", 0,
                                      "\nOptions specific to " + "evaluator " +
                                      m_setSizeEval.getClass().getName() + ":"));

      Enumeration enu = ((OptionHandler) m_setSizeEval).listOptions();

      while (enu.hasMoreElements()) {
        newVector.addElement(enu.nextElement());
      }
    }

    return newVector.elements();
  }

  /**
   * Parses a given list of options.
   *
   * Valid options are:
   * <p>
   *
   * -I <br>
   * Perform initial ranking to select top-ranked attributes.
   * <p>
   *
   * -K <num> <br>
   * Number of top-ranked attributes that are taken into account.
   * <p>
   *
   * -T <0 = fixed-set | 1 = fixed-width> <br>
   * Typ of Linear Forward Selection (default = 0).
   * <p>
   *
   * -S <num> <br>
   * Size of lookup cache for evaluated subsets. Expressed as a multiple of
   * the number of attributes in the data set. (default = 1).
   * <p>
   *
   * -E <string> <br>
   * class name of subset evaluator to use for subset size determination
   * (default = null, same subset evaluator as for ranking and final forward
   * selection is used). Place any evaluator options LAST on the command line
   * following a "--". eg. -A weka.attributeSelection.ClassifierSubsetEval ... --
   * -M
   *
   * </pre>
   *
   * -F <num> <br>
   * Number of cross validation folds for subset size determination (default =
   * 5).
   * <p>
   *
   * -R <num> <br>
   * Seed for cross validation subset size determination. (default = 1)
   * <p>
   *
   * -Z <br>
   * verbose on/off.
   * <p>
   *
   * @param options
   *            the list of options as an array of strings
   * @exception Exception
   *                if an option is not supported
   *
   */
  public void setOptions(String[] options) throws Exception {
    String optionString;
    resetOptions();

    setPerformRanking(Utils.getFlag('I', options));

    optionString = Utils.getOption('K', options);

    if (optionString.length() != 0) {
      setNumUsedAttributes(Integer.parseInt(optionString));
    }

    optionString = Utils.getOption('T', options);

    if (optionString.length() != 0) {
      setType(new SelectedTag(Integer.parseInt(optionString), TAGS_TYPE));
    } else {
      setType(new SelectedTag(TYPE_FIXED_SET, TAGS_TYPE));
    }

    optionString = Utils.getOption('S', options);

    if (optionString.length() != 0) {
      setLookupCacheSize(Integer.parseInt(optionString));
    }

    optionString = Utils.getOption('E', options);

    if (optionString.length() == 0) {
      System.out.println(
                         "No subset size evaluator given, using evaluator that is used for final search.");
      m_setSizeEval = null;
    } else {
      setSubsetSizeEvaluator(ASEvaluation.forName(optionString,
                                                  Utils.partitionOptions(options)));
    }

    optionString = Utils.getOption('F', options);

    if (optionString.length() != 0) {
      setNumSubsetSizeCVFolds(Integer.parseInt(optionString));
    }

    optionString = Utils.getOption('R', options);

    if (optionString.length() != 0) {
      setSeed(Integer.parseInt(optionString));
    }

    m_verbose = Utils.getFlag('Z', options);
  }

  /**
   * Set the maximum size of the evaluated subset cache (hashtable). This is
   * expressed as a multiplier for the number of attributes in the data set.
   * (default = 1).
   *
   * @param size
   *            the maximum size of the hashtable
   */
  public void setLookupCacheSize(int size) {
    if (size >= 0) {
      m_cacheSize = size;
    }
  }

  /**
   * Return the maximum size of the evaluated subset cache (expressed as a
   * multiplier for the number of attributes in a data set.
   *
   * @return the maximum size of the hashtable.
   */
  public int getLookupCacheSize() {
    return m_cacheSize;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String lookupCacheSizeTipText() {
    return "Set the maximum size of the lookup cache of evaluated subsets. This is " +
      "expressed as a multiplier of the number of attributes in the data set. " +
      "(default = 1).";
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String performRankingTipText() {
    return "Perform initial ranking to select top-ranked attributes.";
  }

  /**
   * Perform initial ranking to select top-ranked attributes.
   *
   * @param b
   *            true if initial ranking should be performed
   */
  public void setPerformRanking(boolean b) {
    m_performRanking = b;
  }

  /**
   * Get boolean if initial ranking should be performed to select the
   * top-ranked attributes
   *
   * @return true if initial ranking should be performed
   */
  public boolean getPerformRanking() {
    return m_performRanking;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String numUsedAttributesTipText() {
    return "Set the amount of top-ranked attributes that are taken into account by the search process.";
  }

  /**
   * Set the number of top-ranked attributes that taken into account by the
   * search process.
   *
   * @param k
   *            the number of attributes
   * @exception Exception
   *                if k is less than 2
   */
  public void setNumUsedAttributes(int k) throws Exception {
    if (k < 2) {
      throw new Exception("Value of -K must be >= 2.");
    }

    m_numUsedAttributes = k;
  }

  /**
   * Get the number of top-ranked attributes that taken into account by the
   * search process.
   *
   * @return the number of top-ranked attributes that taken into account
   */
  public int getNumUsedAttributes() {
    return m_numUsedAttributes;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String typeTipText() {
    return "Set the type of the search.";
  }

  /**
   * Set the type
   *
   * @param t
   *            the Linear Forward Selection type
   */
  public void setType(SelectedTag t) {
    if (t.getTags() == TAGS_TYPE) {
      m_linearSelectionType = t.getSelectedTag().getID();
    }
  }

  /**
   * Get the type
   *
   * @return the Linear Forward Selection type
   */
  public SelectedTag getType() {
    return new SelectedTag(m_linearSelectionType, TAGS_TYPE);
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String subsetSizeEvaluatorTipText() {
    return "Subset evaluator to use for subset size determination.";
  }

  /**
   * Set the subset evaluator to use for subset size determination.
   *
   * @param eval
   *            the subset evaluator to use for subset size determination.
   */
  public void setSubsetSizeEvaluator(ASEvaluation eval)
    throws Exception {
    if (!(eval instanceof SubsetEvaluator)) {
      throw new Exception(eval.getClass().getName() +
                          " is no subset evaluator.");
    }

    m_setSizeEval = eval;
  }

  /**
   * Get the subset evaluator used for subset size determination.
   *
   * @return the evaluator used for subset size determination.
   */
  public ASEvaluation getSubsetSizeEvaluator() {
    return m_setSizeEval;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String numSubsetSizeCVFoldsTipText() {
    return "Number of cross validation folds for subset size determination";
  }

  /**
   * Set the number of cross validation folds for subset size determination
   * (default = 5).
   *
   * @param f
   *            number of folds
   */
  public void setNumSubsetSizeCVFolds(int f) {
    m_numFolds = f;
  }

  /**
   * Get the number of cross validation folds for subset size determination
   * (default = 5).
   *
   * @return number of folds
   */
  public int getNumSubsetSizeCVFolds() {
    return m_numFolds;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String seedTipText() {
    return "Seed for cross validation subset size determination. (default = 1)";
  }

  /**
   * Seed for cross validation subset size determination. (default = 1)
   *
   * @param s
   *            seed
   */
  public void setSeed(int s) {
    m_seed = s;
  }

  /**
   * Seed for cross validation subset size determination. (default = 1)
   *
   * @return seed
   */
  public int getSeed() {
    return m_seed;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String verboseTipText() {
    return "Turn on verbose output for monitoring the search's progress.";
  }

  /**
   * Set whether verbose output should be generated.
   *
   * @param b
   *            true if output is to be verbose.
   */
  public void setVerbose(boolean b) {
    m_verbose = b;
  }

  /**
   * Get whether output is to be verbose
   *
   * @return true if output will be verbose
   */
  public boolean getVerbose() {
    return m_verbose;
  }

  /**
   * Gets the current settings of LinearForwardSelection.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions() {
    String[] evaluatorOptions = new String[0];

    if ((m_setSizeEval != null) && (m_setSizeEval instanceof OptionHandler)) {
      evaluatorOptions = ((OptionHandler) m_setSizeEval).getOptions();
    }

    String[] options = new String[15 + evaluatorOptions.length];
    int current = 0;

    if (m_performRanking) {
      options[current++] = "-I";
    }

    options[current++] = "-K";
    options[current++] = "" + m_numUsedAttributes;
    options[current++] = "-T";
    options[current++] = "" + m_linearSelectionType;

    options[current++] = "-F";
    options[current++] = "" + m_numFolds;
    options[current++] = "-S";
    options[current++] = "" + m_seed;

    options[current++] = "-Z";
    options[current++] = "" + m_verbose;

    if (m_setSizeEval != null) {
      options[current++] = "-E";
      options[current++] = m_setSizeEval.getClass().getName();
    }

    options[current++] = "--";
    System.arraycopy(evaluatorOptions, 0, options, current,
                     evaluatorOptions.length);
    current += evaluatorOptions.length;

    while (current < options.length) {
      options[current++] = "";
    }

    return options;
  }

  /**
   * returns a description of the search as a String
   *
   * @return a description of the search
   */
  public String toString() {
    StringBuffer LFSString = new StringBuffer();

    LFSString.append("\tSubset Size Forward Selection.\n");

    LFSString.append("\tLinear Forward Selection Type: ");

    if (m_linearSelectionType == TYPE_FIXED_SET) {
      LFSString.append("fixed-set\n");
    } else {
      LFSString.append("fixed-width\n");
    }

    LFSString.append("\tNumber of top-ranked attributes that are used: " +
                     m_numUsedAttributes + "\n");

    LFSString.append(
                     "\tNumber of cross validation folds for subset size determination: " +
                     m_numFolds + "\n");
    LFSString.append("\tSeed for cross validation subset size determination: " +
                     m_seed + "\n");

    LFSString.append("\tTotal number of subsets evaluated: " + m_totalEvals +
                     "\n");
    LFSString.append("\tMerit of best subset found: " +
                     Utils.doubleToString(Math.abs(m_bestMerit), 8, 3) + "\n");

    return LFSString.toString();
  }

  /**
   * Searches the attribute subset space by subset size forward selection
   *
   * @param ASEval
   *            the attribute evaluator to guide the search
   * @param data
   *            the training instances.
   * @return an array (not necessarily ordered) of selected attribute indexes
   * @exception Exception
   *                if the search can't be completed
   */
  public int[] search(ASEvaluation ASEval, Instances data)
    throws Exception {
    m_totalEvals = 0;

    if (!(ASEval instanceof SubsetEvaluator)) {
      throw new Exception(ASEval.getClass().getName() + " is not a " +
                          "Subset evaluator!");
    }

    if (m_setSizeEval == null) {
      m_setSizeEval = ASEval;
    }

    m_numAttribs = data.numAttributes();

    if (m_numUsedAttributes > m_numAttribs) {
      System.out.println(
                         "Decreasing number of top-ranked attributes to total number of attributes: " +
                         data.numAttributes());
      m_numUsedAttributes = m_numAttribs;
    }

    Instances[] trainData = new Instances[m_numFolds];
    Instances[] testData = new Instances[m_numFolds];
    LFSMethods[] searchResults = new LFSMethods[m_numFolds];

    Random random = new Random(m_seed);
    Instances dataCopy = new Instances(data);
    dataCopy.randomize(random);

    if (dataCopy.classAttribute().isNominal()) {
      dataCopy.stratify(m_numFolds);
    }

    for (int f = 0; f < m_numFolds; f++) {
      trainData[f] = dataCopy.trainCV(m_numFolds, f, random);
      testData[f] = dataCopy.testCV(m_numFolds, f);
    }

    LFSMethods LSF = new LFSMethods();

    int[] ranking;

    if (m_performRanking) {
      ASEval.buildEvaluator(data);
      ranking = LSF.rankAttributes(data, (SubsetEvaluator) ASEval, m_verbose);
    } else {
      ranking = new int[m_numAttribs];

      for (int i = 0; i < ranking.length; i++) {
        ranking[i] = i;
      }
    }

    int maxSubsetSize = 0;

    for (int f = 0; f < m_numFolds; f++) {
      if (m_verbose) {
        System.out.println("perform search on internal fold: " + (f + 1) + "/" +
                           m_numFolds);
      }

      m_setSizeEval.buildEvaluator(trainData[f]);
      searchResults[f] = new LFSMethods();
      searchResults[f].forwardSearch(m_cacheSize, new BitSet(m_numAttribs),
                                     ranking, m_numUsedAttributes,
                                     m_linearSelectionType == TYPE_FIXED_WIDTH, 1, -1, trainData[f],
                                     (SubsetEvaluator)m_setSizeEval, m_verbose);

      maxSubsetSize = Math.max(maxSubsetSize,
                               searchResults[f].getBestGroup().cardinality());
    }

    if (m_verbose) {
      System.out.println(
                         "continue searches on internal folds to maxSubsetSize (" +
                         maxSubsetSize + ")");
    }

    for (int f = 0; f < m_numFolds; f++) {
      if (m_verbose) {
        System.out.print("perform search on internal fold: " + (f + 1) + "/" +
                         m_numFolds + " with starting set ");
        LFSMethods.printGroup(searchResults[f].getBestGroup(),
                              trainData[f].numAttributes());
      }

      if (searchResults[f].getBestGroup().cardinality() < maxSubsetSize) {
        m_setSizeEval.buildEvaluator(trainData[f]);
        searchResults[f].forwardSearch(m_cacheSize,
                                       searchResults[f].getBestGroup(), ranking, m_numUsedAttributes,
                                       m_linearSelectionType == TYPE_FIXED_WIDTH, 1, maxSubsetSize,
                                       trainData[f], (SubsetEvaluator)m_setSizeEval, m_verbose);
      }
    }

    double[][] testMerit = new double[m_numFolds][maxSubsetSize + 1];

    for (int f = 0; f < m_numFolds; f++) {
      for (int s = 1; s <= maxSubsetSize; s++) {
        if (HoldOutSubsetEvaluator.class.isInstance(m_setSizeEval)) {
          m_setSizeEval.buildEvaluator(trainData[f]);
          testMerit[f][s] = ((HoldOutSubsetEvaluator) m_setSizeEval).evaluateSubset(searchResults[f].getBestGroupOfSize(
                                                                                                                        s), testData[f]);
        } else {
          m_setSizeEval.buildEvaluator(testData[f]);
          testMerit[f][s] = ((SubsetEvaluator)m_setSizeEval).evaluateSubset(searchResults[f].getBestGroupOfSize(
                                                                                             s));
        }
      }
    }

    double[] avgTestMerit = new double[maxSubsetSize + 1];
    int finalSubsetSize = -1;

    for (int s = 1; s <= maxSubsetSize; s++) {
      for (int f = 0; f < m_numFolds; f++) {
        avgTestMerit[s] = ((avgTestMerit[s] * f) + testMerit[f][s]) / (double) (f +
                                                                                1);
      }

      if ((finalSubsetSize == -1) ||
          (avgTestMerit[s] > avgTestMerit[finalSubsetSize])) {
        finalSubsetSize = s;
      }

      if (m_verbose) {
        System.out.println("average merit for subset-size " + s + ": " +
                           avgTestMerit[s]);
      }
    }

    if (m_verbose) {
      System.out.println("performing final forward selection to subset-size: " +
                         finalSubsetSize);
    }

    ASEval.buildEvaluator(data);
    LSF.forwardSearch(m_cacheSize, new BitSet(m_numAttribs), ranking,
                      m_numUsedAttributes, m_linearSelectionType == TYPE_FIXED_WIDTH, 1,
                      finalSubsetSize, data, (SubsetEvaluator) ASEval, m_verbose);

    m_totalEvals = LSF.getNumEvalsTotal();
    m_bestMerit = LSF.getBestMerit();

    return attributeList(LSF.getBestGroup());
  }

  /**
   * Reset options to default values
   */
  protected void resetOptions() {
    m_performRanking = true;
    m_numUsedAttributes = 50;
    m_linearSelectionType = TYPE_FIXED_SET;
    m_setSizeEval = new ClassifierSubsetEval();
    m_numFolds = 5;
    m_seed = 1;
    m_totalEvals = 0;
    m_cacheSize = 1;
    m_verbose = false;
  }

  /**
   * converts a BitSet into a list of attribute indexes
   *
   * @param group
   *            the BitSet to convert
   * @return an array of attribute indexes
   */
  protected int[] attributeList(BitSet group) {
    int count = 0;

    // count how many were selected
    for (int i = 0; i < m_numAttribs; i++) {
      if (group.get(i)) {
        count++;
      }
    }

    int[] list = new int[count];
    count = 0;

    for (int i = 0; i < m_numAttribs; i++) {
      if (group.get(i)) {
        list[count++] = i;
      }
    }

    return list;
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }
}

