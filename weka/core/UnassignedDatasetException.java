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
 *    UnassignedDatasetException.java
 *    Copyright (C) 2001 Webmind Corp.
 */

package weka.core;

/**
 * <code>UnassignedDatasetException</code> is used when
 * a method of an Instance is called that requires access to
 * the Instance structure, but that the Instance does not contain
 * a reference to any Instances (as set by Instance.setDataset(), or when
 * an Instance is added to a set of Instances)).
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.2 $
 */
public class UnassignedDatasetException extends RuntimeException {

  /**
   * Creates a new <code>UnassignedDatasetException</code> instance
   * with no detail message.
   */
  public UnassignedDatasetException() { 
    super(); 
  }

  /**
   * Creates a new <code>UnassignedDatasetException</code> instance
   * with a specified message.
   *
   * @param messagae a <code>String</code> containing the message.
   */
  public UnassignedDatasetException(String message) { 
    super(message); 
  }
}
