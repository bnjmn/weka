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
 * AddAttribute.java
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package wekaexamples.filters;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Add;

import java.util.Random;

/**
 * Adds a nominal and a numeric attribute to the dataset provided as first
 * parameter (and fills it with random values) and outputs the result to
 * stdout. It's either done via the Add filter (second option "filter") 
 * or manual with Java (second option "java").<p/>
 *
 * Usage: AddAttribute &lt;file.arff&gt; &lt;filter|java&gt; 
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class AddAttribute {
  
  /**
   * Adds the attributes.
   *
   * @param args	the commandline arguments
   * @throws Exception	if something goes wrong
   */
  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      System.out.println("\nUsage: <file.arff> <filter|java>\n");
      System.exit(1);
    }

    // load dataset
    Instances data = DataSource.read(args[0]);
    Instances newData = null;

    // filter or java?
    if (args[1].equals("filter")) {
      Add filter;
      newData = new Instances(data);
      // 1. nominal attribute
      filter = new Add();
      filter.setAttributeIndex("last");
      filter.setNominalLabels("A,B,C,D");
      filter.setAttributeName("NewNominal");
      filter.setInputFormat(newData);
      newData = Filter.useFilter(newData, filter);
      // 2. numeric attribute
      filter = new Add();
      filter.setAttributeIndex("last");
      filter.setAttributeName("NewNumeric");
      filter.setInputFormat(newData);
      newData = Filter.useFilter(newData, filter);
    }
    else if (args[1].equals("java")) {
      newData = new Instances(data);
      // add new attributes
      // 1. nominal
      FastVector values = new FastVector();
      values.addElement("A");
      values.addElement("B");
      values.addElement("C");
      values.addElement("D");
      newData.insertAttributeAt(new Attribute("NewNominal", values), newData.numAttributes());
      // 2. numeric
      newData.insertAttributeAt(new Attribute("NewNumeric"), newData.numAttributes());
    }
    else {
      System.out.println("\nUsage: <file.arff> <filter|java>\n");
      System.exit(2);
    }

    // random values
    Random rand = new Random(1);
    for (int i = 0; i < newData.numInstances(); i++) {
      // 1. nominal
      newData.instance(i).setValue(newData.numAttributes() - 2, rand.nextInt(4));  // index of labels A:0,B:1,C:2,D:3
      // 2. numeric
      newData.instance(i).setValue(newData.numAttributes() - 1, rand.nextDouble());
    }

    // output on stdout
    System.out.println(newData);
  }
}
