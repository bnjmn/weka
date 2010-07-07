/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    AbsoluteLossFunction.java
 *    Copyright (C) 2004 Stijn Lievens
 *
 */

package weka.classifiers.misc.monotone;

import weka.core.RevisionHandler;
import weka.core.RevisionUtils;

/**
 * Class implementing the absolute loss function, this means 
 * the returned loss is the abolute value of the difference
 * between the predicted and actual value.
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
public class AbsoluteLossFunction
  implements NominalLossFunction, RevisionHandler {

  /**
   * Returns the absolute loss function between two class values.
   * 
   * @param actual the actual class value
   * @param predicted the predicted class value
   * @return the absolute value of the difference between the actual 
   * and predicted value
   */
  public final double loss(double actual, double predicted) {
    return Math.abs(actual - predicted);
  }

  /**
   * Returns a string with the name of the loss function.
   *
   * @return a string with the name of the loss function
   */
  public String toString() {
    return "AbsoluteLossFunction";
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
