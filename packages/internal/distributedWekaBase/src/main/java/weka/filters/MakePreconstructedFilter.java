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
 *    MakePreconstructedFilter.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.gui.beans.KFIgnore;
import distributed.core.DistributedJobConfig;

/**
 * Class for wrapping a standard filter and making it a PreconstructedFilter.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
@KFIgnore
public class MakePreconstructedFilter extends Filter implements
  PreconstructedFilter, StreamableFilter, Serializable {

  /** For serialization */
  private static final long serialVersionUID = -6057003058954267357L;

  /** The filter to delegate to */
  protected Filter m_delegate;

  /** Whether this preconstructed filter has been reset */
  protected boolean m_isReset = true;

  /** True if we've done a check for string atts in the output format */
  protected boolean m_stringsChecked;

  /** True if the output format contains string atts */
  protected boolean m_hasStringAtts;

  public MakePreconstructedFilter() {
    super();
  }

  public MakePreconstructedFilter(Filter base) {
    setBaseFilter(base);
  }

  /**
   * List the options for this filter
   * 
   * @return an enumeration of options
   */
  public Enumeration listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.add(new Option("\tFilter to wrap + options.", "-filter", 1,
      "-filter <filter and options>"));

    return result.elements();
  }

  /**
   * Set the options for this filter
   * 
   * @param options the options
   * @throws Exception if a problem occurs
   */
  public void setOptions(String[] options) throws Exception {
    String filterSpec = Utils.getOption("filter", options);

    if (!DistributedJobConfig.isEmpty(filterSpec)) {
      String[] spec = Utils.splitOptions(filterSpec);
      String filterClass = spec[0];
      spec[0] = "";

      Filter base = (Filter) Class.forName(filterClass).newInstance();
      if (base instanceof OptionHandler) {
        ((OptionHandler) base).setOptions(spec);
      }

      setBaseFilter(base);
    }
  }

  /**
   * Set the base filter to wrap
   * 
   * @param f the base filter to wrap
   */
  public void setBaseFilter(Filter f) {
    if (!(f instanceof StreamableFilter)) {
      throw new IllegalArgumentException("Base filter must be Streamable!");
    }

    m_delegate = f;
  }

  /**
   * Get the base filter being wrapped
   * 
   * @return the base filter being wrapped
   */
  public Filter getBaseFilter() {
    return m_delegate;
  }

  @Override
  public Capabilities getCapabilities() {
    return m_delegate.getCapabilities();
  }

  @Override
  public Capabilities getCapabilities(Instances data) {
    return m_delegate.getCapabilities(data);
  }

  @Override
  public boolean mayRemoveInstanceAfterFirstBatchDone() {
    return m_delegate.mayRemoveInstanceAfterFirstBatchDone();
  }

  @Override
  public boolean isNewBatch() {
    return m_delegate.isNewBatch();
  }

  @Override
  public boolean isFirstBatchDone() {
    return m_delegate.isFirstBatchDone();
  }

  @Override
  public boolean setInputFormat(Instances insts) throws Exception {
    if (m_isReset) {
      m_isReset = false;
      return getBaseFilter().setInputFormat(insts);
    }

    return true;
  }

  @Override
  public Instances getInputFormat() {
    return getBaseFilter().getCopyOfInputFormat();
  }

  @Override
  public Instances getOutputFormat() {
    Instances outFormat = m_delegate.getOutputFormat();
    if (!m_stringsChecked) {
      m_hasStringAtts = outFormat.checkForStringAttributes();
      m_stringsChecked = true;
    }
    return outFormat;
  }

  @Override
  public boolean batchFinished() throws Exception {
    return getBaseFilter().batchFinished();
  }

  @Override
  public Instance output() {
    Instance outInst = m_delegate.output();
    if (m_hasStringAtts && outInst != null) {
      for (int i = 0; i < outInst.dataset().numAttributes(); i++) {
        if (outInst.dataset().attribute(i).isString() && !outInst.isMissing(i)) {
          String val = outInst.stringValue(i);
          outInst.attribute(i).setStringValue(val);
          outInst.setValue(i, 0);
        }
      }
    }

    return outInst;
  }

  @Override
  public boolean input(Instance inst) throws Exception {
    return m_delegate.input(inst);
  }

  @Override
  public boolean isOutputFormatDefined() {
    return m_delegate.isOutputFormatDefined();
  }

  @Override
  public int numPendingOutput() {
    return m_delegate.numPendingOutput();
  }

  @Override
  public Instance outputPeek() {
    return m_delegate.outputPeek();
  }

  @Override
  public boolean isConstructed() {
    return getBaseFilter() != null
      && getBaseFilter().getCopyOfInputFormat() != null && !m_isReset;
  }

  /**
   * Mark this pre-constructed filter as "constructed" - i.e. it is already
   * ready to use and has an input format set on the underlying filter.
   */
  public void setConstructed() {
    if (getBaseFilter() != null
      && getBaseFilter().getCopyOfInputFormat() != null) {
      m_isReset = false;
    }
  }

  @Override
  public void resetPreconstructed() {
    m_isReset = true;
  }
}
