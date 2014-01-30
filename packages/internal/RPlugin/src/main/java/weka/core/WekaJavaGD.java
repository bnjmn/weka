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
 *    WekaJavaGD.java
 *    Copyright (C) 2012-2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import org.rosuda.javaGD.GDInterface;

/**
 * A hook into the JavaGD system. Allows us to register our rendering system as
 * the one to use by JavaGD. We register this by executing the following R
 * command:
 * <p>
 * 
 * <code>Sys.putenv('JAVAGD_CLASS_NAME'='weka/core/WekaJavaGD')</code>
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class WekaJavaGD extends GDInterface {

  @Override
  public void gdOpen(double w, double h) {
    c = JavaGDOffscreenRenderer.s_renderer;
  }
}
