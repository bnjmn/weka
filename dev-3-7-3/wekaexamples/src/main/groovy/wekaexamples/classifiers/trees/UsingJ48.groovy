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
 *    UsingJ48.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *
 */

package wekaexamples.classifiers.trees;

import weka.classifiers.trees.J48
import weka.core.converters.ConverterUtils.DataSource
import weka.core.Instances

/** 
 * An example of using J48 from within Groovy.
 * <p/>
 * First parameter is the dataset to be processed by J48. The last attribute
 * is used as class attribute.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */

if (args.size() == 0) {
  println "Usage: UsingJ48.groovy <ARFF-file>"
  System.exit(0)
}

// load data and set class index
data = DataSource.read(args[0])
data.setClassIndex(data.numAttributes() - 1)

// create the model
j48 = new J48()
j48.buildClassifier(data)

// print out the built model
println j48

