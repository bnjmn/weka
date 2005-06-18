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
 * ArffViewer.java
 * Copyright (C) 2005 FracPete
 *
 */

package weka.gui.arffviewer;

import weka.gui.ComponentHelper;
import weka.gui.ExtensionFileFilter;
import weka.gui.JTableHelper;
import weka.gui.LookAndFeel;
import weka.core.Instances;
import weka.core.Memory;
import weka.core.converters.AbstractSaver;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVSaver;
import weka.gui.ListSelectorDialog;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A little tool for viewing ARFF files.
 *
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.1.2.3 $ 
 */

public class ArffViewer 
extends     JFrame
implements  ActionListener, ChangeListener, WindowListener
{
  /** the default for width */
  private final static int    DEFAULT_WIDTH     = -1;
  /** the default for height */
  private final static int    DEFAULT_HEIGHT    = -1;
  /** the default for left */
  private final static int    DEFAULT_LEFT      = -1;
  /** the default for top */
  private final static int    DEFAULT_TOP       = -1;
  /** default width */
  private final static int    WIDTH             = 800;
  /** default height */
  private final static int    HEIGHT            = 600;
  
  /** for monitoring the Memory consumption */
  private static Memory m_Memory = new Memory(true);
  
  private JTabbedPane           tabbedPane;
  private JMenuBar              menuBar;
  private JMenu                 menuFile;
  private JMenuItem             menuFileOpen;
  private JMenuItem             menuFileSave;
  private JMenuItem             menuFileSaveAs;
  private JMenuItem             menuFileClose;
  private JMenuItem             menuFileProperties;
  private JMenuItem             menuFileExit;
  private JMenu                 menuEdit;
  private JMenuItem             menuEditUndo;
  private JMenuItem             menuEditCopy;
  private JMenuItem             menuEditSearch;
  private JMenuItem             menuEditClearSearch;
  private JMenuItem             menuEditDeleteAttribute;
  private JMenuItem             menuEditDeleteAttributes;
  private JMenuItem             menuEditDeleteInstance;
  private JMenuItem             menuEditDeleteInstances;
  private JMenuItem             menuEditSortInstances;
  private JMenu                 menuView;
  private JMenuItem             menuViewAttributes;
  private JMenuItem             menuViewValues;
  
  private FileChooser           fileChooser;
  private ExtensionFileFilter   arffFilter;
  private ExtensionFileFilter   csvFilter;
  private String                frameTitle;
  private boolean               confirmExit;
  private int                   width;
  private int                   height;
  private int                   top;
  private int                   left;
  private boolean               exitOnClose;
  
  /**
   * initializes the object
   */
  public ArffViewer() {
    super("ARFF-Viewer");
    frameTitle = "ARFF-Viewer"; 
    createFrame();
  }
  
  /**
   * creates all the components in the frame
   */
  protected void createFrame() {
    // basic setup
    setIconImage(ComponentHelper.getImage("weka_icon.gif"));
    setSize(WIDTH, HEIGHT);
    setCenteredLocation();
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    // remove the listener - otherwise we get the strange behavior that one
    // frame receives a window-event for every single open frame!
    removeWindowListener(this);
    // add listener anew
    addWindowListener(this);
    
    setConfirmExit(false);
    getContentPane().setLayout(new BorderLayout());
    
    // file dialog
    arffFilter              = new ExtensionFileFilter("arff", "ARFF-Files");
    csvFilter               = new ExtensionFileFilter("csv", "CSV-File");
    fileChooser             = new FileChooser();
    fileChooser.setMultiSelectionEnabled(true);
    fileChooser.addChoosableFileFilter(arffFilter);
    fileChooser.addChoosableFileFilter(csvFilter);
    fileChooser.setFileFilter(arffFilter);
    
    // menu
    menuBar        = new JMenuBar();
    setJMenuBar(menuBar);
    menuFile       = new JMenu("File");
    menuFileOpen   = new JMenuItem("Open...", ComponentHelper.getImageIcon("open.gif"));
    menuFileOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_MASK));
    menuFileOpen.addActionListener(this);
    menuFileSave   = new JMenuItem("Save", ComponentHelper.getImageIcon("save.gif"));
    menuFileSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_MASK));
    menuFileSave.addActionListener(this);
    menuFileSaveAs = new JMenuItem("Save as...", ComponentHelper.getImageIcon("empty.gif"));
    menuFileSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_MASK + KeyEvent.SHIFT_MASK));
    menuFileSaveAs.addActionListener(this);
    menuFileClose  = new JMenuItem("Close", ComponentHelper.getImageIcon("empty.gif"));
    menuFileClose.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_MASK));
    menuFileClose.addActionListener(this);
    menuFileProperties  = new JMenuItem("Properties", ComponentHelper.getImageIcon("empty.gif"));
    menuFileProperties.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_MASK));
    menuFileProperties.addActionListener(this);
    menuFileExit   = new JMenuItem("Exit", ComponentHelper.getImageIcon("forward.gif"));
    menuFileExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.ALT_MASK));
    menuFileExit.addActionListener(this);
    menuFile.add(menuFileOpen);
    menuFile.add(menuFileSave);
    menuFile.add(menuFileSaveAs);
    menuFile.add(menuFileClose);
    menuFile.addSeparator();
    menuFile.add(menuFileProperties);
    menuFile.addSeparator();
    menuFile.add(menuFileExit);
    menuBar.add(menuFile);
    
    menuEdit       = new JMenu("Edit");
    menuEditUndo   = new JMenuItem("Undo", ComponentHelper.getImageIcon("undo.gif"));
    menuEditUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_MASK));
    menuEditUndo.addActionListener(this);
    menuEditCopy   = new JMenuItem("Copy", ComponentHelper.getImageIcon("copy.gif"));
    menuEditCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, KeyEvent.CTRL_MASK));
    menuEditCopy.addActionListener(this);
    menuEditSearch   = new JMenuItem("Search...", ComponentHelper.getImageIcon("find.gif"));
    menuEditSearch.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_MASK));
    menuEditSearch.addActionListener(this);
    menuEditClearSearch   = new JMenuItem("Clear search", ComponentHelper.getImageIcon("empty.gif"));
    menuEditClearSearch.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_MASK + KeyEvent.SHIFT_MASK));
    menuEditClearSearch.addActionListener(this);
    menuEditDeleteAttribute = new JMenuItem("Delete attribute", ComponentHelper.getImageIcon("empty.gif"));
    menuEditDeleteAttribute.addActionListener(this);
    menuEditDeleteAttributes = new JMenuItem("Delete attributes", ComponentHelper.getImageIcon("empty.gif"));
    menuEditDeleteAttributes.addActionListener(this);
    menuEditDeleteInstance = new JMenuItem("Delete instance", ComponentHelper.getImageIcon("empty.gif"));
    menuEditDeleteInstance.addActionListener(this);
    menuEditDeleteInstances = new JMenuItem("Delete instances", ComponentHelper.getImageIcon("empty.gif"));
    menuEditDeleteInstances.addActionListener(this);
    menuEditSortInstances = new JMenuItem("Sort data (ascending)", ComponentHelper.getImageIcon("sort.gif"));
    menuEditSortInstances.addActionListener(this);
    menuEdit.add(menuEditUndo);
    menuEdit.addSeparator();
    menuEdit.add(menuEditCopy);
    menuEdit.addSeparator();
    menuEdit.add(menuEditSearch);
    menuEdit.add(menuEditClearSearch);
    menuEdit.addSeparator();
    menuEdit.add(menuEditDeleteAttribute);
    menuEdit.add(menuEditDeleteAttributes);
    menuEdit.addSeparator();
    menuEdit.add(menuEditDeleteInstance);
    menuEdit.add(menuEditDeleteInstances);
    menuEdit.add(menuEditSortInstances);
    menuBar.add(menuEdit);
    
    menuView       = new JMenu("View");
    menuViewAttributes   = new JMenuItem("Attributes...", ComponentHelper.getImageIcon("objects.gif"));
    menuViewAttributes.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_MASK + KeyEvent.SHIFT_MASK));
    menuViewAttributes.addActionListener(this);
    menuViewValues   = new JMenuItem("Values...", ComponentHelper.getImageIcon("properties.gif"));
    menuViewValues.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_MASK + KeyEvent.SHIFT_MASK));
    menuViewValues.addActionListener(this);
    menuView.add(menuViewAttributes);
    menuView.add(menuViewValues);
    menuBar.add(menuView);
    
    // tabbed pane
    tabbedPane  = new JTabbedPane();
    tabbedPane.addChangeListener(this);
    getContentPane().add(tabbedPane, BorderLayout.CENTER);
    
    setMenu();
  }
  
  /**
   * resizes the frame according to the parameters centers it
   */
  protected void resizeFrame() {
    // the position
    if (width  != DEFAULT_WIDTH)  setSize(width, getSize().height);
    if (height != DEFAULT_HEIGHT) setSize(getSize().width, height);
    
    // at first center it
    setCenteredLocation();
    if (top  != DEFAULT_TOP) setLocation(getLocation().x, top);
    if (left != DEFAULT_LEFT) setLocation(left, getLocation().y);
  }
  
  /**
   * returns the left coordinate if the frame would be centered
   */
  protected int getCenteredLeft() {
    int            width;
    int            x;
    
    width  = getBounds().width;
    x      = (getGraphicsConfiguration().getBounds().width  - width)  / 2;
    
    if (x < 0) x = 0;
    
    return x;
  }
  
  /**
   * returns the top coordinate if the frame would be centered
   */
  protected int getCenteredTop() {
    int            height;
    int            y;
    
    height = getBounds().height;
    y      = (getGraphicsConfiguration().getBounds().height - height) / 2;
    
    if (y < 0) y = 0;
    
    return y;
  }
  
  /**
   * positions the window at the center of the screen
   */
  public void setCenteredLocation() { 
    setLocation(getCenteredLeft(), getCenteredTop());
  }
  
  /**
   * whether to present a MessageBox on Exit or not
   * @param confirm           whether a MessageBox pops up or not to confirm
   *                          exit
   */
  public void setConfirmExit(boolean confirm) {
    confirmExit = confirm;
  }
  
  /**
   * returns the setting of whether to display a confirm messagebox or not
   * on exit
   * @return                  whether a messagebox is displayed or not
   */
  public boolean getConfirmExit() {
    return confirmExit;
  }
  
  /**
   * whether to do a System.exit(0) on close
   */
  public void setExitOnClose(boolean value) {
    exitOnClose = value;
  }

  /**
   * returns TRUE if a System.exit(0) is done on a close
   */
  public boolean getExitOnClose() {
    return exitOnClose;
  }
  
  /**
   * validates and repaints the frame
   */
  public void refresh() {
    validate();
    repaint();
  }
  
  /**
   * sets the title (incl. filename)
   */
  protected void setFrameTitle() {
    if (getCurrentFilename().equals(""))
      setTitle(frameTitle);
    else
      setTitle(frameTitle + " - " + getCurrentFilename());
  }
  
  /**
   * sets the enabled/disabled state of the menu 
   */
  private void setMenu() {
    boolean       fileOpen;
    boolean       isChanged;
    boolean       canUndo;
    
    fileOpen  = (getCurrentPanel() != null);
    isChanged = fileOpen && (getCurrentPanel().isChanged());
    canUndo   = fileOpen && (getCurrentPanel().canUndo());
    
    // File
    menuFileOpen.setEnabled(true);
    menuFileSave.setEnabled(isChanged);
    menuFileSaveAs.setEnabled(fileOpen);
    menuFileClose.setEnabled(fileOpen);
    menuFileProperties.setEnabled(fileOpen);
    menuFileExit.setEnabled(true);
    // Edit
    menuEditUndo.setEnabled(canUndo);
    menuEditCopy.setEnabled(fileOpen);
    menuEditSearch.setEnabled(fileOpen);
    menuEditClearSearch.setEnabled(fileOpen);
    menuEditDeleteAttribute.setEnabled(fileOpen);
    menuEditDeleteAttributes.setEnabled(fileOpen);
    menuEditDeleteInstance.setEnabled(fileOpen);
    menuEditDeleteInstances.setEnabled(fileOpen);
    menuEditSortInstances.setEnabled(fileOpen);
    // View
    menuViewAttributes.setEnabled(fileOpen);
    menuViewValues.setEnabled(fileOpen);
  }
  
  /**
   * sets the title of the tab that contains the given component
   */
  private void setTabTitle(JComponent component) {
    int            index;
    
    if (!(component instanceof ArffPanel))
      return;
    
    index = tabbedPane.indexOfComponent(component);
    if (index == -1)
      return;
    
    tabbedPane.setTitleAt(index, ((ArffPanel) component).getTitle());
    setFrameTitle();
  }
  
  /**
   * returns the number of panels currently open
   */
  public int getPanelCount() {
    return tabbedPane.getTabCount();
  }
  
  /**
   * returns the specified panel, <code>null</code> if index is out of bounds  
   */
  public ArffPanel getPanel(int index) {
    if ((index >= 0) && (index < getPanelCount()))
      return (ArffPanel) tabbedPane.getComponentAt(index);
    else
      return null;
  }
  
  /**
   * returns the currently selected tab index
   */
  public int getCurrentIndex() {
    return tabbedPane.getSelectedIndex();
  }
  
  /**
   * returns the currently selected panel
   */
  public ArffPanel getCurrentPanel() {
    return getPanel(getCurrentIndex());
  }
  
  /**
   * checks whether a panel is currently selected
   */
  public boolean isPanelSelected() {
    return (getCurrentPanel() != null);
  }
  
  /**
   * returns the filename of the specified panel 
   */
  public String getFilename(int index) {
    String            result;
    ArffPanel         panel;
    
    result = "";
    panel  = getPanel(index);
    
    if (panel != null)
      result = panel.getFilename();
    
    return result;
  }
  
  /**
   * returns the filename of the current tab
   */
  public String getCurrentFilename() {
    return getFilename(getCurrentIndex());
  }
  
  /**
   * sets the filename of the specified panel
   */
  public void setFilename(int index, String filename) {
    ArffPanel         panel;
    
    panel = getPanel(index);
    
    if (panel != null) {
      panel.setFilename(filename);
      setTabTitle(panel);
    }
  }
  
  /**
   * sets the filename of the current tab
   */
  public void setCurrentFilename(String filename) {
    setFilename(getCurrentIndex(), filename);
  }
  
  /**
   * if the file is changed it pops up a dialog whether to change the
   * settings. if the project wasn't changed or saved it returns TRUE
   */
  private boolean saveChanges() {
    return saveChanges(true);
  }
  
  /**
   * if the file is changed it pops up a dialog whether to change the
   * settings. if the project wasn't changed or saved it returns TRUE
   * @param showCancel           whether we have YES/NO/CANCEL or only YES/NO
   */
  private boolean saveChanges(boolean showCancel) {
    int            button;
    boolean        result;
    
    if (!isPanelSelected())
      return true;
    
    result = !getCurrentPanel().isChanged();
    
    if (getCurrentPanel().isChanged()) {
      try {
        if (showCancel)
          button = ComponentHelper.showMessageBox(
              this,
              "Changed",
              "The file is not saved - Do you want to save it?",
              JOptionPane.YES_NO_CANCEL_OPTION,
              JOptionPane.QUESTION_MESSAGE );
        else
          button = ComponentHelper.showMessageBox(
              this,
              "Changed",
              "The file is not saved - Do you want to save it?",
              JOptionPane.YES_NO_OPTION,
              JOptionPane.QUESTION_MESSAGE );
      }
      catch (Exception e) {
        button = JOptionPane.CANCEL_OPTION; 
      }
      
      switch (button) {
        case JOptionPane.YES_OPTION:
          saveFile();
          result = !getCurrentPanel().isChanged();
          break;
        case JOptionPane.NO_OPTION:
          result = true;
          break;
        case JOptionPane.CANCEL_OPTION: 
          result = false;
          break;
      }
    }
    
    return result;
  }

  /**
   * loads the specified file
   */
  private void loadFile(String filename) {
    ArffPanel         panel;

    panel    = new ArffPanel(filename);
    panel.addChangeListener(this);
    tabbedPane.addTab(panel.getTitle(), panel);
    tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
  }
   
  /**
   * loads the specified file into the table
   */
  private void loadFile() {
    int               retVal;
    int               i;
    String            filename;
    
    retVal = fileChooser.showOpenDialog(this);
    if (retVal != FileChooser.APPROVE_OPTION)
      return;
    
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    
    for (i = 0; i< fileChooser.getSelectedFiles().length; i++) {
      filename = fileChooser.getSelectedFiles()[i].getAbsolutePath();
      loadFile(filename);
    }
    
    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }
  
  /**
   * saves the current data into a file
   */
  private void saveFile() {
    ArffPanel           panel;
    String              filename;
    AbstractSaver       saver;
    
    // no panel? -> exit
    panel = getCurrentPanel();
    if (panel == null)
      return;
    
    filename = panel.getFilename();
    if (filename.equals(ArffPanel.TAB_INSTANCES)) {
      saveFileAs();
    }
    else {
      if (fileChooser.getFileFilter() == arffFilter)
        saver = new ArffSaver();
      else if (fileChooser.getFileFilter() == csvFilter)
        saver = new CSVSaver();
      else
        saver = null;
      
      if (saver != null) {
        try {
          saver.setRetrieval(AbstractSaver.BATCH);
          saver.setInstances(panel.getInstances());
          saver.setFile(fileChooser.getSelectedFile());
          saver.setDestination(fileChooser.getSelectedFile());
          saver.writeBatch();
          panel.setChanged(false);
          setCurrentFilename(filename);
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }
  
  /**
   * saves the current data into a new file
   */
  private void saveFileAs() {
    int                  retVal;
    ArffPanel            panel;
    
    // no panel? -> exit
    panel = getCurrentPanel();
    if (panel == null) {
      System.out.println("nothing selected!");
      return;
    }
    
    if (!getCurrentFilename().equals("")) {
      try {
        fileChooser.setSelectedFile(new File(getCurrentFilename()));
      }
      catch (Exception e) {
        // ignore
      }
    }
    
    retVal = fileChooser.showSaveDialog(this);
    if (retVal != FileChooser.APPROVE_OPTION)
      return;
    
    panel.setChanged(false);
    setCurrentFilename(fileChooser.getSelectedFile().getAbsolutePath());
    saveFile();
  }
  
  /**
   * closes the current tab
   */
  private void closeFile() {
    closeFile(true);
  }
  
  /**
   * closes the current tab
   * @param showCancel           whether to show an additional CANCEL button
   *                             in the "Want to save changes"-dialog
   * @see                        #saveChanges(boolean)
   */
  private void closeFile(boolean showCancel) {
    if (getCurrentIndex() == -1)
      return;
    
    if (!saveChanges(showCancel))
      return;
    
    tabbedPane.removeTabAt(getCurrentIndex());
    setFrameTitle();
    System.gc();
  }
  
  /**
   * displays some properties of the instances
   */
  private void showProperties() {
    ArffPanel             panel;
    ListSelectorDialog    dialog;
    Vector                props;
    Instances             inst;
    
    panel = getCurrentPanel();
    if (panel == null)
      return;
    
    inst  = panel.getInstances();
    if (inst == null)
      return;
    if (inst.classIndex() < 0)
      inst.setClassIndex(inst.numAttributes() - 1);
    
    // get some data
    props = new Vector();
    props.add("Filename: " + panel.getFilename());
    props.add("Relation name: " + inst.relationName());
    props.add("# of instances: " + inst.numInstances());
    props.add("# of attributes: " + inst.numAttributes());
    props.add("Class attribute: " + inst.classAttribute().name());
    props.add("# of class labels: " + inst.numClasses());
    
    dialog = new ListSelectorDialog(this, new JList(props));
    dialog.showDialog();
  }
  
  /**
   * closes the window
   */
  private void close() {
    windowClosing(null);
  }
  
  /**
   * undoes the last action 
   */
  private void undo() {
    if (!isPanelSelected())
      return;
    
    getCurrentPanel().undo();
  }
  
  /**
   * copies the content of the selection to the clipboard
   */
  private void copyContent() {
    if (!isPanelSelected())
      return;
    
    getCurrentPanel().copyContent();
  }
  
  /**
   * searches for a string in the cells
   */
  private void search() {
    if (!isPanelSelected())
      return;

    getCurrentPanel().search();
  }
  
  /**
   * clears the search, i.e. resets the found cells
   */
  private void clearSearch() {
    if (!isPanelSelected())
      return;

    getCurrentPanel().clearSearch();
  }
  
  /**
   * deletes the current selected Attribute or several chosen ones
   */
  private void deleteAttribute(boolean multiple) {
    if (!isPanelSelected())
      return;
    
    if (multiple)
      getCurrentPanel().deleteAttributes();
    else
      getCurrentPanel().deleteAttribute();
  }
  
  /**
   * deletes the current selected Instance or several chosen ones
   */
  private void deleteInstance(boolean multiple) {
    if (!isPanelSelected())
      return;
    
    if (multiple)
      getCurrentPanel().deleteInstances();
    else
      getCurrentPanel().deleteInstance();
  }
  
  /**
   * sorts the current selected attribute
   */
  private void sortInstances() {
    if (!isPanelSelected())
      return;
    
    getCurrentPanel().sortInstances();
  }
  
  /**
   * displays all the attributes, returns the selected item or NULL if canceled
   */
  private String showAttributes() {
    ArffTableSorter     model;
    ListSelectorDialog  dialog;
    int                 i;
    JList               list;
    String              name;
    int                 result;
    
    if (!isPanelSelected())
      return null;
    
    list   = new JList(getCurrentPanel().getAttributes());
    dialog = new ListSelectorDialog(this, list);
    result = dialog.showDialog();
    
    if (result == ListSelectorDialog.APPROVE_OPTION) {
      model = (ArffTableSorter) getCurrentPanel().getTable().getModel();
      name  = list.getSelectedValue().toString();
      i     = model.getAttributeColumn(name);
      JTableHelper.scrollToVisible(getCurrentPanel().getTable(), 0, i);
      getCurrentPanel().getTable().setSelectedColumn(i);
      return name;
    }
    else {
      return null;
    }
  }
  
  /**
   * displays all the distinct values for an attribute
   */
  private void showValues() {
    String                attribute;
    ArffTableSorter       model;
    ArffTable             table;
    HashSet               values;
    Vector                items;
    Iterator              iter;
    ListSelectorDialog    dialog;
    int                   i;
    int                   col;
    
    // choose attribute to retrieve values for
    attribute = showAttributes();
    if (attribute == null)
      return;
    
    table  = (ArffTable) getCurrentPanel().getTable();
    model  = (ArffTableSorter) table.getModel();
    
    // get column index
    col    = -1;
    for (i = 0; i < table.getColumnCount(); i++) {
      if (table.getPlainColumnName(i).equals(attribute)) {
        col = i;
        break;
      }
    }
    // not found?
    if (col == -1)
      return;
    
    // get values
    values = new HashSet();
    items  = new Vector();
    for (i = 0; i < model.getRowCount(); i++)
      values.add(model.getValueAt(i, col).toString());
    if (values.isEmpty())
      return;
    iter = values.iterator();
    while (iter.hasNext())
      items.add(iter.next());
    Collections.sort(items);
    
    dialog = new ListSelectorDialog(this, new JList(items));
    dialog.showDialog();
  }
  
  /**
   * invoked when an action occurs
   */
  public void actionPerformed(ActionEvent e) {
    Object          o;
    
    o = e.getSource();
    
    if (o == menuFileOpen)
      loadFile();
    else if (o == menuFileSave)
      saveFile();
    else if (o == menuFileSaveAs)
      saveFileAs();
    else if (o == menuFileClose)
      closeFile();
    else if (o == menuFileProperties)
      showProperties();
    else if (o == menuFileExit)
      close();
    else if (o == menuEditUndo)
      undo();
    else if (o == menuEditCopy)
      copyContent();
    else if (o == menuEditSearch)
      search();
    else if (o == menuEditClearSearch)
      clearSearch();
    else if (o == menuEditDeleteAttribute)
      deleteAttribute(false);
    else if (o == menuEditDeleteAttributes)
      deleteAttribute(true);
    else if (o == menuEditDeleteInstance)
      deleteInstance(false);
    else if (o == menuEditDeleteInstances)
      deleteInstance(true);
    else if (o == menuEditSortInstances)
      sortInstances();
    else if (o == menuViewAttributes)
      showAttributes();
    else if (o == menuViewValues)
      showValues();
    
    setMenu();
  }
  
  /**
   * Invoked when the target of the listener has changed its state.
   */
  public void stateChanged(ChangeEvent e) {
    setFrameTitle();
    setMenu();
    
    // did the content of panel change? -> change title of tab
    if (e.getSource() instanceof JComponent)
      setTabTitle((JComponent) e.getSource());
  }
  
  /**
   * invoked when a window is activated
   */
  public void windowActivated(WindowEvent e) {
  }
  
  /**
   * invoked when a window is closed
   */
  public void windowClosed(WindowEvent e) {
  }
  
  /**
   * invoked when a window is in the process of closing
   */
  public void windowClosing(WindowEvent e) {
    int         button;
    
    while (tabbedPane.getTabCount() > 0)
      closeFile(false);
    
    if (confirmExit) {
      button = ComponentHelper.showMessageBox(
          this,
          "Quit - " + getTitle(),
          "Do you really want to quit?",
          JOptionPane.YES_NO_OPTION,
          JOptionPane.QUESTION_MESSAGE);
      if (button == JOptionPane.YES_OPTION)
        dispose();
    } 
    else {
      dispose();
    }

    if (getExitOnClose())
      System.exit(0);
  }
  
  /**
   * invoked when a window is deactivated
   */
  public void windowDeactivated(WindowEvent e) {
  }
  
  /**
   * invoked when a window is deiconified
   */
  public void windowDeiconified(WindowEvent e) {
  }
  
  /**
   * invoked when a window is iconified
   */
  public void windowIconified(WindowEvent e) {
  }
  
  /**
   * invoked when a window is has been opened
   */
  public void windowOpened(WindowEvent e) {
  }
  
  /**
   * returns only the classname
   */
  public String toString() {
    return this.getClass().getName();
  }
  
  /** the viewer if started from command line */
  private static ArffViewer m_Viewer;

  /** whether the files were already loaded */
  private static boolean m_FilesLoaded;

  /** the command line arguments */
  private static String[] m_Args;
  
  /**
   * shows the frame and it tries to load all the arff files that were
   * provided as arguments.
   */
  public static void main(String[] args) throws Exception {
    LookAndFeel.setLookAndFeel();
    
    try {
      // uncomment to disable the memory management:
      //m_Memory.setEnabled(false);

      m_Viewer      = new ArffViewer();
      m_Viewer.setExitOnClose(true);
      m_Viewer.setVisible(true);
      m_FilesLoaded = false;
      m_Args        = args;

      Thread memMonitor = new Thread() {
        public void run() {
          while(true) {
            try {
              if ( (m_Args.length > 0) && (!m_FilesLoaded) ) {
                for (int i = 0; i < m_Args.length; i++) {
                  System.out.println("Loading " + (i+1) + "/" 
                      + m_Args.length +  ": '" + m_Args[i] + "'...");
                  m_Viewer.loadFile(m_Args[i]);
                }
                m_Viewer.tabbedPane.setSelectedIndex(0);
                System.out.println("Finished!");
                m_FilesLoaded = true;
              }

              //System.out.println("before sleeping");
              this.sleep(4000);
              
              System.gc();
              
              if (m_Memory.isOutOfMemory()) {
                // clean up
                m_Viewer.dispose();
                m_Viewer = null;
                System.gc();

                // stop threads
                m_Memory.stopThreads();

                // display error
                System.err.println("\ndisplayed message:");
                m_Memory.showOutOfMemory();
                System.err.println("\nrestarting...");

                // restart GUI
                System.gc();
                m_Viewer = new ArffViewer();
                m_Viewer.setExitOnClose(true);
                m_Viewer.setVisible(true);
                // Note: no re-loading of datasets, otherwise we could end up
                //       in an endless loop!
              }
            }
            catch(InterruptedException ex) { 
              ex.printStackTrace(); 
            }
          }
        }
      };

      memMonitor.setPriority(Thread.NORM_PRIORITY);
      memMonitor.start();    
    } 
    catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
