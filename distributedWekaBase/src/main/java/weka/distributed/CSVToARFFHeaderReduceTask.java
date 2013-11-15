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
 *    CSVToARFFHeaderReduceTask.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import weka.core.Attribute;
import weka.core.Instances;
import weka.distributed.CSVToARFFHeaderMapTask.ArffSummaryNumericMetric;
import weka.distributed.CSVToARFFHeaderMapTask.NumericStats;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

/**
 * Reduce task for ARFF header and summary attribute creation.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class CSVToARFFHeaderReduceTask implements Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -2626548935034818537L;

  /**
   * Utility method that returns a header Instances object without any summary
   * attributes.
   * 
   * @param insts the header to remove summary attributes from
   * @return a new Instances object that does not contain any summary attributes
   * @throws DistributedWekaException if a problem occurs
   */
  public static Instances stripSummaryAtts(Instances insts)
    throws DistributedWekaException {
    int startOfSummary = 0;

    for (int i = 0; i < insts.numAttributes(); i++) {
      if (insts.attribute(i).name()
        .startsWith(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX)) {
        startOfSummary = i + 1;
        break;
      }
    }

    if (startOfSummary > 0) {
      Remove r = new Remove();
      r.setAttributeIndices("" + startOfSummary + "-" + "last");
      try {
        r.setInputFormat(insts);
        insts = Filter.useFilter(insts, r);
      } catch (Exception ex) {
        throw new DistributedWekaException(ex);
      }
    }

    return insts;
  }

  /**
   * Aggregates a list of Instances (headers) into a final Instances object.
   * 
   * @param headers a list of headers to aggregate
   * @return a unified header as an Instances object
   * @throws DistributedWekaException if a problem occurs
   */
  public Instances aggregate(List<Instances> headers)
    throws DistributedWekaException {

    if (headers.size() == 0) {
      throw new DistributedWekaException("Nothing to aggregate!");
    }

    // basic checks first - all instances have the same number of attributes
    // with
    // the same names and types in the same order

    // use the first as the master
    Instances master = headers.get(0);
    Instances masterHeaderCheck = stripSummaryAtts(master);

    Map<Integer, Attribute> masterNumericNominalMismatch = new HashMap<Integer, Attribute>();

    for (int i = 1; i < headers.size(); i++) {
      Instances toCheck = headers.get(i);
      toCheck = stripSummaryAtts(toCheck);

      if (toCheck.numAttributes() != masterHeaderCheck.numAttributes()) {
        System.err.println("Master:\n\n" + masterHeaderCheck);
        System.err.println("\nTo aggregate # " + (i + 1) + ":\n\n" + toCheck);

        throw new DistributedWekaException(
          "Number of attributes differ between headers to aggregate!");
      }

      for (int j = 0; j < masterHeaderCheck.numAttributes(); j++) {
        if (!toCheck.attribute(j).name()
          .equals(masterHeaderCheck.attribute(j).name())) {
          System.err.println("Master:\n\n" + masterHeaderCheck);
          System.err.println("\nTo aggregate # " + (i + 1) + ":\n\n" + toCheck);

          throw new DistributedWekaException(
            "Attribute names differ in headers to aggregate: att (master) '"
              + masterHeaderCheck.attribute(j).name()
              + "' != att (to aggregate) '" + toCheck.attribute(j).name()
              + "' at pos " + (j + 1));
        }

        if (toCheck.attribute(j).type() != masterHeaderCheck.attribute(j)
          .type()) {
          System.err.println("Master:\n\n" + master);
          System.err.println("\nTo aggregate # " + (i + 1) + ":\n\n" + toCheck);

          // We might have a chunk of data that has had all missing values
          // for this particular attribute. In this case, the default is
          // to create a numeric attribute. It's corresponding summary
          // stats attribute (if we're creating them) will show a missing
          // count > 0 and a count == 0.
          boolean throwException = false;
          if (masterHeaderCheck.attribute(j).isNumeric()) {
            if (toCheck.attribute(j).isNominal()
              || toCheck.attribute(j).isString()) {
              masterNumericNominalMismatch.put(new Integer(j),
                toCheck.attribute(j));
            } else {
              throwException = true;
            }
          } else if (toCheck.attribute(j).isNumeric()) {
            if (!masterHeaderCheck.attribute(j).isNominal()
              && !masterHeaderCheck.attribute(j).isString()) {
              throwException = true;
            }
          } else {
            throwException = true;
          }

          if (throwException) {
            throw new DistributedWekaException(
              "Types differ in headers to aggregate: att (master) '"
                + masterHeaderCheck.attribute(j).name() + "' ("
                + Attribute.typeToString(masterHeaderCheck.attribute(j))
                + ") != att (to aggregate) '" + toCheck.attribute(j).name()
                + "' (" + Attribute.typeToString(toCheck.attribute(j))
                + ") at pos " + (j + 1));
          }
        }
      }
    }

    if (masterNumericNominalMismatch.size() > 0) {
      ArrayList<Attribute> fixedAtts = new ArrayList<Attribute>();

      for (int i = 0; i < masterHeaderCheck.numAttributes(); i++) {
        Attribute a = (Attribute) masterHeaderCheck.attribute(i).copy();
        if (masterNumericNominalMismatch.get(new Integer(i)) != null) {
          Attribute target = masterNumericNominalMismatch.get(new Integer(i));
          if (target.isNominal()) {
            // replace this with a dummy nominal one
            ArrayList<String> dummyVals = new ArrayList<String>();
            dummyVals.add("Dummy");
            a = new Attribute(a.name(), dummyVals);
          } else {
            // string
            a = new Attribute(a.name(), (List<String>) null);
          }
        }
        fixedAtts.add(a);
      }

      masterHeaderCheck = new Instances(master.relationName(), fixedAtts, 0);
    }

    // now aggregate
    ArrayList<Attribute> attribs = new ArrayList<Attribute>();
    for (int i = 0; i < masterHeaderCheck.numAttributes(); i++) {
      if (masterHeaderCheck.attribute(i).isNominal()) {
        ArrayList<String> vals = new ArrayList<String>();
        for (Instances h : headers) {
          Attribute toAgg = h.attribute(i);

          // check for numeric type here...
          if (toAgg.isNumeric()) {
            // this data chunk must have seen all missing values
            // for this attribute
          } else {
            for (int z = 0; z < toAgg.numValues(); z++) {
              if (!vals.contains(toAgg.value(z))) {
                vals.add(toAgg.value(z));
              }
            }
          }
        }
        Collections.sort(vals);
        attribs.add(new Attribute(masterHeaderCheck.attribute(i).name(), vals));
      } else if (masterHeaderCheck.attribute(i).isString()) {
        attribs.add(new Attribute(masterHeaderCheck.attribute(i).name(),
          (java.util.List<String>) null));
      } else if (masterHeaderCheck.attribute(i).isDate()) {
        attribs.add(new Attribute(masterHeaderCheck.attribute(i).name(),
          masterHeaderCheck.attribute(i).getDateFormat()));
      } else if (masterHeaderCheck.attribute(i).isRelationValued()) {
        Instances masterR = masterHeaderCheck.attribute(i).relation();

        // scan across the headers for inconsistent relational att
        for (int j = 1; j < headers.size(); j++) {
          Instances toCheck = headers.get(j);
          Instances checkR = toCheck.attribute(i).relation();

          String problem = masterR.equalHeadersMsg(checkR);
          if (problem != null) {
            throw new DistributedWekaException("Relational attribute '"
              + master.attribute(i).name()
              + "' differs in structure amongs the headers to be aggregated!");
          }
        }

        attribs.add(new Attribute(masterHeaderCheck.attribute(i).name(),
          new Instances(masterR, 0)));
      } else {
        // numeric
        attribs.add(new Attribute(masterHeaderCheck.attribute(i).name()));
      }
    }

    // Any summary stats atts?
    List<Attribute> summaryStats = aggregateSummaryStats(headers,
      masterHeaderCheck);
    for (Attribute a : summaryStats) {
      attribs.add(a);
    }

    return new Instances(master.relationName(), attribs, 0);
  }

  /**
   * Return the numeric stats for a given summary attribute as an array
   * 
   * @param a the summary attribute to get stats from
   * @return an array of stats
   * @throws IllegalArgumentException if a problem occurs
   */
  protected static double[] attributeToStatsArray(Attribute a)
    throws IllegalArgumentException {

    NumericStats ns = NumericStats.attributeToStats(a);
    return ns.m_stats;

  }

  /**
   * Aggregates the summary statistics from all the headers
   * 
   * @param headers the headers to aggregate
   * @param masterHeaderCheck the reference header with final types determined
   * @return a list of aggregated Stats objects
   * @throws DistributedWekaException if a problem occurs
   */
  protected static List<Attribute> aggregateSummaryStats(
    List<Instances> headers, Instances masterHeaderCheck)
    throws DistributedWekaException {
    List<Attribute> aggregated = new ArrayList<Attribute>();
    Map<String, CSVToARFFHeaderMapTask.Stats> aggStats = new LinkedHashMap<String, CSVToARFFHeaderMapTask.Stats>();

    int index = -1;

    // scan for the start index of summary attributes
    for (Instances h : headers) {
      for (int i = 0; i < h.numAttributes(); i++) {
        if (h.attribute(i).name()
          .startsWith(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX)) {
          index = i;
          break;
        }
      }
    }

    if (index < 0) {
      return aggregated;
    }

    for (Instances h : headers) {
      for (int i = index; i < h.numAttributes(); i++) {
        Attribute current = h.attribute(i);
        Attribute original = masterHeaderCheck.attribute(current.name()
          .replace(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX, ""));
        if (original == null) {
          throw new DistributedWekaException(
            "Can't find corresponding original attribute for "
              + "summary stats attribute '" + current.name() + "'");
        }

        if (original.isNumeric()) {
          double[] currentStats = attributeToStatsArray(current);
          CSVToARFFHeaderMapTask.NumericStats ns = (CSVToARFFHeaderMapTask.NumericStats) aggStats
            .get(original.name());
          if (ns == null) {
            ns = new CSVToARFFHeaderMapTask.NumericStats(original.name());
            ns.m_stats = currentStats;
            aggStats.put(original.name(), ns);
          } else {
            for (ArffSummaryNumericMetric m : ArffSummaryNumericMetric.values()) {
              if (m == ArffSummaryNumericMetric.COUNT
                || m == ArffSummaryNumericMetric.SUM
                || m == ArffSummaryNumericMetric.SUMSQ
                || m == ArffSummaryNumericMetric.MISSING) {
                ns.m_stats[m.ordinal()] += currentStats[m.ordinal()];
              } else if (m == ArffSummaryNumericMetric.MIN) {
                if (currentStats[m.ordinal()] < ns.m_stats[m.ordinal()]) {
                  ns.m_stats[m.ordinal()] = currentStats[m.ordinal()];
                }
              } else if (m == ArffSummaryNumericMetric.MAX) {
                if (currentStats[m.ordinal()] > ns.m_stats[m.ordinal()]) {
                  ns.m_stats[m.ordinal()] = currentStats[m.ordinal()];
                }
              }
            }
          }
        } else {
          // nominal original attribute
          CSVToARFFHeaderMapTask.NominalStats ns = (CSVToARFFHeaderMapTask.NominalStats) aggStats
            .get(original.name());
          if (ns == null) {
            ns = new CSVToARFFHeaderMapTask.NominalStats(original.name());
            aggStats.put(original.name(), ns);
          }

          // Is the current attribute actually an numeric stats one?
          // Could happen if a data chunk had all missing values for
          // this attribute. In this case the default (in the case
          // where the user has not explicitly provided types)
          // is to assume numeric.

          // check number of values and the name of a few labels
          if (current.numValues() == ArffSummaryNumericMetric.values().length
            && current.value(ArffSummaryNumericMetric.COUNT.ordinal())
              .startsWith(ArffSummaryNumericMetric.COUNT.toString())
            && current.value(ArffSummaryNumericMetric.STDDEV.ordinal())
              .startsWith(ArffSummaryNumericMetric.STDDEV.toString())) {
            // OK - copy over the missing count
            double missing = attributeToStatsArray(current)[ArffSummaryNumericMetric.MISSING
              .ordinal()];
            ns.add(null, missing);
          } else {

            for (int j = 0; j < current.numValues(); j++) {
              String v = current.value(j);
              String label = v.substring(0, v.lastIndexOf("_"));
              String freqCount = v
                .substring(v.lastIndexOf("_") + 1, v.length());
              try {
                double fC = Double.parseDouble(freqCount);
                if (label
                  .equals(CSVToARFFHeaderMapTask.NominalStats.MISSING_LABEL)) {
                  ns.add(null, fC);
                } else {
                  ns.add(label, fC);
                }
              } catch (NumberFormatException n) {
                throw new DistributedWekaException(n);
              }
            }
          }
        }
      }
    }

    for (Map.Entry<String, CSVToARFFHeaderMapTask.Stats> e : aggStats
      .entrySet()) {
      CSVToARFFHeaderMapTask.Stats stats = e.getValue();

      if (stats instanceof CSVToARFFHeaderMapTask.NumericStats) {
        // derived stats
        ((CSVToARFFHeaderMapTask.NumericStats) stats).computeDerived();
      }
      Attribute newAtt = stats.makeAttribute();
      aggregated.add(newAtt);
    }

    return aggregated;
  }

  public static void main(String[] args) {
    try {
      List<Instances> headerList = new ArrayList<Instances>();

      for (String h : args) {
        if (h != null && h.length() > 0) {
          Instances aHeader = new Instances(new BufferedReader(
            new FileReader(h)));
          aHeader = new Instances(aHeader, 0);
          headerList.add(aHeader);
        }
      }

      if (headerList.size() > 0) {
        CSVToARFFHeaderReduceTask task = new CSVToARFFHeaderReduceTask();
        Instances aggregated = task.aggregate(headerList);
        System.out.println("Aggregated header\n\n" + aggregated.toString());
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

}
