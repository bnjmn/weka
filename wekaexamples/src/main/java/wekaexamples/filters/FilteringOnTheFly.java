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
 * FilteringOnTheFly.java
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package wekaexamples.filters;

import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.J48;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.unsupervised.attribute.Remove;

/**
 * Example class to demonstrate filtering on-the-fly using the 
 * FilteredClassifier meta-classifier. The Remove filter (removing the first
 * attribute) and the J48 classifier are used in this setup.
 * 
 * @author  FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class FilteringOnTheFly {

  /**
   * Expects two parameters: training and test file.
   * It is assumed that the class attribute is the last attribute in the
   * dataset.
   * 
   * @param args	the commandline arguments
   * @throws Exception	if something goes wrong
   */
  public static void main(String[] args) throws Exception {
    // load data
    Instances train = DataSource.read(args[0]);
    Instances test = DataSource.read(args[1]);
    train.setClassIndex(train.numAttributes() - 1);
    test.setClassIndex(test.numAttributes() - 1);
    if (!train.equalHeaders(test))
      throw new IllegalArgumentException(
	  "Datasets are not compatible:\n" + train.equalHeadersMsg(test));
    
    // filter
    Remove rm = new Remove();
    rm.setAttributeIndices("1");  // remove 1st attribute
    
    // classifier
    J48 j48 = new J48();
    j48.setUnpruned(true);        // using an unpruned J48
    
    // meta-classifier
    FilteredClassifier fc = new FilteredClassifier();
    fc.setFilter(rm);
    fc.setClassifier(j48);
    
    // train and make predictions
    fc.buildClassifier(train);
    for (int i = 0; i < test.numInstances(); i++) {
      double pred = fc.classifyInstance(test.instance(i));
      System.out.print("ID: " + test.instance(i).value(0));
      System.out.print(", actual: " + test.classAttribute().value((int) test.instance(i).classValue()));
      System.out.println(", predicted: " + test.classAttribute().value((int) pred));
    }
  }
}
