package weka.gui;


import weka.core.Tag;
import weka.core.SelectedTag;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import java.io.File;
import java.awt.FontMetrics;
import javax.swing.JFileChooser;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class FileEditor extends PropertyEditorSupport {

  protected JFileChooser m_FileChooser;
  
  public String getJavaInitializationString() {

    File f = (File) getValue();
    if (f == null) {
      return "null";
    }
    return "new File(\"" + f.getName() + "\")";
  }

  /**
   * Returns true because we do support a custom editor.
   *
   * @return true
   */
  public boolean supportsCustomEditor() {
    return true;
  }
  public java.awt.Component getCustomEditor() {

    if (m_FileChooser == null) {
      m_FileChooser = new JFileChooser();
      m_FileChooser.setApproveButtonText("Select");
      m_FileChooser.setApproveButtonMnemonic('S');
      m_FileChooser.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  String cmdString = e.getActionCommand();
	  if (cmdString.equals(JFileChooser.APPROVE_SELECTION)) {
	    File newVal = m_FileChooser.getSelectedFile();
	    setValue(newVal);
	  }
	}
      });
    }
    return m_FileChooser;
  }

  public boolean isPaintable() {
    return true;
  }

  /**
   * Paints a representation of the current Object.
   *
   * @param gfx the graphics context to use
   * @param box the area we are allowed to paint into
   */
  public void paintValue(java.awt.Graphics gfx, java.awt.Rectangle box) {

    FontMetrics fm = gfx.getFontMetrics();
    int vpad = (box.height - fm.getHeight()) / 2 ;
    File f = (File) getValue();
    String val = "No file";
    if (f != null) {
      val = f.getName();
    }
    gfx.drawString(val, 2, fm.getHeight() + vpad);
  }  
}

