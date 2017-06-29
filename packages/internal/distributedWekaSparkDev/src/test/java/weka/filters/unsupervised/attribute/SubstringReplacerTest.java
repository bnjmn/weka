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
 *    SubstringReplacerTest
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.unsupervised.attribute;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;

import org.junit.Test;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.WekaException;

/**
 * Test class for SubstringReplacer
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class SubstringReplacerTest {

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
    SubstringReplacer replacer = new SubstringReplacer();

    String[] opts =
      { "-match-rule", "first@@MR@@f@@MR@@t@@MR@@quick@@MR@@slow" };
    replacer.setOptions(opts);
    Instances data = getData(s_test1);

    replacer.setInputFormat(data);

    Instances outFormat = replacer.getOutputFormat();
    if (!outFormat.equalHeaders(data)) {
      throw new WekaException(outFormat.equalHeadersMsg(data));
    }

    replacer.input(data.instance(0));
    Instance firstTransformed = replacer.output();
    assertTrue(firstTransformed.stringValue(0).contains("slow"));

    replacer.input(data.instance(1));
    Instance secondTransformed = replacer.output();
    System.err.println(secondTransformed);
    assertTrue(secondTransformed.stringValue(0).contains("slow"));
  }

  @Test
  public void testNonRegexOneRuleTwoAtts() throws Exception {
    SubstringReplacer replacer = new SubstringReplacer();

    String[] opts =
      { "-match-rule", "second@@MR@@f@@MR@@t@@MR@@dog@@MR@@fish" };
    replacer.setOptions(opts);
    Instances data = getData(s_test2);

    replacer.setInputFormat(data);

    Instances outFormat = replacer.getOutputFormat();
    if (!outFormat.equalHeaders(data)) {
      throw new WekaException(outFormat.equalHeadersMsg(data));
    }

    replacer.input(data.instance(0));
    Instance firstTransformed = replacer.output();
    assertTrue(firstTransformed.stringValue(1).contains("fish"));
    assertFalse(firstTransformed.stringValue(0).contains("fish"));

    replacer.input(data.instance(1));
    Instance secondTransformed = replacer.output();
    assertFalse(secondTransformed.stringValue(0).contains("fish"));
    assertFalse(secondTransformed.stringValue(1).contains("fish"));
  }

  @Test
  public void testNonRegexTwoRules() throws Exception {
    SubstringReplacer replacer = new SubstringReplacer();

    String[] opts =
      { "-match-rule", "first@@MR@@f@@MR@@t@@MR@@quick@@MR@@slow",
        "-match-rule", "first@@MR@@f@@MR@@t@@MR@@turnip@@MR@@carrot" };
    replacer.setOptions(opts);
    Instances data = getData(s_test1);

    replacer.setInputFormat(data);

    Instances outFormat = replacer.getOutputFormat();
    if (!outFormat.equalHeaders(data)) {
      throw new WekaException(outFormat.equalHeadersMsg(data));
    }

    replacer.input(data.instance(0));
    Instance firstTransformed = replacer.output();
    assertTrue(firstTransformed.stringValue(0).contains("slow"));
    assertFalse(firstTransformed.stringValue(0).contains("carrot"));

    replacer.input(data.instance(1));
    Instance secondTransformed = replacer.output();
    System.err.println(secondTransformed);
    assertTrue(secondTransformed.stringValue(0).contains("slow"));
    assertTrue(secondTransformed.stringValue(0).contains("carrot"));
  }

  @Test
  public void testNonRegexCaseSensitiveOneRule() throws Exception {
    SubstringReplacer replacer = new SubstringReplacer();

    String[] opts =
      { "-match-rule", "first@@MR@@f@@MR@@f@@MR@@quick@@MR@@Slow" };
    replacer.setOptions(opts);
    Instances data = getData(s_test1);

    replacer.setInputFormat(data);

    Instances outFormat = replacer.getOutputFormat();
    if (!outFormat.equalHeaders(data)) {
      throw new WekaException(outFormat.equalHeadersMsg(data));
    }

    replacer.input(data.instance(0));
    Instance firstTransformed = replacer.output();
    assertFalse(firstTransformed.stringValue(0).contains("slow"));

    replacer.input(data.instance(1));
    Instance secondTransformed = replacer.output();
    System.err.println(secondTransformed);
    assertFalse(secondTransformed.stringValue(0).contains("slow"));
  }

  @Test
  public void testRegexOneRule() throws Exception {
    SubstringReplacer replacer = new SubstringReplacer();

    String[] opts = { "-match-rule", "first@@MR@@t@@MR@@t@@MR@@dog$@@MR@@cat" };
    replacer.setOptions(opts);
    Instances data = getData(s_test1);

    replacer.setInputFormat(data);

    Instances outFormat = replacer.getOutputFormat();
    if (!outFormat.equalHeaders(data)) {
      throw new WekaException(outFormat.equalHeadersMsg(data));
    }

    replacer.input(data.instance(0));
    Instance firstTransformed = replacer.output();
    assertTrue(firstTransformed.stringValue(0).contains("lazy cat"));
    assertTrue(firstTransformed.stringValue(0).contains("brown dog"));

    replacer.input(data.instance(1));
    Instance secondTransformed = replacer.output();
    assertFalse(secondTransformed.stringValue(0).contains("cat"));
  }

  public static void main(String[] args) {
    try {
      SubstringReplacerTest t = new SubstringReplacerTest();
      t.testNonRegexOneRuleTwoAtts();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
