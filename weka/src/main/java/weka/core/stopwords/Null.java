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
 * Null.java
 * Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 */

package weka.core.stopwords;

/**
 <!-- globalinfo-start -->
 * Dummy stopwords scheme, always returns false.
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
 <!-- options-end -->
 *
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class Null
  extends AbstractStopwords {

  /** for serialization. */
  private static final long serialVersionUID = -3319681866579617385L;

  /**
   * Returns a string describing the stopwords scheme.
   *
   * @return a description suitable for displaying in the gui
   */
  @Override
  public String globalInfo() {
    return
	"Dummy stopwords scheme, always returns false.";
  }

  /**
   * Returns true if the given string is a stop word.
   *
   * @param word the word to test
   * @return always false
   */
  @Override
  protected boolean is(String word) {
    return false;
  }
}
