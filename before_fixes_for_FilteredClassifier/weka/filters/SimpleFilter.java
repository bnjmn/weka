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

/*
 * SimpleFilter.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */


package weka.filters;

import weka.filters.Filter;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;

import java.util.Enumeration;
import java.util.Vector;

/** 
 * This filter contains common behavior of the SimpleBatchFilter and the
 * SimpleStreamFilter.
 *
 * @author  FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.2 $
 * @see     SimpleBatchFilter 
 * @see     SimpleStreamFilter 
 */
public abstract class SimpleFilter
  extends Filter 
  implements OptionHandler {

  /** Whether debugging is on */
  protected boolean m_Debug = false;

  /** Whether the first batch has been processed already. Useful, if the
   * second batch of files depends on data collected in the first batch. */
  protected boolean m_FirstBatchDone = false;
  
  /**
   * Returns a string describing this classifier.
   *
   * @return      a description of the classifier suitable for
   *              displaying in the explorer/experimenter gui
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
              "\tTurns on output of debugging information.",
              "D", 0, "-D"));

    return result.elements();
  }

  /**
   * Parses a list of options for this object. 
   * Also resets the state of the filter (this reset doesn't affect the 
   * options).
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   * @see    #reset()
   */
  public void setOptions(String[] options) throws Exception {
    reset();

    setDebug(Utils.getFlag('D', options));
  }

  /**
   * Gets the current settings of the filter.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    Vector        result;

    result = new Vector();

    if (getDebug())
      result.add("-D");

    return (String[]) result.toArray(new String[result.size()]);
  }

  /**
   * Sets the debugging mode
   *
   * @param value     if true, debugging information is output
   */
  public void setDebug(boolean value) {
    m_Debug = value;
  }

  /**
   * Returns the current debugging mode state.
   *
   * @return      true if debugging mode is on
   */
  public boolean getDebug() {
    return m_Debug;
  }
  
  /**
   * Returns the tip text for this property
   * @return    tip text for this property suitable for
   *            displaying in the explorer/experimenter gui
   */
  public String debugTipText() {
    return "Turns on output of debugging information.";
  }

  /**
   * resets the filter, i.e., m_NewBatch to true and m_FirstBatchDone to
   * false.
   *
   * @see #m_NewBatch
   * @see #m_FirstBatchDone
   */
  protected void reset() {
    m_NewBatch       = true;
    m_FirstBatchDone = false;
  }

  /**
   * Returns true if the a new batch was started, either a new instance of the 
   * filter was created or the batchFinished() method got called.
   * 
   * @return true if a new batch has been initiated
   * @see #m_NewBatch .
   * @see #batchFinished()
   */
  public boolean isNewBatch() {
    return m_NewBatch;
  }
  
  /**
   * Returns true if the first batch of instances got processed. Necessary for
   * supervised filters, which "learn" from the first batch and then shouldn't
   * get updated with subsequent calls of batchFinished().
   * 
   * @return true if the first batch has been processed
   * @see #m_FirstBatchDone
   * @see #batchFinished()
   */
  public boolean isFirstBatchDone() {
    return m_FirstBatchDone;
  }
  
  /**
   * returns true if the output format is immediately available after the
   * input format has been set and not only after all the data has been
   * seen (see batchFinished())
   *
   * @return      true if the output format is immediately available
   * @see         #batchFinished()
   * @see         #setInputFormat(Instances)
   */
  protected abstract boolean hasImmediateOutputFormat();
  
  /**
   * Determines the output format based on the input format and returns 
   * this. In case the output format cannot be returned immediately, i.e.,
   * immediateOutputFormat() returns false, then this method will be called
   * from batchFinished().
   *
   * @param inputFormat     the input format to base the output format on
   * @return                the output format
   * @throws Exception      in case the determination goes wrong
   * @see   #hasImmediateOutputFormat()
   * @see   #batchFinished()
   */
  protected abstract Instances determineOutputFormat(Instances inputFormat) throws Exception;

  /**
   * Processes the given data (may change the provided dataset) and returns
   * the modified version. This method is called in batchFinished().
   *
   * @param instances   the data to process
   * @return            the modified data
   * @throws Exception  in case the processing goes wrong
   * @see               #batchFinished()
   */
  protected abstract Instances process(Instances instances) throws Exception;
  
  /**
   * Sets the format of the input instances. 
   * Also resets the state of the filter (this reset doesn't affect the 
   * options).
   *
   * @param instanceInfo    an Instances object containing the input instance
   *                        structure (any instances contained in the object 
   *                        are ignored - only the structure is required).
   * @return                true if the outputFormat may be collected 
   *                        immediately
   * @see                   #reset()
   */
  public boolean setInputFormat(Instances instanceInfo) throws Exception {
    super.setInputFormat(instanceInfo);

    reset();
    
    if (hasImmediateOutputFormat())
      setOutputFormat(determineOutputFormat(instanceInfo));
      
    return hasImmediateOutputFormat();
  }
}

