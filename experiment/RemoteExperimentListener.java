/*
 *    RemoteExperimentListener.java
 *    Copyright (C) 2000 Mark Hall
 *
 */


package weka.experiment;

/**
 * Interface for classes that want to listen for updates on RemoteExperiment
 * progress
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public interface RemoteExperimentListener {

  /**
   * Called when progress has been made in a remote experiment
   * @param e the event encapsulating what happened
   */
  public void remoteExperimentStatus(RemoteExperimentEvent e);

}
