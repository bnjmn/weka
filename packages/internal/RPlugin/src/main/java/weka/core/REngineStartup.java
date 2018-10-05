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
 *    REngineStartup.java
 *    Copyright (C) 2018 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import org.rosuda.REngine.REngine;
import java.util.concurrent.Callable;

/**
 * Need this as a separate class so that we can inject its bytecode more easily.
 * Used to create thread for R engine start up.
 */
public class REngineStartup implements Callable<REngine> {

  private RSessionImpl s_sessionSingleton;

  public REngineStartup(RSessionImpl s) {
    s_sessionSingleton = s;
  }

  public REngine call() {
    try {
      return REngine.engineForClass("org.rosuda.REngine.JRI.JRIEngine",
              new String[]{"--slave"}, s_sessionSingleton, false);
    } catch (Exception ex) {
     return null;
    }
  }
}
