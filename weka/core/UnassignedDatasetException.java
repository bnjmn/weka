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
 * @version $Revision: 1.1 $
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
