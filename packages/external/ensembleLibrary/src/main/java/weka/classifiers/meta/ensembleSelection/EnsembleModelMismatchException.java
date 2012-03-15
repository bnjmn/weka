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
 *    EnsembleModelMismatchException.java
 *    Copyright (C) 2006 Robert Jung
 *
 */

package weka.classifiers.meta.ensembleSelection;

/**
 * This excpetion gets thrown when a models don't match.
 * 
 * @author  Robert Jung (mrbobjung@gmail.com)
 * @version $Revision$
 */
public class EnsembleModelMismatchException
  extends Exception {
  
  /** for serialization */
  private static final long serialVersionUID = 4660917211181280739L;

  /**
   * constructor of the exception
   * 
   * @param s		the message for the exception
   */
  public EnsembleModelMismatchException(String s) {
    super(s);
  }
}
