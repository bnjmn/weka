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
 *    LoaderCustomizer.java
 *    Copyright (C) 2002 Mark Hall
 *
 */

package weka.gui.beans;

import weka.core.converters.DatabaseConverter;
import weka.core.converters.DatabaseLoader;
import weka.core.converters.FileSourcedConverter;
import weka.gui.ExtensionFileFilter;
import weka.gui.GenericObjectEditor;
import weka.gui.PropertySheetPanel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.Customizer;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * GUI Customizer for the loader bean
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.13 $
 */
public class LoaderCustomizer
  extends JPanel
  implements Customizer, CustomizerCloseRequester {

  /** for serialization */
  private static final long serialVersionUID = 6990446313118930298L;

  static {
     GenericObjectEditor.registerEditors();
  }

  private PropertyChangeSupport m_pcSupport = 
    new PropertyChangeSupport(this);

  private weka.gui.beans.Loader m_dsLoader;

  private PropertySheetPanel m_LoaderEditor = 
    new PropertySheetPanel();

  private JFileChooser m_fileChooser 
    = new JFileChooser(new File(System.getProperty("user.dir")));
  /*  private JDialog m_chooserDialog = 
    new JDialog((JFrame)getTopLevelAncestor(),
    true); */

  private JFrame m_parentFrame;
  
  private JTextField m_dbaseURLText;
  
  private JTextField m_userNameText;
  
  private JTextField m_queryText;
   
  private JTextField m_keyText;
  
  private JPasswordField m_passwordText;

  public LoaderCustomizer() {
    /*    m_fileEditor.addPropertyChangeListener(new PropertyChangeListener() {
	public void propertyChange(PropertyChangeEvent e) {
	  if (m_dsLoader != null) {
	    m_dsLoader.setDataSetFile((File)m_fileEditor.getValue());
	  }
	}
	}); */

    try {
      /*      m_LoaderEditor.setClassType(weka.core.converters.Loader.class);
	      m_LoaderEditor.setValue(new weka.core.converters.ArffLoader()); */
      m_LoaderEditor.addPropertyChangeListener(
	  new PropertyChangeListener() {
	      public void propertyChange(PropertyChangeEvent e) {
		repaint();
		if (m_dsLoader != null) {
		  System.err.println("Property change!!");
		  m_dsLoader.setLoader(m_dsLoader.getLoader());
		}
	      }
	    });
      repaint();
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    setLayout(new BorderLayout());
    //    add(m_fileEditor.getCustomEditor(), BorderLayout.CENTER);
    //    add(m_LoaderEditor, BorderLayout.CENTER);
    m_fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
    m_fileChooser.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  if (e.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
	    try {
	      ((FileSourcedConverter)m_dsLoader.getLoader()).
		setFile(m_fileChooser.getSelectedFile());
	      // tell the loader that a new file has been selected so
	      // that it can attempt to load the header
	      m_dsLoader.setLoader(m_dsLoader.getLoader());
	    } catch (Exception ex) {
	      ex.printStackTrace();
	    }
	  }
	  // closing
	  if (m_parentFrame != null) {
	    m_parentFrame.dispose();
	  }
	}
      });   
  }

  public void setParentFrame(JFrame parent) {
    m_parentFrame = parent;
  }
  
  private void setUpOther() {
    removeAll();
    add(m_LoaderEditor, BorderLayout.CENTER);
    validate();
    repaint();
  }
  
  
  /** Sets up a customizer window for a Database Connection*/
  private void setUpDatabase() {
  
      removeAll();
      
      JPanel db = new JPanel();
      db.setLayout(new GridLayout(6, 1));
      m_dbaseURLText = new JTextField(((DatabaseConverter)m_dsLoader.getLoader()).getUrl(),50); 
      JLabel dbaseURLLab = new JLabel(" Database URL:", SwingConstants.LEFT);
      dbaseURLLab.setFont(new Font("Monospaced", Font.PLAIN, 12));

      m_userNameText = new JTextField(((DatabaseConverter)m_dsLoader.getLoader()).getUser(),50); 
      JLabel userNameLab = new JLabel(" Username:    ", SwingConstants.LEFT);
      userNameLab.setFont(new Font("Monospaced", Font.PLAIN, 12));

      m_passwordText = new JPasswordField(50); 
      JLabel passwordLab = new JLabel(" Password:    ", SwingConstants.LEFT);
      passwordLab.setFont(new Font("Monospaced", Font.PLAIN, 12));
      
      m_queryText = new JTextField(((DatabaseLoader)m_dsLoader.getLoader()).getQuery(),50); 
      JLabel queryLab = new JLabel(" Query:       ", SwingConstants.LEFT);
      queryLab.setFont(new Font("Monospaced", Font.PLAIN, 12));

      m_keyText = new JTextField(((DatabaseLoader)m_dsLoader.getLoader()).getKeys(),50); 
      JLabel keyLab = new JLabel(" Key columns: ", SwingConstants.LEFT);
      keyLab.setFont(new Font("Monospaced", Font.PLAIN, 12));

      JPanel urlP = new JPanel();   

      urlP.setLayout(new FlowLayout(FlowLayout.LEFT));
      urlP.add(dbaseURLLab);
      urlP.add(m_dbaseURLText);
      db.add(urlP);

      JPanel usernameP = new JPanel();   
      //usernameP.setLayout(new BorderLayout());
      usernameP.setLayout(new FlowLayout(FlowLayout.LEFT));
      usernameP.add(userNameLab);
      usernameP.add(m_userNameText);
      db.add(usernameP);

      JPanel passwordP = new JPanel();   
      //passwordP.setLayout(new BorderLayout());
      passwordP.setLayout(new FlowLayout(FlowLayout.LEFT));
      passwordP.add(passwordLab);
      passwordP.add(m_passwordText);
      db.add(passwordP);
      
      JPanel queryP = new JPanel();   

      queryP.setLayout(new FlowLayout(FlowLayout.LEFT));
      queryP.add(queryLab);
      queryP.add(m_queryText);
      db.add(queryP);
      
      JPanel keyP = new JPanel();   

      keyP.setLayout(new FlowLayout(FlowLayout.LEFT));
      keyP.add(keyLab);
      keyP.add(m_keyText);
      db.add(keyP);

      JPanel buttonsP = new JPanel();
      buttonsP.setLayout(new FlowLayout());
      JButton ok,cancel;
      buttonsP.add(ok = new JButton("OK"));
      buttonsP.add(cancel=new JButton("Cancel"));
      ok.addActionListener(new ActionListener(){
	public void actionPerformed(ActionEvent evt){
          ((DatabaseLoader)m_dsLoader.getLoader()).resetStructure();  
	  ((DatabaseConverter)m_dsLoader.getLoader()).setUrl(m_dbaseURLText.getText());
          ((DatabaseConverter)m_dsLoader.getLoader()).setUser(m_userNameText.getText());
          ((DatabaseConverter)m_dsLoader.getLoader()).setPassword(new String(m_passwordText.getPassword()));
	  ((DatabaseLoader)m_dsLoader.getLoader()).setQuery(m_queryText.getText());
          ((DatabaseLoader)m_dsLoader.getLoader()).setKeys(m_keyText.getText());
          try{
           m_dsLoader.notifyStructureAvailable(((DatabaseLoader)m_dsLoader.getLoader()).getStructure());
           //database connection has been configured
           m_dsLoader.setDB(true);
          }catch (Exception ex){
          }
          if (m_parentFrame != null) {
	    m_parentFrame.dispose();
	  }
      }
     });
     cancel.addActionListener(new ActionListener(){
	public void actionPerformed(ActionEvent evt){
	  if (m_parentFrame != null) {
	    m_parentFrame.dispose();
	  }
      }
    });
   
    db.add(buttonsP);
    JPanel about = m_LoaderEditor.getAboutPanel();
    if (about != null) {
      add(about, BorderLayout.NORTH);
    }
    add(db,BorderLayout.SOUTH);
  }

  public void setUpFile() {
    removeAll();
    m_fileChooser.setSelectedFile(
	((FileSourcedConverter)m_dsLoader.getLoader()).retrieveFile());
    FileSourcedConverter loader = (FileSourcedConverter) m_dsLoader.getLoader();
    String[] ext = loader.getFileExtensions();
    ExtensionFileFilter firstFilter = null;
    for (int i = 0; i < ext.length; i++) {
      ExtensionFileFilter ff =
	new ExtensionFileFilter(
	    ext[i], loader.getFileDescription() + " (*" + ext[i] + ")");
      if (i == 0)
	firstFilter = ff;
      m_fileChooser.addChoosableFileFilter(ff);
    }
    if (firstFilter != null)
      m_fileChooser.setFileFilter(firstFilter);
    JPanel about = m_LoaderEditor.getAboutPanel();
    if (about != null) {
      add(about, BorderLayout.NORTH);
    }
    add(m_fileChooser, BorderLayout.CENTER);
  }

  /**
   * Set the loader to be customized
   *
   * @param object a weka.gui.beans.Loader
   */
  public void setObject(Object object) {
    m_dsLoader = (weka.gui.beans.Loader)object;
    m_LoaderEditor.setTarget(m_dsLoader.getLoader());
    //    m_fileEditor.setValue(m_dsLoader.getDataSetFile());
    if (m_dsLoader.getLoader() instanceof FileSourcedConverter) {
      setUpFile();
    } else{ 
        if(m_dsLoader.getLoader() instanceof DatabaseConverter) {
            setUpDatabase();
        }
        else
      setUpOther();
    }
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
