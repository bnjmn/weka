/*
 *    PreprocessPanel.java
 *    Copyright (C) 1999 Len Trigg
 *
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

package weka.gui.explorer;

import weka.core.Instances;
import weka.filters.Filter;
import weka.gui.ExtensionFileFilter;
import weka.gui.AttributeSelectionPanel;
import weka.gui.AttributeSummaryPanel;
import weka.gui.GenericArrayEditor;
import weka.gui.Logger;
import weka.gui.SysErrLog;
import weka.gui.InstancesSummaryPanel;
import weka.gui.TaskLogger;
import weka.experiment.InstanceQuery;

import java.io.Reader;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.ByteArrayOutputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeSupport;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.BorderFactory;
import javax.swing.filechooser.FileFilter;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.ListSelectionModel;

/** 
 * This panel controls simple preprocessing of instances. Attributes may be
 * selected for inclusion/exclusion, summary information on instances and
 * attributes is shown. A sequence of filters may be configured to alter the
 * set of instances. Altered instances may also be saved.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.14 $
 */
public class PreprocessPanel extends JPanel {

  /** Displays simple stats on the base instances */
  protected InstancesSummaryPanel m_BaseInstPanel =
    new InstancesSummaryPanel();
  
  /** Displays simple stats on the working instances */
  protected InstancesSummaryPanel m_WorkingInstPanel =
    new InstancesSummaryPanel();

  /** Click to load base instances from a file */
  protected JButton m_OpenFileBut = new JButton("Open file...");

  /** Click to load base instances from a URL */
  protected JButton m_OpenURLBut = new JButton("Open URL...");

  /** Click to load base instances from a Database */
  protected JButton m_OpenDBBut = new JButton("Open DB...");

  /** Click to apply filters and replace the working dataset */
  protected JButton m_ApplyBut = new JButton("Apply Filters");

  /** Click to replace the base dataset with the working dataset */
  protected JButton m_ReplaceBut = new JButton("Replace");

  /** Click to apply filters and save the results */
  protected JButton m_SaveBut = new JButton("Save...");
  
  /** Panel to let the user toggle attributes */
  protected AttributeSelectionPanel m_AttPanel = new AttributeSelectionPanel();

  /** Lets the user add a series of filters */
  protected GenericArrayEditor m_Filters = new GenericArrayEditor();

  /** Displays summary stats on the selected attribute */
  protected AttributeSummaryPanel m_AttSummaryPanel =
    new AttributeSummaryPanel();
  
  /** Filter to ensure only arff files are selected */  
  protected FileFilter m_ArffFilter =
    new ExtensionFileFilter(Instances.FILE_EXTENSION, "Arff data files");

  /** The file chooser for selecting arff files */
  protected JFileChooser m_FileChooser 
    = new JFileChooser(new File(System.getProperty("user.dir")));

  /** Stores the last URL that instances were loaded from */
  protected String m_LastURL = "http://";
  
  /** Stores the last sql query executed */
  protected String m_SQLQ = new String("SELECT * FROM ?");
  
  /** The unadulterated instances */
  protected Instances m_BaseInstances;

  /** The working (filtered) copy */
  protected Instances m_WorkingInstances;
  
  /**
   * Manages sending notifications to people when we change the set of
   * working instances.
   */
  protected PropertyChangeSupport m_Support = new PropertyChangeSupport(this);

  /** A thread to loading/saving instances from a file or URL */
  protected Thread m_IOThread;

  protected Logger m_Log = new SysErrLog();

  /** A copy of the most recently applied filters */
  protected byte [] m_FiltersCopy = null;
  
  static {
    java.beans.PropertyEditorManager
      .registerEditor(weka.core.SelectedTag.class,
		      weka.gui.SelectedTagEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(weka.filters.Filter.class,
		      weka.gui.GenericObjectEditor.class);
     java.beans.PropertyEditorManager
      .registerEditor(weka.attributeSelection.ASSearch.class,
		      weka.gui.GenericObjectEditor.class);
     java.beans.PropertyEditorManager
      .registerEditor(weka.attributeSelection.ASEvaluation.class,
		      weka.gui.GenericObjectEditor.class);
  }
  
  /**
   * Creates the instances panel with no initial instances.
   */
  public PreprocessPanel() {

    // Create/Configure/Connect components
    m_OpenFileBut.setToolTipText("Open a set of instances from a file");
    m_OpenURLBut.setToolTipText("Open a set of instances from a URL");
    m_OpenDBBut.setToolTipText("Open a set of instances from a database");
    m_ReplaceBut
      .setToolTipText("Replace the base relation with the working relation");
    m_ApplyBut.setToolTipText("Update working relation with current filters");
    m_SaveBut.setToolTipText("Save the working relation to a file");
    m_Filters.setToolTipText("Edit a list of filters to transform instances");
    m_FileChooser.setFileFilter(m_ArffFilter);
    m_FileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    m_OpenURLBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	setBaseInstancesFromURLQ();
      }
    });
    m_OpenDBBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	setBaseInstancesFromDBQ();
      }
    });
    m_OpenFileBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	setBaseInstancesFromFileQ();
      }
    });
    m_ReplaceBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	setBaseInstances(m_WorkingInstances);
      }
    });
    m_ApplyBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	setWorkingInstancesFromFilters();
      }
    });
    m_SaveBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	saveWorkingInstancesToFileQ();
      }
    });
    m_AttPanel.getSelectionModel()
      .addListSelectionListener(new ListSelectionListener() {
	public void valueChanged(ListSelectionEvent e) {
	  if (!e.getValueIsAdjusting()) {
	    ListSelectionModel lm = (ListSelectionModel) e.getSource();
	    for (int i = e.getFirstIndex(); i <= e.getLastIndex(); i++) {
	      if (lm.isSelectedIndex(i)) {
		m_AttSummaryPanel.setAttribute(i);
		break;
	      }
	    }
	  }
	}
    });
    m_BaseInstPanel.setBorder(BorderFactory
			      .createTitledBorder("Base relation"));
    m_WorkingInstPanel.setBorder(BorderFactory
				 .createTitledBorder("Working relation"));
    m_AttPanel.setBorder(BorderFactory
		    .createTitledBorder("Attributes in base relation"));
    m_Filters.setBorder(BorderFactory.createTitledBorder("Filters"));
    m_AttSummaryPanel.setBorder(BorderFactory
		    .createTitledBorder("Attribute info for base relation"));
    m_Filters.setValue(new Filter [0]);
    m_ReplaceBut.setEnabled(false);
    m_ApplyBut.setEnabled(false);
    m_SaveBut.setEnabled(false);
    
    // Set up the GUI layout
    JPanel instInfo = new JPanel();
    instInfo.setLayout(new GridLayout(1, 2));
    instInfo.add(m_BaseInstPanel);
    instInfo.add(m_WorkingInstPanel);

    JPanel buttons = new JPanel();
    buttons.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
    buttons.setLayout(new GridLayout(1, 6, 5, 5));
    buttons.add(m_OpenFileBut);
    buttons.add(m_OpenURLBut);
    buttons.add(m_OpenDBBut);
    buttons.add(m_ApplyBut);
    buttons.add(m_ReplaceBut);
    buttons.add(m_SaveBut);

    JPanel filterNAttInfo = new JPanel();
    filterNAttInfo.setLayout(new GridLayout(2, 1));
    filterNAttInfo.add(m_Filters);
    filterNAttInfo.add(m_AttSummaryPanel);
    JPanel filters = new JPanel();
    filters.setLayout(new GridLayout(1, 2));
    filters.add(m_AttPanel);
    filters.add(filterNAttInfo);

    JPanel p3 = new JPanel();
    p3.setLayout(new BorderLayout());
    p3.add(instInfo, BorderLayout.NORTH);
    p3.add(filters, BorderLayout.CENTER);
    
    setLayout(new BorderLayout());
    add(buttons, BorderLayout.NORTH);
    add(p3, BorderLayout.CENTER);
  }

  /**
   * gets a copy of the most recently applied filters.
   * @return a serialized array of the most recently applied filters
   */
  protected synchronized byte [] getMostRecentFilters() {
    return m_FiltersCopy;
  }

  /**
   * Sets the Logger to receive informational messages
   *
   * @param newLog the Logger that will now get info messages
   */
  public void setLog(Logger newLog) {

    m_Log = newLog;
  }
  
  /**
   * Tells the panel to use a new base set of instances.
   *
   * @param inst a set of Instances
   */
  public void setBaseInstances(Instances inst) {

    m_BaseInstances = inst;
    try {
      Runnable r = new Runnable() {
	public void run() {
	  m_BaseInstPanel.setInstances(m_BaseInstances);
	  m_AttPanel.setInstances(m_BaseInstances);
	  m_AttSummaryPanel.setInstances(m_BaseInstances);
	  m_Log.logMessage("Base relation is now "
			   + m_BaseInstances.relationName()
			   + " (" + m_BaseInstances.numInstances()
			   + " instances)");
	  m_Log.statusMessage("OK");

	  // clear most recently applied filters
	  m_FiltersCopy = null;
	  setWorkingInstances(m_BaseInstances);
	  m_ApplyBut.setEnabled(true);
	  m_ReplaceBut.setEnabled(false);
	  m_SaveBut.setEnabled(false);
	}
      };
      if (SwingUtilities.isEventDispatchThread()) {
	r.run();
      } else {
	SwingUtilities.invokeAndWait(r);
      }
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(this,
				    "Problem setting base instances:\n"
				    + ex.getMessage(),
				    "Instances",
				    JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Tells the panel to use a new working set of instances.
   *
   * @param inst a set of Instances
   */
  public void setWorkingInstances(Instances inst) {

    if (m_WorkingInstances != inst) {
      m_WorkingInstances = inst;
      m_WorkingInstPanel.setInstances(m_WorkingInstances);
      m_Log.logMessage("Working relation is now "
		       + m_WorkingInstances.relationName()
		       + " (" + m_WorkingInstances.numInstances()
		       + " instances)");
      m_Log.statusMessage("OK");
      m_ReplaceBut.setEnabled(true);
      m_SaveBut.setEnabled(true);
      
      // Fire a propertychange event
      m_Support.firePropertyChange("", null, null);
    }
  }

  /**
   * Gets the working set of instances.
   *
   * @return the working instances
   */
  public Instances getWorkingInstances() {

    return m_WorkingInstances;
  }
  
  /**
   * Adds a PropertyChangeListener who will be notified of value changes.
   *
   * @param l a value of type 'PropertyChangeListener'
   */
  public void addPropertyChangeListener(PropertyChangeListener l) {
    m_Support.addPropertyChangeListener(l);
  }

  /**
   * Removes a PropertyChangeListener.
   *
   * @param l a value of type 'PropertyChangeListener'
   */
  public void removePropertyChangeListener(PropertyChangeListener l) {
    m_Support.removePropertyChangeListener(l);
  }

  /**
   * Gets an array of all the filters that have been configured for use.
   *
   * @return an array containing all the filters
   */
  protected Filter [] getFilters() {

    Filter [] extras = (Filter [])m_Filters.getValue();
    if (extras == null) {
      extras = new Filter [0];
    }
    weka.filters.AttributeFilter af = null;
    try {
      // Configure the attributeFilter from the current attribute panel
      int [] selectedAttributes = m_AttPanel.getSelectedAttributes();
      if (selectedAttributes.length < m_BaseInstances.numAttributes()) {
	af = new weka.filters.AttributeFilter();
	af.setInvertSelection(true);
	af.setAttributeIndicesArray(selectedAttributes);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      m_Log.logMessage(ex.getMessage());
    }
    if (af == null) {
      return extras;
    }
    Filter [] result = new Filter[extras.length + 1];
    result[0] = af;
    System.arraycopy(extras, 0, result, 1, extras.length);
    return result;
  }
  
  /**
   * Passes the supplied instances through all the filters that have
   * been configured for use.
   *
   * @param instances the input instances
   * @return the filtered instances
   */
  protected Instances filterInstances(Instances instances) {

    Filter [] filters = getFilters();
    Instances temp = instances;
    try {
      if (m_Log instanceof TaskLogger) {
	((TaskLogger)m_Log).taskStarted();
      }
      for (int i = 0; i < filters.length; i++) {
	m_Log.statusMessage("Passing through filter " + (i + 1) + ": "
			    + filters[i].getClass().getName());
	filters[i].inputFormat(temp);
	temp = Filter.useFilter(temp, filters[i]);
      }
      if (m_Log instanceof TaskLogger) {
	((TaskLogger)m_Log).taskFinished();
      }
      // try to save a copy of the filters using serialization
      try {
	ByteArrayOutputStream bo = new ByteArrayOutputStream();
	BufferedOutputStream bbo = new BufferedOutputStream(bo);
	ObjectOutputStream oo = new ObjectOutputStream(bbo);
	oo.writeObject(filters);
	oo.close();
	m_FiltersCopy = bo.toByteArray();
      } catch (Exception ex) {
	JOptionPane.showMessageDialog(this,
				      "Could not create copy of filters",
				      null,
				      JOptionPane.ERROR_MESSAGE);
      }
      return temp;
    } catch (Exception ex) {
      // Pop up an error optionpane
      JOptionPane.showMessageDialog(this,
				    "Problem filtering instances:\n"
				    + ex.getMessage(),
				    "Instance Filter",
				    JOptionPane.ERROR_MESSAGE);
      ex.printStackTrace();
      m_Log.logMessage(ex.getMessage());
      return null;
    }
  }

  /**
   * Applies the current filters and attribute selection settings and
   * sets the result as the working dataset. This is done in the IO
   * thread, and an error message is popped up if the IO thread is busy.
   */
  public void setWorkingInstancesFromFilters() {
    if (m_IOThread == null) {
      m_IOThread = new Thread() {
	public void run() {
	  Instances f = filterInstances(m_BaseInstances);
	  if (f != null) {
	    if ((f == m_BaseInstances) && (f == m_WorkingInstances)) {
	      m_Log.logMessage("No changes from base relation");
	    } else {
	      setWorkingInstances(f);
	    }
	  }
	  m_IOThread = null;
	}
      };
      m_IOThread.setPriority(Thread.MIN_PRIORITY); // UI has most priority
      m_IOThread.start();
    } else {
      JOptionPane.showMessageDialog(this,
				    "Can't apply filters at this time,\n"
				    + "currently busy with other IO",
				    "Apply filters",
				    JOptionPane.WARNING_MESSAGE);

    }
  }

  /**
   * Queries the user for a file to save instances as, then saves the
   * instances in a background process. This is done in the IO
   * thread, and an error message is popped up if the IO thread is busy.
   */
  public void saveWorkingInstancesToFileQ() {
    
    if (m_IOThread == null) {
      int returnVal = m_FileChooser.showSaveDialog(this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
	File sFile = m_FileChooser.getSelectedFile();
	if (!sFile.getName().toLowerCase().endsWith(Instances.FILE_EXTENSION)) {
	  sFile = new File(sFile.getParent(), sFile.getName() 
                           + Instances.FILE_EXTENSION);
	}
	File selected = sFile;
	saveInstancesToFile(selected, m_WorkingInstances);
      }
    } else {
      JOptionPane.showMessageDialog(this,
				    "Can't save at this time,\n"
				    + "currently busy with other IO",
				    "Save Instances",
				    JOptionPane.WARNING_MESSAGE);
    }
  }
  
  /**
   * Queries the user for a file to load instances from, then loads the
   * instances in a background process. This is done in the IO
   * thread, and an error message is popped up if the IO thread is busy.
   */
  public void setBaseInstancesFromFileQ() {
    
    if (m_IOThread == null) {
      int returnVal = m_FileChooser.showOpenDialog(this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
	File selected = m_FileChooser.getSelectedFile();
	setBaseInstancesFromFile(selected);
      }
    } else {
      JOptionPane.showMessageDialog(this,
				    "Can't load at this time,\n"
				    + "currently busy with other IO",
				    "Load Instances",
				    JOptionPane.WARNING_MESSAGE);
    }
  }

  /**
   * Queries the user for a URL to a database to load instances from, 
   * then loads the instances in a background process. This is done in the IO
   * thread, and an error message is popped up if the IO thread is busy.
   */
  public void setBaseInstancesFromDBQ() {
    if (m_IOThread == null) {
      try {
	InstanceQuery InstQ = new InstanceQuery();
	String dbaseURL = InstQ.getDatabaseURL();
	dbaseURL = (String) JOptionPane.showInputDialog(this,
					     "Enter the database URL",
					     "Query Database",
					     JOptionPane.QUESTION_MESSAGE,
					     null,
					     null,
					     dbaseURL);
	if (dbaseURL == null) {
	  return;
	}
	InstQ.setDatabaseURL(dbaseURL);
	InstQ.connectToDatabase();      
	m_SQLQ = (String) JOptionPane.showInputDialog(this,
					       "Enter an SQL query",
					       "Query Database",
					       JOptionPane.QUESTION_MESSAGE,
					       null,
					       null,
					       m_SQLQ);
	if (m_SQLQ == null) {
	  m_SQLQ = new String("SELECT * FROM ?");
	} else {
	  setBaseInstancesFromDB(InstQ);
	}
      } catch (Exception ex) {
	JOptionPane.showMessageDialog(this,
				      "Problem with database URL:\n"
				      + ex.getMessage(),
				      "Load Instances",
				      JOptionPane.ERROR_MESSAGE);
      }
      
    } else {
      JOptionPane.showMessageDialog(this,
				     "Can't load at this time,\n"
				    + "currently busy with other IO",
				    "Load Instances",
				    JOptionPane.WARNING_MESSAGE);
    }
  }
    
  /**
   * Queries the user for a URL to load instances from, then loads the
   * instances in a background process. This is done in the IO
   * thread, and an error message is popped up if the IO thread is busy.
   */
  public void setBaseInstancesFromURLQ() {
    
    if (m_IOThread == null) {
      try {
	String urlName = (String) JOptionPane.showInputDialog(this,
			"Enter the source URL",
			"Load Instances",
			JOptionPane.QUESTION_MESSAGE,
			null,
			null,
			m_LastURL);
	if (urlName != null) {
	  m_LastURL = urlName;
	  URL url = new URL(urlName);
	  setBaseInstancesFromURL(url);
	}
      } catch (Exception ex) {
	JOptionPane.showMessageDialog(this,
				      "Problem with URL:\n"
				      + ex.getMessage(),
				      "Load Instances",
				      JOptionPane.ERROR_MESSAGE);
      }
    } else {
      JOptionPane.showMessageDialog(this,
				    "Can't load at this time,\n"
				    + "currently busy with other IO",
				    "Load Instances",
				    JOptionPane.WARNING_MESSAGE);
    }
  }
  
  
  /**
   * Saves the filtered instances to the supplied file.
   *
   * @param f a value of type 'File'
   * @param inst the instances to save
   */
  protected void saveInstancesToFile(final File f, final Instances inst) {
      
    if (m_IOThread == null) {
      m_IOThread = new Thread() {
	public void run() {
	  try {
	    m_Log.statusMessage("Saving to file...");
	    Writer w = new BufferedWriter(new FileWriter(f));
	    Instances h = new Instances(inst, 0);
	    w.write(h.toString());
	    w.write("\n");
	    for (int i = 0; i < inst.numInstances(); i++) {
	      w.write(inst.instance(i).toString());
	      w.write("\n");
	    }
	    w.close();
	    m_Log.statusMessage("OK");
	  } catch (Exception ex) {
	    ex.printStackTrace();
	    m_Log.logMessage(ex.getMessage());
	  }
	  m_IOThread = null;
	}
      };
      m_IOThread.setPriority(Thread.MIN_PRIORITY); // UI has most priority
      m_IOThread.start();
    } else {
      JOptionPane.showMessageDialog(this,
				    "Can't save at this time,\n"
				    + "currently busy with other IO",
				    "Save Instances",
				    JOptionPane.WARNING_MESSAGE);
    }
  }

  /**
   * Loads results from a set of instances contained in the supplied
   * file. This is started in the IO thread, and a dialog is popped up
   * if there's a problem.
   *
   * @param f a value of type 'File'
   */
  public void setBaseInstancesFromFile(final File f) {
      
    if (m_IOThread == null) {
      m_IOThread = new Thread() {
	public void run() {
	  try {
	    m_Log.statusMessage("Reading from file...");
	    Reader r = new BufferedReader(new FileReader(f));
	    setBaseInstances(new Instances(r));
	    r.close();
	  } catch (Exception ex) {
	    m_Log.statusMessage("Problem reading " + f.getName());
	    JOptionPane.showMessageDialog(PreprocessPanel.this,
					  "Couldn't read from file:\n"
					  + f.getName() + "\n"
					  + ex.getMessage(),
					  "Load Instances",
					  JOptionPane.ERROR_MESSAGE);
	  }
	  m_IOThread = null;
	}
      };
      m_IOThread.setPriority(Thread.MIN_PRIORITY); // UI has most priority
      m_IOThread.start();
    } else {
      JOptionPane.showMessageDialog(this,
				    "Can't load at this time,\n"
				    + "currently busy with other IO",
				    "Load Instances",
				    JOptionPane.WARNING_MESSAGE);
    }
  }
  
  /**
   * Loads instances from a database
   *
   * @param iq the InstanceQuery object to load from (this is assumed
   * to have been already connected to a valid database).
   */
  public void setBaseInstancesFromDB(final InstanceQuery iq) {
    if (m_IOThread == null) {
      m_IOThread = new Thread() {
	public void run() {
	  
	  try {
	    m_Log.statusMessage("Reading from database...");
	    final Instances i = iq.getInstances(m_SQLQ);
	    SwingUtilities.invokeAndWait(new Runnable() {
	      public void run() {
		setBaseInstances(new Instances(i));
	      }
	    });
	    iq.disconnectFromDatabase();
	  } catch (Exception ex) {
	    m_Log.statusMessage("Probelm executing DB query "+m_SQLQ);
	    JOptionPane.showMessageDialog(PreprocessPanel.this,
					  "Couldn't read from database:\n"
					  + m_SQLQ + "\n"
					  + ex.getMessage(),
					  "Load Instances",
					  JOptionPane.ERROR_MESSAGE);
	  }

	   m_IOThread = null;
	}
      };

      m_IOThread.setPriority(Thread.MIN_PRIORITY); // UI has most priority
      m_IOThread.start();
    } else {
       JOptionPane.showMessageDialog(this,
				    "Can't load at this time,\n"
				    + "currently busy with other IO",
				    "Load Instances",
				    JOptionPane.WARNING_MESSAGE);
    }
  }

  /**
   * Loads instances from a URL.
   *
   * @param u the URL to load from.
   */
  public void setBaseInstancesFromURL(final URL u) {

    if (m_IOThread == null) {
      m_IOThread = new Thread() {
	public void run() {

	  try {
	    m_Log.statusMessage("Reading from URL...");
	    Reader r = new BufferedReader(
		       new InputStreamReader(u.openStream()));
	    setBaseInstances(new Instances(r));
	    r.close();
	  } catch (Exception ex) {
	    m_Log.statusMessage("Problem reading " + u);
	    JOptionPane.showMessageDialog(PreprocessPanel.this,
					  "Couldn't read from URL:\n"
					  + u + "\n"
					  + ex.getMessage(),
					  "Load Instances",
					  JOptionPane.ERROR_MESSAGE);
	  }

	  m_IOThread = null;
	}
      };
      m_IOThread.setPriority(Thread.MIN_PRIORITY); // UI has most priority
      m_IOThread.start();
    } else {
      JOptionPane.showMessageDialog(this,
				    "Can't load at this time,\n"
				    + "currently busy with other IO",
				    "Load Instances",
				    JOptionPane.WARNING_MESSAGE);
    }
  }
  
  /**
   * Tests out the instance-preprocessing panel from the command line.
   *
   * @param args ignored
   */
  public static void main(String [] args) {

    try {
      final JFrame jf = new JFrame("Weka Knowledge Explorer: Preprocess");
      jf.getContentPane().setLayout(new BorderLayout());
      final PreprocessPanel sp = new PreprocessPanel();
      jf.getContentPane().add(sp, BorderLayout.CENTER);
      weka.gui.LogPanel lp = new weka.gui.LogPanel();
      sp.setLog(lp);
      jf.getContentPane().add(lp, BorderLayout.SOUTH);
      jf.addWindowListener(new WindowAdapter() {
	public void windowClosing(WindowEvent e) {
	  jf.dispose();
	  System.exit(0);
	}
      });
      jf.pack();
      jf.setSize(800, 600);
      jf.setVisible(true);
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
