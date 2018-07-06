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
 *    MLlibDatasetMaker
 *    Copyright (C) 2017 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.spark.mllib.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.mllib.regression.LabeledPoint;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.core.Utils;
import weka.core.stats.ArffSummaryNumericMetric;
import weka.core.stats.NominalStats;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.DistributedWekaException;
import weka.distributed.spark.CachingStrategy;
import weka.filters.Filter;
import weka.filters.MakePreconstructedFilter;
import weka.filters.MultiFilter;
import weka.filters.PreconstructedFilter;
import weka.filters.StreamableFilter;
import weka.filters.unsupervised.attribute.NominalToBinary;
import weka.filters.unsupervised.attribute.PreconstructedMissingValuesReplacer;

/**
 * Helper class for converting RDDs of Weka Instance objects into RDDs of
 * LabeledPoint, for use by MLlib learning schemes.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 */
public class MLlibDatasetMaker implements Serializable {

  public static final int DEFAULT_MAX_NOMINAL_LABELS = 4;
  private static final long serialVersionUID = 2361799396306995802L;

  /** Filters to apply to the data */
  protected Filter m_filtersInUse;

  /** True if the first batch has been processed */
  protected boolean m_firstBatchDone;

  protected Instances m_headerNoSummary;

  protected Instances m_transformedHeader;

  protected int m_classIndex = -1;

  protected Map<Integer, Integer> m_categoricalFeaturesMap =
    new HashMap<Integer, Integer>();

  /**
   * Convert a {@code RDD<Instance>} dataset into a {@code RDD<LabeledPoint>}
   * one suitable for use with MLlib learners.
   * 
   * @param headerWithSummary header of the incoming Instance RDD with summary
   *          attributes
   * @param wekaDataset the {@code RDD<Instance>}
   * @param replaceMissingValues true if missing values should be replaced with
   *          mean/mode
   * @param preconstructedFilters any preconstructed filter(s) to use. These are
   *          filtering model(s) that have been learned during a spark job and
   *          then encapsulated as a {@code PreconstructedFilter} (e.g. the PCA
   *          filter learned by the CorrelationMatrixSparkJob).
   * @param preprocessors any streaming filters to apply on-the-fly as the data
   *          is transformed from {@code Instance}s to {@code LabeledPoint}s.
   *          Can be null if there are no filters to apply.
   * @param maxNominalLabels the maximum number of nominal labels/categories
   *          above which a nominal attribute is to be treated as numeric.
   *          Nominal attributes with fewer than this number of values will be
   *          binarized via NominalToBinary (unless
   *          generateCategoricalFeaturesMap is set to true)
   * @param generateCategoricalFeaturesMap if true then an index,arity map will
   *          be generated for those nominal attributes that meet the
   *          maxNominalLabels criterion; if false, then Weka's unsupervised
   *          NominalToBinary filter will be used to make numeric indicator
   *          attributes for nominals that meet the maxNominalLabels criterion
   * @param strategy caching strategy to use for the new
   *          {@code RDD<LabeledPoint>} dataset
   * @return a {@code RDD<LabeledPoint>} dataset
   * @throws DistributedWekaException if a problem occurs
   */
  public JavaRDD<LabeledPoint> labeledPointRDDFirstBatch(
    Instances headerWithSummary, JavaRDD<Instance> wekaDataset,
    boolean replaceMissingValues, PreconstructedFilter preconstructedFilters,
    List<StreamableFilter> preprocessors, int maxNominalLabels,
    boolean generateCategoricalFeaturesMap, CachingStrategy strategy)
    throws DistributedWekaException {

    if (maxNominalLabels <= 0) {
      maxNominalLabels = DEFAULT_MAX_NOMINAL_LABELS;
    }

    Instances headerNoSummary =
      CSVToARFFHeaderReduceTask.stripSummaryAtts(headerWithSummary);

    // replace missing values filter
    PreconstructedMissingValuesReplacer missingValuesReplacer = null;
    if (replaceMissingValues) {
      // check for missing values first
      boolean hasMissing = false;
      for (int i = 0; i < headerNoSummary.numAttributes(); i++) {
        String name = headerNoSummary.attribute(i).name();
        Attribute summary =
          headerWithSummary
            .attribute(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
              + name);

        if (headerNoSummary.attribute(i).isNumeric()) {
          if (ArffSummaryNumericMetric.MISSING.valueFromAttribute(summary) > 0) {
            hasMissing = true;
            break;
          }
        } else {
          NominalStats nominalStats = NominalStats.attributeToStats(summary);
          if (nominalStats.getNumMissing() > 0) {
            hasMissing = true;
            break;
          }
        }
      }
      if (hasMissing) {
        try {
          missingValuesReplacer =
            new PreconstructedMissingValuesReplacer(headerWithSummary);
        } catch (Exception ex) {
          throw new DistributedWekaException(ex);
        }
      }
    }

    List<Filter> filtersToUse = null;
    if (preconstructedFilters != null || missingValuesReplacer != null) {
      filtersToUse = new ArrayList<>();
      if (missingValuesReplacer != null) {
        filtersToUse.add(missingValuesReplacer);
      }
      if (preconstructedFilters != null) {
        filtersToUse.add((Filter) preconstructedFilters);
      }
    }

    if (preprocessors != null && preprocessors.size() > 0) {
      if (filtersToUse == null) {
        filtersToUse = new ArrayList<>();
      }
      for (StreamableFilter f : preprocessors) {
        filtersToUse.add((Filter) f);
      }
    }

    m_classIndex = headerNoSummary.classIndex();
    // Instances headerAfterAnyFiltersExceptNominalToBinary = new
    // Instances(m_headerNoSummary, 0);
    Instances headerAfterAnyFiltersExceptNominalToBinary = headerNoSummary;
    // System.out.println(headerNoSummary);

    if (filtersToUse != null) {
      MultiFilter mf = new MultiFilter();
      mf.setFilters(filtersToUse.toArray(new Filter[filtersToUse.size()]));
      try {
        mf.setInputFormat(headerNoSummary);
        headerAfterAnyFiltersExceptNominalToBinary = mf.getOutputFormat();
      } catch (Exception e) {
        throw new DistributedWekaException(e);
      }
    }

    if (headerAfterAnyFiltersExceptNominalToBinary.checkForStringAttributes()) {
      throw new DistributedWekaException(
        "String attributes have to be preprocessed "
          + "before they can be used in MLlib algorithms");
    }

    // headerNoSummary.setClassIndex(classAtt.index());
    boolean nominalToBinary = false;
    if (headerAfterAnyFiltersExceptNominalToBinary
      .checkForAttributeType(Attribute.NOMINAL)) {
      for (int i = 0; i < headerAfterAnyFiltersExceptNominalToBinary
        .numAttributes(); i++) {
        if (i != m_classIndex
          && headerAfterAnyFiltersExceptNominalToBinary.attribute(i)
            .isNominal()) {
          if (headerAfterAnyFiltersExceptNominalToBinary.attribute(i)
            .numValues() <= maxNominalLabels) {
            nominalToBinary = true;
            break;
          }
        }
      }
    }

    if (nominalToBinary && generateCategoricalFeaturesMap) {
      nominalToBinary = false;

      // nominal to binary is never applied when generating a categorical
      // feature map
      int index = 0;
      for (int i = 0; i < headerAfterAnyFiltersExceptNominalToBinary
        .numAttributes(); i++) {
        if (i != headerAfterAnyFiltersExceptNominalToBinary.classIndex()) {
          if (headerAfterAnyFiltersExceptNominalToBinary.attribute(i)
            .isNominal()) {
            if (headerAfterAnyFiltersExceptNominalToBinary.attribute(i)
              .numValues() <= maxNominalLabels) {
              m_categoricalFeaturesMap.put(index,
                headerAfterAnyFiltersExceptNominalToBinary.attribute(i)
                  .numValues());
            }
          }
          index++;
        }
      }
    }

    // System.err.println("****************** categorical map is of size: "
    // + m_categoricalFeaturesMap.size());

    try {
      if (nominalToBinary) {

        NominalToBinary nomToBin = new NominalToBinary();
        MakePreconstructedFilter p = new MakePreconstructedFilter(nomToBin);
        if (filtersToUse != null) {
          filtersToUse.add(p);
        } else {
          m_filtersInUse = p;
        }
      }

      if (filtersToUse != null) {
        MultiFilter mf = new MultiFilter();
        mf.setFilters(filtersToUse.toArray(new Filter[filtersToUse.size()]));
        m_filtersInUse = new MakePreconstructedFilter(mf);
      }

      if (m_filtersInUse != null) {
        m_filtersInUse.setInputFormat(headerNoSummary);
      }
    } catch (Exception ex) {
      throw new DistributedWekaException(ex);
    }

    JavaRDD<LabeledPoint> mllibData =
      wekaDataset.mapPartitions(new InstanceToLabeledPointFlatMapFunction(
        headerNoSummary, m_filtersInUse));
    if (strategy != null) {
      mllibData.persist(strategy.getStorageLevel());
    }

    m_firstBatchDone = true;
    m_headerNoSummary = headerNoSummary;

    try {
      m_transformedHeader =
        m_filtersInUse != null ? Filter.useFilter(headerNoSummary,
          m_filtersInUse) : m_headerNoSummary;
    } catch (Exception e) {
      throw new DistributedWekaException(e);
    }

    return mllibData;
  }

  public Instances getTransformedHeader() {
    return m_transformedHeader;
  }

  public Filter getFiltersInUse() {
    return m_filtersInUse;
  }

  /**
   * Returns the class index of the original (untransformed) data
   *
   * @return the original class index
   */
  public int getClassIndex() {
    return m_classIndex;
  }

  public Map<Integer, Integer> getCategoricalFeaturesMap() {
    return m_categoricalFeaturesMap;
  }

  /**
   * Create an RDD[LabeledPoint] from an RDD[Instance] as a "next batch" - i.e.
   * using information determined from such a conversion on an initial batch.
   * 
   * WARNING: this method is not implemented yet, and I'm not sure whether it is
   * actually necessary :-)
   * 
   * @param wekaBatch RDD[Instance] batch
   * @param strategy caching strategy to use
   * @return RDD[LabeledPoint]
   * @throws DistributedWekaException if a problem occurs
   */
  public JavaRDD<LabeledPoint> labeledPointRDDNextBatch(
    JavaRDD<Instance> wekaBatch, CachingStrategy strategy)
    throws DistributedWekaException {

    if (!m_firstBatchDone) {
      throw new DistributedWekaException(
        "First batch has not been processed yet!");
    }

    // TODO

    return null;
  }

  /**
   * Vectorize a Weka Instance
   * 
   * @param toProcess the instance to vectorize
   * @return a Vector instance
   * @throws Exception if a problem occurs
   */
  public Vector vectorizeInstance(Instance toProcess) throws Exception {
    if (m_filtersInUse != null) {
      m_filtersInUse.input(toProcess);
      toProcess = m_filtersInUse.output();
    }

    return instanceToVector(toProcess, toProcess.classIndex());
  }

  /**
   * Static method for converting an Instance to a Vector
   *
   * @param toProcess the Instance to process
   * @param classIndex the class index (-1 means no class set)
   * @return a Vector
   */
  protected static Vector instanceToVector(Instance toProcess, int classIndex) {
    if (toProcess instanceof SparseInstance) {
      int classModifier = classIndex >= 0 ? 1 : 0;
      if (classModifier > 0) {
        double classValue = toProcess.classValue();
        if (classValue == 0) {
          classModifier = 0; // class is sparse
        }
      }
      SparseInstance toProcessSparse = ((SparseInstance) toProcess);
      int[] indices = new int[toProcessSparse.numValues() - classModifier];
      double[] values = new double[toProcessSparse.numValues() - classModifier];
      int index = 0;
      for (int i = 0; i < toProcessSparse.numValues(); i++) {
        if (toProcessSparse.index(i) != classIndex) {
          indices[index] = toProcessSparse.index(i);
          values[index++] = toProcessSparse.valueSparse(i);
        }
      }
      return Vectors.sparse(
        toProcess.numAttributes() - (classIndex >= 0 ? 1 : 0), indices, values);
    } else {
      if (classIndex < 0) {
        return Vectors.dense(toProcess.toDoubleArray());
      }
      double[] independent = new double[toProcess.numAttributes() - 1];
      int index = 0;
      for (int i = 0; i < toProcess.numAttributes(); i++) {
        if (i != classIndex) {
          independent[index++] = toProcess.value(i);
        }
      }
      return Vectors.dense(independent);
    }
  }

  /**
   * Flat map function for converting Instances into LabeledPoints
   */
  public static class InstanceToLabeledPointFlatMapFunction implements
    FlatMapFunction<Iterator<Instance>, LabeledPoint> {

    private static final long serialVersionUID = 5671667587016948792L;

    protected InstanceToLabeledPointIterable m_instanceToLabel;
    protected Instances m_header;
    protected Filter m_filtersInUse;

    public InstanceToLabeledPointFlatMapFunction(Instances header,
      Filter filtersToUse) {
      m_header = header;
      m_filtersInUse = filtersToUse;
    }

    @Override
    public Iterable<LabeledPoint> call(Iterator<Instance> split)
      throws Exception {

      if (m_instanceToLabel == null) {
        m_instanceToLabel =
          new InstanceToLabeledPointIterable(m_header, m_filtersInUse, split);
      }

      return m_instanceToLabel;
    }

    /**
     * An iterable for converting Instances to LabeledPoints
     */
    protected static class InstanceToLabeledPointIterable implements
      Iterable<LabeledPoint>, Iterator<LabeledPoint> {

      protected Instances m_header;
      protected Filter m_filtersInUse;
      protected Iterator<Instance> m_instanceIterator;

      public InstanceToLabeledPointIterable(Instances header,
        Filter filtersToUse, Iterator<Instance> instanceIterator) {
        m_header = header;
        m_filtersInUse = filtersToUse;
        m_instanceIterator = instanceIterator;

        if (m_filtersInUse != null) {
          m_header = m_filtersInUse.getOutputFormat();
        }
      }

      @Override
      public Iterator<LabeledPoint> iterator() {
        return this;
      }

      @Override
      public boolean hasNext() {
        return m_instanceIterator.hasNext();
      }

      @Override
      public LabeledPoint next() {
        Instance toProcess = m_instanceIterator.next();
        while (toProcess.classIsMissing()) {
          if (m_instanceIterator.hasNext()) {
            toProcess = m_instanceIterator.next();
          } else {
            throw new IllegalStateException(
              "Unable to produce next LabeledPoint "
                + "because there are no more instances that don't have missing "
                + "class values");
          }
        }
        if (m_filtersInUse != null) {
          try {
            m_filtersInUse.input(toProcess);
            toProcess = m_filtersInUse.output();
          } catch (Exception ex) {
            throw new IllegalStateException(ex);
          }
        }

        double classValue = toProcess.value(m_header.classIndex());
        // if (Utils.isMissingValue(classValue)) {
        // throw new IllegalStateException("Class value is missing!");
        // }
        return new LabeledPoint(classValue, instanceToVector(toProcess,
          m_header.classIndex()));
      }

      @Override
      public void remove() {

      }
    }
  }
}
