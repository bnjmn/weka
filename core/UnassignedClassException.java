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
 * @version $Revision: 1.1 $
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
