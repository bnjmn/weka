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
 *    OutputLayer.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.dl4j.layers;

import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.distribution.Distribution;
import weka.dl4j.distribution.NormalDistribution;
import org.deeplearning4j.nn.weights.WeightInit;

import org.nd4j.linalg.lossfunctions.ILossFunction;
import org.nd4j.linalg.lossfunctions.impl.LossMCXENT;

import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;
import weka.gui.ProgrammaticProperty;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Map;

/**
 * A version of DeepLearning4j's OutputLayer that implements WEKA option handling.
 *
 * @author Christopher Beckham
 * @author Eibe Frank
 *
 * @version $Revision: 11711 $
 */
public class OutputLayer extends org.deeplearning4j.nn.conf.layers.OutputLayer implements OptionHandler, Serializable {

	private static final long serialVersionUID = 139321786136127207L;

	/**
	 * Global info.
	 *
	 * @return string describing this class.
	 */
	public String globalInfo() {
		return "An output layer from DeepLearning4J.";
	}

	/**
	 * Constructor for setting some defaults.
	 */
	public OutputLayer() {
		setLayerName("Output layer");
		setActivationFunction("softmax");
		setWeightInit(WeightInit.XAVIER);
		setDist(new NormalDistribution());
		setUpdater(Updater.NESTEROVS);
		setLossFn(new LossMCXENT());
		setLearningRate(0.01);
		setBiasLearningRate(getLearningRate());
		setMomentum(0.9);
		setBiasInit(1.0);
		setAdamMeanDecay(0.9);
		setAdamVarDecay(0.999);
		setEpsilon(1e-6);
		setRmsDecay(0.95);
	}

	@OptionMetadata(
					displayName = "layer name",
					description = "The name of the layer (default = Output Layer).",
					commandLineParamName = "name", commandLineParamSynopsis = "-name <string>",
					displayOrder = 1)
	public String getLayerName() {
		return this.layerName;
	}
	public void setLayerName(String layerName) {
		this.layerName = layerName;
	}

	@OptionMetadata(
					displayName = "name of activation function",
					description = "The name of the activation function (default = softmax; options are softmax,logsoftmax,maxout,identity,abs,cos,elu,exp,log,pow,sin,acos,asin,atan,ceil,relu,sign,sqrt,step,tanh,floor,round,hardtanh,timesoneminus,negative,softplus,softsign,leakyrelu,stabilize,sigmoid).",
					commandLineParamName = "activation", commandLineParamSynopsis = "-activation <string>",
					displayOrder = 2)
	public String getActivationFunction() {
		return this.activationFunction;
	}
	public void setActivationFunction(String activationFunction) {
		this.activationFunction = activationFunction;
	}

	@OptionMetadata(
					displayName = "weight initialization method",
					description = "The method for weight initialization (default = XAVIER).",
					commandLineParamName = "weightInit", commandLineParamSynopsis = "-weightInit <specification>",
					displayOrder = 3)
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
					displayOrder = 4)
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
					displayOrder = 5)
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
					displayOrder = 6)
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
					displayOrder = 7)
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
					displayOrder = 8)
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
					displayOrder = 9)
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
					displayOrder = 10)
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
					displayOrder = 11)
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
					displayOrder = 12)
	public double getL2() {
		return this.l2;
	}
	public void setL2(double l2) {
		this.l2 = l2;
	}

	@OptionMetadata(
					displayName = "bias L1",
					description = "The bias L1 parameter (default = 0).",
					commandLineParamName = "biasL1", commandLineParamSynopsis = "-biasL1 <double>",
					displayOrder = 13)
	public double getBiasL1() {
		return this.biasL1;
	}
	public void setBiasL1(double biasL1) {
		this.biasL1 = biasL1;
	}

	@OptionMetadata(
					displayName = "bias L2",
					description = "The bias L2 parameter (default = 0).",
					commandLineParamName = "biasL2", commandLineParamSynopsis = "-biasL2 <double>",
					displayOrder = 14)
	public double getBiasL2() {
		return this.biasL2;
	}
	public void setBiasL2(double biasL2) {
		this.biasL2 = biasL2;
	}

	@OptionMetadata(
					displayName = "dropout parameter",
					description = "The dropout parameter (default = 0).",
					commandLineParamName = "dropout", commandLineParamSynopsis = "-dropout <double>",
					displayOrder = 15)
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
					displayOrder = 16)
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
					displayOrder = 17)
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
					displayOrder = 18)
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
					displayOrder = 19)
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
					displayOrder = 20)
	public double getAdamMeanDecay() { return this.adamMeanDecay; }
	public void setAdamMeanDecay(double adamMeanDecay) {
		this.adamMeanDecay = adamMeanDecay;
	}

	@OptionMetadata(
					displayName = "ADAMS's var decay parameter",
					description = "ADAM's var decay parameter (default 0.999).",
					commandLineParamName = "adamVarDecay", commandLineParamSynopsis = "-adamVarDecay <double>",
					displayOrder = 21)
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
					displayOrder = 22)
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
					displayOrder = 23)
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

	@OptionMetadata(
					displayName = "loss function",
					description = "The loss function to use (default = MCXENT).",
					commandLineParamName = "lossFn", commandLineParamSynopsis = "-lossFn <specification>",
					displayOrder = 24)
	public ILossFunction getLossFn() {
		return this.lossFn;
	}
	public void setLossFn(ILossFunction lossFn) {
		this.lossFn = lossFn;
	}

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