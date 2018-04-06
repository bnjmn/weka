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
 * InMemory.java
 * Copyright (C) 2018 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.evaluation.output.prediction;

import weka.classifiers.Classifier;
import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.evaluation.NumericPrediction;
import weka.classifiers.evaluation.Prediction;
import weka.core.Attribute;
import weka.core.Instance;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <!-- globalinfo-start -->
 * * Stores the predictions in memory for programmatic retrieval.<br>
 * * Stores the instance, a prediction object and a map of attribute names with their associated values if an attribute was defined in a container per prediction.<br>
 * * The list of predictions can get retrieved using the getPredictions() method.<br>
 * * File output is disabled and buffer doesn't need to be supplied.
 * * <br><br>
 * <!-- globalinfo-end -->
 *
 * <!-- options-start -->
 * * Valid options are: <p>
 * *
 * * <pre> -p &lt;range&gt;
 * *  The range of attributes to print in addition to the classification.
 * *  (default: none)</pre>
 * *
 * * <pre> -distribution
 * *  Whether to turn on the output of the class distribution.
 * *  Only for nominal class attributes.
 * *  (default: off)</pre>
 * *
 * * <pre> -decimals &lt;num&gt;
 * *  The number of digits after the decimal point.
 * *  (default: 3)</pre>
 * *
 * * <pre> -file &lt;path&gt;
 * *  The file to store the output in, instead of outputting it on stdout.
 * *  Gets ignored if the supplied path is a directory.
 * *  (default: .)</pre>
 * *
 * * <pre> -suppress
 * *  In case the data gets stored in a file, then this flag can be used
 * *  to suppress the regular output.
 * *  (default: not suppressed)</pre>
 * *
 * <!-- options-end -->
 * 
 * @author fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class InMemory extends AbstractOutput {

  /** for serialization. */
  private static final long serialVersionUID = 3401604538169573720L;

  /**
   * Container for storing the predictions alongside the additional attributes.
   */
  public static class PredictionContainer {

    /** the instance. */
    public Instance instance = null;

    /** the prediction. */
    public Prediction prediction = null;

    /** the associated attribute values (attribute-name - value). */
    public Map<String,Object> attributeValues = new HashMap<>();

    /**
     * Returns a string representation of the container.
     *
     * @return		the string representation
     */
    @Override
    public String toString() {
      return instance + " - " + prediction + " - " + attributeValues;
    }
  }

  /** for storing the predictions. */
  protected List<PredictionContainer> m_Predictions;

  /**
   * Returns a string describing the output generator.
   * 
   * @return a description suitable for displaying in the GUI
   */
  @Override
  public String globalInfo() {
    return "Stores the predictions in memory for programmatic retrieval.\n"
      + "Stores the instance, a prediction object and a map of attribute names "
      + "with their associated values if an attribute was defined in a container "
      + "per prediction.\n"
      + "The list of predictions can get retrieved using the getPredictions() method.\n"
      + "File output is disabled and buffer doesn't need to be supplied.";
  }

  /**
   * Returns a short display text, to be used in comboboxes.
   * 
   * @return a short display text
   */
  @Override
  public String getDisplay() {
    return "InMemory";
  }

  /**
   * Ignored, as it does not generate any output.
   *
   * @param value ignored
   */
  @Override
  public void setOutputFile(File value) {
    super.setOutputFile(new File("."));
  }

  /**
   * Performs checks whether everything is correctly setup for the header.
   *
   * @return null if everything is in order, otherwise the error message
   */
  protected String checkHeader() {
    if (m_Buffer == null)
      m_Buffer = new StringBuffer();
    return super.checkHeader();
  }

  /**
   * Performs the actual printing of the header.
   */
  @Override
  protected void doPrintHeader() {
    m_Predictions = new ArrayList<>();
  }

  /**
   * Returns the additional attribute values as map.
   *
   * @param instance	the current instance
   * @return		the generated map (attribute-name - value)
   */
  protected Map<String,Object> attributeValuesToMap(Instance instance) {
    Map<String,Object>	result;

    result = new HashMap<>();
    m_Attributes.setUpper(instance.numAttributes() - 1);
    for (int i = 0; i < instance.numAttributes(); i++) {
      if (m_Attributes.isInRange(i) && i != instance.classIndex()) {
        switch (instance.attribute(i).type()) {
	  case Attribute.NOMINAL:
	  case Attribute.STRING:
	  case Attribute.RELATIONAL:
	  case Attribute.DATE:
	    result.put(instance.attribute(i).name(), instance.stringValue(i));
	    break;
	  case Attribute.NUMERIC:
	    result.put(instance.attribute(i).name(), instance.value(i));
	    break;
	  default:
	    throw new IllegalStateException(
	      "Unhandled attribute type for attribute '" + instance.attribute(i).name() + ": "
		+ Attribute.typeToString(instance.attribute(i).type()));
	}
      }
    }

    return result;
  }

  /**
   * Store the prediction made by the classifier as a string.
   * 
   * @param dist the distribution to use
   * @param inst the instance to generate text from
   * @param index the index in the dataset
   * @throws Exception if something goes wrong
   */
  @Override
  protected void doPrintClassification(double[] dist, Instance inst, int index) throws Exception {
    PredictionContainer cont;

    cont = new PredictionContainer();
    cont.instance = inst;
    if (inst.classAttribute().isNominal())
      cont.prediction = new NominalPrediction(inst.classValue(), dist, inst.weight());
    else
      cont.prediction = new NumericPrediction(inst.classValue(), dist[0], inst.weight());
    cont.attributeValues.putAll(attributeValuesToMap(inst));

    m_Predictions.add(cont);
  }

  /**
   * Store the prediction made by the classifier as a string.
   * 
   * @param classifier the classifier to use
   * @param inst the instance to generate text from
   * @param index the index in the dataset
   * @throws Exception if something goes wrong
   */
  @Override
  protected void doPrintClassification(Classifier classifier, Instance inst,
    int index) throws Exception {

    double[] d = classifier.distributionForInstance(inst);
    doPrintClassification(d, inst, index);
  }

  /**
   * Does nothing.
   */
  @Override
  protected void doPrintFooter() {
  }

  /**
   * Returns the collected predictions.
   *
   * @return the predictions
   */
  public List<PredictionContainer> getPredictions() {
    return m_Predictions;
  }
}
