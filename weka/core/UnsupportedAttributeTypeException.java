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
 * @version $Revision: 1.1 $
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
