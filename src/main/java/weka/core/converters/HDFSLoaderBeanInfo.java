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
 *    HDFSLoaderBeanInfo.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.converters;

import java.beans.BeanDescriptor;
import java.beans.SimpleBeanInfo;

/**
 * BeanInfo class for the HDFSLoader
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class HDFSLoaderBeanInfo extends SimpleBeanInfo {

  @Override
  public BeanDescriptor getBeanDescriptor() {
    return new BeanDescriptor(weka.core.converters.HDFSLoader.class,
      weka.gui.converters.HDFSLoaderCustomizer.class);
  }
}
