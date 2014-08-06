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
 *    UpdateableBatchProcessor.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers;

/**
 * Updateable classifiers can implement this if they wish to be informed at the
 * end of the training stream. This could be useful for cleaning up temporary
 * data structures, pruning dictionaries etc.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public interface UpdateableBatchProcessor {

  /**
   * Signal that the training data is finished (for now).
   * 
   * @throws Exception if a problem occurs
   */
  public void batchFinished() throws Exception;
}
