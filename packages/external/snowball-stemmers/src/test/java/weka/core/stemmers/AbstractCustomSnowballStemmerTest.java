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
 * AbstractCustomSnowballStemmerTest.java
 * Copyright (C) 2015 University of Waikato, Hamilton, NZ
 */

package weka.core.stemmers;

import junit.framework.TestCase;
import weka.core.CheckGOE;
import weka.core.CheckOptionHandler;
import weka.core.OptionHandler;
import weka.core.SerializationHelper;
import weka.test.Regression;
import weka.tests.TestHelper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.StringTokenizer;

/**
 * Ancestor for test cases for SnowballStemmer classes.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public abstract class AbstractCustomSnowballStemmerTest
  extends TestCase {

  /** data for the regression tests */
  protected String[] m_Data;

  /** The stopwords scheme to be tested */
  protected Stemmer m_Stemmer;

  /** the OptionHandler tester */
  protected CheckOptionHandler m_OptionTester;

  /** for testing GOE stuff */
  protected CheckGOE m_GOETester;

  /**
   * Constructs the <code>AbstractStopwordsTest</code>. Called by subclasses.
   *
   * @param name the name of the test
   */
  public AbstractCustomSnowballStemmerTest(String name) {
    super(name);
  }

  /**
   * Returns the tmp directory.
   *
   * @return		the tmp directory
   */
  public String getTmpDirectory() {
    return System.getProperty("java.io.tmpdir");
  }

  /**
   * Returns the location in the tmp directory for given resource.
   *
   * @param resource	the resource (path in project) to get the tmp location for
   * @return		the tmp location
   * @see		#getTmpDirectory()
   */
  public String getTmpLocationFromResource(String resource) {
    String	result;
    File file;

    file   = new File(resource);
    result = getTmpDirectory() + File.separator + file.getName();

    return result;
  }

  /**
   * Returns the data directory.
   *
   * @return		the directory
   */
  protected String getDataDirectory() {
    return "weka/core/stemmers";
  }

  /**
   * Copies the given resource to the tmp directory.
   *
   * @param resource	the resource (path in project) to copy
   * @return		false if copying failed
   * @see		#getTmpLocationFromResource(String)
   */
  public boolean copyResourceToTmp(String resource) {
    boolean			result;
    BufferedInputStream input;
    BufferedOutputStream output;
    byte[]			buffer;
    int				read;

    input    = null;
    output   = null;
    resource = getDataDirectory() + "/" + resource;

    try {
      input  = new BufferedInputStream(ClassLoader.getSystemResourceAsStream(resource));
      output = new BufferedOutputStream(new FileOutputStream(getTmpLocationFromResource(resource)));
      buffer = new byte[1024];
      while ((read = input.read(buffer)) != -1) {
	output.write(buffer, 0, read);
	if (read < buffer.length)
	  break;
      }
      result = true;
    }
    catch (IOException e) {
      if (e.getMessage().equals("Stream closed")) {
	System.err.println("Resource '" + resource + "' not available?");
      }
      e.printStackTrace();
      result = false;
    }
    catch (Exception e) {
      e.printStackTrace();
      result = false;
    }

    if (input != null) {
      try {
	input.close();
      }
      catch (Exception e) {
	// ignored
      }
    }
    if (output != null) {
      try {
	output.close();
      }
      catch (Exception e) {
	// ignored
      }
    }

    return result;
  }

  /**
   * Removes the file from the tmp directory.
   *
   * @param filename	the file in the tmp directory to delete (no path!)
   * @return		true if deleting succeeded or file not present
   */
  public boolean deleteFileFromTmp(String filename) {
    boolean	result;
    File	file;

    result = true;
    file   = new File(getTmpDirectory() + File.separator + filename);
    if (file.exists())
      result = file.delete();

    return result;
  }

  /**
   * returns the data to use in the tests
   *
   * @return the data to use in the tests
   */
  protected abstract String[] getData();

  /**
   * Configures the CheckOptionHandler uses for testing the option handling.
   * Sets the stopwords algorithm returned from the getStopwords() method if that can
   * handle options.
   *
   * @return the fully configured CheckOptionHandler
   * @see #getStemmer()
   */
  protected CheckOptionHandler getOptionTester() {
    CheckOptionHandler result;

    result = new CheckOptionHandler();
    if (getStemmer() instanceof OptionHandler) {
      result.setOptionHandler((OptionHandler) getStemmer());
    } else {
      result.setOptionHandler(null);
    }
    result.setUserOptions(new String[0]);
    result.setSilent(true);

    return result;
  }

  /**
   * Configures the CheckGOE used for testing GOE stuff. Sets the Stopwords
   * returned from the getStopwords() method.
   *
   * @return the fully configured CheckGOE
   * @see #getStemmer()
   */
  protected CheckGOE getGOETester() {
    CheckGOE result;

    result = new CheckGOE();
    result.setObject(getStemmer());
    result.setSilent(true);

    return result;
  }

  /**
   * Called by JUnit before each test method. This implementation creates the
   * default stopwords algorithm to test and loads a test set of Instances.
   *
   * @exception Exception if an error occurs reading the example instances.
   */
  @SuppressWarnings("unchecked")
  @Override
  protected void setUp() throws Exception {
    TestHelper.setRegressionRoot();
    m_Stemmer = getStemmer();
    m_OptionTester = getOptionTester();
    m_GOETester = getGOETester();
    m_Data = getData();
  }

  /** Called by JUnit after each test method */
  @Override
  protected void tearDown() {
    m_Stemmer = null;
    m_OptionTester = null;
    m_GOETester = null;
    m_Data = null;
  }

  /**
   * Used to create an instance of a specific stopwords scheme.
   *
   * @return a suitably configured <code>Stemmer</code> value
   */
  public abstract Stemmer getStemmer();

  /**
   * tests whether the scheme declares a serialVersionUID.
   */
  public void testSerialVersionUID() {
    boolean result;

    result = !SerializationHelper.needsUID(m_Stemmer.getClass());

    if (!result) {
      fail("Doesn't declare serialVersionUID!");
    }
  }

  /**
   * Tokenizes a string.
   *
   * @param s		the string to tokenize
   * @return		the tokens
   */
  protected String[] tokenize(String s) {
    StringTokenizer tok;
    String[] data;
    int i;

    tok = new StringTokenizer(s, " \t\n\r\f,.!?");
    data = new String[tok.countTokens()];
    for (i = 0; i < data.length; i++)
      data[i] = tok.nextToken();

    return data;
  }

  /**
   * tests whether the stemmer algorithm correctly initializes.
   */
  public void testBuildInitialization() {
    boolean result;
    int i;
    int n;
    int m;
    String[][][] processed;
    String[] data;
    String msg;

    // process data twice
    processed = new String[2][m_Data.length][];
    for (n = 0; n < 2; n++) {
      for (i = 0; i < m_Data.length; i++) {
        try {
          data = tokenize(m_Data[i]);
          processed[n][i] = new String[data.length];
          for (m = 0; m < data.length; m++)
            processed[n][i][m] = m_Stemmer.stem(data[m]);
        } catch (Exception e) {
          processed[n][i] = new String[]{null};
        }
      }
    }

    // was the same data produced?
    result = true;
    msg = "";
    for (i = 0; i < m_Data.length; i++) {
      if (processed[0].length == processed[1].length) {
        for (n = 0; n < processed[0][i].length; n++) {
          if (!processed[0][i][n].equals(processed[1][i][n])) {
            result = false;
            msg = "different stemming results";
            break;
          }
        }
      } else {
        result = false;
        msg = "different number of words";
        break;
      }
    }

    if (!result) {
      fail("Incorrect build initialization (" + msg + ")!");
    }
  }

  /**
   * Runs the stopwords algorithm over the given tokens and returns the result.
   *
   * @param tokens the tokens to analyze
   * @return the results for the tokens (stopwords yes/no)
   * @throws Exception if tokenization/analysis fails
   */
  protected String[] useStemmer(String[] tokens) throws Exception {
    String[] result;
    int i;

    result = new String[tokens.length];

    for (i = 0; i < tokens.length; i++)
      result[i] = m_Stemmer.stem(tokens[i]);

    return result;

  }

  /**
   * Returns a string containing all the results.
   *
   * @param tokens the string tokens
   * @param results the results for the tokens (stemmed words)
   * @return a <code>String</code> representing the tokens/results
   */
  protected String predictionsToString(String[] tokens, String[] results) {
    StringBuilder sb = new StringBuilder();

    sb.append(tokens.length).append(" words\n");
    for (int i = 0; i < tokens.length; i++) {
      sb.append(tokens[i]).append(" --> ").append(results[i]).append('\n');
    }

    return sb.toString();
  }

  /**
   * Runs a regression test -- this checks that the output of the tested object
   * matches that in a reference version. When this test is run without any
   * pre-existing reference output, the reference version is created.
   */
  public void testRegression() {
    int i;
    boolean succeeded;
    Regression reg;
    String[] tokens;
    String[] results;

    reg = new Regression(this.getClass());
    succeeded = false;

    for (i = 0; i < m_Data.length; i++) {
      try {
	tokens = tokenize(m_Data[i]);
	results = useStemmer(tokens);
        succeeded = true;
        reg.println(predictionsToString(tokens, results));
      } catch (Exception e) {
	results = new String[0];
      }
    }

    if (!succeeded) {
      fail("Problem during regression testing: no successful results generated for any string");
    }

    try {
      String diff = reg.diff();
      if (diff == null) {
        System.err.println("Warning: No reference available, creating.");
      } else if (!diff.equals("")) {
        fail("Regression test failed. Difference:\n" + diff);
      }
    } catch (IOException ex) {
      fail("Problem during regression testing.\n" + ex);
    }
  }

  /**
   * tests the listing of the options
   */
  public void testListOptions() {
    if (m_OptionTester.getOptionHandler() != null) {
      if (!m_OptionTester.checkListOptions()) {
        fail("Options cannot be listed via listOptions.");
      }
    }
  }

  /**
   * tests the setting of the options
   */
  public void testSetOptions() {
    if (m_OptionTester.getOptionHandler() != null) {
      if (!m_OptionTester.checkSetOptions()) {
        fail("setOptions method failed.");
      }
    }
  }

  /**
   * tests whether the default settings are processed correctly
   */
  public void testDefaultOptions() {
    if (m_OptionTester.getOptionHandler() != null) {
      if (!m_OptionTester.checkDefaultOptions()) {
        fail("Default options were not processed correctly.");
      }
    }
  }

  /**
   * tests whether there are any remaining options
   */
  public void testRemainingOptions() {
    if (m_OptionTester.getOptionHandler() != null) {
      if (!m_OptionTester.checkRemainingOptions()) {
        fail("There were 'left-over' options.");
      }
    }
  }

  /**
   * tests the whether the user-supplied options stay the same after setting.
   * getting, and re-setting again.
   *
   * @see #getOptionTester()
   */
  public void testCanonicalUserOptions() {
    if (m_OptionTester.getOptionHandler() != null) {
      if (!m_OptionTester.checkCanonicalUserOptions()) {
        fail("setOptions method failed");
      }
    }
  }

  /**
   * tests the resetting of the options to the default ones
   */
  public void testResettingOptions() {
    if (m_OptionTester.getOptionHandler() != null) {
      if (!m_OptionTester.checkSetOptions()) {
        fail("Resetting of options failed");
      }
    }
  }

  /**
   * tests for a globalInfo method
   */
  public void testGlobalInfo() {
    if (!m_GOETester.checkGlobalInfo()) {
      fail("No globalInfo method");
    }
  }

  /**
   * tests the tool tips
   */
  public void testToolTips() {
    if (!m_GOETester.checkToolTips()) {
      fail("Tool tips inconsistent");
    }
  }
}
