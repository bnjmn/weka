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
 *    CostMatrixEditor.java
 *    Copyright (C) 1999 Intelligenesis Corp.
 *
 */


package weka.gui;

import weka.classifiers.CostMatrix;
import weka.core.Matrix;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.File;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JButton;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;


/** 
 * A PropertyEditor for CostMatrices. Allows editing of individual elements
 * of the cost matrix, as well as simple operations like loading, saving.
 *
 * @author Len Trigg (len@intelligenesis.net)
 * @version $Revision: 1.5 $
 */
public class CostMatrixEditor implements PropertyEditor {

  /** The current cost matrix */
  private CostMatrix m_CostMatrix = new CostMatrix(2);

  /** Handles property change notification */
  private PropertyChangeSupport m_Support = new PropertyChangeSupport(this);

  /** The instantiated custom property editor */
  private CostMatrixEditorPanel m_EditorPanel = null;


  /** Class that wraps a TableModel around a CostMatrix */
  class CostMatrixTableModel extends AbstractTableModel {
    
    /** The CostMatrix providing data for the table model */
    private CostMatrix m_Matrix;

    /**
     * Constructs the table model with a given CostMatrix
     *
     * @param m theCostMatrix
     */
    public CostMatrixTableModel(CostMatrix m) {

      m_Matrix = m;
    }

    /**
     * Updates the table model to use a new CostMatrix
     *
     * @param m theCostMatrix
     */
    public void setMatrix(CostMatrix m) {

      m_Matrix = m;
      fireTableStructureChanged();
    }

    /** Gets the number of rows in the table */
    public int getRowCount() {

      return m_Matrix.numRows();
    }

    /** Gets the number of columns in the table */
    public int getColumnCount() {

      return m_Matrix.numColumns();
    }

    /**
     * Gets an element from the table.
     *
     * @param row the row index
     * @param column the column index
     * @return the element at row, column
     */
    public Object getValueAt(int row, int column) {

      return new Double(m_Matrix.getElement(row, column));
    }

    /**
     * Sets an element in the table
     *
     * @param value the object to place in the table
     * @param row the row index
     * @param column the column index
     */
    public void setValueAt(Object value, int row, int column) {

      double newVal = 0;

      if (value instanceof String) {
	try {
	  newVal = Double.valueOf((String)value).doubleValue();
	} catch (Exception ex) {
	  return;
	}
      } else if (value instanceof Double) {
	newVal = ((Double)value).doubleValue();
      } else {
	return;
      }

      m_Matrix.setElement(row, column, newVal);
      fireTableCellUpdated(row, column);
    }

    /**
     * Determines if a cell is editable. For our purposes, all
     * cells may be edited.
     *
     * @param row the row index
     * @param column the column index
     * @return true if the cell is editable
     */
    public boolean isCellEditable(int row, int column) {

      return true;
    }

    /** Resets the CostMatrix to default values */
    public void defaults() {

      m_Matrix.initialize();
      fireTableDataChanged();
    }
  }


  /** The custom editing component */
  class CostMatrixEditorPanel extends JPanel {

    /** The TableModel containing the cost matrix */
    private CostMatrixTableModel m_Model = 
      new CostMatrixTableModel(m_CostMatrix);

    /** The table component */
    private JTable m_Table = new JTable(m_Model);

    /** Click to reset cells to default values */
    private JButton m_DefaultBut = new JButton("Defaults");

    /** Click to open a cost file from disk */
    private JButton m_OpenBut = new JButton("Open...");

    /** Click to save the current cost matrix to disk */
    private JButton m_SaveBut = new JButton("Save...");

    /** A text field for entering new cost matrix size */
    private JTextField m_NumClasses = new JTextField("2");

    /** A filter to only show cost files in the filechooser */
    protected FileFilter m_CostFilter =
      new ExtensionFileFilter(CostMatrix.FILE_EXTENSION, 
                              "Misclassification cost files");

    /** The filechooser for opening and saving cost files */
    private JFileChooser m_FileChooser
      = new JFileChooser(new File(System.getProperty("user.dir")));

    /** Sets up the cost matrix editor panel */
    public CostMatrixEditorPanel() {

      m_Model.addTableModelListener(new TableModelListener() {
	public void tableChanged(TableModelEvent e) {
	  m_Support.firePropertyChange("", null, null);
	}
      });

      m_DefaultBut.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  m_Model.defaults();
	}
      });

      m_NumClasses.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  try {
	    int numClasses = Integer.parseInt(m_NumClasses.getText());
	    if (numClasses > 0) {
	      setValue(new CostMatrix(numClasses));
	    }
	  } catch (Exception ex) {
	  }
	}
      });
      
      m_OpenBut.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  openMatrix();
	}
      });

      m_SaveBut.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  saveMatrix();
	}
      });

      m_FileChooser.setFileFilter(m_CostFilter);
      m_FileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

      setVisibleMatrix(m_CostMatrix);


      // Lay out the GUI
      JPanel classes = new JPanel();
      classes.setLayout(new GridLayout(1, 2, 5, 5));
      classes.add(new JLabel("Classes:", SwingConstants.RIGHT));
      classes.add(m_NumClasses);
      JPanel buttons = new JPanel();
      buttons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      buttons.setLayout(new GridLayout(4, 1, 5, 5));
      buttons.add(m_DefaultBut);
      buttons.add(m_OpenBut);
      buttons.add(m_SaveBut);
      buttons.add(classes);
      JPanel right = new JPanel();
      right.setLayout(new BorderLayout());
      right.add(buttons, BorderLayout.NORTH);
      right.add(Box.createVerticalGlue(), BorderLayout.CENTER);

      JPanel table = new JPanel();
      table.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      table.setLayout(new BorderLayout());
      table.add(new JScrollPane(m_Table), BorderLayout.CENTER);

      setLayout(new BorderLayout());
      add(table, BorderLayout.CENTER);
      add(right, BorderLayout.EAST);
    }

    /** Opens a cost file selected from a filechooser */
    private void openMatrix() {

      int returnVal = m_FileChooser.showOpenDialog(this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
	File selected = m_FileChooser.getSelectedFile();
	Reader r = null;
	try {
	  r = new BufferedReader(new FileReader(selected));
	  CostMatrix c = new CostMatrix(r);
	  r.close();
	  setValue(c);
	} catch (Exception ex) {
	  JOptionPane.showMessageDialog(this,
					"Couldn't read from file: "
					+ selected.getName() 
					+ "\n" + ex.getMessage(),
					"Open cost file",
					JOptionPane.ERROR_MESSAGE);
	}
      }
    }

    /** Saves to a cost file selected from a filechooser */
    private void saveMatrix() {

      int returnVal = m_FileChooser.showSaveDialog(this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
	File sFile = m_FileChooser.getSelectedFile();
	if (!sFile.getName().toLowerCase()
            .endsWith(CostMatrix.FILE_EXTENSION)) {
	  sFile = new File(sFile.getParent(), sFile.getName() 
                           + CostMatrix.FILE_EXTENSION);
	}
	Writer w = null;
	try {
	  w = new BufferedWriter(new FileWriter(sFile));
	  m_CostMatrix.write(w);
	  w.close();
	} catch (Exception ex) {
	  JOptionPane.showMessageDialog(this,
					"Couldn't write to file: "
					+ sFile.getName() 
					+ "\n" + ex.getMessage(),
					"Save cost file",
					JOptionPane.ERROR_MESSAGE);
	}
      }
    }

    /** 
     * Updates the GUI components when the cost matrix changes 
     *
     * @param c the new cost matrix to display
     */
    private void setVisibleMatrix(CostMatrix c) {

      m_NumClasses.setText("" + c.size());
      m_Model.setMatrix(c);
    }
  }

  /**
   * Sets the current object array.
   *
   * @param o an object that must be an array.
   */
  public void setValue(Object o) {

    m_CostMatrix = (CostMatrix) o;
    if (m_EditorPanel != null) {
      m_EditorPanel.setVisibleMatrix(m_CostMatrix);
    }
  }

  /**
   * Gets the current object array.
   *
   * @return the current object array
   */
  public Object getValue() {

    return m_CostMatrix;
  }
  
  /**
   * Supposedly returns an initialization string to create a classifier
   * identical to the current one, including it's state, but this doesn't
   * appear possible given that the initialization string isn't supposed to
   * contain multiple statements.
   *
   * @return the java source code initialisation string
   */
  public String getJavaInitializationString() {

    return "null";
  }

  /**
   * Returns true to indicate that we can paint a representation of the
   * string array
   *
   * @return true
   */
  public boolean isPaintable() {
    return true;
  }

  /**
   * Paints a representation of the current classifier.
   *
   * @param gfx the graphics context to use
   * @param box the area we are allowed to paint into
   */
  public void paintValue(Graphics gfx, Rectangle box) {
    
    FontMetrics fm = gfx.getFontMetrics();
    int vpad = (box.height - fm.getAscent()) / 2 - 1;
    String rep = "" + m_CostMatrix.size() + "x" + m_CostMatrix.size()
      + " cost matrix";
    gfx.drawString(rep, 2, fm.getHeight() + vpad);
  }

  /**
   * Returns null as we don't support getting/setting values as text.
   *
   * @return null
   */
  public String getAsText() {
    return null;
  }

  /**
   * Returns null as we don't support getting/setting values as text. 
   *
   * @param text the text value
   * @exception IllegalArgumentException as we don't support
   * getting/setting values as text.
   */
  public void setAsText(String text) throws IllegalArgumentException {
    throw new IllegalArgumentException(text);
  }

  /**
   * Returns null as we don't support getting values as tags.
   *
   * @return null
   */
  public String[] getTags() {
    return null;
  }

  /**
   * Returns true because we do support a custom editor.
   *
   * @return true
   */
  public boolean supportsCustomEditor() {
    return true;
  }
  
  /**
   * Returns the array editing component.
   *
   * @return a value of type 'java.awt.Component'
   */
  public java.awt.Component getCustomEditor() {

    if (m_EditorPanel == null) {
      m_EditorPanel = new CostMatrixEditorPanel();
    }
    return m_EditorPanel;
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
   * Tests out the array editor from the command line.
   *
   * @param args ignored
   */
  public static void main(String [] args) {

    try {
      System.err.println("---Registering Weka Editors---");
      java.beans.PropertyEditorManager
	.registerEditor(weka.classifiers.Classifier.class,
			GenericObjectEditor.class);
      java.beans.PropertyEditorManager
	.registerEditor(weka.core.SelectedTag.class,
			SelectedTagEditor.class);
      java.beans.PropertyEditorManager
	.registerEditor(weka.filters.Filter.class,
			GenericObjectEditor.class);
      java.beans.PropertyEditorManager
	.registerEditor(CostMatrix.class,
			CostMatrixEditor.class);
      final CostMatrixEditor ce = new CostMatrixEditor();
      CostMatrix c = new CostMatrix(3);
      ce.addPropertyChangeListener(new PropertyChangeListener() {
	public void propertyChange(PropertyChangeEvent e) {
	  System.err.println("PropertyChange");
	}
      });
      PropertyDialog pd = new PropertyDialog(ce, 100, 100);
      pd.setSize(250,150);
      pd.addWindowListener(new java.awt.event.WindowAdapter() {
	public void windowClosing(java.awt.event.WindowEvent e) {
	  System.exit(0);
	}
      });
      ce.setValue(c);
      //ce.validate();
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }

}

