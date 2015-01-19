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
 *    SemanticException.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.expressionlanguage.core;

/**
 * An exception that should be used if a program doesn't have valid semantics
 * 
 * @author Benjamin Weber ( benweber at student dot ethz dot ch )
 * @version $Revision: 1000 $
 */
public class SemanticException extends Exception {

  /** for serialization */
  private static final long serialVersionUID = -1212116135142845573L;

  /**
   * Constructs a {@link SemanticException} with a message and cause
   * 
   * @param msg the message of the exception
   * @param e the cause of the exception
   */
  public SemanticException(String msg, Exception e) {
    super(msg, e);
  }

  /**
   * Constructs a {@link SemanticException} with a message
   * 
   * @param msg the message of the exception
   */
  public SemanticException(String msg) {
    super(msg);
  }
}
