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
 * DataSource.java
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package wekaexamples.core.converters;

import weka.core.Instances;
import weka.core.converters.ArffLoader;

import java.io.File;

/**
 * Helper class for reading ARFF files.
 * 
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class DataSource {

  /**
   * Reads an ARFF file from a file.
   * 
   * @param filename	the ARFF file to read
   * @return		the data
   * @throws Exception  if reading fails
   */
  public static Instances read(String filename) throws Exception {
    ArffLoader loader = new ArffLoader();
    loader.setSource(new File(filename));
    Instances data = loader.getDataSet();
    return data;
  }
}
