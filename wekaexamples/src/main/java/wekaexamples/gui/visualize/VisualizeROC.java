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
 * VisualizeROC.java
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package wekaexamples.gui.visualize;

import weka.classifiers.evaluation.ThresholdCurve;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ConverterUtils.DataSource;
import weka.gui.visualize.PlotData2D;
import weka.gui.visualize.ThresholdVisualizePanel;

import java.awt.BorderLayout;

/**
 * Visualizes a previously saved ROC curve. Code taken from the 
 * <code>weka.gui.explorer.ClassifierPanel</code> - involved methods:
 * <ul>
 *    <li>visualize(String,int,int)</li>
 *    </li>visualizeClassifierErrors(VisualizePanel)</li>
 * </ul>
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 * @see weka.gui.explorer.ClassifierPanel
 */
public class VisualizeROC {
  
  /**
   * Takes one argument: previously saved ROC curve data (ARFF file).
   * 
   * @param args	the commandline arguments
   * @throws Exception	if something goes wrong
   */
  public static void main(String[] args) throws Exception {
    Instances curve = DataSource.read(args[0]);
    curve.setClassIndex(curve.numAttributes() - 1);
    
    // method visualize
    ThresholdVisualizePanel tvp = new ThresholdVisualizePanel();
    tvp.setROCString("(Area under ROC = " + 
        Utils.doubleToString(ThresholdCurve.getROCArea(curve), 4) + ")");
    tvp.setName(curve.relationName());
    PlotData2D plotdata = new PlotData2D(curve);
    plotdata.setPlotName(curve.relationName());
    plotdata.addInstanceNumberAttribute();
    tvp.addPlot(plotdata);
    
    // method visualizeClassifierErrors
    final javax.swing.JFrame jf = new javax.swing.JFrame("WEKA ROC: " + tvp.getName());
    jf.setSize(500,400);
    jf.getContentPane().setLayout(new BorderLayout());
    jf.getContentPane().add(tvp, BorderLayout.CENTER);
    jf.addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowClosing(java.awt.event.WindowEvent e) {
	jf.dispose();
      }
    });
    jf.setVisible(true);
  }
}
