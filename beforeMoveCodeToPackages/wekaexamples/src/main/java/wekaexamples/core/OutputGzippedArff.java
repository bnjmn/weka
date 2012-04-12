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
 *    OutputGzippedArff.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *
 */

package wekaexamples.core;

import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.zip.GZIPOutputStream;

/**
 * Converts an ARFF file to a gzipped ARFF file.
 * <p/>
 * Commandline usage:
 * <pre>
 * OutputGzippedArff input.arff output.arff.gz
 * </pre>
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class OutputGzippedArff {

  public static void main(String[] args) throws Exception {
    // correct parameters?
    if (args.length != 2) {
      System.err.println("\nUsage: " + OutputGzippedArff.class.getName() + " input.arff output.arff.gz\n");
      System.exit(1);
    }

    // load dataset
    Instances data = DataSource.read(args[0]);

    // save dataset compressed
    BufferedWriter writer =
      new BufferedWriter(
        new OutputStreamWriter(
          new GZIPOutputStream(
            new FileOutputStream(
              new File(args[0])))));
    // header
    writer.write(new Instances(data, 0).toString());
    writer.newLine();

    // rows
    for (int i = 0; i < data.numInstances(); i++) {
      writer.write(data.instance(i).toString());
      writer.newLine();
    }

    writer.flush();
    writer.close();
  }
}
