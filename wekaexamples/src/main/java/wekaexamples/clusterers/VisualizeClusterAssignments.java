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
 * VisualizeClusterAssignments.java
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package wekaexamples.clusterers;

import weka.clusterers.AbstractClusterer;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.Clusterer;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ConverterUtils.DataSource;
import weka.gui.explorer.ClustererPanel;
import weka.gui.visualize.PlotData2D;
import weka.gui.visualize.VisualizePanel;

import java.awt.BorderLayout;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JFrame;

/**
 * Runs a clusterer on a dataset and visualizes the cluster assignments, 
 * like with right-click menu in Explorer.
 * <p/>
 * Takes two arguments:
 * <ol>
 *   <li>-t dataset</li>
 *   <li>-W cluster algorithm with options</li>
 * </ol>
 * 
 * @author  FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class VisualizeClusterAssignments {
  
  /**
   * Takes two arguments.
   * <ol>
   *   <li>-t dataset</li>
   *   <li>-W cluster algorithm with options</li>
   * </ol>
   * 
   * @param args	the commandline arguments
   * @throws Exception	if something goes wrong
   */
  public static void main(String[] args) throws Exception {
    // load data
    Instances train = DataSource.read(Utils.getOption('t', args));
    // some data formats store the class attribute information as well
    if (train.classIndex() != -1)
      throw new IllegalArgumentException("Data cannot have class attribute!");

    // instantiate clusterer
    String[] options = Utils.splitOptions(Utils.getOption('W', args));
    String classname = options[0];
    options[0] = "";
    Clusterer clusterer = AbstractClusterer.forName(classname, options);
    
    // evaluate clusterer
    clusterer.buildClusterer(train);
    ClusterEvaluation eval = new ClusterEvaluation();
    eval.setClusterer(clusterer);
    eval.evaluateClusterer(train);

    // setup visualization
    // taken from: ClustererPanel.startClusterer()
    PlotData2D predData = ClustererPanel.setUpVisualizableInstances(train, eval);
    String name = (new SimpleDateFormat("HH:mm:ss - ")).format(new Date());
    String cname = clusterer.getClass().getName();
    if (cname.startsWith("weka.clusterers."))
      name += cname.substring("weka.clusterers.".length());
    else
      name += cname;
    name = name + " (" + train.relationName() + ")";

    VisualizePanel vp = new VisualizePanel();
    vp.setName(name);
    predData.setPlotName(name);
    vp.addPlot(predData);

    // display data
    // taken from: ClustererPanel.visualizeClusterAssignments(VisualizePanel)
    JFrame jf = new JFrame("Weka Clusterer Visualize: " + vp.getName());
    jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    jf.setSize(500, 400);
    jf.getContentPane().setLayout(new BorderLayout());
    jf.getContentPane().add(vp, BorderLayout.CENTER);
    jf.setVisible(true);
  }
}
