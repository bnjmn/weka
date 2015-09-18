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
 *    WekaScoringMapTask.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import weka.classifiers.Classifier;
import weka.clusterers.Clusterer;
import weka.core.Attribute;
import weka.core.BatchPredictor;
import weka.core.DenseInstance;
import weka.core.Environment;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.core.Utils;
import distributed.core.DistributedJobConfig;

/**
 * Map task for scoring data using a model that has been previously learned.
 * Handles both classifiers and clusterers. Builds a mapping between incoming
 * fields and those that the model expects. Degrades gracefully (via missing
 * values) when model fields are missing from the incoming data or there are
 * type mismatches.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class WekaScoringMapTask implements Serializable {

  /** For serialization */
  private static final long serialVersionUID = 146378352037860956L;

  /** Model to use */
  protected ScoringModel m_model;

  /** For storing instances for batch prediction */
  protected Instances m_batchScoringData;

  /** Default batch size */
  protected int m_batchSize = 1000;

  /** True if the model header contains string attributes */
  protected boolean m_isUsingStringAttributes;

  /**
   * Holds the names of any model attributes that are missing or have a type
   * mismatch compared to the incoming instances structure.
   */
  protected Map<String, String> m_missingMismatch =
    new HashMap<String, String>();

  /**
   * Indexes of model attributes in the incoming instances structure. Missing or
   * type mismatches are indicated by an -1 index
   */
  protected int[] m_attributeMap;

  /** True if the incoming and model headers match */
  protected boolean m_equalHeaders;

  /**
   * Base class for wrapping classifiers or clusterers
   *
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  protected abstract static class ScoringModel implements Serializable {

    /** For serialization */
    private static final long serialVersionUID = 6418927792442398048L;

    /** The header of the training data used to train the model */
    protected Instances m_modelHeader;

    /** Holds the labels that the model will predict */
    protected List<String> m_predictionLabels;

    /**
     * Constructor for a new ScoringModel
     *
     * @param model the model to wrap
     */
    public ScoringModel(Object model) {
      setModel(model);
    }

    /**
     * Return true if the underlying model is a BatchPredictor
     *
     * @return return true if the underlying model is a BatchPredictor
     */
    public boolean isBatchPredicor() {
      if (!(getModel() instanceof BatchPredictor)) {
        return false;
      }
      return (((BatchPredictor) getModel())
        .implementsMoreEfficientBatchPrediction());
    }

    /**
     * Returns predictions in the case where the base model is a BatchPredictor
     *
     * @param insts the instances to provide predictions for
     * @return the predictions
     * @throws Exception if a problem occurs
     */
    public double[][] distributionsForInstances(Instances insts)
      throws Exception {

      return ((BatchPredictor) getModel()).distributionsForInstances(insts);
    }

    /**
     * Set the training header used to train the model
     *
     * @param header the header of the data used to train the model
     */
    public void setHeader(Instances header) {
      m_modelHeader = header;
    }

    /**
     * Get the header of the data used to train the model
     *
     * @return the header of the data used to train the model
     */
    public Instances getHeader() {
      return m_modelHeader;
    }

    /**
     * Set the model to wrap
     *
     * @param model the model to wrap
     */
    public abstract void setModel(Object model);

    /**
     * Get the wrapped model
     *
     * @return the wrapped model
     */
    public abstract Object getModel();

    /**
     * Get a list of labels that this ScoringModel can produce as predictions
     *
     * @return a list of labels that the model can produce as predictions
     * @throws DistributedWekaException if a problem occurs
     */
    public abstract List<String> getPredictionLabels()
      throws DistributedWekaException;

    /**
     * Return a probability distribution over the predicted labels
     *
     * @param inst the instance to predict/score
     * @return a probability distribution over the possible labels
     * @throws Exception if a problem occurs
     */
    public abstract double[] distributionForInstance(Instance inst)
      throws Exception;

    /**
     * Static factory method to create an appropriate concrete ScoringModel
     * given a particular base model
     *
     * @param model the model to wrap in a ScoringModel
     * @return a concrete subclass of ScoringModel
     * @throws Exception if a problem occurs
     */
    public static ScoringModel createScorer(Object model) throws Exception {
      if (model instanceof Classifier) {
        return new ClassifierScoringModel(model);
      } else if (model instanceof Clusterer) {
        return new ClustererScoringModel(model);
      }
      return null;
    }
  }

  /**
   * Subclass of ScoringModel for classifiers
   *
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  protected static class ClassifierScoringModel extends ScoringModel {

    /** For serialization */
    private static final long serialVersionUID = -5823090343185762045L;

    /** The wrapped classifier */
    protected Classifier m_model;

    /**
     * Construct a new ClassifierScoringModel
     *
     * @param model the model to wrap
     */
    public ClassifierScoringModel(Object model) {
      super(model);
    }

    @Override
    public void setModel(Object model) {
      m_model = (Classifier) model;
    }

    @Override
    public Object getModel() {
      return m_model;
    }

    @Override
    public List<String> getPredictionLabels() {

      if (m_modelHeader == null) {
        return null;
      }

      if (m_modelHeader.classAttribute().isNominal()) {
        if (m_predictionLabels == null) {
          m_predictionLabels = new ArrayList<String>();

          for (int i = 0; i < m_modelHeader.classAttribute().numValues(); i++) {
            m_predictionLabels.add(m_modelHeader.classAttribute().value(i));
          }
        }
      }

      return m_predictionLabels;
    }

    @Override
    public double[] distributionForInstance(Instance inst) throws Exception {
      return m_model.distributionForInstance(inst);
    }
  }

  /**
   * Subclass of ScoringModel for clusterers
   *
   * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
   */
  protected static class ClustererScoringModel extends ScoringModel {

    /** For serialization */
    private static final long serialVersionUID = 6415571646466462751L;

    /** The wrapped clusterer */
    protected Clusterer m_model;

    /**
     * Construct a new ClustererScoringModel
     *
     * @param model the clusterer to wrap
     */
    public ClustererScoringModel(Object model) {
      super(model);
    }

    @Override
    public void setModel(Object model) {
      m_model = (Clusterer) model;

    }

    @Override
    public Object getModel() {
      return m_model;
    }

    @Override
    public List<String> getPredictionLabels() throws DistributedWekaException {

      if (m_predictionLabels == null) {
        m_predictionLabels = new ArrayList<String>();

        try {
          for (int i = 0; i < m_model.numberOfClusters(); i++) {
            m_predictionLabels.add("Cluster_" + i);
          }
        } catch (Exception ex) {
          throw new DistributedWekaException(ex);
        }
      }

      return m_predictionLabels;
    }

    @Override
    public double[] distributionForInstance(Instance inst) throws Exception {
      return m_model.distributionForInstance(inst);
    }

    /**
     * Get the number of clusters from the base clusterer
     *
     * @return the number of clusters
     * @throws Exception if a problem occurs
     */
    public int numberOfClusters() throws Exception {
      return m_model.numberOfClusters();
    }
  }

  /**
   * Builds a mapping between the header for the incoming data to be scored and
   * the header used to train the model. Uses attribute names to match between
   * the two. Also constructs a list of missing attributes and a list of type
   * mismatches.
   *
   * @param modelHeader the header of the data used to train the model
   * @param incomingHeader the header of the incoming data
   * @throws DistributedWekaException if more than 50% of the attributes
   *           expected by the model are missing or have a type mismatch with
   *           the incoming data
   */
  protected void buildAttributeMap(Instances modelHeader,
    Instances incomingHeader) throws DistributedWekaException {
    m_attributeMap = new int[modelHeader.numAttributes()];

    int problemCount = 0;
    for (int i = 0; i < modelHeader.numAttributes(); i++) {
      Attribute modAtt = modelHeader.attribute(i);
      Attribute incomingAtt = incomingHeader.attribute(modAtt.name());

      if (incomingAtt == null) {
        // missing model attribute
        m_attributeMap[i] = -1;
        m_missingMismatch.put(modAtt.name(), "missing from incoming data");
        problemCount++;
      } else if (modAtt.type() != incomingAtt.type()) {
        // type mismatch
        m_attributeMap[i] = -1;
        m_missingMismatch.put(modAtt.name(),
          "type mismatch - " + "model: " + Attribute.typeToString(modAtt)
            + " != incoming: " + Attribute.typeToString(incomingAtt));
        problemCount++;
      } else {
        m_attributeMap[i] = incomingAtt.index();
      }
    }

    // -1 for the class (if set)
    int adjustForClass = modelHeader.classIndex() >= 0 ? 1 : 0;
    if (problemCount > (modelHeader.numAttributes() - adjustForClass) / 2) {
      throw new DistributedWekaException(
        "More than 50% of the attributes that the model "
          + "is expecting to see are either missing or have a type mismatch in the "
          + "incoming data.");
    }
  }

  /**
   * Set the model to use
   *
   * @param model the model to use
   * @param modelHeader the header of the training data used to train the model
   * @param dataHeader the header of the incoming data
   * @throws DistributedWekaException if more than 50% of the attributes
   *           expected by the model are missing or have a type mismatch with
   *           the incoming data
   */
  public void
    setModel(Object model, Instances modelHeader, Instances dataHeader)
      throws DistributedWekaException {

    m_missingMismatch.clear();

    if (dataHeader == null || modelHeader == null) {
      throw new DistributedWekaException(
        "Can't continue without a header for the model and incoming data");
    }
    try {
      m_isUsingStringAttributes = modelHeader.checkForStringAttributes();
      m_model = ScoringModel.createScorer(model);

      if (modelHeader != null) {
        m_model.setHeader(modelHeader);
      }

      if (m_model.isBatchPredicor()) {
        m_batchScoringData = new Instances(modelHeader, 0);
        Environment env = Environment.getSystemWide();
        String batchSize = ((BatchPredictor) model).getBatchSize();
        if (!DistributedJobConfig.isEmpty(batchSize)) {
          m_batchSize = Integer.parseInt(env.substitute(batchSize));
        } else {
          m_batchSize = 1000;
        }
      }
    } catch (Exception ex) {
      throw new DistributedWekaException(ex);
    }

    buildAttributeMap(modelHeader, dataHeader);
  }

  /**
   * Update the underlying model for this scoring task
   * 
   * @param model the new model to use
   * @throws DistributedWekaException if the task has not yet been initialized
   *           by calling setModel().
   */
  public void updateModel(Object model) throws DistributedWekaException {
    if (m_model == null) {
      throw new DistributedWekaException("Must set a model before it can be "
        + "updated!");
    }
    m_model.setModel(model);
  }

  /**
   * Constructs an instance suitable for passing to the model for scoring
   *
   * @param incoming the incoming instance
   * @return an instance with values mapped to be consistent with what the model
   *         is expecting
   */
  protected Instance mapIncomingFieldsToModelFields(Instance incoming) {
    Instances modelHeader = m_model.getHeader();
    double[] vals = new double[modelHeader.numAttributes()];

    for (int i = 0; i < modelHeader.numAttributes(); i++) {

      if (m_attributeMap[i] < 0) {
        // missing or type mismatch
        vals[i] = Utils.missingValue();
        continue;
      }

      Attribute modelAtt = modelHeader.attribute(i);
      Attribute incomingAtt = incoming.dataset().attribute(m_attributeMap[i]);

      if (incoming.isMissing(incomingAtt.index())) {
        vals[i] = Utils.missingValue();
        continue;
      }

      if (modelAtt.isNumeric()) {
        vals[i] = incoming.value(m_attributeMap[i]);
      } else if (modelAtt.isNominal()) {
        String incomingVal = incoming.stringValue(m_attributeMap[i]);
        int modelIndex = modelAtt.indexOfValue(incomingVal);

        if (modelIndex < 0) {
          vals[i] = Utils.missingValue();
        } else {
          vals[i] = modelIndex;
        }
      } else if (modelAtt.isString()) {
        vals[i] = 0;
        modelAtt.setStringValue(incoming.stringValue(m_attributeMap[i]));
      }
    }

    if (modelHeader.classIndex() >= 0) {
      // set class to missing value
      vals[modelHeader.classIndex()] = Utils.missingValue();
    }

    Instance newInst = null;
    if (incoming instanceof SparseInstance) {
      newInst = new SparseInstance(incoming.weight(), vals);
    } else {
      newInst = new DenseInstance(incoming.weight(), vals);
    }

    newInst.setDataset(modelHeader);
    return newInst;
  }

  /**
   * Process (score) an instance
   *
   * @param inst the instance to score
   * @return a probability distribution over the labels that the model can
   *         predict
   * @throws DistributedWekaException if a problem occurs
   */
  public double[] processInstance(Instance inst)
    throws DistributedWekaException {

    inst = mapIncomingFieldsToModelFields(inst);
    double[] preds = null;
    try {
      preds = m_model.distributionForInstance(inst);
    } catch (Exception e) {
      throw new DistributedWekaException(e);
    }

    return preds;
  }

  /**
   * Process an instance. When the batch size is met then a batch of predictions
   * is returned; returns null if the batch size has not been matched yet
   *
   * @param inst the instance to process
   * @return a batch of predictions or null if we have not seen enough input
   *         instances to meet the batch size yet
   * @throws DistributedWekaException if a problem occurs
   */
  public double[][] processInstanceBatchPredictor(Instance inst)
    throws DistributedWekaException {

    inst = mapIncomingFieldsToModelFields(inst);
    m_batchScoringData.add(inst);

    if (m_batchScoringData.numInstances() == m_batchSize) {
      try {
        double[][] predictions =
          m_model.distributionsForInstances(m_batchScoringData);
        if (predictions.length != m_batchScoringData.numInstances()) {
          throw new Exception("Number of predictions did not match the number "
            + "of instances in the batch");
        }

        m_batchScoringData.delete();

        return predictions;
      } catch (Exception ex) {
        throw new DistributedWekaException(ex);
      }
    }

    return null;
  }

  /**
   * Finish off the last partial batch (if any).
   *
   * @return predictions for the last partial batch or null
   * @throws DistributedWekaException if a problem occurs
   */
  public double[][] finalizeBatchPrediction() throws DistributedWekaException {

    if (m_batchScoringData != null && m_batchScoringData.numInstances() > 0) {
      // finish of the last partial batch

      try {
        double[][] predictions =
          m_model.distributionsForInstances(m_batchScoringData);

        m_batchScoringData = null;
        return predictions;
      } catch (Exception ex) {
        throw new DistributedWekaException(ex);
      }
    }

    return null;
  }

  /**
   * Get a string summarizing missing and type mismatches between the incoming
   * data and what the model expects
   *
   * @return a summary string of problems
   */
  public String getMissingMismatchAttributeInfo() {
    StringBuilder b = new StringBuilder();

    for (Map.Entry<String, String> e : m_missingMismatch.entrySet()) {
      b.append(e.getKey()).append(" ").append(e.getValue()).append("\n");
    }

    return b.toString();
  }

  /**
   * Get a list of labels that the model can predict
   *
   * @return a list of labels that the model can predict
   * @throws DistributedWekaException if a problem occurs
   */
  public List<String> getPredictionLabels() throws DistributedWekaException {
    return m_model.getPredictionLabels();
  }

  /**
   * Returns true if the underlying model is a BatchPredictor
   *
   * @return true if the underlying model is a BatchPredictor
   */
  public boolean isBatchPredictor() {
    return m_model == null ? false : m_model.isBatchPredicor();
  }

  /**
   * Returns true if model is using string attributes
   *
   * @return true if model is using string attributes
   */
  public boolean modelIsUsingStringAttributes() {
    return m_isUsingStringAttributes;
  }

  /**
   * Returns true if the underlying model is a classifier
   *
   * @return true if the underlying model is a classifier
   */
  public boolean modelIsAClassifier() {
    return m_model.getModel() instanceof weka.classifiers.Classifier;
  }
}
