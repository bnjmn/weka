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
 *    UsingJ48Ext.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *
 */

package wekaexamples.classifiers.trees;

import weka.classifiers.Evaluation
import weka.classifiers.trees.J48
import weka.core.converters.ConverterUtils.DataSource
import weka.classifiers.evaluation.output.prediction.PlainText
import weka.core.Instances
import weka.core.Range

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
  println "Usage: UsingJ48Ext.groovy <ARFF-file>"
  System.exit(0)
}

// load data and set class index
print "Loading data..."
data = DataSource.read(args[0])
data.setClassIndex(data.numAttributes() - 1)

// create the model
evaluation = new Evaluation(data)
output = new PlainText()  // plain text output for predictions
output.setHeader(data)
buffer = new StringBuffer() // buffer to use
output.setBuffer(buffer)
attRange = new Range()  // no additional attributes to output
outputDistribution = false  // we don't want distribution
j48 = new J48()
j48.buildClassifier(data)
evaluation.evaluateModel(j48, data, output, attRange, outputDistribution)

// print out the built model
println "--> Generated model:\n"
println j48

println "--> Evaluation:\n"
println evaluation.toSummaryString()

println "--> Predictions:\n"
println buffer

