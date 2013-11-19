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
 * SimpleFilter.java
 * Copyright (C) 2005-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters;

import java.util.Enumeration;
import java.util.Vector;

import weka.core.Capabilities;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;

/**
 * This filter contains common behavior of the SimpleBatchFilter and the
 * SimpleStreamFilter.
 * 
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 * @see SimpleBatchFilter
 * @see SimpleStreamFilter
 */
public abstract class SimpleFilter extends Filter implements OptionHandler {

  /** for serialization */
  private static final long serialVersionUID = 5702974949137433141L;

  /** Whether the filter is run in debug mode. */
  protected boolean m_Debug = false;

  /** Whether capabilities should not be checked when input format is set. */
  protected boolean m_DoNotCheckCapabilities = false;

  /**
   * Returns a string describing this filter.
   * 
   * @return a description of the filter suitable for displaying in the
   *         explorer/experimenter gui
   */
  public abstract String globalInfo();

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>(2);

    newVector.addElement(new Option(
      "\tIf set, filter is run in debug mode and\n"
        + "\tmay output additional info to the console", "output-debug-info",
      0, "-output-debug-info"));
    newVector.addElement(new Option(
      "\tIf set, filter capabilities are not checked when input format is set\n"
        + "\t(use with caution).", "-do-not-check-capabilities", 0,
      "-do-not-check-capabilities"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:
   * <p>
   * 
   * -D <br>
   * If set, filter is run in debug mode and may output additional info to the
   * console.
   * <p>
   * 
   * -do-not-check-capabilities <br>
   * If set, filter capabilities are not checked when input format is set (use
   * with caution).
   * <p>
   * 
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    setDebug(Utils.getFlag("output-debug-info", options));
    setDoNotCheckCapabilities(Utils.getFlag("do-not-check-capabilities",
      options));
  }

  /**
   * Gets the current settings of the filter.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> options = new Vector<String>();

    if (getDebug()) {
      options.add("-output-debug-info");
    }
    if (getDoNotCheckCapabilities()) {
      options.add("-do-not-check-capabilities");
    }

    return options.toArray(new String[0]);
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
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String debugTipText() {
    return "If set to true, filter may output additional info to "
      + "the console.";
  }

  /**
   * Set whether not to check capabilities.
   * 
   * @param doNotCheckCapabilities true if capabilities are not to be checked.
   */
  public void setDoNotCheckCapabilities(boolean doNotCheckCapabilities) {

    m_DoNotCheckCapabilities = doNotCheckCapabilities;
  }

  /**
   * Get whether capabilities checking is turned off.
   * 
   * @return true if capabilities checking is turned off.
   */
  public boolean getDoNotCheckCapabilities() {

    return m_DoNotCheckCapabilities;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String doNotCheckCapabilitiesTipText() {
    return "If set, filter capabilities are not checked when input format is set"
      + " (Use with caution to reduce runtime).";
  }

  /**
   * Returns the Capabilities of this filter. Maximally permissive capabilities
   * are allowed by default. Derived filters should override this method and
   * first disable all capabilities and then enable just those capabilities that
   * make sense for the scheme.
   * 
   * @return the capabilities of this object
   * @see Capabilities
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // Do we want to effectively turn off the testWithFail
    // method in Capabilities to save runtime()?
    result.setTestWithFailAlwaysSucceeds(getDoNotCheckCapabilities());

    return result;
  }

  /**
   * resets the filter, i.e., m_NewBatch to true and m_FirstBatchDone to false.
   * 
   * @see #m_NewBatch
   * @see #m_FirstBatchDone
   */
  protected void reset() {
    m_NewBatch = true;
    m_FirstBatchDone = false;
  }

  /**
   * returns true if the output format is immediately available after the input
   * format has been set and not only after all the data has been seen (see
   * batchFinished())
   * 
   * @return true if the output format is immediately available
   * @see #batchFinished()
   * @see #setInputFormat(Instances)
   */
  protected abstract boolean hasImmediateOutputFormat();

  /**
   * Determines the output format based on the input format and returns this. In
   * case the output format cannot be returned immediately, i.e.,
   * immediateOutputFormat() returns false, then this method will be called from
   * batchFinished().
   * 
   * @param inputFormat the input format to base the output format on
   * @return the output format
   * @throws Exception in case the determination goes wrong
   * @see #hasImmediateOutputFormat()
   * @see #batchFinished()
   */
  protected abstract Instances determineOutputFormat(Instances inputFormat)
    throws Exception;

  /**
   * Processes the given data (may change the provided dataset) and returns the
   * modified version. This method is called in batchFinished().
   * 
   * @param instances the data to process
   * @return the modified data
   * @throws Exception in case the processing goes wrong
   * @see #batchFinished()
   */
  protected abstract Instances process(Instances instances) throws Exception;

  /**
   * Sets the format of the input instances. Also resets the state of the filter
   * (this reset doesn't affect the options).
   * 
   * @param instanceInfo an Instances object containing the input instance
   *          structure (any instances contained in the object are ignored -
   *          only the structure is required).
   * @return true if the outputFormat may be collected immediately
   * @see #reset()
   */
  @Override
  public boolean setInputFormat(Instances instanceInfo) throws Exception {
    super.setInputFormat(instanceInfo);

    reset();

    if (hasImmediateOutputFormat()) {
      setOutputFormat(determineOutputFormat(instanceInfo));
    }

    return hasImmediateOutputFormat();
  }
}
