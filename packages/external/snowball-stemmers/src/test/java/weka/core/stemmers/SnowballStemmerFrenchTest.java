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

/**
 * SnowballStemmerFrenchTest.java
 * Copyright (C) 2015 University of Waikato, Hamilton, NZ
 */

package weka.core.stemmers;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Tests the french stemmer.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class SnowballStemmerFrenchTest
  extends AbstractCustomSnowballStemmerTest {

  /**
   * Constructs the <code>AbstractStopwordsTest</code>. Called by subclasses.
   *
   * @param name the name of the test
   */
  public SnowballStemmerFrenchTest(String name) {
    super(name);
  }

  /**
   * returns the data to use in the tests
   *
   * @return the data to use in the tests
   */
  protected String[] getData() {
    return new String[] {
      "Humpty Dumpty est un personnage éponyme d'une comptine anglaise extrêmement populaire, le plus souvent représenté comme un œuf.",
      "Mars (prononcé en français : /maʁs/3) est la quatrième planète par ordre de distance croissante au Soleil et la deuxième par masse et par taille croissantes sur les huit planètes que compte le Système solaire. Son éloignement au Soleil est compris entre 1,381 et 1,666 UA (206,6 à 249,2 millions de km), avec une période orbitale de 686,71 jours terrestres.",
      "Faust est le héros d'un conte populaire allemand ayant fait florès au XVIe siècle, à l'origine de nombreuses réinterprétations."
    };
  }

  /**
   * Used to create an instance of a specific stopwords scheme.
   *
   * @return a suitably configured <code>Stemmer</code> value
   */
  public Stemmer getStemmer() {
    SnowballStemmer	result;

    result = new SnowballStemmer();
    result.setStemmer("french");

    return result;
  }

  public static Test suite() {
    return new TestSuite(SnowballStemmerFrenchTest.class);
  }

  public static void main(String[] args) {
    TestRunner.run(suite());
  }
}
