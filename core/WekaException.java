/*
 *    WekaException.java
 *    Copyright (C) 2001 Webmind Corp.
 */

package weka.core;

/**
 * <code>WekaException</code> is used when some Weka-specific
 * checked exception must be raised.
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.1 $
 */
public class WekaException extends Exception {

  /**
   * Creates a new <code>WekaException</code> instance
   * with no detail message.
   */
  public WekaException() { 
    super(); 
  }

  /**
   * Creates a new <code>WekaException</code> instance
   * with a specified message.
   *
   * @param messagae a <code>String</code> containing the message.
   */
  public WekaException(String message) { 
    super(message); 
  }
}
