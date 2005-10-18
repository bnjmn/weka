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
 * NullStemmer.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.stemmers;

/**
 * Actually no real stemmer, since it doesn't perform any stemming at all.
 * Only as dummy stemmer used.
 *
 * @author    FracPete (fracpete at waikato dot ac dot nz)
 * @version   $Revision: 1.1 $
 */
public class NullStemmer 
  implements Stemmer {
  
  /**
   * Returns the word as it is.
   *
   * @param word      the unstemmed word
   * @return          the unstemmed word, again
   */
  public String stem(String word) {
    return new String(word);
  }

  /**
   * for testing only
   */
  public static void main(String[] args) {
    Stemmer     s;
    String      word;
    
    s = new NullStemmer();
    
    word = "shoes";
    System.out.println(word + " -> " + s.stem(word));
    
    word = "houses";
    System.out.println(word + " -> " + s.stem(word));
    
    word = "programmed";
    System.out.println(word + " -> " + s.stem(word));
  }
}
