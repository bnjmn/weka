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

/*
 * Copyright (C) 2015 University of Waikato
 */

package weka.filters.unsupervised.attribute;

import junit.framework.Test;
import junit.framework.TestSuite;
import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import java.io.StringReader;

/**
 * Tests FixedDictionaryStringToWordVector
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class FixedDictionaryStringToWordVectorTest extends AbstractFilterTest {

  protected static final String DICTIONARY =
    "the\nquick\nbrown\nfox\njumped\nover\n"
      + "lazy\ndog\nhumpty\ndumpty\nsat\non\na\nwall\nhad\ngreat\nfall\nall\n"
      + "kings\nhorses\nmen\nand\n";

  /**
   * Constructs the <code>AbstractFilterTest</code>. Called by subclasses.
   *
   * @param name the name of the test class
   */
  public FixedDictionaryStringToWordVectorTest(String name) {
    super(name);
  }

  @Override
  public Filter getFilter() {
    FixedDictionaryStringToWordVector f =
      new FixedDictionaryStringToWordVector();

    StringReader reader = new StringReader(DICTIONARY);
    f.setDictionarySource(reader);

    return f;
  }

  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numInstances(),  result.numInstances());
  }

  public static Test suite() {
    return new TestSuite(FixedDictionaryStringToWordVectorTest.class);
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
