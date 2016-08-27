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
 *    BaseModelSerializer.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.timeseries.core;

import weka.classifiers.Classifier;

/**
 * An interface for predictors which implement methods for serializing the base
 * model.
 *
 * Created by pedro on 25-08-2016.
 */
public interface BaseModelSerializer extends Classifier {

  /**
   * Serialize model
   *
   * @param path the path to the file to hold the serialized base learner
   * @throws Exception
   */
  void serializeModel(String path) throws Exception;

  /**
   * De-serialize model
   *
   * @param path the path to the file to load the serialized base learner from
   * @throws Exception
   */
  void loadSerializedModel(String path) throws Exception;

}
