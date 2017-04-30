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
 * SnowballStemmerDutchTest.java
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
public class SnowballStemmerDutchTest
  extends AbstractCustomSnowballStemmerTest {

  /**
   * Constructs the <code>AbstractStopwordsTest</code>. Called by subclasses.
   *
   * @param name the name of the test
   */
  public SnowballStemmerDutchTest(String name) {
    super(name);
  }

  /**
   * returns the data to use in the tests
   *
   * @return the data to use in the tests
   */
  protected String[] getData() {
    return new String[] {
      "Humpty Dumpty is een fictief wezen dat voor het eerst genoemd werd in een Engelstalig kinderlied uit 1803. Hij wordt grafisch doorgaans weergegeven als een aangekleed ei, hoewel zijn bedenker dit nooit expliciet zo benoemd heeft. Humpty Dumpty geldt in Engelstalige gebieden als een verwensing aan het adres van een ander persoon, die daarmee wordt aangeduid als klein en onhandig.",
      "Mars is vanaf de zon geteld de vierde planeet van ons zonnestelsel, om de zon draaiend in een baan tussen die van de Aarde en Jupiter. De planeet is kleiner dan de Aarde en met een (maximale) magnitude van -2,9 minder helder dan Venus en meestal minder helder dan Jupiter. Mars wordt wel de rode planeet genoemd maar is in werkelijkheid eerder okerkleurig. De planeet is vernoemd naar de Romeinse god van de oorlog. Mars is gemakkelijk met het blote oog te zien, vooral in de maanden rond een oppositie. 's Nachts is Mars dan te zien als een heldere roodachtige \"ster\" die evenwel door zijn relatieve nabijheid geen puntbron is maar een schijfje. Daarom flonkert Mars niet zoals een verre rode ster als Aldebaran.",
      "Faust is een legendarische figuur, gebaseerd op de Duitse magiër en medicus Johann Faust. Gelijk met zijn dood, vermoedelijk rond 1540, ontstond het gerucht dat hij een pact met de duivel had gesloten. "
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
    result.setStemmer("dutch");

    return result;
  }

  public static Test suite() {
    return new TestSuite(SnowballStemmerDutchTest.class);
  }

  public static void main(String[] args) {
    TestRunner.run(suite());
  }
}
