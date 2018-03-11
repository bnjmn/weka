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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.*;

/**
 * Displays probability calibration curve for a chosen class value.
 *
 * @author  eibe
 * @version $Revision$
 */
public class CalibrationCurve implements VisualizePlugin {

    /**
     * Get the specific version of Weka the class is designed for.
     * eg: <code>3.5.1</code>
     *
     * @return the version string
     */
    public String getDesignVersion() {
        return "3.8.2";
    }

    /**
     * Get the minimum version of Weka, inclusive, the class
     * is designed to work with.  eg: <code>3.5.0</code>
     *
     * @return the version string
     */
    public String getMinVersion() {
        return "3.8.0";
    }

    /**
     * Get the maximum version of Weka, exclusive, the class
     * is designed to work with.  eg: <code>3.6.0</code>
     *
     * @return the version string
     */
    public String getMaxVersion() {
        return "100.100.100";
    }

    /**
     * Get a JMenu or JMenuItem which contain action listeners
     * that perform the visualization
     *
     * @param preds    predictions
     * @param classAtt class attribute
     * @return menuitem for opening visualization(s), or null
     * to indicate no visualization is applicable for the input
     */
    public JMenuItem getVisualizeMenuItem(ArrayList<Prediction> preds, Attribute classAtt) {
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
     * @param preds    the predictions to plot
     * @param classAtt the class attribute
     * @param classValue the class value
     */
    protected void display(ArrayList<Prediction> preds, Attribute classAtt, int classValue) {

        if (preds == null) {
            JOptionPane.showMessageDialog(null, "No data available for display!");
            return;
        }

        // Remove prediction objects where either the prediction or the actual value are missing
        ArrayList<Prediction> newPreds = new ArrayList<>();
        for (Prediction p : preds) {
            if (!Utils.isMissingValue(p.actual()) && !Utils.isMissingValue(p.predicted())) {
                newPreds.add(p);
            }
        }
        preds = newPreds;

        ArrayList<Attribute> attributes = new ArrayList<>(1);
        attributes.add(new Attribute("class_prob"));
        Instances data = new Instances("class_probabilities", attributes, preds.size());

        for (int i = 0; i < preds.size(); i++) {
            double[] inst = { ((NominalPrediction)preds.get(i)).distribution()[classValue] };
            data.add(new DenseInstance(preds.get(i).weight(), inst ));
        }

        try {
            Discretize d = new Discretize();
            d.setUseEqualFrequency(true);
            d.setBins(Integer.max(1, (int)Math.round(Math.sqrt(data.sumOfWeights()))));
            d.setUseBinNumbers(true);
            d.setInputFormat(data);
            data = Filter.useFilter(data, d);

            int numBins = data.attribute(0).numValues();
            double[] sumClassProb = new double[numBins];
            double[] sumTrueClass = new double[numBins];
            double[] sizeOfBin = new double[numBins];
            for (int i = 0; i < data.numInstances(); i++) {
                int binIndex = (int) data.instance(i).value(0);
                sizeOfBin[binIndex] += preds.get(i).weight();
                sumTrueClass[binIndex] += preds.get(i).weight() *
                        ((((int) preds.get(i).actual()) == classValue) ? 1.0 : 0.0);
                sumClassProb[binIndex] += preds.get(i).weight() *
                        ((NominalPrediction) preds.get(i)).distribution()[classValue];
            }

            ArrayList<Attribute> atts = new ArrayList<>(1);
            atts.add(new Attribute("average_class_prob"));
            atts.add(new Attribute("average_true_class_value"));

            // Collect data for plotting, making sure that 0,0 and 1,1 are included as invisible points
            Instances cdata = new Instances("calibration_curve_data", atts, numBins + 2);
            int[] shapeType = new int[numBins + 2];
            boolean[] connectPoint = new boolean[numBins + 2];
            for (int i = 0; i < numBins; i++) {
                double[] v = new double[2];
                v[0] = sumClassProb[i] / sizeOfBin[i];
                v[1] = sumTrueClass[i] / sizeOfBin[i];
                cdata.add(new DenseInstance(sizeOfBin[i], v));
                shapeType[i] = Plot2D.PLUS_SHAPE;
                connectPoint[i] = true;
            }
            double[] zero = new double[2];
            double[] one = new double[2];
            one[0] = 1.0;
            one[1] = 1.0;
            cdata.add(new DenseInstance(0.0, zero));
            cdata.add(new DenseInstance(0.0, one));
            shapeType[shapeType.length - 2] = -2; // Hack to make sure that corner points are invisible
            shapeType[shapeType.length - 1] = -2;

            PlotData2D plotInfo = new PlotData2D(cdata);
            plotInfo.setConnectPoints(connectPoint);
            plotInfo.setShapeType(shapeType);
            Plot2D plot = new Plot2D();
            plotInfo.setPlotName("\"Calibration curve for class value \" + classAtt.value(classValue)");
            plot.setMasterPlot(plotInfo);
            plot.setXindex(0);
            plot.setYindex(1);

            JFrame frame = new JFrame("Calibration curve (x: estimated probability, y: observed probability) for " +
                    classAtt.value(classValue) + " based on" +
                    " " + numBins + " equal-frequency bins");
            frame.setSize(1024, 800);
            frame.setContentPane(plot);
            frame.setVisible(true);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
