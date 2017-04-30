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
 * SnowballStemmerEnglishTest.java
 * Copyright (C) 2015 University of Waikato, Hamilton, NZ
 */

package weka.core.stemmers;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Tests the english stemmer.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class SnowballStemmerEnglishTest
  extends AbstractCustomSnowballStemmerTest {

  /**
   * Constructs the <code>AbstractStopwordsTest</code>. Called by subclasses.
   *
   * @param name the name of the test
   */
  public SnowballStemmerEnglishTest(String name) {
    super(name);
  }

  /**
   * returns the data to use in the tests
   *
   * @return the data to use in the tests
   */
  protected String[] getData() {
    return new String[]{
      "Humpty Dumpty is a character in an English nursery rhyme, probably originally a riddle and one of the best known in the English-speaking world. Though not explicitly described, he is typically portrayed as an anthropomorphic egg. The first recorded versions of the rhyme date from late eighteenth century England and the tune from 1870 in James William Elliott's National Nursery Rhymes and Nursery Songs. Its origins are obscure and several theories have been advanced to suggest original meanings.",
      "Mars is the fourth planet from the Sun and the second smallest planet in the Solar System, after Mercury. Named after the Roman god of war, it is often referred to as the \"Red Planet\" because the iron oxide prevalent on its surface gives it a reddish appearance.[15] Mars is a terrestrial planet with a thin atmosphere, having surface features reminiscent both of the impact craters of the Moon and the volcanoes, valleys, deserts, and polar ice caps of Earth. The rotational period and seasonal cycles of Mars are likewise similar to those of Earth, as is the tilt that produces the seasons. Mars is the site of Olympus Mons, the largest volcano and second-highest known mountain in the Solar System, and of Valles Marineris, one of the largest canyons in the Solar System. The smooth Borealis basin in the northern hemisphere covers 40% of the planet and may be a giant impact feature.[16][17] Mars has two moons, Phobos and Deimos, which are small and irregularly shaped. These may be captured asteroids,[18][19] similar to 5261 Eureka, a Mars trojan.",
      "Faust is the protagonist of a classic German legend. He is a scholar who is highly successful yet dissatisfied with his life, which leads him to make a pact with the Devil, exchanging his soul for unlimited knowledge and worldly pleasures. The Faust legend has been the basis for many literary, artistic, cinematic, and musical works that have reinterpreted it through the ages. Faust and the adjective Faustian imply a situation in which an ambitious person surrenders moral integrity in order to achieve power and success for a delimited term.[1]"
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
    result.setStemmer("english");

    return result;
  }

  public static Test suite() {
    return new TestSuite(SnowballStemmerEnglishTest.class);
  }

  public static void main(String[] args) {
    TestRunner.run(suite());
  }
}
