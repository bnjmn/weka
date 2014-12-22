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
 *    DistributedUtils.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package distributed.core;

import weka.core.Attribute;
import weka.core.AttributeStats;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.stats.ArffSummaryNumericMetric;
import weka.core.stats.NominalStats;
import weka.core.stats.NumericStats;

/**
 * A few utility routines
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class DistributedUtils {

  public static NumericStats getNumericAttributeStatsSparse(
    Instances denormalized, int attIndex) {
    NumericStats ns = new NumericStats(denormalized.attribute(attIndex).name());

    for (int j = 0; j < denormalized.numInstances(); j++) {
      double value = denormalized.instance(j).value(attIndex);

      if (Utils.isMissingValue(value) || value == 0) {
        ns.getStats()[ArffSummaryNumericMetric.MISSING.ordinal()]++;
      } else {
        ns.getStats()[ArffSummaryNumericMetric.COUNT.ordinal()]++;
        ns.getStats()[ArffSummaryNumericMetric.SUM.ordinal()] += value;
        ns.getStats()[ArffSummaryNumericMetric.SUMSQ.ordinal()] += value
          * value;
        if (Double.isNaN(ns.getStats()[ArffSummaryNumericMetric.MIN.ordinal()])) {
          ns.getStats()[ArffSummaryNumericMetric.MIN.ordinal()] =
            ns.getStats()[ArffSummaryNumericMetric.MAX
              .ordinal()] = value;
        } else if (value < ns.getStats()[ArffSummaryNumericMetric.MIN.ordinal()]) {
          ns.getStats()[ArffSummaryNumericMetric.MIN.ordinal()] = value;
        } else if (value > ns.getStats()[ArffSummaryNumericMetric.MAX.ordinal()]) {
          ns.getStats()[ArffSummaryNumericMetric.MAX.ordinal()] = value;
        }
      }
    }

    ns.computeDerived();

    return ns;
  }

  public static Instances makeHeaderWithSummaryAtts(Instances denormalized,
    boolean treatZerosAsMissing) {
    Instances header = new Instances(denormalized, 0);

    for (int i = 0; i < denormalized.numAttributes(); i++) {
      AttributeStats stats = denormalized.attributeStats(i);
      if (denormalized.attribute(i).isNumeric()) {
        NumericStats ns = new NumericStats(denormalized.attribute(i).name());
        if (!treatZerosAsMissing) {
          ns.getStats()[ArffSummaryNumericMetric.MIN.ordinal()] =
            stats.numericStats.min;
          ns.getStats()[ArffSummaryNumericMetric.MAX.ordinal()] =
            stats.numericStats.max;
          ns.getStats()[ArffSummaryNumericMetric.COUNT.ordinal()] =
            stats.numericStats.count;
          ns.getStats()[ArffSummaryNumericMetric.SUM.ordinal()] =
            stats.numericStats.sum;
          ns.getStats()[ArffSummaryNumericMetric.SUMSQ.ordinal()] =
            stats.numericStats.sumSq;
          ns.getStats()[ArffSummaryNumericMetric.MISSING.ordinal()] =
            stats.missingCount;

          ns.computeDerived();
        } else {
          ns = getNumericAttributeStatsSparse(denormalized, i);
        }

        Attribute newAtt = ns.makeAttribute();
        header.insertAttributeAt(newAtt, header.numAttributes());
      } else if (denormalized.attribute(i).isNominal()) {
        NominalStats nom = new NominalStats(denormalized.attribute(i).name());
        nom.setNumMissing(stats.missingCount);

        double[] labelFreqs = stats.nominalWeights;
        for (int j = 0; j < denormalized.attribute(i).numValues(); j++) {
          nom.add(denormalized.attribute(i).value(j), labelFreqs[j]);
        }

        Attribute newAtt = nom.makeAttribute();
        header.insertAttributeAt(newAtt, header.numAttributes());
      }
    }

    return header;
  }
}
