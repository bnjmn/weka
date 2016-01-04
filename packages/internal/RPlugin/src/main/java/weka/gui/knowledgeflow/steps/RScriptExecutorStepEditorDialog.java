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
 *    RScriptExecutorStepEditorDialog
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import weka.core.RSession;
import weka.core.Utils;
import weka.gui.knowledgeflow.GOEStepEditorDialog;
import weka.gui.visualize.VisualizeUtils;
import weka.knowledgeflow.steps.RScriptExecutor;
import weka.knowledgeflow.steps.Step;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Constructor;
import java.util.Properties;

/**
 * Editor dialog for the RScriptExecutor step
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class RScriptExecutorStepEditorDialog extends GOEStepEditorDialog {

  /** editor setup */
  public final static String PROPERTIES_FILE =
    "weka/knowledgeflow/steps/R.props";

  private static final long serialVersionUID = -5194050989123506681L;

  /** True if the R environment is available */
  private boolean m_rAvailable = true;

  /** Editor pane for the script */
  protected JTextPane m_scriptEditor;

  protected JMenuBar m_menuBar;

  @Override
  public void setStepToEdit(Step step) {
    copyOriginal(step);

    Properties props;
    try {
      props = Utils.readProperties(PROPERTIES_FILE);
    } catch (Exception ex) {
      ex.printStackTrace();
      props = new Properties();
    }

    // check for SyntaxDocument
    boolean syntaxDocAvailable = true;
    try {
      Class.forName("weka.gui.scripting.SyntaxDocument");
    } catch (Exception ex) {
      ex.printStackTrace();
      syntaxDocAvailable = false;
    }

    addPrimaryEditorPanel(BorderLayout.CENTER);

    String script = ((RScriptExecutor) getStepToEdit()).getRScript();
    m_scriptEditor = new JTextPane();
    if (props.getProperty("Syntax", "false").equals("true")
      && syntaxDocAvailable) {
      try {
        Class syntaxClass = Class.forName("weka.gui.scripting.SyntaxDocument");
        Constructor constructor = syntaxClass.getConstructor(Properties.class);
        Object doc = constructor.newInstance(props);
        m_scriptEditor.setDocument((DefaultStyledDocument) doc);
        m_scriptEditor.setBackground(VisualizeUtils.processColour(
          props.getProperty("BackgroundColor", "white"), Color.WHITE));
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    } else {
      m_scriptEditor.setForeground(VisualizeUtils.processColour(
        props.getProperty("ForegroundColor", "black"), Color.BLACK));
      m_scriptEditor.setBackground(VisualizeUtils.processColour(
        props.getProperty("BackgroundColor", "white"), Color.WHITE));
      m_scriptEditor
        .setFont(new Font(props.getProperty("FontName", "monospaced"),
          Font.PLAIN, Integer.parseInt(props.getProperty("FontSize", "12"))));
    }

    // check R availablility

    try {
      RSession.acquireSession(this);
    } catch (Exception e) {
      m_rAvailable = false;
    } finally {
      RSession.releaseSession(this);
    }

    try {
      if (m_rAvailable) {
        m_scriptEditor.getDocument().insertString(0, script, null);
      } else {
        String message = "R does not seem to be available. Check that "
          + "you have the R_HOME environment variable set, R is in your"
          + " path and that java.library.path property points to the "
          + "JRI native library. Information on settup for different "
          + "OS can be found at http://www.rforge.net/JRI";
        m_scriptEditor.getDocument().insertString(0, message, null);
      }
    } catch (BadLocationException e) {
      showErrorDialog(e);
    }

    final JFileChooser fileChooser = new JFileChooser();
    fileChooser.setAcceptAllFileFilterUsed(true);
    fileChooser.setMultiSelectionEnabled(false);

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
        m_scriptEditor.setText("");
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
        int retVal =
          fileChooser.showOpenDialog(RScriptExecutorStepEditorDialog.this);
        if (retVal == JFileChooser.APPROVE_OPTION) {
          // boolean ok = m_script.open(fileChooser.getSelectedFile());
          StringBuilder sb = new StringBuilder();
          try {
            BufferedReader br =
              new BufferedReader(new FileReader(fileChooser.getSelectedFile()));
            String line;
            while ((line = br.readLine()) != null) {
              sb.append(line).append("\n");
            }
            // m_scriptEditor.getDocument().insertString(0, sb.toString(),
            // null);
            m_scriptEditor.setText(sb.toString());
            br.close();
          } catch (Exception ex) {
            JOptionPane.showMessageDialog(RScriptExecutorStepEditorDialog.this,
              "Couldn't open file '" + fileChooser.getSelectedFile() + "'!");
            ex.printStackTrace();
          }
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
        // save(fileChooser);
        if (m_scriptEditor.getText() != null
          && m_scriptEditor.getText().length() > 0) {

          int retVal =
            fileChooser.showSaveDialog(RScriptExecutorStepEditorDialog.this);
          if (retVal == JFileChooser.APPROVE_OPTION) {
            try {
              BufferedWriter bw = new BufferedWriter(
                new FileWriter(fileChooser.getSelectedFile()));
              bw.write(m_scriptEditor.getText());
              bw.flush();
              bw.close();
            } catch (Exception ex) {
              JOptionPane.showMessageDialog(
                RScriptExecutorStepEditorDialog.this,
                "Unable to save script file '" + fileChooser.getSelectedFile()
                  + "'!");
              ex.printStackTrace();
            }
          }
        }
      }
    });

    JPanel p = new JPanel(new BorderLayout());
    JScrollPane editorScroller = new JScrollPane(m_scriptEditor);
    editorScroller.setBorder(BorderFactory.createTitledBorder("R Script"));
    p.add(editorScroller, BorderLayout.CENTER);
    Dimension d = new Dimension(450, 200);
    editorScroller.setMinimumSize(d);
    editorScroller.setPreferredSize(d);

    m_primaryEditorHolder.add(p, BorderLayout.CENTER);
    add(m_editorHolder, BorderLayout.CENTER);

    try {
      if (m_rAvailable) {
        m_scriptEditor.setText(script);
      } else {
        String message = "R does not seem to be available:\n";
        m_scriptEditor.getDocument().insertString(0, message, null);
      }
    } catch (BadLocationException ex) {
      ex.printStackTrace();
    }

    if (m_parent instanceof javax.swing.JDialog) {
      ((javax.swing.JDialog) m_parent).setJMenuBar(m_menuBar);
    }
  }

  /**
   * Gets called when the OK button is pressed. Sets the edited script on the
   * PythonScriptExecutor step being edited
   */
  @Override
  protected void okPressed() {
    if (!m_rAvailable) {
      return;
    }

    ((RScriptExecutor) getStepToEdit())
      .setRScript(m_scriptEditor.getText());
  }
}
