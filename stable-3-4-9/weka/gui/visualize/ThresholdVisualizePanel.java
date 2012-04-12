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
 *    ThresholdVisualizePanel.java
 *    Copyright (C) 2003 Dale Fletcher
 *
 */


package weka.gui.visualize;


import weka.core.*;

import java.awt.event.*;

import javax.swing.BorderFactory;
import javax.swing.border.TitledBorder;



/** 
 * This panel is a VisualizePanel, with the added ablility to display the
 * area under the ROC curve if an ROC curve is chosen.
 *
 * @author Dale Fletcher (dale@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */


public class ThresholdVisualizePanel extends VisualizePanel{

  /** The string to add to the Plot Border. */
  private String m_ROCString="";
 
  /** Original border text */
  private String m_savePanelBorderText;

  /**
   * Set the string with ROC area
   * @param str ROC area string to add to border
   */  
  public void setROCString(String str) {
    m_ROCString=str;
  }

  /**
   * This extracts the ROC area string 
   * @return ROC area string 
   */
  public String getROCString() {
    return m_ROCString;
  }

  /**
   * This overloads VisualizePanel's setUpComboBoxes to add 
   * ActionListeners to watch for when the X/Y Axis comboboxes
   * are changed. 
   * @param inst a set of instances with data for plotting
   */
  public void setUpComboBoxes(Instances inst) {
    super.setUpComboBoxes(inst);

    m_XCombo.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  setBorderText();
	}
    });
    m_YCombo.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  setBorderText();
	}
    });

    // Save the current border text
    TitledBorder tb=(TitledBorder) m_plotSurround.getBorder();
    m_savePanelBorderText = tb.getTitle();

    // Just in case the default is ROC
    setBorderText();
  }

  /**
   * This checks the current selected X/Y Axis comboBoxes to see if 
   * an ROC graph is selected. If so, add the ROC area string to the
   * plot border, otherwise display the original border text.
   */
  private void setBorderText() {

    String xs = m_XCombo.getSelectedItem().toString();
    String ys = m_YCombo.getSelectedItem().toString();

    if (xs.equals("X: False Positive Rate (Num)") && ys.equals("Y: True Positive Rate (Num)"))   {
        m_plotSurround.setBorder((BorderFactory.createTitledBorder(m_savePanelBorderText+" "+m_ROCString)));
    } else
        m_plotSurround.setBorder((BorderFactory.createTitledBorder(m_savePanelBorderText))); 
  }
}

 






