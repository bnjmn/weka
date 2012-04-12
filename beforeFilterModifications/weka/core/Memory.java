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
 * Memory.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */


package weka.core;

import javax.swing.JOptionPane;

/**
 * A little helper class for Memory management. Very crude, since JDK 1.4 
 * doesn't offer real Memory Management.<p/>
 * The memory management can be disabled by using the setEnabled(boolean)
 * method.
 *
 * @author    FracPete (fracpete at waikato dot ac dot nz)
 * @version   $Revision: 1.3 $
 * @see       #setEnabled(boolean)
 */
public class Memory {
  /** whether memory management is enabled */
  protected static boolean m_Enabled = true;
  
  /** whether a GUI is present */
  protected boolean m_UseGUI = false;

  /** the initial size of the JVM */
  protected static long m_Initial = Runtime.getRuntime().totalMemory();

  /** the total memory that is used */
  protected long m_Total;

  /** the maximum amount of memory that can be used */
  protected long m_Max;

  /** the current runtime variable  */
  protected Runtime m_Runtime;

  /**
   * initializes the memory management without GUI support
   */
  public Memory() {
    this(false);
  }

  /**
   * initializes the memory management
   * @param useGUI      whether a GUI is present
   */
  public Memory(boolean useGUI) {
    m_UseGUI  = useGUI;
    m_Runtime = Runtime.getRuntime();
    m_Max     = m_Runtime.maxMemory();
    m_Total   = m_Runtime.totalMemory();
  }

  /**
   * returns whether the memory management is enabled
   */
  public boolean isEnabled() {
    return m_Enabled;
  }

  /**
   * sets whether the memory management is enabled
   */
  public void setEnabled(boolean value) {
    m_Enabled = value;
  }

  /**
   * whether to display a dialog in case of a problem (= TRUE) or just print
   * on stderr (= FALSE)
   */
  public boolean getUseGUI() {
    return m_UseGUI;
  }

  /**
   * returns the initial size of the JVM
   */
  public long getInitial() {
    return m_Initial;
  }

  /**
   * returns the current memory consumption
   */
  public long getCurrent() {
    m_Runtime = Runtime.getRuntime();
    m_Total   = m_Runtime.totalMemory();

    return m_Total;
  }

  /**
   * returns the maximum amount of memory that can be assigned
   */
  public long getMax() {
    return m_Max;
  }

  /**
   * checks if there's still enough memory left. if ENABLED is true, then
   * false is returned always
   */
  public boolean isOutOfMemory() {
    if (isEnabled())
      return ((getMax() - getCurrent()) < (getInitial() + 200000));
    else
      return false;
  }

  /**
   * prints an error message if OutOfMemory (and if GUI is present a dialog),
   * otherwise nothing happens. isOutOfMemory() has to be called beforehand,
   * since it sets all the memory parameters.
   * @see #isOutOfMemory()
   * @see #ENABLED
   */
  public void showOutOfMemory() {
    double        MB;
    double        initial;
    double        total;
    double        max;

    if (!isEnabled())
      return;
      
    System.gc();

    MB      = 1024 * 1024;
    initial = m_Initial / MB;
    total   = m_Total / MB;
    max     = m_Max / MB;
    
    String msg =   "Not enough memory. Please load a smaller "  
                 + "dataset or use larger heap size.\n"
                 + "- initial JVM size:   " 
                 + Utils.doubleToString(initial, 1) + "MB\n"
                 + "- total memory used:  " 
                 + Utils.doubleToString(total, 1) + "MB\n"
                 + "- max. memory avail.: " 
                 + Utils.doubleToString(max, 1) + "MB\n"
                 + "\n"
                 + "Note:\n"
                 + "The Java heap size can be specified with the -Xmx option.\n"
                 + "E.g., to use 128MB as heap size, the command line looks like this:\n"
                 + "   java -Xmx128m -classpath ...\n"
                 + "This does NOT work in the SimpleCLI, the java command refers\n"
                 + "to the one with which Weka is started.";
    
    System.err.println(msg);
    
    if (getUseGUI())
      JOptionPane.showMessageDialog(
          null, msg, "OutOfMemory", JOptionPane.WARNING_MESSAGE);
  }

  /**
   * stops all the current threads, to make a restart possible
   */
  public void stopThreads() {
    int           i;
    Thread[]      thGroup;
    Thread        t;

    thGroup = new Thread[Thread.activeCount()];
    Thread.enumerate(thGroup);

    for (i = 0; i < thGroup.length; i++) {
      t = thGroup[i];
      if (t != null) {
        if (t != Thread.currentThread()) {
          if (t.getName().startsWith("Thread"))
            t.stop();
          else if (t.getName().startsWith("AWT-EventQueue"))
            t.stop();
        }
      }
    }

    thGroup = null;

    System.gc();
  }
}
