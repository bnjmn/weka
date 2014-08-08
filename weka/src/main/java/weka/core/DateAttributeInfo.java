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
 *    DateAttributeInfo.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.core;

import java.text.SimpleDateFormat;

/**
 * Stores information for date attributes.
 */
public class DateAttributeInfo implements AttributeInfo {

  /** Date format specification for date attributes */
  protected SimpleDateFormat m_DateFormat;

  /**
   * Constructs info based on argument.
   */
  public DateAttributeInfo(String dateFormat) {
    if (dateFormat != null) {
      m_DateFormat = new SimpleDateFormat(dateFormat);
    } else {
      m_DateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    }
    m_DateFormat.setLenient(false);
  }
}