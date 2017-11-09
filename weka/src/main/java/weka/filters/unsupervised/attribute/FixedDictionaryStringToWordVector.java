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
 *    FixedDictionaryStringToWordVector.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.unsupervised.attribute;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.swing.JFileChooser;

import weka.core.*;
import weka.core.stemmers.NullStemmer;
import weka.core.stemmers.Stemmer;
import weka.core.stopwords.Null;
import weka.core.stopwords.StopwordsHandler;
import weka.core.tokenizers.Tokenizer;
import weka.filters.SimpleStreamFilter;
import weka.filters.UnsupervisedFilter;
import weka.gui.FilePropertyMetadata;

/**
 * <!-- globalinfo-start --> Converts String attributes into a set of attributes
 * representing word occurrence (depending on the tokenizer) information from
 * the text contained in the strings. The set of words (attributes) is taken
 * from a user-supplied dictionary, either in plain text form or as a serialized
 * java object. <br>
 * <br>
 * <!-- globalinfo-end -->
 *
 * <!-- options-start --> Valid options are:
 * <p>
 * 
 * <pre>
 *  -dictionary &lt;path to dictionary file&gt;
 *  The path to the dictionary to use
 * </pre>
 * 
 * <pre>
 *  -binary-dict
 *  Dictionary file contains a binary serialized dictionary
 * </pre>
 * 
 * <pre>
 *  -C
 *  Output word counts rather than boolean 0 or 1 (indicating presence or absence of a word
 * </pre>
 * 
 * <pre>
 *  -R &lt;range&gt;
 *  Specify range of attributes to act on. This is a comma separated list of attribute
 *  indices, with "first" and "last" valid values.
 * </pre>
 * 
 * <pre>
 *  -V
 *  Set attributes selection mode. If false, only selected attributes in the range will
 *  be worked on. If true, only non-selected attributes will be processed
 * </pre>
 * 
 * <pre>
 *  -P &lt;attribute name prefix&gt;
 *  Specify a prefix for the created attribute names (default: "")
 * </pre>
 * 
 * <pre>
 *  -T
 *  Set whether the word frequencies should be transformed into
 *  log(1+fij), where fij is the frequency of word i in document (instance) j.
 * </pre>
 * 
 * <pre>
 *  -I
 *  Set whether the word frequencies in a document should be transformed into
 *  fij*log(num of Docs/num of docs with word i), where fij is the frequency
 *  of word i in document (instance) j.
 * </pre>
 * 
 * <pre>
 *  -N
 *  Whether to normalize to average length of documents seen during dictionary construction
 * </pre>
 * 
 * <pre>
 *  -L
 *  Convert all tokens to lowercase when matching against dictionary entries.
 * </pre>
 * 
 * <pre>
 *  -stemmer &lt;spec&gt;
 *  The stemming algorithm (classname plus parameters) to use.
 * </pre>
 * 
 * <pre>
 *  -stopwords-handler &lt;spec&gt;
 *  The stopwords handler to use (default = Null)
 * </pre>
 * 
 * <pre>
 *  -tokenizer &lt;spec&gt;
 *  The tokenizing algorithm (classname plus parameters) to use.
 *  (default: weka.core.tokenizers.WordTokenizer)
 * </pre>
 * 
 * <pre>
 *  -output-debug-info
 *  If set, filter is run in debug mode and
 *  may output additional info to the console
 * </pre>
 * 
 * <pre>
 *  -do-not-check-capabilities
 *  If set, filter capabilities are not checked before filter is built
 *  (use with caution).
 * </pre>
 * 
 * <!-- options-end -->
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class FixedDictionaryStringToWordVector extends SimpleStreamFilter
  implements UnsupervisedFilter, EnvironmentHandler, WeightedInstancesHandler {

  private static final long serialVersionUID = 7990892846966916757L;

  protected DictionaryBuilder m_vectorizer = new DictionaryBuilder();

  protected File m_dictionaryFile = new File("-- set me --");

  /** Source stream to read a binary serialized dictionary from */
  protected transient InputStream m_dictionarySource;

  /** Source reader to read a textual dictionary from */
  protected transient Reader m_textDictionarySource;

  /**
   * Whether the dictionary file contains a binary serialized dictionary, rather
   * than plain text - used when loading from a file in order to differentiate
   */
  protected boolean m_dictionaryIsBinary;

  protected transient Environment m_env = Environment.getSystemWide();

  /**
   * Returns the Capabilities of this filter.
   *
   * @return the capabilities of this object
   * @see Capabilities
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enableAllAttributes();
    result.enable(Capabilities.Capability.MISSING_VALUES);

    // class
    result.enableAllClasses();
    result.enable(Capabilities.Capability.MISSING_CLASS_VALUES);
    result.enable(Capabilities.Capability.NO_CLASS);

    return result;
  }

  /**
   * Get the dictionary builder used to manage the dictionary and perform the
   * actual vectorization
   *
   * @return the DictionaryBuilder in use
   */
  public DictionaryBuilder getDictionaryHandler() {
    return m_vectorizer;

  }

  /**
   * Set an input stream to load a binary serialized dictionary from, rather
   * than source it from a file
   *
   * @param source the input stream to read the dictionary from
   */
  public void setDictionarySource(InputStream source) {
    m_dictionarySource = source;
  }

  /**
   * Set an input reader to load a textual dictionary from, rather than source
   * it from a file
   *
   * @param source the input reader to read the dictionary from
   */
  public void setDictionarySource(Reader source) {
    m_textDictionarySource = source;
  }

  /**
   * Set the dictionary file to read from
   * 
   * @param file the file to read from
   */
  @OptionMetadata(displayName = "Dictionary file",
    description = "The path to the dictionary to use",
    commandLineParamName = "dictionary",
    commandLineParamSynopsis = "-dictionary <path to dictionary file>",
    displayOrder = 1)
  @FilePropertyMetadata(fileChooserDialogType = JFileChooser.OPEN_DIALOG,
    directoriesOnly = false)
  public void setDictionaryFile(File file) {
    m_dictionaryFile = file;
  }

  /**
   * Get the dictionary file to read from
   * 
   * @return the dictionary file to read from
   */
  public File getDictionaryFile() {
    return m_dictionaryFile;
  }

  /**
   * Set whether the dictionary file contains a binary serialized dictionary,
   * rather than a plain text one
   * 
   * @param binary true if the dictionary is a binary serialized one
   */
  @OptionMetadata(displayName = "Dictionary is binary",
    description = "Dictionary file contains a binary serialized dictionary",
    commandLineParamName = "binary-dict",
    commandLineParamSynopsis = "-binary-dict", commandLineParamIsFlag = true,
    displayOrder = 2)
  public void setDictionaryIsBinary(boolean binary) {
    m_dictionaryIsBinary = binary;
  }

  public boolean getDictionaryIsBinary() {
    return m_dictionaryIsBinary;
  }

  /**
   * Gets whether output instances contain 0 or 1 indicating word presence, or
   * word counts.
   *
   * @return true if word counts should be output.
   */
  public boolean getOutputWordCounts() {
    return m_vectorizer.getOutputWordCounts();
  }

  /**
   * Sets whether output instances contain 0 or 1 indicating word presence, or
   * word counts.
   *
   * @param outputWordCounts true if word counts should be output.
   */
  @OptionMetadata(displayName = "Output word counts",
    description = "Output word counts rather than boolean 0 or 1 (indicating presence or absence of "
      + "a word",
    commandLineParamName = "C", commandLineParamSynopsis = "-C",
    commandLineParamIsFlag = true, displayOrder = 3)
  public void setOutputWordCounts(boolean outputWordCounts) {
    m_vectorizer.setOutputWordCounts(outputWordCounts);
  }

  /**
   * Gets the current range selection.
   *
   * @return a string containing a comma separated list of ranges
   */
  public String getAttributeIndices() {
    return m_vectorizer.getAttributeIndices();
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
  @OptionMetadata(displayName = "Range of attributes to operate on",
    description = "Specify range of attributes to act on. This is a comma "
      + "separated list of attribute\nindices, with \"first\" and "
      + "\"last\" valid values.",
    commandLineParamName = "R", commandLineParamSynopsis = "-R <range>",
    displayOrder = 4)
  public void setAttributeIndices(String rangeList) {
    m_vectorizer.setAttributeIndices(rangeList);
  }

  /**
   * Gets whether the supplied columns are to be processed or skipped.
   *
   * @return true if the supplied columns will be kept
   */
  public boolean getInvertSelection() {
    return m_vectorizer.getInvertSelection();
  }

  /**
   * Sets whether selected columns should be processed or skipped.
   *
   * @param invert the new invert setting
   */
  @OptionMetadata(displayName = "Invert selection",
    description = "Set attributes selection mode. "
      + "If false, only selected attributes in the range will\nbe worked on. If true, "
      + "only non-selected attributes will be processed",
    commandLineParamName = "V", commandLineParamSynopsis = "-V",
    commandLineParamIsFlag = true, displayOrder = 5)
  public void setInvertSelection(boolean invert) {
    m_vectorizer.setInvertSelection(invert);
  }

  /**
   * Get the attribute name prefix.
   *
   * @return The current attribute name prefix.
   */
  public String getAttributeNamePrefix() {
    return m_vectorizer.getAttributeNamePrefix();
  }

  /**
   * Set the attribute name prefix.
   *
   * @param newPrefix String to use as the attribute name prefix.
   */
  @OptionMetadata(displayName = "Prefix for created attribute names",
    description = "Specify a prefix for the created attribute names "
      + "(default: \"\")",
    commandLineParamName = "P",
    commandLineParamSynopsis = "-P <attribute name prefix>", displayOrder = 6)
  public void setAttributeNamePrefix(String newPrefix) {
    m_vectorizer.setAttributeNamePrefix(newPrefix);
  }

  /**
   * Gets whether if the word frequencies should be transformed into log(1+fij)
   * where fij is the frequency of word i in document(instance) j.
   *
   * @return true if word frequencies are to be transformed.
   */
  public boolean getTFTransform() {
    return m_vectorizer.getTFTransform();
  }

  /**
   * Sets whether if the word frequencies should be transformed into log(1+fij)
   * where fij is the frequency of word i in document(instance) j.
   *
   * @param TFTransform true if word frequencies are to be transformed.
   */
  @OptionMetadata(displayName = "TFT transform",
    description = "Set whether the word frequencies should be transformed into\n"
      + "log(1+fij), where fij is the frequency of word i in document (instance) "
      + "j.",
    commandLineParamName = "T", commandLineParamSynopsis = "-T",
    displayOrder = 7)
  public void setTFTransform(boolean TFTransform) {
    m_vectorizer.setTFTransform(TFTransform);
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
    return m_vectorizer.getIDFTransform();
  }

  /**
   * Sets whether if the word frequencies in a document should be transformed
   * into: <br>
   * fij*log(num of Docs/num of Docs with word i) <br>
   * where fij is the frequency of word i in document(instance) j.
   *
   * @param IDFTransform true if the word frequecies are to be transformed
   */
  @OptionMetadata(displayName = "IDF transform",
    description = "Set whether the word frequencies in a document should be "
      + "transformed into\nfij*log(num of Docs/num of docs with word i), "
      + "where fij is the frequency\nof word i in document (instance) j.",
    commandLineParamName = "I", commandLineParamSynopsis = "-I",
    displayOrder = 8)
  public void setIDFTransform(boolean IDFTransform) {
    m_vectorizer.setIDFTransform(IDFTransform);
  }

  /**
   * Sets whether if the word frequencies for a document (instance) should be
   * normalized or not.
   *
   * @param normalize the new type.
   */
  @OptionMetadata(displayName = "Normalize word frequencies",
    description = "Whether to normalize to average length of documents seen "
      + "during dictionary construction",
    commandLineParamName = "N", commandLineParamSynopsis = "-N",
    commandLineParamIsFlag = true, displayOrder = 9)
  public void setNormalizeDocLength(boolean normalize) {
    m_vectorizer.setNormalize(normalize);
  }

  /**
   * Gets whether if the word frequencies for a document (instance) should be
   * normalized or not.
   *
   * @return true if word frequencies are to be normalized.
   */
  public boolean getNormalizeDocLength() {
    return m_vectorizer.getNormalize();
  }

  /**
   * Gets whether if the tokens are to be downcased or not.
   *
   * @return true if the tokens are to be downcased.
   */
  public boolean getLowerCaseTokens() {
    return m_vectorizer.getLowerCaseTokens();
  }

  /**
   * Sets whether if the tokens are to be downcased or not. (Doesn't affect
   * non-alphabetic characters in tokens).
   *
   * @param downCaseTokens should be true if only lower case tokens are to be
   *          formed.
   */
  @OptionMetadata(displayName = "Lower case tokens",
    description = "Convert all tokens to lowercase when matching against "
      + "dictionary entries.",
    commandLineParamName = "L", commandLineParamSynopsis = "-L",
    commandLineParamIsFlag = true, displayOrder = 10)
  public void setLowerCaseTokens(boolean downCaseTokens) {
    m_vectorizer.setLowerCaseTokens(downCaseTokens);
  }

  /**
   * the stemming algorithm to use, null means no stemming at all (i.e., the
   * NullStemmer is used).
   *
   * @param value the configured stemming algorithm, or null
   * @see NullStemmer
   */
  @OptionMetadata(displayName = "Stemmer to use",
    description = "The stemming algorithm (classname plus parameters) to use.",
    commandLineParamName = "stemmer",
    commandLineParamSynopsis = "-stemmer <spec>", displayOrder = 11)
  public void setStemmer(Stemmer value) {
    if (value != null) {
      m_vectorizer.setStemmer(value);
    } else {
      m_vectorizer.setStemmer(new NullStemmer());
    }
  }

  /**
   * Returns the current stemming algorithm, null if none is used.
   *
   * @return the current stemming algorithm, null if none set
   */
  public Stemmer getStemmer() {
    return m_vectorizer.getStemmer();
  }

  /**
   * Sets the stopwords handler to use.
   *
   * @param value the stopwords handler, if null, Null is used
   */
  @OptionMetadata(displayName = "Stop words handler",
    description = "The stopwords handler to use (default = Null)",
    commandLineParamName = "stopwords-handler",
    commandLineParamSynopsis = "-stopwords-handler <spec>", displayOrder = 12)
  public void setStopwordsHandler(StopwordsHandler value) {
    if (value != null) {
      m_vectorizer.setStopwordsHandler(value);
    } else {
      m_vectorizer.setStopwordsHandler(new Null());
    }
  }

  /**
   * Gets the stopwords handler.
   *
   * @return the stopwords handler
   */
  public StopwordsHandler getStopwordsHandler() {
    return m_vectorizer.getStopwordsHandler();
  }

  /**
   * the tokenizer algorithm to use.
   *
   * @param value the configured tokenizing algorithm
   */
  @OptionMetadata(displayName = "Tokenizer",
    description = "The tokenizing algorithm (classname plus parameters) to use.\n"
      + "(default: weka.core.tokenizers.WordTokenizer)",
    commandLineParamName = "tokenizer",
    commandLineParamSynopsis = "-tokenizer <spec>", displayOrder = 13)
  public void setTokenizer(Tokenizer value) {
    m_vectorizer.setTokenizer(value);
  }

  /**
   * Returns the current tokenizer algorithm.
   *
   * @return the current tokenizer algorithm
   */
  public Tokenizer getTokenizer() {
    return m_vectorizer.getTokenizer();
  }

  @Override
  public String globalInfo() {
    return "Converts String attributes into a set of attributes representing "
      + "word occurrence (depending on the tokenizer) information from the "
      + "text contained in the strings. The set of words (attributes) is "
      + "taken from a user-supplied dictionary, either in plain text form or "
      + "as a serialized java object.";
  }

  @Override
  protected Instances determineOutputFormat(Instances inputFormat)
    throws Exception {

    if (m_vectorizer.readyToVectorize()
      && inputFormat.equalHeaders(m_vectorizer.getInputFormat())) {
      return m_vectorizer.getVectorizedFormat();
    }

    m_vectorizer.reset();
    m_vectorizer.setup(inputFormat);

    if (m_dictionaryFile == null && m_dictionarySource == null
      && m_textDictionarySource == null) {
      throw new IOException("No dictionary file/source specified!");
    }

    if (m_dictionarySource != null) {
      m_vectorizer.loadDictionary(m_dictionarySource);
    } else if (m_textDictionarySource != null) {
      m_vectorizer.loadDictionary(m_textDictionarySource);
    } else {

      String fString = m_dictionaryFile.toString();
      if (fString.length() == 0) {
        throw new IOException("No dictionary file specified!");
      }
      try {
        fString = m_env.substitute(fString);
      } catch (Exception ex) {
        //
      }
      File dictFile = new File(fString);
      if (!dictFile.exists()) {
        throw new IOException("Specified dictionary file '" + fString
          + "' does not seem to exist!");
      }
      m_vectorizer.loadDictionary(dictFile, !m_dictionaryIsBinary);
    }

    return m_vectorizer.getVectorizedFormat();
  }

  @Override
  protected Instance process(Instance instance) throws Exception {
    return m_vectorizer.vectorizeInstance(instance);
  }

  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
  }

  public static void main(String[] args) {
    runFilter(new FixedDictionaryStringToWordVector(), args);
  }
}
