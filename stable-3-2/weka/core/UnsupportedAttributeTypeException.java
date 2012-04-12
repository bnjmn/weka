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
 *    UnsupportedAttributeTypeException.java
 *    Copyright (C) 2001 Webmind Corp.
 */

package weka.core;

/**
 * <code>UnsupportedAttributeTypeException</code> is used in situations
 * where the throwing object is not able to accept Instances with the
 * supplied structure, because one or more of the Attributes in the
 * Instances are of the wrong type.
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.2 $
 */
public class UnsupportedAttributeTypeException extends WekaException {

  /**
   * Creates a new <code>UnsupportedAttributeTypeException</code> instance
   * with no detail message.
   */
  public UnsupportedAttributeTypeException() { 
    super(); 
  }

  /**
   * Creates a new <code>UnsupportedAttributeTypeException</code> instance
   * with a specified message.
   *
   * @param messagae a <code>String</code> containing the message.
   */
  public UnsupportedAttributeTypeException(String message) { 
    super(message); 
  }
}
