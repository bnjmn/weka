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

/**
 * GenerateData.java
 * Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 */
package wekaexamples.datagenerators;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;

import weka.datagenerators.classifiers.classification.Agrawal;

/**
 * Demonstrates how to generate data with the Agrawal data generator.
 * If no ARFF output file is provided, data is written to stdout.
 * 
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class GenerateData {

  /**
   * Generates the data. First parameter the ARFF file to write to. If omitted,
   * the data is written to stdout.
   * 
   * @param args	the command-line parameters
   */
  public static void main(String[] args) throws Exception {
    PrintWriter output;
    if (args.length == 0)
      output = new PrintWriter(System.out);
    else
      output = new PrintWriter(new BufferedWriter(new FileWriter(args[0])));
    Agrawal generator = new Agrawal();
    generator.setBalanceClass(true);
    generator.setDatasetFormat(generator.defineDataFormat());
    // write header
    output.write(generator.getDatasetFormat().toString());
    // write data
    if (generator.getSingleModeFlag()) {
      for (int i = 0; i < generator.getNumExamplesAct(); i++) {
	output.write(generator.generateExample().toString());
	output.write(System.getProperty("line.separator"));
      }
    }
    else {
      output.write(generator.generateExamples().toString());
    }
    output.flush();
    output.close();
  }
}
