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

public class SelectedTagEditor extends PropertyEditorSupport {

  public String getJavaInitializationString() {

    SelectedTag s = (SelectedTag)getValue();
    Tag [] tags = s.getTags();
    String result = "new SelectedTag("
      + s.getSelectedTag().getID()
      + ", {\n";
    for (int i = 0; i < tags.length; i++) {
      result += "new Tag(" + tags[i].getID()
	+ ",\"" + tags[i].getReadable()
	+ "\")";
      if (i < tags.length - 1) {
	result += ',';
      }
      result += '\n';
    }
    return result + "})";
  }

  public String getAsText() {

    SelectedTag s = (SelectedTag)getValue();
    return s.getSelectedTag().getReadable();
  }

  public void setAsText(String text)
    throws java.lang.IllegalArgumentException {

    SelectedTag s = (SelectedTag)getValue();
    Tag [] tags = s.getTags();
    try {
      for (int i = 0; i < tags.length; i++) {
	if (text.equals(tags[i].getReadable())) {
	  setValue(new SelectedTag(tags[i].getID(), tags));
	  return;
	}
      }
    } catch (Exception ex) {
      throw new java.lang.IllegalArgumentException(text);
    }
  }

  public String[] getTags() {

    SelectedTag s = (SelectedTag)getValue();
    Tag [] tags = s.getTags();
    String [] result = new String [tags.length];
    for (int i = 0; i < tags.length; i++) {
      result[i] = tags[i].getReadable();
    }
    return result;
  }
  
  /**
   * Tests out the selectedtag editor from the command line.
   *
   * @param args ignored
   */
  public static void main(String [] args) {

    try {

      System.err.println("---Registering Weka Editors---");
      java.beans.PropertyEditorManager.registerEditor(SelectedTag.class,
						      SelectedTagEditor.class);
      Tag [] tags =  {
	new Tag(1, "First option"),
	new Tag(2, "Second option"),
	new Tag(3, "Third option"),
	new Tag(4, "Fourth option"),
	new Tag(5, "Fifth option"),
      };
      SelectedTag initial = new SelectedTag(1, tags);
      SelectedTagEditor ce = new SelectedTagEditor();
      ce.setValue(initial);
      PropertyValueSelector ps = new PropertyValueSelector(ce);
      JFrame f = new JFrame(); 
      f.addWindowListener(new WindowAdapter() {
	public void windowClosing(WindowEvent e) {
	  System.exit(0);
	}
      });
      f.getContentPane().setLayout(new BorderLayout());
      f.getContentPane().add(ps, BorderLayout.CENTER);
      f.pack();
      f.setVisible(true);
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}

