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
 * SnowballStemmerNorwegianTest.java
 * Copyright (C) 2015 University of Waikato, Hamilton, NZ
 */

package weka.core.stemmers;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Tests the norwegian stemmer.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class SnowballStemmerNorwegianTest
  extends AbstractCustomSnowballStemmerTest {

  /**
   * Constructs the <code>AbstractStopwordsTest</code>. Called by subclasses.
   *
   * @param name the name of the test
   */
  public SnowballStemmerNorwegianTest(String name) {
    super(name);
  }

  /**
   * returns the data to use in the tests
   *
   * @return the data to use in the tests
   */
  protected String[] getData() {
    return new String[] {
      "Mars (symbol:Det astronomiske symbolet for Mars) er den fjerde planeten fra solen i vårt solsystem og er oppkalt etter den romerske krigsguden Mars. Planeten blir ofte beskrevet som den «røde planeten» på grunn av sitt rødlige utseende, forårsaket av jern(III)oksid på overflaten.[10] Mars er en steinplanet med en tynn atmosfære. Overflateegenskapene minner om både nedslagskraterene på månen og vulkanene, dalene, ørkenene og de polare iskappene på jorden. Rotasjonsperioden og årstidssyklusene på Mars ligner også på jorden siden det er aksehelningen som fører til årstidene. Olympus Mons er det høyeste kjente fjellet i solsystemet, og Valles Marineris er det største dalsystemet. Det flate Borealisbassenget på den nordlige halvkulen dekker ca. 40 % av planeten og kan stamme fra et gigantisk nedslag.[11][12]",
      "Faust (tysk for «neve») eller Faustus (latin for «den lykkelige», eventuelt avledet av Fustis for «stokk, kølle»[2]) er hovedpersonen i en tysk legende som er kjent fra reformasjonstiden på 1500-tallet. Den lærde herr Johann Faust inngår en pakt med djevelens sendebud Mefistofeles og vil gi djevelen sin sjel hvis bare djevelen vil tjene ham en tid. En kontrakt underskrives med blod. I den eldste versjonen av fortellingen blir Johann Faust hentet av ukjente krefter når paktstiden er utløpt. Andre versjoner av fortellingen forblir Fausts sjel dog hans egen etter at hans tjenestetid for djevelen er opphørt."
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
    result.setStemmer("norwegian");

    return result;
  }

  public static Test suite() {
    return new TestSuite(SnowballStemmerNorwegianTest.class);
  }

  public static void main(String[] args) {
    TestRunner.run(suite());
  }
}
