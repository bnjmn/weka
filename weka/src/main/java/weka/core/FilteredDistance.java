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
 *    FilteredDistance.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import weka.core.DistanceFunction;
import weka.core.neighboursearch.PerformanceStats;

import weka.filters.Filter;
import weka.filters.unsupervised.attribute.RandomProjection;
import weka.filters.unsupervised.attribute.Remove;

import java.util.Enumeration;
import java.util.Vector;
import java.util.Collections;

import java.io.Serializable;

/**
 <!-- globalinfo-start -->
 Applies the given filter before calling the given distance function.
 <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 Valid options are: <p/>
 
 <pre> -F
  The filter to use. (default: weka.unsupervised.attribute.RandomProjection</pre>
 
 <pre> -E
  The distance function to use. (default: weka.core.EuclideanDistance</pre>
 
 <pre> 
 Options specific to filter weka.filters.unsupervised.attribute.RandomProjection:
 </pre>
 
 <pre> -N &lt;number&gt;
  The number of dimensions (attributes) the data should be reduced to
  (default 10; exclusive of the class attribute, if it is set).</pre>
 
 <pre> -D [SPARSE1|SPARSE2|GAUSSIAN]
  The distribution to use for calculating the random matrix.
  Sparse1 is:
    sqrt(3)*{-1 with prob(1/6), 0 with prob(2/3), +1 with prob(1/6)}
  Sparse2 is:
    {-1 with prob(1/2), +1 with prob(1/2)}
 </pre>
 
 <pre> -P &lt;percent&gt;
  The percentage of dimensions (attributes) the data should
  be reduced to (exclusive of the class attribute, if it is set). The -N
  option is ignored if this option is present and is greater
  than zero.</pre>
 
 <pre> -M
  Replace missing values using the ReplaceMissingValues filter</pre>
 
 <pre> -R &lt;num&gt;
  The random seed for the random number generator used for
  calculating the random matrix (default 42).</pre>
 
 <pre> 
 Options specific to distance function weka.core.EuclideanDistance:
 </pre>
 
 <pre> -D
  Turns off the normalization of attribute 
  values in distance calculation.</pre>
 
 <pre> -R &lt;col1,col2-col4,...&gt;
  Specifies list of columns to used in the calculation of the 
  distance. 'first' and 'last' are valid indices.
  (default: first-last)</pre>
 
 <pre> -V
  Invert matching sense of column indices.</pre>
 
 <pre> -R &lt;col1,col2-col4,...&gt;
  Specifies list of columns to used in the calculation of the 
  distance. 'first' and 'last' are valid indices.
  (default: first-last)</pre>
 
 <pre> -V
  Invert matching sense of column indices.</pre>
 
 <!-- options-end --> 
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 8034 $
 */

public class FilteredDistance implements DistanceFunction, OptionHandler, Serializable {

  /** The distance function to use. */
  DistanceFunction m_Distance = new EuclideanDistance();

  /** The filter to use. */
  Filter m_Filter = new RandomProjection();

  /** Remove filter to remove attributes if required. */
  Remove m_Remove = new Remove();

  /**
   * Default constructor: need to set up Remove filter.
   */
  public FilteredDistance() {
    
    m_Remove.setInvertSelection(true);
    m_Remove.setAttributeIndices("first-last");
  }

  /**
   * Returns a string describing this object.
   * 
   * @return 		a description of the evaluator suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return 
        "Applies the given filter before calling the given distance function.";
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
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
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String distanceTipText() {
    return "The distance to be used.";
  }

  /**
   * Sets the distance
   *
   * @param distance the distance with all options set.
   */
  public void setDistance(DistanceFunction distance) {

    m_Distance = distance;
  }

  /**
   * Gets the distance used.
   *
   * @return the distance
   */
  public DistanceFunction getDistance() {

    return m_Distance;
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.add(new Option("\tThe filter to use. (default: weka.unsupervised.attribute.RandomProjection",
      "F", 1, "-F"));

    result.addElement(new Option("\tThe distance function to use. (default: weka.core.EuclideanDistance",
      "E", 0, "-E"));
    
    if (m_Filter instanceof OptionHandler) {
      result.addElement(new Option(
                                      "",
                                      "", 0, "\nOptions specific to filter "
                                      + m_Filter.getClass().getName() + ":"));
      result.addAll(Collections.list(((OptionHandler)m_Filter).listOptions()));
    }

    if (m_Distance instanceof OptionHandler) {
      
      result.addElement(new Option(
                                      "",
                                      "", 0, "\nOptions specific to distance function "
                                      + m_Distance.getClass().getName() + ":"));
      result.addAll(Collections.list(((OptionHandler)m_Distance).listOptions()));
    }

    result.addElement(new Option(
      "\tSpecifies list of columns to used in the calculation of the \n"
        + "\tdistance. 'first' and 'last' are valid indices.\n"
        + "\t(default: first-last)", "R", 1, "-R <col1,col2-col4,...>"));

    result.addElement(new Option("\tInvert matching sense of column indices.",
      "V", 0, "-V"));

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

    result.add("-R");
    result.add(getAttributeIndices());

    if (getInvertSelection()) {
      result.add("-V");
    }

    result.add("-F");
    result.add("" + getFilterSpec());

    result.add("-D");
    result.add("" + getDistanceSpec());

    return result.toArray(new String[result.size()]);
  }
  
  /**
   * Gets the filter specification string, which contains the class name of
   * the filter and any options to the filter
   *
   * @return the filter string.
   */
  protected String getFilterSpec() {
    
    Filter c = getFilter();
    if (c instanceof OptionHandler) {
      return c.getClass().getName() + " "
	+ Utils.joinOptions(((OptionHandler)c).getOptions());
    }
    return c.getClass().getName();
  }
  
  /**
   * Gets the distance specification string, which contains the class name of
   * the distance and any options to the distance
   *
   * @return the distance string.
   */
  protected String getDistanceSpec() {
    
    DistanceFunction c = getDistance();
    if (c instanceof OptionHandler) {
      return c.getClass().getName() + " "
	+ Utils.joinOptions(((OptionHandler)c).getOptions());
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

    String distance = Utils.getOption('D', options);
    if (distance.length() != 0) {
      String distanceSpec[] = Utils.splitOptions(distance);
      if (distanceSpec.length == 0) {
        throw new Exception("Invalid distance specification string.");
      }
      String className = distanceSpec[0];
      distanceSpec[0] = "";

      setDistance((DistanceFunction) Utils.forName(
        DistanceFunction.class, className, distanceSpec));
    } else {
      setDistance(new EuclideanDistance());
    }

    String filter = Utils.getOption('F', options);
    if (filter.length() != 0) {
      String filterSpec[] = Utils.splitOptions(filter);
      if (filterSpec.length == 0) {
        throw new Exception("Invalid filter specification string.");
      }
      String className = filterSpec[0];
      filterSpec[0] = "";

      setFilter((Filter) Utils.forName(
        Filter.class, className, filterSpec));
    } else {
      setFilter(new RandomProjection());
    }

    String tmpStr = Utils.getOption('R', options);
    if (tmpStr.length() != 0) {
      setAttributeIndices(tmpStr);
    } else {
      setAttributeIndices("first-last");
    }

    setInvertSelection(Utils.getFlag('V', options));
  }

  /**
   * Sets the instances.
   * 
   * @param insts the instances to use
   */
  public void setInstances(Instances insts) {
    
    try {
      m_Remove.setInputFormat(insts);
      Instances reducedInstances = Filter.useFilter(insts, m_Remove);
      m_Filter.setInputFormat(reducedInstances);
      m_Distance.setInstances(Filter.useFilter(reducedInstances, m_Filter));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * returns the instances currently set.
   * 
   * @return the current instances
   */
  public Instances getInstances() {
    
    return m_Distance.getInstances();
  }

  /**
   * Sets the range of attributes to use in the calculation of the distance. The
   * indices start from 1, 'first' and 'last' are valid as well. E.g.:
   * first-3,5,6-last
   * 
   * @param value the new attribute index range
   */
  public void setAttributeIndices(String value) {

    m_Remove.setAttributeIndices(value);
  }

  /**
   * Gets the range of attributes used in the calculation of the distance.
   * 
   * @return the attribute index range
   */
  public String getAttributeIndices() {

    return m_Remove.getAttributeIndices();
  }

  /**
   * Sets whether the matching sense of attribute indices is inverted or not.
   * 
   * @param value if true the matching sense is inverted
   */
  public void setInvertSelection(boolean value) {

    m_Remove.setInvertSelection(!value);
  }

  /**
   * Gets whether the matching sense of attribute indices is inverted or not.
   * 
   * @return true if the matching sense is inverted
   */
  public boolean getInvertSelection() {

    return !m_Remove.getInvertSelection();
  }

  /**
   * Calculates the distance between two instances.
   * 
   * @param first the first instance
   * @param second the second instance
   * @return the distance between the two given instances
   */
  public double distance(Instance first, Instance second) {

    return distance(first, second, Double.POSITIVE_INFINITY, null);
  }

  /**
   * Calculates the distance between two instances.
   * 
   * @param first the first instance
   * @param second the second instance
   * @param stats the performance stats object
   * @return the distance between the two given instances
   * @throws Exception if calculation fails
   */
  public double distance(Instance first, Instance second, PerformanceStats stats)
    throws Exception {

    return distance(first, second, Double.POSITIVE_INFINITY, stats);
  }

  /**
   * Calculates the distance between two instances. Offers speed up (if the
   * distance function class in use supports it) in nearest neighbour search by
   * taking into account the cutOff or maximum distance. Depending on the
   * distance function class, post processing of the distances by
   * postProcessDistances(double []) may be required if this function is used.
   * 
   * @param first the first instance
   * @param second the second instance
   * @param cutOffValue If the distance being calculated becomes larger than
   *          cutOffValue then the rest of the calculation is discarded.
   * @return the distance between the two given instances or
   *         Double.POSITIVE_INFINITY if the distance being calculated becomes
   *         larger than cutOffValue.
   */
  public double distance(Instance first, Instance second, double cutOffValue) {

    return distance(first, second, cutOffValue, null);    
  }

  /**
   * Calculates the distance between two instances. Offers speed up (if the
   * distance function class in use supports it) in nearest neighbour search by
   * taking into account the cutOff or maximum distance. Depending on the
   * distance function class, post processing of the distances by
   * postProcessDistances(double []) may be required if this function is used.
   * 
   * @param first the first instance
   * @param second the second instance
   * @param cutOffValue If the distance being calculated becomes larger than
   *          cutOffValue then the rest of the calculation is discarded.
   * @param stats the performance stats object
   * @return the distance between the two given instances or
   *         Double.POSITIVE_INFINITY if the distance being calculated becomes
   *         larger than cutOffValue.
   */
  public double distance(Instance first, Instance second, double cutOffValue,
                         PerformanceStats stats) {

    try {
      m_Remove.input(first);
      m_Filter.input(m_Remove.output());
      Instance firstFiltered = m_Filter.output();
      m_Remove.input(second);
      m_Filter.input(m_Remove.output());
      Instance secondFiltered = m_Filter.output();
      return m_Distance.distance(firstFiltered, secondFiltered, cutOffValue, stats);
    } catch (Exception e) {
      e.printStackTrace();
      return -1;
    }
  }

  /**
   * Does post processing of the distances (if necessary) returned by
   * distance(distance(Instance first, Instance second, double cutOffValue). It
   * may be necessary, depending on the distance function, to do post processing
   * to set the distances on the correct scale. Some distance function classes
   * may not return correct distances using the cutOffValue distance function to
   * minimize the inaccuracies resulting from floating point comparison and
   * manipulation.
   * 
   * @param distances the distances to post-process
   */
  public void postProcessDistances(double distances[]) {

    m_Distance.postProcessDistances(distances);
  }

  /**
   * Update the distance function (if necessary) for the newly added instance.
   * 
   * @param ins the instance to add
   */
  public void update(Instance ins) {

    try {
      m_Remove.input(ins);
      m_Filter.input(m_Remove.output());
      m_Distance.update(m_Filter.output());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Free any references to training instances
   */
  public void clean() {

    m_Distance.clean();
  }
}

