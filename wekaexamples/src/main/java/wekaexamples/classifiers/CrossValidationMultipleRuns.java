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
 *    CrossValidationMultipleRuns.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *
 */

package wekaexamples.classifiers;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ConverterUtils.DataSource;

import java.util.Random;

/**
 * Performs multiple runs of cross-validation.
 *
 * Command-line parameters:
 * <ul>
 *    <li>-t filename - the dataset to use</li>
 *    <li>-x int - the number of folds to use</li>
 *    <li>-r int - the number of runs to perform</li>
 *    <li>-c int - the class index, "first" and "last" are accepted as well;
 *    "last" is used by default</li>
 *    <li>-W classifier - classname and options, enclosed by double quotes; 
 *    the classifier to cross-validate</li>
 * </ul>
 *
 * Example command-line:
 * <pre>
 * java wekaexamples.classifiers.CrossValidationMultipleRuns -t labor.arff -c last -x 10 -r 10 -W "weka.classifiers.trees.J48 -C 0.25"
 * </pre>
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class CrossValidationMultipleRuns {

  /**
   * Performs the cross-validation. See Javadoc of class for information
   * on command-line parameters.
   *
   * @param args	the command-line parameters
   * @throws Exception	if something goes wrong
   */
  public static void main(String[] args) throws Exception {
    // loads data and set class index
    Instances data = DataSource.read(Utils.getOption("t", args));
    String clsIndex = Utils.getOption("c", args);
    if (clsIndex.length() == 0)
      clsIndex = "last";
    if (clsIndex.equals("first"))
      data.setClassIndex(0);
    else if (clsIndex.equals("last"))
      data.setClassIndex(data.numAttributes() - 1);
    else
      data.setClassIndex(Integer.parseInt(clsIndex) - 1);

    // classifier
    String[] tmpOptions;
    String classname;
    tmpOptions     = Utils.splitOptions(Utils.getOption("W", args));
    classname      = tmpOptions[0];
    tmpOptions[0]  = "";
    Classifier cls = (Classifier) Utils.forName(Classifier.class, classname, tmpOptions);

    // other options
    int runs  = Integer.parseInt(Utils.getOption("r", args));
    int folds = Integer.parseInt(Utils.getOption("x", args));

    // perform cross-validation
    for (int i = 0; i < runs; i++) {
      // randomize data
      int seed = i + 1;
      Random rand = new Random(seed);
      Instances randData = new Instances(data);
      randData.randomize(rand);
      if (randData.classAttribute().isNominal())
        randData.stratify(folds);

      Evaluation eval = new Evaluation(randData);
      for (int n = 0; n < folds; n++) {
        Instances train = randData.trainCV(folds, n);
        Instances test = randData.testCV(folds, n);
        // the above code is used by the StratifiedRemoveFolds filter, the
        // code below by the Explorer/Experimenter:
        // Instances train = randData.trainCV(folds, n, rand);

        // build and evaluate classifier
        Classifier clsCopy = AbstractClassifier.makeCopy(cls);
        clsCopy.buildClassifier(train);
        eval.evaluateModel(clsCopy, test);
      }

      // output evaluation
      System.out.println();
      System.out.println("=== Setup run " + (i+1) + " ===");
      System.out.println("Classifier: " + Utils.toCommandLine(cls));
      System.out.println("Dataset: " + data.relationName());
      System.out.println("Folds: " + folds);
      System.out.println("Seed: " + seed);
      System.out.println();
      System.out.println(eval.toSummaryString("=== " + folds + "-fold Cross-validation run " + (i+1) + "===", false));
    }
  }
}
