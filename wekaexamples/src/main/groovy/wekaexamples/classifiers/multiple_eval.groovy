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
 *    multiple_eval.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *
 */

package wekaexamples.classifiers;

// A little Groovy script that evaluates several classifiers on multiple train/test pairs.
//
// Author: FracPete (fracpete at waikato dot ac dot nz)
// Version: $Revision$

import weka.classifiers.Evaluation
import weka.classifiers.Classifier
import weka.core.converters.ConverterUtils.DataSource
import weka.core.Utils

// list of training/test sets
training_dir  = "/directory/containing/training/sets"
training_sets = ["train1.arff", "train2.arff"]
test_dir      = "/directory/containing/test/sets"
test_sets     = ["test1.arff", "test2.arff"]
assert training_sets.size() == test_sets.size()

// list of classifiers
classifiers = ["weka.classifiers.trees.J48 -C 0.25", "weka.classifiers.trees.J48 -U", "weka.classifiers.functions.SMO -K \"weka.classifiers.functions.supportVector.PolyKernel -E 2\""]

// perform the evaluation
for (i in 0..(training_sets.size()-1)) {
  for (c in classifiers) {
    // progress info
    println "\n" + training_sets[i] + "/" + test_sets[i] + "/" + c

    // load datasets
    train = DataSource.read(training_dir + "/" + training_sets[i])
    if (train.classIndex() == -1) train.setClassIndex(train.numAttributes() - 1)
    test  = DataSource.read(test_dir + "/" + test_sets[i])
    if (test.classIndex() == -1) test.setClassIndex(test.numAttributes() - 1)
    // make sure they're compatible
    assert train.equalHeaders(test)

    // instantiate classifier
    options    = Utils.splitOptions(c)
    classname  = options[0]
    options[0] = ""
    cls        = Classifier.forName(classname, options)

    // build and evaluate classifier
    cls.buildClassifier(train)
    eval = new Evaluation(train)
    eval.evaluateModel(cls, test)

    // output statistics, e.g., Accuracy
    println "  Accuracy: " + eval.pctCorrect()
  }
}