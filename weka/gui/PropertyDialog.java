package weka.gui;

import javax.swing.JFrame;
import javax.swing.JButton;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyEditor;

// Support for PropertyEditor with custom editors.
public class PropertyDialog extends JFrame {

  //  private JButton m_DoneButton;
  private PropertyEditor m_Editor;
  private Component m_EditorComponent;
  
  public PropertyDialog(PropertyEditor pe, int x, int y) {

    super(pe.getClass().getName());
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
	e.getWindow().dispose();
      }
    });
    getContentPane().setLayout(new BorderLayout());

    m_Editor = pe;
    m_EditorComponent = pe.getCustomEditor();
    getContentPane().add(m_EditorComponent, BorderLayout.CENTER);

    /*
    m_DoneButton = new JButton("Done");
    m_DoneButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
	dispose();
      }
    });
    getContentPane().add(m_DoneButton, BorderLayout.SOUTH);
    */
    
    pack();
    setLocation(x, y);
    setVisible(true);
  }

  public PropertyEditor getEditor() {

    return m_Editor;
  }
}

