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
 * SingleBatch.java
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package wekaexamples.filters;

import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSink;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

/**
 * Processes a single dataset using the Remove filter.
 * 
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 * @see Remove
 */
public class SingleBatch {

  /**
   * Expects two parameters: first is input file, second the output file.
   * 
   * @param args	the commandline arguments
   * @throws Exception	if something goes wrong
   */
  public static void main(String[] args) throws Exception {
    // load data
    Instances data = DataSource.read(args[0]);
    
    // filter data
    String[] options = new String[2];
    options[0] = "-R";                                    // "range"
    options[1] = "1";                                     // first attribute
    Remove remove = new Remove();                         // new instance of filter
    remove.setOptions(options);                           // set options
    remove.setInputFormat(data);                          // inform filter about dataset **AFTER** setting options
    Instances newData = Filter.useFilter(data, remove);   // apply filter    
    
    // save data
    DataSink.write(args[1], newData);
  }
}
