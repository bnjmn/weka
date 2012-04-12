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

package wekaexamples.gui.visualize.plugins;

import weka.classifiers.evaluation.NominalPrediction;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.gui.visualize.plugins.VisualizePlugin;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.math.plot.Plot2DPanel;

/**
 * A panel that displays the prediction errors.
 * 
 * @author  peter (peter at waikato dot ac dot nz)
 * @version $Revision$
 */
public class PredictionError
  implements VisualizePlugin {

  /**
   * Get a JMenu or JMenuItem which contain action listeners
   * that perform the visualization, using some but not
   * necessarily all of the data.  Exceptions thrown because of
   * changes in Weka since compilation need to be caught by
   * the implementer.
   *
   * @see NoClassDefFoundError
   * @see IncompatibleClassChangeError
   *
   * @param  preds 	predictions
   * @param  classAtt 	class attribute
   * @return 		menuitem for opening visualization(s), or null
   *         		to indicate no visualization is applicable for the input
   */
  public JMenuItem getVisualizeMenuItem(FastVector preds, Attribute classAtt) {
    final FastVector finalPreds = preds;
    final Attribute finalClassAtt = classAtt;
    
    // only for nominal classes
    if (!classAtt.isNominal())
      return null;
    
    JMenuItem result = new JMenuItem("Prediction error");
    result.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent arg0) {
	display(finalPreds, finalClassAtt);
      }
    });
    
    return result;
  }

  /**
   * Get the minimum version of Weka, inclusive, the class
   * is designed to work with.  eg: <code>3.5.0</code>
   * 
   * @return 		the minimum version
   */
  public String getMinVersion() {
    return "3.5.3";
  }

  /**
   * Get the maximum version of Weka, exclusive, the class
   * is designed to work with.  eg: <code>3.6.0</code>
   * 
   * @return 		the maximum version
   */
  public String getMaxVersion() {
    return "3.6.0";
  }

  /**
   * Get the specific version of Weka the class is designed for.
   * eg: <code>3.5.1</code>
   * 
   * @return 		the version it was designed for
   */
  public String getDesignVersion() {
    return "3.5.7";
  }
  
  /**
   * Displays the prediction error.
   * 
   * @param preds	the predictions to display
   * @param classAtt	the class attribute
   */
  protected void display(FastVector preds, Attribute classAtt) {
    double[] 		x;
    double[] 		y;
    Vector<Double>	xVals;
    Vector<Double>	yVals;
    Plot2DPanel 	plot;
    JFrame 		frame;
    NominalPrediction	pred;
    int			i;
    int			n;

    if (preds == null) {
      JOptionPane.showMessageDialog(null, "No predictions to display!");
      return;
    }

    // setup plot
    plot = new Plot2DPanel();
    plot.addLegend("SOUTH");
    for (n = 1; n <= 2; n++) {
      // collect data: 1=correct, 2=incorrect predictions
      xVals = new Vector<Double>();
      yVals = new Vector<Double>();
      for (i = 0; i < preds.size(); i++) {
	pred = (NominalPrediction) preds.elementAt(i);
	if (n == 1) {
	  if (pred.actual() == pred.predicted()) {
	    xVals.add((double) i);
	    yVals.add(pred.distribution()[(int) pred.actual()]);
	  }
	}
	else {
	  if (pred.actual() != pred.predicted()) {
	    xVals.add((double) i);
	    yVals.add(pred.distribution()[(int) pred.actual()]);
	  }
	}
      }
      
      // transfer into arrays
      x = new double[xVals.size()];
      y = new double[yVals.size()];
      for (i = 0; i < x.length; i++) {
	x[i] = xVals.get(i);
	y[i] = yVals.get(i);
      }
      
      // add plot
      if (n == 1)
	plot.addBarPlot("Correct", x, y);
      else
	plot.addBarPlot("Incorrect", x, y);
    }

    // setup frame
    frame = new JFrame("Prediction error");
    frame.setSize(600, 600);
    frame.setVisible(true);
    frame.getContentPane().setLayout(new BorderLayout());
    frame.getContentPane().add(plot, BorderLayout.CENTER);
    frame.getContentPane().add(new JLabel("Displays the probability the classifier returns for the actual class label."), BorderLayout.SOUTH);
  }
}
