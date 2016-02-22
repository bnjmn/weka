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
 * BaseInteractiveViewer.java
 * Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 */

package weka.gui.knowledgeflow;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.*;

import weka.core.Defaults;
import weka.core.Settings;
import weka.gui.SettingsEditor;
import weka.knowledgeflow.steps.Step;

/**
 * Base class than clients can extend when implementing
 * {@code StepInteractiveViewer}. Provides a {@code BorderLayout} with a close
 * button in the south position. Also provides a {@code addButton()} method that
 * subclasses can call to add additional buttons to the bottom of the window. If
 * the subclass returns a default settings object from the
 * {@code getDefaultSettings()} method, then a settings button will also appear
 * at the bottom of the window - this will pop up a settings editor. In this
 * case, the subclass should also override the no-op {@code applySettings()}
 * method in order to apply any settings changes that the user might have made
 * in the settings editor. There is also a {@code closePressed()} method that is
 * called when the close button is pressed. Subclasses can override this method
 * as necessary.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public abstract class BaseInteractiveViewer extends JPanel implements
  StepInteractiveViewer {

  private static final long serialVersionUID = -1191494001428785466L;

  /** Holds the step that this interactive viewer relates to */
  protected Step m_step;

  /** The close button, for closing the viewer */
  protected JButton m_closeBut = new JButton("Close");

  /** Holds buttons displayed at the bottom of the window */
  protected JPanel m_buttonHolder = new JPanel(new GridLayout());

  /** The parent window */
  protected Window m_parent;

  /** The main Knowledge Flow perspective */
  protected MainKFPerspective m_mainPerspective;

  /**
   * Constructor
   */
  public BaseInteractiveViewer() {
    super();
    setLayout(new BorderLayout());

    JPanel tempP = new JPanel(new BorderLayout());
    tempP.add(m_buttonHolder, BorderLayout.WEST);
    add(tempP, BorderLayout.SOUTH);

    m_closeBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        close();
      }
    });
    addButton(m_closeBut);

    if (getDefaultSettings() != null) {
      JButton editSettings = new JButton("Settings");
      editSettings.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          // popup single settings editor
          // String ID = getDefaultSettings().getID();
          try {
            if (SettingsEditor.showSingleSettingsEditor(getMainKFPerspective()
              .getMainApplication().getApplicationSettings(),
              getDefaultSettings().getID(), getViewerName(),
              BaseInteractiveViewer.this) == JOptionPane.OK_OPTION) {
              applySettings(getSettings());
            }
          } catch (IOException ex) {
            getMainKFPerspective().getMainApplication().showErrorDialog(ex);
          }
        }
      });
      addButton(editSettings);
    }
  }

  /**
   * Get the settings object for the application
   *
   * @return the settings object the settings object
   */
  @Override
  public Settings getSettings() {
    return m_mainPerspective.getMainApplication().getApplicationSettings();
  }

  /**
   * No-op implementation. Subcasses should override to be notified about, and
   * apply, any changed settings
   * 
   * @param settings the settings object that might (or might not) have been
   *          altered by the user
   */
  public void applySettings(Settings settings) {

  }

  /**
   * Set the main knowledge flow perspective. Implementations can then access
   * application settings if necessary
   *
   * @param perspective the main knowledge flow perspective
   */
  @Override
  public void setMainKFPerspective(MainKFPerspective perspective) {
    m_mainPerspective = perspective;

    m_mainPerspective.getMainApplication().getApplicationSettings()
      .applyDefaults(getDefaultSettings());
  }

  /**
   * Get the main knowledge flow perspective. Implementations can the access
   * application settings if necessary
   *
   * @return
   */
  @Override
  public MainKFPerspective getMainKFPerspective() {
    return m_mainPerspective;
  }

  /**
   * Set the step that owns this viewer. Implementations may want to access data
   * that has been computed by the step in question.
   *
   * @param theStep the step that owns this viewer
   */
  @Override
  public void setStep(Step step) {
    m_step = step;
  }

  /**
   * Get the step that owns this viewer.
   *
   * @return the {@code Step} that owns this viewer
   */
  public Step getStep() {
    return m_step;
  }

  /**
   * Called by the KnowledgeFlow application once the enclosing JFrame is
   * visible
   */
  @Override
  public void nowVisible() {
    // no-op. Subclasses to override if necessary
  }

  /**
   * Set the parent window for this viewer
   *
   * @param parent the parent window
   */
  @Override
  public void setParentWindow(Window parent) {
    m_parent = parent;
    m_parent.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        super.windowClosing(e);
        closePressed();
      }
    });
  }

  /**
   * Adds a button to the bottom of the window.
   *
   * @param button the button to add.
   */
  public void addButton(JButton button) {
    m_buttonHolder.add(button);
  }

  private void close() {
    closePressed();

    if (m_parent != null) {
      m_parent.dispose();
    }
  }

  /**
   * Called when the close button is pressed. Subclasses should override if they
   * need to do something before the window is closed
   */
  public void closePressed() {
    // subclasses to override if they need to do something before
    // the window is closed
  }

  /**
   * Get default settings for the viewer. Default implementation returns null,
   * i.e. no default settings. Subclasses can override if they have settings
   * (with default values) that the user can edit.
   *
   * @return the default settings for this viewer, or null if there are no
   *         user-editable settings
   */
  public Defaults getDefaultSettings() {
    // subclasses to override if they have default settings
    return null;
  }
}
