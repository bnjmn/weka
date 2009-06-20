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
 * VisualizeBayesNet.java
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package wekaexamples.gui.graphvisualizer;

import weka.classifiers.bayes.BayesNet;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.gui.graphvisualizer.GraphVisualizer;

import java.awt.BorderLayout;
import java.io.ByteArrayInputStream;

import javax.swing.JFrame;

/**
 * Displays a trained BayesNet graph.
 * Expects an ARFF filename as first argument.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class VisualizeBayesNet {
  
  /**
   * Expects an ARFF file as first parameter (class attribute is assumed to 
   * be the last attribute).
   * 
   * @param args	the commandline arguments
   * @throws Exception	if something goes wrong
   */
  public static void main(String args[]) throws Exception {
    // train classifier
    BayesNet cls = new BayesNet();
    Instances data = DataSource.read(args[0]);
    data.setClassIndex(data.numAttributes() - 1);
    cls.buildClassifier(data);
    
    // display graph
    GraphVisualizer gv = new GraphVisualizer();
    gv.readBIF(new ByteArrayInputStream(cls.graph().getBytes()));
    JFrame jf = new JFrame("BayesNet graph");
    jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    jf.setSize(800, 600);
    jf.getContentPane().setLayout(new BorderLayout());
    jf.getContentPane().add(gv, BorderLayout.CENTER);
    jf.setVisible(true);
    // layout graph
    gv.layoutGraph();
  }
}

