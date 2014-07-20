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
 * WordsFromFile.java
 * Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 */

package weka.core.stopwords;

import java.util.HashSet;
import java.util.List;

/**
 <!-- globalinfo-start -->
 * Uses the stopwords located in the specified file (ignored _if pointing to a directory). One stopword per line. Lines starting with '#' are considered comments and ignored.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 *
 * <pre> -D
 *  If set, stopword scheme is run in debug mode and
 *  may output additional info to the console</pre>
 *
 * <pre> -stopwords &lt;file&gt;
 *  The file containing the stopwords.
 *  (default: .)</pre>
 *
 <!-- options-end -->
 *
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class WordsFromFile
  extends AbstractFileBasedStopwords {

  /** for serialization. */
  private static final long serialVersionUID = -722795295494945193L;

  /** The hash set containing the list of stopwords. */
  protected HashSet<String> m_Words;

  /**
   * Returns a string describing the stopwords scheme.
   *
   * @return a description suitable for displaying in the gui
   */
  @Override
  public String globalInfo() {
    return
	"Uses the stopwords located in the specified file (ignored _if "
	+ "pointing to a directory). One stopword per line. Lines "
	+ "starting with '#' are considered comments and ignored.";
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  @Override
  public String stopwordsTipText() {
    return "The file containing the stopwords.";
  }

  /**
   * Performs intialization of the scheme.
   */
  @Override
  protected void initialize() {
    List<String>	words;

    super.initialize();

    m_Words = new HashSet<String>();
    words   = read();
    for (String word: words) {
      // comment?
      if (!word.startsWith("#"))
	m_Words.add(word);
    }
  }

  /**
   * Returns true if the given string is a stop word.
   *
   * @param word the word to test
   * @return true if the word is a stopword
   */
  @Override
  protected synchronized boolean is(String word) {
    return m_Words.contains(word.trim().toLowerCase());
  }
}
