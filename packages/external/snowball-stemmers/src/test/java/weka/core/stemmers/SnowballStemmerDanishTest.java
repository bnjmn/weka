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
 * SnowballStemmerDanishTest.java
 * Copyright (C) 2015 University of Waikato, Hamilton, NZ
 */

package weka.core.stemmers;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Tests the danish stemmer.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class SnowballStemmerDanishTest
  extends AbstractCustomSnowballStemmerTest {

  /**
   * Constructs the <code>AbstractStopwordsTest</code>. Called by subclasses.
   *
   * @param name the name of the test
   */
  public SnowballStemmerDanishTest(String name) {
    super(name);
  }

  /**
   * returns the data to use in the tests
   *
   * @return the data to use in the tests
   */
  protected String[] getData() {
    return new String[] {
      "Mars er den fjerde planet i Solsystemet talt fra Solen, og naboplanet til vores egen planet Jorden. Som Jorden har Mars en atmosfære, om end denne er ganske tynd og næsten udelukkende består af kuldioxid. Mars kaldes også den røde planet på grund af sin karakteristiske farve.",
      "Faust er hovedpersonen i en populær fortælling der har været anvendt som grundlag i mange forskellige værker. Fortællingen handler om en lærd mand, Heinrich Faust, der hidkalder djævelen, der i fortællingen almindeligvis hedder Mephistopheles, og tilbyder at sælge sin sjæl til ham hvis djævelen vil tjene ham en tid. En kontrakt underskrives med blod, men i de fleste senere udgaver af fortællingen forbliver Fausts sjæl dog hans egen efter at djævelens tjenestetid er ophørt."
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
    result.setStemmer("danish");

    return result;
  }

  public static Test suite() {
    return new TestSuite(SnowballStemmerDanishTest.class);
  }

  public static void main(String[] args) {
    TestRunner.run(suite());
  }
}
