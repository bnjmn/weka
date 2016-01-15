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
 *    StepEditorDialog.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow;

import weka.core.Environment;
import weka.core.EnvironmentHandler;
import weka.core.Utils;
import weka.gui.PropertyDialog;
import weka.gui.SettingsEditor;
import weka.knowledgeflow.steps.Step;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Base class for step editor dialogs. Clients can extend this in order to
 * supply their own custom dialogs. The constructor for this class provides OK
 * and Cancel buttons in the SOUTH location of a BorderLayout. To do meaningful
 * things on click of OK or Cancel, subclasses should override okPressed()
 * and/or cancelPressed(). If setStepToEdit() is not overridden, then this class
 * will also provide an "about" panel in the NORTH location of BorderLayout.
 * This then leaves the CENTER of the BorderLayout for custom widgets.
 * Subclasses should typically override the no-op layoutEditor() method to add
 * their widgets.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public abstract class StepEditorDialog extends JPanel
  implements EnvironmentHandler {

  /** For serialization */
  private static final long serialVersionUID = -4860182109190301676L;

  protected boolean m_isEdited;

  protected Environment m_env = Environment.getSystemWide();

  protected JPanel m_buttonHolder = new JPanel(new GridLayout(1, 0));
  protected JButton m_okBut = new JButton("OK");
  protected JButton m_cancelBut = new JButton("Cancel");
  protected JButton m_settingsBut = new JButton("Settings");

  protected MainKFPerspective m_mainPerspective;

  protected Window m_parent;

  protected ClosingListener m_closingListener;

  protected Step m_stepToEdit;
  protected StringBuilder m_helpText = new StringBuilder();
  protected JButton m_helpBut = new JButton("About");

  public StepEditorDialog() {
    setLayout(new BorderLayout());

    m_buttonHolder.add(m_okBut);
    m_buttonHolder.add(m_cancelBut);
    add(m_buttonHolder, BorderLayout.SOUTH);

    m_okBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        ok();
      }
    });

    m_cancelBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cancel();
      }
    });
  }

  protected void setMainPerspective(MainKFPerspective main) {
    m_mainPerspective = main;
  }

  protected MainKFPerspective getMainPerspective() {
    return m_mainPerspective;
  }

  protected void showErrorDialog(Exception cause) {
    m_mainPerspective.showErrorDialog(cause);
  }

  protected void showInfoDialog(Object information, String title,
    boolean isWarning) {
    m_mainPerspective.showInfoDialog(information, title, isWarning);
  }

  protected Step getStepToEdit() {
    return m_stepToEdit;
  }

  protected void setStepToEdit(Step step) {
    m_stepToEdit = step;

    createAboutPanel(step);
    if (step.getDefaultSettings() != null) {
      addSettingsButton();
    }
    layoutEditor();
  }

  protected void layoutEditor() {
    // subclasses can override to add their
    // stuff to the center of the borderlayout
  }

  protected void addSettingsButton() {
    getMainPerspective().getMainApplication().getApplicationSettings()
      .applyDefaults(getStepToEdit().getDefaultSettings());
    m_buttonHolder.add(m_settingsBut);
    m_settingsBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          SettingsEditor.showSingleSettingsEditor(
            getMainPerspective().getMainApplication().getApplicationSettings(),
            getStepToEdit().getDefaultSettings().getID(),
            getStepToEdit().getName(), StepEditorDialog.this);
        } catch (IOException ex) {
          showErrorDialog(ex);
        }
      }
    });
  }

  protected void setParentWindow(Window parent) {
    m_parent = parent;
  }

  protected void setClosingListener(ClosingListener c) {
    m_closingListener = c;
  }

  protected boolean isEdited() {
    return m_isEdited;
  }

  protected void setEdited(boolean edited) {
    m_isEdited = edited;
  }

  private void ok() {
    setEdited(true);

    okPressed();
    if (m_parent != null) {
      m_parent.dispose();
    }

    if (m_closingListener != null) {
      m_closingListener.closing();
    }
  }

  protected void okPressed() {
    // subclasses to override
  }

  protected void cancelPressed() {
    // subclasses to override
  }

  private void cancel() {
    setEdited(false);

    cancelPressed();
    if (m_parent != null) {
      m_parent.dispose();
    }

    if (m_closingListener != null) {
      m_closingListener.closing();
    }
  }

  protected void createAboutPanel(Step step) {
    String globalFirstSentence = "";
    String globalInfo = Utils.getGlobalInfo(step, false);
    if (globalInfo == null) {
      globalInfo = "No info available";
      globalFirstSentence = globalInfo;
    } else {
      globalInfo = globalInfo.replace("font color=blue", "font color=black");
      try {
        Method gI = step.getClass().getMethod("globalInfo");
        String globalInfoNoHTML = gI.invoke(step).toString();
        globalFirstSentence = globalInfoNoHTML.contains(".")
          ? globalInfoNoHTML.substring(0, globalInfoNoHTML.indexOf('.'))
          : globalInfoNoHTML;
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }

    createAboutPanel(globalInfo, globalFirstSentence);
  }

  private void createAboutPanel(String about, String firstSentence) {
    JTextArea jt = new JTextArea();

    m_helpText.append(about);
    jt.setColumns(30);
    // jt.setContentType("text/html");
    jt.setFont(new Font("SansSerif", Font.PLAIN, 12));
    jt.setEditable(false);
    jt.setLineWrap(true);
    jt.setWrapStyleWord(true);

    jt.setText(firstSentence);
    jt.setBackground(getBackground());

    String className = m_stepToEdit.getClass().getName();
    className =
      className.substring(className.lastIndexOf('.') + 1, className.length());
    m_helpBut.setToolTipText("More information about " + className);

    final JPanel jp = new JPanel();
    jp.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createTitledBorder("About"),
      BorderFactory.createEmptyBorder(5, 5, 5, 5)));
    jp.setLayout(new BorderLayout());
    jp.add(new JScrollPane(jt), BorderLayout.CENTER);

    JPanel p2 = new JPanel();
    p2.setLayout(new BorderLayout());
    p2.add(m_helpBut, BorderLayout.NORTH);
    jp.add(p2, BorderLayout.EAST);

    add(jp, BorderLayout.NORTH);

    int preferredWidth = jt.getPreferredSize().width;
    jt.setSize(new Dimension(Math.min(preferredWidth, 600), Short.MAX_VALUE));
    Dimension d = jt.getPreferredSize();
    jt.setPreferredSize(new Dimension(Math.min(preferredWidth, 600), d.height));

    m_helpBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent a) {
        openHelpFrame(jp);
        m_helpBut.setEnabled(false);
      }
    });
  }

  private void openHelpFrame(JPanel aboutPanel) {
    JTextPane ta = new JTextPane();
    ta.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    ta.setContentType("text/html");
    // ta.setLineWrap(true);
    // ta.setWrapStyleWord(true);
    // ta.setBackground(getBackground());
    ta.setEditable(false);
    ta.setText(m_helpText.toString());
    ta.setCaretPosition(0);
    JDialog jdtmp;
    if (PropertyDialog.getParentDialog(this) != null) {
      jdtmp = new JDialog(PropertyDialog.getParentDialog(this), "Information");
    } else if (PropertyDialog.getParentFrame(this) != null) {
      jdtmp = new JDialog(PropertyDialog.getParentFrame(this), "Information");
    } else {
      jdtmp = new JDialog((Frame) null, "Information");
    }
    final JDialog jd = jdtmp;
    jd.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        jd.dispose();
        m_helpBut.setEnabled(true);
      }
    });
    jd.getContentPane().setLayout(new BorderLayout());
    jd.getContentPane().add(new JScrollPane(ta), BorderLayout.CENTER);
    jd.pack();
    jd.setSize(400, 350);
    jd.setLocation(
      aboutPanel.getTopLevelAncestor().getLocationOnScreen().x
        + aboutPanel.getTopLevelAncestor().getSize().width,
      aboutPanel.getTopLevelAncestor().getLocationOnScreen().y);
    jd.setVisible(true);
  }

  public Environment getEnvironment() {
    return m_env;
  }

  @Override
  public void setEnvironment(Environment env) {
    m_env = env;
  }

  /**
   * Substitute the values of any environment variables present in the supplied
   * string
   *
   * @param source the source string to substitute vars in
   * @return the string with any environment variables substituted
   */
  public String environmentSubstitute(String source) {
    String result = source;
    if (result != null) {
      try {
        result = m_env.substitute(result);
      } catch (Exception ex) {
        // ignore
      }
    }

    return result;
  }

  /**
   * Interface for those that want to be notified when this dialog closes
   */
  public interface ClosingListener {
      void closing();
  }
}
