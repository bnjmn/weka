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
 *    AggregateableFilteredClassifier.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.meta;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import weka.classifiers.SingleClassifierEnhancer;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Aggregateable;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.PreconstructedFilter;
import weka.gui.beans.KFIgnore;

/**
 * A FilteredClassifier that implements Aggregateable. Requires the base
 * classifier to be Aggregateable so that when the aggretate method receives
 * another AggregateableFilteredClassifier it can aggregate the new one's base
 * model with this one's. Only allows PreconstructedFilters to be used. Any
 * Streambable filter that produces an output instance immediately after
 * receiving an input instance can be used by wrapping it in a
 * MakePreconstructedFilter.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
@KFIgnore
public class AggregateableFilteredClassifier extends SingleClassifierEnhancer
  implements Serializable, Aggregateable<AggregateableFilteredClassifier> {

  /** For serialization */
  private static final long serialVersionUID = 9208442051180638552L;

  /** Filter to use */
  protected Filter m_preConstructedFilter;

  /** Path to filter to load */
  protected String m_pathToFilter = "";

  /** The header of the filtered data */
  protected Instances m_filteredInstances;

  public AggregateableFilteredClassifier() {
    m_Classifier = new NaiveBayes();
  }

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> options = new Vector<Option>();

    options.add(new Option("\tPath to pre-constructed filter to use.",
      "load-filter", 1,
      "-load-filter <path to serialized pre-constructed filter>"));

    Enumeration superOpts = super.listOptions();
    while (superOpts.hasMoreElements()) {
      options.add((Option) superOpts.nextElement());
    }

    return options.elements();
  }

  @Override
  public String[] getOptions() {
    List<String> options = new ArrayList<String>();

    if (getPathToPreConstructedFilter() != null
      && getPathToPreConstructedFilter().length() > 0) {
      options.add("-load-filter");
      options.add(getPathToPreConstructedFilter());
    }

    String[] superOpts = super.getOptions();
    for (String s : superOpts) {
      options.add(s);
    }

    return options.toArray(new String[options.size()]);
  }

  @Override
  public void setOptions(String[] options) throws Exception {
    setPathToPreConstructedFilter(Utils.getOption("load-filter", options));

    super.setOptions(options);
  }

  public void setPathToPreConstructedFilter(String path) {
    m_pathToFilter = path;
  }

  public String getPathToPreConstructedFilter() {
    return m_pathToFilter;
  }

  /**
   * Set the PreconstructedFilter to use
   * 
   * @param f the PreconstructedFilter to use
   */
  public void setPreConstructedFilter(Filter f) {

    if (!(f instanceof PreconstructedFilter)) {
      throw new IllegalArgumentException(
        "The filter must be a Preconstructed one!");
    } else if (!((PreconstructedFilter) f).isConstructed()) {
      throw new IllegalArgumentException("PreconstructedFilter: "
        + f.getClass().getName() + " has not been initialized!");
    }

    m_preConstructedFilter = f;
  }

  /**
   * Get the PreconstructedFilter to use
   * 
   * @return the PreconstructedFilter to use
   */
  public Filter getPreConstructedFilter() {
    return m_preConstructedFilter;
  }

  /**
   * Load the PreconstructedFilter to use from the file system
   * 
   * @throws Exception if a problem occurs
   */
  protected void loadFilter() throws Exception {
    File f = new File(getPathToPreConstructedFilter());

    if (!f.exists()) {
      throw new IOException("The serialized filter '"
        + getPathToPreConstructedFilter()
        + "' does not seem to exit on the file system!");
    }

    ObjectInputStream ois = null;
    try {
      ois = new ObjectInputStream(new BufferedInputStream(
        new FileInputStream(f)));
      setPreConstructedFilter((Filter) ois.readObject());
    } finally {
      if (ois != null) {
        ois.close();
      }
    }
  }

  @Override
  public void buildClassifier(Instances data) throws Exception {

    if (getPathToPreConstructedFilter() != null
      && getPathToPreConstructedFilter().length() > 0) {
      loadFilter();
    }

    if (getClassifier() == null) {
      throw new Exception("No base classifier set!");
    }

    if (getPreConstructedFilter() == null) {
      throw new Exception("No filter set!");
    }

    Instances filtered = Filter.useFilter(data, getPreConstructedFilter());
    m_filteredInstances = filtered.stringFreeStructure();

    getClassifier().buildClassifier(filtered);
  }

  @Override
  public double[] distributionForInstance(Instance inst) throws Exception {
    if (!getPreConstructedFilter().input(inst)) {
      throw new Exception("Filter did not make instance available immediately!");
    }
    getPreConstructedFilter().batchFinished();
    Instance testI = getPreConstructedFilter().output();

    return getClassifier().distributionForInstance(testI);
  }

  @SuppressWarnings("unchecked")
  @Override
  public AggregateableFilteredClassifier aggregate(
    AggregateableFilteredClassifier toAggregate) throws Exception {

    if (!(getClassifier() instanceof Aggregateable)) {
      throw new IllegalArgumentException(
        "Can't aggregate because base classifier is not Aggregateable!");
    }

    if (!getClassifier().getClass().isAssignableFrom(
      toAggregate.getClassifier().getClass())) {
      throw new IllegalArgumentException(
        "The base classifier to aggregate is not of the same "
          + "type as our base classifier!");
    }

    ((Aggregateable) getClassifier()).aggregate(toAggregate.getClassifier());

    return this;
  }

  @Override
  public void finalizeAggregation() throws Exception {

    ((Aggregateable) getClassifier()).finalizeAggregation();
  }

  /**
   * Gets the filter specification string, which contains the class name of the
   * filter and any options to the filter
   * 
   * @return the filter string.
   */
  protected String getFilterSpec() {

    Filter c = getPreConstructedFilter();
    if (c instanceof OptionHandler) {
      return c.getClass().getName() + " "
        + Utils.joinOptions(((OptionHandler) c).getOptions());
    }
    return c.getClass().getName();
  }

  @Override
  public String toString() {
    if (m_filteredInstances == null) {
      return "AggregateableFilteredClassifier: No model built yet";
    }

    StringBuilder b = new StringBuilder();

    b.append("AggregateableFilterdClassifier using ")
      .append(getClassifierSpec()).append(" on data filtered through ")
      .append(getFilterSpec()).append("\n\n").append("Filtered Header\n")
      .append(m_filteredInstances.toString()).append(m_Classifier.toString());

    return b.toString();
  }
}
