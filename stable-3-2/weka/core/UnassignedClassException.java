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
 *    UnassignedClassException.java
 *    Copyright (C) 2001 Webmind Corp.
 */

package weka.core;

/**
 * <code>UnassignedClassException</code> is used when
 * a method requires access to the Attribute designated as 
 * the class attribute in a set of Instances, but the Instances does not
 * have any class attribute assigned (such as by setClassIndex()).
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.2 $
 */
public class UnassignedClassException extends RuntimeException {

  /**
   * Creates a new <code>UnassignedClassException</code> instance
   * with no detail message.
   */
  public UnassignedClassException() { 
    super(); 
  }

  /**
   * Creates a new <code>UnassignedClassException</code> instance
   * with a specified message.
   *
   * @param messagae a <code>String</code> containing the message.
   */
  public UnassignedClassException(String message) { 
    super(message); 
  }
}
