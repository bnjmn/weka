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
 *    AddCluster.java
 *    Copyright (C) 2002 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.unsupervised.attribute;

import weka.clusterers.Clusterer;
import weka.clusterers.AbstractClusterer;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Range;
import weka.core.RevisionUtils;
import weka.core.SparseInstance;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.UnsupervisedFilter;

import java.util.Enumeration;
import java.util.Vector;

/** 
 <!-- globalinfo-start -->
 * A filter that adds a new nominal attribute representing the cluster assigned to each instance by the specified clustering algorithm.
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -W &lt;clusterer specification&gt;
 *  Full class name of clusterer to use, followed
 *  by scheme options. eg:
 *   "weka.clusterers.SimpleKMeans -N 3"
 *  (default: weka.clusterers.SimpleKMeans)</pre>
 * 
 * <pre> -I &lt;att1,att2-att4,...&gt;
 *  The range of attributes the clusterer should ignore.
 * </pre>
 * 
 <!-- options-end -->
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 1.10.2.2 $
 */
public class AddCluster 
  extends Filter 
  implements UnsupervisedFilter, OptionHandler {
  
  /** for serialization */
  static final long serialVersionUID = 7414280611943807337L;

  /** The clusterer used to do the cleansing */
  protected Clusterer m_Clusterer = new weka.clusterers.SimpleKMeans();

  /** Range of attributes to ignore */
  protected Range m_IgnoreAttributesRange = null;

  /** Filter for removing attributes */
  protected Filter m_removeAttributes = new Remove();

  /** 
   * Returns the Capabilities of this filter, makes sure that the class is
   * never set (for the clusterer).
   *
   * @param data	the data to use for customization
   * @return            the capabilities of this object, based on the data
   * @see               #getCapabilities()
   */
  public Capabilities getCapabilities(Instances data) {
    Instances	newData;
    
    newData = new Instances(data, 0);
    newData.setClassIndex(-1);
    
    return super.getCapabilities(newData);
  }

  /** 
   * Returns the Capabilities of this filter.
   *
   * @return            the capabilities of this object
   * @see               Capabilities
   */
  public Capabilities getCapabilities() {
    Capabilities result = m_Clusterer.getCapabilities();
    
    result.setMinimumNumberInstances(0);
    
    return result;
  }
  
  /**
   * tests the data whether the filter can actually handle it
   * 
   * @param instanceInfo	the data to test
   * @throws Exception		if the test fails
   */
  protected void testInputFormat(Instances instanceInfo) throws Exception {
    getCapabilities(instanceInfo).testWithFail(removeIgnored(instanceInfo));
  }

  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input instance
   * structure (any instances contained in the object are ignored - only the
   * structure is required).
   * @return true if the outputFormat may be collected immediately
   * @throws Exception if the inputFormat can't be set successfully 
   */ 
  public boolean setInputFormat(Instances instanceInfo) throws Exception {
    
    super.setInputFormat(instanceInfo);
    m_removeAttributes = null;

    return false;
  }

  /**
   * filters all attributes that should be ignored
   * 
   * @param data	the data to filter
   * @return		the filtered data
   * @throws Exception	if filtering fails
   */
  protected Instances removeIgnored(Instances data) throws Exception {
    Instances result = data;
    
    if (m_IgnoreAttributesRange != null || data.classIndex() >= 0) {
      m_removeAttributes = new Remove();
      String rangeString = "";
      if (m_IgnoreAttributesRange != null) {
	rangeString += m_IgnoreAttributesRange.getRanges();
      }
      if (data.classIndex() >= 0) {
	if (rangeString.length() > 0) {
	  rangeString += "," + (data.classIndex() + 1);
	} else {
	  rangeString = "" + (data.classIndex() + 1);
	}
      }
      ((Remove) m_removeAttributes).setAttributeIndices(rangeString);
      ((Remove) m_removeAttributes).setInvertSelection(false);
      m_removeAttributes.setInputFormat(data);
      result = Filter.useFilter(data, m_removeAttributes);
    }
    
    return result;
  }
  
  /**
   * Signify that this batch of input to the filter is finished.
   *
   * @return true if there are instances pending output
   * @throws IllegalStateException if no input structure has been defined 
   */  
  public boolean batchFinished() throws Exception {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }

    Instances toFilter = getInputFormat();
    
    if (!isFirstBatchDone()) {
      // filter out attributes if necessary
      Instances toFilterIgnoringAttributes = removeIgnored(toFilter);

      // build the clusterer
      m_Clusterer.buildClusterer(toFilterIgnoringAttributes);

      // create output dataset with new attribute
      Instances filtered = new Instances(toFilter, 0); 
      FastVector nominal_values = new FastVector(m_Clusterer.numberOfClusters());
      for (int i=0; i<m_Clusterer.numberOfClusters(); i++) {
	nominal_values.addElement("cluster" + (i+1)); 
      }
      filtered.insertAttributeAt(new Attribute("cluster", nominal_values),
	  filtered.numAttributes());

      setOutputFormat(filtered);
    }

    // build new dataset
    for (int i=0; i<toFilter.numInstances(); i++) {
      convertInstance(toFilter.instance(i));
    }
    
    flushInput();
    m_NewBatch = true;
    m_FirstBatchDone = true;

    return (numPendingOutput() != 0);
  }

  /**
   * Input an instance for filtering. Ordinarily the instance is processed
   * and made available for output immediately. Some filters require all
   * instances be read before producing output.
   *
   * @param instance the input instance
   * @return true if the filtered instance may now be
   * collected with output().
   * @throws IllegalStateException if no input format has been defined.
   */
  public boolean input(Instance instance) throws Exception {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }
    
    if (outputFormatPeek() != null) {
      convertInstance(instance);
      return true;
    }

    bufferInput(instance);
    return false;
  }

  /**
   * Convert a single instance over. The converted instance is added to 
   * the end of the output queue.
   *
   * @param instance the instance to convert
   * @throws Exception if something goes wrong
   */
  protected void convertInstance(Instance instance) throws Exception {
    Instance original, processed;
    original = instance;

    // copy values
    double[] instanceVals = new double[instance.numAttributes()+1];
    for(int j = 0; j < instance.numAttributes(); j++) {
      instanceVals[j] = original.value(j);
    }
    Instance filteredI = null;
    if (m_removeAttributes != null) {
      m_removeAttributes.input(instance);
      filteredI = m_removeAttributes.output();
    } else {
      filteredI = instance;
    }

    // add cluster to end
    try {
      instanceVals[instance.numAttributes()] = m_Clusterer.clusterInstance(filteredI);
    }
    catch (Exception e) {
      // clusterer couldn't cluster instance -> missing
      instanceVals[instance.numAttributes()] = Instance.missingValue();
    }

    // create new instance
    if (original instanceof SparseInstance) {
      processed = new SparseInstance(original.weight(), instanceVals);
    } else {
      processed = new Instance(original.weight(), instanceVals);
    }

    processed.setDataset(instance.dataset());
    copyValues(processed, false, instance.dataset(), getOutputFormat());
    processed.setDataset(getOutputFormat());
      
    push(processed);
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    
    Vector newVector = new Vector(2);
    
    newVector.addElement(new Option(
	      "\tFull class name of clusterer to use, followed\n"
	      + "\tby scheme options. eg:\n"
	      + "\t\t\"weka.clusterers.SimpleKMeans -N 3\"\n"
	      + "\t(default: weka.clusterers.SimpleKMeans)",
	      "W", 1, "-W <clusterer specification>"));
    
    newVector.addElement(new Option(
	      "\tThe range of attributes the clusterer should ignore.\n",
	      "I", 1,"-I <att1,att2-att4,...>"));

    return newVector.elements();
  }


  /**
   * Parses a given list of options. <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -W &lt;clusterer specification&gt;
   *  Full class name of clusterer to use, followed
   *  by scheme options. eg:
   *   "weka.clusterers.SimpleKMeans -N 3"
   *  (default: weka.clusterers.SimpleKMeans)</pre>
   * 
   * <pre> -I &lt;att1,att2-att4,...&gt;
   *  The range of attributes the clusterer should ignore.
   * </pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String clustererString = Utils.getOption('W', options);
    if (clustererString.length() == 0)
      clustererString = weka.clusterers.SimpleKMeans.class.getName();
    String[] clustererSpec = Utils.splitOptions(clustererString);
    if (clustererSpec.length == 0) {
      throw new Exception("Invalid clusterer specification string");
    }
    String clustererName = clustererSpec[0];
    clustererSpec[0] = "";
    setClusterer(AbstractClusterer.forName(clustererName, clustererSpec));
        
    setIgnoredAttributeIndices(Utils.getOption('I', options));

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the filter.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [5];
    int current = 0;

    options[current++] = "-W"; options[current++] = "" + getClustererSpec();
    
    if (!getIgnoredAttributeIndices().equals("")) {
      options[current++] = "-I"; options[current++] = getIgnoredAttributeIndices();
    }

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "A filter that adds a new nominal attribute representing the cluster "
      + "assigned to each instance by the specified clustering algorithm.";
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String clustererTipText() {

    return "The clusterer to assign clusters with.";
  }

  /**
   * Sets the clusterer to assign clusters with.
   *
   * @param clusterer The clusterer to be used (with its options set).
   */
  public void setClusterer(Clusterer clusterer) {

    m_Clusterer = clusterer;
  }
  
  /**
   * Gets the clusterer used by the filter.
   *
   * @return The clusterer being used.
   */
  public Clusterer getClusterer() {

    return m_Clusterer;
  }

  /**
   * Gets the clusterer specification string, which contains the class name of
   * the clusterer and any options to the clusterer.
   *
   * @return the clusterer string.
   */
  protected String getClustererSpec() {
    
    Clusterer c = getClusterer();
    if (c instanceof OptionHandler) {
      return c.getClass().getName() + " "
	+ Utils.joinOptions(((OptionHandler)c).getOptions());
    }
    return c.getClass().getName();
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String ignoredAttributeIndicesTipText() {

    return "The range of attributes to be ignored by the clusterer. eg: first-3,5,9-last";
  }

  /**
   * Gets ranges of attributes to be ignored.
   *
   * @return a string containing a comma-separated list of ranges
   */
  public String getIgnoredAttributeIndices() {

    if (m_IgnoreAttributesRange == null) {
      return "";
    } else {
      return m_IgnoreAttributesRange.getRanges();
    }
  }

  /**
   * Sets the ranges of attributes to be ignored. If provided string
   * is null, no attributes will be ignored.
   *
   * @param rangeList a string representing the list of attributes. 
   * eg: first-3,5,6-last
   * @throws IllegalArgumentException if an invalid range list is supplied 
   */
  public void setIgnoredAttributeIndices(String rangeList) {

    if ((rangeList == null) || (rangeList.length() == 0)) {
      m_IgnoreAttributesRange = null;
    } else {
      m_IgnoreAttributesRange = new Range();
      m_IgnoreAttributesRange.setRanges(rangeList);
    }
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 1.10.2.2 $");
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String [] argv) {
    runFilter(new AddCluster(), argv);
  }
}
