/*
 *    RemoteExperimentEvent.java
 *    Copyright (C) 2000 Mark Hall
 *
 */


package weka.experiment;

import java.io.Serializable;
/**
 * Class encapsulating information on progress of a remote experiment
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public class RemoteExperimentEvent implements Serializable {

  /** A status type message */
  public boolean m_statusMessage;

  /** A log type message */
  public boolean m_logMessage;

  /** The message */
  public String m_messageString;

  /** True if a remote experiment has finished */
  public boolean m_experimentFinished;

  /**
   * Constructor
   * @param status true for status type messages
   * @param log true for log type messages
   * @param finished true if experiment has finished
   * @param message the message
   */
  public RemoteExperimentEvent(boolean status, boolean log, boolean finished,
			       String message) {
    m_statusMessage = status;
    m_logMessage = log;
    m_experimentFinished = finished;
    m_messageString = message;
  }
}
