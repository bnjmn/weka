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
 *    PythonScriptExecutorStepEditorDialog
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import jsyntaxpane.DefaultSyntaxKit;
import weka.gui.knowledgeflow.GOEStepEditorDialog;
import weka.knowledgeflow.steps.PythonScriptExecutor;
import weka.knowledgeflow.steps.Step;
import weka.python.PythonSession;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Editor dialog for the PythonScriptExecutor step
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class PythonScriptExecutorStepEditorDialog extends GOEStepEditorDialog {

  private static final long serialVersionUID = 4072516424536789076L;

  /** True if the python environment is available */
  private boolean m_pyAvailable = true;

  /** Editor pane for the script */
  protected JEditorPane m_scriptEditor;

  /** Menu bar to use */
  protected JMenuBar m_menuBar;

  /**
   * Set the step to edit. Also configures most of the GUI
   *
   * @param step the step to edit
   */
  @Override
  public void setStepToEdit(Step step) {
    copyOriginal(step);

    addPrimaryEditorPanel(BorderLayout.CENTER);

    String script = ((PythonScriptExecutor) getStepToEdit()).getPythonScript();
    ClassLoader orig = Thread.currentThread().getContextClassLoader();

    // jsyntaxpane uses Class.forName() to instantiate syntax handlers defined
    // properties files. Force Class.forName() to use our classloader
    try {
      Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
      DefaultSyntaxKit.initKit();
    } finally {
      Thread.currentThread().setContextClassLoader(orig);
    }
    m_scriptEditor = new JEditorPane();

    // check python availability
    m_pyAvailable = true;
    String envEvalResults = null;
    Exception envEvalEx = null;
    if (!PythonSession.pythonAvailable()) {
      // try initializing
      try {
        if (!PythonSession.initSession("python",
          ((PythonScriptExecutor) getStepToEdit()).getDebug())) {
          envEvalResults = PythonSession.getPythonEnvCheckResults();
          m_pyAvailable = false;
        }
      } catch (Exception ex) {
        envEvalEx = ex;
        m_pyAvailable = false;
        showErrorDialog(ex);
      }
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
          fileChooser.showOpenDialog(PythonScriptExecutorStepEditorDialog.this);
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
            JOptionPane.showMessageDialog(
              PythonScriptExecutorStepEditorDialog.this,
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

          int retVal = fileChooser
            .showSaveDialog(PythonScriptExecutorStepEditorDialog.this);
          if (retVal == JFileChooser.APPROVE_OPTION) {
            try {
              BufferedWriter bw = new BufferedWriter(
                new FileWriter(fileChooser.getSelectedFile()));
              bw.write(m_scriptEditor.getText());
              bw.flush();
              bw.close();
            } catch (Exception ex) {
              JOptionPane.showMessageDialog(
                PythonScriptExecutorStepEditorDialog.this,
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
    m_scriptEditor.setContentType("text/python");
    editorScroller.setBorder(BorderFactory.createTitledBorder("Python Script"));
    p.add(editorScroller, BorderLayout.CENTER);
    Dimension d = new Dimension(450, 200);
    editorScroller.setMinimumSize(d);
    editorScroller.setPreferredSize(d);

    m_primaryEditorHolder.add(p, BorderLayout.CENTER);
    add(m_editorHolder, BorderLayout.CENTER);

    try {
      if (m_pyAvailable) {
        m_scriptEditor.setText(script);
      } else {
        String message = "Python does not seem to be available:\n\n"
          + (envEvalResults != null && envEvalResults.length() > 0
            ? envEvalResults
            : (envEvalEx != null ? envEvalEx.getMessage() : ""));
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
    if (!m_pyAvailable) {
      return;
    }

    ((PythonScriptExecutor) getStepToEdit())
      .setPythonScript(m_scriptEditor.getText());
  }
}
