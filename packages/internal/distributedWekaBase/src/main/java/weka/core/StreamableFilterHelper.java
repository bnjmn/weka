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
 *    StreamableFilterHelper.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import weka.filters.Filter;
import weka.filters.MakePreconstructedFilter;
import weka.filters.MultiFilter;
import weka.filters.PreconstructedFilter;
import weka.filters.StreamableFilter;
import distributed.core.DistributedJobConfig;

/**
 * Utility class for wrapping one or more StreamableFilters in a
 * MakePreconstructedFilter
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class StreamableFilterHelper {

  /**
   * The tool tip text for this property.
   * 
   * @return the tool tip text for this property
   */
  public String filtersToUseTipText() {
    return "Filters to pre-process the data with before "
      + "passing it to the classifier. Note that in order "
      + "to remain directly aggregateable to a single model "
      + "StreamableFilters must be used with Aggregateable classifiers.";
  }

  /**
   * Wraps a list of filters into a Preconstructed filter
   * 
   * @param filters the filters to wrap
   * @return a PreconstructedFilter
   * @throws Exception if a problem occurs
   */
  public static PreconstructedFilter wrapStreamableFilters(
    List<StreamableFilter> filters) throws Exception {

    PreconstructedFilter wrapper = null;

    List<Filter> finalList = new ArrayList<Filter>();
    for (StreamableFilter f : filters) {
      if (f instanceof PreconstructedFilter) {
        finalList.add((Filter) f);
      } else {
        MakePreconstructedFilter temp =
          new MakePreconstructedFilter((Filter) f);
        finalList.add(temp);
      }
    }

    if (finalList.size() > 1) {
      MultiFilter mf = new MultiFilter();
      mf.setFilters(finalList.toArray(new Filter[finalList.size()]));
      wrapper = new MakePreconstructedFilter(mf);
    } else {
      wrapper = (PreconstructedFilter) finalList.get(0);
    }

    return wrapper;
  }

  /**
   * Wraps specified filters into a Preconstructed filter
   * 
   * @param options options specifying the filters to use
   * @return a PreconstructedFilter or null if there are no usable
   *         StreamableFilters specified
   * @throws Exception if a problem occurs
   */
  public static PreconstructedFilter wrapStreamableFilters(String[] options)
    throws Exception {

    List<StreamableFilter> filtersToUse = new ArrayList<StreamableFilter>();

    while (true) {
      String filterString = Utils.getOption("filter", options);
      if (DistributedJobConfig.isEmpty(filterString)) {
        break;
      }

      String[] spec = Utils.splitOptions(filterString);
      if (spec.length == 0) {
        throw new IllegalArgumentException(
          "Invalid filter specification string");
      }
      String filterClass = spec[0];
      Filter f = (Filter) Class.forName(filterClass).newInstance();
      if (!(f instanceof StreamableFilter)) {
        throw new Exception("Filter '" + filterClass
          + "' must be a StreamableFilter");
      }

      spec[0] = "";
      if (f instanceof OptionHandler) {
        ((OptionHandler) f).setOptions(spec);
      }

      filtersToUse.add((StreamableFilter) f);
    }

    if (filtersToUse.size() == 0) {
      return null;
    }

    return wrapStreamableFilters(filtersToUse);
  }

  /**
   * Return a list of options for this utility class
   * 
   * @return a list of options
   */
  public static Enumeration<Option> listOptions() {
    Vector<Option> options = new Vector<Option>();

    options
      .add(new Option(
        "\tSpecify a filter to pre-process the data with.\n\t"
          + "This filter must be a StreamableFilter, meaning that the output format\n\t"
          + "produced by the filter must be able to be determined directly from \n\t"
          + "the input data format (this makes the data format\n\t"
          + "compatible across map tasks). Many unsupervised attribute-based\n\t"
          + "filters are StreamableFilters.\n\t"
          + "This option may be supplied multiple times in order to apply more\n\t"
          + "than one filter.", "filter", 1, "-filter <filter spec>"));

    return options.elements();

  }

  /**
   * Create an option specification string for a filter
   * 
   * @param f the filter
   * @return the option spec
   */
  public static String getFilterSpec(Filter f) {

    return f.getClass().getName()
      + (f instanceof OptionHandler ? " "
        + Utils.joinOptions(((OptionHandler) f).getOptions()) : "");
  }

  /**
   * Utility method to return an array of options representing the configuration
   * of one or more filters wrapped in a PreconstructedFilter
   * 
   * @param filter the PreconstructedFilter to generate a list of options for
   * @return an array of options
   */
  public static String[] getOptions(PreconstructedFilter filter) {
    List<String> opts = new ArrayList<String>();

    if (filter instanceof MakePreconstructedFilter) {
      Filter baseFilter = ((MakePreconstructedFilter) filter).getBaseFilter();

      if (baseFilter instanceof MultiFilter) {
        Filter[] mfilters = ((MultiFilter) baseFilter).getFilters();

        for (Filter f : mfilters) {
          opts.add("-filter");
          opts.add(getFilterSpec(f));
        }
      } else {
        opts.add("-filter");
        opts.add(getFilterSpec(baseFilter));
      }
    } else {
      opts.add("-filter");
      opts.add(getFilterSpec((Filter) filter));
    }

    return opts.toArray(new String[opts.size()]);
  }
}
