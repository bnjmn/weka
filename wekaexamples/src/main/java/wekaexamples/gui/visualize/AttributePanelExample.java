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
 * AttributePanelExample.java
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package wekaexamples.gui.visualize;

import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.gui.visualize.AttributePanel;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;

/**
 * Example class explaining the usage of the AttributePanel class, which
 * displays one dimensional views of the attributes in a dataset.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 * @see AttributePanel
 */
public class AttributePanelExample {

  /**
   * Expects a filename of a dataset as first argument.
   *
   * @param args 	the commandline arguments
   * @throws Exception 	if something goes wrong
   */
  public static void main(String[] args) throws Exception {
    Instances data = DataSource.read(args[0]);

    AttributePanel panel = new AttributePanel();
    panel.setInstances(data);
    if (data.classIndex() == -1)
      panel.setCindex(data.numAttributes() - 1);
    else
      panel.setCindex(data.classIndex());

    JFrame frame = new JFrame("AttributePanel example");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.getContentPane().setLayout(new BorderLayout());
    frame.getContentPane().add(panel);
    frame.setSize(new Dimension(600, 400));
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }
}
