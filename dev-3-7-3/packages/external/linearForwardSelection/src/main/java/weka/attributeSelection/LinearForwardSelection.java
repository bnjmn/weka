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
 *    LinearForwardSelection.java
 *    Copyright (C) 2007 Martin Guetlein
 *
 */

package weka.attributeSelection;

import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Range;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.Utils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.Vector;


/**
  <!-- globalinfo-start -->
  * LinearForwardSelection:<br/>
  * <br/>
  * Extension of BestFirst. Takes a restricted number of k attributes into account. Fixed-set selects a fixed number k of attributes, whereas k is increased in each step when fixed-width is selected. The search uses either the initial ordering to select the top k attributes, or performs a ranking (with the same evalutator the search uses later on). The search direction can be forward, or floating forward selection (with opitional backward search steps).<br/>
  * <br/>
  * For more information see:<br/>
  * <br/>
  * Martin Guetlein, Eibe Frank, Mark Hall, Andreas Karwath: Large Scale Attribute Selection Using Wrappers. In: Proc IEEE Symposium on Computational Intelligence and Data Mining, 332-339, 2009.<br/>
  * <br/>
  * Martin Guetlein (2006). Large Scale Attribute Selection Using Wrappers. Freiburg, Germany.
  * <p/>
  <!-- globalinfo-end -->
 *
  <!-- options-start -->
  * Valid options are: <p/>
  * 
  * <pre> -P &lt;start set&gt;
  *  Specify a starting set of attributes.
  *  Eg. 1,3,5-7.</pre>
  * 
  * <pre> -D &lt;0 = forward selection | 1 = floating forward selection&gt;
  *  Forward selection method. (default = 0).</pre>
  * 
  * <pre> -N &lt;num&gt;
  *  Number of non-improving nodes to
  *  consider before terminating search.</pre>
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
  * <pre> -Z
  *  verbose on/off</pre>
  * 
  <!-- options-end -->
 *
 * @author Martin Guetlein (martin.guetlein@gmail.com)
 * @version $Revision$
 */
public class LinearForwardSelection 
  extends ASSearch 
  implements OptionHandler,
             StartSetHandler, 
             TechnicalInformationHandler {
  /** search directions */
  protected static final int SEARCH_METHOD_FORWARD = 0;
  protected static final int SEARCH_METHOD_FLOATING = 1;
  public static final Tag[] TAGS_SEARCH_METHOD = {
    new Tag(SEARCH_METHOD_FORWARD, "Forward selection"),
    new Tag(SEARCH_METHOD_FLOATING, "Floating forward selection"),
  };

  /** search directions */
  protected static final int TYPE_FIXED_SET = 0;
  protected static final int TYPE_FIXED_WIDTH = 1;
  public static final Tag[] TAGS_TYPE = {
    new Tag(TYPE_FIXED_SET, "Fixed-set"),
    new Tag(TYPE_FIXED_WIDTH, "Fixed-width"),
  };

  // member variables
  /** maximum number of stale nodes before terminating search */
  protected int m_maxStale;

  /** 0 == forward selection, 1 == floating forward search */
  protected int m_forwardSearchMethod;

  /** perform initial ranking to select top-ranked attributes */
  protected boolean m_performRanking;

  /**
   * number of top-ranked attributes that are taken into account for the
   * search
   */
  protected int m_numUsedAttributes;

  /** 0 == fixed-set, 1 == fixed-width */
  protected int m_linearSelectionType;

  /** holds an array of starting attributes */
  protected int[] m_starting;

  /** holds the start set for the search as a Range */
  protected Range m_startRange;

  /** does the data have a class */
  protected boolean m_hasClass;

  /** holds the class index */
  protected int m_classIndex;

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
  public LinearForwardSelection() {
    resetOptions();
  }

  /**
   * Returns a string describing this search method
   *
   * @return a description of the search method suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "LinearForwardSelection:\n\n" +
      "Extension of BestFirst. Takes a restricted number of k attributes " +
      "into account. Fixed-set selects a fixed number k of attributes, " +
      "whereas k is increased in each step when fixed-width is selected. " +
      "The search uses either the initial ordering to select the " +
      "top k attributes, or performs a ranking (with the same evalutator the " +
      "search uses later on). The search direction can be forward, " +
      "or floating forward selection (with opitional backward search steps).\n\n"
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
    result.setValue(Field.AUTHOR, "Martin Guetlein and Eibe Frank and Mark Hall and Andreas Karwath");
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
    Vector newVector = new Vector(8);

    newVector.addElement(new Option("\tSpecify a starting set of attributes." +
                                    "\n\tEg. 1,3,5-7.", "P", 1, "-P <start set>"));
    newVector.addElement(new Option(
                                    "\tForward selection method. (default = 0).", "D", 1,
                                    "-D <0 = forward selection | 1 = floating forward selection>"));
    newVector.addElement(new Option("\tNumber of non-improving nodes to" +
                                    "\n\tconsider before terminating search.", "N", 1, "-N <num>"));
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
    newVector.addElement(new Option("\tverbose on/off", "Z", 0, "-Z"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options.
   *
   * Valid options are:
   * <p>
   *
   * -P <start set> <br>
   * Specify a starting set of attributes. Eg 1,4,7-9.
   * <p>
   *
   * -D <0 = forward selection | 1 = floating forward selection> <br>
   * Forward selection method of the search. (default = 0).
   * <p>
   *
   * -N <num> <br>
   * Number of non improving nodes to consider before terminating search.
   * (default = 5).
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

    optionString = Utils.getOption('P', options);

    if (optionString.length() != 0) {
      setStartSet(optionString);
    }

    optionString = Utils.getOption('D', options);

    if (optionString.length() != 0) {
      setForwardSelectionMethod(new SelectedTag(Integer.parseInt(optionString),
                                                TAGS_SEARCH_METHOD));
    } else {
      setForwardSelectionMethod(new SelectedTag(SEARCH_METHOD_FORWARD,
                                                TAGS_SEARCH_METHOD));
    }

    optionString = Utils.getOption('N', options);

    if (optionString.length() != 0) {
      setSearchTermination(Integer.parseInt(optionString));
    }

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
  public String startSetTipText() {
    return "Set the start point for the search. This is specified as a comma " +
      "seperated list off attribute indexes starting at 1. It can include " +
      "ranges. Eg. 1,2,5-9,17.";
  }

  /**
   * Sets a starting set of attributes for the search. It is the search
   * method's responsibility to report this start set (if any) in its
   * toString() method.
   *
   * @param startSet
   *            a string containing a list of attributes (and or ranges), eg.
   *            1,2,6,10-15.
   * @exception Exception
   *                if start set can't be set.
   */
  public void setStartSet(String startSet) throws Exception {
    m_startRange.setRanges(startSet);
  }

  /**
   * Returns a list of attributes (and or attribute ranges) as a String
   *
   * @return a list of attributes (and or attribute ranges)
   */
  public String getStartSet() {
    return m_startRange.getRanges();
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String searchTerminationTipText() {
    return "Set the amount of backtracking. Specify the number of ";
  }

  /**
   * Set the numnber of non-improving nodes to consider before terminating
   * search.
   *
   * @param t
   *            the number of non-improving nodes
   * @exception Exception
   *                if t is less than 1
   */
  public void setSearchTermination(int t) throws Exception {
    if (t < 1) {
      throw new Exception("Value of -N must be > 0.");
    }

    m_maxStale = t;
  }

  /**
   * Get the termination criterion (number of non-improving nodes).
   *
   * @return the number of non-improving nodes
   */
  public int getSearchTermination() {
    return m_maxStale;
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
  public String forwardSelectionMethodTipText() {
    return "Set the direction of the search.";
  }

  /**
   * Set the search direction
   *
   * @param d
   *            the direction of the search
   */
  public void setForwardSelectionMethod(SelectedTag d) {
    if (d.getTags() == TAGS_SEARCH_METHOD) {
      m_forwardSearchMethod = d.getSelectedTag().getID();
    }
  }

  /**
   * Get the search direction
   *
   * @return the direction of the search
   */
  public SelectedTag getForwardSelectionMethod() {
    return new SelectedTag(m_forwardSearchMethod, TAGS_SEARCH_METHOD);
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
    String[] options = new String[13];
    int current = 0;

    if (!(getStartSet().equals(""))) {
      options[current++] = "-P";
      options[current++] = "" + startSetToString();
    }

    options[current++] = "-D";
    options[current++] = "" + m_forwardSearchMethod;
    options[current++] = "-N";
    options[current++] = "" + m_maxStale;

    if (m_performRanking) {
      options[current++] = "-I";
    }

    options[current++] = "-K";
    options[current++] = "" + m_numUsedAttributes;
    options[current++] = "-T";
    options[current++] = "" + m_linearSelectionType;

    if (m_verbose)
      options[current++] = "-Z";

    while (current < options.length) {
      options[current++] = "";
    }

    return options;
  }

  /**
   * converts the array of starting attributes to a string. This is used by
   * getOptions to return the actual attributes specified as the starting set.
   * This is better than using m_startRanges.getRanges() as the same start set
   * can be specified in different ways from the command line---eg 1,2,3 ==
   * 1-3. This is to ensure that stuff that is stored in a database is
   * comparable.
   *
   * @return a comma seperated list of individual attribute numbers as a
   *         String
   */
  private String startSetToString() {
    StringBuffer FString = new StringBuffer();
    boolean didPrint;

    if (m_starting == null) {
      return getStartSet();
    }

    for (int i = 0; i < m_starting.length; i++) {
      didPrint = false;

      if ((m_hasClass == false) ||
          ((m_hasClass == true) && (i != m_classIndex))) {
        FString.append((m_starting[i] + 1));
        didPrint = true;
      }

      if (i == (m_starting.length - 1)) {
        FString.append("");
      } else {
        if (didPrint) {
          FString.append(",");
        }
      }
    }

    return FString.toString();
  }

  /**
   * returns a description of the search as a String
   *
   * @return a description of the search
   */
  public String toString() {
    StringBuffer LFSString = new StringBuffer();

    LFSString.append("\tLinear Forward Selection.\n\tStart set: ");

    if (m_starting == null) {
      LFSString.append("no attributes\n");
    } else {
      LFSString.append(startSetToString() + "\n");
    }

    LFSString.append("\tForward selection method: ");

    if (m_forwardSearchMethod == SEARCH_METHOD_FORWARD) {
      LFSString.append("forward selection\n");
    } else {
      LFSString.append("floating forward selection\n");
    }

    LFSString.append("\tStale search after " + m_maxStale +
                     " node expansions\n");

    LFSString.append("\tLinear Forward Selection Type: ");

    if (m_linearSelectionType == TYPE_FIXED_SET) {
      LFSString.append("fixed-set\n");
    } else {
      LFSString.append("fixed-width\n");
    }

    LFSString.append("\tNumber of top-ranked attributes that are used: " +
                     m_numUsedAttributes + "\n");

    LFSString.append("\tTotal number of subsets evaluated: " + m_totalEvals +
                     "\n");
    LFSString.append("\tMerit of best subset found: " +
                     Utils.doubleToString(Math.abs(m_bestMerit), 8, 3) + "\n");

    return LFSString.toString();
  }

  /**
   * Searches the attribute subset space by linear forward selection
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

    if (ASEval instanceof UnsupervisedSubsetEvaluator) {
      m_hasClass = false;
    } else {
      m_hasClass = true;
      m_classIndex = data.classIndex();
    }

    ((ASEvaluation) ASEval).buildEvaluator(data);

    m_numAttribs = data.numAttributes();

    if (m_numUsedAttributes > m_numAttribs) {
      System.out.println(
                         "Decreasing number of top-ranked attributes to total number of attributes: " +
                         data.numAttributes());
      m_numUsedAttributes = m_numAttribs;
    }

    BitSet start_group = new BitSet(m_numAttribs);
    m_startRange.setUpper(m_numAttribs - 1);

    if (!(getStartSet().equals(""))) {
      m_starting = m_startRange.getSelection();
    }

    // If a starting subset has been supplied, then initialise the bitset
    if (m_starting != null) {
      for (int i = 0; i < m_starting.length; i++) {
        if ((m_starting[i]) != m_classIndex) {
          start_group.set(m_starting[i]);
        }
      }
    }

    LFSMethods LFS = new LFSMethods();

    int[] ranking;

    if (m_performRanking) {
      ranking = LFS.rankAttributes(data, (SubsetEvaluator) ASEval, m_verbose);
    } else {
      ranking = new int[m_numAttribs];

      for (int i = 0; i < ranking.length; i++) {
        ranking[i] = i;
      }
    }

    if (m_forwardSearchMethod == SEARCH_METHOD_FORWARD) {
      LFS.forwardSearch(m_cacheSize, start_group, ranking, m_numUsedAttributes,
                        m_linearSelectionType == TYPE_FIXED_WIDTH, m_maxStale, -1, data,
                        (SubsetEvaluator) ASEval, m_verbose);
    } else if (m_forwardSearchMethod == SEARCH_METHOD_FLOATING) {
      LFS.floatingForwardSearch(m_cacheSize, start_group, ranking,
                                m_numUsedAttributes, m_linearSelectionType == TYPE_FIXED_WIDTH,
                                m_maxStale, data, (SubsetEvaluator) ASEval, m_verbose);
    }

    m_totalEvals = LFS.getNumEvalsTotal();
    m_bestMerit = LFS.getBestMerit();

    return attributeList(LFS.getBestGroup());
  }

  /**
   * Reset options to default values
   */
  protected void resetOptions() {
    m_maxStale = 5;
    m_forwardSearchMethod = SEARCH_METHOD_FORWARD;
    m_performRanking = true;
    m_numUsedAttributes = 50;
    m_linearSelectionType = TYPE_FIXED_SET;
    m_starting = null;
    m_startRange = new Range();
    m_classIndex = -1;
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

