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
 *    FilteredNeighbourSearch.java
 *    Copyright (C) 2014 University of Waikato
 */
package weka.core.neighboursearch;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.SerializedObject;
import weka.core.Utils;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.CapabilitiesHandler;
import weka.filters.AllFilter;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.AddID;

/**
 * <!-- globalinfo-start -->
 * Applies the given filter before calling the given neighbour search method. The filter must not change the size of the dataset or the order of the instances! Also, the range setting that is specified for the distance function is ignored: all attributes are used for the distance calculation.
 * <p/>
 * <!-- globalinfo-end -->
 *
 * <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -F
 *  The filter to use. (default: weka.filters.AllFilter)</pre>
 * 
 * <pre> -S
 *  The search method to use. (default: weka.core.neighboursearch.LinearNNSearch)</pre>
 * 
 * <pre> 
 * Options specific to filter weka.filters.AllFilter:
 * </pre>
 * 
 * <pre> -output-debug-info
 *  If set, filter is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -do-not-check-capabilities
 *  If set, filter capabilities are not checked before filter is built
 *  (use with caution).</pre>
 * 
 * <pre> 
 * Options specific to search method weka.core.neighboursearch.LinearNNSearch:
 * </pre>
 * 
 * <pre> -S
 *  Skip identical instances (distances equal to zero).
 * </pre>
 * 
 * <pre> -A &lt;classname and options&gt;
 *  Distance function to use.
 *  (default: weka.core.EuclideanDistance)</pre>
 * 
 * <pre> -P
 *  Calculate performance statistics.</pre>
 * 
 * <!-- options-end -->
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 8034 $
 */
public class FilteredNeighbourSearch extends NearestNeighbourSearch implements CapabilitiesHandler {

  /** For serialization */
  private static final long serialVersionUID = 1369174644087067375L;

  /** Need to use ID filter to add ID so that we can identify instances */
  protected AddID m_AddID = new AddID();

  /** The index of the ID attribute */
  protected int m_IndexOfID = -1;

  /** The filter object to use. */
  protected Filter m_Filter = new AllFilter();

  /** The neighborhood search method to use. */
  protected NearestNeighbourSearch m_SearchMethod = new LinearNNSearch();

  /** The modified search method, where ID is skipped */
  protected NearestNeighbourSearch m_ModifiedSearchMethod = null;

  /**
   * Returns default capabilities of the classifier.
   *
   * @return the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
 
   Capabilities result = getFilter().getCapabilities();

    // set dependencies
    for (Capability cap : Capability.values())
      result.enableDependency(cap);

    return result;
  }

  /**
   * Sets the instances to build the filtering model from.
   * 
   * @param insts the Instances object
   */
  public void setInstances(Instances data) {

    try {
      super.setInstances(data);

      // Apply user-specified filter
      getCapabilities().testWithFail(data);
      Instances filteredData = new Instances(data);
      getFilter().setInputFormat(filteredData);
      filteredData = Filter.useFilter(data, getFilter());
      if (data.numInstances() != filteredData.numInstances()) {
        throw new IllegalArgumentException(
          "FilteredNeighbourSearch: Filter has changed the number of instances!");
      }

      // Set up filter to add ID
      m_IndexOfID = filteredData.numAttributes();
      m_AddID.setIDIndex("" + (filteredData.numAttributes() + 1));
      ;
      m_AddID.setInputFormat(filteredData);
      filteredData = Filter.useFilter(filteredData, m_AddID);

      // Modify distance function for base method to skip ID
      // User-specified range setting for the distance function is simply
      // ignored
      m_ModifiedSearchMethod = (NearestNeighbourSearch) new SerializedObject(
        getSearchMethod()).getObject();
      m_ModifiedSearchMethod.getDistanceFunction().setAttributeIndices(
        "1-" + m_IndexOfID);
      m_ModifiedSearchMethod.getDistanceFunction().setInvertSelection(false);

      // Set up the distance function
      m_ModifiedSearchMethod.setInstances(filteredData);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Returns a string describing this object.
   * 
   * @return a description of the evaluator suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "Applies the given filter before calling the given neighbour search method. The filter "
      + "must not change the size of the dataset or the order of the instances! Also, the range "
      + "setting that is specified for the distance function is ignored: all attributes are used "
      + "for the distance calculation.";
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String filterTipText() {
    return "The filter to be used.";
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
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String searchMethodTipText() {
    return "The search method to be used.";
  }

  /**
   * Sets the search method
   *
   * @param searchMethod the search method with all options set.
   */
  public void setSearchMethod(NearestNeighbourSearch search) {
    m_SearchMethod = search;
  }

  /**
   * Gets the search method used.
   *
   * @return the search method
   */
  public NearestNeighbourSearch getSearchMethod() {
    return m_SearchMethod;
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
      .add(new Option(
        "\tThe filter to use. (default: weka.filters.AllFilter",
        "F", 1, "-F"));

    result
      .addElement(new Option(
        "\tThe search method to use. (default: weka.core.neighboursearch.LinearNNSearch)",
        "S", 0, "-S"));

    if (m_Filter instanceof OptionHandler) {
      result.addElement(new Option("", "", 0, "\nOptions specific to filter "
        + m_Filter.getClass().getName() + ":"));
      result.addAll(Collections.list(((OptionHandler) m_Filter).listOptions()));
    }

    if (m_SearchMethod instanceof OptionHandler) {

      result.addElement(new Option("", "", 0,
        "\nOptions specific to search method "
          + m_SearchMethod.getClass().getName() + ":"));
      result.addAll(Collections.list(((OptionHandler) m_SearchMethod)
        .listOptions()));
    }

    return result.elements();
  }

  /**
   * Gets the current settings. Returns empty array.
   * 
   * @return an array of strings suitable for passing to setOptions()
   */
  @Override
  public String[] getOptions() {

    Vector<String> result = new Vector<String>();

    result.add("-F");
    result.add("" + getFilterSpec());

    result.add("-S");
    result.add("" + getSearchMethodSpec());

    return result.toArray(new String[result.size()]);
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
   * Gets the search method specification string, which contains the class name
   * of the search method and any options to the search method
   *
   * @return the search method string.
   */
  protected String getSearchMethodSpec() {

    NearestNeighbourSearch c = getSearchMethod();
    if (c instanceof OptionHandler) {
      return c.getClass().getName() + " "
        + Utils.joinOptions(((OptionHandler) c).getOptions());
    }
    return c.getClass().getName();
  }

  /**
   * Parses a given list of options.
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    String searchMethod = Utils.getOption('S', options);
    if (searchMethod.length() != 0) {
      String searchMethodSpec[] = Utils.splitOptions(searchMethod);
      if (searchMethodSpec.length == 0) {
        throw new Exception("Invalid search method specification string.");
      }
      String className = searchMethodSpec[0];
      searchMethodSpec[0] = "";

      setSearchMethod((NearestNeighbourSearch) Utils.forName(
        NearestNeighbourSearch.class, className, searchMethodSpec));
    } else {
      setSearchMethod(new LinearNNSearch());
    }

    String filter = Utils.getOption('F', options);
    if (filter.length() != 0) {
      String filterSpec[] = Utils.splitOptions(filter);
      if (filterSpec.length == 0) {
        throw new Exception("Invalid filter specification string.");
      }
      String className = filterSpec[0];
      filterSpec[0] = "";

      setFilter((Filter) Utils.forName(Filter.class, className, filterSpec));
    } else {
      setFilter(new AllFilter());
    }
  }

  /**
   * Returns the revision string
   * 
   * @return the revision
   * 
   * @see weka.core.RevisionHandler#getRevision()
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 11006 $");
  }

  /**
   * Returns the nearest neighbour for the given instance based on distance
   * measured in the filtered space.
   * 
   * @param target the instance for which to find the nearest neighbour
   * @return the nearest neighbour
   * 
   * @see weka.core.neighboursearch.NearestNeighbourSearch#nearestNeighbour(weka.core.Instance)
   */
  @Override
  public Instance nearestNeighbour(Instance target) throws Exception {

    getFilter().input(target);
    m_AddID.input(getFilter().output());
    return getInstances().instance(
      (int) m_ModifiedSearchMethod.nearestNeighbour(m_AddID.output()).value(
        m_IndexOfID) - 1);
  }

  /**
   * Returns the nearest neighbours for the given instance based on distance
   * measured in the filtered space.
   * 
   * @param target the instance for which to find the nearest neighbour
   * @param k the number of nearest neighbours to return
   * @return the nearest Neighbours
   * 
   * @see weka.core.neighboursearch.NearestNeighbourSearch#kNearestNeighbours(weka.core.Instance,
   *      int)
   */
  @Override
  public Instances kNearestNeighbours(Instance target, int k) throws Exception {

    // Get neighbors in filtered space
    getFilter().input(target);
    m_AddID.input(getFilter().output());
    Instances neighboursInFilteredSpace = m_ModifiedSearchMethod
      .kNearestNeighbours(m_AddID.output(), k);

    // Collect corresponding instances in original space
    Instances neighbours = new Instances(getInstances(), k);
    for (Instance inst : neighboursInFilteredSpace) {
      neighbours
        .add(getInstances().instance((int) inst.value(m_IndexOfID) - 1));
    }
    return neighbours;
  }

  /**
   * Returns the distances for the nearest neighbours in the FILTERED space
   * 
   * @return the array of distances for the nearest neighbours
   * 
   * @see weka.core.neighboursearch.NearestNeighbourSearch#getDistances()
   */
  @Override
  public double[] getDistances() throws Exception {
    return m_ModifiedSearchMethod.getDistances();
  }

  /**
   * Updates ranges based on the given instance, once it has been filtered.
   * 
   * @see weka.core.neighboursearch.NearestNeighbourSearch#update(weka.core.Instance)
   */
  @Override
  public void update(Instance ins) throws Exception {

    getFilter().input(ins);
    m_AddID.input(getFilter().output());
    m_ModifiedSearchMethod.update(m_AddID.output());
  }

  /**
   * Updates the instance info in the underlying search method, once the
   * instance has been filtered.
   * 
   * @param ins The instance to add the information of.
   */
  @Override
  public void addInstanceInfo(Instance ins) {
    if (m_Instances != null)
      try {
        getFilter().input(ins);
        m_AddID.input(getFilter().output());
        m_ModifiedSearchMethod.addInstanceInfo(m_AddID.output());
      } catch (Exception ex) {
        ex.printStackTrace();
      }
  }
}

