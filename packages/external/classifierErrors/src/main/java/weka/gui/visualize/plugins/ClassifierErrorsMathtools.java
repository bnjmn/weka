/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * ClassifierErrorsMathtools.java
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package weka.gui.visualize.plugins;

import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Utils;
import weka.core.Instances;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JMenuItem;

import org.math.plot.Plot2DPanel;

/**
 * Example class for displaying the classifier errors using JMathtools.
 * 
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class ClassifierErrorsMathtools
  implements ErrorVisualizePlugin {

  /**
   * Post processes numeric class errors into shape sizes for plotting
   * in the visualize panel.
   * 
   * @param plotSize 	a FastVector of numeric class errors
   */
  protected void postProcessPlotInfo(FastVector plotSize) {
    int maxpSize = 20;
    double maxErr = Double.NEGATIVE_INFINITY;
    double minErr = Double.POSITIVE_INFINITY;
    double err;
    
    for (int i = 0; i < plotSize.size(); i++) {
      Double errd = (Double)plotSize.elementAt(i);
      if (errd != null) {
	err = Math.abs(errd.doubleValue());
        if (err < minErr) {
	  minErr = err;
	}
	if (err > maxErr) {
	  maxErr = err;
	}
      }
    }
    
    for (int i = 0; i < plotSize.size(); i++) {
      Double errd = (Double)plotSize.elementAt(i);
      if (errd != null) {
	err = Math.abs(errd.doubleValue());
	if (maxErr - minErr > 0) {
	  double temp = (((err - minErr) / (maxErr - minErr)) 
			 * maxpSize);
	  plotSize.setElementAt(new Integer((int)temp), i);
	} else {
	  plotSize.setElementAt(new Integer(1), i);
	}
      } else {
	plotSize.setElementAt(new Integer(1), i);
      }
    }
  }

  /**
   * Get a JMenu or JMenuItem which contain action listeners
   * that perform the visualization of the classifier errors.  
   * <p/>
   * The actual class is the attribute declared as class attribute, the
   * predicted class values is found in the attribute prior to the class
   * attribute's position. In other words, if the <code>classIndex()</code>
   * method returns 10, then the attribute position for the predicted class 
   * values is 9.
   * <p/>
   * Exceptions thrown because of changes in Weka since compilation need to 
   * be caught by the implementer.
   *
   * @see NoClassDefFoundError
   * @see IncompatibleClassChangeError
   *
   * @param predInst 	the instances with the actual and predicted class values
   * @return menuitem 	for opening visualization(s), or null
   *         		to indicate no visualization is applicable for the input
   */
  public JMenuItem getVisualizeMenuItem(Instances predInst) {
    if (!predInst.classAttribute().isNumeric())
      return null;
    
    final Instances predInstF = predInst;
    JMenuItem result = new JMenuItem("Classifier errors (JMathtools)");
    result.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	// obtain numbers
	FastVector plotSize = new FastVector();
	double[][] XY = new double[predInstF.numInstances()][2];
	double[][] dXdY = new double[predInstF.numInstances()][2];
	for (int i = 0; i < predInstF.numInstances(); i++) {
	  double actual = predInstF.instance(i).value(predInstF.classIndex());
	  double predicted = predInstF.instance(i).value(predInstF.classIndex() - 1);
	  // store the error (to be converted to a point size later)
	  Double errd = null;
	  if (!Utils.isMissingValue(actual) && !Utils.isMissingValue(predicted)) {
	    errd     = new Double(predicted - actual);
	    XY[i][0] = actual;
	    XY[i][1] = predicted;
	  }
	  plotSize.addElement(errd);
	}
	if (predInstF.attribute(predInstF.classIndex()).isNumeric())
	  postProcessPlotInfo(plotSize);
	
	// setup plot panel
	Plot2DPanel plot = new Plot2DPanel();
	for (int i = 0; i < predInstF.numInstances(); i++) {
	    dXdY[i][0] = 0;
	    dXdY[i][1] = (Integer) plotSize.elementAt(i);
	}
	plot.addBoxPlot("Errorplot", XY, dXdY);

	// create and display frame
	final JFrame jf = new JFrame("Classifier errors for " + predInstF.relationName());
	jf.setSize(600,400);
	jf.getContentPane().setLayout(new BorderLayout());
	jf.getContentPane().add(plot, BorderLayout.CENTER);
	jf.addWindowListener(new WindowAdapter() {
	  public void windowClosing(WindowEvent e) {
	    jf.dispose();
	  }
	});
	jf.setVisible(true);
      }
    });
    
    return result;
  }

  /**
   * Get the minimum version of Weka, inclusive, the class
   * is designed to work with.  eg: <code>3.5.0</code>
   * 
   * @return		the minimum version
   */
  public String getMinVersion() {
    return "3.5.9";
  }

  /**
   * Get the maximum version of Weka, exclusive, the class
   * is designed to work with.  eg: <code>3.6.0</code>
   * 
   * @return		the maximum version
   */
  public String getMaxVersion() {
    return "3.7.1";
  }

  /**
   * Get the specific version of Weka the class is designed for.
   * eg: <code>3.5.1</code>
   * 
   * @return		the version the plugin was designed for
   */
  public String getDesignVersion() {
    return "3.6.0";
  }
}
