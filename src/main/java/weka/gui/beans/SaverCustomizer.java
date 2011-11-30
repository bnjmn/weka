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
 *    SaverCustomizer.java
 *    Copyright (C) 2004 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import weka.core.converters.DatabaseConverter;
import weka.core.converters.DatabaseSaver;
import weka.core.converters.FileSourcedConverter;
import weka.gui.GenericObjectEditor;
import weka.gui.PropertySheetPanel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.Customizer;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;

/**
 * GUI Customizer for the saver bean
 *
 * @author <a href="mailto:mutter@cs.waikato.ac.nz">Stefan Mutter</a>
 * @version $Revision$
 */
public class SaverCustomizer
  extends JPanel
  implements Customizer, CustomizerCloseRequester {

  /** for serialization */
  private static final long serialVersionUID = -4874208115942078471L;

  static {
     GenericObjectEditor.registerEditors();
  }

  private PropertyChangeSupport m_pcSupport = 
    new PropertyChangeSupport(this);

  private weka.gui.beans.Saver m_dsSaver;

  private PropertySheetPanel m_SaverEditor = 
    new PropertySheetPanel();

  private JFileChooser m_fileChooser 
    = new JFileChooser(new File(System.getProperty("user.dir")));
  

  private JFrame m_parentFrame;
  
  private JTextField m_dbaseURLText;
  
  private JTextField m_userNameText;
  
  private JPasswordField m_passwordText;
  
  private JTextField m_tableText;
  
  private JComboBox m_idBox;
  
  private JComboBox m_tabBox;
  
  private JTextField m_prefixText;

  private JCheckBox m_relativeFilePath;
  
  private JCheckBox m_relationNameForFilename;
  

  /** Constructor */  
  public SaverCustomizer() {

    try {
      m_SaverEditor.addPropertyChangeListener(
	  new PropertyChangeListener() {
	      public void propertyChange(PropertyChangeEvent e) {
		repaint();
		if (m_dsSaver != null) {
		  System.err.println(Messages.getInstance().getString("SaverCustomizer_Error_Text"));
		  m_dsSaver.setSaverTemplate(m_dsSaver.getSaverTemplate());
		}
	      }
	    });
      repaint();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    setLayout(new BorderLayout());
    m_fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
    m_fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    m_fileChooser.setApproveButtonText(Messages.getInstance().getString("SaverCustomizer_FileChooser_SetApproveButtonText_Text"));
    m_fileChooser.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  if (e.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
	    try {
                (m_dsSaver.getSaverTemplate()).setFilePrefix(m_prefixText.getText());
                (m_dsSaver.getSaverTemplate()).setDir(m_fileChooser.getSelectedFile().getPath());
                m_dsSaver.
                  setRelationNameForFilename(m_relationNameForFilename.isSelected());
               
	      // m_dsSaver.setSaver(m_dsSaver.getSaver());
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
  
  /** Sets up dialog for saving instances in other data sinks then files
   * To be extended.
   */ 
  private void setUpOther() {
    removeAll();
    add(m_SaverEditor, BorderLayout.CENTER);
    validate();
    repaint();
  }
  
  /** Sets up the dialog for saving to a database*/
  private void setUpDatabase() {
  
      removeAll();
      JPanel db = new JPanel();
      db.setLayout(new GridLayout(7, 1));
      m_dbaseURLText = new JTextField(((DatabaseConverter)m_dsSaver.getSaverTemplate()).getUrl(),50); 
      JLabel dbaseURLLab = new JLabel(Messages.getInstance().getString("SaverCustomizer_SetUpDatabase_DbaseURLLab_JLabel_Text"), SwingConstants.LEFT);
      dbaseURLLab.setFont(new Font("Monospaced", Font.PLAIN, 12));

      m_userNameText = new JTextField(((DatabaseConverter)m_dsSaver.getSaverTemplate()).getUser(),50); 
      JLabel userNameLab = new JLabel(Messages.getInstance().getString("SaverCustomizer_SetUpDatabase_UserNameLab_JLabel_Text"), SwingConstants.LEFT);
      userNameLab.setFont(new Font("Monospaced", Font.PLAIN, 12));

      m_passwordText = new JPasswordField(50); 
      m_passwordText.setText(((DatabaseSaver)m_dsSaver.getSaverTemplate()).getPassword());
      JLabel passwordLab = new JLabel(Messages.getInstance().getString("SaverCustomizer_SetUpDatabase_PasswordLab_JLabel_Text"), SwingConstants.LEFT);
      passwordLab.setFont(new Font("Monospaced", Font.PLAIN, 12));
      
      m_tableText = new JTextField(((DatabaseSaver)m_dsSaver.getSaverTemplate()).getTableName(),50); 
      m_tableText.setEditable(!((DatabaseSaver)m_dsSaver.getSaverTemplate()).getRelationForTableName());
      JLabel tableLab = new JLabel(Messages.getInstance().getString("SaverCustomizer_SetUpDatabase_TableLab_JLabel_Text"), SwingConstants.LEFT);
      tableLab.setFont(new Font("Monospaced", Font.PLAIN, 12));
      
      m_tabBox = new JComboBox();
      m_tabBox.addItem(new Boolean(true));
      m_tabBox.addItem(new Boolean(false));
      if(((DatabaseSaver)m_dsSaver.getSaverTemplate()).getRelationForTableName() == false)
          m_tabBox.setSelectedIndex(1);
      else
          m_tabBox.setSelectedIndex(0); 
      m_tabBox.addItemListener(new ItemListener(){
            public void itemStateChanged(ItemEvent e){
                m_tableText.setEditable(!((Boolean)m_tabBox.getSelectedItem()).booleanValue());
            }
      });
      
      JLabel tabLab = new JLabel(Messages.getInstance().getString("SaverCustomizer_SetUpDatabase_TabLab_JLabel_Text"), SwingConstants.LEFT);
      tabLab.setFont(new Font("Monospaced", Font.PLAIN, 12));
      
      m_idBox = new JComboBox();
      m_idBox.addItem(new Boolean(true));
      m_idBox.addItem(new Boolean(false));
      if(((DatabaseSaver)m_dsSaver.getSaverTemplate()).getAutoKeyGeneration() == false)
          m_idBox.setSelectedIndex(1);
      else
          m_idBox.setSelectedIndex(0); 
      JLabel idLab = new JLabel(Messages.getInstance().getString("SaverCustomizer_SetUpDatabase_IdLab_JLabel_Text"), SwingConstants.LEFT);
      idLab.setFont(new Font("Monospaced", Font.PLAIN, 12));

      JPanel urlP = new JPanel();   

      urlP.setLayout(new FlowLayout(FlowLayout.LEFT));
      urlP.add(dbaseURLLab);//, BorderLayout.WEST);
      urlP.add(m_dbaseURLText);//, BorderLayout.CENTER);
      db.add(urlP);

      JPanel usernameP = new JPanel();   
      usernameP.setLayout(new FlowLayout(FlowLayout.LEFT));
      usernameP.add(userNameLab);//, BorderLayout.WEST);
      usernameP.add(m_userNameText);//, BorderLayout.CENTER);
      db.add(usernameP);

      JPanel passwordP = new JPanel();   
      passwordP.setLayout(new FlowLayout(FlowLayout.LEFT));
      passwordP.add(passwordLab);//, BorderLayout.WEST);
      passwordP.add(m_passwordText);//, BorderLayout.CENTER);
      db.add(passwordP);
      
      JPanel tabP = new JPanel();   

      tabP.setLayout(new FlowLayout(FlowLayout.LEFT));
      tabP.add(tabLab);//, BorderLayout.WEST);
      tabP.add(m_tabBox);//, BorderLayout.CENTER);
      db.add(tabP);
      
      JPanel tableP = new JPanel();   

      tableP.setLayout(new FlowLayout(FlowLayout.LEFT));
      tableP.add(tableLab);//, BorderLayout.WEST);
      tableP.add(m_tableText);//, BorderLayout.CENTER);
      db.add(tableP);
      
      JPanel keyP = new JPanel();   

      keyP.setLayout(new FlowLayout(FlowLayout.LEFT));
      keyP.add(idLab);//, BorderLayout.WEST);
      keyP.add(m_idBox);//, BorderLayout.CENTER);
      db.add(keyP);

      JPanel buttonsP = new JPanel();
      buttonsP.setLayout(new FlowLayout());
      JButton ok,cancel;
      buttonsP.add(ok = new JButton(Messages.getInstance().getString("SaverCustomizer_SetUpDatabase_ButtonsP_Ok_JButton_Text")));
      buttonsP.add(cancel=new JButton(Messages.getInstance().getString("SaverCustomizer_SetUpDatabase_ButtonsP_Cancel_JButton_Text")));
      ok.addActionListener(new ActionListener(){
	public void actionPerformed(ActionEvent evt){
          ((DatabaseSaver)m_dsSaver.getSaverTemplate()).resetStructure();  
	  ((DatabaseConverter)m_dsSaver.getSaverTemplate()).setUrl(m_dbaseURLText.getText());
          ((DatabaseConverter)m_dsSaver.getSaverTemplate()).setUser(m_userNameText.getText());
          ((DatabaseConverter)m_dsSaver.getSaverTemplate()).setPassword(new String(m_passwordText.getPassword()));
          if(!((Boolean)m_tabBox.getSelectedItem()).booleanValue())
                ((DatabaseSaver)m_dsSaver.getSaverTemplate()).setTableName(m_tableText.getText());
          ((DatabaseSaver)m_dsSaver.getSaverTemplate()).setAutoKeyGeneration(((Boolean)m_idBox.getSelectedItem()).booleanValue());
          ((DatabaseSaver)m_dsSaver.getSaverTemplate()).setRelationForTableName(((Boolean)m_tabBox.getSelectedItem()).booleanValue());
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
    JPanel about = m_SaverEditor.getAboutPanel();
    if (about != null) {
      add(about, BorderLayout.NORTH);
    }
    add(db,BorderLayout.SOUTH);
  }

  /** Sets up dialog for saving instances in a file */  
  public void setUpFile() {
    removeAll();
    m_fileChooser.setFileFilter(new FileFilter()
      { public boolean accept(File f)
        { return f.isDirectory();}
        public String getDescription()
        { return Messages.getInstance().getString("SaverCustomizer_SetUpFile_FileChooser_SetFileFilter_GetDescription_Text");}
      });
    m_fileChooser.setAcceptAllFileFilterUsed(false);
    try{
      if(!(((m_dsSaver.getSaverTemplate()).retrieveDir()).equals(""))) {
        File tmp = new File(m_dsSaver.getSaverTemplate().retrieveDir());
        tmp = new File(tmp.getAbsolutePath());
        m_fileChooser.setCurrentDirectory(tmp);
      }
    }catch(Exception ex){
      System.out.println(ex);
    }
    JPanel innerPanel = new JPanel();
    innerPanel.setLayout(new BorderLayout());
    try{
      m_prefixText = new JTextField(m_dsSaver.getSaverTemplate().filePrefix(),25);
      m_prefixText.setToolTipText(Messages.getInstance().getString("SaverCustomizer_SetUpFile_PrefixText_SetToolTipText_Text"));
      final JLabel prefixLab = 
        new JLabel(Messages.getInstance().getString("SaverCustomizer_SetUpFile_PrefixLab_JLabel_Text"), SwingConstants.LEFT);
      
      m_relationNameForFilename = new JCheckBox(Messages.getInstance().getString("SaverCustomizer_SetUpFile_RelationNameForFilename_JCheckBox_Text"));
      m_relationNameForFilename.setSelected(m_dsSaver.getRelationNameForFilename());
      m_relationNameForFilename.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (m_relationNameForFilename.isSelected()) {
            prefixLab.setText(Messages.getInstance().getString("SaverCustomizer_SetUpFile_PrefixLab_SetText_Text_First"));
            m_fileChooser.setApproveButtonText(Messages.getInstance().getString("SaverCustomizer_SetUpFile_FileChooser_SetApproveButtonText_Text_First"));
          } else {
            prefixLab.setText(Messages.getInstance().getString("SaverCustomizer_SetUpFile_PrefixLab_SetText_Text_Second"));
            m_fileChooser.setApproveButtonText(Messages.getInstance().getString("SaverCustomizer_SetUpFile_FileChooser_SetApproveButtonText_Text_Second"));
          }
        }
      });
      
      JPanel prefixP = new JPanel();   
      prefixP.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

      prefixP.setLayout(new BorderLayout());
      prefixP.add(prefixLab, BorderLayout.WEST);
      prefixP.add(m_prefixText, BorderLayout.CENTER);
      prefixP.add(m_relationNameForFilename, BorderLayout.SOUTH);
      innerPanel.add(prefixP, BorderLayout.SOUTH);
    } catch(Exception ex){
    }
    //innerPanel.add(m_SaverEditor, BorderLayout.SOUTH);
    JPanel about = m_SaverEditor.getAboutPanel();
    if (about != null) {
      innerPanel.add(about, BorderLayout.NORTH);
    }
    add(innerPanel, BorderLayout.NORTH);
    add(m_fileChooser, BorderLayout.CENTER);

    m_relativeFilePath = new JCheckBox(Messages.getInstance().getString("SaverCustomizer_SetUpFile_RelativeFilePath_JCheckBox_Text"));
    m_relativeFilePath.
      setSelected(((FileSourcedConverter)m_dsSaver.getSaverTemplate()).getUseRelativePath());

    m_relativeFilePath.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          ((FileSourcedConverter)m_dsSaver.getSaverTemplate()).
            setUseRelativePath(m_relativeFilePath.isSelected());
        }
      });
    JPanel holderPanel = new JPanel();
    holderPanel.setLayout(new FlowLayout());
    holderPanel.add(m_relativeFilePath);
    add(holderPanel, BorderLayout.SOUTH);
  }

  /**
   * Set the saver to be customized
   *
   * @param object a weka.gui.beans.Saver
   */
  public void setObject(Object object) {
    m_dsSaver = (weka.gui.beans.Saver)object;
    m_SaverEditor.setTarget(m_dsSaver.getSaverTemplate());
    if(m_dsSaver.getSaverTemplate() instanceof DatabaseConverter){
            setUpDatabase();
    }
    else{
        if (m_dsSaver.getSaverTemplate() instanceof FileSourcedConverter) {
            setUpFile();
        } else {
            setUpOther();
        }
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
