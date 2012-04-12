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
import javax.swing.JFrame;
import javax.swing.SwingConstants;
import java.awt.*;
import java.beans.*;
import java.beans.beancontext.*;

/**
 * Bean that encapsulates weka.gui.visualize.VisualizePanel
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.9 $
 */
public class DataVisualizer extends JPanel
  implements DataSourceListener, TrainingSetListener,
	     TestSetListener, Visible, UserRequestAcceptor, Serializable,
	     BeanContextChild {

  protected BeanVisual m_visual;

  protected transient Instances m_visualizeDataSet;

  protected transient JFrame m_popupFrame;

  protected boolean m_framePoppedUp = false;

  /**
   * True if this bean's appearance is the design mode appearance
   */
  protected boolean m_design;

  /**
   * BeanContex that this bean might be contained within
   */
  protected transient BeanContext m_beanContext = null;

  private VisualizePanel m_visPanel;
  
  /**
   * BeanContextChild support
   */
  protected BeanContextChildSupport m_bcSupport = 
    new BeanContextChildSupport(this);

  public DataVisualizer() {
    appearanceFinal();
  }

  /**
   * Global info for this bean
   *
   * @return a <code>String</code> value
   */
  public String globalInfo() {
    return "Visualize incoming data/training/test sets in a 2D scatter plot.";
  }

  protected void appearanceDesign() {
    m_visPanel = null;
    removeAll();
    m_visual = new BeanVisual("DataVisualizer", 
			      BeanVisual.ICON_PATH+"DefaultDataVisualizer.gif",
			      BeanVisual.ICON_PATH
			      +"DefaultDataVisualizer_animated.gif");
    setLayout(new BorderLayout());
    add(m_visual, BorderLayout.CENTER);
  }

  protected void appearanceFinal() {
    removeAll();
    setLayout(new BorderLayout());
    setUpFinal();
  }

  protected void setUpFinal() {
    if (m_visPanel == null) {
      m_visPanel = new VisualizePanel();
    }
    add(m_visPanel, BorderLayout.CENTER);
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
    // ignore structure only events
    if (e.isStructureOnly()) {
      return;
    }
    m_visualizeDataSet = new Instances(e.getDataSet());
    if (m_visualizeDataSet.classIndex() < 0) {
      m_visualizeDataSet.setClassIndex(m_visualizeDataSet.numAttributes()-1);
    }
    if (!m_design) {
      try {
	setInstances(m_visualizeDataSet);
      } catch (Exception ex) {
	ex.printStackTrace();
      }
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
    if (m_visualizeDataSet != null) {
      newVector.addElement("Show plot");
    }
    return newVector.elements();
  }

  /**
   * Add a property change listener to this bean
   *
   * @param name the name of the property of interest
   * @param pcl a <code>PropertyChangeListener</code> value
   */
  public void addPropertyChangeListener(String name,
					PropertyChangeListener pcl) {
    m_bcSupport.addPropertyChangeListener(name, pcl);
  }

  /**
   * Remove a property change listener from this bean
   *
   * @param name the name of the property of interest
   * @param pcl a <code>PropertyChangeListener</code> value
   */
  public void removePropertyChangeListener(String name,
					   PropertyChangeListener pcl) {
    m_bcSupport.removePropertyChangeListener(name, pcl);
  }

  /**
   * Add a vetoable change listener to this bean
   *
   * @param name the name of the property of interest
   * @param vcl a <code>VetoableChangeListener</code> value
   */
  public void addVetoableChangeListener(String name,
				       VetoableChangeListener vcl) {
    m_bcSupport.addVetoableChangeListener(name, vcl);
  }
  
  /**
   * Remove a vetoable change listener from this bean
   *
   * @param name the name of the property of interest
   * @param vcl a <code>VetoableChangeListener</code> value
   */
  public void removeVetoableChangeListener(String name,
					   VetoableChangeListener vcl) {
    m_bcSupport.removeVetoableChangeListener(name, vcl);
  }

  /**
   * Set a bean context for this bean
   *
   * @param bc a <code>BeanContext</code> value
   */
  public void setBeanContext(BeanContext bc) {
    m_beanContext = bc;
    m_design = m_beanContext.isDesignTime();
    if (m_design) {
      appearanceDesign();
    } else {
      appearanceFinal();
    }
  }

  /**
   * Return the bean context (if any) that this bean is embedded in
   *
   * @return a <code>BeanContext</code> value
   */
  public BeanContext getBeanContext() {
    return m_beanContext;
  }

  /**
   * Set instances for this bean. This method is a convenience method
   * for clients who use this component programatically
   *
   * @param inst an <code>Instances</code> value
   * @exception Exception if an error occurs
   */
  public void setInstances(Instances inst) throws Exception {
    if (m_design) {
      throw new Exception("This method is not to be used during design "
			  +"time. It is meant to be used if this "
			  +"bean is being used programatically as as "
			  +"stand alone component.");
    }
    m_visualizeDataSet = inst;
    PlotData2D pd1 = new PlotData2D(m_visualizeDataSet);
    pd1.setPlotName(m_visualizeDataSet.relationName());
    try {
      m_visPanel.setMasterPlot(pd1);
    } catch (Exception ex) {
      System.err.println("Problem setting up "
			 +"visualization (DataVisualizer)");
      ex.printStackTrace();
    }
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
	  final JFrame jf = new JFrame("Visualize");
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
	  m_popupFrame = jf;
	} else {
	  m_popupFrame.toFront();
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

  public static void main(String [] args) {
    try {
      if (args.length != 1) {
	System.err.println("Usage: DataVisualizer <dataset>");
	System.exit(1);
      }
      java.io.Reader r = new java.io.BufferedReader(
			 new java.io.FileReader(args[0]));
      Instances inst = new Instances(r);
      final javax.swing.JFrame jf = new javax.swing.JFrame();
      jf.getContentPane().setLayout(new java.awt.BorderLayout());
      final DataVisualizer as = new DataVisualizer();
      as.setInstances(inst);
      
      jf.getContentPane().add(as, java.awt.BorderLayout.CENTER);
      jf.addWindowListener(new java.awt.event.WindowAdapter() {
        public void windowClosing(java.awt.event.WindowEvent e) {
          jf.dispose();
          System.exit(0);
        }
      });
      jf.setSize(800,600);
      jf.setVisible(true);
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
