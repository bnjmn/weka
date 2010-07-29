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
 *    SimpleCLI.java
 *    Copyright (C) 1999 University of Waikato, Hamilton, New Zealand
 *
 */


package weka.gui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Creates a very simple command line for invoking the main method of
 * classes. System.out and System.err are redirected to an output area.
 * Features a simple command history -- use up and down arrows to move
 * through previous commmands. This gui uses only AWT (i.e. no Swing).
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.10 $
 */
public class SimpleCLI
  extends Frame {
  
  /** for serialization */
  static final long serialVersionUID = -50661410800566036L;
  
  /**
   * Constructor
   *
   * @throws Exception if an error occurs
   */
  public SimpleCLI() throws Exception {
    setTitle("SimpleCLI");
    setLayout(new BorderLayout());
    add(new SimpleCLIPanel());
    pack();
    setSize(600, 500);
    setVisible(true);
  }

  /**
   * Method to start up the simple cli
   *
   * @param args array of command line arguments. Not used.
   */
  public static void main(String[] args) {
    
    try {
      final SimpleCLI frame = new SimpleCLI();
      frame.addWindowListener(new WindowAdapter() {
	public void windowClosing(WindowEvent param1) {
	  System.err.println("window closed");
	  frame.dispose();
	}
      });
      frame.setVisible(true);
    } catch (Exception e) {
      System.out.println(e.getMessage());
      System.exit(0);
    }
  }
}
