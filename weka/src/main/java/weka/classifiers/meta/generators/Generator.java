/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/**
 *   Generator.java
 *   Copyright (C) 2008 K.Hempstalk, University of Waikato, Hamilton, New Zealand.
 */

package weka.classifiers.meta.generators;

import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

/**
 * An artificial data generator.
 *
 * @author Kathryn Hempstalk (kah18 at cs.waikato.ac.nz)
 * @version $Revision$
 */
public abstract class Generator 
  implements Serializable, OptionHandler {

  /** for serialization. */
  private static final long serialVersionUID = 2412127331483792089L;

  /** Whether the generator is run in debug mode. */
  protected boolean m_Debug = false;

  /**
   * Gets the probability that a value falls under
   * this distribution.
   * 
   *
   * @param valuex The value to get the probability of.
   * @return The probability of the given value.
   */
  public abstract double getProbabilityOf(double somedata);

  /**
   * Gets the (natural) log of the probability of a given value.
   *
   * @param valuex The value to get the log probability of.
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
   * Clones this generator.  It is a shallow copy,
   * only settings are copied, not probabilities.
   *
   * @return A copy of this generator.
   */
  public Generator copy() {
    Generator	result;
    
    try {
      result = getClass().newInstance();
      result.setOptions(getOptions());
    }
    catch (Exception e) {
      e.printStackTrace();
      result = null;
    }
    
    return result;
  }

  /**
   * Creates a new instance of a generator given it's class name and
   * (optional) arguments to pass to it's setOptions method. If the
   * classifier implements OptionHandler and the options parameter is
   * non-null, the classifier will have it's options set.
   *
   * @param generatorName the fully qualified class name of the generator
   * @param options an array of options suitable for passing to setOptions. May
   * be null.
   * @return the newly created classifier, ready for use.
   * @throws Exception if the classifier name is invalid, or the options
   * supplied are not acceptable to the classifier
   */
  public static Generator forName(String generatorName,
      String[] options) throws Exception {

    return (Generator)Utils.forName(Generator.class,
	generatorName,
	options);
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
  public Enumeration listOptions() {
    Vector result = new Vector();

    result.addElement(new Option(
	"\tIf set, generator is run in debug mode and\n"
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
  public void setOptions(String[] options) throws Exception {
    setDebug(Utils.getFlag('D', options));
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    Vector<String>	result;
    
    result = new Vector<String>();
    
    if (getDebug())
      result.add("-D");

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
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String debugTipText() {
    return 
        "If set to true, the generator might output debugging information "
      + "in the console.";
  }
}
