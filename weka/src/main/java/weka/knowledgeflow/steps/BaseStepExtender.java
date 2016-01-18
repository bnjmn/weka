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
 *    BaseStepExtender.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.WekaException;
import weka.knowledgeflow.Data;

import java.util.List;

/**
 * A minimal set of methods, duplicated from the Step interface, that a simple
 * subclass of BaseStep would need to implement in order to function as a start
 * and/or main processing step in the Knowledge Flow.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 * @see Step
 * @see BaseStep
 */
public interface BaseStepExtender {

  /**
   * Initialize the step.
   *
   * @throws WekaException if a problem occurs during initialization
   */
  void stepInit() throws WekaException;

  /**
   * Get a list of incoming connection types that this step can accept. Ideally
   * (and if appropriate), this should take into account the state of the step
   * and any existing incoming connections. E.g. a step might be able to accept
   * one (and only one) incoming batch data connection.
   *
   * @return a list of incoming connections that this step can accept given its
   *         current state
   */
  List<String> getIncomingConnectionTypes();

  /**
   * Get a list of outgoing connection types that this step can produce. Ideally
   * (and if appropriate), this should take into account the state of the step
   * and the incoming connections. E.g. depending on what incoming connection is
   * present, a step might be able to produce a trainingSet output, a testSet
   * output or neither, but not both.
   *
   * @return a list of outgoing connections that this step can produce
   */
  List<String> getOutgoingConnectionTypes();

  /**
   * Start executing (if this component is a start point). Either this method,
   * processIncoming(), or both must be implemented.
   *
   * @throws WekaException if a problem occurs
   */
  void start() throws WekaException;

  /**
   * Process an incoming data payload (if the step accepts incoming
   * connections). Either this method, start(), or both must be implemented.
   *
   * @param data the payload to process
   * @throws WekaException if a problem occurs
   */
  void processIncoming(Data data) throws WekaException;
}
