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
 *    StringStats
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.stats;

import java.io.Serializable;
import java.util.ArrayList;

import weka.core.Attribute;
import weka.core.Utils;
import weka.core.tokenizers.WordTokenizer;
import weka.distributed.CSVToARFFHeaderMapTask;
import distributed.core.DistributedJobConfig;

/**
 * Class for computing string-related stats. Computes counts, means, min max
 * etc. for string length and word count
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * 
 */
public class StringStats extends Stats implements Serializable {

  /** ID prefix for string length stats */
  protected static final String STRLEN_PREFIX = "strlen_";

  /** ID prefix for word count stats */
  protected static final String WORDC_PREFIX = "wordc_";

  /** For serialization */
  private static final long serialVersionUID = -743216387263285114L;

  /** The NumericStats to keep track of string length stats */
  protected NumericStats m_stringLengthStats;

  /** The NumericStats to keep track of word count stats */
  protected NumericStats m_wordStats;

  /** The tokenizer to use */
  protected StringStats.CountWordTokenizer m_tokenizer =
    new CountWordTokenizer();

  /** The count of missing values for this field */
  protected double m_missingCount;

  /**
   * Constructs a new StringStats
   * 
   * @param attributeName the name of the attribute/field to collect stats for
   */
  public StringStats(String attributeName) {
    super(attributeName);

    m_stringLengthStats = new NumericStats(attributeName);
    m_wordStats = new NumericStats(attributeName);
  }

  /**
   * Get the underlying NumericStats object that is tracking string length stats
   * 
   * @return the string length stats
   */
  public NumericStats getStringLengthStats() {
    return m_stringLengthStats;
  }

  /**
   * Get the underlying NumericStats object that is tracking word count stats
   * 
   * @return the word count stats
   */
  public NumericStats getWordCountStats() {
    return m_wordStats;
  }

  @Override
  public Attribute makeAttribute() {

    ArrayList<String> vals = new ArrayList<String>();
    Attribute stringStats = m_stringLengthStats.makeAttribute();
    Attribute wordStats = m_wordStats.makeAttribute();

    for (int i = 0; i < stringStats.numValues(); i++) {
      vals.add(STRLEN_PREFIX + stringStats.value(i));
    }

    for (int i = 0; i < wordStats.numValues(); i++) {
      vals.add(WORDC_PREFIX + wordStats.value(i));
    }

    Attribute a =
      new Attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
        + m_attributeName, vals);

    return a;
  }

  /**
   * Update with a new value
   * 
   * @param value the string value to update with
   * @param weight the weight for the update
   */
  public void update(String value, double weight) {
    if (DistributedJobConfig.isEmpty(value)) {
      m_missingCount += weight;
      m_stringLengthStats.update(Utils.missingValue(), weight, false, false);
      m_wordStats.update(Utils.missingValue(), weight, false, false);
    } else {
      m_stringLengthStats.update(value.length(), weight, false, false);

      m_tokenizer.tokenize(value);
      m_wordStats.update(m_tokenizer.countTokens(), weight, false, false);
    }
  }

  /**
   * Compute derived statistics - e.g. mean, standard deviation
   */
  public void computeDerived() {
    m_stringLengthStats.computeDerived();
    m_wordStats.computeDerived();
  }

  /**
   * Convert a meta summary attribute containing string stats into a StringStats
   * object
   * 
   * @param a a meta summary attribute
   * @return a StringStats object
   */
  public static StringStats attributeToStats(Attribute a) {

    ArrayList<String> strLenVals = new ArrayList<String>();
    ArrayList<String> wordVals = new ArrayList<String>();

    for (int i = 0; i < a.numValues(); i++) {
      if (a.value(i).startsWith(STRLEN_PREFIX)) {
        strLenVals.add(a.value(i).replace(STRLEN_PREFIX, ""));
      } else if (a.value(i).startsWith(WORDC_PREFIX)) {
        wordVals.add(a.value(i).replace(WORDC_PREFIX, ""));
      }
    }

    Attribute strLenA = new Attribute(a.name(), strLenVals);
    Attribute wordCA = new Attribute(a.name(), wordVals);
    NumericStats strLenStats = NumericStats.attributeToStats(strLenA);
    NumericStats wordCStats = NumericStats.attributeToStats(wordCA);

    StringStats strStats =
      new StringStats(a.name().replace(
        CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX, ""));
    strStats.m_stringLengthStats = strLenStats;
    strStats.m_wordStats = wordCStats;

    strStats.m_missingCount =
      ArffSummaryNumericMetric.MISSING.valueFromAttribute(strLenA);

    return strStats;
  }

  /**
   * Extends the standard Weka WordTokenizer to expose the countTokens() method
   * in the underlying Tokenizer
   * 
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  protected static class CountWordTokenizer extends WordTokenizer {
    /** For serialization */
    private static final long serialVersionUID = -7144386321420719962L;

    /**
     * Constructor
     * 
     * @return the number of tokens in the currently set string to be tokenized
     */
    public int countTokens() {
      return m_Tokenizer.countTokens();
    }
  }
}
