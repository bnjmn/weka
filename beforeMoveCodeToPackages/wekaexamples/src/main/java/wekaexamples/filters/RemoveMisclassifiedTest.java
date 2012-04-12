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
 * RemoveMisclassifiedTest.java
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package wekaexamples.filters;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSink;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.instance.RemoveMisclassified;

/**
 * Runs the RemoveMisclassified filter over a given ARFF file and saves the
 * reduced dataset again.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class RemoveMisclassifiedTest {
  
  /**
   * Expects three parameters: 
   * First parameter is the input file, the second the classifier
   * to use (no options) and the third one is the output file. 
   * It is assumed that the class attribute is the last attribute in the
   * dataset.
   * 
   * @param args	the commandline arguments
   * @throws Exception	if something goes wrong
   */
  public static void main(String[] args) throws Exception {
    if (args.length != 3) {
      System.out.println("\nUsage: RemoveMisclassifiedTest <input.arff> <classname> <output.arff>\n");
      System.exit(1);
    }

    // get data 
    Instances input = DataSource.read(args[0]);
    input.setClassIndex(input.numAttributes() - 1);

    // get classifier
    Classifier c = AbstractClassifier.forName(args[1], new String[0]);

    // setup and run filter
    RemoveMisclassified filter = new RemoveMisclassified();
    filter.setClassifier(c);
    filter.setClassIndex(-1);
    filter.setNumFolds(0);
    filter.setThreshold(0.1);
    filter.setMaxIterations(0);
    filter.setInputFormat(input);
    Instances output = Filter.useFilter(input, filter);
    
    // output file
    DataSink.write(args[2], output);
  }
}
