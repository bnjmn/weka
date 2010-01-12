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
 *    MISwap.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *
 */

package wekaexamples.core;

import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

/**
 * Example class that loads a multi-instance dataset and swaps the values of
 * the MI-bag of two instances.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class MultiInstanceSwap {

  /**
   * Expects a filename pointing to an MI dataset as first parameter.
   * The last attribute is used as class attribute.
   *
   * @param args        the commandline arguments
   * @throws Exception  if something goes wrong
   */
  public static void main(String[] args) throws Exception {
    // load data
    Instances data = DataSource.read(args[0]);
    data.setClassIndex(data.numAttributes() -1);

    // swap multi-instance value of instance 0 and 1 at attribute position 1
    int attPos = 1;
    Instances value0 = data.instance(0).relationalValue(attPos);
    Instances value1 = data.instance(1).relationalValue(attPos);
    data.instance(0).setValue(attPos, data.attribute(attPos).addRelation(value1));
    data.instance(1).setValue(attPos, data.attribute(attPos).addRelation(value0));

    // dump the Instances object in ARFF format to stdout
    System.out.println(data);
  }
}
