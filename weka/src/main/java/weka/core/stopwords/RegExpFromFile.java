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
 * RegExpFromFile.java
 * Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 */

package weka.core.stopwords;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 <!-- globalinfo-start -->
 * Uses the regular expressions stored in the file for determining whether a word is a stopword (ignored if pointing to a directory). One expression per line.
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
 *  The file containing the stopwords (ignored if directory)
 *  (default: .)</pre>
 *
 <!-- options-end -->
 *
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class RegExpFromFile
  extends AbstractFileBasedStopwords {

  /** for serialization. */
  private static final long serialVersionUID = -722795295494945193L;

  /** The list of regular expressions. */
  protected List<Pattern> m_Patterns;

  /**
   * Returns a string describing the stopwords scheme.
   *
   * @return a description suitable for displaying in the gui
   */
  @Override
  public String globalInfo() {
    return
	"Uses the regular expressions stored in the file for determining "
	+ "whether a word is a stopword (ignored if "
	+ "pointing to a directory). One expression per line.\n"
	+ "More information on regular expressions:\n"
	+ "http://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html";
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  @Override
  public String stopwordsTipText() {
    return "The file containing the regular expressions.";
  }

  /**
   * Performs intialization of the scheme.
   */
  @Override
  protected void initialize() {
    List<String>	patterns;

    super.initialize();

    m_Patterns = new ArrayList<Pattern>();
    patterns   = read();
    for (String pattern: patterns) {
      m_Patterns.add(Pattern.compile(pattern));
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
    for (Pattern pattern: m_Patterns) {
      if (pattern.matcher(word.trim().toLowerCase()).matches()) {
	if (m_Debug)
	  debug(pattern.pattern() + " --> true");
	return true;
      }
      else {
	if (m_Debug)
	  debug(pattern.pattern() + " --> false");
      }
    }
    return false;
  }
}
