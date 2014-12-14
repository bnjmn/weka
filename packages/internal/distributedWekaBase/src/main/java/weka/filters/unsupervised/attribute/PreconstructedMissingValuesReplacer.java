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
 *    PreconstructedMissingValuesReplacer.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.filters.unsupervised.attribute;

import java.io.Serializable;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.core.Utils;
import weka.core.stats.ArffSummaryNumericMetric;
import weka.core.stats.NominalStats;
import weka.core.stats.NumericStats;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.DistributedWekaException;
import weka.filters.PreconstructedFilter;
import weka.filters.SimpleStreamFilter;
import weka.gui.GPCIgnore;
import weka.gui.beans.KFIgnore;

/**
 * Preconstructed filter for replacing missing values with mean/mode
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFIgnore
@GPCIgnore
public class PreconstructedMissingValuesReplacer extends SimpleStreamFilter
  implements Serializable, PreconstructedFilter {

  /** For serialization */
  private static final long serialVersionUID = 6343310744702405761L;

  /** The means and modes */
  protected double[] m_meansAndModes;

  /**
   * Constructor. Takes a set of instances with summary metadata attributes.
   * 
   * @param headerWithSummary the header of the data that includes summary
   *          attributes
   * @throws Exception if a problem occurs
   */
  public PreconstructedMissingValuesReplacer(Instances headerWithSummary)
    throws Exception {
    Instances headerNoSummary = CSVToARFFHeaderReduceTask
      .stripSummaryAtts(headerWithSummary);

    m_meansAndModes = new double[headerNoSummary.numAttributes()];

    for (int i = 0; i < headerNoSummary.numAttributes(); i++) {
      Attribute orig = headerNoSummary.attribute(i);
      Attribute summary = headerWithSummary
        .attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
          + orig.name());

      if (summary == null) {
        throw new DistributedWekaException(
          "Unable to find correspoding summary attribute for '" + orig.name()
            + "'");
      }

      if (orig.isNumeric()) {
        m_meansAndModes[i] =
          NumericStats.attributeToStats(summary).getStats()[ArffSummaryNumericMetric.MEAN
            .ordinal()];
      } else if (orig.isNominal()) {
        m_meansAndModes[i] = NominalStats.attributeToStats(summary).getMode();
        if (m_meansAndModes[i] < 0) {
          m_meansAndModes[i] = Utils.missingValue();
        }
      }
    }

    setDoNotCheckCapabilities(true);
    setInputFormat(headerNoSummary);
  }

  @Override
  public boolean isConstructed() {
    return true;
  }

  @Override
  protected Instances determineOutputFormat(Instances inputFormat)
    throws Exception {
    return new Instances(inputFormat, 0);
  }

  @Override
  protected Instance process(Instance instance) throws Exception {
    Instance inst = null;
    if (instance instanceof SparseInstance) {
      double[] vals = new double[instance.numValues()];
      int[] indices = new int[instance.numValues()];
      int num = 0;
      for (int j = 0; j < instance.numValues(); j++) {
        if (instance.isMissingSparse(j)
          && (getInputFormat().classIndex() != instance.index(j))
          && (instance.attributeSparse(j).isNominal() || instance
            .attributeSparse(j).isNumeric())) {
          if (m_meansAndModes[instance.index(j)] != 0.0) {
            vals[num] = m_meansAndModes[instance.index(j)];
            indices[num] = instance.index(j);
            num++;
          }
        } else {
          vals[num] = instance.valueSparse(j);
          indices[num] = instance.index(j);
          num++;
        }
      }
      if (num == instance.numValues()) {
        inst = new SparseInstance(instance.weight(), vals, indices,
          instance.numAttributes());
      } else {
        double[] tempVals = new double[num];
        int[] tempInd = new int[num];
        System.arraycopy(vals, 0, tempVals, 0, num);
        System.arraycopy(indices, 0, tempInd, 0, num);
        inst = new SparseInstance(instance.weight(), tempVals, tempInd,
          instance.numAttributes());
      }
    } else {
      double[] vals = new double[getInputFormat().numAttributes()];
      for (int j = 0; j < instance.numAttributes(); j++) {
        if (instance.isMissing(j)
          && (getInputFormat().classIndex() != j)
          && (getInputFormat().attribute(j).isNominal() || getInputFormat()
            .attribute(j).isNumeric())) {
          vals[j] = m_meansAndModes[j];
        } else {
          vals[j] = instance.value(j);
        }
      }
      inst = new DenseInstance(instance.weight(), vals);
    }
    inst.setDataset(instance.dataset());

    return inst;
  }

  @Override
  public String globalInfo() {
    return "A missing values replacement filter that is constructed from summary meta data";
  }

  @Override
  public void resetPreconstructed() {
    throw new UnsupportedOperationException("This filter can't be reset!");
  }
}
