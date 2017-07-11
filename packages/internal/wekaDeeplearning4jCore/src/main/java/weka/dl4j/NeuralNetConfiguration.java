package weka.dl4j;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.LearningRatePolicy;
import org.deeplearning4j.nn.conf.stepfunctions.StepFunction;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;
import weka.dl4j.stepfunctions.NegativeGradientStepFunction;
import weka.gui.ProgrammaticProperty;
import java.io.Serializable;

import java.util.Enumeration;

/**
 * A version of DeepLearning4j's NeuralNetConfiguration that implements WEKA option handling.
 *
 * @author Eibe Frank
 *
 * @version $Revision: 11711 $
 */
public class NeuralNetConfiguration extends org.deeplearning4j.nn.conf.NeuralNetConfiguration implements Serializable, OptionHandler {

  /**
   * Constructor that provides default values for the settings.
   */
  public NeuralNetConfiguration() {

    this.leakyreluAlpha = 0.01D;
    this.miniBatch = true;
    this.numIterations = 1;
    this.maxNumLineSearchIterations = 5;
    this.useRegularization = true; // Changed this from the default in deepLearning4j.
    this.optimizationAlgo = OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT;
    this.stepFunction = new NegativeGradientStepFunction();
    this.useDropConnect = false;
    this.minimize = true;
    this.learningRatePolicy = LearningRatePolicy.None;
    this.lrPolicyDecayRate = 0.0D / 0.0;
    this.lrPolicySteps = 0.0D / 0.0;
    this.lrPolicyPower = 0.0D / 0.0;
    this.pretrain = false;
  }

  @OptionMetadata(
          description = "Optimization algorithm (LINE_GRADIENT_DESCENT,"
                  + " CONJUGATE_GRADIENT, HESSIAN_FREE, "
                  + "LBFGS, STOCHASTIC_GRADIENT_DESCENT)",
          displayName = "optimization algorithm", commandLineParamName = "algorithm",
          commandLineParamSynopsis = "-algorithm <string>", displayOrder = 0)
  public OptimizationAlgorithm getOptimizationAlgo() {
    return super.getOptimizationAlgo();
  }

  public void setOptimizationAlgo(OptimizationAlgorithm optimAlgorithm) {
    super.setOptimizationAlgo(optimAlgorithm);
  }

  @OptionMetadata(
          displayName = "leaky relu alpha",
          description = "The parameter for the leaky relu (default = 0.1).",
          commandLineParamName = "leakyreluAlpha", commandLineParamSynopsis = "-leakyreluAlpha <double>",
          displayOrder = 1)
  public double getLeakyreluAlpha() { return super.getLeakyreluAlpha(); }
  public void setLeakyreluAlpha(double a) { super.setLeakyreluAlpha(a); }

  @OptionMetadata(
          displayName = "learning rate policy",
          description = "The learning rate policy (default = None).",
          commandLineParamName = "learningRatePolicy", commandLineParamSynopsis = "-learningRatePolicy <string>",
          displayOrder = 2)
  public LearningRatePolicy getLearningRatePolicy() { return super.getLearningRatePolicy(); }
  public void setLearningRatePolicy(LearningRatePolicy p) { super.setLearningRatePolicy(p); }

  @OptionMetadata(
          displayName = "learning rate policy decay rate",
          description = "The learning rate policy decay rate (default = NaN).",
          commandLineParamName = "lrPolicyDecayRate", commandLineParamSynopsis = "-lrPolicyDecayRate <double>",
          displayOrder = 3)
  public double getLrPolicyDecayRate() { return super.getLrPolicyDecayRate(); }
  public void setLrPolicyDecayRate(double r) { super.setLrPolicyDecayRate(r); }

  @OptionMetadata(
          displayName = "learning rate policy power",
          description = "The learning rate policy power (default = NaN).",
          commandLineParamName = "lrPolicyPower", commandLineParamSynopsis = "-lrPolicyPower <double>",
          displayOrder = 4)
  public double getLrPolicyPower() { return super.getLrPolicyPower(); }
  public void setLrPolicyPower(double r) { super.setLrPolicyPower(r); }

  @OptionMetadata(
          displayName = "learning rate policy steps",
          description = "The learning rate policy steps (default = NaN).",
          commandLineParamName = "lrPolicySteps", commandLineParamSynopsis = "-lrPolicySteps <double>",
          displayOrder = 5)
  public double getLrPolicySteps() { return super.getLrPolicySteps(); }
  public void setLrPolicySteps(double r) { super.setLrPolicySteps(r); }

  @OptionMetadata(
          displayName = "maximum number of line search iterations",
          description = "The maximum number of line search iterations (default = 5).",
          commandLineParamName = "maxNumLineSearchIterations", commandLineParamSynopsis = "-maxNumLineSearchIterations <int>",
          displayOrder = 6)
  public int getMaxNumLineSearchIterations() { return super.getMaxNumLineSearchIterations(); }
  public void setMaxNumLineSearchIterations(int n) { super.setMaxNumLineSearchIterations(n); }

  @OptionMetadata(
          displayName = "whether to minimize objective",
          description = "Whether to minimize objective.", commandLineParamIsFlag = true,
          commandLineParamName = "minimize", commandLineParamSynopsis = "-minimize",
          displayOrder = 7)
  public boolean isMinimize() { return super.isMinimize(); }
  public void setMinimize(boolean b) { super.setMinimize(b); }

  @OptionMetadata(
          displayName = "whether to use drop connect",
          description = "Whether to use drop connect.", commandLineParamIsFlag = true,
          commandLineParamName = "useDropConnect", commandLineParamSynopsis = "-useDropConnect",
          displayOrder = 8)
  public boolean isUseDropConnect() { return super.isUseDropConnect(); }
  public void setUseDropConnect(boolean b) { super.setUseDropConnect(b); }

  @OptionMetadata(
          displayName = "whether to use regularization",
          description = "Whether to use regularization.", commandLineParamIsFlag = true,
          commandLineParamName = "useRegularization", commandLineParamSynopsis = "-useRegularization",
          displayOrder = 9)
  public boolean isUseRegularization() { return super.isUseRegularization(); }
  public void setUseRegularization(boolean b) { super.setUseRegularization(b); }

  @OptionMetadata(
          displayName = "number of iterations for optimization",
          description = "The number of iterations for optimization (default = 1).",
          commandLineParamName = "numIterations", commandLineParamSynopsis = "-numIterations <int>",
          displayOrder = 10)
  public int getNumIterations() { return super.getNumIterations(); }
  public void setNumIterations(int n) { super.setNumIterations(n); }

  @OptionMetadata(
          displayName = "step function",
          description = "The step function to use (default = default).",
          commandLineParamName = "stepFunction", commandLineParamSynopsis = "-stepFunction <string>",
          displayOrder = 11)
  public StepFunction getStepFunction() { return super.getStepFunction(); }
  public void setStepFunction(StepFunction f) { super.setStepFunction(f); }

  @ProgrammaticProperty
  public int getIterationCount() { return super.getIterationCount(); }
  public void setIterationCount(int n) { super.setIterationCount(n); }

  @ProgrammaticProperty
  public long getSeed() { return super.getSeed(); }
  public void setSeed(long n) { super.setSeed(n); }

  @ProgrammaticProperty
  public boolean isMiniBatch() { return super.isMiniBatch(); }
  public void setMiniBatch(boolean b) { super.setMiniBatch(b); }

  @ProgrammaticProperty
  public boolean isPretrain() { return super.isPretrain(); }
  public void setPretrain(boolean b) { super.setPretrain(b); }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration<Option> listOptions() {

    return Option.listOptionsForClass(this.getClass()).elements();
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
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
