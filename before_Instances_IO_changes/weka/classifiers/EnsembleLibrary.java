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
 *    EnsembleLibrary.java
 *    Copyright (C) 2006 Robert Jung
 *
 */

package weka.classifiers;

import weka.gui.ensembleLibraryEditor.LibrarySerialization;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Serializable;
import java.io.Writer;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JOptionPane;

/** 
 * This class represents a library of classifiers.  This class 
 * follows the factory design pattern of creating LibraryModels
 * when asked.  It also has the methods necessary for saving
 * and loading models from lists.  
 * 
 * @author  Robert Jung (mrbobjung@gmail.com)
 * @version $Revision: 1.1 $
 */
public class EnsembleLibrary
  implements Serializable {
  
  /** for serialization */
  private static final long serialVersionUID = -7987178904923706760L;

  /** The default file extension for model list files */
  public static final String XML_FILE_EXTENSION = ".model.xml";
  
  /** The flat file extension for model list files */
  public static final String FLAT_FILE_EXTENSION = ".mlf";
  
  /** the set of classifiers that constitute the library */
  public TreeSet m_Models;
  
  /** A helper class for notifying listeners when the library changes */
  private transient PropertyChangeSupport m_LibraryPropertySupport = new PropertyChangeSupport(this);
  
  /**
   * Constructor is responsible for initializing the data 
   * structure hoilding all of the models
   *
   */
  public EnsembleLibrary() {
    
    m_Models = new TreeSet(new EnsembleLibraryModelComparator());
  }
  
  /**
   * Returns the number of models in the ensemble library 
   * 
   * @return	the number of models
   */
  public int size() {
    if (m_Models != null)
      return m_Models.size();
    else
      return 0;
  }
  
  /**
   * adds a LibraryModel to the Library
   * 
   * @param model	the model to add
   */
  public void addModel(EnsembleLibraryModel model) {
    m_Models.add(model);
    if (m_LibraryPropertySupport != null)
      m_LibraryPropertySupport.firePropertyChange(null, null, null);
  }
  
  /**
   * adds a LibraryModel to the Library
   * 
   * @param modelString	the model to add
   */
  public void addModel(String modelString) {
    m_Models.add(createModel(modelString));
    m_LibraryPropertySupport.firePropertyChange(null, null, null);
  }
  
  /**
   * removes a LibraryModel from the Library
   * 
   * @param model	the model to remove
   */
  public void removeModel(EnsembleLibraryModel model) {
    m_Models.remove(model);
    m_LibraryPropertySupport.firePropertyChange(null, null, null);
  }
  
  /**
   * creates a LibraryModel from a string representing the command
   * line invocation
   * 
   * @param classifier	the classifier to create a model from 
   * @return		the generated model
   */
  public EnsembleLibraryModel createModel(Classifier classifier) {
    EnsembleLibraryModel model = new EnsembleLibraryModel(classifier);
    
    return model;
  }
  
  /**
   * This method takes a String argument defining a classifier and
   * uses it to create a base Classifier.
   * 
   * @param modelString	the classifier string
   * @return		the generated model
   */
  public EnsembleLibraryModel createModel(String modelString) {
    
    String[] splitString = modelString.split("\\s+");
    String className = splitString[0];
    
    String argString = modelString.replaceAll(splitString[0], "");
    String[] optionStrings = argString.split("\\s+");
    
    EnsembleLibraryModel model = null;
    try {
      model = new EnsembleLibraryModel(Classifier.forName(className,
	  optionStrings));
    } catch (Exception e) {
      e.printStackTrace();
    }
    return model;
  }
  
  /**
   * getter for the set of models in this library
   * 
   * @return		the current models
   */
  public TreeSet getModels() {
    return m_Models;
  }
  
  /**
   * setter for the set of models in this library
   * 
   * @param models	the models to use
   */
  public void setModels(TreeSet models) {
    m_Models = models;
    m_LibraryPropertySupport.firePropertyChange(null, null, null);
  }
  
  /**
   * removes all models from the current library
   */
  public void clearModels() {
    m_Models.clear();
    m_LibraryPropertySupport.firePropertyChange(null, null, null);
  }
  
  /**
   * Loads and returns a library from the specified file
   * 
   * @param selectedFile	the file to load from
   * @param dialogParent	the parent component
   * @param library		will contain the data after loading
   */
  public static void loadLibrary(File selectedFile, JComponent dialogParent,
      EnsembleLibrary library) {
    
    try {
      loadLibrary(selectedFile, library);
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(dialogParent, "Error reading file '"
	  + selectedFile.getName() + "':\n" + ex.getMessage(),
	  "Load failed", JOptionPane.ERROR_MESSAGE);
      System.err.println(ex.getMessage());
    }
  }
  
  /**
   * This method takes a model list file and a library object as arguments and
   * Instantiates all of the models in the library list file.  It is assumed
   * that the passed library was an associated working directory and can take
   * care of creating the model objects itself.  
   * 
   * @param selectedFile	the file to load
   * @param library		the library 
   * @throws Exception		if something goes wrong
   */
  public static void loadLibrary(File selectedFile, EnsembleLibrary library)
    throws Exception {
    
    //decide what type of model file list we are dealing with and
    //then load accordingly
    
    //deal with XML extension for xml files
    if (selectedFile.getName().toLowerCase().endsWith(
	EnsembleLibrary.XML_FILE_EXTENSION)) {
      
      LibrarySerialization librarySerialization;
      
      Vector classifiers = null;
      
      try {
	librarySerialization = new LibrarySerialization();
	classifiers = (Vector) librarySerialization.read(selectedFile.getPath());
      } catch (Exception e) {
	e.printStackTrace();
      }
      
      //library.setClassifiers(classifiers);
      
      for (Iterator it = classifiers.iterator(); it.hasNext();) {
	
	EnsembleLibraryModel model = library.createModel((Classifier) it.next());
	model.testOptions();
	library.addModel(model);
      }
      
      //deal with MLF extesion for flat files
    } else if (selectedFile.getName().toLowerCase().endsWith(
	EnsembleLibrary.FLAT_FILE_EXTENSION)) {
      
      BufferedReader reader = null;
      
      reader = new BufferedReader(new FileReader(selectedFile));
      
      String modelString;
      
      while ((modelString = reader.readLine()) != null) {
	
	EnsembleLibraryModel model = library.createModel(modelString);
	
	if (model != null) {
	  model.testOptions();
	  library.addModel(model);
	} else {
	  System.err.println("Failed to create model: " + modelString);
	}
      }
      
      reader.close();
    }
  }
  
  /**
   * This method takes an XML input stream and a library object as arguments 
   * and Instantiates all of the models in the stream.  It is assumed
   * that the passed library was an associated working directory and can take
   * care of creating the model objects itself.  
   * 
   * @param stream		the XML stream to load
   * @param library		the library 
   * @throws Exception		if something goes wrong
   */
  public static void loadLibrary(InputStream stream, EnsembleLibrary library)
    throws Exception {

    Vector classifiers = null;
    
    try {
      LibrarySerialization librarySerialization = new LibrarySerialization();
      classifiers = (Vector) librarySerialization.read(stream);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    
    for (int i = 0; i < classifiers.size(); i++) {
      EnsembleLibraryModel model = library.createModel((Classifier) classifiers.get(i));
      model.testOptions();
      library.addModel(model);
    }
  }
    
  
  /**
   * Saves the given library in the specified file.  This saves only
   * the specification of the models as a model list.  
   *
   * @param selectedFile	the file to save to
   * @param library		the library to save
   * @param dialogParent	the component parent
   */
  public static void saveLibrary(File selectedFile, EnsembleLibrary library,
      JComponent dialogParent) {
    
    //save decide what type of model file list we are dealing with and
    //then save accordingly
    
    //System.out.println("writing to file: "+selectedFile.getPath());
    
    //deal with XML extension for xml files
    if (selectedFile.getName().toLowerCase().endsWith(
	EnsembleLibrary.XML_FILE_EXTENSION)) {
      
      LibrarySerialization librarySerialization;
      
      Vector classifiers = new Vector();
      
      for (Iterator it = library.getModels().iterator(); it.hasNext();) {
	EnsembleLibraryModel model = (EnsembleLibraryModel) it.next();
	classifiers.add(model.getClassifier());
      }
      
      try {
	librarySerialization = new LibrarySerialization();
	librarySerialization.write(selectedFile.getPath(), classifiers);
      } catch (Exception e) {
	e.printStackTrace();
      }
      
      //deal with MLF extesion for flat files
    } else if (selectedFile.getName().toLowerCase().endsWith(
	EnsembleLibrary.FLAT_FILE_EXTENSION)) {
      
      Writer writer = null;
      try {
	writer = new BufferedWriter(new FileWriter(selectedFile));
	
	Iterator it = library.getModels().iterator();
	
	while (it.hasNext()) {
	  EnsembleLibraryModel model = (EnsembleLibraryModel) it.next();
	  writer.write(model.getStringRepresentation() + "\n");
	}
	
	writer.close();
      } catch (Exception ex) {
	JOptionPane.showMessageDialog(dialogParent,
	    "Error writing file '" + selectedFile.getName()
	    + "':\n" + ex.getMessage(), "Save failed",
	    JOptionPane.ERROR_MESSAGE);
	System.err.println(ex.getMessage());
      }
    }
  }
  
  /**
   * Adds an object to the list of those that wish to be informed when the
   * library changes.
   *
   * @param listener a new listener to add to the list
   */
  public void addPropertyChangeListener(PropertyChangeListener listener) {
    
    if (m_LibraryPropertySupport != null) {
      m_LibraryPropertySupport.addPropertyChangeListener(listener);
      
    }
  }
}
