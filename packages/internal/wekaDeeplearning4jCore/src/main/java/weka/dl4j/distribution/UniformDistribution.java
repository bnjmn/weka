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
 *    UniformDistribution.java
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
 * A version of DeepLearning4j's UniformDistribution that implements WEKA option handling.
 *
 * @author Eibe Frank
 *
 * @version $Revision: 11711 $
 */
@JsonTypeName("uniform")
public class UniformDistribution extends org.deeplearning4j.nn.conf.distribution.UniformDistribution implements OptionHandler {

  /**
   * Constructions normal distribution with lower limit -1 and upper limit 1.
   */
  public UniformDistribution() {
    super(-1.0, 1.0);
  }

  @OptionMetadata(
          displayName = "lower limit",
          description = "The lower limit (default = -1.0).",
          commandLineParamName = "lower", commandLineParamSynopsis = "-lower <double>",
          displayOrder = 1)
  public double getLower() { return super.getLower(); }
  public void setLower(double mean) {
    super.setLower(mean);
  }


  @OptionMetadata(
          displayName = "upper limit",
          description = "The upper limit (default = 1.0).",
          commandLineParamName = "upper", commandLineParamSynopsis = "-upper <double>",
          displayOrder = 2)
  public double getUpper() { return super.getUpper(); }
  public void setUpper(double std) {
    super.setUpper(std);
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

