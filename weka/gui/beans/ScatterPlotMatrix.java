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
 *    ScatterPlotMatrix.java
 *    Copyright (C) 2003 Mark Hall
 *
 */

package weka.gui.beans;

import weka.core.Instances;
import weka.gui.visualize.MatrixPanel;
import weka.gui.visualize.VisualizePanel;
import weka.gui.visualize.PlotData2D;

import java.io.Serializable;
import java.util.Vector;
import java.util.Enumeration;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;
import java.awt.*;

/**
 * Bean that encapsulates weka.gui.visualize.MatrixPanel for displaying a
 * scatter plot matrix.
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.2 $
 */
public class ScatterPlotMatrix extends DataVisualizer
  implements DataSourceListener, TrainingSetListener,
	     TestSetListener, Visible, UserRequestAcceptor, Serializable {

  public ScatterPlotMatrix() {
    m_visual = 
      new BeanVisual("ScatterPlotMatrix", 
		     BeanVisual.ICON_PATH+"ScatterPlotMatrix.gif",
		     BeanVisual.ICON_PATH+"ScatterPlotMatrix_animated.gif");
    setLayout(new BorderLayout());
    add(m_visual, BorderLayout.CENTER);
  }

  /**
   * Perform a named user request
   *
   * @param request a string containing the name of the request to perform
   * @exception IllegalArgumentException if request is not supported
   */
  public void performRequest(String request) {
    if (request.compareTo("Show plot") == 0) {
      try {
	// popup matrix panel
	if (!m_framePoppedUp) {
	  m_framePoppedUp = true;
	  final MatrixPanel vis = new MatrixPanel();
	  vis.setInstances(m_visualizeDataSet);

	  final javax.swing.JFrame jf = 
	    new javax.swing.JFrame("Visualize");
	  jf.setSize(800,600);
	  jf.getContentPane().setLayout(new BorderLayout());
	  jf.getContentPane().add(vis, BorderLayout.CENTER);
	  jf.addWindowListener(new java.awt.event.WindowAdapter() {
	      public void windowClosing(java.awt.event.WindowEvent e) {
		jf.dispose();
		m_framePoppedUp = false;
	      }
	    });
	  jf.setVisible(true);
	}
      } catch (Exception ex) {
	ex.printStackTrace();
	m_framePoppedUp = false;
      }
    } else {
      throw new IllegalArgumentException(request
					 + " not supported (ScatterPlotMatrix)");
    }
  }
}
