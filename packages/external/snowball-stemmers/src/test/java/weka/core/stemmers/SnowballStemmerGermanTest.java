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
 * SnowballStemmerGermanTest.java
 * Copyright (C) 2015 University of Waikato, Hamilton, NZ
 */

package weka.core.stemmers;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Tests the german stemmer.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class SnowballStemmerGermanTest
  extends AbstractCustomSnowballStemmerTest {

  /**
   * Constructs the <code>AbstractStopwordsTest</code>. Called by subclasses.
   *
   * @param name the name of the test
   */
  public SnowballStemmerGermanTest(String name) {
    super(name);
  }

  /**
   * returns the data to use in the tests
   *
   * @return the data to use in the tests
   */
  protected String[] getData() {
    return new String[] {
      "Humpty Dumpty ist eine Figur aus einem britischen Kinderreim.[1] Er ist ein menschenähnliches Ei, was im Text des Vierzeilers nicht ausdrücklich erwähnt wird. Im englischen Sprachraum ist dieser Kinderreim seit Jahrhunderten populär und so etwas wie ein fester Bestandteil der Sammlung von Kinderreimen in Mother Goose. Außerhalb des englischen Sprachraumes wurde er vor allem deswegen bekannt, weil Lewis Carroll ihn in Alice hinter den Spiegeln (1871) auftreten ließ (in der deutschen Übersetzung heißt Humpty Dumpty hier Goggelmoggel). Dort diskutiert er mit Alice über Semantik und erklärt ihr unter anderem die Wortschöpfungen Jabberwockys.[2] Als „Humpty Dumpty“ wird von englischen Muttersprachlern aber auch gerne eine kleine und rundliche Person bezeichnet. Hin und wieder ist es jedoch das Synonym für etwas Zerbrechliches, das man überhaupt nicht oder nur schwerlich wieder reparieren kann.",
      "Der Mars ist, von der Sonne aus gesehen, der vierte Planet im Sonnensystem und der äußere Nachbar der Erde. Er zählt zu den erdähnlichen (terrestrischen) Planeten. Sein Durchmesser ist mit knapp 6800 Kilometer etwa halb so groß wie der Erddurchmesser, sein Volumen beträgt gut ein Siebentel der Erde. Damit ist der Mars nach dem Merkur der zweitkleinste Planet des Sonnensystems. Mit einer durchschnittlichen Entfernung von 228 Millionen Kilometern ist er rund 1,5-mal so weit von der Sonne entfernt wie die Erde.",
      "Der Fauststoff, die Geschichte des Doktor Johannes Faustus und seines Pakts mit Mephistopheles, gehört zu den am weitesten verbreiteten Stoffen in der europäischen Literatur seit dem 16. Jahrhundert. Das lückenhafte Wissen über den historischen Johann Georg Faust (wohl etwa 1480–1540) und sein spektakuläres Ende begünstigten Legendenbildungen und ließ Schriftstellern, die sich mit seinem Leben befassten, einigen Spielraum. Eigenschaften des Fauststoffs, die in den unterschiedlichsten Versionen wiederkehren, sind Fausts Erkenntnis- oder Machtstreben, sein Teufelspakt und seine erotischen Ambitionen."
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
    result.setStemmer("german");

    return result;
  }

  public static Test suite() {
    return new TestSuite(SnowballStemmerGermanTest.class);
  }

  public static void main(String[] args) {
    TestRunner.run(suite());
  }
}
