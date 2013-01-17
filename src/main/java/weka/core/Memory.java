/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Memory.java
 * Copyright (C) 2005-2012 University of Waikato, Hamilton, New Zealand
 *
 */


package weka.core;

import javax.swing.JOptionPane;

import java.lang.management.MemoryMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;


/**
 * A little helper class for Memory management.
 * The memory management can be disabled by using the setEnabled(boolean)
 * method.
 *
 * @author    FracPete (fracpete at waikato dot ac dot nz)
 * @version   $Revision$
 * @see       #setEnabled(boolean)
 */
public class Memory
  implements RevisionHandler {
  
  /** whether memory management is enabled */
  protected static boolean m_Enabled = true;
  
  /** whether a GUI is present */
  protected boolean m_UseGUI = false;

  /** the managed bean to use */
  protected static MemoryMXBean m_MemoryMXBean = ManagementFactory.getMemoryMXBean();

  /** the last MemoryUsage object obtained */
  protected MemoryUsage m_MemoryUsage = null;

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
  }

  /**
   * returns whether the memory management is enabled
   * 
   * @return		true if enabled
   */
  public boolean isEnabled() {
    return m_Enabled;
  }

  /**
   * sets whether the memory management is enabled
   * 
   * @param value	true if the management should be enabled
   */
  public void setEnabled(boolean value) {
    m_Enabled = value;
  }

  /**
   * whether to display a dialog in case of a problem (= TRUE) or just print
   * on stderr (= FALSE)
   * 
   * @return		true if the GUI is used
   */
  public boolean getUseGUI() {
    return m_UseGUI;
  }

  /**
   * returns the initial size of the JVM heap, 
   * obtains a fresh MemoryUsage object to do so.
   * 
   * @return		the initial size in bytes
   */
  public long getInitial() {
    m_MemoryUsage = m_MemoryMXBean.getHeapMemoryUsage();
    return m_MemoryUsage.getInit();
  }

  /**
   * returns the currently used size of the JVM heap, 
   * obtains a fresh MemoryUsage object to do so.
   * 
   * @return		the used size in bytes
   */
  public long getCurrent() {
    m_MemoryUsage = m_MemoryMXBean.getHeapMemoryUsage();
    return m_MemoryUsage.getUsed();
  }

  /**
   * returns the maximum size of the JVM heap, 
   * obtains a fresh MemoryUsage object to do so.
   * 
   * @return		the maximum size in bytes
   */
  public long getMax() {
    m_MemoryUsage = m_MemoryMXBean.getHeapMemoryUsage();
    return m_MemoryUsage.getMax();
  }

  /**
   * checks if there's still enough memory left by checking whether
   * there is still a 50MB margin between getUsed() and getMax(). 
   * if ENABLED is true, then
   * false is returned always. updates the MemoryUsage variable
   * before checking.
   * 
   * @return		true if out of memory (only if management enabled, 
   * 			otherwise always false)
   */
  public boolean isOutOfMemory() {
    m_MemoryUsage = m_MemoryMXBean.getHeapMemoryUsage();
    if (isEnabled()) {
      return ((m_MemoryUsage.getMax() - m_MemoryUsage.getUsed()) < 52428800);
    } else
      return false;
  }

  /**
   * returns the amount of bytes as MB
   * 
   * @return		the MB amount
   */
  public static double toMegaByte(long bytes) {
    return (bytes / (double) (1024 * 1024));
  }

  /**
   * prints an error message if OutOfMemory (and if GUI is present a dialog),
   * otherwise nothing happens. isOutOfMemory() has to be called beforehand,
   * since it sets all the memory parameters.
   * @see #isOutOfMemory()
   * @see #m_Enabled
   */
  public void showOutOfMemory() {
    if (!isEnabled() || (m_MemoryUsage == null))
      return;
      
    System.gc();

    String msg =   "Not enough memory (less than 50MB left on heap). Please load a smaller "  
                 + "dataset or use a larger heap size.\n"
                 + "- initial heap size:   " 
                 + Utils.doubleToString(toMegaByte(m_MemoryUsage.getInit()), 1) + "MB\n"
                 + "- current memory (heap) used:  " 
                 + Utils.doubleToString(toMegaByte(m_MemoryUsage.getUsed()), 1) + "MB\n"
                 + "- max. memory (heap) available: " 
                 + Utils.doubleToString(toMegaByte(m_MemoryUsage.getMax()), 1) + "MB\n"
                 + "\n"
                 + "Note:\n"
                 + "The Java heap size can be specified with the -Xmx option.\n"
                 + "E.g., to use 128MB as heap size, the command line looks like this:\n"
                 + "   java -Xmx128m -classpath ...\n"
                 + "This does NOT work in the SimpleCLI, the above java command refers\n"
                 + "to the one with which Weka is started. See the Weka FAQ on the web\n"
                 + "for further info.";
    
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
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  /**
   * prints only some statistics
   *
   * @param args the commandline arguments - ignored
   */
  public static void main(String[] args) {
    Memory mem = new Memory();
    System.out.println(
        "Initial memory: "
        + Utils.doubleToString(Memory.toMegaByte(mem.getInitial()), 1) + "MB" 
        + " (" + mem.getInitial() + ")");
    System.out.println(
        "Max memory: "
        + Utils.doubleToString(Memory.toMegaByte(mem.getMax()), 1) + "MB"
        + " (" + mem.getMax() + ")");
  }
}
