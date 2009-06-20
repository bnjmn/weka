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
 *    CreateInstances.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *
 */

package wekaexamples.core;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Generates an weka.core.Instances object with different attribute types.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class CreateInstances {
  
  /**
   * Generates the Instances object and outputs it in ARFF format to stdout.
   * 
   * @param args	ignored
   * @throws Exception	if generation of instances fails
   */
  public static void main(String[] args) throws Exception {
    FastVector      atts;
    FastVector      attVals;
    Instances       data;
    double[]        vals;
    int             i;

    // 1. set up attributes
    atts = new FastVector();
    // - numeric
    atts.addElement(new Attribute("att1"));
    // - nominal
    attVals = new FastVector();
    for (i = 0; i < 5; i++)
      attVals.addElement("val" + (i+1));
    atts.addElement(new Attribute("att2", attVals));
    // - string
    atts.addElement(new Attribute("att3", (FastVector) null));
    // - date
    atts.addElement(new Attribute("att4", "yyyy-mm-dd"));

    // 2. create Instances object
    data = new Instances("MyRelation", atts, 0);

    // 3. fill with data
    // first instance
    vals = new double[data.numAttributes()];
    // - numeric
    vals[0] = Math.PI;
    // - nominal
    vals[1] = attVals.indexOf("val3");
    // - string
    vals[2] = data.attribute(2).addStringValue("This is a string!");
    // - date
    vals[3] = data.attribute(3).parseDate("2001-11-09");
    // add
    data.add(new Instance(1.0, vals));

    // second instance
    vals = new double[data.numAttributes()];  // important: needs NEW array!
    // - numeric
    vals[0] = Math.E;
    // - nominal
    vals[1] = attVals.indexOf("val1");
    // - string
    vals[2] = data.attribute(2).addStringValue("And another one!");
    // - date
    vals[3] = data.attribute(3).parseDate("2000-12-01");
    // add
    data.add(new Instance(1.0, vals));

    // 4. output data
    System.out.println(data);
  }
}
