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
 *    DataVisualizer.java
 *    Copyright (C) 2002 Mark Hall
 *
 */

package weka.gui.beans;

import weka.core.Instances;
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
 * Bean that encapsulates weka.gui.visualize.VisualizePanel
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.3 $
 */
public class DataVisualizer extends JPanel
  implements DataSourceListener, TrainingSetListener,
	     TestSetListener, Visible, UserRequestAcceptor, Serializable {

  protected BeanVisual m_visual = 
    new BeanVisual("DataVisualizer", 
		   BeanVisual.ICON_PATH+"DefaultDataVisualizer.gif",
		   BeanVisual.ICON_PATH+"DefaultDataVisualizer_animated.gif");

  private transient Instances m_visualizeDataSet;

  private boolean m_framePoppedUp = false;

  public DataVisualizer() {
    setLayout(new BorderLayout());
    add(m_visual, BorderLayout.CENTER);
  }

  /**
   * Accept a training set
   *
   * @param e a <code>TrainingSetEvent</code> value
   */
  public void acceptTrainingSet(TrainingSetEvent e) {
    Instances trainingSet = e.getTrainingSet();
    DataSetEvent dse = new DataSetEvent(this, trainingSet);
    acceptDataSet(dse);
  }

  /**
   * Accept a test set
   *
   * @param e a <code>TestSetEvent</code> value
   */
  public void acceptTestSet(TestSetEvent e) {
    Instances testSet = e.getTestSet();
    DataSetEvent dse = new DataSetEvent(this, testSet);
    acceptDataSet(dse);
  }

  /**
   * Accept a data set
   *
   * @param e a <code>DataSetEvent</code> value
   */
  public synchronized void acceptDataSet(DataSetEvent e) {
    m_visualizeDataSet = new Instances(e.getDataSet());
    if (m_visualizeDataSet.classIndex() <= 0) {
      m_visualizeDataSet.setClassIndex(m_visualizeDataSet.numAttributes()-1);
    }
  }

  /**
   * Set the visual appearance of this bean
   *
   * @param newVisual a <code>BeanVisual</code> value
   */
  public void setVisual(BeanVisual newVisual) {
    m_visual = newVisual;
  }

  /**
   * Return the visual appearance of this bean
   */
  public BeanVisual getVisual() {
    return m_visual;
  }

  /**
   * Use the default appearance for this bean
   */
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH+"DefaultDataVisualizer.gif",
		       BeanVisual.ICON_PATH+"DefaultDataVisualizer_animated.gif");
  }

  /**
   * Describe <code>enumerateRequests</code> method here.
   *
   * @return an <code>Enumeration</code> value
   */
  public Enumeration enumerateRequests() {
    Vector newVector = new Vector(0);
    if (m_visualizeDataSet != null && !m_framePoppedUp) {
      newVector.addElement("Show plot");
    }
    return newVector.elements();
  }

  /**
   * Describe <code>performRequest</code> method here.
   *
   * @param request a <code>String</code> value
   * @exception IllegalArgumentException if an error occurs
   */
  public void performRequest(String request) {
    if (request.compareTo("Show plot") == 0) {
      try {
	// popup visualize panel
	if (!m_framePoppedUp) {
	  m_framePoppedUp = true;
	  final VisualizePanel vis = new VisualizePanel();
	  PlotData2D pd1 = new PlotData2D(m_visualizeDataSet);
	  pd1.setPlotName(m_visualizeDataSet.relationName());
	  try {
	    vis.setMasterPlot(pd1);
	  } catch (Exception ex) {
	    System.err.println("Problem setting up "
			       +"visualization (DataVisualizer)");
	    ex.printStackTrace();
	  }
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
					 + " not supported (DataVisualizer)");
    }
  }
}
