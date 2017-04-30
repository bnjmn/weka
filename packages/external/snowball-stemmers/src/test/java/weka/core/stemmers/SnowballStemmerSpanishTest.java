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
 * SnowballStemmerSpanishTest.java
 * Copyright (C) 2015 University of Waikato, Hamilton, NZ
 */

package weka.core.stemmers;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Tests the spanish stemmer.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class SnowballStemmerSpanishTest
  extends AbstractCustomSnowballStemmerTest {

  /**
   * Constructs the <code>AbstractStopwordsTest</code>. Called by subclasses.
   *
   * @param name the name of the test
   */
  public SnowballStemmerSpanishTest(String name) {
    super(name);
  }

  /**
   * returns the data to use in the tests
   *
   * @return the data to use in the tests
   */
  protected String[] getData() {
    return new String[] {
      "Humpty Dumpty es un personaje en una rima infantil de Mamá Ganso, creado en Inglaterra. Es representado como un huevo antropomórfico o personificado. Su traducción definitiva puede ser Zanco Panco.",
      "Marte es el cuarto planeta del Sistema Solar más cercano al Sol. Llamado así por el dios de la guerra de la mitología romana Marte, recibe a veces el apodo de planeta rojo debido a la apariencia rojiza que le confiere el óxido de hierro que domina su superficie. Tiene una atmósfera delgada formada por dióxido de carbono, y dos satélites: Fobos y Deimos. Forma parte de los llamados planetas telúricos (de naturaleza rocosa, como la Tierra) y es el planeta interior más alejado del Sol. Es, en muchos aspectos, el más parecido a la Tierra.",
      "Fausto es el protagonista de una leyenda clásica alemana, un erudito de gran éxito, pero también insatisfecho con su vida, por lo que hace un trato con el diablo, intercambiando su alma por el conocimiento ilimitado y los placeres mundanos. La historia de Fausto es la base de muchas obras literarias, artísticas, cinematográficas y musicales."
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
    result.setStemmer("spanish");

    return result;
  }

  public static Test suite() {
    return new TestSuite(SnowballStemmerSpanishTest.class);
  }

  public static void main(String[] args) {
    TestRunner.run(suite());
  }
}
