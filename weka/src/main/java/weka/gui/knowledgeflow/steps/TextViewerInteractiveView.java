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

/*`
 *    TextViewerInteractiveView.java
 *    Copyright (C) 2002-2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow.steps;

import weka.core.Defaults;
import weka.core.Environment;
import weka.core.Settings;
import weka.gui.ResultHistoryPanel;
import weka.gui.SaveBuffer;
import weka.gui.knowledgeflow.BaseInteractiveViewer;
import weka.knowledgeflow.steps.TextViewer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

/**
 * Interactive viewer for the TextViewer step
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class TextViewerInteractiveView extends BaseInteractiveViewer implements
  TextViewer.TextNotificationListener {

  private static final long serialVersionUID = -3164518320257969282L;

  /** Button for clearing the results */
  protected JButton m_clearButton = new JButton("Clear results");

  /** Holds the list of results */
  protected ResultHistoryPanel m_history;

  /** The main text output area */
  protected JTextArea m_outText;

  /** Scroll panel for the text area */
  protected JScrollPane m_textScroller;

  /**
   * Initialize the viewer
   */
  @Override
  public void init() {
    addButton(m_clearButton);
    m_outText = new JTextArea(20, 80);
    m_outText.setEditable(false);
    m_outText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    m_history = new ResultHistoryPanel(m_outText);
    m_history.setBorder(BorderFactory.createTitledBorder("Result list"));
    m_history.setHandleRightClicks(false);
    m_history.setDeleteListener(new ResultHistoryPanel.RDeleteListener() {
      @Override
      public void entryDeleted(String name, int index) {
        ((TextViewer) getStep()).getResults().remove(name);
      }

      @Override
      public void entriesDeleted(java.util.List<String> names,
        java.util.List<Integer> indexes) {
        for (String name : names) {
          ((TextViewer) getStep()).getResults().remove(name);
        }
      }
    });
    m_history.getList().addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (((e.getModifiers() & InputEvent.BUTTON1_MASK) != InputEvent.BUTTON1_MASK)
          || e.isAltDown()) {
          int index = m_history.getList().locationToIndex(e.getPoint());
          if (index != -1) {
            String name = m_history.getNameAtIndex(index);
            visualize(name, e.getX(), e.getY());
          } else {
            visualize(null, e.getX(), e.getY());
          }
        }
      }
    });

    m_textScroller = new JScrollPane(m_outText);
    m_textScroller.setBorder(BorderFactory.createTitledBorder("Text"));
    JSplitPane p2 =
      new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, m_history, m_textScroller);
    add(p2, BorderLayout.CENTER);

    // copy all results over to the history panel.
    Map<String, String> runResults = ((TextViewer) getStep()).getResults();
    if (runResults.size() > 0) {
      boolean first = true;
      String firstKey = "";
      for (Map.Entry<String, String> e : runResults.entrySet()) {
        if (first) {
          firstKey = e.getKey();
          first = false;
        }
        m_history
          .addResult(e.getKey(), new StringBuffer().append(e.getValue()));
      }
      m_history.setSingle(firstKey);
    }

    m_clearButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        m_history.clearResults();
        ((TextViewer) getStep()).getResults().clear();
        m_outText.setText("");
      }
    });

    applySettings(getSettings());
    ((TextViewer) getStep()).setTextNotificationListener(this);
  }

  /**
   * Called when the close button is pressed
   */
  @Override
  public void closePressed() {
    ((TextViewer) getStep())
      .removeTextNotificationListener(TextViewerInteractiveView.this);
  }

  /**
   * Applys settings from the supplied settings object
   *
   * @param settings the settings object that might (or might not) have been
   */
  @Override
  public void applySettings(Settings settings) {
    m_outText.setFont(settings.getSetting(TextViewerInteractiveViewDefaults.ID,
      TextViewerInteractiveViewDefaults.OUTPUT_FONT_KEY,
      TextViewerInteractiveViewDefaults.OUTPUT_FONT,
      Environment.getSystemWide()));
    m_history.setFont(settings.getSetting(TextViewerInteractiveViewDefaults.ID,
      TextViewerInteractiveViewDefaults.OUTPUT_FONT_KEY,
      TextViewerInteractiveViewDefaults.OUTPUT_FONT,
      Environment.getSystemWide()));
    m_outText.setForeground(settings.getSetting(
      TextViewerInteractiveViewDefaults.ID,
      TextViewerInteractiveViewDefaults.OUTPUT_TEXT_COLOR_KEY,
      TextViewerInteractiveViewDefaults.OUTPUT_TEXT_COLOR,
      Environment.getSystemWide()));
    m_outText.setBackground(settings.getSetting(
      TextViewerInteractiveViewDefaults.ID,
      TextViewerInteractiveViewDefaults.OUTPUT_BACKGROUND_COLOR_KEY,
      TextViewerInteractiveViewDefaults.OUTPUT_BACKGROUND_COLOR,
      Environment.getSystemWide()));
    m_textScroller.setBackground(settings.getSetting(
      TextViewerInteractiveViewDefaults.ID,
      TextViewerInteractiveViewDefaults.OUTPUT_BACKGROUND_COLOR_KEY,
      TextViewerInteractiveViewDefaults.OUTPUT_BACKGROUND_COLOR,
      Environment.getSystemWide()));
    m_outText.setRows(settings.getSetting(TextViewerInteractiveViewDefaults.ID,
      TextViewerInteractiveViewDefaults.NUM_ROWS_KEY,
      TextViewerInteractiveViewDefaults.NUM_ROWS, Environment.getSystemWide()));
    m_outText.setColumns(settings.getSetting(
      TextViewerInteractiveViewDefaults.ID,
      TextViewerInteractiveViewDefaults.NUM_COLUMNS_KEY,
      TextViewerInteractiveViewDefaults.NUM_COLUMNS,
      Environment.getSystemWide()));

    m_history.setBackground(settings.getSetting(
      TextViewerInteractiveViewDefaults.ID,
      TextViewerInteractiveViewDefaults.OUTPUT_BACKGROUND_COLOR_KEY,
      TextViewerInteractiveViewDefaults.OUTPUT_BACKGROUND_COLOR,
      Environment.getSystemWide()));
  }

  /**
   * Get the viewer name
   *
   * @return the viewer name
   */
  @Override
  public String getViewerName() {
    return "Text Viewer";
  }

  /**
   * Handles constructing a popup menu with visualization options.
   *
   * @param name the name of the result history list entry clicked on by the
   *          user
   * @param x the x coordinate for popping up the menu
   * @param y the y coordinate for popping up the menu
   */
  protected void visualize(String name, int x, int y) {
    final JPanel panel = this;
    final String selectedName = name;
    JPopupMenu resultListMenu = new JPopupMenu();

    JMenuItem visMainBuffer = new JMenuItem("View in main window");
    if (selectedName != null) {
      visMainBuffer.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          m_history.setSingle(selectedName);
        }
      });
    } else {
      visMainBuffer.setEnabled(false);
    }
    resultListMenu.add(visMainBuffer);

    JMenuItem visSepBuffer = new JMenuItem("View in separate window");
    if (selectedName != null) {
      visSepBuffer.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          m_history.openFrame(selectedName);
        }
      });
    } else {
      visSepBuffer.setEnabled(false);
    }
    resultListMenu.add(visSepBuffer);

    JMenuItem saveOutput = new JMenuItem("Save result buffer");
    if (selectedName != null) {
      saveOutput.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          SaveBuffer saveOut = new SaveBuffer(null, panel);
          StringBuffer sb = m_history.getNamedBuffer(selectedName);
          if (sb != null) {
            saveOut.save(sb);
          }
        }
      });
    } else {
      saveOutput.setEnabled(false);
    }
    resultListMenu.add(saveOutput);

    JMenuItem deleteOutput = new JMenuItem("Delete result buffer");
    if (selectedName != null) {
      deleteOutput.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          m_history.removeResult(selectedName);
        }
      });
    } else {
      deleteOutput.setEnabled(false);
    }
    resultListMenu.add(deleteOutput);

    resultListMenu.show(m_history.getList(), x, y);
  }

  /**
   * Get the default settings of this viewer
   *
   * @return the default settings
   */
  @Override
  public Defaults getDefaultSettings() {
    return new TextViewerInteractiveViewDefaults();
  }

  /**
   * Accept a new text result and add it to the result list
   *
   * @param name the name of the result
   * @param text the text of the result
   */
  @Override
  public void acceptTextResult(String name, String text) {
    m_history.addResult(name, new StringBuffer().append(text));
    m_history.setSingle(name);
  }

  /**
   * Defaults for this viewer
   */
  protected static final class TextViewerInteractiveViewDefaults extends
    Defaults {

    public static final String ID = "weka.gui.knowledgeflow.steps.textviewer";

    protected static final Settings.SettingKey OUTPUT_FONT_KEY =
      new Settings.SettingKey(ID + ".outputFont", "Font for text output",
        "Font to " + "use in the output area");
    protected static final Font OUTPUT_FONT = new Font("Monospaced",
      Font.PLAIN, 12);

    protected static final Settings.SettingKey OUTPUT_TEXT_COLOR_KEY =
      new Settings.SettingKey(ID + ".outputFontColor", "Output text color",
        "Color " + "of output text");
    protected static final Color OUTPUT_TEXT_COLOR = Color.black;

    protected static final Settings.SettingKey OUTPUT_BACKGROUND_COLOR_KEY =
      new Settings.SettingKey(ID + ".outputBackgroundColor",
        "Output background color", "Output background color");
    protected static final Color OUTPUT_BACKGROUND_COLOR = Color.white;

    protected static final Settings.SettingKey NUM_COLUMNS_KEY =
      new Settings.SettingKey(ID + ".numColumns", "Number of columns of text",
        "Number of columns of text");
    protected static final int NUM_COLUMNS = 80;

    protected static final Settings.SettingKey NUM_ROWS_KEY =
      new Settings.SettingKey(ID + ".numRows", "Number of rows of text",
        "Number of rows of text");
    protected static final int NUM_ROWS = 20;

    private static final long serialVersionUID = 8361658568822013306L;

    public TextViewerInteractiveViewDefaults() {
      super(ID);
      m_defaults.put(OUTPUT_FONT_KEY, OUTPUT_FONT);
      m_defaults.put(OUTPUT_TEXT_COLOR_KEY, OUTPUT_TEXT_COLOR);
      m_defaults.put(OUTPUT_BACKGROUND_COLOR_KEY, OUTPUT_BACKGROUND_COLOR);
      m_defaults.put(NUM_COLUMNS_KEY, NUM_COLUMNS);
      m_defaults.put(NUM_ROWS_KEY, NUM_ROWS);
    }
  }
}
