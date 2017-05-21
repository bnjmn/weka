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
 *    ConvolutionLayer.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.dl4j.layers;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Map;

import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.distribution.Distribution;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.IActivation;

import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;
import weka.dl4j.distribution.NormalDistribution;
import weka.gui.ProgrammaticProperty;
import weka.dl4j.activations.ActivationIdentity;

/**
 * A version of DeepLearning4j's BatchNormalization layer that implements WEKA option handling.
 *
 * @author Christopher Beckham
 * @author Eibe Frank
 *
 * @version $Revision: 11711 $
 */
public class BatchNormalization extends org.deeplearning4j.nn.conf.layers.BatchNormalization implements OptionHandler, Serializable {

	/** The ID used to serialize this class. */
	private static final long serialVersionUID = 6804344091980568487L;

	/**
	 * Global info.
	 *
	 * @return string describing this class.
	 */
	public String globalInfo() {
		return "A convolution layer from DeepLearning4J.";
	}

	/**
	 * Constructor for setting some defaults.
	 */
	public BatchNormalization() {
		setLayerName("Batch normalization layer");
		setActivationFunction(new ActivationIdentity());
		setWeightInit(WeightInit.XAVIER);
		setDist(new NormalDistribution());
		setUpdater(Updater.NESTEROVS);
		setLearningRate(0.01);
		setBiasLearningRate(getLearningRate());
		setMomentum(0.9);
		setBiasInit(1.0);
		setAdamMeanDecay(0.9);
		setAdamVarDecay(0.999);
		setEpsilon(1e-6);
		setRmsDecay(0.95);
		setDecay(0.9);
		setEps(1e-5);
		setGamma(1.0);
		setBeta(0.0);
	}

	@OptionMetadata(
					displayName = "layer name",
					description = "The name of the layer (default = Batch normalization Layer).",
					commandLineParamName = "name", commandLineParamSynopsis = "-name <string>",
					displayOrder = 0)
	public String getLayerName() {
		return this.layerName;
	}
	public void setLayerName(String layerName) {
		this.layerName = layerName;
	}

	@OptionMetadata(
			displayName = "decay parameter",
			description = "The decay parameter (default = 0.9).",
			commandLineParamName = "decay", commandLineParamSynopsis = "-decay <double>",
			displayOrder = 1)
	public double getDecay() {
		return this.decay;
	}
	public void setDecay(double decay) {
		this.decay = decay;
	}

	@OptionMetadata(
			displayName = "eps parameter",
			description = "The eps parameter (default = 1e-5).",
			commandLineParamName = "eps", commandLineParamSynopsis = "-eps <double>",
			displayOrder = 2)
	public double getEps() {
		return this.eps;
	}
	public void setEps(double eps) {
		this.eps = eps;
	}

	@OptionMetadata(
			displayName = "gamma parameter",
			description = "The gamma parameter (default = 1).",
			commandLineParamName = "gamma", commandLineParamSynopsis = "-gamma <double>",
			displayOrder = 3)
	public double getGamma() {
		return this.gamma;
	}
	public void setGamma(double gamma) {
		this.gamma = gamma;
	}

	@OptionMetadata(
			displayName = "beta parameter",
			description = "The beta parameter (default = 0).",
			commandLineParamName = "beta", commandLineParamSynopsis = "-beta <double>",
			displayOrder = 4)
	public double getBeta() {
		return this.beta;
	}
	public void setBeta(double beta) {
		this.beta = beta;
	}

	@OptionMetadata(
			displayName = "lock gamma and beta",
			description = "Whether to lock gamma and beta.",
			commandLineParamName = "beta", commandLineParamSynopsis = "-lockGammaBeta",
			displayOrder = 5)
	public boolean getLockGammaAndBeta() {
		return super.isLockGammaBeta();
	}
	public void setLockGammaAndBeta(boolean lgb) { super.setLockGammaBeta(lgb); }

	@ProgrammaticProperty
	public boolean isLockGammaBeta() { return super.isLockGammaBeta(); }
	public void setLockGammaBeta(boolean lgb) { super.setLockGammaBeta(lgb); }

	@OptionMetadata(
			displayName = "noMinibatch",
			description = "Whether minibatches are not not used.",
			commandLineParamName = "noMinibatch", commandLineParamSynopsis = "-noMinibatch",
			displayOrder = 6)
	public boolean getNoMinibatch() {
		return !super.isMinibatch();
	}
	public void setNoMinibatch(boolean b) { super.setMinibatch(!b); }

	@ProgrammaticProperty
	public boolean isMinibatch() { return super.isMinibatch(); }
	public void setMinibatch(boolean b) { super.setMinibatch(b); }

	@OptionMetadata(
					displayName = "activation function",
					description = "The activation function to use (default = Identity).",
					commandLineParamName = "activation", commandLineParamSynopsis = "-activation <specification>",
					displayOrder = 7)
	public IActivation getActivationFunction() { return this.activationFn; }
	public void setActivationFunction(IActivation activationFn) {
		this.activationFn = activationFn;
	}

	@ProgrammaticProperty
	public IActivation getActivationFn() { return super.getActivationFn(); }
	public void setActivationFn(IActivation fn) {
		super.setActivationFn(fn);
	}

	@OptionMetadata(
					displayName = "weight initialization method",
					description = "The method for weight initialization (default = XAVIER).",
					commandLineParamName = "weightInit", commandLineParamSynopsis = "-weightInit <specification>",
					displayOrder = 8)
	public WeightInit getWeightInit() {
		return this.weightInit;
	}
	public void setWeightInit(WeightInit weightInit) {
		this.weightInit = weightInit;
	}

	@OptionMetadata(
					displayName = "bias initialization",
					description = "The bias initialization (default = 1.0).",
					commandLineParamName = "biasInit", commandLineParamSynopsis = "-biasInit <double>",
					displayOrder = 9)
	public double getBiasInit() {
		return this.biasInit;
	}
	public void setBiasInit(double biasInit) {
		this.biasInit = biasInit;
	}

	@OptionMetadata(
					displayName = "distribution",
					description = "The distribution (default = NormalDistribution(1e-3, 1)).",
					commandLineParamName = "dist", commandLineParamSynopsis = "-dist <specification>",
					displayOrder = 10)
	public Distribution getDist() {
		return this.dist;
	}
	public void setDist(Distribution dist) {
		this.dist = dist;
	}

	@OptionMetadata(
					displayName = "learning rate",
					description = "The learning rate (default = 0.01).",
					commandLineParamName = "lr", commandLineParamSynopsis = "-lr <double>",
					displayOrder = 11)
	public double getLearningRate() {
		return this.learningRate;
	}
	public void setLearningRate(double learningRate) {
		this.learningRate = learningRate;
	}

	@OptionMetadata(
					displayName = "bias learning rate",
					description = "The bias learning rate (default = 0.01).",
					commandLineParamName = "blr", commandLineParamSynopsis = "-blr <double>",
					displayOrder = 12)
	public double getBiasLearningRate() {
		return this.biasLearningRate;
	}
	public void setBiasLearningRate(double biasLearningRate) {
		this.biasLearningRate = biasLearningRate;
	}

	@OptionMetadata(
					displayName = "learning rate schedule",
					description = "The learning rate schedule.",
					commandLineParamName = "lrSchedule", commandLineParamSynopsis = "-lrSchedule <specification>",
					displayOrder = 13)
	public Map<Integer, Double> getLearningRateSchedule() {
		return this.learningRateSchedule;
	}
	public void setLearningRateSchedule(Map<Integer, Double> learningRateSchedule) {
		this.learningRateSchedule = learningRateSchedule;
	}

	@OptionMetadata(
					displayName = "momentum",
					description = "The momentum (default = 0.9).",
					commandLineParamName = "momentum", commandLineParamSynopsis = "-momentum <double>",
					displayOrder = 14)
	public double getMomentum() {
		return this.momentum;
	}
	public void setMomentum(double momentum) {
		this.momentum = momentum;
	}

	@OptionMetadata(
					displayName = "momentum schedule",
					description = "The momentum schedule.",
					commandLineParamName = "momentumSchedule", commandLineParamSynopsis = "-momentumSchedule <specification>",
					displayOrder = 15)
	public Map<Integer, Double> getMomentumSchedule() {
		return this.momentumSchedule;
	}
	public void setMomentumSchedule(Map<Integer, Double> momentumSchedule) {
		this.momentumSchedule = momentumSchedule;
	}

	@OptionMetadata(
					displayName = "L1",
					description = "The L1 parameter (default = 0).",
					commandLineParamName = "L1", commandLineParamSynopsis = "-L1 <double>",
					displayOrder = 16)
	public double getL1() {
		return this.l1;
	}
	public void setL1(double l1) {
		this.l1 = l1;
	}

	@OptionMetadata(
					displayName = "L2",
					description = "The L2 parameter (default = 0).",
					commandLineParamName = "L2", commandLineParamSynopsis = "-L2 <double>",
					displayOrder = 17)
	public double getL2() {
		return this.l2;
	}
	public void setL2(double l2) {
		this.l2 = l2;
	}

	@OptionMetadata(
					displayName = "L1 bias",
					description = "The L1 bias parameter (default = 0).",
					commandLineParamName = "l1Bias", commandLineParamSynopsis = "-l1Bias <double>",
					displayOrder = 18)
	public double getBiasL1() {
		return this.l1Bias;
	}
	public void setBiasL1(double biasL1) {
		this.l1Bias = biasL1;
	}

	@OptionMetadata(
					displayName = "L2 bias",
					description = "The L2 bias parameter (default = 0).",
					commandLineParamName = "l2Bias", commandLineParamSynopsis = "-l2Bias <double>",
					displayOrder = 19)
	public double getBiasL2() {
		return this.l2Bias;
	}
	public void setBiasL2(double biasL2) {
		this.l2Bias = biasL2;
	}

	@OptionMetadata(
					displayName = "dropout parameter",
					description = "The dropout parameter (default = 0).",
					commandLineParamName = "dropout", commandLineParamSynopsis = "-dropout <double>",
					displayOrder = 20)
	public double getDropOut() {
		return this.dropOut;
	}
	public void setDropOut(double dropOut) {
		this.dropOut = dropOut;
	}

	@OptionMetadata(
					displayName = "updater for stochastic gradient descent",
					description = "The updater for stochastic gradient descent (default NESTEROVS).",
					commandLineParamName = "updater", commandLineParamSynopsis = "-updater <speficiation>",
					displayOrder = 21)
	public Updater getUpdater() {
		return this.updater;
	}
	public void setUpdater(Updater updater) {
		this.updater = updater;
	}

	@OptionMetadata(
					displayName = "ADADELTA's rho parameter",
					description = "ADADELTA's rho parameter (default = 0).",
					commandLineParamName = "rho", commandLineParamSynopsis = "-rho <double>",
					displayOrder = 22)
	public double getRho() {
		return this.rho;
	}
	public void setRho(double rho) {
		this.rho = rho;
	}

	@OptionMetadata(
					displayName = "ADADELTA's epsilon parameter",
					description = "ADADELTA's epsilon parameter (default = 1e-6).",
					commandLineParamName = "epsilon", commandLineParamSynopsis = "-epsilon <double>",
					displayOrder = 23)
	public double getEpsilon() {
		return this.epsilon;
	}
	public void setEpsilon(double epsilon) {
		this.epsilon = epsilon;
	}

	@OptionMetadata(
					displayName = "RMSPROP's RMS decay parameter",
					description = "RMSPROP's RMS decay parameter (default = 0.95).",
					commandLineParamName = "rmsDecay", commandLineParamSynopsis = "-rmsDecay <double>",
					displayOrder = 24)
	public double getRmsDecay() {
		return this.rmsDecay;
	}
	public void setRmsDecay(double rmsDecay) {
		this.rmsDecay = rmsDecay;
	}

	@OptionMetadata(
					displayName = "ADAM's mean decay parameter",
					description = "ADAM's mean decay parameter (default 0.9).",
					commandLineParamName = "adamMeanDecay", commandLineParamSynopsis = "-adamMeanDecay <double>",
					displayOrder = 25)
	public double getAdamMeanDecay() { return this.adamMeanDecay; }
	public void setAdamMeanDecay(double adamMeanDecay) {
		this.adamMeanDecay = adamMeanDecay;
	}

	@OptionMetadata(
					displayName = "ADAMS's var decay parameter",
					description = "ADAM's var decay parameter (default 0.999).",
					commandLineParamName = "adamVarDecay", commandLineParamSynopsis = "-adamVarDecay <double>",
					displayOrder = 26)
	public double getAdamVarDecay() {
		return this.adamVarDecay;
	}
	public void setAdamVarDecay(double adamVarDecay) {
		this.adamVarDecay = adamVarDecay;
	}

	@OptionMetadata(
					displayName = "gradient normalization method",
					description = "The gradient normalization method (default = None).",
					commandLineParamName = "gradientNormalization", commandLineParamSynopsis = "-gradientNormalization <specification>",
					displayOrder = 27)
	public GradientNormalization getGradientNormalization() {
		return this.gradientNormalization;
	}
	public void setGradientNormalization(GradientNormalization gradientNormalization) {
		this.gradientNormalization = gradientNormalization;
	}

	@OptionMetadata(
					displayName = "gradient normalization threshold",
					description = "The gradient normalization threshold (default = 1).",
					commandLineParamName = "gradNormThreshold", commandLineParamSynopsis = "-gradNormThreshold <double>",
					displayOrder = 28)
	public double getGradientNormalizationThreshold() {
		return this.gradientNormalizationThreshold;
	}
	public void setGradientNormalizationThreshold(double gradientNormalizationThreshold) {
		this.gradientNormalizationThreshold = gradientNormalizationThreshold;
	}

	@ProgrammaticProperty
	public int getNIn() { return super.getNIn(); }
	public void setNIn(int nIn) {
		this.nIn = nIn;
	}

	@ProgrammaticProperty
	public int getNOut() { return super.getNOut(); }
	public void setNOut(int nOut) {
		this.nOut = nOut;
	}

	@ProgrammaticProperty
	public double getL1Bias() { return super.getL1Bias(); }
	public void setL1Bias(int l1bias) { super.setL1Bias(l1bias); }

	@ProgrammaticProperty
	public double getL2Bias() { return super.getL2Bias(); }
	public void setL2Bias(int l2bias) { super.setL2Bias(l2bias); }

	/**
	 * Returns an enumeration describing the available options.
	 *
	 * @return an enumeration of all the available options.
	 */
	@Override
	public Enumeration<Option> listOptions() {

		return Option.listOptionsForClass(this.getClass()).elements();
	}

	/**
	 * Gets the current settings of the Classifier.
	 *
	 * @return an array of strings suitable for passing to setOptions
	 */
	@Override
	public String[] getOptions() {

		return Option.getOptions(this, this.getClass());
	}

	/**
	 * Parses a given list of options.
	 *
	 * @param options the list of options as an array of strings
	 * @exception Exception if an option is not supported
	 */
	public void setOptions(String[] options) throws Exception {

		Option.setOptions(options, this, this.getClass());
	}
}