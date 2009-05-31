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
 * GeneratorPanel.java
 * Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 */

package wekaexamples.gui.explorer;

import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.datagenerators.DataGenerator;
import weka.gui.GenericObjectEditor;
import weka.gui.Logger;
import weka.gui.PropertyPanel;
import weka.gui.SysErrLog;
import weka.gui.explorer.Explorer;
import weka.gui.explorer.Explorer.ExplorerPanel;
import weka.gui.explorer.Explorer.LogHandler;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * A simple demonstration for extending the Explorer by another tab, in this
 * case for the data generaors (as an extra tab instead of only the button in 
 * the PreprocessPanel). <br/>
 * The <code>Explorer.props</code> file needs to edited to make it
 * available and since this tab does not rely on the PreprocessPanel, one
 * should add the "standalone" option. In other words, this would be the
 * string to be added to the list of tabs in the <code>Explorer.props</code> 
 * file: <br/>
 *   <code>weka.gui.explorer.GeneratorPanel:standalone</code>
 * 
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class GeneratorPanel
  extends JPanel
  implements ExplorerPanel, LogHandler {
  
  /** for serialization */
  private static final long serialVersionUID = -7668783169747258984L;

  /** the parent frame */
  protected Explorer m_Explorer = null;
  
  /**
   * Manages sending notifications to people when we change the set of
   * working instances.
   */
  protected PropertyChangeSupport m_Support = new PropertyChangeSupport(this);

  /** The destination for log/status messages */
  protected Logger m_Log = new SysErrLog();

  /** the GOE for the generators */
  protected GenericObjectEditor m_GeneratorEditor = new GenericObjectEditor();

  /** the text area for the output of the generated data */
  protected JTextArea m_Output = new JTextArea();
  
  /** the Generate button */
  protected JButton m_ButtonGenerate = new JButton("Generate");
  
  /** the Use button */
  protected JButton m_ButtonUse = new JButton("Use");

  /** register the classes */
  static {
     GenericObjectEditor.registerEditors();
  }
  
  /**
   * initializes the panel
   */
  public GeneratorPanel() {
    super();

    initGUI();
  }

  /**
   * initializes the GUI
   */
  protected void initGUI() {
    JPanel              panel;
    JPanel              panel2;
    
    setLayout(new BorderLayout());
    
    // the editor
    add(new PropertyPanel(m_GeneratorEditor), BorderLayout.NORTH);
    m_GeneratorEditor.setClassType(DataGenerator.class);
    m_GeneratorEditor.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
	repaint();
      }
    });
    m_GeneratorEditor.setValue(new weka.datagenerators.classifiers.classification.RDG1());

    // text panel for output of generated data
    panel2 = new JPanel(new BorderLayout());
    add(panel2, BorderLayout.CENTER);
    panel2.add(new JScrollPane(m_Output));
    m_Output.setFont(new java.awt.Font("Monospaced", 0, 12));
    
    // Buttons
    panel = new JPanel();
    panel.setLayout(new FlowLayout(FlowLayout.RIGHT));
    panel2.add(panel, BorderLayout.SOUTH);
    m_ButtonGenerate.setMnemonic('G');
    panel.add(m_ButtonGenerate);
    m_ButtonGenerate.addActionListener(new ActionListener(){
	public void actionPerformed(ActionEvent evt){
	    DataGenerator generator = (DataGenerator) m_GeneratorEditor.getValue();
	    String relName = generator.getRelationName();

	    String cname = generator.getClass().getName().replaceAll(".*\\.", "");
	    String cmd = generator.getClass().getName();
	    if (generator instanceof OptionHandler)
	      cmd += " " + Utils.joinOptions(((OptionHandler) generator).getOptions());
	    
	    try {
	      m_Log.logMessage("Started " + cname);
	      m_Log.logMessage("Command: " + cmd);

	      // generate data
	      StringWriter output = new StringWriter();
	      generator.setOutput(new PrintWriter(output));
	      DataGenerator.makeData(generator, generator.getOptions());
	      m_Output.setText(output.toString());

	      m_Log.logMessage("Finished " + cname);
	    }
	    catch (Exception ex) {
	      ex.printStackTrace();
	      JOptionPane.showMessageDialog(
	          getExplorer(), "Error generating data:\n" + ex.getMessage(), 
	          "Error", JOptionPane.ERROR_MESSAGE);
	    }

	    generator.setRelationName(relName);
      }
    });
    m_ButtonUse.setMnemonic('U');
    panel.add(m_ButtonUse);
    m_ButtonUse.addActionListener(new ActionListener(){
	public void actionPerformed(ActionEvent evt){
	  m_Support.firePropertyChange("", null, null);
      }
    });
    
    addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
	try {
	  Instances data = new Instances(new StringReader(m_Output.getText()));
	  // set data in preproc panel as well (will notify of capabilties changes)
	  getExplorer().getPreprocessPanel().setInstances(data);
	}
	catch (Exception ex) {
	  ex.printStackTrace();
	  JOptionPane.showMessageDialog(
	      getExplorer(), "Error generating data:\n" + ex.getMessage(), 
	      "Error", JOptionPane.ERROR_MESSAGE);
	}
      }
    });
  }
  
  /**
   * Sets the Explorer to use as parent frame (used for sending notifications
   * about changes in the data)
   * 
   * @param parent	the parent frame
   */
  public void setExplorer(Explorer parent) {
    m_Explorer = parent;
  }
  
  /**
   * returns the parent Explorer frame
   * 
   * @return		the parent
   */
  public Explorer getExplorer() {
    return m_Explorer;
  }
  
  /**
   * Returns the title for the tab in the Explorer
   * 
   * @return 		the title of this tab
   */
  public String getTabTitle() {
    return "DataGeneration";
  }
  
  /**
   * Returns the tooltip for the tab in the Explorer
   * 
   * @return 		the tooltip of this tab
   */
  public String getTabTitleToolTip() {
    return "Generating artificial datasets";
  }

  /**
   * ignored
   * 
   * @param inst	ignored
   */
  public void setInstances(Instances inst) {
    // ignored
  }
  
  /**
   * Sets the Logger to receive informational messages
   *
   * @param newLog 	the Logger that will now get info messages
   */
  public void setLog(Logger newLog) {
    m_Log = newLog;
  }
  
  /**
   * Adds a PropertyChangeListener who will be notified of value changes.
   *
   * @param l 		a value of type 'PropertyChangeListener'
   */
  public void addPropertyChangeListener(PropertyChangeListener l) {
    m_Support.addPropertyChangeListener(l);
  }

  /**
   * Removes a PropertyChangeListener.
   *
   * @param l 		a value of type 'PropertyChangeListener'
   */
  public void removePropertyChangeListener(PropertyChangeListener l) {
    m_Support.removePropertyChangeListener(l);
  }

  /**
   * For testing only.
   * 
   * @param args 	commandline arguments - ignored
   */
  public static void main(String[] args) {
    try {
      final javax.swing.JFrame jf =
	new javax.swing.JFrame("Weka Explorer: Data Generation");
      jf.getContentPane().setLayout(new BorderLayout());
      final GeneratorPanel sp = new GeneratorPanel();
      jf.getContentPane().add(sp, BorderLayout.CENTER);
      jf.addWindowListener(new java.awt.event.WindowAdapter() {
	public void windowClosing(java.awt.event.WindowEvent e) {
	  jf.dispose();
	  System.exit(0);
	}
      });
      jf.pack();
      jf.setSize(800, 600);
      jf.setVisible(true);
    }
    catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
