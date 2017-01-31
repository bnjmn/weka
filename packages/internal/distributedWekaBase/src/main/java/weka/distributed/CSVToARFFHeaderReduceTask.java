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

import weka.core.Attribute;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.stats.ArffSummaryNumericMetric;
import weka.core.stats.NominalStats;
import weka.core.stats.NumericAttributeBinData;
import weka.core.stats.NumericStats;
import weka.core.stats.QuantileCalculator;
import weka.core.stats.Stats;
import weka.core.stats.StringStats;
import weka.core.stats.TDigest;
import weka.distributed.CSVToARFFHeaderMapTask.HeaderAndQuantileDataHolder;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
   * Performs aggregation over a list of header and quantile data holder
   * objects. Produces a final aggregated heaer with quantile and histogram data
   * + normal stats for numeric attributes.
   * 
   * @param toAggregate the list of header and quantile data holders to
   *          aggregate over
   * @return a final aggregated header with normal stats plus quantiles and
   *         histograms for numeric attributes
   * @throws DistributedWekaException if a problem occurs
   */
  public static Instances aggregateHeadersAndQuartiles(
    List<HeaderAndQuantileDataHolder> toAggregate)
    throws DistributedWekaException {

    // do the headers first
    List<Instances> headerList = new ArrayList<Instances>();
    for (HeaderAndQuantileDataHolder h : toAggregate) {
      headerList.add(h.getHeader());
    }

    Instances aggregatedHeader = aggregate(headerList);
    ArrayList<Attribute> newAtts = new ArrayList<Attribute>();

    Instances noSummary = stripSummaryAtts(aggregatedHeader);

    // add in the normal attributes first
    for (int i = 0; i < noSummary.numAttributes(); i++) {
      newAtts.add((Attribute) noSummary.attribute(i).copy());
    }

    // now do the quantile estimators (if any) attribute by attribute
    for (int i = 0; i < noSummary.numAttributes(); i++) {
      if (noSummary.attribute(i).isNumeric()) {
        String name = noSummary.attribute(i).name();
        List<TDigest> toMerge = new ArrayList<TDigest>();

        // now loop over the quantile estimators for this attribute
        for (HeaderAndQuantileDataHolder h : toAggregate) {
          try {
            TDigest decoded = h.getQuantileEstimator(name);

            if (decoded != null) {
              toMerge.add(decoded);
            } else {
              System.err
                .println("[CSVReducer] Partial quantile estimator for attribute '"
                  + name + "' is null!");
            }
          } catch (DistributedWekaException ex) {
            // just report to sys error here because there
            // may be data splits with all missing values or something
            System.err
              .println("[CSVReducer] No partial quantile estimator for attribute '"
                + name + "'");
          }
        }

        Attribute summary =
          (Attribute) aggregatedHeader.attribute(
            CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
              + noSummary.attribute(i).name()).copy();
        if (toMerge.size() > 0) {
          TDigest mergedForAtt =
            TDigest.merge(toMerge.get(0).compression(), toMerge);

          NumericStats newStats = NumericStats.attributeToStats(summary);
          newStats.setQuantileEstimator(mergedForAtt);
          newStats.setCompression(mergedForAtt.compression());
          newStats.computeQuartilesAndHistogram();

          // add the updated attribute with quantiles and histogram
          newAtts.add(newStats.makeAttribute());
        } else {
          newAtts.add(summary);
        }
      } else {
        // get this summary attribute unchanged
        Attribute summary =
          (Attribute) aggregatedHeader.attribute(
            CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
              + noSummary.attribute(i).name()).copy();

        if (summary != null) {
          newAtts.add(summary);
        }
      }
    }

    Instances newHeader =
      new Instances(aggregatedHeader.relationName(), newAtts, 0);

    return newHeader;
  }

  /**
   * Aggregates a list of Instances (headers) into a final Instances object.
   * 
   * @param headers a list of headers to aggregate
   * @return a unified header as an Instances object
   * @throws DistributedWekaException if a problem occurs
   */
  public static Instances aggregate(List<Instances> headers)
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

    Map<Integer, Attribute> masterNumericNominalMismatch =
      new HashMap<Integer, Attribute>();

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
    List<Attribute> summaryStats =
      aggregateSummaryStats(headers, masterHeaderCheck);
    for (Attribute a : summaryStats) {
      attribs.add(a);
    }

    return new Instances(master.relationName(), attribs, 0);
  }

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
   * Updates a header that contains summary attributes with quartiles and
   * histogram data. Assumes the header does not already contain these.
   * 
   * @param trainingHeaderWithSummary header with first pass summary data
   * @param quartiles QuartileCalculator containing quartiles to add to the
   *          header
   * @param histograms Map (keyed by attribute index) of histogram data for
   *          numeric attributes to add to the header
   * @return an updated header
   * @throws DistributedWekaException if a problem occurs
   */
  public static Instances updateSummaryAttsWithQuartilesAndHistograms(
    Instances trainingHeaderWithSummary, QuantileCalculator quartiles,
    Map<Integer, NumericAttributeBinData> histograms)
    throws DistributedWekaException {

    Instances trainingHeader = stripSummaryAtts(trainingHeaderWithSummary);

    // have to construct a new Instances header
    ArrayList<Attribute> atts = new ArrayList<Attribute>();
    for (int i = 0; i < trainingHeader.numAttributes(); i++) {
      atts.add((Attribute) trainingHeader.attribute(i).copy());
    }

    for (int i = 0; i < trainingHeader.numAttributes(); i++) {
      String name = trainingHeader.attribute(i).name();

      Attribute summary =
        trainingHeaderWithSummary
          .attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
            + name);
      if (trainingHeader.attribute(i).isNumeric()) {

        NumericStats stats = NumericStats.attributeToStats(summary);
        // add in the quantiles
        try {
          double[] quantiles = quartiles.getQuantiles(name);
          stats.getStats()[ArffSummaryNumericMetric.FIRSTQUARTILE.ordinal()] =
            quantiles[0];
          stats.getStats()[ArffSummaryNumericMetric.MEDIAN.ordinal()] =
            quantiles[1];
          stats.getStats()[ArffSummaryNumericMetric.THIRDQUARTILE.ordinal()] =
            quantiles[2];

          NumericAttributeBinData hist = histograms.get(i);
          if (!hist.getAttributeName().equals(name)) {
            throw new DistributedWekaException("Histogram data at index " + i
              + "(" + hist.getAttributeName()
              + ") does not match quantile data (" + name + ")!");
          }
          stats.setHistogramData(hist.getBinLabels(), hist.getBinFreqs());

          Attribute updatedSummary = stats.makeAttribute();
          atts.add(updatedSummary);
        } catch (Exception ex) {
          // Print out error, but don't cause job to stop (could be
          // the case that we haven't seen more than 5 non-missing
          // values for this attribute (quantile estimator requires at
          // least five values)
          System.err.println(ex);

          // just add in the old stats in this case
          atts.add(summary);
        }
      } else {
        if (summary != null) {
          atts.add((Attribute) summary.copy());
        }
      }
    }

    // make the new instances
    Instances updatedHeader = new Instances("Updated with quartiles", atts, 0);

    return updatedHeader;
  }

  /**
   * Returns true if the supplied header contains numeric attributes
   * 
   * @param headerWithSummary a header (with summary attributes) to check
   * @return true if the header contains numeric attributes
   * @throws DistributedWekaException if a problem occurs
   */
  public static boolean headerContainsNumericAttributes(
    Instances headerWithSummary) throws DistributedWekaException {

    Instances headerNoSummary =
      CSVToARFFHeaderReduceTask.stripSummaryAtts(headerWithSummary);
    boolean hasNumeric = false;
    for (int i = 0; i < headerNoSummary.numAttributes(); i++) {
      if (headerNoSummary.attribute(i).isNumeric()) {
        hasNumeric = true;
        break;
      }
    }

    return hasNumeric;
  }

  /**
   * Returns true if the supplied header already has quartile infomration
   * calculated and there are numeric attributes in the data
   * 
   * @param headerWithSummary the header to check
   * @return true if the supplied header has quartile information
   * @throws DistributedWekaException if a problem occurs
   */
  public static boolean headerContainsQuartiles(Instances headerWithSummary)
    throws DistributedWekaException {

    Instances headerNoSummary =
      CSVToARFFHeaderReduceTask.stripSummaryAtts(headerWithSummary);
    boolean hasQuartiles = false;
    for (int i = 0; i < headerNoSummary.numAttributes(); i++) {
      if (headerNoSummary.attribute(i).isNumeric()) {
        Attribute summary =
          headerWithSummary
            .attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
              + headerNoSummary.attribute(i).name());

        if (!Utils.isMissingValue(ArffSummaryNumericMetric.FIRSTQUARTILE
          .valueFromAttribute(summary))) {
          hasQuartiles = true;
          break;
        }
      }
    }

    return hasQuartiles;
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
    return ns.getStats();

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
    Map<String, Stats> aggStats =
      new LinkedHashMap<String, Stats>();

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
        Attribute original =
          masterHeaderCheck.attribute(current.name().replace(
            CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX, ""));
        if (original == null) {
          throw new DistributedWekaException(
            "Can't find corresponding original attribute for "
              + "summary stats attribute '" + current.name() + "'");
        }

        if (original.isNumeric()) {
          double[] currentStats = attributeToStatsArray(current);
          // ensure derived stats and quantiles are set to missing
          for (int k = ArffSummaryNumericMetric.MEAN.ordinal(); k < currentStats.length; k++) {
            currentStats[k] = Utils.missingValue();
          }
          NumericStats ns =
            (NumericStats) aggStats.get(original.name());
          if (ns == null) {
            ns = new NumericStats(original.name());
            ns.setStats(currentStats);
            aggStats.put(original.name(), ns);
          } else {
            for (ArffSummaryNumericMetric m : ArffSummaryNumericMetric.values()) {
              if (m == ArffSummaryNumericMetric.COUNT
                || m == ArffSummaryNumericMetric.SUM
                || m == ArffSummaryNumericMetric.SUMSQ
                || m == ArffSummaryNumericMetric.MISSING) {
                ns.getStats()[m.ordinal()] += currentStats[m.ordinal()];
              } else if (m == ArffSummaryNumericMetric.MIN) {
                if (currentStats[m.ordinal()] < ns.getStats()[m.ordinal()]) {
                  ns.getStats()[m.ordinal()] = currentStats[m.ordinal()];
                }
              } else if (m == ArffSummaryNumericMetric.MAX) {
                if (currentStats[m.ordinal()] > ns.getStats()[m.ordinal()]) {
                  ns.getStats()[m.ordinal()] = currentStats[m.ordinal()];
                }
              }
            }
          }
        } else if (original.isNominal()) {
          // nominal original attribute
          NominalStats ns =
            (NominalStats) aggStats.get(original.name());
          if (ns == null) {
            ns = new NominalStats(original.name());
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
            double missing =
              attributeToStatsArray(current)[ArffSummaryNumericMetric.MISSING
                .ordinal()];
            ns.add(null, missing);
          } else {

            for (int j = 0; j < current.numValues(); j++) {
              String v = current.value(j);
              String label = v.substring(0, v.lastIndexOf("_"));
              String freqCount =
                v.substring(v.lastIndexOf("_") + 1, v.length());
              try {
                double fC = Double.parseDouble(freqCount);
                if (label
                  .equals(NominalStats.MISSING_LABEL)) {
                  ns.add(null, fC);
                } else {
                  ns.add(label, fC);
                }
              } catch (NumberFormatException n) {
                throw new DistributedWekaException(n);
              }
            }
          }
        } else if (original.isString()) {
          StringStats currentStringStats = null;
          try {
            currentStringStats = StringStats.attributeToStats(current);
          } catch (IllegalArgumentException e) {
            // current is not string - means that there has been at least one
            // split of all missing values, or cases where a value was not parsable
            // as a number. In both cases, we drop back to treating the attribute
            // in question as string. There will be lost statistics, but the user
            // should examine the stats and realize that there is a data quality issue
            currentStringStats = new StringStats(original.name());
          }

          StringStats ss =
            (StringStats) aggStats.get(original.name());

          if (ss == null) {
            // ss = new CSVToARFFHeaderMapTask.StringStats(original.name());
            aggStats.put(original.name(), currentStringStats);
          } else {
            for (ArffSummaryNumericMetric m : ArffSummaryNumericMetric.values()) {
              if (m == ArffSummaryNumericMetric.COUNT
                || m == ArffSummaryNumericMetric.SUM
                || m == ArffSummaryNumericMetric.SUMSQ
                || m == ArffSummaryNumericMetric.MISSING) {
                ss.getStringLengthStats().getStats()[m.ordinal()] +=
                  currentStringStats.getStringLengthStats().getStats()[m
                    .ordinal()];
                ss.getWordCountStats().getStats()[m.ordinal()] +=
                  currentStringStats.getWordCountStats().getStats()[m.ordinal()];
              } else if (m == ArffSummaryNumericMetric.MIN) {
                if (currentStringStats.getStringLengthStats().getStats()[m
                  .ordinal()] < ss.getStringLengthStats().getStats()[m
                  .ordinal()]) {
                  ss.getStringLengthStats().getStats()[m.ordinal()] =
                    currentStringStats.getStringLengthStats().getStats()[m
                      .ordinal()];
                  ss.getWordCountStats().getStats()[m.ordinal()] =
                    currentStringStats.getWordCountStats().getStats()[m
                      .ordinal()];
                }
              } else if (m == ArffSummaryNumericMetric.MAX) {
                if (currentStringStats.getStringLengthStats().getStats()[m
                  .ordinal()] > ss.getStringLengthStats().getStats()[m
                  .ordinal()]) {
                  ss.getStringLengthStats().getStats()[m.ordinal()] =
                    currentStringStats.getStringLengthStats().getStats()[m
                      .ordinal()];
                  ss.getWordCountStats().getStats()[m.ordinal()] =
                    currentStringStats.getWordCountStats().getStats()[m
                      .ordinal()];
                }
              }
            }
          }
        }
      }
    }

    for (Map.Entry<String, Stats> e : aggStats
      .entrySet()) {
      Stats stats = e.getValue();

      if (stats instanceof NumericStats) {
        // derived stats
        ((NumericStats) stats).computeDerived();
      } else if (stats instanceof StringStats) {
        ((StringStats) stats).computeDerived();
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
          Instances aHeader =
            new Instances(new BufferedReader(new FileReader(h)));
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
