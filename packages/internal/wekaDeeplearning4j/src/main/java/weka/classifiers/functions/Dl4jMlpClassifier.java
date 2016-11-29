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
 *    Dl4jMlpClassifier.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.classifiers.functions;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Random;

import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration.ListBuilder;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.optimize.api.IterationListener;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weka.classifiers.RandomizableClassifier;
import weka.classifiers.rules.ZeroR;
import weka.core.*;
import weka.dl4j.FileIterationListener;
import weka.dl4j.iterators.AbstractDataSetIterator;
import weka.dl4j.iterators.DefaultInstancesIterator;
import weka.dl4j.iterators.ImageDataSetIterator;
import weka.dl4j.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.Layer;
import weka.dl4j.layers.OutputLayer;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NominalToBinary;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import weka.filters.unsupervised.attribute.Standardize;

/**
 * A wrapper for DeepLearning4j that can be used to train a multi-layer
 * perceptron using that library.
 *
 * @author Christopher Beckham
 * @author Eibe Frank
 *
 * @author Christopher Beckham
 *
 * @version $Revision: 11711 $
 */
public class Dl4jMlpClassifier extends RandomizableClassifier implements
  BatchPredictor, CapabilitiesHandler {

  /** The ID used for serializing this class. */
  protected static final long serialVersionUID = -6363254116597574265L;

  /** The logger used in this class. */
  protected final Logger m_log = LoggerFactory
    .getLogger(Dl4jMlpClassifier.class);

  /** Filter used to replace missing values. */
  protected ReplaceMissingValues m_replaceMissing;

  /** Filter used to normalize or standardize the data. */
  protected Filter m_normalize;

  /** Filter used to convert nominal attributes to binary numeric attributes. */
  protected NominalToBinary m_nominalToBinary;

  /**
   * ZeroR classifier, just in case we don't actually have any data to train a
   * network.
   */
  protected ZeroR m_zeroR;

  /** The actual neural network model. **/
  protected transient MultiLayerNetwork m_model;

  /** The file that log information will be written to. */
  protected File m_logFile = new File(System.getProperty("user.dir"));

  /** The layers of the network. */
  protected Layer[] m_layers = new Layer[] {new OutputLayer()};

  /** The configuration parameters of the network. */
  protected OptimizationAlgorithm m_algo = OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT;;

  /** The number of epochs to perform. */
  protected int m_numEpochs = 10;

  /** The dataset iterator to use. */
  protected AbstractDataSetIterator m_iterator = new DefaultInstancesIterator();

  /** Whether to standardize or normalize the data. */
  protected boolean m_standardizeInsteadOfNormalize = true;

  /** Coefficients used for normalizing the class */
  protected double m_x1 = 1.0;
  protected double m_x0 = 0.0;

  /**
   * The main method for running this class.
   *
   * @param argv the command-line arguments
   */
  public static void main(String[] argv) {
    runClassifier(new Dl4jMlpClassifier(), argv);
  }

  public String globalInfo() {
    return "Classification and regression with multilayer perceptrons using DeepLearning4J.";
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    if (getDataSetIterator() instanceof ImageDataSetIterator) {
      result.enable(Capabilities.Capability.STRING_ATTRIBUTES);
    } else {
      result.enable(Capabilities.Capability.NOMINAL_ATTRIBUTES);
      result.enable(Capabilities.Capability.NUMERIC_ATTRIBUTES);
      result.enable(Capabilities.Capability.DATE_ATTRIBUTES);
      result.enable(Capabilities.Capability.MISSING_VALUES);
      result.enableDependency(Capabilities.Capability.STRING_ATTRIBUTES); // User might switch to ImageDSI in GUI
    }

    // class
    result.enable(Capabilities.Capability.NOMINAL_CLASS);
    result.enable(Capabilities.Capability.NUMERIC_CLASS);
    result.enable(Capabilities.Capability.DATE_CLASS);
    result.enable(Capabilities.Capability.MISSING_CLASS_VALUES);

    return result;
  }

  /**
   * Custom serialization method.
   *
   * @param oos the object output stream
   * @throws IOException
   */
  private void writeObject(ObjectOutputStream oos) throws IOException {

    // default serialization
    oos.defaultWriteObject();

    // write the network
    if (m_replaceMissing != null) {
      ModelSerializer.writeModel(m_model, oos, false);
    }
  }

  /**
   * Custom deserialization method
   *
   * @param ois the object input stream
   * @throws ClassNotFoundException
   * @throws IOException
   */
  private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {

    // default deserialization
    ois.defaultReadObject();

    // restore the model
    if (m_replaceMissing != null) {
      m_model = ModelSerializer.restoreMultiLayerNetwork(ois, false);
    }
  }

  /**
   * Get the log file
   * 
   * @return the log file
   */
  public File getLogFile() {
    return m_logFile;
  }

  /**
   * Set the log file
   *
   * @param logFile the log file
   */
  @OptionMetadata(displayName = "log file",
    description = "The name of the log file to write loss information to "
      + "(default = no log file).", commandLineParamName = "logFile",
    commandLineParamSynopsis = "-logFile <string>", displayOrder = 1)
  public void setLogFile(File logFile) {
    m_logFile = logFile;
  }

  public Layer[] getLayers() {
    return m_layers;
  }

  @OptionMetadata(displayName = "layer specification",
    description = "The specification of the layers.",
    commandLineParamName = "layers",
    commandLineParamSynopsis = "-layers <string>", displayOrder = 2)
  public void setLayers(Layer[] layers) {
    m_layers = layers;
  }

  public int getNumEpochs() {
    return m_numEpochs;
  }

  @OptionMetadata(description = "The number of epochs to perform",
    displayName = "number of epochs", commandLineParamName = "numEpochs",
    commandLineParamSynopsis = "-numEpochs <int>", displayOrder = 4)
  public void setNumEpochs(int numEpochs) {
    m_numEpochs = numEpochs;
  }

  @OptionMetadata(
    description = "Optimization algorithm (LINE_GRADIENT_DESCENT,"
      + " CONJUGATE_GRADIENT, HESSIAN_FREE, "
      + "LBFGS, STOCHASTIC_GRADIENT_DESCENT)",
    displayName = "optimization algorithm", commandLineParamName = "algorithm",
    commandLineParamSynopsis = "-algorithm <string>", displayOrder = 5)
  public OptimizationAlgorithm getOptimizationAlgorithm() {
    return m_algo;
  }

  public void setOptimizationAlgorithm(OptimizationAlgorithm optimAlgorithm) {
    m_algo = optimAlgorithm;
  }

  @OptionMetadata(description = "The dataset iterator to use",
    displayName = "dataset iterator", commandLineParamName = "iterator",
    commandLineParamSynopsis = "-iterator <string>", displayOrder = 6)
  public AbstractDataSetIterator getDataSetIterator() {
    return m_iterator;
  }

  public void setDataSetIterator(AbstractDataSetIterator iterator) {
    m_iterator = iterator;
  }

  /**
   * Get the current number of units for a particular layer. Returns -1 for
   * anything that is not a DenseLayer or an OutputLayer.
   *
   * @param layer the layer
   * @return the number of units
   */
  protected int getNumUnits(Layer layer) {

    if (layer instanceof DenseLayer) {
      return ((DenseLayer) layer).getNOut();
    } else if (layer instanceof OutputLayer) {
      return ((OutputLayer) layer).getNOut();
    }
    return -1;
  }

  /**
   * Sets the number of incoming connections for the nodes in the given layer.
   *
   * @param layer the layer
   * @param numInputs the number of inputs
   */
  protected void setNumIncoming(Layer layer, int numInputs) {

    if (layer instanceof DenseLayer) {
      ((DenseLayer) layer).setNIn(numInputs);
    } else if (layer instanceof OutputLayer) {
      ((OutputLayer) layer).setNIn(numInputs);
    }
  }

  /**
   * The method used to train the classifier.
   * 
   * @param data set of instances serving as training data
   * @throws Exception if something goes wrong in the training process
   */
  @Override
  public void buildClassifier(Instances data) throws Exception {

    // Can classifier handle the data?
    getCapabilities().testWithFail(data);

    // Check basic network structure
    if (m_layers.length == 0) {
      throw new Exception("No layers have been added!");
    }
    if (!(m_layers[m_layers.length - 1] instanceof OutputLayer)) {
      throw new Exception("Last layer in network must be an output layer!");
    }

    // Remove instances with missing class and check that instances and
    // predictor attributes remain.
    data = new Instances(data);
    data.deleteWithMissingClass();
    m_zeroR = null;
    if (data.numInstances() == 0 || data.numAttributes() < 2) {
      m_zeroR = new ZeroR();
      m_zeroR.buildClassifier(data);
      return;
    }

    // Replace missing values
    m_replaceMissing = new ReplaceMissingValues();
    m_replaceMissing.setInputFormat(data);
    data = Filter.useFilter(data, m_replaceMissing);

    // Retrieve two different class values used to determine filter
    // transformation
    double y0 = data.instance(0).classValue();
    int index = 1;
    while (index < data.numInstances()
      && data.instance(index).classValue() == y0) {
      index++;
    }
    if (index == data.numInstances()) {
      // degenerate case, all class values are equal
      // we don't want to deal with this, too much hassle
      throw new Exception(
        "All class values are the same. At least two class values should be different");
    }
    double y1 = data.instance(index).classValue();

    // Replace nominal attributes by binary numeric attributes.
    m_nominalToBinary = new NominalToBinary();
    m_nominalToBinary.setInputFormat(data);
    data = Filter.useFilter(data, m_nominalToBinary);

    // Standardize or normalize (as requested), including the class
    if (m_standardizeInsteadOfNormalize) {
      m_normalize = new Standardize();
      m_normalize.setOptions(new String[] { "-unset-class-temporarily" });
    } else {
      m_normalize = new Normalize();
    }
    m_normalize.setInputFormat(data);
    data = Filter.useFilter(data, m_normalize);

    double z0 = data.instance(0).classValue();
    double z1 = data.instance(index).classValue();
    m_x1 = (y0 - y1) / (z0 - z1); // no division by zero, since y0 != y1
                                  // guaranteed => z0 != z1 ???
    m_x0 = (y0 - m_x1 * z0); // = y1 - m_x1 * z1

    // Randomize the data, just in case
    Random rand = new Random(getSeed());
    data.randomize(rand);

    // Initialize random number generator for construction of network
    NeuralNetConfiguration.Builder builder = new NeuralNetConfiguration.Builder();
    if (getOptimizationAlgorithm() == null) {
      builder.setOptimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT);
    } else {
      builder.setOptimizationAlgo(getOptimizationAlgorithm());
    }
    builder.setSeed(rand.nextInt());

    // Construct the mlp configuration
    ListBuilder ip = builder.list(getLayers());
    int numInputAttributes = getDataSetIterator().getNumAttributes(data);

    // Connect up the layers appropriately
    for (int x = 0; x < m_layers.length; x++) {

      // Is this the first hidden layer?
      if (x == 0) {
        setNumIncoming(m_layers[x], numInputAttributes);
      } else {
        setNumIncoming(m_layers[x], getNumUnits(m_layers[x - 1]));
      }

      // Is this the output layer?
      if (x == m_layers.length - 1) {
        ((OutputLayer) m_layers[x]).setNOut(data.numClasses());
      }
      ip = ip.layer(x, m_layers[x]);
    }

    // If we have a convolutional network
    if (getDataSetIterator() instanceof ImageDataSetIterator) {
      ImageDataSetIterator idsi = (ImageDataSetIterator) getDataSetIterator();
      ip.setInputType(InputType.convolutionalFlat(idsi.getWidth(),
        idsi.getHeight(), idsi.getNumChannels()));
    }

    ip = ip.pretrain(false).backprop(true);

    MultiLayerConfiguration conf = ip.build();

    if (getDebug()) {
      System.err.println(conf.toJson());
    }

    // build the network
    m_model = new MultiLayerNetwork(conf);
    m_model.init();

    if (getDebug()) {
      System.err.println(m_model.conf().toYaml());
    }

    ArrayList<IterationListener> listeners = new ArrayList<IterationListener>();
    listeners.add(new ScoreIterationListener(data.numInstances()
      / getDataSetIterator().getTrainBatchSize()));

    // if the log file doesn't point to a directory, set up the listener
    if (getLogFile() != null && !getLogFile().isDirectory()) {
      int numMiniBatches =
        (int) Math.ceil(((double) data.numInstances())
          / ((double) getDataSetIterator().getTrainBatchSize()));
      listeners.add(new FileIterationListener(getLogFile().getAbsolutePath(),
        numMiniBatches));
    }

    m_model.setListeners(listeners);

    // Abusing the MultipleEpochsIterator because it splits the data into
    // batches
    DataSetIterator iter = getDataSetIterator().getIterator(data, getSeed());
    for (int i = 0; i < getNumEpochs(); i++) {
      m_model.fit(iter); // Note that this calls the reset() method of the
                         // iterator
      if (getDebug()) {
        m_log.info("*** Completed epoch {} ***", i + 1);
      }
      iter.reset();
    }
  }

  /**
   * The method to use when making predictions for a test instance.
   *
   * @param inst the instance to get a prediction for
   * @return the class probability estimates (if the class is nominal) or the
   *         numeric prediction (if it is numeric)
   * @throws Exception if something goes wrong at prediction time
   */
  @Override
  public double[] distributionForInstance(Instance inst) throws Exception {

    // Do we only have a ZeroR model?
    if (m_zeroR != null) {
      return m_zeroR.distributionForInstance(inst);
    }

    // Filter the instance
    m_replaceMissing.input(inst);
    inst = m_replaceMissing.output();
    m_nominalToBinary.input(inst);
    inst = m_nominalToBinary.output();
    m_normalize.input(inst);
    inst = m_normalize.output();

    Instances insts = new Instances(inst.dataset(), 0);
    insts.add(inst);
    DataSet ds = getDataSetIterator().getIterator(insts, getSeed(), 1).next();
    INDArray predicted = m_model.output(ds.getFeatureMatrix(), false);
    predicted = predicted.getRow(0);
    double[] preds = new double[inst.numClasses()];
    for (int i = 0; i < preds.length; i++) {
      preds[i] = predicted.getDouble(i);
    }
    // only normalise if we're dealing with classification
    if (preds.length > 1) {
      weka.core.Utils.normalize(preds);
    } else {
      preds[0] = preds[0] * m_x1 + m_x0;
    }
    return preds;
  }

  /**
   * Returns a string describing the model.
   *
   * @return the model string
   */
  @Override
  public String toString() {

    if (m_replaceMissing != null) {
      return m_model.getLayerWiseConfigurations().toYaml();
    }
    return null;
  }
}
