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
 * VisualizeXMLBIF.java
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package wekaexamples.gui.graphvisualizer;

import weka.gui.graphvisualizer.GraphVisualizer;

import java.awt.BorderLayout;
import java.io.File;
import java.io.FileInputStream;

import javax.swing.JFrame;

/**
 * Displays a graph stored in a XML BIF file.
 * Expects an ARFF filename as first argument.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class VisualizeXMLBIF {
  
  /**
   * Expects a XML BIF file as first parameter.
   * 
   * @param args	the commandline arguments
   * @throws Exception	if something goes wrong
   */
  public static void main(String args[]) throws Exception {
    // display graph
    GraphVisualizer gv = new GraphVisualizer();
    gv.readBIF(new FileInputStream(new File(args[0])));
    JFrame jf = new JFrame("BayesNet graph");
    jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    jf.setSize(800, 600);
    jf.getContentPane().setLayout(new BorderLayout());
    jf.getContentPane().add(gv, BorderLayout.CENTER);
    jf.setVisible(true);
    // layout graph
    gv.layoutGraph();
  }
}

