/*
 *    SetupPanel.java
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


package weka.gui.experiment;

import weka.core.Tag;
import weka.core.SelectedTag;
import weka.core.Utils;

import weka.gui.ExtensionFileFilter;
import weka.gui.SelectedTagEditor;
import weka.gui.GenericObjectEditor;
import weka.gui.GenericArrayEditor;
import weka.gui.PropertyPanel;
import weka.gui.FileEditor;

import weka.experiment.Experiment;
import weka.experiment.ResultProducer;
import weka.experiment.ResultListener;
import weka.experiment.CrossValidationResultProducer;
import weka.experiment.DatabaseResultListener;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.BufferedInputStream;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.SwingConstants;
import javax.swing.JTextField;
import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;


/** 
 * This panel controls the configuration of an experiment.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.5 $
 */
public class SetupPanel extends JPanel {

  /** The experiment being configured */
  protected Experiment m_Exp;

  /** Click to load an experiment */
  protected JButton m_OpenBut = new JButton("Open...");

  /** Click to save an experiment */
  protected JButton m_SaveBut = new JButton("Save...");

  /** Click to create a new experiment with default settings */
  protected JButton m_NewBut = new JButton("New");

  /** A filter to ensure only experiment files get shown in the chooser */
  protected FileFilter m_ExpFilter = 
    new ExtensionFileFilter(".exp", "Experiment configuration files");

  /** The file chooser for selecting experiments */
  protected JFileChooser m_FileChooser = new JFileChooser();

  /** The ResultProducer editor */
  protected GenericObjectEditor m_RPEditor = new GenericObjectEditor();

  /** The panel to contain the ResultProducer editor */
  protected PropertyPanel m_RPEditorPanel = new PropertyPanel(m_RPEditor);

  /** The ResultListener editor */
  protected GenericObjectEditor m_RLEditor = new GenericObjectEditor();

  /** The panel to contain the ResultListener editor */
  protected PropertyPanel m_RLEditorPanel = new PropertyPanel(m_RLEditor);

  /** The panel that configures iteration on custom resultproducer property */
  protected GeneratorPropertyIteratorPanel m_GeneratorPropertyPanel
    = new GeneratorPropertyIteratorPanel();

  /** The panel for configuring run numbers */
  protected RunNumberPanel m_RunNumberPanel = new RunNumberPanel();

  /** The panel for configuring selected datasets */
  protected DatasetListPanel m_DatasetListPanel = new DatasetListPanel();

  /** Area for user notes */
  protected JTextArea m_NotesText = new JTextArea();

  /**
   * Manages sending notifications to people when we change the experiment,
   * at this stage, only the resultlistener so the resultpanel can update.
   */
  protected PropertyChangeSupport m_Support = new PropertyChangeSupport(this);
  
  // Registers the appropriate property editors
  static {
    System.err.println("---Registering Weka Editors---");
    java.beans.PropertyEditorManager
      .registerEditor(java.io.File.class,
		      FileEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(weka.experiment.ResultListener.class,
		      GenericObjectEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(weka.experiment.ResultProducer.class,
		      GenericObjectEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(weka.experiment.SplitEvaluator.class,
		      GenericObjectEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(weka.classifiers.Classifier.class,
		      GenericObjectEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(weka.classifiers.Classifier [].class,
		      GenericArrayEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(weka.filters.Filter.class,
		      GenericObjectEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(weka.attributeSelection.ASEvaluation.class,
		      GenericObjectEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(weka.attributeSelection.ASSearch.class,
		      GenericObjectEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(SelectedTag.class,
		      SelectedTagEditor.class);
  }
  
  /**
   * Creates the setup panel with the supplied initial experiment.
   *
   * @param exp a value of type 'Experiment'
   */
  public SetupPanel(Experiment exp) {

    this();
    setExperiment(exp);
  }
  
  /**
   * Creates the setup panel with no initial experiment.
   */
  public SetupPanel() {

    m_NewBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	setExperiment(new Experiment());
      }
    });
    m_SaveBut.setEnabled(false);
    m_SaveBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	saveExperiment();
      }
    });
    m_OpenBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	openExperiment();
      }
    });
    m_FileChooser.setFileFilter(m_ExpFilter);
    m_FileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    
    m_RPEditor.setClassType(ResultProducer.class);
    m_RPEditor.setEnabled(false);
    m_RPEditor.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
	m_Exp.setResultProducer((ResultProducer) m_RPEditor.getValue());
	m_Exp.setUsePropertyIterator(false);
	m_Exp.setPropertyArray(null);
	m_Exp.setPropertyPath(null);
	m_GeneratorPropertyPanel.setExperiment(m_Exp);
	repaint();
      }
    });

    m_RLEditor.setClassType(ResultListener.class);
    m_RLEditor.setEnabled(false);
    m_RLEditor.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
	m_Exp.setResultListener((ResultListener) m_RLEditor.getValue());
	m_Support.firePropertyChange("", null, null);
	repaint();
      }
    });

    m_NotesText.setEnabled(false);
    m_NotesText.setEditable(true);
    m_NotesText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    m_NotesText.addKeyListener(new KeyAdapter() {
      public void keyReleased(KeyEvent e) {
	m_Exp.setNotes(m_NotesText.getText());
      }
    });
    m_NotesText.addFocusListener(new FocusAdapter() {
      public void focusLost(FocusEvent e) {
	m_Exp.setNotes(m_NotesText.getText());
      }
    });

    // Set up the GUI layout
    JPanel buttons = new JPanel();
    buttons.setLayout(new GridLayout(1, 3));
    buttons.add(m_OpenBut);
    buttons.add(m_SaveBut);
    buttons.add(m_NewBut);
    
    JPanel src = new JPanel();
    src.setLayout(new BorderLayout());
    src.setBorder(BorderFactory.createCompoundBorder(
		  BorderFactory.createTitledBorder("Result generator"),
		  BorderFactory.createEmptyBorder(0, 5, 5, 5)
		  ));
    src.add(m_RPEditorPanel, BorderLayout.NORTH);

    JPanel dest = new JPanel();
    dest.setLayout(new BorderLayout());
    dest.setBorder(BorderFactory.createCompoundBorder(
		   BorderFactory.createTitledBorder("Destination"),
		   BorderFactory.createEmptyBorder(0, 5, 5, 5)
		   ));
    dest.add(m_RLEditorPanel, BorderLayout.NORTH);

    JPanel simpleIterators = new JPanel();
    simpleIterators.setLayout(new BorderLayout());
    simpleIterators.add(m_RunNumberPanel, BorderLayout.NORTH);
    simpleIterators.add(m_DatasetListPanel, BorderLayout.CENTER);
    JPanel iterators = new JPanel();
    iterators.setLayout(new GridLayout(1, 2));
    iterators.add(simpleIterators);
    iterators.add(m_GeneratorPropertyPanel);

    JPanel top = new JPanel();
    top.setLayout(new GridLayout(2, 1));
    top.add(dest);
    top.add(src);

    JPanel notes = new JPanel();
    notes.setLayout(new BorderLayout());
    notes.setBorder(BorderFactory.createTitledBorder("Notes"));
    notes.add(new JScrollPane(m_NotesText), BorderLayout.CENTER);
    
    JPanel p2 = new JPanel();
    p2.setLayout(new GridLayout(2, 1));
    p2.add(iterators);
    p2.add(notes);

    JPanel p3 = new JPanel();
    p3.setLayout(new BorderLayout());
    p3.add(buttons, BorderLayout.NORTH);
    p3.add(top, BorderLayout.SOUTH);
    setLayout(new BorderLayout());
    add(p3, BorderLayout.NORTH);
    add(p2, BorderLayout.CENTER);
  }
  
  /**
   * Sets the experiment to configure.
   *
   * @param exp a value of type 'Experiment'
   */
  public void setExperiment(Experiment exp) {

    m_Exp = exp;
    m_SaveBut.setEnabled(true);
    m_RPEditor.setValue(m_Exp.getResultProducer());
    m_RPEditor.setEnabled(true);
    m_RPEditorPanel.repaint();
    m_RLEditor.setValue(m_Exp.getResultListener());
    m_RLEditor.setEnabled(true);
    m_RLEditorPanel.repaint();
    m_NotesText.setText(m_Exp.getNotes());
    m_NotesText.setEnabled(true);
    
    m_GeneratorPropertyPanel.setExperiment(m_Exp);
    m_RunNumberPanel.setExperiment(m_Exp);
    m_DatasetListPanel.setExperiment(m_Exp);
    m_Support.firePropertyChange("", null, null);
  }

  /**
   * Gets the currently configured experiment.
   *
   * @return the currently configured experiment.
   */
  public Experiment getExperiment() {

    return m_Exp;
  }
  
  /**
   * Prompts the user to select an experiment file and loads it.
   */
  private void openExperiment() {
    
    int returnVal = m_FileChooser.showOpenDialog(this);
    if (returnVal != JFileChooser.APPROVE_OPTION) {
      return;
    }
    File expFile = m_FileChooser.getSelectedFile();
    try {
      FileInputStream fi = new FileInputStream(expFile);
      ObjectInputStream oi = new ObjectInputStream(
			     new BufferedInputStream(fi));
      Experiment exp = (Experiment)oi.readObject();
      oi.close();
      setExperiment(exp);
      System.err.println("Opened experiment:\n" + m_Exp);
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(this, "Couldn't open experiment file:\n"
				    + expFile
				    + "\nReason:\n" + ex.getMessage(),
				    "Open Experiment",
				    JOptionPane.ERROR_MESSAGE);
      // Pop up error dialog
    }
  }

  /**
   * Prompts the user for a filename to save the experiment to, then saves
   * the experiment.
   */
  private void saveExperiment() {

    int returnVal = m_FileChooser.showSaveDialog(this);
    if (returnVal != JFileChooser.APPROVE_OPTION) {
      return;
    }
    File expFile = m_FileChooser.getSelectedFile();
    if (!expFile.getName().toLowerCase().endsWith(".exp")) {
      expFile = new File(expFile.getParent(), expFile.getName() + ".exp");
    }
    try {
      FileOutputStream fo = new FileOutputStream(expFile);
      ObjectOutputStream oo = new ObjectOutputStream(
			      new BufferedOutputStream(fo));
      oo.writeObject(m_Exp);
      oo.close();
      System.err.println("Saved experiment:\n" + m_Exp);
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(this, "Couldn't save experiment file:\n"
				    + expFile
				    + "\nReason:\n" + ex.getMessage(),
				    "Save Experiment",
				    JOptionPane.ERROR_MESSAGE);
    }
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
   * Tests out the experiment setup from the command line.
   *
   * @param args arguments to the program.
   */
  public static void main(String [] args) {

    try {
      boolean readExp = Utils.getFlag('l', args);
      final boolean writeExp = Utils.getFlag('s', args);
      final String expFile = Utils.getOption('f', args);
      if ((readExp || writeExp) && (expFile.length() == 0)) {
	throw new Exception("A filename must be given with the -f option");
      }
      Experiment exp = null;
      if (readExp) {
	FileInputStream fi = new FileInputStream(expFile);
	ObjectInputStream oi = new ObjectInputStream(
			       new BufferedInputStream(fi));
	exp = (Experiment)oi.readObject();
	oi.close();
      } else {
	exp = new Experiment();
      }
      System.err.println("Initial Experiment:\n" + exp.toString());
      final JFrame jf = new JFrame("Weka Experiment Setup");
      jf.getContentPane().setLayout(new BorderLayout());
      final SetupPanel sp = new SetupPanel();
      //sp.setBorder(BorderFactory.createTitledBorder("Setup"));
      jf.getContentPane().add(sp, BorderLayout.CENTER);
      jf.addWindowListener(new WindowAdapter() {
	public void windowClosing(WindowEvent e) {
	  System.err.println("\nFinal Experiment:\n"
			     + sp.m_Exp.toString());
	  // Save the experiment to a file
	  if (writeExp) {
	    try {
	      FileOutputStream fo = new FileOutputStream(expFile);
	      ObjectOutputStream oo = new ObjectOutputStream(
				      new BufferedOutputStream(fo));
	      oo.writeObject(sp.m_Exp);
	      oo.close();
	    } catch (Exception ex) {
	      System.err.println("Couldn't write experiment to: " + expFile
				 + '\n' + ex.getMessage());
	    }
	  }
	  jf.dispose();
	  System.exit(0);
	}
      });
      jf.pack();
      jf.setVisible(true);
      System.err.println("Short nap");
      Thread.currentThread().sleep(3000);
      System.err.println("Done");
      sp.setExperiment(exp);
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
