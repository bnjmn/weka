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
 * OutputClassDistribution.java
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package wekaexamples.classifiers;

import weka.classifiers.trees.J48;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ConverterUtils.DataSource;

/**
 * This example class trains a J48 classifier on a dataset and outputs for 
 * a second dataset the actual and predicted class label, as well as the 
 * class distribution.
 * 
 * @author  FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class OutputClassDistribution {

  /**
   * Expects two parameters: training file and test file.
   * 
   * @param args	the commandline arguments
   * @throws Exception	if something goes wrong
   */
  public static void main(String[] args) throws Exception {
    // load data
    Instances train = DataSource.read(args[0]);
    train.setClassIndex(train.numAttributes() - 1);
    Instances test = DataSource.read(args[1]);
    test.setClassIndex(test.numAttributes() - 1);
    if (!train.equalHeaders(test))
      throw new IllegalArgumentException(
	  "Train and test set are not compatible: " + train.equalHeadersMsg(test));
    
    // train classifier
    J48 cls = new J48();
    cls.buildClassifier(train);
    
    // output predictions
    System.out.println("# - actual - predicted - error - distribution");
    for (int i = 0; i < test.numInstances(); i++) {
      double pred = cls.classifyInstance(test.instance(i));
      double[] dist = cls.distributionForInstance(test.instance(i));
      System.out.print((i+1));
      System.out.print(" - ");
      System.out.print(test.instance(i).toString(test.classIndex()));
      System.out.print(" - ");
      System.out.print(test.classAttribute().value((int) pred));
      System.out.print(" - ");
      if (pred != test.instance(i).classValue())
	System.out.print("yes");
      else
	System.out.print("no");
      System.out.print(" - ");
      System.out.print(Utils.arrayToString(dist));
      System.out.println();
    }
  }
}
