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
 * SqlPanel.java
 * Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 */

package wekaexamples.gui.explorer;

import weka.core.Instances;
import weka.experiment.InstanceQuery;
import weka.gui.Logger;
import weka.gui.SysErrLog;
import weka.gui.explorer.Explorer;
import weka.gui.explorer.Explorer.ExplorerPanel;
import weka.gui.explorer.Explorer.LogHandler;
import weka.gui.sql.SqlViewer;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A simple demonstration for extending the Explorer by another tab, in this
 * case the SqlViewer (as an extra tab instead of only the button in the
 * PreprocessPanel). <br/>
 * The <code>Explorer.props</code> file needs to edited to make it
 * available and since this tab does not rely on the PreprocessPanel, one
 * should add the "standalone" option. In other words, this would be the
 * string to be added to the list of tabs in the <code>Explorer.props</code> 
 * file: <br/>
 *   <code>weka.gui.explorer.SqlPanel:standalone</code>
 * 
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class SqlPanel
  extends JPanel
  implements ExplorerPanel, LogHandler {

  /** for serialization */
  private static final long serialVersionUID = 2926260895970369406L;
  
  /** the parent frame */
  protected Explorer m_Explorer = null;
  
  /**
   * Manages sending notifications to people when we change the set of
   * working instances.
   */
  protected PropertyChangeSupport m_Support = new PropertyChangeSupport(this);

  /** The destination for log/status messages */
  protected Logger m_Log = new SysErrLog();
  
  /** the SQL panel */
  protected SqlViewer m_Viewer;

  /** the panel for the buttons */
  protected JPanel m_PanelButtons;

  /** the Load button */
  protected JButton m_ButtonLoad = new JButton("Load data");

  /** displays the current query */
  protected JLabel m_LabelQuery = new JLabel("");

  /**
   * initializes the panel
   */
  public SqlPanel() {
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

    // sql panel
    m_Viewer = new SqlViewer(null);
    add(m_Viewer, BorderLayout.CENTER);
    
    panel2 = new JPanel(new BorderLayout());
    add(panel2, BorderLayout.SOUTH);
    
    // Button
    panel = new JPanel();
    panel.setLayout(new FlowLayout());
    panel2.add(panel, BorderLayout.EAST);
    m_ButtonLoad.setMnemonic('L');
    panel.add(m_ButtonLoad);
    m_ButtonLoad.addActionListener(new ActionListener(){
	public void actionPerformed(ActionEvent evt){
	  m_Support.firePropertyChange("", null, null);
      }
    });
   
    // current Query
    panel = new JPanel(new FlowLayout());
    panel2.add(panel, BorderLayout.CENTER);
    panel.add(m_LabelQuery);
    
    addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
	try {
	  m_Log.statusMessage("Querying database...");
	  m_Log.logMessage(
	      "SQL query: "
	      + "URL=" + m_Viewer.getURL()
	      + ", User=" + m_Viewer.getUser()
	      + ", Password=" + m_Viewer.getPassword().replaceAll(".*", "*")
	      + ", Query=" + m_Viewer.getQuery());
	  
	  // load data
	  InstanceQuery query = new InstanceQuery();
	  query.setDatabaseURL(m_Viewer.getURL());
	  query.setUsername(m_Viewer.getUser());
	  query.setPassword(m_Viewer.getPassword());
	  Instances data = query.retrieveInstances(m_Viewer.getQuery());
	  
	  // set data in preproc panel (will also notify of capabilties changes)
	  getExplorer().getPreprocessPanel().setInstances(data);

	  m_Log.logMessage(
	      "SQL query returned " + data.numInstances() 
	      + " rows and " + data.numAttributes() 
	      + " columns");
	  m_Log.statusMessage("Database query finished and data loaded.");
	}
	catch (Exception ex) {
	  ex.printStackTrace();
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
    return "SQL";
  }
  
  /**
   * Returns the tooltip for the tab in the Explorer
   * 
   * @return 		the tooltip of this tab
   */
  public String getTabTitleToolTip() {
    return "Retrieving data from databases";
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
	new javax.swing.JFrame("Weka Explorer: SQL");
      jf.getContentPane().setLayout(new BorderLayout());
      final SqlPanel sp = new SqlPanel();
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
