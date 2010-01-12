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
 * DataSink.java
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package wekaexamples.core.converters;

import weka.core.Instances;
import weka.core.converters.ArffSaver;

import java.io.File;

/**
 * Helper class for writing ARFF files.
 * 
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class DataSink {

  /**
   * Writes a dataset to an ARFF file.
   * 
   * @param filename	the file to write to
   * @param data	the data to write as ARFF
   * @throws Exception	if writing fails
   */
  public static void write(String filename, Instances data) throws Exception {
    ArffSaver saver = new ArffSaver();
    saver.setInstances(data);
    saver.setFile(new File(filename));
    saver.writeBatch();
  }
}
