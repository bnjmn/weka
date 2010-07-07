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
 *    ModelList.java
 *    Copyright (C) 2006 Robert Jung
 *
 */

package weka.gui.ensembleLibraryEditor;

import weka.classifiers.EnsembleLibraryModel;
import weka.classifiers.EnsembleLibraryModelComparator;

import java.awt.Color;
import java.awt.Component;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JList;

/**
 * This class is basically a customization of the JList class to allow it
 * to display LibraryModel objects.  It has two nested helper classes that
 * respectively take care of rendering and modelling the List of models.
 * 
 * @author Robert Jung (mrbobjung@gmail.com)
 */
public class ModelList 
  extends JList {
  
  /** for serialization */
  private static final long serialVersionUID = -421567241792939539L;

  /**
   * The constructor simply initializes the model and the renderer.
   */
  public ModelList() {
    super();
    this.setModel(new SortedListModel());
    this.setCellRenderer(new ModelListRenderer());
  }
  
  /**
   * This nested helper class is responsible for rendering each Library
   * Model object.  
   */
  public class ModelListRenderer 
    extends DefaultListCellRenderer {
    
    /** for serialization */
    private static final long serialVersionUID = -7061163240718897794L;

    /**
     * This is the only method necessary to overload.  All we have to 
     * do is print the String value of the model along with its index 
     * in the ModelList data structure.
     * 
     * @param list		the JList
     * @param value		the value
     * @param index		the index of the value
     * @param isSelected	if true the item is selected
     * @param cellHasFocus	whether it has the focus
     * @return			the rendering component
     */
    public Component getListCellRendererComponent(JList list, Object value,
	int index, boolean isSelected, boolean cellHasFocus) {
      
      Component modelComponent = null;
      
      if (value instanceof EnsembleLibraryModel) {
	
	EnsembleLibraryModel model = ((EnsembleLibraryModel) value);
	
	String modelString = index
	+ ": "
	+ model.getStringRepresentation().replaceAll(
	    "weka.classifiers.", "");
	
	modelComponent = super.getListCellRendererComponent(list,
	    modelString, index, isSelected, cellHasFocus);
	
	if (!model.getOptionsWereValid()) {
	  modelComponent.setBackground(Color.pink);
	}
	
	((JComponent) modelComponent).setToolTipText(model
	    .getDescriptionText());
	
      }
      
      return modelComponent;
    }
  }
  
  /**
   * 
   * This is a helper class that creates a custom list model for the ModelList class.
   * It basically ensures that all model entries are 1) unique - so that no duplicate
   * entries can find their way in, and 2) sorted alphabetically.  It also numbers 
   * them.
   * <p/>
   * This nested class was adapted from code found in a freely available tutorial on
   * sorting JList entries by John Zukowski - wait a sec, he's the guy who wrote the
   * other tutorial I cited in the AddModelsPanel. wow, different web site even. 
   * This guy is really in to writing tutorials.  Anyway, it was very helpful, if 
   * you would like to know more about implementing swing MVC stuff.
   * <p/>
   * Anyway, John Zukowski's tutorial can be found at: <br/>
   * <a href="http://www.jguru.com/faq/view.jsp?EID=15245" target="_blank">http://www.jguru.com/faq/view.jsp?EID=15245</a>
   */
  public class SortedListModel extends AbstractListModel {
    
    /** for serialization */
    private static final long serialVersionUID = -8334675481243839371L;
    
    /** Define a SortedSet */
    SortedSet m_Models;
    
    /**
     * default constructor
     */
    public SortedListModel() {
      // Create a TreeSet
      // Store it in SortedSet variable
      m_Models = new TreeSet(new EnsembleLibraryModelComparator());
    }
    
    // ListModel methods
    public int getSize() {
      // Return the model size
      return m_Models.size();
    }
    
    public Object getElementAt(int index) {
      // Return the appropriate element
      return m_Models.toArray()[index];
    }
    
    // Other methods
    public void add(Object element) {
      if (m_Models.add(element)) {
	fireContentsChanged(this, 0, getSize());
      }
    }
    
    public void addAll(Object elements[]) {
      Collection c = Arrays.asList(elements);
      m_Models.addAll(c);
      fireContentsChanged(this, 0, getSize());
    }
    
    public void clear() {
      m_Models.clear();
      fireContentsChanged(this, 0, getSize());
    }
    
    public boolean contains(Object element) {
      return m_Models.contains(element);
    }
    
    public Object firstElement() {
      // Return the appropriate element
      return m_Models.first();
    }
    
    public Iterator iterator() {
      return m_Models.iterator();
    }
    
    public Object lastElement() {
      // Return the appropriate element
      return m_Models.last();
    }
    
    public boolean removeElement(Object element) {
      boolean removed = m_Models.remove(element);
      if (removed) {
	fireContentsChanged(this, 0, getSize());
      }
      return removed;
    }
  }
}
