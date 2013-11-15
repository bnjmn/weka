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
 *    DistributedWekaException.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed;

import weka.core.WekaException;

/**
 * Exception class for use when errors arise in distributed applications
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class DistributedWekaException extends WekaException {

  /** For serialization */
  private static final long serialVersionUID = -4677106502290302516L;

  /**
   * Constructor
   */
  public DistributedWekaException() {
    super();
  }

  /**
   * Constructor with message argument
   * 
   * @param message the message for the exception
   */
  public DistributedWekaException(String message) {
    super(message);
  }

  /**
   * Constructor with message and cause
   * 
   * @param message the message for the exception
   * @param cause the root cause Throwable
   */
  public DistributedWekaException(String message, Throwable cause) {
    this(message);
    initCause(cause);
    fillInStackTrace();
  }

  /**
   * Constructor with cause argument
   * 
   * @param cause the root cause Throwable
   */
  public DistributedWekaException(Throwable cause) {
    super(cause.getMessage());
    initCause(cause);
    fillInStackTrace();
  }
}
