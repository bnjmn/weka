import weka.clusterers.*;
import weka.core.*;
import weka.core.converters.ConverterUtils.*;
import weka.gui.explorer.ClustererPanel;
import weka.gui.visualize.*;

import java.awt.*;
import java.io.*;
import java.text.*;
import java.util.*;

import javax.swing.*;

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
 * Note: code should work with Weka 3.6.0 and 3.5.8.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class VisualizeClusterAssignments {
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

    VisualizePanel vp = new VisualizePanel();
    vp.setName(name + " (" + train.relationName() + ")");
    predData.setPlotName(name + " (" + train.relationName() + ")");
    vp.addPlot(predData);

    // display data
    // taken from: ClustererPanel.visualizeClusterAssignments(VisualizePanel)
    String plotName = vp.getName();
    final javax.swing.JFrame jf = 
      new javax.swing.JFrame("Weka Clusterer Visualize: " + plotName);
    jf.setSize(500,400);
    jf.getContentPane().setLayout(new BorderLayout());
    jf.getContentPane().add(vp, BorderLayout.CENTER);
    jf.addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowClosing(java.awt.event.WindowEvent e) {
        jf.dispose();
      }
    });
    jf.setVisible(true);
  }
}
