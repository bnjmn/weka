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
 * RandomizableDistributionGenerator.java
 * Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.meta.generators;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.Option;
import weka.core.Utils;

/**
 * An abstract superclass for randomizable generators that make use of mean and
 * standard deviation.
 * 
 * @author fracpete (fracpet at waikato dot ac dot nz)
 * @version $Revision$
 */
public abstract class RandomizableDistributionGenerator extends
  RandomizableGenerator implements Mean {

  /** for serialization. */
  private static final long serialVersionUID = 955762136858704289L;

  /** The mean of the underlying distribution. */
  protected double m_Mean = 0.0;

  /** The standard deviation of the underlying distribution. */
  protected double m_StandardDeviation = 1.0;

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> result = new Vector<Option>();

    result.addAll(Collections.list(super.listOptions()));

    result.addElement(new Option("\tSets the mean of the generator\n"
      + "\t(default: 0)", "M", 1, "-M <num>"));

    result.addElement(new Option(
      "\tSets the standard deviation of the generator\n" + "\t(default: 1)",
      "SD", 1, "-SD <num>"));

    return result.elements();
  }

  /**
   * Parses a given list of options.
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    String tmpStr;

    super.setOptions(options);

    tmpStr = Utils.getOption("M", options);
    if (tmpStr.length() != 0) {
      setMean(Double.parseDouble(tmpStr));
    } else {
      setMean(0.0);
    }

    tmpStr = Utils.getOption("SD", options);
    if (tmpStr.length() != 0) {
      setStandardDeviation(Double.parseDouble(tmpStr));
    } else {
      setStandardDeviation(1);
    }
  }

  /**
   * Gets the current settings of the generator.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> result = new Vector<String>();

    Collections.addAll(result, super.getOptions());

    result.add("-M");
    result.add("" + m_Mean);

    result.add("-SD");
    result.add("" + m_StandardDeviation);

    return result.toArray(new String[result.size()]);
  }

  /**
   * Gets the current mean of the underlying Gaussian distribution.
   * 
   * @return The current mean of the Gaussian distribution.
   */
  public double getMean() {
    return m_Mean;
  }

  /**
   * Sets the mean of the Gaussian distribution to a new mean.
   * 
   * @param value The new mean for the distribution.
   */
  @Override
  public void setMean(double value) {
    m_Mean = value;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String meanTipText() {
    return "The mean of the underlying distribution.";
  }

  /**
   * Gets the current standard deviation of the underlying distribution.
   * 
   * @return The current standard deviation of the distribution.
   */
  public double getStandardDeviation() {
    return m_StandardDeviation;
  }

  /**
   * Sets the standard deviation of the distribution to a new value.
   * 
   * @param value The new standard deviation.
   */
  @Override
  public void setStandardDeviation(double value) {
    if (value > 0) {
      m_StandardDeviation = value;
    } else {
      m_StandardDeviation = 0.01;
    }
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String standardDeviationTipText() {
    return "The standard deviation of the underlying distribution.";
  }
}
