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
 *    DL4JHomogenousMultiLayerClassifier.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.deeplearning4j.models.classifiers.lstm.LSTM;
import org.deeplearning4j.models.featuredetectors.autoencoder.AutoEncoder;
import org.deeplearning4j.models.featuredetectors.autoencoder.recursive.RecursiveAutoEncoder;
import org.deeplearning4j.models.featuredetectors.rbm.RBM;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.layers.OutputLayer;
import org.deeplearning4j.nn.layers.convolution.ConvolutionDownSampleLayer;
import org.deeplearning4j.nn.layers.factory.*;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.api.activation.*;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.functions.dl4j.Utils;
import weka.classifiers.rules.ZeroR;
import weka.core.*;
import weka.filters.Filter;
import weka.filters.supervised.attribute.NominalToBinary;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import weka.filters.unsupervised.attribute.Standardize;
import weka.gui.ProgrammaticProperty;

/**
 * Wraps a DL4J Homogenous multilayer network. All layers of the network are of
 * the same type (e.g. RBM). NOTE: this is a (potentially fragile) experimental
 * classifier. DL4J is very new and its API is changing daily.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class DL4JHomogenousMultiLayerClassifier extends AbstractClassifier {

  public static final Tag[] TAGS_SELECTION_LAYER =
    {
      new Tag(LayerType.RBM.ordinal(), "RBM"),
      new Tag(LayerType.AUTOENCODER.ordinal(), "Auto encoder"),
      new Tag(LayerType.RECURSIVEAUTOENCODER.ordinal(),
        "Recursive auto encoder") };

  public static final Tag[] TAGS_SELECTION_OPTIMIZATION = {
    new Tag(OptimizationAlgorithm.CONJUGATE_GRADIENT.ordinal(),
      "Conjugate gradient"),
    new Tag(OptimizationAlgorithm.GRADIENT_DESCENT.ordinal(),
      "Gradient descent"),
    new Tag(OptimizationAlgorithm.HESSIAN_FREE.ordinal(), "Hessian free"),
    new Tag(OptimizationAlgorithm.ITERATION_GRADIENT_DESCENT.ordinal(),
      "Iteration gradient descent"),
    new Tag(OptimizationAlgorithm.LBFGS.ordinal(), "LBFGS") };

  public static final Tag[] TAGS_ACTIVATION = {
    new Tag(ActivationFunction.SIGMOID.ordinal(), "Sigmoid"),
    new Tag(ActivationFunction.LINEAR.ordinal(), "Linear"),
    new Tag(ActivationFunction.TANH.ordinal(), "Tanh"),
    new Tag(ActivationFunction.EXP.ordinal(), "Exp"),
    new Tag(ActivationFunction.SOFTMAX.ordinal(), "Softmax"),
    new Tag(ActivationFunction.HARDTANH.ordinal(), "HardTanh"),
    new Tag(ActivationFunction.RECTIFIEDLINEAR.ordinal(), "RectifiedLinear"),
    new Tag(ActivationFunction.ROUNDLINEAR.ordinal(), "RoundLinear"),
    new Tag(ActivationFunction.MAXOUT.ordinal(), "Maxout") };

  public static final Tag[] TAGS_LOSSFUNCTION = {
    new Tag(LossFunctions.LossFunction.MSE.ordinal(), "MSE"),
    new Tag(LossFunctions.LossFunction.EXPLL.ordinal(), "Exp log likelihood"),
    new Tag(LossFunctions.LossFunction.XENT.ordinal(), "Cross entropy"),
    new Tag(LossFunctions.LossFunction.MCXENT.ordinal(),
      "Multi-class cross entroy"),
    new Tag(LossFunctions.LossFunction.RMSE_XENT.ordinal(),
      "RMSE cross entropy"),
    new Tag(LossFunctions.LossFunction.SQUARED_LOSS.ordinal(), "Squared loss"),
    new Tag(LossFunctions.LossFunction.RECONSTRUCTION_CROSSENTROPY.ordinal(),
      "Reconstruction cross entropy"),
    new Tag(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD.ordinal(),
      "Negative log likelihood"), };

  public static final Tag[] TAGS_RBM_VISIBLE = {
    new Tag(RBM.VisibleUnit.BINARY.ordinal(), "Binary"),
    new Tag(RBM.VisibleUnit.GAUSSIAN.ordinal(), "Gaussian"),
    new Tag(RBM.VisibleUnit.LINEAR.ordinal(), "Linear"),
    new Tag(RBM.VisibleUnit.SOFTMAX.ordinal(), "Softmax") };

  public static final Tag[] TAGS_RBM_HIDDEN = {
    new Tag(RBM.HiddenUnit.BINARY.ordinal(), "Binary"),
    new Tag(RBM.HiddenUnit.GAUSSIAN.ordinal(), "Gaussian"),
    new Tag(RBM.HiddenUnit.RECTIFIED.ordinal(), "Rectified"),
    new Tag(RBM.HiddenUnit.SOFTMAX.ordinal(), "Softmax") };

  public static final Tag[] TAGS_WEIGHT_INIT = {
    new Tag(WeightInit.VI.ordinal(), "Variance normalized (VI)"),
    new Tag(WeightInit.ZERO.ordinal(), "Zero"),
    new Tag(WeightInit.SIZE.ordinal(), "Size"),
    new Tag(WeightInit.DISTRIBUTION.ordinal(), "Normalized"),
    new Tag(WeightInit.UNIFORM.ordinal(), "Uniform") };


  private static final long serialVersionUID = -9010659606478329862L;

  protected ReplaceMissingValues m_replaceMissing;
  protected Filter m_normalize;
  protected boolean m_standardizeInsteadOfNormalize;
  protected NominalToBinary m_nominalToBinary;
  protected NeuralNetConfiguration m_layerConfig;
  protected int m_iterations = 100;
  protected double m_learningRate = 1e-1;
  protected double m_momentum = 0.5;
  protected double m_dropOut = 0.0;
  protected int m_k = 1;
  protected boolean m_useRegularization;
  protected double m_l2 = 0;
  protected LayerType m_layerType = LayerType.RBM;
  protected OptimizationAlgorithm m_optimizationStrategy =
    OptimizationAlgorithm.CONJUGATE_GRADIENT;
  protected ActivationFunction m_activationFunction =
    ActivationFunction.SIGMOID;
  protected boolean m_constrainGradientToUnitNorm;
  protected LossFunctions.LossFunction m_lossFunction =
    LossFunctions.LossFunction.RECONSTRUCTION_CROSSENTROPY;
  protected RBM.VisibleUnit m_rbmVisibleUnit = RBM.VisibleUnit.BINARY;
  protected RBM.HiddenUnit m_rbmHiddenUnit = RBM.HiddenUnit.BINARY;
  protected WeightInit m_weightInit = WeightInit.VI;

  protected String m_hiddenLayerSizes = "2";
  protected boolean m_useDropConnect;
  protected boolean m_dontPretrain;
  protected boolean m_useGaussNewtonVectorProductBackProp;
  protected boolean m_dontUseRBMPropUpAsActivations;
  protected double m_dampingFactor = 100;

  protected MultiLayerNetwork m_network;

  protected ZeroR m_zeroR = new ZeroR();

  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String[] argv) {
    runClassifier(new DL4JHomogenousMultiLayerClassifier(), argv);
  }

  /**
   * Gets the layer configuration used in this classifier
   *
   * @return the layer configuration used in this classifier
   */
  @ProgrammaticProperty
  public NeuralNetConfiguration getLayerConfig() {
    return m_layerConfig;
  }

  /**
   * Sets the layer configuration used in this classifier
   *
   * @param layerConfig the configuration used in this classifier
   */
  public void setLayerConfig(NeuralNetConfiguration layerConfig) {
    m_layerConfig = layerConfig;
  }

  /**
   * Get whether to standardize rather than normalize the data
   *
   * @return true if standardization is to be used instead of normalization
   */
  @OptionMetadata(displayName = "Standardize", description = "Standardize the "
    + "data instead of normalize the data",
    commandLineParamName = "standardize", commandLineParamIsFlag = true,
    commandLineParamSynopsis = "-standardize", displayOrder = 0)
  public boolean getStandardize() {
    return m_standardizeInsteadOfNormalize;
  }

  /**
   * Set whether to standardize rather than normalize the data
   *
   * @param s true if standardization is to be used instead of normalization
   */
  public void setStandardize(boolean s) {
    m_standardizeInsteadOfNormalize = s;
  }

  /**
   * Get the number of iterations to perform
   *
   * @return the number of iterations to perform
   */
  @OptionMetadata(displayName = "Number of iterations",
    description = "The number of iterations to perform (default = 100)",
    commandLineParamName = "iterations",
    commandLineParamSynopsis = "-iterations <integer>", displayOrder = 1)
  public int getIterations() {
    return m_iterations;
  }

  /**
   * Set the number of iterations to perform
   *
   * @param iter the number of iterations to perform
   */
  public void setIterations(int iter) {
    m_iterations = iter;
  }

  /**
   * Get the learning rate to use
   *
   * @return the learning rate to use
   */
  @OptionMetadata(displayName = "Learning rate",
    description = "The learning rate to use (default = 1e-1)",
    commandLineParamName = "learning-rate",
    commandLineParamSynopsis = "-learning-rate <double>", displayOrder = 2)
  public double getLearningRate() {
    return m_learningRate;
  }

  /**
   * Set the learning rate to use
   *
   * @param lr the learning rate to use
   */
  public void setLearningRate(double lr) {
    m_learningRate = lr;
  }

  /**
   * Get the momentum to use
   *
   * @return the momentum to use
   */
  @OptionMetadata(displayName = "Momentum",
    description = "The momentum to use " + "(default = 0.5)",
    commandLineParamName = "momentum",
    commandLineParamSynopsis = "-momentum <double>", displayOrder = 3)
  public double getMomentum() {
    return m_momentum;
  }

  /**
   * Set the momentum to use
   *
   * @param m the momentum to use
   */
  public void setMomentum(double m) {
    m_momentum = m;
  }

  /**
   * Get the drop out to use
   *
   * @return the drop out to use
   */
  @OptionMetadata(displayName = "Drop out",
    description = "The drop out value to use (default = 0)",
    commandLineParamName = "drop-out",
    commandLineParamSynopsis = "-drop-out <double>", displayOrder = 4)
  public double getDropOut() {
    return m_dropOut;
  }

  /**
   * Set the drop out to use
   *
   * @param d the drop out to use
   */
  public void setDropOut(double d) {
    m_dropOut = d;
  }

  /**
   * Get the type of layer to use in the network
   *
   * @return the type of layer
   */
  @OptionMetadata(displayName = "Type of layer",
    description = "The type of network layer to use: 0 = RBM, 1 = Auto encoder"
      + ", 2 = Recursive auto encoder.\n(default = 0)",
    commandLineParamName = "layer",
    commandLineParamSynopsis = "-layer <integer>", displayOrder = 5)
  public SelectedTag getLayerType() {
    return new SelectedTag(m_layerType.ordinal(), TAGS_SELECTION_LAYER);
  }

  /**
   * Set the type of layer to use in the network
   *
   * @param layerType the type of layer
   */
  public void setLayerType(SelectedTag layerType) {
    int layerID = layerType.getSelectedTag().getID();
    for (LayerType t : LayerType.values()) {
      if (t.ordinal() == layerID) {
        m_layerType = t;
        break;
      }
    }
  }

  /**
   * Get the activation function to use
   *
   * @return the activation function to use
   */
  @OptionMetadata(displayName = "Activation function",
    description = "The activation function to use: 0 = sigmoid, 1 = linear, "
      + "2 = tanh, 3 = exp, 4 = softmax, 5 = hard tanh, 6 = rectified linear,"
      + "7 = round linear, 8 = maxout.\n(default = 0",
    commandLineParamName = "activation",
    commandLineParamSynopsis = "-activation <integer>", displayOrder = 6)
  public SelectedTag getActivationFunction() {
    return new SelectedTag(m_activationFunction.ordinal(), TAGS_ACTIVATION);
  }

  /**
   * Set the activation function to use
   *
   * @param activationFunction the activation function to use
   */
  public void setActivationFunction(SelectedTag activationFunction) {
    int actID = activationFunction.getSelectedTag().getID();
    for (ActivationFunction a : ActivationFunction.values()) {
      if (a.ordinal() == actID) {
        m_activationFunction = a;
        break;
      }
    }
  }

  /**
   * Get whether to constrain the gradient to unit norm
   *
   * @return true if the gradient is to be constrained to unit norm
   */
  @OptionMetadata(displayName = "Constrain gradient to unit norm",
    description = "Constrain gradient to unit norm",
    commandLineParamName = "constrain-gradient", commandLineParamIsFlag = true,
    commandLineParamSynopsis = "-constrain-gradient", displayOrder = 7)
  public boolean getConstrainGradientToUnitNorm() {
    return m_constrainGradientToUnitNorm;
  }

  /**
   * Set whether to constrain the gradient to unit norm
   *
   * @param constrain true if the gradient is to be constrained to unit norm
   */
  public void setConstrainGradientToUnitNorm(boolean constrain) {
    m_constrainGradientToUnitNorm = constrain;
  }

  /**
   * Get k (the number of iterations of contrastive divergence to run)
   *
   * @return the number of iterations of contrastive divergence to run
   */
  @OptionMetadata(displayName = "k",
    description = "The number of iterations of contrastive divergence to run "
      + "(default 1)", commandLineParamName = "K",
    commandLineParamSynopsis = "-K <integer>", displayOrder = 8)
  public int getK() {
    return m_k;
  }

  /**
   * Set k (the number of iterations of contrastive divergence to run)
   *
   * @param k the number of iterations of contrastive divergence to run
   */
  public void setK(int k) {
    m_k = k;
  }

  /**
   * Get whether to use regularization
   *
   * @return true if regularization is to be used
   */
  @OptionMetadata(displayName = "Use regularization",
    description = "Use regularization",
    commandLineParamName = "regularlization", commandLineParamIsFlag = true,
    commandLineParamSynopsis = "-regularization", displayOrder = 9)
  public boolean getUseRegularization() {
    return m_useRegularization;
  }

  /**
   * Set whether to use regularization
   *
   * @param u true if regularization is to be used
   */
  public void setUseRegularization(boolean u) {
    m_useRegularization = u;
  }

  /**
   * Get the l2 regularization constant
   *
   * @return the l2 regularization constant
   */
  @OptionMetadata(displayName = "l2",
    description = "Regularization constant (default = 0)",
    commandLineParamName = "l2", commandLineParamSynopsis = "-l2 <double>",
    displayOrder = 10)
  public double getL2() {
    return m_l2;
  }

  /**
   * Set the l2 regularization constant
   *
   * @param l2 the l2 regularization constant
   */
  public void setL2(double l2) {
    m_l2 = l2;
  }

  /**
   * Get the optimization strategy to use
   *
   * @return the optimization strategy to use
   */
  @OptionMetadata(
    displayName = "Optimization strategy",
    description = "The "
      + "optimization strategy to use: 0 = conjugate gradient, "
      + "1 = gradient descent, 2 = hessian free, 3 = iteration gradient descent, "
      + "4 = LBFGS.\n(default = 0)", commandLineParamName = "optimization",
    commandLineParamSynopsis = "-optimization <integer>", displayOrder = 11)
  public
    SelectedTag getOptimizationStrategy() {
    return new SelectedTag(m_optimizationStrategy.ordinal(),
      TAGS_SELECTION_OPTIMIZATION);
  }

  /**
   * Set the optimization strategy to use
   *
   * @param optimizationStrategy the optimization strategy to use
   */
  public void setOptimizationStrategy(SelectedTag optimizationStrategy) {
    int optID = optimizationStrategy.getSelectedTag().getID();
    for (OptimizationAlgorithm o : OptimizationAlgorithm.values()) {
      if (o.ordinal() == optID) {
        m_optimizationStrategy = o;
        break;
      }
    }
  }

  /**
   * Get the loss function to use
   *
   * @return the loss function to use
   */
  @OptionMetadata(
    displayName = "Loss function",
    description = "The loss function "
      + "to use: 0 = MSE, 1 = exp log likelihood, 2 = cross entropy, 3 = "
      + "multi-class cross "
      + "entropy, 4 = RMSE cross entropy, 5 = squared loss, 6 = reconstruction "
      + "cross entropy, 7 = negative log likelihood\n(default = 6",
    commandLineParamSynopsis = "-log-likelihood <integer>", displayOrder = 12)
  public
    SelectedTag getLossFunction() {
    return new SelectedTag(m_lossFunction.ordinal(), TAGS_LOSSFUNCTION);
  }

  /**
   * Set the loss function to use
   *
   * @param lossFunction the loss function to use
   */
  public void setLossFunction(SelectedTag lossFunction) {
    int lossID = lossFunction.getSelectedTag().getID();
    for (LossFunctions.LossFunction l : LossFunctions.LossFunction.values()) {
      if (l.ordinal() == lossID) {
        m_lossFunction = l;
        break;
      }
    }
  }

  /**
   * Get the RBMVisible unit type
   *
   * @return the type of the visible unit
   */
  @OptionMetadata(displayName = "RBM visible unit", description = "RBM visible"
    + " unit to use: 0 = binary, 1 = gaussian, 2 = linear, 3 = softmax.\n"
    + "(default = binary)", commandLineParamName = "rbm-visible",
    commandLineParamSynopsis = "-rmb-visible <integer>", displayOrder = 13)
  public SelectedTag getRBMVisibleUnit() {
    return new SelectedTag(m_rbmVisibleUnit.ordinal(), TAGS_RBM_VISIBLE);
  }

  /**
   * Set the RBMVisible unit type
   *
   * @param visible the type of the visible unit
   */
  public void setRBMVisibleUnit(SelectedTag visible) {
    int visID = visible.getSelectedTag().getID();
    for (RBM.VisibleUnit v : RBM.VisibleUnit.values()) {
      if (v.ordinal() == visID) {
        m_rbmVisibleUnit = v;
        break;
      }
    }
  }

  /**
   * Get the RBMHidden unit type
   *
   * @return the type of the hidden unit
   */
  @OptionMetadata(displayName = "RBM hidden unit", description = "RBM hidden"
    + " unit to use: 0 = binary, 1 = gaussian, 2 = rectified, 3 = softmax.\n"
    + "(default = binary)", commandLineParamName = "rbm-hidden",
    commandLineParamSynopsis = "-rmb-hidden <integer>", displayOrder = 14)
  public SelectedTag getRBMHiddenUnit() {
    return new SelectedTag(m_rbmHiddenUnit.ordinal(), TAGS_RBM_HIDDEN);
  }

  /**
   * Set the RBMHidden unit type
   *
   * @param hidden the type of the hidden unit
   */
  public void setRBMHiddenUnit(SelectedTag hidden) {
    int hidID = hidden.getSelectedTag().getID();
    for (RBM.HiddenUnit v : RBM.HiddenUnit.values()) {
      if (v.ordinal() == hidID) {
        m_rbmHiddenUnit = v;
        break;
      }
    }
  }

  // --------- MultiLayerConfiguration below this point -----------

  /**
   * Get the weight initialization strategy
   *
   * @return the weight initialization method to use
   */
  @OptionMetadata(displayName = "Weight init method",
    description = "The weight "
      + "initialization method to use: 0 = variance normalized, 1 = zero, "
      + "2 = size, 3 = distribution, 4 = normalized, 5 = uniform.\n"
      + "(default = 0)", commandLineParamName = "weight-init",
    commandLineParamSynopsis = "-weight-init <integer>", displayOrder = 15)
  public SelectedTag getWeightInit() {
    return new SelectedTag(m_weightInit.ordinal(), TAGS_WEIGHT_INIT);
  }

  /**
   * Set the weight initialization strategy
   *
   * @param weightInit the weight initialization method to use
   */
  public void setWeightInit(SelectedTag weightInit) {
    int wID = weightInit.getSelectedTag().getID();
    for (WeightInit w : WeightInit.values()) {
      if (w.ordinal() == wID) {
        m_weightInit = w;
        break;
      }
    }
  }

  /**
   * Get the sizes (number of nodes) in each hidden layer. This is a comma
   * separated list of sizes. The number of elements in this list determines the
   * number of hidden layers in the network.
   *
   * @return the sizes of the hidden layers
   */
  @OptionMetadata(
    displayName = "Sizes of hidden layers (comma separated list)",
    description = "Set the size of each hidden layer. Specified as a comma "
      + "separated list where the number of elements in the list\n"
      + "determines the number of hidden layers\n(default = 2)"
      + "hidden layers)", commandLineParamName = "layers",
    commandLineParamSynopsis = "-layers <int,...,int>)", displayOrder = 16)
  public String getHiddenLayerSizes() {
    return m_hiddenLayerSizes;
  }

  /**
   * Set the sizes (number of nodes) in each hidden layer. This is a comma
   * separated list of sizes. The number of elements in this list determines the
   * number of hidden layers in the network.
   *
   * @param sizes the sizes of the hidden layers
   */
  public void setHiddenLayerSizes(String sizes) {
    m_hiddenLayerSizes = sizes;
  }

  /**
   * Get whether to use drop connect
   *
   * @return true if drop connect is to be used
   */
  @OptionMetadata(displayName = "Use drop connect",
    description = "Use drop connect.", commandLineParamName = "drop-connect",
    commandLineParamIsFlag = true, commandLineParamSynopsis = "-drop-connect",
    displayOrder = 17)
  public boolean getUseDropConnect() {
    return m_useDropConnect;
  }

  /**
   * Set whether to use drop connect
   *
   * @param d true if drop connect is to be used
   */
  public void setUseDropConnect(boolean d) {
    m_useDropConnect = d;
  }

  /**
   * Get whether to use pretraining or not
   *
   * @return true if pretraining is to be disabled
   */
  @OptionMetadata(displayName = "Don't pretrain",
    description = "Don't apply pretraining.",
    commandLineParamName = "no-pretraining", commandLineParamIsFlag = true,
    commandLineParamSynopsis = "-no-pretraining", displayOrder = 18)
  public boolean getDontPretrain() {
    return m_dontPretrain;
  }

  /**
   * Set whether to use pretraining or not
   *
   * @param d true if pretraining is to be disabled
   */
  public void setDontPretrain(boolean d) {
    m_dontPretrain = d;
  }

  /**
   * Get whether to use gaussian newton vector product back propagation
   *
   * @return true to use gaussian newton vector product back prop
   */
  @OptionMetadata(displayName = "Use gauss newton vector product backprop",
    description = "Use gaussian newton vector product back propagation",
    commandLineParamName = "G", commandLineParamIsFlag = true,
    commandLineParamSynopsis = "-G", displayOrder = 19)
  public boolean getUseGaussNewtonVectorProductBackProp() {
    return m_useGaussNewtonVectorProductBackProp;
  }

  /**
   * Set whether to use gaussian newton vector product back propagation
   *
   * @param u true to use gaussian newton vector product back prop
   */
  public void setUseGaussNewtonVectorProductBackProp(boolean u) {
    m_useGaussNewtonVectorProductBackProp = u;
  }

  /**
   * Get whether to use RBM prop up as activations
   *
   * @return true if this is to be disabled
   */
  @OptionMetadata(displayName = "Don't use RBM prop-up as activations",
    description = "Don't use RBM prop-up as activations",
    commandLineParamName = "R", commandLineParamIsFlag = true,
    commandLineParamSynopsis = "-R", displayOrder = 20)
  public boolean getDontUseRBMPropUpAsActivations() {
    return m_dontUseRBMPropUpAsActivations;
  }

  /**
   * Set whether to use RBM prop up as activations
   *
   * @param d true if this is to be disabled
   */
  public void setDontUseRBMPropUpAsActivations(boolean d) {
    m_dontUseRBMPropUpAsActivations = d;
  }

  /**
   * Get the damping factor to use
   *
   * @return the damping factor to use
   */
  @OptionMetadata(displayName = "Damping factor",
    description = "The damping factor to use (default = 100)",
    commandLineParamName = "damping",
    commandLineParamSynopsis = "-damping <double>", displayOrder = 21)
  public double getDampingFactor() {
    return m_dampingFactor;
  }

  /**
   * Set the damping factor to use
   *
   * @param damp the damping factor to use
   */
  public void setDampingFactor(double damp) {
    m_dampingFactor = damp;
  }

  /**
   * Global help info
   *
   * @return global help info
   */
  public String globalInfo() {
    return "Wrapper classifier for the deeplearning4j library";
  }

  /**
   * Generates the classifier.
   *
   * @param data set of instances serving as training data
   * @exception Exception if the classifier has not been generated successfully
   */
  @Override
  public void buildClassifier(Instances data) throws Exception {
    m_zeroR = null;

    // remove instances with missing class
    data = new Instances(data);
    data.deleteWithMissingClass();

    if (data.numInstances() == 0 || data.numAttributes() < 2) {
      m_zeroR.buildClassifier(data);
      return;
    }

    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    m_replaceMissing = new ReplaceMissingValues();
    m_replaceMissing.setInputFormat(data);
    data = Filter.useFilter(data, m_replaceMissing);
    if (m_standardizeInsteadOfNormalize) {
      m_normalize = new Standardize();
    } else {
      m_normalize = new Normalize();
    }
    m_normalize.setInputFormat(data);
    data = Filter.useFilter(data, m_normalize);
    m_nominalToBinary = new NominalToBinary();
    m_nominalToBinary.setInputFormat(data);
    data = Filter.useFilter(data, m_nominalToBinary);
    data.randomize(new Random(123));

    DataSet dataset = Utils.instancesToDataSet(data);

    m_layerConfig = new NeuralNetConfiguration();
    RandomGenerator gen = new MersenneTwister(123);
    m_layerConfig.setRng(gen);
    m_layerConfig.setBatchSize(data.numInstances());
    m_layerConfig.setNumIterations(m_iterations);
    m_layerConfig.setLr(m_learningRate);
    m_layerConfig.setMomentum(m_momentum);
    m_layerConfig.setDropOut(m_dropOut);
    m_layerConfig.setConstrainGradientToUnitNorm(m_constrainGradientToUnitNorm);
    m_layerConfig.setUseRegularization(m_useRegularization);
    m_layerConfig.setL2(m_l2);
    m_layerConfig.setK(m_k);
    m_layerType.configureLayerFactory(m_layerConfig);
    m_activationFunction.configureActivationFunction(m_layerConfig);
    m_layerConfig.setOptimizationAlgo(m_optimizationStrategy);
    m_layerConfig.setLossFunction(m_lossFunction);
    m_layerConfig.setVisibleUnit(m_rbmVisibleUnit);
    m_layerConfig.setHiddenUnit(m_rbmHiddenUnit);

    m_layerConfig.setWeightInit(m_weightInit);

    // TODO Distributions (for sampling?). These are commons.math3 in this
    // version of DL4J but in master they have moved to
    // org.nd4j.linalg.api.rng.distribution.Distribution.

    m_layerConfig.setnIn(data.numAttributes() - 1);
    m_layerConfig.setnOut(data.classAttribute().numValues());

    if (m_hiddenLayerSizes == null || m_hiddenLayerSizes.length() == 0) {
      throw new Exception("Must specify at least one hidden layer");
    }
    String[] split = m_hiddenLayerSizes.split(",");
    int size = split.length;
    if (size < 1) {
      throw new Exception("Must have at least one hidden layer in the network");
    }
    int[] layerSizes = new int[size];
    size++; // +1 for the output layer
    List<NeuralNetConfiguration> layerConfs =
      new ArrayList<NeuralNetConfiguration>();
    for (int i = 0; i < size; i++) {
      layerConfs.add(m_layerConfig.clone());
      if (i < size - 1) {
        try {
          layerSizes[i] = Integer.parseInt(split[i]);
        } catch (NumberFormatException e) {
          throw new Exception(e);
        }
      }
    }

    // make sure our net is a classifier!
    layerConfs.get(size - 1).setWeightInit(WeightInit.ZERO);
    layerConfs.get(size - 1).setLayerFactory(
      LayerFactories.getFactory(OutputLayer.class));
    layerConfs.get(size - 1).setActivationFunction(new SoftMax());
    layerConfs.get(size - 1).setLossFunction(LossFunctions.LossFunction.MCXENT);

    System.err.println("Layer conf:\n" + m_layerConfig.toString() + "\n\n");

    MultiLayerConfiguration.Builder multiBuilder =
      new MultiLayerConfiguration.Builder();
    multiBuilder.confs(layerConfs);
    multiBuilder.hiddenLayerSizes(layerSizes);
    multiBuilder.useDropConnect(m_useDropConnect);
    multiBuilder.pretrain(!m_dontPretrain);
    multiBuilder.useRBMPropUpAsActivations(!m_dontUseRBMPropUpAsActivations);
    multiBuilder.dampingFactor(m_dampingFactor);
    MultiLayerConfiguration multiConf = multiBuilder.build();

    System.err.println("Multilayer conf:\n" + multiConf.toString());

    m_network = new MultiLayerNetwork(multiConf);
    m_network.fit(dataset);
  }

  /**
   * Calculates the class membership probabilities for the given test instance.
   *
   * @param inst the instance to be classified
   * @return predicted class probability distribution
   * @exception Exception if there is a problem generating the prediction
   */
  public double[] distributionForInstance(Instance inst) throws Exception {
    if (m_zeroR != null) {
      return m_zeroR.distributionForInstance(inst);
    }

    m_replaceMissing.input(inst);
    inst = m_replaceMissing.output();
    m_normalize.input(inst);
    inst = m_normalize.output();
    m_nominalToBinary.input(inst);
    inst = m_nominalToBinary.output();

    INDArray predicted = m_network.output(Utils.instanceToINDArray(inst));
    predicted = predicted.getRow(0);
    double[] preds = new double[inst.numClasses()];
    for (int i = 0; i < preds.length; i++) {
      preds[i] = predicted.getDouble(i);
    }
    weka.core.Utils.normalize(preds);

    return preds;
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return the capabilities of this classifier
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enable(Capabilities.Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capabilities.Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capabilities.Capability.MISSING_VALUES);

    // class
    result.enable(Capabilities.Capability.NOMINAL_CLASS);
    result.enable(Capabilities.Capability.MISSING_CLASS_VALUES);

    // instances
    result.setMinimumNumberInstances(0);

    return result;
  }

  public String toString() {
    if (m_layerConfig == null) {
      return "Classifier not built yet";
    }
    return m_layerConfig.toString();
  }

  /**
   * Enumeration for layer type
   */
  public static enum LayerType {
    RBM() {
      public void configureLayerFactory(NeuralNetConfiguration conf) {
        conf.setLayerFactory(new PretrainLayerFactory(RBM.class));
      }
    },
    AUTOENCODER() {
      public void configureLayerFactory(NeuralNetConfiguration conf) {
        conf.setLayerFactory(new PretrainLayerFactory(AutoEncoder.class));
      }
    },
    RECURSIVEAUTOENCODER() {
      public void configureLayerFactory(NeuralNetConfiguration conf) {
        conf.setLayerFactory(new RecursiveAutoEncoderLayerFactory(
          RecursiveAutoEncoder.class));
      }
    },
    CONVOLUTIONDOWNSAMPLELAYER() {
      public void configureLayerFactory(NeuralNetConfiguration conf) {
        conf.setLayerFactory(new ConvolutionLayerFactory(
          ConvolutionDownSampleLayer.class));
      }
    },
    LSTM() {
      public void configureLayerFactory(NeuralNetConfiguration conf) {
        conf.setLayerFactory(new LSTMLayerFactory(LSTM.class));
      }
    };

    abstract void configureLayerFactory(NeuralNetConfiguration config);
  }

  /**
   * Enumeration for activation function type
   */
  public static enum ActivationFunction {
    SIGMOID() {
      public void configureActivationFunction(NeuralNetConfiguration conf) {
        conf.setActivationFunction(new Sigmoid());
      }
    },
    LINEAR() {
      public void configureActivationFunction(NeuralNetConfiguration conf) {
        conf.setActivationFunction(new Linear());
      }
    },
    TANH() {
      public void configureActivationFunction(NeuralNetConfiguration conf) {
        conf.setActivationFunction(new Tanh());
      }
    },
    EXP() {
      public void configureActivationFunction(NeuralNetConfiguration conf) {
        conf.setActivationFunction(new Exp());
      }
    },
    SOFTMAX() {
      public void configureActivationFunction(NeuralNetConfiguration conf) {
        conf.setActivationFunction(new SoftMax());
      }
    },
    HARDTANH() {
      public void configureActivationFunction(NeuralNetConfiguration conf) {
        conf.setActivationFunction(new HardTanh());
      }
    },
    RECTIFIEDLINEAR() {
      public void configureActivationFunction(NeuralNetConfiguration conf) {
        conf.setActivationFunction(new RectifiedLinear());
      }
    },
    ROUNDLINEAR() {
      public void configureActivationFunction(NeuralNetConfiguration conf) {
        conf.setActivationFunction(new RoundedLinear());
      }
    },
    MAXOUT() {
      public void configureActivationFunction(NeuralNetConfiguration conf) {
        conf.setActivationFunction(new MaxOut());
      }
    };

    abstract void configureActivationFunction(NeuralNetConfiguration conf);
  }
}
