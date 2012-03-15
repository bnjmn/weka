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
 *    ZeroOneLossFunction.java
 *    Copyright (C) 2004 Stijn Lievens
 *
 */

package weka.classifiers.misc.monotone;

import weka.core.RevisionHandler;
import weka.core.RevisionUtils;

/**
 * Class implementing the zero-one loss function, this is 
 * an incorrect prediction always accounts for one unit loss.
 *
 * <p>
 * This implementation is done as part of the master's thesis: "Studie
 * en implementatie van instantie-gebaseerde algoritmen voor gesuperviseerd
 * rangschikken", Stijn Lievens, Ghent University, 2004. 
 * </p>
 * 
 * @author Stijn Lievens (stijn.lievens@ugent.be)
 * @version $Revision$
 */
public class ZeroOneLossFunction
  implements NominalLossFunction, RevisionHandler {

  /**
   * Returns the zero-one loss function between two class values.
   * 
   * @param actual the actual class value
   * @param predicted the predicted class value
   * @return 1 if the actual and predicted value differ, 0 otherwise
   */
  public final double loss(double actual, double predicted) {
    return actual == predicted ? 0 : 1;
  }

  /**
   * Returns a string with the name of the loss function.
   *
   * @return a string with the name of the loss function
   */
  public String toString() {
    return "ZeroOneLossFunction";
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }
}
