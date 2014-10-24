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
 * NGramTokenizer.java
 * Copyright (C) 2007-2012 University of Waikato
 */

package weka.core.tokenizers;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.Option;
import weka.core.RevisionUtils;
import weka.core.Utils;

/**
 * <!-- globalinfo-start --> Splits a string into an n-gram with min and max
 * grams.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -max &lt;int&gt;
 *  The max size of the Ngram (default = 3).
 * </pre>
 * 
 * <pre>
 * -min &lt;int&gt;
 *  The min size of the Ngram (default = 1).
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Sebastian Germesin (sebastian.germesin@dfki.de)
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * 
 * @version $Revision: 10971 $
 */
public class CharacterNGramTokenizer extends Tokenizer {

  /** for serialization */
  private static final long serialVersionUID = -1181896253171647218L;

  /** the maximum number of N */
  protected int m_NMax = 3;

  /** the minimum number of N */
  protected int m_NMin = 1;

  /** the current length of the N-grams */
  protected int m_N;

  /** the current position for returning elements */
  protected int m_CurrentPosition;

  /** the string to tokenize */
  protected String m_String;

  /**
   * Returns a string describing the tokenizer
   * 
   * @return a description suitable for displaying in the explorer/experimenter
   *         GUI
   */
  @Override
  public String globalInfo() {
    return "Splits a string into all character n-grams it contains based on the given maximum and minimum for n.";
  }

  /**
   * Returns an enumeration of all the available options..
   * 
   * @return an enumeration of all available options.
   */
  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.addElement(new Option("\tThe maximum number of characters (default = 3).",
      "max", 1, "-max <int>"));

    result.addElement(new Option("\tThe minimum number of characters (default = 1).",
      "min", 1, "-min <int>"));

    result.addAll(Collections.list(super.listOptions()));

    return result.elements();
  }

  /**
   * Gets the current option settings for the OptionHandler.
   * 
   * @return the list of current option settings as an array of strings
   */
  @Override
  public String[] getOptions() {
    Vector<String> result = new Vector<String>();

    result.add("-max");
    result.add("" + getNGramMaxSize());

    result.add("-min");
    result.add("" + getNGramMinSize());

    Collections.addAll(result, super.getOptions());

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
   * -max &lt;int&gt;
   *  The max size of the Ngram (default = 3).
   * </pre>
   * 
   * <pre>
   * -min &lt;int&gt;
   *  The min size of the Ngram (default = 1).
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    String value;

    value = Utils.getOption("max", options);
    if (value.length() != 0) {
      setNGramMaxSize(Integer.parseInt(value));
    } else {
      setNGramMaxSize(3);
    }

    value = Utils.getOption("min", options);
    if (value.length() != 0) {
      setNGramMinSize(Integer.parseInt(value));
    } else {
      setNGramMinSize(1);
    }

    super.setOptions(options);
  }

  /**
   * Gets the max N of the NGram.
   * 
   * @return the size (N) of the NGram.
   */
  public int getNGramMaxSize() {
    return m_NMax;
  }

  /**
   * Sets the max size of the Ngram.
   * 
   * @param value the size of the NGram.
   */
  public void setNGramMaxSize(int value) {
    if (value < 1) {
      m_NMax = 1;
    } else {
      m_NMax = value;
    }
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String NGramMaxSizeTipText() {
    return "The maximum size of an n-gram.";
  }

  /**
   * Sets the min size of the Ngram.
   * 
   * @param value the size of the NGram.
   */
  public void setNGramMinSize(int value) {
    if (value < 1) {
      m_NMin = 1;
    } else {
      m_NMin = value;
    }
  }

  /**
   * Gets the min N of the NGram.
   * 
   * @return the size (N) of the NGram.
   */
  public int getNGramMinSize() {
    return m_NMin;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String NGramMinSizeTipText() {
    return "The minimum size of an n-gram.";
  }

  /**
   * returns true if there's more elements available
   * 
   * @return true if there are more elements available
   */
  @Override
  public boolean hasMoreElements() {
    
    return (m_CurrentPosition + m_N <= m_String.length());
  }

  /**
   * Returns N-grams and also (N-1)-grams and .... 
   * 
   * @return the next element
   */
  @Override
  public String nextElement() {
    
    String result = null;
    try {
       result = m_String.substring(m_CurrentPosition, m_CurrentPosition + m_N); 
    } catch (StringIndexOutOfBoundsException ex) {
      // Just return null;
    }
    m_N++;
    if ((m_N > m_NMax) || (m_CurrentPosition + m_N > m_String.length())) {
      m_N = m_NMin;
      m_CurrentPosition++;
    }
    return result;
  }


  /**
   * Sets the string to tokenize. 
   * 
   * @param s the string to tokenize
   */
  @Override
  public void tokenize(String s) {
    
    m_CurrentPosition = 0;
    m_String = s;
    m_N = m_NMin;
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 10971 $");
  }

  /**
   * Runs the tokenizer with the given options and strings to tokenize. The
   * tokens are printed to stdout.
   * 
   * @param args the commandline options and strings to tokenize
   */
  public static void main(String[] args) {
    runTokenizer(new CharacterNGramTokenizer(), args);
  }
}
