/*
 *    SysErrLog.java
 *    Copyright (C) 1999 Len Trigg
 *
 */


package weka.gui;

import java.text.SimpleDateFormat;
import java.util.Date;

/** 
 * This Logger just sends messages to System.err.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public class SysErrLog implements Logger {

  /**
   * Gets a string containing current date and time.
   *
   * @return a string containing the date and time.
   */
  protected static String getTimestamp() {

    return (new SimpleDateFormat("yyyy.MM.dd hh:mm:ss")).format(new Date());
  }

  /**
   * Sends the supplied message to the log area. The current timestamp will
   * be prepended.
   *
   * @param message a value of type 'String'
   */
  public void logMessage(String message) {
    
    System.err.println("LOG " + SysErrLog.getTimestamp() + ": "
		       + message);
  }

  /**
   * Sends the supplied message to the status line.
   *
   * @param message the status message
   */
  public void statusMessage(String message) {

    System.err.println("STATUS: " + message);
  }
}
