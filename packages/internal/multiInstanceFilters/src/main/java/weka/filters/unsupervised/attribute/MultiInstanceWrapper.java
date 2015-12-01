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
 * MultiInstanceWrapper.java
 * Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.attribute;

import java.util.Enumeration;
import java.util.Vector;

import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.MultiInstanceCapabilitiesHandler;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.MultiFilter;
import weka.filters.supervised.attribute.PartitionMembership;

/**
 * <!-- globalinfo-start --> Applies a single-instance filter to multi-instance
 * data by converting each bag to a collection of instances, using the filter
 * MultiInstanceToPropositional with default parameters, where each instance is
 * labeled with its bag's class label. Aggregates resulting data using sum/mode.
 * The resulting data can be processed by a single-instance classifier.
 * <p>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start --> Valid options are:
 * </p>
 * 
 * <pre>
 * -F &lt;filter name and options&gt;
 *  The single-instance filter to use, including all arguments.
 *  (default: weka.filters.unsupervised.attribute.PartitionMembership)
 * </pre>
 * 
 * <pre>
 * -A
 *  Use average of numeric attribute values across bag instead of sum.
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Eibe Frank (eibe@.cs.waikato.ac.nz)
 * @version $Revision$
 */
public class MultiInstanceWrapper extends Filter implements OptionHandler,
  MultiInstanceCapabilitiesHandler {

  /** For serialization */
  private static final long serialVersionUID = -3232591375578585231L;

  /** The filter to apply */
  protected Filter m_Filter = new PartitionMembership();

  /** The filter used to convert the data into single-instance data */
  protected MultiInstanceToPropositional m_MItoP = null;

  /** The filter used to remove the bag index */
  protected MultiFilter m_MF = null;

  /**
   * Whether to use average instead of sum of attribute values for numeric
   * attributes
   */
  protected boolean m_UseAverage = false;

  /**
   * Returns a string describing this filter
   * 
   * @return a description of the filter suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {

    return "Applies a single-instance filter to multi-instance data by converting each bag "
      + "to a collection of instances, using the filter MultiInstanceToPropositional with default "
      + "parameters, where each instance is labeled with its bag's class label. Aggregates resulting "
      + "data using sum/mode. The resulting data can be processed by a single-instance classifier.";
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> result = new Vector<Option>();

    result
      .addElement(new Option(
        "\tThe single-instance filter to use, including all arguments.\n"
          + "\t(default: weka.filters.unsupervised.attribute.PartitionMembership)",
        "F", 1, "-F <filter name and options>"));

    result.addElement(new Option(
      "\tUse average of numeric attribute values across bag instead of sum.\n",
      "A", 0, "-A"));

    return result.elements();
  }

  /**
   * <p>Parses the options for this object.
   * </p>
   * 
   * <!-- options-start --> Valid options are:
   * 
   * <pre>
   * -F &lt;filter name and options&gt;
   *  The single-instance filter to use, including all arguments.
   *  (default: weka.filters.unsupervised.attribute.PartitionMembership)
   * </pre>
   * 
   * <pre>
   * -A
   *  Use average of numeric attribute values across bag instead of sum.
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the options to use
   * @throws Exception if setting of options fails
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    // Set filter specification
    String filterString = Utils.getOption('F', options);
    if (filterString.length() > 0) {
      String[] filterSpec = Utils.splitOptions(filterString);
      if (filterSpec.length == 0) {
        throw new IllegalArgumentException(
          "Invalid filter specification string");
      }
      String filterName = filterSpec[0];
      filterSpec[0] = "";
      setFilter((Filter) Utils.forName(Filter.class, filterName, filterSpec));
    } else {
      setFilter(new PartitionMembership());
    }

    setUseAverage(Utils.getFlag('A', options));

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the classifier.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> options = new Vector<String>();

    options.add("-F");
    options.add("" + getFilterSpec());

    if (getUseAverage()) {
      options.add("-A");
    }
    return options.toArray(new String[0]);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String filterTipText() {

    return "The single-instance filter to be used.";
  }

  /**
   * Sets the filter
   * 
   * @param filter the filter with all options set.
   */
  public void setFilter(Filter filter) {

    m_Filter = filter;
  }

  /**
   * Gets the filter used.
   * 
   * @return the filter
   */
  public Filter getFilter() {

    return m_Filter;
  }

  /**
   * Gets the filter specification string, which contains the class name of the
   * filter and any options to the filter
   * 
   * @return the filter string.
   */
  protected String getFilterSpec() {

    Filter c = getFilter();
    if (c instanceof OptionHandler) {
      return c.getClass().getName() + " "
        + Utils.joinOptions(((OptionHandler) c).getOptions());
    }
    return c.getClass().getName();
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String useAverageTipText() {

    return "If true, average of numeric attribute values across bag is used instead of sum.";
  }

  /**
   * Sets whether sum is used.
   * 
   * @param useAverage true if average should be used instead of sum
   */
  public void setUseAverage(boolean useAverage) {

    m_UseAverage = useAverage;
  }

  /**
   * Gets whether average is used.
   * 
   * @return true if sum is used instead of mean
   */
  public boolean getUseAverage() {

    return m_UseAverage;
  }

  /**
   * Returns the Capabilities of this filter.
   * 
   * @return the capabilities of this object
   * @see Capabilities
   */
  @Override
  public Capabilities getCapabilities() {

    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.disableAllAttributes();
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.RELATIONAL_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enableAllClasses();
    result.enable(Capability.MISSING_CLASS_VALUES);

    // other
    result.enable(Capability.ONLY_MULTIINSTANCE);

    return result;
  }

  /**
   * Returns the capabilities of this multi-instance filter for the relational
   * data (i.e., the bags).
   * 
   * @return the capabilities of this object
   * @see Capabilities
   */
  @Override
  public Capabilities getMultiInstanceCapabilities() {
    Capabilities result = m_Filter.getCapabilities();

    // The class will be attached
    result.enable(Capability.NO_CLASS);

    // other
    result.setMinimumNumberInstances(0);

    return result;
  }

  /**
   * Sets the format of the input instances.
   * 
   * @param instanceInfo an Instances object containing the input instance
   *          structure (any instances contained in the object are ignored -
   *          only the structure is required).
   * @return false the outputFormat may not be collected immediately
   * @throws Exception if the input format can't be set successfully
   */
  @Override
  public boolean setInputFormat(Instances instanceInfo) throws Exception {

    super.setInputFormat(instanceInfo);

    m_MItoP = null;
    m_MF = null;

    return false;
  }

  /**
   * Input an instance for filtering. Filter requires all training instances be
   * read before producing output.
   * 
   * @param instance the input instance
   * @return true if the filtered instance may now be collected with output().
   * @throws IllegalStateException if no input format has been set.
   */
  @Override
  public boolean input(Instance instance) throws Exception {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }

    if (m_MItoP != null) {
      bufferInput(instance);
      Instances SIdata = Filter.useFilter(getInputFormat(), m_MItoP);
      flushInput();

      // Filter data using appropriate filter, removing the bag index
      Instances convertedInstances = m_MF.getOutputFormat();
      Instances tempInstances = new Instances(convertedInstances,
        convertedInstances.numInstances());
      for (Instance inst : SIdata) {
        m_MF.input(inst);
        tempInstances.add(m_MF.output());
      }

      double[] newVals = new double[tempInstances.numAttributes() + 1];
      newVals[0] = instance.value(0);
      for (int i = 1; i < newVals.length; i++) {
        if (i - 1 == tempInstances.classIndex()) {
          newVals[i] = instance.classValue();
        } else {
          if (!getUseAverage() && tempInstances.attribute(i - 1).isNumeric()) {
            for (Instance tempInst : tempInstances) {
              newVals[i] += tempInst.value(i - 1);
            }
          } else {
            newVals[i] = tempInstances.meanOrMode(i - 1); // Use mean or mode
          }
        }
      }
      DenseInstance insT = new DenseInstance(instance.weight(), newVals);
      push(insT);
      return true;
    }

    bufferInput(instance);
    return false;
  }

  /**
   * Signify that this batch of input to the filter is finished. If the filter
   * requires all instances prior to filtering, output() may now be called to
   * retrieve the filtered instances.
   * 
   * @return true if there are instances pending output
   * @throws IllegalStateException if no input structure has been defined
   */
  @Override
  public boolean batchFinished() throws Exception {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_MItoP == null) {

      // Expand MI data into SI format
      m_MItoP = new MultiInstanceToPropositional();
      m_MItoP.setInputFormat(getInputFormat());
      Instances SIdata = Filter.useFilter(getInputFormat(), m_MItoP);

      // Filter data using appropriate filter, removing the bag index
      m_MF = new MultiFilter();
      Filter[] twoFilters = new Filter[2];
      twoFilters[0] = new Remove();
      ((Remove) twoFilters[0]).setAttributeIndices("1");
      twoFilters[1] = m_Filter;
      m_MF.setFilters(twoFilters);
      m_MF.setInputFormat(SIdata);

      for (Instance inst : SIdata) {
        m_MF.input(inst);
      }
      m_MF.batchFinished();

      Instances convertedInstances = m_MF.getOutputFormat();
      Instances tempInstances = new Instances(convertedInstances,
        convertedInstances.numInstances());
      convertedInstances.insertAttributeAt((Attribute) getInputFormat()
        .attribute(0).copy(), 0);
      setOutputFormat(convertedInstances);

      int origInstanceIndex = 0;
      int counter = 0;
      for (int index = 0; index < SIdata.numInstances(); index++) {

        // Store converted instance
        tempInstances.add(m_MF.output());

        // Figure out size of current bag
        int bagSize = 1;
        Instances relation = getInputFormat().instance(origInstanceIndex)
          .relationalValue(1);
        if (relation != null) {
          bagSize = relation.numInstances();
        }

        // Are we finished with the current bag?
        if (++counter == bagSize) {
          double[] newVals = new double[convertedInstances.numAttributes()];
          newVals[0] = getInputFormat().instance(origInstanceIndex).value(0);
          for (int i = 1; i < newVals.length; i++) {
            if (i - 1 == tempInstances.classIndex()) {
              newVals[i] = tempInstances.instance(0).classValue(); // All class
                                                                   // values
                                                                   // should be
                                                                   // the same
            } else {
              if (!getUseAverage()
                && tempInstances.attribute(i - 1).isNumeric()) {
                for (Instance tempInst : tempInstances) {
                  newVals[i] += tempInst.value(i - 1);
                }
              } else {
                newVals[i] = tempInstances.meanOrMode(i - 1); // Use mean or
                                                              // mode
              }
            }
          }
          DenseInstance insT = new DenseInstance(getInputFormat().instance(
            origInstanceIndex++).weight(), newVals);
          insT.setDataset(convertedInstances);
          push(insT);
          tempInstances.delete();
          counter = 0;
        }
      }
    }

    flushInput();

    m_NewBatch = true;
    return (numPendingOutput() != 0);
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {

    return RevisionUtils.extract("$Revision$");
  }

  /**
   * runs the filter with the given arguments
   * 
   * @param args the commandline arguments
   */
  public static void main(String[] args) {

    runFilter(new MultiInstanceWrapper(), args);
  }
}
