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
 *    StringToWordVector.java
 *    Copyright (C) 2002 University of Waikato
 *
 *    Updated 12/Dec/2001 by Gordon Paynter (gordon.paynter@ucr.edu)
 *                        Added parameters for delimiter set,
 *                        number of words to add, and input range.
 *    updated 27/Nov/2003 by Asrhaf M. Kibriya (amk14@cs.waikato.ac.nz)
 *                        Added options for TF/IDF transforms, length 
 *                        normalization and down casing the tokens. Also 
 *                        added another onlyAlphabeticStringTokenizer and
 *                        support for using a list of stopwords.
 */


package weka.filters.unsupervised.attribute;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Range;
import weka.core.SelectedTag;
import weka.core.SparseInstance;
import weka.core.Tag;
import weka.core.Utils;
import weka.core.stemmers.NullStemmer;
import weka.core.stemmers.Stemmer;
import weka.filters.Filter;
import weka.filters.UnsupervisedFilter;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;


/** 
 <!-- globalinfo-start -->
 * Converts String attributes into a set of attributes representing word occurrence information from the text contained in the strings. The set of words (attributes) is determined by the first batch filtered (typically training data).
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -C
 *  Output word counts rather than boolean word presence.
 * </pre>
 * 
 * <pre> -D &lt;delimiter set&gt;
 *  String containing the set of delimiter characters
 *  (default: " \n\t.,:'\"()?!")</pre>
 * 
 * <pre> -R &lt;index1,index2-index4,...&gt;
 *  Specify list of string attributes to convert to words (as weka Range).
 *  (default: select all string attributes)</pre>
 * 
 * <pre> -P &lt;attribute name prefix&gt;
 *  Specify a prefix for the created attribute names.
 *  (default: "")</pre>
 * 
 * <pre> -W &lt;number of words to keep&gt;
 *  Specify approximate number of word fields to create.
 *  Surplus words will be discarded..
 *  (default: 1000)</pre>
 * 
 * <pre> -T
 *  Transform the word frequencies into log(1+fij)
 *  where fij is the frequency of word i in jth document(instance).
 * </pre>
 * 
 * <pre> -I
 *  Transform each word frequency into:
 *  fij*log(num of Documents/num of  documents containing word i)
 *    where fij if frequency of word i in  jth document(instance)</pre>
 * 
 * <pre> -N
 *  Whether to 0=not normalize/1=normalize all data/2=normalize test data only
 *  to average length of training documents (default 0=don't normalize).</pre>
 * 
 * <pre> -A
 *  Only form tokens from contiguous alphabetic sequences
 *  (The delimiter string is ignored if this is set).</pre>
 * 
 * <pre> -L
 *  Convert all tokens to lowercase before adding to the dictionary.</pre>
 * 
 * <pre> -S
 *  Ignore words that are in the stoplist.</pre>
 * 
 * <pre> -stemmer &lt;spec&gt;
 *  The stemmering algorihtm (classname plus parameters) to use.</pre>
 * 
 * <pre> -M &lt;int&gt;
 *  The minimum term frequency (default = 1).</pre>
 * 
 * <pre> -O
 *  If this is set, the maximum number of words and the 
 *  minimum term frequency is not enforced on a per-class 
 *  basis but based on the documents in all the classes 
 *  (even if a class attribute is set).</pre>
 * 
 <!-- options-end -->
 *
 * @author Len Trigg (len@reeltwo.com)
 * @author Stuart Inglis (stuart@reeltwo.com)
 * @version $Revision: 1.12 $ 
 */
public class StringToWordVector 
  extends Filter
  implements UnsupervisedFilter, OptionHandler {

  /** for serialization */
  static final long serialVersionUID = 8249106275278565424L;
  
  /** Delimiters used in tokenization */
  private String delimiters = " \n\t.,:'\"()?!";

  /** Range of columns to convert to word vectors */
  protected Range m_SelectedRange = null;

  /** Contains a mapping of valid words to attribute indexes */
  private TreeMap m_Dictionary = new TreeMap();
  
  /** True if the first batch has been done */
  private boolean m_FirstBatchDone = false;

  /** True if output instances should contain word frequency rather than boolean 0 or 1. */
  private boolean m_OutputCounts = false;

  /** A String prefix for the attribute names */
  private String m_Prefix = "";
  
  /** Contains the number of documents (instances) a particular word appears in.
      The counts are stored with the same indexing as given by m_Dictionary.  */
  private int [] docsCounts;
  
  /** Contains the number of documents (instances) in the input format from 
      which the dictionary is created. It is used in IDF transform. */
  private int numInstances = -1;

  /**
   * Contains the average length of documents (among the first batch of 
   * instances aka training data). This is used in length normalization of 
   * documents which will be normalized to average document length.
   */
  private double avgDocLength = -1;
  
  /**
   * The default number of words (per class if there is a class attribute
   * assigned) to attempt to keep.
   */
  private int m_WordsToKeep = 1000;

  /** True if word frequencies should be transformed into log(1+fi) 
      where fi is the frequency of word i
   */
  private boolean m_TFTransform;

  /** The normalization to apply. */
  protected int m_filterType = FILTER_NONE;
  
  /** normalization: No normalization */
  public static final int FILTER_NONE = 0;
  /** normalization: Normalize all data */
  public static final int FILTER_NORMALIZE_ALL = 1;
  /** normalization: Normalize test data only */
  public static final int FILTER_NORMALIZE_TEST_ONLY = 2;

  /** Specifies whether document's (instance's) word frequencies are
   * to be normalized.  The are normalized to average length of
   * documents specified as input format. */
  public static final Tag [] TAGS_FILTER = {
    new Tag(FILTER_NONE, "No normalization"),
    new Tag(FILTER_NORMALIZE_ALL, "Normalize all data"),
    new Tag(FILTER_NORMALIZE_TEST_ONLY, "Normalize test data only"),
  };

  /** True if word frequencies should be transformed into 
     fij*log(numOfDocs/numOfDocsWithWordi) */
  private boolean m_IDFTransform;
  
  /** True if tokens are to be formed only from alphabetic sequences of 
      characters. (The delimiters string property is ignored if this
      is true).
   */
  private boolean m_onlyAlphabeticTokens;  
  
  /** True if all tokens should be downcased */
  private boolean m_lowerCaseTokens;
  
  /** True if tokens that are on a stoplist are to be ignored. */
  private boolean m_useStoplist;  

  /** the stemming algorithm */
  private Stemmer m_Stemmer = new NullStemmer();

  /** the minimum (per-class) word frequency */
  private int m_minTermFreq = 1;
  
  /** whether to operate on a per-class basis */
  private boolean m_doNotOperateOnPerClassBasis = false;

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector();

    newVector.addElement(new Option(
				    "\tOutput word counts rather than boolean word presence.\n",
				    "C", 0, "-C"));
    newVector.addElement(new Option(
				    "\tString containing the set of delimiter characters\n"
				    + "\t(default: \" \\n\\t.,:'\\\"()?!\")",
				    "D", 1, "-D <delimiter set>"));
    newVector.addElement(new Option(
				    "\tSpecify list of string attributes to convert to words (as weka Range).\n"
				    + "\t(default: select all string attributes)",
				    "R", 1, "-R <index1,index2-index4,...>"));
    newVector.addElement(new Option(
				    "\tSpecify a prefix for the created attribute names.\n"
				    + "\t(default: \"\")",
				    "P", 1, "-P <attribute name prefix>"));
    newVector.addElement(new Option(
				    "\tSpecify approximate number of word fields to create.\n"
				    + "\tSurplus words will be discarded..\n"
				    + "\t(default: 1000)",
				    "W", 1, "-W <number of words to keep>"));
    newVector.addElement(new Option(
				    "\tTransform the word frequencies into log(1+fij)\n"+
                                    "\twhere fij is the frequency of word i in jth "+
                                    "document(instance).\n",
				    "T", 0, "-T"));
    newVector.addElement(new Option(
				    "\tTransform each word frequency into:\n"+
                                    "\tfij*log(num of Documents/num of "+
                                    " documents containing word i)\n"+
                                    "\t  where fij if frequency of word i in "+
                                    " jth document(instance)",
				    "I", 0, "-I"));
    newVector.addElement(new Option("\tWhether to 0=not normalize/1=normalize all data/2=normalize test data only\n" 
				    + "\tto average length of training documents "
				    + "(default 0=don\'t normalize).",
				    "N", 1, "-N"));
    newVector.addElement(new Option(
				    "\tOnly form tokens from contiguous "+
                                    "alphabetic sequences\n\t(The delimiter "+
                                    "string is ignored if this is set).",
				    "A", 0, "-A"));
    newVector.addElement(new Option(
				    "\tConvert all tokens to lowercase before "+
                                    "adding to the dictionary.",
				    "L", 0, "-L"));
    newVector.addElement(new Option(
				    "\tIgnore words that are in the stoplist.",
				    "S", 0, "-S"));
    newVector.addElement(new Option(
				    "\tThe stemmering algorihtm (classname plus parameters) to use.",
				    "stemmer", 1, "-stemmer <spec>"));
    newVector.addElement(new Option(
				    "\tThe minimum term frequency (default = 1).",
				    "M", 1, "-M <int>"));
    newVector.addElement(new Option(
				    "\tIf this is set, the maximum number of words and the \n"
				    + "\tminimum term frequency is not enforced on a per-class \n"
				    + "\tbasis but based on the documents in all the classes \n"
				    + "\t(even if a class attribute is set).",
				    "O", 0, "-O"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options. <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -C
   *  Output word counts rather than boolean word presence.
   * </pre>
   * 
   * <pre> -D &lt;delimiter set&gt;
   *  String containing the set of delimiter characters
   *  (default: " \n\t.,:'\"()?!")</pre>
   * 
   * <pre> -R &lt;index1,index2-index4,...&gt;
   *  Specify list of string attributes to convert to words (as weka Range).
   *  (default: select all string attributes)</pre>
   * 
   * <pre> -P &lt;attribute name prefix&gt;
   *  Specify a prefix for the created attribute names.
   *  (default: "")</pre>
   * 
   * <pre> -W &lt;number of words to keep&gt;
   *  Specify approximate number of word fields to create.
   *  Surplus words will be discarded..
   *  (default: 1000)</pre>
   * 
   * <pre> -T
   *  Transform the word frequencies into log(1+fij)
   *  where fij is the frequency of word i in jth document(instance).
   * </pre>
   * 
   * <pre> -I
   *  Transform each word frequency into:
   *  fij*log(num of Documents/num of  documents containing word i)
   *    where fij if frequency of word i in  jth document(instance)</pre>
   * 
   * <pre> -N
   *  Whether to 0=not normalize/1=normalize all data/2=normalize test data only
   *  to average length of training documents (default 0=don't normalize).</pre>
   * 
   * <pre> -A
   *  Only form tokens from contiguous alphabetic sequences
   *  (The delimiter string is ignored if this is set).</pre>
   * 
   * <pre> -L
   *  Convert all tokens to lowercase before adding to the dictionary.</pre>
   * 
   * <pre> -S
   *  Ignore words that are in the stoplist.</pre>
   * 
   * <pre> -stemmer &lt;spec&gt;
   *  The stemmering algorihtm (classname plus parameters) to use.</pre>
   * 
   * <pre> -M &lt;int&gt;
   *  The minimum term frequency (default = 1).</pre>
   * 
   * <pre> -O
   *  If this is set, the maximum number of words and the 
   *  minimum term frequency is not enforced on a per-class 
   *  basis but based on the documents in all the classes 
   *  (even if a class attribute is set).</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String value = Utils.getOption('D', options);
    if (value.length() != 0) {
      setDelimiters(value);
    }

    value = Utils.getOption('R', options);
    if (value.length() != 0) {
      setSelectedRange(value);
    }

    value = Utils.getOption('P', options);
    if (value.length() != 0) {
      setAttributeNamePrefix(value);
    }

    value = Utils.getOption('W', options);
    if (value.length() != 0) {
      setWordsToKeep(Integer.valueOf(value).intValue());
    }

    value = Utils.getOption('M', options);
    if (value.length() != 0) {
      setMinTermFreq(Integer.valueOf(value).intValue());
    }
    
    setOutputWordCounts(Utils.getFlag('C', options));

    setTFTransform(Utils.getFlag('T',  options));

    setIDFTransform(Utils.getFlag('I',  options));
    
    setDoNotOperateOnPerClassBasis(Utils.getFlag('O', options));

    String nString = Utils.getOption('N', options);
    if (nString.length() != 0) {
      setNormalizeDocLength(new SelectedTag(Integer.parseInt(nString), TAGS_FILTER));
    } else {
      setNormalizeDocLength(new SelectedTag(FILTER_NONE, TAGS_FILTER));
    }
    
    setLowerCaseTokens(Utils.getFlag('L', options));
    
    setOnlyAlphabeticTokens(Utils.getFlag('A', options));
    
    setUseStoplist(Utils.getFlag('S', options));
    
    String stemmerString = Utils.getOption("stemmer", options);
    if (stemmerString.length() == 0) {
      setStemmer(null);
    }
    else {
      String[] stemmerSpec = Utils.splitOptions(stemmerString);
      if (stemmerSpec.length == 0)
        throw new Exception("Invalid stemmer specification string");
      String stemmerName = stemmerSpec[0];
      stemmerSpec[0] = "";
      Stemmer stemmer = (Stemmer) Class.forName(stemmerName).newInstance();
      if (stemmer instanceof OptionHandler)
        ((OptionHandler) stemmer).setOptions(stemmerSpec);
      setStemmer(stemmer);
    }
  }

  /**
   * Gets the current settings of the filter.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [22];
    int current = 0;

    options[current++] = "-D"; 
    options[current++] = getDelimiters();

    if (getSelectedRange() != null) {
      options[current++] = "-R"; 
      m_SelectedRange.setUpper(getInputFormat().numAttributes() - 1);
      options[current++] = getSelectedRange().getRanges();
    }

    if (!"".equals(getAttributeNamePrefix())) {
      options[current++] = "-P"; 
      options[current++] = getAttributeNamePrefix();
    }

    options[current++] = "-W"; 
    options[current++] = String.valueOf(getWordsToKeep());

    if (getOutputWordCounts()) {
      options[current++] = "-C";
    }

    if(getTFTransform())
        options[current++] = "-T";
    
    if(getIDFTransform())
        options[current++] = "-I";
    
    options[current++] = "-N"; options[current++] = "" + m_filterType;
    
    if(this.getLowerCaseTokens())
        options[current++] = "-L";
    
    if(this.getOnlyAlphabeticTokens())
        options[current++] = "-A";
    
    if(this.getUseStoplist())
        options[current++] = "-S";
    
    if (getStemmer() != null) {
      options[current++] = "-stemmer";
      String spec = getStemmer().getClass().getName();
      if (getStemmer() instanceof OptionHandler)
        spec += " " + Utils.joinOptions(
                          ((OptionHandler) getStemmer()).getOptions());
      options[current++] = spec.trim();
    }

    options[current++] = "-M"; 
    options[current++] = String.valueOf(getMinTermFreq());
    
    if(this.getDoNotOperateOnPerClassBasis())
      options[current++] = "-O";
    
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Default constructor. Targets 1000 words in the output.
   */
  public StringToWordVector() {
  }

  /**
   * Constructor that allows specification of the target number of words
   * in the output.
   *
   * @param wordsToKeep the number of words in the output vector (per class
   * if assigned).
   */
  public StringToWordVector(int wordsToKeep) {
    m_WordsToKeep = wordsToKeep;
  }
  
  /** 
   * Used to store word counts for dictionary selection based on 
   * a threshold.
   */
  private class Count 
    implements Serializable {

    /** for serialization */
    static final long serialVersionUID = 2157223818584474321L;
    
    /** the counts */
    public int count, docCount;
    
    /**
     * the constructor
     * 
     * @param c the count
     */
    public Count(int c) { 
      count = c; 
    }
  }

  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input 
   * instance structure (any instances contained in the object are 
   * ignored - only the structure is required).
   * @return true if the outputFormat may be collected immediately
   * @throws Exception if the input format can't be set 
   * successfully
   */
  public boolean setInputFormat(Instances instanceInfo) 
    throws Exception {
    super.setInputFormat(instanceInfo);
    m_FirstBatchDone = false;
    avgDocLength = -1;
    numInstances = -1;
    return false;
  }

  /**
   * Input an instance for filtering. Filter requires all
   * training instances be read before producing output.
   *
   * @param instance the input instance.
   * @return true if the filtered instance may now be
   * collected with output().
   * @throws IllegalStateException if no input structure has been defined.
   */
  public boolean input(Instance instance) throws Exception {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }
    if (m_FirstBatchDone) {
      FastVector fv = new FastVector();
      int firstCopy = convertInstancewoDocNorm(instance, fv);
      Instance inst = (Instance)fv.elementAt(0);
      if (m_filterType != FILTER_NONE) {
	normalizeInstance(inst, firstCopy);
      }
      push(inst);
      return true;
    } else {
      bufferInput(instance);
      return false;
    }
  }

  /**
   * Signify that this batch of input to the filter is finished. 
   * If the filter requires all instances prior to filtering,
   * output() may now be called to retrieve the filtered instances.
   *
   * @return true if there are instances pending output.
   * @throws IllegalStateException if no input structure has been defined.
   */
  public boolean batchFinished() throws Exception {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }

    // We only need to do something in this method
    // if the first batch hasn't been processed. Otherwise
    // input() has already done all the work.
    if (!m_FirstBatchDone) {

      // Determine the dictionary from the first batch (training data)
      determineDictionary();

      // Convert all instances w/o normalization
      FastVector fv = new FastVector();
      int firstCopy=0;
      for(int i=0; i < numInstances; i++) {
	firstCopy = convertInstancewoDocNorm(getInputFormat().instance(i), fv);
      }
      
      // Need to compute average document length if necessary
      if (m_filterType != FILTER_NONE) {
	avgDocLength = 0;
	for(int i=0; i<fv.size(); i++) {
	  Instance inst = (Instance) fv.elementAt(i);
	  double docLength = 0;
	  for(int j=0; j<inst.numValues(); j++) {
	    if(inst.index(j)>=firstCopy) {
	      docLength += inst.valueSparse(j) * inst.valueSparse(j);
	    }
	  }        
	  avgDocLength += Math.sqrt(docLength);
	}
	avgDocLength /= numInstances;
      }

      // Perform normalization if necessary.
      if (m_filterType == FILTER_NORMALIZE_ALL) {
	for(int i=0; i<fv.size(); i++) {
	  normalizeInstance((Instance) fv.elementAt(i), firstCopy);
	}
      }

      // Push all instances into the output queue
      for(int i=0; i<fv.size(); i++) {
	push((Instance) fv.elementAt(i));
      }
    }

    // Flush the input
    flushInput();

    m_NewBatch = true;
    m_FirstBatchDone = true;
    return (numPendingOutput() != 0);
  }

  /**
   * Returns a string describing this filter
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */  
  public String globalInfo() {
    return "Converts String attributes into a set of attributes representing "+
           "word occurrence information from the text contained in the "+
           "strings. The set of words (attributes) is determined by the first "+
           "batch filtered (typically training data).";
  }  
  
  /**
   * Gets whether output instances contain 0 or 1 indicating word
   * presence, or word counts.
   *
   * @return true if word counts should be output.
   */
  public boolean getOutputWordCounts() {
    return m_OutputCounts;
  }

  /**
   * Sets whether output instances contain 0 or 1 indicating word
   * presence, or word counts.
   *
   * @param outputWordCounts true if word counts should be output.
   */
  public void setOutputWordCounts(boolean outputWordCounts) {
    m_OutputCounts = outputWordCounts;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String outputWordCountsTipText() {
      return "Output word counts rather than boolean 0 or 1"+
             "(indicating presence or absence of a word).";
  }

  /**
   * Get the value of delimiters.
   *
   * @return Value of delimiters.
   */
  public String getDelimiters() {
    return delimiters;
  }
    
  /**
   * Set the value of delimiters.
   *
   * @param newDelimiters Value to assign to delimiters.
   */
  public void setDelimiters(String newDelimiters) {
    delimiters = newDelimiters;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String delimitersTipText() {
      return "Set of delimiter characters to use in tokenizing "+
             "(default: \" \\n\\t.,:'\\\"()?!\"). "+
             "This option is ignored if onlyAlphabeticTokens option is set to"+
             " true.";
  }

  /**
   * Get the value of m_SelectedRange.
   *
   * @return Value of m_SelectedRange.
   */
  public Range getSelectedRange() {
    return m_SelectedRange;
  }
    
  /**
   * Set the value of m_SelectedRange.
   *
   * @param newSelectedRange Value to assign to m_SelectedRange.
   */
  public void setSelectedRange(String newSelectedRange) {
    m_SelectedRange = new Range(newSelectedRange);
  }

  /**
   * Get the attribute name prefix.
   *
   * @return The current attribute name prefix.
   */
  public String getAttributeNamePrefix() {
    return m_Prefix;
  }
    
  /**
   * Set the attribute name prefix.
   *
   * @param newPrefix String to use as the attribute name prefix.
   */
  public void setAttributeNamePrefix(String newPrefix) {
    m_Prefix = newPrefix;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String attributeNamePrefixTipText() {
      return "Prefix for the created attribute names. "+
             "(default: \"\")";
  }

  /**
   * Gets the number of words (per class if there is a class attribute
   * assigned) to attempt to keep.
   *
   * @return the target number of words in the output vector (per class if
   * assigned).
   */
  public int getWordsToKeep() {
    return m_WordsToKeep;
  }
  
  /**
   * Sets the number of words (per class if there is a class attribute
   * assigned) to attempt to keep.
   *
   * @param newWordsToKeep the target number of words in the output 
   * vector (per class if assigned).
   */
  public void setWordsToKeep(int newWordsToKeep) {
    m_WordsToKeep = newWordsToKeep;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String wordsToKeepTipText() {
      return "The number of words (per class if there is a class attribute "+
             "assigned) to attempt to keep.";
  }

  /** Gets whether if the word frequencies should be transformed into
   *  log(1+fij) where fij is the frequency of word i in document(instance) j.
   *
   * @return true if word frequencies are to be transformed.
   */
  public boolean getTFTransform() {
      return this.m_TFTransform;
  }
  
  /** Sets whether if the word frequencies should be transformed into
   *  log(1+fij) where fij is the frequency of word i in document(instance) j.
   *
   * @param TFTransform true if word frequencies are to be transformed.
   */
  public void setTFTransform(boolean TFTransform) {
      this.m_TFTransform = TFTransform;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String TFTransformTipText() {
      return "Sets whether if the word frequencies should be transformed into:\n "+
             "   log(1+fij) \n"+
             "       where fij is the frequency of word i in document (instance) j.";
  }
  
  /** Sets whether if the word frequencies in a document should be transformed
   * into: <br>
   * fij*log(num of Docs/num of Docs with word i) <br>
   *      where fij is the frequency of word i in document(instance) j.
   *
   * @return true if the word frequencies are to be transformed.
   */
  public boolean getIDFTransform() {
      return this.m_IDFTransform;
  }
  
  /** Sets whether if the word frequencies in a document should be transformed
   * into: <br>
   * fij*log(num of Docs/num of Docs with word i) <br>
   *      where fij is the frequency of word i in document(instance) j.
   *
   * @param IDFTransform true if the word frequecies are to be transformed
   */
  public void setIDFTransform(boolean IDFTransform) {
      this.m_IDFTransform = IDFTransform;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String IDFTransformTipText() {
      return "Sets whether if the word frequencies in a document should be "+
             "transformed into: \n"+
             "   fij*log(num of Docs/num of Docs with word i) \n"+
             "      where fij is the frequency of word i in document (instance) j.";
  }

  
  /** Gets whether if the word frequencies for a document (instance) should
   *  be normalized or not.
   *
   * @return true if word frequencies are to be normalized.
   */
  public SelectedTag getNormalizeDocLength() {

    return new SelectedTag(m_filterType, TAGS_FILTER);
  }
  
  /** Sets whether if the word frequencies for a document (instance) should
   *  be normalized or not.
   *
   * @param newType the new type.
   */
  public void setNormalizeDocLength(SelectedTag newType) {
    
    if (newType.getTags() == TAGS_FILTER) {
      m_filterType = newType.getSelectedTag().getID();
    }
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String normalizeDocLengthTipText() {
      return "Sets whether if the word frequencies for a document (instance) "+
             "should be normalized or not.";
  }
  
  /** Gets whether if the tokens are to be formed only from contiguous 
   *  alphabetic sequences. The delimiter string is ignored if this is true.
   *
   * @return true if tokens are to be formed from contiguous alphabetic 
   * characters.
   */
  public boolean getOnlyAlphabeticTokens() {
      return m_onlyAlphabeticTokens;
  }  
  
  /** Sets whether if tokens are to be formed only from contiguous alphabetic
   * character sequences. The delimiter string is ignored if this option is 
   * set to true.
   *
   * @param tokenizeOnlyAlphabeticSequences should be set to true if only alphabetic 
   * tokens should be formed.
   */
  public void setOnlyAlphabeticTokens(boolean tokenizeOnlyAlphabeticSequences) {
      m_onlyAlphabeticTokens = tokenizeOnlyAlphabeticSequences;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String onlyAlphabeticTokensTipText() {
      return "Sets whether if the word tokens are to be formed only from "+
             "contiguous alphabetic sequences (The delimiter string is "+
             "ignored if this option is set to true).";
  }
  
  /** Gets whether if the tokens are to be downcased or not.
   *
   * @return true if the tokens are to be downcased.
   */
  public boolean getLowerCaseTokens() {
      return this.m_lowerCaseTokens;
  }
  
  /** Sets whether if the tokens are to be downcased or not. (Doesn't affect
   * non-alphabetic characters in tokens).
   *
   * @param downCaseTokens should be true if only lower case tokens are 
   * to be formed.
   */
  public void setLowerCaseTokens(boolean downCaseTokens) {
      this.m_lowerCaseTokens = downCaseTokens;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String doNotOperateOnPerClassBasisTipText() {
      return "If this is set, the maximum number of words and the "
	+ "minimum term frequency is not enforced on a per-class "
	+ "basis but based on the documents in all the classes "
	+  "(even if a class attribute is set).";
  }

  /**
   * Get the DoNotOperateOnPerClassBasis value.
   * @return the DoNotOperateOnPerClassBasis value.
   */
  public boolean getDoNotOperateOnPerClassBasis() {
    return m_doNotOperateOnPerClassBasis;
  }

  /**
   * Set the DoNotOperateOnPerClassBasis value.
   * @param newDoNotOperateOnPerClassBasis The new DoNotOperateOnPerClassBasis value.
   */
  public void setDoNotOperateOnPerClassBasis(boolean newDoNotOperateOnPerClassBasis) {
    this.m_doNotOperateOnPerClassBasis = newDoNotOperateOnPerClassBasis;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String minTermFreqTipText() {
      return "Sets the minimum term frequency. This is enforced "
	+ "on a per-class basis.";
  }

  /**
   * Get the MinTermFreq value.
   * @return the MinTermFreq value.
   */
  public int getMinTermFreq() {
    return m_minTermFreq;
  }

  /**
   * Set the MinTermFreq value.
   * @param newMinTermFreq The new MinTermFreq value.
   */
  public void setMinTermFreq(int newMinTermFreq) {
    this.m_minTermFreq = newMinTermFreq;
  }
  
  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String lowerCaseTokensTipText() {
      return "If set then all the word tokens are converted to lower case "+
             "before being added to the dictionary.";
  }

  /** Gets whether if the words on the stoplist are to be ignored (The stoplist
   *  is in weka.core.StopWords).
   *
   * @return true if the words on the stoplist are to be ignored.
   */
  public boolean getUseStoplist() {
      return m_useStoplist;
  }  
  
  /** Sets whether if the words that are on a stoplist are to be ignored (The
   * stop list is in weka.core.StopWords).
   *
   * @param useStoplist true if the tokens that are on a stoplist are to be 
   * ignored.
   */
  public void setUseStoplist(boolean useStoplist) {
      m_useStoplist = useStoplist;
  }  
  
  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String useStoplistTipText() {
      return "Ignores all the words that are on the stoplist, if set to true.";
  } 

  /**
   * the stemming algorithm to use, null means no stemming at all (i.e., the
   * NullStemmer is used)
   *
   * @param value     the configured stemming algorithm, or null
   * @see             NullStemmer
   */
  public void setStemmer(Stemmer value) {
    if (value != null)
      m_Stemmer = value;
    else
      m_Stemmer = new NullStemmer();
  }

  /**
   * Returns the current stemming algorithm, null if none is used.
   *
   * @return          the current stemming algorithm, null if none set
   */
  public Stemmer getStemmer() {
    return m_Stemmer;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String stemmerTipText() {
    return "The stemming algorithm to use on the words.";
  }
  
  /**
   * sorts an array
   * 
   * @param array the array to sort
   */
  private static void sortArray(int [] array) {
      
    int i, j, h, N = array.length - 1;
	
    for (h = 1; h <= N / 9; h = 3 * h + 1); 
	
    for (; h > 0; h /= 3) {
      for (i = h + 1; i <= N; i++) { 
        int v = array[i]; 
        j = i; 
        while (j > h && array[j - h] > v ) { 
          array[j] = array[j - h]; 
          j -= h; 
        } 
        array[j] = v; 
      } 
    }
  }

  /**
   * determines the selected range
   */
  private void determineSelectedRange() {
    
    Instances inputFormat = getInputFormat();
    
    // Calculate the default set of fields to convert
    if (m_SelectedRange == null) {
      StringBuffer fields = new StringBuffer();
      for (int j = 0; j < inputFormat.numAttributes(); j++) { 
	if (inputFormat.attribute(j).type() == Attribute.STRING)
	  fields.append((j + 1) + ",");
      }
      m_SelectedRange = new Range(fields.toString());
    }
    m_SelectedRange.setUpper(inputFormat.numAttributes() - 1);
    
    // Prevent the user from converting non-string fields
    StringBuffer fields = new StringBuffer();
    for (int j = 0; j < inputFormat.numAttributes(); j++) { 
      if (m_SelectedRange.isInRange(j) 
	  && inputFormat.attribute(j).type() == Attribute.STRING)
	fields.append((j + 1) + ",");
    }
    m_SelectedRange.setRanges(fields.toString());
    m_SelectedRange.setUpper(inputFormat.numAttributes() - 1);

    // System.err.println("Selected Range: " + getSelectedRange().getRanges()); 
  }
  
  /**
   * determines the dictionary
   */
  private void determineDictionary() {

    // Operate on a per-class basis if class attribute is set
    int classInd = getInputFormat().classIndex();
    int values = 1;
    if (!m_doNotOperateOnPerClassBasis && (classInd != -1)) {
      values = getInputFormat().attribute(classInd).numValues();
    }

    //TreeMap dictionaryArr [] = new TreeMap[values];
    TreeMap [] dictionaryArr = new TreeMap[values];
    for (int i = 0; i < values; i++) {
      dictionaryArr[i] = new TreeMap();
    }

    // Make sure we know which fields to convert
    determineSelectedRange();

    // Tokenize all training text into an orderedMap of "words".
    for (int i = 0; i < getInputFormat().numInstances(); i++) {
      Instance instance = getInputFormat().instance(i);
      int vInd = 0;
      if (!m_doNotOperateOnPerClassBasis && (classInd != -1)) {
	vInd = (int)instance.classValue();
      }

      // Iterate through all relevant string attributes of the current instance
      Hashtable h = new Hashtable();
      for (int j = 0; j < instance.numAttributes(); j++) { 
        if (m_SelectedRange.isInRange(j) && (instance.isMissing(j) == false)) {

	  // Get tokenizer
          Enumeration st;
          if(this.m_onlyAlphabeticTokens==false)
              st = new StringTokenizer(instance.stringValue(j),
                                                   delimiters);
          else
              st = new AlphabeticStringTokenizer(instance.stringValue(j));
          
	  // Iterate through tokens, perform stemming, and remove stopwords
	  // (if required)
          while (st.hasMoreElements()) {
            String word = ((String)st.nextElement()).intern();
            
            if(this.m_lowerCaseTokens==true)
                word = word.toLowerCase();
            
            word = m_Stemmer.stem(word);
            
            if(this.m_useStoplist==true)
                if(weka.core.Stopwords.isStopword(word))
                    continue;
            
            if(!(h.contains(word)))
                h.put(word, new Integer(0));

            Count count = (Count)dictionaryArr[vInd].get(word);
            if (count == null) {
              dictionaryArr[vInd].put(word, new Count(1));
            } else {
	      count.count ++;                
            }
          }          
        }
      }

      //updating the docCount for the words that have occurred in this
      //instance(document).
      Enumeration e = h.keys();
      while(e.hasMoreElements()) {
	String word = (String) e.nextElement();
	Count c = (Count)dictionaryArr[vInd].get(word);
	if(c!=null) {
	  c.docCount++;
	} else 
	  System.err.println("Warning: A word should definitely be in the "+
			     "dictionary.Please check the code");
      }
    }

    // Figure out the minimum required word frequency
    int totalsize = 0;
    int prune[] = new int[values];
    for (int z = 0; z < values; z++) {
      totalsize += dictionaryArr[z].size();

      int array[] = new int[dictionaryArr[z].size()];
      int pos = 0;
      Iterator it = dictionaryArr[z].keySet().iterator();
      while (it.hasNext()) {
        String word = (String)it.next();
        Count count = (Count)dictionaryArr[z].get(word);
        array[pos] = count.count;
        pos++;
      }

      // sort the array
      sortArray(array);
      if (array.length < m_WordsToKeep) {
        // if there aren't enough words, set the threshold to
	// minFreq
        prune[z] = m_minTermFreq;
      } else {
        // otherwise set it to be at least minFreq
        prune[z] = Math.max(m_minTermFreq, 
			    array[array.length - m_WordsToKeep]);
      }
    }

    // Convert the dictionary into an attribute index
    // and create one attribute per word
    FastVector attributes = new FastVector(totalsize +
					   getInputFormat().numAttributes());

    // Add the non-converted attributes 
    int classIndex = -1;
    for (int i = 0; i < getInputFormat().numAttributes(); i++) {
      if (!m_SelectedRange.isInRange(i)) { 
        if (getInputFormat().classIndex() == i) {
          classIndex = attributes.size();
        }
	attributes.addElement(getInputFormat().attribute(i).copy());
      }     
    }
    
    // Add the word vector attributes (eliminating duplicates
    // that occur in multiple classes)
    TreeMap newDictionary = new TreeMap();
    int index = attributes.size();
    for(int z = 0; z < values; z++) {
      Iterator it = dictionaryArr[z].keySet().iterator();
      while (it.hasNext()) {
        String word = (String)it.next();
        Count count = (Count)dictionaryArr[z].get(word);
        if (count.count >= prune[z]) {
          if(newDictionary.get(word) == null) {
            newDictionary.put(word, new Integer(index++));
            attributes.addElement(new Attribute(m_Prefix + word));
          }
        }
      }
    }
    
    // Compute document frequencies
    docsCounts = new int[attributes.size()];
    Iterator it = newDictionary.keySet().iterator();
    while(it.hasNext()) {
      String word = (String) it.next();
      int idx = ((Integer)newDictionary.get(word)).intValue();
      int docsCount=0;
      for(int j=0; j<values; j++) {
	Count c = (Count) dictionaryArr[j].get(word);
	if(c!=null)
	  docsCount += c.docCount;
      }
      docsCounts[idx]=docsCount;
    }

    // Trim vector and set instance variables
    attributes.trimToSize();
    m_Dictionary = newDictionary;
    numInstances = getInputFormat().numInstances();
    
    // Set the filter's output format
    Instances outputFormat = new Instances(getInputFormat().relationName(), 
                                           attributes, 0);
    outputFormat.setClassIndex(classIndex);
    setOutputFormat(outputFormat);
  }

  /**
   * Converts the instance w/o normalization.
   * 
   * @oaram instance the instance to convert
   * @param v
   * @return the conerted instance
   */
  private int convertInstancewoDocNorm(Instance instance, FastVector v) {

    // Convert the instance into a sorted set of indexes
    TreeMap contained = new TreeMap();
    
    // Copy all non-converted attributes from input to output
    int firstCopy = 0;
    for (int i = 0; i < getInputFormat().numAttributes(); i++) {
      if (!m_SelectedRange.isInRange(i)) { 
	if (getInputFormat().attribute(i).type() != Attribute.STRING) {
	  // Add simple nominal and numeric attributes directly
	  if (instance.value(i) != 0.0) {
	    contained.put(new Integer(firstCopy), 
			  new Double(instance.value(i)));
	  } 
	} else {
	  if (instance.isMissing(i)) {
	    contained.put(new Integer(firstCopy),
			  new Double(Instance.missingValue()));
	  } else {
	    
	    // If this is a string attribute, we have to first add
	    // this value to the range of possible values, then add
	    // its new internal index.
	    if (outputFormatPeek().attribute(firstCopy).numValues() == 0) {
	      // Note that the first string value in a
	      // SparseInstance doesn't get printed.
	      outputFormatPeek().attribute(firstCopy)
		.addStringValue("Hack to defeat SparseInstance bug");
	    }
	    int newIndex = outputFormatPeek().attribute(firstCopy)
	      .addStringValue(instance.stringValue(i));
	    contained.put(new Integer(firstCopy), 
			  new Double(newIndex));
	  }
	}
	firstCopy++;
      }     
    }
    
    for (int j = 0; j < instance.numAttributes(); j++) { 
      //if ((getInputFormat().attribute(j).type() == Attribute.STRING) 
      if (m_SelectedRange.isInRange(j)
	  && (instance.isMissing(j) == false)) {          
        Enumeration st;
        
        if(this.m_onlyAlphabeticTokens==false)
	  st = new StringTokenizer(instance.stringValue(j),
				   delimiters);
        else
	  st = new AlphabeticStringTokenizer(instance.stringValue(j));
        
        while (st.hasMoreElements()) {
          String word = (String)st.nextElement(); 
          if(this.m_lowerCaseTokens==true)
	    word = word.toLowerCase();
          word = m_Stemmer.stem(word);
          Integer index = (Integer) m_Dictionary.get(word);
          if (index != null) {
            if (m_OutputCounts) { // Separate if here rather than two lines down to avoid hashtable lookup
              Double count = (Double)contained.get(index);
              if (count != null) {
                contained.put(index, new Double(count.doubleValue() + 1.0));
              } else {
                contained.put(index, new Double(1));
              }
            } else {
              contained.put(index, new Double(1));
            }                
          }
        }
      }
    }
    
    //Doing TFTransform
    if(m_TFTransform==true) {
      Iterator it = contained.keySet().iterator();
      for(int i=0; it.hasNext(); i++) {
	Integer index = (Integer)it.next();
	if( index.intValue() >= firstCopy ) { 
	  double val = ((Double)contained.get(index)).doubleValue();
	  val = Math.log(val+1);
	  contained.put(index, new Double(val));
	}
      }
    }
    
    //Doing IDFTransform
    if(m_IDFTransform==true) {
      Iterator it = contained.keySet().iterator();
      for(int i=0; it.hasNext(); i++) {
	Integer index = (Integer)it.next();
	if( index.intValue() >= firstCopy ) {
	  double val = ((Double)contained.get(index)).doubleValue();
	  val = val*Math.log( numInstances /
			      (double) docsCounts[index.intValue()] );
	  contained.put(index, new Double(val));
	}
      }        
    }
    
    // Convert the set to structures needed to create a sparse instance.
    double [] values = new double [contained.size()];
    int [] indices = new int [contained.size()];
    Iterator it = contained.keySet().iterator();
    for (int i = 0; it.hasNext(); i++) {
      Integer index = (Integer)it.next();
      Double value = (Double)contained.get(index);
      values[i] = value.doubleValue();
      indices[i] = index.intValue();
    }

    Instance inst = new SparseInstance(instance.weight(), values, indices, 
                                       outputFormatPeek().numAttributes());
    inst.setDataset(outputFormatPeek());

    v.addElement(inst);
    
    return firstCopy;    
  }
  
  /**
   * Normalizes given instance to average doc length (only the newly
   * constructed attributes).
   * 
   * @param inst	the instance to normalize
   * @param firstCopy
   * @throws Exception if avg. doc length not set
   */
  private void normalizeInstance(Instance inst, int firstCopy) 
    throws Exception {

    double docLength = 0;

    if (avgDocLength < 0) {
      throw new Exception("Average document length not set.");
    }

    // Compute length of document vector
    for(int j=0; j<inst.numValues(); j++) {
      if(inst.index(j)>=firstCopy) {
	docLength += inst.valueSparse(j) * inst.valueSparse(j);
      }
    }        
    docLength = Math.sqrt(docLength);

    // Normalize document vector
    for(int j=0; j<inst.numValues(); j++) {
      if(inst.index(j)>=firstCopy) {
	double val = inst.valueSparse(j) * avgDocLength / docLength;
	inst.setValueSparse(j, val);
	if (val == 0){
	  System.err.println("setting value "+inst.index(j)+" to zero.");
	  j--;
	}
      }
    }        
  }
  
  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: 
   * use -h for help
   */
  public static void main(String [] argv) {

    try {
      if (Utils.getFlag('b', argv)) {
 	Filter.batchFilterFile(new StringToWordVector(), argv);
      } else {
	Filter.filterFile(new StringToWordVector(), argv);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      System.out.println(ex.getMessage());
    }
  }
  
  
  /**
   * alphabetic string tokenizer
   */
  private class AlphabeticStringTokenizer 
      implements Enumeration {
    
      /** the characters of the string */
      private char[] str;
      
      /** the current position */
      int currentPos=0;
      
      /**
       * Constructor
       * 
       * @param toTokenize the string to tokenize
       */
      public AlphabeticStringTokenizer(String toTokenize) {
          str = new char[toTokenize.length()];
          toTokenize.getChars(0, toTokenize.length(), str, 0);
      }
      
      /**
       * returns whether there are more elements still
       * 
       * @return true if there are still more elements
       */
      public boolean hasMoreElements() {
          int beginpos = currentPos;
          
          while( beginpos < str.length && 
                 (str[beginpos]<'a' || str[beginpos]>'z') &&
                 (str[beginpos]<'A' || str[beginpos]>'Z') ) {
                     beginpos++;    
          }
          currentPos = beginpos;
          //System.out.println("Currently looking at "+str[beginpos]);
          
          if( beginpos<str.length && 
              ((str[beginpos]>='a' && str[beginpos]<='z') ||
               (str[beginpos]>='A' && str[beginpos]<='Z')) ) {
                   return true;
          }
          else
              return false;
      }
      
      /**
       * returns the next element
       * 
       * @return the next element
       */
      public Object nextElement() {
          int beginpos, endpos;
          beginpos = currentPos;
          
          while( beginpos < str.length && 
                 (str[beginpos]<'a' && str[beginpos]>'z') &&
                 (str[beginpos]<'A' && str[beginpos]>'Z') ) {
                     beginpos++;    
          }
          currentPos = endpos = beginpos;
          
          if(beginpos>=str.length)
              throw new NoSuchElementException("no more tokens present");
          
          while( endpos < str.length && 
                 ((str[endpos]>='a' && str[endpos]<='z') ||
                  (str[endpos]>='A' && str[endpos]<='Z')) ) {                     
                     endpos++;
          }
          
          String s = new String(str, beginpos, endpos-currentPos);
          currentPos = endpos;
          //System.out.println("found token >"+s+
          //                   "< beginpos: "+beginpos+
          //                   " endpos: "+endpos+
          //                   " str.length: "+str.length+
          //                   " str[beginpos]: "+str[beginpos]);
          return s;
      }      
  }
}
