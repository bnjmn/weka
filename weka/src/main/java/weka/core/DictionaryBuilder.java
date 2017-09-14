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
 *    DictionaryBuilder.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import weka.core.stemmers.NullStemmer;
import weka.core.stemmers.Stemmer;
import weka.core.stopwords.Null;
import weka.core.stopwords.StopwordsHandler;
import weka.core.tokenizers.Tokenizer;
import weka.core.tokenizers.WordTokenizer;
import weka.gui.ProgrammaticProperty;

/**
 * Class for building and maintaining a dictionary of terms. Has methods for
 * loading, saving and aggregating dictionaries. Supports loading/saving in
 * binary and textual format. Textual format is expected to have one or two
 * comma separated values per line of the format.
 * <p>
 * 
 * <pre>
 * term [,doc_count]
 * </pre>
 * 
 * where
 * 
 * <pre>
 * doc_count
 * </pre>
 * 
 * is the number of documents that the term has occurred in.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class DictionaryBuilder implements Aggregateable<DictionaryBuilder>,
  OptionHandler, Serializable {

  /** For serialization */
  private static final long serialVersionUID = 5579506627960356012L;

  /** Input structure */
  protected Instances m_inputFormat;

  /** Output structure */
  protected Instances m_outputFormat;

  /** Holds the dictionaries (one per class) that are compiled while processing */
  protected Map<String, int[]>[] m_dictsPerClass;

  /**
   * Holds the final dictionary that is consolidated across classes and pruned
   * according to m_wordsToKeep. First element of array contains the index of
   * the word. The second (optional) element contains the document count for the
   * word (i.e. number of training docs the word occurs in).
   */
  protected Map<String, int[]> m_consolidatedDict;

  /** Holds the tokenized input vector */
  protected transient Map<String, int[]> m_inputVector;

  /**
   * True if the final number of words to keep should not be applied on a per
   * class basis
   */
  protected boolean m_doNotOperateOnPerClassBasis;

  /** Whether to output frequency counts instead of presence indicators */
  protected boolean m_outputCounts;

  /** True if all tokens should be downcased. */
  protected boolean m_lowerCaseTokens;

  /** the stemming algorithm. */
  protected Stemmer m_stemmer = new NullStemmer();

  /** Stopword handler to use. */
  protected StopwordsHandler m_stopwordsHandler = new Null();

  /**
   * The default number of words (per class if there is a class attribute
   * assigned) to attempt to keep.
   */
  protected int m_wordsToKeep = 1000;

  /**
   * Prune dictionary (per class) of low freq terms after every x documents. 0 =
   * no periodic pruning
   */
  protected long m_periodicPruneRate;

  /** Minimum frequency to retain dictionary entries */
  protected int m_minFrequency = 1;

  /** Count of input vectors seen */
  protected int m_count = 0;

  /** the tokenizer algorithm to use. */
  protected Tokenizer m_tokenizer = new WordTokenizer();

  /** Range of columns to convert to word vectors. */
  protected Range m_selectedRange = new Range("first-last");

  /** Holds the class index */
  protected int m_classIndex = -1;

  /** Number of classes */
  protected int m_numClasses = 1;

  /** A String prefix for the attribute names. */
  protected String m_Prefix = "";

  /** True if the TF transform is to be applied */
  protected boolean m_TFTransform;

  /** True if the IDF transform is to be applied */
  protected boolean m_IDFTransform;

  /** Whether to normalize to average length of training docs */
  protected boolean m_normalize;

  /** The sum of document lengths */
  protected double m_docLengthSum;

  /** The average document length */
  protected double m_avgDocLength;

  /** Whether to keep the dictionary(s) sorted alphabetically */
  protected boolean m_sortDictionary;

  /** True if the input data contains string attributes to convert */
  protected boolean m_inputContainsStringAttributes;

  /**
   * Set the average document length to use when normalizing
   *
   * @param averageDocLength the average document length to use
   */
  @ProgrammaticProperty
  public void setAverageDocLength(double averageDocLength) {
    m_avgDocLength = averageDocLength;
  }

  /**
   * Get the average document length to use when normalizing
   *
   * @return the average document length
   */
  public double getAverageDocLength() {
    return m_avgDocLength;
  }

  /**
   * Tip text for this property
   *
   * @return the tip text for this property
   */
  public String sortDictionaryTipText() {
    return "Sort the dictionary alphabetically";
  }

  /**
   * Set whether to keep the dictionary sorted alphabetically as it is built.
   * Setting this to true uses a TreeMap internally (which is slower than the
   * default unsorted LinkedHashMap).
   *
   * @param sortDictionary true to keep the dictionary sorted alphabetically
   */
  public void setSortDictionary(boolean sortDictionary) {
    m_sortDictionary = sortDictionary;
  }

  /**
   * Get whether to keep the dictionary sorted alphabetically as it is built.
   * Setting this to true uses a TreeMap internally (which is slower than the
   * default unsorted LinkedHashMap).
   *
   * @return true to keep the dictionary sorted alphabetically
   */
  public boolean getSortDictionary() {
    return m_sortDictionary;
  }

  /**
   * Gets whether output instances contain 0 or 1 indicating word presence, or
   * word counts.
   * 
   * @return true if word counts should be output.
   */
  public boolean getOutputWordCounts() {
    return m_outputCounts;
  }

  /**
   * Sets whether output instances contain 0 or 1 indicating word presence, or
   * word counts.
   * 
   * @param outputWordCounts true if word counts should be output.
   */
  public void setOutputWordCounts(boolean outputWordCounts) {
    m_outputCounts = outputWordCounts;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String outputWordCountsTipText() {
    return "Output word counts rather than boolean 0 or 1"
      + "(indicating presence or absence of a word).";
  }

  /**
   * Get the value of m_SelectedRange.
   * 
   * @return Value of m_SelectedRange.
   */
  public Range getSelectedRange() {
    return m_selectedRange;
  }

  /**
   * Set the value of m_SelectedRange.
   * 
   * @param newSelectedRange Value to assign to m_SelectedRange.
   */
  public void setSelectedRange(String newSelectedRange) {
    m_selectedRange = new Range(newSelectedRange);
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String attributeIndicesTipText() {
    return "Specify range of attributes to act on."
      + " This is a comma separated list of attribute indices, with"
      + " \"first\" and \"last\" valid values. Specify an inclusive"
      + " range with \"-\". E.g: \"first-3,5,6-10,last\".";
  }

  /**
   * Gets the current range selection.
   * 
   * @return a string containing a comma separated list of ranges
   */
  public String getAttributeIndices() {
    return m_selectedRange.getRanges();
  }

  /**
   * Sets which attributes are to be worked on.
   * 
   * @param rangeList a string representing the list of attributes. Since the
   *          string will typically come from a user, attributes are indexed
   *          from 1. <br>
   *          eg: first-3,5,6-last
   * @throws IllegalArgumentException if an invalid range list is supplied
   */
  public void setAttributeIndices(String rangeList) {
    m_selectedRange.setRanges(rangeList);
  }

  /**
   * Sets which attributes are to be processed.
   * 
   * @param attributes an array containing indexes of attributes to process.
   *          Since the array will typically come from a program, attributes are
   *          indexed from 0.
   * @throws IllegalArgumentException if an invalid set of ranges is supplied
   */
  public void setAttributeIndicesArray(int[] attributes) {
    setAttributeIndices(Range.indicesToRangeList(attributes));
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String invertSelectionTipText() {
    return "Set attribute selection mode. If false, only selected"
      + " attributes in the range will be worked on; if"
      + " true, only non-selected attributes will be processed.";
  }

  /**
   * Gets whether the supplied columns are to be processed or skipped.
   * 
   * @return true if the supplied columns will be kept
   */
  public boolean getInvertSelection() {
    return m_selectedRange.getInvert();
  }

  /**
   * Sets whether selected columns should be processed or skipped.
   * 
   * @param invert the new invert setting
   */
  public void setInvertSelection(boolean invert) {
    m_selectedRange.setInvert(invert);
  }

  /**
   * Gets the number of words (per class if there is a class attribute assigned)
   * to attempt to keep.
   * 
   * @return the target number of words in the output vector (per class if
   *         assigned).
   */
  public int getWordsToKeep() {
    return m_wordsToKeep;
  }

  /**
   * Sets the number of words (per class if there is a class attribute assigned)
   * to attempt to keep.
   * 
   * @param newWordsToKeep the target number of words in the output vector (per
   *          class if assigned).
   */
  public void setWordsToKeep(int newWordsToKeep) {
    m_wordsToKeep = newWordsToKeep;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String wordsToKeepTipText() {
    return "The number of words (per class if there is a class attribute "
      + "assigned) to attempt to keep.";
  }

  /**
   * Gets the rate (number of instances) at which the dictionary is periodically
   * pruned.
   * 
   * @return the rate at which the dictionary is periodically pruned
   */
  public long getPeriodicPruning() {
    return m_periodicPruneRate;
  }

  /**
   * Sets the rate (number of instances) at which the dictionary is periodically
   * pruned
   * 
   * 
   * @param newPeriodicPruning the rate at which the dictionary is periodically
   *          pruned
   */
  public void setPeriodicPruning(long newPeriodicPruning) {
    m_periodicPruneRate = newPeriodicPruning;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String periodicPruningTipText() {
    return "Specify the rate (x% of the input dataset) at which to periodically prune the dictionary. "
      + "wordsToKeep prunes after creating a full dictionary. You may not have enough "
      + "memory for this approach.";
  }

  /**
   * Gets whether if the word frequencies should be transformed into log(1+fij)
   * where fij is the frequency of word i in document(instance) j.
   * 
   * @return true if word frequencies are to be transformed.
   */
  public boolean getTFTransform() {
    return m_TFTransform;
  }

  /**
   * Sets whether if the word frequencies should be transformed into log(1+fij)
   * where fij is the frequency of word i in document(instance) j.
   * 
   * @param TFTransform true if word frequencies are to be transformed.
   */
  public void setTFTransform(boolean TFTransform) {
    this.m_TFTransform = TFTransform;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String TFTransformTipText() {
    return "Sets whether if the word frequencies should be transformed into:\n "
      + "   log(1+fij) \n"
      + "       where fij is the frequency of word i in document (instance) j.";
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
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String attributeNamePrefixTipText() {
    return "Prefix for the created attribute names. " + "(default: \"\")";
  }

  /**
   * Sets whether if the word frequencies in a document should be transformed
   * into: <br>
   * fij*log(num of Docs/num of Docs with word i) <br>
   * where fij is the frequency of word i in document(instance) j.
   * 
   * @return true if the word frequencies are to be transformed.
   */
  public boolean getIDFTransform() {
    return this.m_IDFTransform;
  }

  /**
   * Sets whether if the word frequencies in a document should be transformed
   * into: <br>
   * fij*log(num of Docs/num of Docs with word i) <br>
   * where fij is the frequency of word i in document(instance) j.
   * 
   * @param IDFTransform true if the word frequecies are to be transformed
   */
  public void setIDFTransform(boolean IDFTransform) {
    this.m_IDFTransform = IDFTransform;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String IDFTransformTipText() {
    return "Sets whether if the word frequencies in a document should be "
      + "transformed into: \n"
      + "   fij*log(num of Docs/num of Docs with word i) \n"
      + "      where fij is the frequency of word i in document (instance) j.";
  }

  /**
   * Get whether word frequencies for a document should be normalized
   *
   * @return true if word frequencies should be normalized
   */
  public boolean getNormalize() {
    return m_normalize;
  }

  /**
   * Set whether word frequencies for a document should be normalized
   *
   * @param n true if word frequencies should be normalized
   */
  public void setNormalize(boolean n) {
    m_normalize = n;
  }

  /**
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String normalizeTipText() {
    return "Whether word frequencies for a document (instance) should "
      + "be normalized or not";
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String normalizeDocLengthTipText() {
    return "Sets whether if the word frequencies for a document (instance) "
      + "should be normalized or not.";
  }

  /**
   * Gets whether if the tokens are to be downcased or not.
   * 
   * @return true if the tokens are to be downcased.
   */
  public boolean getLowerCaseTokens() {
    return this.m_lowerCaseTokens;
  }

  /**
   * Sets whether if the tokens are to be downcased or not. (Doesn't affect
   * non-alphabetic characters in tokens).
   * 
   * @param downCaseTokens should be true if only lower case tokens are to be
   *          formed.
   */
  public void setLowerCaseTokens(boolean downCaseTokens) {
    this.m_lowerCaseTokens = downCaseTokens;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String lowerCaseTokensTipText() {
    return "If set then all the word tokens are converted to lower case "
      + "before being added to the dictionary.";
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String doNotOperateOnPerClassBasisTipText() {
    return "If this is set, the maximum number of words and the "
      + "minimum term frequency is not enforced on a per-class "
      + "basis but based on the documents in all the classes "
      + "(even if a class attribute is set).";
  }

  /**
   * Get the DoNotOperateOnPerClassBasis value.
   * 
   * @return the DoNotOperateOnPerClassBasis value.
   */
  public boolean getDoNotOperateOnPerClassBasis() {
    return m_doNotOperateOnPerClassBasis;
  }

  /**
   * Set the DoNotOperateOnPerClassBasis value.
   * 
   * @param newDoNotOperateOnPerClassBasis The new DoNotOperateOnPerClassBasis
   *          value.
   */
  public void setDoNotOperateOnPerClassBasis(
    boolean newDoNotOperateOnPerClassBasis) {
    this.m_doNotOperateOnPerClassBasis = newDoNotOperateOnPerClassBasis;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String minTermFreqTipText() {
    return "Sets the minimum term frequency. This is enforced "
      + "on a per-class basis.";
  }

  /**
   * Get the MinTermFreq value.
   * 
   * @return the MinTermFreq value.
   */
  public int getMinTermFreq() {
    return m_minFrequency;
  }

  /**
   * Set the MinTermFreq value.
   * 
   * @param newMinTermFreq The new MinTermFreq value.
   */
  public void setMinTermFreq(int newMinTermFreq) {
    m_minFrequency = newMinTermFreq;
  }

  /**
   * Returns the current stemming algorithm, null if none is used.
   *
   * @return the current stemming algorithm, null if none set
   */
  public Stemmer getStemmer() {
    return m_stemmer;
  }

  /**
   * the stemming algorithm to use, null means no stemming at all (i.e., the
   * NullStemmer is used).
   *
   * @param value the configured stemming algorithm, or null
   * @see NullStemmer
   */
  public void setStemmer(Stemmer value) {
    if (value != null) {
      m_stemmer = value;
    } else {
      m_stemmer = new NullStemmer();
    }
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String stemmerTipText() {
    return "The stemming algorithm to use on the words.";
  }

  /**
   * Gets the stopwords handler.
   *
   * @return the stopwords handler
   */
  public StopwordsHandler getStopwordsHandler() {
    return m_stopwordsHandler;
  }

  /**
   * Sets the stopwords handler to use.
   *
   * @param value the stopwords handler, if null, Null is used
   */
  public void setStopwordsHandler(StopwordsHandler value) {
    if (value != null) {
      m_stopwordsHandler = value;
    } else {
      m_stopwordsHandler = new Null();
    }
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String stopwordsHandlerTipText() {
    return "The stopwords handler to use (Null means no stopwords are used).";
  }

  /**
   * Returns the current tokenizer algorithm.
   *
   * @return the current tokenizer algorithm
   */
  public Tokenizer getTokenizer() {
    return m_tokenizer;
  }

  /**
   * the tokenizer algorithm to use.
   *
   * @param value the configured tokenizing algorithm
   */
  public void setTokenizer(Tokenizer value) {
    m_tokenizer = value;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String tokenizerTipText() {
    return "The tokenizing algorithm to use on the strings.";
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> result = new Vector<Option>();

    result
      .addElement(new Option(
        "\tOutput word counts rather than boolean word presence.\n", "C", 0,
        "-C"));

    result.addElement(new Option(
      "\tSpecify list of string attributes to convert to words (as weka Range).\n"
        + "\t(default: select all string attributes)", "R", 1,
      "-R <index1,index2-index4,...>"));

    result.addElement(new Option("\tInvert matching sense of column indexes.",
      "V", 0, "-V"));

    result.addElement(new Option(
      "\tSpecify a prefix for the created attribute names.\n"
        + "\t(default: \"\")", "P", 1, "-P <attribute name prefix>"));

    result.addElement(new Option(
      "\tSpecify approximate number of word fields to create.\n"
        + "\tSurplus words will be discarded..\n" + "\t(default: 1000)", "W",
      1, "-W <number of words to keep>"));

    result
      .addElement(new Option(
        "\tSpecify the rate (e.g., every x instances) at which to periodically prune the dictionary.\n"
          + "\t-W prunes after creating a full dictionary. You may not have enough memory for this approach.\n"
          + "\t(default: no periodic pruning)", "prune-rate", 1,
        "-prune-rate <every x instances>"));

    result
      .addElement(new Option(
        "\tTransform the word frequencies into log(1+fij)\n"
          + "\twhere fij is the frequency of word i in jth document(instance).\n",
        "T", 0, "-T"));

    result.addElement(new Option("\tTransform each word frequency into:\n"
      + "\tfij*log(num of Documents/num of documents containing word i)\n"
      + "\t  where fij if frequency of word i in jth document(instance)", "I",
      0, "-I"));

    result
      .addElement(new Option(
        "\tWhether to 0=not normalize/1=normalize all data/2=normalize test data only\n"
          + "\tto average length of training documents "
          + "(default 0=don\'t normalize).", "N", 1, "-N"));

    result.addElement(new Option("\tConvert all tokens to lowercase before "
      + "adding to the dictionary.", "L", 0, "-L"));

    result.addElement(new Option(
      "\tThe stopwords handler to use (default Null).", "-stopwords-handler",
      1, "-stopwords-handler"));

    result.addElement(new Option(
      "\tThe stemming algorithm (classname plus parameters) to use.",
      "stemmer", 1, "-stemmer <spec>"));

    result.addElement(new Option("\tThe minimum term frequency (default = 1).",
      "M", 1, "-M <int>"));

    result.addElement(new Option(
      "\tIf this is set, the maximum number of words and the \n"
        + "\tminimum term frequency is not enforced on a per-class \n"
        + "\tbasis but based on the documents in all the classes \n"
        + "\t(even if a class attribute is set).", "O", 0, "-O"));

    result.addElement(new Option(
      "\tThe tokenizing algorihtm (classname plus parameters) to use.\n"
        + "\t(default: " + WordTokenizer.class.getName() + ")", "tokenizer", 1,
      "-tokenizer <spec>"));

    return result.elements();
  }

  /**
   * Gets the current settings of the DictionaryBuilder
   *
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {
    Vector<String> result = new Vector<String>();

    result.add("-R");
    result.add(getSelectedRange().getRanges());

    if (getInvertSelection()) {
      result.add("-V");
    }

    if (!"".equals(getAttributeNamePrefix())) {
      result.add("-P");
      result.add(getAttributeNamePrefix());
    }

    result.add("-W");
    result.add(String.valueOf(getWordsToKeep()));

    result.add("-prune-rate");
    result.add(String.valueOf(getPeriodicPruning()));

    if (getOutputWordCounts()) {
      result.add("-C");
    }

    if (getTFTransform()) {
      result.add("-T");
    }

    if (getIDFTransform()) {
      result.add("-I");
    }

    if (getNormalize()) {
      result.add("-N");
    }

    if (getLowerCaseTokens()) {
      result.add("-L");
    }

    if (getStemmer() != null) {
      result.add("-stemmer");
      String spec = getStemmer().getClass().getName();
      if (getStemmer() instanceof OptionHandler) {
        spec +=
          " " + Utils.joinOptions(((OptionHandler) getStemmer()).getOptions());
      }
      result.add(spec.trim());
    }

    if (getStopwordsHandler() != null) {
      result.add("-stopwords-handler");
      String spec = getStopwordsHandler().getClass().getName();
      if (getStopwordsHandler() instanceof OptionHandler) {
        spec +=
          " "
            + Utils.joinOptions(((OptionHandler) getStopwordsHandler())
              .getOptions());
      }
      result.add(spec.trim());
    }

    result.add("-M");
    result.add(String.valueOf(getMinTermFreq()));

    if (getDoNotOperateOnPerClassBasis()) {
      result.add("-O");
    }

    result.add("-tokenizer");
    String spec = getTokenizer().getClass().getName();

    spec +=
      " " + Utils.joinOptions(((OptionHandler) getTokenizer()).getOptions());

    result.add(spec.trim());

    return result.toArray(new String[result.size()]);
  }

  /**
   * Parses a given list of options.
   * <p/>
   *
   * <!-- options-start --> Valid options are:
   * <p/>
   *
   * <pre>
   * -C
   *  Output word counts rather than boolean word presence.
   * </pre>
   *
   * <pre>
   * -R &lt;index1,index2-index4,...&gt;
   *  Specify list of string attributes to convert to words (as weka Range).
   *  (default: select all string attributes)
   * </pre>
   *
   * <pre>
   * -V
   *  Invert matching sense of column indexes.
   * </pre>
   *
   * <pre>
   * -P &lt;attribute name prefix&gt;
   *  Specify a prefix for the created attribute names.
   *  (default: "")
   * </pre>
   *
   * <pre>
   * -W &lt;number of words to keep&gt;
   *  Specify approximate number of word fields to create.
   *  Surplus words will be discarded..
   *  (default: 1000)
   * </pre>
   *
   * <pre>
   * -prune-rate &lt;rate as a percentage of dataset&gt;
   *  Specify the rate (e.g., every 10% of the input dataset) at which to periodically prune the dictionary.
   *  -W prunes after creating a full dictionary. You may not have enough memory for this approach.
   *  (default: no periodic pruning)
   * </pre>
   *
   * <pre>
   * -T
   *  Transform the word frequencies into log(1+fij)
   *  where fij is the frequency of word i in jth document(instance).
   * </pre>
   *
   * <pre>
   * -I
   *  Transform each word frequency into:
   *  fij*log(num of Documents/num of documents containing word i)
   *    where fij if frequency of word i in jth document(instance)
   * </pre>
   *
   * <pre>
   * -N
   *  Whether to 0=not normalize/1=normalize all data/2=normalize test data only
   *  to average length of training documents (default 0=don't normalize).
   * </pre>
   *
   * <pre>
   * -L
   *  Convert all tokens to lowercase before adding to the dictionary.
   * </pre>
   *
   * <pre>
   * -stopwords-handler
   *  The stopwords handler to use (default Null).
   * </pre>
   *
   * <pre>
   * -stemmer &lt;spec&gt;
   *  The stemming algorithm (classname plus parameters) to use.
   * </pre>
   *
   * <pre>
   * -M &lt;int&gt;
   *  The minimum term frequency (default = 1).
   * </pre>
   *
   * <pre>
   * -O
   *  If this is set, the maximum number of words and the
   *  minimum term frequency is not enforced on a per-class
   *  basis but based on the documents in all the classes
   *  (even if a class attribute is set).
   * </pre>
   *
   * <pre>
   * -tokenizer &lt;spec&gt;
   *  The tokenizing algorihtm (classname plus parameters) to use.
   *  (default: weka.core.tokenizers.WordTokenizer)
   * </pre>
   *
   * <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    String value = Utils.getOption('R', options);
    if (value.length() != 0) {
      setSelectedRange(value);
    } else {
      setSelectedRange("first-last");
    }

    setInvertSelection(Utils.getFlag('V', options));

    value = Utils.getOption('P', options);
    if (value.length() != 0) {
      setAttributeNamePrefix(value);
    } else {
      setAttributeNamePrefix("");
    }

    value = Utils.getOption('W', options);
    if (value.length() != 0) {
      setWordsToKeep(Integer.valueOf(value).intValue());
    } else {
      setWordsToKeep(1000);
    }

    value = Utils.getOption("prune-rate", options);
    if (value.length() > 0) {
      setPeriodicPruning(Integer.parseInt(value));
    } else {
      setPeriodicPruning(-1);
    }

    value = Utils.getOption('M', options);
    if (value.length() != 0) {
      setMinTermFreq(Integer.valueOf(value).intValue());
    } else {
      setMinTermFreq(1);
    }

    setOutputWordCounts(Utils.getFlag('C', options));

    setTFTransform(Utils.getFlag('T', options));

    setIDFTransform(Utils.getFlag('I', options));

    setDoNotOperateOnPerClassBasis(Utils.getFlag('O', options));

    setNormalize(Utils.getFlag('N', options));

    setLowerCaseTokens(Utils.getFlag('L', options));

    String stemmerString = Utils.getOption("stemmer", options);
    if (stemmerString.length() == 0) {
      setStemmer(null);
    } else {
      String[] stemmerSpec = Utils.splitOptions(stemmerString);
      if (stemmerSpec.length == 0) {
        throw new Exception("Invalid stemmer specification string");
      }
      String stemmerName = stemmerSpec[0];
      stemmerSpec[0] = "";
      Stemmer stemmer =
        (Stemmer) Utils.forName(weka.core.stemmers.Stemmer.class, stemmerName,
          stemmerSpec);
      setStemmer(stemmer);
    }

    String stopwordsHandlerString =
      Utils.getOption("stopwords-handler", options);
    if (stopwordsHandlerString.length() == 0) {
      setStopwordsHandler(null);
    } else {
      String[] stopwordsHandlerSpec =
        Utils.splitOptions(stopwordsHandlerString);
      if (stopwordsHandlerSpec.length == 0) {
        throw new Exception("Invalid StopwordsHandler specification string");
      }
      String stopwordsHandlerName = stopwordsHandlerSpec[0];
      stopwordsHandlerSpec[0] = "";
      StopwordsHandler stopwordsHandler =
        (StopwordsHandler) Utils.forName(
          weka.core.stopwords.StopwordsHandler.class, stopwordsHandlerName,
          stopwordsHandlerSpec);
      setStopwordsHandler(stopwordsHandler);
    }

    String tokenizerString = Utils.getOption("tokenizer", options);
    if (tokenizerString.length() == 0) {
      setTokenizer(new WordTokenizer());
    } else {
      String[] tokenizerSpec = Utils.splitOptions(tokenizerString);
      if (tokenizerSpec.length == 0) {
        throw new Exception("Invalid tokenizer specification string");
      }
      String tokenizerName = tokenizerSpec[0];
      tokenizerSpec[0] = "";
      Tokenizer tokenizer =
        (Tokenizer) Utils.forName(weka.core.tokenizers.Tokenizer.class,
          tokenizerName, tokenizerSpec);

      setTokenizer(tokenizer);
    }

    Utils.checkForRemainingOptions(options);
  }

  @SuppressWarnings("unchecked")
  public void setup(Instances inputFormat) throws Exception {

    m_inputContainsStringAttributes = inputFormat.checkForStringAttributes();
    m_inputFormat = inputFormat.stringFreeStructure();

    if (!m_inputContainsStringAttributes) {
      return;
    }

    m_numClasses =
      !m_doNotOperateOnPerClassBasis && m_inputFormat.classIndex() >= 0 && m_inputFormat.classAttribute().isNominal() ?
              m_inputFormat.numClasses() : 1;
    m_dictsPerClass =
      m_sortDictionary ? new TreeMap[m_numClasses]
        : new LinkedHashMap[m_numClasses];
    m_classIndex = m_inputFormat.classIndex();

    for (int i = 0; i < m_numClasses; i++) {
      m_dictsPerClass[i] =
        m_sortDictionary ? new TreeMap<String, int[]>()
          : new LinkedHashMap<String, int[]>();
    }

    determineSelectedRange(inputFormat);
  }

  /**
   * Gets the currently set input format
   *
   * @return the current input format
   */
  public Instances getInputFormat() {
    return m_inputFormat;
  }

  /**
   * Returns true if this DictionaryBuilder is ready to vectorize incoming
   * instances
   *
   * @return true if we can vectorize incoming instances
   */
  public boolean readyToVectorize() {
    return m_inputFormat != null && m_consolidatedDict != null;
  }

  /**
   * determines the selected range.
   */
  private void determineSelectedRange(Instances inputFormat) {

    // Calculate the default set of fields to convert
    if (m_selectedRange == null) {
      StringBuffer fields = new StringBuffer();
      for (int j = 0; j < inputFormat.numAttributes(); j++) {
        if (inputFormat.attribute(j).type() == Attribute.STRING) {
          fields.append((j + 1) + ",");
        }
      }
      m_selectedRange = new Range(fields.toString());
    }
    m_selectedRange.setUpper(inputFormat.numAttributes() - 1);

    // Prevent the user from converting non-string fields
    StringBuffer fields = new StringBuffer();
    for (int j = 0; j < inputFormat.numAttributes(); j++) {
      if (m_selectedRange.getInvert()) {
        if (!m_selectedRange.isInRange(j) ||
                inputFormat.attribute(j).type() != Attribute.STRING) {
          fields.append((j + 1) + ",");
        }
      } else {
        if (m_selectedRange.isInRange(j)
                && inputFormat.attribute(j).type() == Attribute.STRING) {
          fields.append((j + 1) + ",");
        }
      }
    }
    m_selectedRange.setRanges(fields.toString());
    m_selectedRange.setUpper(inputFormat.numAttributes() - 1);
  }

  /**
   * Get the output format
   * 
   * @return the output format
   * @throws Exception if there is no input format set and/or the dictionary has
   *           not been constructed yet.
   */
  public Instances getVectorizedFormat() throws Exception {

    if (m_inputFormat == null) {
      throw new Exception("No input format available. Call setup() and "
        + "make sure a dictionary has been built first.");
    }

    if (!m_inputContainsStringAttributes) {
      return m_inputFormat;
    }

    if (m_consolidatedDict == null) {
      throw new Exception("Dictionary hasn't been built or finalized yet!");
    }

    if (m_outputFormat != null) {
      return m_outputFormat;
    }

    ArrayList<Attribute> newAtts = new ArrayList<Attribute>();
    // int classIndex = m_inputFormat.classIndex();
    int classIndex = -1;
    for (int i = 0; i < m_inputFormat.numAttributes(); i++) {
      /*
       * if (i == m_inputFormat.classIndex()) { continue; }
       */

      if (!m_selectedRange.isInRange(i)) {
        if (m_inputFormat.classIndex() == i) {
          classIndex = newAtts.size();
        }
        newAtts.add((Attribute) m_inputFormat.attribute(i).copy());
      }
    }

    // now do the dictionary
    for (Map.Entry<String, int[]> e : m_consolidatedDict.entrySet()) {
      newAtts.add(new Attribute(m_Prefix + e.getKey()));
    }

    /* Instances newFormat =
      new Instances(m_inputFormat.relationName() + "_dictionaryBuilder_"
        + m_consolidatedDict.size(), newAtts, 0); */

    Instances newFormat =
      new Instances(m_inputFormat.relationName(), newAtts, 0);


    if (classIndex >= 0) {
      newFormat.setClassIndex(classIndex);
    }

    return newFormat;
  }

  /**
   * Convert a batch of instances
   * 
   * @param batch the batch to convert.
   * @param setAvgDocLength true to compute and set the average document length
   *          for this DictionaryBuilder from the batch - this uses the final
   *          pruned dictionary when computing doc lengths. When vectorizing
   *          non-training batches, and normalization has been turned on, this
   *          should be set to false.
   * 
   * @return the converted batch
   * @throws Exception if there is no input format set and/or the dictionary has
   *           not been constructed yet.
   */
  public Instances vectorizeBatch(Instances batch, boolean setAvgDocLength)
    throws Exception {

    if (m_inputFormat == null) {
      throw new Exception("No input format available. Call setup() and "
        + "make sure a dictionary has been built first.");
    }

    if (!m_inputContainsStringAttributes) {
      // nothing to do
      return batch;
    }

    if (m_consolidatedDict == null) {
      throw new Exception("Dictionary hasn't been built or consolidated yet!");
    }

    Instances vectorized = new Instances(m_outputFormat, batch.numInstances());

    boolean normTemp = m_normalize;
    if (setAvgDocLength) {
      // make sure normalization is turned off until the second pass once
      // we've set the post dictionary pruning average document length
      m_normalize = false;
    }

    if (batch.numInstances() > 0) {
      int[] offsetHolder = new int[1];
      vectorized.add(vectorizeInstance(batch.instance(0), offsetHolder, true));
      for (int i = 1; i < batch.numInstances(); i++) {
        vectorized
          .add(vectorizeInstance(batch.instance(i), offsetHolder, true));
      }

      if (setAvgDocLength) {
        m_avgDocLength = 0;
        for (int i = 0; i < vectorized.numInstances(); i++) {
          Instance inst = vectorized.instance(i);
          double docLength = 0;
          for (int j = 0; j < inst.numValues(); j++) {
            if (inst.index(j) >= offsetHolder[0]) {
              docLength += inst.valueSparse(j) * inst.valueSparse(j);
            }
          }
          m_avgDocLength += Math.sqrt(docLength);
        }
        m_avgDocLength /= vectorized.numInstances();

        if (normTemp) {
          for (int i = 0; i < vectorized.numInstances(); i++) {
            normalizeInstance(vectorized.instance(i), offsetHolder[0]);
          }
        }
      }
    }

    m_normalize = normTemp;

    vectorized.compactify();
    return vectorized;
  }

  /**
   * Convert an input instance. Any string attributes not being vectorized do
   * not have their values retained in memory (i.e. only the string values for
   * the instance being vectorized are held in memory).
   * 
   * @param input the input instance
   * @return a converted instance
   * @throws Exception if there is no input format set and/or the dictionary has
   *           not been constructed yet.
   */
  public Instance vectorizeInstance(Instance input) throws Exception {
    return vectorizeInstance(input, new int[1], false);
  }

  /**
   * Convert an input instance.
   * 
   * @param input the input instance
   * @param retainStringAttValuesInMemory true if the values of string
   *          attributes not being vectorized should be retained in memory
   * @return a converted instance
   * @throws Exception if there is no input format set and/or the dictionary has
   *           not been constructed yet
   */
  public Instance vectorizeInstance(Instance input,
    boolean retainStringAttValuesInMemory) throws Exception {
    return vectorizeInstance(input, new int[1], retainStringAttValuesInMemory);
  }

  private Instance vectorizeInstance(Instance input, int[] offsetHolder,
    boolean retainStringAttValuesInMemory) throws Exception {

    if (!m_inputContainsStringAttributes) {
      return input;
    }

    if (m_inputFormat == null) {
      throw new Exception("No input format available. Call setup() and "
        + "make sure a dictionary has been built first.");
    }

    if (m_consolidatedDict == null) {
      throw new Exception("Dictionary hasn't been built or consolidated yet!");
    }

    int indexOffset = 0;
    int classIndex = m_outputFormat.classIndex();
    Map<Integer, double[]> contained = new TreeMap<Integer, double[]>();
    for (int i = 0; i < m_inputFormat.numAttributes(); i++) {
      if (!m_selectedRange.isInRange(i)) {
        if (!m_inputFormat.attribute(i).isString()
          && !m_inputFormat.attribute(i).isRelationValued()) {

          // add nominal and numeric directly
          if (input.value(i) != 0.0) {
            contained.put(indexOffset, new double[] { input.value(i) });
          }
        } else {
          if (input.isMissing(i)) {
            contained.put(indexOffset, new double[] { Utils.missingValue() });
          } else if (m_inputFormat.attribute(i).isString()) {
            String strVal = input.stringValue(i);
            if (retainStringAttValuesInMemory) {
              double strIndex =
                m_outputFormat.attribute(indexOffset).addStringValue(strVal);
              contained.put(indexOffset, new double[] { strIndex });
            } else {
              m_outputFormat.attribute(indexOffset).setStringValue(strVal);
              contained.put(indexOffset, new double[] { 0 });
            }
          } else {
            // relational
            if (m_outputFormat.attribute(indexOffset).numValues() == 0) {
              Instances relationalHeader =
                m_outputFormat.attribute(indexOffset).relation();

              // hack to defeat sparse instances bug
              m_outputFormat.attribute(indexOffset).addRelation(
                relationalHeader);
            }
            int newIndex =
              m_outputFormat.attribute(indexOffset).addRelation(
                input.relationalValue(i));
            contained.put(indexOffset, new double[] { newIndex });
          }
        }
        indexOffset++;
      }
    }

    offsetHolder[0] = indexOffset;

    // dictionary entries
    for (int i = 0; i < m_inputFormat.numAttributes(); i++) {
      if (m_selectedRange.isInRange(i) && !input.isMissing(i)) {
        m_tokenizer.tokenize(input.stringValue(i));

        while (m_tokenizer.hasMoreElements()) {
          String word = m_tokenizer.nextElement();
          if (m_lowerCaseTokens) {
            word = word.toLowerCase();
          }
          word = m_stemmer.stem(word);

          int[] idxAndDocCount = m_consolidatedDict.get(word);
          if (idxAndDocCount != null) {
            if (m_outputCounts) {
              double[] inputCount =
                contained.get(idxAndDocCount[0] + indexOffset);
              if (inputCount != null) {
                inputCount[0]++;
              } else {
                contained.put(idxAndDocCount[0] + indexOffset,
                  new double[] { 1 });
              }
            } else {
              contained
                .put(idxAndDocCount[0] + indexOffset, new double[] { 1 });
            }
          }
        }
      }
    }

    // TF transform
    if (m_TFTransform) {
      for (Map.Entry<Integer, double[]> e : contained.entrySet()) {
        int index = e.getKey();
        if (index >= indexOffset) {
          double[] val = e.getValue();
          val[0] = Math.log(val[0] + 1);
        }
      }
    }

    // IDF transform
    if (m_IDFTransform) {
      for (Map.Entry<Integer, double[]> e : contained.entrySet()) {
        int index = e.getKey();
        if (index >= indexOffset) {
          double[] val = e.getValue();
          String word = m_outputFormat.attribute(index).name();
          word = word.substring(m_Prefix.length());
          int[] idxAndDocCount = m_consolidatedDict.get(word);
          if (idxAndDocCount == null) {
            throw new Exception("This should never occur");
          }
          if (idxAndDocCount.length != 2) {
            throw new Exception("Can't compute IDF transform as document "
              + "counts are not available");
          }
          val[0] = val[0] * Math.log(m_count / (double) idxAndDocCount[1]);
        }
      }
    }

    double[] values = new double[contained.size()];
    int[] indices = new int[contained.size()];
    int i = 0;
    for (Map.Entry<Integer, double[]> e : contained.entrySet()) {
      values[i] = e.getValue()[0];
      indices[i++] = e.getKey().intValue();
    }

    Instance inst =
      new SparseInstance(input.weight(), values, indices,
        m_outputFormat.numAttributes());
    inst.setDataset(m_outputFormat);

    if (m_normalize) {
      normalizeInstance(inst, indexOffset);
    }

    return inst;
  }

  /**
   * Normalizes given instance to average doc length (only the newly constructed
   * attributes).
   * 
   * @param inst the instance to normalize
   * @param offset index of the start of the dictionary attributes
   * @throws Exception if avg. doc length not set
   */
  private void normalizeInstance(Instance inst, int offset) throws Exception {
    if (m_avgDocLength <= 0) {
      throw new Exception("Average document length is not set!");
    }

    double docLength = 0;

    // compute length of document vector
    for (int i = 0; i < inst.numValues(); i++) {
      if (inst.index(i) >= offset
        && inst.index(i) != m_outputFormat.classIndex()) {
        docLength += inst.valueSparse(i) * inst.valueSparse(i);
      }
    }
    docLength = Math.sqrt(docLength);

    // normalized document vector
    for (int i = 0; i < inst.numValues(); i++) {
      if (inst.index(i) >= offset
        && inst.index(i) != m_outputFormat.classIndex()) {
        double val = inst.valueSparse(i) * m_avgDocLength / docLength;
        inst.setValueSparse(i, val);
        if (val == 0) {
          System.err.println("setting value " + inst.index(i) + " to zero.");
          i--;
        }
      }
    }
  }

  /**
   * Process an instance by tokenizing string attributes and updating the
   * dictionary.
   * 
   * @param inst the instance to process
   */
  public void processInstance(Instance inst) {

    if (!m_inputContainsStringAttributes) {
      return;
    }

    if (m_inputVector == null) {
      m_inputVector = new LinkedHashMap<String, int[]>();
    } else {
      m_inputVector.clear();
    }

    int dIndex = 0;
    if (!m_doNotOperateOnPerClassBasis && m_classIndex >= 0 && m_inputFormat.classAttribute().isNominal()) {
      if (!inst.classIsMissing()) {
        dIndex = (int) inst.classValue();
      } else {
        return; // skip missing class instances
      }
    }

    for (int j = 0; j < inst.numAttributes(); j++) {
      if (m_selectedRange.isInRange(j) && !inst.isMissing(j)) {
        m_tokenizer.tokenize(inst.stringValue(j));

        while (m_tokenizer.hasMoreElements()) {
          String word = m_tokenizer.nextElement();

          if (m_lowerCaseTokens) {
            word = word.toLowerCase();
          }
          word = m_stemmer.stem(word);
          if (m_stopwordsHandler.isStopword(word)) {
            continue;
          }

          int[] counts = m_inputVector.get(word);
          if (counts == null) {
            counts = new int[2];
            counts[0] = 1; // word count
            counts[1] = 1; // doc count
            m_inputVector.put(word, counts);
          } else {
            counts[0]++;
          }
        }
      }
    }

    // now update dictionary for the words that have
    // occurred in this instance (document)
    double docLength = 0;
    for (Map.Entry<String, int[]> e : m_inputVector.entrySet()) {
      int[] dictCounts = m_dictsPerClass[dIndex].get(e.getKey());
      if (dictCounts == null) {
        dictCounts = new int[2];
        m_dictsPerClass[dIndex].put(e.getKey(), dictCounts);
      }
      dictCounts[0] += e.getValue()[0];
      dictCounts[1] += e.getValue()[1];
      docLength += e.getValue()[0] * e.getValue()[0];
    }
    if (m_normalize) {
      // this is normalization based document length *before* final dictionary
      // pruning. DictionaryBuilder operates incrementally, so it is not
      // possible to normalize based on a final dictionary
      m_docLengthSum += Math.sqrt(docLength);
    }

    m_count++;

    pruneDictionary();
  }

  /**
   * Prunes the dictionary of low frequency terms
   */
  protected void pruneDictionary() {
    if (m_periodicPruneRate > 0 && m_count % m_periodicPruneRate == 0) {
      for (Map<String, int[]> m_dictsPerClas : m_dictsPerClass) {
        Iterator<Map.Entry<String, int[]>> entries =
          m_dictsPerClas.entrySet().iterator();
        while (entries.hasNext()) {
          Map.Entry<String, int[]> entry = entries.next();
          if (entry.getValue()[0] < m_minFrequency) {
            entries.remove();
          }
        }
      }
    }
  }

  /**
   * Clear the dictionary(s)
   */
  public void reset() {
    m_dictsPerClass = null;
    m_count = 0;
    m_docLengthSum = 0;
    m_avgDocLength = 0;
    m_inputFormat = null;
    m_outputFormat = null;
    m_consolidatedDict = null;
  }

  /**
   * Get the current dictionary(s) (one per class for nominal class, if set).
   * These are the dictionaries that are built/updated when processInstance() is
   * called. The finalized dictionary (used for vectorization) can be obtained
   * by calling finalizeDictionary() - this returns a consolidated (over
   * classes) and pruned final dictionary.
   * 
   * @param minFrequencyPrune prune the dictionaries of low frequency terms
   *          before returning them
   * @return the dictionaries
   */
  public Map<String, int[]>[] getDictionaries(boolean minFrequencyPrune)
    throws WekaException {

    if (m_dictsPerClass == null) {
      throw new WekaException("No dictionaries have been built yet!");
    }

    if (minFrequencyPrune) {
      pruneDictionary();
    }

    return m_dictsPerClass;
  }

  @Override
  public DictionaryBuilder aggregate(DictionaryBuilder toAgg) throws Exception {
    Map<String, int[]>[] toAggDicts = toAgg.getDictionaries(false);

    if (toAggDicts.length != m_dictsPerClass.length) {
      throw new Exception("Number of dictionaries from the builder to "
        + "be aggregated does not match our number of dictionaries");
    }

    // we assume that the order of class values is consistent
    for (int i = 0; i < toAggDicts.length; i++) {
      Map<String, int[]> toAggDictForClass = toAggDicts[i];
      for (Map.Entry<String, int[]> e : toAggDictForClass.entrySet()) {
        int[] ourCounts = m_dictsPerClass[i].get(e.getKey());
        if (ourCounts == null) {
          ourCounts = new int[2];
          m_dictsPerClass[i].put(e.getKey(), ourCounts);
        }
        ourCounts[0] += e.getValue()[0]; // word count
        ourCounts[1] += e.getValue()[1]; // doc count
      }
    }

    m_count += toAgg.m_count;
    m_docLengthSum += toAgg.m_docLengthSum;

    return this;
  }

  @Override
  public void finalizeAggregation() throws Exception {
    finalizeDictionary();
  }

  /**
   * Performs final pruning and consolidation according to the number of words
   * to keep property. Finalization is performed just once, subsequent calls to
   * this method return the finalized dictionary computed on the first call
   * (unless reset() has been called in between).
   * 
   * @return the consolidated and pruned final dictionary, or null if the input
   *         format did not contain any string attributes within the selected
   *         range to process
   * @throws Exception if a problem occurs
   */
  public Map<String, int[]> finalizeDictionary() throws Exception {

    if (!m_inputContainsStringAttributes) {
      return null;
    }

    // perform final pruning and consolidation
    // according to wordsToKeep
    if (m_consolidatedDict != null) {
      return m_consolidatedDict;
    }

    if (m_dictsPerClass == null) {
      System.err.println(this.hashCode());
      throw new WekaException("No dictionary built yet!");
    }

    int[] prune = new int[m_dictsPerClass.length];
    for (int z = 0; z < prune.length; z++) {
      int[] array = new int[m_dictsPerClass[z].size()];
      int index = 0;
      for (Map.Entry<String, int[]> e : m_dictsPerClass[z].entrySet()) {
        array[index++] = e.getValue()[0];
      }

      if (array.length < m_wordsToKeep) {
        prune[z] = m_minFrequency;
      } else {
        Arrays.sort(array);
        prune[z] =
          Math.max(m_minFrequency, array[array.length - m_wordsToKeep]);
      }
    }

    // now consolidate across classes
    Map<String, int[]> consolidated = new LinkedHashMap<String, int[]>();
    int index = 0;
    for (int z = 0; z < prune.length; z++) {
      for (Map.Entry<String, int[]> e : m_dictsPerClass[z].entrySet()) {
        if (e.getValue()[0] >= prune[z]) {
          int[] counts = consolidated.get(e.getKey());
          if (counts == null) {
            counts = new int[2];
            counts[0] = index++;
            consolidated.put(e.getKey(), counts);
          }
          // counts[0] += e.getValue()[0];
          counts[1] += e.getValue()[1];
        }
      }
    }

    m_consolidatedDict = consolidated;
    m_dictsPerClass = null;

    if (m_normalize) {
      m_avgDocLength = m_docLengthSum / m_count;
    }

    m_outputFormat = getVectorizedFormat();

    return m_consolidatedDict;
  }

  /**
   * Load a dictionary from a file
   * 
   * @param filename the file to load from
   * @param plainText true if the dictionary is in text format
   * @throws IOException if a problem occurs
   */
  public void loadDictionary(String filename, boolean plainText)
    throws IOException {
    loadDictionary(new File(filename), plainText);
  }

  /**
   * Load a dictionary from a file
   * 
   * @param toLoad the file to load from
   * @param plainText true if the dictionary is in text format
   * @throws IOException if a problem occurs
   */
  public void loadDictionary(File toLoad, boolean plainText) throws IOException {
    if (plainText) {
      loadDictionary(new FileReader(toLoad));
    } else {
      loadDictionary(new FileInputStream(toLoad));
    }
  }

  /**
   * Load a textual dictionary from a reader
   * 
   * @param reader the reader to read from
   * @throws IOException if a problem occurs
   */
  public void loadDictionary(Reader reader) throws IOException {
    BufferedReader br = new BufferedReader(reader);
    m_consolidatedDict = new LinkedHashMap<String, int[]>();

    try {
      String line = br.readLine();
      int index = 0;
      if (line != null) {
        if (line.startsWith("@@@") && line.endsWith("@@@")) {
          String avgS = line.replace("@@@", "");
          try {
            m_avgDocLength = Double.parseDouble(avgS);
          } catch (NumberFormatException ex) {
            System.err.println("Unable to parse average document length '"
              + avgS + "'");
          }
          line = br.readLine();
          if (line == null) {
            throw new IOException("Empty dictionary file!");
          }
        }

        boolean hasDocCounts = false;
        if (line.lastIndexOf(",") > 0) {
          String countS =
            line.substring(line.lastIndexOf(",") + 1, line.length()).trim();
          try {
            int dCount = Integer.parseInt(countS);
            hasDocCounts = true;
            int[] holder = new int[2];
            holder[1] = dCount;
            holder[0] = index++;
            m_consolidatedDict.put(line.substring(0, line.lastIndexOf(",")),
              holder);
          } catch (NumberFormatException ex) {
            // ignore quietly
          }
        }

        while ((line = br.readLine()) != null) {
          int[] holder = new int[hasDocCounts ? 2 : 1];
          holder[0] = index++;
          if (hasDocCounts) {
            String countS =
              line.substring(line.lastIndexOf(",") + 1, line.length()).trim();
            line = line.substring(0, line.lastIndexOf(","));
            try {
              int dCount = Integer.parseInt(countS);
              holder[1] = dCount;
            } catch (NumberFormatException e) {
              throw new IOException(e);
            }
          }
          m_consolidatedDict.put(line, holder);
        }
      } else {
        throw new IOException("Empty dictionary file!");
      }
    } finally {
      br.close();
    }
    try {
      m_outputFormat = getVectorizedFormat();
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  /**
   * Load a binary dictionary from an input stream
   * 
   * @param is the input stream to read from
   * @throws IOException if a problem occurs
   */
  @SuppressWarnings("unchecked")
  public void loadDictionary(InputStream is) throws IOException {
    ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(is));
    try {
      List<Object> holder = (List<Object>) ois.readObject();
      m_avgDocLength = (Double) holder.get(0);
      m_consolidatedDict = (Map<String, int[]>) holder.get(1);
    } catch (ClassNotFoundException ex) {
      throw new IOException(ex);
    } finally {
      ois.close();
    }
  }

  /**
   * Save the dictionary
   * 
   * @param filename the file to save to
   * @param plainText true if the dictionary should be saved in text format
   * @throws IOException if a problem occurs
   */
  public void saveDictionary(String filename, boolean plainText)
    throws IOException {
    saveDictionary(new File(filename), plainText);
  }

  /**
   * Save a dictionary
   * 
   * @param toSave the file to save to
   * @param plainText true if the dictionary should be saved in text format
   * @throws IOException if a problem occurs
   */
  public void saveDictionary(File toSave, boolean plainText) throws IOException {
    if (plainText) {
      saveDictionary(new FileWriter(toSave));
    } else {
      saveDictionary(new FileOutputStream(toSave));
    }
  }

  /**
   * Save the dictionary in textual format
   * 
   * @param writer the writer to write to
   * @throws IOException if a problem occurs
   */
  public void saveDictionary(Writer writer) throws IOException {
    if (!m_inputContainsStringAttributes) {
      throw new IOException("Input did not contain any string attributes!");
    }

    if (m_consolidatedDict == null) {
      throw new IOException("No dictionary to save!");
    }

    BufferedWriter br = new BufferedWriter(writer);
    try {
      if (m_avgDocLength > 0) {
        br.write("@@@" + m_avgDocLength + "@@@\n");
      }
      for (Map.Entry<String, int[]> e : m_consolidatedDict.entrySet()) {
        int[] v = e.getValue();
        br.write(e.getKey() + "," + (v.length > 1 ? v[1] : "") + "\n");
      }
    } finally {
      br.flush();
      br.close();
    }
  }

  /**
   * Save the dictionary in binary form
   * 
   * @param os the output stream to write to
   * @throws IOException if a problem occurs
   */
  public void saveDictionary(OutputStream os) throws IOException {

    if (!m_inputContainsStringAttributes) {
      throw new IOException("Input did not contain any string attributes!");
    }

    if (m_consolidatedDict == null) {
      throw new IOException("No dictionary to save!");
    }
    ObjectOutputStream oos =
      new ObjectOutputStream(new BufferedOutputStream(os));
    List<Object> holder = new ArrayList<Object>();
    holder.add(m_avgDocLength);
    holder.add(m_consolidatedDict);
    try {
      oos.writeObject(holder);
    } finally {
      oos.flush();
      oos.close();
    }
  }
}
