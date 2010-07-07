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
 * VisualizeJ48.java
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package wekaexamples.gui.treevisualizer;

import weka.classifiers.trees.J48;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.gui.treevisualizer.PlaceNode2;
import weka.gui.treevisualizer.TreeVisualizer;

import java.awt.BorderLayout;

import javax.swing.JFrame;

/**
 * Displays a trained J48 as tree.
 * Expects an ARFF filename as first argument.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class VisualizeJ48 {
  
  /**
   * Expects an ARFF file as first parameter (class attribute is assumed to 
   * be the last attribute).
   * 
   * @param args	the commandline arguments
   * @throws Exception	if something goes wrong
   */
  public static void main(String args[]) throws Exception {
    // train classifier
    J48 cls = new J48();
    Instances data = DataSource.read(args[0]);
    data.setClassIndex(data.numAttributes() - 1);
    cls.buildClassifier(data);
    
    // display tree
    TreeVisualizer tv = new TreeVisualizer(null, cls.graph(), new PlaceNode2());
    JFrame jf = new JFrame("Weka Classifier Tree Visualizer: J48");
    jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    jf.setSize(800, 600);
    jf.getContentPane().setLayout(new BorderLayout());
    jf.getContentPane().add(tv, BorderLayout.CENTER);
    jf.setVisible(true);
    
    // adjust tree
    tv.fitToScreen();
  }
}

