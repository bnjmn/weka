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
 *    SaveDataToAnyFile.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *
 */

package wekaexamples.core.converters;

import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSink;
import weka.core.converters.ConverterUtils.DataSource;

/**
 * Loads the data from the file provided as first parameter and saves it to a
 * file provided as second parameter. The converter is automatically chosen
 * based on the file's extension, this applies to the source and destination
 * file.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class SaveDataToAnyFile {

  /**
   * Expects a filename as first and second parameter.
   *
   * @param args        the command-line parameters
   * @throws Exception  if something goes wrong
   */
  public static void main(String[] args) throws Exception {
    // output usage
    if (args.length != 2) {
      System.err.println("\nUsage: java SaveDataToAnyFile <input> <output>\n");
      System.exit(1);
    }

    System.out.println("\nReading from file " + args[0] + "...");
    Instances data = DataSource.read(args[0]);

    System.out.println("\nSaving to file " + args[1] + "...");
    DataSink.write(args[1], data);
  }
}
