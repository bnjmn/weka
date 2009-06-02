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
 * VisualizeMultipleROC.java
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package wekaexamples.gui.visualize;

import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.gui.visualize.PlotData2D;
import weka.gui.visualize.ThresholdVisualizePanel;

import java.awt.BorderLayout;

import javax.swing.JFrame;

/**
 * Visualizes previously saved ROC curves.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 * @see VisualizeROC
 */
public class VisualizeMultipleROC {

  /**
   * Takes arbitraty number of arguments: 
   * previously saved ROC curve data (ARFF file)
   * 
   * @param args	the commandline arguments
   * @throws Exception	if something goes wrong
   */
  public static void main(String[] args) throws Exception {
    boolean first = true;
    ThresholdVisualizePanel tvp = new ThresholdVisualizePanel();
    for (int i = 0; i < args.length; i++) {
      Instances curve = DataSource.read(args[i]);
      curve.setClassIndex(curve.numAttributes() - 1);
      // method visualize
      PlotData2D plotdata = new PlotData2D(curve);
      plotdata.setPlotName(curve.relationName());
      plotdata.addInstanceNumberAttribute();
      if (first)
	tvp.setMasterPlot(plotdata);
      else
	tvp.addPlot(plotdata);
      first = false;
    }
    // method visualizeClassifierErrors
    final JFrame jf = new JFrame("WEKA ROC");
    jf.setSize(500,400);
    jf.getContentPane().setLayout(new BorderLayout());
    jf.getContentPane().add(tvp, BorderLayout.CENTER);
    jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    jf.setVisible(true);
  }
}
