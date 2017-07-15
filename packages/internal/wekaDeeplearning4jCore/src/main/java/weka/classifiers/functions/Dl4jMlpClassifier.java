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

import java.io.*;
import java.util.ArrayList;
import java.util.Random;

import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.deeplearning4j.datasets.iterator.AsyncDataSetIterator;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration.ListBuilder;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.optimize.api.IterationListener;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weka.classifiers.IterativeClassifier;
import weka.classifiers.RandomizableClassifier;
import weka.classifiers.rules.ZeroR;
import weka.core.*;
import weka.dl4j.FileIterationListener;
import weka.dl4j.iterators.AbstractDataSetIterator;
import weka.dl4j.iterators.ConvolutionalInstancesIterator;
import weka.dl4j.iterators.DefaultInstancesIterator;
import weka.dl4j.iterators.ImageDataSetIterator;
import weka.dl4j.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.Layer;
import weka.dl4j.layers.OutputLayer;
import weka.dl4j.NeuralNetConfiguration;
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
 * @version $Revision: 11711 $
 */
public class Dl4jMlpClassifier extends RandomizableClassifier implements
  BatchPredictor, CapabilitiesHandler, IterativeClassifier {

  /** The ID used for serializing this class. */
  protected static final long serialVersionUID = -6363254116597574265L;

  /** The logger used in this class. */
  protected final Logger m_log = LoggerFactory.getLogger(Dl4jMlpClassifier.class);

  /** Filter used to replace missing values. */
  protected ReplaceMissingValues m_replaceMissing;

  /** Filter used to normalize or standardize the data. */
  protected Filter m_Filter;

  /** Filter used to convert nominal attributes to binary numeric attributes. */
  protected NominalToBinary m_nominalToBinary;

  /**
   * ZeroR classifier, just in case we don't actually have any data to train a
   * network.
   */
  protected ZeroR m_zeroR;

  /** The actual neural network model. **/
  protected transient MultiLayerNetwork m_model;

  /** The size of the serialized network model in bytes. **/
  protected long m_modelSize;

  /** The file that log information will be written to. */
  protected File m_logFile = new File(System.getProperty("user.dir"));

  /** The layers of the network. */
  protected Layer[] m_layers = new Layer[] {new OutputLayer()};

  /** The configuration of the network. */
  protected NeuralNetConfiguration m_configuration = new NeuralNetConfiguration();

  /** The number of epochs to perform. */
  protected int m_numEpochs = 10;

  /** The number of epochs that have been performed. */
  protected int m_NumEpochsPerformed;

  /** The actual dataset iterator. */
  protected transient DataSetIterator m_Iterator;

  /** The training instances (set to null when done() is called). */
  protected Instances m_Data;

  /** The dataset iterator to use. */
  protected AbstractDataSetIterator m_iterator = new DefaultInstancesIterator();

  /** Queue size for AsyncDataSetIterator (if < 1, AsyncDataSetIterator is not used) */
  protected int m_queueSize = 0;

  /** filter: Normalize training data */
  public static final int FILTER_NORMALIZE = 0;
  /** filter: Standardize training data */
  public static final int FILTER_STANDARDIZE = 1;
  /** filter: No normalization/standardization */
  public static final int FILTER_NONE = 2;
  /** The filter to apply to the training data */
  public static final Tag [] TAGS_FILTER = {
          new Tag(FILTER_NORMALIZE, "Normalize training data"),
          new Tag(FILTER_STANDARDIZE, "Standardize training data"),
          new Tag(FILTER_NONE, "No normalization/standardization"),
  };

  /** Whether to normalize/standardize/neither */
  protected int m_filterType = FILTER_STANDARDIZE;

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

    // figure out size of the written network
    CountingOutputStream cos = new CountingOutputStream(new NullOutputStream());
    if (m_replaceMissing != null) {
      ModelSerializer.writeModel(m_model, cos, false);
    }
    m_modelSize = cos.getByteCount();

    // default serialization
    oos.defaultWriteObject();

    // actually write the network
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
    ClassLoader origLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
      // default deserialization
      ois.defaultReadObject();

      // restore the network model
      if (m_replaceMissing != null) {
        File tmpFile = File.createTempFile("restore", "multiLayer");
        tmpFile.deleteOnExit();
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tmpFile));
        long remaining = m_modelSize;
        while (remaining > 0) {
          int bsize = 10024;
          if (remaining < 10024) {
            bsize = (int)remaining;
          }
          byte[] buffer = new byte[bsize];
          int len = ois.read(buffer);
          if (len == -1) {
            throw new IOException("Reached end of network model prematurely during deserialization.");
          }
          bos.write(buffer, 0, len);
          remaining -= len;
        }
        bos.flush();
        m_model = ModelSerializer.restoreMultiLayerNetwork(tmpFile, false);
      }
    } finally {
      Thread.currentThread().setContextClassLoader(origLoader);
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

  @OptionMetadata(displayName = "layer specification.",
    description = "The specification of a layer. This option can be used multiple times.",
    commandLineParamName = "layer",
    commandLineParamSynopsis = "-layer <string>", displayOrder = 2)
  public void setLayers(Layer[] layers) {
    m_layers = layers;
  }

  public int getNumEpochs() {
    return m_numEpochs;
  }

  @OptionMetadata(description = "The number of epochs to perform.",
    displayName = "number of epochs", commandLineParamName = "numEpochs",
    commandLineParamSynopsis = "-numEpochs <int>", displayOrder = 4)
  public void setNumEpochs(int numEpochs) {
    m_numEpochs = numEpochs;
  }

  @OptionMetadata(description = "The dataset iterator to use.",
          displayName = "dataset iterator", commandLineParamName = "iterator",
          commandLineParamSynopsis = "-iterator <string>", displayOrder = 6)
  public AbstractDataSetIterator getDataSetIterator() {
    return m_iterator;
  }

  public void setDataSetIterator(AbstractDataSetIterator iterator) {
    m_iterator = iterator;
  }

  @OptionMetadata(description = "The neural network configuration to use.",
          displayName = "network configuration", commandLineParamName = "config",
          commandLineParamSynopsis = "-config <string>", displayOrder = 7)
  public NeuralNetConfiguration getNeuralNetConfiguration() {
    return m_configuration;
  }

  public void setNeuralNetConfiguration(NeuralNetConfiguration config) {
    m_configuration = config;
  }

  @OptionMetadata(description = "The type of normalization to perform.",
          displayName = "attribute normalization", commandLineParamName = "normalization",
          commandLineParamSynopsis = "-normalization <int>", displayOrder = 8)
  public SelectedTag getFilterType() {
    return new SelectedTag(m_filterType, TAGS_FILTER);
  }

  public void setFilterType(SelectedTag newType) {
    if (newType.getTags() == TAGS_FILTER) {
      m_filterType = newType.getSelectedTag().getID();
    }
  }

  public int getQueueSize() {
    return m_queueSize;
  }

  @OptionMetadata(description = "The queue size for asynchronous data transfer (default: 0, synchronous transfer).",
          displayName = "queue size for asynchronous data transfer", commandLineParamName = "queueSize",
          commandLineParamSynopsis = "-queueSize <int>", displayOrder = 9)
  public void setQueueSize(int QueueSize) {
    m_queueSize = QueueSize;
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

    // Initialize classifier
    initializeClassifier(data);

    // For the given number of iterations
    while (next()) {}

    // Clean up
    done();
  }

  /**
   * The method used to initialize the classifier.
   *
   * @param data set of instances serving as training data
   * @throws Exception if something goes wrong in the training process
   */
  @Override
  public void initializeClassifier(Instances data) throws Exception {

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

    if (m_filterType == FILTER_STANDARDIZE) {
      m_Filter = new Standardize();
      m_Filter.setOptions(new String[]{"-unset-class-temporarily"});
      m_Filter.setInputFormat(data);
      data = Filter.useFilter(data, m_Filter);
    } else if (m_filterType == FILTER_NORMALIZE) {
      m_Filter = new Normalize();
      m_Filter.setOptions(new String[]{"-unset-class-temporarily"});
      m_Filter.setInputFormat(data);
      data = Filter.useFilter(data, m_Filter);
    } else {
      m_Filter = null;
    }

    double z0 = data.instance(0).classValue();
    double z1 = data.instance(index).classValue();
    m_x1 = (y0 - y1) / (z0 - z1); // no division by zero, since y0 != y1
    // guaranteed => z0 != z1 ???
    m_x0 = (y0 - m_x1 * z0); // = y1 - m_x1 * z1

    // Randomize the data, just in case
    Random rand = new Random(getSeed());
    data.randomize(rand);

    ClassLoader origLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

      // Initialize random number generator for construction of network
      NeuralNetConfiguration.Builder builder = new NeuralNetConfiguration.Builder(m_configuration);
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
      } else if (getDataSetIterator() instanceof ConvolutionalInstancesIterator) {
        ConvolutionalInstancesIterator cii = (ConvolutionalInstancesIterator) getDataSetIterator();
        ip.setInputType(InputType.convolutionalFlat(cii.getWidth(),
                cii.getHeight(), cii.getNumChannels()));
      }

      ip = ip.backprop(true);

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

      m_Data = data;

      m_Iterator = getDataSetIterator().getIterator(m_Data, getSeed());
      if (m_queueSize > 0) {
        m_Iterator = new AsyncDataSetIterator(m_Iterator, m_queueSize);
      }
      m_NumEpochsPerformed = 0;
    } finally {
      Thread.currentThread().setContextClassLoader(origLoader);
    }
  }

  /**
   * Perform another epoch.
   */
  public boolean next() throws Exception {

    if (m_NumEpochsPerformed >= getNumEpochs() || m_zeroR != null || m_Data == null) {
      return false;
    }

    if (m_Iterator == null) {
      m_Iterator = getDataSetIterator().getIterator(m_Data, getSeed());
      if (m_queueSize > 0) {
        m_Iterator = new AsyncDataSetIterator(m_Iterator, m_queueSize);
      }
    }

    ClassLoader origLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

      m_model.fit(m_Iterator); // Note that this calls the reset() method of the iterator
      if (getDebug()) {
        m_log.info("*** Completed epoch {} ***", m_NumEpochsPerformed + 1);
      }
      m_NumEpochsPerformed++;
    } finally {
      Thread.currentThread().setContextClassLoader(origLoader);
    }

    return true;
  }

  /**
   * Clean up after learning.
   */
  public void done() {

    m_Data = null;
  }

  /**
   * Performs efficient batch prediction
   *
   * @return true, as LogitBoost can perform efficient batch prediction
   */
  @Override
  public boolean implementsMoreEfficientBatchPrediction() {
    return true;
  }


  /**
   * The method to use when making a prediction for a test instance. Use distributionsForInstances() instead
   * for speed if possible.
   *
   * @param inst the instance to get a prediction for
   * @return the class probability estimates (if the class is nominal) or the
   *         numeric prediction (if it is numeric)
   * @throws Exception if something goes wrong at prediction time
   */
  @Override
  public double[] distributionForInstance(Instance inst) throws Exception {

    Instances data = new Instances(inst.dataset());
    data.add(inst);
    return distributionsForInstances(data)[0];
  }

  /**
   * The method to use when making predictions for test instances.
   *
   * @param insts the instances to get predictions for
   * @return the class probability estimates (if the class is nominal) or the
   *         numeric predictions (if it is numeric)
   * @throws Exception if something goes wrong at prediction time
   */
  @Override
  public double[][] distributionsForInstances(Instances insts) throws Exception {

    // Do we only have a ZeroR model?
    if (m_zeroR != null) {
      return m_zeroR.distributionsForInstances(insts);
    }

    // Filter the instance
    insts = Filter.useFilter(insts, m_replaceMissing);
    insts = Filter.useFilter(insts, m_nominalToBinary);
    if (m_Filter != null) {
      insts = m_Filter.useFilter(insts, m_Filter);
    }

    DataSetIterator it = getDataSetIterator().getIterator(insts, getSeed());
    INDArray predicted = m_model.output(it, false);
    double[][] preds = new double[insts.numInstances()][insts.numClasses()];
    for (int i = 0; i < preds.length; i++) {
      for (int j = 0; j < preds[i].length; j++) {
        preds[i][j] = predicted.getRow(i).getDouble(j);
      }

      // only normalise if we're dealing with classification
      if (preds[i].length > 1) {
        weka.core.Utils.normalize(preds[i]);
      } else {
        preds[i][0] = preds[i][0] * m_x1 + m_x0;
      }
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
