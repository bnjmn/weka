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
 * Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 */

package weka.core.tokenizers;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests CharacterNGramTokenizer. Run from the command line with:
 * <p>
 * java weka.core.tokenizers.CharacterNGramTokenizerTest
 * 
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 10971 $
 */
public class CharacterNGramTokenizerTest
  extends AbstractTokenizerTest {

  public CharacterNGramTokenizerTest(String name) {
    super(name);
  }

  /** Creates a default CharacterNGramTokenizer */
  @Override
  public Tokenizer getTokenizer() {
    return new CharacterNGramTokenizer();
  }

  /**
   * tests the number of generated tokens
   */
  public void testNumberOfGeneratedTokens() {
    String s;
    String[] result;

    s = "HOWEVER, the egg only got larger and larger, and more and more human";

    // only 1-grams
    try {
      result =
        Tokenizer.tokenize(m_Tokenizer, new String[] { "-min", "1", "-max",
          "1", s });
      assertEquals("number of tokens differ (1)", 68, result.length);
    } catch (Exception e) {
      fail("Error tokenizing string '" + s + "'!");
    }

    // only 2-grams
    try {
      result =
        Tokenizer.tokenize(m_Tokenizer, new String[] { "-min", "2", "-max",
          "2", s });
      assertEquals("number of tokens differ (2)", 67, result.length);
    } catch (Exception e) {
      fail("Error tokenizing string '" + s + "'!");
    }

    // 1 to 3-grams
    try {
      result =
        Tokenizer.tokenize(m_Tokenizer, new String[] { "-min", "1", "-max",
          "3", s });
      assertEquals("number of tokens differ (3)", 201, result.length);
    } catch (Exception e) {
      fail("Error tokenizing string '" + s + "'!");
    }

    // 1 to 3-grams, but sentence only has 1 and 2 grams
    try {
      s = "ca";
      result =
        Tokenizer.tokenize(m_Tokenizer, new String[] { "-min", "1", "-max",
          "3", s });
      assertEquals("number of tokens differ (4)", 3, result.length);
    } catch (Exception e) {
      fail("Error tokenizing string '" + s + "'!");
    }
  }

  public static Test suite() {
    return new TestSuite(CharacterNGramTokenizerTest.class);
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
