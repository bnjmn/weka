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
 * SnowballStemmerFinnishTest.java
 * Copyright (C) 2015 University of Waikato, Hamilton, NZ
 */

package weka.core.stemmers;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Tests the finnish stemmer.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class SnowballStemmerFinnishTest
  extends AbstractCustomSnowballStemmerTest {

  /**
   * Constructs the <code>AbstractStopwordsTest</code>. Called by subclasses.
   *
   * @param name the name of the test
   */
  public SnowballStemmerFinnishTest(String name) {
    super(name);
  }

  /**
   * returns the data to use in the tests
   *
   * @return the data to use in the tests
   */
  protected String[] getData() {
    return new String[] {
      "Mars (symboli: Mars symbol.svg) on Maan naapuriplaneetta ja aurinkokunnan neljäs planeetta Auringosta laskettuna. Planeetta on nimetty roomalaisessa mytologiassa esiintyvän sodanjumala Marsin mukaan. Punertavan Marsin läpimitta on noin puolet Maan läpimitasta,[1] ja se on ihmiselle elinkelvoton.[2] Marsissa on höyrynä ja jäänä esiintyvää vettä sekä hyvin ohut kaasukehä, josta suurin osa on hiilidioksidia ja loput pääasiassa typpeä.[1] Kaasukehän ohuus johtuu Marsin pienestä painovoimasta. Mars on lisäksi niin kylmä, että sen kaasukehän hiilidioksidi tiivistyy siellä aika ajoin napalakkeihin. Marsilla on myös kaksi pientä kuuta, Phobos ja Deimos.[3]",
      "Faust on fiktiivinen henkilöhahmo, joka myi sielunsa paholaiselle ja sai vastineeksi rajattoman tiedon sekä yliluonnollisia kykyjä maallisen elämänsä ajaksi, mutta hän alkoi myöhemmin katua kauppaansa. Tarinassa paholaisen edustajana esiintyy Mefistofeles-niminen hahmo. Tarinan eri versioissa on joko traaginen tai onnellinen loppu: Faust joko päätyy ikuiseen kadotukseen tai pelastuu katuvaisena Jumalan armon ansiosta."
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
    result.setStemmer("finnish");

    return result;
  }

  public static Test suite() {
    return new TestSuite(SnowballStemmerFinnishTest.class);
  }

  public static void main(String[] args) {
    TestRunner.run(suite());
  }
}
