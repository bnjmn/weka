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
 * ArffPanel.java
 * Copyright (C) 2005 FracPete
 *
 */

package weka.gui.arffviewer;

import weka.core.Instances;
import weka.core.Undoable;
import weka.core.Utils;
import weka.gui.ComponentHelper;
import weka.gui.JTableHelper;
import weka.gui.ListSelectorDialog;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;

/**
 * A Panel representing an ARFF-Table and the associated filename.
 *
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.5 $ 
 */

public class ArffPanel 
  extends JPanel
  implements ActionListener, ChangeListener, MouseListener, Undoable {
  
  /** for serialization */
  static final long serialVersionUID = -4697041150989513939L;
  
  /** the name of the tab for instances that were set directly */
  public final static String       TAB_INSTANCES     = "Instances";
  
  private ArffTable             tableArff;
  private JPopupMenu            popupHeader;
  private JPopupMenu            popupRows;
  private JLabel                labelName;
  
  private JMenuItem             menuItemMean;
  private JMenuItem             menuItemSetAllValues;
  private JMenuItem             menuItemSetMissingValues;
  private JMenuItem             menuItemReplaceValues;
  private JMenuItem             menuItemRenameAttribute;
  private JMenuItem             menuItemAttributeAsClass;
  private JMenuItem             menuItemDeleteAttribute;
  private JMenuItem             menuItemDeleteAttributes;
  private JMenuItem             menuItemSortInstances;
  private JMenuItem             menuItemDeleteSelectedInstance;
  private JMenuItem             menuItemDeleteAllSelectedInstances;
  private JMenuItem             menuItemSearch;
  private JMenuItem             menuItemClearSearch;
  private JMenuItem             menuItemUndo;
  private JMenuItem             menuItemCopy;
  private JMenuItem             menuItemOptimalColWidth;
  private JMenuItem             menuItemOptimalColWidths;
  
  private String                filename;
  private String                title;
  private int                   currentCol;
  private boolean               changed;
  private HashSet               changeListeners;
  private String                lastSearch;
  private String                lastReplace;
  
  /**
   * initializes the panel with no data
   */
  public ArffPanel() {
    super();
    
    initialize();
    createPanel();
  }
  
  /**
   * initializes the panel and loads the specified file
   * 
   * @param filename	the file to load
   */
  public ArffPanel(String filename) {
    this();
    
    loadFile(filename);
  }
  
  /**
   * initializes the panel with the given data
   * 
   * @param data	the data to use
   */
  public ArffPanel(Instances data) {
    this();
    
    filename = "";
    
    setInstances(data);
  }
  
  /**
   * any member variables are initialized here
   */
  protected void initialize() {
    filename        = "";
    title           = "";
    currentCol      = -1;
    lastSearch      = "";
    lastReplace     = "";
    changed         = false;
    changeListeners = new HashSet();
  }
  
  /**
   * creates all the components in the frame
   */
  protected void createPanel() {
    JScrollPane                pane;
    
    setLayout(new BorderLayout());
    
    // header popup
    popupHeader  = new JPopupMenu();
    popupHeader.addMouseListener(this);
    menuItemMean = new JMenuItem("Get mean...");
    menuItemMean.addActionListener(this);
    popupHeader.add(menuItemMean);
    popupHeader.addSeparator();
    menuItemSetAllValues = new JMenuItem("Set all values to...");
    menuItemSetAllValues.addActionListener(this);
    popupHeader.add(menuItemSetAllValues);
    menuItemSetMissingValues = new JMenuItem("Set missing values to...");
    menuItemSetMissingValues.addActionListener(this);
    popupHeader.add(menuItemSetMissingValues);
    menuItemReplaceValues = new JMenuItem("Replace values with...");
    menuItemReplaceValues.addActionListener(this);
    popupHeader.add(menuItemReplaceValues);
    popupHeader.addSeparator();
    menuItemRenameAttribute = new JMenuItem("Rename attribute...");
    menuItemRenameAttribute.addActionListener(this);
    popupHeader.add(menuItemRenameAttribute);
    menuItemAttributeAsClass = new JMenuItem("Attribute as class");
    menuItemAttributeAsClass.addActionListener(this);
    popupHeader.add(menuItemAttributeAsClass);
    menuItemDeleteAttribute = new JMenuItem("Delete attribute");
    menuItemDeleteAttribute.addActionListener(this);
    popupHeader.add(menuItemDeleteAttribute);
    menuItemDeleteAttributes = new JMenuItem("Delete attributes...");
    menuItemDeleteAttributes.addActionListener(this);
    popupHeader.add(menuItemDeleteAttributes);
    menuItemSortInstances = new JMenuItem("Sort data (ascending)");
    menuItemSortInstances.addActionListener(this);
    popupHeader.add(menuItemSortInstances);
    popupHeader.addSeparator();
    menuItemOptimalColWidth = new JMenuItem("Optimal column width (current)");
    menuItemOptimalColWidth.addActionListener(this);
    popupHeader.add(menuItemOptimalColWidth);
    menuItemOptimalColWidths = new JMenuItem("Optimal column width (all)");
    menuItemOptimalColWidths.addActionListener(this);
    popupHeader.add(menuItemOptimalColWidths);
    
    // row popup
    popupRows = new JPopupMenu();
    popupRows.addMouseListener(this);
    menuItemUndo = new JMenuItem("Undo");
    menuItemUndo.addActionListener(this);
    popupRows.add(menuItemUndo);
    popupRows.addSeparator();
    menuItemCopy = new JMenuItem("Copy");
    menuItemCopy.addActionListener(this);
    popupRows.add(menuItemCopy);
    popupRows.addSeparator();
    menuItemSearch = new JMenuItem("Search...");
    menuItemSearch.addActionListener(this);
    popupRows.add(menuItemSearch);
    menuItemClearSearch = new JMenuItem("Clear search");
    menuItemClearSearch.addActionListener(this);
    popupRows.add(menuItemClearSearch);
    popupRows.addSeparator();
    menuItemDeleteSelectedInstance = new JMenuItem("Delete selected instance");
    menuItemDeleteSelectedInstance.addActionListener(this);
    popupRows.add(menuItemDeleteSelectedInstance);
    menuItemDeleteAllSelectedInstances = new JMenuItem("Delete ALL selected instances");
    menuItemDeleteAllSelectedInstances.addActionListener(this);
    popupRows.add(menuItemDeleteAllSelectedInstances);
    
    // table
    tableArff = new ArffTable();
    tableArff.setToolTipText("Right click (or left+alt) for context menu");
    tableArff.getTableHeader().addMouseListener(this);
    tableArff.getTableHeader().setToolTipText("<html><b>Sort view:</b> left click = ascending / Shift + left click = descending<br><b>Menu:</b> right click (or left+alt)</html>");
    tableArff.getTableHeader().setDefaultRenderer(new ArffTableCellRenderer());
    tableArff.addChangeListener(this);
    tableArff.addMouseListener(this);
    pane = new JScrollPane(tableArff);
    add(pane, BorderLayout.CENTER);
    
    // relation name
    labelName   = new JLabel();
    add(labelName, BorderLayout.NORTH);
  }
  
  /**
   * sets the enabled/disabled state of the menu items 
   */
  private void setMenu() {
    boolean            isNumeric;
    boolean            hasColumns;
    boolean            hasRows;
    boolean            attSelected;
    ArffSortedTableModel    model;
    
    model       = (ArffSortedTableModel) tableArff.getModel();
    hasColumns  = (model.getInstances().numAttributes() > 0);
    hasRows     = (model.getInstances().numInstances() > 0);
    attSelected = hasColumns && (currentCol > 0);
    isNumeric   = attSelected && (model.getAttributeAt(currentCol).isNumeric());
    
    menuItemUndo.setEnabled(canUndo());
    menuItemCopy.setEnabled(true);
    menuItemSearch.setEnabled(true);
    menuItemClearSearch.setEnabled(true);
    menuItemMean.setEnabled(isNumeric);
    menuItemSetAllValues.setEnabled(attSelected);
    menuItemSetMissingValues.setEnabled(attSelected);
    menuItemReplaceValues.setEnabled(attSelected);
    menuItemRenameAttribute.setEnabled(attSelected);
    menuItemDeleteAttribute.setEnabled(attSelected);
    menuItemDeleteAttributes.setEnabled(attSelected);
    menuItemSortInstances.setEnabled(hasRows && attSelected);
    menuItemDeleteSelectedInstance.setEnabled(hasRows && tableArff.getSelectedRow() > -1);
    menuItemDeleteAllSelectedInstances.setEnabled(hasRows && (tableArff.getSelectedRows().length > 0));
  }
  
  /**
   * returns the table component
   * 
   * @return 		the table
   */
  public ArffTable getTable() {
    return tableArff;
  }
  
  /**
   * returns the title for the Tab, i.e. the filename
   * 
   * @return 		the title for the tab
   */
  public String getTitle() {
    return title;
  }
  
  /**
   * returns the filename
   * 
   * @return		the filename
   */
  public String getFilename() {
    return filename;
  }
  
  /**
   * sets the filename
   * 
   * @param filename	the new filename
   */
  public void setFilename(String filename) {
    this.filename = filename;
    createTitle();
  }
  
  /**
   * returns the instances of the panel, if none then NULL
   * 
   * @return		the instances of the panel
   */
  public Instances getInstances() {
    Instances            result;
    
    result = null;
    
    if (tableArff.getModel() != null)
      result = ((ArffSortedTableModel) tableArff.getModel()).getInstances();
    
    return result;
  }
  
  /**
   * displays the given instances, i.e. creates a tab with the title 
   * TAB_INSTANCES. if one already exists it closes it.<br>
   * if a different instances object is used here, don't forget to clear
   * the undo-history by calling <code>clearUndo()</code>
   * 
   * @param data	the instances to display
   * @see               #TAB_INSTANCES
   * @see               #clearUndo()
   */
  public void setInstances(Instances data) {
    ArffSortedTableModel         model;
    
    this.filename = TAB_INSTANCES;
    
    createTitle();
    if (data == null)   
      model = null;
    else
      model = new ArffSortedTableModel(data);
    
    tableArff.setModel(model);
    clearUndo();
    setChanged(false);
    createName();
  }
  
  /**
   * returns a list with the attributes
   * 
   * @return		a list of the attributes
   */
  public Vector getAttributes() {
    Vector               result;
    int                  i;
    
    result = new Vector();
    for (i = 0; i < getInstances().numAttributes(); i++)
      result.add(getInstances().attribute(i).name());
    Collections.sort(result);
    
    return result;
  }
  
  /**
   * can only reset the changed state to FALSE
   * 
   * @param changed		if false, resets the changed state
   */
  public void setChanged(boolean changed) {
    if (!changed)
    {
      this.changed = changed;
      createTitle();
    }
  }
  
  /**
   * returns whether the content of the panel was changed
   * 
   * @return		true if the content was changed
   */
  public boolean isChanged() {
    return changed;
  }

  /**
   * returns whether undo support is enabled
   * 
   * @return 		true if undo is enabled
   */
  public boolean isUndoEnabled() {
    return ((ArffSortedTableModel) tableArff.getModel()).isUndoEnabled();
  }
  
  /**
   * sets whether undo support is enabled
   * 
   * @param enabled		whether to enable/disable undo support
   */
  public void setUndoEnabled(boolean enabled) {
    ((ArffSortedTableModel) tableArff.getModel()).setUndoEnabled(enabled);
  }
  
  /**
   * removes the undo history
   */
  public void clearUndo() {
    ((ArffSortedTableModel) tableArff.getModel()).clearUndo();
  }
  
  /**
   * returns whether an undo is possible 
   * 
   * @return		true if undo is possible
   */
  public boolean canUndo() {
    return ((ArffSortedTableModel) tableArff.getModel()).canUndo();
  }
  
  /**
   * performs an undo action
   */
  public void undo() {
    if (canUndo()) {
      ((ArffSortedTableModel) tableArff.getModel()).undo();
      
      // notify about update
      notifyListener();
    }
  }
  
  /**
   * adds the current state of the instances to the undolist 
   */
  public void addUndoPoint() {
    ((ArffSortedTableModel) tableArff.getModel()).addUndoPoint();
        
    // update menu
    setMenu();
  }
  
  /**
   * sets the title (i.e. filename)
   */
  private void createTitle() {
    File              file;
    
    if (filename.equals("")) {
      title = "-none-";
    }
    else if (filename.equals(TAB_INSTANCES)) {
      title = TAB_INSTANCES;
    }
    else {
      try {
        file  = new File(filename);
        title = file.getName();
      }
      catch (Exception e) {
        title = "-none-";
      }
    }
    
    if (isChanged())
      title += " *";
  }
  
  /**
   * sets the relation name
   */
  private void createName() {
    ArffSortedTableModel         model;
    
    model = (ArffSortedTableModel) tableArff.getModel();
    if (model != null)
      labelName.setText("Relation: " + model.getInstances().relationName());
    else
      labelName.setText("");
  }
  
  /**
   * loads the specified file into the table
   * 
   * @param filename		the file to load
   */
  private void loadFile(String filename) {
    ArffSortedTableModel         model;
    
    this.filename = filename;
    
    createTitle();
    
    if (filename.equals(""))   
      model = null;
    else
      model = new ArffSortedTableModel(filename);
    
    tableArff.setModel(model);
    setChanged(false);
    createName();
  }
  
  /**
   * calculates the mean of the given numeric column
   */
  private void calcMean() {
    ArffSortedTableModel   model;
    int               i;
    double            mean;
    
    // no column selected?
    if (currentCol == -1)
      return;
    
    model = (ArffSortedTableModel) tableArff.getModel();
    
    // not numeric?
    if (!model.getAttributeAt(currentCol).isNumeric())
      return;
    
    mean = 0;
    for (i = 0; i < model.getRowCount(); i++)
      mean += model.getInstances().instance(i).value(currentCol - 1);
    mean = mean / model.getRowCount();
    
    // show result
    ComponentHelper.showMessageBox(
        getParent(), 
        "Mean for attribute...",
        "Mean for attribute '" 
        + tableArff.getPlainColumnName(currentCol) 
        + "':\n\t" + Utils.doubleToString(mean, 3),
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE);
  }
  
  /**
   * sets the specified values in a column to a new value
   * 
   * @param o		the menu item
   */
  private void setValues(Object o) {
    String                     msg;
    String                     title;
    String                     value;
    String                     valueNew;
    int                        i;
    ArffSortedTableModel      model;
    
    value    = "";
    valueNew = "";
    
    if (o == menuItemSetMissingValues) {
      title = "Replace missing values..."; 
      msg   = "New value for MISSING values";
    }
    else if (o == menuItemSetAllValues) {
      title = "Set all values..."; 
      msg   = "New value for ALL values";
    }
    else if (o == menuItemReplaceValues) {
      title = "Replace values..."; 
      msg   = "Old value";
    }
    else
      return;
    
    value = ComponentHelper.showInputBox(tableArff.getParent(), title, msg, lastSearch);
    
    // cancelled?
    if (value == null)
      return;

    lastSearch = value;
    
    // replacement
    if (o == menuItemReplaceValues) {
      valueNew = ComponentHelper.showInputBox(tableArff.getParent(), title, "New value", lastReplace);
      if (valueNew == null)
        return;
      lastReplace = valueNew;
    }
    
    model = (ArffSortedTableModel) tableArff.getModel();
    model.setNotificationEnabled(false);

    // undo
    addUndoPoint();
    model.setUndoEnabled(false);
    
    // set value
    for (i = 0; i < tableArff.getRowCount(); i++) {
      if (o == menuItemSetAllValues)
        model.setValueAt(value, i, currentCol);
      else
        if ( (o == menuItemSetMissingValues) 
            && model.isMissingAt(i, currentCol) )
          model.setValueAt(value, i, currentCol);
        else if ( (o == menuItemReplaceValues) 
            && model.getValueAt(i, currentCol).toString().equals(value) )
          model.setValueAt(valueNew, i, currentCol);
    }
    model.setUndoEnabled(true);
    model.setNotificationEnabled(true);
    model.notifyListener(new TableModelEvent(model, 0, model.getRowCount(), currentCol, TableModelEvent.UPDATE));
    
    // refresh
    tableArff.repaint();
  }
  
  /**
   * deletes the currently selected attribute
   */
  public void deleteAttribute() {
    ArffSortedTableModel   model;
    
    // no column selected?
    if (currentCol == -1)
      return;
    
    model = (ArffSortedTableModel) tableArff.getModel();

    // really an attribute column?
    if (model.getAttributeAt(currentCol) == null)
      return;
    
    // really?
    if (ComponentHelper.showMessageBox(
        getParent(), 
        "Confirm...",
        "Do you really want to delete the attribute '" 
        + model.getAttributeAt(currentCol).name() + "'?",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION)
      return;
    
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    model.deleteAttributeAt(currentCol);
    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }
  
  /**
   * deletes the chosen attributes
   */
  public void deleteAttributes() {
    ListSelectorDialog    dialog;
    ArffSortedTableModel       model;
    Object[]              atts;
    int[]                 indices;
    int                   i;
    JList                 list;
    int                   result;
    
    list   = new JList(getAttributes());
    dialog = new ListSelectorDialog(null, list);
    result = dialog.showDialog();
    
    if (result != ListSelectorDialog.APPROVE_OPTION)
      return;
    
    atts = list.getSelectedValues();
    
    // really?
    if (ComponentHelper.showMessageBox(
        getParent(), 
        "Confirm...",
        "Do you really want to delete these " 
        + atts.length + " attributes?",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION)
      return;
    
    model   = (ArffSortedTableModel) tableArff.getModel();
    indices = new int[atts.length];
    for (i = 0; i < atts.length; i++)
      indices[i] = model.getAttributeColumn(atts[i].toString());
    
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    model.deleteAttributes(indices);
    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }
  
  /**
   * sets the current attribute as class attribute, i.e. it moves it to the end
   * of the attributes
   */
  public void attributeAsClass() {
    ArffSortedTableModel   model;
    
    // no column selected?
    if (currentCol == -1)
      return;
    
    model   = (ArffSortedTableModel) tableArff.getModel();

    // really an attribute column?
    if (model.getAttributeAt(currentCol) == null)
      return;
    
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    model.attributeAsClassAt(currentCol);
    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }
  
  /**
   * renames the current attribute
   */
  public void renameAttribute() {
    ArffSortedTableModel   model;
    String            newName;
    
    // no column selected?
    if (currentCol == -1)
      return;
    
    model   = (ArffSortedTableModel) tableArff.getModel();

    // really an attribute column?
    if (model.getAttributeAt(currentCol) == null)
      return;
    
    newName = ComponentHelper.showInputBox(getParent(), "Rename attribute...", "Enter new Attribute name", model.getAttributeAt(currentCol).name());
    if (newName == null)
      return;
    
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    model.renameAttributeAt(currentCol, newName);
    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }
  
  /**
   * deletes the currently selected instance
   */
  public void deleteInstance() {
    int               index;
    
    index = tableArff.getSelectedRow();
    if (index == -1)
      return;
    
    ((ArffSortedTableModel) tableArff.getModel()).deleteInstanceAt(index);
  }
  
  /**
   * deletes all the currently selected instances
   */
  public void deleteInstances() {
    int[]             indices;
    
    if (tableArff.getSelectedRow() == -1)
      return;
    
    indices = tableArff.getSelectedRows();
    ((ArffSortedTableModel) tableArff.getModel()).deleteInstances(indices);
  }
  
  /**
   * sorts the instances via the currently selected column
   */
  public void sortInstances() {
    if (currentCol == -1)
      return;
    
    ((ArffSortedTableModel) tableArff.getModel()).sortInstances(currentCol);
  }
  
  /**
   * copies the content of the selection to the clipboard
   */
  public void copyContent() {
    StringSelection      selection;
    Clipboard            clipboard;
    
    selection = getTable().getStringSelection();
    if (selection == null)
      return;
    
    clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(selection, selection);
  }
  
  /**
   * searches for a string in the cells
   */
  public void search() {
    String              searchString;
    
    // display dialog
    searchString = ComponentHelper.showInputBox(getParent(), "Search...", "Enter the string to search for", lastSearch);
    if (searchString != null)
      lastSearch = searchString;
    
    getTable().setSearchString(searchString);
  }
  
  /**
   * clears the search, i.e. resets the found cells
   */
  public void clearSearch() {
    getTable().setSearchString("");
  }
  
  /**
   * calculates the optimal column width for the current column
   */
  public void setOptimalColWidth() {
    // no column selected?
    if (currentCol == -1)
      return;

    JTableHelper.setOptimalColumnWidth(getTable(), currentCol);
  }
  
  /**
   * calculates the optimal column widths for all columns
   */
  public void setOptimalColWidths() {
    JTableHelper.setOptimalColumnWidth(getTable());
  }
  
  /**
   * invoked when an action occurs
   * 
   * @param e		the action event
   */
  public void actionPerformed(ActionEvent e) {
    Object          o;
    
    o = e.getSource();
    
    if (o == menuItemMean)
      calcMean();
    else if (o == menuItemSetAllValues)
      setValues(menuItemSetAllValues);
    else if (o == menuItemSetMissingValues)
      setValues(menuItemSetMissingValues);
    else if (o == menuItemReplaceValues)
      setValues(menuItemReplaceValues);
    else if (o == menuItemRenameAttribute)
      renameAttribute();
    else if (o == menuItemAttributeAsClass)
      attributeAsClass();
    else if (o == menuItemDeleteAttribute)
      deleteAttribute();
    else if (o == menuItemDeleteAttributes)
      deleteAttributes();
    else if (o == menuItemDeleteSelectedInstance)
      deleteInstance();
    else if (o == menuItemDeleteAllSelectedInstances)
      deleteInstances();
    else if (o == menuItemSortInstances)
      sortInstances();
    else if (o == menuItemSearch)
      search();
    else if (o == menuItemClearSearch)
      clearSearch();
    else if (o == menuItemUndo)
      undo();
    else if (o == menuItemCopy)
      copyContent();
    else if (o == menuItemOptimalColWidth)
      setOptimalColWidth();
    else if (o == menuItemOptimalColWidths)
      setOptimalColWidths();
  }
  
  /**
   * Invoked when a mouse button has been pressed and released on a component
   * 
   * @param e		the mouse event
   */
  public void mouseClicked(MouseEvent e) {
    int		col;
    boolean	popup;
    
    col   = tableArff.columnAtPoint(e.getPoint());
    popup =    ((e.getButton() == MouseEvent.BUTTON3) && (e.getClickCount() == 1))
            || ((e.getButton() == MouseEvent.BUTTON1) && (e.getClickCount() == 1) && e.isAltDown() && !e.isControlDown() && !e.isShiftDown());
    
    if (e.getSource() == tableArff.getTableHeader()) {
      currentCol = col;
      
      // Popup-Menu
      if (popup) {
        e.consume();
        setMenu();
        popupHeader.show(e.getComponent(), e.getX(), e.getY());
      }
    }
    else if (e.getSource() == tableArff) {
      // Popup-Menu
      if (popup) {
        e.consume();
        setMenu();
        popupRows.show(e.getComponent(), e.getX(), e.getY());
      }
    }
    
    // highlihgt column
    if (    (e.getButton() == MouseEvent.BUTTON1)  
         && (e.getClickCount() == 1) 
         && (!e.isAltDown())
         && (col > -1) ) {
      tableArff.setSelectedColumn(col);
    }
  }
  
  /**
   * Invoked when the mouse enters a component.
   * 
   * @param e		the mouse event
   */
  public void mouseEntered(MouseEvent e) {
  }
  
  /**
   * Invoked when the mouse exits a component
   * 
   * @param e		the mouse event
   */
  public void mouseExited(MouseEvent e) {
  }
  
  /**
   * Invoked when a mouse button has been pressed on a component
   * 
   * @param e		the mouse event
   */
  public void mousePressed(MouseEvent e) {
  }
  
  /**
   * Invoked when a mouse button has been released on a component.
   * 
   * @param e		the mouse event
   */
  public void mouseReleased(MouseEvent e) {
  }
  
  /**
   * Invoked when the target of the listener has changed its state.
   * 
   * @param e		the change event
   */
  public void stateChanged(ChangeEvent e) {
    changed = true;
    createTitle();
    notifyListener();
  }
  
  /**
   * notfies all listener of the change
   */
  public void notifyListener() {
    Iterator                iter;
    
    iter = changeListeners.iterator();
    while (iter.hasNext())
      ((ChangeListener) iter.next()).stateChanged(new ChangeEvent(this));
  }
  
  /**
   * Adds a ChangeListener to the panel
   * 
   * @param l		the listener to add
   */
  public void addChangeListener(ChangeListener l) {
    changeListeners.add(l);
  }
  
  /**
   * Removes a ChangeListener from the panel
   * 
   * @param l		the listener to remove
   */
  public void removeChangeListener(ChangeListener l) {
    changeListeners.remove(l);
  }
}
