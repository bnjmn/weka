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
 *    BinomialDistribution.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.dl4j.distribution;

import org.nd4j.shade.jackson.annotation.JsonTypeName;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;

import java.util.Enumeration;

/**
 * A version of DeepLearning4j's BinomialDistribution that implements WEKA option handling.
 * Currently only allows one trial.
 *
 * @author Eibe Frank
 *
 * @version $Revision: 11711 $
 */
@JsonTypeName("binomial")
public class BinomialDistribution extends org.deeplearning4j.nn.conf.distribution.BinomialDistribution implements OptionHandler {

  /**
   * Constructs binomial distribution with 1 trial and success probability 0.5.
   */
  public BinomialDistribution() {
    super(1, 0.5);
  }

  @OptionMetadata(
          displayName = "probability of success",
          description = "The probability of success (default = 0.5).",
          commandLineParamName = "prob", commandLineParamSynopsis = "-prob <double>",
          displayOrder = 1)
  public double getProbabilityOfSuccess() {
    return super.getProbabilityOfSuccess();
  }
  public void setProbabilityOfSuccess(double probabilityOfSuccess) {
    super.setProbabilityOfSuccess(probabilityOfSuccess);
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

