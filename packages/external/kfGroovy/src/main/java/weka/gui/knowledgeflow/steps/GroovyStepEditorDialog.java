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
 *    GroovyStepEditorDialog.java
 *    Copyright (C) 2009 - 2015 University of Waikato
 *
 */

package weka.gui.knowledgeflow.steps;

import weka.core.Utils;
import weka.gui.knowledgeflow.StepEditorDialog;
import weka.gui.scripting.GroovyScript;
import weka.gui.visualize.VisualizeUtils;
import weka.knowledgeflow.steps.GroovyStep;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.text.DefaultStyledDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.lang.reflect.Constructor;
import java.util.Properties;

/**
 * Step editor dialog for the GroovyStep
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 */
public class GroovyStepEditorDialog extends StepEditorDialog {

  /** For serialization */
  private static final long serialVersionUID = -5303157822778214991L;

  /** The script from the GroovyComponent */
  protected GroovyScript m_script;

  /** Editor pane */
  protected JTextPane m_textPane = new JTextPane();

  /** Holds the template text of a new script */
  protected String m_newScript;

  /** Menu bar for this dialog */
  protected JMenuBar m_menuBar;

  /** File chooser to use */
  protected JFileChooser m_fileChooser = new JFileChooser();

  /** the Groovy setup. */
  public static final String PROPERTIES_FILE =
    "weka/gui/scripting/Groovy.props";

  /**
   * Get the script from the GroovyStep and do some initialization
   *
   * @throws Exception if a problem occurs
   */
  @SuppressWarnings("unchecked")
  protected void initialize() throws Exception {
    Properties props = Utils.readProperties(PROPERTIES_FILE);

    // check for SyntaxDocument
    boolean syntaxDocAvailable = true;
    try {
      Class.forName("weka.gui.scripting.SyntaxDocument");
    } catch (Exception ex) {
      syntaxDocAvailable = false;
    }

    if (props.getProperty("Syntax", "false").equals("true")
      && syntaxDocAvailable) {
      try {
        Class syntaxClass = Class.forName("weka.gui.scripting.SyntaxDocument");
        Constructor constructor = syntaxClass.getConstructor(Properties.class);
        Object doc = constructor.newInstance(props);
        // SyntaxDocument doc = new SyntaxDocument(props);
        m_textPane.setDocument((DefaultStyledDocument) doc);
        // m_textPane.setBackground(doc.getBackgroundColor());
        m_textPane.setBackground(VisualizeUtils.processColour(
          props.getProperty("BackgroundColor", "white"), Color.WHITE));
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    } else {
      m_textPane.setForeground(VisualizeUtils.processColour(
        props.getProperty("ForegroundColor", "black"), Color.BLACK));
      m_textPane.setBackground(VisualizeUtils.processColour(
        props.getProperty("BackgroundColor", "white"), Color.WHITE));
      m_textPane.setFont(new Font(props.getProperty("FontName", "monospaced"),
        Font.PLAIN, Integer.parseInt(props.getProperty("FontSize", "12"))));
    }

    m_script = new GroovyScript(m_textPane.getDocument());
    String script = ((GroovyStep) getStepToEdit()).getScript();
    if (script != null && script.length() > 0) {
      m_script.setContent(script);
    }

    Dimension d = new Dimension(600, 800);
    m_textPane.setMinimumSize(d);
    m_textPane.setPreferredSize(d);
  }

  /**
   * Layout the editor
   */
  @Override
  protected void layoutEditor() {

    try {
      initialize();
    } catch (Exception ex) {
      showErrorDialog(ex);
    }

    JPanel mainHolder = new JPanel(new BorderLayout());
    m_fileChooser.setAcceptAllFileFilterUsed(true);
    m_fileChooser.setMultiSelectionEnabled(false);

    setupNewScript();
    mainHolder.add(new JScrollPane(m_textPane), BorderLayout.CENTER);

    JButton newBut = new JButton("New script");
    JButton compileBut = new JButton("Compile script");

    m_buttonHolder.add(newBut);
    m_buttonHolder.add(compileBut);

    newBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        newScript(m_fileChooser);
      }
    });

    compileBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doCompile();
      }
    });

    m_menuBar = new JMenuBar();

    JMenu fileM = new JMenu();
    m_menuBar.add(fileM);
    fileM.setText("File");
    fileM.setMnemonic('F');

    JMenuItem newItem = new JMenuItem();
    fileM.add(newItem);
    newItem.setText("New");
    newItem.setAccelerator(
      KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_MASK));
    newItem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        newScript(m_fileChooser);
      }
    });

    JMenuItem loadItem = new JMenuItem();
    fileM.add(loadItem);
    loadItem.setText("Open File...");
    loadItem.setAccelerator(
      KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_MASK));
    loadItem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int retVal = m_fileChooser.showOpenDialog(GroovyStepEditorDialog.this);
        if (retVal == JFileChooser.APPROVE_OPTION) {
          boolean ok = m_script.open(m_fileChooser.getSelectedFile());
          if (!ok) {
            JOptionPane.showMessageDialog(GroovyStepEditorDialog.this,
              "Couldn't open file '" + m_fileChooser.getSelectedFile() + "'!");
          }
        }
      }
    });

    JMenuItem saveItem = new JMenuItem();
    fileM.add(saveItem);

    saveItem.setText("Save");
    saveItem.setAccelerator(
      KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_MASK));
    saveItem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_script.getFilename() != null) {
          save(null);
        } else {
          save(m_fileChooser);
        }
      }
    });

    JMenuItem saveAsItem = new JMenuItem();
    fileM.add(saveAsItem);

    saveAsItem.setText("Save As...");
    saveAsItem.setAccelerator(
      KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_MASK));
    saveAsItem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        save(m_fileChooser);
      }
    });

    JMenu scriptM = new JMenu();
    m_menuBar.add(scriptM);
    scriptM.setText("Script");
    scriptM.setMnemonic('S');

    JMenuItem compileItem = new JMenuItem();
    scriptM.add(compileItem);
    compileItem.setText("Compile");
    compileItem.setAccelerator(
      KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK));
    compileItem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doCompile();
      }
    });

    add(mainHolder, BorderLayout.CENTER);

    if (m_parent instanceof javax.swing.JDialog) {
      ((javax.swing.JDialog) m_parent).setJMenuBar(m_menuBar);
      ((javax.swing.JDialog) m_parent).setTitle("Groovy Script Editor");
    }
  }

  /**
   * Create a new "template" script
   * 
   * @param fileChooser a filechooser to popup if there are unsaved changes to
   *          an existing script
   */
  private void newScript(JFileChooser fileChooser) {
    if (m_script.isModified()) {
      // prompt for are you sure?
      int retVal = JOptionPane.showConfirmDialog(GroovyStepEditorDialog.this,
        "Save changes" + ((m_script.getFilename() != null)
          ? (" to " + m_script.getFilename() + "?") : " first?"));
      if (retVal == JOptionPane.OK_OPTION) {
        if (m_script.getFilename() != null) {
          save(null);
        } else {
          save(fileChooser);
        }
      }
    }
    m_script.empty();
    m_script.setContent(m_newScript);
  }

  private void doCompile() {
    String script = m_script.getContent();
    if (script != null && script.length() > 0) {
      try {
        GroovyStep.compileScript(script);
        JOptionPane.showMessageDialog(GroovyStepEditorDialog.this,
          "Script compiled OK.", "Script Status",
          JOptionPane.INFORMATION_MESSAGE);
      } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(GroovyStepEditorDialog.this,
          "Problem compiling script:\n" + ex.getMessage(), "Script Status",
          JOptionPane.INFORMATION_MESSAGE);
      }
    }
  }

  private void save(JFileChooser fileChooser) {
    boolean ok = false;
    int retVal;

    if (m_script.getFilename() == null || fileChooser != null) {
      retVal = fileChooser.showSaveDialog(GroovyStepEditorDialog.this);
      if (retVal == JFileChooser.APPROVE_OPTION) {
        ok = m_script.saveAs(fileChooser.getSelectedFile());
      }
    } else {
      ok = m_script.save();
    }

    if (!ok) {
      if (m_script.getFilename() != null) {
        JOptionPane.showMessageDialog(GroovyStepEditorDialog.this,
          "Failed to save file '" + m_script.getFilename().toString() + "'!");
      } else {
        JOptionPane.showMessageDialog(GroovyStepEditorDialog.this,
          "Failed to save file!");
      }
    }
  }

  /**
   * Set the edited script on the GroovyStep when OK is pressed
   */
  @Override
  protected void okPressed() {
    ((GroovyStep) getStepToEdit()).setScript(m_script.getContent());
  }

  /**
   * Check for unsaved changes when the cancel button is pressed
   */
  @Override
  protected void cancelPressed() {
    // if there are modifications, ask the user
    if (m_script.isModified()) {
      // prompt for are you sure?
      int retVal = JOptionPane.showConfirmDialog(GroovyStepEditorDialog.this,
        "Save changes" + ((m_script.getFilename() != null)
          ? (" to " + m_script.getFilename() + "?") : " first?"));
      if (retVal == JOptionPane.OK_OPTION) {
        if (m_script.getFilename() != null) {
          save(null);
        } else {
          save(m_fileChooser);
        }
      }
    }
  }

  /**
   * Creates a new "template" Groovy script that extends BaseStep
   */
  protected void setupNewScript() {
    StringBuilder temp = new StringBuilder();

    temp.append("import java.util.List\n");
    temp.append("import java.util.ArrayList\n");
    temp.append("import weka.core.*\n");
    temp.append("import weka.knowledgeflow.Data\n");
    temp.append("import weka.knowledgeflow.StepManager\n");
    temp.append("import weka.knowledgeflow.StepTask\n");
    temp.append("import weka.knowledgeflow.StepTaskCallback\n");
    temp.append("import weka.knowledgeflow.steps.Step\n");
    temp.append("import weka.knowledgeflow.steps.BaseStep\n\n");
    temp.append("// add further imports as necessary\n\n");

    temp.append("class MyScript extends BaseStep {\n\n");

    temp.append("\t/** Implement initialization stuff here */\n");
    temp.append("\tvoid stepInit() { }\n\n");

    temp.append("\t/** Main processing logic here for start points */\n");
    temp.append("\tvoid start() throws WekaException { }\n\n");

    temp
      .append("\t/** Main processing logic here for incoming connections */\n");
    temp
      .append("\tvoid processIncoming(Data data) throws WekaException { }\n\n");

    temp.append("\t/** Return a list of connection types that this step can\n"
      + "\t *  accept. See constants defined in weka.knowledgeflow.StepManager\n"
      + "\t * (or define your own connection types). */\n");
    temp.append(
      "\tList<String> getIncomingConnectionTypes() { return null }\n\n");

    temp.append("\t/** Return a list of connection types that this step can\n"
      + "\t *  produce as output. See constants defined in\n "
      + "\t * weka.knowledgeflow.StepManager (or define your own connection\n"
      + "\t * types). */\n");
    temp.append(
      "\tList<String> getOutgoingConnectionTypes() { return null }\n\n");

    temp.append("}\n");

    m_newScript = temp.toString();
  }
}
