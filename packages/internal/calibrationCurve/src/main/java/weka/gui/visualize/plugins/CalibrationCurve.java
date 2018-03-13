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
package weka.gui.visualize.plugins;

import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.evaluation.Prediction;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Discretize;
import weka.gui.visualize.Plot2D;
import weka.gui.visualize.PlotData2D;
import weka.gui.visualize.VisualizePanel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

/**
 * Displays probability calibration curve for a chosen class value.
 *
 * @author eibe
 * @version $Revision$
 */
public class CalibrationCurve implements VisualizePlugin {

  /**
   * Get the specific version of Weka the class is designed for. eg:
   * <code>3.5.1</code>
   *
   * @return the version string
   */
  public String getDesignVersion() {
    return "3.8.2";
  }

  /**
   * Get the minimum version of Weka, inclusive, the class is designed to work
   * with. eg: <code>3.5.0</code>
   *
   * @return the version string
   */
  public String getMinVersion() {
    return "3.8.0";
  }

  /**
   * Get the maximum version of Weka, exclusive, the class is designed to work
   * with. eg: <code>3.6.0</code>
   *
   * @return the version string
   */
  public String getMaxVersion() {
    return "100.100.100";
  }

  /**
   * Get a JMenu or JMenuItem which contain action listeners that perform the
   * visualization
   *
   * @param preds predictions
   * @param classAtt class attribute
   * @return menuitem for opening visualization(s), or null to indicate no
   *         visualization is applicable for the input
   */
  public JMenuItem getVisualizeMenuItem(ArrayList<Prediction> preds,
    Attribute classAtt) {
    final ArrayList<Prediction> finalPreds = preds;
    final Attribute finalClassAtt = classAtt;

    // only for nominal classes
    if (!classAtt.isNominal())
      return null;

    JMenu result = new JMenu("Calibration curve");
    if ((preds != null) && (classAtt != null) && (classAtt.isNominal())) {
      for (int i = 0; i < classAtt.numValues(); i++) {
        JMenuItem clv = new JMenuItem(classAtt.value(i));
        final int classValue = i;
        clv.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            try {
              display(finalPreds, finalClassAtt, classValue);
            } catch (Exception ex) {
              ex.printStackTrace();
            }
          }
        });
        result.add(clv);
      }
    }

    return result;
  }

  /**
   * Displays the calibration curve.
   *
   * @param preds the predictions to plot
   * @param classAtt the class attribute
   * @param classValue the class value
   */
  protected void display(ArrayList<Prediction> preds, Attribute classAtt,
    int classValue) {

    if (preds == null) {
      JOptionPane.showMessageDialog(null, "No data available for display!");
      return;
    }

    try {
      // Collect data for plotting, making sure that 0,0 and 1,1 are included as
      // invisible points
      List<Object> curveData =
        CalibrationCurveUtils.getCalibrationCurveData(preds, classAtt,
          classValue);

      Instances cdata = (Instances) curveData.get(0);
      int numBins = (int) curveData.get(1);

      int[] shapeType = new int[numBins + 2];
      boolean[] connectPoint = new boolean[numBins + 2];
      for (int i = 0; i < numBins; i++) {
        shapeType[i] = Plot2D.PLUS_SHAPE;
        connectPoint[i] = true;
      }
      shapeType[shapeType.length - 2] = -2; // Hack to make sure that corner
                                            // points are invisible
      shapeType[shapeType.length - 1] = -2;

      PlotData2D plotInfo = new PlotData2D(cdata);
      plotInfo.setConnectPoints(connectPoint);
      plotInfo.setShapeType(shapeType);
      plotInfo.setPlotName("Calibration curve for class value " + classAtt.value(classValue));
      VisualizePanel vp = new VisualizePanel();
      vp.setName("Calibration curve (x: estimated probability, y: observed probability) for "
        + classAtt.value(classValue)
        + " based on"
        + " "
        + numBins
        + " equal-frequency bins");
      vp.setMasterPlot(plotInfo);

      JFrame frame =
        new JFrame(
          "Calibration curve (x: estimated probability, y: observed probability) for "
            + classAtt.value(classValue) + " based on" + " " + numBins
            + " equal-frequency bins");
      frame.setSize(1024, 800);
      frame.setContentPane(vp);
      frame.setVisible(true);

    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
