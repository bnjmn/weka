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
 * ClassifierErrorsWeka.java
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package wekaexamples.gui.visualize.plugins;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JMenuItem;

import weka.core.Instances;
import weka.core.Utils;
import weka.gui.visualize.Plot2D;
import weka.gui.visualize.PlotData2D;
import weka.gui.visualize.VisualizePanel;
import weka.gui.visualize.plugins.ErrorVisualizePlugin;

/**
 * Example class for displaying the classifier errors using Weka panels.
 * 
 * @author fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class ClassifierErrorsWeka implements ErrorVisualizePlugin {

  /**
   * Post processes numeric class errors into shape sizes for plotting in the
   * visualize panel.
   * 
   * @param plotSize an ArrayList of numeric class errors
   */
  protected void postProcessPlotInfo(ArrayList<Object> plotSize) {
    int maxpSize = 20;
    double maxErr = Double.NEGATIVE_INFINITY;
    double minErr = Double.POSITIVE_INFINITY;
    double err;

    for (int i = 0; i < plotSize.size(); i++) {
      Double errd = (Double) plotSize.get(i);
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
      Double errd = (Double) plotSize.get(i);
      if (errd != null) {
        err = Math.abs(errd.doubleValue());
        if (maxErr - minErr > 0) {
          double temp = (((err - minErr) / (maxErr - minErr)) * maxpSize);
          plotSize.set(i, new Integer((int) temp));
        } else {
          plotSize.set(i, new Integer(1));
        }
      } else {
        plotSize.set(i, new Integer(1));
      }
    }
  }

  /**
   * Get a JMenu or JMenuItem which contain action listeners that perform the
   * visualization of the classifier errors.
   * <p/>
   * The actual class is the attribute declared as class attribute, the
   * predicted class values is found in the attribute prior to the class
   * attribute's position. In other words, if the <code>classIndex()</code>
   * method returns 10, then the attribute position for the predicted class
   * values is 9.
   * <p/>
   * Exceptions thrown because of changes in Weka since compilation need to be
   * caught by the implementer.
   * 
   * @see NoClassDefFoundError
   * @see IncompatibleClassChangeError
   * 
   * @param predInst the instances with the actual and predicted class values
   * @return menuitem for opening visualization(s), or null to indicate no
   *         visualization is applicable for the input
   */
  @Override
  public JMenuItem getVisualizeMenuItem(Instances predInst) {
    final Instances predInstF = predInst;
    JMenuItem result = new JMenuItem("Classifier errors (Weka panels)");
    result.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        // setup visualize panel
        VisualizePanel vp = new VisualizePanel();
        vp.setName("Classifier errors for " + predInstF.relationName());
        PlotData2D tempd = new PlotData2D(predInstF);
        ArrayList<Object> plotSize = new ArrayList<Object>();
        ArrayList<Integer> plotShape = new ArrayList<Integer>();
        for (int i = 0; i < predInstF.numInstances(); i++) {
          double actual = predInstF.instance(i).value(predInstF.classIndex());
          double predicted = predInstF.instance(i).value(
            predInstF.classIndex() - 1);
          if (predInstF.classAttribute().isNominal()) {
            if (Utils.isMissingValue(actual) || Utils.isMissingValue(predicted)) {
              plotShape.add(new Integer(Plot2D.MISSING_SHAPE));
            } else if (actual != predicted) {
              // set to default error point shape
              plotShape.add(new Integer(Plot2D.ERROR_SHAPE));
            } else {
              // otherwise set to constant (automatically assigned) point shape
              plotShape.add(new Integer(Plot2D.CONST_AUTOMATIC_SHAPE));
            }
            plotSize.add(new Integer(Plot2D.DEFAULT_SHAPE_SIZE));
          } else {
            // store the error (to be converted to a point size later)
            Double errd = null;
            if (Utils.isMissingValue(actual) || Utils.isMissingValue(predicted)) {
              // missing shape if actual class not present or prediction is
              // missing
              plotShape.add(new Integer(Plot2D.MISSING_SHAPE));
            } else {
              errd = new Double(predicted - actual);
              plotShape.add(new Integer(Plot2D.CONST_AUTOMATIC_SHAPE));
            }
            plotSize.add(errd);
          }
        }
        try {
          if (predInstF.attribute(predInstF.classIndex()).isNumeric()) {
            postProcessPlotInfo(plotSize);
          }
          tempd.setShapeSize(plotSize);
          tempd.setShapeType(plotShape);
          tempd.setPlotName("Classifier errors for " + predInstF.relationName());
          tempd.addInstanceNumberAttribute();

          vp.addPlot(tempd);
          vp.setColourIndex(predInstF.classIndex() + 1);
        } catch (Exception ex) {
          ex.printStackTrace();
          return;
        }
        // pre-select class and predicted class
        try {
          vp.setXIndex(vp.getInstances().classIndex()); // class
          vp.setYIndex(vp.getInstances().classIndex() - 1); // predicted class
        } catch (Exception ex) {
          // ignored
        }

        // create and display frame
        final JFrame jf = new JFrame("Classifier errors for "
          + predInstF.relationName());
        jf.setSize(600, 400);
        jf.getContentPane().setLayout(new BorderLayout());
        jf.getContentPane().add(vp, BorderLayout.CENTER);
        jf.addWindowListener(new WindowAdapter() {
          @Override
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
   * Get the minimum version of Weka, inclusive, the class is designed to work
   * with. eg: <code>3.5.0</code>
   * 
   * @return the minimum version
   */
  @Override
  public String getMinVersion() {
    return "3.5.9";
  }

  /**
   * Get the maximum version of Weka, exclusive, the class is designed to work
   * with. eg: <code>3.6.0</code>
   * 
   * @return the maximum version
   */
  @Override
  public String getMaxVersion() {
    return "3.8.0";
  }

  /**
   * Get the specific version of Weka the class is designed for. eg:
   * <code>3.5.1</code>
   * 
   * @return the version the plugin was designed for
   */
  @Override
  public String getDesignVersion() {
    return "3.6.0";
  }
}
