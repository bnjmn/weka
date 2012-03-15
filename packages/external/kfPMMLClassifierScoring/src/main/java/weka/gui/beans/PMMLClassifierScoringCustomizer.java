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
 *    PMMLClassifierScoringCustomizer.java
 *    Copyright (C) 2008 Pentaho Corporation
 *
 */

package weka.gui.beans;

import java.awt.BorderLayout;
import java.awt.Font;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.beans.Customizer;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.gui.PropertySheetPanel;
import weka.core.pmml.PMMLFactory;
import weka.core.pmml.PMMLModel;
import weka.classifiers.pmml.consumer.PMMLClassifier;

/**
 * Customizer class for the PMMLClassifierScoring component.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com
 * @version $Revision 1.0 $
 */
public class PMMLClassifierScoringCustomizer extends JPanel 
  implements Customizer, CustomizerClosingListener, EnvironmentHandler {
  
  /**
   * For serialization
   */
  private static final long serialVersionUID = -3416034073289828558L;


  /**
   * Small inner class that will eventually handle giving the
   * user access to environment variables that can be used in
   * the filename string via a pop-up window.
   * Requires a method to be added to weka.core.Environment before
   * this functionality can be implemented.
   
  protected class TextVar extends JTextField {
                
   
    private static final long serialVersionUID = -8240673574285803320L;

    public TextVar() {
      this.addKeyListener(new KeyAdapter() {
        public void keyTyped(KeyEvent k) {
          if (k.isControlDown() && k.getKeyCode() == 32) {
            handleControlSpace();
          }
        }
      });
    }
    
    private void handleControlSpace() {
      // Need to add a method to weka.core.Environment before
      // we can popup a list of environment variables to choose
      // from!!!
    }
    } */

  /** The object we are editing */
  protected PMMLClassifierScoring m_scoring;
  
  /**
   * The PMMLClassifier to display and use
   */
  protected PMMLClassifier m_pmmlClassifier = null;

  /** Handles the text field and file browser */
  protected FileEnvironmentField m_filenameField = 
    new FileEnvironmentField();

  /** Environment variables to use */
  protected transient Environment m_env = Environment.getSystemWide();
   
  /** Text field for the filename */
  //  protected TextVar m_filenameField = new TextVar();

  /** Button to popup the file chooser */
  //  protected JButton m_fileChooserBut = new JButton("Browse...");
  
  /** The text area to display the model in */
  protected JTextArea m_modelDisplay = new JTextArea(20, 60);

  /** The filechooser */
  //  private JFileChooser m_fileChooser
  //    = new JFileChooser(new File(System.getProperty("user.dir")));
  
  /** Property sheet panel used to get the "About" panel for global info */
  protected PropertySheetPanel m_sheetPanel = new PropertySheetPanel();
  
  /**
   * Constructs the customizer. 
   */
  public PMMLClassifierScoringCustomizer() {
    setLayout(new BorderLayout());
    
    /*m_filenameField.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        loadModel();
        if (m_pmmlClassifier != null) {
          m_modelDisplay.setText(m_pmmlClassifier.toString());
        }
      }
      }); */
    
    m_filenameField.resetFileFilters();
    m_filenameField.addFileFilter(new FileFilter() { 
      public boolean accept(File f) {
        String name = f.getName().toLowerCase();
        if (f.isDirectory()) {
          return true;
        }
        if (name.endsWith(".xml")) {
          return true;
        }
        return false;
      }
      public String getDescription() { 
        return "PMML model file";
        }
     });

    m_filenameField.addPropertyChangeListener(new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          loadModel();
          if (m_pmmlClassifier != null) {
            m_modelDisplay.setText(m_pmmlClassifier.toString());
          }
        }
      });
        
    /*    m_fileChooserBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String fromTextVar = m_filenameField.getText();
        if (!PMMLClassifierScoring.isEmpty(fromTextVar)) {
          try {
            Environment env = m_scoring.getEnvironment();
            if (env == null) {
              env = Environment.getSystemWide();
            }
            fromTextVar = env.substitute(fromTextVar);
            File temp = new File(fromTextVar);
            if (temp.isFile()) {
              temp = temp.getParentFile();
            }
            if (temp.isDirectory()) {
              m_fileChooser.setCurrentDirectory(temp);
            }
          } catch (Exception ex) {
            // contains unknown environment variable
          }
        }
        
        int returnVal = m_fileChooser.showOpenDialog(PMMLClassifierScoringCustomizer.this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
          m_filenameField.setText(m_fileChooser.getSelectedFile().getAbsolutePath());
          loadModel();
          if (m_pmmlClassifier != null) {
            m_modelDisplay.setText(m_pmmlClassifier.toString());
          }
        }
      }
      }); */
  }
  
  private void setUpLayout() {
    removeAll();
    JPanel tempP = new JPanel();
    tempP.setLayout(new BorderLayout());
    tempP.setBorder(BorderFactory.createTitledBorder("File"));
    tempP.add(m_filenameField, BorderLayout.CENTER);
    //    tempP.add(m_fileChooserBut, BorderLayout.EAST);
    
    JPanel tempP2 = new JPanel();
    tempP2.setLayout(new BorderLayout());
    
    tempP2.add(tempP, BorderLayout.SOUTH);
    
    JPanel about = m_sheetPanel.getAboutPanel();
    if (about != null) {
      tempP2.add(about, BorderLayout.NORTH);
    }
    add(tempP2, BorderLayout.NORTH);
    
    m_modelDisplay.setEditable(false);
    m_modelDisplay.setFont(new Font("Monospaced", Font.PLAIN, 12));
    m_modelDisplay.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    JPanel tempP3 = new JPanel();
    tempP3.setLayout(new BorderLayout());
    tempP3.setBorder(BorderFactory.createTitledBorder("Model"));
    
    JScrollPane scrollPane = new JScrollPane(m_modelDisplay);
    tempP3.add(scrollPane, BorderLayout.CENTER);
    add(tempP3, BorderLayout.CENTER);
  }
  
  
  /**                                                                                                             
   * Set the PMMLClassifierScoring object to be edited                                                                       
   *                                                                                                              
   * @param object an <code>Object</code> value                                                                   
   */
  public void setObject(Object object) {
    m_scoring = (PMMLClassifierScoring)object;
    m_sheetPanel.setTarget(m_scoring);
    
    m_pmmlClassifier = m_scoring.getClassifier();
 
    // A filename overrides any object set in the PMMLClassifierScoring already
    if (!PMMLClassifierScoring.isEmpty(m_scoring.getFilename())) {
      m_filenameField.setText(m_scoring.getFilename());
      loadModel();
    }
    
    if (m_pmmlClassifier != null) {
      m_modelDisplay.setText(m_pmmlClassifier.toString());
    }
    
    setUpLayout();
  }
  
  private void loadModel() {
    if (!PMMLClassifierScoring.isEmpty(m_filenameField.getText())) {
      // attempt to load the PMML from here so that we can display the model
      try {
        // if we can successfully substitute any environment variables and
        // resolve this filename to a real file, then we can load it.
        String filename = m_filenameField.getText();
        try {
          //          Environment env = m_scoring.getEnvironment();
          if (m_env == null) {
            m_env = Environment.getSystemWide();
          }
          filename = m_env.substitute(filename);
        } catch (Exception e) {
          // Quietly ignore any problems with environment variables.
          // A variable might not be set now, but could be when the
          // PMMLClassifierScoring component is executed at a later time
        }
        File theFile = new File(filename);
        if (theFile.isFile()) {
          PMMLModel model = PMMLFactory.getPMMLModel(theFile, null);
          if (model instanceof PMMLClassifier) {
            m_pmmlClassifier = (PMMLClassifier)model;
          }
        }
      } catch (Exception ex) {
        // report any problems with loading the PMML
        ex.printStackTrace();
      }
    }
  }

  /**
   * Set the environment variables to use.
   *
   * @param env the environment variables to use
   */
  public void setEnvironment(Environment env) {
    m_env = env;
    m_filenameField.setEnvironment(env);
  }
  

  /**
   * Gets called by the KnowledgeFlow environment when the customizer's
   * window is closed. Allows us to set the filename (if not empty) on the
   * PMMLClassifierScoring we are editing.
   */
  public void customizerClosing() {
    if (!PMMLClassifierScoring.isEmpty(m_filenameField.getText())) {
      m_scoring.setFilename(m_filenameField.getText());
    }
  }
}
