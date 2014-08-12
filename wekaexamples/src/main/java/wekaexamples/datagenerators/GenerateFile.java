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
 * GenerateFile.java
 * Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 */
package wekaexamples.datagenerators;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;

import weka.datagenerators.DataGenerator;
import weka.datagenerators.classifiers.classification.RDG1;

/**
 * Demonstrates how to write RDG1 generated data directly to an ARFF file.
 * If no output file provided, the generated data gets written to stdout.
 * 
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class GenerateFile {

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
    RDG1 generator = new RDG1();
    generator.setMaxRuleSize(5);
    generator.setOutput(output);
    DataGenerator.makeData(generator, generator.getOptions());
    output.flush();
    output.close();
  }
}
