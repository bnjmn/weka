/*
 *    Logger.java
 *    Copyright (C) 1999 Len Trigg
 *
 */


package weka.gui;

/** 
 * Interface for objects that display log (permanent historical) and
 * status (transient) messages.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public interface Logger {

  /**
   * Sends the supplied message to the log area. These message will typically
   * have the current timestamp prepended, and be viewable as a history.
   *
   * @param message the log message
   */
  public void logMessage(String message);
  
  /**
   * Sends the supplied message to the status line. These messages are
   * typically one-line status messages to inform the user of progress
   * during processing (i.e. it doesn't matter if the user doesn't happen
   * to look at each message)
   *
   * @param message the status message.
   */
  public void statusMessage(String message);
  
}
