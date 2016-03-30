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
 *    DictionarySaver.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.converters;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import weka.core.Capabilities;
import weka.core.DictionaryBuilder;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionMetadata;
import weka.core.RevisionUtils;
import weka.core.stemmers.NullStemmer;
import weka.core.stemmers.Stemmer;
import weka.core.stopwords.Null;
import weka.core.stopwords.StopwordsHandler;
import weka.core.tokenizers.Tokenizer;

/**
 * <!-- globalinfo-start --> Writes a dictionary constructed from string
 * attributes in incoming instances to a destination. <br>
 * <br>
 * <!-- globalinfo-end -->
 *
 * <!-- options-start --> Valid options are:
 * <p>
 * 
 * <pre>
 * -binary-dict
 *  Save as a binary serialized dictionary
 * </pre>
 * 
 * <pre>
 * -R &lt;range&gt;
 *  Specify range of attributes to act on. This is a comma separated list of attribute
 *  indices, with "first" and "last" valid values.
 * </pre>
 * 
 * <pre>
 * -V
 *  Set attributes selection mode. If false, only selected attributes in the range will
 *  be worked on. If true, only non-selected attributes will be processed
 * </pre>
 * 
 * <pre>
 * -L
 *  Convert all tokens to lowercase when matching against dictionary entries.
 * </pre>
 * 
 * <pre>
 * -stemmer &lt;spec&gt;
 *  The stemming algorithm (classname plus parameters) to use.
 * </pre>
 * 
 * <pre>
 * -stopwords-handler &lt;spec&gt;
 *  The stopwords handler to use (default = Null)
 * </pre>
 * 
 * <pre>
 * -tokenizer &lt;spec&gt;
 *  The tokenizing algorithm (classname plus parameters) to use.
 *  (default: weka.core.tokenizers.WordTokenizer)
 * </pre>
 * 
 * <pre>
 * -P &lt;integer&gt;
 *  Prune the dictionary every x instances
 *  (default = 0 - i.e. no periodic pruning)
 * </pre>
 * 
 * <pre>
 * -W &lt;integer&gt;
 *  The number of words (per class if there is a class attribute assigned) to attempt to keep.
 * </pre>
 * 
 * <pre>
 * -M &lt;integer&gt;
 *  The minimum term frequency to use when pruning the dictionary
 *  (default = 1).
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
 * -sort
 *  Sort the dictionary alphabetically
 * </pre>
 * 
 * <pre>
 * -i &lt;the input file&gt;
 *  The input file
 * </pre>
 * 
 * <pre>
 * -o &lt;the output file&gt;
 *  The output file
 * </pre>
 * 
 * <!-- options-end -->
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class DictionarySaver extends AbstractFileSaver implements
  BatchConverter, IncrementalConverter {

  private static final long serialVersionUID = -19891905988830722L;

  protected transient OutputStream m_binaryStream;

  /**
   * The dictionary builder to use
   */
  protected DictionaryBuilder m_dictionaryBuilder = new DictionaryBuilder();

  /**
   * Whether the dictionary file contains a binary serialized dictionary, rather
   * than plain text
   */
  protected boolean m_dictionaryIsBinary;

  /**
   * Prune the dictionary every x instances. <=0 means no periodic pruning
   */
  private long m_periodicPruningRate;

  public DictionarySaver() {
    resetOptions();
  }

  /**
   * Returns a string describing this Saver.
   *
   * @return a description of the Saver suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "Writes a dictionary constructed from string attributes in "
      + "incoming instances to a destination.";
  }

  /**
   * Set whether to save the dictionary as a binary serialized dictionary,
   * rather than a plain text one
   *
   * @param binary true if the dictionary is to be saved as binary rather than
   *          plain text
   */
  @OptionMetadata(displayName = "Save dictionary in binary form",
    description = "Save as a binary serialized dictionary",
    commandLineParamName = "binary-dict",
    commandLineParamSynopsis = "-binary-dict", commandLineParamIsFlag = true,
    displayOrder = 2)
  public void setSaveBinaryDictionary(boolean binary) {
    m_dictionaryIsBinary = binary;
  }

  /**
   * Get whether to save the dictionary as a binary serialized dictionary,
   * rather than a plain text one
   *
   * @return true if the dictionary is to be saved as binary rather than plain
   *         text
   */
  public boolean getSaveBinaryDictionary() {
    return m_dictionaryIsBinary;
  }

  /**
   * Gets the current range selection.
   *
   * @return a string containing a comma separated list of ranges
   */
  public String getAttributeIndices() {
    return m_dictionaryBuilder.getAttributeIndices();
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
      + "\"last\" valid values.", commandLineParamName = "R",
    commandLineParamSynopsis = "-R <range>", displayOrder = 4)
  public void setAttributeIndices(String rangeList) {
    m_dictionaryBuilder.setAttributeIndices(rangeList);
  }

  /**
   * Gets whether the supplied columns are to be processed or skipped.
   *
   * @return true if the supplied columns will be kept
   */
  public boolean getInvertSelection() {
    return m_dictionaryBuilder.getInvertSelection();
  }

  /**
   * Sets whether selected columns should be processed or skipped.
   *
   * @param invert the new invert setting
   */
  @OptionMetadata(
    displayName = "Invert selection",
    description = "Set attributes selection mode. "
      + "If false, only selected attributes in the range will\nbe worked on. If true, "
      + "only non-selected attributes will be processed",
    commandLineParamName = "V", commandLineParamSynopsis = "-V",
    commandLineParamIsFlag = true, displayOrder = 5)
  public
    void setInvertSelection(boolean invert) {
    m_dictionaryBuilder.setInvertSelection(invert);
  }

  /**
   * Gets whether if the tokens are to be downcased or not.
   *
   * @return true if the tokens are to be downcased.
   */
  public boolean getLowerCaseTokens() {
    return m_dictionaryBuilder.getLowerCaseTokens();
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
      + "dictionary entries.", commandLineParamName = "L",
    commandLineParamSynopsis = "-L", commandLineParamIsFlag = true,
    displayOrder = 10)
  public void setLowerCaseTokens(boolean downCaseTokens) {
    m_dictionaryBuilder.setLowerCaseTokens(downCaseTokens);
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
      m_dictionaryBuilder.setStemmer(value);
    } else {
      m_dictionaryBuilder.setStemmer(new NullStemmer());
    }
  }

  /**
   * Returns the current stemming algorithm, null if none is used.
   *
   * @return the current stemming algorithm, null if none set
   */
  public Stemmer getStemmer() {
    return m_dictionaryBuilder.getStemmer();
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
      m_dictionaryBuilder.setStopwordsHandler(value);
    } else {
      m_dictionaryBuilder.setStopwordsHandler(new Null());
    }
  }

  /**
   * Gets the stopwords handler.
   *
   * @return the stopwords handler
   */
  public StopwordsHandler getStopwordsHandler() {
    return m_dictionaryBuilder.getStopwordsHandler();
  }

  /**
   * the tokenizer algorithm to use.
   *
   * @param value the configured tokenizing algorithm
   */
  @OptionMetadata(
    displayName = "Tokenizer",
    description = "The tokenizing algorithm (classname plus parameters) to use.\n"
      + "(default: weka.core.tokenizers.WordTokenizer)",
    commandLineParamName = "tokenizer",
    commandLineParamSynopsis = "-tokenizer <spec>", displayOrder = 13)
  public
    void setTokenizer(Tokenizer value) {
    m_dictionaryBuilder.setTokenizer(value);
  }

  /**
   * Returns the current tokenizer algorithm.
   *
   * @return the current tokenizer algorithm
   */
  public Tokenizer getTokenizer() {
    return m_dictionaryBuilder.getTokenizer();
  }

  /**
   * Gets the rate at which the dictionary is periodically pruned, as a
   * percentage of the dataset size.
   *
   * @return the rate at which the dictionary is periodically pruned
   */
  public long getPeriodicPruning() {
    return m_periodicPruningRate;
  }

  /**
   * Sets the rate at which the dictionary is periodically pruned, as a
   * percentage of the dataset size.
   *
   * @param newPeriodicPruning the rate at which the dictionary is periodically
   *          pruned
   */
  @OptionMetadata(
    displayName = "Periodic pruning rate",
    description = "Prune the "
      + "dictionary every x instances\n(default = 0 - i.e. no periodic pruning)",
    commandLineParamName = "P", commandLineParamSynopsis = "-P <integer>",
    displayOrder = 14)
  public
    void setPeriodicPruning(long newPeriodicPruning) {
    m_periodicPruningRate = newPeriodicPruning;
  }

  /**
   * Gets the number of words (per class if there is a class attribute assigned)
   * to attempt to keep.
   *
   * @return the target number of words in the output vector (per class if
   *         assigned).
   */
  public int getWordsToKeep() {
    return m_dictionaryBuilder.getWordsToKeep();
  }

  /**
   * Sets the number of words (per class if there is a class attribute assigned)
   * to attempt to keep.
   *
   * @param newWordsToKeep the target number of words in the output vector (per
   *          class if assigned).
   */
  @OptionMetadata(
    displayName = "Number of words to attempt to keep",
    description = "The number of words (per class if there is a class attribute "
      + "assigned) to attempt to keep.", commandLineParamName = "W",
    commandLineParamSynopsis = "-W <integer>", displayOrder = 15)
  public
    void setWordsToKeep(int newWordsToKeep) {
    m_dictionaryBuilder.setWordsToKeep(newWordsToKeep);
  }

  /**
   * Get the MinTermFreq value.
   *
   * @return the MinTermFreq value.
   */
  public int getMinTermFreq() {
    return m_dictionaryBuilder.getMinTermFreq();
  }

  /**
   * Set the MinTermFreq value.
   *
   * @param newMinTermFreq The new MinTermFreq value.
   */
  @OptionMetadata(
    displayName = "Minimum term frequency",
    description = "The minimum term frequency to use when pruning the dictionary\n"
      + "(default = 1).", commandLineParamName = "M",
    commandLineParamSynopsis = "-M <integer>", displayOrder = 16)
  public
    void setMinTermFreq(int newMinTermFreq) {
    m_dictionaryBuilder.setMinTermFreq(newMinTermFreq);
  }

  /**
   * Get the DoNotOperateOnPerClassBasis value.
   *
   * @return the DoNotOperateOnPerClassBasis value.
   */
  public boolean getDoNotOperateOnPerClassBasis() {
    return m_dictionaryBuilder.getDoNotOperateOnPerClassBasis();
  }

  /**
   * Set the DoNotOperateOnPerClassBasis value.
   *
   * @param newDoNotOperateOnPerClassBasis The new DoNotOperateOnPerClassBasis
   *          value.
   */
  @OptionMetadata(displayName = "Do not operate on a per-class basis",
    description = "If this is set, the maximum number of words and the\n"
      + "minimum term frequency is not enforced on a per-class\n"
      + "basis but based on the documents in all the classes\n"
      + "(even if a class attribute is set).", commandLineParamName = "O",
    commandLineParamSynopsis = "-O", commandLineParamIsFlag = true,
    displayOrder = 17)
  public void setDoNotOperateOnPerClassBasis(
    boolean newDoNotOperateOnPerClassBasis) {
    m_dictionaryBuilder
      .setDoNotOperateOnPerClassBasis(newDoNotOperateOnPerClassBasis);
  }

  /**
   * Set whether to keep the dictionary sorted alphabetically or not
   *
   * @param sorted true to keep the dictionary sorted
   */
  @OptionMetadata(displayName = "Sort dictionary",
    description = "Sort the dictionary alphabetically",
    commandLineParamName = "sort", commandLineParamSynopsis = "-sort",
    commandLineParamIsFlag = true, displayOrder = 18)
  public void setKeepDictionarySorted(boolean sorted) {
    m_dictionaryBuilder.setSortDictionary(sorted);
  }

  /**
   * Get whether to keep the dictionary sorted alphabetically or not
   *
   * @return true to keep the dictionary sorted
   */
  public boolean getKeepDictionarySorted() {
    return m_dictionaryBuilder.getSortDictionary();
  }

  /**
   * Returns the Capabilities of this saver.
   *
   * @return the capabilities of this object
   * @see Capabilities
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capabilities.Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capabilities.Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capabilities.Capability.DATE_ATTRIBUTES);
    result.enable(Capabilities.Capability.STRING_ATTRIBUTES);
    result.enable(Capabilities.Capability.MISSING_VALUES);

    // class
    result.enable(Capabilities.Capability.NOMINAL_CLASS);
    result.enable(Capabilities.Capability.NUMERIC_CLASS);
    result.enable(Capabilities.Capability.DATE_CLASS);
    result.enable(Capabilities.Capability.STRING_CLASS);
    result.enable(Capabilities.Capability.MISSING_CLASS_VALUES);
    result.enable(Capabilities.Capability.NO_CLASS);

    return result;
  }

  @Override
  public String getFileDescription() {
    return "Plain text or binary serialized dictionary files created from text "
      + "in string attributes";
  }

  @Override
  public void writeIncremental(Instance inst) throws IOException {
    int writeMode = getWriteMode();
    Instances structure = getInstances();

    if (getRetrieval() == BATCH || getRetrieval() == NONE) {
      throw new IOException("Batch and incremental saving cannot be mixed.");
    }

    if (writeMode == WAIT) {
      if (structure == null) {
        setWriteMode(CANCEL);
        if (inst != null) {
          throw new IOException("Structure (header Information) has to be set "
            + "in advance");
        }
      } else {
        setWriteMode(STRUCTURE_READY);
      }
      writeMode = getWriteMode();
    }
    if (writeMode == CANCEL) {
      cancel();
    }

    if (writeMode == STRUCTURE_READY) {
      m_dictionaryBuilder.reset();
      try {
        m_dictionaryBuilder.setup(structure);
      } catch (Exception ex) {
        throw new IOException(ex);
      }
      setWriteMode(WRITE);
      writeMode = getWriteMode();
    }

    if (writeMode == WRITE) {
      if (structure == null) {
        throw new IOException("No instances information available.");
      }

      if (inst != null) {
        m_dictionaryBuilder.processInstance(inst);
      } else {
        try {
          m_dictionaryBuilder.finalizeDictionary();
        } catch (Exception e) {
          throw new IOException(e);
        }
        if (retrieveFile() == null && getWriter() == null) {
          if (getSaveBinaryDictionary()) {
            throw new IOException(
              "Can't output binary dictionary to standard out!");
          }
          m_dictionaryBuilder.saveDictionary(System.out);
        } else {
          if (getSaveBinaryDictionary()) {
            m_dictionaryBuilder.saveDictionary(m_binaryStream);
          } else {
            m_dictionaryBuilder.saveDictionary(getWriter());
          }
        }

        resetStructure();
        resetWriter();
      }
    }
  }

  @Override
  public void writeBatch() throws IOException {
    if (getInstances() == null) {
      throw new IOException("No instances to save");
    }
    if (getRetrieval() == INCREMENTAL) {
      throw new IOException("Batch and incremental saving cannot be mixed.");
    }
    setRetrieval(BATCH);
    setWriteMode(WRITE);

    m_dictionaryBuilder.reset();
    try {
      m_dictionaryBuilder.setup(getInstances());
    } catch (Exception ex) {
      throw new IOException(ex);
    }

    for (int i = 0; i < getInstances().numInstances(); i++) {
      m_dictionaryBuilder.processInstance(getInstances().instance(i));
    }
    try {
      m_dictionaryBuilder.finalizeDictionary();
    } catch (Exception ex) {
      throw new IOException(ex);
    }

    if (retrieveFile() == null && getWriter() == null) {
      if (getSaveBinaryDictionary()) {
        throw new IOException("Can't output binary dictionary to standard out!");
      }

      m_dictionaryBuilder.saveDictionary(System.out);
      setWriteMode(WAIT);
      return;
    }

    if (getSaveBinaryDictionary()) {
      m_dictionaryBuilder.saveDictionary(m_binaryStream);
    } else {
      m_dictionaryBuilder.saveDictionary(getWriter());
    }
    setWriteMode(WAIT);
    resetWriter();
    setWriteMode(CANCEL);
  }

  @Override
  public void resetOptions() {
    super.resetOptions();
    setFileExtension(".dict");
  }

  @Override
  public void resetWriter() {
    super.resetWriter();

    m_binaryStream = null;
  }

  @Override
  public void setDestination(OutputStream output) throws IOException {
    super.setDestination(output);
    m_binaryStream = new BufferedOutputStream(output);
  }

  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  public static void main(String[] args) {
    runFileSaver(new DictionarySaver(), args);
  }
}
