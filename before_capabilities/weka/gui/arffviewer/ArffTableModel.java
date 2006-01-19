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
 * ArffTableModel.java
 * Copyright (C) 2005 FracPete
 *
 */

package weka.gui.arffviewer;

import weka.core.converters.AbstractLoader;
import weka.core.converters.ArffLoader;
import weka.core.converters.CSVLoader;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Undoable;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Reorder;
import weka.gui.ComponentHelper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.table.TableModel;
import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;

/**
 * The model for the Arff-Viewer.
 *
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.4 $ 
 */

public class ArffTableModel implements TableModel, Undoable {
  // the listeners
  private HashSet         listeners;
  // the data
  private Instances       data;
  // whether notfication is enabled
  private boolean         notificationEnabled;
  // whether undo is active
  private boolean         undoEnabled;
  // whether to ignore changes, i.e. not adding to undo history
  private boolean         ignoreChanges;
  // the undo list (contains temp. filenames)
  private Vector          undoList;
  
  /**
   * performs some initialization
   */
  private ArffTableModel() {
    super();
    
    listeners           = new HashSet();
    data                = null;
    notificationEnabled = true;
    undoList            = new Vector();
    ignoreChanges       = false;
    undoEnabled         = true;
  }
  
  /**
   * initializes the object and loads the given file
   */
  public ArffTableModel(String filename) {
    this();
    
    if ( (filename != null) && (!filename.equals("")) )
      loadFile(filename);
  }
  
  /**
   * initializes the model with the given data
   */
  public ArffTableModel(Instances data) {
    this();
    
    this.data = data;
  }

  /**
   * returns whether the notification of changes is enabled
   */
  public boolean isNotificationEnabled() {
    return notificationEnabled;
  }
  
  /**
   * sets whether the notification of changes is enabled
   */
  public void setNotificationEnabled(boolean enabled) {
    notificationEnabled = enabled;
  }

  /**
   * returns whether undo support is enabled
   */
  public boolean isUndoEnabled() {
    return undoEnabled;
  }
  
  /**
   * sets whether undo support is enabled
   */
  public void setUndoEnabled(boolean enabled) {
    undoEnabled = enabled;
  }
  
  /**
   * loads the specified ARFF file
   */
  private void loadFile(String filename) {
    AbstractLoader          loader;
    
    if (filename.toLowerCase().endsWith(".arff"))
      loader = new ArffLoader();
    else if (filename.toLowerCase().endsWith(".csv"))
      loader = new CSVLoader();
    else
      loader = null;
    
    if (loader != null) {
      try {
        loader.setSource(new File(filename));
        data = loader.getDataSet();
      }
      catch (Exception e) {
        ComponentHelper.showMessageBox(
            null, 
            "Error loading file...", 
            e.toString(), 
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.ERROR_MESSAGE );
        System.out.println(e);
        data = null;
      }
    }
  }
  
  /**
   * sets the data
   */
  public void setInstances(Instances data) {
    this.data = data;
  }
  
  /**
   * returns the data
   */
  public Instances getInstances() {
    return data;
  }
  
  /**
   * returns the attribute at the given index, can be NULL if not an attribute
   * column
   */
  public Attribute getAttributeAt(int columnIndex) {
    if ( (columnIndex > 0) && (columnIndex < getColumnCount()) )
      return data.attribute(columnIndex - 1);
    else
      return null;
  }
  
  /**
   * returns the TYPE of the attribute at the given position
   */
  public int getType(int columnIndex) {
    return getType(0, columnIndex);
  }
  
  /**
   * returns the TYPE of the attribute at the given position
   */
  public int getType(int rowIndex, int columnIndex) {
    int            result;
    
    result = Attribute.STRING;
    
    if (    (rowIndex >= 0) && (rowIndex < getRowCount())
        && (columnIndex > 0) & (columnIndex < getColumnCount()) )
      result = data.instance(rowIndex).attribute(columnIndex - 1).type();
    
    return result;
  }
  
  /**
   * deletes the attribute at the given col index. notifies the listeners.
   * @param columnIndex     the index of the attribute to delete
   */
  public void deleteAttributeAt(int columnIndex) {
    deleteAttributeAt(columnIndex, true);
  }
  
  /**
   * deletes the attribute at the given col index
   * @param columnIndex     the index of the attribute to delete
   * @param notify          whether to notify the listeners
   */
  public void deleteAttributeAt(int columnIndex, boolean notify) {
    if ( (columnIndex > 0) && (columnIndex < getColumnCount()) ) {
      if (!ignoreChanges)
        addUndoPoint();
      data.deleteAttributeAt(columnIndex - 1);
      if (notify) 
        notifyListener(new TableModelEvent(this, TableModelEvent.HEADER_ROW));
    }
  }
  
  /**
   * deletes the attributes at the given indices
   */
  public void deleteAttributes(int[] columnIndices) {
    int            i;
    
    Arrays.sort(columnIndices);
    
    addUndoPoint();

    ignoreChanges = true;
    for (i = columnIndices.length - 1; i >= 0; i--)
      deleteAttributeAt(columnIndices[i], false);
    ignoreChanges = false;

    notifyListener(new TableModelEvent(this, TableModelEvent.HEADER_ROW));
  }
  
  /**
   * renames the attribute at the given col index
   */
  public void renameAttributeAt(int columnIndex, String newName) {
    if ( (columnIndex > 0) && (columnIndex < getColumnCount()) ) {
      addUndoPoint();
      data.renameAttribute(columnIndex - 1, newName);
      notifyListener(new TableModelEvent(this, TableModelEvent.HEADER_ROW));
    }
  }
  
  /**
   * sets the attribute at the given col index as the new class attribute, i.e.
   * it moves it to the end of the attributes
   */
  public void attributeAsClassAt(int columnIndex) {
    Reorder     reorder;
    String      order;
    int         i;
    
    if ( (columnIndex > 0) && (columnIndex < getColumnCount()) ) {
      addUndoPoint();
      
      try {
        // build order string (1-based!)
        order = "";
        for (i = 1; i < data.numAttributes() + 1; i++) {
          // skip new class
          if (i == columnIndex)
            continue;
          
          if (!order.equals(""))
            order += ",";
          order += Integer.toString(i);
        }
        if (!order.equals(""))
          order += ",";
        order += Integer.toString(columnIndex);
        
        // process data
        reorder = new Reorder();
        reorder.setAttributeIndices(order);
        reorder.setInputFormat(data);
        data = Filter.useFilter(data, reorder);
        
        // set class index
        data.setClassIndex(data.numAttributes() - 1);
      }
      catch (Exception e) {
        e.printStackTrace();
        undo();
      }
      
      notifyListener(new TableModelEvent(this, TableModelEvent.HEADER_ROW));
    }
  }
  
  /**
   * deletes the instance at the given index
   */
  public void deleteInstanceAt(int rowIndex) {
    deleteInstanceAt(rowIndex, true);
  }
  
  /**
   * deletes the instance at the given index
   */
  public void deleteInstanceAt(int rowIndex, boolean notify) {
    if ( (rowIndex >= 0) && (rowIndex < getRowCount()) ) {
      if (!ignoreChanges)
        addUndoPoint();
      data.delete(rowIndex);
      if (notify)
        notifyListener(
            new TableModelEvent(
                this, rowIndex, rowIndex, 
                TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE));
    }
  }
  
  /**
   * deletes the instances at the given positions
   */
  public void deleteInstances(int[] rowIndices) {
    int               i;
    
    Arrays.sort(rowIndices);
    
    addUndoPoint();
    
    ignoreChanges = true;
    for (i = rowIndices.length - 1; i >= 0; i--)
      deleteInstanceAt(rowIndices[i], false);
    ignoreChanges = false;

    notifyListener(
        new TableModelEvent(
            this, rowIndices[0], rowIndices[rowIndices.length - 1], 
            TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE));
  }
  
  /**
   * sorts the instances via the given attribute
   */
  public void sortInstances(int columnIndex) {
    if ( (columnIndex > 0) && (columnIndex < getColumnCount()) ) {
      addUndoPoint();
      data.sort(columnIndex - 1);
      notifyListener(new TableModelEvent(this));
    }
  }
  
  /**
   * returns the column of the given attribute name, -1 if not found
   */
  public int getAttributeColumn(String name) {
    int            i;
    int            result;
    
    result = -1;
    
    for (i = 0; i < data.numAttributes(); i++) {
      if (data.attribute(i).name().equals(name)) {
        result = i + 1;
        break;
      }
    }
    
    return result;
  }
  
  /**
   * returns the most specific superclass for all the cell values in the 
   * column (always String)
   */
  public Class getColumnClass(int columnIndex) {
    Class       result;
    
    result = null;
    
    if ( (columnIndex >= 0) && (columnIndex < getColumnCount()) ) {
      if (columnIndex == 0)
        result = Integer.class;
      else if (getType(columnIndex) == Attribute.NUMERIC)
        result = Double.class;
      else
        result = String.class;   // otherwise no input of "?"!!!
    }
    
    return result;
  }
  
  /**
   * returns the number of columns in the model
   */
  public int getColumnCount() {
    int         result;
    
    result = 1;
    if (data != null)
      result += data.numAttributes();
    
    return result;
  }
  
  /**
   * checks whether the column represents the class or not
   */
  private boolean isClassIndex(int columnIndex) {
    boolean        result;
    int            index;
    
    index  = data.classIndex();
    result =    ((index == - 1) && (data.numAttributes() == columnIndex))
             || (index == columnIndex - 1);
    
    return result;
  }
  
  /**
   * returns the name of the column at columnIndex
   */
  public String getColumnName(int columnIndex) {
    String      result;
    
    result = "";
    
    if ( (columnIndex >= 0) && (columnIndex < getColumnCount()) ) {
      if (columnIndex == 0) {
        result = "<html><center>No.<br><font size=\"-2\">&nbsp;</font></center></html>";
      }
      else {
        if (data != null) {
          if ( (columnIndex - 1 < data.numAttributes()) ) {
            result = "<html><center>";
            // name
            if (isClassIndex(columnIndex))
              result +=   "<b>" 
                + data.attribute(columnIndex - 1).name() 
                + "</b>";
            else
              result += data.attribute(columnIndex - 1).name();
            
            // attribute type
            switch (getType(columnIndex)) {
              case Attribute.DATE: 
                result += "<br><font size=\"-2\">Date</font>";
                break;
              case Attribute.NOMINAL:
                result += "<br><font size=\"-2\">Nominal</font>";
                break;
              case Attribute.STRING:
                result += "<br><font size=\"-2\">String</font>";
                break;
              case Attribute.NUMERIC:
                result += "<br><font size=\"-2\">Numeric</font>";
                break;
            }
            
            result += "</center></html>";
          }
        }
      }
    }
    
    return result;
  }
  
  /**
   * returns the number of rows in the model
   */
  public int getRowCount() {
    if (data == null)
      return 0;
    else
      return data.numInstances(); 
  }
  
  /**
   * checks whether the value at the given position is missing
   */
  public boolean isMissingAt(int rowIndex, int columnIndex) {
    boolean           result;
    
    result = false;
    
    if (    (rowIndex >= 0) && (rowIndex < getRowCount())
        && (columnIndex > 0) & (columnIndex < getColumnCount()) )
      result = (data.instance(rowIndex).isMissing(columnIndex - 1));
    
    return result;
  }
  
  /**
   * returns the value for the cell at columnindex and rowIndex
   */
  public Object getValueAt(int rowIndex, int columnIndex) {
    Object            result;
    String            tmp;
    
    result = null;
    
    if (    (rowIndex >= 0) && (rowIndex < getRowCount())
        && (columnIndex >= 0) & (columnIndex < getColumnCount()) ) {
      if (columnIndex == 0) {
        result = new Integer(rowIndex + 1);
      }
      else {
        if (isMissingAt(rowIndex, columnIndex)) {
          result = null;
        }
        else {
          switch (getType(columnIndex)) {
            case Attribute.DATE: 
            case Attribute.NOMINAL:
            case Attribute.STRING:
              result = data.instance(rowIndex).stringValue(columnIndex - 1);
              break;
            case Attribute.NUMERIC:
              result = new Double(data.instance(rowIndex).value(columnIndex - 1));
              break;
          }
        }
      }
    }
    
    if (getType(columnIndex) != Attribute.NUMERIC) {
      if (result != null) {
        // does it contain "\n" or "\r"? -> replace with ", "
        tmp = result.toString();
        if ( (tmp.indexOf("\n") > -1) || (tmp.indexOf("\r") > -1) ) {
          tmp    = tmp.replaceAll("\\r\\n", ", ");
          tmp    = tmp.replaceAll("\\r", ", ").replaceAll("\\n", ", ");
          tmp    = tmp.replaceAll(", $", "");
          result = tmp;
        }
      }
    }
    
    return result;
  }
  
  /**
   * returns true if the cell at rowindex and columnindexis editable
   */
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return (columnIndex > 0);
  }
  
  /**
   * sets the value in the cell at columnIndex and rowIndex to aValue.
   * but only the value and the value can be changed
   */
  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    setValueAt(aValue, rowIndex, columnIndex, true);
  }
  
  /**
   * sets the value in the cell at columnIndex and rowIndex to aValue.
   * but only the value and the value can be changed
   */
  public void setValueAt(Object aValue, int rowIndex, int columnIndex, boolean notify) {
    int            type;
    int            index;
    String         tmp;
    Instance       inst;
    Attribute      att;
    Object         oldValue;
    
    if (!ignoreChanges)
      addUndoPoint();
    
    oldValue = getValueAt(rowIndex, columnIndex);
    type     = getType(rowIndex, columnIndex);
    index    = columnIndex - 1;
    inst     = data.instance(rowIndex);
    att      = inst.attribute(index);
    
    // missing?
    if (aValue == null) {
      inst.setValue(index, Instance.missingValue());
    }
    else {
      tmp = aValue.toString();
      
      switch (type) {
        case Attribute.DATE:
          try {
            att.parseDate(tmp);
            inst.setValue(index, att.parseDate(tmp));
          }
          catch (Exception e) {
            // ignore
          }
          break;
      
        case Attribute.NOMINAL:
          if (att.indexOfValue(tmp) > -1)
            inst.setValue(index, att.indexOfValue(tmp));
          break;
      
        case Attribute.STRING:
          inst.setValue(index, tmp);
          break;
      
        case Attribute.NUMERIC:
          try {
            Double.parseDouble(tmp);
            inst.setValue(index, Double.parseDouble(tmp));
          }
          catch (Exception e) {
            // ignore
          }
          break;
      }
    }
    
    // notify only if the value has changed!
    if (notify && (!("" + oldValue).equals("" + aValue)) )
        notifyListener(new TableModelEvent(this, rowIndex, columnIndex));
  }
  
  /**
   * adds a listener to the list that is notified each time a change to data 
   * model occurs
   */
  public void addTableModelListener(TableModelListener l) {
    listeners.add(l);
  }
  
  /**
   * removes a listener from the list that is notified each time a change to
   * the data model occurs
   */
  public void removeTableModelListener(TableModelListener l) {
    listeners.remove(l);
  }
  
  /**
   * notfies all listener of the change of the model
   */
  public void notifyListener(TableModelEvent e) {
    Iterator                iter;
    TableModelListener      l;

    // is notification enabled?
    if (!isNotificationEnabled())
      return;
    
    iter = listeners.iterator();
    while (iter.hasNext()) {
      l = (TableModelListener) iter.next();
      l.tableChanged(e);
    }
  }

  /**
   * removes the undo history
   */
  public void clearUndo() {
    undoList = new Vector();
  }
  
  /**
   * returns whether an undo is possible, i.e. whether there are any undo points
   * saved so far
   * 
   * @return returns TRUE if there is an undo possible 
   */
  public boolean canUndo() {
    return !undoList.isEmpty();
  }
  
  /**
   * undoes the last action
   */
  public void undo() {
    File                  tempFile;
    Instances             inst;
    ObjectInputStream     ooi;
    
    if (canUndo()) {
      // load file
      tempFile = (File) undoList.get(undoList.size() - 1);
      try {
        // read serialized data
        ooi = new ObjectInputStream(new BufferedInputStream(new FileInputStream(tempFile)));
        inst = (Instances) ooi.readObject();
        ooi.close();
        
        // set instances
        setInstances(inst);
        notifyListener(new TableModelEvent(this, TableModelEvent.HEADER_ROW));
        notifyListener(new TableModelEvent(this));
      }
      catch (Exception e) {
        e.printStackTrace();
      }
      tempFile.delete();
      
      // remove from undo
      undoList.remove(undoList.size() - 1);
    }
  }
  
  /**
   * adds an undo point to the undo history, if the undo support is enabled
   * @see #isUndoEnabled()
   * @see #setUndoEnabled(boolean)
   */
  public void addUndoPoint() {
    File                  tempFile;
    ObjectOutputStream    oos;

    // undo support currently on?
    if (!isUndoEnabled())
      return;
    
    if (getInstances() != null) {
      try {
        // temp. filename
        tempFile = File.createTempFile("arffviewer", null);
        tempFile.deleteOnExit();
        
        // serialize instances
        oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile)));
        oos.writeObject(getInstances());
        oos.flush();
        oos.close();
        
        // add to undo list
        undoList.add(tempFile);
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
