
package weka.gui.experiment;

import weka.experiment.Experiment;
import weka.core.Utils;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.BufferedInputStream;
import javax.swing.SwingConstants;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;


public class RunPanel extends JPanel implements ActionListener {

  protected final static String NOT_RUNNING = "Not running";
  
  protected JButton m_StartBut = new JButton("Start");
  protected JButton m_StopBut = new JButton("Stop");
  protected JLabel m_StatusLab = new JLabel(NOT_RUNNING,
					    SwingConstants.CENTER);
  protected JTextArea m_ErrorLog = new JTextArea();

  protected Experiment m_Exp;
  protected Thread m_RunThread = null;

  /*
   * A class that handles running the experiment
   * in a separate thread
   */
  class ExperimentRunner extends Thread {

    Experiment m_ExpCopy;
    
    public ExperimentRunner(final Experiment exp) throws Exception {

      // Create a full copy using serialization
      if (exp == null) {
	System.err.println("Null experiment!!!");
      } else {
	System.err.println("Running experiment: " + exp.toString());
      }
      ByteArrayOutputStream bo = new ByteArrayOutputStream();
      BufferedOutputStream bbo = new BufferedOutputStream(bo);
      ObjectOutputStream oo = new ObjectOutputStream(bbo);
      System.err.println("Writing experiment copy");
      oo.writeObject(exp);
      oo.close();
      
      System.err.println("Reading experiment copy");
      ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
      BufferedInputStream bbi = new BufferedInputStream(bi);
      ObjectInputStream oi = new ObjectInputStream(bbi);
      m_ExpCopy = (Experiment) oi.readObject();
      oi.close();
      System.err.println("Made experiment copy");
    }
    
    /**
     * Starts running the main method.
     */
    public void run() {
      
      m_StartBut.setEnabled(false);
      m_StopBut.setEnabled(true);
      try {
	logMessage("Started");
	System.err.println("Initializing...");
	m_ExpCopy.initialize();
	int errors = 0;
	System.err.println("Iterating...");
	while (m_RunThread != null && m_ExpCopy.hasMoreIterations()) {
	  try {
	    String current = "Iteration: CustomValue="
	      + (m_ExpCopy.getCurrentCustomNumber() + 1)
	      + " DatasetNumber=" + (m_ExpCopy.getCurrentDatasetNumber() + 1)
	      + " RunNumber=" + (m_ExpCopy.getCurrentRunNumber());
	    m_StatusLab.setText(current);
	    m_ExpCopy.nextIteration();
	  } catch (Exception ex) {
	    errors ++;
	    logMessage(ex.getMessage());
	    boolean continueAfterError = false;
	    if (continueAfterError) {
	      m_ExpCopy.advanceCounters(); // Try to keep plowing through
	    } else {
	      m_RunThread = null;
	    }
	  }
	}
	System.err.println("Postprocessing...");
	m_ExpCopy.postProcess();
	if (m_RunThread == null) {
	  logMessage("Interrupted");
	} else {
	  logMessage("Finished");
	}
	if (errors == 1) {
	  logMessage("There was " + errors + " error");
	} else {
	  logMessage("There were " + errors + " errors");
	}
	m_StatusLab.setText(NOT_RUNNING);
      } catch (Exception ex) {
	ex.printStackTrace();
	System.err.println(ex.getMessage());
	m_StatusLab.setText(ex.getMessage());
      } finally {
	m_RunThread = null;
	m_StartBut.setEnabled(true);
	m_StopBut.setEnabled(false);
      }
      System.err.println("Done...");
    }
  }

  
  public RunPanel() {

    m_StartBut.addActionListener(this);
    m_StopBut.addActionListener(this);
    m_StartBut.setEnabled(false);
    m_StopBut.setEnabled(false);
    m_StatusLab.setBorder(BorderFactory.createCompoundBorder(
			  BorderFactory.createTitledBorder("Status"),
			  BorderFactory.createEmptyBorder(0, 5, 5, 5)));
    m_ErrorLog.setEditable(false);
    m_ErrorLog.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    
    JPanel controls = new JPanel();
    controls.setLayout(new GridLayout(1,2));
    controls.add(m_StartBut);
    controls.add(m_StopBut);

    JPanel output = new JPanel();
    output.setLayout(new BorderLayout());
    output.add(m_StatusLab, BorderLayout.NORTH);
    JScrollPane logOut = new JScrollPane(m_ErrorLog);
    logOut.setBorder(BorderFactory.createTitledBorder("Log"));
    output.add(logOut, BorderLayout.CENTER);

    setLayout(new BorderLayout());
    add(controls, BorderLayout.NORTH);
    add(output, BorderLayout.CENTER);
  }
    
  public RunPanel(Experiment exp) {

    this();
    setExperiment(exp);
  }

  public void setExperiment(Experiment exp) {
    
    m_Exp = exp;
    m_StartBut.setEnabled(true);
    m_StopBut.setEnabled(false);
    m_RunThread = null;
  }
  
  public void actionPerformed(ActionEvent e) {

    if (e.getSource() == m_StartBut) {
      if (m_RunThread == null) {
	try {
	  m_RunThread = new ExperimentRunner(m_Exp);
	  m_RunThread.setPriority(Thread.MIN_PRIORITY); // UI has most priority
	  m_RunThread.start();
	} catch (Exception ex) {
	  logMessage("Problem creating experiment copy to run: "
		     + ex.getMessage());
	}
      }
    } else if (e.getSource() == m_StopBut) {
      m_StopBut.setEnabled(false);
      m_RunThread = null;
    }
  }
  
  /**
   * Gets a string containing current date and time.
   *
   * @return a string containing the date and time.
   */
  public static String getTimestamp() {

    return (new SimpleDateFormat("yyyy.MM.dd hh:mm:ss")).format(new Date());
  }

  public void logMessage(String message) {
    
    m_ErrorLog.append(RunPanel.getTimestamp() + ' ' + message + '\n');
  }
  
  /**
   * Tests out the classifier editor from the command line.
   *
   * @param args may contain the class name of a classifier to edit
   */
  public static void main(String [] args) {

    try {
      boolean readExp = Utils.getFlag('l', args);
      final String expFile = Utils.getOption('f', args);
      if (readExp && (expFile.length() == 0)) {
	throw new Exception("A filename must be given with the -f option");
      }
      Experiment exp = null;
      if (readExp) {
	FileInputStream fi = new FileInputStream(expFile);
	ObjectInputStream oi = new ObjectInputStream(
			       new BufferedInputStream(fi));
	exp = (Experiment)oi.readObject();
	oi.close();
      } else {
	exp = new Experiment();
      }
      System.err.println("Initial Experiment:\n" + exp.toString());
      final JFrame jf = new JFrame("Run Weka Experiment");
      jf.getContentPane().setLayout(new BorderLayout());
      final RunPanel sp = new RunPanel(exp);
      //sp.setBorder(BorderFactory.createTitledBorder("Setup"));
      jf.getContentPane().add(sp, BorderLayout.CENTER);
      jf.addWindowListener(new WindowAdapter() {
	public void windowClosing(WindowEvent e) {
	  System.err.println("\nExperiment Configuration\n"
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
