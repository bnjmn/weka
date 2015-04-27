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
 *    SessionMutex.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.python;

/**
 * Class to use for session locking
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class SessionMutex {

  private boolean m_verbose;

  /** defines the current mutex state */
  private boolean m_locked;

  /** thread that m_locked this mutex */
  private Thread m_lockedBy;

  public SessionMutex() {
  }

  public SessionMutex(boolean verbose) {
    m_verbose = verbose;
  }

  private synchronized void lock() {
    while (m_locked) {
      if (m_lockedBy == Thread.currentThread()) {
        System.err
          .println("INFO: Mutex detected a deadlock! The application is likely to hang indefinitely!");
      }

      if (m_verbose) {
        System.out.println("INFO: " + toString() + " is m_locked by " + m_lockedBy
          + ", but " + Thread.currentThread() + " waits for release");
      }
      try {
        wait();
      } catch (InterruptedException e) {
        if (m_verbose)
          System.out.println("INFO: " + toString()
            + " caught InterruptedException");
      }
    }
    m_locked = true;
    m_lockedBy = Thread.currentThread();
    if (m_verbose) {
      System.out.println("INFO: " + toString() + " m_locked by " + m_lockedBy);
    }
  }

  public synchronized boolean safeLock() {
    if (m_locked && m_lockedBy == Thread.currentThread()) {
      if (m_verbose) {
        System.out.println("INFO: " + toString()
          + " unable to provide safe lock for " + Thread.currentThread());
      }
      return false;
    }
    lock();
    return true;
  }

  public synchronized void unlock() {
    if (m_locked && m_lockedBy != Thread.currentThread()) {
      System.err
        .println("WARNING: Mutex was unlocked by other thread");
    }
    m_locked = false;
    if (m_verbose) {
      System.out.println("INFO: " + toString() + " unlocked by "
        + Thread.currentThread());
    }

    // notify just 1 in case more of them are waiting
    notify();
  }
}
