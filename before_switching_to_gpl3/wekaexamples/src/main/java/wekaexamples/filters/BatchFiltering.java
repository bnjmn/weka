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
 * BatchFiltering.java
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package wekaexamples.filters;

import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSink;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Standardize;

/**
 * Filters two datasets: the first is used to initialize the filter and the
 * second one is filtered according to this setup.
 * <p/>
 * Standardize is used as filter in this example.
 * 
 * @author  FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class BatchFiltering {

  /**
   * Expects four parameters: two input files and two output files.
   * 
   * @param args	the commandline arguments
   * @throws Exception	if something goes wrong
   */
  public static void main(String[] args) throws Exception {
    // load data
    Instances train = DataSource.read(args[0]);
    Instances test = DataSource.read(args[1]);
    
    // filter data
    Standardize filter = new Standardize();
    filter.setInputFormat(train);                          // initializing the filter once with training set
    Instances newTrain = Filter.useFilter(train, filter);  // configures the Filter based on train instances and returns filtered instances
    Instances newTest = Filter.useFilter(test, filter);    // create new test set
    
    // save data
    DataSink.write(args[2], newTrain);
    DataSink.write(args[3], newTest);
  }
}
