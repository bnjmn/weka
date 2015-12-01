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
 * MILES.java
 * Copyright (C) 2008-09 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.attribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Vector;

import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.filters.SimpleBatchFilter;
import weka.filters.UnsupervisedFilter;

/**
 * <!-- globalinfo-start --> Implements the MILES transformation that maps
 * multiple instance bags into a high-dimensional single-instance feature space.<br>
 * For more information see:<br>
 * <br>
 * Y. Chen, J. Bi, J.Z. Wang (2006). MILES: Multiple-instance learning via
 * embedded instance selection. IEEE PAMI. 28(12):1931-1947.<br>
 * <br>
 * <p>James Foulds, Eibe Frank: Revisiting multiple-instance learning via embedded
 * instance selection. In: 21st Australasian Joint Conference on Artificial
 * Intelligence, 300-310, 2008.
 * </p>
 * <!-- globalinfo-end -->
 * 
 * <!-- technical-bibtex-start --> BibTeX:
 * 
 * <pre>
 * &#64;article{Chen2006,
 *    author = {Y. Chen and J. Bi and J.Z. Wang},
 *    journal = {IEEE PAMI},
 *    number = {12},
 *    pages = {1931-1947},
 *    title = {MILES: Multiple-instance learning via embedded instance selection},
 *    volume = {28},
 *    year = {2006}
 * }
 * 
 * &#64;inproceedings{Foulds2008,
 *    author = {James Foulds and Eibe Frank},
 *    booktitle = {21st Australasian Joint Conference on Artificial Intelligence},
 *    pages = {300-310},
 *    publisher = {Springer},
 *    title = {Revisiting multiple-instance learning via embedded instance selection},
 *    year = {2008}
 * }
 * </pre>
 * <p>
 * <!-- technical-bibtex-end -->
 * 
 * <!-- options-start --> Valid options are:
 * </p>
 * 
 * <pre>
 * -S &lt;num&gt;
 *  Specify the sigma parameter (default: sqrt(800000)
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Jimmy Foulds
 * @author Eibe Frank
 * @version $Revision$
 */
public class MILESFilter extends SimpleBatchFilter implements
  UnsupervisedFilter, OptionHandler, TechnicalInformationHandler {

  /** For serialization */
  static final long serialVersionUID = 4694489111366063853L;

  /** Index of bag attribute */
  public static final int BAG_ATTRIBUTE = 1;

  /** Index of label attribute */
  public static final int LABEL_ATTRIBUTE = 2;

  /** Sigma parameter (default: square root of 800000) */
  private double m_sigma = Math.sqrt(800000);

  /** Linked list of all instances collected */
  private LinkedList<Instance> m_allInsts = null;

  /**
   * Returns the tip text for this property
   *
   * @return the tip text for this property
   */
  public String sigmaTipText() {

    return "The value of the sigma parameter.";
  }

  /**
   * Sets the sigma parameter.
   *
   * @param sigma the sigma value
   */
  public void setSigma(double sigma) {
    m_sigma = sigma;
  }

  /**
   * Gets the sigma parameter.
   *
   * @return the tip-text for this property
   */
  public double getSigma() {
    return m_sigma;
  }

  /**
   * Global info for the filter.
   */
  @Override
  public String globalInfo() {
    return "Implements the MILES transformation that maps multiple instance bags into"
      + " a high-dimensional single-instance feature space."
      + "\n"
      + "For more information see:\n\n" + getTechnicalInformation().toString();
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing detailed
   * information about the technical background of this class, e.g., paper
   * reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  @Override
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation result;
    TechnicalInformation additional;

    result = new TechnicalInformation(Type.ARTICLE);
    result.setValue(Field.AUTHOR, "Y. Chen and J. Bi and J.Z. Wang");
    result.setValue(Field.TITLE,
      "MILES: Multiple-instance learning via embedded instance selection");
    result.setValue(Field.JOURNAL, "IEEE PAMI");
    result.setValue(Field.YEAR, "2006");
    result.setValue(Field.VOLUME, "28");
    result.setValue(Field.PAGES, "1931-1947");
    result.setValue(Field.NUMBER, "12");

    additional = result.add(Type.INPROCEEDINGS);
    additional.setValue(Field.AUTHOR, "James Foulds and Eibe Frank");
    additional.setValue(Field.TITLE,
      "Revisiting multiple-instance learning via embedded instance selection");
    additional.setValue(Field.BOOKTITLE,
      "21st Australasian Joint Conference on Artificial Intelligence");
    additional.setValue(Field.YEAR, "2008");
    additional.setValue(Field.PAGES, "300-310");
    additional.setValue(Field.PUBLISHER, "Springer");

    return result;
  }

  /**
   * Capabilities for the filter.
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.enable(Capability.ONLY_MULTIINSTANCE);
    return result;
  }

  /**
   * Determines the output format for the filter.
   */
  @Override
  protected Instances determineOutputFormat(Instances inputFormat) {

    // Create attributes
    ArrayList<Attribute> atts = new ArrayList<Attribute>();
    m_allInsts = new LinkedList<Instance>();
    for (int i = 0; i < getInputFormat().numInstances(); i++) {
      Instances bag = getInputFormat().instance(i).relationalValue(
        BAG_ATTRIBUTE);
      for (int j = 0; j < bag.numInstances(); j++) {
        m_allInsts.add(bag.instance(j));
      }
    }
    for (int i = 0; i < m_allInsts.size(); i++) {
      atts.add(new Attribute("" + i));
    }
    atts.add(inputFormat.attribute(LABEL_ATTRIBUTE)); // class

    // TODO set relation name properly
    Instances returner = new Instances("", atts, 0);
    returner.setClassIndex(returner.numAttributes() - 1);

    return returner;
  }

  /**
   * Processes a set of instances.
   */
  @Override
  protected Instances process(Instances inst) {

    // Get instances object with correct output format
    Instances result = getOutputFormat();
    result.setClassIndex(result.numAttributes() - 1);

    // Can't do much if bag is empty
    if (inst.numInstances() == 0) {
      return result;
    }

    // Go through all the instances in the bag to be transformed
    for (int i = 0; i < inst.numInstances(); i++) // for every bag
    {

      // Allocate memory for instance
      double[] outputInstance = new double[result.numAttributes()];

      // Get the bag
      Instances bag = inst.instance(i).relationalValue(BAG_ATTRIBUTE);
      int k = 0;
      for (Instance x_k : m_allInsts) // for every instance in every bag
      {
        // TODO handle empty bags
        double dSquared = Double.MAX_VALUE;
        for (int j = 0; j < bag.numInstances(); j++) // for every instance in
                                                     // the current bag
        {
          // Compute sum of squared differences
          double total = 0;
          Instance x_ij = bag.instance(j);
          double numMissingValues = 0;
          for (int l = 0; l < x_k.numAttributes(); l++) // for every attribute
          {
            // Can skip missing values in reference instance
            if (x_k.isMissing(l)) {
              continue;
            }
            // Need to keep track of how many values in current instance are
            // missing
            if (!x_ij.isMissing(l)) {
              total += (x_ij.value(l) - x_k.value(l))
                * (x_ij.value(l) - x_k.value(l));
            } else {
              numMissingValues++;
            }
          }
          // Adjust for missing values
          total *= x_k.numAttributes()
            / (x_k.numAttributes() - numMissingValues);

          // Update minimum
          if (total < dSquared || dSquared == Double.MAX_VALUE) {
            dSquared = total;
          }
        }
        if (dSquared == Double.MAX_VALUE) {
          outputInstance[k] = 0; // TODO is this ok?
        } else {
          outputInstance[k] = Math.exp(-1.0 * dSquared / (m_sigma * m_sigma));
        }
        k++;
      }

      // Set class label
      double label = inst.instance(i).value(LABEL_ATTRIBUTE);
      outputInstance[outputInstance.length - 1] = label;

      // Add instance to result
      result.add(new DenseInstance(inst.instance(i).weight(), outputInstance));
    }

    return result;
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>(1);

    newVector.add(new Option(
      "\tSpecify the sigma parameter (default: sqrt(800000)", "S", 1,
      "-S <num>"));

    newVector.addAll(Collections.list(super.listOptions()));

    return newVector.elements();
  }

  /**
   * <p>Parses a given list of options.</p>
   * 
   * <p>
   * <!-- options-start --> Valid options are:
   * </p>
   * 
   * <pre>
   * -S &lt;num&gt;
   *  Specify the sigma parameter (default: sqrt(800000)
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    String sigmaString = Utils.getOption('S', options);
    if (sigmaString.length() != 0) {
      setSigma(Double.parseDouble(sigmaString));
    } else {
      setSigma(Math.sqrt(800000));
    }

    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the filter.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> options = new Vector<String>();

    options.add("-S");
    options.add("" + getSigma());

    Collections.addAll(options, super.getOptions());

    return options.toArray(new String[0]);
  }

  public static void main(String[] args) {
    runFilter(new MILESFilter(), args);
  }

  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }
}
