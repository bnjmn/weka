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
 * SnowballStemmerSwedishTest.java
 * Copyright (C) 2015 University of Waikato, Hamilton, NZ
 */

package weka.core.stemmers;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Tests the swedish stemmer.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class SnowballStemmerSwedishTest
  extends AbstractCustomSnowballStemmerTest {

  /**
   * Constructs the <code>AbstractStopwordsTest</code>. Called by subclasses.
   *
   * @param name the name of the test
   */
  public SnowballStemmerSwedishTest(String name) {
    super(name);
  }

  /**
   * returns the data to use in the tests
   *
   * @return the data to use in the tests
   */
  protected String[] getData() {
    return new String[] {
      "Mars (symbol: Mars symbol.svg) är den fjärde planeten från solen och solsystemets näst minsta planet. Den har fått sitt namn efter den romerska krigsguden Mars och kallas ibland för \"den röda planeten\" på grund av sitt rödaktiga utseende. Den röda färgen beror på stora mängder järnoxid (rost) som finns fördelat över ytan och i atmosfären. Mars är en av de fyra stenplaneterna och har en tunn atmosfär som till största delen består av koldioxid. Ytan är täckt av kratrar av olika storlekar likt månen, men Mars har precis som jorden även många vulkaner, dalgångar, vidsträckta slätter och iskalotter vid polerna.",
      "Faust är en gestalt som finns i flera versioner av tyska folksagor och som delvis bygger på Johann Fausts öde. Sagans Faust är en ung man som sluter ett avtal med Mefistofeles, det vill säga djävulen. Faust och adjektivet faustiansk syftar numera på en ambitiös person som offrar sin moraliska integritet för att nå makt och framgång under en kort period."
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
    result.setStemmer("swedish");

    return result;
  }

  public static Test suite() {
    return new TestSuite(SnowballStemmerSwedishTest.class);
  }

  public static void main(String[] args) {
    TestRunner.run(suite());
  }
}
