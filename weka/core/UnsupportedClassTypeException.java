/*
 *    UnsupportedClassTypeException.java
 *    Copyright (C) 2001 Webmind Corp.
 */

package weka.core;

/**
 * <code>UnsupportedClassTypeException</code> is used in situations
 * where the throwing object is not able to accept Instances with the
 * supplied structure, because the class Attribute is of the wrong type.
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.1 $
 */
public class UnsupportedClassTypeException extends WekaException {

  /**
   * Creates a new <code>UnsupportedClassTypeException</code> instance
   * with no detail message.
   */
  public UnsupportedClassTypeException() { 
    super(); 
  }

  /**
   * Creates a new <code>UnsupportedClassTypeException</code> instance
   * with a specified message.
   *
   * @param messagae a <code>String</code> containing the message.
   */
  public UnsupportedClassTypeException(String message) { 
    super(message); 
  }
}
