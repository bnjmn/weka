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
 *    PreprocessPanel.java
 *    Copyright (C) 2003 Richard Kirkby, Len Trigg
 *
 */

package weka.gui.explorer;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import weka.core.Instances;
import weka.core.SerializedObject;
import weka.core.converters.Loader;
import weka.experiment.InstanceQuery;
import weka.filters.Filter;
import weka.filters.UnsupervisedFilter;
import weka.gui.AttributeListPanel;
import weka.gui.AttributeSummaryPanel;
import weka.gui.ExtensionFileFilter;
import weka.gui.FileEditor;
import weka.gui.GenericArrayEditor;
import weka.gui.GenericObjectEditor;
import weka.gui.InstancesSummaryPanel;
import weka.gui.Logger;
import weka.gui.PropertyDialog;
import weka.gui.SysErrLog;
import weka.gui.TaskLogger;
import weka.gui.PropertyPanel;
import weka.gui.AttributeVisualizationPanel;
import weka.core.UnassignedClassException;

/** 
 * This panel controls simple preprocessing of instances. Summary
 * information on instances and attributes is shown. Filters may be
 * configured to alter the set of instances. Altered instances may
 * also be saved.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.31 $
 */
public class PreprocessPanel extends JPanel {
  
  /** Displays simple stats on the working instances */
  protected InstancesSummaryPanel m_InstSummaryPanel =
    new InstancesSummaryPanel();

  /** Click to load base instances from a file */
  protected JButton m_OpenFileBut = new JButton("Open file...");

  /** Click to load base instances from a URL */
  protected JButton m_OpenURLBut = new JButton("Open URL...");

  /** Click to load base instances from a Database */
  protected JButton m_OpenDBBut = new JButton("Open DB...");

  /** Lets the user enter a DB query */
  protected GenericObjectEditor m_DatabaseQueryEditor = 
    new GenericObjectEditor();

  /** Click to revert back to the last saved point */
  protected JButton m_UndoBut = new JButton("Undo");

  /** Click to apply filters and save the results */
  protected JButton m_SaveBut = new JButton("Save...");
  
  /** Panel to let the user toggle attributes */
  protected AttributeListPanel m_AttPanel = new AttributeListPanel();

  /** Displays summary stats on the selected attribute */
  protected AttributeSummaryPanel m_AttSummaryPanel =
    new AttributeSummaryPanel();

  /** Lets the user configure the filter */
  protected GenericObjectEditor m_FilterEditor =
    new GenericObjectEditor();

  /** Filter configuration */
  protected PropertyPanel m_FilterPanel = new PropertyPanel(m_FilterEditor);

  /** Click to apply filters and save the results */
  protected JButton m_ApplyFilterBut = new JButton("Apply");

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
 
  /** The working instances */
  protected Instances m_Instances;

  /** The visualization of the attribute values */
  protected AttributeVisualizationPanel m_AttVisualizePanel = new AttributeVisualizationPanel();

  /** Keeps track of undo points */
  protected File[] m_tempUndoFiles = new File[20]; // set number of undo ops here

  /** The next available slot for an undo point */
  protected int m_tempUndoIndex = 0;
  
  /**
   * Manages sending notifications to people when we change the set of
   * working instances.
   */
  protected PropertyChangeSupport m_Support = new PropertyChangeSupport(this);

  /** A thread for loading/saving instances from a file or URL */
  protected Thread m_IOThread;

  /** The message logger */
  protected Logger m_Log = new SysErrLog();
  
  static {
    java.beans.PropertyEditorManager
      .registerEditor(java.io.File.class,
                      FileEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(weka.core.SelectedTag.class,
		      weka.gui.SelectedTagEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(weka.filters.UnsupervisedFilter.class,
		      weka.gui.GenericObjectEditor.class);
     java.beans.PropertyEditorManager
      .registerEditor(weka.attributeSelection.ASSearch.class,
		      weka.gui.GenericObjectEditor.class);
     java.beans.PropertyEditorManager
      .registerEditor(weka.attributeSelection.ASEvaluation.class,
		      weka.gui.GenericObjectEditor.class);
     java.beans.PropertyEditorManager
      .registerEditor(weka.experiment.InstanceQuery.class,
		      weka.gui.GenericObjectEditor.class);
     java.beans.PropertyEditorManager
       .registerEditor(weka.core.converters.Loader.class,
		      weka.gui.GenericObjectEditor.class);
  }
  
  /**
   * Creates the instances panel with no initial instances.
   */
  public PreprocessPanel() {

    // Create/Configure/Connect components
    try {
    m_DatabaseQueryEditor.setClassType(weka.experiment.InstanceQuery.class);
    m_DatabaseQueryEditor.setValue(new weka.experiment.InstanceQuery());
    ((GenericObjectEditor.GOEPanel)m_DatabaseQueryEditor.getCustomEditor())
      .addOkListener(new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
	    setInstancesFromDBQ();
	  }
	});
    } catch (Exception ex) {
    }
    m_FilterEditor.setClassType(weka.filters.UnsupervisedFilter.class);
    m_OpenFileBut.setToolTipText("Open a set of instances from a file");
    m_OpenURLBut.setToolTipText("Open a set of instances from a URL");
    m_OpenDBBut.setToolTipText("Open a set of instances from a database");
    m_UndoBut.setToolTipText("Undo the last change to the dataset");
    m_SaveBut.setToolTipText("Save the working relation to a file");
    m_ApplyFilterBut.setToolTipText("Apply the current filter to the data");
    m_FileChooser.setFileFilter(m_ArffFilter);
    m_FileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    m_OpenURLBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	setInstancesFromURLQ();
      }
    });
    m_OpenDBBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	PropertyDialog pd = new PropertyDialog(m_DatabaseQueryEditor,100,100);
      }
    });
    m_OpenFileBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	setInstancesFromFileQ();
      }
    });
    m_UndoBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	undo();
      }
    });
    m_SaveBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	saveWorkingInstancesToFileQ();
      }
    });
    m_ApplyFilterBut.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  applyFilter();
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
		m_AttVisualizePanel.setAttribute(i);
		break;
	      }
	    }
	  }
	}
    });

    m_InstSummaryPanel.setBorder(BorderFactory
				 .createTitledBorder("Current relation"));
    m_AttPanel.setBorder(BorderFactory
		    .createTitledBorder("Attributes"));
    m_AttSummaryPanel.setBorder(BorderFactory
		    .createTitledBorder("Selected attribute"));
    m_UndoBut.setEnabled(false);
    m_ApplyFilterBut.setEnabled(false);
    
    // Set up the GUI layout
    JPanel buttons = new JPanel();
    buttons.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
    buttons.setLayout(new GridLayout(1, 6, 5, 5));
    buttons.add(m_OpenFileBut);
    buttons.add(m_OpenURLBut);
    buttons.add(m_OpenDBBut);
    buttons.add(m_UndoBut);
    buttons.add(m_SaveBut);

    JPanel attInfo = new JPanel();

    attInfo.setLayout(new BorderLayout());
    attInfo.add(m_AttPanel, BorderLayout.CENTER);

    JPanel filter = new JPanel();
    filter.setBorder(BorderFactory
		    .createTitledBorder("Filter"));
    filter.setLayout(new BorderLayout());
    filter.add(m_FilterPanel, BorderLayout.CENTER);
    filter.add(m_ApplyFilterBut, BorderLayout.EAST); 

    JPanel attVis = new JPanel();
    attVis.setLayout( new GridLayout(2,1) );
    attVis.add(m_AttSummaryPanel);
    attVis.add(m_AttVisualizePanel);

    JPanel lhs = new JPanel();
    lhs.setLayout(new BorderLayout());
    lhs.add(m_InstSummaryPanel, BorderLayout.NORTH);
    lhs.add(attInfo, BorderLayout.CENTER);

    JPanel rhs = new JPanel();
    rhs.setLayout(new BorderLayout());
    rhs.add(attVis, BorderLayout.CENTER);

    JPanel relation = new JPanel();
    relation.setLayout(new GridLayout(1, 2));
    relation.add(lhs);
    relation.add(rhs);

    JPanel middle = new JPanel();
    middle.setLayout(new BorderLayout());
    middle.add(filter, BorderLayout.NORTH);
    middle.add(relation, BorderLayout.CENTER);

    setLayout(new BorderLayout());
    add(buttons, BorderLayout.NORTH);
    add(middle, BorderLayout.CENTER);
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
  public void setInstances(Instances inst) {

    m_Instances = inst;
    try {
      Runnable r = new Runnable() {
	public void run() {
	  m_InstSummaryPanel.setInstances(m_Instances);
	  m_AttPanel.setInstances(m_Instances);
	  m_AttSummaryPanel.setInstances(m_Instances);
	  m_AttVisualizePanel.setInstances(m_Instances);

	  // select the first attribute in the list
	  m_AttPanel.getSelectionModel().setSelectionInterval(0, 0);
	  m_AttSummaryPanel.setAttribute(0);
	  m_AttVisualizePanel.setAttribute(0);

	  m_ApplyFilterBut.setEnabled(true);

	  m_Log.logMessage("Base relation is now "
			   + m_Instances.relationName()
			   + " (" + m_Instances.numInstances()
			   + " instances)");
	  m_Log.statusMessage("OK");
	  // Fire a propertychange event
	  m_Support.firePropertyChange("", null, null);
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
   * Gets the working set of instances.
   *
   * @return the working instances
   */
  public Instances getInstances() {

    return m_Instances;
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
   * Passes the dataset through the filter that has been configured for use.
   */
  protected void applyFilter() {

    if (m_IOThread == null) {
      m_IOThread = new Thread() {
	public void run() {
	  try {

	    Filter filter = (Filter) m_FilterEditor.getValue();
	    if (filter != null) {
	    
	      if (m_Log instanceof TaskLogger) {
		((TaskLogger)m_Log).taskStarted();
	      }
	      m_Log.statusMessage("Saving undo information");
	      addUndoPoint();
	      m_Log.statusMessage("Passing dataset through filter "
				  + filter.getClass().getName());
	      filter.setInputFormat(m_Instances);
	      m_Instances = filter.useFilter(m_Instances, filter);
	      setInstances(m_Instances);
	      if (m_Log instanceof TaskLogger) {
		((TaskLogger)m_Log).taskFinished();
	      }
	    }
	    
	  } catch (Exception ex) {
	
	    if (m_Log instanceof TaskLogger) {
	      ((TaskLogger)m_Log).taskFinished();
	    }
	    // Pop up an error optionpane
	    JOptionPane.showMessageDialog(PreprocessPanel.this,
					  "Problem filtering instances:\n"
					  + ex.getMessage(),
					  "Apply Filter",
					  JOptionPane.ERROR_MESSAGE);
	    ex.printStackTrace();
	    m_Log.logMessage("Problem filtering instances: " + ex.getMessage());
	  }
	  m_IOThread = null;
	}
      };
      m_IOThread.setPriority(Thread.MIN_PRIORITY); // UI has most priority
      m_IOThread.start();
    } else {
      JOptionPane.showMessageDialog(this,
				    "Can't apply filter at this time,\n"
				    + "currently busy with other IO",
				    "Apply Filter",
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
	saveInstancesToFile(selected, m_Instances);
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
  public void setInstancesFromFileQ() {
    
    if (m_IOThread == null) {
      int returnVal = m_FileChooser.showOpenDialog(this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
	File selected = m_FileChooser.getSelectedFile();
	try {
	  addUndoPoint();
	} catch (Exception ignored) {}
	setInstancesFromFile(selected);
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
  public void setInstancesFromDBQ() {
    if (m_IOThread == null) {
      try {
	InstanceQuery InstQ = 
	  (InstanceQuery)m_DatabaseQueryEditor.getValue();
	
	InstQ.connectToDatabase();      
	try {
	  addUndoPoint();
	} catch (Exception ignored) {}
	setInstancesFromDB(InstQ);
      } catch (Exception ex) {
	JOptionPane.showMessageDialog(this,
				      "Problem connecting to database:\n"
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
  public void setInstancesFromURLQ() {
    
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
	  try {
	    addUndoPoint();
	  } catch (Exception ignored) {}
	  setInstancesFromURL(url);
	}
      } catch (Exception ex) {
	ex.printStackTrace();
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
   * Saves the current instances to the supplied file.
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
   * Pops up generic object editor with list of conversion filters
   *
   * @param f the File
   */
  private void converterQuery(final File f) {
    final GenericObjectEditor convEd = new GenericObjectEditor(true);

    try {
      convEd.setClassType(weka.core.converters.Loader.class);
      convEd.setValue(new weka.core.converters.CSVLoader());
      ((GenericObjectEditor.GOEPanel)convEd.getCustomEditor())
	.addOkListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
	      tryConverter((Loader)convEd.getValue(), f);
	    }
	  });
    } catch (Exception ex) {
    }

    PropertyDialog pd = new PropertyDialog(convEd, 100, 100);
  }

  /**
   * Applies the selected converter
   *
   * @param cnv the converter to apply to the input file
   * @param f the input file
   */
  private void tryConverter(final Loader cnv, final File f) {

    if (m_IOThread == null) {
      m_IOThread = new Thread() {
	  public void run() {
	    try {
	      cnv.setSource(f);
	      Instances inst = cnv.getDataSet();
	      setInstances(inst);
	    } catch (Exception ex) {
	      m_Log.statusMessage(cnv.getClass().getName()+" failed to load "
				 +f.getName());
	      JOptionPane.showMessageDialog(PreprocessPanel.this,
					    cnv.getClass().getName()+" failed to load '"
					    + f.getName() + "'.\n"
					    + "Reason:\n" + ex.getMessage(),
					    "Convert File",
					    JOptionPane.ERROR_MESSAGE);
	      m_IOThread = null;
	      converterQuery(f);
	    }
	    m_IOThread = null;
	  }
	};
      m_IOThread.setPriority(Thread.MIN_PRIORITY); // UI has most priority
      m_IOThread.start();
    }
  }

  /**
   * Loads results from a set of instances contained in the supplied
   * file. This is started in the IO thread, and a dialog is popped up
   * if there's a problem.
   *
   * @param f a value of type 'File'
   */
  public void setInstancesFromFile(final File f) {
      
    if (m_IOThread == null) {
      m_IOThread = new Thread() {
	public void run() {
	  try {
	    m_Log.statusMessage("Reading from file...");
	    Reader r = new BufferedReader(new FileReader(f));
	    setInstances(new Instances(r));
	    r.close();
	  } catch (Exception ex) {
	    m_Log.statusMessage("File '" + f.getName() + "' not recognised as an arff file.");
	    m_IOThread = null;
	    if (JOptionPane.showOptionDialog(PreprocessPanel.this,
					     "File '" + f.getName()
					     + "' not recognised as an arff file.\n"
					     + "Reason:\n" + ex.getMessage(),
					     "Load Instances",
					     0,
					     JOptionPane.ERROR_MESSAGE,
					     null,
					     new String[] {"OK", "Use Converter"},
					     null) == 1) {
	    
	      converterQuery(f);
	    }
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
  public void setInstancesFromDB(final InstanceQuery iq) {
    if (m_IOThread == null) {
      m_IOThread = new Thread() {
	public void run() {
	  
	  try {
	    m_Log.statusMessage("Reading from database...");
	    final Instances i = iq.retrieveInstances();
	    SwingUtilities.invokeAndWait(new Runnable() {
	      public void run() {
		setInstances(new Instances(i));
	      }
	    });
	    iq.disconnectFromDatabase();
	  } catch (Exception ex) {
	    m_Log.statusMessage("Problem executing DB query "+m_SQLQ);
	    JOptionPane.showMessageDialog(PreprocessPanel.this,
					  "Couldn't read from database:\n"
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
  public void setInstancesFromURL(final URL u) {

    if (m_IOThread == null) {
      m_IOThread = new Thread() {
	public void run() {

	  try {
	    m_Log.statusMessage("Reading from URL...");
	    Reader r = new BufferedReader(
		       new InputStreamReader(u.openStream()));
	    setInstances(new Instances(r));
	    r.close();
	  } catch (Exception ex) {
	    ex.printStackTrace();
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
   * Backs up the current state of the dataset, so the changes can be undone.
   */
  public void addUndoPoint() throws Exception {

    // create temporary file
    File tempFile = File.createTempFile("weka", null);
    tempFile.deleteOnExit();

    // write current dataset to file
    Writer w = new BufferedWriter(new FileWriter(tempFile));
    Instances h = new Instances(m_Instances, 0);
    w.write(h.toString());
    w.write("\n");
    for (int i = 0; i < m_Instances.numInstances(); i++) {
      w.write(m_Instances.instance(i).toString());
      w.write("\n");
    }
    w.close();

    // update undo file list
    if (m_tempUndoFiles[m_tempUndoIndex] != null) {
      // remove undo points that are too old
      m_tempUndoFiles[m_tempUndoIndex].delete();
    }
    m_tempUndoFiles[m_tempUndoIndex] = tempFile;
    if (++m_tempUndoIndex >= m_tempUndoFiles.length) {
      // wrap pointer around
      m_tempUndoIndex = 0;
    }

    m_UndoBut.setEnabled(true);
  }

  /**
   * Reverts to the last backed up version of the dataset.
   */
  public void undo() {

    if (--m_tempUndoIndex < 0) {
      // wrap pointer around
      m_tempUndoIndex = m_tempUndoFiles.length-1;
    }
    
    if (m_tempUndoFiles[m_tempUndoIndex] != null) {
      // load instances from the temporary file
      setInstancesFromFile(m_tempUndoFiles[m_tempUndoIndex]);

      // update undo file list
      m_tempUndoFiles[m_tempUndoIndex] = null;
    }
    
    // update undo button
    int temp = m_tempUndoIndex-1;
    if (temp < 0) {
      temp = m_tempUndoFiles.length-1;
    }
    m_UndoBut.setEnabled(m_tempUndoFiles[temp] != null);
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
