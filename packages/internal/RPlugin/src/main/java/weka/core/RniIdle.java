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
 *    RniIdle.java
 *    Copyright (C) 2017 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import org.rosuda.REngine.JRI.JRIEngine;
import org.rosuda.REngine.REngine;

/**
 * Need this as a separate class so that we can inject its bytecode more easily.
 * Used to enable R to do some other event handling at regular intervals.
 */
public class RniIdle implements Runnable {

  private JRIEngine m_engine;

  public RniIdle(REngine rengine) {
    m_engine = (JRIEngine)rengine;
  }

  public void run() {
    boolean obtainedLock = m_engine.getRni().getRsync().safeLock();
    try {
      m_engine.getRni().rniIdle();
    } finally {
      if (obtainedLock) m_engine.getRni().getRsync().unlock();
    }
  }
}
