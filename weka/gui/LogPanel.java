/*
 *    LogPanel.java
 *    Copyright (C) 1999 Len Trigg
 *
 */


package weka.gui;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import javax.swing.SwingConstants;
import javax.swing.JViewport;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Point;

/** 
 * This panel allows log and status messages to be posted. Log messages
 * appear in a scrollable text area, and status messages appear as one-line
 * transient messages.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.9 $
 */
public class LogPanel extends JPanel implements Logger, TaskLogger {

  /** Displays the current status */
  protected JLabel m_StatusLab = new JLabel("OK");
  
  /** Displays the log messages */
  protected JTextArea m_LogText = new JTextArea(4, 20);

  /** An indicator for whether text has been output yet */
  protected boolean m_First = true;

  /** The panel for monitoring the number of running tasks (if supplied)*/
  protected WekaTaskMonitor m_TaskMonitor=null;
  
  /**
   * Creates the log panel
   */
  public LogPanel() {
    m_LogText.setEditable(false);
    m_LogText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    m_StatusLab.setBorder(BorderFactory.createCompoundBorder(
			  BorderFactory.createTitledBorder("Status"),
			  BorderFactory.createEmptyBorder(0, 5, 5, 5)));    
    JPanel p1 = new JPanel();
    p1.setBorder(BorderFactory.createTitledBorder("Log"));
    p1.setLayout(new BorderLayout());
    final JScrollPane js = new JScrollPane(m_LogText);
    p1.add(js, BorderLayout.CENTER);
    js.getViewport().addChangeListener(new ChangeListener() {
      private int lastHeight;
      public void stateChanged(ChangeEvent e) {
	JViewport vp = (JViewport)e.getSource();
	int h = vp.getViewSize().height; 
	if (h != lastHeight) { // i.e. an addition not just a user scrolling
	  lastHeight = h;
	  int x = h - vp.getExtentSize().height;
	  vp.setViewPosition(new Point(0, x));
	}
      }
    });
    setLayout(new BorderLayout());
    add(p1, BorderLayout.CENTER);
    add(m_StatusLab, BorderLayout.SOUTH);
    addPopup();
  }

  /**
   * Creates the log panel
   */
  public LogPanel(WekaTaskMonitor tm) {
    /*    if (!(tm instanceof java.awt.Component)) {
      throw new Exception("TaskLogger must be a graphical component");
      } */
    m_TaskMonitor = tm;
    m_LogText.setEditable(false);
    m_LogText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    m_StatusLab.setBorder(BorderFactory.createCompoundBorder(
			  BorderFactory.createTitledBorder("Status"),
			  BorderFactory.createEmptyBorder(0, 5, 5, 5)));    
    JPanel p1 = new JPanel();
    p1.setBorder(BorderFactory.createTitledBorder("Log"));
    p1.setLayout(new BorderLayout());
    final JScrollPane js = new JScrollPane(m_LogText);
    p1.add(js, BorderLayout.CENTER);
    js.getViewport().addChangeListener(new ChangeListener() {
      private int lastHeight;
      public void stateChanged(ChangeEvent e) {
	JViewport vp = (JViewport)e.getSource();
	int h = vp.getViewSize().height; 
	if (h != lastHeight) { // i.e. an addition not just a user scrolling
	  lastHeight = h;
	  int x = h - vp.getExtentSize().height;
	  vp.setViewPosition(new Point(0, x));
	}
      }
    });
    setLayout(new BorderLayout());
    add(p1, BorderLayout.CENTER);
    JPanel p2 = new JPanel();
    p2.setLayout(new BorderLayout());
    p2.add(m_StatusLab,BorderLayout.CENTER);
    p2.add((java.awt.Component)m_TaskMonitor, BorderLayout.EAST);
    add(p2, BorderLayout.SOUTH);
    addPopup();
  }

  /**
   * Add a popup menu for displaying the amount of free memory
   * and running the garbage collector
   */
  private void addPopup() {
    addMouseListener(new MouseAdapter() {
	public void mouseClicked(MouseEvent e) {
	  if ((e.getModifiers() & InputEvent.BUTTON1_MASK)
	      == InputEvent.BUTTON1_MASK) {
	  } else {
	    JPopupMenu gcMenu = new JPopupMenu();
	    JMenuItem availMem = new JMenuItem("Available memory");
	    availMem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent ee) {
		  System.gc();
		  Runtime currR = Runtime.getRuntime();
		  long freeM = currR.freeMemory();
		  logMessage("Available memory : "+freeM+" bytes");
		}
	      });
	    gcMenu.add(availMem);
	    JMenuItem runGC = new JMenuItem("Run garabage collector");
	    runGC.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent ee) {
		  statusMessage("Running garbage collector");
		  System.gc();
		  statusMessage("OK");
		}
	      });
	    gcMenu.add(runGC);
	    gcMenu.show(LogPanel.this, e.getX(), e.getY());
	  }
	}
      });
  }

  /**
   * Record the starting of a new task
   */
  public void taskStarted() {
    if (m_TaskMonitor != null) {
      m_TaskMonitor.taskStarted();
    }
  }

  /**
   * Record a task ending
   */
  public void taskFinished() {
    if (m_TaskMonitor != null) {
      m_TaskMonitor.taskFinished();
    }
  }
    
  /**
   * Gets a string containing current date and time.
   *
   * @return a string containing the date and time.
   */
  protected static String getTimestamp() {

    return (new SimpleDateFormat("HH:mm:ss:")).format(new Date());
  }

  /**
   * Sends the supplied message to the log area. The current timestamp will
   * be prepended.
   *
   * @param message a value of type 'String'
   */
  public void logMessage(String message) {

    if (m_First) {
      m_First = false;
    } else {
      m_LogText.append("\n");
    }
    m_LogText.append(LogPanel.getTimestamp() + ' ' + message);
  }

  /**
   * Sends the supplied message to the status line.
   *
   * @param message the status message
   */
  public void statusMessage(String message) {

    m_StatusLab.setText(message);
  }

  
  /**
   * Tests out the log panel from the command line.
   *
   * @param args ignored
   */
  public static void main(String [] args) {

    try {
      final javax.swing.JFrame jf = new javax.swing.JFrame("Log Panel");
      jf.getContentPane().setLayout(new BorderLayout());
      final LogPanel lp = new LogPanel();
      jf.getContentPane().add(lp, BorderLayout.CENTER);
      jf.addWindowListener(new java.awt.event.WindowAdapter() {
	public void windowClosing(java.awt.event.WindowEvent e) {
	  jf.dispose();
	  System.exit(0);
	}
      });
      jf.pack();
      jf.setVisible(true);
      lp.logMessage("Welcome to the generic log panel!");
      lp.statusMessage("Hi there");
      lp.logMessage("Funky chickens");
      
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
