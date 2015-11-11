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
 *    DictionaryBuilderTest.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test cases for the DictionaryBuilder class
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 */
public class DictionaryBuilderTest extends TestCase {

  // one string, no class
  protected static final String DATA1 = "@relation test1\n"
    + "@attribute text1 string\n" + "@data\n"
    + "'the quick brown fox jumped over the lazy turnip.'\n"
    + "'the slow sherman tank drove over the animated vw beetle'\n";

  // one string + class
  protected static final String DATA2 = "@relation test2\n"
    + "@attribute text1 string\n" + "@attribute class {one,two}\n" + "@data\n"
    + "'the quick brown fox jumped over the lazy turnip.',one\n"
    + "'the slow sherman tank drove over the animated vw beetle',two\n";

  // one string plus a few extra attributes, no class
  protected static final String DATA3 = "@relation test3\n"
    + "@attribute first numeric\n" + "@attribute second numeric\n"
    + "@attribute text1 string\n" + "@data\n"
    + "1,2,'the quick brown fox jumped over the lazy turnip.'\n"
    + "3,4,'the slow sherman tank drove over the animated vw beetle'\n";

  // one string, no class
  protected static final String DATA4 = "@relation test1\n"
    + "@attribute text1 string\n" + "@data\n"
    + "'the quick brown fox jumped over the lazy armchair.'\n"
    + "'the slow sherman tank drove over the animated unicycle'\n";

  protected Instances getData1() throws Exception {
    Instances data1 = new Instances(new StringReader(DATA1));

    return data1;
  }

  protected Instances getData2() throws Exception {
    Instances data2 = new Instances(new StringReader(DATA2));

    data2.setClassIndex(1);
    return data2;
  }

  protected Instances getData3() throws Exception {
    Instances data3 = new Instances(new StringReader(DATA3));

    return data3;
  }

  protected Instances getData4() throws Exception {
    Instances data4 = new Instances(new StringReader(DATA4));

    return data4;
  }

  public DictionaryBuilderTest(String name) {
    super(name);
  }

  public void testInit() throws Exception {
    Instances data1 = getData1();
    Instances structure = new Instances(data1, 0);

    DictionaryBuilder builder = new DictionaryBuilder();
    builder.setMinTermFreq(2);
    builder.setup(structure);

    // should be just one dictionary (i.e. no class attribute, so no per-class
    // dictionaries)
    assertEquals(1, builder.getDictionaries(false).length);
  }

  public void testTypicalNoClass() throws Exception {
    Instances data1 = getData1();
    Instances structure = new Instances(data1, 0);

    DictionaryBuilder builder = new DictionaryBuilder();
    builder.setMinTermFreq(2);
    builder.setup(structure);

    for (int i = 0; i < data1.numInstances(); i++) {
      builder.processInstance(data1.instance(i));
    }

    assertEquals(15, builder.getDictionaries(false)[0].size());

    // check a couple of words
    assertTrue(builder.getDictionaries(false)[0].get("the") != null);

    // word count (index 0) should be 4
    assertEquals(4, builder.getDictionaries(false)[0].get("the")[0]);

    // doc count (index 1) should be 2
    assertEquals(2, builder.getDictionaries(false)[0].get("the")[1]);
  }

  public void testFinalizeDictionaryNoClass() throws Exception {
    Instances data1 = getData1();
    Instances structure = new Instances(data1, 0);

    DictionaryBuilder builder = new DictionaryBuilder();
    builder.setMinTermFreq(2);
    builder.setup(structure);

    for (int i = 0; i < data1.numInstances(); i++) {
      builder.processInstance(data1.instance(i));
    }

    assertEquals(15, builder.getDictionaries(false)[0].size());

    Map<String, int[]> consolidated = builder.finalizeDictionary();

    // all but "the" and "over" should have been pruned from the dictionary
    // according to the default min freq of 2
    assertEquals(2, consolidated.size());
  }

  public void testPruneMinFreq() throws Exception {
    Instances data1 = getData1();
    Instances structure = new Instances(data1, 0);

    DictionaryBuilder builder = new DictionaryBuilder();
    builder.setMinTermFreq(1);
    builder.setup(structure);

    for (int i = 0; i < data1.numInstances(); i++) {
      builder.processInstance(data1.instance(i));
    }

    assertEquals(15, builder.getDictionaries(false)[0].size());

    Map<String, int[]> consolidated = builder.finalizeDictionary();

    // min freq of 1 should keep all terms
    assertEquals(15, consolidated.size());
  }

  public void testGetVectorizedStructureNoClass() throws Exception {
    Instances data1 = getData1();
    Instances structure = new Instances(data1, 0);

    DictionaryBuilder builder = new DictionaryBuilder();
    builder.setMinTermFreq(2);
    builder.setup(structure);

    for (int i = 0; i < data1.numInstances(); i++) {
      builder.processInstance(data1.instance(i));
    }

    builder.finalizeDictionary();

    Instances format = builder.getVectorizedFormat();
    assertTrue(format != null);
    assertEquals(2, format.numAttributes());
  }

  public void testVectorizeInstanceWordPresenceNoClass() throws Exception {
    Instances data1 = getData1();
    Instances structure = new Instances(data1, 0);

    DictionaryBuilder builder = new DictionaryBuilder();
    builder.setMinTermFreq(2);
    builder.setup(structure);

    for (int i = 0; i < data1.numInstances(); i++) {
      builder.processInstance(data1.instance(i));
    }

    builder.finalizeDictionary();

    Instance vectorized = builder.vectorizeInstance(data1.instance(0));
    assertEquals(2, vectorized.numAttributes());

    // values of the two attributes should be 1 (presence indicators)
    assertEquals(1, (int) vectorized.value(0));
    assertEquals(1, (int) vectorized.value(1));
  }

  public void testVectorizeInstanceWordCountsNoClass() throws Exception {
    Instances data1 = getData1();
    Instances structure = new Instances(data1, 0);

    DictionaryBuilder builder = new DictionaryBuilder();
    builder.setMinTermFreq(2);
    builder.setOutputWordCounts(true);
    builder.setup(structure);

    for (int i = 0; i < data1.numInstances(); i++) {
      builder.processInstance(data1.instance(i));
    }

    builder.finalizeDictionary();

    Instance vectorized = builder.vectorizeInstance(data1.instance(0));
    assertEquals(2, vectorized.numAttributes());

    // "the" occurs twice in the first index and "over" once
    assertEquals(2, (int) vectorized.value(0));
    assertEquals(1, (int) vectorized.value(1));
  }

  public void testTypicalNoClassExtraAtts() throws Exception {
    Instances data1 = getData3();
    Instances structure = new Instances(data1, 0);

    DictionaryBuilder builder = new DictionaryBuilder();
    builder.setMinTermFreq(2);
    builder.setup(structure);

    for (int i = 0; i < data1.numInstances(); i++) {
      builder.processInstance(data1.instance(i));
    }

    assertEquals(15, builder.getDictionaries(false)[0].size());

    // check a couple of words
    assertTrue(builder.getDictionaries(false)[0].get("the") != null);

    // word count (index 0) should be 4
    assertEquals(4, builder.getDictionaries(false)[0].get("the")[0]);

    // doc count (index 1) should be 2
    assertEquals(2, builder.getDictionaries(false)[0].get("the")[1]);
  }

  public void testFinalizeDictionaryNoClassExtraAtts() throws Exception {
    Instances data1 = getData3();
    Instances structure = new Instances(data1, 0);

    DictionaryBuilder builder = new DictionaryBuilder();
    builder.setMinTermFreq(2);
    builder.setup(structure);

    for (int i = 0; i < data1.numInstances(); i++) {
      builder.processInstance(data1.instance(i));
    }

    assertEquals(15, builder.getDictionaries(false)[0].size());

    Map<String, int[]> consolidated = builder.finalizeDictionary();

    // all but "the" and "over" should have been pruned from the dictionary
    assertEquals(2, consolidated.size());
  }

  public void testGetVectorizedStructureNoClassExtraAtts() throws Exception {
    Instances data1 = getData3();
    Instances structure = new Instances(data1, 0);

    DictionaryBuilder builder = new DictionaryBuilder();
    builder.setMinTermFreq(2);
    builder.setup(structure);

    for (int i = 0; i < data1.numInstances(); i++) {
      builder.processInstance(data1.instance(i));
    }

    builder.finalizeDictionary();

    Instances format = builder.getVectorizedFormat();
    assertTrue(format != null);
    assertEquals(4, format.numAttributes());
  }

  public void testTypicalClassAttPresent() throws Exception {
    Instances data2 = getData2();
    Instances structure = new Instances(data2, 0);

    DictionaryBuilder builder = new DictionaryBuilder();
    builder.setMinTermFreq(2);
    builder.setup(structure);

    for (int i = 0; i < data2.numInstances(); i++) {
      builder.processInstance(data2.instance(i));
    }

    // should be two dictionaries (one for each class)
    assertEquals(2, builder.getDictionaries(false).length);

    assertEquals(8, builder.getDictionaries(false)[0].size());
    assertEquals(9, builder.getDictionaries(false)[1].size());

    // check a couple of words
    assertTrue(builder.getDictionaries(false)[0].get("the") != null);

    // first dictionary: word count (index 0) should be 2
    assertEquals(2, builder.getDictionaries(false)[0].get("the")[0]);

    // first dictionary: doc count (index 1) should be 1
    assertEquals(1, builder.getDictionaries(false)[0].get("the")[1]);
  }

  public void testAggregateDictionaries() throws Exception {
    Instances data1 = getData1();
    Instances data4 = getData4();

    Instances structure = new Instances(data1, 0);

    DictionaryBuilder builder = new DictionaryBuilder();
    builder.setMinTermFreq(1);
    builder.setup(structure);

    for (int i = 0; i < data1.numInstances(); i++) {
      builder.processInstance(data1.instance(i));
    }

    Instances structure2 = new Instances(data4, 0);

    DictionaryBuilder builder2 = new DictionaryBuilder();
    builder2.setMinTermFreq(1);
    builder2.setup(structure2);

    for (int i = 0; i < data4.numInstances(); i++) {
      builder2.processInstance(data4.instance(i));
    }

    builder = builder.aggregate(builder2);

    builder.finalizeAggregation();
    Map<String, int[]> consolidated = builder.finalizeDictionary();
    assertEquals(17, consolidated.size());
  }

  public void testSaveLoadDictionaryPlainTextNoNormalize() throws Exception {
    Instances data1 = getData1();
    Instances structure = new Instances(data1, 0);

    DictionaryBuilder builder = new DictionaryBuilder();
    builder.setMinTermFreq(2);
    builder.setup(structure);

    for (int i = 0; i < data1.numInstances(); i++) {
      builder.processInstance(data1.instance(i));
    }
    builder.finalizeDictionary();

    StringWriter sw = new StringWriter();
    builder.saveDictionary(sw);

    StringReader sr = new StringReader(sw.toString());
    DictionaryBuilder builder2 = new DictionaryBuilder();
    builder2.setup(structure);
    builder2.loadDictionary(sr);

    // just returns the loaded dictionary
    Map<String, int[]> consolidated = builder2.finalizeDictionary();

    assertEquals(2, consolidated.size());
  }

  public void testSaveLoadDictionaryPlainTextNormalize() throws Exception {
    Instances data1 = getData1();
    Instances structure = new Instances(data1, 0);

    DictionaryBuilder builder = new DictionaryBuilder();
    builder.setMinTermFreq(2);
    builder.setNormalize(true);
    builder.setup(structure);

    for (int i = 0; i < data1.numInstances(); i++) {
      builder.processInstance(data1.instance(i));
    }
    builder.finalizeDictionary();

    StringWriter sw = new StringWriter();
    builder.saveDictionary(sw);
    String dictText = sw.toString();
    assertTrue(dictText.startsWith("@@@3.39036"));

    StringReader sr = new StringReader(dictText);
    DictionaryBuilder builder2 = new DictionaryBuilder();
    builder2.setup(structure);
    builder2.loadDictionary(sr);

    // just returns the loaded dictionary
    Map<String, int[]> consolidated = builder2.finalizeDictionary();

    assertEquals(2, consolidated.size());
  }

  public void testListOptions() {
    CheckOptionHandler optionHandler = new CheckOptionHandler();
    DictionaryBuilder builder = new DictionaryBuilder();
    optionHandler.setOptionHandler(builder);

    if (!optionHandler.checkListOptions()) {
      fail("Options cannot be listed via listOptions");
    }
  }

  public void testSetOptions() {
    CheckOptionHandler optionHandler = new CheckOptionHandler();
    DictionaryBuilder builder = new DictionaryBuilder();
    optionHandler.setOptionHandler(builder);

    if (!optionHandler.checkSetOptions()) {
      fail("setOptions method failed");
    }
  }

  public void testCanonicalUserOptions() {
    CheckOptionHandler optionHandler = new CheckOptionHandler();
    DictionaryBuilder builder = new DictionaryBuilder();
    optionHandler.setOptionHandler(builder);

    if (!optionHandler.checkCanonicalUserOptions()) {
      fail("setOptions method failed");
    }
  }

  public void testResettingOptions() {
    CheckOptionHandler optionHandler = new CheckOptionHandler();
    DictionaryBuilder builder = new DictionaryBuilder();
    optionHandler.setOptionHandler(builder);

    if (!optionHandler.checkResettingOptions()) {
      fail("Resetting of options failed");
    }
  }

  public void testRemainingOptions() {
    CheckOptionHandler optionHandler = new CheckOptionHandler();
    DictionaryBuilder builder = new DictionaryBuilder();
    optionHandler.setOptionHandler(builder);

    if (!optionHandler.checkRemainingOptions()) {
      fail("There were leftover options");
    }
  }

  public static Test suite() {
    return new TestSuite(DictionaryBuilderTest.class);
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run( suite() );
  }
}
