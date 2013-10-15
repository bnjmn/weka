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
 *    CorrelationMatrixMapTask.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed;

import java.io.Serializable;
import java.util.List;

import weka.core.Attribute;
import weka.core.Instances;
import weka.distributed.CSVToARFFHeaderMapTask.ArffSummaryNumericMetric;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

/**
 * A reduce task that sums the incoming partial covariance sums for a given row
 * of the matrix and then computes the final correlation/ covariance values.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class CorrelationMatrixRowReduceTask implements Serializable {

  /** For serialization */
  private static final long serialVersionUID = -2314677791571204717L;

  /**
   * Aggregate a list of partial rows of the matrix.
   * 
   * @param matrixRowNumber the index of the row in the matrix to be aggregated
   * @param toAggregate a list rows to be aggregated
   * @param headerWithSummaryAtts the header of the data (including summary
   *          attributes)
   * @param missingsWereReplacedWithMeans true if missing values were replaced
   *          by means in the map tasks
   * @param covarianceInsteadOfCorrelation final matrix is to be a covariance
   *          one rather than correlation
   * @return the aggregated row
   * @throws DistributedWekaException if a problem occurs
   */
  public double[] aggregate(int matrixRowNumber, List<double[]> toAggregate,
    Instances headerWithSummaryAtts, boolean missingsWereReplacedWithMeans,
    boolean covarianceInsteadOfCorrelation) throws DistributedWekaException {

    StringBuilder rem = new StringBuilder();
    Instances trainingHeader = CSVToARFFHeaderReduceTask
      .stripSummaryAtts(headerWithSummaryAtts);

    // remove all nominal attributes
    for (int i = 0; i < trainingHeader.numAttributes(); i++) {
      if (!trainingHeader.attribute(i).isNumeric()) {
        rem.append("" + (i + 1)).append(",");
      }
    }
    if (rem.length() > 0) {
      Remove remove = new Remove();
      rem.deleteCharAt(rem.length() - 1); // remove the trailing ,
      String attIndices = rem.toString();
      remove.setAttributeIndices(attIndices);
      remove.setInvertSelection(false);

      try {
        remove.setInputFormat(trainingHeader);

        trainingHeader = Filter.useFilter(trainingHeader, remove);
      } catch (Exception ex) {
        throw new DistributedWekaException(ex);
      }
    }

    Attribute origCorrespondingToRow = trainingHeader
      .attribute(matrixRowNumber);

    Attribute correspondingSummaryAtt = headerWithSummaryAtts
      .attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
        + origCorrespondingToRow.name());

    if (correspondingSummaryAtt == null) {
      throw new DistributedWekaException(
        "Was unable to find the summary stats attribute for "
          + "original attribute '" + origCorrespondingToRow.name()
          + "' corresponding to matrix row number: " + matrixRowNumber);
    }

    if (toAggregate.size() == 0) {
      throw new DistributedWekaException("Nothing to aggregate!");
    }

    double[] aggregated = toAggregate.get(0).clone();
    for (int i = 1; i < toAggregate.size(); i++) {
      double[] toAgg = toAggregate.get(i);

      for (int j = 0; j < toAgg.length; j++) {
        aggregated[j] += toAgg[j];
      }
    }

    // correlation or covariance?
    double[] statsForRowAtt = CSVToARFFHeaderReduceTask
      .attributeToStatsArray(correspondingSummaryAtt);

    for (int i = 0; i < aggregated.length; i++) {
      Attribute attI = trainingHeader.attribute(i);
      Attribute currespondingSummaryI = headerWithSummaryAtts
        .attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
          + attI.name());
      double[] statsForI = CSVToARFFHeaderReduceTask
        .attributeToStatsArray(currespondingSummaryI);

      // if missings were replaced then sum is divided by the total count (i.e.
      // count + #missing); otherwise only min(countRowAtt, countIAtt) values
      // would have been multiplied and summed over all the map tasks
      double denom = missingsWereReplacedWithMeans ? statsForRowAtt[ArffSummaryNumericMetric.COUNT
        .ordinal()]
        + statsForRowAtt[ArffSummaryNumericMetric.MISSING.ordinal()] : Math
        .min(statsForRowAtt[ArffSummaryNumericMetric.COUNT.ordinal()],
          statsForI[ArffSummaryNumericMetric.COUNT.ordinal()]);

      if (covarianceInsteadOfCorrelation) {
        if (denom > 1) {
          aggregated[i] /= (denom - 1);
        } else {
          aggregated[i] = Double.POSITIVE_INFINITY;
        }
      } else {

        if (matrixRowNumber == i || denom <= 1) {
          aggregated[i] = 1.0;
        } else {
          double sR = statsForRowAtt[ArffSummaryNumericMetric.STDDEV.ordinal()];
          double sI = statsForI[ArffSummaryNumericMetric.STDDEV.ordinal()];

          if (sR * sI > 0.0) {
            aggregated[i] = aggregated[i] / ((denom - 1) * sR * sI);
          }
        }
      }
    }

    return aggregated;
  }
}
