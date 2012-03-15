/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    EnsembleLibraryModel.java
 *    Copyright (C) 2006 Robert Jung
 *
 */

package weka.classifiers;

import weka.core.OptionHandler;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.gui.GenericObjectEditor;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This class is a light wrapper that is meant to represent a
 * classifier in a classifier library.  This simple base class
 * is intended only to store the class type and the parameters
 * for a specific "type" of classifier.  You will need to
 * extend this class to add functionality specific to models
 * that have been trained on data (as we have for Ensemble Selection)
 *
 * @author  Robert Jung (mrbobjung@gmail.com)
 * @version $Revision$
 */
public class EnsembleLibraryModel
  implements Serializable, RevisionHandler {

  /** this stores the class of the classifier */
  //private Class m_ModelClass;

  /**
   * This is the serialVersionUID that SHOULD stay the same
   * so that future modified versions of this class will be
   * backwards compatible with older model versions.
   */
  private static final long serialVersionUID = 7932816660173443200L;

  /** this is an array of options*/
  protected Classifier m_Classifier;

  /** the description of this classifier*/
  protected String m_DescriptionText;

  /** this is also saved separately */
  protected String m_ErrorText;

  /** a boolean variable tracking whether or not this classifier was
   * able to be built successfully with the given set of options*/
  protected boolean m_OptionsWereValid;

  /** this is stores redundantly to speed up the many operations that
   frequently need to get the model string representations (like JList
   renderers) */
  protected String m_StringRepresentation;

  /**
   * Default Constructor
   */
  public EnsembleLibraryModel() {
    super();
  }

  /**
   * Initializes the class with the given classifier.
   *
   * @param classifier	the classifier to use
   */
  public EnsembleLibraryModel(Classifier classifier) {

    m_Classifier = classifier;

    //this may seem redundant and stupid, but it really is a good idea
    //to cache the stringRepresentation to minimize the amount of work
    //that needs to be done when these Models are rendered
    m_StringRepresentation = toString();

    updateDescriptionText();
  }

  /**
   * This method will attempt to instantiate this classifier with the given
   * options. If an exception is thrown from the setOptions method of the
   * classifier then the resulting exception text will be saved in the
   * description text string.
   */
  public void testOptions() {

    Classifier testClassifier = null;
    try {
      testClassifier = ((Classifier) m_Classifier.getClass().newInstance());
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }

    setOptionsWereValid(true);
    setErrorText(null);
    updateDescriptionText();

    try {
      ((OptionHandler)testClassifier).setOptions(((OptionHandler)m_Classifier).getOptions());
    } catch (Exception e) {

      setOptionsWereValid(false);
      setErrorText(e.getMessage());
    }

    updateDescriptionText();
  }

  /**
   * Returns the base classifier this library model represents.
   *
   * @return		the base classifier
   */
  public Classifier getClassifier() {
    return m_Classifier;
  }

  /**
   * getter for the string representation
   *
   * @return		the string representation
   */
  public String getStringRepresentation() {
    return m_StringRepresentation;
  }

  /**
   * setter for the description text
   *
   * @param descriptionText	the description
   */
  public void setDescriptionText(String descriptionText) {
    m_DescriptionText = descriptionText;
  }

  /**
   * getter for the string representation
   *
   * @return		the description
   */
  public String getDescriptionText() {
    return m_DescriptionText;
  }

  /**
   * setter for the error text
   *
   * @param errorText	the error text
   */
  public void setErrorText(String errorText) {
    this.m_ErrorText = errorText;
  }

  /**
   * getter for the error text
   *
   * @return		the error text
   */
  public String getErrorText() {
    return m_ErrorText;
  }

  /**
   * setter for the optionsWereValid member variable
   *
   * @param optionsWereValid	if true, the options were valid
   */
  public void setOptionsWereValid(boolean optionsWereValid) {
    this.m_OptionsWereValid = optionsWereValid;
  }

  /**
   * getter for the optionsWereValid member variable
   *
   * @return		true if the options were valid
   */
  public boolean getOptionsWereValid() {
    return m_OptionsWereValid;
  }

  /**
   * This method converts the current set of arguments and the
   * class name to a string value representing the command line
   * invocation
   *
   * @return		a string representation of classname and options
   */
  public String toString() {

    String str = m_Classifier.getClass().getName();

    str += " " + Utils.joinOptions(((OptionHandler) m_Classifier).getOptions());

    return str;

  }

  /**
   * getter for the modelClass
   *
   * @return		the model class
   */
  public Class getModelClass() {
    return m_Classifier.getClass();
  }

  /**
   * getter for the classifier options
   *
   * @return		the classifier options
   */
  public String[] getOptions() {
    return ((OptionHandler)m_Classifier).getOptions();
  }

  /**
   * Custom serialization method
   *
   * @param stream		the stream to write the object to
   * @throws IOException	if something goes wrong IO-wise
   */
  private void writeObject(ObjectOutputStream stream) throws IOException {
    //stream.defaultWriteObject();
    //stream.writeObject(b);

    //serialize the LibraryModel fields

    stream.writeObject(m_Classifier);

    stream.writeObject(m_DescriptionText);

    stream.writeObject(m_ErrorText);

    stream.writeObject(new Boolean(m_OptionsWereValid));

    stream.writeObject(m_StringRepresentation);
  }

  /**
   * Custom serialization method
   *
   * @param stream			the stream to write to
   * @throws IOException		if something goes wrong IO-wise
   * @throws ClassNotFoundException	if class couldn't be found
   */
  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {

    m_Classifier = (Classifier) stream.readObject();

    m_DescriptionText = (String) stream.readObject();

    m_ErrorText = (String) stream.readObject();

    m_OptionsWereValid = ((Boolean) stream.readObject()).booleanValue();

    m_StringRepresentation = (String) stream.readObject();
  }

  /**
   * This method loops through all of the properties of a classifier to
   * build the html toolTipText that will show all of the property values
   * for this model.
   *
   * Note that this code is copied+adapted from the PropertySheetPanel
   * class.
   */
  public void updateDescriptionText() {

    String toolTipText = new String("<html>");

    if (!m_OptionsWereValid) {

      toolTipText += "<font COLOR=\"#FF0000\">"
        + "<b>Invalid Model:</b><br>" + m_ErrorText + "<br></font>";
    }

    toolTipText += "<TABLE>";

    PropertyDescriptor properties[] = null;
    MethodDescriptor methods[] = null;

    try {
      BeanInfo bi = Introspector.getBeanInfo(m_Classifier.getClass());
      properties = bi.getPropertyDescriptors();
      methods = bi.getMethodDescriptors();
    } catch (IntrospectionException ex) {
      System.err.println("LibraryModel: Couldn't introspect");
      return;
    }

    for (int i = 0; i < properties.length; i++) {

      if (properties[i].isHidden() || properties[i].isExpert()) {
        continue;
      }

      String name = properties[i].getDisplayName();
      Class type = properties[i].getPropertyType();
      Method getter = properties[i].getReadMethod();
      Method setter = properties[i].getWriteMethod();

      // Only display read/write properties.
      if (getter == null || setter == null) {
        continue;
      }

      try {
        Object args[] = {};
        Object value = getter.invoke(m_Classifier, args);

        PropertyEditor editor = null;
        Class pec = properties[i].getPropertyEditorClass();
        if (pec != null) {
          try {
            editor = (PropertyEditor) pec.newInstance();
          } catch (Exception ex) {
            // Drop through.
          }
        }
        if (editor == null) {
          editor = PropertyEditorManager.findEditor(type);
        }
        //editors[i] = editor;

        // If we can't edit this component, skip it.
        if (editor == null) {
          continue;
        }
        if (editor instanceof GenericObjectEditor) {
          ((GenericObjectEditor) editor).setClassType(type);
        }

        // Don't try to set null values:
        if (value == null) {
          continue;
        }

        toolTipText += "<TR><TD>" + name + "</TD><TD>"
          + value.toString() + "</TD></TR>";

      } catch (InvocationTargetException ex) {
        System.err.println("Skipping property " + name
            + " ; exception on target: " + ex.getTargetException());
        ex.getTargetException().printStackTrace();
        continue;
      } catch (Exception ex) {
        System.err.println("Skipping property " + name
            + " ; exception: " + ex);
        ex.printStackTrace();
        continue;
      }
    }

    toolTipText += "</TABLE>";
    toolTipText += "</html>";
    m_DescriptionText = toolTipText;
  }

  /**
   * Returns the revision string.
   *
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }
}
