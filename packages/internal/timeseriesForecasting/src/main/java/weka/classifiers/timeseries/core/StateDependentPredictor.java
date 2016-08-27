package weka.classifiers.timeseries.core;

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
 *    StateDependentPredictor.java
 *    Copyright (C) 2016 University of Waikato, Hamilton, New Zealand
 */

import weka.classifiers.Classifier;

/**
 * An interface for state-dependent predictors. Has methods for clearing,
 * setting and getting previous model state as well as for model/state
 * serialization. Useful for forecasters which use models which store state of
 * last prediction to be used in the next prediction.
 *
 * A state-dependent predictor is
 *
 * Created by pedrofale on 17-08-2016.
 */
public interface StateDependentPredictor extends Classifier {

  /**
   * Clear/reset state of the model.
   */
  void clearPreviousState();

  /**
   * Load state into model.
   *
   * @param previousState the state to set the model to
   */
  void setPreviousState(Object previousState);

  /**
   * Get the last set state of the model.
   *
   * @return the state of the model to be used in next prediction
   */
  Object getPreviousState();

  /**
   * Serialize model state
   *
   * @param path the path to the file to hold the serialized state
   * @throws Exception
   */
  void serializeState(String path) throws Exception;

  /**
   * Load serialized model state
   *
   * @param path the path to the file to load the serialized state from
   * @throws Exception
   */
  void loadSerializedState(String path) throws Exception;
}
