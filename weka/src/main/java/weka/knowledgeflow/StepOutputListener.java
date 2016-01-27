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
 *    StepOutputListener
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow;

import weka.core.WekaException;

/**
 * Inteface to something that listens to the output from a {@code Step}
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public interface StepOutputListener {

  /**
   * Process data produced by a knowledge flow step
   * 
   * @param data the payload to process
   * @return true if processing was successful
   * @throws WekaException in the case of a catastrophic failure of the
   *           StepOutputListener
   */
  boolean dataFromStep(Data data) throws WekaException;
}
