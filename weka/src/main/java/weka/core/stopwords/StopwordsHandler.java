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
 * StopwordsHandler.java
 * Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 */

package weka.core.stopwords;

/**
 * Interface for classes that support stopword handling.
 *
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public interface StopwordsHandler {

  /**
   * Returns true if the given string is a stop word.
   *
   * @param word the word to test
   * @return true if the word is a stopword
   */
  public boolean isStopword(String word);
}
