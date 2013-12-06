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
 *   Generator.java
 *   Copyright (C) 2008 K.Hempstalk, University of Waikato, Hamilton, New Zealand.
 */

package weka.classifiers.meta.generators;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;

/**
 * An artificial data generator.
 * 
 * @author Kathryn Hempstalk (kah18 at cs.waikato.ac.nz)
 * @version $Revision$
 */
public abstract class Generator implements Serializable, OptionHandler {

  /** for serialization. */
  private static final long serialVersionUID = 2412127331483792089L;

  /** Whether the generator is run in debug mode. */
  protected boolean m_Debug = false;

  /**
   * Gets the probability that a value falls under this distribution.
   * 
   * 
   * @param somedata The value to get the probability of.
   * @return The probability of the given value.
   */
  public abstract double getProbabilityOf(double somedata);

  /**
   * Gets the (natural) log of the probability of a given value.
   * 
   * @param somedata The value to get the log probability of.
   * @return The (natural) log of the probability.
   */
  public abstract double getLogProbabilityOf(double somedata);

  /**
   * Generates a value that falls under this distribution.
   * 
   * @return A generated value.
   */
  public abstract double generate();

  /**
   * Clones this generator. It is a shallow copy, only settings are copied, not
   * probabilities.
   * 
   * @return A copy of this generator.
   */
  public Generator copy() {
    Generator result;

    try {
      result = getClass().newInstance();
      result.setOptions(getOptions());
    } catch (Exception e) {
      e.printStackTrace();
      result = null;
    }

    return result;
  }

  /**
   * Creates a new instance of a generator given it's class name and (optional)
   * arguments to pass to it's setOptions method. If the classifier implements
   * OptionHandler and the options parameter is non-null, the classifier will
   * have it's options set.
   * 
   * @param generatorName the fully qualified class name of the generator
   * @param options an array of options suitable for passing to setOptions. May
   *          be null.
   * @return the newly created classifier, ready for use.
   * @throws Exception if the classifier name is invalid, or the options
   *           supplied are not acceptable to the classifier
   */
  public static Generator forName(String generatorName, String[] options)
    throws Exception {

    return (Generator) Utils.forName(Generator.class, generatorName, options);
  }

  /**
   * Returns a string describing this class' ability.
   * 
   * @return A description of the class.
   */
  public abstract String globalInfo();

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> result = new Vector<Option>();

    result.addElement(new Option(
      "\tIf set, generator is run in debug mode and\n"
        + "\tmay output additional info to the console", "output-debug-info",
      0, "-output-debug-info"));

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

    setDebug(Utils.getFlag("output-debug-info", options));
  }

  /**
   * Gets the current settings of the Classifier.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> result = new Vector<String>();

    if (getDebug()) {
      result.add("-output-debug-info");
    }

    return result.toArray(new String[result.size()]);
  }

  /**
   * Set debugging mode.
   * 
   * @param debug true if debug output should be printed
   */
  public void setDebug(boolean debug) {
    m_Debug = debug;
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
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String debugTipText() {
    return "If set to true, the generator might output debugging information "
      + "in the console.";
  }
}
