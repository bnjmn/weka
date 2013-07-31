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
 *    RunPanel.java
 *    Copyright (C) 1999 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.experiment;

import weka.core.SerializedObject;
import weka.core.Utils;
import weka.experiment.Experiment;
import weka.experiment.RemoteExperiment;
import weka.experiment.RemoteExperimentEvent;
import weka.experiment.RemoteExperimentListener;
import weka.gui.LogPanel;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

/** 
 * This panel controls the running of an experiment.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision$
 */
public class RunPanel
  extends JPanel
  implements ActionListener {

  /** for serialization */
  private static final long serialVersionUID = 1691868018596872051L;

  /** The message displayed when no experiment is running */
  protected static final String NOT_RUNNING = Messages.getInstance().getString("RunPanel_NOT_RUNNING_Text");

  /** Click to start running the experiment */
  protected JButton m_StartBut = new JButton(Messages.getInstance().getString("RunPanel_StartBut_JButton_Text"));

  /** Click to signal the running experiment to halt */
  protected JButton m_StopBut = new JButton(Messages.getInstance().getString("RunPanel_StopBut_JButton_Text"));

  protected LogPanel m_Log = new LogPanel();

  /** The experiment to run */
  protected Experiment m_Exp;

  /** The thread running the experiment */
  protected Thread m_RunThread = null;

  /** A pointer to the results panel */
  protected ResultsPanel m_ResultsPanel = null;

  /*
   * A class that handles running a copy of the experiment
   * in a separate thread
   */
  class ExperimentRunner
    extends Thread
    implements Serializable {

    /** for serialization */
    private static final long serialVersionUID = -5591889874714150118L;

    Experiment m_ExpCopy;
    
    public ExperimentRunner(final Experiment exp) throws Exception {

      // Create a full copy using serialization
      if (exp == null) {
	System.err.println(Messages.getInstance().getString("RunPanel_ExperimentRunner_Error_Text_First"));
      } else {
	System.err.println(Messages.getInstance().getString("RunPanel_ExperimentRunner_Error_Text_Second") + exp.toString());
      }
      System.err.println(Messages.getInstance().getString("RunPanel_ExperimentRunner_Error_Text_Third"));
      SerializedObject so = new SerializedObject(exp);
      System.err.println(Messages.getInstance().getString("RunPanel_ExperimentRunner_Error_Text_Fourth"));
      m_ExpCopy = (Experiment) so.getObject();
      System.err.println(Messages.getInstance().getString("RunPanel_ExperimentRunner_Error_Text_Fifth"));
    }

    public void abortExperiment() {
      if (m_ExpCopy instanceof RemoteExperiment) {
	((RemoteExperiment)m_ExpCopy).abortExperiment();
	//	m_StartBut.setEnabled(true);
	m_StopBut.setEnabled(false);
	//	statusMessage(NOT_RUNNING);
      }
    }

    /**
     * Starts running the experiment.
     */
    public void run() {
      
      m_StartBut.setEnabled(false);
      m_StopBut.setEnabled(true);
      if (m_ResultsPanel != null) {
	m_ResultsPanel.setExperiment(null);
      }
      try {
	if (m_ExpCopy instanceof RemoteExperiment) {
	  // add a listener
	  System.err.println(Messages.getInstance().getString("RunPanel_ExperimentRunner_Run_Error_Text"));
	  ((RemoteExperiment)m_ExpCopy).
	    addRemoteExperimentListener(new RemoteExperimentListener() {
		public void remoteExperimentStatus(RemoteExperimentEvent e) {
		  if (e.m_statusMessage) {
		    statusMessage(e.m_messageString);
		  }
		  if (e.m_logMessage) {
		    logMessage(e.m_messageString);
		  }
		  if (e.m_experimentFinished) {
		    m_RunThread = null;
		    m_StartBut.setEnabled(true);
		    m_StopBut.setEnabled(false);
		    statusMessage(NOT_RUNNING);
		  }
		}
	      });
	}
	logMessage(Messages.getInstance().getString("RunPanel_ExperimentRunner_Run_LogMessage_Text_First"));
	statusMessage(Messages.getInstance().getString("RunPanel_ExperimentRunner_Run_StatusMessage_Text_First"));
	m_ExpCopy.initialize();
	int errors = 0;
	if (!(m_ExpCopy instanceof RemoteExperiment)) {
	  statusMessage(Messages.getInstance().getString("RunPanel_ExperimentRunner_Run_StatusMessage_Text_Second"));
	  while (m_RunThread != null && m_ExpCopy.hasMoreIterations()) {
	    try {
	      String current = Messages.getInstance().getString("RunPanel_ExperimentRunner_Run_Current_Text_First");
	      if (m_ExpCopy.getUsePropertyIterator()) {
		int cnum = m_ExpCopy.getCurrentPropertyNumber();
		String ctype = m_ExpCopy.getPropertyArray().getClass().getComponentType().getName();
		int lastDot = ctype.lastIndexOf('.');
		if (lastDot != -1) {
		  ctype = ctype.substring(lastDot + 1);
		}
		String cname = " " + ctype + "="
		  + (cnum + 1) + ":"
		  + m_ExpCopy.getPropertyArrayValue(cnum).getClass().getName();
		current += cname;
	      }
	      String dname = ((File) m_ExpCopy.getDatasets()
			      .elementAt(m_ExpCopy.getCurrentDatasetNumber()))
		.getName();
	      current += Messages.getInstance().getString("RunPanel_ExperimentRunner_Run_Current_Text_Second") + dname
		+ Messages.getInstance().getString("RunPanel_ExperimentRunner_Run_Current_Text_Third")  + (m_ExpCopy.getCurrentRunNumber());
	      statusMessage(current);
	      m_ExpCopy.nextIteration();
	    } catch (Exception ex) {
	      errors ++;
	      logMessage(ex.getMessage());
	      ex.printStackTrace();
	      boolean continueAfterError = false;
	      if (continueAfterError) {
		m_ExpCopy.advanceCounters(); // Try to keep plowing through
	      } else {
		m_RunThread = null;
	      }
	    }
	  }
	  statusMessage(Messages.getInstance().getString("RunPanel_ExperimentRunner_Run_StatusMessage_Text_Third"));
	  m_ExpCopy.postProcess();
	  if (m_RunThread == null) {
	    logMessage(Messages.getInstance().getString("RunPanel_ExperimentRunner_Run_LogMessage_Text_Third"));
	  } else {
	    logMessage(Messages.getInstance().getString("RunPanel_ExperimentRunner_Run_LogMessage_Text_Fourth"));
	  }
	  if (errors == 1) {
	    logMessage(Messages.getInstance().getString("RunPanel_ExperimentRunner_Run_LogMessage_Text_Fifth_Front") + errors + " " + Messages.getInstance().getString("RunPanel_ExperimentRunner_Run_LogMessage_Text_Fifth_End"));
	  } else {
	    logMessage(Messages.getInstance().getString("RunPanel_ExperimentRunner_Run_LogMessage_Text_Sixth_Front") + errors + " " + Messages.getInstance().getString("RunPanel_ExperimentRunner_Run_LogMessage_Text_Sixth_End"));
	  }
	  statusMessage(NOT_RUNNING);
	} else {
	  statusMessage(Messages.getInstance().getString("RunPanel_ExperimentRunner_Run_StatusMessage_Text_Fourth"));
	  ((RemoteExperiment)m_ExpCopy).runExperiment();
	}
      } catch (Exception ex) {
	ex.printStackTrace();
	System.err.println(ex.getMessage());
	statusMessage(ex.getMessage());
      } finally {
	if (m_ResultsPanel != null) {
	  m_ResultsPanel.setExperiment(m_ExpCopy);
	}
	if (!(m_ExpCopy instanceof RemoteExperiment)) {
	  m_RunThread = null;
	  m_StartBut.setEnabled(true);
	  m_StopBut.setEnabled(false);
	  System.err.println(Messages.getInstance().getString("RunPanel_ExperimentRunner_Run_Error_Text"));
	}
      }
    }
  }

  /**
   * Sets the pointer to the results panel.
   */
  public void setResultsPanel(ResultsPanel rp) {

    m_ResultsPanel = rp;
  }
  
  /**
   * Creates the run panel with no initial experiment.
   */
  public RunPanel() {

    m_StartBut.addActionListener(this);
    m_StopBut.addActionListener(this);
    m_StartBut.setEnabled(false);
    m_StopBut.setEnabled(false);
    m_StartBut.setMnemonic('S');
    m_StopBut.setMnemonic('t');
    m_Log.statusMessage(NOT_RUNNING);

    // Set the GUI layout
    JPanel controls = new JPanel();
    GridBagLayout gb = new GridBagLayout();
    GridBagConstraints constraints = new GridBagConstraints();
    controls.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
    //    controls.setLayout(new GridLayout(1,2));
    controls.setLayout(gb);
    constraints.gridx=0;constraints.gridy=0;constraints.weightx=5;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.gridwidth=1;constraints.gridheight=1;
    constraints.insets = new Insets(0,2,0,2);
    controls.add(m_StartBut,constraints);
    constraints.gridx=1;constraints.gridy=0;constraints.weightx=5;
    constraints.gridwidth=1;constraints.gridheight=1;
    controls.add(m_StopBut,constraints);
    setLayout(new BorderLayout());
    add(controls, BorderLayout.NORTH);
    add(m_Log, BorderLayout.CENTER);
  }
    
  /**
   * Creates the panel with the supplied initial experiment.
   *
   * @param exp a value of type 'Experiment'
   */
  public RunPanel(Experiment exp) {

    this();
    setExperiment(exp);
  }

  /**
   * Sets the experiment the panel operates on.
   *
   * @param exp a value of type 'Experiment'
   */
  public void setExperiment(Experiment exp) {
    
    m_Exp = exp;
    m_StartBut.setEnabled(m_RunThread == null);
    m_StopBut.setEnabled(m_RunThread != null);
  }
  
  /**
   * Controls starting and stopping the experiment.
   *
   * @param e a value of type 'ActionEvent'
   */
  public void actionPerformed(ActionEvent e) {

    if (e.getSource() == m_StartBut) {
      if (m_RunThread == null) {
        boolean proceed = true;
        if (Experimenter.m_Memory.memoryIsLow()) {
          proceed = Experimenter.m_Memory.showMemoryIsLow();
        }
        if (proceed) {
          try {
            m_RunThread = new ExperimentRunner(m_Exp);
            m_RunThread.setPriority(Thread.MIN_PRIORITY); // UI has most priority
            m_RunThread.start();
          } catch (Exception ex) {
            ex.printStackTrace();
            logMessage(Messages.getInstance().getString("RunPanel_ExperimentRunner_ActionPerformed_LogMessage_Text_First")
                + ex.getMessage());
          }
        }
      }
    } else if (e.getSource() == m_StopBut) {
      m_StopBut.setEnabled(false);
      logMessage(Messages.getInstance().getString("RunPanel_ExperimentRunner_ActionPerformed_LogMessage_Text_Second"));
      if (m_Exp instanceof RemoteExperiment) {
	logMessage(Messages.getInstance().getString("RunPanel_ExperimentRunner_ActionPerformed_LogMessage_Text_Third"));
      }
      ((ExperimentRunner)m_RunThread).abortExperiment();
      // m_RunThread.stop() ??
      m_RunThread = null;
    }
  }
  
  /**
   * Sends the supplied message to the log panel log area.
   *
   * @param message the message to log
   */
  protected void logMessage(String message) {

    m_Log.logMessage(message);
  }

  /**
   * Sends the supplied message to the log panel status line.
   *
   * @param message the status message
   */
  protected void statusMessage(String message) {
    
    m_Log.statusMessage(message);
  }
  
  /**
   * Tests out the run panel from the command line.
   *
   * @param args may contain options specifying an experiment to run.
   */
  public static void main(String [] args) {

    try {
      boolean readExp = Utils.getFlag('l', args);
      final String expFile = Utils.getOption('f', args);
      if (readExp && (expFile.length() == 0)) {
	throw new Exception(Messages.getInstance().getString("RunPanel_ExperimentRunner_Main_Exception_Text"));
      }
      Experiment exp = null;
      if (readExp) {
	FileInputStream fi = new FileInputStream(expFile);
	ObjectInputStream oi = new ObjectInputStream(
			       new BufferedInputStream(fi));
	Object to = oi.readObject();
	if (to instanceof RemoteExperiment) {
	  exp = (RemoteExperiment)to;
	} else {
	  exp = (Experiment)to;
	}
	oi.close();
      } else {
	exp = new Experiment();
      }
      System.err.println(Messages.getInstance().getString("RunPanel_ExperimentRunner_Main_Error_Text_First") + exp.toString());
      final JFrame jf = new JFrame(Messages.getInstance().getString("RunPanel_ExperimentRunner_Main_JFrame_Text"));
      jf.getContentPane().setLayout(new BorderLayout());
      final RunPanel sp = new RunPanel(exp);
      //sp.setBorder(BorderFactory.createTitledBorder("Setup"));
      jf.getContentPane().add(sp, BorderLayout.CENTER);
      jf.addWindowListener(new WindowAdapter() {
	public void windowClosing(WindowEvent e) {
	  System.err.println(Messages.getInstance().getString("RunPanel_ExperimentRunner_Main_Error_Text_Second")
			     + sp.m_Exp.toString());
	  jf.dispose();
	  System.exit(0);
	}
      });
      jf.pack();
      jf.setVisible(true);
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
