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
 *    Visualize3D.java
 *    Copyright (C) 2010 Pentaho Corporation
 *
 */

package weka.gui.visualize;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;

import weka.core.Attribute;
import weka.core.Instances;

/**
 * Panel that displays a 3D scatter plot of the data. Has widgets
 * to allow the user to select the axes to be visualized. 
 * Requires Java 3D to be installed.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class Visualize3D extends JPanel {
    
  /** For serialization */
  private static final long serialVersionUID = 5395759425648712461L;

  /** The VisualizePanel3D that does the actual rendering */
  protected VisualizePanel3D m_visPanel = new VisualizePanel3D();
  
  /** Combo box for selecting the x axis */
  protected JComboBox m_xCombo = new JComboBox();
  
  /** Combo box for selecting the y axis */
  protected JComboBox m_yCombo = new JComboBox();
  
  /** Combo box for selecting the z axis */
  protected JComboBox m_zCombo = new JComboBox();
  
  /** Combo box for selecting the coloring axis */
  protected JComboBox m_cCombo = new JComboBox();
  
  /** Button for upating the display after changing axis etc. */
  protected JButton m_updateBut = new JButton("Update display");
  
  protected boolean m_combosReady = false;
  protected boolean m_combosChanged = false;
  
  /** A titled panel that holds the plot */
  protected JPanel m_plotSurround = new JPanel();
  
  public Visualize3D() {
    setLayout(new BorderLayout());
    
    m_xCombo.setEnabled(false);
    m_yCombo.setEnabled(false);
    m_zCombo.setEnabled(false);
    m_cCombo.setEnabled(false);
    m_xCombo.setLightWeightPopupEnabled(false);
    m_yCombo.setLightWeightPopupEnabled(false);
    m_zCombo.setLightWeightPopupEnabled(false);
    m_cCombo.setLightWeightPopupEnabled(false);
    m_updateBut.setEnabled(false);

    JPanel controlHolder = new JPanel();
    controlHolder.setLayout(new BorderLayout());
    
    JPanel comboHolder = new JPanel();
    comboHolder.setLayout(new GridLayout(2,2));
    comboHolder.add(m_xCombo);
    comboHolder.add(m_yCombo);
    comboHolder.add(m_zCombo);
    comboHolder.add(m_cCombo);
    controlHolder.add(comboHolder, BorderLayout.NORTH);
    
    JPanel butHolder = new JPanel();
    butHolder.setLayout(new BorderLayout());
    butHolder.add(m_updateBut, BorderLayout.NORTH);
    controlHolder.add(butHolder, BorderLayout.SOUTH);
    
    add(controlHolder, BorderLayout.NORTH);
    
    m_plotSurround.setLayout(new BorderLayout());
    m_plotSurround.setBorder(BorderFactory.createTitledBorder("Plot"));
    m_plotSurround.add(m_visPanel, BorderLayout.CENTER);
    
    add(m_plotSurround, BorderLayout.CENTER);
    
    m_xCombo.addActionListener(new ActionListener() {
       public void actionPerformed(ActionEvent e) {
         m_combosChanged = true;
       }
    });
    
    m_yCombo.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {       
        m_combosChanged = true;
      }
   });
    
    m_zCombo.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        m_combosChanged = true;
      }
   });
    
    m_cCombo.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        m_combosChanged = true;
      }
   });
    
    m_updateBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        updateDisplay();
      }
    });
  }
  
  /**
   * Tell's the panel to update the visualization. 
   */
  public void updateDisplay() {
    if (m_combosChanged) {
      int x = m_xCombo.getSelectedIndex();
      int y = m_yCombo.getSelectedIndex();
      int z = m_zCombo.getSelectedIndex();
      int c = m_cCombo.getSelectedIndex();

      if (m_combosReady) {
        m_visPanel.setAxes(x, y, z, c);
        m_combosChanged = false;
      }
    }
  }

  /**
   * Sets a new set of instances to be visualized.
   * 
   * @param inst the instances to visualize.
   * @param display true if the display should be updated at this point.
   */
  public void setInstances(Instances inst, boolean display) {
    m_visPanel.setInstances(inst);
    m_plotSurround.setBorder(BorderFactory.createTitledBorder("Plot: "
        + inst.relationName()));
    setupComboBoxes(inst);
    if (display) {
      updateDisplay();
    }
  }
  
  /**
   * Sets a new set of instances to be visualized. Updates the display.
   * 
   * @param inst the instances to be visualized.
   */
  public void setInstances(Instances inst) {
    setInstances(inst, true);
  }
  
  /**
   * Sets up the combo boxes.
   * 
   * @param inst the instances to use for setting combo box choices.
   */
  protected void setupComboBoxes(Instances inst) {
    m_combosReady = false;
    String [] XNames = new String [inst.numAttributes()];
    String [] YNames = new String [inst.numAttributes()];
    String [] ZNames = new String [inst.numAttributes()];
    String [] CNames = new String [inst.numAttributes()];
    for (int i = 0; i < XNames.length; i++) {
      String type = "";
      switch (inst.attribute(i).type()) {
      case Attribute.NOMINAL:
        type = " (Nom)";
        break;
      case Attribute.NUMERIC:
        type = " (Num)";
        break;
      case Attribute.STRING:
        type = " (Str)";
        break;
      case Attribute.DATE:
        type = " (Dat)";
        break;
      case Attribute.RELATIONAL:
        type = " (Rel)";
        break;
      default:
        type = " (???)";
      }
      XNames[i] = "X: "+ inst.attribute(i).name()+type;
      YNames[i] = "Y: "+ inst.attribute(i).name()+type;
      ZNames[i] = "Z: " + inst.attribute(i).name() + type;
      CNames[i] = "Colour: "+ inst.attribute(i).name()+type;
    }
    
    m_xCombo.setModel(new DefaultComboBoxModel(XNames));
    m_yCombo.setModel(new DefaultComboBoxModel(YNames));
    m_zCombo.setModel(new DefaultComboBoxModel(ZNames));
    m_cCombo.setModel(new DefaultComboBoxModel(CNames));
    
    m_xCombo.setEnabled(true);
    m_yCombo.setEnabled(true);
    m_zCombo.setEnabled(true);
    m_cCombo.setEnabled(true);
    m_updateBut.setEnabled(true);

    int xIndex = 0;
    int yIndex = 0;
    int zIndex = 0;
    int cIndex = 0;
    if (inst.numAttributes() > 1) {
      zIndex = 1;
      yIndex = 1;
    }
    
    if (inst.numAttributes() > 2) {
      zIndex = 2;
    }
    
    cIndex = inst.numAttributes() - 1;
    m_xCombo.setSelectedIndex(xIndex);
    m_yCombo.setSelectedIndex(yIndex);
    m_zCombo.setSelectedIndex(zIndex);    
    m_cCombo.setSelectedIndex(cIndex);
    m_combosReady = true;
  }
  
  /**
   * Frees resources held by the Java 3D system.
   */
  public void freeResources() {
    m_visPanel.freeResources();
  }
  
  /**
   * @param args
   */
  public static void main(String[] args) {

    try {
      Instances insts = new Instances(new BufferedReader(new FileReader(args[0])));
      
      final Visualize3D vis = new Visualize3D();
      vis.setInstances(insts);
      
      final JFrame frame = new JFrame("Visualize 3D");
      frame.addWindowListener(new java.awt.event.WindowAdapter() {
        public void windowClosing(java.awt.event.WindowEvent e) {
          vis.freeResources();
          frame.dispose();
          System.exit(1);
        }
      });
      frame.setSize(800, 600);
      frame.setContentPane(vis);
      frame.setVisible(true);
      
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

}
