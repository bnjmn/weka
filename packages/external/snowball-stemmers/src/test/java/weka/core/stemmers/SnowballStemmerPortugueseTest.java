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
 * SnowballStemmerPortugueseTest.java
 * Copyright (C) 2015 University of Waikato, Hamilton, NZ
 */

package weka.core.stemmers;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Tests the portuguese stemmer.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class SnowballStemmerPortugueseTest
  extends AbstractCustomSnowballStemmerTest {

  /**
   * Constructs the <code>AbstractStopwordsTest</code>. Called by subclasses.
   *
   * @param name the name of the test
   */
  public SnowballStemmerPortugueseTest(String name) {
    super(name);
  }

  /**
   * returns the data to use in the tests
   *
   * @return the data to use in the tests
   */
  protected String[] getData() {
    return new String[] {
      "Humpty Dumpty é um personagem de uma rima enigmática infantil, melhor conhecido no mundo anglófono pela versão de Mamãe Gansa na Inglaterra. Ele é retratado como um ovo antropomórfico, com rosto, braços e pernas. Este personagem aparece em muitas obras literárias, como Alice Através do Espelho de Lewis Carroll, e também nas histórias em quadrinhos, como na revista Fábulas da Vertigo/DC Comics.",
      "Marte é o quarto planeta a partir do Sol, o segundo menor do Sistema Solar. Batizado em homenagem ao deus romano da guerra, muitas vezes é descrito como o \"Planeta Vermelho\", porque o óxido de ferro predominante em sua superfície lhe dá uma aparência avermelhada.1",
      "Fausto é o protagonista de uma popular lenda alemã de um pacto com o demônio, baseada no médico, mago e alquimista alemão Dr. Johannes Georg Faust (1480-1540). O nome Fausto tem sido usado como base de diversos textos literários, o mais famoso deles a peça teatral do autor Goethe, produzido em duas partes, escrita e reescrita ao longo de quase sessenta anos. A primeira parte - mais famosa - foi publicada em 1806 e a segunda, em 1832 - às vésperas da morte do autor."
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
    result.setStemmer("portuguese");

    return result;
  }

  public static Test suite() {
    return new TestSuite(SnowballStemmerPortugueseTest.class);
  }

  public static void main(String[] args) {
    TestRunner.run(suite());
  }
}
