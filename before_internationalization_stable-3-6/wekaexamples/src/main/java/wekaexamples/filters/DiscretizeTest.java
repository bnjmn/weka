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
 * DiscretizeTest.java
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package wekaexamples.filters;

import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.supervised.attribute.Discretize;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Shows how to generate compatible train/test sets using the Discretize
 * filter.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class DiscretizeTest {

  /**
   * loads the given ARFF file and sets the class attribute as the last
   * attribute.
   *
   * @param filename    the file to load
   * @throws Exception  if somethings goes wrong
   */
  protected static Instances load(String filename) throws Exception {
    Instances       result;
    BufferedReader  reader;

    reader = new BufferedReader(new FileReader(filename));
    result = new Instances(reader);
    result.setClassIndex(result.numAttributes() - 1);
    reader.close();

    return result;
  }

  /**
   * saves the data to the specified file
   *
   * @param data        the data to save to a file
   * @param filename    the file to save the data to
   * @throws Exception  if something goes wrong
   */
  protected static void save(Instances data, String filename) throws Exception {
    BufferedWriter  writer;

    writer = new BufferedWriter(new FileWriter(filename));
    writer.write(data.toString());
    writer.newLine();
    writer.flush();
    writer.close();
  }
  
  /**
   * Takes four arguments:
   * <ol>
   *   <li>input train file</li>
   *   <li>input test file</li>
   *   <li>output train file</li>
   *   <li>output test file</li>
   * </ol>
   *
   * @param args        the commandline arguments
   * @throws Exception  if something goes wrong
   */
  public static void main(String[] args) throws Exception {
    Instances     inputTrain;
    Instances     inputTest;
    Instances     outputTrain;
    Instances     outputTest;
    Discretize    filter;
    
    // load data (class attribute is assumed to be last attribute)
    inputTrain = load(args[0]);
    inputTest  = load(args[1]);

    // setup filter
    filter = new Discretize();
    filter.setInputFormat(inputTrain);

    // apply filter
    outputTrain = Filter.useFilter(inputTrain, filter);
    outputTest  = Filter.useFilter(inputTest,  filter);

    // save output
    save(outputTrain, args[2]);
    save(outputTest,  args[3]);
  }
}
