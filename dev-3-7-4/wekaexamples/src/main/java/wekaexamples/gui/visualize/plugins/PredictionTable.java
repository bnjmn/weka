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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;

/**
 * Displays the predictions of the classifier in a JTable.
 * 
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class PredictionTable
  implements VisualizePlugin {

  /**
   * Get the specific version of Weka the class is designed for.
   * eg: <code>3.5.1</code>
   * 
   * @return		the version string
   */
  public String getDesignVersion() {
    return "3.5.7";
  }

  /**
   * Get the minimum version of Weka, inclusive, the class
   * is designed to work with.  eg: <code>3.5.0</code>
   * 
   * @return		the version string
   */
  public String getMinVersion() {
    return "3.5.3";
  }

  /**
   * Get the maximum version of Weka, exclusive, the class
   * is designed to work with.  eg: <code>3.6.0</code>
   * 
   * @return		the version string
   */
  public String getMaxVersion() {
    return "3.6.0";
  }

  /**
   * Get a JMenu or JMenuItem which contain action listeners
   * that perform the visualization, using some but not
   * necessarily all of the data.  Exceptions thrown because of
   * changes in Weka since compilation need to be caught by
   * the implementer.
   *
   * @see 		NoClassDefFoundError
   * @see 		IncompatibleClassChangeError
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

    JMenuItem result = new JMenuItem("Prediction table");
    result.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	display(finalPreds, finalClassAtt);
      }
    });
    
    return result;
  }

  /**
   * Displays the predictions in a JTable.
   * 
   * @param preds	the predictions to plot
   * @param classAtt	the class attribute
   */
  protected void display(FastVector preds, Attribute classAtt) {
    int			i;
    NominalPrediction	pred;
    JFrame 		frame;
    JTable		table;
    Object		data[][];

    if (preds == null) {
      JOptionPane.showMessageDialog(null, "No data available for display!");
      return;
    }

    // fill table
    data = new Object[preds.size()][];
    for (i = 0; i < preds.size(); i++) {
      pred    = (NominalPrediction) preds.elementAt(i);
      data[i] = new Object[]{
	  		i, 
	  		classAtt.value((int) pred.actual()),
	  		classAtt.value((int) pred.predicted()),
	  		(pred.predicted() != pred.actual()) ? "+" : "",
	  		 pred.distribution()[(int) pred.actual()]};
    }
    table = new JTable(data, new String[]{
			"Index", 
			"Actual", 
			"Predicted", 
			"Error", 
			"Prob. for 'Actual'"});
    
    // put the PlotPanel in a JFrame like a JPanel
    frame = new JFrame("Prediction table");
    frame.setSize(600, 600);
    frame.setContentPane(new JScrollPane(table));
    frame.setVisible(true);
  }
}
