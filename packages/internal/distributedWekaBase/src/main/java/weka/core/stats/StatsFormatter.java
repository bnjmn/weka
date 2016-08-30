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
 *    StatsFormatter
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.stats;

import weka.core.Attribute;
import weka.core.Instances;
import weka.core.Utils;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.DistributedWekaException;

/**
 * Class for formatting summary stats into nice readable output
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class StatsFormatter {

  /**
   * Format stats contained in an Instance header that contains summary
   * attributes
   *
   * @param headerWithSummary the header with summary attributes
   * @param quantiles true if quantiles were included
   * @param decimalPlaces the number of decimal places to use in the output
   * @return a formatted string containing the statistics for each attribute
   * @throws DistributedWekaException if a problem occurs
   */
  public static String formatStats(Instances headerWithSummary,
    boolean quantiles, int decimalPlaces) throws DistributedWekaException {

    Instances headerNoSummary =
      CSVToARFFHeaderReduceTask.stripSummaryAtts(headerWithSummary);

    Stats[] stats = new Stats[headerNoSummary.numAttributes()];
    for (int i = 0; i < headerNoSummary.numAttributes(); i++) {
      Attribute summary =
        headerWithSummary
          .attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
            + headerNoSummary.attribute(i).name());
      if (summary != null) {
        if (headerNoSummary.attribute(i).isNumeric()) {
          stats[i] = NumericStats.attributeToStats(summary);
        } else if (headerNoSummary.attribute(i).isString()) {
          stats[i] = StringStats.attributeToStats(summary);
        } else if (headerNoSummary.attribute(i).isNominal()) {
          stats[i] = NominalStats.attributeToStats(summary);
        }
      }
    }

    return formatStats(headerNoSummary, stats, quantiles, decimalPlaces);
  }

  /**
   * Format stats contained in the supplied array of Stats objects
   *
   * @param attributeStats the array of Stats objects
   * @param quantiles true if quantiles were included
   * @param decimalPlaces the number of decimal places to use in the output
   * @return a formatted string containing the statistics for each attribute
   */
  public static String formatStats(Instances header, Stats[] attributeStats,
    boolean quantiles, int decimalPlaces) {

    int maxAttNameLength = 0;
    for (int i = 0; i < header.numAttributes(); i++) {
      if (header.attribute(i).name().length() > maxAttNameLength) {
        maxAttNameLength = header.attribute(i).name().length();
      }
      if (header.attribute(i).isNumeric()) {
        ((NumericStats) attributeStats[i]).computeQuartilesAndHistogram();
      } else if (header.attribute(i).isString()) {
        ((StringStats) attributeStats[i]).computeDerived();
      }
    }
    if (maxAttNameLength < 12) {
      maxAttNameLength = 12;
    }

    StringBuilder numericBuilder = new StringBuilder();
    StringBuilder dateBuilder = new StringBuilder();
    StringBuilder nominalBuilder = new StringBuilder();
    StringBuilder stringBuilder = new StringBuilder();

    numericBuilder.append("Numeric attributes\n");
    numericBuilder.append(Utils.padLeft(" ", maxAttNameLength)).append(
      "    MISSING      COUNT        SUM      SUMSQ        MIN        MAX "
        + "      MEAN     STDDEV ");
    if (quantiles) {
      numericBuilder.append("1stQUARTLE     MEDIAN 3rdQUARTLE");
    }
    numericBuilder.append("\n");

    nominalBuilder.append("Nominal attributes\n");
    stringBuilder.append("String attributes\n");
    stringBuilder.append(Utils.padLeft(" ", maxAttNameLength)).append(
      "    MISSING      COUNT        SUM      SUMSQ        MIN        MAX "
        + "      MEAN     STDDEV\n");
    dateBuilder.append("Date attributes\n");
    dateBuilder.append(Utils.padLeft(" ", maxAttNameLength)).append(
      "    MISSING                    MIN                    MAX\n");

    for (int i = 0; i < header.numAttributes(); i++) {
      String attName = header.attribute(i).name();
      if (attName.length() < maxAttNameLength) {
        attName = Utils.padLeft(attName, maxAttNameLength);
      }

      if (header.attribute(i).isDate()) {
        dateBuilder.append(Utils.padLeft(attName, maxAttNameLength))
          .append(" ");
        double[] stats = ((NumericStats) attributeStats[i]).getStats();
        dateBuilder.append(
          formatNumber(stats[ArffSummaryNumericMetric.MISSING.ordinal()]))
          .append(" ");
        double min = stats[ArffSummaryNumericMetric.MIN.ordinal()];
        double max = stats[ArffSummaryNumericMetric.MAX.ordinal()];
        dateBuilder.append(
          Utils.padLeft(header.attribute(i).formatDate(min), 22)).append(" ");
        dateBuilder.append(
          Utils.padLeft(header.attribute(i).formatDate(max), 22)).append(" ");
        dateBuilder.append("\n");
      } else if (header.attribute(i).isNumeric()) {
        numericBuilder.append(Utils.padLeft(attName, maxAttNameLength)).append(
          " ");
        double[] stats = ((NumericStats) attributeStats[i]).getStats();
        appendNumericLine(numericBuilder, stats, quantiles, decimalPlaces);
      } else if (header.attribute(i).isNominal()) {
        nominalBuilder.append(Utils.padLeft(attName, maxAttNameLength)).append(
          " ");
        nominalBuilder.append("[missing]").append(":")
          .append(((NominalStats) attributeStats[i]).getNumMissing())
          .append(" ");
        for (int j = 0; j < header.attribute(i).numValues(); j++) {
          String attVal = header.attribute(i).value(j);
          double count = ((NominalStats) attributeStats[i]).getCount(attVal);
          if (Utils.isMissingValue(count)) {
            count = 0;
          }
          nominalBuilder.append(attVal).append(":")
            .append(formatNumberNoWidth(count, decimalPlaces)).append(" ");
        }
        nominalBuilder.append("\n");
      } else if (header.attribute(i).isString()) {
        double[] strLenStats =
          ((StringStats) attributeStats[i]).getStringLengthStats().getStats();
        double[] wordCountStats =
          ((StringStats) attributeStats[i]).getWordCountStats().getStats();
        stringBuilder.append(Utils.padLeft(attName, maxAttNameLength)).append(
          "\n");
        stringBuilder.append(Utils.padLeft("(str len)", maxAttNameLength))
          .append(" ");
        appendNumericLine(stringBuilder, strLenStats, false, decimalPlaces);
        stringBuilder.append(Utils.padLeft("(word count)", maxAttNameLength))
          .append(" ");
        appendNumericLine(stringBuilder, wordCountStats, false, decimalPlaces);
      }
    }

    String result = "";
    if (header.checkForAttributeType(Attribute.DATE)) {
      result += "\n" + dateBuilder.toString();
    }
    if (header.checkForAttributeType(Attribute.NUMERIC)) {
      result += "\n" + numericBuilder.toString();
    }
    if (header.checkForAttributeType(Attribute.STRING)) {
      result += "\n" + stringBuilder.toString();
    }
    if (header.checkForAttributeType(Attribute.NOMINAL)) {
      result += "\n" + nominalBuilder.toString();
    }

    return result;
  }

  protected static void appendNumericLine(StringBuilder builder,
    double[] stats, boolean computeQuantiles, int decimalPlaces) {
    builder.append(
      formatNumber(stats[ArffSummaryNumericMetric.MISSING.ordinal()])).append(
      " ");
    builder.append(
      formatNumber(stats[ArffSummaryNumericMetric.COUNT.ordinal()]))
      .append(" ");
    builder
      .append(
        formatNumber(stats[ArffSummaryNumericMetric.SUM.ordinal()],
          decimalPlaces)).append(" ");
    builder.append(
      formatNumber(stats[ArffSummaryNumericMetric.SUMSQ.ordinal()],
        decimalPlaces)).append(" ");
    double min = stats[ArffSummaryNumericMetric.MIN.ordinal()];
    if (Utils.isMissingValue(min)) {
      min = 0;
    }
    builder.append(formatNumber(min, decimalPlaces)).append(" ");
    double max = stats[ArffSummaryNumericMetric.MAX.ordinal()];
    if (Utils.isMissingValue(max)) {
      max = 0;
    }
    builder.append(formatNumber(max, decimalPlaces)).append(" ");
    builder.append(
      formatNumber(stats[ArffSummaryNumericMetric.MEAN.ordinal()],
        decimalPlaces)).append(" ");
    builder.append(
      formatNumber(stats[ArffSummaryNumericMetric.STDDEV.ordinal()],
        decimalPlaces)).append(" ");
    if (computeQuantiles) {
      double firstQ = stats[ArffSummaryNumericMetric.FIRSTQUARTILE.ordinal()];
      if (Utils.isMissingValue(firstQ)) {
        firstQ = 0;
      }
      builder.append(formatNumber(firstQ, 2)).append(" ");
      double median = stats[ArffSummaryNumericMetric.MEDIAN.ordinal()];
      if (Utils.isMissingValue(median)) {
        median = 0;
      }
      builder.append(formatNumber(median, decimalPlaces)).append(" ");
      double thirdQ = stats[ArffSummaryNumericMetric.THIRDQUARTILE.ordinal()];
      if (Utils.isMissingValue(thirdQ)) {
        thirdQ = 0;
      }
      builder.append(formatNumber(thirdQ, decimalPlaces));
    }
    builder.append("\n");
  }

  public static String formatNumber(double number) {
    return formatNumber(number, 0);
  }

  public static String formatNumber(double number, int decimals) {
    String result = "";
    if ((int) number == number) {
      result = String.format("%1$10d", (int) number);
    } else {
      result = String.format("%1$10." + decimals + "f", number);
    }
    if (result.length() > 10) {
      result = String.format("%1$10.4" + "e", number);
    }
    return result;
  }

  public static String formatNumberNoWidth(double number, int decimals) {
    String result = "";
    if ((int) number == number) {
      result = String.format("%1$d", (int) number);
    } else {
      result = String.format("%1$." + decimals + "f", number);
    }
    if (result.length() > 10) {
      result = String.format("%1$10.4" + "e", number);
    }
    return result;
  }
}
