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
 *    SubstringLabelerTest
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.unsupervised.attribute;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;

import junit.framework.Assert;
import org.junit.Test;

import weka.core.Instance;
import weka.core.Instances;

/**
 * Test class for SubstringLabeler
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class SubstringLabelerTest {

  protected static final String s_test1 = "@relation test1\n"
    + "@attribute first string\n" + "@data\n"
    + "'The quick brown dog jumped over the lazy dog'\n"
    + "'The quick green turnip sat on the hyperactive snail'\n"
    + "'The quick green turnip sat on the hyperactive snail'\n";

  protected static final String s_test2 = "@relation test2\n"
    + "@attribute first string\n" + "@attribute second string\n" + "@data\n"
    + "'The quick brown dog jumped over the lazy dog','a dog'\n"
    + "'The quick green turnip sat on the hyperactive snail', 'a cat'\n"
    + "'The quick green turnip sat on the hyperactive snail', 'a snail'\n";

  protected static Instances getData(String data) throws Exception {
    Instances d = new Instances(new StringReader(data));

    return d;
  }

  @Test
  public void testNonRegexOneRule() throws Exception {
    SubstringLabeler labeler = new SubstringLabeler();

    String[] opts =
      { "-match-rule", "first@@MR@@f@@MR@@t@@MR@@quick@@MR@@class1" };
    labeler.setOptions(opts);
    Instances data = getData(s_test1);
    labeler.setInputFormat(data);

    Instances outFormat = labeler.getOutputFormat();
    assertTrue(outFormat != null);
    assertEquals(2, outFormat.numAttributes());
    assertEquals(1, outFormat.classIndex());
    assertTrue(outFormat.classAttribute().isNominal());
    assertEquals(1, outFormat.classAttribute().numValues());
    assertEquals("class1", outFormat.classAttribute().value(0));

    labeler.input(data.instance(0));
    Instance firstTransformed = labeler.output();
    assertFalse(firstTransformed.classIsMissing());

    labeler.input(data.instance(1));
    assertFalse(labeler.output().classIsMissing());
    labeler.input(data.instance(2));
    assertFalse(labeler.output().classIsMissing());
  }

  @Test
  public void testNonRegexTwoRules() throws Exception {
    SubstringLabeler labeler = new SubstringLabeler();

    String[] opts =
      { "-match-rule", "first@@MR@@f@@MR@@t@@MR@@dog@@MR@@class1",
      "-match-rule", "first@@MR@@f@@MR@@t@@MR@@snail@@MR@@class2"};
    labeler.setOptions(opts);
    Instances data = getData(s_test1);
    labeler.setInputFormat(data);

    Instances outFormat = labeler.getOutputFormat();
    assertTrue(outFormat != null);
    assertEquals(2, outFormat.numAttributes());
    assertEquals(1, outFormat.classIndex());
    assertTrue(outFormat.classAttribute().isNominal());
    assertEquals(2, outFormat.classAttribute().numValues());
    Assert.assertEquals("class1", outFormat.classAttribute().value(0));

    labeler.input(data.instance(0));
    Instance firstTransformed = labeler.output();
    assertFalse(firstTransformed.classIsMissing());
    assertEquals("class1", firstTransformed.stringValue(1));

    labeler.input(data.instance(1));
    Instance secondTransformed = labeler.output();
    assertFalse(secondTransformed.classIsMissing());
    assertEquals("class2", secondTransformed.stringValue(1));

  }

  public static void main(String[] args) {
    try {
      SubstringLabelerTest t = new SubstringLabelerTest();
      t.testNonRegexTwoRules();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
