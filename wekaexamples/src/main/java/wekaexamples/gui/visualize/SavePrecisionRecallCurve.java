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
 * SavePrecisionRecallCurve.java
 * Copyright (C) 2009,2014 University of Waikato, Hamilton, New Zealand
 */

package wekaexamples.gui.visualize;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ConverterUtils.DataSource;
import weka.gui.visualize.PlotData2D;
import weka.gui.visualize.ThresholdVisualizePanel;
import weka.gui.visualize.JComponentWriter;
import weka.gui.visualize.JPEGWriter;

import java.io.File;

import java.util.Random;

import javax.swing.JFrame;
 
/**
 * Generates and saves a precision-recall curve. Uses a cross-validation
 * with NaiveBayes to make the curve.
 *
 * @author FracPete
 * @version $Revision: 5663 $
 * @author Eibe Frank
 */
public class SavePrecisionRecallCurve {
 
  /**
   * takes two arguments: dataset in ARFF format (expects class to 
   * be last attribute) and name of file with output
   */
  public static void main(String[] args) throws Exception {

    // load data
    Instances data = DataSource.read(args[0]);
    data.setClassIndex(data.numAttributes() - 1);
 
    // train classifier
    Classifier cl = new NaiveBayes();
    Evaluation eval = new Evaluation(data);
    eval.crossValidateModel(cl, data, 10, new Random(1));

    // generate curve
    ThresholdCurve tc = new ThresholdCurve();
    int classIndex = 0;
    Instances result = tc.getCurve(eval.predictions(), classIndex);
 
    // plot curve
    ThresholdVisualizePanel vmc = new ThresholdVisualizePanel();
    PlotData2D tempd = new PlotData2D(result);

    // specify which points are connected
    boolean[] cp = new boolean[result.numInstances()];
    for (int n = 1; n < cp.length; n++)
      cp[n] = true;
    tempd.setConnectPoints(cp);
    // add plot
    vmc.addPlot(tempd);

    // We want a precision-recall curve
    vmc.setXIndex(result.attribute("Recall").index()); // ID has been added
    vmc.setYIndex(result.attribute("Precision").index()); // ID has been added

    // Make window with plot but don't show it
    JFrame jf =  new JFrame();
    jf.setSize(500,400);
    jf.getContentPane().add(vmc);
    jf.pack();
    
    // Save to file specified as second argument (can use any of 
    // BMPWriter, JPEGWriter, PNGWriter, PostscriptWriter for different formats)
    JComponentWriter jcw = new JPEGWriter(vmc.getPlotPanel(), new File(args[1]));
    jcw.toOutput();
    System.exit(1);
  }
}