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
 *    ClassifierCustomizer.java
 *    Copyright (C) 2002 Mark Hall
 *
 */

package weka.gui.beans;

import weka.core.Utils;
import weka.core.OptionHandler;
import java.beans.*;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import weka.gui.GenericObjectEditor;
import weka.gui.PropertyPanel;
import weka.classifiers.Classifier;


/**
 * GUI customizer for the classifier wrapper bean
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.2 $
 */
public class ClassifierCustomizer extends JPanel
  implements Customizer {

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
    java.beans.PropertyEditorManager
      .registerEditor(weka.classifiers.Classifier [].class,
		      weka.gui.GenericArrayEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(weka.classifiers.Classifier.class,
		      weka.gui.GenericObjectEditor.class);
    java.beans.PropertyEditorManager
      .registerEditor(weka.classifiers.CostMatrix.class,
		      weka.gui.CostMatrixEditor.class);
  }

  private PropertyChangeSupport m_pcSupport = 
    new PropertyChangeSupport(this);
  
  private weka.gui.beans.Classifier m_dsClassifier;
  private GenericObjectEditor m_ClassifierEditor = 
    new GenericObjectEditor(true);
 
  public ClassifierCustomizer() {
    try {
      m_ClassifierEditor.
	setClassType(Classifier.class);      
      m_ClassifierEditor.setValue(new weka.classifiers.bayes.NaiveBayes());
      m_ClassifierEditor.addPropertyChangeListener(new PropertyChangeListener() {
	  public void propertyChange(PropertyChangeEvent e) {
	    repaint();
	    if (m_dsClassifier != null) {
	      Classifier editedC = (Classifier)m_ClassifierEditor.getValue();
	      m_dsClassifier.setClassifier(editedC);
	      // should pass on the property change to any other interested
	      // listeners
	      //	      System.err.println("--> "+Utils.joinOptions(((OptionHandler)editedC).getOptions()));
	      //	      System.err.println("Setting classifier in weka.gui.Classifier");
	    }
	  }
	});
      //      System.out.println("Here");
      repaint();
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    setLayout(new BorderLayout());
    add(m_ClassifierEditor.getCustomEditor(), BorderLayout.CENTER);
  }
  
  /**
   * Set the classifier object to be edited
   *
   * @param object an <code>Object</code> value
   */
  public void setObject(Object object) {
    m_dsClassifier = (weka.gui.beans.Classifier)object;
    //    System.err.println(Utils.joinOptions(((OptionHandler)m_dsClassifier.getClassifier()).getOptions()));
    m_ClassifierEditor.setValue(m_dsClassifier.getClassifier());
    //    m_ClassifierEditor.setValue(m_dsClassifier.getClassifier());
  }

  /**
   * Add a property change listener
   *
   * @param pcl a <code>PropertyChangeListener</code> value
   */
  public void addPropertyChangeListener(PropertyChangeListener pcl) {
    m_pcSupport.addPropertyChangeListener(pcl);
  }

  /**
   * Remove a property change listener
   *
   * @param pcl a <code>PropertyChangeListener</code> value
   */
  public void removePropertyChangeListener(PropertyChangeListener pcl) {
    m_pcSupport.removePropertyChangeListener(pcl);
  }
}
