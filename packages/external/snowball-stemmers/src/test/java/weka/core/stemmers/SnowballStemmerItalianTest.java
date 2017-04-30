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
 * SnowballStemmerItalianTest.java
 * Copyright (C) 2015 University of Waikato, Hamilton, NZ
 */

package weka.core.stemmers;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Tests the italian stemmer.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class SnowballStemmerItalianTest
  extends AbstractCustomSnowballStemmerTest {

  /**
   * Constructs the <code>AbstractStopwordsTest</code>. Called by subclasses.
   *
   * @param name the name of the test
   */
  public SnowballStemmerItalianTest(String name) {
    super(name);
  }

  /**
   * returns the data to use in the tests
   *
   * @return the data to use in the tests
   */
  protected String[] getData() {
    return new String[] {
      "Humpty Dumpty, talvolta tradotto Unto Dunto, è un personaggio di una filastrocca di Mamma Oca, rappresentato come un grosso uovo antropomorfizzato seduto sulla cima di un muretto. Fu utilizzato anche da Lewis Carroll, che gli fece incontrare Alice in uno dei capitoli più celebri di Attraverso lo specchio e quel che Alice vi trovò, molto caro, oltre che ai bambini, agli studiosi di semantica e linguistica.",
      "Marte è il quarto pianeta del sistema solare in ordine di distanza dal Sole e l'ultimo dei pianeti di tipo terrestre dopo Mercurio, Venere e la Terra. Viene chiamato il Pianeta rosso a causa del suo colore caratteristico dovuto alle grandi quantità di ossido di ferro che lo ricoprono.",
      "Faust – per esteso Doktor Faust o Doctor Faustus, talvolta italianizzato in Fausto[1] – è il protagonista di un racconto popolare tedesco che è stato usato come base per numerose opere di fantasia. Il racconto riguarda il destino di un sapiente (scienziato o chierico) chiamato Faust il quale, nella sua continua ricerca di conoscenze avanzate o proibite delle cose materiali, invoca il diavolo (rappresentato da Mefistofele), che si offre di servirlo per un periodo di tempo, in tutto ventiquattro anni, e al prezzo della sua anima gli consentirà la conoscenza assoluta."
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
    result.setStemmer("italian");

    return result;
  }

  public static Test suite() {
    return new TestSuite(SnowballStemmerItalianTest.class);
  }

  public static void main(String[] args) {
    TestRunner.run(suite());
  }
}
