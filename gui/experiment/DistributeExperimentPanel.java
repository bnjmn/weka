/*
 *    DistributeExperimentPanel.java
 *    Copyright (C) 2000 Mark Hall
 *
 */


package weka.gui.experiment;

import weka.experiment.Experiment;
import weka.experiment.RemoteExperiment;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.SwingConstants;
import javax.swing.JCheckBox;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.Box;
import javax.swing.BoxLayout;

/** 
 * This panel enables an experiment to be distributed to multiple hosts;
 * it also allows remote host names to be specified.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */

public class DistributeExperimentPanel extends JPanel {
  
  /** Distribute the current experiment to remote hosts */
  protected JCheckBox m_enableDistributedExperiment = 
    new JCheckBox();

  /** Popup the HostListPanel */
  protected JButton m_configureHostNames = new JButton("Hosts");

  /** The host list panel */
  protected HostListPanel m_hostList = new HostListPanel();

  /**
   * Constructor
   */
  public DistributeExperimentPanel() {
    m_enableDistributedExperiment.setSelected(false);
    m_enableDistributedExperiment.
      setToolTipText("Allow this experiment to be distributed to remote hosts");
    m_enableDistributedExperiment.setEnabled(false);
    m_configureHostNames.setEnabled(false);
    m_configureHostNames.setToolTipText("Edit the list of remote hosts");
    
    m_enableDistributedExperiment.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  m_configureHostNames.setEnabled(m_enableDistributedExperiment.
					  isSelected());
	}
      });

    m_configureHostNames.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  popupHostPanel();
	}
      });
    
    setLayout(new BorderLayout());
    setBorder(BorderFactory.createTitledBorder("Distribute experiment"));
    add(m_enableDistributedExperiment, BorderLayout.WEST);
    add(m_configureHostNames, BorderLayout.CENTER);
  }

  /**
   * Creates the panel with the supplied initial experiment.
   *
   * @param exp a value of type 'Experiment'
   */
  public DistributeExperimentPanel(Experiment exp) {
    this();
    setExperiment(exp);
  }

  /**
   * Sets the experiment to be configured.
   *
   * @param exp a value of type 'Experiment'
   */
  public void setExperiment(Experiment exp) {
    
    m_enableDistributedExperiment.setEnabled(true);
    if (exp instanceof RemoteExperiment) {
      m_enableDistributedExperiment.setSelected(true);
      m_configureHostNames.setEnabled(true);
      m_hostList.setExperiment((RemoteExperiment)exp);
    }
  }

  /**
   * Pop up the host list panel
   */
  private void popupHostPanel() {
    try {
      final JFrame jf = new JFrame("Edit host names");
      
      jf.getContentPane().setLayout(new BorderLayout());
      jf.getContentPane().add(m_hostList,
			      BorderLayout.CENTER);
      jf.addWindowListener(new WindowAdapter() {
	  public void windowClosing(WindowEvent e) {
	    jf.dispose();
	  }
	});
      jf.pack();
      jf.setVisible(true);
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }

  /**
   * Returns true if the distribute experiment checkbox is selected
   * @return true if the user has opted for distributing the experiment
   */
  public boolean distributedExperimentSelected() {
    return m_enableDistributedExperiment.isSelected();
  }

  /**
   * Enable objects to listen for changes to the check box
   * @param al an ActionListener
   */
  public void addCheckBoxActionListener(ActionListener al) {
    m_enableDistributedExperiment.addActionListener(al);
  }

  /**
   * Tests out the panel from the command line.
   *
   * @param args ignored.
   */
  public static void main(String [] args) {
    try {
      final JFrame jf = new JFrame("DistributeExperiment");
      jf.getContentPane().setLayout(new BorderLayout());
      jf.getContentPane().add(new DistributeExperimentPanel(new Experiment()),
			      BorderLayout.CENTER);
      jf.addWindowListener(new WindowAdapter() {
	public void windowClosing(WindowEvent e) {
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
