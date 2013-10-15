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
import distributed.core.DistributedJobConfig;

public class MakePreconstructedFilter extends Filter implements
  PreconstructedFilter, StreamableFilter, Serializable {

  /** For serialization */
  private static final long serialVersionUID = -6057003058954267357L;

  protected Filter m_delegate;

  protected boolean m_isReset = true;

  public MakePreconstructedFilter() {
    super();
  }

  public MakePreconstructedFilter(Filter base) {
    setBaseFilter(base);
  }

  public Enumeration listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.add(new Option("\tFilter to wrap + options.", "-filter", 1,
      "-filter <filter and options>"));

    return result.elements();
  }

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

  public void setBaseFilter(Filter f) {
    if (!(f instanceof StreamableFilter)) {
      throw new IllegalArgumentException("Base filter must be Streamable!");
    }

    m_delegate = f;
  }

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
    return getBaseFilter().getInputFormat();
  }

  @Override
  public Instances getOutputFormat() {
    return getBaseFilter().getOutputFormat();
  }

  @Override
  public boolean batchFinished() throws Exception {
    return getBaseFilter().batchFinished();
  }

  @Override
  public Instance output() {
    return m_delegate.output();
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
    return getBaseFilter() != null && getBaseFilter().getInputFormat() != null
      && !m_isReset;
  }

  @Override
  public void reset() {
    m_isReset = true;
  }
}
