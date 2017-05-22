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
 *    ActivationIdentity.java
 *    Copyright (C) 2017 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.dl4j.activations;

import org.nd4j.shade.jackson.annotation.JsonTypeName;
import weka.core.Option;
import weka.core.OptionHandler;

import java.util.Enumeration;

/**
 * A version of DeepLearning4j's ActivationIdentity that implements WEKA option handling.
 *
 * @author Eibe Frank
 *
 * @version $Revision: 11711 $
 */
@JsonTypeName("Identity")
public class ActivationIdentity extends org.nd4j.linalg.activations.impl.ActivationIdentity implements OptionHandler {

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

