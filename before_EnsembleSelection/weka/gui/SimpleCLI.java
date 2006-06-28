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
 *    SimpleCLI.java
 *    Copyright (C) 1999 Len Trigg
 *
 */


package weka.gui;

import weka.core.Utils;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Vector;

/**
 * Creates a very simple command line for invoking the main method of
 * classes. System.out and System.err are redirected to an output area.
 * Features a simple command history -- use up and down arrows to move
 * through previous commmands. This gui uses only AWT (i.e. no Swing).
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.8 $
 */
public class SimpleCLI
  extends Frame
  implements ActionListener {
  
  /** for serialization */
  static final long serialVersionUID = -50661410800566036L;
  
  /** The output area canvas added to the frame */
  protected TextArea m_OutputArea = new TextArea();

  /** The command input area */
  protected TextField m_Input = new TextField();

  /** The history of commands entered interactively */
  protected Vector m_CommandHistory = new Vector();

  /** The current position in the command history */
  protected int m_HistoryPos = 0;

  /** The new output stream for System.out */
  protected PipedOutputStream m_POO = new PipedOutputStream();

  /** The new output stream for System.err */
  protected PipedOutputStream m_POE = new PipedOutputStream();

  /** The thread that sends output from m_POO to the output box */
  protected Thread m_OutRedirector;

  /** The thread that sends output from m_POE to the output box */
  protected Thread m_ErrRedirector;

  /** The thread currently running a class main method */
  protected Thread m_RunThread;
  

  /**
   * A class that sends all lines from a reader to a TextArea component
   * 
   * @author Len Trigg (trigg@cs.waikato.ac.nz)
   * @version $Revision: 1.8 $
   */
  class ReaderToTextArea extends Thread {

    /** The reader being monitored */
    protected LineNumberReader m_Input;

    /** The output text component */
    protected TextArea m_Output;
    
    /**
     * Sets up the ReaderToTextArea
     *
     * @param input the Reader to monitor
     * @param output the TextArea to send output to
     */
    public ReaderToTextArea(Reader input, TextArea output) {
      setDaemon(true);
      m_Input = new LineNumberReader(input);
      m_Output = output;
    }

    /**
     * Sit here listening for lines of input and appending them straight
     * to the text component.
     */
    public void run() {

      while (true) {
	try {
	  m_Output.append(m_Input.readLine() + '\n');
	} catch (Exception ex) {
	  try {
	    sleep(100);
	  } catch (Exception e) {
	  }
	}
      }
    }
  }

  /**
   * A class that handles running the main method of the class
   * in a separate thread
   * 
   * @author Len Trigg (trigg@cs.waikato.ac.nz)
   * @version $Revision: 1.8 $
   */
  class ClassRunner extends Thread {

    /** Stores the main method to call */
    protected Method m_MainMethod;

    /** Stores the command line arguments to pass to the main method */
    String [] m_CommandArgs;
    
    /**
     * Sets up the class runner thread.
     *
     * @param theClass the Class to call the main method of
     * @param commandArgs an array of Strings to use as command line args
     * @exception Exception if an error occurs
     */
    public ClassRunner(Class theClass, String [] commandArgs)
      throws Exception {
      setDaemon(true);
      Class [] argTemplate = { String[].class };
      m_CommandArgs = commandArgs;
      m_MainMethod = theClass.getMethod("main", argTemplate);
      if (((m_MainMethod.getModifiers() & Modifier.STATIC) == 0)
	  || (m_MainMethod.getModifiers() & Modifier.PUBLIC) == 0) {
	throw new NoSuchMethodException("main(String[]) method of " +
					theClass.getName() +
					" is not public and static.");
      }
    }

    /**
     * Starts running the main method.
     */
    public void run() {
      
      try {
	Object [] args = { m_CommandArgs };
	m_MainMethod.invoke(null, args);
	if (isInterrupted()) {
	  System.err.println("[...Interrupted]");
	}
      } catch (Exception ex) {
	if (ex.getMessage() == null) {
	  System.err.println("[...Killed]");
	} else {
	  System.err.println("[Run exception] " + ex.getMessage());
	}
      } finally {
	m_RunThread = null;
      }
    }
  }


  
  /**
   * Constructor
   *
   * @exception Exception if an error occurs
   */
  public SimpleCLI() throws Exception {
    
    setLayout(new BorderLayout());
    add(m_OutputArea, "Center");
    add(m_Input, "South");

    m_Input.addActionListener(this);
    m_Input.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
	doHistory(e);
      }
    });
    m_OutputArea.setEditable(false);
    m_OutputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
    // Redirect System.out to the text area
    //    System.out.println("Redirecting System.out");
    PipedInputStream pio = new PipedInputStream(m_POO);
    System.setOut(new PrintStream(m_POO));
    Reader r = new InputStreamReader(pio);
    m_OutRedirector = new ReaderToTextArea(r, m_OutputArea);
    m_OutRedirector.start();

    // Redirect System.err to the text area
    //    System.err.println("Redirecting System.err");
    PipedInputStream pie = new PipedInputStream(m_POE);
    System.setErr(new PrintStream(m_POE));
    r = new InputStreamReader(pie);
    m_ErrRedirector = new ReaderToTextArea(r, m_OutputArea);
    m_ErrRedirector.start();
    
    // Finish setting up the frame and show it
    pack();
    setSize(600,500);

    System.out.println("\nWelcome to the WEKA SimpleCLI\n\n"
                       +"Enter commands in the textfield at the bottom of \n"
                       +"the window. Use the up and down arrows to move \n"
                       +"through previous commands.\n\n");
    runCommand("help");
  }

  /**
   * Executes a simple cli command.
   *
   * @param commands the command string
   * @exception Exception if an error occurs
   */
  public void runCommand(String commands) throws Exception {

    System.out.println("> " + commands + '\n');
    System.out.flush();
    String [] commandArgs = Utils.splitOptions(commands);
    if (commandArgs.length == 0) {
      return;
    }
    if (commandArgs[0].equals("java")) {
      // Execute the main method of a class
      commandArgs[0] = "";
      try {
	if (commandArgs.length == 1) {
	  throw new Exception("No class name given");
	}
	String className = commandArgs[1];
	commandArgs[1] = "";
	if (m_RunThread != null) {
	  throw new Exception("An object is already running, use \"break\""
			      + " to interrupt it.");
	}
	Class theClass = Class.forName(className);

	// some classes expect a fixed order of the args, i.e., they don't
	// use Utils.getOption(...) => create new array without first two
	// empty strings (former "java" and "<classname>")
	Vector argv = new Vector();
	for (int i = 2; i < commandArgs.length; i++)
	  argv.add(commandArgs[i]);
  
	m_RunThread = new ClassRunner(theClass, (String[]) argv.toArray(new String[argv.size()]));
	m_RunThread.setPriority(Thread.MIN_PRIORITY); // UI has most priority
	m_RunThread.start();	
      } catch (Exception ex) {
	System.err.println(ex.getMessage());
      }

    } else if (commandArgs[0].equals("cls")) {
      // Clear the text area
      m_OutputArea.setText("");

    } else if (commandArgs[0].equals("break")) {
      if (m_RunThread == null) {
	System.err.println("Nothing is currently running.");
      } else {
	System.out.println("[Interrupt...]");
	m_RunThread.interrupt();
      }
    } else if (commandArgs[0].equals("kill")) {
      if (m_RunThread == null) {
	System.err.println("Nothing is currently running.");
      } else {
	System.out.println("[Kill...]");
	m_RunThread.stop();
	m_RunThread = null;
      }
    } else if (commandArgs[0].equals("exit")) {
      // Shut down
      // fire the frame close event
      processEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
      
    } else {
      boolean help = ((commandArgs.length > 1)
		      && commandArgs[0].equals("help"));
      if (help && commandArgs[1].equals("java")) {
	System.err.println("java <classname> <args>\n\n"
			   + "Starts the main method of <classname> with "
			   + "the supplied command line arguments (if any).\n"
			   + "The command is started in a separate thread, "
			   + "and may be interrupted with the \"break\"\n"
			   + "command (friendly), or killed with the \"kill\" "
			   + "command (unfriendly).\n");
      } else if (help && commandArgs[1].equals("break")) {
	System.err.println("break\n\n"
			   + "Attempts to nicely interrupt the running job, "
			   + "if any. If this doesn't respond in an\n"
			   + "acceptable time, use \"kill\".\n");
      } else if (help && commandArgs[1].equals("kill")) {
	System.err.println("kill\n\n"
			   + "Kills the running job, if any. You should only "
			   + "use this if the job doesn't respond to\n"
			   + "\"break\".\n");
      } else if (help && commandArgs[1].equals("cls")) {
	System.err.println("cls\n\n"
			   + "Clears the output area.\n");
      } else if (help && commandArgs[1].equals("exit")) {
	System.err.println("exit\n\n"
			   + "Exits the SimpleCLI program.\n");
      } else {
	// Print a help message
	System.err.println("Command must be one of:\n"
			   + "\tjava <classname> <args>\n"
			   + "\tbreak\n"
			   + "\tkill\n"
			   + "\tcls\n"
			   + "\texit\n"
			   + "\thelp <command>\n");
      }
    }
  }

  /**
   * Changes the currently displayed command line when certain keys
   * are pressed. The up arrow moves back through history entries
   * and the down arrow moves forward through history entries.
   *
   * @param e a value of type 'KeyEvent'
   */
  public void doHistory(KeyEvent e) {
    
    if (e.getSource() == m_Input) {
      switch (e.getKeyCode()) {
      case KeyEvent.VK_UP:
	if (m_HistoryPos > 0) {
	  m_HistoryPos--;
	  String command = (String) m_CommandHistory.elementAt(m_HistoryPos);
	  m_Input.setText(command);
	}
	break;
      case KeyEvent.VK_DOWN:
	if (m_HistoryPos < m_CommandHistory.size()) {
	  m_HistoryPos++;
	  String command = "";
	  if (m_HistoryPos < m_CommandHistory.size()) {
	    command = (String) m_CommandHistory.elementAt(m_HistoryPos);
	  }
	  m_Input.setText(command);
	}
	break;
      default:
	break;
      }
    }
  }

  /**
   * Only gets called when return is pressed in the input area, which
   * starts the command running.
   *
   * @param e a value of type 'ActionEvent'
   */
  public void actionPerformed(ActionEvent e) {

    try {
      if (e.getSource() == m_Input) {
	String command = m_Input.getText();
	int last = m_CommandHistory.size() - 1;
	if ((last < 0)
	    || !command.equals((String)m_CommandHistory.elementAt(last))) {
	  m_CommandHistory.addElement(command);
	  m_HistoryPos = m_CommandHistory.size();
	}
	runCommand(command);
	
	m_Input.setText("");
      }
    } catch (Exception ex) {
      System.err.println(ex.getMessage());
    }
  }

  /**
   * Method to start up the simple cli
   *
   * @param args array of command line arguments. Not used.
   */
  public static void main(String[] args) {
    
    try {
      final SimpleCLI frame = new SimpleCLI();
      frame.addWindowListener(new WindowAdapter() {
	public void windowClosing(WindowEvent param1) {
	  System.err.println("window closed");
	  frame.dispose();
	}
      });
      frame.setVisible(true);
    } catch (Exception e) {
      System.out.println(e.getMessage());
      System.exit(0);
    }
  }
}
