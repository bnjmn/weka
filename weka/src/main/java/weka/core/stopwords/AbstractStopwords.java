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
 * AbstractStopwords.java
 * Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 */

package weka.core.stopwords;

import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

/**
 * Ancestor for stopwords classes.
 *
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public abstract class AbstractStopwords
  implements OptionHandler, StopwordsHandler, Serializable {

  /** for serialization. */
  private static final long serialVersionUID = -1975256329586388142L;

  /** whether the scheme has been initialized. */
  protected boolean m_Initialized;

  /** debugging flag. */
  protected boolean m_Debug;

  /**
   * Returns a string describing the stopwords scheme.
   *
   * @return a description suitable for displaying in the gui
   */
  public abstract String globalInfo();

  /**
   * Resets the scheme and the initialized state.
   */
  protected void reset() {
    m_Initialized = false;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.addElement(new Option(
      "\tIf set, stopword scheme is run in debug mode and\n"
        + "\tmay output additional info to the console",
        "D", 0, "-D"));

    return result.elements();
  }

  /**
   * Parses a given list of options.
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    setDebug(Utils.getFlag("D", options));
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {
    List<String> options = new ArrayList<String>();

    if (getDebug()) {
      options.add("-D");
    }

    return options.toArray(new String[options.size()]);
  }

  /**
   * Set debugging mode.
   *
   * @param debug true if debug output should be printed
   */
  public void setDebug(boolean debug) {
    m_Debug = debug;
    reset();
  }

  /**
   * Get whether debugging is turned on.
   *
   * @return true if debugging output is on
   */
  public boolean getDebug() {
    return m_Debug;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String debugTipText() {
    return "If set to true, stopwords scheme may output additional info to "
      + "the console.";
  }

  /**
   * Outputs the error message, prefixed with classname.
   *
   * @param msg		the message to output
   */
  protected void error(String msg) {
    System.err.println(getClass().getName() + "-ERROR: " + msg);
  }

  /**
   * Outputs the debug message, prefixed with classname.
   *
   * @param msg		the message to output
   */
  protected void debug(String msg) {
    System.err.println(getClass().getName() + "-DEBUG: " + msg);
  }

  /**
   * Performs intialization of the scheme.
   * <p/>
   * Default implementation does nothing.
   */
  protected void initialize() {
  }

  /**
   * Returns true if the given string is a stop word.
   *
   * @param word the word to test
   * @return true if the word is a stopword
   */
  protected abstract boolean is(String word);

  /**
   * Returns true if the given string is a stop word.
   *
   * @param word the word to test
   * @return true if the word is a stopword
   */
  @Override
  public boolean isStopword(String word) {
    boolean	result;

    if (!m_Initialized) {
      if (m_Debug)
	debug("Initializing stopwords");
      initialize();
      m_Initialized = true;
    }

    result = is(word);
    if (m_Debug)
      debug(word + " --> " + result);

    return result;
  }
}
